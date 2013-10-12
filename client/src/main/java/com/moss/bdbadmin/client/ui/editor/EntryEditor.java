/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of bdbadmin.
 *
 * bdbadmin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * bdbadmin is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with bdbadmin; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.bdbadmin.client.ui.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import com.moss.bdbadmin.api.db.DbInfo;
import com.moss.bdbadmin.api.entry.EntryInfo;
import com.moss.bdbadmin.api.util.KeyFactory;
import com.moss.bdbadmin.client.service.BdbClient;
import com.moss.bdbadmin.client.service.BdbMemoryResourceHandle;
import com.moss.bdbadmin.client.service.ServiceException;
import com.moss.identity.tools.IdProover;
import com.moss.swing.LoadingDialog;

@SuppressWarnings("serial")
public final class EntryEditor extends EntryEditorView {
	public interface EditsListener {
		void deleted();
	}
	private final Component ancestor;
	private final BdbClient client;
	private final IdProover idFactory;
	private final String dbPath;
	private final Lock loadLock = new Lock();
	
	private EntryEditorComponent comp;

	private Context context;
	
	public EntryEditor(Component ancestor, IdProover idFactory, BdbClient client, String dbPath) {
		
		this.ancestor = ancestor;
		this.client = client;
		this.idFactory = idFactory;
		this.dbPath = dbPath;

		
		for (EditMode mode : EditMode.values()) {
			getFieldEditMode().addItem(mode);
		}
		getFieldEditMode().setSelectedItem(null);
		getFieldEditMode().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateEditor();
				update();
			}
		});
		
		getContainerPanel().setLayout(new BorderLayout());
		
		getButtonPut().addActionListener(new SaveListener());
		getButtonDelete().addActionListener(new DeleteListener());
		getButtonRefresh().addActionListener(new RefreshListener());
		getButtonPrevious().addActionListener(new PreviousListener());
		getButtonNext().addActionListener(new NextListener());

		
		update();
	}
	
	private final static String mimeType(byte[] data){
		String mimeType;
		{
			String t;
			try {
				MagicMatch m = Magic.getMagicMatch(data);
				t = m.getMimeType();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				t = "unknown";
			}
			mimeType = t;
		}
		
		if(mimeType.equals("text/plain") && data.length >= 100){
			StringBuffer first100Chars = new StringBuffer();
			for(int x=0;x<100;x++){
				first100Chars.append((char) data[x]);
			}
			if(first100Chars.toString().trim().startsWith("<?xml")){
				mimeType = "text/xml";
			}
		}
		return mimeType;
	}
	
	/**
	 * Meant to be executed by non-ui threads. 
	 */
	public void loadContext(byte[] key) throws Exception {
		loadContext(key, false);
	}
	
	/**
	 * Meant to be executed by non-ui threads. 
	 */
	public void loadContext(byte[] key, boolean insert) throws Exception {

		final Context context = new Context();
		context.insert = insert;
		context.key = key;
		context.dbInfo = client.dbInfo(idFactory.giveProof(), dbPath);
		context.resourcePath = dbPath + "/" + KeyFactory.encode(key);
		if (insert) {
			context.entryInfo = new EntryInfo();
			context.data = new byte[]{};
		}
		else {
			context.entryInfo = client.entryInfo(idFactory.giveProof(), context.resourcePath);
			context.data = ((BdbMemoryResourceHandle)client.get(idFactory.giveProof(), context.resourcePath)).data();
		}

		final String mimeType = mimeType(context.data);

		final String keyString = new String(key, "UTF8");
		
		EntryEditor.this.context = context;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				EditMode mode = (EditMode)getFieldEditMode().getSelectedItem();
				if (mode == null) {
					for (EditMode m : EditMode.values()) {
						if (m.supports(mimeType)) {
							mode = m;
							break;
						}
					}

					if (mode == null) {
						mode = EditMode.BINARY;
					}
					getFieldEditMode().setSelectedItem(mode);
				}
				
				updateEditor();
				
				getFieldKey().setText(keyString);
				getFieldStatusInfo().setText("Db contains " + context.dbInfo.getEntryCount() + " entries. This entry has a mime type of '" + mimeType + "'");
				
				update();
			}
		});
	}
	
	public void clearContext() {
		context = null;
		update();
	}

	
	private final class Context {
		boolean insert;
		byte[] key;
		byte[] data;
		String resourcePath;
		DbInfo dbInfo;
		EntryInfo entryInfo;	
	}
	
	private final class Lock  {
		
		boolean locked = false;
		
		synchronized boolean locked() {
			return locked;
		}
		
		synchronized void setLock(boolean locked) {
			this.locked = locked;
		}
	}
	
	private enum EditMode {
		XML() {
			public boolean supports(String mimeType) {
				return "text/xml".equals(mimeType);
			}
			public EntryEditorComponent editor() {
				return new XmlEditorComponent();
			}
		},
		PLAINTEXT() {
			public boolean supports(String mimeType) {
				return "text/plain".equals(mimeType);
			}
			public EntryEditorComponent editor() {
				return new PlainTextEditorComponent();
			}
		},
		BINARY() {
			public boolean supports(String mimeType) {
				return true;
			}
			public EntryEditorComponent editor() {
				return new BinaryEditorComponent();
			}
		};
		
		public abstract boolean supports(String mimeType);
		public abstract EntryEditorComponent editor();
	}
	
	private void updateEditor() {
		
		EditMode chosen = (EditMode)getFieldEditMode().getSelectedItem();
		
		System.out.println("CHOSEN: " + chosen);
		
		comp = chosen.editor();
		
		getContainerPanel().removeAll();
		getContainerPanel().add(comp.view(), BorderLayout.CENTER);
		getContainerPanel().invalidate();

		getContainerPanel().getParent().validate();
		getContainerPanel().getParent().repaint();
		
		if (context != null) {
			comp.setContent(context.data);
		}
	}
	
	private void update() {
		
		boolean hasContext = context != null;
		boolean contextRequiresInsert = hasContext && context.insert;
		
		getButtonPut().setEnabled(hasContext);
		getButtonDelete().setEnabled(hasContext && !contextRequiresInsert);
		getButtonRefresh().setEnabled(hasContext&& !contextRequiresInsert);
		getFieldEditMode().setEnabled(hasContext);
		
		boolean canPrevious = hasContext && context.entryInfo.getKeyPrevious() != null && !contextRequiresInsert;
		getButtonPrevious().setEnabled(canPrevious);
		
		boolean canNext = hasContext && context.entryInfo.getKeyNext() != null && !contextRequiresInsert;
		getButtonNext().setEnabled(canNext);
	}
	
	private final class SaveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			Runnable save = new Runnable() {
				public void run() {
					try {
						synchronized (loadLock) {
							if (loadLock.locked()) {
								System.out.println("Something is already happening, ignoring save request");
								return;
							}
							else {
								loadLock.setLock(true);
							}
						}
						
						LoadingDialog loading = new LoadingDialog(ancestor);
						try {
							loading.display("Saving Entry", "Saving entry");
							client.put(idFactory.giveProof(), context.resourcePath, new BdbMemoryResourceHandle(comp.getContent()));
							if (context.insert) {
								context.insert = false;
							}
							loadContext(context.key);
							loading.dispose();
						}
						catch (ServiceException ex) {
							loading.dispose();
							if (ex.responseCode() == 404) {
								JOptionPane.showMessageDialog(
										ancestor,
										"Entry no longer exists.",
										"Refresh Failed",
										JOptionPane.WARNING_MESSAGE
								);
							}
							else {
								ex.printStackTrace();
								JOptionPane.showMessageDialog(ancestor, "Failed to save entry");
							}
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to save entry");
						}
					}
					finally {
						loadLock.setLock(false);
					}
				}
			};
			Thread saveThread = new Thread(save, "SaveThread");
			saveThread.start();
		}
	}
	
	private final class DeleteListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			Runnable delete = new Runnable() {
				public void run() {
					try {
						synchronized (loadLock) {
							if (loadLock.locked()) {
								System.out.println("Something is already happening, ignoring delete request");
								return;
							}
							else {
								loadLock.setLock(true);
							}
						}
						LoadingDialog loading =new LoadingDialog(ancestor);
						try {
							loading.display("Deleting Entry", "Deleting entry");
							client.delete(idFactory.giveProof(), context.resourcePath);
							loading.dispose();
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to delete entry");
						}
					}
					finally {
						loadLock.setLock(false);
					}
				}
			};
			Thread deleteThread = new Thread(delete, "DeleteThread");
			deleteThread.start();
		}
	}
	
	private final class RefreshListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			Runnable refresh = new Runnable() {
				public void run() {
					try {
						synchronized (loadLock) {
							if (loadLock.locked()) {
								System.out.println("Something is already happening, ignoring refresh request");
								return;
							}
							else {
								loadLock.setLock(true);
							}
						}
						LoadingDialog loading =new LoadingDialog(ancestor);
						try {
							loading.display("Refreshing Entry", "Refreshing entry");

							try {
								loadContext(context.key);
							}
							catch (ServiceException ex) {
								loading.dispose();
								if (ex.responseCode() == 404) {
									JOptionPane.showMessageDialog(
											ancestor,
											"Entry no longer exists.",
											"Refresh Failed",
											JOptionPane.WARNING_MESSAGE
									);
								}
								else throw ex;
							}
							loading.dispose();
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to refresh entry");
						}
					}
					finally {
						loadLock.setLock(false);
					}
				}
			};
			Thread refreshThread = new Thread(refresh, "RefreshThread");
			refreshThread.start();
		}
	}
	
	private final class PreviousListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			Runnable previous = new Runnable() {
				public void run() {
					try {
						synchronized (loadLock) {
							if (loadLock.locked()) {
								System.out.println("Something is already happening, ignoring previous request");
								return;
							}
							else {
								loadLock.setLock(true);
							}
						}
						LoadingDialog loading =new LoadingDialog(ancestor);
						try {
							loading.display("Loading Previous Entry", "Loading previous entry");

							try {
								loadContext(context.entryInfo.getKeyPrevious());
							}
							catch (ServiceException ex) {
								loading.dispose();
								if (ex.responseCode() == 404) {
									JOptionPane.showMessageDialog(
											ancestor,
											"Entry no longer exists.",
											"Loading Previous Entry Failed",
											JOptionPane.WARNING_MESSAGE
									);
								}
								else throw ex;
							}
							loading.dispose();
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to load previous entry");
						}
					}
					finally {
						loadLock.setLock(false);
					}
				}
			};
			Thread previousThread = new Thread(previous, "PreviousThread");
			previousThread.start();
		}
	}
	
	private final class NextListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			Runnable next = new Runnable() {
				public void run() {
					try {
						synchronized (loadLock) {
							if (loadLock.locked()) {
								System.out.println("Something is already happening, ignoring next request");
								return;
							}
							else {
								loadLock.setLock(true);
							}
						}
						LoadingDialog loading =new LoadingDialog(ancestor);
						try {
							loading.display("Loading Next Entry", "Loading next entry");

							try {
								loadContext(context.entryInfo.getKeyNext());
							}
							catch (ServiceException ex) {
								loading.dispose();
								if (ex.responseCode() == 404) {
									JOptionPane.showMessageDialog(
											ancestor,
											"Entry no longer exists.",
											"Loading Next Entry Failed",
											JOptionPane.WARNING_MESSAGE
									);
								}
								else throw ex;
							}
							loading.dispose();
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to load next entry");
						}
					}
					finally {
						loadLock.setLock(false);
					}
				}
			};
			Thread nextThread = new Thread(next, "NextThread");
			nextThread.start();
		}
	}
}

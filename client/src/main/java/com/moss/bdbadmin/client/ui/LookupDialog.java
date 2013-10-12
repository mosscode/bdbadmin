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
package com.moss.bdbadmin.client.ui;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.moss.bdbadmin.api.util.KeyFactory;
import com.moss.bdbadmin.client.service.BdbClient;
import com.moss.identity.tools.IdProover;

/**
 * Need to add 'head' support for this, to return 'exists|notexists' sorts of feedback. 
 */
@SuppressWarnings("serial")
final class LookupDialog extends LookupDialogView {

	private final BdbClient client;
	private final IdProover idFactory;
	private final JDialog dialog;
	private final String dbPath;
	private final LookupDialogSemantic semantic;
	
	private boolean cancelled = true;
	private String key;
	
	private final Object lock = new Object();
	private boolean lockedLookupInProgress;
	private String lockedSubsequentLookup;
	private boolean lockedFound;
	private boolean swingInProgress;
	private boolean swingFound;
	
	public LookupDialog(Component ancestor, BdbClient client, String dbPath, IdProover idFactory, LookupDialogSemantic semantic) {
		super();
		this.client = client;
		this.idFactory = idFactory;
		this.dbPath = dbPath;
		this.semantic = semantic;
		
		if (ancestor instanceof JFrame) {
			dialog = new JDialog((JFrame)ancestor);
		}
		else if (ancestor instanceof JDialog) {
			dialog = new JDialog((JDialog)ancestor);
		}
		else if (ancestor == null) {
			dialog = new JDialog();
		}
		else {
			throw new RuntimeException();
		}
		
		String title;
		switch (semantic) {
		case FIND:
			title = "Query DB";
			getButtonOpen().setText("Open");
			break;
		case INSERT:
			title = "New Key";
			getButtonOpen().setText("Insert");
			break;
		default:
			throw new RuntimeException();
		}
		
		getFieldQueryPath().setText(client.url() + dbPath);
		
		getFieldKey().getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				changed();
			}
			public void insertUpdate(DocumentEvent e) {
				changed();
			}
			public void removeUpdate(DocumentEvent e) {
				changed();
			}
		});
		
		getButtonCancel().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		
		getButtonOpen().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelled = false;
				try {
					key = getFieldKey().getText().trim();
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				dialog.dispose();
			}
		});
		
		dialog.setTitle(title);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setModal(true);
		dialog.getContentPane().add(this);
		dialog.pack();
		dialog.setLocationRelativeTo(ancestor);
		
		update();
		
		try {
			Object clipboardContent = getToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			if(clipboardContent!=null){
				getFieldKey().setText(clipboardContent.toString().trim());
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public void display() {
		dialog.setVisible(true);
	}
	
	public boolean cancelled() {
		return cancelled;
	}
	
	public String key() {
		return key;
	}
	
	private final class LookupWorker implements Runnable {

		private final String key;

		public LookupWorker(String key) {
			this.key = key;
		}

		public void run() {
			
			try {
				System.out.println("Evaluating " + key);
				String resourcePath = dbPath + "/" + KeyFactory.encode(key.getBytes("UTF8"));
				client.head(idFactory.giveProof(), resourcePath);
				System.out.println("Resource exists");
				lockedFound = true;
			}
			catch (Exception ex) {
				System.out.println("Resource not found");
				ex.printStackTrace();
				lockedFound = false;
			}
			
			synchronized (lock) {
				if (lockedSubsequentLookup != null) {
					Runnable r = new LookupWorker(lockedSubsequentLookup);
					Thread t = new Thread(r, "LookupWorkerThread-" + lockedSubsequentLookup);
					lockedSubsequentLookup = null;
					t.start();
				}
				else {
					lockedLookupInProgress = false;
				}
				
				SwingUtilities.invokeLater(new Runnable() {
					
					final boolean inProgress = LookupDialog.this.lockedLookupInProgress;
					final boolean found = LookupDialog.this.lockedFound;
					
					public void run() {
						swingInProgress = inProgress;
						swingFound = found;
						update();
					}
				});
			}
		}
	}

	public void changed() {

		String key = getFieldKey().getText().trim();

		synchronized (lock) {
		
			if (!lockedLookupInProgress) {
				Runnable r = new LookupWorker(key);
				Thread t = new Thread(r, "LookupWorkerThread-" + key);
				t.start();
				lockedLookupInProgress = true;
			}
			else {
				lockedSubsequentLookup = key;
			}
		}
	}
	
	private void update() {
		
		String key = getFieldKey().getText().trim();
		String status = null;
		boolean canContinue = false;
		
		if (key.length() > 0) {
			
			switch (semantic) {
			case FIND:
			{
				if (swingInProgress) {
					status = "Looking up Entry...";
				}
				else if (swingFound) {
					canContinue = true;
					status = "Entry Found";
				}
				else {
					status = "Entry Not Found";
				}
				break;
			}
			case INSERT:
			{
				if (swingInProgress) {
					status = "Looking up Entry...";
				}
				else if (swingFound) {
					canContinue = false;
					status = "Entry Exists";
				}
				else {
					status = "Entry Not Found";
					canContinue = true;
				}
				break;
			}
			default:
				throw new RuntimeException();
			}
		}
		
		getFieldSearchStatus().setText(status == null ? " " : status);
		getButtonOpen().setEnabled(canContinue);
	}
}

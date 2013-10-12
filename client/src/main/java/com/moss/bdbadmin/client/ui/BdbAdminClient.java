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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.sf.jmimemagic.Magic;

import org.apache.commons.httpclient.HttpClient;

import com.moss.bdbadmin.api.db.DbInfo;
import com.moss.bdbadmin.api.map.Category;
import com.moss.bdbadmin.api.map.Db;
import com.moss.bdbadmin.api.map.DbVisitor;
import com.moss.bdbadmin.api.map.Env;
import com.moss.bdbadmin.api.map.PrimaryDb;
import com.moss.bdbadmin.api.map.SecondaryDb;
import com.moss.bdbadmin.client.service.BdbClient;
import com.moss.bdbadmin.client.ui.config.ServiceConfig;
import com.moss.bdbadmin.client.ui.config.ServiceInfo;
import com.moss.bdbadmin.client.ui.editor.EntryEditor;
import com.moss.identity.standard.Password;
import com.moss.identity.tools.IdProover;
import com.moss.identity.tools.IdTool;
import com.moss.identity.veracity.VeracityId;
import com.moss.identity.veracity.VeracityIdToolPlugin;
import com.moss.identity.veracity.VeracityProxyFactory;
import com.moss.rpcutil.proxy.ProxyFactory;
import com.moss.swing.LoadingDialog;

@SuppressWarnings("serial")
public final class BdbAdminClient extends BdbAdminClientView {
	
	private final HttpClient httpClient;
	private final Component ancestor;
	private final File configPath;
	private final ProxyFactory proxyFactory;
	
	private final JAXBContext configJaxbContext;
	
	private final DefaultMutableTreeNode root;
	private final DefaultTreeModel model;
	
	public BdbAdminClient(HttpClient httpClient, JFrame window, File configPath, ProxyFactory proxyFactory) {
		
		this.httpClient = httpClient;
		this.ancestor = window;
		this.configPath = configPath;
		this.proxyFactory = proxyFactory;
		
		try {
			configJaxbContext = JAXBContext.newInstance(ServiceConfig.class);
		}
		catch (JAXBException ex) {
			throw new RuntimeException(ex);
		}
		
		JMenuItem newServiceItem = new JMenuItem("New Service");
		newServiceItem.setAction(new NewServiceAction());
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(newServiceItem);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		
		window.setJMenuBar(menuBar);
		window.setIconImage(new ImageIcon(this.getClass().getClassLoader().getResource("service.gif")).getImage());
		
		this.root = new DefaultMutableTreeNode();
		this.model = new DefaultTreeModel(root);
		getTree().setModel(model);
		getTree().setShowsRootHandles(true);
		getTree().setRootVisible(false);
		getTree().setCellRenderer(new Renderer());
		getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		getTree().getSelectionModel().addTreeSelectionListener(new SelectionListener());
		getTree().addMouseListener(new ContextMenuHandler());

		addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent event) {
				loadConfig();
			}
			public void ancestorMoved(AncestorEvent event) {}
			public void ancestorRemoved(AncestorEvent event) {}
		});
	}
	
	private class ContextMenuHandler extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			
			if (e.getButton() != 3) {
				return;
			}
			
			TreePath treePath = getTree().getSelectionPath();
			
			if (treePath == null) {
				return;
			}
			
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			Object o = node.getUserObject();
			
			JPopupMenu menu = null;
			
			if (o instanceof ServiceContainer) {
				
				JMenuItem refreshItem = new JMenuItem(new RefreshServiceAction());
				JMenuItem editItem = new JMenuItem(new EditServiceAction());
				JMenuItem deleteItem = new JMenuItem(new DeleteServiceAction());
				
				menu = new JPopupMenu();
				menu.add(refreshItem);
				menu.add(editItem);
				menu.add(deleteItem);
			}
			else if (o instanceof Db) {
				
				JMenuItem lookupItem = new JMenuItem();
				lookupItem.setAction(new LookupAction());
				
				JMenuItem browseItem = new JMenuItem(new BrowseAction());
				
				JMenuItem insertItem = new JMenuItem(new InsertAction());
				
				JMenuItem clearItem = new JMenuItem(new ClearAction());
				
				menu = new JPopupMenu();
				menu.add(lookupItem);
				menu.add(browseItem);
				menu.add(insertItem);
				menu.add(clearItem);
			}
			
			if (menu != null) {
				menu.show(getTree(), e.getX(), e.getY());
			}
		}
	}
	
	private void loadConfig() {
		
		if (!configPath.exists()) {
			return;
		}
		
		root.removeAllChildren();
		model.nodeStructureChanged(root);
		
			Runnable load = new Runnable() {
				public void run() {
					
					LoadingDialog loading = new LoadingDialog(ancestor);
					try {
						loading.display("Loading Config", "Loading configuration details.");
						
						Unmarshaller u = configJaxbContext.createUnmarshaller();
						ServiceConfig config = (ServiceConfig)u.unmarshal(configPath);
						
						for (ServiceInfo info : config.services()) {
							
							final ServiceContainer container = new ServiceContainer();
							container.setInfo(info);
							
							boolean success;
							try {
								refresh(container);
								success = true;
							}
							catch (Exception ex) {
								ex.printStackTrace();
								JOptionPane.showMessageDialog(
									ancestor, 
									"Could not refresh service " + info.getUrl(),
									"Service Refresh Failed",
									JOptionPane.ERROR_MESSAGE
								);
								success = false;
							}
							
							class UpdateUI implements Runnable {
								
								private final boolean success;
	
								public UpdateUI(boolean success) {
									this.success = success;
								}
	
								public void run() {
									
									DefaultMutableTreeNode serviceNode = new DefaultMutableTreeNode(container);
									root.add(serviceNode);
									model.nodeStructureChanged(root);
//									model.nodesWereInserted(root, new int[]{ root.getIndex(serviceNode) });
									
									if (success) {
										rebuild(container);
									}
								}
							}
							
							SwingUtilities.invokeLater(new UpdateUI(success));
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(
							ancestor, 
							"Could not load configuration from file: " + configPath,
							"Config Load Failed",
							JOptionPane.ERROR_MESSAGE
						);
					}
					finally {
						loading.dispose();
					}
				}
			};
			
			Thread loaderThread = new Thread(load, "LoaderThread");
			loaderThread.start();
	}
	
	private void refresh(ServiceContainer service) throws Exception {
		
		ServiceInfo info = service.getInfo();
		
		IdTool agent = new IdTool();
		agent.addHandler(new VeracityIdToolPlugin(proxyFactory));
		IdProover idFactory = agent.getFactory(new VeracityId(info.getIdentity()), new Password(info.getPassword()));
		idFactory.giveProof();
		
		BdbClient client = new BdbClient(httpClient, info.getUrl());
		
		Category rootCategory = client.map(idFactory.giveProof());
		sort(rootCategory);
		
		service.setIdFactory(idFactory);
		service.setClient(client);
		service.setRootCategory(rootCategory);
	}
	
	private void rebuild(ServiceContainer container) {
		
		DefaultMutableTreeNode serviceNode = null;
		for (int i=0; i<root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getChildAt(i);
			ServiceContainer thisContainer = (ServiceContainer) node.getUserObject();
			if (container == thisContainer) {
				serviceNode = node;
				break;
			}
		}
		
		if (serviceNode == null) {
			throw new RuntimeException("Cannot find container in tree");
		}
		
		Category rootCategory = container.getRootCategory();
		
		if (rootCategory == null) {
			throw new NullPointerException();
		}
		
		serviceNode.removeAllChildren();
		buildCategory(serviceNode, rootCategory);
		
		model.nodeStructureChanged(serviceNode);
	}
	
	private void buildCategory(DefaultMutableTreeNode catNode, Category cat) {
		
		for (Category childCat : cat.categories()) {
			
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childCat);
			catNode.add(childNode);
			
			buildCategory(childNode, childCat);
		}
		
		for (Env env : cat.environments()) {
			
			final DefaultMutableTreeNode envNode = new DefaultMutableTreeNode(env);
			catNode.add(envNode);
			
			Map<String, DefaultMutableTreeNode> primaryNameNodeMap = new HashMap<String, DefaultMutableTreeNode>();
			for (final Db db : env.databases()) {
				
				PrimaryDb primary = db.acceptVisitor(new DbVisitor<PrimaryDb>() {
					public PrimaryDb visit(PrimaryDb primary) {
						return primary;
					}
					public PrimaryDb visit(SecondaryDb secondary) {
						return null;
					}
				});
				
				if (primary != null) {
					DefaultMutableTreeNode primaryNode = new DefaultMutableTreeNode(primary);
					envNode.add(primaryNode);
					primaryNameNodeMap.put(primary.getName(), primaryNode);
				}
			}
			
			for (final Db db : env.databases()) {
				
				SecondaryDb secondary = db.acceptVisitor(new DbVisitor<SecondaryDb>() {
					public SecondaryDb visit(PrimaryDb primary) {
						return null;
					}
					public SecondaryDb visit(SecondaryDb secondary) {
						return secondary;
					}
				});
				
				if (secondary != null) {
					DefaultMutableTreeNode secondaryNode = new DefaultMutableTreeNode(secondary);
					DefaultMutableTreeNode primaryNode = primaryNameNodeMap.get(secondary.getPrimaryName());
					primaryNode.add(secondaryNode);
				}
			}
		}
	}
	
	private void saveConfig() {
		try {
			ServiceConfig config = new ServiceConfig();
			
			for (int i=0; i<root.getChildCount(); i++) {
				DefaultMutableTreeNode serviceNode = (DefaultMutableTreeNode)root.getChildAt(i);
				ServiceContainer container = (ServiceContainer) serviceNode.getUserObject();
				config.services().add(container.getInfo());
			}
			
			Marshaller m = configJaxbContext.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(config, new FileOutputStream(configPath));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(
				ancestor, 
				"Could not save configuration from file: " + configPath, 
				"Config Save Failed", 
				JOptionPane.ERROR_MESSAGE
			);
		}
	}
	
	private final class NewServiceAction extends AbstractAction {
		
		public NewServiceAction() {
			super("New Service");
		}

		public void actionPerformed(ActionEvent e) {
			
			Runnable load = new Runnable() {
				public void run() {
					
					final ServiceInfo info = new ServiceInfo();
					
					do {
						ServiceEditor editor = new ServiceEditor("Add new Service", ancestor, info);
						editor.display();
					
						if (editor.cancelled()) {
							return;
						}
					}
					while (!load(info));
					
					saveConfig();
				}
				
				private boolean load(ServiceInfo info) {
					
					LoadingDialog loading = new LoadingDialog(ancestor);
					try {
						loading.display("Loading Service", "Loading service details.");
						
						final ServiceContainer container = new ServiceContainer();
						container.setInfo(info);

						boolean success;
						try {
							refresh(container);
							success = true;
						}
						catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(
									ancestor, 
									"Could not refresh service " + info.getUrl(), 
									"Service Refresh Failed", 
									JOptionPane.ERROR_MESSAGE
							);
							success = false;
						}
							
						class UpdateUI implements Runnable {

							private final boolean success;

							public UpdateUI(boolean success) {
								this.success = success;
							}

							public void run() {

								DefaultMutableTreeNode serviceNode = new DefaultMutableTreeNode(container);
								root.add(serviceNode);
								model.nodeStructureChanged(root);
//								model.nodesWereInserted(root, new int[]{ root.getIndex(serviceNode) });

								if (success) {
									rebuild(container);
								}
							}
						}
							
						SwingUtilities.invokeLater(new UpdateUI(success));
						
						return success;
					}
					finally {
						loading.dispose();
					}
				}
			};
			
			Thread loaderThread = new Thread(load, "LoaderThread");
			loaderThread.start();
		}
	}
	
	private class RefreshServiceAction extends AbstractAction {
		public RefreshServiceAction() {
			super("Refresh");
		}

		public void actionPerformed(ActionEvent e) {
			
			TreePath treePath = getTree().getSelectionPath();
			final DefaultMutableTreeNode serviceNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			
			// make sure it really is a container that's selected
			Object o = serviceNode.getUserObject();
			final ServiceContainer container = (ServiceContainer)o;
			
			Runnable load = new Runnable() {
				public void run() {
					
					LoadingDialog loading = new LoadingDialog(ancestor);
					try {
						loading.display("Refreshing Service", "Refreshing service details.");
						
						boolean success;
						try {
							refresh(container);
							success = true;
						}
						catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(
									ancestor, 
									"Could not refresh service " + container.getInfo().getUrl(), 
									"Service Refresh Failed", 
									JOptionPane.ERROR_MESSAGE
							);
							success = false;
						}
							
						if (success) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									rebuild(container);
								}
							});
						}
					}
					finally {
						loading.dispose();
					}
				}
			};
			
			Thread loaderThread = new Thread(load, "RefresherThread");
			loaderThread.start();
		}
	}
	
	private class EditServiceAction extends AbstractAction {
		public EditServiceAction() {
			super("Edit");
		}

		public void actionPerformed(ActionEvent e) {
			
			TreePath treePath = getTree().getSelectionPath();
			final DefaultMutableTreeNode serviceNode = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			
			// make sure it really is a container that's selected
			Object o = serviceNode.getUserObject();
			final ServiceContainer container = (ServiceContainer)o;
			
			Runnable load = new Runnable() {
				public void run() {
					
					ServiceInfo info = new ServiceInfo();
					info.setUrl(container.getInfo().getUrl());
					info.setIdentity(container.getInfo().getIdentity());
					info.setPassword(container.getInfo().getPassword());
					
					do {
						ServiceEditor editor = new ServiceEditor("Edit Service", ancestor, info);
						editor.display();
					
						if (editor.cancelled()) {
							return;
						}
					}
					while (!update(info));
					
					container.setInfo(info);
					saveConfig();
				}
				
				private boolean update(ServiceInfo info) {
					
					LoadingDialog loading = new LoadingDialog(ancestor);
					try {
						loading.display("Updating Service", "Updating service details.");
						
						boolean success;
						try {
							refresh(container);
							success = true;
						}
						catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(
									ancestor, 
									"Could not load service " + info.getUrl(), 
									"Service Update Failed", 
									JOptionPane.ERROR_MESSAGE
							);
							success = false;
						}
							
						if (success) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									rebuild(container);
								}
							});
						}
						
						return success;
					}
					finally {
						loading.dispose();
					}
				}
			};
			
			Thread loaderThread = new Thread(load, "EditThread");
			loaderThread.start();
		}
	}
	
	private class DeleteServiceAction extends AbstractAction {
		public DeleteServiceAction() {
			super("Delete");
		}

		public void actionPerformed(ActionEvent e) {
			
			TreePath treePath = getTree().getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			
			// make sure it really is a container that's selected
			Object o = node.getUserObject();
			if (! (o instanceof ServiceContainer)) {
				throw new RuntimeException();
			}
			
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
			int oldIndex = parent.getIndex(node);
			parent.remove(node);
			
			saveConfig();
			
			model.nodesWereRemoved(parent, new int[]{ oldIndex }, new Object[]{ node });
		}
	}
	
	private class LookupAction extends AbstractAction {
		public LookupAction() {
			super("Lookup");
		}

		public void actionPerformed(ActionEvent e) {
		
			TreePath treePath = getTree().getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			Object[] userObjectPath = node.getUserObjectPath();
			
			List<Object> path = new ArrayList<Object>(Arrays.asList(userObjectPath));
			
			path.remove(0); // remove root
			
			// remove primary db from secondary db path (flatten the path out to match what the server expects)
			if (path.get(path.size() - 1) instanceof SecondaryDb) {
				path.remove(path.remove(path.size() - 2)); 
			}
			
			final ServiceContainer container = (ServiceContainer)path.remove(0);

			final String dbPath;
			{
				String p = "";
				for (Object o : path) {
					String pathElement = MapUtil.describe(o);
					try {
						p = p + "/" + URLEncoder.encode(pathElement, "UTF8");
					}
					catch (UnsupportedEncodingException ex) {
						throw new RuntimeException(ex);
					}
				}
				
				dbPath = p;
			}
			
			final LookupDialog dialog = new LookupDialog(ancestor, container.getClient(), dbPath, container.getIdFactory(), LookupDialogSemantic.FIND);
			dialog.display();
			
			if (!dialog.cancelled()) {
				
				Runnable load = new Runnable() {
					public void run() {
						LoadingDialog loading = new LoadingDialog(ancestor);
						try {
							loading.display("Loading Entry", "Loading entry " + dialog.key());
							EntryEditor editor = new EntryEditor(ancestor, container.getIdFactory(), container.getClient(), dbPath);
							editor.loadContext(dialog.key().getBytes("UTF8"));
							loading.dispose();
							showInDialog(container.getClient().url() + dbPath, editor);
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to load entry: " + dialog.key());
						}
					}
				};
				Thread loadThread = new Thread(load, "LoadThread");
				loadThread.start();
			}
		}
	}
	
	private final class InsertAction extends AbstractAction {
		public InsertAction() {
			super("Insert");
		}

		public void actionPerformed(ActionEvent e) {
		
			TreePath treePath = getTree().getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			Object[] userObjectPath = node.getUserObjectPath();
			
			List<Object> path = new ArrayList<Object>(Arrays.asList(userObjectPath));
			
			path.remove(0); // remove root
			
			// remove primary db from secondary db path (flatten the path out to match what the server expects)
			if (path.get(path.size() - 1) instanceof SecondaryDb) {
				path.remove(path.remove(path.size() - 2)); 
			}
			
			final ServiceContainer container = (ServiceContainer)path.remove(0);

			final String dbPath;
			{
				String p = "";
				for (Object o : path) {
					String pathElement = MapUtil.describe(o);
					try {
						p = p + "/" + URLEncoder.encode(pathElement, "UTF8");
					}
					catch (UnsupportedEncodingException ex) {
						throw new RuntimeException(ex);
					}
				}
				
				dbPath = p;
			}
			
			final LookupDialog lookup = new LookupDialog(ancestor, container.getClient(), dbPath, container.getIdFactory(), LookupDialogSemantic.INSERT);
			lookup.display();
			
			if (!lookup.cancelled()) {
				
				Runnable load = new Runnable() {
					public void run() {
						LoadingDialog loading = new LoadingDialog(ancestor);
						try {
							loading.display("Loading Db Info", "Loading Db Info");
							EntryEditor editor = new EntryEditor(ancestor, container.getIdFactory(), container.getClient(), dbPath);
							editor.loadContext(lookup.key().getBytes("UTF8"), true);
							loading.dispose();
							showInDialog(container.getClient().url() + dbPath, editor);
						}
						catch (Exception ex) {
							loading.dispose();
							ex.printStackTrace();
							JOptionPane.showMessageDialog(ancestor, "Failed to load entry: " + lookup.key());
						}
					}
				};
				Thread loadThread = new Thread(load, "LoadThread");
				loadThread.start();
			}
		}
	}
	
	private final class ClearAction extends AbstractAction {
		public ClearAction() {
			super("Clear");
		}

		public void actionPerformed(ActionEvent e) {
		
			TreePath treePath = getTree().getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			Object[] userObjectPath = node.getUserObjectPath();
			
			List<Object> path = new ArrayList<Object>(Arrays.asList(userObjectPath));
			
			path.remove(0); // remove root
			
			// remove primary db from secondary db path (flatten the path out to match what the server expects)
			if (path.get(path.size() - 1) instanceof SecondaryDb) {
				path.remove(path.remove(path.size() - 2)); 
			}
			
			final ServiceContainer container = (ServiceContainer)path.remove(0);

			final String dbPath;
			{
				String p = "";
				for (Object o : path) {
					String pathElement = MapUtil.describe(o);
					try {
						p = p + "/" + URLEncoder.encode(pathElement, "UTF8");
					}
					catch (UnsupportedEncodingException ex) {
						throw new RuntimeException(ex);
					}
				}
				
				dbPath = p;
			}
			
			int selected = JOptionPane.showConfirmDialog(ancestor, "Are you sure you want to clear the entire database?", "Warning", JOptionPane.OK_CANCEL_OPTION);
			if (selected != JOptionPane.OK_OPTION) {
				return;
			}

			Runnable load = new Runnable() {
				public void run() {
					LoadingDialog loading = new LoadingDialog(ancestor);
					try {
						loading.display("Clearing Database Entries", "Clearing all database entries from " + dbPath);
						
						container.getClient().delete(container.getIdFactory().giveProof(), dbPath);
						
						loading.dispose();
					}
					catch (Exception ex) {
						loading.dispose();
						ex.printStackTrace();
						JOptionPane.showMessageDialog(ancestor, "Failed to clear entries from : " + dbPath);
					}
				}
			};
			Thread loadThread = new Thread(load, "LoadThread");
			loadThread.start();
		}
	}
	
	private void showInDialog(String title, Component ancestor){

//		final JDialog dialog;
		
//		if (ancestor instanceof JFrame) {
//			dialog = new JDialog((JFrame)ancestor);
//		}
//		else if (ancestor instanceof JDialog) {
//			dialog = new JDialog((JDialog)ancestor);
//		}
//		else if (ancestor == null) {
//			dialog = new JDialog();
//		}
//		else {
//			throw new RuntimeException();
//		}
		
		JInternalFrame dialog = new JInternalFrame(title);
		
//		dialog.setTitle(client.url() + dbPath);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//		dialog.setModal(false);
		dialog.getContentPane().add(ancestor);
		dialog.setSize(400, 300);
//		dialog.setLocationRelativeTo(ancestor);
		dialog.setResizable(true);
		dialog.setClosable(true);
		dialog.setIconifiable(true);
		dialog.setMaximizable(true);
		getDesktopPane().add(dialog);
		try {
			dialog.setMaximum(true);
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}
		dialog.setVisible(true);
	}
	
	private final class BrowseAction extends AbstractAction {
		public BrowseAction() {
			super("Browse");
		}

		public void actionPerformed(ActionEvent e) {
		
			TreePath treePath = getTree().getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
			Object[] userObjectPath = node.getUserObjectPath();
			
			List<Object> path = new ArrayList<Object>(Arrays.asList(userObjectPath));
			
			path.remove(0); // remove root
			
			// remove primary db from secondary db path (flatten the path out to match what the server expects)
			if (path.get(path.size() - 1) instanceof SecondaryDb) {
				path.remove(path.remove(path.size() - 2)); 
			}
			
			final ServiceContainer container = (ServiceContainer)path.remove(0);

			final String dbPath;
			{
				String p = "";
				for (Object o : path) {
					String pathElement = MapUtil.describe(o);
					try {
						p = p + "/" + URLEncoder.encode(pathElement, "UTF8");
					}
					catch (UnsupportedEncodingException ex) {
						throw new RuntimeException(ex);
					}
				}
				
				dbPath = p;
			}
			
			Runnable browse = new Runnable() {
				public void run() {
					LoadingDialog loading = new LoadingDialog(ancestor);
					try {
						loading.display("Loading First Entry", "Loading first entry");
						DbInfo info = container.getClient().dbInfo(container.getIdFactory().giveProof(), dbPath);
						
						if (info.getEntryCount() == 0) {
							loading.dispose();
							JOptionPane.showMessageDialog(
								ancestor, 
								"This database appears to be empty.", 
								"No Entries Found", 
								JOptionPane.INFORMATION_MESSAGE
							);
							return;
						}
						
						EntryEditor editor = new EntryEditor(ancestor, container.getIdFactory(), container.getClient(), dbPath);
						editor.loadContext(info.getFirstKey());
						loading.dispose();
						showInDialog(container.getClient().url() + dbPath, editor);
					}
					catch (Exception ex) {
						loading.dispose();
						ex.printStackTrace();
						JOptionPane.showMessageDialog(ancestor, "Failed to load first database entry");
					}
				}
			};
			Thread browseThread = new Thread(browse, "BrowseThread");
			browseThread.start();
		}
	}
	
	private final class SelectionListener implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			
			TreePath path = e.getPath();
			
			if (path == null) {
				
			}
			else {
				
			}
		}
	}
	
	private final class Renderer extends DefaultTreeCellRenderer {
		
		private final Icon service, category, environment, primary, secondary;
		
		public Renderer() {
			ClassLoader cl = this.getClass().getClassLoader();
			service = new ImageIcon(cl.getResource("service.gif"));
			category = new ImageIcon(cl.getResource("category.gif"));
			environment = new ImageIcon(cl.getResource("environment.gif"));
			primary = new ImageIcon(cl.getResource("primary.gif"));
			secondary = new ImageIcon(cl.getResource("secondary.gif"));
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			
			if (value instanceof DefaultMutableTreeNode) {
				
				Object o = ((DefaultMutableTreeNode)value).getUserObject();
				
				String text = MapUtil.describe(o);
				Icon icon = null;
				if (o instanceof ServiceContainer) {
					icon = service;
				}
				else if (o instanceof Category) {
					icon = category;
				}
				else if (o instanceof Env) {
					icon = environment;
				}
				else if (o instanceof PrimaryDb) {
					icon = primary;
				}
				else if (o instanceof SecondaryDb) {
					icon = secondary;
				}
				
				setText(text);
				setIcon(icon);
			}
			
			return this;
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		Thread t = new Thread() {
			public void run()  {
				try {
					Magic.initialize();
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		HttpClient httpClient = new HttpClient();
		BdbClient.init();
		
		JFrame frame = new JFrame();
		
		ProxyFactory proxyFactory = VeracityProxyFactory.create();
		
		BdbAdminClient client = new BdbAdminClient(httpClient, frame, new File("config.xml"), proxyFactory);
		
		frame.setTitle("Bdb Network Explorer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(client);
		frame.setSize(1024, 768);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private void sort(Category category) {
		
		final Comparator<Category> catComp = new Comparator<Category>() {
			public int compare(Category o1, Category o2) {
				String a = o1.getName().toLowerCase();
				String b = o2.getName().toLowerCase();
				int result = a.compareTo(b);
				return result;
			}
		};
		
		final Comparator<Env> envComp = new Comparator<Env>() {
			public int compare(Env o1, Env o2) {
				String a = o1.getName().toLowerCase();
				String b = o2.getName().toLowerCase();
				int result = a.compareTo(b);
				return result;
			}
		};
		
		final Comparator<Db> dbComp = new Comparator<Db>() {
			public int compare(Db o1, Db o2) {
				String a = o1.getName().toLowerCase();
				String b = o2.getName().toLowerCase();
				int result = a.compareTo(b);
				return result;
			}
		};
		
		Collections.sort(category.categories(), catComp);
		for (Category cat : category.categories()) {
			sort(cat);
		}
		
		Collections.sort(category.environments(), envComp);
		
		for (Env env : category.environments()) {
			Collections.sort(env.databases(), dbComp);
		}
	}
}

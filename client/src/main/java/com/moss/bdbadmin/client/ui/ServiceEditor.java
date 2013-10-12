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
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.moss.bdbadmin.client.ui.config.ServiceInfo;
import com.moss.veracity.api.util.NameParser;

/**
 * Need to add 'head' support for this, to return 'exists|notexists' sorts of feedback. 
 */
@SuppressWarnings("serial")
final class ServiceEditor extends ServiceEditorView {

	private final JDialog dialog;
	
	private boolean cancelled = true;
	private ServiceInfo info;
	
	public ServiceEditor(String title, Component ancestor, ServiceInfo i) {
		this.info = i;
		
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
		
		getButtonCancel().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		
		getButtonOk().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					info.setUrl(getFieldUrl().getText().trim());
					info.setIdentity(getFieldIdentity().getText().trim());
					info.setPassword(new String(getFieldPassword().getPassword()));
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				cancelled = false;
				dialog.dispose();
			}
		});
		
		getFieldUrl().setText(info.getUrl());
		getFieldIdentity().setText(info.getIdentity());
		getFieldPassword().setText(info.getPassword());
		
		DocumentListener changeListener = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				update();
			}
			public void insertUpdate(DocumentEvent e) {
				update();
			}
			public void removeUpdate(DocumentEvent e) {
				update();
			}
		};
		getFieldUrl().getDocument().addDocumentListener(changeListener);
		getFieldIdentity().getDocument().addDocumentListener(changeListener);
		getFieldPassword().getDocument().addDocumentListener(changeListener);
		
		dialog.setTitle(title);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setModal(true);
		dialog.getContentPane().add(this);
		dialog.pack();
		dialog.setLocationRelativeTo(ancestor);
		
		update();
	}
	
	public void display() {
		dialog.setVisible(true);
	}
	
	public boolean cancelled() {
		return cancelled;
	}
	
	private void update() {
		
		String validationState = null;
		
		String url = getFieldUrl().getText().trim();
		String identity = getFieldIdentity().getText().trim();
		String password = new String(getFieldPassword().getPassword());
		
		if (url.length() == 0) {
			validationState = "URL must not be empty.";
			getLabelUrl().setFlagged(true);
		}
		else if (!isValid(url)) {
			validationState = "URL is not valid.";
			getLabelUrl().setFlagged(true);
		}
		else {
			getLabelUrl().setFlagged(false);	
		}
		
		if (validationState == null && identity.length() == 0) {
			validationState = "Identity must not be empty.";
			getLabelIdentity().setFlagged(true);
		}
		else if (validationState == null && NameParser.parse(identity) == null) {
			validationState = "Identity is not valid.";
			getLabelIdentity().setFlagged(true);
		}
		else {
			getLabelIdentity().setFlagged(false);
		}
		
		if (validationState == null && password.length() == 0) {
			validationState = "Password must not be empty.";
			getLabelPassword().setFlagged(true);
		}
		else {
			getLabelPassword().setFlagged(false);
		}
		
		if (validationState == null) {
			getFieldValidationMessage().setText(" ");
		}
		else {
			getFieldValidationMessage().setText(validationState);
		}
		
		boolean canContinue = validationState == null;
		getButtonOk().setEnabled(canContinue);
	}
	
	public static boolean isValid(String url) {
		try {
			new URL(url);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}
}

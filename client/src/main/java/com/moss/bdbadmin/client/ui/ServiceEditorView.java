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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.moss.swing.FlagLabel;

@SuppressWarnings("serial")
abstract class ServiceEditorView extends JPanel {
	private JLabel fieldValidationMessage;
	private FlagLabel labelPassword;
	private FlagLabel labelIdentity;
	private FlagLabel labelUrl;
	private JPasswordField fieldPassword;
	private JTextField fieldIdentity;
	private JTextField fieldUrl;
	private JButton buttonCancel;
	private JButton buttonOk;
	public ServiceEditorView() {
		super();
		setLayout(new BorderLayout());

		final JPanel panel = new JPanel();
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowHeights = new int[] {7,7,7,0,7,0,7,7,7};
		gridBagLayout.columnWidths = new int[] {7,7,7,0,7};
		panel.setLayout(gridBagLayout);
		add(panel, BorderLayout.CENTER);

		labelUrl = new FlagLabel();
		labelUrl.setText("URL:");
		final GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridx = 1;
		panel.add(labelUrl, gridBagConstraints);

		fieldUrl = new JTextField();
		final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
		gridBagConstraints_1.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_1.ipadx = 200;
		gridBagConstraints_1.weightx = 1.0;
		gridBagConstraints_1.gridy = 1;
		gridBagConstraints_1.gridx = 3;
		panel.add(fieldUrl, gridBagConstraints_1);

		labelIdentity = new FlagLabel();
		labelIdentity.setText("Identity:");
		final GridBagConstraints gridBagConstraints_2 = new GridBagConstraints();
		gridBagConstraints_2.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_2.gridy = 3;
		gridBagConstraints_2.gridx = 1;
		panel.add(labelIdentity, gridBagConstraints_2);

		fieldIdentity = new JTextField();
		final GridBagConstraints gridBagConstraints_4 = new GridBagConstraints();
		gridBagConstraints_4.weightx = 1.0;
		gridBagConstraints_4.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_4.gridy = 3;
		gridBagConstraints_4.gridx = 3;
		panel.add(fieldIdentity, gridBagConstraints_4);

		labelPassword = new FlagLabel();
		labelPassword.setText("Password:");
		final GridBagConstraints gridBagConstraints_3 = new GridBagConstraints();
		gridBagConstraints_3.gridy = 5;
		gridBagConstraints_3.gridx = 1;
		panel.add(labelPassword, gridBagConstraints_3);

		fieldPassword = new JPasswordField();
		final GridBagConstraints gridBagConstraints_5 = new GridBagConstraints();
		gridBagConstraints_5.weightx = 1.0;
		gridBagConstraints_5.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_5.gridy = 5;
		gridBagConstraints_5.gridx = 3;
		panel.add(fieldPassword, gridBagConstraints_5);

		final JLabel spacer = new JLabel();
		final GridBagConstraints gridBagConstraints_6 = new GridBagConstraints();
		gridBagConstraints_6.weighty = 1;
		gridBagConstraints_6.gridy = 7;
		gridBagConstraints_6.gridx = 1;
		panel.add(spacer, gridBagConstraints_6);

		final JPanel panel_2 = new JPanel();
		final GridBagConstraints gridBagConstraints_7 = new GridBagConstraints();
		gridBagConstraints_7.fill = GridBagConstraints.BOTH;
		gridBagConstraints_7.gridwidth = 3;
		gridBagConstraints_7.gridy = 8;
		gridBagConstraints_7.gridx = 1;
		panel.add(panel_2, gridBagConstraints_7);

		fieldValidationMessage = new JLabel();
		fieldValidationMessage.setForeground(Color.RED);
		fieldValidationMessage.setText(" ");
		panel_2.add(fieldValidationMessage);

		final JPanel panel_1 = new JPanel();
		final FlowLayout flowLayout = new FlowLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		panel_1.setLayout(flowLayout);
		add(panel_1, BorderLayout.SOUTH);

		buttonOk = new JButton();
		buttonOk.setText("Ok");
		panel_1.add(buttonOk);

		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel_1.add(buttonCancel);
	}
	public JButton getButtonOk() {
		return buttonOk;
	}
	public JButton getButtonCancel() {
		return buttonCancel;
	}
	public FlagLabel getLabelUrl() {
		return labelUrl;
	}
	public FlagLabel getLabelIdentity() {
		return labelIdentity;
	}
	public FlagLabel getLabelPassword() {
		return labelPassword;
	}
	public JTextField getFieldUrl() {
		return fieldUrl;
	}
	public JTextField getFieldIdentity() {
		return fieldIdentity;
	}
	public JPasswordField getFieldPassword() {
		return fieldPassword;
	}
	public JLabel getFieldValidationMessage() {
		return fieldValidationMessage;
	}

}

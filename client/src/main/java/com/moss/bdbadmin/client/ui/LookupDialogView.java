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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
abstract class LookupDialogView extends JPanel {
	private JButton buttonCancel;
	private JButton buttonOpen;
	private JLabel fieldQueryPath;
	private JLabel fieldSearchStatus;
	private JTextField fieldKey;
	public LookupDialogView() {
		super();
		setLayout(new BorderLayout());

		final JPanel panel = new JPanel();
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowHeights = new int[] {7,7};
		gridBagLayout.columnWidths = new int[] {0,7};
		panel.setLayout(gridBagLayout);
		add(panel, BorderLayout.CENTER);

		final JPanel panel_2 = new JPanel();
		final FlowLayout flowLayout_1 = new FlowLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		panel_2.setLayout(flowLayout_1);
		final GridBagConstraints gridBagConstraints_6 = new GridBagConstraints();
		gridBagConstraints_6.fill = GridBagConstraints.BOTH;
		gridBagConstraints_6.gridx = 0;
		gridBagConstraints_6.gridy = 0;
		panel.add(panel_2, gridBagConstraints_6);

		final JLabel queryPathLabel = new JLabel();
		queryPathLabel.setText("Query Path:");
		panel_2.add(queryPathLabel);

		fieldQueryPath = new JLabel();
		panel_2.add(fieldQueryPath);

		final JPanel searchPanel = new JPanel();
		final GridBagLayout gridBagLayout_1 = new GridBagLayout();
		gridBagLayout_1.rowHeights = new int[] {0};
		gridBagLayout_1.columnWidths = new int[] {7,7,7,0,0};
		searchPanel.setLayout(gridBagLayout_1);
		final GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridx = 0;
		panel.add(searchPanel, gridBagConstraints);

		final JLabel keyLabel = new JLabel();
		keyLabel.setText("Key:");
		final GridBagConstraints gridBagConstraints_4 = new GridBagConstraints();
		gridBagConstraints_4.gridy = 0;
		gridBagConstraints_4.gridx = 1;
		gridBagConstraints_4.fill = GridBagConstraints.BOTH;
		searchPanel.add(keyLabel, gridBagConstraints_4);

		fieldKey = new JTextField();
		final GridBagConstraints gridBagConstraints_2 = new GridBagConstraints();
		gridBagConstraints_2.weightx = 1.0;
		gridBagConstraints_2.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_2.ipadx = 200;
		gridBagConstraints_2.gridy = 0;
		gridBagConstraints_2.gridx = 3;
		searchPanel.add(fieldKey, gridBagConstraints_2);

		final JPanel resultPanel = new JPanel();
		final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
		gridBagConstraints_1.weightx = 1.0;
		gridBagConstraints_1.fill = GridBagConstraints.BOTH;
		gridBagConstraints_1.gridy = 3;
		gridBagConstraints_1.gridx = 0;
		panel.add(resultPanel, gridBagConstraints_1);

		fieldSearchStatus = new JLabel();
		fieldSearchStatus.setText(" ");
		resultPanel.add(fieldSearchStatus);

		final JPanel panel_1 = new JPanel();
		final FlowLayout flowLayout = new FlowLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		panel_1.setLayout(flowLayout);
		final GridBagConstraints gridBagConstraints_5 = new GridBagConstraints();
		gridBagConstraints_5.fill = GridBagConstraints.BOTH;
		gridBagConstraints_5.gridy = 5;
		gridBagConstraints_5.gridx = 0;
		panel.add(panel_1, gridBagConstraints_5);

		buttonOpen = new JButton();
		buttonOpen.setText("Open");
		panel_1.add(buttonOpen);

		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel_1.add(buttonCancel);
	}
	public JLabel getFieldSearchStatus() {
		return fieldSearchStatus;
	}
	public JLabel getFieldQueryPath() {
		return fieldQueryPath;
	}
	public JTextField getFieldKey() {
		return fieldKey;
	}
	public JButton getButtonOpen() {
		return buttonOpen;
	}
	public JButton getButtonCancel() {
		return buttonCancel;
	}

}

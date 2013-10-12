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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JToolBar;

@SuppressWarnings("serial")
abstract class EntryEditorView extends JPanel {
	private JPanel containerPanel;
	private JTextPane fieldKey;
	private JPanel editPanel;
	private JComboBox fieldEditMode;
	private JButton buttonNext;
	private JButton buttonPrevious;
	private JButton buttonRefresh;
	private JLabel fieldStatusInfo;
	private JButton buttonDelete;
	private JButton buttonSave;
	public EntryEditorView() {
		super();
		setLayout(new BorderLayout());

		final JToolBar toolBar = new JToolBar();
		add(toolBar, BorderLayout.NORTH);

		buttonSave = new JButton();
		buttonSave.setText("Put");
		toolBar.add(buttonSave);

		buttonDelete = new JButton();
		buttonDelete.setText("Delete");
		toolBar.add(buttonDelete);

		buttonRefresh = new JButton();
		buttonRefresh.setText("Refresh");
		toolBar.add(buttonRefresh);

		buttonPrevious = new JButton();
		buttonPrevious.setText("Previous");
		toolBar.add(buttonPrevious);

		buttonNext = new JButton();
		buttonNext.setText("Next");
		toolBar.add(buttonNext);

		fieldEditMode = new JComboBox();
		toolBar.add(fieldEditMode);

		final JPanel mainPanel = new JPanel();
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowHeights = new int[] {0,7};
		mainPanel.setLayout(gridBagLayout);
		add(mainPanel, BorderLayout.CENTER);

		final JPanel keyPanel = new JPanel();
		final GridBagLayout gridBagLayout_1 = new GridBagLayout();
		gridBagLayout_1.rowHeights = new int[] {7,0,7};
		gridBagLayout_1.columnWidths = new int[] {7,7,7,0,7};
		keyPanel.setLayout(gridBagLayout_1);
		final GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridx = 0;
		mainPanel.add(keyPanel, gridBagConstraints);

		final JLabel keyLabel = new JLabel();
		keyLabel.setText("Key:");
		final GridBagConstraints gridBagConstraints_3 = new GridBagConstraints();
		gridBagConstraints_3.gridy = 1;
		gridBagConstraints_3.gridx = 1;
		keyPanel.add(keyLabel, gridBagConstraints_3);

		fieldKey = new JTextPane();
		fieldKey.setEditable(false);
		final GridBagConstraints gridBagConstraints_2 = new GridBagConstraints();
		gridBagConstraints_2.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_2.weightx = 1.0;
		gridBagConstraints_2.ipadx = 200;
		gridBagConstraints_2.gridy = 1;
		gridBagConstraints_2.gridx = 3;
		keyPanel.add(fieldKey, gridBagConstraints_2);

		editPanel = new JPanel();
		final GridBagLayout gridBagLayout_2 = new GridBagLayout();
		gridBagLayout_2.columnWidths = new int[] {7,0,7};
		editPanel.setLayout(gridBagLayout_2);
		final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
		gridBagConstraints_1.fill = GridBagConstraints.BOTH;
		gridBagConstraints_1.weightx = 1.0;
		gridBagConstraints_1.weighty = 1.0;
		gridBagConstraints_1.gridy = 1;
		gridBagConstraints_1.gridx = 0;
		mainPanel.add(editPanel, gridBagConstraints_1);

		containerPanel = new JPanel();
		final GridBagConstraints gridBagConstraints_4 = new GridBagConstraints();
		gridBagConstraints_4.fill = GridBagConstraints.BOTH;
		gridBagConstraints_4.weighty = 1;
		gridBagConstraints_4.weightx = 1;
		gridBagConstraints_4.gridy = 0;
		gridBagConstraints_4.gridx = 1;
		editPanel.add(containerPanel, gridBagConstraints_4);

		final JPanel panel = new JPanel();
		final FlowLayout flowLayout = new FlowLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		panel.setLayout(flowLayout);
		add(panel, BorderLayout.SOUTH);

		fieldStatusInfo = new JLabel();
		panel.add(fieldStatusInfo);
	}
	public JButton getButtonPut() {
		return buttonSave;
	}
	public JButton getButtonDelete() {
		return buttonDelete;
	}
	public JLabel getFieldStatusInfo() {
		return fieldStatusInfo;
	}
	public JButton getButtonRefresh() {
		return buttonRefresh;
	}
	public JButton getButtonPrevious() {
		return buttonPrevious;
	}
	public JButton getButtonNext() {
		return buttonNext;
	}
	public JComboBox getFieldEditMode() {
		return fieldEditMode;
	}
	public JPanel getEditPanel() {
		return editPanel;
	}
	public JTextPane getFieldKey() {
		return fieldKey;
	}
	public JPanel getContainerPanel() {
		return containerPanel;
	}

}

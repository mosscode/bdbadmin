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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

@SuppressWarnings("serial")
final class XmlEditorComponent extends XmlEditorView implements EntryEditorComponent {

	public void setContent(byte[] content) {
		try {
			content = beautify(content);
			getFieldContent().setText(new String(content, "UTF8"));
			getScrollPane().getVerticalScrollBar().setUnitIncrement(50);
			getScrollPane().getVerticalScrollBar().setBlockIncrement(50);
			scrollToStart(getScrollPane().getHorizontalScrollBar());
			scrollToStart(getScrollPane().getVerticalScrollBar());
			getFieldContent().setCaretPosition(0);
//			getScrollPane().getViewport().setViewPosition(new Point(0,0));
//			getScrollPane().getVerticalScrollBar().setValue(getScrollPane().getVerticalScrollBar().getMinimum());
		}
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private void scrollToStart(JScrollBar bar){
		bar.setValue(bar.getMinimum());
	}
	
	public byte[] getContent() {
		try {
			return getFieldContent().getText().getBytes("UTF8");
		}
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	public JComponent view() {
		return this;
	}
	
	private byte[] beautify(byte[] raw) {
		try {
			Source xmlSource = new StreamSource(new ByteArrayInputStream(raw));
			Source xsltSource = new StreamSource(getClass().getResourceAsStream("/prettyprint.xsl"));

			TransformerFactory transFact = TransformerFactory.newInstance();
			Transformer trans = transFact.newTransformer(xsltSource);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			trans.transform(xmlSource, new StreamResult(out));

			return out.toByteArray();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return raw;
		}
	}
}

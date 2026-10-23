/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2026, Plutext Pty Ltd, and contributors.
 *  Portions contributed before 15 July 2013 formed part of docx4j 
 *  and were contributed under ASL v2 (a copy of which is incorporated
 *  herein by reference and applies to those portions). 
 *   
 *  This library as a whole is licensed under the GNU Lesser General 
 *  Public License as published by the Free Software Foundation; 
    version 2.1.
    
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library (see legals/LICENSE); if not, 
    see http://www.gnu.org/licenses/lgpl-2.1.html
    
 */
package org.docx4j.convert.in.xhtml.tests;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * convert(Node, baseUrl) given an element rather than a whole document: the element is
 * copied into a new document and imported, not dropped (before, the copy was never
  * appended, so the importer saw an empty document).  A DocumentFragment of several nodes
 * is imported in a div; a node which can't be a document's root gives a Docx4JException.
 */
public class NodeInputTest {

	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>"
			+ "<p>Outside the div</p><div><p>NODE_INPUT_TEXT</p></div></body></html>";

	private static Document parse() throws Exception {
		return XmlUtils.getNewDocumentBuilder().parse(
				new ByteArrayInputStream(XHTML.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void elementIsImported() throws Exception {
		Element div = (Element) parse().getElementsByTagName("div").item(0);
		List<Object> content = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(div, null);

		String xml = marshal(content);
		Assert.assertTrue(xml, xml.contains("NODE_INPUT_TEXT"));
		Assert.assertFalse(xml, xml.contains("Outside the div"));
	}

	@Test
	public void documentStillImported() throws Exception {
		List<Object> content = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(parse(), null);

		String xml = marshal(content);
		Assert.assertTrue(xml, xml.contains("Outside the div"));
		Assert.assertTrue(xml, xml.contains("NODE_INPUT_TEXT"));
	}

		@Test
	public void fragmentIsImported() throws Exception {
		Document doc = parse();
		DocumentFragment fragment = doc.createDocumentFragment();
		fragment.appendChild(doc.getElementsByTagName("p").item(0).cloneNode(true));
		fragment.appendChild(doc.getElementsByTagName("div").item(0).cloneNode(true));
		List<Object> content = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(fragment, null);

		String xml = marshal(content);
		Assert.assertTrue(xml, xml.contains("Outside the div"));
		Assert.assertTrue(xml, xml.contains("NODE_INPUT_TEXT"));
	}

	@Test
	public void textNodeIsRefused() throws Exception {
		try {
			new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(parse().createTextNode("just text"), null);
			Assert.fail("converted");
		} catch (Docx4JException expected) {
			Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("DocumentFragment"));
		}
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

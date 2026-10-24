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

import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Assert;
import org.junit.Test;

/**
 * A br is one w:br.  From openhtmltopdf 1.1.97, the "\A" its default stylesheet puts
 * before a br is laid out inside the br's own box, which that line break splits in two,
 * and each part gave a w:br.
 */
public class LineBreakTest {

	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>"
			+ "<p>a<br/>b</p>"
			+ "<p>c<br/><br/>d</p>"
			+ "<div>e<br/>f</div>"
			+ "<table><tr><td>x<br/>y</td></tr></table>"
			+ "</body></html>";

	@Test
	public void oneBreakForEachBr() throws Exception {
		String xml = marshal(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(XHTML, null));

		Assert.assertEquals(xml, 5, count(xml, "<w:br/>"));

		// and where the br was, between its text
		int a = xml.indexOf(">a</w:t>");
		int b = xml.indexOf(">b</w:t>");
		Assert.assertTrue(xml, a >= 0 && b > a);
		Assert.assertEquals(xml, 1, count(xml.substring(a, b), "<w:br/>"));
		int c = xml.indexOf(">c</w:t>");
		int d = xml.indexOf(">d</w:t>");
		Assert.assertTrue(xml, c >= 0 && d > c);
		Assert.assertEquals(xml, 2, count(xml.substring(c, d), "<w:br/>"));
	}

	private static int count(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
		return n;
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

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
import org.junit.Assume;
import org.junit.Test;

/**
 * Text with word-wrap: break-word that fits only when broken between characters converts.
 * openhtmltopdf 1.1.100 laid it out, but with assertions enabled, as Maven runs tests, it
 * threw an AssertionError from LineBreakContext.checkConsistency.  Fixed in 1.1.102 (its
 * issue 258), whose reproducer this is.
 */
public class BreakWordTest {

	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>"
			+ "table.box td { border: .5pt solid #B9C4C1; padding: 1.2mm 1.6mm; word-wrap: break-word; }"
			+ "</style></head><body>"
			+ "<table class=\"box\"><tr>"
			+ "<td><div class=\"cap\"><span class=\"n\">GUARANTEE REFERENCE NO.</span></div></td>"
			+ "<td><div class=\"cap\"><span class=\"n\">AMOUNT OF GUARANTEE</span></div></td>"
			+ "<td><div class=\"cap\"><span class=\"n\"></span></div><div class=\"v\" dir=\"ltr\">01 Aug 2026</div></td>"
			+ "<td class=\"row\"><span class=\"n\">33</span><span class=\"lbl\">FEES</span></td>"
			+ "<td class=\"row\" colspan=\"8\" dir=\"ltr\">1250.00</td>"
			+ "</tr></table></body></html>";

	@Test
	public void breakWordInATableCell() throws Exception {
		// openhtmltopdf's assertion is what this tests for
		Assume.assumeTrue(com.openhtmltopdf.layout.LineBreakContext.class.desiredAssertionStatus());

		String xml = marshal(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(XHTML, null));

		for (String text : new String[] {"GUARANTEE REFERENCE NO.", "AMOUNT OF GUARANTEE", "01 Aug 2026", "FEES", "1250.00"}) {
			Assert.assertTrue(text + ": " + xml, xml.contains(">" + text + "</w:t>"));
		}
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

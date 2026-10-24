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
 * page-break-inside: avoid gives w:keepLines only where the author asked for it: a style
 * attribute, a style element or a linked stylesheet, on the paragraph or on the nearest
 * ancestor that sets it.  From 1.1.79, openhtmltopdf's default stylesheet sets it on
 * thead, which gave every paragraph in a header row w:keepLines (CR-006 D1).
 */
public class PageBreakInsideTest {

	private static final String TABLE = "<table><thead><tr><th>HEAD</th></tr></thead>"
			+ "<tbody><tr><td>BODY</td></tr></tbody></table>";

	@Test
	public void notFromTheDefaultStylesheet() throws Exception {
		String xml = convert("", TABLE);
		Assert.assertTrue(xml, xml.contains("HEAD"));
		Assert.assertFalse(xml, xml.contains("keepLines"));
	}

	@Test
	public void theAuthorsOnTheParagraph() throws Exception {
		String xml = convert("p.keep { page-break-inside: avoid; }",
				"<p class=\"keep\">STYLE ELEMENT</p><p style=\"page-break-inside: avoid\">STYLE ATTRIBUTE</p><p>NEITHER</p>");
		Assert.assertEquals(xml, 2, count(xml, "<w:keepLines/>"));
		Assert.assertTrue(xml, xml.indexOf("<w:keepLines/>") < xml.indexOf("STYLE ELEMENT"));
		Assert.assertTrue(xml, xml.lastIndexOf("<w:keepLines/>") < xml.indexOf("STYLE ATTRIBUTE"));
	}

	@Test
	public void theAuthorsInherited() throws Exception {
		String xml = convert("div.keep { page-break-inside: avoid; } thead { page-break-inside: avoid; }",
				"<div class=\"keep\"><p>IN THE DIV</p></div>" + TABLE);
		Assert.assertEquals(xml, 2, count(xml, "<w:keepLines/>")); // the div's p, and the th's
		Assert.assertTrue(xml, xml.lastIndexOf("<w:keepLines/>") < xml.indexOf("HEAD"));
		Assert.assertTrue(xml, xml.lastIndexOf("<w:keepLines/>") > xml.indexOf("IN THE DIV"));
	}

	@Test
	public void theAuthorsAutoWins() throws Exception {
		String xml = convert("th { page-break-inside: auto; }", TABLE);
		Assert.assertFalse(xml, xml.contains("keepLines"));
	}

	private static String convert(String css, String body) throws Exception {
		return marshal(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>" + css + "</style></head><body>"
				+ body + "</body></html>", null));
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

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
 * A paragraph's w:ind outside a list, and a table's w:tblInd, from the margin-left and
 * padding-left of the blocks around them (#118).  The sum stops at the body, whose margin,
 * border and padding lie outside the docx's text, and at a table cell, whose padding is its
 * w:tcMar.  Before, the default stylesheet's 8px body margin gave every paragraph 120, and a
 * paragraph in a cell had the cell's padding again, and the indents of the blocks around the
 * table, which itself had none.
 */
public class IndentTest {

	@Test
	public void paragraphs() throws Exception {
		String xml = convert("", "<p>x</p>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"0\"/>"));

		xml = convert("", "<div style=\"margin-left:1in\"><p>x</p></div>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"1440\"/>"));

		xml = convert("", "<div style=\"padding-left:0.5in\"><p style=\"margin-left:0.5in\">x</p></div>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"1440\"/>"));

		// the body's own margin and padding are outside the text, as the page's margins are
		xml = convert("body{margin-left:1in;padding-left:1in}", "<p>x</p>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"0\"/>"));
	}

	@Test
	public void paragraphsInACell() throws Exception {
		String xml = convert("", "<table><tr><td><p>x</p></td></tr></table>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"0\"/>"));

		// the cell's padding is its w:tcMar, and isn't repeated in the paragraph
		xml = convert("", "<table><tr><td style=\"padding-left:1in\"><p>x</p></td></tr></table>");
		Assert.assertTrue(xml, xml.contains("<w:left w:w=\"1440\" w:type=\"dxa\"/>"));
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"0\"/>"));

		xml = convert("", "<table><tr><td><p style=\"margin-left:1in\">x</p></td></tr></table>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"1440\"/>"));

		// the div indents the table, not the paragraphs in its cells
		xml = convert("", "<div style=\"margin-left:1in\"><table><tr><td><p>x</p></td></tr></table></div>");
		Assert.assertTrue(xml, xml.contains("<w:tblInd w:w=\"1440\" w:type=\"dxa\"/>"));
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"0\"/>"));

		xml = convert("", "<table><tr><td><table><tr><td><p>x</p></td></tr></table></td></tr></table>");
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"0\"/>"));
	}

	@Test
	public void tables() throws Exception {
		String xml = convert("", "<div style=\"margin-left:1in\"><table><tr><td>x</td></tr></table></div>");
		Assert.assertTrue(xml, xml.contains("<w:tblInd w:w=\"1440\" w:type=\"dxa\"/>"));

		xml = convert("", "<div style=\"margin-left:1in\"><table style=\"margin-left:0.5in\"><tr><td>x</td></tr></table></div>");
		Assert.assertTrue(xml, xml.contains("<w:tblInd w:w=\"2160\" w:type=\"dxa\"/>"));

		// a table in a cell is indented from the cell, not from the blocks around the outer table
		xml = convert("", "<div style=\"margin-left:1in\"><table><tr><td><table><tr><td>x</td></tr></table></td></tr></table></div>");
		Assert.assertEquals(xml, 1, count(xml, "<w:tblInd "));

		// the body's margin doesn't indent a table
		xml = convert("body{margin-left:1in}", "<table><tr><td>x</td></tr></table>");
		Assert.assertFalse(xml, xml.contains("<w:tblInd "));
	}

	@Test
	public void listsInATable() throws Exception {
		// a list in a cell takes the numbering's indent, whatever indents the table
		String[] around = {"<table style=\"margin-left:1in\">", "<div style=\"margin-left:1in\"><table>"};
		for (String start : around) {
			String xml = convert("", start + "<tr><td><ul><li>x</li></ul></td></tr></table>" + (start.startsWith("<div") ? "</div>" : ""));
			Assert.assertTrue(xml, xml.contains("<w:tblInd w:w=\"1440\" w:type=\"dxa\"/>"));
			Assert.assertFalse(start + ": " + xml, xml.contains("<w:ind "));
		}

		// a table in a list item is indented by the list, which a list in its cells is placed from
		String xml = convert("", "<ul><li>a<table><tr><td><ul><li>x</li></ul></td></tr></table></li></ul>");
		Assert.assertTrue(xml, xml.contains("<w:tblInd w:w=\"960\" w:type=\"dxa\"/>"));
		Assert.assertTrue(xml, xml.contains("<w:ind w:left=\"600\" w:hanging=\"360\"/>"));
	}

	private static int count(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
		return n;
	}

	private static String convert(String style, String body) throws Exception {
		List<Object> content = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>" + style + "</style></head><body>"
				+ body + "</body></html>", null);
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

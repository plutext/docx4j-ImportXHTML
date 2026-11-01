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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Assert;
import org.junit.Test;

/**
 * A table with a set width keeps it beside its horizontal margins (#119).  openhtmltopdf takes
 * the margins off the width (its TableBox.getCSSWidth, since openhtmltopdf #59), so a 4in table
 * with a 1in margin came out 3in wide.  The columns get the margins back, as far as the line has
 * room beside them, so a width: 100% table with a margin still fits.
 */
public class TableMarginTest {

	private static final String COLLAPSED = "table{border-collapse:collapse}td{padding:0}";
	private static final String TWO = "<tr><td>a</td><td>b</td></tr>";

	@Test
	public void setWidth() throws Exception {
		String xml = convert(COLLAPSED, "<table style=\"width:4in;margin-left:1in\">" + TWO + "</table>");
		Assert.assertEquals(xml, 5760, grid(xml));
		Assert.assertTrue(xml, xml.contains("<w:tblInd w:w=\"1440\" w:type=\"dxa\"/>"));

		xml = convert(COLLAPSED, "<table style=\"width:4in;margin-right:1in\">" + TWO + "</table>");
		Assert.assertEquals(xml, 5760, grid(xml));

		xml = convert(COLLAPSED, "<table style=\"width:2in;margin:0 1in\">" + TWO + "</table>");
		Assert.assertEquals(xml, 2880, grid(xml));
	}

	@Test
	public void percentWidth() throws Exception {
		int without = grid(convert(COLLAPSED, "<table style=\"width:50%\">" + TWO + "</table>"));
		String xml = convert(COLLAPSED, "<table style=\"width:50%;margin-left:10%\">" + TWO + "</table>");
		Assert.assertEquals(xml, without, grid(xml), 2);
	}

	@Test
	public void fitsTheLine() throws Exception {
		// width: 100% and a margin: narrowed by the margin, as openhtmltopdf #59 has it
		int full = grid(convert(COLLAPSED, "<table style=\"width:100%\">" + TWO + "</table>"));
		String xml = convert(COLLAPSED, "<table style=\"width:100%;margin-left:15px\">" + TWO + "</table>");
		Assert.assertEquals(xml, full - 225, grid(xml), 2);
	}

	@Test
	public void unchanged() throws Exception {
		// no set width: the margin was never taken off it
		String xml = convert(COLLAPSED, "<table style=\"margin-left:1in\">" + TWO + "</table>");
		Assert.assertTrue(xml, grid(xml) < 1440);

		// wider than its width for its content: as wide as the content, with or without the margin
		String wide = "<tr><td>WWWWWWWWWWWWWWWWWWWWWWWWWWWW</td></tr>";
		int without = grid(convert(COLLAPSED, "<table style=\"width:1in\">" + wide + "</table>"));
		xml = convert(COLLAPSED, "<table style=\"width:1in;margin-left:1in\">" + wide + "</table>");
		Assert.assertEquals(xml, without, grid(xml), 2);
	}

	private static int grid(String xml) {
		int sum = 0;
		Matcher m = Pattern.compile("<w:gridCol w:w=\"(\\d+)\"/>").matcher(xml);
		while (m.find()) sum += Integer.parseInt(m.group(1));
		return sum;
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

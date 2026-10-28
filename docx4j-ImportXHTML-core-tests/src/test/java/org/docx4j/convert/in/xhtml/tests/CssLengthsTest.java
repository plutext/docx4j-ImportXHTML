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
 * CSS lengths in their own units (CR-008).  openhtmltopdf lays out in dots, 20 to the CSS
 * pixel, so a dot is 3/4 of a twip.  Table and border lengths were written as if a dot were a
 * twip, a third too large, and padding as if its number were pixels, whatever its unit.  The
 * padding is now the layout's, so em and % are as openhtmltopdf resolved them.
 */
public class CssLengthsTest {

	private static final String[] ONE_INCH = {"1in", "72pt", "96px", "6em"};

	@Test
	public void columnWidths() throws Exception {
		String xml = convert("table{width:6in;border-collapse:collapse}td{padding:0}",
				"<table><tr><td>x</td><td>y</td></tr></table>");
		Assert.assertEquals(xml, 2, count(xml, "<w:gridCol w:w=\"4320\"/>"));
		Assert.assertEquals(xml, 2, count(xml, "<w:tcW w:w=\"4320\" w:type=\"dxa\"/>"));

		for (String length : ONE_INCH) {
			xml = convert("table{border-collapse:collapse}td{padding:0;width:" + length + "}",
					"<table><tr><td>x</td></tr></table>");
			Assert.assertTrue(length + ": " + xml, xml.contains("<w:gridCol w:w=\"1440\"/>"));
			Assert.assertTrue(length + ": " + xml, xml.contains("<w:tcW w:w=\"1440\" w:type=\"dxa\"/>"));
		}
	}

	@Test
	public void tableIndent() throws Exception {
		for (String length : ONE_INCH) {
			String xml = convert("table{margin-left:" + length + "}", "<table><tr><td>x</td></tr></table>");
			Assert.assertTrue(length + ": " + xml, xml.contains("<w:tblInd w:w=\"1440\" w:type=\"dxa\"/>"));
		}
	}

	@Test
	public void cellSpacing() throws Exception {
		// halved, since Word leaves it twice: between cells, and between a cell and the table's border
		String[][] cases = {{"10pt", "100"}, {"0.25in", "180"}, {"4px", "30"}, {"1em", "120"}};
		for (String[] c : cases) {
			String xml = convert("table{border-collapse:separate;border-spacing:" + c[0] + "}",
					"<table><tr><td>x</td></tr></table>");
			Assert.assertTrue(c[0] + ": " + xml, xml.contains("<w:tblCellSpacing w:w=\"" + c[1] + "\" w:type=\"dxa\"/>"));
		}
		// the default stylesheet's 2px
		String xml = convert("", "<table><tr><td>x</td></tr></table>");
		Assert.assertTrue(xml, xml.contains("<w:tblCellSpacing w:w=\"15\" w:type=\"dxa\"/>"));
	}

	@Test
	public void borderWidths() throws Exception {
		// w:sz is in eighths of a point
		String xml = convert("p,td,table{border:1px solid red}", "<p>x</p><table><tr><td>y</td></tr></table>");
		Assert.assertEquals(xml, 12, count(xml, "w:sz=\"6\""));

		String[][] cases = {{"0.5pt", "4"}, {"1pt", "8"}, {"4pt", "32"}, {"2px", "12"}, {"0.0625in", "36"},
				{"0.125em", "12"}, {"thin", "6"}, {"medium", "12"}, {"thick", "18"}};
		for (String[] c : cases) {
			xml = convert("p,td{border:" + c[0] + " solid red}", "<p>x</p><table><tr><td>y</td></tr></table>");
			Assert.assertEquals(c[0] + ": " + xml, 8, count(xml, "w:sz=\"" + c[1] + "\""));
		}
	}

	@Test
	public void doubleBorder() throws Exception {
		// Word's w:sz is each line's width, and the gap is as wide, so CSS's width is divided by 3
		String xml = convert("p,td{border:3pt double red}", "<p>x</p><table><tr><td>y</td></tr></table>");
		Assert.assertEquals(xml, 8, count(xml, "w:val=\"double\" w:color=\"FF0000\" w:sz=\"8\""));
	}

	@Test
	public void cellMargins() throws Exception {
		String[][] cases = {{"padding:10px", "150"}, {"padding:10pt", "200"}, {"padding:0.25in", "360"},
				{"padding:1em", "240"}, {"padding:2em", "480"}, {"padding:1em;font-size:20pt", "400"},
				{"padding:0", "0"},
				// openhtmltopdf 1.1.100 lays a cell's % padding out as none
				{"padding:5%", "0"}};
		for (String[] c : cases) {
			String xml = convert("td{" + c[0] + "}", "<table><tr><td>x</td></tr></table>");
			Assert.assertEquals(c[0] + ": " + xml, 4, count(tcMar(xml), "w:w=\"" + c[1] + "\""));
		}
		// the default stylesheet's 1px
		String xml = convert("", "<table><tr><td>x</td></tr></table>");
		Assert.assertEquals(xml, 4, count(tcMar(xml), "w:w=\"15\""));
	}

	@Test
	public void borderSpace() throws Exception {
		// w:space is in points
		String[][] cases = {{"24px", "18"}, {"24pt", "24"}, {"4pt", "4"}, {"1em", "12"}, {"0.25in", "18"}};
		for (String[] c : cases) {
			String xml = convert("p{border:1px solid red;padding:" + c[0] + "}", "<p>x</p>");
			Assert.assertEquals(c[0] + ": " + xml, 4, count(xml, "w:space=\"" + c[1] + "\""));
		}
	}

	@Test
	public void borderSpaceOnlyWithABorder() throws Exception {
		// a w:pBdr side needs its w:val, so padding with no border gives none (D8)
		String xml = convert("p{padding:24pt}", "<p>x</p>");
		Assert.assertFalse(xml, xml.contains("<w:pBdr"));

		xml = convert("p{border-left:1px solid red;padding:24pt}", "<p>x</p>");
		Assert.assertTrue(xml, xml.contains("<w:left w:val=\"single\" w:color=\"FF0000\" w:sz=\"6\" w:space=\"24\"/>"));
		Assert.assertEquals(xml, 1, count(xml, "w:space="));

		// a transparent border is none (CR-006), so it keeps no space
		xml = convert("p{border:1px solid transparent;padding:24pt}", "<p>x</p>");
		Assert.assertEquals(xml, 4, count(xml, "w:val=\"nil\""));
		Assert.assertFalse(xml, xml.contains("w:space="));
	}

	private static String tcMar(String xml) {
		return xml.substring(xml.indexOf("<w:tcMar>"), xml.indexOf("</w:tcMar>"));
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

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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Integer attributes within Word's range (CR-007).  Word keeps each in a fixed width, so a
 * value outside it wraps, with no error (docx4j CR-066).  The importer writes the nearest end
 * of the range instead, with a WARN.  And a row gets a w:trHeight only from a CSS height
 * (CR-007 D6 (d)): Word sizes a row to its content, and gives each part of a split row at
 * least its minimum height.
 */
public class WordRangeTest {

	private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	@Test
	public void rowSizedByItsContentHasNoHeight() throws Exception {
		StringBuilder cell = new StringBuilder();
		for (int i = 1; i <= 85; i++) {
			cell.append("<p>Line " + i + " of 85</p>");
		}
		Converted c = convert("table{border-collapse:collapse}td{border:1px solid black}",
				"<table><tr><td>" + cell + "</td></tr><tr><td>short</td></tr></table>");
		Assert.assertTrue(c.xml, c.xml.contains("Line 85 of 85") && c.xml.contains("short"));
		Assert.assertFalse(c.xml, c.xml.contains("<w:trHeight"));
		Assert.assertEquals("", c.log);
	}

	@Test
	public void rowHeightFromTheCss() throws Exception {
		Converted c = convert("",
				"<table><tr><td style=\"height: 1in\">TD</td><td>other</td></tr></table>"
				+ "<table><tr style=\"height: 72pt\"><td>TR</td></tr></table>"
				+ "<table><tr><td style=\"height: 50%\">PERCENT</td><td style=\"height: 0\">ZERO</td></tr></table>");
		Assert.assertEquals(c.xml, 2, count(c.xml, "<w:trHeight w:val=\"1440\" w:hRule=\"atLeast\"/>"));
		Assert.assertEquals(c.xml, 2, count(c.xml, "<w:trHeight"));
		Assert.assertEquals("", c.log);

		// the greatest of the row's
		c = convert("", "<table><tr style=\"height: 0.5in\"><td style=\"height: 2in\">A</td>"
				+ "<td style=\"height: 1in\">B</td></tr></table>");
		Assert.assertEquals(c.xml, 1, count(c.xml, "<w:trHeight"));
		Assert.assertTrue(c.xml, c.xml.contains("<w:trHeight w:val=\"2880\" w:hRule=\"atLeast\"/>"));

		// 43200 is read as exact 22336 by Word, cutting the cell off
		c = convert("", "<table><tr><td style=\"height: 30in\">TALL</td></tr></table>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:trHeight w:val=\"32767\" w:hRule=\"atLeast\"/>"));
		assertWarned(c, "w:trHeight/@w:val 43200 (from height on a row or cell)", "0..32767", 32767);
	}

	@Test
	public void borderSpace() throws Exception {
		// 45 is read as 13 by Word
		Converted c = convert("p{border:1px solid red;padding:60px}", "<p>x</p>");
		Assert.assertEquals(c.xml, 4, count(c.xml, "w:space=\"31\""));
		assertWarned(c, "a border's w:space 45 (from padding-left on a bordered paragraph)", "0..31", 31);
	}

	@Test
	public void borderSz() throws Exception {
		// 400 is read as 144 by Word
		Converted c = convert("p,td,table{border:50px solid red}",
				"<p>x</p><table><tr><td>y</td></tr></table>");
		Assert.assertEquals(c.xml, 12, count(c.xml, "w:sz=\"255\""));
		Assert.assertFalse(c.xml, c.xml.contains("w:sz=\"400\""));
		assertWarned(c, "a border's w:sz 400 (from border-top-width)", "0..255", 255);
	}

	@Test
	public void indents() throws Exception {
		Converted c = convert("p.far{margin-left:5000pt}p.back{margin-left:-5000pt}",
				"<p class=\"far\">FAR</p><p class=\"back\">BACK</p>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:ind w:left=\"32767\"/>"));
		// -99880 is read as 31192 by Word: text moved off the page lands to the right
		Assert.assertTrue(c.xml, c.xml.contains("<w:ind w:left=\"-32768\"/>"));
		assertWarned(c, "w:ind/@w:left 100120 (from margin-left and padding-left)", "-32768..32767", 32767);
		assertWarned(c, "w:ind/@w:left -99880 (from margin-left and padding-left)", "-32768..32767", -32768);

		// in the numbering part
		c = convert("ul{padding-left:5000pt}", "<ul><li>x</li></ul>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:ind w:left=\"32767\" w:hanging=\"360\"/>"));
		assertWarned(c, "w:ind/@w:left 100360 (from a list's indent)", "-32768..32767", 32767);

		c = convert("table{margin-left:5000pt}", "<table><tr><td>x</td></tr></table>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:tblInd w:w=\"32767\" w:type=\"dxa\"/>"));
		assertWarned(c, "w:tblInd 133333 (from margin-left on a table)", "-32768..32767", 32767);
	}

	@Test
	public void tableWidths() throws Exception {
		Converted c = convert("td{padding:3000pt}", "<table><tr><td>x</td></tr></table>");
		String tcMar = c.xml.substring(c.xml.indexOf("<w:tcMar>"), c.xml.indexOf("</w:tcMar>"));
		Assert.assertEquals(c.xml, 4, count(tcMar, "w:w=\"32767\""));
		Assert.assertTrue(c.xml, c.xml.contains("<w:tcW w:w=\"32767\" w:type=\"dxa\"/>"));
		Assert.assertTrue(c.xml, c.xml.contains("<w:gridCol w:w=\"32767\"/>"));
		assertWarned(c, "w:tcMar/@w:w 45000 (from padding-left on a cell)", "0..32767", 32767);
		assertWarned(c, "w:tcW/@w:w 160200 (from a column's width)", "0..32767", 32767);

		c = convert("table{border-collapse:separate;border-spacing:3000pt}", "<table><tr><td>x</td></tr></table>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:tblCellSpacing w:w=\"32767\" w:type=\"dxa\"/>"));
		assertWarned(c, "w:tblCellSpacing/@w:w 40000 (from border-spacing)", "0..32767", 32767);
	}

	/**
	 * What docx4j's converters write, after addParagraphProperties and addRunProperties.
	 */
	@Test
	public void docx4jConverters() throws Exception {
		Converted c = convert("p{margin-top:5000pt;margin-bottom:5000pt}", "<p>x</p>");
		Assert.assertTrue(c.xml, c.xml.contains("w:before=\"65535\"") && c.xml.contains("w:after=\"65535\""));
		assertWarned(c, "w:spacing/@w:before 100000 (from margin-top)", "0..65535", 65535);
		assertWarned(c, "w:spacing/@w:after 100000 (from margin-bottom)", "0..65535", 65535);

		c = convert("p{line-height:200}", "<p>x</p>");
		Assert.assertTrue(c.xml, c.xml.contains("w:line=\"32767\""));
		assertWarned(c, "w:spacing/@w:line 48000 (from line-height)", "-32768..32767", 32767);

		c = convert("span.wide{letter-spacing:2000pt}span.narrow{letter-spacing:-2000pt}",
				"<p><span class=\"wide\">x</span> <span class=\"narrow\">y</span></p>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:spacing w:val=\"32767\"/>"));
		Assert.assertTrue(c.xml, c.xml.contains("<w:spacing w:val=\"-32768\"/>"));
		assertWarned(c, "w:rPr/w:spacing/@w:val 40000 (from letter-spacing)", "-32768..32767", 32767);
		assertWarned(c, "w:rPr/w:spacing/@w:val -40000 (from letter-spacing)", "-32768..32767", -32768);

		c = convert("span{font-size:40000pt}", "<p><span>x</span></p>");
		Assert.assertTrue(c.xml, c.xml.contains("<w:sz w:val=\"65535\"/>"));
		assertWarned(c, "w:sz/@w:val 80000 (from font-size)", "0..65535", 65535);
	}

	/**
	 * Every integer attribute of the content and the numbering part, against CR-007 §3's
	 * ranges, after converting §2's inputs: a writer the guard's sites missed fails it.
	 */
	@Test
	public void everyAttributeInRange() throws Exception {
		StringBuilder longCell = new StringBuilder();
		for (int i = 1; i <= 300; i++) {
			longCell.append("<p>line " + i + " of a long cell</p>");
		}
		String[][] inputs = {
				{"table{border-collapse:separate;border-spacing:3000pt}", "<table><tr><td>x</td></tr></table>"},
				{"li{margin-left:5000pt}", "<ul><li>x</li></ul>"},
				{"p{border:50px solid red;padding:40px}", "<p>x</p>"},
				{"p{border:1px solid red;padding:60px}", "<p>x</p>"},
				{"p{line-height:3000pt}", "<p>x</p>"},
				{"p{line-height:200}", "<p>x</p>"},
				{"p{margin-left:5000pt}", "<p>x</p>"},
				{"p{margin-left:-5000pt}", "<p>x</p>"},
				{"p{margin-top:5000pt;margin-bottom:5000pt}", "<p>x</p>"},
				{"span{font-size:40000pt}", "<p><span>x</span></p>"},
				{"span{letter-spacing:2000pt}", "<p><span>x</span></p>"},
				{"span{letter-spacing:-2000pt}", "<p><span>x</span></p>"},
				{"table{margin-left:5000pt}", "<table><tr><td>x</td></tr></table>"},
				{"table{margin-left:-5000pt}", "<table><tr><td>x</td></tr></table>"},
				{"table{border:50px solid red}", "<table><tr><td>x</td></tr></table>"},
				{"td{border:50px solid red}", "<table><tr><td>x</td></tr></table>"},
				{"td{padding:3000pt}", "<table><tr><td>x</td></tr></table>"},
				{"td{width:3000pt}", "<table><tr><td>x</td></tr></table>"},
				{"tr{height:3000pt}", "<table><tr><td>x</td></tr></table>"},
				{"td{height:30in}", "<table><tr><td>x</td></tr></table>"},
				{"", "<table><tr><td><div style=\"height:3000px\">x</div></td></tr></table>"},
				{"", "<table><tr><td>" + longCell + "</td></tr></table>"},
				{"ul{padding-left:5000pt}", "<ul><li>x</li></ul>"},
				{"", nest("<blockquote>", "</blockquote>", 60, "<p>x</p>")},
				{"", nest("<div style=\"margin-left:40px\">", "</div>", 60, "<p>x</p>")},
				{"", nest("<ul><li>l", "</li></ul>", 50, "")},
				{"ul{padding-left:5000pt}", "<table><tr><td><ul><li>x</li><li><p>a</p><p>b</p></li></ul></td></tr></table>"},
				{"ul{padding-left:5000pt}", "<ul><li><p>a</p><table><tr><td>x</td></tr></table></li></ul>"},
		};
		List<String> outside = new ArrayList<String>();
		for (String[] input : inputs) {
			WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
			Converted c = convert(pkg, input[0], input[1]);
			for (Object o : c.content) {
				scan(XmlUtils.marshaltoW3CDomDocument(o).getDocumentElement(), null, input[0], outside);
			}
			NumberingDefinitionsPart ndp = pkg.getMainDocumentPart().getNumberingDefinitionsPart();
			if (ndp != null) {
				scan(XmlUtils.marshaltoW3CDomDocument(ndp.getJaxbElement()).getDocumentElement(), null,
						input[0] + " (numbering)", outside);
			}
		}
		Assert.assertTrue(outside.toString(), outside.isEmpty());
	}

	private static final Set<String> BORDERS = new HashSet<String>(Arrays.asList(
			"top", "left", "bottom", "right", "start", "end", "insideH", "insideV", "between", "bar"));

	private static void scan(Element el, Element parent, String input, List<String> outside) {
		NamedNodeMap attrs = el.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr a = (Attr) attrs.item(i);
			if (!W.equals(a.getNamespaceURI()) || !a.getValue().matches("-?\\d+")) {
				continue;
			}
			long[] range = range(el.getLocalName(), parent == null ? "" : parent.getLocalName(), a.getLocalName());
			long v = Long.parseLong(a.getValue());
			if (range != null && (v < range[0] || v > range[1])) {
				outside.add(input + ": " + el.getLocalName() + "/@" + a.getLocalName() + "=" + v);
			}
		}
		for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element) {
				scan((Element) n, el, input, outside);
			}
		}
	}

	/** CR-007 §3's table */
	private static long[] range(String el, String parent, String attr) {
		long[] i16 = {-32768, 32767}, u16 = {0, 65535}, u15 = {0, 32767};
		if (BORDERS.contains(el) && attr.equals("sz")) return new long[] {0, 255};
		if (BORDERS.contains(el) && attr.equals("space")) return new long[] {0, 31};
		if (el.equals("ind")) return i16;
		if (el.equals("spacing") && parent.equals("pPr")) return attr.equals("line") ? i16 : u16;
		if (el.equals("spacing") && parent.equals("rPr")) return i16;
		if ((el.equals("sz") || el.equals("szCs")) && attr.equals("val")) return u16;
		if (el.equals("trHeight")) return u15;
		if (el.equals("tblInd") && attr.equals("w")) return i16;
		if (attr.equals("w") && (el.equals("tcW") || el.equals("tblW") || el.equals("tblCellSpacing")
				|| el.equals("gridCol") || parent.equals("tcMar"))) return u15;
		return null;
	}

	private static String nest(String open, String close, int depth, String inner) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= depth; i++) sb.append(open.replace("<li>l", "<li>l" + i));
		sb.append(inner);
		for (int i = 0; i < depth; i++) sb.append(close);
		return sb.toString();
	}

	private static void assertWarned(Converted c, String what, String range, long written) {
		String expected = what + " is outside Word's range " + range + ", so " + written
				+ " is written (CR-007)";
		Assert.assertTrue(expected + " in: " + c.log, c.log.contains(expected));
	}

	private static int count(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
		return n;
	}

	private static final class Converted {
		List<Object> content;
		/** the content, then the numbering part */
		String xml;
		/** what the guard logged (simplelogger.properties gives only that) */
		String log;
	}

	private static Converted convert(String style, String body) throws Exception {
		return convert(WordprocessingMLPackage.createPackage(), style, body);
	}

	private static Converted convert(WordprocessingMLPackage pkg, String style, String body) throws Exception {
		Converted c = new Converted();
		PrintStream err = System.err;
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		System.setErr(new PrintStream(log, true, "UTF-8"));
		try {
			c.content = new XHTMLImporterImpl(pkg).convert(
					"<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>" + style + "</style></head><body>"
					+ body + "</body></html>", null);
		} finally {
			System.setErr(err);
		}
		// openhtmltopdf logs through java.util.logging to System.err too; pass that on
		StringBuilder guard = new StringBuilder();
		for (String line : log.toString("UTF-8").split("\\R")) {
			if (line.contains(" org.docx4j.convert.in.xhtml.WordRange - ")) {
				guard.append(line).append('\n');
			} else if (!line.isEmpty()) {
				err.println(line);
			}
		}
		c.log = guard.toString();
		StringBuilder sb = new StringBuilder();
		for (Object o : c.content) sb.append(XmlUtils.marshaltoString(o, true, true));
		NumberingDefinitionsPart ndp = pkg.getMainDocumentPart().getNumberingDefinitionsPart();
		if (ndp != null) sb.append(XmlUtils.marshaltoString(ndp.getJaxbElement(), true, true));
		c.xml = sb.toString();
		return c;
	}

}

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

import java.util.ArrayList;
import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.junit.Assert;
import org.junit.Test;

/**
 * A table with a caption comes out as the caption's paragraph(s) beside an ordinary
 * w:tbl, before it, or after it for caption-side: bottom; not as a w:tbl directly
 * inside a w:tbl (docs/change-requests/CR-002 section 4).
 */
public class TableCaptionTest {

	private static final String HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>";
	private static final String END = "</body></html>";

	@Test
	public void captionWithTheadRowspanColspan() throws Exception {
		// docx4j CR-053's repro
		List<Object> out = convert("<table><caption>CAPTION</caption>"
				+ "<thead><tr><th>H1</th><th>H2</th><th>H3</th></tr></thead>"
				+ "<tbody><tr><td rowspan='2'>A</td><td colspan='2'>B</td></tr><tr><td>C</td><td>D</td></tr></tbody>"
				+ "</table>");

		Assert.assertEquals(dump(out), 2, out.size());
		Assert.assertEquals("CAPTION", text(out.get(0)));
		Tbl tbl = assertOrdinaryTable(out.get(1));
		Assert.assertEquals(3, tbl.getTblGrid().getGridCol().size());
		Assert.assertEquals(3, rows(tbl).size());
		Assert.assertEquals("H1H2H3", text(rows(tbl).get(0)));
	}

	@Test
	public void plainCaption() throws Exception {
		List<Object> out = convert("<table><caption>CAP</caption><tr><td>A</td><td>B</td></tr></table>");

		Assert.assertEquals(dump(out), 2, out.size());
		Assert.assertEquals("CAP", text(out.get(0)));
		Assert.assertTrue(dump(out), XmlUtils.marshaltoString(out.get(0)).contains("<w:jc w:val=\"center\"/>"));
		Tbl tbl = assertOrdinaryTable(out.get(1));
		Assert.assertEquals("AB", text(tbl));
	}

	@Test
	public void captionSideBottom() throws Exception {
		List<Object> out = convert("<table><caption style='caption-side:bottom'>CAPB</caption><tr><td>A</td></tr></table>");

		Assert.assertEquals(dump(out), 2, out.size());
		Tbl tbl = assertOrdinaryTable(out.get(0));
		Assert.assertEquals("A", text(tbl));
		Assert.assertEquals("CAPB", text(out.get(1)));
	}

	@Test
	public void captionOfTwoParagraphs() throws Exception {
		List<Object> out = convert("<table><caption><p>P1</p><p>P2</p></caption><tr><td>A</td></tr></table>");

		Assert.assertEquals(dump(out), 3, out.size());
		Assert.assertEquals("P1", text(out.get(0)));
		Assert.assertEquals("P2", text(out.get(1)));
		assertOrdinaryTable(out.get(2));
	}

	@Test
	public void idOnCaptionedTable() throws Exception {
		List<Object> out = convert("<table id='t1'><caption>CAPID</caption><tr><td>A</td></tr></table>");

		String xml = dump(out);
		Assert.assertEquals(xml, 1, count(xml, "<w:bookmarkStart"));
		Assert.assertTrue(xml, xml.contains("w:name=\"t1\""));

		boolean sawTable = false;
		for (Object o : out) {
			if (XmlUtils.unwrap(o) instanceof Tbl) {
				assertOrdinaryTable(o);
				sawTable = true;
			}
		}
		Assert.assertTrue(dump(out), sawTable);
	}

	@Test
	public void captionedTableInsideCell() throws Exception {
		List<Object> out = convert("<table><tr><td>X<table><caption>INNERCAP</caption><tr><td>Y</td></tr></table></td></tr></table>");

		Assert.assertEquals(dump(out), 1, out.size());
		Tbl outer = assertOrdinaryTable(out.get(0));
		Tc tc = (Tc)XmlUtils.unwrap(rows(outer).get(0).getContent().get(0));

		List<Object> cell = new ArrayList<>();
		for (Object o : tc.getContent()) {
			if (XmlUtils.unwrap(o) instanceof P || XmlUtils.unwrap(o) instanceof Tbl) cell.add(XmlUtils.unwrap(o));
		}
		Assert.assertEquals(dump(out), "X", text(cell.get(0)));
		Assert.assertEquals(dump(out), "INNERCAP", text(cell.get(1)));
		Tbl inner = assertOrdinaryTable(cell.get(2));
		Assert.assertEquals("Y", text(inner));
		Assert.assertTrue(dump(out), cell.get(cell.size() - 1) instanceof P); // a cell ends with a w:p
	}

	/**
	 * A w:tbl with a w:tblGrid, whose content holds only rows, recursively: no
	 * w:tbl directly inside a w:tbl anywhere within it.
	 */
	private static Tbl assertOrdinaryTable(Object o) {
		Assert.assertTrue(XmlUtils.marshaltoString(o), XmlUtils.unwrap(o) instanceof Tbl);
		Tbl tbl = (Tbl)XmlUtils.unwrap(o);
		String xml = XmlUtils.marshaltoString(tbl);
		Assert.assertNotNull(xml, tbl.getTblGrid());
		for (Object child : tbl.getContent()) {
			Assert.assertTrue(xml, XmlUtils.unwrap(child) instanceof Tr);
			for (Object tc : ((Tr)XmlUtils.unwrap(child)).getContent()) {
				if (!(XmlUtils.unwrap(tc) instanceof Tc)) continue;
				for (Object inCell : ((Tc)XmlUtils.unwrap(tc)).getContent()) {
					if (XmlUtils.unwrap(inCell) instanceof Tbl) assertOrdinaryTable(inCell);
				}
			}
		}
		return tbl;
	}

	private static List<Tr> rows(Tbl tbl) {
		List<Tr> rows = new ArrayList<>();
		for (Object o : tbl.getContent()) {
			if (XmlUtils.unwrap(o) instanceof Tr) rows.add((Tr)XmlUtils.unwrap(o));
		}
		return rows;
	}

	private static String text(Object o) {
		return XmlUtils.marshaltoString(o, true, false).replaceAll("<w:instrText[^>]*>.*?</w:instrText>", "")
				.replaceAll("<[^>]+>", "");
	}

	private static int count(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
		return n;
	}

	private static List<Object> convert(String body) throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		return importer.convert(HTML + body + END, null);
	}

	private static String dump(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

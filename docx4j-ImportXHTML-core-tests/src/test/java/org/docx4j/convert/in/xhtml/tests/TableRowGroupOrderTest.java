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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;

/**
 * setTableRowGroupsInSourceOrder: a table's thead, tbody and tfoot in the order the
 * source has them, as Word imports an HTML altChunk; by default, the first thead first
 * and the first tfoot last, as openhtmltopdf (and a browser) lays them out
 * (docs/change-requests/CR-003).
 */
public class TableRowGroupOrderTest {

	private static final String HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>";
	private static final String END = "</body></html>";

	/** The source, then its rows by default, then with the option. */
	private static final String[][] CASES = {
		{"<table><thead><tr><td>HEAD</td></tr></thead><tfoot><tr><td>FOOT</td></tr></tfoot>"
				+ "<tbody><tr><td>BODY</td></tr></tbody></table>",
				"HEAD BODY FOOT", "HEAD FOOT BODY"},
		{"<table><tfoot><tr><td>FOOT</td></tr></tfoot><tbody><tr><td>BODY</td></tr></tbody></table>",
				"BODY FOOT", "FOOT BODY"},
		{"<table><tbody><tr><td>BODY</td></tr></tbody><thead><tr><td>HEAD</td></tr></thead></table>",
				"HEAD BODY", "BODY HEAD"},
		{"<table><tfoot><tr><td>F1</td></tr></tfoot><tbody><tr><td>BODY</td></tr></tbody>"
				+ "<tfoot><tr><td>F2</td></tr></tfoot></table>",
				"BODY F2 F1", "F1 BODY F2"},
		{"<table><tbody><tr><td>B1</td></tr></tbody><thead><tr><td>H1</td></tr></thead>"
				+ "<thead><tr><td>H2</td></tr></thead><tbody><tr><td>B2</td></tr></tbody></table>",
				"H1 B1 H2 B2", "B1 H1 H2 B2"},
		{"<table><tbody><tr><td>B1</td></tr></tbody><tfoot><tr><td>FOOT</td></tr></tfoot>"
				+ "<tbody><tr><td>B2</td></tr></tbody></table>",
				"B1 B2 FOOT", "B1 FOOT B2"},
		// rows directly in the table: anonymous row groups, placed by their first row
		{"<table><tfoot><tr><td>FOOT</td></tr></tfoot><tr><td>R1</td></tr><tr><td>R2</td></tr></table>",
				"R1 R2 FOOT", "FOOT R1 R2"},
		{"<table><tr><td>R1</td></tr><tfoot><tr><td>FOOT</td></tr></tfoot><tr><td>R2</td></tr></table>",
				"R1 R2 FOOT", "R1 FOOT R2"},
		{"<table><tbody style='display: table-footer-group'><tr><td>CSSFOOT</td></tr></tbody>"
				+ "<tbody><tr><td>BODY</td></tr></tbody></table>",
				"BODY CSSFOOT", "CSSFOOT BODY"},
		{"<table><caption>CAP</caption><tfoot><tr><td>FOOT</td></tr></tfoot>"
				+ "<tbody><tr><td>BODY</td></tr></tbody></table>",
				"BODY FOOT", "FOOT BODY"},
		{"<table><tfoot><tr><td rowspan='2'>FA</td><td>FB</td></tr><tr><td>FC</td></tr></tfoot>"
				+ "<tbody><tr><td>B1</td><td>B2</td></tr></tbody></table>",
				"B1|B2 FA|FB |FC", "FA|FB |FC B1|B2"},
	};

	@Test
	public void offByDefault() throws Exception {
		Assert.assertFalse(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage())
				.isTableRowGroupsInSourceOrder());
	}

	@Test
	public void rowOrder() throws Exception {
		for (String[] c : CASES) {
			Assert.assertEquals(c[0], c[1], order(table(convert(c[0], false))));
			Assert.assertEquals(c[0], c[2], order(table(convert(c[0], true))));
		}
	}

	/**
	 * Only the order changes: each row, its cells, their merges and borders, are the
	 * same either way.
	 */
	@Test
	public void rowsUnchanged() throws Exception {
		for (String[] c : CASES) {
			Assert.assertEquals(c[0], sortedRows(table(convert(c[0], false))),
					sortedRows(table(convert(c[0], true))));
		}
	}

	@Test
	public void rowspanInTfoot() throws Exception {
		List<Tr> rows = rows(table(convert(CASES[CASES.length - 1][0], true)));
		Assert.assertEquals("restart", cell(rows.get(0), 0).getTcPr().getVMerge().getVal());
		Assert.assertNotNull(cell(rows.get(1), 0).getTcPr().getVMerge());
		Assert.assertNull(cell(rows.get(2), 0).getTcPr().getVMerge());
	}

	@Test
	public void nestedTable() throws Exception {
		String inner = "<table><tfoot><tr><td>IFOOT</td></tr></tfoot><tbody><tr><td>IBODY</td></tr></tbody></table>";
		for (boolean sourceOrder : new boolean[] {false, true}) {
			Tr outer = rows(table(convert("<table><tr><td>" + inner + "</td></tr></table>", sourceOrder))).get(0);
			Tbl tbl = null;
			for (Object o : cell(outer, 0).getContent()) {
				if (XmlUtils.unwrap(o) instanceof Tbl) tbl = (Tbl)XmlUtils.unwrap(o);
			}
			Assert.assertEquals(sourceOrder ? "IFOOT IBODY" : "IBODY IFOOT", order(tbl));
		}
	}

	/**
	 * docx4j CR-053's r11-8, as docx4j reads an altChunk: jsoup, W3CDom, convert(Node),
	 * nothing fetched, display:none hidden.  Word 365 keeps the hidden tfoot where it
	 * stands, its row w:hidden.
	 */
	@Test
	public void hiddenTfootThroughJsoup() throws Exception {
		String html = "<html><head><style></style></head><body><table border='1'>"
				+ "<thead><tr><td>R11_8_HEAD</td></tr></thead>"
				+ "<tfoot style='display:none'><tr><td>R11_8_FOOT</td></tr></tfoot>"
				+ "<tbody><tr><td>R11_8_BODY</td></tr></tbody></table><p>R11_8_VISIBLE</p></body></html>";

		XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		importer.setExternalResourcesAllowed(false);
		importer.setDisplayNoneAsHidden(true);
		importer.setTableRowGroupsInSourceOrder(true);
		Tbl tbl = table(importer.convert(W3CDom.convert(Jsoup.parse(html)), null));

		Assert.assertEquals("R11_8_HEAD R11_8_FOOT R11_8_BODY", order(tbl));
		List<Tr> rows = rows(tbl);
		Assert.assertFalse(hidden(rows.get(0)));
		Assert.assertTrue(hidden(rows.get(1)));
		Assert.assertFalse(hidden(rows.get(2)));

		List<String> keys = new ArrayList<>();
		for (Element e : importer.getHiddenContent().keySet()) keys.add(e.getNodeName());
		Assert.assertEquals(Arrays.asList("tfoot"), keys);
	}

	private static List<Object> convert(String table, boolean sourceOrder) throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		importer.setTableRowGroupsInSourceOrder(sourceOrder);
		return importer.convert(HTML + table + END, null);
	}

	private static Tbl table(List<Object> content) {
		for (Object o : content) {
			if (XmlUtils.unwrap(o) instanceof Tbl) return (Tbl)XmlUtils.unwrap(o);
		}
		Assert.fail("no table");
		return null;
	}

	private static List<Tr> rows(Tbl tbl) {
		List<Tr> rows = new ArrayList<>();
		for (Object o : tbl.getContent()) {
			if (XmlUtils.unwrap(o) instanceof Tr) rows.add((Tr)XmlUtils.unwrap(o));
		}
		return rows;
	}

	private static Tc cell(Tr tr, int i) {
		List<Tc> cells = new ArrayList<>();
		for (Object o : tr.getContent()) {
			if (XmlUtils.unwrap(o) instanceof Tc) cells.add((Tc)XmlUtils.unwrap(o));
		}
		return cells.get(i);
	}

	/** Each row's cells' text, joined by |; the rows by a space. */
	private static String order(Tbl tbl) {
		List<String> rows = new ArrayList<>();
		for (Tr tr : rows(tbl)) {
			List<String> cells = new ArrayList<>();
			for (Object o : tr.getContent()) {
				if (XmlUtils.unwrap(o) instanceof Tc) cells.add(text(o).trim());
			}
			rows.add(String.join("|", cells));
		}
		return String.join(" ", rows);
	}

	private static List<String> sortedRows(Tbl tbl) {
		List<String> rows = new ArrayList<>();
		for (Tr tr : rows(tbl)) rows.add(XmlUtils.marshaltoString(tr, true, false));
		Collections.sort(rows);
		return rows;
	}

	private static boolean hidden(Tr tr) {
		if (tr.getTrPr() == null) return false;
		for (JAXBElement<?> j : tr.getTrPr().getCnfStyleOrDivIdOrGridBefore()) {
			if (j.getName().getLocalPart().equals("hidden")) return true;
		}
		return false;
	}

	private static String text(Object o) {
		return XmlUtils.marshaltoString(o, true, false).replaceAll("<[^>]+>", "");
	}
}

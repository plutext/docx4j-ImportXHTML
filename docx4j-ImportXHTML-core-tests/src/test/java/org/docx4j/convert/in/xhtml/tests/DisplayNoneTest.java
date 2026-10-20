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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.xml.bind.JAXBElement;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.FormattingOption;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * setDisplayNoneAsHidden(true): what display: none hides comes out as hidden text
 * (w:vanish), and getHiddenContent() reports it by the outermost hidden element it
 * came from (docs/change-requests/CR-002 sections 2 and 3).
 */
public class DisplayNoneTest {

	private static final String HEAD = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>";
	private static final String BODY = "</head><body>";
	private static final String END = "</body></html>";

	/** docx4j CR-053 section 1's chunk */
	private static final String CR053 = HEAD + "<style>.h { display: none; }</style>" + BODY
			+ "<p>Before</p>"
			+ "<p id=\"inline\" style=\"display:none\">INLINE_NONE</p>"
			+ "<div id=\"cls\" class=\"h\"><p>CLASS_NONE</p></div>"
			+ "<p>Visible <span id=\"span\" style=\"display:none\">SPAN_NONE</span> text</p>"
			+ END;

	@Test
	public void offByDefault() throws Exception {
		XHTMLImporterImpl importer = importer();
		Assert.assertFalse(importer.isDisplayNoneAsHidden());
		String byDefault = marshal(importer.convert(CR053, null));
		Assert.assertTrue(importer.getHiddenContent().isEmpty());

		importer = importer();
		importer.setDisplayNoneAsHidden(false);
		String off = marshal(importer.convert(CR053, null));

		Assert.assertEquals(byDefault, off);
		Assert.assertFalse(off, off.contains("vanish"));
		Assert.assertFalse(off, off.contains("_NONE"));
		Assert.assertTrue(off, off.contains("Before"));
		Assert.assertTrue(importer.getHiddenContent().isEmpty());
	}

	@Test
	public void cr053Chunk() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(CR053, null);

		Assert.assertEquals(Arrays.asList("Before", "Visible ", " text"), runs(out, false));
		Assert.assertEquals(Arrays.asList("INLINE_NONE", "CLASS_NONE", "SPAN_NONE"), runs(out, true));

		// a hidden block gives a hidden paragraph, its mark included; a hidden span, a
		// hidden run in a visible paragraph
		List<P> ps = paragraphs(out);
		Assert.assertEquals(4, ps.size());
		Assert.assertFalse(markHidden(ps.get(0)));
		Assert.assertTrue(markHidden(ps.get(1)));
		Assert.assertTrue(markHidden(ps.get(2)));
		Assert.assertFalse(markHidden(ps.get(3)));

		Map<Element, List<Object>> hidden = importer.getHiddenContent();
		Assert.assertEquals(Arrays.asList("inline", "cls", "span"), ids(hidden));
		List<List<Object>> values = new ArrayList<>(hidden.values());
		Assert.assertEquals(Collections.singletonList(ps.get(1)), values.get(0));
		Assert.assertEquals(Collections.singletonList(ps.get(2)), values.get(1));
		Assert.assertEquals(1, values.get(2).size());
		Assert.assertTrue(values.get(2).get(0) instanceof R);
		Assert.assertEquals("SPAN_NONE", text(values.get(2).get(0)));
	}

	@Test
	public void nestedIsOneKey() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD + BODY
				+ "<div id=\"outer\" style=\"display:none\"><p>A <span style=\"display:none\">B</span></p>"
				+ "<div style=\"display:none\"><p>C</p></div></div>"
				+ "<p>after</p>" + END, null);

		Assert.assertEquals(Arrays.asList("A ", "B", "C"), runs(out, true));
		Assert.assertEquals(Arrays.asList("after"), runs(out, false));
		Assert.assertEquals(Arrays.asList("outer"), ids(importer.getHiddenContent()));
		List<Object> value = importer.getHiddenContent().values().iterator().next();
		Assert.assertEquals(2, value.size());
		Assert.assertEquals("A B", text(value.get(0)));
		Assert.assertEquals("C", text(value.get(1)));
	}

	@Test
	public void important() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD
				+ "<style>.imp { display: none !important; } .shown { display: block !important; }</style>" + BODY
				+ "<p id=\"rule\" class=\"imp\" style=\"display:block\">RULE</p>"
				+ "<p id=\"attr\" class=\"shown\" style=\"display:none !important\">ATTR</p>"
				+ "<p>visible</p>" + END, null);

		Assert.assertEquals(Arrays.asList("RULE", "ATTR"), runs(out, true));
		Assert.assertEquals(Arrays.asList("rule", "attr"), ids(importer.getHiddenContent()));
	}

	@Test
	public void mediaAsOpenhtmltopdfHasIt() throws Exception {
		// print media, as the renderer is set up: an @media print rule applies, an @media
		// screen rule doesn't.  (Word applies neither; a caller who wants that rewrites
		// the style elements first.)
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD
				+ "<style>@media print { .pr { display: none; } } @media screen { .sc { display: none; } }</style>"
				+ "<style media=\"screen\">.ms { display: none; }</style>" + BODY
				+ "<p id=\"pr\" class=\"pr\">PRINT</p><p class=\"sc\">SCREEN</p><p class=\"ms\">MEDIASCREEN</p>" + END, null);

		Assert.assertEquals(Arrays.asList("PRINT"), runs(out, true));
		Assert.assertEquals(Arrays.asList("SCREEN", "MEDIASCREEN"), runs(out, false));
		Assert.assertEquals(Arrays.asList("pr"), ids(importer.getHiddenContent()));
	}

	@Test
	public void explicitDisplayRevealsAsInWord() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD + BODY
				+ "<p>before</p>"
				+ "<div id=\"div\" style=\"display:none\">OWN "
				+ "<span style=\"display:inline\">SHOWSPAN</span> "
				+ "<span style=\"display:inherit\">INHERIT</span>"
				+ "<p style=\"display:block\">SHOWP <span id=\"again\" style=\"display:none\">AGAIN</span></p>"
				+ "</div>" + END, null);

		Assert.assertEquals(Arrays.asList("before", "SHOWSPAN", "SHOWP "), runs(out, false));
		List<String> hidden = runs(out, true);
		Assert.assertTrue(hidden.toString(), hidden.containsAll(Arrays.asList("OWN ", "INHERIT", "AGAIN")));
		Assert.assertEquals(Arrays.asList("div", "again"), ids(importer.getHiddenContent()));

		List<Object> again = importer.getHiddenContent().get(element(importer, "again"));
		Assert.assertEquals(1, again.size());
		Assert.assertEquals("AGAIN", text(again.get(0)));

		// the p which shows again is a visible paragraph
		for (P p : paragraphs(out)) {
			if (text(p).startsWith("SHOWP")) Assert.assertFalse(markHidden(p));
		}
	}

	@Test
	public void headAndScriptUntouched() throws Exception {
		Document doc = parse(HEAD + "<title>TITLE</title><style>script { display: none; }</style>" + BODY
				+ "<p>visible</p><script style=\"display:none\">var x;</script>" + END);
		XHTMLImporterImpl importer = hiding();
		String xml = marshal(importer.convert(doc, null));

		Assert.assertFalse(xml, xml.contains("TITLE"));
		Assert.assertFalse(xml, xml.contains("var x"));
		Assert.assertFalse(xml, xml.contains("vanish"));
		Assert.assertTrue(importer.getHiddenContent().isEmpty());
	}

	@Test
	public void listItemCellAndRowKeepTheirPlace() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD + "<style>.h { display: none; }</style>" + BODY
				+ "<ol><li>one</li><li id=\"li\" class=\"h\">two</li><li>three</li></ol>"
				+ "<table><tr><td>a</td><td id=\"td\" style=\"display:none\">b</td></tr>"
				+ "<tr id=\"tr\" style=\"display:none\"><td>c</td><td>d</td></tr></table>" + END, null);

		// the li is kept and numbered, its paragraph hidden
		List<P> ps = paragraphs(out);
		Assert.assertEquals("two", text(ps.get(1)));
		Assert.assertNotNull(ps.get(1).getPPr().getNumPr());
		Assert.assertTrue(markHidden(ps.get(1)));
		Assert.assertFalse(markHidden(ps.get(2)));

		// the cell keeps its column, and the row its place
		Tbl tbl = tables(out).get(0);
		Assert.assertEquals(2, tbl.getTblGrid().getGridCol().size());
		List<Tr> rows = rows(tbl);
		Assert.assertEquals(2, rows.size());
		int cells = 0;
		for (Object o : rows.get(0).getContent()) {
			if (XmlUtils.unwrap(o) instanceof Tc) cells++;
		}
		Assert.assertEquals(2, cells);
		Assert.assertFalse(rowHidden(rows.get(0)));
		Assert.assertTrue(rowHidden(rows.get(1)));

		Assert.assertEquals(Arrays.asList("b", "c", "d"), runs(Collections.singletonList(tbl), true));
		Assert.assertEquals(Arrays.asList("a"), runs(Collections.singletonList(tbl), false));
		Assert.assertEquals(Arrays.asList("li", "td", "tr"), ids(importer.getHiddenContent()));
	}

	@Test
	public void hiddenTableAndRowGroups() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD + "<style>.h { display: none; }</style>" + BODY
				+ "<p>x</p>"
				+ "<table id=\"table\" style=\"display:none\"><tr><td>T1</td></tr><tr><td>T2</td></tr></table>"
				+ "<table><thead id=\"thead\" style=\"display:none\"><tr><td>H</td></tr></thead>"
				+ "<tbody><tr><td>B</td></tr></tbody>"
				+ "<tbody id=\"tbody\" class=\"h\"><tr><td>B2</td></tr></tbody></table>" + END, null);

		Tbl hiddenTable = tables(out).get(0);
		for (Tr tr : rows(hiddenTable)) Assert.assertTrue(rowHidden(tr));
		Assert.assertEquals(Arrays.asList("T1", "T2"), runs(Collections.singletonList(hiddenTable), true));
		for (P p : paragraphs(Collections.singletonList(hiddenTable))) Assert.assertTrue(markHidden(p));

		Tbl table = tables(out).get(1);
		List<Tr> rows = rows(table);
		Assert.assertEquals(3, rows.size());
		Assert.assertTrue(rowHidden(rows.get(0)));
		Assert.assertFalse(rowHidden(rows.get(1)));
		Assert.assertTrue(rowHidden(rows.get(2)));
		Assert.assertEquals(Arrays.asList("H", "B2"), runs(Collections.singletonList(table), true));

		Map<Element, List<Object>> hidden = importer.getHiddenContent();
		Assert.assertEquals(Arrays.asList("table", "thead", "tbody"), ids(hidden));
		// a wholly hidden table is reported as the table, not its paragraphs
		Assert.assertEquals(Collections.singletonList(hiddenTable), hidden.values().iterator().next());
	}

	@Test
	public void hiddenHyperlinkAndImage() throws Exception {
		String png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD + BODY
				+ "<p>a <a id=\"a\" href=\"http://example.com/\" style=\"display:none\">LINK</a> b"
				+ "<img id=\"img\" style=\"display:none\" src=\"" + png + "\"/></p>" + END, null);

		Assert.assertEquals(Arrays.asList("LINK"), runs(out, true));
		Assert.assertEquals(Arrays.asList("a ", " b"), runs(out, false));
		Assert.assertFalse(markHidden(paragraphs(out).get(0)));

		Map<Element, List<Object>> hidden = importer.getHiddenContent();
		Assert.assertEquals(Arrays.asList("a", "img"), ids(hidden));
		R image = (R)hidden.get(element(importer, "img")).get(0);
		Assert.assertTrue(XmlUtils.marshaltoString(image).contains("drawing"));
		Assert.assertNotNull(image.getRPr().getVanish());
	}

	@Test
	public void elementWithNoContentIsAbsent() throws Exception {
		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(HEAD + BODY
				+ "<p>x<span id=\"empty\" style=\"display:none\"></span>y</p>"
				+ "<p>z<span id=\"full\" style=\"display:none\">Z</span></p>" + END, null);

		Assert.assertEquals(Arrays.asList("x", "y", "z"), runs(out, false));
		Assert.assertEquals(Arrays.asList("full"), ids(importer.getHiddenContent()));
	}

	@Test
	public void callersDocumentRestored() throws Exception {
		String xhtml = HEAD + "<style>.h { display: none; }</style>" + BODY
				+ "<p style=\"display:none\">A</p>"
				+ "<p style=\"color:red; display:none\">B</p>"
				+ "<p style=\"display:none;float:none !important\">C</p>"
				+ "<div class=\"h\"><p>D</p></div>"
				+ "<p style=\"\" class=\"h\">E</p>"
				+ END;
		Document doc = parse(xhtml);
		String before = XmlUtils.w3CDomNodeToString(doc);

		XHTMLImporterImpl importer = hiding();
		List<Object> out = importer.convert(doc, null);

		Assert.assertEquals(before, XmlUtils.w3CDomNodeToString(doc));
		Assert.assertEquals(Arrays.asList("A", "B", "C", "D", "E"), runs(out, true));

		// the keys are the caller's elements
		NodeList ps = doc.getElementsByTagName("p");
		Map<Element, List<Object>> hidden = importer.getHiddenContent();
		Assert.assertEquals(5, hidden.size());
		Assert.assertTrue(hidden.containsKey(ps.item(0)));
		Assert.assertTrue(hidden.containsKey(doc.getElementsByTagName("div").item(0)));
		Assert.assertFalse(hidden.containsKey(ps.item(3))); // D, through the div
	}

	@Test
	public void everyFormattingOption() throws Exception {
		for (FormattingOption option : FormattingOption.values()) {
			XHTMLImporterImpl importer = hiding();
			importer.setRunFormatting(option);
			importer.setParagraphFormatting(option);
			importer.setTableFormatting(option);
			List<Object> out = importer.convert(CR053, null);

			Assert.assertEquals(option.toString(), Arrays.asList("INLINE_NONE", "CLASS_NONE", "SPAN_NONE"), runs(out, true));
			List<P> ps = paragraphs(out);
			Assert.assertTrue(option.toString(), markHidden(ps.get(1)));
			Assert.assertTrue(option.toString(), markHidden(ps.get(2)));
			Assert.assertFalse(option.toString(), markHidden(ps.get(3)));
			Assert.assertEquals(option.toString(), Arrays.asList("inline", "cls", "span"), ids(importer.getHiddenContent()));
		}
	}

	@Test
	public void eachConvertReplacesTheMap() throws Exception {
		XHTMLImporterImpl importer = hiding();
		importer.convert(CR053, null);
		Assert.assertEquals(3, importer.getHiddenContent().size());
		importer.convert(HEAD + BODY + "<p>nothing hidden</p>" + END, null);
		Assert.assertTrue(importer.getHiddenContent().isEmpty());
	}

	private static XHTMLImporterImpl importer() throws Exception {
		return new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
	}

	private static XHTMLImporterImpl hiding() throws Exception {
		XHTMLImporterImpl importer = importer();
		importer.setDisplayNoneAsHidden(true);
		return importer;
	}

	private static Document parse(String xhtml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xhtml.getBytes(StandardCharsets.UTF_8)));
	}

	private static List<String> ids(Map<Element, List<Object>> hidden) {
		List<String> ids = new ArrayList<>();
		for (Element e : hidden.keySet()) ids.add(e.getAttribute("id"));
		return ids;
	}

	private static Element element(XHTMLImporterImpl importer, String id) {
		for (Element e : importer.getHiddenContent().keySet()) {
			if (id.equals(e.getAttribute("id"))) return e;
		}
		throw new AssertionError("no key " + id);
	}

	/** The texts of the runs which are hidden (or not), in order */
	private static List<String> runs(List<Object> content, boolean hidden) {
		List<String> texts = new ArrayList<>();
		for (Object o : content) collectRuns(o, hidden, texts);
		return texts;
	}

	private static void collectRuns(Object o, boolean hidden, List<String> texts) {
		o = XmlUtils.unwrap(o);
		if (o instanceof R) {
			R r = (R)o;
			boolean vanish = r.getRPr()!=null && r.getRPr().getVanish()!=null;
			String text = text(r);
			if (vanish==hidden && !text.trim().isEmpty()) texts.add(text);
		} else if (o instanceof ContentAccessor) {
			for (Object child : ((ContentAccessor)o).getContent()) collectRuns(child, hidden, texts);
		}
	}

	private static List<P> paragraphs(List<Object> content) {
		List<P> ps = new ArrayList<>();
		for (Object o : content) collectParagraphs(o, ps);
		return ps;
	}

	private static void collectParagraphs(Object o, List<P> ps) {
		o = XmlUtils.unwrap(o);
		if (o instanceof P) {
			ps.add((P)o);
		} else if (o instanceof ContentAccessor && !(o instanceof R)) {
			for (Object child : ((ContentAccessor)o).getContent()) collectParagraphs(child, ps);
		}
	}

	/** The top-level tables (the content has bookmarks too, for the ids) */
	private static List<Tbl> tables(List<Object> content) {
		List<Tbl> tables = new ArrayList<>();
		for (Object o : content) {
			if (XmlUtils.unwrap(o) instanceof Tbl) tables.add((Tbl)XmlUtils.unwrap(o));
		}
		return tables;
	}

	private static List<Tr> rows(Tbl tbl) {
		List<Tr> rows = new ArrayList<>();
		for (Object o : tbl.getContent()) {
			if (XmlUtils.unwrap(o) instanceof Tr) rows.add((Tr)XmlUtils.unwrap(o));
		}
		return rows;
	}

	private static boolean markHidden(P p) {
		return p.getPPr()!=null && p.getPPr().getRPr()!=null && p.getPPr().getRPr().getVanish()!=null;
	}

	private static boolean rowHidden(Tr tr) {
		if (tr.getTrPr()==null) return false;
		for (JAXBElement<?> el : tr.getTrPr().getCnfStyleOrDivIdOrGridBefore()) {
			if (el.getName().getLocalPart().equals("hidden")) return true;
		}
		return false;
	}

	private static String text(Object o) {
		return XmlUtils.marshaltoString(o, true, false).replaceAll("<w:instrText[^>]*>.*?</w:instrText>", "")
				.replaceAll("<[^>]+>", "");
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

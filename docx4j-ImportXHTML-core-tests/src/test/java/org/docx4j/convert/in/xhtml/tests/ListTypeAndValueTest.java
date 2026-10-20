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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.model.listnumbering.ListLevel;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.Lvl;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.Numbering;
import org.docx4j.wml.Numbering.Num.LvlOverride;
import org.docx4j.wml.P;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * An ol's type attribute, an li's value, and an ol's start where a nested list
 * restarts (docs/change-requests/CR-002 section 6.1).
 */
public class ListTypeAndValueTest {

	private static final String HEAD = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>";
	private static final String BODY = "</head><body>";
	private static final String END = "</body></html>";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	@Test
	public void olType() throws Exception {
		String[][] cases = {
				{"1", "DECIMAL"}, {"a", "LOWER_LETTER"}, {"A", "UPPER_LETTER"},
				{"i", "LOWER_ROMAN"}, {"I", "UPPER_ROMAN"}, {"x", "DECIMAL"}};
		for (String[] c : cases) {
			setup();
			List<P> ps = convert(HEAD + BODY + "<ol type=\"" + c[0] + "\"><li>one</li><li>two</li></ol>" + END);
			Assert.assertEquals("type=" + c[0], NumberFormat.valueOf(c[1]), numFmt(ps.get(0), 0));
		}
	}

	@Test
	public void cssBeatsType() throws Exception {
		List<P> ps = convert(HEAD + "<style>.dec { list-style-type: decimal; }</style>" + BODY
				+ "<ol type=\"a\" style=\"list-style-type: upper-roman\"><li>one</li></ol>"
				+ "<ol type=\"a\" class=\"dec\"><li>two</li></ol>"
				+ END);
		Assert.assertEquals(NumberFormat.UPPER_ROMAN, numFmt(ps.get(0), 0));
		Assert.assertEquals(NumberFormat.DECIMAL, numFmt(ps.get(1), 0));
	}

	@Test
	public void liValue() throws Exception {
		List<P> ps = convert(HEAD + BODY
				+ "<ol><li>one</li><li value=\"7\">seven</li><li>eight</li></ol>" + END);

		BigInteger first = numId(ps.get(0));
		BigInteger seventh = numId(ps.get(1));
		Assert.assertNotEquals(first, seventh);
		Assert.assertEquals(seventh, numId(ps.get(2))); // and on from 7

		LvlOverride override = override(seventh, 0);
		Assert.assertNotNull(override);
		Assert.assertEquals(BigInteger.valueOf(7), override.getStartOverride().getVal());
		Assert.assertEquals(abstractNumId(first), abstractNumId(seventh));
	}

	@Test
	public void liValueKeepsTheLevelsFormat() throws Exception {
		// the second nested list has its own override (its numFmt); a value in it keeps that
		List<P> ps = convert(HEAD + BODY
				+ "<ol><li>one"
				+ "<ol style=\"list-style-type: lower-alpha\"><li>a</li></ol>"
				+ "<ol style=\"list-style-type: upper-roman\"><li>I</li><li value=\"5\">V</li></ol>"
				+ "</li></ol>" + END);

		Assert.assertEquals("V", text(ps.get(3)));
		Assert.assertNotEquals(numId(ps.get(2)), numId(ps.get(3)));
		LvlOverride override = override(numId(ps.get(3)), 1);
		Assert.assertEquals(BigInteger.valueOf(5), override.getStartOverride().getVal());
		Assert.assertEquals(NumberFormat.UPPER_ROMAN, override.getLvl().getNumFmt().getVal());
	}

	@Test
	public void liValueIgnoredInUl() throws Exception {
		List<P> ps = convert(HEAD + BODY + "<ul><li>one</li><li value=\"7\">two</li></ul>" + END);
		Assert.assertEquals(numId(ps.get(0)), numId(ps.get(1)));
	}

	@Test
	public void startWhereANestedListRestarts() throws Exception {
		List<P> ps = convert(HEAD + BODY
				+ "<ol><li>foo"
				+ "<ol style=\"list-style-type: lower-alpha\"><li>A</li></ol>"
				+ "<ol start=\"3\" style=\"list-style-type: lower-alpha\"><li>C</li></ol>"
				+ "</li></ol>" + END);

		Assert.assertEquals("C", text(ps.get(2)));
		LvlOverride override = override(numId(ps.get(2)), 1);
		Assert.assertEquals(BigInteger.valueOf(3), override.getStartOverride().getVal());
	}

	/**
	 * docx4j's NumberingDefinitionsPart caches a definition per w:num, made from its
	 * abstract numbering's levels as they were then, but the importer adds levels after
	 * the w:num.  So after a convert the cache must have been rebuilt, or docx4j's own
	 * numbering (its Emulator, its text extraction) finds no levels for these lists.
	 * Found by docx4j CR-053 after an li value; true of every list before.
	 */
	@Test
	public void docx4jsNumberingModelSeesEveryList() throws Exception {
		String[] cases = {
				"<ol><li>ONE</li><li value=\"7\">SEVEN</li></ol>"
					+ "<ol start=\"4\" style=\"list-style-type: lower-roman\"><li>FOUR</li></ol><ul><li>BULLET</li></ul>",
				"<ol><li>ONE</li><li>TWO</li></ol>"
					+ "<ol start=\"4\" style=\"list-style-type: lower-roman\"><li>FOUR</li></ol><ul><li>BULLET</li></ul>"};
		for (String lists : cases) {
			setup();
			List<P> ps = convert(HEAD + BODY + lists + END);

			Map<String, ListNumberingDefinition> defs = ndp().getInstanceListDefinitions(); // as cached
			for (P p : ps) {
				ListNumberingDefinition def = defs.get(numId(p).toString());
				Assert.assertNotNull(text(p), def);
				Assert.assertTrue(text(p), def.levelExists(0));
			}
			ListLevel four = defs.get(numId(ps.get(2)).toString()).getLevel("0");
			Assert.assertEquals(NumberFormat.LOWER_ROMAN, four.getNumFmt());
			Assert.assertEquals(NumberFormat.BULLET, defs.get(numId(ps.get(3)).toString()).getLevel("0").getNumFmt());
		}
	}

	private List<P> convert(String xhtml) throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		List<P> ps = new ArrayList<>();
		for (Object o : importer.convert(xhtml, null)) {
			if (XmlUtils.unwrap(o) instanceof P) ps.add((P)XmlUtils.unwrap(o));
		}
		return ps;
	}

	private NumberingDefinitionsPart ndp() {
		return wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart();
	}

	private static BigInteger numId(P p) {
		return p.getPPr().getNumPr().getNumId().getVal();
	}

	private Numbering.Num num(BigInteger numId) {
		for (Numbering.Num num : ndp().getJaxbElement().getNum()) {
			if (num.getNumId().equals(numId)) return num;
		}
		throw new AssertionError("no num " + numId);
	}

	private BigInteger abstractNumId(BigInteger numId) {
		return num(numId).getAbstractNumId().getVal();
	}

	private LvlOverride override(BigInteger numId, int ilvl) {
		for (LvlOverride lo : num(numId).getLvlOverride()) {
			if (lo.getIlvl().intValue()==ilvl) return lo;
		}
		return null;
	}

	/** The level's numFmt, from the num's override, else its abstract numbering */
	private NumberFormat numFmt(P p, int ilvl) {
		BigInteger numId = numId(p);
		LvlOverride lo = override(numId, ilvl);
		if (lo!=null && lo.getLvl()!=null && lo.getLvl().getNumFmt()!=null) {
			return lo.getLvl().getNumFmt().getVal();
		}
		for (Numbering.AbstractNum an : ndp().getJaxbElement().getAbstractNum()) {
			if (an.getAbstractNumId().equals(abstractNumId(numId))) {
				for (Lvl lvl : an.getLvl()) {
					if (lvl.getIlvl().intValue()==ilvl) return lvl.getNumFmt().getVal();
				}
			}
		}
		throw new AssertionError("no level " + ilvl);
	}

	private static String text(P p) {
		return XmlUtils.marshaltoString(p, true, false).replaceAll("<[^>]+>", "");
	}
}

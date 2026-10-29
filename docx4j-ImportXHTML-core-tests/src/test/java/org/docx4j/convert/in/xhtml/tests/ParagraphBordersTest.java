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

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.STBorder;
import org.junit.Test;

/**
 * A paragraph's CSS border-style as its w:pBdr side's w:val (ParagraphBorderHelper), as
 * TableBordersTest.testTableBorderStyles does for tables.
 */
public class ParagraphBordersTest {

	@Test public void testParagraphBorderStyles() throws Exception {
		Map<String, STBorder> styles = new LinkedHashMap<String, STBorder>();
		styles.put("solid", STBorder.SINGLE);
		styles.put("dotted", STBorder.DOTTED);
		styles.put("dashed", STBorder.DASHED);
		styles.put("double", STBorder.DOUBLE);
		styles.put("inset", STBorder.INSET);
		styles.put("outset", STBorder.OUTSET);
		styles.put("groove", STBorder.SINGLE);	// no direct substitution
		styles.put("ridge", STBorder.SINGLE);	// no direct substitution

		for (Map.Entry<String, STBorder> style : styles.entrySet()) {
			XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
			P p = (P) importer.convert("<div><p style='border: 1px " + style.getKey() + " black;'>1</p></div>", "").get(0);
			assertEquals(style.getKey(), style.getValue(), p.getPPr().getPBdr().getTop().getVal());
		}
	}
}

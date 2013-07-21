/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2013, Plutext Pty Ltd, and contributors.
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
package org.docx4j.convert.in.xhtml;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class TableBordersTest {

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	private List<Object> convert(String xhtml) throws Docx4JException {
		return XHTMLImporter.convert(xhtml, "", wordMLPackage);
	}

	private Tbl table(String tableContent) throws Docx4JException {
		List<Object> converted = convert("<div>" +tableContent + "</div>");
		return (Tbl) converted.get(1);
	}
	
	@Test public void testTableBorderAbsence() throws Docx4JException {
		Tbl tbl = table("<table><tr><td>1</td></tr></table>");
		TblBorders borders = tbl.getTblPr().getTblBorders();
		assertEquals(STBorder.NONE, borders.getTop().getVal());
		assertEquals(STBorder.NONE, borders.getBottom().getVal());
		assertEquals(STBorder.NONE, borders.getLeft().getVal());
		assertEquals(STBorder.NONE, borders.getRight().getVal());
		assertEquals(STBorder.NONE, borders.getInsideH().getVal());
		assertEquals(STBorder.NONE, borders.getInsideV().getVal());

		TcPrInner.TcBorders borders2 = ((Tc) ((Tr) tbl.getContent().get(0)).getContent().get(0)).getTcPr().getTcBorders();
		assertNull(borders2.getTop());
		assertNull(borders2.getBottom());
		assertNull(borders2.getLeft());
		assertNull(borders2.getRight());
		assertNull(borders2.getInsideH());
		assertNull(borders2.getInsideV());
	}

	@Test public void testTableBorderAbsenceExplicit() throws Docx4JException {
		Tbl tbl = table("<table border='0'><tr><td>1</td></tr></table>");
		TblBorders borders = tbl.getTblPr().getTblBorders();
		assertEquals(STBorder.NONE, borders.getTop().getVal());
		assertEquals(STBorder.NONE, borders.getBottom().getVal());
		assertEquals(STBorder.NONE, borders.getLeft().getVal());
		assertEquals(STBorder.NONE, borders.getRight().getVal());
		assertEquals(STBorder.NONE, borders.getInsideH().getVal());
		assertEquals(STBorder.NONE, borders.getInsideV().getVal());

		TcPrInner.TcBorders borders2 = ((Tc) ((Tr) tbl.getContent().get(0)).getContent().get(0)).getTcPr().getTcBorders();
		assertNull(borders2.getTop());
		assertNull(borders2.getBottom());
		assertNull(borders2.getLeft());
		assertNull(borders2.getRight());
		assertNull(borders2.getInsideH());
		assertNull(borders2.getInsideV());
	}
	
	@Test public void testTableBorderPresence() throws Docx4JException {
		Tbl tbl = table("<table border='1'><tr><th>1</th></tr></table>");
		TblBorders borders = tbl.getTblPr().getTblBorders();
		assertEquals(STBorder.INSET, borders.getTop().getVal());
		assertEquals(STBorder.INSET, borders.getBottom().getVal());
		assertEquals(STBorder.INSET, borders.getLeft().getVal());
		assertEquals(STBorder.INSET, borders.getRight().getVal());
		assertEquals(STBorder.NONE, borders.getInsideH().getVal());
		assertEquals(STBorder.NONE, borders.getInsideV().getVal());
		assertTrue(borders.getTop().getSz().longValue() > 0);
		assertTrue(borders.getBottom().getSz().longValue() > 0);
		assertTrue(borders.getLeft().getSz().longValue() > 0);
		assertTrue(borders.getRight().getSz().longValue() > 0);

		TcPrInner.TcBorders borders2 = ((Tc) ((Tr) tbl.getContent().get(0)).getContent().get(0)).getTcPr().getTcBorders();
		assertEquals(STBorder.OUTSET, borders2.getTop().getVal());
		assertEquals(STBorder.OUTSET, borders2.getBottom().getVal());
		assertEquals(STBorder.OUTSET, borders2.getLeft().getVal());
		assertEquals(STBorder.OUTSET, borders2.getRight().getVal());
		assertNull(borders2.getInsideH());
		assertNull(borders2.getInsideV());
		assertTrue(borders2.getTop().getSz().longValue() > 0);
		assertTrue(borders2.getBottom().getSz().longValue() > 0);
		assertTrue(borders2.getLeft().getSz().longValue() > 0);
		assertTrue(borders2.getRight().getSz().longValue() > 0);
	}

	@Test public void testTableBorderCollapse() throws Docx4JException {
		Tbl tbl = table("<table border='1' style='border-collapse: collapse;'><tr><td>1</td></tr></table>");

		TblWidth spacing = tbl.getTblPr().getTblCellSpacing();
		assertEquals("auto", spacing.getType());
		assertEquals(BigInteger.ZERO, spacing.getW());

		tbl = table("<table border='1'><tr><td>1</td></tr></table>");

		spacing = tbl.getTblPr().getTblCellSpacing();
		assertEquals("dxa", spacing.getType());
		assertTrue(spacing.getW().longValue() > 0);
	}
	
	@Test public void testTableBorderStyles() throws Docx4JException {
		HashMap<String, STBorder> styles = new HashMap<String, STBorder>();
		styles.put("none", STBorder.NONE);
		styles.put("hidden", STBorder.NONE);
		styles.put("solid", STBorder.SINGLE);
		styles.put("dotted", STBorder.DOTTED);
		styles.put("dashed", STBorder.DASHED);
		styles.put("double", STBorder.DOUBLE);
		styles.put("inset", STBorder.INSET);
		styles.put("outset", STBorder.OUTSET);
		styles.put("groove", STBorder.SINGLE);	// no direct substitution
		styles.put("ridge", STBorder.SINGLE);	// no direct substitution
		styles.put("anyOther", STBorder.NONE);

		for (String style : styles.keySet()) {
			Tbl tbl = table("<table style='border: 1px "+style+" black;'><tr><td>1</td></tr></table>");
			TblBorders borders = tbl.getTblPr().getTblBorders();
			assertEquals(styles.get(style), borders.getTop().getVal());
		}
	}

	@Test public void testTableBorderColors() throws Docx4JException {
		Tbl tbl = table("<table style='border-style: solid; border-color:#111 #222 #333 #444;'><tr><th>1</th></tr></table>");
		TblBorders borders = tbl.getTblPr().getTblBorders();

		assertEquals("#111111", borders.getTop().getColor());
		assertEquals("#222222", borders.getRight().getColor());
		assertEquals("#333333", borders.getBottom().getColor());
		assertEquals("#444444", borders.getLeft().getColor());
	}

	@Test public void testTableBorderWidths() throws Docx4JException {
		Tbl tbl = table("<table style='border-style: solid; border-width: 0px 1px 2px 3px;'><tr><th>1</th></tr></table>");
		TblBorders borders = tbl.getTblPr().getTblBorders();

		assertNull(borders.getTop().getSz());
		assertTrue(borders.getRight().getSz().longValue() > 0);
		assertTrue(borders.getBottom().getSz().longValue() > borders.getRight().getSz().longValue());
		assertTrue(borders.getLeft().getSz().longValue() > borders.getBottom().getSz().longValue());
	}
}

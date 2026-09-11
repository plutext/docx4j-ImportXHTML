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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.FormattingOption;
import org.docx4j.convert.in.xhtml.ImportXHTMLProperties;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * CSS font-family -> w:rFonts is direct formatting, so it must follow
 * the run-level FormattingOption like every other run property.
 * Before 17.1.1 it was written unconditionally.  See CR-001.
 */
public class RunFormattingFontTest {

	private static final String LEGACY_PROPERTY = "docx4j-ImportXHTML.Fonts.IgnoreRunFormattingOption";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();

		// A character style for @class to map to
		Style strong = new org.docx4j.wml.ObjectFactory().createStyle();
		strong.setType("character");
		strong.setStyleId("Strong");
		wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart()
				.getJaxbElement().getStyle().add(strong);
	}

	@After
	public void resetProperty() {
		ImportXHTMLProperties.setProperty(LEGACY_PROPERTY, Boolean.FALSE);
	}

	private RPr convertFirstRun(String xhtml, FormattingOption runFormatting) throws Docx4JException {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		importer.setRunFormatting(runFormatting);
		List<Object> results = importer.convert(xhtml, "");
		P p = (P) results.get(0);
		for (Object o : p.getContent()) {
			o = XmlUtils.unwrap(o);
			if (o instanceof R) {
				return ((R) o).getRPr();
			}
		}
		throw new AssertionError("no run in " + XmlUtils.marshaltoString(p));
	}

	private static final String CLASS_AND_FONT = "<div><p><span class=\"Strong\" style=\"font-family:Arial\">text</span></p></div>";
	private static final String NO_CSS = "<div><p>text</p></div>";
	private static final String SANS_SERIF = "<div><p><span style=\"font-family:sans-serif\">text</span></p></div>";

	@Test public void classToStyleOnlyGivesStyleButNoFont() throws Docx4JException {
		RPr rPr = convertFirstRun(CLASS_AND_FONT, FormattingOption.CLASS_TO_STYLE_ONLY);
		assertNotNull(rPr);
		assertEquals("Strong", rPr.getRStyle().getVal());
		assertNull(rPr.getRFonts());
	}

	@Test public void classToStyleOnlyDoesNotLeakDefaultFont() throws Docx4JException {
		// Unstyled XHTML computes to font-family:serif, which used to become
		// Times New Roman on every run.
		RPr rPr = convertFirstRun(NO_CSS, FormattingOption.CLASS_TO_STYLE_ONLY);
		assertNotNull(rPr);
		assertNull(rPr.getRFonts());
	}

	@Test public void classPlusOtherStillMapsFont() throws Docx4JException {
		RPr rPr = convertFirstRun(SANS_SERIF, FormattingOption.CLASS_PLUS_OTHER);
		assertNotNull(rPr.getRFonts());
		assertEquals(ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.fonts.default.sans-serif", "Arial"),
				rPr.getRFonts().getAscii());
	}

	@Test public void ignoreClassStillMapsFont() throws Docx4JException {
		RPr rPr = convertFirstRun(SANS_SERIF, FormattingOption.IGNORE_CLASS);
		assertNotNull(rPr.getRFonts());
		assertEquals(ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.fonts.default.sans-serif", "Arial"),
				rPr.getRFonts().getAscii());
	}

	@Test public void legacyPropertyRestoresUnconditionalFont() throws Docx4JException {
		ImportXHTMLProperties.setProperty(LEGACY_PROPERTY, Boolean.TRUE);
		RPr rPr = convertFirstRun(CLASS_AND_FONT, FormattingOption.CLASS_TO_STYLE_ONLY);
		assertEquals("Strong", rPr.getRStyle().getVal());
		assertNotNull(rPr.getRFonts());
		assertEquals("Arial", rPr.getRFonts().getAscii());
	}

}

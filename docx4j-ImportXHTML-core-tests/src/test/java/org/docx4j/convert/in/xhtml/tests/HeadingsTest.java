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

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.FormattingOption;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.Style;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * h1 to h6 and Word's heading styles: off by default; when on, the style named
 * "heading N", else w:outlineLvl (docs/change-requests/CR-002 section 6.2).
 */
public class HeadingsTest {

	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>"
			+ "<h1>One</h1><h2>Two</h2><h5>Five</h5><h6>Six</h6><p>para</p>"
			+ "</body></html>";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception {
		wordMLPackage = WordprocessingMLPackage.createPackage(); // defines Heading1 to Heading4
	}

	@Test
	public void offByDefault() throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		Assert.assertFalse(importer.isMapHeadingsToStyles());

		for (P p : convert(importer)) {
			Assert.assertNull(XmlUtils.marshaltoString(p), p.getPPr().getPStyle());
			Assert.assertNull(XmlUtils.marshaltoString(p), p.getPPr().getOutlineLvl());
		}
	}

	@Test
	public void headingStyleElseOutlineLevel() throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		importer.setMapHeadingsToStyles(true);
		List<P> ps = convert(importer);

		Assert.assertEquals("Heading1", ps.get(0).getPPr().getPStyle().getVal());
		Assert.assertEquals("Heading2", ps.get(1).getPPr().getPStyle().getVal());

		// no Heading5 or Heading6 in the package: an outline level, and no style added
		Assert.assertNull(ps.get(2).getPPr().getPStyle());
		Assert.assertEquals(BigInteger.valueOf(4), ps.get(2).getPPr().getOutlineLvl().getVal());
		Assert.assertEquals(BigInteger.valueOf(5), ps.get(3).getPPr().getOutlineLvl().getVal());
		Assert.assertFalse(wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement()
				.getStyle().stream().anyMatch(s -> "Heading5".equals(s.getStyleId())));

		Assert.assertNull(ps.get(4).getPPr().getPStyle());
		Assert.assertNull(ps.get(4).getPPr().getOutlineLvl());
	}

	@Test
	public void styleFoundByNameNotId() throws Exception {
		// as in a localised template, whose heading styles keep their English names
		Style s = Context.getWmlObjectFactory().createStyle();
		s.setType("paragraph");
		s.setStyleId("berschrift6");
		Style.Name name = Context.getWmlObjectFactory().createStyleName();
		name.setVal("heading 6");
		s.setName(name);
		wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement().getStyle().add(s);

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		importer.setMapHeadingsToStyles(true);
		List<P> ps = convert(importer);

		Assert.assertEquals("berschrift6", ps.get(3).getPPr().getPStyle().getVal());
		Assert.assertNull(ps.get(3).getPPr().getOutlineLvl());
	}

	@Test
	public void styleWithNoName() throws Exception {
		// w:name is optional; such a style used to give a NullPointerException
		Style s = Context.getWmlObjectFactory().createStyle();
		s.setType("paragraph");
		s.setStyleId("Nameless");
		wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement().getStyle().add(s);

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		importer.setMapHeadingsToStyles(true);
		List<P> ps = convert(importer);

		Assert.assertEquals("Heading1", ps.get(0).getPPr().getPStyle().getVal());
	}

	@Test
	public void classToStyleOnly() throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		importer.setMapHeadingsToStyles(true);
		importer.setParagraphFormatting(FormattingOption.CLASS_TO_STYLE_ONLY);
		List<P> ps = convert(importer);

		Assert.assertEquals("Heading2", ps.get(1).getPPr().getPStyle().getVal());
		Assert.assertNull(ps.get(1).getPPr().getSpacing()); // no direct formatting
	}

	private static List<P> convert(XHTMLImporterImpl importer) throws Exception {
		List<P> ps = new ArrayList<>();
		for (Object o : importer.convert(XHTML, null)) {
			if (XmlUtils.unwrap(o) instanceof P) ps.add((P)XmlUtils.unwrap(o));
		}
		return ps;
	}
}

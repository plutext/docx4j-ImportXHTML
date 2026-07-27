/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2026, Plutext Pty Ltd, and contributors.
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

import org.docx4j.TextUtils;
import org.docx4j.convert.in.xhtml.ImportXHTMLProperties;
import org.docx4j.convert.in.xhtml.MissingImageException;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * An image which can't be added is replaced with placeholder text by default,
 * or, if docx4j-ImportXHTML.Images.ThrowOnMissing is set, causes
 * MissingImageException to be thrown.
 *
 * @since 17.0.1
 */
public class MissingImageTest {

	private static final String THROW_ON_MISSING = "docx4j-ImportXHTML.Images.ThrowOnMissing";

	// 2x2 pixels
	private static final String PNG_IMAGE_DATA = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";

	// no comma, so no data follows the media type
	private static final String MALFORMED_DATA_URI = "data:image/png;base64";

	private static final String MISSING_SRC = "no-such-image-does-not-exist.png";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception {
		wordMLPackage = WordprocessingMLPackage.createPackage();
		ImportXHTMLProperties.setProperty(THROW_ON_MISSING, false);
	}

	@After
	public void tearDown() throws Exception {
		// the property is global, so don't leak it into other tests
		ImportXHTMLProperties.setProperty(THROW_ON_MISSING, false);
	}

	@Test
	public void testMissingImageIsReplacedWithPlaceholderByDefault() throws Exception {

		String text = convertToText("<div><img src='" + MISSING_SRC + "' alt='my alt'/></div>");

		Assert.assertTrue("expected placeholder text, got: " + text,
				text.contains("MISSING IMAGE") && text.contains("my alt"));
	}

	@Test
	public void testMissingImageThrowsWhenConfigured() throws Exception {

		ImportXHTMLProperties.setProperty(THROW_ON_MISSING, true);

		try {
			convertToText("<div><img src='" + MISSING_SRC + "' alt='my alt'/></div>");
			Assert.fail("expected MissingImageException");
		} catch (MissingImageException mie) {
			Assert.assertEquals(MISSING_SRC, mie.getSrc());
		}
	}

	@Test
	public void testMalformedDataUriIsReplacedWithPlaceholderByDefault() throws Exception {

		String text = convertToText("<div><img src='" + MALFORMED_DATA_URI + "' alt='my alt'/></div>");

		Assert.assertTrue("expected placeholder text, got: " + text,
				text.contains("INVALID DATA URI"));
	}

	@Test
	public void testMalformedDataUriThrowsWhenConfigured() throws Exception {

		ImportXHTMLProperties.setProperty(THROW_ON_MISSING, true);

		try {
			convertToText("<div><img src='" + MALFORMED_DATA_URI + "' alt='my alt'/></div>");
			Assert.fail("expected MissingImageException");
		} catch (MissingImageException mie) {
			Assert.assertEquals(MALFORMED_DATA_URI, mie.getSrc());
		}
	}

	/**
	 * An image which is present must still be added when the property is set.
	 */
	@Test
	public void testValidImageUnaffectedWhenConfiguredToThrow() throws Exception {

		ImportXHTMLProperties.setProperty(THROW_ON_MISSING, true);

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		List<Object> convert = importer.convert(
				"<div><img src='" + PNG_IMAGE_DATA + "'/></div>", null);

		Inline inline = ((Inline)((Drawing)((R)((P)convert.get(0)).getContent().get(0))
				.getContent().get(0)).getAnchorOrInline().get(0));
		Assert.assertTrue(inline.getExtent().getCx() > 0);
	}

	private String convertToText(String html) throws Exception {

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		List<Object> convert = importer.convert(html, null);

		StringBuilder sb = new StringBuilder();
		for (Object o : convert) {
			sb.append(TextUtils.getText(o));
		}
		return sb.toString();
	}
}

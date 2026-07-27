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

import java.util.ArrayList;
import java.util.List;

import org.docx4j.TextUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * An li which isn't in an ol|ul is invalid XHTML, but well formed.  We accept
 * it, as a browser would, treating it as an ordinary block; it just can't be
 * numbered, since the Word numbering comes from the ol|ul.
 *
 * Before 17.0.2, such an li caused the conversion to fail.
 *
 * @since 17.0.2
 */
public class OrphanListItemTest {

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	@Test
	public void testOrphanListItemInDiv() throws Exception {

		List<P> paragraphs = convertToParagraphs("<div><li>orphan item</li></div>");

		Assert.assertEquals(1, paragraphs.size());
		Assert.assertEquals("orphan item", getText(paragraphs.get(0)));
		Assert.assertFalse("an li outside ol|ul can't be numbered", isNumbered(paragraphs.get(0)));
	}

	@Test
	public void testOrphanListItemAsSoleContent() throws Exception {

		List<P> paragraphs = convertToParagraphs("<li>bare orphan</li>");

		Assert.assertEquals(1, paragraphs.size());
		Assert.assertEquals("bare orphan", getText(paragraphs.get(0)));
		Assert.assertFalse(isNumbered(paragraphs.get(0)));
	}

	@Test
	public void testSuccessiveOrphanListItems() throws Exception {

		List<P> paragraphs = convertToParagraphs("<div><li>one</li><li>two</li></div>");

		Assert.assertEquals(2, paragraphs.size());
		Assert.assertEquals("one", getText(paragraphs.get(0)));
		Assert.assertEquals("two", getText(paragraphs.get(1)));
		Assert.assertFalse(isNumbered(paragraphs.get(0)));
		Assert.assertFalse(isNumbered(paragraphs.get(1)));
	}

	/**
	 * The list stack is popped when the ul ends, so a following li is an orphan,
	 * even though a list appears earlier in the document.
	 */
	@Test
	public void testListItemFollowingClosedList() throws Exception {

		List<P> paragraphs = convertToParagraphs(
				"<div><ul><li>in list</li></ul><li>orphan after</li></div>");

		Assert.assertEquals(2, paragraphs.size());
		Assert.assertEquals("in list", getText(paragraphs.get(0)));
		Assert.assertTrue("li in the ul should still be numbered", isNumbered(paragraphs.get(0)));

		Assert.assertEquals("orphan after", getText(paragraphs.get(1)));
		Assert.assertFalse("li after the ul closed is an orphan", isNumbered(paragraphs.get(1)));
	}

	/**
	 * Valid lists must be unaffected.
	 */
	@Test
	public void testValidListIsStillNumbered() throws Exception {

		List<P> paragraphs = convertToParagraphs("<ul><li>alpha</li><li>beta</li></ul>");

		Assert.assertEquals(2, paragraphs.size());
		for (P p : paragraphs) {
			Assert.assertTrue("li in a ul should be numbered", isNumbered(p));
		}
	}

	/**
	 * Valid nested lists must be unaffected.
	 */
	@Test
	public void testValidNestedListIsStillNumbered() throws Exception {

		List<P> paragraphs = convertToParagraphs(
				"<ol><li>a<ol><li>a.1</li></ol></li><li>b</li></ol>");

		Assert.assertEquals(3, paragraphs.size());
		for (P p : paragraphs) {
			Assert.assertTrue("li in an ol should be numbered", isNumbered(p));
		}
		// the inner item is at the second level
		Assert.assertEquals(0, paragraphs.get(0).getPPr().getNumPr().getIlvl().getVal().intValue());
		Assert.assertEquals(1, paragraphs.get(1).getPPr().getNumPr().getIlvl().getVal().intValue());
		Assert.assertEquals(0, paragraphs.get(2).getPPr().getNumPr().getIlvl().getVal().intValue());
	}

	private boolean isNumbered(P p) {
		return p.getPPr() != null
				&& p.getPPr().getNumPr() != null
				&& p.getPPr().getNumPr().getNumId() != null;
	}

	private String getText(P p) {
		return TextUtils.getText(p);
	}

	private List<P> convertToParagraphs(String html) throws Exception {

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);

		List<P> paragraphs = new ArrayList<P>();
		for (Object o : importer.convert(html, null)) {
			if (o instanceof P) {
				paragraphs.add((P)o);
			}
		}
		return paragraphs;
	}
}

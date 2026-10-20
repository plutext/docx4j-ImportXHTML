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
import org.junit.Assert;
import org.junit.Test;

/**
 * An hr is Word's VML horizontal rule, in a paragraph of its own: a fresh one each
 * time (one P object used to go into every document).
 */
public class HorizontalRuleTest {

	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>"
			+ "<p>a</p><hr/><p>b</p><hr/></body></html>";

	@Test
	public void freshParagraphEachTime() throws Exception {
		List<P> first = rules(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(XHTML, null));
		List<P> second = rules(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(XHTML, null));

		Assert.assertEquals(2, first.size());
		Assert.assertEquals(2, second.size());
		Assert.assertNotSame(first.get(0), first.get(1));
		Assert.assertNotSame(first.get(0), second.get(0));

		// so a change to one, here, isn't a change to another document's
		first.get(0).setPPr(new org.docx4j.wml.PPr());
		Assert.assertNull(second.get(0).getPPr());
	}

	private static List<P> rules(List<Object> content) {
		List<P> rules = new ArrayList<>();
		for (Object o : content) {
			if (XmlUtils.unwrap(o) instanceof P
					&& XmlUtils.marshaltoString(o).contains("o:hr=\"t\"")) {
				rules.add((P)XmlUtils.unwrap(o));
			}
		}
		return rules;
	}
}

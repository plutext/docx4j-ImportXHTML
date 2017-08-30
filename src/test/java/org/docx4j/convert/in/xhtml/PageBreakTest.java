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

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.docx4j.wml.STBrType.PAGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PageBreakTest {

	private WordprocessingMLPackage wordMLPackage;
	
	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	private List<Object> convert(String xhtml) throws Docx4JException {
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		return XHTMLImporter.convert(xhtml, "");
	}

	private List<Object> fromXHTML(String content) throws Docx4JException {
		List<Object> converted = convert(content);
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));
		return converted;
	}

	@Test public void testPageBreakAfter() throws Docx4JException {
		String content = "content";
		List<Object> objects = fromXHTML(pageBreak("after", content));

		//test content
		P p1 = (P) objects.get(0);
		R r = (R) p1.getContent().get(0);
		Object unwrap = XmlUtils.unwrap( r.getContent().get(0));
		assertThat(((Text) unwrap).getValue(), is(content));

		//test page break
		P p2 = (P) objects.get(1);
		Br pageBreak = (Br) p2.getContent().get(0);
		assertThat(pageBreak.getType(), is(PAGE));
	}

	@Test public void testPageBreakBefore() throws Docx4JException {
		String content = "content";
		List<Object> objects = fromXHTML(pageBreak("before", content));
		P p = (P) objects.get(0);

		//test content
		Br pageBreak = (Br) p.getContent().get(0);
		assertThat(pageBreak.getType(), is(PAGE));

		//test page break
		R r = (R) p.getContent().get(1);
		Object unwrap = XmlUtils.unwrap( r.getContent().get(0));
		assertThat(((Text) unwrap).getValue(), is(content));
	}

	private String pageBreak(final String position, final String content) {
		return "<div style=\"page-break-" + position + ": always\"><p>" + content + "</p></div>";
	}

}

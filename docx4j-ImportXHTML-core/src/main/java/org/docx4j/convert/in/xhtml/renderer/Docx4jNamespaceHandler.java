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
package org.docx4j.convert.in.xhtml.renderer;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.extend.StylesheetFactory;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;

/**
 * openhtmltopdf's XhtmlNamespaceHandler, plus the presentational hint HTML gives an
 * ol's type attribute: a list-style-type.  Like the handler's other hints, it enters
 * the cascade ahead of the author's rules, so it beats the default stylesheet's decimal
 * and loses to the author's CSS, as in a browser.
 *
 * @since 17.3.4
 */
public class Docx4jNamespaceHandler extends XhtmlNamespaceHandler {

	/**
	 * openhtmltopdf reads its default stylesheet with getClass().getResourceAsStream,
	 * which, for a subclass in another JPMS module, looks in that module and fails; and
	 * the failure is remembered for the JVM.  So let a stock handler read it, into the
	 * cache they share.
	 */
	@Override
	public StylesheetInfo getDefaultStylesheet(StylesheetFactory factory) {
		return new XhtmlNamespaceHandler().getDefaultStylesheet(factory);
	}

	@Override
	public String getNonCssStyling(Element e) {

		String styling = super.getNonCssStyling(e);
		if (!e.getNodeName().equals("ol")) return styling;

		String listStyleType = listStyleType(e.getAttribute("type"));
		if (listStyleType==null) return styling;

		return (styling==null || styling.trim().isEmpty() ? "" : styling + ";")
				+ "list-style-type: " + listStyleType;
	}

	/**
	 * @param type an ol's type attribute, which is case-sensitive
	 */
	private static String listStyleType(String type) {
		switch (type.trim()) {
		case "1": return "decimal";
		case "a": return "lower-alpha";
		case "A": return "upper-alpha";
		case "i": return "lower-roman";
		case "I": return "upper-roman";
		default: return null;
		}
	}
}

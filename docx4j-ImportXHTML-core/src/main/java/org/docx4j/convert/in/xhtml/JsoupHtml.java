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
package org.docx4j.convert.in.xhtml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;

/**
 * HTML which isn't well-formed XML, read with jsoup's HTML parser into a W3C DOM.
 * jsoup is an optional dependency, and this is the only class which names it, so
 * that nothing loads it unless jsoup is there (see XHTMLImporterImpl.isJsoupAvailable).
 */
final class JsoupHtml {

	private JsoupHtml() {}
	
	/**
	 * @param charset the declared encoding, or null to detect it (from a byte order
	 * mark, which wins anyway, or a meta element)
	 */
	static Document parse(byte[] html, Charset charset, String baseUrl) throws IOException {
		org.jsoup.nodes.Document doc = Jsoup.parse(new ByteArrayInputStream(html),
				charset==null ? null : charset.name(),
				baseUrl==null ? "" : baseUrl);
		return W3CDom.convert(doc);
	}
}

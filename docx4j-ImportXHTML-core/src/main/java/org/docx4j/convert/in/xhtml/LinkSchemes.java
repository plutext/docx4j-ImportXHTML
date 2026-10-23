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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which link targets the importers write as links (CR-005).  An a/@href becomes an External
 * relationship, and the document is then opened by more than docx4j: Word, LibreOffice, other
 * converters, previewers.  So, as POI's WordToHtmlConverter (apache/poi#1455) and docx4j's
 * HTML output (docx4j CR-056) do, a target is allowed if it has no scheme (a relative path, a
 * fragment, a UNC path), is a Windows drive path (C:\), or its scheme is http, https, ftp,
 * mailto, file, news or tel.  The property docx4j-ImportXHTML.Hyperlink.Schemes, a
 * comma-separated list, adds schemes to those; nothing removes one.
 *
 * <p>The scheme is read as a browser reads it: tabs and line breaks anywhere are dropped, and
 * control characters and spaces at the start ignored, so "java&#9;script:" is javascript:.
 *
 * @since 17.3.4
 */
public final class LinkSchemes {

	/** The property whose comma-separated schemes are allowed as well as the defaults. */
	public static final String PROPERTY = "docx4j-ImportXHTML.Hyperlink.Schemes";

	private static final Set<String> DEFAULTS = Collections.unmodifiableSet(new HashSet<String>(
			Arrays.asList("http", "https", "ftp", "mailto", "file", "news", "tel")));

	private LinkSchemes() {
	}

	/**
	 * @return the schemes allowed whatever the property says
	 */
	public static Set<String> defaults() {
		return DEFAULTS;
	}

	/**
	 * @return whether target may be written as a link: it has no scheme, is a drive path, or
	 * its scheme is one of the defaults or in the property
	 */
	public static boolean isAllowed(String target) {
		String scheme = scheme(target);
		return scheme == null || DEFAULTS.contains(scheme) || extra().contains(scheme);
	}

	/**
	 * @return the target's scheme in lower case, read as a browser reads it; null where it has
	 * none, or a single letter, which is a drive
	 */
	static String scheme(String target) {
		if (target == null) return null;
		StringBuilder sb = new StringBuilder(target.length());
		for (int i = 0; i < target.length(); i++) {
			char c = target.charAt(i);
			if (c == '\t' || c == '\n' || c == '\r') continue;
			if (c <= ' ' && sb.length() == 0) continue;
			sb.append(c);
		}
		// scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." ) ":"
		int end = 0;
		while (end < sb.length() && isSchemeChar(sb.charAt(end), end == 0)) end++;
		if (end < 2 || end == sb.length() || sb.charAt(end) != ':') return null;
		return sb.substring(0, end).toLowerCase(Locale.ROOT);
	}

	private static boolean isSchemeChar(char c, boolean first) {
		if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return true;
		return !first && ((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.');
	}

	/** The property's value as last parsed, and the schemes in it */
	private static final class Extra {
		final String list;
		final Set<String> schemes;

		Extra(String list) {
			this.list = list;
			Set<String> parsed = new HashSet<String>();
			for (String s : list.split(",")) {
				s = s.trim().toLowerCase(Locale.ROOT);
				if (s.endsWith(":")) s = s.substring(0, s.length() - 1);
				if (!s.isEmpty()) parsed.add(s);
			}
			this.schemes = Collections.unmodifiableSet(parsed);
		}
	}

	private static volatile Extra extra;

	/**
	 * @return the schemes the property adds, parsed once per value of the property (it can
	 * be set at run time), not once per link
	 */
	private static Set<String> extra() {
		String list = ImportXHTMLProperties.getProperty(PROPERTY);
		if (list == null) return Collections.emptySet();
		Extra e = extra;
		if (e == null || !e.list.equals(list)) {
			e = new Extra(list);
			extra = e;
		}
		return e.schemes;
	}
}

/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2014-2022, Plutext Pty Ltd, and contributors.
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

import java.util.HashMap;
import java.util.Map;

import org.docx4j.wml.Style;
import org.docx4j.wml.Styles;

public class HeadingHandler {
	
	
	/**
	 * HTML element (eg h1) to style ID (eg berschrift1) 
	 */
	private Map<String, String> elementToStyleId = new HashMap<String, String>(); 
	
	private static final String HEADING_NAME_PREFIX = "heading ";
	
	protected HeadingHandler(Styles styles) {
		
		/*  A style looks like:
		 * 
		 *   <w:style w:type="paragraph" w:styleId="berschrift1">
		 *       <w:name w:val="heading 1"/>
		 *       
		 *  In other words, irrespective of the language, the name
		 *  remains in English.
		 */
		
		for (Style s : styles.getStyle()) {
			
			if (s.getName().getVal().startsWith(HEADING_NAME_PREFIX)) {
				
				// We may wish to map this
				String styleName= s.getName().getVal();
				String suffix =  styleName.substring(HEADING_NAME_PREFIX.length());
				
				try {
					int lvl = Integer.parseInt(suffix);
					
					if (lvl>=1 && lvl <=9) {
						elementToStyleId.put("h"+lvl, s.getStyleId());
					}
					
				} catch (NumberFormatException nfe) {
					
				}
				
			}
		}
		
	}

	protected String getStyle(String localname) {
		return elementToStyleId.get(localname);
	}
	
}

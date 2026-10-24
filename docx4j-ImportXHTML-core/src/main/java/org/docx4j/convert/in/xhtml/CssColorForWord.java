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

import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.ColorValue;

/**
 * A CSS colour as WordprocessingML can have it.  Word's colours have no alpha, so a
 * fully transparent colour is none, and partial alpha is blended over white, as it
 * looks on a white page (CR-006 D2).  openhtmltopdf parses rgba(), hsl() and hsla()
 * from 1.1.100.
 * 
 * @since 17.3.5
 */
public final class CssColorForWord {

	private CssColorForWord() {}

	/**
	 * @return null where the colour is fully transparent; otherwise the colour, opaque,
	 * with partial alpha blended over white.  A colour other than RGB (cmyk()) is
	 * returned as it is.
	 */
	public static FSColor opaque(FSColor color) {
		if (!(color instanceof FSRGBColor)) return color;
		FSRGBColor rgb = (FSRGBColor)color;
		float alpha = rgb.getAlpha();
		if (alpha <= 0f) return null;
		if (alpha >= 1f) return rgb;
		return new FSRGBColor(overWhite(rgb.getRed(), alpha), overWhite(rgb.getGreen(), alpha),
				overWhite(rgb.getBlue(), alpha));
	}

	private static int overWhite(int c, float alpha) {
		return Math.round(c * alpha + 255 * (1 - alpha));
	}

	/**
	 * @return a border colour for w:color, without a leading '#', or null where the colour
	 * is transparent, for the caller to give no border (CR-006 D3).  A value that isn't a
	 * colour is returned as its CSS text, as before.
	 */
	static String borderColor(FSDerivedValue value) {
		String color;
		if (value instanceof ColorValue) {
			FSColor opaque = opaque(((ColorValue)value).asColor());
			if (opaque==null) return null;
			color = opaque.toString(); // #rrggbb, for an opaque FSRGBColor
		} else {
			color = value.asString();
			if ("transparent".equalsIgnoreCase(color)) return null;
		}
		if (color.startsWith("#")) color=color.substring(1); // Fix for https://github.com/plutext/docx4j/issues/101
		return color;
	}
}

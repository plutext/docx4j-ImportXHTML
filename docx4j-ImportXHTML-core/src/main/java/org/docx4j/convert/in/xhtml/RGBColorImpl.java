/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2021-2022, Plutext Pty Ltd, and contributors.
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

import com.openhtmltopdf.css.parser.FSRGBColor;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.RGBColor;

public class RGBColorImpl implements RGBColor {

	
	RGBColorImpl(FSRGBColor fsColor) {
		this._red = fsColor.getRed();
		this._green = fsColor.getGreen();
		this._blue = fsColor.getBlue();
	}
	
    private final int _red;
    private final int _green;
    private final int _blue;
	
	
	@Override
	public CSSPrimitiveValue getRed() {
		return toCSSPrimitiveValue(_red);
	}

	@Override
	public CSSPrimitiveValue getGreen() {
		return toCSSPrimitiveValue(_green);
	}

	@Override
	public CSSPrimitiveValue getBlue() {
		return toCSSPrimitiveValue(_blue);
	}

	
	private CSSPrimitiveValue toCSSPrimitiveValue(int n) {
		DomCssValueAdaptor primitive = new DomCssValueAdaptor();
		primitive.setFloatValue((short) 0, n);
		return primitive;
		
	}
}

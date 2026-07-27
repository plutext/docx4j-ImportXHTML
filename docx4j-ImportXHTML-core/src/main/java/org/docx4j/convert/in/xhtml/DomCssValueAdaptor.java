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
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.derived.ColorValue;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.Counter;
import org.w3c.dom.css.RGBColor;
import org.w3c.dom.css.Rect;

public class DomCssValueAdaptor implements org.w3c.dom.css.CSSPrimitiveValue {

	
	public DomCssValueAdaptor(PropertyValue val) {
		
		this.val = val;
		
		this.cssText = val.getCssText();
		this.cssValueType = val.getCssValueType();

		this.floatValue = val.getFloatValue();
		
	}

	DomCssValueAdaptor() {
				
	}
	
	private PropertyValue val;
	
	private String cssText;
	private short cssValueType;
    
	private float floatValue;
	
	@Override
	public String getCssText() {
		return cssText;
	}

	@Override
	public void setCssText(String cssText) throws DOMException {
		this.cssText = cssText;
	}

	@Override
	public short getCssValueType() {
		return cssValueType;
	}

	@Override
	public short getPrimitiveType() {
		return val.getPrimitiveType();
	}

	@Override
	public void setFloatValue(short unitType, float floatValue) throws DOMException {
		// Used in RGB color stuff
		this.floatValue = floatValue;
		
	}

	
	@Override
	public float getFloatValue(short unitType) throws DOMException {
		
		return floatValue;
	}

	@Override
	public void setStringValue(short stringType, String stringValue) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getStringValue() throws DOMException {
		return val.getStringValue();
	}

	@Override
	public Counter getCounterValue() throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Rect getRectValue() throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RGBColor getRGBColorValue() throws DOMException {
		
		if (val.getFSColor()!=null ) {
			return new RGBColorImpl((FSRGBColor)val.getFSColor() );
		}
		
		throw new UnsupportedOperationException();
	}

}

/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2014-2026, Plutext Pty Ltd, and contributors.
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

import java.math.BigInteger;
import java.util.Map;

import org.docx4j.UnitsOfMeasurement;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.CyclicStylesException;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.layout.Styleable;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.PBdr;
import org.docx4j.wml.STBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.css.CSSValue;

/**
 * Consider p/@style="border: 1px solid black;padding-left:20px;"
 * 
 * That should generates something like:
 * 
 * &lt;w:pBdr&gt;
 *    &lt;w:left w:val="single" w:sz="6" w:space="15" w:color="000000"/&gt;
 *    
 * Because this is multiple CSS properties to one WordML element, it
 * is best to handle it in this dedicated helper class.  
 * 
 * No attempt is made to reconcile borders eg between a p and its enclosing
 * div or tc (if any), or preceding or following p. 
 * 
 * @author jharrop
 * @since 3.2.2
 */
public class ParagraphBorderHelper {
	
	public static Logger log = LoggerFactory.getLogger(ParagraphBorderHelper.class);
    
	private XHTMLImporterImpl importer;
    protected ParagraphBorderHelper(XHTMLImporterImpl importer) {
    	this.importer=importer;
    }
    
    PBdr pBdr = null;
	
	private PBdr createPBdr(PPr pPr) {

		pBdr = pPr.getPBdr();
		if (pBdr == null) {
			pBdr = Context.getWmlObjectFactory().createPPrBasePBdr();
			pPr.setPBdr(pBdr);
		}

		return pBdr;
	}
	
	private CTBorder getBorder(PPr pPr, String side) {
		
		pBdr = createPBdr(pPr);
		CTBorder border = null;
		
		if (side.equals("left")) {
			border = pBdr.getLeft(); 
		} else if (side.equals("right")) {
			border = pBdr.getRight(); 
		} else if (side.equals("top")) {
			border = pBdr.getTop(); 
		} else if (side.equals("bottom")) {
			border = pBdr.getBottom(); 
		}
		
		if (border==null) {
			border = Context.getWmlObjectFactory().createCTBorder();
			if (side.equals("left")) {
				pBdr.setLeft(border); 
			} else if (side.equals("right")) {
				pBdr.setRight(border); 
			} else if (side.equals("top")) {
				pBdr.setTop(border); 
			} else if (side.equals("bottom")) {
				pBdr.setBottom(border); 
			}
		}
		
		return border;
	}

	private void setBorder(PPr pPr, String side, CTBorder border ) {
		
		pBdr = createPBdr(pPr);
		
		if (side.equals("left")) {
			pBdr.setLeft(border); 
		} else if (side.equals("right")) {
			pBdr.setRight(border); 
		} else if (side.equals("top")) {
			pBdr.setTop(border); 
		} else if (side.equals("bottom")) {
			pBdr.setBottom(border); 
		}
	}
	
    protected void addBorderProperties(PPr pPr, Styleable styleable, Map<String, PropertyValue> cssMap) throws CyclicStylesException {
    	
    	doSide( pPr,  styleable,cssMap, "left");
    	doSide( pPr,  styleable,cssMap, "right");
    	doSide( pPr,  styleable,cssMap, "top");
    	doSide( pPr,  styleable,cssMap, "bottom");

    }
    
    protected void doSide(PPr pPr, Styleable styleable, Map<String, PropertyValue> cssMap, String side) throws CyclicStylesException {
    	
    	CTBorder border = null;
    	
    	PropertyValue borderStyle = cssMap.get("border-"+side+"-style");
    	if (borderStyle!=null && !"none".equals(borderStyle.getCssText())) {
    		
    		// paragraph has a border
    		border = createBorderStyle( styleable, side);

    		setBorder( pPr,  side,  border );
    	}
    	
		// padding to space
    	BigInteger spaceAttrVal = paddingToSpace(styleable, side);
    	
    	if (spaceAttrVal!=null) {
    		
    		border = getBorder( pPr,  side);
    		border.setSpace(spaceAttrVal);
    	}
    	
    	// NB:  Word 2010 doesn't seem to honour this when its in a table cell!
    	// and there doesn't seem to be a compat setting which should affect this.
    	
    }
		
	private BigInteger paddingToSpace(Styleable styleable,  String side)  {

		FSDerivedValue padding = styleable.getStyle().valueByName( CSSName.getByPropertyName("padding-" + side));
		if (padding != null && padding instanceof LengthValue) {
						
			int twip = UnitsOfMeasurement.pxToTwip(((LengthValue)padding).asFloat()/20);
			
			if (twip>0) {
				return BigInteger.valueOf(twip);
			} else {
				// Don't create a @space setting
			}
		}
		return null;
	}
	
	/**
	 *  borders support
	 * @param box table or cell to copy css border properties from
	 * @param side "top"/"bottom"/"left"/"right"
	 * @return  border style
	 * @throws CyclicStylesException 
	 */
	private CTBorder createBorderStyle(Styleable styleable, String side) throws CyclicStylesException {
		
		FSDerivedValue borderStyle = styleable.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-style") );
		FSDerivedValue borderColor = styleable.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-color") );
		float width = styleable.getStyle().getFloatPropertyProportionalHeight(
				CSSName.getByPropertyName("border-"+side+"-width"), 0, importer.getRenderer().getLayoutContext() );

		STBorder stBorder;
		try {
			stBorder = STBorder.fromValue( borderStyle.asString() );
		} catch (IllegalArgumentException e) {
			stBorder = STBorder.SINGLE; 
		}

		// w:ST_EighthPointMeasure - Measurement in Eighths of a Point
		width = UnitsOfMeasurement.twipToPoint( Math.round(width) ) * 8.0f;
		
		String color = borderColor.asString();
		if (color.startsWith("#")) color=color.substring(1);
		
		return createBorderStyle( stBorder, color, BigInteger.valueOf( Math.round(width) ) );
	}

	private CTBorder createBorderStyle(STBorder val, String color, BigInteger sz) {
		CTBorder border = Context.getWmlObjectFactory().createCTBorder();
		border.setVal(val);
		border.setColor(color);
		border.setSz(sz);
		return border;
	}



}

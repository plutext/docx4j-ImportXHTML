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
package org.pptx4j.convert.in.xhtml;

import java.util.LinkedList;
import java.util.Map;

import org.docx4j.convert.in.xhtml.DomCssValueAdaptor;
import org.docx4j.dml.CTRegularTextRun;
import org.docx4j.dml.CTTextAutonumberBullet;
import org.docx4j.dml.CTTextCharBullet;
import org.docx4j.dml.CTTextCharacterProperties;
import org.docx4j.dml.CTTextParagraph;
import org.docx4j.dml.CTTextParagraphProperties;
import org.docx4j.dml.CTTextSpacing;
import org.docx4j.dml.CTTextSpacingPercent;
import org.docx4j.dml.STTextAutonumberScheme;
import org.docx4j.dml.TextFont;

import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.render.BlockBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSValue;

public class ListHelper {
	
	/* Note: currently we don't populate
	 * 
		<p:txBody>
        	<a:lstStyle>
                    	 */

	public static Logger log = LoggerFactory.getLogger(ListHelper.class);

	public ListHelper(/* XHTMLtoPPTX importer*/) {
//		this.importer=importer;
	}

	private static org.docx4j.dml.ObjectFactory dmlObjectFactory = new org.docx4j.dml.ObjectFactory();
	
	/**
	 * The indentation after the number, typically the same
	 * as the hanging indent, which lines up subsequent paragraphs.
	 */
	protected static final int INDENT_AFTER = 360;
	
//	private XHTMLtoPPTX importer;

	// Commented out for now; See list.txt
//	public static final String XHTML_AbstractNum_For_OL = "XHTML_AbstractNum_For_OL";
//	public static final String XHTML_AbstractNum_For_UL = "XHTML_AbstractNum_For_UL";


	private LinkedList<BlockBox> listStack = new LinkedList<BlockBox>();
	// These are the incoming ul and ol.
	// Generally, these will be BlockBox (display:inline or display:inline-block).
	// <ul style="display:inline"> hides them entirely..


	protected void pushListStack(BlockBox ca) {
		listStack.push(ca);
	}

	protected BlockBox popListStack() {
		BlockBox box = listStack.pop();
		if (listStack.size()==0) {
			// We're not in a list any more
			log.debug("outside list");
		}
		return box;
	}
	protected BlockBox peekListStack() {
		return listStack.peek();
	}

	protected int getDepth() {
		return listStack.size();
	}



	private org.docx4j.dml.STTextAutonumberScheme getNumberFormatFromCSSListStyleType(String listStyleType) {
		

		if ( listStyleType.equals("decimal")) return STTextAutonumberScheme.ARABIC_PLAIN;

		if ( listStyleType.equals("decimal-leading-zero")) return STTextAutonumberScheme.ARABIC_PLAIN; // no match

		if ( listStyleType.equals("lower-roman")) return STTextAutonumberScheme.ROMAN_LC_PAREN_BOTH;
		if ( listStyleType.equals("upper-roman")) return STTextAutonumberScheme.ROMAN_UC_PAREN_BOTH;

		if ( listStyleType.equals("lower-greek")) return STTextAutonumberScheme.ARABIC_PLAIN; // no match

		if ( listStyleType.equals("lower-latin")) return STTextAutonumberScheme.ALPHA_LC_PAREN_BOTH; // no match
		if ( listStyleType.equals("upper-latin")) return STTextAutonumberScheme.ALPHA_UC_PAREN_BOTH; // no match

		if ( listStyleType.equals("armenian")) return STTextAutonumberScheme.ARABIC_PLAIN; // no match
		if ( listStyleType.equals("georgian")) return STTextAutonumberScheme.ARABIC_PLAIN; // no match

		if ( listStyleType.equals("lower-alpha")) return STTextAutonumberScheme.ALPHA_LC_PAREN_BOTH;
		if ( listStyleType.equals("upper-alpha")) return STTextAutonumberScheme.ALPHA_UC_PAREN_BOTH;

		if ( listStyleType.equals("none")) return null; // TODO FIXME 
		if ( listStyleType.equals("inherit")) return STTextAutonumberScheme.ARABIC_PLAIN; // TODO FIXME 

		return STTextAutonumberScheme.ARABIC_PLAIN; // appropriate fallback?

	}

//	private String getLvlTextFromCSSListStyleType(String listStyleType, int level) {
//
//		if ( listStyleType.equals("disc")) {
//			return "";
//		}
//		if ( listStyleType.equals("circle")) {
//			return "o";
//		}
//		if ( listStyleType.equals("square")) {
//			return "";
//		}
//
//		return "%"+level+".";
//	}

	private void addBulletForCSSListStyleType(String listStyleType, CTTextParagraphProperties textparagraphproperties) {
		
		/*
        <a:buFont typeface="Arial" panose="020B0604020202020204" pitchFamily="34" charset="0"/>
        <a:buChar char="•"/>
    */
	
    // Create object for buFont
    TextFont textfont = dmlObjectFactory.createTextFont(); 
    textparagraphproperties.setBuFont(textfont); 
        textfont.setTypeface( "Arial"); 
        textfont.setPanose( "020B0604020202020204"); 
//        textfont.setPitchFamily( new Byte(34) );
//        textfont.setCharset( new Byte(0) );
    // Create object for buChar
    CTTextCharBullet textcharbullet = dmlObjectFactory.createCTTextCharBullet(); 
    textparagraphproperties.setBuChar(textcharbullet); 
        textcharbullet.setChar( "•"); 
		
		if (listStyleType.equals("disc")) {
		}
		if (listStyleType.equals("circle")) {
		}
		if (listStyleType.equals("square")) {
		}

	}


	void addNumbering(CTTextParagraph textparagraph, Element e, Map<String, PropertyValue> map) {
		
		log.debug("add");
		
		
		
		/*
                            <a:p>
                                <a:pPr marL="171450" lvl="0" indent="-171450">
                                    <a:lnSpc>
                                        <a:spcPct val="110000"/>
                                    </a:lnSpc>
                                    <a:buFont typeface="Arial" panose="020B0604020202020204" pitchFamily="34" charset="0"/>
                                    <a:buChar char="•"/>
                                </a:pPr>
                                <a:r>
                                    <a:rPr lang="en-AU" sz="1200" dirty="false">
                                        <a:latin typeface="Metropolis" pitchFamily="2" charset="77"/>
                                    </a:rPr>
                                    <a:t>My bullet</a:t>
                                </a:r>
                            </a:p>
                            		 */


		    // Create object for pPr
		    CTTextParagraphProperties textparagraphproperties = dmlObjectFactory.createCTTextParagraphProperties(); 
		    textparagraph.setPPr(textparagraphproperties); 
		        textparagraphproperties.setIndent( new Integer(-171450) ); // TODO
		        textparagraphproperties.setLvl( this.getDepth() );
		        // Create object for lnSpc
		        CTTextSpacing textspacing = dmlObjectFactory.createCTTextSpacing(); 
		        textparagraphproperties.setLnSpc(textspacing); 
		            // Create object for spcPct
		            CTTextSpacingPercent textspacingpercent = dmlObjectFactory.createCTTextSpacingPercent(); 
		            textspacing.setSpcPct(textspacingpercent); 
		                textspacingpercent.setVal( 110000 );
		                
        String listStyleType = null;
        CSSValue cssVal = new DomCssValueAdaptor(map.get("list-style-type" ));
        if (cssVal==null) {
        	log.debug("No list-style-type found in css for element " + e.getTagName());		                
        } else {
        	listStyleType = cssVal.getCssText();
        }
		//  disc | circle | square |
		// decimal | decimal-leading-zero | lower-roman | upper-roman |
		// lower-greek | lower-latin | upper-latin | armenian | georgian |
		// lower-alpha | upper-alpha | none | inherit
		if ( listStyleType.equals("disc")
			 || listStyleType.equals("circle")
			 || listStyleType.equals("square")
				) {
			
			addBulletForCSSListStyleType( listStyleType,  textparagraphproperties);
		    textparagraphproperties.setMarL( new Integer(171450) );
			
		} else {
			
			/*
                <a:buFont typeface="+mj-lt"/>
                <a:buAutoNum type="arabicPeriod"/>
            		 */

			// Create object for buAutoNum
			CTTextAutonumberBullet textautonumberbullet = dmlObjectFactory.createCTTextAutonumberBullet(); 
			textparagraphproperties.setBuAutoNum(textautonumberbullet); 
			//textautonumberbullet.setStartAt( new Integer(1) );
			textautonumberbullet.setType(org.docx4j.dml.STTextAutonumberScheme.ARABIC_PERIOD);
			textparagraphproperties.setMarL( new Integer(228600) );		
		}
		
//		    // Create object for r
//		    CTRegularTextRun regulartextrun = dmlObjectFactory.createCTRegularTextRun(); 
//		    textparagraph.getEGTextRun().add( regulartextrun); 
//		        // Create object for rPr
//		        CTTextCharacterProperties textcharacterproperties = dmlObjectFactory.createCTTextCharacterProperties(); 
//		        regulartextrun.setRPr(textcharacterproperties); 
//		            textcharacterproperties.setLang( "en-AU"); 
//		            // Create object for latin
//		            TextFont textfont2 = dmlObjectFactory.createTextFont(); 
//		            textcharacterproperties.setLatin(textfont2); 
//		                textfont2.setTypeface( "Metropolis"); 
////		                textfont2.setPitchFamily( new Byte(2) );
////		                textfont2.setCharset( new Byte(77) );
//		            textcharacterproperties.setSz( new Integer(1200) );
//		            textcharacterproperties.setSmtId( new Long(0) );
////		        regulartextrun.setT( "My bullet"); 

	}


}


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

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.docx4j.UnitsOfMeasurement;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.model.properties.paragraph.Indent;
import org.docx4j.openpackaging.exceptions.InvalidOperationException;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.org.xhtmlrenderer.css.constants.CSSName;
import org.docx4j.org.xhtmlrenderer.css.style.derived.LengthValue;
import org.docx4j.org.xhtmlrenderer.layout.Styleable;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.docx4j.wml.CTLongHexNumber;
import org.docx4j.wml.Jc;
import org.docx4j.wml.Lvl;
import org.docx4j.wml.NumFmt;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.Numbering;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.PPrBase.Ind;
import org.docx4j.wml.PPrBase.NumPr;
import org.docx4j.wml.PPrBase.NumPr.Ilvl;
import org.docx4j.wml.PPrBase.NumPr.NumId;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Numbering.Num;
import org.docx4j.wml.Numbering.Num.AbstractNumId;
import org.docx4j.wml.Numbering.Num.LvlOverride;
import org.docx4j.wml.Numbering.Num.LvlOverride.StartOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;

public class ListHelper {
	
	public static Logger log = LoggerFactory.getLogger(ListHelper.class);		
	
	public ListHelper(XHTMLImporterImpl importer, NumberingDefinitionsPart ndp) {
    	this.importer=importer;
		this.ndp=ndp;
	}
	
	private XHTMLImporterImpl importer;

	// Commented out for now; See list.txt
//	public static final String XHTML_AbstractNum_For_OL = "XHTML_AbstractNum_For_OL";
//	public static final String XHTML_AbstractNum_For_UL = "XHTML_AbstractNum_For_UL";	
	
	private ObjectFactory wmlObjectFactory  = Context.getWmlObjectFactory();
	
	private NumberingDefinitionsPart ndp;
	
    private LinkedList<BlockBox> listStack = new LinkedList<BlockBox>();
	// These are the incoming ul and ol.
	// Generally, these will be BlockBox (display:inline or display:inline-block).
	// <ul style="display:inline"> hides them entirely..
    
    // The current list
    private Numbering.AbstractNum abstractList;
    private Numbering.Num concreteList;

	protected void pushListStack(BlockBox ca) {
		listStack.push(ca);
		pushListItemStateStack();
		
	}
	protected BlockBox popListStack() {
		BlockBox box = listStack.pop();
		if (listStack.size()==0) {
			// We're not in a list any more
			concreteList=null;
		}
		listItemStateStack.pop();
		return box;
	}
	protected BlockBox peekListStack() {
		return listStack.peek();
	}	
	
	protected int getDepth() {
		return listStack.size();
	}
	
	
	
    /**
     *  The ListItemContentState helps us handle structures such as:
     *  
     *  <li>
     *     <p>this item gets the bullet</p>
     *     <p>this one needs to be indented</p>
     *  </li>
     *  
     *  ListItemContentState needs to be re-inited as we enter 
     *  each list item.   
     */
    private LinkedList<ListItemContentState> listItemStateStack = new LinkedList<ListItemContentState>();
	
	class ListItemContentState {
	
		protected boolean isFirstChild = true;
		protected boolean haveMergedFirstP = false;
		
		void init() {
			isFirstChild = true;
			haveMergedFirstP = false;
		}
	
	}
	
	protected ListItemContentState peekListItemStateStack() {
		return listItemStateStack.peek();
	}	
	private void pushListItemStateStack() {
		listItemStateStack.push(new ListItemContentState());
	}	
	
	
	/**
	 * Creates a new empty abstract list.
	 * 
	 * @return
	 * @throws JAXBException
	 */
	protected Numbering.AbstractNum createNewAbstractList() {
		
	    // Create object for abstractNum
	    Numbering.AbstractNum numberingabstractnum = Context.getWmlObjectFactory().createNumberingAbstractNum(); 
//	    numbering.getAbstractNum().add( numberingabstractnum); 
	        numberingabstractnum.setAbstractNumId( BigInteger.valueOf( 0) );		
	        
//	        // Create object for nsid
//	        CTLongHexNumber longhexnumber = Context.getWmlObjectFactory().createCTLongHexNumber(); 
//	        numberingabstractnum.setNsid(longhexnumber); 
//	            longhexnumber.setVal( "3DEB26AB"); 
	            
	        // Create object for multiLevelType
	        Numbering.AbstractNum.MultiLevelType numberingabstractnummultileveltype = Context.getWmlObjectFactory().createNumberingAbstractNumMultiLevelType(); 
	        numberingabstractnum.setMultiLevelType(numberingabstractnummultileveltype); 
	            numberingabstractnummultileveltype.setVal( "multilevel"); 
	            
//	        // Create object for tmpl
//	        CTLongHexNumber longhexnumber2 = Context.getWmlObjectFactory().createCTLongHexNumber(); 
//	        numberingabstractnum.setTmpl(longhexnumber2); 
//	            longhexnumber2.setVal( "0C090023"); 	
	            
	        return numberingabstractnum;    
	}
	
	
	private Lvl getLevel(Numbering.AbstractNum theList, int level) {
		
		if (level>8) level=8; 
		
		for (Lvl lvl : theList.getLvl() ) {
			if (lvl.getIlvl().intValue()==level) return lvl;
		}
		return null;
	}

	private NumberFormat getNumberFormatFromCSSListStyleType(String listStyleType) {
		
		//  disc | circle | square | 
		// decimal | decimal-leading-zero | lower-roman | upper-roman | 
		// lower-greek | lower-latin | upper-latin | armenian | georgian | 
		// lower-alpha | upper-alpha | none | inherit
		if ( listStyleType.equals("disc")
				|| listStyleType.equals("circle")
				|| listStyleType.equals("square")
				) {
			return NumberFormat.BULLET;
		}

		if ( listStyleType.equals("decimal")) return NumberFormat.DECIMAL; 

		if ( listStyleType.equals("decimal-leading-zero")) return NumberFormat.DECIMAL_ZERO;   

		if ( listStyleType.equals("lower-roman")) return NumberFormat.LOWER_ROMAN; 
		if ( listStyleType.equals("upper-roman")) return NumberFormat.UPPER_ROMAN; 

		if ( listStyleType.equals("lower-greek")) return NumberFormat.DECIMAL;  // no match 

		if ( listStyleType.equals("lower-latin")) return NumberFormat.LOWER_LETTER; 
		if ( listStyleType.equals("upper-latin")) return NumberFormat.UPPER_LETTER; 

		if ( listStyleType.equals("armenian")) return NumberFormat.DECIMAL;  // no match
		if ( listStyleType.equals("georgian")) return NumberFormat.DECIMAL;  // no match

		if ( listStyleType.equals("lower-alpha")) return NumberFormat.LOWER_LETTER; 
		if ( listStyleType.equals("upper-alpha")) return NumberFormat.UPPER_LETTER; 
		
		if ( listStyleType.equals("none")) return NumberFormat.NONE; 
		if ( listStyleType.equals("inherit")) return NumberFormat.DECIMAL; // TODO FIXME -
		
		return NumberFormat.DECIMAL; // appropriate fallback?
		
	}

	private String getLvlTextFromCSSListStyleType(String listStyleType, int level) {
		
		if ( listStyleType.equals("disc")) {
			return "";
		}
		if ( listStyleType.equals("circle")) {
			return "o";
		}
		if ( listStyleType.equals("square")) {
			return "";
		}
		
		return "%"+level;
	}
	
	private RFonts geRFontsForCSSListStyleType(String listStyleType) {
		RFonts rfonts = null;
		if (listStyleType.equals("disc")) {
			rfonts = wmlObjectFactory.createRFonts();
			rfonts.setAscii("Symbol");
			rfonts.setHint(org.docx4j.wml.STHint.DEFAULT);
			rfonts.setHAnsi("Symbol");
		}
		if (listStyleType.equals("circle")) {
			rfonts = wmlObjectFactory.createRFonts();
			rfonts.setAscii("Courier New");
			rfonts.setHint(org.docx4j.wml.STHint.DEFAULT);
			rfonts.setHAnsi("Courier New");
			rfonts.setCs("Courier New");
		}
		if (listStyleType.equals("square")) {
			rfonts = wmlObjectFactory.createRFonts();
			rfonts.setAscii("Wingdings");
			rfonts.setHint(org.docx4j.wml.STHint.DEFAULT);
			rfonts.setHAnsi("Wingdings");
		}
		return rfonts;
	}
	
	protected Ind getInd(int twip) {
		
		if (twip < 40) twip = 40;  // TMP FIXME!
		
		Ind ind = Context.getWmlObjectFactory().createPPrBaseInd();
		
//		ind.setLeft(BigInteger.valueOf(twip) );
		
		// Hanging hack
		ind.setLeft(BigInteger.valueOf(twip+360) );
		ind.setHanging(BigInteger.valueOf(360) );
		return ind;
	}
	
	protected int getAncestorIndentation() {

        // Indentation.  Sum of padding-left and margin-left on ancestor ol|ul
        // Expectation is that one or other would generally be used.
		int totalPadding = 0;
		for(BlockBox bb : listStack) {
			
			log.debug(bb.getElement().getLocalName());
			
            LengthValue padding = (LengthValue)bb.getStyle().valueByName(CSSName.PADDING_LEFT);
            totalPadding +=Indent.getTwip(padding.getCSSPrimitiveValue());
            
            log.debug("+padding-left: " + totalPadding);
            
            LengthValue margin = (LengthValue)bb.getStyle().valueByName(CSSName.MARGIN_LEFT);
            totalPadding +=Indent.getTwip(margin.getCSSPrimitiveValue());
            
            log.debug("+margin-left: " + totalPadding);
            
		}
		return totalPadding;
	}
	
    protected int getAbsoluteListItemIndent(Styleable styleable) {

		int totalPadding = 0;
        LengthValue padding = (LengthValue)styleable.getStyle().valueByName(CSSName.PADDING_LEFT);
        totalPadding +=Indent.getTwip(padding.getCSSPrimitiveValue());
        
        LengthValue margin = (LengthValue)styleable.getStyle().valueByName(CSSName.MARGIN_LEFT);
        totalPadding +=Indent.getTwip(margin.getCSSPrimitiveValue());    			                
    	
        totalPadding +=getAncestorIndentation();
        
        return totalPadding;
    }
	

		
	private Lvl createLevel(int level, Map<String, CSSValue> cssMap) {
		
		if (level>8) level=8; // Word can't open a document with Ilvl>8

        // Create object for lvl
        Lvl lvl = wmlObjectFactory.createLvl(); 
            lvl.setIlvl( BigInteger.valueOf( level) );
            
//            // Create object for pStyle
//            Lvl.PStyle lvlpstyle = wmlObjectFactory.createLvlPStyle(); 
//            lvl.setPStyle(lvlpstyle); 
//                lvlpstyle.setVal( "Heading1"); 
                
            // Create object for pPr
            PPr ppr = wmlObjectFactory.createPPr(); 
            lvl.setPPr(ppr); 
            
            ppr.setInd(getInd(getAncestorIndentation())); 
                    
            // Create object for numFmt
            NumFmt numfmt = wmlObjectFactory.createNumFmt(); 
            lvl.setNumFmt(numfmt); 
                numfmt.setVal(
                		getNumberFormatFromCSSListStyleType(
                				cssMap.get("list-style-type" ).getCssText()));
                
                
            // Create object for lvlText
            Lvl.LvlText lvllvltext = wmlObjectFactory.createLvlLvlText(); 
            lvl.setLvlText(lvllvltext); 
                lvllvltext.setVal( getLvlTextFromCSSListStyleType(
        				cssMap.get("list-style-type" ).getCssText(), 
        				level+1));

            // Bullets have an associated font
            RFonts rfonts = geRFontsForCSSListStyleType(cssMap.get("list-style-type" ).getCssText());
            if (rfonts!=null) {
            	RPr rpr = wmlObjectFactory.createRPr(); 
    	        rpr.setRFonts(rfonts);
    	        lvl.setRPr(rpr);
            }
                
                
            // Create object for lvlJc
            Jc jc = wmlObjectFactory.createJc(); 
            lvl.setLvlJc(jc); 
                jc.setVal(org.docx4j.wml.JcEnumeration.LEFT);
                
            // Create object for start
            Lvl.Start lvlstart = wmlObjectFactory.createLvlStart(); 
            lvl.setStart(lvlstart); 
                
            BlockBox list = listStack.peek(); 
            Element listEl = list.getElement();
            BigInteger startVal = null;
            if (listEl.hasAttribute("start") ) {
            	try {
            		startVal = BigInteger.valueOf(Long.parseLong(listEl.getAttribute("start")));
            	} catch (NumberFormatException nfe) {
            		log.warn("Can't parse number from @start=" + listEl.getAttribute("start"));
            	}
            }
            if (startVal==null) {
                lvlstart.setVal( BigInteger.valueOf( 1) );
            } else {
                lvlstart.setVal( startVal );
            	
            }
                
            return lvl;
                
	}
	
	void addNumbering(P p, Element e, Map<String, CSSValue> cssMap) {
		
		if (concreteList==null) {
			// We've just entered a list, so create a new one
			abstractList = createNewAbstractList();		
			concreteList = ndp.addAbstractListNumberingDefinition(abstractList);			
		}
		
		// Do we have a definition for this level yet?
		Lvl lvl = getLevel(abstractList, listStack.size()-1);
		if (lvl==null) {
			// Nope, need to create it
			int level = listStack.size()-1;
			ndp.addAbstractListNumberingDefinitionLevel(abstractList, createLevel(level, cssMap));
			//log.debug("ADDED LEVEL " + level);
		} else {
			log.debug("Numbering definition exists for this level " + lvl.getIlvl().intValue());
			// Can we re-use it?
            NumFmt numfmtExisting = lvl.getNumFmt(); 
            
            if (concreteList.getLvlOverride().size()>0
            		&& concreteList.getLvlOverride().get(0).getIlvl().intValue()==(listStack.size()-1)) {
            	
            		// TODO: assumes a single override level is defined
            	
            	Lvl overrideLvlTmp = concreteList.getLvlOverride().get(0).getLvl();
            	if (overrideLvlTmp.getNumFmt()!=null) {
            		numfmtExisting = overrideLvlTmp.getNumFmt();
            	}
            }
            
            NumberFormat specified = getNumberFormatFromCSSListStyleType(
                				cssMap.get("list-style-type" ).getCssText());
			if (numfmtExisting.getVal()==specified) {
				log.debug(".. using pre-existing definition ");				
			} else {
				log.debug(".. but it is different");	
				// do we have a suitable override?
				
					// at present, we define a new override each time
				
				// if not, we need to add an override
			    // docx4j provides machinery to restart numbering
				int ilvl = lvl.getIlvl().intValue();
			    long newNumId = ndp.restart(concreteList.getAbstractNumId().getVal().longValue(), ilvl, 
			    		/* restart at */ 1);
			    // retrieve it
			    ListNumberingDefinition listDef = ndp.getInstanceListDefinitions().get(""+newNumId);
			    
			    concreteList = listDef.getNumNode();
			    
			    // TODO code below is copy/pasted.  Should extract method.
			    
		        // Create object for lvl
		        Lvl overrideLvl = wmlObjectFactory.createLvl(); 
		            overrideLvl.setIlvl( BigInteger.valueOf( ilvl) );
		            
		            // Create object for pPr
		            PPr ppr = wmlObjectFactory.createPPr(); 
		            overrideLvl.setPPr(ppr); 
		            
		            ppr.setInd(getInd(getAncestorIndentation())); 
		                    
		            // Create object for numFmt
		            NumFmt numfmt = wmlObjectFactory.createNumFmt(); 
		            overrideLvl.setNumFmt(numfmt); 
		                numfmt.setVal(
		                		getNumberFormatFromCSSListStyleType(
		                				cssMap.get("list-style-type" ).getCssText()));
		                
		                
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext = wmlObjectFactory.createLvlLvlText(); 
		            overrideLvl.setLvlText(lvllvltext); 
		                lvllvltext.setVal( getLvlTextFromCSSListStyleType(
		        				cssMap.get("list-style-type" ).getCssText(), 
		        				ilvl+1));
		                
	                // Bullets have an associated font
	                RFonts rfonts = geRFontsForCSSListStyleType(cssMap.get("list-style-type" ).getCssText());
	                if (rfonts!=null) {
	                	RPr rpr = wmlObjectFactory.createRPr(); 
	        	        rpr.setRFonts(rfonts);
	        	        overrideLvl.setRPr(rpr);
	                }
		                
			    listDef.getNumNode().getLvlOverride().get(0).setLvl(overrideLvl);
			    
			}
			
		}
		
		setNumbering(p.getPPr(), concreteList.getNumId());
		
	}
	
	
	protected void setNumbering(PPr pPr, BigInteger numId) {
		
	    // Create and add <w:numPr>
	    NumPr numPr =  Context.getWmlObjectFactory().createPPrBaseNumPr();
	    pPr.setNumPr(numPr);

	    // The <w:numId> element
	    NumId numIdElement = Context.getWmlObjectFactory().createPPrBaseNumPrNumId();
	    numPr.setNumId(numIdElement);
	    numIdElement.setVal( numId ); // point to the correct list
	    	    
	    // The <w:ilvl> element
	    Ilvl ilvlElement = Context.getWmlObjectFactory().createPPrBaseNumPrIlvl();
	    numPr.setIlvl(ilvlElement);
	    ilvlElement.setVal(BigInteger.valueOf(this.listStack.size()-1));
	    
	    // TMP: don't let this override our numbering
//	    p.getPPr().setInd(null);
		
	}
	
	

}

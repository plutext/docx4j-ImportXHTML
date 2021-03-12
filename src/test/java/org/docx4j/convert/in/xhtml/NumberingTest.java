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

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTLongHexNumber;
import org.docx4j.wml.Jc;
import org.docx4j.wml.Lvl;
import org.docx4j.wml.NumFmt;
import org.docx4j.wml.Numbering;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.PPrBase.Ind;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;
import org.docx4j.wml.Styles;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class NumberingTest {

	private WordprocessingMLPackage wordMLPackage;
//    private XHTMLImporter XHTMLImporter = new XHTMLImporter(wordMLPackage);		

	
	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	private List<Object> convert(String xhtml, FormattingOption paragraphFormattingOption) throws Docx4JException {
		XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);				
		XHTMLImporter.setParagraphFormatting(paragraphFormattingOption);
		return XHTMLImporter.convert(xhtml, "");
	}
	
	// ===============================================================================
	// FormattingOption tests
	// - basic tests of @class

	@Test public void testUnorderedWithStylePresent() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul class=\"MyListStyle\">"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	List<Object> results = convert( xhtml, FormattingOption.CLASS_TO_STYLE_ONLY);
    		
    	P p = (P)results.get(0);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()==PREDEFINED_OL_NUMID);
    	
    	// CLASS_TO_STYLE_ONLY, so Indent should not be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}

	@Test public void testUnorderedWithStyleOnlyAndAbsent() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul class=\"My_MISSING_ListStyle\">"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	List<Object> results = convert( xhtml, FormattingOption.CLASS_TO_STYLE_ONLY);
    		
    	P p = (P)results.get(0);
    	
    	// Can't number
    	assertTrue( p.getPPr().getNumPr()==null);
    	
    	// CLASS_TO_STYLE_ONLY, so Indent should not be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}
	
	@Test public void testUnorderedWithStyleAbsent() throws Docx4JException {
				
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul class=\"My_MISSING_ListStyle\">"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	List<Object> results = convert( xhtml, FormattingOption.CLASS_PLUS_OTHER);
    	
//    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(0);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);
	}

	@Test public void testUnorderedIgnoreClass() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul class=\"MyListStyle\">"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
//    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(0);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);
	}
	
	// ===============================================================================
	// indentation tests

	@Ignore // TODO: broken in 3.3.6; revisit this
	public void testUnorderedCssOnLiToIndent() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul>"
				+"<li style=\"margin-left: 1in;\">List item one</li>" // TODO this is currently ignored?
				+"</ul>"+
    		  "</div>";
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(0);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);
    	
    	// default of 600 + 1440 + hanging hack (360)    	
    	
    	wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().initialiseMaps(); // TODO this shouldn't be necessary
    	
    	Ind ind = wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getInd(p.getPPr().getNumPr());
    	System.out.println(XmlUtils.marshaltoString(ind));
    	assertTrue( ind.getLeft().intValue()==2400);    	
	}
	
	/**
	 * We go to some effort to honour an 'inherited' value.
	 */
	@Test public void testUnorderedCssOnUlToIndent() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul style=\"margin-left: 0.5in;\">"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	// 40px default = 600
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(0);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);

    	
    	wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().initialiseMaps(); // TODO this shouldn't be necessary
    	
    	Ind ind = wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getInd(p.getPPr().getNumPr());
    	//System.out.println(XmlUtils.marshaltoString(ind));
    	assertTrue( ind.getLeft().intValue()==1680);
	}
	
	
	
	/**
	 * Don't add pPr Ind for default css
	 */
	@Test public void testUnorderedCssDefaultIndentUnset() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul>"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(0);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);
    	
    	// Indent should not be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}

	/**
	 * Since we don't add pPr Ind for default css, an explicit setting which
	 * evaluates to same, will be ignored.
	 */
	@Test public void testUnorderedCssDefaultIndentFalsePositive() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
    	String xhtml= "<div>" +
				"<ul style=\"margin-left: 0in;\">"
				+"<li>List item one</li>"
				+"</ul>"+
    		  "</div>";
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(0);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);
    	
    	// Indent should be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}
	
	// ===============================================================================
	// nested list tests

	/**
	 * For a nested list, we should get ilvl right; there should no pPr ind
	 */
	@Test public void testNestedNoClass() throws Docx4JException {
				
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
		String xhtml= "<div>" +
		"<ul>"
		+"<li>List item two with subitems:"
			+"<ul>"
		        +"<li>Subitem 1</li>"
		    +"</ul>"
		+"</li>"
		+"</ul>"+
	  "</div>";    	
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(1);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);

    	assertTrue( p.getPPr().getNumPr().getIlvl()!=null);
    	assertTrue( p.getPPr().getNumPr().getIlvl().getVal().intValue()==1);  // nested
    	
    	// Indent should not be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}

	@Test public void testNestedNoClassButExplicitIndent() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
		String xhtml= "<div>" +
		"<ul>"
		+"<li>List item two with subitems:"
			+"<ul  style=\"margin-left: 2in;\">"
		        +"<li>Subitem 1</li>"
		    +"</ul>"
		+"</li>"
		+"</ul>"+
	  "</div>";    	
    	
    	List<Object> results = convert( xhtml, FormattingOption.IGNORE_CLASS);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(1);
    	
    	// Should be numbered, but not using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID);

    	assertTrue( p.getPPr().getNumPr().getIlvl()!=null);
    	assertTrue( p.getPPr().getNumPr().getIlvl().getVal().intValue()==1);  // nested
    	
   	// default of 2*600 + 2880 + hanging hack (360)
    	
    	wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().initialiseMaps(); // TODO this shouldn't be necessary
    	
    	Ind ind = wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getInd(p.getPPr().getNumPr());
    	//System.out.println(XmlUtils.marshaltoString(ind));
    	assertTrue( ind.getLeft().intValue()==4440);    	
    	
	}
	
	/**
	 * For a nested list, we should get ilvl right; there should no pPr ind
	 */
	@Test public void testNestedWithClass() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
		String xhtml= "<div>" +
		"<ul>"
		+"<li>List item two with subitems:"
		    +"<ul class=\"MyListStyle\">"
		        +"<li>Subitem 1</li>"
		    +"</ul>"
		+"</li>"
		+"</ul>"+
	  "</div>";    	
    	
    	List<Object> results = convert( xhtml, FormattingOption.CLASS_PLUS_OTHER);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(1);
    	
    	// Should be numbered, using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()==PREDEFINED_OL_NUMID);

    	assertTrue( p.getPPr().getNumPr().getIlvl()!=null);
    	assertTrue( p.getPPr().getNumPr().getIlvl().getVal().intValue()==1);  // nested
    	
    	// Indent should be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}
	
	/**
	 * For a nested list, we should get ilvl right; there should no pPr ind
	 */
	@Test public void testNestedWithNoClassOnLevel() throws Docx4JException {
		
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		
		String xhtml= "<div>" +
		"<ul class=\"MyListStyle\">"
		+"<li>List item two with subitems:"
		    +"<ul >" // no class here
		        +"<li>Subitem 1</li>"
		    +"</ul>"
		+"</li>"
		+"</ul>"+
	  "</div>";    	
    	
    	List<Object> results = convert( xhtml, FormattingOption.CLASS_PLUS_OTHER);
    	
    	wordMLPackage.getMainDocumentPart().getContent().addAll(results);
    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
//    	System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
	
    	P p = (P)results.get(1);
    	
    	// Should be numbered, using our predefined list
    	assertTrue( p.getPPr().getNumPr()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId()!=null);
    	assertTrue( p.getPPr().getNumPr().getNumId().getVal().intValue()!=PREDEFINED_OL_NUMID); // since no @class on this level

    	assertTrue( p.getPPr().getNumPr().getIlvl()!=null);
    	assertTrue( p.getPPr().getNumPr().getIlvl().getVal().intValue()==1);  // nested
    	
    	// Indent should be present in pPr
    	assertTrue( p.getPPr().getInd()==null);
    	
	}

	@Test public void testListItemValueOverridden() throws Docx4JException {
		this.addNumberingPart(wordMLPackage.getMainDocumentPart());
		this.addStylesPart(wordMLPackage.getMainDocumentPart());
		String xhtml= "<div>" +
				"<ol>"
				+"<li>Item 1</li>"
				+"<li value=\"1\">Second item with 1 as number</li>"
				+"<li>Item 2</li>"
				+"<li value=\"2\">Second item with 2 as number</li>"
				+"</ol>"+
				"</div>";
		List<Object> results = convert( xhtml, FormattingOption.CLASS_PLUS_OTHER);
		wordMLPackage.getMainDocumentPart().getContent().addAll(results);
		System.out.println(XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
		final P item1 = (P) results.get(0);
		final P secondItem1 = (P) results.get(1);
		final P item2 = (P) results.get(2);
		final P secondItem2 = (P) results.get(3);
		assertEquals(BigInteger.valueOf(2L),item1.getPPr().getNumPr().getNumId().getVal());
		assertEquals(BigInteger.valueOf(3L),secondItem1.getPPr().getNumPr().getNumId().getVal());
		assertEquals(BigInteger.valueOf(3L),item2.getPPr().getNumPr().getNumId().getVal());
		assertEquals(BigInteger.valueOf(4L),secondItem2.getPPr().getNumPr().getNumId().getVal());
	}


	// ===============================================================================
	// machinery / helpers
	
	
	private void addStylesPart(MainDocumentPart  mdp) throws InvalidFormatException {
		
		StyleDefinitionsPart sdp = new StyleDefinitionsPart(); 
		mdp.addTargetPart(sdp);
		
		sdp.setJaxbElement(getStyles());
	}

	public Styles getStyles() {
		
		org.docx4j.wml.ObjectFactory wmlObjectFactory = new org.docx4j.wml.ObjectFactory();
		
		Styles styles = wmlObjectFactory.createStyles(); 
		    // Create object for style
		    Style style = wmlObjectFactory.createStyle(); 
		    styles.getStyle().add( style); 
		        style.setStyleId( "MyListStyle"); 
		        // Create object for pPr
		        PPr ppr = wmlObjectFactory.createPPr(); 
		        style.setPPr(ppr); 
		            // Create object for numPr
		            PPrBase.NumPr pprbasenumpr = wmlObjectFactory.createPPrBaseNumPr(); 
		            ppr.setNumPr(pprbasenumpr); 
		                // Create object for numId
		                PPrBase.NumPr.NumId pprbasenumprnumid = wmlObjectFactory.createPPrBaseNumPrNumId(); 
		                pprbasenumpr.setNumId(pprbasenumprnumid); 
		                    pprbasenumprnumid.setVal( BigInteger.valueOf( PREDEFINED_OL_NUMID ) ); 
		        // Create object for uiPriority
		        Style.UiPriority styleuipriority = wmlObjectFactory.createStyleUiPriority(); 
		        style.setUiPriority(styleuipriority); 
		            styleuipriority.setVal( BigInteger.valueOf( 99) ); 
		        // Create object for name
		        Style.Name stylename = wmlObjectFactory.createStyleName(); 
		        style.setName(stylename); 
		            stylename.setVal( "MyListStyle"); 
		        style.setType( "numbering"); 
		        
		// org.docx4j.model.PropertyResolver.init(PropertyResolver.java:173)
		// currently requires a default p style
		styles.getStyle().add(createNormal());
		
		return styles;
	}

	public Style createNormal() {

		org.docx4j.wml.ObjectFactory wmlObjectFactory = new org.docx4j.wml.ObjectFactory();

		Style style = wmlObjectFactory.createStyle(); 
		    style.setStyleId( "Normal"); 
		    // Create object for qFormat
		    BooleanDefaultTrue booleandefaulttrue = wmlObjectFactory.createBooleanDefaultTrue(); 
		    style.setQFormat(booleandefaulttrue); 
		    // Create object for name
		    Style.Name stylename = wmlObjectFactory.createStyleName(); 
		    style.setName(stylename); 
		        stylename.setVal( "Normal"); 
		    style.setType( "paragraph"); 
		    
		    style.setDefault(true);

		return style;
	}	
	
	private void addNumberingPart(MainDocumentPart  mdp) throws InvalidFormatException {
		
		NumberingDefinitionsPart ndp = new NumberingDefinitionsPart(); 
		mdp.addTargetPart(ndp);
		
		ndp.setJaxbElement(getNumbering());
	}
	
	private static int PREDEFINED_OL_ABSTRACT_NUMID = 10;
	private static int PREDEFINED_OL_NUMID = 11;

	public Numbering getNumbering() {

		org.docx4j.wml.ObjectFactory wmlObjectFactory = new org.docx4j.wml.ObjectFactory();

		Numbering numbering = wmlObjectFactory.createNumbering(); 
		
		    // Create object for numberingnum
		    Numbering.Num numberingnum = wmlObjectFactory.createNumberingNum(); 
		    numbering.getNum().add( numberingnum); 
		        numberingnum.setNumId( BigInteger.valueOf( PREDEFINED_OL_NUMID ) );  // so we can detect this in use
		        // Create object for abstractNumId
		        Numbering.Num.AbstractNumId numberingnumabstractnumid = wmlObjectFactory.createNumberingNumAbstractNumId(); 
		        numberingnum.setAbstractNumId(numberingnumabstractnumid); 
		            numberingnumabstractnumid.setVal( BigInteger.valueOf( PREDEFINED_OL_ABSTRACT_NUMID) ); 
		            
		    // Create object for abstractNum
		    Numbering.AbstractNum numberingabstractnum = wmlObjectFactory.createNumberingAbstractNum(); 
		    numbering.getAbstractNum().add( numberingabstractnum); 
		        numberingabstractnum.setAbstractNumId( BigInteger.valueOf( PREDEFINED_OL_ABSTRACT_NUMID ) ); 
		        // Create object for lvl
		        Lvl lvl = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl); 
		            lvl.setIlvl( BigInteger.valueOf( 0) ); 
		            // Create object for pPr
		            PPr ppr = wmlObjectFactory.createPPr(); 
		            lvl.setPPr(ppr); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind = wmlObjectFactory.createPPrBaseInd(); 
		                ppr.setInd(pprbaseind); 
		                    pprbaseind.setLeft( BigInteger.valueOf( 720) ); 
		                    pprbaseind.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr = wmlObjectFactory.createRPr(); 
		            lvl.setRPr(rpr); 
		                // Create object for rFonts
		                RFonts rfonts = wmlObjectFactory.createRFonts(); 
		                rpr.setRFonts(rfonts); 
		                    rfonts.setAscii( "Symbol"); 
		                    rfonts.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts.setHAnsi( "Symbol"); 
		            // Create object for numFmt
		            NumFmt numfmt = wmlObjectFactory.createNumFmt(); 
		            lvl.setNumFmt(numfmt); 
		                numfmt.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext = wmlObjectFactory.createLvlLvlText(); 
		            lvl.setLvlText(lvllvltext); 
		                lvllvltext.setVal( ""); 
		            // Create object for lvlJc
		            Jc jc = wmlObjectFactory.createJc(); 
		            lvl.setLvlJc(jc); 
		                jc.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart = wmlObjectFactory.createLvlStart(); 
		            lvl.setStart(lvlstart); 
		                lvlstart.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl2 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl2); 
		            lvl2.setIlvl( BigInteger.valueOf( 1) ); 
		            // Create object for pPr
		            PPr ppr2 = wmlObjectFactory.createPPr(); 
		            lvl2.setPPr(ppr2); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind2 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr2.setInd(pprbaseind2); 
		                    pprbaseind2.setLeft( BigInteger.valueOf( 1440) ); 
		                    pprbaseind2.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr2 = wmlObjectFactory.createRPr(); 
		            lvl2.setRPr(rpr2); 
		                // Create object for rFonts
		                RFonts rfonts2 = wmlObjectFactory.createRFonts(); 
		                rpr2.setRFonts(rfonts2); 
		                    rfonts2.setAscii( "Courier New"); 
		                    rfonts2.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts2.setHAnsi( "Courier New"); 
		                    rfonts2.setCs( "Courier New"); 
		            // Create object for numFmt
		            NumFmt numfmt2 = wmlObjectFactory.createNumFmt(); 
		            lvl2.setNumFmt(numfmt2); 
		                numfmt2.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext2 = wmlObjectFactory.createLvlLvlText(); 
		            lvl2.setLvlText(lvllvltext2); 
		                lvllvltext2.setVal( "o"); 
		            // Create object for lvlJc
		            Jc jc2 = wmlObjectFactory.createJc(); 
		            lvl2.setLvlJc(jc2); 
		                jc2.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart2 = wmlObjectFactory.createLvlStart(); 
		            lvl2.setStart(lvlstart2); 
		                lvlstart2.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl3 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl3); 
		            lvl3.setIlvl( BigInteger.valueOf( 2) ); 
		            // Create object for pPr
		            PPr ppr3 = wmlObjectFactory.createPPr(); 
		            lvl3.setPPr(ppr3); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind3 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr3.setInd(pprbaseind3); 
		                    pprbaseind3.setLeft( BigInteger.valueOf( 2160) ); 
		                    pprbaseind3.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr3 = wmlObjectFactory.createRPr(); 
		            lvl3.setRPr(rpr3); 
		                // Create object for rFonts
		                RFonts rfonts3 = wmlObjectFactory.createRFonts(); 
		                rpr3.setRFonts(rfonts3); 
		                    rfonts3.setAscii( "Wingdings"); 
		                    rfonts3.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts3.setHAnsi( "Wingdings"); 
		            // Create object for numFmt
		            NumFmt numfmt3 = wmlObjectFactory.createNumFmt(); 
		            lvl3.setNumFmt(numfmt3); 
		                numfmt3.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext3 = wmlObjectFactory.createLvlLvlText(); 
		            lvl3.setLvlText(lvllvltext3); 
		                lvllvltext3.setVal( ""); 
		            // Create object for lvlJc
		            Jc jc3 = wmlObjectFactory.createJc(); 
		            lvl3.setLvlJc(jc3); 
		                jc3.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart3 = wmlObjectFactory.createLvlStart(); 
		            lvl3.setStart(lvlstart3); 
		                lvlstart3.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl4 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl4); 
		            lvl4.setIlvl( BigInteger.valueOf( 3) ); 
		            // Create object for pPr
		            PPr ppr4 = wmlObjectFactory.createPPr(); 
		            lvl4.setPPr(ppr4); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind4 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr4.setInd(pprbaseind4); 
		                    pprbaseind4.setLeft( BigInteger.valueOf( 2880) ); 
		                    pprbaseind4.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr4 = wmlObjectFactory.createRPr(); 
		            lvl4.setRPr(rpr4); 
		                // Create object for rFonts
		                RFonts rfonts4 = wmlObjectFactory.createRFonts(); 
		                rpr4.setRFonts(rfonts4); 
		                    rfonts4.setAscii( "Symbol"); 
		                    rfonts4.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts4.setHAnsi( "Symbol"); 
		            // Create object for numFmt
		            NumFmt numfmt4 = wmlObjectFactory.createNumFmt(); 
		            lvl4.setNumFmt(numfmt4); 
		                numfmt4.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext4 = wmlObjectFactory.createLvlLvlText(); 
		            lvl4.setLvlText(lvllvltext4); 
		                lvllvltext4.setVal( ""); 
		            // Create object for lvlJc
		            Jc jc4 = wmlObjectFactory.createJc(); 
		            lvl4.setLvlJc(jc4); 
		                jc4.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart4 = wmlObjectFactory.createLvlStart(); 
		            lvl4.setStart(lvlstart4); 
		                lvlstart4.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl5 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl5); 
		            lvl5.setIlvl( BigInteger.valueOf( 4) ); 
		            // Create object for pPr
		            PPr ppr5 = wmlObjectFactory.createPPr(); 
		            lvl5.setPPr(ppr5); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind5 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr5.setInd(pprbaseind5); 
		                    pprbaseind5.setLeft( BigInteger.valueOf( 3600) ); 
		                    pprbaseind5.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr5 = wmlObjectFactory.createRPr(); 
		            lvl5.setRPr(rpr5); 
		                // Create object for rFonts
		                RFonts rfonts5 = wmlObjectFactory.createRFonts(); 
		                rpr5.setRFonts(rfonts5); 
		                    rfonts5.setAscii( "Courier New"); 
		                    rfonts5.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts5.setHAnsi( "Courier New"); 
		                    rfonts5.setCs( "Courier New"); 
		            // Create object for numFmt
		            NumFmt numfmt5 = wmlObjectFactory.createNumFmt(); 
		            lvl5.setNumFmt(numfmt5); 
		                numfmt5.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext5 = wmlObjectFactory.createLvlLvlText(); 
		            lvl5.setLvlText(lvllvltext5); 
		                lvllvltext5.setVal( "o"); 
		            // Create object for lvlJc
		            Jc jc5 = wmlObjectFactory.createJc(); 
		            lvl5.setLvlJc(jc5); 
		                jc5.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart5 = wmlObjectFactory.createLvlStart(); 
		            lvl5.setStart(lvlstart5); 
		                lvlstart5.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl6 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl6); 
		            lvl6.setIlvl( BigInteger.valueOf( 5) ); 
		            // Create object for pPr
		            PPr ppr6 = wmlObjectFactory.createPPr(); 
		            lvl6.setPPr(ppr6); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind6 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr6.setInd(pprbaseind6); 
		                    pprbaseind6.setLeft( BigInteger.valueOf( 4320) ); 
		                    pprbaseind6.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr6 = wmlObjectFactory.createRPr(); 
		            lvl6.setRPr(rpr6); 
		                // Create object for rFonts
		                RFonts rfonts6 = wmlObjectFactory.createRFonts(); 
		                rpr6.setRFonts(rfonts6); 
		                    rfonts6.setAscii( "Wingdings"); 
		                    rfonts6.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts6.setHAnsi( "Wingdings"); 
		            // Create object for numFmt
		            NumFmt numfmt6 = wmlObjectFactory.createNumFmt(); 
		            lvl6.setNumFmt(numfmt6); 
		                numfmt6.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext6 = wmlObjectFactory.createLvlLvlText(); 
		            lvl6.setLvlText(lvllvltext6); 
		                lvllvltext6.setVal( ""); 
		            // Create object for lvlJc
		            Jc jc6 = wmlObjectFactory.createJc(); 
		            lvl6.setLvlJc(jc6); 
		                jc6.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart6 = wmlObjectFactory.createLvlStart(); 
		            lvl6.setStart(lvlstart6); 
		                lvlstart6.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl7 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl7); 
		            lvl7.setIlvl( BigInteger.valueOf( 6) ); 
		            // Create object for pPr
		            PPr ppr7 = wmlObjectFactory.createPPr(); 
		            lvl7.setPPr(ppr7); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind7 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr7.setInd(pprbaseind7); 
		                    pprbaseind7.setLeft( BigInteger.valueOf( 5040) ); 
		                    pprbaseind7.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr7 = wmlObjectFactory.createRPr(); 
		            lvl7.setRPr(rpr7); 
		                // Create object for rFonts
		                RFonts rfonts7 = wmlObjectFactory.createRFonts(); 
		                rpr7.setRFonts(rfonts7); 
		                    rfonts7.setAscii( "Symbol"); 
		                    rfonts7.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts7.setHAnsi( "Symbol"); 
		            // Create object for numFmt
		            NumFmt numfmt7 = wmlObjectFactory.createNumFmt(); 
		            lvl7.setNumFmt(numfmt7); 
		                numfmt7.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext7 = wmlObjectFactory.createLvlLvlText(); 
		            lvl7.setLvlText(lvllvltext7); 
		                lvllvltext7.setVal( ""); 
		            // Create object for lvlJc
		            Jc jc7 = wmlObjectFactory.createJc(); 
		            lvl7.setLvlJc(jc7); 
		                jc7.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart7 = wmlObjectFactory.createLvlStart(); 
		            lvl7.setStart(lvlstart7); 
		                lvlstart7.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl8 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl8); 
		            lvl8.setIlvl( BigInteger.valueOf( 7) ); 
		            // Create object for pPr
		            PPr ppr8 = wmlObjectFactory.createPPr(); 
		            lvl8.setPPr(ppr8); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind8 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr8.setInd(pprbaseind8); 
		                    pprbaseind8.setLeft( BigInteger.valueOf( 5760) ); 
		                    pprbaseind8.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr8 = wmlObjectFactory.createRPr(); 
		            lvl8.setRPr(rpr8); 
		                // Create object for rFonts
		                RFonts rfonts8 = wmlObjectFactory.createRFonts(); 
		                rpr8.setRFonts(rfonts8); 
		                    rfonts8.setAscii( "Courier New"); 
		                    rfonts8.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts8.setHAnsi( "Courier New"); 
		                    rfonts8.setCs( "Courier New"); 
		            // Create object for numFmt
		            NumFmt numfmt8 = wmlObjectFactory.createNumFmt(); 
		            lvl8.setNumFmt(numfmt8); 
		                numfmt8.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext8 = wmlObjectFactory.createLvlLvlText(); 
		            lvl8.setLvlText(lvllvltext8); 
		                lvllvltext8.setVal( "o"); 
		            // Create object for lvlJc
		            Jc jc8 = wmlObjectFactory.createJc(); 
		            lvl8.setLvlJc(jc8); 
		                jc8.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart8 = wmlObjectFactory.createLvlStart(); 
		            lvl8.setStart(lvlstart8); 
		                lvlstart8.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for lvl
		        Lvl lvl9 = wmlObjectFactory.createLvl(); 
		        numberingabstractnum.getLvl().add( lvl9); 
		            lvl9.setIlvl( BigInteger.valueOf( 8) ); 
		            // Create object for pPr
		            PPr ppr9 = wmlObjectFactory.createPPr(); 
		            lvl9.setPPr(ppr9); 
		                // Create object for ind
		                PPrBase.Ind pprbaseind9 = wmlObjectFactory.createPPrBaseInd(); 
		                ppr9.setInd(pprbaseind9); 
		                    pprbaseind9.setLeft( BigInteger.valueOf( 6480) ); 
		                    pprbaseind9.setHanging( BigInteger.valueOf( 360) ); 
		            // Create object for rPr
		            RPr rpr9 = wmlObjectFactory.createRPr(); 
		            lvl9.setRPr(rpr9); 
		                // Create object for rFonts
		                RFonts rfonts9 = wmlObjectFactory.createRFonts(); 
		                rpr9.setRFonts(rfonts9); 
		                    rfonts9.setAscii( "Wingdings"); 
		                    rfonts9.setHint(org.docx4j.wml.STHint.DEFAULT);
		                    rfonts9.setHAnsi( "Wingdings"); 
		            // Create object for numFmt
		            NumFmt numfmt9 = wmlObjectFactory.createNumFmt(); 
		            lvl9.setNumFmt(numfmt9); 
		                numfmt9.setVal(org.docx4j.wml.NumberFormat.BULLET);
		            // Create object for lvlText
		            Lvl.LvlText lvllvltext9 = wmlObjectFactory.createLvlLvlText(); 
		            lvl9.setLvlText(lvllvltext9); 
		                lvllvltext9.setVal( ""); 
		            // Create object for lvlJc
		            Jc jc9 = wmlObjectFactory.createJc(); 
		            lvl9.setLvlJc(jc9); 
		                jc9.setVal(org.docx4j.wml.JcEnumeration.LEFT);
		            // Create object for start
		            Lvl.Start lvlstart9 = wmlObjectFactory.createLvlStart(); 
		            lvl9.setStart(lvlstart9); 
		                lvlstart9.setVal( BigInteger.valueOf( 1) ); 
		        // Create object for nsid
		        CTLongHexNumber longhexnumber = wmlObjectFactory.createCTLongHexNumber(); 
		        numberingabstractnum.setNsid(longhexnumber); 
		            longhexnumber.setVal( "2C877BED"); 
		        // Create object for multiLevelType
		        Numbering.AbstractNum.MultiLevelType numberingabstractnummultileveltype = wmlObjectFactory.createNumberingAbstractNumMultiLevelType(); 
		        numberingabstractnum.setMultiLevelType(numberingabstractnummultileveltype); 
		            numberingabstractnummultileveltype.setVal( "multilevel"); 
		        // Create object for tmpl
		        CTLongHexNumber longhexnumber2 = wmlObjectFactory.createCTLongHexNumber(); 
		        numberingabstractnum.setTmpl(longhexnumber2); 
		            longhexnumber2.setVal( "04905A0A"); 
		        // Create object for styleLink
		        Numbering.AbstractNum.StyleLink numberingabstractnumstylelink = wmlObjectFactory.createNumberingAbstractNumStyleLink(); 
		        numberingabstractnum.setStyleLink(numberingabstractnumstylelink); 
		            numberingabstractnumstylelink.setVal( "MyListStyle"); 

		return numbering;
		}	


}

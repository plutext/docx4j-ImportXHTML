/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2024, Plutext Pty Ltd, and contributors.
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
package org.docx4j.samples;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.FormattingOption;
import org.docx4j.convert.in.xhtml.HeadingHandler;
import org.docx4j.convert.in.xhtml.ImportXHTMLProperties;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.RFonts;

/**
 * This sample converts HTML lists to pre-defined Word
 * styles.
 * 
 * For best results, be sure to include src/main/resources on your classpath. 
 *
 */
public class ConvertListUsingWordStyles {

    public static void main(String[] args) throws Exception {
        
    	// Sample input
    	String xhtml = "<ul class='Bullet0'>"+  // Bullet0 is the ID (w:styleId) of a paragraph style (w:type="paragraph") defined in the styles part of the docx
    	"  <li> Outer 1 </li>"+
    	 " <li> Outer 2 </li>"+
    	  "  <ul  class='Bullet1'>"+
    	   "   <li> Inner 1 </li>"+
    	    "  <li> Inner 2 </li>"+
    	    "</ul>"+
    	 " <li> Outer 3 </li>"+
    	"</ul>";
    	
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(new File(System.getProperty("user.dir") + "/sample-docs/docx/list styles.docx"));
		
		// Clear existing content for the purposes of this example
		wordMLPackage.getMainDocumentPart().getContent().clear();
		
		// Show available lists
		//System.out.println(wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getXML() );
		
		
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);
        
        // Important: required for this approach to work
        XHTMLImporter.setParagraphFormatting(FormattingOption.CLASS_PLUS_OTHER); // or CLASS_TO_STYLE_ONLY
		
		wordMLPackage.getMainDocumentPart().getContent().addAll( 
				XHTMLImporter.convert( xhtml, null) );
	
	System.out.println(
			XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
      
	wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/OUT_from_XHTML.docx") );
	
  }
	
}



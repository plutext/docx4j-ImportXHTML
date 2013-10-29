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
package org.docx4j.samples;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.RFonts;

/**
 * This sample converts XHTML to docx content.
 * 
 * Beware that a file created with a Microsoft text editor
 * will start with a byte order mark (BOM):
 * 
 *    http://msdn.microsoft.com/en-us/library/windows/desktop/dd374101(v=vs.85).aspx
 * 
 * and if this is converted to a String, it can result in 
 * "Content not allowed in prolog" error.
 * 
 * So it is preferable to use one of the XHTMLImporter.convert
 * signatures which doesn't use a String (eg File or InputStream).
 * 
 * Here a string may be used for convenience where the XHTML is escaped 
 * (as required for OpenDoPE input), so it can be unescaped first.
 *
 * For best results, be sure to include src/main/resources on your classpath.
 *  
 */
public class ConvertInXHTMLFile {

    public static void main(String[] args) throws Exception {
        
    	
        String inputfilepath = System.getProperty("user.dir") + "/somedir/some.html";    	
        String baseURL = "file:///C:/Users/jharrop/git/docx4j-ImportXHTML/somedir/";

        
        String stringFromFile = FileUtils.readFileToString(new File(inputfilepath), "UTF-8");
        
        String unescaped = stringFromFile;
//        if (stringFromFile.contains("&lt;/") ) {
//    		unescaped = StringEscapeUtils.unescapeHtml(stringFromFile);        	
//        }
        
		
//        XHTMLImporter.setTableFormatting(FormattingOption.IGNORE_CLASS);
//        XHTMLImporter.setParagraphFormatting(FormattingOption.IGNORE_CLASS);
        
		System.out.println("Unescaped: " + unescaped);
        
                
        // Setup font mapping
		RFonts rfonts = Context.getWmlObjectFactory().createRFonts();
		rfonts.setAscii("Century Gothic");
        XHTMLImporterImpl.addFontMapping("Century Gothic", rfonts);
        
        // Create an empty docx package
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

		
		NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
		wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
		ndp.unmarshalDefaultNumbering();		
					
		// Convert the XHTML, and add it into the empty docx we made
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);
        XHTMLImporter.setHyperlinkStyle("Hyperlink");
		wordMLPackage.getMainDocumentPart().getContent().addAll( 
				XHTMLImporter.convert(unescaped, baseURL) );
		
		System.out.println(
				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));

//		System.out.println(
//				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart().getJaxbElement(), true, true));
		
		wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/OUT_from_XHTML.docx") );
      
  }
	
}

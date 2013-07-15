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

import java.net.URL;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;

/**
 * This sample retrieves an XHTML Web page URL and converts it to docx content.
 * 
 * If your page is not well-formed XML, you will need to tidy it first.
 * 
 * For best results, be sure to include src/main/resources on your classpath.
 *  
 */
public class ConvertInXHTMLURL {

    public static void main(String[] args) throws Exception {
        
    	// Must tidy first :-(
        //URL url = new URL("http://stackoverflow.com/questions/10887580/how-to-convert-a-webpage-from-an-intranet-wiki-to-an-office-document");
    	
    	
        //URL url = new URL("http://en.wikipedia.org/wiki/Office_Open_XML");
    	//URL url = new URL("http://en.wikipedia.org/w/index.php?title=Office_Open_XML&printable=yes");
    	URL url = new URL("http://en.wikipedia.org/w/index.php?title=Microsoft_Word&printable=yes");
                        
        XHTMLImporter.setHyperlinkStyle("Hyperlink");
        
        // Create an empty docx package
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
		
		NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
		wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
		ndp.unmarshalDefaultNumbering();		
					
		// Convert the XHTML, and add it into the empty docx we made
		wordMLPackage.getMainDocumentPart().getContent().addAll( 
				XHTMLImporter.convert(url , wordMLPackage) );
		
		System.out.println(
				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
		
		wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/OUT_ConvertInXHTMLURL.docx") );
      
  }
	
}

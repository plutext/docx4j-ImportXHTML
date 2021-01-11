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
import java.io.OutputStream;

import org.docx4j.Docx4jProperties;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.convert.out.html.AbstractHtmlExporter;
import org.docx4j.convert.out.html.AbstractHtmlExporter.HtmlSettings;
import org.docx4j.convert.out.html.HtmlExporterNG2;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;

/**
 * This sample converts an XHTML document to docx.
 *
 * For best results, be sure to include src/main/resources on your classpath.
 *  
 */
public class ConvertInXHTMLDocument {

    public static void main(String[] args) throws Exception {

    	Docx4jProperties.setProperty("docx4j.Convert.Out.HTML.OutputMethodXML", true);
    	
    	// The input would generally be an XHTML document,
    	// but for convenience, this sample can convert a 
    	// docx to XHTML first (ie round trip).
        String inputfilepath = System.getProperty("user.dir") + "/sample-docs/docx/sample-docxv2.docx";

        
        // Create an empty docx package
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
		
		NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
		wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
		ndp.unmarshalDefaultNumbering();		

        XHTMLImporterImpl xHTMLImporter = new XHTMLImporterImpl(wordMLPackage);
        xHTMLImporter.setHyperlinkStyle("Hyperlink");
		
		if (inputfilepath.endsWith("html")) {
			
			// Convert the XHTML, and add it into the empty docx we made
			wordMLPackage.getMainDocumentPart().getContent().addAll( 
					xHTMLImporter.convert(new File(inputfilepath), null) );
			
		} else if (inputfilepath.endsWith("docx")) {
			//Round trip docx -> XHTML -> docx
			WordprocessingMLPackage docx = WordprocessingMLPackage.load( new File(inputfilepath));	    	
			AbstractHtmlExporter exporter = new HtmlExporterNG2();
			
			
			// Use file system, so there is somewhere to save images (if any)
			OutputStream os = new java.io.FileOutputStream(inputfilepath + ".html");	
			
	    	HtmlSettings htmlSettings = new HtmlSettings();
	    	htmlSettings.setImageDirPath(inputfilepath + "_files"); 
	    	htmlSettings.setImageTargetUri(inputfilepath.substring(inputfilepath.lastIndexOf("/")+1) 
	    			  + "_files");
			
			javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(os);
			exporter.html(docx, result, htmlSettings );			
			
			// Now after all that, we have XHTML we can convert 
			wordMLPackage.getMainDocumentPart().getContent().addAll( 
					xHTMLImporter.convert( new File(inputfilepath + ".html"), null) );
		} else {
			return;
		}
		
		System.out.println(
				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
		
		wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/html_output.docx") );
      
  }
	
}

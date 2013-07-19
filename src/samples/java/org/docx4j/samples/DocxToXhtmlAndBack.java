/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.

    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.docx4j.samples;

import java.io.File;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.convert.out.html.AbstractHtmlExporter;
import org.docx4j.convert.out.html.AbstractHtmlExporter.HtmlSettings;
import org.docx4j.convert.out.html.HtmlExporterNG2;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;

/**
 * docx to xhtml to docx again.
 * 
 * Useful for testing support for specific features.
 *
 */
public class DocxToXhtmlAndBack extends AbstractSample {

	// Config for non-command line version
	static {
	
//    	inputfilepath = System.getProperty("user.dir") + "/sample-docs/word/sample-docxv2.docx";
    	inputfilepath = System.getProperty("user.dir") + "/sample-docs/docx/tables.docx";
		
	}

    public static void main(String[] args)
            throws Exception {

		try {
			getInputFilePath(args);
		} catch (IllegalArgumentException e) {
		}

		System.out.println(inputfilepath);
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(new java.io.File(inputfilepath));

		// XHTML export
		AbstractHtmlExporter exporter = new HtmlExporterNG2();
    	HtmlSettings htmlSettings = new HtmlSettings();

    	htmlSettings.setImageDirPath(inputfilepath + "_files");
    	htmlSettings.setImageTargetUri(inputfilepath.substring(inputfilepath.lastIndexOf("/")+1)
    			+ "_files");
    	
    	String htmlFilePath = System.getProperty("user.dir") + "/DocxToXhtmlAndBack.html";
		OutputStream os = new java.io.FileOutputStream(htmlFilePath);

		javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(os);
		exporter.html(wordMLPackage, result, htmlSettings);
		os.flush();
		os.close();

		// XHTML to docx
        String stringFromFile = FileUtils.readFileToString(new File(htmlFilePath), "UTF-8");
		
        XHTMLImporter.setHyperlinkStyle("Hyperlink");
        
		WordprocessingMLPackage docxOut = WordprocessingMLPackage.createPackage();
		NumberingDefinitionsPart ndp = new NumberingDefinitionsPart();
		docxOut.getMainDocumentPart().addTargetPart(ndp);
		ndp.unmarshalDefaultNumbering();		
					
		docxOut.getMainDocumentPart().getContent().addAll( 
				XHTMLImporter.convert(stringFromFile, null, docxOut) );
				
		docxOut.save(new java.io.File(System.getProperty("user.dir") + "/DocxToXhtmlAndBack.docx") );

    }
}
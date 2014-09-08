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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.docx4j.Docx4J;
import org.docx4j.Docx4jProperties;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.DivToSdt;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.convert.out.html.SdtTagHandler;
import org.docx4j.convert.out.html.SdtWriter;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.SdtPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

/**
 * Converts divs to content controls, and back again.
 */
public class DivRoundtrip {
	
	private static Logger log = LoggerFactory.getLogger(DivRoundtrip.class);		
	

    public static void main(String[] args) throws Exception {
        

    	String xhtml= 
    	"<div id=\"top\">" +
			"<h1>Heading</h1>" +
			"<div class=\"inner\">" +
				"<p>p1</p>" +
				"<p>p2</p>" +
			  "</div>"+
			"<div id=\"transient-container\" class=\"IGNORE\">" +
				"<p>p1</p>" +
				"<p>p2</p>" +
		  "</div>"+
		"</div>";    	
    	
    	// To docx, with content controls
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
		
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);
        XHTMLImporter.setDivHandler(new DivToSdt());
		
		wordMLPackage.getMainDocumentPart().getContent().addAll( 
				XHTMLImporter.convert( xhtml, null) );

		System.out.println(XmlUtils.marshaltoString(wordMLPackage
				.getMainDocumentPart().getJaxbElement(), true, true));

		wordMLPackage.save(new java.io.File(System.getProperty("user.dir")
				+ "/OUT_from_XHTML.docx"));

		// Back to XHTML

		HTMLSettings htmlSettings = Docx4J.createHTMLSettings();
		htmlSettings.setWmlPackage(wordMLPackage);

		// Sample sdt tag handler (tag handlers insert specific
		// html depending on the contents of an sdt's tag).
		// This will only have an effect if the sdt tag contains
		// the string class=
		SdtWriter.registerTagHandler("*", new MyTagClass());	
	
		// output to an OutputStream.
		OutputStream os = new ByteArrayOutputStream();

		// If you want XHTML output
		Docx4jProperties.setProperty("docx4j.Convert.Out.HTML.OutputMethodXML",
				true);
		Docx4J.toHTML(htmlSettings, os, Docx4J.FLAG_EXPORT_PREFER_XSL);

		System.out.println(((ByteArrayOutputStream) os).toString());	
	
  }
    
    
    static class MyTagClass extends SdtTagHandler {   
    	
    	/**
    	 * Modify this method to alter the attributes set on the div etc
    	 */
    	private Element createDiv(Document document, DocumentFragment docfrag, 
    			SdtPr sdtPr,
    			HashMap<String, String> tagMap) throws ParserConfigurationException, IOException, SAXException {

    		Element xhtmlDiv = document.createElement("div");
    		docfrag.appendChild(xhtmlDiv);						
    		
    		// Handle @class
    		String classVal = tagMap.get("class");
    		if (classVal!=null) {
    			xhtmlDiv.setAttribute("class", classVal);
    		}

    		// Handle @class
    		String idVal = tagMap.get("id");
    		if (idVal!=null) {
    			xhtmlDiv.setAttribute("id", idVal);
    		}
    		
    		return xhtmlDiv;
    	}
    	
    	@Override
    	public Node toNode(WordprocessingMLPackage wmlPackage, SdtPr sdtPr,
    			HashMap<String, String> tagMap,
    			NodeIterator childResults) throws TransformerException {

    		try {
    			// Create a DOM builder and parse the fragment
    			Document document = XmlUtils.getNewDocumentBuilder().newDocument();
    			DocumentFragment docfrag = document.createDocumentFragment();

				if (tagMap.get("class") != null
						&& "IGNORE".endsWith(tagMap.get("class"))) {

    				// don't add a div
					return attachContents(docfrag, docfrag, childResults);
					
				} else if (tagMap.get("id") == null
    					&& tagMap.get("class")==null) {

    				// don't add a div
					return attachContents(docfrag, docfrag, childResults);
    				
    			} else {
    			
    				// create a div
	    			Element xhtmlDiv = this.createDiv(document, docfrag, sdtPr, tagMap);
	    			return attachContents(docfrag, xhtmlDiv, childResults);
    			}
    			
    		} catch (Exception e) {
    			log.error(e.getMessage(), e);
    			throw new TransformerException(e);
    		}

    	}
    	

		@Override
		public Node toNode(WordprocessingMLPackage wmlPackage, SdtPr sdtPr,
				HashMap<String, String> tagMap,
				Node resultSoFar) throws TransformerException {
			try {
				// Create a DOM builder and parse the fragment
				Document document = XmlUtils.getNewDocumentBuilder().newDocument();
				DocumentFragment docfrag = document.createDocumentFragment();

				if (tagMap.get("class") != null
						&& "IGNORE".endsWith(tagMap.get("class"))) {

    				// don't add a div
					return attachContents(docfrag, docfrag, resultSoFar);
					
				} else if (tagMap.get("id") == null
    					&& tagMap.get("class")==null) {

    				// don't add a div
					return attachContents(docfrag, docfrag, resultSoFar);
    				
    			} else {
    			
    				// create a div
	    			Element xhtmlDiv = this.createDiv(document, docfrag, sdtPr, tagMap);
	    			return attachContents(docfrag, xhtmlDiv, resultSoFar);
    			}
				
				
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new TransformerException(e);
			}
		}
    	
    	    	
    }
	
}



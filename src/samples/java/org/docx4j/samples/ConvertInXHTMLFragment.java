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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 * This sample converts a fragment of XHTML to docx.  The fragment should
 * be one or more block level objects.
 * 
 * For best results, be sure to include src/main/resources on your classpath. 
 *
 */
public class ConvertInXHTMLFragment {

    public static void main(String[] args) throws Exception {
        
//    	String xhtml= "<div>" +
//		    			"<h1>Heading</h1>" +
//		    			"<table style='border:solid 1px white;'><tr><th>1</th></tr></table>" +
//		    		  "</div>";    	

//    	String xhtml = "<div><p>Hello here we <span style='background-color:red;'> were </span> and are now </p></div>";
    	
//    	String xhtml = "<table><tr><td>1</td></tr></table>";
    	
    	String xhtml = "<ul>"+
    	"  <li> Outer 1 </li>"+
    	 " <li> Outer 2 </li>"+
    	  "  <ul>"+
    	   "   <li> Inner 1 </li>"+
    	    "  <li> Inner 2 </li>"+
    	    "</ul>"+
    	 " <li> Outer 3 </li>"+
    	"</ul>";
    	
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
//		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(new File(System.getProperty("user.dir") + "/Hello.docx"));

//		// Setup white list
//		Set<String> cssWhiteList = new HashSet<String>();
//		List lines = FileUtils.readLines(new File(System.getProperty("user.dir") + "/src/main/resources/CSS-WhiteList.txt"));
//		// TODO catch exception
//		for (Object o : lines) {
//			String line = ((String)o).trim();
//			if (line.length()>0 && !line.startsWith("#")) {
//				cssWhiteList.add(line);
//			}
//		}
//		XHTMLImporter.setCssWhiteList(cssWhiteList);
		
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);
		
		wordMLPackage.getMainDocumentPart().getContent().addAll( 
				XHTMLImporter.convert( xhtml, null) );
	
	System.out.println(
			XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
      
	wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/OUT_from_XHTML.docx") );
	
  }
	
}



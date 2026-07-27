/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2018-2022, Plutext Pty Ltd, and contributors.
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

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPartAltChunkHost;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;

/**
 * Open a docx containing an XHTML or .mht AltChunk,
 * and then convert that to normal docx content.
 * @author jharrop
 *
 */
public class ConvertAltChunks {

	public static void main(String[] args)  throws Exception {

		WordprocessingMLPackage wordMLPackage = Docx4J.load(new File(System.getProperty("user.dir") + "your.docx"));
				
				
		// MDP
		wordMLPackage.getMainDocumentPart().convertAltChunks();
		
		// Headers/footers
	    for (Relationship rel : wordMLPackage.getMainDocumentPart().getRelationshipsPart().getRelationships().getRelationship() ) {
	    	
	    	if (rel.getType().equals(Namespaces.HEADER)
	    			|| rel.getType().equals(Namespaces.FOOTER)) {
	    		JaxbXmlPartAltChunkHost part = (JaxbXmlPartAltChunkHost)wordMLPackage.getMainDocumentPart().getRelationshipsPart().getPart(rel);
	    		System.out.println("processing " + part.getPartName().getName());
	    		part.convertAltChunks();
	    	}
	    }		
		// Display result
//		System.out.println(
//				XmlUtils.marshaltoString(pkgOut.getMainDocumentPart().getJaxbElement(), true, true));
		
	    wordMLPackage.save(new File(System.getProperty("user.dir") + "/OUT_ConvertAltChunks_all.docx"));
		
		
	}

}

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

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

/**
 * Create a docx containing an XHTML AltChunk,
 * and then convert that to normal docx content.
 * @author jharrop
 *
 */
public class AltChunkXHTMLRoundTrip {

	/**
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {

		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
		MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();
		
		mdp.addParagraphOfText("Paragraph 1");

		// Add the XHTML altChunk
		String xhtml = "<html><head><title>Import me</title></head><body><p>Hello World!</p></body></html>"; 
		mdp.addAltChunk(AltChunkType.Xhtml, xhtml.getBytes());
		
		mdp.addParagraphOfText("Paragraph 3");
		
		// Round trip
		mdp.convertAltChunks();
		
		// Display result
		System.out.println(
				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true));
		
		wordMLPackage.save(new java.io.File(System.getProperty("user.dir") + "/OUT_AltChunkXHTMLRoundTrip.docx"));
		
		
	}

}

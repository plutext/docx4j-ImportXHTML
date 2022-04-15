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

import java.util.List;

import org.docx4j.Docx4J;
import org.docx4j.UnitsOfMeasurement;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImageAddTestGif{

	// 2x2 pixels
    private final String GIF_IMAGE_DATA = "data:image/gif;base64,R0lGODdhAgACAKEEAAMA//8AAAD/Bv/8ACwAAAAAAgACAAACAww0BQA7";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception  {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	@Test
	public void testSizeUnspecified() throws Exception {
		
		Inline inline1 = getInline("<div><img src='" + GIF_IMAGE_DATA + "'/></div>");
		
		// DPI is configurable since docx4j 3.3.7
		if (UnitsOfMeasurement.DPI==96) {
			Assert.assertTrue(inline1.getExtent().getCx() == 19050); 
		} else if (UnitsOfMeasurement.DPI==72) {
			Assert.assertTrue(inline1.getExtent().getCx() == 25400); 
		} else {
			System.out.println("Skipping test for DPI " + UnitsOfMeasurement.DPI);
		}
		
		Docx4J.save(wordMLPackage, new java.io.File(System.getProperty("user.dir") + "/OUT_from_XHTML.docx") );
	}

//	@Test
//	public void testSize20px() throws Exception {
//		
//		Inline inline1 = getInline("<div><img src='" + PNG_IMAGE_DATA + "' width='20px' height='20px' /></div>");
//		Assert.assertTrue(inline1.getExtent().getCx() == 338667);
//	}
//
//	@Test
//	public void testSize20NoUnits() throws Exception {
////		// values in dots are 20x as expected		
//		Inline inline1 = getInline("<div><img src='" + PNG_IMAGE_DATA + "' width='20' height='20' /></div>");
//		Assert.assertTrue(inline1.getExtent().getCx() == 338667);
//	}
//	
//	@Test
//	public void testSize20pt() throws Exception {
//		
//		Inline inline1 = getInline("<div><img src='" + PNG_IMAGE_DATA + "' width='20pt' height='20pt' /></div>");
//		Assert.assertTrue(inline1.getExtent().getCx() == 451273);
//	}
//	
//	@Test
//	public void testSizeNoHeight() throws Exception {
//		
//		Inline inline = getInline("<div><img src='" + PNG_IMAGE_DATA + "' width='20pt'  /></div>");
//		Assert.assertTrue(  Math.round(inline.getExtent().getCx()/10) 
//				== Math.round(inline.getExtent().getCy()/10)); // +/- a few EMU
//	}
//	
//	@Test
//	public void testSizeNoWidth() throws Exception {
//		
//		Inline inline = getInline("<div><img src='" + PNG_IMAGE_DATA + "' height='20pt'  /></div>");
//		Assert.assertTrue(  Math.round(inline.getExtent().getCx()/10) 
//				== Math.round(inline.getExtent().getCy()/10)); // +/- a few EMU
//	}
//	
//	
//	
//	@Test
//	public void testSizeSpecifiedPxPlusCSS() throws Exception {
//		
//		// box.getHeight() and  box.getWidth() include padding
//		
//		Inline inline2 = getInline("<div><img style='padding-top:10px;padding-left:10px;' src='" + PNG_IMAGE_DATA + "' width='20px' height='10px' /></div>");
//		Assert.assertTrue(inline2.getExtent().getCx() == 338667);		
//		//Assert.assertTrue(inline2.getExtent().getCx() / inline2.getExtent().getCy() == 2);
//	}
	
	private Inline getInline(String html) throws Exception {
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		List<Object> convert = XHTMLImporter.convert(html, null);
		
		wordMLPackage.getMainDocumentPart().getContent().addAll( convert );
		
		return ((Inline)((Drawing)((R)((P)convert.get(0)).getContent().get(0)).getContent().get(0)).getAnchorOrInline().get(0));
	}
}
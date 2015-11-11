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

import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class ImageResizeTest{

	// 2x2 pixels
    private final String GIF_IMAGE_DATA = "data:image/gif;base64,R0lGODdhAgACAKEEAAMA//8AAAD/Bv/8ACwAAAAAAgACAAACAww0BQA7";
    private final String PNG_IMAGE_DATA = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception  {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	@Test
	public void testFixedSizeImage() throws Exception {
		Inline inline1 = getInline("<div><img src='" + PNG_IMAGE_DATA + "'/></div>");
		Inline inline2 = getInline("<div><img src='" + PNG_IMAGE_DATA + "' width='40px' height='20px' /></div>");
      Assert.assertTrue(inline2.getExtent().getCx() / inline1.getExtent().getCx() == 20);
      Assert.assertTrue(inline2.getExtent().getCy() / inline1.getExtent().getCy() == 10);
	}

	@Test
	public void testCmAgainstPx() throws Exception {
		Inline inline1 = getInline("<div><img src='" + PNG_IMAGE_DATA + "' height='20px' width='40px'/></div>");
		Inline inline2 = getInline("<div><img src='" + PNG_IMAGE_DATA + "' height='20cm' width='40cm'/></div>");
		Assert.assertTrue(inline2.getExtent().getCx() / inline1.getExtent().getCx() > 10);
		Assert.assertTrue(inline2.getExtent().getCx() / inline1.getExtent().getCy() > 10);
	}

	@Test
	public void testScaling() throws Exception {
		Inline inline = getInline("<div><img src='" + PNG_IMAGE_DATA + "' style='width: 50px'/></div>");
		Assert.assertTrue(  Math.round(inline.getExtent().getCx()/10) 
				== Math.round(inline.getExtent().getCy()/10)); // +/- a few EMU
	}

	public void testHeightWidthInPx() throws Exception {
		
		String PNG_IMAGE_DATA = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";		
		String html= "<div>" +
					"<p><img src='" + PNG_IMAGE_DATA + "' width='40px' height='20px' /></p>" +
					"<p><img src='" + PNG_IMAGE_DATA + "' style='width:40px; height:20px' /></p>" +
				"</div>";
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		List<Object> convert = XHTMLImporter.convert(html, null);
		wordMLPackage.getMainDocumentPart().getContent().addAll(convert);
		wordMLPackage.save(new File(System.getProperty("user.dir") + "/px.docx") );
	}
	
	private Inline getInline(String html) throws Exception{
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		List<Object> convert = XHTMLImporter.convert(html, null);
		return ((Inline)((Drawing)((R)((P)convert.get(0)).getContent().get(0)).getContent().get(0)).getAnchorOrInline().get(0));
	}
}
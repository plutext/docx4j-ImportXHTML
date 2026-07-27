/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2013-2022, Plutext Pty Ltd, and contributors.
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

import org.docx4j.convert.in.xhtml.renderer.Docx4jUserAgent;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.w3c.dom.Element;

public interface XHTMLImageHandler {
	
	/**
	 * @param docx4jUserAgent
	 * @param wordMLPackage
	 * @param p
	 * @param e
	 * @param cx  width of image itself (ie excluding CSS margin, padding) in EMU 
	 * @param cy
	 */
	public void addImage(Docx4jUserAgent docx4jUserAgent, WordprocessingMLPackage wordMLPackage, P p, Element e, Long cx, Long cy);
	
	
	/*
	 * That sets text wrapping distance from text to 0.
	 * 
	 *   <wp:anchor distT="457200" distB="118745" distL="457200"  distR="0"
	 * 
	 * would set distance from text top 0.5", bottom 0.13", left 0.5", right 0.
	 * 
	 * so we could have another method
	 * 
	 *   addImage(Docx4jUserAgent docx4jUserAgent, WordprocessingMLPackage wordMLPackage, P p, Element e, Long cx, Long cy,
	 *       Long distT, Long distB, Long distL, Long distR);
	 * 
	 */
	
	/**
	 * Set the maximum width available (in twips); useful for scaling bare images
	 * if they are to go in a table cell.
	 * <br>Also set table style if images are really to go in a table cell 
     * (needed to remove table style margins from final width).
     * @param maxWidth
     * @param tableStyle - can be null
     */
	public void setMaxWidth(int maxWidth, String tableStyle);
}

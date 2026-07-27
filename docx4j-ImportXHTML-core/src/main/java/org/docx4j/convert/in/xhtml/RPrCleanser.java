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

import org.docx4j.model.styles.StyleUtil;
import org.docx4j.wml.RPr;

public class RPrCleanser {
	
	public static void removeRedundantProperties(RPr pLevelRPr, RPr runPr) {
		
		// Bold
		if ( StyleUtil.areEqual(pLevelRPr.getB(), runPr.getB())) {
			runPr.setB(null);
		}

		// Italics
		if ( StyleUtil.areEqual(pLevelRPr.getI(), runPr.getI())) {
			runPr.setI(null);
		}

		// Font color
		if ( StyleUtil.areEqual(pLevelRPr.getColor(), runPr.getColor())) {
			runPr.setColor(null);
		}
		// Font size
		if ( StyleUtil.areEqual(pLevelRPr.getSz(), runPr.getSz())) {
			runPr.setSz(null);
		}
		
		// Font
		if ( StyleUtil.areEqual(pLevelRPr.getRFonts(), runPr.getRFonts())) {
			runPr.setRFonts(null);
		}
		
		// Highlight color
		if ( StyleUtil.areEqual(pLevelRPr.getHighlight(), runPr.getHighlight())) {
			runPr.setHighlight(null);
		}
		
		// Border
		if ( StyleUtil.areEqual(pLevelRPr.getBdr(), runPr.getBdr())) {
			runPr.setBdr(null);
		}
		
		// Shading
		if ( StyleUtil.areEqual(pLevelRPr.getShd(), runPr.getShd())) {
			runPr.setShd(null);
		}
		
		// Strike
		if ( StyleUtil.areEqual(pLevelRPr.getStrike(), runPr.getStrike())) {
			runPr.setStrike(null);
		}
		
		// Text direction
		if ( StyleUtil.areEqual(pLevelRPr.getRtl(), runPr.getRtl())) {
			runPr.setRtl(null);
		}
		
		// Underline
		if ( StyleUtil.areEqual(pLevelRPr.getU(), runPr.getU())) {
			runPr.setU(null);
		}
		
		// Vertical alignment
		if ( StyleUtil.areEqual(pLevelRPr.getVertAlign(), runPr.getVertAlign())) {
			runPr.setVertAlign(null);
		}
		
	}
	

}

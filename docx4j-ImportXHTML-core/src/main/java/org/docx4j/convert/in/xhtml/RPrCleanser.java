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

package org.docx4j.convert.in.xhtml;

import java.math.BigInteger;

import org.docx4j.jaxb.Context;
import org.docx4j.model.styles.StyleUtil;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.Jc;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.Ind;
import org.docx4j.wml.PPrBase.PBdr;
import org.docx4j.wml.PPrBase.Spacing;
import org.docx4j.wml.STBorder;

public class PPrCleanser {
	
	public static void removeRedundantProperties(PPr stylePPr, PPr directPr) {
		
		// Indent
		Ind stylePPrInd = stylePPr.getInd();
		if (stylePPrInd==null) {
			stylePPrInd = Context.getWmlObjectFactory().createPPrBaseInd();
			stylePPrInd.setLeft(BigInteger.ZERO);
			stylePPrInd.setRight(BigInteger.ZERO);
			stylePPrInd.setFirstLine(BigInteger.ZERO);
			stylePPrInd.setHanging(BigInteger.ZERO);
		}
		if (directPr.getInd()!=null) {
			if ( StyleUtil.areEqual(stylePPrInd.getLeft(), directPr.getInd().getLeft())) {
				directPr.getInd().setLeft(null);
			}
			if ( StyleUtil.areEqual(stylePPrInd.getRight(), directPr.getInd().getRight())) {
				directPr.getInd().setRight(null);
			}
			if ( StyleUtil.areEqual(stylePPrInd.getFirstLine(), directPr.getInd().getFirstLine())) {
				directPr.getInd().setFirstLine(null);
			}
			if ( StyleUtil.areEqual(stylePPrInd.getHanging(), directPr.getInd().getHanging())) {
				directPr.getInd().setHanging(null);
			}
		}
		if (directPr.getInd().getLeft()==null
				&& directPr.getInd().getFirstLine()==null 
				&& directPr.getInd().getHanging()==null 
				&& directPr.getInd().getRight()==null ) {
			directPr.setInd(null);
		}
		
		// Justification
		Jc stylePPrJc = stylePPr.getJc();
		if (stylePPrJc==null) {
			stylePPrJc = Context.getWmlObjectFactory().createJc();
			stylePPrJc.setVal(JcEnumeration.LEFT);
		}
		if ( StyleUtil.areEqual(stylePPrJc, directPr.getJc())) {
			directPr.setJc(null);
		}
		
		// KeepNext
		if ( StyleUtil.areEqual(stylePPr.getKeepNext(), directPr.getKeepNext())) {
			directPr.setKeepNext(null);
		}
		
		
		// NumberingProp
		if ( StyleUtil.areEqual(stylePPr.getNumPr(), directPr.getNumPr())) {
			directPr.setNumPr(null);
		}
		
		// PageBreakBefore
		if ( StyleUtil.areEqual(stylePPr.getPageBreakBefore(), directPr.getPageBreakBefore())) {
			directPr.setPageBreakBefore(null);
		}
		
		// Border
		PBdr stylePPrBdr = stylePPr.getPBdr();
		if (stylePPrBdr==null) {
			// Default values. TODO: better way?
			stylePPrBdr = Context.getWmlObjectFactory().createPPrBasePBdr();
			CTBorder borderNone = Context.getWmlObjectFactory().createCTBorder();
			borderNone.setVal(STBorder.NONE);
			
			stylePPrBdr.setLeft(borderNone);
			stylePPrBdr.setRight(borderNone);
			stylePPrBdr.setTop(borderNone);
			stylePPrBdr.setBottom(borderNone);
		}
		if (directPr.getPBdr()!=null) {
			if ( StyleUtil.areEqual(stylePPrBdr.getLeft(), directPr.getPBdr().getLeft())) {
				directPr.getPBdr().setLeft(null);
			}
			if ( StyleUtil.areEqual(stylePPrBdr.getRight(), directPr.getPBdr().getRight())) {
				directPr.getPBdr().setRight(null);
			}
			if ( StyleUtil.areEqual(stylePPrBdr.getTop(), directPr.getPBdr().getTop())) {
				directPr.getPBdr().setTop(null);
			}
			if ( StyleUtil.areEqual(stylePPrBdr.getBottom(), directPr.getPBdr().getBottom())) {
				directPr.getPBdr().setBottom(null);
			}
		}
		
		
		// Shading
		if ( StyleUtil.areEqual(stylePPr.getShd(), directPr.getShd())) {
			directPr.setShd(null);
		}
		
		// Spacing
		Spacing stylePPrSpacing = stylePPr.getSpacing();
		if (stylePPrSpacing==null) {
			// Default values. TODO: better way?
			stylePPrSpacing = Context.getWmlObjectFactory().createPPrBaseSpacing();
			stylePPrSpacing.setBefore(BigInteger.ZERO);
			stylePPrSpacing.setAfter(BigInteger.ZERO);
			stylePPrSpacing.setLine(BigInteger.ONE); //?
		}
		
		if (directPr.getSpacing()!=null) {
			if ( StyleUtil.areEqual(stylePPrSpacing.getBefore(), directPr.getSpacing().getBefore())) {
				directPr.getSpacing().setBefore(null);
			}
			if ( StyleUtil.areEqual(stylePPrSpacing.getAfter(), directPr.getSpacing().getAfter())) {
				directPr.getSpacing().setAfter(null);
			}
			if ( StyleUtil.areEqual(stylePPrSpacing.getLine(), directPr.getSpacing().getLine())) {
				directPr.getSpacing().setLine(null);
			}
		}
		// don't want <w:spacing/>
		Spacing directPrSpacing = directPr.getSpacing();
		if (directPrSpacing.getAfter()==null
				&& directPrSpacing.getAfterLines()==null
				&& directPrSpacing.getBefore()==null
				&& directPrSpacing.getBeforeLines()==null
				&& directPrSpacing.getLine()==null
				// && directPrSpacing.getLineRule() 
				) {
			directPr.setSpacing(null);
		}
		
		
		// Text Alignment Vertical
		if ( StyleUtil.areEqual(stylePPr.getTextAlignment(), directPr.getTextAlignment())) {
			directPr.setTextAlignment(null);
		}
		

		
	}
	

}

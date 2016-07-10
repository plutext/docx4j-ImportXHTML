package org.docx4j.convert.in.xhtml;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.org.xhtmlrenderer.docx.Docx4jUserAgent;
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

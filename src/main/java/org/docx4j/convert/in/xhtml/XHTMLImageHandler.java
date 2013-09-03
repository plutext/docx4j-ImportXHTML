package org.docx4j.convert.in.xhtml;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.org.xhtmlrenderer.docx.Docx4jUserAgent;
import org.docx4j.wml.P;
import org.w3c.dom.Element;

public interface XHTMLImageHandler {
	
	public void addImage(Docx4jUserAgent docx4jUserAgent, WordprocessingMLPackage wordMLPackage, P p, Element e, Long cx, Long cy);

}

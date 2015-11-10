package org.docx4j.convert.in.xhtml;

import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.xhtmlrenderer.docx.Docx4JFSImage;
import org.xhtmlrenderer.docx.Docx4jUserAgent;
import org.docx4j.wml.P;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class XHTMLImageHandlerDefault implements XHTMLImageHandler {
	
	public static Logger log = LoggerFactory.getLogger(XHTMLImageHandlerDefault.class);		
	
    protected HashMap<String, BinaryPartAbstractImage> imagePartCache = new HashMap<String, BinaryPartAbstractImage>(); 
	
	/**
	 * @param docx4jUserAgent
	 * @param wordMLPackage
	 * @param p
	 * @param e
	 * @param cx  width of image itself (ie excluding CSS margin, padding) in EMU 
	 * @param cy
	 */    
	public void addImage(Docx4jUserAgent docx4jUserAgent, WordprocessingMLPackage wordMLPackage, P p, Element e, Long cx, Long cy) {

		BinaryPartAbstractImage imagePart = null;
		
		boolean isError = false;
		try {
			byte[] imageBytes = null;

			if (e.getAttribute("src").startsWith("data:image")) {
				// Supports 
				//   data:[<MIME-type>][;charset=<encoding>][;base64],<data>
				// eg data:image/png;base64,iVBORw0KGgo...
				// http://www.greywyvern.com/code/php/binary2base64 is a convenient online encoder
				String base64String = e.getAttribute("src");
				int commaPos = base64String.indexOf(",");
				if (commaPos < 6) { // or so ...
					// .. its broken
					org.docx4j.wml.R run = Context.getWmlObjectFactory().createR();
					p.getContent().add(run);

					org.docx4j.wml.Text text = Context.getWmlObjectFactory().createText();
					text.setValue("[INVALID DATA URI: " + e.getAttribute("src"));

					run.getContent().add(text);

					return;
				}
				base64String = base64String.substring(commaPos + 1);
				log.debug(base64String);
				imageBytes = Base64.decodeBase64(base64String.getBytes("UTF8"));
			} else {
				
				imagePart = imagePartCache.get(e.getAttribute("src"));
				
				if (imagePart==null) {
					
					String url = e.getAttribute("src");
					// Workaround for cannot resolve the URL C:\... with base URL file:/C:/...
					// where @src points to a raw file path
					if (url.substring(1,2).equals(":")) {
						url = "file:/" + url;
					}
					
					Docx4JFSImage docx4JFSImage = docx4jUserAgent.getDocx4JImageResource(url);
					if (docx4JFSImage != null) {// in case of wrong URL - docx4JFSImage will be null
						imageBytes = docx4JFSImage.getBytes();
					}
				}
			}
			if (imageBytes == null
					&& imagePart==null) {
				isError = true;
			} else {
				
				if (imagePart==null) {
					// Its not cached
					imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, imageBytes);
					if (e.getAttribute("src").startsWith("data:image")) {
						// don't bother caching
					} else {
						// cache it
						imagePartCache.put(e.getAttribute("src"), imagePart);
					}
				}


				
				Inline inline;
				if (cx == null && cy == null) {
					inline = imagePart.createImageInline(null, e.getAttribute("alt"), 0, 1, false);
				} else {
					
					if (cx == null) {
						
						cx = imagePart.getImageInfo().getSize().getWidthPx() *
								(cy / imagePart.getImageInfo().getSize().getHeightPx());
						
					} else if (cy == null) {
						
						cy = imagePart.getImageInfo().getSize().getHeightPx() *
								(cx / imagePart.getImageInfo().getSize().getWidthPx());
						
					}
					inline = imagePart.createImageInline(null, e.getAttribute("alt"), 0, 1, cx, cy, false);
					
					/*
					 * That sets text wrapping distance from text to 0.
					 * 
					 *   <wp:anchor distT="457200" distB="118745" distL="457200"  distR="0"
					 * 
					 * would set distance from text top 0.5", bottom 0.13", left 0.5", right 0.
					 * 
					 */
				}

				// Now add the inline in w:p/w:r/w:drawing
				org.docx4j.wml.R run = Context.getWmlObjectFactory().createR();
				p.getContent().add(run);
				org.docx4j.wml.Drawing drawing = Context.getWmlObjectFactory().createDrawing();
				run.getContent().add(drawing);
				drawing.getAnchorOrInline().add(inline);
			}
		} catch (Exception e1) {
			log.error(MessageFormat.format("Error during image processing: ''{0}'', insert default text.", new Object[] {e.getAttribute("alt")}), e1);
			isError = true;
		}

		if (isError) {
			org.docx4j.wml.R run = Context.getWmlObjectFactory().createR();
			p.getContent().add(run);

			org.docx4j.wml.Text text = Context.getWmlObjectFactory().createText();
			text.setValue("[MISSING IMAGE: " + e.getAttribute("alt") + ", " + e.getAttribute("alt") + " ]");

			run.getContent().add(text);
		}
		
	}
	

}

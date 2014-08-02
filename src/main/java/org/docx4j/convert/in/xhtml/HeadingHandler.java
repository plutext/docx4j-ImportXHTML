package org.docx4j.convert.in.xhtml;

import java.util.HashMap;
import java.util.Map;

import org.docx4j.wml.Style;
import org.docx4j.wml.Styles;

public class HeadingHandler {
	
	
	/**
	 * HTML element (eg h1) to style ID (eg berschrift1) 
	 */
	private Map<String, String> elementToStyleId = new HashMap<String, String>(); 
	
	private static final String HEADING_NAME_PREFIX = "heading ";
	
	public HeadingHandler(Styles styles) {
		
		/*  A style looks like:
		 * 
		 *   <w:style w:type="paragraph" w:styleId="berschrift1">
		 *       <w:name w:val="heading 1"/>
		 *       
		 *  In other words, irrespective of the language, the name
		 *  remains in English.
		 */
		
		for (Style s : styles.getStyle()) {
			
			if (s.getName().getVal().startsWith(HEADING_NAME_PREFIX)) {
				
				// We may wish to map this
				String styleName= s.getName().getVal();
				String suffix =  styleName.substring(HEADING_NAME_PREFIX.length());
				
				try {
					int lvl = Integer.parseInt(suffix);
					
					if (lvl>=1 && lvl <=9) {
						elementToStyleId.put("h"+lvl, s.getStyleId());
					}
					
				} catch (NumberFormatException nfe) {
					
				}
				
			}
		}
		
	}

	protected String getStyle(String localname) {
		return elementToStyleId.get(localname);
	}
	
}

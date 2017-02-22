package org.docx4j.convert.in.xhtml;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.docx4j.fonts.microsoft.MicrosoftFonts;
import org.docx4j.fonts.microsoft.MicrosoftFontsRegistry;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.css.CSSValue;




/**
 * @author jharrop
 * @since 3.2.0
 */
public class FontHandler {
	
	public static Logger log = LoggerFactory.getLogger(FontHandler.class);			
	
	private static FontFamilyMap fontFamilyToFont = new FontFamilyMap();
	
	static {
		
		// Add the defaults
		addFontMapping("serif", 
				ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.fonts.default.serif", "Times New Roman"));
		addFontMapping("sans-serif", 
				ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.fonts.default.sans-serif", "Arial"));
		addFontMapping("monospace", 
				ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.fonts.default.monospace", "Courier New"));

		// Add Microsoft ones
		Map<String, MicrosoftFonts.Font> msFontsByName = MicrosoftFontsRegistry.getMsFonts();
		for (String cssFontFamily : msFontsByName.keySet()){
			addFontMapping( cssFontFamily, cssFontFamily); // identity mapping for these
		}
	}

	
	/* Word 2010 Web Options lets you set:
	 * 
	 *  proportional font: defaults to TNR 12
	 *  fixed width font: defaults to Courier New 10
	 * 
	 */	

	protected static void addFontMapping(String cssFontFamily, RFonts rFonts) {
		fontFamilyToFont.put(cssFontFamily, rFonts);
	}
	
	protected static void addFontMapping(String cssFontFamily, String font) {
		
		RFonts rFonts = Context.getWmlObjectFactory().createRFonts();
		rFonts.setAscii(font);
		rFonts.setHAnsi(font);  // handle chars with diacritic eg ěščřžýáíé
		
		FontHandler.addFontMapping(cssFontFamily, rFonts);
	}
	
	protected static RFonts getRFonts(String cssFontFamily) {
		
		return fontFamilyToFont.get(cssFontFamily);
	}
	
	protected static void setRFont(CSSValue fontFamily, RPr rpr) {
		
		if (fontFamily==null) return;
//		log.debug(fontFamily.getCssText());
		
		// Short circuit
		RFonts rfonts = fontFamiliesToFont.get(fontFamily.getCssText());
		if (rfonts!=null) {
			rpr.setRFonts(rfonts);
			return;
		}
		
		StringTokenizer st = new StringTokenizer(fontFamily.getCssText(), ",");
		// font-family:"Century Gothic", Helvetica, Arial, sans-serif;
		while (st.hasMoreTokens()) {
			String thisFontFamily = st.nextToken().trim();
			
			thisFontFamily = thisFontFamily.replace("'", "");
			thisFontFamily = thisFontFamily.replace("\"", "");
			
			RFonts mappedTo = FontHandler.getRFonts(thisFontFamily);
			// Assume the first font family for which we have a mapping will contain a glyph
			// TODO should check. See fonts.txt
			if (mappedTo==null) {
				log.warn("No mapping for: '" + thisFontFamily + "'");
				
				/* TODO:  bold italic handling.
				 * 
				 * consider further the round trip scenario, 
				 * where docx4j's xhtml export specifies fonts
				 * such as
				 * 
				 *    Segoe UI Bold
				 *    Segoe UI Bold Italic
				 *    Times New Roman Bold
				 * 
				 * but those yield warnings here. 
				 * 
				 */
			} else {
				rpr.setRFonts(mappedTo);
				// Save for re-use
				fontFamiliesToFont.put(fontFamily.getCssText(), mappedTo);
				return;
			}
		}
	}
    private static Map<String, RFonts> fontFamiliesToFont = new HashMap<String, RFonts>(); 
	
	
	/**
	 * Case insensitive key
	 * (matching http://www.w3.org/TR/css3-fonts/#font-family-casing
	 */
	static class FontFamilyMap extends HashMap<String, RFonts> {

		@Override
		public RFonts put(String key, RFonts value) {
			return super.put(key.toLowerCase(), value);
		}

		// not @Override because that would require the key parameter to be of
		// type Object
		public RFonts get(String key) {
			return super.get(key.toLowerCase());
		}
	}	
}

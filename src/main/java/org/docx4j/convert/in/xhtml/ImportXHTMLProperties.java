package org.docx4j.convert.in.xhtml;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4j.utils.ResourceUtils;

/**
 * @since 3.1
 */public class ImportXHTMLProperties {
	 
	 /* Currently, the following can be specified in docx4j-ImportXHTML.properties:
	  *
	  * "docx4j-ImportXHTML.fonts.default.serif", default: "Times New Roman"
	  * "docx4j-ImportXHTML.fonts.default.sans-serif", default: "Arial"
	  * "docx4j-ImportXHTML.fonts.default.monospace", default: "Courier New"
	  * 
	  * "docx4j-ImportXHTML.Bidi.Heuristic", default: false
	  * 
	  * "docx4j-ImportXHTML.Element.Heading.MapToStyle", false
	  * 
	  * Note: Via code, you can configure:
	  * 
	  * FontHandler.addFontMapping which lets you map a font family, for example "Century Gothic" in:
	  * 
	  *    font-family:"Century Gothic", Helvetica, Arial, sans-serif;
	  * 
	  * to a w:rFonts object, for example:
	  * 
	  *    <w:rFonts w:ascii="Arial Black" w:hAnsi="Arial Black"/>	
	  *    
	  * You can set FormattingOption: CLASS_TO_STYLE_ONLY, CLASS_PLUS_OTHER, IGNORE_CLASS
	  * independently for runs, paragraphs, tables
	  * 
	  * CLASS_TO_STYLE_ONLY: a Word style matching a class attribute will
	  * be used, and nothing else
	  * 
	  * CLASS_PLUS_OTHER: a Word style matching a class attribute will
	  * be used; other css will be translated to direct formatting
	  * 
	  * IGNORE_CLASS: css will be translated to direct formatting
	  * 
	  * There is also the idea of a CSS white list.  If it is non-null,
	  * a CSS property will only be honoured if it is on the list.
	  * Useful where suitable default values aren't being provided via
	  * @class, or direct values are otherwise providing unwanted results.

     
	 *      */
	
	protected static Logger log = LoggerFactory.getLogger(ImportXHTMLProperties.class);
	
	private static Properties properties;
	
	private static void init() {
		
		properties = new Properties();
		try {
			properties.load(
					ResourceUtils.getResource("docx4j-ImportXHTML.properties"));
		} catch (Exception e) {
			log.warn("Couldn't find/read docx4j-ImportXHTML.properties; " + e.getMessage());
		}
	}
	
	public static String getProperty(String key) {
		
		if (properties==null) {init();}
				
		return properties.getProperty(key);		
	}

	
	public static String getProperty(String key, String defaultValue) {
		
		if (properties==null) {init();}
				
		return properties.getProperty(key, defaultValue);		
	}
	
	public static boolean getProperty(String key, boolean defaultValue) {
		
		if (properties==null) {init();}
		String result = properties.getProperty(key, Boolean.toString(defaultValue));
		return Boolean.parseBoolean(result);
	}
	
	public static Properties getProperties() {
		
		if (properties==null) {init();}
		return properties;		
	}
	
	/**
	 * Useful if a unit test requires a certain property value.
	 */
	public static void setProperty(String key, Boolean value) {
		if (properties==null) {init();}
		properties.setProperty(key, value.toString());		
	}	

	/**
	 * Useful if a unit test requires a certain property value.
	 */
	public static void setProperty(String key, String value) {
		if (properties==null) {init();}
		properties.setProperty(key, value);		
	}	
	
}

package org.docx4j.convert.in.xhtml;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4j.utils.ResourceUtils;

/**
 * @since 3.1
 */public class ImportXHTMLProperties {
	
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

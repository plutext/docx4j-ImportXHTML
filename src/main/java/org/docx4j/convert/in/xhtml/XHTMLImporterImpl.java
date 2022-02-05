/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2013, Plutext Pty Ltd, and contributors.
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

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Bidi;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;

import org.docx4j.Docx4jProperties;
import org.docx4j.UnitsOfMeasurement;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.renderer.DocxRenderer;
import org.docx4j.convert.out.html.HtmlCssHelper;
import org.docx4j.jaxb.Context;
import org.docx4j.model.PropertyResolver;
import org.docx4j.model.properties.Property;
import org.docx4j.model.properties.PropertyFactory;
import org.docx4j.model.properties.paragraph.AbstractParagraphProperty;
import org.docx4j.model.properties.paragraph.Indent;
import org.docx4j.model.properties.run.AbstractRunProperty;
import org.docx4j.model.properties.run.FontSize;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.wml.Body;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTMarkupRange;
import org.docx4j.wml.CTSimpleField;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.DocDefaults.RPrDefault;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.P;
import org.docx4j.wml.P.Hyperlink;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.Ind;
import org.docx4j.wml.PPrBase.NumPr;
import org.docx4j.wml.PPrBase.PStyle;
import org.docx4j.wml.R;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.RStyle;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSPrimitiveValue;
//import org.w3c.dom.css.CSSPrimitiveValue;
//import org.w3c.dom.css.CSSValue;
import org.xml.sax.InputSource;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.DerivedValue;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.ColorValue;
import com.openhtmltopdf.css.style.derived.CountersValue;
import com.openhtmltopdf.css.style.derived.FunctionValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.ListValue;
import com.openhtmltopdf.css.style.derived.NumberValue;
import com.openhtmltopdf.css.style.derived.StringValue;
import com.openhtmltopdf.layout.Styleable;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.render.AnonymousBlockBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.InlineBox;
import com.openhtmltopdf.resource.XMLResource;

/**
 * Convert XHTML + CSS to WordML content.  Can convert an entire document, 
 * or a fragment consisting of one or more block level objects.
 * 
 * Your XHTML must be well formed XML!  
 * 
 * For usage examples, please see org.docx4j.samples/XHTMLImportFragment, 
 * and XHTMLImportDocument 
 * 
 * For best results, be sure to include src/main/resources on your classpath. 
 * 
 * Includes support for:
 * - paragraph and run formatting
 * - tables
 * - images
 * - lists (ordered, unordered)#
 * 
 * People complain flying-saucer is slow
 * (due to DTD related network lookups).
 * See http://stackoverflow.com/questions/5431646/is-there-any-way-improve-the-performance-of-flyingsaucer
 * 
 * Looking at FSEntityResolver, the problem is that there
 * is no resources/schema on dir anymore which can be put on
 * the classpath.  Once this problem is fixed, things work better.
 * 
 * TODO:
 * - insert, delete
 * - space-before, space-after unrecognized CSS property
 * 
 * @author jharrop
 * @since 2.8
 *
 */
public class XHTMLImporterImpl implements XHTMLImporter {
	
	public static Logger log = LoggerFactory.getLogger(XHTMLImporterImpl.class);		

	private XHTMLImporterImpl() {}

    protected WordprocessingMLPackage wordMLPackage;
    private RelationshipsPart rp;
    private NumberingDefinitionsPart ndp;
	
	public XHTMLImporterImpl(WordprocessingMLPackage wordMLPackage) {
		
		displayFormattingOptionSettings();
		
    	this.wordMLPackage= wordMLPackage;
    	rp = wordMLPackage.getMainDocumentPart().getRelationshipsPart();
    	ndp = wordMLPackage.getMainDocumentPart().getNumberingDefinitionsPart();
		if (ndp==null) {
			log.debug("No NumberingDefinitions part - so adding");
			try {
				ndp = new NumberingDefinitionsPart();
				wordMLPackage.getMainDocumentPart().addTargetPart(ndp);
				ndp.setJaxbElement( Context.getWmlObjectFactory().createNumbering() );				
			} catch (InvalidFormatException e1) {
				// Won't happen
				e1.printStackTrace();
			}
		}

    	listHelper = new ListHelper(this, ndp);
    	tableHelper = new TableHelper(this);
    	
    	
		if (hyperlinkStyleId !=null && wordMLPackage instanceof WordprocessingMLPackage) {
			((WordprocessingMLPackage)wordMLPackage).getMainDocumentPart().getPropertyResolver().activateStyle(hyperlinkStyleId);
		}
		
		initStyleMap(wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart());
		
		if (ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.Element.Heading.MapToStyle", false)) {
			headingHandler = new HeadingHandler(wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement());
		}
		
		imports = Context.getWmlObjectFactory().createBody();
		contentContextStack.push(imports);
		
		bookmarkHelper = new BookmarkHelper(wordMLPackage);
    }	
	
	/**
	 * Configure, how the Importer styles hyperlinks
	 * 
	 * If hyperlinkStyleId is set to <code>null</code>, hyperlinks are
	 * styled using just the CSS. This is the default behavior.
	 * 
	 * If hyperlinkStyleId is set to <code>"someWordHyperlinkStyleName"</code>, 
	 * that style is used. The default Word hyperlink style name is "Hyperlink".
	 * It is currently your responsibility to define that style in your
	 * styles definition part.
	 * 
	 * @param hyperlinkStyleID
	 *            The style to use for hyperlinks (eg Hyperlink)
	 */
	public void setHyperlinkStyle (
			String hyperlinkStyleID) {
		hyperlinkStyleId = hyperlinkStyleID;
	}
	private String hyperlinkStyleId = null;	
	
	
    /**
	 * If you have your own implementation of the XHTMLImageHandler interface
	 * which you'd like to use.
	 */
	public void setXHTMLImageHandler(XHTMLImageHandler xHTMLImageHandler) {
		this.xHTMLImageHandler = xHTMLImageHandler;
	}
	
	private XHTMLImageHandler xHTMLImageHandler = new XHTMLImageHandlerDefault(this);
	
	@Override
	public void setMaxWidth(int maxWidth, String tableStyle) {
	    xHTMLImageHandler.setMaxWidth(maxWidth, tableStyle);
	}
	
	
	
	// Extension point, for example DivToSdt
	private DivHandler divHandler;
	
	public void setDivHandler(DivHandler divHandler) {
		this.divHandler = divHandler;
	}
	
	
	
	private Body imports = null; 
    
    
    
    private ListHelper listHelper;
    
    protected ListHelper getListHelper() {
		return listHelper;
	}
	private TableHelper tableHelper;
    
	protected TableHelper getTableHelper() {
	    return tableHelper;
	}
	
    private DocxRenderer renderer;
    
    /**
	 * @return the renderer
	 */
	public DocxRenderer getRenderer() {
		if (renderer==null) {
			
			if (paragraphFormatting==FormattingOption.CLASS_PLUS_OTHER
					|| paragraphFormatting==FormattingOption.CLASS_TO_STYLE_ONLY ) {
					// Not strictly necessary in the CLASS_TO_STYLE_ONLY case
				
				renderer = new DocxRenderer(stylesToCSS());
				
			} else {			
				renderer = new DocxRenderer();
			}
			
		}
		
		return renderer;
	}

	/**
	 * @param renderer the renderer to set
	 */
	public void setRenderer(DocxRenderer renderer) {
		this.renderer = renderer;
	}
	
    /**
	 * Map a font family, for example "Century Gothic" in:
	 * 
	 *    font-family:"Century Gothic", Helvetica, Arial, sans-serif;
	 * 
	 * to a w:rFonts object, for example:
	 * 
	 *    <w:rFonts w:ascii="Arial Black" w:hAnsi="Arial Black"/>
	 * 
	 * Assuming style font-family:"Century Gothic", Helvetica, Arial, sans-serif;
	 * the first font family for which there is a mapping is the one
	 * which will be used. 
	 * 
	 * xhtml-renderer's CSSName defaults font-family: serif
	 * 
	 * It is your responsibility to ensure a suitable font is available 
	 * on the target system (or embedded in the docx package).  If we 
	 * (eventually) support CSS @font-face, docx4j could do that
	 * for you (at least for font formats we can convert to something
	 * embeddable).
	 * 
	 * You should set these up once, for all your subsequent 
	 * imports, since some stuff is cached and currently won't get updated
	 * if you add fonts later.
	 * 
	 * @since 3.0
	 */
	public static void addFontMapping(String cssFontFamily, RFonts rFonts) {
		FontHandler.addFontMapping(cssFontFamily, rFonts);
	}

	public static void addFontMapping(String cssFontFamily, String font) {
		
		FontHandler.addFontMapping(cssFontFamily, font);
	}

	/**
	 * @param runFormatting
	 *            the runFormatting to set
	 */
	public void setRunFormatting(FormattingOption runFormatting) {
		this.runFormatting = runFormatting;
	}
	private FormattingOption runFormatting = FormattingOption.CLASS_PLUS_OTHER;

	/**
	 * @param paragraphFormatting
	 *            the paragraphFormatting to set
	 */
	public void setParagraphFormatting(
			FormattingOption paragraphFormatting) {
		this.paragraphFormatting = paragraphFormatting;
	}
	private FormattingOption paragraphFormatting = FormattingOption.CLASS_PLUS_OTHER;

	/**
	 * @param tableFormatting the tableFormatting to set
	 */
	public void setTableFormatting(FormattingOption tableFormatting) {
		this.tableFormatting = tableFormatting;
	}
	private FormattingOption tableFormatting = FormattingOption.CLASS_PLUS_OTHER;

	protected FormattingOption getTableFormatting() {
		return tableFormatting;
	}

	private void displayFormattingOptionSettings() {
		log.info("tableFormatting: " + tableFormatting);
		log.info("paragraphFormatting: " + paragraphFormatting);
		log.info("runFormatting: " + runFormatting);
	}
	
	/**
	 * If the CSS white list is non-null,
	 * a CSS property will only be honoured if it is on the list.
	 * 
	 * Useful where suitable default values aren't being provided via
	 * @class, or direct values are otherwise providing unwanted results.
	 * 
	 * Using this should be a last resort.
	 * 
	 * @param cssWhiteList the cssWhiteList to set
	 */
	@Deprecated
	public static void setCssWhiteList(Set<String> cssWhiteList) {
		cssWhiteList = cssWhiteList;
	}
	private static Set<String> cssWhiteList = null;



	
	private HeadingHandler headingHandler = null;
	
	private BookmarkHelper bookmarkHelper; 
	
	//@Override
	public AtomicInteger getBookmarkIdLast() // actually, this returns that incremented by 1.
	{  
		return bookmarkHelper.getBookmarkId();
	}

	/* 
	 * Support injecting a starting bookmark value, so bookmark numbers
	 * can be managed across invocations.
	 * 
	 * @see org.docx4j.convert.in.xhtml.XHTMLImporter#setBookmarkIdNext(java.util.concurrent.atomic.AtomicInteger)
	 */
	//@Override
	public void setBookmarkIdNext(AtomicInteger val) {
		bookmarkHelper.setBookmarkId(val);		
	}
	
	
	private CTMarkupRange markuprange;
	

	/**
	 * Use the default font size in this docx, as equivalent of CSS font-size: medium
	 * @since 3.0
	 */
	private void setDefaultFontSize() {
		
		StyleDefinitionsPart sdp = wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart();
		if (sdp!=null
			 && sdp.getJaxbElement().getDocDefaults()!=null) {
			
			RPrDefault rPrDefault = sdp.getJaxbElement().getDocDefaults().getRPrDefault();
			
			// <w:rPrDefault>
			//   <w:rPr>
			//     <w:sz w:val="22"/>
		        
			if (rPrDefault!=null
					&& rPrDefault.getRPr()!=null
						&& rPrDefault.getRPr().getSz()!=null) {
				
				HpsMeasure sz = rPrDefault.getRPr().getSz();
				FontSize.mediumHalfPts.set(sz.getVal());
			}
		}
	}
	
	private void unsetDefaultFontSize() {
		FontSize.mediumHalfPts.remove(); // remove thread local var when we're done
	}
	
	java.util.Map<String, org.docx4j.wml.Style> stylesByID = new java.util.HashMap<String, org.docx4j.wml.Style>();
    private void initStyleMap(StyleDefinitionsPart sdp) {
    	
    	if (sdp==null) return;

    	org.docx4j.wml.Styles styles = sdp.getJaxbElement();
    	
		for ( org.docx4j.wml.Style s : styles.getStyle() ) {				
			stylesByID.put(s.getStyleId(), s);				
		}
    }
	
	/**
	 * Where @class is to be used as a mapping to an existing Word style,
	 * we also want FS to use that Word style in the CSS it is applying
	 * (since if this does not happen, some of the CSS applied will be 
	 * default CSS, and this will overwrite the intended style with direct
	 * formatting assuming CLASS_PLUS_OTHER) 
	 * @param pkg
	 * @return
	 */
	private String stylesToCSS() {
		
		String css = wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getCss();
		
		if (css==null) {
			StringBuilder result = new StringBuilder();
			HtmlCssHelper.createCssForStyles(wordMLPackage, wordMLPackage.getMainDocumentPart().getStyleTree(), result);
			css = result.toString();
			wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().setCss(css);
		}
		
		log.info(css);
		
		return css;
	}
    

    /**
     * Convert the well formed XHTML contained in file to a list of WML objects.
     * 
     * @param file
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(File file, String baseUrl) throws Docx4JException {

        renderer = getRenderer();
        
        File parent = file.getAbsoluteFile().getParentFile();
        
        try {
			renderer.setDocument(
					renderer.loadDocument(file.toURI().toURL().toExternalForm()),
			        (parent == null ? "" : parent.toURI().toURL().toExternalForm())
			);
		} catch (MalformedURLException e) {
			throw new Docx4JException("Malformed URL", e);
		}

        renderer.layout();
                    
        traverse(renderer.getRootBox(), null);
        
        return imports.getContent();    	
    }

    /**
     * Convert the well formed XHTML from the specified SAX InputSource
     * 
     * @param is
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(InputSource is,  String baseUrl) throws Docx4JException {

        renderer = getRenderer();
        
        Document dom = XMLResource.load(is).getDocument();        
        renderer.setDocument(dom, baseUrl);
        
        renderer.layout();
                    
        traverse(renderer.getRootBox(),  null);
        
        return imports.getContent();    	
    }

    /**
     * @param is
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(InputStream is, String baseUrl) throws Docx4JException {
    	
        renderer = getRenderer();
        
        Document dom = XMLResource.load(is).getDocument();        
        renderer.setDocument(dom, baseUrl);

        renderer.layout();
                    
        traverse(renderer.getRootBox(), null);
        
        return imports.getContent();    	
    }
    
    /**
     * @param node
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(Node node,  String baseUrl) throws Docx4JException {
    	
        renderer = getRenderer();
        if (node instanceof Document) {
        	renderer.setDocument( (Document)node, baseUrl );
        } else {
        	Document doc = XmlUtils.neww3cDomDocument();
        	doc.importNode(node, true);
        	renderer.setDocument( doc, baseUrl );
        }
        renderer.layout();
                    
        traverse(renderer.getRootBox(),  null);
        
        return imports.getContent();    	
    }
    
    /**
     * @param reader
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(Reader reader,  String baseUrl) throws Docx4JException {
    	
        renderer = getRenderer();
        
        Document dom = XMLResource.load(reader).getDocument();        
        renderer.setDocument(dom, baseUrl);
        
        renderer.layout();
                    
        traverse(renderer.getRootBox(),  null);
        
        return imports.getContent();    	
    }
    
//    /**
//     * @param source
//     * @param baseUrl
//     * @param wordMLPackage
//     * @return
//     * @throws IOException
//     */
//    public List<Object> convert(Source source,  String baseUrl) throws Docx4JException {
//    	    	
//        renderer = getRenderer();
//                
//        Document dom = XMLResource.load(source).getDocument();        
//        renderer.setDocument(dom, baseUrl);
//
//        renderer.layout();
//                    
//        traverse(renderer.getRootBox(),  null);
//        
//        return imports.getContent();    	
//    }
    
    //public List<Object> convert(XMLEventReader reader) throws IOException {
    //public List<Object> convert(XMLStreamReader reader) throws IOException {
    
    /**
     * Convert the well formed XHTML found at the specified URI to a list of WML objects.
     * 
     * @param url
     * @param wordMLPackage
     * @return
     */
    public List<Object> convert(URL url) throws Docx4JException {

        renderer = getRenderer();
        
        String urlString = url.toString();
        Document dom =renderer.loadDocument(urlString);
        renderer.setDocument(dom, urlString);
        renderer.layout();
                    
        traverse(renderer.getRootBox(),  null);
        
        return imports.getContent();    	
    }

    /**
     * 
     * Convert the well formed XHTML contained in the string to a list of WML objects.
     * 
     * @param content
     * @param baseUrl
     * @param wordMLPackage
     * @return
     */
    public List<Object> convert(String content,  String baseUrl) throws Docx4JException {
    	
    	/* Test for and if present remove BOM, which causes "SAXParseException: Content is not allowed in prolog"
    	 * See further:
    	 *     http://stackoverflow.com/questions/4897876/reading-utf-8-bom-marker
    	 *     http://www.unicode.org/faq/utf_bom.html#BOM
    	 */
    	
    	int firstChar = content.codePointAt(0);
    	if (firstChar==0xFEFF) {
    		log.info("Removing BOM..");
    		content = content.substring(1);
    	}
    	
        renderer = getRenderer();
        
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        
        Document dom;
        try {
        	dom = XMLResource.load(is).getDocument();
        } catch  ( com.openhtmltopdf.util.XRRuntimeException xre) {
        	// javax.xml.transform.TransformerException te
        	Throwable t = xre.getCause();
        	log.error(t.getMessage(), t);
        	if (t instanceof javax.xml.transform.TransformerException) {
	        	// eg content of elements must consist of well-formed character data or markup.
        		
        		
	        	Throwable t2 = ((javax.xml.transform.TransformerException)t).getCause();
	        	if (t2 instanceof org.xml.sax.SAXParseException) {
	        		throw new Docx4JException(
		        			"issues at Line " + ((org.xml.sax.SAXParseException)t2).getLineNumber() 
		        			+ ", Col " + ((org.xml.sax.SAXParseException)t2).getColumnNumber(), t);
	        		
	        	}

        		throw new Docx4JException(
	        			((javax.xml.transform.TransformerException)t).getLocationAsString(), t);
	        	
        	} else {
        		throw xre;
        	}
        }
        
        
        renderer.setDocument(dom, baseUrl);
        renderer.layout();
                    
        traverse(renderer.getRootBox(),  null);
        
        return imports.getContent();    	
    }
    
    public Map<String, PropertyValue> getCascadedProperties(CalculatedStyle cs) {
    	
    	// Similar to renderer.getLayoutContext().getSharedContext().getCss().getCascadedPropertiesMap(e)?
    	
    	// or use getStyle().valueByName directly; see TableHelper for example

    	
    	Map<String, PropertyValue> cssMap = new HashMap<String, PropertyValue>();
    	
    	
    	FSDerivedValue[] derivedValues = cs.getderivedValuesById();
        for (int i = 0; i < derivedValues.length; i++) {
        	        	
            CSSName name = CSSName.getByID(i);
            
            if (name.toString().startsWith("-fs")) continue;
            
            if (cssWhiteList!=null) {
            	if (cssWhiteList.contains(name.toString())) {
//            		log.debug("Whitelist: contains " + name.toString() );
            	} else {
            		continue; // ignore it
            	}
            }
                        
            FSDerivedValue val = cs.valueByName(name); // walks parents as necessary to get the value
            
        	// An IdentValue represents a string that you can assign to a CSS property,
        	// where the string is one of several enumerated values. 
        	// font-size could be a IdentValue (eg medium) or a LengthValue (eg 12px) 
            
            if (val == null) {
            	
            	log.warn("Skipping " +  name.toString() + " .. (null value)" );            	
            	
            } else {
            	
            	if (log.isDebugEnabled()) {
            		log.debug(val.getClass().getName() + ": " + name + " = " + val.asString());
            	}
            	
            	if (val instanceof IdentValue) {

		        	// Workaround for docx4j < 8.3, which doesn't handle start|end
		        	if (name.toString().equals("text-align")
		        			&& (val.asString().equals("start")
		        					|| val.asString().equals("end"))) {
		        		
						PropertyValue val2; 
		        		if (val.asString().equals("start")) {
		        			// Not bidi aware; assume ltr
		        			val2 = new PropertyValue(CSSPrimitiveValue.CSS_IDENT, "left", "left"); 
		        		} else {
		        			val2 = new PropertyValue(CSSPrimitiveValue.CSS_IDENT, "right", "right"); 		        			
		        		}
			        	cssMap.put(name.toString(), val2 );
		        		
		        	} else {
            		
						PropertyValue val2 = new PropertyValue( (IdentValue)val ); 
		//				PropertyValue val2 = new PropertyValue(CSSPrimitiveValue.CSS_IDENT, val.asString(), val.asString()); 
			        	cssMap.put(name.toString(), val2 );
		        	}
	        	
	            } else if (val instanceof ColorValue) {
	            	
	//            	Object o = ((ColorValue)val).asColor();
	    			PropertyValue val2 = new PropertyValue( ((ColorValue)val).asColor() ); 
	            	cssMap.put(name.toString(), val2 );            		
	
	            } else if (val instanceof LengthValue) {

	    			PropertyValue val2 = new PropertyValue(getLengthPrimitiveType(val) , val.asFloat(), val.asString()); 
	            	cssMap.put(name.toString(), val2 );
	            	
	            } else if (val instanceof NumberValue) {
	            	
	    			PropertyValue val2 = new PropertyValue(((NumberValue)val).getCssSacUnitType() , val.asFloat(), val.asString()); 
	            	cssMap.put(name.toString(), val2 );
	
	            } else if (val instanceof StringValue) {
	            	
	    			PropertyValue val2 = new PropertyValue(((StringValue)val).getCssSacUnitType() , val.asString(), val.asString()); 
	            	cssMap.put(name.toString(), val2 );
	
	            } else if (val instanceof ListValue) {
	            	
	    			PropertyValue val2 = new PropertyValue( ((ListValue)val).getValues() ); 
	            	cssMap.put(name.toString(), val2 );
	
	            } else if (val instanceof CountersValue) {
	            	
	            	boolean unused = false;
	    			PropertyValue val2 = new PropertyValue( ((CountersValue)val).getValues(), unused ); 
	            	cssMap.put(name.toString(), val2 );
	
	            } else if (val instanceof FunctionValue) {
	            	
	    			PropertyValue val2 = new PropertyValue( ((FunctionValue)val).getFunction() ); 
	            	cssMap.put(name.toString(), val2 );
	            	
	            }  else 
	            	if (val instanceof DerivedValue) {   
	            		
	            	// We should've handled all known types of abstract class DerivedValue above!
	            	log.warn("TODO handle DerivedValue type " +  val.getClass().getName() 
	            			+ " with name  " + name + " = " + val.asString());
	    			PropertyValue val2 = new PropertyValue( ((DerivedValue)val).getCssSacUnitType() , val.asString(), val.asString()); 
	            	cssMap.put(name.toString(), val2 );
            	
	            } else  {
	            	
	            	log.warn("TODO Skipping " +  name.toString() + " .. " + val.getClass().getName() );
	            }
            }
        }
    	
        return cssMap;
    	
    }
    

    public static short getLengthPrimitiveType(FSDerivedValue val) {
    	
    	if (val instanceof LengthValue) {
    		return ((LengthValue) val).getLengthPrimitiveType();
    	} else {
    		throw new RuntimeException("Unexpected type " + val.getClass().getName());
    	}
    }
    
    /**
     * The Block level elements that our content may go into, ie
     * Body, Table, Tr, Td.
     * 
     *  P and P.Hyperlink are NOT added to contentContextStack.
     */
    private LinkedList<ContentAccessor> contentContextStack = new LinkedList<ContentAccessor>();
    
	protected LinkedList<ContentAccessor> getContentContextStack() {
		return contentContextStack;
	}
    
    private void pushBlockStack(ContentAccessor ca) {
    	
    	//log.debug("pushed " + ca.getClass().getSimpleName());
    	
    	contentContextStack.push(ca);
    	attachmentPointP = null;
    }
    private ContentAccessor popBlockStack() {
    	
    	//log.debug("popping " + contentContextStack.peek().getClass().getSimpleName());
    	
    	attachmentPointP = null;
    	return contentContextStack.pop();
    }
    

    
    // Our runs may go into a P, or a hyperlink.
    // Currently the approach to tracking this is simple.
    // The content goes into a P, unless the hyperlink object is non-null.
    // And we clear the P object whenever we transition in/out of a tc,
    // or away from body level.
    
    P attachmentPointP = null;
    P.Hyperlink attachmentPointH = null;
    private P getCurrentParagraph(boolean create) {
    	
    	if (attachmentPointP !=null) return attachmentPointP;  
    	if (create) {
//    		log.debug("defining new p", new Throwable());
			P newP = Context.getWmlObjectFactory().createP();
			attachmentPointP = newP;
			this.contentContextStack.peek().getContent().add(newP);
            return newP;
    	} else {
    		return null;
    	}
    }
    
    private ContentAccessor getListForRun() {
    	
    	if (attachmentPointH!=null) return attachmentPointH;
    	return getCurrentParagraph(true);
    }
    


    
    private void traverse(Box box, TableProperties tableProperties) throws Docx4JException {
    	setDefaultFontSize();
    	traverse( box, null,  tableProperties);
    	unsetDefaultFontSize();
    }    
    
    private void traverse(Box box,  Box parent, TableProperties tableProperties) throws Docx4JException {
    	
        log.debug(box.getClass().getName() );
        if (box instanceof BlockBox) {
        	        	
            traverseBlockBox( box,   parent,  tableProperties);
            
        } else if (box instanceof AnonymousBlockBox) {
            log.debug("AnonymousBlockBox");            
        } else {
        	log.warn(box.getClass().getName());
        }
            
    }
    
    private void traverseBlockBox(Box box,  Box parent, TableProperties tableProperties) throws Docx4JException {

    	boolean mustPop = false;
        BlockBox blockBox = ((BlockBox)box);
    	
        Element e = box.getElement(); 
        if (e==null) {
        	// Shouldn't happen
            log.debug("<NULL>");
        } else {            
//            log.debug("BB"  + "<" + e.getNodeName() + " " + box.getStyle().toStringMine() );
            log.debug(box.getStyle().getStringProperty(CSSName.DISPLAY) );
//                log.debug(box.getElement().getAttribute("class"));            	
        }
        
        // bookmark start?
        CTMarkupRange markupRangeForID = null;
        if (box instanceof com.openhtmltopdf.newtable.TableSectionBox) {
        	// ignore, since <table id = ..
        	// generates TableBox<table and TableSectionBox<table
        	// but we only want a single bookmark
        } else if(box instanceof com.openhtmltopdf.newtable.TableBox) {

        	// null P, so it bookmark is a P sibling
        	markupRangeForID = bookmarkHelper.anchorToBookmark(e, bookmarkNamePrefix, 
            		null, this.contentContextStack.peek());
        	
        } else {
        	
        	markupRangeForID = bookmarkHelper.anchorToBookmark(e, bookmarkNamePrefix, 
        		getCurrentParagraph(false), this.contentContextStack.peek());
        }
        
        if (markupRangeForID!=null) {
            log.debug("Added bookmark for "+ box.getClass().getName()  + "<" + e.getNodeName() );//+ " " + box.getStyle().toStringMine() );
        }
        
        // Don't add a new paragraph if this BlockBox is display: inline
        if (e!=null) {
            
        	//Map cssMap = styleReference.getCascadedPropertiesMap(e);
            Map<String, PropertyValue> cssMap = getCascadedProperties(box.getStyle());
        	
        	/* Sometimes, when it is display: inline, the following is not set:
            	CSSValue cssValue = (CSSValue)cssMap.get("display");
            	if (cssValue !=null) {
            		log.debug(cssValue.getCssText() );
            	}
            */
        	// So do it this way ...
            if (e.getNodeName().equals("div")) {
            	
            	if (divHandler!=null) {
            		ContentAccessor ca = divHandler.enter(blockBox, this.contentContextStack.peek());
            		if (ca!=null) {
            			pushBlockStack(ca);
            			mustPop = true;
            		}
            	}
            	
            	/* consider:
            	 * 
            	 *     <li><div>ListItem2</div></li>
            	 * 
            	 * That div has eg list-style-type: decimal; but display: block; 
            	 * and won't have written the number yet, so handle this
            	 * 
            	 */
            	if (/* its a block (the inline case is ok; list-item is TBD) */
            			box.getStyle().getStringProperty(CSSName.DISPLAY).equals("block")
            			
            		/* and we have an inherited definition */
            			&& this.getCurrentParagraph(false)!=null
            			&& this.getCurrentParagraph(false).getPPr()!=null 
            			&& this.getCurrentParagraph(false).getPPr().getNumPr()!=null 
            			) {
            		NumPr numPr = this.getCurrentParagraph(false).getPPr().getNumPr();
                	this.getCurrentParagraph(true).setPPr(this.getPPr(blockBox, cssMap));
                	this.getCurrentParagraph(false).getPPr().setNumPr(numPr);
                	/* actually, we should be using the definition found here, 
                	 * since it could be intended to override! 
                	 * 
                	 * But currently we only write a list definition at line ~1100 where isListItem.
                	 * so for now we don't support overriding the list definition
                	 * */ 
            	} else {
            		// usual case
                	this.getCurrentParagraph(true).setPPr(this.getPPr(blockBox, cssMap));
            		
            	}
            	
            } else if (box.getStyle().getStringProperty(CSSName.DISPLAY).equals("inline") ) {
        		
//                	// Don't add a paragraph for this, unless ..
//                	if (currentP==null) {
//                		currentP = Context.getWmlObjectFactory().createP();
//    		            contentContext.getContent().add(currentP);
//    		            paraStillEmpty = true;
//                	}            		
        	} else if (e.getNodeName().equals("ol")
        			|| e.getNodeName().equals("ul") ) {
        		
        		log.info("entering list");
        		listHelper.pushListStack(blockBox);
        		
            	
        	} else if (box instanceof com.openhtmltopdf.newtable.TableSectionBox) {
            	// nb, both TableBox and TableSectionBox 
            	// have node name 'table' (or can have),
        		// so this else clause is before the TableBox one,
        		// to avoid a class cast exception
        		
        		// eg <tbody color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; border-collapse: collapse; -fs-border-spacing-horizontal: 0; -fs-border-spacing-vertical: 0; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table-row-group; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: static; ; right: auto; src: none; table-layout: auto; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: middle; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: none; border-right-style: none; border-bottom-style: none; border-left-style: none; border-top-width: 2px; border-right-width: 2px; border-bottom-width: 2px; border-left-width: 2px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0; 
        		log.debug(".. processing <tbody");
        		
        		// Do nothing here for now .. the switch statement below traverses children
        		
        		// TODO: give effect to this CSS

        	} else if (box instanceof com.openhtmltopdf.newtable.TableBox)  {
            	
        		log.debug(".. processing table");  // what happened to <colgroup><col style="width: 2.47in;" /><col style="width: 2.47in;" /> 
        		
        		/*
        		 * BEWARE: xhtmlrenderer seems to parse tables differently,
        		 * depending on whether:
        		 * 
        		 * (i) the table is contained within a <div>
        		 * 
        		 * (ii) the table contains <caption>
        		 * 
        		 * See https://github.com/plutext/flyingsaucer/issues/1
        		 * 
        		 * Bare table with caption: BlockBox cannot be cast to TableSectionBox in xhtmlrenderer
        		 * 
        		 * div/table[count(caption)=1] ... table becomes TableBox, children are CONTENT_BLOCK
        		 * 
        		 * div/table[count(caption)=0] ... table becomes TableBox, children are CONTENT_BLOCK
        		 */

        		// eg <table color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; 
        		//           border-collapse: collapse; -fs-border-spacing-horizontal: 2px; -fs-border-spacing-vertical: 2px; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: relative; ; right: auto; src: none; 
        		//           table-layout: fixed; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: baseline; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: solid; border-right-style: solid; border-bottom-style: solid; border-left-style: solid; border-top-width: 1px; border-right-width: 1px; border-bottom-width: 1px; border-left-width: 1px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0in; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0;
        		
//            		if (this.contentContextStack.peek() instanceof Tr) {
//            			popStack();
//            		}
//            		if (this.contentContextStack.peek() instanceof Tbl) {
//            			popStack();
//            		}
        		
        		ContentAccessor contentContext = this.contentContextStack.peek();
        		tableHelper.nestedTableHierarchyFix(contentContext,parent);
        		
        		// If we added a p for a div, but its empty, then remove it 
        		P currentP = getCurrentParagraph(false);
        		if (currentP!=null
        				&& currentP.getContent().size()==0) {
        			// remove it
        			contentContext.getContent().remove(currentP);
        		}
        		
        		Tbl tbl = Context.getWmlObjectFactory().createTbl();
        		contentContext.getContent().add(tbl);
	            pushBlockStack(tbl);
	            mustPop = true;
	            
	            TableBox tableBox = (com.openhtmltopdf.newtable.TableBox)box;
	            
	    		tableProperties = new TableProperties();
	    		tableProperties.setTableBox(tableBox);

//		    		log.debug("parent " + parent.getClass().getSimpleName());
//		    		log.debug("parent " + parent.getElement().getNodeName());
	    		
	    		/*
	    		boolean isNested = (parent instanceof TableBox
	    				|| (parent!=null
	    					&& parent.getElement()!=null
	    					&& ( parent.getElement().getNodeName().equals("table")
	    						 || parent.getElement().getNodeName().equals("td"))));
	    		// 2017 08 30, could look at block stack instead?
	    		*/
	    		
//		    		log.debug("is nested? " + isNested);
	            
	    		tableHelper.setupTblPr( tableBox,  tbl,  tableProperties);
	    		tableHelper.setupTblGrid( tableBox,  tbl,  tableProperties);
	            
            	
        	} else if (e.getNodeName().equals("table") ) {
        		// but not instanceof com.openhtmltopdf.newtable.TableBox
        		// .. this does happen.  See test/resources/block-level-lots.xhtml
        		
        		// TODO: look at whether we can style the table in this case

        		log.warn("Encountered non-TableBox table: " + box.getClass().getName() );

//            		if (this.contentContextStack.peek() instanceof Tr) {
//            			popStack();
//            		}
//            		if (this.contentContextStack.peek() instanceof Tbl) {
//            			popStack();
//            		}
        		
        		ContentAccessor contentContext = this.contentContextStack.peek();
        		tableHelper.nestedTableHierarchyFix(contentContext,parent);
        		
        		// If we added a p for a div, but its empty, then remove it 
        		P currentP = getCurrentParagraph(false);
        		if (currentP!=null
        				&& currentP.getContent().size()==0) {
        			// remove it
        			contentContext.getContent().remove(currentP);
        		}            		
        		
        		Tbl tbl = Context.getWmlObjectFactory().createTbl();
        		contentContext.getContent().add(tbl);
	            pushBlockStack(tbl);
	            mustPop = true;
	            
        		
        	} else if (box instanceof com.openhtmltopdf.newtable.TableRowBox) {
        		
        		// eg <tr color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; border-collapse: collapse; -fs-border-spacing-horizontal: 0; -fs-border-spacing-vertical: 0; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table-row; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: static; ; right: auto; src: none; table-layout: auto; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: top; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: none; border-right-style: none; border-bottom-style: none; border-left-style: none; border-top-width: 2px; border-right-width: 2px; border-bottom-width: 2px; border-left-width: 2px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0;
        		
        		// TODO support vertical-align
        		
        		log.debug(".. processing <tr");            		

//            		if (this.contentContextStack.peek() instanceof Tr) {
//            			this.contentContextStack.pop();
//            		} 
        		
        		Tr tr = Context.getWmlObjectFactory().createTr();
        		this.contentContextStack.peek().getContent().add(tr);
	            pushBlockStack(tr);
	            mustPop = true;
        		
	            tableHelper.setupTrPr((com.openhtmltopdf.newtable.TableRowBox)box, tr); // does nothing at present
        		
        	} else if (box instanceof com.openhtmltopdf.newtable.TableCellBox) {
        		            		
        		log.debug(".. processing <td");            		
        		// eg <td color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; border-collapse: collapse; -fs-border-spacing-horizontal: 0; -fs-border-spacing-vertical: 0; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table-row; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: static; ; right: auto; src: none; table-layout: auto; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: top; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: none; border-right-style: none; border-bottom-style: none; border-left-style: none; border-top-width: 2px; border-right-width: 2px; border-bottom-width: 2px; border-left-width: 2px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0;

        		ContentAccessor trContext = contentContextStack.peek();

        		com.openhtmltopdf.newtable.TableCellBox tcb = (com.openhtmltopdf.newtable.TableCellBox)box;
	            
        		// rowspan support: vertically merged cells are
        		// represented as a top cell containing the actual content with a vMerge tag with "restart" attribute 
        		// and a series of dummy cells having a vMerge tag with no (or "continue") attribute.            		
        		            		
        		// if current cell is the first real cell in the row, but is not in the leftmost position, then
        		// search for vertically spanned cells to the left and insert dummy cells before current
        		if (tcb.getParent().getChild(0) == tcb && tcb.getCol() > 0) {
        			tableHelper.insertDummyVMergedCells(contentContextStack.peek(), tcb, true);
        		}
        		
				Tc tc = Context.getWmlObjectFactory().createTc();
        		contentContextStack.peek().getContent().add(tc);
        		pushBlockStack(tc);//.getContent();
	            mustPop = true;

        		
	            tableHelper.setupTcPr(tcb, tc, tableProperties);
        		
				// search for vertically spanned cells to the right from current, and insert dummy cells after it
	            tableHelper.insertDummyVMergedCells(trContext, tcb, false);

        	} else if (isListItem(blockBox.getElement())
        			/*  <li class="ListParagraph Normal DocDefaults " style="display: list-item;text-align: justify;"><span style="">Level 1</span></li>

        			 *   <li>
				            <p>Item 2</p>
				            DON"T TRIGGER THIS LINE
				        </li>
        			 */
        			&& !(blockBox instanceof com.openhtmltopdf.render.AnonymousBlockBox)) {

	            // Paragraph level styling
            	//P currentP = this.getCurrentParagraph(true);
            	
        		// You'll get an NPE here if you have li which isn't in ol|ul
            	listHelper.peekListItemStateStack().init(); 
            	
                PPr pPr =  Context.getWmlObjectFactory().createPPr();
                this.getCurrentParagraph(true).setPPr(pPr);
            	
                if (paragraphFormatting.equals(FormattingOption.IGNORE_CLASS)) {
                	
            		listHelper.addNumbering(this.getCurrentParagraph(true), blockBox.getElement(), cssMap);	                	
            		addParagraphProperties(pPr, blockBox, cssMap );
	                        		
                } else {
                	// CLASS_TO_STYLE_ONLY or CLASS_PLUS_OTHER
	            	if (listHelper.peekListStack().getElement()!=null
	            			&& listHelper.peekListStack().getElement().getAttribute("class")!=null) {
	            		// NB Currently, you need to put this @class on the ol|ul at each level of nesting,
	            		// if you want to use the list style.
	            		// If you only put it on some levels, well, new list(s) will be created for the others,
	            		// with imperfect results...
	            		
	            		String cssClass = listHelper.peekListStack().getElement().getAttribute("class").trim();
	            		log.debug(cssClass);
	            		if (cssClass.equals("")) {
	            			// What to do? same thing as if no @class specified
	            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
	    	            		listHelper.addNumbering(this.getCurrentParagraph(true), blockBox.getElement(), cssMap);
	            				addParagraphProperties(pPr, blockBox, cssMap );
	            			}
	            			// else its CLASS_TO_STYLE_ONLY,
	            			// but since we have no @class, do nothing
	            			
	            		} else {
	            			// Usual case...
	            			
		            		// Our XHTML export gives a space separated list of class names,
		            		// reflecting the style hierarchy.  Here, we just want the first one.
		            		// TODO, replace this with a configurable stylenamehandler.
		            		int pos = cssClass.indexOf(" ");
		            		if (pos>-1) {
		            			cssClass = cssClass.substring(0,  pos);
		            		}
		            		
		            		// if the docx contains this stylename, set it
		            		Style s = this.stylesByID.get(cssClass);
		            		if (s==null) {
		            			log.debug("No docx style for @class='" + cssClass + "'");
		            			
		            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
		    	            		listHelper.addNumbering(this.getCurrentParagraph(true), blockBox.getElement(), cssMap);
		            				addParagraphProperties(pPr, blockBox, cssMap );
		            			}
		            			// else, can't number
		            			
		            		} else if (s.getType()!=null && s.getType().equals("numbering")) {
		            			log.debug("Using list style from @class='" + cssClass + "'");
		            			
		            			/* it should contain something like:
		            			 * 
		            			 *     <w:pPr>
									      <w:numPr>
									        <w:numId w:val="1"/>
									      </w:numPr>
									    </w:pPr>
		            			 *
		            			 * Use this... 
		            			 * 
		            			 * We don't actually set the numbering style on the 
		            			 * paragraph, because numbering styles aren't used that way
		            			 * in Word.
		            			 */
		            			BigInteger numId = s.getPPr().getNumPr().getNumId().getVal();
		            			listHelper.setNumbering(pPr, numId);  
		            				// TODO: @start is ignored in this case (it is handled in addNumbering) 
		            			
		            			// Note that we just use the numbering it points to;
		            			// we don't follow it to its abstract num (which is in fact
		            			// where the w:styleLink matching our @class should be found).
		            			
		            			// TODO: if this list is being used a second time, we should
		            			// restart numbering??  Is it restarted in the HTML?

			            		// OK, we've applied @class
		            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
		            				// now apply ad hoc formatting
		            				addParagraphProperties(pPr, blockBox, cssMap );
		            			}			            		
		            			
		            		} else {
		            			log.debug("For docx style for @class='" + cssClass + "', but its not a numbering style ");
		            			
		            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
		            				
		    	            		listHelper.addNumbering(this.getCurrentParagraph(true), blockBox.getElement(), cssMap);
		    	            		
		    	            		// SPECIAL CASE
		    	            		if (Docx4jProperties.getProperty("docx4j.model.datastorage.BindingTraverser.XHTML.Block.rStyle.Adopt", false)
				            				&& s.getType()!=null && s.getType().equals("paragraph")) { 	
		    	            			
		    	            			log.debug(".. using " + s.getStyleId() );
		    	            			
		    	            			PStyle pStyle = Context.getWmlObjectFactory().createPPrBasePStyle();
		    	            			pStyle.setVal(s.getStyleId());
		    	            			this.getCurrentParagraph(false).getPPr().setPStyle(pStyle);			    	            			
		    	            		}
		            				addParagraphProperties(pPr, blockBox, cssMap );
		            			}			            			
		            			
		            		}
		            		
	            		}
	            	} else {
	            		// No @class
            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
            				addParagraphProperties(pPr, blockBox, cssMap );
            			}
            			// else its CLASS_TO_STYLE_ONLY,
            			// but since we have no @class, do nothing
	            	}
	            	
            	} 
        		
        	} else if  (e.getNodeName().equals("img")) {
	            	addImage(blockBox);

        	} else if  (e.getNodeName().equals("hr")) {
            	
        		this.contentContextStack.peek().getContent().add(
        				getPforHR());
	            	
            } else {
            	
            	log.debug("default handling for " + e.getNodeName());
            	
            	// Paragraph processing.  Generally, we'll create a new P.
            	// An exception to that is li/p[1], where we want to use 
            	// the p created for the li.
            	if (listHelper.getDepth()>0
            			&& !listHelper.peekListItemStateStack().haveMergedFirstP) {
            		// use existing attachmentPoint
            		log.debug("use existing attachmentPoint");
            		listHelper.peekListItemStateStack().haveMergedFirstP = true;
            	} else {
            		
            		log.debug("create new attachmentPoint");
	            	P currentP = this.getCurrentParagraph(true);
	                currentP.setPPr(this.getPPr(blockBox, cssMap));
	                
	                log.debug(XmlUtils.marshaltoString(currentP));
	                

	            	if (e.getNodeName().equals("figcaption")) {
	            		prepareCaption(e, currentP);
	            	}
            	}
                
            }
    	}
        
        // the recursive bit:
        
    	
        	log.debug("Processing children of " + box.getElement().getNodeName() );
            switch (blockBox.getChildrenContentType()) {
                case BLOCK:
                	log.debug(".. which are BlockBox.CONTENT_BLOCK");	                	
                    for (Object o : ((BlockBox)box).getChildren() ) {
                        log.debug("   processing child " + o.getClass().getName() );
                    	
                        traverse((Box)o,  box, tableProperties);                    
                        log.debug(".. processed child " + o.getClass().getName() );
                    }
                    break;
                case INLINE:
                	
                	log.debug(".. which are BlockBox.CONTENT_INLINE");	                	
                	
                    if ( ((BlockBox)box).getInlineContent()!=null) {
                    	
                        for (Object o : ((BlockBox)box).getInlineContent() ) {
//                            log.debug("        " + o.getClass().getName() ); 
                            if (o instanceof InlineBox ) {
//                                    && ((InlineBox)o).getElement()!=null // skip these (pseudo-elements?)
//                                    && ((InlineBox)o).isStartsHere()) {
                                
                            	processInlineBox( (InlineBox)o);
                            		                            	
                            } else if (o instanceof BlockBox ) {
                            	
                                traverse((Box)o, box, tableProperties); // commenting out gets rid of unwanted extra parent elements
                                //contentContext = tmpContext;
                            } else {
                                log.debug("What to do with " + box.getClass().getName() );                        
                            }
	                        log.debug(".. processed child " + o.getClass().getName() );
                        }
                        
                        
                    	if (markuprange!=null) {        		
                    		getCurrentParagraph(true).getContent().add( markuprange);
                    		markuprange = null;
                    	}
//                    		inAlreadyProcessed = false;        		

                    	// Handle case:  <li>plain text
                    	if (listHelper.getDepth()>0) {
    	            		listHelper.peekListItemStateStack().haveMergedFirstP = true;
                    	}
                    }
                    break;

                case EMPTY:
                    break;
                    
                case UNKNOWN:
                	log.warn(".. which are UNKNOWN " );
                    break;
                    
               default:
                	log.warn(".. which are ??? " + blockBox.getChildrenContentType() );
                	break;
            } 
        
	    
        log.debug("Done processing children of " + box.getClass().getName() );
        // contentContext gets its old value back each time recursion finishes,
        // ensuring elements are added at the appropriate level (eg inside tr) 
    	if (e.getNodeName().equals("ol")
    			|| e.getNodeName().equals("ul") ) {
    		
    		log.info(".. exiting list");
    		
    		listHelper.popListStack();
    	}    
        
        if (this.contentContextStack.peek() instanceof Tc) {
            // nested tables must end with a <p/> or Word 2010 can't open the docx!
            // ie:
            // <w:tc>
            //   <w:tbl>..</w:tbl>
            //   <w:p/>                <---------- 
            // </w:tc>
        	// This fixes the dodgy table/table case
        	Tc tc = (Tc)this.contentContextStack.peek();
        	
        	if (tc.getContent().size()==0
        			|| tc.getContent().get(tc.getContent().size()-1) instanceof Tbl) {
        		tc.getContent().add(
        				Context.getWmlObjectFactory().createP());
        	}
        }

        // new P, except following an image
    	if  (!e.getNodeName().equals("img")) {
    		attachmentPointP = null; 
            if (mustPop) popBlockStack();
    	}
    	
        if (e.getNodeName().equals("div")) {
        	if (divHandler!=null) {
        		divHandler.leave();
        	}
        }
    	
        	
//            // An empty tc shouldn't make the table disappear!
//            // TODO - make more elegant
//            if (e.getNodeName().equals("table")) {            	
//            	paraStillEmpty = false;
//            }
//
//            
    	
    	// bookmark end
    	if (markupRangeForID!=null) {
    		bookmarkHelper.attachBookmarkEnd(markupRangeForID, getCurrentParagraph(false), this.contentContextStack.peek());
    		markupRangeForID = null;
    	}
    
    }
    
    private static final String FIGCAPTION_SEQUENCE_ATTRIBUTE_NAME="sequence";
    private static final String FIGCAPTION_SEQUENCE_ATTRIBUTE_VALUE_DEFAULT="Figure"; 
    
    
    
    private Map<String, Integer> sequenceCounters = null;
    
	/**
	 * Get the current numbers of SEQ fields, used in image captions.
	 * Typically you'd use this if you are importing multiple
	 * times into a single docx (as for example, OpenDoPE does).
	 * 
	 * @param sequenceCounters
	 */
    public Map<String, Integer> getSequenceCounters() {
    	if (sequenceCounters==null) {
    		sequenceCounters = new HashMap<String, Integer>();
    	}
		return sequenceCounters;
	}

	/**
	 * Set the last used numbers of SEQ fields, used in image captions.
	 * Key is sequence name.  The default is "Figure", but you can also use
	 * others (matching value of @sequence).
	 * @param sequenceCounters
	 */
	public void setSequenceCounters(Map<String, Integer> sequenceCounters) {
		this.sequenceCounters = sequenceCounters;
	}

	private void prepareCaption(Element figcaption, P currentP) {
    	
    	// set <w:pStyle w:val="Caption"/>
    	PPr pPr = currentP.getPPr();
    	if (pPr == null) {
    		pPr = Context.getWmlObjectFactory().createPPr();
    		currentP.setPPr(pPr);    	
    	}
    	PStyle pStyle = pPr.getPStyle();
    	if (pStyle == null) {
    		pStyle = Context.getWmlObjectFactory().createPPrBasePStyle();
    		pPr.setPStyle(pStyle); 	
    	}
    	pStyle.setVal("Caption");
    	
    	String sequenceName = FIGCAPTION_SEQUENCE_ATTRIBUTE_VALUE_DEFAULT;
    	if (figcaption.getAttribute(FIGCAPTION_SEQUENCE_ATTRIBUTE_NAME)!=null 
    			&& figcaption.getAttribute(FIGCAPTION_SEQUENCE_ATTRIBUTE_NAME).trim().length()>0) {
    		sequenceName = figcaption.getAttribute(FIGCAPTION_SEQUENCE_ATTRIBUTE_NAME);
    	}
    	
    	Integer i = this.getSequenceCounters().get(sequenceName);
    	int count = (i == null ? 0 : i);
    	this.getSequenceCounters().put(sequenceName, ++count);    	
    	
    	
    	if (!sequenceName.endsWith(" ")) sequenceName +=" "; // adds trailing space
    	
    	// The run, "Figure"
    	R figureRun = new R();
    	Text text = Context.getWmlObjectFactory().createText();
    	figureRun.getContent().add(text);
    	text.setValue(sequenceName);
    	text.setSpace("preserve");
    	currentP.getContent().add(figureRun);
    	
    	CTSimpleField simpleField = new CTSimpleField();
    	simpleField.setInstr(" SEQ " + sequenceName + " \\* ARABIC ");
    	JAXBElement<CTSimpleField> wrapper = Context.getWmlObjectFactory().createPFldSimple(simpleField); 
    	currentP.getContent().add(wrapper);
    	
    	// add w:t containing value
    	R resultRun = new R();
    	Text result = Context.getWmlObjectFactory().createText();
    	result.setValue("" + count);
    	resultRun.getContent().add(result);
    	
    	simpleField.getContent().add(resultRun);
    }

    protected PPr getPPr(BlockBox blockBox, Map<String, PropertyValue> cssMap) {
    	
        PPr pPr =  Context.getWmlObjectFactory().createPPr();
        populatePPr(pPr, blockBox, cssMap);
    	return pPr;
    }
    
    protected boolean isBidi(String pText) {
    	
    	if (pText==null
    			|| pText.trim().length()==0) {
    		return false;
    	}
    	
		if ( !ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.Bidi.Heuristic", false)) {
			return false;
		}
		
		int ltr=0;
		int rtl=0;

	    Bidi bidi = new Bidi(pText, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
//    	System.out.println("count " + bidi.getRunCount());
	    for (int i=0; i<bidi.getRunCount(); i++) {
	    	
	    	int start = bidi.getRunStart(i);
	    	int end = bidi.getRunLimit(i);
	    	
//	    	System.out.println("adding " + i);
	    	
	    	if (isEven(bidi.getRunLevel(i) )) {
	    		ltr += (end-start);
	    	} else {
	    		rtl += (end-start);	    		
	    	}
	    }
	    
	    if (ltr==0) {
	    	if (rtl>0) {
	    		return true;
	    	} else {
	    		return false; // default to LTR	    		
	    	}
	    }
	    return ((rtl/ltr)>0.5);    	
    }
    
    protected void populatePPr(PPr pPr, Styleable blockBox, Map<String, PropertyValue> cssMap) {
    	
        if (paragraphFormatting.equals(FormattingOption.IGNORE_CLASS)) {
    		addParagraphProperties(pPr, blockBox, cssMap );
    		handleHeadingElement( pPr,  blockBox); // (if its h1, h2 etc)
        } else {
        	// CLASS_TO_STYLE_ONLY or CLASS_PLUS_OTHER
        	if (blockBox.getElement()==null) {
        		log.debug("null blockBox element");        		
        	} else if (blockBox.getElement().getAttribute("class")==null) {
        		log.debug("no @class");        		        		
        	} else  {
        		
        		String cssClass = blockBox.getElement().getAttribute("class").trim();
        		if (cssClass.equals("")) {
        			// Since there is no @class value, we might use a heading style 
            		handleHeadingElement( pPr,  blockBox); // (if its h1, h2 etc)
        		} else {
        			
            		// Our XHTML export gives a space separated list of class names,
            		// reflecting the style hierarchy.  Here, we just want the first one.
            		// TODO, replace this with a configurable stylenamehandler.
            		int pos = cssClass.indexOf(" ");
            		if (pos>-1) {
            			cssClass = cssClass.substring(0,  pos);
            		}
            		
            		// if the docx contains this stylename, set it
            		Style s = this.stylesByID.get(cssClass);
            		if (s==null) {
            			log.debug("No docx style for @class='" + cssClass + "'");
            			// Since there is no style, we might use a heading style 
                		handleHeadingElement( pPr,  blockBox); // (if its h1, h2 etc)
            		} else if (s.getType()!=null && s.getType().equals("paragraph")) {
            			PStyle pStyle = Context.getWmlObjectFactory().createPPrBasePStyle();
            			pPr.setPStyle(pStyle);
            			pStyle.setVal(cssClass);
            		} else {
            			log.debug("For docx style for @class='" + cssClass + "', but its not a paragraph style ");
            			// Since that's not a p style, we might use a heading style 
                		handleHeadingElement( pPr,  blockBox); // (if its h1, h2 etc)
            		}
        		}
        	}
			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
				addParagraphProperties(pPr, blockBox, cssMap );
			}        	
    	}

        if (blockBox.getElement()==null) {
        	log.debug("BB getElement is null");        
        } else if (isBidi(blockBox.getElement().getTextContent())) {
        	log.debug(".. setting bidi property");
        	pPr.setBidi(new BooleanDefaultTrue() );
        }
    	
        
    }
    
    /**
     * If one parameter is passed then search style by id (1st parameter), 
     * if style by id is not found then search style by name (also 1st parameter).
     * <br>If two - then search by id (1st parameter) and if style by id is not found then search style by name (2nd parameter).
     * <br>Other parameters are ignored.
     */
    protected Style getStyleByIdOrName(String... parameters) {
        String id = parameters[0];
        String name = parameters.length > 1 ? parameters[1] : id;
        Style s = null;
        // Our XHTML export gives a space separated list of class names,
        // reflecting the style hierarchy. Here, we just want the first one.
        int pos = id.indexOf(" ");
        if (pos > -1) {
            id = id.substring(0,  pos);
        }
        // try Id
        s = this.stylesByID.get(id);
        if(s == null) {
            // try name, which can have spaces
            for(Style style: this.stylesByID.values()) {
                if(style.getName() != null && style.getName().getVal().equals(name)) {
                    s = style;
                    break;
                }
            }
        }
        return s;
    }
    
    private void handleHeadingElement(PPr pPr, Styleable blockBox) {
    	
    	if (headingHandler==null 
    			|| !isHeading(blockBox)) return;
    	
    	// Its a heading; set the style
    	String styleId = headingHandler.getStyle(blockBox.getElement().getLocalName());
    	
    	if (styleId!=null) {
			PStyle pStyle = Context.getWmlObjectFactory().createPPrBasePStyle();
			pPr.setPStyle(pStyle);
			pStyle.setVal(styleId);
    	}    	
    }
    
    private boolean isHeading(Styleable blockBox) {
    	
    	if (blockBox.getElement()==null) return false;
    	
    	String elName = blockBox.getElement().getLocalName();
    	
    	return (("h1").equals(elName)
    			|| ("h2").equals(elName)
    			|| ("h3").equals(elName)
    			|| ("h4").equals(elName)
    			|| ("h5").equals(elName)
    			|| ("h6").equals(elName) );    	
    }
    

 /**
		Currently flying saucer is initialized with DEFAULT_DOTS_PER_PIXEL = 20.
		Keep in mind that the values returned from FS are in dots (as opposed to px)
	*/
	private void addImage(BlockBox box) {
		
		/*
			com.openhtmltopdf.layout.SharedContext
		     * 
				 * @deprecated Belongs in Java2D renderer.
			    private final static float DEFAULT_DPI = 72;
				
			     * @deprecated Belongs in Java2D renderer.
			    private final static int DEFAULT_DOTS_PER_PIXEL = 1;
							    
			     * Used to adjust fonts, ems, points, into screen resolution.
			     * Internal program dots per inch.
			    private float dpi;
			
			     * Internal program dots per pixel.
			    private int dotsPerPixel = DEFAULT_DOTS_PER_PIXEL;
			    
			     * dpi in a more usable way
			     * Internal program dots per mm (probably a fraction).
			    private float mmPerDot;			     * 
		
   	*/
		
		// System.out.println(box.getStyle().toStringMine() );
		// max-height: none; max-width: none; min-height: 0; min-width: 0;
		
		
		// 20x as expected
		Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), 
				getRenderer().getLayoutContext()); // LayoutContext implements CssContext
//		log.debug("contentBounds H: " + contentBounds.height);
//		log.debug("contentBounds W: " + contentBounds.width);

		// contentBounds.width = box.getContentWidth()
		// System.out.println("content width: " + box.getContentWidth());
		// box.getHeight() and  box.getWidth() impacted by style='padding-top:10px;' etc, so don't use here
		
		// support max-width eg 10px
		int oldMaxWidth = -1;
		if (xHTMLImageHandler instanceof XHTMLImageHandlerDefault) {
			oldMaxWidth = ((XHTMLImageHandlerDefault)xHTMLImageHandler).getMaxWidth();
			if (!box.getStyle().isMaxWidthNone()) {
				// max-width is set; to what?
				int maxWidth = box.getStyle().getMaxWidth(getRenderer().getLayoutContext(), 0); // revisit base value?
					// eg 10px x 20 dots per pixel = 200
				if (maxWidth>0) {
					((XHTMLImageHandlerDefault)xHTMLImageHandler)
						.setMaxWidth( (int) dotsToTwip(maxWidth));
				}
			}
		}
		
		Long cy = contentBounds.height==0 ? null : 
			UnitsOfMeasurement.twipToEMU( dotsToTwip(contentBounds.height) );
		Long cx = contentBounds.width==0 ? null : 
			UnitsOfMeasurement.twipToEMU( dotsToTwip(contentBounds.width) );
		
		xHTMLImageHandler.addImage( renderer.getDocx4jUserAgent(), wordMLPackage, 
				this.getCurrentParagraph(true), box.getElement(), cx, cy);
		
		// reset maxWidth
		if ( (xHTMLImageHandler instanceof XHTMLImageHandlerDefault) 
			&& !box.getStyle().isMaxWidthNone()) {
			((XHTMLImageHandlerDefault)xHTMLImageHandler).setMaxWidth(oldMaxWidth);
		}
	}
	
	private float dotsToTwip(float dots) {
		return dots * 72f/(UnitsOfMeasurement.DPI); 
		// Java2D DPI/
	}
	
	
//	private void addHyperlinkIfNec(String href, Map<String, CSSValue> cssMap) {
//		
//		if (href!=null
//				&& !href.trim().equals("")) {
//			
//        	Hyperlink h = createHyperlink(
//            			href, 
//            			addRunProperties( cssMap ),
//            			href, rp);                                    	            		
//                currentP.getContent().add(h);	
//		}
//	}
	
	
	
    private void  processInlineBox( InlineBox inlineBox) {
    	
    	if (inlineBox.getPseudoElementOrClass()!=null) {
    		log.debug("Ignoring Pseudo");
    		return;
    	}
    	
        // Doesn't extend box
        Styleable s = inlineBox;
        
    	if (log.isDebugEnabled() ) {
        	log.debug(inlineBox.toString());

        	if (s.getElement() == null) {
        		log.debug("Null element name" ); 
        		// log.debug(inlineBox.getPseudoElementOrClass()); // empty
    		} else {
        		log.debug(s.getElement().getNodeName());
//                log.debug(s.getElement().getAttribute("class"));
        	}
    	}
    			
    			
        if (s.getStyle()==null) { // Assume this won't happen
        	log.error("getStyle returned null!");
        }
        

        // Short circuit for <a /> ie no child elements
    	if (s.getElement() !=null
    			&& s.getElement().getNodeName().equals("a")
    			&& inlineBox.isStartsHere()
    			//&& inlineBox.isEndsHere() 
    			&& !inlineBox.getElement().hasChildNodes()
    			) {
    		// self closing tag
    		log.debug("anchor which starts and ends here");
    		
    		/* Don't use inlineBox.isEndsHere(), since it incorrectly
    		 * returns true for opening anchor of:
    		 * 
    		 *    <a href="#_summary" class="report_table_of_content">Summary</a>
    		 * 
    		 */
    		
    		CTMarkupRange inlineMarkupRange = bookmarkHelper.anchorToBookmark(s.getElement(), bookmarkNamePrefix, getCurrentParagraph(false), this.contentContextStack.peek());
    		if (inlineMarkupRange!=null) {
    			bookmarkHelper.attachBookmarkEnd( inlineMarkupRange, getCurrentParagraph(false), this.contentContextStack.peek() );
        		markuprange = null;        		
    		}
        
    		String href = s.getElement().getAttribute("href"); 
    		if (href!=null && !href.trim().equals("")) {
    			log.warn("Ignoring @href on <a> without content.");
    		}
    		return;
    		
    	} 
        
        Map<String, PropertyValue> cssMap = getCascadedProperties(s.getStyle());
//        Map cssMap = styleReference.getCascadedPropertiesMap(s.getElement());
        
        
        // Make sure the current paragraph is formatted
        P p = this.getCurrentParagraph(true);
        if (p.getPPr()==null) {
        	PPr pPr = Context.getWmlObjectFactory().createPPr();
        	
        	populatePPr(pPr, s,  cssMap);  // nb, since the element is likely to be null, this is unlikely to give us a @class; that's why we need to make the p in the enclosing div (or p).       	
        	//addParagraphProperties( pPr,  s,  cssMap);
        	p.setPPr(pPr);
        }
        
                        
        String debug = "<UNKNOWN Styleable";
        if (s.getElement()==null) {
        	// Do nothing
        } else {
            debug = "<" + s.getElement().getNodeName();
            
            String cssClass = getClassAttribute(s.getElement());
        	if (cssClass!=null) {
        	 	cssClass=cssClass.trim();
        	}
        	
            
            if (s.getElement().getNodeName().equals("a")) {
            	
            	if (inlineBox.isStartsHere()) {
                	log.debug("Processing <a>... with class " + cssClass);
            		
            		String name = s.getElement().getAttribute("name");
            		String href = s.getElement().getAttribute("href"); 
            		
            		if (name!=null
            				&& !name.trim().equals("")) {
            			
                		markuprange = bookmarkHelper.anchorToBookmark(s.getElement(), bookmarkNamePrefix, getCurrentParagraph(false), this.contentContextStack.peek());
                		if (markuprange!=null) {
//                    		markuprange = null;         		
                		}
        		        
        		        if (href==null
                				|| href.trim().equals("")) {
        		        	
		                	String theText = inlineBox.getElement().getTextContent();
		                    addRuns(cssClass, cssMap, theText);
		                    
		                	// bookmark end
		                	if (markuprange!=null) {
		                		bookmarkHelper.attachBookmarkEnd(markuprange, getCurrentParagraph(false), this.contentContextStack.peek());
	                    		markuprange = null;        		
		                	}            				
		                    
	
		                	return;
        		        }
            		}
            		
            		if (href!=null
            				&& !href.trim().equals("")) {
            			
	                	Hyperlink h = null;
	                	String linkText = inlineBox.getElement().getTextContent();
	                	//log.debug("getTextContent:" + linkText);  
	                	
	                    RPr rPr =  Context.getWmlObjectFactory().createRPr();
	                    formatRPr(rPr, cssClass, cssMap);
	                    
	                	log.debug(XmlUtils.marshaltoString(rPr));
	                	
	                	// ensure we've got our current p set correctly; this is done above already 
                		// this.getCurrentParagraph(true);
	                	
	                	if (linkText!=null
	                			&& !linkText.trim().equals("")) {
	                		
	                    	// For example, consider <a href=\"#requirement897\">[R_897] <b>Requirement</b> 3</a>
	                    	// Here we are processing the text "[R_897] " (ie the leading text)	                		
		                	log.debug("getText:" + inlineBox.getText());
	                		
	                    	h = createHyperlink(
	                    			href, 
	                    			rPr,
	                    			inlineBox.getText(), rp);                                    	            		
	                    	this.getCurrentParagraph(false).getContent().add(h);
	                    	
	                    	// bookmark end
	                    	if (markuprange!=null) {
	                    		bookmarkHelper.attachBookmarkEnd(markuprange, getCurrentParagraph(false), this.contentContextStack.peek());
	                    		markuprange = null;        			                    		
	                    	}            				
	                    	
	                        		                	
		                	if (inlineBox.isEndsHere()) {
		                    	log.debug("Processing ..</a> (ends here as well) ");
		                    	return; // don't change contentContext
		                		
		                	} else {
		                    	log.debug("now attaching inside hyperlink ");
		                		attachmentPointH = h;
		                		return; 
		                	}
		                	
	                	} 
	                	else {
	                    	// No text content.  An image or something?  TODO handle hyperlink around inline image
	                		log.warn("Expected hyperlink content, since tag not self-closing");
	                    	h = createHyperlink(
	                    			href, 
	                    			rPr,
	                    			href, rp);                                    	            		
	                    	this.getCurrentParagraph(false).getContent().add(h);
		                	
		                	// bookmark end
		                	if (markuprange!=null) {
		                		bookmarkHelper.attachBookmarkEnd(markuprange, getCurrentParagraph(false), this.contentContextStack.peek());
	                    		markuprange = null;        				                		
		                	}            				
		                	
		                	return;
	                	}
            		}
            		
            	} else if (inlineBox.isEndsHere()) {
                	log.debug("Processing ..</a> ");
                	
                	// For example, consider <a href=\"#requirement897\">[R_897] <b>Requirement</b> 3</a>
                	// Here we are processing the text " 3" (ie the trailing content)
                	log.debug("getText:" + inlineBox.getText()); // [R_897]
                	
                	String endingText = inlineBox.getText();
                	
                	if (endingText!=null
                			&& endingText.length()>0) {
                		addRuns(cssClass, cssMap, inlineBox.getText());
                	}
                	
                	attachmentPointH = null;
                	return; 
            		// Can't do bookmark end processing here,
                	// since this isn't always triggered
            	} else {
                	log.debug("Processing <a> content!");
                	// Add it ...
            		
            	}
            	
            	
            } 
            
//            else if (s.getElement().getNodeName().equals("p")) {
//            	// This seems to be the usual case. Odd?
//            	log.debug("p in inline");
//        		currentP = Context.getWmlObjectFactory().createP();                                        	
//            	if (paraStillEmpty) {
//            		// Replace it
//		            contentContext.getContent().remove( contentContext.getContent().size()-1);                                        		
//            	} 
//	            contentContext.getContent().add(currentP);
//	            paraStillEmpty = true;
//	            currentP.setPPr(
//	            		addParagraphProperties( cssMap ));
//            }	            
        }
        if (s.getStyle()!=null) {
//            debug +=  " " + s.getStyle().toStringMine();
        }
        
        
        log.debug(debug );
        //log.debug("'" + ((InlineBox)o).getTextNode().getTextContent() );  // don't use .getText()
        
        processInlineBoxContent(inlineBox, s, cssMap);
        
    }

	private void processInlineBoxContent(InlineBox inlineBox, Styleable s,
			Map<String, PropertyValue> cssMap) {
				
        
        // bookmark start?
        CTMarkupRange markupRangeForID = bookmarkHelper.anchorToBookmark(inlineBox.getElement(), bookmarkNamePrefix, 
        		getCurrentParagraph(false), this.contentContextStack.peek());
		
        if (s!=null && s.getElement()!=null && s.getElement().getNodeName().equals("br") ) {
            
            R run = Context.getWmlObjectFactory().createR();
            getListForRun().getContent().add(run);                
       		run.getContent().add(Context.getWmlObjectFactory().createBr());
        	
        } else if (inlineBox.getText()==null 
        		|| inlineBox.getText().length()==0
        		) {
        	// Doesn't happen anymore, now we're using openhtmltopdf?
			
			if (s == null) {
        		log.debug("Null Styleable" ); 
			} else if (s.getElement() == null) {
        		log.debug("Null element " ); 
			} else if (s.getElement().getNodeName() == null) {
        		log.debug("Null element nodename " ); 
			} else {
            	log.debug("InlineBox has no TextNode, so skipping" );
            	
            	// TODO .. a span in a span or a?
            	// need to traverse, how?
            	
            }
            
        } else  {
            log.warn( "\n\n"+inlineBox.getText() );  // don't use .getText()

            String theText = inlineBox.getText(); 
            log.debug("Processing " + theText);
            
            String cssClass = getClassAttribute(s.getElement());
        	if (cssClass!=null) {
        	 	cssClass=cssClass.trim();
        	}
            addRuns(cssClass, cssMap, theText);
    	            
//                                    else {
//                                    	// Get it from the parent element eg p
//                        	            //Map cssMap = styleReference.getCascadedPropertiesMap(e);
//                        	            run.setRPr(
//                        	            		addRunProperties( cssMap ));                                    	                                    	
//                                    }
        }
		
    	// bookmark end
    	if (markupRangeForID!=null) {
    		bookmarkHelper.attachBookmarkEnd(markupRangeForID, getCurrentParagraph(false), this.contentContextStack.peek());
    	}            				
	}
	
	private String getClassAttribute(Element e) {
		
		if (e==null) {
			return null;
		} else if (e.getAttribute("class")!=null
				&& !e.getAttribute("class").trim().equals("")) {
			return e.getAttribute("class");
		} else if (e.getParentNode() instanceof Element ){
			return getClassAttribute((Element)e.getParentNode());
		} else {
			return null;
		}
		
	}

	/**
	 * @param cssMap
	 * @param theText
	 */
	private void addRuns( String cssClass, Map<String, PropertyValue> cssMap, String theText) {
		
//    	System.out.println(theText);
		
		if ( ImportXHTMLProperties.getProperty("docx4j-ImportXHTML.Bidi.Heuristic", false)) {

			// Is this p bidi?
		    Bidi bidi = null;	
		    Object o = getListForRun();
		    if (o instanceof P) {
			    P p = (P)o;
			    if (p.getPPr()!=null
			    		&& p.getPPr().getBidi()!=null
			    		&& p.getPPr().getBidi().isVal()
			    		) {	
			    	log.debug("using Bidi.DIRECTION_RIGHT_TO_LEFT");
			    	bidi = new Bidi(theText, Bidi.DIRECTION_RIGHT_TO_LEFT);
				} else {
			    	bidi = new Bidi(theText, Bidi.DIRECTION_LEFT_TO_RIGHT);
				}
		    } else {
		    	// eg P.Hyperlink, eg testRichContentTail
		    	log.warn("TODO: bidi handling for " + o.getClass().getName());
		    	bidi = new Bidi(theText, Bidi.DIRECTION_LEFT_TO_RIGHT);		    	
		    }
		    
//	    	System.out.println("count " + bidi.getRunCount());
		    for (int i=0; i<bidi.getRunCount(); i++) {
		    	
		    	int start = bidi.getRunStart(i);
		    	int end = bidi.getRunLimit(i);
		    	
//		    	System.out.println("adding " + i);

				this.addRun(cssClass, cssMap, theText.substring(start, end), 
						!isEven(bidi.getRunLevel(i) )); // even means left to right
		    }
			
		} else { 
		// usual case
			this.addRun(cssClass, cssMap, theText, false);
		}
		
	}


	private boolean isEven(int x) {
		return ((x & 1) == 0 ) ;
	}
	
	private void addRun( String cssClass, Map<String, PropertyValue> cssMap, String theText, boolean isRTL) {
		
		R run = Context.getWmlObjectFactory().createR();
		Text text = Context.getWmlObjectFactory().createText();
		text.setValue( theText );
		if (theText.startsWith(" ")
				|| theText.endsWith(" ") ) {
			text.setSpace("preserve");
		}
		run.getContent().add(text);
		
		getListForRun().getContent().add(run);
		
		// Run level styling
        RPr rPr =  Context.getWmlObjectFactory().createRPr();
        run.setRPr(rPr);
        formatRPr(rPr, cssClass, cssMap);
        
        if (isRTL) {
        	rPr.setRtl(new BooleanDefaultTrue());
        }
	}
	
	private void formatRPr(RPr rPr, String cssClass, Map<String, PropertyValue> cssMap) {

		//addRunProperties(rPr, cssMap );  // ?????
		
        if (runFormatting.equals(FormattingOption.IGNORE_CLASS)) {
    		addRunProperties(rPr, cssMap );
        } else {
        	// CLASS_TO_STYLE_ONLY or CLASS_PLUS_OTHER
        	if (cssClass!=null) {
//        	if (box.getElement()!=null
//        			&& box.getElement().getAttribute("class")!=null) {
//        		String cssClass = box.getElement().getAttribute("class").trim();
        		
        		if (!cssClass.equals("")) {
            		// Our XHTML export gives a space separated list of class names,
            		// reflecting the style hierarchy.  Here, we just want the first one.
            		// TODO, replace this with a configurable stylenamehandler.
            		int pos = cssClass.indexOf(" ");
            		if (pos>-1) {
            			cssClass = cssClass.substring(0,  pos);
            		}
            		
            		// if the docx contains this stylename, set it
            		Style s = this.stylesByID.get(cssClass);
            		if (s==null) {
            			log.debug("No docx style for @class='" + cssClass + "'");
            		} else if (s.getType()!=null && s.getType().equals("character")) {
            			RStyle rStyle = Context.getWmlObjectFactory().createRStyle();
            			rPr.setRStyle(rStyle);
            			rStyle.setVal(cssClass);
            		} else {
            			log.debug("For docx style for @class='" + cssClass + "', but its not a character style ");
            		}
        		}
        	}
			if (runFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
				addRunProperties(rPr, cssMap );
			}
        	
    	} 
				
		// Font is handled separately.  TODO: review this
        PropertyValue fontFamily = cssMap.get("font-family");
		FontHandler.setRFont(fontFamily, rPr );

	}
	
    private boolean isListItem(Element e) {
    	
    	return e.getNodeName().equals("li");
    	// TODO better: display: list-item; ?
    }
    
	
    private void addParagraphProperties(PPr pPr, Styleable styleable, Map<String, PropertyValue> cssMap) {
    	// NB, not invoked in CLASS_TO_STYLE_ONLY case
    	
//    	log.debug("BEFORE " + XmlUtils.marshaltoString(pPr, true, true));
        
        for (Object o : cssMap.keySet()) {
        	
        	String cssName = (String)o;
        	PropertyValue cssValue = cssMap.get(cssName);
        	
        	Property p = PropertyFactory.createPropertyFromCssName(cssName, new DomCssValueAdaptor(cssValue));
        	
        	if (p!=null) {
	        	if (p instanceof AbstractParagraphProperty) {        		
	        		((AbstractParagraphProperty)p).set(pPr);
	        	} else {
	        	    // try specific method
	        	    p = PropertyFactory.createPropertyFromCssNameForPPr(cssName,  new DomCssValueAdaptor(cssValue));
	        	    if (p!=null) {
	        	        if (p instanceof AbstractParagraphProperty) {               
	        	            ((AbstractParagraphProperty)p).set(pPr);
	        	        }
	        	    }
	        	    //log.debug(p.getClass().getName() );
	        	}
        	}
        	
        }

//    	log.debug(XmlUtils.marshaltoString(pPr, true, true));

    	ParagraphBorderHelper pbh = new ParagraphBorderHelper(this);
    	pbh.addBorderProperties(pPr, styleable, cssMap);        

//    	log.debug("after pbh:" + XmlUtils.marshaltoString(pPr, true, true));
    	
        log.debug("list depth:" + listHelper.getDepth());

        if (listHelper.getDepth()>0) {
//        if (styleable.getElement()!=null
//        		&& isListItem(styleable.getElement()) ) {

        	/* In Word, indentation is given effect in the following priority:
        	 * 1. ad hoc setting (if present)
        	 * 2. if specified in the numbering, then that
        	 * 3. finally, the paragraph style 
        	 * 
        	 * so for IGNORE_CLASS or CLASS_PLUS_OTHER, the way to
        	 * honour the CSS is to make an ad hoc setting
        	 * (ie pPr.setInd ).
        	 * 
        	 *  But the practical problem is:
        	 *  (i)  distinguishing a genuine CSS value from a default.
        	 *       Since 0 is the default, we'll ignore that. (Which means
        	 *       explicitly setting 0 will not be honoured!)
        	 *  (ii) in the CLASS_PLUS_OTHER case, will removeRedundantProperties work
        	 *       (using ppr from paragraph style, overridden by numbering)
        	 */
        	
        	// Special handling for indent, since we need to sum values for ancestors
    		int totalPadding = getListHelper().getAbsoluteIndent(getListHelper().peekListStack());
    		log.debug("totalPadding: " + totalPadding);
            
/*        	// FS default css is 40px padding per level = 600 twip
            int defaultInd =  600 * listHelper.getDepth();
            if (totalPadding==defaultInd
            		&& listHelper.peekListItemStateStack().isFirstChild) {
            	// we can't tell whether this is just a default, so ignore it; use the numbering setting
            	log.debug("List indent case 1: pPr indent null; defer to numbering");
            	pPr.setInd(null); 
            } else 
*/            	
    		
    		int tableIndentContrib = tableHelper.tableIndentContrib(this.contentContextStack);
    		if (tableIndentContrib>0) {
    			// we are in a table, so override the numPr indent
    			if (listHelper.peekListItemStateStack().isFirstChild) {

                	// totalPadding gives indent to the bullet;
                	log.debug("List indent table case : pPr indent set for item itself");
                	pPr.setInd(listHelper.createIndent(totalPadding-tableIndentContrib, true)); 
                    listHelper.peekListItemStateStack().isFirstChild=false;
    			} else {
                	pPr.setInd(listHelper.createIndent(totalPadding 
                			+ ListHelper.INDENT_AFTER
                			+ getLocalIndentation(styleable) 
                			-tableIndentContrib, false));
    				
    			}
    			
    		} else if (listHelper.peekListItemStateStack().isFirstChild) {

            	// totalPadding gives indent to the bullet;
            	log.debug("List indent case 2: pPr indent set for item itself");
            	//pPr.setInd(listHelper.createIndent(totalPadding-tableIndentContrib, true)); 
            	pPr.setInd(null); // use the indent in numPr

                listHelper.peekListItemStateStack().isFirstChild=false;
                log.debug("first child in this list item now set to false");
            	
            } else {
            	
            	// totalPadding gives indent to the bullet;
            	// we want to align this subsequent p with the preceding text;
            	// assume 360 twips
            	
            	log.debug("List indent case 3: pPr indent set for follwing child");
            	pPr.setInd(listHelper.createIndent(totalPadding 
            			+ ListHelper.INDENT_AFTER
            			+ getLocalIndentation(styleable) 
            			-tableIndentContrib, false));
            } 
        	
            
        } else {
        	// not in list; handle indent
        	log.debug("not in list; handle indent");
    		Ind ind = Context.getWmlObjectFactory().createPPrBaseInd();
    		ind.setLeft(BigInteger.valueOf(getAncestorIndentation(styleable)) );			
    		pPr.setInd(ind);
        }
        
                
//        for (int i = 0; i < cStyle.getDerivedValues().length; i++) {
//            CSSName name = CSSName.getByID(i);
//            FSDerivedValue val = cStyle.getDerivedValues()[i];
//            Property p = PropertyFactory.createPropertyFromCssName(name, value)
//        }
        
    	// Avoid adding a property which
    	// simply duplicates something which is already in the paragraph style,
    	// since such direct formatting is probably not the author's intent,
    	// and makes the document less maintainable
    	PPr stylePPr = null;
    		// TODO: take numbering pPr into account here (see above)
    	PropertyResolver propertyResolver = this.wordMLPackage.getMainDocumentPart().getPropertyResolver();
    	if (this.getCurrentParagraph(false).getPPr()!=null
    			&& this.getCurrentParagraph(false).getPPr().getPStyle()!=null) {
    			// that's only be true for paragraphFormatting = FormattingOption.CLASS_PLUS_OTHER;
    			// (we never get here for the other options)
    		
    		String styleId = this.getCurrentParagraph(false).getPPr().getPStyle().getVal();
    		stylePPr = propertyResolver.getEffectivePPr(styleId);
    		PPrCleanser.removeRedundantProperties(stylePPr, pPr);
    	}
    	
    	// TODO: cleansing in table context
    	    	
    	log.debug(XmlUtils.marshaltoString(pPr, true, true));
    	
    }

	/**
	 * Inside a list item, get the contribution of any div.
	 */
	protected int getLocalIndentation(Styleable styleable) {

		int localPadding = 0;
		
		Object o = styleable;
		
		while (o !=null
				&& o instanceof BlockBox 
				&& !(o instanceof com.openhtmltopdf.newtable.TableCellBox) 
				&& !( ((BlockBox)o).getElement()!=null && ((BlockBox)o).getElement().getLocalName().equals("li"))
				) {
			
			BlockBox bb = (BlockBox)o;
			localPadding += getBBIndentContrib(bb);
			
			o = bb.getContainingBlock();
		}

		return localPadding;
	}
    
	protected int getAncestorIndentation(Styleable styleable) {

		// Indentation.  Sum of padding-left and margin-left on ancestor divs
		// Expectation is that one or other would generally be used.
		int totalPadding = 0;
		
		Object o = styleable;
		
		while (o !=null
				&& o instanceof BlockBox 
				/* && !(o instanceof com.openhtmltopdf.newtable.TableCellBox) */ //  need to stop if we encounter a table cell? 
				) {
			
			BlockBox bb = (BlockBox)o;
			totalPadding += getBBIndentContrib(bb);
			
			o = bb.getContainingBlock();
		}
		return totalPadding;
	}
    
	private int getBBIndentContrib(BlockBox bb) {

		int paddingI = 0;

		if (bb.getElement()==null) {
			log.debug("null ... " + bb.getClass().getName()) ;
		} else {
			log.debug(bb.getElement().getLocalName() + bb.getClass().getName()) ;				
		}

		if (bb.getStyle()!=null
				&& bb.getStyle().valueByName(CSSName.PADDING_LEFT) instanceof LengthValue) {
				
			LengthValue padding = (LengthValue)bb.getStyle().valueByName(CSSName.PADDING_LEFT);
			PropertyValue val = new PropertyValue(getLengthPrimitiveType(padding) , padding.asFloat(), padding.asString()); 
			paddingI +=Indent.getTwip(new DomCssValueAdaptor( val));
			
			
		}
//		log.debug("+padding-left: " + totalPadding);

		if (bb.getStyle()!=null
				&& bb.getStyle().valueByName(CSSName.MARGIN_LEFT) instanceof LengthValue) {
			
			// margin-left: auto is an IdentValue
			
			LengthValue margin = (LengthValue)bb.getStyle().valueByName(CSSName.MARGIN_LEFT);
			PropertyValue val = new PropertyValue(getLengthPrimitiveType(margin) , margin.asFloat(), margin.asString()); 
			paddingI +=Indent.getTwip(new DomCssValueAdaptor(val));
		}
//		log.debug("+margin-left: " + totalPadding);
		
		// TODO: CSSName.BORDER_LEFT_WIDTH if CSSName.BORDER_LEFT_STYLE is not none
		log.debug("adding: " + paddingI);
		
		return paddingI;
	}
	
	
    private void addRunProperties(RPr rPr, Map cssMap) {
    	
    	log.debug("addRunProperties");
    	
    	String pStyleId = null;
    	if (this.getCurrentParagraph(false).getPPr()!=null
    			&& this.getCurrentParagraph(false).getPPr().getPStyle()!=null) {
    		
    		pStyleId = this.getCurrentParagraph(false).getPPr().getPStyle().getVal();

    		// Special case: suppress run styling in image caption
    		if ("Caption".equals(pStyleId)) return;
    	}
    	
        for (Object o : cssMap.keySet()) {
        	
        	String cssName = (String)o;
        	PropertyValue cssValue = (PropertyValue)cssMap.get(cssName);
        	
        	Property runProp = PropertyFactory.createPropertyFromCssName(cssName, new DomCssValueAdaptor(cssValue));
        	
        	if (runProp!=null) {
	        	if (runProp instanceof AbstractRunProperty) {  
	        		((AbstractRunProperty)runProp).set(rPr);
	            	log.debug("added " + runProp.getClass().getName() );
	        	} else {
	            	//log.debug(p.getClass().getName() );
	        	}
        	}
        }
        
    	// An objective here is to avoid adding a run-level property which
    	// simply duplicates something which is already in the paragraph style,
    	// since such direct formatting is probably not the author's intent,
    	// and makes the document less maintainable
    	RPr styleRPr = null;
    	PropertyResolver propertyResolver = this.wordMLPackage.getMainDocumentPart().getPropertyResolver();
    	if (pStyleId!=null) {
    		
    		styleRPr = propertyResolver.getEffectiveRPr(pStyleId);
    		if (styleRPr!=null) {
    			RPrCleanser.removeRedundantProperties(styleRPr, rPr);
    		}
    		// Works nicely, except for color.  TODO: look into that
    	}
    	// Repeat the process for overlap with run level styles,
    	propertyResolver = this.wordMLPackage.getMainDocumentPart().getPropertyResolver();
    	if (rPr.getRStyle()!=null) {
    		
    		String styleId = rPr.getRStyle().getVal();
    		styleRPr = propertyResolver.getEffectiveRPr(styleId);
    		RPrCleanser.removeRedundantProperties(styleRPr, rPr);
    	}
    	
    	// TODO: cleansing in table context
    	
    }
    
    private String bookmarkNamePrefix = "";
    
    
    /**
     * The prefix (if any) to be added to bookmark names generated during this run.
     * Useful for preventing name collisions, when importing multiple fragments into
     * a single docx.
     * 
     * @param bookmarkNamePrefix
     */
    public void setBookmarkNamePrefix(String bookmarkNamePrefix) {
		this.bookmarkNamePrefix = bookmarkNamePrefix;
	}

	
	private Hyperlink createHyperlink(String url, RPr rPr, String linkText, RelationshipsPart rp) {
		
		// Handle XML predefined entities - escape them so we can unmarshall
		if (linkText.contains("&")
				&& !linkText.contains("&amp;") ) {
			linkText = linkText.replace("&", "&amp;");
		}
		if (linkText.contains("<") ) {
			linkText = linkText.replace("<", "&lt;");  // no need to worry about embedded elements (ie <b>) here since there can't be any - any such are added to the hyperlink later
		}
		if (linkText.contains(">") ) {
			linkText = linkText.replace(">", "&gt;");
		}		
		// No need for special handling for &apos; or &quot; 
		
		try {
			String hpl = null;
			
			if (url.startsWith("#")) { // Internal link --> w:anchor
				
				hpl = "<w:hyperlink w:anchor=\"" + bookmarkHelper.anchorToBookmarkName(bookmarkNamePrefix, url) + "\" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
			            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" >" +
			            "<w:r>" +
			            "<w:t>" + linkText + "</w:t>" +
			            "</w:r>" +
			            "</w:hyperlink>";				
				
				
			} else {                   // External link --> r:id

				// We need to add a relationship to word/_rels/document.xml.rels
				// but since its external, we don't use the 
				// usual wordMLPackage.getMainDocumentPart().addTargetPart
				// mechanism
				org.docx4j.relationships.ObjectFactory factory =
					new org.docx4j.relationships.ObjectFactory();
				
				org.docx4j.relationships.Relationship rel = factory.createRelationship();
				rel.setType( Namespaces.HYPERLINK  );
				rel.setTarget(url);
				if (log.isDebugEnabled()) {
					log.debug("target " + url);
				}
				rel.setTargetMode("External");  
										
				rp.addRelationship(rel);
				
				// addRelationship sets the rel's @Id
				
				hpl = "<w:hyperlink r:id=\"" + rel.getId() + "\" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
	            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" >" +
	            "<w:r>" +
	            "<w:t>" + linkText + "</w:t>" +
	            "</w:r>" +
	            "</w:hyperlink>";
			}

			Hyperlink hyperlink = (Hyperlink)XmlUtils.unmarshalString(hpl);
			R r = (R)hyperlink.getContent().get(0);
			r.setRPr(rPr);
			
			// Style the hyperlink with hyperlinkStyleId,
			// unless another style is already in use
			P currentP = getCurrentParagraph(false);
			
//			System.out.println("p/h:" + XmlUtils.marshaltoString(currentP));
			
			
//			if (currentP.getPPr()!=null
//					&& currentP.getPPr().getRPr()!=null
//					&& currentP.getPPr().getRPr().getRStyle()!=null) {
//				
//				// Respect p/ppr/rpr
//				
//			} else 
				
			if (rPr.getRStyle()==null // don't set it if its set already
					&& hyperlinkStyleId!=null) {
				RStyle rStyle = Context.getWmlObjectFactory().createRStyle();
				rStyle.setVal(hyperlinkStyleId);
				rPr.setRStyle(rStyle );
			}
			return hyperlink;
			
		} catch (Exception e) {
			// eg  org.xml.sax.SAXParseException: The reference to entity "ballot_id" must end with the ';' delimiter. 
			log.error("Dodgy link text: '" + linkText + "'", e);
			return null;
		}
		
		
	}

	
    private static String HR_XML =  "<w:p xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
    		+"<w:r>"
		    +"<w:pict>"
		    +"<v:rect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" id=\"_x0000_i1025\" style=\"width:0;height:1.5pt\" o:hralign=\"center\" o:hrstd=\"t\" o:hr=\"t\" fillcolor=\"#a0a0a0\" stroked=\"f\"/>"
		    +"</w:pict>"
		+"</w:r>"
		+"</w:p>";
    private static P HR_P = null;
    
    private P getPforHR() {
    	
    	if (HR_P==null) {
    		try {
				HR_P = (P)XmlUtils.unmarshalString(HR_XML);
			} catch (JAXBException e) {}
    	}
    	return HR_P;
    }

	

	public final static class TableProperties {
		
		private TableBox tableBox;
		
		public TableBox getTableBox() {
			return tableBox;
		}

		public void setTableBox(TableBox tableBox) {
			this.tableBox = tableBox;
			colPos = tableBox.getColumnPos();
		}
		
    	private int[] colPos; 
    	public int[] getColumnPos() {
    		return colPos;
    	}
		
    	public int getColumnWidth(int col) {
    		return colPos[col] - colPos[col-1];
    	}

		boolean isFixedWidth;

		public boolean isFixedWidth() {
			return isFixedWidth;
		}

		public void setFixedWidth(boolean isFixedWidth) {
			this.isFixedWidth = isFixedWidth;
		}
	}
    
    
}

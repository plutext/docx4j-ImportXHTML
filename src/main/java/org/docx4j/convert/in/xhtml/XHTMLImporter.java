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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;

import org.apache.commons.codec.binary.Base64;
import org.docx4j.wml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4j.UnitsOfMeasurement;
import org.docx4j.XmlUtils;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.model.PropertyResolver;
import org.docx4j.model.fields.FieldRef;
import org.docx4j.model.properties.Property;
import org.docx4j.model.properties.PropertyFactory;
import org.docx4j.model.properties.paragraph.AbstractParagraphProperty;
import org.docx4j.model.properties.paragraph.Indent;
import org.docx4j.model.properties.run.AbstractRunProperty;
import org.docx4j.model.properties.run.FontSize;
import org.docx4j.model.styles.StyleTree;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.exceptions.InvalidOperationException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.org.xhtmlrenderer.css.constants.CSSName;
import org.docx4j.org.xhtmlrenderer.css.constants.IdentValue;
import org.docx4j.org.xhtmlrenderer.css.parser.FSColor;
import org.docx4j.org.xhtmlrenderer.css.parser.FSRGBColor;
import org.docx4j.org.xhtmlrenderer.css.style.CalculatedStyle;
import org.docx4j.org.xhtmlrenderer.css.style.DerivedValue;
import org.docx4j.org.xhtmlrenderer.css.style.FSDerivedValue;
import org.docx4j.org.xhtmlrenderer.css.style.derived.LengthValue;
import org.docx4j.org.xhtmlrenderer.docx.Docx4JFSImage;
import org.docx4j.org.xhtmlrenderer.docx.Docx4jUserAgent;
import org.docx4j.org.xhtmlrenderer.docx.DocxRenderer;
import org.docx4j.org.xhtmlrenderer.layout.Styleable;
import org.docx4j.org.xhtmlrenderer.newtable.TableBox;
import org.docx4j.org.xhtmlrenderer.newtable.TableCellBox;
import org.docx4j.org.xhtmlrenderer.render.AnonymousBlockBox;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.docx4j.org.xhtmlrenderer.render.Box;
import org.docx4j.org.xhtmlrenderer.render.InlineBox;
import org.docx4j.org.xhtmlrenderer.resource.XMLResource;
import org.docx4j.wml.CTTblPrBase.TblStyle;
import org.docx4j.wml.DocDefaults.RPrDefault;
import org.docx4j.wml.P.Hyperlink;
import org.docx4j.wml.PPrBase.NumPr;
import org.docx4j.wml.PPrBase.NumPr.Ilvl;
import org.docx4j.wml.PPrBase.NumPr.NumId;
import org.docx4j.wml.PPrBase.PStyle;
import org.docx4j.wml.TcPrInner.GridSpan;
import org.docx4j.wml.TcPrInner.VMerge;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSValue;
import org.xml.sax.InputSource;

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
 * Includes rudimentary support for:
 * - paragraph and run formatting
 * - tables
 * - images
 * - lists (ordered, unordered)
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
public class XHTMLImporter {
	
	public static Logger log = LoggerFactory.getLogger(XHTMLImporter.class);		
	    
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
	 * Due to the architecture of this class, this is a static flag changing the
	 * behavior of all following calls.
	 * 
	 * @param hyperlinkStyleID
	 *            The style to use for hyperlinks (eg Hyperlink)
	 */
	public static void setHyperlinkStyle (
			String hyperlinkStyleID) {
		hyperlinkStyleId = hyperlinkStyleID;
	}
	private static String hyperlinkStyleId = null;	
	
    private Body imports = null; 
    
    
    private WordprocessingMLPackage wordMLPackage;
    private RelationshipsPart rp;
    private NumberingDefinitionsPart ndp;
    
    private ListHelper listHelper;
    
    private DocxRenderer renderer;
    
    private static FontFamilyMap fontFamilyToFont = new FontFamilyMap();
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
	 * @since 3.0
	 */
	public static void addFontMapping(String cssFontFamily, RFonts rFonts) {
		fontFamilyToFont.put(cssFontFamily, rFonts);
	}
	
	/**
	 * Case insensitive key
	 * (matching http://www.w3.org/TR/css3-fonts/#font-family-casing
	 */
	private static class FontFamilyMap extends HashMap<String, RFonts> {

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

	/**
	 * CLASS_TO_STYLE_ONLY: a Word style matching a class attribute will
	 * be used, and nothing else
	 * 
	 * CLASS_PLUS_OTHER: a Word style matching a class attribute will
	 * be used; other css will be translated to direct formatting
	 * 
	 * IGNORE_CLASS: css will be translated to direct formatting
	 *
	 */
	public enum FormattingOption {

		CLASS_TO_STYLE_ONLY, CLASS_PLUS_OTHER, IGNORE_CLASS;
	}

	/**
	 * @param runFormatting
	 *            the runFormatting to set
	 */
	public static void setRunFormatting(FormattingOption runFormatting) {
		XHTMLImporter.runFormatting = runFormatting;
	}
	private static FormattingOption runFormatting = FormattingOption.CLASS_PLUS_OTHER;

	/**
	 * @param paragraphFormatting
	 *            the paragraphFormatting to set
	 */
	public static void setParagraphFormatting(
			FormattingOption paragraphFormatting) {
		XHTMLImporter.paragraphFormatting = paragraphFormatting;
	}
	private static FormattingOption paragraphFormatting = FormattingOption.CLASS_PLUS_OTHER;

	/**
	 * @param tableFormatting the tableFormatting to set
	 */
	public static void setTableFormatting(FormattingOption tableFormatting) {
		XHTMLImporter.tableFormatting = tableFormatting;
	}
	private static FormattingOption tableFormatting = FormattingOption.CLASS_PLUS_OTHER;

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
	public static void setCssWhiteList(Set<String> cssWhiteList) {
		XHTMLImporter.cssWhiteList = cssWhiteList;
	}
	private static Set<String> cssWhiteList = null;


	private XHTMLImporter() {}
	
	private XHTMLImporter(WordprocessingMLPackage wordMLPackage) {
		
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

    	listHelper = new ListHelper(ndp);
    	
		if (hyperlinkStyleId !=null && wordMLPackage instanceof WordprocessingMLPackage) {
			((WordprocessingMLPackage)wordMLPackage).getMainDocumentPart().getPropertyResolver().activateStyle(hyperlinkStyleId);
		}
		
		initStyleMap(wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart());
		
		imports = Context.getWmlObjectFactory().createBody();
		contentContextStack.push(imports);
    }
	
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
     * Convert the well formed XHTML contained in file to a list of WML objects.
     * 
     * @param file
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public static List<Object> convert(File file, String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {

        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);

        importer.renderer = new DocxRenderer();
        
        File parent = file.getAbsoluteFile().getParentFile();
        
        try {
			importer.renderer.setDocument(
					importer.renderer.loadDocument(file.toURI().toURL().toExternalForm()),
			        (parent == null ? "" : parent.toURI().toURL().toExternalForm())
			);
		} catch (MalformedURLException e) {
			throw new Docx4JException("Malformed URL", e);
		}

        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(), null);
        
        return importer.imports.getContent();    	
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
    public static List<Object> convert(InputSource is,  String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {

        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);

        importer.renderer = new DocxRenderer();
        
        Document dom = XMLResource.load(is).getDocument();        
        importer.renderer.setDocument(dom, baseUrl);
        
        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(),  null);
        
        return importer.imports.getContent();    	
    }

    /**
     * @param is
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public static List<Object> convert(InputStream is, String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {
        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);
    	
        importer.renderer = new DocxRenderer();
        
        Document dom = XMLResource.load(is).getDocument();        
        importer.renderer.setDocument(dom, baseUrl);

        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(), null);
        
        return importer.imports.getContent();    	
    }
    
    /**
     * @param node
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public static List<Object> convert(Node node,  String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {
        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);
    	
        importer.renderer = new DocxRenderer();
        if (node instanceof Document) {
        	importer.renderer.setDocument( (Document)node, baseUrl );
        } else {
        	Document doc = XmlUtils.neww3cDomDocument();
        	doc.importNode(node, true);
        	importer.renderer.setDocument( doc, baseUrl );
        }
        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(),  null);
        
        return importer.imports.getContent();    	
    }
    
    /**
     * @param reader
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public static List<Object> convert(Reader reader,  String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {
        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);
    	
        importer.renderer = new DocxRenderer();
        
        Document dom = XMLResource.load(reader).getDocument();        
        importer.renderer.setDocument(dom, baseUrl);
        
        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(),  null);
        
        return importer.imports.getContent();    	
    }
    
    /**
     * @param source
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public static List<Object> convert(Source source,  String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {
    	
        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);
    	
        importer.renderer = new DocxRenderer();
                
        Document dom = XMLResource.load(source).getDocument();        
        importer.renderer.setDocument(dom, baseUrl);

        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(),  null);
        
        return importer.imports.getContent();    	
    }
    
    //public static List<Object> convert(XMLEventReader reader, WordprocessingMLPackage wordMLPackage) throws IOException {
    //public static List<Object> convert(XMLStreamReader reader, WordprocessingMLPackage wordMLPackage) throws IOException {
    
    /**
     * Convert the well formed XHTML found at the specified URI to a list of WML objects.
     * 
     * @param url
     * @param wordMLPackage
     * @return
     */
    public static List<Object> convert(URL url, WordprocessingMLPackage wordMLPackage) throws Docx4JException {

        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);
    	
        importer.renderer = new DocxRenderer();
        
        String urlString = url.toString();
        Document dom =importer.renderer.loadDocument(urlString);
        importer.renderer.setDocument(dom, urlString);
        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(),  null);
        
        return importer.imports.getContent();    	
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
    public static List<Object> convert(String content,  String baseUrl, WordprocessingMLPackage wordMLPackage) throws Docx4JException {
    	
        XHTMLImporter importer = new XHTMLImporter(wordMLPackage);

        importer.renderer = new DocxRenderer();
        
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        
        Document dom;
        try {
        	dom = XMLResource.load(is).getDocument();
        } catch  ( org.docx4j.org.xhtmlrenderer.util.XRRuntimeException xre) {
        	// javax.xml.transform.TransformerException te
        	Throwable t = xre.getCause();
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
        
        
        importer.renderer.setDocument(dom, baseUrl);
        importer.renderer.layout();
                    
        importer.traverse(importer.renderer.getRootBox(),  null);
        
        return importer.imports.getContent();    	
    }
    
    
    private Map<String, CSSValue> getCascadedProperties(CalculatedStyle cs) {
    	
    	Map<String, CSSValue> cssMap = new HashMap<String, CSSValue>();
    	
    	FSDerivedValue[] derivedValues = cs.getDerivedValues();
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
            
            if (val != null && val instanceof DerivedValue) {    
            	
            	cssMap.put(name.toString(), ((DerivedValue)val).getCSSPrimitiveValue() );
            	
            } else if (val != null && val instanceof IdentValue) {
            	
            	cssMap.put(name.toString(), ((IdentValue)val).getCSSPrimitiveValue() );

            } else if (val != null && val instanceof LengthValue) {
            	
            	cssMap.put(name.toString(), ((LengthValue)val).getCSSPrimitiveValue() );
            	
            } else  if (val!=null ) {
            	
//            	log.debug("Skipping " +  name.toString() + " .. " + val.getClass().getName() );
            } else {
//            	log.debug("Skipping " +  name.toString() + " .. (null value)" );            	
            }
        }
    	
        return cssMap;
    	
    }

    
    
    /**
     * The Block level elements that our content may go into, ie
     * Body, Table, Tr, Td.
     * 
     *  P and P.Hyperlink are NOT added to contentContextStack.
     */
    private LinkedList<ContentAccessor> contentContextStack = new LinkedList<ContentAccessor>();
    
    private void pushBlockStack(ContentAccessor ca) {
    	contentContextStack.push(ca);
    	attachmentPointP = null;
    }
    private ContentAccessor popBlockStack() {
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
			P newP = Context.getWmlObjectFactory().createP();
			attachmentPointP = newP;
			this.contentContextStack.peek().getContent().add(newP);
            paraStillEmpty = true;
            return newP;
    	} else {
    		return null;
    	}
    }
    
    private ContentAccessor getListForRun() {
    	
    	if (attachmentPointH!=null) return attachmentPointH;
    	return getCurrentParagraph(true);
    }
    
    // A paragraph created for a div can be replaced by
    // one created for a p within it, if it is still empty
    // TODO revisit this
    boolean paraStillEmpty;

    
    private void traverse(Box box, TableProperties tableProperties) throws Docx4JException {
    	setDefaultFontSize();
    	traverse( box, null,  tableProperties);
    	unsetDefaultFontSize();
    }    
    
    private void traverse(Box box,  Box parent, TableProperties tableProperties) throws Docx4JException {
        
    	boolean mustPop = false;
    	
        log.debug(box.getClass().getName() );
        if (box instanceof BlockBox) {
        	        	
            BlockBox blockBox = ((BlockBox)box);

            Element e = box.getElement(); 

            // Don't add a new paragraph if this BlockBox is display: inline
            if (e==null) {
            	// Shouldn't happen
                log.debug("<NULL>");
            } else {            
                log.debug("BB"  + "<" + e.getNodeName() + " " + box.getStyle().toStringMine() );
                log.debug(box.getStyle().getDisplayMine() );
//                log.debug(box.getElement().getAttribute("class"));
                
                
            	//Map cssMap = styleReference.getCascadedPropertiesMap(e);
                Map<String, CSSValue> cssMap = getCascadedProperties(box.getStyle());
            	
            	/* Sometimes, when it is display: inline, the following is not set:
	            	CSSValue cssValue = (CSSValue)cssMap.get("display");
	            	if (cssValue !=null) {
	            		log.debug(cssValue.getCssText() );
	            	}
	            */
            	// So do it this way ...
            	if (box.getStyle().getDisplayMine().equals("inline") ) {
            		
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
                	
            	} else if (box instanceof org.docx4j.org.xhtmlrenderer.newtable.TableSectionBox) {
                	// nb, both TableBox and TableSectionBox 
                	// have node name 'table' (or can have),
            		// so this else clause is before the TableBox one,
            		// to avoid a class cast exception
            		
            		// eg <tbody color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; border-collapse: collapse; -fs-border-spacing-horizontal: 0; -fs-border-spacing-vertical: 0; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table-row-group; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: static; ; right: auto; src: none; table-layout: auto; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: middle; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: none; border-right-style: none; border-bottom-style: none; border-left-style: none; border-top-width: 2px; border-right-width: 2px; border-bottom-width: 2px; border-left-width: 2px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0; 
            		log.debug(".. processing <tbody");
            		
            		// Do nothing here for now .. the switch statement below traverses children
            		
            		// TODO: give effect to this CSS

            	} else if (box instanceof org.docx4j.org.xhtmlrenderer.newtable.TableBox)  {
                	
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
            		 * 
            		 * 

            		 * 
            		 */

            		org.docx4j.org.xhtmlrenderer.newtable.TableBox cssTable = (org.docx4j.org.xhtmlrenderer.newtable.TableBox)box;
            		
            		tableProperties = new TableProperties();
            		tableProperties.setTableBox(cssTable);

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
            		nestedTableHierarchyFix(contentContext,parent);
            		
            		Tbl tbl = Context.getWmlObjectFactory().createTbl();
            		contentContext.getContent().add(tbl);
		            paraStillEmpty = true;
//		            contentContext = tbl;
		            pushBlockStack(tbl);
		            mustPop = true;
		            
            		TblPr tblPr = Context.getWmlObjectFactory().createTblPr();
            		tbl.setTblPr(tblPr);    

                    String cssClass = null;
                	if (e.getAttribute("class")!=null) {
                	 	cssClass=e.getAttribute("class").trim();
                	}
                    setTableStyle(tblPr, cssClass, "TableGrid");
            		
            		
					// table borders
					TblBorders borders = Context.getWmlObjectFactory().createTblBorders();
					borders.setTop( copyBorderStyle(cssTable, "top", true) );
					borders.setBottom( copyBorderStyle(cssTable, "bottom", true) );
					borders.setLeft( copyBorderStyle(cssTable, "left", true) );
					borders.setRight( copyBorderStyle(cssTable, "right", true) );
					borders.setInsideH( createBorderStyle(STBorder.NONE, null, null) );
					borders.setInsideV( createBorderStyle(STBorder.NONE, null, null) );
					tblPr.setTblBorders(borders);

					TblWidth spacingWidth = Context.getWmlObjectFactory().createTblWidth();
					if(cssTable.getStyle().isCollapseBorders()) {
						spacingWidth.setW(BigInteger.ZERO);
						spacingWidth.setType(TblWidth.TYPE_AUTO);
					} else {
						int cssSpacing = cssTable.getStyle().getBorderHSpacing(renderer.getLayoutContext());
						spacingWidth.setW( BigInteger.valueOf(cssSpacing  / 2) );	// appears twice thicker, probably taken from both sides 
						spacingWidth.setType(TblWidth.TYPE_DXA);
					}
					tblPr.setTblCellSpacing(spacingWidth); 
            		
            		// Table indent.  
            		// cssTable.getLeftMBP() which is setLeftMBP((int) margin.left() + (int) border.left() + (int) padding.left());
            		// cssTable.getTx(); which is (int) margin.left() + (int) border.left() + (int) padding.left();
            		// But want just margin.left
            		if (cssTable.getMargin() !=null
            				&& cssTable.getMargin().left()>0) {
            			log.debug("Calculating TblInd from margin.left: " + cssTable.getMargin().left() );
                		TblWidth tblIW = Context.getWmlObjectFactory().createTblWidth();
                		tblIW.setW( BigInteger.valueOf( Math.round(
                				cssTable.getMargin().left()
                				)));
                		tblIW.setType(TblWidth.TYPE_DXA);
            			tblPr.setTblInd(tblIW);
            		} else {
            		
	            		// Indent is zero.  In this case, if the table has borders,
	            		// adjust the indent to align the left border with the left edge of text outside the table
	            		// See http://superuser.com/questions/126451/changing-the-placement-of-the-left-border-of-tables-in-word
            			CTBorder leftBorder = borders.getLeft();
            			if (leftBorder!=null
            					&& leftBorder.getVal()!=null
            					&& leftBorder.getVal()!=STBorder.NONE
            					&& leftBorder.getVal()!=STBorder.NIL) {
            				// set table indent to .08", ie 115 twip
            				// <w:tblInd w:w="115" w:type="dxa"/>
            				// TODO For a wider line, or a line style which is eg double lines, you might need more indent
            				log.debug("applying fix to align left edge of table with text");
                    		TblWidth tblIW = Context.getWmlObjectFactory().createTblWidth();
                    		tblIW.setW( BigInteger.valueOf( 115));
                    		tblIW.setType(TblWidth.TYPE_DXA);
                			tblPr.setTblInd(tblIW);
            			}
            			
            		}
            			
            		// <w:tblW w:w="0" w:type="auto"/>
            		// for both fixed width and auto fit tables.
            		// You'd only set it to something else
            		// eg <w:tblW w:w="5670" w:type="dxa"/>
            		// for what in Word corresponds to 
            		// "Preferred width".  TODO: decide what CSS
            		// requires that.
            		TblWidth tblW = Context.getWmlObjectFactory().createTblWidth();
            		tblW.setW(BigInteger.ZERO);
            		tblW.setType(TblWidth.TYPE_AUTO);
            		tblPr.setTblW(tblW);
            		
	            	if (cssTable.getStyle().isIdent(CSSName.TABLE_LAYOUT, IdentValue.AUTO) 
	            			|| cssTable.getStyle().isAutoWidth()) {
	            		// Conditions under which FS creates AutoTableLayout
	            		
	            		tableProperties.setFixedWidth(false);
	            		
	            		// This is the default, so no need to set 
	            		// STTblLayoutType.AUTOFIT
	            		
	                } else {
	            		// FS creates FixedTableLayout
	            		tableProperties.setFixedWidth(true);
	            		
	            		// <w:tblLayout w:type="fixed"/>
	            		CTTblLayoutType tblLayout = Context.getWmlObjectFactory().createCTTblLayoutType();
	            		tblLayout.setType(STTblLayoutType.FIXED);
	            		tblPr.setTblLayout(tblLayout);
	                }		            	
		            
	            	// Word can generally open a table without tblGrid:
	                // <w:tblGrid>
	                //  <w:gridCol w:w="4621"/>
	                //  <w:gridCol w:w="4621"/>
	                // </w:tblGrid>
	            	// but for an AutoFit table (most common), it 
	            	// is the w:gridCol val which prob specifies the actual width
	            	TblGrid tblGrid = Context.getWmlObjectFactory().createTblGrid();
	            	tbl.setTblGrid(tblGrid);
	            	
	            	int[] colPos = tableProperties.getColumnPos();
	            	
	            	for (int i=1; i<=cssTable.numEffCols(); i++) {
	            		
	            		TblGridCol tblGridCol = Context.getWmlObjectFactory().createTblGridCol();
	            		tblGrid.getGridCol().add(tblGridCol);
	            		
	            		log.debug("colpos=" + colPos[i]);
	            		tblGridCol.setW( BigInteger.valueOf(colPos[i]-colPos[i-1]) );
	            		
	            	}
	            	
            	} else if (e.getNodeName().equals("table") ) {
            		// but not instanceof org.docx4j.org.xhtmlrenderer.newtable.TableBox
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
            		nestedTableHierarchyFix(contentContext,parent);
            		
            		Tbl tbl = Context.getWmlObjectFactory().createTbl();
            		contentContext.getContent().add(tbl);
		            paraStillEmpty = true;
		            pushBlockStack(tbl);
		            mustPop = true;
		            
            		
            	} else if (box instanceof org.docx4j.org.xhtmlrenderer.newtable.TableRowBox) {
            		
            		// eg <tr color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; border-collapse: collapse; -fs-border-spacing-horizontal: 0; -fs-border-spacing-vertical: 0; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table-row; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: static; ; right: auto; src: none; table-layout: auto; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: top; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: none; border-right-style: none; border-bottom-style: none; border-left-style: none; border-top-width: 2px; border-right-width: 2px; border-bottom-width: 2px; border-left-width: 2px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0;
            		
            		// TODO support vertical-align
            		
            		log.debug(".. processing <tr");            		

//            		if (this.contentContextStack.peek() instanceof Tr) {
//            			this.contentContextStack.pop();
//            		} 
            		
            		Tr tr = Context.getWmlObjectFactory().createTr();
            		this.contentContextStack.peek().getContent().add(tr);
		            paraStillEmpty = true;
		            pushBlockStack(tr);
		            mustPop = true;
            		
            		
            	} else if (box instanceof org.docx4j.org.xhtmlrenderer.newtable.TableCellBox) {
            		            		
            		log.debug(".. processing <td");            		
            		// eg <td color: #000000; background-color: transparent; background-image: none; background-repeat: repeat; background-attachment: scroll; background-position: [0%, 0%]; background-size: [auto, auto]; border-collapse: collapse; -fs-border-spacing-horizontal: 0; -fs-border-spacing-vertical: 0; -fs-font-metric-src: none; -fs-keep-with-inline: auto; -fs-page-width: auto; -fs-page-height: auto; -fs-page-sequence: auto; -fs-pdf-font-embed: auto; -fs-pdf-font-encoding: Cp1252; -fs-page-orientation: auto; -fs-table-paginate: auto; -fs-text-decoration-extent: line; bottom: auto; caption-side: top; clear: none; ; content: normal; counter-increment: none; counter-reset: none; cursor: auto; ; display: table-row; empty-cells: show; float: none; font-style: normal; font-variant: normal; font-weight: normal; font-size: medium; line-height: normal; font-family: serif; -fs-table-cell-colspan: 1; -fs-table-cell-rowspan: 1; height: auto; left: auto; letter-spacing: normal; list-style-type: disc; list-style-position: outside; list-style-image: none; max-height: none; max-width: none; min-height: 0; min-width: 0; orphans: 2; ; ; ; overflow: visible; page: auto; page-break-after: auto; page-break-before: auto; page-break-inside: auto; position: static; ; right: auto; src: none; table-layout: auto; text-align: left; text-decoration: none; text-indent: 0; text-transform: none; top: auto; ; vertical-align: top; visibility: visible; white-space: normal; word-wrap: normal; widows: 2; width: auto; word-spacing: normal; z-index: auto; border-top-color: #000000; border-right-color: #000000; border-bottom-color: #000000; border-left-color: #000000; border-top-style: none; border-right-style: none; border-bottom-style: none; border-left-style: none; border-top-width: 2px; border-right-width: 2px; border-bottom-width: 2px; border-left-width: 2px; margin-top: 0; margin-right: 0; margin-bottom: 0; margin-left: 0; padding-top: 0; padding-right: 0; padding-bottom: 0; padding-left: 0;

            		ContentAccessor trContext = contentContextStack.peek();
            		org.docx4j.org.xhtmlrenderer.newtable.TableCellBox tcb = (org.docx4j.org.xhtmlrenderer.newtable.TableCellBox)box;
            		// tcb.getVerticalAlign()
            		
            		
            		
            		// rowspan support: vertically merged cells are
            		// represented as a top cell containing the actual content with a vMerge tag with "restart" attribute 
            		// and a series of dummy cells having a vMerge tag with no (or "continue") attribute.            		
            		            		
					// if current cell is the first real cell in the row, but is not in the leftmost position, then
					// search for vertically spanned cells to the left and insert dummy cells before current
					if (tcb.getParent().getChild(0) == tcb && tcb.getCol() > 0) {
						insertDummyVMergedCells(trContext, tcb, true);
					}

					int effCol = tcb.getTable().colToEffCol(tcb.getCol());
            		
                    // The cell proper
//					if (this.contentContextStack.peek() instanceof Tc) {
//            			popStack();
//					}
					Tc tc = Context.getWmlObjectFactory().createTc();
            		contentContextStack.peek().getContent().add(tc);
            		pushBlockStack(tc);//.getContent();
		            mustPop = true;

            		// if the td contains bare text (eg <td>apple</td>)
            		// we need a p for it
//            		currentP = Context.getWmlObjectFactory().createP();                                        	
//    	            contentContext.getContent().add(currentP);            		
//		            paraStillEmpty = true;
            		
            		// Do we need a vMerge tag with "restart" attribute?
            		// get cell below (only 1 section supported at present)
            		TcPr tcPr = Context.getWmlObjectFactory().createTcPr();
        			tc.setTcPr(tcPr);
                    if (tcb.getStyle().getRowSpan()> 1) {
            			
            			VMerge vm = Context.getWmlObjectFactory().createTcPrInnerVMerge();
            			vm.setVal("restart");
            			tcPr.setVMerge(vm);            
                    }
                    // eg <w:tcW w:w="2268" w:type="dxa"/>
                    try {
	            		TblWidth tblW = Context.getWmlObjectFactory().createTblWidth();
	            		tblW.setW(BigInteger.valueOf(tableProperties.getColumnWidth(effCol+1) ));
	            		tblW.setType(TblWidth.TYPE_DXA);
	            		tcPr.setTcW(tblW);    	                    
                    } catch (java.lang.ArrayIndexOutOfBoundsException aioob) {
                    	// happens with http://en.wikipedia.org/wiki/Office_Open_XML
                    	log.error("Problem with getColumnWidth for col" + (effCol+1) );
                    }
/*                  The below works, but the above formulation is simpler
 * 
 * 					int r = tcb.getRow() + tcb.getStyle().getRowSpan() - 1;
                    if (r < tcb.getSection().numRows() - 1) {
                        // The cell is not in the last row, so use the next row in the
                        // section.
                        TableCellBox belowCell = section.cellAt( r + 1, effCol);
	                    log.debug("Got belowCell for " + tcb.getRow() + ", " + tcb.getCol() );
	                    log.debug("it is  " + belowCell.getRow() + ", " + belowCell.getCol() );
                        if (belowCell.getRow() > tcb.getRow() + 1 ) {
	                		TcPr tcPr = Context.getWmlObjectFactory().createTcPr();
	            			tc.setTcPr(tcPr);
	            			
	            			VMerge vm = Context.getWmlObjectFactory().createTcPrInnerVMerge();
	            			vm.setVal("restart");
	            			tcPr.setVMerge(vm);                        	
                        }
                    } 
 */            		
            		// colspan support: horizontally merged cells are represented by one cell
            		// with a gridSpan attribute; 
            		int colspan = tcb.getStyle().getColSpan(); 
            		if (colspan>1) {
            			
						TcPr tcPr2 = tc.getTcPr();
						if (tcPr2 == null) {
							tcPr2 = Context.getWmlObjectFactory().createTcPr();
							tc.setTcPr(tcPr2);
						}

            			GridSpan gs = Context.getWmlObjectFactory().createTcPrInnerGridSpan();
            			gs.setVal( BigInteger.valueOf(colspan));
            			tcPr2.setGridSpan(gs);
            			
            			this.setCellWidthAuto(tcPr2);            			
            		}
            		
            		// BackgroundColor
            		FSColor fsColor = tcb.getStyle().getBackgroundColor();
            		if (fsColor != null
            				&& fsColor instanceof FSRGBColor) {
           				
            				FSRGBColor rgbResult = (FSRGBColor)fsColor;
            				CTShd shd = Context.getWmlObjectFactory().createCTShd();
            				shd.setFill(
            						UnitsOfMeasurement.rgbTripleToHex(rgbResult.getRed(), rgbResult.getGreen(), rgbResult.getBlue())  );
            				tcPr.setShd(shd);
            		}
					
					// cell borders
					tcPr.setTcBorders( copyCellBorderStyles(tcb) );
					
            		
					// search for vertically spanned cells to the right from current, and insert dummy cells after it
					insertDummyVMergedCells(trContext, tcb, false);

            	} else if (isListItem(blockBox.getElement())) {

		            // Paragraph level styling
	            	P currentP = this.getCurrentParagraph(true);
	            	
	                PPr pPr =  Context.getWmlObjectFactory().createPPr();
	                currentP.setPPr(pPr);
	            	
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
			            			 */
			            			BigInteger numId = s.getPPr().getNumPr().getNumId().getVal();
			            			listHelper.setNumbering(pPr, numId);
			            			
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
			            			log.debug("For docx style for @class='" + cssClass + "', but its not a paragraph style ");
			            			
			            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
			    	            		listHelper.addNumbering(this.getCurrentParagraph(true), blockBox.getElement(), cssMap);
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
            		
	            } else {
	            	
	            	// Paragraph processing
	            	
	            	// Avoid creating paragraphs for html, body
//	            	if (contentContext.getContent().size()>0 && paraStillEmpty) {
//			            contentContext.getContent().remove( contentContext.getContent().size()-1);                                        		
//	            	} 
	            	
		            // Paragraph level styling
	            	P currentP = this.getCurrentParagraph(true);
	            	
	                PPr pPr =  Context.getWmlObjectFactory().createPPr();
	                currentP.setPPr(pPr);
	            	
	                if (paragraphFormatting.equals(FormattingOption.IGNORE_CLASS)) {
	            		addParagraphProperties(pPr, blockBox, cssMap );
	                } else {
	                	// CLASS_TO_STYLE_ONLY or CLASS_PLUS_OTHER
		            	if (box.getElement()!=null
		            			&& box.getElement().getAttribute("class")!=null) {
		            		
		            		String cssClass = box.getElement().getAttribute("class").trim();
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
			            		} else if (s.getType()!=null && s.getType().equals("paragraph")) {
			            			PStyle pStyle = Context.getWmlObjectFactory().createPPrBasePStyle();
			            			pPr.setPStyle(pStyle);
			            			pStyle.setVal(cssClass);
			            		} else {
			            			log.debug("For docx style for @class='" + cssClass + "', but its not a paragraph style ");
			            		}
		            		}
		            	}
            			if (paragraphFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
            				addParagraphProperties(pPr, blockBox, cssMap );
            			}
		            	
	            	} 
		            
//		            if (e.getNodeName().equals("li")) {
//		            	addNumbering(e, cssMap);
//		            } else 
		            	if  (e.getNodeName().equals("img")) {
		        		// TODO, should we be using ReplacedElementFactory approach instead?		
		            	
		            	addImage(blockBox);
		            }
		            
	            }
        	}
            
            // the recursive bit:
            

//        	if (contentContext instanceof Body) {
//        		currentP = Context.getWmlObjectFactory().createP();                                        	
//        		contentContext.getContent().add(currentP);            		
//	            paraStillEmpty = true;	
//	            contentContext = currentP;
//	            contentContextStack.push(currentP);
//        	}

        	
            	log.debug("Processing children of " + box.getElement().getNodeName() );
	            switch (blockBox.getChildrenContentType()) {
	                case BlockBox.CONTENT_BLOCK:
	                	log.debug(".. which are BlockBox.CONTENT_BLOCK");	                	
	                    for (Object o : ((BlockBox)box).getChildren() ) {
	                        log.debug("   processing child " + o.getClass().getName() );
	                    	
	                        traverse((Box)o,  box, tableProperties);                    
	                        log.debug(".. processed child " + o.getClass().getName() );
	                    }
	                    break;
	                case BlockBox.CONTENT_INLINE:
	                	
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
	                        
	                    }
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

            // new P
            attachmentPointP = null; 
            
            if (mustPop) popBlockStack();
            	
//            // An empty tc shouldn't make the table disappear!
//            // TODO - make more elegant
//            if (e.getNodeName().equals("table")) {            	
//            	paraStillEmpty = false;
//            }
//
//            
            
        } else if (box instanceof AnonymousBlockBox) {
            log.debug("AnonymousBlockBox");            
        }
    
    }

	/**
	 * Table borders support
	 * @param box table or cell to copy css border properties from
	 * @param side "top"/"bottom"/"left"/"right"
	 * @param keepNone if true, then missed borders returned as border with style NONE (for tables), else as null (for cells) 
	 * @return reproduced border style
	 */
	private CTBorder copyBorderStyle(Box box, String side, boolean keepNone) {
		FSDerivedValue borderStyle = box.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-style") );
		FSDerivedValue borderColor = box.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-color") );
		float width = box.getStyle().getFloatPropertyProportionalHeight(
				CSSName.getByPropertyName("border-"+side+"-width"), 0, renderer.getLayoutContext() );

		// zero-width border still drawn as "hairline", so remove it too
		if(borderStyle.asIdentValue() == IdentValue.NONE || width == 0.0f) {
			// a table have default borders which we need to disable explicitly, 
			// while a cell with no own border can obtain a border from the table or other cell and shouldn't overwrite it
			return keepNone ? createBorderStyle(STBorder.NONE, null, null) : null;
		}

		// there is a special style for such an overwrite
		if(borderStyle.asIdentValue() == IdentValue.HIDDEN) {
			return createBorderStyle(STBorder.NONE, "FFFFFF", BigInteger.ZERO);
		}
		
		// double border width in html is applied to the whole border, while the word applying it to each bar and the gap in between 
		if(borderStyle.asIdentValue() == IdentValue.DOUBLE) {
			width /= 3;
		}

		STBorder stBorder;
		try {
			stBorder = STBorder.fromValue( borderStyle.asString() );
		} catch (IllegalArgumentException e) {
			stBorder = STBorder.SINGLE; 
		}

		// w:ST_EighthPointMeasure - Measurement in Eighths of a Point
		width = UnitsOfMeasurement.twipToPoint( Math.round(width) ) * 8.0f;
		
		return createBorderStyle( stBorder, borderColor.asString(), BigInteger.valueOf( Math.round(width) ) );
	}

	private CTBorder createBorderStyle(STBorder val, String color, BigInteger sz) {
		CTBorder border = Context.getWmlObjectFactory().createCTBorder();
		border.setVal(val);
		border.setColor(color);
		border.setSz(sz);
		return border;
	}

	private TcPrInner.TcBorders copyCellBorderStyles(TableCellBox box) {
		TcPrInner.TcBorders tcBorders = Context.getWmlObjectFactory().createTcPrInnerTcBorders();
		tcBorders.setTop( copyBorderStyle(box, "top", false) );
		tcBorders.setBottom( copyBorderStyle(box, "bottom", false) );
		tcBorders.setLeft( copyBorderStyle(box, "left", false) );
		tcBorders.setRight( copyBorderStyle(box, "right", false) );
		return tcBorders;
	}

	/**
	 * Rowspan and colspan support.
	 * Search for lower parts of vertically merged cells, adjacent to current cell in given direction.
	 * Then insert the appropriate number of dummy cells, with the same horizontal merging as in their top parts into row context.
	 * @param trContext context of the row to insert dummies into
	 * @param tcb current cell
	 * @param backwards direction flag: if true, then scan to the left
	 */
	private void insertDummyVMergedCells(ContentAccessor trContext, TableCellBox tcb, boolean backwards) {

		log.debug("Scanning cells from " + tcb.getRow() + ", " + tcb.getCol() + " to the " + (backwards ? "left" : "right") );

		ArrayList<TableCellBox> adjCells = new ArrayList<TableCellBox>();
		int numEffCols = tcb.getTable().numEffCols();

		for ( int i = tcb.getCol(); i >= 0 && i < numEffCols; i += backwards ? -1 : 1 ) {

			TableCellBox adjCell = tcb.getSection().cellAt(tcb.getRow(), i);

			if ( adjCell == null ) {
				// Check your table is OK
				log.error("XHTML table import: Null adjCell for row " + tcb.getRow() + ", col " + tcb.getCol() + " at col " + i);
				break;
			}
			if ( adjCell == tcb || adjCell == TableCellBox.SPANNING_CELL ) {
				continue;
			}
			log.debug("Got adjCell, it is  " + adjCell.getRow() + ", " + adjCell.getCol());

			if ( adjCell.getRow() < tcb.getRow()
					&& adjCell.getStyle()!=null
					&& adjCell.getStyle().getRowSpan()>1 ) {
				// eg tcb is r2,c1 & adjCell is r1,c0
				adjCells.add(adjCell);
			} else {
				break;
			}
		}

		if ( backwards && !adjCells.isEmpty() ) {
			Collections.reverse(adjCells);
		}

		for (TableCellBox adjCell : adjCells) {
			Tc dummy = Context.getWmlObjectFactory().createTc();
			trContext.getContent().add(dummy);

			TcPr tcPr = Context.getWmlObjectFactory().createTcPr();
			dummy.setTcPr(tcPr);

			VMerge vm = Context.getWmlObjectFactory().createTcPrInnerVMerge();
			//vm.setVal("continue");
			tcPr.setVMerge(vm);

			int colspan = adjCell.getStyle().getColSpan();
			if (colspan > 1) {
				GridSpan gs = Context.getWmlObjectFactory().createTcPrInnerGridSpan();
				gs.setVal( BigInteger.valueOf(colspan));
				tcPr.setGridSpan(gs);
			}

			TcPrInner.TcBorders borders = copyCellBorderStyles(adjCell);
			borders.setTop( createBorderStyle(STBorder.NIL, null, null) );
			tcPr.setTcBorders(borders);

			this.setCellWidthAuto(tcPr);

			// Must have an empty w:p
			dummy.getContent().add( new P() );
		}
	}


	/**
	 * nested tables XHTML renderer seems to construct a tree: table/table
	 * instead of table/tr/td/table?
	 * TODO fix this upstream.
	 * TestCase is http://en.wikipedia.org/wiki/Office_Open_XML
	 * 
	 * @param contentContext
	 * @param parent
	 * @return
	 */
	private void nestedTableHierarchyFix(ContentAccessor contentContext,
			Box parent) {
		
		if (parent==null) return; // where importing a table fragment 
		
		if (parent instanceof TableBox
				|| parent.getElement().getNodeName().equals("table") ) {
			log.warn("table: Constructing missing w:tr/w:td..");
			
			//if table was with caption move P (generated for caption) to nested column
			P captionP = null;
//			Iterator<Object> contentIterator = contentContext.iterator();
//			Object next;
//			while(contentIterator.hasNext()){
//			    next = contentIterator.next();
//			    if(next instanceof P){
//			        captionP = (P)XmlUtils.deepCopy((P)next);
//			        contentIterator.remove();
//			        break;
//			    }
//			}
			Object next;
			for (int i=0; i<contentContext.getContent().size(); i++) {
			    next = contentContext.getContent().get(i);
			    if(next instanceof P){
			        captionP = (P)XmlUtils.deepCopy((P)next);
			        contentContext.getContent().remove(i);
			        break;
			    }
			}
			
			TblPr tblPr = Context.getWmlObjectFactory().createTblPr();
            contentContext.getContent().add(tblPr);
            
            String cssClass = null;
        	if (parent.getElement().getAttribute("class")!=null) {
        	 	cssClass=parent.getElement().getAttribute("class").trim();
        	}
            
            setTableStyle(tblPr, cssClass, "none");
			
			Tr tr = Context.getWmlObjectFactory().createTr();
			contentContext.getContent().add(tr);
		    contentContext = tr;            			
			
			Tc tc = Context.getWmlObjectFactory().createTc();
			contentContext.getContent().add(tc);
		    contentContext = tc;
		    
		    //if caption was found add it
		    if(captionP != null){
		        contentContext.getContent().add(captionP);
		    }
		}
//		return contentContext;
	}
	
	private void setTableStyle(TblPr tblPr, String cssClass, String fallbackStyle) {

        TblStyle tblStyle = Context.getWmlObjectFactory().createCTTblPrBaseTblStyle();
        tblPr.setTblStyle(tblStyle);
		
        if (tableFormatting.equals(FormattingOption.IGNORE_CLASS)) {
            tblStyle.setVal(fallbackStyle);
        } else {
        	// CLASS_TO_STYLE_ONLY or CLASS_PLUS_OTHER
        	if (cssClass==null) {
        		// Word 2010 can't open a docx which contains <w:tblStyle/>
        		// so we need to either remove the tblStyle element, 
        		// or 
                tblStyle.setVal(fallbackStyle);
        	} else {
//        	if (box.getElement()!=null
//        			&& box.getElement().getAttribute("class")!=null) {
//        		String cssClass = box.getElement().getAttribute("class").trim();
        		
        		if (cssClass.equals("")) {
                    tblStyle.setVal(fallbackStyle);
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
                        tblStyle.setVal(fallbackStyle);
            		} else if (s.getType()!=null && s.getType().equals("table")) {
                        tblStyle.setVal(cssClass);
            		} else {
            			log.debug("For docx style for @class='" + cssClass + "', but its not a character style ");
                        tblStyle.setVal(fallbackStyle);
            		}
        		}
        	}
//			if (tableFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
//				addRunProperties(rPr, cssMap );
//			}
        	
    	} 
		
	}
    
    private void setCellWidthAuto(TcPr tcPr) {
    	// <w:tcW w:w="0" w:type="auto"/>
		TblWidth tblW = Context.getWmlObjectFactory().createTblWidth();
		tblW.setW(BigInteger.ZERO);
		tblW.setType(TblWidth.TYPE_AUTO);
		tcPr.setTcW(tblW);    	
    }

    
    private HashMap<String, BinaryPartAbstractImage> imagePartCache = new HashMap<String, BinaryPartAbstractImage>(); 

 /**
		Currently flying saucer is initialized
		with DEFAULT_DOTS_PER_POINT = DEFAULT_DOTS_PER_PIXEL = 20.
		Keep this in mind that, it may affect the resulting image sizes.
	*/
	private void addImage(BlockBox box) {
		
		Element e = box.getElement(); 
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
					this.getCurrentParagraph(true).getContent().add(run);

					org.docx4j.wml.Text text = Context.getWmlObjectFactory().createText();
					text.setValue("[INVALID DATA URI: " + e.getAttribute("src"));

					run.getContent().add(text);

					paraStillEmpty = false;
					return;
				}
				base64String = base64String.substring(commaPos + 1);
				log.debug(base64String);
				imageBytes = Base64.decodeBase64(base64String.getBytes("UTF8"));
			} else {
				
				imagePart = imagePartCache.get(e.getAttribute("src"));
				
				if (imagePart==null) {
					Docx4jUserAgent docx4jUserAgent = renderer.getDocx4jUserAgent();
					Docx4JFSImage docx4JFSImage = docx4jUserAgent.getDocx4JImageResource(e.getAttribute("src"));
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


				Long cx = (box.getStyle().valueByName(CSSName.WIDTH) == IdentValue.AUTO) ? null :
						UnitsOfMeasurement.twipToEMU(box.getWidth());
				Long cy = (box.getStyle().valueByName(CSSName.HEIGHT) == IdentValue.AUTO) ? null :
						UnitsOfMeasurement.twipToEMU(box.getHeight());
				
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
				}

				// Now add the inline in w:p/w:r/w:drawing
				org.docx4j.wml.R run = Context.getWmlObjectFactory().createR();
				this.getCurrentParagraph(true).getContent().add(run);
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
			this.getCurrentParagraph(true).getContent().add(run);

			org.docx4j.wml.Text text = Context.getWmlObjectFactory().createText();
			text.setValue("[MISSING IMAGE: " + e.getAttribute("alt") + ", " + e.getAttribute("alt") + " ]");

			run.getContent().add(text);
		}

		paraStillEmpty = false;

	}


	private CTMarkupRange markuprange;
	private void storeBookmarkEnd() {
		
	    markuprange = Context.getWmlObjectFactory().createCTMarkupRange(); 
	    JAXBElement<org.docx4j.wml.CTMarkupRange> markuprangeWrapped = Context.getWmlObjectFactory().createPBookmarkEnd(markuprange); 
	        markuprange.setId( BigInteger.valueOf(bookmarkId.getAndIncrement() ) );          		        
	}
	
	private AtomicInteger bookmarkId = new AtomicInteger();	
	
	
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
    	
        // Doesn't extend box
        Styleable s = inlineBox;
    	
    	if (log.isDebugEnabled() ) {
        	log.debug(inlineBox.toString());

        	if (s.getElement() == null)
        		log.debug("Null element name" ); 
        	else {
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
        
    		String name = s.getElement().getAttribute("name");
    		String href = s.getElement().getAttribute("href"); 
    		if (name!=null
    				&& !name.trim().equals("")) {
        		log.debug("[NAMED ANCHOR] " + name);
    			
    		    CTBookmark bookmark = Context.getWmlObjectFactory().createCTBookmark(); 
    		    JAXBElement<org.docx4j.wml.CTBookmark> bookmarkWrapped = Context.getWmlObjectFactory().createPBookmarkStart(bookmark); 
    		    this.getCurrentParagraph(true).getContent().add( bookmarkWrapped); 
    		        bookmark.setName( name ); 
    		        bookmark.setId( BigInteger.valueOf( bookmarkId.get()) ); 
    		        
//    		        addHyperlinkIfNec(href, getCascadedProperties(s.getStyle()));
    		        
    		        storeBookmarkEnd();    	
    		        this.getCurrentParagraph(true).getContent().add( markuprange); 
            		markuprange = null;
            		
            	paraStillEmpty = false;            		
    		} 
    		
    		if (href!=null && !href.trim().equals("")) {
    			log.warn("Ignoring @href on <a> without content.");
    		}
    		return;
    		
    	} 
        
        Map<String, CSSValue> cssMap = getCascadedProperties(s.getStyle());
//        Map cssMap = styleReference.getCascadedPropertiesMap(s.getElement());
        
        
        // Make sure the current paragraph is formatted
        P p = this.getCurrentParagraph(true);
        if (p.getPPr()==null) {
        	PPr pPr = Context.getWmlObjectFactory().createPPr();
        	addParagraphProperties( pPr,  s,  cssMap);
        	p.setPPr(pPr);
        }
        
                        
        String debug = "<UNKNOWN Styleable";
        if (s.getElement()==null) {
        	// Do nothing
        } else {
            debug = "<" + s.getElement().getNodeName();
            
            String cssClass = null;
        	if (s.getElement().getAttribute("class")!=null) {
        	 	cssClass=s.getElement().getAttribute("class").trim();
        	}
            
            if (s.getElement().getNodeName().equals("a")) {
            	
            	if (inlineBox.isStartsHere()) {
                	log.debug("Processing <a>... ");
            		
            		String name = s.getElement().getAttribute("name");
            		String href = s.getElement().getAttribute("href"); 
            		
            		if (name!=null
            				&& !name.trim().equals("")) {
            			log.debug("NAMED ANCHOR " + name);
            			
            		    CTBookmark bookmark = Context.getWmlObjectFactory().createCTBookmark(); 
            		    JAXBElement<org.docx4j.wml.CTBookmark> bookmarkWrapped = Context.getWmlObjectFactory().createPBookmarkStart(bookmark); 
            		    this.getCurrentParagraph(true).getContent().add( bookmarkWrapped); 
                    	paraStillEmpty = false;            		
            		    
        		        bookmark.setName( name ); 
        		        bookmark.setId( BigInteger.valueOf( bookmarkId.get()) );
            		    
        		        storeBookmarkEnd();    		                		        
        		        
        		        if (href==null
                				|| href.trim().equals("")) {
        		        	
		                	String theText = inlineBox.getElement().getTextContent();
		                    addRun(cssClass, cssMap, theText);
	
		                	return;
        		        }
            		}
            		
            		if (href!=null
            				&& !href.trim().equals("")) {
            			
	                	Hyperlink h = null;
	                	String linkText = inlineBox.getElement().getTextContent();
	                	
	                    RPr rPr =  Context.getWmlObjectFactory().createRPr();
	                    formatRPr(rPr, cssClass, cssMap);
	                    	                	
	                	log.debug(linkText);
	                	if (linkText!=null
	                			&& !linkText.trim().equals("")) {

	                		
	                    	h = createHyperlink(
	                    			href, 
	                    			rPr,
	                    			inlineBox.getText(), rp);                                    	            		
	                    	this.getCurrentParagraph(true).getContent().add(h);
	                        
		                	paraStillEmpty = false;  
		                	
		                	if (inlineBox.isEndsHere()) {
		                    	log.debug("Processing ..</a> (ends here as well) ");
		                    	return; // don't change contentContext
		                		
		                	} else {
		                		attachmentPointH = h;
		                		return; //.getContent();
		                	}
		                	
	                	} 
	                	else {
	                    	// No text content.  An image or something?  TODO handle hyperlink around inline image
	                		log.warn("Expected hyperlink content, since tag not self-closing");
	                    	h = createHyperlink(
	                    			href, 
	                    			rPr,
	                    			href, rp);                                    	            		
	                    	this.getCurrentParagraph(true).getContent().add(h);
		                	paraStillEmpty = false;            				                	
		                	return;
	                	}
            		}
            		
            	} else if (inlineBox.isEndsHere()) {
                	log.debug("Processing ..</a> ");
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
            debug +=  " " + s.getStyle().toStringMine();
        }
        
        
        log.debug(debug );
        //log.debug("'" + ((InlineBox)o).getTextNode().getTextContent() );  // don't use .getText()
        
        processInlineBoxContent(inlineBox, s, cssMap);
        
    }

	private void processInlineBoxContent(InlineBox inlineBox, Styleable s,
			Map<String, CSSValue> cssMap) {
				
		
		if (inlineBox.getTextNode()==null) {
                
            if (s.getElement().getNodeName().equals("br") ) {
                
                R run = Context.getWmlObjectFactory().createR();
                getListForRun().getContent().add(run);                
           		run.getContent().add(Context.getWmlObjectFactory().createBr());
            	
            } else {
            	log.debug("InlineBox has no TextNode, so skipping" );
            	
            	// TODO .. a span in a span or a?
            	// need to traverse, how?
            	
            }
            
        } else  {
            log.debug( inlineBox.getTextNode().getTextContent() );  // don't use .getText()

            String theText = inlineBox.getTextNode().getTextContent(); 
            log.debug("Processing " + theText);
            
            paraStillEmpty = false;   
            
            String cssClass = null;
        	if (s.getElement()!=null
        			&& s.getElement().getAttribute("class")!=null) {
        	 	cssClass=s.getElement().getAttribute("class").trim();
        	}
            addRun(cssClass, cssMap, theText);
    	            
//                                    else {
//                                    	// Get it from the parent element eg p
//                        	            //Map cssMap = styleReference.getCascadedPropertiesMap(e);
//                        	            run.setRPr(
//                        	            		addRunProperties( cssMap ));                                    	                                    	
//                                    }
        }
	}

	/**
	 * @param cssMap
	 * @param theText
	 */
	private void addRun( String cssClass, Map<String, CSSValue> cssMap, String theText) {
		
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
	}
		
	private void formatRPr(RPr rPr, String cssClass, Map<String, CSSValue> cssMap) {

		addRunProperties(rPr, cssMap );
		
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
		CSSValue fontFamily = cssMap.get("font-family");
		setRFont(fontFamily, rPr );

	}
	
	private void setRFont(CSSValue fontFamily, RPr rpr) {
		
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
			RFonts mappedTo = this.fontFamilyToFont.get(thisFontFamily);
			// Assume the first font family for which we have a mapping will contain a glyph
			// TODO should check. See fonts.txt
			if (mappedTo!=null) {
				rpr.setRFonts(mappedTo);
				// Save for re-use
				fontFamiliesToFont.put(fontFamily.getCssText(), mappedTo);
				return;
			}
		}
	}
    private Map<String, RFonts> fontFamiliesToFont = new HashMap<String, RFonts>(); 
    
    
    private boolean isListItem(Element e) {
    	
    	return e.getNodeName().equals("li");
    }
	
    private void addParagraphProperties(PPr pPr, Styleable styleable, Map cssMap) {
    	// NB, not invoked in CLASS_TO_STYLE_ONLY case
        
        for (Object o : cssMap.keySet()) {
        	
        	String cssName = (String)o;
        	CSSValue cssValue = (CSSValue)cssMap.get(cssName);
        	
        	Property p = PropertyFactory.createPropertyFromCssName(cssName, cssValue);
        	
        	if (p!=null) {
	        	if (p instanceof AbstractParagraphProperty) {        		
	        		((AbstractParagraphProperty)p).set(pPr);
	        	} else {
	        	    // try specific method
	        	    p = PropertyFactory.createPropertyFromCssNameForPPr(cssName, cssValue);
	        	    if (p!=null) {
	        	        if (p instanceof AbstractParagraphProperty) {               
	        	            ((AbstractParagraphProperty)p).set(pPr);
	        	        }
	        	    }
	        	    //log.debug(p.getClass().getName() );
	        	}
        	}
        	
        }
        
        
        if (styleable.getElement()!=null
        		&& isListItem(styleable.getElement()) ) {

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
    		int totalPadding = 0;
            LengthValue padding = (LengthValue)styleable.getStyle().valueByName(CSSName.PADDING_LEFT);
            totalPadding +=Indent.getTwip(padding.getCSSPrimitiveValue());
            
            LengthValue margin = (LengthValue)styleable.getStyle().valueByName(CSSName.MARGIN_LEFT);
            totalPadding +=Indent.getTwip(margin.getCSSPrimitiveValue());    			                
        	
            totalPadding +=listHelper.getAncestorIndentation();
            
        	// FS default css is 40px padding per level = 600 twip
            int defaultInd =  600 * listHelper.getDepth();
            if (totalPadding==defaultInd) {
            	// we can't tell whether this is just a default, so ignore it; use the numbering setting
            	log.debug("explicitly unsetting pPr indent");
            	pPr.setInd(null); 
            } else {
            	pPr.setInd(listHelper.getInd(totalPadding)); 
            } 
        	
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
    
    

    private void addRunProperties(RPr rPr, Map cssMap) {
    	
        for (Object o : cssMap.keySet()) {
        	
        	String cssName = (String)o;
        	CSSValue cssValue = (CSSValue)cssMap.get(cssName);
        	
        	Property runProp = PropertyFactory.createPropertyFromCssName(cssName, cssValue);
        	
        	if (runProp!=null) {
	        	if (runProp instanceof AbstractRunProperty) {  
	        		((AbstractRunProperty)runProp).set(rPr);
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
    	if (this.getCurrentParagraph(false).getPPr()!=null
    			&& this.getCurrentParagraph(false).getPPr().getPStyle()!=null) {
    		
    		String styleId = this.getCurrentParagraph(false).getPPr().getPStyle().getVal();
    		styleRPr = propertyResolver.getEffectiveRPr(styleId);
    		RPrCleanser.removeRedundantProperties(styleRPr, rPr);
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

	private Hyperlink createHyperlink(String url, RPr rPr, String linkText, RelationshipsPart rp) {
		
		if (linkText.contains("&")
				&& !linkText.contains("&amp;")) {
			// escape them so we can unmarshall
			linkText = linkText.replace("&", "&amp;");
		}
		
		try {

			// We need to add a relationship to word/_rels/document.xml.rels
			// but since its external, we don't use the 
			// usual wordMLPackage.getMainDocumentPart().addTargetPart
			// mechanism
			org.docx4j.relationships.ObjectFactory factory =
				new org.docx4j.relationships.ObjectFactory();
			
			org.docx4j.relationships.Relationship rel = factory.createRelationship();
			rel.setType( Namespaces.HYPERLINK  );
			rel.setTarget(url);
			rel.setTargetMode("External");  
									
			rp.addRelationship(rel);
			
			// addRelationship sets the rel's @Id
			
			String hpl = "<w:hyperlink r:id=\"" + rel.getId() + "\" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" >" +
            "<w:r>" +
            "<w:t>" + linkText + "</w:t>" +
            "</w:r>" +
            "</w:hyperlink>";

			Hyperlink hyperlink = (Hyperlink)XmlUtils.unmarshalString(hpl);
			R r = (R)hyperlink.getContent().get(0);
			r.setRPr(rPr);
			if (hyperlinkStyleId!=null) {
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

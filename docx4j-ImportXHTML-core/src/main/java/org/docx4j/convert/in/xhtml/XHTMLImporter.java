/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2013-2022, Plutext Pty Ltd, and contributors.
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
/**
 * 
 */
package org.docx4j.convert.in.xhtml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.Source;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;


/**
 * @author jharrop
 *
 */
public interface XHTMLImporter {
	
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
	public void setHyperlinkStyle (String hyperlinkStyleID);
	
	public void setRunFormatting(FormattingOption runFormatting);
	public void setParagraphFormatting(FormattingOption paragraphFormatting);
	public void setTableFormatting(FormattingOption tableFormatting);
	
	

    /**
     * Convert the well formed XHTML contained in file to a list of WML objects.
     * 
     * @param file
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(File file, String baseUrl) throws Docx4JException;

    /**
     * Convert the well formed XHTML from the specified SAX InputSource
     * 
     * @param is
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(InputSource is,  String baseUrl) throws Docx4JException;
    
    
    /**
     * @param is
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(InputStream is, String baseUrl) throws Docx4JException;
    
    
    /**
     * @param node
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(Node node,  String baseUrl) throws Docx4JException;
    
    
    /**
     * @param reader
     * @param baseUrl
     * @param wordMLPackage
     * @return
     * @throws IOException
     */
    public List<Object> convert(Reader reader,  String baseUrl) throws Docx4JException;
    
    
    
//    /**
//     * @param source
//     * @param baseUrl
//     * @param wordMLPackage
//     * @return
//     * @throws IOException
//     */
//    public List<Object> convert(Source source,  String baseUrl) throws Docx4JException;
    
        
    //public List<Object> convert(XMLEventReader reader) throws IOException {
    //public List<Object> convert(XMLStreamReader reader) throws IOException {
    
    /**
     * Convert the well formed XHTML found at the specified URI to a list of WML objects.
     * 
     * @param url
     * @param wordMLPackage
     * @return
     */
    public List<Object> convert(URL url) throws Docx4JException;
    
    
    /**
     * 
     * Convert the well formed XHTML contained in the string to a list of WML objects.
     * 
     * @param content
     * @param baseUrl
     * @param wordMLPackage
     * @return
     */
    public List<Object> convert(String content,  String baseUrl) throws Docx4JException;
    
    	
	/**
	 * Get the current numbers of SEQ fields, used in image captions.
	 * Typically you'd use this if you are importing multiple
	 * times into a single docx (as for example, OpenDoPE does).
	 * 
	 * @param sequenceCounters
	 * @since 3.2.0
	 */
    public Map<String, Integer> getSequenceCounters();

	/**
	 * Set the last used numbers of SEQ fields, used in image captions.
	 * Key is sequence name.  The default is "Figure", but you can also use
	 * others (matching value of @sequence).
	 * @param sequenceCounters
	 * @since 3.2.0
	 */
	public void setSequenceCounters(Map<String, Integer> sequenceCounters); 
	
	public AtomicInteger getBookmarkIdLast();
	
	/**
	 * Support injecting a starting bookmark value, so bookmark numbers
	 * can be managed across invocations.
	 * @param val
	 * @since 3.3.0
	 */
	//public void setBookmarkIdNext(AtomicInteger val);
	
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

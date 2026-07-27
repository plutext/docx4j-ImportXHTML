/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2014-2024, Plutext Pty Ltd, and contributors.
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

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.xml.bind.JAXBElement;

import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.finders.RangeFinder;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.docx4j.wml.CTBookmark;
import org.docx4j.wml.CTMarkupRange;
import org.docx4j.wml.CTSdtContentCell;
import org.docx4j.wml.CTSdtContentRow;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.SdtContentBlock;
import org.docx4j.wml.SdtElement;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * @author jharrop
 * @since 3.2.1
 */
public class BookmarkHelper {
	
	/*
	 * Nested bookmarks work as-is, without need
	 * for a stack of CTMarkupRange.
	 * 
	 * Warning/known issue:  avoid block level anchor eg
	 * 
    	String xhtml= "<div id=\"top\">" +
    			"<a name=\"AVOID\">" +
					"<h1>Heading</h1>" +
					"<div id=\"inner\">" +
					"<p>p1</p>" +
				  "</div>"+
					"</a>" +
		"</div>";	 * 
	 * 
	 * since this seems to confuse our Flying Saucer badly!  Suspect an upstream 
	 * fix is required.
	 * 
	 */
	
	public static Logger log = LoggerFactory.getLogger(BookmarkHelper.class);		
	
	private BookmarkHelper() {}
	
	BookmarkHelper(WordprocessingMLPackage wordMLPackage) {
		this.wordMLPackage=wordMLPackage;
	}
	
	private WordprocessingMLPackage wordMLPackage; // so we can calculate bookmark starting ID on demand
	
	private AtomicInteger bookmarkId = null;
	
	
	protected void setBookmarkId(AtomicInteger bookmarkId) {
		
		/* TODO: OpenDoPE needs a central mechanism to keep track of bookmark allocation,
		 * replacing what is done inBookmarkRenumber.
		 */
		
		this.bookmarkId = bookmarkId;
	}

	protected AtomicInteger getBookmarkId() {
		
		if (bookmarkId==null) {
			// Work out starting ID
			bookmarkId = new AtomicInteger(initBookmarkIdStart());
		}
		return bookmarkId;
	}

	private int initBookmarkIdStart() {

		int highestId = 0;
		
		RangeFinder rt = new RangeFinder();
		new TraversalUtil(wordMLPackage.getMainDocumentPart().getContent(), rt);
		
		for (CTBookmark bm : rt.getStarts()) {
			
			BigInteger id = bm.getId();
			if (id!=null && id.intValue()>highestId) {
				highestId = id.intValue();
			}
		}
		return highestId;
	}	
	
    /**
     * Convert a destination anchor given by @id or a/@name, to a bookmark start,
     * and return the corresponding bookmark end.
     * 
     * @param e
     * @return
     */
    protected CTMarkupRange anchorToBookmark(Element e, String bookmarkNamePrefix, P currentP, ContentAccessor contentContext) {
    	
    	if (e==null) {
//    		log.debug("passed null element", new Throwable());
    		return null;
    	}
    	
    	String name = null;
        if (e.getNodeName().equals("a")) {
        	name = e.getAttribute("name");
        } else {
        	name = e.getAttribute("id");
        }
        
		if (name==null
				|| name.trim().equals("")) {
			return null;
		}
		
		
		log.debug("[NAMED ANCHOR] " + name);
		//log.debug("trace", new Throwable());
		
	    CTBookmark bookmark = Context.getWmlObjectFactory().createCTBookmark(); 
	    JAXBElement<org.docx4j.wml.CTBookmark> bookmarkWrapped = null;
	    
	    // What to attach it to?
//		    P currentP = getCurrentParagraph(false);
	    if (currentP!=null) {
	    	
		    bookmarkWrapped = Context.getWmlObjectFactory().createPBookmarkStart(bookmark);
		    currentP.getContent().add( bookmarkWrapped); 
	    	
	    } else {
	    
//		    	ContentAccessor contentContext = this.contentContextStack.peek();
	    	if (contentContext instanceof Body) {

			    bookmarkWrapped = Context.getWmlObjectFactory().createBodyBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);

	    	} else if (contentContext instanceof SdtContentBlock) {
	    		
	    		bookmarkWrapped = Context.getWmlObjectFactory().createSdtContentBlockBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);
			    
	    	} else if (contentContext instanceof CTSdtContentRow) {
	    		
	    		bookmarkWrapped = Context.getWmlObjectFactory().createCTSdtContentRowBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);
	    		
	    	} else if (contentContext instanceof CTSdtContentCell) {
	    		
	    		bookmarkWrapped = Context.getWmlObjectFactory().createCTSdtContentCellBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);
	    		
	    	} else if (contentContext instanceof Tbl) {

			    bookmarkWrapped = Context.getWmlObjectFactory().createTblBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);
			    
	    	} else if (contentContext instanceof Tr) {

			    bookmarkWrapped = Context.getWmlObjectFactory().createTrBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);
			    
	    	} else if (contentContext instanceof Tc) {

			    bookmarkWrapped = Context.getWmlObjectFactory().createTcBookmarkStart(bookmark);
			    contentContext.getContent().add(bookmarkWrapped);
	    	} else {
	    		log.error("COuldn't attach bookmark " + name + " to " + contentContext.getClass().getName());
	    	}
	    }
	    
        bookmark.setName( idToBookmarkName(bookmarkNamePrefix, name) ); 
        bookmark.setId( BigInteger.valueOf( getBookmarkId().get()) ); 
	        
	        
	     return generateBookmarkEnd();    	
    }
    
	private CTMarkupRange generateBookmarkEnd() {
		
		CTMarkupRange markuprange = Context.getWmlObjectFactory().createCTMarkupRange(); 
	    markuprange.setId( BigInteger.valueOf(getBookmarkId().getAndIncrement() ) ); 
	    
        
        return markuprange;
	}

	protected void attachBookmarkEnd(CTMarkupRange markuprange, P currentP, ContentAccessor contentContext) {
			    
	    JAXBElement<CTMarkupRange> markuprangeWrapped;	 
	    
	    // What to attach it to?
//	    P currentP = getCurrentParagraph(false);
	    if (currentP!=null) {
	    	
	    	markuprangeWrapped = Context.getWmlObjectFactory().createPBookmarkEnd(markuprange);
	    	currentP.getContent().add( markuprangeWrapped); 
	    	
	    } else {
	    
//	    	ContentAccessor contentContext = this.contentContextStack.peek();
	    	if (contentContext instanceof Body) {

	    		markuprangeWrapped = Context.getWmlObjectFactory().createBodyBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);

	    	} else if (contentContext instanceof SdtContentBlock) {
	    		
	    		markuprangeWrapped = Context.getWmlObjectFactory().createSdtContentBlockBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);
			    
	    	} else if (contentContext instanceof CTSdtContentRow) {
	    		
	    		markuprangeWrapped = Context.getWmlObjectFactory().createCTSdtContentRowBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);
	    		
	    	} else if (contentContext instanceof CTSdtContentCell) {
	    		
	    		markuprangeWrapped = Context.getWmlObjectFactory().createCTSdtContentCellBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);
			    
	    	} else if (contentContext instanceof Tbl) {

	    		markuprangeWrapped = Context.getWmlObjectFactory().createTblBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);
			    
	    	} else if (contentContext instanceof Tr) {

	    		markuprangeWrapped = Context.getWmlObjectFactory().createTrBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);
			    
	    	} else if (contentContext instanceof Tc) {

	    		markuprangeWrapped = Context.getWmlObjectFactory().createTcBookmarkEnd(markuprange);
			    contentContext.getContent().add(markuprangeWrapped);
	    	} else {
	    		log.error("COuldn't attach bookmark  to " + contentContext.getClass().getName());
	    	}
	    }
	}
    
	/**
	 * Convert @id or a/@name to w:hyperlink/@w:anchor
	 * 
	 * @param href
	 * @return
	 */
	protected String idToBookmarkName(String bookmarkNamePrefix, String id) {
    	
    	return bookmarkNamePrefix + id;
    	
    }

	/**
	 * Convert a/@name to w:hyperlink/@w:anchor
	 * 
	 * @param href
	 * @return
	 */
	protected String anchorToBookmarkName(String bookmarkNamePrefix, String href) {
    	
    	if (!href.startsWith("#")) {
    		log.error(href + " is not a relative link", new Throwable());
    		return href;
    	}
    	
    	return bookmarkNamePrefix + href.substring(1);
    	
    }
	

}

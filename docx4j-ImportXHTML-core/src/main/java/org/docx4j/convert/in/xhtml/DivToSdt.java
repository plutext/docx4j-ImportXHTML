/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2014-2022, Plutext Pty Ltd, and contributors.
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

import java.util.HashMap;

import org.docx4j.XmlUtils;
import org.docx4j.model.sdt.QueryString;
import com.openhtmltopdf.render.BlockBox;
import org.docx4j.wml.Body;
import org.docx4j.wml.CTSdtCell;
import org.docx4j.wml.CTSdtContentCell;
import org.docx4j.wml.CTSdtContentRow;
import org.docx4j.wml.CTSdtRow;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;
import org.docx4j.wml.SdtElement;
import org.docx4j.wml.SdtPr;
import org.docx4j.wml.Tag;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Optionally convert each div element to an SDT (aka content control).
 *
 */
public class DivToSdt implements DivHandler {
	
	public static Logger log = LoggerFactory.getLogger(DivToSdt.class);		
	
	public ContentAccessor enter(BlockBox blockBox, ContentAccessor contentContext) {
		
		SdtElement sdt = null;
		
    	if (contentContext instanceof Body
    			|| contentContext instanceof SdtContentBlock) {

    		sdt = new SdtBlock();
    		((SdtBlock)sdt).setSdtContent(new SdtContentBlock());
    		
    	} else if (contentContext instanceof Tbl
    			|| contentContext instanceof CTSdtContentRow) {

    		sdt = new CTSdtRow();
    		((CTSdtRow)sdt).setSdtContent(new CTSdtContentRow());
		    
    	} else if (contentContext instanceof Tr
    			|| contentContext instanceof CTSdtContentCell) {

    		sdt = new CTSdtCell();
    		((CTSdtCell)sdt).setSdtContent(new CTSdtContentCell());
		    
    	} else if (contentContext instanceof Tc) {

    		sdt = new SdtBlock();
    		((SdtBlock)sdt).setSdtContent(new SdtContentBlock());
    		
    	} else {
    		log.warn("Couldn't handle div in context " + contentContext.getClass().getName());
    		log.warn(XmlUtils.w3CDomNodeToString(blockBox.getElement()));
    	}
    	
    	if (sdt==null) {
    		return null;
    	} else {
    		
    		SdtPr sdtPr = new SdtPr();
    		sdt.setSdtPr(sdtPr);
    		
    		// Set tag
    		Element el = blockBox.getElement();
    		HashMap<String, String> attrs= new HashMap<String, String>();
    		String id = el.getAttribute("id");
    		if (id != null
    				&& id.trim().length()>0 ) {
    			attrs.put("id", id );
    		}
    		String clas = el.getAttribute("class");
    		if (clas != null
    				&& clas.trim().length()>0 ) {
    			attrs.put("class", clas );
    		}
    		if (attrs.size()>0) {
    			Tag tag = new Tag();
    			tag.setVal(QueryString.create(attrs));
    			
    			sdtPr.setTag(tag);
    		}
    		
    		// Set ID
    		sdtPr.setId();
    		
    		contentContext.getContent().add(sdt);
    		
    		return (ContentAccessor) sdt.getSdtContent();
    	}
		
		
	}

	public void leave() {
		
	}
	
}

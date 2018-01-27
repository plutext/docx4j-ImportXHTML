package org.docx4j.convert.in.xhtml;

import java.util.HashMap;

import org.docx4j.XmlUtils;
import org.docx4j.model.sdt.QueryString;
import org.xhtmlrenderer.render.BlockBox;
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

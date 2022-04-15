package org.docx4j.convert.in.xhtml;

import com.openhtmltopdf.render.BlockBox;
import org.docx4j.wml.ContentAccessor;

public interface DivHandler {
	
	public ContentAccessor enter(BlockBox blockBox, ContentAccessor contentContext);

	public void leave();
	
}

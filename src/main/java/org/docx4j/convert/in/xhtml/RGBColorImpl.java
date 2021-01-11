package org.docx4j.convert.in.xhtml;

import com.openhtmltopdf.css.parser.FSRGBColor;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.RGBColor;

public class RGBColorImpl implements RGBColor {

	
	RGBColorImpl(FSRGBColor fsColor) {
		this._red = fsColor.getRed();
		this._green = fsColor.getGreen();
		this._blue = fsColor.getBlue();
	}
	
    private final int _red;
    private final int _green;
    private final int _blue;
	
	
	@Override
	public CSSPrimitiveValue getRed() {
		return toCSSPrimitiveValue(_red);
	}

	@Override
	public CSSPrimitiveValue getGreen() {
		return toCSSPrimitiveValue(_green);
	}

	@Override
	public CSSPrimitiveValue getBlue() {
		return toCSSPrimitiveValue(_blue);
	}

	
	private CSSPrimitiveValue toCSSPrimitiveValue(int n) {
		DomCssValueAdaptor primitive = new DomCssValueAdaptor();
		primitive.setFloatValue((short) 0, n);
		return primitive;
		
	}
}

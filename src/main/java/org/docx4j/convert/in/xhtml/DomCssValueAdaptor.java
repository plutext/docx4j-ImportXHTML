package org.docx4j.convert.in.xhtml;

import org.docx4j.org.xhtmlrenderer.css.parser.FSRGBColor;
import org.docx4j.org.xhtmlrenderer.css.parser.PropertyValue;
import org.docx4j.org.xhtmlrenderer.css.style.derived.ColorValue;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.Counter;
import org.w3c.dom.css.RGBColor;
import org.w3c.dom.css.Rect;

public class DomCssValueAdaptor implements org.w3c.dom.css.CSSPrimitiveValue {

	
	DomCssValueAdaptor(PropertyValue val) {
		
		this.val = val;
		
		this.cssText = val.getCssText();
		this.cssValueType = val.getCssValueType();

		this.floatValue = val.getFloatValue();
		
	}

	DomCssValueAdaptor() {
				
	}
	
	private PropertyValue val;
	
	private String cssText;
	private short cssValueType;
    
	private float floatValue;
	
	@Override
	public String getCssText() {
		return cssText;
	}

	@Override
	public void setCssText(String cssText) throws DOMException {
		this.cssText = cssText;
	}

	@Override
	public short getCssValueType() {
		return cssValueType;
	}

	@Override
	public short getPrimitiveType() {
		return val.getPrimitiveType();
	}

	@Override
	public void setFloatValue(short unitType, float floatValue) throws DOMException {
		// Used in RGB color stuff
		this.floatValue = floatValue;
		
	}

	
	@Override
	public float getFloatValue(short unitType) throws DOMException {
		
		return floatValue;
	}

	@Override
	public void setStringValue(short stringType, String stringValue) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getStringValue() throws DOMException {
		return val.getStringValue();
	}

	@Override
	public Counter getCounterValue() throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Rect getRectValue() throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RGBColor getRGBColorValue() throws DOMException {
		
		if (val.getFSColor()!=null ) {
			return new RGBColorImpl((FSRGBColor)val.getFSColor() );
		}
		
		throw new UnsupportedOperationException();
	}

}

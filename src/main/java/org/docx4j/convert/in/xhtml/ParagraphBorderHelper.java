package org.docx4j.convert.in.xhtml;

import java.math.BigInteger;
import java.util.Map;

import org.docx4j.UnitsOfMeasurement;
import org.docx4j.jaxb.Context;
import org.docx4j.org.xhtmlrenderer.css.constants.CSSName;
import org.docx4j.org.xhtmlrenderer.css.style.FSDerivedValue;
import org.docx4j.org.xhtmlrenderer.css.style.derived.LengthValue;
import org.docx4j.org.xhtmlrenderer.layout.Styleable;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.PBdr;
import org.docx4j.wml.STBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.css.CSSValue;

/**
 * Consider p/@style="border: 1px solid black;padding-left:20px;"
 * 
 * That should generates something like:
 * 
 * &lt;w:pBdr&gt;
 *    &lt;w:left w:val="single" w:sz="6" w:space="15" w:color="000000"/&gt;
 *    
 * Because this is multiple CSS properties to one WordML element, it
 * is best to handle it in this dedicated helper class.  
 * 
 * No attempt is made to reconcile borders eg between a p and its enclosing
 * div or tc (if any), or preceding or following p. 
 * 
 * @author jharrop
 * @since 3.2.2
 */
public class ParagraphBorderHelper {
	
	public static Logger log = LoggerFactory.getLogger(ParagraphBorderHelper.class);
    
	private XHTMLImporterImpl importer;
    protected ParagraphBorderHelper(XHTMLImporterImpl importer) {
    	this.importer=importer;
    }
    
    PBdr pBdr = null;
	
	private PBdr createPBdr(PPr pPr) {

		pBdr = pPr.getPBdr();
		if (pBdr == null) {
			pBdr = Context.getWmlObjectFactory().createPPrBasePBdr();
			pPr.setPBdr(pBdr);
		}

		return pBdr;
	}
	
	private CTBorder getBorder(PPr pPr, String side) {
		
		pBdr = createPBdr(pPr);
		CTBorder border = null;
		
		if (side.equals("left")) {
			border = pBdr.getLeft(); 
		} else if (side.equals("right")) {
			border = pBdr.getRight(); 
		} else if (side.equals("top")) {
			border = pBdr.getTop(); 
		} else if (side.equals("bottom")) {
			border = pBdr.getBottom(); 
		}
		
		if (border==null) {
			border = Context.getWmlObjectFactory().createCTBorder();
			if (side.equals("left")) {
				pBdr.setLeft(border); 
			} else if (side.equals("right")) {
				pBdr.setRight(border); 
			} else if (side.equals("top")) {
				pBdr.setTop(border); 
			} else if (side.equals("bottom")) {
				pBdr.setBottom(border); 
			}
		}
		
		return border;
	}

	private void setBorder(PPr pPr, String side, CTBorder border ) {
		
		pBdr = createPBdr(pPr);
		
		if (side.equals("left")) {
			pBdr.setLeft(border); 
		} else if (side.equals("right")) {
			pBdr.setRight(border); 
		} else if (side.equals("top")) {
			pBdr.setTop(border); 
		} else if (side.equals("bottom")) {
			pBdr.setBottom(border); 
		}
	}
	
    protected void addBorderProperties(PPr pPr, Styleable styleable, Map<String, CSSValue> cssMap) {
    	
    	doSide( pPr,  styleable,cssMap, "left");
    	doSide( pPr,  styleable,cssMap, "right");
    	doSide( pPr,  styleable,cssMap, "top");
    	doSide( pPr,  styleable,cssMap, "bottom");

    }
    
    protected void doSide(PPr pPr, Styleable styleable, Map<String, CSSValue> cssMap, String side) {
    	
    	CTBorder border = null;
    	
    	CSSValue borderStyle = cssMap.get("border-"+side+"-style");
    	if (borderStyle!=null && !borderStyle.equals("none")) {
    		// paragraph has a border
    		border = createBorderStyle( styleable, side);

    		setBorder( pPr,  side,  border );
    	}
    	
		// padding to space
    	BigInteger spaceAttrVal = paddingToSpace(styleable, side);
    	
    	if (spaceAttrVal!=null) {
    		
    		border = getBorder( pPr,  side);
    		border.setSpace(spaceAttrVal);
    	}
    	
    	// NB:  Word 2010 doesn't seem to honour this when its in a table cell!
    	// and there doesn't seem to be a compat setting which should affect this.
    	
    }
		
	private BigInteger paddingToSpace(Styleable styleable,  String side)  {

		FSDerivedValue padding = styleable.getStyle().valueByName( CSSName.getByPropertyName("padding-" + side));
		if (padding != null && padding instanceof LengthValue) {
						
			int twip = UnitsOfMeasurement.pxToTwip(((LengthValue)padding).asFloat()/20);
			
			if (twip>0) {
				return BigInteger.valueOf(twip);
			} else {
				// Don't create a @space setting
			}
		}
		return null;
	}
	
	/**
	 *  borders support
	 * @param box table or cell to copy css border properties from
	 * @param side "top"/"bottom"/"left"/"right"
	 * @return  border style
	 */
	private CTBorder createBorderStyle(Styleable styleable, String side) {
		
		FSDerivedValue borderStyle = styleable.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-style") );
		FSDerivedValue borderColor = styleable.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-color") );
		float width = styleable.getStyle().getFloatPropertyProportionalHeight(
				CSSName.getByPropertyName("border-"+side+"-width"), 0, importer.getRenderer().getLayoutContext() );

		STBorder stBorder;
		try {
			stBorder = STBorder.fromValue( borderStyle.asString() );
		} catch (IllegalArgumentException e) {
			stBorder = STBorder.SINGLE; 
		}

		// w:ST_EighthPointMeasure - Measurement in Eighths of a Point
		width = UnitsOfMeasurement.twipToPoint( Math.round(width) ) * 8.0f;
		
		String color = borderColor.asString();
		if (color.startsWith("#")) color=color.substring(1);
		
		return createBorderStyle( stBorder, color, BigInteger.valueOf( Math.round(width) ) );
	}

	private CTBorder createBorderStyle(STBorder val, String color, BigInteger sz) {
		CTBorder border = Context.getWmlObjectFactory().createCTBorder();
		border.setVal(val);
		border.setColor(color);
		border.setSz(sz);
		return border;
	}



}

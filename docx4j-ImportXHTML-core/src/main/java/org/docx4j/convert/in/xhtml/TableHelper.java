/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2014-2026, Plutext Pty Ltd, and contributors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.docx4j.UnitsOfMeasurement;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl.TableProperties;
import org.docx4j.jaxb.Context;
import org.docx4j.model.properties.table.tr.TrHeight;
import org.docx4j.openpackaging.exceptions.CyclicStylesException;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTHeight;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.CTTblLayoutType;
import org.docx4j.wml.CTTblPrBase.TblStyle;
import org.docx4j.wml.CTVerticalJc;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.STTblLayoutType;
import org.docx4j.wml.STVerticalJc;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcMar;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner;
import org.docx4j.wml.TcPrInner.GridSpan;
import org.docx4j.wml.TcPrInner.VMerge;
import org.docx4j.wml.Tr;
import org.docx4j.wml.TrPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.newtable.TableSectionBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;

import jakarta.xml.bind.JAXBElement;

public class TableHelper {
	
	private static final String TABLE = "table";
	
	public static Logger log = LoggerFactory.getLogger(TableHelper.class);
	    
	private XHTMLImporterImpl importer;
    protected TableHelper(XHTMLImporterImpl importer) {
    	this.importer=importer;
    }
	
    /**
     * @param cssTable
     * @param tbl
     * @param tableProperties
     * @throws CyclicStylesException 
     */
    protected void setupTblPr(TableBox cssTable, Tbl tbl, TableProperties tableProperties) throws CyclicStylesException {
    	
        Element e = cssTable.getElement();     	

		TblPr tblPr = Context.getWmlObjectFactory().createTblPr();
		tbl.setTblPr(tblPr);    

        String cssClass = null;
    	if (e.getAttribute("class")!=null) {
    	 	cssClass=e.getAttribute("class").trim();
            setTableStyle(tblPr, cssClass);
    	}
		
		
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
			int cssSpacing = cssTable.getStyle().getBorderHSpacing(importer.getRenderer().getLayoutContext()); // in dots
			// halved, since Word leaves it twice: between cells, and between a cell and the table's border (CR-008 §4)
			spacingWidth.setW( WordRange.TBL_CELL_SPACING.clamp(Math.round(importer.layoutDotsToTwip(cssSpacing) / 2), "border-spacing") ); 
			spacingWidth.setType(TblWidth.TYPE_DXA);
		}
		tblPr.setTblCellSpacing(spacingWidth); 
		
		// Table indent.  
		// cssTable.getLeftMBP() which is setLeftMBP((int) margin.left() + (int) border.left() + (int) padding.left());
		// cssTable.getTx(); which is (int) margin.left() + (int) border.left() + (int) padding.left();
		// But want just margin.left
		
        log.debug("list depth:" + importer.getListHelper().getDepth());
        if (importer.getListHelper().getDepth()>0 ) { 

    		TblWidth tblInd = tblPr.getTblInd();
    		if (tblInd==null) {
    			tblInd = new TblWidth();
    			tblPr.setTblInd(tblInd);
    		}
    		tblInd.setType(TblWidth.TYPE_DXA);
        	
        	// Handle using the same logic we use for indenting a paragraph
    		int totalPadding = importer.getListHelper().getAbsoluteIndent(cssTable);
    		int tableIndentContrib = tableIndentContrib(importer.getContentContextStack());
            if (importer.getListHelper().peekListItemStateStack().isFirstChild) {
            	

            	// totalPadding gives indent to the bullet;
            	log.debug("Table in list indent case 1: tblInd set for item itself");
            	tblInd.setW(WordRange.TBL_IND.clamp(totalPadding-tableIndentContrib, "a list's indent")); 
                importer.getListHelper().peekListItemStateStack().isFirstChild=false;
            	
            } else {
            	
            	// totalPadding gives indent to the bullet;
            	// we want to align this subsequent p with the preceding text;
            	// assume 360 twips
            	
            	log.debug("Table in list indent case 2: tblInd set for follwing child");
            	tblInd.setW(WordRange.TBL_IND.clamp(totalPadding+ListHelper.INDENT_AFTER-tableIndentContrib, "a list's indent")); 
            } 
        	
    		
        } else if (cssTable.getMargin(importer.getRenderer().getLayoutContext()) !=null
				&& cssTable.getMargin(importer.getRenderer().getLayoutContext()).left()>0) {
        	
			log.debug("Calculating TblInd from margin.left: " + cssTable.getMargin(importer.getRenderer().getLayoutContext()).left() );
    		TblWidth tblInd = Context.getWmlObjectFactory().createTblWidth();
    		tblInd.setW( WordRange.TBL_IND.clamp( Math.round(importer.layoutDotsToTwip(
    				cssTable.getMargin(importer.getRenderer().getLayoutContext()).left()
    				)), "margin-left on a table"));
    		tblInd.setType(TblWidth.TYPE_DXA);
			tblPr.setTblInd(tblInd);
			
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
        		TblWidth tblInd = Context.getWmlObjectFactory().createTblWidth();
        		tblInd.setW( BigInteger.valueOf( 115));
        		tblInd.setType(TblWidth.TYPE_DXA);
    			tblPr.setTblInd(tblInd);
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
    }
    
    protected void setupTblGrid(TableBox cssTable, Tbl tbl, TableProperties tableProperties) throws CyclicStylesException {
    	
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
    	
    	log.debug("setupTblGrid " + cssTable.numEffCols() + "  " + colPos.length);
    	
    	for (int i=1; i<=cssTable.numEffCols(); i++) {
    		
    		TblGridCol tblGridCol = Context.getWmlObjectFactory().createTblGridCol();
    		tblGrid.getGridCol().add(tblGridCol);
    		
    		log.debug("colpos=" + colPos[i]);
    		tblGridCol.setW( WordRange.GRID_COL.clamp(columnWidth(colPos, i), "a column's width") );
    		
    	}
    	
    }
    
    /**
     * @return column col's width (from 1), in twips.  openhtmltopdf's column positions are in dots;
     * each is converted before they're subtracted, so the columns add up to the table (CR-008).
     */
    private int columnWidth(int[] colPos, int col) throws CyclicStylesException {
    	return Math.round(importer.layoutDotsToTwip(colPos[col])) - Math.round(importer.layoutDotsToTwip(colPos[col-1]));
    }

    protected void setupTrPr(com.openhtmltopdf.newtable.TableRowBox trBox, Tr tr, boolean isTblHeader ) throws CyclicStylesException {

	    TrPr trPr = Context.getWmlObjectFactory().createTrPr();
	    tr.setTrPr(trPr);

	    /* Row height (CR-007 D6 (d)): only where the CSS gives the row or one of its cells a
	     * height, as atLeast the greatest of them.  A row sized by its content gets none, and
	     * Word sizes it to its content.  
	     * 
	     * The row's laid-out height was written here before, for every row.  But Word gives each
	     * part of a row split across pages at least the row's minimum height, so the cell ran on,
	     * empty, to the foot of its last page, and a row whose minimum is more than a page starts
	     * on a page of its own (Word 365, CR-007 §4).
	     * 
	     * A web browser ignores td height 0 (tested in Chrome), or any value less than height of contents,
	     * hence atLeast.
	     */	    
	    float height = cssHeight(trBox); // in openhtmltopdf's dots: 100px = 2000
	    if (height > 0) { 

    		log.debug("CSS height: " + height);
	    	
	    	TrHeight thr = new TrHeight(); // uses STHeightRule.AT_LEAST
	    	thr.set(trPr);
	    	/* Per https://github.com/danfickle/openhtmltopdf/blob/4a475a91563f7f0c8c1d81029b1726e4ff112093/openhtmltopdf-pdfbox/src/main/java/com/openhtmltopdf/pdfboxout/PdfBoxFastOutputDevice.java#L76
			    //   PDF points are defined as 1/72 inch.
			    //   CSS pixels are defined as 1/96 inch.
			    //   PDF text units are defined as 1/1000 of a PDF point.
			    //   OpenHTMLtoPDF dots are defined as 1/20 of a CSS pixel.
			    //   Therefore dots per point is 20 * 96/72 or about 26.66.
			    //   Dividing by _dotsPerPoint will convert OpenHTMLtoPDF dots to PDF points.
     * 	    	 * 
	    	 */
			int twip = UnitsOfMeasurement.pxToTwip(height/20);
			((CTHeight)thr.getObject()).setVal(WordRange.TR_HEIGHT.clamp(twip, "height on a row or cell"));	    	
	    }
	    
	    if (isTblHeader) {
            // Create object for tblHeader (wrapped in JAXBElement) 
            BooleanDefaultTrue booleandefaulttrue = Context.getWmlObjectFactory().createBooleanDefaultTrue(); 
            JAXBElement<org.docx4j.wml.BooleanDefaultTrue> booleandefaulttrueWrapped = Context.getWmlObjectFactory().createCTTrPrBaseTblHeader(booleandefaulttrue); 
            trPr.getCnfStyleOrDivIdOrGridBefore().add( booleandefaulttrueWrapped); 	
            }

    }
    
    /**
     * @return the greatest height the CSS gives the row or one of its cells, in openhtmltopdf's 
     * dots, or 0.  A height that isn't absolute (a percentage) is auto, as openhtmltopdf has it 
     * (isAutoHeight), and a cell's height is its border box's, as openhtmltopdf lays it out.  A 
     * cell spanning rows gives none.
     */
    private float cssHeight(com.openhtmltopdf.newtable.TableRowBox trBox) throws CyclicStylesException {
    	
    	float height = 0;
    	if (!trBox.isAutoHeight()) {
    		height = trBox.getStyle().getFloatPropertyProportionalHeight(
    				CSSName.HEIGHT, 0, importer.getRenderer().getLayoutContext());
    	}
    	for (Box box : trBox.getChildren()) {
    		if (box instanceof TableCellBox
    				&& box.getStyle().getRowSpan()==1
    				&& !((TableCellBox)box).isAutoHeight()) {
    			height = Math.max(height, box.getStyle().getFloatPropertyProportionalHeight(
    					CSSName.HEIGHT, 0, importer.getRenderer().getLayoutContext()));
    		}
    	}
    	return height;
    }
    
    protected void setupTcPr(TableCellBox tcb, Tc tc, TableProperties tableProperties) throws CyclicStylesException {

		int effCol = tcb.getTable().colToEffCol(tcb.getCol());
		
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
    		tblW.setW(WordRange.TC_W.clamp(columnWidth(tableProperties.getColumnPos(), effCol+1), "a column's width" ));
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
		FSColor fsColor = CssColorForWord.opaque(tcb.getStyle().getBackgroundColor()); // none where transparent (CR-006)
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
		
		// Vertical alignment; eg vertical-align: bottom; to <w:vAlign w:val="bottom"/>
		IdentValue valign = tcb.getVerticalAlign();
		// eg baseline|length|sub|super|top|text-top|middle|bottom|text-bottom|initial|inherit
		// we support top | middle | bottom
		if (valign!=null) {
			
			if ("top".equals(valign.asString())) {
				
				CTVerticalJc vjc = new CTVerticalJc(); 
				vjc.setVal(STVerticalJc.TOP);
				tcPr.setVAlign(vjc);
				
			} else if ("middle".equals(valign.asString())) {

				CTVerticalJc vjc = new CTVerticalJc(); 
				vjc.setVal(STVerticalJc.CENTER);
				tcPr.setVAlign(vjc);
				
			} else if ("bottom".equals(valign.asString())) {

				CTVerticalJc vjc = new CTVerticalJc(); 
				vjc.setVal(STVerticalJc.BOTTOM);
				tcPr.setVAlign(vjc);
			} 
		}
		
		// padding becomes margin: the padding as openhtmltopdf laid it out, in dots, so em and % 
		// are resolved, and 0 is 0 (CR-008 D2, D5)
		RectPropertySet padding = tcb.getPadding(importer.getRenderer().getLayoutContext());
		TcMar tcMar = Context.getWmlObjectFactory().createTcMar();
		tcPr.setTcMar(tcMar); 

		tcMar.setLeft(getCellMargin(padding.left(), "left"));
		tcMar.setRight(getCellMargin(padding.right(), "right"));
		tcMar.setTop(getCellMargin(padding.top(), "top"));
		tcMar.setBottom(getCellMargin(padding.bottom(), "bottom"));
		
    }
		
	private TblWidth getCellMargin(float dots, String side) throws CyclicStylesException {
		
		TblWidth tblWidth = Context.getWmlObjectFactory().createTblWidth();
		tblWidth.setW(WordRange.TC_MAR.clamp(Math.round(importer.layoutDotsToTwip(dots)), "padding-" + side + " on a cell"));
		tblWidth.setType("dxa");
		return tblWidth;
	}
    
	/**
	 * Table borders support
	 * @param box table or cell to copy css border properties from
	 * @param side "top"/"bottom"/"left"/"right"
	 * @param keepNone if true, then missed borders returned as border with style NONE (for tables), else as null (for cells) 
	 * @return reproduced border style
	 * @throws CyclicStylesException 
	 */
	private CTBorder copyBorderStyle(Box box, String side, boolean keepNone) throws CyclicStylesException {
		FSDerivedValue borderStyle = box.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-style") );
		FSDerivedValue borderColor = box.getStyle().valueByName( CSSName.getByPropertyName("border-"+side+"-color") );
		float width = box.getStyle().getFloatPropertyProportionalHeight(
				CSSName.getByPropertyName("border-"+side+"-width"), 0, importer.getRenderer().getLayoutContext() );
		log.debug("border width: " + width);

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
		
		// double border width in html is applied to the whole border, 
		// while the word applying it to each bar and the gap in between 
		if(borderStyle.asIdentValue() == IdentValue.DOUBLE) {
			width /= 3;
		}

		STBorder stBorder;
		try {
			stBorder = STBorder.fromValue( borderStyle.asString() );
		} catch (IllegalArgumentException e) {
			stBorder = STBorder.SINGLE; 
		}
		if (log.isDebugEnabled()) {
			log.debug(borderStyle.asString() + " -> " + stBorder);
		}

		// w:ST_EighthPointMeasure - Measurement in Eighths of a Point; 20 twips to the point (CR-008)
		width = importer.layoutDotsToTwip(width) / 20f * 8.0f;
		log.debug("converted border width: " + width);
		
		// hex, or none where it's transparent, and then no border (CR-006)
		String color = CssColorForWord.borderColor(borderColor);
		if (color==null) {
			return createBorderStyle(STBorder.NIL, null, null);
		}
		
		return createBorderStyle( stBorder, color, WordRange.BORDER_SZ.clamp( Math.round(width), "border-" + side + "-width" ) );
	}

	private CTBorder createBorderStyle(STBorder val, String color, BigInteger sz) {
		CTBorder border = Context.getWmlObjectFactory().createCTBorder();
		border.setVal(val);
		border.setColor(color);
		border.setSz(sz);
		return border;
	}

	private TcPrInner.TcBorders copyCellBorderStyles(TableCellBox box) throws CyclicStylesException {
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
	 * @throws CyclicStylesException 
	 */
	protected void insertDummyVMergedCells(ContentAccessor trContext, TableCellBox tcb, boolean backwards) throws CyclicStylesException {

		log.debug("Scanning cells from " + tcb.getRow() + ", " + tcb.getCol() + " to the " + (backwards ? "left" : "right") );

		ArrayList<TableCellBox> adjCells = new ArrayList<TableCellBox>();
		int numEffCols = tcb.getTable().numEffCols();

		for ( int i = tcb.getCol(); i >= 0 && i < numEffCols; i += backwards ? -1 : 1 ) {

			TableSectionBox tsb = tcb.getSection();
			TableCellBox adjCell = tsb.cellAt(tcb.getRow(), i);

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
	 * The table's row groups in the order the source has them.  openhtmltopdf moves
	 * the first table-header-group to the top and the first table-footer-group to the
	 * bottom (BoxBuilder.reorderTableContent), as a browser lays them out; Word's HTML
	 * import keeps each where it is.  An anonymous row group (rows directly in the
	 * table) is placed by its first row.  If a group can't be placed, the order is
	 * openhtmltopdf's.
	 */
	protected static List<Box> rowGroupsInSourceOrder(TableBox table) {
		List<Box> groups = new ArrayList<Box>(table.getChildren());
		Element tableElement = table.getElement();
		if (tableElement == null) return groups;

		Map<Node, Integer> position = new IdentityHashMap<Node, Integer>();
		int i = 0;
		for (Node n = tableElement.getFirstChild(); n != null; n = n.getNextSibling()) {
			position.put(n, i++);
		}
		final Map<Box, Integer> order = new IdentityHashMap<Box, Integer>();
		for (Box group : groups) {
			Integer at = position.get(childOfTable(group, tableElement));
			if (at == null) {
				log.debug("Can't place a row group of <" + tableElement.getNodeName() + ">; openhtmltopdf's order kept");
				return groups;
			}
			order.put(group, at);
		}
		groups.sort(Comparator.comparing(order::get)); // stable
		return groups;
	}

	/**
	 * The child of the table element a row group comes from: its own element, or for
	 * an anonymous group, its first row's (or that row's first cell's).
	 */
	private static Node childOfTable(Box box, Element tableElement) {
		for (Box b = box; b != null; b = b.getChildCount() == 0 ? null : b.getChild(0)) {
			if (b.getElement() != null && b.getElement().getParentNode() == tableElement) {
				return b.getElement();
			}
		}
		return null;
	}

	/**
	 * nested tables XHTML renderer seems to construct a tree: table/table
	 * instead of table/tr/td/table?
	 * TODO fix this upstream.
	 * TestCase is http://en.wikipedia.org/wiki/Office_Open_XML
	 * 
	 * A table with a caption is not nested: openhtmltopdf wraps it in an anonymous
	 * block box carrying the table element (isFromCaptionedTable), which the importer
	 * treats as transparent, so the caption comes out as a paragraph beside the w:tbl.
	 * 
	 * @param contentContext
	 * @param parent
	 */
	protected void nestedTableHierarchyFix(ContentAccessor contentContext,
			Box parent) {
		
		if (parent==null) return; // where importing a table fragment 
		
		if (parent instanceof BlockBox
				&& ((BlockBox)parent).isFromCaptionedTable()) {
			// the anonymous wrapper openhtmltopdf creates for a table with a caption;
			// the table is not nested
			return;
		}
		
		if (parent instanceof TableBox
				|| parent.getElement().getNodeName().equals("table") ) {
			log.warn("table: Constructing missing w:tr/w:td..");
			
			TblPr tblPr = Context.getWmlObjectFactory().createTblPr();
            contentContext.getContent().add(tblPr);
            
            String cssClass = null;
        	if (parent.getElement().getAttribute("class")!=null) {
        	 	cssClass=parent.getElement().getAttribute("class").trim();
        	}
            
//            setTableStyle(tblPr, cssClass, "none");
            setTableStyle(tblPr, cssClass);
			
			Tr tr = Context.getWmlObjectFactory().createTr();
			contentContext.getContent().add(tr);
		    contentContext = tr;            			
			
			Tc tc = Context.getWmlObjectFactory().createTc();
			contentContext.getContent().add(tc);
			// TODO the caller never makes this w:tc the place the inner table goes
			// (lost in 0cd7a41, 2013), so <table><table> still gives a w:tbl directly
			// inside a w:tbl.  See docs/change-requests/CR-002 section 4.
		}
	}
	
	private void setTableStyle(TblPr tblPr, String cssClass) {

		
        if (importer.getTableFormatting().equals(FormattingOption.IGNORE_CLASS)) {
//            tblStyle.setVal(fallbackStyle);
        } else {
        	// CLASS_TO_STYLE_ONLY or CLASS_PLUS_OTHER
        	if (cssClass==null) {
        		// Word 2010 can't open a docx which contains <w:tblStyle/>
        		// so we need to either remove the tblStyle element, 
        		// or 
//                tblStyle.setVal(fallbackStyle);
        	} else {
//        	if (box.getElement()!=null
//        			&& box.getElement().getAttribute("class")!=null) {
//        		String cssClass = box.getElement().getAttribute("class").trim();
        		
        		if (cssClass.equals("")) {
//                    tblStyle.setVal(fallbackStyle);
        		} else {
        			
            		// Our XHTML export gives a space separated list of class names,
            		// reflecting the style hierarchy.  Here, we just want the first one.
            		// TODO, replace this with a configurable stylenamehandler.
            		int pos = cssClass.indexOf(" ");
            		if (pos>-1) {
            			cssClass = cssClass.substring(0,  pos);
            		}
            		
            		// if the docx contains this stylename, set it
            		Style s = importer.stylesByID.get(cssClass);
            		if (s==null) {
            			log.debug("No docx style for @class='" + cssClass + "'");
//                        tblStyle.setVal(fallbackStyle);
            		} else if (s.getType()!=null && s.getType().equals("table")) {
                        TblStyle tblStyle = Context.getWmlObjectFactory().createCTTblPrBaseTblStyle();
                        tblPr.setTblStyle(tblStyle);
                        tblStyle.setVal(cssClass);
            		} else {
            			log.debug("For docx style for @class='" + cssClass + "', but its not a table style ");
//                        tblStyle.setVal(fallbackStyle);
            		}
        		}
        	}
//			if (tableFormatting.equals(FormattingOption.CLASS_PLUS_OTHER)) {
//				addRunProperties(rPr, cssMap );
//			}
        	
    	} 
        
    
		
	}
    
	protected boolean isTableStyle(Style s) {
	    if(s.getType() != null && s.getType().equals(TABLE)) {
	        return true;
	    }
	    return false;
	}
	
    private void setCellWidthAuto(TcPr tcPr) {
    	// <w:tcW w:w="0" w:type="auto"/>
		TblWidth tblW = Context.getWmlObjectFactory().createTblWidth();
		tblW.setW(BigInteger.ZERO);
		tblW.setType(TblWidth.TYPE_AUTO);
		tcPr.setTcW(tblW);    	
    }

    /**
     * Where list item indentation is affected by the presence of tables,
     * we could adjust for this in the numbering, or in an ad hoc property.
     * Which is better?  Ad hoc property is better, since in a contrived
     * example, not all list items are in the table. 
     * See example src/test/resources/numbering/indents_with_tables.html
     * 
     * @return
     */
    protected int tableIndentContrib(LinkedList<ContentAccessor> contentContextStack) {
    	
    	int tblIndents = 0;
    	
    	for (ContentAccessor ca : contentContextStack) {
    		
    		log.debug(ca.getClass().getName());
    		
    		if (ca instanceof Tbl) {
    			Tbl tbl = (Tbl)ca;
    			if (tbl.getTblPr()!=null
    					&& tbl.getTblPr().getTblInd()!=null
    					&& tbl.getTblPr().getTblInd().getW() !=null) {
    				
    				tblIndents = tblIndents + tbl.getTblPr().getTblInd().getW().intValue();
    				
    			}
    		}
    		
    	}
    	
    	log.debug("taking into account tbl indent: " + tblIndents);
    	
    	return tblIndents;
    }
    
	

}

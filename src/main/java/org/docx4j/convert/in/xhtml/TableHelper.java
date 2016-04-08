package org.docx4j.convert.in.xhtml;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.docx4j.UnitsOfMeasurement;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl.TableProperties;
import org.docx4j.jaxb.Context;
import org.docx4j.model.properties.table.tr.TrHeight;
import org.docx4j.org.xhtmlrenderer.css.constants.CSSName;
import org.docx4j.org.xhtmlrenderer.css.constants.IdentValue;
import org.docx4j.org.xhtmlrenderer.css.parser.FSColor;
import org.docx4j.org.xhtmlrenderer.css.parser.FSRGBColor;
import org.docx4j.org.xhtmlrenderer.css.style.CalculatedStyle;
import org.docx4j.org.xhtmlrenderer.css.style.FSDerivedValue;
import org.docx4j.org.xhtmlrenderer.css.style.derived.LengthValue;
import org.docx4j.org.xhtmlrenderer.newtable.TableBox;
import org.docx4j.org.xhtmlrenderer.newtable.TableCellBox;
import org.docx4j.org.xhtmlrenderer.render.Box;
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
import org.w3c.dom.css.CSSValue;

import javax.xml.bind.JAXBElement;

public class TableHelper {
	
	public static Logger log = LoggerFactory.getLogger(TableHelper.class);
	    
	private XHTMLImporterImpl importer;
    protected TableHelper(XHTMLImporterImpl importer) {
    	this.importer=importer;
    }
	
    /**
     * @param cssTable
     * @param tbl
     * @param tableProperties
     * @param isNested - don't adjust indent for table in table
     */
    protected void setupTblPr(TableBox cssTable, Tbl tbl, TableProperties tableProperties, boolean isNested) {
    	
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
			int cssSpacing = cssTable.getStyle().getBorderHSpacing(importer.getRenderer().getLayoutContext());
			spacingWidth.setW( BigInteger.valueOf(cssSpacing  / 2) );	// appears twice thicker, probably taken from both sides 
			spacingWidth.setType(TblWidth.TYPE_DXA);
		}
		tblPr.setTblCellSpacing(spacingWidth); 
		
		// Table indent.  
		// cssTable.getLeftMBP() which is setLeftMBP((int) margin.left() + (int) border.left() + (int) padding.left());
		// cssTable.getTx(); which is (int) margin.left() + (int) border.left() + (int) padding.left();
		// But want just margin.left
		
        log.debug("list depth:" + importer.getListHelper().getDepth());
        if (importer.getListHelper().getDepth()>0
        		&& !isNested) { // don't adjust indent for table in table
        	
        	// Special handling for indent, since we need to sum values for ancestors
    		int totalPadding = importer.getListHelper().getAbsoluteListItemIndent(cssTable);
            
    		TblWidth tblInd = tblPr.getTblInd();
    		if (tblInd==null) {
    			tblInd = new TblWidth();
    			tblPr.setTblInd(tblInd);
    		}
        	// totalPadding gives indent to the bullet;
        	// we want to align this subsequent p with the preceding text;
        	// assume 360 twips
    		tblInd.setW(BigInteger.valueOf(totalPadding + 360 + 115)); // TODO FIX 360, and take cssTable.getMargin() into account
    		tblInd.setType(TblWidth.TYPE_DXA);
            	        	
    		importer.getListHelper().peekListItemStateStack().isFirstChild=false;
            
        } else if (cssTable.getMargin() !=null
				&& cssTable.getMargin().left()>0) {
        	
			log.debug("Calculating TblInd from margin.left: " + cssTable.getMargin().left() );
    		TblWidth tblInd = Context.getWmlObjectFactory().createTblWidth();
    		tblInd.setW( BigInteger.valueOf( Math.round(
    				cssTable.getMargin().left()
    				)));
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
    
    protected void setupTblGrid(TableBox cssTable, Tbl tbl, TableProperties tableProperties) {
    	
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
    		tblGridCol.setW( BigInteger.valueOf(colPos[i]-colPos[i-1]) );
    		
    	}
    	
    }

    protected void setupTrPr(org.docx4j.org.xhtmlrenderer.newtable.TableRowBox trBox, Tr tr) {

	    TrPr trPr = Context.getWmlObjectFactory().createTrPr();
	    tr.setTrPr(trPr);

	    /* Row height:- 
	     * 
	     * In HTML, you can only set height on a cell td, not tr, but Flying Saucer calculates this 
	     * based on cell contents.
	     * 
	     * Since there is no height property on a tr in CSS, this.importer.getCascadedProperties 
	     * does not contain that property.
	     * 
	     * But happily, Flying Saucer, sets trBox.getHeight()
	     * 
	     * A web browser ignores td height 0 (tested in Chrome), or any value less than height of contents.
	     * 
	     * But Flying Saucer, at least the version we modified, sets trBox.getHeight() based on
	     * td CSS values, not the greater of the specified height and the actual height of the contents.
	     * 
	     * So in Word, we want to use STHeightRule.AT_LEAST (rather than EXACT) so contents don't
	     * get cut off. 
	     * 
	     */	    
	    int height = trBox.getHeight(); // 100px = 2000 so looks like 1/20 of a pixel! 
	    if (height == 0) { 
		    // do nothing; equivalent per spec to STHeightRule.AUTO
	    } else {
	    		    	
	    	// Since we don't have a CSSValue we can use, do it manually
	    	TrHeight thr = new TrHeight(); // uses STHeightRule.AT_LEAST
	    	thr.set(trPr);
			int twip = UnitsOfMeasurement.pxToTwip(height/20);
			((CTHeight)thr.getObject()).setVal(BigInteger.valueOf(twip));	    	
	    }	    

    }
    
    protected void setupTcPr(TableCellBox tcb, Tc tc, TableProperties tableProperties) {

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
		
		// padding becomes margin
		TcMar tcMar = Context.getWmlObjectFactory().createTcMar();
		tcPr.setTcMar(tcMar); 

		// .. left
		TblWidth width = getCellMargin(tcb.getStyle(), "left");
		if (width!=null) tcMar.setLeft(width);
		
		// .. right
		width = getCellMargin(tcb.getStyle(), "right");
		if (width!=null) tcMar.setRight(width);

		// .. top
		width = getCellMargin(tcb.getStyle(), "top");
		if (width!=null) tcMar.setTop(width);

		// .. bottom
		width = getCellMargin(tcb.getStyle(), "bottom");
		if (width!=null) tcMar.setBottom(width);
		
    }
		
	private TblWidth getCellMargin(CalculatedStyle tcStyle, String side)  {
		
		TblWidth tblWidth = null;

		FSDerivedValue padding = tcStyle.valueByName( CSSName.getByPropertyName("padding-" + side));
		if (padding != null && padding instanceof LengthValue) {
						
			int twip = UnitsOfMeasurement.pxToTwip(((LengthValue)padding).asFloat());
			if (twip==0) twip = 15; // Default
			
			tblWidth = Context.getWmlObjectFactory().createTblWidth();
			tblWidth.setW(BigInteger.valueOf(twip));
			tblWidth.setType("dxa");
		}
		
		return tblWidth;
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
				CSSName.getByPropertyName("border-"+side+"-width"), 0, importer.getRenderer().getLayoutContext() );

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
		
		String color = borderColor.asString();
		if (color.startsWith("#")) color=color.substring(1); // Fix for https://github.com/plutext/docx4j/issues/101
		//if (color.equals("transparent")) color = "FFFFFF";  // assume white page  
		// should not be getting that as a value?
		// http://stackoverflow.com/questions/7851830/what-is-the-color-code-for-transparency-in-css
		
		return createBorderStyle( stBorder, color, BigInteger.valueOf( Math.round(width) ) );
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
	protected void insertDummyVMergedCells(ContentAccessor trContext, TableCellBox tcb, boolean backwards) {

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
	protected void nestedTableHierarchyFix(ContentAccessor contentContext,
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
            
//            setTableStyle(tblPr, cssClass, "none");
            setTableStyle(tblPr, cssClass);
			
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
    
    private void setCellWidthAuto(TcPr tcPr) {
    	// <w:tcW w:w="0" w:type="auto"/>
		TblWidth tblW = Context.getWmlObjectFactory().createTblWidth();
		tblW.setW(BigInteger.ZERO);
		tblW.setType(TblWidth.TYPE_AUTO);
		tcPr.setTcW(tblW);    	
    }

    
	

}

/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.docx4j.convert.in.xhtml.renderer;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.extend.FSDOMMutator;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.UserInterface;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxFontContext;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver;
import com.openhtmltopdf.pdfboxout.PdfBoxTextRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.CacheStore;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.util.Configuration;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class DocxRenderer {
    
    // These two defaults combine to produce an effective resolution of 96 px to the inch
    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
    private static final int DEFAULT_DOTS_PER_PIXEL = 20;
    // DPI is then set = 72 * dotsPerPoint

	private final SharedContext _sharedContext;
	private final Docx4jDocxOutputDevice _outputDevice;
//    private final List<FSDOMMutator> _domMutators;

	private Document _doc;
	private BlockBox _root;
	public BlockBox getRootBox() {
		return _root;
	}
//	    private final float _dotsPerPoint;
	
	/* FontResolver needs a PDDocument */
	private PDDocument _pdfDoc = new PDDocument();

    private PdfAConformance _pdfAConformance;
    private boolean _pdfUaConformance;

	
	private Docx4jUserAgent userAgent;
	public Docx4jUserAgent getDocx4jUserAgent() {
		return userAgent;
	}

	private LayoutContext _layoutContext;

	public LayoutContext getLayoutContext() {
		return _layoutContext;
	}

	private final float _dotsPerPoint;

	public DocxRenderer() {
		this(new Docx4jUserAgent(), null, DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL);
	}

    public DocxRenderer(String extraCSS) {
        
        this(new Docx4jUserAgent(), readCSS(extraCSS), DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL);
    }
    
	
    public DocxRenderer(Docx4jUserAgent userAgent) {
        this(userAgent, null, DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL);
    }

    public DocxRenderer(float dotsPerPoint, int dotsPerPixel) {
        this(new Docx4jUserAgent(), null, dotsPerPoint,  dotsPerPixel);
    }
    
	public DocxRenderer(Docx4jUserAgent userAgent, StylesheetInfo[] extraCSS, float dotsPerPoint, int dotsPerPixel) {
		_dotsPerPoint = dotsPerPoint;

		_outputDevice = new Docx4jDocxOutputDevice();

//        userAgent = new Docx4jUserAgent(_outputDevice);        
		this.userAgent = userAgent;
		
		
		/**
		 * The SharedContext stores pseudo global variables.
		 * Use a new instance for every run.
		 * So use a new DocxRenderer for every run.
		 */		
		_sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
//        _sharedContext._preferredTransformerFactoryImplementationClass = state._preferredTransformerFactoryImplementationClass;
//        _sharedContext._preferredDocumentBuilderFactoryImplementationClass = state._preferredDocumentBuilderFactoryImplementationClass;
        		
		_sharedContext.setUserAgentCallback(userAgent);

		_sharedContext.setCss(new StyleReference(userAgent));
//		_sharedContext.setCss(new StyleReference(userAgent, extraCSS));

		
		
		//        userAgent.setSharedContext(_sharedContext);
//        _outputDevice.setSharedContext(_sharedContext);

        /* Fonts
         * 
         * We need them in order to calculate size of
         * table cells etc. (which is presumably
         * important for conversion of fixed width tables).
         */
		
        Map<CacheStore, FSCacheEx<String, FSCacheValue>> _caches = new EnumMap<>(CacheStore.class);
		
        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc, 
        		_caches.get(CacheStore.PDF_FONT_METRICS), 
        		_pdfAConformance.NONE, false);
		_sharedContext.setFontResolver(fontResolver);

//        Docx4jFontResolver fontResolver = new Docx4jFontResolver(_sharedContext);
//      _sharedContext.setFontResolver(fontResolver);    

		Docx4jReplacedElementFactory replacedElementFactory =
				new Docx4jReplacedElementFactory(_outputDevice);
		_sharedContext.setReplacedElementFactory(replacedElementFactory);

		_sharedContext.setTextRenderer(new Docx4jTextRenderer());
		_sharedContext.setDPI(72*_dotsPerPoint);
		_sharedContext.setDotsPerPixel(dotsPerPixel);
		_sharedContext.setPrint(true);
		_sharedContext.setInteractive(false);
	}

    private static StylesheetInfo[] readCSS(String css) {
        // adapted from com.openhtmltopdf.simple.extend.XhtmlCssOnlyNamespaceHandler
        
        String media = "all";
        StylesheetInfo info = new StylesheetInfo();
        info.setMedia(media);
        
        info.setType("text/css");
        info.setTitle("Word styles");
        info.setOrigin(StylesheetInfo.AUTHOR);
        
        info.setContent(css);
        
        StylesheetInfo[] array = { info };

        return array;
    }
        
	
	public SharedContext getSharedContext() {
		return _sharedContext;
	}


	public Document loadDocument(final String uri) {
		return _sharedContext.getUac().getXMLResource(uri).getDocument();
	}

	public void setDocument(Document doc, String url) {
		setDocument(doc, url, new XhtmlNamespaceHandler());
	}

	private void setDocument(Document doc, String url, NamespaceHandler nsh) {
		_doc = doc;

//        getFontResolver().flushFontFaceFonts();

//		_sharedContext.reset();
//		if (Configuration.isTrue("xr.cache.stylesheets", true)) {
//			_sharedContext.getCss().flushStyleSheets();
//		} else {
//			_sharedContext.getCss().flushAllStyleSheets();
//		}
		_sharedContext.setBaseURL(url);
		_sharedContext.setNamespaceHandler(nsh);
		_sharedContext.getCss().setDocumentContext(
				_sharedContext, _sharedContext.getNamespaceHandler(),
				doc, new NullUserInterface());
//        getFontResolver().importFontFaces(_sharedContext.getCss().getFontFaceRules());
	}


	public void layout() {
		LayoutContext c = newLayoutContext();
		BlockBox root = BoxBuilder.createRootBox(c, _doc);
		root.setContainingBlock(new ViewportBox(getInitialExtents(c)));
		root.layout(c);

//        Dimension dim = root.getLayer().getPaintingDimension(c);
//        root.getLayer().trimEmptyPages(c, dim.height);
//        root.getLayer().layoutPages(c);

		_root = root;
		_layoutContext = c;
	}

	private Rectangle getInitialExtents(LayoutContext c) {
		PageBox first = Layer.createPageBox(c, "first");

		return new Rectangle(0, 0, first.getContentWidth(c), first.getContentHeight(c));
	}


	private LayoutContext newLayoutContext() {
//		_sharedContext.getTextRenderer().setup(result.getFontContext());
//
		
        LayoutContext result = _sharedContext.newLayoutContextInstance();
        result.setFontContext(new PdfBoxFontContext());
        
//        if (_splitterFactory != null)
//            result.setBidiSplitterFactory(_splitterFactory);
//        
//        if (_reorderer != null)
//        	result.setBidiReorderer(_reorderer);
//
//        result.setDefaultTextDirection(_defaultTextDirection);

        ((PdfBoxTextRenderer) _sharedContext.getTextRenderer()).setup(
//        		result.getFontContext(), _reorderer != null ? _reorderer : new SimpleBidiReorderer());
        		result.getFontContext(), new SimpleBidiReorderer());

        return result;
		
	}


	private static final class NullUserInterface implements UserInterface {
		public boolean isHover(Element e) {
			return false;
		}

		public boolean isActive(Element e) {
			return false;
		}

		public boolean isFocus(Element e) {
			return false;
		}
	}
}

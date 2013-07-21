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
package org.docx4j.org.xhtmlrenderer.docx;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

import org.docx4j.org.xhtmlrenderer.context.StyleReference;
import org.docx4j.org.xhtmlrenderer.css.style.CalculatedStyle;
import org.docx4j.org.xhtmlrenderer.extend.NamespaceHandler;
import org.docx4j.org.xhtmlrenderer.extend.UserInterface;
import org.docx4j.org.xhtmlrenderer.layout.BoxBuilder;
import org.docx4j.org.xhtmlrenderer.layout.Layer;
import org.docx4j.org.xhtmlrenderer.layout.LayoutContext;
import org.docx4j.org.xhtmlrenderer.layout.SharedContext;
import org.docx4j.org.xhtmlrenderer.pdf.ITextFontContext;
import org.docx4j.org.xhtmlrenderer.pdf.ITextFontResolver;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.docx4j.org.xhtmlrenderer.render.PageBox;
import org.docx4j.org.xhtmlrenderer.render.RenderingContext;
import org.docx4j.org.xhtmlrenderer.render.ViewportBox;
import org.docx4j.org.xhtmlrenderer.resource.XMLResource;
import org.docx4j.org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.docx4j.org.xhtmlrenderer.util.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.lowagie.text.DocumentException;


public class DocxRenderer {
////    // These two defaults combine to produce an effective resolution of 96 px to the inch
//    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
//    private static final int DEFAULT_DOTS_PER_PIXEL = 20;

	//  // These two defaults combine to produce an effective resolution of 96 px to the inch
	private static final float DEFAULT_DOTS_PER_POINT = 20f;
	private static final int DEFAULT_DOTS_PER_PIXEL = 20;


	private final SharedContext _sharedContext;
	private final Docx4jDocxOutputDevice _outputDevice;

	private Docx4jUserAgent userAgent;
	public Docx4jUserAgent getDocx4jUserAgent() {
		return userAgent;
	}

	private Document _doc;


	private BlockBox _root;
	public BlockBox getRootBox() {
		return _root;
	}

	private LayoutContext _layoutContext;

	public LayoutContext getLayoutContext() {
		return _layoutContext;
	}

	private final float _dotsPerPoint;

	public DocxRenderer() {
		this(DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL);
	}

	public DocxRenderer(float dotsPerPoint, int dotsPerPixel) {
		_dotsPerPoint = dotsPerPoint;

		_outputDevice = new Docx4jDocxOutputDevice();

//        userAgent = new Docx4jUserAgent(_outputDevice);        
		userAgent = new Docx4jUserAgent();
		_sharedContext = new SharedContext();
		_sharedContext.setUserAgentCallback(userAgent);
		_sharedContext.setCss(new StyleReference(userAgent));
//        userAgent.setSharedContext(_sharedContext);
//        _outputDevice.setSharedContext(_sharedContext);

        /* Fonts
         * 
         * We need them in order to calculate size of
         * table cells etc. (which is presumably
         * important for conversion of fixed width tables).
         * 
         * Thinking re font resolution:-
         * 
         * I don't really want a dependency on:
         * 
            <groupId>com.lowagie</groupId>
            <artifactId>itext</artifactId>
            <version>2.1.7</version>
         *
         * So it is desirable to have a font resolver 
         * which uses the docx4j font stuff
         * (which is mainly FOP's EmbedFontInfo).
         * 
         * When the time comes to make this,
         * package org.docx4j.fonts should probably be
         * made into a separate project
         * (so xhtmlrenderer isn't dependent on docx4j).
         * 
         * It is expedient to use ITextFontResolver
         * (and to pay the dependency cost), so that
         * this release can focus on getting cell widths
         * right.
         * 
         * In a later release, I'll try to get rid
         * of the iText dependency.
         *   
         */
		ITextFontResolver fontResolver = new ITextFontResolver(_sharedContext);
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

		_sharedContext.reset();
		if (Configuration.isTrue("xr.cache.stylesheets", true)) {
			_sharedContext.getCss().flushStyleSheets();
		} else {
			_sharedContext.getCss().flushAllStyleSheets();
		}
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
		LayoutContext result = _sharedContext.newLayoutContextInstance();
		result.setFontContext(new ITextFontContext());

		_sharedContext.getTextRenderer().setup(result.getFontContext());

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

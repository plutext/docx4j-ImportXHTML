/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2026, Plutext Pty Ltd, and contributors.
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
package org.docx4j.convert.in.xhtml.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.docx4j.Docx4jProperties;
import org.docx4j.TraversalUtil;
import org.docx4j.TraversalUtil.CallbackImpl;
import org.docx4j.XmlUtils;
import org.docx4j.model.datastorage.BindingHandler;
import org.docx4j.model.datastorage.CustomXmlDataStorage;
import org.docx4j.model.datastorage.CustomXmlDataStorageImpl;
import org.docx4j.model.datastorage.XHTMLImporterFormatting;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePart;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePropertiesPart;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.opendope.XPathsPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.wml.CTDataBinding;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;
import org.docx4j.wml.SdtPr;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tag;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Text;
import org.opendope.xpaths.Xpaths;
import org.junit.After;
import org.junit.Test;

/**
 * docx4j CR-013 phase 3: the OpenDoPE binding traversers configure the
 * XHTMLImporterImpl they construct from three docx4j.properties keys
 * (docx4j.model.datastorage.BindingTraverser.XHTML.{Run,Paragraph,Table}Formatting).
 *
 * docx4j-core-tests can't exercise that end to end, since ImportXHTML isn't
 * on its classpath; this module has both, so the real coverage lives here.
 *
 * Builds a package programmatically: a custom XML part holding escaped XHTML,
 * an XPaths part with one entry, and an od:ContentType=application/xhtml+xml
 * content control pointing at it.  No binary test resource.
 */
public class BindingFormattingOptionsTest {

	private static final String IMPLEMENTATION = "docx4j.model.datastorage.BindingHandler.Implementation";
	private static final String CLASS_TO_STYLE_ONLY = XHTMLImporterFormatting.CLASS_TO_STYLE_ONLY;

	/** @class names a paragraph style, a character style and a table style; each carries inline CSS too */
	private static final String XHTML = "<div>"
			+ "<p class=\"MyPara\" style=\"margin-left:36pt;text-align:center\">"
			+ "<span class=\"Strong\" style=\"color:#FF0000;font-family:Arial;font-weight:bold\">bound</span>"
			+ "</p>"
			+ "<table class=\"MyTable\" style=\"width:50%\"><tr><td>cell</td></tr></table>"
			+ "</div>";

	@After
	public void resetProperties() {
		Docx4jProperties.getProperties().remove(XHTMLImporterFormatting.RUN);
		Docx4jProperties.getProperties().remove(XHTMLImporterFormatting.PARAGRAPH);
		Docx4jProperties.getProperties().remove(XHTMLImporterFormatting.TABLE);
		Docx4jProperties.getProperties().remove(IMPLEMENTATION);
	}

	// ---- the cases

	@Test public void unsetKeysGiveStylePlusDirectFormatting() throws Exception {
		// today's behaviour: CLASS_PLUS_OTHER at every level
		Bound bound = bind();

		assertEquals("MyPara", bound.pPr.getPStyle().getVal());
		assertNotNull("indent from CSS expected", bound.pPr.getInd());
		assertNotNull("jc from CSS expected", bound.pPr.getJc());

		assertEquals("Strong", bound.rPr.getRStyle().getVal());
		assertNotNull("color from CSS expected", bound.rPr.getColor());
		assertNotNull("rFonts from CSS expected", bound.rPr.getRFonts());
		assertNotNull("bold from CSS expected", bound.rPr.getB());

		assertEquals("MyTable", bound.tbl.getTblPr().getTblStyle().getVal());
	}

	@Test public void classToStyleOnlyAtEveryLevel() throws Exception {
		Docx4jProperties.setProperty(XHTMLImporterFormatting.RUN, CLASS_TO_STYLE_ONLY);
		Docx4jProperties.setProperty(XHTMLImporterFormatting.PARAGRAPH, CLASS_TO_STYLE_ONLY);
		Docx4jProperties.setProperty(XHTMLImporterFormatting.TABLE, CLASS_TO_STYLE_ONLY);
		Bound bound = bind();

		assertStylesOnly(bound);
	}

	@Test public void classToStyleOnlyViaXsltTraverser() throws Exception {
		// convertXHTML is shared by all three traversers; confirm the XSLT one too
		Docx4jProperties.setProperty(IMPLEMENTATION, "BindingTraverserXSLT");
		Docx4jProperties.setProperty(XHTMLImporterFormatting.RUN, CLASS_TO_STYLE_ONLY);
		Docx4jProperties.setProperty(XHTMLImporterFormatting.PARAGRAPH, CLASS_TO_STYLE_ONLY);
		Docx4jProperties.setProperty(XHTMLImporterFormatting.TABLE, CLASS_TO_STYLE_ONLY);
		Bound bound = bind();

		assertStylesOnly(bound);
	}

	@Test public void runLevelOnlyLeavesParagraphCss() throws Exception {
		// the three keys are independent
		Docx4jProperties.setProperty(XHTMLImporterFormatting.RUN, CLASS_TO_STYLE_ONLY);
		Bound bound = bind();

		assertEquals("MyPara", bound.pPr.getPStyle().getVal());
		assertNotNull("paragraph CSS still wanted", bound.pPr.getInd());

		assertEquals("Strong", bound.rPr.getRStyle().getVal());
		assertNull(bound.rPr.getColor());
		assertNull(bound.rPr.getRFonts());
		assertNull(bound.rPr.getB());
	}

	@Test public void unknownValueIsIgnored() throws Exception {
		// warn-and-skip: behaves as if unset
		Docx4jProperties.setProperty(XHTMLImporterFormatting.RUN, "NO_SUCH_OPTION");
		Bound bound = bind();

		assertEquals("Strong", bound.rPr.getRStyle().getVal());
		assertNotNull(bound.rPr.getColor());
	}

	private void assertStylesOnly(Bound bound) {
		assertEquals("MyPara", bound.pPr.getPStyle().getVal());
		assertNull("no indent: paragraph CSS discarded", bound.pPr.getInd());
		assertNull("no jc: paragraph CSS discarded", bound.pPr.getJc());

		assertEquals("Strong", bound.rPr.getRStyle().getVal());
		assertNull("no color: run CSS discarded", bound.rPr.getColor());
		assertNull("no rFonts: run CSS discarded (ImportXHTML CR-001)", bound.rPr.getRFonts());
		assertNull("no bold: run CSS discarded", bound.rPr.getB());

		assertEquals("MyTable", bound.tbl.getTblPr().getTblStyle().getVal());
	}

	// ---- plumbing

	/** what the bound content control ended up containing */
	private static class Bound {
		PPr pPr;
		RPr rPr;
		Tbl tbl;
	}

	private Bound bind() throws Exception {

		WordprocessingMLPackage pkg = buildPackage();

		BindingHandler bh = new BindingHandler(pkg);
		bh.applyBindings(pkg.getMainDocumentPart());

		// must still marshal
		XmlUtils.marshaltoString(pkg.getMainDocumentPart().getJaxbElement());

		final Bound bound = new Bound();
		new TraversalUtil(pkg.getMainDocumentPart().getContent(), new CallbackImpl() {
			@Override
			public List<Object> apply(Object o) {
				if (o instanceof Text && "bound".equals(((Text) o).getValue())) {
					R r = (R) ((org.jvnet.jaxb.lang.Child) o).getParent();
					P p = (P) r.getParent();
					bound.rPr = r.getRPr();
					bound.pPr = p.getPPr();
				} else if (o instanceof Tbl && bound.tbl == null) {
					bound.tbl = (Tbl) o;
				}
				return null;
			}
		});
		assertNotNull("bound run not found", bound.rPr);
		assertNotNull("bound paragraph has no pPr", bound.pPr);
		assertNotNull("bound table not found", bound.tbl);
		return bound;
	}

	private WordprocessingMLPackage buildPackage() throws Exception {

		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		MainDocumentPart mdp = pkg.getMainDocumentPart();
		ObjectFactory factory = new ObjectFactory();

		// styles for @class to map to
		List<Style> styles = mdp.getStyleDefinitionsPart().getJaxbElement().getStyle();
		styles.add(style(factory, "paragraph", "MyPara"));
		styles.add(style(factory, "character", "Strong"));
		styles.add(style(factory, "table", "MyTable"));

		// custom XML part: <root><html>escaped xhtml</html></root>
		String itemId = "{" + UUID.randomUUID().toString().toUpperCase() + "}";

		CustomXmlDataStoragePart dataPart = new CustomXmlDataStoragePart(new PartName("/customXml/item1.xml"));
		CustomXmlDataStorage data = new CustomXmlDataStorageImpl();
		data.setDocument(XmlUtils.getNewDocumentBuilder().parse(
				new org.xml.sax.InputSource(new java.io.StringReader(
						"<root><html>" + escape(XHTML) + "</html></root>"))));
		dataPart.setData(data);
		mdp.addTargetPart(dataPart);

		CustomXmlDataStoragePropertiesPart propsPart = new CustomXmlDataStoragePropertiesPart();
		org.docx4j.customXmlProperties.DatastoreItem dsi = new org.docx4j.customXmlProperties.ObjectFactory().createDatastoreItem();
		dsi.setItemID(itemId);
		propsPart.setJaxbElement(dsi);
		dataPart.addTargetPart(propsPart);
		pkg.getCustomXmlDataStorageParts().put(itemId.toLowerCase(), dataPart);

		// XPaths part with one entry
		Xpaths xpaths = new org.opendope.xpaths.ObjectFactory().createXpaths();
		Xpaths.Xpath xpath = new Xpaths.Xpath();
		xpath.setId("x1");
		Xpaths.Xpath.DataBinding xdb = new Xpaths.Xpath.DataBinding();
		xdb.setStoreItemID(itemId);
		xdb.setXpath("/root/html");
		xdb.setPrefixMappings("");
		xpath.setDataBinding(xdb);
		xpaths.getXpath().add(xpath);

		XPathsPart xPathsPart = new XPathsPart(new PartName("/customXml/item2.xml"));
		xPathsPart.setJaxbElement(xpaths);
		mdp.addTargetPart(xPathsPart);

		// the XHTML content control
		SdtBlock sdt = factory.createSdtBlock();
		SdtPr sdtPr = factory.createSdtPr();
		sdt.setSdtPr(sdtPr);
		Tag tag = factory.createTag();
		tag.setVal("od:xpath=x1&od:ContentType=application/xhtml+xml");
		sdtPr.setTag(tag);
		sdtPr.setId();
		CTDataBinding db = factory.createCTDataBinding();
		db.setStoreItemID(itemId);
		db.setXpath("/root/html");
		db.setPrefixMappings("");
		sdtPr.setDataBinding(db);

		SdtContentBlock content = factory.createSdtContentBlock();
		sdt.setSdtContent(content);
		content.getContent().add(mdp.createParagraphOfText("placeholder"));
		mdp.getContent().add(sdt);

		return pkg;
	}

	private static Style style(ObjectFactory factory, String type, String id) {
		Style s = factory.createStyle();
		s.setType(type);
		s.setStyleId(id);
		return s;
	}

	private static String escape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

}

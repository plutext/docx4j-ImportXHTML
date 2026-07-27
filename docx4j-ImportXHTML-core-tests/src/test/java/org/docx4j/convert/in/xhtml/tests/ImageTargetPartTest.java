/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2026, Plutext Pty Ltd, and contributors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.docx4j.convert.in.xhtml.XHTMLImageHandlerDefault;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.Hdr;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Images are added as a relationship of the main document part by default, but
 * XHTMLImageHandlerDefault.setTargetPart lets you put them somewhere else.
 *
 * Relationship ids are resolved per part, so content destined for a header or
 * footer needs its images to be relationships of that part; otherwise the
 * r:embed can't be resolved, and Word shows a missing image.
 *
 * @since 17.0.1
 */
public class ImageTargetPartTest {

	// 2x2 pixels
	private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";
	private static final String PNG_DATA_URI = "data:image/png;base64," + PNG_BASE64;

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	@Test
	public void testImageBelongsToMainDocumentPartByDefault() throws Exception {

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		List<Object> content = importer.convert(img(PNG_DATA_URI), null);

		Assert.assertEquals(1, imageRelationships(wordMLPackage.getMainDocumentPart()).size());
		Assert.assertEquals(getEmbedId(content),
				imageRelationships(wordMLPackage.getMainDocumentPart()).get(0).getId());
	}

	@Test
	public void testImageBelongsToTargetPart() throws Exception {

		HeaderPart header = addHeaderPart();

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		XHTMLImageHandlerDefault imageHandler = new XHTMLImageHandlerDefault(importer);
		imageHandler.setTargetPart(header);
		importer.setXHTMLImageHandler(imageHandler);

		List<Object> content = importer.convert(img(PNG_DATA_URI), null);
		header.getJaxbElement().getContent().addAll(content);

		Assert.assertEquals("image should not be a relationship of the main document part",
				0, imageRelationships(wordMLPackage.getMainDocumentPart()).size());
		Assert.assertEquals("image should be a relationship of the header",
				1, imageRelationships(header).size());
	}

	/**
	 * The r:embed must be resolvable in the part the content is added to.
	 */
	@Test
	public void testEmbedIdResolvesInTargetPart() throws Exception {

		HeaderPart header = addHeaderPart();

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		XHTMLImageHandlerDefault imageHandler = new XHTMLImageHandlerDefault(importer);
		imageHandler.setTargetPart(header);
		importer.setXHTMLImageHandler(imageHandler);

		List<Object> content = importer.convert(img(PNG_DATA_URI), null);
		header.getJaxbElement().getContent().addAll(content);

		String embedId = getEmbedId(content);
		Assert.assertNotNull(embedId);
		Assert.assertNotNull("r:embed " + embedId + " must resolve in the header",
				header.getRelationshipsPart().getRelationshipByID(embedId));
	}

	/**
	 * Image parts are cached by @src, but a cached part is a relationship of the
	 * part it was created for, so it must not be reused for a different target.
	 * (Only images which aren't data URIs are cached.)
	 */
	@Test
	public void testCachedImageIsNotReusedForADifferentTargetPart() throws Exception {

		File imageFile = writeTempImage();
		String html = img(imageFile.toURI().toString());

		HeaderPart header = addHeaderPart();

		// The cache belongs to the handler, so share one handler; but use a new
		// importer per conversion, since an importer accumulates its content.
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		XHTMLImageHandlerDefault imageHandler = new XHTMLImageHandlerDefault(importer);
		importer.setXHTMLImageHandler(imageHandler);

		// first, to the main document part
		List<Object> body = importer.convert(html, null);
		wordMLPackage.getMainDocumentPart().getContent().addAll(body);

		// then the same image, to the header
		XHTMLImporterImpl headerImporter = new XHTMLImporterImpl(wordMLPackage);
		imageHandler.setTargetPart(header);
		headerImporter.setXHTMLImageHandler(imageHandler);

		List<Object> headerContent = headerImporter.convert(html, null);
		header.getJaxbElement().getContent().addAll(headerContent);

		Assert.assertEquals("main document part keeps its own image",
				1, imageRelationships(wordMLPackage.getMainDocumentPart()).size());
		Assert.assertEquals("header needs its own image relationship",
				1, imageRelationships(header).size());

		Assert.assertNotNull("header r:embed must resolve in the header",
				header.getRelationshipsPart().getRelationshipByID(getEmbedId(headerContent)));
		Assert.assertNotNull("main document r:embed must still resolve there",
				wordMLPackage.getMainDocumentPart().getRelationshipsPart()
						.getRelationshipByID(getEmbedId(body)));
	}

	/**
	 * The cache should still do its job when the target part hasn't changed:
	 * the same image used twice results in one image part, not two.
	 */
	@Test
	public void testImageIsStillCachedWhenTargetPartUnchanged() throws Exception {

		File imageFile = writeTempImage();
		String html = img(imageFile.toURI().toString());

		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		XHTMLImageHandlerDefault imageHandler = new XHTMLImageHandlerDefault(importer);
		importer.setXHTMLImageHandler(imageHandler);

		List<Object> first = importer.convert(html, null);

		XHTMLImporterImpl importer2 = new XHTMLImporterImpl(wordMLPackage);
		importer2.setXHTMLImageHandler(imageHandler);
		List<Object> second = importer2.convert(html, null);

		Assert.assertEquals("the image part should have been reused",
				1, imageRelationships(wordMLPackage.getMainDocumentPart()).size());
		Assert.assertEquals(getEmbedId(first), getEmbedId(second));
	}

	private String img(String src) {
		return "<div><img src='" + src + "'/></div>";
	}

	private HeaderPart addHeaderPart() throws Exception {
		HeaderPart header = new HeaderPart(new PartName("/word/header1.xml"));
		header.setJaxbElement(new Hdr());
		wordMLPackage.getMainDocumentPart().addTargetPart(header);
		return header;
	}

	private File writeTempImage() throws Exception {
		File f = File.createTempFile("docx4j-ImportXHTML-test", ".png");
		f.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(f);
		try {
			fos.write(Base64.decodeBase64(PNG_BASE64.getBytes("UTF8")));
		} finally {
			fos.close();
		}
		return f;
	}

	/**
	 * @return the image relationships of the part (never null)
	 */
	private List<Relationship> imageRelationships(Part part) {

		List<Relationship> imageRels = new ArrayList<Relationship>();

		RelationshipsPart rels = part.getRelationshipsPart();
		if (rels == null) {
			return imageRels;
		}
		for (Relationship r : rels.getRelationships().getRelationship()) {
			if (Namespaces.IMAGE.equals(r.getType())) {
				imageRels.add(r);
			}
		}
		return imageRels;
	}

	/**
	 * @return the r:embed of the first image in the converted content
	 */
	private String getEmbedId(List<Object> content) {

		Inline inline = (Inline)((Drawing)((R)((P)content.get(0)).getContent().get(0))
				.getContent().get(0)).getAnchorOrInline().get(0);

		return inline.getGraphic().getGraphicData().getPic().getBlipFill().getBlip().getEmbed();
	}
}

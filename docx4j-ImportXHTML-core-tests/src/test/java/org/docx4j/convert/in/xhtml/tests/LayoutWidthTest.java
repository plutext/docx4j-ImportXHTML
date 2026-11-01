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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Assert;
import org.junit.Test;

/**
 * The XHTML is laid out at the docx's text width: the writable width of the last section, or
 * a width given to setTextWidth (CR-009).  Before, it was laid out on openhtmltopdf's A4, less
 * 0.5in margins and the body's 8px, whatever the docx's page: width: 100% was 7.1in on every
 * page, so 73% of a landscape A4 line.
 */
public class LayoutWidthTest {

	private static final String COLLAPSED = "table{border-collapse:collapse}td{padding:0}";
	private static final String TWO = "<tr><td>a</td><td>b</td></tr>";

	@Test
	public void percentWidthsAreOfTheLine() throws Exception {
		Object[][] pages = {{PageSizePaper.A4, false}, {PageSizePaper.LETTER, true}, {PageSizePaper.A3, false}};
		for (Object[] page : pages) {
			WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage((PageSizePaper) page[0], (Boolean) page[1]);
			int text = textWidth(pkg);
			String which = page[0] + (((Boolean) page[1]) ? " landscape" : "") + ", text " + text;

			String xml = convert(pkg, COLLAPSED, "<table style=\"width:100%\">" + TWO + "</table>");
			Assert.assertEquals(which + ": " + xml, text, grid(xml), 2);

			xml = convert(pkg, COLLAPSED, "<table style=\"width:50%\">" + TWO + "</table>");
			Assert.assertEquals(which + ": " + xml, text / 2, grid(xml), 2);

			// a table that fills its line, for its content
			xml = convert(pkg, COLLAPSED, "<table><tr><td>" + "Lorem ipsum dolor sit amet. ".repeat(30) + "</td></tr></table>");
			Assert.assertEquals(which + ": " + xml, text, grid(xml), 2);

			xml = convert(pkg, "", "<p><img src=\"" + png() + "\" style=\"width:100%\"/></p>");
			Assert.assertEquals(which + ": " + xml, text * 635L, extentCx(xml), 2 * 635);
		}
	}

	@Test
	public void aCentredTableIsCentredOnTheLine() throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		String xml = convert(pkg, COLLAPSED, "<table style=\"width:2in;margin:0 auto\">" + TWO + "</table>");
		Matcher m = Pattern.compile("<w:tblInd w:w=\"(\\d+)\"").matcher(xml);
		Assert.assertTrue(xml, m.find());
		Assert.assertEquals(xml, (textWidth(pkg) - 2880) / 2, Integer.parseInt(m.group(1)), 2);
	}

	@Test
	public void theBodysMarginsAreOutsideTheLine() throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		String xml = convert(pkg, COLLAPSED + "body{margin:0 1in;padding-left:0.5in;border-right:6px solid red}",
				"<table style=\"width:100%\">" + TWO + "</table>");
		Assert.assertEquals(xml, textWidth(pkg), grid(xml), 2);
	}

	@Test
	public void setTextWidth() throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		XHTMLImporterImpl importer = new XHTMLImporterImpl(pkg);
		importer.setTextWidth(4320);
		List<Object> content = importer.convert(html(COLLAPSED, "<table style=\"width:100%\">" + TWO + "</table>"), null);
		String xml = marshal(content);
		Assert.assertEquals(xml, 4320, grid(xml), 2);

		// convert returns all that the importer has converted, so only what follows the first
		int before = content.size();
		importer.setTextWidth(null);
		content = importer.convert(html(COLLAPSED, "<table style=\"width:100%\">" + TWO + "</table>"), null);
		xml = marshal(content.subList(before, content.size()));
		Assert.assertEquals(xml, textWidth(pkg), grid(xml), 2);
	}

	@Test
	public void theDocumentsPageDoesNotSetTheWidth() throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		String xml = convert(pkg, COLLAPSED + "@page{size:11in 8.5in;margin:1in}", "<table style=\"width:100%\">" + TWO + "</table>");
		Assert.assertEquals(xml, textWidth(pkg), grid(xml), 2);
	}

	@Test
	public void noSectPr() throws Exception {
		// openhtmltopdf's A4, less 0.5in margins and the body's 8px, as before
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		pkg.getMainDocumentPart().getJaxbElement().getBody().setSectPr(null);
		String xml = convert(pkg, COLLAPSED, "<table style=\"width:100%\">" + TWO + "</table>");
		Assert.assertEquals(xml, 10224, grid(xml), 30);
	}

	private static int textWidth(WordprocessingMLPackage pkg) {
		List<SectionWrapper> sections = pkg.getDocumentModel().getSections();
		return sections.get(sections.size() - 1).getPageDimensions().getWritableWidthTwips();
	}

	private static int grid(String xml) {
		int sum = 0;
		Matcher m = Pattern.compile("<w:gridCol w:w=\"(\\d+)\"/>").matcher(xml);
		while (m.find()) sum += Integer.parseInt(m.group(1));
		return sum;
	}

	private static long extentCx(String xml) {
		Matcher m = Pattern.compile("<wp:extent cx=\"(\\d+)\"").matcher(xml);
		Assert.assertTrue(xml, m.find());
		return Long.parseLong(m.group(1));
	}

	private static String png() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), "png", bos);
		return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
	}

	private static String html(String style, String body) {
		return "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>" + style + "</style></head><body>"
				+ body + "</body></html>";
	}

	private static String convert(WordprocessingMLPackage pkg, String style, String body) throws Exception {
		return marshal(new XHTMLImporterImpl(pkg).convert(html(style, body), null));
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

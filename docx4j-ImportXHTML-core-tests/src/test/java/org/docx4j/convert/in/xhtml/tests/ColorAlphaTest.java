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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.PresentationMLPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.PresentationML.MainPresentationPart;
import org.docx4j.openpackaging.parts.PresentationML.SlideLayoutPart;
import org.docx4j.openpackaging.parts.PresentationML.SlidePart;
import org.junit.Assert;
import org.junit.Test;
import org.pptx4j.convert.in.xhtml.XHTMLtoPPTX;

/**
 * Colours with alpha (CR-006): Word's have none, so a fully transparent colour is none,
 * and partial alpha is blended over white.  openhtmltopdf parses rgba(), hsl() and
 * hsla() from 1.1.100; before, it dropped them.  Every w:color is ST_HexColor.
 */
public class ColorAlphaTest {

	private static final Pattern W_COLOR = Pattern.compile("w:color=\"([^\"]*)\"");

	@Test
	public void noneWhereTransparent() throws Exception {
		String xml = convert(
				"<p>run <span style=\"background-color: rgba(0,0,0,0)\">SPAN</span></p>"
				+ "<p style=\"background-color: rgba(0,0,0,0)\">PARAGRAPH</p>"
				+ "<table><tr><td style=\"background-color: rgba(0,0,0,0)\">CELL</td></tr></table>");
		Assert.assertTrue(xml, xml.contains("SPAN") && xml.contains("PARAGRAPH") && xml.contains("CELL"));
		Assert.assertFalse(xml, xml.contains("<w:shd"));
		Assert.assertFalse(xml, xml.contains("<w:highlight"));
	}

	@Test
	public void partialAlphaOverWhite() throws Exception {
		String xml = convert(
				"<p><span style=\"color: rgba(200,0,0,0.5)\">TEXT</span></p>"
				+ "<p style=\"background-color: rgba(255,255,0,0.4)\">PARAGRAPH</p>"
				+ "<table><tr><td style=\"background-color: hsla(0,100%,50%,0.5)\">CELL</td></tr></table>")
				.toLowerCase();
		Assert.assertTrue(xml, xml.contains("<w:color w:val=\"e48080\"/>"));
		Assert.assertTrue(xml, xml.contains("w:fill=\"ffff99\""));
		Assert.assertTrue(xml, xml.contains("w:fill=\"ff8080\""));
	}

	@Test
	public void opaqueAsBefore() throws Exception {
		String xml = convert(
				"<p><span style=\"color: #c00000\">HEX</span> <span style=\"color: rgb(0,0,192)\">RGB</span>"
				+ " <span style=\"color: hsl(120,100%,25%)\">HSL</span></p>").toLowerCase();
		Assert.assertTrue(xml, xml.contains("<w:color w:val=\"c00000\"/>"));
		Assert.assertTrue(xml, xml.contains("<w:color w:val=\"0000c0\"/>"));
		Assert.assertTrue(xml, xml.contains("<w:color w:val=\"008000\"/>"));
	}

	@Test
	public void bordersHexOrNone() throws Exception {
		String xml = convert(
				"<table style=\"border-collapse: collapse\"><tr>"
				+ "<td style=\"border: 1px solid rgba(0,0,255,0.5)\">HALF</td>"
				+ "<td style=\"border: 1px solid transparent\">TRANSPARENT</td>"
				+ "<td style=\"border: 1px solid rgba(0,0,0,0)\">ALPHA0</td>"
				+ "</tr></table>"
				+ "<p style=\"border: 1px solid transparent\">PARAGRAPH</p>"
				+ "<p style=\"border: 1px solid rgba(255,0,0,0.5)\">HALFRED</p>");
		Matcher m = W_COLOR.matcher(xml);
		while (m.find()) {
			Assert.assertTrue(m.group(1) + " in " + xml, m.group(1).matches("auto|[0-9A-Fa-f]{6}"));
		}
		String lower = xml.toLowerCase();
		Assert.assertTrue(xml, lower.contains("w:color=\"8080ff\""));
		Assert.assertTrue(xml, lower.contains("w:color=\"ff8080\""));
		Assert.assertTrue(xml, xml.contains("w:val=\"nil\""));
	}

	/**
	 * XHTMLtoPPTX takes the same rule, though docx4j doesn't yet put a text colour on a
	 * pptx run (FontColor.set(CTTextCharacterProperties) is a TODO), so only the text shows.
	 */
	@Test
	public void pptxConverts() throws Exception {
		SlidePart slide = slide();
		List<Object> shapes = new XHTMLtoPPTX((PresentationMLPackage) slide.getPackage(), slide,
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>"
				+ "<p><span style=\"color: rgba(200,0,0,0.5)\">TEXT</span>"
				+ " <span style=\"color: rgba(0,0,0,0)\">INVISIBLE</span></p>"
				+ "</body></html>", null).convertSingleSlide();
		StringBuilder sb = new StringBuilder();
		for (Object o : shapes) sb.append(XmlUtils.marshaltoString(o, true, true, org.pptx4j.jaxb.Context.jcPML));
		String xml = sb.toString().toLowerCase();
		Assert.assertTrue(xml, xml.contains(">text<") && xml.contains(">invisible<"));
	}

	private static String convert(String body) throws Exception {
		return marshal(new XHTMLImporterImpl(WordprocessingMLPackage.createPackage()).convert(
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>" + body + "</body></html>", null));
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}

	/** A slide in a new package, as XHTMLFragmentToSlide makes one */
	private static SlidePart slide() throws Exception {
		PresentationMLPackage pml = PresentationMLPackage.createPackage();
		MainPresentationPart pp = (MainPresentationPart) pml.getParts().getParts().get(new PartName("/ppt/presentation.xml"));
		SlideLayoutPart layout = (SlideLayoutPart) pml.getParts().getParts().get(new PartName("/ppt/slideLayouts/slideLayout1.xml"));
		SlidePart slide = new SlidePart(new PartName("/ppt/slides/slide1.xml"));
		slide.setContents(SlidePart.createSld());
		pp.addSlide(0, slide);
		slide.addTargetPart(layout);
		return slide;
	}
}

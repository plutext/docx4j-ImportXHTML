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
package org.docx4j.convert.in.xhtml.bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

/** Loaded by BundleIT in a class loader holding only the two jars. */
public class Smoke {

	public static String run() throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		MainDocumentPart mdp = pkg.getMainDocumentPart();

		// ImportXHTML directly
		List<Object> content = new XHTMLImporterImpl(pkg).convert(
				"<html xmlns='http://www.w3.org/1999/xhtml'><body><h1>SMOKE</h1><ol><li>one</li></ol>"
				+ "<table border='1'><caption>CAP</caption><tr><td>A</td></tr></table></body></html>", null);
		mdp.getContent().addAll(content);

		// docx4j's altChunk conversion: jsoup, then ImportXHTML
		mdp.addAltChunk(AltChunkType.Html, ("<html><body><table border=1>"
				+ "<thead><tr><td>R_HEAD</td></tr></thead>"
				+ "<tfoot style='display:none'><tr><td>R_FOOT</td></tr></tfoot>"
				+ "<tbody><tr><td>R_BODY</td></tr></tbody></table><p>VISIBLE<br></p></body></html>").getBytes("UTF-8"));
		mdp.convertAltChunks();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		pkg.save(out);
		String xml = XmlUtils.marshaltoString(WordprocessingMLPackage.load(new ByteArrayInputStream(out.toByteArray()))
				.getMainDocumentPart().getJaxbElement(), true, false);
		for (String s : new String[] {"SMOKE", "CAP", "w:numPr", "VISIBLE"}) {
			if (!xml.contains(s)) throw new AssertionError("missing " + s);
		}
		int h = xml.indexOf("R_HEAD"), f = xml.indexOf("R_FOOT"), b = xml.indexOf("R_BODY");
		return (h < f && f < b ? "HEAD FOOT BODY" : "rows " + h + " " + f + " " + b)
				+ (xml.contains("w:hidden") ? ", hidden" : ", not hidden")
				+ (from(XHTMLImporterImpl.class) && from(Class.forName("org.jsoup.Jsoup")) ? ", ImportXHTML and jsoup from the bundle" : "");
	}

	private static boolean from(Class<?> c) {
		return new java.io.File(c.getProtectionDomain().getCodeSource().getLocation().getPath())
				.equals(new java.io.File(System.getProperty("bundle.jar")));
	}
}

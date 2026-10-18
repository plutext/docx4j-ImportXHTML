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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Assert;
import org.junit.Test;

/**
 * convertMHT: the text/html part is read while mime4j's parser is on it (before, the
 * stream was kept and read after parse() returned, by which time mime4j had closed it,
 * so the importer saw an empty document), and is transfer-decoded by mime4j rather than
 * patched up by MHTFilterInputStream.
 */
public class MHTImportTest {

	/**
	 * The BIRT output in MHTContentHandler's comment: one quoted-printable text/html part
	 * on a single line, UTF-8 with a byte order mark although it declares gb2312.
	 */
	private static final String BIRT = "From:\r\n"
			+ "Subject:\r\n"
			+ "Date:\r\n"
			+ "MIME-Version: 1.0\r\n"
			+ "Content-Type: multipart/related; type=\"text/html\"; boundary=\"___Actuate_Content_Boundary___\"\r\n"
			+ "\r\n"
			+ "--___Actuate_Content_Boundary___\r\n"
			+ "Content-Type: text/html; charset=\"gb2312\"\r\n"
			+ "Content-Transfer-Encoding: quoted-printable\r\n"
			+ "\r\n"
			+ "=EF=BB=BF<html><head><style type=3D\"text/css\">.styleForeign{ font-family: serif; font-style: normal; font-variant: normal; font-weight: normal; font-size: 10pt; color: rgb(0, 0, 0); margin: 0; padding: 0; text-indent: 0pt; letter-spacing: 0; word-spacing: 0; text-transform: none; white-space: normal; line-height: normal;}</style></head><body><div class=3D\"styleForeign\">Dec 2, 2021 3:02 PM</div></body>\r\n"
			+ "</html>\r\n"
			+ "--___Actuate_Content_Boundary___--\r\n"
			+ "\r\n";

	private static final String MULTIPART_HEADER = "MIME-Version: 1.0\r\n"
			+ "Content-Type: multipart/related; type=\"text/html\"; boundary=\"BOUNDARY\"\r\n"
			+ "\r\n";

	@Test
	public void birt() throws Exception {
		String xml = convertMHT(BIRT);
		Assert.assertTrue(xml, xml.contains("Dec 2, 2021 3:02 PM"));
		Assert.assertTrue(xml, xml.contains("<w:sz w:val=\"20\"/>")); // the stylesheet's 10pt
	}

	/**
	 * Quoted-printable as RFC 2045 writes it, which MHTFilterInputStream mangled,
	 * and the text/html part followed by a resource part.
	 */
	@Test
	public void quotedPrintableThenImagePart() throws Exception {
		String xml = convertMHT(MULTIPART_HEADER
				+ "--BOUNDARY\r\n"
				+ "Content-Type: text/html; charset=\"utf-8\"\r\n"
				+ "Content-Transfer-Encoding: quoted-printable\r\n"
				+ "\r\n"
				+ "<html><body><p>caf=C3=A9 au lait</p><p>a line wrapped with a soft br=\r\n"
				+ "eak</p><p>1 + 1 =3D 2</p><p class=3D\"k\">QP_LAST</p></body></html>\r\n"
				+ "--BOUNDARY\r\n"
				+ "Content-Type: image/png\r\n"
				+ "Content-Transfer-Encoding: base64\r\n"
				+ "Content-Location: file:///image001.png\r\n"
				+ "\r\n"
				+ "iVBORw0KGgo=\r\n"
				+ "--BOUNDARY--\r\n");
		Assert.assertTrue(xml, xml.contains("café au lait"));
		Assert.assertTrue(xml, xml.contains("a line wrapped with a soft break"));
		Assert.assertTrue(xml, xml.contains("1 + 1 = 2"));
		Assert.assertTrue(xml, xml.contains("QP_LAST"));
	}

	/**
	 * BIRT writes its HTML on one line: longer than mime4j's read buffer (which, with
	 * mime4j 0.8.7, was as much of a kept stream as could still be read) and its
	 * 1000 character header line limit.
	 */
	@Test
	public void longSingleLine() throws Exception {
		StringBuilder html = new StringBuilder("<html><body>");
		for (int i = 0; i < 500; i++) {
			html.append("<p>Line ").append(i).append("</p>");
		}
		html.append("</body></html>");

		List<Object> content = importer().convertMHT(stream(MULTIPART_HEADER
				+ "--BOUNDARY\r\n"
				+ "Content-Type: text/html; charset=\"utf-8\"\r\n"
				+ "Content-Transfer-Encoding: quoted-printable\r\n"
				+ "\r\n"
				+ html + "\r\n"
				+ "--BOUNDARY--\r\n"), null);
		Assert.assertEquals(500, content.size());
		String xml = marshal(content);
		Assert.assertTrue(xml, xml.contains("Line 0"));
		Assert.assertTrue(xml, xml.contains("Line 499"));
	}

	/** With no byte order mark, the part's declared charset is used. */
	@Test
	public void declaredCharset() throws Exception {
		String chinese = "中文测试"; // 中文测试
		byte[] html = ("<html><body><p>" + chinese + "</p></body></html>").getBytes(Charset.forName("GB2312"));

		String xml = convertMHT(MULTIPART_HEADER
				+ "--BOUNDARY\r\n"
				+ "Content-Type: text/html; charset=\"gb2312\"\r\n"
				+ "Content-Transfer-Encoding: quoted-printable\r\n"
				+ "\r\n"
				+ quotedPrintable(html) + "\r\n"
				+ "--BOUNDARY--\r\n");
		Assert.assertTrue(xml, xml.contains(chinese));
	}

	@Test
	public void singlePartBase64() throws Exception {
		String html = "<html><body><p>BASE64_TEXT</p></body></html>";

		String xml = convertMHT("MIME-Version: 1.0\r\n"
				+ "Content-Type: text/html; charset=\"utf-8\"\r\n"
				+ "Content-Transfer-Encoding: base64\r\n"
				+ "\r\n"
				+ Base64.getMimeEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8)) + "\r\n");
		Assert.assertTrue(xml, xml.contains("BASE64_TEXT"));
	}

	@Test(expected = Docx4JException.class)
	public void noHtmlPart() throws Exception {
		convertMHT(MULTIPART_HEADER
				+ "--BOUNDARY\r\n"
				+ "Content-Type: text/plain\r\n"
				+ "\r\n"
				+ "not html\r\n"
				+ "--BOUNDARY--\r\n");
	}

	private static XHTMLImporterImpl importer() throws Exception {
		return new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
	}

	private static ByteArrayInputStream stream(String mht) {
		return new ByteArrayInputStream(mht.getBytes(StandardCharsets.US_ASCII));
	}

	private static String convertMHT(String mht) throws Exception {
		return marshal(importer().convertMHT(stream(mht), null));
	}

	/** Every byte outside printable ASCII, and '=', as =XX; no soft line breaks. */
	private static String quotedPrintable(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			int c = b & 0xFF;
			if (c >= 32 && c <= 126 && c != '=') {
				sb.append((char) c);
			} else {
				sb.append(String.format("=%02X", c));
			}
		}
		return sb.toString();
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

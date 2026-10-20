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

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * setExternalResourcesAllowed(false): nothing the document refers to is fetched, by http
 * or from a file: URL (linked and @import-ed stylesheets, images), and a refused image
 * leaves no "[MISSING IMAGE ...]" text; data: images, style elements and the default
 * stylesheet still apply.  By default, all of it is fetched.
 */
public class ExternalResourcesTest {

	private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";

	private static final String LINKED = "<w:color w:val=\"112233\"/>";
	private static final String IMPORTED = "<w:color w:val=\"445566\"/>";
	private static final String FILE = "<w:color w:val=\"778899\"/>";
	private static final String INLINE = "<w:color w:val=\"123456\"/>";

	private HttpServer server;
	private final List<String> requested = Collections.synchronizedList(new ArrayList<>());
	private Path dir;

	@Before
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
		serve("/linked.css", "text/css", "p.linked { color: #112233; }".getBytes(StandardCharsets.UTF_8));
		serve("/imported.css", "text/css", "p.imported { color: #445566; }".getBytes(StandardCharsets.UTF_8));
		serve("/image.png", "image/png", Base64.getDecoder().decode(PNG_BASE64));
		server.start();

		dir = Files.createTempDirectory("ExternalResourcesTest");
		Files.write(dir.resolve("file.css"), "p.file { color: #778899; }".getBytes(StandardCharsets.UTF_8));
		Files.write(dir.resolve("image.png"), Base64.getDecoder().decode(PNG_BASE64));
	}

	@After
	public void tearDown() throws Exception {
		server.stop(0);
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		}
	}

	@Test
	public void fetchedByDefault() throws Exception {
		String xml = convert(true);

		Assert.assertTrue(requested.toString(),
				requested.containsAll(Arrays.asList("/linked.css", "/imported.css", "/image.png")));
		Assert.assertTrue(xml, xml.contains(LINKED));
		Assert.assertTrue(xml, xml.contains(IMPORTED));
		Assert.assertTrue(xml, xml.contains(FILE));
		Assert.assertTrue(xml, xml.contains(INLINE));
		Assert.assertEquals(xml, 3, count(xml, "<w:drawing>")); // http, file: and data:
	}

	@Test
	public void nothingFetchedWhenNotAllowed() throws Exception {
		String xml = convert(false);

		Assert.assertEquals(Collections.emptyList(), requested);
		Assert.assertFalse(xml, xml.contains(LINKED));
		Assert.assertFalse(xml, xml.contains(IMPORTED));
		Assert.assertFalse(xml, xml.contains(FILE));
		Assert.assertEquals(xml, 1, count(xml, "<w:drawing>")); // data: only
		Assert.assertFalse(xml, xml.contains("MISSING IMAGE"));

		Assert.assertTrue(xml, xml.contains(INLINE));
		Assert.assertTrue(xml, xml.contains("LINKED"));
		Assert.assertTrue(xml, xml.contains("<w:jc w:val=\"center\"/>")); // the default stylesheet's center
	}

	@Test
	public void fileAndURLReadWhenNotAllowed() throws Exception {
		Path doc = dir.resolve("doc.xhtml");
		Files.write(doc, xhtml().getBytes(StandardCharsets.UTF_8));

		XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		importer.setExternalResourcesAllowed(false);
		String fromFile = marshal(importer.convert(doc.toFile(), null));

		importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		importer.setExternalResourcesAllowed(false);
		String fromURL = marshal(importer.convert(doc.toUri().toURL()));

		Assert.assertEquals(Collections.emptyList(), requested);
		for (String xml : Arrays.asList(fromFile, fromURL)) {
			Assert.assertTrue(xml, xml.contains(INLINE));
			Assert.assertFalse(xml, xml.contains(LINKED));
			Assert.assertFalse(xml, xml.contains(FILE));
			Assert.assertEquals(xml, 1, count(xml, "<w:drawing>")); // data: only
		}
	}

	private String convert(boolean allowed) throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		importer.setExternalResourcesAllowed(allowed);
		return marshal(importer.convert(xhtml(), null));
	}

	private String xhtml() {
		String http = "http://127.0.0.1:" + server.getAddress().getPort();
		String file = dir.toUri().toString(); // ends with /
		return "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
				+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + http + "/linked.css\"/>"
				+ "<style type=\"text/css\">@import url(\"" + http + "/imported.css\");</style>"
				+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + file + "file.css\"/>"
				+ "<style type=\"text/css\">p.inline { color: #123456; }</style>"
				+ "</head><body>"
				+ "<p class=\"linked\">LINKED</p>"
				+ "<p class=\"imported\">IMPORTED</p>"
				+ "<p class=\"file\">FILE</p>"
				+ "<p class=\"inline\">INLINE</p>"
				+ "<center>CENTERED</center>"
				+ "<p><img src=\"" + http + "/image.png\" alt=\"http\"/></p>"
				+ "<p><img src=\"" + file + "image.png\" alt=\"file\"/></p>"
				+ "<p><img src=\"data:image/png;base64," + PNG_BASE64 + "\" alt=\"data\"/></p>"
				+ "</body></html>";
	}

	private void serve(String path, String contentType, byte[] body) {
		server.createContext(path, exchange -> {
			requested.add(exchange.getRequestURI().getPath());
			exchange.getResponseHeaders().set("Content-Type", contentType);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
		});
	}

	private static int count(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) n++;
		return n;
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

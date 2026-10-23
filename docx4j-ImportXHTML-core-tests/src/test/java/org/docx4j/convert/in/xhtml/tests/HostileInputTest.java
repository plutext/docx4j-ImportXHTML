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
import java.io.File;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.ImportXHTMLProperties;
import org.docx4j.convert.in.xhtml.LinkSchemes;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.PresentationMLPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.PresentationML.MainPresentationPart;
import org.docx4j.openpackaging.parts.PresentationML.SlideLayoutPart;
import org.docx4j.openpackaging.parts.PresentationML.SlidePart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pptx4j.convert.in.xhtml.XHTMLtoPPTX;
import org.xml.sax.InputSource;

import com.sun.net.httpserver.HttpServer;

/**
 * Input written to attack the conversion, one test per row of CR-005's table (section 1), so that
 * what holds today keeps holding: a link whose scheme LinkSchemes doesn't allow gives its
 * text without the link, in docx and pptx; a quote, &amp; or &lt; in an internal link's
 * fragment, its text or an image's alt text stays text (it neither adds an attribute nor
 * loses the link or image); external entities, external DTDs and parameter entities are
 * neither expanded nor fetched, through every entry point that parses; entity expansion is
 * bounded; input that isn't well-formed gives a Docx4JException from every entry point; and an
 * image that fails is fetched once.  Network: a listener on 127.0.0.1 only.
 */
public class HostileInputTest {

	private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";
	private static final String XHTML_NS = "xmlns=\"http://www.w3.org/1999/xhtml\"";

	private HttpServer server;
	private final List<String> requested = Collections.synchronizedList(new ArrayList<>());
	private Path dir;

	@Before
	public void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
		server.createContext("/", exchange -> {
			requested.add(exchange.getRequestURI().getPath());
			byte[] body = exchange.getRequestURI().getPath().endsWith(".dtd")
					? "<!ENTITY ext 'FROM_REMOTE_DTD'>".getBytes(StandardCharsets.UTF_8) : null;
			exchange.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : body.length);
			if (body != null) exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();

		dir = Files.createTempDirectory("HostileInputTest");
		Files.write(dir.resolve("secret.txt"), "SECRET_FROM_FILE".getBytes(StandardCharsets.UTF_8));
	}

	@After
	public void tearDown() throws Exception {
		server.stop(0);
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		}
	}

	// Row 1: link schemes

	@Test
	public void refusedSchemesGiveTheTextWithoutALink() throws Exception {
		for (String href : new String[] {"javascript:alert(1)", " JaVaScRiPt:alert(2)", "java\tscript:alert(3)",
				"vbscript:msgbox(1)", "data:text/html,<script>alert(4)</script>", "ms-msdt:/id PCWDiagnostic"}) {
			WordprocessingMLPackage pkg = linked("<a href=\"" + attribute(href) + "\">LINKTEXT</a>", true);
			String xml = mainXml(pkg);
			Assert.assertFalse(href + ": " + xml, xml.contains("<w:hyperlink"));
			Assert.assertTrue(href + ": " + xml, xml.contains("LINKTEXT"));
			Assert.assertEquals(href, Collections.emptyList(), externalTargets(pkg.getMainDocumentPart()));
		}
	}

	@Test
	public void allowedTargetsKeepTheirLinks() throws Exception {
		for (String href : new String[] {"http://example.org/", "https://example.org/a?b=1&c=2",
				"mailto:someone@example.org", "ftp://example.org/f", "tel:+61200000000", "news:comp.text.xml",
				"relative/page.html", "C:\\docs\\doc.docx", "\\\\host\\share\\doc.docx",
				"file://attacker.invalid/share/x"}) { // file: and UNC paths stay links (CR-005 D3)
			WordprocessingMLPackage pkg = linked("<a href=\"" + attribute(href) + "\">LINKTEXT</a>", true);
			Assert.assertEquals(href, Collections.singletonList(href), externalTargets(pkg.getMainDocumentPart()));
		}
		String xml = mainXml(linked("<a href=\"#frag\">LINKTEXT</a>", true));
		Assert.assertTrue(xml, xml.contains("<w:hyperlink w:anchor=\"frag\">"));
	}

	@Test
	public void refusedLinkKeepsItsContent() throws Exception {
		String xml = mainXml(linked("<a href=\"javascript:alert(1)\">click <b>HERE</b> now</a>", true));
		Assert.assertFalse(xml, xml.contains("<w:hyperlink"));
		Assert.assertTrue(xml, xml.contains("click"));
		Assert.assertTrue(xml, xml.contains("HERE"));
		Assert.assertTrue(xml, xml.contains("<w:b/>"));
		Assert.assertTrue(xml, xml.contains("now"));
	}

	@Test
	public void propertyAddsAScheme() throws Exception {
		String before = ImportXHTMLProperties.getProperty(LinkSchemes.PROPERTY);
		String notes = "<a href=\"notes://server/db\">LINKTEXT</a>";
		try {
			Assert.assertEquals(Collections.emptyList(), externalTargets(linked(notes, true).getMainDocumentPart()));

			ImportXHTMLProperties.setProperty(LinkSchemes.PROPERTY, "onenote, Notes");
			Assert.assertEquals(Collections.singletonList("notes://server/db"),
					externalTargets(linked(notes, true).getMainDocumentPart()));
			Assert.assertEquals(Collections.emptyList(),
					externalTargets(linked("<a href=\"javascript:alert(1)\">LINKTEXT</a>", true).getMainDocumentPart()));
		} finally {
			// the property is global, so don't leak it into other tests
			if (before == null) {
				ImportXHTMLProperties.getProperties().remove(LinkSchemes.PROPERTY);
			} else {
				ImportXHTMLProperties.setProperty(LinkSchemes.PROPERTY, before);
			}
		}
	}

	@Test
	public void uncheckedKeepsEveryTarget() throws Exception {
		WordprocessingMLPackage pkg = linked("<a href=\"javascript:alert(1)\">LINKTEXT</a>", false);
		Assert.assertEquals(Collections.singletonList("javascript:alert(1)"), externalTargets(pkg.getMainDocumentPart()));
	}

	@Test
	public void pptxRefusesTheSameSchemes() throws Exception {
		String xhtml = p("<a href=\"javascript:alert(1)\">JS</a> <a href=\"https://example.org/\">OK</a>");

		SlidePart slide = slide();
		new XHTMLtoPPTX((PresentationMLPackage) slide.getPackage(), slide, xhtml, null).convertSingleSlide();
		Assert.assertEquals(Collections.singletonList("https://example.org/"), externalTargets(slide));

		slide = slide();
		XHTMLtoPPTX unchecked = new XHTMLtoPPTX((PresentationMLPackage) slide.getPackage(), slide, xhtml, null);
		unchecked.setLinkSchemesChecked(false);
		unchecked.convertSingleSlide();
		Assert.assertTrue(externalTargets(slide).toString(), externalTargets(slide).contains("javascript:alert(1)"));
	}

	// Rows 2 to 4: an internal link's w:anchor and its text

	@Test
	public void quoteInFragmentAddsNoAttribute() throws Exception {
		String xml = convert(p("<a href='#x\" w:tooltip=\"INJECTED'>LINKTEXT</a>"));
		Assert.assertTrue(xml, xml.contains("w:anchor=\"x&quot; w:tooltip=&quot;INJECTED\""));
		Assert.assertFalse(xml, xml.contains("w:tooltip=\"INJECTED\""));
		Assert.assertTrue(xml, xml.contains("<w:t>LINKTEXT</w:t>"));

		xml = convert(p("<a href='#x\" r:id=\"rId1'>LINKTEXT</a>"));
		Assert.assertFalse(xml, xml.contains("r:id=\"rId1\""));
		Assert.assertTrue(xml, xml.contains("<w:t>LINKTEXT</w:t>"));
	}

	@Test
	public void ampersandOrLessThanInFragmentKeepsTheLink() throws Exception {
		String xml = convert(p("<a href=\"#a&amp;b\">LINKTEXT</a>"));
		Assert.assertTrue(xml, xml.contains("w:anchor=\"a&amp;b\""));
		Assert.assertTrue(xml, xml.contains("<w:t>LINKTEXT</w:t>"));

		xml = convert(p("<a href=\"#a&lt;b\">LINKTEXT</a>"));
		Assert.assertTrue(xml, xml.contains("w:anchor=\"a&lt;b\""));
		Assert.assertTrue(xml, xml.contains("<w:t>LINKTEXT</w:t>"));
	}

	@Test
	public void linkTextKeepsItsAmpersands() throws Exception {
		// the text is "&amp; then &": it has "&amp;" in it, so the old escaping left the "&" bare
		String xml = convert(p("<a href=\"https://example.org/\">&amp;amp; then &amp;</a>"));
		Assert.assertTrue(xml, xml.contains("<w:hyperlink"));
		Assert.assertTrue(xml, xml.contains("<w:t>&amp;amp; then &amp;</w:t>"));
	}

	// Rows 5 and 6: an image's alt text

	@Test
	public void ampersandOrLessThanInAltKeepsTheImage() throws Exception {
		String xml = convert(p("<img alt=\"Profit &amp; Loss\" src=\"data:image/png;base64," + PNG_BASE64 + "\"/>"));
		Assert.assertTrue(xml, xml.contains("descr=\"Profit &amp; Loss\""));
		Assert.assertFalse(xml, xml.contains("MISSING IMAGE"));

		xml = convert(p("<img alt=\"a &lt; b\" src=\"data:image/png;base64," + PNG_BASE64 + "\"/>"));
		Assert.assertTrue(xml, xml.contains("descr=\"a &lt; b\""));
		Assert.assertFalse(xml, xml.contains("MISSING IMAGE"));
	}

	@Test
	public void quoteInAltAddsNoAttribute() throws Exception {
		String xml = convert(p("<img alt='x\" title=\"INJECTED' src=\"data:image/png;base64," + PNG_BASE64 + "\"/>"));
		Assert.assertTrue(xml, xml.contains("descr=\"x&quot; title=&quot;INJECTED\""));
		Assert.assertFalse(xml, xml.contains("title=\"INJECTED\""));
	}

	// Rows 7 to 9: the parser, through each entry point that parses, fetching allowed or not

	@Test
	public void externalEntityNotExpanded() throws Exception {
		String xhtml = "<!DOCTYPE html [<!ENTITY x SYSTEM '" + dir.resolve("secret.txt").toUri() + "'>]>"
				+ "<html " + XHTML_NS + "><body><p>[&x;]</p></body></html>";
		for (boolean allowed : new boolean[] {true, false}) {
			for (Map.Entry<String, EntryPoint> e : entryPoints().entrySet()) {
				String xml = marshal(e.getValue().convert(importer(allowed), xhtml));
				String where = e.getKey() + ", allowed " + allowed + ": " + xml;
				Assert.assertFalse(where, xml.contains("SECRET_FROM_FILE"));
				Assert.assertTrue(where, xml.contains("[]"));
			}
		}
	}

	@Test
	public void externalDtdAndParameterEntityNotFetched() throws Exception {
		String http = "http://127.0.0.1:" + server.getAddress().getPort();
		String[] doctypes = {
				"<!DOCTYPE html SYSTEM '" + http + "/ext.dtd'>",
				"<!DOCTYPE html [<!ENTITY % p SYSTEM '" + http + "/param.dtd'> %p;]>" };
		for (String doctype : doctypes) {
			String xhtml = doctype + "<html " + XHTML_NS + "><body><p>[&ext;]</p></body></html>";
			for (boolean allowed : new boolean[] {true, false}) {
				for (Map.Entry<String, EntryPoint> e : entryPoints().entrySet()) {
					String xml = "";
					try {
						xml = marshal(e.getValue().convert(importer(allowed), xhtml));
					} catch (Docx4JException ex) {
						// the parameter entity is a parse error; what matters is that nothing was fetched
					}
					String where = e.getKey() + ", allowed " + allowed + ", " + doctype + ": " + xml;
					Assert.assertEquals(where, Collections.emptyList(), requested);
					Assert.assertFalse(where, xml.contains("FROM_REMOTE_DTD"));
				}
			}
		}
	}

	@Test(timeout = 60000)
	public void entityExpansionIsBounded() throws Exception {
		StringBuilder xhtml = new StringBuilder("<!DOCTYPE html [<!ENTITY l0 'lol'>");
		for (int i = 1; i <= 9; i++) {
			xhtml.append("<!ENTITY l" + i + " '");
			for (int j = 0; j < 10; j++) xhtml.append("&l" + (i - 1) + ";");
			xhtml.append("'>");
		}
		xhtml.append("]><html " + XHTML_NS + "><body><p>&l9;</p></body></html>");
		try {
			importer(true).convert(xhtml.toString(), null);
			Assert.fail("10^9 entity expansions were accepted");
		} catch (Docx4JException expected) {
			// the parser's limit
		}
	}

	@Test
	public void notWellFormedGivesDocx4JException() throws Exception {
		String xhtml = "<html " + XHTML_NS + "><body><p>unclosed</body></html>";
		for (boolean allowed : new boolean[] {true, false}) {
			for (Map.Entry<String, EntryPoint> e : entryPoints().entrySet()) {
				if (e.getKey().equals("MHT")) continue; // convertMHT reads such HTML with jsoup instead
				String where = e.getKey() + ", allowed " + allowed;
				try {
					e.getValue().convert(importer(allowed), xhtml);
					Assert.fail(where + ": converted");
				} catch (Docx4JException expected) {
					// not openhtmltopdf's unchecked XRRuntimeException, which all but convert(String) let out
					Assert.assertTrue(where + ": " + expected.getMessage(), expected.getMessage().startsWith("issues at Line 1"));
				}
			}
		}
	}

		@Test
	public void importerUsableAfterNotWellFormed() throws Exception {
		// A parse which fails leaves no renderer behind, so the next convert on the same importer
		// gets a fresh one, and a setExternalResourcesAllowed(true) in between takes effect
		Path png = dir.resolve("image.png");
		Files.write(png, Base64.getDecoder().decode(PNG_BASE64));
		String bad = "<html " + XHTML_NS + "><body><p>unclosed</body></html>";
		String good = p("<img src=\"" + png.toUri() + "\"/>");
		for (Map.Entry<String, EntryPoint> e : entryPoints().entrySet()) {
			if (e.getKey().equals("MHT")) continue;
			XHTMLImporterImpl importer = importer(false);
			try {
				e.getValue().convert(importer, bad);
				Assert.fail(e.getKey() + ": converted");
			} catch (Docx4JException expected) {
			}
			importer.setExternalResourcesAllowed(true);
			String xml = marshal(importer.convert(good, null));
			Assert.assertTrue(e.getKey() + ": " + xml, xml.contains("<w:drawing>"));
		}
	}

	@Test
	public void missingFileGivesDocx4JException() throws Exception {
		// whether or not the renderer's user agent reads it (it returns null, not an exception)
		File missing = dir.resolve("missing.xhtml").toFile();
		for (boolean allowed : new boolean[] {true, false}) {
			try {
				importer(allowed).convert(missing, null);
				Assert.fail("File, allowed " + allowed + ": converted");
			} catch (Docx4JException expected) {
				Assert.assertTrue(expected.getMessage(), expected.getMessage().startsWith("Couldn't read "));
			}
			try {
				importer(allowed).convert(missing.toURI().toURL());
				Assert.fail("URL, allowed " + allowed + ": converted");
			} catch (Docx4JException expected) {
				Assert.assertTrue(expected.getMessage(), expected.getMessage().startsWith("Couldn't read "));
			}
		}
	}

	// Row 11: an image that fails is asked for once

	@Test
	public void failedImageFetchedOnce() throws Exception {
		String http = "http://127.0.0.1:" + server.getAddress().getPort();
		String xml = convert(p("<img src=\"" + http + "/missing.png\"/>"));
		Assert.assertEquals(Collections.singletonList("/missing.png"), requested);
		Assert.assertTrue(xml, xml.contains("MISSING IMAGE"));
	}

	private static WordprocessingMLPackage linked(String inline, boolean checked) throws Exception {
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		XHTMLImporterImpl importer = new XHTMLImporterImpl(pkg);
		importer.setLinkSchemesChecked(checked);
		pkg.getMainDocumentPart().getContent().addAll(importer.convert(p(inline), null));
		return pkg;
	}

	private static String mainXml(WordprocessingMLPackage pkg) {
		return XmlUtils.marshaltoString(pkg.getMainDocumentPart().getJaxbElement(), true, false);
	}

	/** The targets of the part's External hyperlink relationships */
	private static List<String> externalTargets(Part part) {
		List<String> targets = new ArrayList<>();
		for (Relationship r : part.getRelationshipsPart().getRelationships().getRelationship()) {
			if (Namespaces.HYPERLINK.equals(r.getType()) && "External".equals(r.getTargetMode())) {
				targets.add(r.getTarget());
			}
		}
		return targets;
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

	/** href as an XML attribute value, a tab included */
	private static String attribute(String href) {
		return href.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;").replace("\t", "&#9;");
	}

	private interface EntryPoint {
		List<Object> convert(XHTMLImporterImpl importer, String xhtml) throws Exception;
	}

	/** Every convert that parses: the same parser (openhtmltopdf's XMLResource) behind each. */
	private Map<String, EntryPoint> entryPoints() {
		Map<String, EntryPoint> m = new LinkedHashMap<>();
		m.put("String", (i, x) -> i.convert(x, null));
		m.put("InputStream", (i, x) -> i.convert(new ByteArrayInputStream(x.getBytes(StandardCharsets.UTF_8)), null));
		m.put("Reader", (i, x) -> i.convert(new StringReader(x), null));
		m.put("InputSource", (i, x) -> i.convert(new InputSource(new StringReader(x)), null));
		m.put("File", (i, x) -> i.convert(file(x).toFile(), null));
		m.put("URL", (i, x) -> i.convert(file(x).toUri().toURL()));
		m.put("MHT", (i, x) -> i.convertMHT(new ByteArrayInputStream(("MIME-Version: 1.0\r\n"
				+ "Content-Type: text/html; charset=\"utf-8\"\r\n\r\n" + x).getBytes(StandardCharsets.UTF_8)), null));
		return m;
	}

	private Path file(String xhtml) throws Exception {
		Path f = Files.createTempFile(dir, "doc", ".xhtml");
		Files.write(f, xhtml.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private static XHTMLImporterImpl importer(boolean externalResourcesAllowed) throws Exception {
		XHTMLImporterImpl importer = new XHTMLImporterImpl(WordprocessingMLPackage.createPackage());
		importer.setExternalResourcesAllowed(externalResourcesAllowed);
		return importer;
	}

	private static String p(String inline) {
		return "<html " + XHTML_NS + "><body><p>before " + inline + " after</p></body></html>";
	}

	private static String convert(String xhtml) throws Exception {
		return marshal(importer(true).convert(xhtml, null));
	}

	private static String marshal(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object o : content) sb.append(XmlUtils.marshaltoString(o, true, true));
		return sb.toString();
	}
}

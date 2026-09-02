package org.docx4j.convert.in.xhtml.tests;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Before;
import org.junit.Test;

/**
 * XHTML import turns embedded MathML into native OMML via MathMLToOmml — no
 * XSLT, no Microsoft MML2OMML.XSL required. See CR-math-omml-mathml.
 */
public class MathMLImportTest {

	private static final String MML = "http://www.w3.org/1998/Math/MathML";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	private String convertToXml(String mathml) throws Docx4JException {
		String xhtml = "<html><body><p><math xmlns=\"" + MML + "\">"
				+ mathml + "</math></p></body></html>";
		List<Object> content = new XHTMLImporterImpl(wordMLPackage).convert(xhtml, "");
		StringBuilder sb = new StringBuilder();
		for (Object o : content) {
			sb.append(XmlUtils.marshaltoString(o, true, false));
		}
		return sb.toString();
	}

	@Test
	public void fractionBecomesOmmlFraction() throws Exception {
		String xml = convertToXml("<mfrac><mn>1</mn><mn>2</mn></mfrac>");
		assertTrue("should produce an OMath: " + xml, xml.contains("oMath"));
		assertTrue("mfrac -> m:f: " + xml, xml.contains("<m:f>"));
		assertTrue("numerator kept: " + xml, xml.contains("<m:num>"));
	}

	@Test
	public void squareRootBecomesRadical() throws Exception {
		String xml = convertToXml("<msqrt><mo>-</mo><mn>1</mn></msqrt>");
		assertTrue("msqrt -> m:rad: " + xml, xml.contains("<m:rad>"));
		assertTrue("degree hidden for a square root: " + xml, xml.contains("<m:degHide"));
	}

	@Test
	public void subscriptBecomesSSub() throws Exception {
		String xml = convertToXml("<msub><mi>m</mi><mn>1</mn></msub>");
		assertTrue("msub -> m:sSub: " + xml, xml.contains("<m:sSub>"));
	}

	@Test
	public void fencedBecomesDelimiter() throws Exception {
		String xml = convertToXml("<mfenced open='[' close=']'><mi>a</mi></mfenced>");
		assertTrue("mfenced -> m:d: " + xml, xml.contains("<m:d>"));
		assertTrue("open delimiter kept: " + xml, xml.contains("m:val=\"[\""));
	}
}

package org.docx4j.convert.in.xhtml.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.Bidi;

import org.docx4j.convert.in.xhtml.ImportXHTMLProperties;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.Before;
import org.junit.Test;

public class BidiTest {
	
	private WordprocessingMLPackage wordMLPackage;
    private XHTMLImporterImpl XHTMLImporter;
    
    private static final String MIXED_HEBREW="ליצור מהרשת רשתthe catכלל 123עולמית באמת!";

	
	@Before
	public void setup() throws Docx4JException {
		
		ImportXHTMLProperties.setProperty("docx4j-ImportXHTML.Bidi.Heuristic", true);
		wordMLPackage = WordprocessingMLPackage.createPackage();
		XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);
	}


	@Test
	public void testMixed() {
		
		assertTrue(XHTMLImporter.isBidi(MIXED_HEBREW));
	}

	@Test
	public void testLTR() {
		
		assertFalse(XHTMLImporter.isBidi("this is just left to right"));
	}

	@Test
	public void testRun0_LTR() {

    	Bidi bidi = new Bidi("this is just left to right", Bidi.DIRECTION_LEFT_TO_RIGHT);
		
		assertTrue(nthRunIsLeftToRight(bidi,0));
	}

	@Test
	public void testMixedHebrewRun0_RTL() {

    	Bidi bidi = new Bidi(MIXED_HEBREW, Bidi.DIRECTION_RIGHT_TO_LEFT);
		
		assertFalse(nthRunIsLeftToRight(bidi,0));
	}
	@Test
	public void testMixedHebrewRun1_RTL() {

    	Bidi bidi = new Bidi(MIXED_HEBREW, Bidi.DIRECTION_RIGHT_TO_LEFT);
		
		assertTrue(nthRunIsLeftToRight(bidi,1));
	}
	
	private boolean nthRunIsLeftToRight(Bidi bidi, int n) {
		// even means its left to right
		return isEven(bidi.getRunLevel(n) );
	}
	
	private boolean isEven(int x) {
		return ((x & 1) == 0 ) ;
	}
	
}

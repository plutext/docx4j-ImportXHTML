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

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTHeight;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tr;
import org.docx4j.wml.TrPr;

/**
 * The second Word check for CR-007 (D6): how Word lays out a table row whose atLeast height is
 * more than a page. In the first, the row clamped to 32767 showed every line, but started on the
 * page after its heading and ran on, empty, to the bottom margin of its last page.
 *
 * <p>Each case is a bordered cell of numbered lines, converted by {@link XHTMLImporterImpl}, with
 * its {@code w:trHeight} then set as the case says:</p>
 * <ul>
 * <li>A: 85 lines at 32767, CR-007's clamp (round 1's 1b again);</li>
 * <li>B: 85 lines with no {@code w:trHeight};</li>
 * <li>C: 85 lines at 10000, less than a page;</li>
 * <li>D: 40 lines as ImportXHTML writes them today, in range but more than a page.</li>
 * </ul>
 *
 * <p>The predictions are in CR-007 §4, and the results go in
 * {@code src/test/resources/word-check/evidence/word-open-results.md}.</p>
 *
 * <p>Usage: {@code TallRowProbeBuilder <outDir>} writes {@code cr007-tall-row-r2.docx} and
 * {@code README.txt}. Not a test: run it from the test classpath.</p>
 */
public class TallRowProbeBuilder {

	static final String DOCX = "cr007-tall-row-r2.docx";

	/** A4's text height with createPackage's 1-inch margins, in twips. */
	static final int PAGE_TEXT_HEIGHT = 16838 - 2 * 1440;

	public static void main(String[] args) throws Exception {

		File outDir = new File(args[0]);
		outDir.mkdirs();

		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
		MainDocumentPart mdp = pkg.getMainDocumentPart();
		mdp.getContent().add(mdp.createStyledParagraphOfText("Title",
				"ImportXHTML CR-007 D6: Word check, round 2"));
		mdp.addParagraphOfText("Four cases, one per page. README.txt says what to look at.");

		// A: written over 32767 today (45600 here), so set to the clamp
		Tbl a = addCase(pkg, "A", "85 lines, w:trHeight 32767 atLeast (CR-007's clamp)", 85).tbl;
		int written = getHeight(a);
		if (written <= Short.MAX_VALUE) {
			throw new IllegalStateException("A: expected a written height over 32767, got " + written);
		}
		setHeight(a, 32767);

		// B: no height at all
		Tbl b = addCase(pkg, "B", "85 lines, no w:trHeight", 85).tbl;
		setHeight(b, -1);

		// C: a minimum less than a page
		Tbl c = addCase(pkg, "C", "85 lines, w:trHeight 10000 atLeast (6.9 in)", 85).tbl;
		setHeight(c, 10000);

		// D: as written today, which must be in range and more than a page for the case to mean anything
		Case d = addCase(pkg, "D", "40 lines, w:trHeight as ImportXHTML writes it today", 40);
		int dHeight = getHeight(d.tbl);
		if (dHeight <= PAGE_TEXT_HEIGHT || dHeight > Short.MAX_VALUE) {
			throw new IllegalStateException("D: expected a written height between a page and 32767, got " + dHeight);
		}
		// the heading is written before the height is known
		d.heading.getContent().clear();
		d.heading.getContent().addAll(mdp.createParagraphOfText(
				"Case D: 40 lines, w:trHeight " + dHeight + " atLeast (as ImportXHTML writes it today)").getContent());

		pkg.save(new File(outDir, DOCX));
		Files.write(new File(outDir, "README.txt").toPath(),
				readme(written, dHeight).getBytes(StandardCharsets.UTF_8));
		System.out.println("Wrote " + DOCX + " and README.txt to " + outDir
				+ "; A was written " + written + ", D " + dHeight);
	}

	static class Case {
		P heading;
		Tbl tbl;
	}

	/**
	 * Adds the case's heading on a new page, then its table, then a paragraph after it.
	 */
	static Case addCase(WordprocessingMLPackage pkg, String id, String what, int lines) throws Exception {

		MainDocumentPart mdp = pkg.getMainDocumentPart();
		P heading = mdp.createStyledParagraphOfText("Heading2", "Case " + id + ": " + what);
		heading.getPPr().setPageBreakBefore(new BooleanDefaultTrue());
		mdp.getContent().add(heading);

		StringBuilder cell = new StringBuilder();
		for (int i = 1; i <= lines; i++) {
			cell.append("<p>Line " + i + " of " + lines + (i == lines ? " R2" + id + "_LAST" : "") + "</p>");
		}
		String xhtml = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>"
				+ "table{border-collapse:collapse}td{border:1px solid black}</style></head><body>"
				+ "<table><tr><td>" + cell + "</td></tr></table>"
				+ "<p>After case " + id + "'s table. R2" + id + "_AFTER</p></body></html>";

		Case c = new Case();
		c.heading = heading;
		for (Object o : new XHTMLImporterImpl(pkg).convert(xhtml, null)) {
			mdp.getContent().add(o);
			if (XmlUtils.unwrap(o) instanceof Tbl) {
				c.tbl = (Tbl) XmlUtils.unwrap(o);
			}
		}
		if (c.tbl == null) {
			throw new IllegalStateException(id + ": no table");
		}
		return c;
	}

	/** The w:trHeight of the table's only row, or -1 if it has none. */
	static int getHeight(Tbl tbl) {
		CTHeight h = height(tbl);
		return h == null ? -1 : h.getVal().intValue();
	}

	/** Sets the w:trHeight of the table's only row, keeping its rule; -1 removes it. */
	static void setHeight(Tbl tbl, int twips) {
		TrPr trPr = row(tbl).getTrPr();
		Iterator<JAXBElement<?>> it = trPr.getCnfStyleOrDivIdOrGridBefore().iterator();
		while (it.hasNext()) {
			JAXBElement<?> el = it.next();
			if (el.getValue() instanceof CTHeight) {
				if (twips < 0) {
					it.remove();
				} else {
					((CTHeight) el.getValue()).setVal(BigInteger.valueOf(twips));
				}
				return;
			}
		}
		throw new IllegalStateException("no w:trHeight to set");
	}

	static CTHeight height(Tbl tbl) {
		TrPr trPr = row(tbl).getTrPr();
		if (trPr == null) {
			return null;
		}
		for (JAXBElement<?> el : trPr.getCnfStyleOrDivIdOrGridBefore()) {
			if (el.getValue() instanceof CTHeight) {
				return (CTHeight) el.getValue();
			}
		}
		return null;
	}

	static Tr row(Tbl tbl) {
		List<Object> rows = tbl.getContent();
		if (rows.size() != 1) {
			throw new IllegalStateException("expected one row, got " + rows.size());
		}
		return (Tr) XmlUtils.unwrap(rows.get(0));
	}

	static String readme(int aWritten, int dHeight) {
		return "docx4j-ImportXHTML CR-007 D6: Word check, round 2\n"
			+ "Built by TallRowProbeBuilder (docx4j-ImportXHTML-core-tests). Case A's row was written\n"
			+ aWritten + " and set to 32767; case D's is as written, " + dHeight + ".\n"
			+ "\n"
			+ "Open " + DOCX + " in Word 365 normally, not with Open and Repair. Each case starts\n"
			+ "a page with the heading \"Case X\"; its last line ends R2X_LAST, and the paragraph after\n"
			+ "its table ends R2X_AFTER.\n"
			+ "\n"
			+ "Look at, per case (expected, from CR-007 section 4):\n"
			+ "A. 85 lines, atLeast 32767: as round 1's 1b. The row starts on the page after its\n"
			+ "   heading, and the cell runs on, empty, from line 85 to the bottom margin.\n"
			+ "B. 85 lines, no height: the row starts under its heading and ends at line 85, with\n"
			+ "   R2B_AFTER straight after it.\n"
			+ "C. 85 lines, atLeast 10000 (6.9 in): the question. Is the cell's last part at least\n"
			+ "   6.9 in, with empty space below line 85, or does the row end at line 85?\n"
			+ "D. 40 lines, atLeast " + dHeight + " (as written today): as A, starting on the page\n"
			+ "   after its heading, with the cell running on, empty, from line 40 to the bottom\n"
			+ "   margin. If not, today's output is unaffected.\n"
			+ "\n"
			+ "Send back:\n"
			+ "- Word's version and build (File > Account > About Word), and any prompt or repair;\n"
			+ "- the document saved from Word as PDF, beside this README: cr007-tall-row-r2-word365.pdf;\n"
			+ "- anything that differs from the expectations above, in a sentence.\n";
	}

}

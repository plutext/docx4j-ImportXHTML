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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.model.structure.MarginsWellKnown;
import org.docx4j.model.structure.PageDimensions;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;

/**
 * The Word check for CR-009 (phase 0): how Word shows what ImportXHTML writes when the XHTML is
 * laid out at openhtmltopdf's 7.1in, as 17.3.5 did before CR-009, and at the docx's text width.
 *
 * <p>Two sections: A4 portrait (a 6.27in line), then Letter landscape (9in). Each case is
 * converted by {@link XHTMLImporterImpl}, with setTextWidth giving the width to lay out at:</p>
 * <ul>
 * <li>1a, 1b: A4, a width: 100% table, at 7.1in and at the line;</li>
 * <li>2a, 2b: A4, a width: 100% image, the same;</li>
 * <li>3a, 3b: A4, a centred table, the same;</li>
 * <li>4a: Letter landscape, a width: 100% table at 7.1in; 4b: 4a's widths a third larger, as
 * 17.3.4 wrote them; 4c: at the line (the last section's, so with no setTextWidth).</li>
 * </ul>
 *
 * <p>The predictions are in CR-009 §4, and the results go in
 * {@code src/test/resources/word-check/evidence/word-open-results.md}.</p>
 *
 * <p>Usage: {@code LayoutWidthProbeBuilder <outDir>} writes {@code cr009-layout-width-r1.docx}
 * and {@code README.txt}. Not a test: run it from the test classpath, on CR-009's phase 1.</p>
 */
public class LayoutWidthProbeBuilder {

	static final String DOCX = "cr009-layout-width-r1.docx";

	/**
	 * The width 17.3.5 laid out at before CR-009, given to setTextWidth: openhtmltopdf's A4 less
	 * 0.5in margins (10469 twips, 7.27in), less the body's 8px margins, which are added back.
	 */
	static final int BEFORE = 10469 - 240;

	/** A4's text width with 1in margins, in twips, from docx4j's A4 (11907 twips wide). */
	static final int A4 = a4TextWidth();

	static int a4TextWidth() {
		PageDimensions page = new PageDimensions();
		page.setPgSize(PageSizePaper.A4, false);
		page.setMargins(MarginsWellKnown.NORMAL);
		return page.getWritableWidthTwips();
	}

	static final String CELL = "border:1px solid black";

	public static void main(String[] args) throws Exception {

		File outDir = new File(args[0]);
		outDir.mkdirs();

		// the last section, Letter landscape, is the body's
		WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage(PageSizePaper.LETTER, true);
		MainDocumentPart mdp = pkg.getMainDocumentPart();
		mdp.getContent().add(mdp.createStyledParagraphOfText("Title",
				"ImportXHTML CR-009: Word check, round 1"));
		mdp.addParagraphOfText("Section 1 is A4 portrait, section 2 Letter landscape. README.txt says what to look at.");
		ruler(pkg, A4);
		List<String> report = new ArrayList<>();

		String table = "<table style=\"width:100%%;border-collapse:collapse\"><tr><td style=\"" + CELL
				+ "\">%s</td><td style=\"" + CELL + "\">right</td></tr></table>";
		String image = "<p><img src=\"" + png() + "\" style=\"width:100%%\"/></p>";
		String centred = "<table style=\"margin:0 auto;border-collapse:collapse\"><tr><td style=\"" + CELL
				+ ";width:2in\">%s</td></tr></table>";

		report.add("1a " + grid(addCase(pkg, "1a", "A4, a width: 100% table laid out at 7.1in", String.format(table, "R1_1A"), BEFORE)));
		report.add("1b " + grid(addCase(pkg, "1b", "A4, a width: 100% table laid out at the line", String.format(table, "R1_1B"), A4)));
		report.add("2a " + extent(addCase(pkg, "2a", "A4, a width: 100% image laid out at 7.1in (R1_2A)", String.format(image), BEFORE)));
		report.add("2b " + extent(addCase(pkg, "2b", "A4, a width: 100% image laid out at the line (R1_2B)", String.format(image), A4)));
		report.add("3a " + indent(addCase(pkg, "3a", "A4, a centred 2in table laid out at 7.1in", String.format(centred, "R1_3A"), BEFORE)));
		report.add("3b " + indent(addCase(pkg, "3b", "A4, a centred 2in table laid out at the line", String.format(centred, "R1_3B"), A4)));
		endSection(pkg, PageSizePaper.A4, false);

		ruler(pkg, null);
		List<Object> a = addCase(pkg, "4a", "Letter landscape, a width: 100% table laid out at 7.1in", String.format(table, "R1_4A"), BEFORE);
		report.add("4a " + grid(a));
		List<Object> b = addCase(pkg, "4b", "Letter landscape, 4a's widths a third larger, as 17.3.4 wrote them", String.format(table, "R1_4B"), BEFORE);
		scale(b, 4, 3);
		report.add("4b " + grid(b));
		report.add("4c " + grid(addCase(pkg, "4c", "Letter landscape, a width: 100% table laid out at the line", String.format(table, "R1_4C"), null)));

		pkg.save(new File(outDir, DOCX));
		Files.write(new File(outDir, "README.txt").toPath(), readme(report).getBytes(StandardCharsets.UTF_8));
		System.out.println("Wrote " + DOCX + " and README.txt to " + outDir + ": " + report);
	}

	/**
	 * Adds the case's heading, then its content, converted with the text width given (null for
	 * the last section's).
	 */
	static List<Object> addCase(WordprocessingMLPackage pkg, String id, String what, String body, Integer textWidth) throws Exception {
		MainDocumentPart mdp = pkg.getMainDocumentPart();
		mdp.getContent().add(mdp.createStyledParagraphOfText("Heading2", "Case " + id + ": " + what));
		XHTMLImporterImpl importer = new XHTMLImporterImpl(pkg);
		importer.setTextWidth(textWidth);
		List<Object> content = new ArrayList<>(importer.convert(
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>" + body + "</body></html>", null));
		mdp.getContent().addAll(content);
		return content;
	}

	/** A paragraph with a red bottom border, which spans the text from margin to margin. */
	static void ruler(WordprocessingMLPackage pkg, Integer textWidth) throws Exception {
		addCase(pkg, "ruler", "the red line spans the text, margin to margin",
				"<p style=\"border-bottom:1px solid red\">ruler</p>", textWidth);
	}

	/** Ends the section so far, with the page given and 1in margins. */
	static void endSection(WordprocessingMLPackage pkg, PageSizePaper size, boolean landscape) {
		PageDimensions page = new PageDimensions();
		page.setPgSize(size, landscape);
		page.setMargins(MarginsWellKnown.NORMAL);
		ObjectFactory factory = new ObjectFactory();
		SectPr sectPr = factory.createSectPr();
		sectPr.setPgSz(page.getPgSz());
		sectPr.setPgMar(page.getPgMar());
		P p = factory.createP();
		PPr pPr = factory.createPPr();
		pPr.setSectPr(sectPr);
		p.setPPr(pPr);
		pkg.getMainDocumentPart().getContent().add(p);
	}

	/** Multiplies the table's w:gridCol and w:tcW by num/den. */
	static void scale(List<Object> content, int num, int den) {
		Tbl tbl = table(content);
		for (TblGridCol col : tbl.getTblGrid().getGridCol()) {
			col.setW(col.getW().multiply(BigInteger.valueOf(num)).divide(BigInteger.valueOf(den)));
		}
		for (Object row : tbl.getContent()) {
			for (Object cell : ((Tr) XmlUtils.unwrap(row)).getContent()) {
				Tc tc = (Tc) XmlUtils.unwrap(cell);
				tc.getTcPr().getTcW().setW(tc.getTcPr().getTcW().getW().multiply(BigInteger.valueOf(num)).divide(BigInteger.valueOf(den)));
			}
		}
	}

	static Tbl table(List<Object> content) {
		for (Object o : content) {
			if (XmlUtils.unwrap(o) instanceof Tbl) return (Tbl) XmlUtils.unwrap(o);
		}
		throw new IllegalStateException("no table");
	}

	static String grid(List<Object> content) {
		int sum = 0;
		for (TblGridCol col : table(content).getTblGrid().getGridCol()) sum += col.getW().intValue();
		return String.format("grid %d (%.2fin)", sum, sum / 1440.0);
	}

	static String indent(List<Object> content) {
		Tbl tbl = table(content);
		int ind = tbl.getTblPr().getTblInd() == null ? 0 : tbl.getTblPr().getTblInd().getW().intValue();
		return String.format("tblInd %d (%.2fin), %s", ind, ind / 1440.0, grid(content));
	}

	static String extent(List<Object> content) {
		StringBuilder xml = new StringBuilder();
		for (Object o : content) xml.append(XmlUtils.marshaltoString(o));
		Matcher m = Pattern.compile("<wp:extent cx=\"(\\d+)\" cy=\"(\\d+)\"").matcher(xml);
		if (!m.find()) throw new IllegalStateException("no image");
		return String.format("extent %.2fin x %.2fin", Long.parseLong(m.group(1)) / 914400.0, Long.parseLong(m.group(2)) / 914400.0);
	}

	/** A mid-grey 400 x 100 image with a black frame, so its edges are easy to find. */
	static String png() throws Exception {
		BufferedImage bi = new BufferedImage(400, 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		g.setColor(Color.GRAY);
		g.fillRect(0, 0, 400, 100);
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, 399, 99);
		g.drawRect(1, 1, 397, 97);
		g.dispose();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(bi, "png", bos);
		return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
	}

	static String readme(List<String> report) {
		return "docx4j-ImportXHTML CR-009: Word check, round 1\n"
			+ "Built by LayoutWidthProbeBuilder (docx4j-ImportXHTML-core-tests). As written:\n"
			+ "  " + String.join("\n  ", report) + "\n"
			+ "\n"
			+ "Open " + DOCX + " in Word 365 normally, not with Open and Repair. Section 1 is A4\n"
			+ "portrait (a 6.27in line), section 2 Letter landscape (9in). Each section starts with a\n"
			+ "red line that spans its text, margin to margin. Each case has a heading \"Case X\" and\n"
			+ "a token R1_X in its table, or in its heading for the images.\n"
			+ "\n"
			+ "Look at, per case (expected, from CR-009 section 4):\n"
			+ "1a. A4, width: 100% table written 7.1in: fitted to the line, as long as the red line.\n"
			+ "1b. The same, written at the line: the same as 1a.\n"
			+ "2a. A4, width: 100% image written 7.1in: does it run about 0.8in past the red line's\n"
			+ "    right end, into the margin, or is it shrunk to the line?\n"
			+ "2b. The same, written at the line (6.27in): as long as the red line.\n"
			+ "3a. A4, a centred 2in table laid out at 7.1in: right of centre, by about 0.4in.\n"
			+ "3b. The same, laid out at the line: centred under the red line.\n"
			+ "4a. Letter landscape, width: 100% table written 7.1in: kept at 7.1in, about 1.9in\n"
			+ "    short of the red line's right end.\n"
			+ "4b. Written 9.4in, as 17.3.4 wrote 4a: fitted to the line, as long as the red line.\n"
			+ "4c. Written at the line (9in): as long as the red line.\n"
			+ "\n"
			+ "Send back:\n"
			+ "- Word's version and build (File > Account > About Word), and any prompt or repair;\n"
			+ "- the document saved from Word as PDF, beside this README: cr009-layout-width-r1-word365.pdf;\n"
			+ "- anything that differs from the expectations above, in a sentence.\n";
	}

}

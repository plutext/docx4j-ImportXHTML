package org.docx4j.samples;

import java.io.File;

import org.docx4j.Docx4J;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.AltChunkType;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

/**
 * Open a docx containing an XHTML AltChunk,
 * and then convert that to normal docx content.
 * @author jharrop
 *
 */
public class ConvertAltChunks {

	/**
	 * @param args
	 */
	public static void main(String[] args)  throws Exception {

		WordprocessingMLPackage wordMLPackage = Docx4J.load(new File(System.getProperty("user.dir") + "/your.docx"));
				
				
		// Round trip
		WordprocessingMLPackage pkgOut = wordMLPackage.getMainDocumentPart().convertAltChunks();
		
		// Display result
		System.out.println(
				XmlUtils.marshaltoString(pkgOut.getMainDocumentPart().getJaxbElement(), true, true));
		
		pkgOut.save(new File(System.getProperty("user.dir") + "/OUT_AltChunkXHTMLRoundTrip.docx"));
		
		
	}

}

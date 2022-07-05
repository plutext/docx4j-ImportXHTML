package org.docx4j.samples;

import java.io.File;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPartAltChunkHost;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;

/**
 * Open a docx containing an XHTML or .mht AltChunk,
 * and then convert that to normal docx content.
 * @author jharrop
 *
 */
public class ConvertAltChunks {

	public static void main(String[] args)  throws Exception {

		WordprocessingMLPackage wordMLPackage = Docx4J.load(new File(System.getProperty("user.dir") + "your.docx"));
				
				
		// MDP
		wordMLPackage.getMainDocumentPart().convertAltChunks();
		
		// Headers/footers
	    for (Relationship rel : wordMLPackage.getMainDocumentPart().getRelationshipsPart().getRelationships().getRelationship() ) {
	    	
	    	if (rel.getType().equals(Namespaces.HEADER)
	    			|| rel.getType().equals(Namespaces.FOOTER)) {
	    		JaxbXmlPartAltChunkHost part = (JaxbXmlPartAltChunkHost)wordMLPackage.getMainDocumentPart().getRelationshipsPart().getPart(rel);
	    		System.out.println("processing " + part.getPartName().getName());
	    		part.convertAltChunks();
	    	}
	    }		
		// Display result
//		System.out.println(
//				XmlUtils.marshaltoString(pkgOut.getMainDocumentPart().getJaxbElement(), true, true));
		
	    wordMLPackage.save(new File(System.getProperty("user.dir") + "/OUT_ConvertAltChunks_all.docx"));
		
		
	}

}

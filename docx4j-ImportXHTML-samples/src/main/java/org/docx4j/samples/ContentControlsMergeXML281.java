/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package org.docx4j.samples;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.xml.bind.JAXBContext;

import org.docx4j.XmlUtils;
import org.docx4j.dml.CTBlip;
import org.docx4j.model.datastorage.BindingHandler;
import org.docx4j.model.datastorage.CustomXmlDataStoragePartSelector;
import org.docx4j.model.datastorage.OpenDoPEHandler;
import org.docx4j.model.datastorage.OpenDoPEIntegrity;
import org.docx4j.model.datastorage.RemovalHandler;
import org.docx4j.model.datastorage.RemovalHandler.Quantifier;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io.SaveToZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePart;
import org.docx4j.openpackaging.parts.CustomXmlPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.utils.SingleTraversalUtilVisitorCallback;
import org.docx4j.utils.TraversalUtilVisitor;
import org.docx4j.wml.SdtElement;
import org.opendope.xpaths.Xpaths.Xpath;


/** 
 * This sample demonstrates populating content controls
 * from a custom xml part (based on the xpaths given
 * in the content controls)
 * 
 * In this example, the XML part is injected at runtime,
 * and OpenDoPE extensions are supported (if present).
 * 
 * So this example is like 
 * See https://github.com/plutext/OpenDoPE-WAR/blob/master/webapp-simple/src/main/java/org/opendope/webapp/SubmitBoth.java
*/
public class ContentControlsMergeXML281 {
	
	public static JAXBContext context = org.docx4j.jaxb.Context.jc; 
	
	private final static boolean DEBUG = true;
	private final static boolean SAVE = true;
	

	public static void main(String[] args) throws Exception {
			
		// the docx 'template'
		String input_DOCX = System.getProperty("user.dir") + "/sample-docs/word/databinding/binding-simple.docx";
		
		// the instance data
		String input_XML = System.getProperty("user.dir") + "/sample-docs/word/databinding/binding-simple-data.xml";
		
		// resulting docx
		String OUTPUT_DOCX = System.getProperty("user.dir") + "/OUT_ContentControlsMergeXML.docx";

		// Load input_template.docx
		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(
				new java.io.File(input_DOCX));
				
		// Inject data_file.xml
		// (this code assumes it is not a StandardisedAnswersPart)
		CustomXmlPart customXmlDataStoragePart
			= CustomXmlDataStoragePartSelector.getCustomXmlDataStoragePart(wordMLPackage);		
		if (customXmlDataStoragePart==null) {
			System.out.println("Couldn't find CustomXmlDataStoragePart! exiting..");
			return;			
		}
		System.out.println("Getting " + input_XML);
		FileInputStream fis = new FileInputStream(new File(input_XML));
		customXmlDataStoragePart.setXML(
				XmlUtils.getNewDocumentBuilder().parse(fis));
		
		SaveToZipFile saver = new SaveToZipFile(wordMLPackage);
		OpenDoPEHandler odh = null;
		try {
			// Process conditionals and repeats
			odh = new OpenDoPEHandler(wordMLPackage);
			odh.preprocess();
			
			OpenDoPEIntegrity odi = new OpenDoPEIntegrity();
			odi.process(wordMLPackage);
			
			if (DEBUG) {
				String save_preprocessed; 						
				if (OUTPUT_DOCX.lastIndexOf(".")==-1) {
					save_preprocessed = OUTPUT_DOCX + "_INT.docx"; 
				} else {
					save_preprocessed = OUTPUT_DOCX.substring(0, OUTPUT_DOCX.lastIndexOf(".") ) + "_INT.docx"; 
				}
//				System.out.println(
//						XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true)
//						);		
				saver.save(save_preprocessed);
				System.out.println("Saved: " + save_preprocessed);
			}
			
		} catch (Docx4JException d) {
			// Probably this docx doesn't contain OpenDoPE convention parts
			System.out.println(d.getMessage());
		}
		
		
		// Apply the bindings
		BindingHandler.setHyperlinkStyle("Hyperlink");
		
		// For docx4j <= 3.2.0
		//BindingHandler.applyBindings(wordMLPackage.getMainDocumentPart());
		
		// For docx4j > 3.2.0, replace that with:
		
			AtomicInteger bookmarkId = odh.getNextBookmarkId();
			BindingHandler bh = new BindingHandler(wordMLPackage);
			bh.setStartingIdForNewBookmarks(bookmarkId);
			bh.applyBindings(wordMLPackage.getMainDocumentPart());
		
		
		// If you inspect the output, you should see your data in 2 places:
		// 1. the custom xml part 
		// 2. (more importantly) the main document part
//		System.out.println(
//				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true)
//				);
		
		// Strip content controls
		RemovalHandler rh = new RemovalHandler();
		rh.removeSDTs(wordMLPackage, Quantifier.ALL);

		saver.save(OUTPUT_DOCX);
		System.out.println("Saved: " + OUTPUT_DOCX);
		
	}
	
	
}

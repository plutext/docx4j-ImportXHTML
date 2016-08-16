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
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;

import org.docx4j.XmlUtils;
import org.docx4j.model.datastorage.BindingHandler;
import org.docx4j.model.datastorage.CustomXmlDataStoragePartSelector;
import org.docx4j.model.datastorage.OpenDoPEHandler;
import org.docx4j.model.datastorage.OpenDoPEIntegrity;
import org.docx4j.model.datastorage.OpenDoPEReverter;
import org.docx4j.model.datastorage.RemovalHandler;
import org.docx4j.model.datastorage.RemovalHandler.Quantifier;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io.SaveToZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePart;


/**
 * This sample demonstrates populating content controls
 * from a custom xml part (based on the xpaths given
 * in the content controls).
 * 
 * Word does this itself automatically, for if there is
 * a w:databinding element in the sdtPr.
 * 
 * However, out of the box, Word doesn't allow for
 * repeating things (table rows, paragraphs etc), nor
 * conditional inclusion/exclusion, nor XHTML import.
 * 
 * The OpenDoPE conventions support those things;  this sample 
 * demonstrates docx4j's implementation of import of escaped XHTML
 * content via a bound control.
*/
public class ContentControlBindingExtensions {
	
	public static JAXBContext context = org.docx4j.jaxb.Context.jc; 

	static String filepathprefix;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		String inputfilepath = System.getProperty("user.dir") + "/sample-docs/word/databinding/invoice.docx";

		
		String data = System.getProperty("user.dir") + "/sample-docs/word/databinding/invoice-data.xml";


		WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(new java.io.File(inputfilepath));		
		
		filepathprefix = inputfilepath.substring(0, inputfilepath.lastIndexOf("."));
		System.out.println(filepathprefix);
		
		StringBuilder timingSummary = new StringBuilder();

		
		// Find custom xml item id and inject data_file.xml		
		long startTime = System.currentTimeMillis();
		CustomXmlDataStoragePart customXmlDataStoragePart 
			= CustomXmlDataStoragePartSelector.getCustomXmlDataStoragePart(wordMLPackage);		
		if (customXmlDataStoragePart==null) {
			throw new RuntimeException("no xml");
		}
		customXmlDataStoragePart.getData().setDocument(new FileInputStream(new File(data)));
		long endTime = System.currentTimeMillis();
		timingSummary.append("\nmerge data: " + (endTime-startTime));
		System.out.println("data merged");
	
		SaveToZipFile saver = new SaveToZipFile(wordMLPackage);		
		saver.save(new File(System.getProperty("user.dir") + "/OUT_injected.docx"));
		
		

		// Process conditionals and repeats
		 startTime = System.currentTimeMillis();
		OpenDoPEHandler odh = new OpenDoPEHandler(wordMLPackage);
		odh.preprocess();
		 endTime = System.currentTimeMillis();
		timingSummary.append("OpenDoPEHandler: " + (endTime-startTime));

//		System.out.println(
//				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true)
//				);		
		saver.save(filepathprefix + "_1_preprocessed.docx");
		System.out.println("Saved: " + filepathprefix + "_1_preprocessed.docx");
		
		startTime = System.currentTimeMillis();
		OpenDoPEIntegrity odi = new OpenDoPEIntegrity();
		odi.process(wordMLPackage);
		endTime = System.currentTimeMillis();
		timingSummary.append("\nOpenDoPEIntegrity: " + (endTime-startTime));
		
//		System.out.println(
//				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true)
//				);		
		saver = new SaveToZipFile(wordMLPackage);
		saver.save(filepathprefix + "_2_integrity.docx");
		System.out.println("Saved: " + filepathprefix + "_2_integrity.docx");
		
		// Apply the bindings
		saver = new SaveToZipFile(wordMLPackage);		
		
		BindingHandler.setHyperlinkStyle("Hyperlink");						
		startTime = System.currentTimeMillis();

		
//			AtomicInteger bookmarkId = odh.getNextBookmarkId();
		AtomicInteger bookmarkId = new AtomicInteger();

		BindingHandler bh = new BindingHandler(wordMLPackage);
		bh.setStartingIdForNewBookmarks(bookmarkId);
		bh.applyBindings(wordMLPackage.getMainDocumentPart());
		
		
		endTime = System.currentTimeMillis();
		timingSummary.append("\nBindingHandler.applyBindings: " + (endTime-startTime));
//		System.out.println(
//				XmlUtils.marshaltoString(wordMLPackage.getMainDocumentPart().getJaxbElement(), true, true)
//				);
		saver.save(filepathprefix + "_3_bound.docx");
		System.out.println("Saved: " + filepathprefix + "_3_bound.docx");
		
		// Either demonstrate reverter, or stripping of controls;
		// you can't do both. So comment out one or the other.
//		reverter(inputfilepath, filepathprefix + "_bound.docx");
//		
		// Strip content controls
		startTime = System.currentTimeMillis();
		RemovalHandler rh = new RemovalHandler();
		rh.removeSDTs(wordMLPackage, Quantifier.ALL);
		endTime = System.currentTimeMillis();
		timingSummary.append("\nRemovalHandler: " + (endTime-startTime));

		saver.save(filepathprefix + "_4_stripped.docx");
		System.out.println("Saved: " + filepathprefix + "_4_stripped.docx");
		
		System.out.println(timingSummary);
	}	
	
	public static void reverter(String inputfilepath, String instancePath) throws Docx4JException {
		
		WordprocessingMLPackage instancePkg = WordprocessingMLPackage.load(new java.io.File(instancePath));		
		
		OpenDoPEReverter reverter = new OpenDoPEReverter(
				WordprocessingMLPackage.load(new java.io.File(inputfilepath)), 
				instancePkg);
		
		System.out.println("reverted? " + reverter.revert() );
		
		SaveToZipFile saver = new SaveToZipFile(instancePkg);
		saver.save(filepathprefix + "_5_reverted.docx");
		System.out.println("Saved: " + filepathprefix + "_5_reverted.docx");
		
	}
				

}

package org.pptx4j.samples;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.docx4j.openpackaging.packages.PresentationMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.PresentationML.MainPresentationPart;
import org.docx4j.openpackaging.parts.PresentationML.SlideLayoutPart;
import org.docx4j.openpackaging.parts.PresentationML.SlidePart;
import org.pptx4j.convert.in.xhtml.XHTMLtoPPTX;

public class XHTMLFileToSlide {
	
	   public static void main(String[] args) throws Exception {
	        
			// Where will we save our new .ppxt?
			String outputfilepath = System.getProperty("user.dir") + "/OUT_XHTMLFileToSlide.pptx";
	    	
	        String inputfilepath = System.getProperty("user.dir") + "/fragment.html";    	
	        String baseUrl = "file:///C:/Users/jharrop/git/docx4j-ImportXHTML/";

	        String TXBODY_SHAPE_TEMPLATE =            
                    "<p:sp xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">" +
                    "  <p:nvSpPr>" + 
                    "    <p:cNvPr id=\"4\" name=\"Title 3\" />" +
                    "    <p:cNvSpPr>" +
                    "      <a:spLocks noGrp=\"1\" />" +
                    "    </p:cNvSpPr>" +
                    "    <p:nvPr>" +
                    "      <p:ph type=\"title\" />" +
                    "    </p:nvPr>" +
                    "  </p:nvSpPr>" +
                    "  <p:spPr />" +
                    "  <p:txBody>" +
                    "    <a:bodyPr />" +
                    "    <a:lstStyle />" +
                    "  </p:txBody>" +
                    "</p:sp>";
	        
	        String stringFromFile = FileUtils.readFileToString(new File(inputfilepath), "UTF-8");
	        
	        String content = stringFromFile;
	        
			// Setup target pptx
			PresentationMLPackage presentationMLPackage = getPkg();
			SlidePart slidePart =(SlidePart) presentationMLPackage.getParts().get(new PartName("/ppt/slides/slide1.xml"));
			// TODO - add a convenience method to get slide by slide number!
					
					
			// Process XHTML
			XHTMLtoPPTX converter = new XHTMLtoPPTX(presentationMLPackage, slidePart, content, baseUrl);
			converter.setTxBodyShapeTemplate(TXBODY_SHAPE_TEMPLATE);
			List<Object> results = converter.convertSingleSlide();
			
			System.out.println("Got results: " + results.size());

			// Add results to slide
			slidePart.getJaxbElement().getCSld().getSpTree().getSpOrGrpSpOrGraphicFrame().addAll(results);
			
			// All done: save it
			presentationMLPackage.save(new java.io.File(outputfilepath));

			System.out.println("\n\n done .. saved " + outputfilepath);
			
	   }
	   
		public static PresentationMLPackage getPkg() throws Exception {

			
			// Create skeletal package, including a MainPresentationPart and a SlideLayoutPart
			PresentationMLPackage presentationMLPackage = PresentationMLPackage.createPackage(); 
			
			// Need references to these parts to create a slide
			// Please note that these parts *already exist* - they are
			// created by createPackage() above.  See that method
			// for instruction on how to create and add a part.
			MainPresentationPart pp = (MainPresentationPart)presentationMLPackage.getParts().getParts().get(
					new PartName("/ppt/presentation.xml"));		
			SlideLayoutPart layoutPart = (SlideLayoutPart)presentationMLPackage.getParts().getParts().get(
					new PartName("/ppt/slideLayouts/slideLayout1.xml"));
			
			// OK, now we can create a slide
			SlidePart slidePart = new SlidePart(new PartName("/ppt/slides/slide1.xml"));
			slidePart.setContents( SlidePart.createSld() );		
			pp.addSlide(0, slidePart);
			
			// Slide layout part
			slidePart.addTargetPart(layoutPart);
			
					
			
			return presentationMLPackage;
		}	   

}

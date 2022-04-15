module docx4j_ImportXHTML {

	requires org.slf4j;
	requires org.docx4j.core;
	requires org.docx4j.openxml_objects;
	
	requires jakarta.xml.bind;
	requires openhtmltopdf.core;
	requires openhtmltopdf.pdfbox;
	requires org.apache.pdfbox;
	
	exports org.docx4j.convert.in.xhtml;
	exports org.pptx4j.convert.in.xhtml;
	
}

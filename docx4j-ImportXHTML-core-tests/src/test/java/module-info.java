module docx4j_ImportXHTML_tests {

	requires org.slf4j;
	requires docx4j_ImportXHTML;
	requires org.docx4j.core;
	requires jakarta.xml.bind;
	requires org.apache.commons.lang3;
	requires jdk.httpserver; // ExternalResourcesTest
	requires org.jsoup; // MHTImportTest: convertMHT reads HTML with it
	
	requires junit;
	
	exports  org.docx4j.convert.in.xhtml.tests; // to JUnit
}

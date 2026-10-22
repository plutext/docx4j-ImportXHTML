docx4j-ImportXHTML
==================

Converts XHTML to OpenXML WordML (docx) using docx4j; there is also some support for converting to pptx.

docx4j is licensed under ASLv2.  This project is licensed under LGPL v2.1 (or later), which is the license used by openhtmltopdf (the main dependency).  See legals/NOTICE for details.

In Maven this is https://central.sonatype.com/artifact/org.docx4j/docx4j-ImportXHTML-core

To use docx4j-ImportXHTML, also add the dep corresponding to the JAXB implementation you wish to use:

* https://central.sonatype.com/artifact/org.docx4j/docx4j-JAXB-ReferenceImpl
* https://central.sonatype.com/artifact/org.docx4j/docx4j-JAXB-MOXy

With docx4j-bundle
------------------

If you use docx4j-bundle (docx4j's fat jar) rather than docx4j-core, use https://central.sonatype.com/artifact/org.docx4j/docx4j-ImportXHTML-bundle instead of -core.  It is a fat jar of docx4j-ImportXHTML, openhtmltopdf, PDFBox, xmpbox, graphics2d, apache-mime4j and jsoup, holding nothing docx4j-bundle does; its pom depends on docx4j-bundle alone.  Without Maven, put the two jars on the classpath side by side.

Use it with the docx4j-bundle version its pom names: the jar is checked against that one, and another may duplicate or miss classes.  It has no module-info, so on the module path use -core.

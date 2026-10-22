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

Converting HTML you didn't write
--------------------------------

If the HTML comes from someone else (an editor's users, an email, an uploaded file):

* Call setExternalResourcesAllowed(false), so that nothing the HTML refers to is fetched: stylesheets, images and fonts, over http or from file: alike.  By default they are fetched, with openhtmltopdf's timeouts (10 s to connect, 30 s for each read).
* Links: by default (setLinkSchemesChecked), an href whose scheme isn't allowed (javascript:, vbscript:, data: and the like) gives its text without a link.  Relative links, fragments, drive and UNC paths, and http, https, ftp, mailto, file, news and tel are allowed; the property docx4j-ImportXHTML.Hyperlink.Schemes adds schemes.
* Nesting: openhtmltopdf's layout is recursive, so HTML nested a few hundred elements deep (about 730 divs, on a 1 MB thread stack) gives a StackOverflowError.  Convert such HTML on a thread with a bigger stack, or check its depth first.
* XML entities: external entities and DTDs aren't loaded, and the JDK's parser limits entity expansion.

See docs/change-requests/CR-005.

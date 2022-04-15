package org.docx4j.convert.in.xhtml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.xml.bind.JAXBException;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTBookmark;
import org.docx4j.wml.CTMarkupRange;
import org.junit.Before;
import org.junit.Test;

/**
 * Test conversion of @id and a/@name to bookmark.
 * 
 * Some of hte a/@name cases are tested in HyperlinkTest
 * 
 * @author jharrop
 *
 */
public class DestinationAnchorTest {
	
	private WordprocessingMLPackage wordMLPackage;
	
	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	private List<Object> convert(String xhtml) throws Docx4JException {
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		return XHTMLImporter.convert(xhtml, "");
	}


	@Test public void bookmarkDiv() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div id=\"" + id + "\">foo</div>";
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}
	
	@Test public void bookmarkInDivP() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div>" +
    				"<p  id=\""+ id + "\">foo</p>" +				
				"</div>"
;
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}
	
	
	@Test public void bookmarkInP() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<p>Hello <span id=\"" + id + "\">foo</span> done</p>";
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}
	
	@Test public void bookmarkInTable() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div>" +
    			"<table id=\"" + id + "\"><tr><th>1</th></tr></table>" +
				"</div>"
;
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}
	
	@Test public void bookmarkInTr() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div>" +
    			"<table><tr  id=\"" + id + "\"><th>1</th></tr></table>" +
				"</div>"
;
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}

	@Test public void bookmarkInTd() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div>" +
    			"<table><tr><td  id=\"" + id + "\">1</td></tr></table>" +
				"</div>"
;
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}

	@Test public void bookmarkInTdP() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div>" +
    			"<table><tr><td>" +
    				"<p  id=\""+ id + "\">foo</p>" +				
    					"</td></tr></table>" +
				"</div>"
;
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}

	@Test public void bookmarkUL() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = 
			"<ul>"+
	    	"  <li> Outer 1 </li>"+
	    	 " <li> Outer 2 </li>"+
	    	  "  <ul  id=\""+ id + "\">"+
	    	   "   <li> Inner 1 </li>"+
	    	    "  <li> Inner 2 </li>"+
	    	    "</ul>"+
	    	 " <li> Outer 3 </li>"+
	    	"</ul>";		
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}

	@Test public void bookmarkLI() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = 
			"<ul>"+
	    	"  <li> Outer 1 </li>"+
	    	 " <li> Outer 2 </li>"+
	    	  "  <ul >"+
	    	   "   <li  id=\""+ id + "\"> Inner 1 </li>"+
	    	    "  <li> Inner 2 </li>"+
	    	    "</ul>"+
	    	 " <li> Outer 3 </li>"+
	    	"</ul>";		
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}

    private final String PNG_IMAGE_DATA = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";
	
	@Test public void bookmarkImg() throws Docx4JException, JAXBException {
		
		String id = "mydiv";
		String content = "<div><img  id=\""+ id + "\" src='" + PNG_IMAGE_DATA + "'/></div>";
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		
		assertTrue(start.getName().equals(id));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));

		assertEquals(start.getId(), end.getId());
	}

	@Test public void bookmarksNested() throws Docx4JException, JAXBException {
		
		String id = "id1";
		String id2 = "id2";
		String content = 
				"<ul>"+
				    	"  <li> Outer 1 </li>"+
				    	 " <li> Outer 2 </li>"+
				    	  "  <ul  id=\""+ id + "\">"+
				    	   "   <li  id=\""+ id2 + "\"> Inner 1 </li>"+
				    	    "  <li> Inner 2 </li>"+
				    	    "</ul>"+
				    	 " <li> Outer 3 </li>"+
				    	"</ul>";
		
		wordMLPackage.getMainDocumentPart().getContent().addAll(
				convert(content));
		
		System.out.println(wordMLPackage.getMainDocumentPart().getXML());
		
		List<Object> matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkStart", false);
		assertTrue(matches.size()==2);
		CTBookmark start = (CTBookmark)XmlUtils.unwrap(matches.get(0));
		CTBookmark start2 = (CTBookmark)XmlUtils.unwrap(matches.get(1));
		
		assertTrue(start.getName().equals(id));
		assertTrue(start2.getName().equals(id2));

		matches = wordMLPackage.getMainDocumentPart().getJAXBNodesViaXPath("//w:bookmarkEnd", false);
		CTMarkupRange end2 = (CTMarkupRange)XmlUtils.unwrap(matches.get(0));
		CTMarkupRange end1 = (CTMarkupRange)XmlUtils.unwrap(matches.get(1));

		assertEquals(start.getId(), end1.getId());
		assertEquals(start2.getId(), end2.getId());
	}
	
}

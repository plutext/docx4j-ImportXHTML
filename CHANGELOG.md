CHANGELOG
=========


Version 3.3.0 (minor release)
===============

Release date
------------

21 April 2016


Notable Changes in Version 3.3.0
---------------------------------

Image size (where specified in HTML)

Nested table handling

Bump dependent jar versions


Compatibility notes
-------------------

Uses docx4j 3.3.0 (though not heavily dependent on it)



Version 3.2.2
===============

Release date
------------

28 Dec 2014

Contributors to this release
----------------------------

Haarli
Jason Harrop

Notable Changes in Version 3.2.2
---------------------------------

Paragraph borders
Table cell padding (to margin) 
Support for <hr/>
Create/add p for div



Version 3.2.1
===============

Release date
------------

20 Sept 2014

Contributors to this release
----------------------------

Jason Harrop
Roded

Notable Changes in Version 3.2.1
---------------------------------

TR height support.

Support for @start, as in <ol start="5">

table cell support @valign="bottom", @style="vertical-align:bottom;"

Refactor: move table stuff into a helper class

Support for p and table elements in list item

interface DivHandler: Customisable div handling
- DivToSdt implementation: allows roundtripping of divs by ID
  See further: http://www.docx4java.org/blog/2014/09/xhtml-docx-roundtrip-content-tracking/
  and samples/DivRoundtrip.java

Convert a relative link (#) to w:hyperlink/@w:anchor (in createHyperlink method).
- Convert @id (on most elements) to bookmark
- Refactor code into new BookmarkHelper


Compatibility notes
-------------------

We will try to ensure that any formal releases of 3.2.x of docx4j and 3.2.y of ImportXHTML 
can be used together.

- https://github.com/plutext/docx4j/commit/df5d726e3359b29893153433082c13ed6c4eb56d 
  17 Sept 2014 introduces 
  
			BindingHandler bh = new BindingHandler(wordMLPackage);
			bh.setStartingIdForNewBookmarks(bookmarkId);
			
  Use of that code is commented out in the relevant samples here; those samples assume docx4j 3.2.0.
  
At some point we might add to XHTMLImporter interface in docx4j:
  
     	public AtomicInteger getBookmarkIdLast();      	
		public void setBookmarkIdNext(AtomicInteger val);
		
If we did that, a docx4j using that would need the implementation (in ImportXHTML 3.2.1 or greater).

Because a docx4j using that would not work with ImportXHTML 3.2.0, it would have to be
numbered 3.3.

But we avoid all this by leaving the XHTMLImporter interface as-is, and using that
method via reflection only.
  


Version 3.2.0
=============

Release date
------------

26 August 2014

Notable Changes in Version 3.2.0
---------------------------------

HTML 5 figure/figcaption converted to Figure SEQ field 

Support for applying Word heading styles to heading (h1, h2, h3..) elements

FontHandler, which creates default font mappings



Version 3.1.0
=============

No release, since no changes in 3.0.1


Version 3.0.1
=============


Release date
------------

8 Feb 2014

Contributors to this release
----------------------------

ai-github
Jason Harrop
leedavidr 


Notable Changes in Version 3.0.1
---------------------------------

Nil

Other Changes (non-exhaustive)
------------------------------

0d6c106 - if (color.startsWith("#")) .. Fix for plutext/docx4j#101
a452b43 - BindingTraverser.XHTML.Block.rStyle.Adopt handling in list items
6a7b6fc - NPE in indent handling fixed
e902f20 - xhtml to pptx
			Very rudimentary support for h1-3, ol|ul, li
			Write all paragraphs <a:p> into a single <p:txBody>


Version 3.0.0
=============

3.0.0 is the first release of the XHTML Import stuff as a separate project; 
so numbered to match corresponding docx4j version.


Release date
------------

26 Nov 2013

Contributors to this release
----------------------------

ai
bezda
bsl-zcs
EthanTsui
hpeng
Jason Harrop
jlesquembre
jeffbeard
fmmfonseca
meletis
pnml
siilk
tj09
tstirrat
vollewijn
zluspai


Notable Changes in Version 3.0.0
---------------------------------

Better table support, inc merged cells (pnml) and cell border support (bsl-zcs)
Font handling
Conversion of CSS class to existing matching  Word style (FormattingOption enum: CLASS_TO_STYLE_ONLY, CLASS_PLUS_OTHER, IGNORE_CLASS) 
Nested list support
Image resizing
 

Other Changes (non-exhaustive)
------------------------------

35fae5e - Handle inline images (avoid forcing new paragraph afterwards)
008604c - Ability to require a CSS property to be on a white list 
5b69f1d - addFontMapping(String cssFontFamily, RFonts rFonts) 
3b11904 - tables: import cell border styles (bsl-zcs) 
bcfe159 - XHTML import hyperlink named anchors to bookmarks 
e486b05 - XHTMLImporter: improve logging; better support for pixel unit in Indent and SpaceAfter. 
6a02270 - Support underline in XHTML import. 
e0034b2 - XHTML importer for presentation slide notes, needs work on element positioning to support imports of slides (bezda)
c297662 - XHTML: margin-bottom -> space-after
7d8af58 - XHTML table import: support for adjacent vertically merged cells and cells merged both vertically and horizontally (pnml)
5553600 - fixed vertical merging of table cells in xhtml importer (cherry-picked from 05fdacb)

CHANGELOG
=========


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

35fae5e- Handle inline images (avoid forcing new paragraph afterwards)
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

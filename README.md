[![Build Status](https://api.travis-ci.org/danfickle/openhtmltopdf.svg?branch=open-dev-v1)](https://travis-ci.org/danfickle/openhtmltopdf)

# OPEN HTML TO PDF

## CURRENTY SEEKING FEEDBACK
+ [Thinking of moving to Java 8](https://github.com/danfickle/openhtmltopdf/issues/280)
+ [Roadmap for version 1](https://github.com/danfickle/openhtmltopdf/issues/170)

## OVERVIEW
Open HTML to PDF is a pure-Java library for rendering arbitrary well-formed XML/XHTML (and even HTML5)
using CSS 2.1 for layout and formatting, outputting to PDF or images.

Use this library to generated nice looking PDF documents. But be aware that you can not throw modern HTML5+ at
this engine and expect a great result. You must special craft the HTML document for this library and 
use it's extended CSS feature like [#31](https://github.com/danfickle/openhtmltopdf/pull/31) or
[#32](https://github.com/danfickle/openhtmltopdf/pull/32) 
to get good results. Avoid floats near page breaks and use table layouts.

## GETTING STARTED
+ [Showcase Document - PDF](https://openhtmltopdf.com/showcase.pdf)
+ [Integration guide](docs/integration-guide.md) - get maven artifacts and code to get started.
+ [Documentation wiki](https://github.com/danfickle/openhtmltopdf/wiki)
+ [Template Author Guide - PDF - DEPRECATED - Prefer wiki](https://openhtmltopdf.com/template-guide.pdf) - Moving info to wiki
+ [RC11 Online Sandbox](https://sandbox.openhtmltopdf.com/) - Please do not abuse.
+ [Sample Project - Pretty Resume Generator](https://github.com/danfickle/pretty-resume)

## LICENSE
Open HTML to PDF is distributed under the LGPL.  Open HTML to PDF itself is licensed 
under the GNU Lesser General Public License, version 2.1 or later, available at
http://www.gnu.org/copyleft/lesser.html. You can use Open HTML to PDF in any
way and for any purpose you want as long as you respect the terms of the 
license. A copy of the LGPL license is included as license-lgpl-2.1.txt or license-lgpl-3.txt
in our distributions and in our source tree.

Open HTML to PDF uses a couple of FOSS packages to get the job done. A list
of these, along with the license they each have, is listed in the 
LICENSE file in our distribution.   

## CREDITS
Open HTML to PDF is based on [Flying-saucer](https://github.com/flyingsaucerproject/flyingsaucer). Credit goes to the contributors of that project. Code will also be used from [neoFlyingSaucer](https://github.com/danfickle/neoflyingsaucer)

## FAQ
+ OPEN HTML TO PDF is tested with OpenJDK 7 and 11, Oracle JDK 8, 9, 11.
+ No, you can not use it on Android.
+ You should be able to use it on Google App Engine (Java 8 or greater environment). [Let us know your experience](https://github.com/danfickle/openhtmltopdf/issues/179).
+ <s>Flowing columns are not implemented.</s> Implemented in RC12.
+ No, it's not a web browser.

## TEST CASES
Test cases, failing or working are welcome, please place them
in ````/openhtmltopdf-examples/src/main/resources/testcases/````
and run them
from ````/openhtmltopdf-examples/src/main/java/com/openhtmltopdf/testcases/TestcaseRunner.java````.

## CHANGELOG

### head - 0.0.1-RC17-SNAPSHOT


### 0.0.1-RC16
+ [#279](https://github.com/danfickle/openhtmltopdf/pull/279) [#264](https://github.com/danfickle/openhtmltopdf/pull/264)
IMPORTANT: This release was brought forward so that we link against PDFBOX-2.0.12 as previous versions had another DOS security vulnerability when parsing arbitary PDF files. Also there was a security issue in the old version of JSoup used by the optional jsoup-dom-converter module.
  While I believe these vulnerabilities should not impact this project directly, having an insecure library on your classpath may be dangerous if you use it for other tasks.
  Thanks @rototor, @dheid
+ [#279](https://github.com/danfickle/openhtmltopdf/pull/279) Support for testing and running on JDK-11. Extensive work by @rototor. Thanks.
+ [#278](https://github.com/danfickle/openhtmltopdf/pull/278) Support for additional PDF/A conformance levels.  Thanks @TheUnnamedDude
+ [87dc1a9](https://github.com/danfickle/openhtmltopdf/commit/87dc1a98f5821b4b80b6f85db93def53f770ecc5) Fixed nasty bug where positioned elements (absolute, fixed) were being printed twice. By @danfickle
+ [#271](https://github.com/danfickle/openhtmltopdf/pull/271) Support right-to-left list items. Thanks @ieugen for work, @sandre1 for reporting.
+ Much more work on the fast renderer. But not ready for prime time yet!


### 0.0.1-RC15
+ NOTE: Started moving [project documentation to wiki](https://github.com/danfickle/openhtmltopdf/wiki).
+ [#228](https://github.com/danfickle/openhtmltopdf/issues/228) Support for letter-spacing CSS property. By @danfickle
+ [#143](https://github.com/danfickle/openhtmltopdf/pull/143) Merging of remaining items thanks to @backslash47
  + Support for ```box-sizing:border-box```. With additional work (for min/max width/height) by @danfickle
  + Text justification for embedded unicode fonts
  + [#250](https://github.com/danfickle/openhtmltopdf/pull/250) Optional PDF/A conformance. Thanks @syjer
+ [#252](https://github.com/danfickle/openhtmltopdf/issues/252) Incorrect placement of form controls. Thanks @tiredelk
+ [#249](https://github.com/danfickle/openhtmltopdf/pull/249) Cache font metrics across runs to avoid having to load fallback fonts on each run. By @danfickle
+ [#254](https://github.com/danfickle/openhtmltopdf/pull/254) Allow use of SVG image in image tag. Thanks @syjer


### 0.0.1-RC14
+ IMPORTANT: This release was brought forward so that we link against PDFBOX-2.0.11 as previous versions had a security vulnerability when parsing arbitary PDF files.
  While I believe this should not impact this project directly, having an insecure library on your classpath may be dangerous if you use it for other tasks.
  [#241](https://github.com/danfickle/openhtmltopdf/issues/241) [#239](https://github.com/danfickle/openhtmltopdf/pull/239) Thanks @rototor, @cseblog
+ NOTE: This release incorportate a new faster renderer (especially for large documents) that is in alpha state. Specifically, it can be used with everything except inline-blocks.
  You can start testing it now with ````builder.useFastMode()```` [#180](https://github.com/danfickle/openhtmltopdf/issues/180) Thanks @rajaningle @javimartinez @dilworks @rototor
+ Image with CSS max-width and max-height incorrectly scaled [#242](https://github.com/danfickle/openhtmltopdf/issues/242) Thanks @koan00
+ Bold and italic emulation [#240](https://github.com/danfickle/openhtmltopdf/pull/240) Thanks @syjer @backslash47
+ Work on correctly outputting multiple HTML files to one PDF [#222](https://github.com/danfickle/openhtmltopdf/pull/222) Thanks @rototor
+ ONGOING: Attempt at fixing font file handle leak [#215](https://github.com/danfickle/openhtmltopdf/pull/215) Thanks @rototor
+ Don't throw NPE when no base URI is provided [#206](https://github.com/danfickle/openhtmltopdf/issues/206)
+ [Fix link annotation placement in margin or generated boxes](https://github.com/danfickle/openhtmltopdf/issues/213) Thanks @jesselong, @Kuhlware, @markhowardnz 


### 0.0.1-RC13
+ [Use common base class for PDF and Java2D builder - SOME IMPORTS MAY CHANGE](https://github.com/danfickle/openhtmltopdf/pull/177) Thanks @rototor
+ Major work on transforms, we're getting there, but still test well before use. Thanks @rototor
+ [Make it possible to set a PDF producer](https://github.com/danfickle/openhtmltopdf/pull/158) Thanks @schmitch
+ [Support for JFreeCharts diagrams with simple markup](https://github.com/danfickle/openhtmltopdf/pull/165) Thanks @rototor
+ [Ability to stamp another PDF on page](https://github.com/danfickle/openhtmltopdf/pull/165) Thanks @rototor
+ [Support for Latex](https://github.com/danfickle/openhtmltopdf/pull/177) Thanks @rototor
+ [Support for MathML](https://github.com/danfickle/openhtmltopdf/issues/161#issuecomment-365844595) Thanks @m-a-t
+ [Support for HTML and MathML character entities such as nbsp, etc](https://github.com/danfickle/openhtmltopdf/blob/19828ff579863fdbd0ffe28108d3b14626ec64da/openhtmltopdf-examples/src/main/resources/documentation/documentation.md#character-entities)
+ [Allow user to construct their own PDDocument so memory options can be specified](https://github.com/danfickle/openhtmltopdf/issues/180) Thanks @rajaningle, @javimartinez
+ [Fixed silly bug and got dramatic performance improvement for large documents](https://github.com/danfickle/openhtmltopdf/issues/170#issuecomment-376441271)
+ [Fix for XML loading problems where older version of xerces was being picked up](https://github.com/danfickle/openhtmltopdf/issues/187) Thanks @sosnut
+ [Fix for XML loading problems with JBoss,Wildfly](https://github.com/danfickle/openhtmltopdf/issues/54) Thanks @estevandiedrich, @alanhay
+ [Support image maps on img, object and svg](https://github.com/danfickle/openhtmltopdf/pull/163) Uses a kong polygon tesselation implementation by [sunshine2k](https://www.sunshine2k.de/coding/java/Polygon/Kong/Kong.html)
+ API BREAKING CHANGE: [Support custom shape <=> link maps in object drawers](https://github.com/danfickle/openhtmltopdf/pull/163)

Note: Shaped links only work in Acrobat Reader. All other PDF reader seem to ignore them.

### 0.0.1-RC12
+ [Upgrade the PDFBox to 2.0.8 and PDFBox-Graphics2D to 0.10 versions again](https://github.com/danfickle/openhtmltopdf/pull/150) Thanks @rototor
+ [Fix incorrect strikethrough offset](https://github.com/danfickle/openhtmltopdf/issues/136) Thanks @alebar, @backslash47, @izhenka
+ Allow percentages for max-width and max-height of images Thanks @backslash47
+ [Better sizing system for inline SVG images](https://github.com/danfickle/openhtmltopdf/issues/128) Thanks @harbulot
+ [Allow uri resolver to resolve uri before checking if a data uri](https://github.com/danfickle/openhtmltopdf/pull/132) Thanks @AlbanSeurat
+ [Ability to run examples from maven](https://github.com/danfickle/openhtmltopdf/pull/108) Thanks @jartysiewicz
+ [Fix to allow jar scheme urls](https://github.com/danfickle/openhtmltopdf/issues/125) Thanks @geesen
+ [Font mapping in custom object drawer rather than using vector shapes](https://github.com/danfickle/openhtmltopdf/pull/121) Thanks @rototor
+ [Upgraded PDFBOX to 2.0.7, ICU4J to 59.1 and PDFBOX-GRAPHICS2D to 0.7](https://github.com/danfickle/openhtmltopdf/pull/121) Thanks @rototor
+ [Implemented CSS3 flowing text columns](https://github.com/danfickle/openhtmltopdf/issues/60#issuecomment-310959602) Thanks @miminno
+ [FIX: Don't write miter values of zero into the PDF, fixes dotted/dashed lines in Acrobat Reader](https://github.com/danfickle/openhtmltopdf/issues/135)

### 0.0.1-RC11
+ [Allow collapsed borders with table pagination](https://github.com/danfickle/openhtmltopdf/issues/97) Thanks @Epimetheus89 
+ [FIX: Dispose of thread local when renderer is cleaned up](https://github.com/danfickle/openhtmltopdf/issues/94) Thanks @rototor
+ [FIX: Link handling when identical link positions on multiple pages](https://github.com/danfickle/openhtmltopdf/pull/95) Thanks @rototor
+ [FIX: Allow user to disable logging](https://github.com/danfickle/openhtmltopdf/pull/90) Thanks @GrammyTraore
+ [Handle TrueType font collections added in builder](https://github.com/danfickle/openhtmltopdf/pull/89) Thanks @rototor
+ [Implement custom object drawer for Java2D output](https://github.com/danfickle/openhtmltopdf/pull/87) Thanks @rototor
+ [Upgrade PDFBox library to 2.05](https://github.com/danfickle/openhtmltopdf/pull/86) Thanks @rototor, PDFBox team


### 0.0.1-RC10
+ [Support for inline SVG images](https://github.com/danfickle/openhtmltopdf/issues/23) Thanks @rototor
+ [Support for outputting paged or continuous images](https://github.com/danfickle/openhtmltopdf/issues/73#issuecomment-291070264) Thanks @rototor


### 0.0.1-RC9
+ [Don't output acroform for formless document](https://github.com/danfickle/openhtmltopdf/issues/52) Thanks @aleksandr-m
+ [Upgraded to PDFBox 2.0.4](https://github.com/danfickle/openhtmltopdf/issues/59) Thanks PDFBox team
+ [Fixed memory leak - properly - in image processing on some JREs](https://github.com/danfickle/openhtmltopdf/issues/51) Thanks @skjardenCode and @MartyMcMartface

### 0.0.1-RC8
+ [Initial support for CSS transform property](https://github.com/danfickle/openhtmltopdf/issues/38) Thanks @rototor
+ [Add support for max-width and max-height on img elements](https://github.com/danfickle/openhtmltopdf/pull/48) Thanks @achuinard

### 0.0.1-RC7
+ SECURITY ISSUE: [Prevent XXE Attacks](https://github.com/danfickle/openhtmltopdf/issues/44) Thanks @lillesand
+ BREAKING CHANGE: [Support for dir attribute and bdi element](https://github.com/danfickle/openhtmltopdf/issues/9#issuecomment-257072765)
+ [Do not download fonts that are not actually used](https://github.com/danfickle/openhtmltopdf/issues/43)
+ [Fixed resolution of relative URLs in inline style declarations](https://github.com/danfickle/openhtmltopdf/issues/27)
+ [Added support for hidden controls and submit controls with values](https://github.com/danfickle/openhtmltopdf/issues/24)
+ [Corrected naming scheme for form controls](https://github.com/danfickle/openhtmltopdf/issues/42) Thanks @scoldwell

### 0.0.1-RC5
+ [Reimplemented text justification](https://github.com/danfickle/openhtmltopdf/pull/33) Thanks @hiddendog 
+ [Fixed bug in table borders](https://github.com/danfickle/openhtmltopdf/pull/34) Thanks @rototor
+ [Added support for -fs-page-break-min-height CSS property](https://github.com/danfickle/openhtmltopdf/pull/31) Thanks @rototor
+ [Added support for -fs-table-paginate-repeated-visible CSS property](https://github.com/danfickle/openhtmltopdf/pull/32) Thanks @rototor
+ BREAKING CHANGE: Removed font subset method in builder, replaced with property in font face rule. Example: ````-fs-font-subset: complete-font;````
+ [Added support for text, password, textarea, submit, reset, checkbox, radio and select - multiple and single - controls](https://github.com/danfickle/openhtmltopdf/issues/24)
+ BREAKING CHANGE: Changed bi-directional method names in builder to be more consistent.
+ [Add method to builder to specify custom text transformers](https://github.com/danfickle/openhtmltopdf/issues/28)
+ [Add method to builder to specify a custom line breaker](https://github.com/danfickle/openhtmltopdf/issues/25) Thanks @Magotchi

### 0.0.1-RC4
+ Add method to builder to specify replacement text if no specified font can render a character.
+ [BREAKING CHANGE: Reworked URI resolver, changed FSUriResolver interface and made sure it is used everywhere](https://github.com/danfickle/openhtmltopdf/issues/27) - See example in integration guide.
+ Fixed issue where different size pages in the same document were not being recognized.
+ Add method to builder to specify default page size. Example: ````builder.useDefaultPageSize(PdfRendererBuilder.PAGE_SIZE_LETTER_WIDTH, PdfRendererBuilder.PAGE_SIZE_LETTER_HEIGHT, PdfRendererBuilder.PAGE_SIZE_LETTER_UNITS);````
+ BREAKING CHANGE: If no page size is specified in builder or CSS use A4, rather than locale dependent. See above.
+ [Silently discard control characters, etc at the rendering stage](https://github.com/danfickle/openhtmltopdf/issues/21#issuecomment-227850449) Thanks @scoldwell
+ [Fixed incorrect spacing when characters are replaced](https://github.com/danfickle/openhtmltopdf/issues/26) Thanks @scoldwell
 
### 0.0.1-RC3
+ [Experimental and unstable SVG support - early prototype](https://github.com/danfickle/openhtmltopdf/issues/23)
+ [Replaced non-breaking spaces - and other unusual spaces - with normal space if font does not support them](https://github.com/danfickle/openhtmltopdf/issues/21) Thanks @rototor
+ [Added a method for adding a PDF font using an input stream](https://github.com/danfickle/openhtmltopdf/issues/20) Thanks @aleksandr-m
+ [Added support for plugging in an external URI resolver](https://github.com/danfickle/openhtmltopdf/issues/18)
+ [Added support for plugging in an external cache](https://github.com/danfickle/openhtmltopdf/issues/18)
+ [Added support for font fallback for Java2D](https://github.com/danfickle/openhtmltopdf/issues/10) Thanks @willamette
+ [Fixed crash issue when document contained CDATA sections](https://github.com/danfickle/openhtmltopdf/issues/16) Thanks @hiddendog

### 0.0.1-RC2
+ [Added support for font fallback for PDFs](https://github.com/danfickle/openhtmltopdf/issues/10)
+ [Added fluent builder style API for PDF conversion](https://github.com/danfickle/openhtmltopdf/issues/14)
+ [Added ability to plugin external HTTP/HTTPS implementation](https://github.com/danfickle/openhtmltopdf/issues/13)
+ [Added Jsoup HTML5 to DOM converter module](https://github.com/danfickle/openhtmltopdf/issues/12)
+ Fixed divide-by-zero error in BorderPainter class. Thanks @fenrhil
+ [Added slf4j logging facade adapter](https://github.com/danfickle/openhtmltopdf/issues/11)
+ [Added right-to-left(RTL) and bi-directional text support](https://github.com/danfickle/openhtmltopdf/issues/9)
+ [Added output device using PDF-BOX 2.0.0](https://github.com/danfickle/openhtmltopdf/issues/1)
+ [Make sure XML Document Builder doesn't resolve external DTDs](https://github.com/danfickle/openhtmltopdf/issues/2)
+ [Removed obsolete ITEXT based output devices](https://github.com/danfickle/openhtmltopdf/issues/4)
+ [Removed SWT support](https://github.com/danfickle/openhtmltopdf/issues/6)
+ Regressions (please open issue if required):
	+ <s>PDF form controls.</s> [Reimplemented in RC5](https://github.com/danfickle/openhtmltopdf/issues/24)
	+ PDF font types other than built-in and truetype.
	+ XMP PDF metadata in PDFs.
	+ <s>PDF encryption.</s> [Reimplemented in RC5](https://github.com/danfickle/openhtmltopdf/issues/30)
	+ <s>[PDF text justification](https://github.com/danfickle/openhtmltopdf/issues/3)</s> [Reimplemented in RC5](https://github.com/danfickle/openhtmltopdf/pull/33) 

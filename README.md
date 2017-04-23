[![Build Status](https://api.travis-ci.org/danfickle/openhtmltopdf.svg?branch=open-dev-v1)](https://travis-ci.org/danfickle/openhtmltopdf)

OPEN HTML TO PDF
---------

OVERVIEW
========
Open HTML to PDF is a pure-Java library for rendering arbitrary well-formed XML/XHTML (and even HTML5)
using CSS 2.1 for layout and formatting, outputting to PDF or images.

GETTING STARTED
========
+ [Showcase Document - PDF](https://openhtmltopdf.com/showcase.pdf)
+ [Integration guide](docs/integration-guide.md) - get maven artifacts and code to get started.
+ [RC7 Online Sandbox](https://sandbox.openhtmltopdf.com/) - Please do not abuse.

LICENSE
========
Open HTML to PDF is distributed under the LGPL.  Open HTML to PDF itself is licensed 
under the GNU Lesser General Public License, version 2.1 or later, available at
http://www.gnu.org/copyleft/lesser.html. You can use Open HTML to PDF in any
way and for any purpose you want as long as you respect the terms of the 
license. A copy of the LGPL license is included as license-lgpl-2.1.txt or license-lgpl-3.txt
in our distributions and in our source tree.

Open HTML to PDF uses a couple of FOSS packages to get the job done. A list
of these, along with the license they each have, is listed in the 
LICENSE file in our distribution.   

CREDITS
========
Open HTML to PDF is based on [Flying-saucer](https://github.com/flyingsaucerproject/flyingsaucer). Credit goes to the contributors of that project. Code will also be used from [neoFlyingSaucer](https://github.com/danfickle/neoflyingsaucer)

FAQ
===
+ OPEN HTML TO PDF is tested with OpenJDK 7 and Oracle JDK 8.
+ No, you can not use it on Android or Google App Engine.
+ Flowing columns are not implemented.
+ No, it's not a web browser.

TEST CASES
========
Test cases, failing or working are welcome, please place them
in ````/openhtmltopdf-examples/src/main/resources/testcases/````
and run them
from ````/openhtmltopdf-examples/src/main/java/com/openhtmltopdf/testcases/TestcaseRunner.java````.

CHANGELOG
========

head - 0.0.1-RC11-SNAPSHOT
========
+ [FIX: Allow user to disable logging](https://github.com/danfickle/openhtmltopdf/pull/90) Thanks @GrammyTraore
+ [Handle TrueType font collections added in builder](https://github.com/danfickle/openhtmltopdf/pull/89) Thanks @rototor
+ [Implement custom object drawer for Java2D output](https://github.com/danfickle/openhtmltopdf/pull/87) Thanks @rototor
+ [Upgrade PDFBox library to 2.05](https://github.com/danfickle/openhtmltopdf/pull/86) Thanks @rototor, PDFBox team


0.0.1-RC10
========
+ [Support for inline SVG images](https://github.com/danfickle/openhtmltopdf/issues/23) Thanks @rototor
+ [Support for outputting paged or continuous images](https://github.com/danfickle/openhtmltopdf/issues/73#issuecomment-291070264) Thanks @rototor


0.0.1-RC9
========
+ [Don't output acroform for formless document](https://github.com/danfickle/openhtmltopdf/issues/52) Thanks @aleksandr-m
+ [Upgraded to PDFBox 2.0.4](https://github.com/danfickle/openhtmltopdf/issues/59) Thanks PDFBox team
+ [Fixed memory leak - properly - in image processing on some JREs](https://github.com/danfickle/openhtmltopdf/issues/51) Thanks @skjardenCode and @MartyMcMartface

0.0.1-RC8
========
+ [Initial support for CSS transform property](https://github.com/danfickle/openhtmltopdf/issues/38) Thanks @rototor
+ [Add support for max-width and max-height on img elements](https://github.com/danfickle/openhtmltopdf/pull/48) Thanks @achuinard

0.0.1-RC7
========
+ SECURITY ISSUE: [Prevent XXE Attacks](https://github.com/danfickle/openhtmltopdf/issues/44) Thanks @lillesand
+ BREAKING CHANGE: [Support for dir attribute and bdi element](https://github.com/danfickle/openhtmltopdf/issues/9#issuecomment-257072765)
+ [Do not download fonts that are not actually used](https://github.com/danfickle/openhtmltopdf/issues/43)
+ [Fixed resolution of relative URLs in inline style declarations](https://github.com/danfickle/openhtmltopdf/issues/27)
+ [Added support for hidden controls and submit controls with values](https://github.com/danfickle/openhtmltopdf/issues/24)
+ [Corrected naming scheme for form controls](https://github.com/danfickle/openhtmltopdf/issues/42) Thanks @scoldwell

0.0.1-RC5
========
+ [Reimplemented text justification](https://github.com/danfickle/openhtmltopdf/pull/33) Thanks @hiddendog 
+ [Fixed bug in table borders](https://github.com/danfickle/openhtmltopdf/pull/34) Thanks @rototor
+ [Added support for -fs-page-break-min-height CSS property](https://github.com/danfickle/openhtmltopdf/pull/31) Thanks @rototor
+ [Added support for -fs-table-paginate-repeated-visible CSS property](https://github.com/danfickle/openhtmltopdf/pull/32) Thanks @rototor
+ BREAKING CHANGE: Removed font subset method in builder, replaced with property in font face rule. Example: ````-fs-font-subset: complete-font;````
+ [Added support for text, password, textarea, submit, reset, checkbox, radio and select - multiple and single - controls](https://github.com/danfickle/openhtmltopdf/issues/24)
+ BREAKING CHANGE: Changed bi-directional method names in builder to be more consistent.
+ [Add method to builder to specify custom text transformers](https://github.com/danfickle/openhtmltopdf/issues/28)
+ [Add method to builder to specify a custom line breaker](https://github.com/danfickle/openhtmltopdf/issues/25) Thanks @Magotchi

0.0.1-RC4
========
+ Add method to builder to specify replacement text if no specified font can render a character.
+ [BREAKING CHANGE: Reworked URI resolver, changed FSUriResolver interface and made sure it is used everywhere](https://github.com/danfickle/openhtmltopdf/issues/27) - See example in integration guide.
+ Fixed issue where different size pages in the same document were not being recognized.
+ Add method to builder to specify default page size. Example: ````builder.useDefaultPageSize(PdfRendererBuilder.PAGE_SIZE_LETTER_WIDTH, PdfRendererBuilder.PAGE_SIZE_LETTER_HEIGHT, PdfRendererBuilder.PAGE_SIZE_LETTER_UNITS);````
+ BREAKING CHANGE: If no page size is specified in builder or CSS use A4, rather than locale dependent. See above.
+ [Silently discard control characters, etc at the rendering stage](https://github.com/danfickle/openhtmltopdf/issues/21#issuecomment-227850449) Thanks @scoldwell
+ [Fixed incorrect spacing when characters are replaced](https://github.com/danfickle/openhtmltopdf/issues/26) Thanks @scoldwell
 
0.0.1-RC3
========
+ [Experimental and unstable SVG support - early prototype](https://github.com/danfickle/openhtmltopdf/issues/23)
+ [Replaced non-breaking spaces - and other unusual spaces - with normal space if font does not support them](https://github.com/danfickle/openhtmltopdf/issues/21) Thanks @rototor
+ [Added a method for adding a PDF font using an input stream](https://github.com/danfickle/openhtmltopdf/issues/20) Thanks @aleksandr-m
+ [Added support for plugging in an external URI resolver](https://github.com/danfickle/openhtmltopdf/issues/18)
+ [Added support for plugging in an external cache](https://github.com/danfickle/openhtmltopdf/issues/18)
+ [Added support for font fallback for Java2D](https://github.com/danfickle/openhtmltopdf/issues/10) Thanks @willamette
+ [Fixed crash issue when document contained CDATA sections](https://github.com/danfickle/openhtmltopdf/issues/16) Thanks @hiddendog

0.0.1-RC2
========
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

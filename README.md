[![Build Status](https://api.travis-ci.org/danfickle/openhtmltopdf.svg?branch=open-dev-v1)](https://travis-ci.org/danfickle/openhtmltopdf)

# OPEN HTML TO PDF

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
+ [Integration guide](https://github.com/danfickle/openhtmltopdf/wiki/Integration-Guide) - get maven artifacts and code to get started.
+ [Documentation wiki](https://github.com/danfickle/openhtmltopdf/wiki)
+ [Template Author Guide - PDF - DEPRECATED - Prefer wiki](https://openhtmltopdf.com/template-guide.pdf) - Moving info to wiki
+ [RC18 Online Sandbox](https://sandbox.openhtmltopdf.com/) - With more samples!
+ [Sample Project - Pretty Resume Generator](https://github.com/danfickle/pretty-resume)

## DIFFERENCES WITH FLYING SAUCER
+ Uses the well-maintained and open-source (LGPL compatible) PDFBOX as PDF library, rather than iText.
+ Proper support for generating accessible PDFs (Section 508, PDF/UA, WCAG 2.0).
+ Proper support for generating PDF/A standards compliant PDFs.
+ New, faster renderer means this project can be several times faster for very large documents.
+ Better support for CSS3 transforms.
+ Automatic visual regression testing of PDFs, with many end-to-end tests.
+ Ability to insert pages for cut-off content.
+ Built-in plugins for SVG and MathML.
+ Font fallback support.
+ Limited support for RTL and bi-directional documents.
+ On the negative side, no support for OpenType fonts.
+ Much more. See changelog below.

## LICENSE
Open HTML to PDF is distributed under the LGPL.  Open HTML to PDF itself is licensed 
under the GNU Lesser General Public License, version 2.1 or later, available at
http://www.gnu.org/copyleft/lesser.html. You can use Open HTML to PDF in any
way and for any purpose you want as long as you respect the terms of the 
license. A copy of the LGPL license is included as license-lgpl-2.1.txt or license-lgpl-3.txt
in our distributions and in our source tree.

An exception to this is the pdf-a testing module, which is licensed under the GPL. This module is not distributed to Maven Central
and is for testing only.

Open HTML to PDF uses a couple of FOSS packages to get the job done. A list
of these can be found in the [dependency graph](https://github.com/danfickle/openhtmltopdf/network/dependencies).

## CREDITS
Open HTML to PDF is based on [Flying-saucer](https://github.com/flyingsaucerproject/flyingsaucer). Credit goes to the contributors of that project. Code will also be used from [neoFlyingSaucer](https://github.com/danfickle/neoflyingsaucer)

## FAQ
+ OPEN HTML TO PDF is tested with OpenJDK 8 and 11. We are [not currently testing with Oracle JDKs](https://github.com/travis-ci/travis-ci/issues/10290). As of RC18, it requires at least Java 8 to run.
+ No, you can not use it on Android.
+ You should be able to use it on Google App Engine (Java 8 or greater environment). [Let us know your experience](https://github.com/danfickle/openhtmltopdf/issues/179).
+ <s>Flowing columns are not implemented.</s> Implemented in RC12.
+ No, it's not a web browser. Specifically, it does not run javascript or implement many modern standards such as flex and grid layout.

## TEST CASES
Test cases, failing or working are welcome, please place them
in ````/openhtmltopdf-examples/src/main/resources/testcases/````
and run them
from ````/openhtmltopdf-examples/src/main/java/com/openhtmltopdf/testcases/TestcaseRunner.java````.

## CHANGELOG

### head - 1.0.1-SNAPSHOT
+ [#413](https://github.com/danfickle/openhtmltopdf/pull/413) Handle form problems such as no name on input element without throwing a NPE. Thanks @syjer for PR and @mmatecki for reporting.
+ [#412](https://github.com/danfickle/openhtmltopdf/pull/412) Add HTML block level elements usch as `section` to default CSS. Thanks @syjer.
+ [#339](https://github.com/danfickle/openhtmltopdf/issues/339) Remove the JSoup to DOM converter module. Thanks @kewilson.
+ [0cd098](https://github.com/danfickle/openhtmltopdf/commit/0cd09893af364c184403497ed0a9cc4df8f3073a) Fix for letter-spacing support on last line of block with trailing space. Also performance improvements and refactoring. By @danfickle.
+ [#410](https://github.com/danfickle/openhtmltopdf/pull/410) Fix for wrong bold setting on list item counters. Thanks @syjer for PR fix (and test!) and @acieplinski for reporting.
+ [Wiki](https://github.com/danfickle/openhtmltopdf/wiki/Custom-CSS-properties#-fs-max-justification-inter-char-and--fs-max-justification-inter-word) Configurable text justification settings as part of a justification overhaul that also allows more space to be used inter-char when there are no spaces on the line. By @danfickle. Commits listed in #403.
+ [#403](https://github.com/danfickle/openhtmltopdf/issues/403) Soft hyphen support. Soft hyphens are now replaced with hard hyphens when used as line ending character. Thanks @sbrunecker.
+ [#408](https://github.com/danfickle/openhtmltopdf/issues/408) Fix for bookmarks not working with HTML5 parsers such as JSoup. Thanks @syjer for investigating and fixing and @Milchreis for reporting.
+ [#404](https://github.com/danfickle/openhtmltopdf/issues/404) Upgrade Batik to 1.12 and xmlgraphics-common to 2.4 (both used in SVG module) to avoid CVE in one or both. Thanks @avoiculet.
+ [#396](https://github.com/danfickle/openhtmltopdf/issues/396) Much faster rendering of boxes using border-radius properties. Thanks @mndzielski.
+ [#400](https://github.com/danfickle/openhtmltopdf/pull/400) Support for `lang` and `title` attrbiutes and `abbr` tag for accessible PDFs. Thanks @Ignaciort91.
+ [#394](https://github.com/danfickle/openhtmltopdf/pull/394), [#395](https://github.com/danfickle/openhtmltopdf/pull/395) Upgrade PDFBOX to 2.0.17 and pdfbox-graphics2d to 0.25. Thanks @cristan, @rototor.
+ [#384](https://github.com/danfickle/openhtmltopdf/pull/384) Allow user to provide PDFont supplier. Thanks @DSW-PS.
+ [#373](https://github.com/danfickle/openhtmltopdf/pull/373) Fix regression where both max-width and max-height are provided for images with certain aspect ratios. Thanks @rototor.
+ [#380](https://github.com/danfickle/openhtmltopdf/pull/380) Much better support for flowing columns including explicit column breaks, floating content, block level nested content. By @danfickle.


### 1.0.0 (2019-July-23)
+ [#372](https://github.com/danfickle/openhtmltopdf/pull/372) Much improved sizing support for `img`, `svg` and `math` elements.
+ [#344](https://github.com/danfickle/openhtmltopdf/issues/344) Use PDFs in `img` tag: `<img src="some.pdf" page="1" alt="Some alt text" />`.

### 0.0.1-RC21 (2019-June-29)
+ [#361](https://github.com/danfickle/openhtmltopdf/issues/361) The SVG renderer now uses Batik in a more secure mode (no scripts, no external resource requests) by default. If you need the old behavior that allowed external resource requests and possibly scripts, please see the new BatikSVGDrawer constructor (only for trusted SVGs). Thanks @krabbenpuler.
+ [#363](https://github.com/danfickle/openhtmltopdf/pull/363) Upgrade PDFBOX to 2.0.16. Thanks @rototor.
+ [#353](https://github.com/danfickle/openhtmltopdf/issues/353) Better error handling around SVGs linked from `img` tag. Thanks @ieugen.
+ [#342](https://github.com/danfickle/openhtmltopdf/issues/342) Fixed text-justification/letter-spacing when fallback fonts are in use. Thanks @daliuss.
+ [#351](https://github.com/danfickle/openhtmltopdf/issues/351) Improved text-justification by removing spaces at ends of lines. Thanks @halcsi.


### 0.0.1-RC20 (2019-April-26)
**IMPORTANT:** This release was brought forward due to a CVE in PDFBOX. While not directly affecting this project (it affects parsing of untrusted PDFs), it is better not to have a vulnerable library on your classpath.
+ [#349](https://github.com/danfickle/openhtmltopdf/issues/349) Upgrade PDF-BOX to 2.0.15 to avoid CVE in 2.0.14. Thanks @BryceMehring.
+ [#347](https://github.com/danfickle/openhtmltopdf/issues/347) Add document language and title preference for PDF/A documents to satisfy Acrobat Pro validator. Thanks @mattstjean.
+ [#339](https://github.com/danfickle/openhtmltopdf/issues/339) Mark Jsoup DOM converter module as deprecated (for removal). Please see integration guide for replacement. This module may also pull in an insecure version of Guava so please migrate now.

### 0.0.1-RC19 (2019-March-18)
+ [#336](https://github.com/danfickle/openhtmltopdf/issues/336) Fix for broken image links causing an NPE. Thanks @svenfrauen.
+ [#334](https://github.com/danfickle/openhtmltopdf/pull/334) Allow the user to supply `PDPage` objects via page supplier. Thanks @DSW-PS.


### 0.0.1-RC18 (2019-March-10)
+ Please start using the fast renderer (`builder.useFastMode()`) as the old renderer will be removed in a future version.
+ [#180](https://github.com/danfickle/openhtmltopdf/issues/180) Fast renderer is finally ready for production. The fast renderer comes with:
  + Nearly 150 automated end-to-end regression tests. This is about 150 more than the old renderer.
  + Improved performance. This renderer scales linearly with the number of pages, compared to the old renderer which scaled with the page count squared.
  + Far better support for transforms, including nested transforms, multiple transforms and transforms interacting with hidden overflow, etc.
  + Better support for hidden overflow, with boxes now not escaping except with accordance to the standard.
  + Support for inserted cut off overflow pages. See [Cut-off page support](https://github.com/danfickle/openhtmltopdf/wiki/Cut-off-page-support) on the wiki.
  + Link areas and their hash link targets now repect transforms.
  + Bookmark targets now respect transforms.
  + Improved page placement for boxes. Now respects overflow and tranform properties.
  + Greater understanding which should make fixes and feature improvements easier.
+ Visual testing API is now available to use in the PDFBOX module. Please see [testing your PDF](https://github.com/danfickle/openhtmltopdf/wiki/Testing-Your-PDF-Document-Output) on the wiki. Thanks @red6.
+ [#333](https://github.com/danfickle/openhtmltopdf/pull/333) Upgraded PDFBox to 2.0.14 and PDFBox-Graphics2D to 0.21.
+ [#315](https://github.com/danfickle/openhtmltopdf/pull/315), [#79](https://github.com/danfickle/openhtmltopdf/issues/79) Accessible and tagged PDF support. See [PDF Accessibility (PDF UA, WCAG, Section 508) Support](https://github.com/danfickle/openhtmltopdf/wiki/PDF-Accessibility-(PDF-UA,-WCAG,-Section-508)-Support) on the wiki.
+ [#326](https://github.com/danfickle/openhtmltopdf/issues/326) Proper support for PDF/A standards with automatic regression testing. See [PDF/A Standards Compliance](https://github.com/danfickle/openhtmltopdf/wiki/PDF-A-Standards-Compliance) on the wiki.
+ [#328](https://github.com/danfickle/openhtmltopdf/issues/328) SVG with `page` rule was crashing in certain circumstances.
+ [#324](https://github.com/danfickle/openhtmltopdf/issues/324) Better logging with invalid or missing fonts.
+ [#320](https://github.com/danfickle/openhtmltopdf/pull/320) NPE prevention in case of incorrect font configuration.
+ [a145329](https://github.com/danfickle/openhtmltopdf/commit/a145329aa03bab62725a883b3a5c05ffd49996c8) Were using incorrect font-metrics in certain situations.
+ [#303](https://github.com/danfickle/openhtmltopdf/issues/303) Fixed: Table borders are partly transparent.
+ [#297](https://github.com/danfickle/openhtmltopdf/issues/297) Fixed: Border not printed with "overflow: hidden".
+ [#304](https://github.com/danfickle/openhtmltopdf/pull/304) Fix warnings for icon font without space inside PDF/A, add tests.
+ [#301](https://github.com/danfickle/openhtmltopdf/pull/301) Make loading resources from classpath work when openhtmltopdf is a named module.
+ [#232](https://github.com/danfickle/openhtmltopdf/issues/232) Were using JRE internal APIs.
+ [#289](https://github.com/danfickle/openhtmltopdf/issues/289) System.out.println("Getting image") in NaiveUserAgent.

Thanks to these people for pull-requests:
+ @rototor
+ @brundipub
+ @zimmi
+ @dnguyenminh

Finally, a big thanks to all issue reporters and extra thanks to those who help out in issues.

### OLDER RELEASES

[View CHANGELOG.md](CHANGELOG.md).

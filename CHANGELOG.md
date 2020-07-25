## CHANGELOG

### head - 1.0.5-SNAPSHOT
+ See commit log.


## 1.0.4 (2020-July-25)
+ [b88538](https://github.com/danfickle/openhtmltopdf/commit/b8853841b44d1e689d9bb3510a36633485fe21ab) Fix for endless loop when using `word-wrap: break-word`. Thanks for reporting, testing and investigating @swarl. Thanks for tests and debugging  @rototor and @syjer.
+ [#492](https://github.com/danfickle/openhtmltopdf/pull/492) Lots of testing of the line-breaking algorithm to avoid future endless loops. By @danfickle.
+ [#515](https://github.com/danfickle/openhtmltopdf/pull/515) Pass document CSS styles applying to SVG element to SVG implementation. Thanks for requesting and contributing @amckain92.
+ [#514](https://github.com/danfickle/openhtmltopdf/issues/514) FIX: Correctly position boxes when justifying rtl lines. Thanks for reporting and testing @lzhy1101.
+ [#512](https://github.com/danfickle/openhtmltopdf/pull/512) [#507](https://github.com/danfickle/openhtmltopdf/pull/507) [#502](https://github.com/danfickle/openhtmltopdf/pull/502) Cleanup code including deleting unused code, generics, etc. Thanks for PRs @syjer.
+ [#489](https://github.com/danfickle/openhtmltopdf/pull/489) Extensive overhaul of logging including per run diagnostic consumer. Huge thanks @syjer, a lot of work in this PR. See [logging page on wiki](https://github.com/danfickle/openhtmltopdf/wiki/Logging) for more info.
+ [#501](https://github.com/danfickle/openhtmltopdf/pull/501) Upgrade PDFBOX to 2.0.20 and PDFBox-Graphics2D to 0.26. Thanks for PR @rototor.
+ [#490](https://github.com/danfickle/openhtmltopdf/pull/490) Fix for NPE when decoding image data url fails. Thanks for PR @syjer and reporting @AlexisCothenet.
+ [#516](https://github.com/danfickle/openhtmltopdf/pull/516) Add OSGI bundle metadata to MANIFEST.MFs. Thanks for requesting and investigating @zspitzer.


### 1.0.3 (2020-May-25)
+ **IMPORTANT**: This release contains fixes for two bugs that may result in endless loops/denial of service when using `word-wrap: break-word`. If you are using this feature, please upgrade promptly.
+ [#483](https://github.com/danfickle/openhtmltopdf/pull/483) Fix for endless loop bug with `word-wrap: break-word` and soft hyphens. Thanks @rototor for PR, @syjer for analysis and @swarl for reporting.
+ [#466](https://github.com/danfickle/openhtmltopdf/issues/466) Fix for endless loop bug with `word-wrap: break-word` and zero width boxes. Thanks @syjer for analysis and @AlexisCothenet for reporting.
+ [#486](https://github.com/danfickle/openhtmltopdf/pull/486) SVG plugin can now provide a list of allowed protocols for external resources and any configured uri resolver/stream handlers will be used. Thanks @syjer for PR and @ieugen for reporting.
+ [#480](https://github.com/danfickle/openhtmltopdf/pull/480) Fix for link shapes being returned from custom object drawers. Thanks @rototor for PR and @hbergmey for reporting.
+ [#485](https://github.com/danfickle/openhtmltopdf/pull/485) Implement support for SVG data uris. Thanks @syjer for PR and @adrianrodfer for reporting.
+ [#470](https://github.com/danfickle/openhtmltopdf/pull/470) Allow `mailto:` links or any other valid link. Thanks @syjer for PR and @mndzielski for reporting.
+ [#464](https://github.com/danfickle/openhtmltopdf/issues/464) Honor the `direction` CSS property. Thanks @AnanasPizza for reporting.
+ [#460](https://github.com/danfickle/openhtmltopdf/pull/460) Change thrown exception class to more specific `IOException`. Thanks for PR @leonorader.
+ [#459](https://github.com/danfickle/openhtmltopdf/issues/459) Implement the `rem` CSS unit. Thanks to @leonorader for reporting.
+ [#211](https://github.com/danfickle/openhtmltopdf/issues/211) Images can now be used in the CSS `content` property. Thanks for requesting @Kuhlware.
+ [#445](https://github.com/danfickle/openhtmltopdf/issues/445) Fix for not picking up attribute values in Jsoup converted documents. Thanks for reporting @testinfected.
+ [#450](https://github.com/danfickle/openhtmltopdf/pull/450) Java2D output only: Ability to add fonts via code. Also environment fonts will no longer be used by default. To use environment fonts: `builder.useEnvironmentFonts(true)`.


### 1.0.2 (2020-February-25)
+ [SECURITY](https://github.com/danfickle/openhtmltopdf/commit/82bdedaf341ef3368dcebbb700a60ef870ab89b5) Removed Log4J 1.x adaptor as it had CVE-2019-17571 with no updated version available.
+ [#448](https://github.com/danfickle/openhtmltopdf/pull/448) Implement `linear-gradient` support for `background-image` property. By @danfickle. Requested by @rja907.
+ [#429](https://github.com/danfickle/openhtmltopdf/issues/429) Major overhaul of `word-wrap: break-word`. Now a word will not be broken unless it is too big for a line by itself. By @danfickle. Thanks for reporting and testing @mndzielski.
+ [#433](https://github.com/danfickle/openhtmltopdf/issues/433) Do not justify lines ending with `<br/>` tag. Thanks for reporting @fcorneli.
+ [#440](https://github.com/danfickle/openhtmltopdf/issues/440) Remove trailing white space for right aligned text to avoid jagged appearance. Thanks for reporting @AnanasPizza.
+ [#446](https://github.com/danfickle/openhtmltopdf/issues/446) Look for lang attribute on ancestor elements when using `lang()` selector. Thanks for reporting and tracking down the bug @fungc.
+ [#430](https://github.com/danfickle/openhtmltopdf/pull/430) Use relative path to license in source jars instead of absolute path. Thanks for reporting @gabro and fixing via PR @syjer.
+ [#417](https://github.com/danfickle/openhtmltopdf/issues/417) Keep aspect ratio of images with width/height properties as well as min/max width/height properties. Thanks for reporting and basis for fix @swarl.
+ [#423](https://github.com/danfickle/openhtmltopdf/pull/423) Allow multiple font sources to be specified with `format` tags. Only use `format(truetype)`. Thanks for requesting @MichaelZaleskovsky and basis for implementation @syjer.
+ [#415](https://github.com/danfickle/openhtmltopdf/pull/415) Avoid class cast exception if user tries to float table cell. Thanks for reporting @dmartineau99 and PR @syjer.
+ [#421](https://github.com/danfickle/openhtmltopdf/pull/421) Avoid NPE when justified text is mixed with unjustifiable content. Thanks for reporting @Megingjard and PR @syjer.
+ Updated PDFBOX 2.0.17 to 2.0.19.


### 1.0.1 (2019-November-18)
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


### 0.0.1-RC17
+ [#284](https://github.com/danfickle/openhtmltopdf/pull/284) [#288](https://github.com/danfickle/openhtmltopdf/issues/288) IMPORTANT: This release was brought forward due to a CVE in Apache Batik used by the optional SVG module.
  While this project strongly advises not to use untrusted XML either in SVG or XHTML, you may be using Batik in another part of your project and therefore it is a good idea to update. Thanks a lot @ghenadiibatalski, @chubbard
+ [#286](https://github.com/danfickle/openhtmltopdf/issues/286) [#281](https://github.com/danfickle/openhtmltopdf/issues/281) Fix for text decorations/background incorrect coverage in justified text. Thanks @koritakoa, @allartammik
+ [#280](https://github.com/danfickle/openhtmltopdf/issues/280) This will be the last release compatible with Java 7, from now on Java 8 or above will be required. Thanks for everyone's thoughts on this.

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

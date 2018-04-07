[#-- @ftlvariable name="" type="com.openhtmltopdf.freemarker.FreeMarkerGenerator.FreemarkerRootObject" --]
<html>
<head>
	<title>OpenHtmlToPDF Feature Overview</title>
	<style type="text/css">
		@page {
			size: A4;
			margin: 2.5cm 1.8cm 2cm 1.9cm;
			@bottom-left {
				content: element(footer);
			}
			@top-left {
				content: element(header);
			}
			@top-left-corner {
				content: element(topleftcorner);
			}
			@left-middle {
				content: element(left);
			}
			@bottom-right {
				content: element(bottomright);
			}
		}

		#bottomright {
			position: running(bottomright);
			border-top: 1px solid black;
			text-align: right;
		}

		#topleftcorner {
			position: running(topleftcorner);
		}

		#header {
			position: running(header);
			border-bottom: 1px solid black;
		}

		#footer {
			position: running(footer);
			border-top: 1px solid black;
		}

		#left {
			position: running(left);
			width: 10cm;
		}

		.pomCode, .htmlCode {
			background-color: #eeeeee;
		}

		.pomCode {
			-fs-page-break-min-height: 4cm;
		}

		.htmlCodeInline {
			display: inline-block;
			margin: 0;
		}

		h4 {
			page-break-after: avoid;
		}

		#pagenum:before {
			content: counter(page);
		}

		#pagecount:before {
			content: counter(pages);
		}

		h1, h2, h3, h4 {
			-fs-page-break-min-height: 4cm;
		}

		/* does not seem to work (yet?) */
		#tocPlaceholder:after {
			content:element(toc);
			display:block;
			min-height:20px;
			min-width:20px;
			border:2px solid black;
		}
	</style>
</head>
<body>

<div id="header">
	OpenHtmlToPdf Feature Documentation
	<object type="pdf/background" pdfsrc="background.pdf" style="width:1px;height:1px"></object>
</div>
<div id="footer">
	File: ${.template_name}
</div>
<div id="left">
	<div style="transform: rotate(90deg); text-transform: uppercase">
		Feature Documentation
	</div>
</div>
<div id="topleftcorner">
</div>
<div id="bottomright">
	Page <span id="pagenum"></span> / <span id="pagecount"></span>
</div>

[#assign tableOfContentHTML = ""]
[#assign sectionAnchorCounter = 0]
[#macro hn level]
	[#assign sectionAnchorCounter = sectionAnchorCounter + 1]
	<a id="sa${sectionAnchorCounter?c}"></a>
	[#local content][#nested][/#local]
	<h${level}>${content}</h${level}>
	[#assign tableOfContentHTML]
	${tableOfContentHTML}
	<li>	<a href="#sa${sectionAnchorCounter?c}">${content}</a> </li>
	[/#assign]
[/#macro]
[#macro h1][@hn 1][#nested][/@hn][/#macro]
[#macro h2][@hn 2][#nested][/@hn][/#macro]
[#macro h3][@hn 3][#nested][/@hn][/#macro]
[#macro h4][@hn 4][#nested][/@hn][/#macro]

[#macro pomCode]
	[#local content][#nested][/#local]
<pre class="pomCode">${content?trim?html}
</pre>
[/#macro]
[#macro javaCode]
	[#local content][#nested][/#local]
<pre class="pomCode">${content?trim?html}
</pre>
[/#macro]

[#macro htmlCode]
	[#local content][#nested][/#local]
	[#local countNewLines = countChar(content,"\n")]
	[#local spaceNeeded = countNewLines * 0.5]
	[#local inlineClass = ""]
	[#if countNewLines == 0]
		[#local inlineClass = "htmlCodeInline"]
	[/#if]
<pre class="htmlCode ${inlineClass}" style="-fs-page-break-min-height:${spaceNeeded?c}cm">${content?trim?html}
</pre>
[/#macro]

[#macro htmlCodeAndExec][#compress]
	[#local content][#nested][/#local]
[/#compress]${content}
	[#local countNewLines = countChar(content,"\n")]
	[#local spaceNeeded = countNewLines * 0.5]
<pre class="htmlCode" style="-fs-page-break-min-height:${spaceNeeded?c}cm">${content?trim?html}
</pre>
[/#macro]

<div id="tocPlaceholder"></div>

[@h1]OpenHtmlToPdf Feature Documentation[/@h1]
<a name="start"></a>

This documentation tries to show the advanced features of OpenHtmlToPdf. To generate the documentation a
<a href="https://freemarker.apache.org">FreeMarker</a> template is used to finally generate the HTML to render with
OpenHtmlToPdf. FreeMarker allows to do conditionals and calculation within the template. This is not pure MVC but
very useful to generate a report HTML. In combination with the excellent FreeMarker support in the (expensive) IntelliJ
IDEA IDE this is a very productive environment to build reports.

Please lookup the source of this document if you want to know some tricks not explicit mentioned in this documentation.

[@h2]Pagebreak Tuning[/@h2]

In a perfect world [@htmlCode]style="page-break-inside: avoid"[/@htmlCode] would just work and all reports would look
beautiful. OpenHtmlToPdf tries its best to avoid a page break inside. But this is not always possible and also rather
complex. If you know or can calculate how much space a block will take you can use the special
[@htmlCode]-fs-page-break-min-height[/@htmlCode] CSS
property on the block. You can specify the minimum height needed on the page any not relativ CSS unit (e.g. in cm
but not in %). If that amount of space is not remaining on the page, a pagebreak happens before the block is drawn.

Example:
[@htmlCode]
<div style="-fs-page-break-min-height:5cm">
	My nice content, which should not break... But should
	also not be higher than 5cm.
</div>
[/@htmlCode]

[@h2]Pixelated Images[/@h2]

OpenHTMLToPdf supports the standard "image-rendering" CSS property:

[@htmlCodeAndExec]
This image is interpolated and blured:
	<img src="../images/go-home.png"
		 style="width:64px;height:64px;" />
And this one is not interpolated and crisp:
	<img src="../images/go-home.png"
		 style="width:64px;height:64px;image-rendering: pixelated" />
[/@htmlCodeAndExec]

[@h2]MathML &amp; LaTeX[/@h2]
To display math you can use MathML and latex. To do so you need the dependencies:

[@pomCode]
<dependency>
	<groupId>com.openhtmltopdf</groupId>
	<artifactId>openhtmltopdf-mathml-support</artifactId>
	<version>...</version>
</dependency>
<dependency>
	<groupId>com.openhtmltopdf</groupId>
	<artifactId>openhtmltopdf-latex-support</artifactId>
	<version>...</version>
</dependency>
[/@pomCode]

If you don't use the LaTeX feature you don't need to include the openhtmltopdf-latex-support.
You must activate the support in the Builder to use it:

[@javaCode]
builder.useMathMLDrawer(new MathMLDrawer());
builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
[/@javaCode]

The LaTeX support translates a LaTeX fragment using SnuggleTeX to HTML+MathML, which is then
rendered using the MathML support.

[@htmlCodeAndExec]
<latex>
	This is a small inline formular: $$a^2 + b^2 = c^2$$. You can use many LaTeX
	features and environments. The exact amount of supported features is depending on
	StruggleTex and JEuclid which are the backing libraries for the LaTeX and MathML support.

	$$\sum\limits_{i=1}^n i^2 = \frac{n(n+1)(2n+1)}{6}$$

	$\prod\limits_{i=1}^n x = x^n$
</latex>
[/@htmlCodeAndExec]

Here is some pure MathML:

[@htmlCodeAndExec]
<math xmlns="http://www.w3.org/1998/Math/MathML">
	<mrow>
		<mi>a</mi>
		<mo>x</mo>
		<mfenced open="(" close=")">
			<mrow>
				<mi>b</mi>
				<mo>+</mo>
				<mi>c</mi>
			</mrow>
		</mfenced>
	</mrow>
</math>

<br/>
If you are writing the document by hand it may be just simpler to use LaTeX:
<latex>$$a x (b+c)$$</latex>
[/@htmlCodeAndExec]

[@h2]Objects[/@h2]

OpenHtmlToPdf comes with some builtin objects, which you can use to quickly create diagrams, add background PDF images
and so on. To use them include the openhtmltopdf-objects dependency in your pom:

[@pomCode]
<dependency>
	<groupId>com.openhtmltopdf</groupId>
	<artifactId>openhtmltopdf-objects</artifactId>
	<version>...</version>
</dependency>
[/@pomCode]

[@h3]Merge Background PDF[/@h3]

You can add a watermark / background to your document. To do so you should place

[@htmlCode]
	<object type="pdf/background" pdfsrc="background.pdf" pdfpage="1" style="width:1px;height:1px"></object>
[/@htmlCode]

into the header or footer of the document. The document will be placed unscaled in the PDF origin, i.e. in the left
lower corner.
<ul>
	<li><b>pdfsrc</b>: URI of the PDFFile to use</li>
	<li><b>pdfpage</b>: Page to import from the PDF file.</li>
</ul>

[@h3]JFreeGraph[/@h3]

For simple charts you can use the builtin objects for JFreeGraph. Note: You must specify the dependency to JFreeMarker
in your
POM, because it is declared as a optional dependency on openhtmltopdf-objects.

[@pomCode]
<dependency>
	<groupId>org.jfree</groupId>
	<artifactId>jfreechart</artifactId>
	<version>1.5.0</version>
</dependency>
[/@pomCode]

If you specify a URL for a data point then the segment in the diagram used for that datapoint is a link to that URL.
Note: This only works in Acrobat Reader, all other PDF Viewer ignore this feature.

[@h4]The Pie Diagram[/@h4]
[@htmlCodeAndExec]
<object type="jfreechart/pie"
		style="width:400px;height:400px;-fs-page-break-min-height:400px"
		title="Fruit Pie Chart">
	<data name="Apple" value="23.2" url="https://www.google.de?q=apple-pie"/>
	<data name="Pear" value="43.2" url="https://www.google.de?q=pear-pie"/>
	<data name="Orange" value="53.2" url="#start"/>
</object>
[/@htmlCodeAndExec]

[@h4]The Bar Diagram[/@h4]
[@htmlCodeAndExec]
<object type="jfreechart/bar"
		style="width:400px;height:400px; -fs-page-break-min-height:400px"
		title="Fruit Bar Chart"
		categories-title="Category" series-title="Series">
	<data series="Value" category="Apple" value="23.2" url="#apple"/>
	<data series="Value" category="Pear" value="43.2" url="#pear"/>
	<data series="Value" category="Orange" value="33.2" url="#orange"/>
	<data series="Price/kg" category="Apple" value="2.2"/>
	<data series="Price/kg" category="Pear" value="4.2"/>
	<data series="Price/kg" category="Orange" value="5.2"/>
</object>
[/@htmlCodeAndExec]

<br style="page-break-after: always"/>

<div id="toc">
	<b>Table of Content</b>
	<ul>
	${tableOfContentHTML}
	</ul>
</div>

</body>
</html>
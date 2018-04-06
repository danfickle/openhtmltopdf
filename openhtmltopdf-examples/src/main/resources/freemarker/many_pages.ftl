[#-- @ftlvariable name="" type="com.openhtmltopdf.freemarker.FreeMarkerGenerator.FreemarkerRootObject" --]
<html>
<head>
	<title>Document with *many* pages</title>
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
			content: element(toc);
			display: block;
			min-height: 20px;
			min-width: 20px;
			border: 2px solid black;
		}

		.repeated_table {
			-fs-table-paginate: paginate;
			-fs-keep-with-inline: keep;
			border: 2px solid black;
			border-radius: 5px;
			width: 100%;
		}

		.repeated_table th {
			padding: 5px;
			background: black;
			color: white;
			border: 2px solid white;
			border-radius: 5px;
		}

		.repeated_table td {
			padding: 5px;
			border: 2px solid #999;
			border-radius: 6px;
		}

		.repeated_table tr:odd td {
			background: #eee;
		}
	</style>
</head>
<body>

<div id="header">
	Many Pages Test
</div>
<div id="footer">
	File: ${.template_name}
</div>
<div id="left">
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
	<li><a href="#sa${sectionAnchorCounter?c}">${content}</a></li>
	[/#assign]
[/#macro]
[#macro h1][@hn 1][#nested][/@hn][/#macro]
[#macro h2][@hn 2][#nested][/@hn][/#macro]
[#macro h3][@hn 3][#nested][/@hn][/#macro]
[#macro h4][@hn 4][#nested][/@hn][/#macro]


[#list 1..100 as page]
	[@h1]H1 on Page ${page}[/@h1]
Some nice text, just to fill ${page?c} with something...

	[@h2]H2.1 on Page ${page}[/@h2]
Some nice text, just to fill ${page?c} with something...
	[@h2]H2.2 on Page ${page}[/@h2]
Some nice text, just to fill ${page?c} with something...
	[@h3]H3.1 on Page ${page}[/@h3]
Some nice text, just to fill ${page?c} with something...

<table class='repeated_table'>
	<thead>
	<tr>
		<th>Header1</th>
		<th>Header2</th>
	</tr>
	</thead>
	<tbody>
	[#list 1..50 as rownum]
	<tr>
		<td>Cell 1 ${rownum}</td>
		<td>Cell 2 ${rownum}</td>
	</tr>
	[/#list]
	</tbody>
</table>
	[@h2]H2.3 on Page ${page}[/@h2]
Some nice text, just to fill ${page?c} with something...
<br style="page-break-after: always"/>
[/#list]

<br style="page-break-after: always"/>

<div id="toc">
	<b>Table of Content</b>
	<ul>
	${tableOfContentHTML}
	</ul>
</div>

</body>
</html>
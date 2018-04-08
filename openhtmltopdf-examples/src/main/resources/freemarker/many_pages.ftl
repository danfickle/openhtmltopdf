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

		body {
			font-family: 'Times New Roman', Times, serif;
			font-size: 14px;
			margin: 0;
		}

		.cH3 {
			width: 14%;
		}

		/*
		 * Lista de registros
		 */
		.cMAsiento {
			width: 3%;
		}

		.cMFecha {
			width: 5%;
		}

		.cMDetalle {

		}

		.cMSaldo {
			width: 7%;
		}

		.cMAfectado {
			width: 5%;
		}

		.dinero {
			text-align: right;
		}

		.hdrLista {
			font-weight: normal;
			border-bottom: 1px solid;
		}

		.resRight {
			text-align: right;
		}

		.idAsig {
			font-size: 12px;
		}

		.nomAsig {
			vertical-align: top;
		}

		table tr td {
			vertical-align: top;
		}

		.MAIN tr td {
			vertical-align: top;
		}

		.MAIN {
			-fs-table-paginate: paginate;
			page-break-inside: auto;
			page-break-after: always;
			width: 100%;
			display: table-row-group;
			font-size: 9px;
		}

		.MAIN:last-of-type {
			page-break-after: avoid;
		}

		.HDR tr td {
			vertical-align: top;
		}

		.HDR {
			width: 100%;
			display: table-header-group;
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


[#list 1..10 as page]
	[@h1]H1 on Page ${page}[/@h1]
Some nice text, just to fill ${page?c} with something...
    <table class="HDR">
		<colgroup>
			<col class="cH1"/>
			<col class="cH2"/>
			<col class="cH3"/>
		</colgroup>
		<tr class="printhax">
			<td colspan="3">
				<br/>
				<br/>
				<br/>
			</td>
		</tr>
		<tr>
			<td rowspan="2" class="logobox"></td>
			<td class="mTitulo">
				REGISTRO DE LA EJECUCIÓN FINANCIERA DEL PRESUPUESTO DE GASTO HASTA EL
				30/04/2018
			</td>
			<td class="pagecount" rowspan="2">
				Fecha:
				06/04/2018
				<br/>
				Hora:
				12:31 PM
				<br/>
			</td>
		</tr>
		<tr>
			<td class="mSubtitulo">
				<br/>
				ORGANISMO: Alcaldía de Ningún Lugar
			</td>
		</tr>
	</table>
	[@h2]H2.1 on Page ${page}[/@h2]
Some nice text, just to fill ${page?c} with something...
	[@h2]H2.2 on Page ${page}[/@h2]
Some nice text, just to fill ${page?c} with something...
	<table class="MAIN">
		<colgroup>
			<col class="cMAsiento"/>
			<col class="cMFecha"/>
			<col class="cMDetalle"/>
			<col class="cMSaldo"/>
			<col class="cMSaldo"/>
			<col class="cMAfectado"/>
			<col class="cMSaldo"/>
			<col class="cMSaldo"/>
			<col class="cMAfectado"/>
			<col class="cMSaldo"/>
		</colgroup>
		<thead>
		<tr class="idAsig">

			<th colspan="4" class="nomAsig">
				UNIDAD EJECUTORA: MyonMyonMyonMyonMyonMyon
				<br/>
				PARTIDA: Fondo de victimaz del voraz Pyonta: victimas devoradas en el Templo Hakurei -
				Reimu Hakurei
			</th>

			<th colspan="6">
				<table class="codAsigBox">
					<colgroup>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
						<col class="cACodigo"/>
					</colgroup>
					<thead>
					<tr>
						<th colspan="9" class="hdrAsig"
							style="border-top: 1px solid; border-left: 1px solid; border-right: 1px solid">
							CÓDIGO PRESUPUESTARIO
						</th>
					</tr>
					<tr>
						<th class="hdrAsig" style="border-top: 1px solid;border-left: 1px solid">
							SECT
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							PROG
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							SUBP
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							PROY
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							ACTI
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							PART
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							GENE
						</th>
						<th class="hdrAsig" style="border-top: 1px solid">
							ESPE
						</th>
						<th class="hdrAsig" style="border-top: 1px solid; border-right: 1px solid">
							SUBE
						</th>
					</tr>
					</thead>
					<tbody>
					<tr>

						<td class="hdrAsig"
							style="border-top: 1px solid; border-bottom: 1px solid; border-left:1px solid">
							01
						</td>
						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							01
						</td>
						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							09
						</td>
						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							02
						</td>
						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							03
						</td>

						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							450
						</td>
						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							02
						</td>
						<td class="hdrAsig" style="border-top: 1px solid; border-bottom: 1px solid">
							02
						</td>
						<td class="hdrAsig"
							style="border-top: 1px solid; border-bottom: 1px solid; border-right: 1px solid">
							01
						</td>
					</tr>
					</tbody>
				</table>
			</th>
		</tr>
		<tr>
			<th colspan="10"></th>
		</tr>
		<tr>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				ASIENTO
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				FECHA
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				DETALLE
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				ACTUALIZADO
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				COMPROMISO
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				AFECTADO POR
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				POR COMPROMETER
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				CAUSADO
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				AFECTADO POR
			</th>
			<th class="hdrAsig" style="border-bottom: 1px solid">
				PAGADO
			</th>
		</tr>
		</thead>

		<tbody>
			[#list 1..10 as subRow]
			<tr>

				<td class="codigo">
					000000
				</td>

				<td class="codigo">01/01/2016
				</td>

				<td>
					Inicio de la ejecución
				</td>

				<td class="dinero">
					120.000,00
				</td>

				<td class="dinero">
					0,00
				</td>

				<td class="codigo">
				</td>

				<td class="dinero">
					120.000,00
				</td>

				<td class="dinero">
					0,00
				</td>

				<td class="codigo">
				</td>

				<td class="dinero">
					0,00
				</td>
			</tr>
			[/#list]
		<tr>

			<td class="codigo">
				001010
			</td>

			<td class="codigo">30/12/2016
			</td>

			<td>
				REVERSO:

				Orden de pago: 000357 - Fulanirigillo: Ranko!
			</td>

			<td class="dinero">
				(2.060.877,00)
			</td>

			<td class="dinero">
				0,00
			</td>

			<td class="codigo">
			</td>

			<td class="dinero">
				(2.060.877,00)
			</td>

			<td class="dinero">
				(350,00)
			</td>

			<td class="codigo">
			</td>

			<td class="dinero">
				0,00
			</td>
		</tr>
		<tr>

			<td class="codigo">
				001011
			</td>

			<td class="codigo">30/12/2016
			</td>

			<td>
				Orden de pago: 000358 - Fulanirigillo: Ranko!
			</td>

			<td class="dinero">
				(2.060.877,00)
			</td>

			<td class="dinero">
				0,00
			</td>

			<td class="codigo">
			</td>

			<td class="dinero">
				(2.060.877,00)
			</td>

			<td class="dinero">
				1.000,00
			</td>

			<td class="codigo">
			</td>

			<td class="dinero">
				0,00
			</td>
		</tr>
		<tr>
			<td colspan="3" class="total" style="border-top: 1px solid">
				TOTAL MES (Bs.):
			</td>

			<td colspan="2" class="total" style="border-top: 1px solid">
				100.000,00
			</td>

			<td colspan="3" class="total" style="border-top: 1px solid">
				2.155.000,00
			</td>

			<td colspan="2" class="total" style="border-top: 1px solid">
				2.332.100,00
			</td>
		</tr>
		<tr>
			<td colspan="10"></td>
		</tr>
		<tr>
			<td colspan="10"></td>
		</tr>

		<tr>
			<td colspan="3" class="total">
				TOTAL ACUMULADO PARTIDA (Bs.):
			</td>

			<td colspan="2" class="total">
				2.481.432,00
			</td>

			<td colspan="3" class="total">
				2.402.430,00
			</td>

			<td colspan="2" class="total">
				2.332.100,00
			</td>
		</tr>
		</tbody>
	</table>

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

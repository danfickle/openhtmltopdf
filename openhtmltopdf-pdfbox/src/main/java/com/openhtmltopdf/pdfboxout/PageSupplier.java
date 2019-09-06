package com.openhtmltopdf.pdfboxout;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * The PageSupplier is called whenever a (new) page (or shadow-page) is needed.<br>
 * With this you can control page-creation (or maybe you want to supply already existing pages).<p>
 * Quick note on shadow-pages (<b>optional feature</b>):<br>
 * Shadow-pages would contain overflow content which would otherwise be silently discared, 
 * see the <a href="https://github.com/danfickle/openhtmltopdf/wiki/Cut-off-page-support">OpenHtmlToPdf wiki</a>.
 */
@FunctionalInterface
public interface PageSupplier {
	
	/**
	 * Called whenever a page or shadow-page is needed.
	 * 
	 * @param doc 
	 *          {@link PDDocument} the page belongs to
	 * @param pageWidth 
	 *          Width of page in PDF points (1/72 inch)
	 * @param pageHeight 
	 *          Height of page in PDF points
	 * @param pageNumber 
	 *          Number of the layout page - this may differ from the PDF document page if there are previous shadow pages.
	 * @param shadowPageNumber 
	 *          Number of the shadow-page or -1 if on a main page.
	 * @return {@link PDPage}
	 */
	PDPage requestPage(PDDocument doc, float pageWidth, float pageHeight, int pageNumber, int shadowPageNumber);

}

package com.openhtmltopdf.testcases;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import org.junit.Test;

public class IconFontTest {

    /**
     * Icon fonts sometimes contain no space character. They should still be
     * usable without warnings.
     */
    @Test
    public void testFontWithoutSpace() throws Exception {
        TestcaseRunner.runTestWithoutOutput("icon-font");
    }

    /**
     * Should also work for PDF/A.
     */
    @Test
    public void testFontWithoutSpacePdfA() throws Exception {
        TestcaseRunner.runTestWithoutOutput("icon-font", PdfAConformance.PDFA_2_U);
    }

}

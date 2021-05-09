package com.openhtmltopdf.testcases;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PrintingRunner.class)
public class IconFontTest {
    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

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

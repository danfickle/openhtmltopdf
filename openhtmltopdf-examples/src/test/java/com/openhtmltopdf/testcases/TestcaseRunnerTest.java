package com.openhtmltopdf.testcases;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualregressiontests.VisualRegressionTest;
import com.openhtmltopdf.visualtest.TestSupport;

import java.io.File;
import java.io.IOException;

import static com.openhtmltopdf.testcases.TestcaseRunner.runTestCase;
import static org.junit.Assert.assertEquals;


/**
 * These are smoke tests, they just test that we don't crash and generate a 
 * valid PDF/PNG file. They do not test correct output.
 *
 * New tests should be placed in {@link VisualRegressionTest} instead.
 *
 */
@RunWith(PrintingRunner.class)
public class TestcaseRunnerTest {
    private static final File targetDirectory = new File("target/test/testcase-runner-tests/");

    @BeforeClass
    public static void configure() {
        targetDirectory.mkdirs();
        System.setProperty("OUT_DIRECTORY", targetDirectory.getAbsolutePath());
        System.out.println("Writing testcases to: " + targetDirectory.getAbsolutePath());

        TestSupport.quietLogs();
    }

    private void confirmPages(String filename, int expectedPages) throws IOException {
        File pdf = new File(targetDirectory, filename + ".pdf");

        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(expectedPages, doc.getNumberOfPages());
        }
    }

    @Test
    public void testSoftHyphenOom() throws IOException {
        String test = "soft-hypen-oom";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testRepeatedTableSample() throws IOException {
        /*
         * Note: The RepeatedTableSample optionally requires the font file
         * NotoSans-Regular.ttf to be placed in the resources directory.
         * 
         * This sample demonstrates the failing repeated table header on each page.
         */
        String test = "RepeatedTableSample";
        runTestCase(test);
        confirmPages(test, 2);
    }

    @Test
    public void testFSPageBreakMinHeight() throws IOException {
        /*
         * This sample demonstrates the -fs-pagebreak-min-height css property
         */
        String test = "FSPageBreakMinHeightSample";
        runTestCase(test);
        confirmPages(test, 6);
    }

    @Test
    public void testColor() throws IOException {
        String test = "color";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testTextDecoration() throws IOException {
        String test = "text-decoration";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testBackgroundColor() throws IOException {
        String test = "background-color";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testBavkgroundImage() throws IOException {
        String test = "background-image";
        runTestCase(test);
        confirmPages(test, 5);
    }

    @Test
    public void testInvalidUrlBackgroundImage() throws IOException {
        String test = "invalid-url-background-image";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testTextAlign() throws IOException {
        String test = "text-align";
        runTestCase(test);
        confirmPages(test, 2);
    }

    @Test
    public void testFontFamilyBuiltIn() throws IOException {
        String test = "font-family-built-in";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testFormControls() throws IOException {
        String test = "form-controls";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testSvgInline() throws IOException {
        String test = "svg-inline";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testSvgSizes() throws IOException {
        String test = "svg-sizes";
        runTestCase(test);
        confirmPages(test, 2);
    }

    @Test
    public void testMoonbase() throws IOException {
        /*
         * Graphics2D Texterror Case
         */
        String test = "moonbase";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testCustomObjects() throws IOException {
        /*
         * Custom Objects
         */
        String test = "custom-objects";
        runTestCase(test);
        confirmPages(test, 2);
    }

    @Test
    public void testMultiColumnLayout() throws IOException {
        /*
         * CSS3 multi-column layout
         */
        String test = "multi-column-layout";
        runTestCase(test);
        confirmPages(test, 7);
    }

    @Test
    public void testAbodeBorderStyleBugs() throws IOException {
        /*
         * Adobe Borderyle Problem
         */
        String test = "adobe-borderstyle-bugs";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testTransform() throws IOException {
        /*
         * CSS Transform Test
         */
        String test = "transform";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testSimpleRotate() throws IOException {
        String test = "simplerotate";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testQuoting() throws IOException {
        String test = "quoting";
        runTestCase(test);
        confirmPages(test, 1);
    }

    @Test
    public void testMathMl() throws IOException {
        String test = "math-ml";
        runTestCase(test);
        confirmPages(test, 2);
    }

    @Test
    public void testLatexSample() throws IOException {
        String test = "latex-sample";
        runTestCase(test);
        confirmPages(test, 3);
    }

    @Test
    public void testRepeatedTableTransform() throws IOException {
        /*
         * Broken rotate() on the second page
         */
        String test = "RepeatedTableTransformSample";
        runTestCase(test);
        confirmPages(test, 3);
    }

    @Test
    public void testMultipageTable() throws IOException {
        String test = "multipage-table";
        runTestCase(test);
        confirmPages(test, 4);
    }
    
}

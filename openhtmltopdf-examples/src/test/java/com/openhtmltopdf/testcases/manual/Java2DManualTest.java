package com.openhtmltopdf.testcases.manual;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.Java2DVisualTester;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.Java2DVisualTester.Java2DBuilderConfig;

/**
 * These tests need to be manually reviewed to confirm roughly correct.
 * However, even if not manually inspected, prove that we are not crashing for
 * Java2D output.
 */
@RunWith(PrintingRunner.class)
public class Java2DManualTest {
    private static Java2DVisualTester vtester;

    @BeforeClass
    public static void configure() throws IOException {
        File outputDirectory = new File("target/test/visual-tests/test-output/j2d");
        TestSupport.makeFontFiles();
        vtester = new Java2DVisualTester(
                     "/visualtest/j2d/html/",     /* Resource path. */
                     "/visualtest/j2d/expected/", /* Expected resource path */
                     "/visualtest/j2d/expected-single-page/", /* Single page expected */
                     outputDirectory
                     );
    }

    private void run(String resource, Java2DBuilderConfig config) throws IOException {
        vtester.runTest(resource, config);
    }

    private void run(String resource) throws IOException {
        run(resource, (b) -> {});
    }

    private void single(String resource, Java2DBuilderConfig config) throws IOException {
        vtester.runSinglePageTest(resource, config);
    }

    private void single(String resource) throws IOException {
        single(resource, (b) -> {});
    }

    @Test
    public void testBlocks() throws IOException {
        run("simple-blocks");
    }

    @Test
    public void testText() throws IOException {
        run("simple-text");
    }

    @Test
    public void testMarginsClippingTransforms() throws IOException {
        run("margins-clipping-transforms");
    }

    @Test
    public void testClipInsideTransform() throws IOException {
        run("clip-inside-transform");
    }

    @Test
    public void testLinearGradient() throws IOException {
        run("linear-gradient");
    }

    @Test
    public void testPositioned() throws IOException {
        run("positioned-elements");
    }

    @Test
    public void testImages() throws IOException {
        run("images", builder -> builder.useSVGDrawer(new BatikSVGDrawer()));
    }

    @Test
    public void testSizedRepeatImages() throws IOException {
        run("sized-repeat-images");
    }

    @Test
    public void testFootnotes() throws IOException {
        run("footnotes");
    }

    @Test
    public void testBlocksS() throws IOException {
        single("simple-blocks");
    }

    @Test
    public void testTextS() throws IOException {
        single("simple-text");
    }

    @Test
    public void testMarginsClippingTransformsS() throws IOException {
        single("margins-clipping-transforms");
    }

    @Test
    public void testClipInsideTransformS() throws IOException {
        single("clip-inside-transform");
    }

    @Test
    public void testLinearGradientS() throws IOException {
        single("linear-gradient");
    }

    @Test
    public void testPositionedS() throws IOException {
        single("positioned-elements");
    }

    @Test
    public void testImagesS() throws IOException {
        single("images", builder -> builder.useSVGDrawer(new BatikSVGDrawer()));
    }

    @Test
    public void testSizedRepeatImagesS() throws IOException {
        single("sized-repeat-images");
    }

    @Test
    public void testFootnotesS() throws IOException {
        single("footnotes");
    }

}

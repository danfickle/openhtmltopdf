package com.openhtmltopdf.visualregressiontests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.VisualTester;

@RunWith(PrintingRunner.class)
public class FootnoteVisualRegressionTest {
    private VisualTester vt;

    @BeforeClass
    public static void configureTests() throws IOException {
        TestSupport.quietLogs();
        TestSupport.makeFontFiles();
    }

    @Before
    public void configureTester() {
        File outputDirectory = new File("target/test/visual-tests/test-output/");

        outputDirectory.mkdirs();

        vt = new VisualTester(
                "/visualtest/html/footnote/",     /* Resource path. */
                "/visualtest/expected/footnote/", /* Expected resource path */
                outputDirectory
                );
    }

    /**
     * Tests that we support CSS footnotes.
     */
    @Test
    @Ignore
    public void testIssue364FootnotesBasicExample() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-basic"));
    }

    /**
     * Tests that a line of text can contain two multi-page footnotes.
     */
    @Test
    @Ignore
    public void testIssue364FootnotesMultiPage() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-multi-page"));
    }

    /**
     * Test what happens when a line of in-flow text and a line of footnotes
     * do not fit on a single page.
     */
    @Test
    @Ignore
    public void testIssue364FootnotesTooLarge() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-too-large"));
    }

    /**
     * Tests that we support display: block elements in the footnote area
     * and that blocks with page-break-inside: avoid do not intersect
     * with the footnote area.
     */
    @Test
    @Ignore
    public void testIssue364FootnotesBlocks() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-blocks"));
    }

    /**
     * Tests paginated table head/footer is placed in the right place in the
     * presence of footnotes.
     */
    @Test
    @Ignore
    public void testIssue364FootnotesPaginatedTable() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-paginated-table"));
    }

    /**
     * Tests that position: absolute and fixed content with bottom: 0 sits
     * above footnotes.
     */
    @Test
    @Ignore
    public void testIssue364FootnotesPositionedContent() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-positioned-content"));
    }

    /**
     * Tests that position: absolute and fixed inside a footnote (or
     * as a footnote element) does something sensible.
     * <ul>
     * <li>Fixed inside fn will escape footnote area and be positioned on page as normal fixed.</li>
     * <li>Absolute inside fn will be contained by footnote area.</li>
     * <li>Fixed as fn element will be positioned as normal fixed but will not have footnote mark.</li>
     * <li>Absolute as fn element will be contained by html layer and have footnote mark.</li>
     * </ul>
     */
    @Test
    @Ignore
    public void testIssue364PositionedInsideFootnotes() throws IOException {
        assertTrue(vt.runTest("issue-364-positioned-inside-footnotes"));
    }

    /**
     * Tests floats in footnotes, floated footnote calls and floats
     * interacting with footnotes.
     * <ul>
     * <li>Footnotes calls may be floated (typically right).</li>
     * <li>Footnotes act as float containers.</li>
     * <li>Floats work in footnotes.</li>
     * <li>Left floated content will appear left of the footnote marker.</li>
     * <li>Footnote area will clear floated content above.</li>
     * </ul>
     */
    @Test
    @Ignore
    public void testIssue364Floats() throws IOException {
        assertTrue(vt.runTest("issue-364-floats"));
    }

    /**
     * Tests images in footnotes, as footnotes and intersecting with footnotes.
     */
    @Test
    @Ignore
    public void testIssue364Images() throws IOException {
        assertTrue(vt.runTest("issue-364-images"));
    }

    /**
     * Tests using an image as the footnote call.
     */
    @Test
    public void testIssue364ImageCalls() throws IOException {
        assertTrue(vt.runTest("issue-364-image-calls"));
    }

    /**
     * Tests using an image as the footnote marker.
     */
    @Test
    public void testIssue364ImageMarkers() throws IOException {
        assertTrue(vt.runTest("issue-364-image-markers"));
    }
}

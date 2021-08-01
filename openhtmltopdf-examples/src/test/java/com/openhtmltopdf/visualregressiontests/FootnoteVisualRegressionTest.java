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

@Ignore
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
    public void testIssue364FootnotesBasicExample() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-basic"));
    }

    /**
     * Tests that a line of text can contain two multi-page footnotes.
     */
    @Test
    public void testIssue364FootnotesMultiPage() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-multi-page"));
    }

    /**
     * Test what happens when a line of in-flow text and a line of footnotes
     * do not fit on a single page.
     */
    @Test
    public void testIssue364FootnotesTooLarge() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-too-large"));
    }

    /**
     * Tests that we support display: block elements in the footnote area
     * and that blocks with page-break-inside: avoid do not intersect
     * with the footnote area.
     */
    @Test
    public void testIssue364FootnotesBlocks() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-blocks"));
    }

    /**
     * Tests paginated table head/footer is placed in the right place in the
     * presence of footnotes.
     */
    @Test
    public void testIssue364FootnotesPaginatedTable() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-paginated-table"));
    }

    /**
     * Tests that positon: absolute and fixed content with bottom: 0 sits
     * above footnotes.
     */
    @Test
    public void testIssue364FootnotesPositionedContent() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-positioned-content"));
    }

}

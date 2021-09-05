package com.openhtmltopdf.visualregressiontests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.util.LogMessageId;
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
     * Tests that position: absolute and fixed content with bottom: 0 sits
     * above footnotes.
     */
    @Test
    public void testIssue364FootnotesPositionedContent() throws IOException {
        assertTrue(vt.runTest("issue-364-footnotes-positioned-content"));
    }

    /**
     * Tests that position: absolute and fixed inside a footnote
     * does something sensible.
     * <ul>
     * <li>Fixed inside fn will escape footnote area and be positioned on page as normal fixed.</li>
     * <li>Absolute inside fn will be contained by footnote area.</li>
     * </ul>
     */
    @Test
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
    public void testIssue364Floats() throws IOException {
        assertTrue(vt.runTest("issue-364-floats"));
    }

    /**
     * Tests images in footnotes, as footnotes and intersecting with footnotes.
     */
    @Test
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

    /**
     * Tests non-paginated table content works inside footnote body
     * as well as footnotes being called from inside an in-flow table.
     * Also tests use of ::before and ::after in the footnote element.
     */
    @Test
    public void testIssue364NonPaginatedTable() throws IOException {
        assertTrue(vt.runTest("issue-364-non-paginated-table"));
    }

    /**
     * Tests that footnotes inside footnotes are treated as non-footnote content
     * and do not cause infinite loop, stack overflow, OOM, etc.
     */
    @Test
    public void testIssue364FootnoteInsideFootnote() throws IOException {
        TestSupport.withLog((log, builder) -> {
            assertTrue(vt.runTest("issue-364-footnote-inside-footnote", builder));
            assertThat(log, hasItem(LogMessageId.LogMessageId0Param.GENERAL_NO_FOOTNOTES_INSIDE_FOOTNOTES));
        });
    }

    /**
     * Tests that content incompatible with being a footnote will be
     * treated as normal content, even with float: bottom set.
     */
    @Test
    public void testIssue364InvalidStyle() throws IOException {
        TestSupport.withLog((log, builder) -> {
            assertTrue(vt.runTest("issue-364-invalid-style", builder));
            assertThat(log, hasItem(LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_INVALID));
        });
    }

    /**
     * Tests that inline-blocks are able to be used in footnotes and
     * intersecting with footnotes.
     */
    @Test
    public void testIssue364InlineBlocks() throws IOException {
        assertTrue(vt.runTest("issue-364-inline-blocks"));
    }

    /**
     * Tests transforms of the footnote area, footnote body, elements inside
     * footnote body and footnote pseudos.
     */
    @Test
    public void testIssue364Transforms() throws IOException {
        assertTrue(vt.runTest("issue-364-transforms"));
    }

    /**
     * + Footnotes can not be called from fixed position area.
     * + Footnotes can be called from relative or absolute positioned areas.
     * + Physical ordering of footnotes may differ from counter order as
     * positioned boxes are encounted in different order in BoxBuilder and
     * layout.
     */
    @Test
    public void testIssue364CalledFromPositioned() throws IOException {
        assertTrue(vt.runTest("issue-364-called-from-positioned"));
    }

    /**
     * Tests that footnotes inside page margins are treated as normal
     * content (and warning is logged).
     */
    @Test
    public void testIssue364FootnotesInsidePageMargins() throws IOException {
        TestSupport.withLog((log, builder) -> {
            assertTrue(vt.runTest("issue-364-page-margins", builder));
            assertThat(log, hasItem(LogMessageId.LogMessageId0Param.GENERAL_NO_FOOTNOTES_INSIDE_FOOTNOTES));
            assertThat(log, hasItem(LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_INVALID));
        });
    }

}

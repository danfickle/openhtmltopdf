package com.openhtmltopdf.nonvisualregressiontests;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport;
import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport.TestDocument;
import com.openhtmltopdf.test.generators.LinkTestCreator;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkArea;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.pdRectEquals;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkDestinationUri;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkDestinationTop;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkDestinationPageNo;

/**
 * To add tests to this class, please see the following method:<br/>
 * {@link LinkTestCreator#main(String[])}
 */
@RunWith(PrintingRunner.class)
public class LinkRegressionTest {
    public static final String RES_PATH = "/visualtest/links/";
    public static final String OUT_PATH = "target/test/visual-tests/test-output/";

    private final NonVisualTestSupport support = new NonVisualTestSupport(RES_PATH, OUT_PATH);

    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    /**
     * Tests links in repeating table headers/footers over multiple pages.
     */
    @Test
    public void testPr798MultipageTableLinkAreas() throws IOException {
        try (TestDocument doc = support.run("pr-798-multipage-table")) {
            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 82.88f, 121.24f, 32.70f, 13.84f));
            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 82.88f, 207.79f, 32.70f, 13.84f));
            assertThat(linkArea(doc.pd, 2, 0), pdRectEquals(2, 0, 82.88f, 207.79f, 32.70f, 13.84f));
            assertThat(linkArea(doc.pd, 3, 0), pdRectEquals(3, 0, 82.88f, 207.79f, 32.70f, 13.84f));

            assertThat(linkDestinationUri(doc.pd, 0, 0), equalTo("http://localhost"));
        }
    }

    /**
     * Tests that an inline link with multiple inline boxes generates one link annotation for each line.
     * ie. Multiple inline boxes are concatenated into one rect for the purposes of creating a link area.
     */
    @Test
    public void testLinkAreaMultipleBoxes() throws IOException {
        try (TestDocument doc = support.run("link-area-multiple-boxes")) {
            // One link annotation for each line.
            assertEquals(2, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 6.00f, 130.01f, 132.60f, 13.99f));
            assertThat(linkArea(doc.pd, 0, 1), pdRectEquals(0, 1, 6.00f, 113.29f, 106.58f, 13.99f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(144));
            assertThat(linkDestinationPageNo(doc.pd, 0, 1), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 1), equalTo(144));
        }
    }

    /**
     * Tests that a link area is created for each page (normal and overflow) that the link appears on.
     */
    @Test
    public void testLinkAreaMultiplePage() throws IOException {
        try (TestDocument doc = support.run("link-area-multiple-page")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(1).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(2).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(3).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 8.25f, 7.50f, 59.25f, 59.25f));
            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 7.50f, 7.50f, 45.75f, 59.25f));
            assertThat(linkArea(doc.pd, 2, 0), pdRectEquals(2, 0, 8.25f, 14.25f, 59.25f, 53.25f));
            assertThat(linkArea(doc.pd, 3, 0), pdRectEquals(3, 0, 7.50f, 14.25f, 45.75f, 53.25f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(67));
            assertThat(linkDestinationPageNo(doc.pd, 1, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 1, 0), equalTo(67));
            assertThat(linkDestinationPageNo(doc.pd, 2, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 2, 0), equalTo(67));
            assertThat(linkDestinationPageNo(doc.pd, 3, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 3, 0), equalTo(67));
        }
    }

    /**
     * Tests that an inline link over multiple lines generates at least one link annotation for each line.
     */
    @Test
    public void testLinkAreaMultipleLine() throws IOException {
        try (TestDocument doc = support.run("link-area-multiple-line")) {
            // One link annotation for each line.
            assertEquals(2, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 6.00f, 130.01f, 132.60f, 13.99f));
            assertThat(linkArea(doc.pd, 0, 1), pdRectEquals(0, 1, 6.00f, 116.02f, 95.14f, 13.99f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(144));
            assertThat(linkDestinationPageNo(doc.pd, 0, 1), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 1), equalTo(144));
        }
    }

    /**
     * Tests a link element inside a transformed element in the page content.
     */
    @Test
    public void testLinkAreaTransformNested() throws IOException {
        try (TestDocument doc = support.run("link-area-transform-nested")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 45.75f, 27.75f, 37.50f, 75.00f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(103));
        }
    }

    /**
     * Tests a link area inside a transformed element in the page margin.
     */
    @Test
    public void testLinkAreaPageMarginTransform() throws IOException {
        try (TestDocument doc = support.run("link-area-page-margin-transform")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(1).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 127.50f, 0.00f, 7.50f, 22.50f));
            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 127.50f, 0.00f, 7.50f, 22.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(46));
            assertThat(linkDestinationPageNo(doc.pd, 1, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 1, 0), equalTo(46));
        }
    }

    /**
     * Tests a link area in the page margin.
     */
    @Test
    public void testLinkAreaPageMargin() throws IOException {
        try (TestDocument doc = support.run("link-area-page-margin")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(1).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 127.50f, 7.50f, 22.50f, 7.50f));
            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 127.50f, 7.50f, 22.50f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(46));
            assertThat(linkDestinationPageNo(doc.pd, 1, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 1, 0), equalTo(46));
        }
    }

    /**
     * Tests that a link to an external url works correclty.
     */
    @Test
    public void testLinkExternalUrl() throws IOException {
        try (TestDocument doc = support.run("link-external-url")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 0.00f, 142.50f, 75.00f, 7.50f));

            assertThat(linkDestinationUri(doc.pd, 0, 0), equalTo("https://openhtmltopdf.com"));
        }
    }

    /**
     * Tests link area after overflow page is correctly placed.
     */
    @Test
    public void testLinkAreaAfterOverflowPage() throws IOException {
        try (TestDocument doc = support.run("link-area-after-overflow-page")) {
            assertEquals(0, doc.pd.getPage(0).getAnnotations().size());
            assertEquals(0, doc.pd.getPage(1).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(2).getAnnotations().size());

            assertThat(linkArea(doc.pd, 2, 0), pdRectEquals(2, 0, 7.50f, 60.00f, 60.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 2, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 2, 0), equalTo(67));
        }
    }

    /**
     * Tests link area on overflow page is correctly placed.
     */
    @Test
    public void testLinkAreaOverflowPage() throws IOException {
        try (TestDocument doc = support.run("link-area-overflow-page")) {
            assertEquals(0, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 23.25f, 59.25f, 30.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 1, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 1, 0), equalTo(67));
        }
    }

    /**
     * Tests that a simple block link successfully links to an element that is after an inserted overflow page.
     */
    @Test
    public void testLinkAfterOverflowTarget() throws IOException {
        try (TestDocument doc = support.run("link-after-overflow-target")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 7.50f, 60.00f, 60.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(2));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(67));
        }
    }

    /**
     * Tests that link annotation area is correctly rotated.
     */
    @Test
    public void testLinkAreaTransformRotate() throws IOException {
        try (TestDocument doc = support.run("link-area-transform-rotate")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 11.44f, 69.97f, 106.05f, 72.53f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(103));
        }
    }

    /**
     * Tests that link annotation area is correctly translated-y.
     */
    @Test
    public void testLinkAreaTransformTranslateY() throws IOException {
        try (TestDocument doc = support.run("link-area-transform-translatey")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 8.25f, 66.75f, 112.50f, 37.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(103));
        }
    }

    /**
     * Tests that a link can successfully target a destination comprised of an inline element. 
     */
    @Test
    public void testLinkInlineTarget() throws IOException {
        try (TestDocument doc = support.run("link-inline-target")) {
            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 0.00f, 67.50f, 75.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(75));
        }
    }

    /**
     * Tests that a simple block link successfully links to an element on an inserted overflow page.
     */
    @Test
    public void testLinkOnOverflowTarget() throws IOException {
        try (TestDocument doc = support.run("link-on-overflow-target")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 7.50f, 60.00f, 60.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(2));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(66));
        }
    }

    /**
     * Tests that a simple block link successfully links to a simple block target on second page. 
     */
    @Test
    public void testLinkSimpleBlock() throws IOException {
        try (TestDocument doc = support.run("link-simple-block")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 0.00f, 142.50f, 75.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(150));
        }
    }

    /**
     * Tests that a simple block link successfully links to an element that is transformed to top of third page.
     */
    @Test
    public void testLinkTransformTarget() throws IOException {
        try (TestDocument doc = support.run("link-transform-target")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 0.00f, 67.50f, 75.00f, 7.50f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(2));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(75));
        }
    }

    /**
     * Tests that ::footnote-call links are placed correctly and link
     * to the correct ::footnote-marker.
     */
    @Test
    public void testIssue364FootnoteCallLink() throws IOException {
        try (TestDocument doc = support.run("issue-364-footnote-call-link")) {
            assertEquals(1, doc.pd.getPage(0).getAnnotations().size());
            assertEquals(1, doc.pd.getPage(1).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 106.50f, 141.49f, 15.75f, 15.07f));
            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 103.50f, 158.85f, 15.75f, 15.07f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(51));
            assertThat(linkDestinationPageNo(doc.pd, 1, 0), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 1, 0), equalTo(81));
        }
    }

    /**
     * Tests using a link from in-flow content to an element inside a footnote.
     */
    @Test
    public void testIssue364LinkToFootnoteContent() throws IOException {
        try (TestDocument doc = support.run("issue-364-link-to-footnote-content")) {
            assertEquals(2, doc.pd.getPage(0).getAnnotations().size());
            assertEquals(2, doc.pd.getPage(1).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 133.31f, 149.48f, 42.56f, 16.76f));
            assertThat(linkArea(doc.pd, 0, 1), pdRectEquals(0, 1, 42.00f, 132.71f, 97.95f, 16.76f));
            assertThat(linkArea(doc.pd, 1, 0), pdRectEquals(1, 0, 171.60f, 155.48f, 0.00f, 16.76f));
            assertThat(linkArea(doc.pd, 1, 1), pdRectEquals(1, 1, 42.00f, 138.71f, 22.50f, 16.76f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(52));

            assertThat(linkDestinationPageNo(doc.pd, 0, 1), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 0, 1), equalTo(52));

            assertThat(linkDestinationPageNo(doc.pd, 1, 0), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 1, 0), equalTo(103));

            assertThat(linkDestinationPageNo(doc.pd, 1, 1), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 1, 1), equalTo(103));
        }
    }

    /**
     * Tests using a link from footnote content to in-flow content.
     */
    @Test
    public void testIssue364LinkToInFlowContent() throws IOException {
        try (TestDocument doc = support.run("issue-364-link-to-in-flow-content")) {
            assertEquals(3, doc.pd.getPage(0).getAnnotations().size());

            assertThat(linkArea(doc.pd, 0, 0), pdRectEquals(0, 0, 42.00f, 166.24f, 91.76f, 16.76f));
            assertThat(linkArea(doc.pd, 0, 1), pdRectEquals(0, 1, 72.00f, 115.95f, 22.50f, 16.76f));
            assertThat(linkArea(doc.pd, 0, 2), pdRectEquals(0, 2, 42.00f, 36.00f, 97.58f, 16.76f));

            assertThat(linkDestinationPageNo(doc.pd, 0, 0), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 0, 0), equalTo(172));

            assertThat(linkDestinationPageNo(doc.pd, 0, 1), equalTo(0));
            assertThat(linkDestinationTop(doc.pd, 0, 1), equalTo(103));

            assertThat(linkDestinationPageNo(doc.pd, 0, 2), equalTo(1));
            assertThat(linkDestinationTop(doc.pd, 0, 2), equalTo(172));
        }
    }

    // IMPORTANT: To create additional link tests, please see the Javadoc for this class above.

}

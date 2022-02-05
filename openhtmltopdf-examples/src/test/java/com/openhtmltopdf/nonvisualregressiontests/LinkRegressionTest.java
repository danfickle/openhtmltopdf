package com.openhtmltopdf.nonvisualregressiontests;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport;
import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport.TestDocument;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkArea;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.pdRectEquals;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkDestinationUri;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkDestinationTop;
import static com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport.linkDestinationPageNo;

@RunWith(PrintingRunner.class)
public class LinkRegressionTest {
    public static final String RES_PATH = "/visualtest/links/";
    public static final String OUT_PATH = "target/test/visual-tests/test-output/";

    private final NonVisualTestSupport support = new NonVisualTestSupport(RES_PATH, OUT_PATH);

    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    @Test
    @SuppressWarnings("resource")
    public void testPr798MultipageTableLinkAreas() throws IOException {
        try (TestDocument doc = support.run("pr-798-multipage-table")) {
            assertThat(linkArea(doc.doc(), 0, 0), pdRectEquals(0, 0, 82.88f, 121.24f, 32.70f, 13.84f));
            assertThat(linkArea(doc.doc(), 1, 0), pdRectEquals(1, 0, 82.88f, 207.79f, 32.70f, 13.84f));
            assertThat(linkArea(doc.doc(), 2, 0), pdRectEquals(2, 0, 82.88f, 207.79f, 32.70f, 13.84f));
            assertThat(linkArea(doc.doc(), 3, 0), pdRectEquals(3, 0, 82.88f, 207.79f, 32.70f, 13.84f));
        }
    }

    // To create further link tests, please see the main method in:
    // com.openhtmltopdf.nonvisualregressiontests.support.LinkTestCreator
}

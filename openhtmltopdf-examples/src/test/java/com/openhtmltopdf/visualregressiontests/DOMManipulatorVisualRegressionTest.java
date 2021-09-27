package com.openhtmltopdf.visualregressiontests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.openhtmltopdf.extend.FSDOMMutator;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.VisualTester;

@RunWith(PrintingRunner.class)
public class DOMManipulatorVisualRegressionTest {
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
                "/visualtest/html/",     /* Resource path. */
                "/visualtest/expected/", /* Expected resource path */
                outputDirectory
                );
    }

    /**
     * Tests an example of converting font tags with color attributes to CSS
     * style attributes.
     */
    @Test
    public void testIssue736FontTagConverter() throws IOException {
        FSDOMMutator domChanger = (doc) -> {
            NodeList fontTags = doc.getElementsByTagName("font");

            for (int i = 0; i < fontTags.getLength(); i++) {
                Element fontTag = (Element) fontTags.item(i);

                if (fontTag.hasAttribute("color")) {
                    String color = fontTag.getAttribute("color");

                    if (!fontTag.hasAttribute("style")) {
                        fontTag.setAttribute("style", "color: " + color + ';');
                    } else {
                        String oldStyle = fontTag.getAttribute("style");
                        String newStyle = oldStyle + "; color: " + color + ';';

                        fontTag.setAttribute("style", newStyle);
                    }
                }
            }
        };

        assertTrue(vt.runTest("issue-736-font-tag-converter", (builder) -> {
            builder.addDOMMutator(domChanger);
        }));
    }
}

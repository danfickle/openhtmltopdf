package com.openhtmltopdf.test.generators;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import com.openhtmltopdf.nonvisualregressiontests.LinkRegressionTest;
import com.openhtmltopdf.nonvisualregressiontests.support.LinkTestSupport;
import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport;
import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport.TestDocument;
import com.openhtmltopdf.test.support.TestGeneratorSupport;
import com.openhtmltopdf.util.OpenUtil;

/**
 * Generator to create tests for link annotation placement and
 * actions.<br/><br/>
 *
 * IMPORTANT: This class location is referenced in:
 * {@code /openhtmltopdf-examples/src/main/resources/visualtest/links/README.md}
 */
public class LinkTestCreator {
    /**
     * General process of creating a link area (and destination) test is:<br/>
     * + Craft a minimal html file that demonstrates a broken or fixed problem.<br/>
     * + Place the html file in {@code /openhtmltopdf-examples/src/main/resources/visualtest/links/} ({@link LinkRegressionTest#RES_PATH}) folder.<br/>
     * + Replace the resource variable below with the filename (without .html extension).<br/>
     * + Run this main method.<br/>
     * + The PDF output will be in {@code /openhtmltopdf-examples/target/test/visual-tests/test-output/} ({@link LinkRegressionTest#OUT_PATH}) folder.<br/>
     * + The console output will be a test that contains assertions on the position (x, y, width, height)
     *   of each link in the document (plus link destination assertions).<br/>
     * + Once you have opened and manually verified the links are in the correct place (via mouse cursor
     *   changes for example) you can copy and paste the test into ({@link LinkRegressionTest})
     *   {@code /openhtmltopdf-examples/src/test/java/com/openhtmltopdf/nonvisualregressiontests/LinkRegressionTest.java}<br/>
     * + Run the test and submit a pull-request!
     */
    public static void main(String[] args) throws IOException {
        createSingleLinkTest("issue-364-link-to-in-flow-content");
        //recreateAllLinkTests();
    }

    private static final List<String> ALL_TESTS = Arrays.asList(
            "pr-798-multipage-table",
            "link-area-multiple-boxes",
            "link-area-multiple-page",
            "link-area-multiple-line",
            "link-area-transform-nested",
            "link-area-page-margin-transform",
            "link-area-page-margin",
            "link-external-url",
            "link-area-after-overflow-page",
            "link-area-overflow-page",
            "link-after-overflow-target",
            "link-area-transform-rotate",
            "link-area-transform-translatey",
            "link-inline-target",
            "link-on-overflow-target",
            "link-simple-block",
            "link-transform-target",
            "issue-364-footnote-call-link",
            "issue-364-link-to-footnote-content",
            "issue-364-link-to-in-flow-content"
            );

    private static void createSingleLinkTest(String resource) throws IOException {
        createLinkAnnotationTest(resource, null);
    }

    /**
     * Use this to generate all tests again. Useful if the generator
     * has been improved for example.
     */
    @SuppressWarnings("unused")
    private static void recreateAllLinkTests() {
        StringBuilder sb = new StringBuilder();

        ALL_TESTS.forEach(OpenUtil.rethrowingConsumer(
                res -> createLinkAnnotationTest(res, sb)));

        System.out.println("Generated tests follow:\n\n");
        System.out.println(sb.toString());
    }

    private static class LinkContainer {
        /** Index of the annotation in the page annotations list */
        final int annotationIndex;

        final PDRectangle area;

        /** Zero based destination page number or -1 if not internal link */
        final int destinationPage;

        /** y destination of the link, page relative, bottom up units. */
        final int destinationTop;

        /** If an external link contains uri, otherwise null */
        final String destinationUri;

        LinkContainer(int annotIdx, PDRectangle area, int destinationPage, int destinationTop, String destUri) {
            this.annotationIndex = annotIdx;
            this.area = area;
            this.destinationPage = destinationPage;
            this.destinationTop = destinationTop;
            this.destinationUri = destUri;
        }
    }

    private static class PageContainer {
        /** Zero base page number */
        final int pageNo;

        final List<LinkContainer> links;

        PageContainer(int pageNo, List<LinkContainer> links) {
            this.pageNo = pageNo;
            this.links = links;
        }
    }

    /**
     * Creates a mapper function that takes an annotation index, returning the details
     * we are concerned in a holder object.<br/>
     * Each created function is specific to a specific document and page.
     */
    private static Function<Integer, LinkContainer> linkExtractor(PDDocument doc, int pageNo) {
        return OpenUtil.rethrowingFunction((annotIndex) -> {
            PDAnnotationLink link = (PDAnnotationLink) doc.getPage(pageNo).getAnnotations().get(annotIndex);

            int destPage;
            int destTop;
            String destUri;

            PDPageXYZDestination dest = LinkTestSupport.linkDestinationXYZ(doc, pageNo, annotIndex);
            if (dest != null) {
                destPage = doc.getPages().indexOf(dest.getPage());
                destTop = dest.getTop();
                destUri = null;
            } else {
                destPage = -1;
                destTop = -1;
                destUri = LinkTestSupport.linkDestinationUri(doc, pageNo, annotIndex);
            }

            return new LinkContainer(annotIndex, link.getRectangle(), destPage, destTop, destUri);
        });
    }

    private static List<LinkContainer> getPageLinks(PDDocument doc, int pageNo) throws IOException {
        List<PDAnnotation> lst = doc.getPage(pageNo).getAnnotations();

        return IntStream.range(0, lst.size())
            .boxed()
            .filter(idx -> lst.get(idx) instanceof PDAnnotationLink)
            .map(LinkTestCreator.linkExtractor(doc, pageNo))
            .collect(Collectors.toList());
    }

    private static List<PageContainer> getLinks(PDDocument doc) throws IOException {
        return IntStream.range(0, doc.getNumberOfPages())
                .boxed()
                .map(OpenUtil.rethrowingFunction(
                        i -> new PageContainer(i, getPageLinks(doc, i))))
                .collect(Collectors.toList());
    }

    /**
     * + Generates a PDF from a html resource.<br/>
     * + Outputs the PDF to an output folder {@link LinkRegressionTest#OUT_PATH} so
     * that links can be checked manually.<br/>
     * + Calls {@link #createLinkAnnotationTest(String, PDDocument, StringBuilder)} to generate junit test.
     * @param sb nullable
     */
    private static void createLinkAnnotationTest(String filename, StringBuilder sb) throws IOException {
        NonVisualTestSupport support = 
                new NonVisualTestSupport(LinkRegressionTest.RES_PATH, LinkRegressionTest.OUT_PATH);
        try (TestDocument doc = support.run(filename, false)) {
            System.out.println("Generating test method for " + filename);
            System.out.println("Please check manually before continuing: " +
                Paths.get(LinkRegressionTest.OUT_PATH, filename + ".pdf").toAbsolutePath());
            System.out.println("The test should be added (via copy & paste) in class com.openhtmltopdf.nonvisualregressiontests.LinkRegressionTest");
            System.out.println("\n\n");
            System.out.println(createLinkAnnotationTest(filename, doc.pd, sb));
        }
    }

    /**
     * + Extracts details of each link from the document.<br/>
     * + Generate a junit test that asserts each link is positioned identically and has
     * the same action (ie. go to a page position in current document or external uri).
     * @param sbOpt nullable
     */
    private static String createLinkAnnotationTest(String filename, PDDocument doc, StringBuilder sbOpt) throws IOException {
        StringBuilder sb = sbOpt == null ? new StringBuilder() : sbOpt;
        List<PageContainer> pages = getLinks(doc);
        String testname = TestGeneratorSupport.kebabToPascal(filename);

        sb.append("    /**\n")
          .append("     * TODO: Test description\n")
          .append("     */\n")
          .append("    @Test\n")
          .append(String.format(Locale.ROOT, "    public void test%s() throws IOException {\n", testname))
          .append(String.format(Locale.ROOT, "        try (TestDocument doc = support.run(\"%s\")) {\n", filename));

        // First output the link area assertions
        for (PageContainer page : pages) {
            for (LinkContainer link : page.links) {
                PDRectangle r = link.area;
                sb.append(
                  String.format(Locale.ROOT, "            assertThat(linkArea(doc.pd, %1$d, %2$d), pdRectEquals(%1$d, %2$d, %3$.2ff, %4$.2ff, %5$.2ff, %6$.2ff));\n",
                  page.pageNo, link.annotationIndex,
                  r.getLowerLeftX(), r.getLowerLeftY(), r.getWidth(), r.getHeight()));
            }
        }

        sb.append("\n");

        // Now output the link destination assertions
        for (PageContainer page : pages) {
            for (LinkContainer link : page.links) {
                if (link.destinationUri != null) {
                    sb.append(
                      String.format(Locale.ROOT, "            assertThat(linkDestinationUri(doc.pd, %1$d, %2$d), equalTo(\"%3$s\"));\n",
                        page.pageNo, link.annotationIndex,
                        TestGeneratorSupport.escapeJavaStringLiteral(link.destinationUri)));
                } else if (link.destinationPage != -1) {
                    sb.append(
                      String.format(Locale.ROOT, "            assertThat(linkDestinationPageNo(doc.pd, %1$d, %2$d), equalTo(%3$d));\n",
                        page.pageNo, link.annotationIndex, link.destinationPage));
                    sb.append(
                      String.format(Locale.ROOT, "            assertThat(linkDestinationTop(doc.pd, %1$d, %2$d), equalTo(%3$d));\n",
                        page.pageNo, link.annotationIndex, link.destinationTop));
                }
            }
        }

        sb.append("        }\n")
          .append("    }\n\n");

        return sb.toString();
    }

}

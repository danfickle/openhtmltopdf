package com.openhtmltopdf.nonvisualregressiontests.support;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import com.openhtmltopdf.nonvisualregressiontests.LinkRegressionTest;
import com.openhtmltopdf.nonvisualregressiontests.support.NonVisualTestSupport.TestDocument;
import com.openhtmltopdf.util.OpenUtil;

public class LinkTestCreator {
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

    private static final String LITERAL_REPLACE = "\n\r\t\'\"\\\b\f";
    private static final String LITERAL_WITH = "nrt'\"\\bf";

    public static String escapeJavaStringLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        // None of the bad chars are valid trailing surrogates in UTF16 so
        // we should be right in using chars here rather than codePoints.
        s.chars().forEachOrdered(c -> {
            int idx = LITERAL_REPLACE.indexOf(c);

            if (idx != -1) {
                sb.append('\\')
                  .append(LITERAL_WITH.charAt(idx));
            } else {
                sb.append((char) c);
            }
        });

        return sb.toString();
    }

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

    @SuppressWarnings("resource")
    private static void createLinkAnnotationTest(String filename) throws IOException {
        NonVisualTestSupport support = 
                new NonVisualTestSupport(LinkRegressionTest.RES_PATH, LinkRegressionTest.OUT_PATH);
        try (TestDocument doc = support.run(filename, false)) {
            System.out.println("Generating test method for " + filename);
            System.out.println("Please check manually before continuing: " +
                Paths.get(LinkRegressionTest.OUT_PATH, filename + ".pdf").toAbsolutePath());
            System.out.println("The test should be added (via copy & paste) in class com.openhtmltopdf.nonvisualregressiontests.LinkRegressionTest");
            System.out.println("\n\n");
            System.out.println(createLinkAnnotationTest(filename, doc.doc()));
        }
    }

    private static String createLinkAnnotationTest(String filename, PDDocument doc) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<PageContainer> pages = getLinks(doc);
        String fileParts[] = filename.split(Pattern.quote("-"));
        String testname = Arrays.stream(fileParts)
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());

        sb.append("    @Test\n")
          .append("    @SuppressWarnings(\"resource\")\n")
          .append(String.format(Locale.ROOT, "    public void test%sLinkAreas() throws IOException {\n", testname))
          .append(String.format(Locale.ROOT, "        try (TestDocument doc = support.run(\"%s\")) {\n", filename));

        // First output the link area assertions
        for (PageContainer page : pages) {
            for (LinkContainer link : page.links) {
                PDRectangle r = link.area;
                sb.append(
                  String.format(Locale.ROOT, "            assertThat(linkArea(doc.doc(), %1$d, %2$d), pdRectEquals(%1$d, %2$d, %3$.2ff, %4$.2ff, %5$.2ff, %6$.2ff));\n",
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
                      String.format(Locale.ROOT, "            assertThat(linkDestinationUri(doc.doc(), %1$d, %2$d), equalTo(\"%3$s\"));\n",
                        page.pageNo, link.annotationIndex,
                        escapeJavaStringLiteral(link.destinationUri)));
                } else if (link.destinationPage != -1) {
                    sb.append(
                      String.format(Locale.ROOT, "            assertThat(linkDestinationPageNo(doc.doc(), %1$d, %2$d), equalTo(%3$d));\n",
                        page.pageNo, link.annotationIndex, link.destinationPage));
                    sb.append(
                      String.format(Locale.ROOT, "            assertThat(linkDestinationTop(doc.doc(), %1$d, %2$d), equalTo(%3$d));\n",
                        page.pageNo, link.annotationIndex, link.destinationTop));
                }
            }
        }

        sb.append("        }\n")
          .append("    }\n\n");

        return sb.toString();
    }

    /**
     * General process of creating a link area (and destination) test is:
     * + Craft a minimal html file that demonstrates a broken or fixed problem.
     * + Place the html file in "/openhtmltopdf-examples/src/main/resources/visualtest/links/" folder.
     * + Replace the resource variable below with the filename (without .html extension).
     * + Run this main method.
     * + The PDF output will be in "/openhtmltopdf-examples/target/test/visual-tests/test-output/" folder.
     * + The console output will be a test that contains assertions on the position (x, y, width, height)
     *   of each link in the document (plus link destination assertions).
     * + Once you have opened and manually verified the links are in the correct place (via mouse cursor
     *   changes for example) you can copy and paste the test into:
     *   /openhtmltopdf-examples/src/test/java/com/openhtmltopdf/nonvisualregressiontests/LinkRegressionTest.java
     * + Run the test and submit a pull-request!
     */
    public static void main(String[] args) throws IOException {
        String resource = "pr-798-multipage-table";

        createLinkAnnotationTest(resource);
    }
}

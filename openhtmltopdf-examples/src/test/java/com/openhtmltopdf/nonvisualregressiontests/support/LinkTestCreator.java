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
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
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

        /** Zero based destination page number */
        @SuppressWarnings("unused")
        final int destinationPage;

        /** y destination of the link, page relative, bottom up units. */
        @SuppressWarnings("unused")
        final int destinationTop;

        LinkContainer(int annotIdx, PDRectangle area, int destinationPage, int destinationTop) {
            this.annotationIndex = annotIdx;
            this.area = area;
            this.destinationPage = destinationPage;
            this.destinationTop = 0;
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

    private static PDPageXYZDestination getLinkDestination(PDAnnotation link) throws IOException {
        PDAnnotationLink link0 = (PDAnnotationLink) link;
        PDActionGoTo goto0 = (PDActionGoTo) link0.getAction();
        return (PDPageXYZDestination) goto0.getDestination();
    }

    private static Function<Integer, LinkContainer> linkExtractor(PDDocument doc, List<PDAnnotation> lst) {
        return OpenUtil.rethrowingFunction((Integer annotIndex) -> {
            PDAnnotationLink link = (PDAnnotationLink) lst.get(annotIndex);

            int destPage;
            int destTop;

            if (link.getAction() instanceof PDActionGoTo) {
                PDPageXYZDestination dest = getLinkDestination(link);
                destPage = doc.getPages().indexOf(dest.getPage());
                destTop = dest.getTop();
            } else {
                destPage = -1;
                destTop = -1;
            }

            return new LinkContainer(annotIndex, link.getRectangle(), destPage, destTop);
        });
    }

    private static List<LinkContainer> getPageLinks(PDDocument doc, int i) throws IOException {
        List<PDAnnotation> lst = doc.getPage(i).getAnnotations();

        return IntStream.range(0, doc.getPage(i).getAnnotations().size())
            .boxed()
            .filter(idx -> lst.get(idx) instanceof PDAnnotationLink)
            .map(LinkTestCreator.linkExtractor(doc, lst))
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
    private static void createLinkAnnotationAreaTest(String filename) throws IOException {
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

        for (PageContainer page : pages) {
            for (LinkContainer link : page.links) {
                PDRectangle r = link.area;
                sb.append(
                  String.format(Locale.ROOT, "            assertThat(linkArea(doc.doc(), %1$d, %2$d), pdRectEquals(%1$d, %2$d, %3$.2ff, %4$.2ff, %5$.2ff, %6$.2ff));\n",
                  page.pageNo, link.annotationIndex,
                  r.getLowerLeftX(), r.getLowerLeftY(), r.getWidth(), r.getHeight()));
            }
        }

        sb.append("        }\n")
          .append("    }\n\n");

        return sb.toString();
    }

    /**
     * General process of creating a link area (and link destination test
     * when implemented) is:
     * + Craft a minimal html file that demonstrates a broken or fixed problem.
     * + Place the html file in "/openhtmltopdf-examples/src/main/resources/visualtest/links/" folder.
     * + Replace the resource variable below with the filename (without .html extension).
     * + Run this main method.
     * + The PDF output will be in "/openhtmltopdf-examples/target/test/visual-tests/test-output/" folder.
     * + The console output will be a test that contains assertions on the position (x, y, width, height)
     *   of each link in the document.
     * + Once you have opened and manually verified the links are in the correct place (via mouse cursor
     *   changes for example) you can copy and paste the test into:
     *   /openhtmltopdf-examples/src/test/java/com/openhtmltopdf/nonvisualregressiontests/LinkRegressionTest.java
     * + Run the test and submit a pull-request!
     */
    public static void main(String[] args) throws IOException {
        String resource = "pr-798-multipage-table";

        createLinkAnnotationAreaTest(resource);

        // Not implemented yet:
        // createLinkAnnotationDestinationTest(resource);
    }
}

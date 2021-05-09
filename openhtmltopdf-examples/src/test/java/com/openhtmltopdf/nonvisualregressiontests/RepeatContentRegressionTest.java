package com.openhtmltopdf.nonvisualregressiontests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.testcases.TestcaseRunner;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

@RunWith(PrintingRunner.class)
public class RepeatContentRegressionTest {
    private static final String RES_PATH = "/repeated-content-tests/";
    private static final String OUT_PATH = "target/test/visual-tests/test-output/";

    @BeforeClass
    public static void configure() {
        TestSupport.quietLogs();
    }

    interface StringMatcher {
        List<String> problems(String expected, String actual);
        void doAssert(String expected, String actual, String fileName, List<String> problems);
    }

    /**
     * Simple tests with content all in one layer
     * (ie. no z-index, absolute, relative, fixed, transform)
     * should generally use an ordered matcher.
     */
    private static class OrderedMatcher implements StringMatcher {
        public List<String> problems(String expected, String actual) {
            if (!expected.equals(actual)) {
                return Collections.singletonList("Mismatched ordered");
            }

            return Collections.emptyList();
        }

        public void doAssert(String expected, String actual, String fileName, List<String> problems) {
            assertEquals("Mismatched: " + fileName, expected, actual);
        }
    }

    /**
     * Layers have to follow a specific painting order so they may be out of order.
     */
    private static class UnOrderedMatcher implements StringMatcher {
        public List<String> problems(String expected, String actual) {
            String[] expWords = expected.split("\\s");
            String[] actWords = actual.split("\\s");

            Predicate<String> filter = w -> !w.trim().isEmpty();

            Set<String> exp = Arrays.stream(expWords)
                                    .filter(filter)
                                    .collect(Collectors.toSet());

            List<String> act = Arrays.stream(actWords)
                                     .filter(filter)
                                     .collect(Collectors.toList());

            Set<String> seen = new HashSet<>();

            List<String> problems = new ArrayList<>();

            for (String word : act) {
                if (seen.contains(word)) {
                    problems.add("Repeat content: " + word);
                }

                seen.add(word);

                if (!exp.contains(word)) {
                    problems.add("Unexpected content: " + word);
                }
            }

            for (String word : exp) {
                if (!act.contains(word)) {
                    problems.add("Missing: " + word);
                }
            }

            return problems;
        }

        public void doAssert(String expected, String actual, String fileName, List<String> problems) {
            fail(problems.stream().collect(Collectors.joining("\n")));
        }
    }

    private static final StringMatcher ORDERED = new OrderedMatcher();
    private static final StringMatcher UNORDERED = new UnOrderedMatcher();

    private static void render(String fileName, String html, BuilderConfig config, String proof, StringMatcher matcher) throws IOException {
        ByteArrayOutputStream actual = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(html, NonVisualRegressionTest.class.getResource(RES_PATH).toString());
        builder.toStream(actual);
        builder.useFastMode();
        builder.testMode(true);
        config.configure(builder);

        try {
            builder.run();
        } catch (IOException e) {
            System.err.println("Failed to render resource (" + fileName + ")");
            e.printStackTrace();
            throw e;
        }

        byte[] pdfBytes = actual.toByteArray();

        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSuppressDuplicateOverlappingText(false);
            stripper.setLineSeparator("\n");

            String text = stripper.getText(doc).trim();
            String expected = proof.trim().replace("\r\n", "\n");

            List<String> problems = matcher.problems(expected, text);

            if (!problems.isEmpty()) {
                FileUtils.writeByteArrayToFile(new File(OUT_PATH, fileName + ".pdf"), pdfBytes);

                matcher.doAssert(expected, text, fileName, problems);
            }
        }
    }

    private static void run(String fileName, BuilderConfig config, StringMatcher matcher) throws IOException {
        String absResPath = RES_PATH + fileName + ".html";

        try (InputStream is = TestcaseRunner.class.getResourceAsStream(absResPath)) {
            byte[] htmlBytes = IOUtils.toByteArray(is);
            String htmlWithProof = new String(htmlBytes, StandardCharsets.UTF_8);

            String[] parts = htmlWithProof.split(Pattern.quote("======="));
            String html = parts[0];
            String proof = parts[1];

            render(fileName, html, config, proof, matcher);
        }
    }

    private static void runOrdered(String fileName) throws IOException {
        run(fileName, builder -> { }, ORDERED);
    }

    private static void runUnOrdered(String fileName) throws IOException {
        run(fileName, builder -> { }, UNORDERED);
    }

    /**
     * inline-blocks/relative/absolute with z-index.
     */
    @Test
    public void testInlineBlockZIndex() throws IOException {
        runOrdered("inline-block-z-index");
    }

    /**
     * Multiple in-flow inline-blocks on the one page. 
     */
    @Test
    public void testInlineBlockMultiple() throws IOException {
        runUnOrdered("inline-block-multiple");
    }

    /**
     * Multiple in-flow inline-blocks across pages with large page margin.
     */
    @Test
    public void testInlineBlockPages() throws IOException {
        runUnOrdered("inline-block-pages");
    }

    /**
     * Float that goes over two pages.
     */
    @Test
    public void testFloatsPages() throws IOException {
        runUnOrdered("floats-pages");
    }

}

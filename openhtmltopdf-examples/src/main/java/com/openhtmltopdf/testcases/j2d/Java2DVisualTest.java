package com.openhtmltopdf.testcases.j2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.visualtest.Java2DVisualTester;
import com.openhtmltopdf.visualtest.Java2DVisualTester.Java2DBuilderConfig;
import com.openhtmltopdf.visualtest.TestSupport;

public class Java2DVisualTest {
    private final Java2DVisualTester vtester;
    private final List<String> failed = new ArrayList<>();
    
    private Java2DVisualTest() throws IOException {
       File outputDirectory = new File("target/test/visual-tests/test-output/j2d");
       TestSupport.makeFontFiles();
       vtester = new Java2DVisualTester(
                    "/visualtest/j2d/html/",     /* Resource path. */
                    "/visualtest/j2d/expected/", /* Expected resource path */
                    "/visualtest/j2d/expected-single-page/", /* Single page expected */
                    outputDirectory
                    );
    }
    
    private void run(String resource, Java2DBuilderConfig config) throws IOException {
        if (!vtester.runTest(resource, config)) {
            failed.add(resource);
        }
        if (!vtester.runSinglePageTest(resource, config)) {
            failed.add(resource + " (single-page mode)");
        }
    }
    
    private void run(String resource) throws IOException {
        run(resource, (builder) -> {});
    }
    
    private void runAllTests() throws IOException {
        run("simple-blocks");
        run("simple-text");
        run("margins-clipping-transforms");
        run("clip-inside-transform");
        run("linear-gradient");
        run("positioned-elements");
        run("images", builder -> builder.useSVGDrawer(new BatikSVGDrawer()));
        run("sized-repeat-images");

        // If you add a test here, please remember to also
        // add it to runOneTest (commented out).
    }
    
    private void runOneTest() throws IOException {
        // run("simple-blocks");
        // run("simple-text");
        // run("margins-clipping-transforms");
        // run("clip-inside-transform");
        // run("linear-gradient");
        // run("positioned-elements");
        // run("images", builder -> builder.useSVGDrawer(new BatikSVGDrawer()));
        // run("sized-repeat-images");

        // If you add a test here, please remember to also add
        // it to runAllTests.
    }

    // These are not automated tests due to the slight differences between JDK versions
    // for Java2D output. Rather they are manual tests meant to be run before a release.
    public static void main(String[] args) throws Exception {
        Java2DVisualTest test = new Java2DVisualTest();

        test.runOneTest();
        test.runAllTests();

        System.out.println("\nThe failed tests were:\n");
        System.out.println(test.failed.stream().collect(Collectors.joining("\n")));
    }
}

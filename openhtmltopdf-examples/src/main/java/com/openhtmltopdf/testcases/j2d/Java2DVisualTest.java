package com.openhtmltopdf.testcases.j2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                    outputDirectory
                    );
    }
    
    private void run(String resource, Java2DBuilderConfig config) throws IOException {
        if (!vtester.runTest(resource, config)) {
            failed.add(resource);
        }
    }
    
    private void run(String resource) throws IOException {
        run(resource, (builder) -> {});
    }
    
    private void runAllTests() throws IOException {
        run("simple-blocks");
        
        // If you add a test here, please remember to also
        // add it to runOneTest (commented out).
        
        System.out.println("The failed tests were:");
        System.out.println(failed);
    }
    
    private void runOneTest() throws IOException {
        // run("simple-blocks");
        
        // If you add a test here, please remember to also add
        // it to runAllTests.
    }
    
    // These are not automated tests due to the slight differences between JDK versions
    // for Java2D output. Rather they are manual tests meant to be run before a release.
    public static void main(String[] args) throws Exception {
        Java2DVisualTest test = new Java2DVisualTest();
        
        test.runOneTest();
        test.runAllTests();
    }
}

package com.openhtmltopdf.testcases;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

public class CssPropertiesTest {
    @Test
    public void testColorProperty() throws Exception {
        if (SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("color");
        } else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("color");
        }
    }

    @Test
    public void testBackgroundColorProperty() throws Exception {
        if (SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("background-color");
        } else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("background-color");
        }
    }

    /**
     * Also tests background-repeat, background-size and background-position.
     */
    @Test
    public void testBackgroundImageProperty() throws Exception {
        if (SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("background-image");
        } else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("background-image");
        }
    }

    @Test
    public void testInvalidBackgroundImageUrl() throws Exception {
        TestcaseRunner.runTestWithoutOutputAndAllowWarnings("invalid-url-background-image");
    }

    @Test
    public void testTextAlignProperty() throws Exception {
        if (SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("text-align");
        } else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("text-align");
        }
    }

    @Test
    public void testFontFamilyBuiltIn() throws Exception {
        if(SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("font-family-built-in");
        }else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("font-family-built-in");
        }
    }

    @Test
    public void testFormControls() throws Exception {
        if (SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("form-controls");
        } else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("form-controls");
        }
    }

    @Test
    public void testCss3MultiColumnLayout() throws Exception {
        if (SystemUtils.isJavaVersionAtLeast(170)) {
            TestcaseRunner.runTestWithoutOutput("multi-column-layout");
        } else {
            TestcaseRunner.runTestWithoutOutputAndAllowWarnings("multi-column-layout");
        }
    }
}

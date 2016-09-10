package com.openhtmltopdf.testcases;

import org.junit.Test;

public class CssPropertiesTest {
	@Test
	public void testColorProperty() throws Exception {
		TestcaseRunner.runTestWithoutOutput("color");
	}
	
	@Test
	public void testBackgroundColorProperty() throws Exception {
		TestcaseRunner.runTestWithoutOutput("background-color");
	}

	/**
	 * Also tests background-repeat, background-size and background-position.
	 */
	@Test
	public void testBackgroundImageProperty() throws Exception {
		TestcaseRunner.runTestWithoutOutput("background-image");
	}
	
	@Test
	public void testInvalidBackgroundImageUrl() throws Exception {
		TestcaseRunner.runTestWithoutOutputAndAllowWarnings("invalid-url-background-image");
	}
	
	@Test
	public void testTextAlignProperty() throws Exception {
		TestcaseRunner.runTestWithoutOutput("text-align");
	}
	
	@Test
	public void testFontFamilyBuiltIn() throws Exception {
		TestcaseRunner.runTestWithoutOutput("font-family-built-in");
	}

}

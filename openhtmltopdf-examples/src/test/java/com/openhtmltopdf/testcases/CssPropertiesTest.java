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

}

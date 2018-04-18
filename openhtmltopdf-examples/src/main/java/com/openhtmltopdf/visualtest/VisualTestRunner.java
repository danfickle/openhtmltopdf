package com.openhtmltopdf.visualtest;

import java.io.File;
import java.io.IOException;

public class VisualTestRunner {
	public static void main(String[] args) throws IOException {
		File overrideDirectory = new File("target/test/visual-tests/user-override/");
		File outputDirectory = new File("target/test/visual-tests/test-output/");
		
		overrideDirectory.mkdirs();
		outputDirectory.mkdirs();
		
		VisualTester vt = new VisualTester("/visualtest/html/",
				new File("src/main/resources/visualtest/expected/"),
				overrideDirectory,
				outputDirectory
				);

		vt.runTest("z-index-absolute");
		
		/* Add more visual test cases here. */
		
		
	}
}

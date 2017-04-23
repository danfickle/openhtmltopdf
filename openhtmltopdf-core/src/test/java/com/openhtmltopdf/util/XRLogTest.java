package com.openhtmltopdf.util;

import org.junit.Assert;
import org.junit.Test;

public class XRLogTest {

	@Test
	public void testDisableLogBeforeFirstLog() {
		XRLog.setLoggingEnabled(false);
		Assert.assertFalse(XRLog.isLoggingEnabled());
		XRLog.load("First log");
		Assert.assertFalse(XRLog.isLoggingEnabled());
	}
	
}

package com.openhtmltopdf.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class XRLogTest {

	@Test
	public void testDisableLogBeforeFirstLog() {
		XRLog.setLoggingEnabled(false);
		Assert.assertFalse(XRLog.isLoggingEnabled());
		XRLog.log(Level.INFO, LogMessageId.LogMessageId0Param.LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES);
		Assert.assertFalse(XRLog.isLoggingEnabled());
	}
	
}

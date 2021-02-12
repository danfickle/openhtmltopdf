package com.openhtmltopdf.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class XRLogTest {

	@Test
	public void testDisableLogBeforeFirstLog() {
		XRLog.setLoggingEnabled(false);
		Assert.assertFalse(XRLog.isLoggingEnabled());
		XRLog.log(Level.INFO, LogMessageId.LogMessageId0Param.LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES);
		Assert.assertFalse(XRLog.isLoggingEnabled());
	}

	/**
	 * See issue https://github.com/danfickle/openhtmltopdf/issues/646
	 */
	@Test
	public void testConcurrentInitLog() throws InterruptedException {
		int p = 20;
		CountDownLatch latch = new CountDownLatch(p);
		CountDownLatch end = new CountDownLatch(p);
		AtomicInteger counter = new AtomicInteger();
		for (int i = 0; i < p;i++) {
			new Thread(() -> {
				latch.countDown();
				try {
					latch.await();
					XRLog.log(Level.SEVERE, LogMessageId.LogMessageId0Param.CASCADE_IS_ABSOLUTE_CSS_UNKNOWN_GIVEN);
					counter.incrementAndGet();
					end.countDown();
				} catch (Throwable e) {
					end.countDown();
				}
			}).start();
		}
		end.await();
		Assert.assertEquals(p, counter.get()); //we expect 0 NPE -> counter = 20
	}
	
}

package com.openhtmltopdf.util;

import com.openhtmltopdf.layout.SharedContext;

/**
 * Because OpenHTMLtoPDF is designed to run in a single thread at all times for one invocation,
 * we can use a ThreadLocal to store pseudo global variables.
 * This MUST be set up in the appropriate renderer.
 */
public class ThreadCtx {
	private static final ThreadLocal<ThreadData> data = new ThreadLocal<ThreadCtx.ThreadData>() {
		@Override
		protected ThreadData initialValue() {
			return new ThreadData();
		}
	};
	
	public static ThreadData get() {
		return data.get();
	}
	
	public static void cleanup() {
		data.remove();
	}
	
	public static class ThreadData {
		private ThreadData() { }
		private SharedContext sharedContext;
		
		public SharedContext sharedContext() {
			if (this.sharedContext == null)
				throw new NullPointerException("SharedContext must be registered in renderer.");
			
			return this.sharedContext;
		}
		
		public void setSharedContext(SharedContext sharedContext) {
			this.sharedContext = sharedContext;
		}
	}
}

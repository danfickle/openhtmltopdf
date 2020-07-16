package com.openhtmltopdf.util;

import com.openhtmltopdf.layout.SharedContext;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Because OpenHTMLtoPDF is designed to run in a single thread at all times for one invocation,
 * we can use a ThreadLocal to store pseudo global variables.
 * This MUST be set up in the appropriate renderer.
 */
public class ThreadCtx {

	private static final ThreadLocal<ThreadData> data = ThreadLocal.withInitial(ThreadData::new);
	private static final ThreadLocal<Consumer<Diagnostic>> diagnosticConsumer = new ThreadLocal<>();



	public static ThreadData get() {
		return data.get();
	}

	static void addDiagnostic(Diagnostic diagnostic) {
		Consumer<Diagnostic> consumer = diagnosticConsumer.get();
		if (consumer != null) {
			consumer.accept(diagnostic);
		}
	}
	
	public static void cleanup() {
		data.remove();
	}

	public static Closeable applyDiagnosticConsumer(Consumer<Diagnostic> consumer) {
		diagnosticConsumer.set(consumer);
		return diagnosticConsumer::remove;
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

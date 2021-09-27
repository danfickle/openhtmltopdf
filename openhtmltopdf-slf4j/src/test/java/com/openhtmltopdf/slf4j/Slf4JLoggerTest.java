package com.openhtmltopdf.slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.BeforeClass;
import org.junit.Test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class Slf4JLoggerTest {

    @BeforeClass
    public static void levelFormat() {
        // We use this property to differentiate log messages
        // going through slf4j to those going through the default
        // java.util.logging logger implementation.
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
    }

    /**
     * This is a simple automatic test to ensure log messages are
     * delivered by slf4j in the console.
     */
    @Test
    public void testLogger() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "UTF-8");
        PrintStream old = System.err;
        System.setErr(ps);

        XRLog.setLoggerImpl(new Slf4jLogger());
        runWithLogOutput();

        ps.flush();
        String log = baos.toString("UTF-8");

        // Uncomment to see what is being logged.
        // old.println(log);
        System.setErr(old);

        assertThat(log, containsString("] [INFO]"));
        assertThat(log, containsString("] [WARN]"));
    }

    /**
     * This is a manual test to show that log level can be changed
     * by the concrete log implementation.
     * 
     * It should not output info level messages.
     * 
     * NOTE: This test should be run in isolation as the system property
     * only takes affect if set before the logger is first created.
     */
    @Test
    public void testLoggerWarnLevel() throws IOException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        XRLog.setLoggerImpl(new Slf4jLogger());

        runWithLogOutput();
    }

    private void runWithLogOutput() throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.useFastMode();
            builder.toStream(os);
            builder.withHtmlContent("<html><body style=\"invalid-prop: 5;\">Test</body></html>", null);
            builder.run();
        }
    }
}

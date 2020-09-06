package com.openhtmltopdf.templates;

import static org.junit.Assert.assertEquals;

import java.util.logging.Level;

import org.junit.BeforeClass;
import org.junit.Test;

import com.openhtmltopdf.util.XRLog;

public class PdfTemplatesTest {
    @BeforeClass
    public static void configure() {
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));
    }

    @Test
    public void testPdfTemplates() {
        Application app = new Application();
        int code = app.run();

        assertEquals(0, code);
    }
}

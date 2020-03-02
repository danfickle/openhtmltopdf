package com.openhtmltopdf.visualtest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.bidi.support.ICUBreakers;
import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLogger;
import com.openhtmltopdf.visualtest.Java2DVisualTester.Java2DBuilderConfig;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class TestSupport {
    /**
     * Output the font file as a regular file so we don't have to use streams.
     * @throws IOException
     */
    public static void makeFontFile(String resource) throws IOException {
        File outputDirectory = new File("target/test/visual-tests/test-output/j2d/");
        outputDirectory.mkdirs();

        File fontFile = new File("target/test/visual-tests/" + resource);
        
        if (!fontFile.exists()) {
            try (InputStream in = TestSupport.class.getResourceAsStream("/visualtest/html/fonts/" + resource)) {
                Files.copy(in, fontFile.toPath());
            }
        }
    }
    
    /**
     * Output the test fonts from classpath to files in target so we can use them 
     * without streams.
     */
    public static void makeFontFiles() throws IOException {
        makeFontFile("Karla-Bold.ttf");
        makeFontFile("NotoNaskhArabic-Regular.ttf");
        makeFontFile("SourceSansPro-Regular.ttf");
    }
    
    public static class StringBuilderLogger implements XRLogger {
        private final StringBuilder sb;
        private final XRLogger delegate;
        
        public StringBuilderLogger(StringBuilder sb, XRLogger delegate) {
            this.delegate = delegate;
            this.sb = sb;
        }
        
        @Override
        public void setLevel(String logger, Level level) {
        }

        @Override
        public void log(String where, Level level, String msg, Throwable th) {
            if (th == null) {
                log(where, level, msg);
                return;
            }
            StringWriter sw = new StringWriter();
            th.printStackTrace(new PrintWriter(sw, true));
            sb.append(where + ": " + level + ":\n" + msg + sw.toString() + "\n");
            delegate.log(where, level, msg, th);
        }

        @Override
        public void log(String where, Level level, String msg) {
            sb.append(where + ": " + level + ": " + msg + "\n");
            delegate.log(where, level, msg);
        }
    }
    
    /**
     * A simple line breaker so that our tests are not reliant on the external Java API.
     */
    public static class SimpleTextBreaker implements FSTextBreaker {
        private String text;
        private int position;
        
        @Override
        public int next() {
            int ret = text.indexOf(' ', this.position);
            this.position = ret + 1;
            return ret;
        }

        @Override
        public void setText(String newText) {
            this.text = newText;
            this.position = 0;
        }
    }
    
    /**
     * A simple line breaker that produces similar results to the JRE standard line breaker.
     * So we can test line breaking/justification with conditions more like real world.
     */
    public static class CollapsedSpaceTextBreaker implements FSTextBreaker {
        private final static Pattern SPACES = Pattern.compile("[\\s\u00AD]");
        private Matcher matcher;
        
        @Override
        public int next() {
            if (!matcher.find()) {
                return -1;
            }
        
            return matcher.end();
        }

        @Override
        public void setText(String newText) {
            this.matcher = SPACES.matcher(newText);
        }
    }
    
    public static final BuilderConfig WITH_FONT = (builder) -> {
        builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
        builder.useUnicodeLineBreaker(new SimpleTextBreaker());
    };
    
    public static final Java2DBuilderConfig J2D_WITH_FONT = (builder) -> {
        builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
        builder.useUnicodeLineBreaker(new SimpleTextBreaker());
    };
    
    public static final BuilderConfig WITH_EXTRA_FONT = (builder) -> {
        WITH_FONT.configure(builder);
        builder.useFont(new File("target/test/visual-tests/SourceSansPro-Regular.ttf"), "ExtraFont");
    };
    
    public static final BuilderConfig WITH_ARABIC = (builder) -> {
        WITH_FONT.configure(builder);
        builder.useFont(new File("target/test/visual-tests/NotoNaskhArabic-Regular.ttf"), "arabic");
        builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
        builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
        builder.useUnicodeLineBreaker(new ICUBreakers.ICULineBreaker(Locale.US)); // Overrides WITH_FONT
        builder.defaultTextDirection(TextDirection.LTR);
    };
    
    public static final BuilderConfig WITH_COLLAPSED_LINE_BREAKER = (builder) -> {
        WITH_FONT.configure(builder);
        builder.useUnicodeLineBreaker(new CollapsedSpaceTextBreaker());
    };
    
    /**
     * Configures the builder to use SVG drawer but not font.
     */
    public static final BuilderConfig WITH_SVG = (builder) -> builder.useSVGDrawer(new BatikSVGDrawer());
}

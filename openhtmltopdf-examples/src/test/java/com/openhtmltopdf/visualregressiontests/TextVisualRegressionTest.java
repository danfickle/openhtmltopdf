package com.openhtmltopdf.visualregressiontests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.openhtmltopdf.extend.FSTextBreaker;
import com.openhtmltopdf.visualtest.VisualTester;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class TextVisualRegressionTest {
    private VisualTester vtester;
    
    /**
     * A simple line breaker so that our tests are not reliant on the external Java API.
     */
    private static class SimpleTextBreaker implements FSTextBreaker {
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
    
    private static final BuilderConfig WITH_FONT = (builder) -> {
        builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "TestFont");
        builder.useUnicodeLineBreaker(new SimpleTextBreaker());
    };
    
    /**
     * Output the font file as a regular file so we don't have to use streams.
     * @throws IOException
     */
    @BeforeClass
    public static void makeFontFile() throws IOException {
        File overrideDirectory = new File("target/test/visual-tests/user-override/");
        File outputDirectory = new File("target/test/visual-tests/test-output/");
        
        overrideDirectory.mkdirs();
        outputDirectory.mkdirs();
        
        File fontFile = new File("target/test/visual-tests/Karla-Bold.ttf");
        
        if (!fontFile.exists()) {
            try (InputStream in = TextVisualRegressionTest.class.getResourceAsStream("/visualtest/html/fonts/Karla-Bold.ttf");
                 OutputStream out = new FileOutputStream("target/test/visual-tests/Karla-Bold.ttf")) {
                 IOUtils.copy(in, out);
            }
        }
    }
    
    @Before
    public void configureTester() {
        File overrideDirectory = new File("target/test/visual-tests/user-override/");
        File outputDirectory = new File("target/test/visual-tests/test-output/");
        
        vtester = new VisualTester("/visualtest/html/text/", /* Resource path. */
                new File("src/main/resources/visualtest/expected/text/"), /* Expected directory */
                overrideDirectory,
                outputDirectory
                );
    }
    
    private boolean run(String resource) throws IOException {
        return vtester.runTest(resource, WITH_FONT);
    }
    
    /**
     * Tests simple text output in absolute positioned blocks.
     */
    @Test
    public void testPositioningAbsolute() throws IOException {
        assertTrue(run("positioning-absolute"));
    }
    
    /**
     * Tests z-index property for text with absolute positioned elements. 
     */
    @Test
    public void testZIndexWithAbsolutePosition() throws IOException {
        assertTrue(run("z-index-absolute"));
    }
    
    /**
     * Tests fixed element text is repeated on each page.
     */
    @Test
    public void testPositioningFixed() throws IOException {
        assertTrue(run("positioning-fixed"));
    } 

    /**
     * Tests overflow:hidden and visible with text. Containers are static blocks.
     * Overflow content includes static blocks and floats.
     */
    @Test
    public void testOverflow() throws IOException {
        assertTrue(run("overflow"));
    }
    
    /**
     * Tests that static block text overflows onto inserted shadow page. 
     */
    @Test
    public void testHorizPageOverflowStatic() throws IOException {
        assertTrue(run("horiz-page-overflow-static"));
    }
    
    /**
     * Tests that absolute positioned block text overflows onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowAbsolute() throws IOException {
        assertTrue(run("horiz-page-overflow-absolute"));
    }
    
    /**
     * Tests that static floated block text overflows onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowFloat() throws IOException {
        assertTrue(run("horiz-page-overflow-float"));
    }
    
    /**
     * Tests that non-paginated table column text overflows onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowTable() throws IOException {
        assertTrue(run("horiz-page-overflow-table"));
    }
    
    /**
     * Tests that paginated table column (including header and footer) text overflows onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowTablePaged() throws IOException {
        assertTrue(run("horiz-page-overflow-table-paged"));
    }
    
    /**
     * Tests that fixed block text does NOT overflow onto inserted shadow pages.
     */
    @Test
    public void testHorizPageOverflowFixed() throws IOException {
        assertTrue(run("horiz-page-overflow-fixed"));
    }
    
    /**
     * Tests that nowrap and too long text overflows onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowInline() throws IOException {
        assertTrue(run("horiz-page-overflow-inline"));
    }
    
    /**
     * Tests that static inline-block text overflows onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowInlineBlock() throws IOException {
        assertTrue(run("horiz-page-overflow-inline-block"));
    }

    /**
     * Tests that a static inline-block sitting entirely on an overflow page appears.
     */
    @Test
    @Ignore // The inline block and its text does not appear on the overflow page. 
    public void testHorizPageOverflowInlineBlock2() throws IOException {
        assertTrue(run("horiz-page-overflow-inline-block-2"));
    }
    
    /**
     * Tests that overflow:hidden text does NOT generate shadow pages. Includes case where content
     * is absolute block and a case where content is a static block. 
     */
    @Test
    @Ignore // Text is causing overflow pages and text appears on overflow pages in contravention of overflow hidden.
    public void testHorizPageOverflowHidden() throws IOException {
        assertTrue(run("horiz-page-overflow-hidden"));
    }
    
    /**
     * Tests that text content transformed past page edge appears on shadow page.
     */
    @Test
    public void testHorizPageOverflowTransform() throws IOException {
        assertTrue(run("horiz-page-overflow-transform"));
    }
    
    /**
     * Tests that text content transformed past page edge generates a shadow page.
     */
    @Test
    public void testHorizPageOverflowTransform2() throws IOException {
        assertTrue(run("horiz-page-overflow-transform-2"));
    }
    
    /**
     * Tests that a nowrap span inside a line wraps to a new line if needed. Issue 302.
     */
    @Test
    @Ignore // Greedily puts nowrap span on same line even though it does not fit.
    public void testLineWrapNoWrapSpan() throws IOException {
        assertTrue(run("line-wrap-nowrap-span"));
    }

    /**
     * Tests that an element boundary is NOT seen as a line break opportunity by itself (eg. mid word). Issue 39.
     */
    @Test
    @Ignore // Element start and finish are seen as line breaking opportunities.
    public void testLineWrapShouldNotWrapElementBoundary() throws IOException {
        assertTrue(run("line-wrap-should-not-wrap-element-boundary"));
    }
    
    /**
     * Tests that with word-wrap: break-word an oversized word will start on its
     * own line and be split over as many lines as needed.
     */
    @Test
    public void testLineWrapBreakWord() throws IOException {
        assertTrue(run("line-wrap-break-word"));
    }
    
    /**
     * Tests that word-break: break-all is supported. Ie. A break can be inserted in the middle of 
     * a word at the end of the line even if the word could fit on a line by itself. Issue 113.
     */
    @Test
    @Ignore // We do not support the word-break CSS property.
    public void testLineWrapBreakAll() throws IOException {
        assertTrue(run("line-wrap-break-all"));
    }
    
    /**
     * Tests that white-space: pre-wrap works as specified. Issue 305.
     */
    @Test
    public void testLineWrapPreWrap() throws IOException {
        assertTrue(run("line-wrap-pre-wrap"));
    }
    
    /**
     * Tests that text content dows not overflow a static block with overflow:hidden.
     */
    @Test
    public void testHiddenStatic() throws IOException {
        assertTrue(run("hidden-static"));
    }
    
    /**
     * Tests that text in a static inline-block content does not overflow a static block with overflow:hidden.
     */
    @Test
    public void testHiddenInlineBlock() throws IOException {
        assertTrue(run("hidden-inline-block"));
    }

    /**
     * Tests that text in a floated block does not overflow a static block with overflow:hidden.
     */
    @Test
    public void testHiddenFloat() throws IOException {
        assertTrue(run("hidden-float"));
    }
    
    /**
     * Tests that text in transformed static blocks does not overflow static block parent with overflow:hidden.
     */
    @Test
    public void testHiddenTransform() throws IOException {
        assertTrue(run("hidden-transform"));
    }
    
    /**
     * Tests that text in an absolute block does not overflow relative block parent with overflow:hidden.
     */
    @Test
    public void testHiddenAbsolute() throws IOException {
        assertTrue(run("hidden-absolute"));
    }
    
    /**
     * Tests that overflow hidden inside a transformed element correctly uses
     * the transformed coordinate space.
     */
    @Test
    public void testHiddenInsideTransform() throws IOException {
        assertTrue(run("hidden-inside-transform"));
    }
}

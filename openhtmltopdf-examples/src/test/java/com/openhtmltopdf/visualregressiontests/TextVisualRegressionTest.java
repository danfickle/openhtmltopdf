package com.openhtmltopdf.visualregressiontests;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.FSObjectDrawerFactory;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.testlistener.PrintingRunner;
import com.openhtmltopdf.visualtest.TestSupport;
import com.openhtmltopdf.visualtest.VisualTester;

@RunWith(PrintingRunner.class)
public class TextVisualRegressionTest {
    private VisualTester vtester;

    @BeforeClass
    public static void makeFontFiles() throws IOException {
        TestSupport.quietLogs();
        TestSupport.makeFontFiles();
    }

    @Before
    public void configureTester() {
        File outputDirectory = new File("target/test/visual-tests/test-output/");
        
        vtester = new VisualTester(
                "/visualtest/html/text/",     /* Resource path. */
                "/visualtest/expected/text/", /* Expected resource path */
                outputDirectory
                );
    }
    
    private boolean run(String resource) throws IOException {
        return vtester.runTest(resource, TestSupport.WITH_FONT);
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
    public void testHorizPageOverflowInlineBlock2() throws IOException {
        assertTrue(run("horiz-page-overflow-inline-block-2"));
    }
    
    /**
     * Tests that overflow:hidden text does NOT generate shadow pages. Includes case where content
     * is absolute block and a case where content is a static block. 
     */
    @Test
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
     * Tests that rotated text on overflow page entirely clipped out by the page margin
     * should not generate an overflow page as such page will be visually empty.
     */
    @Test
    @Ignore // Output contains visually empty overflow page.
    public void testHorizPageOverflowTransform3() throws IOException {
        assertTrue(run("horiz-page-overflow-transform-3"));
    }
    
    /**
     * Tests that a nowrap span inside a line wraps to a new line if needed. Issue 302.
     */
    @Test
    public void testLineWrapNoWrapSpan() throws IOException {
        assertTrue(run("line-wrap-nowrap-span"));
    }

    /**
     * Tests that various nested nowrap spans inside normal wrap works.
     */
    @Test
    public void testPr587LineWrapNoWrapSpanNested() throws IOException {
        assertTrue(run("line-wrap-nowrap-span-nested"));
    }

    /**
     * Test provided with pr#587, fix for break before nowrap span.
     */
    @Test
    public void testPr587FixForBreakBeforeNoWrap() throws IOException {
        assertTrue(run("nowrap"));
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
     * Tests that text in an absolute block does not overflow relative block parent with overflow:hidden.
     * Issue 273.
     */
    @Test
    public void testHiddenAbsolute2() throws IOException {
        assertTrue(run("hidden-absolute-2"));
    }
    
    /**
     * Tests that overflow hidden inside a transformed element correctly uses
     * the transformed coordinate space.
     */
    @Test
    public void testHiddenInsideTransform() throws IOException {
        assertTrue(run("hidden-inside-transform"));
    }
    
    /**
     * Tests that static inline-blocks expand to fit their text.
     */
    @Test
    public void testInlineBlockExpands() throws IOException {
        assertTrue(run("inline-block-expands"));
    }

    /**
     * Tests that text does not overflow inline-block with overflow set to hidden.
     */
    @Test
    public void testInlineBlockHidden() throws IOException {
        assertTrue(run("inline-block-hidden"));
    }

    /**
     * Tests that static inline-block can contain floating static block text.
     * See {@link VisualRegressionTest#testInlineBlockFloat()}
     */
    @Ignore // Float is hidden behind the background-color of inline-block.
            // This is because floats are painted before inline-blocks.
            // This problem is also present in the old slow renderer.
    @Test
    public void testInlineBlockFloat() throws IOException {
        assertTrue(run("inline-block-float"));
    }
    
    /**
     * Tests that relative inline-block can contain absolute positioned block with text.
     */
    @Test
    public void testInlineBlockAbsolute() throws IOException {
        assertTrue(run("inline-block-absolute"));
    }
    
    /**
     * With static blocks, rotate then translate, page margin, body margin and padding.
     */
    @Test
    public void testTransformWithinTransform() throws IOException {
        assertTrue(run("transform-inside-transform"));
    }
    
    /**
     * With an absolute block, rotate, large page margin, small block margin, small padding, small border.
     */
    @Test
    public void testTransformAbsolute() throws IOException {
        assertTrue(run("transform-absolute"));
    }

    /**
     * With a floated static block, rotate, large page margin, small block margin, small padding, small border.
     * Also tests transform across multiple vertical pages.
     */
    @Test
    public void testTransformFloat() throws IOException {
        assertTrue(run("transform-float"));
    }
    
    /**
     * Tests that transform of inline-block renders correctly.
     */
    @Test
    public void testTransformInlineBlock() throws IOException {
        assertTrue(run("transform-inline-block"));
    }
    
    /**
     * Tests a long text transform in left-middle on overflow page.
     * Common case of wanting a strip of vertical text in the left margin.
     */
    @Test
    public void testPageMarginsLongTextTransform() throws IOException {
        assertTrue(run("page-margins-long-text-transform"));
    }
    
    /**
     * Tests a running div with overflow hidden containing a larger replaced text.
     * On two vertical pages and one overflow page. 
     */
    @Test
    public void testRunningOverflowHidden() throws IOException {
        assertTrue(run("running-overflow-hidden"));
    }
    
    /**
     * Tests that an oversized text in a running element does not generate a horizontal overflow page.
     */
    @Test
    public void testRunningOverflowNotGenerated() throws IOException {
        assertTrue(run("running-overflow-not-generated"));
    }
    
    /**
     * Tests that an oversized text in a fixed element does not generate a horizontal overflow page.
     */
    @Test
    public void testFixedOverflowNotGenerated() throws IOException {
        assertTrue(run("fixed-overflow-not-generated"));
    }
    
    /**
     * Tests that fixed position elements are appearing on overflow pages.
     */
    @Test
    public void testFixedOnOverflowPages() throws IOException {
        assertTrue(run("fixed-on-overflow-pages"));
    }
    
    /**
     * Tests that a nested float in a fixed element renders correctly.
     */
    @Test
    public void testFixedNestedFloat() throws IOException {
        assertTrue(run("fixed-nested-float"));
    }
    
    /**
     * Tests that a nested inline-block in a fixed element renders correctly.
     */
    @Test
    public void testFixedNestedInlineBlock() throws IOException {
        assertTrue(run("fixed-nested-inline-block"));
    }
    
    /**
     * Tests that a nested transform in a fixed element renders correctly.
     */
    @Test
    public void testFixedNestedTransform() throws IOException {
        assertTrue(run("fixed-nested-transform"));
    }
    
    /**
     * Tests that hidden overflow works in fixed position elements.
     */
    @Test
    public void testFixedNestedHidden() throws IOException {
        assertTrue(run("fixed-nested-hidden"));
    }
    
    /**
     * Tests that a non-paginated table does not output table header, footer or caption on every page.
     */
    @Test
    public void testTableNonPaginated() throws IOException {
        assertTrue(run("table-non-paginated"));
    }
    
    /**
     * Tests that a paginated table DOES output table header and footer on every page (but caption only on first page).
     */
    @Test
    public void testTablePaginated() throws IOException {
        assertTrue(run("table-paginated"));
    }
    
    /**
     * Tests that a text-only table too wide after auto-layout will generate overflow pages.
     */
    @Test
    public void testTableHorizPageOverflow() throws IOException {
        assertTrue(run("table-horiz-page-overflow"));
    }
    
    /**
     * Tests page and pages counter as well as -fs-if-cut-off function with overflow page.
     */
    @Test
    public void testContentPageNumbers() throws IOException {
        assertTrue(run("content-page-numbers"));
    }
    
    /**
     * Tests a typical table-of-contents setup with leader function, attr function and target-counter function.
     * With overflow page in the middle.
     */
    @Test
    public void testContentTableOfContentsExample() throws IOException {
        assertTrue(run("content-toc-example"));
    }
    
    /**
     * Tests that the table caption position is correct in the case when the table is not 100% width.
     * Issue 340.
     */
    @Test
    @Ignore // Failing because the caption box is set as 100% the width of its container, regardless of the table width.
    public void testTableCaptionPosition() throws IOException {
        assertTrue(run("table-caption-position"));
    }
    
    /**
     * Tests that bi-directional Arabic renders correctly (at least as far as I can compare with browser).
     */
    @Test
    public void testArabicBiDi() throws IOException {
        assertTrue(vtester.runTest("arabic-bidi", TestSupport.WITH_ARABIC));
    }

    /**
     * Tests that list numbers are placed correctly for RTL ol elements.
     * https://github.com/danfickle/openhtmltopdf/pull/655
     */
    @Test
    public void testPr655RtlNumericList() throws IOException {
        assertTrue(vtester.runTest("pr-655-rtl-numeric-list", TestSupport.WITH_ARABIC));
    }

    /**
     * Tests that rtl and bidi with text-align: justify works correctly.
     */
    @Test
    public void testIssue514JustifyRtl() throws IOException {
        assertTrue(vtester.runTest("rtl-justify", TestSupport.WITH_ARABIC));
    }

    /**
     * Tests that letter-spacing property works correctly with bi-directional text.
     * Semi-related to issue 342.
     */
    @Test
    public void testLetterSpacingBidi() throws IOException {
        assertTrue(vtester.runTest("letter-spacing-bidi", TestSupport.WITH_ARABIC));   
    }

    /**
     * Tests that the direction CSS property (as opposed to dir attribute)
     * is honored.
     */
    @Test
    public void testIssue464RtlWithPhoneNumber() throws IOException {
        assertTrue(vtester.runTest("issue-464-rtl-with-phone-number", TestSupport.WITH_ARABIC));
    }

    /**
     * Tests that letter spacing property works correctly with mutliple fallback fonts.
     * Issue 342.
     */
    @Test
    public void testLetterSpacingFallbackFonts() throws IOException {
        assertTrue(vtester.runTest("letter-spacing-fallback-fonts", TestSupport.WITH_EXTRA_FONT));
    }
    
    /**
     * Tests that text-justification works when fallback fonts are being used.
     */
    @Test
    public void testJustificationFallbackFonts() throws IOException {
        assertTrue(vtester.runTest("text-justify-fallback-fonts", TestSupport.WITH_EXTRA_FONT));
    }
    
    /**
     * Tests that justified text doesn't have space at the end of some lines.
     * Issue 351.
     */
    @Test
    public void testJustifySpaceAtEnd() throws IOException {
        assertTrue(vtester.runTest("text-justify-space-at-end", TestSupport.WITH_COLLAPSED_LINE_BREAKER));
    }
    
    /**
     * Tests that soft hyphens used as a line break are converted to visible hyphens.
     * Issue 403.
     */
    @Test
    public void testSoftHyphens() throws IOException {
        assertTrue(vtester.runTest("soft-hyphens", builder -> {
            TestSupport.WITH_COLLAPSED_LINE_BREAKER.configure(builder);
            builder.useFont(new File("target/test/visual-tests/SourceSansPro-Regular.ttf"), "ExtraFont");
        }));
    }
    
    /**
     * Tests that flowing columns containing only text in unbalanced mode
     * are correctly laid out.
     */
    @Test
    public void testColumnsSimpleUnbalanced() throws IOException {
        assertTrue(run("columns-simple-unbalanced"));
    }

    /**
     * Tests columns with nested content such as paragraphs, lists and span.
     */
    @Test
    public void testColumnsNestedUnbalanced() throws IOException {
        assertTrue(run("columns-nested-unbalanced"));
    }
    
    /**
     * Tests columns containing floated and clear elements.
     * Also tests explicit column breaks.
     */
    @Test
    public void testColumnsFloatsUnbalanced() throws IOException {
        assertTrue(run("columns-floats-unbalanced"));
    }

    private static class WatermarkDrawer implements FSObjectDrawer {
        @Override
        public Map<Shape, String> drawObject(Element e, double x, double y, double width, double height,
                OutputDevice outputDevice, RenderingContext ctx, int dotsPerPixel) {
            outputDevice.drawWithGraphics((float) x, (float) y, (float) width / dotsPerPixel,
                (float) height / dotsPerPixel, (Graphics2D g2d) -> {

                double realWidth = width / dotsPerPixel;
                double realHeight = height / dotsPerPixel;

                Font font;
                try {
                    Font parent = Font.createFont(Font.TRUETYPE_FONT, new File("target/test/visual-tests/Karla-Bold.ttf"));
                    font = parent.deriveFont(20f);
                } catch (FontFormatException | IOException e1) {
                    e1.printStackTrace();
                    throw new RuntimeException(e1);
                }

                Rectangle2D bounds = font.getStringBounds("OpenHTMLToPDF", g2d.getFontRenderContext());

                g2d.setFont(font);
                g2d.setPaint(Color.RED);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));

                g2d.drawString("OpenHTMLToPDF",
                   (float) ((realWidth - bounds.getWidth()) / 2),
                   (float) ((realHeight - bounds.getHeight()) / 2));

            });

            return null;
        }
    }

    private static class WatermarkDrawerFactory implements FSObjectDrawerFactory {
        @Override
        public FSObjectDrawer createDrawer(Element e) {
           if (isReplacedObject(e)) {
              return new WatermarkDrawer();
           }
           return null;
        }

        @Override
        public boolean isReplacedObject(Element e) {
           return e.getAttribute("type").equals("watermark");
        }
    }

    /**
     * Tests adding a transparent watermark using a custom object drawer
     * inside a static position object on multiple pages.
     */
    @Test
    public void testIssue472CustomObjectDrawerMultiplePages() throws IOException {
         assertTrue(vtester.runTest("issue-472-custom-object-drawer-multiple-pages", builder -> {
             builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "Karla", 700, FontStyle.NORMAL, true);
             builder.useObjectDrawerFactory(new WatermarkDrawerFactory());
         }));
    }

    /**
     * Tests adding a transparent watermark using a custom object drawer
     * inside a fixed position object.
     */
    @Test
    public void testIssue472AddSemiTransparentWatermark() throws IOException {
         assertTrue(vtester.runTest("issue-472-add-semi-transparent-watermark", builder -> {
             builder.useFont(new File("target/test/visual-tests/Karla-Bold.ttf"), "Karla", 700, FontStyle.NORMAL, true);
             builder.useObjectDrawerFactory(new WatermarkDrawerFactory());
         }));
    }

    /**
     * Tests the ability to use fallback fonts.
     */
    @Test
    public void testIssue641FontFallback() throws IOException {
        assertTrue(vtester.runTest("issue-641-font-fallback", builder -> {
            builder.useFont(
                    new File("target/test/visual-tests/SourceSansPro-Regular.ttf"),
                    "SourceSans", 400, FontStyle.NORMAL, true,
                    EnumSet.of(FSFontUseCase.DOCUMENT));
            builder.useFont(
                    new File("target/test/visual-tests/Karla-Bold.ttf"),
                    "Karla", 700, FontStyle.NORMAL, true,
                    EnumSet.of(FSFontUseCase.FALLBACK_PRE));
        }));
    }

    /**
     * Tests the ability to use fallback fonts via input streams.
     */
    @Test
    public void testIssue641FontFallbackInputStream() throws IOException {
        assertTrue(vtester.runTest("issue-641-font-fallback", builder -> {
            builder.useFont(
                    () -> TextVisualRegressionTest.class.getResourceAsStream("/visualtest/html/fonts/SourceSansPro-Regular.ttf"),
                    "SourceSans", 400, FontStyle.NORMAL, true,
                    EnumSet.of(FSFontUseCase.DOCUMENT));
            builder.useFont(
                    () -> TextVisualRegressionTest.class.getResourceAsStream("/visualtest/html/fonts/Karla-Bold.ttf"),
                    "Karla", 700, FontStyle.NORMAL, true,
                    EnumSet.of(FSFontUseCase.FALLBACK_PRE));
        }));
    }
}

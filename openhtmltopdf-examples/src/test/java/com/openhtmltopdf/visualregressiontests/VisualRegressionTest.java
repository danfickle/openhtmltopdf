package com.openhtmltopdf.visualregressiontests;

import java.io.File;
import static org.junit.Assert.assertTrue;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.openhtmltopdf.latexsupport.LaTeXDOMMutator;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.objects.jfreechart.JFreeChartBarDiagramObjectDrawer;
import com.openhtmltopdf.objects.jfreechart.JFreeChartPieDiagramObjectDrawer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.render.DefaultObjectDrawerFactory;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer.SvgExternalResourceMode;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer.SvgScriptMode;
import com.openhtmltopdf.visualtest.VisualTester;
import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class VisualRegressionTest {
    private VisualTester vt;
    
    @Before
    public void configureTester() {
        File outputDirectory = new File("target/test/visual-tests/test-output/");
        
        outputDirectory.mkdirs();
        
        vt = new VisualTester(
                "/visualtest/html/", /* Resource path. */
                "/visualtest/expected/", /* Expected resource path */
                outputDirectory
                );
    }
    
    private static class WithSvg implements BuilderConfig {
        @Override
        public void configure(PdfRendererBuilder builder) {
            builder.useSVGDrawer(new BatikSVGDrawer());
        }
    }
    
    private static final BuilderConfig WITH_SVG = new WithSvg();
    
    /**
     * Tests z-index property with absolute positioned elements. 
     */
    @Test
    public void testZIndexWithAbsolutePosition() throws IOException {
        assertTrue(vt.runTest("z-index-absolute"));
    }
    
    /**
     * Tests top/right/bottom/left properties with absolute positioned elements.
     */
    @Test
    public void testPositioningAbsolute() throws IOException {
        assertTrue(vt.runTest("positioning-absolute"));
    }
    
    /**
     * Tests that a absolute positioned element with left and right set to 0 and
     * margin-left and margin-right set to auto should be centered horizontally.
     */
    @Ignore // Currently stuck to the left of the containing block rather than centered
    @Test
    public void testAutoMarginCenteringWithPositionAbsolute() throws IOException {
        assertTrue(vt.runTest("auto-margin-centering"));
    }

    /**
     * Tests fixed elements are repeated on each page and top/right/bottom/left properties
     * for fixed position block elements. 
     */
    @Test
    public void testPositioningFixed() throws IOException {
        assertTrue(vt.runTest("positioning-fixed"));
    }    

    /**
     * Tests box-sizing: content-box for static block elements.
     * Includes max/min width properties.
     */
    @Test
    public void testSizingWidthContentBox() throws IOException {
        assertTrue(vt.runTest("sizing-width-content-box"));
    }

    /**
     * Tests box-sizing: border-box for static block elements.
     * Includes max/min width properties.
     */
    @Test
    public void testSizingWidthBorderBox() throws IOException {
        assertTrue(vt.runTest("sizing-width-border-box"));
    }

    /**
     * Tests min/max/height properties with static block elements.
     * Includes both border-box and content-box sizing.
     */
    @Test
    public void testSizingHeight() throws IOException {
        assertTrue(vt.runTest("sizing-height"));
    }
    
    /**
     * Tests overflow:hidden. Containers are static blocks. Overflow content includes
     * static blocks and floats.
     */
    @Test
    public void testOverflow() throws IOException {
        assertTrue(vt.runTest("overflow"));
    }
    
    /**
     * Tests that static blocks overflow onto inserted shadow page. 
     */
    @Test
    public void testHorizPageOverflowStatic() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-static"));
    }
    
    /**
     * Tests that absolute positioned blocks overflow onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowAbsolute() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-absolute"));
    }

    /**
     * Tests that static floated blocks overflow onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowFloat() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-float"));
    }

    /**
     * Tests that non-paginated table columns overflow onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowTable() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-table"));
    }

    /**
     * Tests that paginated table columns (including header and footer) overflow onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowTablePaged() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-table-paged"));
    }
    
    /**
     * Tests that fixed blocks do NOT overflow onto inserted shadow pages.
     */
    @Test
    public void testHorizPageOverflowFixed() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-fixed"));
    }
    
    /**
     * Tests that static inline-blocks overflow onto inserted shadow page.
     */
    @Test
    public void testHorizPageOverflowInlineBlock() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-inline-block"));
    }
    
    /**
     * Tests that overflow:hidden content does NOT generate shadow pages. Includes case where content
     * is absolute block and a case where content is a static block. 
     */
    @Test
    public void testHorizPageOverflowHidden() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-hidden"));
    }
    
    /**
     * Tests that overflow hidden clipping works on generated shadow page. Includes case where content
     * is absolute block and a case where content is a static block. 
     */
    @Test
    public void testHorizPageOverflowHiddenHidden() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-hidden-hidden"));
    }

    /**
     * Tests that content transformed past page edge generates a shadow page.
     */
    @Test
    public void testHorizPageOverflowTransform() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-transform"));
    }
    
    /**
     * Tests that static block content dows not overflow a static block with overflow:hidden.
     */
    @Test
    public void testHiddenStatic() throws IOException {
        assertTrue(vt.runTest("hidden-static"));
    }

    /**
     * Tests that static inline-block content does not overflow a static block with overflow:hidden.
     */
    @Test
    public void testHiddenInlineBlock() throws IOException {
        assertTrue(vt.runTest("hidden-inline-block"));
    }

    /**
     * Tests that a floated block does not overflow a static block with overflow:hidden.
     */
    @Test
    public void testHiddenFloat() throws IOException {
        assertTrue(vt.runTest("hidden-float"));
    }
    
    /**
     * Tests that static block content does not overflow a floater with overflow set to hidden.
     * Similar to {@link #testHiddenBorder()}
     */
    @Test
    public void testHiddenInsideFloat() throws IOException {
        assertTrue(vt.runTest("hidden-inside-float"));
    }
    
    /**
     * Tests that in the case static block :: static block :: static floated block, that the floated
     * element does not overflow its grand parent which has overflow:hidden. 
     */
    @Test
    public void testHiddenGrandchildFloat() throws IOException {
        assertTrue(vt.runTest("hidden-grandchild-float"));
    }

    /**
     * Tests that transformed static blocks do not overflow static block parent with overflow:hidden.
     */
    @Test
    public void testHiddenTransform() throws IOException {
        assertTrue(vt.runTest("hidden-transform"));
    }
    
    /**
     * Tests that absolute block does not overflow relative block parent with overflow:hidden.
     * Issue#273.
     */
    @Test
    public void testHiddenAbsolute() throws IOException {
        assertTrue(vt.runTest("hidden-absolute"));
    }
    
    /**
     * Tests that static inline-blocks too long for the line stack one on top of the other.
     */
    @Test
    public void testInlineBlockStacked() throws IOException {
        assertTrue(vt.runTest("inline-block-stacked"));
    }

    /**
     * Tests that static inline-blocks that can stack from left to right, top to bottom.
     */
    @Test
    public void testInlineBlockInline() throws IOException {
        assertTrue(vt.runTest("inline-block-inline"));
    }

    /**
     * Tests that static inline-block can contain floating static block.
     */
    @Ignore // Float is hidden behind the background-color of inline-block. Perhaps painting order issue?
    @Test
    public void testInlineBlockFloat() throws IOException {
        assertTrue(vt.runTest("inline-block-float"));
    }
    
    /**
     * Tests that relative inline-block can contain absolute positioned block.
     */
    @Test
    public void testInlineBlockAbsolute() throws IOException {
        assertTrue(vt.runTest("inline-block-absolute"));
    }
    
    /**
     * Tests that page-break-inside: avoid works for img elements.
     */
    @Test
    public void testReplacedImgPageBreakInsideAvoid() throws IOException {
        assertTrue(vt.runTest("replaced-img-page-break-inside-avoid"));
    }

    /**
     * Tests that page-break-inside: auto works for img elements.
     */
    @Ignore // Currently img elements are not allowed to split between pages (because they are inline-blocks). Issue#117.
    @Test
    public void testReplacedImgPageBreakInsideAllow() throws IOException {
        assertTrue(vt.runTest("replaced-img-page-break-inside-allow"));
    }
    
    /**
     * Tests that a img inside an absolute positioned element shows up.
     */
    @Test
    public void testReplacedImgInsideAbsolute() throws IOException {
        assertTrue(vt.runTest("replaced-img-inside-absolute"));
    }
    
    /**
     * Tests that page-break-inside: avoid works for svg elements.
     */
    @Test
    public void testReplacedSvgPageBreakInsideAvoid() throws IOException {
        assertTrue(vt.runTest("replaced-svg-page-break-inside-avoid", WITH_SVG));
    }

    /**
     * Tests that page-break-inside: auto works for svg elements.
     */
    @Test
    public void testReplacedSvgPageBreakInsideAllow() throws IOException {
        assertTrue(vt.runTest("replaced-svg-page-break-inside-allow", WITH_SVG));
    }
    
    /**
     * Tests that page-break-inside: avoid works for static inline-block elements.
     */
    @Test
    public void testInlineBlockPageBreakInsideAvoid() throws IOException {
        assertTrue(vt.runTest("inline-block-page-break-inside-avoid"));
    }

    /**
     * Tests that page-break-inside: auto works for static inline-block elements.
     */
    @Ignore // No way currently to specify that inline-block elements can split between pages. Issue#117.
    @Test
    public void testInlineBlockPageBreakInsideAllow() throws IOException {
        assertTrue(vt.runTest("inline-block-page-break-inside-allow"));
    }
    
    /**
     * With a static block, rotate, no page margin, no block margin or padding.
     */
    @Test
    public void testTransformStaticNoPageMargin() throws IOException {
        assertTrue(vt.runTest("transform-static-no-page-margin"));
    }
    
    /**
     * With a static block, rotate, large page margin, no block margin or padding.
     */
    @Test
    public void testTransformStaticPageMargin() throws IOException {
        assertTrue(vt.runTest("transform-static-page-margin"));
    }

    /**
     * With static blocks, rotate then translate, no page margin, no block margin or padding.
     */
    @Test
    public void testTransformWithinTransform() throws IOException {
        assertTrue(vt.runTest("transform-inside-transform"));
    }

    /**
     * With single static block, rotate then translate, small page margin, no block margin or padding.
     */
    @Test
    public void testTransformMultiTranform() throws IOException {
        assertTrue(vt.runTest("transform-multi-transform"));
    }

    /**
     * With a static block, rotate, large page margin, small block margin, no padding.
     */
    @Test
    public void testTransformStaticBlockMargin() throws IOException {
        assertTrue(vt.runTest("transform-static-block-margin"));
    }
    
    /**
     * With a static block, rotate, large page margin, no block margin, small padding.
     */
    @Test
    public void testTransformStaticBlockPadding() throws IOException {
        assertTrue(vt.runTest("transform-static-block-padding"));
    }
    
    /**
     * With an absolute block, rotate, large page margin, small block margin, small padding, small border.
     */
    @Test
    public void testTransformAbsolute() throws IOException {
        assertTrue(vt.runTest("transform-absolute"));
    }

    /**
     * With a floated static block, rotate, large page margin, small block margin, small padding, small border.
     */
    @Test
    public void testTransformFloat() throws IOException {
        assertTrue(vt.runTest("transform-float"));
    }
    
    /**
     * Tests that a floated block inside a transformed element does not escape.
     */
    @Test
    public void testTransformFloatInsideTransform() throws IOException {
        assertTrue(vt.runTest("transform-float-inside-transform"));
    }
    
    /**
     * Tests that two transforms in a row do not impact each other. Issue 259.
     */
    @Test
    public void testTransformConsecutive() throws IOException {
        assertTrue(vt.runTest("transform-consecutive"));
    }
    
    /**
     * Tests that a transformed element does not impact subsequent elements. Issue 260.
     */
    @Test
    public void testTransformSubsequent() throws IOException {
        assertTrue(vt.runTest("transform-subsequent"));
    }
    
    /**
     * Tests that a grandchild inline-block does not overflow its grandparent with overflow hidden.
     */
    @Test
    public void testHiddenGrandchildInlineBlock() throws IOException {
        assertTrue(vt.runTest("hidden-grandchild-inline-block"));
    }
    
    /**
     * Tests that a grandchild absolute does not overflow its grandparent with overflow hidden.
     */
    @Test
    public void testHiddenGrandchildAbsolute() throws IOException {
        assertTrue(vt.runTest("hidden-grandchild-absolute"));
    }
    
    /**
     * Tests that a grandchild absolute overflows its grandparent with overflow hidden,
     * when the grandparent is not the containing block.
     */
    @Test
    public void testHiddenGrandchildAbsoluteEscapes() throws IOException {
        assertTrue(vt.runTest("hidden-grandchild-absolute-escapes"));
    }
    
    /**
     * Tests that a grandchild fixed overflows its grandparent with overflow hidden,
     * because the grandparent is not the containing block, the viewport is, even
     * though the grandparent has position relative.
     */
    @Test
    public void testHiddenGrandchildFixedEscapes() throws IOException {
        assertTrue(vt.runTest("hidden-grandchild-fixed-escapes"));
    }

    /**
     * Tests that overflow hidden inside a transformed element correctly uses
     * the transformed coordinate space.
     */
    @Test
    public void testHiddenInsideTransform() throws IOException {
        assertTrue(vt.runTest("hidden-inside-transform"));
    }
    
    /**
     * Tests a simple color in each margin box cell on two pages.
     */
    @Test
    public void testPageMarginsSimple() throws IOException {
        assertTrue(vt.runTest("page-margins-simple"));
    }
    
    /**
     * Tests that simple page margins work on inserted overflow pages.
     */
    @Test
    public void testPageMarginsHorizPageOverflow() throws IOException {
        assertTrue(vt.runTest("page-margins-horiz-page-overflow"));
    }
    
    /**
     * Tests a simple transform in top-center, bottom-center, left-middle, right-middle.
     */
    @Test
    public void testPageMarginsSimpleTransform() throws IOException {
        assertTrue(vt.runTest("page-margins-simple-transform"));
    }
    
    /**
     * Tests that default overflow visible works.
     */
    @Test
    @Ignore // Margin boxes are currently expanding to fit their content so overflow is impossible to test reliably.
    public void testPageMarginsOverflowVisible() throws IOException {
        assertTrue(vt.runTest("page-margins-overflow-visible"));
    }
    
    /**
     * Tests a long text transform in left-middle. Issue no. 269.
     * Common case of wanting a strip of vertical text in the left margin.
     */
    @Test
    public void testPageMarginsLongTextTransform() throws IOException {
        assertTrue(vt.runTest("page-margins-long-text-transform"));
    }
    
    /**
     * Tests a simple header and footer running blocks with background-color. 
     */
    @Test
    public void testRunningSimple() throws IOException {
        assertTrue(vt.runTest("running-simple"));
    }
    
    /**
     * Tests a simple rotate transform of the running elements themselves. 
     */
    @Test
    public void testRunningTransform() throws IOException {
        assertTrue(vt.runTest("running-transform"));
    }
    
    /**
     * Tests nested transforms inside running elements. 
     */
    @Test
    public void testRunningNestedTransform() throws IOException {
        assertTrue(vt.runTest("running-nested-transform"));
    }
    
    /**
     * Tests nested floats inside running elements. 
     */
    @Test
    public void testRunningNestedFloat() throws IOException {
        assertTrue(vt.runTest("running-nested-float"));
    }

    /**
     * Tests nested list items inside running elements. 
     */
    @Test
    public void testRunningNestedListItems() throws IOException {
        assertTrue(vt.runTest("running-nested-list-items"));
    }
    
    /**
     * Tests a simple replaced img as the running element. 
     */
    @Test
    public void testRunningReplaced() throws IOException {
        assertTrue(vt.runTest("running-replaced"));
    }
    
    /**
     * Tests a replaced img with a transform as the running element. 
     */
    @Test
    public void testRunningReplacedTransform() throws IOException {
        assertTrue(vt.runTest("running-replaced-transform"));
    }
    
    /**
     * Tests a running div with overflow hidden containing a larger replaced img. 
     */
    @Test
    public void testRunningOverflowHidden() throws IOException {
        assertTrue(vt.runTest("running-overflow-hidden"));
    }
    
    /**
     * Tests a running table including table header. 
     */
    @Test
    public void testRunningTable() throws IOException {
        assertTrue(vt.runTest("running-table"));
    }
    
    /**
     * Tests that page border and background-color work.
     */
    @Test
    @Ignore // Leaving a 2px white border around the background-color for some reason.
    public void testPageBorderBackground() throws IOException {
        assertTrue(vt.runTest("page-border-background"));
    }
    
    /**
     * Tests that transforms to the left of the page margin generate an overflow page.
     */
    @Test
    public void testRtlPageOverflowTransform() throws IOException {
        assertTrue(vt.runTest("rtl-page-overflow-transform"));
    }
    
    /**
     * Tests that blocks with negative margin-left generate an overflow page.
     */
    @Test
    public void testRtlPageOverflowStatic() throws IOException {
        assertTrue(vt.runTest("rtl-page-overflow-static"));
    }
    
    /**
     * Tests that blocks with overflow hidden work correctly on overflow pages.
     */
    @Test
    public void testRtlPageOverflowHiddenHidden() throws IOException {
        assertTrue(vt.runTest("rtl-page-overflow-hidden-hidden"));
    }
    
    /**
     * Tests that tds with a background-color do not have fine lines between them. Issues: 291 and 169.
     */
    @Test
    @Ignore // Fine lines appear between tds.
    public void testTableFineLines() throws IOException {
        assertTrue(vt.runTest("table-fine-lines"));
    }
    
    /**
     * Further tests on collapsed table cell borders. Issue 303.
     */
    @Test
    public void testTableCellBorders() throws IOException {
        assertTrue(vt.runTest("table-cell-borders"));
    }
    
    /**
     * Further tests on separated table cell borders.
     */
    @Test
    public void testTableCellBorders2() throws IOException {
        assertTrue(vt.runTest("table-cell-borders-2"));
    }
    
    /**
     * Tests than an inline block, floated, with border, that the border is visible. Issue 297.
     */
    @Test
    public void testHiddenBorder() throws IOException {
        assertTrue(vt.runTest("hidden-border"));
    }
    
    /**
     * Tests that a data uri can be used for fonts. Issue 290.
     */
    @Test
    public void testDataUriFont() throws IOException {
        assertTrue(vt.runTest("data-uri-font"));
    }
    
    /**
     * Tests that an img (with percentage max-width) shows up in an auto width table cell. Issue 313.
     */
    @Test
    @Ignore // img max-width inside table-cell is resolving to zero and therefore image is not inserted.
    public void testReplacedImgInTableCell() throws IOException {
        assertTrue(vt.runTest("replaced-img-in-table-cell"));
    }
    
    /**
     * 1. Tests that an img (with percentage max-width) shows up in an absolute width table cell.
     * 2. Tests that an img (with absolute max-width) shows up correctly sized in an auto width table cell.
     */
    @Test
    public void testReplacedImgInTableCell2() throws IOException {
        assertTrue(vt.runTest("replaced-img-in-table-cell-2"));
    }
    
    /**
     * Tests that large images with smaller max-width and/or max-height are correctly resized.
     * PR number 373.
     */
    @Test
    public void testReplacedImgInTableCell3() throws IOException {
        assertTrue(vt.runTest("replaced-img-in-table-cell-3"));
    }
    
    /**
     * Tests that an image with width and height specified but max-width or max-height
     * takes precedence, that the image will keep its aspect ratio.
     * https://github.com/danfickle/openhtmltopdf/issues/417
     */
    @Test
    public void testIssue417ReplacedSizingWidthHeightWithMax() throws IOException {
        assertTrue(vt.runTest("issue-417-replaced-sizing-width-height-with-max"));
    }
    
    /**
     * Tests that a fixed position element correctly resizes to the sum of its child boxes
     * using border-box sizing.
     */
    @Test
    @Ignore // Fixed position element oversized because it is treating child inline-blocks
            // as using content-box rather than the true border-box sizing.
    public void testFixedExpandsContent() throws IOException {
        assertTrue(vt.runTest("fixed-expands-content"));
    }
    
    /**
     * Tests padding with a percentage value. Issue 145.
     */
    @Test
    @Ignore // Padding with a percentage value is always resolving to zero because it
            // is resolved against a zero width (too early in layout).
    public void testPaddingPercentage() throws IOException {
        assertTrue(vt.runTest("padding-percentage"));
    }
    
    /**
     * Tests that a SVG in a wrapper with no other content and on a named
     * page will not crash. Issue 328.
     */
    @Test
    public void testSvgInWrapperWithNamedPage() throws IOException {
        assertTrue(vt.runTest("svg-in-wrapper-with-named-page", WITH_SVG));
    }
    
    /**
     * Tests that a broken image inside a table cell renders with a zero sized image rather
     * than crashing with a NPE. See issue 336.
     */
    @Test
    public void testBrokenImgInTableCell() throws IOException {
        assertTrue(vt.runTest("broken-img-in-table-cell"));
    }
    
    /**
     * Tests that a broken image inside an inline block does not show and
     * does not crash. See issue 336.
     */
    @Test
    public void testBrokenImgInInlineBlock() throws IOException {
        assertTrue(vt.runTest("broken-img-in-inline-block"));
    }
    
    /**
     * Tests that an image can have display: block and break over two pages.
     */
    @Test
    public void testReplacedImgDisplayBlock() throws IOException {
        assertTrue(vt.runTest("replaced-img-display-block"));
    }
    
    /**
     * Tests various sizing properties for replaced images including box-sizing,
     * min/max, etc.
     */
    @Test
    public void testReplacedSizingImg() throws IOException {
        assertTrue(vt.runTest("replaced-sizing-img"));
    }
    
    /**
     * Tests various sizing properties for replaced SVG images including box-sizing,
     * min/max, etc.
     */
    @Test
    public void testReplacedSizingSvg() throws IOException {
        assertTrue(vt.runTest("replaced-sizing-svg", WITH_SVG));
    }
    
    /**
     * Tests that non-css sizing for SVG works. For example width/height
     * attributes or if not present the last two values of viewBox attribute.
     * Finally, if neither is present, it should default to 400px x 400px.
     */
    @Test
    public void testReplacedSizingSvgNonCss() throws IOException {
        assertTrue(vt.runTest("replaced-sizing-svg-non-css", WITH_SVG));
    }
    
    /**
     * Tests all the CSS sizing properties for MathML elements.
     */
    @Test
    @Ignore // MathML renderer produces slightly different results on JDK11 vs JDK8
            // so can only be run manually.
    public void testReplacedSizingMathMl() throws IOException {
        assertTrue(vt.runTest("replaced-sizing-mathml", (builder) -> {
          builder.useMathMLDrawer(new MathMLDrawer());
        }));
    }

    /**
     * Tests that in the default secure mode, SVG images will not run script or allow
     * fetching of external resources.
     */
    @Test
    public void testMaliciousSvgSecureMode() throws IOException {
        assertTrue(vt.runTest("malicious-svg-secure-mode", WITH_SVG));
    }

    /**
     * Tests that in insecure mode, the svg renderer will allow scripts and external resource
     * requests.
     * 
     * NOTE: This tests downloads <code>https://openhtmltopdf.com/flyingsaucer.png</code> and so will be slower
     * than other tests.
     */
    @Test
    public void testMaliciousSvgInsecureMode() throws IOException {
        assertTrue(vt.runTest("malicious-svg-insecure-mode", builder -> {
            builder.useSVGDrawer(new BatikSVGDrawer(SvgScriptMode.INSECURE_ALLOW_SCRIPTS, SvgExternalResourceMode.INSECURE_ALLOW_EXTERNAL_RESOURCE_REQUESTS));
        }));
    }
    
    /**
     * Tests the Latex support plugin including maths which are interpreted with
     * the MathML plugin.
     */
    @Test
    @Ignore // MathML renderer produces slightly different results on JDK11 vs JDK8
            // so can only be run manually.
    public void testReplacedPluginLatexWithMath() throws IOException {
        assertTrue(vt.runTest("replaced-plugin-latex-with-math", (builder) -> {
            builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
            builder.useMathMLDrawer(new MathMLDrawer());
        }));
    }
    
    /**
     * Tests Latex rendering without math. Separate test because we can not test 
     * Latex with math automatically because of the MathML rendering issue on
     * different JDKs (see above).
     */
    @Test
    public void testReplacedPluginLatex() throws IOException {
        assertTrue(vt.runTest("replaced-plugin-latex", (builder) -> {
            builder.addDOMMutator(LaTeXDOMMutator.INSTANCE);
        }));
    }

    /**
     * Tests that a SVG image referenced from a <code>img</code> tag
     * successfully renders. Also make sure a missing SVG resource
     * does not shutdown rendering altogether. Issue 353.
     */
    @Test
    public void testSvgLinkedFromImgTag() throws IOException {
        assertTrue(vt.runTest("svg-linked-from-img-tag", WITH_SVG));
    }
    
    /**
     * Tests that we correctly render PDF pages in the img tag at 
     * the correct CSS specified sizing. Issue 344.
     */
    @Test
    public void testPdfLinkedFromImgTag() throws IOException {
        assertTrue(vt.runTest("pdf-linked-from-img-tag"));
    }
    
    /**
     * Tests the JFreeChart object drawers respect CSS width and height
     * properties. Other sizing properties are not supported for JFreeChart
     * plugins.
     */
    @Test
    @Ignore // Unlikely to be stable across JDK versions.
    public void testReplacedSizingJFreeChartPie() throws IOException {
        assertTrue(vt.runTest("replaced-sizing-jfreechart-pie", builder -> {
            DefaultObjectDrawerFactory factory = new DefaultObjectDrawerFactory();
            factory.registerDrawer("jfreechart/pie", new JFreeChartPieDiagramObjectDrawer());
            factory.registerDrawer("jfreechart/bar", new JFreeChartBarDiagramObjectDrawer());
            builder.useObjectDrawerFactory(factory);
        }));
    }
    
    /**
     * Tests that a div element with clear: both set actually does clear both.
     */
    @Test
    public void testFloatClearBoth() throws IOException {
        assertTrue(vt.runTest("float-clear-both"));
    }
    
    /**
     * Tests that border radii work. Issue 396.
     */
    @Test
    public void testBorderRadius() throws IOException {
        assertTrue(vt.runTest("border-radius"));
    }

    /**
     * Check counter style after page break, it should not be affected by a previous bolded text, see issue
     * https://github.com/danfickle/openhtmltopdf/issues/366
     */
    @Test
    public void testListCounterAfterPageBreak() throws IOException {
        assertTrue(vt.runTest("list-counter-after-page-break"));
    }

    /**
     * Check the added elements are considered as block by default.
     *
     * See issue: https://github.com/danfickle/openhtmltopdf/issues/382
     *
     * @throws IOException
     */
    @Test
    public void testMissingHtml5BlockElements() throws IOException {
        assertTrue(vt.runTest("html5-missing-block-elements"));
    }

    /**
     * Tests that a paginated table doesn't add header and footer with no rows
     * on a page.
     * https://github.com/danfickle/openhtmltopdf/issues/399
     */
    @Test
    public void testIssue399TableHeaderFooterWithNoRows() throws IOException {
        assertTrue(vt.runTest("issue-399-table-header-with-no-rows"));    
    }
    
    /**
     * Tests that a paginated table pushed to the next page does not have too
     * much height in the first body row and the thead section is not orphaned on
     * the first page.
     * https://github.com/danfickle/openhtmltopdf/issues/202
     */
    @Test
    public void testIssue202PaginatedTableAtStartOfNewPage() throws IOException {
        assertTrue(vt.runTest("issue-202-paginated-table-start-page"));
    }

    /**
     * Tests that a paginated table pushed to the next page does not have too
     * much height in the first body row and the thead section is not orphaned on
     * the first page. With explicit allowing of page breaks in thead.
     * https://github.com/danfickle/openhtmltopdf/issues/202
     */
    @Test
    @Ignore // Has both problems. Low priority as very few people want their thead
            // broken over two or more pages.
    public void testIssue202PaginatedTableAllowBreakThead() throws IOException {
        assertTrue(vt.runTest("issue-202-paginated-table-allow-break-thead"));
    }    
    
    /**
     * Tests that paginated tables with cells which have large border and padding
     * lays out correctly.
     */
    @Test
    public void testPaginatedTableLargeBorderPadding() throws IOException {
        assertTrue(vt.runTest("paginated-table-large-border-padding"));
    }
    
    /**
     * Tests that a paginated table with rows that go over two or more pages
     * lays out correctly.
     */
    @Test
    public void testPaginatedTableMutliPageRow() throws IOException {
        assertTrue(vt.runTest("paginated-table-multi-page-row"));
    }

    /**
     * Tests that justified text with non-justified content (br) nested inside it
     * will not throw a NPE.
     * https://github.com/danfickle/openhtmltopdf/issues/420
     */
    @Test
    public void testIssue420JustifyTextNullPointerException() throws IOException {
        assertTrue(vt.runTest("issue-420-justify-text-null-pointer-exception"));
    }
    
    /**
     * Tests that justified text with non-justified content nested inside it
     * correctly justifies.
     */
    @Test
    public void testIssue420JustifyTextWhiteSpacePre() throws IOException {
        assertTrue(vt.runTest("issue-420-justify-text-white-space-pre"));
    }
    
    /**
     * Tests that in break-word mode, a long word will only be broken if it would not
     * fit on a line by itself.
     * https://github.com/danfickle/openhtmltopdf/issues/429
     */
    @Test
    public void testIssue429BreakWordNested() throws IOException {
        assertTrue(vt.runTest("issue-429-break-word-nested"));
    }

    /**
     * Tests additional problems with break-word such as the first word being too long, etc.
     */
    @Test
    public void testIssue429BreakWordExtra() throws IOException {
        assertTrue(vt.runTest("issue-429-break-word-extra"));
    }

    /**
     * Similar to break word extra but with with text-align: center enabled.
     */
    @Test
    public void testIssue429BreakWordExtraCentered() throws IOException {
        assertTrue(vt.runTest("issue-429-break-word-extra-centered"));
    }

    /**
     * Tests the behavior of very large unbreakable characters with break-word enabled.
     */
    @Test
    public void testIssue429BreakWordLargeChars() throws IOException {
        assertTrue(vt.runTest("issue-429-break-word-large-chars"));
    }
  
    /**
     * Tests break-word in the presence of floats.
     */
    @Test
    @Ignore // If the first too long word is next to a float it will be pushed 
            // below the float rather than be character broken (see chrome for correct display).
            // Otherwise working pretty well.
    public void testIssue429BreakWordWithFloats() throws IOException {
        assertTrue(vt.runTest("issue-429-break-word-with-floats"));
    }

    /**
     * Tests that a line ending with the br tag does not get justified.
     * https://github.com/danfickle/openhtmltopdf/issues/433
     */
    @Test
    public void testIssue433TextJustifyWithBr() throws IOException {
        assertTrue(vt.runTest("issue-433-text-justify-with-br"));
    }

    /**
     * Tests that the lang() selector takes into account any lang set on 
     * ancestor elements.
     */
    @Test
    public void testIssue446LangSelector() throws IOException {
        assertTrue(vt.runTest("issue-446-lang-selector"));
    }

    /**
     * Tests that aligned right text doesn't have trailing spaces
     * that cause ragged text on the right.
     */
    @Test
    public void testIssue440TrailingWsAlignRight() throws IOException {
        assertTrue(vt.runTest("issue-440-trailing-ws-align-right"));
    }

    /**
     * Don't launch a ClassCastException if a td in a table is floated.
     *
     * See issue: https://github.com/danfickle/openhtmltopdf/issues/309
     */
    @Test
    public void testIssue309ClassCastExceptionOnFloatTd() throws IOException {
        assertTrue(vt.runTest("issue-309-classcastexception-on-float-td"));
    }

    /**
     * Tests various linear gradients.
     * https://github.com/danfickle/openhtmltopdf/issues/439
     */
    @Test
    public void testIssue439LinearGradient() throws IOException {
        assertTrue(vt.runTest("issue-439-linear-gradient"));
    }

    /**
     * Tests that a font-face rule with multiple sources in different formats
     * loads the truetype font only.
     */
    @Test
    public void testCssFontFaceRuleAdvanced() throws IOException {
        assertTrue(vt.runTest("css-font-face-rule-advanced"));
    }
    
    /**
     * Tests that a simple font-face rule continues to work.
     */
    @Test
    public void testCssFontFaceRuleSimple() throws IOException {
        assertTrue(vt.runTest("css-font-face-rule-simple"));
    }
    
    /**
     * Tests that a google font import will work (provided that truetype font is included).
     */
    @Test
    @Ignore // Passing manual test - we do not want to rely on google always returning the same thing
            // and network load of font slows down the tests.
    public void testCssFontFaceRuleGoogle() throws IOException {
        assertTrue(vt.runTest("css-font-face-rule-google"));
    }

    // TODO:
    // + Elements that appear just on generated overflow pages.
    // + content property (page counters, etc)
    // + Inline layers.
    // + vertical page overflow, page-break-inside, etc.
    // + CSS columns.
}

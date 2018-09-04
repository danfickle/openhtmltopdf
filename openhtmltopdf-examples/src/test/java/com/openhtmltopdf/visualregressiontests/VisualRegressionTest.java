package com.openhtmltopdf.visualregressiontests;

import java.io.File;
import static org.junit.Assert.assertTrue;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.openhtmltopdf.visualtest.VisualTester;

public class VisualRegressionTest {
    private VisualTester vt;
    
    @Before
    public void configureTester() {
        File overrideDirectory = new File("target/test/visual-tests/user-override/");
        File outputDirectory = new File("target/test/visual-tests/test-output/");
        
        overrideDirectory.mkdirs();
        outputDirectory.mkdirs();
        
        vt = new VisualTester("/visualtest/html/", /* Resource path. */
                new File("src/main/resources/visualtest/expected/"), /* Expected directory */
                overrideDirectory,
                outputDirectory
                );
    }
    
    @Test
    public void testZIndexWithAbsolutePosition() throws IOException {
        assertTrue(vt.runTest("z-index-absolute"));
    }
    
    @Test
    public void testPositioningAbsolute() throws IOException {
        assertTrue(vt.runTest("positioning-absolute"));
    }

    @Test
    public void testPositioningFixed() throws IOException {
        assertTrue(vt.runTest("positioning-fixed"));
    }    

    @Test
    public void testSizingWidthContentBox() throws IOException {
        assertTrue(vt.runTest("sizing-width-content-box"));
    }

    @Test
    public void testSizingWidthBorderBox() throws IOException {
        assertTrue(vt.runTest("sizing-width-border-box"));
    }

    @Test
    public void testSizingHeight() throws IOException {
        assertTrue(vt.runTest("sizing-height"));
    }
    
    @Test
    public void testOverflow() throws IOException {
        assertTrue(vt.runTest("overflow"));
    }
    
    @Test
    public void testHorizPageOverflowStatic() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-static"));
    }
    
    @Test
    public void testHorizPageOverflowAbsolute() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-absolute"));
    }

    @Test
    public void testHorizPageOverflowFloat() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-float"));
    }
    
    @Test
    public void testHorizPageOverflowTable() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-table"));
    }
    
    @Test
    public void testHorizPageOverflowTablePaged() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-table-paged"));
    }
    
    @Test
    public void testHorizPageOverflowFixed() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-fixed"));
    }
    
    @Test
    public void testHorizPageOverflowInlineBlock() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-inline-block"));
    }
    
    @Ignore // Overflow: hidden is generating shadow page and is visible on shadow page.
    @Test
    public void testHorizPageOverflowHidden() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-hidden"));
    }

    @Ignore // Tranforms not working on shadow pages.
    @Test
    public void testHorizPageOverflowTransform() throws IOException {
        assertTrue(vt.runTest("horiz-page-overflow-transform"));
    }
    
    @Test
    public void testHiddenStatic() throws IOException {
        assertTrue(vt.runTest("hidden-static"));
    }

    @Test
    public void testHiddenInlineBlock() throws IOException {
        assertTrue(vt.runTest("hidden-inline-block"));
    }

    @Test
    public void testHiddenFloat() throws IOException {
        assertTrue(vt.runTest("hidden-float"));
    }

    @Ignore // Transformed elements escaping overflow:hidden on their containing block.
    @Test
    public void testHiddenTransform() throws IOException {
        assertTrue(vt.runTest("hidden-transform"));
    }
    
    @Ignore // Positioned elements escaping overflow:hidden on their containing block. Issue#273.
    @Test
    public void testHiddenAbsolute() throws IOException {
        assertTrue(vt.runTest("hidden-absolute"));
    }
}

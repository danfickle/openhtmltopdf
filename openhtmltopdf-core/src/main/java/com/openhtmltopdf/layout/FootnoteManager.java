package com.openhtmltopdf.layout;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.EmptyStyle;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.util.BoxUtil;
import com.openhtmltopdf.util.OpenUtil;

/**
 * The footnote manager which is only created if footnotes are detected in
 * the document. See {@link LayoutContext#getFootnoteManager()}
 */
public class FootnoteManager {

    private static class FootnoteArea {
        // The container for one or more footnotes belonging to a page.
        BlockBox footnoteArea;

        // The pages this footnote area covers, usually a singleton list
        // so may not be mutable.
        List<PageBox> pages;
    }

    // Every page has zero or one footnote areas but a footnote area may go
    // over several pages.
    private final Map<PageBox, FootnoteArea> _footnoteAreas = new HashMap<>();

    // Map from a footnote body back to its footnote area.
    private final Map<BlockBox, FootnoteArea> _containerMap = new HashMap<>();

    private FootnoteArea createFootnoteArea(LayoutContext c, PageBox page) {
        FootnoteArea area = new FootnoteArea();
        area.footnoteArea = createFootnoteAreaBlock(c, page);
        return area;
    }

    /**
     * Sets up the footnote area.
     */
    private BlockBox createFootnoteAreaBlock(LayoutContext c, PageBox footnoteCallPage) {
        // Create style from @footnote page rules with absolute
        // positioning and block display.
        CalculatedStyle style = new EmptyStyle().deriveStyle(
                footnoteCallPage.getPageInfo().createFootnoteAreaStyle());

        Box rootBox = c.getRootLayer().getMaster();
        Element root = rootBox.getElement();

        // We get a NPE if our new BlockBox doesn't have an element
        // so just create one.
        Element me = root.getOwnerDocument().createElement("fs-footnote");
        Element body = BoxUtil.getBodyElementOrSomething(root);

        body.appendChild(me);

        Box bodyBox = BoxUtil.getBodyBoxOrSomething(c.getRootLayer().getMaster());
        int containingBlockWidth = OpenUtil.firstNonZero(
                         bodyBox.getContentWidth(), bodyBox.getWidth(),
                         rootBox.getContentWidth(), rootBox.getWidth(),
                         footnoteCallPage.getContentWidth(c), footnoteCallPage.getWidth(c));

        BlockBox footnoteArea = new BlockBox();
        footnoteArea.setContainingBlock(new ViewportBox(new Rectangle(0, 0, containingBlockWidth, 0)));
        footnoteArea.setStyle(style);

        // For now we make sure all footnote bodies have block display.
        footnoteArea.setChildrenContentType(BlockBox.ContentType.BLOCK);
        footnoteArea.setElement(me);

        // Create our layer as a child of root layer.
        Layer footnoteLayer = new Layer(c.getRootLayer(), footnoteArea, c);
        footnoteArea.setLayer(footnoteLayer);
        footnoteArea.setContainingLayer(footnoteLayer);

        return footnoteArea;
    }

    private void positionFootnoteArea(LayoutContext c, FootnoteArea area, PageBox firstPage, int lineHeight) {
        final int minLines = 2;

        if (area.pages != null) {
            // We clear any page footnote area info as
            // they may have changed and are recreated.
            for (PageBox page : area.pages) {
                page.setFootnoteAreaHeight(0);
                _footnoteAreas.remove(page);
            }

            area.pages = null;
        }

        int desiredHeight = area.footnoteArea.getBorderBoxHeight(c);
        int pageTop = firstPage.getTop();

        int minFootnoteTop = pageTop + (lineHeight * minLines);
        int maxFootnoteHeight = firstPage.getBottom() - minFootnoteTop;

        int firstPageHeight = Math.min(
                maxFootnoteHeight,
                desiredHeight);

        if (firstPageHeight < desiredHeight) {
            // Goes over multiple pages.
            area.pages = c.getRootLayer().getPages(c, minFootnoteTop, minFootnoteTop + desiredHeight);

            for (PageBox page : area.pages) {
                // Reserve entire page for footnotes. This is
                // overridden for first page below.
                page.setFootnoteAreaHeight(page.getContentHeight(c));
                _footnoteAreas.put(page, area);
            }
        } else {
            area.pages = Collections.singletonList(firstPage);
            _footnoteAreas.put(firstPage, area);
        }

        firstPage.setFootnoteAreaHeight(firstPageHeight);

        int x = 0;
        Box body = BoxUtil.getBodyOrNull(c.getRootLayer().getMaster());

        if (body != null) {
            x = body.getMarginBorderPadding(c, CalculatedStyle.LEFT);
        }

        area.footnoteArea.setAbsX(x);
        area.footnoteArea.setAbsY(firstPage.getBottom() - firstPageHeight);
        area.footnoteArea.calcChildLocations();
    }

    private void layoutFootnoteArea(LayoutContext c, BlockBox footnoteArea) {
        footnoteArea.layout(c);
    }

    /**
     * Adds a footnote body to the line box page, creating the footnote area as required.
     * <br><br>
     * Important: This changes the page break point by expanding the footnote area.
     * It also reserves subsequent pages if required for the footnote area.
     * <br><br>
     * If the line box is moved to a different page, one must call
     * {@link #removeFootnoteBodies(LayoutContext, List, LineBox)} before the page change
     * and this method again after.
     **/
    public void addFootnoteBody(LayoutContext c, BlockBox footnoteBody, LineBox line) {
        PageBox page = c.getRootLayer().getFirstPage(c, line);
        FootnoteArea footnote = _footnoteAreas.get(page);

        // We need to know that we are in the footnote area during layout so we
        // know which page bottom to use.
        c.setIsInFloatBottom(true);

        boolean createdArea = false;

        if (footnote == null) {
            footnote = createFootnoteArea(c, page);
            createdArea = true;
        }

        footnoteBody.setContainingBlock(footnote.footnoteArea.getContainingBlock());
        footnote.footnoteArea.addChild(footnoteBody);
        _containerMap.put(footnoteBody, footnote);

        if (!createdArea) {
            footnote.footnoteArea.reset(c);
        } else {
            footnoteBody.reset(c);
        }

        // FIXME: Not very efficient to layout all footnotes again after one
        // is added.
        layoutFootnoteArea(c, footnote.footnoteArea);
        positionFootnoteArea(c, footnote, page, line.getHeight());

        c.setIsInFloatBottom(false);
    }

    /**
     * Removes footnotes. This is used when a line is moved to a
     * new page. We remove from first page and add to the next.
     */
    public void removeFootnoteBodies(
            LayoutContext c, List<BlockBox> footnoteBodies, LineBox line) {

        for (BlockBox body : footnoteBodies) {
            FootnoteArea area = _containerMap.get(body);

            area.footnoteArea.removeChild(body);
            _containerMap.remove(body);

            if (area.footnoteArea.getChildCount() > 0) {
                c.setIsInFloatBottom(true);

                PageBox page = c.getRootLayer().getFirstPage(c, line);

                area.footnoteArea.reset(c);
                layoutFootnoteArea(c, area.footnoteArea);
                positionFootnoteArea(c, area, page, line.getHeight());

                c.setIsInFloatBottom(false);
            } else {
                if (area.footnoteArea.getLayer() != null) {
                    area.footnoteArea.getLayer().detach();
                }

                if (area.pages != null) {
                    for (PageBox page : area.pages) {
                        page.setFootnoteAreaHeight(0);
                        _footnoteAreas.remove(page);
                    }

                    area.pages = null;
                }
            }
        }
    }

}

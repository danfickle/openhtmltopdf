package com.openhtmltopdf.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.layout.LayoutContext;

public class FlowingColumnContainerBox extends BlockBox {
    private FlowingColumnBox _child;

    // FIXME: Inefficient, replace with binary search.
    private int findPageIndex(List<PageBox> pages, int y) {
        int idx = 0;
        for (PageBox page : pages) {
            if (y >= page.getTop() && y <= page.getBottom()) {
                return idx;
            }
            idx++;
        }
        return idx - 1;
    }

    private static class ColumnPosition {
        private final int copyY;  // Absolute, What y position starts the column in the long column block.
        private final int pasteY; // Absolute, What y position starts the column in the flowing column block for
                                  // final render.
        private final int maxColHeight; // Absolute, Maximum height of the column.
        private final int pageIdx;

        private ColumnPosition(int copyY, int pasteY, int maxColHeight, int pageIdx) {
            this.copyY = copyY;
            this.pasteY = pasteY;
            this.maxColHeight = maxColHeight;
            this.pageIdx = pageIdx;
        }
    }
    
    public static class ColumnBreakOpportunity {
        private final Box box;             // The box where we can break.
        private final List<Box> ancestors; // Ancestors of this box which should be moved with it.

        private ColumnBreakOpportunity(Box box, List<Box> ancestors) {
            this.box = box;
            this.ancestors = ancestors;
        }
        
        static ColumnBreakOpportunity of(Box box, List<Box> ancestors) {
            return new ColumnBreakOpportunity(box, ancestors);
        }
        
        @Override
        public String toString() {
            return String.valueOf(box);
        }
    }
    
    public static class ColumnBreakStore {
        // Break opportunity boxes.
        private final List<ColumnBreakOpportunity> breaks = new ArrayList<>();
        // Which container boxes have been processed, so we don't move them twice.
        private final Set<Box> processedContainers = new HashSet<>();
        
        /**
         * Add a break opportunity. If this is a break opportunity and a first child, it 
         * should also add all unprocessed ancestors, so they can be moved with the
         * first child.
         */
        public void addBreak(Box box, List<Box> ancestors) {
            breaks.add(ColumnBreakOpportunity.of(box, ancestors));
        }
        
        /**
         * Whether an ancestor box needs to be added to the list of ancestors.
         * @return true to process this ancestor (we haven't seen it yet).
         */
        public boolean checkContainerShouldProcess(Box container) {
            if (container instanceof FlowingColumnContainerBox ||
                container instanceof FlowingColumnBox) {
                return false;
            }
            
            return processedContainers.add(container);
        }

        @Override
        public String toString() {
            return breaks.toString();
        }
    }
    
    private int adjustUnbalanced(LayoutContext c, Box child, int colGap, int colWidth, int columnCount, int xStart) {
        // At the start of this method we have one long column in child.
        // This method works by going through the boxes and adjusting their position
        // into the current column.
        
        final int startY = this.getAbsY();
        final List<PageBox> pages = c.getRootLayer().getPages();
        
        // These are all running values that change as we layout our boxes into columns.
        int pageIdx      = findPageIndex(pages, startY);
        int colStart     = startY;
        int colHeight    = pages.get(pageIdx).getBottom() - this.getChild().getAbsY();
        int colIdx       = 0;
        int finalHeight  = 0;

        if (child.getHeight() <= colHeight) {
            // We fit in the first column.
            return child.getHeight();
        }
        
        // Recursively find all the column break opportunities (typically line boxes).
        ColumnBreakStore store = new ColumnBreakStore();
        child.findColumnBreakOpportunities(store);
        
        if (store.breaks.isEmpty() || store.breaks.size() == 1) {
            // Nothing we can do except overflow.
            // The only break is at the start of the first child.
            return this.getChild().getHeight();
        }

        // Add our first column.
        ColumnPosition current = new ColumnPosition(/* copy-from */ colStart, /* copy-to */ colStart, colHeight, pageIdx);
         
        Collections.sort(store.breaks, 
                Comparator.comparingInt(brk -> brk.box.getAbsY() + brk.box.getHeight()));
        
        for (int i = 0; i < store.breaks.size(); i++) {
            ColumnBreakOpportunity br = store.breaks.get(i);
            ColumnBreakOpportunity nextBr = i < store.breaks.size() - 1 ? store.breaks.get(i + 1) : null;
            Box ch = br.box;

            int yAdjust = current.pasteY - current.copyY;
            int yProposedFinal = ch.getAbsY() + yAdjust;
            ch.setAbsY(yProposedFinal);

            // We need the max height of the column which is the bottom of the current box
            // minus the top of the column.
            finalHeight = Math.max((yProposedFinal + ch.getHeight()) - current.pasteY, finalHeight);

            // x position should be easy.
            int xAdjust = ((colIdx % columnCount) * colWidth) + ((colIdx % columnCount) * colGap);
            ch.setAbsX(ch.getAbsX() + xAdjust);

            if (br.ancestors != null) {
                // We move container ancestors with the first child that is
                // a break opportunity.
                // EXAMPLE: column box -> p -> ul -> li -> line box
                // We would move the p, ul and li on the first line of the first li.
                // For the second li we only have to move the parent li as p and ul have
                // already been processed.
                for (Box ancestor : br.ancestors) {
                    ancestor.setAbsY(ancestor.getAbsY() + yAdjust);
                    ancestor.setAbsX(ancestor.getAbsX() + xAdjust);
                }
                // FIXME: We do not resize or duplicate ancestor container boxes,
                // so if user has used border, background color
                // or overflow: hidden it will produce incorrect results.
            }
            
            if (ch instanceof LineBox) {
                // We do not call this on other kind of boxes as it would undo our work in moving them.
                ch.calcChildLocations();
            }
 
            if (nextBr != null) {
                Box next = nextBr.box;
                int nextYHeight = next.getAbsY() + yAdjust + next.getHeight() - current.pasteY;
                
                if (nextYHeight > current.maxColHeight) {
                    // We have moved past the bottom of the current column.
                    // Time for a new column.
                    // FIXME: What if box doesn't fit in new column either?
                    int newColIdx = colIdx + 1;
                
                    // And possibly a new page.
                    boolean needNewPage = newColIdx % columnCount == 0;
                    int newPageIdx = needNewPage ? current.pageIdx + 1 : current.pageIdx;

                    if (newPageIdx >= pages.size()) {
                        c.getRootLayer().addPage(c);
                    }

                    // We need the y top of the new column.
                    PageBox page = pages.get(newPageIdx);
                    int pasteY = needNewPage ? page.getTop() : current.pasteY;
                    int copyY  = next.getAbsY();
                    
                    current = new ColumnPosition(copyY, pasteY, page.getBottom() - pasteY, newPageIdx);
                    colIdx++;
                }
            }
        }
        
        return finalHeight;
    }

    @Override
    public void layout(LayoutContext c, int contentStart) {
        this.calcDimensions(c);

        int colCount = getStyle().columnCount();
        int colGapCount = colCount - 1;

        float colGap = getStyle().isIdent(CSSName.COLUMN_GAP, IdentValue.NORMAL) ? getStyle().getLineHeight(c)
                : /* Use the line height as a normal column gap. */
                getStyle().getFloatPropertyProportionalWidth(CSSName.COLUMN_GAP, getContentWidth(), c);

        float totalGap = colGap * colGapCount;
        int colWidth = (int) ((this.getContentWidth() - totalGap) / colCount);

        _child.setContainingLayer(this.getContainingLayer());
        _child.setContentWidth(colWidth);
        _child.setColumnWidth(colWidth);
        _child.setAbsX(this.getAbsX());
        _child.setAbsY(this.getAbsY());

        c.setIsPrintOverride(false);
        _child.layout(c, contentStart);
        c.setIsPrintOverride(null);

        int height = adjustUnbalanced(c, _child, (int) colGap, colWidth, colCount, this.getLeftMBP() + this.getX());
        _child.setHeight(0);
        this.setHeight(height);
    }

    public void setOnlyChild(LayoutContext c, FlowingColumnBox child) {
        this._child = child;
        this.addChild(child);
    }

    public FlowingColumnBox getChild() {
        return _child;
    }
}

package com.openhtmltopdf.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.layout.BlockFormattingContext;
import com.openhtmltopdf.layout.FloatManager;
import com.openhtmltopdf.layout.FloatManager.BoxOffset;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.PersistentBFC;

public class FlowingColumnContainerBox extends BlockBox {
    private FlowingColumnBox _child;

    private int findPageIndex(LayoutContext c, int y) {
        return c.getRootLayer().getPageIndex(y);
    }

    private static class ColumnPosition {
        private final int columnIndex;
        private final int copyY;  // Absolute, What y position starts the column in the long column block.
        private final int pasteY; // Absolute, What y position starts the column in the flowing column block for
                                  // final render.
        private final int maxColHeight; // Absolute, Maximum height of the column.
        private final int pageIdx;

        private ColumnPosition(int columnIndex, int copyY, int pasteY, int maxColHeight, int pageIdx) {
            this.columnIndex = columnIndex;
            this.copyY = copyY;
            this.pasteY = pasteY;
            this.maxColHeight = maxColHeight;
            this.pageIdx = pageIdx;
        }
        
        @Override
        public String toString() {
            return String.format("[index='%d', copyY='%d', pasteY='%d', maxColHeight='%d', pageIdx='%d']",
                           columnIndex, copyY, pasteY, maxColHeight, pageIdx);
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
    
    private void layoutFloats(TreeMap<Integer, ColumnPosition> columns, List<BoxOffset> floats, int columnCount, int colWidth, int colGap) {
        for (BoxOffset bo : floats) {
            BlockBox floater = bo.getBox();
            
            ColumnBreakStore store = new ColumnBreakStore();
            floater.findColumnBreakOpportunities(store);
            
            for (ColumnBreakOpportunity breakOp : store.breaks) {
                Map.Entry<Integer, ColumnPosition> entry = columns.floorEntry(breakOp.box.getAbsY());
                ColumnPosition column = entry.getValue();
            
                int yAdjust = column.pasteY - column.copyY;
                int xAdjust = ((column.columnIndex % columnCount) * colWidth) + ((column.columnIndex % columnCount) * colGap);

                reposition(breakOp.box, xAdjust, yAdjust);
                
                if (breakOp.ancestors != null) {
                    repositionAncestors(breakOp.ancestors, xAdjust, yAdjust);
                }
                
                if (breakOp.box instanceof LineBox) {
                    breakOp.box.calcChildLocations();
                }
            }
        }
    }

    private void layoutFloats(TreeMap<Integer, ColumnPosition> columnMap, PersistentBFC bfc, int columnCount, int colWidth, int colGap) {
        List<BoxOffset> floatsL = this.getPersistentBFC().getFloatManager().getFloats(FloatManager.FloatDirection.LEFT);
        List<BoxOffset> floatsR = this.getPersistentBFC().getFloatManager().getFloats(FloatManager.FloatDirection.RIGHT);

        layoutFloats(columnMap, floatsL, columnCount, colWidth, colGap);
        layoutFloats(columnMap, floatsR, columnCount, colWidth, colGap);
    }
    
    private void reposition(Box box, int xAdjust, int yAdjust) {
        if (box instanceof BlockBox &&
            ((BlockBox) box).isFloated()) {
            box.setX(box.getX() + xAdjust);
            box.setY(box.getY() + yAdjust);
        } else {
            box.setAbsY(box.getAbsY() + yAdjust);
            box.setAbsX(box.getAbsX() + xAdjust);
        }
    }
    
    private void repositionAncestors(List<Box> ancestors, int xAdjust, int yAdjust) {
        for (Box ancestor : ancestors) {
            reposition(ancestor, xAdjust, yAdjust);
        }

        // FIXME: We do not resize or duplicate ancestor container boxes,
        // so if user has used border, background color
        // or overflow: hidden it will produce incorrect results.
    }
    
    private int adjustUnbalanced(LayoutContext c, Box child, int colGap, int colWidth, int columnCount, int xStart) {
        // At the start of this method we have one long column in child.
        // This method works by going through the boxes and adjusting their position
        // into the current column.
        
        final int startY = this.getAbsY();
        final List<PageBox> pages = c.getRootLayer().getPages();
        
        final boolean haveFloats = 
                !this.getPersistentBFC().getFloatManager().getFloats(FloatManager.FloatDirection.LEFT).isEmpty() ||
                !this.getPersistentBFC().getFloatManager().getFloats(FloatManager.FloatDirection.RIGHT).isEmpty();
        
        // We only need the tree map if we have floats.
        final TreeMap<Integer, ColumnPosition> columnMap = haveFloats ? new TreeMap<>() : null;
        
        // These are all running values that change as we layout our boxes into columns.
        int pageIdx      = findPageIndex(c, startY);
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
        ColumnPosition current = new ColumnPosition(colIdx, /* copy-from */ colStart, /* copy-to */ colStart, colHeight, pageIdx);
        if (haveFloats) {
            columnMap.put(colStart, current);
        }
         
        // FIXME: Don't sort if we have in order - common case.
        Collections.sort(store.breaks, 
                Comparator.comparingInt(brk -> brk.box.getAbsY() + brk.box.getBorderBoxHeight(c)));
        
        for (int i = 0; i < store.breaks.size(); i++) {
            ColumnBreakOpportunity br = store.breaks.get(i);
            ColumnBreakOpportunity nextBr = i < store.breaks.size() - 1 ? store.breaks.get(i + 1) : null;
            Box ch = br.box;

            int yAdjust = current.pasteY - current.copyY;
            int yProposedFinal = ch.getAbsY() + yAdjust;
            ch.setAbsY(yProposedFinal);

            // We need the max height of the column which is the bottom of the current box
            // minus the top of the column.
            finalHeight = Math.max((yProposedFinal + ch.getBorderBoxHeight(c)) - startY, finalHeight);

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
                repositionAncestors(br.ancestors, xAdjust, yAdjust);
            }
            
            if (ch instanceof LineBox) {
                // We do not call this on other kind of boxes as it would undo our work in moving them.
                ch.calcChildLocations();
            }
 
            if (nextBr != null) {
                Box next = nextBr.box;
                int nextYHeight = next.getAbsY() + yAdjust + next.getBorderBoxHeight(c) - current.pasteY;
                
                if (nextYHeight > current.maxColHeight ||
                    ch.getStyle().isColumnBreakAfter() ||
                    next.getStyle().isColumnBreakBefore()) {
                    // We have moved past the bottom of the current column (or explicit break).
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
                    
                    current = new ColumnPosition(newColIdx, copyY, pasteY, page.getBottom() - pasteY, newPageIdx);
                    if (haveFloats) {
                        columnMap.put(copyY, current);
                    }
                    colIdx++;
                }
            }
        }
        
        if (haveFloats) {
            layoutFloats(columnMap, this.getPersistentBFC(), columnCount, colWidth, colGap);
        }

        return finalHeight;
    }

    @Override
    public void layout(LayoutContext c, int contentStart) {
        BlockFormattingContext bfc = new BlockFormattingContext(this, c);
        c.pushBFC(bfc);
        
        addBoxID(c);
        
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
        c.popBFC();
    }

    public void setOnlyChild(LayoutContext c, FlowingColumnBox child) {
        this._child = child;
        this.addChild(child);
    }

    public FlowingColumnBox getChild() {
        return _child;
    }
}

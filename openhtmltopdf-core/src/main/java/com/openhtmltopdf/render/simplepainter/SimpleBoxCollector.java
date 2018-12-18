package com.openhtmltopdf.render.simplepainter;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.newtable.TableSectionBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.OperatorClip;
import com.openhtmltopdf.render.OperatorSetClip;
import com.openhtmltopdf.render.RenderingContext;

public class SimpleBoxCollector {
    private List<DisplayListItem> _blocks = null;
    private List<DisplayListItem> _inlines = null;
    private List<TableCellBox> _tcells = null;
    private List<DisplayListItem> _replaceds = null;
    private List<DisplayListItem> _listItems = null;
    
    private boolean _hasListItems = false;
    private boolean _hasReplaceds = false;
    
    private void addBlock(DisplayListItem block) {
        if (_blocks == null) {
            _blocks = new ArrayList<DisplayListItem>();
        }
        _blocks.add(block);
    }
    
    private void addInline(DisplayListItem inline) {
        if (_inlines == null) {
            _inlines = new ArrayList<DisplayListItem>();
        }
        _inlines.add(inline);
    }
    
    private void addTableCell(TableCellBox tcell) {
        if (_tcells == null) {
            _tcells = new ArrayList<TableCellBox>();
        }
        _tcells.add(tcell);
    }
    
    private void addReplaced(DisplayListItem replaced) {
        if (_replaceds == null) {
            _replaceds = new ArrayList<DisplayListItem>();
        }
        _replaceds.add(replaced);
        
        if (!(replaced instanceof OperatorClip) &&
            !(replaced instanceof OperatorSetClip)) {
            _hasReplaceds = true;
        }
    }
    
    private void addListItem(DisplayListItem listItem) {
        if (_listItems == null) {
            _listItems = new ArrayList<DisplayListItem>();
        }
        _listItems.add(listItem);
        
        if (!(listItem instanceof OperatorClip) &&
            !(listItem instanceof OperatorSetClip)) {
            _hasListItems = true;
        }
    }
    
    private void clipAll(OperatorClip dli) {
        addBlock(dli);
        addInline(dli);
        addReplaced(dli);
        addListItem(dli);
    }
    
    private void setClipAll(OperatorSetClip dli) {
        addBlock(dli);
        addInline(dli);
        addReplaced(dli);
        addListItem(dli);
    }
    
    public List<DisplayListItem> blocks() {
        return this._blocks == null ? Collections.<DisplayListItem>emptyList() : this._blocks;
    }
    
    public List<DisplayListItem> inlines() {
        return this._inlines == null ? Collections.<DisplayListItem>emptyList() : this._inlines;
    }
    
    public List<TableCellBox> tcells() {
        return this._tcells == null ? Collections.<TableCellBox>emptyList() : this._tcells;
    }
    
    public List<DisplayListItem> replaceds() {
        return this._hasReplaceds ? this._replaceds : Collections.<DisplayListItem>emptyList();
    }
    
    public List<DisplayListItem> listItems() {
        return this._hasListItems ? this._listItems : Collections.<DisplayListItem>emptyList();
    }
    
    /**
     * Adds block box to appropriate flat box lists.
     */
    private boolean addBlockToLists(RenderingContext c, Layer layer, Box container, Shape ourClip) {
        addBlock(container);
        
        if (container instanceof BlockBox) {
            BlockBox block = (BlockBox) container;
            
            if (block.getReplacedElement() != null) {
                addReplaced(block);
            }
            
            if (block.isListItem()) {
                addListItem(block);
            }
        }
        
        if (container instanceof TableCellBox &&
            ((TableCellBox) container).hasCollapsedPaintingBorder()) {
            addTableCell((TableCellBox) container);
        }
        
        if (ourClip != null) {
            clipAll(new OperatorClip(ourClip));
            return true;
        }
        
        return false;
    }
    
    public void collect(RenderingContext c, Layer layer) {
        if (layer.isInline()) {
            collectInline(c, layer);
        } else {
            collect(c, layer, layer.getMaster());
        }
    }
    
    private void collectInline(RenderingContext c, Layer layer) {
        // TODO Auto-generated method stub
        
    }

    public void collect(RenderingContext c, Layer layer, Box container) {
        if (layer != container.getContainingLayer()) {
            // Different layers are responsible for their own box collection.
            return;
        }

        if (container instanceof LineBox) {
            addLineBox(c, layer, (LineBox) container);
        } else {
            Shape ourClip = null;
            boolean pushedClip = false;
            
            if (container.getLayer() == null ||
                layer.getMaster() == container ||
                !(container instanceof BlockBox)) {
        
                if (c instanceof RenderingContext &&
                    container instanceof BlockBox) {

                    BlockBox block = (BlockBox) container;
                    
                    if (block.isNeedsClipOnPaint((RenderingContext) c)) {
                        // A box with overflow set to hidden.
                        ourClip = block.getChildrenClipEdge((RenderingContext) c);
                    }
                }
                
                pushedClip = addBlockToLists(c, layer, container, ourClip);
            }

            if (container instanceof TableSectionBox &&
                (((TableSectionBox) container).isHeader() || ((TableSectionBox) container).isFooter()) &&
                ((TableSectionBox) container).getTable().hasContentLimitContainer() &&
                (container.getLayer() == null || container == layer.getMaster()) &&
                c instanceof RenderingContext) {
                // TODO
                //addTableHeaderFooter(c, layer, container);
            } else {
                // Recursively, process all children and their children.
                if (container.getLayer() == null || container == layer.getMaster()) {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        Box child = container.getChild(i);
                        collect(c, layer, child);
                    }
                }
            }
            
            if (pushedClip) {
                setClipAll(new OperatorSetClip(null));
            }
        }
    }

    private void addLineBox(RenderingContext c, Layer layer, LineBox container) {
        addInline(container);

        // Recursively add all children of the line box to the inlines list.
        container.addAllChildren(this._inlines, layer);
    }
}

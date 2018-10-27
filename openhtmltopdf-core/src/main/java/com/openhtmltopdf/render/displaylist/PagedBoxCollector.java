package com.openhtmltopdf.render.displaylist;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.PaintingInfo;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.newtable.TableSectionBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.OperatorClip;
import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.InlineLayoutBox;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.OperatorSetClip;

public class PagedBoxCollector {

	public static class PageResult {
		private List<DisplayListItem> _blocks = null;
		private List<DisplayListItem> _inlines = null;
		private List<TableCellBox> _tcells = null;
		private List<DisplayListItem> _replaceds = null;
		private List<DisplayListItem> _listItems = null;
		private List<BlockBox> _floats = null;
		private List<PageResult> _shadowPages = null;
		private boolean _hasListItems = false;
		private boolean _hasReplaceds = false;
		private Rectangle _contentWindowOnDocument = null;
        private Rectangle _firstShadowPageContentWindowOnDocument = null;

        private void addShadowPage(PageResult shadowPage) {
            if (_shadowPages == null) {
                _shadowPages = new ArrayList<PageResult>();
            }
            _shadowPages.add(shadowPage);
        }
		
		private void addFloat(BlockBox floater) {
		    if (_floats == null) {
		        _floats = new ArrayList<BlockBox>();
		    }
		    _floats.add(floater);
		}
		
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
		
		public List<BlockBox> floats() {
		    return this._floats == null ? Collections.<BlockBox>emptyList() : this._floats;
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
		
		public List<PageResult> shadowPages() {
		    return this._shadowPages == null ? Collections.<PageResult>emptyList() : this._shadowPages;
		}
		
		public boolean hasShadowPage(int shadow) {
		    return this._shadowPages != null && shadow < this._shadowPages.size();
		}
		
		private Rectangle getContentWindowOnDocument(PageBox page, CssContext c) {
			if (_contentWindowOnDocument == null) {
				_contentWindowOnDocument = page.getDocumentCoordinatesContentBounds(c);
			}
			return _contentWindowOnDocument;
		}

        public Rectangle getShadowWindowOnDocument(PageBox page, CssContext c, int shadowPageNumber) {
            if (shadowPageNumber == 0 && _firstShadowPageContentWindowOnDocument == null) {
                _firstShadowPageContentWindowOnDocument = page.getDocumentCoordinatesContentBoundsForInsertedPage(c, shadowPageNumber);
                return _firstShadowPageContentWindowOnDocument;
            } else if (shadowPageNumber == 0) {
                return _firstShadowPageContentWindowOnDocument;
            } else {
                return page.getDocumentCoordinatesContentBoundsForInsertedPage(c, shadowPageNumber);
            }
        }
	}
	
	public static class PageFinder {
		private int lastRequested = 0;
		private final List<PageBox> pages;
		private static final int PAGE_BEFORE_START = -1;
		private static final int PAGE_AFTER_END = -2;
		
		public PageFinder(List<PageBox> pages) {
			this.pages = pages;
		}
		
		public int findPageAdjusted(CssContext c, int yOffset) {
		    int pg = this.findPage(c, yOffset);
		    
		    if (pg == PAGE_BEFORE_START) {
		        return 0;
		    } else if (pg == PAGE_AFTER_END) {
		        return pages.size() - 1;
		    } else {
		        return pg;
		    }
		}
		
		private int findPage(CssContext c, int yOffset) {
			if (yOffset < 0) {
				return PAGE_BEFORE_START;
		    } else {
		    	PageBox lastRequest = pages.get(lastRequested);
		    	if (yOffset >= lastRequest.getTop() && yOffset < lastRequest.getBottom()) {
		    			return lastRequested;
		        }
		        
		    	PageBox last = pages.get(pages.size() - 1);
		        
		    	if (yOffset >= last.getTop() && yOffset < last.getBottom()) {
		    		lastRequested = pages.size() - 1;
	    			return lastRequested;
	            }
		    	
		    	if (yOffset < last.getBottom()) {
		                // The page we're looking for is probably at the end of the
		                // document so do a linear search for the first few pages
		                // and then fall back to a binary search if that doesn't work
		                // out
		                int count = pages.size();
		                for (int i = count - 1; i >= 0 && i >= count - 5; i--) {
		                    PageBox pageBox = pages.get(i);
		                    
		                    if (yOffset >= pageBox.getTop() && yOffset < pageBox.getBottom()) {
		                        lastRequested = i;
		                        return i;
		                    }
		                }

		                int low = 0;
		                int high = count-6;

		                while (low <= high) {
		                    int mid = (low + high) >> 1;
		                    PageBox pageBox = pages.get(mid);

		                    if (yOffset >= pageBox.getTop() && yOffset < pageBox.getBottom()) {
		                        lastRequested = mid;
		                        return mid;
		                    }

		                    if (pageBox.getTop() < yOffset) {
		                        low = mid + 1;
		                    } else {
		                        high = mid - 1;
		                    }
		                }
		        } else {
		        	return PAGE_AFTER_END;
		        }
		    }
			
			// Unreachable
			return -1;
		}
	}
	
	private final List<PageResult> result;
	private final List<PageBox> pages;
	private final PageFinder finder;
	private final int startPage;
	
	/**
	 * A more efficient paged box collector that can only find boxes on pages minPage to
	 * maxPage inclusive.
	 */
	public PagedBoxCollector(List<PageBox> pages, int minPage, int maxPage) {
	    this.pages = pages;
	    this.result = new ArrayList<PageResult>(maxPage - minPage + 1);
	    this.finder = new PageFinder(pages);
	    this.startPage = minPage;
	    
	    for (int i = minPage; i <= maxPage; i++) {
	        result.add(new PageResult());
	    }
	}
	
	public void collect(CssContext c, Layer layer) {
		if (layer.isInline()) {
			collectInline(c, layer);
		} else {
			collect(c, layer, layer.getMaster(), PAGE_ALL);
		}
	}
	
	// TODO: MAke sahdow page aware.
	private void collectInline(CssContext c, Layer layer) {
        InlineLayoutBox iB = (InlineLayoutBox) layer.getMaster();
        List<Box> content = iB.getElementWithContent();

        for (Box b : content) {

        	int pgStart = findStartPage(c, b, layer.getCurrentTransformMatrix());
        	int pgEnd = findEndPage(c, b, layer.getCurrentTransformMatrix());
        	
        	for (int i = pgStart; i <= pgEnd; i++) {
        	    Shape pageClip = getPageResult(i).getContentWindowOnDocument(getPageBox(i), c);
        	
        		if (b.intersects(c, pageClip)) {
        			if (b instanceof InlineLayoutBox) {
        				getPageResult(i).addInline(b);
        			} else { 
        				BlockBox bb = (BlockBox) b;

        				if (bb.isInline()) {
        					if (intersectsAny(c, pageClip, b, b)) {
        						getPageResult(i).addInline(b);
        					}
        				} else {
        					collect(c, layer, bb, PAGE_ALL);
        				}
        			}
        		}
        	}
        }
	}
	
	public void collectFloats(CssContext c, Layer layer) {
	    for (int iflt = layer.getFloats().size() - 1; iflt >= 0; iflt--) {
            BlockBox floater = (BlockBox) layer.getFloats().get(iflt);
            
            int pgStart = findStartPage(c, floater, layer.getCurrentTransformMatrix());
            int pgEnd = findEndPage(c, floater, layer.getCurrentTransformMatrix());
            
            for (int i = getValidMinPageNumber(pgStart); i <= getValidMaxPageNumber(pgEnd); i++) {
                PageResult pgRes = getPageResult(i);
                PageBox pageBox = getPageBox(i);
                
                if (intersectsAggregateBounds(pgRes.getContentWindowOnDocument(pageBox, c), floater)) {
                    pgRes.addFloat(floater);
                }
                
                if (pageBox.shouldInsertPages()) {
                    addBoxToShadowPages(c, floater, i, pgRes, null, null, layer, AddFloatToShadowPage.INSTANCE);
                }
            }
        }
	}
	
	/**
	 * The main box collection method. This method works recursively
	 * to add all the boxes (inlines and blocks separately) owned by this layer
	 * to their respective flat page display lists. It also adds clip and setClip operations
	 * where needed to clip content in <code>overflow:hidden</code>blocks.
	 * @param c
	 * @param layer
	 * @param container
	 */
	public void collect(CssContext c, Layer layer, Box container, int shadowPageNumber) {
	    int pgStart;
	    int pgEnd;
	    
	    if (container instanceof BlockBox) {
           Rectangle bounds = container.getBorderBox(c);
           pgStart = findStartPage(c, bounds, layer.getCurrentTransformMatrix());
           pgEnd = findEndPage(c, bounds, layer.getCurrentTransformMatrix());
	    } else {
	       pgStart = findStartPage(c, container, layer.getCurrentTransformMatrix());
	       pgEnd = findEndPage(c, container, layer.getCurrentTransformMatrix());
	    }

	    collect(c, layer, container, pgStart, pgEnd, shadowPageNumber);
	}
	
	/**
	 * Add collected items to base page only, ignoring inserted shadow pages.
	 */
	public static final int PAGE_BASE_ONLY = -1;
	
	/**
	 * Add collected boxes to all pages, including inserted shadow pages.
	 */
	public static final int PAGE_ALL = -2;
	
	public void collect(CssContext c, Layer layer, Box container, int pgStart, int pgEnd, int shadowPageNumber) {
		if (layer != container.getContainingLayer()) {
			// Different layers are responsible for their own box collection.
			return;
	    }

        if (container instanceof LineBox) {

            for (int i = getValidMinPageNumber(pgStart); i <= getValidMaxPageNumber(pgEnd); i++) {
                if (shadowPageNumber == PAGE_ALL) {
                    addLineBoxToAll(c, layer, (LineBox) container, i, true);
                } else if (shadowPageNumber == PAGE_BASE_ONLY) {
                    addLineBoxToAll(c, layer, (LineBox) container, i, false);
                } else {
                    addLineBoxToShadowPage(c, layer, (LineBox) container, i, shadowPageNumber);
                }
            }

        } else {
        	
        	Shape ourClip = null;
        	List<PageResult> clipPages = null;
        	
        	if (container.getLayer() == null ||
        		layer.getMaster() == container ||
        	    !(container instanceof BlockBox)) {
        
            	// Check if we need to clip this box.
            	if (c instanceof RenderingContext &&
            		container instanceof BlockBox) {
            		
            		BlockBox block = (BlockBox) container;
            		
            		if (block.isNeedsClipOnPaint((RenderingContext) c)) {
            			// A box with overflow set to hidden.
            			ourClip = block.getChildrenClipEdge((RenderingContext) c);
            			clipPages = new ArrayList<PagedBoxCollector.PageResult>();
             		}
            	}
            	
            	if (shadowPageNumber == PAGE_ALL) {
            	    addBlockToAll(c, layer, container, pgStart, pgEnd, ourClip, clipPages, true);
            	} else if (shadowPageNumber == PAGE_BASE_ONLY) {
            	    addBlockToAll(c, layer, container, pgStart, pgEnd, ourClip, clipPages, false);
            	} else {
            	    addBlockToShadowPage(c, layer, container, pgStart, pgEnd, ourClip, clipPages, shadowPageNumber);
            	}
        	}

            if (container instanceof TableSectionBox &&
                (((TableSectionBox) container).isHeader() || ((TableSectionBox) container).isFooter()) &&
                ((TableSectionBox) container).getTable().hasContentLimitContainer() &&
                (container.getLayer() == null || container == layer.getMaster()) &&
                c instanceof RenderingContext) {
                
                addTableHeaderFooter(c, layer, container, shadowPageNumber);
            } else {
                // Recursively, process all children and their children.
                if (container.getLayer() == null || container == layer.getMaster()) {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        Box child = container.getChild(i);
                        collect(c, layer, child, shadowPageNumber);
                    }
                }
            }
            
            if (clipPages != null) {
                // Pop the clip on those pages it was set.
                for (PageResult pgRes : clipPages) {
                    pgRes.setClipAll(new OperatorSetClip(null));
                }
            }
        }
	}

    private void addBlockToAll(CssContext c, Layer layer, Box container, int pgStart, int pgEnd, Shape ourClip,
            List<PageResult> clipPages, boolean includeShadowPages) {
        for (int i = getValidMinPageNumber(pgStart); i <= getValidMaxPageNumber(pgEnd); i++) {
        	PageResult pageResult = getPageResult(i);
        	PageBox pageBox = getPageBox(i);
        	Rectangle pageClip = pageResult.getContentWindowOnDocument(pageBox, c);

        	// Test to see if it fits within the page margins.
        	if (intersectsBorderBoxBounds(c, pageClip, container)) {
        		addBlock(container, pageResult);

        		if (ourClip != null) {
        			// Add a clip operation before the block's descendents (inline or block).
        			pageResult.clipAll(new OperatorClip(ourClip));
        			
        			// Add the page result to a list, so we can pop clip later.
        			clipPages.add(pageResult);
        		}
        	}
        	
        	if (includeShadowPages && pageBox.shouldInsertPages()) {
        	    addBoxToShadowPages(c, container, i, pageResult, ourClip, clipPages, layer, AddBlockToShadowPage.INSTANCE);
        	}
        }
    }
    
    private void addBlockToShadowPage(CssContext c, Layer layer, Box container, int pgStart, int pgEnd, Shape ourClip, List<PageResult> clipPages, int shadowPageNumber) {
        for (int i = getValidMinPageNumber(pgStart); i <= getValidMaxPageNumber(pgEnd); i++) {
            PageResult pageResult = getPageResult(i);
            PageBox pageBox = getPageBox(i);
            Rectangle shadowPageClip = pageResult.getShadowWindowOnDocument(pageBox, c, shadowPageNumber);

            // Test to see if it fits within the page margins.
            if (intersectsBorderBoxBounds(c, shadowPageClip, container)) {
                PageResult shadowPageResult = getOrCreateShadowPage(pageResult, shadowPageNumber);
                addBlock(container, shadowPageResult);

                if (ourClip != null) {
                    // Add a clip operation before the block's descendents (inline or block).
                    shadowPageResult.clipAll(new OperatorClip(ourClip));

                    // Add the page result to a list, so we can pop clip later.
                    clipPages.add(shadowPageResult);
                }
            } 
        }
    }

	private void addLineBoxToShadowPage(CssContext c, Layer layer, LineBox container, int basePageNumber, int shadowPageNumber) {
        PageResult pageResult = getPageResult(basePageNumber);
        PageBox pageBox = getPageBox(basePageNumber);
        Rectangle shadowPageClip = pageResult.getShadowWindowOnDocument(pageBox, c, shadowPageNumber);
        
        if (intersectsAggregateBounds(shadowPageClip, container)) {
            PageResult shadowPageResult = getOrCreateShadowPage(pageResult, shadowPageNumber);
            
            shadowPageResult.addInline(container);

            // Recursively add all children of the line box to the inlines list.
            ((LineBox) container).addAllChildren(shadowPageResult._inlines, layer);
        }
    }

    /**
	 * Adds a line box to the base page if needed and any shadow pages as needed.
	 */
    private void addLineBoxToAll(CssContext c, Layer layer, LineBox container, int basePageNumber, boolean includeShadowPages) {
        PageResult pageResult = getPageResult(basePageNumber);
        PageBox pageBox = getPageBox(basePageNumber);
        Rectangle pageClip = pageResult.getContentWindowOnDocument(pageBox, c);

        if (intersectsAggregateBounds(pageClip, container)) {
            pageResult.addInline(container);

            // Recursively add all children of the line box to the inlines list.
            ((LineBox) container).addAllChildren(pageResult._inlines, layer);
        }
        
        if (includeShadowPages && pageBox.shouldInsertPages()) {
            addBoxToShadowPages(c, container, basePageNumber, pageResult, null, null, layer, AddInlineToShadowPage.INSTANCE);
        }
    }
	
    /**
     * Inserts shadow pages as needed.
     */
	private PageResult getOrCreateShadowPage(PageResult basePage, int shadowPageNumber) {
	    while (shadowPageNumber >= basePage.shadowPages().size()) {
	        basePage.addShadowPage(new PageResult());
	    }
	    
	    return basePage.shadowPages().get(shadowPageNumber);
	}
	
	/**
	 * The joys of lambda style programming in Java 6! Provides a method to add a box to a shadow page
	 * if it is determined to sit on a particular shadow page.
	 * 
	 * Other method (boundsBox) is whether to use the border-box or the aggregate (includes overflowing children)
	 * to test whether a box sits on a particular shadow page.
	 */
	private interface AddToShadowPage {
	    final static int BORDER_BOX = 1;
	    final static int AGGREGATE_BOX = 2;
	    int boundsBox();
	    boolean add(PagedBoxCollector collector, PageResult shadowPageResult, Box container, Shape clip, Layer layer);
	}
	
	private static class AddBlockToShadowPage implements AddToShadowPage  {
	    private static final AddToShadowPage INSTANCE = new AddBlockToShadowPage();
	    
	    @Override
	    public int boundsBox() {
	        return AddToShadowPage.BORDER_BOX;
	    }
	    
        @Override
        public boolean add(PagedBoxCollector collector, PageResult shadowPageResult, Box container, Shape clip, Layer layer) {
            collector.addBlock(container, shadowPageResult);
            
            if (clip != null) {
                shadowPageResult.clipAll(new OperatorClip(clip));
                return true;
            }
            
            return false;
        }
	}
	
	private static class AddInlineToShadowPage implements AddToShadowPage {
	    private static final AddToShadowPage INSTANCE = new AddInlineToShadowPage();
	    
	    @Override
	    public int boundsBox() {
	       return AddToShadowPage.AGGREGATE_BOX;
	    }
	    
        @Override
        public boolean add(PagedBoxCollector collector, PageResult shadowPageResult, Box container, Shape clip, Layer layer) {
            shadowPageResult.addInline(container);

            // Recursively add all children of the line box to the inlines list.
            ((LineBox) container).addAllChildren(shadowPageResult._inlines, layer);
            
            return false;
        }
	}
	
	private static class AddFloatToShadowPage implements AddToShadowPage {
	    private static final AddToShadowPage INSTANCE = new AddFloatToShadowPage();
	    
	    @Override
	    public int boundsBox() {
	       return AddToShadowPage.BORDER_BOX;
	    }
	    
        @Override
        public boolean add(PagedBoxCollector collector, PageResult shadowPageResult, Box container, Shape clip, Layer layer) {
            shadowPageResult.addFloat((BlockBox) container);
            return false;
        }
	}
	
	/**
	 * Adds box to inserted shadow pages as needed.
	 */
    private void addBoxToShadowPages(
            CssContext c, Box container, int pageNumber,
            PageResult pageResult, Shape ourClip,
            /* adds-to: */ List<PageResult> clipPages,
            Layer layer, AddToShadowPage addToMethod) {
        
        PageBox basePageBox = getPageBox(pageNumber);
        
        AffineTransform ctm = container.getContainingLayer().getCurrentTransformMatrix();
        Rectangle bounds = container.getBorderBox(c);
        FourPoint corners = ctm == null ? null : getCornersFromTransformedBounds(bounds, ctm);

        int maxShadowPages;
        if (basePageBox.getCutOffPageDirection() == IdentValue.LTR) { 
            int maxX = (int) (ctm == null ? bounds.getMaxX() : getMaxX(corners));
            maxShadowPages = Math.min(basePageBox.getMaxInsertedPages(), basePageBox.getMaxShadowPagesForXPos(c, maxX));
        } else {
            int minX = (int) (ctm == null ? bounds.getMinX() : getMinX(corners));
            maxShadowPages = Math.min(basePageBox.getMaxInsertedPages(), basePageBox.getMaxShadowPagesForXPos(c, minX));
        }
        
        for (int i = 0; i < maxShadowPages; i++) {
            Rectangle shadowPageClip = pageResult.getShadowWindowOnDocument(basePageBox, c, i);
            
            boolean intersects = addToMethod.boundsBox() == AddToShadowPage.AGGREGATE_BOX ? 
                    intersectsAggregateBounds(shadowPageClip, container) :
                    intersectsBorderBoxBounds(c, shadowPageClip, container);
            
            if (intersects) {
                PageResult shadowPageResult = getOrCreateShadowPage(pageResult, i);
                
                if (addToMethod.add(this, shadowPageResult, container, ourClip, layer)) {
                    clipPages.add(shadowPageResult);
                }
            }
        }
    }

    private void addTableHeaderFooter(CssContext c, Layer layer, Box container, int shadowPageNumber) {
        // Yes, this is one giant hack. The problem is that there is only one tfoot and thead box per table
        // but if -fs-table-paginate is set to paginate we need to collect the header and footer on every page
        // that the table appears on. The solution we use here is to loop through the table's pages and update 
        // the section's position before collecting its children.
        
        TableBox table = ((TableSectionBox) container).getTable();
        RenderingContext rc = (RenderingContext) c;
        
        int tableStart = findStartPage(c, table, layer.getCurrentTransformMatrix());
        int tableEnd = findEndPage(c, table, layer.getCurrentTransformMatrix());
        
        for (int pgTable = getValidMinPageNumber(tableStart); pgTable <= getValidMaxPageNumber(tableEnd); pgTable++) {
            rc.setPage(pgTable, getPageBox(pgTable));
            table.updateHeaderFooterPosition(rc);

            for (int i = 0; i < container.getChildCount(); i++) {
                Box child = container.getChild(i);
                collect(c, layer, child, shadowPageNumber);
            }
        }
    }

    /**
     * Adds block box to appropriate flat box lists.
     */
    private void addBlock(Box container, PageResult pageResult) {
        pageResult.addBlock(container);
        
        if (container instanceof BlockBox) {
        	BlockBox block = (BlockBox) container;
        	
        	if (block.getReplacedElement() != null) {
        		pageResult.addReplaced(block);
        	}
        	
        	if (block.isListItem()) {
        		pageResult.addListItem(block);
        	}
        }
        
        if (container instanceof TableCellBox &&
        	((TableCellBox) container).hasCollapsedPaintingBorder()) {
        	pageResult.addTableCell((TableCellBox) container);
        }
    }

    private boolean intersectsAggregateBounds(Shape clip, Box box) {
        if (clip == null) {
            return true;
        }
        
        PaintingInfo info = box.getPaintingInfo();
        
        if (info == null) {
            return false;
        }
        
        Rectangle bounds = info.getAggregateBounds();
        
        AffineTransform ctm = box.getContainingLayer().getCurrentTransformMatrix();
        
        if (ctm == null) {
        	return clip.intersects(bounds);
        } else {
        	Shape boxShape = ctm.createTransformedShape(bounds);
        	return clip.intersects(boxShape.getBounds2D());
        }
    }
    
    /**
     * Returns whether a box (out to the outside edge of border) is partially or fully in a clip shape.
     * This should give us the painting bounds of the box itself, although child boxes can overflow.
     */
    private boolean intersectsBorderBoxBounds(CssContext c, Shape clip, Box box) {
        Rectangle borderBoxBounds = box.getBorderBox(c);
        
        AffineTransform ctm = box.getContainingLayer().getCurrentTransformMatrix();
        Area overflowClip = box.getAbsoluteClipBox(c);

        if (ctm == null && overflowClip == null) {
            return clip.intersects(borderBoxBounds);
        } else if (ctm != null && overflowClip == null) {
            Shape boxShape = ctm.createTransformedShape(borderBoxBounds);
            Area boxArea = new Area(boxShape);
            Area clipArea = new Area(clip);
            boxArea.intersect(clipArea);

            return !boxArea.isEmpty();
        } else { // if (overflowClip != null)
            Area boxArea = new Area(ctm == null ? borderBoxBounds : ctm.createTransformedShape(borderBoxBounds));
            boxArea.intersect(overflowClip);
            boxArea.intersect(new Area(clip));
            
            return !boxArea.isEmpty();
        }
    }
    
    private boolean intersectsAny(
            CssContext c, Shape clip, 
            Box master, Box container) {
        if (container instanceof LineBox) {
            if (container.intersects(c, clip)) {
                return true;
            }
        } else {
            if (container.getLayer() == null || !(container instanceof BlockBox)) {
                if (container.intersects(c, clip)) {
                    return true;
                }
            }

            if (container.getLayer() == null || container == master) {
                for (int i = 0; i < container.getChildCount(); i++) {
                    Box child = container.getChild(i);
                    boolean possibleResult = intersectsAny(c, clip, master, child);
                    if (possibleResult) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private static class FourPoint {
        private final Point2D ul;
        private final Point2D ur;
        private final Point2D ll;
        private final Point2D lr;
        
        private FourPoint(Point2D ul, Point2D ur, Point2D ll, Point2D lr) {
            this.ul = ul;
            this.ur = ur;
            this.ll = ll;
            this.lr = lr;
        }
    }
    
    private static FourPoint getCornersFromTransformedBounds(Rectangle bounds, AffineTransform transform) {
        Point2D ul = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
        Point2D ur = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        Point2D ll = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
        Point2D lr = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
        
        Point2D ult = transform.transform(ul, null);
        Point2D urt = transform.transform(ur, null);
        Point2D llt = transform.transform(ll, null);
        Point2D lrt = transform.transform(lr, null);

        return new FourPoint(ult, urt, llt, lrt);
    }
    
	/**
	 * There is a matrix in effect, we have to apply it to the box bounds before checking what page(s) it
	 * sits on. To do this we transform the four corners of the box.
	 */
    private static double getMinYFromTransformedBox(Rectangle bounds, AffineTransform transform) {
        FourPoint corners = getCornersFromTransformedBounds(bounds, transform);

        // Now get the least Y.
        double minY = Math.min(corners.ul.getY(), corners.ur.getY());
        minY = Math.min(corners.ll.getY(), minY);
        minY = Math.min(corners.lr.getY(), minY);
        
        return minY;
    }
    
	/**
	 * There is a matrix in effect, we have to apply it to the box bounds before checking what page(s) it
	 * sits on. To do this we transform the four corners of the box.
	 */
    private static double getMaxYFromTransformedBox(Rectangle bounds, AffineTransform transform) {
		FourPoint corners = getCornersFromTransformedBounds(bounds, transform);
		
		// Now get the max Y.
		double maxY = Math.max(corners.ul.getY(), corners.ur.getY());
		maxY = Math.max(corners.ll.getY(), maxY);
		maxY = Math.max(corners.lr.getY(), maxY);
		
		return maxY;
    }
    
    /**
     * There is a matrix in effect. We need the max x to see how many shadow pages need creating.
     */
    private static double getMaxXFromTransformedBox(Rectangle bounds, AffineTransform transform) {
        FourPoint corners = getCornersFromTransformedBounds(bounds, transform);

        // Now get the max X.
        double maxX = Math.max(corners.ul.getX(), corners.ur.getX());
        maxX = Math.max(corners.ll.getX(), maxX);
        maxX = Math.max(corners.lr.getX(), maxX);
        
        return maxX;
    }
    
    protected int findStartPage(CssContext c, Rectangle bounds, AffineTransform transform) {
        double minY = transform == null ? bounds.getMinY() : getMinYFromTransformedBox(bounds, transform);
        return this.finder.findPageAdjusted(c, (int) minY);
    }
    
    protected int findEndPage(CssContext c, Rectangle bounds, AffineTransform transform) {
        double maxY = transform == null ? bounds.getMaxY() : getMaxYFromTransformedBox(bounds, transform);
        return this.finder.findPageAdjusted(c, (int) maxY);
    }
   
    
    protected int findStartPage(CssContext c, Box container, AffineTransform transform) {
    	PaintingInfo info = container.calcPaintingInfo(c, true);
    	if (info == null) {
    		return -1;
    	}
    	Rectangle bounds = info.getAggregateBounds();
    	if (bounds == null) {
    		return -1;
    	}
    	
    	double minY = transform == null ? bounds.getMinY() : getMinYFromTransformedBox(bounds, transform);
       	return this.finder.findPageAdjusted(c, (int) minY);
    }
    
    protected int findEndPage(CssContext c, Box container, AffineTransform transform) {
    	PaintingInfo info = container.calcPaintingInfo(c, true);
    	if (info == null) {
    		return -1;
    	}
    	Rectangle bounds = info.getAggregateBounds();
    	if (bounds == null) {
    		return -1;
    	}
    	
    	double maxY = transform == null ? bounds.getMaxY() : getMaxYFromTransformedBox(bounds, transform);
       	return this.finder.findPageAdjusted(c, (int) maxY);
    }
    
    protected PageResult getPageResult(int pageNo) {
        return result.get(pageNo - this.startPage);
    }
    
    protected int getMaxPageNumber() {
        return this.startPage + result.size() - 1;
    }
    
    protected int getMinPageNumber() {
        return this.startPage;
    }
    
    protected int getValidMinPageNumber(int pageNo) {
        return Math.max(pageNo, getMinPageNumber());
    }
    
    protected int getValidMaxPageNumber(int pageNo) {
        return Math.min(pageNo, getMaxPageNumber());
    }
    
    protected PageBox getPageBox(int pageNo) {
        return pages.get(pageNo);
    }
    
    private static Rectangle getBoxRect(CssContext c, Box container) {
        PaintingInfo info = container.calcPaintingInfo(c, true);
        Rectangle bounds = info.getAggregateBounds();
        return bounds;
    }
    
    public static Rectangle findLayerRect(CssContext c, Layer layer) {
        Box container = layer.getMaster();
        Rectangle bounds = getBoxRect(c, container);

        // Floaters may be outside master box.
        // TODO: If this layer was triggered by a transform (not a positioned element)
        // then child positioned boxes may also fall otuside master box.
        for (BlockBox floater : layer.getFloats()) {
            Rectangle fBounds = getBoxRect(c, floater);
            bounds.add(fBounds);
        }
        
        return bounds;
    }
    
    private static double getMinY(FourPoint corners) {
        double minY = Math.min(corners.ul.getY(), corners.ur.getY());
        minY = Math.min(corners.ll.getY(), minY);
        minY = Math.min(corners.lr.getY(), minY);
        return minY;
    }

    private static double getMinX(FourPoint corners) {
        double minX = Math.min(corners.ul.getX(), corners.ur.getX());
        minX = Math.min(corners.ll.getX(), minX);
        minX = Math.min(corners.lr.getX(), minX);
        return minX;
    }
    
    private static double getMaxY(FourPoint corners) {
        double maxY = Math.max(corners.ul.getY(), corners.ur.getY());
        maxY = Math.max(corners.ll.getX(), maxY);
        maxY = Math.max(corners.lr.getX(), maxY);
        return maxY;
    }
    
    private static double getMaxX(FourPoint corners) {
        double maxX = Math.max(corners.ul.getX(), corners.ur.getX());
        maxX = Math.max(corners.ll.getX(), maxX);
        maxX = Math.max(corners.lr.getX(), maxX);
        return maxX;
    }
    
    public static class PageInfo {
        private PageInfo(int pgNumber, int shadowPgNumber) {
            this.pageNumber = pgNumber;
            this.shadowPageNumber = shadowPgNumber;
        }
        
        public static final int BASE_PAGE = -1;
        public final int pageNumber;
        public final int shadowPageNumber;
    }

    /**
     * Returns the pages a layer appears on including inserted overflow pages.
     * Takes into account any transform and overflow hidden clipping.
     */
    public static List<PageInfo> findLayerPages(CssContext c, Layer layer, List<PageBox> pages) {
        PageFinder finder = new PageFinder(pages);
        Rectangle bounds = findLayerRect(c, layer);
        Box container = layer.getMaster();
        AffineTransform transform = container.getContainingLayer().getCurrentTransformMatrix();
        Area overflowClip = container.getAbsoluteClipBox(c);
        
        if (transform != null) {
            FourPoint corners = getCornersFromTransformedBounds(bounds, transform);

            double minX = getMinX(corners);
            double minY = getMinY(corners);
            double maxX = getMaxX(corners);
            double maxY = getMaxY(corners);
            
            bounds.setBounds((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
        }
        
        if (overflowClip != null) {
            Area boxArea = new Area(bounds);
            boxArea.intersect(overflowClip);
            bounds = boxArea.getBounds();
        }
        
        int firstPage = finder.findPageAdjusted(c, (int) bounds.getMinY());
        int lastPage = finder.findPageAdjusted(c, (int) bounds.getMaxY());
        
        List<PageInfo> result = new ArrayList<PageInfo>();
        
        for (int i = firstPage; i <= lastPage; i++) {
            result.add(new PageInfo(i, PageInfo.BASE_PAGE));
            
            if (pages.get(i).shouldInsertPages()) {
                int maxXShadowPage = pages.get(i).getMaxShadowPagesForXPos(c, (int) bounds.getMaxX());
                int minXShadowPage = pages.get(i).getMaxShadowPagesForXPos(c, (int) bounds.getMinX()); 

                int shadowPageCount = Math.max(maxXShadowPage, minXShadowPage);
                shadowPageCount = Math.min(shadowPageCount, pages.get(i).getMaxInsertedPages());
                
                for (int j = 0; j < shadowPageCount; j++) {
                    result.add(new PageInfo(i, j));
                }
            }
        }
        
        return result;
    }

    /**
     * @return 0 based page number of start of container paint area (including overflow)
     */
	public static int findStartPage(CssContext c, Box container, List<PageBox> pages) {
		PageFinder finder = new PageFinder(pages);
    	PaintingInfo info = container.calcPaintingInfo(c, true);
    	Rectangle bounds = info.getAggregateBounds();

    	AffineTransform transform = container.getContainingLayer().getCurrentTransformMatrix();
    	double minY = transform == null ? bounds.getMinY() : getMinYFromTransformedBox(bounds, transform);
    	return finder.findPageAdjusted(c, (int) minY);
	}
	
	/**
     * @return 0 based page number of end of container paint area (including overflow)
	 */
	public static int findEndPage(CssContext c, Box container, List<PageBox> pages) {
		PageFinder finder = new PageFinder(pages);
    	PaintingInfo info = container.calcPaintingInfo(c, true);
    	Rectangle bounds = info.getAggregateBounds();

    	AffineTransform transform = container.getContainingLayer().getCurrentTransformMatrix();
    	double maxY = transform == null ? bounds.getMaxY() : getMaxYFromTransformedBox(bounds, transform);
		return finder.findPageAdjusted(c, (int) maxY);
	}
}

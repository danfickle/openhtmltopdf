package com.openhtmltopdf.render.displaylist;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		private boolean _hasListItems = false;
		private boolean _hasReplaceds = false;
		private Rectangle contentWindowOnDocument = null;
		
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
		
		private Rectangle getContentWindowOnDocument(PageBox page, CssContext c) {
			if (contentWindowOnDocument == null) {
				contentWindowOnDocument = page.getDocumentCoordinatesContentBounds(c);
			}
			return contentWindowOnDocument;
		}
	}
	
	public static class PageFinder {
		private int lastRequested = 0;
		private final List<PageBox> pages;
		
		public PageFinder(List<PageBox> pages) {
			this.pages = pages;
		}
		
		public int findPage(CssContext c, int yOffset) {
			if (yOffset < 0) {
				return -1;
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
		        	return -1;
		        }
		    }
			
			return -1;
		}
	}
	
	private final List<PageResult> result;
	private final List<PageBox> pages;
	private final PageFinder finder;
	
	protected PagedBoxCollector() {
	    this.result = null;
	    this.pages = null;
	    this.finder = null;
	}
	
	public PagedBoxCollector(List<PageBox> pages) {
		this.pages = pages;
		this.result = new ArrayList<PageResult>(pages.size());
		this.finder = new PageFinder(pages);
		
		for (int i = 0; i < pages.size(); i++) {
			result.add(new PageResult());
		}
	}
	
	public void collect(CssContext c, Layer layer) {
		if (layer.isInline()) {
			collectInline(c, layer);
		} else {
			collect(c, layer, layer.getMaster());
		}
	}
	
	private void collectInline(CssContext c, Layer layer) {
        InlineLayoutBox iB = (InlineLayoutBox) layer.getMaster();
        List<Box> content = iB.getElementWithContent();
        
        for (Box b : content) {

        	int pgStart = findStartPage(c, b, layer.getCurrentTransformMatrix());
        	int pgEnd = findEndPage(c, b, layer.getCurrentTransformMatrix());
        	
        	for (int i = pgStart; i <= pgEnd; i++) {
        		if (i < 0 || i > getMaxPageNumber()) {
        			continue;
        		}
        		
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
        					collect(c, layer, bb);
        				}
        			}
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
	public void collect(CssContext c, Layer layer, Box container) {
		if (layer != container.getContainingLayer()) {
			// Different layers are responsible for their own box collection.
			return;
	    }
		
		int pgStart = -1;
		int pgEnd = -1;

        if (container instanceof LineBox) {
        
        	pgStart = findStartPage(c, container, layer.getCurrentTransformMatrix());
        	pgEnd = findEndPage(c, container, layer.getCurrentTransformMatrix());

        	for (int i = pgStart; i <= pgEnd; i++) {
        		if (i < 0 || i > getMaxPageNumber()) {
        			continue;
        		}
        		
        		PageResult res = getPageResult(i);
        		res.addInline(container);
        		
        		// Recursively add all children of the line box to the inlines list.
        		((LineBox) container).addAllChildren(res._inlines, layer);
        	}

        } else {
        	
        	Shape ourClip = null;
        	
        	if (container.getLayer() == null ||
        		layer.getMaster() == container ||
        	    !(container instanceof BlockBox)) {
        		
        		pgStart = findStartPage(c, container, layer.getCurrentTransformMatrix());
            	pgEnd = findEndPage(c, container, layer.getCurrentTransformMatrix());
        
            	// Check if we need to clip this box.
            	if (c instanceof RenderingContext &&
            		container instanceof BlockBox) {
            		
            		BlockBox block = (BlockBox) container;
            		
            		if (block.isNeedsClipOnPaint((RenderingContext) c)) {
            			// A box with overflow set to hidden.
            			ourClip = block.getChildrenClipEdge((RenderingContext) c);
             		}
            	}
            	
            	for (int i = pgStart; i <= pgEnd; i++) {
            		if (i < 0 || i > getMaxPageNumber()) {
            			continue;
            		}
            		
            		PageResult pageResult = getPageResult(i);
            		Rectangle pageClip = pageResult.getContentWindowOnDocument(getPageBox(i), c);

            		// Test to see if it fits within the page margins.
            		if (intersectsAggregateBounds(pageClip, container)) {
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

            			if (ourClip != null) {
            				// Add a clip operation before the block's descendents (inline or block).
            				pageResult.clipAll(new OperatorClip(ourClip));
            			}
            		}
            	}
        	}

        	
            if (container instanceof TableSectionBox &&
                (((TableSectionBox) container).isHeader() || ((TableSectionBox) container).isFooter()) &&
                ((TableSectionBox) container).getTable().hasContentLimitContainer() &&
                (container.getLayer() == null || container == layer.getMaster()) &&
                c instanceof RenderingContext) {
                
                // Yes, this is one giant hack. The problem is that there is only one tfoot and thead box per table
                // but if -fs-table-paginate is set to paginate we need to collect the header and footer on every page
                // that the table appears on. The solution we use here is to loop through the table's pages and update 
                // the section's position before collecting its children.
                
                TableBox table = ((TableSectionBox) container).getTable();
                RenderingContext rc = (RenderingContext) c;
                
                int tableStart = findStartPage(c, table, layer.getCurrentTransformMatrix());
                int tableEnd = findEndPage(c, table, layer.getCurrentTransformMatrix());
                
                for (int pgTable = tableStart; pgTable <= tableEnd; pgTable++) {
                    if (pgTable < 0 || pgTable > getMaxPageNumber()) {
                        continue;
                    }
                    
                    rc.setPage(pgTable, getPageBox(pgTable));
                    table.updateHeaderFooterPosition(rc);

                    for (int i = 0; i < container.getChildCount(); i++) {
                        Box child = container.getChild(i);
                        collect(c, layer, child);
                    }
                }
            } else {
                // Recursively, process all children and their children.
                if (container.getLayer() == null || container == layer.getMaster()) {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        Box child = container.getChild(i);
                        collect(c, layer, child);
                    }
                }
            }
            
            if (ourClip != null) {
            	// Restore the clip on those pages it was changed.
            	for (int i = pgStart; i <= pgEnd; i++) {
            		if (i < 0 || i > getMaxPageNumber()) {
            			continue;
            		}
            		
            		PageResult pageResult = getPageResult(i);
            		Rectangle pageClip = pageResult.getContentWindowOnDocument(getPageBox(i), c);
            		
            		// Test to see if it fits within the page margins.
            		if (intersectsAggregateBounds(pageClip, container)) {
          				pageResult.setClipAll(new OperatorSetClip(null));
            		}
            	}
            }
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
    
	/**
	 * There is a matrix in effect, we have to apply it to the box bounds before checking what page(s) it
	 * sits on. To do this we transform the four corners of the box.
	 */
    private static double getMinYFromTransformedBox(Rectangle bounds, AffineTransform transform) {
		Point2D ul = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
		Point2D ur = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
		Point2D ll = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
		Point2D lr = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
		
		Point2D ult = transform.transform(ul, null);
		Point2D urt = transform.transform(ur, null);
		Point2D llt = transform.transform(ll, null);
		Point2D lrt = transform.transform(lr, null);

		// Now get the least Y.
		double minY = Math.min(ult.getY(), urt.getY());
		minY = Math.min(llt.getY(), minY);
		minY = Math.min(lrt.getY(), minY);
		
		return minY;
    }
    
	/**
	 * There is a matrix in effect, we have to apply it to the box bounds before checking what page(s) it
	 * sits on. To do this we transform the four corners of the box.
	 */
    private static double getMaxYFromTransformedBox(Rectangle bounds, AffineTransform transform) {
		Point2D ul = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
		Point2D ur = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
		Point2D ll = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
		Point2D lr = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
		
		Point2D ult = transform.transform(ul, null);
		Point2D urt = transform.transform(ur, null);
		Point2D llt = transform.transform(ll, null);
		Point2D lrt = transform.transform(lr, null);

		// Now get the max Y.
		double maxY = Math.max(ult.getY(), urt.getY());
		maxY = Math.max(llt.getY(), maxY);
		maxY = Math.max(lrt.getY(), maxY);
		
		return maxY;
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
       	return this.finder.findPage(c, (int) minY);
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
       	return this.finder.findPage(c, (int) maxY);
    }
    
    protected PageResult getPageResult(int pageNo) {
        return result.get(pageNo);
    }
    
    protected int getMaxPageNumber() {
        return result.size() - 1;
    }
    
    protected PageBox getPageBox(int pageNo) {
        return pages.get(pageNo);
    }
	
	public static int findStartPage(CssContext c, Box container, List<PageBox> pages) {
		PageFinder finder = new PageFinder(pages);
    	PaintingInfo info = container.calcPaintingInfo(c, true);
    	if (info == null) {
    		return -1;
    	}
    	Rectangle bounds = info.getAggregateBounds();
    	if (bounds == null) {
    		return -1;
    	}
    	AffineTransform transform = container.getContainingLayer().getCurrentTransformMatrix();
    	double minY = transform == null ? bounds.getMinY() : getMinYFromTransformedBox(bounds, transform);
    	return finder.findPage(c, (int) minY);
	}
	
	public static int findEndPage(CssContext c, Box container, List<PageBox> pages) {
		PageFinder finder = new PageFinder(pages);
    	PaintingInfo info = container.calcPaintingInfo(c, true);
    	if (info == null) {
    		return -1;
    	}
    	Rectangle bounds = info.getAggregateBounds();
    	if (bounds == null) {
    		return -1;
    	}
    	AffineTransform transform = container.getContainingLayer().getCurrentTransformMatrix();
    	double maxY = transform == null ? bounds.getMaxY() : getMaxYFromTransformedBox(bounds, transform);
		return finder.findPage(c, (int) maxY);
	}

	public List<PageResult> getCollectedPageResults() {
		return result;
	}
}

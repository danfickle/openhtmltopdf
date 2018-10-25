package com.openhtmltopdf.render.displaylist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openhtmltopdf.layout.CollapsedBorderSide;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.newtable.CollapsedBorderValue;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector.PageInfo;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector.PageResult;

public class DisplayListCollector {
    protected static enum CollectFlags {
        /**
         * Fixed layers appear on each page. To avoid having to clone each box in a fixed layer onto 
         * each page, we have this flag so we can exclude fixed boxes in the multi page run and just
         * collect them at the last minute when painting a particular page. 
         */
        INCLUDE_FIXED_BOXES;
    }
    
 	private final List<PageBox> _pages;
	
	public DisplayListCollector(List<PageBox> pages) {
		this._pages = pages;
	}

	private void collectLayers(RenderingContext c, List<Layer> layers, DisplayListContainer dlPages, Set<CollectFlags> flags) {
		for (Layer layer : layers) {
			collect(c, layer, dlPages, flags);
		}
	}

	/**
	 * Adds a paint operation to a selection of pages, from pgStart to pgEnd inclusive.
	 */
	protected void addItem(DisplayListOperation item, int pgStart, int pgEnd,
			DisplayListContainer dlPages) {
		for (int i = pgStart; i <= pgEnd; i++) {
			dlPages.getPageInstructions(i).addOp(item);
		}
	}
	
	protected void addItem(DisplayListOperation item, List<PageInfo> pages, DisplayListContainer dlPages) {
	    for (PageInfo pg : pages) {
	        if (pg.shadowPageNumber == PageInfo.BASE_PAGE) {
	            dlPages.getPageInstructions(pg.pageNumber).addOp(item);
	        } else {
	            dlPages.getPageInstructions(pg.pageNumber).getShadowPage(pg.shadowPageNumber).addOp(item);
	        }
	    }
	}
	
	protected void addTransformItem(Box master, List<PageInfo> pages, DisplayListContainer dlPages) {
	    for (PageInfo pg : pages) {
	        if (pg.shadowPageNumber == PageInfo.BASE_PAGE) {
                dlPages.getPageInstructions(pg.pageNumber).addOp(new PaintPushTransformLayer(master, -1));
            } else {
                dlPages.getPageInstructions(pg.pageNumber).getShadowPage(pg.shadowPageNumber).addOp(new PaintPushTransformLayer(master, pg.shadowPageNumber));
            }
	    }
	}

	/**
	 * Use this method to collect all boxes recursively into a list of paint instructions
	 * for each page.
	 */
	public DisplayListContainer collectRoot(RenderingContext c, Layer rootLayer) {
		if (!rootLayer.isRootLayer()) {
			return null;
		}
		
		// We propagate any transformation matrixes recursively after layout has finished.
		rootLayer.propagateCurrentTransformationMatrix(c);

		DisplayListContainer displayList = new ArrayDisplayListContainer(0, _pages.size() - 1);

		// Recursively collect boxes for root layer and any children layers. Don't include
		// fixed boxes at this point. They are collected by the <code>SinglePageDisplayListCollector</code>
		// at the point of painting each page.
		collect(c, rootLayer, displayList, EnumSet.noneOf(CollectFlags.class));

		return displayList;
	}

	/** 
	 * The main method to create a list of paint instruction for each page.
	 */
	protected void collect(RenderingContext c, Layer layer, DisplayListContainer dlPages, Set<CollectFlags> flags) {
		if (layer.getMaster().getStyle().isFixed() && !flags.contains(CollectFlags.INCLUDE_FIXED_BOXES)) {
			// We don't collect fixed layers or their children here, because we don't want to have
			// to clone the entire subtree of the fixed box and all descendents.
			// So just paint it at the last minute.
			DisplayListOperation dlo = new PaintFixedLayer(layer);
			addItem(dlo, 0, _pages.size() - 1, dlPages);
			return;
		}
		
		List<PageInfo> layerPages = PagedBoxCollector.findLayerPages(c, layer, _pages);
		int layerPageStart = findStartPage(c, layer);
		int layerPageEnd = findEndPage(c, layer);
		boolean pushedClip = false;

		Rectangle parentClip = layer.getMaster().getParentClipBox(c, layer.getParent());

        if (parentClip != null) {
            // There is a clip in effect, so use it.
		    DisplayListOperation dlo = new PaintPushClipRect(parentClip);
		    addItem(dlo, layerPages, dlPages);
		    pushedClip = true;
		}
		
		if (layer.hasLocalTransform()) {
	        addTransformItem(layer.getMaster(), layerPages, dlPages);
	    }
		
		if (layer.isRootLayer() && layer.getMaster().hasRootElementBackground(c)) {

			// IMPROVEMENT: If the background image doesn't cover every page,
			// we could perhaps optimize this.
			DisplayListOperation dlo = new PaintRootElementBackground(layer.getMaster());
			addItem(dlo, dlPages.getMinPage(), dlPages.getMaxPage(), dlPages);
		}
		
		if (!layer.isInline() && ((BlockBox) layer.getMaster()).isReplaced()) {
			collectReplacedElementLayer(c, layer, dlPages, layerPageStart, layerPageEnd);
		} else {

			PagedBoxCollector collector = createBoundedBoxCollector(layerPageStart, layerPageEnd);
			
			collector.collectFloats(c, layer);
			collector.collect(c, layer);

			if (!layer.isInline() && layer.getMaster() instanceof BlockBox) {
				collectLayerBackgroundAndBorder(c, layer, dlPages, layerPageStart, layerPageEnd);
			}

			if (layer.isRootLayer() || layer.isStackingContext()) {
				collectLayers(c, layer.getSortedLayers(Layer.NEGATIVE), dlPages, flags);
			}

			for (int pageNumber = layerPageStart; pageNumber <= layerPageEnd; pageNumber++) {
				PageResult pg = collector.getPageResult(pageNumber);
				DisplayListPageContainer dlPageList = dlPages.getPageInstructions(pageNumber);

				processPage(c, layer, pg, dlPageList, true, pageNumber, -1);
				processShadowPages(c, layer, pageNumber, pg, dlPageList, true);
			}

			if (layer.isRootLayer() || layer.isStackingContext()) {
				collectLayers(c, layer.collectLayers(Layer.AUTO), dlPages, flags);
				// TODO z-index: 0 layers should be painted atomically
				collectLayers(c, layer.getSortedLayers(Layer.ZERO), dlPages, flags);
				collectLayers(c, layer.getSortedLayers(Layer.POSITIVE), dlPages, flags);
			}
		}
		
		if (layer.hasLocalTransform()) {
			DisplayListOperation dlo = new PaintPopTransformLayer(layer.getMaster());
			addItem(dlo, layerPages, dlPages);
		}
		
        if (pushedClip) {
		    DisplayListOperation dlo = new PaintPopClipRect();
            addItem(dlo, layerPages, dlPages);
		}
	}

    /**
     * Convert a list of boxes to a list of paint instructions for a page.
     */
    protected void processPage(RenderingContext c, Layer layer, PageResult pg, DisplayListPageContainer dlPageList, boolean includeFloats, int pageNumber, int shadowPageNumber) {

        if (!pg.blocks().isEmpty()) {
            Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders = pg.tcells().isEmpty() ? null
                    : collectCollapsedTableBorders(c, pg.tcells());
            DisplayListOperation dlo = new PaintBackgroundAndBorders(pg.blocks(), collapsedTableBorders);
            dlPageList.addOp(dlo);
        }
        
        if (includeFloats) {
            for (BlockBox floater : pg.floats()) {
                collectFloatAsLayer(c, layer, floater, dlPageList, pageNumber, shadowPageNumber);
            }
        }

        if (!pg.listItems().isEmpty()) {
            DisplayListOperation dlo = new PaintListMarkers(pg.listItems());
            dlPageList.addOp(dlo);
        }

        if (!pg.inlines().isEmpty()) {
            DisplayListOperation dlo = new PaintInlineContent(pg.inlines());
            dlPageList.addOp(dlo);
        }

        if (!pg.replaceds().isEmpty()) {
            DisplayListOperation dlo = new PaintReplacedElements(pg.replaceds());
            dlPageList.addOp(dlo);
        }
    }
    
    /**
     * If we have cut-off boxes we have to process them as separate pages.
     */
    private void processShadowPages(RenderingContext c, Layer layer, int pageNumber, PageResult pg, DisplayListPageContainer dlPageList, boolean includeFloats) {
        int shadowCnt = 0;
        for (PageResult shadow : pg.shadowPages()) {
            DisplayListPageContainer shadowPage = dlPageList.getShadowPage(shadowCnt);
            processPage(c, layer, shadow, shadowPage, includeFloats, pageNumber, shadowCnt);
            shadowCnt++;
        }
    }
 
    /**
     * This method can be reached by two code paths:
     * <code>
     * collectRoot -: collect -: processPage -: collectFloatAsLayer -: processPage
     * collectRoot -: collect -: processShadowPages -: foreach(shadowPage) -: processPage -: collectFloatAsLayer -: processPage
     * </code>
     * Therefore, it is important to be careful when expecting a base page or shadow page.
     */
	private void collectFloatAsLayer(RenderingContext c, Layer layer, BlockBox floater, DisplayListPageContainer pageInstructions, int pageNumber, int shadowPageNumber) {
	    PagedBoxCollector collector = createBoundedBoxCollector(pageNumber, pageNumber);
	    collector.collect(c, layer, floater, pageNumber, pageNumber, shadowPageNumber);
	    PageResult pageBoxes = collector.getPageResult(pageNumber);

	    if (shadowPageNumber >= 0 && pageBoxes.hasShadowPage(shadowPageNumber)) {
		    pageBoxes = pageBoxes.shadowPages().get(shadowPageNumber);
		} else if (shadowPageNumber >= 0) {
		    /* Nothing for this float on this shadow page. */
		    return;
		}
	    
	    boolean pushedClip = false;
	    
	    if (floater.getClipBox(c, floater.getContainingLayer()) != null) {
            // There is a clip in effect, so use it.
            DisplayListOperation dlo = new PaintPushClipRect(floater.getClipBox(c, floater.getContainingLayer()));
            pageInstructions.addOp(dlo);
            pushedClip = true;
        }

		processPage(c, layer, pageBoxes, pageInstructions, false, pageNumber, shadowPageNumber);

		if (pushedClip) {
		    DisplayListOperation dlo = new PaintPopClipRect();
		    pageInstructions.addOp(dlo);
		}
	}

	private void collectLayerBackgroundAndBorder(RenderingContext c, Layer layer,
			DisplayListContainer dlPages, int pgStart, int pgEnd) {

		DisplayListOperation dlo = new PaintLayerBackgroundAndBorder(layer.getMaster());
		addItem(dlo, pgStart, pgEnd, dlPages);
	}

	private void collectReplacedElementLayer(RenderingContext c, Layer layer,
			DisplayListContainer dlPages, int pgStart, int pgEnd) {

		DisplayListOperation dlo = new PaintLayerBackgroundAndBorder(layer.getMaster());
		addItem(dlo, pgStart, pgEnd, dlPages);

		DisplayListOperation dlo2 = new PaintReplacedElement((BlockBox) layer.getMaster());
		addItem(dlo2, pgStart, pgEnd, dlPages);
	}

	// Bit of a kludge here. We need to paint collapsed table borders according
	// to priority so (for example) wider borders float to the top and aren't
	// overpainted by thinner borders. This method takes the table cell boxes
	// (only those with collapsed border painting)
	// we're about to draw and returns a map with the last cell in a given table
	// we'll paint as a key and a sorted list of borders as values. These are
	// then painted after we've drawn the background for this cell.
	public static Map<TableCellBox, List<CollapsedBorderSide>> collectCollapsedTableBorders(RenderingContext c,
			List<TableCellBox> tcells) {
		Map<TableBox, List<CollapsedBorderSide>> cellBordersByTable = new HashMap<TableBox, List<CollapsedBorderSide>>();
		Map<TableBox, TableCellBox> triggerCellsByTable = new HashMap<TableBox, TableCellBox>();

		Set<CollapsedBorderValue> all = new HashSet<CollapsedBorderValue>(0);

		for (TableCellBox cell : tcells) {
			List<CollapsedBorderSide> borders = cellBordersByTable.get(cell.getTable());

			if (borders == null) {
				borders = new ArrayList<CollapsedBorderSide>();
				cellBordersByTable.put(cell.getTable(), borders);
			}

			triggerCellsByTable.put(cell.getTable(), cell);
			cell.addCollapsedBorders(all, borders);
		}

		if (triggerCellsByTable.isEmpty()) {
			return null;
		} else {
			Map<TableCellBox, List<CollapsedBorderSide>> result = new HashMap<TableCellBox, List<CollapsedBorderSide>>(
					triggerCellsByTable.size());

			for (TableCellBox cell : triggerCellsByTable.values()) {
				List<CollapsedBorderSide> borders = cellBordersByTable.get(cell.getTable());
				Collections.sort(borders);
				result.put(cell, borders);
			}

			return result;
		}
	}
	
    public DisplayListPageContainer collectInlineBlock(RenderingContext c, BlockBox bb, EnumSet<CollectFlags> noneOf) {
        DisplayListPageContainer pgInstructions = new DisplayListPageContainer(null);
        PagedBoxCollector boxCollector = createBoundedBoxCollector(c.getPageNo(), c.getPageNo());
        boxCollector.collect(c, bb.getContainingLayer(), bb, c.getPageNo(), c.getPageNo(), PagedBoxCollector.PAGE_BASE_ONLY);
        
        PageResult pgResult = boxCollector.getPageResult(c.getPageNo());
        processPage(c, bb.getContainingLayer(), pgResult, pgInstructions, /* includeFloats: */ false, c.getPageNo(), /* shadow page number: */ -1);
        
        return pgInstructions;
    }
	
	public DisplayListContainer collectFixed(RenderingContext c, Layer layer) {
        // This is called from the painter to collect fixed boxes just before paint.
        DisplayListContainer res = new MapDisplayListContainer(_pages.size(), 1);
        collect(c, layer, res, EnumSet.of(CollectFlags.INCLUDE_FIXED_BOXES));
        return res;
    }
	
	protected PagedBoxCollector createBoundedBoxCollector(int pgStart, int pgEnd) {
	    return new PagedBoxCollector(_pages, pgStart, pgEnd);
	}
	
	protected int findStartPage(RenderingContext c, Layer layer) {
	    int start = PagedBoxCollector.findStartPage(c, layer.getMaster(), _pages);
	    
	    // Floats maybe outside the master box.
	    for (BlockBox floater : layer.getFloats()) {
	        start = Math.min(start, PagedBoxCollector.findStartPage(c, floater, _pages));
	    }
	    
	    return start;
	}
	
	protected int findEndPage(RenderingContext c, Layer layer) {
	    int end = PagedBoxCollector.findEndPage(c, layer.getMaster(), _pages);
	    
	    // Floats may be outside the master box.
	    for (BlockBox floater : layer.getFloats()) {
	        end = Math.max(end, PagedBoxCollector.findEndPage(c, floater, _pages));
	    }
	    
	    return end;
	}
}

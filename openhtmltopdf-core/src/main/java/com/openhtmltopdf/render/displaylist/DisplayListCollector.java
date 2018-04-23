package com.openhtmltopdf.render.displaylist;

import java.util.ArrayList;
import java.util.Collections;
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
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector.PageResult;

public class DisplayListCollector {

	private final List<PageBox> _pages;

	public DisplayListCollector(List<PageBox> pages) {
		this._pages = pages;
	}

	private void collectLayers(RenderingContext c, List<Layer> layers, DisplayListContainer dlPages,
			List<PageBox> pages) {
		for (Layer layer : layers) {
			collect(c, layer, dlPages, pages);
		}
	}

	private void addItem(DisplayListOperation item, int pgStart, int pgEnd,
			DisplayListContainer dlPages) {
		for (int i = pgStart; i <= pgEnd; i++) {
			dlPages.getPageInstructions(i).addOp(item);
		}
	}

	public DisplayListContainer collectRoot(RenderingContext c, Layer rootLayer) {
		if (!rootLayer.isRootLayer()) {
			return null;
		}

		DisplayListContainer displayList = new DisplayListContainer(_pages.size());

		collect(c, rootLayer, displayList, _pages);

		return displayList;
	}

	private void collect(RenderingContext c, Layer layer, DisplayListContainer dlPages,
			List<PageBox> pages) {
		if (layer.getMaster().getStyle().isFixed()) {
			layer.positionFixedLayer(c); // TODO
		}

		if (layer.isRootLayer() && layer.getMaster().hasRootElementBackground(c)) {

			// IMPROVEMENT: If the background image doesn't cover every page,
			// we could perhaps optimize this.
			DisplayListOperation dlo = new PaintRootElementBackground(c, layer.getMaster());
			addItem(dlo, 0, dlPages.getNumPages() - 1, dlPages);
		}

		if (!layer.isInline() && ((BlockBox) layer.getMaster()).isReplaced()) {
			collectReplacedElementLayer(c, layer, dlPages, pages);
		} else {

			PagedBoxCollector collector = new PagedBoxCollector(pages);
			collector.collect(c, layer);

			if (!layer.isInline() && layer.getMaster() instanceof BlockBox) {
				collectLayerBackgroundAndBorder(c, layer, dlPages, pages);
			}

			if (layer.isRootLayer() || layer.isStackingContext()) {
				collectLayers(c, layer.getSortedLayers(Layer.NEGATIVE), dlPages, pages);
			}

			List<PageResult> pgResults = collector.getCollectedPageResults();

			for (int i = 0; i < pgResults.size(); i++) {
				PageResult pg = pgResults.get(i);
				DisplayListPageContainer dlPageList = dlPages.getPageInstructions(i);

				if (!pg.blocks().isEmpty()) {
					Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders = pg.tcells().isEmpty() ? null
							: collectCollapsedTableBorders(c, pg.tcells());
					DisplayListOperation dlo = new PaintBackgroundAndBorders(pg.blocks(), c, collapsedTableBorders);
					dlPageList.addOp(dlo);
				}

				if (layer.getFloats() != null && !layer.getFloats().isEmpty()) {
					for (int iflt = layer.getFloats().size() - 1; iflt >= 0; iflt--) {
						BlockBox floater = (BlockBox) layer.getFloats().get(iflt);
						collectFloatAsLayer(c, layer, pages, floater, dlPages);
					}
				}

				if (!pg.blocks().isEmpty()) {
					DisplayListOperation dlo = new PaintListMarkers(pg.blocks(), c);
					dlPageList.addOp(dlo);
				}

				if (!pg.inlines().isEmpty()) {
					DisplayListOperation dlo = new PaintInlineContent(pg.inlines(), c);
					dlPageList.addOp(dlo);
				}

				if (!pg.replaceds().isEmpty()) {
					DisplayListOperation dlo = new PaintReplacedElements(pg.replaceds(), c);
					dlPageList.addOp(dlo);
				}
			}

			if (layer.isRootLayer() || layer.isStackingContext()) {
				collectLayers(c, layer.collectLayers(Layer.AUTO), dlPages, pages);
				// TODO z-index: 0 layers should be painted atomically
				collectLayers(c, layer.getSortedLayers(Layer.ZERO), dlPages, pages);
				collectLayers(c, layer.getSortedLayers(Layer.POSITIVE), dlPages, pages);
			}
		}
	}

	private void collectFloatAsLayer(RenderingContext c, Layer layer, List<PageBox> pages, BlockBox startingPoint,
			DisplayListContainer dlPages) {
		PagedBoxCollector collector = new PagedBoxCollector(pages);

		collector.collect(c, layer, startingPoint, null);

		List<PageResult> pgResults = collector.getCollectedPageResults();

		for (int i = 0; i < pgResults.size(); i++) {
			PageResult pg = pgResults.get(i);
			DisplayListPageContainer dlPageList = dlPages.getPageInstructions(i);

			if (!pg.blocks().isEmpty()) {
				Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders = pg.tcells().isEmpty() ? null
						: collectCollapsedTableBorders(c, pg.tcells());
				DisplayListOperation dlo = new PaintBackgroundAndBorders(pg.blocks(), c, collapsedTableBorders);
				dlPageList.addOp(dlo);
			}

			if (!pg.blocks().isEmpty()) {
				DisplayListOperation dlo = new PaintListMarkers(pg.blocks(), c);
				dlPageList.addOp(dlo);
			}

			if (!pg.inlines().isEmpty()) {
				DisplayListOperation dlo = new PaintInlineContent(pg.inlines(), c);
				dlPageList.addOp(dlo);
			}

			if (!pg.replaceds().isEmpty()) {
				DisplayListOperation dlo = new PaintReplacedElements(pg.replaceds(), c);
				dlPageList.addOp(dlo);
			}
		}
	}

	private void collectLayerBackgroundAndBorder(RenderingContext c, Layer layer,
			DisplayListContainer dlPages, List<PageBox> pages) {

		DisplayListOperation dlo = new PaintLayerBackgroundAndBorder(c, layer.getMaster());
		int pgStart = PagedBoxCollector.findStartPage(c, layer.getMaster(), pages);
		int pgEnd = PagedBoxCollector.findEndPage(c, layer.getMaster(), pages);
		addItem(dlo, pgStart, pgEnd, dlPages);
	}

	private void collectReplacedElementLayer(RenderingContext c, Layer layer,
			DisplayListContainer dlPages, List<PageBox> pages) {

		if (layer.getMaster() instanceof BlockBox) {
			DisplayListOperation dlo = new PaintLayerBackgroundAndBorder(c, layer.getMaster());
			int pgStart = PagedBoxCollector.findStartPage(c, layer.getMaster(), pages);
			int pgEnd = PagedBoxCollector.findEndPage(c, layer.getMaster(), pages);
			addItem(dlo, pgStart, pgEnd, dlPages);
		}

		DisplayListOperation dlo = new PaintReplacedElement(c, (BlockBox) layer.getMaster());
		int pgStart = PagedBoxCollector.findStartPage(c, layer.getMaster(), pages);
		int pgEnd = PagedBoxCollector.findEndPage(c, layer.getMaster(), pages);
		addItem(dlo, pgStart, pgEnd, dlPages);
	}

	// Bit of a kludge here. We need to paint collapsed table borders according
	// to priority so (for example) wider borders float to the top and aren't
	// overpainted by thinner borders. This method takes the table cell boxes
	// (only those with collapsed border painting)
	// we're about to draw and returns a map with the last cell in a given table
	// we'll paint as a key and a sorted list of borders as values. These are
	// then painted after we've drawn the background for this cell.
	private Map<TableCellBox, List<CollapsedBorderSide>> collectCollapsedTableBorders(RenderingContext c,
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

}

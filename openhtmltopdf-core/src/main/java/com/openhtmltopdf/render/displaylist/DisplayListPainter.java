package com.openhtmltopdf.render.displaylist;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.layout.CollapsedBorderSide;
import com.openhtmltopdf.layout.InlinePaintable;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.OperatorClip;
import com.openhtmltopdf.render.OperatorSetClip;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.DisplayListCollector.CollectFlags;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;

public class DisplayListPainter {
    
    private void debugOnly(String msg, Object arg) {
        //System.out.println(msg + " : " + arg);
    }
	
	private void clip(RenderingContext c, OperatorClip clip) {
	    debugOnly("clipping", clip.getClip());
		c.getOutputDevice().pushClip(clip.getClip());
	}
	
	private void setClip(RenderingContext c, OperatorSetClip setclip) {
	    debugOnly("popping clip", null);
		c.getOutputDevice().popClip();
	}
	
	/**
	 * If the container is a table and it is set to <code>paginate</code> then update its header
	 * and footer position for this page.
	 */
	private void updateTableHeaderFooterPosition(RenderingContext c, BlockBox container) {
        if (container.getStyle().isTable()) {
            TableBox table = (TableBox) container;
            if (table.hasContentLimitContainer()) {
                table.updateHeaderFooterPosition(c);
            }
        }
	}
	
	private void paintBackgroundAndBorders(RenderingContext c, List<DisplayListItem> blocks,
			Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders) {

		for (DisplayListItem dli : blocks) {
			if (dli instanceof OperatorClip) {
				OperatorClip clip = (OperatorClip) dli;
				clip(c, clip);
			} else if (dli instanceof OperatorSetClip) {
				OperatorSetClip setClip = (OperatorSetClip) dli;
				setClip(c, setClip);
			} else {
				BlockBox box = (BlockBox) dli;
				
				updateTableHeaderFooterPosition(c, box);
				debugOnly("painting bg", box);
				box.paintBackground(c);
				box.paintBorder(c);

				if (collapsedTableBorders != null && box instanceof TableCellBox) {
					TableCellBox cell = (TableCellBox) box;

					if (cell.hasCollapsedPaintingBorder()) {
						List<CollapsedBorderSide> borders = collapsedTableBorders.get(cell);

						if (borders != null) {
							for (CollapsedBorderSide border : borders) {
								border.getCell().paintCollapsedBorder(c, border.getSide());
							}
						}
					}
				}
			}
		}
	}

	private void paintListMarkers(RenderingContext c, List<DisplayListItem> blocks) {
		for (DisplayListItem dli : blocks) {
			if (dli instanceof OperatorClip) {
				OperatorClip clip = (OperatorClip) dli;
				clip(c, clip);
			} else if (dli instanceof OperatorSetClip) {
				OperatorSetClip setClip = (OperatorSetClip) dli;
				setClip(c, setClip);
			} else {
				((BlockBox) dli).paintListMarker(c);
			}
		}
	}

	private void paintInlineContent(RenderingContext c, List<DisplayListItem> inlines) {
		for (DisplayListItem dli : inlines) {
			if (dli instanceof OperatorClip) {
				OperatorClip clip = (OperatorClip) dli;
				clip(c, clip);
			} else if (dli instanceof OperatorSetClip) {
				OperatorSetClip setClip = (OperatorSetClip) dli;
				setClip(c, setClip);
			} else if (dli instanceof BlockBox) {
			    // Inline blocks need to be painted as a layer.
			    BlockBox bb = (BlockBox) dli;
			    List<PageBox> pageBoxes = bb.getContainingLayer().getPages();
			    DisplayListCollector dlCollector = new DisplayListCollector(pageBoxes);
			    DisplayListPageContainer pageInstructions = dlCollector.collectInlineBlock(c, bb, EnumSet.noneOf(CollectFlags.class));
			
			    paint(c, pageInstructions);
			} else {
                InlinePaintable paintable = (InlinePaintable) dli;
                paintable.paintInline(c);
			}
		}
	}

	private void paintReplacedElements(RenderingContext c, List<DisplayListItem> replaceds) {
		for (DisplayListItem dli : replaceds) {
			if (dli instanceof OperatorClip) {
				OperatorClip clip = (OperatorClip) dli;
				clip(c, clip);
			} else if (dli instanceof OperatorSetClip) {
				OperatorSetClip setClip = (OperatorSetClip) dli;
				setClip(c, setClip);
			} else {
				BlockBox box = (BlockBox) dli;
				paintReplacedElement(c, box);
			}
		}
	}
	
    private void paintReplacedElement(RenderingContext c, BlockBox replaced) {
        
    	Rectangle contentBounds = replaced.getContentAreaEdge(
                replaced.getAbsX(), replaced.getAbsY(), c);
    	
        // Minor hack:  It's inconvenient to adjust for margins, border, padding during
        // layout so just do it here.
        Point loc = replaced.getReplacedElement().getLocation();
        if (contentBounds.x != loc.x || contentBounds.y != loc.y) {
            replaced.getReplacedElement().setLocation(contentBounds.x, contentBounds.y);
        }
        
        c.getOutputDevice().paintReplacedElement(c, replaced);
    }
    
    private void pushTransform(RenderingContext c, Box master) {
    	AffineTransform transform = TransformCreator.createPageCoordinatesTranform(c, master, c.getPage());
    	debugOnly("pushing transform", transform);
    	c.getOutputDevice().pushTransformLayer(transform);
    }
    
    private void popTransform(RenderingContext c, Box master) {
        debugOnly("popping transform", null);
    	c.getOutputDevice().popTransformLayer();
    }
    
    private void pushClipRect(RenderingContext c, Rectangle clip) {
        debugOnly("pushing clip rect", clip);
        c.getOutputDevice().pushClip(clip);
    }
    
    private void popClipRect(RenderingContext c) {
        debugOnly("popping clip rect", null);
        c.getOutputDevice().popClip();
    }
    
    private void paintFixed(RenderingContext c, Layer layer) {
    	layer.positionFixedLayer(c);

    	List<PageBox> pages = layer.getPages();
    	DisplayListCollector collector = new DisplayListCollector(pages);
        DisplayListContainer dlPages = collector.collectFixed(c, layer); 
        paint(c, dlPages.getPageInstructions(c.getPageNo()));
    }

	public void paint(RenderingContext c, DisplayListPageContainer pageOperations) {
		for (DisplayListOperation op : pageOperations.getOperations()) {

			if (op instanceof PaintRootElementBackground) {

				PaintRootElementBackground dlo = (PaintRootElementBackground) op;
				dlo.getRoot().paintRootElementBackground(c);

			} else if (op instanceof PaintLayerBackgroundAndBorder) {

				PaintLayerBackgroundAndBorder dlo = (PaintLayerBackgroundAndBorder) op;
				dlo.getMaster().paintBackground(c);
				dlo.getMaster().paintBorder(c);

			} else if (op instanceof PaintReplacedElement) {

				PaintReplacedElement dlo = (PaintReplacedElement) op;
				paintReplacedElement(c, dlo.getMaster());

			} else if (op instanceof PaintBackgroundAndBorders) {

				PaintBackgroundAndBorders dlo = (PaintBackgroundAndBorders) op;
				paintBackgroundAndBorders(c, dlo.getBlocks(), dlo.getCollapedTableBorders());
				//System.out.println("painting blocks: " + dlo.getBlocks());

			} else if (op instanceof PaintListMarkers) {

				PaintListMarkers dlo = (PaintListMarkers) op;
				paintListMarkers(c, dlo.getBlocks());

			} else if (op instanceof PaintInlineContent) {

				PaintInlineContent dlo = (PaintInlineContent) op;
				paintInlineContent(c, dlo.getInlines());
				//System.out.println("painting inlines: " + dlo.getInlines());

			} else if (op instanceof PaintReplacedElements) {

				PaintReplacedElements dlo = (PaintReplacedElements) op;
				paintReplacedElements(c, dlo.getReplaceds());

			} else if (op instanceof PaintPushTransformLayer) {
				
				PaintPushTransformLayer dlo = (PaintPushTransformLayer) op;
				pushTransform(c, dlo.getMaster());
				
			} else if (op instanceof PaintPopTransformLayer) {
				
				PaintPopTransformLayer dlo = (PaintPopTransformLayer) op;
				popTransform(c, dlo.getMaster());
				
			} else if (op instanceof PaintFixedLayer) {
				
				PaintFixedLayer dlo = (PaintFixedLayer) op;
				paintFixed(c, dlo.getLayer());
				
			} else if (op instanceof PaintPushClipRect) {
			    
			    PaintPushClipRect dlo = (PaintPushClipRect) op;
			    pushClipRect(c, dlo.getClipBox());
			    
			} else if (op instanceof PaintPopClipRect) {
			    
			    popClipRect(c);
			    
			}
		}
	}

}

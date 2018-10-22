package com.openhtmltopdf.render.simplepainter;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.layout.CollapsedBorderSide;
import com.openhtmltopdf.layout.InlinePaintable;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.DisplayListItem;
import com.openhtmltopdf.render.OperatorClip;
import com.openhtmltopdf.render.OperatorSetClip;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.DisplayListCollector;
import com.openhtmltopdf.render.displaylist.TransformCreator;

public class SimplePainter {
    private final int xTranslate;
    private final int yTranslate;
    
    public SimplePainter(int xtrans, int ytrans) {
        this.xTranslate = xtrans;
        this.yTranslate = ytrans;
    }
    
    private void debugOnly(String msg, Object arg) {
        //System.out.println(msg + " : " + arg);
    }
    
    public void paintLayer(RenderingContext c, Layer layer) {
        Box master = layer.getMaster();
        Rectangle parentClip = layer.getMaster().getParentClipBox(c, layer.getParent());
        
        if (parentClip != null) {
            c.getOutputDevice().pushClip(parentClip);
        }
        
        if (layer.hasLocalTransform()) {
            AffineTransform transform = TransformCreator.createPageMarginCoordinatesTransform(c, master, c.getPage(), xTranslate, yTranslate);
            c.getOutputDevice().pushTransformLayer(transform);
        }
        
        if (!layer.isInline() && ((BlockBox) master).isReplaced()) {
            // TODO
        } else {
            SimpleBoxCollector boxCollector = new SimpleBoxCollector();
            boxCollector.collect(c, layer);
            
            if (!layer.isInline() && master instanceof BlockBox) {
                paintLayerBackgroundAndBorder(c, master);
            }

            if (layer.isRootLayer() || layer.isStackingContext()) {
                paintLayers(c, layer.getSortedLayers(Layer.NEGATIVE));
            }
            
            Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders = boxCollector.tcells().isEmpty() ? null
                    : DisplayListCollector.collectCollapsedTableBorders(c, boxCollector.tcells());
            
            paintBackgroundsAndBorders(c, boxCollector.blocks(), collapsedTableBorders);
            paintFloats(c, layer.getFloats());
            paintListMarkers(c, boxCollector.listItems());
            paintInlineContent(c, boxCollector.inlines());
            paintReplacedElements(c, boxCollector.replaceds());
            
            if (layer.isRootLayer() || layer.isStackingContext()) {
                paintLayers(c, layer.collectLayers(Layer.AUTO));
                // TODO z-index: 0 layers should be painted atomically
                paintLayers(c, layer.getSortedLayers(Layer.ZERO));
                paintLayers(c, layer.getSortedLayers(Layer.POSITIVE));
            }
        }

        if (layer.hasLocalTransform()) {
            c.getOutputDevice().popTransformLayer();
        }
        
        if (parentClip != null) {
            c.getOutputDevice().popClip();
        }
    }
    
    private void paintLayerBackgroundAndBorder(RenderingContext c, Box master) {
        master.paintBackground(c);
        master.paintBorder(c);
    }
    
    private void clip(RenderingContext c, OperatorClip clip) {
        debugOnly("clipping", clip.getClip());
        c.getOutputDevice().pushClip(clip.getClip());
    }
    
    private void setClip(RenderingContext c, OperatorSetClip setclip) {
        debugOnly("popping clip", null);
        c.getOutputDevice().popClip();
    }

    private void paintBackgroundsAndBorders(RenderingContext c, List<DisplayListItem> blocks, Map<TableCellBox, List<CollapsedBorderSide>> collapsedTableBorders) {
        for (DisplayListItem dli : blocks) {
            if (dli instanceof OperatorClip) {
                OperatorClip clip = (OperatorClip) dli;
                clip(c, clip);
            } else if (dli instanceof OperatorSetClip) {
                OperatorSetClip setClip = (OperatorSetClip) dli;
                setClip(c, setClip);
            } else {
                BlockBox box = (BlockBox) dli;
                
                // TODO: updateTableHeaderFooterPosition(c, box);
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

    private void paintListMarkers(RenderingContext c, List<DisplayListItem> listItems) {
        for (DisplayListItem dli : listItems) {
            if (dli instanceof OperatorClip) {
                OperatorClip clip = (OperatorClip) dli;
                clip(c, clip);
            } else if (dli instanceof OperatorSetClip) {
                OperatorSetClip setClip = (OperatorSetClip) dli;
                setClip(c, setClip);
            } else {
                debugOnly("Painting list item", dli);
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
                paintAsLayer(c, bb);
            } else {
                InlinePaintable paintable = (InlinePaintable) dli;
                debugOnly("Painting Inline", paintable);
                paintable.paintInline(c);
            }
        }
        
    }

    private void paintReplacedElements(RenderingContext c, List<DisplayListItem> replaceds) {
        // TODO Auto-generated method stub
        
    }

    private void paintFloats(RenderingContext c, List<BlockBox> floaters) {
        for (int i = floaters.size() - 1; i >= 0; i--) {
            paintAsLayer(c, floaters.get(i));
        }
    }
    
    private void paintLayers(RenderingContext c, List<Layer> layers) {
        for (Layer layer : layers) {
            paintLayer(c, layer);
        }
    }
    
    public void paintAsLayer(RenderingContext c, BlockBox startingPoint) {
        SimpleBoxCollector collector = new SimpleBoxCollector();
        collector.collect(c, startingPoint.getContainingLayer(), startingPoint);

        Map<TableCellBox, List<CollapsedBorderSide>>  collapsedTableBorders = collector.tcells().isEmpty() ? null : DisplayListCollector.collectCollapsedTableBorders(c, collector.tcells());

        paintBackgroundsAndBorders(c, collector.blocks(), collapsedTableBorders);
        paintListMarkers(c, collector.listItems());
        paintInlineContent(c, collector.inlines());
        paintReplacedElements(c, collector.replaceds());
    }
}

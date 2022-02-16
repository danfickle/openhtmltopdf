/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2005 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.render;

import java.awt.RenderingHints;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.FSImage;

/**
 * A utility class to paint list markers (all types).
 * @see MarkerData 
 */
public class ListItemPainter {
    public static void paint(RenderingContext c, BlockBox box) {
        MarkerData markerData = box.getMarkerData();

        if (markerData == null ||
            (!c.isInPageMargins() &&
             !isInVisiblePageArea(c, c.getPage(), markerData.getStructMetrics(), box))) {
            return;
        }

        if (markerData.getImageMarker() != null) {
            drawImage(c, box, markerData);
        } else {
            CalculatedStyle style = box.getStyle();
            IdentValue listStyle = style.getIdent(CSSName.LIST_STYLE_TYPE);
            
            c.getOutputDevice().setColor(style.getColor());
    
            if (markerData.getGlyphMarker() != null) {
                drawGlyph(c, box, style, listStyle);
            } else if (markerData.getTextMarker() != null){
                drawText(c, box, listStyle);
            }
        }
    }

    private static void drawImage(RenderingContext c, BlockBox box, MarkerData markerData) {
        FSImage img = null;
        MarkerData.ImageMarker marker = markerData.getImageMarker();
        img = marker.getImage();
        if (img != null) {
            StrutMetrics strutMetrics = box.getMarkerData().getStructMetrics();
            int x = getReferenceX(c, box);
            // FIXME: findbugs possible loss of precision, cf. int / (float)2
            x += -marker.getLayoutWidth() +
                    (marker.getLayoutWidth() / 2 - img.getWidth() / 2);
            c.getOutputDevice().drawImage(img, 
                    x,
                    (int)(getReferenceBaseline(c, box)
                        - strutMetrics.getAscent() / 2 - img.getHeight() / 2), box.getStyle().isImageRenderingInterpolate());
        }
    }
    
    private static int getReferenceX(RenderingContext c, BlockBox box) {
        MarkerData markerData = box.getMarkerData();
        
        if (markerData.getReferenceLine() != null) {
            return markerData.getReferenceLine().getAbsX();
        } else {
            return box.getAbsX() + (int)box.getMargin(c).left();
        }
    }
    
    private static int getReferenceBaseline(RenderingContext c, BlockBox box) {
        MarkerData markerData = box.getMarkerData();
        StrutMetrics strutMetrics = box.getMarkerData().getStructMetrics();
        
        if (markerData.getReferenceLine() != null) {
            return markerData.getReferenceLine().getAbsY() + strutMetrics.getBaseline();
        } else {
            return box.getAbsY() + box.getTy() + strutMetrics.getBaseline();
        }
    }

    private static void drawGlyph(RenderingContext c, BlockBox box, 
            CalculatedStyle style, IdentValue listStyle) {
        // save the old AntiAliasing setting, then force it on
        Object aa_key = c.getOutputDevice().getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        c.getOutputDevice().setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // calculations for bullets
        MarkerData markerData = box.getMarkerData();
        StrutMetrics strutMetrics = markerData.getStructMetrics();
        MarkerData.GlyphMarker marker = markerData.getGlyphMarker();
        int x = getReferenceX(c, box);
        // see issue 478. To be noted, the X positioning does not consider the available padding space
        // (like all the browsers it seems), so if the font is too big, the list decoration will be cut or outside
        // the viewport.
        if (style.getDirection() == IdentValue.LTR) {
            x += -marker.getLayoutWidth() + marker.getDiameter() * 1.1;
        }
        if (style.getDirection() == IdentValue.RTL){
            x += markerData.getReferenceLine().getWidth() + marker.getDiameter() * 1.1;
        }

        // see issue https://github.com/danfickle/openhtmltopdf/issues/478#issuecomment-682066113
        int bottomLine = getReferenceBaseline(c, box);
        int top = bottomLine - (int) (strutMetrics.getAscent() / 1.5);

        int y = bottomLine - (bottomLine-top) / 2 - marker.getDiameter() / 2 ;
        if (listStyle == IdentValue.DISC) {
            c.getOutputDevice().fillOval(x, y, marker.getDiameter(), marker.getDiameter());
        } else if (listStyle == IdentValue.SQUARE) {
            c.getOutputDevice().fillRect(x, y, marker.getDiameter(), marker.getDiameter());
        } else if (listStyle == IdentValue.CIRCLE) {
            c.getOutputDevice().drawOval(x, y, marker.getDiameter(), marker.getDiameter());
        }

        // restore the old AntiAliasing setting
        c.getOutputDevice().setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                aa_key == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa_key);
    }

    /**
     * The list item box may go over two pages. Therefore this method checks
     * if the marker is in the visible zone of the current page.
     */
    private static boolean isInVisiblePageArea(RenderingContext c, PageBox page, StrutMetrics metrics, BlockBox box) {
        float bs = getReferenceBaseline(c, box);
        float topY = bs - metrics.getAscent();
        float bottomY = bs + metrics.getDescent();
        float pageTop = page.getPaintingTop();
        float pageBottom = page.getPaintingBottom();

        return (topY >= pageTop && topY <= pageBottom) ||
               (bottomY >= pageTop && bottomY <= pageBottom);
    }

    private static void drawText(RenderingContext c, BlockBox box, IdentValue listStyle) {
        MarkerData.TextMarker text = box.getMarkerData().getTextMarker();
        int x = getReferenceX(c, box);
        int y = getReferenceBaseline(c, box);

        // calculations for numbered lists
        MarkerData markerData = box.getMarkerData();

        // Chrome uses the direction determined for the ol for all list-items.
        IdentValue direction = box.getParent().getStyle().getDirection();

        if (direction == IdentValue.RTL){
            x = markerData.getReferenceLine() != null ?
                 x + markerData.getReferenceLine().getWidth() :
                 box.getParent().getAbsX() + box.getParent().getWidth() - (int) box.getParent().getPadding(c).right();
        } else {
            assert direction == IdentValue.LTR || direction == IdentValue.AUTO;
            x += -text.getLayoutWidth();
        }

        c.getOutputDevice().setColor(box.getStyle().getColor());
        if (c.getOutputDevice() instanceof AbstractOutputDevice) {
            ((AbstractOutputDevice) c.getOutputDevice()).setFontSpecification(box.getStyle().getFontSpecification());
        }
        c.getOutputDevice().setFont(box.getStyle().getFSFont(c));
        c.getTextRenderer().drawString(
                c.getOutputDevice(), text.getText(), x, y);
    }
}

/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.render;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.BackgroundPosition;
import com.openhtmltopdf.css.style.BackgroundSize;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CalculatedStyle.BackgroundContainer;
import com.openhtmltopdf.css.style.CalculatedStyle.BackgroundImageType;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.FSLinearGradient;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import java.awt.*;
import java.awt.geom.Area;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * An abstract implementation of an {@link OutputDevice}.  It provides complete
 * implementations for many <code>OutputDevice</code> methods.
 */
public abstract class AbstractOutputDevice implements OutputDevice {

    private FontSpecification _fontSpec;

    protected abstract void drawLine(int x1, int y1, int x2, int y2);
    
    @Override
    public void drawText(RenderingContext c, InlineText inlineText) {
        InlineLayoutBox iB = inlineText.getParent();
        String text = inlineText.isEndsOnSoftHyphen() ? inlineText.getSubstring() + '-' : inlineText.getSubstring();

        // We reorder text here for RTL.
        if (inlineText.getTextDirection() == BidiSplitter.RTL) {
        	BidiReorderer bidi = c.getBidiReorderer();
        	text = bidi.reorderRTLTextToLTR(text);
        }
       
        if (text != null && text.length() > 0) {
            setColor(iB.getStyle().getColor());
            setFontSpecification(iB.getStyle().getFontSpecification());
            setFont(iB.getStyle().getFSFont(c));
            if (inlineText.getLetterSpacing() != 0f) {
                JustificationInfo info = new JustificationInfo();
                info.setNonSpaceAdjust(inlineText.getLetterSpacing());
                info.setSpaceAdjust(inlineText.getLetterSpacing());
                c.getTextRenderer().drawString(
                        c.getOutputDevice(),
                        text,
                        iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline(),
                        info);
            } else if (inlineText.getParent().getStyle().isTextJustify()) {
                // NOTE: Use of letter-spacing turns off justification
                JustificationInfo info = inlineText.getParent().getLineBox().getJustificationInfo();

                if (info != null) {
                    c.getTextRenderer().drawString(
                            c.getOutputDevice(),
                            text,
                            iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline(),
                            info);
                } else {
                    c.getTextRenderer().drawString(
                            c.getOutputDevice(),
                            text,
                            iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline());
                }
            } else {
                c.getTextRenderer().drawString(
                        c.getOutputDevice(),
                        text,
                        iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline());
            }
        }

        if (c.debugDrawFontMetrics()) {
            drawFontMetrics(c, inlineText);
        }
    }

    private void drawFontMetrics(RenderingContext c, InlineText inlineText) {
        InlineLayoutBox iB = inlineText.getParent();
        String text = inlineText.getSubstring();

        setColor(new FSRGBColor(0xFF, 0x33, 0xFF));

        FSFontMetrics fm = iB.getStyle().getFSFontMetrics(null);
        int width = c.getTextRenderer().getWidth(
                c.getFontContext(),
                iB.getStyle().getFSFont(c), text);
        int x = iB.getAbsX() + inlineText.getX();
        int y = iB.getAbsY() + iB.getBaseline();

        drawLine(x, y, x + width, y);

        y += (int) Math.ceil(fm.getDescent());
        drawLine(x, y, x + width, y);

        y -= (int) Math.ceil(fm.getDescent());
        y -= (int) Math.ceil(fm.getAscent());
        drawLine(x, y, x + width, y);
    }

    @Override
    public void drawTextDecoration(
            RenderingContext c, InlineLayoutBox iB, TextDecoration decoration) {
        setColor(iB.getStyle().getColor());

        Rectangle edge = iB.getContentAreaEdge(iB.getAbsX(), iB.getAbsY(), c);

        fillRect(edge.x, iB.getAbsY() + decoration.getOffset(),
                    edge.width, decoration.getThickness());
    }

    @Override
    public void drawTextDecoration(RenderingContext c, LineBox lineBox) {
        setColor(lineBox.getStyle().getColor());
        Box parent = lineBox.getParent();
        List<TextDecoration> decorations = lineBox.getTextDecorations();
        for (Iterator<TextDecoration> i = decorations.iterator(); i.hasNext(); ) {
            TextDecoration textDecoration = i.next();
            if (parent.getStyle().isIdent(
                    CSSName.FS_TEXT_DECORATION_EXTENT, IdentValue.BLOCK)) {
                fillRect(
                    lineBox.getAbsX(),
                    lineBox.getAbsY() + textDecoration.getOffset(),
                    parent.getAbsX() + parent.getTx() + parent.getContentWidth() - lineBox.getAbsX(),
                    textDecoration.getThickness());
            } else {
                fillRect(
                    lineBox.getAbsX(), lineBox.getAbsY() + textDecoration.getOffset(),
                    lineBox.getContentWidth(),
                    textDecoration.getThickness());
            }
        }
    }

    @Override
    public void drawDebugOutline(RenderingContext c, Box box, FSColor color) {
        setColor(color);
        Rectangle rect = box.getMarginEdge(box.getAbsX(), box.getAbsY(), c, 0, 0);
        rect.height -= 1;
        rect.width -= 1;
        drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void paintCollapsedBorder(
            RenderingContext c, BorderPropertySet border, Rectangle bounds, int side) {
        BorderPainter.paint(bounds, side, border, c, 0, false);
    }

    @Override
    public void paintBorder(RenderingContext c, Box box) {
        if (! box.getStyle().isVisible(c, box)) {
            return;
        }

        Rectangle borderBounds = box.getPaintingBorderEdge(c);

        BorderPainter.paint(borderBounds, box.getBorderSides(), box.getBorder(c), c, 0, true);
    }

    @Override
    public void paintBorder(RenderingContext c, CalculatedStyle style, Rectangle edge, int sides) {
        BorderPainter.paint(edge, sides, style.getBorder(c), c, 0, true);
    }

    private FSImage getBackgroundImage(PropertyValue bgImage, RenderingContext c) {
        if (bgImage.getIdentValue() != IdentValue.NONE) {
            String uri = bgImage.getStringValue();

            try {
                return c.getUac().getImageResource(uri).getImage();
            } catch (Exception ex) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_FAILED_TO_LOAD_BACKGROUND_IMAGE_AT_URI, uri, ex);
            }
        }

        return null;
    }

    @Override
    public void paintBackground(
            RenderingContext c, CalculatedStyle style,
            Rectangle bounds, Rectangle bgImageContainer, BorderPropertySet border) {
        paintBackground0(c, style, bounds, bgImageContainer, border);
    }

    @Override
    public void paintBackground(RenderingContext c, Box box) {
        if (! box.getStyle().isVisible(c, box)) {
            return;
        }

        Rectangle backgroundBounds = box.getPaintingBorderEdge(c);
        BorderPropertySet border = box.getStyle().getBorder(c);
        paintBackground0(c, box.getStyle(), backgroundBounds, backgroundBounds, border);
    }

    private void paintBackground0(
            RenderingContext c, CalculatedStyle style,
            Rectangle backgroundBounds, Rectangle bgImageContainer,
            BorderPropertySet border) {

        if (!style.isHasBackground()) {
            return;
        }

        FSColor backgroundColor = style.getBackgroundColor();
        List<BackgroundContainer> bgImages = style.getBackgroundImages();

        Shape borderBoundsShape = BorderPainter.generateBorderBounds(backgroundBounds, border, true);

        // FIXME for issue 396 - generating an Area for a shape with curves is very very slow and
        // memory intensive. However, not generating an area for simple squares breaks many tests.
        // Therefore, for now, we just don't use an area if there are border radii present.
        Area borderBounds = border.hasBorderRadius() && c.isFastRenderer() ? null : new Area(borderBoundsShape);

        if (style.isHasBackgroundImage()) {
        	pushClip(borderBounds != null ? borderBounds : borderBoundsShape);
        }

        if (backgroundColor != null && backgroundColor != FSRGBColor.TRANSPARENT) {
            setColor(backgroundColor);
            fill(borderBounds != null ? borderBounds : borderBoundsShape);
        }

        for (BackgroundContainer bgImage : bgImages) {
            if (bgImage.type == BackgroundImageType.GRADIENT) {
                FSLinearGradient backgroundLinearGradient = 
                        style.getLinearGradient(bgImage.imageGradientOrNone, c, (int) (bgImageContainer.width - border.width()), (int) (bgImageContainer.height - border.height()));

                if (backgroundLinearGradient != null) {
                    Dimension xyoff = calcInitialXYOff(bgImage, bgImageContainer, border, style, c);

                    int xoff = xyoff.width;
                    int yoff = xyoff.height;

                    drawLinearGradient(backgroundLinearGradient, new Rectangle(xoff, yoff, bgImageContainer.width, bgImageContainer.height));
                }
            } else if (bgImage.type == BackgroundImageType.NONE) {
                // Do nothing...
            } else {
                assert bgImage.type == BackgroundImageType.URI;

                FSImage backgroundImage = getBackgroundImage(bgImage.imageGradientOrNone, c);

                // If the image width or height is zero, then there's nothing to draw.
                // Also prevents infinte loop when trying to tile an image with zero size.
                if (backgroundImage != null && backgroundImage.getHeight() != 0 && backgroundImage.getWidth() != 0) {
                    drawBgImage(c, style, backgroundBounds, bgImageContainer, border, backgroundImage, bgImage);
                }
            }
        }

        if (style.isHasBackgroundImage()) {
        	popClip();
        }
    }

    private Dimension calcInitialXYOff(
            BackgroundContainer bgImage,
            Rectangle bgImageContainer,
            BorderPropertySet border,
            CalculatedStyle style,
            RenderingContext c) {

        Rectangle localBGImageContainer = bgImageContainer;

        int xoff = localBGImageContainer.x;
        int yoff = localBGImageContainer.y;

        if (border != null) {
            xoff += (int) border.left();
            yoff += (int) border.top();
        }

        return new Dimension(xoff, yoff);
    }

    private void drawBgImage(
            RenderingContext c,
            CalculatedStyle style,
            Rectangle backgroundBounds,
            Rectangle bgImageContainer,
            BorderPropertySet border,
            FSImage backgroundImage,
            BackgroundContainer bgImage) {

        Dimension xyoff = calcInitialXYOff(bgImage, bgImageContainer, border, style, c);

        int xoff = xyoff.width;
        int yoff = xyoff.height;

        Rectangle localBGImageContainer = bgImageContainer;

        scaleBackgroundImage(c, style, localBGImageContainer, backgroundImage, bgImage);

        float imageWidth = backgroundImage.getWidth();
        float imageHeight = backgroundImage.getHeight();

        BackgroundPosition position = bgImage.backgroundPosition;

        xoff += calcOffset(c, style, position.getHorizontal(), localBGImageContainer.width, imageWidth);
        yoff += calcOffset(c, style, position.getVertical(), localBGImageContainer.height, imageHeight);

        boolean hrepeat = style.isHorizontalBackgroundRepeat(bgImage.backgroundRepeat);
        boolean vrepeat = style.isVerticalBackgroundRepeat(bgImage.backgroundRepeat);

        if (!hrepeat && !vrepeat) {
            Rectangle imageBounds = new Rectangle(xoff, yoff, (int) imageWidth, (int) imageHeight);
            if (imageBounds.intersects(backgroundBounds)) {
                drawImage(backgroundImage, xoff, yoff, style.isImageRenderingInterpolate());
            }
        } else if (hrepeat && vrepeat) {
            paintTiles(backgroundImage, adjustTo(backgroundBounds.x, xoff, (int) imageWidth),
                    adjustTo(backgroundBounds.y, yoff, (int) imageHeight), backgroundBounds.x + backgroundBounds.width,
                    backgroundBounds.y + backgroundBounds.height, style.isImageRenderingInterpolate());
        } else if (hrepeat) {
            xoff = adjustTo(backgroundBounds.x, xoff, (int) imageWidth);
            Rectangle imageBounds = new Rectangle(xoff, yoff, (int) imageWidth, (int) imageHeight);
            if (imageBounds.intersects(backgroundBounds)) {
                paintHorizontalBand(backgroundImage, xoff, yoff, backgroundBounds.x + backgroundBounds.width,
                        style.isImageRenderingInterpolate());
            }
        } else if (vrepeat) {
            yoff = adjustTo(backgroundBounds.y, yoff, (int) imageHeight);
            Rectangle imageBounds = new Rectangle(xoff, yoff, (int) imageWidth, (int) imageHeight);
            if (imageBounds.intersects(backgroundBounds)) {
                paintVerticalBand(backgroundImage, xoff, yoff, backgroundBounds.y + backgroundBounds.height,
                        style.isImageRenderingInterpolate());
            }
        }
    }

    private int adjustTo(int target, int current, int imageDim) {
        int result = current;
        if (result > target) {
            while (result > target) {
                result -= imageDim;
            }
        } else if (result < target) {
            while (result < target) {
                result += imageDim;
            }
            if (result != target) {
                result -= imageDim;
            }
        }
        return result;
    }

    private void paintTiles(FSImage image, int left, int top, int right, int bottom, boolean interpolate) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = left; x < right; x+= width) {
            for (int y = top; y < bottom; y+= height) {
                drawImage(image, x, y, interpolate);
            }
        }
    }

    private void paintVerticalBand(FSImage image, int left, int top, int bottom, boolean interpolate) {
        int height = image.getHeight();

        for (int y = top; y < bottom; y+= height) {
            drawImage(image, left, y, interpolate);
        }
    }

    private void paintHorizontalBand(FSImage image, int left, int top, int right, boolean interpolate) {
        int width = image.getWidth();

        for (int x = left; x < right; x+= width) {
            drawImage(image, x, top, interpolate);
        }
    }

    private int calcOffset(CssContext c, CalculatedStyle style, PropertyValue value, float boundsDim, float imageDim) {
        if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) {
            float percent = value.getFloatValue() / 100.0f;
            return Math.round(boundsDim*percent - imageDim*percent);
        } else { /* it's a <length> */
            return (int)LengthValue.calcFloatProportionalValue(
                    style,
                    CSSName.BACKGROUND_POSITION,
                    value.getCssText(),
                    value.getFloatValue(),
                    value.getPrimitiveType(),
                    0,
                    c);
        }
    }

    private void scaleBackgroundImage(CssContext c, CalculatedStyle style, Rectangle backgroundContainer, FSImage image, BackgroundContainer bgImage) {
        BackgroundSize backgroundSize = bgImage.backgroundSize;

        if (! backgroundSize.isBothAuto()) {
            if (backgroundSize.isCover() || backgroundSize.isContain()) {
                int testHeight = (int)((double)image.getHeight() * backgroundContainer.width / image.getWidth());
                if (backgroundSize.isContain()) {
                    if (testHeight > backgroundContainer.height) {
                        image.scale(-1, backgroundContainer.height);
                    } else {
                        image.scale(backgroundContainer.width, -1);
                    }
                } else if (backgroundSize.isCover()) {
                    if (testHeight > backgroundContainer.height) {
                        image.scale(backgroundContainer.width, -1);
                    } else {
                        image.scale(-1, backgroundContainer.height);
                    }
                }
            } else {
                int scaledWidth = calcBackgroundSizeLength(c, style, backgroundSize.getWidth(), backgroundContainer.width);
                int scaledHeight = calcBackgroundSizeLength(c, style, backgroundSize.getHeight(), backgroundContainer.height);

                image.scale(scaledWidth, scaledHeight);
            }
        }
    }

    private int calcBackgroundSizeLength(CssContext c, CalculatedStyle style, PropertyValue value, float boundsDim) {
        if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) { // 'auto'
            return -1;
        } else if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) {
            float percent = value.getFloatValue() / 100.0f;
            return Math.round(boundsDim*percent);
        } else {
            return (int)LengthValue.calcFloatProportionalValue(
                    style,
                    CSSName.BACKGROUND_SIZE,
                    value.getCssText(),
                    value.getFloatValue(),
                    value.getPrimitiveType(),
                    0,
                    c);
        }
    }

    /**
     * Gets the FontSpecification for this AbstractOutputDevice.
     *
     * @return current FontSpecification.
     */
    public FontSpecification getFontSpecification() {
	return _fontSpec;
    }

    /**
     * Sets the FontSpecification for this AbstractOutputDevice.
     *
     * @param fs current FontSpecification.
     */
    public void setFontSpecification(FontSpecification fs) {
	_fontSpec = fs;
    }

    @Override
    public boolean isPDF() {
        return false;
    }
}

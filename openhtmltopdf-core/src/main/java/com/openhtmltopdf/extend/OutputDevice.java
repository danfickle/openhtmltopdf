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
package com.openhtmltopdf.extend;

import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.render.*;
import com.openhtmltopdf.render.AbstractOutputDevice.ClipInfo;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.util.List;

public interface OutputDevice {
	public void setPaint(Paint paint);

	// Required for CSS transforms.

	/**
	 * Apply the given transform on top of the current one in the PDF graphics stream.
	 * This is a cumulative operation. You should popTransform after the box and children are painted.
	 * @return the list of inverse transforms to undo the effect of this transform
	 */
	@Deprecated
	public List<AffineTransform> pushTransforms(List<AffineTransform> transforms);
	
	@Deprecated
	public void popTransforms(List<AffineTransform> inverse);
	
	@Deprecated
	float getAbsoluteTransformOriginX();
	
	@Deprecated
	float getAbsoluteTransformOriginY();
	
	// And the rest.
    public void drawText(RenderingContext c, InlineText inlineText);
    public void drawSelection(RenderingContext c, InlineText inlineText);
    
    public void drawTextDecoration(RenderingContext c, LineBox lineBox);
    public void drawTextDecoration(
            RenderingContext c, InlineLayoutBox iB, TextDecoration decoration);
    
    public void paintBorder(RenderingContext c, Box box);
    public void paintBorder(RenderingContext c, CalculatedStyle style, 
            Rectangle edge, int sides);
    public void paintCollapsedBorder(
            RenderingContext c, BorderPropertySet border, Rectangle bounds, int side);
    
    public void paintBackground(RenderingContext c, Box box);
    public void paintBackground(
            RenderingContext c, CalculatedStyle style, 
            Rectangle bounds, Rectangle bgImageContainer,
            BorderPropertySet border);
    
    public void paintReplacedElement(RenderingContext c, BlockBox box);
    
    public void drawDebugOutline(RenderingContext c, Box box, FSColor color);
    
    public void setFont(FSFont font);
    
    public void setColor(FSColor color);
    
    public void drawRect(int x, int y, int width, int height);
    public void drawOval(int x, int y, int width, int height);
    
    public void drawBorderLine(Shape bounds, int side, int width, boolean solid);
    
    public void drawImage(FSImage image, int x, int y, boolean interpolate);

    public void draw(Shape s);
    public void fill(Shape s);
    public void fillRect(int x, int y, int width, int height);
    public void fillOval(int x, int y, int width, int height);
    
    @Deprecated
    public void clip(Shape s);
    
    @Deprecated
    public Shape getClip();
    
    @Deprecated
    public void setClip(Shape s);
    
    public void translate(double tx, double ty);
    
    public void setStroke(Stroke s);
    public Stroke getStroke();

    public Object getRenderingHint(Key key);
    public void setRenderingHint(Key key, Object value);
    
    @Deprecated
    public boolean isSupportsSelection();
    
    public boolean isSupportsCMYKColors();

    /**
     * Draw something using a Graphics2D at the given rectangle.
     */
    public void drawWithGraphics(float x, float y, float width, float height, OutputDeviceGraphicsDrawer renderer);

    public boolean isPDF();

    /**
     * Applies a transform on the output device. This is a cumulativew operation.
	 * <p></p>
	 * <b>NOTE</b>: The current implementation shares a stack
	 * between transforms and clips, so calls to {@link #pushTransformLayer(AffineTransform)},
	 * {@link #popTransformLayer()}, {@link #pushClip(Shape)} and {@link #popClip()} <strong>MUST</strong>
	 * be nested correctly.
     */
	public void pushTransformLayer(AffineTransform transform);
	
	/**
	 * Reverts the transform on the stack.
	 * <p></p>
	 * <b>NOTE</b>: The current implementation shares a stack
	 * between transforms and clips, so calls to {@link #pushTransformLayer(AffineTransform)},
	 * {@link #popTransformLayer()}, {@link #pushClip(Shape)} and {@link #popClip()} <strong>MUST</strong>
	 * be nested correctly.
	 */
	public void popTransformLayer();

	/**
	 * Applies a clip on the output device. This is a cumulative operation.
	 * The fast renderer MUST use <code>pushClip</code> and <code>popClip</code>
	 * in preference to <code>clip</code> and <code>setClip</code>.
	 * <p></p>
	 * <b>NOTE</b>: The current implementation shares a stack
	 * between transforms and clips, so calls to {@link #pushTransformLayer(AffineTransform)},
	 * {@link #popTransformLayer()}, {@link #pushClip(Shape)} and {@link #popClip()} <strong>MUST</strong>
	 * be nested correctly.
	 */
	public void pushClip(Shape s);

	/**
	 * Reverts the last clip on the stack. 
	 * The fast renderer MUST use <code>pushClip</code> and <code>popClip</code>
	 * in preference to <code>clip</code> and <code>setClip</code>.
	 * <p></p>
	 * <b>NOTE</b>: The current implementation shares a stack
	 * between transforms and clips, so calls to {@link #pushTransformLayer(AffineTransform)},
	 * {@link #popTransformLayer()}, {@link #pushClip(Shape)} and {@link #popClip()} <strong>MUST</strong>
	 * be nested correctly.
	 */
	public void popClip();
	
	/**
	 * The new (2018) fast renderer is in use.
	 */
	public boolean isFastRenderer();
}

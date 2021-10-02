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
package com.openhtmltopdf.java2d;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.derived.FSLinearGradient;
import com.openhtmltopdf.css.style.derived.FSLinearGradient.StopPoint;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;
import com.openhtmltopdf.java2d.image.AWTFSImage;
import com.openhtmltopdf.java2d.image.ImageReplacedElement;
import com.openhtmltopdf.render.*;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.Deque;

public class Java2DOutputDevice extends AbstractOutputDevice implements OutputDevice {
    private final Deque<Shape> _clipStack = new ArrayDeque<>();
    private final Deque<AffineTransform> _transformStack = new ArrayDeque<>();

    private Graphics2D _graphics;
    private Java2DFont _font;

    public Java2DOutputDevice(Graphics2D layoutGraphics) {
    	this._graphics = layoutGraphics;
    }

    @Override
    @Deprecated
    public void drawSelection(RenderingContext c, InlineText inlineText) {
    }

    @Override
    public void drawBorderLine(
            Shape bounds, int side, int lineWidth, boolean solid) {
        draw(bounds);
    }

    @Override
    public void paintReplacedElement(RenderingContext c, BlockBox box) {
        ReplacedElement replaced = box.getReplacedElement();
        if (replaced instanceof ImageReplacedElement) {
            Image image = ((ImageReplacedElement)replaced).getImage();
            
            Point location = replaced.getLocation();
            _graphics.drawImage(
                    image, (int)location.getX(), (int)location.getY(), null);
		} else if (replaced instanceof Java2DRendererBuilder.Graphics2DPaintingReplacedElement) {
			Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
			((Java2DRendererBuilder.Graphics2DPaintingReplacedElement) replaced).paint(this, c, contentBounds.x,
					contentBounds.y, contentBounds.width, contentBounds.height);
		}
    }
    
    @Override
    public void setColor(FSColor color) {
        if (color instanceof FSRGBColor) {
            FSRGBColor rgb = (FSRGBColor) color;
            _graphics.setColor(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue()));
        } else {
            throw new RuntimeException("internal error: unsupported color class " + color.getClass().getName());
        }
    }

    @Override
    protected void drawLine(int x1, int y1, int x2, int y2) {
        _graphics.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        _graphics.drawRect(x, y, width, height);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        _graphics.fillRect(x, y, width, height);
    }

    @Override
    public void translate(double tx, double ty) {
        _graphics.translate(tx, ty);
    }

    public Graphics2D getGraphics() {
        return _graphics;
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        _graphics.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        _graphics.fillOval(x, y, width, height);
    }

    @Override
    public Object getRenderingHint(Key key) {
        return _graphics.getRenderingHint(key);
    }

    @Override
    public void setRenderingHint(Key key, Object value) {
        _graphics.setRenderingHint(key, value);
    }
    
    @Override
    public void setFont(FSFont font) {
        this._font = (Java2DFont) font;
        _graphics.setFont(this._font.getAWTFonts().get(0));
    }

    public Java2DFont getFont() {
    	return this._font;
    }
    
    @Override
    public void setStroke(Stroke s) {
        _graphics.setStroke(s);
    }

    @Override
    public Stroke getStroke() {
        return _graphics.getStroke();
    }

    @Override
    public void fill(Shape s) {
        _graphics.fill(s);
    }
    
    @Override
    public void draw(Shape s) {
        _graphics.draw(s);
    }
    
    @Override
    public void drawImage(FSImage image, int x, int y, boolean interpolate) {
		Object oldInterpolation = _graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		if (interpolate)
			_graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		else
			_graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			
        _graphics.drawImage(((AWTFSImage)image).getImage(), x, y, null);
		if (oldInterpolation != null)
			_graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
    }

    @Deprecated
    @Override
    public boolean isSupportsSelection() {
        return false;
    }

    @Override
    public boolean isSupportsCMYKColors() {
        return false;
    }

	@Override
	public void drawWithGraphics(float x, float y, float width, float height, OutputDeviceGraphicsDrawer renderer) {
		Graphics2D graphics = (Graphics2D) _graphics.create((int) x, (int) y, (int) width, (int) height);
		renderer.render(graphics);
		graphics.dispose();
	}

    @Override
    public void setPaint(Paint paint) {
        _graphics.setPaint(paint);
    }

    public void setBidiReorderer(BidiReorderer _reorderer) {
        // TODO Auto-generated method stub
    }

    public void setRenderingContext(RenderingContext result) {
    }

    public void setRoot(BlockBox _root) {
    }

    public void initializePage(Graphics2D pageGraphics) {
        _graphics = pageGraphics;

        if (_graphics.getClip() != null) {
            _clipStack.push(_graphics.getClip());
        }

        if (_graphics.getTransform() != null) {
            _transformStack.push(_graphics.getTransform());
        } else {
            _transformStack.push(new AffineTransform());
        }
    }

	public void finish(RenderingContext c, BlockBox _root) {
	}

    @Override
    public void pushTransformLayer(AffineTransform transform) {
        _graphics.transform(transform);
        _transformStack.push(_graphics.getTransform());
    }

    @Override
    public void popTransformLayer() {
        _transformStack.pop();
        AffineTransform transform = _transformStack.peek();
        _graphics.setTransform(transform);
    }

    @Override
    public void popClip() {
        _clipStack.pop();
        Shape previous = _clipStack.peek();
        _graphics.setClip(previous);
    }

    @Override
    public void pushClip(Shape s) {
        _graphics.clip(s);
        _clipStack.push(_graphics.getClip());
    }

    @Override
    public Object startStructure(StructureType type, Box box) {
        return null;
    }

    @Override
    public void endStructure(Object token) {
    }

    @Override
    public void drawLinearGradient(FSLinearGradient lg, Shape bounds) {
        if (lg.getStopPoints().size() < 2) {
            return;
        }

        Color[] colors = new Color[lg.getStopPoints().size()];
        float[] fractions = new float[lg.getStopPoints().size()];

        float maxLength = lg.getStopPoints().get(lg.getStopPoints().size() - 1).getLength();

        if (maxLength == 0f) {
            return;
        }

        for (int i = 0; i < lg.getStopPoints().size(); i++) {
            StopPoint sp = lg.getStopPoints().get(i);
            FSRGBColor col = (FSRGBColor) sp.getColor();

            colors[i] = new Color(col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
            fractions[i] = sp.getLength() / maxLength;
        }

        Rectangle rect = bounds.getBounds();
        Point2D pt1 = new Point2D.Double(lg.getX1() + rect.getMinX(), lg.getY1() + rect.getMinY());
        Point2D pt2 = new Point2D.Double(lg.getX2() + rect.getMinX(), lg.getY2() + rect.getMinY());

        Paint oldPaint = _graphics.getPaint();
        _graphics.setPaint(new LinearGradientPaint(pt1, pt2, fractions, colors));
        _graphics.fill(bounds);
        _graphics.setPaint(oldPaint);
    }
}

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
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.java2d.api.Java2DRendererBuilder;
import com.openhtmltopdf.render.*;
import com.openhtmltopdf.swing.AWTFSFont;
import com.openhtmltopdf.swing.AWTFSImage;
import com.openhtmltopdf.swing.ImageReplacedElement;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class Java2DOutputDevice extends AbstractOutputDevice implements OutputDevice {
    private Graphics2D _graphics;
    private AWTFSFont _font;

    public Java2DOutputDevice(Graphics2D layoutGraphics) {
    	this._graphics = layoutGraphics;
    }

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
    public void setClip(Shape s) {
        _graphics.setClip(s);
    }
    
    @Override
    public Shape getClip() {
        return _graphics.getClip();
    }
    
    @Override
    public void clip(Shape s) {
        _graphics.clip(s);
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
        this._font = (AWTFSFont) font;
        _graphics.setFont(this._font.getAWTFonts().get(0));
    }

    public AWTFSFont getFont() {
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
    public void drawImage(FSImage image, int x, int y) {
        _graphics.drawImage(((AWTFSImage)image).getImage(), x, y, null);
    }
    
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

	private Stack<AffineTransform> transformStack = new Stack<AffineTransform>();
    private Stack<Shape> clipStack= new Stack<Shape>();


	@Override
	public void setPaint(Paint paint) {
		_graphics.setPaint(paint);
	}

	@Override
	public void setAlpha(int alpha) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public List<AffineTransform> pushTransforms(List<AffineTransform> transforms) {
//		AffineTransform currentTransform  = _graphics.getTransform();
//		currentTransform.concatenate(transform);
//        _graphics.setTransform(currentTransform);
// TODO
    	return Collections.emptyList();
    }

	@Override
	public void popTransforms(List<AffineTransform> inverse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getAbsoluteTransformOriginX() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getAbsoluteTransformOriginY() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setBidiReorderer(BidiReorderer _reorderer) {
		// TODO Auto-generated method stub
		
	}

	public void setRenderingContext(RenderingContext result) {
		// TODO Auto-generated method stub
		
	}

	public void setRoot(BlockBox _root) {
		// TODO Auto-generated method stub
		
	}

	public void initializePage(Graphics2D pageGraphics) {
		_graphics = pageGraphics;
	}

	public void finish(RenderingContext c, BlockBox _root) {
	}
}

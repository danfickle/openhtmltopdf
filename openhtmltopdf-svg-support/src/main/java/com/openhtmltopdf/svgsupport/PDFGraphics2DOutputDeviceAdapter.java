package com.openhtmltopdf.svgsupport;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;

import org.apache.batik.ext.awt.g2d.AbstractGraphics2D;
import org.apache.batik.ext.awt.g2d.GraphicContext;

import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.render.RenderingContext;

public class PDFGraphics2DOutputDeviceAdapter extends AbstractGraphics2D {

	private final RenderingContext ctx;
	private final OutputDevice od;
	private final Graphics2D g2d2;
	private final double x;
	private final double y;
	private final AffineTransform defaultTransform;
	private final float dotsPerPoint;

	private Color _c = Color.BLACK;
	private Stroke _stroke;
	private Paint _paint = Color.BLACK;
	private Composite _composite = AlphaComposite.getInstance(AlphaComposite.SRC);
	private Color _background = Color.WHITE;

	public PDFGraphics2DOutputDeviceAdapter(RenderingContext ctx, OutputDevice od, double x, double y, float dotsPerInch) {
		super(false);
		
		this.od = od;
		this.ctx = ctx;
		this.x = x;
		this.y = y;
		this.dotsPerPoint = dotsPerInch / 96f;
		
		AffineTransform scale = AffineTransform.getScaleInstance(dotsPerPoint, dotsPerPoint);
		defaultTransform = scale;
		
		BufferedImage img = new BufferedImage(BufferedImage.TYPE_INT_ARGB, 1, 1);
		g2d2 = img.createGraphics();
		
		this.gc = new GraphicContext();
	}

	@Override
	public Shape getClip() {
		return od.getRawClip();
	}
	
	@Override
	public void setClip(Shape arg0) {
		od.setRawClip(arg0);
	}
	
	@Override
	public void clip(Shape arg0) {
		od.rawClip(arg0);
	}
	
	@Override
	public Composite getComposite() {
		return _composite;
	}
	
	@Override
	public void setBackground(Color arg0) {
		_background = arg0;
	}
	
	@Override
	public Color getBackground() {
		return _background;
	}
	
	@Override
	public void setColor(Color arg0) {
		_c = arg0;
	}
	
	@Override
	public Color getColor() {
		return _c;
	}
	
	@Override
	public void setPaint(Paint in) {
		if (in instanceof org.apache.batik.ext.awt.LinearGradientPaint) {
			org.apache.batik.ext.awt.LinearGradientPaint lg = (org.apache.batik.ext.awt.LinearGradientPaint) in;
			in = lg.getColors()[0];
			// TODO: Proper handling of linear gradients
		}

		_paint = in;
	}
	
	@Override
	public Rectangle getClipBounds() {
		return getClip().getBounds();
	}
	@Override
	public Paint getPaint() {
		return _paint;
	}
	
	@Override
	public void setComposite(Composite arg0) {
		_composite  = arg0;
	}
	
	@Override
	public void setStroke(Stroke arg0) {
		_stroke = arg0;
	}
	
	@Override
	public Stroke getStroke() {
		return _stroke;
	}
	
	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		// TODO: Images
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		// TODO: Images
	}

	@Override
	public void drawString(String str, float x, float y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void draw(Shape s) {
        Color c = getColor();

        od.setColor(new FSRGBColor(c.getRed(), c.getGreen(), c.getBlue()));
        od.setAlpha(c.getAlpha());
        od.setStroke(getStroke());
        od.setPaint(getPaint());
        
        AffineTransform offsetTransform = new AffineTransform(defaultTransform);
        offsetTransform.translate(this.x / this.dotsPerPoint, this.y / this.dotsPerPoint);
        offsetTransform.concatenate(getTransform());
        
        od.saveState();
        od.draw(offsetTransform.createTransformedShape(s));
        od.restoreState();
	}
	
	@Override
	public void fill(Shape s) {
		Color c = getColor();

		od.setColor(new FSRGBColor(c.getRed(), c.getGreen(), c.getBlue()));
        od.setAlpha(c.getAlpha());
        od.setPaint(getPaint());
        
        AffineTransform offsetTransform = new AffineTransform(defaultTransform);
        offsetTransform.translate(this.x / this.dotsPerPoint, this.y / this.dotsPerPoint);
        offsetTransform.concatenate(getTransform());

        od.saveState();
        od.fill(offsetTransform.createTransformedShape(s));
        od.restoreState();
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		return new GraphicsConfiguration() {
			
			@Override
			public AffineTransform getNormalizingTransform() {
				return null;
			}
			
			@Override
			public GraphicsDevice getDevice() {
				return null;
			}
			
			@Override
			public AffineTransform getDefaultTransform() {
				return null;
			}
			
			@Override
			public ColorModel getColorModel(int transparency) {
				return ColorModel.getRGBdefault();
			}
			
			@Override
			public ColorModel getColorModel() {
				return ColorModel.getRGBdefault();
			}
			
			@Override
			public Rectangle getBounds() {
				return null;
			}
		};
	}

	@Override
	public Graphics create() {
		return new PDFGraphics2DOutputDeviceAdapter(ctx, od, x, y, this.dotsPerPoint * 96f);
	}

	@Override
	public void setXORMode(Color c1) {
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		return g2d2.getFontMetrics(f);
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) { }

	@Override
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		return false;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		return false;
	}
	
	@Override
	public void dispose() { }
}

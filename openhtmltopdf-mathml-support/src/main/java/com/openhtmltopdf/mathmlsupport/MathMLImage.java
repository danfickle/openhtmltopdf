package com.openhtmltopdf.mathmlsupport;

import java.awt.Graphics2D;
import java.util.List;

import net.sourceforge.jeuclid.DOMBuilder;
import net.sourceforge.jeuclid.context.LayoutContextImpl;
import net.sourceforge.jeuclid.context.Parameter;
import net.sourceforge.jeuclid.elements.generic.DocumentElement;
import net.sourceforge.jeuclid.layout.JEuclidView;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.render.RenderingContext;

public class MathMLImage implements SVGImage {
	private final JEuclidView _view;
	private final DocumentElement _mathDoc;
	private final double _dotsPerPixel;
	private final MathLayoutContext _context = new MathLayoutContext();
	private final double _scaledWidthInOutputDeviceDots;
	private final double _scaledHeightInOutputDeviceDots;
	private double _sx = 1;
	private double _sy = 1;
	
	public static class MathLayoutContext extends LayoutContextImpl {
		private static final long serialVersionUID = 1;
	}
	
	private boolean haveValue(double val) {
		return val >= 0;
	}
	
	private double minIgnoringNoValues(double spec, double max) {
		if (haveValue(spec) && haveValue(max)) {
			return Math.min(spec,  max);
		} else {
			return haveValue(spec) ? spec : max;
		}
	}
	
	public MathMLImage(Element mathMlElement, double cssWidth,
			double cssHeight, double cssMaxWidth, double cssMaxHeight,
			double dotsPerPixel, List<String> fonts) {
		this._dotsPerPixel = dotsPerPixel;
		this._mathDoc = DOMBuilder.getInstance().createJeuclidDom(mathMlElement);
		
		this._context.setParameter(Parameter.FONTS_SERIF, fonts);
		this._context.setParameter(Parameter.FONTS_DOUBLESTRUCK, fonts);
		this._context.setParameter(Parameter.FONTS_FRAKTUR, fonts);
		this._context.setParameter(Parameter.FONTS_MONOSPACED, fonts);
		this._context.setParameter(Parameter.FONTS_SANSSERIF, fonts);
		this._context.setParameter(Parameter.FONTS_SCRIPT, fonts);
		
		
		this._context.setParameter(Parameter.MATHSIZE, 16f); // TODO: Proper font size pickup from CSS.
		this._view = new JEuclidView(this._mathDoc, this._context, null);

		if (this.getViewWidthInOutputDeviceDots() <= 0 || this.getViewHeightInOutputDeviceDots() <= 0) {
			this._scaledWidthInOutputDeviceDots = 0;
			this._scaledHeightInOutputDeviceDots = 0;
			return;
		}
		
		boolean haveBoth = haveValue(cssWidth) && haveValue(cssHeight);
		boolean haveNone = !haveValue(cssWidth) && !haveValue(cssHeight);
		boolean haveOne = !haveBoth && !haveNone;
		
		double w = -1f;
		double h = -1f;
		
		if (haveBoth) {
			// Have both from CSS, use them constrained by max-xxx values, don't keep aspect ratio.
			w = minIgnoringNoValues(cssWidth, cssMaxWidth);
			h = minIgnoringNoValues(cssHeight, cssMaxHeight);
		} else if (haveNone) {
			// Use rendered view size with max-xxx constraints, keeping aspect ratio.
			double prelimW = minIgnoringNoValues(this.getViewWidthInOutputDeviceDots(), cssMaxWidth); 
			double prelimH = minIgnoringNoValues(this.getViewHeightInOutputDeviceDots(), cssMaxHeight);
			
			double prelimScaleX = prelimW / this.getViewWidthInOutputDeviceDots();
			double prelimScaleY = prelimH / this.getViewHeightInOutputDeviceDots();
			
			double scale = Math.min(prelimScaleX, prelimScaleY);
			
			w = scale * this.getViewWidthInOutputDeviceDots();
			h = scale * this.getViewHeightInOutputDeviceDots();
		} else if (haveOne) {
			if (haveValue(cssWidth)) {
				// Keep aspect ratio, if we have both a width and a conflicting max-height, the max-height wins out.
				double prelimW = minIgnoringNoValues(cssWidth, cssMaxWidth);
				double prelimScale = prelimW / this.getViewWidthInOutputDeviceDots();
				double prelimH = this.getViewHeightInOutputDeviceDots() * prelimScale;
				
				double scale = (haveValue(cssMaxHeight) && prelimH > cssMaxHeight) ? cssMaxHeight / this.getViewHeightInOutputDeviceDots() : prelimScale;
				
				w = scale * this.getViewWidthInOutputDeviceDots();
				h = scale * this.getViewHeightInOutputDeviceDots();
			} else if (haveValue(cssHeight)) {
				// Keep aspect ratio, if we have both a height and a conflicting max-width, the max-width wins out.
				double prelimH = minIgnoringNoValues(cssHeight, cssMaxHeight);
				double prelimScale = prelimH / this.getViewHeightInOutputDeviceDots();
				double prelimW = this.getViewWidthInOutputDeviceDots() * prelimScale;
				
				double scale = (haveValue(cssMaxWidth) && prelimW > cssMaxWidth) ? cssMaxWidth / this.getViewWidthInOutputDeviceDots() : prelimScale;

				w = scale * this.getViewWidthInOutputDeviceDots();
				h = scale * this.getViewHeightInOutputDeviceDots();
			}
		}
		
		_sx = w / this.getViewWidthInOutputDeviceDots();
		_sy = h / this.getViewHeightInOutputDeviceDots();
		
		this._scaledWidthInOutputDeviceDots = w;
		this._scaledHeightInOutputDeviceDots = h;
	}
	
	private double getViewWidthInOutputDeviceDots() {
		return (this._view.getWidth() * _dotsPerPixel);
	}
	
	private double getViewHeightInOutputDeviceDots() {
		return ((this._view.getAscentHeight() + this._view.getDescentHeight()) * _dotsPerPixel);
	}

	@Override
	public int getIntrinsicWidth() {
		return (int) this._scaledWidthInOutputDeviceDots;
	}

	@Override
	public int getIntrinsicHeight() {
		return (int) this._scaledHeightInOutputDeviceDots;
	}

	@Override
	public void drawSVG(OutputDevice outputDevice, RenderingContext ctx,
			double x, double y) {
		outputDevice.drawWithGraphics((float) x, (float) y, (float) this._scaledWidthInOutputDeviceDots, (float) this._scaledHeightInOutputDeviceDots, new OutputDeviceGraphicsDrawer() {
			@Override
			public void render(Graphics2D g2d) {
				g2d.scale(_sx, _sy);
				_view.draw(g2d, 0, _view.getAscentHeight());
			}
		});
	}

}

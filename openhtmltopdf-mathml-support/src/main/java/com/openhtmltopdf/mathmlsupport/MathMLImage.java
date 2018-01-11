package com.openhtmltopdf.mathmlsupport;

import java.awt.Graphics2D;
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
	
	public MathMLImage(Element mathMlElement, double cssWidth,
			double cssHeight, double cssMaxWidth, double cssMaxHeight,
			double dotsPerPixel) {
		this._dotsPerPixel = dotsPerPixel;
		this._mathDoc = DOMBuilder.getInstance().createJeuclidDom(mathMlElement);
		this._context.setParameter(Parameter.MATHSIZE, 16f); // TODO: Proper font size pickup from CSS.
		this._view = new JEuclidView(this._mathDoc, this._context, null);
		
		double w = -1f;
		double h = -1f;
		
		// If either width or max-width is available, use the lesser of the two (min-xxx not implemented at this time).
		if (cssWidth >= 0) {
			w = cssWidth;
		}
		if (cssMaxWidth >= 0 && w > cssMaxWidth) {
			w = cssMaxWidth;
		}
		if (cssHeight >= 0) {
			h = cssHeight;
		}
		if (cssMaxHeight >= 0 && h > cssMaxHeight) {
			h = cssMaxHeight;
		}
		
		// If width or height is specified in first step, then calculate scale factors.
		if (w > 0 && this.getViewWidthInOutputDeviceDots() > 0) {
			_sx = w / this.getViewWidthInOutputDeviceDots();
		}
		if (h > 0 && this.getViewHeightInOutputDeviceDots() > 0) {
			_sy = h / this.getViewHeightInOutputDeviceDots();
		}
		
		if (_sy == 1 && _sx != 1 && h < 0) {
			// Width without height, so keep aspect ratio.
			_sy = _sx;
			h = this.getViewHeightInOutputDeviceDots() * _sy;
		}
		if (_sx == 1 && _sy != 1 && w < 0) {
			// Height without width, so keep aspect ratio.
			_sx = _sy;
			w = this.getViewWidthInOutputDeviceDots() * _sx;
		}
		
		// If we still don't have width or height, use whatever JEuclid calculates is needed.
		this._scaledWidthInOutputDeviceDots = w >= 0 ? w : this.getViewWidthInOutputDeviceDots();
		this._scaledHeightInOutputDeviceDots = h >= 0 ? h : this.getViewHeightInOutputDeviceDots();
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

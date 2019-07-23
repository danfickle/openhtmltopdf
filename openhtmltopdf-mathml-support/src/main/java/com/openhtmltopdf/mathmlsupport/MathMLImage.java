package com.openhtmltopdf.mathmlsupport;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
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
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.simple.extend.ReplacedElementScaleHelper;

public class MathMLImage implements SVGImage {
	private final JEuclidView _view;
	private final DocumentElement _mathDoc;
	private final double _dotsPerPixel;
	private final Box _box;
	private final MathLayoutContext _context = new MathLayoutContext();
	
	public static class MathLayoutContext extends LayoutContextImpl {
		private static final long serialVersionUID = 1;
	}

    public MathMLImage(Element mathMlElement, Box box, double cssWidth,
			double cssHeight, double cssMaxWidth, double cssMaxHeight,
			double dotsPerPixel, List<String> fonts) {
        this._box = box;
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
	}
	
	private double getViewWidthInOutputDeviceDots() {
		return (this._view.getWidth() * _dotsPerPixel);
	}
	
	private double getViewHeightInOutputDeviceDots() {
		return ((this._view.getAscentHeight() + this._view.getDescentHeight()) * _dotsPerPixel);
	}

	@Override
	public int getIntrinsicWidth() {
        return (int) this.getViewWidthInOutputDeviceDots();
	}

	@Override
	public int getIntrinsicHeight() {
        return (int) this.getViewHeightInOutputDeviceDots();
	}

    @Override
    public void drawSVG(OutputDevice outputDevice, RenderingContext ctx, double x, double y) {
        Rectangle contentBounds = _box.getContentAreaEdge(_box.getAbsX(), _box.getAbsY(), ctx);

        final AffineTransform scale2 = ReplacedElementScaleHelper.createScaleTransform(_dotsPerPixel, contentBounds, this._view.getWidth(), this._view.getAscentHeight() + this._view.getDescentHeight());
        final AffineTransform inverse2 = ReplacedElementScaleHelper.inverseOrNull(scale2);
        final boolean transformed2 = scale2 != null && inverse2 != null;

        outputDevice.drawWithGraphics((float) x, (float) y,
                (float) (contentBounds.width / _dotsPerPixel),
                (float) (contentBounds.height / _dotsPerPixel),
                new OutputDeviceGraphicsDrawer() {
            @Override
            public void render(Graphics2D g2d) {
                if (transformed2) {
                    g2d.transform(scale2);
                }
                _view.draw(g2d, 0, _view.getAscentHeight());
                if (transformed2) {
                    g2d.transform(inverse2);
                }
            }
		});
	}

}

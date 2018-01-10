package com.openhtmltopdf.mathmlsupport;

import java.awt.Graphics2D;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import net.sourceforge.jeuclid.DOMBuilder;
import net.sourceforge.jeuclid.elements.generic.DocumentElement;
import net.sourceforge.jeuclid.layout.JEuclidView;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.extend.SVGDrawer.SVGImage;
import com.openhtmltopdf.render.RenderingContext;

public class MathMLImage implements SVGImage {
	private final JEuclidView _view;
	private final DocumentElement _mathDoc;
	private final double _dotsPerPixel;
	
	public MathMLImage(Element mathMlElement, double cssWidth,
			double cssHeight, double cssMaxWidth, double cssMaxHeight,
			double dotsPerPixel) {
		this._dotsPerPixel = dotsPerPixel;
		this._mathDoc = DOMBuilder.getInstance().createJeuclidDom(mathMlElement);
		this._view = this._mathDoc.getDefaultView();
	}

	@Override
	public int getIntrinsicWidth() {
		// TODO: Incorporate dotsPerPixel.
		return (int) (_view.getWidth());
	}

	@Override
	public int getIntrinsicHeight() {
		// TODO: Incorporate dotsPerPixel.
		return (int) ((_view.getAscentHeight() + _view.getDescentHeight()));
	}

	@Override
	public void drawSVG(OutputDevice outputDevice, RenderingContext ctx,
			double x, double y) {

		final float calcX = (float) x;
		final float calcY = (float) y;
		
		outputDevice.drawWithGraphics((float) x, (float) y, 200, 200, new OutputDeviceGraphicsDrawer() {
			@Override
			public void render(Graphics2D g2d) {
				_view.draw(g2d, calcX, calcY);
			}
		});
	}

}

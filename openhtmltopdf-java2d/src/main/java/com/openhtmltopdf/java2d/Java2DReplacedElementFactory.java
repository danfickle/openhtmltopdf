package com.openhtmltopdf.java2d;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.Length;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.swing.SwingReplacedElementFactory;

public class Java2DReplacedElementFactory extends SwingReplacedElementFactory {

	private final SVGDrawer _svgImpl;
	private final FSObjectDrawerFactory _objectDrawerFactory;
	private final SVGDrawer _mathMLImpl;

	public Java2DReplacedElementFactory(SVGDrawer svgImpl, FSObjectDrawerFactory objectDrawerFactory, SVGDrawer
			mathMLImpl) {
		this._svgImpl = svgImpl;
		this._objectDrawerFactory = objectDrawerFactory;
		this._mathMLImpl = mathMLImpl;
	}
	
	@Override
	public ReplacedElement createReplacedElement(LayoutContext context, BlockBox box, UserAgentCallback uac,
			int cssWidth, int cssHeight) {
		Element e = box.getElement();
		if (e == null) {
			return null;
		}

		String nodeName = e.getNodeName();
		if (nodeName.equals("math") && _mathMLImpl != null) {
			return new Java2DSVGReplacedElement(e, _mathMLImpl, cssWidth, cssHeight, box, context);
		} else if (nodeName.equals("svg") && _svgImpl != null) {
			return new Java2DSVGReplacedElement(e, _svgImpl, cssWidth, cssHeight, box, context);
		} else if (nodeName.equals("object") && _objectDrawerFactory != null) {
			FSObjectDrawer drawer = _objectDrawerFactory.createDrawer(e);
			if (drawer != null)
				return new Java2DObjectDrawerReplacedElement(e, drawer, cssWidth, cssHeight,
						context.getSharedContext().getDotsPerPixel());
        }

		/*
		 * Default: Just let the base class handle everything
		 */
		return super.createReplacedElement(context, box, uac, cssWidth, cssHeight);
	}
}

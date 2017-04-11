package com.openhtmltopdf.java2d;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.swing.SwingReplacedElementFactory;

public class Java2DReplacedElementFactory extends SwingReplacedElementFactory {

	private SVGDrawer _svgImpl;

	public Java2DReplacedElementFactory(SVGDrawer svgImpl) {
		this._svgImpl = svgImpl;
	}
	
	@Override
	public ReplacedElement createReplacedElement(LayoutContext context, BlockBox box, UserAgentCallback uac,
			int cssWidth, int cssHeight) {
		Element e = box.getElement();
		if (e == null) {
			return null;
		}

		String nodeName = e.getNodeName();
		if (nodeName.equals("svg") && _svgImpl != null)
			return new Java2DSVGReplacedElement(e, _svgImpl, cssWidth, cssHeight);

		/*
		 * Default: Just let the base class handle everything
		 */
		return super.createReplacedElement(context, box, uac, cssWidth, cssHeight);
	}
}

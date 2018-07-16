package com.openhtmltopdf.java2d;

import org.w3c.dom.Element;

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
			if (drawer != null) {
				return new Java2DObjectDrawerReplacedElement(e, drawer, cssWidth, cssHeight,
						context.getSharedContext().getDotsPerPixel());
			}
		} else if (nodeName.equals("img") && _svgImpl != null) {
			String srcAttr = e.getAttribute("src");
			if (srcAttr != null && srcAttr.endsWith(".svg")) {
				return new Java2DSVGReplacedElement(uac.getXMLResource(srcAttr).getDocument().getDocumentElement(), _svgImpl, cssWidth, cssHeight, box, context);
			}
		}

		/*
		 * Default: Just let the base class handle everything
		 */
		return super.createReplacedElement(context, box, uac, cssWidth, cssHeight);
	}

    @Override
    public boolean isReplacedElement(Element e) {
        if (e == null) {
            return false;
        }

        String nodeName = e.getNodeName();
        if (nodeName.equals("img")) {
            return true;
        } else if (nodeName.equals("math") && _mathMLImpl != null) {
            return true;
        } else if (nodeName.equals("svg") && _svgImpl != null) {
            return true;
        } else if (nodeName.equals("object") && _objectDrawerFactory != null) {
            return _objectDrawerFactory.isReplacedObject(e);
        }
        
        return false;
    }
}

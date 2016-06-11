package com.openhtmltopdf.svgsupport;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;

public class BatikSVGDrawer implements SVGDrawer {

	private static final String DEFAULT_VP_WIDTH = "400";
	private static final String DEFAULT_VP_HEIGHT = "400";
	
	@Override
	public void drawSVG(Element svgElement, OutputDevice outputDevice, RenderingContext ctx, double x, double y) {
		PDFTranscoder transcoder = new PDFTranscoder(outputDevice, ctx, x, y);

		try {
			DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
			Document newDocument = impl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
			
			for (int i = 0; i < svgElement.getChildNodes().getLength(); i++)
			{
				Node importedNode = newDocument.importNode(svgElement.getChildNodes().item(i), true);
				newDocument.getDocumentElement().appendChild(importedNode);
			}
			
			if (svgElement.hasAttribute("width")) {
				newDocument.getDocumentElement().setAttribute("width", svgElement.getAttribute("width"));
			}
			else {
				newDocument.getDocumentElement().setAttribute("width", DEFAULT_VP_WIDTH);
			}
			
			if (svgElement.hasAttribute("height")) {
				newDocument.getDocumentElement().setAttribute("height", svgElement.getAttribute("height"));
			}
			else {
				newDocument.getDocumentElement().setAttribute("height", DEFAULT_VP_HEIGHT);
			}

			TranscoderInput in = new TranscoderInput(newDocument);
			transcoder.transcode(in, null);
		} catch (TranscoderException e) {
			XRLog.exception("Couldn't draw SVG.", e);
		}
	}
}

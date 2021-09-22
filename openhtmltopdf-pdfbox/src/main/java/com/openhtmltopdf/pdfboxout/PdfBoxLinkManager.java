package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Use fast link manager instead.
 */
public class PdfBoxLinkManager {

	/**
	 * All Elements which can have a shaped image map implement this
	 */
	public interface IPdfBoxElementWithShapedLinks {
		Map<Shape, String> getLinkMap();
	}

	@Deprecated
	public static Rectangle2D createTargetArea(RenderingContext c, Box box, float pageHeight, AffineTransform transform,
			Box _root, PdfBoxOutputDevice _od) {
		Rectangle bounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
		
		Point2D pt = new Point2D.Float(bounds.x, (float) bounds.getMaxY());
		Point2D ptTransformed = transform.transform(pt, null);
		
		return new Rectangle2D.Float((float) ptTransformed.getX(),
                    _od.normalizeY((float) ptTransformed.getY(), pageHeight),
                    _od.getDeviceLength(bounds.width),
                    _od.getDeviceLength(bounds.height));
	}
}

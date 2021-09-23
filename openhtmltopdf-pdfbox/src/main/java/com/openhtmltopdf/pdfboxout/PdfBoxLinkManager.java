package com.openhtmltopdf.pdfboxout;

import java.awt.*;
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
}

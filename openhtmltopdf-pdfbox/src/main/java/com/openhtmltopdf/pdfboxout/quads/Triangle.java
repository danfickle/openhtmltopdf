package com.openhtmltopdf.pdfboxout.quads;

import java.awt.geom.Point2D;

/**
 *
 * @author Sunshine A class to represent a triangle Note that all three points
 *         should be different in order to work properly
 * 
 *         Source: https://www.sunshine2k.de/coding/java/Polygon/Kong/Kong.html
 */
public class Triangle {

	// coordinates
	public final Point2D.Float a;
	public final Point2D.Float b;
	public final Point2D.Float c;

	Triangle(Point2D.Float a, Point2D.Float b, Point2D.Float c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public static boolean isInside(Point2D.Float x, Point2D.Float y, Point2D.Float z, Point2D.Float p) {
		Point2D.Float v1 = new Point2D.Float(y.x - x.x, y.y - x.y);
		Point2D.Float v2 = new Point2D.Float(z.x - x.x, z.y - x.y);

		double det = v1.x * v2.y - v2.x * v1.y;
		Point2D.Float tmp = new Point2D.Float(p.x - x.x, p.y - x.y);
		double lambda = (tmp.x * v2.y - v2.x * tmp.y) / det;
		double mue = (v1.x * tmp.y - tmp.x * v1.y) / det;

		return (lambda > 0 && mue > 0 && (lambda + mue) < 1);
	}

}

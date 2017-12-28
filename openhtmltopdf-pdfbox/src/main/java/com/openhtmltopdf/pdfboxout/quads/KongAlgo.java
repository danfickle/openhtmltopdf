package com.openhtmltopdf.pdfboxout.quads;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Sunshine Source:
 *         https://www.sunshine2k.de/coding/java/Polygon/Kong/Kong.html
 */
public class KongAlgo {
	private static boolean isDebug = false;

	private List<Point2D.Float> points;
	private List<Point2D.Float> nonconvexPoints;
	private List<Triangle> triangles;

	// orientation of polygon - true = clockwise, false = counterclockwise
	private boolean isCw;

	public KongAlgo(List<Point2D.Float> points) {
		// we have to copy the point vector as we modify it
		this.points = new ArrayList<Point2D.Float>();
		for (Point2D.Float point : points)
			this.points.add(new Point2D.Float(point.x, point.y));

		nonconvexPoints = new ArrayList<Point2D.Float>();
		triangles = new ArrayList<Triangle>();

		calcPolyOrientation();
		calcNonConvexPoints();
	}

	/*
	 * This determines all concave vertices of the polygon.
	 */
	private void calcNonConvexPoints() {
		// safety check, with less than 4 points we have to do nothing
		if (points.size() <= 3) {
			if (points.size() == 3) {
				triangles.add(new Triangle(points.get(0), points.get(1), points.get(2)));
			}
			return;
		}

		// actual three points
		Point2D.Float p;
		Point2D.Float v;
		Point2D.Float u;
		// result value of test function
		float res;
		for (int i = 0; i < points.size() - 1; i++) {
			p = points.get(i);
			Point2D.Float tmp = points.get(i + 1);
			v = new Point2D.Float(); // interpret v as vector from i to i+1
			v.x = tmp.x - p.x;
			v.y = tmp.y - p.y;

			// ugly - last polygon segment goes from last point to first point
			if (i == points.size() - 2)
				u = points.get(0);
			else
				u = points.get(i + 2);

			res = u.x * v.y - u.y * v.x + v.x * p.y - v.y * p.x;
			// note: cw means res/newres is <= 0
			if ((res > 0 && isCw) || (res <= 0 && !isCw)) {
				nonconvexPoints.add(tmp);
				if (isDebug)
					System.out.println("konkav point #" + (i + 1) + "  Coords: " + tmp.x + "/" + tmp.y);
			}

		}
	}

	/*
	 * Get the orientation of the polygon - clockwise (cw) or counter-clockwise
	 * (ccw)
	 */
	private void calcPolyOrientation() {
		if (points.size() < 3)
			return;

		// first find point with minimum x-coord - if there are several ones take
		// the one with maximal y-coord
		int index = 0; // index of point in vector to find
		Point2D.Float pointOfIndex = points.get(0);
		for (int i = 1; i < points.size(); i++) {
			if (points.get(i).x < pointOfIndex.x) {
				pointOfIndex = points.get(i);
				index = i;
			} else if (points.get(i).x == pointOfIndex.x && points.get(i).y > pointOfIndex.y) {
				pointOfIndex = points.get(i);
				index = i;
			}
		}

		// get vector from index-1 to index
		Point2D.Float prevPointOfIndex;
		if (index == 0)
			prevPointOfIndex = points.get(points.size() - 1);
		else
			prevPointOfIndex = points.get(index - 1);
		Point2D.Float v1 = new Point2D.Float(pointOfIndex.x - prevPointOfIndex.x, pointOfIndex.y - prevPointOfIndex.y);
		// get next point
		Point2D.Float succPointOfIndex;
		if (index == points.size() - 1)
			succPointOfIndex = points.get(0);
		else
			succPointOfIndex = points.get(index + 1);

		// get orientation
		float res = succPointOfIndex.x * v1.y - succPointOfIndex.y * v1.x + v1.x * prevPointOfIndex.y
				- v1.y * prevPointOfIndex.x;

		isCw = (res <= 0);
		if (isDebug)
			System.out.println("isCw : " + isCw);
	}

	/*
	 * Returns true if the triangle formed by the three given points is an ear
	 * considering the polygon - thus if no other point is inside and it is convex.
	 * Otherwise false.
	 */
	private boolean isEar(Point2D.Float p1, Point2D.Float p2, Point2D.Float p3) {
		// not convex, bye
		if (!(isConvex(p1, p2, p3)))
			return false;

		// iterate over all konkav points and check if one of them lies inside the given
		// triangle
		for (Point2D.Float nonconvexPoint : nonconvexPoints) {
			if (Triangle.isInside(p1, p2, p3, nonconvexPoint))
				return false;
		}
		return true;
	}

	/*
	 * Returns true if the point p2 is convex considered the actual polygon. p1, p2
	 * and p3 are three consecutive points of the polygon.
	 */
	private boolean isConvex(Point2D.Float p1, Point2D.Float p2, Point2D.Float p3) {
		Point2D.Float v = new Point2D.Float(p2.x - p1.x, p2.y - p1.y);
		float res = p3.x * v.y - p3.y * v.x + v.x * p1.y - v.y * p1.x;
		return !((res > 0 && isCw) || (res <= 0 && !isCw));
	}

	/*
	 * This is a helper function for accessing consecutive points of the polygon
	 * vector. It ensures that no IndexOutofBoundsException occurs.
	 * 
	 * @param index is the base index of the point to be accessed
	 * 
	 * @param offset to be added/subtracted to the index value
	 */
	private int getIndex(int index, int offset) {
		int newindex;
		if (isDebug)
			System.out.println("size " + points.size() + " index:" + index + " offset:" + offset);
		if (index + offset >= points.size())
			newindex = points.size() - (index + offset);
		else {
			if (index + offset < 0)
				newindex = points.size() + (index + offset);
			else
				newindex = index + offset;
		}
		if (isDebug)
			System.out.println("new index = " + newindex);
		return newindex;
	}

	/*
	 * The actual Kong's Triangulation Algorithm
	 */
	public void runKong() {
		if (points.size() <= 3)
			return;

		triangles.clear();
		int index = 1;

		while (points.size() > 3) {
			if (isEar(points.get(getIndex(index, -1)), points.get(index), points.get(getIndex(index, 1)))) {
				// cut ear
				triangles.add(new Triangle(points.get(getIndex(index, -1)), points.get(index),
						points.get(getIndex(index, 1))));
				points.remove(points.get(index));
				index = getIndex(index, -1);

			} else {
				index = getIndex(index, 1);
			}
		}
		// add last triangle
		triangles.add(new Triangle(points.get(0), points.get(1), points.get(2)));

	}

	public List<Triangle> getTriangles() {
		return triangles;
	}

}

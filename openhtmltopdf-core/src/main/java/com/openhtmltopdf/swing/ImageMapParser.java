package com.openhtmltopdf.swing;
/*
 * {{{ header & license
 * Copyright (c) 2008 elbart0 at free.fr (submitted via email)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */

import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.util.XRLog;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Parser for image maps on elements.
 */
public class ImageMapParser {
	private static final String IMG_USEMAP_ATTR = "usemap";
	private static final String MAP_ELT = "map";
	private static final String MAP_NAME_ATTR = "name";
	private static final String AREA_ELT = "area";
	private static final String AREA_SHAPE_ATTR = "shape";
	private static final String AREA_COORDS_ATTR = "coords";
	private static final String AREA_HREF_ATTR = "href";
	private static final String RECT_SHAPE = "rect";
	private static final String RECTANGLE_SHAPE = "rectangle";
	private static final String CIRC_SHAPE = "circ";
	private static final String CIRCLE_SHAPE = "circle";
	private static final String POLY_SHAPE = "poly";
	private static final String POLYGON_SHAPE = "polygon";

	public static Map<Shape, String> findAndParseMap(Element elem, SharedContext c) {
		String usemapAttr = elem.getAttribute(IMG_USEMAP_ATTR);
		if (usemapAttr == null || usemapAttr.isEmpty())
			return null;

		// lookup in cache, or instantiate
		final String mapName = usemapAttr.substring(1);
		Node map = elem.getOwnerDocument().getElementById(mapName);
		if (null == map) {
			final NodeList maps = elem.getOwnerDocument().getElementsByTagName(MAP_ELT);
			for (int i = 0; i < maps.getLength(); i++) {
				String mapAttr = getAttribute(maps.item(i).getAttributes(), MAP_NAME_ATTR);
				if (areEqual(mapName, mapAttr)) {
					map = maps.item(i);
					break;
				}
			}
			if (null == map) {
				XRLog.layout(Level.INFO, "No map named: '" + mapName + "'");
				return null;
			}
		}
		return parseMap(map, c);
	}

	private static boolean areEqual(String str1, String str2) {
		return (str1 == null && str2 == null) || (str1 != null && str1.equals(str2));
	}

	private static boolean areEqualIgnoreCase(String str1, String str2) {
		return (str1 == null && str2 == null) || (str1 != null && str1.equalsIgnoreCase(str2));
	}

	private static Map<Shape, String> parseMap(Node map, SharedContext c) {
		if (null == map) {
			return Collections.emptyMap();
		} else if (map.hasChildNodes()) {
			AffineTransform scaleInstance = AffineTransform.getScaleInstance(c.getDotsPerPixel(), c.getDotsPerPixel());
			final NodeList children = map.getChildNodes();
			final Map<Shape, String> areas = new HashMap<Shape, String>(children.getLength());
			for (int i = 0; i < children.getLength(); i++) {
				final Node area = children.item(i);
				if (areEqualIgnoreCase(AREA_ELT, area.getNodeName())) {
					if (area.hasAttributes()) {
						final NamedNodeMap attrs = area.getAttributes();
						final String shapeAttr = getAttribute(attrs, AREA_SHAPE_ATTR);
						String coordsAttr = getAttribute(attrs, AREA_COORDS_ATTR);
						if (coordsAttr == null)
							continue;
						final String[] coords = coordsAttr.split(",");
						final String href = getAttribute(attrs, AREA_HREF_ATTR);
						if (areEqualIgnoreCase(RECT_SHAPE, shapeAttr)
								|| areEqualIgnoreCase(RECTANGLE_SHAPE, shapeAttr)) {
							final Shape shape = getCoords(coords, 4);
							if (null != shape) {
								areas.put(scaleInstance.createTransformedShape(shape), href);
							}
						} else if (areEqualIgnoreCase(CIRC_SHAPE, shapeAttr)
								|| areEqualIgnoreCase(CIRCLE_SHAPE, shapeAttr)) {
							final Shape shape = getCoords(coords, 3);
							if (null != shape) {
								areas.put(scaleInstance.createTransformedShape(shape), href);
							}
						} else if (areEqualIgnoreCase(POLY_SHAPE, shapeAttr)
								|| areEqualIgnoreCase(POLYGON_SHAPE, shapeAttr)) {
							final Shape shape = getCoords(coords, -1);
							if (null != shape) {
								areas.put(scaleInstance.createTransformedShape(shape), href);
							}
						} else {
							if (XRLog.isLoggingEnabled()) {
								XRLog.layout(Level.INFO, "Unsupported shape: '" + shapeAttr + "'");
							}
						}
					}
				}
			}
			return areas;
		} else {
			return Collections.emptyMap();
		}
	}

	private static String getAttribute(NamedNodeMap attrs, String attrName) {
		final Node node = attrs.getNamedItem(attrName);
		return null == node ? null : node.getNodeValue();
	}

	private static Shape getCoords(String[] coordValues, int length) {
		if ((-1 == length && 0 == coordValues.length % 2) || length == coordValues.length) {
			float[] coords = new float[coordValues.length];
			int i = 0;
			for (String coord : coordValues) {
				try {
					coords[i++] = Float.parseFloat(coord.trim());
				} catch (NumberFormatException e) {
					XRLog.layout(Level.WARNING, "Error while parsing shape coords", e);
					return null;
				}
			}
			if (4 == length) {
				return new Rectangle2D.Float(coords[0], coords[1], coords[2] - coords[0], coords[3] - coords[1]);
			} else if (3 == length) {
				final float radius = coords[2];
				return new Ellipse2D.Float(coords[0] - radius, coords[1] - radius, radius * 2, radius * 2);
			} else if (-1 == length) {
				final int npoints = coords.length / 2;
				final int[] xpoints = new int[npoints];
				final int[] ypoints = new int[npoints];
				for (int c = 0, p = 0; p < npoints; p++) {
					xpoints[p] = (int)coords[c++];
					ypoints[p] = (int)coords[c++];
				}
				return new Polygon(xpoints, ypoints, npoints);
			} else {
				XRLog.layout(Level.INFO, "Unsupported shape: '" + length + "'");
				return null;
			}
		} else {
			return null;
		}
	}
}

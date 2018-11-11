package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxLinkManager.IPdfBoxElementWithShapedLinks;
import com.openhtmltopdf.pdfboxout.quads.KongAlgo;
import com.openhtmltopdf.pdfboxout.quads.Triangle;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

public class PdfBoxFastLinkManager {

	private final Map<PDPage, Set<String>> _linkTargetAreas;
	private final SharedContext _sharedContext;
	private final float _dotsPerPoint;
	private final Box _root;
	private final PdfBoxFastOutputDevice _od;
	private final List<LinkDetails> _links;

	public PdfBoxFastLinkManager(SharedContext ctx, float dotsPerPoint, Box root, PdfBoxFastOutputDevice od) {
		this._sharedContext = ctx;
		this._dotsPerPoint = dotsPerPoint;
		this._root = root;
		this._od = od;
		this._linkTargetAreas = new HashMap<PDPage, Set<String>>();
		this._links = new ArrayList<LinkDetails>();
	}

	private Rectangle2D calcTotalLinkArea(RenderingContext c, Box box, float pageHeight, AffineTransform transform) {
		Box current = box;
		while (true) {
			Box prev = current.getPreviousSibling();
			if (prev == null || prev.getElement() != box.getElement()) {
				break;
			}

			current = prev;
		}

		Rectangle2D result = createTargetArea(c, current, pageHeight, transform, _root, _od);

		current = current.getNextSibling();
		while (current != null && current.getElement() == box.getElement()) {
			result = add(result, createTargetArea(c, current, pageHeight, transform, _root, _od));

			current = current.getNextSibling();
		}

		return result;
	}

	private Rectangle2D add(Rectangle2D r1, Rectangle2D r2) {
		return r1.createUnion(r2);
	}

	private String createRectKey(Rectangle2D rect, Shape linkShape, AffineTransform transform) {
		StringBuilder key = new StringBuilder(
				rect.getMinX() + ":" + rect.getMaxY() + ":" + rect.getMaxX() + ":" + rect.getMinY());
		if (linkShape != null) {
			PathIterator pathIterator = linkShape.getPathIterator(transform);
			double[] vals = new double[6];
			while (!pathIterator.isDone()) {
				int type = pathIterator.currentSegment(vals);
				switch (type) {
				case PathIterator.SEG_CUBICTO:
					key.append("C");
					key.append(vals[0]).append(":").append(vals[1]).append(":").append(vals[2]).append(":")
							.append(vals[3]).append(":").append(vals[4]).append(":").append(vals[5]);
					break;
				case PathIterator.SEG_LINETO:
					key.append("L");
					key.append(vals[0]).append(":").append(vals[1]).append(":");
					break;
				case PathIterator.SEG_MOVETO:
					key.append("M");
					key.append(vals[0]).append(":").append(vals[1]).append(":");
					break;
				case PathIterator.SEG_QUADTO:
					key.append("Q");
					key.append(vals[0]).append(":").append(vals[1]).append(":").append(vals[2]).append(":")
							.append(vals[3]);
					break;
				case PathIterator.SEG_CLOSE:
					key.append("cp");
					break;
				default:
					break;
				}
				pathIterator.next();
			}
		}
		return key.toString();
	}

	private Rectangle2D checkLinkArea(PDPage page, RenderingContext c, Box box, float pageHeight,
			AffineTransform transform, Shape linkShape) {
		Rectangle2D targetArea = calcTotalLinkArea(c, box, pageHeight, transform);
		String key = createRectKey(targetArea, linkShape, transform);
		Set<String> keys = _linkTargetAreas.get(page);
		if (keys == null) {
			keys = new HashSet<String>();
			_linkTargetAreas.put(page, keys);
		}
		if (keys.contains(key)) {
			return null;
		}
		keys.add(key);
		return targetArea;
	}

	private void processLink(RenderingContext c, Box box, PDPage page, float pageHeight, AffineTransform transform) {
		Element elem = box.getElement();
		if (elem != null) {
			NamespaceHandler handler = _sharedContext.getNamespaceHandler();
			String uri = handler.getLinkUri(elem);
			if (uri != null) {
				addUriAsLink(c, box, page, pageHeight, transform, elem, handler, uri, null);
			}
		}
		if (box instanceof BlockBox) {
			ReplacedElement element = ((BlockBox) box).getReplacedElement();
			if (element instanceof IPdfBoxElementWithShapedLinks) {
				Map<Shape, String> linkMap = ((IPdfBoxElementWithShapedLinks) element).getLinkMap();
				if (linkMap != null) {
					for (Entry<Shape, String> shapeStringEntry : linkMap.entrySet()) {
						Shape shape = shapeStringEntry.getKey();
						String shapeUri = shapeStringEntry.getValue();
						NamespaceHandler handler = _sharedContext.getNamespaceHandler();
						addUriAsLink(c, box, page, pageHeight, transform, elem, handler, shapeUri, shape);
					}
				}
			}
		}
	}

	private static boolean isPointEqual(Point2D.Float p1, Point2D.Float p2) {
		final double epsilon = 0.000001;
		return Math.abs(p1.x - p2.x) < epsilon && Math.abs(p1.y - p2.y) < epsilon;
	}

	private static void removeDoublicatePoints(List<Point2D.Float> points) {
		boolean rerun;
		do {
			rerun = false;
			/*
			 * We can only form triangles if three points are not the same. So we must
			 * filter out all points which follow each other and are the same.
			 */
			for (int i = 0; i < points.size() - 1; i++) {
				Point2D.Float p1 = points.get(i);
				Point2D.Float p2 = points.get(i + 1);
				if (isPointEqual(p1, p2)) {
					points.remove(i);
					rerun = true;
				}
			}
			/*
			 * And we must filter out the same points with gap of 1 between them
			 */
			for (int i = 0; i < points.size() - 2; i++) {
				Point2D.Float p1 = points.get(i);
				Point2D.Float p2 = points.get(i + 2);
				if (isPointEqual(p1, p2)) {
					points.remove(i);
					rerun = true;
				}
			}
		} while (rerun);
	}

	private void addUriAsLink(RenderingContext c, Box box, PDPage page, float pageHeight, AffineTransform transform,
			Element elem, NamespaceHandler handler, String uri, Shape linkShape) {
		if (uri.length() > 1 && uri.charAt(0) == '#') {
			String anchor = uri.substring(1);
			Box target = _sharedContext.getBoxById(anchor);
			if (target != null) {
				PDPageXYZDestination dest = createDestination(c, target);

				PDAction action;
				if (handler.getAttributeValue(elem, "onclick") != null
						&& !"".equals(handler.getAttributeValue(elem, "onclick"))) {
					action = new PDActionJavaScript(handler.getAttributeValue(elem, "onclick"));
				} else {
					PDActionGoTo go = new PDActionGoTo();
					go.setDestination(dest);
					action = go;
				}

				Rectangle2D targetArea = checkLinkArea(page, c, box, pageHeight, transform, linkShape);
				if (targetArea == null) {
					return;
				}

				PDAnnotationLink annot = new PDAnnotationLink();
				annot.setAction(action);
				if (!placeAnnotation(transform, linkShape, targetArea, annot))
					return;

				addLinkToPage(page, annot);
			} else {
			    XRLog.general(Level.WARNING, "Could not find valid target for link. Link href = " + uri);
			}
		} else if (uri.contains("://")) {
			PDActionURI uriAct = new PDActionURI();
			uriAct.setURI(uri);

			Rectangle2D targetArea = checkLinkArea(page, c, box, pageHeight, transform, linkShape);
			if (targetArea == null) {
				return;
			}
			PDAnnotationLink annot = new PDAnnotationLink();
			annot.setAction(uriAct);
			if (!placeAnnotation(transform, linkShape, targetArea, annot))
				return;

			addLinkToPage(page, annot);
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean placeAnnotation(AffineTransform transform, Shape linkShape, Rectangle2D targetArea,
			PDAnnotationLink annot) {
		annot.setRectangle(new PDRectangle((float) targetArea.getMinX(), (float) targetArea.getMinY(),
				(float) targetArea.getWidth(), (float) targetArea.getHeight()));
		if (linkShape != null) {
			float[] quadPoints = mapShapeToQuadPoints(transform, linkShape, targetArea);
			/*
			 * Is this not an area shape? Then we can not setup quads - ignore this shape.
			 */
			if (quadPoints.length == 0)
				return false;
			annot.setQuadPoints(quadPoints);
		}
		return true;
	}

	private float[] mapShapeToQuadPoints(AffineTransform transform, Shape linkShape, Rectangle2D targetArea) {
		List<Point2D.Float> points = new ArrayList<Point2D.Float>();
		AffineTransform transformForQuads = new AffineTransform();
		transformForQuads.translate(targetArea.getMinX(), targetArea.getMinY());
		// We must flip the whole thing upside down
		transformForQuads.translate(0, targetArea.getHeight());
		transformForQuads.scale(1, -1);
		transformForQuads.concatenate(transform);
		Area area = new Area(linkShape);
		PathIterator pathIterator = area.getPathIterator(transformForQuads, 1.0);
		double[] vals = new double[6];
		while (!pathIterator.isDone()) {
			int type = pathIterator.currentSegment(vals);
			switch (type) {
			case PathIterator.SEG_CUBICTO:
				throw new RuntimeException("Invalid State, Area should never give us a curve here!");
			case PathIterator.SEG_LINETO:
				points.add(new Point2D.Float((float) vals[0], (float) vals[1]));
				break;
			case PathIterator.SEG_MOVETO:
				points.add(new Point2D.Float((float) vals[0], (float) vals[1]));
				break;
			case PathIterator.SEG_QUADTO:
				throw new RuntimeException("Invalid State, Area should never give us a curve here!");
			case PathIterator.SEG_CLOSE:
				break;
			default:
				break;
			}
			pathIterator.next();
		}

		removeDoublicatePoints(points);

		KongAlgo algo = new KongAlgo(points);
		algo.runKong();

		float ret[] = new float[algo.getTriangles().size() * 8];
		int i = 0;
		for (Triangle triangle : algo.getTriangles()) {
			ret[i++] = triangle.a.x;
			ret[i++] = triangle.a.y;
			ret[i++] = triangle.b.x;
			ret[i++] = triangle.b.y;
			/*
			 * To get a quad we add the point between b and c
			 */
			ret[i++] = triangle.b.x + (triangle.c.x - triangle.b.x) / 2;
			ret[i++] = triangle.b.y + (triangle.c.y - triangle.b.y) / 2;

			ret[i++] = triangle.c.x;
			ret[i++] = triangle.c.y;
		}

		if (ret.length % 8 != 0)
			throw new IllegalStateException("Not exact 8xn QuadPoints!");
		for (; i < ret.length; i += 2) {
			if (ret[i] < targetArea.getMinX() || ret[i] > targetArea.getMaxX())
				throw new IllegalStateException("Invalid rectangle calculation. Map shape is out of bound.");
			if (ret[i + 1] < targetArea.getMinY() || ret[i + 1] > targetArea.getMaxY())
				throw new IllegalStateException("Invalid rectangle calculation. Map shape is out of bound.");
		}
		return ret;
	}

	private void addLinkToPage(PDPage page, PDAnnotationLink annot) {
		PDBorderStyleDictionary styleDict = new PDBorderStyleDictionary();
		styleDict.setWidth(0);
		styleDict.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
		annot.setBorderStyle(styleDict);

		try {
			List<PDAnnotation> annots = page.getAnnotations();

			if (annots == null) {
				annots = new ArrayList<PDAnnotation>();
				page.setAnnotations(annots);
			}

			annots.add(annot);
		} catch (IOException e) {
			throw new PdfContentStreamAdapter.PdfException("processLink", e);
		}
	}

	private PDPageXYZDestination createDestination(RenderingContext c, Box box) {
	    return PdfBoxBookmarkManager.createBoxDestination(c, _od.getWriter(), _od, _dotsPerPoint, _root, box);
	}

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

	private static class LinkDetails {

		RenderingContext c;
		Box box;
		PDPage page;
		float pageHeight;
		AffineTransform transform;
	}

	public void processLinkLater(RenderingContext c, Box box, PDPage page, float pageHeight,
			AffineTransform transform) {
	    
	    if ((box instanceof BlockBox &&
	        ((BlockBox) box).getReplacedElement() != null) ||
	        (box.getElement() != null && box.getElement().getNodeName().equals("a"))) {
	    
		LinkDetails link = new LinkDetails();
		link.c = c;
		link.box = box;
		link.page = page;
		link.pageHeight = pageHeight;
		link.transform = (AffineTransform) transform.clone();

		_links.add(link);
	    }
	}

	public void processLinks() {
		for (LinkDetails link : _links) {
			processLink(link.c, link.box, link.page, link.pageHeight, link.transform);
		}
	}
}

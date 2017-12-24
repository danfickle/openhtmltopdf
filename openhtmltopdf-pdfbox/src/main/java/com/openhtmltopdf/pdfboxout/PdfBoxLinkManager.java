package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class PdfBoxLinkManager {

	private final Map<PDPage, Set<String>> _linkTargetAreas;
	private final SharedContext _sharedContext;
	private final float _dotsPerPoint;
	private final Box _root;
	private final PdfBoxOutputDevice _od;
	private final List<LinkDetails> _links;

	/**
	 * All Elements which can have a shaped image map implement this
	 */
	public interface IPdfBoxElementWithShapedLinks {
		Map<Shape, String> getLinkMap();
	}

	public PdfBoxLinkManager(SharedContext ctx, float dotsPerPoint, Box root, PdfBoxOutputDevice od) {
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
		float llx = (float) Math.min(r1.getMinX(), r2.getMinX());
		float urx = (float) Math.max(r1.getMaxX(), r2.getMaxX());
		float lly = (float) Math.min(r1.getMaxY(), r2.getMaxY());
		float ury = (float) Math.max(r1.getMinY(), r2.getMinY());

		return new Rectangle2D.Float(llx, lly, urx, ury);
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
				annot.setRectangle(new PDRectangle((float) targetArea.getMinX(), (float) targetArea.getMinY(),
						(float) targetArea.getWidth(), (float) targetArea.getHeight()));
				if (linkShape != null)
					annot.setQuadPoints(mapShapeToQuadPoints(elem, transform, linkShape));

				addLinkToPage(page, annot);
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
			annot.setRectangle(new PDRectangle((float) targetArea.getMinX(), (float) targetArea.getMinY(),
					(float) targetArea.getWidth(), (float) targetArea.getHeight()));
			if (linkShape != null)
				annot.setQuadPoints(mapShapeToQuadPoints(elem, transform, linkShape));

			addLinkToPage(page, annot);
		}
	}

	private float[] mapShapeToQuadPoints(Element elem, AffineTransform transform, Shape linkShape) {
		List<Float> points = new ArrayList<Float>();

		PathIterator pathIterator = new Area(linkShape).getPathIterator(transform, 1.0);
		double[] vals = new double[6];
		double lastX = 0;
		double lastY = 0;
		Double firstX = null;
		Double firstY = null;
		while (!pathIterator.isDone()) {
			int type = pathIterator.currentSegment(vals);
			switch (type) {
			case PathIterator.SEG_CUBICTO:
				throw new RuntimeException("Invalid State, Area should never give us a curve here!");
			case PathIterator.SEG_LINETO:
				points.add((float) vals[0]);
				points.add((float) vals[1]);
				lastX = vals[0];
				lastY = vals[1];
				break;
			case PathIterator.SEG_MOVETO:
				points.add((float) vals[0]);
				points.add((float) vals[1]);
				lastX = vals[0];
				lastY = vals[1];
				if (firstX == null) {
					firstX = lastX;
					firstY = lastY;
				}
				break;
			case PathIterator.SEG_QUADTO:
				throw new RuntimeException("Invalid State, Area should never give us a curve here!");
			case PathIterator.SEG_CLOSE:
				if (firstX != null && points.size() % 8 != 0) {
					points.add((float) (double) firstX);
					points.add((float) (double) firstY);
				}
				break;
			default:
				break;
			}
			pathIterator.next();
		}

		while (points.size() % 8 != 0) {
			points.add((float) lastX);
			points.add((float) lastY);
		}

		float ret[] = new float[points.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = points.get(0);
		if (ret.length % 8 != 0)
			throw new IllegalStateException("Not exact 8xn QuadPoints!");
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
		PDPageXYZDestination result = new PDPageXYZDestination();

		PageBox page = _root.getLayer().getPage(c, _od.getPageRefY(box));
		int distanceFromTop = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
		distanceFromTop += box.getAbsY() + box.getMargin(c).top() - page.getTop();

		result.setTop((int) (page.getHeight(c) / _dotsPerPoint - distanceFromTop / _dotsPerPoint));
		result.setPage(_od.getWriter().getPage(_od.getStartPageNo() + page.getPageNo()));

		return result;
	}

	public static Rectangle2D createTargetArea(RenderingContext c, Box box, float pageHeight, AffineTransform transform,
			Box _root, PdfBoxOutputDevice _od) {
		Rectangle bounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
		PageBox page = _root.getLayer().getPage(c, bounds.y);

		float bottom = _od.getDeviceLength(
				page.getBottom() - (bounds.y + bounds.height) + page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM));
		float left = _od.getDeviceLength(page.getMarginBorderPadding(c, CalculatedStyle.LEFT) + bounds.x);

		return new Rectangle2D.Float(left, bottom, _od.getDeviceLength(bounds.width),
				_od.getDeviceLength(bounds.height));
	}

	public static class LinkDetails {

		RenderingContext c;
		Box box;
		PDPage page;
		float pageHeight;
		AffineTransform transform;
	}

	public void processLinkLater(RenderingContext c, Box box, PDPage page, float pageHeight,
			AffineTransform transform) {
		LinkDetails link = new LinkDetails();
		link.c = c;
		link.box = box;
		link.page = page;
		link.pageHeight = pageHeight;
		link.transform = transform;

		_links.add(link);
	}

	public void processLinks() {
		for (LinkDetails link : _links) {
			processLink(link.c, link.box, link.page, link.pageHeight, link.transform);
		}
	}
}

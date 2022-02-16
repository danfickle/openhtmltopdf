package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.pdfboxout.PdfBoxLinkManager.IPdfBoxElementWithShapedLinks;
import com.openhtmltopdf.pdfboxout.quads.KongAlgo;
import com.openhtmltopdf.pdfboxout.quads.Triangle;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
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
    private PdfBoxAccessibilityHelper _pdfUa;

    /**
     * A map from uri to embedded file, so we don't embed files twice
     * in case of a split link (example, two link boxes are formed when
     * a link breaks in the middle).
     */
    private final Map<String, PDComplexFileSpecification> _embeddedFiles;

    /**
     * The lazily created appearance dict for emedded files.
     */
    private PDAppearanceDictionary _embeddedFileAppearance;

	public PdfBoxFastLinkManager(SharedContext ctx, float dotsPerPoint, Box root, PdfBoxFastOutputDevice od) {
		this._sharedContext = ctx;
		this._dotsPerPoint = dotsPerPoint;
		this._root = root;
		this._od = od;
		this._linkTargetAreas = new HashMap<>();
		this._links = new ArrayList<>();
        this._embeddedFiles = new HashMap<>();
	}

	private Rectangle2D calcTotalLinkArea(RenderingContext c, Box box, float pageHeight, AffineTransform transform) {
	    if (_pdfUa != null) {
	        // For PDF/UA we need one link annotation per box.
	        return createTargetArea(c, box, pageHeight, transform, _root, _od);
	    }
	    
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

	private Rectangle2D checkLinkArea(LinkDetails link, Shape linkShape) {
		Rectangle2D targetArea = calcTotalLinkArea(link.c, link.box, link.pageHeight, link.transform);
		String key = createRectKey(targetArea, linkShape, link.transform);
		Set<String> keys = _linkTargetAreas.get(link.page);
		if (keys == null) {
			keys = new HashSet<>();
			_linkTargetAreas.put(link.page, keys);
		}
		if (keys.contains(key)) {
			return null;
		}
		keys.add(key);
		return targetArea;
	}

	private void processLink(LinkDetails linkDetails) {
		Element elem = linkDetails.box.getElement();
		if (elem != null) {
			NamespaceHandler handler = _sharedContext.getNamespaceHandler();
			String uri = handler.getLinkUri(elem);
			if (uri != null) {
				addUriAsLink(linkDetails, elem, handler, uri, null);
			}
		}
		if (linkDetails.box instanceof BlockBox) {
			ReplacedElement element = ((BlockBox) linkDetails.box).getReplacedElement();
			if (element instanceof IPdfBoxElementWithShapedLinks) {
				Map<Shape, String> linkMap = ((IPdfBoxElementWithShapedLinks) element).getLinkMap();
				if (linkMap != null) {
					for (Entry<Shape, String> shapeStringEntry : linkMap.entrySet()) {
						Shape shape = shapeStringEntry.getKey();
						String shapeUri = shapeStringEntry.getValue();
						NamespaceHandler handler = _sharedContext.getNamespaceHandler();
						addUriAsLink(linkDetails, elem, handler, shapeUri, shape);
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

	private void addUriAsLink(LinkDetails linkDetails,
			Element elem, NamespaceHandler handler, String uri, Shape linkShape) {
		if (uri.length() > 1 && uri.charAt(0) == '#') {
			String anchor = uri.substring(1);
			Box target = _sharedContext.getBoxById(anchor);
			if (target != null) {
				PDPageXYZDestination dest = createDestination(linkDetails.c, target);

				PDAction action;
				if (handler.getAttributeValue(elem, "onclick") != null
						&& !"".equals(handler.getAttributeValue(elem, "onclick"))) {
					action = new PDActionJavaScript(handler.getAttributeValue(elem, "onclick"));
				} else {
					PDActionGoTo go = new PDActionGoTo();
					go.setDestination(dest);
					action = go;
				}

				Rectangle2D targetArea = checkLinkArea(linkDetails, linkShape);
				if (targetArea == null) {
					return;
				}

				PDAnnotationLink annot = new PDAnnotationLink();
				annot.setAction(action);

                AnnotationContainer annotContainer = new AnnotationContainer.PDAnnotationLinkContainer(annot);

				if (!placeAnnotation(linkDetails.transform, linkShape, targetArea, annotContainer))
					return;

                addLinkToPage(linkDetails.page, annotContainer, linkDetails.box, target);
			} else {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_PDF_COULD_NOT_FIND_VALID_TARGET_FOR_LINK, uri);
			}
		} else if (isURI(uri)) {
            AnnotationContainer annotContainer = null;

            if (!elem.hasAttribute("download")) {
                PDActionURI uriAct = new PDActionURI();
                uriAct.setURI(uri);

                PDAnnotationLink annot = new PDAnnotationLink();
                annot.setAction(uriAct);

                annotContainer = new AnnotationContainer.PDAnnotationLinkContainer(annot);
            } else {
                annotContainer = createFileEmbedLinkAnnotation(elem, uri);
            }

            if (annotContainer != null) {
                if (linkDetails.targetArea == null) {
                    return;
                }

                if (!placeAnnotation(linkDetails.transform, linkShape, linkDetails.targetArea, annotContainer)) {
                    return;
                }

                addLinkToPage(linkDetails.page, annotContainer, linkDetails.box, null);
            }
        }
    }

    /**
     * Create a file attachment link, being careful not to embed the same
     * file (as specified by uri) more than once.
     *
     * The element should have the following attributes:
     * download="embedded-filename.ext",
     * data-content-type="file-mime-type" which
     * defaults to "application/octet-stream",
     * relationship (required for PDF/A3), one of:
     * "Source", "Supplement", "Data", "Alternative", "Unspecified",
     * title="file description" (recommended for PDF/A3).
     */
    private AnnotationContainer createFileEmbedLinkAnnotation(
            Element elem, String uri) {
        PDComplexFileSpecification fs = _embeddedFiles.get(uri);

        if (fs != null) {
            PDAnnotationFileAttachment annotationFileAttachment = new PDAnnotationFileAttachment();

            annotationFileAttachment.setFile(fs);
            annotationFileAttachment.setAppearance(this._embeddedFileAppearance);

            return new AnnotationContainer.PDAnnotationFileAttachmentContainer(annotationFileAttachment);
        }

        byte[] file = _sharedContext.getUserAgentCallback().getBinaryResource(uri, ExternalResourceType.FILE_EMBED);

        if (file != null) {
            try {
                String contentType = elem.getAttribute("data-content-type").isEmpty() ? 
                        "application/octet-stream" : 
                        elem.getAttribute("data-content-type");

                PDEmbeddedFile embeddedFile = new PDEmbeddedFile(_od.getWriter(), new ByteArrayInputStream(file));
                embeddedFile.setSubtype(contentType);
                embeddedFile.setSize(file.length);

                // PDF/A3 requires a mod date for the file.
                if (elem.hasAttribute("relationship")) {
                    // FIXME: Should we make this specifiable.
                    embeddedFile.setModDate(Calendar.getInstance());
                }

                String fileName = elem.getAttribute("download");

                fs = new PDComplexFileSpecification();
                fs.setEmbeddedFile(embeddedFile);
                fs.setFile(fileName);
                fs.setFileUnicode(fileName);

                // The PDF/A3 standard requires one to specify the relationship
                // this embedded file has to the link annotation.
                if (elem.hasAttribute("relationship") &&
                    Arrays.asList("Source", "Supplement", "Data", "Alternative", "Unspecified")
                          .contains(elem.getAttribute("relationship"))) {
                    fs.getCOSObject().setItem(
                            COSName.getPDFName("AFRelationship"),
                            COSName.getPDFName(elem.getAttribute("relationship")));
                }

                if (elem.hasAttribute("title")) {
                    fs.setFileDescription(elem.getAttribute("title"));
                }

                this._embeddedFiles.put(uri, fs);

                if (this._embeddedFileAppearance == null) {
                    this._embeddedFileAppearance = createFileEmbedLinkAppearance();
                }

                PDAnnotationFileAttachment annotationFileAttachment = new PDAnnotationFileAttachment();

                annotationFileAttachment.setFile(fs);
                annotationFileAttachment.setAppearance(this._embeddedFileAppearance);

                // PDF/A3 requires we explicitly list this link as associated with file.
                if (elem.hasAttribute("relationship")) {
                    COSArray fileRefArray = new COSArray();
                    fileRefArray.add(fs);

                    annotationFileAttachment.getCOSObject().setItem(COSName.getPDFName("AF"), fileRefArray);
                }

                return new AnnotationContainer.PDAnnotationFileAttachmentContainer(annotationFileAttachment);
            } catch (IOException e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_EMBEDDED_FILE, uri, e);
            }
        } else {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.LOAD_COULD_NOT_LOAD_EMBEDDED_FILE, uri);
        }

        return null;
    }

    /**
     * Create an empty appearance stream to
     * hide the pin icon used by various pdf reader for signaling an embedded file
     */
    private PDAppearanceDictionary createFileEmbedLinkAppearance() {
        PDAppearanceDictionary appearanceDictionary = new PDAppearanceDictionary();
        PDAppearanceStream appearanceStream = new PDAppearanceStream(_od.getWriter());
        appearanceStream.setResources(new PDResources());
        appearanceDictionary.setNormalAppearance(appearanceStream);
        return appearanceDictionary;
    }

	private static boolean isURI(String uri) {
		try {
			return URI.create(uri) != null;
		} catch (IllegalArgumentException e) {
			XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.GENERAL_PDF_URI_IN_HREF_IS_NOT_A_VALID_URI, uri);
			return false;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean placeAnnotation(AffineTransform transform, Shape linkShape, Rectangle2D targetArea,
			AnnotationContainer annot) {
		annot.setRectangle(new PDRectangle((float) targetArea.getMinX(), (float) targetArea.getMinY(),
				(float) targetArea.getWidth(), (float) targetArea.getHeight()));
		
		// PDF/A standard requires the print flag to be set and there shouldn't
		// be any harm in setting it for other documents.
		annot.setPrinted(true);
		
		if (linkShape != null) {
			QuadPointShape quadPointsResult = mapShapeToQuadPoints(transform, linkShape, targetArea);
			/*
			 * Is this not an area shape? Then we can not setup quads - ignore this shape.
			 */
			if (quadPointsResult.quadPoints.length == 0)
				return false;
			annot.setQuadPoints(quadPointsResult.quadPoints);
			Rectangle2D reducedTarget = quadPointsResult.boundingBox;
			annot.setRectangle(new PDRectangle((float) reducedTarget.getMinX(), (float) reducedTarget.getMinY(),
					(float) reducedTarget.getWidth(), (float) reducedTarget.getHeight()));
		}
		return true;
	}

	static class QuadPointShape {
		float[] quadPoints;
		Rectangle2D boundingBox;
	}
	
	static QuadPointShape mapShapeToQuadPoints(AffineTransform transform, Shape linkShape, Rectangle2D targetArea) {
		List<Point2D.Float> points = new ArrayList<>();
		AffineTransform transformForQuads = new AffineTransform();
		transformForQuads.translate(targetArea.getMinX(), targetArea.getMinY());
		// We must flip the whole thing upside down
		transformForQuads.translate(0, targetArea.getHeight());
		transformForQuads.scale(1, -1);
		transformForQuads.concatenate(AffineTransform.getScaleInstance(transform.getScaleX(), transform.getScaleX()));
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

		float minX = (float) targetArea.getMaxX();
		float maxX = (float) targetArea.getMinX();
		float minY = (float) targetArea.getMaxY();
		float maxY = (float) targetArea.getMinY();
		
		float[] ret = new float[algo.getTriangles().size() * 8];
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
			
			for (Point2D.Float p : new Point2D.Float[] { triangle.a, triangle.b, triangle.c }) {
				float x = p.x;
				float y = p.y;

				minX = Math.min(x, minX);
				minY = Math.min(y, minY);

				maxX = Math.max(x, maxX);
				maxY = Math.max(y, maxY);
			}
		}

		//noinspection ConstantConditions
		if (ret.length % 8 != 0)
			throw new IllegalStateException("Not exact 8xn QuadPoints!");
		for (; i < ret.length; i += 2) {
			if (ret[i] < targetArea.getMinX() || ret[i] > targetArea.getMaxX())
				throw new IllegalStateException("Invalid rectangle calculation. Map shape is out of bound.");
			if (ret[i + 1] < targetArea.getMinY() || ret[
					i + 1] > targetArea.getMaxY())
				throw new IllegalStateException("Invalid rectangle calculation. Map shape is out of bound.");
		}

		QuadPointShape result = new QuadPointShape();
		result.quadPoints = ret;
		Rectangle2D.Float boundingRectangle = new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
		Rectangle2D.intersect(targetArea, boundingRectangle, boundingRectangle);
		result.boundingBox = boundingRectangle;
		return result;
	}

	private void addLinkToPage(
          PDPage page, AnnotationContainer annot, Box anchor, Box target) {
		PDBorderStyleDictionary styleDict = new PDBorderStyleDictionary();
		styleDict.setWidth(0);
		styleDict.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
		annot.setBorderStyle(styleDict);

		try {
			List<PDAnnotation> annots = page.getAnnotations();

			if (annots == null) {
				annots = new ArrayList<>();
				page.setAnnotations(annots);
			}

			annots.add(annot.getPdAnnotation());
			
			if (_pdfUa != null) {
			    _pdfUa.addLink(anchor, target, annot.getPdAnnotation(), page);
			}
		} catch (IOException e) {
			throw new PdfContentStreamAdapter.PdfException("processLink", e);
		}
	}

	private PDPageXYZDestination createDestination(RenderingContext c, Box box) {
	    return PdfBoxBookmarkManager.createBoxDestination(c, _od.getWriter(), _od, _dotsPerPoint, _root, box);
	}

	public static Rectangle2D createTargetArea(RenderingContext c, Box box, float pageHeight, AffineTransform transform,
			Box _root, PdfBoxOutputDevice _od) {
		Rectangle bounds = PagedBoxCollector.findAdjustedBoundsForContentBox(c, box); 
		
		if (!c.isInPageMargins()) {
		    int shadow = c.getShadowPageNumber();
		    Rectangle pageBounds = shadow == -1 ? 
		        c.getPage().getDocumentCoordinatesContentBounds(c) :
		        c.getPage().getDocumentCoordinatesContentBoundsForInsertedPage(c, shadow);
		    
		    bounds = bounds.intersection(pageBounds);
		}
		
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
		Rectangle2D targetArea;
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
		link.c = (RenderingContext) c.clone();
		link.box = box;
		link.page = page;
		link.pageHeight = pageHeight;
		link.transform = (AffineTransform) transform.clone();
		link.targetArea = calcTotalLinkArea(c, box, pageHeight, transform);

		_links.add(link);
	    }
	}

	public void processLinks(PdfBoxAccessibilityHelper pdfUa) {
	    this._pdfUa = pdfUa;
		for (LinkDetails link : _links) {
			processLink(link);
		}
	}
}

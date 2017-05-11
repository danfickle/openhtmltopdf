package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.layout.SharedContext;
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
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class PdfBoxLinkManager {
    
    private final Map<PDPage, Set<String>> _linkTargetAreas;
    private final SharedContext _sharedContext;
    private final float _dotsPerPoint;
    private final Box _root;
    private final PdfBoxOutputDevice _od;
    private final List _links;
    
    public PdfBoxLinkManager(SharedContext ctx, float dotsPerPoint, Box root, PdfBoxOutputDevice od)
    {
        this._sharedContext = ctx;
        this._dotsPerPoint = dotsPerPoint;
        this._root = root;
        this._od = od;
        this._linkTargetAreas = new HashMap<PDPage,Set<String>>();
        this._links = new ArrayList();
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

    private String createRectKey(Rectangle2D rect) {
        return rect.getMinX() + ":" + rect.getMaxY() + ":" + rect.getMaxX() + ":" + rect.getMinY();
    }

    private Rectangle2D checkLinkArea(PDPage page, RenderingContext c, Box box, float pageHeight, AffineTransform transform) {
        Rectangle2D targetArea = calcTotalLinkArea(c, box, pageHeight, transform);
        String key = createRectKey(targetArea);
        Set<String> keys = _linkTargetAreas.get(page);
        if( keys == null ) {
            keys = new HashSet<String>();
            _linkTargetAreas.put(page,keys);
        }
        if (keys.contains(key)) {
            return null;
        }
        keys.add(key);
        return targetArea;
    }

    public void processLink(RenderingContext c, Box box, PDPage page, float pageHeight, AffineTransform transform) {
        Element elem = box.getElement();
        if (elem != null) {
            NamespaceHandler handler = _sharedContext.getNamespaceHandler();
            String uri = handler.getLinkUri(elem);
            if (uri != null) {
                if (uri.length() > 1 && uri.charAt(0) == '#') {
                    String anchor = uri.substring(1);
                    Box target = _sharedContext.getBoxById(anchor);
                    if (target != null) {
                        PDPageXYZDestination dest = createDestination(c, target);

                        PDAction action;
                        if (handler.getAttributeValue(elem, "onclick") != null && !"".equals(handler.getAttributeValue(elem, "onclick"))) {
                            action = new PDActionJavaScript(handler.getAttributeValue(elem, "onclick"));
                        } else {
                            PDActionGoTo go = new PDActionGoTo();
                            go.setDestination(dest);
                            action = go;
                        }

                        Rectangle2D targetArea = checkLinkArea(page, c, box, pageHeight, transform);
                        if (targetArea == null) {
                            return;
                        }

                        PDAnnotationLink annot = new PDAnnotationLink();
                        annot.setAction(action);
                        annot.setRectangle(new PDRectangle((float) targetArea.getMinX(), (float) targetArea.getMinY(), (float) targetArea.getWidth(), (float) targetArea.getHeight()));
                        
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
                } else if (uri.indexOf("://") != -1) {
                    PDActionURI uriAct = new PDActionURI();
                    uriAct.setURI(uri);

                    Rectangle2D targetArea = checkLinkArea(page, c, box, pageHeight, transform);
                    if (targetArea == null) {
                        return;
                    }
                    PDAnnotationLink annot = new PDAnnotationLink();
                    annot.setAction(uriAct);
                    annot.setRectangle(new PDRectangle((float) targetArea.getMinX(), (float) targetArea.getMinY(), (float) targetArea.getWidth(), (float) targetArea.getHeight()));
                    
                    PDBorderStyleDictionary styleDict = new PDBorderStyleDictionary();
                    styleDict.setWidth(0);
                    styleDict.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
                    annot.setBorderStyle(styleDict);
                    
                    try {
                        page.getAnnotations().add(annot);
                    } catch (IOException e) {
                        throw new PdfContentStreamAdapter.PdfException("processLink", e);
                    }
                }
            }
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

    public static Rectangle2D createTargetArea(RenderingContext c, Box box, float pageHeight, AffineTransform transform, Box _root, PdfBoxOutputDevice _od) {
        Rectangle bounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), c);
        PageBox page = _root.getLayer().getPage(c, bounds.y);

        float bottom = _od.getDeviceLength(page.getBottom() - (bounds.y + bounds.height)
               + page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM));
        float left = _od.getDeviceLength(page.getMarginBorderPadding(c, CalculatedStyle.LEFT) + bounds.x);

        Rectangle2D result = new Rectangle2D.Float(left, bottom, _od.getDeviceLength(bounds.width), _od.getDeviceLength(bounds.height));
        return result;
    }
    
    public static class LinkDetails {
        
        RenderingContext c;
        Box box;
        PDPage page;
        float pageHeight;
        AffineTransform transform;   
    }
    
    public void processLinkLater(RenderingContext c, Box box, PDPage page, float pageHeight, AffineTransform transform)
    {
        LinkDetails link = new LinkDetails();
        link.c = c;
        link.box = box;
        link.page = page;
        link.pageHeight = pageHeight;
        link.transform = transform;
        
        _links.add(link);
    }
    
    public void processLinks()
    {
        for (int i = 0; i < _links.size(); i++)
        {
            LinkDetails link = (LinkDetails) _links.get(i);
            processLink(link.c, link.box, link.page, link.pageHeight, link.transform);
        }
    }
}

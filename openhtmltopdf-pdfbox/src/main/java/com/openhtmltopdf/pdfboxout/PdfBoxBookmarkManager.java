package com.openhtmltopdf.pdfboxout;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitHeightDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxBookmarkManager {
    private final List<Bookmark> _bookmarks = new ArrayList<Bookmark>();
    private final PDDocument _writer;
    private final SharedContext _sharedContext;
    private final float _dotsPerPoint;
    private final PdfBoxFastOutputDevice _od;
    private final Document _xml;
    private PDDestination _defaultDestination;

    public PdfBoxBookmarkManager(Document xml, PDDocument doc, SharedContext sharedContext, float dotsPerPoint, PdfBoxFastOutputDevice od) {
        this._writer = doc;
        this._sharedContext = sharedContext;
        this._dotsPerPoint = dotsPerPoint;
        this._od = od;
        this._xml = xml;
    }

    public void writeOutline(RenderingContext c, Box root) {
        if (_bookmarks.size() > 0) {
            // Create a default destination to the top of the first page.
            PDPageFitHeightDestination dest = new PDPageFitHeightDestination();
            dest.setPage(_writer.getPage(0));
            _defaultDestination = dest;

            PDDocumentOutline outline = _writer.getDocumentCatalog().getDocumentOutline();

            if (outline == null) {
                outline = new PDDocumentOutline();
                _writer.getDocumentCatalog().setDocumentOutline(outline);
            }

            writeBookmarks(c, root, outline, _bookmarks);
        }
    }

    private void writeBookmarks(RenderingContext c, Box root, PDOutlineNode parent, List<Bookmark> bookmarks) {
        for (Bookmark bookmark : bookmarks) {
            writeBookmark(c, root, parent, bookmark);
        }
    }

    private void writeBookmark(RenderingContext c, Box root, PDOutlineNode parent, Bookmark bookmark) {
        String href = bookmark.getHRef();
        PDPageXYZDestination target = null;

        if (href.length() > 0 && href.charAt(0) == '#') {
            Box box = _sharedContext.getBoxById(href.substring(1));

            if (box != null) {
                target = createBoxDestination(c, _writer, _od, _dotsPerPoint, root, box);
            }
        }
        
        if (target == null) {
            XRLog.general(Level.WARNING, "Could not find valid target for bookmark. Bookmark href = " + href);
        }

        PDOutlineItem outline = new PDOutlineItem();
        outline.setDestination(target == null ? _defaultDestination : target);
        outline.setTitle(bookmark.getName());
        parent.addLast(outline);
        writeBookmarks(c, root, outline, bookmark.getChildren());
    }

    /**
     * Creates a <code>PDPageXYZDestination</code> with the Y set to the min Y of the border box and 
     * the X and Z set to null. Takes into account any transforms set for the box as well as inserted overflow pages.
     */
    public static PDPageXYZDestination createBoxDestination(
            RenderingContext c, PDDocument writer, PdfBoxFastOutputDevice od, float dotsPerPoint, Box root, Box box) {
        
        List<PageBox> pages = root.getLayer().getPages();
        Rectangle bounds = PagedBoxCollector.findAdjustedBoundsForBorderBox(c, box, pages);

        int pageBoxIndex = PagedBoxCollector.findPageForY(c, bounds.getMinY(), pages);
        PageBox page = pages.get(pageBoxIndex);

        int distanceFromTop = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
        distanceFromTop += bounds.getMinY() - page.getTop();

        int shadowPage = PagedBoxCollector.getShadowPageForBounds(c, bounds, page);

        int pdfPageIndex = shadowPage == -1 ? page.getBasePagePdfPageIndex() : shadowPage + 1 + page.getBasePagePdfPageIndex();

        PDPageXYZDestination target = new PDPageXYZDestination();
        target.setTop((int) (od.normalizeY(distanceFromTop, page.getHeight(c)) / dotsPerPoint));
        target.setPage(writer.getPage(pdfPageIndex));
        
        return target;
    }

    public void loadBookmarks() {
        Document doc = _xml;
        Element head = DOMUtil.getChild(doc.getDocumentElement(), "head");

        if (head != null) {
            Element bookmarks = DOMUtil.getChild(head, "bookmarks");
            if (bookmarks != null) {
                List<Element> l = DOMUtil.getChildren(bookmarks, "bookmark");
                if (l != null) {
                    for (Element e : l) {
                        loadBookmark(null, e);
                    }
                }
            }
        }
    }

    private void loadBookmark(Bookmark parent, Element bookmark) {
        Bookmark us = new Bookmark(bookmark.getAttribute("name"), bookmark.getAttribute("href"));
        if (parent == null) {
            _bookmarks.add(us);
        } else {
            parent.addChild(us);
        }
        List<Element> l = DOMUtil.getChildren(bookmark, "bookmark");
        if (l != null) {
            for (Element e : l) {
                loadBookmark(us, e);
            }
        }
    }

    private static class Bookmark {
        private final String _name;
        private final String _HRef;

        private List<Bookmark> _children;

        public Bookmark(String name, String href) {
            _name = name;
            _HRef = href;
        }

        public String getHRef() {
            return _HRef;
        }

        public String getName() {
            return _name;
        }

        public void addChild(Bookmark child) {
            if (_children == null) {
                _children = new ArrayList<Bookmark>();
            }
            _children.add(child);
        }

        public List<Bookmark> getChildren() {
            return _children == null ? Collections.<Bookmark>emptyList() : _children;
        }
    }
}

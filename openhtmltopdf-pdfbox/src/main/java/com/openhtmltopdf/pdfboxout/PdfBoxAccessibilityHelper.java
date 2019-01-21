package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.Revisions;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.InlineLayoutBox;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxAccessibilityHelper {
    private final List<List<StructureItem>> _pageContentItems = new ArrayList<>();
    private final PdfBoxFastOutputDevice _od;
    private final Box _rootBox;
    private final Document _doc;
    private final StructureItem _root;

    private int _nextMcid;
    private PdfContentStreamAdapter _cs;
    private RenderingContext _ctx;
    private PDPage _page;
    private float _pageHeight;
    private AffineTransform _transform;
    
    public PdfBoxAccessibilityHelper(PdfBoxFastOutputDevice od, Box root, Document doc) {
        this._od = od;
        this._rootBox = root;
        this._doc = doc;
        
        StructureItem rootStruct = new StructureItem(null, null);
        _root = rootStruct;
    }

    private static class StructureItem {
        private final StructureType type;
        private Box box;
        private final List<StructureItem> children = new ArrayList<>();
        
        private COSDictionary dict;
        private PDStructureElement elem;
        private PDStructureElement parentElem;
        private int mcid = -1;
        private StructureItem parent;
        private PDPage page;
        private PDRectangle pdRect;
        
        private StructureItem(StructureType type, Box box) {
            this.type = type;
            this.box = box;
        }
        
        @Override
        public String toString() {
            return box != null ? box.toString() : "null";
        }
    }
 
    public void finishPdfUa() {
        PDStructureTreeRoot root = _od.getWriter().getDocumentCatalog().getStructureTreeRoot();
        if (root == null) {
            root = new PDStructureTreeRoot();
            
            HashMap<String, String> roleMap = new HashMap<>();
            roleMap.put("Annotation", "Span");
            roleMap.put("Artifact", "P");
            roleMap.put("Bibliography", "BibEntry");
            roleMap.put("Chart", "Figure");
            roleMap.put("Diagram", "Figure");
            roleMap.put("DropCap", "Figure");
            roleMap.put("EndNote", "Note");
            roleMap.put("FootNote", "Note");
            roleMap.put("InlineShape", "Figure");
            roleMap.put("Outline", "Span");
            roleMap.put("Strikeout", "Span");
            roleMap.put("Subscript", "Span");
            roleMap.put("Superscript", "Span");
            roleMap.put("Underline", "Span");
            root.setRoleMap(roleMap);

            PDStructureElement rootElem = new PDStructureElement(StandardStructureTypes.DOCUMENT, null);
            
            String lang = _doc.getDocumentElement().getAttribute("lang");
            rootElem.setLanguage(lang.isEmpty() ? "EN-US" : lang);
            
            root.appendKid(rootElem);
            
            _root.elem = rootElem;
            finishStructure(_root, _root);
            
            _od.getWriter().getDocumentCatalog().setStructureTreeRoot(root);
        }
        
        COSArray numTree = new COSArray();
        
        for (int i = 0; i < _pageContentItems.size(); i++) {
            PDPage page = _od.getWriter().getPage(i);
            List<StructureItem> pageItems = _pageContentItems.get(i);
            
            COSArray mcidParentReferences = new COSArray();
            for (StructureItem item : pageItems) {
System.out.println("%%%%%%%item = " + item + ", parent = " + item.parentElem + ", mcid == " + item.mcid);
                mcidParentReferences.add(item.parentElem);
            }
        
            numTree.add(COSInteger.get(i));
            numTree.add(mcidParentReferences);
            
            page.getCOSObject().setItem(COSName.STRUCT_PARENTS, COSInteger.get(i));
        }
        
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.NUMS, numTree);
    
        PDNumberTreeNode numberTreeNode = new PDNumberTreeNode(dict, dict.getClass());
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTreeNextKey(_pageContentItems.size());
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTree(numberTreeNode);
    }
        
    private String chooseTag(StructureItem item) {
        if (item.box != null) {
            if (item.box.getLayer() != null) {
                return StandardStructureTypes.SECT;
            } else if (item.box instanceof BlockBox) {
                BlockBox block = (BlockBox) item.box;
                
                if (block.isFloated()) {
                    return StandardStructureTypes.DIV;
                } else if (block.isInline()) {
                    return StandardStructureTypes.SPAN;
                } else if (block.getElement() != null && block.getElement().getNodeName().equals("p")) {
                    return StandardStructureTypes.P;
                } else {
                    return StandardStructureTypes.DIV;
                }
                
                // TODO: Tables.
            } else {
                return StandardStructureTypes.SPAN;
            }
        }
        
        return StandardStructureTypes.SPAN;
    }
    
    private void finishStructure(StructureItem item, StructureItem parent) {
        for (StructureItem child : item.children) {
            if (child.mcid == -1 && child.pdRect == null) {
                // A structual element such as Div, Sect, p, etc
                // which contains other structual elements or content items (text).
                
                if (child.children.isEmpty()) {
                    // There is no point in outputting empty structual elements.
                    continue;
                }
                
                if (child.box instanceof LineBox &&
                    !child.box.hasNonTextContent(_ctx)) {
                    // We skip line boxes in the tree.
                    finishStructure(child, parent);
                } else {
                    String pdfTag = chooseTag(child);
                
                    child.parentElem = parent.elem;
                    child.elem = new PDStructureElement(pdfTag, child.parentElem);
                    child.elem.setParent(child.parentElem);
                    child.elem.setPage(child.page);

                    child.parentElem.appendKid(child.elem);

                    // Recursively, depth first, process the structual tree.
                    finishStructure(child, child);
                }
            } else if (child.mcid == -1) {
                // We have a bounding box, so must be a figure (image or replaced, etc).

                if (child.children.isEmpty()) {
                    // There is no point in outputting empty structual elements.
                    continue;
                }
             
                child.parentElem = parent.elem;
                child.elem = new PDStructureElement(StandardStructureTypes.Figure, child.parentElem);
                child.elem.setParent(child.parentElem);
                child.elem.setPage(child.page);
                
                // Add alt text.
                String alternateText = child.box.getElement() == null ? "" : child.box.getElement().getAttribute("alt");
                if (alternateText.isEmpty()) {
                    XRLog.general(Level.WARNING, "No alt attribute provided for image/replaced in PDF/UA document.");
                }
                child.elem.setAlternateDescription(alternateText);

                // Add bounding box attribute.
                COSDictionary attributeDict = new COSDictionary();
                attributeDict.setItem(COSName.BBOX, child.pdRect);
                attributeDict.setItem(COSName.O, COSName.getPDFName("Layout"));
                
                Revisions<PDAttributeObject> attributes = new Revisions<>();
                attributes.addObject(PDAttributeObject.create(attributeDict), 0);
                child.elem.setAttributes(attributes);

                child.parentElem.appendKid(child.elem);
                
                finishStructure(child, child);
            } else if (child.type == StructureType.TEXT ||
                       child.type == StructureType.REPLACED) {
                // A content item (text or replaced image), we need to add it to its parent structual item.
                
                if (child.page == parent.page) { 
                    // If this is on the same page as its parent structual element
                    // we can just use the dict with mcid in it only.
                    child.parentElem = parent.elem;
                    child.parentElem.appendKid(new PDMarkedContent(child.type == StructureType.TEXT ? COSName.getPDFName("Span") : COSName.getPDFName("Figure"), child.dict));
                } else {
                    // Otherwise we need a more complete dict with the page.
                    child.parentElem = parent.elem;
                    child.dict = new COSDictionary();
                    child.dict.setItem(COSName.TYPE, COSName.getPDFName("MCR"));
                    child.dict.setItem(COSName.PG, child.page);
                    child.dict.setInt(COSName.MCID, child.mcid);

                    PDMarkedContentReference ref = new PDMarkedContentReference(child.dict);
                    child.parentElem.appendKid(ref);
                }
            }
        }
    }
    
    private Element getBoxElement(Box box) {
        if (box.getElement() != null) {
            return box.getElement();
        } else if (box.getParent() != null) {
            return getBoxElement(box.getParent());
        } else {
            return _doc.getDocumentElement();
        }
    }
    
    private COSDictionary createMarkedContentDictionary() {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.MCID, _nextMcid);
        _nextMcid++;
        return dict;
    }
    
    private void ensureAncestorTree(StructureItem child, Box parent) {
        // Walk up the ancestor tree making sure they all have accessibility objects.
        while (parent != null && parent.getAccessibilityObject() == null) {
            StructureItem parentItem = createStructureItem(null, parent);
            parent.setAccessiblityObject(parentItem);
            parentItem.children.add(child);
            child.parent = parentItem;
            child = parentItem;
            parent = parent.getParent();
        }
    }
    
    private StructureItem createStructureItem(StructureType type, Box box) {
        StructureItem child = (StructureItem) box.getAccessibilityObject();
        
        if (child == null) {
            child = new StructureItem(type, box);
            child.page = _page;
            
            box.setAccessiblityObject(child);
            
            ensureAncestorTree(child, box.getParent());
            ensureParent(box, child);

            if (child.box instanceof BlockBox) {
                BlockBox bb = (BlockBox) child.box;

                if (bb.isReplaced()) {
                    // For replaced elements we will need to create a BBox.
                    Rectangle2D rect = PdfBoxFastLinkManager.createTargetArea(
                            _ctx, child.box, _pageHeight, _transform, _rootBox, _od);
                    child.pdRect = new PDRectangle(
                            (float) rect.getMinX(),
                            (float) rect.getMinY(),
                            (float) rect.getWidth(),
                            (float) rect.getHeight());
                }
            }
        }
        
        return child;
    }

    public void ensureParent(Box box, StructureItem child) {
        if (child.parent == null) {
            if (box.getParent() != null) {
                StructureItem parent = (StructureItem) box.getParent().getAccessibilityObject();
                parent.children.add(child);
                child.parent = parent;
            } else {
                _root.children.add(child);
                child.parent = _root;
            }
        }
    }
    
    private StructureItem createMarkedContentStructureItem(StructureType type, Box box) {
        StructureItem current = new StructureItem(type, box);
        
        ensureAncestorTree(current, box.getParent());
        ensureParent(box, current);

        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.page = _page;
        
        _pageContentItems.get(_pageContentItems.size() - 1).add(current);

        return current;
    }
    
    private StructureItem createFigureContentStructureItem(StructureType type, Box box) {
        StructureItem parent = (StructureItem) box.getAccessibilityObject();
        
        if (parent == null ||
            parent.children.size() > 0) {
            return null;
        }
                
        StructureItem current = new StructureItem(type, box);
        
        ensureAncestorTree(current, box.getParent());
        
        current.parent = parent;
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.page = _page;

        current.parent.children.add(current);
        
        _pageContentItems.get(_pageContentItems.size() - 1).add(current);
        
        return current;
    }
    
    private COSDictionary createBackgroundArtifact(StructureType type, Box box) {
        Rectangle2D rect = PdfBoxFastLinkManager.createTargetArea(_ctx, box, _pageHeight, _transform, _rootBox, _od);
        PDRectangle pdRect = new PDRectangle((float) rect.getMinX(), (float) rect.getMinY(), (float) rect.getWidth(), (float) rect.getHeight());
        
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.BACKGROUND);
        dict.setItem(COSName.BBOX, pdRect);
        
        return dict;
    }
    
    public Object startStructure(StructureType type, Box box) {
            switch (type) {
            case LAYER:
            case FLOAT:
            case BLOCK: {
                createStructureItem(type, box);
                return Boolean.FALSE;
            }
            case INLINE: {
                if (box.hasNonTextContent(_ctx) || 
                    box.getChildCount() > 0 ||
                    (box instanceof InlineLayoutBox && !((InlineLayoutBox) box).isAllTextItems(_ctx))) {
                    createStructureItem(type, box);
                }
                return Boolean.FALSE;
            }
            case BACKGROUND: {
                if (box.hasNonTextContent(_ctx)) {
                    COSDictionary current = createBackgroundArtifact(type, box);
                    _cs.beginMarkedContent(COSName.ARTIFACT, current);
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
            case TEXT: {
                StructureItem current = createMarkedContentStructureItem(type, box);
                _cs.beginMarkedContent(COSName.getPDFName(StandardStructureTypes.SPAN), current.dict);
                return Boolean.TRUE;
            }
            case REPLACED: {
                createStructureItem(type, box);
                StructureItem current = createFigureContentStructureItem(type, box);
                if (current != null) {
                    _cs.beginMarkedContent(COSName.getPDFName(StandardStructureTypes.Figure), current.dict);
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
            default: {
                return Boolean.FALSE;
            }
            }
    }

    public void endStructure(Object token) {
        Boolean marked = (Boolean) token;
        
        if (marked.booleanValue()) {
            _cs.endMarkedContent();
        }
    }

    public void startPage(PDPage page, PdfContentStreamAdapter cs, RenderingContext ctx, float pageHeight, AffineTransform transform) {
        this._cs = cs;
        this._ctx = ctx;
        this._nextMcid = 0;
        this._page = page;
        this._pageHeight = pageHeight;
        this._transform = transform;
        this._pageContentItems.add(new ArrayList<>());
    }
    
    public void endPage() {
        
    }
}

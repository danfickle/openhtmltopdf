package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDArtifactMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.RenderingContext;

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
System.out.println("%%%%%%%item = " + item + ", parent = " + item.parentElem);
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
            if (child.mcid == -1) {
                if (child.children.isEmpty()) {
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
System.out.println("ADDING$$: " + child + " :::: " + child.elem + "-----" + pdfTag);
                    child.parentElem.appendKid(child.elem);
                    
                    finishStructure(child, child);
                }
            } else if (child.type == StructureType.TEXT) {
                if (child.page == parent.page) { 
                    // If this is on the same page as its parent structual element
                    // we can just use the dict with mcid in it only.
System.out.println("ADDING MCID DIRECT: " + child.mcid + " , " + child.page);
                    child.parentElem = parent.elem;
                    child.parentElem.appendKid(new PDMarkedContent(COSName.getPDFName("Span"), child.dict));
                } else {
                    // Otherwise we need a more complete dict with the page.
                    child.parentElem = parent.elem;
                    child.dict = new COSDictionary();
                    child.dict.setItem(COSName.TYPE, COSName.getPDFName("MCR"));
                    child.dict.setItem(COSName.PG, child.page);
                    child.dict.setInt(COSName.MCID, child.mcid);
System.out.println("ADDING MCR: " + child.mcid + " , " + child.page);
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
        } 
        
        if (child.box == null) {
            child.box = box;
        }

System.out.println("-------ADD: " + child + " && " + child.parent);
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

System.out.println("+++++++ADD: " + current + " !! " + current.parent + " !! " + current.mcid);
        
        return current;
    }
    
    private COSDictionary createBackgroundArtifact(StructureType type, Box box) {
        Rectangle2D rect = PdfBoxFastLinkManager.createTargetArea(_ctx, box, _pageHeight, _transform, _rootBox, _od);
        PDRectangle pdRect = new PDRectangle((float) rect.getMinX(), (float) rect.getMinY(), (float) rect.getWidth(), (float) rect.getHeight());
        
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.BACKGROUND);
        dict.setItem(COSName.BBOX, pdRect);
        
System.out.println("CREATING BG ARTIFACT: " + box + " at: " + rect);
        
        return dict;
    }
    
    public void startStructure(StructureType type, Box box) {
            switch (type) {
            case LAYER:
            case FLOAT:
            case BLOCK: {
                createStructureItem(type, box);
                break;
            }
            case INLINE: {
                if (box.hasNonTextContent(_ctx)) {
                    createStructureItem(type, box);
                }
                break;
            }
            case BACKGROUND: {
                if (box.hasNonTextContent(_ctx)) {
                    COSDictionary current = createBackgroundArtifact(type, box);
                    _cs.beginMarkedContent(COSName.ARTIFACT, current);
                }
                break;
            }
            case TEXT: {
                StructureItem current = createMarkedContentStructureItem(type, box);
                _cs.beginMarkedContent(COSName.getPDFName(StandardStructureTypes.SPAN), current.dict);
                break;
            }
            default:
                break;
            }
    }

    public void endStructure(StructureType type, Box box) {
            switch (type) {
            case LAYER:
            case FLOAT:
            case BLOCK: {
                break;
            }
            case INLINE: {
                break;
            }
            case BACKGROUND: {
                if (box.hasNonTextContent(_ctx)) {
                    _cs.endMarkedContent();
                }
                break;
            }
            case TEXT: {
                _cs.endMarkedContent();
                break;
            }
            default:
                break;
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

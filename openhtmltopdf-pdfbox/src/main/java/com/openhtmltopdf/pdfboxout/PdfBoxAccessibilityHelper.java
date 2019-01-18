package com.openhtmltopdf.pdfboxout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDArtifactMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;

public class PdfBoxAccessibilityHelper {
    private final Map<Element, StructureItem> _structureMap = new HashMap<>();
    private final List<List<StructureItem>> _pageContentItems = new ArrayList<>();
    private final PdfBoxFastOutputDevice _od;
    
    private Document _doc;
    private StructureItem _root;

    private int _nextMcid;
    private PdfContentStreamAdapter _cs;
    private RenderingContext _ctx;
    private PDPage _page;
    
    public PdfBoxAccessibilityHelper(PdfBoxFastOutputDevice od) {
        this._od = od;
    }

    private static class StructureItem {
        private final StructureType type;
        private final Box box;
        private final List<StructureItem> children = new ArrayList<>();
        
        private COSDictionary dict;
        private PDStructureElement elem;
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
            
            StructureItem rootStruct = _structureMap.get(_doc.getDocumentElement());
            rootStruct.elem = rootElem;
            finishStructure(rootStruct);
            
            _od.getWriter().getDocumentCatalog().setStructureTreeRoot(root);
        }
        
        COSArray numTree = new COSArray();
        
        for (int i = 0; i < _pageContentItems.size(); i++) {
            PDPage page = _od.getWriter().getPage(i);
            List<StructureItem> pageItems = _pageContentItems.get(i);
            
            COSArray mcidParentReferences = new COSArray();
            for (StructureItem item : pageItems) {
                System.out.println("item = " + item + ", parent = " + item.parent + " ,," + item.parent.elem);
                mcidParentReferences.add(item.parent.elem);
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
        return item.box != null && item.box.getStyle().isInline() ? "Span" : "P"; // TODO.
    }
    
    private void finishStructure(StructureItem item) {
        System.out.println("item = " + item + " ,," + item.mcid);
        for (StructureItem child : item.children) {
            if (child.mcid == -1) {
                if (child.children.isEmpty()) {
                    continue;
                }
                
                String pdfTag = chooseTag(child);
                
                child.elem = new PDStructureElement(pdfTag, item.elem);
                System.out.println("child = " + child + "!!!!!!" + child.elem);
                child.elem.setParent(item.elem);
                child.elem.setPage(child.page);

                item.elem.appendKid(child.elem);
                
                finishStructure(child);
            } else if (child.type == StructureType.TEXT) {
                item.elem.appendKid(new PDMarkedContent(COSName.getPDFName("Span"), child.dict));
            } else if (child.type == StructureType.BACKGROUND) {
                item.elem.appendKid(new PDArtifactMarkedContent(child.dict));
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
    
    private StructureItem findParentStructualElement(Box box) {
        Element elem = getBoxElement(box);
        Node parent = elem.getParentNode();

        StructureItem item;
        
        if (parent == null || parent instanceof Document) {
            item = _root;
        } else {
            item = _structureMap.get(elem.getParentNode());
        }

        System.out.println("ch = " + box + " parent = " + item + ", " + elem.getParentNode().getNodeName());
        
        return item;
    }
    
    private StructureItem findCurrentStructualElement(Box box) {
        Element elem = getBoxElement(box);
        
        return _structureMap.get(elem);
    }
    
    private COSDictionary createMarkedContentDictionary() {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.MCID, _nextMcid);
        _nextMcid++;
        return dict;
    }
    
    private StructureItem createStructureItem(StructureType type, Box box) {
        
        Element elem = getBoxElement(box);
        StructureItem item = _structureMap.get(elem);
        
        if (item == null) {
            item = new StructureItem(type, box);
            _structureMap.put(elem, item);

            item.parent = findParentStructualElement(box);
            item.parent.children.add(item);
            
            item.page = _page;
        }

        //System.out.println("-------ADD: " + item + " , &&" + item.parent);
        return item;
    }
    
    private StructureItem createMarkedContentStructureItem(StructureType type, Box box) {
        StructureItem current = new StructureItem(type, box);
        StructureItem parent = findCurrentStructualElement(box);
        System.out.println("mcid prent = " + parent + " , " + current);
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.parent = parent;
        current.parent.children.add(current);
        
        _pageContentItems.get(_pageContentItems.size() - 1).add(current);
        
        return current;
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
                    StructureItem current = createMarkedContentStructureItem(type, box);
                    _cs.beginMarkedContent(COSName.ARTIFACT, current.dict);    
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

    public void startPage(PDPage page, PdfContentStreamAdapter cs, RenderingContext ctx) {
        this._cs = cs;
        this._ctx = ctx;
        this._nextMcid = 0;
        this._page = page;
        this._pageContentItems.add(new ArrayList<>());
    }
    
    public void endPage() {
        
    }

    public void setDocument(Document doc) {
        this._doc = doc;
        
        StructureItem rootStruct = new StructureItem(null, null);
        _structureMap.put(_doc.getDocumentElement(), rootStruct);
        _root = rootStruct;
    }
    
}

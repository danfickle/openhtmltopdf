package com.openhtmltopdf.pdfboxout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
import org.w3c.dom.Element;

import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;

public class PdfBoxAccessibilityHelper {
    private final Map<Element, StructureItem> _structureMap = new HashMap<>();
    private final Deque<StructureItem> _structureStack = new ArrayDeque<>();
    private final List<StructureItem> _pageContentItems = new ArrayList<>();
    private final COSArray _numTree = new COSArray();
    private final PdfBoxFastOutputDevice _od;
    
    private int _nextMcid = 0;
    private int _nextPageIndex;
    
    private PDPage _page;
    private PdfContentStreamAdapter _cs;
    private RenderingContext _ctx;
    private PDStructureElement _rootElem;

    public PdfBoxAccessibilityHelper(PdfBoxFastOutputDevice od) {
        this._od = od;
    }

    public void finishPdfUa() {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.NUMS, _numTree);
    
        PDNumberTreeNode numberTreeNode = new PDNumberTreeNode(dict, dict.getClass());
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTreeNextKey(_nextPageIndex);
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTree(numberTreeNode);
    }

    private static class StructureItem {
        private final StructureType type;
        private final Box box;
        private final List<StructureItem> children = new ArrayList<>();
        
        private COSDictionary dict;
        private PDStructureElement elem;
        private int mcid = -1;
        private StructureItem parent;
        private String pdfTag;
        
        private StructureItem(StructureType type, Box box) {
            this.type = type;
            this.box = box;
        }
        
        @Override
        public String toString() {
            return box.toString();
        }
    }
    
    private String chooseTag(StructureItem item) {
        return item.box.getStyle().isInline() ? "SPAN" : "P"; // TODO.
    }
    
    private void finishStructure(StructureItem item) {
        for (StructureItem child : item.children) {
            if (child.mcid == -1) {
                String pdfTag = chooseTag(child);
                
                child.elem = new PDStructureElement(pdfTag, item.elem);
                child.elem.setPage(_page);
                child.elem.setLanguage("EN-US"); // TODO: Only set lang if different from parent.
                child.elem.setParent(item.elem);
                
                item.elem.appendKid(child.elem);
                
                finishStructure(child);
            } else if (child.type == StructureType.TEXT) {
                item.elem.appendKid(new PDMarkedContent(COSName.getPDFName("SPAN"), child.dict));
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
            return null;
        }
    }
    
    private StructureItem findParentStructualElement(Box box) {
        Element elem = getBoxElement(box);
        
        StructureItem item = _structureMap.get(elem);
        
        if (item != null) {
            return item;
        } else {
            return _structureMap.get(elem.getParentNode());
        }
    }
    
    private COSDictionary createMarkedContentDictionary() {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.MCID, _nextMcid);
        _nextMcid++;
        return dict;
    }
    
    private StructureItem createStructureItem(StructureType type, Box box) {
        System.out.println("STRUCT: " + box);
        StructureItem item = new StructureItem(type, box);
        StructureItem parent = _structureStack.getLast();

        parent.children.add(item);
        _structureStack.addLast(item);
        _structureMap.put(box.getElement(), item);
        
        return item;
    }
    
    private StructureItem createMarkedContentStructureItem(StructureType type, Box box) {
        System.out.println("MARK: " + box);
        StructureItem current = new StructureItem(type, box);
        StructureItem parent = findParentStructualElement(box);
        
        System.out.println("mark = " + current.box + " , parent = " + parent.box);
        
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.parent = parent;
        
        _pageContentItems.add(current);
        parent.children.add(current);
        
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
                System.out.println("REMV1: " + box);
                _structureStack.removeLast();
                break;
            }
            case INLINE: {
                if (box.hasNonTextContent(_ctx)) {
                    System.out.println("REMV2: " + box);
                    _structureStack.removeLast();
                }
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
        this._page = page;
        this._cs = cs;
        this._ctx = ctx;
        
        StructureItem item = new StructureItem(null, null);
        _structureStack.clear();
        _structureStack.addLast(item);
        
        _structureMap.clear();
    }
    
    public void endPage() {
        
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

            _rootElem = new PDStructureElement(StandardStructureTypes.DOCUMENT, null);
            _rootElem.setLanguage("EN-US"); // TODO
            root.appendKid(_rootElem);
            
            _od.getWriter().getDocumentCatalog().setStructureTreeRoot(root);
        }

        StructureItem page = _structureStack.removeLast();

        PDStructureElement pageElem = new PDStructureElement(StandardStructureTypes.NON_STRUCT, _rootElem);
        pageElem.setPage(_page);
        page.elem = pageElem;
        _rootElem.appendKid(pageElem);;
        
        finishStructure(page);
        
        _page.getCOSObject().setItem(COSName.STRUCT_PARENTS, COSInteger.get(_nextPageIndex));
        
        COSArray mcidParentReferences = new COSArray();
        for (StructureItem item : _pageContentItems) {
            mcidParentReferences.add(item.parent.elem);
        }
        
        _numTree.add(COSInteger.get(_nextPageIndex));
        _numTree.add(mcidParentReferences);
        
        _nextPageIndex++;
        _pageContentItems.clear();
        _nextMcid = 0;
    }
    
}

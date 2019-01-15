package com.openhtmltopdf.pdfboxout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
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

public class PdfBoxAccessibilityHelper {
    
    private final Deque<StructureItem> _structureStack = new ArrayDeque<>();
    private final List<StructureItem> _pageContentItems = new ArrayList<>();
    private final COSArray _numTree = new COSArray();
    private final PdfBoxFastOutputDevice _od;
    
    private int _nextMcid = 0;
    private int _nextPageIndex;
    
    private PDPage _page;
    private PdfContentStreamAdapter _cs;

    public PdfBoxAccessibilityHelper(PdfBoxFastOutputDevice od) {
        this._od = od;
    }

    public void finishPdfUa() {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.NUMS, _numTree);
    
        PDNumberTreeNode numberTreeNode = new PDNumberTreeNode(dict, dict.getClass());
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTreeNextKey(_numTree.size());
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTree(numberTreeNode);
    }

    private static class StructureItem {
        private final StructureType type;
        private final Box box;
        
        private COSDictionary dict;
        private PDStructureElement elem;
        private int mcid = -1;
        
        private StructureItem parent;
        private List<StructureItem> children = new ArrayList<>();
        
        private StructureItem(StructureType type, Box box) {
            this.type = type;
            this.box = box;
        }
        
        @Override
        public String toString() {
            return box.toString();
        }
    }
    
    private void finishStructure(StructureItem item) {
        for (StructureItem child : item.children) {
            switch (child.type) {
            case LAYER:
            case FLOAT:
            case BLOCK:
            case INLINE: {
                System.out.println("Append strcuture: " + child.toString());
                
                child.elem = new PDStructureElement(COSName.P.getName(), item.elem);
                child.elem.setPage(_page);
                child.elem.setLanguage("EN-US"); // TODO: Only set lang if different from parent.
                child.elem.setParent(item.elem);
                
                item.elem.appendKid(child.elem);
                
                finishStructure(child);
            }
            break;
            case TEXT: {
                System.out.println("Append text: " + child);
                item.elem.appendKid(new PDMarkedContent(COSName.P, child.dict));
                _numTree.add(item.elem.getCOSObject());
            }
            break;
            case BACKGROUND: {
                item.elem.appendKid(new PDArtifactMarkedContent(child.dict));
                _numTree.add(item.elem.getCOSObject());
            }
            break;
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
    
    private COSDictionary createMarkedContentDictionary() {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.MCID, _nextMcid);
        _nextMcid++;
        return dict;
    }
    
    public void startStructure(StructureType type, Box box) {
            System.out.println("Start: " + type);
            switch (type) {
            case LAYER:
            case FLOAT:
            case BLOCK:
            case INLINE: {
                //if (!_structureMap.containsKey(box)) {
                    StructureItem item = new StructureItem(type, box);
                    StructureItem parent = _structureStack.getLast();
 
                    parent.children.add(item);
                    _structureStack.addLast(item);
                //}
                break;
            }
            case BACKGROUND: {
                // TODO: Only create if actually has a background or border.
                StructureItem current = new StructureItem(type, box);
                StructureItem parent = _structureStack.getLast();
                
                current.mcid = _nextMcid;
                current.dict = createMarkedContentDictionary();
                current.parent = parent;
                
                _pageContentItems.add(current);
                parent.children.add(current);

                _cs.beginMarkedContent(COSName.ARTIFACT, current.dict);
                break;
            }
            case TEXT: {
                StructureItem current = new StructureItem(type, box);
                StructureItem parent = _structureStack.getLast();

                current.mcid = _nextMcid;
                current.dict = createMarkedContentDictionary();
                current.parent = parent;
                
                _pageContentItems.add(current);
                parent.children.add(current);
                
                _cs.beginMarkedContent(COSName.getPDFName(StandardStructureTypes.SPAN), current.dict);
                break;
            }
            default:
                break;
            }
    }

    public void endStructure(StructureType type, Box box) {

            System.out.println("END: " + type);
            switch (type) {
            case LAYER:
            case FLOAT:
            case BLOCK:
            case INLINE: {
                _structureStack.removeLast();
            }
            break;
            case BACKGROUND:
            case TEXT: {
                // TODO: Only background if actually has a background or border.
                _cs.endMarkedContent();
            }
            default:
                break;
            }
    }

    public void startPage(PDPage page, PdfContentStreamAdapter cs) {
        this._page = page;
        this._cs = cs;
        
        StructureItem item = new StructureItem(null, null);
        _structureStack.clear();
        _structureStack.addLast(item);
    }
    
    public void endPage() {
        StructureItem page = _structureStack.removeLast();
        PDStructureElement item = new PDStructureElement(StandardStructureTypes.DOCUMENT, null);
        item.setPage(_page);
        
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
            _od.getWriter().getDocumentCatalog().setStructureTreeRoot(root);
        }
        root.appendKid(item);
        page.elem = item;
        
        finishStructure(page);
        
        _page.getCOSObject().setItem(COSName.STRUCT_PARENTS, COSInteger.get(_nextPageIndex));
        
        for (int i = 0; i < _pageContentItems.size(); i++) {
            System.out.println("NUM TREE: " + i + ", " + _pageContentItems.get(i) + ", " + _pageContentItems.get(i).parent);
            _numTree.add(COSInteger.get(i));
            _numTree.add(_pageContentItems.get(i).parent.elem);
        }
        
        _nextPageIndex += _pageContentItems.size();
        _pageContentItems.clear();
        _nextMcid = 0;
    }
    
}

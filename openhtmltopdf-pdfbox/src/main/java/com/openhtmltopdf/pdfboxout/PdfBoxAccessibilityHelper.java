package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.Revisions;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.w3c.dom.Document;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.InlineLayoutBox;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.MarkerData;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxAccessibilityHelper {
    // This maps from page to a list of content items.
    private final Map<PDPage, List<GenericContentItem>> _pageContentItems = new LinkedHashMap<>();
    private final Map<PDPage, List<AnnotationWithStructureParent>> _pageAnnotations = new HashMap<>();
    private final PdfBoxFastOutputDevice _od;
    private final Box _rootBox;
    private final Document _doc;
    private final GenericStructualElement _root;
    
    private static final Map<String, Supplier<AbstractStructualElement>> _tagSuppliers;
    
    private int _nextMcid;
    
    // These change with every page.
    private List<GenericContentItem> _contentItems;
    private PdfContentStreamAdapter _cs;
    private RenderingContext _ctx;
    private PDPage _page;
    private float _pageHeight;
    private AffineTransform _transform;
    
    private int _runningLevel;
    
    static {
        _tagSuppliers = createTagSuppliers();
    }
    
    private static Map<String, Supplier<AbstractStructualElement>> createTagSuppliers() {
        Map<String, Supplier<AbstractStructualElement>> suppliers = new HashMap<>();
       
        suppliers.put("ul", ListStructualElement::new);
        suppliers.put("ol", ListStructualElement::new);
        suppliers.put("li", ListItemStructualElement::new);
        
        suppliers.put("table", TableStructualElement::new);
        suppliers.put("tr", TableRowStructualElement::new);
        suppliers.put("td", TableCellStructualElement::new);
        suppliers.put("th", TableHeaderStructualElement::new);
        
        suppliers.put("a", AnchorStuctualElement::new);
        
        return suppliers;
    }
    
    public PdfBoxAccessibilityHelper(PdfBoxFastOutputDevice od, Box root, Document doc) {
        this._od = od;
        this._rootBox = root;
        this._doc = doc;
        this._root = new GenericStructualElement();
        this._root.box = root;
        root.setAccessiblityObject(this._root);
    }
    
    private static class AbstractTreeItem {
        AbstractStructualElement parent;
    }

    private static abstract class AbstractStructualElement extends AbstractTreeItem {
        Box box;
        PDStructureElement elem;
        PDStructureElement parentElem;
        PDPage page;
        
        abstract void addChild(AbstractTreeItem child);
        abstract String getPdfTag();
        
        @Override
        public String toString() {
            return String.format("[Structual Element-%s:%s]", super.toString(), box);
        }
    }
    
    private static class GenericStructualElement extends AbstractStructualElement {
        final List<AbstractTreeItem> children = new ArrayList<>();

        @Override
        String getPdfTag() {
            return chooseTag(this.box);
        }

        @Override
        void addChild(AbstractTreeItem child) {
            this.children.add(child);
        }
    }
    
    private static class AnchorStuctualElement extends GenericStructualElement {
        String titleText;

        @Override
        String getPdfTag() {
            return StandardStructureTypes.LINK;
        }
    }
    
    private static class ListStructualElement extends AbstractStructualElement {
        final List<ListItemStructualElement> listItems = new ArrayList<>();

        @Override
        String getPdfTag() {
            return StandardStructureTypes.L;
        }

        @Override
        void addChild(AbstractTreeItem child) {
            this.listItems.add((ListItemStructualElement) child);
            
        }
    }

    private static class ListItemStructualElement extends AbstractStructualElement {
        final ListLabelStructualElement label;
        final ListBodyStructualElement body;
        
        ListItemStructualElement() {
            this.body = new ListBodyStructualElement();
            this.body.parent = this;
            
            this.label = new ListLabelStructualElement();
            this.label.parent = this;
        }
        
        @Override
        String getPdfTag() {
            return StandardStructureTypes.LI;
        }

        @Override
        void addChild(AbstractTreeItem child) {
            this.body.addChild(child);
        }
    }

    private static class ListLabelStructualElement extends AbstractStructualElement {
        final List<AbstractTreeItem> children = new ArrayList<>(1);
        
        @Override
        String getPdfTag() {
            return StandardStructureTypes.LBL;
        }

        @Override
        void addChild(AbstractTreeItem child) {
            this.children.add(child);
        }
    }
    
    private static class ListBodyStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.L_BODY;
        }
    }
    
    private static class TableStructualElement extends AbstractStructualElement {
        final TableHeadStructualElement thead = new TableHeadStructualElement();
        final List<TableBodyStructualElement> tbodies = new ArrayList<>(1);
        final TableFootStructualElement tfoot = new TableFootStructualElement();
        
        @Override
        void addChild(AbstractTreeItem child) {
            this.tbodies.add((TableBodyStructualElement) child);
        }

        @Override
        String getPdfTag() {
            return StandardStructureTypes.TABLE;
        }
    }
    
    private static class TableHeadStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.T_HEAD;
        }
    }
    
    private static class TableBodyStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.T_BODY;
        }
    }
    
    private static class TableFootStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.T_FOOT;
        }
    }
    
    private static class TableRowStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TR;
        }
    }
    
    private static class TableHeaderStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TH;
        }
    }
    
    private static class TableCellStructualElement extends GenericStructualElement {
        int rowspan = 1;
        int colspan = 1;
        
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TD;
        }
    }
    
    private static class FigureStructualElement extends AbstractStructualElement {
        String alternateText;
        PDRectangle boundingBox;
        FigureContentItem content;

        @Override
        String getPdfTag() {
            return StandardStructureTypes.Figure;
        }

        @Override
        void addChild(AbstractTreeItem child) {
            this.content = (FigureContentItem) child;
        }
    }
    
    private static class GenericContentItem extends AbstractTreeItem {
        PDStructureElement parentElem;
        int mcid;
        COSDictionary dict;
        PDPage page;
        
        @Override
        public String toString() {
            return String.format("[Content Item-%s:%d]", super.toString(), mcid);
        }
    }

    private static class FigureContentItem extends GenericContentItem {
    }
    
    /**
     * Given a box, gets its structual element.
     */
    public static PDStructureElement getStructualElementForBox(Box targetBox) {
        if (targetBox != null &&
            targetBox.getAccessibilityObject() != null &&
            targetBox.getAccessibilityObject() instanceof AbstractStructualElement) {
            
            return ((AbstractStructualElement) targetBox.getAccessibilityObject()).elem;
        }
        
        return null;
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
            _root.children.forEach(itm -> finishTreeItem(itm, _root));
            
            _od.getWriter().getDocumentCatalog().setStructureTreeRoot(root);
        }
    }
    
    public void finishNumberTree() {
        COSArray numTree = new COSArray();
        int i = 0;
        
        for (Map.Entry<PDPage, List<GenericContentItem>> entry : _pageContentItems.entrySet()) {
            List<GenericContentItem> pageItems = entry.getValue();
            List<AnnotationWithStructureParent> pageAnnotations = _pageAnnotations.get(entry.getKey());

            COSArray mcidParentReferences = new COSArray();
            pageItems.forEach(itm -> mcidParentReferences.add(itm.parentElem));
        
            numTree.add(COSInteger.get(i));
            numTree.add(mcidParentReferences);
            
            entry.getKey().getCOSObject().setItem(COSName.STRUCT_PARENTS, COSInteger.get(i));
            entry.getKey().getCOSObject().setItem(COSName.getPDFName("Tabs"), COSName.S);
            i++;
            
            for (AnnotationWithStructureParent annot : pageAnnotations) {
                numTree.add(COSInteger.get(i));
                numTree.add(annot.structureParent);
                annot.annotation.setStructParent(i);
                i++;
            }
        }
        
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.NUMS, numTree);
    
        PDNumberTreeNode numberTreeNode = new PDNumberTreeNode(dict, dict.getClass());
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTreeNextKey(i);
        _od.getWriter().getDocumentCatalog().getStructureTreeRoot().setParentTree(numberTreeNode);
    }

    private static String guessBoxTag(Box box) {
        if (box instanceof BlockBox) {
            BlockBox block = (BlockBox) box;
            
            if (block.isInline()) {
                return StandardStructureTypes.SPAN;
            } else {
                return StandardStructureTypes.DIV;
            }
        } else {
            return StandardStructureTypes.SPAN;
        }
    }
    
    /**
     * Choose a tag for a {@link GenericStructualElement}.
     */
    private static String chooseTag(Box box) {
        if (box != null) {
            if (box.getLayer() != null) {
                return StandardStructureTypes.SECT;
            } else if (box.isAnonymous()) {
                return guessBoxTag(box);
            } if (box.getElement() != null) {
                String htmlTag = box.getElement().getTagName();
                
                if (htmlTag.equals("p")) {
                    return StandardStructureTypes.P;
                } else if (htmlTag.equals("h1")) {
                    return StandardStructureTypes.H1;
                } else if (htmlTag.equals("h2")) {
                    return StandardStructureTypes.H2;
                } else if (htmlTag.equals("h3")) {
                    return StandardStructureTypes.H3;
                } else if (htmlTag.equals("h4")) {
                    return StandardStructureTypes.H4;
                } else if (htmlTag.equals("h5")) {
                    return StandardStructureTypes.H5;
                } else if (htmlTag.equals("h6")) {
                    return StandardStructureTypes.H6;
                } else if (htmlTag.equals("article")) {
                    return StandardStructureTypes.ART;
                }
            }
            
            return guessBoxTag(box);
        }
        
        return StandardStructureTypes.SPAN;
    }
    
    private void finishTreeItems(List<? extends AbstractTreeItem> children, AbstractStructualElement parent) {
        children.forEach(itm -> finishTreeItem(itm, parent));
    }
    
    private void finishTreeItem(AbstractTreeItem item, AbstractStructualElement parent) {
        if (item instanceof GenericContentItem) {
            // A content item (text or replaced image), we need to add it to its parent structual item.
            GenericContentItem child = (GenericContentItem) item;
            boolean isReplaced = child instanceof FigureContentItem;
            
            if (child.page == parent.page) { 
                // If this is on the same page as its parent structual element
                // we can just use the dict with mcid in it only.
                child.parentElem = parent.elem;
                child.parentElem.appendKid(new PDMarkedContent(isReplaced ? COSName.getPDFName("Figure") : COSName.getPDFName("Span"), child.dict));
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
        } else if (item instanceof FigureStructualElement) {
            // Must be a figure (image or replaced, etc).
            FigureStructualElement child = (FigureStructualElement) item;
            
            child.parentElem = parent.elem;
            child.elem = new PDStructureElement(child.getPdfTag(), child.parentElem);
            child.elem.setParent(child.parentElem);
            child.elem.setPage(child.page);
            
            // Add alt text.
            String alternateText = child.alternateText;
            if (alternateText.isEmpty()) {
                XRLog.general(Level.WARNING, "No alt attribute provided for image/replaced in PDF/UA document.");
            }
            child.elem.setAlternateDescription(alternateText);

            // Add bounding box attribute.
            COSDictionary attributeDict = new COSDictionary();
            attributeDict.setItem(COSName.BBOX, child.boundingBox);
            attributeDict.setItem(COSName.O, COSName.getPDFName("Layout"));
            
            Revisions<PDAttributeObject> attributes = new Revisions<>();
            attributes.addObject(PDAttributeObject.create(attributeDict), 0);
            child.elem.setAttributes(attributes);

            child.parentElem.appendKid(child.elem);
            
            finishTreeItem(child.content, child);
        } else if (item instanceof AnchorStuctualElement) {
            AnchorStuctualElement child = (AnchorStuctualElement) item;

            createPdfStrucureElement(parent, child);
            
            String alternate = child.titleText;
            if (alternate.isEmpty()) {
                XRLog.general("PDF/UA - No title text provided for link.");
            }
            child.elem.setAlternateDescription(alternate);
            
            finishTreeItems(child.children, child);
            
        } else if (item instanceof ListStructualElement) {
            ListStructualElement child = (ListStructualElement) item;
            
            createPdfStrucureElement(parent, child);
            
            finishTreeItems(child.listItems, child);
            
        } else if (item instanceof ListItemStructualElement) {
            ListItemStructualElement child = (ListItemStructualElement) item;
            
            createPdfStrucureElement(parent, child);

            finishTreeItem(child.label, child);
            finishTreeItem(child.body, child);
            
        } else if (item instanceof ListLabelStructualElement) {
            ListLabelStructualElement child = (ListLabelStructualElement) item;

            if (child.children.isEmpty()) {
                // Must be list-style-type: none.
                return;
            }
            
            createPdfStrucureElement(parent, child);

            finishTreeItems(child.children, child);

        } else if (item instanceof ListBodyStructualElement) {
            ListBodyStructualElement child = (ListBodyStructualElement) item;
            
            createPdfStrucureElement(parent, child);
            
            finishTreeItems(child.children, child);
            
        } else if (item instanceof TableStructualElement) {
            TableStructualElement child = (TableStructualElement) item;
            
            createPdfStrucureElement(parent, child);
            
            finishTreeItem(child.thead, child);
            finishTreeItems(child.tbodies, child);
            finishTreeItem(child.tfoot, child);
        } else if (item instanceof TableHeadStructualElement) {
            TableHeadStructualElement child = (TableHeadStructualElement) item;
            createPdfStrucureElement(parent, child);
            finishTreeItems(child.children, child);
        } else if (item instanceof TableBodyStructualElement) {
            TableBodyStructualElement child = (TableBodyStructualElement) item;
            createPdfStrucureElement(parent, child);
            finishTreeItems(child.children, child);
        } else if (item instanceof TableFootStructualElement) {
            TableFootStructualElement child = (TableFootStructualElement) item;
            createPdfStrucureElement(parent, child);
            finishTreeItems(child.children, child);
        } else if (item instanceof TableRowStructualElement) {
            TableRowStructualElement child = (TableRowStructualElement) item;
            createPdfStrucureElement(parent, child);
            finishTreeItems(child.children, child);
        } else if (item instanceof TableHeaderStructualElement) {
            TableHeaderStructualElement child = (TableHeaderStructualElement) item;
            createPdfStrucureElement(parent, child);
            finishTreeItems(child.children, child);
        } else if (item instanceof TableCellStructualElement) {
            TableCellStructualElement child = (TableCellStructualElement) item;
            createPdfStrucureElement(parent, child);
            
            COSDictionary colspan = null;
            COSDictionary rowspan = null;
            
            if (child.colspan != 1) {
                colspan = new COSDictionary();
                colspan.setInt(COSName.getPDFName("ColSpan"), child.colspan);
                colspan.setItem(COSName.O, COSName.getPDFName("Table"));
            }
            if (child.rowspan != 1) {
                rowspan = new COSDictionary();
                rowspan.setInt(COSName.getPDFName("RowSpan"), child.rowspan);
                rowspan.setItem(COSName.O, COSName.getPDFName("Table"));
            }
            
            if (colspan != null || rowspan != null) {
                Revisions<PDAttributeObject> attributes = new Revisions<>();
                if (colspan != null) {
                    attributes.addObject(PDAttributeObject.create(colspan), 0);
                }
                if (rowspan != null) {
                    attributes.addObject(PDAttributeObject.create(rowspan), 0);
                }
                child.elem.setAttributes(attributes);
            }
            
            finishTreeItems(child.children, child);
        } else if (item instanceof GenericStructualElement) {
            // A structual element such as Div, Sect, p, etc
            // which contains other structual elements or content items (text).
            GenericStructualElement child = (GenericStructualElement) item;
            
            if (child.children.isEmpty() &&
                (child.box.getElement() == null || !child.box.getElement().hasAttribute("id"))) {
                // There is no point in outputting empty structual elements.
                // Exception is elements with an id which may be there to
                // use as a link or bookmark destination.
                return;
            }
            
            if (child.box instanceof LineBox &&
                !child.box.hasNonTextContent(_ctx)) {
                // We skip line boxes in the tree.
                finishTreeItems(child.children, parent);
            } else {
                createPdfStrucureElement(parent, child);

                // Recursively, depth first, process the structual tree.
                finishTreeItems(child.children, child);
            }
        }
    }

    private void createPdfStrucureElement(AbstractStructualElement parent, AbstractStructualElement child) {
        child.parentElem = parent.elem;
        child.elem = new PDStructureElement(child.getPdfTag(), child.parentElem);
        child.elem.setParent(child.parentElem);
        child.elem.setPage(child.page);

        child.parentElem.appendKid(child.elem);
    }
    
    private COSDictionary createMarkedContentDictionary() {
        COSDictionary dict = new COSDictionary();
        dict.setInt(COSName.MCID, _nextMcid);
        _nextMcid++;
        return dict;
    }
    
    private void ensureAncestorTree(AbstractTreeItem child, Box parent) {
        // Walk up the ancestor tree making sure they all have accessibility objects.
        while (parent != null && parent.getAccessibilityObject() == null) {
            AbstractStructualElement parentItem = createStructureItem(null, parent);
            parent.setAccessiblityObject(parentItem);
            
            parentItem.addChild(child);
            
            child.parent = parentItem;
            child = parentItem;
            parent = parent.getParent();
        }
    }
    
    private AbstractStructualElement createStructureItem(StructureType type, Box box) {
            AbstractStructualElement child = null;
        
            if (box instanceof BlockBox) {
                BlockBox bb = (BlockBox) box;

                if (bb.isReplaced()) {
                    // For replaced elements we will need to create a BBox.
                    Rectangle2D rect = PdfBoxFastLinkManager.createTargetArea(
                            _ctx, box, _pageHeight, _transform, _rootBox, _od);
                    
                    child = new FigureStructualElement();
                    ((FigureStructualElement) child).boundingBox = new PDRectangle(
                            (float) rect.getMinX(),
                            (float) rect.getMinY(),
                            (float) rect.getWidth(),
                            (float) rect.getHeight());
                    ((FigureStructualElement) child).alternateText = box.getElement() == null ? "" : box.getElement().getAttribute("alt");
                }
            } 
            
            if (child == null && box.getElement() != null && !box.isAnonymous()) {
                String htmlTag = box.getElement().getTagName();
                Supplier<AbstractStructualElement> supplier = _tagSuppliers.get(htmlTag);
                
                if (supplier != null) {
                    child = supplier.get();
                }
            }
            
            if (child == null &&
                box.getParent() != null &&
                box.getParent().getAccessibilityObject() instanceof TableStructualElement) {
                
                TableStructualElement table = (TableStructualElement) box.getParent().getAccessibilityObject();
                
                if (box.getStyle().isIdent(CSSName.DISPLAY, IdentValue.TABLE_HEADER_GROUP)) {
                    child = table.thead;
                } else if (box.getStyle().isIdent(CSSName.DISPLAY, IdentValue.TABLE_ROW_GROUP)) {
                    child = new TableBodyStructualElement();
                } else if (box.getStyle().isIdent(CSSName.DISPLAY, IdentValue.TABLE_FOOTER_GROUP)) {
                    child = table.tfoot;
                }
            }
            
            
            if (child == null) {
                child = new GenericStructualElement();
            }
            
            child.page = _page;
            child.box = box;
        
            if (child instanceof TableCellStructualElement &&
                box instanceof TableCellBox) {
                
                TableCellBox cell = (TableCellBox) box;

                ((TableCellStructualElement) child).colspan = cell.getStyle().getColSpan();
                ((TableCellStructualElement) child).rowspan = cell.getStyle().getRowSpan();
            } else if (child instanceof AnchorStuctualElement) {
                ((AnchorStuctualElement) child).titleText = box.getElement() != null ? box.getElement().getAttribute("title") : "";
            }
            
            return child;
    }
    
    private void setupStructureElement(AbstractStructualElement child, Box box) {
        box.setAccessiblityObject(child);
        
        ensureAncestorTree(child, box.getParent());
        ensureParent(box, child);
    }

    private void ensureParent(Box box, AbstractTreeItem child) {
        if (child.parent == null) {
            if (child instanceof TableHeadStructualElement ||
                child instanceof TableFootStructualElement) {
                child.parent = (TableStructualElement) box.getParent().getAccessibilityObject();
            } else if (child instanceof TableBodyStructualElement) {
                child.parent = (TableStructualElement) box.getParent().getAccessibilityObject();
                ((TableStructualElement) child.parent).tbodies.add((TableBodyStructualElement) child);
            } else if (box.getParent() != null) {
                AbstractStructualElement parent = (AbstractStructualElement) box.getParent().getAccessibilityObject();
                parent.addChild(child);
                child.parent = parent;
            } else {
                _root.children.add(child);
                child.parent = _root;
            }
        }
    }
    
    private GenericContentItem createMarkedContentStructureItem(StructureType type, Box box) {
        GenericContentItem current = new GenericContentItem();
        
        ensureAncestorTree(current, box.getParent());
        //ensureParent(box, current);

        AbstractStructualElement parent = (AbstractStructualElement) box.getAccessibilityObject();
        parent.addChild(current);
        current.parent = parent;
        
        
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.page = _page;
        
        _contentItems.add(current);

        return current;
    }
    
    private GenericContentItem createListItemLabelMarkedContent(StructureType type, Box box) {
        GenericContentItem current = new GenericContentItem();
        
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.page = _page;

        ListItemStructualElement li = (ListItemStructualElement) box.getAccessibilityObject();
        li.label.addChild(current);
        current.parent = li.label;
        
        _contentItems.add(current);

        return current;
    }
    
    
    private FigureContentItem createFigureContentStructureItem(StructureType type, Box box) {
        FigureStructualElement parent = (FigureStructualElement) box.getAccessibilityObject();
        
        if (parent == null ||
            parent.content != null) {
            // This figure structual element already has an image associatted with it.
            // Images continued on subsequent pages will be treated as artifacts.
            return null;
        }
                
        FigureContentItem current = new FigureContentItem();
        
        ensureAncestorTree(current, box.getParent());
        
        current.parent = parent;
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.page = _page;

        parent.content = current;
        
        _contentItems.add(current);
        
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
    
    private COSDictionary createPaginationArtifact(StructureType type, Box box) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.getPDFName("Pagination"));
        return dict;
    }
    
    private static class Token {
    }
    
    private static final Token TRUE_TOKEN = new Token();
    private static final Token FALSE_TOKEN = new Token();
    private static final Token INSIDE_RUNNING = new Token();
    private static final Token STARTING_RUNNING = new Token();
    private static final Token NESTED_RUNNING = new Token();
    
    public Token startStructure(StructureType type, Box box) {
            // Check for items that appear on every page (fixed, running, page margins).    
            if (type == StructureType.RUNNING) {
                // Only mark artifact for first level of running element (we might have
                // nested fixed elements).
                if (_runningLevel == 0) {
                    _runningLevel++;
                    COSDictionary run = createPaginationArtifact(type, box);
                    _cs.beginMarkedContent(COSName.ARTIFACT, run);
                    return STARTING_RUNNING;
                }
                
                _runningLevel++;
                return NESTED_RUNNING;
            } else if (_runningLevel > 0) {
                // We are in a running artifact.
                return INSIDE_RUNNING;
            }
        
            switch (type) {
            case LAYER:
            case FLOAT:
            case BLOCK: {
                AbstractStructualElement struct = (AbstractStructualElement) box.getAccessibilityObject();
                if (struct == null) {
                    struct = createStructureItem(type, box);
                    setupStructureElement(struct, box);
                }
                return FALSE_TOKEN;
            }
            case INLINE: {
                    AbstractStructualElement struct = (AbstractStructualElement) box.getAccessibilityObject();
                    if (struct == null) {
                        struct = createStructureItem(type, box);
                        setupStructureElement(struct, box);
                    }
                return FALSE_TOKEN;
            }
            case INLINE_CHILD_BOX: {
                AbstractStructualElement struct = (AbstractStructualElement) box.getAccessibilityObject();
                if (struct == null) {
                    struct = createStructureItem(type, box);
                    setupStructureElement(struct, box);
                }
                return FALSE_TOKEN;
            }
            case BACKGROUND: {
                if (box.hasNonTextContent(_ctx)) {
                    COSDictionary current = createBackgroundArtifact(type, box);
                    _cs.beginMarkedContent(COSName.ARTIFACT, current);
                    return TRUE_TOKEN;
                }
                return FALSE_TOKEN;
            }
            case LIST_MARKER: {
                if (box instanceof BlockBox) {
                    MarkerData markers = ((BlockBox) box).getMarkerData();
                    
                    if (markers == null || 
                        (markers.getGlyphMarker() == null &&
                         markers.getTextMarker() == null &&
                         markers.getImageMarker() == null)) {
                        return FALSE_TOKEN;    
                    }
                }

                GenericContentItem current = createListItemLabelMarkedContent(type, box);
                _cs.beginMarkedContent(COSName.getPDFName("Span"), current.dict);
                return TRUE_TOKEN;
            }
            case TEXT: {
                GenericContentItem current = createMarkedContentStructureItem(type, box);
                _cs.beginMarkedContent(COSName.getPDFName(StandardStructureTypes.SPAN), current.dict);
                return TRUE_TOKEN;
            }
            case REPLACED: {
                AbstractStructualElement struct = (AbstractStructualElement) box.getAccessibilityObject();
                if (struct == null) {
                    struct = createStructureItem(type, box);
                    setupStructureElement(struct, box);
                }
                
                FigureContentItem current = createFigureContentStructureItem(type, box);
                
                if (current != null) {
                    _cs.beginMarkedContent(COSName.getPDFName(StandardStructureTypes.Figure), current.dict);
                    return TRUE_TOKEN;
                } else {
                    // For images that continue over more than one page, just mark the portion on the second
                    // and subsequent pages as an artifact. The spec (PDF 1.7) is not clear on what to do in this
                    // situation.
                    COSDictionary bg = createBackgroundArtifact(type, box);
                    _cs.beginMarkedContent(COSName.ARTIFACT, bg);
                    return TRUE_TOKEN;
                }
            }
            default: {
                return FALSE_TOKEN;
            }
            }
    }

    public void endStructure(Object token) {
        Token value = (Token) token;
        
        if (value == TRUE_TOKEN) {
            _cs.endMarkedContent();
        } else if (value == FALSE_TOKEN ||
                   value == INSIDE_RUNNING) {
            // do nothing...
        } else if (value == NESTED_RUNNING) {
            _runningLevel--;
        } else if (value == STARTING_RUNNING) {
            _runningLevel--;
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
        this._contentItems = new ArrayList<>();
        this._pageContentItems.put(page, this._contentItems);
        this._pageAnnotations.put(page, new ArrayList<>());
    }
    
    public void endPage() {
        
    }
    
    private static class AnnotationWithStructureParent {
        PDStructureElement structureParent;
        PDAnnotation annotation;
    }

    public void addLink(Box anchor, Box target, PDAnnotationLink annotation, PDPage page) {
        PDStructureElement struct = getStructualElementForBox(anchor);
        if (struct != null) {
            // We have to append the link annotationobject reference as a kid of its associated structure element.
            PDObjectReference ref = new PDObjectReference();
            ref.setReferencedObject(annotation);
            struct.appendKid(ref);  
            
            // We also need to save the pair so we can add it to the number tree for reverse lookup.
            AnnotationWithStructureParent annotStructParentPair = new AnnotationWithStructureParent();
            annotStructParentPair.annotation = annotation;
            annotStructParentPair.structureParent = struct;
            
            _pageAnnotations.get(page).add(annotStructParentPair);
        }
        
    }
}

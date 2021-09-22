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

import com.openhtmltopdf.util.LogMessageId;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
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
    // This maps from page to a list of content items, we need to process in page order, so a linked map.
    private final Map<PDPage, PageItems> _pageItemsMap = new LinkedHashMap<>();
    private final PdfBoxFastOutputDevice _od;
    private final Box _rootBox;
    private final Document _doc;
    private final GenericStructualElement _root;
    
    private static final Map<String, Supplier<AbstractStructualElement>> _tagSuppliers = createTagSuppliers();
    
    private int _nextMcid;
    
    // These change with every page, most of them are only needed while we paint the page, so we don't add
    // them to the PageItems class, which is stored for later processing.
    private PageItems _pageItems;
    private PdfContentStreamAdapter _cs;
    private RenderingContext _ctx;
    private PDPage _page;
    private float _pageHeight;
    private AffineTransform _transform;
    
    private int _runningLevel;
    
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
        suppliers.put("abbr", AbbrStuctualElement::new);
        
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
    
    private static class PageItems {
        final List<GenericContentItem> _contentItems = new ArrayList<>();
        final List<AnnotationWithStructureParent> _pageAnnotations = new ArrayList<>();
    }
    
    /**
     * Can be either a structure element or a content item.
     */
    private static abstract class AbstractTreeItem {
        AbstractStructualElement parent;
        abstract void finish(AbstractStructualElement parent);
    }

    private static abstract class AbstractStructualElement extends AbstractTreeItem {
        Box box;
        PDStructureElement elem;
        PDStructureElement parentElem; // May be different from this.parent.elem if we skip boxes in the tree.
        PDPage page;
        
        abstract void addChild(AbstractTreeItem child);
        abstract String getPdfTag();
        
        void createPdfStrucureElement(AbstractStructualElement parent, AbstractStructualElement child) {
            child.parentElem = parent.elem;
            child.elem = new PDStructureElement(child.getPdfTag(), child.parentElem);
            child.elem.setParent(child.parentElem);
            child.elem.setPage(child.page);

            child.parentElem.appendKid(child.elem);
        }
        
        /**
         * Handles globally valid HTML attributes such as title and lang.
         */
        void handleGlobalAttributes() {
            handleLangAttribute();
            handleTitleAttribute();
        }
        
        void handleLangAttribute() {
            if (box != null && box.getElement() != null) {
                String lang = box.getElement().getAttribute("lang");
                if (!lang.isEmpty()) {
                    this.elem.setLanguage(lang);
                }
            }
        }
        
        void handleTitleAttribute() {
            if (box != null && box.getElement() != null) {
                String alternate = box.getElement().getAttribute("title");
                if (!alternate.isEmpty()) {
                    this.elem.setAlternateDescription(alternate);
                }
            }
        }
        
        /**
         * Only a couple of types of structural elements need the PDF version
         * so leave empty in the base class.
         */
        void setPdfVersion(float version) { }
        
        /**
         * The optional attribute dictionary is used for additional information about
         * the structural element such as bounding box, cell spans, etc.
         * NOTE: This only allows one attribute dictionary (possibly with multiple attributes) with one owner.
         */
        void setAttributeDictionary(COSDictionary attrDict) {
            // NOTE: We could do this instead:
            //   this.elem.addAttribute(PDAttributeObject.create(attrDict));
            // However, PDFBOX adds a revision number.
            // This is allowed by the spec but causes the PDF Accessiblity Checker (PAC)
            // to crash.
            this.elem.getCOSObject().setItem(COSName.A, attrDict);
        }
        
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

        private String chooseTag(Box box) {
            if (box != null) {
                if (box.getLayer() != null) {
                    return StandardStructureTypes.SECT;
                } else if (box.isAnonymous()) {
                    return guessBoxTag(box);
                } else if (box.getElement() != null) {
                    String htmlTag = box.getElement().getTagName();

                    switch (htmlTag) {
                    case "p":
                        return StandardStructureTypes.P;
                    case "h1":
                        return StandardStructureTypes.H1;
                    case "h2":
                        return StandardStructureTypes.H2;
                    case "h3":
                        return StandardStructureTypes.H3;
                    case "h4":
                        return StandardStructureTypes.H4;
                    case "h5":
                        return StandardStructureTypes.H5;
                    case "h6":
                        return StandardStructureTypes.H6;
                    case "article": // Fall-thru
                    case "art":
                        return StandardStructureTypes.ART;
                    case "part":
                        return StandardStructureTypes.PART;
                    case "section": // Fall-thru
                    case "sect":
                        return StandardStructureTypes.SECT;
                    case "caption":
                        return StandardStructureTypes.CAPTION;
                    case "blockquote":
                        return StandardStructureTypes.BLOCK_QUOTE;
                    }
                }

                return guessBoxTag(box);
            }

            return StandardStructureTypes.SPAN;
        }

        @Override
        void finish(AbstractStructualElement parent) {
            // A structual element such as Div, Sect, p, etc
            // which contains other structual elements or content items (text).
            GenericStructualElement child = this;
            
            if (child.children.isEmpty() &&
                (child.box.getElement() == null || !child.box.getElement().hasAttribute("id"))) {
                // There is no point in outputting empty structual elements.
                // Exception is elements with an id which may be there to
                // use as a link or bookmark destination.
                return;
            }
            
            if (child.box instanceof LineBox ||
                (child.box instanceof InlineLayoutBox &&
                 child.children.size() == 1 &&
                 child.box.getParent() instanceof LineBox)) {
                // We skip (don't create structure element) line boxes in the tree.
                // We also skip the common case of a intermediary InlineLayoutBox between the 
                // LineBox and a single InlineText.
                finishTreeItems(child.children, parent);
            } else {
                createPdfStrucureElement(parent, child);
                
                handleGlobalAttributes();

                // Recursively, depth first, process the structual tree.
                finishTreeItems(child.children, child);
            }
        }
    }
    
    private static class AnchorStuctualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.LINK;
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            AnchorStuctualElement child = this;

            createPdfStrucureElement(parent, child);
            
            String alternate = box.getElement() != null ? box.getElement().getAttribute("title") : "";
            if (alternate.isEmpty()) {
                XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.GENERAL_PDF_ACCESSIBILITY_NO_TITLE_TEXT_PROVIDED_FOR, "link");
            }
            child.elem.setAlternateDescription(alternate);
            
            handleLangAttribute();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class AbbrStuctualElement extends GenericStructualElement {
        float pdfVersion = 1.5f;
        
        @Override
        String getPdfTag() {
            return StandardStructureTypes.SPAN;
        }

        @Override
        void finish(AbstractStructualElement parent) {
            AbbrStuctualElement child = this;

            createPdfStrucureElement(parent, child);

            if (box.getElement() != null) {
                String expanded = box.getElement().getAttribute("title");
                if (!expanded.isEmpty()) {
                    if (pdfVersion < 1.5f) {
                        // The expanded entry for structural elements was only introduced in 1.5.
                        child.elem.setAlternateDescription(expanded);
                    } else {
                        child.elem.setExpandedForm(expanded);
                    }
                } else {
                    XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.GENERAL_PDF_ACCESSIBILITY_NO_TITLE_TEXT_PROVIDED_FOR, "abbr tag");
                }
                
                handleLangAttribute();
            }

            finishTreeItems(child.children, child);
        }
        
        @Override
        void setPdfVersion(float version) {
            this.pdfVersion = version;
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
            if (child instanceof ListItemStructualElement) {
                this.listItems.add((ListItemStructualElement) child);    
            } else {
                logIncompatibleChild(this, child, ListItemStructualElement.class);
            }
        }

        @Override
        void finish(AbstractStructualElement parent) {
            ListStructualElement child = this;
            
            createPdfStrucureElement(parent, child);

            IdentValue listStyleType = child.box.getStyle().getIdent(CSSName.LIST_STYLE_TYPE);
            String listType;
            
            if (listStyleType == IdentValue.NONE) {
                listType = "None";
            } else if (listStyleType == IdentValue.DISC) {
                listType = "Disc";
            } else if (listStyleType == IdentValue.SQUARE) {
                listType = "Square";
            } else if (listStyleType == IdentValue.CIRCLE) {
                listType = "Circle";
            } else if (listStyleType == IdentValue.DECIMAL ||
                       listStyleType == IdentValue.DECIMAL_LEADING_ZERO) {
                listType = "Decimal";
            } else if (listStyleType == IdentValue.UPPER_ROMAN) {
                listType = "UpperRoman";
            } else if (listStyleType == IdentValue.LOWER_ROMAN) {
                listType = "LowerRoman";
            } else if (listStyleType == IdentValue.UPPER_ALPHA) {
                listType = "UpperAlpha";
            } else if (listStyleType == IdentValue.LOWER_ALPHA) {
                listType = "LowerAlpha";
            } else {
                // Armenian, Georgian, Latin and Greek are not supported by the PDF spec.
                listType = "Decimal";
            }
            
            COSDictionary listNumbering = new COSDictionary();
            listNumbering.setItem(COSName.O, COSName.getPDFName("List"));
            listNumbering.setItem(COSName.getPDFName("ListNumbering"), COSName.getPDFName(listType));
            setAttributeDictionary(listNumbering);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.listItems, child);
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

        @Override
        void finish(AbstractStructualElement parent) {
            ListItemStructualElement child = this;
            
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();

            finishTreeItem(child.label, child);
            finishTreeItem(child.body, child);
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

        @Override
        void finish(AbstractStructualElement parent) {
            ListLabelStructualElement child = this;

            if (child.children.isEmpty()) {
                // Must be list-style-type: none.
                return;
            }
            
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();

            finishTreeItems(child.children, child);
        }
    }
    
    private static class ListBodyStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.L_BODY;
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            ListBodyStructualElement child = this;
            
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class TableStructualElement extends AbstractStructualElement {
        final TableHeadStructualElement thead = new TableHeadStructualElement();
        final List<TableBodyStructualElement> tbodies = new ArrayList<>(1);
        final TableFootStructualElement tfoot = new TableFootStructualElement();
        float pdfVersion = 1.5f;
        
        @Override
        void addChild(AbstractTreeItem child) {
            if (child instanceof TableBodyStructualElement) {
                this.tbodies.add((TableBodyStructualElement) child);
            } else {
                logIncompatibleChild(parent, child, TableBodyStructualElement.class);
            }
        }

        @Override
        void setPdfVersion(float version) {
            this.pdfVersion = version;
        }
        
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TABLE;
        }

        @Override
        void finish(AbstractStructualElement parent) {
            TableStructualElement child = this;
            
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();
            
            if (pdfVersion < 1.5f) {
                // THead, TBody and TFoot were introduced in PDF 1.5 so if we can not use
                // them then we process their rows directly and add them to the table.
                finishTreeItems(child.thead.children, child);
                child.tbodies.forEach(tbody -> finishTreeItems(tbody.children, child));
                finishTreeItems(child.tfoot.children, child);
            } else {
                finishTreeItem(child.thead, child);
                finishTreeItems(child.tbodies, child);
                finishTreeItem(child.tfoot, child);
            }
        }
    }
    
    private static class TableHeadStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.T_HEAD;
        }
        
        @Override
        void addChild(AbstractTreeItem child) {
            if (!(child instanceof TableRowStructualElement)) {
                logIncompatibleChild(this, child, TableRowStructualElement.class);
            }
            super.addChild(child);
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            TableHeadStructualElement child = this;
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class TableBodyStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.T_BODY;
        }
        
        @Override
        void addChild(AbstractTreeItem child) {
            if (!(child instanceof TableRowStructualElement)) {
                logIncompatibleChild(this, child, TableRowStructualElement.class);
            }
            super.addChild(child);
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            TableBodyStructualElement child = this;
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class TableFootStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.T_FOOT;
        }
        
        @Override
        void addChild(AbstractTreeItem child) {
            if (!(child instanceof TableRowStructualElement)) {
                logIncompatibleChild(this, child, TableRowStructualElement.class);
            }
            super.addChild(child);
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            TableFootStructualElement child = this;
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class TableRowStructualElement extends GenericStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TR;
        }
        
        @Override
        void addChild(AbstractTreeItem child) {
            if (!(child instanceof TableHeaderOrCellStructualElement)) {
                logIncompatibleChild(this, child, TableHeaderOrCellStructualElement.class);
            }
            super.addChild(child);
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            TableRowStructualElement child = this;
            createPdfStrucureElement(parent, child);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static abstract class TableHeaderOrCellStructualElement extends GenericStructualElement {
        boolean addCellAttributes(COSDictionary attrDict) {
            TableHeaderOrCellStructualElement child = this;
            
            boolean added = false;
            int rowSpanAttr = 1;
            int colSpanAttr = 1;
            
            if (child.box instanceof TableCellBox) {
                TableCellBox cell = (TableCellBox) box;

                colSpanAttr = cell.getStyle().getColSpan();
                rowSpanAttr = cell.getStyle().getRowSpan();
            }
            
            if (colSpanAttr != 1) {
                added = true;
                attrDict.setInt(COSName.getPDFName("ColSpan"), colSpanAttr);
            }
            
            if (rowSpanAttr != 1) {
                added = true;
                attrDict.setInt(COSName.getPDFName("RowSpan"), rowSpanAttr);
            }
            
            return added;
        }
    }
    
    private static class TableHeaderStructualElement extends TableHeaderOrCellStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TH;
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            TableHeaderStructualElement child = this;
            createPdfStrucureElement(parent, child);
            
            String scope = box.getElement() != null ? box.getElement().getAttribute("scope") : "";
            
            COSDictionary attrDict = new COSDictionary();
            attrDict.setItem(COSName.O, COSName.getPDFName("Table"));
            
            if ("row".equals(scope)) {
                attrDict.setItem(COSName.getPDFName("Scope"), COSName.getPDFName("Row"));
            } else {
                attrDict.setItem(COSName.getPDFName("Scope"), COSName.getPDFName("Column"));
            }
            
            addCellAttributes(attrDict);
            setAttributeDictionary(attrDict);
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class TableCellStructualElement extends TableHeaderOrCellStructualElement {
        @Override
        String getPdfTag() {
            return StandardStructureTypes.TD;
        }
        
        @Override
        void finish(AbstractStructualElement parent) {
            TableCellStructualElement child = this;
            createPdfStrucureElement(parent, child);
            
            COSDictionary attrDict = new COSDictionary();
            attrDict.setItem(COSName.O, COSName.getPDFName("Table"));
            
            if (addCellAttributes(attrDict)) {
                setAttributeDictionary(attrDict);
            }
            
            handleGlobalAttributes();
            
            finishTreeItems(child.children, child);
        }
    }
    
    private static class FigureStructualElement extends AbstractStructualElement {
        PDRectangle boundingBox;
        FigureContentItem content;

        @Override
        String getPdfTag() {
            return StandardStructureTypes.Figure;
        }

        @Override
        void addChild(AbstractTreeItem child) {
            if (child instanceof FigureContentItem) {
                this.content = (FigureContentItem) child;
            } else {
                logIncompatibleChild(parent, child, FigureContentItem.class);
            }
        }

        @Override
        void finish(AbstractStructualElement parent) {
            // Must be a figure (image or replaced, etc).
            FigureStructualElement child = this;
            
            child.parentElem = parent.elem;
            child.elem = new PDStructureElement(child.getPdfTag(), child.parentElem);
            child.elem.setParent(child.parentElem);
            child.elem.setPage(child.page);
            
            // Add alt text.
            String alternateText = child.box.getElement() == null ? "" : box.getElement().getAttribute("alt");
            if (alternateText.isEmpty()) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.GENERAL_PDF_ACCESSIBILITY_NO_ALT_ATTRIBUTE_PROVIDED_FOR_IMAGE);
            }
            child.elem.setAlternateDescription(alternateText);
            
            handleLangAttribute();

            // Add bounding box attribute.
            COSDictionary attributeDict = new COSDictionary();
            attributeDict.setItem(COSName.BBOX, child.boundingBox);
            attributeDict.setItem(COSName.O, COSName.getPDFName("Layout"));
            setAttributeDictionary(attributeDict);

            child.parentElem.appendKid(child.elem);
            
            finishTreeItem(child.content, child);
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

        @Override
        void finish(AbstractStructualElement parent) {
            // A content item (text or replaced image), we need to add it to its parent structual item.
            GenericContentItem child = this;
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
        }
    }

    private static class FigureContentItem extends GenericContentItem {
    }
    
    private static void logIncompatibleChild(AbstractTreeItem parent, AbstractTreeItem child, Class<?> expected) {
        XRLog.log(Level.WARNING, LogMessageId.LogMessageId3Param.GENERAL_PDF_ACCESSIBILITY_INCOMPATIBLE_CHILD, child.getClass().getSimpleName(), parent.getClass().getSimpleName(), expected.getSimpleName());
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
            finishTreeItems(_root.children, _root);
            
            _od.getWriter().getDocumentCatalog().setStructureTreeRoot(root);
        }
    }
    
    public void finishNumberTree() {
        COSArray numTree = new COSArray();
        int i = 0;
        
        for (Map.Entry<PDPage, PageItems> entry : _pageItemsMap.entrySet()) {
            List<GenericContentItem> pageItems = entry.getValue()._contentItems;
            List<AnnotationWithStructureParent> pageAnnotations = entry.getValue()._pageAnnotations;

            COSArray mcidParentReferences = new COSArray();
            
            for (GenericContentItem contentItem : pageItems) {
                mcidParentReferences.add(contentItem.parentElem);
            }
        
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
    
    private static void finishTreeItems(List<? extends AbstractTreeItem> children, AbstractStructualElement parent) {
        for (AbstractTreeItem child : children) {
            child.finish(parent);
        }
    }
    
    private static void finishTreeItem(AbstractTreeItem item, AbstractStructualElement parent) {
        item.finish(parent);
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
                    // This is done here so we don'thave to hang onto the page height, transform, etc.
                    Rectangle2D rect = PdfBoxFastLinkManager.createTargetArea(
                            _ctx, box, _pageHeight, _transform, _rootBox, _od);
                    
                    child = new FigureStructualElement();
                    ((FigureStructualElement) child).boundingBox = new PDRectangle(
                            (float) rect.getMinX(),
                            (float) rect.getMinY(),
                            (float) rect.getWidth(),
                            (float) rect.getHeight());
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
                
                table.setPdfVersion(_od.getWriter().getVersion());
                
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
            child.setPdfVersion(_od.getWriter().getVersion());
            
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

        AbstractStructualElement parent = (AbstractStructualElement) box.getAccessibilityObject();
        parent.addChild(current);

        current.parent = parent;
        current.mcid = _nextMcid;
        current.dict = createMarkedContentDictionary();
        current.page = _page;
        
        _pageItems._contentItems.add(current);

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
        
        _pageItems._contentItems.add(current);

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
        
        _pageItems._contentItems.add(current);
        
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
            case BLOCK:
            case INLINE:
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
        this._pageItems = new PageItems();
        this._pageItemsMap.put(page, this._pageItems);
    }
    
    public void endPage() {
        
    }
    
    private static class AnnotationWithStructureParent {
        PDStructureElement structureParent;
        PDAnnotation annotation;
    }

    public void addLink(Box anchor, Box target, PDAnnotation pdAnnotation, PDPage page) {
        PDStructureElement struct = getStructualElementForBox(anchor);
        if (struct != null) {
            // We have to append the link annotationobject reference as a kid of its associated structure element.
            PDObjectReference ref = new PDObjectReference();
            ref.setReferencedObject(pdAnnotation);
            struct.appendKid(ref);  
            
            // We also need to save the pair so we can add it to the number tree for reverse lookup.
            AnnotationWithStructureParent annotStructParentPair = new AnnotationWithStructureParent();
            annotStructParentPair.annotation = pdAnnotation;
            annotStructParentPair.structureParent = struct;
            
            _pageItems._pageAnnotations.add(annotStructParentPair);
        }
    }
}

/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Torbjoern Gannholm, Joshua Marinacci
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.layout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiTextRun;
import com.openhtmltopdf.bidi.ParagraphSplitter.Paragraph;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.constants.MarginBoxName;
import com.openhtmltopdf.css.constants.PageElementPosition;
import com.openhtmltopdf.css.extend.ContentFunction;
import com.openhtmltopdf.css.newmatch.CascadedStyle;
import com.openhtmltopdf.css.newmatch.PageInfo;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.FSFunction;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.EmptyStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.layout.counter.AbstractCounterContext;
import com.openhtmltopdf.layout.counter.RootCounterContext;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.newtable.TableCellBox;
import com.openhtmltopdf.newtable.TableColumn;
import com.openhtmltopdf.newtable.TableRowBox;
import com.openhtmltopdf.newtable.TableSectionBox;
import com.openhtmltopdf.render.AnonymousBlockBox;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FloatedBoxData;
import com.openhtmltopdf.render.FlowingColumnBox;
import com.openhtmltopdf.render.FlowingColumnContainerBox;
import com.openhtmltopdf.render.InlineBox;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.LogMessageId;

/**
 * This class is responsible for creating the box tree from the DOM.  This is
 * mostly just a one-to-one translation from the <code>Element</code> to an
 * <code>InlineBox</code> or a <code>BlockBox</code> (or some subclass of
 * <code>BlockBox</code>), but the tree is reorganized according to the CSS rules.
 * This includes inserting anonymous block and inline boxes, anonymous table
 * content, and <code>:before</code> and <code>:after</code> content.  White
 * space is also normalized at this point.  Table columns and table column groups
 * are added to the table which owns them, but are not created as regular boxes.
 * Floated and absolutely positioned content is always treated as inline
 * content for purposes of inserting anonymous block boxes and calculating
 * the kind of content contained in a given block box.
 */
public class BoxBuilder {
    public static final int MARGIN_BOX_VERTICAL = 1;
    public static final int MARGIN_BOX_HORIZONTAL = 2;

    private static final int CONTENT_LIST_DOCUMENT = 1;
    private static final int CONTENT_LIST_MARGIN_BOX = 2;

    /**
     * Split the document into paragraphs for use in analyzing bi-directional text runs.
     * @param c
     * @param document
     */
    private static void splitParagraphs(LayoutContext c, Document document) {
        c.getParagraphSplitter().splitRoot(c, document);
        c.getParagraphSplitter().runBidiOnParagraphs(c);
    }

    public static BlockBox createRootBox(LayoutContext c, Document document) {
        splitParagraphs(c, document);

        Element root = document.getDocumentElement();

        CalculatedStyle style = c.getSharedContext().getStyle(root);

        BlockBox result;
        if (style.isTable() || style.isInlineTable()) {
            result = new TableBox();
        } else {
            result = new BlockBox();
        }

        result.setStyle(style);
        result.setElement(root);

        c.addLayoutBoxId(root, result);
        c.resolveCounters(style);

        return result;
    }

    public static void createChildren(LayoutContext c, BlockBox parent) {
        if (parent.shouldBeReplaced()) {
            // Don't create boxes for elements in a SVG element.
            // This avoids many warnings and improves performance.
            parent.setChildrenContentType(BlockBox.ContentType.EMPTY);
            return;
        } else if (isInsertedBoxIgnored(parent.getElement())) {
            return;
        }

        List<Styleable> children = new ArrayList<>();
        ChildBoxInfo info = new ChildBoxInfo();
        CalculatedStyle parentStyle = parent.getStyle();

        boolean oldAllowFootnotes = c.isFootnoteAllowed();
        if (parentStyle.isFixed() || parentStyle.isRunning()) {
            c.setFootnoteAllowed(false);
        }

        createChildren(c, parent, parent.getElement(), children, info, false);

        boolean parentIsNestingTableContent = isNestingTableContent(parentStyle.getIdent(
                CSSName.DISPLAY));

        if (!parentIsNestingTableContent && !info.isContainsTableContent()) {
            resolveChildren(c, parent, children, info);
        } else {
            stripAllWhitespace(children);
            if (parentIsNestingTableContent) {
                resolveTableContent(c, parent, children, info);
            } else {
                resolveChildTableContent(c, parent, children, info, IdentValue.TABLE_CELL);
            }
        }

        c.setFootnoteAllowed(oldAllowFootnotes);

        // The following is very useful for debugging.
        // It shows the contents of the box tree before layout.
//        if (parent == c.getRootLayer().getMaster()) {
//            System.out.println(com.openhtmltopdf.util.LambdaUtil.descendantDump(parent));
//        }
    }

    private static boolean isInsertedBoxIgnored(Element element) {
        if (element == null) {
            return false;
        }

        String tag = element.getTagName();

        if (!tag.startsWith("fs-")) {
            return false;
        }

        switch (tag) {
        case "fs-footnote":
        case "fs-footnote-body":
            return true;
        default:
            return false;
        }
    }

    public static TableBox createMarginTable(
            LayoutContext c,
            PageInfo pageInfo,
            MarginBoxName[] names,
            int height,
            int direction)
    {
        if (! pageInfo.hasAny(names)) {
            return null;
        }

        Element source = c.getRootLayer().getMaster().getElement(); // HACK

        ChildBoxInfo info = new ChildBoxInfo();
        CalculatedStyle pageStyle = new EmptyStyle().deriveStyle(pageInfo.getPageStyle());

        CalculatedStyle tableStyle = pageStyle.deriveStyle(
                CascadedStyle.createLayoutStyle(new PropertyDeclaration[] {
                        new PropertyDeclaration(
                                CSSName.DISPLAY,
                                new PropertyValue(IdentValue.TABLE),
                                true,
                                StylesheetInfo.USER),
                        new PropertyDeclaration(
                                CSSName.WIDTH,
                                new PropertyValue(CSSPrimitiveValue.CSS_PERCENTAGE, 100.0f, "100%"),
                                true,
                                StylesheetInfo.USER),
                }));
        TableBox result = (TableBox)createBlockBox(tableStyle, info, false);
        result.setMarginAreaRoot(true);
        result.setStyle(tableStyle);
        result.setElement(source);
        result.setAnonymous(true);
        result.setChildrenContentType(BlockBox.ContentType.BLOCK);

        CalculatedStyle tableSectionStyle = pageStyle.createAnonymousStyle(IdentValue.TABLE_ROW_GROUP);
        TableSectionBox section = (TableSectionBox)createBlockBox(tableSectionStyle, info, false);
        section.setStyle(tableSectionStyle);
        section.setElement(source);
        section.setAnonymous(true);
        section.setChildrenContentType(BlockBox.ContentType.BLOCK);

        result.addChild(section);

        TableRowBox row = null;
        if (direction == MARGIN_BOX_HORIZONTAL) {
            CalculatedStyle tableRowStyle = pageStyle.createAnonymousStyle(IdentValue.TABLE_ROW);
            row = (TableRowBox)createBlockBox(tableRowStyle, info, false);
            row.setStyle(tableRowStyle);
            row.setElement(source);
            row.setAnonymous(true);
            row.setChildrenContentType(BlockBox.ContentType.BLOCK);

            row.setHeightOverride(height);

            section.addChild(row);
        }

        int cellCount = 0;
        boolean alwaysCreate = names.length > 1 && direction == MARGIN_BOX_HORIZONTAL;

        for (int i = 0; i < names.length; i++) {
            CascadedStyle cellStyle = pageInfo.createMarginBoxStyle(names[i], alwaysCreate);
            if (cellStyle != null) {
                TableCellBox cell = createMarginBox(c, cellStyle, alwaysCreate);
                if (cell != null) {
                    if (direction == MARGIN_BOX_VERTICAL) {
                        CalculatedStyle tableRowStyle = pageStyle.createAnonymousStyle(IdentValue.TABLE_ROW);
                        row = (TableRowBox)createBlockBox(tableRowStyle, info, false);
                        row.setStyle(tableRowStyle);
                        row.setElement(source);
                        row.setAnonymous(true);
                        row.setChildrenContentType(BlockBox.ContentType.BLOCK);

                        row.setHeightOverride(height);

                        section.addChild(row);
                    }
                    row.addChild(cell);
                    cellCount++;
                }
            }
        }

        if (direction == MARGIN_BOX_VERTICAL && cellCount > 0) {
            int rHeight = 0;
            for (Iterator<Box> i = section.getChildIterator(); i.hasNext(); ) {
                TableRowBox r = (TableRowBox)i.next();
                r.setHeightOverride(height / cellCount);
                rHeight += r.getHeightOverride();
            }

            for (Iterator<Box> i = section.getChildIterator(); i.hasNext() && rHeight < height; ) {
                TableRowBox r = (TableRowBox)i.next();
                r.setHeightOverride(r.getHeightOverride()+1);
                rHeight++;
            }
        }

        return cellCount > 0 ? result : null;
    }

    private static TableCellBox createMarginBox(
            LayoutContext c,
            CascadedStyle cascadedStyle,
            boolean alwaysCreate) {
        boolean hasContent = true;

        PropertyDeclaration contentDecl = cascadedStyle.propertyByName(CSSName.CONTENT);

        CalculatedStyle style = new EmptyStyle().deriveStyle(cascadedStyle);

        if (style.isDisplayNone() && ! alwaysCreate) {
            return null;
        }

        if (style.isIdent(CSSName.CONTENT, IdentValue.NONE) ||
                style.isIdent(CSSName.CONTENT, IdentValue.NORMAL)) {
            hasContent = false;
        }

        if (style.isAutoWidth() && ! alwaysCreate && ! hasContent) {
            return null;
        }

        List<Styleable> children = new ArrayList<>();

        ChildBoxInfo info = new ChildBoxInfo();
        info.setContainsTableContent(true);
        info.setLayoutRunningBlocks(true);

        TableCellBox result = new TableCellBox();
        result.setAnonymous(true);
        result.setStyle(style);
        result.setElement(c.getRootLayer().getMaster().getElement()); // XXX Doesn't make sense, but we need something here

        if (hasContent && ! style.isDisplayNone()) {
            children.addAll(createGeneratedMarginBoxContent(
                    c,
                    c.getRootLayer().getMaster().getElement(),
                    (PropertyValue)contentDecl.getValue(),
                    style,
                    info));

            stripAllWhitespace(children);
        }

        resolveChildTableContent(c, result, children, info, IdentValue.TABLE_CELL);

        return result;
    }

    private static void resolveChildren(
            LayoutContext c, BlockBox owner, List<Styleable> children, ChildBoxInfo info) {
        if (children.size() > 0) {
            if (info.isContainsBlockLevelContent()) {
                insertAnonymousBlocks(
                        c.getSharedContext(), owner, children, info.isLayoutRunningBlocks());
                owner.setChildrenContentType(BlockBox.ContentType.BLOCK);
            } else {
                WhitespaceStripper.stripInlineContent(children);
                if (children.size() > 0) {
                    owner.setInlineContent(children);
                    owner.setChildrenContentType(BlockBox.ContentType.INLINE);
                } else {
                    owner.setChildrenContentType(BlockBox.ContentType.EMPTY);
                }
            }
        } else {
            owner.setChildrenContentType(BlockBox.ContentType.EMPTY);
        }
    }

    private static boolean isAllProperTableNesting(IdentValue parentDisplay, List<Styleable> children) {
        return children.stream().allMatch(child -> isProperTableNesting(parentDisplay, child.getStyle().getIdent(CSSName.DISPLAY)));
    }

    /**
     * Handles the situation when we find table content, but our parent is not
     * table related.  For example, <code>div</code> -> <code>td</td></code>.
     * Anonymous tables are then constructed by repeatedly pulling together
     * consecutive same-table-level siblings and wrapping them in the next
     * highest table level (e.g. consecutive <code>td</code> elements will
     * be wrapped in an anonymous <code>tr</code>, then a <code>tbody</code>, and
     * finally a <code>table</code>).
     */
    private static void resolveChildTableContent(
            LayoutContext c, BlockBox parent, List<Styleable> children, ChildBoxInfo info, IdentValue target) {
        List<Styleable> childrenForAnonymous = new ArrayList<>();
        List<Styleable> childrenWithAnonymous = new ArrayList<>();

        IdentValue nextUp = getPreviousTableNestingLevel(target);
        
        for (Styleable styleable : children) {
            if (matchesTableLevel(target, styleable.getStyle().getIdent(CSSName.DISPLAY))) {
                childrenForAnonymous.add(styleable);
            } else {
                if (childrenForAnonymous.size() > 0) {
                    createAnonymousTableContent(c, (BlockBox) childrenForAnonymous.get(0), nextUp,
                            childrenForAnonymous, childrenWithAnonymous);

                    childrenForAnonymous = new ArrayList<>();
                }
                childrenWithAnonymous.add(styleable);
            }
        }

        if (childrenForAnonymous.size() > 0) {
            createAnonymousTableContent(c, (BlockBox) childrenForAnonymous.get(0), nextUp,
                    childrenForAnonymous, childrenWithAnonymous);
        }

        if (nextUp == IdentValue.TABLE) {
            rebalanceInlineContent(childrenWithAnonymous);
            info.setContainsBlockLevelContent(true);
            resolveChildren(c, parent, childrenWithAnonymous, info);
        } else {
            resolveChildTableContent(c, parent, childrenWithAnonymous, info, nextUp);
        }
    }

    private static boolean matchesTableLevel(IdentValue target, IdentValue value) {
        if (target == IdentValue.TABLE_ROW_GROUP) {
            return value == IdentValue.TABLE_ROW_GROUP || value == IdentValue.TABLE_HEADER_GROUP
                    || value == IdentValue.TABLE_FOOTER_GROUP || value == IdentValue.TABLE_CAPTION;
        } else {
            return target == value;
        }
    }

    /**
     * Makes sure that any <code>InlineBox</code> in <code>content</code>
     * both starts and ends within <code>content</code>. Used to ensure that
     * it is always possible to construct anonymous blocks once an element's
     * children has been distributed among anonymous table objects.
     */
    private static void rebalanceInlineContent(List<Styleable> content) {
        Map<Element, InlineBox> boxesByElement = new HashMap<>();
        for (Styleable styleable : content) {
            if (styleable instanceof InlineBox) {
                InlineBox iB = (InlineBox) styleable;
                Element elem = iB.getElement();

                if (!boxesByElement.containsKey(elem)) {
                    iB.setStartsHere(true);
                }

                boxesByElement.put(elem, iB);
            }
        }

        for (InlineBox iB : boxesByElement.values()) {
            iB.setEndsHere(true);
        }
    }

    private static void stripAllWhitespace(List<Styleable> content) {
        int start = 0;
        int current = 0;
        boolean started = false;
        for (current = 0; current < content.size(); current++) {
            Styleable styleable = content.get(current);
            if (! styleable.getStyle().isLayedOutInInlineContext()) {
                if (started) {
                    int before = content.size();
                    WhitespaceStripper.stripInlineContent(content.subList(start, current));
                    int after = content.size();
                    current -= (before - after);
                }
                started = false;
            } else {
                if (! started) {
                    started = true;
                    start = current;
                }
            }
        }

        if (started) {
            WhitespaceStripper.stripInlineContent(content.subList(start, current));
        }
    }

    /**
     * Handles the situation when our current parent is table related.  If
     * everything is properly nested (e.g. a <code>tr</code> contains only
     * <code>td</code> elements), nothing is done.  Otherwise anonymous boxes
     * are inserted to ensure the integrity of the table model.
     */
    private static void resolveTableContent(
            LayoutContext c, BlockBox parent, List<Styleable> children, ChildBoxInfo info) {
        IdentValue parentDisplay = parent.getStyle().getIdent(CSSName.DISPLAY);
        IdentValue next = getNextTableNestingLevel(parentDisplay);
        if (next == null && parent.isAnonymous() && containsOrphanedTableContent(children)) {
            resolveChildTableContent(c, parent, children, info, IdentValue.TABLE_CELL);
        } else if (next == null || isAllProperTableNesting(parentDisplay, children)) {
            if (parent.isAnonymous()) {
                rebalanceInlineContent(children);
            }
            resolveChildren(c, parent, children, info);
        } else {
            List<Styleable> childrenForAnonymous = new ArrayList<>();
            List<Styleable> childrenWithAnonymous = new ArrayList<>();
            
            for (Styleable child : children) {
                IdentValue childDisplay = child.getStyle().getIdent(CSSName.DISPLAY);

                if (isProperTableNesting(parentDisplay, childDisplay)) {
                    if (childrenForAnonymous.size() > 0) {
                        createAnonymousTableContent(c, parent, next, childrenForAnonymous,
                                childrenWithAnonymous);

                        childrenForAnonymous = new ArrayList<>();
                    }
                    childrenWithAnonymous.add(child);
                } else {
                    childrenForAnonymous.add(child);
                }
            }

            if (childrenForAnonymous.size() > 0) {
                createAnonymousTableContent(c, parent, next, childrenForAnonymous,
                        childrenWithAnonymous);
            }

            info.setContainsBlockLevelContent(true);
            resolveChildren(c, parent, childrenWithAnonymous, info);
        }
    }
    
    private static boolean isTableRowOrRowGroup(Styleable child) {
        IdentValue display = child.getStyle().getIdent(CSSName.DISPLAY);
        return (display == IdentValue.TABLE_HEADER_GROUP ||
                display == IdentValue.TABLE_ROW_GROUP ||
                display == IdentValue.TABLE_FOOTER_GROUP ||
                display == IdentValue.TABLE_ROW);
    }

    private static boolean containsOrphanedTableContent(List<Styleable> children) {
        return children.stream().anyMatch(BoxBuilder::isTableRowOrRowGroup);
    }

    private static boolean isParentInline(BlockBox box) {
        CalculatedStyle parentStyle = box.getStyle().getParent();
        return parentStyle != null && parentStyle.isInline();
    }

    private static void createAnonymousTableContent(LayoutContext c, BlockBox source,
                                                    IdentValue next, List<Styleable> childrenForAnonymous, List<Styleable> childrenWithAnonymous) {
        ChildBoxInfo nested = lookForBlockContent(childrenForAnonymous);
        IdentValue anonDisplay;
        if (isParentInline(source) && next == IdentValue.TABLE) {
            anonDisplay = IdentValue.INLINE_TABLE;
        } else {
            anonDisplay = next;
        }
        CalculatedStyle anonStyle = source.getStyle().createAnonymousStyle(anonDisplay);
        BlockBox anonBox = createBlockBox(anonStyle, nested, false);
        anonBox.setStyle(anonStyle);
        anonBox.setAnonymous(true);
        // XXX Doesn't really make sense, but what to do?
        anonBox.setElement(source.getElement());
        resolveTableContent(c, anonBox, childrenForAnonymous, nested);

        if (next == IdentValue.TABLE) {
            childrenWithAnonymous.add(reorderTableContent(c, (TableBox) anonBox));
        } else {
            childrenWithAnonymous.add(anonBox);
        }
    }

    /**
     * Reorganizes a table so that the header is the first row group and the
     * footer the last.  If the table has caption boxes, they will be pulled
     * out and added to an anonymous block box along with the table itself.
     * If not, the table is returned.
     */
    private static BlockBox reorderTableContent(LayoutContext c, TableBox table) {
        List<Box> topCaptions = new ArrayList<>();
        Box header = null;
        List<Box> bodies = new ArrayList<>();
        Box footer = null;
        List<Box> bottomCaptions = new ArrayList<>();

        for (Box b : table.getChildren()) {
            IdentValue display = b.getStyle().getIdent(CSSName.DISPLAY);
            
            if (display == IdentValue.TABLE_CAPTION) {
                IdentValue side = b.getStyle().getIdent(CSSName.CAPTION_SIDE);
                if (side == IdentValue.BOTTOM) {
                    bottomCaptions.add(b);
                } else { /* side == IdentValue.TOP */
                    topCaptions.add(b);
                }
            } else if (display == IdentValue.TABLE_HEADER_GROUP && header == null) {
                header = b;
            } else if (display == IdentValue.TABLE_FOOTER_GROUP && footer == null) {
                footer = b;
            } else {
                bodies.add(b);
            }
        }

        table.removeAllChildren();
        if (header != null) {
            ((TableSectionBox)header).setHeader(true);
            table.addChild(header);
        }
        table.addAllChildren(bodies);
        if (footer != null) {
            ((TableSectionBox)footer).setFooter(true);
            table.addChild(footer);
        }

        if (topCaptions.size() == 0 && bottomCaptions.size() == 0) {
            return table;
        } else {
            // If we have a floated table with a caption, we need to float the
            // outer anonymous box and not the table
            CalculatedStyle anonStyle;
            if (table.getStyle().isFloated()) {
                CascadedStyle cascadedStyle = CascadedStyle.createLayoutStyle(
                        new PropertyDeclaration[]{
                                CascadedStyle.createLayoutPropertyDeclaration(
                                        CSSName.DISPLAY, IdentValue.BLOCK),
                                CascadedStyle.createLayoutPropertyDeclaration(
                                        CSSName.FLOAT, table.getStyle().getIdent(CSSName.FLOAT))});

                anonStyle = table.getStyle().deriveStyle(cascadedStyle);
            } else {
                anonStyle = table.getStyle().createAnonymousStyle(IdentValue.BLOCK);
            }

            BlockBox anonBox = new BlockBox();
            anonBox.setStyle(anonStyle);
            anonBox.setAnonymous(true);
            anonBox.setFromCaptionedTable(true);
            anonBox.setElement(table.getElement());

            anonBox.setChildrenContentType(BlockBox.ContentType.BLOCK);
            anonBox.addAllChildren(topCaptions);
            anonBox.addChild(table);
            anonBox.addAllChildren(bottomCaptions);

            if (table.getStyle().isFloated()) {
                anonBox.setFloatedBoxData(new FloatedBoxData());
                table.setFloatedBoxData(null);

                CascadedStyle original = c.getSharedContext().getCss().getCascadedStyle(
                        table.getElement(), false);
                CascadedStyle modified = CascadedStyle.createLayoutStyle(
                        original,
                        new PropertyDeclaration[]{
                                CascadedStyle.createLayoutPropertyDeclaration(
                                        CSSName.FLOAT, IdentValue.NONE)
                        });
                table.setStyle(table.getStyle().getParent().deriveStyle(modified));
            }

            return anonBox;
        }
    }

    private static ChildBoxInfo lookForBlockContent(List<Styleable> styleables) {
        ChildBoxInfo result = new ChildBoxInfo();
        
        if (styleables.stream().anyMatch(s -> !s.getStyle().isLayedOutInInlineContext())) {
            result.setContainsBlockLevelContent(true);
        }
        
        return result;
    }

    private static IdentValue getNextTableNestingLevel(IdentValue display) {
        if (display == IdentValue.TABLE || display == IdentValue.INLINE_TABLE) {
            return IdentValue.TABLE_ROW_GROUP;
        } else if (display == IdentValue.TABLE_HEADER_GROUP
                || display == IdentValue.TABLE_ROW_GROUP
                || display == IdentValue.TABLE_FOOTER_GROUP) {
            return IdentValue.TABLE_ROW;
        } else if (display == IdentValue.TABLE_ROW) {
            return IdentValue.TABLE_CELL;
        } else {
            return null;
        }
    }

    private static IdentValue getPreviousTableNestingLevel(IdentValue display) {
        if (display == IdentValue.TABLE_CELL) {
            return IdentValue.TABLE_ROW;
        } else if (display == IdentValue.TABLE_ROW) {
            return IdentValue.TABLE_ROW_GROUP;
        } else if (display == IdentValue.TABLE_HEADER_GROUP
                || display == IdentValue.TABLE_ROW_GROUP
                || display == IdentValue.TABLE_FOOTER_GROUP) {
            return IdentValue.TABLE;
        } else {
            return null;
        }
    }

    private static boolean isProperTableNesting(IdentValue parent, IdentValue child) {
        return (parent == IdentValue.TABLE && (child == IdentValue.TABLE_HEADER_GROUP ||
                child == IdentValue.TABLE_ROW_GROUP ||
                child == IdentValue.TABLE_FOOTER_GROUP ||
                child == IdentValue.TABLE_CAPTION))
                || ((parent == IdentValue.TABLE_HEADER_GROUP ||
                parent == IdentValue.TABLE_ROW_GROUP ||
                parent == IdentValue.TABLE_FOOTER_GROUP) &&
                child == IdentValue.TABLE_ROW)
                || (parent == IdentValue.TABLE_ROW && child == IdentValue.TABLE_CELL)
                || (parent == IdentValue.INLINE_TABLE && (child == IdentValue.TABLE_HEADER_GROUP ||
                child == IdentValue.TABLE_ROW_GROUP ||
                child == IdentValue.TABLE_FOOTER_GROUP));

    }

    private static boolean isNestingTableContent(IdentValue display) {
        return display == IdentValue.TABLE || display == IdentValue.INLINE_TABLE ||
                display == IdentValue.TABLE_HEADER_GROUP || display == IdentValue.TABLE_ROW_GROUP ||
                display == IdentValue.TABLE_FOOTER_GROUP || display == IdentValue.TABLE_ROW;
    }

    private static boolean isAttrFunction(FSFunction function) {
        if (function.getName().equals("attr")) {
            List<PropertyValue> params = function.getParameters();
            if (params.size() == 1) {
                PropertyValue value = params.get(0);
                return value.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT;
            }
        }

        return false;
    }

    public static boolean isElementFunction(FSFunction function) {
        if (function.getName().equals("element")) {
            List<PropertyValue> params = function.getParameters();
            if (params.size() < 1 || params.size() > 2) {
                return false;
            }
            boolean ok = true;
            PropertyValue value1 = params.get(0);
            ok = value1.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT;
            if (ok && params.size() == 2) {
                PropertyValue value2 = params.get(1);
                ok = value2.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT;
            }

            return ok;
        }

        return false;
    }

    private static CounterFunction makeCounterFunction(
            FSFunction function, LayoutContext c, CalculatedStyle style) {

        if (function.getName().equals("counter")) {
            List<PropertyValue> params = function.getParameters();
            if (params.size() < 1 || params.size() > 2) {
                return null;
            }

            PropertyValue value = params.get(0);
            if (value.getPrimitiveType() != CSSPrimitiveValue.CSS_IDENT) {
                return null;
            }

            String s = value.getStringValue();
            // counter(page) and counter(pages) are handled separately
            if (s.equals("page") || s.equals("pages")) {
                return null;
            }

            String counter = value.getStringValue();
            IdentValue listStyleType = IdentValue.DECIMAL;
            if (params.size() == 2) {
                value = params.get(1);
                if (value.getPrimitiveType() != CSSPrimitiveValue.CSS_IDENT) {
                    return null;
                }

                IdentValue identValue = IdentValue.valueOf(value.getStringValue());
                if (identValue != null) {
                    value.setIdentValue(identValue);
                    listStyleType = identValue;
                }
            }

            if ("footnote".equals(s)) {
                RootCounterContext rootCc = c.getSharedContext().getGlobalCounterContext();

                int counterValue = rootCc.getCurrentCounterValue(s);
                return new CounterFunction(counterValue, listStyleType);
            }

            AbstractCounterContext cc = c.getCounterContext(style);

            int counterValue = cc.getCurrentCounterValue(counter);

            return new CounterFunction(counterValue, listStyleType);
        } else if (function.getName().equals("counters")) {
            List<PropertyValue> params = function.getParameters();
            if (params.size() < 2 || params.size() > 3) {
                return null;
            }

            PropertyValue value = params.get(0);
            if (value.getPrimitiveType() != CSSPrimitiveValue.CSS_IDENT) {
                return null;
            }

            String counter = value.getStringValue();

            value = params.get(1);
            if (value.getPrimitiveType() != CSSPrimitiveValue.CSS_STRING) {
                return null;
            }

            String separator = value.getStringValue();

            IdentValue listStyleType = IdentValue.DECIMAL;
            if (params.size() == 3) {
                value = params.get(2);
                if (value.getPrimitiveType() != CSSPrimitiveValue.CSS_IDENT) {
                    return null;
                }

                IdentValue identValue = IdentValue.valueOf(value.getStringValue());
                if (identValue != null) {
                    value.setIdentValue(identValue);
                    listStyleType = identValue;
                }
            }

            List<Integer> counterValues = c.getCounterContext(style).getCurrentCounterValues(counter);

            return new CounterFunction(counterValues, separator, listStyleType);
        } else {
            return null;
        }
    }

    private static String getAttributeValue(FSFunction attrFunc, Element e) {
        PropertyValue value = attrFunc.getParameters().get(0);
        return e.getAttribute(value.getStringValue());
    }

    private static List<Styleable> createGeneratedContentList(
            LayoutContext c, Element element, List<PropertyValue> values,
            String peName, CalculatedStyle style, int mode, ChildBoxInfo info,
            List<Styleable> result) {

        for (PropertyValue value : values) {
            ContentFunction contentFunction = null;
            FSFunction function = null;

            String content = null;

            short type = value.getPrimitiveType();
            if (type == CSSPrimitiveValue.CSS_STRING) {
                content = value.getStringValue();
            } else if (type == CSSPrimitiveValue.CSS_URI) {
                Element creator = element != null ? element : c.getRootLayer().getMaster().getElement(); 

                Document doc = creator.getOwnerDocument();
                Element img = doc.createElement("img");

                img.setAttribute("src", value.getStringValue());
                // So we don't recurse into the element and create a duplicate box.
                img.setAttribute("fs-ignore", "true");
                creator.appendChild(img);

                CalculatedStyle anon = style.createAnonymousStyle(IdentValue.INLINE_BLOCK);

                BlockBox iB = new BlockBox();
                iB.setElement(img);
                iB.setStyle(anon);
                iB.setPseudoElementOrClass(peName);

                result.add(iB);
            } else if (value.getPropertyValueType() == PropertyValue.VALUE_TYPE_FUNCTION) {
                if (mode == CONTENT_LIST_DOCUMENT && isAttrFunction(value.getFunction())) {
                    content = getAttributeValue(value.getFunction(), element);
                } else {
                    CounterFunction cFunc = null;

                    if (mode == CONTENT_LIST_DOCUMENT) {
                        cFunc = makeCounterFunction(value.getFunction(), c, style);
                    }

                    if (cFunc != null) {
                        //TODO: counter functions may be called with non-ordered list-style-types, e.g. disc
                        content = cFunc.evaluate();
                        contentFunction = null;
                        function = null;
                    } else if (mode == CONTENT_LIST_MARGIN_BOX && isElementFunction(value.getFunction())) {
                        BlockBox target = getRunningBlock(c, value);
                        if (target != null) {
                            result.add(target.copyOf());
                            info.setContainsBlockLevelContent(true);
                        }
                    } else {
                        contentFunction =
                                c.getContentFunctionFactory().lookupFunction(c, value.getFunction());
                        if (contentFunction != null) {
                            function = value.getFunction();

                            if (contentFunction.isStatic()) {
                                content = contentFunction.calculate(c, function);
                                contentFunction = null;
                                function = null;
                            } else {
                                content = contentFunction.getLayoutReplacementText();
                            }
                        }
                    }
                }
            } else if (type == CSSPrimitiveValue.CSS_IDENT) {
                FSDerivedValue dv = style.valueByName(CSSName.QUOTES);

                if (dv != IdentValue.NONE) {
                    IdentValue ident = value.getIdentValue();

                    if (ident == IdentValue.OPEN_QUOTE) {
                        String[] quotes = style.asStringArray(CSSName.QUOTES);
                        content = quotes[0];
                    } else if (ident == IdentValue.CLOSE_QUOTE) {
                        String[] quotes = style.asStringArray(CSSName.QUOTES);
                        content = quotes[1];
                    }
                }
            }

            if (content != null) {
                InlineBox iB = new InlineBox(content);

                iB.setContentFunction(contentFunction);
                iB.setFunction(function);
                iB.setElement(element);
                iB.setPseudoElementOrClass(peName);
                iB.setStartsHere(true);
                iB.setEndsHere(true);

                c.addLayoutBoxId(element, iB);

                result.add(iB);
            }
        }

        return result;
    }

    /**
     * Creates an element with id for the footnote-marker pseudo element
     * so we can link to it from the footnote-call pseduo element.
     * <br><br>
     * See {@link #createFootnoteCallAnchor(LayoutContext, Element)}
     */
    private static Element createFootnoteTarget(LayoutContext c, Element parent) {
        if (parent == null) {
            return null;
        }

        Document doc = parent.getOwnerDocument();
        Element target = doc.createElement("fs-footnote-marker");

        target.setAttribute("id", "fs-footnote-" + c.getFootnoteIndex());

        parent.appendChild(target);

        return target;
    }

    /**
     * Used to create the anchor element to link to the footnote body for
     * the footnote-call pseudo element.
     * <br><br>
     * See {@link #createFootnoteTarget(LayoutContext, Element)}
     */
    private static Element createFootnoteCallAnchor(LayoutContext c, Element parent) {
        if (parent == null) {
            return null;
        }

        Document doc = parent.getOwnerDocument();
        Element anchor = doc.createElement("a");

        anchor.setAttribute("href", "#fs-footnote-" + c.getFootnoteIndex());

        parent.appendChild(anchor);

        return anchor;
    }

    public static BlockBox getRunningBlock(LayoutContext c, PropertyValue value) {
        List<PropertyValue> params = value.getFunction().getParameters();
        String ident = params.get(0).getStringValue();
        PageElementPosition position = null;
        if (params.size() == 2) {
            position = PageElementPosition.valueOf(params.get(1).getStringValue());
        }
        if (position == null) {
            position = PageElementPosition.FIRST;
        }
        BlockBox target = c.getRootDocumentLayer().getRunningBlock(ident, c.getPage(), position);
        return target;
    }

    private static void insertGeneratedContent(
            LayoutContext c, Element element, CalculatedStyle parentStyle,
            String peName, List<Styleable> children, ChildBoxInfo info) {

        CascadedStyle peStyle = c.getCss().getPseudoElementStyle(element, peName);

        if (peStyle != null) {
            PropertyDeclaration contentDecl = peStyle.propertyByName(CSSName.CONTENT);
            PropertyDeclaration counterResetDecl = peStyle.propertyByName(CSSName.COUNTER_RESET);
            PropertyDeclaration counterIncrDecl = peStyle.propertyByName(CSSName.COUNTER_INCREMENT);

            CalculatedStyle calculatedStyle = null;

            if (contentDecl != null || counterResetDecl != null || counterIncrDecl != null) {
                calculatedStyle = parentStyle.deriveStyle(peStyle);

                if (calculatedStyle.isDisplayNone() ||
                    calculatedStyle.isIdent(CSSName.CONTENT, IdentValue.NONE) ||
                    (calculatedStyle.isIdent(CSSName.CONTENT, IdentValue.NORMAL) && 
                       (peName.equals("before") || peName.equals("after")))) {
                    return;
                }

                if (calculatedStyle.isTable() ||
                    calculatedStyle.isTableRow() ||
                    calculatedStyle.isTableSection()) {

                    calculatedStyle = parentStyle.createAnonymousStyle(IdentValue.BLOCK);
                }

                c.resolveCounters(calculatedStyle);
            }

            if (contentDecl != null) {
                CSSPrimitiveValue propValue = contentDecl.getValue();
                children.addAll(createGeneratedContent(c, element, peName, calculatedStyle,
                        (PropertyValue) propValue, info));
            }
        }
    }

    /**
     * Creates generated content boxes for pseudo elements such as <code>::before</code>.
     *
     * @param element The containing element where the pseudo element appears.
     * For <code>span::before</code> the element would be a <code>span</code>.
     *
     * @param peName Examples include <code>before</code>, <code>after</code>,
     * <code>footnote-call</code> and <code>footnote-marker</code>.
     *
     * @param style The child style for this pseudo element. For <code>span::before</code>
     * this would include all the styles set explicitly on <code>::before</code> as well as
     * those that inherit from <code>span</code> following the cascade rules.
     *
     * @param property The values of the <code>content</code> CSS property.
     * @param info In/out param. Whether the resultant box(es) contain block level content.
     * @return The generated box(es). Typically one {@link BlockBox} or multiple inline boxes.
     */
    private static List<Styleable> createGeneratedContent(
            LayoutContext c, Element element, String peName,
            CalculatedStyle style, PropertyValue property, ChildBoxInfo info) {

        if (style.isDisplayNone()
                || style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)
                || style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN_GROUP)
                || property.getValues() == null) {
            return Collections.emptyList();
        }

        if ("footnote-call".equals(peName) || "footnote-marker".equals(peName)) {
            if (!isValidFootnotePseudo(style)) {
                logInvalidFootnotePseudo(peName, style);
                return Collections.emptyList();
            }
        } else {

            if (c.isInFloatBottom() && !isValidFootnotePseudo(style)) {
                // ::before or ::after in footnote.
                logInvalidFootnotePseudo(peName, style);
                return Collections.emptyList();
            } else if (style.isFootnote()) {
                // ::before or ::after trying to be a footnote.
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_CAN_NOT_BE_PSEUDO, peName);
                return Collections.emptyList();
            }
        }

        ChildBoxInfo childInfo = new ChildBoxInfo();

        List<PropertyValue> values = property.getValues();
        List<Styleable> result = new ArrayList<>(values.size());

        createGeneratedContentList(
                c, element, values, peName, style, CONTENT_LIST_DOCUMENT, childInfo, result);

        return wrapGeneratedContent(c, element, peName, style, info, childInfo, result);
    }

    private static List<Styleable> wrapGeneratedContent(
            LayoutContext c,
            Element element, String peName, CalculatedStyle style,
            ChildBoxInfo info, ChildBoxInfo childInfo, List<Styleable> inlineBoxes) {

        Element wrapperElement = element;
        if ("footnote-call".equals(peName)) {
            wrapperElement = createFootnoteCallAnchor(c, element);
        } else if ("footnote-marker".equals(peName)) {
            wrapperElement = createFootnoteTarget(c, element);
        }

        if (style.isInline()) {
            // Because a content property like: content: counter(page) ". ";
            // will generate multiple inline boxes we have to wrap them in a inline-box
            // and use a child style for the generated boxes. Otherwise, if we use the
            // pseudo style directly and it has a border, etc we will incorrectly get a border
            // around every content box.

            List<Styleable> pseudoInlines = new ArrayList<>(inlineBoxes.size() + 2);

            InlineBox pseudoStart = new InlineBox("");
            pseudoStart.setStartsHere(true);
            pseudoStart.setEndsHere(false);
            pseudoStart.setStyle(style);
            pseudoStart.setElement(wrapperElement);
            pseudoStart.setPseudoElementOrClass(peName);
            
            c.addLayoutBoxId(wrapperElement, pseudoStart);

            pseudoInlines.add(pseudoStart);

            CalculatedStyle inlineContent = style.createAnonymousStyle(IdentValue.INLINE);

            for (Styleable styleable : inlineBoxes) {
                if (styleable instanceof InlineBox) {
                    InlineBox iB = (InlineBox) styleable;

                    iB.setElement(null);
                    iB.setStyle(inlineContent);
                    iB.applyTextTransform();
                }

                pseudoInlines.add(styleable);
            }

            InlineBox pseudoEnd = new InlineBox("");
            pseudoEnd.setStartsHere(false);
            pseudoEnd.setEndsHere(true);
            pseudoEnd.setStyle(style);
            pseudoEnd.setElement(wrapperElement);
            pseudoEnd.setPseudoElementOrClass(peName);

            pseudoInlines.add(pseudoEnd);

            return pseudoInlines;
        } else {
            CalculatedStyle anon = style.createAnonymousStyle(IdentValue.INLINE);

            for (Styleable styleable : inlineBoxes) {
                if (styleable instanceof InlineBox) {
                    InlineBox iB = (InlineBox) styleable;

                    iB.setElement(null);
                    iB.setStyle(anon);
                    iB.applyTextTransform();
                }
            }

            BlockBox result = createBlockBox(style, info, true);
            result.setStyle(style);
            result.setInlineContent(inlineBoxes);
            result.setElement(wrapperElement);
            result.setChildrenContentType(BlockBox.ContentType.INLINE);
            result.setPseudoElementOrClass(peName);

            if (! style.isLayedOutInInlineContext()) {
                info.setContainsBlockLevelContent(true);
            }
            
            c.addLayoutBoxId(wrapperElement, result);

            return new ArrayList<>(Collections.singletonList(result));
        }
    }

    private static List<Styleable> createGeneratedMarginBoxContent(
            LayoutContext c, Element element, PropertyValue property,
            CalculatedStyle style, ChildBoxInfo info) {
        List<PropertyValue> values = property.getValues();

        if (values == null) {
           return Collections.emptyList();
        }

        List<Styleable> result = new ArrayList<>(values.size());
        createGeneratedContentList(
                c, element, values, null, style, CONTENT_LIST_MARGIN_BOX, info, result);

        CalculatedStyle anon = style.createAnonymousStyle(IdentValue.INLINE);
        for (Styleable s : result) {
            if (s instanceof InlineBox) {
                InlineBox iB = (InlineBox)s;
                iB.setElement(null);
                iB.setStyle(anon);
                iB.applyTextTransform();
            }
        }

        return result;
    }

    private static BlockBox createBlockBox(
            CalculatedStyle style, ChildBoxInfo info, boolean generated) {
        if (style.isFloated() && !(style.isAbsolute() || style.isFixed())) {
            BlockBox result;
            if (style.isTable() || style.isInlineTable()) {
                result = new TableBox();
            } else if (style.isTableCell()) {
                info.setContainsTableContent(true);
                result = new TableCellBox();
            } else {
                result = new BlockBox();
            }
            result.setFloatedBoxData(new FloatedBoxData());
            return result;
        } else if (style.isSpecifiedAsBlock()) {
            return new BlockBox();
        } else if (! generated && (style.isTable() || style.isInlineTable())) {
            return new TableBox();
        } else if (style.isTableCell()) {
            info.setContainsTableContent(true);
            return new TableCellBox();
        } else if (! generated && style.isTableRow()) {
            info.setContainsTableContent(true);
            return new TableRowBox();
        } else if (! generated && style.isTableSection()) {
            info.setContainsTableContent(true);
            return new TableSectionBox();
        } else if (style.isTableCaption()) {
            info.setContainsTableContent(true);
            return new BlockBox();
        } else {
            return new BlockBox();
        }
    }

    private static void addColumns(LayoutContext c, TableBox table, TableColumn parent) {
        SharedContext sharedContext = c.getSharedContext();

        Node working = parent.getElement().getFirstChild();
        boolean found = false;
        while (working != null) {
            if (working.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) working;
                CalculatedStyle style = sharedContext.getStyle(element);

                if (style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)) {
                    found = true;
                    TableColumn col = new TableColumn(element, style);
                    col.setParent(parent);
                    table.addStyleColumn(col);
                }
            }
            working = working.getNextSibling();
        }
        if (! found) {
            table.addStyleColumn(parent);
        }
    }

    private static void addColumnOrColumnGroup(
            LayoutContext c, TableBox table, Element e, CalculatedStyle style) {
        if (style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)) {
            table.addStyleColumn(new TableColumn(e, style));
        } else { /* style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN_GROUP) */
            addColumns(c, table, new TableColumn(e, style));
        }
    }

    private static InlineBox createInlineBox(
            String text, Element parent, CalculatedStyle parentStyle, Text node) {
        InlineBox result = new InlineBox(text);

        if (parentStyle.isInline() && ! (parent.getParentNode() instanceof Document)) {
            result.setStyle(parentStyle);
            result.setElement(parent);
        } else {
            result.setStyle(parentStyle.createAnonymousStyle(IdentValue.INLINE));
        }

        result.applyTextTransform();

        return result;
    }

    private static class CreateChildrenContext {
        CreateChildrenContext(
            boolean needStartText, boolean needEndText,
            CalculatedStyle parentStyle, boolean inline) {
            this.needStartText = needStartText;
            this.needEndText = needEndText;
            this.parentStyle = parentStyle;
            this.inline = inline;
        }

        boolean needStartText;
        boolean needEndText;
        boolean inline;

        InlineBox previousIB = null;
        final CalculatedStyle parentStyle;
    }

    private static boolean isValidFootnote(
            LayoutContext c, Element element, CalculatedStyle style) {
        return c.isPrint() &&
               (style.isInline() || style.isSpecifiedAsBlock()) &&
               !style.isPostionedOrFloated() &&
               !style.isRunning() &&
               !c.getSharedContext().getReplacedElementFactory().isReplacedElement(element);
    }

    private static void logInvalidFootnoteStyle(
            LayoutContext c, Element element, CalculatedStyle style) {
        String cause = "";

        if (!style.isInline() && !style.isSpecifiedAsBlock()) {
            cause = "The footnote element should be display: block (such as <div>)";
        } else if (style.isFloated()) {
            cause = "The footnote element must not be floated";
        } else if (c.getSharedContext().getReplacedElementFactory().isReplacedElement(element)) {
            cause = "The footnote element must not be replaced (such as <img>)";
        } else if (style.isPositioned() || style.isRunning()) {
            cause = "The footnote element must have position: static (not absolute, relative, running or fixed)";
        }

        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_INVALID, cause);
    }

    private static boolean isValidFootnotePseudo(CalculatedStyle style) {
        return !style.isFixed() && !style.isFootnote();
    }

    private static void logInvalidFootnotePseudo(String peName, CalculatedStyle style) {
        String cause = "";

        if (style.isFixed()) {
            cause = "Footnote pseudo element (" + peName + ") may not have fixed position";
        } else if (style.isFootnote()) {
            cause = "Footnote pseudo element (" + peName + ") may not have float: footnote set itself";
        }

        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_FOOTNOTE_PSEUDO_INVALID, cause);
    }

    /**
     * Don't output elements that have been artificially created to support
     * footnotes and content property images.
     */
    private static boolean isGeneratedElement(Element element) {
        String tag = element.getNodeName();

        return ("fs-footnote-marker".equals(tag)) ||
               ("a".equals(tag) && element.getAttribute("href").startsWith("#fs-footnote")) ||
               ("img".equals(tag) && element.getAttribute("fs-ignore").equals("true"));
    }

    private static void createElementChild(
            LayoutContext c,
            Element parent,
            BlockBox blockParent,
            Node working,
            List<Styleable> children,
            ChildBoxInfo info,
            CreateChildrenContext context) {

        Styleable child = null;
        SharedContext sharedContext = c.getSharedContext();
        Element element = (Element) working;
        CalculatedStyle style = sharedContext.getStyle(element);

        if (style.isDisplayNone() || isGeneratedElement(element)) {
            return;
        }

        resolveElementCounters(c, working, element, style);

        if (style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN) ||
            style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN_GROUP)) {

            if ((blockParent != null) &&
                (blockParent.getStyle().isTable() || blockParent.getStyle().isInlineTable())) {
                TableBox table = (TableBox) blockParent;
                addColumnOrColumnGroup(c, table, element, style);
            }

            return;
        }

        if (style.isFootnote() && !c.isInFloatBottom() && c.isFootnoteAllowed()) {
            if (isValidFootnote(c, element, style)) {
                c.setFootnoteIndex(c.getFootnoteIndex() + 1);

                // This is the official footnote call content that can generate zero or more boxes
                // depending on user for ::footnote-call pseudo element.
                insertGeneratedContent(c, element, style, "footnote-call", children, info);

                BlockBox footnoteBody = createFootnoteBody(c, element, style);

                // This is purely a marker box for the footnote so we
                // can figure out in layout when to add the footnote body.
                InlineBox iB = createInlineBox("", parent, context.parentStyle, null);
                iB.setStartsHere(true);
                iB.setEndsHere(true);
                iB.setFootnote(footnoteBody);

                children.add(iB);

                return;
            } else {
                logInvalidFootnoteStyle(c, element, style);
            }
        }

        if (style.isInline()) {
            createInlineChildren(c, parent, children, info, context, element);
        } else {
            child = createChildBlockBox(c, info, element, style);
        }

        if (child != null) {
            children.add(child);
        }
    }

    private static void createInlineChildren(
            LayoutContext c,
            Element parent,
            List<Styleable> children,
            ChildBoxInfo info,
            CreateChildrenContext context,
            Element element) {

        if (context.needStartText) {
            context.needStartText = false;
            InlineBox iB = createInlineBox("", parent, context.parentStyle, null);
            iB.setStartsHere(true);
            iB.setEndsHere(false);
            children.add(iB);
            context.previousIB = iB;
        }

        createChildren(c, null, element, children, info, true);

        if (context.inline) {
            if (context.previousIB != null) {
                context.previousIB.setEndsHere(false);
            }
            context.needEndText = true;
        }
    }

    private static Styleable createChildBlockBox(
            LayoutContext c, ChildBoxInfo info, Element element, CalculatedStyle style) {

        Styleable child;

        if (style.hasColumns() && c.isPrint()) {
            child = new FlowingColumnContainerBox();
        } else {
            child = createBlockBox(style, info, false);
        }

        child.setStyle(style);
        child.setElement(element);

        c.addLayoutBoxId(element, child);

        if (style.hasColumns() && c.isPrint()) {
            createColumnContainer(c, child, element, style);
        }

        if (style.isListItem()) {
            BlockBox block = (BlockBox) child;
            block.setListCounter(c.getCounterContext(style).getCurrentCounterValue("list-item"));
        }

        if (style.isTable() || style.isInlineTable()) {
            TableBox table = (TableBox) child;
            table.ensureChildren(c);

            child = reorderTableContent(c, table);
        }

        if (!info.isContainsBlockLevelContent()
                && !style.isLayedOutInInlineContext()) {
            info.setContainsBlockLevelContent(true);
        }

        BlockBox block = (BlockBox) child;

        if (block.getStyle().mayHaveFirstLine()) {
            block.setFirstLineStyle(c.getCss().getPseudoElementStyle(element,
                    "first-line"));
        }
        if (block.getStyle().mayHaveFirstLetter()) {
            block.setFirstLetterStyle(c.getCss().getPseudoElementStyle(element,
                    "first-letter"));
        }

        // I think we need to do this to evaluate counters correctly
        block.ensureChildren(c);
        return child;
    }

    /**
     * Creates the footnote body to put at the bottom of the page inside a
     * page's footnote area.
     */
    private static BlockBox createFootnoteBody(
            LayoutContext c, Element element, CalculatedStyle style) {

        List<Styleable> footnoteChildren = new ArrayList<>();
        ChildBoxInfo footnoteChildInfo = new ChildBoxInfo();

        // Create the out-of-flow footnote-body box as a block box.
        BlockBox footnoteBody = new BlockBox();
        CalculatedStyle footnoteBodyStyle = style.createAnonymousStyle(IdentValue.BLOCK);

        // Create a dummy element for the footnote-body.
        Element fnBodyElement = element.getOwnerDocument().createElement("fs-footnote-body");
        c.getRootLayer().getMaster().getElement().appendChild(fnBodyElement);

        footnoteBody.setElement(fnBodyElement);
        footnoteBody.setStyle(footnoteBodyStyle);

        // This will be set to the same as the footnote area when we add it to the page.
        footnoteBody.setContainingBlock(null);

        // Create an isolated layer for now. This will be changed to the footnote area
        // layer when we add this footnote body to a page.
        Layer layer = new Layer(footnoteBody, c, true);

        footnoteBody.setLayer(layer);
        footnoteBody.setContainingLayer(layer);

        c.pushLayer(layer);
        c.setIsInFloatBottom(true);

        CreateChildrenContext context = new CreateChildrenContext(false, false, style.getParent(), false);
        createElementChild(c, (Element) element.getParentNode(), footnoteBody, element, footnoteChildren, footnoteChildInfo, context);
        resolveChildren(c, footnoteBody, footnoteChildren, footnoteChildInfo);

        c.setFootnoteAllowed(true);
        c.setIsInFloatBottom(false);
        c.popLayer();

//        System.out.println();
//        System.out.println(com.openhtmltopdf.util.LambdaUtil.descendantDump(footnoteBody));
//        System.out.println();

        return footnoteBody;
    }

    private static void createColumnContainer(
         LayoutContext c, Styleable child, Element element, CalculatedStyle style) {

        FlowingColumnContainerBox cont = (FlowingColumnContainerBox) child;
        cont.setOnlyChild(c, new FlowingColumnBox(cont));
        cont.getChild().setStyle(style.createAnonymousStyle(IdentValue.BLOCK));
        cont.getChild().setElement(element);
        cont.getChild().ensureChildren(c);
    }

    private static void resolveElementCounters(
         LayoutContext c, Node working, Element element, CalculatedStyle style) {

        Integer attrValue = null;

        if ("ol".equals(working.getNodeName()) && element.hasAttribute("start")) {
            attrValue = OpenUtil.parseIntegerOrNull(element.getAttribute("start"));
        } else if ("li".equals(working.getNodeName()) && element.hasAttribute("value")) {
            attrValue = OpenUtil.parseIntegerOrNull(element.getAttribute("value"));
        }

        if (attrValue != null) {
            c.resolveCounters(style, attrValue - 1);
        } else {
            c.resolveCounters(style, null);
        }
    }

    private static void createChildren(
            LayoutContext c, BlockBox blockParent, Element parent,
            List<Styleable> children, ChildBoxInfo info, boolean inline) {

        if (isInsertedBoxIgnored(parent)) {
            return;
        }

        SharedContext sharedContext = c.getSharedContext();
        CalculatedStyle parentStyle = sharedContext.getStyle(parent);

        insertGeneratedContent(c, parent, parentStyle, "before", children, info);

        if (parentStyle.isFootnote()) {
            if (c.isFootnoteAllowed() && isValidFootnote(c, parent, parentStyle)) {
                insertGeneratedContent(c, parent, parentStyle, "footnote-marker", children, info);
                // Ban further footnote content until we bubble back up to createFootnoteBody.
                c.setFootnoteAllowed(false);
            } else if (!c.isFootnoteAllowed()) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.GENERAL_NO_FOOTNOTES_INSIDE_FOOTNOTES);
            }
        }

        Node working = parent.getFirstChild();
        CreateChildrenContext context = null;

        if (working != null) {
            context = new CreateChildrenContext(inline, inline, parentStyle, inline);

            do {
                short nodeType = working.getNodeType();

                if (nodeType == Node.ELEMENT_NODE) {
                    createElementChild(
                            c, parent, blockParent, working, children, info, context);
                } else if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
                    context.needStartText = false;
                    context.needEndText = false;

                    Text textNode = (Text) working;

                    // Ignore the text belonging to a textarea.
                    if (!textNode.getParentNode().getNodeName().equals("textarea")) {
                        context.previousIB = doBidi(c, textNode, parent, parentStyle, context.previousIB, children);
                    }
                }
            } while ((working = working.getNextSibling()) != null);
        }

        boolean needStartText = context != null ? context.needStartText : inline;
        boolean needEndText = context != null ? context.needEndText : inline;

        if (needStartText || needEndText) {
            InlineBox iB = createInlineBox("", parent, parentStyle, null);
            iB.setStartsHere(needStartText);
            iB.setEndsHere(needEndText);
            children.add(iB);
        }

        insertGeneratedContent(c, parent, parentStyle, "after", children, info);
    }

    private static InlineBox setupInlineChild(InlineBox child, InlineBox previousIB) {
        child.setEndsHere(true);
        
        if (previousIB == null) {
            child.setStartsHere(true);
        } else {
            previousIB.setEndsHere(false);
        }
        
        return child;
    }
    
    private static InlineBox doFakeBidi(LayoutContext c, Text textNode, Element parent, CalculatedStyle parentStyle, InlineBox previousIB, List<Styleable> children) {
    	String runText = textNode.getData();
    	InlineBox child = createInlineBox(runText, parent, parentStyle, textNode);
    	child.setTextDirection(BidiSplitter.LTR);
    	previousIB = setupInlineChild(child, previousIB);
       	children.add(child);
       	return previousIB;
    }
    
    
    /**
     * Attempts to divide a Text node further into directional text runs, either LTR or RTL. 
     * @param c
     * @param textNode
     * @param parent
     * @param parentStyle
     * @return the previousIB.
     */
    private static InlineBox doBidi(LayoutContext c, Text textNode, Element parent, CalculatedStyle parentStyle, InlineBox previousIB, List<Styleable> children) {
    	
        Paragraph para = c.getParagraphSplitter().lookupParagraph(textNode);
        if (para == null) {
        	// Must be no implementation of BIDI for this Text node.
        	return doFakeBidi(c, textNode, parent, parentStyle, previousIB, children);
        }
        
        int startIndex = para.getFirstCharIndexInParagraph(textNode); // Index into the paragraph.
        
        if (startIndex < 0) {
        	// Must be a fake implementation of BIDI.
        	return doFakeBidi(c, textNode, parent, parentStyle, previousIB, children);
        }
        
        int nodeIndex = 0;                                            // Index into the text node.
        String runText;                                               // Calculated text for the directional run.
        
        BidiTextRun prevSplit = para.prevSplit(startIndex); // Get directional run at or before startIndex.
        	
        assert(prevSplit != null);                  // There should always be a split at zero (start of paragraph) to fall back on. 
        assert(prevSplit.getStart() <= startIndex); // Split should always be before or at the start of this text node.

        // When calculating length, remember that it may overlap the start and/or end of the text node.
        int maxRunLength = prevSplit.getLength() - (startIndex - prevSplit.getStart());
       	int splitLength = Math.min(maxRunLength, textNode.getLength());
        
       	// Advance char indexes.
       	nodeIndex += splitLength;
       	startIndex += splitLength;
       	
       	assert(prevSplit.getDirection() == BidiSplitter.LTR || prevSplit.getDirection() == BidiSplitter.RTL);

       	if (splitLength == textNode.getLength()) {
       		// The simple case: the entire text node is part of a single direction run.
       		runText = textNode.getData();
       	}
       	else {
       		// The complex case: the first directional run only encompasses part of the text node. 
       		runText = textNode.getData().substring(0, nodeIndex);
       	}
       	
		// Shape here, so the layout will get the right visual length for the run.
		if (prevSplit.getDirection() == BidiSplitter.RTL) {
			runText = c.getBidiReorderer().shapeText(runText);
		}

       	InlineBox child = createInlineBox(runText, parent, parentStyle, textNode);
       	child.setTextDirection(prevSplit.getDirection());
       	previousIB = setupInlineChild(child, previousIB);
       	children.add(child);
       	
       	if (splitLength != textNode.getLength()) {
       		// We have more directional runs to extract.
       		
       		do {
        		BidiTextRun newSplit = para.nextSplit(startIndex);
        		assert(newSplit != null); // There should always be enough splits to completely cover the text node.
        		
        		int newLength;
        		
        		if (newSplit != null) {
        			// When calculating length, remember that it may overlap the start and/or end of the text node.
        			int newMaxRunLength = newSplit.getLength() - (startIndex - newSplit.getStart());
        			newLength = Math.min(newMaxRunLength, textNode.getLength() - nodeIndex);
        			
        			runText = textNode.getData().substring(nodeIndex, nodeIndex + newLength);
        			
        			// Shape here, so the layout will get the right visual length for the run.
        			if (newSplit.getDirection() == BidiSplitter.RTL) {
        				runText = c.getBidiReorderer().shapeText(runText);
        			}
        			
        			startIndex += newLength;
        			nodeIndex += newLength;
        			
        			child = createInlineBox(runText, parent, parentStyle, textNode);
        			child.setTextDirection(newSplit.getDirection());
        	       	previousIB = setupInlineChild(child, previousIB);
        	       	children.add(child);
        		}
        		else {
        			// We should never get here, but handle it just in case.
        			
        			newLength = textNode.getLength() - nodeIndex;
        			runText = textNode.getData().substring(nodeIndex, newLength);
        			
        			child = createInlineBox(runText, parent, parentStyle, textNode);
        			child.setTextDirection(c.getDefaultTextDirection());
        	       	previousIB = setupInlineChild(child, previousIB);
        	       	children.add(child);

        			startIndex += newLength;
        			nodeIndex += newLength;
        		}
        	} while(nodeIndex < textNode.getLength());
        }
       	
       	return previousIB;
    }
    
    private static void insertAnonymousBlocks(
            SharedContext c, Box parent, List<Styleable> children, boolean layoutRunningBlocks) {

        List<Styleable> inline = new ArrayList<>();
        Deque<InlineBox> parents = new ArrayDeque<>();
        List<InlineBox> savedParents = null;

        for (Styleable child : children) {
            if (child.getStyle().isLayedOutInInlineContext() &&
                    ! (layoutRunningBlocks && child.getStyle().isRunning()) &&
                    !child.getStyle().isTableCell() //see issue https://github.com/danfickle/openhtmltopdf/issues/309
            ) {
                inline.add(child);

                if (child.getStyle().isInline()) {
                    InlineBox iB = (InlineBox) child;
                    if (iB.isStartsHere()) {
                        parents.add(iB);
                    }
                    if (iB.isEndsHere()) {
                        parents.removeLast();
                    }
                }
            } else {
                if (inline.size() > 0) {
                    createAnonymousBlock(c, parent, inline, savedParents);
                    inline = new ArrayList<>();
                    savedParents = new ArrayList<>(parents);
                }
                parent.addChild((Box) child);
            }
        }

        createAnonymousBlock(c, parent, inline, savedParents);
    }

    private static void createAnonymousBlock(SharedContext c, Box parent, List<Styleable> inline, List<InlineBox> savedParents) {
        createAnonymousBlock(c, parent, inline, savedParents, IdentValue.BLOCK);
    }

    private static void createAnonymousBlock(SharedContext c, Box parent, List<Styleable> inline, List<InlineBox> savedParents, IdentValue display) {
        WhitespaceStripper.stripInlineContent(inline);
        if (inline.size() > 0) {
            AnonymousBlockBox anon = new AnonymousBlockBox(parent.getElement());
            anon.setStyle(parent.getStyle().createAnonymousStyle(display));
            anon.setAnonymous(true);
            if (savedParents != null && savedParents.size() > 0) {
                anon.setOpenInlineBoxes(savedParents);
            }
            parent.addChild(anon);
            anon.setChildrenContentType(BlockBox.ContentType.INLINE);
            anon.setInlineContent(inline);
        }
    }

    private static class ChildBoxInfo {
        private boolean _containsBlockLevelContent;
        private boolean _containsTableContent;
        private boolean _layoutRunningBlocks;

        public ChildBoxInfo() {
        }

        public boolean isContainsBlockLevelContent() {
            return _containsBlockLevelContent;
        }

        public void setContainsBlockLevelContent(boolean containsBlockLevelContent) {
            _containsBlockLevelContent = containsBlockLevelContent;
        }

        public boolean isContainsTableContent() {
            return _containsTableContent;
        }

        public void setContainsTableContent(boolean containsTableContent) {
            _containsTableContent = containsTableContent;
        }

        public boolean isLayoutRunningBlocks() {
            return _layoutRunningBlocks;
        }

        public void setLayoutRunningBlocks(boolean layoutRunningBlocks) {
            _layoutRunningBlocks = layoutRunningBlocks;
        }

        @Override
        public String toString() {
            return String.format(
                    "ChildBoxInfo [_containsBlockLevelContent=%s, _containsTableContent=%s, _layoutRunningBlocks=%s]",
                    _containsBlockLevelContent, _containsTableContent, _layoutRunningBlocks);
        }
    }
}

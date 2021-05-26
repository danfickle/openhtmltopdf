/*
 * Matcher.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package com.openhtmltopdf.css.newmatch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import com.openhtmltopdf.css.constants.MarginBoxName;
import com.openhtmltopdf.css.extend.AttributeResolver;
import com.openhtmltopdf.css.extend.StylesheetFactory;
import com.openhtmltopdf.css.extend.TreeResolver;
import com.openhtmltopdf.css.sheet.*;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;


/**
 * @author Torbjoern Gannholm
 */
public class Matcher {
    private final Mapper docMapper;
    private final AttributeResolver _attRes;
    private final TreeResolver _treeRes;
    private final StylesheetFactory _styleFactory;

    private final Map<Object, Mapper> _map = new HashMap<>();

    private final Set<Object> _hoverElements = new HashSet<>();
    private final Set<Object> _activeElements = new HashSet<>();
    private final Set<Object> _focusElements = new HashSet<>();
    private final Set<Object> _visitElements = new HashSet<>();

    private final List<PageRule> _pageRules = new ArrayList<>();
    private final List<FontFaceRule> _fontFaceRules = new ArrayList<>();

    public Matcher(
            TreeResolver tr, AttributeResolver ar, StylesheetFactory factory, List<Stylesheet> stylesheets, String medium) {
        _treeRes = tr;
        _attRes = ar;
        _styleFactory = factory;

        docMapper = createDocumentMapper(stylesheets, medium);
    }

    public CascadedStyle getCascadedStyle(Object e, boolean restyle) {
            Mapper em;
            if (!restyle) {
                em = getMapper(e);
            } else {
                em = matchElement(e);
            }
            return em.getCascadedStyle(e);
    }

    /**
     * Returns CSS rulesets for descendants of e.
     * For example, if e is an svg element and we have the ruleset
     * 'svg rect { .. }' then the string returned will be 'rect { .. }'.
     * 
     * FIXME: Does not correctly handle sibling selectors.
     */
    public String getCSSForAllDescendants(Object e) {
        // We must use the parent mapper as a starting point
        // to correctly handle direct child selectors such as 'body > svg rect'.
        Object parent = _treeRes.getParentElement(e);
        Mapper child = parent != null ? getMapper(parent) : docMapper;

        AllDescendantMapper descendants = new AllDescendantMapper(child.axes, _attRes, _treeRes);
        descendants.map(e);

        return descendants.toCSS();
    }

    /**
     * May return null.
     * We assume that restyle has already been done by a getCascadedStyle if necessary.
     */
    public CascadedStyle getPECascadedStyle(Object e, String pseudoElement) {
        //synchronized (e) {
            Mapper em = getMapper(e);
            return em.getPECascadedStyle(e, pseudoElement);
        //}
    }
    
    public PageInfo getPageCascadedStyle(String pageName, String pseudoPage) {
        List<PropertyDeclaration> props = new ArrayList<>();
        Map<MarginBoxName, List<PropertyDeclaration>>  marginBoxes = new HashMap<>();
        List<PropertyDeclaration> footnote = new ArrayList<>();

        for (PageRule pageRule : _pageRules) {
            if (pageRule.applies(pageName, pseudoPage)) {
                props.addAll(pageRule.getRuleset().getPropertyDeclarations());
                marginBoxes.putAll(pageRule.getMarginBoxes());

                if (pageRule.getFootnoteAreaProperties() != null) {
                    footnote.addAll(pageRule.getFootnoteAreaProperties());
                }
            }
        }
        
        CascadedStyle style;
        if (props.isEmpty()) {
            style = CascadedStyle.emptyCascadedStyle;
        } else {
            style = new CascadedStyle(props.iterator());
        }
        
        return new PageInfo(props, style, marginBoxes, footnote);
    }
    
    public List<FontFaceRule> getFontFaceRules() {
        return _fontFaceRules;
    }
    
    public boolean isVisitedStyled(Object e) {
        return _visitElements.contains(e);
    }

    public boolean isHoverStyled(Object e) {
        return _hoverElements.contains(e);
    }

    public boolean isActiveStyled(Object e) {
        return _activeElements.contains(e);
    }

    public boolean isFocusStyled(Object e) {
        return _focusElements.contains(e);
    }

    protected Mapper matchElement(Object e) {
            Object parent = _treeRes.getParentElement(e);
            Mapper child;
            if (parent != null) {
                Mapper m = getMapper(parent);
                child = m.mapChild(e);
            } else {//has to be document or fragment node
                child = docMapper.mapChild(e);
            }
            return child;
    }

    Mapper createDocumentMapper(List<Stylesheet> stylesheets, String medium) {
        java.util.TreeMap<String,Selector> sorter = new java.util.TreeMap<>();
        addAllStylesheets(stylesheets, sorter, medium);
        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.MATCH_MATCHER_CREATED_WITH_SELECTOR, sorter.size());
        return new Mapper(sorter.values());
    }
    
    private void addAllStylesheets(List<Stylesheet> stylesheets, TreeMap<String, Selector> sorter, String medium) {
        int count = 0;
        int pCount = 0;
        for (Stylesheet stylesheet : stylesheets) {
            for (Object obj : stylesheet.getContents()) {
                if (obj instanceof Ruleset) {
                    for (Selector selector : ((Ruleset) obj).getFSSelectors()) {
                        selector.setPos(++count);
                        sorter.put(selector.getOrder(), selector);
                    }
                } else if (obj instanceof PageRule) {
                    ((PageRule) obj).setPos(++pCount);
                    _pageRules.add((PageRule) obj);
                } else if (obj instanceof MediaRule) {
                    MediaRule mediaRule = (MediaRule) obj;
                    if (mediaRule.matches(medium)) {
                        for (Object o : mediaRule.getContents()) {
                            Ruleset ruleset = (Ruleset) o;
                            for (Object o1 : ruleset.getFSSelectors()) {
                                Selector selector = (Selector) o1;
                                selector.setPos(++count);
                                sorter.put(selector.getOrder(), selector);
                            }
                        }
                    }
                }
            }

            _fontFaceRules.addAll(stylesheet.getFontFaceRules());
        }
        
        Collections.sort(_pageRules, new Comparator<PageRule>() {
            @Override
            public int compare(PageRule p1, PageRule p2) {
                if (p1.getOrder() - p2.getOrder() < 0) {
                    return -1;
                } else if (p1.getOrder() == p2.getOrder()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }

    private void link(Object e, Mapper m) {
        _map.put(e, m);
    }

    private Mapper getMapper(Object e) {
        Mapper m = _map.get(e);
        if (m != null) {
            return m;
        }
        m = matchElement(e);
        return m;
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private com.openhtmltopdf.css.sheet.Ruleset getElementStyle(Object e) {
        //synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            
            String style = _attRes.getElementStyling(e);
            if (isNullOrEmpty(style)) {
                return null;
            }
            
            return _styleFactory.parseStyleDeclaration(com.openhtmltopdf.css.sheet.StylesheetInfo.AUTHOR, style);
        //}
    }

    private com.openhtmltopdf.css.sheet.Ruleset getNonCssStyle(Object e) {
        //synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            String style = _attRes.getNonCssStyling(e);
            if (isNullOrEmpty(style)) {
                return null;
            }
            return _styleFactory.parseStyleDeclaration(com.openhtmltopdf.css.sheet.StylesheetInfo.AUTHOR, style);
        //}
    }

    /**
     * Mapper represents a local CSS for a Node that is used to match the Node's
     * children.
     *
     * @author Torbjoern Gannholm
     */
    class Mapper {
        private final List<Selector> axes;
        private final Map<String, List<Selector>> pseudoSelectors;
        private final List<Selector> mappedSelectors;

        private Map<String, Mapper> children;

        Mapper(Collection<Selector> selectors) {
            this.axes = new ArrayList<>(selectors);
            this.pseudoSelectors = Collections.emptyMap();
            this.mappedSelectors = Collections.emptyList();
        }

        private Mapper(
                List<Selector> axes, 
                List<Selector> mappedSelectors,
                Map<String,List<Selector>> pseudoSelectors) {
            this.axes = axes;
            this.mappedSelectors = mappedSelectors;
            this.pseudoSelectors = pseudoSelectors;
        }

        /**
         * Side effect: creates and stores a Mapper for the element
         *
         * @param e
         * @return The selectors that matched, sorted according to specificity
         *         (more correct: preserves the sort order from Matcher creation)
         */
        Mapper mapChild(Object e) {
            List<Selector> childAxes = null;
            List<Selector> mappedSelectors = null;
            Map<String, List<Selector>> pseudoSelectors = null;

            StringBuilder key = new StringBuilder();

            for (Selector sel : axes) {
                if (sel.getAxis() == Selector.DESCENDANT_AXIS) {
                    if (childAxes == null) {
                        childAxes = new ArrayList<>();
                    }

                    // Carry it forward to other descendants
                    childAxes.add(sel);
                } else if (sel.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                }

                if (!sel.matches(e, _attRes, _treeRes)) {
                    continue;
                }

                // Assumption: if it is a pseudo-element, it does not also have dynamic pseudo-class
                String pseudoElement = sel.getPseudoElement();

                if (pseudoElement != null) {
                    if (pseudoSelectors == null) {
                        pseudoSelectors = new HashMap<>();
                    }

                    List<Selector> l = pseudoSelectors.computeIfAbsent(pseudoElement, kee -> new ArrayList<>());
                    l.add(sel);

                    key.append(sel.getSelectorID()).append(":");
                    continue;
                }

                if (sel.isPseudoClass(Selector.VISITED_PSEUDOCLASS)) {
                    _visitElements.add(e);
                }
                if (sel.isPseudoClass(Selector.ACTIVE_PSEUDOCLASS)) {
                    _activeElements.add(e);
                }
                if (sel.isPseudoClass(Selector.HOVER_PSEUDOCLASS)) {
                    _hoverElements.add(e);
                }
                if (sel.isPseudoClass(Selector.FOCUS_PSEUDOCLASS)) {
                    _focusElements.add(e);
                }

                if (!sel.matchesDynamic(e, _attRes, _treeRes)) {
                    continue;
                }

                key.append(sel.getSelectorID()).append(":");

                Selector chain = sel.getChainedSelector();

                if (chain == null) {
                    if (mappedSelectors == null) {
                        mappedSelectors = new ArrayList<>();
                    }

                    mappedSelectors.add(sel);
                } else if (chain.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                } else {
                    if (childAxes == null) {
                        childAxes = new ArrayList<>();
                    }

                    childAxes.add(chain);
                }
            }

            if (children == null) {
                children = new HashMap<>();
            }

            List<Selector> normalisedChildAxes = childAxes == null ? Collections.emptyList() : childAxes;
            List<Selector> normalisedMappedSelectors = mappedSelectors == null ? Collections.emptyList() : mappedSelectors;
            Map<String, List<Selector>> normalisedPseudoSelectors = pseudoSelectors == null ? Collections.emptyMap() : pseudoSelectors;

            Mapper childMapper = children.computeIfAbsent(
                    key.toString(),
                    kee -> new Mapper(
                        normalisedChildAxes,
                        normalisedMappedSelectors,
                        normalisedPseudoSelectors));

            link(e, childMapper);

            return childMapper;
        }

        CascadedStyle getCascadedStyle(Object e) {
            Ruleset elementStyling = getElementStyle(e);
            Ruleset nonCssStyling = getNonCssStyle(e);

            List<PropertyDeclaration> propList = new ArrayList<>();

            // Specificity 0,0,0,0
            if (nonCssStyling != null) {
                propList.addAll(nonCssStyling.getPropertyDeclarations());
            }

            // These should have been returned in order of specificity
            for (Selector sel : mappedSelectors) {
                propList.addAll(sel.getRuleset().getPropertyDeclarations());
            }

            // Specificity 1,0,0,0
            if (elementStyling != null) {
                propList.addAll(elementStyling.getPropertyDeclarations());
            }

            if (propList.isEmpty()) {
                return CascadedStyle.emptyCascadedStyle;
            } else {
                return new CascadedStyle(propList.iterator());
            }
        }

        /**
         * May return null.
         * We assume that restyle has already been done by a getCascadedStyle if necessary.
         */
        public CascadedStyle getPECascadedStyle(Object e, String pseudoElement) {
            if (pseudoSelectors.isEmpty()) {
                return null;
            }

            List<Selector> pe = pseudoSelectors.get(pseudoElement);

            if (pe == null) {
                return null;
            }

            List<PropertyDeclaration> propList = new ArrayList<>();

            for (Selector sel : pe) {
                propList.addAll(sel.getRuleset().getPropertyDeclarations());
            }

            if (propList.isEmpty()) {
                return CascadedStyle.emptyCascadedStyle;
            } else {
                return new CascadedStyle(propList.iterator());
            }
        }
    }

    public static class AllDescendantMapper {
        private final List<Selector> axes;
        private final List<Selector> mappedSelectors = new ArrayList<>();
        private final Set<Selector> topSelectors = new HashSet<>();
        private final AttributeResolver attRes;
        private final TreeResolver treeRes;

        AllDescendantMapper(List<Selector> axes, AttributeResolver attRes, TreeResolver treeRes) {
            this.axes = axes;
            this.attRes = attRes;
            this.treeRes = treeRes;
        }

        String toCSS() {
            StringBuilder sb = new StringBuilder();

            for (Selector sel : mappedSelectors) {
                sel.toCSS(sb, topSelectors);
                sel.getRuleset().toCSS(sb);
            }

            return sb.toString();
        }

        void map(Object e) {
            Deque<Selector> queue = new ArrayDeque<>();

            for (Selector sel : axes) {
                if (!sel.matches(e, attRes, treeRes) ||
                    sel.getChainedSelector() == null) {
                    continue;
                }

                queue.addLast(sel);
                this.topSelectors.add(sel);
            }

            while (!queue.isEmpty()) {
                Selector current = queue.removeFirst();

                Selector chain = current.getChainedSelector();

                if (chain == null) {
                    this.mappedSelectors.add(current);
                } else {
                    queue.addLast(chain);
                }
            }
        }
    }
}


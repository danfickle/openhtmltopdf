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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.openhtmltopdf.css.constants.MarginBoxName;
import com.openhtmltopdf.css.extend.AttributeResolver;
import com.openhtmltopdf.css.extend.StylesheetFactory;
import com.openhtmltopdf.css.extend.TreeResolver;
import com.openhtmltopdf.css.sheet.*;
import com.openhtmltopdf.util.Util;
import com.openhtmltopdf.util.XRLog;


/**
 * @author Torbjoern Gannholm
 */
public class Matcher {

    private Mapper docMapper;
    private com.openhtmltopdf.css.extend.AttributeResolver _attRes;
    private com.openhtmltopdf.css.extend.TreeResolver _treeRes;
    private com.openhtmltopdf.css.extend.StylesheetFactory _styleFactory;

    private java.util.Map<Object, Mapper> _map;

    //handle dynamic
    private Set<Object> _hoverElements;
    private Set<Object> _activeElements;
    private Set<Object> _focusElements;
    private Set<Object> _visitElements;
    
    private final List<PageRule> _pageRules = new ArrayList<PageRule>();
    private final List<FontFaceRule> _fontFaceRules = new ArrayList<FontFaceRule>();
    
    public Matcher(
            TreeResolver tr, AttributeResolver ar, StylesheetFactory factory, List<Stylesheet> stylesheets, String medium) {
        newMaps();
        _treeRes = tr;
        _attRes = ar;
        _styleFactory = factory;

        docMapper = createDocumentMapper(stylesheets, medium);
    }
    
    public void removeStyle(Object e) {
        _map.remove(e);
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
        List<PropertyDeclaration>  props = new ArrayList<PropertyDeclaration> ();
        Map<MarginBoxName, List<PropertyDeclaration>>  marginBoxes = new HashMap<MarginBoxName, List<PropertyDeclaration>>();

        for (PageRule pageRule : _pageRules) {
            if (pageRule.applies(pageName, pseudoPage)) {
                props.addAll(pageRule.getRuleset().getPropertyDeclarations());
                marginBoxes.putAll(pageRule.getMarginBoxes());
            }
        }
        
        CascadedStyle style;
        if (props.isEmpty()) {
            style = CascadedStyle.emptyCascadedStyle;
        } else {
            style = new CascadedStyle(props.iterator());
        }
        
        return new PageInfo(props, style, marginBoxes);
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
        java.util.TreeMap<String,Selector> sorter = new java.util.TreeMap<String,Selector>();
        addAllStylesheets(stylesheets, sorter, medium);
        XRLog.match("Matcher created with " + sorter.size() + " selectors");
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

    private void newMaps() {
        _map = new java.util.HashMap<Object, Mapper>();
        _hoverElements = new java.util.HashSet<Object>();
        _activeElements = new java.util.HashSet<Object>();
        _focusElements = new java.util.HashSet<Object>();
        _visitElements = new java.util.HashSet<Object>();
    }

    private Mapper getMapper(Object e) {
        Mapper m = _map.get(e);
        if (m != null) {
            return m;
        }
        m = matchElement(e);
        return m;
    }

    private com.openhtmltopdf.css.sheet.Ruleset getElementStyle(Object e) {
        //synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            
            String style = _attRes.getElementStyling(e);
            if (Util.isNullOrEmpty(style)) {
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
            if (Util.isNullOrEmpty(style)) {
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
        java.util.List<Selector> axes;
        private HashMap<String,List<Selector>> pseudoSelectors;
        private List<Selector> mappedSelectors;
        private Map<String,Mapper> children;

        Mapper(java.util.Collection<Selector> selectors) {
            axes = new java.util.ArrayList<Selector>(selectors);
        }

        private Mapper() {
        }

        /**
         * Side effect: creates and stores a Mapper for the element
         *
         * @param e
         * @return The selectors that matched, sorted according to specificity
         *         (more correct: preserves the sort order from Matcher creation)
         */
        Mapper mapChild(Object e) {
            //Mapper childMapper = new Mapper();
            java.util.List<Selector> childAxes = new ArrayList<Selector>(axes.size() + 10);
            java.util.HashMap<String,List<Selector>> pseudoSelectors = new java.util.HashMap<String,List<Selector>>();
            java.util.List<Selector> mappedSelectors = new java.util.ArrayList<Selector>();
            StringBuilder key = new StringBuilder();
            for (Selector sel : axes) {
                if (sel.getAxis() == Selector.DESCENDANT_AXIS) {
                    //carry it forward to other descendants
                    childAxes.add(sel);
                } else if (sel.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                }
                if (!sel.matches(e, _attRes, _treeRes)) {
                    continue;
                }
                //Assumption: if it is a pseudo-element, it does not also have dynamic pseudo-class
                String pseudoElement = sel.getPseudoElement();
                if (pseudoElement != null) {
                    List<Selector> l = pseudoSelectors.get(pseudoElement);
                    if (l == null) {
                        l = new ArrayList<Selector>();
                        pseudoSelectors.put(pseudoElement, l);
                    }
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
                    mappedSelectors.add(sel);
                } else if (chain.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                } else {
                    childAxes.add(chain);
                }
            }
            if (children == null) children = new HashMap<String,Mapper>();
            Mapper childMapper = children.get(key.toString());
            if (childMapper == null) {
                childMapper = new Mapper();
                childMapper.axes = childAxes;
                childMapper.pseudoSelectors = pseudoSelectors;
                childMapper.mappedSelectors = mappedSelectors;
                children.put(key.toString(), childMapper);
            }
            link(e, childMapper);
            return childMapper;
        }

        CascadedStyle getCascadedStyle(Object e) {
            CascadedStyle result;

                CascadedStyle cs = null;
                com.openhtmltopdf.css.sheet.Ruleset elementStyling = getElementStyle(e);
                com.openhtmltopdf.css.sheet.Ruleset nonCssStyling = getNonCssStyle(e);
                List<PropertyDeclaration> propList = new ArrayList<PropertyDeclaration>();
                //specificity 0,0,0,0
                if (nonCssStyling != null) {
                    propList.addAll(nonCssStyling.getPropertyDeclarations());
                }
                //these should have been returned in order of specificity
                for (Selector sel : mappedSelectors) {
                    propList.addAll(sel.getRuleset().getPropertyDeclarations());
                }
                //specificity 1,0,0,0
                if (elementStyling != null) {
                    propList.addAll(elementStyling.getPropertyDeclarations());
                }
                if (propList.size() == 0)
                    cs = CascadedStyle.emptyCascadedStyle;
                else {
                    cs = new CascadedStyle(propList.iterator());
                }

                result = cs;

            return result;
        }

        /**
         * May return null.
         * We assume that restyle has already been done by a getCascadedStyle if necessary.
         */
        public CascadedStyle getPECascadedStyle(Object e, String pseudoElement) {
            java.util.Iterator<Map.Entry<String,List<Selector>>> si = pseudoSelectors.entrySet().iterator();
            if (!si.hasNext()) {
                return null;
            }
            CascadedStyle cs = null;
            java.util.List<Selector> pe = pseudoSelectors.get(pseudoElement);
            if (pe == null) return null;

            java.util.List<PropertyDeclaration> propList = new java.util.ArrayList<PropertyDeclaration>();
            for (Selector sel : pe) {
                propList.addAll(sel.getRuleset().getPropertyDeclarations());
            }
            if (propList.size() == 0)
                cs = CascadedStyle.emptyCascadedStyle;//already internalized
            else {
                cs = new CascadedStyle(propList.iterator());
            }
            return cs;
        }
    }
}


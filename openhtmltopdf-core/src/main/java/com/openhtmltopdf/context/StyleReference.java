/*
 * StyleReference.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
package com.openhtmltopdf.context;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.util.LogMessageId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.extend.AttributeResolver;
import com.openhtmltopdf.css.extend.lib.DOMTreeResolver;
import com.openhtmltopdf.css.newmatch.CascadedStyle;
import com.openhtmltopdf.css.newmatch.PageInfo;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.sheet.Stylesheet;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.extend.UserInterface;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.util.XRLog;


/**
 * @author Torbjoern Gannholm
 */
public class StyleReference {
    /**
     * The Context this StyleReference operates in; used for property
     * resolution.
     */
    private SharedContext _context;
    private NamespaceHandler _nsh;
    private Document _doc;
    private final StylesheetFactoryImpl _stylesheetFactory;

    /**
     * Instance of our element-styles matching class. Will be null if new rules
     * have been added since last match.
     */
    private com.openhtmltopdf.css.newmatch.Matcher _matcher;

    private UserAgentCallback _uac;
    
    public StyleReference(UserAgentCallback userAgent) {
        _uac = userAgent;
        _stylesheetFactory = new StylesheetFactoryImpl(userAgent);
    }

    /**
     * Gets the style of the root element, should be html tag.
     */
    public CalculatedStyle getRootElementStyle() {
        if (_context != null && _doc != null) {
            return _context.getStyle(_doc.getDocumentElement());
        } else {
            return null;
        }
    }

    /**
     * Sets the documentContext attribute of the StyleReference object
     *
     * @param context The new documentContext value
     * @param nsh     The new documentContext value
     * @param doc     The new documentContext value
     * @param ui
     */
    public void setDocumentContext(SharedContext context, NamespaceHandler nsh, Document doc, UserInterface ui) {
        _context = context;
        _nsh = nsh;
        _doc = doc;
        AttributeResolver attRes = new StandardAttributeResolver(_nsh, _uac, ui);

        List<StylesheetInfo> infos = getStylesheets();

        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.MATCH_MEDIA_IS, _context.getMedia());
        
        _matcher = new com.openhtmltopdf.css.newmatch.Matcher(
                new DOMTreeResolver(), 
                attRes, 
                _stylesheetFactory, 
                readAndParseAll(infos, _context.getMedia()), 
                _context.getMedia());
    }
    
    private List<Stylesheet> readAndParseAll(List<StylesheetInfo> infos, String medium) {
        List<Stylesheet> result = new ArrayList<>(infos.size() + 15);
        
        for (StylesheetInfo info : infos) {
            if (info.appliesToMedia(medium)) {
                Stylesheet sheet = info.getStylesheet();
                
                if (sheet == null) {
                    sheet = _stylesheetFactory.getStylesheet(info);
                }
                
                if (sheet != null) {
                    if (sheet.getImportRules().size() > 0) {
                        result.addAll(readAndParseAll(sheet.getImportRules(), medium));
                    }
                    
                    result.add(sheet);
                } else {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.LOAD_UNABLE_TO_LOAD_CSS_FROM_URI, info.getUri());
                }
            }
        }
        
        return result;
    }

    public boolean isHoverStyled(Element e) {
        return _matcher.isHoverStyled(e);
    }

    /**
     * Returns a Map keyed by CSS property names (e.g. 'border-width'), and the
     * assigned value as a SAC CSSValue instance. The properties should have
     * been matched to the element when the Context was established for this
     * StyleReference on the Document to which the Element belongs.
     * 
     * Only used by broken DOM inspector.
     *
     * @param e The DOM Element for which to find properties
     * @return Map of CSS property names to CSSValue instance assigned to it.
     */
    @Deprecated
	public java.util.Map<String, CSSPrimitiveValue> getCascadedPropertiesMap(Element e) {
        CascadedStyle cs = _matcher.getCascadedStyle(e, false);
        
		java.util.Map<String, CSSPrimitiveValue> props = new java.util.LinkedHashMap<>();
		
		for (PropertyDeclaration pd : cs.getCascadedPropertyDeclarations()) {
            String propName = pd.getPropertyName();
            CSSName cssName = CSSName.getByPropertyName(propName);
            props.put(propName, cs.propertyByName(cssName).getValue());
        }
		
        return props;
    }

    /**
     * Gets the pseudoElementStyle attribute of the StyleReference object
     *
     * @param node          PARAM
     * @param pseudoElement PARAM
     * @return The pseudoElementStyle value
     */
    public CascadedStyle getPseudoElementStyle(Node node, String pseudoElement) {
        Element e = null;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            e = (Element) node;
        } else {
            e = (Element) node.getParentNode();
        }
        return _matcher.getPECascadedStyle(e, pseudoElement);
    }

    /**
     * Gets the CascadedStyle for an element. This must then be converted in the
     * current context to a CalculatedStyle (use getDerivedStyle)
     *
     * @param e       The element
     * @param restyle
     * @return The style value
     */
    public CascadedStyle getCascadedStyle(Element e, boolean restyle) {
        if (e == null) return CascadedStyle.emptyCascadedStyle;
        return _matcher.getCascadedStyle(e, restyle);
    }

    /**
     * Given an element, returns all selectors and their rulesets
     * for its descendants. Useful for getting the styles that should be
     * applied to SVG, etc.
     */
    public String getCSSForAllDescendants(Element e) {
        return _matcher.getCSSForAllDescendants(e);
    }

    public PageInfo getPageStyle(String pageName, String pseudoPage) {
        return _matcher.getPageCascadedStyle(pageName, pseudoPage);
    }

    /**
     * Gets StylesheetInfos for all stylesheets and inline styles associated
     * with the current document. Default (user agent) stylesheet and the inline
     * style for the current media are loaded in the
     * StyleSheetFactory by URI.
     *
     * @return The stylesheets value
     */
    private List<StylesheetInfo> getStylesheets() {
        List<StylesheetInfo> infos = new ArrayList<>();
        long st = System.currentTimeMillis();

        StylesheetInfo defaultStylesheet = _nsh.getDefaultStylesheet(_stylesheetFactory);
        if (defaultStylesheet != null) {
            infos.add(defaultStylesheet);
        }

        StylesheetInfo[] refs = _nsh.getStylesheets(_doc);
        int inlineStyleCount = 0;
        if (refs != null) {
            for (StylesheetInfo ref : refs) {
                String uri;

                if (!ref.isInline()) {
                    uri = _uac.resolveURI(ref.getUri());
                    ref.setUri(uri);
                } else {
                    ref.setUri(_uac.getBaseURL() + "#inline_style_" + (++inlineStyleCount));
                    Stylesheet sheet = _stylesheetFactory.parse(
                            new StringReader(ref.getContent()), ref);
                    ref.setStylesheet(sheet);
                    ref.setUri(null);
                }
            }
            infos.addAll(Arrays.asList(refs));
        }

        // TODO: here we should also get user stylesheet from userAgent

        long el = System.currentTimeMillis() - st;
        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_PARSE_STYLESHEETS_TIME, el);

        return infos;
    }

    public List<FontFaceRule> getFontFaceRules() {
        return _matcher.getFontFaceRules();
    }
    
    public void setUserAgentCallback(UserAgentCallback userAgentCallback) {
        _uac = userAgentCallback;
        _stylesheetFactory.setUserAgentCallback(userAgentCallback);
    }
    
    public void setSupportCMYKColors(boolean b) {
        _stylesheetFactory.setSupportCMYKColors(b);
    }
}

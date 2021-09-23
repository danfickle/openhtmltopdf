/*
 * Copyright (c) 2005 Torbjoern Gannholm
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
package com.openhtmltopdf.simple.extend;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.OpenUtil;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.openhtmltopdf.css.extend.StylesheetFactory;
import com.openhtmltopdf.css.sheet.Stylesheet;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.simple.NoNamespaceHandler;
import com.openhtmltopdf.util.XRLog;

/**
 * Handles xhtml but only css styling is honored,
 * no presentational html attributes (see css 2.1 spec, 6.4.4)
 */
public class XhtmlCssOnlyNamespaceHandler extends NoNamespaceHandler {

    /**
     * Description of the Field
     */
    final static String _namespace = "http://www.w3.org/1999/xhtml";

    private static StylesheetInfo _defaultStylesheet;
    private static boolean _defaultStylesheetError = false;

    private String _contentLanguageMetaValue;

    /**
     * Gets the namespace attribute of the XhtmlNamespaceHandler object
     *
     * @return The namespace value
     */
    @Override
    public String getNamespace() {
        return _namespace;
    }

    /**
     * Gets the class attribute of the XhtmlNamespaceHandler object
     *
     * @return The class value
     */
    @Override
    public String getClass(org.w3c.dom.Element e) {
        return e.getAttribute("class");
    }

    /**
     * Gets the iD attribute of the XhtmlNamespaceHandler object
     *
     * @return The iD value
     */
    @Override
    public String getID(org.w3c.dom.Element e) {
        String result = e.getAttribute("id").trim();
        return result.length() == 0 ? null : result;
    }

    protected String convertToLength(String value) {
        if (isInteger(value)) {
            return value + "px";
        } else {
            return value;
        }
    }

    protected boolean isInteger(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (! (c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    protected String getAttribute(Element e, String attrName) {
        String result = e.getAttribute(attrName);
        result = result.trim();
        return result.length() == 0 ? null : result;
    }

    private static String readTextContent(Element element) {
        StringBuilder result = new StringBuilder();
        Node current = element.getFirstChild();
        while (current != null) {
            short nodeType = current.getNodeType();
            if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE) {
                Text t = (Text)current;
                result.append(t.getData());
            }
            current = current.getNextSibling();
        }
        return result.toString();
    }
    private static String collapseWhiteSpace(String text) {
        StringBuilder result = new StringBuilder();
        int l = text.length();
        for (int i = 0; i < l; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                result.append(' ');
                while (++i < l) {
                    c = text.charAt(i);
                    if (! Character.isWhitespace(c)) {
                        i--;
                        break;
                    }
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    /**
     * Gets the linkUri attribute of the XhtmlNamespaceHandler object
     *
     * @param e PARAM
     * @return The linkUri value
     */
    @Override
    public String getLinkUri(org.w3c.dom.Element e) {
        String href = null;
        if (e.getNodeName().equalsIgnoreCase("a") && e.hasAttribute("href")) {
            href = e.getAttribute("href");
        }
        return href;
    }

    @Override
    public String getAnchorName(Element e) {
        if (e != null && e.getNodeName().equalsIgnoreCase("a") &&
                e.hasAttribute("name")) {
            return e.getAttribute("name");
        }
        return null;
    }
    /**
     * Gets the elementStyling attribute of the XhtmlNamespaceHandler object
     *
     * @return The elementStyling value
     */
    @Override
    public String getElementStyling(org.w3c.dom.Element e) {
        StringBuilder style = new StringBuilder();
        if (e.getNodeName().equals("td") || e.getNodeName().equals("th")) {
            String s;
            s = getAttribute(e, "colspan");
            if (s != null) {
                style.append("-fs-table-cell-colspan: ");
                style.append(s);
                style.append(";");
            }
            s = getAttribute(e, "rowspan");
            if (s != null) {
                style.append("-fs-table-cell-rowspan: ");
                style.append(s);
                style.append(";");
            }
        } else if (e.getNodeName().equals("img")) {
            String s;
            s = getAttribute(e, "width");
            if (s != null) {
                style.append("width: ");
                style.append(convertToLength(s));
                style.append(";");
            }
            s = getAttribute(e, "height");
            if (s != null) {
                style.append("height: ");
                style.append(convertToLength(s));
                style.append(";");
            }
        } else if (e.getNodeName().equals("colgroup") || e.getNodeName().equals("col")) {
            String s;
            s = getAttribute(e, "span");
            if (s != null) {
                style.append("-fs-table-cell-colspan: ");
                style.append(s);
                style.append(";");
            }
            s = getAttribute(e, "width");
            if (s != null) {
                style.append("width: ");
                style.append(convertToLength(s));
                style.append(";");
            }
        }
        style.append(e.getAttribute("style"));
        return style.toString();
    }

    /**
     * Returns the title of the document as located in the contents of /html/head/title, or "" if none could be found.
     *
     * @param doc the document to search for a title
     * @return The document's title, or "" if none found
     */
    @Override
    public String getDocumentTitle(org.w3c.dom.Document doc) {
        String title = "";

        Element html = doc.getDocumentElement();
        Element head = findFirstChild(html, "head");
        if (head != null) {
            Element titleElem = findFirstChild(head, "title");
            if (titleElem != null) {
                title = collapseWhiteSpace(readTextContent(titleElem).trim());
            }
        }

        return title;
    }

    private Element findFirstChild(Element parent, String targetName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(targetName)) {
                return (Element)n;
            }
        }

        return null;
    }

    protected StylesheetInfo readStyleElement(Element style) {
        StylesheetInfo info = new StylesheetInfo();
        String media = style.getAttribute("media");

        info.setMedia(media);
        info.setType(style.getAttribute("type"));
        info.setTitle(style.getAttribute("title"));
        info.setOrigin(StylesheetInfo.AUTHOR);

        StringBuilder buf = new StringBuilder();
        Node current = style.getFirstChild();
        while (current != null) {
            if (current instanceof CharacterData) {
                buf.append(((CharacterData)current).getData());
            }
            current = current.getNextSibling();
        }

        String css = buf.toString().trim();
        if (css.length() > 0) {
            info.setContent(css);

            return info;
        } else {
            return null;
        }
    }

    protected StylesheetInfo readLinkElement(Element link) {
        String rel = link.getAttribute("rel").toLowerCase();
        if (rel.indexOf("alternate") != -1) {
            return null;
        }//DON'T get alternate stylesheets
        if (rel.indexOf("stylesheet") == -1) {
            return null;
        }

        String type = link.getAttribute("type");
        if (! (type.equals("") || type.equals("text/css"))) {
            return null;
        }

        StylesheetInfo info = new StylesheetInfo();

        if (type.equals("")) {
            type = "text/css";
        } // HACK is not entirely correct because default may be set by META tag or HTTP headers
        info.setType(type);

        info.setOrigin(StylesheetInfo.AUTHOR);
        info.setUri(link.getAttribute("href"));

        String media = link.getAttribute("media");
        info.setMedia(media);

        String title = link.getAttribute("title");
        info.setTitle(title);

        return info;
    }

    /**
     * Gets the stylesheetLinks attribute of the XhtmlNamespaceHandler object
     *
     * @return The stylesheetLinks value
     */
    @Override
    public StylesheetInfo[] getStylesheets(org.w3c.dom.Document doc) {
        List<StylesheetInfo> result = new ArrayList<>();
        //get the processing-instructions (actually for XmlDocuments)
        result.addAll(Arrays.asList(super.getStylesheets(doc)));

        //get the link elements
        Element html = doc.getDocumentElement();
        Element head = findFirstChild(html, "head");
        if (head != null) {
            Node current = head.getFirstChild();
            while (current != null) {
                if (current.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element)current;
                    StylesheetInfo info = null;
                    String elemName = elem.getLocalName();
                    if (elemName == null)
                    {
                        elemName = elem.getTagName();
                    }
                    if (elemName.equals("link")) {
                        info = readLinkElement(elem);
                    } else if (elemName.equals("style")) {
                        info = readStyleElement(elem);
                    }
                    if (info != null) {
                        result.add(info);
                    }
                }
                current = current.getNextSibling();
            }
        }

        return result.toArray(new StylesheetInfo[result.size()]);
    }

    @Override
    public StylesheetInfo getDefaultStylesheet(StylesheetFactory factory) {
        synchronized (XhtmlCssOnlyNamespaceHandler.class) {
            if (_defaultStylesheet != null) {
                return _defaultStylesheet;
            }

            if (_defaultStylesheetError) {
                return null;
            }

            StylesheetInfo info = new StylesheetInfo();
            info.setUri(getNamespace());
            info.setOrigin(StylesheetInfo.USER_AGENT);
            info.setMedia("all");
            info.setType("text/css");

            InputStream is = null;
            try {
                is = getDefaultStylesheetStream();

                if (_defaultStylesheetError) {
                    return null;
                }

                try (Reader reader = new InputStreamReader(is)) {
                    Stylesheet sheet = factory.parse(reader, info);
                    info.setStylesheet(sheet);
                }
            } catch (Exception e) {
                _defaultStylesheetError = true;
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_COULD_NOT_PARSE_DEFAULT_STYLESHEET, e);
            } finally {
                OpenUtil.closeQuietly(is);
                is = null;
            }

            _defaultStylesheet = info;

            return _defaultStylesheet;
        }
    }

    private InputStream getDefaultStylesheetStream() {
        InputStream stream = null;
        String defaultStyleSheet = "/resources/css/XhtmlNamespaceHandler.css";
        stream = this.getClass().getResourceAsStream(defaultStyleSheet);
        if (stream == null) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_DEFAULT_CSS, defaultStyleSheet);
            _defaultStylesheetError = true;
        }

        return stream;
    }

    private Map<String, String> getMetaInfo(org.w3c.dom.Document doc) {
        Map<String, String> metadata = new HashMap<>();

        Element html = doc.getDocumentElement();
        Element head = findFirstChild(html, "head");

        if (head != null) {
            Node current = head.getFirstChild();
            while (current != null) {
                if (current.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element)current;
                    String elemName = elem.getLocalName();
                    if (elemName == null)
                    {
                        elemName = elem.getTagName();
                    }
                    if (elemName.equals("meta")) {
                        String http_equiv = elem.getAttribute("http-equiv");
                        String content = elem.getAttribute("content");

                        if(!http_equiv.equals("") && !content.equals("")) {
                            metadata.put(http_equiv, content);
                        }
                    }
                }
                current = current.getNextSibling();
            }
        }

        return metadata;
    }

    /**
     * Get the Content-Language meta tag value from the head section of the doc
     * or the empty string. Caches value so can be called multiple times without performance
     * issues.
     */
    private String getContentLanguageMetaTag(org.w3c.dom.Document doc) {
        if (this._contentLanguageMetaValue == null) {
            String possible = this.getMetaInfo(doc).get("Content-Language");
            this._contentLanguageMetaValue = possible != null ? possible : "";
        }

        return this._contentLanguageMetaValue;
    }

    /**
     * Gets the language of an element as specified (in order of precedence) by the lang attribute on the element itself,
     * the first ancestor with a lang attribute, the Content-Language meta tag or the empty string.
     */
    @Override
    public String getLang(org.w3c.dom.Element e) {
        String lang = e.getAttribute("lang");

        if (lang.isEmpty()) {
            org.w3c.dom.Node parent = e.getParentNode();

            if (parent instanceof org.w3c.dom.Element) {
                return getLang((org.w3c.dom.Element) parent);
            } else {
                return getContentLanguageMetaTag(e.getOwnerDocument());
            }
        }

        return lang;
    }
}

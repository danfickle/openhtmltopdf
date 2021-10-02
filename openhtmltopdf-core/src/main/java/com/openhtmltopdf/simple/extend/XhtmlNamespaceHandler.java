/*
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
package com.openhtmltopdf.simple.extend;

import java.util.logging.Level;

import com.openhtmltopdf.util.LogMessageId;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.util.XRLog;


/**
 * Handles xhtml documents, including presentational html attributes (see css 2.1 spec, 6.4.4).
 * In this class ONLY handling (css equivalents) of presentational properties
 * (according to css 2.1 spec, section 6.4.4) should be specified.
 *
 * @author Torbjoern Gannholm
 */
public class XhtmlNamespaceHandler extends XhtmlCssOnlyNamespaceHandler {
    private static final String DEFAULT_SVG_DIMS = "";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isImageElement(Element e) {
        return (e != null && e.getNodeName().equalsIgnoreCase("img"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFormElement(Element e) {
        return (e != null && e.getNodeName().equalsIgnoreCase("form"));
    }

    @Override
    public String getImageSourceURI(Element e) {
        return e != null ? e.getAttribute("src") : null;
    }

    @Override
    public String getNonCssStyling(Element e) {
        switch(e.getNodeName()) {
        case "table":
            return applyTableStyles(e);
        case "td": /* FALL-THRU */
        case "th":
            return applyTableCellStyles(e);
        case "tr":
            return applyTableRowStyles(e);
        case "img":
            return applyImgStyles(e);
        case "p": /* FALL-THRU */
        case "div":
            return applyBlockAlign(e);
        case "textarea":
            return applyTextareaStyles(e);
        case "input":
            return applyInputStyles(e);
        case "svg":
            return applySvgStyles(e);
        }
        
        return "";
    }
    
    private String applySvgStyles(Element e) {
        String w = e.getAttribute("width");
        String h = e.getAttribute("height");
        
        if (!w.isEmpty() || !h.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            
            if (!w.isEmpty()) {
                sb.append("width: ");
                sb.append(w);
                if (isInteger(w)) {
                    sb.append("px");
                }
                sb.append(';');
            }
            
            if (!h.isEmpty()) {
                sb.append("height: ");
                sb.append(h);
                if (isInteger(h)) {
                    sb.append("px");
                }
                sb.append(';');
            }
            
            return sb.toString();
        }
        
        String viewBoxAttr = e.getAttribute("viewBox");
        String[] splitViewBox = viewBoxAttr.split("\\s+");
        
        if (splitViewBox.length != 4) {
            return DEFAULT_SVG_DIMS;
        }
        try {
            int viewBoxWidth = Integer.parseInt(splitViewBox[2]);
            int viewBoxHeight = Integer.parseInt(splitViewBox[3]);
            
            StringBuilder sb = new StringBuilder();
            
            sb.append("width: ");
            sb.append(viewBoxWidth);
            sb.append("px;");
            
            sb.append("height: ");
            sb.append(viewBoxHeight);
            sb.append("px;");
        } catch (NumberFormatException ex) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_INVALID_INTEGER_PASSED_IN_VIEWBOX_ATTRIBUTE_FOR_SVG, viewBoxAttr);
            /* FALL-THRU */
        }
        
        return DEFAULT_SVG_DIMS;
    }
    
    private String applyInputStyles(Element e) {
    	StringBuilder sb = new StringBuilder();
    	
    	if (e.hasAttribute("width") && isInteger(e.getAttribute("width"))) {
    		sb.append("width: ");
    		sb.append(e.getAttribute("width"));
    		sb.append("px;");
    	} else if (e.hasAttribute("size") && isInteger(e.getAttribute("size"))) {
    		sb.append("width: ");
    		sb.append(e.getAttribute("size"));
    		sb.append("em;");
    	}
    	
    	return sb.toString();
    }
    
    private String applyTextareaStyles(Element e) {
    	StringBuilder sb = new StringBuilder();
    	
    	if (e.hasAttribute("cols") && isInteger(e.getAttribute("cols"))) {
    		sb.append("width: ");
    		sb.append(e.getAttribute("cols"));
    		sb.append("em;");
    	}

    	if (e.hasAttribute("rows") && isInteger(e.getAttribute("rows"))) {
    		sb.append("height: ");
    		sb.append(e.getAttribute("rows"));
    		sb.append("em;");
    	}

    	return sb.toString();
    }
    
    private String applyBlockAlign(Element e) {
        StringBuilder style = new StringBuilder();
        applyTextAlign(e, style);
        return style.toString();
    }
    
    private String applyImgStyles(Element e) {
        StringBuilder style = new StringBuilder();
        applyFloatingAlign(e, style);
        return style.toString();
    }

    private String applyTableCellStyles(Element e) {
        StringBuilder style = new StringBuilder();
        String s;
        //check for cellpadding
        Element table = findTable(e);
        if (table != null) {
            s = getAttribute(table, "cellpadding");
            if (s != null) {
                style.append("padding: ");
                style.append(convertToLength(s));
                style.append(";");
            }
            s = getAttribute(table, "border");
            if (s != null && ! s.equals("0")) {
                style.append("border: 1px outset black;");
            }
        }
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
        applyTableContentAlign(e, style);
        s = getAttribute(e, "bgcolor");
        if (s != null) {
            s = s.toLowerCase();
            style.append("background-color: ");
            if (looksLikeAMangledColor(s)) {
                style.append('#');
                style.append(s);
            } else {
                style.append(s);
            }
            style.append(';');
        }
        s = getAttribute(e, "background");
        if (s != null) {
            style.append("background-image: url(");
            style.append(s);
            style.append(");");
        }
        return style.toString();
    }

    private String applyTableStyles(Element e) {
        StringBuilder style = new StringBuilder();
        String s;
        s = getAttribute(e, "width");
        if (s != null) {
            style.append("width: ");
            style.append(convertToLength(s));
            style.append(";");
        }
        s = getAttribute(e, "border");
        if (s != null) {
            style.append("border: ");
            style.append(convertToLength(s));
            style.append(" inset black;");
        }
        s = getAttribute(e, "cellspacing");
        if (s != null) {
            style.append("border-collapse: separate; border-spacing: ");
            style.append(convertToLength(s));
            style.append(";");
        }
        s = getAttribute(e, "bgcolor");
        if (s != null) {
            s = s.toLowerCase();
            style.append("background-color: ");
            if (looksLikeAMangledColor(s)) {
                style.append('#');
                style.append(s);
            } else {
                style.append(s);
            }
            style.append(';');
        }
        s = getAttribute(e, "background");
        if (s != null) {
            style.append("background-image: url(");
            style.append(s);
            style.append(");");
        }
        applyFloatingAlign(e, style);
        return style.toString();
    }
    
    private String applyTableRowStyles(Element e) {
        StringBuilder style = new StringBuilder();
        applyTableContentAlign(e, style);
        return style.toString();
    }
    
    private void applyFloatingAlign(Element e, StringBuilder style) {
        String s;
        s = getAttribute(e, "align");
        if (s != null) {
            s = s.toLowerCase().trim();
            if (s.equals("left")) {
                style.append("float: left;");
            } else if (s.equals("right")) {
                style.append("float: right;");
            } else if (s.equals("center")) {
                style.append("margin-left: auto; margin-right: auto;");
            }
        }
    }
    
    private void applyTextAlign(Element e, StringBuilder style) {
        String s;
        s = getAttribute(e, "align");
        if (s != null) {
            s = s.toLowerCase().trim();
            if (s.equals("left") || s.equals("right") || 
                    s.equals("center") || s.equals("justify")) {
                style.append("text-align: ");
                style.append(s);
                style.append(";");
            }
        }
    }
    
    private void applyTableContentAlign(Element e, StringBuilder style) {
        String s;
        s = getAttribute(e, "align");
        if (s != null) {
            style.append("text-align: ");
            style.append(s.toLowerCase());
            style.append(";");
        }
        s = getAttribute(e, "valign");
        if (s != null) {
            style.append("vertical-align: ");
            style.append(s.toLowerCase());
            style.append(";");
        }
    }
    
    private boolean looksLikeAMangledColor(String s) {
        if (s.length() != 6) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean valid = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (! valid) {
                return false;
            }
        }
        return true;
    }
    
    private Element findTable(Element cell) {
        Node n = cell.getParentNode();
        Element next;
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            next = (Element)n;
            if (next.getNodeName().equals("tr")) {
                n = next.getParentNode();
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    next = (Element)n;
                    String name = next.getNodeName();
                    if (name.equals("table")) {
                        return next;
                    }
                    
                    if (name.equals("tbody") || name.equals("tfoot") || name.equals("thead")) {
                        n = next.getParentNode();
                        if (n.getNodeType() == Node.ELEMENT_NODE) {
                            next =(Element)n;
                            if (next.getNodeName().equals("table")) {
                                return next;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
}


/*
 * DOMTreeResolver.java
 * Copyright (c) 2005 Scott Cytacki
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
package com.openhtmltopdf.css.extend.lib;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.openhtmltopdf.css.extend.TreeResolver;

/**
 * @author scott
 *         <br>
 *         works for a w3c DOM tree
 */
public class DOMTreeResolver implements TreeResolver {
    /**
     * We cache element positions for nth-child, odd and even condition matchers.
     * To avoid scaling with the square of the number of children elements.
     */
    private final Map<Node, Map<Node, Integer>> cachedPositions = new HashMap<>();
    
    @Override
    public Object getParentElement(Object element) {
        Node parent = ((org.w3c.dom.Element) element).getParentNode();
        if (parent.getNodeType() != Node.ELEMENT_NODE) parent = null;
        return parent;
    }

    @Override
    public Object getPreviousSiblingElement(Object element) {
        Node sibling = ((Element) element).getPreviousSibling();
        while (sibling != null && sibling.getNodeType() != Node.ELEMENT_NODE) {
            sibling = sibling.getPreviousSibling();
        }
        if (sibling == null || sibling.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }
        return sibling;
    }

    @Override
    public String getElementName(Object element) {
        String name = ((Element) element).getLocalName();
        if (name == null) name = ((Element) element).getNodeName();
        return name;
    }

    @Override
    public boolean isFirstChildElement(Object element) {
        org.w3c.dom.Node parent = ((org.w3c.dom.Element) element).getParentNode();
        Node currentChild = parent.getFirstChild();
        while (currentChild != null && currentChild.getNodeType() != Node.ELEMENT_NODE) {
            currentChild = currentChild.getNextSibling();
        }
        return currentChild == element;
    }

    @Override
    public boolean isLastChildElement(Object element) {
        org.w3c.dom.Node parent = ((org.w3c.dom.Element) element).getParentNode();
        Node currentChild = parent.getLastChild();
        while (currentChild != null && currentChild.getNodeType() != Node.ELEMENT_NODE) {
            currentChild = currentChild.getPreviousSibling();
        }
        return currentChild == element;
    }

    @Override
    public boolean matchesElement(Object element, String namespaceURI, String name) {
        Element e = (Element)element;
        String localName = e.getLocalName();
        String eName;

        if (localName == null) {
            eName = e.getNodeName();
        } else {
            eName = localName;
        }

        if (namespaceURI != null) {
            return name.equals(localName) && namespaceURI.equals(e.getNamespaceURI());
        } else /* if (namespaceURI == null) */ {
            return name.equals(eName);
        }
    }

    @Override
    public int getPositionOfElement(Object element) {
        org.w3c.dom.Node parent = ((org.w3c.dom.Element) element).getParentNode();
        
        Map<Node, Integer> positions = cachedPositions.get(parent);
        
        if (positions == null) {
            NodeList nl = parent.getChildNodes();
            int len = nl.getLength();
            
            positions = new HashMap<>();
            
            int pos = 0;
            for (int i = 0; i < len; i++) {
                Node n = nl.item(i);
                
                if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    positions.put(n, pos++);
                }
            }
            
            cachedPositions.put(parent, positions);
        }

        return positions.get(element);
    }
}

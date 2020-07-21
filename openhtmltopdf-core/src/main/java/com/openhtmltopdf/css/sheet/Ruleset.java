/*
 * Ruleset.java
 * Copyright (c) 2004, 2005 Patrick Wright, Torbjoern Gannholm
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
package com.openhtmltopdf.css.sheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openhtmltopdf.css.newmatch.Selector;


/**
 * @author Torbjoern Gannholm
 * @author Patrick Wright
 */
public class Ruleset {
    private final int _origin;
    private final List<PropertyDeclaration> _props;
    private final List<Selector> _fsSelectors;
    private List<InvalidPropertyDeclaration> _invalidProperties;

    public Ruleset(int orig) {
        _origin = orig;
        _props = new ArrayList<>();
        _fsSelectors = new ArrayList<>();
    }

    /**
     * Returns an Iterator of PropertyDeclarations pulled from this
     * CSSStyleRule.
     *
     * @return The propertyDeclarations value
     */
    public List<PropertyDeclaration>  getPropertyDeclarations() {
        return Collections.unmodifiableList(_props);
    }

    public void addProperty(PropertyDeclaration decl) {
        _props.add(decl);
    }
    
    public void addAllProperties(List<PropertyDeclaration> props) {
        _props.addAll(props);
    }
    
    public void addFSSelector(Selector selector) {
        _fsSelectors.add(selector);
    }
    
    public List<Selector> getFSSelectors() {
        return _fsSelectors;
    }
    
    public int getOrigin() {
        return _origin;
    }

    public void toCSS(StringBuilder sb) {
        List<PropertyDeclaration> decls;

        if (_invalidProperties != null) {
            // Create a list of declarations in their proper order.
            // Not efficient, but there should be few of them.
            decls = new ArrayList<>(_props);
            for (InvalidPropertyDeclaration decl : _invalidProperties) {
                decls.add(decl.getOrder(), decl);
            }
        } else {
            decls = _props;
        }

        sb.append('{');
        sb.append('\n');
        for (PropertyDeclaration decl : decls) {
            decl.toCSS(sb);
            sb.append('\n');
        }
        sb.append('}');
        sb.append('\n');
    }

    public void addInvalidProperty(InvalidPropertyDeclaration invalidPropertyDeclaration) {
        if (_invalidProperties == null) {
            _invalidProperties = new ArrayList<>();
        }
        _invalidProperties.add(invalidPropertyDeclaration);
    }

    public List<InvalidPropertyDeclaration> getInvalidPropertyDeclarations() {
        return _invalidProperties == null ? Collections.emptyList() : _invalidProperties;
    }
}

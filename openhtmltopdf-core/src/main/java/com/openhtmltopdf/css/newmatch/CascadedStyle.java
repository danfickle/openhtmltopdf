/*
 * CascadedStyle.java
 * Copyright (c) 2004, 2005 Patrick Wright, Torbjoern Gannholm
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
 *
 */
package com.openhtmltopdf.css.newmatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.sheet.StylesheetInfo;


/**
 * Holds a set of {@link com.openhtmltopdf.css.sheet.PropertyDeclaration}s for
 * each unique CSS property name. What properties belong in the set is not
 * determined, except that multiple entries are resolved into a single set using
 * cascading rules. The set is cascaded during instantiation, so once you have a
 * CascadedStyle, the PropertyDeclarations you retrieve from it will have been
 * resolved following the CSS cascading rules. Note that this class knows
 * nothing about CSS selector-matching rules. Before creating a CascadedStyle,
 * you will need to determine which PropertyDeclarations belong in the set--for
 * example, by matching {@link com.openhtmltopdf.css.sheet.Ruleset}s to {@link
 * org.w3c.dom.Document} {@link org.w3c.dom.Element}s via their selectors. You
 * can get individual properties by using {@link #propertyByName(CSSName)} or an
 * {@link java.util.Iterator} of properties with {@link
 * #getCascadedPropertyDeclarations()}. Check for individual property assignments
 * using {@link #hasProperty(CSSName)}. A CascadedStyle is immutable, as
 * properties can not be added or removed from it once instantiated.
 *
 * @author Torbjoern Gannholm
 * @author Patrick Wright
 */
public class CascadedStyle {
    /**
     * Map of PropertyDeclarations, keyed by {@link CSSName}
     */
    private final Map<CSSName, PropertyDeclaration> cascadedProperties;

    private String fingerprint;
    
    /**
     * Constructs a new CascadedStyle, given an {@link java.util.Iterator} of
     * {@link com.openhtmltopdf.css.sheet.PropertyDeclaration}s already sorted
     * by specificity of the CSS selector they came from. The Iterator can have
     * multiple PropertyDeclarations with the same name; the property cascade
     * will be resolved during instantiation, resulting in a set of
     * PropertyDeclarations. Once instantiated, properties may be retrieved
     * using the normal API for the class.
     *
     * @param iter An Iterator containing PropertyDeclarations in order of
     *             specificity.
     */
    CascadedStyle(java.util.Iterator<PropertyDeclaration> iter) {
        this();

        addProperties(iter);
    }
    
    /**
     * Creates a <code>CascadedStyle</code> using the provided property
     * declarations.  It is used when a box requires a style that does not
     * correspond to anything in the parsed stylesheets.
     * @param decls An array of PropertyDeclaration objects created with 
     * {@link #createLayoutPropertyDeclaration(CSSName, IdentValue)}
     * @see #createLayoutPropertyDeclaration(CSSName, IdentValue)
     */
    public static CascadedStyle createLayoutStyle(PropertyDeclaration[] decls) {
        return new CascadedStyle(Arrays.asList(decls).iterator());
    }
    
    public static CascadedStyle createLayoutStyle(List<PropertyDeclaration> decls) {
        return new CascadedStyle(decls.iterator());
    }    
    
    /**
     * Creates a <code>CascadedStyle</code> using style information from
     * <code>startingPoint</code> and then adding the property declarations
     * from <code>decls</code>.
     * @param decls An array of PropertyDeclaration objects created with 
     * {@link #createLayoutPropertyDeclaration(CSSName, IdentValue)}
     * @see #createLayoutPropertyDeclaration(CSSName, IdentValue)
     */
    public static CascadedStyle createLayoutStyle(
            CascadedStyle startingPoint, PropertyDeclaration[] decls) {
        return new CascadedStyle(startingPoint, Arrays.asList(decls).iterator());
    }

    /**
     * Creates a <code>PropertyDeclaration</code> suitable for passing to
     * {@link #createLayoutStyle(PropertyDeclaration[])} or
     * {@link #createLayoutStyle(CascadedStyle, PropertyDeclaration[])}
     */
    public static PropertyDeclaration createLayoutPropertyDeclaration(
            CSSName cssName, IdentValue display) {
        CSSPrimitiveValue val = new PropertyValue(display);
        // Urk... kind of ugly, but we really want this value to be used
        return new PropertyDeclaration(cssName, val, true, StylesheetInfo.USER);
    }

    private CascadedStyle(CascadedStyle startingPoint, Iterator<PropertyDeclaration> props) {
        cascadedProperties = new TreeMap<>(startingPoint.cascadedProperties);

        addProperties(props);
    }

    /**
     * Default constructor with no initialization. Don't use this to instantiate
     * the class, as the class is immutable and this will leave it without any
     * properties.
     */
    private CascadedStyle() {
        cascadedProperties = new TreeMap<>();
    }

    /**
     * Creates an otherwise empty <code>CascadedStyle</code>, setting the display property
     * to the value of the <code>display</code> parameter.
     */
    public static CascadedStyle createAnonymousStyle(IdentValue display) {
        CSSPrimitiveValue val = new PropertyValue(display);

        List<PropertyDeclaration> props = Collections.singletonList(
                new PropertyDeclaration(CSSName.DISPLAY, val, true, StylesheetInfo.USER));

        return new CascadedStyle(props.iterator());
    }

    private void addProperties(java.util.Iterator<PropertyDeclaration> iter) {
		/*
		 * do a bucket-sort on importance and origin /properties should already be in
		 * order of specificity
		 */
        //noinspection unchecked
        @SuppressWarnings("unchecked")
        java.util.List<PropertyDeclaration>[] buckets =  new java.util.List[PropertyDeclaration.IMPORTANCE_AND_ORIGIN_COUNT];

        while (iter.hasNext()) {
            PropertyDeclaration prop = iter.next();
            List<PropertyDeclaration> bucket = buckets[prop.getImportanceAndOrigin()];
			if (bucket == null) {
				bucket = new ArrayList<>();
				buckets[prop.getImportanceAndOrigin()]  = bucket;
			}
            bucket.add(prop);
        }

        for (List<PropertyDeclaration> bucket : buckets) {
			if (bucket == null)
				continue;
            for (PropertyDeclaration prop : bucket) {
                cascadedProperties.put(prop.getCSSName(), prop);
            }
        }
    }

    /**
     * Get an empty singleton, used to negate inheritance of properties
     */
    public static final CascadedStyle emptyCascadedStyle = new CascadedStyle();


    /**
     * Returns true if property has been defined in this style.
     *
     * @param cssName The CSS property name, e.g. "font-family".
     * @return True if the property is defined in this set.
     */
    public boolean hasProperty(CSSName cssName) {
        return cascadedProperties.containsKey(cssName);
    }

    /**
     * Returns a {@link com.openhtmltopdf.css.sheet.PropertyDeclaration} by CSS
     * property name, e.g. "font-family". Properties are already cascaded during
     * instantiation, so this will return the actual property (and corresponding
     * value) to use for CSS-based layout and rendering.
     *
     * @param cssName The CSS property name, e.g. "font-family".
     * @return The PropertyDeclaration, if declared in this set, or null
     *         if not found.
     */
    public PropertyDeclaration propertyByName(CSSName cssName) {
        return cascadedProperties.get(cssName);
    }

    /**
     * Gets the ident attribute of the CascadedStyle object
     *
     * @param cssName PARAM
     * @return The ident value
     */
    public IdentValue getIdent(CSSName cssName) {
        PropertyDeclaration pd = propertyByName(cssName);
        return (pd == null ? null : pd.asIdentValue());
    }


    /**
     * Returns an {@link java.util.Iterator} over the set of {@link
     * com.openhtmltopdf.css.sheet.PropertyDeclaration}s already matched in this
     * CascadedStyle. For a given property name, there may be no match, in which
     * case there will be no <code>PropertyDeclaration</code> for that property
     * name in the Iterator.
     *
     * @return Iterator over a set of properly cascaded PropertyDeclarations.
     */
    public java.util.Collection<PropertyDeclaration> getCascadedPropertyDeclarations() {
        return cascadedProperties.values();
    }

    public int countAssigned() { return cascadedProperties.size(); }

    public String getFingerprint() {
        if (this.fingerprint == null) {
            StringBuilder sb = new StringBuilder();
            for (PropertyDeclaration o : cascadedProperties.values()) {
                sb.append(o.getFingerprint());
            }
            this.fingerprint = sb.toString();
        }
        return this.fingerprint;
    }
}

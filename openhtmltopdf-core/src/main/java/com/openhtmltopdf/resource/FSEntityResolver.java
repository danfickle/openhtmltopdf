/*
 * {{{ header & license
 * FSEntityResolver.java
 * Copyright (c) 2004, 2005 Patrick Wright
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
 * }}}
 */
package com.openhtmltopdf.resource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.openhtmltopdf.util.GeneralUtil;
import com.openhtmltopdf.util.XRLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * <p>
 * A SAX EntityResolver for common entity references and DTDs in X/HTML
 * processing. Maps official entity references to local copies to avoid network
 * lookup. The local copies are stored in the source tree under /entities, and
 * the references here are resolved by a system ClassLoader. As long as the
 * entity files are in the classpath (or bundled in the FS jar), they will be
 * picked up.
 * </p>
 * <p>
 * The basic form of this class comes from Elliot Rusty Harold, on
 * http://www.cafeconleche.org/books/xmljava/chapters/ch07s02.html
 * </p>
 * <p>
 * This class is a Singleton; use {@link #instance} to retrieve it.
 * </p>
 *
 * @author Patrick Wright
 */
public class FSEntityResolver implements EntityResolver {
    /**
     * Singleton instance, use {@link #instance()} to retrieve.
     */
    private static final FSEntityResolver instance = new FSEntityResolver();

    private final Map<String, String> entities = new HashMap<String, String>();
    
    /**
     * Constructor for the FSEntityResolver object, fill the map of public ids to local urls.
     */
    private FSEntityResolver() {
        FSCatalog catalog = new FSCatalog();
        
        entities.putAll(catalog.parseCatalog("resources/schema/openhtmltopdf/catalog-special.xml"));
    }

    @Override
    public InputSource resolveEntity(String publicID,
                                     String systemID)
            throws SAXException {
        InputSource local = null;
        String url = getEntities().get(publicID);

        if (url != null) {
            URL realUrl = GeneralUtil.getURLFromClasspath(this, url);
            InputStream is = null;
            try {
                is = realUrl.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (is == null) {
                XRLog.xmlEntities(Level.WARNING,
                        "Can't find a local reference for Entity for public ID: " + publicID +
                        " and expected to. The local URL should be: " + url + ". Not finding " +
                        "this probably means a CLASSPATH configuration problem; this resource " +
                        "should be included with the renderer and so not finding it means it is " +
                        "not on the CLASSPATH, and should be. Will let parser use the default in " +
                        "this case.");
            }
            local = new InputSource(is);
            local.setSystemId(realUrl.toExternalForm());
            XRLog.xmlEntities(Level.FINE, "Entity public: " + publicID + " -> " + url +
                    (local == null ? ", NOT FOUND" : " (local)"));
        } else {
            XRLog.xmlEntities("Entity public: " + publicID + ", no local mapping. Returning empty entity to avoid pulling from network.");
            local = new InputSource(new StringReader(""));
        }
        return local;
    }

    /**
     * Gets an instance of this class.
     *
     * @return An instance of FSEntityResolver.
     */
    public static FSEntityResolver instance() {
        return instance;
    }

    /**
     * Returns an unmodifiable map of entities parsed by this resolver.
     * @return an unmodifiable map of entities parsed by this resolver. 
     */
    public Map<String, String> getEntities() {
        return Collections.unmodifiableMap(entities);
    }
}

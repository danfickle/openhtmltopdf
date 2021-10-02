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

import com.openhtmltopdf.util.LogMessageId;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

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

    private final Map<String, String> entities = new HashMap<>();
    
    /**
     * Constructor for the FSEntityResolver object, fill the map of public ids to local urls.
     */
    private FSEntityResolver() {
        FSCatalog catalog = new FSCatalog();
        
        entities.putAll(catalog.parseCatalog("/resources/schema/openhtmltopdf/catalog-special.xml"));
    }

    @Override
    public InputSource resolveEntity(String publicID,
                                     String systemID) {
        InputSource local = null;
        String url = getEntities().get(publicID);

        if (url != null) {
            URL realUrl = FSEntityResolver.class.getResource(url);
            InputStream is = null;
            try {
                is = realUrl.openStream();
            } catch (IOException e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_IO_PROBLEM_FOR_URI, url, e);
            }

            if (is == null) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.XML_ENTITIES_ENTITY_CANT_FIND_LOCAL_REFERENCE, publicID, url);
            }
            local = new InputSource(is);
            local.setSystemId(realUrl.toExternalForm());
            XRLog.log(Level.FINE, LogMessageId.LogMessageId2Param.XML_ENTITIES_ENTITY_PUBLIC_NOT_FOUND_OR_LOCAL, publicID, url + (local == null ? ", NOT FOUND" : " (local)"));
        } else {
            XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.XML_ENTITIES_ENTITY_PUBLIC_NO_LOCAL_MAPPING, publicID);
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

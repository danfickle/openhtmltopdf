/*
 * {{{ header & license
 * FSCatalog.java
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
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRRuntimeException;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * <p>FSCatalog loads an XML catalog file to read mappings of public IDs for
 * XML schemas/dtds, to resolve those mappings to a local store for the schemas.
 * The catalog file allows one to have a single mapping of schema IDs to local
 * files, and is useful when there are many schemas, or when schemas are broken
 * into many smaller files. Currently FSCatalog only supports the very simple
 * mapping of public id to local URI using the public element in the catalog XML.
 * </p>
 * <p>FSCatalog is not an EntityResolver; it only parses a catalog file. See
 * {@link FSEntityResolver} for entity resolution.
 * </p>
 * <p>To use, instantiate the class, and call {@link #parseCatalog(InputSource)}
 * to retrieve a {@link java.util.Map} keyed by public ids. The class uses
 * an XMLReader instance retrieved via {@link XMLResource#newXMLReader()}, so
 * XMLReader configuration (and specification) follows that of the standard XML
 * parsing in Flying Saucer.
 * </p>
 * <p>This class is not safe for multi-threaded access.</p>
 *
 * @author Patrick Wright
 */
public class FSCatalog {
    public FSCatalog() {
    }

    /**
     * Parses an XML catalog file and returns a Map of public ids to local URIs read
     * from the catalog. Only the catalog public elements are parsed.
     *
     * @param catalogURI A String URI to a catalog XML file on the classpath.
     */
    public Map<String, String> parseCatalog(String catalogURI) {
        Map<String, String> map = null;
        try (InputStream in = FSCatalog.class.getResourceAsStream(catalogURI)){
            map = parseCatalog(new InputSource(new BufferedInputStream(in)));
        } catch (Exception ex) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.XML_ENTITIES_COULD_NOT_OPEN_XML_CATALOG_FROM_URI, catalogURI, ex);
            map = Collections.emptyMap();
        }
        return map;
    }

    /**
     * Parses an XML catalog file and returns a Map of public ids to local URIs read
     * from the catalog. Only the catalog public elements are parsed.
     *
     * @param inputSource A SAX InputSource to a catalog XML file on the classpath.
     */
    public Map<String, String> parseCatalog(InputSource inputSource) {
        XMLReader xmlReader = XMLResource.newXMLReader();

        CatalogContentHandler ch = new CatalogContentHandler();
        addHandlers(xmlReader, ch);
        setFeature(xmlReader, "http://xml.org/sax/features/validation", false);

        try {
            xmlReader.parse(inputSource);
        } catch (Exception ex) {
            throw new RuntimeException("Failed on configuring SAX to DOM transformer.", ex);
        }

        return ch.getEntityMap();
    }

    /**
     * Adds the default EntityResolved and ErrorHandler for the SAX parser.
     */
    private void addHandlers(XMLReader xmlReader, ContentHandler ch) {
        try {
            // add our own entity resolver
            xmlReader.setContentHandler(ch);
            xmlReader.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException ex) {
                    if (XRLog.isLoggingEnabled()) {
                        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.XML_ENTITIES_EXCEPTION_MESSAGE, ex.getMessage());
                    }
                }

                public void fatalError(SAXParseException ex) {
                    if (XRLog.isLoggingEnabled()) {
                        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.XML_ENTITIES_EXCEPTION_MESSAGE, ex.getMessage());
                    }
                }

                public void warning(SAXParseException ex) {
                    if (XRLog.isLoggingEnabled()) {
                        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.XML_ENTITIES_EXCEPTION_MESSAGE, ex.getMessage());
                    }
                }
            });
        } catch (Exception ex) {
            throw new XRRuntimeException("Failed on configuring SAX parser/XMLReader.", ex);
        }
    }

    /**
     * A SAX ContentHandler that reads an XML catalog file and builds a Map of
     * public IDs to local URIs. Currently only handles the <public> element and attributes.
     * To use, just call XMLReader.setContentHandler() with an instance of the class,
     * parse, then call getEntityMap().
     */
    private static class CatalogContentHandler extends DefaultHandler {
        private final Map<String, String> entityMap;

        public CatalogContentHandler() {
            this.entityMap = new HashMap<>();
        }

        /**
         * Returns a Map of public Ids to local URIs
         */
        public Map<String, String> getEntityMap() {
            return entityMap;
        }

        /**
         * Receive notification of the beginning of an element; here used to pick up the mappings
         * for public IDs to local URIs in the catalog.
         */
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            if (localName.equalsIgnoreCase("public") ||
                    (localName.equals("") && qName.equalsIgnoreCase("public"))) {
                entityMap.put(atts.getValue("publicId"), atts.getValue("uri"));
            }
        }
    }

    /**
     * Attempts to set requested feature on the parser; logs exception if not supported
     * or not recognized.
     */
    private void setFeature(XMLReader xmlReader, String featureUri, boolean value) {
        try {
            xmlReader.setFeature(featureUri, value);
            XRLog.log(Level.FINE, LogMessageId.LogMessageId2Param.XML_ENTITIES_SAX_FEATURE_SET, featureUri.substring(featureUri.lastIndexOf("/")), Boolean.toString(xmlReader.getFeature(featureUri)));
        } catch (SAXNotSupportedException ex) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.XML_ENTITIES_SAX_FEATURE_NOT_SUPPORTED, featureUri);
        } catch (SAXNotRecognizedException ex) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.XML_ENTITIES_SAX_FEATURE_NOT_RECOGNIZED, featureUri);
        }
    }
}

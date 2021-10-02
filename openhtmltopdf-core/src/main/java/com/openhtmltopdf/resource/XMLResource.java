/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Who?
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import com.openhtmltopdf.util.*;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;


/**
 * @author Patrick Wright
 */
public class XMLResource extends AbstractResource {
    private Document document;
    private static final XMLResourceBuilder XML_RESOURCE_BUILDER;

    static {
        XML_RESOURCE_BUILDER = new XMLResourceBuilder();
    }

    private XMLResource(InputStream stream) {
        super(stream);
    }

    private XMLResource(InputSource source) {
        super(source);
    }

    public static XMLResource load(InputStream stream) {
        try (XMLResource resource = new XMLResource(stream)) {
            return XML_RESOURCE_BUILDER.createXMLResource(resource);
        } catch (IOException e) {
            // Thrown on close failure.
            return null;
        }
    }

    public static XMLResource load(InputSource source) {
        try (XMLResource resource = new XMLResource(source)) {
            return XML_RESOURCE_BUILDER.createXMLResource(resource);
        } catch (IOException e) {
            // Thrown on close failure.
            return null;
        }
    }

    public static XMLResource load(Reader reader) {
        try (XMLResource resource = new XMLResource(new InputSource(reader))) {
            return XML_RESOURCE_BUILDER.createXMLResource(resource);
        } catch (IOException e) {
            // Thrown on close failure.
            return null;
        }
    }

    public Document getDocument() {
        return document;
    }

    /*package*/ void setDocument(Document document) {
        this.document = document;
    }

    public static final XMLReader newXMLReader() {
        XMLReader xmlReader = null;

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            setSaxParserRequestedFeatures(factory);

            SAXParser parser = factory.newSAXParser();
            xmlReader = parser.getXMLReader();
        } catch (Exception ex) {
            XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.GENERAL_MESSAGE, ex.getMessage());
        }

        if (xmlReader == null) {
            throw new XRRuntimeException("Could not instantiate any SAX 2 parser, including JDK default. " +
                    "The name of the class to use may have been read from the 'javax.xml.parsers.SAXParserFactory' System " +
                    "property, which is set to: " + System.getProperty("javax.xml.parsers.SAXParserFactory"));
        }

        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_SAX_XMLREADER_IN_USE, xmlReader.getClass().getName());

        return xmlReader;
    }

    @FunctionalInterface
    interface SetFeature<T> {
        void setFeature(String feature, T set) throws Exception;
    }

    private static <T> boolean trySetFeature(
            String feature,
            T value,
            SetFeature<T> tryer) {
        try {
            tryer.setFeature(feature, value);
            return true;
        } catch (Exception e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.XML_FEATURE_NOT_ABLE_TO_SET, feature, value, e);
            return false;
        }
    }

    private static void setSaxParserRequestedFeatures(SAXParserFactory factory) {
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        boolean b = true;

        b &= trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", false, factory::setFeature);
        b &= trySetFeature("http://xml.org/sax/features/external-general-entities", false, factory::setFeature);
        b &= trySetFeature("http://xml.org/sax/features/external-parameter-entities", false, factory::setFeature);
        b &= trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true, factory::setFeature);
        b &= trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true, factory::setFeature);

        if (!b) {
            XRLog.log(Level.SEVERE, LogMessageId.LogMessageId0Param.LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES);
        }
    }

    private static class XMLResourceBuilder {
        private void setXmlReaderSecurityFeatures(XMLReader xmlReader) {
            boolean b = true;

            // VERY IMPORTANT: Without these lines, users can pull in arbitary files from the system using XXE.
            // DO NOT REMOVE!
            b &= trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", false, xmlReader::setFeature);
            b &= trySetFeature("http://xml.org/sax/features/external-general-entities", false, xmlReader::setFeature);
            b &= trySetFeature("http://xml.org/sax/features/external-parameter-entities", false, xmlReader::setFeature);
            b &= trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", true, xmlReader::setFeature);
            b &= trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true, xmlReader::setFeature);

            if (!b) {
                XRLog.log(Level.SEVERE, LogMessageId.LogMessageId0Param.LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES);
            }
        }

        private void setDocumentBuilderSecurityFeatures(DocumentBuilderFactory dbf) {
            boolean b = true;

            // VERY IMPORTANT: Without these lines, users can pull in arbitary files from the system using XXE.
            // DO NOT REMOVE!
            b &= trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", false, dbf::setFeature);
            b &= trySetFeature("http://xml.org/sax/features/external-general-entities", false, dbf::setFeature);
            b &= trySetFeature("http://xml.org/sax/features/external-parameter-entities", false, dbf::setFeature);
            b &= trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false, dbf::setFeature);
            b &= trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true, dbf::setFeature);

            if (!b) {
                XRLog.log(Level.SEVERE, LogMessageId.LogMessageId0Param.LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES);
            }
        }

        private void setTranformerFactorySecurityFeatures(TransformerFactory xformFactory) {
            boolean b = true;

            b &= trySetFeature(XMLConstants.ACCESS_EXTERNAL_DTD, "", xformFactory::setAttribute);
            b &= trySetFeature(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "", xformFactory::setAttribute);

            if (!b) {
                XRLog.log(Level.SEVERE, LogMessageId.LogMessageId0Param.LOAD_UNABLE_TO_DISABLE_XML_EXTERNAL_ENTITIES);
            }
        }

    	private TransformerFactory loadPreferredTransformerFactory(String preferredImpl) {
            try {
            	return TransformerFactory.newInstance(preferredImpl, null);
            } catch (TransformerFactoryConfigurationError e) {
                XRLog.log(Level.SEVERE, LogMessageId.LogMessageId1Param.LOAD_COULD_NOT_LOAD_PREFERRED_XML, "transformer");
            	return TransformerFactory.newInstance();
            }
    	}
    	
    	private DocumentBuilderFactory loadPreferredDocumentBuilderFactory(String preferredImpl) {
            try {
            	return preferredImpl == null ? DocumentBuilderFactory.newInstance() : DocumentBuilderFactory.newInstance(preferredImpl, null);
            } catch (FactoryConfigurationError e) {
                XRLog.log(Level.SEVERE, LogMessageId.LogMessageId1Param.LOAD_COULD_NOT_LOAD_PREFERRED_XML, "document builder");
            	return DocumentBuilderFactory.newInstance();
            }
    	}

    	private XMLResource createXMLResource(XMLResource target) {
            Source input = null;
            DOMResult output = null;
            TransformerFactory xformFactory = null;
            Transformer idTransform = null;
            XMLReader xmlReader = null;
            long st = 0L;

            xmlReader = XMLResource.newXMLReader();

            setXmlReaderSecurityFeatures(xmlReader);
            addHandlers(xmlReader);
            setParserFeatures(xmlReader);

            st = System.currentTimeMillis();
            try {
                input = new SAXSource(xmlReader, target.getResourceInputSource());
                
                String preferredDocumentBuilderFactory = ThreadCtx.get().sharedContext()._preferredDocumentBuilderFactoryImplementationClass;
                DocumentBuilderFactory dbf = loadPreferredDocumentBuilderFactory(preferredDocumentBuilderFactory);
                
                setDocumentBuilderSecurityFeatures(dbf);
                dbf.setNamespaceAware(true);
                dbf.setValidating(false); // validation is the root of all evil in xml - tobe
                
                output = new DOMResult(dbf.newDocumentBuilder().newDocument());
                
                String preferredTransformerFactory = ThreadCtx.get().sharedContext()._preferredTransformerFactoryImplementationClass;
                
                if (preferredTransformerFactory == null) {
                	xformFactory = TransformerFactory.newInstance();
                } else {
                	xformFactory = loadPreferredTransformerFactory(preferredTransformerFactory);
                }
                
                setTranformerFactorySecurityFeatures(xformFactory);
                idTransform = xformFactory.newTransformer();
                
            } catch (Exception ex) {
                throw new XRRuntimeException(
                        "Failed on configuring SAX to DOM transformer.", ex);
            }

            try {
                idTransform.transform(input, output);
            } catch (Exception ex) {
                throw new XRRuntimeException(
                        "Can't load the XML resource (using TRaX transformer). " + ex.getMessage(), ex);
            }

            long end = System.currentTimeMillis();

            target.setElapsedLoadTime(end - st);

            XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_LOADED_DOCUMENT_TIME, target.getElapsedLoadTime());

            target.setDocument((Document) output.getNode());
            return target;
        }

        /**
         * Adds the default EntityResolved and ErrorHandler for the SAX parser.
         */
        private void addHandlers(XMLReader xmlReader) {
            try {
                // add our own entity resolver
                xmlReader.setEntityResolver(FSEntityResolver.instance());
                xmlReader.setErrorHandler(new ErrorHandler() {

                    public void error(SAXParseException ex) {
                        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_EXCEPTION_MESSAGE, ex.getMessage());
                    }

                    public void fatalError(SAXParseException ex) {
                        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_EXCEPTION_MESSAGE, ex.getMessage());
                    }

                    public void warning(SAXParseException ex) {
                        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_EXCEPTION_MESSAGE, ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                throw new XRRuntimeException("Failed on configuring SAX parser/XMLReader.", ex);
            }
        }

        /**
         * Sets all standard features for SAX parser.
         */
        private void setParserFeatures(XMLReader xmlReader) {
            boolean b = true;

            b &= trySetFeature("http://xml.org/sax/features/validation", false, xmlReader::setFeature);
            b &= trySetFeature("http://xml.org/sax/features/namespaces", true, xmlReader::setFeature);

            if (!b) {
                // nothing to do--some parsers will not allow setting features
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.LOAD_COULD_NOT_SET_VALIDATION_NAMESPACE_FEATURES_FOR_XML_PARSER);
            }
        }
    }
}

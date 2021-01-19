/*
 * UserAgentCallback.java
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
package com.openhtmltopdf.extend;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.resource.CSSResource;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.resource.XMLResource;


/**
 * <p>To be implemented by any user agent using the panel. "User agent" is a
 * term defined by the W3C in the documentation for XHTML and CSS; in most
 * cases, you can think of this as the rendering component for a browser.</p>
 *
 * <p>This interface defines a simple callback mechanism for Flying Saucer to
 * interact with a user agent. The FS toolkit provides a default implementation
 * for this interface which in most cases you can leave as is. </p>
 *
 * <p>The user agent in this case is responsible for retrieving external resources. For
 * privacy reasons, if using the library in an application that can access URIs
 * in an unrestricted fashion, you may decide to restrict access to XML, CSS or images
 * retrieved from external sources; that's one of the purposes of the UAC.</p>
 *
 * <p>To understand how to create your own UAC, it's best to look at some of the
 * implemetations shipped with the library, like the {@link com.openhtmltopdf.swing.NaiveUserAgent}.
 * </p>
 *
 * @author Torbjoern Gannholm
 */
public interface UserAgentCallback {
    /**
     * Retrieves the CSS at the given URI. This is a synchronous call.
     *
     * @param uri Location of the CSS
     * @return A CSSResource for the content at the URI.
     */
    default CSSResource getCSSResource(String uri) {
        return getCSSResource(uri, ExternalResourceType.CSS);
    }

    CSSResource getCSSResource(String uri, ExternalResourceType type);

    /**
     * Retrieves the Image at the given URI. This is a synchronous call.
     *
     * @param uri Location of the image
     * @return An ImageResource for the content at the URI.
     */
    default ImageResource getImageResource(String uri) {
        return getImageResource(uri, ExternalResourceType.IMAGE_RASTER);
    }

    ImageResource getImageResource(String uri, ExternalResourceType type);


    /**
     * @deprecated
     * Use {@link #getXMLResource(String, ExternalResourceType)} instead.
     */
    @Deprecated
    default XMLResource getXMLResource(String uri) {
        return getXMLResource(uri, ExternalResourceType.XML_XHTML);
    }

    /**
     * Retrieves the XML at the given URI. This is a synchronous call.
     *
     * @param uri Location of the XML
     * @param type Either xhtml or svg.
     * @return A XMLResource for the content at the URI.
     */
    XMLResource getXMLResource(String uri, ExternalResourceType type);

    /**
     * @deprecated
     * Use {@link #getBinaryResource(String, ExternalResourceType)} instead.
     */
    @Deprecated
    default byte[] getBinaryResource(String uri) {
        return getBinaryResource(uri, ExternalResourceType.BINARY);
    }

    /**
     * Retrieves a binary resource located at a given URI and returns its contents
     * as a byte array or <code>null</code> if the resource could not be loaded.
     */
    byte[] getBinaryResource(String uri, ExternalResourceType type);

    /**
     * Normally, returns true if the user agent has visited this URI. UserAgent should consider
     * if it should answer truthfully or not for privacy reasons.
     *  
     * @param uri A URI which may have been visited by this user agent.
     * @return The visited value
     */
    boolean isVisited(String uri);

    /**
     * Does not need to be a correct URL, only an identifier that the
     * implementation can resolve.
     *
     * @param url A URL against which relative references can be resolved.
     */
    void setBaseURL(String url);

    /**
     * @return the base uri, possibly in the implementations private uri-space
     */
    String getBaseURL();

    /**
     * Used to find a uri that may be relative to the BaseURL.
     * The returned value will always only be used via methods in the same
     * implementation of this interface, therefore may be a private uri-space.
     *
     * @param uri an absolute or relative (to baseURL) uri to be resolved.
     * @return the full uri in uri-spaces known to the current implementation.
     */
    String resolveURI(String uri);

	String resolveUri(String baseUri, String uri);
}


/*
 * DelegatingUserAgent.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
package com.openhtmltopdf.swing;

import com.openhtmltopdf.resource.ImageResource;

public class DelegatingUserAgent extends NaiveUserAgent {
    private UriResolver _uriResolver;
    private ImageResourceLoader _imageResourceLoader;

    /**
     * Creates a new instance of NaiveUserAgent with a max image cache of 16 images.
     */
    public DelegatingUserAgent() {
        this._uriResolver = new UriResolver();
    }

    public void setImageResourceLoader(ImageResourceLoader loader) {
        _imageResourceLoader = loader;
    }

    /**
     * If the image cache has more items than the limit specified for this class, the least-recently used will
     * be dropped from cache until it reaches the desired size.
     */
    public void shrinkImageCache() {
        _imageResourceLoader.shrink();
    }

    /**
     * Empties the image cache entirely.
     */
    @Override
    public void clearImageCache() {
        _imageResourceLoader.clear();
    }
    
    /**
     * Retrieves the image located at the given URI. It's assumed the URI does point to an image--the URI will
     * be accessed (using java.io or java.net), opened, read and then passed into the JDK image-parsing routines.
     * The result is packed up into an ImageResource for later consumption.
     *
     * @param uri Location of the image source.
     * @return An ImageResource containing the image.
     */
    @Override
    public ImageResource getImageResource(String uri) {
        return _imageResourceLoader.get(resolveURI(uri));
    }

    /**
     * URL relative to which URIs are resolved.
     *
     * @param uri A URI which anchors other, possibly relative URIs.
     */
    @Override
    public void setBaseURL(String uri) {
        _uriResolver.setBaseUri(uri);
    }

    /**
     * Resolves the URI; if absolute, leaves as is, if relative, returns an absolute URI based on the baseUrl for
     * the agent.
     *
     * @param uri A URI, possibly relative.
     * @return A URI as String, resolved, or null if there was an exception (for example if the URI is malformed).
     */
    @Override
    public String resolveURI(String uri) {
        return _uriResolver.resolve(uri);
    }

    /**
     * Returns the current baseUrl for this class.
     */
    @Override
    public String getBaseURL() {
        return _uriResolver.getBaseUri();
    }

    @Override
    public void documentStarted() {
        _imageResourceLoader.stopLoading();
        shrinkImageCache();
    }

    @Override
    public void documentLoaded() { /* ignore*/ }

    @Override
    public void onLayoutException(Throwable t) { /* ignore*/ }

    @Override
    public void onRenderException(Throwable t) { /* ignore*/ }

    public void setRepaintListener(RepaintListener listener) {
        //_imageResourceLoader.setRepaintListener(listener);
    }
}

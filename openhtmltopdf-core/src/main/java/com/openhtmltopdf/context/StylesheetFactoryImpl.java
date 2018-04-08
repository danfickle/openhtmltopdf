/*
 * StylesheetFactoryImpl.java
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
package com.openhtmltopdf.context;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;

import com.openhtmltopdf.css.extend.StylesheetFactory;
import com.openhtmltopdf.css.parser.CSSErrorHandler;
import com.openhtmltopdf.css.parser.CSSParser;
import com.openhtmltopdf.css.sheet.Ruleset;
import com.openhtmltopdf.css.sheet.Stylesheet;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.resource.CSSResource;
import com.openhtmltopdf.util.XRLog;

/**
 * A Factory class for Cascading Style Sheets. Sheets are parsed using a single
 * parser instance for all sheets. Sheets are cached by URI using a LRU test,
 * but timestamp of file is not checked.
 *
 * @author Torbjoern Gannholm
 */
public class StylesheetFactoryImpl implements StylesheetFactory {
    /**
     * the UserAgentCallback to resolve uris
     */
    private UserAgentCallback _userAgentCallback;

    private final int _cacheCapacity = 16;

    /**
     * an LRU cache
     */
    private final java.util.LinkedHashMap<String, Stylesheet> _cache =
            new java.util.LinkedHashMap<String, Stylesheet>(_cacheCapacity, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                protected boolean removeEldestEntry(java.util.Map.Entry<String, Stylesheet> eldest) {
                    return size() > _cacheCapacity;
                }
            };
            
    private CSSParser _cssParser;

    public StylesheetFactoryImpl(UserAgentCallback userAgentCallback) {
        _userAgentCallback = userAgentCallback;
        _cssParser = new CSSParser(new CSSErrorHandler() {
            public void error(String uri, String message) {
                XRLog.cssParse(Level.WARNING, "(" + uri + ") " + message);
            }
        });
    }

    public Stylesheet parse(Reader reader, StylesheetInfo info) {
        try {
            return _cssParser.parseStylesheet(info.getUri(), info.getOrigin(), reader);
        } catch (IOException e) {
            XRLog.cssParse(Level.WARNING, "Couldn't parse stylesheet at URI " + info.getUri() + ": " + e.getMessage(), e);
            return new Stylesheet(info.getUri(), info.getOrigin());
        }
    }

    /**
     * @return Returns null if uri could not be loaded
     */
    private Stylesheet parse(StylesheetInfo info) {
        CSSResource cr = _userAgentCallback.getCSSResource(info.getUri());
        if (cr == null) {
        	return null;
        }
        
        Reader reader = cr.getResourceReader();
        if (reader == null) {
        	return null;
        }
        
        try {
            return parse(reader, info);
        } finally {
            try {
               reader.close();
            } catch (IOException e) {
               // ignore
            }
        }
    }

    public Ruleset parseStyleDeclaration(int origin, String styleDeclaration) {
        return _cssParser.parseDeclaration(origin, styleDeclaration);
    }

    /**
     * Adds a stylesheet to the factory cache. Will overwrite older entry for
     * same key.
     *
     * @param key   Key to use to reference sheet later; must be unique in
     *              factory.
     * @param sheet The sheet to cache.
     */
    @Deprecated
    public void putStylesheet(String key, Stylesheet sheet) {
        _cache.put(key, sheet);
    }

    /**
     * @param key
     * @return true if a Stylesheet with this key has been put in the cache.
     *         Note that the Stylesheet may be null.
     */
    //TODO: work out how to handle caching properly, with cache invalidation
    @Deprecated
    public boolean containsStylesheet(String key) {
        return _cache.containsKey(key);
    }

    /**
     * Returns a cached sheet by its key; null if no entry for that key.
     *
     * @param key The key for this sheet; same as key passed to
     *            putStylesheet();
     * @return The stylesheet
     */
    @Deprecated
    public Stylesheet getCachedStylesheet(String key) {
        return _cache.get(key);
    }

    /**
     * Removes a cached sheet by its key.
     *
     * @param key The key for this sheet; same as key passed to
     *            putStylesheet();
     */
    @Deprecated
    public Object removeCachedStylesheet(String key) {
        return _cache.remove(key);
    }
    
    @Deprecated
    public void flushCachedStylesheets() {
        _cache.clear();
    }

    /**
     * Returns a cached sheet by its key; loads and caches it if not in cache;
     * null if not able to load
     *
     * @param info The StylesheetInfo for this sheet
     * @return The stylesheet
     */
    //TODO: this looks a bit odd
    public Stylesheet getStylesheet(StylesheetInfo info) {
        XRLog.load("Requesting stylesheet: " + info.getUri());

        Stylesheet s = getCachedStylesheet(info.getUri());
        if (s == null && !containsStylesheet(info.getUri())) {
            s = parse(info);
            putStylesheet(info.getUri(), s);
        }
        return s;
    }

    public void setUserAgentCallback(UserAgentCallback userAgent) {
        _userAgentCallback = userAgent;
    }
    
    public void setSupportCMYKColors(boolean b) {
        _cssParser.setSupportCMYKColors(b);
    }
}

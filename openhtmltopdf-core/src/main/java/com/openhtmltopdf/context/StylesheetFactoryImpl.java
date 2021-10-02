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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.openhtmltopdf.css.extend.StylesheetFactory;
import com.openhtmltopdf.css.parser.CSSParser;
import com.openhtmltopdf.css.sheet.Ruleset;
import com.openhtmltopdf.css.sheet.Stylesheet;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.resource.CSSResource;
import com.openhtmltopdf.util.LogMessageId;
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

    /**
     * This may avoid @import loops, ie. one.css includes two.css
     * which then includes one.css.
     */
    private final Map<String, Integer> _seenStylesheetUris = new HashMap<>();

    /**
     * The maximum number of times a stylesheet uri can be link or
     * imported before we give up and conclude there is a loop.
     */
    private static final int MAX_STYLESHEET_INCLUDES = 10;

    private final CSSParser _cssParser;

    public StylesheetFactoryImpl(UserAgentCallback userAgentCallback) {
        _userAgentCallback = userAgentCallback;
        _cssParser = new CSSParser((uri, message) -> {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.CSS_PARSE_GENERIC_MESSAGE, uri, message);
        });
    }

    public Stylesheet parse(Reader reader, StylesheetInfo info) {
        try {
            return _cssParser.parseStylesheet(info.getUri(), info.getOrigin(), reader);
        } catch (IOException e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.CSS_PARSE_COULDNT_PARSE_STYLESHEET_AT_URI, info.getUri(), e.getMessage(), e);
            return new Stylesheet(info.getUri(), info.getOrigin());
        }
    }

    /**
     * @return Returns null if uri could not be loaded
     */
    private Stylesheet parse(StylesheetInfo info) {
        try (CSSResource cr = _userAgentCallback.getCSSResource(info.getUri())) {
            if (cr == null) {
                return null;
            }

            try (Reader reader = cr.getResourceReader()) {
                if (reader == null) {
                    return null;
                }

                return parse(reader, info);
            }

        } catch (IOException e1) {
            // Ignore IOException from close invocation.
            return null;
        }
    }

    public Ruleset parseStyleDeclaration(int origin, String styleDeclaration) {
        return _cssParser.parseDeclaration(origin, styleDeclaration);
    }

    /**
     * Returns a sheet by its key
     * null if not able to load
     *
     *
     * @param info The StylesheetInfo for this sheet
     * @return The stylesheet
     */
    public Stylesheet getStylesheet(StylesheetInfo info) {
        XRLog.log(Level.INFO, LogMessageId.LogMessageId1Param.LOAD_REQUESTING_STYLESHEET_AT_URI, info.getUri());

        Integer includeCount = _seenStylesheetUris.get(info.getUri());

        if (includeCount != null && includeCount >= MAX_STYLESHEET_INCLUDES) {
            // Probably an import loop.
            XRLog.log(Level.SEVERE, LogMessageId.LogMessageId2Param.CSS_PARSE_TOO_MANY_STYLESHEET_IMPORTS, includeCount, info.getUri());
            return null;
        }

        _seenStylesheetUris.merge(info.getUri(), 1, (oldV, newV) -> oldV + 1);

        return parse(info);
    }

    public void setUserAgentCallback(UserAgentCallback userAgent) {
        _userAgentCallback = userAgent;
    }
    
    public void setSupportCMYKColors(boolean b) {
        _cssParser.setSupportCMYKColors(b);
    }
}

/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
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
 * }}}
 */
package com.openhtmltopdf.slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;

public class Slf4jLogger implements XRLogger {
    private static final String DEFAULT_LOGGER_NAME = "com.openhtmltopdf.other";
    
    private static final Map<String, String> LOGGER_NAME_MAP;
    static {
        LOGGER_NAME_MAP = new HashMap<String, String>();
        
        LOGGER_NAME_MAP.put(XRLog.CONFIG, "com.openhtmltopdf.config");
        LOGGER_NAME_MAP.put(XRLog.EXCEPTION, "com.openhtmltopdf.exception");
        LOGGER_NAME_MAP.put(XRLog.GENERAL, "com.openhtmltopdf.general");
        LOGGER_NAME_MAP.put(XRLog.INIT, "com.openhtmltopdf.init");
        LOGGER_NAME_MAP.put(XRLog.JUNIT, "com.openhtmltopdf.junit");
        LOGGER_NAME_MAP.put(XRLog.LOAD, "com.openhtmltopdf.load");
        LOGGER_NAME_MAP.put(XRLog.MATCH, "com.openhtmltopdf.match");
        LOGGER_NAME_MAP.put(XRLog.CASCADE, "com.openhtmltopdf.cascade");
        LOGGER_NAME_MAP.put(XRLog.XML_ENTITIES, "com.openhtmltopdf.load.xmlentities");
        LOGGER_NAME_MAP.put(XRLog.CSS_PARSE, "com.openhtmltopdf.cssparse");
        LOGGER_NAME_MAP.put(XRLog.LAYOUT, "com.openhtmltopdf.layout");
        LOGGER_NAME_MAP.put(XRLog.RENDER, "com.openhtmltopdf.render");
    }
    
    private String _defaultLoggerName = DEFAULT_LOGGER_NAME;
    private Map<String, String> _loggerNameMap = LOGGER_NAME_MAP;
    
    @Override
    public void log(String where, Level level, String msg) {
    	Logger logger = LoggerFactory.getLogger(getLoggerName(where));
    	
    	if (level == Level.SEVERE)
    		logger.error(msg);
    	else if (level == Level.WARNING)
    		logger.warn(msg);
    	else if (level == Level.INFO || level == Level.CONFIG)
    		logger.info(msg);
    	else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
    		logger.debug(msg);
    	else
    		logger.info(msg);
    }

    @Override
    public void log(String where, Level level, String msg, Throwable th) {
    	Logger logger = LoggerFactory.getLogger(getLoggerName(where));
    	
    	if (level == Level.SEVERE)
    		logger.error(msg, th);
    	else if (level == Level.WARNING)
    		logger.warn(msg, th);
    	else if (level == Level.INFO || level == Level.CONFIG)
    		logger.info(msg, th);
    	else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
    		logger.debug(msg, th);
    	else
    		logger.info(msg, th);
    }
    
    private String getLoggerName(String xrLoggerName) {
        String result = _loggerNameMap.get(xrLoggerName);
        if (result != null) {
            return result;
        } else {
            return _defaultLoggerName;
        }
    }

    @Override
    public void setLevel(String logger, Level level) {
        throw new UnsupportedOperationException("log4j should be not be configured here");
    }
    
    public Map<String, String> getLoggerNameMap() {
        return _loggerNameMap;
    }

    public void setLoggerNameMap(Map<String, String> loggerNameMap) {
        _loggerNameMap = loggerNameMap;
    }

    public String getDefaultLoggerName() {
        return _defaultLoggerName;
    }

    public void setDefaultLoggerName(String defaultLoggerName) {
        _defaultLoggerName = defaultLoggerName;
    }
}

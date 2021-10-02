/*
 * {{{ header & license
 * Copyright (c) 2004, 2005, 2008 Joshua Marinacci, Patrick Wright
 * Copyright (c) 2008 Patrick Wright
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
package com.openhtmltopdf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * Utility class for using the java.util.logging package. Relies on the standard
 * configuration for logging, but gives easier access to the various logs
 * (plumbing.load, .init, .render)
 *
 * @author empty
 */
public class XRLog {
    private static final List<String> LOGGER_NAMES = new ArrayList<>(20);
    public final static String CONFIG = registerLoggerByName("com.openhtmltopdf.config");
    public final static String EXCEPTION = registerLoggerByName("com.openhtmltopdf.exception");
    public final static String GENERAL = registerLoggerByName("com.openhtmltopdf.general");
    public final static String INIT = registerLoggerByName("com.openhtmltopdf.init");
    public final static String JUNIT = registerLoggerByName("com.openhtmltopdf.junit");
    public final static String LOAD = registerLoggerByName("com.openhtmltopdf.load");
    public final static String MATCH = registerLoggerByName("com.openhtmltopdf.match");
    public final static String CASCADE = registerLoggerByName("com.openhtmltopdf.cascade");
    public final static String XML_ENTITIES = registerLoggerByName("com.openhtmltopdf.load.xml-entities");
    public final static String CSS_PARSE = registerLoggerByName("com.openhtmltopdf.css-parse");
    public final static String LAYOUT = registerLoggerByName("com.openhtmltopdf.layout");
    public final static String RENDER = registerLoggerByName("com.openhtmltopdf.render");

    private static String registerLoggerByName(final String loggerName) {
        LOGGER_NAMES.add(loggerName);
        return loggerName;
    }

    private static volatile boolean initPending = true;
    private static volatile XRLogger loggerImpl;

    private static volatile Boolean loggingEnabled;

    /**
     * Returns a list of all loggers that will be accessed by XRLog. Each entry is a String with a logger
     * name, which can be used to retrieve the logger using the corresponding Logging API; example name might be
     * "com.openhtmltopdf.render"
     *
     * @return List of loggers, never null.
     */
    public static List<String> listRegisteredLoggers() {
        if (initPending) {
            init();
        }

        // defensive copy
        return new ArrayList<>(LOGGER_NAMES);
    }

    public static void log(Level level, LogMessageId.LogMessageId0Param logMessageId) {
        log(level, logMessageId, false);
    }

    public static void log(Level level, LogMessageId.LogMessageId0Param logMessageId, Throwable t) {
        log(level, logMessageId, true, t);
    }

    public static void log(Level level, LogMessageId.LogMessageId1Param logMessageId, Object arg) {
        log(level, logMessageId, false, arg);
    }

    public static void log(Level level, LogMessageId.LogMessageId1Param logMessageId, Object arg, Throwable throwable) {
        log(level, logMessageId, true, arg, throwable);
    }

    public static void log(Level level, LogMessageId.LogMessageId2Param logMessageId, Object arg1, Object arg2) {
        log(level, logMessageId, false, arg1, arg2);
    }

    public static void log(Level level, LogMessageId.LogMessageId2Param logMessageId, Object arg1, Object arg2, Throwable throwable) {
        log(level, logMessageId, true, arg1, arg2, throwable);
    }

    public static void log(Level level, LogMessageId.LogMessageId3Param logMessageId, Object arg1, Object arg2, Object arg3) {
        log(level, logMessageId, false, arg1, arg2, arg3);
    }

    public static void log(Level level, LogMessageId.LogMessageId3Param logMessageId, Object arg1, Object arg2, Object arg3, Throwable throwable) {
        log(level, logMessageId, true, arg1, arg2, arg3, throwable);
    }

    public static void log(Level level, LogMessageId.LogMessageId4Param logMessageId, Object arg1, Object arg2, Object arg3, Object arg4) {
        log(level, logMessageId, false, arg1, arg2, arg3, arg4);
    }

    public static void log(Level level, LogMessageId.LogMessageId5Param logMessageId, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        log(level, logMessageId, false, arg1, arg2, arg3, arg4, arg5);
    }

    private static void log(Level level, LogMessageId logMessageId, boolean hasError, Object... args) {
        if (initPending) {
            init();
        }
        if (isLoggingEnabled()) {
            Diagnostic diagnostic = new Diagnostic(level, logMessageId, hasError, args);
            if (loggerImpl.isLogLevelEnabled(diagnostic)) {
                loggerImpl.log(diagnostic);
            }
            ThreadCtx.addDiagnostic(diagnostic);
        }
    }

    private static void init() {
        synchronized (XRLog.class) {
            if (!initPending) {
                return;
            }

            if (loggingEnabled == null) {
            	XRLog.setLoggingEnabled(true);
            }

            if (loggerImpl == null) {
                loggerImpl = new JDKXRLogger();
            }

            initPending = false;
        }
    }

    public static synchronized void setLevel(String log, Level level) {
        if (initPending) {
            init();
        }
        loggerImpl.setLevel(log, level);
    }

    /**
     * Whether logging is on or off.
     * @return Returns true if logging is enabled, false if not. Corresponds
     * to configuration file property xr.util-logging.loggingEnabled, or to
     * value passed to setLoggingEnabled(bool).
     */
    public static boolean isLoggingEnabled() {
        return loggingEnabled == true;
    }

    /**
     * Turns logging on or off, without affecting logging configuration.
     *
     * @param loggingEnabled Flag whether logging is enabled or not;
     * if false, all logging calls fail silently. Corresponds
     * to configuration file property xr.util-logging.loggingEnabled
     */
    public static void setLoggingEnabled(boolean loggingEnabled) {
        XRLog.loggingEnabled = loggingEnabled;
    }

    public static synchronized XRLogger getLoggerImpl() {
        return loggerImpl;
    }

    public static synchronized void setLoggerImpl(XRLogger loggerImpl) {
        XRLog.loggerImpl = loggerImpl;
    }
}// end class

/*
 * $Id$
 *
 * $Log$
 * Revision 1.20  2010/01/13 01:28:46  peterbrant
 * Add synchronization to XRLog#log to avoid spurious errors on initializatoin
 *
 * Revision 1.19  2008/01/27 16:40:29  pdoubleya
 * Issues 186 and 130: fix configuration so that logging setup does not override any current settings for JDK logging classes. Disable logging by default.
 *
 * Revision 1.18  2007/09/10 20:28:26  peterbrant
 * Make underlying logging implementation pluggable / Add log4j logging implementation (not currently compiled with Ant to avoid additional compile time dependency)
 *
 * Revision 1.17  2007/06/02 20:00:34  peterbrant
 * Revert earlier change to default CSS parse logging level / Use WARNING explicitly for CSS parse errors
 *
 * Revision 1.16  2007/06/01 21:44:08  peterbrant
 * CSS parsing errors should be logged at WARNING, not INFO level
 *
 * Revision 1.15  2006/08/17 17:32:25  joshy
 * intial patch to fix the logging config issues
 * https://xhtmlrenderer.dev.java.net/issues/show_bug.cgi?id=130
 *
 * Revision 1.14  2006/07/26 17:59:01  pdoubleya
 * Use proper form for logging exceptions.
 *
 * Revision 1.13  2006/07/17 22:15:59  pdoubleya
 * Added loggingEnabled switch to XRLog and config file; default logging to off there and in Configuration. Fix for Issue Tracker #123.
 *
 * Revision 1.12  2005/07/13 22:49:15  joshy
 * updates to get the jnlp to work without being signed
 *
 * Revision 1.11  2005/06/26 01:21:35  tobega
 * Fixed possible infinite loop in init()
 *
 * Revision 1.10  2005/05/06 16:54:32  joshy
 * forgot to add this level stuff
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.9  2005/04/07 16:14:28  pdoubleya
 * Updated to clarify relationship between Configuration and XRLog on load; Configuration must load first, but holds off on logging until XRLog is initialized. LogStartupConfig no longer used.
 *
 * Revision 1.8  2005/03/27 18:36:26  pdoubleya
 * Added separate logging for entity resolution.
 *
 * Revision 1.7  2005/01/29 20:18:38  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.6  2005/01/29 12:18:15  pdoubleya
 * Added cssParse logging.
 *
 * Revision 1.5  2005/01/24 19:01:10  pdoubleya
 * Mass checkin. Changed to use references to CSSName, which now has a Singleton instance for each property, everywhere property names were being used before. Removed commented code. Cascaded and Calculated style now store properties in arrays rather than maps, for optimization.
 *
 * Revision 1.4  2005/01/24 14:33:07  pdoubleya
 * Added junit logging hierarchy.
 *
 * Revision 1.3  2004/10/23 14:06:57  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 *
 */


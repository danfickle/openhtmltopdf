/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2007 Wisconsin Court System
 * Copyright (c) 2008 Patrick Wright
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
package com.openhtmltopdf.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.Arrays;

/**
 * An {@link XRLogger} interface that uses <code>java.util.logging</code>.
 * https://github.com/danfickle/openhtmltopdf/wiki/Logging
 */
public class JDKXRLogger implements XRLogger {
    private volatile boolean initPending = true;

    // Keep a map of Loggers so they are not garbage collected
    // which makes them lose their settings we have applied.
    private volatile Map<String, Logger> loggers;

    private final boolean useParent;
    private final Level level;
    private final Handler handler;
    private final Formatter formatter;

    public JDKXRLogger() {
        // Note: We MUST NOT call retrieveLoggers via this constructor
        // as XRLog.listRegisteredLoggers may call this constructor
        // leading to a stack overflow!
        this(false, Level.INFO, new ConsoleHandler(), new XRSimpleLogFormatter());
    }

    public JDKXRLogger(boolean useParent, Level level, Handler handler, Formatter formatter) {
        this.useParent = useParent;
        this.level = level;
        this.handler = handler;
        this.formatter = formatter;
    }

    private void checkInitPending() {
        if (!initPending) {
            return;
        }

        synchronized (this) {
            init(useParent, level, handler, formatter);
        }
    }

    @Override
    public boolean isLogLevelEnabled(Diagnostic diagnostic) {
        return getLogger(diagnostic.getLogMessageId().getWhere()).isLoggable(diagnostic.getLevel());
    }

    @Override
    public void log(String where, Level level, String msg) {
        getLogger(where).log(level, msg);
    }

    @Override
    public void log(String where, Level level, String msg, Throwable th) {
        getLogger(where).log(level, msg, th);
    }

    @Override
    public void setLevel(String logger, Level level) {
        getLogger(logger).setLevel(level);
    }

    /**
     * Same purpose as Logger.getLogger(), except that the static initialization
     * for XRLog will initialize the LogManager with logging levels and other
     * configuration. Use this instead of Logger.getLogger()
     *
     * @param log PARAM
     * @return The logger value
     */
    private Logger getLogger(String log) {
        checkInitPending();
        return loggers.get(log);
    }

    private void init(boolean useParent, Level level, Handler handler, Formatter formatter) {
            if (!initPending) {
                return;
            }
            try {
                initializeJDKLogManager(useParent, level, handler, formatter);
            } finally {
                initPending = false;
            }
    }

    private void initializeJDKLogManager(boolean useParent, Level level, Handler handler, Formatter formatter) {
        loggers = retrieveLoggers();

        configureLoggerHandlerForwarding(useParent);
        configureLogLevels(level);
        configureLogHandlers(handler, formatter);
    }

    private void configureLoggerHandlerForwarding(boolean useParentHandlers) {
        loggers.forEach((name, logger) -> logger.setUseParentHandlers(useParentHandlers));
    }

    /**
     * Returns a List of all Logger instances used by this project from the JDK LogManager; these will
     * be automatically created if they aren't already available.
     */
    private Map<String, Logger> retrieveLoggers() {
        Map<String, Logger> loggers = new HashMap<>();
        for (String name : XRLog.listRegisteredLoggers()) {
            loggers.put(name, Logger.getLogger(name));
        }
        return loggers;
    }

    private void configureLogHandlers(Handler handler, Formatter formatter) {
        handler.setFormatter(formatter);
        // Note Logger::removeLogger doesn't throw if the handler isn't found
        // so there are no sync issues here.
        loggers.forEach((name, logger) -> Arrays.stream(logger.getHandlers()).forEach(logger::removeHandler));
        loggers.forEach((name, logger) -> logger.addHandler(handler));
    }

    private void configureLogLevels(Level level) {
        loggers.forEach((name, logger) -> logger.setLevel(level));
    }
}

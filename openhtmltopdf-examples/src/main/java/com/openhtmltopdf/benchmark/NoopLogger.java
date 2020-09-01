package com.openhtmltopdf.benchmark;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.XRLogger;

import java.util.logging.Level;

/**
 * @author schrader
 */
class NoopLogger implements XRLogger {
    @Override
    public void log(String where, Level level, String msg) {

    }

    @Override
    public void log(String where, Level level, String msg, Throwable th) {

    }

    @Override
    public void setLevel(String logger, Level level) {

    }

    @Override
    public boolean isLogLevelEnabled(Diagnostic diagnostic) {
        return false;
    }
}

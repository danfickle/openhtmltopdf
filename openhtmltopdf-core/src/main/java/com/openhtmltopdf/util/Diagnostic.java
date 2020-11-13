package com.openhtmltopdf.util;

import java.util.logging.Level;

public class Diagnostic {

    private final Level level;
    private final LogMessageId logMessageId;
    private final Object[] args;
    private final Throwable error;

    Diagnostic(Level level, LogMessageId logMessageId, boolean hasError, Object[] args) {
        this.level = level;
        this.logMessageId = logMessageId;
        this.args = args;
        this.error = hasError ? (Throwable) args[args.length - 1] : null;
    }

    public LogMessageId getLogMessageId() {
        return logMessageId;
    }

    public Level getLevel() {
        return level;
    }

    public boolean hasError() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    public String getFormattedMessage() {
        return logMessageId.formatMessage(args);
    }

    public Object[] getArgs() {
        return args;
    }
}

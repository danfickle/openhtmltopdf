/*
 * {{{ header & license
 * XRSimpleLogFormatter.java
 * Copyright (c) 2004, 2005 Patrick Wright
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

import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.*;


/**
 * A java.util.logging.Formatter class that writes a bare-bones log messages,
 * with no origin class name and no date/time.
 *
 * @author   Patrick Wright
 */
public class XRSimpleLogFormatter extends Formatter {
    /** MessageFormat for standard messages (without Throwable) */
    private final MessageFormat mformat;
    /** MessageFormat for messages with a throwable */
    private final MessageFormat exmformat;

    private final static String MSG_FMT = "{1} {2}:: {5}\n";
    private final static String EX_MSG_FMT = "{1} {2}:: {5} => {6}:: {7}\n";

    private final boolean[] usedPlaceholderForMsgFmt;
    private final boolean[] usedPlaceholderForExmsgFmt;

    public XRSimpleLogFormatter() {
        super();
        mformat = new MessageFormat(MSG_FMT);
        exmformat = new MessageFormat(EX_MSG_FMT);
        usedPlaceholderForMsgFmt = usedPlaceholder(mformat);
        usedPlaceholderForExmsgFmt = usedPlaceholder(exmformat);
    }

    /**
     * Create a custom log formatter for use with:
     * {@link JDKXRLogger#JDKXRLogger(boolean, Level, Handler, Formatter)}
     * 
     * Options:
     * <ul>
     * <li>{0}  String.valueOf(record.getMillis()),</li>
     * <li>{1}  record.getLoggerName(),</li>
     * <li>{2}  record.getLevel().toString(),</li>
     * <li>{3}  record.getSourceClassName(),</li>
     * <li>{4}  record.getSourceMethodName(),</li>
     * <li>{5}  record.getMessage()</li>
     * <li>{6}  record.getThrown().getName()</li>
     * <li>{7}  record.getThrown().getMessage()</li>
     * <li>{8}  record.getThrown() stack trace</li>
     * </ul>
     * Example (msgFmt): <code>{1} {2}:: {5}\n</code><br><br>
     * Example (throwableMsgFmt): <code>{1} {2}:: {5} => {6}:: {7}\n</code>
     */
    public XRSimpleLogFormatter(String msgFmt, String throwableMsgFmt) {
        super();
        mformat = new MessageFormat(msgFmt);
        exmformat = new MessageFormat(throwableMsgFmt);
        usedPlaceholderForMsgFmt = usedPlaceholder(mformat);
        usedPlaceholderForExmsgFmt = usedPlaceholder(exmformat);
    }

    /**
     * Identify which arguments are effectively used.
     */
    private static boolean[] usedPlaceholder(MessageFormat messageFormat) {
        boolean[] used = new boolean[9];
        String identifier = UUID.randomUUID().toString();
        List<String> args = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            args.add(new StringBuilder().append('{').append(identifier).append('-').append(i).append('}').toString());
        }
        String res = messageFormat.format(args.stream().toArray());
        for (int i = 0; i < 9; i++) {
            used[i] = res.contains(args.get(i));
        }
        return used;
    }

    /**
     * Format the given log record and return the formatted string.
     */
    @Override
    public String format( LogRecord record ) {

        Throwable th = record.getThrown();

        boolean[] placeholderUse = th == null ? usedPlaceholderForMsgFmt : usedPlaceholderForExmsgFmt;

        String thName = "";
        String thMessage = "";
        String trace = null;
        if ( th != null ) {
            if (placeholderUse[8]) {
                StringWriter sw = new StringWriter();
                th.printStackTrace(new PrintWriter(sw));
                trace = sw.toString();
            }
            thName = th.getClass().getName();
            thMessage = th.getMessage();
        }

        String[] args = {
                placeholderUse[0] ? String.valueOf( record.getMillis() ) : null,
                placeholderUse[1] ? record.getLoggerName() : null,
                placeholderUse[2] ? record.getLevel().toString() : null,
                placeholderUse[3] ? record.getSourceClassName() : null,
                placeholderUse[4] ? record.getSourceMethodName() : null,
                placeholderUse[5] ? record.getMessage() : null,
                placeholderUse[6] ? thName : null,
                placeholderUse[7] ? thMessage : null,
                placeholderUse[8] ? trace : null
        };
        return th == null ? mformat.format(args) : exmformat.format(args);
    }

    /**
     * Localize and format the message string from a log record.
     */
    @Override
    public String formatMessage( LogRecord record ) {
        return super.formatMessage( record );
    }

    /**
     * Return the header string for a set of formatted records.
     */
    @Override
    public String getHead( Handler h ) {
        return super.getHead( h );
    }

    /**
     * Return the tail string for a set of formatted records.
     */
    @Override
    public String getTail( Handler h ) {
        return super.getTail( h );
    }

}

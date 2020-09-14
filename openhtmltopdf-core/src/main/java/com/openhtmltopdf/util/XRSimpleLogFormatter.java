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
    /** Description of the Field */
    private final MessageFormat mformat;
    /** Description of the Field */
    private final MessageFormat exmformat;
    /** Description of the Field */
    private final static String msgFmt;
    /** Description of the Field */
    private final static String exmsgFmt;

    private final boolean[] usedPlaceholderForMsgFmt;
    private final boolean[] usedPlaceholderForExmsgFmt;

    /** Constructor for the XRSimpleLogFormatter object */
    public XRSimpleLogFormatter() {
        super();
        mformat = new MessageFormat(msgFmt);
        exmformat = new MessageFormat(exmsgFmt);
        usedPlaceholderForMsgFmt = usedPlaceholder(mformat);
        usedPlaceholderForExmsgFmt = usedPlaceholder(exmformat);
    }

    /**
     * Identify which arguments are effectively used.
     *
     * @param messageFormat
     * @return
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
     *
     * @param record  PARAM
     * @return        Returns
     */
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
     *
     * @param record  PARAM
     * @return        Returns
     */
    public String formatMessage( LogRecord record ) {
        return super.formatMessage( record );
    }

    /**
     * Return the header string for a set of formatted records.
     *
     * @param h  PARAM
     * @return   The head value
     */
    public String getHead( Handler h ) {
        return super.getHead( h );
    }

    /**
     * Return the tail string for a set of formatted records.
     *
     * @param h  PARAM
     * @return   The tail value
     */
    public String getTail( Handler h ) {
        return super.getTail( h );
    }

    static {
        msgFmt = Configuration.valueFor( "xr.simple-log-format", "{1} {2}:: {5}" ).trim() + "\n";
        exmsgFmt = Configuration.valueFor( "xr.simple-log-format-throwable", "{1} {2}:: {5}" ).trim() + "\n";
    }

}// end class

/*
 * $Id$
 *
 * $Log$
 * Revision 1.6  2005/04/07 16:15:47  pdoubleya
 * Typo.
 *
 * Revision 1.5  2005/01/29 20:18:37  pdoubleya
 * Clean/reformat code. Removed commented blocks, checked copyright.
 *
 * Revision 1.4  2004/10/23 14:06:57  pdoubleya
 * Re-formatted using JavaStyle tool.
 * Cleaned imports to resolve wildcards except for common packages (java.io, java.util, etc).
 * Added CVS log comments at bottom.
 *
 * Revision 1.3  2004/10/18 12:08:37  pdoubleya
 * Incorrect Configuration key fixed.
 *
 * Revision 1.2  2004/10/14 12:53:26  pdoubleya
 * Added handling for exception messages with stack trace and separate message format.
 *
 * Revision 1.1  2004/10/14 11:13:22  pdoubleya
 * Added to CVS.
 *
 */


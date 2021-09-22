/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Who?
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
package com.openhtmltopdf.resource;

import org.xml.sax.InputSource;

import com.openhtmltopdf.util.OpenUtil;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * @author Patrick Wright
 */
public abstract class AbstractResource implements Resource, Closeable {
    private enum StreamType { READER, STREAM, INPUT_SOURCE; }
    private final StreamType streamType;

    private InputSource inputSource;
    private final InputStream inputStream;
    private Reader inputReader;

    private final long createTimeStamp;
    private long elapsedLoadTime;

    private AbstractResource(
            StreamType streamType, InputSource source, Reader reader, InputStream stream) {
        this.createTimeStamp = System.currentTimeMillis();
        this.streamType = streamType;

        this.inputSource = source;
        this.inputReader = reader;
        this.inputStream = stream;
    }

    public AbstractResource(InputSource source) {
        this(StreamType.INPUT_SOURCE, source, null, null);
    }

    public AbstractResource(Reader reader) {
        this(StreamType.READER, null, reader, null);
    }

    public AbstractResource(InputStream is) {
        this(StreamType.STREAM, null, null, is);
    }

    @Override
    public void close() throws IOException {
        OpenUtil.closeQuietly(inputReader);
        OpenUtil.closeQuietly(inputStream);
    }

    public InputSource getResourceInputSource() {
    	if (streamType == StreamType.STREAM &&
    		this.inputSource == null) {
    		this.inputSource = new InputSource(new BufferedInputStream(this.inputStream));
    	}
    	return this.inputSource;
    }

    public Reader getResourceReader() {
        if (streamType == StreamType.STREAM &&
            this.inputReader == null) {
            this.inputReader = new InputStreamReader(this.inputStream, StandardCharsets.UTF_8);
        }
        return this.inputReader;
    }

    public InputStream getResourceInputStream() {
        return this.inputStream;
    }

    public long getResourceLoadTimeStamp() {
        return this.createTimeStamp;
    }

    public long getElapsedLoadTime() {
        return elapsedLoadTime;
    }

    public void setElapsedLoadTime(long elapsedLoadTime) {
        this.elapsedLoadTime = elapsedLoadTime;
    }
}

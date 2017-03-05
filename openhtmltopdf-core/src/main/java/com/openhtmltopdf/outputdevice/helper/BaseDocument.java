package com.openhtmltopdf.outputdevice.helper;

import java.io.File;

import org.w3c.dom.Document;

public class BaseDocument {
    public final String html;
    public final Document document;
    public final File file;
    public final String uri;
    public final String baseUri;
    
    public BaseDocument(String baseUri, String html, Document document, File file, String uri) {
        this.html = html;
        this.document = document;
        this.file = file;
        this.uri = uri;
        this.baseUri = baseUri;
    }
}

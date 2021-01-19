package com.openhtmltopdf.svgsupport;

import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.UserAgent;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class OpenHtmlDocumentLoader extends DocumentLoader {

    private final UserAgentCallback userAgentCallback;

    public OpenHtmlDocumentLoader(UserAgent userAgent, UserAgentCallback userAgentCallback) {
        super(userAgent);
        this.userAgentCallback = userAgentCallback;
    }


    @Override
    public Document loadDocument(String uri) throws IOException {
        try {
            // special handling of relative uri in case of file protocol, we receive something like "file:file.svg"
            // The path will be null, but the scheme specific part will be not null
            URI parsedURI = new URI(uri);
            if ("file".equals(parsedURI.getScheme()) && parsedURI.getPath() == null && parsedURI.getSchemeSpecificPart() != null) {
                uri = userAgentCallback.resolveURI(parsedURI.getSchemeSpecificPart());
            } else if (!parsedURI.isAbsolute()) {
                uri = userAgentCallback.resolveURI(uri);
            }
        } catch (URISyntaxException uriSyntaxException) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_URI_SYNTAX_WHILE_LOADING_EXTERNAL_SVG_RESOURCE, uri, uriSyntaxException);
        }
        return super.loadDocument(uri, new ByteArrayInputStream(userAgentCallback.getBinaryResource(uri, ExternalResourceType.SVG_BINARY)));
    }
}

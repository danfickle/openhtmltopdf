package com.openhtmltopdf.swing;

import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.util.logging.Level;

public class UriResolver {
    private String _baseUri;

    public String resolve(final String uri) {
        if (uri == null) return null;
        String ret = null;
        if (_baseUri == null) {//first try to set a base URL
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.LOAD_BASE_URL_IS_NULL_TRYING_TO_CONFIGURE_ONE);
        	try {
                URL result = new URL(uri);
                setBaseUri(result.toExternalForm());
            } catch (MalformedURLException e) {
                try {
                    setBaseUri(new File(".").toURI().toURL().toExternalForm());
                } catch (Exception e1) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_DEFAULT_USERAGENT_IS_NOT_ABLE_TO_RESOLVE_BASE_URL_FOR, uri);
                    return null;
                }
            }
        }
        // test if the URI is valid; if not, try to assign the base url as its parent
        try {
            return new URL(uri).toString();
        } catch (MalformedURLException e) {
            XRLog.log(Level.FINE, LogMessageId.LogMessageId2Param.LOAD_COULD_NOT_READ_URI_AT_URL_MAY_BE_RELATIVE, uri, _baseUri);
            try {
                URL result = new URL(new URL(_baseUri), uri);
                ret = result.toString();
                XRLog.log(Level.FINE, LogMessageId.LogMessageId2Param.LOAD_WAS_ABLE_TO_READ_FROM_URI_USING_PARENT_URL, uri, _baseUri);
            } catch (MalformedURLException e1) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.EXCEPTION_DEFAULT_USERAGENT_IS_NOT_ABLE_TO_RESOLVE_URL_WITH_BASE_URL, uri, _baseUri);
            }
        }

        return ret;

    }

    public void setBaseUri(final String baseUri) {
        _baseUri = baseUri;
    }

    public String getBaseUri() {
        return _baseUri;
    }
}

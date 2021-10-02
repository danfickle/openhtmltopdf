/*
 * NaiveUserAgent.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
 *
 */
package com.openhtmltopdf.swing;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.openhtmltopdf.event.DocumentListener;
import com.openhtmltopdf.extend.FSUriResolver;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.resource.CSSResource;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.util.XRLog;

/**
 * <p>NaiveUserAgent is a simple implementation of {@link UserAgentCallback} which places no restrictions on what
 * XML, CSS or images are loaded.</p>
 *
 * <p>The NaiveUserAgent has a simple per-run cache for images so that the same image is not embedded in a document
 *  multiple times.</p>
 *
 * @author Torbjoern Gannholm
 */
public abstract class NaiveUserAgent implements UserAgentCallback, DocumentListener {

    /**
     * a (simple) cache
     * This is only useful for the one run.
     */
    protected final LinkedHashMap<String, ImageResource> _imageCache = new LinkedHashMap<>();
    protected final FSUriResolver DEFAULT_URI_RESOLVER = new DefaultUriResolver(); 

    protected final Map<ExternalResourceControlPriority, BiPredicate<String, ExternalResourceType>> _accessControllers = new EnumMap<>(ExternalResourceControlPriority.class);

    protected FSUriResolver _resolver = DEFAULT_URI_RESOLVER;
    protected String _baseUri;
	protected Map<String, FSStreamFactory> _protocolsStreamFactory = new HashMap<>(2);
    
    public static class DefaultHttpStream implements FSStream {
    	private InputStream strm;
    	
    	public DefaultHttpStream(InputStream strm) {
    		this.strm = strm;
    	}
    	
    	@Override
    	public InputStream getStream() {
    		return this.strm;
    	}

		@Override
		public Reader getReader() {
			if (this.strm != null) {
				return new InputStreamReader(this.strm, StandardCharsets.UTF_8);
			}
			return null;
		}
    }

    public static class DefaultHttpStreamFactory implements FSStreamFactory {
        final static int CONNECTION_TIMEOUT = 10_000;
        final static int READ_TIMEOUT = 30_000;

        final int connectTimeout;
        final int readTimeout;

        /**
         * Create a FSStreamFactory for http, https with specified timeouts.
         * Uses URLConnection to perform requests.
         * Zero value for timeout specifies no timeout.
         */
        public DefaultHttpStreamFactory(int connectTimeout, int readTimeout) {
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;
        }

        /**
         * Create a FSStreamFactory with 10 second connect timeout and
         * 30 second read timeout.
         */
        public DefaultHttpStreamFactory() {
            this(CONNECTION_TIMEOUT, READ_TIMEOUT);
        }

        @SuppressWarnings("resource")
        @Override
		public FSStream getUrl(String uri) {
			InputStream is = null;
			
	        try {
                URLConnection conn = new URL(uri).openConnection();
                conn.setConnectTimeout(this.connectTimeout);
                conn.setReadTimeout(this.readTimeout);
                conn.connect();

                is = conn.getInputStream();
	        } catch (java.net.MalformedURLException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_MALFORMED_URL, uri, e);
	        } catch (java.io.FileNotFoundException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_ITEM_AT_URI_NOT_FOUND, uri, e);
	        } catch (java.io.IOException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_IO_PROBLEM_FOR_URI, uri, e);
	        }

            // Ownership is transferred to DefaultHttpStream which implements Closeable.
	        return new DefaultHttpStream(is);
		}
    }

    private static class ByteStream implements FSStream {

		ByteStream(byte[] input) {
			this.input = input;
		}

    	private final byte[] input;

		@Override
		public InputStream getStream() {
			return new ByteArrayInputStream(input);
		}

		@Override
		public Reader getReader() {
			return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
		}
	}

    public static class DataUriFactory implements FSStreamFactory {

		@Override
		public FSStream getUrl(String url) {
			int idxSeparator;
			if (url != null && url.startsWith("data:") && (idxSeparator = url.indexOf(',')) > 0) {
				String data = url.substring(idxSeparator+1);
				byte[] res;
				if (url.indexOf("base64,") == idxSeparator - 6 /* 6 = "base64,".length */) {
					res = fromBase64Encoded(data);
				} else {
					res = data.getBytes(StandardCharsets.UTF_8);
				}
				return new ByteStream(res);
			}
			return null;
		}
		
        static final Pattern WHITE_SPACE = Pattern.compile("\\s+");

        static byte[] fromBase64Encoded(String b64encoded) {
            return Base64.getMimeDecoder().decode(WHITE_SPACE.matcher(b64encoded).replaceAll(""));
        }
	}

    /**
     * Get the binary content of an embedded base 64 image.
     *
     * @param imageDataUri URI of the embedded image
     * @return The binary content
     */
    public static byte[] getEmbeddedBase64Image(String imageDataUri) {
        int b64Index = imageDataUri.indexOf("base64,");
        if (b64Index != -1) {
            String b64encoded = imageDataUri.substring(b64Index + "base64,".length());
            return DataUriFactory.fromBase64Encoded(b64encoded);
        } else {
            XRLog.log(Level.SEVERE, LogMessageId.LogMessageId0Param.LOAD_EMBEDDED_DATA_URI_MUST_BE_ENCODED_IN_BASE64);
        }
        return null;
    }

    public NaiveUserAgent() {
    	FSStreamFactory factory = new DefaultHttpStreamFactory();
    	this._protocolsStreamFactory.put("http", factory);
    	this._protocolsStreamFactory.put("https", factory);
    	this._protocolsStreamFactory.put("data", new DataUriFactory());
    }
    
    public void setProtocolsStreamFactory(Map<String, FSStreamFactory> protocolsStreamFactory) {
    	this._protocolsStreamFactory = protocolsStreamFactory;
    }

    public void setUriResolver(FSUriResolver resolver) {
    	this._resolver = resolver;
    }
    
    public FSUriResolver getDefaultUriResolver() {
    	return DEFAULT_URI_RESOLVER;
    }
    
    /**
     * Empties the image cache entirely.
     */
    @Deprecated
    public void clearImageCache() {
        _imageCache.clear();
    }
    
    protected FSStreamFactory getProtocolFactory(String protocol) {
    	return _protocolsStreamFactory.get(protocol);
    }
    
    protected boolean hasProtocolFactory(String protocol) {
    	return _protocolsStreamFactory.containsKey(protocol);
    }

	protected String extractProtocol(String uri) throws URISyntaxException {
		int idxSeparator;
		if (uri != null && (idxSeparator = uri.indexOf(':')) > 0) {
			return uri.substring(0, idxSeparator);
		} else {
			throw new URISyntaxException(uri, "missing protocol for URI");
		}
	}

    /**
     * Gets a InputStream for the resource identified by a resolved URI.
     */
    protected InputStream openStream(String uri) {
        java.io.InputStream is = null;
        
        try {
			String protocol = extractProtocol(uri);

			if (hasProtocolFactory(protocol)) {
				return getProtocolFactory(protocol).getUrl(uri).getStream();
			} else {
		        try {
		            is = new URL(uri).openStream();
		        } catch (java.net.MalformedURLException e) {
		        	XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_MALFORMED_URL, uri, e);
		        } catch (java.io.FileNotFoundException e) {
		        	XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_ITEM_AT_URI_NOT_FOUND, uri, e);
		        } catch (java.io.IOException e) {
		        	XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_IO_PROBLEM_FOR_URI, uri, e);
		        }
			}
        } catch (URISyntaxException e1) {
			XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_MALFORMED_URL, uri, e1);
		}

        return is;
    }

    /**
     * Gets a reader for the identified resource by a resolved URI.
     */
    protected Reader openReader(String uri) {
    	InputStream is = null;
    	
        try {
			String protocol = extractProtocol(uri);

			if (hasProtocolFactory(protocol)) {
				return getProtocolFactory(protocol).getUrl(uri).getReader();
			} else {
		        try {
		            is = new URL(uri).openStream();
		        } catch (java.net.MalformedURLException e) {
					XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_MALFORMED_URL, uri, e);
		        } catch (java.io.FileNotFoundException e) {
					XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_ITEM_AT_URI_NOT_FOUND, uri, e);
		        } catch (java.io.IOException e) {
					XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_IO_PROBLEM_FOR_URI, uri, e);
		        }
			}
        } catch (URISyntaxException e1) {
			XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_MALFORMED_URL, uri, e1);
		}

		return is == null ? null : new InputStreamReader(is, StandardCharsets.UTF_8);
    }

    protected String readAll(Reader reader) throws IOException {
        return OpenUtil.readAll(reader);
    }

    /**
     * Retrieves the CSS located at the given URI.  It's assumed the URI does point to a CSS file--the URI will
     * be resolved, accessed (using the set FSStreamFactory or URL::openStream), opened, read and then passed into the CSS parser.
     * The result is packed up into an CSSResource for later consumption.
     *
     * @param uri Location of the CSS source.
     * @return A CSSResource containing the CSS reader or null if not available.
     */
    @SuppressWarnings("resource")
    @Override
    public CSSResource getCSSResource(String uri, ExternalResourceType type) {
        if (!checkAccessAllowed(uri, type, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)) {
            return null;
        }

        String resolved = _resolver.resolveURI(this._baseUri, uri);
        if (!checkAccessAllowed(resolved, type, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)) {
            return null;
        }

    	if (resolved == null) {
    		XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI, "CSS resource", uri);
    		return null;
    	}

        // Ownership is transferred to CSSResource which implements Closeable.
        return new CSSResource(openReader(resolved));
    }

    public abstract ImageResource getImageResource(String uri, ExternalResourceType type);

    /**
     * Retrieves the XML located at the given URI. It's assumed the URI does point to a XML--the URI will
     * be accessed (using the set HttpStreamFactory or URL::openStream), opened, read and then passed into the XML parser (XMLReader)
     * configured for Flying Saucer. The result is packed up into an XMLResource for later consumption.
     *
     * @param uri Location of the XML source.
     * @return An XMLResource containing the image.
     */
    @Override
    public XMLResource getXMLResource(String uri, ExternalResourceType type) {
        if (!checkAccessAllowed(uri, type, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)) {
            return null;
        }
    	String resolved = _resolver.resolveURI(this._baseUri, uri);
        if (!checkAccessAllowed(resolved, type, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)) {
            return null;
        }

    	if (resolved == null) {
    		XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI, "XML resource", uri);
    		return null;
    	}
    	
        try (Reader inputReader = openReader(resolved)) {
            return inputReader == null ? null :
                        XMLResource.load(inputReader);
        } catch (IOException e) {
            // On auto close, swallow.
            return null;
        }
    }

    @Override
    public byte[] getBinaryResource(String uri, ExternalResourceType type) {
        if (!checkAccessAllowed(uri, type, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)) {
            return null;
        }
        String resolved = _resolver.resolveURI(this._baseUri, uri);
        if (!checkAccessAllowed(resolved, type, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)) {
            return null;
        }

    	if (resolved == null) {
			XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI, "binary resource", uri);
    		return null;
    	}

        try (InputStream is = openStream(resolved)) {
            if (is == null) {
                return null;
            }

            return OpenUtil.readAll(is);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns true if the given URI was visited, meaning it was requested at some point since initialization.
     *
     * @param uri A URI which might have been visited.
     * @return Always false; visits are not tracked in the NaiveUserAgent.
     */
    @Override
    public boolean isVisited(String uri) {
        return false;
    }

    /**
     * URL relative to which URIs are resolved.
     *
     * @param uri A URI which anchors other, possibly relative URIs.
     */
    @Override
    public void setBaseURL(String uri) {
        _baseUri = uri;
    }

    public static class DefaultAccessController
                  implements BiPredicate<String, ExternalResourceType> {

        public boolean test(String uri, ExternalResourceType resourceType) {
            if (resourceType == null) {
                return false;
            }

            switch (resourceType) {
            case BINARY:
            case CSS:
            case FONT:
            case IMAGE_RASTER:
            case XML_XHTML:
            case XML_SVG:
            case PDF:
            case SVG_BINARY:
                return true;
            case FILE_EMBED:
                return false;
            }

            return false;
        }
    }

    public void setAccessController(
            ExternalResourceControlPriority prio,
            BiPredicate<String, ExternalResourceType> controller) {
        this._accessControllers.put(prio, controller);
    }

    public boolean checkAccessAllowed(
            String uriOrResolved,
            ExternalResourceType type,
            ExternalResourceControlPriority priority) {
        BiPredicate<String, ExternalResourceType> controller = this._accessControllers.get(priority);

        if (uriOrResolved == null) {
            return false;
        }

        if (controller == null) {
            return true;
        }

        boolean passed = controller.test(uriOrResolved, type);

        if (!passed) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.LOAD_RESOURCE_ACCESS_REJECTED, uriOrResolved, type);
        }

        return passed;
    }

    public static class DefaultUriResolver implements FSUriResolver {
		/**
		 * Resolves the URI; if absolute, leaves as is, if relative, returns an
		 * absolute URI based on the baseUrl for the agent.
		 *
		 * @param uri
		 *            A URI, possibly relative.
		 *
		 * @return A URI as String, resolved, or null if there was an exception
		 *         (for example if the URI is malformed).
		 */
		@Override
		public String resolveURI(String baseUri, String uri) {
			if (uri == null || uri.isEmpty())
				return null;

			if (uri.startsWith("data:")) {
				return uri; //bypass URI "formatting" check for data uri, as we may have whitespace in the base64 encoded data
			}
			
			try {
				URI possiblyRelative = new URI(uri);
				
				if (possiblyRelative.isAbsolute()) {
					return possiblyRelative.toString();
				} else {
					if (baseUri == null) {
						// If user hasn't provided base URI, just reject resolving relative URIs.
						XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.LOAD_COULD_NOT_RESOLVE_RELATIVE_URI_BECAUSE_NO_BASE_URI_WAS_PROVIDED, uri);
					    return null;
					} else if (baseUri.startsWith("jar")) {
				    	// Fix for OpenHTMLtoPDF issue-#125, URI class doesn't resolve jar: scheme urls and so returns only
				    	// the relative part on calling base.resolve(relative) so we use the URL class instead which does
				    	// understand jar: scheme urls.
				    	URL base = new URL(baseUri);
				        URL absolute = new URL(base, uri);
				        return absolute.toString();
				    } else {
						URI base = new URI(baseUri);
						URI absolute = base.resolve(uri);
						return absolute.toString();				    	
				    }
				}
			} catch (URISyntaxException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId3Param.EXCEPTION_URI_WITH_BASE_URI_INVALID, uri, "", baseUri, e);
				return null;
			} catch (MalformedURLException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId3Param.EXCEPTION_URI_WITH_BASE_URI_INVALID, uri, "jar scheme", baseUri, e);
				return null;
			}
		}
	}

    /**
     * Returns the current baseUrl for this class.
     */
    @Override
    public String getBaseURL() {
        return _baseUri;
    }

    @Override
    @Deprecated
    public void documentStarted() {
        clearImageCache();
    }

    @Override
    @Deprecated
    public void documentLoaded() { /* ignore*/ }

    @Override
    @Deprecated
    public void onLayoutException(Throwable t) { /* ignore*/ }

    @Override
    @Deprecated
    public void onRenderException(Throwable t) { /* ignore*/ }

	@Override
	public String resolveURI(String uri) {
		return _resolver.resolveURI(getBaseURL(), uri);
	}

	@Override
	public String resolveUri(String baseUri, String uri) {
		return _resolver.resolveURI(baseUri, uri);
	}
}

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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.openhtmltopdf.event.DocumentListener;
import com.openhtmltopdf.extend.FSCache;
import com.openhtmltopdf.extend.FSMultiThreadCache;
import com.openhtmltopdf.extend.FSUriResolver;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.resource.CSSResource;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.util.ImageUtil;
import com.openhtmltopdf.util.XRLog;

/**
 * <p>NaiveUserAgent is a simple implementation of {@link UserAgentCallback} which places no restrictions on what
 * XML, CSS or images are loaded, and reports visited links without any filtering. The most straightforward process
 * available in the JDK is used to load the resources in question--either using java.io or java.net classes.
 *
 * <p>The NaiveUserAgent has a small cache for images,
 * the size of which (number of images) can be passed as a constructor argument. There is no automatic cleaning of
 * the cache; call {@link #clearImageCache()} to remove the least-accessed elements--for example, you might do this
 * when a new document is about to be loaded. The NaiveUserAgent is also a DocumentListener; if registered with a
 * source of document events (like the panel hierarchy), it will respond to the
 * {@link com.openhtmltopdf.event.DocumentListener#documentStarted()} call and attempt to shrink its cache.
 *
 * @author Torbjoern Gannholm
 */
public class NaiveUserAgent implements UserAgentCallback, DocumentListener {

    /**
     * a (simple) cache
     * This is only useful for the one run. For more than one run, set an external cache with
     * setFSCache.
     */
    protected final LinkedHashMap<String, ImageResource> _imageCache = new LinkedHashMap<String, ImageResource>();
    protected final FSUriResolver DEFAULT_URI_RESOLVER = new DefaultUriResolver(); 

    protected FSCache _externalCache = new NullFSCache(false);
    protected FSUriResolver _resolver = DEFAULT_URI_RESOLVER;
    protected String _baseUri;
	protected Map<String, FSStreamFactory> _protocolsStreamFactory = new HashMap<String, FSStreamFactory>(2);
	protected FSMultiThreadCache<String> _textCache = new NullCache<String>();
	protected FSMultiThreadCache<byte[]> _byteCache = new NullCache<byte[]>();
	
	protected static class NullCache<T> implements FSMultiThreadCache<T> {
		@Override
		public T get(String uri) {
			return null;
		}

		@Override
		public void put(String uri, T value) {
			// Empty
		}
	}
    
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
				try {
					return new InputStreamReader(this.strm, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					XRLog.exception("Exception when creating stream reader", e);
				}
			}
			return null;
		}
    }
    
    public static class DefaultHttpStreamFactory implements FSStreamFactory {

		@Override
		public FSStream getUrl(String uri) {
			InputStream is = null;
			
	        try {
	            is = new URL(uri).openStream();
	        } catch (java.net.MalformedURLException e) {
	            XRLog.exception("bad URL given: " + uri, e);
	        } catch (java.io.FileNotFoundException e) {
	            XRLog.exception("item at URI " + uri + " not found");
	        } catch (java.io.IOException e) {
	            XRLog.exception("IO problem for " + uri, e);
	        }
	        return new DefaultHttpStream(is);
		}
    }
    
    public static class NullFSCache implements FSCache {
    	
    	private final boolean _log;
    	
    	public NullFSCache(boolean log) {
    		this._log = log;
    	}
    	
		@Override
		public Object get(FSCacheKey cacheKey) {
			if (_log) {
				XRLog.load(Level.INFO, "Trying to retrieve object from cache: " + cacheKey.toString());
			}
			return null;
		}

		@Override
		public void put(FSCacheKey cacheKey, Object obj) {
			if (_log) {
				XRLog.load(Level.INFO, "Trying to put object in cache: " + cacheKey.toString());
			}
		}
    }
    
    public NaiveUserAgent() {
    	FSStreamFactory factory = new DefaultHttpStreamFactory();
    	this._protocolsStreamFactory.put("http", factory);
    	this._protocolsStreamFactory.put("https", factory);
    }
    
    public void setProtocolsStreamFactory(Map<String, FSStreamFactory> protocolsStreamFactory) {
    	this._protocolsStreamFactory = protocolsStreamFactory;
    }
    
    public void setExternalCache(FSCache cache) {
    	this._externalCache = cache;
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
    public void clearImageCache() {
        _imageCache.clear();
    }
    
    protected FSStreamFactory getProtocolFactory(String protocol) {
    	return _protocolsStreamFactory.get(protocol);
    }
    
    protected boolean hasProtocolFactory(String protocol) {
    	return _protocolsStreamFactory.containsKey(protocol);
    }

    /**
     * Gets a InputStream for the resource identified by a resolved URI.
     */
    protected InputStream openStream(String uri) {
        java.io.InputStream is = null;
        
        try {
			URI urlObj = new URI(uri);
			String protocol = urlObj.getScheme();

			if (hasProtocolFactory(protocol)) {
				return getProtocolFactory(protocol).getUrl(uri).getStream();
			}
			else {
		        try {
		            is = new URL(uri).openStream();
		        } catch (java.net.MalformedURLException e) {
		            XRLog.exception("bad URL given: " + uri, e);
		        } catch (java.io.FileNotFoundException e) {
		            XRLog.exception("item at URI " + uri + " not found", e);
		        } catch (java.io.IOException e) {
		            XRLog.exception("IO problem for " + uri, e);
		        }
			}
        } catch (URISyntaxException e1) {
        	XRLog.exception("bad URL given: " + uri, e1);
		}

        return is;
    }

    /**
     * Gets a reader for the identified resource by a resolved URI.
     */
    protected Reader openReader(String uri) {
    	InputStream is = null;
    	
        try {
			URI urlObj = new URI(uri);
			String protocol = urlObj.getScheme();

			if (hasProtocolFactory(protocol)) {
				return getProtocolFactory(protocol).getUrl(uri).getReader();
			}
			else {
		        try {
		            is = new URL(uri).openStream();
		        } catch (java.net.MalformedURLException e) {
		            XRLog.exception("bad URL given: " + uri, e);
		        } catch (java.io.FileNotFoundException e) {
		            XRLog.exception("item at URI " + uri + " not found");
		        } catch (java.io.IOException e) {
		            XRLog.exception("IO problem for " + uri, e);
		        }
			}
        } catch (URISyntaxException e1) {
        	XRLog.exception("bad URL given: " + uri, e1);
		}
    	
    	try {
			return is == null ? null : new InputStreamReader(is, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			XRLog.exception("Failed to create stream reader", e);
		}
    	
    	return null;
    }
    
    protected String getCacheText(String uri) {
    	String text = _textCache.get(uri);
    	
    	if (text != null) {
    		return text;
    	}
    	
    	byte[] bytes = _byteCache.get(uri);
    	
    	if (bytes != null) {
    		try {
				return new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException e) { }
    	}

    	return null;
    }
    
    protected String readAll(Reader reader) throws IOException {
    	char[] arr = new char[8 * 1024];
    	StringBuilder buffer = new StringBuilder();
    	int numCharsRead;
    	while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
    		buffer.append(arr, 0, numCharsRead);
    	}
    	return buffer.toString();
    }
    
    protected Reader getCacheReader(String uri) {
    	String text = getCacheText(uri);
    	
    	if (text != null) {
    		return new StringReader(text);
    	}
    	
    	return null;
    }
    
    protected InputStream getCacheStream(String uri) {
    	byte[] bytes = _byteCache.get(uri);
    	
    	if (bytes != null) {
    		return new ByteArrayInputStream(bytes);
    	}
    	
    	return null;
    }
    
    /**
     * Retrieves the CSS located at the given URI.  It's assumed the URI does point to a CSS file--the URI will
     * be resolved, accessed (using the set FSStreamFactory or URL::openStream), opened, read and then passed into the CSS parser.
     * The result is packed up into an CSSResource for later consumption.
     *
     * @param uri Location of the CSS source.
     * @return A CSSResource containing the CSS reader or null if not available.
     */
    @Override
    public CSSResource getCSSResource(String uri) {
    	String resolved = _resolver.resolveURI(this._baseUri, uri);
    	
    	if (resolved == null) {
    		XRLog.load(Level.INFO, "URI resolver rejected loading CSS resource at (" + uri + ")");
    		return null;
    	}
    	
    	Reader reader = getCacheReader(resolved);
    	
    	if (reader != null) {
    		return new CSSResource(reader);
    	}
    	
		if (!(_textCache instanceof NullCache)) {
			Reader res = null;
			
			try {
				res = openReader(resolved);

				if (res != null) {
					String css = readAll(res);
					_textCache.put(resolved, css);
					return new CSSResource(new StringReader(css));
				}
			} catch (IOException e) {
				XRLog.cssParse(Level.WARNING, "Couldn't load stylesheet at URI " + uri + ": " + e.getMessage(), e);
			} finally {
				if (res != null) {
					try {
						res.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			return new CSSResource(openReader(resolved));
		}

    	return null;
    }

    /**
     * Retrieves the image located at the given URI. It's assumed the URI does point to an image--the URI will
     * be accessed (using the set HttpStreamFactory or URL::openStream), opened, read and then passed into the JDK image-parsing routines.
     * The result is packed up into an ImageResource for later consumption.
     *
     * @param uri Location of the image source.
     * @return An ImageResource containing the image.
     */
    @Override
    public ImageResource getImageResource(String uri) {
    	System.out.println("Getting image: " + uri);
        ImageResource ir;
        
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            BufferedImage image = ImageUtil.loadEmbeddedBase64Image(uri);
            return new ImageResource(null, AWTFSImage.createImage(image));
        } else {
            String resolved = _resolver.resolveURI(this._baseUri, uri);
            
            if (resolved == null) {
        		XRLog.load(Level.INFO, "URI resolver rejected loading image resource at (" + uri + ")");
        		return null;
        	}
            
            // First, we check the internal per run cache.
            ir = _imageCache.get(resolved);
            if (ir != null) {
            	return ir;
            }
            
           	// Then check the external multi run cache.
            AWTFSImage fsImage = (AWTFSImage) _externalCache.get(new FSCacheKey(resolved, AWTFSImage.class));
            if (fsImage != null) {
            	return new ImageResource(resolved, fsImage);
            }
            
            // Finally we fetch from the network or file, etc.
            InputStream is = openStream(resolved);

            if (is != null) {
                    try {
                        BufferedImage img = ImageIO.read(is);
                        if (img == null) {
                            throw new IOException("ImageIO.read() returned null");
                        }
                        
                        AWTFSImage fsImage2 = (AWTFSImage) AWTFSImage.createImage(img);
                        _externalCache.put(new FSCacheKey(resolved, AWTFSImage.class), fsImage2);
                        
                        ir = new ImageResource(resolved, fsImage2);
                        _imageCache.put(resolved, ir);
                        
                        return ir;
                    } catch (FileNotFoundException e) {
                        XRLog.exception("Can't read image file; image at URI '" + resolved + "' not found");
                    } catch (IOException e) {
                        XRLog.exception("Can't read image file; unexpected problem for URI '" + resolved + "'", e);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            
            	return new ImageResource(resolved, null);
        }
    }

    /**
     * Retrieves the XML located at the given URI. It's assumed the URI does point to a XML--the URI will
     * be accessed (using the set HttpStreamFactory or URL::openStream), opened, read and then passed into the XML parser (XMLReader)
     * configured for Flying Saucer. The result is packed up into an XMLResource for later consumption.
     *
     * @param uri Location of the XML source.
     * @return An XMLResource containing the image.
     */
    @Override
    public XMLResource getXMLResource(String uri) {
    	String resolved = _resolver.resolveURI(this._baseUri, uri);
    	
    	if (resolved == null) {
    		XRLog.load(Level.INFO, "URI resolver rejected loading XML resource at (" + uri + ")");
    		return null;
    	}
    	
    	XMLResource res = (XMLResource) _externalCache.get(new FSCacheKey(resolved, XMLResource.class));
    	if (res != null) {
    		return res;
    	}
    	
        Reader inputReader = openReader(resolved);
        XMLResource xmlResource;

        try {
            xmlResource = XMLResource.load(inputReader);
        } finally {
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (IOException e) {
                    // swallow
                }
            }
        }
        _externalCache.put(new FSCacheKey(resolved, XMLResource.class), xmlResource);
        return xmlResource;
    }

    @Override
    public byte[] getBinaryResource(String uri) {
    	String resolved = _resolver.resolveURI(this._baseUri, uri);
    	
    	if (resolved == null) {
    		XRLog.load(Level.INFO, "URI resolver rejected loading binary resource at (" + uri + ")");
    		return null;
    	}
    	
    	byte[] bytes = (byte[]) _externalCache.get(new FSCacheKey(resolved, byte[].class));
    	if (bytes != null) {
    		return bytes;
    	}
    	
        InputStream is = openStream(resolved);
        if (is == null) {
        	return null;
        }
        
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buf = new byte[10240];
            int i;
            while ((i = is.read(buf)) != -1) {
                result.write(buf, 0, i);
            }
            is.close();
            is = null;

            byte[] bytes2 = result.toByteArray();
            _externalCache.put(new FSCacheKey(resolved, byte[].class), bytes2);
            return bytes2;
        } catch (IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
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
			
			try {
				URI possiblyRelative = new URI(uri);
				
				if (possiblyRelative.isAbsolute()) {
					return possiblyRelative.toString();
				} else {
					if (baseUri == null) {
						// If user hasn't provided base URI, just reject resolving relative URIs.
						XRLog.load(Level.WARNING, "Couldn't resolve relative URI(" + uri + ") because no base URI was provided.");
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
				XRLog.exception("When trying to load uri(" + uri + ") with base URI(" + baseUri + "), one or both were invalid URIs.", e);
				return null;
			} catch (MalformedURLException e) {
				XRLog.exception("When trying to load uri(" + uri + ") with base jar scheme URI(" + baseUri + "), one or both were invalid URIs.", e);
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
    public void documentStarted() {
        clearImageCache();
    }

    @Override
    public void documentLoaded() { /* ignore*/ }

    @Override
    public void onLayoutException(Throwable t) { /* ignore*/ }

    @Override
    public void onRenderException(Throwable t) { /* ignore*/ }

	@Override
	public String resolveURI(String uri) {
		return _resolver.resolveURI(getBaseURL(), uri);
	}

	@Override
	public String resolveUri(String baseUri, String uri) {
		return _resolver.resolveURI(baseUri, uri);
	}
	
    public void setExternalTextCache(FSMultiThreadCache<String> textCache) {
        this._textCache = textCache;
        
    }

    public void setExternalByteCache(FSMultiThreadCache<byte[]> byteCache) {
    	this._byteCache = byteCache;
    }
}

/*
 * $Id$
 *
 * $Log$
 * Revision 1.40  2009/05/15 16:20:10  pdoubleya
 * ImageResource now tracks the URI for the image that was created and handles mutable images.
 *
 * Revision 1.39  2009/04/12 11:16:51  pdoubleya
 * Remove proposed patch for URLs that are incorrectly handled on Windows; need a more reliable solution.
 *
 * Revision 1.38  2008/04/30 23:14:18  peterbrant
 * Do a better job of cleaning up open file streams (patch by Christophe Marchand)
 *
 * Revision 1.37  2007/11/23 07:03:30  pdoubleya
 * Applied patch from N. Barozzi to allow either toolkit or buffered images to be used, see https://xhtmlrenderer.dev.java.net/servlets/ReadMsg?list=dev&msgNo=3847
 *
 * Revision 1.36  2007/10/31 23:14:43  peterbrant
 * Add rudimentary support for @font-face rules
 *
 * Revision 1.35  2007/06/20 12:24:31  pdoubleya
 * Fix bug in shrink cache, trying to modify iterator without using safe remove().
 *
 * Revision 1.34  2007/06/19 21:25:41  pdoubleya
 * Cleanup for caching in NUA, making it more suitable to use as a reusable UAC. NUA is also now a document listener and uses this to try and trim its cache down. PanelManager and iTextUA are now NUA subclasses.
 *
 * Revision 1.33  2007/05/20 23:25:33  peterbrant
 * Various code cleanups (e.g. remove unused imports)
 *
 * Patch from Sean Bright
 *
 * Revision 1.32  2007/05/09 21:52:06  pdoubleya
 * Fix for rendering problems introduced by removing GraphicsUtil class. Use Image instead of BufferedImage in most cases, convert to AWT image if necessary. Not complete, requires cleanup.
 *
 * Revision 1.31  2007/05/05 21:08:27  pdoubleya
 * Changed image-related interfaces (FSImage, ImageUtil, scaling) to all use BufferedImage, since there were no Image-specific APIs we depended on, and we have more control over what we do with BIs as compared to Is.
 *
 * Revision 1.30  2007/05/05 18:05:21  pdoubleya
 * Remove references to GraphicsUtil and the class itself, no longer needed
 *
 * Revision 1.29  2007/04/10 20:46:02  pdoubleya
 * Fix, was not closing XML source stream when done
 *
 * Revision 1.28  2007/02/07 16:33:31  peterbrant
 * Initial commit of rewritten table support and associated refactorings
 *
 * Revision 1.27  2006/06/28 13:46:59  peterbrant
 * ImageIO.read() can apparently return sometimes null instead of throwing an exception when processing an invalid image
 *
 * Revision 1.26  2006/04/27 13:28:48  tobega
 * Handle situations without base url and no file access gracefully
 *
 * Revision 1.25  2006/04/25 00:23:20  peterbrant
 * Fixes from Mike Curtis
 *
 * Revision 1.23  2006/04/08 08:21:24  tobega
 * relative urls and linked stylesheets
 *
 * Revision 1.22  2006/02/02 02:47:33  peterbrant
 * Support non-AWT images
 *
 * Revision 1.21  2005/10/25 19:40:38  tobega
 * Suggestion from user to use File.toURI.toURL instead of File.toURL because the latter is buggy
 *
 * Revision 1.20  2005/10/09 09:40:27  tobega
 * Use current directory as default base URL
 *
 * Revision 1.19  2005/08/11 01:35:37  joshy
 * removed debugging
 * updated stylesheet to use right aligns
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.17  2005/06/25 19:27:47  tobega
 * UAC now supplies Resources
 *
 * Revision 1.16  2005/06/25 17:23:35  tobega
 * first refactoring of UAC: ImageResource
 *
 * Revision 1.15  2005/06/21 17:52:10  joshy
 * new hover code
 * removed some debug statements
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.14  2005/06/20 23:45:56  joshy
 * hack to fix the mangled background images on osx
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.13  2005/06/20 17:26:45  joshy
 * debugging for image issues
 * font scale stuff
 *
 * Issue number:
 * Obtained from:
 * Submitted by:
 * Reviewed by:
 *
 * Revision 1.12  2005/06/15 11:57:18  tobega
 * Making Browser a better model application with UserAgentCallback
 *
 * Revision 1.11  2005/06/15 11:53:47  tobega
 * Changed UserAgentCallback to getInputStream instead of getReader. Fixed up some consequences of previous change.
 *
 * Revision 1.10  2005/06/13 06:50:16  tobega
 * Fixed a bug in table content resolution.
 * Various "tweaks" in other stuff.
 *
 * Revision 1.9  2005/06/03 00:29:49  tobega
 * fixed potential bug
 *
 * Revision 1.8  2005/06/01 21:36:44  tobega
 * Got image scaling working, and did some refactoring along the way
 *
 * Revision 1.7  2005/03/28 14:24:22  pdoubleya
 * Remove stack trace on loading images.
 *
 * Revision 1.6  2005/02/02 12:14:01  pdoubleya
 * Clean, format, buffer reader.
 *
 *
 */

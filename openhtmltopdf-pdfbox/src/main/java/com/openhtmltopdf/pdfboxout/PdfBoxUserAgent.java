/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Torbj�rn Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Level;

import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.swing.FSCacheKey;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.util.ImageUtil;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxUserAgent extends NaiveUserAgent {
    private SharedContext _sharedContext;

    private final PdfBoxOutputDevice _outputDevice;

    public PdfBoxUserAgent(PdfBoxOutputDevice outputDevice) {
		super();
		_outputDevice = outputDevice;
    }

    private byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
        byte[] buf = new byte[10240];
        int i;
        while ( (i = is.read(buf)) != -1) {
            out.write(buf, 0, i);
        }
        out.close();
        return out.toByteArray();
    }
    
    public ImageResource getImageResource(String uriStr) {
        String uriResolved = resolveURI(uriStr);
        
        if (uriResolved == null) {
           XRLog.load(Level.INFO, "URI resolver rejected loading image at (" + uriStr + ")");
           return new ImageResource(uriStr, null);
        }
        
        ImageResource resource = _imageCache.get(uriResolved);
        
        if (resource == null) {
            resource = (ImageResource) _externalCache.get(new FSCacheKey(uriResolved, PdfBoxImage.class));
        }

        if (resource != null && resource.getImage() instanceof PdfBoxImage) {
            // Make copy of PdfBoxImage so we don't stuff up the cache.
            PdfBoxImage original = (PdfBoxImage) resource.getImage();
            PdfBoxImage copy = new PdfBoxImage(original.getBytes(), original.getUri(), original.getWidth(), original.getHeight(), original.isJpeg(), original.getXObject());
            return new ImageResource(resource.getImageUri(), copy);
        }
        
        if (ImageUtil.isEmbeddedBase64Image(uriResolved)) {
            resource = loadEmbeddedBase64ImageResource(uriResolved);
            _outputDevice.realizeImage((PdfBoxImage) resource.getImage());
            _imageCache.put(uriResolved, resource);
        } else {
            InputStream is = openStream(uriResolved);
            
            if (is != null) {
                try {
                    URI uri = new URI(uriStr);
                    if (uri.getPath() != null
                        && uri.getPath().toLowerCase(Locale.US)
                                    .endsWith(".pdf")) {
                        // TODO: Implement PDF AS IMAGE
                        // PdfReader reader = _outputDevice.getReader(uri);
                        // PDFAsImage image = new PDFAsImage(uri);
                        // Rectangle rect = reader.getPageSizeWithRotation(1);
                        // image.setInitialWidth(rect.getWidth() *
                        // _outputDevice.getDotsPerPoint());
                        // image.setInitialHeight(rect.getHeight() *
                        // _outputDevice.getDotsPerPoint());
                        // resource = new ImageResource(uriStr, image);
                    } else {
                        byte[] imgBytes = readStream(is);
                        PdfBoxImage fsImage = new PdfBoxImage(imgBytes, uriStr);
                        scaleToOutputResolution(fsImage);
                        _outputDevice.realizeImage(fsImage);
                        resource = new ImageResource(uriResolved, fsImage);
                    }
                    _imageCache.put(uriResolved, resource);
                    _externalCache.put(new FSCacheKey(uriResolved, PdfBoxImage.class), resource);
                } catch (Exception e) {
                    XRLog.exception(
                            "Can't read image file; unexpected problem for URI '"
                                    + uriStr + "'", e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            if (resource != null) {
                resource = new ImageResource(resource.getImageUri(), resource.getImage());
            } else {
                resource = new ImageResource(uriStr, null);
            }
        }
        return resource;
    }
    
    private ImageResource loadEmbeddedBase64ImageResource(final String uri) {
        try {
            byte[] buffer = ImageUtil.getEmbeddedBase64Image(uri);
            PdfBoxImage fsImage = new PdfBoxImage(buffer, uri);
            scaleToOutputResolution(fsImage);
            return new ImageResource(null, fsImage);
        } catch (Exception e) {
            XRLog.exception("Can't read XHTML embedded image.", e);
        }
        return new ImageResource(null, null);
    }

    private void scaleToOutputResolution(PdfBoxImage image) {
        float factor = _sharedContext.getDotsPerPixel();
        if (factor != 1.0f) {
            image.scale((int) (image.getWidth() * factor), (int) (image.getHeight() * factor));
        }
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void setSharedContext(SharedContext sharedContext) {
        _sharedContext = sharedContext;
    }
}

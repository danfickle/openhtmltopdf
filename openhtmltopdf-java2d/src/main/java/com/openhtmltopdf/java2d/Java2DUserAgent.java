package com.openhtmltopdf.java2d;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.openhtmltopdf.java2d.image.AWTFSImage;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

public class Java2DUserAgent extends NaiveUserAgent {

    /**
     * Retrieves the image located at the given URI. It's assumed the URI does point to an image--the URI will
     * be accessed (using the set HttpStreamFactory or URL::openStream), opened, read and then passed into the JDK image-parsing routines.
     * The result is packed up into an ImageResource for later consumption.
     *
     * @param uri Location of the image source.
     * @return An ImageResource containing the image.
     */
    @Override
    public ImageResource getImageResource(String uri, ExternalResourceType type) {
        ImageResource ir;

        if (!checkAccessAllowed(uri, type, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)) {
            return null;
        }

        String resolved = _resolver.resolveURI(this._baseUri, uri);

        if (!checkAccessAllowed(resolved, type, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)) {
            return null;
        }

        if (resolved == null) {
            XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI, "image resource", uri);
            return null;
        }

        // First, we check the internal per run cache.
        ir = _imageCache.get(resolved);
        if (ir != null) {
            return ir;
        }

        // Finally we fetch from the network or file, etc.
        try (InputStream is = openStream(resolved)) {
            if (is != null) {

                BufferedImage img = ImageIO.read(is);

                if (img == null) {
                    throw new IOException("ImageIO.read() returned null");
                }

                AWTFSImage fsImage2 = (AWTFSImage) AWTFSImage.createImage(img);

                ir = new ImageResource(resolved, fsImage2);
                _imageCache.put(resolved, ir);

                return ir;
            }
        } catch (FileNotFoundException e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI_NOT_FOUND, resolved);
        } catch (IOException e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI, uri, e);
        }

        // Failed.
        return new ImageResource(resolved, null);
    }
}

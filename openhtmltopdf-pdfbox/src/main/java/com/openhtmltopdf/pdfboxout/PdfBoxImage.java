package com.openhtmltopdf.pdfboxout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxImage implements FSImage {
    private byte[] _bytes;
    private final String _uri;

    private float _intrinsicWidth;
    private float _intrinsicHeight;

    private final boolean _isJpeg;

    private PDImageXObject _xobject;
    
    public PdfBoxImage(byte[] image, String uri) throws IOException {
        _bytes = image;
        _uri = uri;

        ImageInputStream in = ImageIO
                .createImageInputStream(new ByteArrayInputStream(_bytes));
        ImageReader reader = null;
        
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);

            if (readers.hasNext()) {
                reader = readers.next();
                reader.setInput(in);
                _intrinsicWidth = reader.getWidth(0);
                _intrinsicHeight = reader.getHeight(0);

                String type = reader.getFormatName();

                _isJpeg = (type.equalsIgnoreCase("jpeg")
                        || type.equalsIgnoreCase("jpg") || type
                        .equalsIgnoreCase("jfif"));
            } else {
                XRLog.load(Level.WARNING, "Unrecognized image format for: " + uri);
                // TODO: Avoid throw here.
                throw new IOException("Unrecognized Image format");
            }
        } finally {
            if (in != null)
                in.close();
            if (reader != null)
                reader.dispose();
        }
    }

    public PdfBoxImage(byte[] bytes, String uri, float width, float height,
            boolean isJpeg, PDImageXObject xobject) {
        this._bytes = bytes;
        this._uri = uri;
        this._intrinsicWidth = width;
        this._intrinsicHeight = height;
        this._isJpeg = isJpeg;
        this._xobject = xobject;
    }

    public FSImage scaleToOutputResolution(float dotsPerPixel) {
        float factor = dotsPerPixel;
        float width = _intrinsicWidth;
        float height = _intrinsicHeight;

        if (factor != 1.0f) {
            width *= factor;
            height *= factor;
        }

        return new PdfBoxImage(_bytes, _uri, width, height, _isJpeg, _xobject);
    }

    @Override
    public int getWidth() {
        return (int) _intrinsicWidth;
    }

    @Override
    public int getHeight() {
        return (int) _intrinsicHeight;
    }

    @Override
    public void scale(int width, int height) {
        float setWidth, setHeight;

        if (width != -1) {
            setWidth = width;

            if (height == -1 && _intrinsicWidth != 0) {
                // Use the width ratio to set the height.
                setHeight = (int) (((float) setWidth / (float) _intrinsicWidth) * _intrinsicHeight);
            } else {
                setHeight = height;
            }
        } else if (height != -1) {
            setHeight = height;

            if (_intrinsicHeight != 0) {
                // Use the height ratio to set the width.
                setWidth = (int) (((float) setHeight / (float) _intrinsicHeight) * _intrinsicWidth);
            } else {
                setWidth = 0;
            }
        } else {
            setWidth = _intrinsicWidth;
            setHeight = _intrinsicHeight;
        }

        // TODO: Make this class immutable.
        this._intrinsicWidth = setWidth;
        this._intrinsicHeight = setHeight;
    }

    public byte[] getBytes() {
        return _bytes;
    }

    public void clearBytes() {
        _bytes = null;
    }
    
    public PDImageXObject getXObject() {
        return _xobject;
    }
    
    public void setXObject(PDImageXObject xobject) {
        _xobject = xobject;
    }
    
    public String getUri() {
        return _uri;
    }

    public boolean isJpeg() {
        return _isJpeg;
    }
}

package com.openhtmltopdf.pdfboxout;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.openhtmltopdf.util.LogMessageId;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.util.XRLog;

public class PdfBoxImage implements FSImage {
    private byte[] _bytes;
    private final String _uri;

    private float _intrinsicWidth;
    private float _intrinsicHeight;

    private PDImageXObject _xobject;
    
    public PdfBoxImage(byte[] image, String uri) throws IOException {
        _bytes = image;
        _uri = uri;

        ImageReader reader = null;
        
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(_bytes))){
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);

            if (readers.hasNext()) {
                reader = readers.next();
                reader.setInput(in);
                
                int rotation = getImageRotation(image);
                if (rotation != 0) {
                    BufferedImage newimg = rotateImage(reader.read(0), rotation);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(newimg, "jpg", baos);
                    _bytes = baos.toByteArray();
                    _intrinsicWidth = newimg.getWidth();
                    _intrinsicHeight = newimg.getHeight();
                } else {
                    _intrinsicWidth = reader.getWidth(0);
                    _intrinsicHeight = reader.getHeight(0);
                }
            } else {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.LOAD_UNRECOGNIZED_IMAGE_FORMAT_FOR_URI, uri);
                // TODO: Avoid throw here.
                throw new IOException("Unrecognized Image format");
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    public PdfBoxImage(byte[] bytes, String uri, float width, float height,
            PDImageXObject xobject) {
        this._bytes = bytes;
        this._uri = uri;
        this._intrinsicWidth = width;
        this._intrinsicHeight = height;
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

        return new PdfBoxImage(_bytes, _uri, width, height, _xobject);
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
                setHeight = (int) ((setWidth / _intrinsicWidth) * _intrinsicHeight);
            } else {
                setHeight = height;
            }
        } else if (height != -1) {
            setHeight = height;

            if (_intrinsicHeight != 0) {
                // Use the height ratio to set the width.
                setWidth = (int) ((setHeight / _intrinsicHeight) * _intrinsicWidth);
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
    
    private int getImageRotation( byte[] image ) {
        // Get image rotation from the metadata
        int rotation = 0;

        final ImageMetadata metadata;
        try {
            metadata = Imaging.getMetadata(image);
            if (metadata instanceof JpegImageMetadata) {
                final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                TiffField orientation = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_ORIENTATION);
                if (orientation != null) {
                    switch ((Short) orientation.getValue()) {
                        case TiffTagConstants.ORIENTATION_VALUE_ROTATE_180:
                            rotation = 180;
                            break;
                        case TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW:
                            rotation = 270;
                            break;
                        case TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW:
                            rotation = 90;
                            break;
                        default:
                            // No need.  We default the setting to 0...
                            break;
                    }
                }
            }
        } catch (ImageReadException | IOException ex) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_IMAGE_METADATA_COULD_NOT_BE_FOUND, ex);            
        }
        
        return rotation;
    }

    private static BufferedImage rotateImage(BufferedImage img, int angle) {  
        int iw = img.getWidth();  
        int ih = img.getHeight();
        int rotx, roty;
        BufferedImage rimg;

        // Create a rotated image buffer and establish the center of rotation 
        // from the rotation angle.
        switch (angle) {
            case 90:
                rimg = new BufferedImage(ih, iw, img.getType());
                rotx = roty = ih/2;
                break;
            case 180:
                rimg = new BufferedImage(iw, ih, img.getType());
                rotx = iw/2;
                roty = ih/2;
                break;
            case 270:
                rimg = new BufferedImage(ih, iw, img.getType());
                rotx = roty = iw/2;
                break;
            default: 
                // Any other angle: take no action
                return img;
        }
        
        // Perform the rotation by drawing a rotated version of the original
        // image into the new (rotated) image buffer.
        Graphics2D g2d = rimg.createGraphics();
        AffineTransform transformer = new AffineTransform();
        transformer.rotate(Math.toRadians(angle), rotx, roty);
        g2d.drawRenderedImage(img, transformer);
        g2d.dispose();
        
        return rimg;  
    }

}

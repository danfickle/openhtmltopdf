package com.openhtmltopdf.simple.extend;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

public class ReplacedElementScaleHelper {
    /**
     * Creates a scale <code>AffineTransform</code> to scale a given replaced element to the desired size.
     * @param dotsPerPixel
     * @param contentBounds the desired size
     * @param width the intrinsic width
     * @param height the intrinsic height
     * @return AffineTransform or null if not available.
     */
    public static AffineTransform createScaleTransform(double dotsPerPixel, Rectangle contentBounds, float width, float height) {
        double intrinsicWidth = width;
        double intrinsicHeight = height;
        
        double desiredWidth = (contentBounds.getWidth() / dotsPerPixel);
        double desiredHeight = (contentBounds.getHeight() / dotsPerPixel);
        
        AffineTransform scale = null;

        if (width == 0 || height == 0) {
            // Do nothing...
        }
        else if (desiredWidth > intrinsicWidth ||
                 desiredHeight > intrinsicHeight) {
           
            double rw = desiredWidth / width;
            double rh = desiredHeight / height;
            
            double factor = Math.min(rw, rh);
            scale = AffineTransform.getScaleInstance(factor, factor);
        } else if (desiredWidth < intrinsicWidth &&
                   desiredHeight < intrinsicHeight) {
            double rw = desiredWidth / width;
            double rh = desiredHeight / height;
            
            double factor = Math.max(rw, rh);
            scale = AffineTransform.getScaleInstance(factor, factor);
        }

        return scale;
    }
    
    public static AffineTransform inverseOrNull(AffineTransform in) {
        if (in == null) {
            return null;
        }
        try {
            return in.createInverse();
        } catch (NoninvertibleTransformException e) {
            return null;
        }
    }
}

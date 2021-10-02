package com.openhtmltopdf.pdfboxout;

import java.util.List;
import java.awt.geom.*;
import java.awt.Rectangle;
import java.awt.Shape;

import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.derived.FSLinearGradient;
import com.openhtmltopdf.css.style.derived.FSLinearGradient.StopPoint;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;

public class GradientHelper {
    /**
     * This method is used for creating linear gradient with its components.
     * 
     * @return shading for rendering linear gradient in PDF
     */
    public static PDShading createLinearGradient(PdfBoxFastOutputDevice od, AffineTransform transform, FSLinearGradient gradient, Shape bounds)
    {
        PDShadingType2 shading = new PDShadingType2(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(PDDeviceRGB.INSTANCE);

        Rectangle rect = bounds.getBounds();

        Point2D ptStart = new Point2D.Float(gradient.getX1() + (float) rect.getMinX(), gradient.getY1() + (float) rect.getMinY());
        Point2D ptEnd = new Point2D.Float(gradient.getX2() + (float) rect.getMinX(), gradient.getY2() + (float) rect.getMinY());

        Point2D ptStartDevice = transform.transform(ptStart, null);
        Point2D ptEndDevice = transform.transform(ptEnd, null);

        float startX = (float) ptStartDevice.getX();
        float startY = od.normalizeY((float) ptStartDevice.getY());
        float endX = (float) ptEndDevice.getX();
        float endY = od.normalizeY((float) ptEndDevice.getY());

        COSArray coords = new COSArray();
        coords.add(new COSFloat(startX));
        coords.add(new COSFloat(startY));
        coords.add(new COSFloat(endX));
        coords.add(new COSFloat(endY));
        shading.setCoords(coords);

        PDFunctionType3 type3 = buildType3Function(gradient.getStopPoints(), (float) ptEnd.distance(ptStart));

        COSArray extend = new COSArray();
        extend.add(COSBoolean.FALSE);
        extend.add(COSBoolean.FALSE);
        shading.setFunction(type3);
        shading.setExtend(extend);
        return shading;
    }

    /**
     * This method is used for setting colour lengths to linear gradient.
     * 
     * @return the function, which is an important parameter for setting linear
     *         gradient.
     * @param stopPoints
     *            colours and lengths of linear gradient.
     */
    private static PDFunctionType3 buildType3Function(List<StopPoint> stopPoints, float distance) {
        float max = stopPoints.get(stopPoints.size() - 1).getLength();

        COSDictionary function = new COSDictionary();
        function.setInt(COSName.FUNCTION_TYPE, 3);

        COSArray domain = new COSArray();
        domain.add(new COSFloat(0));
        domain.add(new COSFloat(1));

        COSArray encode = new COSArray();

        COSArray range = new COSArray();
        range.add(new COSFloat(0));
        range.add(new COSFloat(1));
        COSArray bounds = new COSArray();
        for (int i = 1; i < stopPoints.size() - 1; i++) {
            float pos = ((stopPoints.get(i).getLength() / max) * distance) * (1 / distance);
            bounds.add(new COSFloat(pos));
        }

        COSArray functions = buildType2Functions(stopPoints, domain, encode);

        function.setItem(COSName.FUNCTIONS, functions);
        function.setItem(COSName.BOUNDS, bounds);
        function.setItem(COSName.ENCODE, encode);
        PDFunctionType3 type3 = new PDFunctionType3(function);
        type3.setDomainValues(domain);
        return type3;
    }

    /**
     * This method is used for setting colours to linear gradient.
     * 
     * @return the COSArray, which is an important parameter for setting linear
     *         gradient.
     * @param stopPoints
     *            colours to use.
     * @param domain
     *            parameter for setting functiontype2
     * @param encode
     *            encoding COSArray
     */
    private static COSArray buildType2Functions(List<StopPoint> stopPoints, COSArray domain, COSArray encode)
    {
        FSRGBColor prevColor = (FSRGBColor) stopPoints.get(0).getColor();

        COSArray functions = new COSArray();
        for (int i = 1; i < stopPoints.size(); i++)
        {

            FSRGBColor color = (FSRGBColor) stopPoints.get(i).getColor();

            float[] component = new float[] { prevColor.getRed() / 255f, prevColor.getGreen() / 255f, prevColor.getBlue() / 255f };
            PDColor prevPdColor = new PDColor(component, PDDeviceRGB.INSTANCE);

            float[] component1 = new float[] { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f };
            PDColor pdColor = new PDColor(component1, PDDeviceRGB.INSTANCE);

            COSArray c0 = new COSArray();
            COSArray c1 = new COSArray();
            for (float component2 : prevPdColor.getComponents())
                c0.add(new COSFloat(component2));
            for (float component3 : pdColor.getComponents())
                c1.add(new COSFloat(component3));

            COSDictionary type2Function = new COSDictionary();
            type2Function.setInt(COSName.FUNCTION_TYPE, 2);
            type2Function.setItem(COSName.C0, c0);
            type2Function.setItem(COSName.C1, c1);
            type2Function.setInt(COSName.N, 1);
            type2Function.setItem(COSName.DOMAIN, domain);
            functions.add(type2Function);

            encode.add(new COSFloat(0));
            encode.add(new COSFloat(1));
            prevColor = color;
        }
        return functions;
    }
}

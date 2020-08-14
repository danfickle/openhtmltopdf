package com.openhtmltopdf.objects.zxing;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.pdf417.encoder.Dimensions;
import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.util.*;
import java.util.logging.Level;

public class ZXingObjectDrawer implements FSObjectDrawer {


    private static Object handleValueForHint(EncodeHintType type, String value) {
        switch (type) {
            case DATA_MATRIX_SHAPE:
                return safeSymbolShapeHint(value);
            case PDF417_DIMENSIONS: {
                try {
                    int[] dim = Arrays.stream(value.trim().split(",")).mapToInt(Integer::parseInt).toArray();
                    if (dim.length == 4) {
                        return new Dimensions(dim[0], dim[1], dim[2], dim[3]);
                    }
                } catch (NumberFormatException nfe) {
                }
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.GENERAL_UNABLE_TO_PARSE_VALUE_AS, value, Dimensions.class.getCanonicalName());
                return null;
            }
            default:
                return value;
        }
    }

    private static SymbolShapeHint safeSymbolShapeHint(String value) {
        try {
            return SymbolShapeHint.valueOf(value);
        } catch (IllegalArgumentException e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.GENERAL_UNABLE_TO_PARSE_VALUE_AS, value, SymbolShapeHint.class.getCanonicalName(), e);
            return null;
        }
    }

    private static EncodeHintType safeEncodeHintTypeValueOf(String value) {
        try {
            return EncodeHintType.valueOf(value);
        } catch (IllegalArgumentException e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.GENERAL_UNABLE_TO_PARSE_VALUE_AS, value, EncodeHintType.class.getCanonicalName(), e);
            return null;
        }
    }

    private static int parseInt(String value, int defaultColor) {
        try {
            return Long.decode(value.toLowerCase(Locale.ROOT)).intValue();
        } catch (NumberFormatException nfe) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.GENERAL_UNABLE_TO_PARSE_VALUE_AS, value, "integer", nfe);
            return defaultColor;
        }
    }

    @Override
    public Map<Shape, String> drawObject(Element e, double x, double y, double width, double height, OutputDevice outputDevice, RenderingContext ctx, int dotsPerPixel) {
        MultiFormatWriter mfw = new MultiFormatWriter();
        int onColor = e.hasAttribute("on-color") ? parseInt(e.getAttribute("on-color"), MatrixToImageConfig.BLACK) : MatrixToImageConfig.BLACK;
        int offColor = e.hasAttribute("off-color") ? parseInt(e.getAttribute("off-color"), MatrixToImageConfig.WHITE) : MatrixToImageConfig.WHITE;

        Map<EncodeHintType, Object> encodeHints = new EnumMap<>(EncodeHintType.class);
        encodeHints.put(EncodeHintType.MARGIN, 0); // default
        NodeList childNodes = e.getChildNodes();
        int childNodesCount = childNodes.getLength();
        for (int i = 0; i < childNodesCount; i++) {
            Node n = childNodes.item(i);
            if (!(n instanceof Element))
                continue;
            Element eChild = (Element) n;
            if (!"encode-hint".equals(eChild.getTagName())) {
                continue;
            }
            EncodeHintType encodeHintType = safeEncodeHintTypeValueOf(eChild.getAttribute("name"));
            Object value = encodeHintType != null ? handleValueForHint(encodeHintType, eChild.getAttribute("value")) : null;
            if (encodeHintType != null && value != null) {
                encodeHints.put(encodeHintType, value);
            }
        }

        String value = e.getAttribute("value");
        BarcodeFormat barcodeFormat = e.hasAttribute("format") ? BarcodeFormat.valueOf(e.getAttribute("format")) : BarcodeFormat.QR_CODE;

        int finalWidth = (int) (width/dotsPerPixel);
        int finalHeight = (int) (height/dotsPerPixel);
        try {
            BitMatrix bitMatrix = mfw.encode(value, barcodeFormat, finalWidth, finalHeight, encodeHints);

            outputDevice.drawWithGraphics((float) x, (float) y, (float) width, (float) height, graphics2D -> {
                //generating a vector from the bitmatrix don't seems to be straightforward, thus a bitmap image...
                graphics2D.drawImage(MatrixToImageWriter.toBufferedImage(bitMatrix, new MatrixToImageConfig(onColor, offColor)), 0, 0, finalWidth, finalHeight, null);
            });
        } catch (WriterException we) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_MESSAGE, "Error while generating the barcode", we);
        }
        return null;
    }
}

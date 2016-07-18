package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import com.openhtmltopdf.util.XRLog;

public class PdfContentStreamAdapter {
    private final PDPageContentStream cs;

    public static class PdfException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PdfException(String method, Exception cause) {
            super(method, cause);
        }
    }

    private void logAndThrow(String method, IOException e) {
        XRLog.exception("Exception in PDF writing method: " + method, e);
        throw new PdfException(method, e);
    }

    public PdfContentStreamAdapter(PDPageContentStream cs) {
        this.cs = cs;
    }

    public void fillRect(float x, float y, float w, float h) {
        try {
            cs.addRect(x, y, w, h);
            cs.fill();
        } catch (IOException e) {
            logAndThrow("fillRect", e);
        }
    }

    public void addRect(float x, float y, float w, float h) {
        try {
            cs.addRect(x, y, w, h);
        } catch (IOException e) {
            logAndThrow("addRect", e);
        }
    }
    
    public void newPath() {
        // I think PDF-BOX does this automatically.
    }
    
    public void setExtGState(PDExtendedGraphicsState gs) {
        try {
            cs.setGraphicsStateParameters(gs);
        } catch (IOException e) {
            logAndThrow("setExtGState", e);
        }
    }

    public void closeSubpath() {
        try {
            cs.closePath();
        } catch (IOException e) {
            logAndThrow("closeSubpath", e);
        }
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3,
            float y3) {
        try {
            cs.curveTo(x1, y1, x2, y2, x3, y3);
        } catch (IOException e) {
            logAndThrow("curveTo(6)", e);
        }
    }

    public void curveTo(float x1, float y1, float x3, float y3) {
        try {
            cs.curveTo1(x1, y1, x3, y3);
        } catch (IOException e) {
            logAndThrow("curveTo(4)", e);
        }
    }

    public void closeContent() {
        try {
            cs.close();
        } catch (IOException e) {
            logAndThrow("closeContent", e);
        }
    }

    public void lineTo(float x1, float y1) {
        try {
            cs.lineTo(x1, y1);
        } catch (IOException e) {
            logAndThrow("lineTo", e);
        }
    }

    public void moveTo(float x1, float y1) {
        try {
            cs.moveTo(x1, y1);
        } catch (IOException e) {
            logAndThrow("moveTo", e);
        }
    }

    public void fillEvenOdd() {
        try {
            cs.fillEvenOdd();
        } catch (IOException e) {
            logAndThrow("fillEvenOdd", e);
        }
    }

    public void fillNonZero() {
        try {
            cs.fill();
        } catch (IOException e) {
            logAndThrow("fillNonZero", e);
        }
    }

    public void stroke() {
        try {
            cs.stroke();
        } catch (IOException e) {
            logAndThrow("stroke", e);
        }
    }

    public void clipNonZero() {
        try {
            cs.clip();
        } catch (IOException e) {
            logAndThrow("clipNonZero", e);
        }
    }

    public void clipEvenOdd() {
        try {
            cs.clipEvenOdd();
        } catch (IOException e) {
            logAndThrow("clipEvenOdd", e);
        }
    }

    public void setStrokingColor(int r, int g, int b) {
        try {
            cs.setStrokingColor(r, g, b);
        } catch (IOException e) {
            logAndThrow("setStrokingColor", e);
        }
    }

    public void setStrokingColor(float c, float m, float y, float k) {
        try {
            cs.setStrokingColor(c, m, y, k);
        } catch (IOException e) {
            logAndThrow("setStrokingColor(CMYK)", e);
        }
    }

    public void setFillColor(int r, int g, int b) {
        try {
            cs.setNonStrokingColor(r, g, b);
        } catch (IOException e) {
            logAndThrow("setFillColor", e);
        }
    }

    public void setFillColor(float c, float m, float y, float k) {
        try {
            cs.setNonStrokingColor(c, m, y, k);
        } catch (IOException e) {
            logAndThrow("setFillColor(CMYK)", e);
        }
    }

    public void setLineWidth(float width) {
        try {
            cs.setLineWidth(width);
        } catch (IOException e) {
            logAndThrow("setLineWidth", e);
        }
    }

    public void setLineCap(int capStyle) {
        try {
            cs.setLineCapStyle(capStyle);
        } catch (IOException e) {
            logAndThrow("setLineCap", e);
        }
    }

    public void setLineJoin(int joinStyle) {
        try {
            cs.setLineJoinStyle(joinStyle);
        } catch (IOException e) {
            logAndThrow("setLineJoin", e);
        }
    }

    public void setLineDash(float[] dash, float phase) {
        try {
            cs.setLineDashPattern(dash, phase);
        } catch (IOException e) {
            logAndThrow("setLineDash", e);
        }
    }

    public void restoreGraphics() {
        try {
            cs.restoreGraphicsState();
        } catch (IOException e) {
            logAndThrow("restoreGraphics", e);
        }
    }

    public void saveGraphics() {
        try {
            cs.saveGraphicsState();
        } catch (IOException e) {
            logAndThrow("saveGraphics", e);
        }
    }

    public void beginText() {
        try {
            cs.beginText();
        } catch (IOException e) {
            logAndThrow("beginText", e);
        }
    }

    public void endText() {
        try {
            cs.endText();
        } catch (IOException e) {
            logAndThrow("endText", e);
        }
    }

    public void setFont(PDFont font, float size) {
        try {
            cs.setFont(font, size);
        } catch (IOException e) {
            logAndThrow("setFont", e);
        }
    }

    public void setTextMatrix(float a, float b, float c, float d, float e,
            float f) {
        try {
            Matrix mtrx = new Matrix(a, b, c, d, e, f);
            cs.setTextMatrix(mtrx);
        } catch (IOException e1) {
            logAndThrow("setTextMatrix", e1);
        }
    }

    public void drawString(String s) {
        try {
            cs.showText(s);
        } catch (IOException e) {
            logAndThrow("drawString", e);
        }
    }

    public void drawImage(PDImageXObject xobject, float x, float y, float w,
            float h) {
        try {
            cs.drawImage(xobject, x, y, w, h);
        } catch (IOException e) {
            logAndThrow("drawImage", e);
        }
    }

    public void setMiterLimit(float miterLimit) {
        // TODO Not currently supported by PDF-BOX.
    }

    public void setTextSpacing(float nonSpaceAdjust) {
        try {
            cs.appendRawCommands(String.format("%f Tc\n", nonSpaceAdjust).replace(',', '.'));
        } catch (IOException e) {
            logAndThrow("setSpaceSpacing", e);
        }
    }

    public void setSpaceSpacing(float spaceAdjust) {
        try {
            cs.appendRawCommands(String.format("%f Tw\n", spaceAdjust).replace(',', '.'));
        } catch (IOException e) {
            logAndThrow("setSpaceSpacing", e);
        }
    }

    public void setPdfMatrix(AffineTransform transform) {
        try {
           cs.transform(new Matrix(transform));
        } catch (IOException e) {
            logAndThrow("setPdfMatrix", e);
        }
    }
}

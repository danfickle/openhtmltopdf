/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
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
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.FSCMYKColor;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;
import com.openhtmltopdf.pdfboxout.PdfBoxPerDocumentFormState;
import com.openhtmltopdf.render.*;
import com.openhtmltopdf.util.ArrayUtil;
import com.openhtmltopdf.util.Configuration;
import com.openhtmltopdf.util.XRLog;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitHeightDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PdfBoxSlowOutputDevice extends AbstractOutputDevice implements OutputDevice, PdfBoxOutputDevice {
    //
    // A discussion on units:
    //   PDF points are defined as 1/72 inch.
    //   CSS pixels are defined as 1/96 inch.
    //   PDF text units are defined as 1/1000 of a PDF point.
    //   OpenHTMLtoPDF dots are defined as 1/20 of a CSS pixel.
    //   Therefore dots per point is 20 * 96/72 or about 26.66.
    //   Dividing by _dotsPerPoint will convert OpenHTMLtoPDF dots to PDF points.
    //   Theoretically, this is all configurable, but not tested at all with other values.
    //
    
    private static final int FILL = 1;
    private static final int STROKE = 2;
    private static final int CLIP = 3;

    private static final AffineTransform IDENTITY = new AffineTransform();
    private static final BasicStroke STROKE_ONE = new BasicStroke(1);

    private static final boolean ROUND_RECT_DIMENSIONS_DOWN = Configuration.isTrue("xr.pdf.round.rect.dimensions.down", false);

    // The current PDF page.
    private PDPage _page;
    
    // A wrapper around the IOException throwing content stream methods which only throws runtime exceptions.
    // Created for every page.
    private PdfContentStreamAdapter _cp;
    
    // We need the page height because the project uses top down units which PDFs use bottom up units.
    // This is in PDF points unit (1/72 inch).
    private float _pageHeight;

    // The desired font as set by setFont.
    // This may not yet be set on the PDF text stream.
    private PdfBoxFSFont _font;

    // This transform is a scale and translate.
    // It scales from internal dots to PDF points.
    // It translates positions to implement page margins.
    private AffineTransform _transform = new AffineTransform();
    
    // A stack of currently in force transforms on the PDF graphics state.
    // NOTE: Transforms are cumulative and order is important.
    // After the graphics state is restored in setClip we must appropriately reapply the transforms
    // that should be in effect.
    private final Deque<AffineTransform> transformStack = new ArrayDeque<AffineTransform>();

    // An index into the transformStack. When we save state we set this to the length of transformStack
    // then we know we have to reapply those transforms set after saving state upon restoring state.
    private int clipTransformIndex;
    
    // We use these to keep track of where the current transform-origin is in absolute internal dots units.
    private float _absoluteTransformOriginX = 0;
    private float _absoluteTransformOriginY = 0;
    
    // The desired color as set by setColor.
    // To make sure this color is set on the PDF graphics stream call ensureFillColor or ensureStrokeColor.
    private FSColor _color = FSRGBColor.BLACK;

    // The actual fill and stroke colors set on the PDF graphics stream.
    // We keep these so we don't bloat the PDF with unneeded color calls.
    private FSColor _fillColor;
    private FSColor _strokeColor;

    // The currently set stroke. This will not yet be set on the PDF graphics stream.
    // This is already transformed to PDF points units.
    // Call setStrokeDiff to set this on the PDF graphics stream.
    private Stroke _stroke = null;
    
    // Same as _stroke, but not transformed. That is, it is in internal dots units.
    private Stroke _originalStroke = null;
    
    // The currently set stroke on the PDF graphics stream. When we call setStokeDiff
    // this is compared with _stroke and only the differences are output to the graphics stream.
    private Stroke _oldStroke = null;

    // The clipped area, as set on the PDF graphics stream, in PDF points units.
    private Area _clip;

    // Essentially per-run global variables.
    private SharedContext _sharedContext;
    
    // The project internal dots per PDF point unit. See discussion of units above.
    private float _dotsPerPoint;

    // The PDF document. Note: We are not responsible for closing it.
    private PDDocument _writer;

    // The default destination for the current page.
    // This is used to create bookmarks without a valid destination.
    private PDDestination _defaultDestination;

    // Contains a list of bookmarks for the document.
    private final List<Bookmark> _bookmarks = new ArrayList<Bookmark>();

    // Contains a list of metadata items for the document.
    private final List<Metadata> _metadata = new ArrayList<Metadata>();

    // Contains all the state needed to manage form controls
    private final PdfBoxPerDocumentFormState _formState = new PdfBoxPerDocumentFormState();
    
    // The root box in the document. We keep this so we can search for specific boxes below it
    // such as links or form controls which we need to position.
    private Box _root;

    // In theory, we can append to a PDF document, rather than creating new. This keeps the start page
    // so we can use it to offset when we need to know the PDF page number.
    // NOTE: Not tested recently, this feature may be broken.
    private int _startPageNo;
    
    // Whether we are in test mode, currently not used here, but keep around in case we need it down the track.
    @SuppressWarnings("unused")
    private final boolean _testMode;
    
    // Link manage handles a links. We add the link in paintBackground and then output links when the document is finished.
    private PdfBoxLinkManager _linkManager;
    
    // Not used currently.
    private RenderingContext _renderingContext;
    
    // The bidi reorderer is responsible for shaping Arabic text, deshaping and 
    // converting RTL text into its visual order.
    private BidiReorderer _reorderer = new SimpleBidiReorderer();

    // Font Mapping for the Graphics2D output
    private PdfBoxGraphics2DFontTextDrawer _fontTextDrawer;
    
    public PdfBoxSlowOutputDevice(float dotsPerPoint, boolean testMode) {
        _dotsPerPoint = dotsPerPoint;
        _testMode = testMode;
    }

    public void setWriter(PDDocument writer) {
        _writer = writer;
    }

    public PDDocument getWriter() {
        return _writer;
    }

    /**
     * Start a page. A new PDF page starts a new content stream so all graphics state has to be 
     * set back to default.
     */
    public void initializePage(PDPageContentStream currentPage, PDPage page, float height) {
        _cp = new PdfContentStreamAdapter(currentPage);
        _page = page;
        _pageHeight = height;

        if (!isFastRenderer()) {
            // We call saveGraphics so we can get back to a raw (unclipped) state after we have clipped.
            // restoreGraphics is only used by setClip and page finish (unless the fast renderer is in use).
            _cp.saveGraphics();
        }
        
        _transform = new AffineTransform();
        _transform.scale(1.0d / _dotsPerPoint, 1.0d / _dotsPerPoint);

        _absoluteTransformOriginX = 0;
        _absoluteTransformOriginY += height * _dotsPerPoint;

        _stroke = transformStroke(STROKE_ONE);
        _originalStroke = _stroke;
        _oldStroke = _stroke;

        setStrokeDiff(_stroke, null);

        if (_defaultDestination == null) {
            // Create a default destination to the top of the first page.
            PDPageFitHeightDestination dest = new PDPageFitHeightDestination();
            dest.setPage(page);
        }
    }

    public void finishPage() {
        if (!isFastRenderer()) {
            _cp.restoreGraphics();
        }
        
        _cp.closeContent();
    }

    public void paintReplacedElement(RenderingContext c, BlockBox box) {
        PdfBoxReplacedElement element = (PdfBoxReplacedElement) box.getReplacedElement();
        element.paint(c, this, box);
    }

    /**
     * We use paintBackground to do extra stuff such as processing links, forms and form controls.
     */
    @Override
    public void paintBackground(RenderingContext c, Box box) {
        super.paintBackground(c, box);

        // processLinkLater will take care of making sure it is actually a link.
        _linkManager.processLinkLater(c, box, _page, _pageHeight, _transform);
       
        if (box.getElement() != null && box.getElement().getNodeName().equals("form")) {
            _formState.addFormIfRequired(box, this);
        } else if (box.getElement() != null &&
                   ArrayUtil.isOneOf(box.getElement().getNodeName(), "input", "textarea", "button", "select", "openhtmltopdf-combo")) {
            // Add controls to list to process later. We do this in case we paint a control background
            // before its associated form.
            _formState.addControlIfRequired(box, _page, _transform, c, _pageHeight);
        }
    }

    private void processControls() {
        _formState.processControls(_sharedContext, _writer, _root);
    }
    


    /**
     * Given a value in dots units, converts to PDF points.
     */
    public float getDeviceLength(float length) {
        return length / _dotsPerPoint;
    }

    public void drawBorderLine(Shape bounds, int side, int lineWidth, boolean solid) {
        draw(bounds);
    }

    public void setColor(FSColor color) {
        if (color instanceof FSRGBColor) {
             _color = color;
        } else if (color instanceof FSCMYKColor) {
            _color = color;
        } else {
            assert(_color instanceof FSRGBColor || _color instanceof FSCMYKColor);
        }
    }

    public void draw(Shape s) {
        followPath(s, STROKE);
    }

    protected void drawLine(int x1, int y1, int x2, int y2) {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        draw(line);
    }

    public void drawRect(int x, int y, int width, int height) {
        draw(new Rectangle(x, y, width, height));
    }

    public void drawOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        draw(oval);
    }

    public void fill(Shape s) {
        followPath(s, FILL);
    }

    public void fillRect(int x, int y, int width, int height) {
        if (ROUND_RECT_DIMENSIONS_DOWN) {
            fill(new Rectangle(x, y, width - 1, height - 1));
        } else {
            fill(new Rectangle(x, y, width, height));
        }
    }

    public void fillOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        fill(oval);
    }

    public void translate(double tx, double ty) {
        _transform.translate(tx, ty);
    }

    public Object getRenderingHint(Key key) {
        return null;
    }

    public void setRenderingHint(Key key, Object value) {
    }

    public void setFont(FSFont font) {
        _font = ((PdfBoxFSFont) font);
    }

    /**
     * This returns a matrix that will convert y values to bottom up coordinate space (as used by PDFs).
     */
    private AffineTransform normalizeMatrix(AffineTransform current) {
        double[] mx = new double[6];
        AffineTransform result = new AffineTransform();
        result.getMatrix(mx);
        mx[3] = -1;
        mx[5] = _pageHeight;
        result = new AffineTransform(mx);
        result.concatenate(current);
        return result;
    }

    public void drawString(String s, float x, float y, JustificationInfo info) {
        PDFont firstFont = _font.getFontDescription().get(0).getFont();
        
        // First check if the string will print with the current font entirely.
        try {
            firstFont.getStringWidth(s);
            // We got here, so all is good.
            drawStringFast(s, x, y, info, _font.getFontDescription().get(0), _font.getSize2D());
            return;
        } 
        catch (Exception e) {
            // Fallthrough, we'll have to process the string into font runs.
        }
        
        List<FontRun> fontRuns = PdfBoxTextRenderer.divideIntoFontRuns(_font, s, _reorderer);
        
        float xOffset = 0f;
        for (FontRun run : fontRuns) {
            drawStringFast(run.str, x + xOffset, y, info, run.des, _font.getSize2D());
            try {
                xOffset += (run.des.getFont().getStringWidth(run.str) / 1000f) * _font.getSize2D();
            } catch (Exception e) {
                XRLog.render(Level.WARNING, "BUG. Font didn't contain expected character.", e);
            }
        }
    }
    
    public void drawStringFast(String s, float x, float y, JustificationInfo info, FontDescription desc, float fontSize) {
        if (s.length() == 0)
            return;

        ensureFillColor();
        AffineTransform at = (AffineTransform) getTransform().clone();
        at.translate(x, y);
        AffineTransform inverse = normalizeMatrix(at);
        AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
        inverse.concatenate(flipper);
        inverse.scale(_dotsPerPoint, _dotsPerPoint);
        double[] mx = new double[6];
        inverse.getMatrix(mx);
        
        float b = (float) mx[1];
        float c = (float) mx[2];
        
        fontSize = fontSize / _dotsPerPoint;
        
        boolean resetMode = false;
        FontSpecification fontSpec = getFontSpecification();
        if (fontSpec != null) {
            int need = FontResolverHelper.convertWeightToInt(fontSpec.fontWeight);
            int have = desc.getWeight();
            if (need > have) {
                _cp.setRenderingMode(RenderingMode.FILL_STROKE);
                float lineWidth = fontSize * 0.04f; // 4% of font size
                _cp.setLineWidth(lineWidth);
                resetMode = true;
                ensureStrokeColor();
            }
            if ((fontSpec.fontStyle == IdentValue.ITALIC) && (desc.getStyle() != IdentValue.ITALIC)) {
                b = 0f;
                c = 0.21256f;
            }
        }

        _cp.beginText();
        
        _cp.setFont(desc.getFont(), fontSize);
        _cp.setTextMatrix((float) mx[0], b, c, (float) mx[3], (float) mx[4], (float) mx[5]);

        if (info != null ) {
            // Note: Justification info is also used
            // to implement letter-spacing CSS property.
            // Justification must be done through TJ rendering
            // because Tw param does not work for UNICODE fonts
            Object[] array = makeJustificationArray(s, info);
            _cp.drawStringWithPositioning(array);
        } else {
            _cp.drawString(s);
        }
        
        _cp.endText();

        if (resetMode) {
            _cp.setRenderingMode(RenderingMode.FILL);
            _cp.setLineWidth(1);
        }
    }
    
    public static class FontRun {
        String str;
        FontDescription des;
    }
    
    private Object[] makeJustificationArray(String s, JustificationInfo info) {
        List<Object> data = new ArrayList<Object>(s.length() * 2);

        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            data.add(Character.toString(c));
            if (i != len - 1) {
                float offset;
                if (c == ' ' || c == '\u00a0' || c == '\u3000') {
                    offset = info.getSpaceAdjust();
                } else {
                    offset = info.getNonSpaceAdjust();
                }
                data.add(Float.valueOf((-offset / _dotsPerPoint) * 1000 / (_font.getSize2D() / _dotsPerPoint)));
            }
        }
        return data.toArray();
    }

    private AffineTransform getTransform() {
        return _transform;
    }

    private void ensureFillColor() {
        if (!(_color.equals(_fillColor))) {
            _fillColor = _color;

            if (_fillColor instanceof FSRGBColor) {
                FSRGBColor rgb = (FSRGBColor) _fillColor;
                _cp.setFillColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
            } else if (_fillColor instanceof FSCMYKColor) {
                FSCMYKColor cmyk = (FSCMYKColor) _fillColor;
                _cp.setFillColor(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
            }
            else {
                assert(_fillColor instanceof FSRGBColor || _fillColor instanceof FSCMYKColor);
            }
       }
    }

    private void ensureStrokeColor() {
        if (!(_color.equals(_strokeColor))) {
            _strokeColor = _color;

            if (_strokeColor instanceof FSRGBColor) {
                FSRGBColor rgb = (FSRGBColor) _strokeColor;
                _cp.setStrokingColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
            } else if (_strokeColor instanceof FSCMYKColor) {
                FSCMYKColor cmyk = (FSCMYKColor) _strokeColor;
                _cp.setStrokingColor(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
            }
            else {
                assert(_strokeColor instanceof FSRGBColor || _strokeColor instanceof FSCMYKColor);
            }
        }
    }

    public PdfContentStreamAdapter getCurrentPage() {
        return _cp;
    }

    public PDPage getPage(){
        return _page;
    }

    private void followPath(Shape s, int drawType) {
        if (s == null)
            return;
        
        if (drawType == STROKE) {
            if (!(_stroke instanceof BasicStroke)) {
                s = _stroke.createStrokedShape(s);
                followPath(s, FILL);
                return;
            }
        }
        if (drawType == STROKE) {
            setStrokeDiff(_stroke, _oldStroke);
            _oldStroke = _stroke;
            ensureStrokeColor();
        } else if (drawType == FILL) {
            ensureFillColor();
        }
        
        PathIterator points;
        if (drawType == CLIP) {
            points = s.getPathIterator(IDENTITY);
        } else {
            points = s.getPathIterator(_transform);
        }
        float[] coords = new float[6];
        int traces = 0;
        while (!points.isDone()) {
            ++traces;
            int segtype = points.currentSegment(coords);
            normalizeY(coords);
            switch (segtype) {
            case PathIterator.SEG_CLOSE:
                _cp.closeSubpath();
                break;

            case PathIterator.SEG_CUBICTO:
                _cp.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                break;

            case PathIterator.SEG_LINETO:
                _cp.lineTo(coords[0], coords[1]);
                break;

            case PathIterator.SEG_MOVETO:
                _cp.moveTo(coords[0], coords[1]);
                break;

            case PathIterator.SEG_QUADTO:
                _cp.curveTo(coords[0], coords[1], coords[2], coords[3]);
                break;
            }
            points.next();
        }
        
        switch (drawType) {
        case FILL:
            if (traces > 0) {
                if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                    _cp.fillEvenOdd();
                else
                    _cp.fillNonZero();
            }
            break;
        case STROKE:
            if (traces > 0)
                _cp.stroke();
            break;
        default: // drawType==CLIP
            if (traces == 0)
                _cp.addRect(0, 0, 0, 0);
            if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                _cp.clipEvenOdd();
            else
                _cp.clipNonZero();
            _cp.newPath();
        }
    }

    /**
     * Converts a top down unit to a bottom up PDF unit for the current page.
     */
    private float normalizeY(float y) {
        return _pageHeight - y;
    }
    
    /**
     * Converts a top down unit to a bottom up PDF unit for the specified page height.
     */
    public float normalizeY(float y, float pageHeight) {
        return pageHeight - y;
    }

    private void normalizeY(float[] coords) {
        coords[1] = normalizeY(coords[1]);
        coords[3] = normalizeY(coords[3]);
        coords[5] = normalizeY(coords[5]);
    }

    private void setStrokeDiff(Stroke newStroke, Stroke oldStroke) {
        if (newStroke == oldStroke)
            return;
        if (!(newStroke instanceof BasicStroke))
            return;
        BasicStroke nStroke = (BasicStroke) newStroke;
        boolean oldOk = (oldStroke instanceof BasicStroke);
        BasicStroke oStroke = null;
        if (oldOk)
            oStroke = (BasicStroke) oldStroke;
        if (!oldOk || nStroke.getLineWidth() != oStroke.getLineWidth())
            _cp.setLineWidth(nStroke.getLineWidth());
        if (!oldOk || nStroke.getEndCap() != oStroke.getEndCap()) {
            switch (nStroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                _cp.setLineCap(0);
                break;
            case BasicStroke.CAP_SQUARE:
                _cp.setLineCap(2);
                break;
            default:
                _cp.setLineCap(1);
            }
        }
        if (!oldOk || nStroke.getLineJoin() != oStroke.getLineJoin()) {
            switch (nStroke.getLineJoin()) {
            case BasicStroke.JOIN_MITER:
                _cp.setLineJoin(0);
                break;
            case BasicStroke.JOIN_BEVEL:
                _cp.setLineJoin(2);
                break;
            default:
                _cp.setLineJoin(1);
            }
        }
        if (!oldOk || nStroke.getMiterLimit() != oStroke.getMiterLimit())
            _cp.setMiterLimit(nStroke.getMiterLimit());
        boolean makeDash;
        if (oldOk) {
            if (nStroke.getDashArray() != null) {
                if (nStroke.getDashPhase() != oStroke.getDashPhase()) {
                    makeDash = true;
                } else if (!java.util.Arrays.equals(nStroke.getDashArray(), oStroke.getDashArray())) {
                    makeDash = true;
                } else
                    makeDash = false;
            } else if (oStroke.getDashArray() != null) {
                makeDash = true;
            } else
                makeDash = false;
        } else {
            makeDash = true;
        }
        if (makeDash) {
            float dash[] = nStroke.getDashArray();
            if (dash == null)
                _cp.setLineDash(new float[] {}, 0);
            else {
                _cp.setLineDash(dash, nStroke.getDashPhase());
            }
        }
    }

    public void setStroke(Stroke s) {
        _originalStroke = s;
        this._stroke = transformStroke(s);
    }

    private Stroke transformStroke(Stroke stroke) {
        if (!(stroke instanceof BasicStroke))
            return stroke;
        BasicStroke st = (BasicStroke) stroke;
        float scale = (float) Math.sqrt(Math.abs(_transform.getDeterminant()));
        float dash[] = st.getDashArray();
        if (dash != null) {
            for (int k = 0; k < dash.length; ++k)
                dash[k] *= scale;
        }
        return new BasicStroke(st.getLineWidth() * scale, st.getEndCap(), st.getLineJoin(), st.getMiterLimit(), dash, st.getDashPhase()
                * scale);
    }

    public void clip(Shape s) {
        if (isFastRenderer()) {
            XRLog.render(Level.SEVERE, "clip MUST not be used by the fast renderer. Please consider reporting this bug.");
            return;
        }
        
        if (s != null) {
            s = _transform.createTransformedShape(s);
            if (_clip == null)
                _clip = new Area(s);
            else
                _clip.intersect(new Area(s));
            followPath(s, CLIP);
        } else {
            assert(s != null);
        }
        //throw new RuntimeException(s.toString());
    }

    public Shape getClip() {
        if (isFastRenderer()) {
            XRLog.render(Level.SEVERE, "getClip MUST not be used by the fast renderer. Please consider reporting this bug.");
            return null;
        }
        
        try {
            return _transform.createInverse().createTransformedShape(_clip);
        } catch (NoninvertibleTransformException e) {
            return null;
        }
    }
    
    @Override
    public void popClip() {
        _cp.restoreGraphics();
        clearPageState();
    }
    
    @Override
    public void pushClip(Shape s) {
        _cp.saveGraphics();
        if (s != null) {
            Shape s1 = _transform.createTransformedShape(s);
            followPath(s1, CLIP);
        }
    }

    @Override
    public void setClip(Shape s) {
        if (isFastRenderer()) {
            XRLog.render(Level.SEVERE, "setClip MUST not be used by the fast renderer. Please consider reporting this bug.");
            return;
        }

        // Restore graphics to get back to a no-clip situation.
        _cp.restoreGraphics();

        // Reapply the transforms that are in effect.
        reapplyTransforms();
        
        // Save graphics so we can do this again.
        _cp.saveGraphics();
        
        // Set the index so we know which transforms have to be reapplied
        // when we next restore graphics.
        clipTransformIndex = transformStack.size();
        
        if (s != null)
            s = _transform.createTransformedShape(s);
        if (s == null) {
            _clip = null;
        } else {
            _clip = new Area(s);
            followPath(s, CLIP);
        }
        
        clearPageState();
    }

    public Stroke getStroke() {
        return _originalStroke;
    }
    
    public void realizeImage(PdfBoxImage img) {
        PDImageXObject xobject;
        try {
            if (img.isJpeg()) {
                xobject = JPEGFactory.createFromStream(_writer,
                        new ByteArrayInputStream(img.getBytes()));
            } else {
                BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(
                        img.getBytes()));

                xobject = LosslessFactory.createFromImage(_writer, buffered);
            }
        } catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("realizeImage", e);
        }
        img.clearBytes();
        img.setXObject(xobject);
    }

    public void drawImage(FSImage fsImage, int x, int y, boolean interpolate) {
        PdfBoxImage img = (PdfBoxImage) fsImage;

        PDImageXObject xobject = img.getXObject();
		if (interpolate) {
			xobject.setInterpolate(true);
		} else {
			/*
			 * Specialcase for not interpolating an image, default is to always interpolate.
			 * We must copy the image
			 */
			try {
				InputStream inputStream = xobject.getStream().getCOSObject().createRawInputStream();
				PDImageXObject cloneImage = new PDImageXObject(_writer, inputStream, COSName.FLATE_DECODE,
						xobject.getWidth(), xobject.getHeight(), xobject.getBitsPerComponent(),
						xobject.getColorSpace());
				cloneImage.setInterpolate(false);
				if (xobject.getSoftMask() != null)
					cloneImage.getCOSObject().setItem(COSName.SMASK, xobject.getSoftMask());
				inputStream.close();
				xobject = cloneImage;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
        

        AffineTransform transformer = (AffineTransform) getTransform().clone();
        transformer.translate(x, y);
        transformer.translate(0, img.getHeight());
        AffineTransform normalized = normalizeMatrix(transformer);
        normalized.scale(img.getWidth(), -img.getHeight());

        double[] mx = new double[6];
        normalized.getMatrix(mx);

        _cp.drawImage(xobject, (float) mx[4], (float) mx[5], (float) mx[0],
                (float) mx[3]);
    }
/*
    private void drawPDFAsImage(PDFAsImage image, int x, int y) {
        URI uri = image.getURI();
        PdfReader reader = null;

        try {
            reader = getReader(uri);
        } catch (IOException e) {
            throw new XRRuntimeException("Could not load " + uri + ": " + e.getMessage(), e);
        }

        PdfImportedPage page = getWriter().getImportedPage(reader, 1);

        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        at.translate(0, image.getHeightAsFloat());
        at.scale(image.getWidthAsFloat(), image.getHeightAsFloat());

        AffineTransform inverse = normalizeMatrix(_transform);
        AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
        inverse.concatenate(at);
        inverse.concatenate(flipper);

        double[] mx = new double[6];
        inverse.getMatrix(mx);

        mx[0] = image.scaleWidth();
        mx[3] = image.scaleHeight();

        _currentPage.restoreState();
        _currentPage.addTemplate(page, (float) mx[0], (float) mx[1], (float) mx[2], (float) mx[3], (float) mx[4], (float) mx[5]);
        _currentPage.saveState();
    }

    public PdfReader getReader(URI uri) throws IOException {
        PdfReader result = (PdfReader) _readerCache.get(uri);
        if (result == null) {
            result = new PdfReader(getSharedContext().getUserAgentCallback().getBinaryResource(uri.toString()));
            _readerCache.put(uri, result);
        }
        return result;
    }
*/
    public float getDotsPerPoint() {
        return _dotsPerPoint;
    }

    public void start(Document doc) {
        _linkManager = new PdfBoxLinkManager(_sharedContext, _dotsPerPoint, _root, this);
        loadBookmarks(doc);
        loadMetadata(doc);
    }

    public void finish(RenderingContext c, Box root) {
        processControls();
        _linkManager.processLinks();
        writeOutline(c, root);
    }

    private void writeOutline(RenderingContext c, Box root) {
        if (_bookmarks.size() > 0) {
            // TODO: .setViewerPreferences(PdfWriter.PageModeUseOutlines);
    
            PDDocumentOutline outline = _writer.getDocumentCatalog().getDocumentOutline();
			if (outline == null) {
                outline = new PDDocumentOutline();
                _writer.getDocumentCatalog().setDocumentOutline(outline);
            }
            
            writeBookmarks(c, root, outline, _bookmarks);
        }
    }

    private void writeBookmarks(RenderingContext c, Box root, PDOutlineNode parent, List<Bookmark> bookmarks) {
        for (Bookmark bookmark : bookmarks) {
            writeBookmark(c, root, parent, bookmark);
        }
    }

    public int getPageRefY(Box box) {
        if (box instanceof InlineLayoutBox) {
            InlineLayoutBox iB = (InlineLayoutBox) box;
            return iB.getAbsY() + iB.getBaseline();
        } else {
            return box.getAbsY();
        }
    }

    private void writeBookmark(RenderingContext c, Box root, PDOutlineNode parent, Bookmark bookmark) {
        String href = bookmark.getHRef();
        PDPageXYZDestination target = null;
        if (href.length() > 0 && href.charAt(0) == '#') {
            Box box = _sharedContext.getBoxById(href.substring(1));
            if (box != null) {
                PageBox page = root.getLayer().getPage(c, getPageRefY(box));
                int distanceFromTop = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
                distanceFromTop += box.getAbsY() - page.getTop();

                target = new PDPageXYZDestination();
                target.setTop((int) normalizeY(distanceFromTop / _dotsPerPoint));
                target.setPage(_writer.getPage(_startPageNo + page.getPageNo()));
            }
        }

        PDOutlineItem outline = new PDOutlineItem();
        outline.setDestination(target == null ? _defaultDestination : target);
        outline.setTitle(bookmark.getName());
        parent.addLast(outline);
        writeBookmarks(c, root, outline, bookmark.getChildren());
    }

    private void loadBookmarks(Document doc) {
        Element head = DOMUtil.getChild(doc.getDocumentElement(), "head");
        if (head != null) {
            Element bookmarks = DOMUtil.getChild(head, "bookmarks");
            if (bookmarks != null) {
                List<Element> l = DOMUtil.getChildren(bookmarks, "bookmark");
                if (l != null) {
                    for (Element e : l) {
                        loadBookmark(null, e);
                    }
                }
            }
        }
    }

    private void loadBookmark(Bookmark parent, Element bookmark) {
        Bookmark us = new Bookmark(bookmark.getAttribute("name"), bookmark.getAttribute("href"));
        if (parent == null) {
            _bookmarks.add(us);
        } else {
            parent.addChild(us);
        }
        List<Element> l = DOMUtil.getChildren(bookmark, "bookmark");
        if (l != null) {
            for (Element e : l) {
                loadBookmark(us, e);
            }
        }
    }

    private static class Bookmark {
        private final String _name;
        private final String _HRef;

        private List<Bookmark> _children;

        public Bookmark(String name, String href) {
            _name = name;
            _HRef = href;
        }

        public String getHRef() {
            return _HRef;
        }

        public String getName() {
            return _name;
        }

        public void addChild(Bookmark child) {
            if (_children == null) {
                _children = new ArrayList<Bookmark>();
            }
            _children.add(child);
        }

        public List<Bookmark> getChildren() {
            return _children == null ? Collections.<Bookmark>emptyList() : _children;
        }
    }

    // Metadata methods

    // Methods to load and search a document's metadata

    /**
     * Appends a name/content metadata pair to this output device. A name or
     * content value of null will be ignored.
     * 
     * @param name
     *            the name of the metadata element to add.
     */
    public void addMetadata(String name, String value) {
        if ((name != null) && (value != null)) {
            Metadata m = new Metadata(name, value);
            _metadata.add(m);
        }
    }

    /**
     * Searches the metadata name/content pairs of the current document and
     * returns the content value from the first pair with a matching name. The
     * search is case insensitive.
     * 
     * @param name
     *            the metadata element name to locate.
     * @return the content value of the first found metadata element; otherwise
     *         null.
     */
    public String getMetadataByName(String name) {
        if (name != null) {
            for (Metadata m : _metadata) {
                if ((m != null) && m.getName().equalsIgnoreCase(name)) {
                    return m.getContent();
                }
            }
        }
        return null;
    }

    /**
     * Searches the metadata name/content pairs of the current document and
     * returns any content values with a matching name in an ArrayList. The
     * search is case insensitive.
     * 
     * @param name
     *            the metadata element name to locate.
     * @return an ArrayList with matching content values; otherwise an empty
     *         list.
     */
    public List<String> getMetadataListByName(String name) {
        List<String> result = new ArrayList<String>();
        if (name != null) {
            for (Metadata m : _metadata) {
                if ((m != null) && m.getName().equalsIgnoreCase(name)) {
                    result.add(m.getContent());
                }
            }
        }
        return result;
    }

    /**
     * Locates and stores all metadata values in the document head that contain
     * name/content pairs. If there is no pair with a name of "title", any
     * content in the title element is saved as a "title" metadata item.
     * 
     * @param doc
     *            the Document level node of the parsed xhtml file.
     */
    private void loadMetadata(Document doc) {
        Element head = DOMUtil.getChild(doc.getDocumentElement(), "head");
        if (head != null) {
            List<Element> l = DOMUtil.getChildren(head, "meta");
            if (l != null) {
                for (Element e : l) {
                    String name = e.getAttribute("name");
                    if (name != null) { // ignore non-name metadata data
                        String content = e.getAttribute("content");
                        Metadata m = new Metadata(name, content);
                        _metadata.add(m);
                    }
                }
            }
            // If there is no title meta data attribute, use the document title.
            String title = getMetadataByName("title");
            if (title == null) {
                Element t = DOMUtil.getChild(head, "title");
                if (t != null) {
                    title = DOMUtil.getText(t).trim();
                    Metadata m = new Metadata("title", title);
                    _metadata.add(m);
                }
            }
        }
    }

    /**
     * Replaces all copies of the named metadata with a single value. A a new
     * value of null will result in the removal of all copies of the named
     * metadata. Use <code>addMetadata</code> to append additional values with
     * the same name.
     * 
     * @param name
     *            the metadata element name to locate.
     */
    public void setMetadata(String name, String value) {
        if (name != null) {
            boolean remove = (value == null); // removing all instances of name?
            int free = -1; // first open slot in array
            for (int i = 0, len = _metadata.size(); i < len; i++) {
                Metadata m = _metadata.get(i);
                if (m != null) {
                    if (m.getName().equalsIgnoreCase(name)) {
                        if (!remove) {
                            remove = true; // remove all other instances
                            m.setContent(value);
                        } else {
                            _metadata.set(i, null);
                        }
                    }
                } else if (free == -1) {
                    free = i;
                }
            }
            if (!remove) { // not found?
                Metadata m = new Metadata(name, value);
                if (free == -1) { // no open slots?
                    _metadata.add(m);
                } else {
                    _metadata.set(free, m);
                }
            }
        }
    }

    // Class for storing metadata element name/content pairs from the head
    // section of an xhtml document.
    static class Metadata {
        private String _name;
        private String _content;

        Metadata(String name, String content) {
            _name = name;
            _content = content;
        }

        public String getContent() {
            return _content;
        }

        public void setContent(String content) {
            _content = content;
        }

        public String getName() {
            return _name;
        }

        public void setName(String name) {
            _name = name;
        }
    }

    // Metadata end


    /**
     * @return All metadata entries
     */
    public List<Metadata> getMetadata() {
        return _metadata;
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void setSharedContext(SharedContext sharedContext) {
        _sharedContext = sharedContext;
        sharedContext.getCss().setSupportCMYKColors(true);
    }

    public void setRoot(Box root) {
        _root = root;
    }

    public int getStartPageNo() {
        return _startPageNo;
    }

    public void setStartPageNo(int startPageNo) {
        _startPageNo = startPageNo;
    }

    public void drawSelection(RenderingContext c, InlineText inlineText) {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsSelection() {
        return false;
    }

    public boolean isSupportsCMYKColors() {
        return true;
    }

    @Override
    public void drawWithGraphics(float x, float y, float width, float height, OutputDeviceGraphicsDrawer renderer) {
        try {
            PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(_writer, (int) width, (int) height);
			/*
			 * Create and set the fontTextDrawer to perform the font mapping.
			 */
            if (_fontTextDrawer == null) {
                _fontTextDrawer = new PdfBoxGraphics2DFontTextDrawer() {
                    @Override
                    protected PDFont mapFont(Font font, IFontTextDrawerEnv env) {
                        FontSpecification spec = new FontSpecification();
                        spec.size = font.getSize();
                        spec.families = new String[] { font.getFamily() };
                        spec.fontStyle = IdentValue.NORMAL;
                        spec.fontWeight = IdentValue.NORMAL;
                        spec.variant = IdentValue.NORMAL;
                        if ((font.getStyle() & Font.BOLD) == Font.BOLD) {
                            spec.fontWeight = IdentValue.FONT_WEIGHT_700;
                        }
                        if ((font.getStyle() & Font.ITALIC) == Font.ITALIC) {
                            spec.fontStyle = IdentValue.ITALIC;
                        }
                        PdfBoxFSFont fsFont = (PdfBoxFSFont) getSharedContext().getFontResolver()
                                .resolveFont(getSharedContext(), spec);
                        FontDescription fontDescription = fsFont.getFontDescription().get(0);
						/*
						 * Detect the default fallback value
						 */
                        if (fsFont.getFontDescription().size() == 1) {
                            if (fontDescription.getFont().getName().equals("Times-Roman")
                                    && !(font.getFamily().equals("Times New Roman"))) {
								/*
								 * We did not find the font, this is the generic default fallback font.
								 * So use the vectorized text shapes.
								 */
                                return null;
                            }
                        }
                        return fontDescription.getFont();
                    }
                };
            }
            pdfBoxGraphics2D.setFontTextDrawer(_fontTextDrawer);

            /*
             * Do rendering
             */
            renderer.render(pdfBoxGraphics2D);
            /*
             * Dispose to close the XStream
             */
            pdfBoxGraphics2D.dispose();

            /*
             * We convert from 72dpi of the Graphics2D device to our 96dpi
             * using the output matrix of the XForm object.
             * FIXME: Probably want to make this configurable.
             */
            PDFormXObject xFormObject = pdfBoxGraphics2D.getXFormObject();
            xFormObject.setMatrix(AffineTransform.getScaleInstance(72f / 96f, 72f / 96f));
            
            /*
             * Adjust the y to take into account that the y passed to placeXForm below
             * refers to the bottom left of the object while we were passed in y the 
             * position of the top left corner.
             * FIXME: Make DPI conversion configurable (as above).
             */
            y += (height) * _dotsPerPoint * (72f / 96f);

            /*
             * Use the page transform to convert from _dotsPerPoint units to 
             * PDF units. Also takes care of page margins.
             */
            Point2D p = new Point2D.Float(x, y);
            Point2D pResult = new Point2D.Float();
            _transform.transform(p, pResult);

            /*
             * And then stamp it
             */
            _cp.placeXForm((float) pResult.getX(), _pageHeight - (float) pResult.getY(), xFormObject);
        }
        catch(IOException e){
            throw new RuntimeException("Error while drawing on Graphics2D", e);
        }
    }

    public List<PagePosition> findPagePositionsByID(CssContext c, Pattern pattern) {
        Map<String, Box> idMap = _sharedContext.getIdMap();
        if (idMap == null) {
            return Collections.emptyList();
        }

        List<PagePosition> result = new ArrayList<PagePosition>();
        for (Entry<String, Box> entry : idMap.entrySet()) {
            String id = (String) entry.getKey();
            if (pattern.matcher(id).find()) {
                Box box = (Box) entry.getValue();
                PagePosition pos = calcPDFPagePosition(c, id, box);
                if (pos != null) {
                    result.add(pos);
                }
            }
        }

        Collections.sort(result, new Comparator<PagePosition>() {
            public int compare(PagePosition p1, PagePosition p2) {
                return p1.getPageNo() - p2.getPageNo();
            }
        });

        return result;
    }

    private PagePosition calcPDFPagePosition(CssContext c, String id, Box box) {
        PageBox page = _root.getLayer().getLastPage(c, box);
        if (page == null) {
            return null;
        }

        float x = box.getAbsX() + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
        float y = (page.getBottom() - (box.getAbsY() + box.getHeight())) + page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM);
        x /= _dotsPerPoint;
        y /= _dotsPerPoint;

        PagePosition result = new PagePosition();
        result.setId(id);
        result.setPageNo(page.getPageNo());
        result.setX(x);
        result.setY(y);
        result.setWidth(box.getEffectiveWidth() / _dotsPerPoint);
        result.setHeight(box.getHeight() / _dotsPerPoint);

        return result;
    }

    public void setRenderingContext(RenderingContext result) {
        _renderingContext = result;
    }

    public void setBidiReorderer(BidiReorderer reorderer) {
        _reorderer = reorderer;
    }
    
    @Override
    public void popTransforms(List<AffineTransform> inverse) {
        Collections.reverse(inverse);
        for (AffineTransform transform : inverse) {
            transformStack.pop();
            _cp.applyPdfMatrix(transform);
        }
    }
    
    @Override
    public List<AffineTransform> pushTransforms(List<AffineTransform> transforms) {
		if (transforms.size() == 0)
			return Collections.emptyList();
        // We simply do a saveGraphics here, so we don't have to apply the inverse later to restore
        List<AffineTransform> inverse = new ArrayList<AffineTransform>(transforms.size());
        try {
            for (AffineTransform transform : transforms) {
                double[] mx = new double[6];
                transform.getMatrix(mx);
                mx[4] /= _dotsPerPoint;
                mx[5] /= _dotsPerPoint;
                mx[5] = -mx[5];
               
                AffineTransform normalized = new AffineTransform(mx);
                inverse.add(normalized.createInverse());
                transformStack.push(normalized);
                _cp.applyPdfMatrix(normalized);
            }
        } catch (NoninvertibleTransformException e) {
            XRLog.render(Level.WARNING, "Tried to set a non-invertible CSS transform. Ignored.");
        }
        return inverse;
    }
    
    // FIXME: Not sure if this is ever needed.
    private void reapplyTransforms() {
        int idx = 0;

        for (Iterator<AffineTransform> iter = transformStack.descendingIterator(); iter.hasNext(); ) {
            AffineTransform transform = iter.next();
            if (idx >= clipTransformIndex) {
                _cp.applyPdfMatrix(transform);
            }
            idx++;
        }
    }

    @Override
    public float getAbsoluteTransformOriginX() {
        return _absoluteTransformOriginX;
    }
    
    @Override
    public float getAbsoluteTransformOriginY() {
        return _absoluteTransformOriginY;
    }
    


    @Override
    public void setPaint(Paint paint) {
        if (paint instanceof Color) {
            Color c = (Color) paint;
            this.setColor(new FSRGBColor(c.getRed(), c.getGreen(), c.getBlue()));
        } else {
            XRLog.render(Level.WARNING, "Unknown paint: " + paint.getClass().getCanonicalName());
        }
    }

    @Override
    public boolean isPDF() {
        return true;
    }

    /**
     * Perform any internal cleanup needed
     */
    public void close() {
        if (_fontTextDrawer != null) {
            _fontTextDrawer.close();
        }
    }
    
    private AffineTransform normalizeTransform(AffineTransform transform) {
        double[] mx = new double[6];
        transform.getMatrix(mx);
        mx[4] /= _dotsPerPoint;
        mx[5] /= _dotsPerPoint;
        return new AffineTransform(mx);
    }

    @Override
    public void pushTransformLayer(AffineTransform transform) {
        _cp.saveGraphics();
        AffineTransform normalized = normalizeTransform(transform);
        _cp.applyPdfMatrix(normalized);
    }

    @Override
    public void popTransformLayer() {
        _cp.restoreGraphics();
        clearPageState();
    }
    
    @Override
    public boolean isFastRenderer() {
        return _renderingContext.isFastRenderer();
    }
    
    private void clearPageState() {
        _fillColor = null;
        _strokeColor = null;
        _oldStroke = null;
    }
}

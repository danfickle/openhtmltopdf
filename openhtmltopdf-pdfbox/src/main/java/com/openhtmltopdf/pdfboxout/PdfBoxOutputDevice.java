package com.openhtmltopdf.pdfboxout;

import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.io.Closeable;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.w3c.dom.Document;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;
import com.openhtmltopdf.pdfboxout.PdfBoxUtil.Metadata;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.InlineText;
import com.openhtmltopdf.render.JustificationInfo;
import com.openhtmltopdf.render.RenderingContext;

public interface PdfBoxOutputDevice extends OutputDevice, Closeable {

    void setWriter(PDDocument writer);

    PDDocument getWriter();

    /**
     * Start a page. A new PDF page starts a new content stream so all graphics state has to be 
     * set back to default.
     */
    void initializePage(PDPageContentStream currentPage, PDPage page,
            float height);

    void finishPage();

    @Override
    void paintReplacedElement(RenderingContext c, BlockBox box);

    /**
     * We use paintBackground to do extra stuff such as processing links, forms and form controls.
     */
    @Override
    void paintBackground(RenderingContext c, Box box);

    /**
     * Given a value in dots units, converts to PDF points.
     */
    float getDeviceLength(float length);

    @Override
    void drawBorderLine(Shape bounds, int side, int lineWidth, boolean solid);

    @Override
    void setColor(FSColor color);

    @Override
    void draw(Shape s);

    @Override
    void drawRect(int x, int y, int width, int height);

    @Override
    void drawOval(int x, int y, int width, int height);

    @Override
    void fill(Shape s);

    @Override
    void fillRect(int x, int y, int width, int height);

    @Override
    void fillOval(int x, int y, int width, int height);

    @Override
    void translate(double tx, double ty);

    @Override
    Object getRenderingHint(Key key);

    @Override
    void setRenderingHint(Key key, Object value);

    @Override
    void setFont(FSFont font);

    void drawString(String s, float x, float y, JustificationInfo info);

    void drawStringFast(String s, float x, float y, JustificationInfo info,
            FontDescription desc, float fontSize);

    PdfContentStreamAdapter getCurrentPage();

    PDPage getPage();

    /**
     * Converts a top down unit to a bottom up PDF unit for the specified page height.
     */
    float normalizeY(float y, float pageHeight);

    @Override
    void setStroke(Stroke s);

    @Override
    void popClip();

    @Override
    void pushClip(Shape s);

    @Override
    Stroke getStroke();

    void realizeImage(PdfBoxImage img);

    @Override
    void drawImage(FSImage fsImage, int x, int y, boolean interpolate);

    float getDotsPerPoint();

    void start(Document doc);

    void finish(RenderingContext c, Box root);

    /**
     * Appends a name/content metadata pair to this output device. A name or
     * content value of null will be ignored.
     * 
     * @param name
     *            the name of the metadata element to add.
     */
    void addMetadata(String name, String value);

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
    String getMetadataByName(String name);

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
    List<String> getMetadataListByName(String name);

    SharedContext getSharedContext();

    void setSharedContext(SharedContext sharedContext);

    void setRoot(Box root);

    int getStartPageNo();

    void setStartPageNo(int startPageNo);

    @Override
    void drawSelection(RenderingContext c, InlineText inlineText);

    @Override
    boolean isSupportsSelection();

    @Override
    boolean isSupportsCMYKColors();

    @Override
    void drawWithGraphics(float x, float y, float width, float height,
            OutputDeviceGraphicsDrawer renderer);

    List<PagePosition<Box>> findPagePositionsByID(CssContext c, Pattern pattern);

    void setRenderingContext(RenderingContext result);

    void setBidiReorderer(BidiReorderer reorderer);

    @Override
    void setPaint(Paint paint);

    @Override
    boolean isPDF();

    /**
     * Perform any internal cleanup needed
     */
    @Override
    void close();

    @Override
    void pushTransformLayer(AffineTransform transform);

    @Override
    void popTransformLayer();

    boolean isFastRenderer();
    
    int getPageRefY(Box box);
    
    List<Metadata> getMetadata();

    void drawPdfAsImage(PDFormXObject _src, Rectangle contentBounds, float intrinsicWidth, float intrinsicHeight);

}

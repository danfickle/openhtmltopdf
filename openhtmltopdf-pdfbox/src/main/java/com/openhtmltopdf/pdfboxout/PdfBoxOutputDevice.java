package com.openhtmltopdf.pdfboxout;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.w3c.dom.Document;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;
import com.openhtmltopdf.pdfboxout.PdfBoxSlowOutputDevice.Metadata;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.InlineText;
import com.openhtmltopdf.render.JustificationInfo;
import com.openhtmltopdf.render.RenderingContext;

public interface PdfBoxOutputDevice extends OutputDevice {

    void setWriter(PDDocument writer);

    PDDocument getWriter();

    /**
     * Start a page. A new PDF page starts a new content stream so all graphics state has to be 
     * set back to default.
     */
    void initializePage(PDPageContentStream currentPage, PDPage page,
            float height);

    void finishPage();

    void paintReplacedElement(RenderingContext c, BlockBox box);

    /**
     * We use paintBackground to do extra stuff such as processing links, forms and form controls.
     */
    void paintBackground(RenderingContext c, Box box);

    /**
     * Given a value in dots units, converts to PDF points.
     */
    float getDeviceLength(float length);

    void drawBorderLine(Shape bounds, int side, int lineWidth, boolean solid);

    void setColor(FSColor color);

    void draw(Shape s);

    void drawRect(int x, int y, int width, int height);

    void drawOval(int x, int y, int width, int height);

    void fill(Shape s);

    void fillRect(int x, int y, int width, int height);

    void fillOval(int x, int y, int width, int height);

    void translate(double tx, double ty);

    Object getRenderingHint(Key key);

    void setRenderingHint(Key key, Object value);

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

    void setStroke(Stroke s);

    void clip(Shape s);

    Shape getClip();

    void popClip();

    void pushClip(Shape s);

    void setClip(Shape s);

    Stroke getStroke();

    void realizeImage(PdfBoxImage img);

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

    /**
     * Replaces all copies of the named metadata with a single value. A a new
     * value of null will result in the removal of all copies of the named
     * metadata. Use <code>addMetadata</code> to append additional values with
     * the same name.
     * 
     * @param name
     *            the metadata element name to locate.
     */
    void setMetadata(String name, String value);

    SharedContext getSharedContext();

    void setSharedContext(SharedContext sharedContext);

    void setRoot(Box root);

    int getStartPageNo();

    void setStartPageNo(int startPageNo);

    void drawSelection(RenderingContext c, InlineText inlineText);

    boolean isSupportsSelection();

    boolean isSupportsCMYKColors();

    void drawWithGraphics(float x, float y, float width, float height,
            OutputDeviceGraphicsDrawer renderer);

    List<PagePosition> findPagePositionsByID(CssContext c, Pattern pattern);

    void setRenderingContext(RenderingContext result);

    void setBidiReorderer(BidiReorderer reorderer);

    void popTransforms(List<AffineTransform> inverse);

    List<AffineTransform> pushTransforms(List<AffineTransform> transforms);

    float getAbsoluteTransformOriginX();

    float getAbsoluteTransformOriginY();

    void setPaint(Paint paint);

    boolean isPDF();

    /**
     * Perform any internal cleanup needed
     */
    void close();

    void pushTransformLayer(AffineTransform transform);

    void popTransformLayer();

    boolean isFastRenderer();
    
    int getPageRefY(Box box);
    
    List<Metadata> getMetadata();

}
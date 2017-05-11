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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice.Metadata;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.util.Configuration;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class PdfBoxRenderer {
    // See discussion of units at top of PdfBoxOutputDevice.
    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
    private static final int DEFAULT_DOTS_PER_PIXEL = 20;
    private static final int DEFAULT_PDF_POINTS_PER_INCH = 72;

    private final SharedContext _sharedContext;
    private final PdfBoxOutputDevice _outputDevice;

    private Document _doc;
    private BlockBox _root;

    private final float _dotsPerPoint;

    private PDDocument _pdfDoc;
    
    private PDEncryption _pdfEncryption;

    // Usually 1.7
    private float _pdfVersion;
    
    private boolean _testMode;

    private PDFCreationListener _listener;
    
    private OutputStream _os;
    private SVGDrawer _svgImpl;
    
    private BidiSplitterFactory _splitterFactory;
    private byte _defaultTextDirection = BidiSplitter.LTR;
    private BidiReorderer _reorderer;

    /**
     * @param testMode
     * @deprecated Please use the builder.
     */
    @Deprecated
    public PdfBoxRenderer(boolean testMode) {
        this(DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL, true, testMode, null, null, null, null);
    }

    /**
     * @deprecated Please use the builder.
     * @param dotsPerPoint
     * @param dotsPerPixel
     * @param useSubsets
     * @param testMode
     * @param factory
     * @param _resolver
     * @param _cache
     * @param svgImpl
     */
    @Deprecated
    public PdfBoxRenderer(float dotsPerPoint, int dotsPerPixel, boolean useSubsets, boolean testMode, HttpStreamFactory factory, FSUriResolver _resolver, FSCache _cache, SVGDrawer svgImpl) {
        _pdfDoc = new PDDocument();
        _svgImpl = svgImpl;
        _dotsPerPoint = dotsPerPoint;
        _testMode = testMode;
        _outputDevice = new PdfBoxOutputDevice(dotsPerPoint, testMode);
        _outputDevice.setWriter(_pdfDoc);
        
        PdfBoxUserAgent userAgent = new PdfBoxUserAgent(_outputDevice);

        if (factory != null) {
            userAgent.setHttpStreamFactory(factory);
        }
        
        if (_resolver != null) {
            userAgent.setUriResolver(_resolver);
        }
        
        if (_cache != null) {
            userAgent.setExternalCache(_cache);
        }
        
        _sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc);
        _sharedContext.setFontResolver(fontResolver);

        PdfBoxReplacedElementFactory replacedElementFactory = new PdfBoxReplacedElementFactory(_outputDevice, svgImpl, null);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new PdfBoxTextRenderer());
        _sharedContext.setDPI(DEFAULT_PDF_POINTS_PER_INCH * _dotsPerPoint);
        _sharedContext.setDotsPerPixel(dotsPerPixel);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
    }

    /**
     * This method is constantly changing as options are added to the builder.
     */
    PdfBoxRenderer(BaseDocument doc, UnicodeImplementation unicode, 
            HttpStreamFactory httpStreamFactory, 
            OutputStream os, FSUriResolver resolver, FSCache cache, SVGDrawer svgImpl,
            PageDimensions pageSize, float pdfVersion, String replacementText, boolean testMode, FSObjectDrawerFactory objectDrawerFactory) {
        
        _pdfDoc = new PDDocument();
        _pdfDoc.setVersion(pdfVersion);

        _svgImpl = svgImpl;
        _dotsPerPoint = DEFAULT_DOTS_PER_POINT;
        _testMode = testMode;
        _outputDevice = new PdfBoxOutputDevice(DEFAULT_DOTS_PER_POINT, testMode);
        _outputDevice.setWriter(_pdfDoc);
        
        PdfBoxUserAgent userAgent = new PdfBoxUserAgent(_outputDevice);

        if (httpStreamFactory != null) {
            userAgent.setHttpStreamFactory(httpStreamFactory);
        }
        
        if (resolver != null) {
            userAgent.setUriResolver(resolver);
        }
        
        if (cache != null) {
            userAgent.setExternalCache(cache);
        }
        
        _sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc);
        _sharedContext.setFontResolver(fontResolver);

        PdfBoxReplacedElementFactory replacedElementFactory = new PdfBoxReplacedElementFactory(_outputDevice, svgImpl, objectDrawerFactory);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new PdfBoxTextRenderer());
        _sharedContext.setDPI(DEFAULT_PDF_POINTS_PER_INCH * _dotsPerPoint);
        _sharedContext.setDotsPerPixel(DEFAULT_DOTS_PER_PIXEL);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
        
        this.getSharedContext().setDefaultPageSize(pageSize.w, pageSize.h, pageSize.isSizeInches);
        
        if (replacementText != null) {
            this.getSharedContext().setReplacementText(replacementText);
        }
        
        if (unicode.splitterFactory != null) {
            this._splitterFactory = unicode.splitterFactory;
        }
        
        if (unicode.reorderer != null) {
            this._reorderer = unicode.reorderer;
            this._outputDevice.setBidiReorderer(_reorderer);
        }
        
        if (unicode.lineBreaker != null) {
            _sharedContext.setLineBreaker(unicode.lineBreaker);
        }
        
        if (unicode.charBreaker != null) {
            _sharedContext.setCharacterBreaker(unicode.charBreaker);
        }
        
        if (unicode.toLowerTransformer != null) {
            _sharedContext.setUnicodeToLowerTransformer(unicode.toLowerTransformer);
        }
        
        if (unicode.toUpperTransformer != null) {
            _sharedContext.setUnicodeToUpperTransformer(unicode.toUpperTransformer);
        }
        
        if (unicode.toTitleTransformer != null) {
            _sharedContext.setUnicodeToTitleTransformer(unicode.toTitleTransformer);
        }
        
        this._defaultTextDirection = unicode.textDirection ? BidiSplitter.RTL : BidiSplitter.LTR;
        
        if (doc.html != null) {
            this.setDocumentFromStringP(doc.html, doc.baseUri);
        }
        else if (doc.document != null) {
            this.setDocumentP(doc.document, doc.baseUri);
        }
        else if (doc.uri != null) {
            this.setDocumentP(doc.uri);
        }
        else if (doc.file != null) {
            try {
                this.setDocumentP(doc.file);
            } catch (IOException e) {
                XRLog.exception("Problem trying to read input XHTML file", e);
                throw new RuntimeException("File IO problem", e);
            }
        }
        
        this._os = os;
    }

    public Document getDocument() {
        return _doc;
    }
    
    public PDDocument getPdfDocument() {
        return _pdfDoc;
    }
    
    /**
     * @deprecated Use builder instead.
     * @param splitterFactory
     */
    @Deprecated
    public void setBidiSplitter(BidiSplitterFactory splitterFactory) {
        this._splitterFactory = splitterFactory;
    }
    
    /**
     * @deprecated Use builder instead.
     * @param reorderer
     */
    @Deprecated
    public void setBidiReorderer(BidiReorderer reorderer) {
        this._reorderer = reorderer;
        this._outputDevice.setBidiReorderer(reorderer);
    }
    
    /**
     * @deprecated Use builder instead.
     * @param rtl
     */
    @Deprecated
    public void setDefaultTextDirection(boolean rtl) {
        if (rtl)
            _defaultTextDirection = BidiSplitter.RTL;
        else
            _defaultTextDirection = BidiSplitter.LTR;
    }
    
    /**
     * Get the PDF-BOX font resolver. Can be used to add fonts in code.
     * @return
     */
    public PdfBoxFontResolver getFontResolver() {
        return (PdfBoxFontResolver) _sharedContext.getFontResolver();
    }

    private Document loadDocument(String uri) {
        return _sharedContext.getUserAgentCallback().getXMLResource(uri).getDocument();
    }

    /**
     * @deprecated Use builder instead.
     */
    @Deprecated
    public void setDocument(String uri) {
        setDocumentP(loadDocument(uri), uri);
    }

    private void setDocumentP(String uri) {
        setDocumentP(loadDocument(uri), uri);
    }

    private void setDocumentP(Document doc, String url) {
        setDocumentP(doc, url, new XhtmlNamespaceHandler());
    }
    
    private void setDocumentP(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        setDocumentP(loadDocument(file.toURI().toURL().toExternalForm()), (parent == null ? "" : parent.toURI().toURL().toExternalForm()));
    }
    
    private void setDocumentFromStringP(String content, String baseUrl) {
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        Document dom = XMLResource.load(is).getDocument();
        setDocumentP(dom, baseUrl);
    }
    
    private void setDocumentP(Document doc, String url, NamespaceHandler nsh) {
        _doc = doc;

        getFontResolver().flushFontFaceFonts();

        _sharedContext.reset();
        if (Configuration.isTrue("xr.cache.stylesheets", true)) {
            _sharedContext.getCss().flushStyleSheets();
        } else {
            _sharedContext.getCss().flushAllStyleSheets();
        }
        _sharedContext.setBaseURL(url);
        _sharedContext.setNamespaceHandler(nsh);
        _sharedContext.getCss().setDocumentContext(_sharedContext, _sharedContext.getNamespaceHandler(), doc, new NullUserInterface());
        getFontResolver().importFontFaces(_sharedContext.getCss().getFontFaceRules());
        
        if (_svgImpl != null) {
            _svgImpl.importFontFaceRules(_sharedContext.getCss().getFontFaceRules(), _sharedContext);
        }
    }
    
    /**
     * @deprecated Use builder instead.
     */
    @Deprecated
    public void setDocument(Document doc, String url) {
        setDocumentP(doc, url);
    }

    /**
     * @deprecated Use builder instead.
     */
    @Deprecated
    public void setDocument(File file) throws IOException {
        setDocumentP(file);
    }

    /**
     * @deprecated Use builder instead.
     */
    @Deprecated
    public void setDocumentFromString(String content) {
        setDocumentFromStringP(content, null);
    }

    /**
     * @deprecated Use builder instead.
     */
    @Deprecated
    public void setDocumentFromString(String content, String baseUrl) {
        setDocumentFromStringP(content, baseUrl);
    }

    /**
     * @deprecated Use builder instead.
     */
    @Deprecated
    public void setDocument(Document doc, String url, NamespaceHandler nsh) {
        setDocumentP(doc, url, nsh);
    }

    /**
     * @deprecated Use builder instead.
     * @param v
     */
    @Deprecated
    public void setPDFVersion(float v) {
        _pdfDoc.setVersion(v);
        _pdfVersion = v;
    }

    public float getPDFVersion() {
        return _pdfVersion == 0f ? 1.7f : _pdfVersion;
    }

    public void layout() {
        LayoutContext c = newLayoutContext();
        BlockBox root = BoxBuilder.createRootBox(c, _doc);
        root.setContainingBlock(new ViewportBox(getInitialExtents(c)));
        root.layout(c);
        Dimension dim = root.getLayer().getPaintingDimension(c);
        root.getLayer().trimEmptyPages(c, dim.height);
        root.getLayer().layoutPages(c);
        _root = root;
    }

    private Rectangle getInitialExtents(LayoutContext c) {
        PageBox first = Layer.createPageBox(c, "first");

        return new Rectangle(0, 0, first.getContentWidth(c), first.getContentHeight(c));
    }

    private RenderingContext newRenderingContext() {
        RenderingContext result = _sharedContext.newRenderingContextInstance();
        result.setFontContext(new PdfBoxFontContext());

        result.setOutputDevice(_outputDevice);
        
        if (_reorderer != null)
            result.setBidiReorderer(_reorderer);
        
        _outputDevice.setRenderingContext(result);

        _sharedContext.getTextRenderer().setup(result.getFontContext());

        result.setRootLayer(_root.getLayer());

        return result;
    }

    private LayoutContext newLayoutContext() {
        LayoutContext result = _sharedContext.newLayoutContextInstance();
        result.setFontContext(new PdfBoxFontContext());
        
        if (_splitterFactory != null)
            result.setBidiSplitterFactory(_splitterFactory);
        
        if (_reorderer != null)
        	result.setBidiReorderer(_reorderer);

        result.setDefaultTextDirection(_defaultTextDirection);

        ((PdfBoxTextRenderer) _sharedContext.getTextRenderer()).setup(result.getFontContext(), _reorderer != null ? _reorderer : new SimpleBidiReorderer());

        return result;
    }

    public void createPDF() throws IOException {
        createPDF(_os);
    }
    
    public void createPDF(OutputStream os) throws IOException {
        createPDF(os, true, 0);
    }

    public void writeNextDocument() throws IOException {
        writeNextDocument(0);
    }

    public void writeNextDocument(int initialPageNo) throws IOException {
        List<PageBox> pages = _root.getLayer().getPages();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);

        PageBox firstPage = pages.get(0);
        Rectangle2D firstPageSize = new Rectangle2D.Float(0, 0,
                firstPage.getWidth(c) / _dotsPerPoint,
                firstPage.getHeight(c) / _dotsPerPoint);

        _outputDevice.setStartPageNo(_pdfDoc.getNumberOfPages());

        writePDF(pages, c, firstPageSize, _pdfDoc);
    }

    public void finishPDF() throws IOException {
        if (_pdfDoc != null) {
            fireOnClose();
            _pdfDoc.close();
        }
    }

    public void createPDF(OutputStream os, boolean finish) throws IOException {
        createPDF(os, finish, 0);
    }

    /**
     * <B>NOTE:</B> Caller is responsible for cleaning up the OutputStream if
     * something goes wrong.
     * 
     * @throws IOException
     */
    public void createPDF(OutputStream os, boolean finish, int initialPageNo) throws IOException {
        List<PageBox> pages = _root.getLayer().getPages();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);
        
        PageBox firstPage = pages.get(0);
        Rectangle2D firstPageSize = new Rectangle2D.Float(0, 0,
                firstPage.getWidth(c) / _dotsPerPoint,
                firstPage.getHeight(c) / _dotsPerPoint);

        if (_pdfVersion != 0f) {
            _pdfDoc.setVersion(_pdfVersion);
        }
        
        if (_pdfEncryption != null) {
            _pdfDoc.setEncryptionDictionary(_pdfEncryption);
        }

        firePreOpen();

        writePDF(pages, c, firstPageSize, _pdfDoc);

        if (finish) {
            fireOnClose();
            _pdfDoc.save(os);
            _pdfDoc.close();
        }
    }

    private void firePreOpen() {
        if (_listener != null) {
            _listener.preOpen(this);
        }
    }

    private void firePreWrite(int pageCount) {
        if (_listener != null) {
            _listener.preWrite(this, pageCount);
        }
    }

    private void fireOnClose() {
        if (_listener != null) {
            _listener.onClose(this);
        }
    }

    private void writePDF(List<PageBox> pages, RenderingContext c, Rectangle2D firstPageSize, PDDocument doc) throws IOException {
        _outputDevice.setRoot(_root);
        _outputDevice.start(_doc);
        
        PDPage page = new PDPage(new PDRectangle((float) firstPageSize.getWidth(), (float) firstPageSize.getHeight()));
        PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, !_testMode);
        doc.addPage(page);
        
        _outputDevice.initializePage(cs, page, (float) firstPageSize.getHeight());
        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data
        
        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = pages.get(i);
            
            c.setPage(i, currentPage);
            paintPage(c, currentPage);
            _outputDevice.finishPage();
            
            if (i != pageCount - 1) {
                PageBox nextPage = (PageBox) pages.get(i + 1);
                Rectangle2D nextPageSize = new Rectangle2D.Float(0, 0, nextPage.getWidth(c) / _dotsPerPoint,
                        nextPage.getHeight(c) / _dotsPerPoint);
                PDPage pageNext = new PDPage(new PDRectangle((float) nextPageSize.getWidth(), (float) nextPageSize.getHeight()));
                PDPageContentStream csNext = new PDPageContentStream(doc, pageNext, AppendMode.APPEND, !_testMode);
                doc.addPage(pageNext);
                _outputDevice.initializePage(csNext, pageNext, (float) nextPageSize.getHeight());
            }
        }

        _outputDevice.finish(c, _root);
    }

    // Sets the document information dictionary values from html metadata
    private void setDidValues(PDDocument doc) {
        PDDocumentInformation info = new PDDocumentInformation();
        
        info.setCreationDate(Calendar.getInstance());
        info.setProducer("openhtmltopdf.com");

        for (Metadata metadata : _outputDevice.getMetadata()) {
        	String name = metadata.getName();
        	String content = metadata.getContent();
        	if( content == null )
        	    continue;
            if( name.equals("title"))
                info.setTitle(content);
            else if( name.equals("author"))
                info.setAuthor(content);
            else if(name.equals("subject"))
                info.setSubject(content);
            else if(name.equals("keywords"))
                info.setKeywords(content);
            else
                info.setCustomMetadataValue(name,content);
        }

        doc.setDocumentInformation(info);
    }

    private void paintPage(RenderingContext c, PageBox page) throws IOException {
        // TODO: provideMetadataToPage(_pdfDoc, page);

        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        Shape working = _outputDevice.getClip();

        Rectangle content = page.getPrintClippingBounds(c);
        _outputDevice.clip(content);

        int top = -page.getPaintingTop() + page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        _outputDevice.translate(left, top);
        _root.getLayer().paint(c);
        _outputDevice.translate(-left, -top);

        _outputDevice.setClip(working);
    }
/* TODO : Metadata
    private void provideMetadataToPage(PdfWriter writer, PageBox page) throws IOException {
        byte[] metadata = null;
        if (page.getMetadata() != null) {
            try {
                String metadataBody = stringfyMetadata(page.getMetadata());
                if (metadataBody != null) {
                    metadata = createXPacket(stringfyMetadata(page.getMetadata())).getBytes("UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                // Can't happen
                throw new RuntimeException(e);
            }
        }

        if (metadata != null) {
            writer.setPageXmpMetadata(metadata);
        }
    }
*/
    private String stringfyMetadata(Element element) {
        Element target = getFirstChildElement(element);
        if (target == null) {
            return null;
        }

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter output = new StringWriter();
            transformer.transform(new DOMSource(target), new StreamResult(output));

            return output.toString();
        } catch (TransformerConfigurationException e) {
            // Things must be in pretty bad shape to get here so
            // rethrow as runtime exception
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private static Element getFirstChildElement(Element element) {
        Node n = element.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            }
            n = n.getNextSibling();
        }
        return null;
    }

    private String createXPacket(String metadata) {
        StringBuffer result = new StringBuffer(metadata.length() + 50);
        result.append("<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>\n");
        result.append(metadata);
        result.append("\n<?xpacket end='r'?>");

        return result.toString();
    }

    public PdfBoxOutputDevice getOutputDevice() {
        return _outputDevice;
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void exportText(Writer writer) throws IOException {
        RenderingContext c = newRenderingContext();
        c.setPageCount(_root.getLayer().getPages().size());
        _root.exportText(c, writer);
    }

    public BlockBox getRootBox() {
        return _root;
    }

    public float getDotsPerPoint() {
        return _dotsPerPoint;
    }

    public List findPagePositionsByID(Pattern pattern) {
        return _outputDevice.findPagePositionsByID(newLayoutContext(), pattern);
    }

    private static final class NullUserInterface implements UserInterface {
        public boolean isHover(Element e) {
            return false;
        }

        public boolean isActive(Element e) {
            return false;
        }

        public boolean isFocus(Element e) {
            return false;
        }
    }

    public PDFCreationListener getListener() {
        return _listener;
    }

    public void setListener(PDFCreationListener listener) {
        _listener = listener;
    }
    
    /**
     * Cleanup thread resources. MUST be called after finishing with the renderer.
     */
    public void cleanup() {
        _sharedContext.removeFromThread();
        ThreadCtx.cleanup();
    }
}

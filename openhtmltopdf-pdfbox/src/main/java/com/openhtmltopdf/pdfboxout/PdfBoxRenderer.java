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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.FSCache;
import com.openhtmltopdf.extend.FSUriResolver;
import com.openhtmltopdf.extend.HttpStreamFactory;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.extend.UserInterface;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.util.Configuration;

public class PdfBoxRenderer {
    // These two defaults combine to produce an effective resolution of 96 px to
    // the inch
    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
    private static final int DEFAULT_DOTS_PER_PIXEL = 20;

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

    public PdfBoxRenderer(boolean testMode) {
        this(DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL, true, testMode, null, null, null, null);
    }

    public PdfBoxRenderer(float dotsPerPoint, int dotsPerPixel, boolean useSubsets, boolean testMode, HttpStreamFactory factory, FSUriResolver _resolver, FSCache _cache, SVGDrawer svgImpl) {
        _pdfDoc = new PDDocument();
        
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
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc, useSubsets);
        _sharedContext.setFontResolver(fontResolver);

        PdfBoxReplacedElementFactory replacedElementFactory = new PdfBoxReplacedElementFactory(_outputDevice, svgImpl);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new PdfBoxTextRenderer());
        _sharedContext.setDPI(72 * _dotsPerPoint);
        _sharedContext.setDotsPerPixel(dotsPerPixel);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
    }

    public PdfBoxRenderer(boolean textDirection, boolean testMode,
            boolean useSubsets, HttpStreamFactory httpStreamFactory,
            BidiSplitterFactory splitterFactory, BidiReorderer reorderer, String html,
            Document document, String baseUri, String uri, File file,
            OutputStream os, FSUriResolver _resolver, FSCache _cache, SVGDrawer svgImpl) {
        this(DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL, useSubsets, testMode, httpStreamFactory, _resolver, _cache, svgImpl);
        
        if (splitterFactory != null) {
            this.setBidiSplitter(splitterFactory);
        }
        
        if (reorderer != null) {
            this.setBidiReorderer(reorderer);
        }
        
        if (html != null) {
            this.setDocumentFromString(html, baseUri);
        }
        else if (document != null) {
            this.setDocument(document, baseUri);
        }
        else if (uri != null) {
            this.setDocument(uri);
        }
        else if (file != null) {
            try {
                this.setDocument(file);
            } catch (IOException e) {
                throw new RuntimeException("File IO problem", e);
            }
        }
        
        this._os = os;
    }

    public Document getDocument() {
        return _doc;
    }

    private BidiSplitterFactory _splitterFactory;
    private byte _defaultTextDirection = BidiSplitter.LTR;
    private BidiReorderer _reorderer;
    
    public void setBidiSplitter(BidiSplitterFactory splitterFactory) {
        this._splitterFactory = splitterFactory;
    }

    public void setBidiReorderer(BidiReorderer reorderer) {
        this._reorderer = reorderer;
        this._outputDevice.setBidiReorderer(reorderer);
    }
    
    public void setDefaultTextDirection(boolean rtl) {
        if (rtl)
            _defaultTextDirection = BidiSplitter.RTL;
        else
            _defaultTextDirection = BidiSplitter.LTR;
    }
    
    public PdfBoxFontResolver getFontResolver() {
        return (PdfBoxFontResolver) _sharedContext.getFontResolver();
    }

    private Document loadDocument(final String uri) {
        return _sharedContext.getUac().getXMLResource(uri).getDocument();
    }

    public void setDocument(String uri) {
        setDocument(loadDocument(uri), uri);
    }

    public void setDocument(Document doc, String url) {
        setDocument(doc, url, new XhtmlNamespaceHandler());
    }

    public void setDocument(File file) throws IOException {

        File parent = file.getAbsoluteFile().getParentFile();
        setDocument(loadDocument(file.toURI().toURL().toExternalForm()), (parent == null ? "" : parent.toURI().toURL().toExternalForm()));
    }

    public void setDocumentFromString(String content) {
        setDocumentFromString(content, null);
    }

    public void setDocumentFromString(String content, String baseUrl) {
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        Document dom = XMLResource.load(is).getDocument();

        setDocument(dom, baseUrl);
    }

    public void setDocument(Document doc, String url, NamespaceHandler nsh) {
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
    }

    public PDEncryption getPDFEncryption() {
        return _pdfEncryption;
    }

    public void setPDFEncryption(PDEncryption pdfEncryption) {
        _pdfEncryption = pdfEncryption;
    }

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
        List pages = _root.getLayer().getPages();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);
        PageBox firstPage = (PageBox) pages.get(0);
        Rectangle2D firstPageSize = new Rectangle2D.Float(0, 0, firstPage.getWidth(c) / _dotsPerPoint,
                firstPage.getHeight(c) / _dotsPerPoint);

        _outputDevice.setStartPageNo(_pdfDoc.getNumberOfPages());

//        _pdfDoc.setPageSize(firstPageSize);
//        _pdfDoc.newPage();

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
        List pages = _root.getLayer().getPages();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);
        PageBox firstPage = (PageBox) pages.get(0);
        Rectangle2D firstPageSize = new Rectangle2D.Float(0, 0, firstPage.getWidth(c) / _dotsPerPoint,
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

    private void writePDF(List pages, RenderingContext c, Rectangle2D firstPageSize, PDDocument doc) throws IOException {
        _outputDevice.setRoot(_root);
        _outputDevice.start(_doc);
        
        PDPage page = new PDPage(new PDRectangle((float) firstPageSize.getWidth(), (float) firstPageSize.getHeight()));
        PDPageContentStream cs = new PDPageContentStream(doc, page, false, !_testMode);
        doc.addPage(page);
        _outputDevice.initializePage(cs, page, (float) firstPageSize.getHeight());

        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data
        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = (PageBox) pages.get(i);
            c.setPage(i, currentPage);
            paintPage(c, currentPage);
            _outputDevice.finishPage();
            if (i != pageCount - 1) {
                PageBox nextPage = (PageBox) pages.get(i + 1);
                Rectangle2D nextPageSize = new Rectangle2D.Float(0, 0, nextPage.getWidth(c) / _dotsPerPoint,
                        nextPage.getHeight(c) / _dotsPerPoint);
                PDPage pageNext = new PDPage(new PDRectangle((float) firstPageSize.getWidth(), (float) firstPageSize.getHeight()));
                PDPageContentStream csNext = new PDPageContentStream(doc, pageNext, false, !_testMode);
                doc.addPage(pageNext);
                _outputDevice.initializePage(csNext, pageNext, (float) nextPageSize.getHeight());
            }
        }

        _outputDevice.finish(c, _root);
    }

    // Sets the document information dictionary values from html metadata
    private void setDidValues(PDDocument doc) {
        String v = _outputDevice.getMetadataByName("title");

        PDDocumentInformation info = new PDDocumentInformation();
        
        if (v != null) {
            info.setTitle(v);
        }
        v = _outputDevice.getMetadataByName("author");
        if (v != null) {
            info.setAuthor(v);
        }
        v = _outputDevice.getMetadataByName("subject");
        if (v != null) {
            info.setSubject(v);
        }
        v = _outputDevice.getMetadataByName("keywords");
        if (v != null) {
            info.setKeywords(v);
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
}

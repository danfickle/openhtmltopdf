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
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.extend.FSDOMMutator;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.pdfboxout.PdfBoxSlowOutputDevice.Metadata;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.CacheStore;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.render.displaylist.DisplayListCollector;
import com.openhtmltopdf.render.displaylist.DisplayListContainer;
import com.openhtmltopdf.render.displaylist.DisplayListPainter;
import com.openhtmltopdf.render.displaylist.PagedBoxCollector;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.*;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PdfBoxRenderer implements Closeable, PageSupplier {
    // See discussion of units at top of PdfBoxOutputDevice.
    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
    private static final int DEFAULT_DOTS_PER_PIXEL = 20;
    private static final int DEFAULT_PDF_POINTS_PER_INCH = 72;

    private final SharedContext _sharedContext;
    private final PdfBoxOutputDevice _outputDevice;
    private final List<FSDOMMutator> _domMutators;

    private Document _doc;
    private BlockBox _root;

    private final float _dotsPerPoint;

    private PDDocument _pdfDoc;
    
    private PDEncryption _pdfEncryption;

    private String _producer;

    // Usually 1.7
    private float _pdfVersion;

    private PdfAConformance _pdfAConformance;
    private boolean _pdfUaConformance;

    private byte[] _colorProfile;

    private boolean _testMode;

    private PDFCreationListener _listener;
    
    private OutputStream _os;
    private SVGDrawer _svgImpl;
    private SVGDrawer _mathmlImpl;
    
    private BidiSplitterFactory _splitterFactory;
    private byte _defaultTextDirection = BidiSplitter.LTR;
    private BidiReorderer _reorderer;
    private final boolean _useFastMode;

    private PageSupplier _pageSupplier;

    private final Closeable diagnosticConsumer;
    
    private PdfRendererBuilderState state;

    /**
     * This method is constantly changing as options are added to the builder.
     */
    PdfBoxRenderer(BaseDocument doc, UnicodeImplementation unicode,
            PageDimensions pageSize, PdfRendererBuilderState state, Closeable diagnosticConsumer) {
        this.state = state;

        this.diagnosticConsumer = diagnosticConsumer;

        _pdfDoc = state.pddocument != null ? state.pddocument : new PDDocument();
        _pdfDoc.setVersion(state._pdfVersion);

        _producer = state._producer;

        _pageSupplier = state._pageSupplier != null ? state._pageSupplier : this;

        _svgImpl = state._svgImpl;
        _mathmlImpl = state._mathmlImpl;

        _pdfAConformance = state._pdfAConformance;
        _pdfUaConformance = state._pdfUaConform;
        _colorProfile = state._colorProfile;

        _dotsPerPoint = DEFAULT_DOTS_PER_POINT;
        _testMode = state._testMode;
        _useFastMode = state._useFastRenderer;
        _outputDevice = state._useFastRenderer ? 
                new PdfBoxFastOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode,
                        state._pdfUaConform || state._pdfAConformance.getConformanceValue().equals("A"),
                        state._pdfAConformance != PdfAConformance.NONE) : 
                new PdfBoxSlowOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode);
        _outputDevice.setWriter(_pdfDoc);
        _outputDevice.setStartPageNo(_pdfDoc.getNumberOfPages());
        
        PdfBoxUserAgent userAgent = new PdfBoxUserAgent(_outputDevice);

        if (_svgImpl != null) {
            _svgImpl.withUserAgent(userAgent);
        }

        userAgent.setProtocolsStreamFactory(state._streamFactoryMap);
        
        if (state._resolver != null) {
            userAgent.setUriResolver(state._resolver);
        }

        userAgent.setAccessController(ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI, state._beforeAccessController);
        userAgent.setAccessController(ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI, state._afterAccessController);

        _sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
        _sharedContext._preferredTransformerFactoryImplementationClass = state._preferredTransformerFactoryImplementationClass;
        _sharedContext._preferredDocumentBuilderFactoryImplementationClass = state._preferredDocumentBuilderFactoryImplementationClass;
        
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc, state._fontCache, state._caches.get(CacheStore.PDF_FONT_METRICS), state._pdfAConformance, state._pdfUaConform);
        _sharedContext.setFontResolver(fontResolver);

        PdfBoxReplacedElementFactory replacedElementFactory = new PdfBoxReplacedElementFactory(_outputDevice, state._svgImpl, state._objectDrawerFactory, state._mathmlImpl);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new PdfBoxTextRenderer());
        _sharedContext.setDPI(DEFAULT_PDF_POINTS_PER_INCH * _dotsPerPoint);
        _sharedContext.setDotsPerPixel(DEFAULT_DOTS_PER_PIXEL);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
        
        this.getSharedContext().setDefaultPageSize(pageSize.w, pageSize.h, pageSize.isSizeInches);
        
        if (state._replacementText != null) {
            this.getSharedContext().setReplacementText(state._replacementText);
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

        this._domMutators = state._domMutators;

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
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_PROBLEM_TRYING_TO_READ_INPUT_XHTML_FILE, e);
                throw new RuntimeException("File IO problem", e);
            }
        }
        
        this._os = state._os;
    }

    /**
     * Creates a new renderer builder inheriting this configuration.
     */
    public PdfRendererBuilder createBuilder() {
        PdfRendererBuilderState newState = state.clone();
        /*
         * NOTE: Old input references are cleared to ensure a clean new run.
         */
        newState._document = null;
        newState._file = null;
        newState._html = null;
        return new PdfRendererBuilder(newState);
    }

    public Document getDocument() {
        return _doc;
    }
    
    /**
     * Returns the PDDocument or null if it has been closed.
     */
    public PDDocument getPdfDocument() {
        return _pdfDoc;
    }
    
    /**
     * Get the PDF-BOX font resolver. Can be used to add fonts in code.
     * @return
     */
    public PdfBoxFontResolver getFontResolver() {
        return (PdfBoxFontResolver) _sharedContext.getFontResolver();
    }

    private Document loadDocument(String uri) {
        return _sharedContext.getUserAgentCallback().getXMLResource(uri, ExternalResourceType.XML_XHTML).getDocument();
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

        /*
         * Apply potential DOM mutations
         */
        for (FSDOMMutator domMutator : _domMutators)
            domMutator.mutateDocument(doc);

        _sharedContext.setBaseURL(url);
        _sharedContext.setNamespaceHandler(nsh);
        _sharedContext.getCss().setDocumentContext(_sharedContext, _sharedContext.getNamespaceHandler(), doc, new NullUserInterface());
        getFontResolver().importFontFaces(_sharedContext.getCss().getFontFaceRules());
        
        if (_svgImpl != null) {
            _svgImpl.importFontFaceRules(_sharedContext.getCss().getFontFaceRules(), _sharedContext);
        }
        
        if (_mathmlImpl != null) {
            _mathmlImpl.importFontFaceRules(_sharedContext.getCss().getFontFaceRules(), _sharedContext);
        }
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

    /**
     *  Creates a PDF with setup specified by builder. On finsihing or failing, saves (if successful) and closes the PDF document.
     */
    public void createPDF() throws IOException {
        createPDF(_os);
    }

    /**
     *  Creates a PDF with setup specified by builder. On finsihing or failing, DOES NOT save or close the PDF document.
     *  Useful for post-processing the PDDocument which can be retrieved by getPdfDocument().
     */
    public void createPDFWithoutClosing() throws IOException {
        createPDF(_os, false, 0);
    }
    
    /**
     * @deprecated Use builder to set output stream.
     * @param os
     * @throws IOException
     */
    @Deprecated
    public void createPDF(OutputStream os) throws IOException {
        createPDF(os, true, 0);
    }

    /**
     * @deprecated Doubt this still works as untested.
     * @throws IOException
     */
    @Deprecated 
    public void writeNextDocument() throws IOException {
        writeNextDocument(0);
    }

    /**
     * @deprecated Doubt this still works as untested.
     * @throws IOException
     */
    @Deprecated 
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

    /**
     * @deprecated
     * @throws IOException
     */
    @Deprecated 
    public void finishPDF() throws IOException {
        if (_pdfDoc != null) {
            fireOnClose();
            _pdfDoc.close();
        }
    }

    /**
     * @deprecated Use builder to set output stream.
     * @throws IOException
     */
    @Deprecated 
    public void createPDF(OutputStream os, boolean finish) throws IOException {
        createPDF(os, finish, 0);
    }

    /**
     * @deprecated Use builder to set output stream.
     * <B>NOTE:</B> Caller is responsible for cleaning up the OutputStream.
     * 
     * @throws IOException
     */
    @Deprecated
    public void createPDF(OutputStream os, boolean finish, int initialPageNo) throws IOException {
        if (_useFastMode) {
            createPdfFast(finish);
            return;
        }
        
        boolean success = false;
        
        try {
            // renders the layout if it wasn't created
            if (_root == null) {
                this.layout();
            }
            
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
            
            success = true;
        } finally {
            if (finish) {
                fireOnClose();
                if (success) {
                    _pdfDoc.save(os);
                }
                _pdfDoc.close();
                _pdfDoc = null;
            }
        }
    }
    
    /**
     * Go fast!
     */
    private void createPdfFast(boolean finish) throws IOException {
        boolean success = false;

        XRLog.log(Level.INFO, LogMessageId.LogMessageId0Param.GENERAL_PDF_USING_FAST_MODE);

        try {
            // renders the layout if it wasn't created
            if (_root == null) {
                this.layout();
            }
            
            List<PageBox> pages = _root.getLayer().getPages();

            RenderingContext c = newRenderingContext();
            c.setInitialPageNo(0);
            c.setFastRenderer(true);
        
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

            writePDFFast(pages, c, firstPageSize, _pdfDoc);
            
            success = true;
        } finally {
            if (finish) {
                fireOnClose();
                if (success) {
                    _pdfDoc.save(_os);
                }
                _pdfDoc.close();
                _pdfDoc = null;
            }
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
    
    private void writePDFFast(List<PageBox> pages, RenderingContext c, Rectangle2D firstPageSize, PDDocument doc) throws IOException {
        _outputDevice.setRoot(_root);
        _outputDevice.start(_doc);
        
        PDPage page = _pageSupplier.requestPage(doc, (float) firstPageSize.getWidth(), (float) firstPageSize.getHeight(), 0, -1);
        PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, !_testMode);
        
        _outputDevice.initializePage(cs, page, (float) firstPageSize.getHeight());
        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);
        
        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data
        
        if (_pdfUaConformance || _pdfAConformance != PdfAConformance.NONE) {
            addPdfASchema(doc, _pdfAConformance, _pdfUaConformance);
        }
        
        DisplayListCollector dlCollector = new DisplayListCollector(_root.getLayer().getPages());
        DisplayListContainer dlPages = dlCollector.collectRoot(c, _root.getLayer()); 

        int pdfPageIndex = 0;
        
        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = pages.get(i);
            currentPage.setBasePagePdfPageIndex(pdfPageIndex);
            DisplayListPageContainer pageOperations = dlPages.getPageInstructions(i);
            c.setPage(i, currentPage);
            c.setShadowPageNumber(-1);
            paintPageFast(c, currentPage, pageOperations, 0);
            _outputDevice.finishPage();
            pdfPageIndex++;
                        
            if (!pageOperations.shadowPages().isEmpty()) {
                currentPage.setShadowPageCount(pageOperations.shadowPages().size());
                
                int shadowPageIndex = 0;
                int pageContentWidth = currentPage.getContentWidth(c);
                int translateX = pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1);

                for (DisplayListPageContainer shadowPage : pageOperations.shadowPages()) {
                    PDPage shadowPdPage = 
                            _pageSupplier.requestPage(
                                    doc,
                                    (float) currentPage.getWidth(c) / _dotsPerPoint, 
                                    (float) currentPage.getHeight(c) / _dotsPerPoint, i, shadowPageIndex);
                    
                    PDPageContentStream shadowCs = new PDPageContentStream(doc, shadowPdPage, AppendMode.APPEND, !_testMode);

                    _outputDevice.initializePage(shadowCs, shadowPdPage, (float) currentPage.getHeight(c) / _dotsPerPoint);
                    c.setShadowPageNumber(shadowPageIndex);
                    paintPageFast(c, currentPage, shadowPage, -translateX);
                    _outputDevice.finishPage();
                    translateX += (pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1));
                    
                    pdfPageIndex++;
                    shadowPageIndex++;
                }
            }
            
            if (i != pageCount - 1) {
                PageBox nextPage = pages.get(i + 1);
                
                Rectangle2D nextPageSize = new Rectangle2D.Float(0, 0,
                        nextPage.getWidth(c) / _dotsPerPoint,
                        nextPage.getHeight(c) / _dotsPerPoint);
                
                PDPage pageNext = 
                        _pageSupplier.requestPage(doc, (float) nextPageSize.getWidth(), (float) nextPageSize.getHeight(), i + 1, -1);
                
                PDPageContentStream csNext = new PDPageContentStream(doc, pageNext, AppendMode.APPEND, !_testMode);
                _outputDevice.initializePage(csNext, pageNext, (float) nextPageSize.getHeight());
            }
        }
        
        _outputDevice.finish(c, _root);
    }

    private void writePDF(List<PageBox> pages, RenderingContext c, Rectangle2D firstPageSize, PDDocument doc) throws IOException {
        _outputDevice.setRoot(_root);
        _outputDevice.start(_doc);
        
        PDPage page = _pageSupplier.requestPage(doc, (float) firstPageSize.getWidth(), (float) firstPageSize.getHeight(), 0, -1);
        PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, !_testMode);
        
        _outputDevice.initializePage(cs, page, (float) firstPageSize.getHeight());
        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data

        if (_pdfUaConformance || _pdfAConformance != PdfAConformance.NONE) {
            addPdfASchema(doc, _pdfAConformance, _pdfUaConformance);
        }

        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = pages.get(i);
            
            c.setPage(i, currentPage);
            paintPage(c, currentPage);
            _outputDevice.finishPage();
            
            if (i != pageCount - 1) {
                PageBox nextPage = pages.get(i + 1);
                Rectangle2D nextPageSize = new Rectangle2D.Float(0, 0, nextPage.getWidth(c) / _dotsPerPoint,
                        nextPage.getHeight(c) / _dotsPerPoint);
                PDPage pageNext = 
                		_pageSupplier.requestPage(doc, (float) nextPageSize.getWidth(), (float) nextPageSize.getHeight(), i + 1, -1);
                PDPageContentStream csNext = new PDPageContentStream(doc, pageNext, AppendMode.APPEND, !_testMode);
                _outputDevice.initializePage(csNext, pageNext, (float) nextPageSize.getHeight());
            }
        }

        _outputDevice.finish(c, _root);
    }

    // Kindly provided by GurpusMaximus at:
    // https://stackoverflow.com/questions/49682339/how-can-i-create-an-accessible-pdf-with-java-pdfbox-2-0-8-library-that-is-also-v
    private void addPdfASchema(PDDocument document, PdfAConformance pdfAConformance, boolean isPdfUa) {
        PDDocumentInformation information = document.getDocumentInformation();
        XMPMetadata metadata = XMPMetadata.createXMPMetadata();

        try {
            // NOTE: These XMP metadata MUST match up with the document information dictionary
            // to be a valid PDF/A document, As per ISO 19005-1:2005/Cor.1:2007, 6.7.2
            String title = information.getTitle();
            String author = information.getAuthor();
            String subject = information.getSubject();
            String keywords = information.getKeywords();
            String creator = information.getCreator();
            String producer = information.getProducer();
            Calendar creationDate = information.getCreationDate();
            Calendar modDate = information.getModificationDate();

            if (isPdfUa && (title == null || title.isEmpty())) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.GENERAL_PDF_ACCESSIBILITY_NO_DOCUMENT_TITLE_PROVIDED);
            }

            if (pdfAConformance != PdfAConformance.NONE) {
                PDFAIdentificationSchema pdfaid = metadata.createAndAddPFAIdentificationSchema();
                pdfaid.setConformance(pdfAConformance.getConformanceValue());
                pdfaid.setPart(pdfAConformance.getPart());

                AdobePDFSchema pdfSchema = metadata.createAndAddAdobePDFSchema();
                pdfSchema.setPDFVersion(String.valueOf(pdfAConformance.getPdfVersion()));
                if (keywords != null) {
                    pdfSchema.setKeywords(keywords);
                }
                if (producer != null) {
                    pdfSchema.setProducer(producer);
                }

                XMPBasicSchema xmpBasicSchema = metadata.createAndAddXMPBasicSchema();
                if (creator != null) {
                    xmpBasicSchema.setCreatorTool(creator);
                }
                if (creationDate != null) {
                    xmpBasicSchema.setCreateDate(creationDate);
                }
                if (modDate != null) {
                    xmpBasicSchema.setModifyDate(modDate);
                }


                DublinCoreSchema dc = metadata.createAndAddDublinCoreSchema();
                dc.setFormat("application/pdf");
                if (author != null) {
                    dc.addCreator(author);
                }
                if (title != null) {
                    dc.setTitle(title);
                }
                if (subject != null) {
                    dc.setDescription(subject);
                } else if (isPdfUa) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.GENERAL_PDF_ACCESSIBILITY_NO_DOCUMENT_DESCRIPTION_PROVIDED);
                }
            }

            PDFAExtensionSchema pdfAExt = metadata.createAndAddPDFAExtensionSchemaWithDefaultNS();
            pdfAExt.addNamespace("http://www.aiim.org/pdfa/ns/extension/", "pdfaExtension");
            pdfAExt.addNamespace("http://www.aiim.org/pdfa/ns/schema#", "pdfaSchema");
            pdfAExt.addNamespace("http://www.aiim.org/pdfa/ns/property#", "pdfaProperty");

            if (pdfAConformance != PdfAConformance.NONE) {
                // Description of Adobe PDF Schema
                List<XMPSchema> pdfProperties = new ArrayList<>(3);
                pdfProperties.add(
                        createPdfaProperty("internal", "The PDF file version.", "PDFVersion", "Text"));
                pdfProperties.add(
                        createPdfaProperty("external", "Keywords.", "Keywords", "Text"));
                pdfProperties.add(
                        createPdfaProperty("internal", "The name of the tool that created the PDF document.", "Producer", "AgentName"));
                pdfAExt.addBagValue("schemas",
                        createPdfaSchema("Adobe PDF Schema", "http://ns.adobe.com/pdf/1.3/", "pdf", pdfProperties));

                // Description of PDF/A ID Schema
                List<XMPSchema> pdfaidProperties = new ArrayList<>(2);
                pdfaidProperties.add(
                        createPdfaProperty("internal", "Part of PDF/A standard", "part", "Integer"));
                pdfaidProperties.add(
                        createPdfaProperty("internal", "Conformance level of PDF/A standard", "conformance", "Text"));
                pdfAExt.addBagValue("schemas",
                        createPdfaSchema("PDF/A ID Schema", "http://www.aiim.org/pdfa/ns/id/", "pdfaid", pdfaidProperties));
            }
            if (isPdfUa) {
                // Description of PDF/UA
                List<XMPSchema> pdfUaProperties = new ArrayList<>(1);
                pdfUaProperties.add(
                        createPdfaProperty("internal", "Indicates, which part of ISO 14289 standard is followed", "part", "Integer"));
                XMPSchema pdfUa = createPdfaSchema("PDF/UA Universal Accessibility Schema", "http://www.aiim.org/pdfua/ns/id/", "pdfuaid" , pdfUaProperties);
                pdfAExt.addBagValue("schemas", pdfUa);
                pdfAExt.addNamespace("http://www.aiim.org/pdfua/ns/id/", "pdfuaid");
                pdfAExt.setPrefix("pdfuaid");
                pdfAExt.setTextPropertyValue("part", "1");
            }

            PDMetadata metadataStream = new PDMetadata(document);
            PDMarkInfo markInfo = new PDMarkInfo();
            markInfo.setMarked(true);

            // add to catalog
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            catalog.setMetadata(metadataStream);
            catalog.setMarkInfo(markInfo);
            
            String lang = _doc.getDocumentElement().getAttribute("lang");
            catalog.setLanguage(!lang.isEmpty() ? lang : "EN-US");
            catalog.setViewerPreferences(new PDViewerPreferences(new COSDictionary()));
            catalog.getViewerPreferences().setDisplayDocTitle(true);

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(metadata, baos, true);
            String xmp = baos.toString("UTF-8");
            // Fix for bad XML generation by some transformers
            xmp = xmp.replace(" lang=\"x-default\"", " xml:lang=\"x-default\"");
            metadataStream.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));

            if (_colorProfile != null) {
                ByteArrayInputStream colorProfile = new ByteArrayInputStream(_colorProfile);
                PDOutputIntent oi = new PDOutputIntent(document, colorProfile);
                oi.setInfo("sRGB IEC61966-2.1");
                oi.setOutputCondition("sRGB IEC61966-2.1");
                oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
                oi.setRegistryName("http://www.color.org");
                catalog.addOutputIntent(oi);
            }
        } catch (BadFieldValueException | IOException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    // Creates an XML Schema to be used in the PDFA Extension
    private XMPSchema createPdfaSchema(String schema, String namespace, String prefix, List<XMPSchema> properties) {
        XMPSchema xmpSchema = new XMPSchema(XMPMetadata.createXMPMetadata(),
                "pdfaSchema", "pdfaSchema", "pdfaSchema");
        xmpSchema.setTextPropertyValue("schema", schema);
        xmpSchema.setTextPropertyValue("namespaceURI", namespace);
        xmpSchema.setTextPropertyValue("prefix", prefix);
        for (XMPSchema property : properties) {
            xmpSchema.addUnqualifiedSequenceValue("property", property);
        }
        return xmpSchema;
    }

    // Creates an XML Property to be used in the PDFA Extension
    private XMPSchema createPdfaProperty(String category, String description, String name, String valueType) {
        XMPSchema xmpSchema = new XMPSchema(XMPMetadata.createXMPMetadata(),
                "pdfaProperty", "pdfaProperty", "pdfaProperty");
        xmpSchema.setTextPropertyValue("name", name);
        xmpSchema.setTextPropertyValue("valueType", valueType);
        xmpSchema.setTextPropertyValue("category", category);
        xmpSchema.setTextPropertyValue("description", description);
        return xmpSchema;
    }

    // Sets the document information dictionary values from html metadata
    private void setDidValues(PDDocument doc) {
        PDDocumentInformation info = new PDDocumentInformation();

        info.setCreationDate(Calendar.getInstance());

        if (_producer == null) {
            info.setProducer("openhtmltopdf.com");
        } else {
            info.setProducer(_producer);
        }

        for (Metadata metadata : _outputDevice.getMetadata()) {
        	String name = metadata.getName();
			if (name.isEmpty())
				continue;
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
    
    private void paintPageFast(RenderingContext c, PageBox page, DisplayListPageContainer pageOperations, int additionalTranslateX) {
        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        
        c.setInPageMargins(true);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        c.setInPageMargins(false);
        
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        Rectangle content = page.getPrintClippingBounds(c);
        _outputDevice.pushClip(content);

        int top = -page.getPaintingTop() + page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        int translateX = left + additionalTranslateX;
        
        _outputDevice.translate(translateX, top);
        DisplayListPainter painter = new DisplayListPainter();
        painter.paint(c, pageOperations);
        _outputDevice.translate(-translateX, -top);

        _outputDevice.popClip();
    }

    private void paintPage(RenderingContext c, PageBox page) {
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
        } catch (TransformerException e) {
            // Things must be in pretty bad shape to get here so
            // rethrow as runtime exception
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
        StringBuilder result = new StringBuilder(metadata.length() + 50);
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

    /**
     * @deprecated unused and untested.
     * @param writer
     * @throws IOException
     */
    @Deprecated
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

    public List<PagePosition<Box>> findPagePositionsByID(Pattern pattern) {
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
     * @deprecated Use close instead.
     */
    @Deprecated
    public void cleanup() {
        _outputDevice.close();
        _sharedContext.removeFromThread();
        try {
            diagnosticConsumer.close();
        } catch (IOException e) {
        }
        ThreadCtx.cleanup();

        // Close all still open font files
        ((PdfBoxFontResolver)getSharedContext().getFontResolver()).close();

        if (_svgImpl != null) {
            try {
                _svgImpl.close();
            } catch (IOException e) {
            }
        }
        
        if (_mathmlImpl != null) {
            try {
                _mathmlImpl.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Cleanup thread resources. MUST be called after finishing with the renderer.
     */
    @Override
    public void close() {
        this.cleanup();
    }
    
    @Override
    public PDPage requestPage(PDDocument doc, float pageWidth, float pageHeight, int pageNumber, int shadowPageNumber) {
    	PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
    	doc.addPage(page);
    	return page;
    }

    /**
     * Start page to end page and then top to bottom on page.
     */
    private final Comparator<PagePosition<?>> PAGE_POSITION_COMPARATOR =
       Comparator.comparingInt(PagePosition<?>::getPageNo)
                 .thenComparing(Comparator.comparingDouble(PagePosition<?>::getY).reversed());

    /**
     * Returns the bottom Y postion in bottom-up PDF units
     * on the last page of content.
     * 
     * <strong>WARNING:</strong> NOT transform aware.
     */
    public float getLastContentBottom() {
        List<PagePosition<Layer>> positions = getLayersPositions();

        if (positions.isEmpty()) {
            return 0;
        }

        return positions.get(positions.size() - 1).getY();
    }

    /**
     * Returns a list of page positions for all layers in the document.
     * The page positions are sorted from first page to last and then top to bottom.
     * The page position values are in bottom-up PDF units.
     *
     * <strong>WARNING:</strong> NOT transform aware. Transformed layers will return page
     * positions that are not correct.
     */
    public List<PagePosition<Layer>> getLayersPositions() {
        if (getRootBox() == null) {
            this.layout();
        }

        Layer rootLayer = getRootBox().getLayer();

        int[] whiches = new int[] { Layer.NEGATIVE, Layer.AUTO, Layer.ZERO, Layer.POSITIVE };

        List<Layer> layers =
        Arrays.stream(whiches)
              .mapToObj(rootLayer::collectLayers)
              .flatMap(List::stream)
              .collect(Collectors.toList());

        RenderingContext ctx = newRenderingContext();
        List<PageBox> pages = rootLayer.getPages();

        List<PagePosition<Layer>> ret = new ArrayList<>();

        ret.addAll(getLayerPagePositions(rootLayer, pages, ctx));

        layers.stream()
              .map(layer -> getLayerPagePositions(layer, pages, ctx))
              .forEach(ret::addAll);

        Collections.sort(ret, PAGE_POSITION_COMPARATOR);

        return ret;
    }

    /**
     * Returns a list of page positions for a single layer.
     * The page positions are sorted from first page to last and then top to bottom.
     * The page position values are in bottom-up PDF units.
     *
     * Compare to {@link #getLayersPositions()} which will return page
     * positions for all layers.
     *
     * <strong>WARNING:</strong> NOT transform aware. A transformed layer will return page
     * positions that are not correct.
     */
    public List<PagePosition<Layer>> getLayerPositions(Layer layer) {
        RenderingContext ctx = newRenderingContext();
        List<PageBox> pages = layer.getPages();

        List<PagePosition<Layer>> ret = getLayerPagePositions(layer, pages, ctx);

        Collections.sort(ret, PAGE_POSITION_COMPARATOR);

        return ret;
    }

    private List<PagePosition<Layer>> getLayerPagePositions(
            Layer layer, List<PageBox> pages, RenderingContext ctx) {

        // FIXME: This method is not transform aware.

        Box box = layer.getMaster();

        int start = findStartPage(ctx, layer, pages);
        int end = findEndPage(ctx, layer, pages);

        if (box.getStyle().isFixed()) {
            PageBox page = pages.get(start);

            float x = box.getAbsX() + page.getMarginBorderPadding(ctx, CalculatedStyle.LEFT);
            float w = box.getEffectiveWidth();
            float y = page.getMarginBorderPadding(ctx, CalculatedStyle.BOTTOM) +
                        (page.getPaintingBottom() - box.getAbsY() - box.getHeight());
            float h = box.getHeight();

            return IntStream.range(0, pages.size())
                            .mapToObj(pageNo -> createPagePosition(null, layer, pageNo, x, y, w, h))
                            .collect(Collectors.toList());
        }

        List<PagePosition<Layer>> ret = new ArrayList<>((end - start) + 1);

        for (int i = start; i <= end; i++) {
            PageBox page = pages.get(i);

            float x = box.getAbsX() + page.getMarginBorderPadding(ctx, CalculatedStyle.LEFT);
            float w = box.getEffectiveWidth();

            float y;
            float h;

            if (start != end) {
                if (i != start && i != end) {
                    y = page.getMarginBorderPadding(ctx, CalculatedStyle.BOTTOM);
                    h = page.getContentHeight(ctx);
                } else if (i == end) {
                    h = (box.getAbsY() + box.getHeight()) - page.getPaintingTop();
                    y = page.getMarginBorderPadding(ctx, CalculatedStyle.BOTTOM) +
                        page.getContentHeight(ctx) - h;
                } else {
                    assert i == start;
                    y = page.getMarginBorderPadding(ctx, CalculatedStyle.BOTTOM);
                    h = page.getPaintingBottom() - box.getAbsY();
                }
            } else {
                y = page.getMarginBorderPadding(ctx, CalculatedStyle.BOTTOM) +
                     (page.getPaintingBottom() - box.getAbsY() - box.getHeight());
                h = box.getHeight();
            }

            PagePosition<Layer> pos = createPagePosition(null, layer, i, x, y, w, h);

            ret.add(pos);
        }

        return ret;
    }

    private <T> PagePosition<T> createPagePosition(
            String id, T element, int pageNo, float x, float y, float w, float h) {

        return new PagePosition<>(
                id, element, pageNo, x / _dotsPerPoint, y / _dotsPerPoint, w / _dotsPerPoint, h / _dotsPerPoint);
    }

    /**
     * Returns the start page for a layer. Transform aware.
     */
    private int findStartPage(RenderingContext c, Layer layer, List<PageBox> pages) {
        int start = PagedBoxCollector.findStartPage(c, layer.getMaster(), pages);

        // Floats maybe outside the master box.
        for (BlockBox floater : layer.getFloats()) {
            start = Math.min(start, PagedBoxCollector.findStartPage(c, floater, pages));
        }

        return start;
    }

    /**
     * Returns the end page number for a layer. Transform aware.
     */
    private int findEndPage(RenderingContext c, Layer layer, List<PageBox> pages) {
        int end = PagedBoxCollector.findEndPage(c, layer.getMaster(), pages);

        // Floats may be outside the master box.
        for (BlockBox floater : layer.getFloats()) {
            end = Math.max(end, PagedBoxCollector.findEndPage(c, floater, pages));
        }

        return end;
    }
}

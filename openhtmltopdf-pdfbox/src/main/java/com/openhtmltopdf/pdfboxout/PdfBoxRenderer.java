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
import com.openhtmltopdf.pdfboxout.PdfBoxUtil.Metadata;
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
import com.openhtmltopdf.util.OpenUtil;
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
import org.xml.sax.InputSource;

import javax.xml.transform.*;
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

    private PageSupplier _pageSupplier;

    private final Closeable _diagnosticConsumer;

    private final int _initialPageNumber;

    /**
     * This method is constantly changing as options are added to the builder.
     */
    PdfBoxRenderer(
            BaseDocument doc,
            UnicodeImplementation unicode,
            PageDimensions pageSize,
            PdfRendererBuilderState state,
            Closeable diagnosticConsumer) {

        PdfBoxFontResolver fontResolver = null;
        _pdfDoc = state.pddocument != null ? state.pddocument : new PDDocument();

        try {
            _diagnosticConsumer = diagnosticConsumer;

            _pdfDoc.setVersion(state._pdfVersion);
            _pdfVersion = state._pdfVersion;

            _producer = state._producer;

            _pageSupplier = state._pageSupplier != null ? state._pageSupplier : this;

            _svgImpl = state._svgImpl;
            _mathmlImpl = state._mathmlImpl;

            _pdfAConformance = state._pdfAConformance;
            _pdfUaConformance = state._pdfUaConform;
            _colorProfile = state._colorProfile;

            _dotsPerPoint = DEFAULT_DOTS_PER_POINT;
            _testMode = state._testMode;
            _outputDevice = 
                    new PdfBoxFastOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode,
                            state._pdfUaConform || state._pdfAConformance.getConformanceValue().equals("A"),
                            state._pdfAConformance != PdfAConformance.NONE);
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

            fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc, state._caches.get(CacheStore.PDF_FONT_METRICS), state._pdfAConformance, state._pdfUaConform);
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
            this._initialPageNumber = state._initialPageNumber;
        } catch (Throwable e) {
            if (state.pddocument == null) {
                // We created it but exceptioned out before constructor
                // finished so close here.
                OpenUtil.closeQuietly(_pdfDoc);
            }

            OpenUtil.closeQuietly(fontResolver);

            throw e;
        }
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
        Box viewport = new ViewportBox(getInitialExtents(c));

        root.setContainingBlock(viewport);
        root.layout(c);

        // Useful to see the box tree after layout.
        // System.out.println(com.openhtmltopdf.util.LambdaUtil.descendantDump(root));

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
        createPdfFast(true, 0);
    }

    /**
     *  Creates a PDF with setup specified by builder.
     *  On finsihing or failing, <strong>DOES NOT</strong> save or close the PDF document.
     *  Useful for post-processing the PDDocument which can be retrieved by {@link #getPdfDocument()}.
     *  <br><br>
     *  Prefer {@link #createPDFKeepOpen()} with try-with-resources.
     */
    public void createPDFWithoutClosing() throws IOException {
        createPdfFast(false, 0);
    }

    /**
     *  Creates a PDF with setup specified by builder.
     *  On finishing or failing, <strong>DOES NOT</strong> save or close the PDF document.
     *  Useful for post-processing the PDDocument which is returned.
     *  <br><br>
     *  NOTE: It is recommended to use this method with try-with-resources
     *  to avoid leaving the PDDocument open.
     */
    public PDDocument createPDFKeepOpen() throws IOException {
        createPDFWithoutClosing();
        return getPdfDocument();
    }

    /**
     * @deprecated
     */
    @Deprecated 
    public void finishPDF() throws IOException {
        if (_pdfDoc != null) {
            fireOnClose();
            OpenUtil.closeQuietly(_pdfDoc);
        }
    }

    /**
     * Go fast!
     */
    private void createPdfFast(boolean finish, int initialPageNo) throws IOException {
        boolean success = false;

        try {
            XRLog.log(Level.INFO, LogMessageId.LogMessageId0Param.GENERAL_PDF_USING_FAST_MODE);

            // renders the layout if it wasn't created
            if (_root == null) {
                this.layout();
            }

            List<PageBox> pages = _root.getLayer().getPages();

            RenderingContext c = newRenderingContext();
            c.setInitialPageNo(initialPageNo != 0 ? initialPageNo : _initialPageNumber);
            c.setFastRenderer(true);

            PageBox firstPage = pages.get(0);
            Rectangle2D firstPageSize = new Rectangle2D.Float(0, 0,
                    firstPage.getWidth(c) / _dotsPerPoint,
                    firstPage.getHeight(c) / _dotsPerPoint);

            if (_pdfEncryption != null) {
                _pdfDoc.setEncryptionDictionary(_pdfEncryption);
            }

            firePreOpen();

            writePDFFast(pages, c, firstPageSize, _pdfDoc);

            success = true;
        } finally {
            if (finish) {
                try {
                    fireOnClose();
                    if (success) {
                        _pdfDoc.save(_os);
                    }
                } finally {
                    OpenUtil.closeQuietly(_pdfDoc);
                    _pdfDoc = null;
                }
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

    private PDPageContentStream initPage(
            PDDocument doc, float w, float h, int mainPageIndex, int shadowPageIndex) throws IOException {

        PDPage page = _pageSupplier.requestPage(doc, w, h, mainPageIndex, shadowPageIndex);

        PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, !_testMode);

        _outputDevice.initializePage(cs, page, h);
        
        return cs;
    }

    private void writePDFFast(
            List<PageBox> pages,
            RenderingContext c,
            Rectangle2D firstPageSize,
            PDDocument doc) throws IOException {

        _outputDevice.setRoot(_root);
        _outputDevice.start(_doc);

        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);

        int pdfPageIndex = 0;
        DisplayListContainer dlPages = null;

        for (int i = 0; i < pageCount; i++) {
            float nextW;
            float nextH;
            PageBox page = pages.get(i);

            if (i == 0) {
                nextW = (float) firstPageSize.getWidth();
                nextH = (float) firstPageSize.getHeight();
            } else {
                nextW = page.getWidth(c) / _dotsPerPoint;
                nextH = page.getHeight(c) / _dotsPerPoint;
            }

            DisplayListPageContainer pageOperations;

            try (PDPageContentStream cs = initPage(doc, nextW, nextH, i, -1)) {
                if (i == 0) {
                    firePreWrite(pageCount); // opportunity to adjust meta data
                    setDidValues(doc);       // set PDF header fields from meta data

                    if (_pdfUaConformance || _pdfAConformance != PdfAConformance.NONE) {
                        addPdfASchema(doc, _pdfAConformance, _pdfUaConformance);
                    }

                    DisplayListCollector dlCollector = new DisplayListCollector(_root.getLayer().getPages());
                    dlPages = dlCollector.collectRoot(c, _root.getLayer());
                }

                page.setBasePagePdfPageIndex(pdfPageIndex);

                pageOperations = dlPages.getPageInstructions(i);

                c.setPage(i, page);
                c.setShadowPageNumber(-1);

                paintPageFast(c, page, pageOperations, 0);

                _outputDevice.finishPage();
            }

            pdfPageIndex++;

            if (!pageOperations.shadowPages().isEmpty()) {
                paintShadowPages(
                   c, doc, pdfPageIndex, page, pageOperations.shadowPages());

                pdfPageIndex += pageOperations.shadowPages().size();
            }
        }

        _outputDevice.finish(c, _root);
    }

    /**
     * Shadow pages are an opt-in feature that allows cut off content beyond
     * the right edge (or left edge for RTL mode) of the main page to be
     * output as a series of shadow pages.
     * 
     * It may be useful for example for large tables.
     */
    private void paintShadowPages(
            RenderingContext c,
            PDDocument doc,
            int mainPageIndex,
            PageBox currentPage,
            List<DisplayListPageContainer> shadows) throws IOException {

        int count = shadows.size();

        currentPage.setShadowPageCount(count);

        int pageContentWidth = currentPage.getContentWidth(c);
        int translateIncrement = pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1);
        int translateX = translateIncrement;

        for (int i = 0; i < count; i++) {
            DisplayListPageContainer shadow = shadows.get(i);

            float shadowWidth = currentPage.getWidth(c) / _dotsPerPoint;
            float shadowHeight = currentPage.getHeight(c) / _dotsPerPoint;

            PDPage shadowPdPage = 
                _pageSupplier.requestPage(doc, shadowWidth, shadowHeight, mainPageIndex, i);

            try (PDPageContentStream shadowCs = new PDPageContentStream(doc, shadowPdPage, AppendMode.APPEND, !_testMode)) {
                _outputDevice.initializePage(shadowCs, shadowPdPage, shadowHeight);
                c.setShadowPageNumber(i);

                paintPageFast(c, currentPage, shadow, -translateX);

                _outputDevice.finishPage();
            }

            translateX += translateIncrement;
        }
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
                XRLog.log(Level.WARNING,
                        LogMessageId.LogMessageId0Param.GENERAL_PDF_ACCESSIBILITY_NO_DOCUMENT_DESCRIPTION_PROVIDED);
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

    /**
     * Creates an XML Schema to be used in the PDFA Extension
     */
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

    /**
     * Creates an XML Property to be used in the PDFA Extension
     */
    private XMPSchema createPdfaProperty(String category, String description, String name, String valueType) {
        XMPSchema xmpSchema = new XMPSchema(XMPMetadata.createXMPMetadata(),
                "pdfaProperty", "pdfaProperty", "pdfaProperty");
        xmpSchema.setTextPropertyValue("name", name);
        xmpSchema.setTextPropertyValue("valueType", valueType);
        xmpSchema.setTextPropertyValue("category", category);
        xmpSchema.setTextPropertyValue("description", description);
        return xmpSchema;
    }

    /**
     * Sets the document information dictionary values from html metadata
     */
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

    public PdfBoxOutputDevice getOutputDevice() {
        return _outputDevice;
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
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
        @Override
        public boolean isHover(Element e) {
            return false;
        }

        @Override
        public boolean isActive(Element e) {
            return false;
        }

        @Override
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

    private void cleanup() {
        OpenUtil.closeQuietly(_outputDevice);
        OpenUtil.tryQuietly(_sharedContext::removeFromThread);
        OpenUtil.closeQuietly(_diagnosticConsumer);
        OpenUtil.tryQuietly(ThreadCtx::cleanup);

        // Close all still open font files
        OpenUtil.closeQuietly((PdfBoxFontResolver) getSharedContext().getFontResolver());

        if (_svgImpl != null) {
            OpenUtil.closeQuietly(_svgImpl);
        }

        if (_mathmlImpl != null) {
            OpenUtil.closeQuietly(_mathmlImpl);
        }
    }

    /**
     * Cleanup thread resources. 
     * <strong>MUST</strong> be called after finishing with the renderer.
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

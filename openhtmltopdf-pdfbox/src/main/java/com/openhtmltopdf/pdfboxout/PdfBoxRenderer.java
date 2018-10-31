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
import com.openhtmltopdf.extend.FSDOMMutator;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.pdfboxout.PdfBoxSlowOutputDevice.Metadata;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.CacheStore;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.render.displaylist.DisplayListCollector;
import com.openhtmltopdf.render.displaylist.DisplayListContainer;
import com.openhtmltopdf.render.displaylist.DisplayListPainter;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.util.Configuration;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
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
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PdfBoxRenderer implements Closeable {
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

    /**
     * This method is constantly changing as options are added to the builder.
     */
    PdfBoxRenderer(BaseDocument doc, UnicodeImplementation unicode,
            PageDimensions pageSize, PdfRendererBuilderState state) {

        _pdfDoc = state.pddocument != null ? state.pddocument : new PDDocument();
        _pdfDoc.setVersion(state._pdfVersion);

        _producer = state._producer;


        _svgImpl = state._svgImpl;
        _mathmlImpl = state._mathmlImpl;

        _pdfAConformance = state._pdfAConformance;
        _colorProfile = state._colorProfile;

        _dotsPerPoint = DEFAULT_DOTS_PER_POINT;
        _testMode = state._testMode;
        _useFastMode = state._useFastRenderer;
        _outputDevice = state._useFastRenderer ? new PdfBoxFastOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode) : new PdfBoxSlowOutputDevice(DEFAULT_DOTS_PER_POINT, _testMode);
        _outputDevice.setWriter(_pdfDoc);
        _outputDevice.setStartPageNo(_pdfDoc.getNumberOfPages());
        
        PdfBoxUserAgent userAgent = new PdfBoxUserAgent(_outputDevice);

        userAgent.setProtocolsStreamFactory(state._streamFactoryMap);
        
        if (state._resolver != null) {
            userAgent.setUriResolver(state._resolver);
        }
        
        if (state._cache != null) {
            userAgent.setExternalCache(state._cache);
        }
        
        if (state._textCache != null) {
            userAgent.setExternalTextCache(state._textCache);
        }
        
        if (state._byteCache != null) {
            userAgent.setExternalByteCache(state._byteCache);
        }
        
        _sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
        _sharedContext._preferredTransformerFactoryImplementationClass = state._preferredTransformerFactoryImplementationClass;
        _sharedContext._preferredDocumentBuilderFactoryImplementationClass = state._preferredDocumentBuilderFactoryImplementationClass;
        
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        PdfBoxFontResolver fontResolver = new PdfBoxFontResolver(_sharedContext, _pdfDoc, state._caches.get(CacheStore.PDF_FONT_METRICS), state._pdfAConformance);
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
                XRLog.exception("Problem trying to read input XHTML file", e);
                throw new RuntimeException("File IO problem", e);
            }
        }
        
        this._os = state._os;
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
        return _sharedContext.getUserAgentCallback().getXMLResource(uri).getDocument();
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

        getFontResolver().flushFontFaceFonts();

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
     * Completely experimental for now. Buggy as heck!
     * TODO:
     *   - inline-blocks
     *   - auto margin centered absolutes
     *   - debugging everything
     */
    private void createPdfFast(boolean finish) throws IOException {
        boolean success = false;
        
        XRLog.general(Level.INFO, "Using fast-mode renderer. Prepare to fly.");
        
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
        
        PDPage page = new PDPage(new PDRectangle((float) firstPageSize.getWidth(), (float) firstPageSize.getHeight()));
        PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, !_testMode);
        doc.addPage(page);
        
        _outputDevice.initializePage(cs, page, (float) firstPageSize.getHeight());
        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);
        
        int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data
        
        DisplayListCollector dlCollector = new DisplayListCollector(_root.getLayer().getPages());
        DisplayListContainer dlPages = dlCollector.collectRoot(c, _root.getLayer()); 

        int pdfPageIndex = 0;
        
        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = pages.get(i);
            currentPage.setBasePagePdfPageIndex(pdfPageIndex);
            DisplayListPageContainer pageOperations = dlPages.getPageInstructions(i);
            c.setPage(i, currentPage);
            paintPageFast(c, currentPage, pageOperations, 0);
            _outputDevice.finishPage();
            pdfPageIndex++;
            
            if (!pageOperations.shadowPages().isEmpty()) {
                currentPage.setShadowPageCount(pageOperations.shadowPages().size());
                
                int pageContentWidth = currentPage.getContentWidth(c);
                int translateX = pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1);

                for (DisplayListPageContainer shadowPage : pageOperations.shadowPages()) {
                    PDPage shadowPdPage = new PDPage(new PDRectangle((float) currentPage.getWidth(c) / _dotsPerPoint, (float) currentPage.getHeight(c) / _dotsPerPoint));
                    PDPageContentStream shadowCs = new PDPageContentStream(doc, shadowPdPage, AppendMode.APPEND, !_testMode);
                    doc.addPage(shadowPdPage);

                    _outputDevice.initializePage(shadowCs, shadowPdPage, (float) firstPageSize.getHeight());
                    paintPageFast(c, currentPage, shadowPage, -translateX);
                    _outputDevice.finishPage();
                    translateX += (pageContentWidth * (currentPage.getCutOffPageDirection() == IdentValue.LTR ? 1 : -1));
                    pdfPageIndex++;
                }
            }
            
            if (i != pageCount - 1) {
                PageBox nextPage = pages.get(i + 1);
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

        if (_pdfAConformance != PdfAConformance.NONE) {
            addPdfASchema(doc, _pdfAConformance.getPart(), _pdfAConformance.getConformanceValue());
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
                PDPage pageNext = new PDPage(new PDRectangle((float) nextPageSize.getWidth(), (float) nextPageSize.getHeight()));
                PDPageContentStream csNext = new PDPageContentStream(doc, pageNext, AppendMode.APPEND, !_testMode);
                doc.addPage(pageNext);
                _outputDevice.initializePage(csNext, pageNext, (float) nextPageSize.getHeight());
            }
        }

        _outputDevice.finish(c, _root);
    }

    private void addPdfASchema(PDDocument document, int part, String conformance) {
        PDDocumentInformation information = document.getDocumentInformation();
        XMPMetadata metadata = XMPMetadata.createXMPMetadata();

        try {
            PDFAIdentificationSchema pdfaid = metadata.createAndAddPFAIdentificationSchema();
            pdfaid.setConformance(conformance);
            pdfaid.setPart(part);

            AdobePDFSchema pdfSchema = metadata.createAndAddAdobePDFSchema();
            pdfSchema.setProducer(information.getProducer());

            XMPBasicSchema xmpBasicSchema = metadata.createAndAddXMPBasicSchema();
            xmpBasicSchema.setCreateDate(information.getCreationDate());

            PDMetadata metadataStream = new PDMetadata(document);
            PDMarkInfo markInfo = new PDMarkInfo();
            markInfo.setMarked(true);

            // add to catalog
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            catalog.setMetadata(metadataStream);
            // for pdf/a-1 compliance, add the StructTreeRoot that https://www.pdf-online.com/osa/validate.aspx was
            // complaining. Based on https://stackoverflow.com/a/46806392
            catalog.setStructureTreeRoot(new PDStructureTreeRoot());
            //

            catalog.setMarkInfo(markInfo);

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(metadata, baos, true);
            metadataStream.importXMPMetadata( baos.toByteArray() );


            if (_colorProfile != null) {
                ByteArrayInputStream colorProfile = new ByteArrayInputStream(_colorProfile);
                PDOutputIntent oi = new PDOutputIntent(document, colorProfile);
                oi.setInfo("sRGB IEC61966-2.1");
                oi.setOutputCondition("sRGB IEC61966-2.1");
                oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
                oi.setRegistryName("http://www.color.org");
                catalog.addOutputIntent(oi);
            }
        } catch (BadFieldValueException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
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
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
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
     * @deprecated Use close instead.
     */
    @Deprecated
    public void cleanup() {
        _outputDevice.close();
        _sharedContext.removeFromThread();
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
}

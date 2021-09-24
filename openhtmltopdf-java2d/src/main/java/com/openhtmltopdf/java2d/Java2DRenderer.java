package com.openhtmltopdf.java2d;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.List;
import java.util.logging.Level;

import com.openhtmltopdf.java2d.api.Java2DRendererBuilderState;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.OpenUtil;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.java2d.api.FSPage;
import com.openhtmltopdf.java2d.api.FSPageProcessor;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.AddedFont;
import com.openhtmltopdf.outputdevice.helper.BaseDocument;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.NullUserInterface;
import com.openhtmltopdf.outputdevice.helper.PageDimensions;
import com.openhtmltopdf.outputdevice.helper.UnicodeImplementation;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.render.ViewportBox;
import com.openhtmltopdf.render.displaylist.DisplayListCollector;
import com.openhtmltopdf.render.displaylist.DisplayListContainer;
import com.openhtmltopdf.render.displaylist.DisplayListPainter;
import com.openhtmltopdf.render.displaylist.DisplayListContainer.DisplayListPageContainer;
import com.openhtmltopdf.render.simplepainter.SimplePainter;
import com.openhtmltopdf.resource.XMLResource;
import com.openhtmltopdf.simple.extend.XhtmlNamespaceHandler;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.XRLog;

public class Java2DRenderer implements Closeable {
    private final List<FSDOMMutator> _domMutators;
    private final SVGDrawer _mathMLImpl;
	private BlockBox _root;
	
	private final SharedContext _sharedContext;
	private final Java2DOutputDevice _outputDevice;
	
    private BidiSplitterFactory _splitterFactory;
    private byte _defaultTextDirection = BidiSplitter.LTR;
    private BidiReorderer _reorderer;
    
    private final SVGDrawer _svgImpl;
    private Document _doc;
    private final FSObjectDrawerFactory _objectDrawerFactory;
	private final FSPageProcessor _pageProcessor;
    
    private static final int DEFAULT_DOTS_PER_PIXEL = 1;
    private static final int DEFAULT_DPI = 72;
    
    private final int _initialPageNo;
    private final short _pagingMode;

    private final Closeable diagnosticConsumer;


    /**
	 * Subject to change. Not public API. Used exclusively by the Java2DRendererBuilder class. 
	 */
	public Java2DRenderer(BaseDocument doc, UnicodeImplementation unicode, PageDimensions pageSize,
			Java2DRendererBuilderState state, Closeable diagnosticConsumer) {

	    this.diagnosticConsumer = diagnosticConsumer;
	    _pagingMode = state._pagingMode;
		_pageProcessor = state._pageProcessor;
		_initialPageNo = state._initialPageNumber;		
		this._svgImpl = state._svgImpl;
        this._mathMLImpl = state._mathmlImpl;
        this._domMutators = state._domMutators;
        _objectDrawerFactory = state._objectDrawerFactory;
		_outputDevice = new Java2DOutputDevice(state._layoutGraphics);
		
		NaiveUserAgent uac = new Java2DUserAgent();
		
		uac.setProtocolsStreamFactory(state._streamFactoryMap);
		
		if (state._resolver != null) {
			uac.setUriResolver(state._resolver);
		}

        uac.setAccessController(ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI, state._beforeAccessController);
        uac.setAccessController(ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI, state._afterAccessController);

        _sharedContext = new SharedContext();
        _sharedContext.registerWithThread();
        
        _sharedContext._preferredTransformerFactoryImplementationClass = state._preferredTransformerFactoryImplementationClass;
        _sharedContext._preferredDocumentBuilderFactoryImplementationClass = state._preferredDocumentBuilderFactoryImplementationClass;
        
        _sharedContext.setUserAgentCallback(uac);
        _sharedContext.setCss(new StyleReference(uac));
//        uac.setSharedContext(_sharedContext);
//        _outputDevice.setSharedContext(_sharedContext);

        Java2DFontResolver fontResolver = new Java2DFontResolver(_sharedContext, state._useEnvironmentFonts);
        _sharedContext.setFontResolver(fontResolver);
        
        /*
         * Register all Fonts
         */
        for (AddedFont font : state._fonts) {
            IdentValue fontStyle = null;

            if (font.style == FontStyle.NORMAL) {
                fontStyle = IdentValue.NORMAL;
            } else if (font.style == FontStyle.ITALIC) {
                fontStyle = IdentValue.ITALIC;
            } else if (font.style == FontStyle.OBLIQUE) {
                fontStyle = IdentValue.OBLIQUE;
            }

            if (font.supplier != null) {
                fontResolver.addInputStreamFont(font.supplier, font.family, font.weight, fontStyle);
            } else {
                fontResolver.addFontFile(font.fontFile, font.family, font.weight, fontStyle);
            }
        }
        
        
		Java2DReplacedElementFactory replacedFactory = new Java2DReplacedElementFactory(this._svgImpl,
				_objectDrawerFactory, this._mathMLImpl);
        _sharedContext.setReplacedElementFactory(replacedFactory);
        
        _sharedContext.setTextRenderer(new Java2DTextRenderer());
        _sharedContext.setDPI(DEFAULT_DPI * DEFAULT_DOTS_PER_PIXEL);
        _sharedContext.setDotsPerPixel(DEFAULT_DOTS_PER_PIXEL);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
        _sharedContext.setDefaultPageSize(pageSize.w, pageSize.h, pageSize.isSizeInches);

        if (state._replacementText != null) {
            _sharedContext.setReplacementText(state._replacementText);
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
            this.setDocumentFromString(doc.html, doc.baseUri);
        }
        else if (doc.document != null) {
            this.setDocument(doc.document, doc.baseUri);
        }
        else if (doc.uri != null) {
            this.setDocument(doc.uri);
        }
        else if (doc.file != null) {
            try {
                this.setDocument(doc.file);
            } catch (IOException e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_PROBLEM_TRYING_TO_READ_INPUT_XHTML_FILE, e);
                throw new RuntimeException("File IO problem", e);
            }
        }
	}
	
    private void setDocumentFromString(String content, String baseUrl) {
        InputSource is = new InputSource(new BufferedReader(new StringReader(content)));
        Document dom = XMLResource.load(is).getDocument();
        setDocument(dom, baseUrl);
    }
    
    private void setDocument(Document doc, String url) {
        setDocument(doc, url, new XhtmlNamespaceHandler());
    }
    
    private void setDocument(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        setDocument(loadDocument(file.toURI().toURL().toExternalForm()), (parent == null ? "" : parent.toURI().toURL().toExternalForm()));
    }
    
    private void setDocument(String uri) {
        setDocument(loadDocument(uri), uri);
    }
    
    private Document loadDocument(String uri) {
        return _sharedContext.getUserAgentCallback().getXMLResource(uri, ExternalResourceType.XML_XHTML).getDocument();
    }
    
    private void setDocument(Document doc, String url, NamespaceHandler nsh) {
        _doc = doc;
        
        /*
         * Apply potential DOM mutations
         */
        for (FSDOMMutator domMutator : _domMutators)
            domMutator.mutateDocument(doc);

        //TODOgetFontResolver().flushFontFaceFonts();

        _sharedContext.setBaseURL(url);
        _sharedContext.setNamespaceHandler(nsh);
        _sharedContext.getCss().setDocumentContext(_sharedContext, _sharedContext.getNamespaceHandler(), doc, new NullUserInterface());
        
        getFontResolver().importFontFaces(_sharedContext.getCss().getFontFaceRules());
        
        if (_svgImpl != null) {
            _svgImpl.importFontFaceRules(_sharedContext.getCss().getFontFaceRules(), _sharedContext);
        }
        
        if (_mathMLImpl != null) {
            _mathMLImpl.importFontFaceRules(_sharedContext.getCss().getFontFaceRules(), _sharedContext);
        }
    }
    
    public Java2DFontResolver getFontResolver() {
        return (Java2DFontResolver) _sharedContext.getFontResolver();
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
        result.setFontContext(new Java2DFontContext(_outputDevice.getGraphics()));

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
        result.setFontContext(new Java2DFontContext(_outputDevice.getGraphics()));
        
        if (_splitterFactory != null)
            result.setBidiSplitterFactory(_splitterFactory);
        
        if (_reorderer != null)
        	result.setBidiReorderer(_reorderer);

        result.setDefaultTextDirection(_defaultTextDirection);

        ((Java2DTextRenderer) _sharedContext.getTextRenderer()).setup(result.getFontContext(), _reorderer != null ? _reorderer : new SimpleBidiReorderer());

        return result;
    }
    
    public void writePages() throws IOException {
        List<PageBox> pages = _root.getLayer().getPages();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(_initialPageNo);
        c.setFastRenderer(true);

        PageBox firstPage = pages.get(0);
        Rectangle2D firstPageSize = new Rectangle2D.Float(0, 0,
                firstPage.getWidth(c) / DEFAULT_DOTS_PER_PIXEL,
                firstPage.getHeight(c) / DEFAULT_DOTS_PER_PIXEL);

        writePageImages(pages, c, firstPageSize);
    }

    public void writePage(int zeroBasedPageNumber) throws IOException {
        List<PageBox> pages = _root.getLayer().getPages();

        if (zeroBasedPageNumber >= pages.size()) {
            throw new IndexOutOfBoundsException();
        }

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(_initialPageNo);
        c.setFastRenderer(true);

        PageBox page = pages.get(zeroBasedPageNumber);

        Rectangle2D pageSize = new Rectangle2D.Float(0, 0,
                page.getWidth(c) / DEFAULT_DOTS_PER_PIXEL,
                page.getHeight(c) / DEFAULT_DOTS_PER_PIXEL);

        _outputDevice.setRoot(_root);

        FSPage pg = _pageProcessor.createPage(zeroBasedPageNumber, (int) pageSize.getWidth(), (int) pageSize.getHeight());

        try {
            _outputDevice.initializePage(pg.getGraphics());
            _root.getLayer().assignPagePaintingPositions(c, _pagingMode);

            c.setPageCount(pages.size());
            c.setPage(zeroBasedPageNumber, page);

            DisplayListCollector boxCollector = new DisplayListCollector(pages);
            DisplayListContainer displayList = boxCollector.collectRoot(c, _root.getLayer());

            paintPage(c, page, displayList.getPageInstructions(zeroBasedPageNumber));
        } finally {
            _pageProcessor.finishPage(pg);
            _outputDevice.finish(c, _root);
        }
    }

    public void writeSinglePage(){
        List<PageBox> pages = _root.getLayer().getPages();
        int rootHeight = _root.getHeight();

        RenderingContext c = newRenderingContext();
        c.setInitialPageNo(_initialPageNo);
        c.setFastRenderer(true);

        PageBox page = pages.get(0);
        Rectangle2D pageSize = new Rectangle2D.Float(0, 0,
                page.getWidth(c) / DEFAULT_DOTS_PER_PIXEL,
                rootHeight / DEFAULT_DOTS_PER_PIXEL);

        _outputDevice.setRoot(_root);

        int top = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
        int bottom = page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM);
        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
        // int right = page.getMarginBorderPadding(c, CalculatedStyle.RIGHT);

        FSPage pg = _pageProcessor.createPage(0, (int) pageSize.getWidth(), rootHeight + top + bottom);

        try {
            _outputDevice.initializePage(pg.getGraphics());
            _root.getLayer().assignPagePaintingPositions(c, _pagingMode);
            page.setPaintingBottom(rootHeight + top + bottom);

            c.setPageCount(pages.size());
            c.setPage(0, page);

            page.paintBackground(c, 0, _pagingMode);
            page.paintMarginAreas(c, 0, _pagingMode);
            page.paintBorder(c, 0, _pagingMode);

            Rectangle printClip = page.getPrintClippingBounds(c);
            Rectangle pageClip = new Rectangle(0, 0, printClip.width, rootHeight);

            _outputDevice.pushTransformLayer(AffineTransform.getTranslateInstance(left, top));
            _outputDevice.pushClip(pageClip);

            SimplePainter painter = new SimplePainter(0, 0);
            painter.paintLayer(c, _root.getLayer());

            _outputDevice.popClip();
            _outputDevice.popTransformLayer();
        } finally {
            _pageProcessor.finishPage(pg);
            _outputDevice.finish(c, _root);
        }
    }

    public int getPageCount() {
        return _root.getLayer().getPages().size();
    }

    private void writePageImages(
            List<PageBox> pages,
            RenderingContext c,
            Rectangle2D firstPageSize) throws IOException {

        _outputDevice.setRoot(_root);

        _root.getLayer().assignPagePaintingPositions(c, _pagingMode);

        int pageCount = _root.getLayer().getPages().size();

        c.setPageCount(pageCount);

        DisplayListCollector boxCollector = new DisplayListCollector(pages);
        DisplayListContainer displayList = boxCollector.collectRoot(c, _root.getLayer());

        for (int i = 0; i < pageCount; i++) {
            PageBox currentPage = pages.get(i);
            c.setPage(i, currentPage);

            Rectangle2D pageSize = (i == 0 ?
                firstPageSize :
                new Rectangle2D.Float(0, 0,
                     currentPage.getWidth(c) / DEFAULT_DOTS_PER_PIXEL,
                     currentPage.getHeight(c) / DEFAULT_DOTS_PER_PIXEL));

            FSPage pg = initPage(pageSize, i);

            try {
                paintPage(c, currentPage, displayList.getPageInstructions(i));
            } catch (Throwable e) {
                _pageProcessor.finishPage(pg);
                throw e;
            }

            _pageProcessor.finishPage(pg);
        }

        _outputDevice.finish(c, _root);
    }

    private FSPage initPage(Rectangle2D pageSize, int idx) {
        FSPage pg = _pageProcessor.createPage(idx, (int) pageSize.getWidth(), (int) pageSize.getHeight());

        try {
            _outputDevice.initializePage(pg.getGraphics());
        } catch (Throwable e) {
            _pageProcessor.finishPage(pg);
            throw e;
        }

        return pg;
    }

    private void paintPage(RenderingContext c, PageBox page, DisplayListPageContainer pageOperations) {
        page.paintBackground(c, 0, _pagingMode);
        page.paintMarginAreas(c, 0, _pagingMode);
        page.paintBorder(c, 0, _pagingMode);

        int top = -page.getPaintingTop() + page.getMarginBorderPadding(c, CalculatedStyle.TOP);
        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        Rectangle content = new Rectangle(0, page.getPaintingTop(), page.getContentWidth(c), page.getContentHeight(c));

        _outputDevice.pushTransformLayer(AffineTransform.getTranslateInstance(left, top));
        _outputDevice.pushClip(content);

        DisplayListPainter painter = new DisplayListPainter();
        painter.paint(c, pageOperations);

        _outputDevice.popClip();
        _outputDevice.popTransformLayer();
    }

    @Override
    public void close() {
        OpenUtil.tryQuietly(_sharedContext::removeFromThread);
        OpenUtil.tryQuietly(ThreadCtx::cleanup);

        OpenUtil.closeQuietly(diagnosticConsumer);
        OpenUtil.closeQuietly(_svgImpl);
        OpenUtil.closeQuietly(_mathMLImpl);
    }
}

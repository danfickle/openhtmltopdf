package com.openhtmltopdf.pdfboxout;

import java.io.File;
import java.io.OutputStream;

import org.w3c.dom.Document;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.extend.HttpStreamFactory;
import com.openhtmltopdf.swing.NaiveUserAgent;

public class PdfRendererBuilder
{
    public static enum TextDirection { RTL, LTR; }

    private boolean _textDirection = false;
    private boolean _testMode = false;
    private boolean _useSubsets = true;
    private HttpStreamFactory _httpStreamFactory = new NaiveUserAgent.DefaultHttpStreamFactory();
    private BidiSplitterFactory _splitter;
    private BidiReorderer _reorderer;
    private String _html;
    private Document _document;
    private String _baseUri;
    private String _uri;
    private File _file;
    private OutputStream _os;
    
    /**
     * Run the XHTML/XML to PDF conversion and output to an output stream set by toStream.
     * @throws Exception
     */
    public void run() throws Exception {
        PdfBoxRenderer renderer = this.buildPdfRenderer();
        renderer.layout();
        renderer.createPDF();
    }
    
    /**
     * Build a PdfBoxRenderer for further customization.
     * @return
     */
    public PdfBoxRenderer buildPdfRenderer() {
        return new PdfBoxRenderer(_textDirection, _testMode, _useSubsets, _httpStreamFactory, _splitter, _reorderer, _html, _document, _baseUri, _uri, _file, _os);
    }
    
    /**
     * The default text direction of the document. LTR by default.
     * @param textDirection
     * @return
     */
    public PdfRendererBuilder defaultTextDirection(TextDirection textDirection) {
        this._textDirection = textDirection == TextDirection.RTL;
        return this;
    }

    /**
     * Whether to use test mode and output the PDF uncompressed. Turned off by default.
     * @param mode
     * @return
     */
    public PdfRendererBuilder testMode(boolean mode) {
        this._testMode = mode;
        return this;
    }
    
    /**
     * Whether to subset fonts, resulting in a smaller PDF. Turned on by default.
     * @param subset
     * @return
     */
    public PdfRendererBuilder subsetFonts(boolean subset) {
        this._useSubsets = subset;
        return this;
    }    
    
    /**
     * Provides an HttpStreamFactory implementation if the user desires to use an external
     * HTTP/HTTPS implementation. Uses URL::openStream by default.
     * @param factory
     * @return
     */
    public PdfRendererBuilder useHttpStreamImplementation(HttpStreamFactory factory) {
        this._httpStreamFactory = factory;
        return this;
    }
    
    /**
     * Provides a text splitter to split text into directional runs. Does nothing by default.
     * @param splitter
     * @return
     */
    public PdfRendererBuilder useBidiSplitter(BidiSplitterFactory splitter) {
        this._splitter = splitter;
        return this;
    }
    
    /**
     * Provides a reorderer to properly reverse RTL text. No-op by default.
     * @param reorderer
     * @return
     */
    public PdfRendererBuilder useBidiReorderer(BidiReorderer reorderer) {
        this._reorderer = reorderer;
        return this;
    }
    
    /**
     * Provides a string containing XHTML/XML to convert to PDF.
     * @param html
     * @param baseUri
     * @return
     */
    public PdfRendererBuilder withHtmlContent(String html, String baseUri) {
        this._html = html;
        this._baseUri = baseUri;
        return this;
    }

    /**
     * Provides a w3c DOM Document acquired from an external source.
     * @param doc
     * @param baseUri
     * @return
     */
    public PdfRendererBuilder withW3cDocument(org.w3c.dom.Document doc, String baseUri) {
        this._document = doc;
        this._baseUri = baseUri;
        return this;
    }

    /**
     * Provides a URI to convert to PDF. The URI MUST point to a strict XHTML/XML document.
     * @param uri
     * @return
     */
    public PdfRendererBuilder withUri(String uri) {
        this._uri = uri;
        return this;
    }
    
    /**
     * Provides a file to convert to PDF. The file MUST contain XHTML/XML in UTF-8 encoding.
     * @param file
     * @return
     */
    public PdfRendererBuilder withFile(File file) {
        this._file = file;
        return this;
    }

    /**
     * An output stream to output the resulting PDF. The caller is required to close the output stream after calling
     * run.
     * @param out
     * @return
     */
    public PdfRendererBuilder toStream(OutputStream out) {
        this._os = out;
        return this;
    }
}

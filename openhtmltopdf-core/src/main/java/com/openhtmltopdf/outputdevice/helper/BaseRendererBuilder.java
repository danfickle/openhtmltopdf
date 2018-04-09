package com.openhtmltopdf.outputdevice.helper;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.Layer;
import org.w3c.dom.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Baseclass for all RendererBuilders (PDF and Java2D), has all common settings
 */
@SuppressWarnings("unchecked")
public abstract class BaseRendererBuilder<TFinalClass extends BaseRendererBuilder, TBaseRendererBuilderState extends BaseRendererBuilder.BaseRendererBuilderState> {
	public static final float PAGE_SIZE_LETTER_WIDTH = 8.5f;
	public static final float PAGE_SIZE_LETTER_HEIGHT = 11.0f;
	public static final PageSizeUnits PAGE_SIZE_LETTER_UNITS = PageSizeUnits.INCHES;

	/**
	 * This class is an internal implementation detail
	 * @internal
	 */
	public abstract static class BaseRendererBuilderState {
		public final List<FSDOMMutator> _domMutators = new ArrayList<FSDOMMutator>();
		public HttpStreamFactory _httpStreamFactory;
		public FSCache _cache;
		public FSUriResolver _resolver;
		public String _html;
		public String _baseUri;
		public Document _document;
		public SVGDrawer _svgImpl;
		public SVGDrawer _mathmlImpl;
		public String _replacementText;
		public FSTextBreaker _lineBreaker;
		public FSTextBreaker _charBreaker;
		public FSTextTransformer _unicodeToUpperTransformer;
		public FSTextTransformer _unicodeToLowerTransformer;
		public FSTextTransformer _unicodeToTitleTransformer;
		public BidiSplitterFactory _splitter;
		public BidiReorderer _reorderer;
		public boolean _textDirection = false;
		public Float _pageWidth;
		public Float _pageHeight;
		public boolean _isPageSizeInches;
		public String _uri;
		public File _file;
		public boolean _testMode = false;
		public int _initialPageNumber;
		public short _pagingMode = Layer.PAGED_MODE_PRINT;
		public FSObjectDrawerFactory _objectDrawerFactory;
		public String _preferredTransformerFactoryImplementationClass = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
		public String _preferredDocumentBuilderFactoryImplementationClass = "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
	}

	protected final TBaseRendererBuilderState state;

	protected BaseRendererBuilder(TBaseRendererBuilderState state) {
		this.state = state;
	}

	/**
	 * Add a DOM mutator to this builder. DOM mutators allow to modify the DOM
	 * before it is rendered. e.g. LaTeXDOMMutator can be used to translate latex
	 * text within a &lt;latex&gt; node to HTMl and MathML.
	 *
	 * @param domMutator
	 *            the DOM Mutator
	 * @return this for method chaining
	 */
	public TFinalClass addDOMMutator(FSDOMMutator domMutator) {
		state._domMutators.add(domMutator);
		return (TFinalClass) this;
	}

	/**
	 * This method should be considered advanced and is not required for most
	 * setups. Set a preferred implementation class for use as
	 * javax.xml.transform.TransformerFactory. Use null to let a default
	 * implementation class be used. The default is
	 * "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl". This
	 * seems to work with most systems but not JBoss Wildfly and related setups. In
	 * this case you can use null to let the container use whatever
	 * TransformerFactory it has available.
	 *
	 * @param transformerFactoryClass
	 * @return this for method chaining
	 */
	public final TFinalClass useTransformerFactoryImplementationClass(String transformerFactoryClass) {
		state._preferredTransformerFactoryImplementationClass = transformerFactoryClass;
		return (TFinalClass) this;
	}

	/**
	 * This method should be considered advanced and is not required for most
	 * setups. Set a preferred implementation class for use as
	 * javax.xml.parsers.DocumentBuilderFactory. Use null to let a default
	 * implementation class be used. The default is
	 * "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl". If the
	 * default does not work you can use null to let the container use whatever
	 * DocumentBuilderFactory it has available.
	 *
	 * @param documentBuilderFactoryClass
	 * @return this for method chaining
	 */
	public final TFinalClass useDocumentBuilderFactoryImplementationClass(String documentBuilderFactoryClass) {
		state._preferredDocumentBuilderFactoryImplementationClass = documentBuilderFactoryClass;
		return (TFinalClass) this;
	}

	/**
	 * The default text direction of the document. LTR by default.
	 *
	 * @param textDirection
	 * @return this for method chaining
	 */
	public final TFinalClass defaultTextDirection(TextDirection textDirection) {
		state._textDirection = textDirection == TextDirection.RTL;
		return (TFinalClass) this;
	}

	/**
	 * Whether to use test mode and output the PDF uncompressed. Turned off by
	 * default.
	 *
	 * @param mode
	 * @return this for method chaining
	 */
	public final TFinalClass testMode(boolean mode) {
		state._testMode = mode;
		return (TFinalClass) this;
	}

	/**
	 * Provides an HttpStreamFactory implementation if the user desires to use an
	 * external HTTP/HTTPS implementation. Uses URL::openStream by default.
	 *
	 * @param factory
	 * @return this for method chaining
	 */
	public final TFinalClass useHttpStreamImplementation(HttpStreamFactory factory) {
		state._httpStreamFactory = factory;
		return (TFinalClass) this;
	}

	/**
	 * Provides a uri resolver to resolve relative uris or private uri schemes.
	 *
	 * @param resolver
	 * @return this for method chaining
	 */
	public final TFinalClass useUriResolver(FSUriResolver resolver) {
		state._resolver = resolver;
		return (TFinalClass) this;
	}

	/**
	 * Provides an external cache which can choose to cache items between runs, such
	 * as fonts or logo images.
	 *
	 * @param cache
	 * @return this for method chaining
	 */
	public final TFinalClass useCache(FSCache cache) {
		state._cache = cache;
		return (TFinalClass) this;
	}

	/**
	 * Provides a text splitter to split text into directional runs. Does nothing by
	 * default.
	 *
	 * @param splitter
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeBidiSplitter(BidiSplitterFactory splitter) {
		state._splitter = splitter;
		return (TFinalClass) this;
	}

	/**
	 * Provides a reorderer to properly reverse RTL text. No-op by default.
	 *
	 * @param reorderer
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeBidiReorderer(BidiReorderer reorderer) {
		state._reorderer = reorderer;
		return (TFinalClass) this;
	}

	/**
	 * Provides a string containing XHTML/XML to convert to PDF.
	 *
	 * @param html
	 * @param baseUri
	 * @return this for method chaining
	 */
	public final TFinalClass withHtmlContent(String html, String baseUri) {
		state._html = html;
		state._baseUri = baseUri;
		return (TFinalClass) this;
	}

	/**
	 * Provides a w3c DOM Document acquired from an external source.
	 *
	 * @param doc
	 * @param baseUri
	 * @return this for method chaining
	 */
	public final TFinalClass withW3cDocument(org.w3c.dom.Document doc, String baseUri) {
		state._document = doc;
		state._baseUri = baseUri;
		return (TFinalClass) this;
	}

	/**
	 * Provides a URI to convert to PDF. The URI MUST point to a strict XHTML/XML
	 * document.
	 *
	 * @param uri
	 * @return this for method chaining
	 */
	public final TFinalClass withUri(String uri) {
		state._uri = uri;
		return (TFinalClass) this;
	}

	/**
	 * Provides a file to convert to PDF. The file MUST contain XHTML/XML in UTF-8
	 * encoding.
	 *
	 * @param file
	 * @return this for method chaining
	 */
	public final TFinalClass withFile(File file) {
		state._file = file;
		return (TFinalClass) this;
	}

	/**
	 * Uses the specified SVG drawer implementation.
	 *
	 * @param svgImpl
	 *            the SVG implementation
	 * @return this for method chaining
	 */
	public final TFinalClass useSVGDrawer(SVGDrawer svgImpl) {
		state._svgImpl = svgImpl;
		return (TFinalClass) this;
	}

	/**
	 * Use the specified MathML implementation.
	 *
	 * @param mathMlImpl
	 *            the MathML implementation
	 * @return this for method chaining
	 */
	public final TFinalClass useMathMLDrawer(SVGDrawer mathMlImpl) {
		state._mathmlImpl = mathMlImpl;
		return (TFinalClass) this;
	}

	/**
	 * The replacement text to use if a character is cannot be renderered by any of
	 * the specified fonts. This is not broken across lines so should be one or zero
	 * characters for best results. Also, make sure it can be rendered by at least
	 * one of your specified fonts! The default is the # character.
	 *
	 * @param replacement
	 *            the default replacement text
	 * @return this for method chaining
	 */
	public final TFinalClass useReplacementText(String replacement) {
		state._replacementText = replacement;
		return (TFinalClass) this;
	}

	/**
	 * Specify the line breaker. By default a Java default BreakIterator line
	 * instance is used with US locale. Additionally, this is wrapped with
	 * UrlAwareLineBreakIterator to also break before the forward slash (/)
	 * character so that long URIs can be broken on to multiple lines.
	 *
	 * You may want to use a BreakIterator with a different locale (wrapped by
	 * UrlAwareLineBreakIterator or not) or a more advanced BreakIterator from icu4j
	 * (see the rtl-support module for an example).
	 *
	 * @param breaker
	 * @return
	 */
	public final TFinalClass useUnicodeLineBreaker(FSTextBreaker breaker) {
		state._lineBreaker = breaker;
		return (TFinalClass) this;
	}

	/**
	 * Specify the character breaker. By default a break iterator character instance
	 * is used with US locale. Currently this is used when
	 * <code>word-wrap: break-word</code> is in effect.
	 *
	 * @param breaker
	 * @return
	 */
	public final TFinalClass useUnicodeCharacterBreaker(FSTextBreaker breaker) {
		state._charBreaker = breaker;
		return (TFinalClass) this;
	}

	/**
	 * Specify a transformer to use to upper case strings. By default
	 * <code>String::toUpperCase(Locale.US)</code> is used.
	 *
	 * @param tr
	 * @return
	 */
	public final TFinalClass useUnicodeToUpperTransformer(FSTextTransformer tr) {
		state._unicodeToUpperTransformer = tr;
		return (TFinalClass) this;
	}

	/**
	 * Specify a transformer to use to lower case strings. By default
	 * <code>String::toLowerCase(Locale.US)</code> is used.
	 *
	 * @param tr
	 * @return
	 */
	public final TFinalClass useUnicodeToLowerTransformer(FSTextTransformer tr) {
		state._unicodeToLowerTransformer = tr;
		return (TFinalClass) this;
	}

	/**
	 * Specify a transformer to title case strings. By default a best effort
	 * implementation (non locale aware) is used.
	 *
	 * @param tr
	 * @return
	 */
	public final TFinalClass useUnicodeToTitleTransformer(FSTextTransformer tr) {
		state._unicodeToTitleTransformer = tr;
		return (TFinalClass) this;
	}

	/**
	 * Specifies the default page size to use if none is specified in CSS.
	 *
	 * @param pageWidth
	 * @param pageHeight
	 * @param units
	 *            either mm or inches.
	 * @see {@link #PAGE_SIZE_LETTER_WIDTH}, {@link #PAGE_SIZE_LETTER_HEIGHT} and
	 *      {@link #PAGE_SIZE_LETTER_UNITS}
	 * @return
	 */
	public final TFinalClass useDefaultPageSize(float pageWidth, float pageHeight, PageSizeUnits units) {
		state._pageWidth = pageWidth;
		state._pageHeight = pageHeight;
		state._isPageSizeInches = (units == PageSizeUnits.INCHES);
		return (TFinalClass) this;
	}

	/**
	 * Set a factory for &lt;object&gt; drawers
	 *
	 * @param objectDrawerFactory
	 *            Object Drawer Factory
	 * @return this for method chaining
	 */
	public final TFinalClass useObjectDrawerFactory(FSObjectDrawerFactory objectDrawerFactory) {
		state._objectDrawerFactory = objectDrawerFactory;
		return (TFinalClass) this;
	}

	public enum TextDirection {
		RTL, LTR
	}

	public enum PageSizeUnits {
		MM, INCHES
	}

	public enum FontStyle {
		NORMAL, ITALIC, OBLIQUE
	}
}

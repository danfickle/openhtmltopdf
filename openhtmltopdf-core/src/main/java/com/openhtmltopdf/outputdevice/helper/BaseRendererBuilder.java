package com.openhtmltopdf.outputdevice.helper;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.swing.NaiveUserAgent;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.ThreadCtx;
import org.w3c.dom.Document;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Baseclass for all RendererBuilders (PDF and Java2D), has all common settings
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class BaseRendererBuilder<TFinalClass extends BaseRendererBuilder, TBaseRendererBuilderState extends BaseRendererBuilder.BaseRendererBuilderState> {
	public static final float PAGE_SIZE_LETTER_WIDTH = 8.5f;
	public static final float PAGE_SIZE_LETTER_HEIGHT = 11.0f;
	public static final PageSizeUnits PAGE_SIZE_LETTER_UNITS = PageSizeUnits.INCHES;

	/**
	 * This class is an internal implementation detail.<br>
	 * This is internal, please don't use directly.
	 */
	public abstract static class BaseRendererBuilderState {
        public final List<AddedFont> _fonts = new ArrayList<>(); 
        public final List<FSDOMMutator> _domMutators = new ArrayList<>();

        public BiPredicate<String, ExternalResourceType> _beforeAccessController = new NaiveUserAgent.DefaultAccessController();
        public BiPredicate<String, ExternalResourceType> _afterAccessController = new NaiveUserAgent.DefaultAccessController();

        public final Map<String, FSStreamFactory> _streamFactoryMap = new HashMap<>();
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
		public Consumer<Diagnostic> _diagnosticConsumer;
    }

	protected final TBaseRendererBuilderState state;

	protected BaseRendererBuilder(TBaseRendererBuilderState state) {
		this.state = state;
		this.useProtocolsStreamImplementation(new NaiveUserAgent.DefaultHttpStreamFactory(), "http", "https");
		this.useProtocolsStreamImplementation(new NaiveUserAgent.DataUriFactory(), "data");
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
	 * @see #useProtocolsStreamImplementation(FSStreamFactory, String[])
	 *
	 * @param factory the factory to use for HTTP/HTTPS
	 * @return this for method chaining
	 */
	public final TFinalClass useHttpStreamImplementation(FSStreamFactory factory) {
		this.useProtocolsStreamImplementation(factory, "http", "https");
		return (TFinalClass) this;
	}
	
	/**
	 * Provides an {@link com.openhtmltopdf.extend.FSStreamFactory}
	 * implementation if the user desires to use an external
	 * stream provider for a particular set of protocols.
	 * Protocols should always be in lower case.
	 * 
	 * NOTE: HttpStreamFactory, despite its historical name, can be used for any protocol
	 * including private made-up protocols.
	 * 
	 * @see #useHttpStreamImplementation(FSStreamFactory)
	 * @see #useProtocolsStreamImplementation(FSStreamFactory, String[])
	 * @param factory the stream factory to use
	 * @param protocols the list of protocols the factory should be used for
	 * @return this for method chaining
	 */
	public final TFinalClass useProtocolsStreamImplementation(FSStreamFactory factory, Set<String> protocols) {
		for (String protocol : protocols) {
			state._streamFactoryMap.put(protocol, factory);
		}
		return (TFinalClass) this;
	}

	/**
	 * Provides an {@link com.openhtmltopdf.extend.FSStreamFactory}
	 * implementation if the user desires to use an external
	 * stream provider for a particular list of protocols.
	 * Protocols should always be in lower case.
	 * 
	 * NOTE: HttpStreamFactory, despite its historical name, can be used for any protocol
	 * including private made-up protocols.
	 * 
	 * @see #useHttpStreamImplementation(FSStreamFactory)
	 * @see #useProtocolsStreamImplementation(FSStreamFactory, Set)
	 * @param factory the stream factory to use
	 * @param protocols the list of protocols the stream factory should be used for
	 * @return this for method chaining
	 */
	public final TFinalClass useProtocolsStreamImplementation(FSStreamFactory factory, String... protocols) {
		for (String protocol : protocols) {
			state._streamFactoryMap.put(protocol, factory);
		}
		return (TFinalClass) this;
	}

	/**
	 * Provides a uri resolver to resolve relative uris or private uri schemes.
	 *
	 * @param resolver the URI resolver used to resolve any kind of private URIs/protocolls
	 * @return this for method chaining
	 */
	public final TFinalClass useUriResolver(FSUriResolver resolver) {
		state._resolver = resolver;
		return (TFinalClass) this;
	}

	/**
	 * Provides a text splitter to split text into directional runs. Does nothing by
	 * default.
	 *
	 * @param splitter the unicode bidi splitter to use.
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeBidiSplitter(BidiSplitterFactory splitter) {
		state._splitter = splitter;
		return (TFinalClass) this;
	}

	/**
	 * Provides a reorderer to properly reverse RTL text. No-op by default.
	 *
	 * @param reorderer the unicode bidi reorderer to use.
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeBidiReorderer(BidiReorderer reorderer) {
		state._reorderer = reorderer;
		return (TFinalClass) this;
	}

	/**
	 * Provides a string containing XHTML/XML to convert to PDF.
	 * Note that baseDocumentUri can be null if there are no relative resources, otherwise
	 * should be a uri to a (possibly fake) document that is used to resolve relative resources.
	 * Examples:
	 *   <code>file:///Users/user/my-dummy-doc.html</code>,
	 *   <code>file:/C:/Users/me/Desktop/dummy.html</code>
	 *
	 * @param html the HTML text to use.
	 * @param baseDocumentUri the base document URI to resolve future relative resources (e.g. images)
	 * @return this for method chaining
	 */
	public final TFinalClass withHtmlContent(String html, String baseDocumentUri) {
		state._html = html;
		state._baseUri = baseDocumentUri;
		return (TFinalClass) this;
	}

	/**
	 * Provides a w3c DOM Document acquired from an external source.
	 *
	 * @param doc the DOM of the HTML document
	 * @param baseUri the base URI, it will be used to resolve future resources (images, etc.
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
	 * @param uri the URI of the HTML source to convert.
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
	 * @param file the file with the HTML source to convert
	 * @return this for method chaining
	 */
	public final TFinalClass withFile(File file) {
		state._file = file;
		return (TFinalClass) this;
	}

	/**
	 * Uses the specified SVG drawer implementation.
	 * 
	 * NOTE: This implementation is used for both inline SVG markup and SVG markup in external
	 * files included via the <code>img</code> tag. Please be very careful if using an insecure 
	 * <code>SVGDrawer</code> that all SVG images are trusted.
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
	 * @param breaker the text breaker to use
	 * @return this for method chaining
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
	 * @param breaker the character breaker to use
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeCharacterBreaker(FSTextBreaker breaker) {
		state._charBreaker = breaker;
		return (TFinalClass) this;
	}

	/**
	 * Specify a transformer to use to upper case strings. By default
	 * <code>String::toUpperCase(Locale.US)</code> is used.
	 *
	 * @param tr the text transformer to use
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeToUpperTransformer(FSTextTransformer tr) {
		state._unicodeToUpperTransformer = tr;
		return (TFinalClass) this;
	}

	/**
	 * Specify a transformer to use to lower case strings. By default
	 * <code>String::toLowerCase(Locale.US)</code> is used.
	 *
	 * @param tr the text transformer to use.
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeToLowerTransformer(FSTextTransformer tr) {
		state._unicodeToLowerTransformer = tr;
		return (TFinalClass) this;
	}

	/**
	 * Specify a transformer to title case strings. By default a best effort
	 * implementation (non locale aware) is used.
	 *
	 * @param tr the text transformer to use
	 * @return this for method chaining
	 */
	public final TFinalClass useUnicodeToTitleTransformer(FSTextTransformer tr) {
		state._unicodeToTitleTransformer = tr;
		return (TFinalClass) this;
	}

	/**
	 * Specifies the default page size to use if none is specified in CSS.
	 *
	 * @param pageWidth the new default width
	 * @param pageHeight  the new default height
	 * @param units
	 *            either mm or inches.
	 * @see #PAGE_SIZE_LETTER_WIDTH
	 * @see  #PAGE_SIZE_LETTER_HEIGHT
	 * @see  #PAGE_SIZE_LETTER_UNITS
	 * @return this for method chaining
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
	
	/**
	 * Use the new (May 2018) fast renderer.
	 * This renderer can be 100s of times faster for very large documents.
	 * Please note that the fast renderer will be the only renderer at some future
	 * release so please at least test your code using the fast mode.
	 * 
	 * Note: As of version 1.0.11 the fast renderer will be the only renderer
	 * and this method is not required.
	 * 
	 * @return this for method chaining
	 */
	public final TFinalClass useFastMode() {
	    return (TFinalClass) this;
	}
	
    /**
     * <p>Allows the user to provide a font file for use by the main
     * document only (not SVGs). See:
     * {@link #useFont(File, String, Integer, FontStyle, boolean, Set)}</p>
     *
     * <p>For gotchas related to font handling please see:
     * <a href="https://github.com/danfickle/openhtmltopdf/wiki/Fonts">Wiki: Fonts</a></p>
     */
    public TFinalClass useFont(
            File fontFile,
            String fontFamily,
            Integer fontWeight,
            FontStyle fontStyle,
            boolean subset) {
        state._fonts.add(new AddedFont(null, fontFile, fontWeight, fontFamily, subset, fontStyle, EnumSet.of(FSFontUseCase.DOCUMENT)));
        return (TFinalClass) this;
    }

    /**
     * <p>Allows the user to provide a font file for use any or all of
     * the use cases listed in {@link FSFontUseCase} such as main
     * document, SVGs, etc.</p>
     *
     * <p>For gotchas related to font handling please see:
     * <a href="https://github.com/danfickle/openhtmltopdf/wiki/Fonts">Wiki: Fonts</a></p>
     * 
     * @param fontFile A file system font file in true-type format. Beware of using resources as
     * they will not be separate files in the final jar.
     * @param fontFamily Font family name. If using a font in Java2D, SVG or MathML this should match
     * <code>Font.createFont(Font.TRUETYPE_FONT, fontFile).getFamily()</code>.
     * @param fontWeight Font boldness, usually 400 for regular fonts and 700 for bold fonts.
     * @param fontStyle Normal, italic or oblique.
     * @param subset For PDF use whether the font is subset, usually true unless the font is 
     * being used by form controls.
     * @param fontUsedFor Which components use the font such as main document, SVG, etc. Example:
     * <code>EnumSet.of(FSFontUseCase.DOCUMENT, FSFontUseCase.SVG)</code>
     * @return this for method chaining
     */
    public TFinalClass useFont(
            File fontFile,
            String fontFamily,
            Integer fontWeight,
            FontStyle fontStyle,
            boolean subset,
            Set<FSFontUseCase> fontUsedFor) {
        state._fonts.add(new AddedFont(null, fontFile, fontWeight, fontFamily, subset, fontStyle, fontUsedFor));
        return (TFinalClass) this;
    }

    /**
     * Simpler overload for
     * {@link #useFont(File, String, Integer, FontStyle, boolean)}
     *
     * @return this for method chaining
     */
    public TFinalClass useFont(File fontFile, String fontFamily) {
        return this.useFont(fontFile, fontFamily, 400, FontStyle.NORMAL, true);
    }

    /**
     * <p>Add a font programmatically. If the font is NOT subset, it will be downloaded
     * when the renderer is run, otherwise, assuming a font-metrics cache has been configured,
     * the font will only be downloaded if required. Therefore, the user could add many fonts,
     * confident that only those that are needed will be downloaded and processed.</p>
     *
     * <p>The InputStream returned by the supplier will be closed by the caller. Fonts
     * should generally be subset (Java2D renderer ignores this argument),
     * except when used in form controls. FSSupplier is a lambda compatible interface.</p>
     *
     * <p>Fonts can also be added using a font-face at-rule in the CSS (not
     * recommended for Java2D usage).</p>
     * 
     * <p><strong>IMPORTANT:</strong> This method will add fonts for use by the main document
     * only. It is not recommended for use with Java2D.
     * To add fonts for use by Java2D, SVG, etc see:
     * {@link #useFont(File, String, Integer, FontStyle, boolean, Set)}</p>
     * 
     * <p>For gotchas related to font handling please see:
     * <a href="https://github.com/danfickle/openhtmltopdf/wiki/Fonts">Wiki: Fonts</a></p>
     *
     * @return this for method chaining
     */
    public TFinalClass useFont(FSSupplier<InputStream> supplier, String fontFamily, Integer fontWeight,
            FontStyle fontStyle, boolean subset) {
        state._fonts.add(new AddedFont(supplier, null, fontWeight, fontFamily, subset, fontStyle, EnumSet.of(FSFontUseCase.DOCUMENT)));
        return (TFinalClass) this;
    }

    /**
     * <p>Add a font programmatically. If the font is NOT subset, it will be downloaded
     * when the renderer is run, otherwise, assuming a font-metrics cache has been configured,
     * the font will only be downloaded if required. Therefore, the user could add many fonts,
     * confident that only those that are needed will be downloaded and processed.</p>
     *
     * <p>The InputStream returned by the supplier will be closed by the caller. Fonts
     * should generally be subset (Java2D renderer ignores this argument),
     * except when used in form controls. FSSupplier is a lambda compatible interface.</p>
     *
     * <p>Fonts can also be added using a font-face at-rule in the CSS (not
     * recommended for Java2D usage).</p>
     * 
     * <p><strong>IMPORTANT:</strong> This method is not recommended for use with Java2D.
     * To add fonts for use by Java2D, SVG, etc see:
     * {@link #useFont(File, String, Integer, FontStyle, boolean, Set)}</p>
     * 
     * <p>For gotchas related to font handling please see:
     * <a href="https://github.com/danfickle/openhtmltopdf/wiki/Fonts">Wiki: Fonts</a></p>
     *
     * @return this for method chaining
     */
    public TFinalClass useFont(
            FSSupplier<InputStream> supplier, String fontFamily, Integer fontWeight,
            FontStyle fontStyle, boolean subset, Set<FSFontUseCase> useFontFlags) {
        state._fonts.add(new AddedFont(supplier, null, fontWeight, fontFamily, subset, fontStyle, useFontFlags));
        return (TFinalClass) this;
    }

    /**
     * Simpler overload for
     * {@link #useFont(FSSupplier, String, Integer, FontStyle, boolean)}
     *
     * @return this for method chaining
     */
    public TFinalClass useFont(FSSupplier<InputStream> supplier, String fontFamily) {
        return this.useFont(supplier, fontFamily, 400, FontStyle.NORMAL, true);
    }

	public TFinalClass withDiagnosticConsumer(Consumer<Diagnostic> diagnosticConsumer) {
		state._diagnosticConsumer = diagnosticConsumer;
		return (TFinalClass) this;
	}

	protected Closeable applyDiagnosticConsumer() {
		return ThreadCtx.applyDiagnosticConsumer(state._diagnosticConsumer);
	}

    /**
     * Allows to set <strong>one</strong> external access controller to run
     * before the uri resolver and one to run after the uri resolver.
     *
     * The predicate will receive the uri and the resource type. If it returns
     * false, the resource will not be loaded but the rendering process will
     * attempt to continue without the resource.
     *
     * A default controller is registered that allows everything except file
     * embed resources. To override the default controller, register a controller
     * with after priority.
     */
    public TFinalClass useExternalResourceAccessControl(
            BiPredicate<String, ExternalResourceType> allowExternalResource,
            ExternalResourceControlPriority priority) {
        if (priority == null) {
            // Default is after as safest.
            priority = ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI;
        }

        switch (priority) {
        case RUN_AFTER_RESOLVING_URI:
            state._afterAccessController = allowExternalResource;
            break;
        case RUN_BEFORE_RESOLVING_URI:
            state._beforeAccessController = allowExternalResource;
            break;
        }

        return (TFinalClass) this;
    }

    /**
     * Allows the setting of the initial page number to use with the 
     * <code>page</code> and <code>pages</code> CSS counters.
     * Useful when appending to an existing document.
     * Must be one or greater.
     *
     * @return this for method chaining.
     */
    public TFinalClass useInitialPageNumber(int initialPageNumber) {
        state._initialPageNumber = initialPageNumber;
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

    /**
     * Use cases for fonts.
     */
    public enum FSFontUseCase {
        /** Main document (PDF or Java2D) */
        DOCUMENT,
        SVG,
        MATHML,
        /**
         * Use as a fallback font after all supplied fonts have been tried but before
         * the built-in fonts have been attempted.
         */
        FALLBACK_PRE,
        /**
         * Use as a fallback fonts after all supplied fonts and the built-in fonts have been
         * tried. The same font should not be registered with both <code>FALLBACK_PRE</code>
         * and <code>FALLBACK_FINAL</code>.
         */
        FALLBACK_FINAL;
    }
}

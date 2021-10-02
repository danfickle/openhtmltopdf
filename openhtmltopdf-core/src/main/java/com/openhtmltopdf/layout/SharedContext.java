/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package com.openhtmltopdf.layout;

import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.EmptyStyle;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.*;
import com.openhtmltopdf.layout.counter.RootCounterContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.ThreadCtx;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The SharedContext stores pseudo global variables.
 * Originally, it was reusable, but it is now recommended that the developer
 * use a new instance for every run.
 */
public class SharedContext {

    /**
     * @deprecated Belongs in Java2D renderer.
     */
    @Deprecated
    private final static int DEFAULT_DOTS_PER_PIXEL = 1;

    /**
     * @deprecated Belongs in Java2D renderer.
     */
    @Deprecated
    private final static boolean DEFAULT_INTERACTIVE = true;

    private final static int MM__PER__CM = 10;
    private final static float CM__PER__IN = 2.54F;
    
	private TextRenderer textRenderer;
    private String media;
    private UserAgentCallback uac;
    private boolean interactive = DEFAULT_INTERACTIVE;

    private Map<String, Box> idMap;

    /**
     * Used to adjust fonts, ems, points, into screen resolution.
     * Internal program dots per inch.
     */
    private float dpi;

    /**
     * Internal program dots per pixel.
     */
    private int dotsPerPixel = DEFAULT_DOTS_PER_PIXEL;
    
    /**
     * dpi in a more usable way
     * Internal program dots per mm (probably a fraction).
     */
    private float mmPerDot;

    private boolean print;
    private final Map<Element, CalculatedStyle> styleMap = new HashMap<>(1024, 0.75f);
    private ReplacedElementFactory replacedElementFactory;
    private Rectangle tempCanvas;
    
    protected FontResolver fontResolver;
    protected StyleReference css;
    
    protected boolean debug_draw_boxes;
    protected boolean debug_draw_line_boxes;
    protected boolean debug_draw_inline_boxes;
    protected boolean debug_draw_font_metrics;

    protected FSCanvas canvas;

    private NamespaceHandler namespaceHandler;
    
	private Float defaultPageHeight;
	private Float defaultPageWidth;
	private boolean defaultPageSizeIsInches;

	private String replacementText = "#";
	private FSTextBreaker lineBreaker = new UrlAwareLineBreakIterator(BreakIterator.getLineInstance(Locale.US));
	private FSTextBreaker characterBreaker = new TextUtil.DefaultCharacterBreaker(BreakIterator.getCharacterInstance(Locale.US));

	private FSTextTransformer _unicodeToLowerTransformer = new TextUtil.DefaultToLowerTransformer(Locale.US);
	private FSTextTransformer _unicodeToUpperTransformer = new TextUtil.DefaultToUpperTransformer(Locale.US);
	private FSTextTransformer _unicodeToTitleTransformer = new TextUtil.DefaultToTitleTransformer();

	public String _preferredTransformerFactoryImplementationClass = null;
	public String _preferredDocumentBuilderFactoryImplementationClass = null;

    private final RootCounterContext _rootCounterContext = new RootCounterContext();

    public SharedContext() {
    }

    public LayoutContext newLayoutContextInstance() {
        LayoutContext c = new LayoutContext(this);
        return c;
    }

    public RenderingContext newRenderingContextInstance() {
        RenderingContext c = new RenderingContext(this);
        return c;
    }

    /* =========== Font stuff ============== */

    /**
     * Gets the fontResolver attribute of the Context object
     *
     * @return The fontResolver value
     */
    public FontResolver getFontResolver() {
        return fontResolver;
    }

    /**
     * The media for this context
     */
    public String getMedia() {
        return media;
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public boolean debugDrawBoxes() {
        return debug_draw_boxes;
    }

    public boolean debugDrawLineBoxes() {
        return debug_draw_line_boxes;
    }

    public boolean debugDrawInlineBoxes() {
        return debug_draw_inline_boxes;
    }

    public boolean debugDrawFontMetrics() {
        return debug_draw_font_metrics;
    }

    public void setDebug_draw_boxes(boolean debug_draw_boxes) {
        this.debug_draw_boxes = debug_draw_boxes;
    }

    public void setDebug_draw_line_boxes(boolean debug_draw_line_boxes) {
        this.debug_draw_line_boxes = debug_draw_line_boxes;
    }

    public void setDebug_draw_inline_boxes(boolean debug_draw_inline_boxes) {
        this.debug_draw_inline_boxes = debug_draw_inline_boxes;
    }

    public void setDebug_draw_font_metrics(boolean debug_draw_font_metrics) {
        this.debug_draw_font_metrics = debug_draw_font_metrics;
    }

    public StyleReference getCss() {
        return css;
    }

    public void setCss(StyleReference css) {
        this.css = css;
    }

    public FSCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(FSCanvas canvas) {
        this.canvas = canvas;
    }

    public void setTempCanvas(Rectangle rect) {
        this.tempCanvas = rect;
    }

    public Rectangle getFixedRectangle() {
        //Uu.p("this = " + canvas);
        if (getCanvas() == null) {
            return this.tempCanvas;
        } else {
            Rectangle rect = getCanvas().getFixedRectangle();
            rect.translate(getCanvas().getX(), getCanvas().getY());
            return rect;
        }
    }

    public void setNamespaceHandler(NamespaceHandler nh) {
        namespaceHandler = nh;
    }

    public NamespaceHandler getNamespaceHandler() {
        return namespaceHandler;
    }

    public void addBoxId(String id, Box box) {
        if (idMap == null) {
            idMap = new HashMap<>();
        }
        idMap.put(id, box);
    }

    public Box getBoxById(String id) {
        if (idMap == null) {
            idMap = new HashMap<>();
        }
        return idMap.get(id);
    }

    public void removeBoxId(String id) {
        if (idMap != null) {
            idMap.remove(id);
        }
    }

    public Map<String, Box> getIdMap() {
        return idMap;
    }

    /**
     * Sets the textRenderer attribute of the RenderingContext object
     *
     * @param textRenderer The new textRenderer value
     */
    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    /**
     * Set the current media type. This is usually something like <i>screen</i>
     * or <i>print</i> . See the <a href="http://www.w3.org/TR/CSS21/media.html">
     * media section</a> of the CSS 2.1 spec for more information on media
     * types.
     *
     * @param media The new media value
     */
    public void setMedia(String media) {
        this.media = media;
    }

    /**
     * Gets the uac attribute of the RenderingContext object
     *
     * @return The uac value (user agent).
     * @deprecated Use getUserAgentCallback instead for clearer code.
     */
    @Deprecated
    public UserAgentCallback getUac() {
        return uac;
    }

    public UserAgentCallback getUserAgentCallback() {
        return uac;
    }

    public void setUserAgentCallback(UserAgentCallback userAgentCallback) {
        StyleReference styleReference = getCss();
        if (styleReference != null) {
            styleReference.setUserAgentCallback(userAgentCallback);
        }
        uac = userAgentCallback;
    }

    /**
     * Gets the dPI attribute of the RenderingContext object
     *
     * @return The dPI value
     */
    public float getDPI() {
        return this.dpi;
    }

    /**
     * Sets the effective DPI (Dots Per Inch) of the screen. You should normally
     * never need to override the dpi, as it is already set to the system
     * default by <code>Toolkit.getDefaultToolkit().getScreenResolution()</code>
     * You can override the value if you want to scale EVERYTHING.
     *
     * @param dpi The new dPI value
     */
    public void setDPI(float dpi) {
        this.dpi = dpi;
        this.mmPerDot = (CM__PER__IN * MM__PER__CM) / dpi;
    }

    /**
     * Gets the dPI attribute in a more useful form of the RenderingContext object
     *
     * @return The dPI value
     */
    public float getMmPerDotParent() {
        return this.mmPerDot;
    }

    public FSFont getFont(FontSpecification spec) {
        return getFontResolver().resolveFont(this, spec);
    }

    //strike-through offset should always be half of the height of lowercase x...
    //and it is defined even for fonts without 'x'!
    public float getXHeight(FontContext fontContext, FontSpecification fs) {
        FSFont font = getFontResolver().resolveFont(this, fs);
        FSFontMetrics fm = getTextRenderer().getFSFontMetrics(fontContext, font, " ");
        float sto = fm.getStrikethroughOffset();
        return fm.getAscent() - 2 * Math.abs(sto) + fm.getStrikethroughThickness();
    }

    /**
     * Gets the baseURL attribute of the RenderingContext object
     *
     * @return The baseURL value
     */
    public String getBaseURL() {
        return uac.getBaseURL();
    }

    /**
     * Sets the baseURL attribute of the RenderingContext object
     *
     * @param url The new baseURL value
     */
    public void setBaseURL(String url) {
        uac.setBaseURL(url);
    }

    /**
     * Returns true if the currently set media type is paged. Currently returns
     * true only for <i>print</i> , <i>projection</i> , and <i>embossed</i> ,
     * <i>handheld</i> , and <i>tv</i> . See the <a
     * href="http://www.w3.org/TR/CSS21/media.html">media section</a> of the CSS
     * 2.1 spec for more information on media types.
     *
     * @return The paged value
     */
    public boolean isPaged() {
        if (media.equals("print")) {
            return true;
        }
        if (media.equals("projection")) {
            return true;
        }
        if (media.equals("embossed")) {
            return true;
        }
        if (media.equals("handheld")) {
            return true;
        }
        if (media.equals("tv")) {
            return true;
        }
        return false;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public boolean isPrint() {
        return print;
    }

    public void setPrint(boolean print) {
        this.print = print;
        if (print) {
            setMedia("print");
        } else {
            setMedia("screen");
        }
    }

    public void setFontResolver(FontResolver resolver) {
        fontResolver = resolver;
    }

    /**
     * Get the internal dots measurement per CSS pixel.
     */
    public int getDotsPerPixel() {
        return dotsPerPixel;
    }

    /**
     * Set the internal dots measurement per CSS pixel.
     * @param dotsPerPixel
     */
    public void setDotsPerPixel(int dotsPerPixel) {
        this.dotsPerPixel = dotsPerPixel;
    }

    /**
     * Gets the resolved style for an element. All primitive properties will
     * have values.
     * <br><br>
     * This method uses a cache.
     * <br><br>
     * If the parent element's style is not cached this method will recursively
     * work up the ancestor list until it styles the document with the initial values
     * of CSS properties.
     */
    public CalculatedStyle getStyle(Element e) {
        CalculatedStyle result = styleMap.get(e);

        if (result == null) {
            Node parent = e.getParentNode();
            CalculatedStyle parentCalculatedStyle = parent instanceof Document ?
                    new EmptyStyle() : getStyle((Element) parent);

            result = parentCalculatedStyle.deriveStyle(getCss().getCascadedStyle(e, false));

            styleMap.put(e, result);
        }

        return result;
    }

    public ReplacedElementFactory getReplacedElementFactory() {
        return replacedElementFactory;
    }

    public void setReplacedElementFactory(ReplacedElementFactory ref) {
        if (ref == null) {
            throw new NullPointerException("replacedElementFactory may not be null");
        }
        this.replacedElementFactory = ref;
    }

    /**
     * Stores a default page width.
     * @return default page width or null.
     * @see #isDefaultPageSizeInches()
     */
	public Float getDefaultPageWidth() {
		return this.defaultPageWidth;
	}

    /**
     * Stores a default page height.
     * @return default page height or null.
     * @see #isDefaultPageSizeInches()
     */
	public Float getDefaultPageHeight() {
		return this.defaultPageHeight;
	}
	
	/**
	 * If not, consider it as mm.
	 * @return true if the page size is in inches, false if it is in mm.
	 */
	public boolean isDefaultPageSizeInches() {
		return this.defaultPageSizeIsInches;
	}
	
	/**
	 * The replacement text to be used if a character cannot be 
	 * renderered by the current or fallback fonts.
	 * @return the current replacement text, "#" by default
	 */
	public String getReplacementText() {
		return this.replacementText;
	}
	
	public void setReplacementText(String replacement) {
		this.replacementText = replacement;
	}
	
	/**
	 * Set the default page dimensions. These may be overridden in CSS.
	 * If not set in CSS and null here, A4 will be used.
	 * @param pageWidth
	 * @param pageHeight
	 */
	public void setDefaultPageSize(Float pageWidth, Float pageHeight, boolean isInches) {
		this.defaultPageWidth = pageWidth;
		this.defaultPageHeight = pageHeight;
		this.defaultPageSizeIsInches = isInches;
	}
	
	public FSTextBreaker getLineBreaker() {
		return lineBreaker;
	}
	
	public void setLineBreaker(FSTextBreaker breaker) {
		this.lineBreaker = breaker;
	}

	public FSTextBreaker getCharacterBreaker() {
		return characterBreaker;
	}
	
	public void setCharacterBreaker(FSTextBreaker breaker) {
		this.characterBreaker = breaker;
	}
	
	
	/**
	 * This registers the shared context with a thread local so it
	 * can be used anywhere. It should be matched with a call to 
	 * {@link #removeFromThread()} when the run is complete.
	 */
	public void registerWithThread() {
		ThreadCtx.get().setSharedContext(this);
	}
	
	/**
	 * This removes the shared context from a thread local to avoid memory leaks.
	 */
	public void removeFromThread() {
		ThreadCtx.get().setSharedContext(null);
	}

	public FSTextTransformer getUnicodeToLowerTransformer() {
		return this._unicodeToLowerTransformer;
	}

	public FSTextTransformer getUnicodeToUpperTransformer() {
		return this._unicodeToUpperTransformer;
	}

	public FSTextTransformer getUnicodeToTitleTransformer() {
		return this._unicodeToTitleTransformer;
	}
	
	public void setUnicodeToLowerTransformer(FSTextTransformer tr) {
		this._unicodeToLowerTransformer = tr;
	}
	
	public void setUnicodeToUpperTransformer(FSTextTransformer tr) {
		this._unicodeToUpperTransformer = tr;
	}
	
	public void setUnicodeToTitleTransformer(FSTextTransformer tr) {
		this._unicodeToTitleTransformer = tr;
	}

    public RootCounterContext getGlobalCounterContext() {
        return _rootCounterContext;
    }
}

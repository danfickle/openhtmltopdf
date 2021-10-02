/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
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

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.w3c.dom.Element;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.ParagraphSplitter;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.bidi.SimpleBidiSplitterFactory;
import com.openhtmltopdf.context.ContentFunctionFactory;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSCanvas;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.ReplacedElementFactory;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.counter.AbstractCounterContext;
import com.openhtmltopdf.layout.counter.CounterContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.InlineBox;
import com.openhtmltopdf.render.MarkerData;
import com.openhtmltopdf.render.PageBox;

/**
 * This class tracks state which changes over the course of a layout run.
 * Generally speaking, if possible, state information should be stored in the box
 * tree and not here.  It also provides pass-though calls to many methods in
 * {@link SharedContext}.
 */
public class LayoutContext implements CssContext {
    public enum BlockBoxingState {
        NOT_SET,
        ALLOW,
        DENY;
    }

    private BlockBoxingState _blockBoxingState = BlockBoxingState.NOT_SET;

    private SharedContext _sharedContext;

    private Layer _rootLayer;

    private StyleTracker _firstLines;
    private StyleTracker _firstLetters;
    private MarkerData _currentMarkerData;

    private LinkedList<BlockFormattingContext> _bfcs;
    private LinkedList<Layer> _layers;

    private FontContext _fontContext;

    private final ContentFunctionFactory _contentFunctionFactory = new ContentFunctionFactory();

    private int _extraSpaceTop;
    private int _extraSpaceBottom;

    public final Map<CalculatedStyle, CounterContext> _counterContextMap = new HashMap<>();

    private String _pendingPageName;
    private String _pageName;

    private int _noPageBreak = 0;

    private Layer _rootDocumentLayer;
    private PageBox _page;

    private boolean _mayCheckKeepTogether = true;

    private boolean _lineBreakedBecauseOfNoWrap = false;

    private BreakAtLineContext _breakAtLineContext;

    private Boolean isPrintOverride = null; // True, false, or null for no override.

    private boolean _isInFloatBottom;

    private LayoutState _savedLayoutState;

    private int _footnoteIndex;
    private FootnoteManager _footnoteManager;
    private boolean _isFootnoteAllowed = true;

    private final Map<String, Object> _boxMap = new HashMap<>();

    public void addLayoutBoxId(Element elem, Object boxOrInlineBox) {
        String id = elem.getAttribute("id");
        if (!id.isEmpty()) {
            _boxMap.put(id, boxOrInlineBox);
        }
    }

    /**
     * Returns null, an {@link InlineBox} or {@link BlockBox} with the given id.
     * NOTE: This is for use pre-layout. Once layout has occurred and InlineBox
     * objects have been transformed into one or more InlineLayoutBox objects, one
     * can use _sharedContext::getBoxById.
     */
    public Object getLayoutBox(String id) {
        return _boxMap.get(id);
    }

    @Override
    public TextRenderer getTextRenderer() {
        return _sharedContext.getTextRenderer();
    }

    public StyleReference getCss() {
        return _sharedContext.getCss();
    }

    public FSCanvas getCanvas() {
        return _sharedContext.getCanvas();
    }

    public Rectangle getFixedRectangle() {
        return _sharedContext.getFixedRectangle();
    }

    public NamespaceHandler getNamespaceHandler() {
        return _sharedContext.getNamespaceHandler();
    }
    
    private final ParagraphSplitter _splitter = new ParagraphSplitter();
    private BidiSplitterFactory _bidiSplitterFactory = new SimpleBidiSplitterFactory();
    private byte _defaultTextDirection = BidiSplitter.LTR;
    
    /**
     * The paragraph splitter splits the document into paragraphs for the purpose of bi-directional
     * text analysis.
     */
    public ParagraphSplitter getParagraphSplitter() {
    	return _splitter;
    }
    
    private BidiReorderer _bidiReorderer = new SimpleBidiReorderer();

    public void setBidiReorderer(BidiReorderer reorderer) {
    	_bidiReorderer = reorderer;
    }
    
    public BidiReorderer getBidiReorderer() {
    	return _bidiReorderer;
    }
    
    /**
     * The bidi splitter is used to split text runs into LTR and RTL visual ordering.
     */
    public BidiSplitterFactory getBidiSplitterFactory() {
    	return this._bidiSplitterFactory;
    }
    
    /**
     * The bidi splitter is used to split text runs into LTR and RTL visual ordering.
     */
    public void setBidiSplitterFactory(BidiSplitterFactory factory) {
    	this._bidiSplitterFactory = factory;
    }
    
    /**
     * @return the default text direction for a document.
     */
    public byte getDefaultTextDirection() {
    	return _defaultTextDirection;
    }
    
    /**
     * @param direction either BidiSplitter.LTR or BidiSplitter.RTL.
     */
    public void setDefaultTextDirection(byte direction) {
    	this._defaultTextDirection = direction;
    }
    
    //the stuff that needs to have a separate instance for each run.
    LayoutContext(SharedContext sharedContext) {
        _sharedContext = sharedContext;
        
        _bfcs = new LinkedList<>();
        _layers = new LinkedList<>();

        _firstLines = StyleTracker.withNoStyles();
        _firstLetters = StyleTracker.withNoStyles();
    }

    public void reInit(boolean keepLayers) {
        _firstLines = StyleTracker.withNoStyles();
        _firstLetters = StyleTracker.withNoStyles();
        _currentMarkerData = null;

        _bfcs = new LinkedList<>();

        if (! keepLayers) {
            _rootLayer = null;
            _layers = new LinkedList<>();
        }

        _extraSpaceTop = 0;
        _extraSpaceBottom = 0;
    }

    public LayoutState captureLayoutState() {
        if (!isPrint()) {
            return new LayoutState(
                _bfcs,
                _currentMarkerData,
                _firstLetters,
                _firstLines);
        } else {
            return new LayoutState(
                    _bfcs,
                    _currentMarkerData,
                    _firstLetters,
                    _firstLines,
                    getPageName(),
                    getExtraSpaceTop(),
                    getExtraSpaceBottom(),
                    getNoPageBreak());
        }
    }

    public void restoreLayoutState(LayoutState layoutState) {
        _firstLines = layoutState.getFirstLines();
        _firstLetters = layoutState.getFirstLetters();

        _currentMarkerData = layoutState.getCurrentMarkerData();

        _bfcs = layoutState.getBFCs();

        if (isPrint()) {
            setPageName(layoutState.getPageName());
            setExtraSpaceBottom(layoutState.getExtraSpaceBottom());
            setExtraSpaceTop(layoutState.getExtraSpaceTop());
            setNoPageBreak(layoutState.getNoPageBreak());
        }
    }

    public LayoutState copyStateForRelayout() {
        if (_savedLayoutState != null &&
            _savedLayoutState.equal(
                    _currentMarkerData,
                    _firstLetters,
                    _firstLines,
                    isPrint() ? getPageName() : null)) {

            return _savedLayoutState;
        }

        _savedLayoutState =
            new LayoutState(
                _firstLetters,
                _firstLines,
                _currentMarkerData,
                isPrint() ? getPageName() : null);

        return _savedLayoutState;
    }

    public void restoreStateForRelayout(LayoutState layoutState) {
        _firstLines = layoutState.getFirstLines();
        _firstLetters = layoutState.getFirstLetters();

        _currentMarkerData = layoutState.getCurrentMarkerData();

        if (isPrint()) {
            setPageName(layoutState.getPageName());
        }
    }

    public BlockFormattingContext getBlockFormattingContext() {
        return _bfcs.getLast();
    }

    public void pushBFC(BlockFormattingContext bfc) {
        _bfcs.add(bfc);
    }

    public void popBFC() {
        _bfcs.removeLast();
    }

    public void pushLayerIsolated(BlockBox master) {
        pushLayer(new Layer(master, this, true));
    }

    public void pushLayer(BlockBox master) {
        Layer layer = null;

        if (_rootLayer == null) {
            layer = new Layer(master, this);
            _rootLayer = layer;
        } else {
            Layer parent = getLayer();

            layer = new Layer(parent, master, this);

            parent.addChild(layer);
        }

        pushLayer(layer);
    }

    public void pushLayer(Layer layer) {
        _layers.add(layer);
    }

    public void popLayer() {
        Layer layer = getLayer();

        layer.finish(this);

        _layers.removeLast();
    }

    public Layer getLayer() {
        return _layers.getLast();
    }

    public Layer getRootLayer() {
        return _rootLayer;
    }

    public void translate(int x, int y) {
        getBlockFormattingContext().translate(x, y);
    }

    /* code to keep track of all of the id'd boxes */
    public void addBoxId(String id, Box box) {
        _sharedContext.addBoxId(id, box);
    }

    public void removeBoxId(String id) {
        _sharedContext.removeBoxId(id);
    }

    public boolean isInteractive() {
        return _sharedContext.isInteractive();
    }

    public float getMmPerDot() {
        return _sharedContext.getMmPerDotParent();
    }

    public int getDotsPerPixel() {
        return _sharedContext.getDotsPerPixel();
    }

    public float getFontSize2D(FontSpecification font) {
        return _sharedContext.getFont(font).getSize2D();
    }

    public float getXHeight(FontSpecification parentFont) {
        return _sharedContext.getXHeight(getFontContext(), parentFont);
    }

    public FSFont getFont(FontSpecification font) {
        return _sharedContext.getFont(font);
    }

    public UserAgentCallback getUac() {
        return _sharedContext.getUserAgentCallback();
    }

    public boolean isPrint() {
    	if (this.isPrintOverride != null) {
    		return this.isPrintOverride;
    	}
    	
        return _sharedContext.isPrint();
    }
    
    /**
     * @param isPrint true, false or null for no override.
     */
    public void setIsPrintOverride(Boolean isPrint) {
    	this.isPrintOverride = isPrint;
    }

    public StyleTracker getFirstLinesTracker() {
        return _firstLines;
    }

    public StyleTracker getFirstLettersTracker() {
        return _firstLetters;
    }

    public MarkerData getCurrentMarkerData() {
        return _currentMarkerData;
    }

    public void setCurrentMarkerData(MarkerData currentMarkerData) {
        _currentMarkerData = currentMarkerData;
    }

    public ReplacedElementFactory getReplacedElementFactory() {
        return _sharedContext.getReplacedElementFactory();
    }

    @Override
    public FontContext getFontContext() {
        return _fontContext;
    }

    public void setFontContext(FontContext fontContext) {
        _fontContext = fontContext;
    }

    public ContentFunctionFactory getContentFunctionFactory() {
        return _contentFunctionFactory;
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    /**
     * Returns the extra space set aside for the footers of paginated tables.
     */
    public int getExtraSpaceBottom() {
        return _extraSpaceBottom;
    }

    /**
     * See {@link #getExtraSpaceBottom()}
     */
    public void setExtraSpaceBottom(int extraSpaceBottom) {
        _extraSpaceBottom = extraSpaceBottom;
    }

    /**
     * Returns the extra space set aside for the head section of paginated tables.
     */
    public int getExtraSpaceTop() {
        return _extraSpaceTop;
    }

    /**
     * See {@link #getExtraSpaceTop()}
     */
    public void setExtraSpaceTop(int extraSpaceTop) {
        _extraSpaceTop = extraSpaceTop;
    }

    public void resolveCounters(CalculatedStyle style, Integer startIndex) {
        //new context for child elements
        CounterContext cc = new CounterContext(this, style, startIndex);
        _counterContextMap.put(style, cc);
    }

    public void resolveCounters(CalculatedStyle style) {
    	resolveCounters(style, null);
    }

    public AbstractCounterContext getCounterContext(CalculatedStyle style) {
        return _counterContextMap.get(style);
    }

    public FSFontMetrics getFSFontMetrics(FSFont font) {
        return getTextRenderer().getFSFontMetrics(getFontContext(), font, "");
    }

    public String getPageName() {
        return _pageName;
    }

    public void setPageName(String currentPageName) {
        _pageName = currentPageName;
    }

    public int getNoPageBreak() {
        return _noPageBreak;
    }

    public void setNoPageBreak(int noPageBreak) {
        _noPageBreak = noPageBreak;
    }

    public boolean isPageBreaksAllowed() {
        return _noPageBreak == 0;
    }

    public String getPendingPageName() {
        return _pendingPageName;
    }

    public void setPendingPageName(String pendingPageName) {
        _pendingPageName = pendingPageName;
    }

    public Layer getRootDocumentLayer() {
        return _rootDocumentLayer;
    }

    public void setRootDocumentLayer(Layer rootDocumentLayer) {
        _rootDocumentLayer = rootDocumentLayer;
    }

    public PageBox getPage() {
        return _page;
    }

    public void setPage(PageBox page) {
        _page = page;
    }

    public boolean isMayCheckKeepTogether() {
        return _mayCheckKeepTogether;
    }

    public void setMayCheckKeepTogether(boolean mayKeepTogether) {
        _mayCheckKeepTogether = mayKeepTogether;
    }

    public void setBlockBoxingState(BlockBoxingState state) {
        _blockBoxingState = state;
    }

    public BlockBoxingState getBlockBoxingState() {
        return _blockBoxingState;
    }

    public boolean isLineBreakedBecauseOfNoWrap() {
        return _lineBreakedBecauseOfNoWrap;
    }

    public void setLineBreakedBecauseOfNoWrap(boolean value) {
        _lineBreakedBecauseOfNoWrap = value;
    }

    public BreakAtLineContext getBreakAtLineContext() {
        return _breakAtLineContext;
    }

    public void setBreakAtLineContext(BreakAtLineContext breakAtLineContext) {
        _breakAtLineContext = breakAtLineContext;
    }

    /**
     * Whether further footnote content is allowed. Used to prohibit
     * footnotes inside footnotes.
     */
    public boolean isFootnoteAllowed() {
        return _isFootnoteAllowed;
    }

    /**
     * See {@link #isFootnoteAllowed()}.
     */
    public void setFootnoteAllowed(boolean allowed) {
        this._isFootnoteAllowed = allowed;
    }

    /**
     * See {@link #isInFloatBottom()}
     */
    public void setIsInFloatBottom(boolean inFloatBottom) {
        _isInFloatBottom = inFloatBottom;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInFloatBottom() {
        return _isInFloatBottom;
    }

    /**
     * See {@link #getFootnoteIndex()}
     */
    public void setFootnoteIndex(int footnoteIndex) {
        _footnoteIndex = footnoteIndex;
    }

    /**
     * The zero-based footnote index, which will likely be different from any
     * counter used with the footnote.
     */
    public int getFootnoteIndex() {
        return _footnoteIndex;
    }

    public boolean hasActiveFootnotes() {
        return _footnoteManager != null;
    }

    /**
     * Gets the document's footnote manager, creating it if required.
     * From the footnote manager, one can add and remove footnote bodies.
     */
    public FootnoteManager getFootnoteManager() {
        if (_footnoteManager == null) {
            _footnoteManager = new FootnoteManager();
        }

        return _footnoteManager;
    }

    public void setFirstLettersTracker(StyleTracker firstLetters) {
        _firstLetters = firstLetters;
    }

    public void setFirstLinesTracker(StyleTracker firstLines) {
        _firstLines = firstLines;
    }

}

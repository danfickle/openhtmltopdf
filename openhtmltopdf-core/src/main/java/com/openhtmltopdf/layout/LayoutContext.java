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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.BidiSplitter;
import com.openhtmltopdf.bidi.BidiSplitterFactory;
import com.openhtmltopdf.bidi.ParagraphSplitter;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.bidi.SimpleBidiSplitterFactory;
import com.openhtmltopdf.context.ContentFunctionFactory;
import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.CounterData;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSCanvas;
import com.openhtmltopdf.extend.FontContext;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.ReplacedElementFactory;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.MarkerData;
import com.openhtmltopdf.render.PageBox;

/**
 * This class tracks state which changes over the course of a layout run.
 * Generally speaking, if possible, state information should be stored in the box
 * tree and not here.  It also provides pass-though calls to many methods in
 * {@link SharedContext}.
 */
public class LayoutContext implements CssContext {
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

    private final Map<CalculatedStyle, CounterContext> _counterContextMap = new HashMap<CalculatedStyle, CounterContext>();

    private String _pendingPageName;
    private String _pageName;

    private int _noPageBreak = 0;

    private Layer _rootDocumentLayer;
    private PageBox _page;

    private boolean _mayCheckKeepTogether = true;

    private BreakAtLineContext _breakAtLineContext;
    
    private Boolean isPrintOverride = null; // True, false, or null for no override.

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
        
        _bfcs = new LinkedList<BlockFormattingContext>();
        _layers = new LinkedList<Layer>();

        _firstLines = new StyleTracker();
        _firstLetters = new StyleTracker();
    }

    public void reInit(boolean keepLayers) {
        _firstLines = new StyleTracker();
        _firstLetters = new StyleTracker();
        _currentMarkerData = null;

        _bfcs = new LinkedList<BlockFormattingContext>();

        if (! keepLayers) {
            _rootLayer = null;
            _layers = new LinkedList<Layer>();
        }

        _extraSpaceTop = 0;
        _extraSpaceBottom = 0;
    }

    public LayoutState captureLayoutState() {
        LayoutState result = new LayoutState();

        result.setFirstLines(_firstLines);
        result.setFirstLetters(_firstLetters);
        result.setCurrentMarkerData(_currentMarkerData);

        result.setBFCs(_bfcs);

        if (isPrint()) {
            result.setPageName(getPageName());
            result.setExtraSpaceBottom(getExtraSpaceBottom());
            result.setExtraSpaceTop(getExtraSpaceTop());
            result.setNoPageBreak(getNoPageBreak());
        }

        return result;
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
        LayoutState result = new LayoutState();

        result.setFirstLetters(_firstLetters.copyOf());
        result.setFirstLines(_firstLines.copyOf());
        result.setCurrentMarkerData(_currentMarkerData);

        if (isPrint()) {
            result.setPageName(getPageName());
        }

        return result;
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

    public void pushLayer(Box master) {
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

    private void pushLayer(Layer layer) {
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
        return _sharedContext.getUac();
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

    public int getExtraSpaceBottom() {
        return _extraSpaceBottom;
    }

    public void setExtraSpaceBottom(int extraSpaceBottom) {
        _extraSpaceBottom = extraSpaceBottom;
    }

    public int getExtraSpaceTop() {
        return _extraSpaceTop;
    }

    public void setExtraSpaceTop(int extraSpaceTop) {
        _extraSpaceTop = extraSpaceTop;
    }

    public void resolveCounters(CalculatedStyle style, Integer startIndex) {
        //new context for child elements
        CounterContext cc = new CounterContext(style, startIndex);
        _counterContextMap.put(style, cc);
    }

    public void resolveCounters(CalculatedStyle style) {
    	resolveCounters(style, null);
    }

    public CounterContext getCounterContext(CalculatedStyle style) {
        return _counterContextMap.get(style);
    }

    public FSFontMetrics getFSFontMetrics(FSFont font) {
        return getTextRenderer().getFSFontMetrics(getFontContext(), font, "");
    }

    public class CounterContext {
        private Map<String, Integer> _counters = new HashMap<String, Integer>();
        /**
         * This is different because it needs to work even when the counter- properties cascade
         * and it should also logically be redefined on each level (think list-items within list-items)
         */
        private CounterContext _parent;

        /**
         * A CounterContext should really be reflected in the element hierarchy, but CalculatedStyles
         * reflect the ancestor hierarchy just as well and also handles pseudo-elements seamlessly.
         *
         * @param style
         */
        CounterContext(CalculatedStyle style, Integer startIndex) {
        	// Numbering restarted via <ol start="x">
			if (startIndex != null) {
				_counters.put("list-item", startIndex);
			}
            _parent = _counterContextMap.get(style.getParent());
            if (_parent == null) _parent = new CounterContext();//top-level context, above root element
            //first the explicitly named counters
            List resets = style.getCounterReset();
            if (resets != null) for (Iterator i = resets.iterator(); i.hasNext();) {
                CounterData cd = (CounterData) i.next();
                _parent.resetCounter(cd);
            }

            List increments = style.getCounterIncrement();
            if (increments != null) for (Iterator i = increments.iterator(); i.hasNext();) {
                CounterData cd = (CounterData) i.next();
                if (!_parent.incrementCounter(cd)) {
                    _parent.resetCounter(new CounterData(cd.getName(), 0));
                    _parent.incrementCounter(cd);
                }
            }

            // then the implicit list-item counter
			if (style.isIdent(CSSName.DISPLAY, IdentValue.LIST_ITEM)) {
				// Numbering restarted via <li value="x">
				if (startIndex != null) {
					_parent._counters.put("list-item", startIndex);
				}
				_parent.incrementListItemCounter(1);
			}
        }

        private CounterContext() {

        }

        /**
         * @param cd
         * @return true if a counter was found and incremented
         */
        private boolean incrementCounter(CounterData cd) {
            if ("list-item".equals(cd.getName())) {//reserved name for list-item counter in CSS3
                incrementListItemCounter(cd.getValue());
                return true;
            } else {
                Integer currentValue = (Integer) _counters.get(cd.getName());
                if (currentValue == null) {
                    if (_parent == null) return false;
                    return _parent.incrementCounter(cd);
                } else {
                    _counters.put(cd.getName(), new Integer(currentValue.intValue() + cd.getValue()));
                    return true;
                }
            }
        }

        private void incrementListItemCounter(int increment) {
            Integer currentValue = (Integer) _counters.get("list-item");
            if (currentValue == null) {
                currentValue = new Integer(0);
            }
            _counters.put("list-item", new Integer(currentValue.intValue() + increment));
        }

        private void resetCounter(CounterData cd) {
            _counters.put(cd.getName(), new Integer(cd.getValue()));
        }

        public int getCurrentCounterValue(String name) {
            //only the counters of the parent are in scope
            //_parent is never null for a publicly accessible CounterContext
            Integer value = _parent.getCounter(name);
            if (value == null) {
                _parent.resetCounter(new CounterData(name, 0));
                return 0;
            } else {
                return value.intValue();
            }
        }

        private Integer getCounter(String name) {
            Integer value = (Integer) _counters.get(name);
            if (value != null) return value;
            if (_parent == null) return null;
            return _parent.getCounter(name);
        }

        public List getCurrentCounterValues(String name) {
            //only the counters of the parent are in scope
            //_parent is never null for a publicly accessible CounterContext
            List values = new ArrayList();
            _parent.getCounterValues(name, values);
            if (values.size() == 0) {
                _parent.resetCounter(new CounterData(name, 0));
                values.add(new Integer(0));
            }
            return values;
        }

        private void getCounterValues(String name, List values) {
            if (_parent != null) _parent.getCounterValues(name, values);
            Integer value = (Integer) _counters.get(name);
            if (value != null) values.add(value);
        }
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

    public BreakAtLineContext getBreakAtLineContext() {
        return _breakAtLineContext;
    }

    public void setBreakAtLineContext(BreakAtLineContext breakAtLineContext) {
        _breakAtLineContext = breakAtLineContext;
    }
}

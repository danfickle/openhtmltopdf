/*
 * CalculatedStyle.java
 * Copyright (c) 2004, 2005 Patrick Wright, Torbjoern Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package com.openhtmltopdf.css.style;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.openhtmltopdf.context.StyleReference;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.newmatch.CascadedStyle;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.CounterData;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSFunction;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.CountersValue;
import com.openhtmltopdf.css.style.derived.DerivedValueFactory;
import com.openhtmltopdf.css.style.derived.FSLinearGradient;
import com.openhtmltopdf.css.style.derived.FunctionValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.ListValue;
import com.openhtmltopdf.css.style.derived.NumberValue;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.layout.BoxBuilder;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.layout.counter.RootCounterContext;
import com.openhtmltopdf.newtable.TableBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.render.FSFontMetrics;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.ThreadCtx;
import com.openhtmltopdf.util.WebDoc;
import com.openhtmltopdf.util.WebDocLocations;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRRuntimeException;


/**
 * A set of properties that apply to a single Element, derived from all matched
 * properties following the rules for CSS cascade, inheritance, importance,
 * specificity and sequence. A property retrieved by name should always have
 * exactly one value in this class (e.g. one-one map). Some methods to retrieve
 * property values from an instance of this class require a valid {@link
 * com.openhtmltopdf.layout.LayoutContext} be given to them.
 * <br><br>
 * This is the go to class for working with a resolved style. Generally, you can get a
 * instance for a box by calling:
 *
 * <ul>
 *   <li>{@link Box#getStyle()} after a box has been created by the {@link BoxBuilder}</li>
 *   <li>{@link SharedContext#getStyle(org.w3c.dom.Element)} for an element</li>
 *   <li>{@link StyleReference#getPseudoElementStyle(org.w3c.dom.Node, String)} for a pseudo
 *       element. StyleReference is available from {@link LayoutContext}</li>
 *   <li>{@link #deriveStyle(CascadedStyle)}</li> to create a child style (non-inherited
 *       property values will not be available from the child style</li>
 *   <li>{@link EmptyStyle} to start with nothing</li>
 * </ul>
 *
 * @author Torbjoern Gannholm
 * @author Patrick Wright
 */
public class CalculatedStyle {
    /**
     * The parent-style we inherit from
     */
    private CalculatedStyle _parent;

    private BorderPropertySet _border;
    private RectPropertySet _margin;
    private RectPropertySet _padding;

    private float _lineHeight;
    private boolean _lineHeightResolved;

    private FSFont _FSFont;
    private FSFontMetrics _FSFontMetrics;

    private boolean _marginsAllowed = true;
    private boolean _paddingAllowed = true;
    private boolean _bordersAllowed = true;

    /**
     * Cache child styles of this style that have the same cascaded properties
     */
    private final java.util.Map<String, CalculatedStyle> _childCache = new java.util.HashMap<>();

    /**
     * Our main array of property values defined in this style, keyed
     * by the CSSName assigned ID.
     */
    private final FSDerivedValue[] _derivedValuesById;

    public FSDerivedValue[] getderivedValuesById() {
		return _derivedValuesById;
	}

	/**
     * The derived Font for this style
     */
    private FontSpecification _font;


    /**
     * Default constructor; as the instance is immutable after use, don't use
     * this for class instantiation externally.
     */
    protected CalculatedStyle() {
        _derivedValuesById = new FSDerivedValue[CSSName.countCSSPrimitiveNames()];
    }


    /**
     * Constructor for the CalculatedStyle object. To get a derived style, use
     * the Styler objects getDerivedStyle which will cache styles
     *
     * @param parent  PARAM
     * @param matched PARAM
     */
    private CalculatedStyle(CalculatedStyle parent, CascadedStyle matched) {
        this();
        _parent = parent;

        init(matched);
    }


    private void init(CascadedStyle matched) {
        derive(matched);

        checkPaddingAllowed();
        checkMarginsAllowed();
        checkBordersAllowed();
    }

    private void checkPaddingAllowed() {
        IdentValue v = getIdent(CSSName.DISPLAY);
        if (v == IdentValue.TABLE_HEADER_GROUP || v == IdentValue.TABLE_ROW_GROUP ||
                v == IdentValue.TABLE_FOOTER_GROUP || v == IdentValue.TABLE_ROW) {
            _paddingAllowed = false;
        } else if ((v == IdentValue.TABLE || v == IdentValue.INLINE_TABLE) && isCollapseBorders()) {
            _paddingAllowed = false;
        }
    }

    private void checkMarginsAllowed() {
        IdentValue v = getIdent(CSSName.DISPLAY);
        if (v == IdentValue.TABLE_HEADER_GROUP || v == IdentValue.TABLE_ROW_GROUP ||
                v == IdentValue.TABLE_FOOTER_GROUP || v == IdentValue.TABLE_ROW ||
                v == IdentValue.TABLE_CELL) {
            _marginsAllowed = false;
        }
    }

    private void checkBordersAllowed() {
        IdentValue v = getIdent(CSSName.DISPLAY);
        if (v == IdentValue.TABLE_HEADER_GROUP || v == IdentValue.TABLE_ROW_GROUP ||
                v == IdentValue.TABLE_FOOTER_GROUP || v == IdentValue.TABLE_ROW) {
            _bordersAllowed = false;
        }
    }

    /**
     * Derives a <strong>child</strong> style from this style. Non-inherited properties
     * such as borders will be replaced compared to <code>this</code> which is used
     * as parent style.
     * <br><br>
     * Depends on the ability to return the identical CascadedStyle each time a child style is needed
     *
     * @param matched the CascadedStyle to apply
     * @return The derived child style
     */
    public CalculatedStyle deriveStyle(CascadedStyle matched) {
        String fingerprint = matched.getFingerprint();
        CalculatedStyle cs = _childCache.get(fingerprint);

        if (cs == null) {
            cs = new CalculatedStyle(this, matched);
            _childCache.put(fingerprint, cs);
        }

        RootCounterContext cc = ThreadCtx.get().sharedContext().getGlobalCounterContext();
        cc.resetCounterValue(cs);
        cc.incrementCounterValue(cs);

        return cs;
    }

    /**
     * Override this style with specified styles. This will NOT
     * create a child style, rather an exact copy with only the specified
     * properties overridden. Compare to {@link #deriveStyle(CascadedStyle)}.
     */
    public CalculatedStyle overrideStyle(CascadedStyle matched) {
        CalculatedStyle ret = new CalculatedStyle();

        ret._parent = this._parent;
        System.arraycopy(this._derivedValuesById, 0, ret._derivedValuesById, 0, ret._derivedValuesById.length);

        init(matched);

        return ret;
    }

    /**
     * Override this style with specified styles. This will NOT
     * create a child style, rather an exact copy with only the display
     * property overridden. Compare to {@link #createAnonymousStyle(IdentValue)}
     * which creates a child style.
     */
    public CalculatedStyle overrideStyle(IdentValue display) {
        CalculatedStyle ret = new CalculatedStyle();

        ret._parent = this._parent;
        System.arraycopy(this._derivedValuesById, 0, ret._derivedValuesById, 0, ret._derivedValuesById.length);

        ret.init(CascadedStyle.createAnonymousStyle(display));

        return ret;
    }

    /**
     * Returns the parent style.
     *
     * @return Returns the parent style
     */
    public CalculatedStyle getParent() {
        return _parent;
    }

    /**
     * Converts to a String representation of the object.
     *
     * @return The borderWidth value
     */
    @Override
    public String toString() {
        return genStyleKey();
    }

    public FSColor asColor(CSSName cssName) {
        FSDerivedValue prop = valueByName(cssName);
        if (prop == IdentValue.TRANSPARENT) {
            return FSRGBColor.TRANSPARENT;
        } else {
            return prop.asColor();
        }
    }

    public float asFloat(CSSName cssName) {
        return valueByName(cssName).asFloat();
    }

    public String asString(CSSName cssName) {
        return valueByName(cssName).asString();
    }

    public String[] asStringArray(CSSName cssName) {
        return valueByName(cssName).asStringArray();
    }

    // TODO: doc
    public boolean hasAbsoluteUnit(CSSName cssName) {
        boolean isAbs = false;
        try {
            isAbs = valueByName(cssName).hasAbsoluteUnit();
        } catch (Exception e) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.LAYOUT_CSS_PROPERTY_HAS_UNPROCESSABLE_ASSIGNMENT, cssName, e.getMessage());
            isAbs = false;
        }
        return isAbs;
    }

    /**
     * Gets the ident attribute of the CalculatedStyle object
     *
     * @param cssName PARAM
     * @param val     PARAM
     * @return The ident value
     */
    public boolean isIdent(CSSName cssName, IdentValue val) {
        return valueByName(cssName) == val;
    }

    /**
     * Gets the ident attribute of the CalculatedStyle object
     *
     * @param cssName PARAM
     * @return The ident value
     */
    public IdentValue getIdent(CSSName cssName) {
        return valueByName(cssName).asIdentValue();
    }

    /**
     * Convenience property accessor; returns a Color initialized with the
     * foreground color Uses the actual value (computed actual value) for this
     * element.
     *
     * @return The color value
     */
    public FSColor getColor() {
        return asColor(CSSName.COLOR);
    }

    /**
     * Convenience property accessor; returns a Color initialized with the
     * background color value; Uses the actual value (computed actual value) for
     * this element.
     *
     * @return The backgroundColor value
     */
    public FSColor getBackgroundColor() {
        FSDerivedValue prop = valueByName(CSSName.BACKGROUND_COLOR);
        if (prop == IdentValue.TRANSPARENT) {
            return null;
        } else {
            return asColor(CSSName.BACKGROUND_COLOR);
        }
    }

    public List<CounterData> getCounterReset() {
        FSDerivedValue value = valueByName(CSSName.COUNTER_RESET);

        if (value == IdentValue.NONE) {
            return null;
        } else {
            return ((CountersValue) value).getValues();
        }
    }

    public List<CounterData> getCounterIncrement() {
        FSDerivedValue value = valueByName(CSSName.COUNTER_INCREMENT);

        if (value == IdentValue.NONE) {
            return null;
        } else {
            return  ((CountersValue) value).getValues();
        }
    }

    public BorderPropertySet getBorder(CssContext ctx) {
        if (! _bordersAllowed) {
            return BorderPropertySet.EMPTY_BORDER;
        } else {
            BorderPropertySet b = getBorderProperty(this, ctx);
            return b;
        }
    }

    public FontSpecification getFont(CssContext ctx) {
        if (_font == null) {
            _font = new FontSpecification();

            _font.families = valueByName(CSSName.FONT_FAMILY).asStringArray();

            FSDerivedValue fontSize = valueByName(CSSName.FONT_SIZE);
            if (fontSize instanceof IdentValue) {
                PropertyValue replacement;
                IdentValue resolved = resolveAbsoluteFontSize();
                if (resolved != null) {
                    replacement = FontSizeHelper.resolveAbsoluteFontSize(resolved, _font.families);
                } else {
                    replacement = FontSizeHelper.getDefaultRelativeFontSize((IdentValue) fontSize);
                }
                _font.size = LengthValue.calcFloatProportionalValue(
                        this, CSSName.FONT_SIZE, replacement.getCssText(),
                        replacement.getFloatValue(), replacement.getPrimitiveType(), 0, ctx);
            } else {
                _font.size = getFloatPropertyProportionalTo(CSSName.FONT_SIZE, 0, ctx);
            }

            _font.fontWeight = getIdent(CSSName.FONT_WEIGHT);

            _font.fontStyle = getIdent(CSSName.FONT_STYLE);
            _font.variant = getIdent(CSSName.FONT_VARIANT);
        }
        return _font;
    }

    public FontSpecification getFontSpecification() {
    return _font;
    }

    private IdentValue resolveAbsoluteFontSize() {
        FSDerivedValue fontSize = valueByName(CSSName.FONT_SIZE);
        if (! (fontSize instanceof IdentValue)) {
            return null;
        }
        IdentValue fontSizeIdent = (IdentValue) fontSize;
        if (PrimitivePropertyBuilders.ABSOLUTE_FONT_SIZES.get(fontSizeIdent.FS_ID)) {
            return fontSizeIdent;
        }

        IdentValue parent = getParent().resolveAbsoluteFontSize();
        if (parent != null) {
            if (fontSizeIdent == IdentValue.SMALLER) {
                return FontSizeHelper.getNextSmaller(parent);
            } else if (fontSize == IdentValue.LARGER) {
                return FontSizeHelper.getNextLarger(parent);
            }
        }

        return null;
    }

    public float getFloatPropertyProportionalTo(CSSName cssName, float baseValue, CssContext ctx) {
        return valueByName(cssName).getFloatProportionalTo(cssName, baseValue, ctx);
    }

    /**
     * @param cssName
     * @param parentWidth
     * @param ctx
     * @return TODO
     */
    public float getFloatPropertyProportionalWidth(CSSName cssName, float parentWidth, CssContext ctx) {
        return valueByName(cssName).getFloatProportionalTo(cssName, parentWidth, ctx);
    }

    /**
     * @param cssName
     * @param parentHeight
     * @param ctx
     * @return TODO
     */
    public float getFloatPropertyProportionalHeight(CSSName cssName, float parentHeight, CssContext ctx) {
        return valueByName(cssName).getFloatProportionalTo(cssName, parentHeight, ctx);
    }

    public float getLineHeight(CssContext ctx) {
        if (! _lineHeightResolved) {
            if (isIdent(CSSName.LINE_HEIGHT, IdentValue.NORMAL)) {
                float lineHeight1 = getFont(ctx).size * 1.1f;
                // Make sure rasterized characters will (probably) fit inside
                // the line box
                FSFontMetrics metrics = getFSFontMetrics(ctx);
                float lineHeight2 = (float)Math.ceil(metrics.getDescent() + metrics.getAscent());
                _lineHeight = Math.max(lineHeight1, lineHeight2);
            } else if (isLength(CSSName.LINE_HEIGHT)) {
                //could be more elegant, I suppose
                _lineHeight = getFloatPropertyProportionalHeight(CSSName.LINE_HEIGHT, 0, ctx);
            } else {
                //must be a number
                _lineHeight = getFont(ctx).size * valueByName(CSSName.LINE_HEIGHT).asFloat();
            }
            _lineHeightResolved = true;
        }
        return _lineHeight;
    }

    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided margin width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @param cbWidth
     * @param ctx
     * @return The marginWidth value
     */
    public RectPropertySet getMarginRect(float cbWidth, CssContext ctx) {
        return getMarginRect(cbWidth, ctx, true);
    }

    public RectPropertySet getMarginRect(float cbWidth, CssContext ctx, boolean useCache) {
        if (! _marginsAllowed) {
            return RectPropertySet.ALL_ZEROS;
        } else {
            return getMarginProperty(
                    this, CSSName.MARGIN_SHORTHAND, CSSName.MARGIN_SIDE_PROPERTIES, cbWidth, ctx, useCache);
        }
    }

    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided padding width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @param cbWidth
     * @param ctx
     * @return The paddingWidth value
     */
    public RectPropertySet getPaddingRect(float cbWidth, CssContext ctx, boolean useCache) {
        if (! _paddingAllowed) {
            return RectPropertySet.ALL_ZEROS;
        } else {
            return getPaddingProperty(this, CSSName.PADDING_SHORTHAND, CSSName.PADDING_SIDE_PROPERTIES, cbWidth, ctx, useCache);
        }
    }

    public RectPropertySet getPaddingRect(float cbWidth, CssContext ctx) {
        return getPaddingRect(cbWidth, ctx, true);
    }

    /**
     * @param cssName
     * @return TODO
     */
    public String getStringProperty(CSSName cssName) {
        return valueByName(cssName).asString();
    }

    /**
     * TODO: doc
     */
    public boolean isLength(CSSName cssName) {
        FSDerivedValue val = valueByName(cssName);
        return val instanceof LengthValue;
    }

    public boolean isLengthOrNumber(CSSName cssName) {
        FSDerivedValue val = valueByName(cssName);
        return val instanceof NumberValue || val instanceof LengthValue;
    }

    /**
     * Returns a {@link FSDerivedValue} by name. Because we are a derived
     * style, the property will already be resolved at this point.
     * <br><br>
     * This will look up the ancestor tree for inherited properties and
     * use an initial value for unspecified properties which do not inherit.
     *
     * @param cssName The CSS property name, e.g. "font-family"
     */
    public FSDerivedValue valueByName(CSSName cssName) {
        FSDerivedValue val = _derivedValuesById[cssName.FS_ID];

        boolean needInitialValue = val == IdentValue.FS_INITIAL_VALUE;

        // but the property may not be defined for this Element
        if (val == null || needInitialValue) {
            // if it is inheritable (like color) and we are not root, ask our parent
            // for the value
            if (! needInitialValue && CSSName.propertyInherits(cssName)
                    && _parent != null
                    && (val = _parent.valueByName(cssName)) != null) {
                // Do nothing, val is already set
            } else {
                // otherwise, use the initial value (defined by the CSS2 Spec)
                String initialValue = CSSName.initialValue(cssName);
                if (initialValue == null) {
                    throw new XRRuntimeException("Property '" + cssName + "' has no initial values assigned. " +
                            "Check CSSName declarations.");
                }
                if (initialValue.charAt(0) == '=') {
                    CSSName ref = CSSName.getByPropertyName(initialValue.substring(1));
                    val = valueByName(ref);
                } else {
                    val = CSSName.initialDerivedValue(cssName);
                }
            }
            _derivedValuesById[cssName.FS_ID] = val;
        }

        return val;
    }

    /**
     * This method should result in the element for this style having a
     * derived value for all specified (in stylesheets, style attribute, other non
     * CSS attrs, etc) primitive CSS properties. Other properties are picked up
     * from an ancestor (if they inherit) or their initial values (if
     * they don't inherit). See {@link #valueByName(CSSName)}.
     * <br><br>
     * The implementation is based on the notion that
     * the matched styles are given to us in a perfectly sorted order, such that
     * properties appearing later in the rule-set always override properties
     * appearing earlier.
     * <br><br>
     * The current implementation makes no attempt to check
     * this assumption. When this method exits, the derived property
     * list for this class will be populated with the properties defined for
     * this element, properly cascaded.
     */
    private void derive(CascadedStyle matched) {
        if (matched == null) {
            return;
        }

        for (PropertyDeclaration pd : matched.getCascadedPropertyDeclarations()) {
            FSDerivedValue val = deriveValue(pd.getCSSName(), pd.getValue());
            _derivedValuesById[pd.getCSSName().FS_ID] = val;
        }
    }

    private FSDerivedValue deriveValue(CSSName cssName, CSSPrimitiveValue value) {
        return DerivedValueFactory.newDerivedValue(this, cssName, (PropertyValue) value);
    }

    private String genStyleKey() {
        StringBuilder  sb = new StringBuilder();
        for (int i = 0; i < _derivedValuesById.length; i++) {
            CSSName name = CSSName.getByID(i);
            FSDerivedValue val = _derivedValuesById[i];
            if (val != null) {
                sb.append(name.toString());
            } else {
                sb.append("(no prop assigned in this pos)");
            }
            sb.append("|\n");
        }
        return sb.toString();

    }

    public RectPropertySet getCachedPadding() {
        if (_padding == null) {
            throw new XRRuntimeException("No padding property cached yet; should have called getPropertyRect() at least once before.");
        } else {
            return _padding;
        }
    }

    public RectPropertySet getCachedMargin() {
        if (_margin == null) {
            throw new XRRuntimeException("No margin property cached yet; should have called getMarginRect() at least once before.");
        } else {
            return _margin;
        }
    }

    private static RectPropertySet getPaddingProperty(CalculatedStyle style,
                                                      CSSName shorthandProp,
                                                      CSSName.CSSSideProperties sides,
                                                      float cbWidth,
                                                      CssContext ctx,
                                                      boolean useCache) {
        if (! useCache) {
            return newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
        } else {
            if (style._padding == null) {
                RectPropertySet result = newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
                boolean allZeros = result.isAllZeros();

                if (allZeros) {
                    result = RectPropertySet.ALL_ZEROS;
                }

                style._padding = result;

                if (! allZeros && style._padding.hasNegativeValues()) {
                    style._padding.resetNegativeValues();
                }
            }

            return style._padding;
        }
    }

    private static RectPropertySet getMarginProperty(CalculatedStyle style,
                                                     CSSName shorthandProp,
                                                     CSSName.CSSSideProperties sides,
                                                     float cbWidth,
                                                     CssContext ctx,
                                                     boolean useCache) {
        if (! useCache) {
            return newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
        } else {
            if (style._margin == null) {
                RectPropertySet result = newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
                if (result.isAllZeros()) {
                    result = RectPropertySet.ALL_ZEROS;
                }
                style._margin = result;
            }

            return style._margin;
        }
    }

    private static RectPropertySet newRectInstance(CalculatedStyle style,
                                                   CSSName shorthand,
                                                   CSSName.CSSSideProperties sides,
                                                   float cbWidth,
                                                   CssContext ctx) {
        RectPropertySet rect;
        rect = RectPropertySet.newInstance(style,
                shorthand,
                sides,
                cbWidth,
                ctx);
        return rect;
    }

    private static BorderPropertySet getBorderProperty(CalculatedStyle style,
                                                       CssContext ctx) {
        if (style._border == null) {
            BorderPropertySet result = BorderPropertySet.newInstance(style, ctx);

            boolean allZeros = result.isAllZeros();
            if (allZeros && ! result.hasHidden() && !result.hasBorderRadius()) {
                result = BorderPropertySet.EMPTY_BORDER;
            }

            style._border = result;

            if (! allZeros && style._border.hasNegativeValues()) {
                style._border.resetNegativeValues();
            }
        }
        return style._border;
    }

    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    public static final int TOP = 3;
    public static final int BOTTOM = 4;

    public int getMarginBorderPadding(
            CssContext cssCtx, int cbWidth, int which) {
        BorderPropertySet border = getBorder(cssCtx);
        RectPropertySet margin = getMarginRect(cbWidth, cssCtx);
        RectPropertySet padding = getPaddingRect(cbWidth, cssCtx);

        switch (which) {
            case LEFT:
                return (int) (margin.left() + border.left() + padding.left());
            case RIGHT:
                return (int) (margin.right() + border.right() + padding.right());
            case TOP:
                return (int) (margin.top() + border.top() + padding.top());
            case BOTTOM:
                return (int) (margin.bottom() + border.bottom() + padding.bottom());
            default:
                throw new IllegalArgumentException();
        }
    }

    public IdentValue getWhitespace() {
        return getIdent(CSSName.WHITE_SPACE);
    }

    public FSFont getFSFont(CssContext cssContext) {
        if (_FSFont == null) {
            _FSFont = cssContext.getFont(getFont(cssContext));
        }
        return _FSFont;
    }

    public FSFontMetrics getFSFontMetrics(CssContext c) {
        if (_FSFontMetrics == null) {
            _FSFontMetrics = c.getFSFontMetrics(getFSFont(c));
        }
        return _FSFontMetrics;
    }

    public IdentValue getWordWrap() {
        return getIdent(CSSName.WORD_WRAP);
    }

    public boolean isClearLeft() {
        IdentValue clear = getIdent(CSSName.CLEAR);
        return clear == IdentValue.LEFT || clear == IdentValue.BOTH;
    }

    public boolean isClearRight() {
        IdentValue clear = getIdent(CSSName.CLEAR);
        return clear == IdentValue.RIGHT || clear == IdentValue.BOTH;
    }

    public boolean isCleared() {
        return ! isIdent(CSSName.CLEAR, IdentValue.NONE);
    }

    public IdentValue getBackgroundRepeat(PropertyValue value) {
        return value.getIdentValue();
    }

    public boolean isInline() {
        return isIdent(CSSName.DISPLAY, IdentValue.INLINE) &&
                ! (isFloated() || isAbsolute() || isFixed() || isRunning());
    }

    public boolean isInlineBlock() {
        return isIdent(CSSName.DISPLAY, IdentValue.INLINE_BLOCK);
    }

    public boolean isTable() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE);
    }

    public boolean isInlineTable() {
        return isIdent(CSSName.DISPLAY, IdentValue.INLINE_TABLE);
    }

    public boolean isTableCell() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_CELL);
    }

    public boolean isTableSection() {
        IdentValue display = getIdent(CSSName.DISPLAY);

        return display == IdentValue.TABLE_ROW_GROUP ||
                display == IdentValue.TABLE_HEADER_GROUP ||
                display == IdentValue.TABLE_FOOTER_GROUP;
    }

    public boolean isTableCaption() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_CAPTION);
    }

    public boolean isTableHeader() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_HEADER_GROUP);
    }

    public boolean isTableFooter() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_FOOTER_GROUP);
    }

    public boolean isTableRow() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_ROW);
    }

    public boolean isDisplayNone() {
        return isIdent(CSSName.DISPLAY, IdentValue.NONE);
    }

    public boolean isSpecifiedAsBlock() {
        return isIdent(CSSName.DISPLAY, IdentValue.BLOCK);
    }

    public boolean isBlockEquivalent() {
        if (isFloated() || isAbsolute() || isFixed()) {
            return true;
        } else {
            IdentValue display = getIdent(CSSName.DISPLAY);
            if (display == IdentValue.INLINE) {
                return false;
            } else {
                return display == IdentValue.BLOCK || display == IdentValue.LIST_ITEM ||
                        display == IdentValue.RUN_IN || display == IdentValue.INLINE_BLOCK ||
                        display == IdentValue.TABLE || display == IdentValue.INLINE_TABLE;
            }
        }
    }

    public boolean isLayedOutInInlineContext() {
        if (isFloated() || isAbsolute() || isFixed() || isRunning()) {
            return true;
        } else {
            IdentValue display = getIdent(CSSName.DISPLAY);
            return display == IdentValue.INLINE || display == IdentValue.INLINE_BLOCK ||
                    display == IdentValue.INLINE_TABLE;
        }
    }

    public boolean isNeedAutoMarginResolution() {
        return ! (isAbsolute() || isFixed() || isFloated() || isInlineBlock());
    }

    public boolean isAbsolute() {
        return isIdent(CSSName.POSITION, IdentValue.ABSOLUTE);
    }

    public boolean isFixed() {
        return isIdent(CSSName.POSITION, IdentValue.FIXED);
    }

    public boolean isFloated() {
        IdentValue floatVal = getIdent(CSSName.FLOAT);
        return floatVal == IdentValue.LEFT || floatVal == IdentValue.RIGHT;
    }

    public boolean isFloatedLeft() {
        return isIdent(CSSName.FLOAT, IdentValue.LEFT);
    }

    public boolean isFloatedRight() {
        return isIdent(CSSName.FLOAT, IdentValue.RIGHT);
    }

    public boolean isFootnote() {
        return isIdent(CSSName.FLOAT, IdentValue.FOOTNOTE);
    }

    public boolean isFootnoteBody() {
        return isIdent(CSSName.DISPLAY, IdentValue.FS_FOOTNOTE_BODY);
    }

    public boolean isRelative() {
        return isIdent(CSSName.POSITION, IdentValue.RELATIVE);
    }

    public boolean isPostionedOrFloated() {
        return isAbsolute() || isFixed() || isFloated() || isRelative();
    }

    public boolean isPositioned() {
        return isAbsolute() || isFixed() || isRelative();
    }

    public boolean isAutoWidth() {
        return isIdent(CSSName.WIDTH, IdentValue.AUTO);
    }

    public boolean isAbsoluteWidth() {
        return valueByName(CSSName.WIDTH).hasAbsoluteUnit();
    }

    public boolean isAutoHeight() {
        return isIdent(CSSName.HEIGHT, IdentValue.AUTO);
    }

    public boolean isAutoLeftMargin() {
        return isIdent(CSSName.MARGIN_LEFT, IdentValue.AUTO);
    }

    public boolean isAutoRightMargin() {
        return isIdent(CSSName.MARGIN_RIGHT, IdentValue.AUTO);
    }

    public boolean isAutoZIndex() {
        return isIdent(CSSName.Z_INDEX, IdentValue.AUTO);
    }

    public boolean establishesBFC() {
        if (hasColumns()) {
            return true;
        }
        
        FSDerivedValue value = valueByName(CSSName.POSITION);

        if (value instanceof FunctionValue) {  // running(header)
            return false;
        } else {
            IdentValue display = getIdent(CSSName.DISPLAY);
            IdentValue position = (IdentValue)value;

            return isFloated() ||
                    position == IdentValue.ABSOLUTE || position == IdentValue.FIXED ||
                    display == IdentValue.INLINE_BLOCK || display == IdentValue.TABLE_CELL ||
                    ! isIdent(CSSName.OVERFLOW, IdentValue.VISIBLE);
        }
    }

    public boolean requiresLayer() {
        if (isIdent(CSSName.DISPLAY, IdentValue.INLINE)) {
            // Layers must be block or block derived (including inline-block)
            // according to modern html standards.
            return false;
        }

        if (!isIdent(CSSName.TRANSFORM, IdentValue.NONE)) {
            return true;
        }

        FSDerivedValue value = valueByName(CSSName.POSITION);

        if (value instanceof FunctionValue) {  // running(header)
            return false;
        } else {
            IdentValue position = getIdent(CSSName.POSITION);

            if (position == IdentValue.ABSOLUTE ||
                    position == IdentValue.RELATIVE || position == IdentValue.FIXED) {
                return true;
            }

            IdentValue overflow = getIdent(CSSName.OVERFLOW);
            return (overflow == IdentValue.SCROLL || overflow == IdentValue.AUTO) &&
                    isOverflowApplies();
        }
    }

    public boolean isRunning() {
        FSDerivedValue value = valueByName(CSSName.POSITION);
        return value instanceof FunctionValue;
    }

    public String getRunningName() {
        FunctionValue value = (FunctionValue)valueByName(CSSName.POSITION);
        FSFunction function = value.getFunction();

        PropertyValue param = function.getParameters().get(0);

        return param.getStringValue();
    }

    public boolean isOverflowApplies() {
        IdentValue display = getIdent(CSSName.DISPLAY);
        return display == IdentValue.BLOCK || display == IdentValue.LIST_ITEM ||
                display == IdentValue.TABLE || display == IdentValue.INLINE_BLOCK ||
                display == IdentValue.TABLE_CELL;
    }

    public boolean isOverflowVisible() {
        return valueByName(CSSName.OVERFLOW) == IdentValue.VISIBLE;
    }

    public boolean isHorizontalBackgroundRepeat(PropertyValue value) {
        return value.getIdentValue() == IdentValue.REPEAT_X || value.getIdentValue() == IdentValue.REPEAT;
    }

    public boolean isVerticalBackgroundRepeat(PropertyValue value) {
        return value.getIdentValue() == IdentValue.REPEAT_Y || value.getIdentValue() == IdentValue.REPEAT;
    }

    public boolean isTopAuto() {
        return isIdent(CSSName.TOP, IdentValue.AUTO);

    }

    public boolean isBottomAuto() {
        return isIdent(CSSName.BOTTOM, IdentValue.AUTO);
    }

    public boolean isListItem() {
        return isIdent(CSSName.DISPLAY, IdentValue.LIST_ITEM);
    }
    
    public boolean hasColumns() {
        return !isIdent(CSSName.COLUMN_COUNT, IdentValue.AUTO) && asFloat(CSSName.COLUMN_COUNT) > 1;
    }
    
    public int columnCount() {
    	return (int) asFloat(CSSName.COLUMN_COUNT);
    }
    
    public int fsMaxOverflowPages() {
        return (int) asFloat(CSSName.FS_MAX_OVERFLOW_PAGES);
    }

	/**
     * Determine if the element is visible. This is normaly the case
     * if visibility == visible. Only when visibilty is
     * -fs-table-paginate-repeated-visible and we are in a repeated table header
     * the element will also be visible. This allows to only show an element in the table header
     * after a page break.
     * @param renderingContext null or the current renderingContext. If null,
     *                         then the -fs-table-paginate-repeated-visible logic
     *                         will not work.
	 * @param thisElement the element for which the visibility should be determined. Only required if
     * -fs-table-paginate-repeated-visible is specified.
     * @return true if the element is visible
     */
    public boolean isVisible(RenderingContext renderingContext, Box thisElement) {
        IdentValue val = getIdent(CSSName.VISIBILITY);
		if (val == IdentValue.VISIBLE)
			return true;
		if (renderingContext != null) {
			if (val == IdentValue.FS_TABLE_PAGINATE_REPEATED_VISIBLE) {
				/*
				 * We need to find the parent TableBox which has a
				 * ContentLimitContainer and can be repeated.
				 */
				Box parentElement = thisElement.getParent();
				while (parentElement != null
						&& !(parentElement.getStyle().isTable()
                                && ((TableBox) parentElement).hasContentLimitContainer()))
					parentElement = parentElement.getDocumentParent();

				if (parentElement != null) {
				    TableBox tableBox = (TableBox) parentElement;
                    return !tableBox.isTableRenderedOnFirstPage(renderingContext);
				}
			}
		}
        return false;
    }

    public boolean isForcePageBreakBefore() {
        IdentValue val = getIdent(CSSName.PAGE_BREAK_BEFORE);
        return val == IdentValue.ALWAYS || val == IdentValue.LEFT
                || val == IdentValue.RIGHT;
    }

    public boolean isForcePageBreakAfter() {
        IdentValue val = getIdent(CSSName.PAGE_BREAK_AFTER);
        return val == IdentValue.ALWAYS || val == IdentValue.LEFT
                || val == IdentValue.RIGHT;
    }
    
    public boolean isColumnBreakBefore() {
        return isIdent(CSSName.BREAK_BEFORE, IdentValue.COLUMN);
    }
    
    public boolean isColumnBreakAfter() {
        return isIdent(CSSName.BREAK_AFTER, IdentValue.COLUMN);
    }

    public boolean isAvoidPageBreakInside() {
        return isIdent(CSSName.PAGE_BREAK_INSIDE, IdentValue.AVOID);
    }

    /**
     * This method derives a style for an anonymous child box with an overriden
     * value for the display property.
     * <br><br>
     * NOTE: All non-inherited properties of <code>this</code> will be lost as
     * the returned style is for a child box.
     */
    public CalculatedStyle createAnonymousStyle(IdentValue display) {
        return deriveStyle(CascadedStyle.createAnonymousStyle(display));
    }

    public boolean mayHaveFirstLine() {
        IdentValue display = getIdent(CSSName.DISPLAY);
        return display == IdentValue.BLOCK ||
                display == IdentValue.LIST_ITEM ||
                display == IdentValue.RUN_IN ||
                display == IdentValue.TABLE ||
                display == IdentValue.TABLE_CELL ||
                display == IdentValue.TABLE_CAPTION ||
                display == IdentValue.INLINE_BLOCK;
    }

    public boolean mayHaveFirstLetter() {
        IdentValue display = getIdent(CSSName.DISPLAY);
        return display == IdentValue.BLOCK ||
                display == IdentValue.LIST_ITEM ||
                display == IdentValue.TABLE_CELL ||
                display == IdentValue.TABLE_CAPTION ||
                display == IdentValue.INLINE_BLOCK;
    }

    public boolean isNonFlowContent() {
        return isFloated() || isAbsolute() || isFixed() || isRunning();
    }

    public boolean isMayCollapseMarginsWithChildren() {
        return isIdent(CSSName.OVERFLOW, IdentValue.VISIBLE) &&
                ! (isFloated() || isAbsolute() || isFixed() || isInlineBlock());
    }

    public boolean isAbsFixedOrInlineBlockEquiv() {
        return isAbsolute() || isFixed() || isInlineBlock() || isInlineTable();
    }

    public boolean isMaxWidthNone() {
        return isIdent(CSSName.MAX_WIDTH, IdentValue.NONE);
    }

    public boolean isMaxHeightNone() {
        return isIdent(CSSName.MAX_HEIGHT, IdentValue.NONE);
    }

    public boolean isImageRenderingPixelated() {
        return isIdent(CSSName.IMAGE_RENDERING, IdentValue.PIXELATED) || isIdent(CSSName.IMAGE_RENDERING, IdentValue.CRISP_EDGES);
    }
    public boolean isImageRenderingInterpolate(){
        return !isImageRenderingPixelated();
    }

    public int getMinWidth(CssContext c, int cbWidth) {
        return (int) getFloatPropertyProportionalTo(CSSName.MIN_WIDTH, cbWidth, c);
    }

    public int getMaxWidth(CssContext c, int cbWidth) {
        return (int) getFloatPropertyProportionalTo(CSSName.MAX_WIDTH, cbWidth, c);
    }

    public int getMinHeight(CssContext c, int cbHeight) {
        return (int) getFloatPropertyProportionalTo(CSSName.MIN_HEIGHT, cbHeight, c);
    }

    public int getMaxHeight(CssContext c, int cbHeight) {
        return (int) getFloatPropertyProportionalTo(CSSName.MAX_HEIGHT, cbHeight, c);
    }

    public boolean isCollapseBorders() {
    	// The second part of this condition was commented out by @danfickle because seems unneccessary
    	// See https://github.com/danfickle/openhtmltopdf/issues/97
    	
        return isIdent(CSSName.BORDER_COLLAPSE, IdentValue.COLLAPSE); // && ! isPaginateTable();
    }

    public int getBorderHSpacing(CssContext c) {
        return isCollapseBorders() ? 0 : (int) getFloatPropertyProportionalTo(CSSName.FS_BORDER_SPACING_HORIZONTAL, 0, c);
    }

    public int getBorderVSpacing(CssContext c) {
        return isCollapseBorders() ? 0 : (int) getFloatPropertyProportionalTo(CSSName.FS_BORDER_SPACING_VERTICAL, 0, c);
    }

    public int getRowSpan() {
        int result = (int) asFloat(CSSName.FS_ROWSPAN);
        return result > 0 ? result : 1;
    }

    public int getColSpan() {
        int result = (int) asFloat(CSSName.FS_COLSPAN);
        return result > 0 ? result : 1;
    }

    public float getFSPageBreakMinHeight(CssContext c){
        return getFloatPropertyProportionalTo(CSSName.FS_PAGE_BREAK_MIN_HEIGHT, 0, c);
    }

    public Length asLength(CssContext c, CSSName cssName) {
        Length result = new Length();

        FSDerivedValue value = valueByName(cssName);
        if (value instanceof LengthValue || value instanceof NumberValue) {
            if (value.hasAbsoluteUnit()) {
                result.setValue((int) value.getFloatProportionalTo(cssName, 0, c));
                result.setType(Length.FIXED);
            } else {
                result.setValue((int) value.asFloat());
                result.setType(Length.PERCENT);
            }
        }

        return result;
    }

    public boolean isShowEmptyCells() {
        return isCollapseBorders() || isIdent(CSSName.EMPTY_CELLS, IdentValue.SHOW);
    }

    public List<IdentValue> getTextDecorations() {
        FSDerivedValue value = valueByName(CSSName.TEXT_DECORATION);
        if (value == IdentValue.NONE) {
            return null;
        } else {
            List<PropertyValue> idents = ((ListValue) value).getValues();
            return idents.stream()
                    .map(val -> (IdentValue) DerivedValueFactory.newDerivedValue(this, CSSName.TEXT_DECORATION, val))
                    .collect(Collectors.toList());
        }
    }

    public Cursor getCursor() {
        FSDerivedValue value = valueByName(CSSName.CURSOR);

        if (value == IdentValue.AUTO || value == IdentValue.DEFAULT) {
            return Cursor.getDefaultCursor();
        } else if (value == IdentValue.CROSSHAIR) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        } else if (value == IdentValue.POINTER) {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        } else if (value == IdentValue.MOVE) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        } else if (value == IdentValue.E_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        } else if (value == IdentValue.NE_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        } else if (value == IdentValue.NW_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        } else if (value == IdentValue.N_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        } else if (value == IdentValue.SE_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
        } else if (value == IdentValue.SW_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
        } else if (value == IdentValue.S_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        } else if (value == IdentValue.W_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        } else if (value == IdentValue.TEXT) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        } else if (value == IdentValue.WAIT) {
            return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        } else if (value == IdentValue.HELP) {
            // We don't have a cursor for this by default, maybe we need
            // a custom one for this (but I don't like it).
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        } else if (value == IdentValue.PROGRESS) {
            // We don't have a cursor for this by default, maybe we need
            // a custom one for this (but I don't like it).
            return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        }

        return null;
    }

    public boolean isPaginateTable() {
        return isIdent(CSSName.FS_TABLE_PAGINATE, IdentValue.PAGINATE);
    }

    public boolean isTextJustify() {
        return isIdent(CSSName.TEXT_ALIGN, IdentValue.JUSTIFY) &&
                ! (isIdent(CSSName.WHITE_SPACE, IdentValue.PRE) ||
                        isIdent(CSSName.WHITE_SPACE, IdentValue.PRE_LINE));
    }

    public boolean isListMarkerInside() {
        return isIdent(CSSName.LIST_STYLE_POSITION, IdentValue.INSIDE);
    }

    public boolean isKeepWithInline() {
        return isIdent(CSSName.FS_KEEP_WITH_INLINE, IdentValue.KEEP);
    }

    public boolean isDynamicAutoWidth() {
        return isIdent(CSSName.FS_DYNAMIC_AUTO_WIDTH, IdentValue.DYNAMIC);
    }

    public boolean isDynamicAutoWidthApplicable() {
        return isDynamicAutoWidth() && isAutoWidth() && ! isCanBeShrunkToFit();
    }

    public boolean isCanBeShrunkToFit() {
        return isInlineBlock() || isFloated() || isAbsolute() || isFixed();
    }
    
    public boolean isDirectionLTR() {
    	return isIdent(CSSName.DIRECTION, IdentValue.LTR);
    }
    
    public boolean isDirectionRTL() {
    	return isIdent(CSSName.DIRECTION, IdentValue.RTL);
    }
    
    public boolean isDirectionAuto() {
    	return isIdent(CSSName.DIRECTION, IdentValue.AUTO);
    }

	public IdentValue getDirection() {
		return getIdent(CSSName.DIRECTION);
	}
	
	public boolean hasLetterSpacing() {
	    return !isIdent(CSSName.LETTER_SPACING, IdentValue.NORMAL);
	}
	
	public boolean isParagraphContainerForBidi() {
		IdentValue display = getIdent(CSSName.DISPLAY);
		
		return (display != IdentValue.INLINE &&
				display != IdentValue.INLINE_BLOCK &&
				display != IdentValue.INLINE_TABLE) ||
				isNonFlowContent();
	}
	
	/**
	 * @return true for border-box, false for content-box.
	 */
    public boolean isBorderBox() {
        return isIdent(CSSName.BOX_SIZING, IdentValue.BORDER_BOX);
    }
	
	/**
	 * Aims to get the correct resolved max-width for a box in dots unit.
	 * Returns -1 if there is no max-width defined.
	 * Assumptions: box has a containing block.
	 */
	public static int getCSSMaxWidth(CssContext c, Box box) {
		if (box.getStyle().isMaxWidthNone()) {
			return -1;
		}
		
	    return box.getStyle().getMaxWidth(c, box.getContainingBlock().getContentWidth());
	}

	/**
	 * Aims to get the correct resolved max-height for a box in dots unit.
	 * returns -1 if there is no max-height defined.
	 * Assumptions: box has a containing block.
	 */
	public static int getCSSMaxHeight(CssContext c, Box box) {
		if (box.getStyle().isMaxHeightNone()) {
			return -1;
		}
		
		Length cssMaxHeight = box.getStyle().asLength(c, CSSName.MAX_HEIGHT);

		/* 	MDN says:
		 *  The percentage is calculated with respect to the height of the generated box's containing block.
		 *  If the height of the containing block is not specified explicitly (i.e., it depends on content height),
		 *  and this element is not absolutely positioned, the percentage value is treated as none.*/
		
		if (cssMaxHeight.isPercent() &&
			box.getContainingBlock().getStyle().hasAbsoluteUnit(CSSName.HEIGHT)) {
			return (int) (cssMaxHeight.value() * box.getContainingBlock().getStyle().asLength(c, CSSName.HEIGHT).value() / 100f);
		} else if (cssMaxHeight.isPercent()) {
			return -1;
		} else {
			return (int) cssMaxHeight.value();
		}
	}

    public boolean isHasBackground() {
        return !isIdent(CSSName.BACKGROUND_COLOR, IdentValue.TRANSPARENT) ||
               isHasBackgroundImage();
    }

    public boolean isHasBackgroundImage() {
        List<PropertyValue> backgroundImages = ((ListValue) valueByName(CSSName.BACKGROUND_IMAGE)).getValues();

        if (backgroundImages.size() == 1) {
            return backgroundImages.get(0).getIdentValue() != IdentValue.NONE;
        } else {
            return backgroundImages.stream().anyMatch(val -> val.getIdentValue() != IdentValue.NONE);
        }
    }

    public enum BackgroundImageType {
        URI, GRADIENT, NONE;
    }

    public static class BackgroundContainer {
        public BackgroundImageType type;
        public PropertyValue imageGradientOrNone;

        public BackgroundPosition backgroundPosition;
        public BackgroundSize backgroundSize;
        public PropertyValue backgroundRepeat;
    }

    public boolean isLinearGradient(PropertyValue value) {
        return value.getPropertyValueType() == PropertyValue.VALUE_TYPE_FUNCTION &&
               Objects.equals(value.getFunction().getName(), "linear-gradient");
    }

    public FSLinearGradient getLinearGradient(
            PropertyValue value, CssContext cssContext, int boxWidth, int boxHeight) {

        if (!isLinearGradient(value)) {
            return null;
        }

        return new FSLinearGradient(this, value.getFunction(), boxWidth, boxHeight, cssContext);
    }

    /**
     * Gets the values of the background properties and combines in a list
     * of BackgroundContainer values.
     */
    @WebDoc(WebDocLocations.CSS_BACKGROUND_PROPERTIES)
    public List<BackgroundContainer> getBackgroundImages() {
        List<PropertyValue> images = ((ListValue) valueByName(CSSName.BACKGROUND_IMAGE)).getValues();
        List<PropertyValue> positions = ((ListValue) valueByName(CSSName.BACKGROUND_POSITION)).getValues();
        List<PropertyValue> repeats = ((ListValue) valueByName(CSSName.BACKGROUND_REPEAT)).getValues();
        List<PropertyValue> sizes = ((ListValue) valueByName(CSSName.BACKGROUND_SIZE)).getValues();

        assert positions.size() % 2 == 0;
        assert sizes.size() % 2 == 0;

        List<BackgroundPosition> posPairs = new ArrayList<>(positions.size() / 2);
        for (int i = 0; i < positions.size(); i += 2) {
            posPairs.add(new BackgroundPosition(positions.get(i), positions.get(i + 1)));
        }

        List<BackgroundSize> sizePairs = new ArrayList<>(sizes.size() / 2);
        for (int i = 0; i < sizes.size(); i += 2) {
            sizePairs.add(new BackgroundSize(sizes.get(i), sizes.get(i + 1)));
        }

        List<BackgroundContainer> backgrounds = new ArrayList<>(images.size());

        for (int i = 0; i < images.size(); i++) {
            BackgroundContainer bg = new BackgroundContainer();
            PropertyValue img = images.get(i);

            if (isLinearGradient(img)) {
                bg.type = BackgroundImageType.GRADIENT;
            } else if (img.getIdentValue() == IdentValue.NONE) {
                bg.type = BackgroundImageType.NONE;
            } else {
                bg.type = BackgroundImageType.URI;
            }

            bg.imageGradientOrNone = img;

            // If less background-position values are provided than images,
            // they must repeat.
            bg.backgroundPosition = posPairs.get(i % posPairs.size());
            bg.backgroundSize = sizePairs.get(i % sizePairs.size());
            bg.backgroundRepeat = repeats.get(i % repeats.size());

            backgrounds.add(bg);
        }

        // Pre-reverse the images, from back to front.
        Collections.reverse(backgrounds);
        return backgrounds;
    }
}

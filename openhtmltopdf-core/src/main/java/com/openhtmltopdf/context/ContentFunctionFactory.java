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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.extend.ContentFunction;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.FSFunction;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.layout.CounterFunction;
import com.openhtmltopdf.layout.InlineBoxing;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.InlineBox;
import com.openhtmltopdf.render.InlineLayoutBox;
import com.openhtmltopdf.render.InlineText;
import com.openhtmltopdf.render.LineBox;
import com.openhtmltopdf.render.RenderingContext;

public class ContentFunctionFactory {
    private final List<ContentFunction> _functions = new ArrayList<>();

    public ContentFunctionFactory() {
        _functions.add(new PageCounterFunction());
        _functions.add(new PagesCounterFunction());
        _functions.add(new TargetCounterFunction());
        _functions.add(new TargetTextFunction());
        _functions.add(new LeaderFunction());
        _functions.add(new FsIfCutOffFunction());
    }
    
    public ContentFunction lookupFunction(LayoutContext c, FSFunction function) {
        return _functions.stream()
                         .filter(f -> f.canHandle(c, function))
                         .findFirst().orElse(null);
    }
   
    public void registerFunction(ContentFunction function) {
        _functions.add(function);
    }
    
    /**
     * Content function which returns its argument if on a cut off page, else the empty string.
     * Example:
     *   <code>content: "Page " counter(page) -fs-if-cut-off(" continued") " of " counter(pages);</code> 
     */
    private static class FsIfCutOffFunction extends ContentFunctionAbstract {

        @Override
        public String calculate(LayoutContext c, FSFunction function) {
            return null;
        }

        @Override
        public String calculate(RenderingContext c, FSFunction function, InlineText text) {
            return c.getShadowPageNumber() >= 0 ?
                   function.getParameters().get(0).getStringValue() :
                   "";
        }

        @Override
        public String getLayoutReplacementText() {
            // Needs to be non-empty.
            return "cont";
        }

        @Override
        public boolean canHandle(LayoutContext c, FSFunction function) {
            return function.getName().equals("-fs-if-cut-off") &&
                   function.getParameters().size() == 1 &&
                   function.getParameters().get(0).getPrimitiveType() == CSSPrimitiveValue.CSS_STRING;
        }
    }
    
    private static abstract class PageNumberFunction extends ContentFunctionAbstract {
        
        @Override
        public String calculate(LayoutContext c, FSFunction function) {
            return null;
        }
        
        @Override
        public String getLayoutReplacementText() {
            return "999";
        }
        
        protected IdentValue getListStyleType(FSFunction function) {
            IdentValue result = IdentValue.DECIMAL;
            
            List<PropertyValue> parameters = function.getParameters();
            if (parameters.size() == 2) {
                PropertyValue pValue = parameters.get(1);
                IdentValue iValue = IdentValue.valueOf(pValue.getStringValue());
                if (iValue != null) {
                    result = iValue;
                }
            }
            
            return result;
        }
        
        protected boolean isCounter(FSFunction function, String counterName) {
            if (function.getName().equals("counter")) {
                List<PropertyValue> parameters = function.getParameters();
                if (parameters.size() == 1 || parameters.size() == 2) {
                    PropertyValue param = parameters.get(0);
                    if (param.getPrimitiveType() != CSSPrimitiveValue.CSS_IDENT ||
                            ! param.getStringValue().equals(counterName)) {
                        return false;
                    }
                    
                    if (parameters.size() == 2) {
                        param = parameters.get(1);
                        return param.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT;
                    }
                    
                    return true;
                }
            }
            
            return false;
        }
    }
    
    private static class PageCounterFunction extends PageNumberFunction {
        @Override
        public String calculate(RenderingContext c, FSFunction function, InlineText text) {
            int value = c.getRootLayer().getRelativePageNo(c) + 1;
            return CounterFunction.createCounterText(getListStyleType(function), value);
        }
        
        @Override
        public boolean canHandle(LayoutContext c, FSFunction function) {
            return c.isPrint() && isCounter(function, "page");
        }
    }
    
    private static class PagesCounterFunction extends PageNumberFunction {
        @Override
        public String calculate(RenderingContext c, FSFunction function, InlineText text) {
            int value = c.getRootLayer().getRelativePageCount(c);
            return CounterFunction.createCounterText(getListStyleType(function), value);
        }

        @Override
        public boolean canHandle(LayoutContext c, FSFunction function) {
            return c.isPrint() && isCounter(function, "pages");
        }
    }
    
    /**
     * Partially implements target counter as specified here:
     * http://www.w3.org/TR/2007/WD-css3-gcpm-20070504/#cross-references
     */
    private static class TargetCounterFunction extends ContentFunctionAbstract {

        @Override
        public String calculate(RenderingContext c, FSFunction function, InlineText text) {
            // Due to how BoxBuilder::wrapGeneratedContent works, it is likely the immediate
            // parent of text is an anonymous InlineLayoutBox so we have to go up another
            // level to the wrapper box which contains the element.
            Element hrefElement = text.getParent().getElement() == null ?
                    text.getParent().getParent().getElement() :
                    text.getParent().getElement();

            String uri = hrefElement.getAttribute("href");

            if (uri != null && uri.startsWith("#")) {
                String anchor = uri.substring(1);
                Box target = c.getBoxById(anchor);
                if (target != null) {
                    int pageNo = c.getRootLayer().getRelativePageNo(c, target.getAbsY());
                    IdentValue counterStyle = IdentValue.DECIMAL; // default type
                    List<PropertyValue> params = function != null ? function.getParameters() : Collections.emptyList();
                    if (params.size() > 2) {
                        IdentValue iValue = IdentValue.valueOf(params.get(2).getStringValue());
                        if (iValue != null) {
                            counterStyle = iValue;
                        }
                    }
                    return CounterFunction.createCounterText(counterStyle, pageNo + 1);
                }
            }
            return "";
        }

        @Override
        public String calculate(LayoutContext c, FSFunction function) {
            return null;
        }
        
        @Override
        public String getLayoutReplacementText() {
            return "999";
        }

        @Override
        public boolean canHandle(LayoutContext c, FSFunction function) {
            if (c.isPrint() && function.getName().equals("target-counter")) {
                List<PropertyValue> parameters = function.getParameters();
                if (parameters.size() == 2 || parameters.size() == 3) {
                    FSFunction f = parameters.get(0).getFunction();
                    if (f == null ||
                            f.getParameters().size() != 1 ||
                            f.getParameters().get(0).getPrimitiveType() != CSSPrimitiveValue.CSS_IDENT ||
                            ! f.getParameters().get(0).getStringValue().equals("href")) {
                        return false;
                    }

                    PropertyValue param = parameters.get(1);
                    return param.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT &&
                            param.getStringValue().equals("page");
                }
            }
            
            return false;
        }
    }

    /**
     * https://www.w3.org/TR/css-gcpm-3/#target-text<br>
     * https://www.w3.org/TR/css-values-3/#attr-notation
     * <br><br>
     * We support <code>target-text(attr(xxx))</code> and <code>target-text(attr(xxx url))</code>
     * where xxx is an attribute name and url is the
     * type returned (only url is supported as the second param).
     * <br><br>
     * Limitations:<br>
     * The value returned from the attribute must start with hash (#), it
     * is not resolved to an absolute url.<br>
     * We only support returning the content of the target element, not a specific pseudo element.
     */
    private static class TargetTextFunction extends ContentFunctionAbstract {

        @Override
        public boolean isCalculableAtLayout() {
            return true;
        }

        @Override
        public String calculate(RenderingContext c, FSFunction function, InlineText text) {
            // FIXME: Not sure this method is used any longer.
            // Prefer getPostBoxingLayoutReplacementText below.

            // Due to how BoxBuilder::wrapGeneratedContent works, it is likely the immediate
            // parent of text is an anonymous InlineLayoutBox so we have to go up another
            // level to the wrapper box which contains the element.
            Element hrefElement = text.getParent().getElement() == null ?
                    text.getParent().getParent().getElement() :
                    text.getParent().getElement();

            // We want to get xxx in target-text(attr(xxx))
            // Default to href.
            String attribute =
                  function.getParameters().isEmpty() ||
                  function.getParameters().get(0).getFunction() == null ||
                  !"attr".equals(function.getParameters().get(0).getFunction().getName()) ||
                  function.getParameters().get(0).getFunction().getParameters().isEmpty() ?
                     "href" : function.getParameters().get(0).getFunction().getParameters().get(0).getStringValue();

            String uri = hrefElement.getAttribute(attribute);

            if (uri != null && uri.startsWith("#")) {
                String anchor = uri.substring(1);
                Box target = c.getBoxById(anchor);
                if (target != null) {
                    StringBuilder strBuilder = new StringBuilder();
                    target.collectText(c, strBuilder);
                    return strBuilder.toString();
                }
            }
            return "";
        }

        @Override
        public String calculate(LayoutContext c, FSFunction function) {
            return null;
        }

        @Override
        public String getLayoutReplacementText() {
            return "ABCABC";
        }

        @Override
        public String getPostBoxingLayoutReplacementText(
                LayoutContext c, Element hrefElement, FSFunction function) {

            if (hrefElement == null) {
                return "";
            }

            // We want to get xxx in target-text(attr(xxx))
            // Default to href.
            String attribute =
                  function.getParameters().isEmpty() ||
                  function.getParameters().get(0).getFunction() == null ||
                  !"attr".equals(function.getParameters().get(0).getFunction().getName()) ||
                  function.getParameters().get(0).getFunction().getParameters().isEmpty() ?
                     "href" : function.getParameters().get(0).getFunction().getParameters().get(0).getStringValue();

            String uri = hrefElement.getAttribute(attribute);

            if (uri.startsWith("#")) {
                String href = uri.substring(1);
                Object target = c.getLayoutBox(href);

                if (target == null) {
                    return "";
                } else if (target instanceof Box) {
                    Box box = (Box) target;
                    StringBuilder strBuilder = new StringBuilder();
                    box.collectLayoutText(c, strBuilder);
                    return strBuilder.toString();
                } else if (target instanceof InlineBox) {
                    InlineBox ib = (InlineBox) target;
                    return ib.getText();
                }
            }

            return "";
        }

        @Override
        public boolean canHandle(LayoutContext c, FSFunction function) {
            if (c.isPrint() && function.getName().equals("target-text")) {
                List<PropertyValue> parameters = function.getParameters();
                if(parameters.size() == 1) {
                    FSFunction f = parameters.get(0).getFunction();

                    return "attr".equals(f.getName()) &&
                              (f.getParameters().size() == 1 ||
                                (f.getParameters().size() == 2 && "url".equals(f.getParameters().get(1).getStringValue())));
                }
            }
            return false;
        }
    }

    /**
     * Partially implements leaders as specified here:
     * http://www.w3.org/TR/2007/WD-css3-gcpm-20070504/#leaders
     */
    public static class LeaderFunction extends ContentFunctionAbstract {

        @Override
        public String calculate(RenderingContext c, FSFunction function, InlineText text) {
            InlineLayoutBox iB = text.getParent();
            LineBox lineBox = iB.getLineBox();

            // There might be a dynamic function (target-counter, counter, etc)
            // after this function.
            // Because the leader should fill up the line, we need the correct
            // width and must first compute the dynamic function.
            boolean dynamic = false;
            boolean dynamic2 = false;
            Box wrapperBox = iB.getParent();
            List<? extends Object> children = null;

            // The type of wrapperBox will depend on the CSS display property
            // of the pseudo element where the content property using this function exists.
            // See BoxBuilder#wrapGeneratedContent.
            if (wrapperBox instanceof InlineLayoutBox) {
                children = ((InlineLayoutBox) wrapperBox).getInlineChildren();
            } else {
                children = wrapperBox.getChildren();
            }

            for (Object child : children) {
                if (child == iB) {
                    // Don't call InlineLayoutBox::lookForDynamicFunctions on this box
                    // as we will arrive back here and end up as a stack overflow.
                    // Instead set the dynamic flag so we resolve subsequent dynamic functions.
                    dynamic = true;
                } else if (child instanceof InlineLayoutBox) {
                    // This forces the computation of dynamic functions.

                    // NOTE: We do not evaluate other leaders as this would 
                    // cause a battle with this leader and that leader ending in a
                    // stack overflow or worse an endless loop.
                    ((InlineLayoutBox)child).lookForDynamicFunctions(c, false);
                }
            }

            // We also have to evaluate subsequent dynamic functions at the line level
            // in the case that we are in the ::before and subsequent functions are in ::after.
            if (dynamic) {
                for (Box child : lineBox.getChildren()) {
                    if (wrapperBox == child) {
                        dynamic2 = true;
                    } else if (dynamic2 && child instanceof InlineLayoutBox) {
                        ((InlineLayoutBox)child).lookForDynamicFunctions(c, false);
                    }
                }
            }

            if (dynamic) {
                // Re-calculate the width of the line after subsequent dynamic functions
                // have been calculated.
                int totalLineWidth = InlineBoxing.positionHorizontally(c, lineBox, 0);
                lineBox.setContentWidth(totalLineWidth);
            }

            // Get leader value and value width
            PropertyValue param = function.getParameters().get(0);
            String value = param.getStringValue();
            if (param.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                if (value.equals("dotted")) {
                    value = ". ";
                } else if (value.equals("solid")) {
                    value = "_";
                } else if (value.equals("space")) {
                    value = " ";
                }
            }

            // Compute value width using 100x string to get more precise width.
            // Otherwise there might be a small gap at the right side. This is
            // necessary because a TextRenderer usually use double/float for width.
            StringBuilder tmp = new StringBuilder(100 * value.length());
            for (int i = 0; i < 100; i++) {
                tmp.append(value);
            }
            float valueWidth = c.getTextRenderer().getWidth(c.getFontContext(),
                    iB.getStyle().getFSFont(c), tmp.toString()) / 100f;
            int spaceWidth = c.getTextRenderer().getWidth(c.getFontContext(),
                    iB.getStyle().getFSFont(c), " ");

            // compute leader width and necessary count of values
            int leaderWidth = iB.getContainingBlockWidth() - iB.getLineBox().getWidth() + text.getWidth();
            int count = (int) ((leaderWidth - (2 * spaceWidth)) / valueWidth);

            // build leader string
            StringBuilder buf = new StringBuilder(count * value.length() + 2);
            buf.append(' ');
            for (int i = 0; i < count; i++) {
                buf.append(value);
            }
            buf.append(' ');
            String leaderString = buf.toString();

            // set left margin to ensure that the leader is right aligned (for TOC)
            int leaderStringWidth = c.getTextRenderer().getWidth(c.getFontContext(),
                    iB.getStyle().getFSFont(c), leaderString);
            iB.setMarginLeft(c, leaderWidth - leaderStringWidth);

            return leaderString;
        }

        @Override
        public String calculate(LayoutContext c, FSFunction function) {
            return null;
        }
        
        @Override
        public String getLayoutReplacementText() {
            return " . ";
        }

        @Override
        public boolean canHandle(LayoutContext c, FSFunction function) {
            if (c.isPrint() && function.getName().equals("leader")) {
                List<PropertyValue> parameters = function.getParameters();
                if (parameters.size() == 1) {
                    PropertyValue param = parameters.get(0);
                    return param.getPrimitiveType() == CSSPrimitiveValue.CSS_STRING ||
                            (param.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT &&
                                    (param.getStringValue().equals("dotted") ||
                                            param.getStringValue().equals("solid") ||
                                            param.getStringValue().equals("space")));
                }
            }
            
            return false;
        }
    }
}

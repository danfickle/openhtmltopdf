/*
 * {{{ header & license
 * Copyright (c) 2007 Sean Bright
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
package com.openhtmltopdf.simple.extend.form;

import javax.swing.JComponent;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.FSFont;
import com.openhtmltopdf.simple.extend.URLUTF8Encoder;
import com.openhtmltopdf.simple.extend.XhtmlForm;
import com.openhtmltopdf.swing.AWTFSFont;

import java.awt.*;

public abstract class FormField {
    private XhtmlForm _parentForm;
    private Element _element;
    private FormFieldState _originalState;
    private JComponent _component;
    private LayoutContext context;
    private BlockBox box;
    protected Integer intrinsicWidth;
    protected Integer intrinsicHeight;
    

    public FormField(Element e, XhtmlForm form, LayoutContext context, BlockBox box) {
        _element = e;
        _parentForm = form;
        this.context = context;
        this.box = box;
        
        initialize();
    }

    protected Element getElement() {
        return _element;
    }
    
    public JComponent getComponent() {
        return _component;
    }
    
    public XhtmlForm getParentForm() {
        return _parentForm;
    }

    public Dimension getIntrinsicSize(){

        int width = intrinsicWidth == null ? 0 : intrinsicWidth.intValue();
        int height = intrinsicHeight == null ? 0 : intrinsicHeight.intValue();

        return new Dimension(width, height);
    }

    
    public void reset() {
        applyOriginalState();
    }

    protected UserAgentCallback getUserAgentCallback() {
        return _parentForm.getUserAgentCallback();
    }

    protected FormFieldState getOriginalState() {
        if (_originalState == null) {
            _originalState = loadOriginalState();
        }

        return _originalState;
    }
    
    protected boolean hasAttribute(String attributeName) {
        return getElement().getAttribute(attributeName).length() > 0;
    }

    protected String getAttribute(String attributeName) {
        return getElement().getAttribute(attributeName);
    }
    
    private void initialize() {
        _component = create();
        
        if (_component != null) {
            if (intrinsicWidth == null)
                intrinsicWidth = new Integer(_component.getPreferredSize().width);
            if (intrinsicHeight == null)
                intrinsicHeight = new Integer(_component.getPreferredSize().height);

            _component.setSize(getIntrinsicSize());

            String d = _element.getAttribute("disabled");
            if (d.equalsIgnoreCase("disabled")) {
                _component.setEnabled(false);
            }
        }

        applyOriginalState();
    }

    public abstract JComponent create();

    protected FormFieldState loadOriginalState() {
        return FormFieldState.fromString("");
    }
    
    protected void applyOriginalState() {
        // Do nothing
    }
    
    /**
     * Returns true if the value of the current FormField should be
     * sent along with the current submission.  This is used so that
     * only the value of the submit button that is used to trigger the
     * form's submission is sent.
     * 
     * @param source The JComponent that caused the submission
     * @return true if it should
     */
    public boolean includeInSubmission(JComponent source) {
        return true;
    }

    // These two methods are temporary but I am using them to clean up
    // the code in XhtmlForm
    public String[] getFormDataStrings() {
        // Fields MUST have at least a name attribute to get sent.  The attr
        // can be empty, or just white space, but it must be present
        if (!hasAttribute("name")) {
            return new String[] {};
        }

        String name = getAttribute("name");
        String[] values = getFieldValues();

        for (int i = 0; i < values.length; i++) {
            values[i] = URLUTF8Encoder.encode(name) + "=" + URLUTF8Encoder.encode(values[i]);
        }

        return values;
    }

    protected abstract String[] getFieldValues();


    public BlockBox getBox() {
        return box;
    }

    public LayoutContext getContext() {
        return context;
    }

    public CalculatedStyle getStyle() {
        return getBox().getStyle();
    }

    protected void applyComponentStyle(JComponent comp) {
        Font font = getFont();
        if (font != null) {
            comp.setFont(font);
        }

        CalculatedStyle style = getStyle();

        FSColor foreground = style.getColor();
        if (foreground != null) {
            comp.setForeground(toColor(foreground));
        }

        FSColor background = style.getBackgroundColor();
        if (background != null) {
            comp.setBackground(toColor(background));
        }
    }

    private static Color toColor(FSColor color)
    {
        if (color instanceof FSRGBColor) {
            FSRGBColor rgb = (FSRGBColor)color;
            return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
        }
        throw new RuntimeException("internal error: unsupported color class " + color.getClass().getName());
    }

    public Font getFont() {
        FSFont font = getStyle().getFSFont(getContext());
        if (font instanceof AWTFSFont) {
            return ((AWTFSFont) font).getAWTFonts().get(0);
        }
        return null;
    }

    protected static Integer getLengthValue(CalculatedStyle style, CSSName cssName) {
        FSDerivedValue widthValue = style.valueByName(cssName);
        if (widthValue instanceof LengthValue) {
            return new Integer((int)widthValue.asFloat());
        }

        return null;
    }
}

package com.openhtmltopdf.simple.extend.form;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.RectPropertySet;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.simple.extend.XhtmlForm;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicBorders;

import java.awt.*;

public abstract class AbstractButtonField extends InputField {

    public AbstractButtonField(Element e, XhtmlForm form, LayoutContext context, BlockBox box) {
        super(e, form, context, box);
    }

    protected void applyComponentStyle(JButton button) {

        super.applyComponentStyle(button);

        CalculatedStyle style = getBox().getStyle();
        BorderPropertySet border = style.getBorder(null);
        boolean disableOSBorder = (border.leftStyle() != null && border.rightStyle() != null || border.topStyle() != null || border.bottomStyle() != null);

        FSColor backgroundColor = style.getBackgroundColor();

        //if a border is set or a background color is set, then use a special JButton with the BasicButtonUI.
        if (disableOSBorder || backgroundColor instanceof FSRGBColor) {
            //when background color is set, need to use the BasicButtonUI, certainly when using XP l&f
            BasicButtonUI ui = new BasicButtonUI();
            button.setUI(ui);

            if (backgroundColor instanceof FSRGBColor) {
                FSRGBColor rgb = (FSRGBColor)backgroundColor;
                button.setBackground(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue()));
            }

            if (disableOSBorder)
                button.setBorder(new BasicBorders.MarginBorder());
            else
                button.setBorder(BasicBorders.getButtonBorder());
        }

        Integer paddingTop = getLengthValue(style, CSSName.PADDING_TOP);
        Integer paddingLeft = getLengthValue(style, CSSName.PADDING_LEFT);
        Integer paddingBottom = getLengthValue(style, CSSName.PADDING_BOTTOM);
        Integer paddingRight = getLengthValue(style, CSSName.PADDING_RIGHT);


        int top = paddingTop == null ? 2 : Math.max(2, paddingTop.intValue());
        int left = paddingLeft == null ? 12 : Math.max(12, paddingLeft.intValue());
        int bottom = paddingBottom == null ? 2 : Math.max(2, paddingBottom.intValue());
        int right = paddingRight == null ? 12 : Math.max(12, paddingRight.intValue());

        button.setMargin(new Insets(top, left, bottom, right));

        RectPropertySet padding = style.getCachedPadding();
        padding.setRight(0);
        padding.setLeft(0);
        padding.setTop(0);
        padding.setBottom(0);

        FSDerivedValue widthValue = style.valueByName(CSSName.WIDTH);
        if (widthValue instanceof LengthValue)
            intrinsicWidth = new Integer(getBox().getContentWidth());

        FSDerivedValue heightValue = style.valueByName(CSSName.HEIGHT);
        if (heightValue instanceof LengthValue)
            intrinsicHeight = new Integer(getBox().getHeight());
    }
}

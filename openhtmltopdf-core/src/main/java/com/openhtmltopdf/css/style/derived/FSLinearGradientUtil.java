package com.openhtmltopdf.css.style.derived;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.Idents;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.parser.property.AbstractPropertyBuilder;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FSLinearGradientUtil {
    static float deg2rad(final float deg) {
        return (float) Math.toRadians(deg);
    }

    static boolean isLengthOrPercentage(PropertyValue value) {
        return AbstractPropertyBuilder.isLengthHelper(value) ||
                value.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE;
    }

    static int getStopsStartIndex(List<PropertyValue> params) {
        if (Objects.equals(params.get(0).getStringValue(), "to")) {
            int i = 1;
            while (i < params.size() &&
                    params.get(i).getStringValue() != null &&
                    Idents.looksLikeABGPosition(params.get(i).getStringValue())) {
                i++;
            }

            return i;
        } else {
            return 1;
        }
    }

    static float get100PercentDefaultStopLength(CalculatedStyle style, CssContext ctx, float boxWidth) {
        return LengthValue.calcFloatProportionalValue(style, CSSName.BACKGROUND_IMAGE, "100%",
                100f, CSSPrimitiveValue.CSS_PERCENTAGE, boxWidth, ctx);
    }

    /**
     * Calculates the angle of the linear gradient in degrees.
     */
    static float calculateAngle(List<PropertyValue> params, int stopsStartIndex) {
        if (Objects.equals(params.get(0).getStringValue(), "to")) {
            // The to keyword is followed by one or two position
            // idents (in any order).
            // linear-gradient( to left top, blue, red);
            // linear-gradient( to top right, blue, red);
            List<String> positions = new ArrayList<>(2);

            for (int i = 1; i < stopsStartIndex; i++) {
                positions.add(params.get(i).getStringValue());
            }

            if (positions.contains("top") && positions.contains("left"))
                return 315f;
            else if (positions.contains("top") && positions.contains("right"))
                return 45f;
            else if (positions.contains("bottom") && positions.contains("left"))
                return 225f;
            else if (positions.contains("bottom") && positions.contains("right"))
                return 135f;
            else if (positions.contains("bottom"))
                return 180f;
            else if (positions.contains("left"))
                return 270f;
            else if (positions.contains("right"))
                return 90f;
            else
                return 0f;
        }
        else if (params.get(0).getPrimitiveType() == CSSPrimitiveValue.CSS_DEG)
        {
            // linear-gradient(45deg, ...)
            return params.get(0).getFloatValue();
        }
        else if (params.get(0).getPrimitiveType() == CSSPrimitiveValue.CSS_RAD)
        {
            // linear-gradient(2rad)
            return params.get(0).getFloatValue() * (float) (180 / Math.PI);
        }
        else
        {
            return 0f;
        }
    }
}

package com.openhtmltopdf.newtable;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.EmptyStyle;

/**
 * Class with static attribute which is instance of an EmptyStyle
 * with visibility set to hidden.
 * It is used to fix the bug: danfickle/openhtmltopdf#399
 */
class VisibilityHiddenStyle {
    static CalculatedStyle STYLE;

    static {
        STYLE = new EmptyStyle();
        STYLE.setDefaultValue(CSSName.VISIBILITY, IdentValue.HIDDEN);
    };
}

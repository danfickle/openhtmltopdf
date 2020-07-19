package com.openhtmltopdf.css.sheet;

import java.util.List;

import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.parser.Token;

/**
 * Holds an invalid property declaration (ie. one not understood by
 * this project). Useful for passing to plugins such as SVG.
 *
 * WARNING: This is not a general subclass of PropertyDeclaration, the only
 * method which should be used is toCSS.
 */
public class InvalidPropertyDeclaration extends PropertyDeclaration {

    private final String propertyName;
    private final List<PropertyValue> values;
    private final int order;

    public InvalidPropertyDeclaration(
            String propertyName,
            List<PropertyValue> values,
            int origin,
            boolean important,
            int order) {
        super(null, null, important, origin);
        this.propertyName = propertyName;
        this.values = values;
        this.order = order;
    }

    @Override
    public void toCSS(StringBuilder sb) {
        sb.append(this.propertyName);
        sb.append(':');
        for (PropertyValue value : this.values) {
            if (value.getOperator() == Token.TK_COMMA) {
                sb.append(',');
            } else {
                sb.append(' ');
            }
            sb.append(value.getCssText());
        }
        if (this.isImportant()) {
            sb.append(" !important;\n");
        } else {
            sb.append(';');
            sb.append('\n');
        }
    }

    @Override
    public String getPropertyName() {
        return this.propertyName;
    }

    /**
     * Holds the order so as to recreate a list of invalid and valid
     * properties in their original order.
     */
    public int getOrder() {
        return this.order;
    }
}

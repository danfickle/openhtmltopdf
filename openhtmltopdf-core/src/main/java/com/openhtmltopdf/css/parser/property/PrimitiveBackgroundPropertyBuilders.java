package com.openhtmltopdf.css.parser.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.CSSParseException;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.CSSValue;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.parser.Token;
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders.GenericColor;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.util.WebDoc;
import com.openhtmltopdf.util.WebDocLocations;

@WebDoc(WebDocLocations.CSS_BACKGROUND_PROPERTIES)
public class PrimitiveBackgroundPropertyBuilders {
    private static BitSet setOf(IdentValue... val) {
        return PrimitivePropertyBuilders.setFor(val);
    }

    private abstract static class MultipleBackgroundValueBuilder extends AbstractPropertyBuilder {
        protected abstract List<PropertyValue> processValue(CSSName cssName, PropertyValue value);

        protected List<PropertyValue> processValues(CSSName cssName, PropertyValue val1, PropertyValue val2) {
            return Arrays.asList(val1, val2);
        }

        protected boolean allowsTwoValueItems() {
            return false;
        }

        @Override
        public List<PropertyDeclaration> buildDeclarations(
            CSSName cssName, List<PropertyValue> values, int origin,
            boolean important, boolean inheritAllowed) {

            checkValueCount(cssName, 1, Integer.MAX_VALUE, values.size());

            List<PropertyValue> res;

            if (values.size() == 1) {
                PropertyValue val = values.get(0);
                checkInheritAllowed(val, inheritAllowed);

                if (val.getCssValueType() != CSSValue.CSS_INHERIT) {
                    res = processValue(cssName, val);
                } else {
                    return Collections.singletonList(
                            new PropertyDeclaration(cssName, val, important, origin));
                }
            } else {
                res = new ArrayList<>(values.size());

                for (int i = 0; i < values.size(); i++) {
                    boolean atEnd = i == values.size() - 1;
                    boolean beforeComma = !atEnd && values.get(i + 1).getOperator() == Token.TK_COMMA;

                    PropertyValue val1 = values.get(i);
                    PropertyValue val2 = !atEnd && !beforeComma ? values.get(i + 1) : null;

                    checkForbidInherit(val1);

                    if (val2 == null) {
                        res.addAll(processValue(cssName, val1));
                    } else if (!allowsTwoValueItems()) {
                        checkValueCount(cssName, 1, 2);
                    } else {
                        checkForbidInherit(val2);
                        res.addAll(processValues(cssName, val1, val2));
                        i++;
                    }
                }
            }

            return Collections.singletonList(
                     new PropertyDeclaration(cssName, new PropertyValue(res), important, origin));
        }
    }

    public static class BackgroundImage extends MultipleBackgroundValueBuilder {
        @Override
        protected List<PropertyValue> processValue(CSSName cssName, PropertyValue value) {
            if (value.getPropertyValueType() == PropertyValue.VALUE_TYPE_FUNCTION &&
                Objects.equals(value.getFunction().getName(), "linear-gradient")) {
                // TODO: Validation of linear-gradient args.
                return Collections.singletonList(value);
            } else {
                checkIdentOrURIType(cssName, value);

                if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, setOf(IdentValue.NONE), ident);
                }

                return Collections.singletonList(value);
            }
        }
    }

    public static class BackgroundColor extends GenericColor {
    }

    public static class BackgroundSize extends MultipleBackgroundValueBuilder {
        private static final BitSet ALL_ALLOWED = setOf(
                IdentValue.AUTO, IdentValue.CONTAIN, IdentValue.COVER
        );

        @Override
        protected List<PropertyValue> processValue(CSSName cssName, PropertyValue first) {
            checkIdentLengthOrPercentType(cssName, first);

            if (first.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                IdentValue firstIdent = checkIdent(cssName, first);
                checkValidity(cssName, ALL_ALLOWED, firstIdent);

                assert firstIdent == IdentValue.AUTO ||
                       firstIdent == IdentValue.COVER ||
                       firstIdent == IdentValue.CONTAIN;

                // Items are expected to always return a pair so just repeat the ident.
                return Arrays.asList(first, first);
            } else {
                assert isLength(first) || first.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE;

                return Arrays.asList(first, new PropertyValue(IdentValue.AUTO));
            }
        }

        @Override
        protected List<PropertyValue> processValues(CSSName cssName, PropertyValue first, PropertyValue second) {
            checkIdentLengthOrPercentType(cssName, first);
            checkIdentLengthOrPercentType(cssName, second);

            if (first.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                IdentValue firstIdent = checkIdent(cssName, first);
                if (firstIdent != IdentValue.AUTO) {
                    throw new CSSParseException("The only ident value allowed here is 'auto'", -1);
                }
            } else if (first.getFloatValue() < 0.0f) {
                throw new CSSParseException(cssName + " values cannot be negative", -1);
            }

            if (second.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                IdentValue secondIdent = checkIdent(cssName, second);
                if (secondIdent != IdentValue.AUTO) {
                    throw new CSSParseException("The only ident value allowed here is 'auto'", -1);
                }
            } else if (second.getFloatValue() < 0.0f) {
                throw new CSSParseException(cssName + " values cannot be negative", -1);
            }

            return Arrays.asList(first, second);
        }

        @Override
        protected boolean allowsTwoValueItems() {
            return true;
        }
    }

    public static class BackgroundPosition extends MultipleBackgroundValueBuilder {
        @Override
        protected List<PropertyValue> processValue(CSSName cssName, PropertyValue first) {
            checkIdentLengthOrPercentType(cssName, first);

            if (isLength(first) || first.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) {
                return Arrays.asList(first, createValueForIdent(IdentValue.CENTER));
            }

            assert first.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT;

            IdentValue firstIdent = checkIdent(cssName, first);
            checkValidity(cssName, getAllowed(), firstIdent);

            if (firstIdent == IdentValue.TOP ||
                firstIdent == IdentValue.BOTTOM) {
                return Arrays.asList(
                        createValueForIdent(IdentValue.CENTER),
                        createValueForIdent(firstIdent));
            } else {
                assert firstIdent == IdentValue.CENTER ||
                       firstIdent == IdentValue.LEFT ||
                       firstIdent == IdentValue.RIGHT;

                return Arrays.asList(
                        createValueForIdent(firstIdent),
                        createValueForIdent(IdentValue.CENTER));
            }
        }

        @Override
        protected List<PropertyValue> processValues(CSSName cssName, PropertyValue first, PropertyValue second) {
            checkIdentLengthOrPercentType(cssName, first);
            checkIdentLengthOrPercentType(cssName, second);

            IdentValue firstIdent = null;
            if (first.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                firstIdent = checkIdent(cssName, first);
                checkValidity(cssName, getAllowed(), firstIdent);
            }

            IdentValue secondIdent = null;
            if (second.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                secondIdent = checkIdent(cssName, second);
                checkValidity(cssName, getAllowed(), secondIdent);
            }

            if (firstIdent == null && secondIdent == null) {
                assert isLength(first) || first.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE;
                assert isLength(second) || second.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE;

                return Arrays.asList(first, second);
            } else if (firstIdent != null && secondIdent != null) {
                if (firstIdent == IdentValue.TOP || firstIdent == IdentValue.BOTTOM ||
                    secondIdent == IdentValue.LEFT || secondIdent == IdentValue.RIGHT) {
                    // CSS Standard allows to swap ident order.
                    IdentValue temp = firstIdent;
                    firstIdent = secondIdent;
                    secondIdent = temp;
                }

                // Check that we don't have "left left" or "bottom top"
                checkIdentPosition(cssName, firstIdent, secondIdent);

                assert firstIdent == IdentValue.CENTER ||
                       firstIdent == IdentValue.LEFT ||
                       firstIdent == IdentValue.RIGHT;

                assert secondIdent == IdentValue.CENTER ||
                       secondIdent == IdentValue.TOP ||
                       secondIdent == IdentValue.BOTTOM;

                return Arrays.asList(
                        createValueForIdent(firstIdent),
                        createValueForIdent(secondIdent));
            } else {
                // Check that we don't have "70% left" or "bottom 40%"
                checkIdentPosition(cssName, firstIdent, secondIdent);

                if (firstIdent == null) {
                    assert isLength(first) || first.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE;
                    assert secondIdent != null;

                    return Arrays.asList(first, createValueForIdent(secondIdent));
                } else {
                    assert firstIdent != null;
                    assert isLength(second) || second.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE;

                    return Arrays.asList(createValueForIdent(firstIdent), second);
                }
            }
        }

        @Override
        protected boolean allowsTwoValueItems() {
            return true;
        }

        private void checkIdentPosition(CSSName cssName, IdentValue firstIdent, IdentValue secondIdent) {
            if (firstIdent == IdentValue.TOP || firstIdent == IdentValue.BOTTOM ||
                secondIdent == IdentValue.LEFT || secondIdent == IdentValue.RIGHT) {
                throw new CSSParseException("Invalid combination of keywords in " + cssName, -1);
            }
        }

        private float getPercentForIdent(IdentValue ident) {
            float percent;

            if (ident == IdentValue.CENTER) {
                percent = 50.f;
            } else if (ident == IdentValue.BOTTOM || ident == IdentValue.RIGHT) {
                percent = 100.0f;
            } else {
                assert ident == IdentValue.TOP || ident == IdentValue.LEFT;
                percent = 0.0f;
            }

            return percent;
        }

        private PropertyValue createValueForIdent(IdentValue ident) {
            float percent = getPercentForIdent(ident);
            return new PropertyValue(
                    CSSPrimitiveValue.CSS_PERCENTAGE, percent, percent + "%");
        }

        private BitSet getAllowed() {
            return PrimitivePropertyBuilders.BACKGROUND_POSITIONS;
        }
    }

    private abstract static class MultipleIdentValue extends MultipleBackgroundValueBuilder {
        @Override
        protected List<PropertyValue> processValue(CSSName cssName, PropertyValue value) {
            checkIdentType(cssName, value);
            IdentValue ident = checkIdent(cssName, value);

            checkValidity(cssName, getAllowed(), ident);

            return Collections.singletonList(value);
        }

        protected abstract BitSet getAllowed();
    }

    public static class BackgroundRepeat extends MultipleIdentValue {
        @Override
        protected BitSet getAllowed() {
            return PrimitivePropertyBuilders.BACKGROUND_REPEATS;
        }
    }

    public static class BackgroundAttachment extends MultipleIdentValue {
        @Override
        protected BitSet getAllowed() {
            return PrimitivePropertyBuilders.BACKGROUND_ATTACHMENTS;
        }
    }
}

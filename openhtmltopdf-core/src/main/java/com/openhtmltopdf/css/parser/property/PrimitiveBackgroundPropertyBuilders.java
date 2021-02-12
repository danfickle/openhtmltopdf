package com.openhtmltopdf.css.parser.property;

import java.util.ArrayList;
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
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders.GenericColor;
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders.GenericURIWithNone;
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders.SingleIdent;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;

public class PrimitiveBackgroundPropertyBuilders {
    public static class BackgroundImage extends GenericURIWithNone {
        @Override
        public List<PropertyDeclaration> buildDeclarations(
            CSSName cssName, List<PropertyValue> values, int origin,
            boolean important, boolean inheritAllowed) {

            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);

            if (value.getPropertyValueType() == PropertyValue.VALUE_TYPE_FUNCTION &&
                Objects.equals(value.getFunction().getName(), "linear-gradient")) {
                // TODO: Validation of linear-gradient args.
                return Collections.singletonList(
                        new PropertyDeclaration(cssName, value, important, origin));
            } else {
                return super.buildDeclarations(cssName, values, origin, important, inheritAllowed);
            }
        }
    }

    public static class BackgroundColor extends GenericColor {
    }

    public static class BackgroundSize extends AbstractPropertyBuilder {
        private static final BitSet ALL_ALLOWED = PrimitivePropertyBuilders.setFor(new IdentValue[] {
                IdentValue.AUTO, IdentValue.CONTAIN, IdentValue.COVER
        });

        @Override
        public List<PropertyDeclaration> buildDeclarations(CSSName cssName, List<PropertyValue> values, int origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, 2, values.size());

            PropertyValue first = values.get(0);
            PropertyValue second = null;
            if (values.size() == 2) {
                second = values.get(1);
            }

            checkInheritAllowed(first, inheritAllowed);
            if (values.size() == 1 &&
                    first.getCssValueType() == CSSValue.CSS_INHERIT) {
                return Collections.singletonList(
                        new PropertyDeclaration(cssName, first, important, origin));
            }

            if (second != null) {
                checkInheritAllowed(second, false);
            }

            checkIdentLengthOrPercentType(cssName, first);
            if (second == null) {
                if (first.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                    IdentValue firstIdent = checkIdent(cssName, first);
                    checkValidity(cssName, ALL_ALLOWED, firstIdent);

                    if (firstIdent == IdentValue.CONTAIN || firstIdent == IdentValue.COVER) {
                        return Collections.singletonList(
                                new PropertyDeclaration(cssName, first, important, origin));
                    } else {
                        return PrimitivePropertyBuilders.createTwoValueResponse(CSSName.BACKGROUND_SIZE, first, first, origin, important);
                    }
                } else {
                    return PrimitivePropertyBuilders.createTwoValueResponse(CSSName.BACKGROUND_SIZE, first, new PropertyValue(IdentValue.AUTO), origin, important);
                }
            } else {
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

                return PrimitivePropertyBuilders.createTwoValueResponse(CSSName.BACKGROUND_SIZE, first, second, origin, important);
            }
        }

    }

    public static class BackgroundPosition extends AbstractPropertyBuilder {
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, int origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, 2, values.size());

            PropertyValue first = values.get(0);
            PropertyValue second = null;
            if (values.size() == 2) {
                second = values.get(1);
            }

            checkInheritAllowed(first, inheritAllowed);
            if (values.size() == 1 &&
                    first.getCssValueType() == CSSValue.CSS_INHERIT) {
                return Collections.singletonList(
                        new PropertyDeclaration(cssName, first, important, origin));
            }

            if (second != null) {
                checkInheritAllowed(second, false);
            }

            checkIdentLengthOrPercentType(cssName, first);
            if (second == null) {
                if (isLength(first) || first.getPrimitiveType() == CSSPrimitiveValue.CSS_PERCENTAGE) {
                    List<PropertyValue> responseValues = new ArrayList<>(2);
                    responseValues.add(first);
                    responseValues.add(new PropertyValue(
                            CSSPrimitiveValue.CSS_PERCENTAGE, 50.0f, "50%"));
                    return Collections.singletonList(new PropertyDeclaration(
                                CSSName.BACKGROUND_POSITION,
                                new PropertyValue(responseValues), important, origin));
                }
            } else {
                checkIdentLengthOrPercentType(cssName, second);
            }


            IdentValue firstIdent = null;
            if (first.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                firstIdent = checkIdent(cssName, first);
                checkValidity(cssName, getAllowed(), firstIdent);
            }

            IdentValue secondIdent = null;
            if (second == null) {
                secondIdent = IdentValue.CENTER;
            } else if (second.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                secondIdent = checkIdent(cssName, second);
                checkValidity(cssName, getAllowed(), secondIdent);
            }

            if (firstIdent == null && secondIdent == null) {
                return Collections.singletonList(new PropertyDeclaration(
                        CSSName.BACKGROUND_POSITION, new PropertyValue(values), important, origin));
            } else if (firstIdent != null && secondIdent != null) {
                if (firstIdent == IdentValue.TOP || firstIdent == IdentValue.BOTTOM ||
                        secondIdent == IdentValue.LEFT || secondIdent == IdentValue.RIGHT) {
                    IdentValue temp = firstIdent;
                    firstIdent = secondIdent;
                    secondIdent = temp;
                }

                checkIdentPosition(cssName, firstIdent, secondIdent);

                return createTwoPercentValueResponse(
                        getPercentForIdent(firstIdent),
                        getPercentForIdent(secondIdent),
                        important,
                        origin);
            } else {
                checkIdentPosition(cssName, firstIdent, secondIdent);

                List<PropertyValue> responseValues = new ArrayList<>(2);

                if (firstIdent == null) {
                    responseValues.add(first);
                    responseValues.add(createValueForIdent(secondIdent));
                } else {
                    responseValues.add(createValueForIdent(firstIdent));
                    responseValues.add(second);
                }

                return Collections.singletonList(new PropertyDeclaration(
                        CSSName.BACKGROUND_POSITION,
                        new PropertyValue(responseValues), important, origin));
            }
        }

        private void checkIdentPosition(CSSName cssName, IdentValue firstIdent, IdentValue secondIdent) {
            if (firstIdent == IdentValue.TOP || firstIdent == IdentValue.BOTTOM ||
                    secondIdent == IdentValue.LEFT || secondIdent == IdentValue.RIGHT) {
                throw new CSSParseException("Invalid combination of keywords in " + cssName, -1);
            }
        }

        private float getPercentForIdent(IdentValue ident) {
            float percent = 0.0f;

            if (ident == IdentValue.CENTER) {
                percent = 50.f;
            } else if (ident == IdentValue.BOTTOM || ident == IdentValue.RIGHT) {
                percent = 100.0f;
            }

            return percent;
        }

        private PropertyValue createValueForIdent(IdentValue ident) {
            float percent = getPercentForIdent(ident);
            return new PropertyValue(
                    CSSPrimitiveValue.CSS_PERCENTAGE, percent, percent + "%");
        }

        private List<PropertyDeclaration> createTwoPercentValueResponse(
                float percent1, float percent2, boolean important, int origin) {
            PropertyValue value1 = new PropertyValue(
                    CSSPrimitiveValue.CSS_PERCENTAGE, percent1, percent1 + "%");
            PropertyValue value2 = new PropertyValue(
                    CSSPrimitiveValue.CSS_PERCENTAGE, percent2, percent2 + "%");

            List<PropertyValue> values = new ArrayList<>(2);
            values.add(value1);
            values.add(value2);

            PropertyDeclaration result = new PropertyDeclaration(
                    CSSName.BACKGROUND_POSITION,
                    new PropertyValue(values), important, origin);

            return Collections.singletonList(result);
        }

        private BitSet getAllowed() {
            return PrimitivePropertyBuilders.BACKGROUND_POSITIONS;
        }
    }

    public static class BackgroundRepeat extends SingleIdent {
        @Override
        protected BitSet getAllowed() {
            return PrimitivePropertyBuilders.BACKGROUND_REPEATS;
        }
    }

    public static class BackgroundAttachment extends SingleIdent {
        @Override
        protected BitSet getAllowed() {
            return PrimitivePropertyBuilders.BACKGROUND_ATTACHMENTS;
        }
    }
}

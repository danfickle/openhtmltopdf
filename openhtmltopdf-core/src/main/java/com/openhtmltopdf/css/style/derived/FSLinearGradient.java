package com.openhtmltopdf.css.style.derived;

import java.util.ArrayList;
import java.util.List;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSFunction;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.parser.property.Conversions;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;

public class FSLinearGradient {

    /**
     * A stop point which does not yet have a length.
     * We need all the stop points first before we can calculate
     * a length for intermediate stop points without a length.
     */
    private static class IntermediateStopPoint {
        private final FSColor _color;

        IntermediateStopPoint(FSColor color) {
            _color = color;
        }

        public FSColor getColor() {
            return _color;
        }
    }

    public static class StopPoint extends IntermediateStopPoint {
        private final float _length;

        StopPoint(FSColor color, float length) {
            super(color);
            this._length = length;
        }

        public float getLength() {
            return _length;
        }

        @Override
        public String toString() {
            return "StopPoint [length=" + _length +
            ", color=" + getColor() + "]";
        }
    }

    private final List<StopPoint> _stopPoints;
    private final float _angle;
    private int x1;
    private int x2;
    private int y1;
    private int y2;

    public FSLinearGradient(CalculatedStyle style, FSFunction function, int boxWidth, int boxHeight, CssContext ctx) {
        List<PropertyValue> params = function.getParameters();
        int stopsStartIndex = FSLinearGradientUtil.getStopsStartIndex(params);

        float prelimAngle = FSLinearGradientUtil.calculateAngle(params, stopsStartIndex);
        prelimAngle = prelimAngle % 360f;
        if (prelimAngle < 0) {
            prelimAngle += 360f;
        }

        this._angle = prelimAngle;
        this._stopPoints = calculateStopPoints(params, style, ctx, boxWidth, stopsStartIndex);
        endPointsFromAngle(_angle, boxWidth, boxHeight);
    }

    // Compute the endpoints so that a gradient of the given angle
	// covers a box of the given size.
	// From: https://github.com/WebKit/webkit/blob/master/Source/WebCore/css/CSSGradientValue.cpp
    private void endPointsFromAngle(float angleDeg, final int w, final int h) {
        if (angleDeg == 0) {
            x1 = 0;
            y1 = h;

            x2 = 0;
            y2 = 0;
            return;
        }

        if (angleDeg == 90) {
            x1 = 0;
            y1 = 0;

            x2 = w;
            y2 = 0;
            return;
        }

        if (angleDeg == 180) {
            x1 = 0;
            y1 = 0;

            x2 = 0;
            y2 = h;
            return;
        }

        if (angleDeg == 270) {
            x1 = w;
            y1 = 0;

            x2 = 0;
            y2 = 0;
            return;
        }

        // angleDeg is a "bearing angle" (0deg = N, 90deg = E),
        // but tan expects 0deg = E, 90deg = N.
        final float slope = (float) Math.tan(FSLinearGradientUtil.deg2rad(90 - angleDeg));

        // We find the endpoint by computing the intersection of the line formed by the
        // slope,
        // and a line perpendicular to it that intersects the corner.
        final float perpendicularSlope = -1 / slope;

        // Compute start corner relative to center, in Cartesian space (+y = up).
        final float halfHeight = h / 2;
        final float halfWidth = w / 2;
        float xEnd, yEnd;

        if (angleDeg < 90) {
            xEnd = halfWidth;
            yEnd = halfHeight;
        } else if (angleDeg < 180) {
            xEnd = halfWidth;
            yEnd = -halfHeight;
        } else if (angleDeg < 270) {
            xEnd = -halfWidth;
            yEnd = -halfHeight;
        } else {
            xEnd = -halfWidth;
            yEnd = halfHeight;
        }

        // Compute c (of y = mx + c) using the corner point.
        final float c = yEnd - perpendicularSlope * xEnd;
        final float endX = c / (slope - perpendicularSlope);
        final float endY = perpendicularSlope * endX + c;

        // We computed the end point, so set the second point,
        // taking into account the moved origin and the fact that we're in drawing space
        // (+y = down).
        x2 = (int) (halfWidth + endX);
        y2 = (int) (halfHeight - endY);

        // Reflect around the center for the start point.
        x1 = (int) (halfWidth - endX);
        y1 = (int) (halfHeight + endY);
    }

    private List<StopPoint> calculateStopPoints(
        List<PropertyValue> params, CalculatedStyle style, CssContext ctx, float boxWidth, int stopsStartIndex) {

        List<IntermediateStopPoint> points = new ArrayList<>();

        for (int i = stopsStartIndex; i < params.size();) {
            PropertyValue value = params.get(i);
            FSColor color;

            if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                color = Conversions.getColor(value.getStringValue());
            } else {
                color = value.getFSColor();
            }

            if (i + 1 < params.size() && FSLinearGradientUtil.isLengthOrPercentage(params.get(i + 1))) {

                PropertyValue lengthValue = params.get(i + 1);
                float length = LengthValue.calcFloatProportionalValue(style, CSSName.BACKGROUND_IMAGE, "",
                        lengthValue.getFloatValue(), lengthValue.getPrimitiveType(), boxWidth, ctx);
                points.add(new StopPoint(color, length));
                i += 2;
            } else {
                points.add(new IntermediateStopPoint(color));
                i += 1;
            }
        }

        List<StopPoint> ret = new ArrayList<>(points.size());

        for (int i = 0; i < points.size(); i++) {
            IntermediateStopPoint pt = points.get(i);
            boolean intermediate = pt.getClass() == IntermediateStopPoint.class;
            
            if (!intermediate) {
                ret.add((StopPoint) pt);
            } else if (i == 0) {
                ret.add(new StopPoint(pt.getColor(), 0f));
            } else if (i == points.size() - 1) {
                float len = FSLinearGradientUtil.get100PercentDefaultStopLength(style, ctx, boxWidth);
                ret.add(new StopPoint(pt.getColor(), len));
            } else {
                // Poo, we've got a length-less stop in the middle.
                // Lets say we have linear-gradient(to right, red, blue 10px, orange, yellow, black 100px, purple):
                // In this case because orange and yellow don't have lengths we have to devide the difference
                // between them. So difference = 90px and there are 3 color changes means that the interval
                // will be 30px and that orange will be at 40px and yellow at 70px.
                int nextWithLengthIndex = getNextStopPointWithLengthIndex(points, i + 1);
                int prevWithLengthIndex = getPrevStopPointWithLengthIndex(points, i - 1);

                float nextLength = nextWithLengthIndex == -1 ?
                                    FSLinearGradientUtil.get100PercentDefaultStopLength(style, ctx, boxWidth) :
                                    ((StopPoint) points.get(nextWithLengthIndex)).getLength();

                float prevLength = prevWithLengthIndex == -1 ? 0 :
                                    ((StopPoint) points.get(prevWithLengthIndex)).getLength();

                float range = nextLength - prevLength;

                int topRangeIndex = nextWithLengthIndex == -1 ? points.size() - 1 : nextWithLengthIndex;
                int bottomRangeIndex = prevWithLengthIndex == -1 ? 0 : prevWithLengthIndex;
                
                int rangeCount = (topRangeIndex - bottomRangeIndex) + 1;
                int thisCount = i - bottomRangeIndex;

                // rangeCount should never be zero.
                if (rangeCount != 0) {
                    float interval = range / rangeCount;
                    float thisLength = prevLength + (interval * thisCount);
                    ret.add(new StopPoint(pt.getColor(), thisLength));
                }
            }
        }

        return ret;
    }

    private int getPrevStopPointWithLengthIndex(List<IntermediateStopPoint> points, int maxIndex) {
        for (int i = maxIndex; i >= 0; i--) {
            if (isStopPointWithLength(points.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isStopPointWithLength(IntermediateStopPoint pt) {
        return pt.getClass() == StopPoint.class;
    }

    private int getNextStopPointWithLengthIndex(List<IntermediateStopPoint> points, int startIndex) {
        for (int i = startIndex; i < points.size(); i++) {
            if (isStopPointWithLength(points.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public List<StopPoint> getStopPoints() {
        return _stopPoints;
    }

    /**
     * The angle of this linear gradient in compass degrees.
     */
    public float getAngle() {
        return _angle;
    }

    public int getX1() {
        return x1;
    }

    public int getX2() {
        return x2;
    }

    public int getY1() {
        return y1;
    }

    public int getY2() {
        return y2;
    }

    @Override
    public String toString() {
        return "FSLinearGradient [_angle=" + _angle + ", _stopPoints=" + _stopPoints + ", x1=" + x1 + ", x2=" + x2
                + ", y1=" + y1 + ", y2=" + y2 + "]";
    }
}

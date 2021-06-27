package com.openhtmltopdf.css.constants;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a partial list of common SVG properties that are not present in
 * the HTML renderer of this project. This list is here so we can suppress
 * warnings for these properties.
 * 
 * List from:
 * https://css-tricks.com/svg-properties-and-css/
 */
public enum SVGProperty {
    CLIP,
    CLIP_PATH,
    CLIP_RULE,
    MASK,
    FILTER,
    STOP_COLOR,
    STOP_OPACITY,
    FILL,
    FILL_RULE,
    FILL_OPACITY,
    MARKER,
    MARKER_START,
    MARKER_MID,
    MARKER_END,
    STROKE,
    STROKE_DASHARRAY,
    STROKE_DASHOFFSET,
    STROKE_LINECAP,
    STROKE_LINEJOIN,
    STROKE_MITERLIMIT,
    STROKE_OPACITY,
    STROKE_WIDTH,
    SHAPE_RENDERING;

    private static final Set<String> _set =
            Arrays.stream(values())
                  .map(v -> v.name().toLowerCase(Locale.US).replace('_', '-'))
                  .collect(Collectors.toSet());

    public static Set<String> properties() {
        return _set;
    }
}

package com.openhtmltopdf.outputdevice.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.openhtmltopdf.css.constants.IdentValue;

public class FontFamily<T extends MinimalFontDescription> {
    private final List<T> _fontDescriptions = new ArrayList<>(4);
    private final String _family;

    public FontFamily(String family) {
        this._family = family;
    }

    public String getFamily() {
        return _family;
    }

    public List<T> getFontDescriptions() {
        return _fontDescriptions;
    }

    public void addFontDescription(T descr) {
        /*
         * NOTE: Font is prepended, so newer fonts can override predecessors.
         */
        _fontDescriptions.add(0, descr);
        Collections.sort(_fontDescriptions, Comparator.comparing(T::getWeight));
    }

    public T match(int desiredWeight, IdentValue style) {
        if (_fontDescriptions.isEmpty()) {
            return null;
        } else if (_fontDescriptions.size() == 1) {
            return _fontDescriptions.get(0);
        }

        List<T> candidates = new ArrayList<>(_fontDescriptions.size());

        // First try only matching style.
        getStyleMatches(style, candidates);

        // Then try changing italic to oblique.
        if (candidates.isEmpty() && style == IdentValue.ITALIC) {
            getStyleMatches(IdentValue.OBLIQUE, candidates);
        }

        // Then try changing oblique to normal.
        if (candidates.isEmpty() && style == IdentValue.OBLIQUE) {
            getStyleMatches(IdentValue.NORMAL, candidates);
        }

        // Still nothing!
        if (candidates.isEmpty()) {
             candidates.addAll(_fontDescriptions);
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        T result = findByWeight(candidates, desiredWeight, SM_EXACT);

        if (result != null) {
            return result;
        } else {
            if (desiredWeight <= 500) {
                return findByWeight(candidates, desiredWeight, SM_LIGHTER_OR_DARKER);
            } else {
                return findByWeight(candidates, desiredWeight, SM_DARKER_OR_LIGHTER);
            }
        }
    }

    private void getStyleMatches(IdentValue style, List<T> candidates) {
        for (T description : _fontDescriptions) {
            if (description.getStyle() == style) {
                candidates.add(description);
            }
        }
    }

    private static final int SM_EXACT = 1;
    private static final int SM_LIGHTER_OR_DARKER = 2;
    private static final int SM_DARKER_OR_LIGHTER = 3;

    private T findByWeight(List<T> matches, int desiredWeight, int searchMode) {
        if (searchMode == SM_EXACT) {
        	for (T descr : matches) {
                if (descr.getWeight() == desiredWeight) {
                    return descr;
                }
            }
            return null;
        } else if (searchMode == SM_LIGHTER_OR_DARKER){
            int offset = 0;
            T descr = null;
            for (offset = 0; offset < matches.size(); offset++) {
                descr = matches.get(offset);
                if (descr.getWeight() > desiredWeight) {
                    break;
                }
            }

            if (offset > 0 && descr.getWeight() > desiredWeight) {
                return matches.get(offset - 1);
            } else {
                return descr;
            }

        } else if (searchMode == SM_DARKER_OR_LIGHTER) {
            int offset = 0;
            T descr = null;
            for (offset = matches.size() - 1; offset >= 0; offset--) {
                descr = matches.get(offset);
                if (descr.getWeight() < desiredWeight) {
                    break;
                }
            }

            if (offset != matches.size() - 1 && descr.getWeight() < desiredWeight) {
                return matches.get(offset + 1);
            } else {
                return descr;
            }
        }

        return null;
    }
}

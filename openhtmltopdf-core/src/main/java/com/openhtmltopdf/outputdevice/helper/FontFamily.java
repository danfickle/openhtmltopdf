package com.openhtmltopdf.outputdevice.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.openhtmltopdf.css.constants.IdentValue;

public class FontFamily<T extends MinimalFontDescription> {
    private List<T> _fontDescriptions;

    public FontFamily() {
    }

    public List<T> getFontDescriptions() {
        return _fontDescriptions;
    }

    public void addFontDescription(T descr) {
        if (_fontDescriptions == null) {
            _fontDescriptions = new ArrayList<T>();
        }
        _fontDescriptions.add(descr);
        Collections.sort(_fontDescriptions,
                new Comparator<T>() {
                    public int compare(T o1, T o2) {
                        return o1.getWeight() - o2.getWeight();
                    }
        });
    }
    
    public void setName(String fontFamilyName) {
    }

    public T match(int desiredWeight, IdentValue style) {
        if (_fontDescriptions == null) {
            throw new RuntimeException("fontDescriptions is null");
        }

        List<T> candidates = new ArrayList<T>();

        for (T description : _fontDescriptions) {
            if (description.getStyle() == style) {
                candidates.add(description);
            }
        }

        if (candidates.size() == 0) {
            if (style == IdentValue.ITALIC) {
                return match(desiredWeight, IdentValue.OBLIQUE);
            } else if (style == IdentValue.OBLIQUE) {
                return match(desiredWeight, IdentValue.NORMAL);
            } else {
                candidates.addAll(_fontDescriptions);
            }
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

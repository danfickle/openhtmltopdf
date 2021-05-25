package com.openhtmltopdf.layout.counter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.parser.CounterData;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.layout.LayoutContext;

public class CounterContext implements AbstractCounterContext {
    private final Map<String, Integer> _counters = new HashMap<>();
    /**
     * This is different because it needs to work even when the counter- properties cascade
     * and it should also logically be redefined on each level (think list-items within list-items)
     */
    private CounterContext _parent;

    /**
     * A CounterContext should really be reflected in the element hierarchy, but CalculatedStyles
     * reflect the ancestor hierarchy just as well and also handles pseudo-elements seamlessly.
     */
    public CounterContext(LayoutContext ctx, CalculatedStyle style, Integer startIndex) {
        // Numbering restarted via <ol start="x">
        if (startIndex != null) {
            _counters.put("list-item", startIndex);
        }
        _parent = ctx._counterContextMap.get(style.getParent());
        if (_parent == null) _parent = new CounterContext();//top-level context, above root element
        //first the explicitly named counters
        List<CounterData> resets = style.getCounterReset();
        if (resets != null) {
            resets.forEach(_parent::resetCounter);
        }

        List<CounterData> increments = style.getCounterIncrement();
        if (increments != null) {
            for (CounterData cd : increments) {
                if (!_parent.incrementCounter(cd)) {
                    _parent.resetCounter(new CounterData(cd.getName(), 0));
                    _parent.incrementCounter(cd);
                }
            }
        }

        // then the implicit list-item counter
        if (style.isIdent(CSSName.DISPLAY, IdentValue.LIST_ITEM)) {
            // Numbering restarted via <li value="x">
            if (startIndex != null) {
                _parent._counters.put("list-item", startIndex);
            }
            _parent.incrementListItemCounter(1);
        }
    }

    private CounterContext() {

    }

    /**
     * @param cd
     * @return true if a counter was found and incremented
     */
    private boolean incrementCounter(CounterData cd) {
        // list-item is a reserved name for list-item counter in CSS3
        if ("list-item".equals(cd.getName())) {
            incrementListItemCounter(cd.getValue());
            return true;
        } else {
            Integer currentValue = _counters.get(cd.getName());
            if (currentValue == null) {
                if (_parent == null) {
                    return false;
                }
                return _parent.incrementCounter(cd);
            } else {
                _counters.put(cd.getName(), currentValue + cd.getValue());
                return true;
            }
        }
    }

    private void incrementListItemCounter(int increment) {
        Integer currentValue = _counters.get("list-item");
        if (currentValue == null) {
            currentValue = 0;
        }
        _counters.put("list-item", currentValue + increment);
    }

    private void resetCounter(CounterData cd) {
        _counters.put(cd.getName(), cd.getValue());
    }

    @Override
    public int getCurrentCounterValue(String name) {
        //only the counters of the parent are in scope
        //_parent is never null for a publicly accessible CounterContext
        Integer value = _parent.getCounter(name);
        if (value == null) {
            _parent.resetCounter(new CounterData(name, 0));
            return 0;
        } else {
            return value.intValue();
        }
    }

    private Integer getCounter(String name) {
        Integer value = _counters.get(name);
        if (value != null) return value;
        if (_parent == null) return null;
        return _parent.getCounter(name);
    }

    @Override
    public List<Integer> getCurrentCounterValues(String name) {
        //only the counters of the parent are in scope
        //_parent is never null for a publicly accessible CounterContext
        List<Integer> values = new ArrayList<>();
        _parent.getCounterValues(name, values);
        if (values.size() == 0) {
            _parent.resetCounter(new CounterData(name, 0));
            values.add(Integer.valueOf(0));
        }
        return values;
    }

    private void getCounterValues(String name, List<Integer> values) {
        if (_parent != null) _parent.getCounterValues(name, values);
        Integer value = _counters.get(name);
        if (value != null) values.add(value);
    }
}

package com.openhtmltopdf.layout.counter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.css.parser.CounterData;
import com.openhtmltopdf.css.style.CalculatedStyle;

public class RootCounterContext implements AbstractCounterContext {
    private final Map<String, Integer> counterMap = new HashMap<>();

    public void resetCounterValue(CalculatedStyle style) {
        List<CounterData> resets = style.getCounterReset();
        if (resets != null) {
            resets.forEach(cd -> counterMap.put(cd.getName(), cd.getValue()));
        }
    }

    public void incrementCounterValue(CalculatedStyle style) {
        List<CounterData> incs = style.getCounterIncrement();
        if (incs != null) {
            for (CounterData cd : incs) {
                counterMap.merge(cd.getName(), cd.getValue(), (old, newVal) -> old + newVal);
            }
        }
    }

    @Override
    public int getCurrentCounterValue(String name) {
        Integer current = counterMap.get(name);

        if (current != null) {
            return current;
        } else {
            counterMap.put(name, 0);
            return 0;
        }
    }

    @Override
    public List<Integer> getCurrentCounterValues(String name) {
        return Collections.singletonList(getCurrentCounterValue(name));
    }
}

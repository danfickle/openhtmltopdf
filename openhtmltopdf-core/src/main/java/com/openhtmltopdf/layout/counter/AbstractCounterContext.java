package com.openhtmltopdf.layout.counter;

import java.util.List;

public interface AbstractCounterContext {

    int getCurrentCounterValue(String name);

    List<Integer> getCurrentCounterValues(String name);

}

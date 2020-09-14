package com.openhtmltopdf.util;

import java.util.ArrayList;
import java.util.List;

class LogMessageIdFormat {

    private static final Object PLACEHOLDER = new Object();


    private final List<Object> tokens;

    LogMessageIdFormat(String message) {
        this.tokens = prepareFormatter(message);
    }

    private List<Object> prepareFormatter(String messageFormat) {
        List<Object> v = new ArrayList<>();
        int idx = 0;
        while(true) {
            int newIdx = messageFormat.indexOf("{}", idx);
            String messageSegment = newIdx == -1 ? messageFormat.substring(idx) : messageFormat.substring(idx, newIdx);
            if (!messageSegment.isEmpty()) {
                v.add(messageSegment);
            }
            if (newIdx == -1) {
                break;
            }
            idx = newIdx + 2;
            v.add(PLACEHOLDER);
        }
        return v;
    }

    String formatMessage(Object[] args) {
        StringBuilder sb = new StringBuilder();
        int argsLength = args == null ? 0 : args.length;
        int size = tokens.size();
        int argsUse = 0;
        for (int i = 0; i < size; i++) {
            Object f = tokens.get(i);
            if (f == PLACEHOLDER) {
                Object argument = argsUse < argsLength ? args[argsUse] : "";
                sb.append(argument);
                argsUse++;
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }
}

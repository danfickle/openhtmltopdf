package com.openhtmltopdf.util;

import java.util.function.Predicate;

public class LambdaUtil {
    private LambdaUtil() { }
    
    public static <T> Predicate<T> alwaysTrue() {
        return (unused) -> true;
    }
    
    public static <T> Predicate<T> alwaysFalse() {
        return (unused) -> false;
    }
}

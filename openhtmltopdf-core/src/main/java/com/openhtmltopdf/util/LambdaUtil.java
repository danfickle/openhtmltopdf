package com.openhtmltopdf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.render.Box;

public class LambdaUtil {
    private LambdaUtil() { }

    /**
     * Null-safe way to look up the ancestor tree as a stream.
     * Does not include starting box.
     * Will return a empty stream if either bx or bx.getParent() is null.
     */
    public static Stream<Box> ancestors(Box bx) {
        List<Box> list = new ArrayList<>();
        while (bx != null && bx.getParent() != null) {
            list.add(bx.getParent());
            bx = bx.getParent();
        }
        return list.stream();
    }

    /**
     * Null-safe identity hash code.
     * Debug only.
     */
    public static String objectId(Object obj) {
        return obj == null ? "null" : Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * Null-safe box description including object id, layer, containing layer and impl class.
     * Debug only.
     */
    public static String boxDescription(Box box) {
        if (box != null) {
            return String.format("%s => %s, layer = %s, containing = %s, class = %s", 
              objectId(box), box, objectId(box.getLayer()), objectId(box.getContainingLayer()), box.getClass().getName());
        }

        return "[null box]";
    }

    /**
     * Null-safe layer description including object id, toString and master box description.
     * Debug only.
     */
    public static String layerDescription(Layer layer) {
        if (layer != null) {
            return String.format(
                    "%s, master = %s, parent = %s",
                    objectId(layer), boxDescription(layer.getMaster()), objectId(layer.getParent()));
        }

        return "[null layer]";
    }

    /**
     * Ancestor dump to string including starting box and referenced layers.
     * Debug only.
     */
    public static String ancestorDump(Box bx) {
        return
           boxDescription(bx) +
           ancestors(bx)
             .map(LambdaUtil::boxDescription)
             .collect(Collectors.joining("\n    ", "\nANCESTORS = [\n    ", "\n]")) +
           Stream.concat(
               Stream.of(bx.getLayer(), bx.getContainingLayer()),
               ancestors(bx).flatMap(box -> Stream.of(box.getLayer(), box.getContainingLayer()))
           ).filter(Objects::nonNull)
            .distinct()
            .map(LambdaUtil::layerDescription)
            .collect(Collectors.joining("\n    ", "\nREFED LAYERS = [\n    ", "\n]"));
    }

    public static <T> Predicate<T> alwaysTrue() {
        return (unused) -> true;
    }

    public static <T> Predicate<T> alwaysFalse() {
        return (unused) -> false;
    }
}

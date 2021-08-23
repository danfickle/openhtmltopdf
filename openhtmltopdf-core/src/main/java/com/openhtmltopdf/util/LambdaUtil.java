package com.openhtmltopdf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.openhtmltopdf.layout.Layer;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.InlineLayoutBox;

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
     * A stream of all descendant boxes not including
     * InlineText or InlineBox objects.
     *
     * This would usually only be called after layout is concluded
     * as InlineBox objects are converted to one or more InlineLayoutBox
     * during layout.
     * 
     * Should be in breadth first order.
     */
    public static Stream<Box> descendants(Box parent) {
        return StreamSupport.stream(new DescendantBoxSpliterator(parent), false);
    }

    /**
     * See {@link #descendants(Box)}
     */
    public static List<Box> descendantsList(Box parent) {
        return descendants(parent).collect(Collectors.toList());
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

    private static class DescendantContent {
        final Object object;
        final int indent;

        DescendantContent(Object obj, int indent) {
            this.object = obj;
            this.indent = indent;
        }
    }

    private static void descendantDump(Box parent, int indent, List<DescendantContent> out) {
        out.add(new DescendantContent(parent, indent));

        indent++;

        for (Box child : parent.getChildren()) {
            descendantDump(child, indent, out);
        }

        if (parent instanceof BlockBox &&
            ((BlockBox) parent).getInlineContent() != null) {
            for (Object child : ((BlockBox) parent).getInlineContent()) {
                if (child instanceof Box) {
                    descendantDump((Box) child, indent, out);
                } else {
                    out.add(new DescendantContent(child, indent));
                }
            }
        }

        if (parent instanceof InlineLayoutBox) {
            for (Object child : ((InlineLayoutBox) parent).getInlineChildren()) {
                if (child instanceof Box) {
                    descendantDump((Box) child, indent, out);
                } else {
                    out.add(new DescendantContent(child, indent));
                }
            }
        }
    }

    /**
     * Creates an indented dump of the box tree.
     * Includes both the pre-layout inline-content and if layout
     * has been run, the after layout structure.
     * Debug only.
     */
    public static String descendantDump(Box root) {
        StringBuilder spaces = new StringBuilder(100);
        IntStream.range(0, 100).forEach(unused -> spaces.append(' '));
        char[] space = new char[100];
        spaces.getChars(0, spaces.length(), space, 0);

        StringBuilder sb = new StringBuilder();
        List<DescendantContent> renderObjects = new ArrayList<>();

        descendantDump(root, 0, renderObjects);

        for (DescendantContent content : renderObjects) {
            sb.append(space, 0, Math.min(100, content.indent * 4));
            sb.append(content.object.toString() + " => " + Integer.toHexString(System.identityHashCode(content.object)));
            sb.append('\n');
        }

        return sb.toString();
    }

    public static <T> Predicate<T> alwaysTrue() {
        return (unused) -> true;
    }

    public static <T> Predicate<T> alwaysFalse() {
        return (unused) -> false;
    }
}

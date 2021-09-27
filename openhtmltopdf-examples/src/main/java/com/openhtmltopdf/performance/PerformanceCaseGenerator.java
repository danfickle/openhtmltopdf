package com.openhtmltopdf.performance;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.openhtmltopdf.layout.BlockBoxing;

public class PerformanceCaseGenerator {
    private static final String LOREM = 
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Etiam vulputate, nibh eget convallis vestibulum, ex ipsum ullamcorper ligula, " +
            "eget bibendum nulla massa vel metus. Duis sed nunc ornare, convallis purus at, " +
            "fringilla ipsum. Quisque ullamcorper hendrerit ipsum at eleifend. Orci varius " +
            "natoque penatibus et magnis dis parturient montes, nascetur ridiculus.";

    private static String join(String hdr, String repeat, String ftr, int howMany) {
        return IntStream.range(0, howMany)
                .mapToObj(i -> repeat)
                .collect(Collectors.joining("\n", hdr, ftr));
    }

    public static String paragraphs(int howMany) {
        final String hdr = "<html><head><style>p { font-family: sans-serif; }</style></head><body>";
        final String paragraph = "<p>" + LOREM + "</p>";
        final String ftr = "</body></html>";

        return join(hdr, paragraph, ftr, howMany);
    }

    public static String tableRows(int howMany) {
        final String hdr = "<html><head><style>tr:nth-child(odd) > td { background-color: orange; } table { border-collapse: collapse; }" +
                           "</style></head><body><table>";
        final String tr = "<tr><td>One</td><td>Two</td><td>Three</td></tr>";
        final String ftr = "</table></body></html>";

        return join(hdr, tr, ftr, howMany);
    }

    /**
     * Performance of {@link BlockBoxing}
     */
    public static String pageBreakAvoidBlocks(int howMany) {
        final String hdr = "<html><head><style>" +
                "div { page-break-before: avoid; page-break-after: avoid; page-break-inside: avoid; height: 40px; }" +
                "div:nth-child(10) { page-break-before: auto; }" +
                "</style></head><body>";
        final String block = "<div></div>";
        final String ftr = "</body></html>";

        return join(hdr, block, ftr, howMany);
    }

    /**
     * Performance of {@link BlockBoxing}
     */
    public static String blocks(int howMany) {
        final String hdr = "<html><head><style>" +
                "div { height: 40px; }" +
                "</style></head><body>";
        final String block = "<div></div>";
        final String ftr = "</body></html>";

        return join(hdr, block, ftr, howMany);
    }

    /**
     * Performace case for:
     *   Issue 396 - CSS border-radius makes pdf rendering very slow.
     * Caused by Area constructor being very slow (according to VisualVM). 
     *
     */
    public static String borderRadius(int howMany) {
        final String hdr = "<html><head><style>div.avatar { margin-left: auto; margin-right: auto; height: 200px; width: 160px; " +
                           "background-size: cover; background-position: center center; background-repeat: no-repeat; " +
                           "border-radius: 4px;" + 
                           "background-image: url(demos/images/flyingsaucer.png); }" +
                           "</style></head><body>";
        final String div = "<div class=\"avatar\"></div>";
        final String ftr = "</body></html>";

        return join(hdr, div, ftr, howMany);
    }

}

package com.openhtmltopdf.performance;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerformanceCaseGenerator {
    private static final String LOREM = 
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Etiam vulputate, nibh eget convallis vestibulum, ex ipsum ullamcorper ligula, " +
            "eget bibendum nulla massa vel metus. Duis sed nunc ornare, convallis purus at, " +
            "fringilla ipsum. Quisque ullamcorper hendrerit ipsum at eleifend. Orci varius " +
            "natoque penatibus et magnis dis parturient montes, nascetur ridiculus.";
    
    
    public static String paragraphs(int howMany) {
        final String hdr = "<html><head><style>p { font-family: sans-serif; }</style></head><body>";
        final String paragraph = "<p>" + LOREM + "</p>";
        final String ftr = "</body></html>";
               
        return IntStream.range(0, howMany)
                        .mapToObj(i -> paragraph)
                        .collect(Collectors.joining("\n", hdr, ftr));
    }
    
    public static String tableRows(int howMany) {
        final String hdr = "<html><head><style>tr:nth-child(odd) > td { background-color: orange; } table { border-collapse: collapse; }" +
                           "</style></head><body><table>";
        final String tr = "<tr><td>One</td><td>Two</td><td>Three</td></tr>";
        final String ftr = "</table></body></html>";
               
        return IntStream.range(0, howMany)
                        .mapToObj(i -> tr)
                        .collect(Collectors.joining("\n", hdr, ftr));
    }

}

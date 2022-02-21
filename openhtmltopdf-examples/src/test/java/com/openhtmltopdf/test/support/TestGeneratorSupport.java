package com.openhtmltopdf.test.support;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.openhtmltopdf.visualtest.VisualTester.BuilderConfig;

public class TestGeneratorSupport {
    private static final String LITERAL_REPLACE = "\n\r\t\'\"\\\b\f";
    private static final String LITERAL_WITH = "nrt'\"\\bf";

    public static String escapeJavaStringLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        // None of the bad chars are valid trailing surrogates in UTF16 so
        // we should be right in using chars here rather than codePoints.
        s.chars().forEachOrdered(c -> {
            int idx = LITERAL_REPLACE.indexOf(c);

            if (idx != -1) {
                sb.append('\\')
                  .append(LITERAL_WITH.charAt(idx));
            } else {
                sb.append((char) c);
            }
        });

        return sb.toString();
    }

    /**
     * Converts first char to upper case, not to be used
     * with surrogate/supplementary chars.
     */
    public static String upperCaseFirst(String s) {
        if (s.isEmpty() || Character.isUpperCase(s.charAt(0))) {
            return s;
        }

        char first = Character.toUpperCase(s.charAt(0));

        return s.length() == 1 ?
            Character.toString(first) :
            first + s.substring(1);
    }

    /**
     * Converts camel-case to PascalCase.
     * Useful for converting resource name to part of method name.
     */
    public static String kebabToPascal(String kebab) {
        String fileParts[] = kebab.split(Pattern.quote("-"));
        return Arrays.stream(fileParts)
                .map(TestGeneratorSupport::upperCaseFirst)
                .collect(Collectors.joining());
    }

    public static final BuilderConfig EMPTY_BUILDER = (builder) -> {};

    public static class TestWithBuilder {
        public final String resource;
        public final BuilderConfig configurator;

        TestWithBuilder(String resourceName, BuilderConfig configurator) {
            this.resource = resourceName;
            this.configurator = configurator;
        }

        public static TestWithBuilder makeTestWBuilder(String resourceName, BuilderConfig configurator) {
            return new TestWithBuilder(resourceName, configurator);
        }

        public static TestWithBuilder makeTest(String resourceName) {
            return new TestWithBuilder(resourceName, EMPTY_BUILDER);
        }
    }
}

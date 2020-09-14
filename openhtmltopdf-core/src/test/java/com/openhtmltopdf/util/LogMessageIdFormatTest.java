package com.openhtmltopdf.util;

import org.junit.Assert;
import org.junit.Test;

public class LogMessageIdFormatTest {

    @Test
    public void testFormats() {
        Assert.assertEquals("", new LogMessageIdFormat("").formatMessage(new Integer[]{1,2,3}));
        Assert.assertEquals("abcd", new LogMessageIdFormat("abcd").formatMessage(null));
        Assert.assertEquals("1a2b3", new LogMessageIdFormat("1{}2{}3").formatMessage(new String[]{"a", "b"}));
        Assert.assertEquals("123", new LogMessageIdFormat("{}{}{}").formatMessage(new Integer[]{1,2,3}));
        Assert.assertEquals("A1a2b3B", new LogMessageIdFormat("{}1{}2{}3{}").formatMessage(new String[]{"A", "a", "b", "B"}));

        Assert.assertEquals("A123", new LogMessageIdFormat("{}1{}2{}3{}").formatMessage(new String[]{"A"}));

        Assert.assertEquals("", new LogMessageIdFormat("{}{}{}").formatMessage(new Integer[]{}));
        Assert.assertEquals("", new LogMessageIdFormat("{}{}{}").formatMessage(null));
    }
}

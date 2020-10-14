package com.openhtmltopdf.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author schrader
 */
public class OpenUtilTest {

    @Test
    public void areAllCharactersPrintable() {
        String text = "abc 123 \uD844\uDCC1";
        boolean printable = OpenUtil.areAllCharactersPrintable(text);
        assertThat(printable, is(true));
    }

}
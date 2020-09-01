package com.openhtmltopdf.extend;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class TextRendererTest {

    @Test
    public void getEffectivePrintableString() {
        assertThat(TextRenderer.getEffectivePrintableString(null), nullValue());
        assertThat(TextRenderer.getEffectivePrintableString(""), is(""));

        assertThat(TextRenderer.getEffectivePrintableString("abc"), is("abc"));

        assertThat(TextRenderer.getEffectivePrintableString("ab\u00adc"), is("abc"));
    }
}

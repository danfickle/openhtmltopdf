package com.openhtmltopdf.util;

import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

import com.openhtmltopdf.swing.NaiveUserAgent;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

public class DataUriTest {

    @Test
    public void testGetEmbeddedBase64Image() throws IOException {
        String onePixel = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAACXBIWXMAAC4jAAAuIwF4pT92AAAADElEQVQI12P4//8/AAX+Av7czFnnAAAAAElFTkSuQmCC";

        byte[] result = NaiveUserAgent.getEmbeddedBase64Image(onePixel);

        assertThat(result, notNullValue());
        assertThat(result.length, equalTo(90));
    }
}

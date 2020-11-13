package com.openhtmltopdf.util;

import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;

public class ImageUtilTest {

    @Test
    public void testGetEmbeddedBase64Image() {
        String onePixel = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAACXBIWXMAAC4jAAAuIwF4pT92AAAADElEQVQI12P4//8/AAX+Av7czFnnAAAAAElFTkSuQmCC";
        byte[] result = ImageUtil.getEmbeddedBase64Image(onePixel);
        assertThat(result, notNullValue());
    }
}

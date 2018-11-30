package com.openhtmltopdf.util;

import java.awt.image.BufferedImage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.junit.Test;
import org.junit.Assert;

public class ImageUtilTest {

    @Test
    public void testGetEmbeddedBase64Image() {
        String onePixel = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAACXBIWXMAAC4jAAAuIwF4pT92AAAADElEQVQI12P4//8/AAX+Av7czFnnAAAAAElFTkSuQmCC";
        byte[] result = ImageUtil.getEmbeddedBase64Image(onePixel);
        Assert.assertThat(result, notNullValue());
    }

    @Test
    public void testLoadEmbeddedBase64Image() {
        String onePixel = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAACXBIWXMAAC4jAAAuIwF4pT92AAAADElEQVQI12P4//8/AAX+Av7czFnnAAAAAElFTkSuQmCC";
        BufferedImage result = ImageUtil.loadEmbeddedBase64Image(onePixel);
        Assert.assertThat(result.getHeight(), is(1));
        Assert.assertThat(result.getWidth(), is(1));
    }

}

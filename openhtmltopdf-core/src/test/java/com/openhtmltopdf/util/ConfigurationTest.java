package com.openhtmltopdf.util;

import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigurationTest {

    @Test
    public void testValueFor() {
        String key = "xr.test-config-byte";
        String value = Configuration.valueFor(key);
        assertThat(value, is("8"));
    }

    @Test
    public void testValueAsByte() {
        String key = "xr.test-config-byte";
        int value = Configuration.valueAsByte(key, (byte) 0);
        assertThat(value, is(8));
    }

    @Test
    public void testValueAsShort() {
        String key = "xr.test-config-short";
        int value = Configuration.valueAsShort(key, (short) 0);
        assertThat(value, is(16));
    }

    @Test
    public void testValueAsInt() {
        String key = "xr.test-config-int";
        int value = Configuration.valueAsInt(key, (int) 0);
        assertThat(value, is(100));
    }

    @Test
    public void testValueAsLong() {
        String key = "xr.test-config-long";
        long value = Configuration.valueAsLong(key, (long) 0);
        assertThat(value, is(2000L));
    }

    @Test
    public void testValueAsFloat() {
        String key = "xr.test-config-float";
        float value = Configuration.valueAsFloat(key, (float) 0);
        assertThat(value, is(3000.25F));
    }

    @Test
    public void testValueAsDouble() {
        String key = "xr.test-config-double";
        double value = Configuration.valueAsDouble(key, (double) 0);
        assertThat(value, is(4000.50D));
    }

    @Test
    public void testIsTrue() {
        String key = "xr.test-config-boolean";
        boolean value = Configuration.isTrue(key, false);
        assertThat(value, is(true));
    }

    @Test
    public void testIsFalse() {
        String key = "xr.test-config-boolean";
        boolean value = Configuration.isFalse(key, true);
        assertThat(value, is(false));
    }

}

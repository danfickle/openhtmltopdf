package com.openhtmltopdf.util;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;

public class ConfigurationTest {

    @Test
    public void testValueFor() {
        String key = "xr.test-config-byte";
        String value = Configuration.valueFor(key);
        assertThat(value, CoreMatchers.is("8"));
    }

    @Test
    public void testValueAsByte() {
        String key = "xr.test-config-byte";
        int value = Configuration.valueAsByte(key, (byte) 0);
        assertThat(value, CoreMatchers.is(8));
    }

    @Test
    public void testValueAsShort() {
        String key = "xr.test-config-short";
        int value = Configuration.valueAsShort(key, (short) 0);
        assertThat(value, CoreMatchers.is(16));
    }

    @Test
    public void testValueAsInt() {
        String key = "xr.test-config-int";
        int value = Configuration.valueAsInt(key, 0);
        assertThat(value, CoreMatchers.is(100));
    }

    @Test
    public void testValueAsLong() {
        String key = "xr.test-config-long";
        long value = Configuration.valueAsLong(key, 0l);
        assertThat(value, CoreMatchers.is(2000L));
    }

    @Test
    public void testValueAsFloat() {
        String key = "xr.test-config-float";
        float value = Configuration.valueAsFloat(key, 0f);
        assertThat(value, CoreMatchers.is(3000.25F));
    }

    @Test
    public void testValueAsDouble() {
        String key = "xr.test-config-double";
        double value = Configuration.valueAsDouble(key, 0d);
        assertThat(value, CoreMatchers.is(4000.50D));
    }

    @Test
    public void testIsTrue() {
        String key = "xr.test-config-boolean";
        boolean value = Configuration.isTrue(key, false);
        assertThat(value, CoreMatchers.is(true));
    }

    @Test
    public void testIsFalse() {
        String key = "xr.test-config-boolean";
        boolean value = Configuration.isFalse(key, true);
        assertThat(value, CoreMatchers.is(false));
    }

}

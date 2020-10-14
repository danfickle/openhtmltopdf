package com.openhtmltopdf.resource;

import java.io.InputStream;
import java.util.Map;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

public class FSEntityResolverTest {

    @Test
    public void testResolveEntity() throws Exception {
        FSEntityResolver instance = FSEntityResolver.instance();
        String publicId = "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN";
        InputSource resolvedEntity = instance.resolveEntity(publicId, null);
        try (InputStream in = resolvedEntity.getByteStream()) {
            assertThat(in, notNullValue());
        }
    }

    @Test
    public void testGetEntities() {
        FSEntityResolver instance = FSEntityResolver.instance();
        Map<String, String> entities = instance.getEntities();
        String publicId = "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN";
        Assert.assertTrue(entities.containsKey(publicId));
    }

}

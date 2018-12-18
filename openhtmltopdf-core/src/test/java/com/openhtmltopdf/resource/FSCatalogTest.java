package com.openhtmltopdf.resource;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class FSCatalogTest {

    @Test
    public void testParseCatalog() {
        FSCatalog instance = new FSCatalog();
        String catalogUri = "/resources/schema/openhtmltopdf/catalog-special.xml";
        Map<String, String> catalog = instance.parseCatalog(catalogUri);
        String publicId = "-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN";
        Assert.assertTrue(catalog.containsKey(publicId));
    }

}

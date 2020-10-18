package com.openhtmltopdf.objects.pdf;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice;
import com.openhtmltopdf.render.RenderingContext;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class PdfDrawerBase implements FSObjectDrawer
{
    private final Map<PDFBoxDeviceReference, SoftReference<Map<String, PDFormXObject>>> formMap = new HashMap<PDFBoxDeviceReference, SoftReference<Map<String, PDFormXObject>>>();

    protected PDFormXObject importPageAsXForm(RenderingContext ctx, Element e,
            PdfBoxOutputDevice pdfBoxOutputDevice, LayerUtility layerUtility) throws IOException
    {

        Map<String, PDFormXObject> map = getFormCacheMap(pdfBoxOutputDevice);
        int pdfpage = getPageNumber(e);
        String pdfsrc = e.getAttribute("pdfsrc");
        String url = ctx.getUac().resolveURI(pdfsrc);

        PDFormXObject pdFormXObject = map.get(url);
        if (pdFormXObject == null)
        {
            try (InputStream inputStream = new URL(url).openStream())
            {
                PDFParser pdfParser = new PDFParser(new RandomAccessBuffer(inputStream));
                pdfParser.parse();
                pdFormXObject = layerUtility
                        .importPageAsForm(pdfParser.getPDDocument(), pdfpage - 1);
                pdfParser.getPDDocument().close();
            }
            map.put(url, pdFormXObject);
        }
        return pdFormXObject;
    }

    protected Map<String, PDFormXObject> getFormCacheMap(PdfBoxOutputDevice pdfBoxOutputDevice)
    {
        SoftReference<Map<String, PDFormXObject>> mapWeakReference = formMap
                .get(new PDFBoxDeviceReference(pdfBoxOutputDevice));
        Map<String, PDFormXObject> map = null;
        if (mapWeakReference != null)
            map = mapWeakReference.get();
        if (map == null)
        {
            map = new HashMap<String, PDFormXObject>();
            formMap.put(new PDFBoxDeviceReference(pdfBoxOutputDevice),
                    new SoftReference<Map<String, PDFormXObject>>(map));
        }
        return map;
    }

    protected int getPageNumber(Element e)
    {
        String pdfpageValue = e.getAttribute("pdfpage");
        if (pdfpageValue == null || pdfpageValue.isEmpty())
            pdfpageValue = "1";
        return Integer.parseInt(pdfpageValue);
    }

    private static class PDFBoxDeviceReference extends WeakReference<PdfBoxOutputDevice>
    {
        PDFBoxDeviceReference(PdfBoxOutputDevice referent)
        {
            super(referent);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof PDFBoxDeviceReference)
            {
                return ((PDFBoxDeviceReference) obj).get() == get();
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode()
        {
            PdfBoxOutputDevice pdfBoxOutputDevice = get();
            if (pdfBoxOutputDevice != null)
                return pdfBoxOutputDevice.hashCode();
            return 0;
        }
    }
}

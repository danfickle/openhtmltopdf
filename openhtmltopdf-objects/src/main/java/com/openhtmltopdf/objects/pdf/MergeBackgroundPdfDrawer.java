package com.openhtmltopdf.objects.pdf;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice;
import com.openhtmltopdf.render.RenderingContext;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.w3c.dom.Element;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MergeBackgroundPdfDrawer implements FSObjectDrawer {

	private final Map<WeakReference<PdfBoxOutputDevice>, SoftReference<Map<String, PDFormXObject>>> formMap = new HashMap<WeakReference<PdfBoxOutputDevice>, SoftReference<Map<String, PDFormXObject>>>();
	private AtomicInteger counter = new AtomicInteger(0);

	@Override
	public Map<Shape, String> drawObject(Element e, double x, double y, double width, double height,
			OutputDevice outputDevice, RenderingContext ctx, int dotsPerPixel) {

		/*
		 * We can only do something if this is a PDF.
		 */
		if (!(outputDevice instanceof PdfBoxOutputDevice))
			return null;

		String pdfsrc = e.getAttribute("pdfsrc");
		String pdfpageValue = e.getAttribute("pdfpage");
		if (pdfpageValue == null || pdfpageValue.isEmpty())
			pdfpageValue = "1";
		int pdfpage = Integer.parseInt(pdfpageValue);

		PdfBoxOutputDevice pdfBoxOutputDevice = (PdfBoxOutputDevice) outputDevice;
		String url = ctx.getUac().resolveURI(pdfsrc);

		SoftReference<Map<String, PDFormXObject>> mapWeakReference = formMap.get(pdfBoxOutputDevice);
		Map<String, PDFormXObject> map = null;
		if (mapWeakReference != null)
			map = mapWeakReference.get();
		if (map == null) {
			map = new HashMap<String, PDFormXObject>();
			formMap.put(new WeakReference<PdfBoxOutputDevice>(pdfBoxOutputDevice),
					new SoftReference<Map<String, PDFormXObject>>(map));
		}
		try {
			PDFormXObject pdFormXObject = map.get(url);
			if (pdFormXObject == null) {
				InputStream inputStream = new URL(url).openStream();
				try {
					PDFParser pdfParser = new PDFParser(new RandomAccessBuffer(inputStream));
					pdfParser.parse();
					LayerUtility layerUtility = new LayerUtility(pdfBoxOutputDevice.getWriter());
					pdFormXObject = layerUtility.importPageAsForm(pdfParser.getPDDocument(), pdfpage - 1);
				} finally {
					inputStream.close();
				}
				map.put(url, pdFormXObject);
			}

			pdfBoxOutputDevice.getCurrentPage().saveGraphics();
			pdfBoxOutputDevice.getCurrentPage().placeXForm(0, 0, pdFormXObject);
			pdfBoxOutputDevice.getCurrentPage().restoreGraphics();

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return null;
	}
}

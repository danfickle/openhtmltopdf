package com.openhtmltopdf.objects.pdf;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Charsets;
import org.w3c.dom.Element;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice;
import com.openhtmltopdf.render.RenderingContext;

public class MergeBackgroundPdfDrawer implements FSObjectDrawer {
	private final Map<PDFBoxDeviceReference, SoftReference<Map<String, PDFormXObject>>> formMap = new HashMap<PDFBoxDeviceReference, SoftReference<Map<String, PDFormXObject>>>();

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

		SoftReference<Map<String, PDFormXObject>> mapWeakReference = formMap
				.get(new PDFBoxDeviceReference(pdfBoxOutputDevice));
		Map<String, PDFormXObject> map = null;
		if (mapWeakReference != null)
			map = mapWeakReference.get();
		if (map == null) {
			map = new HashMap<String, PDFormXObject>();
			formMap.put(new PDFBoxDeviceReference(pdfBoxOutputDevice),
					new SoftReference<Map<String, PDFormXObject>>(map));
		}
		try {
			PDFormXObject pdFormXObject = map.get(url);
			LayerUtility layerUtility = new LayerUtility(pdfBoxOutputDevice.getWriter());
			if (pdFormXObject == null) {
				InputStream inputStream = new URL(url).openStream();
				try {
					PDFParser pdfParser = new PDFParser(new RandomAccessBuffer(inputStream));
					pdfParser.parse();
					pdFormXObject = layerUtility.importPageAsForm(pdfParser.getPDDocument(), pdfpage - 1);
					pdfParser.getPDDocument().close();
				} finally {
					inputStream.close();
				}
				map.put(url, pdFormXObject);
			}
			PDPage page = pdfBoxOutputDevice.getPage();

			/*
			 * This ensures that the Contents of the page is a COSArray. The first entry in
			 * the array is just a save state (e.g. 'q'). We can override it to add the
			 * XForm.
			 */
			layerUtility.wrapInSaveRestore(page);
			COSArray cosArray = (COSArray) page.getCOSObject().getDictionaryObject(COSName.CONTENTS);
			COSStream saveStateAndPlacePageBackgroundStream = (COSStream) cosArray.get(0);
			OutputStream saveAndPlaceStream = saveStateAndPlacePageBackgroundStream.createOutputStream();
			saveAndPlaceStream.write("q\n".getBytes(Charsets.US_ASCII));
			COSName name = page.getResources().add(pdFormXObject);
			name.writePDF(saveAndPlaceStream);
			saveAndPlaceStream.write(' ');
			saveAndPlaceStream.write("Do\n".getBytes(Charsets.US_ASCII));
			saveAndPlaceStream.write("Q\n".getBytes(Charsets.US_ASCII));
			saveAndPlaceStream.write("q\n".getBytes(Charsets.US_ASCII));
			saveAndPlaceStream.close();

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return null;
	}

	private static class PDFBoxDeviceReference extends WeakReference<PdfBoxOutputDevice> {
		PDFBoxDeviceReference(PdfBoxOutputDevice referent) {
			super(referent);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PDFBoxDeviceReference) {
				return ((PDFBoxDeviceReference) obj).get() == get();
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			PdfBoxOutputDevice pdfBoxOutputDevice = get();
			if (pdfBoxOutputDevice != null)
				return pdfBoxOutputDevice.hashCode();
			return 0;
		}
	}
}

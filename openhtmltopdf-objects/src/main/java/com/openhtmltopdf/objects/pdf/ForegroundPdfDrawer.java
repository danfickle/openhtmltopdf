package com.openhtmltopdf.objects.pdf;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.w3c.dom.Element;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

public class ForegroundPdfDrawer extends PdfDrawerBase
{
    @Override
    public Map<Shape, String> drawObject(Element e, double x, double y, double width, double height,
            OutputDevice outputDevice, RenderingContext ctx, int dotsPerPixel)
    {

        /*
         * We can only do something if this is a PDF.
         */
        if (!(outputDevice instanceof PdfBoxOutputDevice))
            return null;

        PdfBoxOutputDevice pdfBoxOutputDevice = (PdfBoxOutputDevice) outputDevice;

        try
        {
            LayerUtility layerUtility = new LayerUtility(pdfBoxOutputDevice.getWriter());
            PDFormXObject pdFormXObject = importPageAsXForm(ctx, e, pdfBoxOutputDevice,
                    layerUtility);
            PDPage page = pdfBoxOutputDevice.getPage();

            /*
             * This ensures that the Contents of the page is a COSArray. The first entry in
             * the array is just a save state (e.g. 'q'), the last one is just a restore 'Q'.
             * We can override that to add the XForm.
             */
            layerUtility.wrapInSaveRestore(page);
            COSArray cosArray = (COSArray) page.getCOSObject()
                    .getDictionaryObject(COSName.CONTENTS);

            COSStream restoreStateAndPlaceWatermark = (COSStream) cosArray.get(cosArray.size() - 1);
            OutputStream watermarkOutputStream = restoreStateAndPlaceWatermark.createOutputStream();
            watermarkOutputStream.write("Q\nq\n".getBytes(StandardCharsets.US_ASCII));
            COSName name = page.getResources().add(pdFormXObject);
            name.writePDF(watermarkOutputStream);
            watermarkOutputStream.write(' ');
            watermarkOutputStream.write("Do\n".getBytes(StandardCharsets.US_ASCII));
            watermarkOutputStream.write("Q\n".getBytes(StandardCharsets.US_ASCII));
            watermarkOutputStream.close();
        }
        catch (IOException e1)
        {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_MESSAGE, "Error while drawing with the ForegroundPdfDrawer ", e1);
        }
        return null;
    }
}

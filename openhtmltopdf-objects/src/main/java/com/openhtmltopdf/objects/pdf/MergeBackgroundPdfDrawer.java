package com.openhtmltopdf.objects.pdf;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.w3c.dom.Element;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.pdfboxout.PdfBoxOutputDevice;
import com.openhtmltopdf.render.RenderingContext;

public class MergeBackgroundPdfDrawer extends PdfDrawerBase
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
            PDFormXObject pdFormXObject = importPageAsXForm(ctx,e, pdfBoxOutputDevice, layerUtility);
            PDPage page = pdfBoxOutputDevice.getPage();

            /*
             * This ensures that the Contents of the page is a COSArray. The first entry in
             * the array is just a save state (e.g. 'q'). We can override it to add the
             * XForm.
             */
            layerUtility.wrapInSaveRestore(page);
            COSArray cosArray = (COSArray) page.getCOSObject()
                    .getDictionaryObject(COSName.CONTENTS);
            COSStream saveStateAndPlacePageBackgroundStream = (COSStream) cosArray.get(0);
            OutputStream saveAndPlaceStream = saveStateAndPlacePageBackgroundStream
                    .createOutputStream();
            saveAndPlaceStream.write("q\n".getBytes(StandardCharsets.US_ASCII));
            COSName name = page.getResources().add(pdFormXObject);
            name.writePDF(saveAndPlaceStream);
            saveAndPlaceStream.write(' ');
            saveAndPlaceStream.write("Do\n".getBytes(StandardCharsets.US_ASCII));
            saveAndPlaceStream.write("Q\n".getBytes(StandardCharsets.US_ASCII));
            saveAndPlaceStream.write("q\n".getBytes(StandardCharsets.US_ASCII));
            saveAndPlaceStream.close();

        }
        catch (IOException e1)
        {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_MESSAGE, "Error while drawing with the MergeBackgroundPdfDrawer ", e1);
        }
        return null;
    }

}

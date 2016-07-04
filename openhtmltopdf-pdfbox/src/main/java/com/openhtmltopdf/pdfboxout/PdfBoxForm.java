package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSArrayList;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionResetForm;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionSubmitForm;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.w3c.dom.Element;

import com.openhtmltopdf.css.parser.FSCMYKColor;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;


public class PdfBoxForm {
    private final Element element;
    private final List<ControlFontPair> controls = new ArrayList<PdfBoxForm.ControlFontPair>();
    private final List<String> controlNames = new ArrayList<String>();
    private final List<Control> submits = new ArrayList<PdfBoxForm.Control>(2);
    
    public static class Control {
        public final Box box;
        private final PDPage page;
        private final AffineTransform transform;
        private final RenderingContext c;
        private final float pageHeight;
        
        public Control(Box box, PDPage page, AffineTransform transform, RenderingContext c, float pageHeight) {
            this.box = box;
            this.page = page;
            this.transform = transform;
            this.c = c;
            this.pageHeight = pageHeight;
        }
    }
    
    private static class ControlFontPair {
        private final String fontName;
        private final Control control;
        
        private ControlFontPair(Control control, String fontName) {
            this.control = control;
            this.fontName = fontName;
        }
    }
    
    private PdfBoxForm(Element element) {
        this.element = element;
    }
    
    public static PdfBoxForm createForm(Element e) {
        return new PdfBoxForm(e);
    }

    public void addControl(Control ctrl, String fontName) {
        controls.add(new ControlFontPair(ctrl, fontName));
    }
    
    private static Integer getNumber(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static String getColorOperator(FSColor color) {
        String colorOperator = "";
        
        if (color instanceof FSRGBColor) {
            FSRGBColor rgb = (FSRGBColor) color;
            float r = (float) rgb.getRed() / 255;
            float g = (float) rgb.getGreen() / 255;
            float b = (float) rgb.getBlue() / 255;
            
            colorOperator =
                    String.format(Locale.US, "%.4f", r) + ' ' + 
                    String.format(Locale.US, "%.4f", g) + ' ' + 
                    String.format(Locale.US, "%.4f", b) + ' ' +
                    "rg";
        } else if (color instanceof FSCMYKColor) {
            FSCMYKColor cmyk = (FSCMYKColor) color;
            float c = cmyk.getCyan();
            float m = cmyk.getMagenta();
            float y = cmyk.getYellow();
            float k = cmyk.getBlack();
            
            colorOperator = 
                    String.format(Locale.US, "%.4f", c) + ' ' +
                    String.format(Locale.US, "%.4f", m) + ' ' +
                    String.format(Locale.US, "%.4f", y) + ' ' +
                    String.format(Locale.US, "%.4f", k) + ' ' +
                    "k";
        }
        
        return colorOperator;
    }
    
    private String getTextareaText(Element e) {
        return DOMUtil.getText(e);
    }
    
    private String populateOptions(Element e, List<String> labels, List<String> values) {
        List<Element> opts = DOMUtil.getChildren(e, "option");
        String selected = "";
        
        for (Element opt : opts) {
            String label = DOMUtil.getText(opt);
            labels.add(label);
            
            if (opt.hasAttribute("value")) {
                values.add(opt.getAttribute("value"));
            } else {
                values.add(label);
            }
            
            if (selected.isEmpty()) {
                selected = label;
            }
            
            if (opt.hasAttribute("selected")) {
                selected = label;
            }
        }
        
        return selected;
    }
    
    private void processSelectControl(ControlFontPair pair, Control ctrl, PDAcroForm acro, int i, Box root, PdfBoxOutputDevice od) throws IOException {
        PDComboBox field = new PDComboBox(acro);
        
        field.setPartialName("OpenHTMLCtrl" + i);
        controlNames.add("OpenHTMLCtrl" + i);
        
        field.setMappingName(ctrl.box.getElement().getAttribute("name")); // Export name.
        
        List<String> labels = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        String selectedLabel = populateOptions(ctrl.box.getElement(), labels, values);
        
        field.setOptions(values, labels);
        field.setValue(selectedLabel);
        field.setDefaultValue(selectedLabel);
        
        String fontInstruction = "/" + pair.fontName + " 0 Tf";
        field.setDefaultAppearance(fontInstruction);
        
        if (ctrl.box.getElement().hasAttribute("required")) {
            field.setRequired(true);
        }
        
        if (ctrl.box.getElement().hasAttribute("readonly")) {
            field.setReadOnly(true);
        }
        
        if (ctrl.box.getElement().hasAttribute("title")) {
            field.setAlternateFieldName(ctrl.box.getElement().getAttribute("title"));
        }
        
        PDAnnotationWidget widget = field.getWidgets().get(0);

        Rectangle2D rect2D = PdfBoxLinkManager.createTargetArea(ctrl.c, ctrl.box, ctrl.pageHeight, ctrl.transform, root, od);
        PDRectangle rect = new PDRectangle((float) rect2D.getMinX(), (float) rect2D.getMinY(), (float) rect2D.getWidth(), (float) rect2D.getHeight());

        widget.setRectangle(rect);
        widget.setPage(ctrl.page);
        widget.setPrinted(true);
      
        ctrl.page.getAnnotations().add(widget);
        acro.getFields().add(field);
    }
    
    private void processTextControl(ControlFontPair pair, Control ctrl, PDAcroForm acro, int i, Box root, PdfBoxOutputDevice od) throws IOException {
        PDTextField field = new PDTextField(acro);
        
        FSColor color = ctrl.box.getStyle().getColor();
        String colorOperator = getColorOperator(color);

        String fontInstruction = "/" + pair.fontName + " 0 Tf";
        field.setDefaultAppearance(fontInstruction + ' ' + colorOperator);
        
        field.setPartialName("OpenHTMLCtrl" + i); // Internal name.
        controlNames.add("OpenHTMLCtrl" + i);

        String value = ctrl.box.getElement().getNodeName().equals("textarea") ?
                getTextareaText(ctrl.box.getElement()) :
                ctrl.box.getElement().getAttribute("value");
        
        field.setDefaultValue(value); // The reset value.
        field.setValue(value);        // The original value.
    
        if (getNumber(ctrl.box.getElement().getAttribute("max-length")) != null) {
            field.setMaxLen(getNumber(ctrl.box.getElement().getAttribute("max-length")));
        }
        
        if (ctrl.box.getElement().hasAttribute("required")) {
            field.setRequired(true);
        }
        
        if (ctrl.box.getElement().hasAttribute("readonly")) {
            field.setReadOnly(true);
        }
        
        if (ctrl.box.getElement().getNodeName().equals("textarea")) {
            field.setMultiline(true);
        } else if (ctrl.box.getElement().getAttribute("type").equals("password")) {
            field.setPassword(true);
        }
        
        field.setMappingName(ctrl.box.getElement().getAttribute("name")); // Export name.
        
        if (ctrl.box.getElement().hasAttribute("title")) {
            field.setAlternateFieldName(ctrl.box.getElement().getAttribute("title"));
        }
        
        PDAnnotationWidget widget = field.getWidgets().get(0);

        Rectangle2D rect2D = PdfBoxLinkManager.createTargetArea(ctrl.c, ctrl.box, ctrl.pageHeight, ctrl.transform, root, od);
        PDRectangle rect = new PDRectangle((float) rect2D.getMinX(), (float) rect2D.getMinY(), (float) rect2D.getWidth(), (float) rect2D.getHeight());

        widget.setRectangle(rect);
        widget.setPage(ctrl.page);
        widget.setPrinted(true);
      
        ctrl.page.getAnnotations().add(widget);
        acro.getFields().add(field);
    }
    
    private void processSubmitControl(PDAcroForm acro, int i, Control ctrl, Box root, PdfBoxOutputDevice od) throws IOException {
        final int FLAG_USE_GET = 1 << 3;
        final int FLAG_USE_HTML_SUBMIT = 1 << 2;
        
        PDPushButton btn = new PDPushButton(acro);
        btn.setPushButton(true);
        btn.setPartialName("OpenHTMLCtrl" + i);
        
        PDAnnotationWidget widget = btn.getWidgets().get(0);
        
        Rectangle2D rect2D = PdfBoxLinkManager.createTargetArea(ctrl.c, ctrl.box, ctrl.pageHeight, ctrl.transform, root, od);
        PDRectangle rect = new PDRectangle((float) rect2D.getMinX(), (float) rect2D.getMinY(), (float) rect2D.getWidth(), (float) rect2D.getHeight());

        widget.setRectangle(rect);
        widget.setPage(ctrl.page);

        COSArrayList<String> fieldsToInclude = new COSArrayList<String>();
        fieldsToInclude.addAll(controlNames);
        
        if (ctrl.box.getElement().getAttribute("type").equals("reset")) {
            PDActionResetForm reset = new PDActionResetForm();
            reset.setFields(fieldsToInclude.toList());
            widget.setAction(reset);;
        } else {
            PDFileSpecification fs = PDFileSpecification.createFS(new COSString(element.getAttribute("action")));
            PDActionSubmitForm submit = new PDActionSubmitForm();
            
            submit.setFields(fieldsToInclude.toList());
            submit.setFile(fs);

            if (!element.getAttribute("method").equalsIgnoreCase("post")) {
                // Default method is get.
                XRLog.general(Level.WARNING, "Using GET request method for form. You probably meant to add a method=\"post\" attribute to your form");
                submit.setFlags(FLAG_USE_GET | FLAG_USE_HTML_SUBMIT);
            } else {
                submit.setFlags(FLAG_USE_HTML_SUBMIT);
            }

            widget.setAction(submit);
        }

        ctrl.page.getAnnotations().add(widget);
        acro.getFields().add(btn);
    }
    
    public int process(PDAcroForm acro, int startId, Box root, PdfBoxOutputDevice od) throws IOException {
        int  i = startId;

        for (ControlFontPair pair : controls) {
            i++;
            
            Control ctrl = pair.control;
            Element e = ctrl.box.getElement();
            
            if ((e.getNodeName().equals("input") &&
                 e.getAttribute("type").equals("text")) ||
                (e.getNodeName().equals("textarea")) ||
                (e.getNodeName().equals("input") && 
                 e.getAttribute("type").equals("password"))) {

                // Start with the text controls (text, password and textarea).
                processTextControl(pair, ctrl, acro, i, root, od);
            } else if (e.getNodeName().equals("select") &&
                       !e.hasAttribute("multiple")) {
                processSelectControl(pair, ctrl, acro, i, root, od);
            }
            else if ((e.getNodeName().equals("input") && 
                      e.getAttribute("type").equals("submit")) ||
                     (e.getNodeName().equals("button") &&
                      !e.getAttribute("type").equals("button")) ||
                     (e.getNodeName().equals("input") &&
                      e.getAttribute("type").equals("reset"))) {
                // We've got a submit or reset control for this form.
                submits.add(ctrl);
            }
        }
        
        // We do submit controls last as we need all the fields in this form.
        for (Control ctrl : submits) {
            i++;
            processSubmitControl(acro, i, ctrl, root, od);
        }
        
        return i;
    }
}

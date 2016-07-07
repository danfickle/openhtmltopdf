package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSArrayList;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionResetForm;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionSubmitForm;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
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
    
    private String populateOptions(Element e, List<String> labels, List<String> values, List<Integer> selectedIndices) {
        List<Element> opts = DOMUtil.getChildren(e, "option");
        String selected = "";
        int i = 0;
        
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
            
            if (opt.hasAttribute("selected") && selectedIndices != null) {
                selectedIndices.add(i);
            }

            i++;
        }
        
        return selected;
    }
    
    private void processMultiSelectControl(ControlFontPair pair, Control ctrl, PDAcroForm acro, int i, Box root, PdfBoxOutputDevice od) throws IOException {
        PDListBox field = new PDListBox(acro);
        
        field.setPartialName("OpenHTMLCtrl" + i);
        controlNames.add("OpenHTMLCtrl" + i);
        
        field.setMappingName(ctrl.box.getElement().getAttribute("name")); // Export name.
        field.setMultiSelect(true);
        
        List<String> labels = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        List<Integer> selected = new ArrayList<Integer>();
        populateOptions(ctrl.box.getElement(), labels, values, selected);
        
        field.setOptions(values, labels);
        field.setSelectedOptionsIndex(selected);
        
        FSColor color = ctrl.box.getStyle().getColor();
        String colorOperator = getColorOperator(color);
        
        String fontInstruction = "/" + pair.fontName + " 0 Tf";
        field.setDefaultAppearance(fontInstruction + ' ' + colorOperator);
        
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
    
    private void processSelectControl(ControlFontPair pair, Control ctrl, PDAcroForm acro, int i, Box root, PdfBoxOutputDevice od) throws IOException {
        PDComboBox field = new PDComboBox(acro);
        
        field.setPartialName("OpenHTMLCtrl" + i);
        controlNames.add("OpenHTMLCtrl" + i);
        
        field.setMappingName(ctrl.box.getElement().getAttribute("name")); // Export name.
        
        List<String> labels = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        String selectedLabel = populateOptions(ctrl.box.getElement(), labels, values, null);
        
        field.setOptions(values, labels);
        field.setValue(selectedLabel);
        field.setDefaultValue(selectedLabel);
        
        FSColor color = ctrl.box.getStyle().getColor();
        String colorOperator = getColorOperator(color);
        
        String fontInstruction = "/" + pair.fontName + " 0 Tf";
        field.setDefaultAppearance(fontInstruction + ' ' + colorOperator);
        
        if (ctrl.box.getElement().hasAttribute("required")) {
            field.setRequired(true);
        }
        
        if (ctrl.box.getElement().hasAttribute("readonly")) {
            field.setReadOnly(true);
        }
        
        if (ctrl.box.getElement().hasAttribute("title")) {
            field.setAlternateFieldName(ctrl.box.getElement().getAttribute("title"));
        }
        
        if (ctrl.box.getElement().getNodeName().equals("openhtmltopdf-combo")) {
            field.setEdit(true);
            field.setCombo(true);
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
        } else if (ctrl.box.getElement().getAttribute("type").equals("file")) {
            XRLog.general(Level.WARNING, "Acrobat Reader does not support forms with file input controls");
            field.setFileSelect(true);
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

    public static enum CheckboxStyle {
        CHECK("0", ""),

        CROSS("8", ""),

        DIAMOND("0", ""),

        CIRCLE("0", ""),

        STAR("0", ""),

        SQUARE("0",
                "q\n" +
                "0 g\n" +
                "20 20 60 60 re\n" +
                "f\n" +
                "Q\n");
        
        private final String caption;
        private final String appearance;
        
        private CheckboxStyle(String caption, String appearance) {
            this.caption = caption;
            this.appearance = appearance;
        }
    }

    public static PDAppearanceStream createCheckboxAppearance(CheckboxStyle style, PDDocument doc) {
        return createCheckboxAppearance(style.appearance, doc);
    }
    
    public static PDAppearanceStream createCheckboxAppearance(String appear, PDDocument doc) {
        PDAppearanceStream s = new PDAppearanceStream(doc);
        s.setBBox(new PDRectangle(100f, 100f));
        OutputStream os = null;
        
        try {
            os = s.getContentStream().createOutputStream(COSName.FLATE_DECODE);
            os.write(appear.getBytes("ASCII"));
        } catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("createCheckboxAppearance", e);
        } finally {
            try {
                if (os != null)
                    os.close();
            } catch (IOException e) {
            }
        }

        return s;
    }

    private void processCheckboxControl(ControlFontPair pair, PDAcroForm acro, int i, Control ctrl, Box root, PdfBoxOutputDevice od) throws IOException {
        PDCheckBox field = new PDCheckBox(acro);
        
        field.setPartialName("OpenHTMLCtrl" + i); // Internal name.
        controlNames.add("OpenHTMLCtrl" + i);
        
        if (ctrl.box.getElement().hasAttribute("required")) {
            field.setRequired(true);
        }
        
        if (ctrl.box.getElement().hasAttribute("readonly")) {
            field.setReadOnly(true);
        }
        
        field.setMappingName(ctrl.box.getElement().getAttribute("name")); // Export name.
        field.setExportValues(Collections.singletonList(ctrl.box.getElement().getAttribute("value")));
        
        if (ctrl.box.getElement().hasAttribute("title")) {
            field.setAlternateFieldName(ctrl.box.getElement().getAttribute("title"));
        }
        
        if (ctrl.box.getElement().hasAttribute("checked")) {
            field.getCOSObject().setItem(COSName.AS, COSName.YES);
            field.getCOSObject().setItem(COSName.V, COSName.YES);
            field.getCOSObject().setItem(COSName.DV, COSName.YES);
        } else {
           field.getCOSObject().setItem(COSName.AS, COSName.Off);
           field.getCOSObject().setItem(COSName.V, COSName.Off);
           field.getCOSObject().setItem(COSName.DV, COSName.Off);
        }
        
        Rectangle2D rect2D = PdfBoxLinkManager.createTargetArea(ctrl.c, ctrl.box, ctrl.pageHeight, ctrl.transform, root, od);
        PDRectangle rect = new PDRectangle((float) rect2D.getMinX(), (float) rect2D.getMinY(), (float) rect2D.getWidth(), (float) rect2D.getHeight());

        PDAnnotationWidget widget = field.getWidgets().get(0);
        widget.setRectangle(rect);
        widget.setPage(ctrl.page);
        widget.setPrinted(true);
        
        // TOOD: Use CSS style.
        CheckboxStyle style = CheckboxStyle.SQUARE;
        
        PDAppearanceCharacteristicsDictionary appearanceCharacteristics = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
        appearanceCharacteristics.setNormalCaption(style.caption);
        widget.setAppearanceCharacteristics(appearanceCharacteristics);

        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.Off, od.checkboxOffAppearance);
        dict.setItem(COSName.YES, od.checkboxAppearances.get(style));
        PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();
        appearanceDict.getCOSObject().setItem(COSName.N, dict);
        widget.setAppearance(appearanceDict);
        
        
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
                 e.getAttribute("type").equals("password")) ||
                (e.getNodeName().equals("input") &&
                 e.getAttribute("type").equals("file"))) {

                // Start with the text controls (text, password, file and textarea).
                processTextControl(pair, ctrl, acro, i, root, od);
            } else if ((e.getNodeName().equals("select") &&
                        !e.hasAttribute("multiple")) ||
                       (e.getNodeName().equals("openhtmltopdf-combo"))) {
                
                processSelectControl(pair, ctrl, acro, i, root, od);
            } else if (e.getNodeName().equals("select") &&
                       e.hasAttribute("multiple")) {
                
                processMultiSelectControl(pair, ctrl, acro, i, root, od);
            } else if (e.getNodeName().equals("input") &&
                       e.getAttribute("type").equals("checkbox")) {
                
                processCheckboxControl(pair, acro, i, ctrl, root, od);
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

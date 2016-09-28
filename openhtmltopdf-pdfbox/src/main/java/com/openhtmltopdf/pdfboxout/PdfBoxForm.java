package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSArrayList;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionResetForm;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionSubmitForm;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
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
    private final Map<String, List<PdfBoxForm.Control>> radioGroups = new LinkedHashMap<String, List<Control>>();
    
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
        CHECK(52),

        CROSS(53),

        DIAMOND(117),

        CIRCLE(108),

        STAR(72),

        SQUARE(110);
        
        private final int caption;
        
        private CheckboxStyle(int caption) {
            this.caption = caption;
        }
        
        public static CheckboxStyle fromIdent(IdentValue id) {
            if (id == IdentValue.CHECK)
                return CHECK;
            
            if (id == IdentValue.CROSS)
                return CROSS;
            
            if (id == IdentValue.SQUARE)
                return SQUARE;
            
            if (id == IdentValue.CIRCLE)
                return CIRCLE;
            
            if (id == IdentValue.DIAMOND)
                return DIAMOND;
            
            if (id == IdentValue.STAR)
                return STAR;
            
            return CHECK;
        }
    }

    public static PDAppearanceStream createCheckboxAppearance(CheckboxStyle style, PDDocument doc, PDResources resources) {
        String appear = 
                "q\n" + 
                "BT\n" +
                "1 0 0 1 15 20 Tm\n" +
                "/OpenHTMLZap 100 Tf\n" +
                "(" + (char) style.caption + ") Tj\n" +
                "ET\n" +
                "Q\n";
        
        return createCheckboxAppearance(appear, doc, resources);
    }
    
    public static PDAppearanceStream createCheckboxAppearance(String appear, PDDocument doc, PDResources resources) {
        PDAppearanceStream s = new PDAppearanceStream(doc);
        s.setBBox(new PDRectangle(100f, 100f));
        OutputStream os = null;
        try {
            os = s.getContentStream().createOutputStream();
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
        
        s.setResources(resources);
        return s;
    }

    private COSString getCOSStringUTF16Encoded(String value) throws UnsupportedEncodingException {
        // UTF-16BE encoded string with a leading byte order marker
        byte[] data = value.getBytes("UTF-16BE");
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 2);
        out.write(0xFE); // BOM
        out.write(0xFF); // BOM
        try
        {
            out.write(data);
        }
        catch (IOException e)
        {
            // should never happen
            throw new RuntimeException(e);
        }
        byte[] bytes = out.toByteArray();
        COSString valueEncoded = new COSString(bytes);
        return valueEncoded;
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

        /*
         * The only way I could get Acrobat Reader to display the checkbox checked properly was to 
         * use an explicitly encoded unicode string for the OPT entry of the dictionary.
         */
        COSArray arr = new COSArray();
        arr.add(getCOSStringUTF16Encoded(ctrl.box.getElement().getAttribute("value")));
        field.getCOSObject().setItem(COSName.OPT, arr);
        
        if (ctrl.box.getElement().hasAttribute("title")) {
            field.setAlternateFieldName(ctrl.box.getElement().getAttribute("title"));
        }
        
        COSName zero = COSName.getPDFName("0");
        
        if (ctrl.box.getElement().hasAttribute("checked")) {
            field.getCOSObject().setItem(COSName.AS, zero);
            field.getCOSObject().setItem(COSName.V, zero);
            field.getCOSObject().setItem(COSName.DV, zero);
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
        
        CheckboxStyle style = CheckboxStyle.fromIdent(ctrl.box.getStyle().getIdent(CSSName.FS_CHECKBOX_STYLE));
        
        PDAppearanceCharacteristicsDictionary appearanceCharacteristics = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
        appearanceCharacteristics.setNormalCaption(String.valueOf((char) style.caption));
        widget.setAppearanceCharacteristics(appearanceCharacteristics);

        COSDictionary dict = new COSDictionary();
        dict.setItem(zero, od.checkboxAppearances.get(style));
        dict.setItem(COSName.Off, od.checkboxOffAppearance);
        PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();
        appearanceDict.getCOSObject().setItem(COSName.N, dict);
        widget.setAppearance(appearanceDict);
        
        ctrl.page.getAnnotations().add(widget);
        acro.getFields().add(field);
    }
    
    private void processRadioButtonGroup(List<Control> group, PDAcroForm acro, int i, Box root, PdfBoxOutputDevice od) throws IOException {
        String groupName = group.get(0).box.getElement().getAttribute("name");
        PDRadioButton field = new PDRadioButton(acro);
        
        field.setPartialName("OpenHTMLCtrl" + i); // Internal name.
        controlNames.add("OpenHTMLCtrl" + i);
        
        field.setMappingName(groupName);
        
        List<String> values = new ArrayList<String>(group.size());
        for (Control ctrl : group) {
            values.add(ctrl.box.getElement().getAttribute("value"));
        }
        field.setExportValues(values);
        
        List<PDAnnotationWidget> widgets = new ArrayList<PDAnnotationWidget>(group.size());
        
        int radioCnt = 0;
        
        for (Control ctrl : group) {
            Rectangle2D rect2D = PdfBoxLinkManager.createTargetArea(ctrl.c, ctrl.box, ctrl.pageHeight, ctrl.transform, root, od);
            PDRectangle rect = new PDRectangle((float) rect2D.getMinX(), (float) rect2D.getMinY(), (float) rect2D.getWidth(), (float) rect2D.getHeight());
            
            PDAnnotationWidget widget = new PDAnnotationWidget();
            
            widget.setRectangle(rect);
            widget.setPage(ctrl.page);
            widget.setPrinted(true);
            
            COSDictionary dict = new COSDictionary();
            dict.setItem(COSName.getPDFName("" + radioCnt), od.radioBoxOnAppearance);
            dict.setItem(COSName.Off, od.radioBoxOffAppearance);
            PDAppearanceDictionary appearanceDict = new PDAppearanceDictionary();
            appearanceDict.getCOSObject().setItem(COSName.N, dict);

            if (ctrl.box.getElement().hasAttribute("checked")) {
                widget.getCOSObject().setItem(COSName.AS, COSName.getPDFName("" + radioCnt));
            } else {
                widget.getCOSObject().setItem(COSName.AS, COSName.Off);
            }

            widget.setAppearance(appearanceDict);
            
            widgets.add(widget);
            ctrl.page.getAnnotations().add(widget);
            
            radioCnt++;
        }

        field.setWidgets(widgets);

        for (Control ctrl : group) {
            if (ctrl.box.getElement().hasAttribute("checked")) {
               field.setValue(ctrl.box.getElement().getAttribute("value"));
            }
        }
        
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
            } else if (e.getNodeName().equals("input") &&
                       e.getAttribute("type").equals("radio")) {
                // We have to do radio button groups in one hit so add them to a map of list keyed on name.
                List<Control> radioGroup = radioGroups.get(e.getAttribute("name"));
                
                if (radioGroup == null) {
                    radioGroup = new ArrayList<PdfBoxForm.Control>();
                    radioGroups.put(e.getAttribute("name"), radioGroup);
                }

                radioGroup.add(ctrl);
            } else if ((e.getNodeName().equals("input") && 
                      e.getAttribute("type").equals("submit")) ||
                     (e.getNodeName().equals("button") &&
                      !e.getAttribute("type").equals("button")) ||
                     (e.getNodeName().equals("input") &&
                      e.getAttribute("type").equals("reset"))) {

                // We've got a submit or reset control for this form.
                submits.add(ctrl);
            }
        }
        
        // Now process each group of radio buttons.
        for (List<Control> group : radioGroups.values()) {
            i++;
            processRadioButtonGroup(group, acro, i, root, od);
        }
        
        // We do submit controls last as we need all the fields in this form.
        for (Control ctrl : submits) {
            i++;
            processSubmitControl(acro, i, ctrl, root, od);
        }
        
        return i;
    }
}

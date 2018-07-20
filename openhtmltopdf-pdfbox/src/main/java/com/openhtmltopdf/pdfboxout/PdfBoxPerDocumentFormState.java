package com.openhtmltopdf.pdfboxout;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.pdfboxout.PdfBoxForm.CheckboxStyle;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.ArrayUtil;
import com.openhtmltopdf.util.XRLog;

/**
 * The per document container for form and form control state.
 */
public class PdfBoxPerDocumentFormState {
    // We keep a map of forms for the document so we can add controls to the correct form as they are seen.
    private final Map<Element, PdfBoxForm> forms = new HashMap<Element, PdfBoxForm>();

    // The list of controls in the document. Control class contains all the info we need to output a control.
    private final List<PdfBoxForm.Control> controls = new ArrayList<PdfBoxForm.Control>();

    // A set of controls, so we don't double process a control.
    private final Set<Element> seenControls = new HashSet<Element>();

    // We keep a map of fonts to font resource name so we don't double add fonts needed for form controls.
    private final Map<PDFont, String> controlFonts = new HashMap<PDFont, String>();
    
    // The checkbox style to appearance stream map. We only create appearance streams on demand and once for a specific
    // style so we store appearance streams created here.
    private final Map<CheckboxStyle, PDAppearanceStream> checkboxAppearances = new EnumMap<CheckboxStyle, PDAppearanceStream>(CheckboxStyle.class);

    // Again, we only create these appearance streams as needed.
    private PDAppearanceStream checkboxOffAppearance;
    private PDAppearanceStream radioBoxOffAppearance;
    private PDAppearanceStream radioBoxOnAppearance;
    
    // The ZapfDingbats font resource needed by checkbox and radio box appearance streams.
    private PDResources checkBoxFontResource;
    
    public PDAppearanceStream getCheckboxStyle(CheckboxStyle style) {
        return checkboxAppearances.get(style);
    }
    
    public PDAppearanceStream getCheckboxOffStream() {
        return this.checkboxOffAppearance;
    }
    
    public PDAppearanceStream getRadioOffStream() {
        return this.radioBoxOffAppearance;
    }
    
    public PDAppearanceStream getRadioOnStream() {
        return this.radioBoxOnAppearance;
    }
    
    /**
     * Adds a form to a map to be used later by <code>processControls</code>.
     */
    public void addFormIfRequired(Box box, PdfBoxOutputDevice od) {
        if (!forms.containsKey(box.getElement())) {
            PdfBoxForm frm = PdfBoxForm.createForm(box.getElement(), this, od);
            forms.put(box.getElement(), frm);
        }
    }

    /**
     * Adds a control to a list to be used later by <code>processControls</code>.
     */
    public void addControlIfRequired(Box box, PDPage page, AffineTransform transform, RenderingContext c, float pageHeight) {
        if (!seenControls.contains(box.getElement())) {
            controls.add(new PdfBoxForm.Control(box, page, new AffineTransform(transform), c, pageHeight));
            seenControls.add(box.getElement());
        }
    }
    
    private String getControlFont(SharedContext sharedContext, PdfBoxForm.Control ctrl) {
        PDFont fnt = ((PdfBoxFSFont) sharedContext.getFont(ctrl.box.getStyle().getFontSpecification())).getFontDescription().get(0).getFont();
        String fontName;
        
        if (!controlFonts.containsKey(fnt)) {
            fontName = "OpenHTMLFont" + controlFonts.size();
            controlFonts.put(fnt, fontName);
        } else {
            fontName = controlFonts.get(fnt);
        }
        
        return fontName;
    }
    
    private void createCheckboxAppearanceStreams(PDDocument writer, PdfBoxForm.Control ctrl) {
        CheckboxStyle style = CheckboxStyle.fromIdent(ctrl.box.getStyle().getIdent(CSSName.FS_CHECKBOX_STYLE));
        
        if (!checkboxAppearances.containsKey(style)) {
            PDAppearanceStream strm = PdfBoxForm.createCheckboxAppearance(style, writer, checkBoxFontResource);
            checkboxAppearances.put(style, strm);
        }
        
        if (checkboxOffAppearance == null) {
            checkboxOffAppearance = PdfBoxForm.createCheckboxAppearance("q\nQ\n", writer, checkBoxFontResource);
        }
    }
    
    private void createRadioboxAppearanceStream(PDDocument writer, PdfBoxForm.Control ctrl) {
        if (radioBoxOffAppearance == null) {
            radioBoxOffAppearance = PdfBoxForm.createCheckboxAppearance("q\nQ\n", writer, checkBoxFontResource);
        }

        if (radioBoxOnAppearance == null) {
            radioBoxOnAppearance = PdfBoxForm.createCheckboxAppearance(CheckboxStyle.DIAMOND, writer, checkBoxFontResource);
        }
    }
    
    private void createCheckboxFontResource() {
        if (checkBoxFontResource == null) {
            checkBoxFontResource = new PDResources();
            checkBoxFontResource.put(COSName.getPDFName("OpenHTMLZap"), PDType1Font.ZAPF_DINGBATS);
        }
    }
    
    public void processControls(SharedContext sharedContext, PDDocument writer, Box root) {
        for (PdfBoxForm.Control ctrl : controls) {
            PdfBoxForm frm = findEnclosingForm(ctrl.box.getElement());
            String fontName = null;
            
            if (!ArrayUtil.isOneOf(ctrl.box.getElement().getAttribute("type"), "checkbox", "radio", "hidden")) {
                // Need to embed a font for every control other than checkbox, radio and hidden.
                fontName = getControlFont(sharedContext, ctrl);
            } else if (ctrl.box.getElement().getAttribute("type").equals("checkbox")) {
                createCheckboxFontResource();
                createCheckboxAppearanceStreams(writer, ctrl);
            } else if (ctrl.box.getElement().getAttribute("type").equals("radio")) {
                createCheckboxFontResource();
                createRadioboxAppearanceStream(writer, ctrl);
            }
                
            if (frm != null) {
                frm.addControl(ctrl, fontName);
            }
        }
        
        PDResources resources = new PDResources(); 
        for (Map.Entry<PDFont, String> fnt : controlFonts.entrySet()) {
            resources.put(COSName.getPDFName(fnt.getValue()), fnt.getKey());
        }
        
        if (forms.size() != 0) {
            int start = 0;
            PDAcroForm acro = new PDAcroForm(writer);

            acro.setNeedAppearances(Boolean.TRUE);
            acro.setDefaultResources(resources);
        
            writer.getDocumentCatalog().setAcroForm(acro);
        
            for (PdfBoxForm frm : forms.values()) {
                try {
                    start = 1 + frm.process(acro, start, root);
                } catch (IOException e) {
                    throw new PdfContentStreamAdapter.PdfException("processControls", e);
                }
            }
        }
    }
    
    /**
     * Helper function to find an enclosing PdfBoxForm given a control element.
     */
    private PdfBoxForm findEnclosingForm(Node e) {
        Element frmElement = DOMUtil.findClosestEnclosingElementWithNodeName(e, "form");
        
        if (frmElement != null &&
            forms.containsKey(frmElement)) {
            return forms.get(frmElement);
        }

        XRLog.general(Level.WARNING, "Found form control ("
                + e.getNodeName() + ") with no enclosing form. Ignoring.");
        return null;
    }
}
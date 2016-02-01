package com.openhtmltopdf.simple.xhtml.swt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.extend.ReplacedElement;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.simple.xhtml.FormControl;
import com.openhtmltopdf.simple.xhtml.XhtmlForm;
import com.openhtmltopdf.simple.xhtml.XhtmlNamespaceHandler;
import com.openhtmltopdf.simple.xhtml.controls.ButtonControl;
import com.openhtmltopdf.simple.xhtml.controls.CheckControl;
import com.openhtmltopdf.simple.xhtml.controls.SelectControl;
import com.openhtmltopdf.simple.xhtml.controls.TextControl;
import com.openhtmltopdf.swt.BasicRenderer;
import com.openhtmltopdf.swt.FormControlReplacementElement;
import com.openhtmltopdf.swt.SWTReplacedElementFactory;

public class SWTXhtmlReplacedElementFactory extends SWTReplacedElementFactory {
    private final BasicRenderer _parent;

    private Map _forms = new HashMap();
    private Map _controls = null;

    public SWTXhtmlReplacedElementFactory(BasicRenderer parent) {
        _parent = parent;
    }

    /**
     * @param e
     * @return the form corresponding to element <code>e</code> or
     *         <code>null</code> if none
     */
    public XhtmlForm getForm(Element e) {
        return (XhtmlForm) _forms.get(e);
    }

    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
            UserAgentCallback uac, int cssWidth, int cssHeight) {
        ReplacedElement re = super.createReplacedElement(c, box, uac, cssWidth,
            cssHeight);
        if (re == null
                && c.getNamespaceHandler() instanceof XhtmlNamespaceHandler
                && !c.isPrint()) {
            XhtmlNamespaceHandler nsh = (XhtmlNamespaceHandler) c
                .getNamespaceHandler();
            Element e = box.getElement();
            if (e == null) {
                return null;
            }

            // form controls
            // first check if the control already exists
            if (_controls != null) {
                re = (ReplacedElement) _controls.get(e);
            }
            if (re != null) {
                if (re instanceof FormControlReplacementElement) {
                    // update the size
                    ((FormControlReplacementElement) re).calculateSize(c, box
                        .getStyle(), cssWidth, cssHeight);
                }
                return re;
            }

            // not found, try to create one
            Element parentForm = getParentForm(e, c);
            // parentForm may be null, this is not a problem
            XhtmlForm form = (XhtmlForm) _forms.get(parentForm);
            if (form == null) {
                form = nsh.createForm(parentForm);
                _forms.put(parentForm, form);
            }

            FormControl control = form.createControl(e);
            if (control == null) {
                // this is not a form control
                return null;
            }
            SWTFormControl swtControl = null;
            if (control instanceof TextControl) {
                swtControl = new SWTTextControl(control, _parent, c, box
                    .getStyle(), uac);
            } else if (control instanceof ButtonControl) {
                swtControl = new SWTButtonControl(control, _parent, c, box
                    .getStyle(), uac);
            } else if (control instanceof CheckControl) {
                swtControl = new SWTCheckControl(control, _parent, c, box
                    .getStyle(), uac);
            } else if (control instanceof SelectControl) {
                swtControl = new SWTSelectControl(control, _parent, c, box
                    .getStyle(), uac);
            } else {
                // no replacement found
                return null;
            }
            swtControl.getSWTControl().setVisible(false);

            FormControlReplacementElement fcre = new FormControlReplacementElement(
                swtControl);
            fcre.calculateSize(c, box.getStyle(), cssWidth, cssHeight);
            re = fcre;
            if (_controls == null) {
                _controls = new HashMap();
            }
            _controls.put(e, re);
        }
        return re;
    }

    public void remove(Element e) {
        super.remove(e);
        if (_controls != null) {
            ReplacedElement re = (ReplacedElement) _controls.get(e);
            if (re instanceof FormControlReplacementElement) {
                SWTFormControl control = ((FormControlReplacementElement) re)
                    .getControl();
                if (control != null) {
                    control.dispose();
                }
            }
            _controls.remove(e);
        }
    }

    public void reset() {
        super.reset();
        _forms = new HashMap();
        if (_controls != null) {
            for (Iterator iter = _controls.values().iterator(); iter.hasNext();) {
                ReplacedElement re = (ReplacedElement) iter.next();
                if (re instanceof FormControlReplacementElement) {
                    SWTFormControl control = ((FormControlReplacementElement) re)
                        .getControl();
                    if (control != null) {
                        control.dispose();
                    }
                }
            }
            _controls = null;
        }
    }

    /**
     * @param e
     */
    protected Element getParentForm(Element e, LayoutContext context) {
        Node node = e;
        XhtmlNamespaceHandler nsh = (XhtmlNamespaceHandler) context
            .getNamespaceHandler();

        do {
            node = node.getParentNode();
        } while (node.getNodeType() == Node.ELEMENT_NODE
                && !nsh.isFormElement((Element) node));

        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        return (Element) node;
    }

}

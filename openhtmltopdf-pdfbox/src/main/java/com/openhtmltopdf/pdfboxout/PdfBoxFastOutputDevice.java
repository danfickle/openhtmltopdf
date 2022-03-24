/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.bidi.SimpleBidiReorderer;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.newmatch.Selector;
import com.openhtmltopdf.css.parser.CSSPrimitiveValue;
import com.openhtmltopdf.css.parser.FSCMYKColor;
import com.openhtmltopdf.css.parser.FSColor;
import com.openhtmltopdf.css.parser.FSRGBColor;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.BorderPropertySet;
import com.openhtmltopdf.css.style.derived.FSLinearGradient;
import com.openhtmltopdf.css.value.FontSpecification;
import com.openhtmltopdf.extend.FSImage;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.extend.StructureType;
import com.openhtmltopdf.extend.TextRenderer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.FontResolverHelper;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver.FontDescription;
import com.openhtmltopdf.pdfboxout.PdfBoxUtil.FontRun;
import com.openhtmltopdf.pdfboxout.PdfBoxUtil.Metadata;
import com.openhtmltopdf.pdfboxout.fontstore.FontNotFoundException;
import com.openhtmltopdf.render.*;
import com.openhtmltopdf.simple.extend.ReplacedElementScaleHelper;
import com.openhtmltopdf.util.ArrayUtil;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.OpenUtil;
import com.openhtmltopdf.util.XRLog;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentGroup;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentMembershipDictionary;
import org.apache.pdfbox.pdmodel.graphics.optionalcontent.PDOptionalContentProperties;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.geom.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PdfBoxFastOutputDevice extends AbstractOutputDevice implements OutputDevice, PdfBoxOutputDevice {
    //
    // A discussion on units:
    //   PDF points are defined as 1/72 inch.
    //   CSS pixels are defined as 1/96 inch.
    //   PDF text units are defined as 1/1000 of a PDF point.
    //   OpenHTMLtoPDF dots are defined as 1/20 of a CSS pixel.
    //   Therefore dots per point is 20 * 96/72 or about 26.66.
    //   Dividing by _dotsPerPoint will convert OpenHTMLtoPDF dots to PDF points.
    //   Theoretically, this is all configurable, but not tested at all with other values.
    //
    
    private enum GraphicsOperation {
        FILL,
        STROKE,
        CLIP;
    }

    /**
     * Optional content manager [PDF:1.7:8.11].
     *
     * <p>Optional content rendering is CSS-driven. In the source HTML document, each content
     * fragment can be marked by a CSS class defining a layer through the following properties:</p>
     * <ul>
     *   <li>group:
     *     <ul>
     *       <li>{@link CSSName#FS_OCG_ID}</li>
     *       <li>{@link CSSName#FS_OCG_LABEL}</li>
     *       <li>{@link CSSName#FS_OCG_PARENT}</li>
     *       <li>{@link CSSName#FS_OCG_VISIBILITY}</li>
     *     </ul>
     *   </li>
     *   <li>membership:
     *     <ul>
     *       <li>{@link CSSName#FS_OCM_ID}</li>
     *       <li>{@link CSSName#FS_OCM_OCGS}</li>
     *       <li>{@link CSSName#FS_OCM_VISIBLE}</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @apiNote For example, consider we want to define two layers ("OCG 1" and "OCG2"):
     * <pre>
&lt;style>
    .ocg1 {
        -fs-ocg-id: "ocg1";
        -fs-ocg-label: "OCG 1";
    }

    .ocg2 {
        -fs-ocg-id: "ocg2";
        -fs-ocg-label: "OCG 2";
        -fs-ocg-parent: "ocg1";
        -fs-ocg-visibility: hidden;
    }
&lt;/style>
     * </pre>
     * and then use them to mark our contents:
     * <pre>
&lt;body>
    &lt;p class="ocg1">(OCG 1) This is a layered content block.&lt;/p>
    &lt;p class="ocg2">(OCG 2) This is another layered content block.&lt;/p>
&lt;/body>
     * </pre>
     */
    private static final class OptionalContentManager {
        private static class GroupDeclaration extends LayerDeclaration<PDOptionalContentGroup> {
            public final String label;
            @SuppressWarnings("unused")
            public final String parent;
            public final boolean visible;

            public GroupDeclaration(String id, String label, String parent, boolean visible) {
                super(id);
                this.label = label;
                this.parent = parent;
                this.visible = visible;
            }

            @Override
            public PDOptionalContentGroup build(OptionalContentManager manager) {
                PDOptionalContentProperties configuration = manager.getConfiguration();
                if (configuration.hasGroup(label)) {
                    return configuration.getGroup(label);
                } else {
                    PDOptionalContentGroup group = new PDOptionalContentGroup(label);
                    {
                        configuration.setGroupEnabled(group, visible);
                        /*-
                         * TODO: configure parent (hierarchical UI presentation)!
                         *
                         * Should map parent property to configuration (that is, Order entry of D
                         * entry of PDOptionalContentProperties) but, unfortunately, arbitrary group
                         * nesting seems not to be natively supported by currently used PDFBox
                         * (version 2.0), as adding a group via PDOptionalContentProperties.addGroup(..)
                         * automatically builds a flat list inside Order entry instead.
                         */
                    }
                    configuration.addGroup(group);
                    return group;
                }
            }
        }

        private static abstract class LayerDeclaration<T extends PDPropertyList> {
            @SuppressWarnings("unused")
            public final String id;

            public LayerDeclaration(String id) {
                this.id = id;
            }

            public abstract T build(OptionalContentManager manager);
        }

        private static class MembershipDeclaration extends LayerDeclaration<PDOptionalContentMembershipDictionary> {
            public final List<String> ocgs;
            public final COSName visibilityPolicy;

            public MembershipDeclaration(String id, List<String> ocgs, COSName visibilityPolicy) {
                super(id);
                this.ocgs = Collections.unmodifiableList(ocgs);
                this.visibilityPolicy = visibilityPolicy;
            }

            @Override
            public PDOptionalContentMembershipDictionary build(OptionalContentManager manager) {
                PDOptionalContentMembershipDictionary membership = new PDOptionalContentMembershipDictionary();
                {
                    List<PDPropertyList> ocgs = new ArrayList<>();
                    for (String ocg : this.ocgs) {
                        ocgs.add(manager.getLayer(ocg));
                    }
                    membership.setOCGs(ocgs);
                    membership.setVisibilityPolicy(visibilityPolicy);
                }
                return membership;
            }
        }

        private static class StackLayer {
            PDPropertyList base;
            /**
             * Whether this OCG fragment is at block level.
             */
            boolean blocked;
            /**
             * Structural depth (non-positive values mean this OCG fragment ended).
             */
            int level;

            public StackLayer(PDPropertyList base, boolean blocked) {
                this.base = base;
                this.blocked = blocked;
            }

            public boolean out() {
                return --level <= 0;
            }

            public void in() {
                level++;
            }

            @Override
            public String toString() {
                return "Layer '" + base + "'";
            }
        }

        private static final CSSPrimitiveValue CSSEmptyValue = new CSSPrimitiveValue() {
            @Override public short getCssValueType() { return 0; }
            @Override public String getCssText() { return null; }
            @Override public String getStringValue() { return null; }
            @Override public short getPrimitiveType() { return 0; }
            @Override public float getFloatValue(short unitType) { return 0; }
        };
        private static final PropertyDeclaration EmptyPropertyDeclaration = new PropertyDeclaration(null, CSSEmptyValue, false, 0);

        /**
         * Current box.
         */
        private Box box;
        /**
         * Current box's layers.
         */
        private LinkedList<PDPropertyList> boxLayers = new LinkedList<>();
        /**
         * Current structure.
         */
        private StructureType structureType;

        private PDOptionalContentProperties configuration;
        /**
         * Layers mapped in the output document.
         */
        private Map<String, PDPropertyList> layers = new HashMap<>();
        /**
         * Layer definitions.
         */
        private Map<String,LayerDeclaration<?>> layerDeclarations;

        /**
         * Layers currently open in content stream.
         */
        private LinkedList<StackLayer> stack = new LinkedList<>();
        private boolean pending;

        private final PdfBoxFastOutputDevice out;

        public OptionalContentManager(PdfBoxFastOutputDevice out) {
            this.out = out;
        }

        /**
         * Closes any layer up to the given one inside the content stream.
         *
         * @param ocgName
         *          {@code null}, to end all OCG fragments.
         */
        public void end(PDPropertyList layer) {
            while (!stack.isEmpty()) {
                XRLog.log(Level.FINE, LogMessageId.LogMessageId1Param.GENERAL_PDF_OCG_END, stack.peek().base);

                out._cp.endMarkedContent();

                if (stack.pop().base == layer) {
                    break;
                }
            }
            pending = false;
        }

        /**
         * Ensures that pending layers are opened inside the content stream.
         *
         * <p>In case of multiple layers, they are recursively nested.</p>
         */
        public void ensure() {
            if (pending) {
                start();
            }
        }

        /**
         * Notifies the end of current structure.
         *
         * <p>In case of an unstructured document, dying layer fragments are kept alive to possibly
         * merge with contiguous fragments.</p>
         *
         * <p>To ensure proper nesting, it MUST be called BEFORE closing the current structure.</p>
         */
        public void onStructureEnd() {
            /*
             * NOTE: As recommended by the PDF spec [PDF:1.7:8.11.3.2], layer fragments SHOULD be
             * nested inside other marked content; therefore, if PDF/UA is active, current layer
             * fragments are immediately ended. Otherwise, layer fragments can continue across
             * contiguous inline boxes only; on the contrary, layer fragments at block level are
             * always immediately ended, because block-painting operations may be intermingled with
             * interfering out-of-sequence graphical operations (for example, table border is
             * painted between cells' background and cells' content drawing).
             */
            if(!stack.isEmpty() && (
                    (stack.peek().out() && stack.peek().blocked)
                    || out._pdfUa != null)) {
                end(null);
            }
        }

        /**
         * Notifies the start of pending structure.
         *
         * <p>Dead layer fragments are closed, while new layer fragments are kept pending until
         * {@link #ensure()} is called. This lazy mechanism ensures that layers are rendered inside
         * the content stream only when effectively needed (otherwise, the content stream would be
         * polluted by tons of empty layer fragments!).</p>
         *
         * <p>To ensure proper nesting, it MUST be called BEFORE opening the pending structure.</p>
         *
         * @param type
         * @param box
         */
        public void onStructureStart(StructureType type, Box box) {
            /*-
             * NOTE: For optional content [PDF:1.7:8.11.3.2], to avoid conflict with other features
             * that used marked content (such as logical structure), the following strategy is
             * recommended:
             * - where content is to be tagged with optional content markers as well as other
             *   markers, the optional content markers should be nested INSIDE the other marked
             *   content.
             * - where optional content and the other markers would overlap but there is not strict
             *   containment, the optional content should be BROKEN UP into two or more BDC/EMC
             *   sections, nesting the optional content sections inside the others as necessary.
             *   Breaking up optional content spans does not damage the nature of the visibility of
             *   the content, whereas the same guarantee cannot be made for all other uses of marked
             *   content.
             *
             * Since the painting phase deals with imperative operations instead of declarative
             * boxes, a box associated to a layer may be rendered through multiple (possibly
             * non-contiguous) operations, each one described by one or more nested structures. In
             * order to merge contiguous contents belonging to the same layer, current layer stack
             * is compared to the layer hierarchy of the next box, closing their non-matching
             * levels.
             */
            // Extracting box layer hierarchy...
            boxLayers.clear();
            Box parentBox = box;
            while (parentBox != null) {
                PDPropertyList layer = getLayer(parentBox);
                if (layer != null && (boxLayers.isEmpty() || boxLayers.getLast() != layer)) {
                    boxLayers.add(layer);
                }
                parentBox = parentBox.getParent();
            }
            // Closing dead layers...
            for (int i = 0; i < stack.size(); i++) {
                if (i >= boxLayers.size() || stack.get(i).base != boxLayers.get(i)) {
                    end(stack.get(i).base);
                    break;
                }
            }
            if (pending = (this.stack.size() != boxLayers.size())) {
                this.box = box;
                this.structureType = type;
            }
        }

        private PDOptionalContentProperties getConfiguration() {
            if (configuration == null) {
                PDDocumentCatalog catalog = out._writer.getDocumentCatalog();
                configuration = catalog.getOCProperties();
                if (configuration == null) {
                    catalog.setOCProperties(configuration = new PDOptionalContentProperties());
                }
            }
            return configuration;
        }

        /**
         * Gets the CSS-based declaration associated to the given ID.
         *
         * @apiNote Layer declarations are lazily harvested from the CSS rulesets available in the
         * current context.
         *
         * @param id
         *          Layer ID (either {@link CSSName#FS_OCG_ID group} or
         *          {@link CSSName#FS_OCM_ID membership}).
         */
        @SuppressWarnings("unchecked")
        private <T extends LayerDeclaration<?>> T getDeclaration(String id) {
            if (layerDeclarations == null) {
                /*-
                 * TODO: This could be strengthened if extension at-rules were implemented (@-fs-ocg
                 * for an optional content group and @-fs-ocm for an optional content membership)
                 * beside existing standard at-rules (such as fontFaceRules) in
                 * com.openhtmltopdf.css.sheet.Stylesheet).
                 */
                layerDeclarations = new HashMap<>();
                Map<CSSName, PropertyDeclaration> layerProperties = new HashMap<>();
                for (Selector selector : out._sharedContext.getCss().getSelectors()) {
                    for (PropertyDeclaration property : selector.getRuleset().getPropertyDeclarations()) {
                        CSSName propertyName = property.getCSSName();
                        if (propertyName == CSSName.FS_OCG_ID
                                || propertyName == CSSName.FS_OCG_LABEL
                                || propertyName == CSSName.FS_OCG_PARENT
                                || propertyName == CSSName.FS_OCG_VISIBILITY
                                || propertyName == CSSName.FS_OCM_ID
                                || propertyName == CSSName.FS_OCM_OCGS
                                || propertyName == CSSName.FS_OCM_VISIBLE) {
                            layerProperties.put(propertyName, property);
                        }
                    }
                    if (layerProperties.isEmpty())
                        continue;

                    PropertyDeclaration layerIdProperty;
                    LayerDeclaration<?> layerDeclaration;
                    if ((layerIdProperty = layerProperties.get(CSSName.FS_OCG_ID)) != null) {
                        layerDeclaration = new GroupDeclaration(layerIdProperty.getValue().getStringValue(),
                                        layerProperties.get(CSSName.FS_OCG_LABEL).getValue().getStringValue(),
                                        layerProperties.getOrDefault(CSSName.FS_OCG_PARENT, EmptyPropertyDeclaration).getValue().getStringValue(),
                                        getPropertyValue(layerProperties, CSSName.FS_OCG_VISIBILITY).equals(IdentValue.VISIBLE));
                    } else if ((layerIdProperty = layerProperties.get(CSSName.FS_OCM_ID)) != null) {
                        List<String> ocgs = new ArrayList<>();
                        {
                            PropertyDeclaration ocgsProperty = layerProperties.get(CSSName.FS_OCM_OCGS);
                            if(ocgsProperty == null)
                                throw new RuntimeException(CSSName.FS_OCM_OCGS + " property undefined (" + layerIdProperty + ")");

                            ocgs = Arrays.asList(ocgsProperty.getValue().getStringValue().split(" "));
                        }
                        COSName visibilityPolicy;
                        {
                            IdentValue rawVisibilityPolicy = getPropertyValue(layerProperties, CSSName.FS_OCM_VISIBLE);
                            if (rawVisibilityPolicy == IdentValue.ALL_HIDDEN) {
                                visibilityPolicy = COSName.ALL_OFF;
                            } else if (rawVisibilityPolicy == IdentValue.ALL_VISIBLE) {
                                    visibilityPolicy = COSName.ALL_ON;
                            } else if (rawVisibilityPolicy == IdentValue.ANY_HIDDEN) {
                                visibilityPolicy = COSName.ANY_OFF;
                            } else {
                                visibilityPolicy = COSName.ANY_ON;
                            }
                        }
                        layerDeclaration = new MembershipDeclaration(layerIdProperty.getValue().getStringValue(),
                                        ocgs,
                                        visibilityPolicy);
                    } else
                        throw new UnsupportedOperationException("Unknown OC layer property set: " + layerProperties);

                    layerDeclarations.put(layerIdProperty.getValue().getStringValue(), layerDeclaration);
                    layerProperties.clear();
                }
            }
            return (T)layerDeclarations.get(id);
        }

        private PDPropertyList getLayer(String id) {
            return layers.computeIfAbsent(id, k -> getDeclaration(k).build(this));
        }

        private PDPropertyList getLayer(Box box) {
            FSDerivedValue layerIdObj;
            {
                CalculatedStyle style = box.getStyle();
                layerIdObj = style.valueByName(CSSName.FS_OCG_ID);
                if(layerIdObj == null) {
                    layerIdObj = style.valueByName(CSSName.FS_OCM_ID);
                }
            }
            return layerIdObj != null ? getLayer(layerIdObj.asString()) : null;
        }

        private static IdentValue getPropertyValue(Map<CSSName, PropertyDeclaration> properties, CSSName propertyName) {
            return properties.containsKey(propertyName)
                    ? properties.get(propertyName).asIdentValue()
                    : IdentValue.getByIdentString(CSSName.initialValue(propertyName));
        }

        /**
         * Opens pending layers inside the content stream.
         *
         * <p>In case of multiple layers, they are recursively nested.</p>
         */
        private void start() {
            if (!pending)
                return;

            pending = false;

            for (int i = stack.size(); i < boxLayers.size(); i++) {
                PDPropertyList layer = boxLayers.get(i);

                XRLog.log(Level.FINE, LogMessageId.LogMessageId3Param.GENERAL_PDF_OCG_BEGIN, layer, structureType, box instanceof InlineLayoutBox ? ((InlineLayoutBox)box).getInlineChildren() : box.getElement());

                out._cp.beginMarkedContent(COSName.OC, layer);

                stack.push(new StackLayer(layer, box instanceof BlockBox));
                stack.peek().in();
            }
        }
    }

    private static class PageState {
        // The actual fill and stroke colors set on the PDF graphics stream.
        // We keep these so we don't bloat the PDF with unneeded color calls.
        private FSColor fillColor;
        private FSColor strokeColor;
        
        private PageState copy() {
            PageState ret = new PageState();
            
            ret.fillColor = this.fillColor;
            ret.strokeColor = this.strokeColor;
            
            return ret;
        }
    }
    
    private static final AffineTransform IDENTITY = new AffineTransform();
    private static final BasicStroke STROKE_ONE = new BasicStroke(1);
    private static final boolean ROUND_RECT_DIMENSIONS_DOWN = false;

    // The current PDF page.
    private PDPage _page;
    
    // A wrapper around the IOException throwing content stream methods which only throws runtime exceptions.
    // Created for every page.
    private PdfContentStreamAdapter _cp;
    
    // We need the page height because the project uses top down units which PDFs use bottom up units.
    // This is in PDF points unit (1/72 inch).
    private float _pageHeight;

    // The desired font as set by setFont.
    // This may not yet be set on the PDF text stream.
    private PdfBoxFSFont _font;

    // This transform is a scale and translate.
    // It scales from internal dots to PDF points.
    // It translates positions to implement page margins.
    private AffineTransform _transform = new AffineTransform();

    // The desired colors as set by setColor.
    // To make sure this color is set on the PDF graphics stream call ensureFillColor or ensureStrokeColor.
    private final PageState _desiredPageState = new PageState();
    
    // The page state stack
    private final Deque<PageState> _pageStateStack = new ArrayDeque<>();

    // The currently set stroke. This will not yet be set on the PDF graphics stream.
    // This is already transformed to PDF points units.
    // Call setStrokeDiff to set this on the PDF graphics stream.
    private Stroke _stroke = null;
    
    // Same as _stroke, but not transformed. That is, it is in internal dots units.
    private Stroke _originalStroke = null;
    
    // The currently set stroke on the PDF graphics stream. When we call setStokeDiff
    // this is compared with _stroke and only the differences are output to the graphics stream.
    private Stroke _oldStroke = null;

    // Essentially per-run global variables.
    private SharedContext _sharedContext;
    
    // The project internal dots per PDF point unit. See discussion of units above.
    private float _dotsPerPoint;

    // The PDF document. Note: We are not responsible for closing it.
    private PDDocument _writer;

    // Manages bookmarks for the current document.
    private PdfBoxBookmarkManager _bmManager;

    // Contains a list of metadata items for the document.
    private final List<Metadata> _metadata = new ArrayList<>();

    // Contains all the state needed to manage form controls
    private final PdfBoxPerDocumentFormState _formState = new PdfBoxPerDocumentFormState();
    
    // The root box in the document. We keep this so we can search for specific boxes below it
    // such as links or form controls which we need to position.
    private Box _root;

    // In theory, we can append to a PDF document, rather than creating new. This keeps the start page
    // so we can use it to offset when we need to know the PDF page number.
    // NOTE: Not tested recently, this feature may be broken.
    private int _startPageNo;
    
    // Whether we are in test mode, currently not used here, but keep around in case we need it down the track.
    @SuppressWarnings("unused")
    private final boolean _testMode;
    
    // Link manage handles a links. We add the link in paintBackground and then output links when the document is finished.
    private PdfBoxFastLinkManager _linkManager;

    // Not used currently.
    private RenderingContext _renderingContext;

    // The bidi reorderer is responsible for shaping Arabic text, deshaping and 
    // converting RTL text into its visual order.
    private BidiReorderer _reorderer = new SimpleBidiReorderer();

    // Font Mapping for the Graphics2D output
    private PdfBoxGraphics2DFontTextDrawer _fontTextDrawer;
    
    // If we are attempting to be PDF/UA compliant (ie tagged pdf), a helper, otherwise null.
    private PdfBoxAccessibilityHelper _pdfUa;
    private final boolean _pdfUaConform;
    
    private final boolean _pdfAConform;

    private OptionalContentManager ocManager = new OptionalContentManager(this);

    public PdfBoxFastOutputDevice(float dotsPerPoint, boolean testMode, boolean pdfUaConform, boolean pdfAConform) {
        _dotsPerPoint = dotsPerPoint;
        _testMode = testMode;
        _pdfUaConform = pdfUaConform;
        _pdfAConform = pdfAConform;
    }

    @Override
    public void setWriter(PDDocument writer) {
        _writer = writer;
    }

    @Override
    public PDDocument getWriter() {
        return _writer;
    }

    /**
     * Start a page. A new PDF page starts a new content stream so all graphics state has to be 
     * set back to default.
     */
    @Override
    public void initializePage(PDPageContentStream currentPage, PDPage page, float height) {
        _cp = new PdfContentStreamAdapter(currentPage);
        _page = page;
        _pageHeight = height;
        
        _desiredPageState.fillColor = null;
        _desiredPageState.strokeColor = null;
        
        pushState(new PageState());
        
        _transform = new AffineTransform();
        _transform.scale(1.0d / _dotsPerPoint, 1.0d / _dotsPerPoint);

        _stroke = transformStroke(STROKE_ONE);
        _originalStroke = _stroke;
        _oldStroke = _stroke;

        setStrokeDiff(_stroke, null);
        
        if (_pdfUa != null) {
            _pdfUa.startPage(_page, _cp, _renderingContext, _pageHeight, _transform);
        }
    }
    
    private PageState currentState() {
        return _pageStateStack.peekFirst();
    }
    
    private void pushState(PageState state) {
        _pageStateStack.addFirst(state);
    }
    
    private PageState popState() {
        return _pageStateStack.removeFirst();
    }

    @Override
    public void finishPage() {
        ocManager.end(null) /* Ensures that any pending layer is closed */;

        _cp.closeContent();
        popState();
        
        if (_pdfUa != null) {
            _pdfUa.endPage();
        }
    }

    @Override
    public void paintReplacedElement(RenderingContext c, BlockBox box) {
        PdfBoxReplacedElement element = (PdfBoxReplacedElement) box.getReplacedElement();
        element.paint(c, this, box);
    }

    @Override
    protected void onPaintBackground(RenderingContext c, CalculatedStyle style,
            Rectangle backgroundBounds, Rectangle bgImageContainer, BorderPropertySet border) {
        ocManager.ensure();
    }

    /**
     * We use paintBackground to do extra stuff such as processing links, forms and form controls.
     */
    @Override
    public void paintBackground(RenderingContext c, Box box) {
        super.paintBackground(c, box);

        // processLinkLater will take care of making sure it is actually a link.
        _linkManager.processLinkLater(c, box, _page, _pageHeight, _transform);
       
        if (box.getElement() != null && box.getElement().getNodeName().equals("form")) {
            _formState.addFormIfRequired(box, this);
        } else if (box.getElement() != null &&
                   ArrayUtil.isOneOf(box.getElement().getNodeName(), "input", "textarea", "button", "select", "openhtmltopdf-combo")) {
            // Add controls to list to process later. We do this in case we paint a control background
            // before its associated form.
            _formState.addControlIfRequired(box, _page, _transform, c, _pageHeight);
        }
    }

    private void processControls() {
        _formState.processControls(_sharedContext, _writer, _root);
    }

    /**
     * Given a value in dots units, converts to PDF points.
     */
    @Override
    public float getDeviceLength(float length) {
        return length / _dotsPerPoint;
    }

    @Override
    public void drawBorderLine(Shape bounds, int side, int lineWidth, boolean solid) {
        draw(bounds);
    }

    @Override
    public void setColor(FSColor color) {
        if (color instanceof FSRGBColor) {
             this._desiredPageState.fillColor = color;
             this._desiredPageState.strokeColor = color;
        } else if (color instanceof FSCMYKColor) {
            this._desiredPageState.fillColor = color;
            this._desiredPageState.strokeColor = color;
        } else {
            assert(color instanceof FSRGBColor || color instanceof FSCMYKColor);
        }
    }

    @Override
    public void draw(Shape s) {
        followPath(s, GraphicsOperation.STROKE);
    }

    @Override
    protected void drawLine(int x1, int y1, int x2, int y2) {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        draw(line);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        draw(new Rectangle(x, y, width, height));
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        draw(oval);
    }

    @Override
    public void fill(Shape s) {
        followPath(s, GraphicsOperation.FILL);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (ROUND_RECT_DIMENSIONS_DOWN) {
            fill(new Rectangle(x, y, width - 1, height - 1));
        } else {
            fill(new Rectangle(x, y, width, height));
        }
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        Ellipse2D oval = new Ellipse2D.Float(x, y, width, height);
        fill(oval);
    }

    @Override
    public void translate(double tx, double ty) {
        _transform.translate(tx, ty);
    }

    @Override
    public Object getRenderingHint(Key key) {
        return null;
    }

    @Override
    public void setRenderingHint(Key key, Object value) {
    }

    @Override
    public void setFont(FSFont font) {
        _font = ((PdfBoxFSFont) font);
        if (_font.getFontDescription().isEmpty()) {
            throw new FontNotFoundException(this.getFontSpecification());
        }
    }

    /**
     * This returns a matrix that will convert y values to bottom up coordinate space (as used by PDFs).
     */
    private AffineTransform normalizeMatrix(AffineTransform current) {
        double[] mx = new double[6];
        AffineTransform result = new AffineTransform();
        result.getMatrix(mx);
        mx[3] = -1;
        mx[5] = _pageHeight;
        result = new AffineTransform(mx);
        result.concatenate(current);
        return result;
    }

    @Override
    public void drawString(String s, float x, float y, JustificationInfo info) {
        PDFont firstFont = _font.getFontDescription().get(0).getFont();

        String effectiveString = TextRenderer.getEffectivePrintableString(s);

        // First check if the string contains printable characters only and
        // will print with the current font entirely.
        try {
            firstFont.getStringWidth(effectiveString);
            // We got here, so all is good.
            drawStringFast(effectiveString, x, y, info, _font.getFontDescription().get(0), _font.getSize2D());
            return;
        }
        catch (Exception e) {
            // Fallthrough, we'll have to process the string into font runs.
        }
        
        List<FontRun> fontRuns = PdfBoxTextRenderer.divideIntoFontRuns(_font, effectiveString, _reorderer);
        
        float xOffset = 0f;
        for (FontRun run : fontRuns) {
            drawStringFast(run.str, x + xOffset, y, info, run.des, _font.getSize2D());
            try {
                if (info == null) {
                    xOffset += ((run.des.getFont().getStringWidth(run.str) / 1000f) * _font.getSize2D());
                } else {
                    xOffset += ((run.des.getFont().getStringWidth(run.str) / 1000f) * _font.getSize2D()) +
                               (run.spaceCharacterCount * info.getSpaceAdjust()) +
                               (run.otherCharacterCount * info.getNonSpaceAdjust());
                }
            } catch (Exception e) {
                XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.RENDER_BUG_FONT_DIDNT_CONTAIN_EXPECTED_CHARACTER, e);
            }
        }
    }

    @Override
    public void drawStringFast(String s, float x, float y, JustificationInfo info, FontDescription desc, float fontSize) {
        if (s.length() == 0)
            return;

        ocManager.ensure();

        ensureFillColor();
        AffineTransform at = new AffineTransform(getTransform());
        at.translate(x, y);
        AffineTransform inverse = normalizeMatrix(at);
        AffineTransform flipper = AffineTransform.getScaleInstance(1, -1);
        inverse.concatenate(flipper);
        inverse.scale(_dotsPerPoint, _dotsPerPoint);
        double[] mx = new double[6];
        inverse.getMatrix(mx);
        
        float b = (float) mx[1];
        float c = (float) mx[2];
        
        fontSize = fontSize / _dotsPerPoint;
        
        boolean resetMode = false;
        FontSpecification fontSpec = getFontSpecification();
        if (fontSpec != null) {
            int need = FontResolverHelper.convertWeightToInt(fontSpec.fontWeight);
            int have = desc.getWeight();
            if (need > have) {
                _cp.setRenderingMode(RenderingMode.FILL_STROKE);
                float lineWidth = fontSize * 0.04f; // 4% of font size
                _cp.setLineWidth(lineWidth);
                resetMode = true;
                ensureStrokeColor();
            }
            if ((fontSpec.fontStyle == IdentValue.ITALIC) && (desc.getStyle() != IdentValue.ITALIC)) {
                b = 0f;
                c = 0.21256f;
            }
        }

        _cp.beginText();
        
        _cp.setFont(desc.getFont(), fontSize);
        _cp.setTextMatrix((float) mx[0], b, c, (float) mx[3], (float) mx[4], (float) mx[5]);

        if (info != null ) {
            // Note: Justification info is also used
            // to implement letter-spacing CSS property.
            // Justification must be done through TJ rendering
            // because Tw param does not work for UNICODE fonts
            Object[] array = makeJustificationArray(s, info);
            _cp.drawStringWithPositioning(array);
        } else {
            _cp.drawString(s);
        }
        
        _cp.endText();

        if (resetMode) {
            _cp.setRenderingMode(RenderingMode.FILL);
            _cp.setLineWidth(1);
        }
    }

    private Object[] makeJustificationArray(String s, JustificationInfo info) {
        List<Object> data = new ArrayList<>(s.length() * 2);

        s.codePoints().forEachOrdered(cp -> {
            data.add(String.valueOf(Character.toChars(cp)));

            float offset = InlineText.isJustifySpaceCodePoint(cp) ?
                    info.getSpaceAdjust() :
                    info.getNonSpaceAdjust();

            data.add(Float.valueOf((-offset / _dotsPerPoint) * 1000 / (_font.getSize2D() / _dotsPerPoint)));
        });

        if (data.size() > 0) {
            int lastIndex = data.size() - 1;
            if (data.get(lastIndex) instanceof Float) {
                // The array should not end with a spacing value.
                data.remove(lastIndex);
            }
        }

        return data.toArray();
    }

    private AffineTransform getTransform() {
        return _transform;
    }

    private void ensureFillColor() {
        PageState state = currentState();
        if (state.fillColor == null || !(state.fillColor.equals(_desiredPageState.fillColor))) {
            state.fillColor = _desiredPageState.fillColor;

            if (state.fillColor instanceof FSRGBColor) {
                FSRGBColor rgb = (FSRGBColor) state.fillColor;
                _cp.setFillColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
            } else if (state.fillColor instanceof FSCMYKColor) {
                FSCMYKColor cmyk = (FSCMYKColor) state.fillColor;
                _cp.setFillColor(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
            }
            else {
                assert(state.fillColor instanceof FSRGBColor || state.fillColor instanceof FSCMYKColor);
            }
       }
    }

    private void ensureStrokeColor() {
        PageState state = currentState();
        if (state.strokeColor == null || !(state.strokeColor.equals(_desiredPageState.strokeColor))) {
            state.strokeColor = _desiredPageState.strokeColor;

            if (state.strokeColor instanceof FSRGBColor) {
                FSRGBColor rgb = (FSRGBColor) state.strokeColor;
                _cp.setStrokingColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
            } else if (state.strokeColor instanceof FSCMYKColor) {
                FSCMYKColor cmyk = (FSCMYKColor) state.strokeColor;
                _cp.setStrokingColor(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack());
            }
            else {
                assert(state.strokeColor instanceof FSRGBColor || state.strokeColor instanceof FSCMYKColor);
            }
        }
    }

    @Override
    public PdfContentStreamAdapter getCurrentPage() {
        return _cp;
    }

    @Override
    public PDPage getPage(){
        return _page;
    }

    private void followPath(Shape s, GraphicsOperation drawType) {
        if (s == null)
            return;

        ocManager.ensure();

        if (drawType == GraphicsOperation.STROKE) {
            if (!(_stroke instanceof BasicStroke)) {
                s = _stroke.createStrokedShape(s);
                followPath(s, GraphicsOperation.FILL);
                return;
            }
        }
        if (drawType == GraphicsOperation.STROKE) {
            setStrokeDiff(_stroke, _oldStroke);
            _oldStroke = _stroke;
            ensureStrokeColor();
        } else if (drawType == GraphicsOperation.FILL) {
            ensureFillColor();
        }
        
        PathIterator points;
        if (drawType == GraphicsOperation.CLIP) {
            points = s.getPathIterator(IDENTITY);
        } else {
            points = s.getPathIterator(_transform);
        }
        float[] coords = new float[6];
        int traces = 0;
        while (!points.isDone()) {
            ++traces;
            int segtype = points.currentSegment(coords);
            normalizeY(coords);
            switch (segtype) {
            case PathIterator.SEG_CLOSE:
                _cp.closeSubpath();
                break;

            case PathIterator.SEG_CUBICTO:
                _cp.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                break;

            case PathIterator.SEG_LINETO:
                _cp.lineTo(coords[0], coords[1]);
                break;

            case PathIterator.SEG_MOVETO:
                _cp.moveTo(coords[0], coords[1]);
                break;

            case PathIterator.SEG_QUADTO:
                _cp.curveTo(coords[0], coords[1], coords[2], coords[3]);
                break;
            }
            points.next();
        }
        
        switch (drawType) {
        case FILL:
            if (traces > 0) {
                if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                    _cp.fillEvenOdd();
                else
                    _cp.fillNonZero();
            }
            break;
        case STROKE:
            if (traces > 0)
                _cp.stroke();
            break;
        default: // drawType==CLIP
            if (traces == 0)
                _cp.addRect(0, 0, 0, 0);
            if (points.getWindingRule() == PathIterator.WIND_EVEN_ODD)
                _cp.clipEvenOdd();
            else
                _cp.clipNonZero();
            _cp.newPath();
        }
    }

    /**
     * Converts a top down unit to a bottom up PDF unit for the current page.
     */
    public float normalizeY(float y) {
        return _pageHeight - y;
    }
    
    /**
     * Converts a top down unit to a bottom up PDF unit for the specified page height.
     */
    @Override
    public float normalizeY(float y, float pageHeight) {
        return pageHeight - y;
    }

    private void normalizeY(float[] coords) {
        coords[1] = normalizeY(coords[1]);
        coords[3] = normalizeY(coords[3]);
        coords[5] = normalizeY(coords[5]);
    }

    private void setStrokeDiff(Stroke newStroke, Stroke oldStroke) {
        if (newStroke == oldStroke)
            return;
        if (!(newStroke instanceof BasicStroke))
            return;
        BasicStroke nStroke = (BasicStroke) newStroke;
        boolean oldOk = (oldStroke instanceof BasicStroke);
        BasicStroke oStroke = null;
        if (oldOk)
            oStroke = (BasicStroke) oldStroke;
        if (!oldOk || nStroke.getLineWidth() != oStroke.getLineWidth())
            _cp.setLineWidth(nStroke.getLineWidth());
        if (!oldOk || nStroke.getEndCap() != oStroke.getEndCap()) {
            switch (nStroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                _cp.setLineCap(0);
                break;
            case BasicStroke.CAP_SQUARE:
                _cp.setLineCap(2);
                break;
            default:
                _cp.setLineCap(1);
            }
        }
        if (!oldOk || nStroke.getLineJoin() != oStroke.getLineJoin()) {
            switch (nStroke.getLineJoin()) {
            case BasicStroke.JOIN_MITER:
                _cp.setLineJoin(0);
                break;
            case BasicStroke.JOIN_BEVEL:
                _cp.setLineJoin(2);
                break;
            default:
                _cp.setLineJoin(1);
            }
        }
        if (!oldOk || nStroke.getMiterLimit() != oStroke.getMiterLimit())
            _cp.setMiterLimit(nStroke.getMiterLimit());
        boolean makeDash;
        if (oldOk) {
            if (nStroke.getDashArray() != null) {
                if (nStroke.getDashPhase() != oStroke.getDashPhase()) {
                    makeDash = true;
                } else if (!java.util.Arrays.equals(nStroke.getDashArray(), oStroke.getDashArray())) {
                    makeDash = true;
                } else
                    makeDash = false;
            } else if (oStroke.getDashArray() != null) {
                makeDash = true;
            } else
                makeDash = false;
        } else {
            makeDash = true;
        }
        if (makeDash) {
            float[] dash = nStroke.getDashArray();
            if (dash == null)
                _cp.setLineDash(new float[] {}, 0);
            else {
                _cp.setLineDash(dash, nStroke.getDashPhase());
            }
        }
    }

    @Override
    public void setStroke(Stroke s) {
        _originalStroke = s;
        this._stroke = transformStroke(s);
    }

    private Stroke transformStroke(Stroke stroke) {
        if (!(stroke instanceof BasicStroke))
            return stroke;
        BasicStroke st = (BasicStroke) stroke;
        float scale = (float) Math.sqrt(Math.abs(_transform.getDeterminant()));
        float[] dash = st.getDashArray();
        if (dash != null) {
            for (int k = 0; k < dash.length; ++k)
                dash[k] *= scale;
        }
        return new BasicStroke(st.getLineWidth() * scale, st.getEndCap(), st.getLineJoin(), st.getMiterLimit(), dash, st.getDashPhase()
                * scale);
    }
    
    @Override
    public void popClip() {
        _cp.restoreGraphics();
        popState();
        clearPageState();
    }
    
    @Override
    public void pushClip(Shape s) {
        _cp.saveGraphics();
        pushState(currentState().copy());
        
        if (s != null) {
            Shape s1 = _transform.createTransformedShape(s);
            followPath(s1, GraphicsOperation.CLIP);
        }
    }

    @Override
    public Stroke getStroke() {
        return _originalStroke;
    }
    
    @Override
    public void realizeImage(PdfBoxImage img) {
        PDImageXObject xobject;
        try {
            xobject = PDImageXObject.createFromByteArray(_writer, img.getBytes(), img.getUri());
        } catch (IOException e) {
            throw new PdfContentStreamAdapter.PdfException("realizeImage", e);
        }
        img.clearBytes();
        img.setXObject(xobject);
    }

    @Override
    public void drawLinearGradient(FSLinearGradient backgroundLinearGradient, Shape bounds) {
        ocManager.ensure();

        PDShading shading = GradientHelper.createLinearGradient(this, getTransform(), backgroundLinearGradient, bounds);
        _cp.paintGradient(shading);
    }

    @Override
    public void drawImage(FSImage fsImage, int x, int y, boolean interpolate) {
        ocManager.ensure();

        PdfBoxImage img = (PdfBoxImage) fsImage;

        PDImageXObject xobject = img.getXObject();
		if (interpolate) {
		    // PDF/A does not support setting the interpolate flag to true.
		    if (!_pdfAConform) {
			xobject.setInterpolate(true);
		    }
		} else {
			/*
			 * Specialcase for not interpolating an image, default is to always interpolate.
			 * We must copy the image
			 */
			try {
				InputStream inputStream = xobject.getStream().getCOSObject().createRawInputStream();
				PDImageXObject cloneImage = new PDImageXObject(_writer, inputStream, COSName.FLATE_DECODE,
						xobject.getWidth(), xobject.getHeight(), xobject.getBitsPerComponent(),
						xobject.getColorSpace());
				cloneImage.setInterpolate(false);
				if (xobject.getSoftMask() != null)
					cloneImage.getCOSObject().setItem(COSName.SMASK, xobject.getSoftMask());
				inputStream.close();
				xobject = cloneImage;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
        

        AffineTransform transformer = (AffineTransform) getTransform().clone();
        transformer.translate(x, y);
        transformer.translate(0, img.getHeight());
        AffineTransform normalized = normalizeMatrix(transformer);
        normalized.scale(img.getWidth(), -img.getHeight());

        double[] mx = new double[6];
        normalized.getMatrix(mx);

        _cp.drawImage(xobject, (float) mx[4], (float) mx[5], (float) mx[0],
                (float) mx[3]);
    }
    
    @Override
    public void drawPdfAsImage(PDFormXObject _srcObject, Rectangle contentBounds, float intrinsicWidth, float intrinsicHeight) {
        ocManager.ensure();

        // We start with the page margins...
        AffineTransform af = AffineTransform.getTranslateInstance(
                getTransform().getTranslateX(),
                (_pageHeight) - getTransform().getTranslateY());
        
        // Then the x and y of this object...
        af.translate(contentBounds.getX() / _dotsPerPoint, -(contentBounds.getY() / _dotsPerPoint));

        // Scale to the desired height and width...
        AffineTransform scale = ReplacedElementScaleHelper.createScaleTransform(_dotsPerPoint, contentBounds, intrinsicWidth / _dotsPerPoint, intrinsicHeight / _dotsPerPoint);

        if (scale != null) {
            af.concatenate(scale);
        }
        
        // And take into account the height of the drawn feature...
        // And yes these transforms were all determined by trial and error!
        af.translate(0, -((intrinsicHeight / _dotsPerPoint)));
            
        _cp.saveGraphics();
        _cp.applyPdfMatrix(af);
        _cp.drawXForm(_srcObject);
        _cp.restoreGraphics();
    }

    @Override
    public float getDotsPerPoint() {
        return _dotsPerPoint;
    }

    @Override
    public void start(Document doc) {
        _bmManager = new PdfBoxBookmarkManager(doc, _writer, _sharedContext, _dotsPerPoint, this);
        _linkManager = new PdfBoxFastLinkManager(_sharedContext, _dotsPerPoint, _root, this);
        loadMetadata(doc);
        
        if (_pdfUaConform) {
            _pdfUa = new PdfBoxAccessibilityHelper(this, _root, doc);
        }
    }

    @Override
    public void finish(RenderingContext c, Box root) {
        if (_pdfUa != null) {
            _pdfUa.finishPdfUa();
        }

        // Bookmarks must come after PDF/UA structual tree creation
        // because bookmarks link to structual elements in the tree.
        _bmManager.loadBookmarks();
        _bmManager.writeOutline(c, root);

        // Also need access to the structure tree.
        processControls();
        _linkManager.processLinks(_pdfUa);
        
        if (_pdfUa != null) {
            _pdfUa.finishNumberTree();
        }
    }
    
    @Override
    public int getPageRefY(Box box) {
        if (box instanceof InlineLayoutBox) {
            InlineLayoutBox iB = (InlineLayoutBox) box;
            return iB.getAbsY() + iB.getBaseline();
        } else {
            return box.getAbsY();
        }
    }

    // Metadata methods
    // Methods to load and search a document's metadata

    /**
     * Appends a name/content metadata pair to this output device. A name or
     * content value of null will be ignored.
     * 
     * @param name
     *            the name of the metadata element to add.
     */
    @Override
    public void addMetadata(String name, String value) {
        if ((name != null) && (value != null)) {
            Metadata m = new Metadata(name, value);
            _metadata.add(m);
        }
    }

    /**
     * Searches the metadata name/content pairs of the current document and
     * returns the content value from the first pair with a matching name. The
     * search is case insensitive.
     * 
     * @param name
     *            the metadata element name to locate.
     * @return the content value of the first found metadata element; otherwise
     *         null.
     */
    @Override
    public String getMetadataByName(String name) {
        if (name != null) {
            for (Metadata m : _metadata) {
                if ((m != null) && m.getName().equalsIgnoreCase(name)) {
                    return m.getContent();
                }
            }
        }
        return null;
    }

    /**
     * Searches the metadata name/content pairs of the current document and
     * returns any content values with a matching name in an ArrayList. The
     * search is case insensitive.
     * 
     * @param name
     *            the metadata element name to locate.
     * @return an ArrayList with matching content values; otherwise an empty
     *         list.
     */
    @Override
    public List<String> getMetadataListByName(String name) {
        List<String> result = new ArrayList<>();
        if (name != null) {
            for (Metadata m : _metadata) {
                if ((m != null) && m.getName().equalsIgnoreCase(name)) {
                    result.add(m.getContent());
                }
            }
        }
        return result;
    }

    /**
     * Locates and stores all metadata values in the document head that contain
     * name/content pairs. If there is no pair with a name of "title", any
     * content in the title element is saved as a "title" metadata item.
     * 
     * @param doc
     *            the Document level node of the parsed xhtml file.
     */
    private void loadMetadata(Document doc) {
        Element head = DOMUtil.getChild(doc.getDocumentElement(), "head");
        if (head != null) {
            List<Element> l = DOMUtil.getChildren(head, "meta");
            if (l != null) {
                for (Element e : l) {
                    String name = e.getAttribute("name");
                    if (name != null) { // ignore non-name metadata data
                        String content = e.getAttribute("content");
                        Metadata m = new Metadata(name, content);
                        _metadata.add(m);
                    }
                }
            }
            // If there is no title meta data attribute, use the document title.
            String title = getMetadataByName("title");
            if (title == null) {
                Element t = DOMUtil.getChild(head, "title");
                if (t != null) {
                    title = DOMUtil.getText(t).trim();
                    Metadata m = new Metadata("title", title);
                    _metadata.add(m);
                }
            }
        }
    }

    /**
     * @return All metadata entries
     */
    @Override
    public List<Metadata> getMetadata() {
        return _metadata;
    }

    @Override
    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    @Override
    public void setSharedContext(SharedContext sharedContext) {
        _sharedContext = sharedContext;
        sharedContext.getCss().setSupportCMYKColors(true);
    }

    @Override
    public void setRoot(Box root) {
        _root = root;
    }

    @Override
    public int getStartPageNo() {
        return _startPageNo;
    }

    @Override
    public void setStartPageNo(int startPageNo) {
        _startPageNo = startPageNo;
    }

    @Override
    public void drawSelection(RenderingContext c, InlineText inlineText) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupportsSelection() {
        return false;
    }

    @Override
    public boolean isSupportsCMYKColors() {
        return true;
    }

    @Override
    public void drawWithGraphics(float x, float y, float width, float height, OutputDeviceGraphicsDrawer renderer) {
        ocManager.ensure();

        try {
            PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(_writer, (int) width, (int) height);
			/*
			 * Create and set the fontTextDrawer to perform the font mapping.
			 */
            if (_fontTextDrawer == null) {
                _fontTextDrawer = new PdfBoxGraphics2DFontTextDrawer() {
                    @Override
                    protected PDFont mapFont(Font font, IFontTextDrawerEnv env) {
                        FontSpecification spec = new FontSpecification();
                        spec.size = font.getSize();
                        spec.families = new String[] { font.getFamily() };
                        spec.fontStyle = IdentValue.NORMAL;
                        spec.fontWeight = IdentValue.NORMAL;
                        spec.variant = IdentValue.NORMAL;
                        if ((font.getStyle() & Font.BOLD) == Font.BOLD) {
                            spec.fontWeight = IdentValue.FONT_WEIGHT_700;
                        }
                        if ((font.getStyle() & Font.ITALIC) == Font.ITALIC) {
                            spec.fontStyle = IdentValue.ITALIC;
                        }
                        PdfBoxFSFont fsFont = (PdfBoxFSFont) getSharedContext().getFontResolver()
                                .resolveFont(getSharedContext(), spec);
                        FontDescription fontDescription = fsFont.getFontDescription().get(0);
						/*
						 * Detect the default fallback value
						 */
                        if (fsFont.getFontDescription().size() == 1) {
                            if (fontDescription.getFont().getName().equals("Times-Roman")
                                    && !(font.getFamily().equals("Times New Roman"))) {
								/*
								 * We did not find the font, this is the generic default fallback font.
								 * So use the vectorized text shapes.
								 */
                                return null;
                            }
                        }
                        return fontDescription.getFont();
                    }
                };
            }
            pdfBoxGraphics2D.setFontTextDrawer(_fontTextDrawer);

            /*
             * Do rendering
             */
            renderer.render(pdfBoxGraphics2D);
            /*
             * Dispose to close the XStream
             */
            pdfBoxGraphics2D.dispose();

            /*
             * We convert from 72dpi of the Graphics2D device to our 96dpi
             * using the output matrix of the XForm object.
             * FIXME: Probably want to make this configurable.
             */
            PDFormXObject xFormObject = pdfBoxGraphics2D.getXFormObject();
            xFormObject.setMatrix(AffineTransform.getScaleInstance(72f / 96f, 72f / 96f));
            
            /*
             * Adjust the y to take into account that the y passed to placeXForm below
             * refers to the bottom left of the object while we were passed in y the 
             * position of the top left corner.
             * FIXME: Make DPI conversion configurable (as above).
             */
            y += (height) * _dotsPerPoint * (72f / 96f);

            /*
             * Use the page transform to convert from _dotsPerPoint units to 
             * PDF units. Also takes care of page margins.
             */
            Point2D p = new Point2D.Float(x, y);
            Point2D pResult = new Point2D.Float();
            _transform.transform(p, pResult);

            /*
             * And then stamp it
             */
            _cp.placeXForm((float) pResult.getX(), _pageHeight - (float) pResult.getY(), xFormObject);
        }
        catch(IOException e){
            throw new RuntimeException("Error while drawing on Graphics2D", e);
        }
    }

    @Override
    public List<PagePosition<Box>> findPagePositionsByID(CssContext c, Pattern pattern) {
        Map<String, Box> idMap = _sharedContext.getIdMap();
        if (idMap == null) {
            return Collections.emptyList();
        }

        return
        idMap.entrySet()
             .stream()
             .filter(entry -> pattern.matcher(entry.getKey()).find())
             .map(entry -> calcPDFPagePosition(c, entry.getKey(), entry.getValue()))
             .filter(Objects::nonNull)
             .sorted(Comparator.comparing(PagePosition<Box>::getPageNo))
             .collect(Collectors.toList());
    }

    private PagePosition<Box> calcPDFPagePosition(CssContext c, String id, Box box) {
        PageBox page = _root.getLayer().getLastPage(c, box);
        if (page == null) {
            return null;
        }

        float x = box.getAbsX() + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
        float y = (page.getBottom() - (box.getAbsY() + box.getHeight())) + page.getMarginBorderPadding(c, CalculatedStyle.BOTTOM);
        x /= _dotsPerPoint;
        y /= _dotsPerPoint;

        return new PagePosition<Box>(
                id, box, page.getPageNo(), x, y, box.getEffectiveWidth() / _dotsPerPoint, box.getHeight() / _dotsPerPoint);
    }

    @Override
    public void setRenderingContext(RenderingContext result) {
        _renderingContext = result;
    }

    @Override
    public void setBidiReorderer(BidiReorderer reorderer) {
        _reorderer = reorderer;
    }
    
    @Override
    public void setPaint(Paint paint) {
        if (paint instanceof Color) {
            Color c = (Color) paint;
            this.setColor(new FSRGBColor(c.getRed(), c.getGreen(), c.getBlue()));
        } else {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.RENDER_UNKNOWN_PAINT, paint.getClass().getCanonicalName());
        }
    }

    @Override
    public boolean isPDF() {
        return true;
    }

    /**
     * Perform any internal cleanup needed
     */
    @Override
    public void close() {
        OpenUtil.closeQuietly(_fontTextDrawer);
    }

    private AffineTransform normalizeTransform(AffineTransform transform) {
        double[] mx = new double[6];
        transform.getMatrix(mx);
        mx[4] /= _dotsPerPoint;
        mx[5] /= _dotsPerPoint;
        return new AffineTransform(mx);
    }

    @Override
    public void pushTransformLayer(AffineTransform transform) {
        _cp.saveGraphics();
        pushState(currentState().copy());
        AffineTransform normalized = normalizeTransform(transform);
        _cp.applyPdfMatrix(normalized);
    }

    @Override
    public void popTransformLayer() {
        _cp.restoreGraphics();
        popState();
        clearPageState();
    }
    
    @Override
    public boolean isFastRenderer() {
        return true;
    }
    
    private void clearPageState() {
        _oldStroke = null;
    }

    @Override
    public Object startStructure(StructureType type, Box box) {
        ocManager.onStructureStart(type, box);

        return _pdfUa != null ? _pdfUa.startStructure(type, box) : null;
    }

    @Override
    public void endStructure(Object token) {
        ocManager.onStructureEnd();

        if (_pdfUa != null) {
            _pdfUa.endStructure(token);
        }
    }
    
}

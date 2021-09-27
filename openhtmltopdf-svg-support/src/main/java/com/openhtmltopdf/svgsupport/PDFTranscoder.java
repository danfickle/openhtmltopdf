package com.openhtmltopdf.svgsupport;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.extend.UserAgentCallback;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.simple.extend.ReplacedElementScaleHelper;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.FontFace;
import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.transcoder.ErrorHandler;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class PDFTranscoder extends SVGAbstractTranscoder {
	private OpenHtmlFontResolver fontResolver;
	private OutputDevice outputDevice;
	private double x;
	private double y;
	private final Box box;
	private RenderingContext ctx;
	private final double dotsPerPixel;
    private boolean allowScripts = false;
    private boolean allowExternalResources = false;
	private UserAgentCallback userAgentCallback;
	private Set<String> allowedProtocols;

	public PDFTranscoder(Box box, double dotsPerPixel, double width, double height) {
	    this.box = box;
		this.width = (float)width;
		this.height = (float)height;
		this.dotsPerPixel = dotsPerPixel;
	}
	
	public void setRenderingParameters(OutputDevice od, RenderingContext ctx, double x, double y, OpenHtmlFontResolver fontResolver, UserAgentCallback userAgentCallback) {
	    this.x = x;
            this.y = y;
            this.outputDevice = od;
            this.ctx = ctx;
            this.fontResolver = fontResolver;
            this.userAgentCallback = userAgentCallback;
	}

	@Override
        public void setImageSize(float docWidth, float docHeight) {
            super.setImageSize(docWidth, docHeight);
        }
	
	public float getWidth() {
	    return this.width;
	}
	
	public float getHeight() {
	    return this.height;
	}

    public static class OpenHtmlFontResolver implements FontFamilyResolver {
		private final Map<String, OpenHtmlGvtFontFamily> families = new HashMap<>(4);

		@Override
		public GVTFontFamily resolve(String arg0, FontFace arg1) {
			return null;
		}
		
		@Override
		public GVTFontFamily resolve(String family) {
			if (families.containsKey(family))
				return families.get(family);
			
			return null;
		}
		
		@Override
		public GVTFontFamily loadFont(InputStream arg0, FontFace arg1)
				throws Exception {
			return null;
		}
		
		@Override
		public GVTFontFamily getFamilyThatCanDisplay(char arg0) {
			return null;
		}
		
		@Override
		public GVTFontFamily getDefault() {
			return null;
		}
		
		private Float getStyle(IdentValue fontStyle) {
			if (fontStyle == IdentValue.ITALIC ||
				fontStyle == IdentValue.OBLIQUE)
				return TextAttribute.POSTURE_OBLIQUE;
			
			return null;
		}

        private Float getStyle(FontStyle style) {
            switch (style) {
            case ITALIC:
            case OBLIQUE:
                return TextAttribute.POSTURE_OBLIQUE;
            case NORMAL:
            default:
                return 0f;
            }
        }

        private Float getWeight(Integer weight) {
            if (weight == null) {
                return null;
            }

            switch (weight.intValue()) {
            case 100:
                return TextAttribute.WEIGHT_EXTRA_LIGHT;
            case 200:
            case 300:
                return TextAttribute.WEIGHT_LIGHT;
            case 400:
                return TextAttribute.WEIGHT_REGULAR;
            case 500:
            case 600:
                return TextAttribute.WEIGHT_SEMIBOLD;
            case 700:
                return TextAttribute.WEIGHT_BOLD;
            case 800:
                return TextAttribute.WEIGHT_EXTRABOLD;
            case 900:
                return TextAttribute.WEIGHT_ULTRABOLD;
            default:
                return null;
            }
        }

		private Float getWeight(IdentValue weight) {
	        if (weight == IdentValue.NORMAL) {
	            return TextAttribute.WEIGHT_REGULAR;
	        } else if (weight == IdentValue.BOLD) {
	            return TextAttribute.WEIGHT_BOLD;
	        } else if (weight == IdentValue.FONT_WEIGHT_100) {
	            return TextAttribute.WEIGHT_EXTRA_LIGHT;
	        } else if (weight == IdentValue.FONT_WEIGHT_200) {
	            return TextAttribute.WEIGHT_LIGHT;
	        } else if (weight == IdentValue.FONT_WEIGHT_300) {
	            return TextAttribute.WEIGHT_LIGHT;
	        } else if (weight == IdentValue.FONT_WEIGHT_400) {
	            return TextAttribute.WEIGHT_MEDIUM;
	        } else if (weight == IdentValue.FONT_WEIGHT_500) {
	            return TextAttribute.WEIGHT_SEMIBOLD;
	        } else if (weight == IdentValue.FONT_WEIGHT_600) {
	            return TextAttribute.WEIGHT_SEMIBOLD;
	        } else if (weight == IdentValue.FONT_WEIGHT_700) {
	            return TextAttribute.WEIGHT_BOLD;
	        } else if (weight == IdentValue.FONT_WEIGHT_800) {
	            return TextAttribute.WEIGHT_EXTRABOLD;
	        } else if (weight == IdentValue.FONT_WEIGHT_900) {
	            return TextAttribute.WEIGHT_ULTRABOLD;
	        } else if (weight == IdentValue.LIGHTER) {
	            // FIXME
	            return TextAttribute.WEIGHT_MEDIUM;
	        } else if (weight == IdentValue.BOLDER) {
	            // FIXME
	            return TextAttribute.WEIGHT_MEDIUM;
	        }
	        else {
	        	return null;
	        }
		}
		
		private void addFontFaceFont(
	            String fontFamilyNameOverride, IdentValue fontWeightOverride, IdentValue fontStyleOverride, String uri, byte[] font1)
	            throws FontFormatException {
			
			OpenHtmlGvtFontFamily family;
			
			if (families.containsKey(fontFamilyNameOverride))
				family = families.get(fontFamilyNameOverride);
			else {
				family = new OpenHtmlGvtFontFamily(fontFamilyNameOverride);
				families.put(fontFamilyNameOverride, family);
			}
			
			family.addFont(font1, 1, getWeight(fontWeightOverride), getStyle(fontStyleOverride));
	    }
		
		
		public void importFontFaces(List<FontFaceRule> fontFaces, SharedContext ctx) {
			 for (FontFaceRule rule : fontFaces) {
	            CalculatedStyle style = rule.getCalculatedStyle();

		         FSDerivedValue src = style.valueByName(CSSName.SRC);
		         if (src == IdentValue.NONE) {
		            continue;
		         }

		         byte[] font1 = ctx.getUserAgentCallback().getBinaryResource(src.asString(), ExternalResourceType.FONT);
		         if (font1 == null) {
		         	XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT, src.asString());
		             continue;
		         }
		         
		         String fontFamily = null;
		         IdentValue fontWeight = null;
		         IdentValue fontStyle = null;

		         if (rule.hasFontFamily()) {
		            fontFamily = style.valueByName(CSSName.FONT_FAMILY).asString();
		         }

		         if (rule.hasFontWeight()) {
		            fontWeight = style.getIdent(CSSName.FONT_WEIGHT);
		         }

		         if (rule.hasFontStyle()) {
		            fontStyle = style.getIdent(CSSName.FONT_STYLE);
		         }

		         try {
					addFontFaceFont(fontFamily, fontWeight, fontStyle, src.asString(), font1);
				} catch (FontFormatException e) {
		         	XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_SVG_COULD_NOT_READ_FONT, e);
					continue;
				}
		    }
		 }

        public void addFontFile(File fontFile, String family, Integer weight, FontStyle style) throws IOException, FontFormatException {
            OpenHtmlGvtFontFamily fontFamily = this.families.computeIfAbsent(family, fam -> new OpenHtmlGvtFontFamily(fam));
            // 12 seems to be the default font-size for SVG so use it as our base font size.
            fontFamily.addFont(fontFile, 12, getWeight(weight), getStyle(style));
        }
    }

    public void setSecurityOptions(boolean allowScripts, boolean allowExternalResources, Set<String> allowedProtocols) {
        this.allowScripts = allowScripts;
        this.allowExternalResources = allowExternalResources;
        this.allowedProtocols = allowedProtocols;
    }

    @Override
    protected BridgeContext createBridgeContext(String svgVersion) {
        return SVGImageExtension.newBridge(svgVersion, userAgent, userAgentCallback);
    }

	@Override
	protected void transcode(Document svg, String uri, TranscoderOutput out) throws TranscoderException {
		
		// Note: We have to initialize user agent here and not in ::createUserAgent() as method
		// is called before our constructor is called in the super constructor.
		this.userAgent = new OpenHtmlUserAgent(this.fontResolver, this.allowScripts, this.allowExternalResources, this.allowedProtocols);
		super.transcode(svg, uri, out);
		
        Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), ctx);

        final AffineTransform scale2 = ReplacedElementScaleHelper.createScaleTransform(this.dotsPerPixel, contentBounds, width, height);
        final AffineTransform inverse2 = ReplacedElementScaleHelper.inverseOrNull(scale2);
        final boolean transformed2 = scale2 != null && inverse2 != null;
        
		outputDevice.drawWithGraphics(
		        (float) x,
		        (float) y,
		        (float) (contentBounds.width / this.dotsPerPixel), 
		        (float) (contentBounds.height / this.dotsPerPixel),
		        new OutputDeviceGraphicsDrawer() {
			@Override
			public void render(Graphics2D graphics2D) {
			    if (transformed2) {
			        graphics2D.transform(scale2);
			    }
				/*
				 * Do the real paint
				 */
				PDFTranscoder.this.root.paint(graphics2D);
				
				if (transformed2) {
				    graphics2D.transform(inverse2);
				}
			}
		});
	}
	
	@Override
	protected org.apache.batik.bridge.UserAgent createUserAgent() {
		return null;
	}
	
	@Override
	public ErrorHandler getErrorHandler() {
		return new ErrorHandler() {
			@Override
			public void warning(TranscoderException arg0) throws TranscoderException {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_SVG_ERROR_HANDLER, "WARN", arg0);
			}
			
			@Override
			public void fatalError(TranscoderException arg0) throws TranscoderException {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_SVG_ERROR_HANDLER, "FATAL", arg0);
			}
			
			@Override
			public void error(TranscoderException arg0) throws TranscoderException {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_SVG_ERROR_HANDLER, "ERROR", arg0);
			}
		};
	}
}

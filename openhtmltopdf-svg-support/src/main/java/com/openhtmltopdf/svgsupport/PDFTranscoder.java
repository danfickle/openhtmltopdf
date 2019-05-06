package com.openhtmltopdf.svgsupport;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;
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
import java.awt.geom.NoninvertibleTransformException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDFTranscoder extends SVGAbstractTranscoder {
	private OpenHtmlFontResolver fontResolver;
	private OutputDevice outputDevice;
	private double x;
	private double y;
	private final Box box;
	private RenderingContext ctx;
	private final double dotsPerPixel;

	public PDFTranscoder(Box box, double dotsPerPixel, double width, double height) {
	    this.box = box;
		this.width = (float)width;
		this.height = (float)height;
		this.dotsPerPixel = dotsPerPixel;
	}
	
	public void setRenderingParameters(OutputDevice od, RenderingContext ctx, double x, double y, OpenHtmlFontResolver fontResolver) {
	    this.x = x;
            this.y = y;
            this.outputDevice = od;
            this.ctx = ctx;
            this.fontResolver = fontResolver;
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
		private final Map<String, OpenHtmlGvtFontFamily> families = new HashMap<String, OpenHtmlGvtFontFamily>(4);

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

		         byte[] font1 = ctx.getUac().getBinaryResource(src.asString());
		         if (font1 == null) {
		             XRLog.exception("Could not load font " + src.asString());
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
					XRLog.exception("Couldn't read font", e);
					continue;
				}
		    }
		 }
	}
	
	@Override
	protected void transcode(Document svg, String uri, TranscoderOutput out) throws TranscoderException {
		
		// Note: We have to initialize user agent here and not in ::createUserAgent() as method
		// is called before our constructor is called in the super constructor.
		this.userAgent = new OpenHtmlUserAgent(this.fontResolver);
		super.transcode(svg, uri, out);
		
        int intrinsicWidth = (int) width;
        int intrinsicHeight = (int) height;
        
        Rectangle contentBounds = box.getContentAreaEdge(box.getAbsX(), box.getAbsY(), ctx);
        
        int desiredWidth = (int) (contentBounds.width / this.dotsPerPixel);
        int desiredHeight = (int) (contentBounds.height / this.dotsPerPixel);
        
        boolean transformed = false;
        AffineTransform scale = null;
        
        if (width == 0 || height == 0) {
            // Do nothing...
        }
        else if (desiredWidth > intrinsicWidth &&
            desiredHeight > intrinsicHeight) {
           
            double rw = (double) desiredWidth / width;
            double rh = (double) desiredHeight / height;
            
            double factor = Math.min(rw, rh);
            scale = AffineTransform.getScaleInstance(factor, factor);
            transformed = true;
        } else if (desiredWidth < intrinsicWidth &&
                   desiredHeight < intrinsicHeight) {
            double rw = (double) desiredWidth / width;
            double rh = (double) desiredHeight / height;
            
            double factor = Math.max(rw, rh);
            scale = AffineTransform.getScaleInstance(factor, factor);
            transformed = true;
        }
        
        AffineTransform inverseScale = null;
        try {
            if (transformed) {
                inverseScale = scale.createInverse();
            }
        } catch (NoninvertibleTransformException e) {
            transformed = false;
        }
        final AffineTransform scale2 = scale;
        final AffineTransform inverse2 = inverseScale;
        final boolean transformed2 = transformed; 
        
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
				XRLog.exception("SVG WARN", arg0);
			}
			
			@Override
			public void fatalError(TranscoderException arg0) throws TranscoderException {
				XRLog.exception("SVG FATAL", arg0);
			}
			
			@Override
			public void error(TranscoderException arg0) throws TranscoderException {
				XRLog.exception("SVG ERROR", arg0);
			}
		};
	}
}

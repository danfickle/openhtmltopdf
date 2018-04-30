package com.openhtmltopdf.render.displaylist;

import java.awt.geom.AffineTransform;
import java.util.List;
import org.w3c.dom.css.CSSPrimitiveValue;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.ListValue;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;

/**
 * Static helper methods to create transforms, either in document coordinates or page coordinates.
 */
public class TransformCreator {
	
	private static enum TransformYOrigin {
		DOCUMENT_TOP,
		PAGE_TOP,
		PAGE_BOTTOM;
	}
	
	/**
	 * Creates a transform that can be applied to a page, either PDF or Java2D.
	 * This transform can be applied on top of other tranforms already in effect on the page.
	 */
	public static AffineTransform createPageCoordinatesTranform(RenderingContext c, Box box, PageBox page) {
		TransformYOrigin yOrigin = c.getOutputDevice().isPDF() ? TransformYOrigin.PAGE_BOTTOM : TransformYOrigin.PAGE_TOP;
		
		AffineTransform start = new AffineTransform();
		
		createTransform(c, box, page, start, yOrigin);
		
		return start;
	}
	
	/**
	 * Creates an absolute transform in document coordinates. This is typically used to figure out what pages the box will
	 * fall on. The <code>_parentCtm</code> may be null in case the parent layer uses the identity tranform. If it is not null
	 * it will be cloned before use.
	 */
	public static AffineTransform createDocumentCoordinatesTransform(Box master, CssContext c, AffineTransform _parentCtm) {
		AffineTransform ctm = _parentCtm == null ? new AffineTransform() : (AffineTransform) _parentCtm.clone();
		
		createTransform(c, master, null, ctm, TransformYOrigin.DOCUMENT_TOP);
		
		return ctm;
	}
	
	/**
	 * <code>page</code> may be null in the case that <code>transformYOrigin</code> is <code>DOCUMENT_TOP</code>. Otherwise, every argument is required.
	 */
	private static void createTransform(CssContext c, Box box, PageBox page, AffineTransform result, TransformYOrigin transformYOrigin) {
	
		FSDerivedValue transforms = box.getStyle().valueByName(CSSName.TRANSFORM);

		float relOriginX = box.getStyle().getFloatPropertyProportionalWidth(CSSName.FS_TRANSFORM_ORIGIN_X,
				box.getWidth(), c);
		float relOriginY = box.getStyle().getFloatPropertyProportionalHeight(CSSName.FS_TRANSFORM_ORIGIN_Y,
				box.getHeight(), c);

		float flipFactor = transformYOrigin == TransformYOrigin.PAGE_BOTTOM ? -1 : 1;

		float absTranslateX = relOriginX + box.getAbsX();
		float absTranslateY = relOriginY + box.getAbsY();
		
		AffineTransform translateToOrigin;
		AffineTransform translateBackFromOrigin;
		
		if (transformYOrigin == TransformYOrigin.PAGE_BOTTOM || transformYOrigin == TransformYOrigin.PAGE_TOP) {
			float pageTranslateX;
			float pageTranslateY;
		
			if (transformYOrigin == TransformYOrigin.PAGE_BOTTOM) {
				// The transform point is the lower left of the page (PDF coordinate system).
				pageTranslateX = absTranslateX + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
				float topDownPageTranslateY = (absTranslateY + page.getMarginBorderPadding(c, CalculatedStyle.TOP)) - page.getPaintingTop();
				pageTranslateY = (page.getHeight(c) - topDownPageTranslateY);
			} else { // PAGE_TOP
				// The transform point is the upper left of the page.
				pageTranslateX = absTranslateX + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
				pageTranslateY = (absTranslateY - page.getPaintingTop()) + page.getMarginBorderPadding(c, CalculatedStyle.TOP); 
			}
		
			translateToOrigin = AffineTransform.getTranslateInstance(pageTranslateX, pageTranslateY);
			translateBackFromOrigin = AffineTransform.getTranslateInstance(-pageTranslateX, -pageTranslateY);
		} else { // DOCUMENT_TOP
			translateToOrigin = AffineTransform.getTranslateInstance(absTranslateX, absTranslateY);
			translateBackFromOrigin = AffineTransform.getTranslateInstance(-absTranslateX, -absTranslateY);
		}
		
		List<PropertyValue> transformList = (List<PropertyValue>) ((ListValue) transforms).getValues();

		result.concatenate(translateToOrigin);
		applyTransformFunctions(flipFactor, transformList, result, box, c);
		result.concatenate(translateBackFromOrigin);
	}

	private static void applyTransformFunctions(float flipFactor, List<PropertyValue> transformList, AffineTransform result, Box box, CssContext ctx) {
		for (PropertyValue transform : transformList) {
			String fName = transform.getFunction().getName();
			List<PropertyValue> params = transform.getFunction().getParameters();

			if ("rotate".equalsIgnoreCase(fName)) {
				float radians = flipFactor * convertAngleToRadians(params.get(0));
				result.concatenate(AffineTransform.getRotateInstance(radians));
			} else if ("scale".equalsIgnoreCase(fName) ||
					   "scalex".equalsIgnoreCase(fName)
					|| "scaley".equalsIgnoreCase(fName)) {
				float scaleX = params.get(0).getFloatValue();
				float scaleY = params.get(0).getFloatValue();
				if (params.size() > 1)
					scaleY = params.get(1).getFloatValue();
				if ("scalex".equalsIgnoreCase(fName))
					scaleY = 1;
				if ("scaley".equalsIgnoreCase(fName))
					scaleX = 1;
				result.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
			} else if ("skew".equalsIgnoreCase(fName)) {
				float radiansX = flipFactor * convertAngleToRadians(params.get(0));
				float radiansY = 0;
				if (params.size() > 1)
					radiansY = convertAngleToRadians(params.get(1));
				result.concatenate(AffineTransform.getShearInstance(Math.tan(radiansX), Math.tan(radiansY)));
			} else if ("skewx".equalsIgnoreCase(fName)) {
				float radians = flipFactor * convertAngleToRadians(params.get(0));
				result.concatenate(AffineTransform.getShearInstance(Math.tan(radians), 0));
			} else if ("skewy".equalsIgnoreCase(fName)) {
				float radians = flipFactor * convertAngleToRadians(params.get(0));
				result.concatenate(AffineTransform.getShearInstance(0, Math.tan(radians)));
			} else if ("matrix".equalsIgnoreCase(fName)) {
				result.concatenate(new AffineTransform(params.get(0).getFloatValue(), params.get(1).getFloatValue(),
								params.get(2).getFloatValue(), params.get(3).getFloatValue(),
								params.get(4).getFloatValue(), params.get(5).getFloatValue()));
			} else if ("translate".equalsIgnoreCase(fName)) {
				float translateX = LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						params.get(0).getFloatValue(), params.get(0).getPrimitiveType(), box.getWidth(), ctx);
				
				float translateY = params.size() > 1 ? 
						LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						    params.get(1).getFloatValue(), params.get(0).getPrimitiveType(), box.getWidth(), ctx) : translateX;
						
				result.concatenate(AffineTransform.getTranslateInstance(translateX, flipFactor * translateY));	
			} else if ("translateX".equalsIgnoreCase(fName)) {
				float translateX = LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						params.get(0).getFloatValue(), params.get(0).getPrimitiveType(), box.getWidth(), ctx);

				result.concatenate(AffineTransform.getTranslateInstance(translateX, 0));
			} else if ("translateY".equalsIgnoreCase(fName)) {
				float translateY = LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						params.get(0).getFloatValue(), params.get(0).getPrimitiveType(), box.getHeight(), ctx);

				result.concatenate(AffineTransform.getTranslateInstance(0, flipFactor * translateY));
			}
		}
	}
	
	private static float convertAngleToRadians(PropertyValue param) {
    	if (param.getPrimitiveType() == CSSPrimitiveValue.CSS_DEG) {
    		return (float) Math.toRadians(param.getFloatValue());
    	} else if (param.getPrimitiveType() == CSSPrimitiveValue.CSS_RAD) {
    		return param.getFloatValue();
    	} else { // if (param.getPrimitiveType() == CSSPrimitiveValue.CSS_GRAD)
    		return (float) (param.getFloatValue() * (Math.PI / 200));
    	}
    }
}

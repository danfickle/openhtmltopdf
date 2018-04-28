package com.openhtmltopdf.render.displaylist;

import java.awt.geom.AffineTransform;
import java.util.List;
import org.w3c.dom.css.CSSPrimitiveValue;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.ListValue;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.render.PageBox;
import com.openhtmltopdf.render.RenderingContext;

public class TransformCreator {
	public static AffineTransform createPageTranform(RenderingContext c, Box box, PageBox page) {
		FSDerivedValue transforms = box.getStyle().valueByName(CSSName.TRANSFORM);

		// By default the transform point is the lower left of the page, so we need to
		// translate to correctly apply transform.
		float relOriginX = box.getStyle().getFloatPropertyProportionalWidth(CSSName.FS_TRANSFORM_ORIGIN_X,
				box.getWidth(), c);
		float relOriginY = box.getStyle().getFloatPropertyProportionalHeight(CSSName.FS_TRANSFORM_ORIGIN_Y,
				box.getHeight(), c);

		float flipFactor = c.getOutputDevice().isPDF() ? -1 : 1;

		float absTranslateX = relOriginX + box.getAbsX();
		float absTranslateY = relOriginY + box.getAbsY();

		float pageTranslateX;
		float pageTranslateY;
		
		if (c.getOutputDevice().isPDF()) {
			pageTranslateX = absTranslateX + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
			float topDownPageTranslateY = (absTranslateY + page.getMarginBorderPadding(c, CalculatedStyle.TOP)) - page.getPaintingTop();
			pageTranslateY = (page.getHeight(c) - topDownPageTranslateY);
		} else {
			pageTranslateX = absTranslateX + page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
			pageTranslateY = (absTranslateY - page.getPaintingTop()) + page.getMarginBorderPadding(c, CalculatedStyle.TOP); 
		}

		List<PropertyValue> transformList = (List<PropertyValue>) ((ListValue) transforms).getValues();

		AffineTransform translateToOrigin = AffineTransform.getTranslateInstance(pageTranslateX, pageTranslateY);
		AffineTransform translateBackFromOrigin = AffineTransform.getTranslateInstance(-pageTranslateX, -pageTranslateY);

		AffineTransform result = new AffineTransform();
		result.concatenate(translateToOrigin);
		applyTransformFunctions(flipFactor, transformList, result, box, c);
		result.concatenate(translateBackFromOrigin);

		return result;
	}

	private static void applyTransformFunctions(float flipFactor, List<PropertyValue> transformList, AffineTransform result, Box box, RenderingContext ctx) {
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

package com.openhtmltopdf.layout;

import java.awt.geom.AffineTransform;
import java.util.List;
import org.w3c.dom.css.CSSPrimitiveValue;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.parser.PropertyValue;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.css.style.derived.LengthValue;
import com.openhtmltopdf.css.style.derived.ListValue;
import com.openhtmltopdf.render.Box;

public class TransformUtil {

	private TransformUtil() {
	}

	public static AffineTransform createDocumentCoordinatesTransform(Box master, CssContext c, AffineTransform _parentCtm) {
		FSDerivedValue transforms = master.getStyle().valueByName(CSSName.TRANSFORM);
		List<PropertyValue> transformList = (List<PropertyValue>) ((ListValue) transforms).getValues();
		
		float relOriginX = master.getStyle().getFloatPropertyProportionalWidth(CSSName.FS_TRANSFORM_ORIGIN_X,
				master.getWidth(), c);
		float relOriginY = master.getStyle().getFloatPropertyProportionalHeight(CSSName.FS_TRANSFORM_ORIGIN_Y,
				master.getHeight(), c);

		float absTranslateX = relOriginX + master.getAbsX();
		float absTranslateY = relOriginY + master.getAbsY();

		AffineTransform translateToOrigin = AffineTransform.getTranslateInstance(absTranslateX, absTranslateY);
		AffineTransform translateBackFromOrigin = AffineTransform.getTranslateInstance(-absTranslateX, -absTranslateY);

		AffineTransform ctm = _parentCtm == null ? new AffineTransform() : (AffineTransform) _parentCtm.clone();
		
		ctm.concatenate(translateToOrigin);
		applyDocumentCoordinatesTransformFunctions(c, ctm, master, transformList);
		ctm.concatenate(translateBackFromOrigin);
		
		return ctm;
	}
	
	private static void applyDocumentCoordinatesTransformFunctions(CssContext ctx, AffineTransform ctm, Box box, List<PropertyValue> transformList) {
		for (PropertyValue transform : transformList) {
			String fName = transform.getFunction().getName();
			List<PropertyValue> params = transform.getFunction().getParameters();

			if ("rotate".equalsIgnoreCase(fName)) {
				float radians = convertAngleToRadians(params.get(0));
				ctm.concatenate(AffineTransform.getRotateInstance(radians));
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
				ctm.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
			} else if ("skew".equalsIgnoreCase(fName)) {
				float radiansX = convertAngleToRadians(params.get(0));
				float radiansY = 0;
				if (params.size() > 1)
					radiansY = convertAngleToRadians(params.get(1));
				ctm.concatenate(AffineTransform.getShearInstance(Math.tan(radiansX), Math.tan(radiansY)));
			} else if ("skewx".equalsIgnoreCase(fName)) {
				float radians = convertAngleToRadians(params.get(0));
				ctm.concatenate(AffineTransform.getShearInstance(Math.tan(radians), 0));
			} else if ("skewy".equalsIgnoreCase(fName)) {
				float radians = convertAngleToRadians(params.get(0));
				ctm.concatenate(AffineTransform.getShearInstance(0, Math.tan(radians)));
			} else if ("matrix".equalsIgnoreCase(fName)) {
				ctm.concatenate(new AffineTransform(params.get(0).getFloatValue(), params.get(1).getFloatValue(),
								params.get(2).getFloatValue(), params.get(3).getFloatValue(),
								params.get(4).getFloatValue(), params.get(5).getFloatValue()));
			} else if ("translate".equalsIgnoreCase(fName)) {
				float translateX = LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						params.get(0).getFloatValue(), params.get(0).getPrimitiveType(), box.getWidth(), ctx);
				
				float translateY = params.size() > 1 ? 
						LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						    params.get(1).getFloatValue(), params.get(0).getPrimitiveType(), box.getWidth(), ctx) : translateX;
						
				ctm.concatenate(AffineTransform.getTranslateInstance(translateX, translateY));			
				
			} else if ("translateX".equalsIgnoreCase(fName)) {
				float translateX = LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						params.get(0).getFloatValue(), params.get(0).getPrimitiveType(), box.getWidth(), ctx);

				ctm.concatenate(AffineTransform.getTranslateInstance(translateX, 0));
				
			} else if ("translateY".equalsIgnoreCase(fName)) {

				float translateY = LengthValue.calcFloatProportionalValue(box.getStyle(), null, null,
						params.get(0).getFloatValue(), params.get(0).getPrimitiveType(), box.getHeight(), ctx);

				ctm.concatenate(AffineTransform.getTranslateInstance(0, translateY));
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

package com.openhtmltopdf.bidi.support;

import java.util.Locale;

import com.ibm.icu.lang.UCharacter;
import com.openhtmltopdf.extend.FSTextTransformer;

public class ICUTransformers {
	private ICUTransformers() { }
	
	public static class ICUToLowerTransformer implements FSTextTransformer {
		private final Locale lc;
		
		public ICUToLowerTransformer(Locale lc) {
			this.lc = lc;
		}
		
		@Override
		public String transform(String in) {
			return UCharacter.toLowerCase(lc, in);
		}
	}
	
	public static class ICUToUpperTransformer implements FSTextTransformer {
		private final Locale lc;
		
		public ICUToUpperTransformer(Locale lc) {
			this.lc = lc;
		}
		
		@Override
		public String transform(String in) {
			return UCharacter.toUpperCase(lc, in);
		}
	}
	
	public static class ICUToTitleTransformer implements FSTextTransformer {
		private final Locale lc;
		
		public ICUToTitleTransformer(Locale lc) {
			this.lc = lc;
		}
		
		@Override
		public String transform(String in) {
			return UCharacter.toTitleCase(lc, in, null, UCharacter.TITLECASE_NO_LOWERCASE);
		}
	}
}

package com.openhtmltopdf.bidi;

/**
 * Returns text unchanged.
 */
public class SimpleBidiReorderer implements BidiReorderer {

	@Override
	public String reorderRTLTextToLTR(String text) {
		return text;

// Commented out code, for use with testing only.
//    	char[] chars = text.toCharArray();
//		char[] out = new char[chars.length];
//		int j = 0;
//			
//		for (int i = chars.length - 1; i >= 0; i--)
//		{
//			out[j++] = chars[i];
//		}
//		
//		return String.valueOf(out, 0, out.length);
	}

	@Override
	public String shapeText(String text) {
		return text;
	}

	@Override
	public String deshapeText(String text) {
		return text;
	}

	@Override
	public boolean isLiveImplementation() {
		return false;
	}
}

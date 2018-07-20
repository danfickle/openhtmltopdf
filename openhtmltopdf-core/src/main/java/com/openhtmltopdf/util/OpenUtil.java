package com.openhtmltopdf.util;

public class OpenUtil {

	private OpenUtil() {}

	/**
	 * Checks if a code point is printable. If false, it can be safely discarded at the 
	 * rendering stage, else it should be replaced with the replacement character,
	 * if a suitable glyph can not be found.
	 * @param codePoint
	 * @return whether codePoint is printable
	 */
	public static boolean isCodePointPrintable(int codePoint) {
		if (Character.isISOControl(codePoint))
			return false;
		
		int category = Character.getType(codePoint);
		
		return !(category == Character.CONTROL ||
				 category == Character.FORMAT ||
				 category == Character.UNASSIGNED ||
				 category == Character.PRIVATE_USE ||
				 category == Character.SURROGATE);
	}
	
	public static Integer parseIntegerOrNull(String possibleInteger) {
	        try {
	            return Integer.parseInt(possibleInteger);
	        } catch (NumberFormatException e) {
	            return null;
	        }
	}
}

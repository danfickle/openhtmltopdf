package com.openhtmltopdf.util;

import java.util.Objects;

public class OpenUtil {

	private OpenUtil() {}

	/**
	 * Checks if a code point is printable. If false, it can be safely discarded at the 
	 * rendering stage, else it should be replaced with the replacement character,
	 * if a suitable glyph can not be found.
	 * 
	 * NOTE: This should only be called after a character has been shown to be
	 * NOT present in the font. It can not be called beforehand because some fonts
	 * contain private area characters and so on. Issue#588.
	 * 
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

    /**
     * Whether the code point should be passed through to the font
     * for rendering. It effectively filters out characters that
     * have been shown to be problematic in some (broken) fonts such
     * as visible soft-hyphens.
     */
    public static boolean isSafeFontCodePointToPrint(int codePoint) {
        switch (codePoint) {
        case 0xAD:        // Soft hyphen, PR#550, FALLTHRU
        case 0xFFFC:      // Object replacement character, Issue#564.
            return false;

        default:
            return true;
        }
    }

	/**
	 * Returns <code>true</code>, when all characters of the given string are printable.
	 * @param str a non-null string to test
	 * @return whether all characters are printable
	 */
	public static boolean areAllCharactersPrintable(String str) {
		Objects.requireNonNull(str, "str");
		return str.codePoints().allMatch(OpenUtil::isSafeFontCodePointToPrint);
	}

	public static Integer parseIntegerOrNull(String possibleInteger) {
	        try {
	            return Integer.parseInt(possibleInteger);
	        } catch (NumberFormatException e) {
	            return null;
	        }
	}
}

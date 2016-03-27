package com.openhtmltopdf.bidi;

/**
 * An interface to provide text reordering services.
 * Must use a proper algorithm rather than reverse string to allow
 * for surrogate pairs, control characters, etc.
 */
public interface BidiReorderer {
	public String reorderRTLTextToLTR(String text);
	
	/**
	 * Arabic character shapes depends on whether a character is at the
	 * start, end or middle of a word. This algorithm aims to change the characters
	 * depending on their context.
	 * @param text
	 * @return
	 */
	public String shapeText(String text);
	
	/**
	 * Deshape text, for use if the shaped character is not in a font.
	 * @param text
	 * @return
	 */
	public String deshapeText(String text);

	/**
	 * Useful for optimization.
	 * @return
	 */
	public boolean isLiveImplementation();
}

package com.openhtmltopdf.render;

import java.awt.Shape;

/**
 * A display list item that indicates the output device needs
 * to clip at this point.
 */
public class OperatorClip implements DisplayListItem {
	private final Shape clip;
	
	public OperatorClip(Shape clip) {
		this.clip = clip;
	}
	
	public Shape getClip() {
		return this.clip;
	}
}

package com.openhtmltopdf.render;

import java.awt.Shape;

/**
 * A display list item that indicates the output device needs to expand
 * the clip at this point.
 */
public class OperatorSetClip implements DisplayListItem {
	private final Shape setClip;
	
	public OperatorSetClip(Shape setClip) {
		this.setClip = setClip;
	}
	
	public Shape getSetClipShape() {
		return this.setClip;
	}
}

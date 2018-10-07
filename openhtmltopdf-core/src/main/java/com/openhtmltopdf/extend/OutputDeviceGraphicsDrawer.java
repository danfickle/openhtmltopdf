package com.openhtmltopdf.extend;

import java.awt.*;

/**
 * Render something on a Graphics2D on the OutputDevice.
 *
 * Intented to be a FunctionalInterface in Java 8.
 */
public interface OutputDeviceGraphicsDrawer {

	/**
	 * Draw something using the given graphics. For PDFs it will be converted to vector drawings.
	 * @param graphics2D the graphics you can use to draw
	 */
	public void render(Graphics2D graphics2D);
}

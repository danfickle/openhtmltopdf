package com.openhtmltopdf.extend;

import org.w3c.dom.Element;

/**
 * Factory for ObjectDrawers, i.e. classes which draw &lt;object&gt; tags
 */
public interface FSObjectDrawerFactory {

	/**
	 * Determine an object drawer for the given object tag element.
	 */
	FSObjectDrawer createDrawer(Element e);
}

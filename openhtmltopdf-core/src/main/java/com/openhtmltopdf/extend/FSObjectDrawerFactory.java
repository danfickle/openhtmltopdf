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
	
	/**
	 * @param e eleemnt with tag name of <code>object</code>.
	 * @return true if this object drawer can handle this element.
	 */
	public boolean isReplacedObject(Element e);
}

package com.openhtmltopdf.render;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.FSObjectDrawerFactory;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Default FSObjectDrawer factory, which allows to register drawer for specified
 * content type
 */
public class DefaultObjectDrawerFactory implements FSObjectDrawerFactory {

	/**
	 * Maps content type => Drawer
	 */
	private final Map<String, FSObjectDrawer> drawerMap = new HashMap<String, FSObjectDrawer>();

	@Override
	public FSObjectDrawer createDrawer(Element e) {
		return drawerMap.get(e.getAttribute("type"));
	}

	/**
	 * @param contentType the content type this drawer is for
	 * @param drawer Drawer
	 */
	public void registerDrawer(String contentType, FSObjectDrawer drawer) {
		drawerMap.put(contentType,drawer);
	}
	
	@Override
	public boolean isReplacedObject(Element e) {
		return drawerMap.containsKey(e.getAttribute("type"));
	}
}

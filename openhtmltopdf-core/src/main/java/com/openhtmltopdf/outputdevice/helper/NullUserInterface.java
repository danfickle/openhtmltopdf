package com.openhtmltopdf.outputdevice.helper;

import org.w3c.dom.Element;

import com.openhtmltopdf.extend.UserInterface;

public class NullUserInterface implements UserInterface {
	public boolean isHover(Element e) {
		return false;
	}

	public boolean isActive(Element e) {
		return false;
	}

	public boolean isFocus(Element e) {
		return false;
	}
}

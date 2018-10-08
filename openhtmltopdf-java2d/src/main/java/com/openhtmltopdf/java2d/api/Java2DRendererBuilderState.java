package com.openhtmltopdf.java2d.api;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is an internal implementation detail. This class is only public
 * because there are no friend classes in Java. DO NOT USE!
 */
public class Java2DRendererBuilderState extends BaseRendererBuilder.BaseRendererBuilderState {
	/* Internal! */
	Java2DRendererBuilderState() {
	}

	public Graphics2D _layoutGraphics;
	public FSPageProcessor _pageProcessor;
	public final List<Java2DRendererBuilder.AddedFont> _fonts = new ArrayList<Java2DRendererBuilder.AddedFont>();
}

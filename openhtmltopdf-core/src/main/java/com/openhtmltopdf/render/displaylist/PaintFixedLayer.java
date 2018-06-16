package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.layout.Layer;

public final class PaintFixedLayer implements DisplayListOperation {
	private final Layer _layer;
	
	public PaintFixedLayer(Layer layer) {
		this._layer = layer;
	}

	public Layer getLayer() {
		return this._layer;
	}
}

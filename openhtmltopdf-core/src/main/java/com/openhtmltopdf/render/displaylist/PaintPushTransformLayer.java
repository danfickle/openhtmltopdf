package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;

public final class PaintPushTransformLayer implements DisplayListOperation {
	private final Box _master;

	public PaintPushTransformLayer(Box master) {
		this._master = master;
	}

	public Box getMaster() {
		return _master;
	}
}

package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;

public final class PaintPopTransformLayer implements DisplayListOperation {
	private final Box _master;
	
	public PaintPopTransformLayer(Box master) {
		_master = master;
	}

	public Box getMaster() {
		return _master;
	}
}

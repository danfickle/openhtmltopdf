package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.Box;

public final class PaintPushTransformLayer implements DisplayListOperation {
	private final Box _master;
	private final int _shadowPage;

	public PaintPushTransformLayer(Box master, int shadowPage) {
		this._master = master;
		this._shadowPage = shadowPage;
	}
	
	public int getShadowPageNumber() {
	    return _shadowPage;
	}

	public Box getMaster() {
		return _master;
	}
}

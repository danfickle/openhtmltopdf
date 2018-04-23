package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.BlockBox;

public final class PaintReplacedElement implements DisplayListOperation {
	private final BlockBox master;

	public PaintReplacedElement(BlockBox master) {
		this.master = master;
	}

	public BlockBox getMaster() {
		return master;
	}
}

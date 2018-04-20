package com.openhtmltopdf.render.displaylist;

import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.RenderingContext;

public final class PaintReplacedElement implements DisplayListOperation {
	private final RenderingContext context;
	private final BlockBox master;

	public PaintReplacedElement(RenderingContext c, BlockBox master) {
		this.context = c;
		this.master = master;
	}

	public RenderingContext getContext() {
		return context;
	}

	public BlockBox getMaster() {
		return master;
	}
}

package com.openhtmltopdf.java2d.api;

import java.io.IOException;
import java.io.OutputStream;

public interface FSPageOutputStreamSupplier {
	public OutputStream supply(int zeroBasedPageNumber) throws IOException;
}

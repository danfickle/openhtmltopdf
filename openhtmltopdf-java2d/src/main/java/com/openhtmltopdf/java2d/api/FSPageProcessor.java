package com.openhtmltopdf.java2d.api;

public interface FSPageProcessor {
	public FSPage createPage(int zeroBasedPageNumber, int width, int height);
	public void finishPage(FSPage pg);
}

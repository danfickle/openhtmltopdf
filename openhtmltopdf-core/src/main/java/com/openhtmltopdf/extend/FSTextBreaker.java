package com.openhtmltopdf.extend;

/**
 * Represents a text breaker, such as those on line break opportunities.
 * Implementations usually wrap a BreakIterator of some kind.
 * Will be reused many times during a run.
 */
public interface FSTextBreaker {
	public int next();
	public void setText(String newText);
}

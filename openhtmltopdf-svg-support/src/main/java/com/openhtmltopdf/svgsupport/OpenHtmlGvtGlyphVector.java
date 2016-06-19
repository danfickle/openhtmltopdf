package com.openhtmltopdf.svgsupport;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphJustificationInfo;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;

import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTGlyphMetrics;
import org.apache.batik.gvt.font.GVTGlyphVector;
import org.apache.batik.gvt.text.TextPaintInfo;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator.TextAttribute;


public class OpenHtmlGvtGlyphVector implements GVTGlyphVector {

	private final java.awt.font.GlyphVector vec;
	private final GVTFont font;
	private final FontRenderContext frc;
	
	public OpenHtmlGvtGlyphVector(java.awt.font.GlyphVector vec, GVTFont font, FontRenderContext frc) {
		this.vec = vec;
		this.font = font;
		this.frc = frc;
	}
	
	@Override
	public void draw(Graphics2D g2d, AttributedCharacterIterator arg1) {
		if (arg1.getAttribute(TextAttribute.PAINT_INFO) != null) {
			TextPaintInfo info = (TextPaintInfo) arg1.getAttribute(TextAttribute.PAINT_INFO);
			// TODO: Rest of stuff in info.
			g2d.setPaint(info.fillPaint);
		}
		
		g2d.fill(this.vec.getOutline());
	}

	@Override
	public Rectangle2D getBounds2D(AttributedCharacterIterator arg0) {
		return this.getOutline().getBounds2D();
	}

	@Override
	public int getCharacterCount(int start, int end) {
		return end - start + 1;
	}

	@Override
	public GVTFont getFont() {
		return this.font;
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		return this.frc;
	}

	@Override
	public Rectangle2D getGeometricBounds() {
		return this.vec.getVisualBounds();
	}

	@Override
	public Rectangle2D getGlyphCellBounds(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getGlyphCode(int idx) {
		return this.vec.getGlyphCode(idx);
	}

	@Override
	public int[] getGlyphCodes(int arg0, int arg1, int[] arg2) {
		return this.vec.getGlyphCodes(arg0, arg1, arg2);
	}

	@Override
	public GlyphJustificationInfo getGlyphJustificationInfo(int idx) {
		return this.vec.getGlyphJustificationInfo(idx);
	}

	@Override
	public Shape getGlyphLogicalBounds(int arg0) {
		return this.vec.getGlyphLogicalBounds(arg0);
	}

	@Override
	public GVTGlyphMetrics getGlyphMetrics(int arg0) {
		return new GVTGlyphMetrics(this.vec.getGlyphMetrics(arg0), 100000);
	}

	@Override
	public Shape getGlyphOutline(int arg0) {
		return this.vec.getGlyphOutline(arg0);
	}

	@Override
	public Point2D getGlyphPosition(int arg0) {
		return this.vec.getGlyphPosition(arg0);
	}

	@Override
	public float[] getGlyphPositions(int arg0, int arg1, float[] arg2) {
		return this.vec.getGlyphPositions(arg0, arg1, arg2);
	}

	@Override
	public AffineTransform getGlyphTransform(int arg0) {
		return this.vec.getGlyphTransform(arg0);
	}

	@Override
	public Shape getGlyphVisualBounds(int arg0) {
		return this.vec.getGlyphVisualBounds(arg0);
	}

	@Override
	public Rectangle2D getLogicalBounds() {
		return this.vec.getLogicalBounds();
	}

	@Override
	public int getNumGlyphs() {
		return this.vec.getNumGlyphs();
	}

	@Override
	public Shape getOutline() {
		return this.vec.getOutline();
	}

	@Override
	public Shape getOutline(float arg0, float arg1) {
		return this.vec.getOutline(arg0, arg1);
	}

	@Override
	public boolean isGlyphVisible(int arg0) {
		return true;
	}

	@Override
	public boolean isReversed() {
		return false;
	}

	@Override
	public void maybeReverse(boolean arg0) {
	}

	@Override
	public void performDefaultLayout() {
		this.vec.performDefaultLayout();
	}

	@Override
	public void setGlyphPosition(int arg0, Point2D arg1) {
		if (arg0 == this.getNumGlyphs())
			return;
		
		this.vec.setGlyphPosition(arg0, arg1);
	}

	@Override
	public void setGlyphTransform(int arg0, AffineTransform arg1) {
		this.vec.setGlyphTransform(arg0, arg1);
	}

	@Override
	public void setGlyphVisible(int arg0, boolean arg1) {
	}

}

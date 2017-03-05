package com.openhtmltopdf.java2d.api;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class DefaultPageProcessor implements FSPageProcessor {
	public static class DefaultPage implements FSPage {
		private final BufferedImage _img;
		private final Graphics2D _g2d;
		private final int _pgNo;
		private final String _path;
		
		public DefaultPage(int pgNo, int w, int h, String pathTemplate) {
			_img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
			_g2d = _img.createGraphics();
			_pgNo = pgNo;
			_path = pathTemplate;
		}
		
		@Override
		public Graphics2D getGraphics() {
			return _g2d;
		}
		
		public void save() throws IOException {
			String filePath = _path.replace("%page-number%", String.valueOf(_pgNo));
			ImageIO.write(_img, "png", new File(filePath));
		}
	}
	
	private final String _pathTemplate;
	
	/**
	 * Creates a page processor which saves each page as a PNG image.
	 * The pathTemplate must contain the string <code>%page-number%</code> and otherwise
	 * be an absolute writeable file path.
	 * @param pathTemplate
	 */
	public DefaultPageProcessor(String pathTemplate) {
		_pathTemplate = pathTemplate;
	}
	
	@Override
	public FSPage createPage(int zeroBasedPageNumber, int width, int height) {
		return new DefaultPage(zeroBasedPageNumber, width, height, _pathTemplate);
	}

	@Override
	public void finishPage(FSPage pg) {
		DefaultPage page = (DefaultPage) pg;
		page.getGraphics().dispose();
		try {
			page.save();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't write to output image", e);
		}
	}
}

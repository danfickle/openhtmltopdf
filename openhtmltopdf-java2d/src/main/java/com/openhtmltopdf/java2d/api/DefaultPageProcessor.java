package com.openhtmltopdf.java2d.api;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

public class DefaultPageProcessor implements FSPageProcessor {
	public static class DefaultPage implements FSPage {
		private final BufferedImage _img;
		private final Graphics2D _g2d;
		private final int _pgNo;
		private final FSPageOutputStreamSupplier _osf;
		private final String _imgFrmt;
		
		public DefaultPage(int pgNo, int w, int h, FSPageOutputStreamSupplier osFactory, int imageType, String imageFormat) {
			_img = new BufferedImage(w, h, imageType);
			_g2d = _img.createGraphics();

			if (_img.getColorModel().hasAlpha()) {
				/* We need to clear with white transparent */
				_g2d.setBackground(new Color(255, 255, 255, 0));
				_g2d.clearRect(0, 0, (int) _img.getWidth(), (int) _img.getHeight());
			} else {
				_g2d.setColor(Color.WHITE);
				_g2d.fillRect(0, 0, (int) _img.getWidth(), (int) _img.getHeight());
			}
			
			_pgNo = pgNo;
			_osf = osFactory;
			_imgFrmt = imageFormat;
		}
		
		@Override
		public Graphics2D getGraphics() {
			return _g2d;
		}
		
		public void save() {
			OutputStream os = null;
			try {
				os = _osf.supply(_pgNo);
				ImageIO.write(_img, _imgFrmt, os);
			} catch (IOException e) {
				throw new RuntimeException("Couldn't write page image to output stream", e);
			} finally {
				if (os != null)
					try {
						os.close();
					} catch (IOException e) {
					}
			}
		}
	}
	
	private final FSPageOutputStreamSupplier _osFactory;
	private final int _imageType;
	private final String _imageFormat;
	
	/**
	 * Creates a page processor which saves each page as an image.
	 * @param osFactory must supply an output stream for each page. The os
	 * will be closed by the page processor.
	 * @param imageType must be a constant from the BufferedImage class.
	 * @param imageFormat must be a format such as png or jpeg
	 */
	public DefaultPageProcessor(FSPageOutputStreamSupplier osFactory, int imageType, String imageFormat) {
		_osFactory = osFactory;
		_imageType = imageType;
		_imageFormat = imageFormat;
	}
	
	/**
	 * Create a graphics device that can be supplied to useLayoutGraphics.
	 * The caller is responsible for calling dispose on the returned device.
	 * @return
	 */
	public Graphics2D createLayoutGraphics() {
		BufferedImage bf = new BufferedImage(1, 1, _imageType);
		return bf.createGraphics();
	}
	
	@Override
	public FSPage createPage(int zeroBasedPageNumber, int width, int height) {
		return new DefaultPage(zeroBasedPageNumber, width, height, _osFactory, _imageType, _imageFormat);
	}

	@Override
	public void finishPage(FSPage pg) {
		DefaultPage page = (DefaultPage) pg;
		page.getGraphics().dispose();
		page.save();
	}
}

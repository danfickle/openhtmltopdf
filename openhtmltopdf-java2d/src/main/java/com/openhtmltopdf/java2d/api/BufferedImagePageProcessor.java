package com.openhtmltopdf.java2d.api;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * PageProcessor to render everything to buffered images
 */
public class BufferedImagePageProcessor implements FSPageProcessor {
	private final double _scale;
	private final int _imageType;

	private final List<BufferedImagePage> _pages = new ArrayList<>();

	private class BufferedImagePage implements FSPage {
        final BufferedImage _image;
        Graphics2D graphics;

		BufferedImagePage(BufferedImage image) {
			this._image = image;
		}

		@Override
		public Graphics2D getGraphics() {
            if (graphics != null) {
                return graphics;
            }

            graphics = _image.createGraphics();

			if (_image.getColorModel().hasAlpha()) {
				graphics.clearRect(0, 0, _image.getWidth(), _image.getHeight());
			} else {
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, _image.getWidth(), _image.getHeight());
			}

			/*
			 * Apply the scale on the bitmap
			 */
			graphics.scale(_scale, _scale);
			return graphics;
		}
	}

	/**
	 *
	 * @param imageType
	 *            Type of the BufferedImage, e.g. BufferedImage#TYPE_INT_ARGB
	 * @param scale
	 *            scale factor. You can control what resolution of the images
	 *            you want
	 */
	public BufferedImagePageProcessor(int imageType, double scale) {
		_imageType = imageType;
		_scale = scale;
	}

	@Override
	public FSPage createPage(int zeroBasedPageNumber, int width, int height) {
		BufferedImage image = new BufferedImage((int) (width * _scale), (int) (height * _scale), _imageType);
		BufferedImagePage bufferedImagePage = new BufferedImagePage(image);
		_pages.add(bufferedImagePage);
		return bufferedImagePage;
	}

    @Override
    public void finishPage(FSPage pg) {
        BufferedImagePage page = (BufferedImagePage) pg;
        page.graphics.dispose();
        page.graphics = null;
    }

	public List<BufferedImage> getPageImages() {
		List<BufferedImage> images = new ArrayList<>();
		for (BufferedImagePage page : _pages) {
			images.add(page._image);
		}
		return images;
	}
}

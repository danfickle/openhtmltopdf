package com.openhtmltopdf.objects.jfreechart;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.render.RenderingContext;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

public class JFreeChartBarDiagramObjectDrawer implements FSObjectDrawer {

	static Map<Shape, String> buildShapeLinkMap(ChartRenderingInfo renderingInfo, int dotsPerPixel) {
		Map<Shape, String> linkShapes = null;
		AffineTransform scaleTransform = new AffineTransform();
		scaleTransform.scale(dotsPerPixel, dotsPerPixel);
		for (Object entity : renderingInfo.getEntityCollection().getEntities()) {
			if (!(entity instanceof ChartEntity))
				continue;
			ChartEntity chartEntity = (ChartEntity) entity;
			Shape shape = chartEntity.getArea();
			String url = chartEntity.getURLText();
			if (url != null) {
				if (linkShapes == null)
					linkShapes = new HashMap<Shape, String>();
				linkShapes.put(scaleTransform.createTransformedShape(shape), url);
			}
		}
		return linkShapes;
	}

	@Override
	public Map<Shape, String> drawObject(Element e, final double x, final double y, final double width,
			final double height, OutputDevice outputDevice, RenderingContext ctx, final int dotsPerPixel) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		NodeList childNodes = e.getChildNodes();
		final Map<String, String> urls = new HashMap<String, String>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			if (!(item instanceof Element))
				continue;
			Element childElement = (Element) item;
			String tagName = ((Element) item).getTagName();
			if (!tagName.equals("data"))
				continue;
			String series = childElement.getAttribute("series");
			String categorie = childElement.getAttribute("category");
			double value = Double.parseDouble(childElement.getAttribute("value"));
			String url = childElement.getAttribute("url");
			dataset.setValue(value, series, categorie);
			urls.put(series + ":" + categorie, url);
		}

		final JFreeChart chart1 = ChartFactory.createBarChart(e.getAttribute("title"), e.getAttribute("series-label"),
				e.getAttribute("categories-label"), dataset);
		((CategoryPlot) chart1.getPlot()).getRenderer().setDefaultItemURLGenerator(new CategoryURLGenerator() {
			@Override
			public String generateURL(CategoryDataset dataset, int series, int category) {
				return urls.get(dataset.getRowKey(series) + ":" + dataset.getColumnKey(category));
			}
		});
		final ChartRenderingInfo renderingInfo = new ChartRenderingInfo();
		outputDevice.drawWithGraphics((float) x, (float) y, (float) width / dotsPerPixel, (float) height / dotsPerPixel,
				new OutputDeviceGraphicsDrawer() {
					@Override
					public void render(Graphics2D graphics2D) {
						chart1.draw(graphics2D, new Rectangle2D.Float((float) 0, (float) 0,
								(float) (width / dotsPerPixel), (float) (height / dotsPerPixel)), renderingInfo);
					}
				});

		return buildShapeLinkMap(renderingInfo, dotsPerPixel);
	}
}

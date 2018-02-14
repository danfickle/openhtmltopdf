package com.openhtmltopdf.objects.jfreechart;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.extend.OutputDeviceGraphicsDrawer;
import com.openhtmltopdf.render.RenderingContext;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.urls.PieURLGenerator;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import static com.openhtmltopdf.objects.jfreechart.JFreeChartBarDiagramObjectDrawer.buildShapeLinkMap;

public class JFreeChartPieDiagramObjectDrawer implements FSObjectDrawer {

	@Override
	public Map<Shape, String> drawObject(Element e, final double x, final double y, final double width,
			final double height, OutputDevice outputDevice, RenderingContext ctx, final int dotsPerPixel) {
		DefaultPieDataset dataset = new DefaultPieDataset();
		NodeList childNodes = e.getChildNodes();
		final Map<String, String> urls = new HashMap<String, String>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			if (!(item instanceof Element))
				continue;
			Element childElement = (Element) item;
			String tagName = ((Element) item).getTagName();
			if (!tagName.equals("data") && !tagName.equals("td"))
				continue;
			String name = childElement.getAttribute("name");
			double value = Double.parseDouble(childElement.getAttribute("value"));
			String url = childElement.getAttribute("url");
			dataset.setValue(name, value);
			urls.put(name, url);
		}

		final JFreeChart chart1 = ChartFactory.createPieChart(e.getAttribute("title"), dataset, false, false, true);
		((PiePlot) chart1.getPlot()).setURLGenerator(new PieURLGenerator() {
			@Override
			public String generateURL(PieDataset dataset, Comparable key, int pieIndex) {
				return urls.get(key.toString());
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

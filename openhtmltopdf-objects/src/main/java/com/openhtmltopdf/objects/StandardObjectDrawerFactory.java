package com.openhtmltopdf.objects;

import com.openhtmltopdf.objects.jfreechart.JFreeChartBarDiagramObjectDrawer;
import com.openhtmltopdf.objects.jfreechart.JFreeChartPieDiagramObjectDrawer;
import com.openhtmltopdf.objects.pdf.MergeBackgroundPdfDrawer;
import com.openhtmltopdf.render.DefaultObjectDrawerFactory;

/**
 * A ObjectDrawer Factory which registers some default builtin drawers.
 */
public class StandardObjectDrawerFactory extends DefaultObjectDrawerFactory {

	public static void registerStandardObjects(DefaultObjectDrawerFactory factory) {
		factory.registerDrawer("jfreechart/pie", new JFreeChartPieDiagramObjectDrawer());
		factory.registerDrawer("jfreechart/bar", new JFreeChartBarDiagramObjectDrawer());
		factory.registerDrawer("pdf/background",new MergeBackgroundPdfDrawer());
	}

	public StandardObjectDrawerFactory() {
		registerStandardObjects(this);
	}
}

package com.openhtmltopdf.svgsupport;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;

import org.apache.batik.bridge.BridgeExtension;
import org.apache.batik.bridge.ExternalResourceSecurity;
import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.bridge.Mark;
import org.apache.batik.bridge.ScriptSecurity;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.gvt.event.EventDispatcher;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGAElement;
import org.w3c.dom.svg.SVGDocument;

import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;

public class OpenHtmlUserAgent implements UserAgent {
	
	private final OpenHtmlFontResolver resolver;

	public OpenHtmlUserAgent(OpenHtmlFontResolver resolver) {
		this.resolver = resolver;
	}
	
	@Override
	public boolean supportExtension(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String showPrompt(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String showPrompt(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean showConfirm(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void showAlert(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setTransform(AffineTransform arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setTextSelection(Mark arg0, Mark arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setSVGCursor(Cursor arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void registerExtension(BridgeExtension arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void openLink(SVGAElement arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void loadDocument(String arg0) {
		System.err.println("LOAD DOC: " + arg0);
		
	}
	
	@Override
	public boolean isXMLParserValidating() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean hasFeature(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void handleElement(Element arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getXMLParserClassName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Dimension2D getViewportSize() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getUserStyleSheetURI() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public AffineTransform getTransform() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ScriptSecurity getScriptSecurity(String arg0, ParsedURL arg1,
			ParsedURL arg2) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public float getPixelUnitToMillimeter() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public float getPixelToMM() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public float getMediumFontSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String getMedia() {
		return "print";
	}
	
	@Override
	public float getLighterFontWeight(float arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String getLanguages() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public FontFamilyResolver getFontFamilyResolver() {
		return this.resolver;
	}
	
	@Override
	public ExternalResourceSecurity getExternalResourceSecurity(ParsedURL arg0,
			ParsedURL arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public EventDispatcher getEventDispatcher() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getDefaultFontFamily() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Point getClientAreaLocationOnScreen() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SVGDocument getBrokenLinkDocument(Element arg0, String arg1,
			String arg2) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public float getBolderFontWeight(float arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public String getAlternateStyleSheet() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void displayMessage(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void displayError(Exception arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deselectAll() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void checkLoadScript(String arg0, ParsedURL arg1, ParsedURL arg2)
			throws SecurityException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void checkLoadExternalResource(ParsedURL arg0, ParsedURL arg1)
			throws SecurityException {
		// TODO Auto-generated method stub
		
	}
}

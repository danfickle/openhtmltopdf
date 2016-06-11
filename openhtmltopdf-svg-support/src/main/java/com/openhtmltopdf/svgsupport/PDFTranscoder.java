package com.openhtmltopdf.svgsupport;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.geom.AffineTransform;

import org.apache.batik.bridge.BridgeExtension;
import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.bridge.Mark;
import org.apache.batik.bridge.ScriptSecurity;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.transcoder.ErrorHandler;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGAElement;
import org.w3c.dom.svg.SVGDocument;

import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.render.RenderingContext;
import com.openhtmltopdf.util.XRLog;

public class PDFTranscoder extends SVGAbstractTranscoder {

	private final PDFGraphics2DOutputDeviceAdapter od;
	
	public PDFTranscoder(OutputDevice od, RenderingContext ctx, double x, double y) {
		this.od = new PDFGraphics2DOutputDeviceAdapter(ctx, od, x, y);
	}

	@Override
	protected void transcode(Document svg, String uri, TranscoderOutput out) throws TranscoderException {
		super.transcode(svg, uri, out);
		this.root.paint(od);
	}
	
	@Override
	public TranscodingHints getTranscodingHints() {
		// TODO Auto-generated method stub
		return super.getTranscodingHints();
	}
	
	@Override
	public ErrorHandler getErrorHandler() {
		return new ErrorHandler() {
			@Override
			public void warning(TranscoderException arg0) throws TranscoderException {
				XRLog.exception("SVG WARN", arg0);
			}
			
			@Override
			public void fatalError(TranscoderException arg0) throws TranscoderException {
				XRLog.exception("SVG FATAL", arg0);
			}
			
			@Override
			public void error(TranscoderException arg0) throws TranscoderException {
				XRLog.exception("SVG ERROR", arg0);
			}
		};
	}
}

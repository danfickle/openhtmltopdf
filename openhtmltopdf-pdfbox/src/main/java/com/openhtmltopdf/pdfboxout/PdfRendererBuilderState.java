package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is internal. DO NOT USE! Just ignore it!
 * @internal
 */
public class PdfRendererBuilderState extends BaseRendererBuilder.BaseRendererBuilderState {
	public final List<PdfRendererBuilder.AddedFont> _fonts = new ArrayList<PdfRendererBuilder.AddedFont>();
	public OutputStream _os;
	public float _pdfVersion = 1.7f;
	public String _producer;
	public PDDocument pddocument;
}

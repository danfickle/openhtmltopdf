package com.openhtmltopdf.pdfboxout;

import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.CacheStore;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class is internal. DO NOT USE! Just ignore it!
 */
public class PdfRendererBuilderState extends BaseRendererBuilder.BaseRendererBuilderState {
	/* Internal! */
	PdfRendererBuilderState() {
	}

	public final List<PdfRendererBuilder.AddedFont> _fonts = new ArrayList<PdfRendererBuilder.AddedFont>();
	public OutputStream _os;
	public float _pdfVersion = 1.7f;
	public String _producer;
	public PDDocument pddocument;
        public final Map<CacheStore, FSCacheEx<String, FSCacheValue>> _caches = new EnumMap<CacheStore, FSCacheEx<String, FSCacheValue>>(CacheStore.class);
	public PdfAConformance _pdfAConformance = PdfAConformance.NONE;
	public byte[] _colorProfile;
}

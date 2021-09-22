package com.openhtmltopdf.mathmlsupport;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.openhtmltopdf.util.LogMessageId;
import net.sourceforge.jeuclid.font.DefaultFontFactory;
import net.sourceforge.jeuclid.font.FontFactory;

import org.w3c.dom.Element;

import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.constants.IdentValue;
import com.openhtmltopdf.css.sheet.FontFaceRule;
import com.openhtmltopdf.css.style.CalculatedStyle;
import com.openhtmltopdf.css.style.CssContext;
import com.openhtmltopdf.css.style.FSDerivedValue;
import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.util.XRLog;

public class MathMLDrawer implements SVGDrawer {
	private final FontFactory _fontFactory;
	private SharedContext _sharedCtx;
	private final Set<String> _loadedFontFamilies = new HashSet<>();
    private final Map<String, List<FontEntry>> _availabelFontFamilies = new HashMap<>();

    private static class FontEntry {
        String src;
        File file;
    }

	public MathMLDrawer() {
		this._fontFactory = new DefaultFontFactory();
		FontFactory.setThreadFontFactory(this._fontFactory);
	}
	
	@Override
	public void importFontFaceRules(List<FontFaceRule> fontFaces,
			SharedContext shared) {
		
		// In this method, we just load a map of font families to their sources,
		// as we don't yet know which fonts mathml objects will use.
		
		for (FontFaceRule rule : fontFaces) {
			_sharedCtx = shared;
			
			CalculatedStyle style = rule.getCalculatedStyle();
			
			FSDerivedValue src = style.valueByName(CSSName.SRC);

			if (src == IdentValue.NONE ||
				!rule.hasFontFamily()) {
	            continue;
	        }

            String family = style.valueByName(CSSName.FONT_FAMILY).asString();

            FontEntry entry = new FontEntry();
            entry.src = src.asString();

            _availabelFontFamilies.computeIfAbsent(family, f -> new ArrayList<>()).add(entry);
		}
	}
	
	private void loadFamilyFonts(String family) {
		// In this method, we load all fonts for a given family,
		// as we don't know which styles and weights will be used.
		
		if (_loadedFontFamilies.contains(family)) {
			return;
		} else {
			_loadedFontFamilies.add(family);
		}
		
		if (!_availabelFontFamilies.containsKey(family)) {
			XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_COULD_NOT_FIND_FONT_SPECIFIED_FOR_MATHML_OBJECT_IN_FONT_FACE_RULES,family);
			return;
		}

        for (FontEntry entry : _availabelFontFamilies.get(family)) {
            if (entry.src != null) {
                byte[] font1 = _sharedCtx.getUserAgentCallback().getBinaryResource(entry.src, ExternalResourceType.FONT);
                if (font1 == null) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_COULD_NOT_LOAD_FONT, entry.src);
                    continue;
                }

                try {
                    _fontFactory.registerFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(font1));
                } catch (IOException | FontFormatException e) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_MATHML_COULD_NOT_REGISTER_FONT, e);
                }
            } else if (entry.file != null) {
                try {
                    _fontFactory.registerFont(Font.TRUETYPE_FONT, entry.file);
                } catch (IOException | FontFormatException e) {
                    XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.EXCEPTION_MATHML_COULD_NOT_REGISTER_FONT, e);
                }
            }
        }
    }

	@Override
	public SVGImage buildSVGImage(Element mathMlElement, Box box, CssContext c, double cssWidth,
			double cssHeight, double dotsPerPixel) {
		
		// Make sure the fonts this MathML object uses are loaded.
		String[] fonts = box.getStyle().asStringArray(CSSName.FONT_FAMILY);
		for (String family : fonts) {
			loadFamilyFonts(family);
		}
		
		double cssMaxWidth = CalculatedStyle.getCSSMaxWidth(c, box);
		double cssMaxHeight = CalculatedStyle.getCSSMaxHeight(c, box);
		List<String> fontList = Arrays.asList(fonts);

		MathMLImage img = new MathMLImage(mathMlElement, box, cssWidth, cssHeight, cssMaxWidth, cssMaxHeight, dotsPerPixel, fontList);

		return img;
	}

	@Override
	public void close() throws IOException {
		FontFactory.clearThreadFontFactory();
	}

    @Override
    public void addFontFile(File fontFile, String family, Integer weight, FontStyle style) {
        FontEntry entry = new FontEntry();
        entry.file = fontFile;

        this._availabelFontFamilies.computeIfAbsent(family, f -> new ArrayList<>()).add(entry);
    }
}

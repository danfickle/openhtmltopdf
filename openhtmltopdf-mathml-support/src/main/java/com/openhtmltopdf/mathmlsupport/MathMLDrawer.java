package com.openhtmltopdf.mathmlsupport;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import com.openhtmltopdf.render.Box;
import com.openhtmltopdf.util.XRLog;

public class MathMLDrawer implements SVGDrawer {
	private final FontFactory _fontFactory;
	private SharedContext _sharedCtx;
	private final Set<String> _loadedFontFamilies = new HashSet<String>();
	private final Map<String, List<String>> _availabelFontFamilies = new HashMap<String, List<String>>();

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
			
			if (_availabelFontFamilies.containsKey(family)) {
				_availabelFontFamilies.get(family).add(src.asString());
			} else {
				_availabelFontFamilies.put(family, new ArrayList<String>());
				_availabelFontFamilies.get(family).add(src.asString());
			}
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
			XRLog.general(Level.WARNING, "Could not find font (" + family + ") specifed for MathML object in font-face rules");
			return;
		}
		
		for (String src : _availabelFontFamilies.get(family)) {
			byte[] font1 = _sharedCtx.getUserAgentCallback().getBinaryResource(src);
			if (font1 == null) {
				XRLog.exception("Could not load font " + src);
				continue;
			}
		
			try {
				_fontFactory.registerFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(font1));
			} catch (IOException e) {
				XRLog.exception("Couldn't read memory!", e);
			} catch (FontFormatException e) {
				XRLog.exception("Could not read font correctly", e);
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
		
		MathMLImage img = new MathMLImage(mathMlElement, cssWidth, cssHeight, cssMaxWidth, cssMaxHeight, dotsPerPixel, fontList);

		return img;
	}

	@Override
	public void close() throws IOException {
		FontFactory.clearThreadFontFactory();
	}
}

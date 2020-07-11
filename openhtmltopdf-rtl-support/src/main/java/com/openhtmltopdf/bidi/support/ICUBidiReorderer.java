package com.openhtmltopdf.bidi.support;

import java.util.logging.Level;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.openhtmltopdf.bidi.BidiReorderer;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;

public class ICUBidiReorderer implements BidiReorderer {
	ArabicShaping shaper = new ArabicShaping(ArabicShaping.TEXT_DIRECTION_LOGICAL | ArabicShaping.LETTERS_SHAPE | ArabicShaping.LENGTH_GROW_SHRINK);
	ArabicShaping deshaper = new ArabicShaping(ArabicShaping.TEXT_DIRECTION_LOGICAL | ArabicShaping.LETTERS_UNSHAPE | ArabicShaping.LENGTH_GROW_SHRINK);
	
	@Override
	public String reorderRTLTextToLTR(String text) {
		return Bidi.writeReverse(text, Bidi.DO_MIRRORING);
	}

	@Override
	public String shapeText(String text) {
		try {
			return shaper.shape(text);
		} catch (ArabicShapingException e) {
			XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.GENERAL_EXCEPTION_SHAPING_TEXT, e);
			return text;
		}
	}

	@Override
	public String deshapeText(String text) {
		try {
			return deshaper.shape(text);
		} catch (ArabicShapingException e) {
			XRLog.log(Level.WARNING, LogMessageId.LogMessageId0Param.GENERAL_EXCEPTION_DESHAPING_TEXT, e);
			return text;
		}
	}

	@Override
	public boolean isLiveImplementation() {
		return true;
	}
}

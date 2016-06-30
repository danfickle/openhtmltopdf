package com.openhtmltopdf.bidi.support;

import java.util.Locale;

import com.ibm.icu.text.BreakIterator;
import com.openhtmltopdf.extend.FSTextBreaker;

public class ICUBreakers {
	
	private ICUBreakers() { }
	
	public static class ICULineBreaker implements FSTextBreaker {
		private final BreakIterator breaker;
		
		public ICULineBreaker(Locale locale) {
			this.breaker = BreakIterator.getLineInstance(locale);
		}
		
		@Override
		public int next() {
			return this.breaker.next();
		}

		@Override
		public void setText(String newText) {
			this.breaker.setText(newText);
		}
	}

	public static class ICUCharacterBreaker implements FSTextBreaker {
		private final BreakIterator breaker;
		
		public ICUCharacterBreaker(Locale locale) {
			this.breaker = BreakIterator.getCharacterInstance(locale);
		}
		
		@Override
		public int next() {
			return this.breaker.next();
		}

		@Override
		public void setText(String newText) {
			this.breaker.setText(newText);
		}
	}
}

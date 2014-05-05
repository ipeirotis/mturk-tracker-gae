package com.tracker.util;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SafeCurrencyFormat {

	private static final ThreadLocal<Map<String, NumberFormat>> threadLocal = new ThreadLocal<Map<String, NumberFormat>>() {
		@Override
		protected Map<String, NumberFormat> initialValue() {
			return new HashMap<String, NumberFormat>();
		}
	};

	public static NumberFormat forLocale(Locale locale) {
		Map<String, NumberFormat> cache = threadLocal.get();
		NumberFormat cf = cache.get(locale.getLanguage());
		if (cf == null) {
			cf = NumberFormat.getCurrencyInstance(locale);
			cache.put(locale.getLanguage(), cf);
		}
		return cf;
	}
}

package com.viaoa.util;

import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class OAHttpUtil {

	public static String getUrlEncodedNameValues(Map<String, Object> mapNameValue) {
		if (mapNameValue == null) {
			return null;
		}
		String result = null;
		for (Map.Entry<String, Object> me : mapNameValue.entrySet()) {
			if (result == null) {
				result = "";
			} else {
				result += "&";
			}
			String s = getUrlEncodedNameValues(me.getKey(), me.getValue(), null);
			result += s;
		}
		return result;
	}

	public static String getUrlEncodedNameValues(final String name, final Object value, final String format) {
		if (OAString.isEmpty(name)) {
			throw new RuntimeException("name can not be null");
		}
		String result = null;

		if (value == null) {
			return null;
		}

		if (value.getClass().isArray()) {
			int x = Array.getLength(value);
			for (int i = 0; i < x; i++) {
				Object obj = Array.get(value, i);
				String val = OAConv.toString(obj, format);
				if (val == null) {
					val = "";
				} else {
					try {
						val = URLEncoder.encode(val, "UTF-8");
					} catch (Exception e) {
					}
				}
				if (result == null) {
					result = "";
				} else {
					result += "&";
				}
				result += name + "=" + val;
			}
		} else if (value instanceof List) {
			List list = (List) value;
			for (Object obj : list) {
				String val = OAConv.toString(obj, format);
				if (val == null) {
					val = "";
				} else {
					try {
						val = URLEncoder.encode(val, "UTF-8");
					} catch (Exception e) {
					}
				}
				if (result == null) {
					result = "";
				} else {
					result += "&";
				}
				result += name + "=" + val;
			}
		} else {
			String val = OAConv.toString(value, format);
			if (val == null) {
				val = "";
			} else {
				try {
					val = URLEncoder.encode(val, "UTF-8");
				} catch (Exception e) {
				}
			}
			result = name + "=" + val;
		}

		return result;
	}

	public static String updateSlashes(String urlValue, boolean bLeadingSlash, boolean bTrailingSlash) {
		if (urlValue == null) {
			return "";
		}
		int x = urlValue.length();
		if (x > 0) {
			char c1 = urlValue.charAt(0);
			if (bLeadingSlash) {
				if (c1 != '/') {
					urlValue = '/' + urlValue;
					x++;
				}
			} else {
				if (c1 == '/') {
					urlValue = urlValue.substring(1);
					x--;
				}
			}

			if (x > 1) {
				char c2 = urlValue.charAt(x - 1);
				if (bTrailingSlash) {
					if (c2 != '/') {
						urlValue += "/";
					}
				} else {
					if (c2 == '/') {
						urlValue = urlValue.substring(0, x - 1);
					}
				}
			}
		} else {
			if (bLeadingSlash || bTrailingSlash) {
				urlValue = "/";
			}
		}
		return urlValue;
	}

}

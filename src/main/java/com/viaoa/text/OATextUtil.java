package com.viaoa.text;

import java.awt.Color;

import com.viaoa.util.OAConverter;
import com.viaoa.util.OAString;

public class OATextUtil {

	

	public static String append(String orig, String append) {
		return concat(orig, append, " ");
	}

	public static String append(String orig, String append, String sep) {
		return concat(orig, append, sep, true);
	}

	public static String prepend(String orig, String prepend, String sep) {
		if (orig == null) {
			orig = "";
		}
		else if (sep != null && orig.length() > 0) {
			orig = sep + orig;
		}
		orig = prepend + orig;
		return orig;
	}
    
	public static String concat(String toText, String value) {
		return concat(toText, value, " ", true);
	}

	public static String concat(String toText, Object value, String sepChar) {
		String strValue = value == null ? null : value.toString();
		return concat(toText, strValue, sepChar, false);
	}

	public static String concat(String toText, String value, String sepChar) {
		return concat(toText, value, sepChar, false);
	}

	public static String concat(String toText, String value, String sepChar, boolean bForce) {
		if (!bForce && (value == null || value.length() == 0)) {
			if (toText == null) return "";  // always return non-null, so a null check does not have be used.
			return toText;
		}
		if (value == null) {
			value = "";
		}
		if (toText == null || toText.length() == 0) {
			toText = value;
		} else {
			toText += sepChar;
			toText += value;
		}
		return toText;
	}

    
    
	/**
	 * Converts a color to a String that represents the Hex value.
	 *
	 * @return null if color=null, else Hex String with leading "#", ex: "#00FFCC"
	 */
	public static String colorToHex(Color color) {
		if (color == null) {
			return null;
		}
		String colorStr = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
		return colorStr;
	}
    

	



	/**
	 * Converts any non-Java identifier characters to a '_'
	 */
	public static String makeJavaIdentifier(String txt) {
		if (txt == null) {
			return null;
		}
		int x = txt.length();
		StringBuilder sb = null;
		for (int i = 0; i < x; i++) {
			char ch = txt.charAt(i);
			if (!Character.isJavaIdentifierPart(ch)) {
				if (sb == null) {
					sb = new StringBuilder(x);
					if (i > 0) {
						sb.append(txt.substring(0, i));
					}
				}
				ch = '_';
			}
			if (sb != null) {
				sb.append(ch);
			}
		}
		if (sb == null) {
			return txt;
		}
		return new String(sb);
	}




	/**
	 * get last N chars from string.
	 *
	 * @param len number of chars to get
	 */
	public static String getEnd(String text, int len) {
		if (text == null) {
			return null;
		}
		if (len <= 0) return "";
		int x = text.length();
		if (x <= len) {
			return text;
		}
		String s = text.substring(x - len);
		return s;
	}

	public static String getLast(String text, int len) {
		return getEnd(text, len);
	}

	/**
	 * get first N chars from string.
	 *
	 * @param len number of chars to get
	 */
	public static String getBegin(String text, int len) {
		if (text == null) {
			return null;
		}
		if (len <= 0) return "";
		int x = text.length();
		if (x <= len) {
			return text;
		}
		String s = text.substring(0, len);
		return s;
	}

	public static String getFirst(String text, int len) {
		return getBegin(text, len);
	}


	
	


	
	
	public static int parseInt(String val) {
		int x = 0;
		if (val == null) {
			return x;
		}
		boolean bStarted = false;
		boolean bNeg = false;
		int len = val.length();
		for (int i = 0; i < len; i++) {
			char c = val.charAt(i);
			if (Character.isDigit(c)) {
				x *= 10;
				x += c - '0';
				bStarted = true;
			} else {
				if (bStarted) {
					break;
				}
				if (c == '-') {
					bNeg = true;
					bStarted = true;
				}
			}
		}
		if (bNeg) {
			x *= -1;
		}
		return x;
	}
	
	/**
	 * Update the string to be used for a Like operator search, by converting '*' to '%' and by adding a '%' at the end if there is not one
	 * already.
	 */
	public static String convertToLikeSearch(String s) {
		if (s == null) {
			return s;
		}
		s = s.replace("*", "%");
		if (s.indexOf('%') < 0) {
			s += "%";
		}
		return s;
	}

	/**
	 * Create a 3 row column heading.
	 *
	 * @param startPos first position (usually 0 or 1)
	 * @param endPos
	 * @return 2 rows of numbers, showing each digit vertically
	 */
	public static String getVerticalNumberLines(int startPos, int endPos) {
		StringBuilder sb = new StringBuilder();

		for (int i = startPos; i <= endPos; i++) {
			if (i % 10 == 0) {
				int x = (i / 10);
				sb.append("" + (x % 10));
			} else {
				sb.append(' ');
			}
		}

		sb.append('\n');

		for (int i = startPos; i <= endPos; i++) {
			sb.append("" + (i % 10));
		}

		return sb.toString();
	}

	public static String getVerticalHex(byte[] bs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bs.length; i++) {
			String hex = String.format("%02x", bs[i]).toUpperCase();
			sb.append(hex, 0, 1);
		}

		sb.append('\n');

		for (int i = 0; i < bs.length; i++) {
			String hex = String.format("%02x", bs[i]).toUpperCase();
			sb.append(hex, 1, 2);
		}
		return sb.toString();
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	// https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] hexToBytes(String hex) {
		if (hex == null) {
			return null;
		}
		int x = hex.length();
		byte[] bs = new byte[x / 2];

		for (int i = 0; i < x; i += 2) {
			bs[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return bs;
	}

	public static String createString(char repeatChar, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(repeatChar);
		}
		return sb.toString();
	}
	
	public static String createPropertyPath(String... args) {
		if (args == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(48);

		for (String s : args) {
			if (s == null) {
				continue;
			}

			if (sb.length() == 0) {
				sb.append(s);
			} else {
				if (s.indexOf(':') == 0) {
					sb.append(s); // filter
				} else {
					sb.append(".");
					sb.append(s);
				}
			}
		}
		return sb.toString();
	}

	public static String createPropertyPath(Class clazz, String... args) {
		if (args == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(48);
		for (String s : args) {
			if (s == null) {
				continue;
			}
			if (sb.length() == 0) {
				if (clazz != null) {
					sb.append("(");
					sb.append(clazz.getName());
					sb.append(")");
					sb.append(s);
				} else {
					sb.append(s);
				}
			} else {
				if (s.indexOf(':') == 0) {
					sb.append(s); // filter
				} else {
					sb.append(".");
					sb.append(s);
				}
			}
		}
		return sb.toString();
	}

}



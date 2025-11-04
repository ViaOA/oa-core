/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.text;

import java.util.HashMap;
import java.util.Map;

import com.viaoa.util.OAString;

/**
 * Encoding and escaping utilities for HTML, XML, JavaScript, and JSON text.
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Escaping markup characters for HTML and XML</li>
 *   <li>Encoding illegal XML control characters</li>
 *   <li>Basic unescape and reverse-transform operations</li>
 *   <li>JavaScript and JSON-escape helpers for safe inline usage</li>
 *   <li>Highlighting search matches within text using tag wrappers</li>
 * </ul>
 *
 * <p>All methods in this class are null-safe and operate only on text content.
 * Higher-level layout and formatting features should be delegated to
 * {@link OATextFormat}, and structural parsing to
 * {@link OATextTokenizer}.</p>
 *
 * <p>This class ensures that OA applications safely encode text for browser,
 * markup, and serialization environments where characters such as '&', '&lt;',
 * '&gt;', and quotes require escape sequences.</p>
 *
 */
public class OATextEscape {

	
	/**
	 * Convert '&amp;' prefixed html codes to character.
	 */
	public static String convertFromHtml(String html) {
		if (html == null) {
			return null;
		}

		if (html.indexOf('&') >= 0) {
			html = OATextFilter.convert(html, "&amp;", "&");
			html = OATextFilter.convert(html, "&quot;", "\"");
			html = OATextFilter.convert(html, "&apos;", "'");
			html = OATextFilter.convert(html, "&lt;", "<");
			html = OATextFilter.convert(html, "&gt;", ">");
		}
		return html;
	}

	public static String convertTextToHtml(String value, boolean bAddHTMLTag) {
		if (value == null) {
			return "";
		}
		String s2 = value.toLowerCase();
		if (s2.indexOf("<html") >= 0) {
			return value;
		}
		if (s2.indexOf("<") >= 0 && s2.indexOf(">") >= 0) {
			return value;
		}
		// if (s2.indexOf("<br>") >= 0) return value;
		if (s2.indexOf("&amp;") >= 0) {
			return value;
		}

		value = convertToXml(value, false, true, true);

		if (bAddHTMLTag) {
			value = "<html>" + value + "</html>";
		}
		return value;
	}
	
	public static String convertToHtml(String value) {
		return convertToXml(value, false, true);
	}
	

	/**
	 * Convert a String to a valid XML String, using special coding for illegal characters. <br>
	 * Converts &amp; to &amp;amp;, &quot; to &amp;quot, &#39; to &amp;apos, &lt; to &amp;&lt;, &gt; to &amp;gt;<br>
	 * For characters less then 32, it calls encodeIllegalXML().
	 * <p>
	 * Note: Some characters are illegal even within CDATA blocks.
	 * <p>
	 * NOTE: decodeIllegalXML() should be called to <i>reverse</i> this String, since encodeIllegalXML() uses a special tag.
	 *
	 * @param value  is XML String to convert.
	 * @param bCData true if this String will be used in an XML CDATA block.
	 * @return converted string. If value is null then a blank "" is returned.
	 * @see #decodeIllegalXML
	 * @see #encodeIllegalXML
	 */
	public static String convertToXml(String value, boolean bCData) {
		return convertToXml(value, bCData, false);
	}

	public static String convertToXml(String value, boolean bCData, boolean bIsHtml) {
		return convertToXml(value, bCData, bIsHtml, !bIsHtml);
	}

	public static String convertToXml(String value, boolean bCData, boolean bIsHtml, boolean bLeaveCRLF) {
		if (value == null) {
			return "";
		}

		int x = value.length();
		StringBuilder sb = new StringBuilder(x);
		for (int i = 0; i < x; i++) {
			char ch = value.charAt(i);
			char chNext = (i + 1 == x) ? 0 : value.charAt(i + 1);
			char chPrev = (i == 0) ? 0 : value.charAt(i - 1);

			if (!bCData) {
				switch (ch) {
				case '&':
					sb.append("&amp;");
					continue;
				case '"':
					sb.append("&quot;");
					continue;
				case '\'':
					sb.append("&apos;");
					continue;
				case '<':
					sb.append("&lt;");
					continue;
				case '>':
					sb.append("&gt;");
					continue;
				case '\n':
					if (bIsHtml) {
						if (chPrev != '\r') {
							sb.append("<br>");
						}
					}
					if (!bLeaveCRLF) {
						continue;
					}
					break;
				case '\r': {
					if (bIsHtml && chNext == '\n') {
						sb.append("<br>");
					}
					if (!bLeaveCRLF) {
						continue;
					}
					break;
				}
				}
			}

			switch (ch) {
			case 9:
			case 10:
			case 13:
				sb.append(ch);
				continue;
			}

			if (ch < 32) { // illegal in XML, create special tag
				sb.append(encodeIllegalXml(ch, !bCData));
			} else {
				sb.append(ch);
			}
		}
		return new String(sb);
	}

	/**
	 * converts null to "" and does other xml/html conversions for &lt;, &gt; &amp; &quot; &#39;
	 *
	 * <pre>
	   see: http://www.w3.org/TR/REC-xml#NT-Char

	   Legal Chars ::=   #x9 | #xA | #xD | [#x20-#xD7FF] |
	             [#xE000-#xFFFD] | [#x10000-#x10FFFF]

	   9, 10, 13
	   tab, lf, cr
	 * </pre>
	 *
	 * @see #convertToXml(String,boolean)
	 */
	public static String convertToXml(String value) {
		return convertToXml(value, false, false);
	}

	

	/**
	 * Encode/Replace chars that are illegal for XML/HTML with &amp; codes.
	 *
	 * @see #convertToXml(String,boolean)
	 */
	public static String encodeIllegalXml(String value) {
		return convertToXml(value, true, false);
	}

	/**
	 * Encode illegal XML characters with &lt;OAXML#999/&gt; where 999 is character integer value.<br>
	 * decodeIllegalXML() is used to convert back to character.
	 * <p>
	 * This is used internally by convertToXML
	 *
	 * @param ch           is character to encode
	 * @param bConvertLTGT if true then convert &lt; to &amp;lt; and &gt; to &amp;gt;. This is needed when it is not going to be used in a
	 *                     XML CDATA block.
	 * @see #decodeIllegalXML
	 * @see #convertToXml(String,boolean)
	 */
	public static String encodeIllegalXml(char ch, boolean bConvertLTGT) {
		if (bConvertLTGT) {
			return "&lt;OAXML#" + ((int) ch) + "/&gt;";
		}
		return "<OAXML#" + ((int) ch) + "/>"; // used in [CDATA]
	}


	/**
	 * Used to determine if a String has any illegal XML characters in it.
	 *
	 * @return false if any of the following characters are found: &amp; &quot; \ &lt; &gt; LF CR or char&lt;32. If value is null then false
	 *         is returned.
	 */
	public static boolean isLegalXml(String value) {
		if (value == null) {
			return false;
		}
		int x = value.length();
		for (int i = 0; i < x; i++) {
			char ch = value.charAt(i);
			switch (ch) {
			case '&':
			case '"':
			case '\'':
			case '<':
			case '>':
			case 10:
			case 13:
				return false;
			case 9:
				break;
			default:
				if (ch < 32) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Convert XML Strings converted with encodeIllegalXML() back to a String. Since encodeIllegalXML encodes illegal characters with a
	 * &lt;OAXML#99/&gt; code, this method will convert those tags to the actual character.
	 *
	 * @return string that was decoded. If value is null then null is returned.
	 * @see #encodeIllegalXML
	 */
	public static String decodeIllegalXml(String value) {
		if (value == null) {
			return null;
		}
		int pos = 0;
		for (;;) {
			int apos = value.indexOf("<OAXML#", pos);
			if (apos < 0) {
				break;
			}
			int bpos = value.indexOf("/>", apos + 7);
			if (bpos > 0 && bpos > apos + 7) {
				int ch = ' ';
				try {
					ch = Integer.parseInt(value.substring(apos + 7, bpos));
				} catch (Exception e) {
				}
				value = value.substring(0, apos) + ((char) ch) + value.substring(bpos + 2);
			}
			pos = apos + 1;
		}
		return value;
	}

	public static String escape(String raw) {
		String escaped = raw;
		escaped = escaped.replace("\\", "\\\\");
		escaped = escaped.replace("\"", "\\\"");
		escaped = escaped.replace("\b", "\\b");
		escaped = escaped.replace("\f", "\\f");
		escaped = escaped.replace("\n", "\\n");
		escaped = escaped.replace("\r", "\\r");
		escaped = escaped.replace("\t", "\\t");
		// TODO: escape other non-printing characters using uXXXX notation
		return escaped;
	}


	
	public static String escapeJs(final String text, final char jsQuoteChar) {
	    return escapeJs(text, jsQuoteChar, false);
	}

	public static String escapeJs(final String text, final char jsQuoteChar, final boolean bIsJsCodeEmbeddedInHtml) {
        if (text == null) return "";
        final int x = text.length();
        StringBuilder sb = null;

        for (int i = 0; i < x; i++) {
            char ch = text.charAt(i);

            if (ch == '\r' || ch == '\n' || ch == '\\' || ch == jsQuoteChar || (bIsJsCodeEmbeddedInHtml && (ch == '\'' || ch == '\"'))) {
                if (sb == null) {
                    sb = new StringBuilder(x + 4);
                    if (i > 0) sb.append(text.substring(0, i));
                }

                if (ch == '\'') {
                    if (bIsJsCodeEmbeddedInHtml) sb.append("\\x27"); // x27 = "\'"
                    //was if (bIsJsCodeEmbeddedInHtml) sb.append("\\x5Cx27");  //  x5C = "\"   x27 = "\'"
                    else sb.append("\\" + jsQuoteChar);
                }
                else if (ch == '\"') {
                    if (bIsJsCodeEmbeddedInHtml) sb.append("\\x22"); // x22 = "\""
                    //was if (bIsJsCodeEmbeddedInHtml) sb.append("\\x5Cx22");  //  x5C = "\"   x22 = "\""
                    else sb.append("\\" + jsQuoteChar);
                }
                else if (ch == '<') {
                    sb.append("&lt;");
                }
                else if (ch == '>') {
                    sb.append("&gt;");
                }
                else if (ch == '\n') {
                    if (!bIsJsCodeEmbeddedInHtml) sb.append("\\n");
                    else sb.append("\\n"); //  \n
                    //was else sb.append("\\x5Cn"); //  \n
                }
                else if (ch == '\r') {
                    // no-op
                }
                else if (ch == '\\') {
                    if (!bIsJsCodeEmbeddedInHtml) sb.append("\\\\");
                    else sb.append("\\");
                    //was: else sb.append("\\x5C\\x5C"); // x5C = "\"
                }
            }
            else {
                if (sb != null) sb.append(ch);
            }
        }

        if (sb == null) {
            return text;
        }
        return sb.toString();
    }
	
	
	
	public static String escapeJson(String s) {
		if (s == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		escapeJson(s, sb);
		return sb.toString();
	}

	public static void escapeJson(String s, StringBuffer sb) {
	    if (s == null) return;
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;
            case '\'':
                sb.append("\\\'");
                break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			/*
			case '/':
				sb.append("\\/");
				break;
			*/
			default:
				// http://www.unicode.org/versions/Unicode5.1.0/
				if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int k = 0; k < 4 - ss.length(); k++) {
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}
	}
	
	/* ex:
	 * <div style='background-image:url(oaproperty://com.tmgsc.hifive.model.oa.ImageStore/bytes?232); width:88; height:99' colspan=4 test xyz abc=Abcde123>adfa</div>
	 */
	public static Map<String, String> getHtmlAttributeMap(String htmlTag) {
		Map<String, String> map = new HashMap<String, String>();
		if (htmlTag == null) {
			return map;
		}
		String[] ss = OATextTokenizer.tokenize(htmlTag, '=', true, true, '<', '>', (char) 0);

		for (int i = 1; i < ss.length; i++) { //skip first value, "tag name"
			String s1 = ss[i];

			if (i + 1 == ss.length) {
				map.put(s1, "");
				break;
			}
			String s2 = ss[++i];
			if (s2.equals("=")) {
				if (i + 1 == ss.length) {
					map.put(s1, "");
					break;
				}
				s2 = ss[++i];
			} else {
				map.put(s1, "");
				i--;
			}
			map.put(s1, s2);
		}
		return map;
	}

	public static String unescapeJson(String s) {
		s = OAString.convert(s, "\\\"", "\"");
		s = OAString.convert(s, "\\\\", "\\");
		s = OAString.convert(s, "\\b", "\b");
		s = OAString.convert(s, "\\f", "\f");
		s = OAString.convert(s, "\\n", "\n");
		s = OAString.convert(s, "\\r", "\r");
		s = OAString.convert(s, "\\t", "\t");
		s = OAString.convert(s, "\\/", "/");

		return s;
	}

	public static String hilite(String line, String search, String beginTag, String endTag, boolean bIgnoreCase) {
		if (line == null || search == null) {
			return line;
		}

		final int searchLength = search.length();
		if (searchLength == 0) {
			return line;
		}
		if (bIgnoreCase) {
			search = search.toLowerCase();
		}

		final int lineLength = line.length();
		StringBuilder sb = null; // dont allocate until first match is found
		char c = 0, origChar = 0;

		for (int i = 0, j = 0;; i++) {
			if (i < lineLength) {
				origChar = c = line.charAt(i);
				if (bIgnoreCase) {
					c = Character.toLowerCase(c);
				}
				if (c == search.charAt(j)) {
					j++;
					if (j == searchLength) {
						if (sb == null) {
							sb = new StringBuilder(lineLength + (lineLength / 10));
							int e = (i - j) + 1;
							if (e > 0) {
								sb.append(line.substring(0, e));
							}
						}
						sb.append(beginTag);
						/*
						Search="Vi"
						i=6
						i: 0123456789
						   VinceViNce
						j:      12
						*/
						int b = (i - j) + 1;
						sb.append(line.substring(b, b + j));
						sb.append(endTag);
						j = 0;
					}
					continue;
				}
			}
			if (j > 0) {
				if (sb != null) {
					// go back to previously matched chars
					int b = i - j;
					/*
					Search="Vix"
					i=7
					i: 0123456789
					   VinceViNce
					j:      12
					*/
					sb.append(line.substring(b, b + 1));
				}
				i -= j; // start at last checking point, loop with inc i by +1
				j = 0;
			} else {
				if (i >= lineLength) {
					break;
				}
				if (sb != null) {
					sb.append(origChar);
				}
			}
		}
		if (sb == null) {
			return line;
		}
		return new String(sb);
	}
}

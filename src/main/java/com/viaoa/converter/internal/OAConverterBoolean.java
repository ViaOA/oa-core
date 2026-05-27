/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
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
package com.viaoa.converter.internal;

import com.viaoa.converter.OAConverter;
import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;

/*qqqqqqqqqqqqqqqqqq
CODEX

#2 — OAConverterBoolean.convert(...)

  Concrete bug: signed numeric zero strings convert to true.

  Runtime scenario: "-0", "+0", or "-0.00" pass OAString.isNumber(str), then the sign character is treated as non-
  zero, so the converter returns true.

  Why this violates converter semantics: numeric boolean coercion should treat numeric zero as false. This silently
  stores or compares the wrong boolean value.

  Minimal fix direction: parse the numeric string and compare to zero, or explicitly ignore a leading +/- in the
  current scan loop.

#4 — OAConverterBoolean.convert(...)

  Concrete bug: custom boolean format with only one field can throw NullPointerException.

  Runtime scenario: a format like "yes" matches the documented optional-format style, but for non-yes input the second
  OAString.field(fmt, ";", 2) can return null, then s.equalsIgnoreCase(str) throws.

  Why this violates converter semantics: failed conversion should be controlled and caller-visible as null/failed
  conversion, not an incidental NPE from optional format handling.

  Minimal fix direction: null-check each format field before equalsIgnoreCase; define whether missing false-token
  means “unrecognized returns null”.

*/

/**
 * Converter for transforming values into {@link Boolean} and formatting
 * Boolean values into text using optional format rules.
 *
 * <h3>Conversion to {@code Boolean}</h3>
 * Behavior depends on the runtime type of {@code fromValue}:
 * <ul>
 *   <li>{@link Boolean} — returned directly</li>
 *   <li>{@code null} — {@code Boolean.FALSE}</li>
 *   <li>{@link String}:
 *     <ul>
 *       <li>If {@code fmt} is provided (e.g. {@code "yes;no;maybe"}), the first
 *           entry is treated as {@code true} and the second as {@code false};
 *           if no match, returns {@code null}</li>
 *       <li>Without {@code fmt}, case‐insensitive checks:</li>
 *       <ul>
 *         <li>{@code "true"}, {@code "yes"}, {@code "t"}, {@code "y"} → true</li>
 *         <li>Numeric strings → true if any digit is non-zero</li>
 *         <li>Empty or falsy strings → false</li>
 *       </ul>
 *     </ul>
 *   </li>
 *   <li>{@link Number} — true if value is not zero</li>
 *   <li>{@link Character} — true if {@code 'T','t','Y','y'} or numeric non-zero</li>
 *   <li>Other non-null values → {@code true}</li>
 * </ul>
 *
 * <h3>Formatting from {@code Boolean}</h3>
 * <ul>
 *   <li>If {@code fmt} is provided, returns:
 *     <ol>
 *       <li>true → first field</li>
 *       <li>false → second field</li>
 *       <li>null → third field</li>
 *     </ol>
 *     (fields separated by {@code ';'})</li>
 *   <li>Without {@code fmt}, returns {@code "true"} or {@code "false"}</li>
 *   <li>Null returns {@code ""}</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterBoolean conv = new OAConverterBoolean();
 *
 * Boolean b1 = conv.convert(Boolean.class, "Y", null); // true
 * Boolean b2 = conv.convert(Boolean.class, "0", null); // false
 * Boolean b3 = conv.convert(Boolean.class, "no", "yes;no;null"); // false
 * Boolean b4 = conv.convert(Boolean.class, "maybe", "yes;no;null"); // null
 *
 * String s1 = conv.convertToString(Boolean.TRUE, "yes;no;none"); // "yes"
 * String s2 = conv.convertToString(null, "yes;no;none");         // "none"
 * }</pre>
 *
 * @see com.viaoa.converter.OAConverter
 * @see OAStr#format(String, String)
 */
public class OAConverterBoolean implements OAConverterInterface<Boolean> {

	
    /**
     * Converts the supplied {@code fromValue} into a {@link Boolean} using optional
     * format rules. See the class-level documentation for full conversion rules.
     *
     * @param thisClass the target type (always {@code Boolean.class})
     * @param fromValue the value to convert; may be {@code null}
     * @param fmt       optional semicolon-delimited mask supporting custom text
     *                  representations for true/false/null (e.g. "yes;no;unknown")
     *
     * @return
     *     <ul>
     *       <li>{@link Boolean} value determined by type mapping</li>
     *       <li>{@code Boolean.FALSE} when {@code fromValue} is {@code null}</li>
     *       <li>{@code null} if {@code fmt} is supplied and {@code fromValue}
     *           does not match any token</li>
     *     </ul>
     *
     * @see OAStr#isNotEmpty(CharSequence)
     * @see java.lang.Boolean
     */
	@Override
	public Boolean convert(Class<Boolean> thisClass, Object fromValue, String fmt) {
        if (fromValue instanceof Boolean) {
            return (Boolean) fromValue;
        }
        if (fromValue == null) {
            return Boolean.FALSE;
        }
        
        boolean b = false;
        if (fromValue instanceof String) {
            String str = (String)fromValue;
            if (fmt != null && fmt.length() > 0) {
                String s = OAString.field(fmt,";",1);
                b = (s.equalsIgnoreCase(str));
                if (!b) {
                    s = OAString.field(fmt,";",2);
                    b = (s.equalsIgnoreCase(str));
                    if (!b) return null; // does not match either
                    b = false;
                }
            }
            else {
                if (str.length() == 0) {
                    b = false;
                }
                else if (str.length() == 1) {
                    char c = str.charAt(0);
                    if (c == 'F' || c == 'f' || c == 'N' || c == 'n' || (Character.isDigit(c) && c == '0')) b = false;
                    else b = true;
                }
                else if (OAString.isNumber(str)) {
                    b = false;
                    int cnt = 0;
                    for (int i=0; !b && i<str.length(); i++) {
                        char c = str.charAt(i);
                        if (c == '.') {
                            if (cnt++ > 0) b = true;
                        }
                        else if (c != '0') b = true;
                    }
                }
                else {
                    if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("no")) b = false;
                    else b = (str.length() > 0);
                }
            }
            return Boolean.valueOf(b);
        }
            
        if (fromValue instanceof Number) {
            return Boolean.valueOf(((Number) fromValue).doubleValue() != 0.0);
        }
        char c = 0;
        b = false;
        if (fromValue instanceof Byte) {
             c = (char) ((Byte)fromValue).byteValue();
             b = true;
        }            
        if (fromValue instanceof Character) {
            c = ((Character)fromValue).charValue();
            b = true;
        }
        if (b) {
            if (c == 'T' || c == 't' || c == 'Y' || c == 'y' || (Character.isDigit(c) && c != '0')) b = true;
            return Boolean.valueOf(b);
        }
        return (fromValue != null);
    }

    /**
     * Converts a {@link Boolean} into a formatted {@link String} value.
     *
     * <p>When {@code fmt} is provided, it must contain one, two, or three
     * semicolon-delimited fields representing:</p>
     * <ol>
     *   <li>value for {@code true}</li>
     *   <li>value for {@code false} (optional)</li>
     *   <li>value for {@code null} (optional)</li>
     * </ol>
     *
     * <p>Behavior summary:</p>
     * <ul>
     *   <li>No {@code fmt} → {@code "true"} or {@code "false"}</li>
     *   <li>{@code fromValue == null} → empty string unless {@code fmt} supplies a null token</li>
     * </ul>
     *
     * @param fromValue Boolean to format; may be {@code null}
     * @param fmt       optional semicolon-delimited format mask; may be {@code null}
     *
     * @return non-null formatted string for UI output
     *
     * @see OAStr#format(String, String)
     */
	@Override
	public String convertToString(Boolean fromValue, String fmt) {
        // fmt is three values to use for true/false/null sep by ';'  ex: "yes;no;none"
		String s;
        if (fmt != null) {
            if (fromValue == null) s = OAString.field(fmt, ";", 3);
            else if (fromValue.booleanValue()) s = OAString.field(fmt, ";", 1);
            else s = OAString.field(fmt, ";", 2);
        }
        else {
	        if (fromValue == null) s = "";
	        else s = fromValue.toString();
        }
		return (s == null ? "" : s);
    }
}


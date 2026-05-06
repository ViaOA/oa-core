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
package com.viaoa.converter;

import java.awt.Color;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import com.viaoa.datetime.OADateTime;
import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;
import com.viaoa.model.oa.VEnum;
import com.viaoa.reflect.OAReflect;

/**
 * Converter for parsing and formatting numeric values, supporting a wide variety
 * of source types and optional {@link DecimalFormat} formatting patterns.
 *
 * <h3>Conversion to {@code Number}</h3>
 * Supported input types:
 * <ul>
 *   <li>{@code null} → {@code Double.valueOf(0)}</li>
 *   <li>Any {@link Number} subtype</li>
 *   <li>{@link String} — parsed using {@code fmt} when provided</li>
 *   <li>{@code byte[]} — converted to {@link BigInteger}</li>
 *   <li>{@link Character} — Unicode numeric value</li>
 *   <li>{@link Boolean} — {@code true → 1}, {@code false → 0}</li>
 *   <li>{@link java.awt.Color} — packed RGB integer representation</li>
 *   <li>{@link java.awt.Rectangle} — packed 64-bit integer encoding:
 *     <pre>{@code [x:16 bits][y:16 bits][width:16 bits][height:16 bits]}</pre>
 *   </li>
 *   <li>{@link Enum} — ordinal value</li>
 *   <li>{@link OADateTime} — epoch milliseconds via {@link OADateTime#getTime()}</li>
 * </ul>
 *
 * <p>When parsing a string:</p>
 * <ol>
 *   <li>Try {@code fmt} as provided (default: {@code "#,###"})</li>
 *   <li>If that fails, remove grouping/currency characters
 *       via {@link #cleanNumber(String)}</li>
 *   <li>If still failing, retry with default pattern</li>
 * </ol>
 *
 * <p>Suffix multipliers supported:</p>
 * <ul>
 *   <li>{@code 12k → 12000}</li>
 *   <li>{@code 3.2M → 3200000}</li>
 * </ul>
 *
 * <h3>Conversion to {@code String}</h3>
 * <ul>
 *   <li>Numeric formats use {@link DecimalFormat#format(Object)}</li>
 *   <li>Alignment/formatting masks with R/L/C use {@link OAStr#format(String, String)}</li>
 * </ul>
 *
 *
   * <p>
 * Format String Symbol Meaning (same as DecimalFormat)
 * 
 * <pre>
 0      a digit, if no digit exists, then '0' will be used. 
             ex: '000' for 38 = '038'
 #      a digit, zero shows as absent
             ex: '#' for 8204 = '8204'
 .      placeholder for decimal separator
 ,      placeholder for grouping separator.
 ;      separates formats.
 -      default negative prefix.
 %      multiply by 100 and show as percentage
 (\u2030) � multiply by 1000 and show as per mille
 (\u00A4) � currency sign; replaced by currency symbol; if
         doubled, replaced by international currency symbol.
         If present in a pattern, the monetary decimal separator
         is used instead of the decimal separator.
 X      any other characters can be used in the prefix or suffix
 '      used to quote special characters in a prefix or suffix.
 </pre>
 <p>
 Examples:
 <pre>
 IntegerFormat     = #,###
 DecimalFormat     = #,##0.00
 MoneyFormat       = \u00A4#,##0.00
 BooleanFormat     = true;false;null
 </pre>
 *
 * <h3>Performance & Concurrency</h3>
 * <ul>
 *   <li>Pools {@link DecimalFormat} instances keyed by format pattern</li>
 *   <li>Exclusive formatter ownership enforced using {@link ReentrantReadWriteLock}</li>
 *   <li>Each pooled formatter is reused only when {@code used == false}</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * OAConverterNumber conv = new OAConverterNumber();
 *
 * Number n1 = conv.convert(Double.class, "123.45", "#,##0.00"); // 123.45
 * Number n2 = conv.convert(Long.class, new OADateTime(), null); // epoch millis
 *
 * String s1 = conv.convertToString(1234.5, "#,##0.00"); // "1,234.50"
 * String s2 = conv.convertToString(42, "R10");          // right-align
 * }</pre>
 *
 * <p><strong>Note:</strong> Rounding behavior is controlled by DecimalFormat when
 * fractional digits are truncated.</p>
 *
 * @see OAConverter
 * @see DecimalFormat
 * @see OAStr#format(String, String)
 */
public class OAConverterNumber implements OAConverterInterface<Number> {

	/**
	 * Shared zero value used as the default numeric result when the source
	 * value is {@code null}.
	 */
	protected static final Double DOUBLE_ZERO = Double.valueOf(0.0d);	

	/**
	 * Default {@link DecimalFormat} pattern used when no format is supplied.
	 */
	protected static final String DEFAULT_PATTERN = "#,###";
	
	/**
	 * Regular expression pattern used to remove grouping, currency,
	 * and whitespace characters from numeric strings.
	 */
	protected static final Pattern CLEAN_PATTERN = Pattern.compile("[,$ ]");	
	
	/**
	 * Pool of reusable {@link DecimalFormat} instances keyed by format pattern.
	 */
	protected final List<FormatPool> alFormatPool = new ArrayList<>();
	
	/**
	 * Lock used to coordinate access to the {@link #alFormatPool} and formatter usage.
	 */
	protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	
    /**
     * Converts a value into a {@link Number} using an optional format pattern.
     * <p>
     * The conversion behavior depends on the runtime type of {@code value}:
     * <ul>
     *   <li>{@code null} → {@code Double.valueOf(0)}</li>
     *   <li>{@code Number} → returned as-is (or converted to requested {@code Number} subclass)</li>
     *   <li>{@code String} → parsed using {@link DecimalFormat} with the given {@code fmt}</li>
     *   <li>{@code byte[]} → new {@link BigInteger} from the byte array</li>
     *   <li>{@code Character} → Unicode numeric code point</li>
     *   <li>{@code Boolean} → {@code true → 1} / {@code false → 0}</li>
     *   <li>{@link java.awt.Color} → packed RGB integer value</li>
     *   <li>{@link java.awt.Rectangle} → packed 64-bit value
     *       {@code [x][y][width][height]} (16 bits each)</li>
     *   <li>{@link Enum} → {@code ordinal()}</li>
     *   <li>{@link OADateTime} → epoch milliseconds from {@link OADateTime#getTime()}</li>
     * </ul>
     *
     * <h4>String Parsing Behavior</h4>
     * <ol>
     *   <li>Attempt using provided format {@code fmt}
     *       (default: {@code "#,###"})</li>
     *   <li>If parsing fails, invalid chars such as {@code $, , space}
     *       are removed via {@link #cleanNumber(String)}</li>
     *   <li>Retry with cleaned value</li>
     *   <li>On final failure: return {@code null}</li>
     * </ol>
     *
     * <p>Suffix multipliers such as {@code k} and {@code M} are supported:
     * {@code "25k" → 25000}, {@code "3.5M" → 3500000}.</p>
     *
     * @param clazz The desired {@code Number} subclass to return (ex: {@code Integer.class} or {@code Double.class})
     * @param value The value to convert; may be {@code null}
     * @param fmt   Optional formatting/parsing pattern; may be {@code null}
     *
     * @return a {@link Number} matching {@code clazz}, zero for {@code null},
     *         or {@code null} if conversion is not possible
     *
     * @see #cleanNumber(String)
     * @see DecimalFormat
     */
	@Override
	public Number convert(Class<Number> clazz, Object value, String fmt) {
		if (clazz == null) {
			return null;
		}

		if (value != null && value.getClass().equals(clazz)) {
			return (Number) value;
		}
		if (clazz.isPrimitive()) {
			clazz = OAReflect.getPrimitiveClassWrapper(clazz);
		}

		Number num = null;
		if (value == null) {
			num = DOUBLE_ZERO;
		} else if (value instanceof Number) {
			num = (Number) value;
		} else if (value instanceof String) {
			String sValue = (String) value;
			if (sValue.length() == 0) {
				num = DOUBLE_ZERO;
			} else {
				if (fmt == null) {
					fmt = DEFAULT_PATTERN;
				} else {
					fmt = fmt.replace('$', '\u00A4');
				}

				for (int i = 0; i < 3; i++) {
					FormatPool fp = getFormatter(fmt);
					final ParsePosition pp = new ParsePosition(0);
					try {
						pp.setIndex(0);
						num = fp.decimalFormat.parse(sValue, pp);
						if (pp.getIndex() == sValue.length()) {
							break;
						}
						num = null;
					} catch (Exception e) {
					} finally {
						releaseFormatter(fp);
					}

					if (i == 0) {
						sValue = cleanNumber(sValue);
					} else if (i == 1) {
						fmt = DEFAULT_PATTERN;
					}
				}
			}
		} else if (value instanceof Character) {
			num = Integer.valueOf(((Character) value).charValue());
		} else if (value instanceof Boolean) {
			num = Integer.valueOf(((Boolean) value).booleanValue() == true ? 1 : 0);
		} else if (value instanceof OADateTime) {
			num = Long.valueOf(((OADateTime) value).getTime());
		} else if (value instanceof Rectangle) {
			Rectangle r = (Rectangle) value;
			long l = 0L;

			l += ((long) r.x) << 48;
			l += ((long) r.y) << 32;
			l += ((long) r.width) << 16;
			l += ((long) r.height);
			num = Long.valueOf(l);
		} else if (value instanceof Color) {
			Color c = (Color) value;
			num = Long.valueOf(c.getRGB());
		} else if (value instanceof Enum) {
			Enum<?> e = (Enum<?>) value;
			num = Long.valueOf(e.ordinal());
		} else if (value instanceof VEnum) {
			VEnum e = (VEnum) value;
			num = Long.valueOf(e.getValue());
		}

		if (value instanceof byte[]) {
			num = new java.math.BigInteger((byte[]) value);
		}

		if (num != null && clazz != null) {
			if (num.getClass().equals(clazz)) {
				;
			} else if (clazz.equals(Integer.class)) {
				num = Integer.valueOf(num.intValue());
			} else if (clazz.equals(Long.class)) {
				num = Long.valueOf(num.longValue());
			} else if (clazz.equals(BigDecimal.class)) {
				if (num instanceof BigDecimal) { /* no-op */ }
				else if (num instanceof BigInteger) num = new BigDecimal((BigInteger) num);
				else num = BigDecimal.valueOf(num.doubleValue());				
			} else if (clazz.equals(Double.class)) {
				num = Double.valueOf(num.doubleValue());
			} else if (clazz.equals(Float.class)) {
				num = Float.valueOf(num.floatValue());
			} else if (clazz.equals(Short.class)) {
				num = Short.valueOf(num.shortValue());
			} else if (clazz.equals(Byte.class)) {
				num = Byte.valueOf(num.byteValue());
			}
		}
		return num;
	}

	/**
	 * Removes non-numeric formatting characters from a numeric string and
	 * applies suffix multipliers such as {@code k} and {@code M}.
	 *
	 * @param value the raw numeric string
	 * @return a cleaned numeric string suitable for parsing
	 */
	String cleanNumber(String value) {
		value = CLEAN_PATTERN.matcher(value).replaceAll("");

		int x = value.length();
		if (x > 0) {
			char c = value.charAt(x - 1);
			if (c == 'k' || c == 'K') {
				value = value.substring(0, x - 1) + "000";
			}
			else if (c == 'm' || c == 'M') {
				value = value.substring(0, x - 1) + "000000";
			}
		}
		return value;
	}


	/**
	 * Returns a {@link FormatPool} containing a {@link DecimalFormat} for
	 * the specified format pattern.
	 *
	 * @param fmt the desired decimal format pattern
	 * @return an acquired {@link FormatPool} instance
	 */
	protected FormatPool getFormatter(String fmt) {
		if (fmt == null) fmt = "";
		FormatPool fp = null;
		FormatPool fpAvail = null;
		try {
			lock.writeLock().lock();
			for (FormatPool fpx : alFormatPool) {
				if (fpx.used) continue;
				if (fmt.equals(fpx.fmt)) {
					fp = fpx;
					break;
				}
				fpAvail = fpx;
			}

			if (fp == null) {
				if (fpAvail != null) {
					fp = fpAvail;
					fp.decimalFormat.applyPattern(fmt);
					fp.fmt = fmt;
				}
				else {
					DecimalFormat dfx = new DecimalFormat(fmt);
					dfx.setRoundingMode(RoundingMode.HALF_UP);
					fp = new FormatPool(fmt, dfx);
					alFormatPool.add(fp);
				}
			}
			fp.used = true;
		}
		finally {
			lock.writeLock().unlock();
		}
		
		return fp;
	}

	/**
	 * Releases a previously acquired {@link FormatPool} back to the pool.
	 *
	 * @param fp the formatter pool entry to release
	 */
	protected void releaseFormatter(FormatPool fp) {
		try {
			lock.writeLock().lock();
			fp.used = false;
		}
		finally {
			lock.writeLock().unlock();
		}
	}
	
	
	/**
	 * Container class used to manage a pooled {@link DecimalFormat}
	 * and its usage state.
	 */
	static class FormatPool {
		/**
		 * Format pattern associated with this pooled formatter.
		 */
		volatile String fmt;

		/**
		 * Flag indicating whether this formatter is currently in use.
		 */
		volatile boolean used;
		
		/**
		 * {@link DecimalFormat} instance used for numeric parsing and formatting.
		 */
		DecimalFormat decimalFormat;

		/**
		 * Creates a new formatter pool entry.
		 *
		 * @param fmt the format pattern
		 * @param deciFmt the decimal formatter instance
		 */
		public FormatPool(String fmt, DecimalFormat deciFmt) {
			this.fmt = fmt;
			this.decimalFormat = deciFmt;
		}
	}

    /**
     * Formats a numeric value into a textual representation using an optional
     * format pattern.
     *
     * <p>If the format mask contains alignment symbols:</p>
     * <ul>
     *   <li>{@code 'R'} → right align</li>
     *   <li>{@code 'L'} → left align</li>
     *   <li>{@code 'C'} → center align</li>
     * </ul>
     * then {@link OAStr#format(String, String)} is used to apply alignment rules.
     *
     * <p>Otherwise, the given {@code fmt} is interpreted as a
     * {@link DecimalFormat} pattern.</p>
     *
     * <p>If {@code fromValue} is {@code null}, an empty string ({@code ""}) is returned.</p>
     *
     * @param fromValue Value to be formatted; may be {@code null}
     * @param fmt       Optional formatting/align mask; may be {@code null}
     *
     * @return formatted string, never {@code null}
     *
     * @see DecimalFormat#format(Object)
     * @see OAStr#format(String, String)
     */
	@Override
	public String convertToString(Number fromValue, String fmt) {
		String s;
		if (fromValue == null) {
			return "";
		}
		if (OAStr.isEmpty(fmt)) {
			s = fromValue.toString();
		}
		else { 
			if (fmt.length() > 1 && (fmt.indexOf('R') >= 0 || fmt.indexOf('L') >= 0 || fmt.indexOf('C') >= 0)) {
				s = OAString.format(fromValue.toString(), fmt);
			}
			else {
				FormatPool fp = getFormatter(fmt);
				try {
					s = fp.decimalFormat.format(fromValue);
				} finally {
					releaseFormatter(fp);
				}
			}
		}
		if (s == null) s = "";
		return s;
	}
	
}

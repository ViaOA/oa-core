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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;
import com.viaoa.math.OAMath;
import com.viaoa.model.oa.VEnum;
import com.viaoa.reflect.OAReflect;

/**
 * <p>
 * Core conversion utility used throughout the OA framework.
 * Provides a centralized mechanism to convert values between
 * supported Java types including:
 * </p>
 * <ul>
 *   <li>String formatting and parsing</li>
 *   <li>Numeric type conversions</li>
 *   <li>Date/Time conversions (Java Time & OA temporal types)</li>
 *   <li>Boolean, Character, Class, Enum and UI types (Color, Font, Dimension, Rectangle)</li>
 * </ul>
 *
 * <p>
 * The conversion system is extensible. Custom type converters can be
 * registered globally using {@link #addConverter(Class, OAConverterInterface)}.
 * Converters are selected by target class and searched up the class
 * inheritance hierarchy until a match is found.
 * </p>
 *
 * <p>
 * Most OA applications use {@link #toString(Object, String)} and
 * {@link #convert(Class, Object, String)} either directly or indirectly
 * through OA UI and serialization components.
 * </p>
 *
 * @see OAConverterInterface
 * @see OAConverterNumber
 * @see OAConverterDate
 */
public class OAConverter {

    /**
     * Global registry of converters keyed by logical model class.
     * Populated statically during startup through {@link #addConverter}.
     *
     * <p>Thread safe and cached for performance.</p>
     */
	protected static final Map<Class<?>, OAConverterInterface<?>> hmClassConverter = new ConcurrentHashMap<>(10, 0.75f);
	
	
    /**
     * Default formatting values used by {@link #getFormat(Class)}.
     * These values are not automatically applied unless requested by
     * the caller, typically through:
     * <ul>
     *   <li>{@link #toString(Object, boolean)}</li>
     *   <li>UI components or serialization pipelines</li>
     * </ul>
     */	
	protected static String dateFormat, timeFormat, dateTimeFormat,
			integerFormat, decimalFormat, bigDecimalFormat, moneyFormat = "\u00A4#,##0.00", booleanFormat;

	
	static {
		addConverter(String.class, new OAConverterString());
		addConverter(Number.class, new OAConverterNumber());
		addConverter(Character.class, new OAConverterCharacter());
		addConverter(Boolean.class, new OAConverterBoolean());
		addConverter(BigDecimal.class, new OAConverterBigDecimal());
		addConverter(java.sql.Date.class, new OAConverterSqlDate());
		addConverter(java.sql.Time.class, new OAConverterTime());
		addConverter(java.sql.Timestamp.class, new OAConverterTimestamp());
		addConverter(java.util.Date.class, new OAConverterDate());
		addConverter(com.viaoa.datetime.OADateTime.class, new OAConverterOADateTime());
		addConverter(com.viaoa.datetime.OADate.class, new OAConverterOADate());
		addConverter(com.viaoa.datetime.OATime.class, new OAConverterOATime());
		addConverter(Calendar.class, new OAConverterCalendar());
/* moved to oa-corre-ui repo		
		addConverter(java.awt.Point.class, new OAConverterPoint());
		addConverter(java.awt.Dimension.class, new OAConverterDimension());
		addConverter(java.awt.Rectangle.class, new OAConverterRectangle());
		addConverter(java.awt.Color.class, new OAConverterColor());
		addConverter(java.awt.Font.class, new OAConverterFont());
*/		
		addConverter(Enum.class, new OAConverterEnum());
		addConverter(VEnum.class, new OAConverterVEnum());
		addConverter(TimeZone.class, new OAConverterTimeZone());
		addConverter(Class.class, new OAConverterClass());
		addConverter(LocalDateTime.class, new OAConverterLocalDateTime());
		addConverter(LocalDate.class, new OAConverterLocalDate());
		addConverter(LocalTime.class, new OAConverterLocalTime());
		addConverter(ZonedDateTime.class, new OAConverterZonedDateTime());
		addConverter(Instant.class, new OAConverterInstant());
		addConverter(ZoneId.class, new OAConverterZoneId());
	}

    /**
     * Registers a converter instance for a specific Java type.
     * <p>
     * If a converter already exists for the class, the new one
     * replaces it.
     *
     * @param clazz target type to convert to/from
     * @param converter implementation handling conversions
     */
	public static <T> void addConverter(Class<T> clazz, OAConverterInterface<T> converter) {
	    hmClassConverter.put(clazz, converter);
	}
	
    /**
     * Lookup a converter for a specific class.
     *
     * <p>Resolution behavior:</p>
     * <ul>
     *   <li>If class is primitive, it is automatically boxed</li>
     *   <li>Direct lookup in registry</li>
     *   <li>Search up class inheritance hierarchy (superclasses only)</li>
     *   <li>Returns null if no converter is found</li>
     * </ul>
     *
     * @return converter instance or null
     */	
	@SuppressWarnings("unchecked")
	public static <T> OAConverterInterface<T> getConverter(Class<T> clazz) {
	    if (clazz == null) return null;
	    if (clazz.isPrimitive()) {
	        clazz = (Class<T>) OAReflect.getPrimitiveClassWrapper(clazz);
	    }

	    for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
	        OAConverterInterface<?> ci = hmClassConverter.get(c);
	        if (ci != null) {
	            return (OAConverterInterface<T>) ci;
	        }
	    }
	    return null;
	}	
	
    /**
     * Returns the default format string associated with a specific type.
     * <p>
     * This is not automatically applied unless requested by the caller.
     * Used primarily by:
     * </p>
     * <ul>
     *   <li>UI display formatting</li>
     *   <li>toString(obj, true)</li>
     * </ul>
     *
     * @param clazz type being formatted
     * @return suggested default format, or null if none
     */
	public static String getFormat(Class clazz) {
		if (clazz == null || clazz.equals(String.class)) {
			return null;
		}
		if (clazz.equals(BigDecimal.class)) {
			return getBigDecimalFormat();
		}
		if (OAReflect.isInteger(clazz)) {
			return getIntegerFormat();
		}
		if (OAReflect.isFloat(clazz)) {
			return getDecimalFormat();
		}

		if (Date.class.equals(clazz)) {
			return getDateFormat();
		}
		if (java.sql.Time.class.equals(clazz)) {
			return getTimeFormat();
		}
		if (java.sql.Timestamp.class.equals(clazz)) {
			return getDateTimeFormat();
		}

		if (OADateTime.class.equals(clazz)) {
			return getDateTimeFormat();
		}
		if (OADate.class.equals(clazz)) {
			return getDateFormat();
		}
		if (OATime.class.equals(clazz)) {
			return getTimeFormat();
		}
		if (Calendar.class.isAssignableFrom(clazz)) {
			return getDateFormat();
		}

		if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
			return getBooleanFormat();
		}
		
		if (clazz.equals(LocalDate.class)) {
		    return getDateFormat();
		}
		if (clazz.equals(LocalTime.class)) {
		    return getTimeFormat();
		}
		if (clazz.equals(LocalDateTime.class)) {
		    return getDateTimeFormat();
		}
		if (clazz.equals(ZonedDateTime.class)) {
		    return getDateTimeFormat();
		}
		if (clazz.equals(Instant.class)) {
		    return getDateTimeFormat();
		}
		if (clazz.equals(ZoneId.class)) {
		    return null;
		}

		return null;
	}

	/**
	 * Default format to use for dates.
	 * <p>
	 * Note: OAConverter.toString() will not automatically use this format unless it is sent as a parameter. This method is to be used as a
	 * global area for other APIs to store default system formatting. Note: OADate is used for formatting dates and has its own default
	 * formatting if one is not supplied.
	 *
	 * @see OADate#OADate for format options and default formatting/parsing values
	 */
	public static String getDateFormat() {
		if (dateFormat == null) {
			return OADate.getGlobalOutputFormat();
		}
		return dateFormat;
	}

	/**
	 * Set default format for Dates.
	 *
	 * @see #getDateFormat
	 */
	public static void setDateFormat(String fmt) {
		dateFormat = fmt;
	}

	/**
	 * default format to use for times. Note: OAConverter.toString() will not automatically use this format unless it is sent as a
	 * parameter. This method is to be used as a global area for other APIs to store default system formatting. Note: OADate is used for
	 * formatting times and has its own default formatting.
	 *
	 * @see OADate#OADate for format options and default formatting/parsing values
	 */
	public static String getTimeFormat() {
		if (timeFormat == null) {
			return OATime.getGlobalOutputFormat();
		}
		return timeFormat;
	}

	/**
	 * Set default format for Times.
	 *
	 * @see #getTimeFormat
	 */
	public static void setTimeFormat(String fmt) {
		timeFormat = fmt;
	}

	/**
	 * default format to use for datetimes. Note: OAConverter.toString() will not automatically use this format unless it is sent as a
	 * parameter. This method is to be used as a global area for other APIs to store default system formatting. Note: OADate is used for
	 * formatting times and has its own default formatting.
	 *
	 * @see OADate#OADate for format options and default formatting/parsing values
	 */
	public static String getDateTimeFormat() {
		if (dateTimeFormat == null) {
			return OADateTime.getGlobalOutputFormat();
		}
		return dateTimeFormat;
	}

	/**
	 * Set default format for DateTimes.
	 *
	 * @see #getDateTimeFormat
	 */
	public static void setDateTimeFormat(String fmt) {
		dateTimeFormat = fmt;
	}

	/**
	 * Default format to use for integer values.
	 * <p>
	 * Note: OAConverter.toString() will not automatically use this format unless it is sent as a parameter. This method is to be used as a
	 * global area for other APIs to store default system formatting. see OANumberConverter#OANumberConverter for format options
	 */
	public static String getIntegerFormat() {
		return integerFormat;
	}

	/**
	 * Set default format for Integers.
	 *
	 * @see #getIntegerFormat
	 */
	public static void setIntegerFormat(String fmt) {
		integerFormat = fmt;
	}

	/**
	 * Default format to use for decimal numbers (floats, doubles).
	 * <p>
	 * Note: OAConverter.toString() will not automatically use this format unless it is sent as a parameter. This method is to be used as a
	 * global area for other APIs to store default system formatting.
	 *
	 * @see OAConverterNumber#OAConverterNumber for format options
	 */
	public static String getDecimalFormat() {
		return decimalFormat;
	}

	/**
	 * Set default format for Decimal numbers (floats/doubles).
	 *
	 * @see #getDecimalFormat
	 */
	public static void setDecimalFormat(String fmt) {
		decimalFormat = fmt;
	}

	/**
	 * Default format to use for BigDecimal numbers, mostly to represent currency.
	 */
	public static String getBigDecimalFormat() {
		return bigDecimalFormat;
	}

	/**
	 * Default format to use for BigDecimal numbers, mostly to represent currency.
	 *
	 * @see OAConverterNumber#OAConverterNumber for format options
	 */
	public static void setBigDecimalFormat(String fmt) {
		bigDecimalFormat = fmt;
	}

	/**
	 * Default format to use for Money/Currency.
	 */
	public static String getMoneyFormat() {
		return moneyFormat;
	}

	public static String getCurrencyFormat() {
		return moneyFormat;
	}

	/**
	 * Default format to use for Money/Currency.
	 */
	public static void setMoneyFormat(String fmt) {
		moneyFormat = fmt;
	}

	/**
	 * default format to use for boolean values when converted to a String.
	 * <p>
	 * Note: OAConverter.toString() will not automatically use this format unless it is sent as a parameter. This method is to be used as a
	 * global area for other APIs to store default system formatting. param fmt is a String with a semicolon seperating the three values
	 * "true;false;null" Example: ("t;f;");
	 */
	public static String getBooleanFormat() {
		return booleanFormat;
	}

	/**
	 * Set default format for booleans.
	 *
	 * @see #getBooleanFormat
	 */
	public static void setBooleanFormat(String fmt) {
		booleanFormat = fmt;
	}


	/**
	 * Convert a given value to a specified Java type.
	 *
	 * <p>Resolution behavior:</p>
	 * <ul>
	 *   <li>If {@code clazz} is primitive, it is automatically boxed</li>
	 *   <li>Searches converter registry based on the target class</li>
	 *   <li>If no converter is found, returns the value if already assignable</li>
	 *   <li>If value is already compatible and no format is applied, it is returned as-is</li>
	 *   <li>Otherwise delegates to the type converter</li>
	 * </ul>
	 *
	 * <p>
	 * This method is the core conversion entry point used by UI, persistence,
	 * and serialization frameworks throughout OA.
	 * </p>
	 *
	 * @param clazz target type for desired result
	 * @param value original value, may be null or any type
	 * @param fmt optional format used by converter (may be null or empty)
	 * @param <T> resulting type
	 * @return converted value or null if no valid conversion could be performed
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convert(Class<T> clazz, Object value, String fmt) {
		if (clazz == null) return null;
		if (clazz.isPrimitive()) {
			clazz = (Class<T>) OAReflect.getPrimitiveClassWrapper(clazz);
		}

		OAConverterInterface<T> oci = OAConverter.getConverter(clazz);
		if (oci == null) {
			if (value != null && clazz.isAssignableFrom(value.getClass())) {
				return (T) value;
			}
			return null;
		}
		if (value != null && clazz.isAssignableFrom(value.getClass()) && OAStr.isEmpty(fmt)) {
			return (T) value;
		}
		return oci.convert(clazz, value, fmt);
	}

	/**
	 * Convert a value to the specified Java type using default formatting rules.
	 *
	 * @param clazz target type
	 * @param value source value, may be null
	 * @return converted value or null if no conversion exists
	 * @see #convert(Class, Object, String)
	 */
	public static <T> T convert(Class<T> clazz, Object value) {
		return convert(clazz, value, null);
	}

	/**
	 * Convert an Object to a {@code double}.
	 *
	 * @param value source value to convert
	 * @return converted double value
	 * @throws IllegalArgumentException if value cannot be converted to a number
	 */	
	public static double toDouble(Object value) {
		Number num = (Number) convert(double.class, value);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toDouble(): '" + value + "' cant be converted to double");
		}
		return num.doubleValue();
	}

	/**
	 * Convert an Object to a {@link BigDecimal}.
	 *
	 * @param value source value to convert
	 * @return BigDecimal result
	 * @throws IllegalArgumentException if value cannot be converted to a BigDecimal
	 */
	public static BigDecimal toBigDecimal(Object value) {
		BigDecimal num = (BigDecimal) convert(BigDecimal.class, value);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toBigDecimal(): '" + value + "' cant be converted to BigDecimal");
		}
		return num;
	}

	/**
	 * Convenience alias for {@link #toBigDecimal(Object)}.
	 */
	public static BigDecimal toBD(Object value) {
		return toBigDecimal(value);
	}

	/**
	 * Convert a decimal String into a BigDecimal instance.
	 *
	 * @param value string representing a numeric value
	 * @return BigDecimal parsed value
	 * @throws NumberFormatException if string is not a valid number
	 */
	public static BigDecimal toBigDecimal(String value) {
		BigDecimal bd = new BigDecimal(value);
		return bd;
	}

	/** Alias for {@link #toBigDecimal(String)}. */
	public static BigDecimal toBD(String value) {
		BigDecimal bd = new BigDecimal(value);
		return bd;
	}

	/**
	 * Convert a double into a BigDecimal, preserving decimal precision.
	 *
	 * @param value numeric value
	 * @return BigDecimal representation
	 */
	public static BigDecimal toBigDecimal(double value) {
		BigDecimal bd = BigDecimal.valueOf(value);
		return bd;
	}
	
	/** Alias for {@link #toBigDecimal(double)}. */
	public static BigDecimal toBD(double value) {
		return toBigDecimal(value);
	}

	/**
	 * Convert a double into a scaled BigDecimal.
	 *
	 * @param value          numeric value
	 * @param decimalPlaces  number of decimal places to round to
	 * @return scaled BigDecimal
	 */	
	public static BigDecimal toBigDecimal(double value, int decimalPlaces) {
		BigDecimal bd = BigDecimal.valueOf(value);
		if (decimalPlaces >= 0) bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
		return bd;
	}

	public static BigDecimal toBigDecimal(double value, int decimalPlaces, int roundType) {
		if (roundType < 0) roundType = BigDecimal.ROUND_HALF_UP;
		BigDecimal bd = BigDecimal.valueOf(value);
		if (decimalPlaces >= 0) bd = bd.setScale(decimalPlaces, roundType);
		return bd;
	}
	
	/** Alias for {@link #toBigDecimal(double, int)}. */
	public static BigDecimal toBD(double value, int decimalPlaces) {
		return toBigDecimal(value, decimalPlaces);
	}

	/**
	 * Convert an Object to a {@link BigDecimal} using a formatting hint.
	 *
	 * @param value string or numeric value
	 * @param fmt optional format string
	 * @return converted BigDecimal
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static BigDecimal toBigDecimal(Object value, String fmt) {
		BigDecimal num = convert(BigDecimal.class, value, fmt);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toBigDecimal(): '" + value + "' cant be converted to BigDecimal");
		}
		return num;
	}

	/** Alias for {@link #toBigDecimal(Object, String)}. */
	public static BigDecimal toBD(Object value, String fmt) {
		return toBigDecimal(value, fmt);
	}

	/**
	 * Convert an Object to a {@code float}.
	 *
	 * @param value source value
	 * @return converted float value
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static float toFloat(Object value) {
		Number num = convert(float.class, value);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toFloat(): '" + value + "' cant be converted to float");
		}
		return num.floatValue();
	}

	/**
	 * Convert an Object to a {@code long}.
	 *
	 * @param value source value
	 * @return converted long value, {@code 0L} if value is null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static long toLong(Object value) {
		if (value == null) return 0L;
		Number num = convert(long.class, value);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toLong(): '" + value + "' cant be converted to long");
		}
		return num.longValue();
	}

	/**
	 * Convert an Object to an {@code int}.
	 *
	 * @param value source value
	 * @return converted int value, {@code 0} if value is null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static int toInt(Object value) {
		if (value == null) return (int) 0;
		Number num = convert(int.class, value);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toInt(): Object '" + value + "' cant be converted to int");
		}
		return num.intValue();
	}

	/**
	 * Convert an Object to a {@code short}.
	 *
	 * @param value source value
	 * @return converted short value, {@code 0} if value is null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static short toShort(Object value) {
		if (value == null) return (short) 0;
		Number num = (Number) convert(short.class, value);
		if (num == null) {
			throw new IllegalArgumentException("OAConverter.toShort(): '" + value + "' cant be converted to short");
		}
		return num.shortValue();
	}

	/**
	 * Convert an Object to a {@code char}.
	 *
	 * @param value source value
	 * @return converted character, or {@code 0} if null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static char toChar(Object value) {
		if (value == null) return (char) 0;
		Character c = (Character) convert(char.class, value);
		if (c == null) {
			throw new IllegalArgumentException("OAConverter.toChar(): '" + value + "' cant be converted to char");
		}
		return c.charValue();
	}

	/**
	 * Convert an Object to a {@code byte}.
	 *
	 * @param value source value
	 * @return converted byte, or {@code 0} if null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static byte toByte(Object value) {
		if (value == null) return (byte) 0;
		Byte c = convert(byte.class, value);
		if (c == null) {
			throw new IllegalArgumentException("OAConverter.toByte(): '" + value + "' cant be converted to byte");
		}
		return c.byteValue();
	}

	/**
	 * Convert an Object to a {@code boolean}.
	 *
	 * @param value source value
	 * @return converted boolean, {@code false} if null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static boolean toBoolean(Object value) {
		if (value == null) return false;
		Boolean b = (Boolean) convert(boolean.class, value);
		if (b == null) {
			throw new IllegalArgumentException("OAConverter.toBoolean(): '" + value + "' cant be converted to boolean");
		}
		return b.booleanValue();
	}

	/**
	 * Convert an Object to a {@code boolean} using a custom format.
	 *
	 * @param value source value
	 * @param fmt   conversion format (e.g., "yes;no;")
	 * @return converted boolean, {@code false} if null
	 * @throws IllegalArgumentException if conversion fails
	 */
	public static boolean toBoolean(Object value, String fmt) {
		if (value == null) return false;
		Boolean b = (Boolean) convert(boolean.class, value, fmt);
		if (b == null) {
			throw new IllegalArgumentException("OAConverter.toBoolean(): '" + value + "' cant be converted to boolean");
		}
		return b.booleanValue();
	}

	/** Convert a String to {@link OADateTime}.
	 *
	 * @return OADateTime or null if conversion could not be completed.
	 * @see #convert(Class,Object,String)
	 * @see OADateTime
	 */
	public static OADateTime toDateTime(String value, String fmt) {
		return convert(OADateTime.class, value, fmt);
	}

	/**
	 * Convert a String to {@link OADateTime}, using default parsing rules. 
	 *
	 * @return OADateTime or null if conversion could not be completed.
	 * @see #convert(Class,Object,String)
	 */
	public static OADateTime toDateTime(String value) {
		return convert(OADateTime.class, value, null);
	}

	/**
	 * Convert an Object to {@link OADateTime}. 
	 *
	 * @return OADate or null if conversion could not be completed.
	 * @see #convert(Class,Object,String)
	 * @see OADateTime
	 */
	public static OADate toDate(Object value, String fmt) {
		return convert(OADate.class, value, fmt);
	}

	/**
	 * Convert an Object to {@link OADateTime}, using default parsing rules. 
	 *
	 * @return OADate or null if conversion could not be completed.
	 * @see #convert(Class,Object,String)
	 * @see OADateTime
	 */
	public static OADate toDate(Object value) {
		return convert(OADate.class, value);
	}

	/**
	 * Convert a String to {@link OATime}
	 *
	 * @return OATime or null if conversion could not be completed.
	 * @see #convert(Class,Object,String)
	 * @see OADateTime
	 */
	public static OATime toTime(Object value, String fmt) {
		return convert(OATime.class, value, fmt);
	}

	/**
	 * Convert a String to {@link OATime}.
	 *
	 * @return OATime or null if conversion could not be completed.
	 * @see #convert(Class,Object,String)
	 * @see OADateTime
	 */
	public static OATime toTime(Object value) {
		return convert(OATime.class, value);
	}

	/**
	 * Convert an Object to a String, without formatting.
	 *
	 * @return if obj is null then a blank string is returned, otherwise String version of obj.
	 * @see #convert(Class,Object,String)
	 */
	public static String toString(Object obj) {
		return toString(obj, null);
	}

	/**
	 * Convert an Object to a String, with option to use default formatting.
	 *
	 * @return if obj is null then a blank string is returned, otherwise String version of obj.
	 * @see #convert(Class,Object,String)
	 */
	public static String toString(Object obj, boolean bUseDefaultFormat) {
		String fmt = null;
		if (bUseDefaultFormat && obj != null) {
			fmt = getFormat(obj.getClass());
		}
		return toString(obj, fmt);
	}

	/**
	 * Convert a value to String using an optional format.
	 *
	 * @param obj value to convert
	 * @param fmt optional format String
	 * @return String value (never null)
	 */
	public static String toString(Object obj, String fmt) {
		String s = convert(String.class, obj, fmt);
		if (s == null) return "";
		return s;
	}

	/**
	 * Convert a double to a String, using a specific format..
	 *
	 * @return String version of a double
	 * @see OAConverterNumber
	 * @see #convert(Class,Object,String)
	 * @see OAString#format(String,String)
	 */
	public static String toString(double d, String fmt) {
		return convert(String.class, d, fmt);
	}

	public static String toString(double d, boolean bUseDefaultFormat) {
		String fmt = null;
		if (bUseDefaultFormat) {
			fmt = getDecimalFormat();
		}
		return toString(d, fmt);
	}

	/**
	 * Convert a double to a String
	 *
	 * @return String version of a double
	 * @see OAConverterNumber
	 * @see #convert(Class,Object,String)
	 */
	public static String toString(double d) {
		return convert(String.class, d, null);
	}

	/**
	 * Convert a long to a String, using a specific format.
	 *
	 * @return String version of a long
	 * @see OAConverterNumber
	 * @see #convert(Class,Object,String)
	 * @see OAString#format(String,String)
	 */
	public static String toString(long l, String fmt) {
		return (String) convert(String.class, l, fmt);
	}

	/**
	 * Convert a long to a String.
	 *
	 * @return String version of a long
	 * @see OAConverterNumber
	 * @see #convert(Class,Object,String)
	 */
	public static String toString(long l) {
		return convert(String.class, l, null);
	}

	public static String toString(long l, boolean bUseDefaultFormat) {
		String fmt = null;
		if (bUseDefaultFormat) {
			fmt = getIntegerFormat();
		}
		return toString(l, fmt);
	}

	/**
	 * Convert a character to a String.
	 *
	 * @return String version of a character
	 * @see OAConverterCharacter
	 */
	public static String toString(char c) {
		return convert(String.class, c, null);
	}

	public static String toString(char c, boolean bUseDefaultFormat) {
		return toString(c);
	}

	/**
	 * Convert boolean value to a String.
	 *
	 * @param fmt format string to determine values for true, false, null. Ex: "true;false;null", "yes;no;maybe"
	 * @return String version of a boolean
	 * @see OAConverterBoolean
	 * @see #convert(Class,Object,String)
	 * @see OAString#format(String,String)
	 */
	public static String toString(boolean b, String fmt) {
		return convert(String.class, b, fmt);
	}

	public static String toString(boolean b, boolean bUseDefaultFormat) {
		String fmt = null;
		if (bUseDefaultFormat) {
			fmt = getBooleanFormat();
		}
		return toString(b, fmt);
	}

	/**
	 * Convert boolean value to a String.
	 *
	 * @param b   boolean value, can be null if used as "(Boolean) null"
	 * @param fmt format string to determine values for true, false, null. Ex: "true;false;null", "yes;no;maybe"
	 * @return String version of a boolean
	 * @see OAConverterBoolean
	 * @see #convert(Class,Object,String)
	 * @see OAString#format(String,String)
	 */
	public static String toString(Boolean b, String fmt) {
		return convert(String.class, b, fmt);
	}

	/**
	 * Convert boolean value to a String, using Boolean(b).toString()
	 *
	 * @see toString(boolean,String)
	 * @see toString(Boolean,String)
	 * @see #convert(Class,Object,String)
	 */
	public static String toString(boolean b) {
		return convert(String.class, b, null);
	}

	/**
	 * Check whether a value is considered empty.
	 *
	 * <p>Supported empty types:</p>
	 * <ul>
	 *   <li>{@link String} — zero-length or whitespace based on {@code bTrim}</li>
	 *   <li>Arrays — length == 0</li>
	 *   <li>{@link Hub} — size == 0</li>
	 *   <li>{@link Map} — size == 0</li>
	 *   <li>{@link List} — size == 0</li>
	 *   <li>{@link Set} — size == 0</li>
	 * </ul>
	 *
	 * @param obj  value to check
	 * @param bTrim apply trim when evaluating Strings
	 * @return true if value is empty
	 */	
	public static boolean isEmpty(Object obj, boolean bTrim) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			if (bTrim) {
				if (((String) obj).trim().length() == 0) {
					return true;
				}
			} else {
				if (((String) obj).length() == 0) {
					return true;
				}
			}
		} else if (obj.getClass().isArray()) {
			if (Array.getLength(obj) == 0) {
				return true;
			}
		} else if (obj instanceof Hub) {
			if (((Hub) obj).getSize() == 0) {
				return true;
			}
		} else if (obj instanceof Map) {
			if (((Map) obj).size() == 0) {
				return true;
			}
		} else if (obj instanceof Set) {
			if (((Set) obj).size() == 0) {
				return true;
			}
		} else if (obj instanceof List) {
			if (((List) obj).size() == 0) {
				return true;
			}
		}
		return false;
	}

	/** Default empty check without trimming. */
	public static boolean isEmpty(Object obj) {
		return isEmpty(obj, true);
	}
	
	/** Negation of {@link #isEmpty(Object)}. */
	public static boolean isNotEmpty(Object obj) {
		return !isEmpty(obj, true);
	}
	
	/** Negation of {@link #isEmpty(Object, boolean)}. */
	public static boolean isNotEmpty(Object obj, boolean bTrim) {
		return !isEmpty(obj, bTrim);
	}
	
	/**
	 * Round a double to a set amount of decimal places. 
	 * @param value to round
	 * @param decimalPlaces number of decimal places
	 * @return new double
	 */
	public static double round(double value, int decimalPlaces) {
		return OAMath.round(value, decimalPlaces);
	}
	
	public static double round(double value, int decimalPlaces, int roundType) {
		return OAMath.round(value, decimalPlaces, roundType);
	}

}

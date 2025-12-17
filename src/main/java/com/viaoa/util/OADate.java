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
package com.viaoa.util;

import java.sql.Time;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * Immutable date-only value that represents a calendar day independent from
 * time-of-day. OADate is suitable for business rules where wall-clock time is
 * irrelevant (e.g., birthdays, holidays, settlement dates).
 *
 * <p><b>Timezone behavior:</b><br>
 * OADate stores an underlying instant via {@link OADateTime}, but the time
 * fields are always normalized to midnight and ignored for comparisons,
 * formatting, and field access. This ensures that a given date is interpreted
 * consistently across JVM default timezones.
 *
 * <p><b>Key properties:</b>
 * <ul>
 *   <li>Thread-safe and immutable</li>
 *   <li>Parsing supports multiple global formats</li>
 *   <li>Formatting uses a global default when none is specified</li>
 *   <li>Interoperates with {@link java.time.LocalDate}</li>
 * </ul>
 *
 * <p>Default formats used include:
 * <ul>
 *   <li>{@code "MM/dd/yyyy"}</li>
 *   <li>{@code "dd/MM/yyyy"}</li>
 *   <li>{@code "yyyy-MM-dd"} (SQL)</li>
 * </ul>
 *
 * @see OADateTime
 * @see OATime
 */
public class OADate extends OADateTime {
	private static final long serialVersionUID = 1L;

	/**
	 * Standard date format: year-month-day.
	 */
	public final static String Format1 = "yyyy-MM-dd";

	/**
	 * Standard date format: month/day/year.
	 */
	public final static String Format2 = "MM/dd/yyyy";
	
	/**
	 * Standard date format without separators.
	 */
	public final static String Format3 = "yyyyMMdd";
	
	/**
	 * Standard date format using abbreviated month name.
	 */
	public final static String Format4 = "yyyy-MMM-dd";
	
	/**
	 * Standard date format using month name and comma.
	 */
	public final static String Format5 = "MMM dd, yyyy";
	
	/**
	 * Standard date format using abbreviated year.
	 */
	public final static String Format6 = "MMM dd, yy";

	/**
	 * Date format used for JDBC and SQL interactions.
	 */
	public final static String JdbcFormat = "yyyy-MM-dd"; // SQL
	
	/**
	 * Date format used for JSON serialization.
	 */
	public final static String JsonFormat = Format1;

    // format used by browser: "YYYY-MM-DD";
    // same as JsonFormat
    // public final static String HtmlInputDateFormat = "yyyy-MM-dd"; // java format to use
    
    
	// Unique for this subclass
	/** default output format. Default is DateFormat.SHORT */
	protected static String dateOutputFormat;

	/**
	 * Collection of date formats used when parsing strings into dates.
	 */
	private static Vector vecDateParseFormat = new Vector(10, 10);

	static {
		setLocale(Locale.getDefault());
	}

	/**
	 * Sets locale-specific date parsing and output formats.
	 *
	 * @param loc the locale to use when determining date formats
	 */
	public static void setLocale(Locale loc) {
		vecDateParseFormat = new Vector(15, 10);
		String s = getFormat(DateFormat.SHORT, loc);
		boolean bMonthFirst = true;
		boolean bYearFirst = false;
		if (s != null && s.length() > 0) {
			char ch = s.charAt(0);
			if (ch != 'M') {
				bMonthFirst = false;
			}
			if (ch == 'y') {
				bYearFirst = true;
			}
			;
			vecDateParseFormat.addElement(s);
		}
		if (bMonthFirst) {
			vecDateParseFormat.addElement("MM/dd/yy"); // must be before "MM/dd/yyyy" since "MM/dd/yyyy" will convert 5/4/65 -> 05/04/0065
			vecDateParseFormat.addElement("MM/dd/yyyy");
			dateOutputFormat = "MM/dd/yyyy";
		} else if (bYearFirst) {
			vecDateParseFormat.addElement("yy/MM/dd"); // must be before "MM/dd/yyyy" since "MM/dd/yyyy" will convert 5/4/65 -> 05/04/0065
			vecDateParseFormat.addElement("yyyy/MM/dd");
			dateOutputFormat = "yyyy/MM/dd";
		} else { // day first
			vecDateParseFormat.addElement("dd/MM/yy");
			vecDateParseFormat.addElement("dd/MM/yyyy");
			dateOutputFormat = "dd/MM/yyyy";
		}
		// SQL date formats
		vecDateParseFormat.addElement("yyyy-MM-dd");
	}

	/**
	 * Creates a new date initialized to today's date.
	 */
	public OADate() {
		super(new Date());
		clearTime();
	}

	/**
	 * Creates a new date from a {@link Date} instance.
	 *
	 * @param date the source date
	 */
	public OADate(Date date) {
		super(date);
		clearTime();
	}

	/**
	 * Creates a new date from a millisecond time value.
	 *
	 * @param time milliseconds since epoch
	 */
	public OADate(long time) {
		this(new Date(time));
	}

	/**
	 * Creates a new date from a {@link Time} instance.
	 *
	 * @param time the source time
	 */
	public OADate(Time time) {
		super(time);
		clearTime();
	}

	/**
	 * Creates a new date from a {@link LocalDate} instance.
	 *
	 * @param ld the source local date
	 */
	public OADate(LocalDate ld) {
		this(new Date(ld.getYear() - 1900, (ld.getMonth().getValue()) - 1, ld.getDayOfMonth()));
	}

	/**
	 * Creates a new date from a {@link Calendar} instance.
	 *
	 * @param c the source calendar
	 */
	public OADate(Calendar c) {
		super(c.get(c.YEAR), c.get(c.MONTH), c.get(c.DATE));
		this.timeZone = c.getTimeZone();
	}

	/**
	 * Creates a new date from an {@link OADateTime} instance.
	 *
	 * @param odt the source date-time
	 */
	public OADate(OADateTime odt) {
		super(odt.getYear(), odt.getMonth(), odt.getDay());
		this.timeZone = odt.getTimeZone();
	}

	/**
	 * Creates a new date from a string value.
	 *
	 * @param strDate the string representation of the date
	 */
	public OADate(String strDate) {
		this(OADate.valueOf2(strDate));
		clearTime();
	}

	/**
	 * Creates a new date from a string value using a specified format.
	 *
	 * @param strDate the string representation of the date
	 * @param format the format to use for parsing
	 */
	public OADate(String strDate, String format) {
		super(strDate, format);
		clearTime();
	}

	/*
	 * Create new date using year, month, day.
	 *
	 * @param year full year (not year minus 1900 like Date) param month 0-11, use Calendar.JUNE, etc. param date day of the month (1-31)
	 */
	/**
	 * Creates a new date using year, month, and day values.
	 *
	 * @param year the full year value
	 * @param month the month value (0–11)
	 * @param day the day of the month
	 */
	public OADate(int year, int month, int day) {
		super(year, month, day);
		clearTime();
	}

	/**
	 * Sets the default global output format used when converting dates to strings.
	 *
	 * @param fmt the format string to use
	 */
	public static void setGlobalOutputFormat(String fmt) {
		dateOutputFormat = fmt;
	}

	/**
	 * Returns the default global output format used when converting dates to strings.
	 *
	 * @return the global output format
	 */
	public static String getGlobalOutputFormat() {
		return dateOutputFormat;
	}

	/**
	 * Adds a global parse format used when converting strings to dates.
	 *
	 * @param fmt the format string to add
	 */
	public static void addGlobalParseFormat(String fmt) {
		vecDateParseFormat.addElement(fmt);
	}

	/**
	 * Removes a global parse format used when converting strings to dates.
	 *
	 * @param fmt the format string to remove
	 */
	public static void removeGlobalParseFormat(String fmt) {
		vecDateParseFormat.removeElement(fmt);
	}

	/**
	 * Removes all global parse formats used to convert strings to dates.
	 */
	public static void removeAllGlobalParseFormats() {
		vecDateParseFormat.removeAllElements();
	}

	/**
	 * Tests whether this date is between or equal to two other values.
	 *
	 * @param obj1 the lower bound value
	 * @param obj2 the upper bound value
	 * @return true if between or equal
	 */
	public boolean between(Object obj1, Object obj2) {
		int i = compareTo(obj1);
		if (i < 0) {
			return false;
		}
		i = compareTo(obj2);
		return (i <= 0);
	}

	/**
	 * Delegates to {@link #isBetweenOrEqual(Object, Object)}.
	 *
	 * @param obj1 the lower bound value
	 * @param obj2 the upper bound value
	 * @return true if between or equal
	 */
    public boolean betweenOrEqual(Object obj1, Object obj2) {
        return isBetweenOrEqual(obj1, obj2);
    }

    /**
     * Tests whether this date is greater than or equal to the first value
     * and less than or equal to the second value.
     *
     * @param obj1 the lower bound value
     * @param obj2 the upper bound value
     * @return true if between or equal
     */
    public boolean isBetweenOrEqual(Object obj1, Object obj2) {
        int i = compareTo(obj1);
        if (i < 0) {
            return false;
        }
        i = compareTo(obj2);
        return (i <= 0);
    }

    /**
     * Delegates to {@link #isBetweenNotEqual(Object, Object)}.
     *
     * @param obj1 the lower bound value
     * @param obj2 the upper bound value
     * @return true if strictly between
     */
    public boolean betweenNotEqual(Object obj1, Object obj2) {
        return isBetweenNotEqual(obj1, obj2);
    }
    
    /**
     * Tests whether this date is strictly greater than the first value
     * and strictly less than the second value.
     *
     * @param obj1 the lower bound value
     * @param obj2 the upper bound value
     * @return true if strictly between
     */
    public boolean isBetweenNotEqual(Object obj1, Object obj2) {
        int i = compareTo(obj1);
        if (i <= 0) {
            return false;
        }
        i = compareTo(obj2);
        return (i < 0);
    }
    
    /**
     * Converts this date to a string using the default format.
     *
     * @return the formatted date string
     */
	public String toString() {
		return toString(null);
	}

	/**
	 * Converts this date to a string using the specified format.
	 *
	 * @param f the format string to use
	 * @return the formatted date string
	 */
	public String toString(String f) {
		if (f == null) {
			f = (format == null) ? dateOutputFormat : format;
			if (f == null || f.length() == 0) {
				f = "yyyy-MMM-dd";
			}
		}
		return toStringMain(f);
	}

	/**
	 * Converts a string to an {@link OADate} using the specified format.
	 *
	 * @param date the string representation of the date
	 * @param fmt the format string to use
	 * @return the parsed OADate instance
	 */
	public static OADate dateValue(String date, String fmt) {
		return (OADate) valueOf(date, fmt);
	}

	/**
	 * Converts a string to an {@link OADate} using default parsing rules.
	 *
	 * @param date the string representation of the date
	 * @return the parsed OADate instance
	 */
	public static OADate dateValue(String date) {
		return (OADate) valueOf(date, null);
	}

	/**
	 * Converts a string to an {@link OADateTime} using the specified format
	 * and global parsing rules.
	 *
	 * @param date the string representation of the date
	 * @param fmt the format string to use
	 * @return the parsed OADateTime instance, or null if parsing fails
	 */
	public static OADateTime valueOf(String date, String fmt) {
		if (date == null) {
			return null;
		}
		Date d = valueOfMain(date, fmt, vecDateParseFormat, dateOutputFormat);
		if (d == null) {
			if (date.length() < 6 && OAString.isNumber(date)) {
				return OADate.valueOf(date + "/" + (new OADate()).getYear());
			}

			d = valueOfMain(fixDate(date), fmt, vecDateParseFormat, dateOutputFormat);
			if (d == null) {
				return null;
			}
		}
		return new OADate(d);
	}

	/**
	 * Converts a string to an {@link OADateTime} using default parsing rules.
	 *
	 * @param date the string representation of the date
	 * @return the parsed OADateTime instance, or null if parsing fails
	 */
	public static OADateTime valueOf(String date) {
		return OADate.valueOf(date, null);
	}

	/**
	 * Converts a string to an {@link OADateTime} and throws an exception
	 * if the date cannot be parsed.
	 *
	 * @param date the string representation of the date
	 * @return the parsed OADateTime instance
	 * @throws IllegalArgumentException if the date cannot be parsed
	 */
	public static OADateTime valueOf2(String date) {
		OADateTime dt = OADate.valueOf(date, null);
		if (dt == null) {
			throw new IllegalArgumentException("OADate cant create date from String \"" + date + "\"");
		}
		return dt;
	}

	/**
	 * Returns a {@link LocalDate} representing this date.
	 *
	 * @return the corresponding LocalDate value
	 */
	public LocalDate getLocalDate() {
		LocalDate ld = LocalDate.of(getYear(), getMonth() + 1, getDay());
		return ld;
	}

}

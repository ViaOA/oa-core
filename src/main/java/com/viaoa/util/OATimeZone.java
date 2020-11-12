package com.viaoa.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Helper for building timezone list, display and lookups.
 * 
 * @author vvia
 */
public class OATimeZone {
	private static ArrayList<TZ> alTZ;

	public static class TZ {
		public String id;
		public String gmtValue;
		public String shortName;
		public String longName;

		public String getDisplay() {
			return "(" + gmtValue + ") " + longName + " (" + shortName + ")";
		}
	}

	public static TZ getLocalOATimeZone() {
		TimeZone timeZone = TimeZone.getDefault();
		TZ tz = getOATimeZone(timeZone);
		return tz;
	}

	public static ArrayList<TZ> getOATimeZones() {
		if (alTZ != null) {
			return alTZ;
		}
		alTZ = new ArrayList<>();

		String[] tzs = TimeZone.getAvailableIDs();
		final ArrayList<TimeZone> al = new ArrayList<>();
		for (String s : tzs) {
			TimeZone tz = TimeZone.getTimeZone(s);
			al.add(tz);
		}

		Collections.sort(al, (o1, o2) -> {
			int x1 = o1.getRawOffset();
			int x2 = o2.getRawOffset();
			if (x1 == x2) {
				return 0;
			}
			if (x1 > x2) {
				return 1;
			}
			return -1;
		});

		for (TimeZone timeZone : al) {
			long hours = TimeUnit.MILLISECONDS.toHours(timeZone.getRawOffset());
			long minutes = TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset()) - TimeUnit.HOURS.toMinutes(hours);
			// avoid -4:-30 issue
			minutes = Math.abs(minutes);

			String gmtValue = "";
			if (minutes == 0) {
				if (hours >= 0) {
					gmtValue = String.format("GMT+%02d", hours);
				} else {
					gmtValue = String.format("GMT-%02d", Math.abs(hours));
				}
			} else {
				if (hours >= 0) {
					gmtValue = String.format("GMT+%02d:%02d", hours, minutes);
				} else {
					gmtValue = String.format("GMT-%02d:%02d", Math.abs(hours), minutes);
				}
			}

			String shortName = timeZone.getDisplayName(false, timeZone.SHORT, Locale.getDefault());

			String longName = timeZone.getDisplayName();

			TZ tz = new TZ();
			tz.id = timeZone.getID();
			tz.shortName = shortName;
			tz.longName = longName;
			tz.gmtValue = gmtValue;
			alTZ.add(tz);
		}
		return alTZ;
	}

	/**
	 * Find the java TimeZone.
	 * 
	 * @param value can be the tz.id, display name, short name, or long name.
	 */
	public static TimeZone getTimeZone(final String value) {
		if (OAString.isEmpty(value)) {
			return TimeZone.getDefault();
		}

		TimeZone timeZone = TimeZone.getTimeZone(value);
		if (timeZone != null) {
			return timeZone;
		}

		for (TZ tz : getOATimeZones()) {
			if (value.equalsIgnoreCase(tz.id) || value.equalsIgnoreCase(tz.gmtValue) || value.equalsIgnoreCase(tz.shortName)
					|| value.equalsIgnoreCase(tz.longName) || value.equalsIgnoreCase(tz.getDisplay())) {
				timeZone = TimeZone.getTimeZone(tz.id);
				if (timeZone != null) {
					return timeZone;
				}
			}
		}
		return null;
	}

	public static TZ getOATimeZone(TimeZone timeZone) {
		if (timeZone == null) {
			return null;
		}
		final String id = timeZone.getID();
		for (TZ tz : getOATimeZones()) {
			if (id.equalsIgnoreCase(tz.id)) {
				return tz;
			}
		}
		return null;
	}

	public static TZ getOATimeZone(String value) {
		if (OAString.isEmpty(value)) {
			value = TimeZone.getDefault().getID();
		}

		for (TZ tz : getOATimeZones()) {
			if (value.equalsIgnoreCase(tz.id) || value.equalsIgnoreCase(tz.gmtValue) || value.equalsIgnoreCase(tz.shortName)
					|| value.equalsIgnoreCase(tz.longName) || value.equalsIgnoreCase(tz.getDisplay())) {
				return tz;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		getOATimeZones();

		//System.out.println(tz.getDisplay());
	}
}

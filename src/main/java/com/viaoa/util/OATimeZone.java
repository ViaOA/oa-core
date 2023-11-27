package com.viaoa.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Helper for building timezone list, display and lookups.
 * 
 * @author vvia
 */
public class OATimeZone {
	private static volatile ArrayList<TZ> alTZ;
	private static String[] shortNames;
	private static TimeZone tzUTC;

	public static class TZ {
		public String id;
		public String utcValue;  // ex:  -2:00
		public String shortName;
		public String longName;
		public TimeZone timeZone;

		public String getDisplay() {
			return "(" + utcValue + ") " + id + " (" + longName + "/" + shortName + ")";
		}
	}


	public static TimeZone getTimeZoneUTC() {
		if (tzUTC == null) {
			tzUTC = TimeZone.getTimeZone("UTC");
		}
		return tzUTC;
	}
	
	
	public static TZ getLocalOATimeZone() {
		TimeZone timeZone = TimeZone.getDefault();
		TZ tz = getOATimeZone(timeZone);
		return tz;
	}

	public static TimeZone getLocalTimeZone() {
		TimeZone timeZone = TimeZone.getDefault();
		return timeZone;
	}
	
	public static String[] getShortNames() {
		if (shortNames != null) return shortNames;
		
		List<String> al = new ArrayList();
		Set<String> set = new HashSet();
		for (TZ tz : getOATimeZones()) {
			if (!set.contains(tz.shortName)) {
				set.add(tz.shortName);
				al.add(tz.shortName);
			}
		}
		al.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return OAStr.compare(o1, o2);
			}
		});
		String[] ss = new String[al.size()];
		al.toArray(ss);
		
		shortNames = ss;
		return shortNames;
	}
	
	private static final Object lockTimeZones = new Object();
	public static ArrayList<TZ> getOATimeZones() {
		if (alTZ == null) {
	        synchronized (lockTimeZones) {
	            if (alTZ == null) {
	                alTZ = _getOATimeZones();
	            }	            
	        }
		}
        return alTZ;
	}	
    protected static ArrayList<TZ> _getOATimeZones() {
        ArrayList<TZ> alTZ = new ArrayList<>();

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

			String utcValue = "";
			if (minutes == 0) {
				if (hours > 0) {
					utcValue = String.format("UTC+%02d", hours);
				} else {
					utcValue = String.format("UTC-%02d", Math.abs(hours));
				}
			} else {
				if (hours > 0) {
					utcValue = String.format("UTC+%02d:%02d", hours, minutes);
				} else {
					utcValue = String.format("UTC-%02d:%02d", Math.abs(hours), minutes);
				}
			}

			String shortName = timeZone.getDisplayName(timeZone.useDaylightTime(), timeZone.SHORT, Locale.getDefault());
			String longName = timeZone.getDisplayName();

			TZ tz = new TZ();
			tz.id = timeZone.getID();
			tz.shortName = shortName;
			tz.longName = longName;
			tz.utcValue = utcValue;
			tz.timeZone = timeZone;
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
			if (value.equalsIgnoreCase(tz.id) || value.equalsIgnoreCase(tz.utcValue) || value.equalsIgnoreCase(tz.shortName)
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
			if (value.equalsIgnoreCase(tz.id) || value.equalsIgnoreCase(tz.utcValue) || value.equalsIgnoreCase(tz.shortName)
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

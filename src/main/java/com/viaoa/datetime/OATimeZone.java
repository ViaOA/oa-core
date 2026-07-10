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
package com.viaoa.datetime;

import java.util.*;
import java.util.concurrent.*;

import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;

/**
 * Utility for working with Java {@link TimeZone} objects, including:
 * <ul>
 *   <li>Building and caching all available zones sorted by UTC offset</li>
 *   <li>Lookup by ID, abbreviation, or formatted display strings</li>
 *   <li>Conversion targets for {@link OADateTime}</li>
 *   <li>Efficient repeated lookups via internal caching</li>
 * </ul>
 *
 * <p><b>Thread-safety:</b><br>
 * Time zone lists are created once and published via safe publication. A
 * background update mechanism exists but currently only refreshes at startup.
 *
 * <p><b>Display formatting:</b><br>
 * Each {@code TZ} entry exposes:
 * <ul>
 *   <li>{@code id} – IANA timezone ID</li>
 *   <li>{@code utcValue} – formatted as {@code UTC±hh[:mm]}</li>
 *   <li>{@code shortName} / {@code longName}</li>
 * </ul>
 *
 * <p>Motivation:
 * <br>Java offers multiple representations for timezones and names; this class
 * provides a stable lookup and identity layer for OA applications.
 *
 * @see OADateTime#convertTo(TimeZone)
 */
public class OATimeZone {
	
	/**
	 * Cached list of all available time zones wrapped as {@link TZ} objects.
	 * This list is lazily initialized and refreshed based on a time threshold.
	 */
	private static volatile ArrayList<TZ> alTZ;
	
	/**
	 * Cached array of unique timezone short names derived from available time zones.
	 */
	private static String[] shortNames;
	
	/**
	 * Cached instance of the UTC {@link TimeZone}.
	 */
	private static TimeZone tzUTC;

	/**
	 * Time zone ID constant for Eastern Time (New York).
	 */
    public static final String TZ_Eastern = "America/New_York";
    
    /**
     * Time zone ID constant for New York (Eastern Time).
     */
    public static final String TZ_NewYork = "America/New_York"; // Eastern
    
    /**
     * Time zone ID constant for Central Time.
     */
    public static final String TZ_Central = "America/Chicago";  // Central
    
    /**
     * Time zone ID constant for Chicago (Central Time).
     */
    public static final String TZ_Chicago = "America/Chicago";  // Central
    
    /**
     * Time zone ID constant for Mountain Time.
     */
    public static final String TZ_Mountain = "America/Phoenix"; 
    
    /**
     * Time zone ID constant for Phoenix (Mountain Time).
     */
    public static final String TZ_Phoenix = "America/Phoenix"; // Mountain
    
    /**
     * Time zone ID constant for Pacific Time.
     */
    public static final String TZ_Pacific = "America/Los_Angeles";
    
    /**
     * Time zone ID constant for Los Angeles (Pacific Time).
     */
    public static final String TZ_LosAngeles = "America/Los_Angeles";  // Pacific
    
    /**
     * Time zone ID constant for Anchorage.
     */
    public static final String TZ_Anchorage = "America/Anchorage";
    
    /**
     * Time zone ID constant for London.
     */
    public static final String TZ_London = "Europe/London";
    
    /**
     * Time zone ID constant for Tokyo.
     */
    public static final String TZ_Tokyo = "Asia/Tokyo";
    
    /**
     * Time zone ID constant for Hong Kong.
     */
    public static final String TZ_HongKong = "Asia/Hong_Kong";

    /**
     * Time zone ID constant for GMT.
     */
    public static final String TZ_GMT = "GMT";
    
    /**
     * Time zone ID constant for Zulu time (UTC).
     */
    public static final String TZ_Zulu = "Zulu";  // UTC-00
    // public static final String TZ_ = "";
    
    /**
     * Time zone ID constant for UTC.
     */
    public static final String TZ_UTC = "UTC";
		
	
    /**
     * Container class that represents a time zone with associated display
     * and formatting information.
     */
	public static class TZ {
		/**
		 * IANA or Java timezone identifier.
		 */
		public String id;
		/**
		 * Formatted UTC offset display value.
		 */
		public String utcValue; 
		/**
		 * Short timezone display name.
		 */
		public String shortName;
		/**
		 * Long timezone display name.
		 */
		public String longName;
		/**
		 * Underlying Java timezone instance.
		 */
		public TimeZone timeZone;

		/**
		 * Returns a combined display string with UTC offset, id, and names.
		 *
		 * @return display text for this timezone
		 */
		public String getDisplay() {
			return "(" + utcValue + ") " + id + " (" + longName + "/" + shortName + ")";
		}
	}


	/**
	 * Returns the cached UTC {@link TimeZone} instance.
	 * If it has not yet been initialized, it will be created.
	 *
	 * @return the UTC time zone
	 */
	public static TimeZone getTimeZoneUTC() {
		if (tzUTC == null) {
			tzUTC = TimeZone.getTimeZone("UTC");
		}
		return tzUTC;
	}
	
	
	/**
	 * Returns the local system time zone wrapped as a {@link TZ} object.
	 *
	 * @return the local {@link TZ} instance
	 */
	public static TZ getLocalOATimeZone() {
		TimeZone timeZone = TimeZone.getDefault();
		TZ tz = getOATimeZone(timeZone);
		return tz;
	}

	/**
	 * Returns the local system {@link TimeZone}.
	 *
	 * @return the default system time zone
	 */
	public static TimeZone getLocalTimeZone() {
		TimeZone timeZone = TimeZone.getDefault();
		return timeZone;
	}
	
	/**
	 * Returns an array of unique timezone short names.
	 * <p>
	 * The result is cached after the first call.
	 *
	 * @return an array of short timezone names
	 */
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
	
	/**
	 * Lock object used to synchronize time zone list initialization and updates.
	 */
	private static final Object lockTimeZones = new Object();

	/**
	 * Timestamp, in milliseconds, indicating when the cached time zone list
	 * should be refreshed.
	 */
	private static long msNextUpdate = 0;
	
	/**
	 * Returns the cached list of available time zones wrapped as {@link TZ} objects.
	 * <p>
	 * The list is lazily initialized and refreshed based on an internal schedule.
	 *
	 * @return a list of {@link TZ} instances
	 */
	public static ArrayList<TZ> getOATimeZones() {
		if (alTZ == null || msNextUpdate < System.currentTimeMillis()) {
	        synchronized (lockTimeZones) {
                alTZ = _getOATimeZones();
                
                OADate d = new OADate();
                d = (OADate) d.plusDay();
                msNextUpdate = d.getTime();
	        }
		}
        return alTZ;
	}	

	/**
	 * Builds and returns a new list of available time zones wrapped as {@link TZ} objects.
	 * <p>
	 * Time zones are sorted by raw UTC offset.
	 *
	 * @return a newly constructed list of {@link TZ} instances
	 */
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
	 * Finds and returns a {@link TimeZone} matching the supplied value.
	 * <p>
	 * The value may be a timezone ID, UTC offset string, short name, long name,
	 * or formatted display value.
	 *
	 * @param value the timezone identifier or display value
	 * @return the matching {@link TimeZone}, or {@code null} if not found
	 */
	public static TimeZone getTimeZone(final String value) {
		if (OAString.isEmpty(value)) {
			return TimeZone.getDefault();
		}

		TimeZone timeZone = TimeZone.getTimeZone(value);
		if (timeZone != null) {
			if (!"GMT".equals(timeZone.getID()) || "GMT".equalsIgnoreCase(value)) {
			    return timeZone;
			}			
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

	/**
	 * Returns the {@link TZ} wrapper corresponding to the supplied {@link TimeZone}.
	 *
	 * @param timeZone the time zone to match
	 * @return the corresponding {@link TZ}, or {@code null} if not found
	 */
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

	/**
	 * Returns a {@link TZ} representing a UTC offset.
	 *
	 * @param value the hour offset from UTC
	 * @return the matching {@link TZ}, or {@code null} if not found
	 */
    public static TZ getUtcTimeZone(int value) {
        String s = "UTC" + (value > 0 ? "+" : "-") + String.format("%02d", Math.abs(value));
        for (TZ tz : getOATimeZones()) {
            if (s.equalsIgnoreCase(tz.utcValue)) {
                return tz;
            }
        }
        return null;
    }

    /**
     * Holds the last cached reference to the time zone list for cache validation.
     */
    private static ArrayList<TZ> alTZhold;

    /**
     * Cache mapping timezone IDs to {@link TZ} instances for fast lookup.
     */
    private static Map<String, TZ> hmTZbyId = new ConcurrentHashMap();
    
    /**
     * Returns a {@link TimeZone} matching the supplied timezone ID.
     * <p>
     * Results are cached for repeated lookups.
     *
     * @param id the timezone ID
     * @return the matching {@link TimeZone}, or {@code null} if not found
     */
    public static TimeZone getTimeZoneById(String id) {
        if (alTZhold != alTZ) {
            hmTZbyId.clear();
            alTZhold = alTZ;
        }

        if (OAString.isEmpty(id)) {
            id = TimeZone.getDefault().getID();
        }
        
        TZ tzx = hmTZbyId.get(id);
        if (tzx != null) return tzx.timeZone;
        
        for (TZ tz : getOATimeZones()) {
            if (id.equalsIgnoreCase(tz.id)) {
                hmTZbyId.put(id, tz);
                return tz.timeZone;
            }
        }
        return null;
    }
    
    /**
     * Finds and returns a {@link TZ} matching the supplied value.
     * <p>
     * The value may be a timezone ID, UTC offset string, short name, long name,
     * or formatted display value.
     *
     * @param value the timezone identifier or display value
     * @return the matching {@link TZ}, or {@code null} if not found
     */
	public static TZ getOATimeZone(String value) {
		if (OAString.isEmpty(value)) {
			value = TimeZone.getDefault().getID();
		}

		TZ tzFound = null;
		ArrayList<TZ> al = getOATimeZones(); 
		for (TZ tz : al) {
			if (value.equalsIgnoreCase(tz.id)) {
				return tz;
			}
			if (value.equalsIgnoreCase(tz.utcValue) || value.equalsIgnoreCase(tz.shortName) || value.equalsIgnoreCase(tz.longName) || value.equalsIgnoreCase(tz.getDisplay())) {
				if (tzFound == null) tzFound = tz;
			}
		}
		return tzFound;
	}
}

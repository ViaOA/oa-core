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
package com.viaoa.util;

import java.util.*;
import java.util.concurrent.*;

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
	private static volatile ArrayList<TZ> alTZ;
	private static String[] shortNames;
	private static TimeZone tzUTC;

    public static final String TZ_Eastern = "America/New_York";
    public static final String TZ_NewYork = "America/New_York"; // Eastern
    
    public static final String TZ_Central = "America/Chicago";  // Central
    public static final String TZ_Chicago = "America/Chicago";  // Central
    
    public static final String TZ_Mountain = "America/Phoenix"; 
    public static final String TZ_Phoenix = "America/Phoenix"; // Mountain
    
    public static final String TZ_Pacific = "America/Los_Angeles";
    public static final String TZ_LosAngeles = "America/Los_Angeles";  // Pacific
    
    public static final String TZ_Anchorage = "America/Anchorage";
    public static final String TZ_London = "Europe/London";
    public static final String TZ_Tokyo = "Asia/Tokyo";
    public static final String TZ_HongKong = "Asia/Hong_Kong";

    public static final String TZ_GMT = "GMT";
    public static final String TZ_Zulu = "Zulu";  // UTC-00
    // public static final String TZ_ = "";
    
    public static final String TZ_UTC = "UTC";
		
	
	public static class TZ {
		public String id;
		public String utcValue; 
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
	private static long msNextUpdate = 0;
	
	public static ArrayList<TZ> getOATimeZones() {
		if (alTZ == null || msNextUpdate < System.currentTimeMillis()) {
	        synchronized (lockTimeZones) {
	            if (alTZ == null) {
	                alTZ = _getOATimeZones();
	                
	                OADate d = new OADate();
	                d = (OADate) d.addDay();
	                msNextUpdate = d.getTime();
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
	 * 
	 * @param value number from UTC, 0 to +14, and -12 to 0
	 * @return
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

    private static ArrayList<TZ> alTZhold;
    private static Map<String, TZ> hmTZbyId = new ConcurrentHashMap();
    
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
        {
            OATimeZone.TZ tz = OATimeZone.getOATimeZone("America/Chicago");  
            OADateTime dtNow = (new OADateTime()).convertTo(tz);
            OADate dToday = new OADate( dtNow.convertTo(tz) );
            int i = 0;
            i++;
        }
        
	    int i = 0;
	    for (TZ tz : getOATimeZones()) {
	        System.out.println((++i) + ") " + tz.getDisplay());
	    }

        final OATimeZone.TZ tzz = OATimeZone.getOATimeZone("America/Chicago");  
        final OADateTime dtNowz = (new OADateTime()).convertTo(tzz);
		
		
		TZ tz1 = getOATimeZone("UTC-06");
		TZ tz2 = getOATimeZone("CST");
        TZ tz3 = getOATimeZone("CDT");
        
        
        OATimeZone.TZ tz = OATimeZone.getOATimeZone("America/Chicago");
        OADateTime dtNow = new OADateTime();

        OADateTime dtNowCST = dtNow.convertTo(tz);
        OADateTime dtx = dtNow.convertTo(tz1);
        dtx = dtNow.convertTo(tz2);
        dtx = dtNow.convertTo(tz3);
		
		
		int xx = 4;
		xx++;
		//System.out.println(tz.getDisplay());
	}
}

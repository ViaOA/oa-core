package com.viaoa.object;

import com.viaoa.hub.Hub;

public class OAObjectEnumDelegate {

	/**
	 * Get name/value pairs (enum) for a property.
	 */
	public static Hub<String> getNameValues(Class clazz, String propertyName) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getNameValues();
	}

	/**
	 * Get the display name for name/value pairs (enum) for a property.
	 */
	public static Hub<String> getDisplayNameValues(Class clazz, String propertyName) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getDisplayNameValues();
	}

}

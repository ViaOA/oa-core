package com.viaoa.util;


// create 20251103, moved methods from OAString

/**
 * 
 */
public class OAClassUtil {

	public static String getClassName(Class c) {
		if (c == null) {
			return null;
		}
		return c.getSimpleName();
	}

	public static String getPackageName(Class c) {
		if (c == null) {
			return null;
		}
		String s = c.getName();
		int x = s.lastIndexOf('.');
		if (x > 0) {
			s = s.substring(0, x);
		}
		return s;
	}
	
}

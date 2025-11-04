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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectKeyDelegate;

/**
 * Utility class that performs flexible comparisons between values of arbitrary types.
 * <p>
 * OACompare extends standard Java comparison semantics by applying OA-style coercion rules:
 * <ul>
 *   <li>Values of different types (e.g., String "123" vs. Double 123.0) are automatically coerced for comparison.</li>
 *   <li>Supports comparisons involving OAObject, OAObjectKey, arrays, and Hub collections.</li>
 *   <li>Boolean, numeric, and date/time types are normalized through OAConverter before comparing.</li>
 *   <li>Special placeholder objects (e.g., OANullObject, OAAnyValueObject) are supported for flexible matching.</li>
 * </ul>
 * <p>
 * Comparison results follow standard compareTo semantics:
 * <ul>
 *   <li>{@code < 0}: first value is less than second</li>
 *   <li>{@code = 0}: values are equal (or coerced equal)</li>
 *   <li>{@code > 0}: first value is greater than second</li>
 * </ul>
 *
 * Typical usage:
 * <pre>
 * OACompare.isEqual("123", 123);           // true
 * OACompare.isBetween(5.2, 5.0, 6.0);     // true
 * OACompare.isLike("John Smith", "Jo*");  // true
 * </pre>
 *
 * @author Vince Via
 */
public class OACompare {

	/**
     * Returns true if the given object equals or is contained within the supplied match value.
     * <p>
     * Equivalent to {@link #isIn(Object, Object)}.
     *
     * @param obj         the object to test
     * @param matchValue  the value, Hub, or array to search
     * @return true if equal or contained
     */	 
	public static boolean isEqualOrIn(Object obj, Object matchValue) {
		return isIn(obj, matchValue);
	}

	/**
     * Returns true if {@code obj} is contained in {@code matchValue}.
     * <p>
     * Works with Hubs, arrays, or single values. Uses {@link #isEqual(Object, Object)} for element comparison.
     *
     * @param obj         the object to test
     * @param matchValue  a Hub, array, or single object
     * @return true if {@code obj} matches or is contained
     */
	public static boolean isIn(Object obj, Object matchValue) {
		if (obj == null || matchValue == null) {
			return false;
		}
		if (matchValue instanceof Hub) {
			return ((Hub) matchValue).contains(obj);
		}
		if (matchValue.getClass().isArray()) {
			int x = Array.getLength(matchValue);
			for (int i = 0; i < x; i++) {
				Object objx = Array.get(matchValue, i);
				if (isEqual(obj, objx)) {
					return true;
				}
			}
		}
		return isEqual(obj, matchValue);
	}

    /**
     * Performs wildcard comparison between a value and a match pattern.
     * <p>
     * Wildcards: {@code *} or {@code %} can appear at the start, end, or both.
     * <ul>
     *   <li>{@code "Jo*"}  matches any string starting with "Jo"</li>
     *   <li>{@code "*Smith"} matches any string ending with "Smith"</li>
     *   <li>{@code "*ann*"}  matches any string containing "ann"</li>
     * </ul>
     *
     * @param value       the object or string to test
     * @param matchValue  the wildcard string pattern
     * @return true if matched (case-insensitive)
     */
	public static boolean isLike(Object value, Object matchValue) {
		if (value == matchValue) {
			return true;
		}
		if (compare(value, matchValue) == 0) {
			return true;
		}
		if (value == null || matchValue == null) {
			return false;
		}
		if (!(matchValue instanceof String)) {
			return false;
		}

		// convert to strings
		String sValue;
		if (!(value instanceof String)) {
			sValue = OAConverter.toString(value);
			if (sValue == null) {
				return false;
			}
		} else {
			sValue = (String) value;
		}
		sValue = sValue.toLowerCase();

		String sMatchValue = (String) matchValue;
		sMatchValue = sMatchValue.toLowerCase();

		String startMatch = null;
		String endMatch = null;

		final int matchLen = sMatchValue.length();
		if (matchLen == 0) {
			return false;
		}

		char ch = sMatchValue.charAt(0);
		boolean b1 = (ch == '*' || ch == '%');

		ch = sMatchValue.charAt(matchLen - 1);
		boolean b2 = (ch == '*' || ch == '%');

		boolean bUseContains = false;
		if (b1 && b2) {
			bUseContains = true;
			if (matchLen <= 2) {
				return true;
			} else {
				sMatchValue = sMatchValue.substring(1, matchLen - 1);
			}
		} else if (b1) {
			endMatch = sMatchValue.substring(1);
		} else if (b2) {
			startMatch = sMatchValue.substring(0, matchLen - 1);
		} else {
			int pos = sMatchValue.indexOf('*');
			if (pos < 0) {
				pos = sMatchValue.indexOf('%');
			}
			if (pos > 0) {
				startMatch = sMatchValue.substring(0, pos);
				endMatch = sMatchValue.substring(pos + 1);
			}
		}

		if (bUseContains) {
			return sValue.indexOf(sMatchValue) >= 0;
		} else if (startMatch == null && endMatch == null) {
			return sValue.equals(sMatchValue);
		} else if (startMatch != null && endMatch != null) {
			return sValue.startsWith(startMatch) && sValue.endsWith(endMatch);
		} else if (startMatch != null) {
			return sValue.startsWith(startMatch);
		}
		return sValue.endsWith(endMatch);
	}

	
    /**
     * Compares two objects for equality, ignoring case for strings.
     *
     * @param value        first value
     * @param matchValue   second value
     * @return true if equal (case-insensitive for strings)
     */
	public static boolean isEqualIgnoreCase(Object value, Object matchValue) {
		return isEqual(value, matchValue, true);
	}

    /**
     * Returns true if the two values are equal after applying coercion rules.
     * @param value first value
     * @param matchValue second value
     * @return true if equal
     */	
	public static boolean isEqual(Object value, Object matchValue) {
		int x = compare(value, matchValue);
		return x == 0;
	}

    /**
     * Returns true if the two values are not equal after applying coercion rules.
     */
	public static boolean isNotEqual(Object value, Object matchValue) {
	    return !isEqual(value, matchValue);
	}

	public static boolean isEqual(Object value, Object matchValue, boolean bIgnoreCase) {
		return isEqual(value, matchValue, bIgnoreCase, -1);
	}

    public static boolean isNotEqual(Object value, Object matchValue, boolean bIgnoreCase) {
        return !isEqual(value, matchValue, bIgnoreCase, -1);
    }
	
	public static boolean isEqual(Object value, Object matchValue, int decimalPlaces) {
		return isEqual(value, matchValue, false, decimalPlaces);
	}


	
    /**
     * Compares two values with optional case-insensitive and decimal-precision options.
     *
     * @param value first value
     * @param matchValue second value
     * @param bIgnoreCase true to ignore case for string comparisons
     * @param decimalPlaces number of decimal digits to round for numeric comparison;
     *                      negative values perform direct comparison with epsilon tolerance
     * @return true if equal under the given rules
     */
	public static boolean isNotEqual(Object value, Object matchValue, boolean bIgnoreCase, int decimalPlaces) {
        return !isEqual(value, matchValue, bIgnoreCase, decimalPlaces);
    }
	
	public static boolean isEqual(Object value, Object matchValue, boolean bIgnoreCase, int decimalPlaces) {
		if (bIgnoreCase) {
			if (value instanceof String) {
				value = ((String) value).toLowerCase();
			}
			if (matchValue instanceof String) {
				matchValue = ((String) matchValue).toLowerCase();
			}
		}
		int x = compare(value, matchValue, decimalPlaces);
		return x == 0;
	}

    /**
     * Tests whether a value is between (exclusive) two bounds.
     * Supports coercion for mixed-type inputs.
     */
	public static boolean isBetween(Object value, Object fromValue, Object toValue) {
		return isBetween(value, fromValue, toValue, -1);
	}


    /**
     * Tests whether a value is between (exclusive) two bounds, with optional decimal precision.
     */
	public static boolean isBetween(Object value, Object fromValue, Object toValue, int decimalPlaces) {
        if (value == null) {
            return false;
        }
        if (toValue == null) {
            return false;
        }
        int x = compare(value, fromValue, decimalPlaces);
        if (x <= 0) {
            return false;
        }

        x = compare(value, toValue, decimalPlaces);
        if (x >= 0) {
            return false;
        }
        return true;
    }
	
	
    /**
     * Returns true if value is equal to or between (inclusive) two bounds.
     */
	public static boolean isEqualOrBetween(Object value, Object fromValue, Object toValue) {
	    return isEqualOrBetween(value, fromValue, toValue, -1);
	}

    /**
     * Returns true if value is equal to or between (inclusive) two bounds with optional decimal precision.
     */	
	public static boolean isEqualOrBetween(Object value, Object fromValue, Object toValue, int decimalPlaces) {
		if (value == null) {
			return (fromValue == null);
		}
		if (toValue == null) {
			return false;
		}
		int x = compare(value, fromValue, decimalPlaces);
		if (x < 0) {
			return false;
		}

		x = compare(value, toValue, decimalPlaces);
		if (x > 0) {
			return false;
		}
		return true;
	}

	public static boolean isBetweenOrEqual(Object value, Object fromValue, Object toValue) {
		return isEqualOrBetween(value, fromValue, toValue, -1);
	}
    
	public static boolean isBetweenOrEqual(Object value, Object fromValue, Object toValue, int decimalPlaces) {
        return isEqualOrBetween(value, fromValue, toValue, decimalPlaces);
    }

    /**
     * Returns true if value is greater than the given value.
     */
	public static boolean isGreater(Object value, Object fromValue) {
		int x = compare(value, fromValue);
		return x > 0;
	}

    /**
     * Returns true if value is greater than the given value with decimal precision control.
     */
	public static boolean isGreater(Object value, Object fromValue, int decimalPlaces) {
        int x = compare(value, fromValue, decimalPlaces);
        return x > 0;
    }

    /**
     * Returns true if value is greater than or equal to the given value.
     */
	public static boolean isEqualOrGreater(Object value, Object fromValue) {
		int x = compare(value, fromValue);
		return x >= 0;
	}

    /**
     * Returns true if value is greater than or equal to the given value, with decimal precision.
     */
	public static boolean isEqualOrGreater(Object value, Object fromValue, int decimalPlaces) {
        int x = compare(value, fromValue, decimalPlaces);
        return x >= 0;
    }

	public static boolean isGreaterOrEqual(Object value, Object fromValue) {
		int x = compare(value, fromValue);
		return x >= 0;
	}

	public static boolean isGreaterOrEqual(Object value, Object fromValue, int decimalPlaces) {
        int x = compare(value, fromValue, decimalPlaces);
        return x >= 0;
    }

    /**
     * Returns true if value is less than the given value.
     */
	public static boolean isLess(Object value, Object fromValue) {
		int x = compare(value, fromValue);
		return x < 0;
	}
	
    /**
     * Returns true if value is less than the given value with decimal precision.
     */
    public static boolean isLess(Object value, Object fromValue, int decimalPlaces) {
        int x = compare(value, fromValue, decimalPlaces);
        return x < 0;
    }

	public static boolean isEqualOrLess(Object value, Object fromValue) {
		int x = compare(value, fromValue);
		return x <= 0;
	}
    public static boolean isEqualOrLess(Object value, Object fromValue, int decimalPlaces) {
        int x = compare(value, fromValue, decimalPlaces);
        return x <= 0;
    }

    /**
     * Returns true if value is less than or equal to the given value.
     */
	public static boolean isLessOrEqual(Object value, Object fromValue) {
		int x = compare(value, fromValue);
		return x <= 0;
	}
	
    /**
     * Returns true if value is less than or equal to the given value with decimal precision.
     */
    public static boolean isLessOrEqual(Object value, Object fromValue, int decimalPlaces) {
        int x = compare(value, fromValue, decimalPlaces);
        return x <= 0;
    }

    /**
     * Compares two integers using standard compare semantics.
     */
    public static int compare(int a, int b) {
		return Integer.compare(a, b);
	}

   
    /**
     * Compares two double values using fixed decimal precision or epsilon tolerance.
     * Handles NaN and Infinity safely.
     *
     * @param d1 first double
     * @param d2 second double
     * @param decimalPlaces rounding precision; negative values use relative epsilon comparison
     * @return -1 if d1 &lt; d2, 0 if approximately equal, 1 if d1 &gt; d2
     */
    public static int compare(double d1, double d2, int decimalPlaces) {
		if (Double.isNaN(d1) || Double.isNaN(d2)) return Double.compare(d1, d2);
		if (Double.isInfinite(d1) || Double.isInfinite(d2)) return Double.compare(d1, d2);
		
		if (decimalPlaces < 0) {
			final double dx = d1 - d2;
			final double eps = 1e-12 * Math.max(1.0, Math.max(Math.abs(d1), Math.abs(d2)));			
			if (Math.abs(dx) < eps) return 0;
			
			return (dx < 0) ? -1 : 1;
		}
		
		long l1 = getLongCompareValue(d1, decimalPlaces);
		long l2 = getLongCompareValue(d2, decimalPlaces);
		return Long.compare(l1, l2);
	}
    
    
	private static long getLongCompareValue(double d, int decimalPlaces) {
	    if (decimalPlaces < 0) decimalPlaces = 0;
	    else if (decimalPlaces > 9) decimalPlaces = 9; // prevent FP drift, and long overrun

	    if (Double.isNaN(d)) return 0;
	    if (Double.isInfinite(d)) return (d > 0) ? Long.MAX_VALUE : Long.MIN_VALUE;

	    boolean negative = d < 0;
	    if (negative) d = -d;

	    double scaled;
	    if (decimalPlaces == 0) {
	    	scaled = d;
	    }
	    else {
		    double scale = Math.pow(10, decimalPlaces);
		    scaled = d * scale;
	    }

	    long result = StrictMath.round(scaled);
	    return negative ? -result : result;
	}
	
	/**
	 * Compare two doubles, using fixed decimal places.
	 */
	public static boolean isEqual(double d1, double d2, int decimalPlaces) {
		return compare(d1, d2, decimalPlaces) == 0;
	}
	public static boolean isEqual(double d1, double d2) {
		return compare(d1, d2, -1) == 0;
	}
	
	
	
	
	
	
	
	public static int compare(Object value, Object matchValue) {
		return compare(value, matchValue, -1);
	}

    /**
     * Compares two objects with full OA coercion rules.
     * <p>
     * This is the primary comparison method used internally by OA libraries.
     * It supports all OA special types, numeric coercion, arrays, Hubs, and object graphs.
     *
     * @param value first object
     * @param matchValue second object
     * @param decimalPlaces number of decimal places for numeric rounding
     * @return -1, 0, or 1 based on comparison result
     */
	public static int compare(Object value, Object matchValue, final int decimalPlaces) {
		if (value == matchValue) {
			return 0;
		}

        if ((value instanceof Boolean) && (matchValue instanceof Boolean)) {
        	return Boolean.compare((Boolean)value, (Boolean)matchValue);
        }
		
        boolean b1 = value == null || (value instanceof String);
        boolean b2 = matchValue == null || (matchValue instanceof String);
		if (b1 && b2) {
		    if (value == matchValue) return 0;
		    if (value == null) return -1;
		    if (matchValue == null) return 1;
		    return ((String)value).compareTo((String)matchValue);
		}		

		// Fast path: both values are Numbers
		if (value instanceof Number && matchValue instanceof Number) {
		    Number n1 = (Number) value;
		    Number n2 = (Number) matchValue;

		    // Case 1: No rounding
		    if (decimalPlaces <= 0) {
		        // Optimize for identical numeric wrapper types
		        if (n1.getClass() == n2.getClass()) {
		            if (n1 instanceof Integer)   return Integer.compare(n1.intValue(),   n2.intValue());
		            if (n1 instanceof Long)      return Long.compare(n1.longValue(),     n2.longValue());
		            if (n1 instanceof Short)     return Short.compare(n1.shortValue(),   n2.shortValue());
		            if (n1 instanceof Byte)      return Byte.compare(n1.byteValue(),     n2.byteValue());
		            if (n1 instanceof Double)    return Double.compare(n1.doubleValue(), n2.doubleValue());
		            if (n1 instanceof Float)     return Float.compare(n1.floatValue(),   n2.floatValue());
		            if (n1 instanceof BigDecimal) return ((BigDecimal)n1).compareTo((BigDecimal)n2);
		            if (n1 instanceof BigInteger) return ((BigInteger)n1).compareTo((BigInteger)n2);
		        }
		    }

		    // Case 2: Rounding enabled 
		    double d1 = n1.doubleValue();
		    double d2 = n2.doubleValue();

		    return compare(d1, d2, decimalPlaces);
		}				
		
        if (value instanceof OASpecialCompareObject || matchValue instanceof OASpecialCompareObject) {
    		if (value instanceof OAAnyValueObject || matchValue instanceof OAAnyValueObject) {
    			return 0;
    		}
    		if (value instanceof OANotExist) {
    			if (matchValue == null || (matchValue instanceof OANotExist)) {
    				return 0;
    			}
    			return -1;
    		}
    		if (matchValue instanceof OANotExist) {
    			if (value == null) {
    				return 0;
    			}
    			return 1;
    		}
    		if (value instanceof OANullObject) {
    			if (matchValue == null || (matchValue instanceof OANullObject)) {
    				return 0;
    			}
    			return 1;
    		}
    		if (matchValue instanceof OANullObject) {
    			if (value == null) {
    				return 0;
    			}
    			return -1;
    		}
    		if (value instanceof OANotNullObject) {
    			if (matchValue != null) {
    				return 0;
    			}
    			return 1;
    		}
    		if (matchValue instanceof OANotNullObject) {
    			if (value != null) {
    				return 0;
    			}
    			return -1;
    		}
    
            if (value instanceof OAEmptyObject) {
                if (matchValue == null || "".equals(matchValue) || (matchValue instanceof OAEmptyObject)) {
                    return 0;
                }
                return 1;
            }
            if (matchValue instanceof OAEmptyObject) {
                if (value == null || "".equals(value)) {
                    return 0;
                }
                return -1;
            }
    
            if (value instanceof OANotEmptyObject) {
                if ((matchValue != null && !"".equals(matchValue)) || (matchValue instanceof OANotEmptyObject)) {
                    return 0;
                }
                return -1;
            }
            if (matchValue instanceof OANotEmptyObject) {
                if (value != null && !"".equals(value)) {
                    return 0;
                }
                return 1;
            }
            if (value instanceof OAUnknownObject) {
                if (matchValue instanceof OAUnknownObject) {
                    return 0;
                }
                return 1;
            }
            if (matchValue instanceof OAUnknownObject) {
                return -1;
            }
        }

        if (value instanceof OAObject || value instanceof OAObjectKey || matchValue instanceof OAObject || matchValue instanceof OAObjectKey) {
    	    OAObjectKey ka = OAObjectKeyDelegate.createObjectKey(value);
    	    OAObjectKey kb = OAObjectKeyDelegate.createObjectKey(matchValue);
    	  
    	    if (ka == kb) return 0;
    	    if (ka == null) return -1;
    	    if (kb == null) return 1;
    	    
    	    if (OAObjectKeyDelegate.isForSameOAObject(null, ka, kb)) return 0;
    	    return ka.compareTo(kb);
    	}        
        

		Class classValue = (value == null) ? null : value.getClass();
		Class classMatchValue = (matchValue == null) ? null : matchValue.getClass();

		// check if using array
		if (classValue != null && classValue.isArray()) {
			if (classMatchValue != null && classMatchValue.isArray()) {
				// all objects must be same
				int x1 = Array.getLength(value);
				int x2 = Array.getLength(matchValue);
				for (int i = 0; i < x1 && i < x2; i++) {
					Object v1 = Array.get(value, i);
					Object v2 = Array.get(matchValue, i);
					int x = compare(v1, v2);
					if (x != 0) {
						return x;
					}
				}
				if (x1 > x2) return 1;
				if (x1 < x2) return -1;
				return 0;
			}
			if (matchValue == null) {
				int x = Array.getLength(value);
				if (x == 0) return 0;
				return 1;
			}
			if (classMatchValue.equals(Boolean.class)) {
				boolean b = OAConv.toBoolean(matchValue);
				int x = Array.getLength(value);
				if (b) {
					if (x > 0) {
						return 0;
					}
					return -1;
				}
				if (x == 0) {
					return 0;
				}
				return 1;
			}
			// take value from [0]
			int x = Array.getLength(value);
			if (x > 1) {
				return 1;
			}
			if (x == 0) {
				value = null;
			} else {
				value = Array.get(value, 0);
			}
			x = compare(value, matchValue, decimalPlaces);
			return x;
		}
		if (classMatchValue != null && classMatchValue.isArray()) {
			if (value == null) {
				int x = Array.getLength(matchValue);
				if (x == 0) {
					return 0;
				}
				return -1;
			}
			if (Boolean.class.equals(classValue)) {
				boolean b = OAConv.toBoolean(value);
				int x = Array.getLength(matchValue);
				if (b) {
					if (x > 0) {
						return 0;
					}
					return 1;
				}
				if (x == 0) {
					return 0;
				}
				return -1;
			}
			// take value from [0]
			int x = Array.getLength(matchValue);
			if (x > 1) {
				return -1;
			}
			if (x == 0) {
				matchValue = null;
			} else {
				matchValue = Array.get(matchValue, 0);
			}
			x = compare(value, matchValue);
			return x;
		}

		// check if using hub
		if (value instanceof Hub) {
			if (matchValue instanceof Hub) {
				// all objects must be same
				Hub h1 = (Hub) value;
				Hub h2 = (Hub) matchValue;
				int x1 = h1.getSize();
				int x2 = h2.getSize();
				for (int i = 0; i < x1 && i < x2; i++) {
					int x = compare(h1.getAt(i), h2.getAt(i));
					if (x != 0) return x;
				}
				if (x1 < x2) {
					return -1;
				}
				if (x1 > x2) {
					return 1;
				}
				return 0;
			}
			// take value from hub.AO or pos=0 & size=1
			Hub h = (Hub) value;
			value = h.getAO();
			if (value == null) {
				if (h.getSize() > 1) {
					return 1;
				}
				value = h.getAt(0);
			}
			int x = compare(value, matchValue);
			return x;
		} else if (matchValue instanceof Hub) {
			Hub h = (Hub) matchValue;
			matchValue = h.getAO();
			if (matchValue == null) {
				if (h.getSize() > 1) {
					return -1;
				}
				matchValue = h.getAt(0);
			}
			int x = compare(value, matchValue);
			return x;
		}

		boolean bNeedToConvert = false;
		if (value == null) {
			value = OAConverter.convert(classMatchValue, value);
			classValue = (value == null) ? null : value.getClass();
		} else if (matchValue == null) {
			matchValue = OAConverter.convert(classValue, matchValue);
			classMatchValue = (matchValue == null) ? null : matchValue.getClass();
		} else if (classValue.equals(classMatchValue) || classValue.isAssignableFrom(classMatchValue)
				|| classMatchValue.isAssignableFrom(classValue)) {
			// noop
		} else if (classValue.equals(Boolean.class)) {
			matchValue = OAConv.toBoolean(matchValue);
			classMatchValue = (matchValue == null) ? null : matchValue.getClass();
		} else if (classMatchValue.equals(Boolean.class)) {
			value = OAConv.toBoolean(value);
			classValue = (value == null) ? null : value.getClass();
		} else if (OAReflect.isFloat(classValue) && (matchValue == null || OAReflect.isNumber(classMatchValue)
				|| (classMatchValue.equals(String.class) && OAString.isNumber((String) matchValue)))) {
			matchValue = OAConv.convert(classValue, matchValue);
			classMatchValue = (matchValue == null) ? null : matchValue.getClass();
		} else if (OAReflect.isFloat(classMatchValue) && (value == null || OAReflect.isNumber(classValue)
				|| (classValue.equals(String.class) && OAString.isNumber((String) value)))) {
			value = OAConv.convert(classMatchValue, value);
			classValue = (value == null) ? null : value.getClass();
		} else if (OAReflect.isInteger(classValue)) {
			if (OAReflect.isFloat(classMatchValue)) {
				value = OAConv.toDouble(value);
				classValue = classMatchValue = Double.class;
			} else if (OAReflect.isNumber(classMatchValue)
					|| (classMatchValue.equals(String.class) && OAString.isNumber((String) matchValue))) {
				value = OAConv.toDouble(value);
				matchValue = OAConv.toDouble(matchValue);
				classValue = classMatchValue = Double.class;
			} else {
				bNeedToConvert = true;
			}
		} else if (OAReflect.isInteger(classMatchValue)) {
			if (OAReflect.isFloat(classValue)) {
				matchValue = OAConv.toDouble(matchValue);
				classValue = classMatchValue = Double.class;
			} else if (OAReflect.isNumber(classValue) || (classValue.equals(String.class) && OAString.isNumber((String) value))) {
				value = OAConv.toDouble(value);
				matchValue = OAConv.toDouble(matchValue);
				classValue = classMatchValue = Double.class;
			} else {
				bNeedToConvert = true;
			}
		} else if (classValue.equals(OADate.class) && String.class.equals(classMatchValue)) {
			matchValue = new OADate((String) matchValue);
			classMatchValue = OADate.class;
		} else if (classMatchValue.equals(OADate.class) && String.class.equals(classValue)) {
			value = new OADate((String) value);
			classValue = (value == null) ? null : OADate.class;
		} else if (classValue.equals(OADateTime.class) && String.class.equals(classMatchValue)) {
			matchValue = new OADateTime((String) matchValue);
			classMatchValue = OADateTime.class;
		} else if (classMatchValue.equals(OADateTime.class) && String.class.equals(classValue)) {
			value = new OADateTime((String) value);
			classValue = OADateTime.class;
		} else if (classValue.equals(OATime.class) && String.class.equals(classMatchValue)) {
			matchValue = new OATime((String) matchValue);
			classMatchValue = OATime.class;
		} else if (classMatchValue.equals(OATime.class) && String.class.equals(classValue)) {
			value = new OATime((String) value);
			classValue = OATime.class;
		} else {
			bNeedToConvert = true;
		}

		if (bNeedToConvert) {
			try {
				value = OAConverter.convert(classMatchValue, value);
				if (value == null) {
					return -1;
				}
			} catch (Throwable e) {
				try {
					matchValue = OAConverter.convert(classValue, matchValue);
				} catch (Throwable ex) {
					return 1;
				}
			}
			classValue = (value == null) ? null : value.getClass();
			classMatchValue = (matchValue == null) ? null : matchValue.getClass();
		}

		if (decimalPlaces > 0) {
			if (OAReflect.isFloat(classValue)) {
				double d = OAConv.toDouble(value);
				value = OAMath.round(d, decimalPlaces);
			}
			if (OAReflect.isFloat(classMatchValue)) {
				double d = OAConv.toDouble(matchValue);
				matchValue = OAMath.round(d, decimalPlaces);
			}
			classValue = (value == null) ? null : value.getClass();
			classMatchValue = (matchValue == null) ? null : matchValue.getClass();
		}

		if (!(matchValue instanceof Comparable) || !(value instanceof Comparable)) {
			if (value == null) {
				if (value == matchValue) {
					return 0;
				}
				return -1;
			}
			if (matchValue == null) {
				return 1;
			}
			if (value.equals(matchValue)) {
				return 0;
			}
			return value.toString().compareTo(matchValue.toString());
		}
		int x = ((Comparable) value).compareTo(matchValue);
		return x;
	}


	
    /**
     * Returns true if the given object is not empty.
     * Equivalent to {@code !isEmpty(obj)}.
     */
	public static boolean isNotEmpty(Object obj) {
		return !isEmpty(obj);
	}

    /**
     * Returns true if the given object is not empty, with optional string trimming.
     */
	public static boolean isNotEmpty(Object obj, boolean bTrim) {
		return !isEmpty(obj, bTrim);
	}

	public static boolean isEmpty(Object obj) {
		return isEmpty(obj, false);
	}

    /**
     * Returns true if the given object is considered empty.
     * <p>
     * A value is empty if:
     * <ul>
     *   <li>It is null</li>
     *   <li>It is an array or collection with zero elements</li>
     *   <li>It is a primitive wrapper equal to 0, false, or '\u0000'</li>
     *   <li>It is a String that is blank or whitespace-only (if {@code bTrim} is true)</li>
     * </ul>
     *
     * @param obj   the object to check
     * @param bTrim if true, trims strings before testing for emptiness
     * @return true if empty
     */
	public static boolean isEmpty(Object obj, boolean bTrim) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof Hub) {
			return ((Hub) obj).getSize() == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection) obj).isEmpty();
		}
		if (obj.getClass().isArray()) {
			return (Array.getLength(obj) == 0);
		}

		Class c = obj.getClass();
		if (OAReflect.isPrimitiveClassWrapper(c)) {
			if (obj instanceof Number) {
				return (((Number) obj).doubleValue() == 0.0);
			}
			if (obj instanceof Boolean) {
				return (((Boolean) obj).booleanValue() == false);
			}
			if (obj instanceof Character) {
				return (((Character) obj).charValue() == 0);
			}
			return false;
		}

		return OAString.isEmpty(obj, bTrim);
	}

}

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
package com.viaoa.lang;

import java.lang.reflect.Array;
import java.util.Arrays;

/* 
  CODEX
  
  
   - Method: contains(String[] array, String searchValue, boolean bCaseSensitive)
  - Issue: bCaseSensitive is ignored; comparison always uses equalsIgnoreCase.
  - Why it is a problem: Callers requesting case-sensitive search get false positives, e.g. "ABC" matches "abc".
  - Classification: CODEX/FIXNOW

- Method: indexOf(String[] array, String searchValue, boolean bCaseSensitive)
  - Issue: bCaseSensitive is ignored; comparison always uses equalsIgnoreCase.
  - Why it is a problem: Callers can get an index for a value that should not match under case-sensitive semantics.
  - Classification: CODEX/FIXNOW

 - Method: removeValue(int[] array, int searchValue)
  - Issue: The match loop never assigns pos = i, so the method never removes anything.
  - Why it is a problem: removeValue(new int[] {1,2}, 1) returns the original array unchanged.
  - Classification: CODEX/FIXNOW

   - Class: OAArray
  - Method: removeValue(double[] array, double searchValue)
  - Issue: The match loop never assigns pos = i, so the method never removes anything.
  - Why it is a problem: removeValue(new double[] {1.0,2.0}, 1.0) returns the original array unchanged.
  - Classification: CODEX/FIXNOW

  - Method: insert(T[] array, T value, int atPos)
  - Issue: Negative atPos is not handled.
  - Why it is a problem: insert(array, value, -1) can throw from System.arraycopy or newArray[atPos].
  - Classification: CODEX/FIXNOW

 - Method: insert(Class c, Object[] array, Object value, int atPos)
  - Issue: Negative atPos is not handled.
  - Why it is a problem: Same boundary failure as typed insert; this is especially risky because this overload is
    used when callers explicitly manage component type.
  - Classification: CODEX/FIXNOW
  
 - Class: OAArray
  - Method: add(Class c, Object[] array, Object addValue), add(Class c, Object[] array, Object... addValues),
    removeAt(Class c, Object[] array, int pos)
  - Issue: When array is non-null, these methods often use Arrays.copyOf(array, ...), ignoring explicit component
    type c.
  - Why it is a problem: The explicit type contract is not preserved. Example: casting the result of
    removeAt(String.class, new Object[]{"a"}, 0) is safe, but removing the last element from a longer Object[]
    returns Object[], not String[].
  - Classification: CODEX/FIXNOW

 - Method: add(T[] array, T addValue), insert(T[] array, T value, int atPos)
  - Issue: When array is null, runtime component type is taken from addValue.getClass().
  - Why it is a problem: A variable typed as a supertype array can receive a subtype runtime array, causing later
    valid supertype additions to throw ArrayStoreException.
  - Classification: CODEX/DEFER

 - Method: add(T[] array, T... addValues)
  - Issue: Passing a null varargs array throws NullPointerException.
  - Why it is a problem: Other varargs overloads treat null add-values as no-op, so overload behavior is
    inconsistent.
  - Classification: CODEX/DEFER

 - Method: removeValue(Class c, Object[] array, Object searchValue)
  - Issue: Null searchValue is never removed.
  - Why it is a problem: contains and indexOf support finding null, but remove cannot remove null elements.
  - Classification: CODEX/CONTRACT


  - Method: reorderToMatch(Object[] obja, Object[] objb)
  - Issue: Null elements in obja cause NullPointerException.
  - Why it is a problem: Other OAArray search/equality helpers are null-aware; this method can fail on arrays
    containing null.
  - Classification: CODEX/FIXNOW
  - Suggested Java comment to add:


  - Class: OAArray
  - Method: reorderToMatch(Object[] obja, Object[] objb)
  - Issue: Duplicate equal elements can map to the same target position, leaving null holes and losing elements.
  - Why it is a problem: ["a","a"] reordered to match ["a","a"] can become ["a", null].
  - Classification: CODEX/FIXNOW



*/

/**
 * Utility methods for working with Java arrays including search, index lookup,
 * insert, remove, and type-aware resizing.
 *
 * <p>These helpers are null-safe and provide behavior similar to common
 * collection operations without requiring conversion to a List or other
 * collection class.</p>
 *
 * <p>Overloaded variants support primitive arrays as well as object arrays,
 * allowing efficient manipulation without boxing or resizing cost outside of
 * the specific operation.</p>
 *
 * <p>Typical use cases include small dynamic lists, internal framework
 * operations, and situations where a fixed array type is required such as
 * reflection, serialization, or network protocol buffers.</p>
 */
public class OAArray {

	/**
	 * Checks whether an object array contains the supplied value.
	 * <p>
	 * Uses reference comparison first ({@code ==}), then falls back to
	 * {@link Object#equals(Object)} when the array element is non-null.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find (may be {@code null})
	 * @return {@code true} if the value is found, otherwise {@code false}
	 */
	public static boolean contains(Object[] array, Object searchValue) {
		if (array == null || array.length == 0) {
			return false;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return true;
			}
			if (array[i] != null && array[i].equals(searchValue)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether an object array contains the supplied value using reference equality only.
	 * <p>
	 * This method performs only {@code ==} comparisons and does not call {@code equals()}.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find (may be {@code null})
	 * @return {@code true} if the same instance is found, otherwise {@code false}
	 */
	public static boolean containsExact(Object[] array, Object searchValue) {
		if (array == null || array.length == 0) {
			return false;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether an {@code int} array contains the supplied value.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find
	 * @return {@code true} if the value is found, otherwise {@code false}
	 */
	public static boolean contains(int[] array, int searchValue) {
		if (array == null || array.length == 0) {
			return false;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a {@code double} array contains the supplied value.
	 * <p>
	 * Uses direct {@code ==} comparison (no epsilon tolerance).
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find
	 * @return {@code true} if the value is found, otherwise {@code false}
	 */
	public static boolean contains(double[] array, double searchValue) {
		if (array == null || array.length == 0) {
			return false;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a {@code double} array contains the supplied value.
	 * <p>
	 * Uses direct {@code ==} comparison (no epsilon tolerance).
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find
	 * @return {@code true} if the value is found, otherwise {@code false}
	 */
	public static boolean contains(String[] array, String searchValue, boolean bCaseSensitive) {
		if (array == null || array.length == 0) {
			return false;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return true;
			}
			if (array[i] != null) {
				if (array[i].equalsIgnoreCase(searchValue)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Compares two object arrays for element-by-element equality.
	 * <p>
	 * Arrays are considered equal if they are the same reference, or if both are non-null,
	 * have the same length, and each corresponding element is equal by:
	 * <ul>
	 *   <li>reference equality ({@code ==}), or</li>
	 *   <li>{@link Object#equals(Object)} when both elements are non-null.</li>
	 * </ul>
	 *
	 * @param objs1 the first array (may be {@code null})
	 * @param objs2 the second array (may be {@code null})
	 * @return {@code true} if the arrays are equal, otherwise {@code false}
	 */
	public static boolean isEqual(Object[] objs1, Object[] objs2) {
		if (objs1 == objs2) {
			return true;
		}
		if (objs1 == null || objs2 == null) {
			return false;
		}
		int x = objs1.length;
		if (x != objs2.length) {
			return false;
		}
		for (int i = 0; i < x; i++) {
			if (objs1[i] == objs2[i]) {
				continue;
			}
			if (objs1[i] == null || objs2[i] == null) {
				return false;
			}
			if (!objs1[i].equals(objs2[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the index of the first occurrence of a value in an object array.
	 * <p>
	 * Uses reference comparison first ({@code ==}), then falls back to
	 * {@link Object#equals(Object)} when the array element is non-null.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find (may be {@code null})
	 * @return the index of the first matching element, or {@code -1} if not found
	 */
	public static int indexOf(Object[] array, Object searchValue) {
		if (array == null || array.length == 0) {
			return -1;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return i;
			}
			if (array[i] != null && array[i].equals(searchValue)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first occurrence of a value in an object array,
	 * starting at the supplied position.
	 * <p>
	 * Uses reference comparison first ({@code ==}), then falls back to
	 * {@link Object#equals(Object)} when the array element is non-null.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find (may be {@code null})
	 * @param startPos the starting index (0-based); must be within array bounds
	 * @return the index of the first matching element at or after {@code startPos}, or {@code -1} if not found
	 */
	public static int indexOf(Object[] array, Object searchValue, int startPos) {
		if (array == null || array.length == 0) {
			return -1;
		}
		if (startPos < 0 || startPos >= array.length) {
			return -1;
		}

		for (int i = startPos; i < array.length; i++) {
			if (array[i] == searchValue) {
				return i;
			}
			if (array[i] != null && array[i].equals(searchValue)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first occurrence of a value in an {@code int} array.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find
	 * @return the index of the first matching element, or {@code -1} if not found
	 */
	public static int indexOf(int[] array, int searchValue) {
		if (array == null || array.length == 0) {
			return -1;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first occurrence of a value in a {@code double} array.
	 * <p>
	 * Uses direct {@code ==} comparison (no epsilon tolerance).
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find
	 * @return the index of the first matching element, or {@code -1} if not found
	 */
	public static int indexOf(double[] array, double searchValue) {
		if (array == null || array.length == 0) {
			return -1;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first occurrence of a value in a {@code String} array.
	 * <p>
	 * This method first checks reference equality ({@code ==}), then checks string equality.
	 * Note: the current implementation uses {@link String#equalsIgnoreCase(String)} and
	 * does not apply {@code bCaseSensitive} when comparing.
	 *
	 * @param array the array to search (may be {@code null})
	 * @param searchValue the value to find (may be {@code null})
	 * @param bCaseSensitive whether comparisons should be case-sensitive (currently not enforced)
	 * @return the index of the first matching element, or {@code -1} if not found
	 */
	public static int indexOf(String[] array, String searchValue, boolean bCaseSensitive) {
		if (array == null || array.length == 0) {
			return -1;
		}
		for (int i = 0; i < array.length; i++) {
			if (array[i] == searchValue) {
				return i;
			}
			if (array[i] != null) {
				if (array[i].equalsIgnoreCase(searchValue)) {
					return i;
				}
			}
		}
		return -1;
	}

	//qqqqqqqqqq TEST
	/**
	 * Adds multiple values to the end of an object array, returning the resized array.
	 * <p>
	 * Values are appended in the order provided. The returned array may be the same
	 * reference if {@code addValues} is empty, otherwise a new array is returned.
	 *
	 * @param <T> the component type
	 * @param array the array to append to (may be {@code null})
	 * @param addValues the values to append (may be {@code null} or empty)
	 * @return a new array containing the original elements followed by {@code addValues}
	 */
	public static <T> T[] add(T[] array, T... addValues) {
		for (T t : addValues) {
			array = add(array, t);
		}
		return array;
	}

	/**
	 * Adds a single value to the end of an object array, returning the resized array.
	 * <p>
	 * If the input array is {@code null}, a new array of length 1 is created using
	 * the runtime type of {@code addValue}, or the component type of {@code array}
	 * when {@code addValue} is {@code null}.
	 *
	 * @param <T> the component type
	 * @param array the array to append to (may be {@code null})
	 * @param addValue the value to append (may be {@code null})
	 * @return a new array containing the original elements plus {@code addValue}, or {@code null} if type cannot be determined
	 */
	public static <T> T[] add(final T[] array, T addValue) {
		Class c;
		if (addValue != null) {
			c = addValue.getClass();
		} else if (array != null) {
			c = array.getClass().getComponentType();
		} else {
			return null;
		}

		int x = (array == null) ? 0 : array.length;

		T[] newArray;
		if (array == null) {
			newArray = (T[]) Array.newInstance(c, 1);
		} else {
			newArray = Arrays.copyOf(array, x + 1);
		}
		newArray[x] = addValue;
		return newArray;
	}

	/**
	 * Adds a single value to the end of an object array, using an explicit component type.
	 * <p>
	 * This is useful when the existing array is {@code null} and the component type
	 * must be specified for {@link java.lang.reflect.Array#newInstance(Class, int)}.
	 *
	 * @param c the array component type (required when {@code array} is {@code null})
	 * @param array the array to append to (may be {@code null})
	 * @param addValue the value to append (may be {@code null})
	 * @return a new array containing the original elements plus {@code addValue}
	 */
	public static Object[] add(Class c, Object[] array, Object addValue) {
		int x = (array == null) ? 0 : array.length;
		Object[] newArray;
		if (array == null) {
			newArray = (Object[]) Array.newInstance(c, 1);
		} else {
			newArray = Arrays.copyOf(array, x + 1);
		}
		newArray[x] = addValue;
		return newArray;
	}

	/**
	 * Adds multiple values to the end of an object array, using an explicit component type.
	 * <p>
	 * If {@code array} is {@code null}, a new array is created using {@code c}.
	 * If {@code addValues} is {@code null} or empty, the original {@code array} is returned.
	 *
	 * @param c the array component type (required when {@code array} is {@code null})
	 * @param array the array to append to (may be {@code null})
	 * @param addValues the values to append (may be {@code null} or empty)
	 * @return a new array containing the original elements followed by {@code addValues}
	 */
	public static Object[] add(Class c, Object[] array, Object... addValues) {
		if (addValues == null || addValues.length == 0) {
			return array;
		}
		int x = (array == null) ? 0 : array.length;
		int x2 = addValues.length;

		Object[] newArray;

		if (array == null) {
			newArray = (Object[]) Array.newInstance(c, x2);
		} else {
			newArray = Arrays.copyOf(array, x + x2);
		}
		for (int i = 0; i < x2; i++) {
			newArray[x + i] = addValues[i];
		}
		return newArray;
	}

	/**
	 * Adds a single {@code int} value to the end of an {@code int} array.
	 *
	 * @param array the array to append to (may be {@code null})
	 * @param searchValue the value to append
	 * @return a new array containing the original elements plus {@code searchValue}
	 */
	public static int[] add(int[] array, int searchValue) {
		int x = (array == null) ? 0 : array.length;

		int[] newArray;
		if (array == null) {
			newArray = new int[1];
		} else {
			newArray = Arrays.copyOf(array, x + 1);
		}
		newArray[x] = searchValue;
		return newArray;
	}

	/**
	 * Adds a single {@code boolean} value to the end of a {@code boolean} array.
	 *
	 * @param array the array to append to (may be {@code null})
	 * @param bAdd the value to append
	 * @return a new array containing the original elements plus {@code bAdd}
	 */
	public static boolean[] add(boolean[] array, boolean bAdd) {
		int x = (array == null) ? 0 : array.length;

		boolean[] newArray;
		if (array == null) {
			newArray = new boolean[1];
		} else {
			newArray = Arrays.copyOf(array, x + 1);
		}
		newArray[x] = bAdd;
		return newArray;
	}

	/**
	 * Adds a single {@code double} value to the end of a {@code double} array.
	 *
	 * @param array the array to append to (may be {@code null})
	 * @param searchValue the value to append
	 * @return a new array containing the original elements plus {@code searchValue}
	 */
	public static double[] add(double[] array, double searchValue) {
		int x = (array == null) ? 0 : array.length;

		double[] newArray;
		if (array == null) {
			newArray = new double[1];
		} else {
			newArray = Arrays.copyOf(array, x + 1);
		}
		newArray[x] = searchValue;
		return newArray;
	}

	/**
	 * Adds an array of {@code String} values to the end of a {@code String} array.
	 * <p>
	 * This is a convenience overload that allows duplicates and delegates to
	 * {@link #add(String[], String[], boolean)}.
	 *
	 * @param array the destination array to append to (may be {@code null})
	 * @param values the values to append (may be {@code null})
	 * @return the resized array containing the original elements followed by {@code values}
	 */
	public static String[] add(String[] array, String[] values) {
		return add(array, values, true);
	}

	/**
	 * Adds the supplied {@code String} values to the end of a {@code String} array.
	 * <p>
	 * If {@code values} is {@code null}, the original {@code array} is returned.
	 * Null values within {@code values} are ignored.
	 * <p>
	 * When {@code bAllowDups} is {@code false}, each non-null candidate value is
	 * compared against the current contents of {@code array} using
	 * {@link String#equals(Object)}; values already present are skipped. :contentReference[oaicite:1]{index=1}
	 * <p>
	 * Each accepted value is appended by delegating to {@link #add(String[], String)},
	 * which resizes the array as needed. :contentReference[oaicite:2]{index=2}
	 *
	 * @param array the destination array to append to (may be {@code null})
	 * @param values the values to append (may be {@code null}; null entries are skipped)
	 * @param bAllowDups {@code true} to allow duplicates, {@code false} to skip values already present
	 * @return the updated array containing the appended values
	 */
	public static String[] add(String[] array, String[] values, boolean bAllowDups) {
		if (values == null) {
			return array;
		}
		for (String s : values) {
			if (s == null) {
				continue;
			}
			if (!bAllowDups && array != null) {
				boolean bFound = false;
				for (String sx : array) {
					if (sx == null) {
						continue;
					}
					if (sx.equals(s)) {
						bFound = true;
					}
				}
				if (bFound) {
					continue;
				}
			}
			array = add(array, s);
		}
		return array;
	}

	/**
	 * Appends a single {@code String} value to the end of a {@code String} array.
	 * <p>
	 * If {@code array} is {@code null}, a new array of length 1 is created.
	 * Otherwise, the array is resized by one using {@link Arrays#copyOf(Object[], int)}.
	 *
	 * @param array the destination array to append to (may be {@code null})
	 * @param value the value to append (may be {@code null})
	 * @return a new array containing the original elements followed by {@code value}
	 */
	public static String[] add(String[] array, String value) {
		int x = (array == null) ? 0 : array.length;

		String[] newArray;
		if (array == null) {
			newArray = new String[1];
		} else {
			newArray = Arrays.copyOf(array, x + 1);
		}
		newArray[x] = value;
		return newArray;
	}

	/**
	 * Removes the first occurrence of {@code searchValue} from an object array.
	 * <p>
	 * This method searches for the first matching element using:
	 * <ol>
	 *   <li>reference equality ({@code ==}) for an exact match, then</li>
	 *   <li>{@link Object#equals(Object)} for a logical match.</li>
	 * </ol>
	 * Only the first match is removed; additional duplicates (if any) are not removed.
	 * <p>
	 * If {@code array} is {@code null} or empty, or if {@code searchValue} is
	 * {@code null}, the original array is returned unchanged.
	 * <p>
	 * The returned array is created using the supplied component type {@code c}
	 * to preserve the runtime array type via {@link Array#newInstance(Class, int)}.
	 *
	 * @param c the array component type used when creating the resized array
	 * @param array the source array (may be {@code null})
	 * @param searchValue the value to remove (must not be {@code null})
	 * @return a new array with the first matching value removed, or the original array if not found
	 */
	public static Object[] removeValue(Class c, Object[] array, Object searchValue) {
		if (array == null || array.length == 0) {
			return array;
		}
		if (searchValue == null) {
			return array;
		}

		int x = array.length;
		int pos = -1;
		for (int i = 0; pos < 0 && i < x; i++) {
			if (searchValue == array[i]) {
				pos = i;
				break; // exact match
			}
			if (searchValue.equals(array[i])) {
				pos = i;
			}
		}
		if (pos < 0) {
			return array;
		}
		return removeAt(c, array, pos);
	}

	//qqqqqqqqq TEST
	/**
	 * Removes the element at a specified position from a typed object array.
	 * <p>
	 * If {@code pos} is out of range, the original array is returned.
	 * The returned array preserves the component type of the original array.
	 * <p>
	 * This method handles common cases efficiently:
	 * <ul>
	 *   <li>length 0: returns the original array,</li>
	 *   <li>length 1: returns an empty array of the same component type,</li>
	 *   <li>removing last element: uses {@link Arrays#copyOf(Object[], int)},</li>
	 *   <li>otherwise: uses {@link System#arraycopy(Object, int, Object, int, int)}.</li>
	 * </ul>
	 *
	 * @param <T> the component type
	 * @param array the source array (may be {@code null})
	 * @param pos the position to remove (0-based)
	 * @return a new array with the element removed, or the original array if no removal occurs
	 */
	public static <T> T[] removeAt(final T[] array, final int pos) {
		if (array == null) {
			return null;
		}

		final int x = array.length;
		if (x == 0) {
			return array;
		}
		if (pos < 0 || pos >= x) {
			return array;
		}

		Class c = array.getClass().getComponentType();
		if (x == 1) {
			return (T[]) Array.newInstance(c, 0);
		}

		if (pos == x - 1) {
			// remove last element
			T[] newArray = (T[]) Arrays.copyOf(array, x - 1);
			return newArray;
		}

		T[] newArray = (T[]) Array.newInstance(c, x - 1);
		if (pos == 0) {
			System.arraycopy(array, 1, newArray, 0, x - 1);
		} else {
			System.arraycopy(array, 0, newArray, 0, pos);
			System.arraycopy(array, pos + 1, newArray, pos, (x - pos) - 1);
		}
		return newArray;
	}

	/**
	 * Removes the element at a specified position from an object array using an explicit component type.
	 * <p>
	 * This method is useful when the desired component type must be preserved even
	 * when the array is manipulated as an {@code Object[]}.
	 * <p>
	 * If {@code pos} is out of range, the original array is returned. If the array
	 * length is 1, an empty array is returned using {@link Array#newInstance(Class, int)}.
	 *
	 * @param c the array component type used when creating the resized array
	 * @param array the source array (may be {@code null})
	 * @param pos the position to remove (0-based)
	 * @return a new array with the element removed, or the original array if no removal occurs
	 */
	public static Object[] removeAt(final Class c, final Object[] array, final int pos) {
		if (array == null) {
			return null;
		}
		final int x = array.length;
		if (x == 0) {
			return array;
		}
		if (pos < 0 || pos >= x) {
			return array;
		}

		if (x == 1) {
			return (Object[]) Array.newInstance(c, 0);
		}

		if (pos == x - 1) {
			// remove last element
			Object[] newArray = (Object[]) Arrays.copyOf(array, x - 1);
			return newArray;
		}

		Object[] newArray = (Object[]) Array.newInstance(c, x - 1);
		if (pos == 0) {
			System.arraycopy(array, 1, newArray, 0, x - 1);
		} else {
			System.arraycopy(array, 0, newArray, 0, pos);
			System.arraycopy(array, pos + 1, newArray, pos, (x - pos) - 1);
		}
		return newArray;
	}

	//qqqqqqqqqq ?? Insert should add nulls to "pad" when inserting beyond the array size qqqqqqqq

	//qqqqqqqqqq TEST
	/**
	 * Inserts a value into a typed array at the specified position.
	 * <p>
	 * The component type is determined from {@code value} when non-null, otherwise
	 * from {@code array}'s component type. If neither is available, {@code null}
	 * is returned.
	 * <p>
	 * If {@code atPos} is greater than or equal to the array length, this method
	 * appends the value by delegating to {@link #add(Object[], Object)}.
	 *
	 * @param <T> the component type
	 * @param array the source array (may be {@code null})
	 * @param value the value to insert (may be {@code null})
	 * @param atPos the insertion position (0-based)
	 * @return a new array with {@code value} inserted, or {@code null} if the component type cannot be determined
	 */
	public static <T> T[] insert(final T[] array, T value, int atPos) {
		Class c;
		if (value != null) {
			c = value.getClass();
		} else if (array != null) {
			c = array.getClass().getComponentType();
		} else {
			return null;
		}

		int x = (array == null) ? 0 : array.length;

		if (atPos >= x) {
			return add(array, value);
		}

		T[] newArray = (T[]) Array.newInstance(c, x + 1);

		if (atPos == 0) {
			System.arraycopy(array, 0, newArray, 1, x);
		} else {
			System.arraycopy(array, 0, newArray, 0, atPos);
			System.arraycopy(array, atPos, newArray, atPos + 1, x - atPos);
		}
		newArray[atPos] = value;
		return newArray;
	}

	/**
	 * Inserts a value into an object array at the specified position using an explicit component type.
	 * <p>
	 * If {@code atPos} is greater than or equal to the array length, this method
	 * appends the value by delegating to {@link #add(Class, Object[], Object)}.
	 * Otherwise, a new array is created and elements are shifted to make room for
	 * the inserted value.
	 *
	 * @param c the array component type used when creating the resized array
	 * @param array the source array (may be {@code null})
	 * @param value the value to insert (may be {@code null})
	 * @param atPos the insertion position (0-based)
	 * @return a new array with {@code value} inserted
	 */
	public static Object[] insert(Class c, Object[] array, Object value, int atPos) {
		int x = (array == null) ? 0 : array.length;

		if (atPos >= x) {
			return add(c, array, value);
		}

		Object[] newArray = (Object[]) Array.newInstance(c, x + 1);

		if (atPos == 0) {
			System.arraycopy(array, 0, newArray, 1, x);
		} else {
			System.arraycopy(array, 0, newArray, 0, atPos);
			System.arraycopy(array, atPos, newArray, atPos + 1, x - atPos);
		}
		newArray[atPos] = value;
		return newArray;
	}

	/**
	 * Removes the first occurrence of a value from an {@code int} array.
	 * <p>
	 * If the array is {@code null} or empty, the original array is returned.
	 * Only the first matching element is removed.
	 *
	 * @param array the source array (may be {@code null})
	 * @param searchValue the value to remove
	 * @return a new array with the first matching value removed, or the original array if not found
	 */
	public static int[] removeValue(int[] array, int searchValue) {
		if (array == null || array.length == 0) {
			return array;
		}

		int x = array.length;
		int pos = -1;
		for (int i = 0; pos < 0 && i < x; i++) {
			if (searchValue == array[i]) {
				pos = i;
				break;
			}
		}
		if (pos < 0) {
			return array;
		}
		return removeAt(array, pos);
	}

	/**
	 * Removes the element at a specified position from an {@code int} array.
	 * <p>
	 * If {@code pos} is out of range, the original array is returned.
	 * This method handles common cases efficiently, including removal of the
	 * last element using {@link Arrays#copyOf(int[], int)}.
	 *
	 * @param array the source array (may be {@code null})
	 * @param pos the position to remove (0-based)
	 * @return a new array with the element removed, or the original array if no removal occurs
	 */
	public static int[] removeAt(int[] array, int pos) {
		if (array == null || array.length == 0) {
			return array;
		}
		if (pos < 0 || pos >= array.length) {
			return array;
		}

		int x = array.length;
		if (x == 1) {
			return new int[0];
		}

		if (pos == x - 1) {
			// remove last element
			int[] newArray = (int[]) Arrays.copyOf(array, x - 1);
			return newArray;
		}

		int[] newArray = new int[x - 1];
		if (pos == 0) {
			System.arraycopy(array, 1, newArray, 0, x - 1);
		} else {
			System.arraycopy(array, 0, newArray, 0, pos);
			System.arraycopy(array, pos + 1, newArray, pos, (x - pos) - 1);
		}
		return newArray;
	}

	/**
	 * Removes the first occurrence of a value from a {@code double} array.
	 * <p>
	 * If the array is {@code null} or empty, the original array is returned.
	 * Only the first matching element is removed using direct {@code ==} comparison.
	 *
	 * @param array the source array (may be {@code null})
	 * @param searchValue the value to remove
	 * @return a new array with the first matching value removed, or the original array if not found
	 */
	public static double[] removeValue(double[] array, double searchValue) {
		if (array == null || array.length == 0) {
			return array;
		}

		int x = array.length;
		int pos = -1;
		for (int i = 0; pos < 0 && i < x; i++) {
			if (searchValue == array[i]) {
				pos = i;
				break;
			}
		}
		if (pos < 0) {
			return array;
		}
		return removeAt(array, pos);
	}

	/**
	 * Removes the element at a specified position from a {@code double} array.
	 * <p>
	 * If {@code pos} is out of range, the original array is returned.
	 * This method handles common cases efficiently, including removal of the
	 * last element using {@link Arrays#copyOf(double[], int)}.
	 *
	 * @param array the source array (may be {@code null})
	 * @param pos the position to remove (0-based)
	 * @return a new array with the element removed, or the original array if no removal occurs
	 */
	public static double[] removeAt(double[] array, int pos) {
		if (array == null || array.length == 0) {
			return array;
		}
		if (pos < 0 || pos >= array.length) {
			return array;
		}

		int x = array.length;
		if (x == 1) {
			return new double[0];
		}

		if (pos == x - 1) {
			// remove last element
			double[] newArray = (double[]) Arrays.copyOf(array, x - 1);
			return newArray;
		}

		double[] newArray = new double[x - 1];
		if (pos == 0) {
			System.arraycopy(array, 1, newArray, 0, x - 1);
		} else {
			System.arraycopy(array, 0, newArray, 0, pos);
			System.arraycopy(array, pos + 1, newArray, pos, (x - pos) - 1);
		}
		return newArray;
	}

	/**
	 * Reorders the contents of one array to match the ordering of a second array.
	 * <p>
	 * If both arrays are non-null and have the same length, this method attempts to
	 * place each element from {@code obja} into the position where an equal element
	 * occurs in {@code objb}. Equality is determined using {@link Object#equals(Object)}.
	 * <p>
	 * If any element from {@code obja} cannot be matched to an element in {@code objb},
	 * no changes are applied to {@code obja}.
	 *
	 * @param obja the array to reorder (modified in place)
	 * @param objb the array whose ordering should be matched
	 */
	public static void reorderToMatch(Object[] obja, Object[] objb) {
		if (obja == null) {
			return;
		}
		int x = obja.length;
		if (objb == null || objb.length != x) {
			return;
		}

		Object[] objNew = new Object[x];
		for (int i = 0; i < x; i++) {
			boolean b = false;
			for (int j = 0; j < x; j++) {
				if (obja[i].equals(objb[j])) {
					b = true;
					objNew[j] = obja[i];
					break;
				}
			}
			if (!b) {
				return;
			}
		}
		for (int i = 0; i < x; i++) {
			obja[i] = objNew[i];
		}
	}
	
	/**
	 * Determines whether an object array contains any {@code null} elements.
	 *
	 * @param objs the array to check (may be {@code null})
	 * @return {@code true} if the array contains at least one {@code null} element
	 */
	public static boolean hasNull(Object[] objs) {
		if (objs == null) return false;
		for (int i=0; i<objs.length; i++) {
			if (objs[i] == null) return true;
		}
		return false;
	}
}

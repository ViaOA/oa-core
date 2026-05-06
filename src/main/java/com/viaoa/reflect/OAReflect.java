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
package com.viaoa.reflect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.viaoa.converter.OAConverter;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.graph.service.object.OAObjectReflectService;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;



/*qqqqqqqqqqqqqqqqq
CODEX

 - Method: getMethod(Class, String, int, Object[])
  - Issue: Argument matching requires exact runtime class equality.
  - Why it is a problem: Valid reflective calls can fail to resolve when the method parameter is primitive,
    wrapper-compatible, assignable, or an interface/superclass. Example: int parameter with Integer argument, or
    CharSequence parameter with String argument.
  - Classification: CODEX/FIXNOW

 - Method: getMethod(Class, String, int, Object[])
  - Issue: Overloaded methods with null arguments resolve to the first case-insensitive method returned by
    Class.getMethods().
  - Why it is a problem: getMethods() order is not a stable API contract, so overload selection can vary and choose
    an incompatible or semantically wrong method.
  - Classification: CODEX/CONTRACT
  
 - Method: getMethod(Class, String, Class) / isEqualEvenIfWrapper(Class, Class)
  - Issue: All Number subclasses are treated as parameter-compatible.
  - Why it is a problem: This can select setX(Integer) for a Long, BigDecimal, etc. Method.invoke() will still
    reject incompatible numeric wrapper types unless conversion happens separately.
  - Classification: CODEX/FIXNOW

 - Method: getMethods(Class, String, Class, boolean)
  - Issue: Boolean isX() getters are not resolved for property paths.
  - Why it is a problem: The lookup tries getX() and then X(), but not isX(). Boolean properties exposed only as
    isActive() fail property-path resolution even though OAPropertyPath supports this pattern.
  - Classification: CODEX/FIXNOW
  
  - Method: getMethods(Class, String, Class, boolean)
  - Issue: Empty path segments are treated as toString.
  - Why it is a problem: Malformed paths such as employee. or employee..name become valid method chains involving
    toString, which can produce wrong values or misleading later failures instead of rejecting the path.
  - Classification: CODEX/DEFER

 - Method: getMethods(Class, String, Class, boolean)
  - Issue: Hub traversal only detects return type exactly equal to Hub.class.
  - Why it is a problem: A property returning a subclass of Hub will not get the automatic getActiveObject() method
    insertion or object-class resolution.
  - Classification: CODEX/DEFER
  
 - Method: getPropertyValue(Object, Method)
  - Issue: Primitive OAObject getter is invoked before checking the primitive-null flag.
  - Why it is a problem: A primitive-null property still executes the getter and any side effects/lazy calculation
    before returning null. That can mutate state during what callers expect to be a null-preserving read.
  - Classification: CODEX/CONTRACT

 - Method: setPropertyValue(Object, Method, Object)
  - Issue: null assigned to primitive OAObject property calls setNull(...) but does not invoke the setter.
  - Why it is a problem: This can bypass setter-side validation, derived updates, property-change behavior, or
    custom logic unless OA explicitly defines primitive-null assignment as setter bypass.
  - Classification: CODEX/CONTRACT

  - Method: setPropertyValue(Object, Method, Object)
  - Issue: Primitive-null property name is derived by blindly removing the first three characters.
  - Why it is a problem: This assumes a setX method. Any one-arg primitive method not named as a setter will mark
    the wrong primitive-null property.
  - Classification: CODEX/DEFER

 - Method: getEmptyPrimitive(Class)
  - Issue: Boolean primitive default is true.
  - Why it is a problem: Java’s default primitive boolean value is false. This method is used by OAObject and
    remote multiplexer paths to synthesize primitive return/default values, so boolean failures/no-response paths
    can report true.
  - Classification: CODEX/FIXNOW

  - Method: getEmptyPrimitive(Class)
  - Issue: Documentation says wrapper classes are supported, but implementation only handles c.isPrimitive().
  - Why it is a problem: Boolean.class, Integer.class, etc. return null despite the method contract, which can leak
    null where callers expect a wrapper default.
  - Classification: CODEX/CONTRACT

  - Method: getClassPath(Class)
  - Issue: clazz.getResource(className) is dereferenced without a null check.
  - Why it is a problem: Valid Class inputs without a normal class resource, including some primitive/array/module
    cases, can throw NullPointerException.
  - Classification: CODEX/DEFER

 - Method: getOAObjectClasses(String)
  - Issue: Duplicate class names from multiple classpath roots are returned unchanged.
  - Why it is a problem: In layered classpaths or test/runtime overlays, the same package/class can appear more
    than once, causing duplicate logical OA classes in scanner results.
  - Classification: CODEX/DEFER







*/

/**
 * Central reflection utility for OA: resolves property paths, invokes methods,
 * and adapts between primitive field semantics and OAObjectʼs null abstraction.
 *
 * <p><b>Primary Responsibilities</b>
 * <ul>
 *   <li>Locate methods by name (case-insensitive) and argument types</li>
 *   <li>Resolve dotted property paths including Hub link traversal</li>
 *   <li>Null-handling: primitive fields may be stored as “null” in OAObject</li>
 *   <li>Convert parameter Strings using {@link OAConverter}</li>
 *   <li>Dynamic type inspection (numeric, integer, float, wrapper)</li>
 *   <li>Classpath scanning for class loading and diagnostics</li>
 * </ul>
 *
 * <p><b>Hub Navigation</b><br>
 * Property path lookup supports Hub properties by automatically calling
 * {@code getActiveObject()} when a Hub is encountered.
 *
 * <p><b>Thread-safety</b><br>
 * All functionality is stateless and thread-safe.
 *
 * @see com.viaoa.object.OAObject
 * @see OAConverter
 * @see OAObjectReflectDelegate
 */
public class OAReflect {

	private static Logger LOG = Logger.getLogger(OAReflect.class.getName());

	/**
	 * Lookup table that maps primitive types to their corresponding wrapper classes.
	 */
	static private Hashtable tblPrimitives;
	static {
		tblPrimitives = new Hashtable(10, 1.0F);
		tblPrimitives.put(java.lang.Boolean.TYPE, java.lang.Boolean.class);
		tblPrimitives.put(java.lang.Byte.TYPE, java.lang.Byte.class);
		tblPrimitives.put(java.lang.Character.TYPE, java.lang.Character.class);
		tblPrimitives.put(java.lang.Short.TYPE, java.lang.Short.class);
		tblPrimitives.put(java.lang.Integer.TYPE, java.lang.Integer.class);
		tblPrimitives.put(java.lang.Long.TYPE, java.lang.Long.class);
		tblPrimitives.put(java.lang.Float.TYPE, java.lang.Float.class);
		tblPrimitives.put(java.lang.Double.TYPE, java.lang.Double.class);
		tblPrimitives.put(java.lang.Void.TYPE, java.lang.Void.class);
	}

	/**
	 * Delegates to {@link #getMethod(Class,String,int)}.
	 */
	public static Method getMethod(Class clazz, String methodName) {
		return getMethod(clazz, methodName, -1);
	}

	/**
	 * Delegates to {@link #getMethod(Class,String,int,Object[])}.
	 */
	public static Method getMethod(Class clazz, String methodName, int paramCount) {
		return getMethod(clazz, methodName, paramCount, null);
	}

	/**
	 * Delegates to {@link #getMethod(Class,String,int,Object[])}.
	 */
	public static Method getMethod(Class clazz, String methodName, Object[] args) {
		int paramCount = args == null ? 0 : args.length;
		return getMethod(clazz, methodName, paramCount, args);
	}

	/**
	 * Finds a public method on the given class by name, parameter count, and optional argument types.
	 *
	 * @param clazz the class to search
	 * @param methodName case-insensitive name of the method
	 * @param paramCount expected number of parameters, or negative to ignore
	 * @param args optional argument values used to match parameter types
	 * @return the matching Method, or null if none is found
	 */
	public static Method getMethod(Class clazz, String methodName, int paramCount, Object[] args) {
		if (clazz == null || methodName == null || methodName.length() == 0) {
			return null;
		}

		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methodName.equalsIgnoreCase(methods[i].getName())) {
				if (paramCount >= 0) {
					Class[] cs = methods[i].getParameterTypes();
					int x = (cs == null) ? 0 : cs.length;
					if (paramCount != x) {
						continue;
					}

					if (args != null && args.length > 0) {
						boolean b = true;
						for (int j = 0; b && j < cs.length; j++) {
							if (args[j] == null) {
								continue;
							}
							b = args[j].getClass().equals(cs[j]);
						}
						if (!b) {
							continue;
						}
					}
				}
				return methods[i];
			}
		}
		return null;
	}

	/**
	 * Finds a public method with a single parameter, allowing wrapper and primitive equivalence.
	 *
	 * @param clazz the class to search
	 * @param methodName case-insensitive name of the method
	 * @param classParam the expected parameter type
	 * @return the matching Method, or null if none is found
	 */
	public static Method getMethod(Class clazz, String methodName, Class classParam) {
		if (clazz == null || methodName == null || methodName.length() == 0 || classParam == null) {
			return null;
		}

		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (!methodName.equalsIgnoreCase(methods[i].getName())) {
				continue;
			}
			Class[] cs = methods[i].getParameterTypes();
			if (cs == null || cs.length != 1) {
				continue;
			}

			if (isEqualEvenIfWrapper(cs[0], classParam)) {
				return methods[i];
			}
		}
		return null;
	}

	/**
	 * Delegates to {@link #getMethods(Class,String,boolean)}.
	 */
	public static Method[] getMethods(Class clazz, String propertyPath) {
		return getMethods(clazz, propertyPath, true);
	}

	public static Method[] getMethods(Class clazz, String propertyPath, boolean bThrowException) {
		return getMethods(clazz, propertyPath, null, bThrowException);
	}

	/**
	 * Get the methods for a property path.
	 *
	 * @param clazz           beginning Class object to start with.
	 * @param propertyPath    is dot "." separated list (case insensitive). <br>
	 *                        Example: getMethods(Order.class, "employee.department.region.name") will retrieve the following methods:
	 *                        Order.getEmployee(), Employee.getDepartment(), Department.getRegion(), Region.getName()
	 *                        <p>
	 *                        Note: if any of the propertyNames is a Hub, then it will use the Hub's activeObject when retrieving the
	 *                        property.
	 * @param bThrowException flag to know if an exception should be thrown if methods are not found.
	 * @return array of "get' methods that can be used to retrieve a value from an object of type clazz. If the a method can not be found
	 *         then null is returned.
	 * @see #getPropertyValue(Object,Method) throws OAException if methods can not be found and bThrowException is true. also can use newer
	 *      OAPropertyPath, which has more info, including the methods
	 * @param substituteClass class to use if a link property is of type OAObject.class
	 */
	public static Method[] getMethods(Class clazz, String propertyPath, final Class substituteClass, boolean bThrowException) {
		// ex:  (c,"emp.dept.manager.lastname")
		int pos, prev;
		if (propertyPath == null) {
			propertyPath = "";
		}

		List<Method> alMethod = new ArrayList();

		Class classLast = clazz;
		for (pos = prev = 0; pos >= 0; prev = pos + 1) {

			int posx = propertyPath.indexOf('(', prev);
			pos = propertyPath.indexOf('.', prev);

			if (posx >= 0 && posx < pos) {
				pos = propertyPath.indexOf(')', posx);
				pos = propertyPath.indexOf('.', pos);
			} else {
				pos = propertyPath.indexOf('.', prev);
			}

			String name;
			if (pos >= 0) {
				name = propertyPath.substring(prev, pos);
			} else {
				name = propertyPath.substring(prev);
			}

			/**
			 * 2004/09/09 Add support for "casting" a property in a PropertyPath. Example: "(Manager)Employee.Department"
			 */
			String castName = null;
			int p = name.indexOf('(');
			if (p >= 0) {
				int p2 = name.indexOf(')');
				if (p2 > 0) {
					castName = name.substring(p + 1, p2);
					if (p2 + 1 == name.length()) {
						name = "";
					} else {
						name = name.substring(p2 + 1).trim();
					}
				}
			}

			if (name.length() == 0) {
				name = "toString";
			} else {
				name = "get" + name;
			}

			// find method
			// 2007/02/16 make sure method does not have any params
			Method method = OAReflect.getMethod(clazz, name, 0);

			// was: Method method = OAReflect.getMethod(clazz, name);
			if (method == null) {
				method = OAReflect.getMethod(clazz, name.substring(3), 0);
				if (method == null) {
					// 20120807 if OAObject, which is the return value when using <generics>, ex: OALeftJoin
					if (!bThrowException || (clazz != null && clazz.equals(OAObject.class))) {
						return null;
					}
					//was: if (!bThrowException) return null;
					RuntimeException rex = new RuntimeException("Throwing exception, OAReflect.getMethods() cant find method. class="
							+ (clazz == null ? "null" : clazz.getName()) + " prop=" + name + " path=" + propertyPath);
					rex.printStackTrace();
					throw rex;
				}
			}
			alMethod.add(method);

			clazz = method.getReturnType();
			if (clazz != null && clazz.equals(Hub.class)) {
				// try to find the ObjectClass for Hub
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(classLast);
				Class c = og.objectsInternal().callObjectInfoGetHubPropertyClass(classLast, name.substring(3));
				if (c != null) {
					// this needs to then get the activeObject out of the Hub object
					method = OAReflect.getMethod(clazz, "getActiveObject", 0);
					alMethod.add(method);
					clazz = c;
				}
			} else {
				/**
				 * 2004/09/09 Add support for "casting" a property in a PropertyPath. Example: "(Manager)Employee.Department"
				 */
				if (castName != null) {
					String cn;
					if (castName.indexOf('.') >= 0) {
						cn = castName;
					} else {
						if (clazz != null) {
							String s = clazz.getName();
							p = s.lastIndexOf('.');
							if (p >= 0) {
								s = s.substring(0, p + 1);
							} else {
								s = "";
							}
							cn = s + castName;
						} else {
							cn = castName;
						}
					}
					try {
						clazz = Class.forName(cn);
					} catch (Exception e) {
						if (!bThrowException) {
							return null;
						}
						throw new RuntimeException(e);
					}
				}

				if (OAObject.class.equals(clazz) && substituteClass != null) {
					clazz = substituteClass;
				}
			}
			classLast = clazz;
		}
		Method[] ms = new Method[alMethod.size()];
		alMethod.toArray(ms);
		return ms;
	}

	/**
	 * Convert String to required object needed as a parameter to Method. This will call OAConverter to do the conversion.
	 *
	 * @param method is Method that value will be sent to.
	 * @param value  is String that needs to be converted so that it can be used with method.
	 * @return Object that is converted from String value.
	 * @see OAConverter
	 */
	public static Object convertParameterFromString(Method method, String value) {
		Class[] params = method.getParameterTypes();
		if (params.length != 1) {
			return null; // error
		}
		Class param = params[0];
		return convertParameterFromString(param, value);
	}

	/**
	 * Convert String to required object needed as a parameter to Method. This will call OAConverter to do the conversion.
	 *
	 * @param method is Method that value will be sent to.
	 * @param value  is String that needs to be converted so that it can be used with method.
	 * @param format is text format used for String.
	 * @return Object that is converted from String value.
	 * @see OAConverter
	 */
	public static Object convertParameterFromString(Method method, String value, String format) {
		Class[] params = method.getParameterTypes();
		if (params.length != 1) {
			return null; // error
		}
		Class param = params[0];
		return convertParameterFromString(param, value, format);
	}

	/**
	 * Convert a String value to a different value of Class clazz. This will call OAConverter to do the conversion.
	 *
	 * @param clazz Class to convert String value to.
	 * @param value is String to convert.
	 * @return Object that is converted from String value.
	 * @see OAConverter
	 */
	public static Object convertParameterFromString(Class clazz, String value) {
		return OAConverter.convert(clazz, value);
	}

	/**
	 * Convert a String value to a different value of Class clazz. This will call OAConverter to do the conversion. param clazz Class to
	 * convert String value to.
	 *
	 * @param value  is String to convert.
	 * @param format is text format used for String.
	 * @return Object that is converted from String value.
	 * @see OAConverter
	 */
	public static Object convertParameterFromString(Class c, String value, String format) {
		return OAConverter.convert(c, value, format);
	}

	/**
	 * Convert the returned object to a String value.
	 *
	 * @param object beginning object to use when calling methods.
	 * @param method array of methods to call. Will use object for the first method, then will follow using the returned object for each
	 *               method.
	 * @return If any method call returns a null, then null will be returned, else the string value of the last method call.
	 * @see OAReflect#getMethods
	 * @see OAReflect#getPropertyValue
	 */
	public static String getPropertyValueAsString(Object object, Method[] method) {
		return getPropertyValueAsString(object, method, null);
	}

	/**
	 * Convert the returned object, from an array of method calls, to a String value.
	 *
	 * @param object beginning object to use when calling methods.
	 * @param method array of methods to call. Will use object for the first method, then will follow using the returned object for each
	 *               method.
	 * @param format text format to use for conversion to string value.
	 * @return If any method call returns a null, then null will be returned, else the string value of the last method call.
	 * @see OAReflect#getMethods
	 * @see OAReflect#getPropertyValue
	 */
	public static String getPropertyValueAsString(Object object, Method method[], String format) {
		Method m = null;
		int x = method.length;
		if (x > 0) {
			for (int i = 0; object != null && i < x - 1; i++) {
				object = getPropertyValue(object, method[i]);
			}
			m = method[x - 1];
		}
		if (object == null) {
			return null;
		}
		return getPropertyValueAsString(object, m, format);
	}

	/**
	 * Convert the returned object of a method call to a String value.
	 *
	 * @param object beginning object to use when calling methods.
	 * @param method array of methods to call. Will use object for the first method, then will follow using the returned object for each
	 *               method. param format text format to use for conversion to string value.
	 * @return If any method call returns a null, then null will be returned, else the string value of the last method call.
	 * @see OAReflect#getMethods
	 * @see OAReflect#getPropertyValue
	 */
	public static String getPropertyValueAsString(Object object, Method method) {
		return getPropertyValueAsString(object, method, null);
	}

	/**
	 * @return if null then "", else formated string, using OAConverter.toString(value, format)
	 * @see getPropertyValueAsString(Object,Method[])
	 */
	public static String getPropertyValueAsString(Object object, Method method, String format) {
		return getPropertyValueAsString(object, method, format, "");
	}

	/**
	 * @return if null then nullValue, else formated string, using OAConverter.toString(value, format)
	 * @see getPropertyValueAsString(Object,Method[])
	 */
	public static String getPropertyValueAsString(Object object, Method method, String format, String nullValue) {
		object = getPropertyValue(object, method);
		if (object == null) {
			return nullValue;
		}
		return OAConverter.toString(object, format);
	}

	/**
	 * Run the following methods from a starting Object.
	 */
	public static Object executeMethod(Object object, Method method[]) {
		if (method == null || method.length == 0) {
			return null;
		}
		for (int i = 0; object != null && i < method.length; i++) {
			object = getPropertyValue(object, method[i]);
		}
		return object;
	}

	/**
	 * Run the following methods based on a property path from a starting Object.
	 */
	public static Object executeMethod(Object object, String path) {
		if (object == null || path == null || path.length() == 0) {
			return null;
		}
		Method[] method = getMethods(object.getClass(), path);
		if (method == null || method.length == 0) {
			throw new RuntimeException("OAReflect.executeMethod() cant find method " + path + " for class " + object.getClass());
		}
		return executeMethod(object, method);
	}

	/**
	 * This method will walk through the methods starting with the object supplied and then using the returned object. It will
	 *
	 * @param object beginning object to use
	 * @param        method[] methods of property path
	 * @see OAReflect#getMethods
	 * @see OAReflect#getPropertyValueAsString
	 */
	public static Object getPropertyValue(Object object, Method method[]) {
		if (method == null || method.length == 0) {
			return object;
		}
		for (int i = 0; object != null && i < method.length; i++) {
			object = getPropertyValue(object, method[i]);
		}
		return object;
	}

	/**
	 * Invokes up to a specified number of Methods from the given array on the supplied object.
	 *
	 * <p>The method starts with the provided object and sequentially invokes each Method
	 * in the array, stopping when either:
	 * <ul>
	 *   <li>The object becomes null</li>
	 *   <li>The end of the method array is reached</li>
	 *   <li>The specified maximum number of method invocations is reached</li>
	 * </ul>
	 *
	 * @param object the starting object used for the first method invocation
	 * @param method array of Methods to invoke in order
	 * @param amt maximum number of methods from the array to invoke
	 * @return the resulting object after invoking the specified methods, or null if any invocation returns null
	 */
	public static Object getPropertyValue(Object object, Method method[], int amt) {
		if (method == null || method.length == 0) {
			return object;
		}
		for (int i = 0; object != null && i < method.length && i < amt; i++) {
			object = getPropertyValue(object, method[i]);
		}
		return object;
	}

	/**
	 * Uses reflection to get returned value of a method. If object is an OAObject, then object.isNull(...) will be checked.
	 */
	public static Object getPropertyValue(Object object, Method method) {
		if (object == null) {
			return null;
		}
		if (method == null) {
			return object;
		}

		Object obj;
		try {
			obj = method.invoke(object, (Object[]) null);
		} catch (Exception e) {
			String msg = "Error calling Method " + method + ", using object=" + object;
			throw new RuntimeException(msg, e);
		}

		// 20141023 moved this to after calling invoke, in case get method changes the value.
		if (object instanceof OAObject) {
			Class c = method.getReturnType();
			if (c != null && c.isPrimitive()) {
				String s = method.getName();
				if (s.startsWith("get")) {
					s = s.substring(3);
				} else if (s.startsWith("is")) {
					s = s.substring(2);
				}
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph((OAObject) object);
				if (og.objectsInternal().callObjectReflectGetPrimitiveNull((OAObject) object, s)) {
					return null;
				}
			}
		}

		return obj;
	}

	/**
	 * Converts a String object to object type required by method and invokes method
	 *
	 * @param value String value to use with method
	 * @see OAReflect#convertParameterFromString
	 */
	public static void setPropertyValue(Object object, Method method, String value) {
		Object obj = convertParameterFromString(method, value);
		setPropertyValue(object, method, obj);
	}

	/**
	 * Converts a String object to object type required by method and invokes method
	 *
	 * @param value  String value to use with method
	 * @param format is format used for value.
	 * @see OAReflect#convertParameterFromString
	 */
	public static void setPropertyValue(Object object, Method method, String value, String format) {
		Object obj = convertParameterFromString(method, value, format);
		setPropertyValue(object, method, obj);
	}

	/**
	 * Invokes method for an Object using an object value.
	 */
	public static void setPropertyValue(Object object, Method method, Object newValue) {
		Object[] objs = new Object[1];
		objs[0] = newValue;
		try {
			if (newValue == null && object instanceof OAObject && method.getParameterTypes()[0].isPrimitive()) {
				String s = method.getName();
				if (s.length() > 3) {
					s = s.substring(3);
				}
				((OAObject) object).setNull(s);
			} else {
				method.invoke(object, objs);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Determines the Class associated with a Method based on its parameters or return type.
	 *
	 * <p>If the Method has no parameters, the return type is returned unless it is {@code void},
	 * in which case {@code null} is returned. If the Method has exactly one parameter, the
	 * parameter type is returned. For all other cases, {@code null} is returned.
	 *
	 * @param m the Method to inspect
	 * @return the associated Class, or null if it cannot be determined
	 */
	static public Class getClass(Method m) {
		if (m == null) {
			return null;
		}
		Class[] c = m.getParameterTypes();
		if (c.length == 0) {
			if (m.getReturnType() == void.class) {
				return null;
			}
			return m.getReturnType();
		}
		if (c.length == 1) {
			return c[0];
		}
		return null;
	}

	/**
	 * Determines if a class is a Number.
	 */
	static public boolean isNumber(Class clazz) {
		if (clazz == null) {
			return false;
		}
		clazz = getClassWrapper(clazz);
		return (Number.class.isAssignableFrom(clazz));
	}

	/** Determines if a class is a Integer. */
	static public boolean isInteger(Class clazz) {
		if (clazz == null) {
			return false;
		}
		clazz = getClassWrapper(clazz);
		return (clazz.equals(Long.class) || clazz.equals(Integer.class) || clazz.equals(Short.class)
				|| clazz.equals(Byte.class) || clazz.equals(BigInteger.class));
	}

	/** Determines if a class is a Float. */
	static public boolean isFloat(Class clazz) {
		if (clazz == null) {
			return false;
		}
		clazz = getClassWrapper(clazz);
		return (clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(BigDecimal.class));
	}

	/**  */
	static public Class getClassWrapper(Class clazz) {
		Class c = (Class) tblPrimitives.get(clazz);
		if (c != null) {
			return c;
		}
		return clazz;
	}

	/**
	 * Returns Class used to wrap a primitive Class.
	 */
	static public Class getPrimitiveClassWrapper(Class classPrimitive) {
		if (classPrimitive == null) {
			return null;
		}
		Class c = (Class) tblPrimitives.get(classPrimitive);
		if (c != null) {
			return c;
		}
		return classPrimitive;
	}

	/**
	 * Determines whether the given Class represents a primitive wrapper type.
	 *
	 * <p>This method explicitly checks for common Java wrapper classes corresponding
	 * to primitive types.
	 *
	 * @param clazz the Class to test
	 * @return true if the class is a primitive wrapper type, false otherwise
	 */
	static public boolean isPrimitiveClassWrapper(Class clazz) {
		if (clazz == null) {
			return false;
		}
		if (clazz.equals(Integer.class)) {
			return true;
		}
		if (clazz.equals(Long.class)) {
			return true;
		}
		if (clazz.equals(Boolean.class)) {
			return true;
		}
		if (clazz.equals(Double.class)) {
			return true;
		}
		if (clazz.equals(Byte.class)) {
			return true;
		}
		if (clazz.equals(Character.class)) {
			return true;
		}
		if (clazz.equals(Short.class)) {
			return true;
		}
		if (clazz.equals(Float.class)) {
			return true;
		}
		return false;
	}

	
	/**
	 * Determines if two classes should be considered equivalent for comparison or
	 * reflection purposes, including handling wrapper ↔ primitive mappings and
	 * compatible numeric types.
	 */
	public static boolean isEqualEvenIfWrapper(Class c1, Class c2) {
	    if (c1 == c2) return true;
	    if (c1 == null || c2 == null) return false;
	    if (c1.equals(c2)) return true;

	    if (c1.isPrimitive()) {
	        Class c3 = getPrimitiveClassWrapper(c1);
	        if (c3.equals(c2)) return true;
	    }
	    if (c2.isPrimitive()) {
	        Class c3 = getPrimitiveClassWrapper(c2);
	        if (c1.equals(c3)) return true;
	    }

	    // Numeric interoperability:
	    // Treat both as compatible if they are assignable from Number
	    if (Number.class.isAssignableFrom(getClassWrapper(c1)) &&
	        Number.class.isAssignableFrom(getClassWrapper(c2)))
	    {
	        return true;
	    }

	    return false;
	}
	
	
	
	
	/**
	 * Returns an Object that is of a wrapper class for a primitive type.
	 */
	static public Object getPrimitiveClassWrapperObject(Class clazz) {
		if (clazz == null) {
			return null;
		}
		if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
			return Integer.valueOf(0);
		}
		if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
			return Boolean.valueOf(false);
		}
		if (clazz.equals(long.class) || clazz.equals(Long.class)) {
			return Long.valueOf(0);
		}
		if (clazz.equals(double.class) || clazz.equals(Double.class)) {
			return Double.valueOf(0.0D);
		}
		if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
			return Byte.valueOf((byte) 0);
		}
		if (clazz.equals(char.class) || clazz.equals(Character.class)) {
			return Character.valueOf((char) 0);
		}
		if (clazz.equals(short.class) || clazz.equals(Short.class)) {
			return Short.valueOf((short) 0);
		}
		if (clazz.equals(float.class) || clazz.equals(Float.class)) {
			return Float.valueOf(0.0F);
		}
		return null;
	}

	/**
	 * Returns a default empty value for the specified primitive type.
	 *
	 * <p>If the supplied class represents a primitive type (or its wrapper),
	 * this method returns a corresponding default value:
	 * <ul>
	 *   <li>{@code boolean}/{@code Boolean} → {@code true}</li>
	 *   <li>{@code int}/{@code Integer} → {@code 0}</li>
	 *   <li>{@code long}/{@code Long} → {@code 0L}</li>
	 *   <li>{@code short}/{@code Short} → {@code (short) 0}</li>
	 *   <li>{@code double}/{@code Double} → {@code 0.0D}</li>
	 *   <li>{@code float}/{@code Float} → {@code 0.0F}</li>
	 * </ul>
	 *
	 * @param c the Class representing a primitive or wrapper type
	 * @return the default value for the primitive type, or null if the class is not a supported primitive
	 */
	public static Object getEmptyPrimitive(Class c) {
		Object response = null;
		if (c.isPrimitive()) {
			if (c.equals(boolean.class) || c.equals(Boolean.class)) {
				response = true;
			} else if (c.equals(int.class) || c.equals(Integer.class)) {
				response = 0;
			} else if (c.equals(long.class) || c.equals(Long.class)) {
				response = 0L;
			} else if (c.equals(short.class) || c.equals(Short.class)) {
				response = (short) 0;
			} else if (c.equals(double.class) || c.equals(Double.class)) {
				response = 0.0D;
			} else if (c.equals(float.class) || c.equals(Float.class)) {
				response = 0.0F;
			}
		}
		return response;
	}


	@Deprecated
	public static String[] getClasses(String packageName) throws ClassNotFoundException, IOException {
		return getOAObjectClasses(packageName);
	}
	
	/**
	 * Get name of all classes in a package. Example: String[] cs = getClasses("com.viaoa.scheduler.oa"); output: Item SalesOrder Customer
	 * etc ...
	 *
	 * @param packageName
	 * @return String array of class names, without the package prefix or '.class' suffix.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static String[] getOAObjectClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			throw new ClassNotFoundException("classloader not found");
		}
		final String origPath = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(origPath);
		List<String> list = new ArrayList<String>(50);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			String protocol = url.getProtocol();
			
			if ("file".equals(protocol)) {
				File file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
				String[] ss = file.list();
				if (ss != null) {
					for (String s : ss) {
						if (!s.endsWith(".class")) continue;
						if (s.indexOf('$') >= 0) continue;
						list.add(s.substring(0, s.length() - 6));
					}
				}
			} else if ("jar".equals(protocol)) {
				JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
				JarFile jar = jarConnection.getJarFile();

				String path = origPath + "/";

				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					String name = ((JarEntry) entries.nextElement()).getName();
					if (!name.startsWith(path)) {
						continue;
					}
					if (name.indexOf('/', path.length()) >= 0) {
						continue;
					}
					name = name.substring(path.length());
					if (!name.endsWith(".class")) {
						continue;
					}
					if (name.indexOf('$') >= 0) {
						continue;
					}
					name = name.substring(0, name.length() - 6);
					list.add(name);
				}
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Finds the class path used to be able to load a class.
	 *
	 * @return the full pathname to the .class file or the .jar file.
	 */
	public static String getClassPath(Class clazz) {
		if (clazz == null) {
			return null;
		}
		String className = clazz.getName();

		int pos = className.lastIndexOf('.');
		if (pos > 0) {
			className = className.substring(pos + 1);
		}

		className += ".class";

		URL url = clazz.getResource(className);
		String s = url.getPath();

		s = s.replaceAll("%20", " ");

		if (s.startsWith("file:/")) { // It's a jar-file.
			if (File.separatorChar == '/') { // UNIX|LINUX
				s = s.substring(5); // Leaves a / alone.
			} else {
				s = s.substring(6);
			}
		}

		int i = s.indexOf("!/");
		if (i > 0) { // jar files - need to get the jar file used
			s = s.substring(0, i);
		} else { // classpath to class file - need to only return the classpath directory
			className = clazz.getName();
			className = className.replace('.', '/') + ".class";
			pos = s.indexOf(className);
			if (pos > 0) {
				s = s.substring(0, pos);
			}
			if (s.indexOf(':') > 0 && s.charAt(0) == '/') {
				s = s.substring(1); // ex: "/c:/projects/java/viaoa/bin/"
			}
			if (s.endsWith("/")) {
				s = s.substring(0, s.length() - 1);
			}
		}
		s = OAString.convertFileName(s);
		return s;
	}

}

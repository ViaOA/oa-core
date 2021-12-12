/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectReflectDelegate;

/**
 * Used for Reflection services.
 *
 * @see OAObject#setProperty(String, Object) for storing "extra" properties and setting primitive properties to null.
 * @see OAObject#getProperty(String) for getting "extra" properties and getting primitive properties as null.
 * @author vincevia
 */
public class OAReflect {

	private static Logger LOG = Logger.getLogger(OAReflect.class.getName());

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

	public static Method getMethod(Class clazz, String methodName) {
		return getMethod(clazz, methodName, -1);
	}

	/**
	 * Finds a Method in a class.
	 *
	 * @param clazz      is the Class to use to find method name.
	 * @param methodName is case insensitive name of method. <br>
	 *                   Example: Employee.class, "getLastName")
	 * @return method in clazz that matches methodName.
	 */
	public static Method getMethod(Class clazz, String methodName, int paramCount) {
		return getMethod(clazz, methodName, paramCount, null);
	}

	public static Method getMethod(Class clazz, String methodName, Object[] args) {
		int paramCount = args == null ? 0 : args.length;
		return getMethod(clazz, methodName, paramCount, args);
	}

	/**
	 * 20121028
	 *
	 * @param args list of arguments used in method call
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
	 * Get the methods for a property path.
	 *
	 * @param clazz        beginning Class object to start with.
	 * @param propertyPath is dot "." separated list (case insensitive). <br>
	 *                     Example: getMethods(Order.class, "employee.department.region.name") will retrieve the following methods:
	 *                     Order.getEmployee(), Employee.getDepartment(), Department.getRegion(), Region.getName()
	 *                     <p>
	 *                     Note: if any of the propertyNames is a Hub, then it will use the Hub's activeObject when retrieving the property.
	 * @return array of "get' methods that can be used to retrieve a value from an object of type clazz.
	 * @see #getPropertyValue(Object,Method)
	 * @see #getMethods(Class,String,boolean)
	 */
	public static Method[] getMethods(Class clazz, String propertyPath) {
		return getMethods(clazz, propertyPath, true);
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
	 */
	public static Method[] getMethods(Class clazz, String propertyPath, boolean bThrowException) {
		// ex:  (c,"emp.dept.manager.lastname")
		int pos, prev;
		if (propertyPath == null) {
			propertyPath = "";
		}

		Vector vec = new Vector(5, 5);

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
			vec.addElement(method);

			clazz = method.getReturnType();
			if (clazz != null && clazz.equals(Hub.class)) {
				// try to find the ObjectClass for Hub
				Class c = OAObjectInfoDelegate.getHubPropertyClass(classLast, name.substring(3));
				if (c != null) {
					// this needs to then get the activeObject out of the Hub object
					method = OAReflect.getMethod(clazz, "getActiveObject", 0);
					vec.addElement(method);
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
			}
			classLast = clazz;
		}
		Method[] ms = new Method[vec.size()];
		vec.copyInto(ms);
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
			obj = method.invoke(object, null);
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
				if (OAObjectReflectDelegate.getPrimitiveNull((OAObject) object, s)) {
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
	 * If a "get" method, will find type of class to send method, else will find the type of return class.
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
				|| clazz.equals(Character.class) || clazz.equals(Byte.class) || clazz.equals(BigInteger.class));
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

	public static boolean isEqualEvenIfWrapper(Class c1, Class c2) {
		if (c1 == c2) {
			return true;
		}
		if (c1 == null || c2 == null) {
			return false;
		}
		if (c1.equals(c2)) {
			return true;
		}
		if (c1.isPrimitive()) {
			Class c3 = getPrimitiveClassWrapper(c1);
			return c2.equals(c3);
		}
		if (c2.isPrimitive()) {
			Class c3 = getPrimitiveClassWrapper(c2);
			return c1.equals(c3);
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
		if (clazz.equals(int.class)) {
			return new Integer(0);
		}
		if (clazz.equals(boolean.class)) {
			return new Boolean(false);
		}
		if (clazz.equals(long.class)) {
			return new Long(0);
		}
		if (clazz.equals(double.class)) {
			return new Double(0.0D);
		}
		if (clazz.equals(byte.class)) {
			return new Byte((byte) 0);
		}
		if (clazz.equals(char.class)) {
			return new Character((char) 0);
		}
		if (clazz.equals(short.class)) {
			return new Short((short) 0);
		}
		if (clazz.equals(float.class)) {
			return new Float(0.0F);
		}
		return null;
	}

	public static Object getEmptyPrimitive(Class c) {
		Object response = null;
		if (c.isPrimitive()) {
			if (c.equals(boolean.class)) {
				response = true;
			} else if (c.equals(int.class)) {
				response = 0;
			} else if (c.equals(long.class)) {
				response = 0L;
			} else if (c.equals(short.class)) {
				response = (short) 0;
			} else if (c.equals(double.class)) {
				response = 0.0D;
			} else if (c.equals(float.class)) {
				response = 0.0F;
			}
		}
		return response;
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
	public static String[] getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			throw new ClassNotFoundException("classloaded not found");
		}
		final String origPath = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(origPath);
		List<String> list = new ArrayList<String>(50);
		if (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			String fileName = url.getFile();
			String protocol = url.getProtocol();
			File file = new File(fileName);
			if (file.isDirectory()) {
				String[] ss = file.list();
				if (ss != null) {
					for (String s : ss) {
						int pos = s.indexOf(".class");
						if (pos < 0) {
							continue;
						}
						s = s.substring(0, pos);
						list.add(s);
					}
				}
			} else if (protocol == null || !protocol.equals("jar")) {
			} else {
				// get from jar
				// file:/C:/projects/java/ViaOA/lib/trwlRouter.jar!/com/cpex/trade/comm/socket

				url = new URL(protocol + ":" + fileName);
				JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
				JarFile jar = jarConnection.getJarFile();

				String path = origPath + "/";

				Enumeration entries = jar.entries();
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

	public static void main(String[] args) throws Exception {
		/*
		String[] cs = getClasses("com.cpex.trade.comm.socket");
		String[] cs = getClasses("com.viaoa.scheduler.oa");
		for (String c : cs) {
		    System.out.println(" "+c);
		}
		*/
		String s = getClassPath(OAReflect.class);
		System.out.println("==> " + s);
		s = getClassPath(String.class);
		System.out.println("==> " + s);
	}
}

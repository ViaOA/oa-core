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
package com.viaoa.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.hub.CustomHubFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubMerger;
import com.viaoa.object.OACalcInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.runtime.OARuntime;


/**
 * Represents a parsed and navigable property path used for traversing OAObject
 * graphs. A property path is a dotted sequence of property names that may span
 * bi-directional link properties, Hubs, and filtered collections. Examples:
 *
 * <pre>
 *     "customer.address.city"
 *     "orderItems:(OpenItems).product.name"
 *     "((Manager)teamMember).email"
 * </pre>
 *
 * <p><b>Core Responsibilities</b></p>
 * <ul>
 *   <li>Parse property path strings into addressable components</li>
 *   <li>Support Hub navigation using {@code getActiveObject()}</li>
 *   <li>Resolve link information and metadata from the OA model</li>
 *   <li>Optionally apply {@link OAFilter} rules (via :Filter syntax)</li>
 *   <li>Extract the final property value from an arbitrary root object</li>
 * </ul>
 *
 * <p><b>Casting and Filtering</b></p>
 * <ul>
 *   <li>Supports casting segments using parentheses:
 *       {@code (Manager).location.city}</li>
 *   <li>Supports optional {@link OAFilter} insertion applied during traversal</li>
 * </ul>
 *
 * <p><b>Behavior</b></p>
 * <ul>
 *   <li>Traversal stops early when encountering {@code null}</li>
 *   <li>Property path resolution is safe for both OAObject and Hub instances</li>
 *   <li>If a Hub is encountered, its active object is used for continuation</li>
 * </ul>
 *
 * <p><b>Thread-safety</b></p>
 * <ul>
 *   <li>Instances are not shared; parsing state is encapsulated and immutable after construction</li>
 * </ul>
 *
 * <p><b>Use Cases</b></p>
 * <ul>
 *   <li>UI binding and screen component expressions</li>
 *   <li>{@code OASelect} and filtering expressions</li>
 *   <li>Metadata inspection and reflection-based automation</li>
 *   <li>Graph navigation for controllers and remote layers</li>
 * </ul>
 *
 * <p>This class is central to the OA Object Graph pattern, enabling declarative
 * references to related objects without requiring explicit navigation code.</p>
 *
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.filter.OAFilter
 * @see OAPropertyPathInfo
 */
public class OAPropertyPath<F> {

	/**
	 * The starting class used to resolve the property path.
	 *
	 * <p>This defines the root type from which property resolution begins.</p>
	 */
	private Class<F> fromClass;
	
	/**
	 * The raw property path string as provided to the constructor.
	 *
	 * <p>This value is trimmed and parsed during setup.</p>
	 */
	private String propertyPath;
	
	/**
	 * Array of resolved getter or accessor methods corresponding to the property path.
	 *
	 * <p>Each method represents one segment of the property path.</p>
	 */
	private Method[] methods = new Method[0];
	
	/**
	 * Flag indicating whether the final resolved method requires a {@link Hub} parameter.
	 *
	 * <p>This is used when invoking static methods that accept a Hub as input.</p>
	 */
	private boolean bLastMethodHasHubParam; // true if method requires a Hub param

	/*
	 * property class. if casting is used, then this will have the casted class. note: if the method returns a Hub, then this will be the
	 * hub.objectClass
	 */
	/**
	 * Array of resolved classes for each segment in the property path.
	 *
	 * <p>If casting is used, the casted class is stored. If a method returns a Hub,
	 * the Hub object class is used.</p>
	 */
	private Class[] classes = new Class[0];

	// flag that is used when data is needed to find the real classes, since generics are being used.
	/**
	 * Flag indicating that runtime data is required to fully verify class resolution.
	 *
	 * <p>This is used when generics or {@link OAObject} placeholders prevent
	 * compile-time class determination.</p>
	 */
	private boolean bNeedsDataToVerify;
	
	/**
	 * Array of property names extracted from the property path.
	 *
	 * <p>Property names are stored without casting or filtering syntax.</p>
	 */
	private String[] properties = new String[0]; // convert properties, without casting
	
	/**
	 * Array of cast class names applied to individual property path segments.
	 *
	 * <p>Entries may be {@code null} when no cast is specified.</p>
	 */
	private String[] castNames = new String[0];
	
	/**
	 * Array of filter names associated with property path segments.
	 *
	 * <p>Filters are specified using ':' syntax in the property path.</p>
	 */
	private String[] filterNames = new String[0];
	
	/**
	 * Array of raw filter parameter strings.
	 *
	 * <p>Examples include "(a,b)" or "()".</p>
	 */
	private String[] filterParams = new String[0];
	
	/**
	 * Parsed filter parameter values corresponding to each filter.
	 *
	 * <p>Each entry aligns with a filter segment in the property path.</p>
	 */
	private Object[][] filterParamValues = new Object[0][];

	/**
	 * Array of resolved filter classes for each filter segment.
	 *
	 * <p>Entries may be {@code null} if a filter class could not be resolved.</p>
	 */
	private Class[] filterClasses = new Class[0];
	
	/**
	 * Array of constructors used to instantiate filter instances.
	 *
	 * <p>Each constructor corresponds to a resolved filter class.</p>
	 */
	private Constructor[] filterConstructors = new Constructor[0];

	/* NOTE: this also includes endLinkInfo */
	/**
	 * Array of link metadata resolved from the property path.
	 *
	 * <p>This includes link information for each link segment.</p>
	 */
	private OALinkInfo[] linkInfos = new OALinkInfo[0];
	
	/**
	 * Array of recursive link metadata corresponding to {@link #linkInfos}.
	 *
	 * <p>Entries are populated only for recursive link definitions.</p>
	 */
	private OALinkInfo[] recursiveLinkInfos = new OALinkInfo[0]; // for each linkInfos[]
	
	/**
	 * Cached reverse property path derived from link information.
	 *
	 * <p>This value is lazily created when requested.</p>
	 */
	private OAPropertyPath revPropertyPath;

	// the ending propery in the property path will be one of these
	/**
	 * Metadata for the final property resolved by the property path.
	 *
	 * <p>This is set when the path ends on a standard property.</p>
	 */
	private OAPropertyInfo endPropertyInfo;
	
	/**
	 * Metadata for the final calculated property resolved by the property path.
	 *
	 * <p>This is set when the path ends on a calculated property.</p>
	 */
	private OACalcInfo endCalcInfo;
	
	/**
	 * Metadata for the final link resolved by the property path.
	 *
	 * <p>This link is also included in {@link #linkInfos}.</p>
	 */
	private OALinkInfo endLinkInfo; // note: will also be in linkInfos

	/**
	 * Creates a property path using the supplied property path string.
	 *
	 * @param propertyPath the raw property path string to parse
	 */
	public OAPropertyPath(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	/**
	 * Creates a property path using a starting class and property path string.
	 *
	 * @param fromClass    the starting class used to resolve the property path
	 * @param propertyPath the raw property path string to parse
	 */
	public OAPropertyPath(Class<F> fromClass, String propertyPath) {
		this(fromClass, propertyPath, false);
	}

	/**
	 * Creates a property path using a starting class and property path string.
	 *
	 * <p>If setup fails and {@code bIgnoreError} is false, an exception is thrown.</p>
	 *
	 * @param fromClass     the starting class used to resolve the property path
	 * @param propertyPath  the raw property path string to parse
	 * @param bIgnoreError  if true, setup errors are ignored
	 */
	public OAPropertyPath(Class<F> fromClass, String propertyPath, boolean bIgnoreError) {
		this.propertyPath = propertyPath;
		this.fromClass = fromClass;

		try {
			setup(fromClass, bIgnoreError);
		} catch (Exception e) {
			try {
				// setup(fromClass);  // for debugging only
			} catch (Exception e2) {
				// TODO: handle exception
			}
			if (!bIgnoreError) {
				throw new IllegalArgumentException("cant setup, fromClass=" + fromClass + ", propertyPath=" + propertyPath, e);
			}
		}
	}

	/**
	 * Returns the raw property path string.
	 *
	 * @return the property path string
	 */
	public String getPropertyPath() {
		return this.propertyPath;
	}

	/**
	 * Returns the reverse property path using default behavior.
	 *
	 * @return the reverse property path, or {@code null} if not available
	 */
	public OAPropertyPath getReversePropertyPath() {
		return getReversePropertyPath(false);
	}

	/**
	 * Returns the reverse property path.
	 *
	 * <p>The reverse path is constructed from resolved link information.</p>
	 *
	 * @param bAllowPrivateLinks if true, private links are allowed
	 * @return the reverse property path, or {@code null} if not available
	 */
	public OAPropertyPath getReversePropertyPath(final boolean bAllowPrivateLinks) {
		if (revPropertyPath != null) {
			return revPropertyPath;
		}
		if (linkInfos == null || linkInfos.length == 0) {
			return null;
		}

		Class c = null;
		String pp = "";
		for (int i = 0; i < linkInfos.length; i++) {
			OALinkInfo li = linkInfos[i];
			final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(li.getToClass()).objects().getOAObjectInfoService();
			OALinkInfo liRev = srvcObjectInfo.getReverseLinkInfo(li);
			if (!bAllowPrivateLinks && liRev.getPrivateMethod()) {
				return null;
			}
			if (pp.length() > 0) {
				pp = "." + pp;
			}
			pp = liRev.getName() + pp;
		}
		c = linkInfos[linkInfos.length - 1].getToClass();

		revPropertyPath = new OAPropertyPath(c, pp, bAllowPrivateLinks);
		return revPropertyPath;
	}

	/**
	 * Returns a string representation of the property path containing only link segments.
	 *
	 * <p>This includes casting and filter syntax where applicable.</p>
	 *
	 * @return the link-only property path string, or {@code null} if no links exist
	 */
	public String getPropertyPathLinksOnly() {
		if (linkInfos == null || linkInfos.length == 0) {
			return null;
		}
		String s = "";
		for (int i = 0; i < linkInfos.length; i++) {
			OALinkInfo li = linkInfos[i];

			// 20190307 added cast name
			String s2 = "";
			if (castNames != null && i < castNames.length) {
				if (OAString.isNotEmpty(castNames[i])) {
					s2 = "(" + castNames[i] + ")";
				}
			}

			s = OAString.concat(s, s2 + li.getName(), ".");

			// 20190307 added filter name
			if (filterNames != null && i < filterNames.length) {
				if (OAString.isNotEmpty(filterNames[i])) {
					s += ":" + filterNames[i];

					if (filterParams != null && i < filterParams.length && OAString.isNotEmpty(filterParams[i])) {
						s += "(" + filterParams[i] + ")";
					}
				}
			}
		}
		return s;
	}

	/**
	 * Returns metadata for the final property in the property path.
	 *
	 * @return the ending {@link OAPropertyInfo}, or {@code null} if not applicable
	 */
	public OAPropertyInfo getEndPropertyInfo() {
		return endPropertyInfo;
	}

	/**
	 * Returns metadata for the final calculated property in the property path.
	 *
	 * @return the ending {@link OACalcInfo}, or {@code null} if not applicable
	 */
	public OACalcInfo getEndCalcInfo() {
		return endCalcInfo;
	}

	/**
	 * Returns metadata for the final link in the property path.
	 *
	 * @return the ending {@link OALinkInfo}, or {@code null} if not applicable
	 */
	public OALinkInfo getEndLinkInfo() {
		return endLinkInfo;
	}

	/**
	 * Returns the {@link OAProperty} annotation for the final resolved method.
	 *
	 * @return the {@link OAProperty} annotation, or {@code null} if not present
	 */
	public OAProperty getOAPropertyAnnotation() {
		if (methods == null || methods.length == 0) {
			return null;
		}
		return methods[methods.length - 1].getAnnotation(OAProperty.class);
	}

	/**
	 * Returns the {@link OACalculatedProperty} annotation for the final resolved method.
	 *
	 * @return the {@link OACalculatedProperty} annotation, or {@code null} if not present
	 */
	public OACalculatedProperty getOACalculatedPropertyAnnotation() {
		if (methods == null || methods.length == 0) {
			return null;
		}
		return methods[methods.length - 1].getAnnotation(OACalculatedProperty.class);
	}

	/**
	 * Returns the {@link OAOne} annotation for the final resolved method.
	 *
	 * @return the {@link OAOne} annotation, or {@code null} if not present
	 */
	public OAOne getOAOneAnnotation() {
		if (methods == null || methods.length == 0) {
			return null;
		}
		return methods[methods.length - 1].getAnnotation(OAOne.class);
	}

	/**
	 * Returns the list of property names parsed from the property path.
	 *
	 * @return array of property names
	 */
	public String[] getProperties() {
		return properties;
	}

	/**
	 * Returns the list of cast names associated with the property path.
	 *
	 * @return array of cast class names
	 */
	public String[] getCastNames() {
		return castNames;
	}

	// ex: Employees:recentBirthday()  => "recentBirthday()"
	/**
	 * Returns the list of filter names associated with the property path.
	 *
	 * @return array of filter names
	 */
	public String[] getFilterNames() {
		return filterNames;
	}

	// params used for filter, ex: "(a,b)", "()"
	/**
	 * Returns the list of raw filter parameter strings.
	 *
	 * @return array of filter parameter strings
	 */
	public String[] getFilterParams() {
		return filterParams;
	}

	/**
	 * Returns the parsed filter parameter values.
	 *
	 * @return two-dimensional array of filter parameter values
	 */
	public Object[][] getFilterParamValues() {
		return filterParamValues;
	}

	/**
	 * Returns the resolved methods corresponding to the property path.
	 *
	 * @return array of {@link Method} objects
	 */
	public Method[] getMethods() {
		return methods;
	}

	/**
	 * Returns the resolved classes for each segment of the property path.
	 *
	 * @return array of {@link Class} objects
	 */
	public Class[] getClasses() {
		return classes;
	}

	/**
	 * Returns the constructors used to create filter instances.
	 *
	 * @return array of filter constructors
	 */
	public Constructor[] getFilterConstructors() {
		return filterConstructors;
	}

	/**
	 * Returns the link metadata resolved from the property path.
	 *
	 * @return array of {@link OALinkInfo} objects
	 */
	public OALinkInfo[] getLinkInfos() {
		return linkInfos;
	}

	/**
	 * Indicates whether the property path contains any link segments.
	 *
	 * @return {@code true} if link information exists, otherwise {@code false}
	 */
	public boolean hasLinks() {
		return linkInfos != null && linkInfos.length > 0;
	}

	/**
	 * Returns recursive link metadata corresponding to resolved link segments.
	 *
	 * @return array of recursive {@link OALinkInfo} objects
	 */
	public OALinkInfo[] getRecursiveLinkInfos() {
		return recursiveLinkInfos;
	}

	/**
	 * Returns the value of the property path starting from the given object.
	 *
	 * @param fromObject the object to evaluate the property path against
	 * @return the resolved value, or {@code null} if evaluation fails or encounters {@code null}
	 */
	public Object getValue(F fromObject) {
		return getValue(null, fromObject);
	}

	/**
	 * Returns the value of the property path as a formatted string.
	 *
	 * @param fromObject the object to evaluate the property path against
	 * @return the formatted string value, or {@code null}
	 */
	public String getValueAsString(F fromObject) {
		return getValueAsString(null, fromObject);
	}

	/**
	 * Returns the value of the last link in the property path.
	 *
	 * @param fromObject the object to evaluate the property path against
	 * @return the value of the last link, or {@code null}
	 */
	public Object getLastLinkValue(F fromObject) {
		return getValue(null, fromObject, true);
	}

	/**
	 * Returns the name of the last property in the property path.
	 *
	 * @return the last property name, or {@code null} if none exist
	 */
	public String getLastPropertyName() {
		String[] ss = getProperties();
		if (ss == null || ss.length == 0) {
			return null;
		}
		return ss[ss.length - 1];
	}

	/**
	 * Returns the name of the first property in the property path.
	 *
	 * @return the first property name, or {@code null} if none exist
	 */
	public String getFirstPropertyName() {
		String[] ss = getProperties();
		if (ss == null || ss.length == 0) {
			return null;
		}
		return ss[0];
	}

	/*
	 * Returns the value of the propertyPath from a base object. Notes: if any of the property's is null, then null is returned. If any of
	 * the non-last properties is a Hub, then the AO will be used.
	 */
	/**
	 * Returns the value of the property path from a base object using a Hub context.
	 *
	 * <p>If any intermediate property is {@code null}, {@code null} is returned.
	 * If a non-final property resolves to a Hub, its active object is used.</p>
	 *
	 * @param hub        the Hub context used during evaluation
	 * @param fromObject the object to evaluate the property path against
	 * @return the resolved value, or {@code null}
	 */
	public Object getValue(Hub<F> hub, F fromObject) {
		return getValue(hub, fromObject, false);
	}

	/**
	 * Returns the value of the property path with optional link-only traversal.
	 *
	 * @param hub         the Hub context used during evaluation
	 * @param fromObject  the object to evaluate the property path against
	 * @param bLinksOnly  if true, traversal stops after link segments
	 * @return the resolved value, or {@code null}
	 */
	public Object getValue(Hub<F> hub, F fromObject, boolean bLinksOnly) {
		if (fromObject == null) {
			return null;
		}
		if (this.fromClass == null) {
			setup((Class<F>) fromObject.getClass());
		}
		if (methods == null || methods.length == 0) {
			return fromObject; // ex: could be pp="."
		}

		Object result = fromObject;
		for (int i = 0; i < methods.length; i++) {
			if (bLinksOnly && (linkInfos == null || i >= linkInfos.length)) {
				break;
			}
			if (bLastMethodHasHubParam && i + 1 == methods.length) {
				try {
					result = methods[i].invoke(result, hub);
				} catch (Exception e) {
					throw new RuntimeException("error invoking method=" + methods[i], e);
				}
			} else {
				try {
					result = methods[i].invoke(result);
				} catch (Exception e) {
					throw new RuntimeException("error invoking method=" + methods[i], e);
				}
			}

			if (result == null) {
				break;
			}
			if (i + 1 < methods.length && result instanceof Hub) {
				result = ((Hub) result).getAO();
				if (result == null) {
					break;
				}
			}
		}
		return result;
	}

	/*
	 * This will call getValue, and then call OAConv.toString using getFormat.
	 */
	/**
	 * Returns the value of the property path as a formatted string using a Hub context.
	 *
	 * @param hub        the Hub context used during evaluation
	 * @param fromObject the object to evaluate the property path against
	 * @return the formatted string value, or {@code null}
	 */
	public String getValueAsString(Hub<F> hub, F fromObject) {
		Object obj = getValue(hub, fromObject);
		String s = OAConv.toString(obj, getFormat());
		return s;
	}

	/**
	 * Returns the value of the property path as a formatted string using the supplied format.
	 *
	 * @param hub        the Hub context used during evaluation
	 * @param fromObject the object to evaluate the property path against
	 * @param format     the format string to apply
	 * @return the formatted string value, or {@code null}
	 */
	public String getValueAsString(Hub<F> hub, F fromObject, String format) {
		Object obj = getValue(hub, fromObject);
		String s = OAConv.toString(obj, format);
		return s;
	}

	/**
	 * Returns the starting class used to resolve the property path.
	 *
	 * @return the starting class
	 */
	public Class<F> getFromClass() {
		return fromClass;
	}

	/**
	 * Initializes the property path using the supplied Hub.
	 *
	 * @param hub the Hub used to determine the object class
	 */
	public void setup(Hub hub) {
		if (hub == null) {
			return;
		}
		setup(hub, hub.getObjectClass(), false);
	}

	/**
	 * Initializes the property path using the supplied class.
	 *
	 * @param clazz the class used to resolve the property path
	 */
	public void setup(Class clazz) {
		setup(clazz, false);
	}

	/**
	 * Initializes the property path using the supplied class.
	 *
	 * @param clazz              the class used to resolve the property path
	 * @param bIgnorePrivateLink if true, private links are ignored
	 */
	public void setup(Class clazz, boolean bIgnorePrivateLink) {
		String s = setup(null, clazz, bIgnorePrivateLink);
		if (s != null) {
			throw new RuntimeException(s);
		}
	}

	/**
	 * Indicates whether the property path contains any private link segments.
	 *
	 * @return {@code true} if a private link exists, otherwise {@code false}
	 */
	public boolean hasPrivateLink() {
		if (linkInfos == null) {
			return false;
		}
		for (OALinkInfo li : linkInfos) {
			if (li.getPrivateMethod()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Indicates whether the last resolved method requires a Hub parameter.
	 *
	 * @return {@code true} if the last method requires a Hub parameter
	 */
	public boolean getDoesLastMethodHasHubParam() {
		return bLastMethodHasHubParam;
	}

	/**
	 * Indicates whether runtime data is required to fully verify the property path.
	 *
	 * @return {@code true} if runtime data is needed
	 */
	public boolean getNeedsDataToVerify() {
		return bNeedsDataToVerify;
	}

	/**
	 * Initializes the property path using a Hub and starting class.
	 *
	 * @param hub               the Hub used during property resolution
	 * @param clazz             the starting class used to resolve the property path
	 * @param bIgnorePrivateLink if true, private links are ignored
	 * @return an error message if setup fails, otherwise {@code null}
	 */
	public String setup(final Hub hub, Class clazz, final boolean bIgnorePrivateLink) {
		return setup(hub, clazz, null, bIgnorePrivateLink);
	}

	/*
	 * @param substituteClass    class to use if a link property is of type OAObject.class
	 * @param bIgnorePrivateLink if true, then a link that does not have a get method will not throw an exception. Used by HubGroupBy
	 */
	/**
	 * Parses and resolves the property path using the supplied context.
	 *
	 * <p>This method resolves methods, classes, link metadata, and filter metadata
	 * based on the property path string.</p>
	 *
	 * @param hub                the Hub used during resolution
	 * @param clazz              the starting class
	 * @param substituteClass    class to substitute when a link property type is {@link OAObject}
	 * @param bIgnorePrivateLink if true, private links do not cause errors
	 * @return an error message if setup fails, otherwise {@code null}
	 */
	public String setup(final Hub hub, Class clazz, final Class substituteClass, final boolean bIgnorePrivateLink) {
		bNeedsDataToVerify = false;
		if (clazz == null) {
			bNeedsDataToVerify = true;
			return "Hub.objectClass not set";
		}
		this.fromClass = clazz;
		String propertyPath = this.propertyPath;
		if (propertyPath == null) {
			propertyPath = "";
		} else {
			propertyPath = propertyPath.trim();
		}

		// 20140118 if leading with "[ClassName].", then it is the fromClass
		int pos = propertyPath.indexOf("[");
		if (pos >= 0) {
			int pos2 = propertyPath.indexOf("].");
			if (pos2 > 0) {
				String fromClassName = propertyPath.substring(pos + 1, pos2);
				propertyPath = propertyPath.substring(pos2 + 2);

				if (fromClassName.indexOf('.') >= 0) {
					Class c;
					try {
						c = Class.forName(fromClassName);
					} catch (Exception e) {
						throw new RuntimeException("error getting class=" + fromClassName, e);
					}
					this.fromClass = c;
				} else {
					String packageName = fromClass.getName();
					pos = packageName.lastIndexOf('.');
					if (pos > 0) {
						packageName = packageName.substring(0, pos + 1);
					} else {
						packageName = "";
					}
					Class c;
					try {
						c = Class.forName(packageName + fromClassName);
					} catch (Exception e) {
						throw new RuntimeException("error getting class=" + packageName + fromClassName, e);
					}
					this.fromClass = c;
				}

			}
		}
		clazz = this.fromClass;

		String propertyPathClean = propertyPath;
		// a String that uses quotes "" could have special chars ',:()' inside of "" it
		//qqq todo:  this wont protect against \" - need to create a tokenizer
		for (int i = 0;; i++) {
			int p = propertyPathClean.indexOf('\"');
			if (p < 0) {
				break;
			}
			int p2 = propertyPathClean.indexOf('\"', p + 1);
			if (p2 < 0) {
				break;
			}
			int x = (p2 - p) - 1;
			String s = OAString.getRandomString(x, x, false, true, false);
			propertyPathClean = propertyPath.substring(0, p) + "\"" + s + "\"" + propertyPath.substring(p2 + 1);
		}

		Class classLast = clazz;
		int posDot, prevPosDot;
		posDot = prevPosDot = 0;

		if (OAString.isEmpty(propertyPathClean)) {
			posDot = -1;
		}
		int cnter = 0;
		for (; posDot >= 0; prevPosDot = posDot + 1) {
			cnter++;

			if (cnter > 20) {
				throw new RuntimeException("cant parse propertyPath=" + propertyPath + ", class=" + clazz);
			}

			if (prevPosDot >= propertyPathClean.length()) {
				break;
			}
			posDot = propertyPathClean.indexOf('.', prevPosDot);
			if (posDot == 0) {
				continue;
			}

			int posCast = propertyPathClean.indexOf('(', prevPosDot);
			int posFilter = propertyPathClean.indexOf(':', prevPosDot);

			if (posCast >= 0) {
				if (posFilter > 0 && posCast > posFilter) {
					posCast = -1;
				} else if (posDot >= 0) {
					if (posCast > posDot) {
						posCast = -1;
					} else {
						// cast could have package name, with '.' in it
						posDot = propertyPathClean.indexOf(')', posCast + 1);
						posDot = propertyPathClean.indexOf('.', posDot);
					}
				}
			}

			if (posDot >= 0 && posFilter > posDot) {
				posFilter = -1;
			}

			String propertyName;
			String propertyNameClean;

			if (posDot >= 0) {
				propertyName = propertyPath.substring(prevPosDot, posDot);
				propertyNameClean = propertyPathClean.substring(prevPosDot, posDot);
			} else {
				propertyName = propertyPath.substring(prevPosDot);
				propertyNameClean = propertyPathClean.substring(prevPosDot);
			}

			String castName = null;
			if (posCast >= 0) {
				int p = propertyNameClean.indexOf('(');
				if (p >= 0) {
					int p2 = propertyNameClean.indexOf(')', p);
					if (p2 > 0) {
						castName = propertyName.substring(p + 1, p2).trim();
						propertyName = propertyName.substring(p2 + 1).trim();
						propertyNameClean = propertyNameClean.substring(p2 + 1).trim();
					}
				}
			}
			this.castNames = (String[]) OAArray.add(String.class, this.castNames, castName);

			String filterName = null;
			String filterNameClean = null;
			String filterParam = null;
			String filterParamClean = null;
			Constructor filterConstructor = null;
			if (posFilter >= 0) {
				posFilter = propertyNameClean.indexOf(':');
				filterName = propertyName.substring(posFilter + 1).trim();
				filterNameClean = propertyNameClean.substring(posFilter + 1).trim();

				propertyName = propertyNameClean = propertyName.substring(0, posFilter).trim();
				int p = filterNameClean.indexOf('(');

				if (p >= 0) {
					filterParam = filterName.substring(p).trim();
					filterParamClean = filterNameClean.substring(p).trim();
					filterName = filterNameClean = filterName.substring(0, p).trim();
				}
			}
			this.filterNames = (String[]) OAArray.add(String.class, this.filterNames, filterName);
			this.filterParams = (String[]) OAArray.add(String.class, this.filterParams, filterParam); // ex: "(a,b)", "()"

			// figure out params
			int paramCount = 0;
			if (filterParam != null) {
				if (filterParam.charAt(0) == '(') {
					filterParam = filterParam.substring(1);
					filterParamClean = filterParamClean.substring(1);
					if (filterParam.charAt(filterParam.length() - 1) == ')') {
						filterParam = filterParam.substring(0, filterParam.length() - 1);
						filterParamClean = filterParamClean.substring(0, filterParamClean.length() - 1);
					}
				}
				paramCount = OAString.dcount(filterParamClean, ",");
			}

			if (OAObject.class.equals(clazz) && substituteClass != null) {
				clazz = substituteClass;
			}

			final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(clazz).objects().getOAObjectInfoService();
			OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(clazz);
			final OALinkInfo li = srvcObjectInfo.getLinkInfo(oi, propertyName);
			if (li != null) {
				endLinkInfo = li;
				endPropertyInfo = null;
				endCalcInfo = null;
				linkInfos = (OALinkInfo[]) OAArray.add(OALinkInfo.class, linkInfos, li);
			} else {
				endPropertyInfo = srvcObjectInfo.getPropertyInfo(oi, propertyName);
				
				if (endPropertyInfo != null) endCalcInfo = null;
				else {
					endCalcInfo = srvcObjectInfo.getOACalcInfo(oi, propertyName);
				}
				//was: endCalcInfo = endPropertyInfo != null ? null : OAObjectInfoDelegate.getOACalcInfo(oi, propertyName);
				endLinkInfo = null;
			}

			boolean bUsingToString;
			String mname;
			if (propertyName.length() == 0) {
				propertyName = mname = "toString";
				bUsingToString = true;
			} else {
				mname = "get" + propertyName;
				bUsingToString = false;
			}
			this.properties = (String[]) OAArray.add(String.class, this.properties, propertyName);

			// 20240131
            Method method = srvcObjectInfo.getMethod(oi, mname, 0);
            //was: Method method = OAReflect.getMethod(clazz, mname, 0);
			
			bLastMethodHasHubParam = false;
			if (method == null) {
				if (posDot < 0) {
					// 20131029 see if it is for hubCalc, which is a static method that has a Hub param
					//    must be the last property
					method = OAReflect.getMethod(clazz, mname, 1);
					if (method != null && Modifier.isStatic(method.getModifiers())) {
						if (Hub.class.equals(method.getParameterTypes()[0])) {
							bLastMethodHasHubParam = true;
						} else {
							method = null;
						}
					} else {
						method = null;
					}
				}

				if (method == null) {
					mname = "is" + propertyName;
					method = OAReflect.getMethod(clazz, mname, 0);
					if (method == null) {
						if (bIgnorePrivateLink && li != null && li.getPrivateMethod()) {
							// wait to show error
						} else {
							return "OAReflect.setup() cant find method. class=" + (clazz == null ? "null" : clazz.getName()) + " prop="
									+ propertyName + " path=" + propertyPath;
						}
					}
				}
			}
			this.methods = (Method[]) OAArray.add(Method.class, this.methods, method);

			if (bUsingToString) {
				// keep same clazz
			} else if (method == null) {
				clazz = li.getToClass();
			} else {
				clazz = method.getReturnType();
			}

			if (clazz != null && clazz.equals(Hub.class) && castName == null) {
				// try to find the ObjectClass for Hub
				Class c = srvcObjectInfo.getHubPropertyClass(classLast, propertyName);
				if (c != null) {
					clazz = c;
				}
			} else {
				if (castName != null) {
					String cn;
					if (castName.indexOf('.') >= 0) {
						cn = castName;
					} else {
						if (clazz != null) {
							String s = clazz.getName();
							int p = s.lastIndexOf('.');
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
						throw new RuntimeException("error getting castName class=" + cn, e);
					}

				} else if (cnter == 1 && clazz.equals(OAObject.class) && hub != null) { // 20150712
					// see if there is an object to check with
					for (Object o : hub) {
						if (o instanceof OAObject) {
							Object x = ((OAObject) o).getProperty(propertyName);
							if (x != null) {
								clazz = x.getClass();
								break;
							}
						}
					}
				}
			}

			// 20150715
			if (clazz.equals(OAObject.class) && hub != null) {
				// see if it can be found using data from hub
				Class c = findLastClass(hub);
				if (c == null || c.equals(OAObject.class)) {
					bNeedsDataToVerify = true;
				} else {
					clazz = c;
				}
			}

			this.classes = (Class[]) OAArray.add(Class.class, this.classes, clazz);
			classLast = clazz;

			// finish with Filter info
			Class filterClass = null;
			if (filterName != null) {
				String filterClassName;
				if (filterName.indexOf('.') >= 0) {
					filterClassName = filterName;
				} else {
					String s = clazz.getName();
					int p = s.lastIndexOf('.');
					filterClassName = s.substring(p + 1) + filterName + "Filter";

					// check annotations for correct upper/lower case
					OAClass oac = (OAClass) clazz.getAnnotation(OAClass.class);
					if (oac != null) {
						Class[] cs = oac.filterClasses();
						for (Class c : cs) {
							if (!CustomHubFilter.class.isAssignableFrom(c)) {
								continue;
							}
							int px = c.getName().toUpperCase().indexOf("." + filterClassName.toUpperCase());
							if (px >= 0) {
								if ((px + filterClassName.length() + 1) == c.getName().length()) {
									filterClass = c;
									filterClassName = c.getName();
									break;
								}
							}
						}
					}

					if (filterClass == null) {
						if (p >= 0) {
							s = s.substring(0, p + 1) + "filter.";
						} else {
							s = "";
						}
						filterClassName = s + filterName;
					}
				}
				if (filterClass == null) {
					try {
						filterClass = Class.forName(filterClassName);
						// note: filterClass does not have to exist, as some tools will allow
						//    creating custom ones.  ex: OAFinder has a method that is called to create the filter
					} catch (Exception e) {
					}
				}
				if (filterClass != null && !CustomHubFilter.class.isAssignableFrom(filterClass)) {
					return "Filter must implement interface CustomHubFilter";
				}
			}
			this.filterClasses = (Class[]) OAArray.add(Class.class, this.filterClasses, filterClass);

			Object[] filterParamValue = null;
			if (filterClass != null && paramCount > 0) {
				for (Constructor con : filterClass.getConstructors()) {
					Class[] cs = con.getParameterTypes();
					if (cs.length != paramCount + 2) {
						continue;
					}
					if (!cs[0].equals(Hub.class)) {
						continue;
					}
					filterConstructor = con;

					filterParamValue = new Object[paramCount];
					int p = 0;
					int prev = 0;
					for (int i = 0; p >= 0; i++, prev = p + 1) {
						p = filterParamClean.indexOf(',', prev);
						String s;
						if (p < 0) {
							s = filterParam.substring(prev).trim();
						} else {
							s = filterParam.substring(prev, p).trim();
						}

						// remove double quotes
						int x = s.length();
						if (x > 0 && s.charAt(0) == '\"' && s.charAt(x - 1) == '\"') {
							if (x < 3) {
								s = "";
							} else {
								s = s.substring(1, x - 2);
							}
						}
						if (s.equals("?")) {
							// needs to be an inputValue
							filterParamValue[i] = "?";
						} else {
							filterParamValue[i] = OAConv.convert(cs[i + 2], s);
						}
					}
					break;
				}
			} else {
				if (filterClass != null) {
					try {
						filterConstructor = filterClass.getConstructor(new Class[] { Hub.class, Hub.class });
					} catch (Exception e) {
						throw new RuntimeException("error getting filter constructor", e);
					}
				}
			}
			if (filterClass != null && filterConstructor == null) {
				//throw new RuntimeException("Could not find constructor for Filter, name="+filterName);
			}
			this.filterConstructors = (Constructor[]) OAArray.add(Constructor.class, this.filterConstructors, filterConstructor);

			if (this.filterParamValues == null) {
				this.filterParamValues = new Object[1][];
			} else {
				Object[][] objs = new Object[filterParamValues.length + 1][];
				System.arraycopy(filterParamValues, 0, objs, 0, filterParamValues.length);
				filterParamValues = objs;
			}
			filterParamValues[filterParamValues.length - 1] = filterParamValue;
		}

		// 20140118 update recursiveMethods
		if (linkInfos != null) {
			recursiveLinkInfos = new OALinkInfo[linkInfos.length];
			int j = 0;
			for (OALinkInfo li : linkInfos) {
				j++;
				if (li == null || !li.getRecursive()) {
					continue;
				}
				final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(li.getToClass()).objects().getOAObjectInfoService();
				OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(li.getToClass());

				OALinkInfo lix;
				if (li.getType() == OALinkInfo.MANY) {
					lix = srvcObjectInfo.getRecursiveLinkInfo(oi, OALinkInfo.MANY);
				} else {
					lix = srvcObjectInfo.getRecursiveLinkInfo(oi, OALinkInfo.ONE);
				}
				recursiveLinkInfos[j - 1] = lix;
			}
		}
		return null;
	}

	// 20150715 find the class by looking at the data
	/**
	 * Determines the final resolved class by inspecting runtime data.
	 *
	 * @param obj the object to inspect
	 * @return the resolved class, or {@code null} if it cannot be determined
	 */
	private Class findLastClass(final Object obj) {
		return _findLastClass(obj, 0);
	}

	/**
	 * Recursively determines the final resolved class starting at a given position.
	 *
	 * @param obj the current object to inspect
	 * @param pos the property index position
	 * @return the resolved class, or {@code null}
	 */
	private Class _findLastClass(final Object obj, final int pos) {
		if (this.properties == null || pos >= this.properties.length) {
			return null;
		}
		Class clazz;
		if (obj instanceof Hub) {
			clazz = _findLastClass((Hub) obj, pos);
		} else if (obj instanceof OAObject) {
			clazz = _findLastClass((OAObject) obj, pos);
		} else {
			clazz = null;
		}
		return clazz;
	}

	/**
	 * Determines the final resolved class by iterating through objects in a Hub.
	 *
	 * @param hubRoot the Hub to inspect
	 * @param pos     the property index position
	 * @return the resolved class, or {@code null}
	 */
	private Class _findLastClass(final Hub hubRoot, final int pos) {
		if (hubRoot == null) {
			return null;
		}

		if (this.properties == null || pos >= this.properties.length) {
			// this is last prop
			return hubRoot.getObjectClass();
		}

		Class clazz = null;
		for (Object obj : hubRoot) {
			if (!(obj instanceof OAObject)) {
				break;
			}
			clazz = _findLastClass(obj, pos + 1);
			if (clazz != null) {
				break;
			}
		}
		return clazz;
	}

	/**
	 * Determines the final resolved class by traversing an {@link OAObject}.
	 *
	 * @param obj the object to inspect
	 * @param pos the property index position
	 * @return the resolved class, or {@code null}
	 */
	private Class _findLastClass(final OAObject obj, final int pos) {
		if (obj == null) {
			return null;
		}

		if (this.properties == null || pos >= this.properties.length) {
			// this is last prop
			return obj.getClass();
		}

		Class clazz = null;
		Object objValue = ((OAObject) obj).getProperty(this.properties[pos]);
		if (objValue != null) {
			clazz = _findLastClass(objValue, pos + 1);
		}
		return clazz;
	}

	/**
	 * Flag indicating whether the output format has been evaluated.
	 */
	private boolean bFormat;

	/**
	 * Cached format string used when converting values to strings.
	 */
	private String format;

	/**
	 * Determines and returns the output format for the final property.
	 *
	 * @return the format string, or {@code null} if none applies
	 */
	public String getFormat() {
		if (format != null || bFormat) {
			return format;
		}
		bFormat = true;
		Class[] cs = getClasses();
		if (cs == null || cs.length == 0) {
			return null;
		}

		Class c = cs[cs.length - 1];

		OAProperty op = getOAPropertyAnnotation();
		if (op != null) {
			format = op.format();
			if (!OAString.isEmpty(format)) {
				return format;
			}

			int deci = op.decimalPlaces();
			if (OAReflect.isFloat(c)) {
				if (op.isCurrency()) {
					format = OAConv.getCurrencyFormat();
					format = getDecimalFormat(format, deci);
				} else {
					format = getDecimalFormat(deci);
				}
				return format;
			}
			if (OAReflect.isInteger(c)) {
				if (op.isCurrency()) {
					String fx = OAConv.getCurrencyFormat();
					if (deci < 0) {
						deci = 0;
					}
					format = getDecimalFormat(fx, deci);
				} else {
					format = OAConv.getIntegerFormat();
					format = getDecimalFormat(format, deci);
				}
				return format;
			}
		}

		OACalculatedProperty cp = getOACalculatedPropertyAnnotation();
		if (cp != null) {
			format = cp.outputFormat();
			if (!OAString.isEmpty(format)) {
				return format;
			}

			if (OAReflect.isFloat(c)) {
				if (cp.isCurrency()) {
					format = OAConv.getCurrencyFormat();
					format = getDecimalFormat(format, cp.decimalPlaces());
				} else {
					format = getDecimalFormat(cp.decimalPlaces());
				}
				return format;
			}
			if (OAReflect.isInteger(c)) {
				if (cp.isCurrency()) {
					String format = OAConv.getCurrencyFormat();
					int x = cp.decimalPlaces();
					if (x < 0) {
						x = 0;
					}
					format = getDecimalFormat(format, x);
				} else {
					format = OAConv.getIntegerFormat();
					format = getDecimalFormat(format, cp.decimalPlaces());
				}
				return format;
			}
		}

		if (OAReflect.isFloat(c)) {
			format = OAConv.getDecimalFormat();
		} else {
			format = OAConverter.getFormat(c);
		}
		return format;
	}

	/**
	 * Returns a decimal format string with the specified number of decimal places.
	 *
	 * @param deci the number of decimal places
	 * @return the decimal format string
	 */
	private String getDecimalFormat(int deci) {
		String format = OAConv.getDecimalFormat();
		return getDecimalFormat(format, deci);
	}

	/**
	 * Adjusts a decimal format string to the specified number of decimal places.
	 *
	 * @param format the base format string
	 * @param deci   the number of decimal places
	 * @return the adjusted decimal format string
	 */
	private String getDecimalFormat(String format, int deci) {
		if (format == null) {
			format = "";
		}
		if (deci < 0) {
			return format;
		}

		// DecimalFormat     = #,##0.00
		int pos = format.indexOf('.');
		if (pos < 0) {
			if (deci != 0) {
				format += ".";
				for (int i = 0; i < deci; i++) {
					format += "0";
				}
			}
		} else {
			int current = (format.length() - pos) - 1;
			int diff = deci - current;
			if (diff < 0) {
				format = format.substring(0, pos + deci + 1);
			} else if (diff > 0) {
				for (int i = 0; i < diff; i++) {
					format += "0";
				}
			}
		}
		return format;
	}

	/**
	 * Flag indicating whether the property path contains a Hub property.
	 */
	private boolean bHasHubProperty;

	/**
	 * Flag indicating whether the Hub property check has been performed.
	 */
	private boolean bHasHubPropertyCheck;

	/**
	 * Indicates whether the property path contains a Hub-valued property.
	 *
	 * @return {@code true} if a Hub property exists, otherwise {@code false}
	 */
	public boolean getHasHubProperty() {
		if (bHasHubPropertyCheck) {
			return bHasHubProperty;
		}
		if (linkInfos != null) {
			for (OALinkInfo li : linkInfos) {
				if (li.getType() == li.TYPE_MANY) {
					bHasHubProperty = true;
					break;
				}
			}
		}
		bHasHubPropertyCheck = true;
		return bHasHubProperty;
	}

	/*
	 * Used when fromObject is already in the propertyPath.
	 *
	 * @param fromObject object to start with
	 * @param startPos   number of properties to skip in the PP, that aligns with fromObject's position in the PP.
	 */
	/**
	 * Returns the value of the property path starting from a specific position.
	 *
	 * @param fromObject the object to start evaluation from
	 * @param startPos   the index of the property path to begin traversal
	 * @return the resolved value, or {@code null}
	 */
	public Object getValue(final OAObject fromObject, final int startPos) {
		if (fromObject == null) {
			return null;
		}

		Object result = fromObject;
		for (int i = startPos; i < methods.length; i++) {
			try {
				result = methods[i].invoke(result);
			} catch (Exception e) {
				throw new RuntimeException("error invoking method=" + methods[i], e);
			}

			if (result == null) {
				break;
			}
			if (i + 1 < methods.length && result instanceof Hub) {
				result = ((Hub) result).getAO();
				if (result == null) {
					break;
				}
			}
		}
		return result;
	}
}

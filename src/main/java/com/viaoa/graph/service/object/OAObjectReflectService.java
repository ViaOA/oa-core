package com.viaoa.graph.service.object;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubAutoMatch;
import com.viaoa.hub.HubAutoSequence;
import com.viaoa.hub.HubMerger;
import com.viaoa.hub.HubSortListener;
import com.viaoa.object.OACallback;
import com.viaoa.object.OACascade;
import com.viaoa.object.OACopyCallback;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAHierFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OASiblingHelper;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OANullObject;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAStr;
import com.viaoa.util.OAString;

public abstract class OAObjectReflectService {
	private static final Logger LOG = Logger.getLogger(OAObjectReflectService.class.getName());
	
	private final OAObject.FriendAccess faobject;
		
    public OAObjectReflectService(OAObject.FriendAccess oaObjectFriendAccess) {
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faobject = oaObjectFriendAccess;
    }
	
 	/**
	 * Creates a new instance of the specified class by delegating to
	 * the internal {@code _createNewObject} method. This method will
	 * attempt construction using the default no-arg constructor and
	 * return the resulting object instance.
	 *
	 * @param clazz the class to instantiate
	 * @return a new instance of the class, or a primitive wrapper/empty
	 *         primitive placeholder when applicable
	 */
	public <T extends OAObject> T createNewObject(Class<T> clazz) {
		T obj = _createNewObject(clazz);
		return obj;
	}

	/**
	 * Attempts to construct a new instance of the given class using its
	 * no-argument constructor. If the constructor is missing, the method
	 * falls back to primitive/primitive-wrapper helpers when applicable.
	 * Runtime exceptions are thrown when construction fails.
	 *
	 * @param clazz the class to instantiate
	 * @return the newly created instance or a primitive wrapper/default
	 * @throws RuntimeException if construction fails for any reason
	 */
	private <T> T _createNewObject(Class<T> clazz) {
		OAObjectInfo oi = getOAObjectInfo(clazz);
		T obj = null;

		/**
		 * 20190205 create on client if (!oi.getLocalOnly()) { RemoteSessionInterface rc = OASyncDelegate.getRemoteSession(clazz); if (rc !=
		 * null) { obj = rc.createNewObject(clazz); return obj; } }
		 **/

		try {
			Constructor constructor = clazz.getConstructor(new Class[] {});
			obj = (T) constructor.newInstance(new Object[] {});
		} catch (NoSuchMethodException nsme) {
			if (clazz.isPrimitive()) {
				obj = (T) OAReflect.getEmptyPrimitive(clazz);
			} else if (OAReflect.isPrimitiveClassWrapper(clazz)) {
				obj = (T) OAReflect.getPrimitiveClassWrapperObject(clazz);
			} else {
				throw new RuntimeException("OAObject.createNewObject() cant get constructor() for class " + clazz.getName() + " "
						+ nsme.getCause(), nsme);
			}
		} catch (InvocationTargetException te) {
			throw new RuntimeException("OAObject.createNewObject() cant get constructor() for class " + clazz.getName() + " "
					+ te.getCause(), te);
		} catch (Exception e) {
			throw new RuntimeException("OAObject.createNewObject() cant get constructor() for class " + clazz.getName() + " " + e, e);
		}
		return obj;
	}

	/**
	 * Retrieves a property value from the active object of the given
	 * {@link Hub} using the supplied property path. Delegates to the
	 * more general {@code getProperty(hub, null, propPath)}.
	 *
	 * @param hub      the Hub whose active object is used
	 * @param propPath the property name or path to evaluate
	 * @return the resolved property value or {@code null}
	 */
	public Object getProperty(Hub<?> hub, String propPath) {
		return getProperty(hub, null, propPath);
	}

	/**
	 * Resolves the value of the specified property path starting from the
	 * given {@link OAObject}. This delegates to the combined hub/object
	 * path evaluator {@code getProperty(null, oaObj, propPath)}.
	 *
	 * @param oaObj    the starting object
	 * @param propPath the property name or dotted path
	 * @return the value resolved from the path, or {@code null}
	 */
	public Object getProperty(OAObject oaObj, String propPath) {
		return getProperty(null, oaObj, propPath);
	}

	/**
	 * Resolves the value of a property or nested property path using
	 * reflection, Hub navigation, and OAObject metadata. Supports path
	 * tokens, optional class-cast segments, and transitions between
	 * Hubs and OAObjects while walking the path.
	 *
	 * @param hubLast  a Hub that may supply context when evaluating
	 *                 calculated Hub-based getters
	 * @param oaObj    the current OAObject in the traversal
	 * @param propPath the property or dotted path to evaluate
	 * @return the final resolved value or {@code null} if unavailable
	 */
	public Object getProperty(Hub<?> hubLast, OAObject oaObj, String propPath) {
		if (propPath == null || propPath.trim().length() == 0) {
			return null;
		}
		if (hubLast == null && oaObj == null) {
			return null;
		}

		if (propPath.indexOf('.') < 0) {
			return _getProperty(hubLast, oaObj, propPath);
		}
		StringTokenizer st = new StringTokenizer(propPath, ".", false);

		boolean b = false;
		for (;;) {
			if (!st.hasMoreTokens()) {
				return oaObj;
			}
			String tok = st.nextToken().trim();

			// 20161019 ignore class cast, ex: (com.test.Employee)emp.lname
			if (tok.length() == 0) {
				continue;
			}
			if (tok.charAt(0) == '(') {
				b = true;
				continue;
			}
			if (b) {
				int x = tok.indexOf(')');
				if (x < 0) {
					continue;
				}
				b = false;
				if (x + 1 == tok.length()) {
					continue;
				}
				tok = tok.substring(x + 1);
				tok = tok.trim();
			}

			Object value = _getProperty(hubLast, oaObj, tok);
			if (value == null || !st.hasMoreTokens()) {
				return value;
			}
			if (!(value instanceof OAObject)) {
				if (!(value instanceof Hub)) {
					break;
				}
				hubLast = (Hub) value;
				value = hubLast.getAO();
			} else {
				hubLast = null;
			}
			oaObj = (OAObject) value;
		}
		return null;
	}

	/**
	 * Retrieves a single property value (no path traversal) via metadata
	 * lookup. Supports calculated Hub-based getters, regular getters, use
	 * of primitive-null flags, and fallback to the OAObject property store
	 * when no getter method exists.
	 *
	 * @param hubLast   context Hub for calculated properties
	 * @param oaObj     the OAObject whose property is accessed
	 * @param propName  the simple property name
	 * @return the resolved value, possibly {@code null}
	 */
	private Object _getProperty(Hub<?> hubLast, OAObject oaObj, String propName) {
		OAObjectInfo oi;
		if (hubLast != null) {
			oi = getOAObjectInfo(hubLast.getObjectClass());
		} else {
			oi = getOAObjectInfo(oaObj.getClass());
		}

		Method m;
		if (oi.isHubCalcInfo(propName)) {
			if (hubLast == null) {
				return null;
			}
			m = callInfoGetMethod(oi, "get" + propName, 1);
			try {
				return m.invoke(oaObj, hubLast);
			} catch (InvocationTargetException e) {
				LOG.log(Level.WARNING, "error calling " + oaObj.getClass().getName() + ".getProperty(\"" + propName + "\")",
						e.getTargetException());
			} catch (Exception e) {
				LOG.log(Level.WARNING, "error calling " + oaObj.getClass().getName() + ".getProperty(\"" + propName + "\")", e);
			}
			return null;
		} else {
			if (oaObj == null) {
				return null;
			}
			m = callInfoGetMethod(oi, "get" + propName, 0);
			if (m == null) {
				m = callInfoGetMethod(oi, "is" + propName, 0);
			}
			if (m != null && (m.getModifiers() & Modifier.PRIVATE) == 0) {
				Class<?> c = m.getReturnType();
				if (c != null && c.isPrimitive() && getPrimitiveNull(oaObj, propName)) {
					return null;
				}
				try {
					return m.invoke(oaObj, (Object[]) null);
				} catch (InvocationTargetException e) {
					String s;
					if (oaObj != null) {
						s = oaObj.getClass().getName();
					} else {
						s = "object is null, ?";
					}
					LOG.log(Level.WARNING, "error calling " + s + ".getProperty(\"" + propName + "\")",
							e.getTargetException());
				} catch (Exception e) {
					String s;
					if (oaObj != null) {
						s = oaObj.getClass().getName();
					} else {
						s = "object is null, ?";
					}
					LOG.log(Level.WARNING, "error calling " + s + ".getProperty(\"" + propName + "\")", e);
				}
				return null;
			}
		}

		// check to see if it is in the oaObj.properties
		Object objx = callPropertyGetProperty(oaObj, propName, false, true);
		return objx;
	}

	/**
	 * Sets a property value on an {@link OAObject}, handling property-path
	 * navigation, primitive-null semantics, link updates, Hub assignment,
	 * type conversion, event firing, and reference resolution. When the
	 * value targets a MANY relationship, Hub-based logic is applied.
	 *
	 * @param oaObj    the target object
	 * @param propName the property name or path
	 * @param value    the new value (may be OAObject, OAObjectKey, or raw)
	 * @param fmt      optional formatter used for type conversion
	 */
	public void setProperty(final OAObject oaObj, String propName, Object value, final String fmt) {
		if (oaObj == null || propName == null || propName.length() == 0) {
			LOG.log(Level.WARNING, "property is invalid, =" + propName, new Exception());
			return;
		}

		// add support for propertyPath
		if (propName.indexOf('.') >= 0) {
			int pos = propName.lastIndexOf('.');
			String s = propName.substring(0, pos);
			propName = propName.substring(pos + 1);

			Object objx = getProperty(oaObj, s);
			if (objx instanceof OAObject) {
				setProperty((OAObject) objx, propName, value, fmt);
			}
			return;
		}

		final boolean bIsLoading = callThreadLocalIsLoading();

		String propNameU = propName.toUpperCase();
		final OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());

		Method m = null;
		if (value != null) {
			m = callInfoGetMethod(oi, "SET" + propNameU, value.getClass());
		}
		if (m == null) {
			m = callInfoGetMethod(oi, "SET" + propNameU, 1);
		}

		Class<?> clazz = null;
		if (m != null) {
			clazz = m.getParameterTypes()[0];
		}

		Object previousValue = null;

		if (clazz == null) {
			// See if this is for a Hub.  OAXMLReader uses setProperty to set MANY references using Object Id value
			m = callInfoGetMethod(oi, "GET" + propNameU, 0);
			if (m != null) {
				clazz = m.getReturnType();
				if (clazz != null && clazz.equals(Hub.class)) {
					setHubProperty(oaObj, propName, propNameU, value, oi, fmt);
					return;
				}
			}
			if (!bIsLoading) {
				previousValue = oaObj.getProperty(propName);
			}

			if (!bIsLoading) {
				callEventFireBeforePropertyChange(oaObj, propName, previousValue, value, oi.getLocalOnly(), true);
			}
			callPropertySetProperty(oaObj, propName, value);
			if (!bIsLoading) {
				callEventFirePropertyChange(oaObj, propName, previousValue, value, oi.getLocalOnly(), true);
			}
			return;
		}

		if (value instanceof OANullObject) {
			value = null;
		}
		OALinkInfo li = callInfoGetLinkInfo(oi, propNameU);

		if (li != null) {
			if (bIsLoading) {
				if (value == null) {
					// 20110315 allow null to be set
					callPropertySetProperty(oaObj, propName, value);
					//was: srvcObject.getOAObjectPropertyService().removeProperty(oaObj, propName, true);
				} else {
					if (!(value instanceof OAObject) && !(value instanceof OAObjectKey)) {
						value = callKeyCreateObjectKey(li.getToClass(), value);
					}
					callPropertySetProperty(oaObj, propName, value);
				}
				return;
			}
			previousValue = callPropertyGetProperty(oaObj, propName, false, true); // get previous value
		}

		boolean bPrimitiveNull = false; // a primitive type that needs to be set to null value
		if (li == null) {
			if (value == null && clazz.isPrimitive()) {
				bPrimitiveNull = true;
			} else if (value != null) { 
				value = OAConverter.convert(clazz, value, fmt); // convert to right type of class value
			}
		} else if (value == null) { // must be a reference property, being set to null value.
			if (previousValue == null) {
				return; // no change
			}
		} else if ((value instanceof OAObject)) { // reference property, that is an OAObject class type value
			if (previousValue == value) {
				return;
			}
			if (previousValue instanceof OAObjectKey) {
				OAObjectKey k = callKeyGetKey((OAObject) value);
				if (callKeyIsForSameOAObject(null, k, (OAObjectKey) previousValue)) {
					callPropertySetProperty(oaObj, propName, value);
					return; // no change; was storing key; now storing oaObject
				}
			}
		} else { //  (value NOT instanceof OAObject) either OAObjectKey or value of key
			if (!(value instanceof OAObjectKey)) {
				value = callKeyCreateObjectKey(li.getToClass(), value);
			}
			if (value.equals(previousValue)) {
				return; // no change
			}
			if (previousValue instanceof OAObject) {
				OAObjectKey k = callKeyGetKey((OAObject) previousValue);
				if (callKeyIsForSameOAObject(null, k, (OAObjectKey) value)) {
					return; // no change
				}
			}

			// have to get the real object
			Object findValue = getObject(li.getToClass(), value);
			if (findValue == null) {
				throw new RuntimeException("Cant find object for Id: " + value + ", class=" + li.getToClass().getSimpleName());
			}
			value = findValue;
		}

		boolean bCallSetMethod = true;
		try {
			if (bPrimitiveNull) {
				if (!bIsLoading) {
					previousValue = getProperty(oaObj, propName);
					if (previousValue == null) {
						return; // no change
					}
				}
				value = OAReflect.getPrimitiveClassWrapperObject(clazz);
				if (value == null) {
					bCallSetMethod = false; // cant call the setMethod, since it is a primitive type that cant be represented with a value
				} else if (value.equals(previousValue)) {
					bCallSetMethod = false; // no change, dont need to set the default value.
				}
			}
			if (bCallSetMethod) {
				m.invoke(oaObj, new Object[] { value });
			}
		} catch (Exception e) {
			String s = "property=" + propName + ", obj=" + oaObj + ", value=" + value;
			LOG.log(Level.WARNING, s, e);
			// e.printStackTrace();
			throw new RuntimeException("Exception in setProperty(), " + s, e);
		} finally {
			if (bPrimitiveNull) {
				// 20131101 calling firePropetyChange will call setPrimitiveNull
				// setPrimitiveNull(oaObj, propNameU);
				callEventFirePropertyChange(oaObj, propName, previousValue, null, oi.getLocalOnly(), true); // setting to null
			}
		}
	}

	/**
	 * Stores a raw link value directly into an object's property store,
	 * converting non-OAObject values to {@link OAObjectKey} when the link
	 * is a ONE relationship. No events or reverse-link handling are
	 * performed.
	 *
	 * @param oaObj        the object whose link is updated
	 * @param propertyName the name of the link property
	 * @param value        the raw value or key to store
	 */
	public void storeLinkValue(OAObject oaObj, String propertyName, Object value) {
		if (!(value instanceof OAObject) && !(value instanceof OAObjectKey)) {
			OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
			OALinkInfo li = oi.getLinkInfo(propertyName);
			if (li != null && li.getType() == li.ONE) {
				value = callKeyCreateObjectKey(li.getToClass(), value);
			}
		}
		callPropertySetProperty(oaObj, propertyName, value);
	}

	/**
	 * Determines whether a primitive property has its null flag set.
	 * This checks the object's internal null-tracking byte array and
	 * delegates to metadata to verify whether the given property is
	 * currently marked as representing a null primitive.
	 *
	 * @param oaObj        the object containing the property
	 * @param propertyName the property name
	 * @return {@code true} if the property represents a null primitive
	 */
	public boolean getPrimitiveNull(OAObject oaObj, String propertyName) {
		if (oaObj == null || propertyName == null) {
			return false;
		}
		
		byte[] nulls = faobject.getNulls(oaObj);
		if (nulls == null || nulls.length == 0) {
			return false;
		}
		synchronized (oaObj) {
			nulls = faobject.getNulls(oaObj);
			if (nulls == null) {
				return false;
			}
			boolean bAllZero = true;
			for (byte b : nulls) {
				if (b != 0) {
					bAllZero = false;
					break;
				}
			}
			if (bAllZero) {
				return false;
			}

			return callInfoIsPrimitiveNull(oaObj, propertyName);
		}
	}

	/**
	 * Sets or clears the null flag for a primitive property. This method
	 * delegates to the appropriate internal setter to mark the primitive
	 * as null or not without firing any property-change events.
	 *
	 * @param oaObj        the object whose property flag is updated
	 * @param propertyName the primitive property name
	 * @param bNull        {@code true} to set null, {@code false} to clear
	 */
	public void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull) {
		if (bNull) {
			setPrimitiveNull(oaObj, propertyName);
		} else {
			removePrimitiveNull(oaObj, propertyName);
		}
	}

	/**
	 * Marks the specified primitive property as null by updating the
	 * primitive-null metadata on the object. No events are fired and
	 * no additional adjustments are made.
	 *
	 * @param oaObj        the object whose property flag is set
	 * @param propertyName the property being marked as null
	 */
	private void setPrimitiveNull(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return;
		}
		synchronized (oaObj) {
			callInfoSetPrimitiveNull(oaObj, propertyName, true);
		}
	}

	/**
	 * Clears the null flag for the specified primitive property. This
	 * updates the internal primitive-null metadata without firing any
	 * property-change events or performing other adjustments.
	 *
	 * @param oaObj        the object whose flag is cleared
	 * @param propertyName the primitive property name
	 */
	private void removePrimitiveNull(OAObject oaObj, String propertyName) {
		byte[] nulls = faobject.getNulls(oaObj);
		if (nulls == null || nulls.length == 0) {
			return;
		}
		if (propertyName == null) {
			return;
		}
		synchronized (oaObj) {
			callInfoSetPrimitiveNull(oaObj, propertyName, false);
		}
	}

	/**
	 * Handles assignment of MANY relationship values to a Hub property.
	 * Converts raw identifiers into {@link OAObjectKey} instances when
	 * necessary, resolves keys to objects when appropriate, and adds
	 * values into the Hub if not already present.
	 *
	 * @param oaObj     the object whose Hub property is being updated
	 * @param propName  the original property name
	 * @param propNameU the uppercase property name
	 * @param value     the Hub-compatible value or key
	 * @param oi        metadata for the object
	 * @param fmt       optional formatter used during conversion
	 */
	private void setHubProperty(OAObject oaObj, String propName, String propNameU, Object value, OAObjectInfo oi, String fmt) {
		// this is for a Hub.  OAXMLReader uses setProperty to set MANY references using Object Id value for objects
		if (value == null) {
			return;
		}

		Hub<?> hub;
		Object objOrig = callPropertyGetProperty(oaObj, propName, false, true);

		if (value instanceof Hub) {
			callPropertySetPropertyCAS(oaObj, propName, value, objOrig);
			return;
		}

		
		OALinkInfo li = callInfoGetLinkInfo(oi, propNameU);
		if (li == null) {
			return;
		}

		if (objOrig != null) {
			if (!(objOrig instanceof Hub)) {
				throw new RuntimeException("stored object for " + propName + " is not a hub");
			}
			hub = (Hub<?>) objOrig;
		} else {
			hub = new Hub<>((Class<? extends OAObject>) li.getToClass());
			callPropertySetProperty(oaObj, propName, hub);
		}

		Class<? extends OAObject> c = hub.getObjectClass();
		boolean bKeyOnly = (c.equals(OAObjectKey.class));

		if (!(value instanceof OAObject)) {
			if (!(value instanceof OAObjectKey)) { // convert to OAObjectKey
				if (value instanceof Hub) {
					throw new RuntimeException("cant not set the Hub for " + propName);
				}
				value = callKeyCreateObjectKey(li.getToClass(), value);
			}
		}

		if (bKeyOnly) {
			if (value instanceof OAObject) {
				value = callKeyGetKey((OAObject) value);
			}
		} else {
			if (value instanceof OAObjectKey) {
				value = getObject(c, value);
			}
		}
		if (value instanceof OAObject && value != null && hub.getObject(value) == null) {
			((Hub<OAObject>)hub).add((OAObject) value);
		}
	}

	/**
	 * Retrieves an {@link OAObject} instance given a key or raw identifier.
	 * The lookup searches the cache first, then the server (when running
	 * as a client), and finally the datasource when needed.
	 *
	 * @param clazz the object's class type
	 * @param key   a key value or {@link OAObjectKey}
	 * @return the resolved object or {@code null} if not found
	 */
	public <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		if (clazz == null || key == null) {
			return null;
		}
		OAObjectInfo oi = getOAObjectInfo(clazz);
		return getObject(clazz, key, oi);
	}

	/**
	 * Variant of {@link #getObject(Class, Object)} that uses a supplied
	 * {@link OAObjectInfo}. Ensures the key is an {@link OAObjectKey}
	 * before performing cache, server, or datasource retrieval.
	 *
	 * @param clazz the object's class type
	 * @param key   a raw identifier or {@link OAObjectKey}
	 * @param oi    metadata associated with the class
	 * @return the located {@link OAObject} or {@code null}
	 */
	public <T extends OAObject> T getObject(final Class<T> clazz, Object key, OAObjectInfo oi) {
		if (clazz == null || key == null) {
			return null;
		}

		if (!(key instanceof OAObjectKey)) {
			key = callKeyCreateObjectKey(clazz, key);
		}

		T oaObj = callCacheGet(clazz, (OAObjectKey) key);
		if (oaObj == null) {
			if (callCSIsClient() && (oi == null || !oi.getLocalOnly())) {
				oaObj = callCSGetServerObject(clazz, (OAObjectKey) key);
			} else {
				oaObj = callDSGetObject(clazz, (OAObjectKey) key);
			}
		}
		return oaObj;
	}

	/**
	 * Retrieves a MANY relationship as a Hub of referenced objects,
	 * optionally applying sort order, sequencing, autoMatch assignment,
	 * and server/client-specific behaviors. Loads data as needed and
	 * caches or wraps the Hub based on metadata rules.
	 *
	 * @param oaObj            the master object
	 * @param linkPropertyName link property name (case insensitive)
	 * @param sortOrder        sort expression or {@code null}
	 * @param bSequence        true to enable sequencing support
	 * @param hubMatch         optional Hub for autoMatch
	 * @return the reference Hub, possibly empty but never {@code null}
	 */
	public <T extends OAObject> Hub<T> getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch) {
		/*
		 lock obj.props[]
		   get Hub from oaObj.props[]
		   if exists, but is null, then create an empty Hub
		   could be weakref, then get value
		   if not exists, then set to null
		   if hub.objClass is objectKey, then need to create new hub can load using keys
		   if client, then get on server, else get from DS
		   store hub in props: if hub is cached, then use weakref
		 unlock obj.props[]
		*/
		if (linkPropertyName == null) {
			return null;
		}
		callSiblingOnGetObjectReference(oaObj, linkPropertyName);

		Hub<T> hub = null;
		final OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		final OALinkInfo linkInfo = callInfoGetLinkInfo(oi, linkPropertyName);

		Object obj = callPropertyGetProperty(oaObj, linkPropertyName, false, true);

		if (obj instanceof Hub) {
			// 20141215 could be server side, that deserialized the object+references without setting up.
			hub = (Hub<T>) obj;

			// sort, seq, asc
			boolean bSortAsc = true;
			String seqProperty = null;
			if (linkInfo != null) {
				if (bSequence) {
					String s = linkInfo.getSeqProperty();
					if (OAString.notEmpty(s)) {
						seqProperty = s;
					} else {
						seqProperty = sortOrder;
					}
					if (OAString.isEmpty(seqProperty)) {
						bSequence = false;
					}
				} else {
					seqProperty = linkInfo.getSeqProperty();
					bSequence = OAString.notEmpty(seqProperty);
				}
				if (bSequence) {
					sortOrder = null;
					bSortAsc = false;
				} else if (OAString.isEmpty(sortOrder)) {
					sortOrder = linkInfo.getSortProperty();
					bSortAsc = linkInfo.isSortAsc();
				}
			}

			if (callCSIsServer()) {
				// 20150130 the same thread that is loading it could be accessing it again. (ex: matching and hubmerger during getReferenceHub)
				if (callLockIsPropertyLocked(oaObj, linkPropertyName)) {
					return (Hub<T>) hub;
				}

				// check to see if there needs to be an autoMatch set up
				if (callHubGetAutoMatch(hub) == null) {
					if (linkInfo != null) {
						String matchProperty = linkInfo.getMatchProperty();
						if (matchProperty != null && matchProperty.length() > 0) {
							if (hubMatch == null) {
								String matchHubPropPath = linkInfo.getMatchHub();
								if (matchHubPropPath != null && matchHubPropPath.length() > 0) {
									OAObjectInfo oix = getOAObjectInfo(linkInfo.getToClass());
									OALinkInfo linkInfox = callInfoGetLinkInfo(oix, matchProperty);
									if (linkInfox != null) {
										if (!callThreadLocalIsDeleting()) {
											hubMatch = new Hub(linkInfox.getToClass());
											HubMerger hm = new HubMerger(oaObj, hubMatch, matchHubPropPath);
											hm.setServerSideOnly(true);
										}
									}
								}
							}
							if (hubMatch != null) {
								hub.setAutoMatch(matchProperty, hubMatch, true, oaObj, linkInfo.getMatchStopProperty()); // serverSide only
							}
						} else {
							// 20220802
							String autoCreatProperty = linkInfo == null ? null : linkInfo.getAutoCreateProperty();
							if (OAString.isNotEmpty(autoCreatProperty)) {
								// get enum property getter method, get return value that is Enum and then number of values 0..n
								hub.setAutoMatch(autoCreatProperty, null, true, oaObj, linkInfo.getMatchStopProperty()); // serverSide only
							}
						}
					}
				}

				if (bSequence) {
					if (callHubGetAutoSequence(hub) == null) {
						hub.setAutoSequence(seqProperty, 0, false); // server will keep autoSequence property updated - clients dont need autoSeq (server side managed)
					}
				} else if (OAString.notEmpty(sortOrder) && callHubSortGetSortListener(hub) == null) {
					// keep the hub sorted on server only
					callHubSortSort(hub, sortOrder, bSortAsc, null, true);// dont sort, or send out sort msg (since no other client has this hub yet)
				}
			} else {
				// client might need a sort listener
				if (!bSequence) {
					boolean bAsc = true;
					String s = callHubSortGetSortProperty(hub); // use sort order from orig hub
					if (OAString.isEmpty(s)) {
						s = sortOrder;
					} else {
						bAsc = callHubSortGetSortAsc(hub);
					}
					
					if (OAString.isNotEmpty(s) && !callHubSortIsSorted(hub)) {
						// client recvd hub that has sorted property, without sortListener, etc.
						// note: serialized hubs do not have sortListener created - must be manually done
						//      this is done here (after checking first), for cases where references are serialized in a CS call.
						//      - or during below, when it is directly called.
						callHubSortSort(hub, s, bAsc, null, true);// dont sort, or send out sort msg
						/* not needed, already resorted on server
						OAPropertyInfo pi = oi.getPropertyInfo(s);
						if (pi == null || String.class.equals(pi.getClassType())) {
						    hub.resort(); // this will not send out event
						}
						*/
					}
				}
			}
		} else {
			boolean b = false;
			try {
				b = callLockSetPropertyLock(oaObj, linkPropertyName);

				obj = callPropertyGetProperty(oaObj, linkPropertyName, false, true);
				if (obj instanceof Hub) {
					return (Hub) obj;
				}

				hub = (Hub<T>) _getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch, oi, linkInfo);
			} finally {
				if (b) {
					callLockReleasePropertyLock(oaObj, linkPropertyName);
				}
			}
		}

		// 20160811 check to see if hub uses a pp
		String spp = linkInfo == null ? null : linkInfo.getMergerPropertyPath();
		if (OAStr.notEmpty(spp)) {
			new HubMerger(oaObj, hub, spp);
		}

		return (Hub<T>) hub;
	}

	// keeps track of siblings that are "in flight"
	private final ConcurrentHashMap<UUID, Boolean> hmIgnoreSibling = new ConcurrentHashMap<>();

	/**
	 * Internal implementation for retrieving the Hub associated with a
	 * MANY relationship. Handles server-side select logic, sibling
	 * loading, autoMatch, sequencing, sorting, cache management,
	 * Hub construction, and reference resolution.
	 *
	 * @param oaObj            the master object
	 * @param linkPropertyName property name for the relationship
	 * @param sortOrder        optional sort expression
	 * @param bSequence        true to apply sequencing rules
	 * @param hubMatch         Hub used for autoMatch
	 * @param oi               metadata for the master class
	 * @param linkInfo         link metadata for the relationship
	 * @return the initialized Hub
	 */
	private Hub<?> _getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder,
			boolean bSequence, Hub<?> hubMatch, final OAObjectInfo oi, final OALinkInfo linkInfo) {

		Object propertyValue = callPropertyGetProperty(oaObj, linkPropertyName, true, true);
		final boolean bThisIsServer = callCSIsServer();
		// dont get calcs from server, calcs are maintained locally, events are not sent
		boolean bIsCalc = (linkInfo != null && linkInfo.getCalculated());
		boolean bIsServerSideCalc = (linkInfo != null && linkInfo.getServerSideCalc());

		// sort, seq, asc
		boolean bSortAsc = true;
		String seqProperty = null;
		if (linkInfo != null) {
			if (bSequence) {
				String s = linkInfo.getSeqProperty();
				if (OAString.notEmpty(s)) {
					seqProperty = s;
				} else {
					seqProperty = sortOrder;
				}
				if (OAString.isEmpty(seqProperty)) {
					bSequence = false;
				}
			} else {
				seqProperty = linkInfo.getSeqProperty();
				bSequence = OAString.notEmpty(seqProperty);
			}
		}
		if (bSequence) {
			sortOrder = seqProperty;
			bSortAsc = true;
		} else if (OAString.isEmpty(sortOrder) && linkInfo != null) {
			sortOrder = linkInfo.getSortProperty();
			bSortAsc = linkInfo.isSortAsc();
		}

		Hub hub = null;
		if (propertyValue == null) {
			// since it is in props with a null, then it was placed that way to mean it has 0 objects
			//   by OAObjectSerializeDelegate._writeObject
			if (linkInfo == null) {
				hub = new Hub();
				callPropertySetProperty(oaObj, linkPropertyName, hub);
				return hub;
			}
			// create an empty hub
			hub = new Hub(linkInfo.getToClass(), oaObj, callInfoGetReverseLinkInfo(linkInfo), false);
		} else if (propertyValue == OANotExist.instance) {
			propertyValue = null;
		}

		if (propertyValue instanceof Hub) {
			hub = (Hub<?>) propertyValue;
			Class<? extends OAObject> c = hub.getObjectClass();
			if (!bThisIsServer) {
				boolean bAsc = true;
				String s = callHubSortGetSortProperty(hub); // use sort order from orig hub
				if (OAString.isEmpty(s)) {
					s = sortOrder;
				} else {
					bAsc = callHubSortGetSortAsc(hub);
				}
				if (!bSequence && !OAString.isEmpty(s) && !callHubSortIsSorted(hub)) {
					// client recvd hub that has sorted property, without sortListener, etc.
					// note: serialized hubs do not have sortListener created - must be manually done
					//      this is done here (after checking first), for cases where references are serialized in a CS call.
					//      - or during below, when it is directly called.
					callHubSortSort(hub, s, bAsc, null, true);// dont sort, or send out sort msg
					OAPropertyInfo pi = oi.getPropertyInfo(s);
					if (pi == null || String.class.equals(pi.getClassType())) {
						hub.resort(); // this will not send out event
					}
				}
			}
			if (callInfoCacheHub(linkInfo, hub)) {
				callPropertySetProperty(oaObj, linkPropertyName, new WeakReference(hub));
			} else {
				callPropertySetProperty(oaObj, linkPropertyName, hub);
			}
			return hub;
		}

		OASelect<?> select = null;
		//String sibIds = null;
		OAObjectKey[] siblingKeys = null;
		HashMap<OAObjectKey, Hub<?>> hmSiblingHub = null;
		final String matchProperty = linkInfo == null ? null : linkInfo.getMatchProperty();

		if (hub != null) {
			// no-op
		} else if (!bThisIsServer && !oi.getLocalOnly() && (!bIsCalc || bIsServerSideCalc)
				&& callSyncIsObjectOnServer(oaObj)) {
			// request from server
			hub = getCSGetServerReferenceHub(oaObj, linkPropertyName);
			if (hub == null) {
				// master not on the Server, might have been GCd, create empty Hub
				if (linkInfo == null) {
					return null;
				}
				Class<? extends OAObject> linkClass = linkInfo.getToClass();
				hub = new Hub(linkClass, oaObj, callInfoGetReverseLinkInfo(linkInfo), false);
				// throw new RuntimeException("getHub from Server failed, this.oaObj="+oaObj+", linkPropertyName="+linkPropertyName);
			}

			if (callHubMasterGetMasterObject(hub) == null) {
				if (hub.getSize() == 0 && hub.getObjectClass() == null) {
					if (linkInfo == null) {
						return null;
					}
					Class<? extends OAObject> linkClass = linkInfo.getToClass();
					hub = new Hub(linkClass, oaObj, callInfoGetReverseLinkInfo(linkInfo), false);
				}
			}
		} else { // hub is null, create now
			if (linkInfo == null) {
				return null;
			}
			Class<? extends OAObject> linkClass = linkInfo.getToClass();
			OALinkInfo liReverse = callInfoGetReverseLinkInfo(linkInfo);
			if (liReverse != null) {

				// 20141109
				hub = new Hub(linkClass, oaObj, liReverse, false);

				if (!bIsCalc && bThisIsServer) {
					// 20171225 support for selecting siblings at same time
					OALinkInfo rli = linkInfo.getReverseLinkInfo();
					if (!bThisIsServer || linkInfo.getRecursive() || rli == null || rli.getType() == OALinkInfo.TYPE_MANY
							|| rli.getPrivateMethod() || (hubMatch != null) || (matchProperty != null && matchProperty.length() > 0)) {
						// not yet supported
						siblingKeys = null;
					} else {
						int x;
						if (linkInfo.getCouldBeLarge()) {
							x = 4;
						} else {
							x = 25;
						}
						if (callThreadLocalIsDeleting()) {
							siblingKeys = null;
						} else {
							siblingKeys = callSiblingGetSiblings(oaObj, linkPropertyName, x, hmIgnoreSibling);
						}
					}

					select = new OASelect(hub.getObjectClass());
					if (siblingKeys != null && siblingKeys.length > 0) {
						hmSiblingHub = new HashMap<>();
						final List<OAObjectKey> alOk = new ArrayList<>();
						alOk.add(oaObj.getObjectKey());

						for (OAObjectKey keyx : siblingKeys) {
							OAObject objx = callCacheGet(oaObj.getClass(), keyx);
							if (objx == null) {
								continue;
							}
							if (!callLockAttemptPropertyLock(objx, linkPropertyName)) {
								continue;
							}
							alOk.add(keyx);
							hmSiblingHub.put(keyx, new Hub(linkClass, objx, liReverse, false));
						}

						select.setWhere(rli.getName() + " IN (?)");

						select.setParams(new Object[] { alOk });
					} else {
						if (bThisIsServer) {
							select.setWhereObject(oaObj);
							select.setPropertyFromWhereObject(linkInfo.getName());
						}
					}
				}

				//was: hub = new Hub(linkClass, oaObj, liReverse, true); // liReverse = liDetailToMaster
				/* 2013/01/08 recursive if this object is the owner (or ONE to Many) and the select
				 * hub is recursive of a different class - need to only select root objects. All
				 * children (recursive) hubs will automatically be assigned the same owner as the
				 * root hub when owner is changed/assigned. */
				/*
				 * 20130919 recurse does not have to be owner */
				//was: if (!srvcObject.getOAObjectInfoService().isMany2Many(linkInfo) && (bThisIsServer || bIsCalc) && linkInfo.isOwner()) {

				// 20131009 new LinkProperty recursive flag.  If owned+recursive, then select root
				if (bThisIsServer && !bIsCalc) {
					if (linkInfo.getOwner() && linkInfo.getRecursive()) {
						OAObjectInfo oi2 = getOAObjectInfo(linkInfo.getToClass());
						OALinkInfo li2 = callInfoGetRecursiveLinkInfo(oi2, OALinkInfo.ONE);
						if (li2 != null) {
							OALinkInfo li3 = callInfoGetReverseLinkInfo(li2);
							if (li3 != linkInfo) {
								if (select != null) {
									select.setWhere(li2.getName() + " == null");
								} else {
									hub.setSelectWhere(li2.getName() + " == null");
								}
							}
						}
					}
				}
				/*was
				if (!srvcObject.getOAObjectInfoService().isMany2Many(linkInfo) && (bThisIsServer || bIsCalc)) {
				    OAObjectInfo oi2 = getOAObjectInfo(linkInfo.getToClass());
				    OALinkInfo li2 = srvcObject.getOAObjectInfoService().getRecursiveLinkInfo(oi2, OALinkInfo.ONE);
				    if (li2 != null && li2 != liReverse) { // recursive
				        hub.setSelectWhere(li2.getName() + " == null");
				        // was: hub.setSelectRequiredWhere(li2.getName() + " == null");
				    }
				}
				*/
			} else {
				hub = new Hub(linkClass, oaObj, null, false);
			}
		}

		/*20171108 moved below. The issue with this is that this adds the Hub to oaObj.props before it runs the
		 *    select (which loads data).  Another thread could get this empty hub before the objects are loaded.
		
		    // 20141204 added check to see if property is now there, in case it was deserialized and then
		    //    the property was set by HubSerializeDelegate._readResolve
		    if (bThisIsServer || srvcObject.getOAObjectPropertyService().getProperty(oaObj, linkPropertyName, false, false) == null) {
		        // set property
		        if (srvcObject.getOAObjectInfoService().cacheHub(linkInfo, hub)) {
		            callPropertySetProperty(oaObj, linkPropertyName, new WeakReference(hub));
		        }
		        else {
		            callPropertySetProperty(oaObj, linkPropertyName, hub);
		        }
		    }
		 */
		if ((bThisIsServer || (bIsCalc && !bIsServerSideCalc)) && sortOrder != null && sortOrder.length() > 0) {
			String s = bSortAsc ? "" : " DESC";
			if (hub.getSelect() != null) {
				hub.setSelectOrder(sortOrder + s);
			} else if (select != null) {
				select.setOrder(sortOrder + s);
			}
		}

		// needs to loadAllData first, otherwise another thread could get the hub without using the lock
		if (bThisIsServer || (bIsCalc && !bIsServerSideCalc)) {
			// 20171225 support for selecting multiple at one time
			if (siblingKeys != null && siblingKeys.length > 0) {
				OALinkInfo rli = linkInfo.getReverseLinkInfo();
				try {
					callThreadLocalSetSuppressCSMessages(true);
					callThreadLocalSetLoading(true);
					for (; select.hasMore();) {
						OAObject objx = select.next();
						// find masterObj to put it in
						Object valx = callPropertyGetProperty(objx, rli.getName(), false, false);
						if (valx instanceof OAObject) {
							valx = ((OAObject) valx).getObjectKey();
						}
						if (!(valx instanceof OAObjectKey)) {
							continue;
						}
						OAObjectKey okx = (OAObjectKey) valx;
						if (callKeyIsForSameOAObject(null, okx, oaObj.getObjectKey())) {
							hub.add(objx);
						} else if (hmSiblingHub != null) {
							Hub hx = hmSiblingHub.get(okx);
							if (hx != null) {
								hx.add(objx);
							} else {
								// LOG.warn
							}
						}
					}
				} finally {
					callThreadLocalSetLoading(false);
					callThreadLocalSetSuppressCSMessages(false);
				}
			} else {
				if (!callCSLoadReferenceHubDataOnServer(hub, select)) { // load all data before passing to client
					callHubSelectLoadAllData(hub, select);
				}
			}

			hub.cancelSelect();
			if (select != null) {
				select.cancel();
				callHubDataResizeToFit(hub);
			}

			if (bThisIsServer) {
				if (bSequence) {
					if (callHubGetAutoSequence(hub) == null) {
						hub.setAutoSequence(seqProperty); // server will keep autoSequence property updated - clients dont need autoSeq (server side managed)
						if (hmSiblingHub != null) {
							// need to loop thru and set Hubs for siblings
							for (Entry<OAObjectKey, Hub<?>> entry : hmSiblingHub.entrySet()) {
								Hub<?> hx = entry.getValue();
								hx.setAutoSequence(seqProperty, 0, false); // server will keep autoSequence property updated - clients dont need autoSeq (server side managed)
							}
						}
					}
				} else if (OAString.notEmpty(sortOrder) && callHubSortGetSortListener(hub) == null) {
					// keep the hub sorted on server only
					callHubSortSort(hub, sortOrder, bSortAsc, null, true);// dont sort, or send out sort msg (since no other client has this hub yet)
					final OAPropertyInfo pi = oi.getPropertyInfo(sortOrder);
					
					if (pi == null || String.class.equals(pi.getClassType())) {
						hub.resort(); // dont trust db sorting, this will not send out event
					}

					if (hmSiblingHub != null) {
						// need to loop thru and set Hubs for siblings
						for (Entry<OAObjectKey, Hub<?>> entry : hmSiblingHub.entrySet()) {
							Hub<?> hx = entry.getValue();
							callHubSortSort(hx, sortOrder, bSortAsc, null, true);
						}
					}
				}
			}

			// 20110505 autoMatch propertyPath
			if (matchProperty != null && matchProperty.length() > 0) {
				if (hubMatch == null) {
					String matchHubProperty = linkInfo.getMatchHub();
					if (matchHubProperty != null && matchHubProperty.length() > 0) {
						OAObjectInfo oix = getOAObjectInfo(linkInfo.getToClass());
						OALinkInfo linkInfox = callInfoGetLinkInfo(oix, matchProperty);
						if (linkInfox != null) {
							hubMatch = new Hub(linkInfox.getToClass());
							HubMerger hm = new HubMerger(oaObj, hubMatch, matchHubProperty);
							hm.setServerSideOnly(true);
						}
					}
				}

				/*
				 * 20171113 moved after hub is added
				 * if (hubMatch != null) {
				 * 		hub.setAutoMatch(matchProperty, hubMatch, true);
				 * }
				 */
			}
		} else {
			if (!bSequence) {
				// create sorter for client
				boolean bAsc = true;
				String s = callHubSortGetSortProperty(hub); // use sort order from orig hub
				if (OAString.isEmpty(s)) {
					s = sortOrder;
				} else {
					bAsc = callHubSortGetSortAsc(hub);
				}
				if (!OAString.isEmpty(s)) {
					callHubSortSort(hub, s, bAsc, null, true);// dont sort, or send out sort msg (since no other client has this hub yet)
				}
			}
		}

		// 20171108 moved here from above
		if (bThisIsServer || callPropertyGetProperty(oaObj, linkPropertyName, false, false) == null) {
			// set property
			if (callInfoCacheHub(linkInfo, hub)) {
				callPropertySetProperty(oaObj, linkPropertyName, new WeakReference(hub));
			} else {
				callPropertySetProperty(oaObj, linkPropertyName, hub);
			}
		}
		// 20171113 moved from above
		if (hubMatch != null && (bThisIsServer || (bIsCalc && !bIsServerSideCalc))) {
			if (OAString.isNotEmpty(matchProperty)) {
				hub.setAutoMatch(matchProperty, hubMatch, true, oaObj, linkInfo.getMatchStopProperty());
			}
		}

		if (bThisIsServer || (bIsCalc && !bIsServerSideCalc)) {
			// 20220802
			String autoCreatProperty = linkInfo == null ? null : linkInfo.getAutoCreateProperty();
			if (OAString.isNotEmpty(autoCreatProperty)) {
				// get enum property getter method, get return value that is Enum and then number of values 0..n
				hub.setAutoMatch(autoCreatProperty, null, true, oaObj, linkInfo.getMatchStopProperty()); // serverSide only
			}
		}

		if (hmSiblingHub != null) {
			// need to loop thru and set Hubs for siblings
			for (Entry<OAObjectKey, Hub<?>> entry : hmSiblingHub.entrySet()) {
				OAObjectKey ok = entry.getKey();
				OAObject obj = callCacheGet(oaObj.getClass(), ok);
				if (obj == null) {
					continue;
				}
				Hub<?> hx = entry.getValue();
				if (callInfoCacheHub(linkInfo, hx)) {
					callPropertySetPropertyHubIfNotSet(obj, linkPropertyName, new WeakReference(hx));
				} else {
					callPropertySetPropertyHubIfNotSet(obj, linkPropertyName, hx);
				}
				callLockReleasePropertyLock(obj, linkPropertyName);
			}
		}
		if (siblingKeys != null) {
			for (OAObjectKey ok : siblingKeys) {
				hmIgnoreSibling.remove(ok.getGuid());
			}
		}
		return hub;
	}

	/**
	 * Returns the raw stored reference value for the specified link
	 * property without triggering loading. The result can be
	 * {@code null}, an {@link OAObjectKey}, an {@link OAObject},
	 * or a Hub containing either keys or objects.
	 *
	 * @param oaObj the object whose link is accessed
	 * @param name  the link property name
	 * @return the raw stored value
	 */
	public Object getRawReference(OAObject oaObj, String name) {
		Object obj = callPropertyGetProperty(oaObj, name, false, true);
		return obj;
	}

	/**
	 * Determines whether the given object is referenced by any of its
	 * relationships. Scans all used link properties and checks for
	 * non-null references, Hubs, resolved objects, or reverse links.
	 *
	 * @param oaObj the object to inspect
	 * @return {@code true} if it is referenced, otherwise {@code false}
	 */
	public boolean hasReference(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		OAObjectInfo io = getOAObjectInfo(oaObj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		for (OALinkInfo li : al) {
			if (!li.getUsed()) {
				continue;
			}
			String name = li.getName();
			Object obj = getRawReference(oaObj, name);
			if (obj == null) {
				continue;
			}
			if (obj instanceof Hub) {
				return true;
			}

			if (obj instanceof OAObjectKey) {
				obj = callCacheGet(li.getToClass(), (OAObjectKey) obj);
			}

			if (obj instanceof OAObject) {
				name = li.getReverseName();
				obj = getRawReference((OAObject) obj, name);
				if (obj != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the names of link properties whose referenced values have
	 * not yet been loaded. Includes or excludes calculated links based
	 * on the flag.
	 *
	 * @param obj          the target object
	 * @param bIncludeCalc true to include calculated links
	 * @return array of unloaded link property names, or {@code null}
	 */
	public String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc) {
		return getUnloadedReferences(obj, bIncludeCalc, null, true);
	}

	/**
	 * Variant of {@link #getUnloadedReferences(OAObject, boolean)} that
	 * excludes a specific property from consideration.
	 *
	 * @param obj                the object inspected
	 * @param bIncludeCalc       include calculated links if true
	 * @param exceptPropertyName property name to exclude
	 * @return array of unloaded link names, or {@code null}
	 */
	public String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName) {
		return getUnloadedReferences(obj, bIncludeCalc, exceptPropertyName, true);
	}

	/**
	 * Returns unloaded reference-property names, optionally filtering out
	 * calculated links, a named exception, and links marked as large.
	 *
	 * @param obj                the object inspected
	 * @param bIncludeCalc       include calculated links if true
	 * @param exceptPropertyName property name to exclude
	 * @param bIncludeLarge      include large links if true
	 * @return array of unloaded reference names, or {@code null}
	 */
	public String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge) {
		if (obj == null) {
			return null;
		}
		OAObjectInfo io = getOAObjectInfo(obj.getClass());
		ArrayList<String> al = null;
		List<OALinkInfo> alLinkInfo = io.getLinkInfos();
		for (OALinkInfo li : alLinkInfo) {
			if (!bIncludeCalc && li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			if (!bIncludeLarge && li.getCouldBeLarge()) {
				continue;
			}
			String property = li.getName();

			if (exceptPropertyName != null && exceptPropertyName.equalsIgnoreCase(property)) {
				continue;
			}

			Object value = getRawReference((OAObject) obj, property);
			if (value == null) {
				if (!callPropertyIsPropertyLoaded((OAObject) obj, property)) {
					if (al == null) {
						al = new ArrayList<String>();
					}
					al.add(property);
				}
			} else if (value instanceof OAObjectKey) {
				if (callCacheGet(li.getToClass(), (OAObjectKey) value) == null) {
					if (al == null) {
						al = new ArrayList<String>();
					}
					al.add(property);
				}
			}
		}
		if (al == null) {
			return null;
		}
		int x = al.size();
		String[] props = new String[x];
		al.toArray(props);
		return props;
	}

	/**
	 * Loads all reference properties for the given object, excluding
	 * calculated links. Delegates to {@code loadAllReferences(obj,false)}.
	 *
	 * @param obj the object whose references will be loaded
	 */
	public void loadAllReferences(OAObject obj) {
		loadAllReferences(obj, false);
	}

	/**
	 * Loads all reference properties for each object contained in the
	 * Hub, excluding calculated links. Delegates to
	 * {@code loadAllReferences(hub,false)}.
	 *
	 * @param hub the Hub whose objects will have references loaded
	 */
	public void loadAllReferences(Hub<?> hub) {
		loadAllReferences(hub, false);
	}

	/**
	 * Loads all reference properties for each object in the Hub, optionally
	 * including calculated links. Creates a sibling helper while loading.
	 *
	 * @param hub          Hub containing objects to load
	 * @param bIncludeCalc true to include calculated links
	 */
	public void loadAllReferences(Hub<?> hub, boolean bIncludeCalc) {
		OASiblingHelper siblingHelper = new OASiblingHelper(hub);
		callThreadLocalAddSiblingHelper(siblingHelper);
		try {
			for (Object obj : hub) {
				if (obj instanceof OAObject) {
					loadAllReferences((OAObject) obj, bIncludeCalc);
				}
			}
		} finally {
			callThreadLocalRemoveSiblingHelper(siblingHelper);
		}
	}

	/**
	 * Loads all reference properties for the given object, optionally
	 * including calculated links. Equivalent to a single-level load.
	 *
	 * @param obj          the object to load
	 * @param bIncludeCalc include calculated links if true
	 */
	public void loadAllReferences(OAObject obj, boolean bIncludeCalc) {
		loadReferences(obj, bIncludeCalc, 0);
	}

	/**
	 * Loads reference properties for the given object up to a maximum
	 * count. Respects calculated-link inclusion rules and uses metadata
	 * to determine whether a link is already loaded.
	 *
	 * @param obj          the object whose references are loaded
	 * @param bIncludeCalc include calculated links if true
	 * @param max          maximum number of references to load
	 */
	public void loadReferences(OAObject obj, boolean bIncludeCalc, int max) {
		OAObjectInfo io = getOAObjectInfo(obj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		int cnt = 0;
		for (OALinkInfo li : al) {
			if (!bIncludeCalc && li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			String name = li.getName();
			if (max > 0) {
				Object objx = callPropertyGetProperty(obj, name, true, true);
				if (objx == null) {
					continue;
				}
				if (objx != OANotExist.instance) {
					if (!(objx instanceof OAObjectKey)) {
						continue; // already loaded
					}
				}
			}
			getProperty(obj, name);
			cnt++;
			if (max > 0 && cnt >= max) {
				continue;
			}
		}
	}

	/**
	 * Determines whether all reference properties for the given object
	 * are fully loaded. Checks raw stored values, keys, Hub configurations,
	 * and server-side autoMatch requirements.
	 *
	 * @param obj          the object to check
	 * @param bIncludeCalc include calculated links if true
	 * @return {@code true} if all references are loaded
	 */
	public boolean areAllReferencesLoaded(OAObject obj, boolean bIncludeCalc) {
		if (obj == null) {
			return false;
		}
		OAObjectInfo io = getOAObjectInfo(obj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		boolean bIsServer = callCSIsServer();
		for (OALinkInfo li : al) {
			if (li == null) {
				continue;
			}
			if (!bIncludeCalc && li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			String name = li.getName();

			Object val = callPropertyGetProperty(obj, name, true, true);
			if (val == OANotExist.instance) {
				return false;
			}
			if (val instanceof OAObjectKey) {
				return false;
			}
			if (val instanceof Hub && bIsServer) {
				Hub<?> hubx = (Hub) val;
				// see if autoMatch (if used) is set up
				String matchProperty = li.getMatchProperty();
				if (matchProperty != null && matchProperty.length() > 0) {
					if (callHubGetAutoMatch(hubx) == null) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Loads reference properties of selected link types (ONE and/or MANY)
	 * for the given object. Increments and returns a count of loaded links.
	 *
	 * @param obj          the object whose references are loaded
	 * @param bOne         include ONE links if true
	 * @param bMany        include MANY links if true
	 * @param bIncludeCalc include calculated links if true
	 * @return number of loaded references
	 */
	public int loadAllReferences(OAObject obj, boolean bOne, boolean bMany, boolean bIncludeCalc) {
		OAObjectInfo io = getOAObjectInfo(obj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		int cnt = 0;
		for (OALinkInfo li : al) {
			if (!bIncludeCalc && li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			if (!bOne && li.getType() == OALinkInfo.ONE) {
				continue;
			}
			if (!bMany && li.getType() == OALinkInfo.MANY) {
				continue;
			}
			getProperty(obj, li.getName());
			cnt++;
		}
		return cnt;
	}

	/**
	 * Recursively loads reference properties up to a maximum depth.
	 *
	 * @param obj              the starting object
	 * @param maxLevelsToLoad  maximum recursive depth
	 * @return count of loaded references
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, 0, true, null, null, 0);
	}

	/**
	 * Loads all reference properties for each object contained in the
	 * supplied Hub up to the specified maximum recursion depth. Uses
	 * the internal recursive reference loader with default settings
	 * for owned-reference levels, calculated-link inclusion, callback,
	 * cascade, and maximum reference count.
	 *
	 * @param hub              the Hub whose objects will have references loaded
	 * @param maxLevelsToLoad  the maximum depth of recursive loading
	 * @return the total number of references loaded
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, 0, true, null, null, 0);
	}

	/**
	 * Loads reference properties for the given object up to the specified
	 * maximum recursion depth, including additional levels of owned links.
	 * Uses the internal recursive loader with defaults for calculated-link
	 * inclusion, callback, cascade, and maximum reference count.
	 *
	 * @param obj                        the starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, 0);
	}

	/**
	 * Loads reference properties for the given object up to a specified
	 * recursion depth and includes additional owned-reference levels.
	 * Limits the total number of references loaded to the supplied maximum.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, int maxRefsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for each object in the Hub up to the
	 * given recursion depth, including extra owned-reference levels.
	 * Uses default settings for calculated-link inclusion, callback,
	 * cascade, and maximum reference count.
	 *
	 * @param hub                        Hub containing objects
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @return number of references loaded
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, 0);
	}

	/**
	 * Loads reference properties for all objects in the Hub, respecting
	 * recursion depth and additional owned-reference levels while limiting
	 * the total number of references loaded.
	 *
	 * @param hub                        Hub to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the given object, optionally including
	 * calculated links, and using the supplied recursion and owned-link depth.
	 *
	 * @param obj                        the object to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, 0);
	}

	/**
	 * Loads reference properties for the given object with control over
	 * recursion depth, owned-link depth, calculated-link inclusion, and
	 * maximum references to load.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of loaded references
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, maxRefsToLoad);
	}

	/**
	 * Loads references for the given object with full control settings,
	 * including recursion depth, owned-link depth, calculated-link
	 * inclusion, maximum reference count, and a time limit for the load.
	 *
	 * @param obj                        the object to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param maxRefsToLoad              maximum references to load
	 * @param maxEndTime                 time limit in milliseconds
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad, long maxEndTime) {
		return _loadAllReferences(	0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, maxRefsToLoad,
									maxEndTime);
	}

	/**
	 * Loads references for each object in the Hub with the specified
	 * recursion depth, owned-link depth, and optional calculated-link
	 * inclusion.
	 *
	 * @param hub                        Hub to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @return number of references loaded
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, 0);
	}

	/**
	 * Loads references for all objects in the Hub using recursion and
	 * owned-link-depth rules while limiting the maximum number of
	 * references loaded.
	 *
	 * @param hub                        Hub containing objects
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of loaded references
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, maxRefsToLoad);
	}

	/**
	 * Loads references for the given object using recursion depth, owned
	 * levels, and optional calculated-link inclusion, calling the supplied
	 * callback before loading each object's references.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading references
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, 0);
	}

	/**
	 * Loads reference properties for the supplied object using the specified
	 * recursion depth, owned-link depth, calculated-link inclusion, and
	 * callback. Limits the total number of references loaded to the
	 * maximum supplied.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param maxRefsToLoad              maximum number of references to load
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback, int maxRefsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for all objects in the supplied Hub using
	 * the specified recursion depth, owned-link depth, calculated-link
	 * inclusion, and callback.
	 *
	 * @param hub                        Hub containing objects to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @return number of references loaded
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, 0);
	}

	/**
	 * Loads references for all objects in the Hub using recursion depth,
	 * owned-link depth, calculated-link inclusion, and callback rules,
	 * while enforcing a maximum number of references to load.
	 *
	 * @param hub                        Hub to process
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback, int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the Hub beginning at a specified
	 * starting depth, applying recursion depth, owned-link depth,
	 * calculated-link inclusion, callback processing, and cascade rules.
	 * Creates and manages a sibling helper for the duration of the load.
	 *
	 * @param hub                        starting Hub
	 * @param levelsLoaded               initial number of levels already loaded
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param cascade                    cascade manager used during loading
	 * @return number of references loaded
	 */
	public int loadAllReferences(final Hub<?> hub, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade) {
		int cnt = 0;

		final OASiblingHelper siblingHelper = new OASiblingHelper(hub);
		callThreadLocalAddSiblingHelper(siblingHelper);
		try {
			cnt = _loadAllReferences(	0, hub, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
										0);
		} finally {
			callThreadLocalRemoveSiblingHelper(siblingHelper);
		}
		return cnt;
	}

	/**
	 * Loads reference properties for the Hub starting at a defined depth,
	 * applying recursion limits, owned-link depth, calculated-link rules,
	 * callback behavior, and cascade management, while enforcing a maximum
	 * number of references to load.
	 *
	 * @param hub                        the Hub being processed
	 * @param levelsLoaded               initial depth already loaded
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public int loadAllReferences(final Hub<?> hub, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade, int maxRefsToLoad) {
		int cnt = 0;

		final OASiblingHelper siblingHelper = new OASiblingHelper(hub);
		callThreadLocalAddSiblingHelper(siblingHelper);
		try {
			cnt = _loadAllReferences(	0, hub, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
										maxRefsToLoad);
		} finally {
			callThreadLocalRemoveSiblingHelper(siblingHelper);
		}
		return cnt;
	}

	/**
	 * Loads reference properties for all objects in the Hub according to
	 * recursion depth, owned-link depth, and calculated-link rules, using
	 * the supplied cascade for traversal.
	 *
	 * @param hub                        Hub being loaded
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param cascade                    cascade handler
	 * @return number of loaded references
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade, 0);
	}

	/**
	 * Loads references for all objects in the Hub using recursion depth,
	 * owned-link depth, calculated-link inclusion, and cascade management,
	 * while enforcing a maximum number of references to load.
	 *
	 * @param hub                        target Hub
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of loaded references
	 */
	public int loadAllReferences(Hub<?> hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade, int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the given object using recursion
	 * depth, owned-link depth, calculated-link inclusion, and callback
	 * behavior. Uses defaults for cascade and maximum reference count.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade) {
		return loadAllReferences(obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade);
	}

	/**
	 * Loads reference properties for the given object using recursion,
	 * owned-link depth, calculated-link inclusion, a callback, and a
	 * maximum reference count.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade, int maxRefsToLoad) {
		return loadAllReferences(obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the Hub with the specified recursion
	 * depth, owned-link depth, calculated-link inclusion, and callback.
	 *
	 * @param hub                        Hub to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade) {
		return loadAllReferences(obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade, 0);
	}

	// ** MAIN reference loader here **
	/**
	 * Loads reference properties for the given object using recursion depth,
	 * owned-link depth, calculated-link inclusion, callback behavior, and cascade
	 * management. Limits the total number of references loaded to the specified
	 * maximum.
	 *
	 * @param obj                        the starting object
	 * @param levelsLoaded               number of previously loaded levels
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading references
	 * @param cascade                    cascade handler for traversal
	 * @param maxRefsToLoad              total max references allowed
	 * @return number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade, final int maxRefsToLoad) {
		return _loadAllReferences(	0, obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
									maxRefsToLoad);
	}

	/**
	 * Internal recursive loader for reference properties of an OAObject.
	 * Applies recursion depth, owned-link depth, calculated-link rules,
	 * callback behavior, cascade traversal, and maximum-reference limits.
	 * Tracks visited objects to prevent cycles.
	 *
	 * @param idStart                    internal identifier seed
	 * @param obj                        the object being processed
	 * @param levelsLoaded               number of loaded levels so far
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   callback invoked before loading
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references allowed
	 * @return number of references loaded
	 */
	private int _loadAllReferences(int currentRefsLoaded, final Hub<?> hub, final int levelsLoaded, final int maxLevelsToLoad,
			final int additionalOwnedLevelsToLoad,
			final boolean bIncludeCalc, final OACallback callback, OACascade cascade, final int maxRefsToLoad) {

		if (cascade == null) {
			cascade = new OACascade();
		}
		int cnt = 0;
		for (Object obj : hub) {
			int max = maxRefsToLoad > 0 ? (maxRefsToLoad - cnt) : 0;
			cnt += _loadAllReferences(	cnt, (OAObject) obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
										max);
			if (maxRefsToLoad > 0 && cnt >= maxRefsToLoad) {
				break;
			}
		}
		return cnt;

	}

	/**
	 * Internal recursive loader for reference properties of all objects in a Hub.
	 * Applies recursion limits, owned-link depth, calculated-link behavior,
	 * callback invocation, cascade traversal, and maximum-reference boundaries.
	 * Manages sibling-helper context during traversal.
	 *
	 * @param idStart                    internal identifier seed
	 * @param hub                        Hub whose objects are processed
	 * @param levelsLoaded               number of loaded levels so far
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   callback invoked before loading
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references allowed
	 * @return number of references loaded
	 */
	private int _loadAllReferences(int currentRefsLoaded, final OAObject obj, final int levelsLoaded, final int maxLevelsToLoad,
			final int additionalOwnedLevelsToLoad,
			final boolean bIncludeCalc, final OACallback callback, OACascade cascade, final int maxRefsToLoad) {

		return _loadAllReferences(	currentRefsLoaded, obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc,
									callback, cascade, maxRefsToLoad, 0);
	}

	/**
	 * Internal recursive loader for reference properties of all objects in a Hub.
	 * Applies recursion limits, owned-link depth, calculated-link behavior,
	 * callback invocation, cascade traversal, and maximum-reference boundaries.
	 * Manages sibling-helper context during traversal.
	 *
	 * @param idStart                    internal identifier seed
	 * @param hub                        Hub whose objects are processed
	 * @param levelsLoaded               number of loaded levels so far
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   callback invoked before loading
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references allowed
	 * @param maxEndTime                 time limit in milliseconds
	 * @return number of references loaded
	 */
	private int _loadAllReferences(int currentRefsLoaded, final OAObject obj, final int levelsLoaded, final int maxLevelsToLoad,
			final int additionalOwnedLevelsToLoad,
			final boolean bIncludeCalc, final OACallback callback, OACascade cascade, final int maxRefsToLoad, final long maxEndTime) {

		if (cascade == null) {
			cascade = new OACascade();
		}

		if (maxRefsToLoad > 0 && currentRefsLoaded >= maxRefsToLoad) {
			return currentRefsLoaded;
		}
		if (obj == null) {
			return currentRefsLoaded;
		}
		if (cascade.wasCascaded(obj, true)) {
			if (levelsLoaded > 0) {
				return currentRefsLoaded;
			}
		}
		if (callback != null) {
			if (!callback.updateObject(obj)) {
				return currentRefsLoaded;
			}
		}

		boolean bOwnedOnly = (levelsLoaded >= maxLevelsToLoad);

		OAObjectInfo oi = getOAObjectInfo(obj.getClass());
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (maxEndTime > 0 && System.currentTimeMillis() > maxEndTime) {
				break;
			}
			if (!bIncludeCalc && li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			if (bOwnedOnly && !li.getOwner()) {
				continue;
			}
			boolean bIsMany = li.getType() == OALinkInfo.TYPE_MANY;

			Object objx = callPropertyGetProperty(obj, li.getName(), true, true);

			if (objx instanceof OANotExist) { // not loaded from ds
				if (bIsMany) {
					currentRefsLoaded++;
				}
			} else if (objx instanceof OAObjectKey) { // not loaded from ds
				if (callCacheGet(li.getToClass(), (OAObjectKey) objx) == null) {
					currentRefsLoaded++;
				}
			}

			objx = obj.getProperty(li.getName()); // load prop
			if (maxRefsToLoad > 0 && currentRefsLoaded >= maxRefsToLoad) {
				break;
			}
			if (objx == null) {
				continue;
			}

			if (levelsLoaded + 1 >= maxLevelsToLoad) {
				if (levelsLoaded + 1 >= (maxLevelsToLoad + additionalOwnedLevelsToLoad)) {
					continue;
				}
			}

			if (objx instanceof Hub) {
				final OASiblingHelper siblingHelper = new OASiblingHelper((Hub) objx);
				callThreadLocalAddSiblingHelper(siblingHelper);
				try {
					for (Object objz : (Hub) objx) {
						currentRefsLoaded = _loadAllReferences(	currentRefsLoaded, (OAObject) objz, levelsLoaded + 1, maxLevelsToLoad,
																additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade, maxRefsToLoad,
																maxEndTime);
						if (maxLevelsToLoad > 0 && currentRefsLoaded >= maxLevelsToLoad) {
							break;
						}
					}
				} finally {
					callThreadLocalRemoveSiblingHelper(siblingHelper);
				}
			} else if (objx instanceof OAObject) {
				currentRefsLoaded = _loadAllReferences(	currentRefsLoaded, (OAObject) objx, levelsLoaded + 1, maxLevelsToLoad,
														additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade, maxRefsToLoad,
														maxEndTime);
			}
		}
		return currentRefsLoaded;
	}

	/**
	 * Retrieves the blob value for a reference property. Attempts to return a
	 * previously loaded byte array when available. If the property has not been
	 * loaded, this method acquires a property lock and retrieves the blob either
	 * from the server (in client mode) or from the datasource (in server mode),
	 * then stores the result using CAS assignment.
	 *
	 * @param oaObj        the object whose reference blob is requested
	 * @param propertyName the name of the reference property
	 * @return the blob as a byte array, or null if unavailable
	 */
	public byte[] getReferenceBlob(OAObject oaObj, String propertyName) {
		if (oaObj == null) {
			return null;
		}
		if (propertyName == null) {
			return null;
		}

		try {
			callLockSetPropertyLock(oaObj, propertyName);

			Object val = callPropertyGetProperty(oaObj, propertyName, true, true);
			if (val instanceof byte[]) {
				return (byte[]) val;
			}
			if (val != OANotExist.instance) {
				return null;
			}

			if (!callCSIsServer()) {
				val = callCSGetServerReferenceBlob(oaObj, propertyName);
			} else {
				OADataSource ds = callDSGetDataSource(oaObj.getClass());
				if (ds != null) {
					val = ds.getPropertyBlobValue(oaObj, propertyName);
				}
			}

			val = callPropertySetPropertyCAS(oaObj, propertyName, val, null, true, false);
			if (val instanceof byte[]) {
				return (byte[]) val;
			}

		} finally {
			callLockReleasePropertyLock(oaObj, propertyName);
		}
		return null;
	}

	
	/**
	 * Retrieves the referenced object for the specified link property. If the
	 * reference is already loaded and not an OAObjectKey, the existing value is
	 * returned. Otherwise this method acquires a property lock and delegates to
	 * the internal reference resolver. If a loaded result replaces a stored key,
	 * the property value is updated using CAS assignment.
	 *
	 * @param oaObj            the source object
	 * @param linkPropertyName the link property name
	 * @return the referenced OAObject or null
	 */
	public Object getReferenceObject(final OAObject oaObj, final String linkPropertyName) {
		if (oaObj == null) return null;
		callSiblingOnGetObjectReference(oaObj, linkPropertyName);

		Object objOriginal = callPropertyGetProperty(oaObj, linkPropertyName, true, true);

		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		OALinkInfo li = callInfoGetLinkInfo(oi, linkPropertyName);
		if (li == null) return null;

		if (objOriginal == null) { // else !null or notExist
			// it is stored as null value
			if (!li.getAutoCreateNew() && !li.getCalculated() && OAString.isEmpty(li.getDefaultPropertyPath())) {
				return null;
			}
		}

		boolean bDidNotExist = (objOriginal == OANotExist.instance);
		if (bDidNotExist) {
			objOriginal = null;
		} else if (objOriginal == null) {
		} else if (!(objOriginal instanceof OAObjectKey)) {
			return objOriginal; // found it
		}

		Object result = null;
		try {
			callLockSetPropertyLock(oaObj, linkPropertyName);
			result = _getReferenceObject(oaObj, linkPropertyName, oi, li);
			if (result != null || objOriginal == null) {
				callPropertySetPropertyCAS(oaObj, linkPropertyName, result, objOriginal, bDidNotExist, false);
			}
		} finally {
			callLockReleasePropertyLock(oaObj, linkPropertyName);
//qqqqqqqqqq makes Chat nervous qqqqqqqq recursive, keep trying to get oabjectKey			
			if (result instanceof OAObjectKey) {
				result = getReferenceObject(oaObj, linkPropertyName);
			}
		}
		return result;
	}

	/**
	 * Internal reference resolver. Uses metadata and stored property state to
	 * retrieve the referenced object. If the value is an OAObjectKey, the method
	 * attempts cache lookup or uses the appropriate datasource or server call to
	 * retrieve the object. Supports calculated links and server/client behaviors.
	 *
	 * @param oaObj            the source object
	 * @param linkPropertyName property name being resolved
	 * @param oi               object metadata
	 * @param li               link metadata
	 * @return the resolved referenced object or null
	 */
	private Object _getReferenceObject(final OAObject oaObj, final String linkPropertyName, final OAObjectInfo oi,
			final OALinkInfo li) {
		// note: this acquired a lock before calling
		if (linkPropertyName == null) {
			return null;
		}

		final boolean bIsServer = callCSIsServer();
		final boolean bIsCalc = li != null && li.getCalculated();

		Object ref = null;
		Object obj = callPropertyGetProperty(oaObj, linkPropertyName, true, true);

		if (!(obj instanceof OAObjectKey)) {
			if (obj == OANotExist.instance || obj == null) {
				// 20190112
				String pps = li.getDefaultPropertyPath();
				if (OAString.isNotEmpty(pps)) {
					if (li.getDefaultPropertyPathIsHierarchy()) {
						if (pps.toUpperCase().endsWith("." + linkPropertyName.toUpperCase())) {
							pps = pps.substring(0, (pps.length() - linkPropertyName.length()) - 1);
						}
						OAHierFinder hf = new OAHierFinder(linkPropertyName, pps, false);
						obj = hf.findFirst(oaObj);
						if (obj != null) {
							callPropertySetPropertyCAS(oaObj, linkPropertyName, obj, null);
							return obj;
						}
					} else {
						OAFinder hf = new OAFinder(pps);
						obj = hf.findFirst(oaObj);
						if (obj != null) {
							callPropertySetPropertyCAS(oaObj, linkPropertyName, obj, null);
							return obj;
						}
					}
				}
			}

			if (obj != OANotExist.instance) {
				if (obj != null) {
					return obj;
				}

				// must be null
				if (li.getAutoCreateNew()) {
					if (callInfoIsOne2One(li)) { // will only be "null" if it was deleted, else it will be oaNotExist
						return null;
					}
				} else {
					if (!li.getCalculated()) {
						return null;
					}
				}
			}

			// == null.  check to see if it is One2One, and if a select must be used to get the object.
			if (li == null) {
				return null;
			}
			if (callInfoIsOne2One(li)) {
				if (!oaObj.isNew()) {
					OALinkInfo liReverse = callInfoGetReverseLinkInfo(li);
					if (!bIsServer && !bIsCalc) {
						if (oaObj.isDeleted()) {
							return null;
						}
						if (liReverse != null && !liReverse.getPrivateMethod()) {
							ref = callCSGetServerReference(oaObj, linkPropertyName);
						} else {
							ref = null;
						}
					} else if (!bIsCalc) {
						if (liReverse != null && !liReverse.getPrivateMethod()) {
							OASelect sel = new OASelect(li.getToClass());
							sel.setWhereObject(oaObj);
							sel.setPropertyFromWhereObject(li.getName());
							sel.select();
							ref = sel.next();
							sel.close();
						}
					}
				}
			} else {
				// first check to see if it is in the hub for the link
				if (li.getPrivateMethod()) {
					Hub hubx = callHubGetHub(oaObj, li);
					if (hubx != null) {
						ref = callHubMasterGetMasterObject(hubx);
					}
				}

				if (ref == null && li.getPrivateMethod()) {
					OADataSource ds = callDSGetDataSource(li.getToClass());
					if (ds != null && ds.supportsStorage()) {
						if (!bIsServer && !bIsCalc) {
							if (oaObj.isDeleted()) {
								return null;
							}
							ref = callCSGetServerReference(oaObj, linkPropertyName);
						} else {
							OALinkInfo liReverse = callInfoGetReverseLinkInfo(li);
							if (liReverse != null) {
								OASelect sel = new OASelect(li.getToClass());
								sel.setWhere(liReverse.getName() + " = ?");
								sel.setParams(new Object[] { oaObj });
								sel.select();
								ref = sel.next();
								sel.close();
							}
						}
					}
				}
			}
		} else {
			OAObjectKey key = (OAObjectKey) obj;

			if (li == null) {
				return null;
			}

			ref = callCacheGet(li.getToClass(), key);

			if (ref == null) {
				if (!bIsServer && !bIsCalc && !oi.getLocalOnly()) {
					ref = callCSGetServerReference(oaObj, linkPropertyName);
				} else {
					OAObjectKey[] siblingKeys;
					if (callThreadLocalIsDeleting()) {
						siblingKeys = null;
					} else {
						siblingKeys = callSiblingGetSiblings(oaObj, linkPropertyName, 75, hmIgnoreSibling);
					}

					if (siblingKeys != null && siblingKeys.length > 0) {
						final List<OAObjectKey> alOk = new ArrayList<>();
						alOk.add(key);

						for (OAObjectKey keyx : siblingKeys) {
							OAObject objx = callCacheGet(oaObj.getClass(), keyx);
							if (objx == null) {
								continue;
							}

							Object val = callPropertyGetProperty(objx, linkPropertyName, false, false);
							if (!(val instanceof OAObjectKey)) {
								continue;
							}

							if (callLockIsPropertyLocked(objx, linkPropertyName)) {
								continue;
							}

							alOk.add((OAObjectKey) val);
						}

						OASelect sel = new OASelect(li.getToClass());
						String[] ss = li.getToObjectInfo().getIdProperties();
						String idProps = "";
						if (ss != null) {
							for (String s : ss) {
								if (idProps.length() > 0) {
									idProps += ", ";
								}
								idProps += s;
							}
							if (ss.length > 1) {
								idProps = "(" + idProps + ")";
							}
						}

						sel.setWhere(idProps + " IN (?)");
						sel.setParams(new Object[] { alOk });
						// was:  sel.setWhere("id IN (" + sibIds + ")");
						sel.select();
						for (; sel.hasMore();) {
							OAObject refx = sel.next(); // this will load into objCache w/weakRef
							if (callKeyIsForSameOAObject(null, refx.getObjectKey(), key)) {
								ref = refx;
							}
						}
						for (OAObjectKey ok : siblingKeys) {
							hmIgnoreSibling.remove(ok.getGuid());
						}
					} else {
						ref = (OAObject) callDSGetObject(oi, li.getToClass(), (OAObjectKey) obj);
					}
				}
			}
		}
		
		if (ref == null && li.getAutoCreateNew() && !bIsCalc) {
			boolean b = callInfoIsOne2One(li);
			if (b && oaObj.isDeleted() && !bIsServer) {
				// 20151117 dont autocreate new if this is deleted
			} else {
				if (!bIsServer && callSyncIsObjectOnServer(oaObj)) {
					ref = callCSGetServerReference(oaObj, linkPropertyName);
				} else {
					ref = createNewObject(li.getToClass());

					// 20190322
					if (((OAObject) ref).isLoading()) {
						callInitializeInitialize((OAObject) ref, getOAObjectInfo(li.getToClass()), true, true, true,
													false, true);
					}

					setProperty(oaObj, linkPropertyName, ref, null); // need to do this so oaObj.changed=true, etc.
					if (b) { // 20190220
						setProperty((OAObject) ref, li.getReverseLinkInfo().getName(), oaObj, null);
					}

					// 20231126 check for equalPropertyPath
                    String s = li.getEqualPropertyPath();
                    if (OAString.isNotEmpty(s)) {
                        OAPropertyPath pp = new OAPropertyPath(oaObj.getClass(), s);
                        final OAObject matchValue = (OAObject) pp.getValue(oaObj);
                        
                        final OALinkInfo liRev = callInfoGetReverseLinkInfo(li);
                        s = liRev.getEqualPropertyPath();
                        if (matchValue != null && OAString.isNotEmpty(s)) {
                            if (s.indexOf('.') < 0) {
                                ((OAObject) ref).setProperty(s, matchValue);
                            }
                            else {
                                pp = new OAPropertyPath(li.getToClass(), s);
                                OAPropertyPath ppRev = pp.getReversePropertyPath();
                                s = ppRev.getPropertyPath();
                                s = s.substring(0, s.lastIndexOf('.'));
                                
                                Object ref2 = matchValue.getProperty(s); 
                                if (ref2 instanceof OAObject) {
                                    s = liRev.getEqualPropertyPath();
                                    s = s.substring(0, s.indexOf('.'));
                                    ((OAObject) ref).setProperty(s, ref2);
                                }
                            }
                        }
                    }
				}
			}
		}
		return ref;
	}

	/**
	 * Retrieves the OAObjectKey for a reference property without loading the
	 * referenced object. Uses the internally stored value, which may be an
	 * OAObjectKey, an OAObject (from which a key is derived), or null when no
	 * key is available. This method never triggers object loading.
	 *
	 * @param oaObj    the source object
	 * @param property the reference property name
	 * @return the stored OAObjectKey, a derived key from an OAObject, or null
	 */
	public OAObjectKey getPropertyObjectKey(OAObject oaObj, String property) {
		if (property == null) {
			return null;
		}
		Object obj = callPropertyGetProperty(oaObj, property, false, true);
		if (obj == null) {
			return null;
		}
		if (obj instanceof OAObjectKey) {
			return (OAObjectKey) obj;
		}
		if (obj instanceof OAObject) {
			return callKeyGetKey((OAObject) obj);
		}
		return null;
	}

	/**
	 * Determines whether the reference value for the given property has been
	 * loaded. This includes detecting stored nulls, OANotExist markers, loaded
	 * OAObjects, non-key Hubs, and OAObjectKeys that can be resolved from the
	 * cache. When a cached match is found for a key, the property value is
	 * updated using CAS assignment.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if the reference is loaded or resolved, false otherwise
	 */
	public boolean hasReferenceObjectBeenLoaded(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true;
		}
		if (obj == OANotExist.instance) {
			return false;
		}
		if (obj instanceof OAObject) {
			return true;
		}
		if (obj instanceof Hub) {
			Hub<?> h = (Hub) obj;
			Class<? extends OAObject> c = h.getObjectClass();
			if (c.equals(OAObjectKey.class)) {
				return false;
			}
			return true;
		}
		if (obj instanceof OAObjectKey) {
			// use Key to see if object is in memory
			OALinkInfo li = callInfoGetLinkInfo(oaObj.getClass(), propertyName);
			if (li == null) {
				return true;
			}

			Object objFound = callCacheGet(li.getToClass(), (OAObjectKey) obj);
			if (objFound != null) {
				callPropertySetPropertyCAS(oaObj, propertyName, objFound, obj);
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the reference property is null or explicitly marked as
	 * not existing. A stored null or OANotExist marker indicates that the reference
	 * is empty without requiring object loading.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if the property is null or OANotExist, false otherwise
	 */
	public boolean isReferenceObjectNullOrEmpty(OAObject oaObj, String propertyName) {
		if (oaObj == null || propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true; // the ref is null, dont need to load it
		}
		if (obj == OANotExist.instance) {
			return true;
		}
		return false;
	}

	/**
	 * Determines whether the reference property is loaded and represents
	 * a non-empty value. Loaded OAObjects, non-key Hubs, and OAObjectKeys
	 * resolved from cache qualify as loaded and not empty. Null and
	 * OANotExist indicate empty or not loaded.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if loaded and non-empty
	 */
	public boolean isReferenceObjectLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return false; // the ref is null, dont need to load it
		}
		if (obj == OANotExist.instance) {
			return false;
		}
		if (obj instanceof OAObject) {
			return true;
		}

		if (obj instanceof OAObjectKey) {
			// use Key to see if object is in memory
			OALinkInfo li = callInfoGetLinkInfo(oaObj.getClass(), propertyName);
			if (li == null) {
				return true;
			}

			Object objFound = callCacheGet(li.getToClass(), (OAObjectKey) obj);
			if (objFound != null) {
				callPropertySetPropertyCAS(oaObj, propertyName, objFound, obj);
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a reference property is either null or not yet loaded.
	 * Null, OANotExist, or unresolved OAObjectKeys are treated as null or not
	 * loaded. Loaded OAObjects or Hubs return false.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property
	 * @return true if null or not loaded
	 */
	public boolean isReferenceNullOrNotLoaded(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true; // not loaded
		}
		if (obj == OANotExist.instance) {
			return true; // null
		}

		if (obj instanceof OAObject) {
			return false;
		}

		if (obj instanceof Hub) {
			return false;
		}

		if (obj instanceof OAObjectKey) {
			return !hasReferenceObjectBeenLoaded(oaObj, propertyName);
		}
		return false;
	}

	/**
	 * Determines whether the reference property is null, not loaded, or
	 * represented by an empty Hub. A stored null or OANotExist marker,
	 * an unresolved OAObjectKey, or a Hub with zero elements will return
	 * true. Loaded OAObjects and non-empty Hubs return false.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if the reference is null, not loaded, or an empty Hub
	 */
	public boolean isReferenceNullOrNotLoadedOrEmptyHub(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true; // not loaded
		}
		if (obj == OANotExist.instance) {
			return true; // ref is null
		}

		if (obj instanceof OAObject) {
			return false;
		}

		if (obj instanceof Hub) {
			return ((Hub) obj).getSize() == 0; // emptyHub
		}

		if (obj instanceof OAObjectKey) {
			return !hasReferenceObjectBeenLoaded(oaObj, propertyName);
		}
		return false;
	}

	/**
	 * Determines whether the MANY-relationship Hub for the specified
	 * property has been loaded. Evaluates the stored raw value and
	 * returns true only when the value is a Hub whose data has been
	 * fully loaded according to its internal load state.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the MANY link property name
	 * @return true if the Hub exists and is fully loaded
	 */
	public boolean isReferenceHubLoaded(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);

		if (obj instanceof OANotExist) {
			return false;
		}
		if (obj == null) {
			return true; // flag that hub could be create, with no objects
		}

		if (obj instanceof Hub) {
			return true;
		}

		OALinkInfo li = callInfoGetLinkInfo(oaObj.getClass(), propertyName);
		if (li == null || li.getType() != li.MANY) {
			return false;
		}
		return true;
	}

	/**
	 * Determines whether the MANY-relationship Hub for the given property
	 * is both loaded and contains zero elements. A Hub qualifies only if
	 * it is fully loaded and its size is zero. Null, OANotExist, unresolved
	 * keys, and non-Hub values do not qualify.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the MANY link property name
	 * @return true if the Hub is loaded and empty
	 */
	public boolean isReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true;
		}
		if (obj instanceof OANotExist) {
			return false;
		}

		if (obj instanceof Hub) {
			return ((Hub) obj).getSize() == 0;
		}

		OALinkInfo li = callInfoGetLinkInfo(oaObj.getClass(), propertyName);
		if (li == null || li.getType() != li.MANY) {
			return false;
		}
		return true;
	}

	/**
	 * Determines whether the MANY-relationship Hub for the given property
	 * is both fully loaded and contains one or more elements. A Hub must
	 * be loaded and have a size greater than zero to qualify. Null,
	 * OANotExist, unresolved keys, and unloaded Hubs do not qualify.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the MANY link property name
	 * @return true if the Hub is loaded and contains data
	 */
	public boolean isReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = callPropertyGetProperty(oaObj, propertyName, false, true);
		if (obj == null) {
			return false;
		}
		if (obj instanceof Hub) {
			return ((Hub) obj).getSize() > 0;
		}
		return false;
	}

	/**
	 * Loads the properties specified by the given property paths. Each path
	 * can reference a simple property or a dotted nested path. For each path,
	 * the method retrieves the corresponding value to ensure that it is
	 * loaded. No property-change events are fired by this method.
	 *
	 * @param oaObj         the object whose properties are to be loaded
	 * @param propertyPaths one or more property names or dotted paths
	 */
	public void loadProperties(OAObject oaObj, String... propertyPaths) {
		if (propertyPaths == null) {
			return;
		}
		if (propertyPaths.length == 0 || oaObj == null) {
			return;
		}

		LoadPropertyNode rootNode = createPropertyTree(propertyPaths);

		_loadProperties(oaObj, rootNode);
	}

	/**
	 * Loads the properties specified by the given property paths for every
	 * object in the supplied Hub. Each path can refer to a simple property
	 * or a dotted nested path. For each object and each path, the property
	 * value is accessed to ensure it is loaded. No property-change events
	 * are fired by this method.
	 *
	 * @param hub           the Hub whose objects will have properties loaded
	 * @param propertyPaths one or more property names or dotted paths
	 */
	public void loadProperties(Hub<?> hub, String... propertyPaths) {
		if (propertyPaths == null) {
			return;
		}
		if (propertyPaths.length == 0 || hub == null) {
			return;
		}

		LoadPropertyNode rootNode = createPropertyTree(propertyPaths);

		_loadProperties(hub, rootNode);
	}

	/**
	 * Builds a tree of LoadPropertyNode instances representing the supplied
	 * property paths. Each path is split into its dot-separated segments and
	 * inserted into the tree so that shared prefixes are merged. The resulting
	 * structure is used to efficiently load nested properties.
	 *
	 * @param propertyPaths one or more property names or dotted paths
	 * @return the root of the constructed property-path tree
	 */
	private LoadPropertyNode createPropertyTree(String... propertyPaths) {
		int x = 0;
		LoadPropertyNode rootNode = new LoadPropertyNode();
		for (String propertyPath : propertyPaths) {
			LoadPropertyNode node = rootNode; // beginning of property paths
			StringTokenizer st = new StringTokenizer(propertyPath, ".", false);
			for (; st.hasMoreTokens();) {
				String prop = st.nextToken();
				boolean b = false;
				if (node.children != null) {
					for (LoadPropertyNode pn : node.children) {
						if (pn.prop.equalsIgnoreCase(prop)) {
							node = pn;
							b = true;
							break;
						}
					}
				}
				if (!b) {
					LoadPropertyNode pn = new LoadPropertyNode();
					pn.prop = prop;
					node.children = (LoadPropertyNode[]) OAArray.add(LoadPropertyNode.class, node.children, pn);
					node = pn;
				}
			}
		}
		return rootNode;
	}

	/**
	 * Recursively loads the properties defined by the supplied property-tree
	 * node for the given object. Each node represents a single property, and
	 * its child nodes represent nested properties. For each node, the method
	 * retrieves the corresponding property value, and if the value is an
	 * OAObject or Hub, continues loading using the child nodes.
	 *
	 * @param object the current object or Hub being processed
	 * @param node   the tree node representing the property to load
	 */
	private void _loadProperties(Object object, LoadPropertyNode node) {
		if (object instanceof OAObject) {
			OAObject oaObj = (OAObject) object;
			if (node.children != null) {
				for (LoadPropertyNode pn : node.children) {
					Object value = _getProperty(null, oaObj, pn.prop);
					if (value != null) {
						_loadProperties(value, pn);
					}
				}
			}
		} else if (object instanceof Hub) {
			Hub<?> h = (Hub) object;
			if (!OAObject.class.isAssignableFrom(h.getObjectClass())) {
				return;
			}

			for (int j = 0;; j++) {
				OAObject obj = (OAObject) h.getAt(j);
				if (obj == null) {
					break;
				}
				_loadProperties(obj, node);
			}
		}
		// else no-op/done
	}

	/**
	 * Creates a shallow copy of the supplied OAObject, excluding any
	 * properties listed in the excludeProperties array. A new instance
	 * of the same class is created, and each property not excluded is
	 * assigned using the source object's current values. Link properties
	 * are copied by reference without loading additional data.
	 *
	 * @param oaObj            the source object to copy
	 * @param excludeProperties property names to exclude from copying
	 * @return the newly created copied object
	 */
	public OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
		return createCopy(oaObj, excludeProperties, null);
	}

	/**
	 * Creates a shallow copy of the supplied OAObject, excluding any
	 * properties listed in the excludeProperties array and allowing a
	 * callback to customize property-copy behavior. A new instance of
	 * the same class is created, and each non-excluded property is
	 * assigned from the source object's current value unless the
	 * callback overrides the assignment.
	 *
	 * @param oaObj            the source object to copy
	 * @param excludeProperties property names to exclude from copying
	 * @param copyCallback     optional callback to customize copying
	 * @return the newly created copied object
	 */
	public OAObject createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback) {
		HashMap<UUID, OAObject> hmNew = new HashMap();
		OAObject obj = _createCopy(oaObj, excludeProperties, copyCallback, hmNew);
		return obj;
	}

	/**
	 * Internal implementation used to create a shallow copy of the supplied
	 * OAObject. A new instance is created, and each non-excluded property is
	 * copied from the source object unless overridden by the callback. The
	 * hmNew map is used to track objects that have already been copied to
	 * prevent duplication when copying graphs of related objects.
	 *
	 * @param oaObj            the source object to copy
	 * @param excludeProperties property names to exclude from copying
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map used to track created copies
	 * @return the newly created copied object
	 */
	public OAObject _createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback,
			Map<UUID, OAObject> hmNew) {
		if (oaObj == null) {
			return null;
		}

		OAObject newObject = (OAObject) hmNew.get(callGuidGetGuid(oaObj));
		if (newObject != null) {
			return newObject;
		}

		// run on server only - otherwise objects can not be updated, since setLoadingObject is true
		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		if (!oi.getLocalOnly()) {
			if (!callCSIsServer()) {
				// 20130505 needs to be put in msg queue
				newObject = callCSCreateCopy(oaObj, excludeProperties);
				return newObject;
			}
		}

		try {
			callThreadLocalSetLoading(true);
			callThreadLocalSetSuppressCSMessages(true);

			newObject = (OAObject) createNewObject(oaObj.getClass());
			callInitializeInitialize(newObject, oi, true, true, false, false, true);

			_copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);

		} finally {
			callThreadLocalSetSuppressCSMessages(false);
			callThreadLocalSetLoading(false);
		}
		callCacheAdd(newObject);
		return newObject;
	}

	
	/**
	 * Copies the properties of the source OAObject into the supplied
	 * destination object. Properties listed in excludeProperties are
	 * skipped. For each non-excluded property, the current value from
	 * the source object is assigned to the destination unless the
	 * callback overrides or blocks the assignment. Link properties
	 * are copied by reference without triggering additional loading.
	 *
	 * @param oaObj            the source object whose values are copied
	 * @param newObject        the destination object
	 * @param excludeProperties property names to exclude from copying
	 * @param copyCallback     optional callback to customize copy behavior
	 */
	public void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback) {
		HashMap<UUID, OAObject> hmNew = new HashMap();
		copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
	}

	/**
	 * Internal implementation used to copy property values from the source
	 * OAObject into the destination object. Properties listed in
	 * excludeProperties are skipped. For each non-excluded property, the
	 * current value from the source object is assigned to the destination
	 * unless the callback overrides the assignment. The hmNew map tracks
	 * objects already processed to prevent duplicate copying when copying
	 * object graphs.
	 *
	 * @param oaObj            the source object
	 * @param newObject        the destination object
	 * @param excludeProperties properties to exclude from copying
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map tracking objects already copied
	 */
	public void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback,
			HashMap<UUID, OAObject> hmNew) {
		try {
			callThreadLocalSetLoading(true);
			callThreadLocalSetSuppressCSMessages(true);

			_copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
		} finally {
			callThreadLocalSetLoading(false);
			callThreadLocalSetSuppressCSMessages(false);
		}
	}

	/**
	 * Internal recursive implementation for copying property values from the
	 * source OAObject into the destination object. Excluded properties are
	 * skipped. For each non-excluded property, the current value from the
	 * source object is assigned to the destination unless the callback
	 * overrides or blocks the assignment. The hmNew map tracks objects that
	 * have already been processed to prevent duplicating work when copying
	 * object graphs.
	 *
	 * @param oaObj            the source object
	 * @param newObject        the destination object
	 * @param excludeProperties property names to exclude
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map tracking already-copied objects
	 */
	public <T extends OAObject> void _copyInto(final T oaObj, final T newObject, final String[] excludeProperties,
			final OACopyCallback copyCallback, final Map<UUID, OAObject> hmNew) {
		if (oaObj == null || newObject == null) {
			return;
		}
		hmNew.put(callGuidGetGuid(oaObj), newObject);
		if (!(oaObj.getClass().isInstance(newObject))) {
			throw new IllegalArgumentException("OAObject.copyInto() object is not same class");
		}
		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (excludeProperties != null) {
				int j = 0;
				for (; j >= 0 && j < excludeProperties.length; j++) {
					if (excludeProperties[j] == null) {
						continue;
					}
					if (excludeProperties[j].equalsIgnoreCase(pi.getName())) {
						j = -5;
					}
				}
				if (j < 0) {
					continue;
				}
			}
			if (!pi.getId()) {
				Object value = oaObj.getProperty(pi.getName());
				if (copyCallback != null) {
					value = copyCallback.getPropertyValue(oaObj, pi.getName(), value);
				}
				newObject.setProperty(pi.getName(), value);
			}
		}

		// make copy of owned many objects
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.MANY) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			boolean bM2M = li.isMany2Many();
			boolean bCopy = (li.isOwner() || bM2M);

			if (bCopy && excludeProperties != null) {
				for (int j = 0; bCopy && j < excludeProperties.length; j++) {
					if (excludeProperties[j] == null) {
						continue;
					}
					if (excludeProperties[j].equalsIgnoreCase(li.getName())) {
						bCopy = false;
					}
				}
			}
			if (copyCallback != null) {
				bCopy = copyCallback.shouldCopyOwnedHub(oaObj, li.getName(), bCopy);
			}
			if (!bCopy) {
				continue;
			}
			Hub hub = (Hub<?>) getProperty(oaObj, li.getName());
			Hub hubNew = (Hub<?>) getProperty(newObject, li.getName());
			for (int j = 0; hub != null && hubNew != null; j++) {
				OAObject obj = (OAObject) hub.elementAt(j);
				if (obj == null) {
					break;
				}

				// 20200405
				if (bM2M) {
					hubNew.add(obj);
					continue;
				}

				OAObject objx = hmNew.get(callGuidGetGuid((OAObject) obj));

				if (objx == null) {
					if (copyCallback != null) {
						objx = copyCallback.createCopy(oaObj, li.getName(), hub, obj);
						if (obj == objx) {
							objx = _createCopy(obj, (String[]) null, copyCallback, hmNew);
						}
					} else {
						objx = _createCopy(obj, (String[]) null, copyCallback, hmNew);
						//was: objx = obj.createCopy();
					}
				}
				if (objx != null) {
					if (obj != objx) {
						hmNew.put(callGuidGetGuid(obj), objx);
					}
					hubNew.add(objx);
					// assign parentProperty
					callPropertyUnsafeSetProperty(	(OAObject) objx, callHubDetailGetPropertyFromDetailToMaster(hubNew), newObject);
				}
			}
		}

		// set One links, if it is not an owner, or if it is autocreated
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.ONE) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (excludeProperties != null) {
				boolean b = true;
				for (int j = 0; j < excludeProperties.length; j++) {
					if (excludeProperties[j] == null) {
						continue;
					}
					if (excludeProperties[j].equalsIgnoreCase(li.getName())) {
						b = false;
						break;
					}
				}
				if (!b) {
					continue;
				}
			}

			Object obj = getProperty(oaObj, li.getName());

			OALinkInfo liRev = callInfoGetReverseLinkInfo(li);
			if (liRev != null && liRev.isOwner() && !li.getAutoCreateNew()) {
				Object newObj = hmNew.get(callGuidGetGuid((OAObject) obj)); // this is the new/replacement one to use
				if (newObj != null) {
					if (copyCallback != null) {
						newObj = copyCallback.getPropertyValue(oaObj, li.getName(), newObj);
					}
					newObject.setProperty(li.getName(), newObj);
				}
				// else dont assign, since it has the owner as the old/original object. It will be assigned when a new ownerObj is copied
				continue;
			}

			if (li.getAutoCreateNew() && obj instanceof OAObject) {
				Object objx = newObject.getProperty(li.getName()); // creates new
				if (objx instanceof OAObject) {
					_copyInto((OAObject) obj, (OAObject) objx, (String[]) null, copyCallback, hmNew);
				}
			} else {
				boolean b = false;
				if (obj != null) {
					Object objx = hmNew.get(callGuidGetGuid((OAObject) obj));
					if (objx != null) {
						b = true; // object is already a copy
						obj = objx;
					}
				}
				if (!b && copyCallback != null) {
					Object objFromCallback = copyCallback.getPropertyValue(oaObj, li.getName(), obj);

					if (obj == objFromCallback && obj instanceof OAObject) {
						obj = objFromCallback;
						if (shouldMakeACopy((OAObject) obj, excludeProperties, copyCallback, hmNew, 0, null)) {
							OAObject objx = _createCopy((OAObject) obj, excludeProperties, copyCallback, hmNew);
							if (objx != obj && objx != null) {
								hmNew.put(callGuidGetGuid((OAObject) obj), objx);
								obj = objx;
							}
						}
					} else {
						obj = objFromCallback;
					}
				}
				newObject.setProperty(li.getName(), obj);
			}
		}
	}

	/**
	 * Determines whether a new copy should be created for the supplied
	 * OAObject during a copy operation. Uses the excludeProperties list,
	 * the callback, the map of already-created copies, and the visitor
	 * set to prevent cycles and repeated work. The counter tracks the
	 * recursion depth or number of processed items.
	 *
	 * @param oaObj            the object being evaluated
	 * @param excludeProperties property names excluded from copying
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map of already-created copies
	 * @param cnt              counter used to track depth or iteration
	 * @param hsVisitor        set of visited object identifiers
	 * @return true if a new copy should be created, false otherwise
	 */
	private boolean shouldMakeACopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback,
			Map<UUID, OAObject> hmNew, int cnt, Set<UUID> hsVisitor) {
		if (oaObj == null) {
			return false;
		}
		if (hsVisitor == null) {
			hsVisitor = new HashSet<UUID>(101, .75f);
		} else if (hsVisitor.contains(callGuidGetGuid(oaObj))) {
			return false;
		}
		hsVisitor.add(callGuidGetGuid(oaObj));

		OAObjectInfo oi = getOAObjectInfo(oaObj.getClass());
		List<OALinkInfo> alLinkInfo = oi.getLinkInfos();
		for (OALinkInfo li : alLinkInfo) {
			if (li.getCalculated()) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}

			if (excludeProperties != null) {
				boolean b = true;
				for (int j = 0; j < excludeProperties.length; j++) {
					if (excludeProperties[j] == null) {
						continue;
					}
					if (excludeProperties[j].equalsIgnoreCase(li.getName())) {
						b = false;
						break;
					}
				}
				if (!b) {
					continue;
				}
			}

			if (li.getType() == li.MANY) {
				Hub<?> hub = (Hub) getProperty(oaObj, li.getName());
				for (int j = 0; hub != null; j++) {
					OAObject obj = (OAObject) hub.elementAt(j);
					if (obj == null) {
						break;
					}
					Object objx = hmNew.get(callGuidGetGuid((OAObject) obj));
					if (objx != null) {
						return true;
					}

					if (cnt < 3 && obj != null) {
						if (shouldMakeACopy(obj, excludeProperties, copyCallback, hmNew, cnt + 1, hsVisitor)) {
							return true;
						}
					}
				}
			} else {
				Object obj = getProperty(oaObj, li.getName());
				if (obj != null) {
					Object objx = hmNew.get(callGuidGetGuid((OAObject) obj));
					if (objx != null) {
						return true;
					}

					if (cnt < 3 && obj instanceof OAObject) {
						if (shouldMakeACopy((OAObject) obj, excludeProperties, copyCallback, hmNew, cnt + 1, hsVisitor)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public Class<?> getHubObjectClass(Method method) {
		Class<? extends OAObject> cx = null;
		Type rt = method.getGenericReturnType();
		if (rt instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) rt;
			try {
				Type[] types = pt.getActualTypeArguments();
				if (types != null && types.length > 0 && types[0] instanceof Class) {
					cx = (Class) types[0];
				}
			} catch (Throwable t) {
			}
		}
		return cx;
	}

	/**
	 * Searches upward through the parent hierarchy of the two supplied
	 * OAObjects to find a common Hub. Each object’s parent chain is
	 * traversed up to the specified maximum number of levels, and the
	 * first Hub encountered in both hierarchies is returned.
	 *
	 * @param obj1             the first object
	 * @param obj2             the second object
	 * @param maxLevelsToCheck maximum number of parent levels to traverse
	 * @return the first common Hub found, or null if none exists
	 */
	public Hub findCommonHierarchyHub(OAObject obj1, OAObject obj2, int maxLevelsToCheck) {
		return findCommonHierarchyHub(obj1, obj2, 0, maxLevelsToCheck);
	}

	/**
	 * Recursive helper used to search for a common Hub in the parent
	 * hierarchies of the two supplied OAObjects. The search proceeds
	 * upward through each object's hierarchy while tracking the
	 * current recursion depth, stopping when the maximum number of
	 * levels has been reached or a common Hub is found.
	 *
	 * @param obj1             the first object
	 * @param obj2             the second object
	 * @param currentLevel     the current recursion level
	 * @param maxLevelsToCheck maximum allowed recursion depth
	 * @return the common Hub if found, otherwise null
	 */
	public Hub findCommonHierarchyHub(OAObject obj1, OAObject obj2, int currentLevel, int maxLevelsToCheck) {
		if (obj1 == null || obj2 == null) {
			return null;
		}
		if (currentLevel >= maxLevelsToCheck) {
			return null;
		}

		Hub[] hubs = callHubGetHubReferences(obj1);
		for (int i = 0; hubs != null && i < hubs.length; i++) {
			Hub<?> nextHub = hubs[i];
			if (nextHub == null) {
				continue;
			}
			int x = getHierarchyLevelsToHub(nextHub, obj2, 0, maxLevelsToCheck);
			if (x > 0) {
				return nextHub;
			}

			OAObject objMaster = nextHub.getMasterObject();
			Hub<?> h = findCommonHierarchyHub(objMaster, obj2, currentLevel + 1, maxLevelsToCheck);
			if (h != null) {
				return h;
			}
		}
		return null;
	}

	/**
	 * Determines how many parent-hierarchy levels separate the supplied
	 * OAObject from the specified Hub. The method walks upward through
	 * the object's parent chain up to the maximum number of levels and
	 * returns the number of levels required to reach the target Hub.
	 * Returns -1 if the Hub is not found within the allowed depth.
	 *
	 * @param findHub          the Hub being searched for
	 * @param fromObj          the starting object
	 * @param maxLevelsToCheck the maximum number of parent levels to traverse
	 * @return the number of levels to reach the Hub, or -1 if not found
	 */
	public int getHierarchyLevelsToHub(Hub<?> findHub, OAObject fromObj, int maxLevelsToCheck) {
		return getHierarchyLevelsToHub(findHub, fromObj, 0, maxLevelsToCheck);
	}

	/**
	 * Recursive helper that determines how many hierarchy levels separate
	 * the supplied OAObject from the target Hub. The search walks upward
	 * through the object's parent chain, incrementing the current recursion
	 * level until the Hub is found or the maximum depth is reached.
	 *
	 * @param findHub          the Hub being searched for
	 * @param fromObj          the starting object
	 * @param currentLevel     the current recursion depth
	 * @param maxLevelsToCheck the maximum number of levels allowed
	 * @return the number of levels to reach the Hub, or -1 if not found
	 */
	public int getHierarchyLevelsToHub(Hub<?> findHub, OAObject fromObj, int currentLevel, int maxLevelsToCheck) {
		if (findHub == null || fromObj == null) {
			return -1;
		}
		if (currentLevel >= maxLevelsToCheck) {
			return -1;
		}

		Hub[] hubs = callHubGetHubReferences(fromObj);
		for (int i = 0; hubs != null && i < hubs.length; i++) {
			Hub<?> hub = hubs[i];
			if (hub == null) {
				continue;
			}
			if (hub == findHub) {
				return currentLevel;
			}

			OAObject nextObj = hub.getMasterObject();
			int x = getHierarchyLevelsToHub(findHub, nextObj, currentLevel + 1, maxLevelsToCheck);
			if (x > 0) {
				return x;
			}
		}
		return -1;
	}

	/**
	 * Determines the property path from the master object of the parent Hub
	 * to the master object of the child Hub. Traverses the relationship
	 * between the two Hubs and returns the property name used to navigate
	 * from the parent to the child. Returns null when no direct path exists.
	 *
	 * @param hubParent the parent Hub
	 * @param hubChild  the child Hub
	 * @return the property path from parent to child, or null if none exists
	 */
	private String getPropertyPathFromMaster(final Hub<?> hubParent, final Hub<?> hubChild) {
		if (hubParent == null) {
			return null;
		}
		if (hubChild == null) {
			return null;
		}
		String pathFromParent = null;

		boolean b = false;
		if (callHubLinkGetLinkedOnPos(hubChild, true)) {
			//String s = HubLinkDelegate.getLinkToProperty(hubChild, true);
			b = true;
		}
		String fromProp = callHubLinkGetLinkFromProperty(hubChild, true);
		if (fromProp != null) {
			b = true;
			//return fromProp;
		}

		// see if there is a link path
		pathFromParent = null;
		Hub<?> h = hubChild;
		for (; !b;) {
			Hub<?> hx = callHubLinkGetLinkToHub(h, true);
			if (hx == null) {
				pathFromParent = null;
				break;
			}

			if (pathFromParent == null) {
				pathFromParent = callHubLinkGetLinkHubPath(h, true);
			} else {
				pathFromParent = callHubLinkGetLinkHubPath(h, true) + "." + pathFromParent;
			}

			if (hx == hubParent) {
				return pathFromParent;
			}
			if (callHubShareIsUsingSameSharedAO(hubParent, hx, true)) {
				return pathFromParent;
			}
			if (hubParent.getMasterHub() == null) { // 20131109 could be a hub copy
				if (hx.getObjectClass().equals(hubParent.getObjectClass())) {
					return pathFromParent;
				}
			}
			h = hx;
		}
		// see if if there is a detail path using masterHub
		h = hubChild;
		for (;;) {
			Hub<?> hx = h.getMasterHub();
			if (hx == null) {
				return null;
			}
			if (pathFromParent == null) {
				pathFromParent = callHubDetailGetPropertyFromMasterToDetail(h);
			} else {
				pathFromParent = callHubDetailGetPropertyFromMasterToDetail(h) + "." + pathFromParent;
			}

			if (hx == hubParent) {
				return pathFromParent;
			}
			if (callHubShareIsUsingSameSharedAO(hubParent, hx, true)) {
				return pathFromParent;
			}
			if (hubParent.getMasterHub() == null) { // 20131109 could be a hub copy
				if (hx.getObjectClass().equals(hubParent.getObjectClass())) {
					return pathFromParent;
				}
			}
			h = hx;
		}
	}

	/**
	 * Determines the property path from the supplied parent OAObject to the
	 * master object of the given child Hub. Traverses the links from the
	 * parent object to identify which property leads to the Hub. Returns
	 * null if no direct relationship path exists.
	 *
	 * @param objParent the parent OAObject
	 * @param hubChild  the child Hub
	 * @return the property path from the parent to the Hub, or null if none exists
	 */
	public String getPropertyPathFromMaster(final OAObject objParent, final Hub<?> hubChild) {
		if (objParent == null) {
			return null;
		}
		if (hubChild == null) {
			return null;
		}
		String pathFromParent = null;
		final Class<? extends OAObject> parentClass = objParent.getClass();

		boolean b = false;
		if (callHubLinkGetLinkedOnPos(hubChild, true)) {
			//String s = HubLinkDelegate.getLinkToProperty(hubChild, true);
			b = true;
		}
		String fromProp = callHubLinkGetLinkFromProperty(hubChild, true);
		if (fromProp != null) {
			b = true;
			//return fromProp;
		}

		// see if there is a link path
		pathFromParent = null;
		Hub<?> h = hubChild;
		for (; !b;) {
			Hub<?> hx = callHubLinkGetLinkToHub(h, true);
			if (hx == null) {
				pathFromParent = null;
				break;
			}

			if (pathFromParent == null) {
				pathFromParent = callHubLinkGetLinkHubPath(h, true);
			} else {
				pathFromParent = callHubLinkGetLinkHubPath(h, true) + "." + pathFromParent;
			}

			if (parentClass.equals(hx.getObjectClass())) {
				return pathFromParent;
			}
			h = hx;
		}

		// see if if there is a detail path using masterHub
		h = hubChild;
		for (;;) {
			Hub<?> hx = h.getMasterHub();
			if (hx == null) {
				return null;
			}
			if (pathFromParent == null) {
				pathFromParent = callHubDetailGetPropertyFromMasterToDetail(h);
			} else {
				pathFromParent = callHubDetailGetPropertyFromMasterToDetail(h) + "." + pathFromParent;
			}
			if (parentClass.equals(hx.getObjectClass())) {
				return pathFromParent;
			}
			h = hx;
		}
	}

	/**
	 * Determines the object that should be displayed in the child Hub when
	 * navigating from the supplied parent Hub. Uses the given source object
	 * and the relationship between the two Hubs to locate the appropriate
	 * referenced object. Returns null when no matching object can be found.
	 *
	 * @param hubFrom    the parent Hub
	 * @param fromObject the object from which navigation begins
	 * @param hubChild   the child Hub whose display object is needed
	 * @return the object to display in the child Hub, or null if none applies
	 */
	public Object getObjectToDisplay(final Hub<?> hubFrom, Object fromObject, final Hub<?> hubChild) {
		if (hubFrom == null) {
			return null;
		}
		if (hubChild == null) {
			return null;
		}
		if (fromObject == null) {
			return null;
		}

		if (!callHubLinkGetLinkedOnPos(hubChild, true)) {
			return fromObject;
		}

		Hub<?> hubPosValue = callHubLinkGetLinkToHub(hubChild, false);
		if (hubPosValue == null) {
			return fromObject;
		}

		// see if there is a link path
		String pathFromParent = null;
		Hub<?> h = hubPosValue;
		for (;;) {
			Hub<?> hx = callHubLinkGetLinkToHub(h, true);
			if (hx == null) {
				pathFromParent = null;
				break;
			}

			if (pathFromParent == null) {
				pathFromParent = callHubLinkGetLinkHubPath(h, true);
			} else {
				pathFromParent = callHubLinkGetLinkHubPath(h, true) + "." + pathFromParent;
			}

			if (hx == hubFrom) {
				break;
			}
			if (callHubShareIsUsingSameSharedAO(hubFrom, hx, true)) {
				break;
			}
			if (hubFrom.getMasterHub() == null) { // 20131109 could be a hub copy
				if (hx.getObjectClass().equals(hubFrom.getObjectClass())) {
					break;
				}
			}
			h = hx;
		}

		if (pathFromParent != null && fromObject instanceof OAObject) {
			Object objx = getProperty((OAObject) fromObject, pathFromParent);
			if (objx == null) {
				return fromObject;
			}
			fromObject = objx;
		}
		if (!(fromObject instanceof OAObject)) {
			return fromObject;
		}

		String fromProp = callHubLinkGetLinkToProperty(hubChild);
		if (fromProp == null) {
			return fromObject;
		}

		Object objx = getProperty((OAObject) fromObject, fromProp);
		int x = OAConv.toInt(objx);
		return hubChild.getAt(x);
	}

	/**
	 * Determines the full property path that links the parent Hub to the
	 * child Hub. Examines the relationship between the master objects of
	 * the two Hubs and returns the property name or dotted path that
	 * connects them. Returns null if no direct relationship path exists.
	 *
	 * @param hubParent the parent Hub
	 * @param hubChild  the child Hub
	 * @return the property path between the two Hubs, or null if none exists
	 */
	public String getPropertyPathBetweenHubs(final Hub<?> hubParent, final Hub<?> hubChild) {
		return getPropertyPathBetweenHubs(null, hubParent, hubChild, true);
	}

	/**
	 * Recursive helper that builds the property path connecting the parent
	 * Hub to the child Hub. Traverses link relationships beginning at the
	 * supplied property path prefix. When bCheckLink is true, direct link
	 * matches are evaluated before continuing deeper through related Hubs.
	 * Returns null if no connecting path can be found.
	 *
	 * @param propPath   the current property path prefix
	 * @param hubParent  the parent Hub
	 * @param hubChild   the child Hub
	 * @param bCheckLink true to check direct link relationships first
	 * @return the completed property path, or null if none exists
	 */
	private String getPropertyPathBetweenHubs(final String propPath, final Hub<?> hubParent, final Hub<?> hubChild, boolean bCheckLink) {
		if (hubChild == hubParent) {
			return null;
		}
		if (hubChild == null || hubParent == null) {
			return null;
		}

		if (callHubShareIsUsingSameSharedHub(hubParent, hubChild)) {
			return null;
		}

		Hub<?> hx;
		if (bCheckLink) {
			hx = callHubLinkGetLinkToHub(hubChild, true);
			if (hx != null) {
				boolean b = callHubLinkGetLinkedOnPos(hubChild, true);
				String s;
				if (!b) {
					s = callHubLinkGetLinkHubPath(hubChild, true);
					if (propPath != null) {
						s = propPath + "." + s;
					}
				} else {
					s = null;
				}

				if (hx == hubParent) {
					return s;
				}
				if (callHubShareIsUsingSameSharedAO(hubParent, hx, true)) {
					return s;
				}
				s = getPropertyPathBetweenHubs(s, hubParent, hx, true);
				if (s != null) {
					return s;
				}
			}
		}

		hx = hubChild.getMasterHub();
		if (hx == null) {
			return null;
		}

		// links must be type=one from master to detail.
		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hubChild);
		if (li == null) {
			return null;
		}
		li = callInfoGetReverseLinkInfo(li);
		if (li == null) {
			return null;
		}
		if (li.getType() != OALinkInfo.ONE) {
			return null;
		}

		String pathFromParent = callHubDetailGetPropertyFromMasterToDetail(hubChild);
		if (pathFromParent == null) {
			return null;
		}
		if (propPath != null) {
			pathFromParent = pathFromParent + "." + propPath;
		}

		if (hx == hubParent) {
			return pathFromParent;
		}
		if (callHubShareIsUsingSameSharedAO(hubParent, hx, true)) {
			return pathFromParent;
		}
		if (hubChild.getMasterHub() == null) { // could be a hub copy
			if (hx.getObjectClass().equals(hubParent.getObjectClass())) {
				return pathFromParent;
			}
		}
		if (hx != null && hubParent.getObjectClass().equals(hx.getObjectClass())) { // 20190731
			return pathFromParent;
		}

		String sx = getPropertyPathBetweenHubs(pathFromParent, hubParent, hx, false);
		if (sx != null) {
			return sx;
		}

		return null;
	}
	
	public abstract <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok);
	public abstract OAObject callCacheAdd(OAObject obj);
	public abstract OAObject callCSCreateCopy(OAObject oaObj, String[] excludeProperties);
	public abstract boolean callCSIsServer();
	public abstract boolean callCSIsClient();
	public abstract <T extends OAObject> T callCSGetServerObject(Class<T> clazz, OAObjectKey key);
	public abstract Hub<?> getCSGetServerReferenceHub(OAObject oaObj, String linkPropertyName);
	public abstract byte[] callCSGetServerReferenceBlob(OAObject oaObj, String propertyName);
	public abstract boolean callCSLoadReferenceHubDataOnServer(Hub<?> thisHub, OASelect select);
	public abstract Object callCSGetServerReference(OAObject oaObj, String linkPropertyName);
	public abstract <T extends OAObject> T callDSGetObject(Class<T> clazz, OAObjectKey key);
	public abstract <T extends OAObject> T callDSGetObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key);
	public abstract void callEventFireBeforePropertyChange(final OAObject oaObj, final String propertyName,
			Object oldObj, final Object newObj, final boolean bLocalOnly, final boolean bSetChanged);	
	public abstract void callEventFirePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj,
			boolean bLocalOnly, boolean bSetChanged);
	public abstract UUID callGuidGetGuid(OAObject oaObj);
	public abstract Hub<?> callHubGetHub(OAObject oaObj, OALinkInfo li);
	public abstract Hub[] callHubGetHubReferences(OAObject oaObj);
	public abstract OAObjectInfo getOAObjectInfo(Class<?> clazz); 
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount);
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, final Class<?> classParam); 
	public abstract OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName);
	public abstract boolean callInfoIsPrimitiveNull(OAObject oaObj, String propertyName); 
	public abstract void callInfoSetPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull);
	public abstract OALinkInfo callInfoGetReverseLinkInfo(OALinkInfo li);
	public abstract boolean callInfoCacheHub(OALinkInfo li, final Hub<?> hub);
	public abstract OALinkInfo callInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type);
	public abstract boolean callInfoIsOne2One(OALinkInfo thisLi);
	public abstract OALinkInfo callInfoGetLinkInfo(Class<?> clazz, String propertyName);
	public abstract void callInitializeInitialize(OAObject oaObj, OAObjectInfo oi, boolean bInitializeNulls,
			boolean bInitializeWithDS, boolean bAddToCache, boolean bInitializeWithCS, boolean bSetChangedToFalse);
	public abstract OAObjectKey callKeyCreateObjectKey(final Class<? extends OAObject> c, final Object ...ids);
	public abstract OAObjectKey callKeyGetKey(OAObject oaObj); 
	public abstract boolean callKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	public abstract Object callPropertyGetProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);	
	public abstract void callPropertySetProperty(OAObject oaObj, String name, Object value);
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue);
	public abstract Object callPropertySetPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist);
	public abstract boolean callLockIsPropertyLocked(OAObject oaObj, String name);
	public abstract boolean callLockSetPropertyLock(OAObject oaObj, String name);
	public abstract void callLockReleasePropertyLock(OAObject oaObj, String name);
	public abstract boolean callLockAttemptPropertyLock(OAObject oaObj, String name);
	public abstract void callPropertySetPropertyHubIfNotSet(OAObject oaObj, String name, Object value);
	public abstract boolean callPropertyIsPropertyLoaded(OAObject oaObj, String name);
	public abstract void callPropertyUnsafeSetProperty(OAObject oaObj, String name, Object value);
	public abstract void callSiblingOnGetObjectReference(final OAObject obj, final String linkPropertyName);
	public abstract OAObjectKey[] callSiblingGetSiblings(final OAObject mainObject, final String property, final int maxAmount,
			ConcurrentHashMap<UUID, Boolean> hmIgnore);
	public abstract HubAutoMatch callHubGetAutoMatch(Hub<?> thisHub);
	public abstract HubAutoSequence callHubGetAutoSequence(Hub<?> thisHub);
	public abstract HubSortListener callHubSortGetSortListener(Hub<?> thisHub);
	public abstract void callHubSortSort(Hub<?> thisHub, String propertyPaths, boolean bAscending, Comparator comp, boolean bAlreadySortedAndLocalOnly);
	public abstract String callHubSortGetSortProperty(Hub<?> thisHub);
	public abstract boolean callHubSortGetSortAsc(Hub<?> thisHub);
	public abstract boolean callHubSortIsSorted(Hub<?> thisHub);
	public abstract OAObject callHubMasterGetMasterObject(Hub<?> hub);
	public abstract void callHubSelectLoadAllData(Hub<?> thisHub, OASelect select);
	public abstract void callHubDataResizeToFit(Hub<?> thisHub);
	public abstract String callHubDetailGetPropertyFromDetailToMaster(Hub<?> thisHub);
	public abstract boolean callHubLinkGetLinkedOnPos(final Hub<?> thisHub, boolean bIncludeCopiedHubs);
	public abstract String callHubLinkGetLinkFromProperty(final Hub<?> thisHub, boolean bIncludeCopiedHubs);
	public abstract Hub<?> callHubLinkGetLinkToHub(final Hub<?> thisHub, boolean bIncludeCopiedHubs);
	public abstract String callHubLinkGetLinkHubPath(final Hub<?> thisHub, boolean bIncludeCopiedHubs);
	public abstract boolean callHubShareIsUsingSameSharedAO(Hub<?> hub1, Hub<?> hub2, boolean bIncludeFilteredHubs);
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub);
	public abstract String callHubLinkGetLinkToProperty(Hub<?> thisHub);
	public abstract boolean callHubShareIsUsingSameSharedHub(Hub<?> hub1, Hub<?> hub2);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);
	public abstract OADataSource callDSGetDataSource(Class<?> c);
	public abstract boolean callSyncIsObjectOnServer(OAObject obj);
	public abstract boolean callThreadLocalIsLoading();
	public abstract boolean callThreadLocalIsDeleting();
	public abstract int callThreadLocalGetObjectCacheAddMode();
	public abstract boolean callThreadLocalAddSiblingHelper(OASiblingHelper sh);
	public abstract void callThreadLocalRemoveSiblingHelper(OASiblingHelper sh);
	public abstract void callThreadLocalSetSuppressCSMessages(boolean b);
	public abstract void callThreadLocalSetLoading(boolean b);
	public abstract boolean callRemoteThreadIsRemoteThread();
}

class LoadPropertyNode {
	String prop;
	LoadPropertyNode[] children;
}

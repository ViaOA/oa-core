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
package com.viaoa.object;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubLinkDelegate;
import com.viaoa.hub.HubMerger;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.hub.HubSortDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OANullObject;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

public class OAObjectReflectDelegate {

	private static Logger LOG = Logger.getLogger(OAObjectReflectDelegate.class.getName());

	/**
	 * Create a new instance of an object. If OAClient.client exists, this will create the object on the server, where the server datasource
	 * can initialize object.
	 */
	public static Object createNewObject(Class clazz) {
		Object obj = _createNewObject(clazz);
		return obj;
	}

	private static Object _createNewObject(Class clazz) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		Object obj = null;

		/**
		 * 20190205 create on client if (!oi.getLocalOnly()) { RemoteSessionInterface rc = OASyncDelegate.getRemoteSession(clazz); if (rc !=
		 * null) { obj = rc.createNewObject(clazz); return obj; } }
		 **/

		try {
			Constructor constructor = clazz.getConstructor(new Class[] {});
			obj = constructor.newInstance(new Object[] {});
		} catch (InvocationTargetException te) {
			throw new RuntimeException("OAObject.createNewObject() cant get constructor() for class " + clazz.getName() + " "
					+ te.getCause(), te);
		} catch (Exception e) {
			throw new RuntimeException("OAObject.createNewObject() cant get constructor() for class " + clazz.getName() + " " + e, e);
		}
		return obj;
	}

	public static Object getProperty(Hub hub, String propPath) {
		return getProperty(hub, null, propPath);
	}

	/**
	 * @see OAObject#getProperty(String)
	 */
	public static Object getProperty(OAObject oaObj, String propPath) {
		return getProperty(null, oaObj, propPath);
	}

	public static Object getProperty(Hub hubLast, OAObject oaObj, String propPath) {
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

	private static Object _getProperty(Hub hubLast, OAObject oaObj, String propName) {
		OAObjectInfo oi;
		if (hubLast != null) {
			oi = OAObjectInfoDelegate.getOAObjectInfo(hubLast.getObjectClass());
		} else {
			oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		}

		Method m;
		if (oi.isHubCalcInfo(propName)) {
			if (hubLast == null) {
				return null;
			}
			m = OAObjectInfoDelegate.getMethod(oi, "get" + propName, 1);
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
			m = OAObjectInfoDelegate.getMethod(oi, "get" + propName, 0);
			if (m == null) {
				m = OAObjectInfoDelegate.getMethod(oi, "is" + propName, 0);
			}
			if (m != null && (m.getModifiers() & Modifier.PRIVATE) == 0) {
				Class c = m.getReturnType();
				if (c != null && c.isPrimitive() && getPrimitiveNull(oaObj, propName)) {
					return null;
				}
				try {
					return m.invoke(oaObj, null);
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
		Object objx = OAObjectPropertyDelegate.getProperty(oaObj, propName, false, true);
		return objx;
	}

	/**
	 * @see OAObject#setProperty(String, Object, String) This can also be used to add Objects (or ObjectKeys) to a Hub. When the Hub is then
	 *      retrieved, the value will be converted to OAObject subclasses.
	 */
	public static void setProperty(final OAObject oaObj, String propName, Object value, final String fmt) {
		if (oaObj == null || propName == null || propName.length() == 0) {
			LOG.log(Level.WARNING, "property is invalid, =" + propName, new Exception());
			return;
		}

		// 20120822 add support for propertyPath
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

		final boolean bIsLoading = OAThreadLocalDelegate.isLoading();

		String propNameU = propName.toUpperCase();
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		Method m = null;
		if (value != null) {
			m = OAObjectInfoDelegate.getMethod(oi, "SET" + propNameU, value.getClass());
		}
		if (m == null) {
			m = OAObjectInfoDelegate.getMethod(oi, "SET" + propNameU, 1);
		}

		Class clazz = null;
		if (m != null) {
			clazz = m.getParameterTypes()[0];
		}

		Object previousValue = null;

		if (clazz == null) {
			// See if this is for a Hub.  OAXMLReader uses setProperty to set MANY references using Object Id value
			m = OAObjectInfoDelegate.getMethod(oi, "GET" + propNameU, 0);
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
				OAObjectEventDelegate.fireBeforePropertyChange(oaObj, propName, previousValue, value, oi.getLocalOnly(), true);
			}
			OAObjectPropertyDelegate.setProperty(oaObj, propName, value);
			if (!bIsLoading) {
				OAObjectEventDelegate.firePropertyChange(oaObj, propName, previousValue, value, oi.getLocalOnly(), true);
			}
			return;
		}

		if (value instanceof OANullObject) {
			value = null;
		}
		OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propNameU);

		if (li != null) {
			if (bIsLoading) {
				if (value == null) {
					// 20110315 allow null to be set
					OAObjectPropertyDelegate.setProperty(oaObj, propName, value);
					//was: OAObjectPropertyDelegate.removeProperty(oaObj, propName, true);
				} else {
					if (!(value instanceof OAObject) && !(value instanceof OAObjectKey)) {
						value = OAObjectKeyDelegate.convertToObjectKey(oi, value);
					}
					OAObjectPropertyDelegate.setProperty(oaObj, propName, value);
				}
				return;
			}
			previousValue = OAObjectPropertyDelegate.getProperty(oaObj, propName, false, true); // get previous value
		}

		boolean bPrimitiveNull = false; // a primitive type that needs to be set to null value
		if (li == null) {
			if (value == null && clazz.isPrimitive()) {
				bPrimitiveNull = true;
			} else {
				if (value != null || !clazz.equals(String.class)) { // conversion will convert a null to a String "" (blank)
					value = OAConverter.convert(clazz, value, fmt); // convert to right type of class value
				}
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
				OAObjectKey k = OAObjectKeyDelegate.getKey((OAObject) value);
				if (k.equals(previousValue)) {
					OAObjectPropertyDelegate.setProperty(oaObj, propName, value);
					return; // no change, was storing key, now storing oaObject
				}
			}
		} else { //  (value NOT instanceof OAObject) either OAObjectKey or value of key
			if (!(value instanceof OAObjectKey)) {
				value = OAObjectKeyDelegate.convertToObjectKey(oi, value);
			}
			if (value.equals(previousValue)) {
				return; // no change
			}
			if (previousValue instanceof OAObject) {
				OAObjectKey k = OAObjectKeyDelegate.getKey((OAObject) previousValue);
				if (k.equals(value)) {
					return; // no change
				}
			}

			// have to get the real object
			Object findValue = getObject(li.toClass, value);
			if (findValue == null) {
				throw new RuntimeException("Cant find object for Id: " + value + ", class=" + li.toClass);
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
				OAObjectEventDelegate.firePropertyChange(oaObj, propName, previousValue, null, oi.getLocalOnly(), true); // setting to null
			}
		}
	}

	/**
	 * used for "quick" storing/loading objects
	 */
	public static void storeLinkValue(OAObject oaObj, String propertyName, Object value) {
		if (!(value instanceof OAObject) && !(value instanceof OAObjectKey)) {
			OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
			OALinkInfo li = oi.getLinkInfo(propertyName);
			if (li != null && li.getType() == li.ONE) {
				oi = li.getToObjectInfo();
				value = OAObjectKeyDelegate.convertToObjectKey(oi, value);
			}
		}
		OAObjectPropertyDelegate.setProperty(oaObj, propertyName, value);
	}

	/* Used to flag primitive property as having a null value. */
	public static boolean getPrimitiveNull(OAObject oaObj, String propertyName) {
		if (oaObj == null || propertyName == null) {
			return false;
		}
		if (oaObj.nulls == null || oaObj.nulls.length == 0) {
			return false;
		}
		synchronized (oaObj) {
			if (oaObj.nulls == null || oaObj.nulls.length == 0) {
				return false;
			}
			return OAObjectInfoDelegate.isPrimitiveNull(oaObj, propertyName);
		}
	}

	// note: a primitive null can only be set by calling OAObjectReflectDelegate.setProperty(...)
	protected static void setPrimitiveNull(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return;
		}
		synchronized (oaObj) {
			OAObjectInfoDelegate.setPrimitiveNull(oaObj, propertyName, true);
		}
	}

	// note: a primitive null can only be removed by OAObjectEventDelegate.firePropertyChagnge(...)
	protected static void removePrimitiveNull(OAObject oaObj, String propertyName) {
		if (oaObj.nulls == null || oaObj.nulls.length == 0) {
			return;
		}
		if (propertyName == null) {
			return;
		}
		synchronized (oaObj) {
			OAObjectInfoDelegate.setPrimitiveNull(oaObj, propertyName, false);
		}
	}

	// called by setProperty() when property is a Hub.
	private static void setHubProperty(OAObject oaObj, String propName, String propNameU, Object value, OAObjectInfo oi, String fmt) {
		// this is for a Hub.  OAXMLReader uses setProperty to set MANY references using Object Id value for objects
		if (value == null) {
			return;
		}

		Hub hub;
		Object objOrig = OAObjectPropertyDelegate.getProperty(oaObj, propName, false, true);

		if (value instanceof Hub) {
			OAObjectPropertyDelegate.setPropertyCAS(oaObj, propName, value, objOrig);
			return;
		}

		OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propNameU);
		if (li == null) {
			return;
		}

		if (objOrig != null) {
			if (!(objOrig instanceof Hub)) {
				throw new RuntimeException("stored object for " + propName + " is not a hub");
			}
			hub = (Hub) objOrig;
		} else {
			hub = new Hub(OAObjectKey.class);
			OAObjectPropertyDelegate.setProperty(oaObj, propName, hub);
		}

		Class c = hub.getObjectClass();
		boolean bKeyOnly = (c.equals(OAObjectKey.class));

		if (!(value instanceof OAObject)) {
			if (!(value instanceof OAObjectKey)) { // convert to OAObjectKey
				if (value instanceof Hub) {
					throw new RuntimeException("cant not set the Hub for " + propName);
				}
				value = OAObjectKeyDelegate.convertToObjectKey(li.toClass, value);
			}
		}

		if (bKeyOnly) {
			if (value instanceof OAObject) {
				value = OAObjectKeyDelegate.getKey((OAObject) value);
			}
		} else {
			if (value instanceof OAObjectKey) {
				value = OAObjectReflectDelegate.getObject(c, value);
			}
		}
		if (value != null && hub.getObject(value) == null) {
			hub.add(value);
		}
	}

	/**
	 * DataSource independent method to retrieve an object. Find the OAObject given a key value. This will look in the Cache, Server (if
	 * running as workstation) and the DataSource (if not running as workstation).
	 *
	 * @param clazz class of reference of to find.
	 * @param key   can be the value of the key or an OAObjectKey
	 */
	public static OAObject getObject(Class clazz, Object key) {
		if (clazz == null || key == null) {
			return null;
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		return getObject(clazz, key, oi);
	}

	public static OAObject getObject(Class clazz, Object key, OAObjectInfo oi) {
		if (clazz == null || key == null) {
			return null;
		}

		if (!(key instanceof OAObjectKey)) {
			key = OAObjectKeyDelegate.convertToObjectKey(clazz, key);
		}

		OAObject oaObj = (OAObject) OAObjectCacheDelegate.get(clazz, (OAObjectKey) key);
		if (oaObj == null) {
			if (OASync.isClient(clazz) && (oi == null || !oi.getLocalOnly())) {
				oaObj = (OAObject) OAObjectCSDelegate.getServerObject(clazz, (OAObjectKey) key);
			} else {
				oaObj = (OAObject) OAObjectDSDelegate.getObject(clazz, (OAObjectKey) key);
			}
		}
		return oaObj;
	}

	/**
	 * DataSource independent method to retrieve a reference property that is a Hub Collection.
	 *
	 * @param linkPropertyName name of property to retrieve. (case insensitive)
	 * @param sortOrder
	 * @param bSequence        if true, then create a hub sequencer to manage the order of the objects in the hub.
	 */
	public static Hub getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence,
			Hub hubMatch) {
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
		OASiblingHelperDelegate.onGetObjectReference(oaObj, linkPropertyName);

		Hub hub = null;
		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		final OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(oi, linkPropertyName);

		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, false, true);

		if (obj instanceof Hub) {
			// 20141215 could be server side, that deserialized the object+references without setting up.
			hub = (Hub) obj;

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

			if (OASync.isServer(oaObj)) {
				// 20150130 the same thread that is loading it could be accessing it again. (ex: matching and hubmerger during getReferenceHub(..))
				if (OAObjectPropertyDelegate.isPropertyLocked(oaObj, linkPropertyName)) {
					return hub;
				}

				// check to see if there needs to be an autoMatch set up
				if (HubDelegate.getAutoMatch(hub) == null) {
					if (linkInfo != null) {
						String matchProperty = linkInfo.getMatchProperty();
						if (matchProperty != null && matchProperty.length() > 0) {
							if (hubMatch == null) {
								String matchHubPropPath = linkInfo.getMatchHub();
								if (matchHubPropPath != null && matchHubPropPath.length() > 0) {
									OAObjectInfo oix = OAObjectInfoDelegate.getOAObjectInfo(linkInfo.getToClass());
									OALinkInfo linkInfox = OAObjectInfoDelegate.getLinkInfo(oix, matchProperty);
									if (linkInfox != null) {
										if (!OAThreadLocalDelegate.isDeleting()) {
											hubMatch = new Hub(linkInfox.getToClass());
											HubMerger hm = new HubMerger(oaObj, hubMatch, matchHubPropPath);
											hm.setServerSideOnly(true);
										}
									}
								}
							}
							if (hubMatch != null) {
								hub.setAutoMatch(matchProperty, hubMatch, true); // serverSide only
							}
						}
					}
				}

				if (bSequence) {
					if (HubDelegate.getAutoSequence(hub) == null) {
						hub.setAutoSequence(seqProperty, 0, false); // server will keep autoSequence property updated - clients dont need autoSeq (server side managed)
					}
				} else if (OAString.notEmpty(sortOrder) && HubSortDelegate.getSortListener(hub) == null) {
					// keep the hub sorted on server only
					HubSortDelegate.sort(hub, sortOrder, bSortAsc, null, true);// dont sort, or send out sort msg (since no other client has this hub yet)
				}
			} else {
				// client might need a sort listener
				if (!bSequence) {
					boolean bAsc = true;
					String s = HubSortDelegate.getSortProperty(hub); // use sort order from orig hub
					if (OAString.isEmpty(s)) {
						s = sortOrder;
					} else {
						bAsc = HubSortDelegate.getSortAsc(hub);
					}
					if (OAString.isNotEmpty(s) && !HubSortDelegate.isSorted(hub)) {
						// client recvd hub that has sorted property, without sortListener, etc.
						// note: serialized hubs do not have sortListener created - must be manually done
						//      this is done here (after checking first), for cases where references are serialized in a CS call.
						//      - or during below, when it is directly called.
						HubSortDelegate.sort(hub, s, bAsc, null, true);// dont sort, or send out sort msg
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
				b = OAObjectPropertyDelegate.setPropertyLock(oaObj, linkPropertyName);

				obj = OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, false, true);
				if (obj instanceof Hub) {
					return (Hub) obj;
				}

				hub = _getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch, oi, linkInfo);
			} finally {
				if (b) {
					OAObjectPropertyDelegate.releasePropertyLock(oaObj, linkPropertyName);
				}
			}
		}

		// 20160811 check to see if hub uses a pp
		if (OAString.notEmpty(linkInfo.getMergerPropertyPath())) {
			String spp = linkInfo.getMergerPropertyPath();
			new HubMerger(oaObj, hub, spp);
		}

		return hub;
	}

	// keeps track of siblings that are "in flight"
	private static final ConcurrentHashMap<Integer, Boolean> hmIgnoreSibling = new ConcurrentHashMap<>();

	private static Hub _getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder,
			boolean bSequence, Hub hubMatch, final OAObjectInfo oi, final OALinkInfo linkInfo) {

		Object propertyValue = OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, true, true);
		final boolean bThisIsServer = OAObjectCSDelegate.isServer(oaObj);
		// dont get calcs from server, calcs are maintained locally, events are not sent
		boolean bIsCalc = (linkInfo != null && linkInfo.bCalculated);
		boolean bIsServerSideCalc = (linkInfo != null && linkInfo.bServerSideCalc);

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
				hub = new Hub(linkInfo.toClass);
				OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, hub);
				return hub;
			}
			// create an empty hub
			hub = new Hub(linkInfo.toClass, oaObj, OAObjectInfoDelegate.getReverseLinkInfo(linkInfo), false);
		} else if (propertyValue == OANotExist.instance) {
			propertyValue = null;
		}

		if (propertyValue instanceof Hub) {
			hub = (Hub) propertyValue;
			Class c = hub.getObjectClass();
			if (!OAObjectKey.class.equals(c)) {
				if (!bThisIsServer) {
					boolean bAsc = true;
					String s = HubSortDelegate.getSortProperty(hub); // use sort order from orig hub
					if (OAString.isEmpty(s)) {
						s = sortOrder;
					} else {
						bAsc = HubSortDelegate.getSortAsc(hub);
					}
					if (!bSequence && !OAString.isEmpty(s) && !HubSortDelegate.isSorted(hub)) {
						// client recvd hub that has sorted property, without sortListener, etc.
						// note: serialized hubs do not have sortListener created - must be manually done
						//      this is done here (after checking first), for cases where references are serialized in a CS call.
						//      - or during below, when it is directly called.
						HubSortDelegate.sort(hub, s, bAsc, null, true);// dont sort, or send out sort msg
						OAPropertyInfo pi = oi.getPropertyInfo(s);
						if (pi == null || String.class.equals(pi.getClassType())) {
							hub.resort(); // this will not send out event
						}
					}
				}
				if (OAObjectInfoDelegate.cacheHub(linkInfo, hub)) {
					OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, new WeakReference(hub));
				} else {
					OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, hub);
				}
				return hub;
			}

			// objects are stored as OAObjectKeys
			// Hub with OAObjectKeys exists, need to convert to "real" objects
			if (linkInfo == null) {
				return null;
			}
			Class linkClass = linkInfo.toClass;
			Hub hubNew = new Hub(linkClass, oaObj, OAObjectInfoDelegate.getReverseLinkInfo(linkInfo), false);
			try {
				OAThreadLocalDelegate.setSuppressCSMessages(true);
				for (int i = 0;; i++) {
					OAObjectKey key = (OAObjectKey) hub.elementAt(i);
					if (key == null) {
						break;
					}
					Object objx = getObject(linkClass, key);
					if (propertyValue != null) {
						hubNew.add(objx);
					}
				}
				hub = hubNew;
				hub.setChanged(false);
			} finally {
				OAThreadLocalDelegate.setSuppressCSMessages(false);
			}
			if (OAObjectInfoDelegate.cacheHub(linkInfo, hub)) {
				OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, new WeakReference(hub));
			} else {
				OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, hub);
			}
			return hub;
		}

		OASelect select = null;
		String sibIds = null;
		OAObjectKey[] siblingKeys = null;
		HashMap<OAObjectKey, Hub> hmSiblingHub = null;
		final String matchProperty = linkInfo.getMatchProperty();

		if (hub != null) {
		} else if (!bThisIsServer && !oi.getLocalOnly() && (!bIsCalc || bIsServerSideCalc)
				&& !OAObjectCSDelegate.isInNewObjectCache(oaObj)) {
			// request from server
			hub = OAObjectCSDelegate.getServerReferenceHub(oaObj, linkPropertyName);
			if (hub == null) {
				// master not on the Server, might have been GCd, create empty Hub
				if (linkInfo == null) {
					return null;
				}
				Class linkClass = linkInfo.toClass;
				hub = new Hub(linkClass, oaObj, OAObjectInfoDelegate.getReverseLinkInfo(linkInfo), false);
				// throw new RuntimeException("getHub from Server failed, this.oaObj="+oaObj+", linkPropertyName="+linkPropertyName);
			}

			if (HubDelegate.getMasterObject(hub) == null) {
				if (hub.getSize() == 0 && hub.getObjectClass() == null) {
					if (linkInfo == null) {
						return null;
					}
					Class linkClass = linkInfo.toClass;
					hub = new Hub(linkClass, oaObj, OAObjectInfoDelegate.getReverseLinkInfo(linkInfo), false);
				}
			}
		} else { // hub is null, create now
			if (linkInfo == null) {
				return null;
			}
			Class linkClass = linkInfo.toClass;
			OALinkInfo liReverse = OAObjectInfoDelegate.getReverseLinkInfo(linkInfo);
			if (liReverse != null) {

				// 20141109
				hub = new Hub(linkClass, oaObj, liReverse, false);

				if (!bIsCalc && bThisIsServer) {
					// 20171225 support for selecting siblings at same time
					OALinkInfo rli = linkInfo.getReverseLinkInfo();
					if (!bThisIsServer || linkInfo.getRecursive() || rli == null || rli.getType() == OALinkInfo.TYPE_MANY
							|| rli.getPrivateMethod() || (hubMatch != null) || (matchProperty != null && matchProperty.length() > 0)) {
						// not supported in this release
						siblingKeys = null;
					} else {
						int x;
						if (linkInfo.getCouldBeLarge()) {
							x = 4;
						} else {
							x = 25;
						}
						if (OAThreadLocalDelegate.isDeleting()) {
							siblingKeys = null;
						} else {
							siblingKeys = OASiblingHelperDelegate.getSiblings(oaObj, linkPropertyName, x, hmIgnoreSibling);
						}
					}

					if (siblingKeys != null) {
						hmSiblingHub = new HashMap<>();
						boolean bChecked = false;
						for (OAObjectKey keyx : siblingKeys) {
							Object[] idsx = keyx.getObjectIds();
							if (idsx == null || idsx.length != 1 || idsx[0] == null) {
								continue;
							}
							// 20211209
							if (!bChecked) {
								bChecked = true;
								Class c = idsx[0].getClass();
								if (!c.isPrimitive() && !c.equals(String.class)) {
									break;
								}
							}

							OAObject objx = OAObjectCacheDelegate.get(oaObj.getClass(), keyx);
							if (objx == null) {
								continue;
							}
							if (!OAObjectPropertyDelegate.attemptPropertyLock(objx, linkPropertyName)) {
								continue;
							}
							hmSiblingHub.put(keyx, new Hub(linkClass, objx, liReverse, false));

							if (sibIds == null) {
								sibIds = "";
							} else {
								sibIds += ", ";
							}
							sibIds += "" + idsx[0];
						}
						if (sibIds != null) {
							Object[] idsx = oaObj.getObjectKey().getObjectIds();
							if (idsx == null || idsx.length != 1 || idsx[0] == null) {
								sibIds = null;
								hmSiblingHub = null;
							} else {
								sibIds = idsx[0] + "," + sibIds;
							}
						}
					}

					select = new OASelect(hub.getObjectClass());
					if (sibIds != null) {
						select.setWhere(rli.getName() + " IN (" + sibIds + ")");
					} else {
						if (bThisIsServer) {
							select.setWhereObject(oaObj);
							select.setPropertyFromWhereObject(linkInfo.getName());
						}
					}

					/* was:
					select = new OASelect(hub.getObjectClass());
					if (oaObj != null) {
					    select.setWhereObject(oaObj);
					    select.setPropertyFromWhereObject(linkInfo.getName());
					}
					*/
				}

				//was: hub = new Hub(linkClass, oaObj, liReverse, true); // liReverse = liDetailToMaster
				/* 2013/01/08 recursive if this object is the owner (or ONE to Many) and the select
				 * hub is recursive of a different class - need to only select root objects. All
				 * children (recursive) hubs will automatically be assigned the same owner as the
				 * root hub when owner is changed/assigned. */
				/*
				 * 20130919 recurse does not have to be owner */
				//was: if (!OAObjectInfoDelegate.isMany2Many(linkInfo) && (bThisIsServer || bIsCalc) && linkInfo.isOwner()) {

				// 20131009 new LinkProperty recursive flag.  If owned+recursive, then select root
				if (bThisIsServer && !bIsCalc) {
					if (linkInfo.getOwner() && linkInfo.getRecursive()) {
						OAObjectInfo oi2 = OAObjectInfoDelegate.getOAObjectInfo(linkInfo.getToClass());
						OALinkInfo li2 = OAObjectInfoDelegate.getRecursiveLinkInfo(oi2, OALinkInfo.ONE);
						if (li2 != null) {
							OALinkInfo li3 = OAObjectInfoDelegate.getReverseLinkInfo(li2);
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
				if (!OAObjectInfoDelegate.isMany2Many(linkInfo) && (bThisIsServer || bIsCalc)) {
				    OAObjectInfo oi2 = OAObjectInfoDelegate.getOAObjectInfo(linkInfo.getToClass());
				    OALinkInfo li2 = OAObjectInfoDelegate.getRecursiveLinkInfo(oi2, OALinkInfo.ONE);
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
		    if (bThisIsServer || OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, false, false) == null) {
		        // set property
		        if (OAObjectInfoDelegate.cacheHub(linkInfo, hub)) {
		            OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, new WeakReference(hub));
		        }
		        else {
		            OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, hub);
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
			if (sibIds != null) {
				OALinkInfo rli = linkInfo.getReverseLinkInfo();
				try {
					OAThreadLocalDelegate.setSuppressCSMessages(true);
					OAThreadLocalDelegate.setLoading(true);
					for (; select.hasMore();) {
						OAObject objx = select.next();
						// find masterObj to put it in
						Object valx = OAObjectPropertyDelegate.getProperty(objx, rli.getName(), false, false);
						if (valx instanceof OAObject) {
							valx = ((OAObject) valx).getObjectKey();
						}
						if (!(valx instanceof OAObjectKey)) {
							continue;
						}
						OAObjectKey okx = (OAObjectKey) valx;
						if (okx.equals(oaObj.getObjectKey())) {
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
					OAThreadLocalDelegate.setLoading(false);
					OAThreadLocalDelegate.setSuppressCSMessages(false);
				}
			} else {
				if (!OAObjectCSDelegate.loadReferenceHubDataOnServer(hub, select)) { // load all data before passing to client
					HubSelectDelegate.loadAllData(hub, select);
				}
			}

			hub.cancelSelect();
			if (select != null) {
				select.cancel();
				HubDataDelegate.resizeToFit(hub);
			}

			if (bThisIsServer) {
				if (bSequence) {
					if (HubDelegate.getAutoSequence(hub) == null) {
						hub.setAutoSequence(seqProperty); // server will keep autoSequence property updated - clients dont need autoSeq (server side managed)
						if (hmSiblingHub != null) {
							// need to loop thru and set Hubs for siblings
							for (Entry<OAObjectKey, Hub> entry : hmSiblingHub.entrySet()) {
								Hub hx = entry.getValue();
								hx.setAutoSequence(seqProperty, 0, false); // server will keep autoSequence property updated - clients dont need autoSeq (server side managed)
							}
						}
					}
				} else if (OAString.notEmpty(sortOrder) && HubSortDelegate.getSortListener(hub) == null) {
					// keep the hub sorted on server only
					HubSortDelegate.sort(hub, sortOrder, bSortAsc, null, true);// dont sort, or send out sort msg (since no other client has this hub yet)
					final OAPropertyInfo pi = oi.getPropertyInfo(sortOrder);
					if (pi == null || String.class.equals(pi.getClassType())) {
						hub.resort(); // 20180613 dont trust db sorting, this will not send out event
					}

					if (hmSiblingHub != null) {
						// need to loop thru and set Hubs for siblings
						for (Entry<OAObjectKey, Hub> entry : hmSiblingHub.entrySet()) {
							Hub hx = entry.getValue();
							HubSortDelegate.sort(hx, sortOrder, bSortAsc, null, true);
							if (pi == null || String.class.equals(pi.getClassType())) {
								hub.resort(); // 20180613 dont trust db sorting, this will not send out event
							}
						}
					}
				}
			}

			// 20110505 autoMatch propertyPath
			if (matchProperty != null && matchProperty.length() > 0) {
				if (hubMatch == null) {
					String matchHubProperty = linkInfo.getMatchHub();
					if (matchHubProperty != null && matchHubProperty.length() > 0) {
						OAObjectInfo oix = OAObjectInfoDelegate.getOAObjectInfo(linkInfo.getToClass());
						OALinkInfo linkInfox = OAObjectInfoDelegate.getLinkInfo(oix, matchProperty);
						if (linkInfox != null) {
							hubMatch = new Hub(linkInfox.getToClass());
							HubMerger hm = new HubMerger(oaObj, hubMatch, matchHubProperty);
							hm.setServerSideOnly(true);
						}
					}
				}

				/**
				 * 20171113 moved after hub is added if (hubMatch != null) { hub.setAutoMatch(matchProperty, hubMatch, true); }
				 */
			}
		} else {
			if (!bSequence) {
				// create sorter for client
				boolean bAsc = true;
				String s = HubSortDelegate.getSortProperty(hub); // use sort order from orig hub
				if (OAString.isEmpty(s)) {
					s = sortOrder;
				} else {
					bAsc = HubSortDelegate.getSortAsc(hub);
				}
				if (!OAString.isEmpty(s)) {
					HubSortDelegate.sort(hub, s, bAsc, null, true);// dont sort, or send out sort msg (since no other client has this hub yet)
				}
			}
		}

		// 20171108 moved here from above
		if (bThisIsServer || OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, false, false) == null) {
			// set property
			if (OAObjectInfoDelegate.cacheHub(linkInfo, hub)) {
				OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, new WeakReference(hub));
			} else {
				OAObjectPropertyDelegate.setProperty(oaObj, linkPropertyName, hub);
			}
		}
		// 20171113 moved from above
		if (hubMatch != null && (bThisIsServer || (bIsCalc && !bIsServerSideCalc))) {
			if (matchProperty != null && matchProperty.length() > 0) {
				hub.setAutoMatch(matchProperty, hubMatch, true);
			}
		}

		if (hmSiblingHub != null) {
			// need to loop thru and set Hubs for siblings
			for (Entry<OAObjectKey, Hub> entry : hmSiblingHub.entrySet()) {
				OAObjectKey ok = entry.getKey();
				OAObject obj = OAObjectCacheDelegate.get(oaObj.getClass(), ok);
				if (obj == null) {
					continue;
				}
				Hub hx = entry.getValue();
				if (OAObjectInfoDelegate.cacheHub(linkInfo, hx)) {
					OAObjectPropertyDelegate.setPropertyHubIfNotSet(obj, linkPropertyName, new WeakReference(hx));
				} else {
					OAObjectPropertyDelegate.setPropertyHubIfNotSet(obj, linkPropertyName, hx);
				}
				OAObjectPropertyDelegate.releasePropertyLock(obj, linkPropertyName);
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
	 * This method is used to get the value of a relationship. Calling it will not load objects. To load objects, call getReferenceHub(name)
	 * or getReferenceObject(name)
	 *
	 * @return one of the following: null, OAObjectKey, OAObject, Hub of OAObjects, Hub of OAObjectKeys
	 * @see #getReferenceObject to have the OAObject returned.
	 * @see #getReferenceHub to have a Hub of OAObjects returned.
	 */
	public static Object getRawReference(OAObject oaObj, String name) {
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, name, false, true);
		return obj;
	}

	// 20120616 check to see if an object has a reference holding it from being GCd.
	public static boolean hasReference(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		OAObjectInfo io = OAObjectInfoDelegate.getObjectInfo(oaObj.getClass());
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
				obj = OAObjectCacheDelegate.get(li.getToClass(), (OAObjectKey) obj);
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

	public static String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc) {
		return getUnloadedReferences(obj, bIncludeCalc, null, true);
	}

	public static String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName) {
		return getUnloadedReferences(obj, bIncludeCalc, exceptPropertyName, true);
	}

	public static String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge) {
		if (obj == null) {
			return null;
		}
		OAObjectInfo io = OAObjectInfoDelegate.getObjectInfo(obj.getClass());
		ArrayList<String> al = null;
		List<OALinkInfo> alLinkInfo = io.getLinkInfos();
		for (OALinkInfo li : alLinkInfo) {
			if (!bIncludeCalc && li.bCalculated) {
				continue;
			}
			if (li.bPrivateMethod) {
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

			Object value = OAObjectReflectDelegate.getRawReference((OAObject) obj, property);
			if (value == null) {
				if (!OAObjectPropertyDelegate.isPropertyLoaded((OAObject) obj, property)) {
					if (al == null) {
						al = new ArrayList<String>();
					}
					al.add(property);
				}
			} else if (value instanceof OAObjectKey) {
				if (OAObjectCacheDelegate.get(li.toClass, value) == null) {
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
	 * Used to load all references to an object.
	 */
	public static void loadAllReferences(OAObject obj) {
		loadAllReferences(obj, false);
	}

	public static void loadAllReferences(Hub hub) {
		loadAllReferences(hub, false);
	}

	public static void loadAllReferences(Hub hub, boolean bIncludeCalc) {
		OASiblingHelper siblingHelper = new OASiblingHelper(hub);
		OAThreadLocalDelegate.addSiblingHelper(siblingHelper);
		try {
			for (Object obj : hub) {
				if (obj instanceof OAObject) {
					loadAllReferences((OAObject) obj, bIncludeCalc);
				}
			}
		} finally {
			OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
		}
	}

	public static void loadAllReferences(OAObject obj, boolean bIncludeCalc) {
		loadReferences(obj, bIncludeCalc, 0);
	}

	public static void loadReferences(OAObject obj, boolean bIncludeCalc, int max) {
		OAObjectInfo io = OAObjectInfoDelegate.getObjectInfo(obj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		int cnt = 0;
		for (OALinkInfo li : al) {
			if (!bIncludeCalc && li.bCalculated) {
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
				Object objx = OAObjectPropertyDelegate.getProperty(obj, name, true, true);
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

	public static boolean areAllReferencesLoaded(OAObject obj, boolean bIncludeCalc) {
		if (obj == null) {
			return false;
		}
		OAObjectInfo io = OAObjectInfoDelegate.getObjectInfo(obj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		boolean bIsServer = OASyncDelegate.isServer(obj);
		for (OALinkInfo li : al) {
			if (al == null) {
				continue;
			}
			if (!bIncludeCalc && li.bCalculated) {
				continue;
			}
			if (li.bPrivateMethod) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			String name = li.getName();

			Object val = OAObjectPropertyDelegate.getProperty(obj, name, true, true);
			if (val == OANotExist.instance) {
				return false;
			}
			if (val instanceof OAObjectKey) {
				return false;
			}
			if (val instanceof Hub && bIsServer) {
				Hub hubx = (Hub) val;
				// see if autoMatch (if used) is set up
				String matchProperty = li.getMatchProperty();
				if (matchProperty != null && matchProperty.length() > 0) {
					if (HubDelegate.getAutoMatch(hubx) == null) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static int loadAllReferences(OAObject obj, boolean bOne, boolean bMany, boolean bIncludeCalc) {
		OAObjectInfo io = OAObjectInfoDelegate.getObjectInfo(obj.getClass());
		List<OALinkInfo> al = io.getLinkInfos();
		int cnt = 0;
		for (OALinkInfo li : al) {
			if (!bIncludeCalc && li.bCalculated) {
				continue;
			}
			if (li.bPrivateMethod) {
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

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, 0, true, null, null, 0);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, 0, true, null, null, 0);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, 0);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, int maxRefsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, maxRefsToLoad);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, 0);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, true, null, null, maxRefsToLoad);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, 0);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, maxRefsToLoad);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad, long maxEndTime) {
		return _loadAllReferences(	0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, maxRefsToLoad,
									maxEndTime);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, 0);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, null, maxRefsToLoad);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, 0);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback, int maxRefsToLoad) {
		return _loadAllReferences(0, obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, maxRefsToLoad);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, 0);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback, int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, null, maxRefsToLoad);
	}

	public static int loadAllReferences(final Hub hub, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade) {
		int cnt = 0;

		final OASiblingHelper siblingHelper = new OASiblingHelper(hub);
		OAThreadLocalDelegate.addSiblingHelper(siblingHelper);
		try {
			cnt = _loadAllReferences(	0, hub, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
										0);
		} finally {
			OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
		}
		return cnt;
	}

	public static int loadAllReferences(final Hub hub, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade, int maxRefsToLoad) {
		int cnt = 0;

		final OASiblingHelper siblingHelper = new OASiblingHelper(hub);
		OAThreadLocalDelegate.addSiblingHelper(siblingHelper);
		try {
			cnt = _loadAllReferences(	0, hub, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
										maxRefsToLoad);
		} finally {
			OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
		}
		return cnt;
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade, 0);
	}

	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade, int maxRefsToLoad) {
		return _loadAllReferences(0, hub, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade, maxRefsToLoad);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade) {
		return loadAllReferences(obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade);
	}

	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade, int maxRefsToLoad) {
		return loadAllReferences(obj, 0, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, null, cascade, maxRefsToLoad);
	}

	public static int loadAllReferences(OAObject obj, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade) {
		return loadAllReferences(obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade, 0);
	}

	// ** MAIN reference loader here **
	/**
	 * @param levelsLoaded                number of levels of references that have been loaded.
	 * @param maxLevelsToLoad             max levels of references to recursively load.
	 * @param additionalOwnedLevelsToLoad additional levels of owned references to load
	 * @param bIncludeCalc                include calculated links
	 * @param callback                    will be called before loading references. If the callback.updateObject returns false, then the
	 *                                    current object references will not be loaded
	 * @param cascade                     used to impl visitor pattern
	 * @param maxRefsToLoad               maximum recursive objects to call loadAllRefereces on.
	 */
	public static int loadAllReferences(OAObject obj, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade, final int maxRefsToLoad) {
		return _loadAllReferences(	0, obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade,
									maxRefsToLoad);
	}

	private static int _loadAllReferences(int currentRefsLoaded, final Hub hub, final int levelsLoaded, final int maxLevelsToLoad,
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

	private static int _loadAllReferences(int currentRefsLoaded, final OAObject obj, final int levelsLoaded, final int maxLevelsToLoad,
			final int additionalOwnedLevelsToLoad,
			final boolean bIncludeCalc, final OACallback callback, OACascade cascade, final int maxRefsToLoad) {

		return _loadAllReferences(	currentRefsLoaded, obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc,
									callback, cascade, maxRefsToLoad, 0);
	}

	private static int _loadAllReferences(int currentRefsLoaded, final OAObject obj, final int levelsLoaded, final int maxLevelsToLoad,
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

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (maxEndTime > 0 && System.currentTimeMillis() > maxEndTime) {
				break;
			}
			if (!bIncludeCalc && li.bCalculated) {
				continue;
			}
			if (li.bPrivateMethod) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			if (bOwnedOnly && !li.bOwner) {
				continue;
			}
			boolean bIsMany = li.getType() == OALinkInfo.TYPE_MANY;

			Object objx = OAObjectPropertyDelegate.getProperty(obj, li.getName(), true, true);

			if (objx instanceof OANotExist) { // not loaded from ds
				if (bIsMany) {
					currentRefsLoaded++;
				}
			} else if (objx instanceof OAObjectKey) { // not loaded from ds
				if (OAObjectCacheDelegate.get(li.getToClass(), (OAObjectKey) objx) == null) {
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
				OAThreadLocalDelegate.addSiblingHelper(siblingHelper);
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
					OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
				}
			} else if (objx instanceof OAObject) {
				currentRefsLoaded = _loadAllReferences(	currentRefsLoaded, (OAObject) objx, levelsLoaded + 1, maxLevelsToLoad,
														additionalOwnedLevelsToLoad, bIncludeCalc, callback, cascade, maxRefsToLoad,
														maxEndTime);
			}
		}
		return currentRefsLoaded;
	}

	// 20121001
	public static byte[] getReferenceBlob(OAObject oaObj, String propertyName) {
		if (oaObj == null) {
			return null;
		}
		if (propertyName == null) {
			return null;
		}

		try {
			OAObjectPropertyDelegate.setPropertyLock(oaObj, propertyName);

			Object val = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
			if (val instanceof byte[]) {
				return (byte[]) val;
			}
			if (val != OANotExist.instance) {
				return null;
			}

			if (!OASyncDelegate.isServer(oaObj)) {
				val = OAObjectCSDelegate.getServerReferenceBlob(oaObj, propertyName);
			} else {
				OADataSource ds = OADataSource.getDataSource(oaObj.getClass());
				if (ds != null) {
					val = ds.getPropertyBlobValue(oaObj, propertyName);
				}
			}

			val = OAObjectPropertyDelegate.setPropertyCAS(oaObj, propertyName, val, null, true, false);
			if (val instanceof byte[]) {
				return (byte[]) val;
			}

		} finally {
			OAObjectPropertyDelegate.releasePropertyLock(oaObj, propertyName);
		}
		return null;
	}

	/**
	 * DataSource independent method to retrieve a reference property.
	 * <p>
	 * If reference object is not already loaded, then OADataSource will be used to retrieve object.
	 */

	public static Object getReferenceObject(final OAObject oaObj, final String linkPropertyName) {
		OASiblingHelperDelegate.onGetObjectReference(oaObj, linkPropertyName);

		Object objOriginal = OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, true, true);

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
		OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, linkPropertyName);

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
			OAObjectPropertyDelegate.setPropertyLock(oaObj, linkPropertyName);
			result = _getReferenceObject(oaObj, linkPropertyName, oi, li);
			OAObjectPropertyDelegate.setPropertyCAS(oaObj, linkPropertyName, result, objOriginal, bDidNotExist, false);
		} finally {
			OAObjectPropertyDelegate.releasePropertyLock(oaObj, linkPropertyName);
			if (result instanceof OAObjectKey) {
				result = getReferenceObject(oaObj, linkPropertyName);
			}
		}
		return result;
	}

	// note: this acquired a lock before calling
	private static Object _getReferenceObject(final OAObject oaObj, final String linkPropertyName, final OAObjectInfo oi,
			final OALinkInfo li) {
		if (linkPropertyName == null) {
			return null;
		}

		final boolean bIsServer = OASyncDelegate.isServer(oaObj);
		final boolean bIsCalc = li != null && li.bCalculated;

		Object ref = null;
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, linkPropertyName, true, true);

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
							OAObjectPropertyDelegate.setPropertyCAS(oaObj, linkPropertyName, obj, null);
							return obj;
						}
					} else {
						OAFinder hf = new OAFinder(pps);
						obj = hf.findFirst(oaObj);
						if (obj != null) {
							OAObjectPropertyDelegate.setPropertyCAS(oaObj, linkPropertyName, obj, null);
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
					if (OAObjectInfoDelegate.isOne2One(li)) { // will only be "null" if it was deleted, else it will be oaNotExist
						return null;
					}
				} else {
					if (!li.bCalculated) {
						return null;
					}
				}
			}

			// == null.  check to see if it is One2One, and if a select must be used to get the object.
			if (li == null) {
				return null;
			}
			if (OAObjectInfoDelegate.isOne2One(li)) {
				if (!oaObj.isNew()) {
					OALinkInfo liReverse = OAObjectInfoDelegate.getReverseLinkInfo(li);
					if (!bIsServer && !bIsCalc) {
						if (oaObj.isDeleted()) {
							return null;
						}
						if (liReverse != null && !liReverse.bPrivateMethod) {
							ref = OAObjectCSDelegate.getServerReference(oaObj, linkPropertyName);
						} else {
							ref = null;
						}
					} else if (!bIsCalc) {
						if (liReverse != null && !liReverse.bPrivateMethod) {
							OASelect sel = new OASelect(li.getToClass());
							sel.setWhereObject(oaObj);
							sel.setPropertyFromWhereObject(li.name);
							sel.select();
							ref = sel.next();
							sel.close();
						}
					}
				}
			} else {
				// first check to see if it is in the hub for the link
				if (li.getPrivateMethod()) {
					Hub hubx = OAObjectHubDelegate.getHub(oaObj, li);
					if (hubx != null) {
						ref = HubDelegate.getMasterObject(hubx);
					}
				}

				if (ref == null && li.getPrivateMethod()) {
					OADataSource ds = OADataSource.getDataSource(li.getToClass());
					if (ds != null && ds.supportsStorage()) {
						if (!bIsServer && !bIsCalc) {
							if (oaObj.isDeleted()) {
								return null;
							}
							ref = OAObjectCSDelegate.getServerReference(oaObj, linkPropertyName);
						} else {
							OALinkInfo liReverse = OAObjectInfoDelegate.getReverseLinkInfo(li);
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

			ref = OAObjectCacheDelegate.get(li.toClass, key);

			if (ref == null) {
				if (!bIsServer && !bIsCalc && !oi.getLocalOnly()) {
					ref = OAObjectCSDelegate.getServerReference(oaObj, linkPropertyName);
				} else {
					OAObjectKey[] siblingKeys;
					if (OAThreadLocalDelegate.isDeleting()) {
						siblingKeys = null;
					} else {
						siblingKeys = OASiblingHelperDelegate.getSiblings(oaObj, linkPropertyName, 75, hmIgnoreSibling);
					}
					String sibIds = null;
					if (siblingKeys != null) {
						for (OAObjectKey keyx : siblingKeys) {
							OAObject objx = OAObjectCacheDelegate.get(oaObj.getClass(), keyx);
							if (objx == null) {
								continue;
							}
							Object valx = OAObjectPropertyDelegate.getProperty(objx, linkPropertyName, false, false);
							if (!(valx instanceof OAObjectKey)) {
								continue;
							}

							if (!OAObjectPropertyDelegate.isPropertyLocked(objx, linkPropertyName)) {
								Object[] idsx = ((OAObjectKey) valx).getObjectIds();
								if (idsx == null || idsx.length != 1) {
									continue;
								}
								if (sibIds == null) {
									sibIds = "" + idsx[0];
								} else {
									sibIds += "," + idsx[0];
								}
							}
						}
						if (sibIds != null) {
							Object[] idsx = key.getObjectIds();
							if (idsx == null || idsx.length != 1) {
								sibIds = null;
							} else {
								sibIds = idsx[0] + "," + sibIds;
							}
						}
					}
					if (sibIds != null) {
						OASelect sel = new OASelect(li.toClass);
						sel.setWhere("id IN (" + sibIds + ")");
						sel.select();
						for (; sel.hasMore();) {
							OAObject refx = sel.next(); // this will load into objCache w/weakRef
							if (refx.getObjectKey().equals(key)) {
								ref = refx;
							}
						}
					} else {
						ref = (OAObject) OAObjectDSDelegate.getObject(oi, li.toClass, (OAObjectKey) obj);
					}
					if (siblingKeys != null) {
						for (OAObjectKey ok : siblingKeys) {
							hmIgnoreSibling.remove(ok.getGuid());
						}
					}
					//was: ref = (OAObject) OAObjectDSDelegate.getObject(oi, li.toClass, (OAObjectKey) obj);
				}
			}
		}

		if (ref == null && li.getAutoCreateNew() && !bIsCalc) {
			boolean b = OAObjectInfoDelegate.isOne2One(li);
			if (b && oaObj.isDeleted() && !bIsServer) {
				// 20151117 dont autocreate new if this is deleted
			} else {
				if (!bIsServer && !OAObjectCSDelegate.isInNewObjectCache(oaObj)) {
					ref = OAObjectCSDelegate.getServerReference(oaObj, linkPropertyName);
				} else {
					ref = OAObjectReflectDelegate.createNewObject(li.getToClass());

					// 20190322
					if (((OAObject) ref).isLoading()) {
						OAObjectDelegate.initialize((OAObject) ref, OAObjectInfoDelegate.getOAObjectInfo(li.getToClass()), true, true, true,
													false, true);
					}

					setProperty(oaObj, linkPropertyName, ref, null); // need to do this so oaObj.changed=true, etc.
					if (b) { // 20190220
						setProperty((OAObject) ref, li.getReverseLinkInfo().getName(), oaObj, null);
					}
				}
			}
		}
		return ref;
	}

	/**
	 * Used to retrieve a reference key without actually loading the object. Datasourcs stores the key value for references, that are then
	 * used to retrieve the object when requested using getObject(property). This method is a way to get the key, without loading the
	 * object.<br>
	 * <br>
	 *
	 * @return the OAObjectKey of a ONE reference. Does not call getMethod, but internally stored value. see isLoaded to see if object has
	 *         been loaded and exists in memory. isLoaded will return false if the property has never been set, or loaded, or if the
	 *         objectKey has been set and the object for the key is not in memory. This method will always return the objectKey for the
	 *         reference. see getObject, which will loaded the object from memory or datasource.
	 */
	public static OAObjectKey getPropertyObjectKey(OAObject oaObj, String property) {
		if (property == null) {
			return null;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, property, false, true);
		if (obj == null) {
			return null;
		}
		if (obj instanceof OAObjectKey) {
			return (OAObjectKey) obj;
		}
		if (obj instanceof OAObject) {
			return OAObjectKeyDelegate.getKey((OAObject) obj);
		}
		return null;
	}

	/**
	 * Checks to see if the actual value for a property has been loaded. This will also check to see if a reference ObjectKey was loaded and
	 * the "real" object is in memory.
	 */
	public static boolean hasReferenceObjectBeenLoaded(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
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
			Hub h = (Hub) obj;
			Class c = h.getObjectClass();
			if (c.equals(OAObjectKey.class)) {
				return false;
			}
			return true;
		}
		if (obj instanceof OAObjectKey) {
			// use Key to see if object is in memory
			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oaObj.getClass(), propertyName);
			if (li == null) {
				return true;
			}

			Object objFound = OAObjectCacheDelegate.get(li.toClass, (OAObjectKey) obj);
			if (objFound != null) {
				OAObjectPropertyDelegate.setPropertyCAS(oaObj, propertyName, objFound, obj);
				return true;
			}
		}
		return false;
	}

	public static boolean isReferenceObjectNullOrEmpty(OAObject oaObj, String propertyName) {
		if (oaObj == null || propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true; // the ref is null, dont need to load it
		}
		if (obj == OANotExist.instance) {
			return true;
		}
		return false;
	}

	public static boolean isReferenceObjectLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
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
			OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oaObj.getClass(), propertyName);
			if (li == null) {
				return true;
			}

			Object objFound = OAObjectCacheDelegate.get(li.toClass, (OAObjectKey) obj);
			if (objFound != null) {
				OAObjectPropertyDelegate.setPropertyCAS(oaObj, propertyName, objFound, obj);
				return true;
			}
		}
		return false;
	}

	public static boolean isReferenceNullOrNotLoaded(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
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

	public static boolean isReferenceNullOrNotLoadedOrEmptyHub(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
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

	public static boolean isReferenceHubLoaded(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);

		if (obj instanceof OANotExist) {
			return false;
		}
		if (obj == null) {
			return true; // flag that hub could be create, with no objects
		}

		if (obj instanceof Hub) {
			return true;
		}

		OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oaObj.getClass(), propertyName);
		if (li == null || li.getType() != li.MANY) {
			return false;
		}
		return true;
	}

	// used to check for a known empty hub (already loaded, with size=0)
	public static boolean isReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, true, true);
		if (obj == null) {
			return true;
		}
		if (obj instanceof OANotExist) {
			return false;
		}

		if (obj instanceof Hub) {
			return ((Hub) obj).getSize() == 0;
		}

		OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oaObj.getClass(), propertyName);
		if (li == null || li.getType() != li.MANY) {
			return false;
		}
		return true;
	}

	public static boolean isReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
		if (propertyName == null) {
			return false;
		}
		Object obj = OAObjectPropertyDelegate.getProperty(oaObj, propertyName, false, true);
		if (obj == null) {
			return false;
		}
		if (obj instanceof Hub) {
			return ((Hub) obj).getSize() > 0;
		}
		return false;
	}

	/**
	 * Used to preload data, this will recursively load all references in the given Property Paths.
	 *
	 * @param oaObj         root object to use.
	 * @param propertyPaths one or more propertyPaths, that can be loaded using a single visit to each property.
	 */
	public static void loadProperties(OAObject oaObj, String... propertyPaths) {
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
	 * Used to preload data, this will recursively load all references in the given Property Paths.
	 *
	 * @param hub           root objects to use.
	 * @param propertyPaths one or more propertyPaths, that can be loaded using a single visit to each property.
	 */
	public static void loadProperties(Hub hub, String... propertyPaths) {
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
	 * Used by loadProperties, to take multiple property paths, and create a tree of unique property paths.
	 *
	 * @param propertyPaths example: "orders.orderItems.item.vendor"
	 * @return root node of tree, that has it's children as the starting point for the property paths.
	 */
	private static LoadPropertyNode createPropertyTree(String... propertyPaths) {
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

	// recursively load path
	private static void _loadProperties(Object object, LoadPropertyNode node) {
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
			Hub h = (Hub) object;
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
	 * Create a copy of an object, excluding selected properties.
	 *
	 * @return new copy of the object
	 */
	public static OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
		return createCopy(oaObj, excludeProperties, null);
	}

	public static OAObject createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback) {
		HashMap<Integer, Object> hmNew = new HashMap<Integer, Object>();
		OAObject obj = _createCopy(oaObj, excludeProperties, copyCallback, hmNew);
		return obj;
	}

	public static OAObject _createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback,
			HashMap<Integer, Object> hmNew) {
		if (oaObj == null) {
			return null;
		}

		OAObject newObject = (OAObject) hmNew.get(OAObjectDelegate.getGuid(oaObj));
		if (newObject != null) {
			return newObject;
		}

		// run on server only - otherwise objects can not be updated, since setLoadingObject is true
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
		if (!oi.getLocalOnly()) {
			if (!OASyncDelegate.isServer(oaObj)) {
				// 20130505 needs to be put in msg queue
				newObject = OAObjectCSDelegate.createCopy(oaObj, excludeProperties);
				return newObject;
			}
		}

		try {
			OAThreadLocalDelegate.setLoading(true);
			OAThreadLocalDelegate.setSuppressCSMessages(true);

			newObject = (OAObject) createNewObject(oaObj.getClass());
			OAObjectDelegate.initialize(newObject, oi, true, true, false, false, true);

			_copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);

		} finally {
			OAThreadLocalDelegate.setSuppressCSMessages(false);
			OAThreadLocalDelegate.setLoading(false);
		}
		OAObjectCacheDelegate.add(newObject);
		return newObject;
	}

	/**
	 * Copies the properties and some of the links from a source object (this) to a new object. For links of type One, all of the links are
	 * used, the same ref object from the source object is used. For links of type Many, only the owned links are used, and clones of the
	 * objects are created in the Hub of the new object. OACopyCallback can be used to control what is copied.
	 */
	public static void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback) {
		HashMap<Integer, Object> hmNew = new HashMap<Integer, Object>();
		copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
	}

	public static void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback,
			HashMap<Integer, Object> hmNew) {
		try {
			OAThreadLocalDelegate.setLoading(true);
			OAThreadLocalDelegate.setSuppressCSMessages(true);

			_copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
		} finally {
			OAThreadLocalDelegate.setLoading(false);
			OAThreadLocalDelegate.setSuppressCSMessages(false);
		}
	}

	/*
	 * note: OAThreadLocalDelegate.setLoadingObject(true/false) is not set in this method.
	 *      it is set by copyInto
	 */
	public static void _copyInto(final OAObject oaObj, final OAObject newObject, final String[] excludeProperties,
			final OACopyCallback copyCallback, final HashMap<Integer, Object> hmNew) {
		if (oaObj == null || newObject == null) {
			return;
		}
		hmNew.put(OAObjectDelegate.getGuid(oaObj), newObject);
		if (!(oaObj.getClass().isInstance(newObject))) {
			throw new IllegalArgumentException("OAObject.copyInto() object is not same class");
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
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
			Hub hub = (Hub) OAObjectReflectDelegate.getProperty(oaObj, li.getName());
			Hub hubNew = (Hub) OAObjectReflectDelegate.getProperty(newObject, li.getName());
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

				Object objx = hmNew.get(OAObjectDelegate.getGuid((OAObject) obj));

				if (objx == null) {
					if (copyCallback != null) {
						objx = copyCallback.createCopy(oaObj, li.getName(), hub, obj);
						if (obj == objx) {
							objx = _createCopy((OAObject) obj, (String[]) null, copyCallback, hmNew);
						}
					} else {
						objx = _createCopy((OAObject) obj, (String[]) null, copyCallback, hmNew);
						//was: objx = obj.createCopy();
					}
				}
				if (objx != null) {
					if (obj != objx) {
						hmNew.put(OAObjectDelegate.getGuid(obj), objx);
					}
					hubNew.add(objx);
					// assign parentProperty
					OAObjectPropertyDelegate.unsafeSetProperty(	(OAObject) objx, HubDetailDelegate.getPropertyFromDetailToMaster(hubNew),
																newObject);
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

			Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.getName());

			OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
			if (liRev != null && liRev.isOwner() && !li.getAutoCreateNew()) {
				Object newObj = hmNew.get(OAObjectDelegate.getGuid((OAObject) obj)); // this is the new/replacement one to use
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
					Object objx = hmNew.get(OAObjectDelegate.getGuid((OAObject) obj));
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
							Object objx = _createCopy((OAObject) obj, excludeProperties, copyCallback, hmNew);
							if (objx != obj && objx != null) {
								hmNew.put(OAObjectDelegate.getGuid((OAObject) obj), objx);
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

	// recursively checks 3 levels for replaced objects
	private static boolean shouldMakeACopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback,
			HashMap<Integer, Object> hmNew, int cnt, HashSet<Integer> hsVisitor) {
		if (oaObj == null) {
			return false;
		}
		if (hsVisitor == null) {
			hsVisitor = new HashSet<Integer>(101, .75f);
		} else if (hsVisitor.contains(OAObjectDelegate.getGuid(oaObj))) {
			return false;
		}
		hsVisitor.add(OAObjectDelegate.getGuid(oaObj));

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());
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
				Hub hub = (Hub) OAObjectReflectDelegate.getProperty(oaObj, li.getName());
				for (int j = 0; hub != null; j++) {
					OAObject obj = (OAObject) hub.elementAt(j);
					if (obj == null) {
						break;
					}
					Object objx = hmNew.get(OAObjectDelegate.getGuid((OAObject) obj));
					if (objx != null) {
						return true;
					}

					if (cnt < 3 && obj instanceof OAObject) {
						if (shouldMakeACopy((OAObject) obj, excludeProperties, copyCallback, hmNew, cnt + 1, hsVisitor)) {
							return true;
						}
					}
				}
			} else {
				Object obj = OAObjectReflectDelegate.getProperty(oaObj, li.getName());
				if (obj != null) {
					Object objx = hmNew.get(OAObjectDelegate.getGuid((OAObject) obj));
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

	public static Class getHubObjectClass(Method method) {
		Class cx = null;
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
	 * Find the common Hub that two objects are descendants of. param currentLevel current level of parents that have been checked
	 *
	 * @param maxLevelsToCheck total number of parents to check
	 */
	public static Hub findCommonHierarchyHub(OAObject obj1, OAObject obj2, int maxLevelsToCheck) {
		return findCommonHierarchyHub(obj1, obj2, 0, maxLevelsToCheck);
	}

	protected static Hub findCommonHierarchyHub(OAObject obj1, OAObject obj2, int currentLevel, int maxLevelsToCheck) {
		if (obj1 == null || obj2 == null) {
			return null;
		}
		if (currentLevel >= maxLevelsToCheck) {
			return null;
		}

		Hub[] hubs = OAObjectHubDelegate.getHubReferences(obj1);
		for (int i = 0; hubs != null && i < hubs.length; i++) {
			Hub nextHub = hubs[i];
			if (nextHub == null) {
				continue;
			}
			int x = getHierarchyLevelsToHub(nextHub, obj2, 0, maxLevelsToCheck);
			if (x > 0) {
				return nextHub;
			}

			OAObject objMaster = nextHub.getMasterObject();
			Hub h = findCommonHierarchyHub(objMaster, obj2, currentLevel + 1, maxLevelsToCheck);
			if (h != null) {
				return h;
			}
		}
		return null;
	}

	public static int getHierarchyLevelsToHub(Hub findHub, OAObject fromObj, int maxLevelsToCheck) {
		return getHierarchyLevelsToHub(findHub, fromObj, 0, maxLevelsToCheck);
	}

	protected static int getHierarchyLevelsToHub(Hub findHub, OAObject fromObj, int currentLevel, int maxLevelsToCheck) {
		if (findHub == null || fromObj == null) {
			return -1;
		}
		if (currentLevel >= maxLevelsToCheck) {
			return -1;
		}

		Hub[] hubs = OAObjectHubDelegate.getHubReferences(fromObj);
		for (int i = 0; hubs != null && i < hubs.length; i++) {
			Hub hub = hubs[i];
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
	 * get the property path from a Parent hub to a child hub.
	 *
	 * @param hubParent parent hub that needs to have the path expanded from.
	 * @param hubChild  child Hub that has a path
	 * @return
	 */
	private static String getPropertyPathFromMaster(final Hub hubParent, final Hub hubChild) {
		if (hubParent == null) {
			return null;
		}
		if (hubChild == null) {
			return null;
		}
		String pathFromParent = null;

		boolean b = false;
		if (HubLinkDelegate.getLinkedOnPos(hubChild, true)) {
			//String s = HubLinkDelegate.getLinkToProperty(hubChild, true);
			b = true;
		}
		String fromProp = HubLinkDelegate.getLinkFromProperty(hubChild, true);
		if (fromProp != null) {
			b = true;
			//return fromProp;
		}

		// see if there is a link path
		pathFromParent = null;
		Hub h = hubChild;
		for (; !b;) {
			Hub hx = HubLinkDelegate.getLinkToHub(h, true);
			if (hx == null) {
				pathFromParent = null;
				break;
			}

			if (pathFromParent == null) {
				pathFromParent = HubLinkDelegate.getLinkHubPath(h, true);
			} else {
				pathFromParent = HubLinkDelegate.getLinkHubPath(h, true) + "." + pathFromParent;
			}

			if (hx == hubParent) {
				return pathFromParent;
			}
			if (HubShareDelegate.isUsingSameSharedAO(hubParent, hx, true)) {
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
			Hub hx = h.getMasterHub();
			if (hx == null) {
				return null;
			}
			if (pathFromParent == null) {
				pathFromParent = HubDetailDelegate.getPropertyFromMasterToDetail(h);
			} else {
				pathFromParent = HubDetailDelegate.getPropertyFromMasterToDetail(h) + "." + pathFromParent;
			}

			if (hx == hubParent) {
				return pathFromParent;
			}
			if (HubShareDelegate.isUsingSameSharedAO(hubParent, hx, true)) {
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

	public static String getPropertyPathFromMaster(final OAObject objParent, final Hub hubChild) {
		if (objParent == null) {
			return null;
		}
		if (hubChild == null) {
			return null;
		}
		String pathFromParent = null;
		final Class parentClass = objParent.getClass();

		boolean b = false;
		if (HubLinkDelegate.getLinkedOnPos(hubChild, true)) {
			//String s = HubLinkDelegate.getLinkToProperty(hubChild, true);
			b = true;
		}
		String fromProp = HubLinkDelegate.getLinkFromProperty(hubChild, true);
		if (fromProp != null) {
			b = true;
			//return fromProp;
		}

		// see if there is a link path
		pathFromParent = null;
		Hub h = hubChild;
		for (; !b;) {
			Hub hx = HubLinkDelegate.getLinkToHub(h, true);
			if (hx == null) {
				pathFromParent = null;
				break;
			}

			if (pathFromParent == null) {
				pathFromParent = HubLinkDelegate.getLinkHubPath(h, true);
			} else {
				pathFromParent = HubLinkDelegate.getLinkHubPath(h, true) + "." + pathFromParent;
			}

			if (parentClass.equals(hx.getObjectClass())) {
				return pathFromParent;
			}
			h = hx;
		}

		// see if if there is a detail path using masterHub
		h = hubChild;
		for (;;) {
			Hub hx = h.getMasterHub();
			if (hx == null) {
				return null;
			}
			if (pathFromParent == null) {
				pathFromParent = HubDetailDelegate.getPropertyFromMasterToDetail(h);
			} else {
				pathFromParent = HubDetailDelegate.getPropertyFromMasterToDetail(h) + "." + pathFromParent;
			}
			if (parentClass.equals(hx.getObjectClass())) {
				return pathFromParent;
			}
			h = hx;
		}
	}

	/**
	 * get the "real" object that needs to be displayed, based on a parent/from Hub, and a from object (ex: row in table), and the hub that
	 * it originates from.
	 *
	 * @param hubFrom    ex: hubDept
	 * @param fromObject ex: dept
	 * @param hubChild   ex: hubEmplyeeType, (enum of strings) and is linked to hubEmp
	 * @return hubEmployeeType.getAt(pos), where pos is dept.manager.employeeType
	 */
	public static Object getObjectToDisplay(final Hub hubFrom, Object fromObject, final Hub hubChild) {
		if (hubFrom == null) {
			return null;
		}
		if (hubChild == null) {
			return null;
		}
		if (fromObject == null) {
			return null;
		}

		if (!HubLinkDelegate.getLinkedOnPos(hubChild, true)) {
			return fromObject;
		}

		Hub hubPosValue = HubLinkDelegate.getLinkToHub(hubChild, false);
		if (hubPosValue == null) {
			return fromObject;
		}

		// see if there is a link path
		String pathFromParent = null;
		Hub h = hubPosValue;
		for (;;) {
			Hub hx = HubLinkDelegate.getLinkToHub(h, true);
			if (hx == null) {
				pathFromParent = null;
				break;
			}

			if (pathFromParent == null) {
				pathFromParent = HubLinkDelegate.getLinkHubPath(h, true);
			} else {
				pathFromParent = HubLinkDelegate.getLinkHubPath(h, true) + "." + pathFromParent;
			}

			if (hx == hubFrom) {
				break;
			}
			if (HubShareDelegate.isUsingSameSharedAO(hubFrom, hx, true)) {
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

		String fromProp = HubLinkDelegate.getLinkToProperty(hubChild);
		if (fromProp == null) {
			return fromObject;
		}

		Object objx = getProperty((OAObject) fromObject, fromProp);
		int x = OAConv.toInt(objx);
		return hubChild.getAt(x);
	}

	/**
	 * get the property path from a Parent hub to a child hub, that is all of type=One.
	 *
	 * @param hubParent parent hub that needs to have the path expanded from.
	 * @param hubChild  child Hub that has a path
	 */
	public static String getPropertyPathBetweenHubs(final Hub hubParent, final Hub hubChild) {
		return getPropertyPathBetweenHubs(null, hubParent, hubChild, true);
	}

	private static String getPropertyPathBetweenHubs(final String propPath, final Hub hubParent, final Hub hubChild, boolean bCheckLink) {
		if (hubChild == hubParent) {
			return null;
		}
		if (hubChild == null || hubParent == null) {
			return null;
		}

		if (HubShareDelegate.isUsingSameSharedHub(hubParent, hubChild)) {
			return null;
		}

		Hub hx;
		if (bCheckLink) {
			hx = HubLinkDelegate.getLinkToHub(hubChild, true);
			if (hx != null) {
				boolean b = HubLinkDelegate.getLinkedOnPos(hubChild, true);
				String s;
				if (!b) {
					s = HubLinkDelegate.getLinkHubPath(hubChild, true);
					if (propPath != null) {
						s = propPath + "." + s;
					}
				} else {
					s = null;
				}

				if (hx == hubParent) {
					return s;
				}
				if (HubShareDelegate.isUsingSameSharedAO(hubParent, hx, true)) {
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
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hubChild);
		if (li == null) {
			return null;
		}
		li = OAObjectInfoDelegate.getReverseLinkInfo(li);
		if (li == null) {
			return null;
		}
		if (li.getType() != OALinkInfo.ONE) {
			return null;
		}

		String pathFromParent = HubDetailDelegate.getPropertyFromMasterToDetail(hubChild);
		if (pathFromParent == null) {
			return null;
		}
		if (propPath != null) {
			pathFromParent = pathFromParent + "." + propPath;
		}

		if (hx == hubParent) {
			return pathFromParent;
		}
		if (HubShareDelegate.isUsingSameSharedAO(hubParent, hx, true)) {
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

}

class LoadPropertyNode {
	String prop;
	LoadPropertyNode[] children;
}

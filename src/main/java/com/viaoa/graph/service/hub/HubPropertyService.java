package com.viaoa.graph.service.hub;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.util.OACompare;
import com.viaoa.util.OANullObject;

public abstract class HubPropertyService {
	private final Logger LOG = Logger.getLogger(HubPropertyService.class.getName());

	private final Hub.FriendAccess faHub;

	public HubPropertyService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	
	/**
	 * Configures the hub to enforce uniqueness based on the specified property.
	 * Validates that the property is not nested, that a corresponding getter
	 * method exists, and that the getter accepts no parameters. When {@code null}
	 * is supplied, the unique property is cleared.
	 *
	 * @param thisHub      the hub whose unique property is being set
	 * @param propertyName the name of the property used for uniqueness, or
	 *                     {@code null} to clear
	 * @throws IllegalArgumentException if the property is nested, lacks a getter,
	 *                                  or the getter requires parameters
	 */
	public <T extends OAObject> void setUniqueProperty(Hub<T> thisHub, String propertyName) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		
		if (propertyName == null) {
			hd.setUniqueProperty(null);
			hd.setUniquePropertyGetMethod(null);
			return;
		}
		if (propertyName.indexOf('.') >= 0) {
			throw new IllegalArgumentException(
					"Property " + propertyName + " can only be for a property in " + thisHub.getObjectClass().getName());
		}

		hd.setUniquePropertyGetMethod(callObjectInfoGetMethod(thisHub.getObjectClass(), "get" + propertyName));
		if (hd.getUniquePropertyGetMethod() == null) {
			throw new IllegalArgumentException("Get Method for Property " + propertyName + " not found");
		}
		if (hd.getUniquePropertyGetMethod().getParameterTypes().length > 0) {
			throw new IllegalArgumentException("Get Method for Property " + propertyName + " expects parameters");
		}
		hd.setUniqueProperty(propertyName);
	}




	/**
	 * Stores a named property value on the hub. Property names are normalized to
	 * uppercase. A {@link OANullObject} marker is stored when the value is
	 * {@code null}. A new property map is created on demand.
	 *
	 * @param thisHub the hub whose property map is updated
	 * @param name    the property name
	 * @param obj     the value to store, or {@code null}
	 */
	public <T extends OAObject> void setProperty(Hub<T> thisHub, String name, Object obj) {
		if (name == null) {
			return;
		}
		name = name.toUpperCase();
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getHashProperty() == null) {
			hd.setHashProperty(new Hashtable(7));
		}
		hd.getHashProperty().put(name, (obj == null) ? OANullObject.instance : obj);
	}

	/**
	 * Retrieves a named property value previously stored on the hub. Property names
	 * are normalized to uppercase. A stored {@link OANullObject} resolves to
	 * {@code null}. If no property map exists, {@code null} is returned.
	 *
	 * @param thisHub the hub whose property is requested
	 * @param name    the property name
	 * @return the stored value, or {@code null} if not found
	 */
	public <T extends OAObject> Object getProperty(Hub<T> thisHub, String name) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getHashProperty() == null) {
			return null;
		}

		name = name.toUpperCase();
		Object obj = hd.getHashProperty().get(name);
		if (obj instanceof OANullObject) {
			obj = null;
		}
		return obj;
	}

	/**
	 * Removes a property from the hub’s property map. Property names are converted
	 * to uppercase. If no property map exists, no action is taken.
	 *
	 * @param thisHub the hub whose property should be removed
	 * @param name    the name of the property to remove
	 */
	public <T extends OAObject> void removeProperty(Hub<T> thisHub, String name) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getHashProperty() != null) {
			name = name.toUpperCase();
			hd.getHashProperty().remove(name);
		}
	}

	
	/**
	 * Verifies that the specified object's unique property value does not already
	 * exist in this hub. If the hub or object is null, or if the object is loading,
	 * uniqueness checking is bypassed. When a unique property is defined, its value
	 * is obtained either through a link property or a getter method. Null or blank
	 * values are not checked.
	 *
	 * <p>
	 * The method iterates through all hub elements and compares each object's
	 * unique property value to that of the given object. If an equal value is found
	 * on a different object, the uniqueness constraint fails.
	 *
	 * @param thisHub the hub in which uniqueness is validated
	 * @param object  the object whose property value is being checked
	 * @return {@code true} if the unique value does not conflict; otherwise
	 *         {@code false}
	 */
	public <T extends OAObject> boolean verifyUniqueProperty(final Hub<T> thisHub, final T object) {
		if (thisHub == null || object == null) {
			return true;
		}

		if (callThreadLocalIsLoading()) {
			return true;
		}

		final HubData<T> hd = faHub.getHubData(thisHub);
		final HubDataMaster hdm = faHub.getHubDataMaster(thisHub);
		
		Object object2;
		Method m = null;
		String uniqueLinkPropName;
		try {
			
			uniqueLinkPropName = hd.getUniqueProperty();
			if (uniqueLinkPropName == null) {
				uniqueLinkPropName = hdm.getUniqueProperty();
			}
			if (uniqueLinkPropName != null) {
				OAObjectInfo oi = callObjectInfoGetOAObjectInfo(thisHub);
				if (oi.getLinkInfo(uniqueLinkPropName) == null) {
					uniqueLinkPropName = null;
				}
			}

			if (uniqueLinkPropName != null) {
				object2 = callObjectPropertyGetProperty((OAObject) object, uniqueLinkPropName);
				
			} else {
				m = hd.getUniquePropertyGetMethod();
				if (m == null) {
					m = hdm.getUniquePropertyGetMethod();
					if (m == null) {
						return true;
					}
				}
				object2 = m.invoke(object, (Object[]) null);
				if (object2 == null) {
					return true;
				}
				if (object2 instanceof String && ((String) object2).equals("")) {
					return true;
				}
			}
		} catch (Exception e) {
			String s = m == null ? "" : m.getName();
			throw new RuntimeException("Error invoking " + s, e);
		}

		for (int i = 0;; i++) {
			Object obj = thisHub.elementAt(i);
			if (obj == null) {
				break;
			}
			if (obj == object) {
				continue;
			}

			try {
				if (uniqueLinkPropName != null) {
					Object obj2 = callObjectPropertyGetProperty((OAObject) obj, uniqueLinkPropName);
					if (OACompare.compare(obj2, object2) == 0) {
						return false;
					}
					continue;
				}

				Object obj2 = m.invoke(obj, (Object[]) null);
				if (obj2 == null) {
					continue;
				}
				if (obj2 == object2 || obj2.equals(object2)) {
					return false;
				}
			} catch (Exception e) {
				String s = m == null ? "" : m.getName();
				throw new RuntimeException("Error invoking " + s, e);
			}
		}
		return true;
	}
	
	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(Hub hub);
	public abstract Method callObjectInfoGetMethod(OALinkInfo li);
	public abstract Method callObjectInfoGetMethod(Class<?> clazz, String methodName);
	public abstract Object callObjectPropertyGetProperty(OAObject oaObj, String name);			
	public abstract boolean callThreadLocalIsLoading();			
}

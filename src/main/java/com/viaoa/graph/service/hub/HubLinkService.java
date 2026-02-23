package com.viaoa.graph.service.hub;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

public abstract class HubLinkService {
	private final Logger LOG = Logger.getLogger(HubLinkService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubLinkService(Hub.FriendAccess faHub ) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Configures this Hub to link to another Hub using the specified reference properties.
	 * <p>
	 * Sets link metadata, resolves getter/setter methods, validates class compatibility,
	 * installs event listeners, and performs initial synchronization of Active Objects.
	 *
	 * @param thisHub               the Hub establishing the link
	 * @param propertyFrom          reference property name from thisHub's objects
	 * @param linkToHub             the Hub to link to
	 * @param propertyTo            reference property name in linkToHub's objects
	 * @param linkPosFlag           true if linking based on positional index
	 * @param bAutoCreate           true to auto-create linked objects
	 * @param bAutoCreateAllowDups  true to allow duplicates when auto-creating objects
	 */
	@SuppressWarnings("unchecked")
	public <T extends OAObject> void setLinkHub(Hub<T> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag,
			boolean bAutoCreate, boolean bAutoCreateAllowDups) {
		// 20110809 add bAutoCreateAllowDups
		if (linkToHub == thisHub) {
			return;
		}

		// 20181211 verify that no other shared hub is linked
		Hub hx = getHubWithLink(thisHub, true);
		if (linkToHub != null && hx != null && hx != thisHub) {

			// 20201221 allow setting bAutoCreate
			if (!linkPosFlag && faHub.getHubDataUnique(hx).getLinkToHub() == linkToHub && OAString.isEmpty(propertyFrom)
					&& OACompare.isEqual(propertyTo, faHub.getHubDataUnique(hx).getLinkToPropertyName())) {
				faHub.getHubDataUnique(thisHub).setAutoCreate(bAutoCreate);
				faHub.getHubDataUnique(thisHub).setAutoCreateAllowDups(bAutoCreateAllowDups);
				return;
			}

			String s = "Hub link failed, since another shared hub is already linked, thisHub=" + thisHub + ", linkToHub=" + linkToHub
					+ ", propertyTo=" + propertyTo;
			throw new RuntimeException(s);
		}

		if (faHub.getHubDataUnique(thisHub).getLinkToHub() != null) {
			if (faHub.getHubDataUnique(thisHub).getLinkToHub() == linkToHub) {
				if (faHub.getHubDataUnique(thisHub).isAutoCreate() == bAutoCreate && faHub.getHubDataUnique(thisHub).isAutoCreateAllowDups() == bAutoCreateAllowDups) {
					return;
				}
			}
			callHubEventRemoveHubListener(faHub.getHubDataUnique(thisHub).getLinkToHub(), faHub.getHubDataUnique(thisHub).getHubLinkEventListener());
			faHub.getHubDataUnique(thisHub).setLinkToHub(null);
			faHub.getHubDataUnique(thisHub).setHubLinkEventListener(null);
			faHub.getHubDataUnique(thisHub).setAutoCreate(false);
			faHub.getHubDataUnique(thisHub).setAutoCreateAllowDups(false);
		}
		if (linkToHub == null) {
			callHubEventFireAfterPropertyChange(thisHub, null, "Link", null, null, null);
			return;
		}

		if (propertyTo == null && linkToHub != null) {
			Class<? extends OAObject> c = linkToHub.getObjectClass();
			OAObjectInfo oi = callObjectInfoGetObjectInfo(c); // this never returns null

			List al = oi.getLinkInfos();
			for (int i = 0; i < al.size(); i++) {
				OALinkInfo li = (OALinkInfo) al.get(i);
				if (li.getType() != li.ONE) {
					continue;
				}
				if (li.getToClass().equals(faHub.getHubData(thisHub).getObjClass())) {
					propertyTo = li.getName();
					break;
				}
			}
		}

		Class<T> verifyClass = thisHub.getObjectClass();
		faHub.getHubDataUnique(thisHub).setLinkFromPropertyName(propertyFrom);
		faHub.getHubDataUnique(thisHub).setLinkFromGetMethod(null);
		if (propertyFrom != null) { // otherwise, use object
            faHub.getHubDataUnique(thisHub).setLinkFromGetMethod(callObjectInfoGetMethod(thisHub.getObjectClass(), "get" + propertyFrom));
			//was: faHub.getHubDataUnique(thisHub).setLinkFromGetMethod(OAReflect.getMethod(thisHub.getObjectClass(), "get" + propertyFrom));
			if (faHub.getHubDataUnique(thisHub).getLinkFromGetMethod() == null) {
				throw new RuntimeException("cant find method for property " + propertyFrom);
			}
			verifyClass = (Class<T>) faHub.getHubDataUnique(thisHub).getLinkFromGetMethod().getReturnType();
		}
		
        faHub.getHubDataUnique(thisHub).setLinkToGetMethod(callObjectInfoGetMethod(linkToHub.getObjectClass(), "get" + propertyTo));
		//was: faHub.getHubDataUnique(thisHub).setLinkToGetMethod(OAReflect.getMethod(linkToHub.getObjectClass(), "get" + propertyTo));
		if (faHub.getHubDataUnique(thisHub).getLinkToGetMethod() == null) {
			throw new RuntimeException(
					"cant find method for property \"" + propertyTo + "\" from linkToHub class=" + linkToHub.getObjectClass());
		}
		if (!linkPosFlag) {
			Class c = faHub.getHubDataUnique(thisHub).getLinkToGetMethod().getReturnType();
			if (!c.equals(verifyClass)) {
				if (!OAObject.class.equals(c)) {
					if (c.isPrimitive()) {
						c = OAReflect.getPrimitiveClassWrapper(c);
					}
					if (!c.equals(verifyClass)) {
						throw new RuntimeException("property is wrong class, property=" + propertyTo + ", class=" + c);
					}
				}
			}
		}
		if (linkPosFlag) {
			faHub.getHubDataUnique(thisHub).setLinkToSetMethod(OAReflect.getMethod(linkToHub.getObjectClass(), "set" + propertyTo, int.class));
		} else {
			faHub.getHubDataUnique(thisHub).setLinkToSetMethod(OAReflect.getMethod(linkToHub.getObjectClass(), "set" + propertyTo));
		}
		if (faHub.getHubDataUnique(thisHub).getLinkToSetMethod() == null) {
			throw new RuntimeException("cant find set method for property " + propertyTo);
		}

		Class[] cc = faHub.getHubDataUnique(thisHub).getLinkToSetMethod().getParameterTypes();

		if (!linkPosFlag) {
			if (cc.length == 1 && cc[0].isPrimitive()) {
				cc[0] = OAReflect.getPrimitiveClassWrapper(cc[0]);
			}
			if (cc.length != 1) {
				throw new RuntimeException(
						"wrong type of parameters for method, property:" + propertyTo + " class:" + thisHub.getObjectClass());
			}
			if (!cc[0].equals(verifyClass)) {
				if (!OAObject.class.equals(cc[0])) {
					Class c = verifyClass;
					if (c.isPrimitive()) {
						c = OAReflect.getPrimitiveClassWrapper(c);
					}
					if (!cc[0].equals(c)) {
						throw new RuntimeException(
								"wrong type of parameter for method, property:" + propertyTo + " class:" + thisHub.getObjectClass());
					}
				}
			}
		}

		if (faHub.getHubDataUnique(thisHub).getLinkToHub() != null) {
			// remove hub listener from previous linkHub
			faHub.getHubDataUnique(thisHub).getLinkToHub().removeHubListener(faHub.getHubDataUnique(thisHub).getHubLinkEventListener());
		}
		faHub.getHubDataUnique(thisHub).setLinkPos(linkPosFlag);
		faHub.getHubDataUnique(thisHub).setLinkToHub(linkToHub);
		faHub.getHubDataUnique(thisHub).setLinkToPropertyName(propertyTo);
		faHub.getHubDataUnique(thisHub).setHubLinkEventListener(new HubLinkEventListener(thisHub, linkToHub));
		faHub.getHubDataUnique(thisHub).setAutoCreate(bAutoCreate);
		faHub.getHubDataUnique(thisHub).setAutoCreateAllowDups(bAutoCreate && bAutoCreateAllowDups); // 20110809

		callHubEventAddHubListener(linkToHub, faHub.getHubDataUnique(thisHub).getHubLinkEventListener());
		faHub.getHubDataUnique(thisHub).getHubLinkEventListener().onNewList(null);

		OAObject ao = faHub.getHubDataUnique(thisHub).getLinkToHub().getActiveObject();
		 
		if (ao == null) { // 20240919
		    thisHub.setActiveObject(null);
		}
		else {
        	int pos = faHub.getHubDataUnique(thisHub).getLinkToHub().getPos();
        	// fire a fake changeActiveObject to have correct thisHub.ao set
        	callHubEventFireAfterChangeActiveObjectEvent((Hub<OAObject>) faHub.getHubDataUnique(thisHub).getLinkToHub(), (OAObject) ao, pos, true);
		}
		
		callHubEventFireAfterPropertyChange(thisHub, null, "Link", null, null, null);
	}

	/**
	 * Determines whether auto-create mode is enabled for this Hub's link.
	 *
	 * @param thisHub the Hub being checked
	 * @return true if auto-create is enabled; otherwise false
	 */
	public boolean isLinkAutoCreated(Hub<?> thisHub) {
		return isLinkAutoCreated(thisHub, false);
	}

	/**
	 * Determines whether auto-create mode is enabled for the Hub or any shared Hub.
	 *
	 * @param thisHub            the Hub being checked
	 * @param bIncludeCopiedHubs true to also check shared/copied Hubs
	 * @return true if auto-create is enabled; otherwise false
	 */
	public <T extends OAObject> boolean isLinkAutoCreated(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).isAutoCreate()) {
			return true;
		}
		if (!bIncludeCopiedHubs) {
			return false;
		}
		Hub<?> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (faHub.getHubDataUnique(h).isAutoCreate()) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		return (hubx != null);
	}

	/**
	 * Determines whether this Hub is linked using positional index.
	 *
	 * @param thisHub the Hub to examine
	 * @return true if linked by position; otherwise false
	 */
	public boolean getLinkedOnPos(Hub<?> thisHub) {
		return getLinkedOnPos(thisHub, false);
	}

	/**
	 * Determines whether this Hub or any shared Hub uses positional linking.
	 *
	 * @param thisHub            the Hub to examine
	 * @param bIncludeCopiedHubs true to evaluate copied/shared Hubs
	 * @return true if positional linking is active; otherwise false
	 */
	public <T extends OAObject> boolean getLinkedOnPos(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).isLinkPos()) {
			return true;
		}
		if (!bIncludeCopiedHubs) {
			return false;
		}
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (faHub.getHubDataUnique(h).isLinkPos()) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		return (hubx != null);
	}

	/**
	 * Updates the linked-to property for the active object based on changes
	 * from the linked-from Hub.
	 *
	 * @param thisHub    the Hub owning the linked property
	 * @param fromObject the source object whose value is being applied
	 * @param pos        location index when linking by position
	 */
	public <T extends OAObject> void updateLinkProperty(Hub<T> thisHub, T fromObject, int pos) {
		Hub h = faHub.getHubDataUnique(thisHub).getLinkToHub();
		if (h == null || faHub.getHubDataUnique(h).isUpdatingActiveObject()) {
			return;
		}
		try {
			_updateLinkProperty(thisHub, fromObject, pos);
		} catch (Exception e) {
			throw new RuntimeException("updateLinkProperty, hub=" + thisHub + ", fromObject=" + fromObject, e);
		}
	}

	/**
	 * Internal method that performs the actual update of the linked-to property.
	 * Handles auto-create logic, property forwarding, and positional updates.
	 *
	 * @param thisHub    the Hub owning the link
	 * @param fromObject the object providing the new value
	 * @param pos        positional index when applicable
	 * @throws Exception if reflection or setter invocation fails
	 */
	private <T extends OAObject, U extends OAObject> void _updateLinkProperty(Hub<T> thisHub, T fromObject, int pos) throws Exception {
		OAObject linkToObject = null;
		if (faHub.getHubDataUnique(thisHub).isAutoCreate()) {
			boolean bOne = false; // is there only supposed to be one object in hub
			HubDataMaster dm = callHubDetailGetDataMaster(thisHub);
			if (dm != null && dm.getDetailToMasterLinkInfo() != null) {
				OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo());
				if (liRev != null) {
					bOne = (liRev.getType() == OALinkInfo.ONE);
				}
			}

			if (fromObject == null) {
				if (!bOne || thisHub.getCurrentSize() == 0) {
					return;
				}
				// ?? set reference to null and delete/remove object from hub
				return;
			}
			if (!bOne || thisHub.getSize() == 0) {
				if (!faHub.getHubDataUnique(thisHub).isAutoCreateAllowDups()) { // 20110809 added flag, was: always did this check
					// see if object already exists
					for (int i = 0;; i++) {
						OAObject obj = faHub.getHubDataUnique(thisHub).getLinkToHub().elementAt(i);
						if (obj == null) {
							break;
						}
						Object obj2 = faHub.getHubDataUnique(thisHub).getLinkToGetMethod().invoke(obj, (Object[]) null);
						if (obj2 == fromObject) {
							faHub.getHubDataUnique(thisHub).getLinkToHub().setAO(obj);
							return;
						}
					}
				}
				// create new object and link to it
				Class<? extends OAObject> c = faHub.getHubDataUnique(thisHub).getLinkToHub().getObjectClass();
				Constructor constructor = c.getConstructor(new Class[] {});
				linkToObject = (OAObject) constructor.newInstance(new Object[] {});

				if (fromObject == null && faHub.getHubDataUnique(thisHub).getLinkToSetMethod().getParameterTypes()[0].isPrimitive()) {
					((OAObject) linkToObject).setNull(faHub.getHubDataUnique(thisHub).getLinkToPropertyName());
				} else {
					faHub.getHubDataUnique(thisHub).getLinkToSetMethod().invoke(linkToObject, new Object[] { fromObject });
				}

				if (faHub.getHubDataUnique(thisHub).getLinkToHub().getObject(linkToObject) == null) {
					((Hub<U>)faHub.getHubDataUnique(thisHub).getLinkToHub()).add((U) linkToObject);
				}
				faHub.getHubDataUnique(thisHub).getLinkToHub().setAO(linkToObject);
				return;
			}
		}

		if (linkToObject == null) {
			linkToObject = faHub.getHubDataUnique(thisHub).getLinkToHub().getActiveObject();
		}
		if (linkToObject != null) {
			Object obj = faHub.getHubDataUnique(thisHub).getLinkToGetMethod().invoke(linkToObject, (Object[]) null);
			if (faHub.getHubDataUnique(thisHub).isLinkPos()) { // allow number returned to set pos of active object, set by setLinkOnPos()
				if (obj instanceof Number) {
					int x = ((Number) obj).intValue();
					// need to check to see if prop value is null
					boolean b = false;
					if (x == pos && linkToObject instanceof OAObject) {
						b = (pos != -1) && ((OAObject) linkToObject).isNull(faHub.getHubDataUnique(thisHub).getLinkToPropertyName());
					}
					if (x != pos || b) {
						faHub.getHubDataUnique(thisHub).getLinkToSetMethod().invoke(linkToObject, new Object[] { Integer.valueOf(pos) });
						if (pos == -1 && linkToObject instanceof OAObject) { // 20131101 setting to null
							((OAObject) linkToObject).setNull(faHub.getHubDataUnique(thisHub).getLinkToPropertyName());
						}
					}
				}
			} else {
				if (fromObject != null && faHub.getHubDataUnique(thisHub).getLinkFromGetMethod() != null) {
					// if linking a property to another property
					fromObject = (T) faHub.getHubDataUnique(thisHub).getLinkFromGetMethod().invoke(fromObject, null);
				}

				if (obj != null || fromObject != null) {
					if ((obj == null || fromObject == null) || (!obj.equals(fromObject))) {
						if (fromObject == null && faHub.getHubDataUnique(thisHub).getLinkToSetMethod().getParameterTypes()[0].isPrimitive()) {
							((OAObject) linkToObject).setNull(faHub.getHubDataUnique(thisHub).getLinkToPropertyName());
						} else {
							faHub.getHubDataUnique(thisHub).getLinkToSetMethod().invoke(linkToObject, new Object[] { fromObject });
						}
					}
				}
			}
		}
	}

	/**
	 * Retrieves the value of the linked-to property for the given object.
	 *
	 * @param thisHub    the Hub whose linking configuration defines the lookup
	 * @param linkObject the object whose linked property value is requested
	 * @return the linked property value, or null if none
	 */
	public <T extends OAObject, U extends OAObject> Object getPropertyValueInLinkedToHub(Hub<?> thisHub, U linkObject) {
		Hub h = getHubWithLink(thisHub, true);
		if (h == null) {
			return null;
		}
		return _getPropertyValueInLinkedToHub(h, linkObject);
	}

	/**
	 * Internal helper used to extract the linked-to property value using the
	 * configured getter or positional logic.
	 *
	 * @param thisHub    the Hub whose link configuration applies
	 * @param linkObject the object to inspect
	 * @return the resolved linked-to value, or null
	 */
	private <T extends OAObject, U extends OAObject> Object _getPropertyValueInLinkedToHub(final Hub<T> thisHub, final U linkObjectOrig) {
		
		// example: hubDept linked to hubEmp.dept ...thisHub=hubDept, linkObjectOrig=Emp   return=emp.dept
		
		Object linkObject = linkObjectOrig;
		
		if (faHub.getHubDataUnique(thisHub).getLinkToGetMethod() == null) {
			return linkObject;
		}
		try {
			if (linkObject != null) {
				if (linkObject instanceof OAObject) {
					OAObject oa = (OAObject) linkObject;
					if (oa.isNull(faHub.getHubDataUnique(thisHub).getLinkToPropertyName())) {
						linkObject = null;
					}
				}
				if (linkObject != null) {
					linkObject = faHub.getHubDataUnique(thisHub).getLinkToGetMethod().invoke(linkObject, (Object[]) null);
				}
			}
			if (faHub.getHubDataUnique(thisHub).isLinkPos()) {
				int x = -1;
				if (linkObject != null && linkObject instanceof Number) {
					x = ((Number) linkObject).intValue();
				}
				return thisHub.elementAt(x);
			}

			if (faHub.getHubDataUnique(thisHub).getLinkFromGetMethod() != null) {
				// if linking a property to another property, need to find which object has matching property
				for (int i = 0;; i++) {
					Object obj = thisHub.elementAt(i);
					if (obj == null) {
						linkObject = null;
						break;
					}
					Object obj2 = faHub.getHubDataUnique(thisHub).getLinkFromGetMethod().invoke(obj, (Object[]) null);
					if ((linkObject == obj2) || (obj2 != null && obj2.equals(linkObject))) {
						linkObject = obj;
						break;
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return linkObject;
	}

	/**
	 * Retrieves the property name used as the link-to target.
	 *
	 * @param thisHub the Hub whose link property is requested
	 * @return the link-to property name, or null if none
	 */
	public String getLinkToProperty(Hub<?> thisHub) {
		return getLinkToProperty(thisHub, false);
	}

	/**
	 * Retrieves the link-to property name from this Hub or shared Hubs.
	 *
	 * @param thisHub            the Hub to inspect
	 * @param bIncludeCopiedHubs true to evaluate copied/shared Hubs
	 * @return the link-to property name, or null if none
	 */
	public <T extends OAObject> String getLinkToProperty(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkToPropertyName() != null) {
			return faHub.getHubDataUnique(thisHub).getLinkToPropertyName();
		}
		if (!bIncludeCopiedHubs) {
			return null;
		}
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (h == thisHub) {
					return false;
				}
				if (faHub.getHubDataUnique(h).getLinkToPropertyName() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		if (hubx == null) {
			return null;
		}
		return faHub.getHubDataUnique(hubx).getLinkToPropertyName();
	}

	/**
	 * Retrieves the property name used as the link-from reference.
	 *
	 * @param thisHub the Hub whose link-from property is requested
	 * @return the link-from property name, or null if none
	 */
	public String getLinkFromProperty(Hub<?> thisHub) {
		return getLinkFromProperty(thisHub, false);
	}

	/**
	 * Retrieves the link-from property name from this Hub or shared Hubs.
	 *
	 * @param thisHub            the Hub to inspect
	 * @param bIncludeCopiedHubs true to inspect copied/shared Hubs
	 * @return the link-from property name, or null if none
	 */
	public <T extends OAObject> String getLinkFromProperty(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkFromPropertyName() != null) {
			return faHub.getHubDataUnique(thisHub).getLinkFromPropertyName();
		}
		if (!bIncludeCopiedHubs) {
			return null;
		}
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (h == thisHub) {
					return false;
				}
				if (faHub.getHubDataUnique(h).getLinkFromPropertyName() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		if (hubx == null) {
			return null;
		}
		return faHub.getHubDataUnique(hubx).getLinkFromPropertyName();
	}

	/**
	 * Retrieves the Hub that this Hub is linked to, optionally searching shared Hubs.
	 *
	 * @param thisHub            the Hub whose link target is requested
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the linked-to Hub, or null if none
	 */
	public <T extends OAObject> Hub getLinkToHub(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkToHub() != null) {
			return faHub.getHubDataUnique(thisHub).getLinkToHub();
		}
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (faHub.getHubDataUnique(h).getLinkToHub() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		if (hubx == null) {
			return null;
		}
		return faHub.getHubDataUnique(hubx).getLinkToHub();
	}

	/**
	 * Retrieves the Hub that this Hub links to.
	 *
	 * @param thisHub            the Hub to check
	 * @param bIncludeCopiedHubs true to check shared/copied Hubs
	 * @return the linked-to Hub, or null
	 */
	public <T extends OAObject> Hub getHubWithLink(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkToHub() != null) {
			return thisHub;
		}
		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (faHub.getHubDataUnique(h).getLinkToHub() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		return hubx;
	}

	/**
	 * Determines whether the Hub is linked using position-based linking.
	 *
	 * @param thisHub the Hub to examine
	 * @return true if linking by position; otherwise false
	 */
	public boolean getLinkHubOnPos(Hub<?> thisHub) {
		return getLinkHubOnPos(thisHub, false);
	}

	/**
	 * Determines whether this Hub or any shared Hub uses position-based linking.
	 *
	 * @param thisHub            the Hub to inspect
	 * @param bIncludeCopiedHubs true to include copied/shared Hubs
	 * @return true if any Hub uses positional linking; otherwise false
	 */
	public <T extends OAObject> boolean getLinkHubOnPos(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).isLinkPos()) {
			return true;
		}
		if (!bIncludeCopiedHubs) {
			return false;
		}

		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (h == thisHub) {
					return false;
				}
				if (faHub.getHubDataUnique(h).isLinkPos()) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		return (hubx != null);
	}

	/**
	 * Retrieves the setter method used to apply linked property values.
	 *
	 * @param thisHub the Hub whose setter method is requested
	 * @return the link-to setter method, or null if none configured
	 */
	public Method getLinkSetMethod(Hub<?> thisHub) {
		return getLinkSetMethod(thisHub, false);
	}

	/**
	 * Retrieves the link-to setter method from this Hub or, optionally, any
	 * shared/copied Hub.
	 *
	 * @param thisHub            the Hub whose setter method is examined
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the link-to setter method, or null if none found
	 */
	public <T extends OAObject> Method getLinkSetMethod(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkToSetMethod() != null) {
			return faHub.getHubDataUnique(thisHub).getLinkToSetMethod();
		}
		if (!bIncludeCopiedHubs) {
			return null;
		}

		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (h == thisHub) {
					return false;
				}
				if (faHub.getHubDataUnique(h).getLinkToSetMethod() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		if (hubx == null) {
			return null;
		}
		return faHub.getHubDataUnique(hubx).getLinkToSetMethod();
	}

	/**
	 * Retrieves the getter method used to obtain values for link updates.
	 *
	 * @param thisHub the Hub whose getter method is requested
	 * @return the link-to getter method, or null if none configured
	 */
	public Method getLinkGetMethod(Hub<?> thisHub) {
		return getLinkGetMethod(thisHub, false);
	}

	/**
	 * Retrieves the getter method used to resolve link values, optionally checking
	 * shared/copied Hubs.
	 *
	 * @param thisHub            the Hub being examined
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the getter method, or null if not found
	 */
	public <T extends OAObject> Method getLinkGetMethod(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkToGetMethod() != null) {
			return faHub.getHubDataUnique(thisHub).getLinkToGetMethod();
		}
		if (!bIncludeCopiedHubs) {
			return null;
		}

		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (h == thisHub) {
					return false;
				}
				if (faHub.getHubDataUnique(h).getLinkToGetMethod() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		if (hubx == null) {
			return null;
		}
		return faHub.getHubDataUnique(hubx).getLinkToGetMethod();
	}

	/**
	 * Retrieves the property path for the link-to property.
	 *
	 * @param thisHub the Hub whose link path is requested
	 * @return the link-to property path, or null if none set
	 */
	public String getLinkHubPath(Hub<?> thisHub) {
		return getLinkHubPath(thisHub, false);
	}

	/**
	 * Retrieves the link-to property path from this Hub or, optionally, any
	 * shared/copied Hubs.
	 *
	 * @param thisHub            the Hub to evaluate
	 * @param bIncludeCopiedHubs true to include shared/copied Hubs
	 * @return the link-to property path, or null if none exists
	 */
	public <T extends OAObject> String getLinkHubPath(final Hub<T> thisHub, boolean bIncludeCopiedHubs) {
		if (faHub.getHubDataUnique(thisHub).getLinkToPropertyName() != null) {
			return faHub.getHubDataUnique(thisHub).getLinkToPropertyName();
		}
		if (!bIncludeCopiedHubs) {
			return null;
		}

		Hub<T> hubx = callHubShareGetFirstSharedHub(thisHub, new OAFilter<Hub<T>>() {
			@Override
			public boolean isUsed(Hub obj) {
				Hub h = (Hub) obj;
				if (h == thisHub) {
					return false;
				}
				if (faHub.getHubDataUnique(h).getLinkToPropertyName() != null) {
					return true;
				}
				return false;
			}
		}, bIncludeCopiedHubs, true);
		if (hubx == null) {
			return null;
		}
		return faHub.getHubDataUnique(hubx).getLinkToPropertyName();
	}

	/**
	 * Updates the from-Hub based on changes from the link-to Hub using its current
	 * link configuration.
	 *
	 * @param fromHub   the Hub receiving the update
	 * @param linkToHub the Hub providing the linked value
	 * @param obj       the new value used for updating
	 */
	public <T extends OAObject> void updateLinkedToHub(Hub<T> fromHub, Hub<?> linkToHub, T obj) {
		updateLinkedToHub(fromHub, linkToHub, obj, null);
	}

	/**
	 * Performs a comprehensive update of the from-Hub when the linked-to Hub
	 * changes, handling recursive relationships, positional links, and cascaded
	 * master/detail adjustments.
	 *
	 * @param fromHub         the Hub being updated
	 * @param linkToHub       the Hub that initiated the update
	 * @param obj             the new value to apply
	 * @param changedPropName the property that triggered the update, or null
	 */
	public <T extends OAObject, U extends OAObject> void updateLinkedToHub(final Hub<T> fromHub, Hub<?> linkToHub, final U objOrig, String changedPropName) {
		if (fromHub == null) {
			return;
		}
		
		if (faHub.getHubDataUnique(fromHub).isAutoCreate()) {
			return;
		}

		Object obj = getPropertyValueInLinkedToHub(fromHub, objOrig); // link property value
		if (faHub.getHubDataUnique(fromHub).isLinkPos()) {
			callHubAOSetActiveObject(fromHub, (T) obj, false, false, false); // adjustMaster, bUpdateLink, force
		} else if (obj == null && faHub.getHubDataUnique(fromHub).getLinkFromGetMethod() != null && faHub.getHubDataUnique(fromHub).getLinkToGetMethod() != null) { // 20170919 link from prop to prop
			callHubAOSetActiveObject(fromHub, null, false, false, false); // adjustMaster, bUpdateLink, force
		} else {
			// see if master can be set to null (flag)
			// see if this hub is linked to a master (bForce)

			if (obj != null && faHub.getHubDataUnique(fromHub).getLinkFromGetMethod() == null) {
				// 20200121
				callThreadLocalAddDontAdjustHub(linkToHub);
				try {
					callHubDataGetPos(fromHub, (T) obj, true, false); // adjust master, bUpdateLink
				} finally {
					callThreadLocalRemoveDontAdjustHub(linkToHub);
				}
			} else {
				if (changedPropName == null) {
					// Update Master/Detail hubs for the LinkedFromHub
					// if none of the master hubs have links or details, then set their
					// activeObject to null
					Hub h = fromHub;
					for (; h != null;) {
						if (!faHub.getHubData(h).isDupAllowAddRemove() && h.getSize() == 1) {
							break; // detail hub using an object instead of a Hub
						}

						Hub[] hubs = callHubShareGetAllSharedHubs(h);
						int flag = 0;
						for (int i = 0; i < hubs.length && flag != 5; i++) {
							if (hubs[i] == fromHub) {
								continue;
							}
							if (hubs[i] == fromHub.getLinkHub(false)) {
								flag = 5; // this hub is linked to hubs[i]
							} else if ((hubs[i].getLinkHub(false) != null)
									|| ( faHub.getHubDataUnique(hubs[i]).getVecHubDetail() != null && faHub.getHubDataUnique(hubs[i]).getVecHubDetail().size() > 1)) {
								if (faHub.getHubDataMaster(hubs[i]) == faHub.getHubDataMaster(h)) {
									flag = 5; // || (hubs[i] == h) flag = 5;
								} else if (hubs[i].getMasterHub() == h.getMasterHub()) {
									flag = 1;
								}
							}
						}
						if (flag < 2 && h != fromHub) {
							callHubAOSetActiveObject(h, null, -1, false, false, false); // bUpdateLink, force,bCalledByShareHub
						}
						if (flag != 0) {
							break;
						}

						HubDataMaster dm = callHubDetailGetDataMaster(h);
						h = dm.getMasterHub();
					}
				}
			}

			/* MIGHT not need this new change (reverted to previous
			 ** ==> use the hubEvent.newList to get the change
			// 20110808 if AO is not changing in fromHub then need to set force=true so that the fromHub hub listeners will
			//    be notified.  Example:  if masterHub.ao was null, fromHub.ao=null and fromHub was invalid (because masterHub.ao=null)
			//                           then if masterHub.ao is not null, but fromHub.ao was still null (but now is valid)
			callHubAOSetActiveObject(fromHub, obj,false,false,true); // adjustMaster, bUpdateLink, force
			*/
			///* was:   was checking to see if bForce should be used

			// check for self referring links, where a link is based on master/details that then also have a link back to this hub.
			boolean bForce = false;
			Hub h = fromHub;
			ArrayList<Hub> al = null;
			for (int i = 0; !bForce; i++) {
				// 20120717 endless loop caused by recursive hubs
				if (i > 5) {
					if (al == null) {
						al = new ArrayList<Hub>();
					} else if (al.contains(h)) {
						break;
					}
					al.add(h);
					break;
				}
				HubDataMaster dm = callHubDetailGetDataMaster(h);
				// 20110805 recursive hubs could be changing, where a hub could be now sharing the same hub as it's detailHubs
				if (dm.getMasterHub() == h) {
					break;
				}
				h = dm.getMasterHub();
				if (h == null) {
					break;
				}
				if (h == fromHub.getLinkHub(false)) {
					bForce = true; // if this hub is linked to its masterHub
				}
			}

			// if fromHub AO=null and linkToHub.AO=null then fromHub.isValid
			//             if linkToHub.AO is changed to != null, but fromHub.AO is still null, then need to set bForce=true
			//                so listeners will be notified of the change
			// ex: in SalesOrder there is a hubCustomer linked to it that needs to know when SalesOrder.AO is not null
			if (fromHub.getAO() == null && obj == null) {
				bForce = true;
			}

			// finally :), change the active object in the from hub.
			callHubAOSetActiveObject(fromHub, (T) obj, false, false, bForce); // adjustMaster, bUpdateLink, force
		}
	}

	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(Class clazz);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getMethod")
	public abstract Method callObjectInfoGetMethod(Class clazz, String methodName);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo")
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);

	@OAParentProvided (example = "srvcHub.getHubEventService().removeHubListener")
	public abstract <T extends OAObject> void callHubEventRemoveHubListener(Hub<T> thisHub, HubListener<T> l);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireAfterPropertyChange")
	public abstract <T extends OAObject> void callHubEventFireAfterPropertyChange(final Hub<T> thisHub, final T oaObj, final String propertyName, final Object oldValue,
			final Object newValue, final OALinkInfo linkInfo);

	@OAParentProvided (example = "srvcHub.getHubEventService().addHubListener")
	public abstract <T extends OAObject> void callHubEventAddHubListener(Hub<T> thisHub, HubListener<T> hl);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireAfterChangeActiveObjectEvent")
	public abstract <T extends OAObject> void callHubEventFireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared);

	@OAParentProvided (example = "srvcHub.getHubShareService().getFirstSharedHub")
	public abstract <T extends OAObject> Hub<T> callHubShareGetFirstSharedHub(Hub<T> thisHub, OAFilter<Hub<T>> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO);

	@OAParentProvided (example = "srvcHub.getHubAOService().setActiveObject")
	public abstract <T extends OAObject> void callHubAOSetActiveObject(Hub<T> thisHub, T object, boolean adjustMaster, boolean bUpdateLink, boolean bForce);

	@OAParentProvided (example = "srvcHub.getHubAOService().setActiveObject")
	public abstract <T extends OAObject> void callHubAOSetActiveObject(final Hub<T> thisHub, T object, int pos, boolean bUpdateLink, boolean bForce, boolean bCalledByShareHub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getDataMaster")
	public abstract HubDataMaster callHubDetailGetDataMaster(final Hub<?> thisHub);

	@OAParentProvided (example = "srvcHub.getHubDataService().getPos")
	public abstract <T extends OAObject> int callHubDataGetPos(final Hub<T> thisHub, T object, final boolean adjustMaster, final boolean bUpdateLink);

	@OAParentProvided (example = "srvcHub.getHubShareService().getAllSharedHubs")
	public abstract <T extends OAObject> Hub<T>[] callHubShareGetAllSharedHubs(Hub<T> thisHub);

	@OAParentProvided (example = "srvcThreadLocal.addDontAdjustHub")
	public abstract void callThreadLocalAddDontAdjustHub(Hub<?> hub);

	@OAParentProvided (example = "srvcThreadLocal.removeDontAdjustHub")
	public abstract void callThreadLocalRemoveDontAdjustHub(Hub<?> hub);
}



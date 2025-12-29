package com.viaoa.graph.object;

import java.util.logging.Logger;

import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAPropertyInfo;

public class OAObjectEnumService {
	private static final Logger LOG = Logger.getLogger(OAObjectEnumService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	
    public OAObjectEnumService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    }
	
    public OAObjectService getObjectService() {
    	return srvcObject;
    }

	/**
	 * Retrieves the enumeration name/value pairs defined for the specified
	 * property of the given class. The enumeration metadata is obtained
	 * from the corresponding {@link OAPropertyInfo}.
	 *
	 * @param clazz the class containing the property
	 * @param propertyName the property whose enumeration values are requested
	 * @return a hub containing the name/value entries, or {@code null}
	 *         if the property does not define enumeration metadata
	 */
	public Hub<String> getNameValues(Class clazz, String propertyName) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getNameValues();
	}

	/**
	 * Retrieves the display-form enumeration name/value pairs defined for
	 * the specified property of the given class. This returns the set of
	 * display labels associated with the underlying enumeration values.
	 *
	 * @param clazz the class containing the property
	 * @param propertyName the property whose display enumeration values
	 *                     are requested
	 * @return a hub containing display-name entries, or {@code null}
	 *         if the property does not define enumeration metadata
	 */
	public Hub<String> getDisplayNameValues(Class clazz, String propertyName) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getDisplayNameValues();
	}

}

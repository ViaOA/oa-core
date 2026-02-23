package com.viaoa.graph.service.object;

import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.Hub;
import com.viaoa.model.oa.VString;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPropertyInfo;

public abstract class OAObjectEnumService {
	private static final Logger LOG = Logger.getLogger(OAObjectEnumService.class.getName());

    public OAObjectEnumService() {
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
	public Hub<VString> getNameValues(Class<? extends OAObject> clazz, String propertyName) {
		OAObjectInfo oi = callInfoGetObjectInfo(clazz);
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
	public Hub<VString> getDisplayNameValues(Class<? extends OAObject> clazz, String propertyName) {
		OAObjectInfo oi = callInfoGetObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getDisplayNameValues();
	}

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz)")
	public abstract OAObjectInfo callInfoGetObjectInfo(Class clazz); 
	
}

package com.viaoa.oa.service.object;

import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.object.OAObject;

/**
 * Resolves enum metadata for OAObject properties.
 */
public abstract class OAObjectEnumService {
	private static final Logger LOG = Logger.getLogger(OAObjectEnumService.class.getName());

	/**
	 * Performs OAObjectEnumService behavior for the OA object service.
	 */
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
	public Hub<VEnum> getVEnums(Class<? extends OAObject> clazz, String propertyName) {
		if (clazz == null) return null;
		if (propertyName == null) return null;
		OAObjectInfo oi = callInfoGetObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getVEnums();
	}



	// @OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz)")
	/**
	 * Dependency hook used by this service to infoGetObjectInfo.
	 *
	 * @param clazz method input
	 * @return result value
	 */
	public abstract OAObjectInfo callInfoGetObjectInfo(Class<? extends OAObject> clazz); 
	
}

package com.viaoa.graph.service.object;

import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.model.oa.VEnum;
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
	public Hub<VEnum> getVEnums(Class<? extends OAObject> clazz, String propertyName) {
		OAObjectInfo oi = callInfoGetObjectInfo(clazz);
		OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
		if (pi == null) {
			return null;
		}
		return pi.getVEnums();
	}



	// @OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz)")
	public abstract OAObjectInfo callInfoGetObjectInfo(Class<? extends OAObject> clazz); 
	
}

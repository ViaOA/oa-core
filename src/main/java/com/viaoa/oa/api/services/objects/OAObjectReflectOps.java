package com.viaoa.oa.api.services.objects;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Public OA object reflection and property-path service operations.
 * <p>
 * This interface exposes curated object/property-path helpers intended for
 * application and advanced service use. Lower-level raw property, metadata,
 * loading, and reference operations remain under {@code OA.internal()}.
 */
public interface OAObjectReflectOps {

	/**
	 * Returns the property path from a parent object to a child Hub when one can
	 * be resolved from OA relationship metadata and runtime Hub state.
	 *
	 * @param objParent the parent object
	 * @param hubChild the child Hub
	 * @return the property path from parent to child Hub, or {@code null} if one
	 *         cannot be resolved
	 */
	public String getPathFromMaster(final OAObject objParent, final Hub<?> hubChild);
	
	/**
	 * Resolves a property or property path from an object.
	 *
	 * @param oaObj the source object
	 * @param propPath the property name or property path to resolve
	 * @return the resolved value, or {@code null}
	 */
	public Object getProperty(OAObject oaObj, String propPath);
	
	/**
	 * Resolves a property or property path from the active object of a Hub.
	 *
	 * @param hub the source Hub
	 * @param propPath the property name or property path to resolve
	 * @return the resolved value, or {@code null}
	 */
	public Object getProperty(Hub<?> hub, String propPath);
	
}

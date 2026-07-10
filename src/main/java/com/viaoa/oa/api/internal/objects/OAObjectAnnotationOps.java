package com.viaoa.oa.api.internal.objects;

import java.lang.reflect.Method;

import com.viaoa.annotation.OAMany;
import com.viaoa.object.OAObject;

/**
 * Internal annotation helpers used to resolve OA object metadata from model annotations.
 */
public interface OAObjectAnnotationOps {
	/**
	 * Resolves the OAObject class represented by an {@link com.viaoa.annotation.OAMany} annotation.
	 *
	 * @param annotation the many-link annotation to inspect
	 * @param method the annotated method that declared the relationship
	 * @return the Hub object class for the relationship
	 */
	public Class<? extends OAObject> getHubObjectClass(OAMany annotation, Method method);
	
}

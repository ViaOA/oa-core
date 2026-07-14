package com.viaoa.oa.api.internal.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.viaoa.callback.OACopyCallback;
import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

/**
 * Internal reflection-style operations for OAObject properties, references, loading, copying, and property paths.
 */
public interface OAObjectReflectOps {

	/**
	 * Sets a property value using reflection-style conversion support.
	 *
	 * @param oaObj the object to update
	 * @param propName the property name
	 * @param value the value to store
	 * @param fmt optional format used for conversion
	 */
	public void setProperty(OAObject oaObj, String propName, Object value, String fmt);
	/**
	 * Returns a property value using reflection-style access.
	 *
	 * @param oaObj the object to inspect
	 * @param propName the property name
	 * @return the property value
	 */
	public Object getProperty(OAObject oaObj, String propName);
	/**
	 * Creates a copy of an object, optionally excluding properties.
	 *
	 * @param oaObj the source object
	 * @param excludeProperties property names to exclude
	 * @return the copied object
	 */
	public OAObject createCopy(OAObject oaObj, String[] excludeProperties);
	
	public OAObject _createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback, Map<UUID, OAObject> hmNew);
	
	public OAObject createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback);
	
	/**
	 * Copies values from one object into another.
	 *
	 * @param oaObj the source object
	 * @param newObject the target object
	 * @param excludeProperties property names to exclude
	 * @param copyCallback copy callback used during copy processing
	 */
	public void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback);
	
	/**
	 * Returns a reference Hub for a link property.
	 *
	 * @param oaObj the source object
	 * @param linkPropertyName the link property name
	 * @param sortOrder optional sort order
	 * @param bSequence {@code true} to maintain sequence ordering
	 * @param hubMatch optional Hub used to match existing references
	 * @return the reference Hub
	 */
	public <T extends OAObject> Hub<T> getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence, Hub<T> hubMatch);
	/**
	 * Returns a reference object for a link property.
	 *
	 * @param oaObj the source object
	 * @param linkPropertyName the link property name
	 * @return the reference object
	 */
	public Object getReferenceObject(OAObject oaObj, String linkPropertyName);
	/**
	 * Returns whether a reference object or Hub is null or empty.
	 *
	 * @param oaObj the source object
	 * @param name the reference property name
	 * @return {@code true} if null or empty
	 */
	public boolean isReferenceObjectNullOrEmpty(OAObject oaObj, String name);
	/**
	 * Returns blob data for a reference property.
	 *
	 * @param oaObj the source object
	 * @param linkPropertyName the blob/reference property name
	 * @return the blob bytes
	 */
	public byte[] getReferenceBlob(OAObject oaObj, String linkPropertyName);
	/**
	 * Returns whether a primitive property is marked null.
	 *
	 * @param oaObj the object to inspect
	 * @param prop the primitive property name
	 * @return {@code true} if the primitive property is null-marked
	 */
	public boolean getPrimitiveNull(OAObject oaObj, String prop);
	/**
	 * Sets the null marker for a primitive property.
	 *
	 * @param oaObj the object to update
	 * @param prop the primitive property name
	 * @param b {@code true} to mark null
	 */
	public void setPrimitiveNull(OAObject oaObj, String prop, boolean b);
	
	/**
	 * Loads all applicable references for an object.
	 *
	 * @param oaObj the object whose references are loaded
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @return the number of references loaded
	 */
	public int loadAllReferences(OAObject oaObj, boolean bIncludeCalc);
	/**
	 * Loads one and/or many references for an object.
	 *
	 * @param oaObj the object whose references are loaded
	 * @param bOne {@code true} to load one references
	 * @param bMany {@code true} to load many references
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @return the number of references loaded
	 */
	public int loadAllReferences(OAObject oaObj, boolean bOne, boolean bMany, boolean bIncludeCalc);
	/**
	 * Loads references up to the supplied depth.
	 *
	 * @param oaObj the object whose references are loaded
	 * @param maxLevelsToLoad maximum reference depth
	 * @param additionalOwnedLevelsToLoad additional depth for owned links
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @return the number of references loaded
	 */
	public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc);
	/**
	 * Loads references with depth and count limits.
	 *
	 * @param oaObj the object whose references are loaded
	 * @param maxLevelsToLoad maximum reference depth
	 * @param additionalOwnedLevelsToLoad additional depth for owned links
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @param maxRefsToLoad maximum references to load
	 * @return the number of references loaded
	 */
	public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad);
	/**
	 * Loads references with depth, count, and time limits.
	 *
	 * @param oaObj the object whose references are loaded
	 * @param maxLevelsToLoad maximum reference depth
	 * @param additionalOwnedLevelsToLoad additional depth for owned links
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @param maxRefsToLoad maximum references to load
	 * @param maxEndTime maximum end time in milliseconds
	 * @return the number of references loaded
	 */
	public int loadAllReferences(OAObject oaObj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad, long maxEndTime);
	/**
	 * Loads references using an existing cascade context.
	 *
	 * @param obj the object whose references are loaded
	 * @param maxLevelsToLoad maximum reference depth
	 * @param additionalOwnedLevelsToLoad additional depth for owned links
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @param cascade the cascade context
	 * @param maxRefsToLoad maximum references to load
	 * @return the number of references loaded
	 */
	public int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, OACascade cascade, int maxRefsToLoad);
	
	public <T extends OAObject> T getObject(Class<T> clazz, Object keyValue); // Consider exposing this through the public OA object services if needed.
	/**
	 * Creates a new OAObject instance for the supplied class.
	 *
	 * @param clazz the object class to instantiate
	 * @return the new object instance
	 */
	public <T extends OAObject> T createNewObject(Class<T> clazz);
	/**
	 * Returns whether all applicable references are loaded.
	 *
	 * @param oaObj the object to inspect
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @return {@code true} if all applicable references are loaded
	 */
	public boolean areAllReferencesLoaded(OAObject oaObj, boolean bIncludeCalc);
	/**
	 * Returns whether a Hub reference has been loaded.
	 *
	 * @param oaObj the object to inspect
	 * @param hubPropertyName the Hub property name
	 * @return {@code true} if the Hub reference is loaded
	 */
	public boolean isReferenceHubLoaded(OAObject oaObj, String hubPropertyName);
	/**
	 * Returns names of references that have not been loaded.
	 *
	 * @param obj the object to inspect
	 * @param bIncludeCalc {@code true} to include calculated references
	 * @param exceptPropertyName optional property name to exclude
	 * @param bIncludeLarge {@code true} to include large references
	 * @return unloaded reference names
	 */
	public String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge);
	/**
	 * Returns the property path from a parent object to a child Hub.
	 *
	 * @param oaObjParent the parent object
	 * @param hubChild the child Hub
	 * @return the property path, or {@code null}
	 */
	public String getPathFromMaster(OAObject oaObjParent, Hub<?> hubChild);
	/**
	 * Returns a value from a Hub active object using a property path.
	 *
	 * @param hub the Hub context
	 * @param path the property path to resolve
	 * @return the resolved value
	 */
	public Object getProperty(Hub<?> hub, String path);
	/**
	 * Returns the object key stored for a reference property.
	 *
	 * @param oaObj the object to inspect
	 * @param propertyName the reference property name
	 * @return the reference object key
	 */
	public OAObjectKey getPropertyObjectKey(OAObject oaObj, String propertyName);
	/**
	 * Returns the raw stored value for a reference property.
	 *
	 * @param oaObj the object to inspect
	 * @param name the reference property name
	 * @return the raw reference value
	 */
	public Object getRawReference(OAObject oaObj, String name);
	/**
	 * Returns the property path between two Hubs.
	 *
	 * @param hubParent the parent Hub
	 * @param hubChild the child Hub
	 * @return the property path, or {@code null}
	 */
	public String getPathBetweenHubs(final Hub<?> hubParent, final Hub<?> hubChild);
	
	public <T extends OAObject> void _copyInto(final T oaObj, final T newObject, final String[] excludeProperties,
			final OACopyCallback copyCallback, final Map<UUID, OAObject> hmNew);

}

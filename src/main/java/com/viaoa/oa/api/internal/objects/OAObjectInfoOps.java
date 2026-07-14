package com.viaoa.oa.api.internal.objects;

import java.lang.reflect.Method;

import com.viaoa.hub.Hub;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;

/**
 * Internal metadata lookup operations for OAObject classes, properties, links, methods, and model structure.
 */
public interface OAObjectInfoOps {

	/**
	 * Returns OA metadata for a class.
	 *
	 * @param clazz the object class
	 * @return the OAObjectInfo metadata
	 */
	public OAObjectInfo getOAObjectInfo(Class<?> clazz);
	/**
	 * Returns the object class for a Hub-valued property.
	 *
	 * @param clazz the owning object class
	 * @param propertyName the Hub property name
	 * @return the Hub object class
	 */
	public Class<? extends OAObject> getHubPropertyClass(Class<? extends OAObject> clazz, String propertyName);
	/**
	 * Returns the Java type for a property on an object class.
	 *
	 * @param clazz the object class
	 * @param propertyName the property name
	 * @return the property class
	 */
	public Class<?> getPropertyClass(Class<? extends OAObject> clazz, String propertyName);
	/**
	 * Returns the Java type for a property using existing object metadata.
	 *
	 * @param oi the object metadata
	 * @param propertyName the property name
	 * @return the property class
	 */
	public Class<?> getPropertyClass(OAObjectInfo oi, String propertyName);
	/**
	 * Returns whether a metadata property is Hub-valued.
	 *
	 * @param oi the object metadata
	 * @param propertyName the property name
	 * @return {@code true} if the property is a Hub link
	 */
	public boolean isHubProperty(OAObjectInfo oi, String propertyName);
	/**
	 * Returns calculated-property metadata by name.
	 *
	 * @param oi the object metadata
	 * @param name the calculated property name
	 * @return the calculated-property metadata
	 */
	public OACalcInfo getCalcInfo(OAObjectInfo oi, String name);
	/**
	 * Returns link metadata by property name.
	 *
	 * @param oi the object metadata
	 * @param propertyName the link property name
	 * @return the link metadata
	 */
	public OALinkInfo getLinkInfo(OAObjectInfo oi, String propertyName);

	public OALinkInfo getLinkInfo(Class<? extends OAObject> fromClass, Class<? extends OAObject> toClass);
	
	/**
	 * Returns OA metadata for an object instance.
	 *
	 * @param oaObj the object instance
	 * @return the OAObjectInfo metadata
	 */
	public OAObjectInfo getOAObjectInfo(OAObject oaObj);
	/**
	 * Returns whether a Hub for the supplied link should be cached.
	 *
	 * @param linkInfo the link metadata
	 * @param hub the Hub instance
	 * @return {@code true} if the Hub should be cached
	 */
	public boolean cacheHub(OALinkInfo linkInfo, Hub<?> hub);
	/**
	 * Returns method metadata by name.
	 *
	 * @param oi the object metadata
	 * @param string the method name
	 * @return the matching method, or {@code null}
	 */
	public Method getMethod(OAObjectInfo oi, String string);
	/**
	 * Returns recursive-link metadata for the supplied recursive type.
	 *
	 * @param oi the object metadata
	 * @param type the recursive-link type
	 * @return the recursive-link metadata
	 */
	public OALinkInfo getRecursiveLinkInfo(OAObjectInfo oi, int type);
	/**
	 * Returns a reflection method by class and name.
	 *
	 * @param clazz the class to inspect
	 * @param methodName the method name
	 * @return the matching method, or {@code null}
	 */
	public Method getMethod(Class<?> clazz, String methodName);
	/**
	 * Returns reverse-link metadata for a link.
	 *
	 * @param li the link metadata
	 * @return the reverse link metadata
	 */
	public OALinkInfo getReverseLinkInfo(OALinkInfo li);
	/**
	 * Returns OA metadata for a class.
	 *
	 * @param clazz the object class
	 * @return the OAObjectInfo metadata
	 */
	public OAObjectInfo getObjectInfo(Class<?> clazz);
	/**
	 * Returns whether a link represents a many-to-many relationship.
	 *
	 * @param li the link metadata
	 * @return {@code true} for many-to-many links
	 */
	public boolean isMany2Many(OALinkInfo li);
	/**
	 * Returns link metadata for an object class and property.
	 *
	 * @param clazz the object class
	 * @param property the link property name
	 * @return the link metadata
	 */
	public OALinkInfo getLinkInfo(Class<? extends OAObject> clazz, String property);
	/**
	 * Returns method metadata by name and argument count.
	 *
	 * @param oi the object metadata
	 * @param methodName the method name
	 * @param argumentCount the required argument count
	 * @return the matching method, or {@code null}
	 */
	public Method getMethod(OAObjectInfo oi, String methodName, int argumentCount);
	/**
	 * Returns property metadata by name.
	 *
	 * @param oi the object metadata
	 * @param propertyName the property name
	 * @return the property metadata
	 */
	public OAPropertyInfo getPropertyInfo(OAObjectInfo oi, String propertyName);
	/**
	 * Returns whether metadata represents a POJO singleton object.
	 *
	 * @param toObjectInfo the object metadata
	 * @return {@code true} for POJO singleton metadata
	 */
	public boolean isPojoSingleton(OAObjectInfo toObjectInfo);
	/**
	 * Returns all OAObject classes known to metadata.
	 *
	 * @return the known OAObject classes
	 */
	public Class<? extends OAObject>[] getAllClasses();

	
}

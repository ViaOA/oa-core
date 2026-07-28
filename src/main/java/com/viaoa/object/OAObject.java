/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.object;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.callback.OAObjectCallback.Type;
import com.viaoa.compare.OACompare;
import com.viaoa.compare.match.OAMatchNotExist;
import com.viaoa.converter.OAConv;
import com.viaoa.converter.OAConverter;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datetime.OADateTime;
import com.viaoa.find.OAHierFinder;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.log.OALogger;
import com.viaoa.metadata.OAFkeyInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.oa.OA;
import com.viaoa.oa.service.object.OAObjectParentService;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.select.OASelect;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteServerInterface;

/**
 * Base class for OA model objects.
 * <p>
 * {@code OAObject} supplies the runtime behavior that lets generated and hand
 * written model classes participate in the OA model: property storage, change
 * notification, identity, lifecycle flags, lazy loading, rule/callback checks,
 * persistence hooks, synchronization, serialization, and Hub relationship
 * support.
 * <p>
 * Application entities normally extend {@code OAObject}. The object itself does
 * not contain datasource-specific code; persistence, metadata, rules, cache,
 * sync, and Hub coordination are delegated through the owning {@link OA}
 * runtime and its services.
 * <p>
 * OA model relationships are represented by object references and {@link Hub}
 * collections. Property paths, metadata, and rules allow the runtime to keep
 * object state, UI bindings, generated application behavior, and distributed
 * updates coordinated from the model definition.
 *
 * @see Hub
 * @see OAObjectInfo
 * @see OAObjectKey
 * @see OA
 */
public class OAObject implements java.io.Serializable, Comparable<Object> {

	
	/**
	 * Serialization version identifier used by Java’s built-in serialization
	 * mechanism to validate compatibility when deserializing OAObject instances.
	 */
	private static final long serialVersionUID = 1L; // internally used by Java Serialization to identify this version of OAObject.

	/**
	 * Framework version string assigned during class initialization. Used for
	 * diagnostics, logging, and compatibility checks.
	 */
	private static final String oaversion;

	/**
	 * Returns the OA framework version string associated with this build.
	 * <p>
	 * The value is initialized once during class loading and typically reflects
	 * the Maven artifact version or internal build identifier. It is used
	 * primarily for logging, diagnostics, and compatibility checks.
	 *
	 * @return the OA framework version for this runtime
	 */
	public static String getOAVersion() {
		return oaversion;
	}

	/**
	 * System-wide logger used for tracking OAObject-level events and diagnostic
	 * messages.
	 */
	public static final Logger OALOG = OALogger.getLogger("OAObject");

	/**
	 * Class-specific logger used internally for OAObject-related diagnostics.
	 */
	private static final Logger LOG = OALogger.getLogger(OAObject.class);
	
	
	static {
		// oaversion
	    
	    // pom version: 4.0.0
		String ver = "4.0.0.202606260";
		
		/*
		 *  previous:
		 *  String ver = "3.7.0.202104100";
		 *  String ver = "3.7.1.202202250";
		 *  String ver = "3.7.2.202206160";
         *  String ver = "3.7.3.202212260";
         *  String ver = "3.7.4.202310070";
         *  String ver = "3.7.5.202310220";
         *  String ver = "3.7.6.202311270";
         *  String ver = "3.7.7.202402260";
         *  String ver = "3.7.8.202405070";
         *  String ver = "3.7.9.202407150";
         *  String ver = "3.7.10.202409160";
         *  String ver = "3.7.11.202504050";
         *  String ver = "3.7.12.202506230";
		 */
		/*
		try {
		    InputStream resourceAsStream = OAObject.class.getResourceAsStream("/META-INF/maven/com.viaoa/oa/pom.properties");
		    Properties props = new Properties();
		    props.load(resourceAsStream);
		
		    // String g = props.getProperty("groupId");
		    // String a = props.getProperty("artifactId");
		    ver = props.getProperty("version");
		}
		catch (Exception e) {
		}
		*/
		oaversion = ver;
		LOG.config("oa-core version=" + oaversion);
	}


	/**
	 * Globally unique identifier for this OAObject instance. Used to enforce
	 * single-instance identity across the OA model.
	 */
	protected UUID guid; // global identifier for this object
	
//	protected volatile OAObjectKey objectKey; // Object identifier

	/**
	 * Tracks whether this object has unsaved property changes. Initialized to
	 * true for newly constructed objects.
	 */
	protected volatile boolean changedFlag = true; // flag to know if this object has been changed

	/**
	 * Indicates whether the object is newly created and not yet saved or loaded
	 * from a datasource.
	 */
	protected volatile boolean newFlag = true; // flag to know if this object is new (not yet saved).  The object key properties can be changed as long as isNew is true.
	
	/**
	 * Bit-array tracking which primitive properties are currently null. Indexed
	 * according to OAObjectInfo’s primitive-property ordering.
	 */
	protected byte[] nulls; // keeps track of which primitive type properties that are NULL. Uses bit position, based on OAObjectInfo getPrimitiveProperties() position
	
	/**
	 * Flag indicating whether the object has been logically deleted according to
	 * OA deletion rules.
	 */
	protected volatile boolean deletedFlag;

	/**
	 * List of hub references in which this object is a member. Stored as weak
	 * references so unused hubs can be garbage-collected.
	 */
	protected transient volatile WeakReference<Hub<?>>[] weakhubs;

	/**
	 * Link/reference properties that have been loaded. Stores uppercase name of property. Possible values: ONE: OAObjectKey (by calling
	 * setProperty(), the value used will be converted to an OAObjectKey OAObject for the value of the reference MANY: WeakReference to Hub.
	 * The objects in the Hub can be OAObjectKey values that will automatically be retrieved and converted to the correct class of object.
	 */

	/**
	 * Storage array for simple and link properties. Elements contain OAObject,
	 * Hub, OAObjectKey, or auxiliary metadata values.
	 */
	protected volatile transient Object[] properties; // stores references (oaobj, hub, oaobjkey), or misc property for object.  ex: [0]="Employee" [1]=Emp [2]="Order" [3]=oakey

	/**
	 * Cascade constant indicating that no linked objects should participate in
	 * change, save, or delete propagation.
	 */
	public static final int CASCADE_NONE = 0;

	/**
	 * Cascade constant meaning that only links configured with cascade rules
	 * participate in propagation. Default for save/delete operations.
	 */
	public static final int CASCADE_LINK_RULES = 1;

	/**
	 * Cascade constant specifying that only owned links should participate in
	 * cascade operations.
	 */
	public static final int CASCADE_OWNED_LINKS = 2;

	/**
	 * Cascade constant indicating that all link relationships are included during
	 * cascade traversal regardless of metadata.
	 */
	public static final int CASCADE_ALL_LINKS = 4;

	/**
	 * Global counter tracking the number of OAObject instances created during the
	 * lifetime of the application runtime.
	 */
	public static final AtomicInteger cntNew = new AtomicInteger();

	/**
	 * Constructs a new {@code OAObject} instance and performs framework-level
	 * initialization.
	 * <p>
	 * Initialization is delegated to {@link OAObjectDelegate#initialize(OAObject)},
	 * which is responsible for:
	 * <ul>
	 *   <li>assigning a globally unique GUID for this instance,</li>
	 *   <li>initializing internal property storage and metadata structures,</li>
	 *   <li>registering the object with the OA model and caching system,</li>
	 *   <li>marking the object as {@code new} and {@code changed} so it can be
	 *       detected by persistence and synchronization layers,</li>
	 *   <li>setting up any property-level or link-level runtime delegates.</li>
	 * </ul>
	 * Newly constructed objects always begin in the “new” and “changed” state until
	 * explicitly saved or committed by the application or datasource layer.
	 */
	public OAObject() {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().initialize().initialize(this);

		cntNew.incrementAndGet();

		// 20141209 removed, since it was creating dup oaObjKeys, one when putting in cache, then clearing it, and then
		//    creating another the next time that OAObj calls for it.
		// 20141127 Note: call oaObject.toString(), until the object is loaded, since it will create an objectKey with Id=0
		//if (objectKey != null) objectKey = null; // in case it was generated before the Id was loaded.
	}

	
	/**
	 * Custom Java serialization hook used when deserializing an {@code OAObject}.
	 * <p>
	 * This method must remain {@code private} so that the Java serialization
	 * mechanism will invoke it automatically. All deserialization behavior is
	 * delegated to
	 * {@link OAObjectSerializeDelegate#_readObject(OAObject, java.io.ObjectInputStream)},
	 * which is responsible for:
	 * <ul>
	 *   <li>restoring serialized property and link values,</li>
	 *   <li>reinitializing transient or delegate-managed state,</li>
	 *   <li>re-attaching the object to the OA cache and OA model based on its GUID,</li>
	 *   <li>ensuring that identity and reference rules are preserved.</li>
	 * </ul>
	 * No application code should call this method directly.
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        OA oa = OARuntime.oa(this);
        oa.internal().objects().serialize().readObject(this, in);
	}

	/**
	 * Ensures that deserialized {@code OAObject} instances preserve OA's
	 * single-instance-per-GUID identity rule.
	 * <p>
	 * During Java deserialization, this method is invoked after the object has been
	 * read from the stream. It delegates to
	 * {@link OAObjectSerializeDelegate#_readResolve(OAObject)}, which:
	 * <ul>
	 *   <li>looks up an existing instance with the same GUID in the OA cache,</li>
	 *   <li>returns that existing instance when found, ensuring identity consistency,</li>
	 *   <li>otherwise registers this new instance and returns it.</li>
	 * </ul>
	 * This mechanism avoids duplicate {@code OAObject} instances appearing in the
	 * OA model after serialization round-trips. Application code should not
	 * call this method directly.
	 *
	 * @return the canonical {@code OAObject} instance for this GUID
	 */
	protected Object readResolve() throws ObjectStreamException {
        OA oa = OARuntime.oa(this);
		Object obj = oa.internal().objects().serialize().readResolve(this);
		return obj;
	}

	/**
	 * Custom Java serialization hook used when writing an {@code OAObject} to an
	 * output stream.
	 * <p>
	 * This method must remain {@code private} so that the Java serialization
	 * subsystem invokes it automatically. All serialization logic is delegated to
	 * {@link OAObjectSerializeDelegate#_writeObject(OAObject, java.io.ObjectOutputStream)},
	 * which is responsible for:
	 * <ul>
	 *   <li>selecting which simple properties and link references are included,</li>
	 *   <li>encoding values according to OA metadata and formatting rules,</li>
	 *   <li>avoiding infinite recursion during link traversal,</li>
	 *   <li>recording enough state to allow correct reattachment to the OA model
	 *       during deserialization.</li>
	 * </ul>
	 * Applications should not call this method directly; it is part of the standard
	 * Java serialization workflow.
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        OA oa = OARuntime.oa(this);
        oa.internal().objects().serialize().writeObject(this, stream);
	}

	/**
	 * Convenience overload for setting a boolean property by name.
	 * <p>
	 * The value is converted to the internal boolean representation used by
	 * OA and delegated to {@link OAObjectReflectDelegate#setProperty(OAObject, String, Object, String)}.
	 * The reflect delegate is responsible for:
	 * <ul>
	 *   <li>resolving the property by name and metadata,</li>
	 *   <li>performing any required type conversion,</li>
	 *   <li>firing before/after property change events,</li>
	 *   <li>marking the object as changed,</li>
	 *   <li>updating reverse links and any associated hubs when the property
	 *       participates in a link.</li>
	 * </ul>
	 *
	 * @param propName the name of the property to update
	 * @param value    the new boolean value to assign
	 */
	public void setProperty(String propName, boolean value) {
		final OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, value ? Boolean.TRUE : Boolean.FALSE, null);
	}

	/**
	 * Convenience overload for setting an {@code int} property by name.
	 * <p>
	 * The value is wrapped as an {@link Integer} and delegated to
	 * {@link OAObjectReflectDelegate#setProperty(OAObject, String, Object, String)},
	 * which performs the standard OA property update workflow:
	 * <ul>
	 *   <li>metadata-based property resolution,</li>
	 *   <li>type checking and conversion as needed,</li>
	 *   <li>before/after property change notifications,</li>
	 *   <li>change-tracking and lifecycle flag updates,</li>
	 *   <li>reverse-link and Hub maintenance for link properties.</li>
	 * </ul>
	 *
	 * @param propName the name of the property to update
	 * @param value    the new integer value to assign
	 */
	public void setProperty(String propName, int value) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, Integer.valueOf(value), null);
	}

	/**
	 * Convenience overload for setting a {@code long} property by name.
	 * <p>
	 * The value is wrapped as a {@link Long} and delegated to
	 * {@link OAObjectReflectDelegate#setProperty(OAObject, String, Object, String)}.
	 * The reflect delegate handles:
	 * <ul>
	 *   <li>property lookup using OA metadata,</li>
	 *   <li>conversion from the supplied value type to the declared property type,</li>
	 *   <li>event firing for before/after change,</li>
	 *   <li>change-tracking and dirty-state management,</li>
	 *   <li>reverse-link updates and Hub notifications when appropriate.</li>
	 * </ul>
	 *
	 * @param propName the name of the property to update
	 * @param value    the new long value to assign
	 */
	public void setProperty(String propName, long value) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, Long.valueOf(value), null);
	}

	/**
	 * Convenience overload for setting a {@code double} property by name.
	 * <p>
	 * The value is wrapped as a {@link Double} and delegated to
	 * {@link OAObjectReflectDelegate#setProperty(OAObject, String, Object, String)}.
	 * All of the normal OA property update behavior is applied, including:
	 * <ul>
	 *   <li>metadata-driven property resolution,</li>
	 *   <li>type enforcement and conversion,</li>
	 *   <li>before/after property change event propagation,</li>
	 *   <li>marking the object as changed,</li>
	 *   <li>cascading and Hub updates for linked properties.</li>
	 * </ul>
	 *
	 * @param propName the name of the property to update
	 * @param value    the new double value to assign
	 */
	public void setProperty(String propName, double value) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, Double.valueOf(value), null);
	}

	/**
	 * Convenience overload for setting a property by name without an explicit
	 * format string.
	 * <p>
	 * The supplied value is passed directly to
	 * {@link OAObjectReflectDelegate#setProperty(OAObject, String, Object, String)}
	 * along with a {@code null} format. The delegate is responsible for:
	 * <ul>
	 *   <li>resolving the property by name and metadata,</li>
	 *   <li>performing any necessary type conversion (for example, from
	 *       {@link String} or identifier types to the declared property type),</li>
	 *   <li>handling reference assignments for link properties,</li>
	 *   <li>firing before/after change callbacks and updating change tracking.</li>
	 * </ul>
	 *
	 * @param propName the name of the property to update
	 * @param value    the new value to assign; may be {@code null}
	 */
	public void setProperty(String propName, Object value) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, value, null);
	}

	/**
	 * Sets the specified property to {@code null}.
	 * <p>
	 * This is equivalent to calling
	 * {@code setProperty(propName, null, null)} and will clear any stored value
	 * for the property. The underlying delegate will:
	 * <ul>
	 *   <li>update the property's null state and any primitive-null tracking,</li>
	 *   <li>fire before/after property change events,</li>
	 *   <li>mark the object as changed when appropriate,</li>
	 *   <li>apply any link and cascade rules if the property participates in a link.</li>
	 * </ul>
	 *
	 * @param propName the name of the property to clear
	 */
	public void setNull(String propName) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, null, null);
	}

	/**
	 * Sets an internal flag to know that a primitive property type is null or not.
	 * Note: no event is sent, no value is set.
	 * @param propName name of property
	 * @param b true or false
	 */
	protected void setPrimitiveNull(String propName, boolean b) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setPrimitiveNull(this, propName, b);
	}
	
	
	/**
	 * Generic entry point for setting any property by name, with an optional
	 * format hint for type conversion.
	 * <p>
	 * The call is delegated to
	 * {@link OAObjectReflectDelegate#setProperty(OAObject, String, Object, String)},
	 * which:
	 * <ul>
	 *   <li>resolves the property definition from metadata,</li>
	 *   <li>uses the {@code fmt} string (when provided) as a hint for converting
	 *       values such as dates, times, or formatted numbers,</li>
	 *   <li>handles assignment to simple, enum, and reference properties,</li>
	 *   <li>fires before/after property change callbacks,</li>
	 *   <li>updates change tracking and cascade state.</li>
	 * </ul>
	 *
	 * @param propName the name (or property path) of the property to update
	 * @param value    the new value to assign
	 * @param fmt      optional format string used for type conversion; may be {@code null}
	 */
	public void setProperty(String propName, Object value, String fmt) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().setProperty(this, propName, value, fmt);
	}

	/**
	 * Returns the value of the named property on this object.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getProperty(OAObject, String)} which:
	 * <ul>
	 *   <li>locates the property definition using OA metadata,</li>
	 *   <li>retrieves the current stored value,</li>
	 *   <li>resolves indirection through property paths when supported,</li>
	 *   <li>performs any necessary type coercion for wrapped primitives,</li>
	 *   <li>handles reference properties by returning the linked {@code OAObject}.</li>
	 * </ul>
	 * No formatting or conversion is applied; callers receive the raw stored
	 * property value as maintained by the OA framework.
	 *
	 * @param propName the name (or property path, when supported) of the property to retrieve
	 * @return the current value of the property, or {@code null} if no value is set
	 */
	public Object getProperty(String propName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getProperty(this, propName);
	}

	/**
	 * Returns the value of the specified property as a {@link String} using
	 * the default OA formatting rules.
	 * <p>
	 * This is a convenience method that delegates to
	 * {@link #getPropertyAsString(String, String)} with a {@code null} format.
	 * The actual conversion is performed by {@link OAConverter#toString(Object, String)}
	 * in the {@code (propName, fmt)} overload.
	 *
	 * @param propName the name (or supported property path) of the property
	 * @return the formatted String value of the property, or {@code ""} when the
	 *         underlying value is {@code null} and no explicit format is supplied
	 */
	public String getPropertyAsString(String propName) {
		return getPropertyAsString(propName, null);
	}

	/**
	 * Returns the value of the specified property as a {@link String}, with control
	 * over whether OA's default formatting rules are applied.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getPropertyAsString(OAObject, String, boolean)},
	 * which:
	 * <ul>
	 *   <li>retrieves the raw property value using OA metadata,</li>
	 *   <li>converts the value to a String using the appropriate OAConverter,</li>
	 *   <li>applies default formatting (dates, times, numbers) when
	 *       {@code bUseDefaultFormatting} is {@code true},</li>
	 *   <li>returns the unformatted converter output when
	 *       {@code bUseDefaultFormatting} is {@code false}.</li>
	 * </ul>
	 * This method never throws for missing or undefined properties; it returns an
	 * empty String when a {@code null} value is encountered and default formatting
	 * is not suppressed.
	 *
	 * @param propName the name (or supported property path) of the property
	 * @param bUseDefaultFormatting whether OA's default formatting rules should be applied
	 * @return the property value as a String, possibly unformatted depending on the flag
	 */
	public String getPropertyAsString(String propName, boolean bUseDefaultFormatting) {
		Object obj = getProperty(propName);
		return OAConverter.toString(obj, bUseDefaultFormatting);
	}

	/**
	 * Returns the value of the specified property as a {@link String} using the
	 * supplied format string.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getPropertyAsString(OAObject, String, String)},
	 * which:
	 * <ul>
	 *   <li>resolves the property definition through OA metadata,</li>
	 *   <li>retrieves the current property value,</li>
	 *   <li>uses the provided {@code fmt} hint to convert the value to a String
	 *       (e.g., date/time patterns or numeric formatting),</li>
	 *   <li>applies OAConverter formatting rules when applicable.</li>
	 * </ul>
	 * When the underlying value is {@code null}, this method returns an empty
	 * String. No exceptions are thrown for undefined properties.
	 *
	 * @param propName the name (or supported property path) of the property
	 * @param fmt      optional formatting pattern to guide String conversion;
	 *                 may be {@code null}
	 * @return the formatted String value, or {@code ""} when the property value is {@code null}
	 */
    public String getPropertyAsString(String propName, String fmt) {
        Object obj = getProperty(propName);
        if (obj == null) {
            if (fmt == null || fmt.length() == 0) return ""; 
        }
        return OAConverter.toString(obj, fmt);
    }
	
    /**
     * Returns the value of the specified property as a {@link String} using an
     * optional format string and a caller-supplied value to return when the
     * underlying property is {@code null}.
     * <p>
     * This method delegates to
     * {@link OAObjectReflectDelegate#getPropertyAsString(OAObject, String, String, String)},
     * which:
     * <ul>
     *   <li>locates the property definition using OA metadata,</li>
     *   <li>retrieves the current property value,</li>
     *   <li>converts the value to a String using the supplied {@code fmt}
     *       formatting pattern (when provided),</li>
     *   <li>returns {@code nullValue} when the property value is {@code null},</li>
     *   <li>otherwise applies OAConverter’s type-appropriate String conversion.</li>
     * </ul>
     * This method is typically used by UI layers and reporting utilities where a
     * non-empty placeholder is preferred over an empty String for {@code null}
     * values.
     *
     * @param propName  the name (or supported property path) of the property
     * @param fmt       optional formatting pattern used for String conversion; may be {@code null}
     * @param nullValue the value to return when the underlying property is {@code null}
     * @return the formatted String value, or {@code nullValue} when the property is {@code null}
     */
	public String getPropertyAsString(String propName, String fmt, String nullValue) {
		Object obj = getProperty(propName);
		if (obj == null) {
			return nullValue;
		}
		return OAConverter.toString(obj, fmt);
	}

	/**
	 * Removes the specified property value from this object, reverting it to an
	 * unassigned (null) state.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#removeProperty(OAObject, String)}, which:
	 * <ul>
	 *   <li>locates the property definition using OA metadata,</li>
	 *   <li>clears the stored value and any primitive-null tracking,</li>
	 *   <li>fires before/after property-change events,</li>
	 *   <li>marks the object as changed for persistence and sync layers,</li>
	 *   <li>updates reverse links and Hub state when the property participates
	 *       in a reference link.</li>
	 * </ul>
	 * Removing a property is equivalent to assigning {@code null} but provides a
	 * clearer semantic distinction for callers that explicitly want to clear the
	 * property rather than set it to a meaningful null value.
	 *
	 * @param name the name of the property to remove
	 */
	public void removeProperty(String name) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().property().removeProperty(this, name, true);
	}

	/**
	 * Determines whether a proposed property change is valid for this object.
	 * <p>
	 * This method is invoked by the OA framework before a property value is
	 * updated. Subclasses may override it to enforce business rules, validation
	 * logic, or conditional constraints on specific properties. Common use cases
	 * include:
	 * <ul>
	 *   <li>preventing changes when the object is in a locked or finalized state,</li>
	 *   <li>validating numeric ranges or string length constraints,</li>
	 *   <li>enforcing immutability of certain properties after creation,</li>
	 *   <li>ensuring reference assignments meet domain prerequisites.</li>
	 * </ul>
	 * The default implementation always returns {@code true}, allowing all
	 * property changes to proceed.
	 *
	 * @param propertyName the name of the property being modified
	 * @param oldValue     the current value of the property (may be {@code null})
	 * @param newValue     the proposed new value for the property (may be {@code null})
	 * @return {@code true} if the property change is permitted; {@code false} to
	 *         cancel the update
	 */
	public boolean isValidPropertyChange(String propertyName, Object oldValue, Object newValue) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().rules().getVerifyPropertyChangeCallbackOnly(this, propertyName, oldValue, newValue);
	}

	/**
	 * Evaluates whether a proposed property change is permitted according to the
	 * OA callback and metadata rules.
	 * <p>
	 * This convenience overload retrieves the current value of the specified
	 * property and delegates to
	 * the OA object rules engine
	 * using {@link OAObjectCallback#CHECK_CallbackMethod} as the evaluation mode.
	 * The delegate performs all validation, including:
	 * <ul>
	 *   <li>invoking any registered {@code beforeChange} callbacks,</li>
	 *   <li>enforcing metadata-defined rules for the property,</li>
	 *   <li>validating cross-object and link-based constraints,</li>
	 *   <li>returning {@code true} only when the change is allowed.</li>
	 * </ul>
	 *
	 * @param propertyName the name of the property whose value is being updated
	 * @param newValue     the proposed new value for the property
	 * @return {@code true} if the change is allowed; {@code false} otherwise
	 */
    public boolean isValidPropertyChange(String propertyName, Object newValue) {
		OA oa = OARuntime.oa(this);
        Object oldValue = getProperty(propertyName);
        return oa.internal().objects().rules().getVerifyPropertyChangeCallbackOnly(this, propertyName, oldValue, newValue);
    }

    /**
     * Retrieves the {@link OAObjectCallback} describing whether a proposed
     * property change is allowed, including any validation messages or metadata
     * returned by the callback system.
     * <p>
     * This method delegates to the OA object rules engine using {@link OAObjectCallback#CHECK_CallbackMethod} as the evaluation mode.
     * The returned callback object includes:
     * <ul>
     *   <li>whether the change is permitted,</li>
     *   <li>any message or reason when the change is rejected,</li>
     *   <li>any exception provided by the callback implementation,</li>
     *   <li>additional metadata relevant to the validation.</li>
     * </ul>
     * Callers typically use this method when they need full detail rather than a
     * simple boolean result.
     *
     * @param propertyName the property being updated
     * @param oldValue     the property's current value
     * @param newValue     the proposed new value
     * @return the callback result describing the validation outcome
     */
	public OAObjectCallback getIsValidPropertyChangeObjectCallback(String propertyName, Object oldValue, Object newValue) {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getVerifyPropertyChangeCallbackOnlyObjectCallback(this, propertyName, oldValue, newValue);
		return eq;
	}
	
	/**
	 * Retrieves the {@link OAObjectCallback} describing whether a proposed
	 * property change is allowed, using the current property value as the
	 * existing (old) value.
	 * <p>
	 * This is a convenience overload that resolves the current value of the
	 * specified property and delegates to
	 * the OA object rules engine
	 * using {@link OAObjectCallback#CHECK_CallbackMethod} as the evaluation mode.
	 * The returned callback object provides:
	 * <ul>
	 *   <li>whether the change is permitted,</li>
	 *   <li>a descriptive message when the change is rejected,</li>
	 *   <li>any associated exception,</li>
	 *   <li>additional metadata generated by callback logic.</li>
	 * </ul>
	 * This method is typically used when callers require detailed feedback rather
	 * than a simple boolean result.
	 *
	 * @param propertyName the property being updated
	 * @param newValue     the proposed new value for the property
	 * @return the callback result describing the validation outcome
	 */
    public OAObjectCallback getIsValidPropertyChangeObjectCallback(String propertyName, Object newValue) {
        Object oldValue = getProperty(propertyName);
		OA oa = OARuntime.oa(this);
        OAObjectCallback eq = oa.internal().objects().rules().getVerifyPropertyChangeCallbackOnlyObjectCallback(this, propertyName, oldValue, newValue);
        return eq;
    }

    /**
     * Determines whether the specified property is enabled according to OA's
     * callback and metadata rules.
     * <p>
     * This method delegates to the OA object rules engine using
     * {@link OAObjectCallback#CHECK_ALL} as the evaluation mode. The
     * delegate evaluates:
     * <ul>
     *   <li>property-level enable/disable rules,</li>
     *   <li>object-level rules affecting all properties,</li>
     *   <li>any active {@link OAObjectCallback} implementations,</li>
     *   <li>metadata-defined constraints.</li>
     * </ul>
     * A return value of {@code true} indicates that the property is enabled and
     * can be modified or interacted with; {@code false} indicates that the
     * property is disabled for the current context.
     *
     * @param propertyName the name of the property to evaluate
     * @return {@code true} if the property is enabled; {@code false} otherwise
     */
	public boolean isEnabled(String propertyName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().rules().getAllowEnabled(null, this, propertyName);
	}

	/**
	 * Retrieves the {@link OAObjectCallback} describing whether the specified
	 * property is enabled, including detailed callback metadata.
	 * <p>
	 * This method delegates to the OA object rules engine using
     * {@link OAObjectCallback#CHECK_ALL} as the evaluation mode. The
	 * returned callback object provides:
	 * <ul>
	 *   <li>whether the property is enabled,</li>
	 *   <li>an explanatory message when disabled,</li>
	 *   <li>any associated exception or diagnostic detail,</li>
	 *   <li>contextual metadata supplied by callback logic.</li>
	 * </ul>
	 * Callers typically use this method when they require detailed information
	 * about the enable/disable decision rather than a simple boolean value.
	 *
	 * @param propertyName the property being evaluated
	 * @param oldValue     the current value of the property (not used by delegate)
	 * @param newValue     the proposed new value for the property (not used by delegate)
	 * @return the callback object describing the enablement decision
	 */
	public OAObjectCallback getIsEnabledObjectCallback(String propertyName, Object oldValue, Object newValue) {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getAllowEnabledObjectCallback(null, this, propertyName);
		return eq;
	}

	/**
	 * Determines whether this object as a whole is enabled according to OA's
	 * callback and metadata rules.
	 * <p>
	 * This method delegates to the OA object rules engine using
	 * {@link OAObjectCallback#CHECK_ALL} as the evaluation mode and a
	 * {@code null} property name. This signals the delegate to evaluate
	 * object-level enablement rules rather than property-specific rules.
	 * <p>
	 * The delegate evaluates:
	 * <ul>
	 *   <li>object-level enable/disable constraints,</li>
	 *   <li>active callbacks that may enforce dynamic rules,</li>
	 *   <li>metadata-defined enablement logic,</li>
	 *   <li>contextual conditions such as read-only states.</li>
	 * </ul>
	 *
	 * @return {@code true} if the object is enabled; {@code false} if it is
	 *         disabled for the current context
	 */
	public boolean isEnabled() {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().rules().getAllowEnabled(null, this, null);
	}

	/**
	 * Retrieves the {@link OAObjectCallback} describing whether this object,
	 * as a whole, is enabled for interaction.
	 * <p>
	 * This method delegates to the OA object rules engine using
	 * {@link OAObjectCallback#CHECK_ALL} as the evaluation mode and a
	 * {@code null} property name. This instructs the delegate to evaluate
	 * object-level enablement rules instead of property-specific rules.
	 * <p>
	 * The returned callback contains:
	 * <ul>
	 *   <li>whether the object is enabled,</li>
	 *   <li>any explanatory message when disabled,</li>
	 *   <li>any exception or diagnostic information produced by callbacks,</li>
	 *   <li>contextual metadata related to the evaluation.</li>
	 * </ul>
	 *
	 * @return a callback object describing the enablement result for this object
	 */
	public OAObjectCallback getIsEnabledObjectCallback() {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getAllowEnabledObjectCallback(null, this, null);
		return eq;
	}

	/**
	 * Determines whether the specified property is visible according to OA's
	 * visibility rules and callback system.
	 * <p>
	 * This method delegates to the OA object rules engine using a {@code null}
	 * callback context and the provided property name.
	 * The delegate evaluates:
	 * <ul>
	 *   <li>property-level metadata controlling visibility,</li>
	 *   <li>object-level rules that may hide or expose groups of properties,</li>
	 *   <li>dynamic {@link OAObjectCallback} handlers,</li>
	 *   <li>any runtime conditions affecting UI visibility.</li>
	 * </ul>
	 * A return value of {@code true} indicates that the property should be visible
	 * to callers such as UI components or reporting tools.
	 *
	 * @param propertyName the name of the property whose visibility is being evaluated
	 * @return {@code true} if the property is visible; {@code false} otherwise
	 */
	public boolean isVisible(String propertyName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().rules().getAllowVisible(null, this, propertyName);
	}

	/**
	 * Retrieves the {@link OAObjectCallback} containing detailed visibility
	 * information for the specified property.
	 * <p>
	 * This method delegates to the OA object rules engine using a {@code null}
	 * callback context and the supplied property name.
	 * The returned callback object reports:
	 * <ul>
	 *   <li>whether the property is visible,</li>
	 *   <li>any message or rationale when visibility is denied,</li>
	 *   <li>any exception produced by callback validation,</li>
	 *   <li>additional visibility-related metadata.</li>
	 * </ul>
	 * This method is typically used when callers require more information than
	 * a simple boolean result.
	 *
	 * @param propertyName the property whose visibility is being evaluated
	 * @return the callback object describing the visibility result
	 */
	public OAObjectCallback getIsVisibleObjectCallback(String propertyName) {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getAllowVisibleObjectCallback(null, this, propertyName);
		return eq;
	}

	/**
	 * Determines whether this object, as a whole, is visible according to OA's
	 * visibility rules and callback evaluations.
	 * <p>
	 * This method delegates to the OA object rules engine using a {@code null} property name, which instructs the delegate to evaluate
	 * object-level visibility rather than property-specific visibility.
	 * <p>
	 * The delegate considers:
	 * <ul>
	 *   <li>object-level metadata controlling visibility,</li>
	 *   <li>dynamic callback logic that may hide or expose the object,</li>
	 *   <li>any contextual runtime conditions affecting visibility.</li>
	 * </ul>
	 *
	 * @return {@code true} if the object is visible; {@code false} otherwise
	 */
	public boolean isVisible() {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().rules().getAllowVisible(null, this, null);
	}

	/**
	 * Retrieves the {@link OAObjectCallback} containing detailed visibility
	 * information for this object as a whole.
	 * <p>
	 * This method delegates to the OA object rules engine using a {@code null} property name, which signals that object-level
	 * visibility—not property-level visibility—should be evaluated.
	 * <p>
	 * The returned callback provides:
	 * <ul>
	 *   <li>whether the object is considered visible,</li>
	 *   <li>any explanatory message when visibility is denied,</li>
	 *   <li>any exception generated during callback processing,</li>
	 *   <li>additional metadata used by the visibility evaluation logic.</li>
	 * </ul>
	 *
	 * @return an {@link OAObjectCallback} describing the object's visibility state
	 */
	public OAObjectCallback getIsVisibleObjectCallback() {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getAllowVisibleObjectCallback(null, this, null);
		return eq;
	}

	/**
	 * Determines whether the specified command (method) is permitted to execute
	 * according to OA's command-validation callback rules.
	 * <p>
	 * This method delegates to the OA object rules engine using {@link OAObjectCallback#CHECK_ALL} as the evaluation mode.
	 * The returned callback is then queried for its {@code allowed} state.
	 * <p>
	 * The delegate evaluates:
	 * <ul>
	 *   <li>model-defined command rules,</li>
	 *   <li>callback logic registered for the method,</li>
	 *   <li>contextual constraints such as object state or user permissions.</li>
	 * </ul>
	 * A return value of {@code true} indicates the command is allowed to proceed.
	 *
	 * @param methodName the name of the command/method being validated
	 * @return {@code true} if execution is allowed; {@code false} otherwise
	 */
	public boolean verifyCommand(String methodName) {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getVerifyCommandObjectCallback(this, methodName);
		return eq.getAllowed();
	}

	/**
	 * Retrieves the {@link OAObjectCallback} describing whether the specified
	 * command (method) is permitted to execute under the current OA callback
	 * rules and context.
	 * <p>
	 * This method delegates to the OA object rules engine using {@link OAObjectCallback#CHECK_ALL} as the evaluation mode.
	 * The returned callback includes:
	 * <ul>
	 *   <li>whether the command is allowed,</li>
	 *   <li>any explanatory message when execution is denied,</li>
	 *   <li>any exception or diagnostic detail from callback processing,</li>
	 *   <li>additional metadata related to command validation.</li>
	 * </ul>
	 * Callers typically use this method when they require full validation details
	 * rather than a simple boolean pass/fail result.
	 *
	 * @param methodName the name of the command/method being validated
	 * @return an {@link OAObjectCallback} describing the command authorization result
	 */
	public OAObjectCallback getVerifyCommand(String methodName) {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getVerifyCommandObjectCallback(this, methodName);
		return eq;
	}

	/**
	 * Retrieves the {@link OAObjectCallback} describing whether this object
	 * is allowed to be submitted according to OA’s submission rules and
	 * callback logic.
	 * <p>
	 * This method delegates to the OA object rules engine,
	 * which evaluates:
	 * <ul>
	 *   <li>model- and metadata-defined submission constraints,</li>
	 *   <li>callback-based validation rules,</li>
	 *   <li>object state conditions affecting submit eligibility.</li>
	 * </ul>
	 * The returned callback contains the full result of the submit check,
	 * including any message, diagnostic information, or exception provided by
	 * callback handlers.
	 *
	 * @return an {@link OAObjectCallback} describing whether submission is allowed
	 */
	public OAObjectCallback getAllowSubmit() {
		OA oa = OARuntime.oa(this);
		OAObjectCallback eq = oa.internal().objects().rules().getAllowSubmitObjectCallback(this);
		return eq;
	}

	/**
	 * Retrieves the {@link OAObjectCallback} describing whether this object
	 * is permitted to be saved according to OA’s save-validation rules.
	 * <p>
	 * This method delegates to the OA object rules engine using {@link OAObjectCallback#CHECK_ALL} as the evaluation mode.
	 * The delegate evaluates:
	 * <ul>
	 *   <li>model-level save rules and metadata constraints,</li>
	 *   <li>registered {@code beforeSave} or validation callbacks,</li>
	 *   <li>object state conditions that may prevent saving,</li>
	 *   <li>any custom application-defined save logic.</li>
	 * </ul>
	 * The returned callback contains both the allow/deny decision and any
	 * diagnostic message or exception associated with the validation.
	 *
	 * @return an {@link OAObjectCallback} describing the save-validation result
	 */
    public OAObjectCallback getVerifySaveObjectCallback() {
		OA oa = OARuntime.oa(this);
        OAObjectCallback eq = oa.internal().objects().rules().getVerifySaveObjectCallback(this);
        return eq;
    }
	
    /**
     * Indicates whether this object is marked as new and has not yet been saved
     * or committed by the persistence layer.
     * <p>
     * This flag is maintained by OA and reflects whether the object was
     * constructed in memory and has not gone through a save or load operation
     * that would clear the new-state indicator.
     *
     * @return {@code true} if the object is newly created and unsaved;
     *         {@code false} otherwise
     */
	public boolean getNew() {
		return newFlag;
	}

	/**
	 * Returns whether this object is currently marked as new.
	 * <p>
	 * This is a convenience alias for {@link #getNew()} and reflects whether the
	 * object has been created in memory but not yet saved or loaded from a
	 * persistence source. The value is maintained internally by OA and updated
	 * through {@link #setNew(boolean)}.
	 *
	 * @return {@code true} if the object is newly created and unsaved;
	 *         {@code false} otherwise
	 */
	public boolean isNew() {
		return newFlag;
	}

	/**
	 * Sets the new-state flag for this object.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectDelegate#setNew(OAObject, boolean)}, which performs all
	 * framework-level processing required when changing the new-state indicator.
	 * Typical delegate responsibilities include:
	 * <ul>
	 *   <li>marking the object as new or not new,</li>
	 *   <li>updating internal change-tracking structures,</li>
	 *   <li>notifying the OA model or caches when appropriate,</li>
	 *   <li>triggering any related callbacks.</li>
	 * </ul>
	 * This method is annotated with {@code @XmlTransient} to prevent the new-state
	 * flag from being serialized as a persistent property.
	 *
	 * @param b the new-state value to assign
	 */
	public void setNew(boolean b) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().state().setNew(this, b);
	}

	/**
	 * Indicates whether this object has been marked as deleted.
	 * <p>
	 * The deleted flag reflects whether the object has been logically removed
	 * through OA's deletion mechanisms but may still exist in memory until the
	 * persistence layer processes the change.
	 *
	 * @return {@code true} if the object is marked as deleted;
	 *         {@code false} otherwise
	 */
	public boolean getDeleted() {
		return deletedFlag;
	}

	/**
	 * Returns whether this object has been marked as deleted.
	 * <p>
	 * This is an alias for {@link #getDeleted()} and provides a more
	 * descriptive method name for callers evaluating historical deletion state.
	 *
	 * @return {@code true} if the object is marked as deleted;
	 *         {@code false} otherwise
	 */
	public boolean wasDeleted() {
		return deletedFlag;
	}

	/**
	 * Indicates whether this object is currently marked as deleted.
	 * <p>
	 * This is a convenience alias for {@link #getDeleted()} and reflects the
	 * object's logical deletion state as maintained by OA. An object marked as
	 * deleted may still reside in memory until the persistence layer or caching
	 * system processes the deletion.
	 *
	 * @return {@code true} if the object is marked as deleted;
	 *         {@code false} otherwise
	 */
	public boolean isDeleted() {
		return deletedFlag;
	}

	/**
	 * Marks this object as deleted or undeleted.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectDeleteDelegate#setDeleted(OAObject, boolean)}, which
	 * performs all framework-level handling associated with deletion, including:
	 * <ul>
	 *   <li>updating the object's deleted state,</li>
	 *   <li>managing cascade delete behavior when applicable,</li>
	 *   <li>interacting with the OA model and caching layers,</li>
	 *   <li>triggering any registered delete-related callbacks.</li>
	 * </ul>
	 * This method is annotated with {@code @XmlTransient} to ensure the deleted
	 * state is not serialized as a persistent property.
	 *
	 * @param tf {@code true} to mark the object as deleted; {@code false} to clear the deleted state
	 */
	public void setDeleted(boolean tf) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().delete().setDeleted(this, tf);
	}

	/**
	 * Compares this object to another for equality based solely on GUID identity.
	 * <p>
	 * OA enforces a single-instance-per-GUID rule across the OA model.
	 * Therefore, two {@code OAObject} instances are considered equal if:
	 * <ul>
	 *   <li>the other object is also an {@code OAObject}, and</li>
	 *   <li>its GUID matches this object's GUID.</li>
	 * </ul>
	 * This method does not consider property values or object state; GUID identity
	 * is the only determinant of equality.
	 *
	 * @param obj the object to compare with this instance
	 * @return {@code true} if the objects share the same GUID; {@code false} otherwise
	 */
	public final boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (!(obj instanceof OAObject)) return false;
		if (this.guid == null) return false;
		return this.guid.equals( ((OAObject) obj).getGuid() );
	}

	/**
	 * Returns a hash code derived solely from this object's GUID.
	 * <p>
	 * Because {@link #equals(Object)} is based strictly on GUID identity,
	 * the hash code must also be derived from the GUID to preserve the
	 * general contract of {@link Object#hashCode()}.
	 * <p>
	 * This method therefore returns {@code Long.hashCode(guid)}, ensuring
	 * consistent hashing behavior across all OAObject instances.
	 *
	 * @return the hash code for this object, derived from its GUID
	 */
	@Override
	public int hashCode() {
		if (this.guid == null) return 0;
		return this.guid.hashCode();
	}

	/**
	 * Compares this object with another for ordering based on GUID identity.
	 * <p>
	 * The comparison rules are:
	 * <ul>
	 *   <li>If {@code obj} is {@code null}, this object is considered greater
	 *       and {@code 1} is returned.</li>
	 *   <li>If {@code obj} is the same instance, {@code 0} is returned.</li>
	 *   <li>If {@code obj} is an {@code OAObject}, the comparison is performed
	 *       by numerically comparing the two GUID values using
	 *       {@link Long#compare(long, long)}.</li>
	 *   <li>If {@code obj} is not an {@code OAObject}, ordering falls back to
	 *       comparing the class names of the two objects.</li>
	 * </ul>
	 * This method provides a consistent, deterministic ordering for OAObjects
	 * within sorted collections or when used by utilities requiring a natural
	 * ordering.
	 *
	 * @param obj the object to compare with this instance
	 * @return a negative value, zero, or a positive value depending on whether
	 *         this object is less than, equal to, or greater than {@code obj}
	 */
	@Override
	public int compareTo(Object obj) {
		if (obj == null) return 1;
		if (obj == this) return 0;
        if (obj instanceof OAObject) {
    		UUID otherGuid = ((OAObject) obj).getGuid();
    		if (this.guid == null) {
    			if (otherGuid == null) return 0;
    			return -1;
    		}
    		else if (otherGuid == null) return 1;
    		return this.guid.compareTo(otherGuid);
		}
    	return this.getClass().getName().compareTo(obj.getClass().getName());
	}

	/**
	 * Determines whether this object is newly created or has any pending
	 * changes that have not yet been saved.
	 * <p>
	 * This is a convenience wrapper that delegates to
	 * {@link #getChanged(int)} using {@link #CASCADE_NONE}, meaning only the
	 * object's own changed state is considered and no link traversal is
	 * performed.
	 *
	 * @return {@code true} if the object is new or has unsaved changes;
	 *         {@code false} otherwise
	 */
	public boolean getChanged() {
		return getChanged(CASCADE_NONE);
	}

	/**
	 * Indicates whether this object is newly created or has unsaved changes.
	 * <p>
	 * This is a convenience alias for {@link #getChanged()} and evaluates only
	 * the object's own change state (no link traversal). The determination
	 * includes whether the object is marked as new or has property-level
	 * modifications recorded by the OA change-tracking system.
	 *
	 * @return {@code true} if the object is new or has unsaved changes;
	 *         {@code false} otherwise
	 */
	public boolean isChanged() {
		return getChanged(CASCADE_NONE);
	}

	/**
	 * Determines whether this object or—optionally—its linked objects have
	 * unsaved changes.
	 * <p>
	 * When {@code bIncludeLinks} is {@code true}, this method delegates to
	 * {@link #getChanged(int)} using {@link #CASCADE_LINK_RULES}, which
	 * instructs OA to examine linked objects whose metadata specifies
	 * cascade-change participation.
	 * When {@code bIncludeLinks} is {@code false}, only this object's own
	 * change state is evaluated.
	 *
	 * @param bIncludeLinks {@code true} to include CASCADE=true link objects
	 *                      in the evaluation; {@code false} to check only
	 *                      this object's state
	 * @return {@code true} if this object (or qualifying linked objects)
	 *         contain unsaved changes; {@code false} otherwise
	 */
	public boolean getChanged(boolean bIncludeLinks) {
		return getChanged(bIncludeLinks ? CASCADE_LINK_RULES : CASCADE_NONE);
	}

	/**
	 * Indicates whether this object—or optionally its linked objects—has
	 * unsaved changes.
	 * <p>
	 * This is a convenience alias for {@link #getChanged(boolean)} and uses
	 * the same cascade logic:
	 * <ul>
	 *   <li>If {@code bIncludeLinks} is {@code false}, only this object's
	 *       own change state is evaluated.</li>
	 *   <li>If {@code true}, OA will also evaluate linked objects whose
	 *       metadata defines CASCADE=true participation.</li>
	 * </ul>
	 *
	 * @param bIncludeLinks {@code true} to include CASCADE=true links in the
	 *                      evaluation; {@code false} to check this object only
	 * @return {@code true} if this object (or qualifying linked objects)
	 *         contain unsaved changes; {@code false} otherwise
	 */
	public boolean isChanged(boolean bIncludeLinks) {
		return getChanged(bIncludeLinks ? CASCADE_LINK_RULES : CASCADE_NONE);
	}

	/**
	 * Determines whether this object—or linked objects specified by the given
	 * relationship type—has unsaved changes.
	 * <p>
	 * This method delegates entirely to
	 * {@link OAObjectDelegate#getChanged(OAObject, int)}, which performs the
	 * actual change-detection logic. The delegate evaluates:
	 * <ul>
	 *   <li>whether this object is marked as new,</li>
	 *   <li>whether this object has local property changes,</li>
	 *   <li>whether linked objects should be included based on the supplied
	 *       {@code relationshipType} (e.g., {@code CASCADE_NONE},
	 *       {@code CASCADE_LINK_RULES}),</li>
	 *   <li>whether TYPE=MANY and CASCADE=true links should be traversed.</li>
	 * </ul>
	 *
	 * @param relationshipType the cascade/relationship mode used to determine
	 *                         whether linked objects participate in change
	 *                         evaluation
	 * @return {@code true} if this object or participating linked objects have
	 *         unsaved changes; {@code false} otherwise
	 */
	public boolean getChanged(int relationshipType) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().change().getChanged(this, relationshipType);
	}

	
	/**
	 * Sets the changed-state flag for this object and performs all associated
	 * OA change-notification and cascade behavior.
	 * <p>
	 * When the requested value differs from the current {@code changedFlag},
	 * this method:
	 * <ol>
	 *   <li>Fires a {@code beforePropertyChange} event for the special
	 *       {@code WORD_Changed} property via
	 *       {@link OAObjectEventDelegate#fireBeforePropertyChange(OAObject, String, Object, Object, boolean, boolean)}.</li>
	 *   <li>Updates the internal {@code changedFlag} to the new value.</li>
	 *   <li>Fires a {@code propertyChange} event for {@code WORD_Changed} so
	 *       listeners, hubs, and remote endpoints can react.</li>
	 *   <li>If the flag is set to {@code true}:
	 *     <ul>
	 *       <li>On non-remote threads, obtains {@link OAObjectInfo} and its
	 *           timestamp property (if any) and updates it with a new
	 *           {@link OADateTime} instance.</li>
	 *       <li>Marks the object as referenceable via
	 *           {@link OAObjectPropertyDelegate#setReferenceable(OAObject, boolean)}.</li>
	 *       <li>Notifies master/owner objects by:
	 *         <ul>
	 *           <li>retrieving hub references with
	 *               {@link OAObjectHubDelegate#getHubReferencesNoCopy(OAObject)},</li>
	 *           <li>for each live {@link Hub}, resolving its master object,</li>
	 *           <li>looking up link metadata via
	 *               {@link HubDetailDelegate#callHubDetailGetLinkInfoFromMasterHubToDetail(Hub)},</li>
	 *           <li>calling {@code setChanged(true)} on the master object when
	 *               the link has {@code cascadeSave} or {@code owner} set.</li>
	 *         </ul>
	 *       </li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * The method is annotated with {@link XmlTransient} so that the changed
	 * state is not treated as a persistent property during XML/JAXB
	 * serialization.
	 *
	 * @param tf {@code true} to mark this object (and, via cascade, certain
	 *           owners) as changed; {@code false} to clear the changed state
	 *           and remove original value tracking as defined by the delegates
	 */
	public void setChanged(boolean tf) {
		if (changedFlag != tf) {
			boolean bOld = changedFlag;
			final OA oa = OARuntime.oa(this);

			oa.internal().objects().event().fireBeforePropertyChange(	this, OAObjectParentService.WORD_Changed,
															bOld ? Boolean.TRUE : Boolean.FALSE,
															tf ? Boolean.TRUE : Boolean.FALSE,
															(tf == false), // local only  20150530 was: "false", now only sending if changed=false
															false);
			changedFlag = tf;
			oa.internal().objects().event().firePropertyChange(	this, OAObjectParentService.WORD_Changed,
														bOld ? Boolean.TRUE : Boolean.FALSE,
														changedFlag ? Boolean.TRUE : Boolean.FALSE, false, false);

			if (changedFlag) {
				// 20190307
				if (!isRemoteThread()) {
					OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
					OAPropertyInfo pi = oi.getTimestampProperty();
					if (pi != null) {
						this.setProperty(pi.getName(), new OADateTime());
					}
				}

				oa.internal().objects().property().setReferenceable(this, true);

				// 20180520 notify owner
				WeakReference<Hub<?>>[] refs = oa.internal().objects().hub().getHubReferencesNoCopy(this);
				if (refs != null) {
					for (WeakReference wr : refs) {
						if (wr == null) {
							continue;
						}
						Hub hx = (Hub) wr.get();
						if (hx == null) {
							continue;
						}

						OAObject obj = hx.getMasterObject();
						if (obj != null) {
							OALinkInfo li = oa.internal().hubs().detail().getLinkInfoFromMasterToDetail(hx);
							if (li != null && (li.getCascadeSave() || li.getOwner())) {
								obj.setChanged(true);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Creates a new {@code OAObject} that is a structural copy of this object.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#createCopy(OAObject, String[])}, passing
	 * {@code null} for the exclusion list. The delegate performs the full
	 * reflection-based copy operation, including:
	 * <ul>
	 *   <li>copying all simple property values,</li>
	 *   <li>reusing all {@code One} link references from the source object,</li>
	 *   <li>for {@code Many} links, creating cloned child objects only for
	 *       owned links,</li>
	 *   <li>initializing the new object according to OA metadata rules.</li>
	 * </ul>
	 * Copying occurs on the server side and follows OA’s ownership and link
	 * semantics to ensure the new object is correctly structured.
	 *
	 * @return a newly created {@code OAObject} that is a copy of this object
	 */
	public OAObject createCopy() {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().createCopy(this, null);
	}

	/**
	 * Creates a copy of this object while excluding the specified property names.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#createCopy(OAObject, String[])}, which
	 * performs a reflection-based copy of the source object. The delegate:
	 * <ul>
	 *   <li>copies all simple properties except those listed in
	 *       {@code excludePropertyNames},</li>
	 *   <li>for {@code One} links, reuses the same referenced target objects,</li>
	 *   <li>for {@code Many} links, copies only owned link contents and creates
	 *       cloned child objects,</li>
	 *   <li>initializes the new object based on OA metadata and link rules.</li>
	 * </ul>
	 * This version allows selective omission of fields or links from the copy
	 * process, enabling customized duplication scenarios without modifying the
	 * source object.
	 *
	 * @param excludePropertyNames the property names to omit from the copy,
	 *                             or {@code null} to include all properties
	 * @return a newly created copy of this object, with the specified properties
	 *         excluded from the copy operation
	 */
	public Object createCopy(String[] excludePropertyNames) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().createCopy(this, excludePropertyNames);
	}

	/**
	 * Copies the properties and eligible links from this object into the
	 * specified target object.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#copyInto(OAObject, OAObject, String[], OACopyCallback)}
	 * using {@code null} for both the exclusion list and the callback.
	 * <br><br>
	 * The delegate performs the full reflection-based copy operation,
	 * including:
	 * <ul>
	 *   <li>copying all simple property values,</li>
	 *   <li>copying all {@code One} links by assigning the same referenced
	 *       object,</li>
	 *   <li>for {@code Many} links, copying only owned link contents and
	 *       cloning child objects into the target Hub,</li>
	 *   <li>honoring OA metadata rules regarding ownership, cascade,
	 *       and link direction.</li>
	 * </ul>
	 * This method does not create a new {@code OAObject}; instead, it populates
	 * the supplied {@code toObject} with values and link structures derived
	 * from the source object.
	 *
	 * @param toObject the object to receive copied property and link values;
	 *                 must not be {@code null}
	 */
	public void copyInto(OAObject toObject) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().copyInto(this, toObject, (String[]) null, null);
	}

	/**
	 * Enables or disables automatic saving of finalized {@code OAObject}
	 * instances.
	 * <p>
	 * This method sets the framework-wide flag
	 * {@code OAObjectDelegate.bFinalizeSave}, which controls whether objects
	 * that become finalized (i.e., eligible for garbage collection) should
	 * automatically trigger a save operation to their datasource.
	 * <p>
	 * The default value is {@code false}. When enabled, OA may attempt to
	 * persist objects during finalization, which can be useful in certain
	 * offline-buffering scenarios but should be used cautiously because finalizer
	 * timing is nondeterministic.
	 *
	 * @param b {@code true} to enable finalize-time save behavior;
	 *          {@code false} to disable it
	 */
	public static void setFinalizeSave(boolean b) {
		// OAObjectService.bFinalizeSave = b;
	}

	/**
	 * Returns the framework-wide setting that controls whether finalized
	 * {@code OAObject} instances are automatically saved to their datasource.
	 * <p>
	 * This value reflects the static flag
	 * {@code OAObjectDelegate.bFinalizeSave}, which can be modified via
	 * {@link #setFinalizeSave(boolean)}.
	 *
	 * @return {@code true} if finalize-time saving is enabled;
	 *         {@code false} otherwise
	 */
	public static boolean getFinalizeSave() {
		// return OAObjectDelegate.bFinalizeSave;
		return false;
	}

	
	/**
	 * Indicates whether the current thread is in the process of loading an
	 * {@code OAObject} from a datasource or serialized state.
	 * <p>
	 * This method delegates to
	 * {@link OAThreadLocalDelegate#callThreadLocalIsLoading()}, which maintains a thread-local
	 * flag used by the OA framework to suppress certain callbacks,
	 * change-notifications, and timestamp updates during load operations.
	 *
	 * @return {@code true} if the current thread is within a load operation;
	 *         {@code false} otherwise
	 */
	public boolean isLoading() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		return srvcOAThreadLocal.isLoading();
	}

	/**
	 * Fires a {@code beforePropertyChange} event for the specified property,
	 * notifying all listeners associated with Hubs that reference this object.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectEventDelegate#fireBeforePropertyChange(OAObject, String, Object, Object, boolean, boolean)}
	 * using the supplied values and a fixed {@code true} for the
	 * {@code bSend} parameter. The delegate is responsible for:
	 * <ul>
	 *   <li>executing any registered before-change callbacks,</li>
	 *   <li>validating the proposed change,</li>
	 *   <li>notifying Hub listeners,</li>
	 *   <li>preparing the event pipeline before the actual property update.</li>
	 * </ul>
	 *
	 * @param propertyName the name of the property being changed (case-insensitive)
	 * @param oldObj       the current value of the property
	 * @param newObj       the proposed new value
	 * @param bLocalOnly   {@code true} to restrict notification to local listeners;
	 *                     {@code false} to include remote or global listeners
	 */
	protected void fireBeforePropertyChange(String propertyName, Object oldObj, Object newObj, boolean bLocalOnly) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().event().fireBeforePropertyChange(this, propertyName, oldObj, newObj, bLocalOnly, true);
	}

	/**
	 * Fires a {@code beforePropertyChange} event for the specified property
	 * using the property's metadata to determine whether the event should be
	 * restricted to local listeners.
	 * <p>
	 * This method:
	 * <ol>
	 *   <li>Retrieves {@link OAObjectInfo} for this object's class via
	 *       {@link OAObjectInfoDelegate#callInfoGetObjectInfo(Class)}.</li>
	 *   <li>Obtains the {@code localOnly} flag from the metadata.</li>
	 *   <li>Delegates event dispatching to
	 *       {@link OAObjectEventDelegate#fireBeforePropertyChange(OAObject, String, Object, Object, boolean, boolean)}
	 *       with {@code bSend=true}.</li>
	 * </ol>
	 * The resulting event notifies Hub listeners and executes registered
	 * callbacks prior to the property being modified.
	 *
	 * @param propertyName the property being changed (case-insensitive)
	 * @param oldObj       the current value of the property
	 * @param newObj       the proposed new value
	 */
	protected void fireBeforePropertyChange(String propertyName, Object oldObj, Object newObj) {
		OA oa = OARuntime.oa(this);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
		oa.internal().objects().event().fireBeforePropertyChange(this, propertyName, oldObj, newObj, oi.getLocalOnly(), true);
	}

	/**
	 * Convenience overload for firing a {@code beforePropertyChange} event
	 * for boolean properties.
	 * <p>
	 * This method converts the primitive boolean values into the framework’s
	 * canonical constants ({@code OAObjectDelegate.TRUE} and
	 * {@code OAObjectDelegate.FALSE}) and delegates to
	 * {@link #fireBeforePropertyChange(String, Object, Object)}.
	 * <p>
	 * No property update occurs here; this only triggers the pre-change event
	 * sequence so listeners and callbacks can react before the new value is
	 * applied.
	 *
	 * @param property the name of the boolean property being changed
	 * @param oldObj   the current boolean value
	 * @param newObj   the proposed boolean value
	 */
	protected void fireBeforePropertyChange(String property, boolean oldObj, boolean newObj) {
		fireBeforePropertyChange(	property, oldObj ? Boolean.TRUE : Boolean.FALSE,
									newObj ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Fires a {@code beforePropertyChange} event for integer-valued properties.
	 * <p>
	 * This method is invoked internally by the OA framework when a primitive
	 * {@code int} property is about to change. It delegates the full event
	 * workflow to
	 * {@link OAObjectEventDelegate#fireBeforePropertyChange(OAObject, String, Object, Object, boolean, boolean)},
	 * wrapping the primitive values in their boxed {@link Integer} form.
	 * <p>
	 * The delegate is responsible for:
	 * <ul>
	 *   <li>invoking {@code beforePropertyChange} callbacks,</li>
	 *   <li>validating the change according to metadata rules,</li>
	 *   <li>notifying listeners such as Hubs and UI bindings,</li>
	 *   <li>determining whether the change should proceed or be cancelled.</li>
	 * </ul>
	 *
	 * @param property the name of the property being changed
	 * @param oldObj   the current (primitive int) value of the property
	 * @param newObj   the proposed new (primitive int) value of the property
	 */
	protected void fireBeforePropertyChange(String property, int oldObj, int newObj) {
		fireBeforePropertyChange(property, Integer.valueOf(oldObj), Integer.valueOf(newObj));
	}

	/**
	 * Fires a {@code beforePropertyChange} event for a primitive {@code long}
	 * property about to be updated.
	 * <p>
	 * This overload boxes the primitive values into {@link Long} instances and
	 * delegates to
	 * {@link #fireBeforePropertyChange(String, Object, Object, boolean)},
	 * which performs the full OA before-change event workflow through
	 * {@link OAObjectEventDelegate#fireBeforePropertyChange}.
	 * <p>
	 * The delegate handles:
	 * <ul>
	 *   <li>triggering any {@code beforePropertyChange} callbacks,</li>
	 *   <li>validating the change using metadata rules,</li>
	 *   <li>notifying Hub listeners and other observers,</li>
	 *   <li>determining whether the proposed update is permitted.</li>
	 * </ul>
	 *
	 * @param property the name of the property being updated
	 * @param oldObj   the current primitive {@code long} value
	 * @param newObj   the proposed new primitive {@code long} value
	 */
	protected void fireBeforePropertyChange(String property, long oldObj, long newObj) {
		fireBeforePropertyChange(property, Long.valueOf(oldObj), Long.valueOf(newObj));
	}

	/**
	 * Fires a {@code beforePropertyChange} event for a primitive {@code double}
	 * property before its value is updated.
	 * <p>
	 * The primitive values are boxed into {@link Double} instances and delegated to
	 * {@link #fireBeforePropertyChange(String, Object, Object)}, which performs the
	 * full OA pre-change processing through
	 * {@link OAObjectEventDelegate#fireBeforePropertyChange}.
	 * <p>
	 * The delegate is responsible for:
	 * <ul>
	 *   <li>invoking any registered {@code beforePropertyChange} callbacks,</li>
	 *   <li>applying metadata-based validation rules,</li>
	 *   <li>notifying Hub listeners and other observers,</li>
	 *   <li>determining whether the change should be allowed to proceed.</li>
	 * </ul>
	 *
	 * @param property the name of the property being updated
	 * @param oldObj   the current primitive {@code double} value
	 * @param newObj   the proposed new primitive {@code double} value
	 */
	protected void fireBeforePropertyChange(String property, double oldObj, double newObj) {
		fireBeforePropertyChange(property, Double.valueOf(oldObj), Double.valueOf(newObj));
	}

	/**
	 * Fires a {@code propertyChange} event for the specified property.
	 * <p>
	 * This method delegates directly to
	 * {@link OAObjectEventDelegate#firePropertyChange(OAObject, String, Object, Object, boolean, boolean)},
	 * forwarding both the old and new values along with the {@code bLocalOnly}
	 * flag. The delegate performs the full OA property-change workflow, including:
	 * <ul>
	 *   <li>notifying Hub listeners and other observers,</li>
	 *   <li>triggering any registered {@code propertyChange} callbacks,</li>
	 *   <li>applying metadata-driven cascade or link behavior,</li>
	 *   <li>propagating events to remote clients when applicable,</li>
	 *   <li>suppressing server-side propagation when {@code bLocalOnly} is true.</li>
	 * </ul>
	 *
	 * @param propertyName the name of the property that changed
	 * @param oldObj       the previous value of the property; may be {@code null}
	 * @param newObj       the new value of the property; may be {@code null}
	 * @param bLocalOnly   if {@code true}, the event is restricted to local
	 *                     listeners and is not sent to remote subscribers
	 */
	protected void firePropertyChange(String propertyName, Object oldObj, Object newObj, boolean bLocalOnly) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().event().firePropertyChange(this, propertyName, oldObj, newObj, bLocalOnly, true);
	}

	/**
	 * Fires a {@code propertyChange} event for the specified property using
	 * default propagation behavior.
	 * <p>
	 * This overload delegates directly to
	 * {@link OAObjectEventDelegate#firePropertyChange(OAObject, String, Object, Object, boolean, boolean)}
	 * with {@code bLocalOnly} fixed to {@code false}. As a result, the event may
	 * propagate to:
	 * <ul>
	 *   <li>local listeners and Hub observers,</li>
	 *   <li>linked Hubs following master–detail or cascade rules,</li>
	 *   <li>remote subscribers when distributed synchronization is enabled.</li>
	 * </ul>
	 * The delegate performs all validation, listener notification, remote
	 * dispatching, and cascade evaluation.
	 *
	 * @param propertyName the name of the property that changed
	 * @param oldObj       the previous value; may be {@code null}
	 * @param newObj       the new value; may be {@code null}
	 */
	protected void firePropertyChange(String propertyName, Object oldObj, Object newObj) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().event().firePropertyChange(this, propertyName, oldObj, newObj, false, true);
	}

	/**
	 * Fires a {@code propertyChange} event for the specified property without
	 * supplying old or new values.
	 * <p>
	 * This is a convenience overload used when callers want to notify listeners
	 * that a property has changed but do not have (or do not wish to supply)
	 * the previous and new values. Both values are passed to the delegate as
	 * {@code null}.
	 * <p>
	 * The call is delegated to
	 * {@link OAObjectEventDelegate#firePropertyChange(OAObject, String, Object, Object, boolean, boolean, boolean)}
	 * with:
	 * <ul>
	 *   <li>{@code oldObj = null}</li>
	 *   <li>{@code newObj = null}</li>
	 *   <li>{@code bLocalOnly = false}</li>
	 *   <li>server and client propagation enabled</li>
	 * </ul>
	 * The delegate performs all listener notification, cascade handling,
	 * metadata-driven propagation, and remote sync behavior.
	 *
	 * @param propertyName the name of the property whose change should be signaled
	 */
	protected void firePropertyChange(String propertyName) {
		final OA oa = OARuntime.oa(this);
		oa.internal().objects().event().firePropertyChange(this, propertyName, null, null, false, true, true);
	}

	/**
	 * Fires a {@code newList} event for the Hub referenced by the specified
	 * property.
	 * <p>
	 * This method retrieves the Hub associated with the given link property via
	 * {@link #getHub(String)}. If the Hub exists, it triggers a
	 * {@code newList} event using
	 * {@link HubEventDelegate#fireOnNewListEvent(Hub, boolean)}.
	 * <p>
	 * A {@code newList} event indicates that the Hub's underlying list has been
	 * replaced, refreshed, or otherwise reinitialized. Typical consequences
	 * include:
	 * <ul>
	 *   <li>clearing selection state,</li>
	 *   <li>resetting iterators and UI bindings,</li>
	 *   <li>notifying listeners that the Hub's contents should be re-evaluated.</li>
	 * </ul>
	 *
	 * @param hubPropertyName the name of the Hub reference property whose list
	 *                        has been refreshed
	 */
	protected void fireNewList(String hubPropertyName) {
		Hub h = getHub(hubPropertyName);
		if (h == null) {
			return;
		}
		final OA oa = OARuntime.oa(h);
		oa.internal().hubs().events().fireOnNewListEvent(h, true);
	}

	/**
	 * Fires a {@code propertyChange} event for a primitive {@code boolean}
	 * property.
	 * <p>
	 * This overload converts the primitive boolean values into the shared
	 * {@link OAObjectDelegate#TRUE} and {@link OAObjectDelegate#FALSE}
	 * singleton objects and delegates to
	 * {@link #firePropertyChange(String, Object, Object)} for full event
	 * processing through
	 * {@link OAObjectEventDelegate#firePropertyChange}.
	 * <p>
	 * Using the shared TRUE/FALSE instances avoids repeated boxing and ensures
	 * consistent identity semantics for boolean values across the OA framework.
	 *
	 * @param property the name of the property that changed
	 * @param oldObj   the previous primitive {@code boolean} value
	 * @param newObj   the new primitive {@code boolean} value
	 */
	protected void firePropertyChange(String property, boolean oldObj, boolean newObj) {
		firePropertyChange(	property, oldObj ? Boolean.TRUE : Boolean.FALSE,
							newObj ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Fires a {@code propertyChange} event for a primitive {@code int}
	 * property.
	 * <p>
	 * This overload boxes the primitive values into {@link Integer} instances
	 * and delegates to
	 * {@link #firePropertyChange(String, Object, Object)}, which performs the
	 * complete OA property-change workflow through
	 * {@link OAObjectEventDelegate#firePropertyChange}.
	 * <p>
	 * Because this overload uses the default broadcast variant, the event may
	 * propagate to:
	 * <ul>
	 *   <li>Hub listeners and model observers,</li>
	 *   <li>linked Hubs via cascade rules,</li>
	 *   <li>remote subscribers when distributed synchronization is enabled.</li>
	 * </ul>
	 *
	 * @param property the name of the property that changed
	 * @param oldObj   the previous primitive {@code int} value
	 * @param newObj   the new primitive {@code int} value
	 */
	protected void firePropertyChange(String property, int oldObj, int newObj) {
		firePropertyChange(property, Integer.valueOf(oldObj), Integer.valueOf(newObj));
	}

	/**
	 * Fires a {@code propertyChange} event for a primitive {@code long}
	 * property.
	 * <p>
	 * The primitive values are boxed into {@link Long} instances and forwarded to
	 * {@link #firePropertyChange(String, Object, Object)}, which applies the full
	 * OA property-change workflow through
	 * {@link OAObjectEventDelegate#firePropertyChange}.
	 * <p>
	 * As with other primitive overloads, this method always uses the default
	 * broadcast behavior ({@code bLocalOnly = false}), allowing the event to
	 * propagate to Hub listeners, linked objects, and remote subscribers when
	 * distributed synchronization is active.
	 *
	 * @param property the name of the property that changed
	 * @param oldObj   the previous primitive {@code long} value
	 * @param newObj   the new primitive {@code long} value
	 */
	protected void firePropertyChange(String property, long oldObj, long newObj) {
		firePropertyChange(property, Long.valueOf(oldObj), Long.valueOf(newObj));
	}

	/**
	 * Fires a {@code propertyChange} event for a primitive {@code double}
	 * property.
	 * <p>
	 * The primitive values are boxed into {@link Double} objects and delegated to
	 * {@link #firePropertyChange(String, Object, Object)}, which performs the
	 * complete OA property-change workflow through
	 * {@link OAObjectEventDelegate#firePropertyChange}.
	 * <p>
	 * Because this overload uses the default broadcast variant
	 * ({@code bLocalOnly = false}), the resulting event may propagate to:
	 * <ul>
	 *   <li>local listeners and Hub observers,</li>
	 *   <li>linked Hubs following cascade rules,</li>
	 *   <li>remote subscribers when distributed synchronization is enabled.</li>
	 * </ul>
	 *
	 * @param property the name of the property that changed
	 * @param oldObj   the previous primitive {@code double} value
	 * @param newObj   the new primitive {@code double} value
	 */
	protected void firePropertyChange(String property, double oldObj, double newObj) {
		firePropertyChange(property, Double.valueOf(oldObj), Double.valueOf(newObj));
	}

	/**
	 * Fires a {@code propertyChange} event for the specified property but restricts
	 * propagation to the local VM.
	 * <p>
	 * This method delegates to
	 * {@link #firePropertyChange(String, Object, Object, boolean)} with
	 * {@code bLocalOnly} forced to {@code true}. As a result, the event:
	 * <ul>
	 *   <li>notifies local listeners and Hub observers,</li>
	 *   <li>applies all local cascade and metadata rules,</li>
	 *   <li>does <b>not</b> propagate to remote subscribers, servers, or distributed sync layers.</li>
	 * </ul>
	 * It is typically used when internal updates should be visible within the same
	 * JVM but must not trigger remote synchronization.
	 *
	 * @param property the name of the property being updated
	 * @param oldObj   the previous value; may be {@code null}
	 * @param newObj   the new value; may be {@code null}
	 */
	protected void fireLocalPropertyChange(String property, Object oldObj, Object newObj) {
		firePropertyChange(property, oldObj, newObj, true);
	}

	/**
	 * Fires a local-only {@code propertyChange} event for the specified property,
	 * without providing old or new values.
	 * <p>
	 * This method delegates directly to
	 * {@link OAObjectEventDelegate#firePropertyChange(OAObject, String, Object, Object, boolean, boolean, boolean)}
	 * with both value parameters set to {@code null} and {@code bLocalOnly}
	 * forced to {@code true}. As a result, the event:
	 * <ul>
	 *   <li>notifies local Hub listeners and observers,</li>
	 *   <li>applies local cascade and metadata rules,</li>
	 *   <li>does <b>not</b> propagate to servers or remote sync subscribers,</li>
	 *   <li>may cause UI components bound to the property to refresh.</li>
	 * </ul>
	 * This overload is typically used when callers only need to signal that a
	 * property should refresh locally, without communicating what changed.
	 *
	 * @param property the name of the property whose change should be signaled
	 */
	protected void fireLocalPropertyChange(String property) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().event().firePropertyChange(this, property, null, null, true, true, true);
	}

	/**
	 * Fires a local-only {@code propertyChange} event for a primitive
	 * {@code int} property.
	 * <p>
	 * This overload boxes the primitive values into {@link Integer} objects and
	 * delegates to
	 * {@link #firePropertyChange(String, Object, Object, boolean)} with
	 * {@code bLocalOnly} forced to {@code true}. As a result, the event:
	 * <ul>
	 *   <li>notifies local listeners and Hub observers,</li>
	 *   <li>applies all local cascade and metadata rules,</li>
	 *   <li>does <b>not</b> propagate to servers or remote-sync subscribers.</li>
	 * </ul>
	 * This method is typically used when internal integer-based updates should
	 * remain confined to the current JVM.
	 *
	 * @param property the name of the property being updated
	 * @param oldObj   the previous primitive {@code int} value
	 * @param newObj   the new primitive {@code int} value
	 */
	protected void fireLocalPropertyChange(String property, int oldObj, int newObj) {
		firePropertyChange(property, Integer.valueOf(oldObj), Integer.valueOf(newObj), true);
	}

	/**
	 * Retrieves the {@link Hub} associated with the specified link property name.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject, String, String, boolean, Hub)}
	 * to resolve the Hub reference using OA's metadata and link rules.
	 * <p>
	 * Characteristics:
	 * <ul>
	 *   <li>Automatically resolves the Hub for a master/detail or one-to-many link property.</li>
	 *   <li>Does not create a new Hub if none exists; returns {@code null} when unresolved.</li>
	 *   <li>Uses no sort order and does not enable auto-sequence.</li>
	 *   <li>Does not filter by a match-Hub (i.e., {@code hubMatch = null}).</li>
	 * </ul>
	 *
	 * @param linkPropertyName the name of the link property whose Hub is requested
	 * @return the resolved {@link Hub}, or {@code null} if the property does not
	 *         represent a Hub reference or the Hub has not been initialized
	 */
	protected Hub<?> getHub(String linkPropertyName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, null, false, null);
	}

	protected <T extends OAObject> Hub<T> getHub(Class<T> type, String linkPropertyName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, null, false, null);
	}
	
	
	/**
	 * Assigns a {@link Hub} to the specified link property.
	 * <p>
	 * This method retrieves the {@link OALinkInfo} metadata for the link property
	 * and determines whether the Hub should be stored as a {@link WeakReference}
	 * based on {@link OAObjectInfoDelegate#cacheHub(OALinkInfo, Hub)} rules.
	 * <p>
	 * If the link metadata indicates that the Hub is cacheable, the property is set
	 * to a {@code WeakReference} pointing to the Hub. Otherwise, the Hub itself is
	 * stored directly. In both cases, the assignment is performed through
	 * {@link OAObjectPropertyDelegate#setProperty(OAObject, String, Object)} so that
	 * all standard OA property-change, reverse-link, and event semantics apply.
	 *
	 * @param linkPropertyName the name of the link property whose Hub is being set
	 * @param hub the Hub instance to associate with the property (may be {@code null})
	 */
	public void setHub(String linkPropertyName, Hub hub) {
		OA oa = OARuntime.oa(this);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this);
		OALinkInfo linkInfo = oa.internal().objects().info().getLinkInfo(oi, linkPropertyName);

		if (oa.internal().objects().info().cacheHub(linkInfo, hub)) {
			oa.internal().objects().property().setProperty(this, linkPropertyName, new WeakReference(hub));
		} else {
			oa.internal().objects().property().setProperty(this, linkPropertyName, hub);
		}
	}

	/**
	 * Retrieves the {@link Hub} associated with the specified link property name,
	 * applying the given sort order.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject, String, String, boolean, Hub)}
	 * to resolve the Hub using OA metadata. The returned Hub:
	 * <ul>
	 *   <li>corresponds to a link (one-to-many) property on this object,</li>
	 *   <li>is sorted according to {@code sortOrder} if supplied,</li>
	 *   <li>is not auto-sequenced ({@code bSequence = false}),</li>
	 *   <li>is not filtered by a match Hub ({@code hubMatch = null}).</li>
	 * </ul>
	 * If the Hub does not yet exist or the property is not a Hub-type link, this
	 * method may return {@code null}.
	 *
	 * @param linkPropertyName the name of the link property whose Hub is being retrieved
	 * @param sortOrder        optional sort order expression (may be {@code null})
	 * @return the resolved {@link Hub}, or {@code null} if unavailable
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, sortOrder, false, null);
	}

	/**
	 * Retrieves the {@link Hub} associated with the specified link property,
	 * optionally applying a sort order and enabling auto-sequencing.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject, String, String, boolean, Hub)},
	 * which performs all metadata-driven resolution and Hub initialization.
	 * <p>
	 * Behavior:
	 * <ul>
	 *   <li>If {@code sortOrder} is provided, it is applied to the returned Hub.</li>
	 *   <li>If {@code bSequence} is {@code true}, the Hub's {@code setAutoSequence}
	 *       mechanism will be enabled, allowing automatic sequence number assignment
	 *       for items within the Hub.</li>
	 *   <li>No match-Hub filtering is applied ({@code hubMatch = null}).</li>
	 * </ul>
	 *
	 * @param linkPropertyName the name of the link property whose Hub is requested
	 * @param sortOrder        optional sort order expression; may be {@code null}
	 * @param bSequence        if {@code true}, enables auto-sequencing on the Hub
	 * @return the resolved Hub, or {@code null} if no such Hub exists or is not initialized
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder, boolean bSequence) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, sortOrder, bSequence, null);
	}

	/**
	 * Retrieves the {@link Hub} associated with the specified link property name,
	 * using the provided sort order and optional auto-sequence behavior.
	 * <p>
	 * Although this overload includes a {@code hubMatch} parameter, it is not used
	 * in the current implementation; the method delegates to
	 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject, String, String, boolean, Hub)}
	 * with {@code hubMatch} forced to {@code null}.
	 * <p>
	 * Behavior:
	 * <ul>
	 *   <li>Resolves the Hub for the link property according to metadata,</li>
	 *   <li>Applies the supplied {@code sortOrder} if not {@code null},</li>
	 *   <li>Enables auto-sequencing if {@code bSequence} is true,</li>
	 *   <li>Does <em>not</em> use the provided {@code hubMatch} parameter.</li>
	 * </ul>
	 *
	 * @param linkPropertyName the name of the link property to resolve
	 * @param sortOrder        optional sort order expression
	 * @param bSequence        whether to enable auto-sequencing behavior
	 * @param hubMatch         an optional Hub used by the delegate to filter or
	 *                         restrict the returned Hub
	 * @return the resolved Hub, or {@code null} if no such Hub exists or is not initialized
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder, boolean bSequence, Hub hubMatch) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, sortOrder, bSequence, hubMatch);
	}

	/**
	 * Retrieves the {@link Hub} associated with the specified link property,
	 * applying a sort order and optionally restricting results using a match Hub.
	 * <p>
	 * This overload delegates to
	 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject, String, String, boolean, Hub)}
	 * with auto-sequencing disabled ({@code bSequence = false}). The delegate:
	 * <ul>
	 *   <li>resolves the Hub for the given link property,</li>
	 *   <li>applies the supplied {@code sortOrder} if present,</li>
	 *   <li>uses {@code hubMatch} to filter or constrain the returned Hub when
	 *       supported by the metadata and delegate logic.</li>
	 * </ul>
	 * If the property is not a Hub-type link or cannot be resolved, this method may
	 * return {@code null}.
	 *
	 * @param linkPropertyName the name of the link property to resolve
	 * @param sortOrder        optional sort-order expression; may be {@code null}
	 * @param hubMatch         an optional Hub used by the delegate to filter or
	 *                         restrict the returned Hub
	 * @return the resolved and optionally matched {@link Hub}, or {@code null} if unavailable
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder, Hub hubMatch) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, sortOrder, false, hubMatch);
	}

	/**
	 * Retrieves the {@link Hub} associated with the specified link property,
	 * optionally applying a match-Hub filter.
	 * <p>
	 * This overload delegates to
	 * {@link OAObjectReflectDelegate#getReferenceHub(OAObject, String, String, boolean, Hub)}
	 * using:
	 * <ul>
	 *   <li>{@code sortOrder = null}</li>
	 *   <li>{@code bSequence = false}</li>
	 *   <li>{@code hubMatch = hubMatch}</li>
	 * </ul>
	 * The delegate resolves the Hub according to metadata and may use
	 * {@code hubMatch} to restrict or filter the returned Hub when supported.
	 * If the property is not a Hub-type link or has not been initialized, the
	 * method may return {@code null}.
	 *
	 * @param linkPropertyName the name of the link property to resolve
	 * @param hubMatch         an optional Hub used to filter items in the returned Hub
	 * @return the resolved Hub, or {@code null} if unavailable
	 */
	protected Hub getHub(String linkPropertyName, Hub hubMatch) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceHub(this, linkPropertyName, null, false, hubMatch);
	}

	/**
	 * Retrieves the reference (single-object) link associated with the specified
	 * property name.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getReferenceObject(OAObject, String)},
	 * which resolves the reference according to OA metadata. If the reference has
	 * not yet been loaded, the delegate may invoke the configured {@code OADataSource}
	 * to retrieve the target object.
	 * <p>
	 * This method does not perform any validation on the property name. If the
	 * property is not a reference-type link, the delegate determines the result,
	 * which may be {@code null}.
	 *
	 * @param linkPropertyName the name of the reference property to retrieve
	 * @return the referenced object, or {@code null} if it is not set, not loaded,
	 *         or cannot be resolved
	 */
	protected Object getObject(String linkPropertyName) {
		OA oa = OARuntime.oa(this);
		Object obj = oa.internal().objects().reflect().getReferenceObject(this, linkPropertyName);
		return obj;
	}

	/**
	 * Determines whether the specified reference (single-object) link property
	 * is currently {@code null} or empty.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#isReferenceObjectNullOrEmpty(OAObject, String)},
	 * which evaluates the reference according to OA metadata. The delegate handles:
	 * <ul>
	 *   <li>checking whether the reference is loaded,</li>
	 *   <li>detecting {@code null} values,</li>
	 *   <li>detecting empty placeholder values,</li>
	 *   <li>handling weak references or unloaded reference states.</li>
	 * </ul>
	 *
	 * @param name the name of the reference property to evaluate
	 * @return {@code true} if the reference is {@code null}, empty, or not loaded;
	 *         {@code false} otherwise
	 */
	public boolean isReferenceObjectNull(String name) {
		OA oa = OARuntime.oa(this);
		boolean b = oa.internal().objects().reflect().isReferenceObjectNullOrEmpty(this, name);
		return b;
	}

	/**
	 * Retrieves a blob (byte array) associated with the specified property name.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#getReferenceBlob(OAObject, String)}, which
	 * resolves the property using OA metadata. If the blob value has not been
	 * loaded, the delegate may invoke the configured {@code OADataSource} to
	 * retrieve the data, potentially using prefetching or batching hints.
	 * <p>
	 * No validation is performed on the property name; if the property is not a
	 * blob-type attribute or cannot be resolved, the delegate determines whether
	 * {@code null} or an empty array is returned.
	 *
	 * @param linkPropertyName the name of the blob property to retrieve
	 * @return the blob {@code byte[]} value, or {@code null} if not present or not resolved
	 */
	protected byte[] getBlob(String linkPropertyName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().reflect().getReferenceBlob(this, linkPropertyName);
	}

	/**
	 * Saves this object using standard OA cascade rules.
	 * <p>
	 * It delegates to {@link #save(int)} using {@code CASCADE_LINK_RULES}, which applies all
	 * metadata-defined cascade behaviors for link properties.
	 * <p>
	 * After the save completes (whether normally or due to an exception), the
	 * previous admin flag is restored.
	 */
	public void save() {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		boolean b3 = srvcOAThreadLocal.setAdmin(true);
		try {
			this.save(CASCADE_LINK_RULES);
		} finally {
			srvcOAThreadLocal.setAdmin(b3);
		}
	}

	/**
	 * Saves this object using the specified cascade rule.
	 * <p>
	 * The method first verifies that the object is allowed to be saved by calling
	 * {@link #canSave()}. If saving is not permitted, an
	 * {@link IllegalArgumentException} is thrown.
	 * <p>
	 * If the object has not been marked as changed and the cascade rule is
	 * non-negative, the method records that a flag update may be required after the
	 * save completes. The save is then performed through
	 * {@link OAObjectPropertyDelegate#save(OAObject, int)}, which executes the
	 * cascade-aware save behavior defined by OA metadata.
	 * <p>
	 * Administrative mode is enabled for the duration of the save using
	 * {@link OAThreadLocalDelegate#setAdmin(boolean)}, and the previous admin value
	 * is restored afterward. If the method determined earlier that the object was
	 * unchanged but a save was still required, the object's internal changed flag is
	 * updated after the delegate save completes.
	 *
	 * @param cascadeRule the cascade rule controlling how linked objects are saved
	 */
	public void save(int iCascadeRule) {
		if (!canSave()) {
			throw new RuntimeException("can Save returned false for " + getClass().getSimpleName());
		}

		OA oa = OARuntime.oa(this);
		oa.internal().objects().save().save(this, iCascadeRule); // this will save on server if using OAClient
	}

	/**
	 * Determines whether this object is permitted to be saved.
	 * <p>
	 * The method allows saving by default when the object either:
	 * <ul>
	 *   <li>has no {@code OAObjectKey} assigned, or</li>
	 *   <li>has an {@code OAObjectKey} marked as new.</li>
	 * </ul>
	 * In all other cases, it temporarily enables administrative mode via
	 * {@link OAThreadLocalDelegate#setAdmin(boolean)} and delegates the decision to
	 * {@link OAObjectDelegate#canSave(OAObject)}. The admin flag is restored after
	 * evaluation.
	 *
	 * @return {@code true} if the object may be saved, {@code false} otherwise
	 */
	public boolean canSave() {
		OA oa = OARuntime.oa(this);
		boolean flag = oa.internal().objects().rules().getAllowSave(this);
		return flag;
	}

	/**
	 * Saves this object and all linked objects participating in the default
	 * cascade-save rules.
	 * <p>
	 * This method delegates directly to
	 * {@link OAObjectDelegate#saveAll(OAObject)}, which performs the full
	 * cascade-aware save operation.  The delegate is responsible for:
	 * <ul>
	 *   <li>validating whether this object is permitted to be saved,</li>
	 *   <li>saving this object when it is new or changed,</li>
	 *   <li>saving linked objects whose metadata specifies cascade-save,</li>
	 *   <li>processing owner/owned relationships according to model rules,</li>
	 *   <li>clearing change flags upon successful persistence,</li>
	 *   <li>triggering save-related callbacks and event notifications.</li>
	 * </ul>
	 * <p>
	 * This method applies <b>default</b> cascade rules
	 * ({@link #CASCADE_LINK_RULES}) and does not allow callers to specify
	 * alternative cascade modes.  For customized cascade behavior, use
	 * the save methods provided by the OA framework at the delegate level.
	 */
	public void saveAll() {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().save().save(this, OAObject.CASCADE_ALL_LINKS);
	}

	/**
	 * Callback invoked after this object has been saved.
	 * <p>
	 * The default implementation does nothing. Subclasses may override this
	 * method to perform post-save logic such as clearing transient state,
	 * recalculating derived values, or triggering additional application-specific
	 * actions.
	 * <p>
	 * This method is guaranteed to be called after the OA save process completes,
	 * whether initiated through {@code save()}, {@code saveAll()}, or delegate-
	 * driven cascade operations.
	 */
	public void afterSave() {
	}

	/**
	 * Deletes this object from the OA model and its underlying datasource.
	 * <p>
	 * Before deletion, this method evaluates delete permissions through
	 * the OA object rules engine
	 * unless the current thread is a remote thread. If the callback denies deletion,
	 * a {@link RuntimeException} is thrown containing the callback's message and
	 * optional cause.
	 * <p>
	 * When validation succeeds, the actual deletion process is delegated to
	 * {@link OAObjectDeleteService#delete(OAObject)}, which performs all framework-
	 * level deletion handling including event firing, Hub removal, and datasource
	 * delete operations.
	 */
	public void delete() {
		// verify with objectCallback
		OA oa = OARuntime.oa(this);
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		if (!srvcOARemoteThread.isRemoteThread()) {
			OAObjectCallback em = oa.internal().objects().rules().getVerifyDeleteObjectCallback(null, this);
			if (!em.getAllowed()) {
				String s = em.getResponse();
				if (OAString.isEmpty(s)) {
					s = "edit query returned false for delete, object=" + this;
				}
				throw new RuntimeException(s, em.getThrowable());
			}
		}
		oa.internal().objects().delete().delete(this);
	}

	/**
	 * Determines whether this object is permitted to be deleted according to
	 * OA's delete-validation rules.
	 * <p>
	 * This method delegates to the OA object rules engine, which
	 * evaluates metadata rules and any registered {@link OAObjectCallback}
	 * handlers to decide whether deletion is allowed for the current context.
	 *
	 * @return {@code true} if this object is allowed to be deleted;
	 *         {@code false} otherwise
	 */
	public boolean canDelete() {
		OA oa = OARuntime.oa(this);
		boolean b = oa.internal().objects().rules().getAllowDelete(this);
		return b;
	}

	/**
	 * Callback invoked after this object has been deleted.
	 * <p>
	 * The default implementation does nothing. Subclasses may override this
	 * method to perform post-delete logic such as cleanup, audit logging,
	 * removing transient state, or triggering additional domain-level actions.
	 * <p>
	 * This method is called by the OA delete framework after the deletion
	 * process has completed.
	 */
	public void afterDelete() {
	}

	/**
	 * Creates a lock on this object.
	 * <p>
	 * This method delegates to {@link OAObjectLockDelegate#lock(OAObject)},
	 * which performs all framework-level locking behavior, including
	 * registering the lock and enforcing any lock rules defined in the
	 * OA locking subsystem.
	 * <p>
	 * Locking is typically used to prevent other threads or processes
	 * from modifying the object while it is in a protected state.
	 */
	public void lock() {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().lock().lock(this);
	}

	/**
	 * Releases the lock previously created on this object.
	 * <p>
	 * This method delegates to {@link OAObjectLockDelegate#unlock(OAObject)},
	 * which performs all framework-level unlocking behavior, including
	 * clearing the lock state and notifying any systems that depend on
	 * object-lock status.
	 * <p>
	 * Unlocking typically allows other threads or processes to resume
	 * modifications to the object after it leaves a protected state.
	 */
	public void unlock() {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().lock().unlock(this);
	}

	/**
	 * Determines whether this object is currently locked.
	 * <p>
	 * The lock state is obtained from
	 * {@link OAObjectLockDelegate#isLocked(OAObject)}, which manages all
	 * framework-level locking behavior.
	 *
	 * @return {@code true} if the object is locked; {@code false} otherwise
	 */
	public boolean isLocked() {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().lock().isLocked(this);
	}

	/**
	 * Finds the first object reachable from this object through the specified
	 * property path whose value matches the provided comparison value.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectDelegate#find(OAObject, String, Object, boolean)}
	 * with {@code false} for the "find all" flag, meaning only the first
	 * matching object (if any) is returned.
	 *
	 * @param path the property path used to navigate from this object
	 * @param value        the value to match against while searching
	 * @return the first matching object, or {@code null} if none are found
	 */
	public Object find(String path, Object value) {
		OA oa = OARuntime.oa(this);
		Object[] objs = oa.internal().objects().find().find(this, path, value, false);
		if (objs != null && objs.length > 0) {
			return objs[0];
		}
		return null;
	}

	/**
	 * Finds all objects reachable from this object through the specified
	 * property path whose value matches the provided comparison value.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectDelegate#find(OAObject, String, Object, boolean)}
	 * with {@code true} for the "find all" flag, causing the delegate to
	 * return every matching object encountered during traversal.
	 *
	 * @param path the property path used to navigate from this object
	 * @param value        the value to match against while searching
	 * @return an array of all matching objects; never {@code null}, but may be empty
	 */
	public Object[] findAll(String path, Object value) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().find().find(this, path, value, true);
	}

	/**
	 * Determines whether the specified property is considered null.
	 * <p>
	 * This is a convenience method that delegates directly to
	 * {@link #isNull(String)}, which performs the full evaluation using
	 * OA's null-tracking and property-resolution rules.
	 *
	 * @param prop the property name to evaluate
	 * @return {@code true} if the property is null; {@code false} otherwise
	 */
	public boolean getNull(String prop) {
		return isNull(prop);
	}

	/**
	 * Determines whether the specified property is considered null according to
	 * OA's primitive-null tracking and property-resolution rules.
	 * <p>
	 * The method first checks OA's primitive-null flag for the property via
	 * {@link OAObjectReflectDelegate#getPrimitiveNull(OAObject, String)}.
	 * If that flag is not set, it retrieves the property's stored value
	 * (possibly a {@link WeakReference} or {@link OAMatchNotExist} placeholder)
	 * using {@link OAObjectPropertyDelegate#getProperty(OAObject, String, boolean, boolean)}.
	 * <p>
	 * A property is considered null when:
	 * <ul>
	 *   <li>its primitive-null flag is set, or</li>
	 *   <li>its stored value is {@code null}, or</li>
	 *   <li>its stored value is {@code OAMatchNotExist} and the resolved property
	 *       value obtained from {@link #getProperty(String)} is also {@code null}.</li>
	 * </ul>
	 *
	 * @param prop the property name to evaluate
	 * @return {@code true} if the property is null; {@code false} otherwise
	 */
	public boolean isNull(String prop) {
		OA oa = OARuntime.oa(this);
		boolean b = oa.internal().objects().reflect().getPrimitiveNull(this, prop);
		if (!b) {
			Object objx = oa.internal().objects().property().getProperty(this, prop, true, false);
			if (objx == null) {
				b = true;
			} else if (!(objx instanceof OAMatchNotExist)) {
				return false;
			} else if (getProperty(prop) == null) {
				b = true;
			}
		}
		return b;
	}

	/**
	 * Determines whether the current thread is an OA remote thread, used to process
	 * OASync messages.
	 * <p>
	 * This method delegates to
	 * {@link OARemoteThreadDelegate#isRemoteThread()}, which identifies
	 * whether the calling thread is part of the OA remote message-processing
	 * infrastructure.
	 *
	 * @return {@code true} if the current thread is a remote thread;
	 *         {@code false} otherwise
	 */
	public boolean isRemoteThread() {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		return srvcOARemoteThread.isRemoteThread();
	}


	/**
	 * Enables or disables OA sync message sending for the current thread.
	 * <p>
	 * This updates the OA thread-local service flag used by runtime operations that
	 * participate in synchronization.
	 *
	 * @param b {@code true} to allow sync message sending; {@code false} to suppress it
	 */
	public void sendMessages(boolean b) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
        srvcOAThreadLocal.setSendSyncMessages(b);
	}

/**
	 * Callback invoked after this object has been loaded from a datasource.
	 * <p>
	 * The default implementation initializes any empty hubs associated with
	 * the object and then triggers framework-level after-load events through:
	 * <ul>
	 *   <li>{@link OAObjectEmptyHubDelegate#initialize(OAObject)}</li>
	 *   <li>{@link OAObjectEventDelegate#fireAfterLoadEvent(OAObject)}</li>
	 *   <li>{@link OAObjectCacheDelegate#fireAfterLoadEvent(OAObject)}</li>
	 * </ul>
	 * Subclasses may override this method to perform additional post-load
	 * initialization or custom logic.
	 */
	public void afterLoad() {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().event().fireAfterLoadEvent(this);
		oa.internal().objects().cache().fireAfterLoadEvent(this);
	}

	/**
	 * Returns the {@link OAObjectKey} associated with this object.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectKeyDelegate#callKeyGetKey(OAObject)}, which retrieves the
	 * framework-managed identity key used for GUID-based identification,
	 * caching, and remote synchronization.
	 *
	 * @return the {@code OAObjectKey} for this object; never {@code null}
	 *         once the object has been initialized
	 */
	public OAObjectKey getObjectKey() {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().key().getKey(this);
	}

	/**
	 * Returns the globally unique identifier (GUID) assigned to this object.
	 * <p>
	 * The GUID is a core component of OA’s identity system and is used for
	 * equality checks, hashing, caching, remote synchronization, and ensuring
	 * the single-instance-per-object rule across the OA model.
	 *
	 * @return the GUID value for this object
	 */
	public UUID getGuid() {
		return guid;
	}

	/**
	 * Sets whether this object should be automatically added to master or
	 * reference hubs when one of its OAObject properties is assigned.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectDelegate#setAutoAdd(OAObject, boolean)}, which manages
	 * the framework-level behavior that determines when an object is inserted
	 * into its parent hubs during property assignment.
	 *
	 * @param b {@code true} to enable automatic hub insertion;
	 *          {@code false} to defer insertion until explicitly added or saved
	 */
	public void setAutoAdd(boolean b) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().autoAdd().setAutoAdd(this, b);
	}

	/**
	 * Returns whether this object is configured to be automatically added to
	 * master or reference hubs when one of its OAObject properties is set.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectDelegate#getAutoAdd(OAObject)} for retrieving the
	 * current auto-add setting. The method is annotated with
	 * {@code @XmlTransient} to ensure that the auto-add state is not
	 * serialized.
	 *
	 * @return {@code true} if automatic hub insertion is enabled;
	 *         {@code false} otherwise
	 */
	public boolean getAutoAdd() {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().autoAdd().getAutoAdd(this);
	}

	/**
	 * Determines whether the given object is considered empty.
	 * <p>
	 * This method delegates to {@link OAString#isEmpty(Object)}, which applies
	 * OA's standard emptiness rules for strings, collections, arrays, and
	 * other supported object types.
	 *
	 * @param obj the object to evaluate
	 * @return {@code true} if the object is empty; {@code false} otherwise
	 */
	public boolean isEmpty(Object obj) {
		return OAString.isEmpty(obj);
	}

	/**
	 * Determines whether the hub property with the given name has been loaded.
	 * <p>
	 * This method retrieves the raw stored value for the property using
	 * {@link OAObjectPropertyDelegate#getProperty(OAObject, String, boolean, boolean)}.
	 * A hub is considered loaded when:
	 * <ul>
	 *   <li>the property value is a non-null hub instance, or</li>
	 *   <li>the value is {@code null} (explicitly loaded as empty).</li>
	 * </ul>
	 * It is considered not loaded when:
	 * <ul>
	 *   <li>the stored value is {@link OAMatchNotExist}, indicating the property has
	 *       not yet been resolved, or</li>
	 *   <li>the value is a {@link WeakReference} whose referent has been cleared.</li>
	 * </ul>
	 *
	 * @param name the hub property name
	 * @return {@code true} if the hub is loaded; {@code false} otherwise
	 */
	public boolean isHubLoaded(String name) {
		OA oa = OARuntime.oa(this);
		Object objx = oa.internal().objects().property().getProperty(this, name, true, true);
		if (objx == OAMatchNotExist.instance) {
			return false;
		}
		if (objx == null) {
			return true;
		}
		if (objx instanceof WeakReference) {
			if (((WeakReference) objx).get() == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Loads this object's reference properties with fine-grained control over
	 * which types of references are included.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#loadAllReferences(OAObject, boolean, boolean, boolean)},
	 * which loads:
	 * <ul>
	 *   <li>one-to-one references when {@code bOne} is {@code true},</li>
	 *   <li>one-to-many references when {@code bMany} is {@code true},</li>
	 *   <li>calculated references when {@code bIncludeCalc} is {@code true}.</li>
	 * </ul>
	 *
	 * @param bOne          whether to load one-to-one references
	 * @param bMany         whether to load one-to-many references
	 * @param bIncludeCalc  whether to include calculated references
	 */
	public void loadReferences(boolean bIncludeCalc) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().loadAllReferences(this, bIncludeCalc);
	}

	/**
	 * Loads this object's reference properties with fine-grained control over
	 * which types of references are included.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#loadAllReferences(OAObject, boolean, boolean, boolean)},
	 * which loads:
	 * <ul>
	 *   <li>one-to-one references when {@code bOne} is {@code true},</li>
	 *   <li>one-to-many references when {@code bMany} is {@code true},</li>
	 *   <li>calculated references when {@code bIncludeCalc} is {@code true}.</li>
	 * </ul>
	 *
	 * @param bOne          whether to load one-to-one references
	 * @param bMany         whether to load one-to-many references
	 * @param bIncludeCalc  whether to include calculated references
	 */
	public void loadReferences(boolean bOne, boolean bMany, boolean bIncludeCalc) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().loadAllReferences(this, bOne, bMany, bIncludeCalc);
	}

	
	/**
	 * Loads reference properties for this object up to the specified depth.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#loadAllReferences(OAObject, int, int, boolean)},
	 * allowing callers to control:
	 * <ul>
	 *   <li>the maximum number of reference levels to load,</li>
	 *   <li>the number of additional owned-reference levels to include,</li>
	 *   <li>whether calculated references should be included.</li>
	 * </ul>
	 *
	 * @param maxLevelsToLoad              maximum reference-depth to load
	 * @param additionalOwnedLevelsToLoad  additional owned-reference levels to include
	 * @param bIncludeCalc                 whether to include calculated references
	 */
	public void loadReferences(int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		OA oa = OARuntime.oa(this);
		oa.internal().objects().reflect().loadAllReferences(this, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc);
	}

	/**
	 * Loads reference properties for this object with full control over
	 * depth, owned-reference levels, inclusion of calculated references,
	 * and a maximum reference-count limit.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#loadAllReferences(OAObject, int, int, boolean, int)},
	 * which performs the actual traversal of reference properties according
	 * to the supplied limits.
	 *
	 * @param maxLevelsToLoad              maximum reference-depth to load
	 * @param additionalOwnedLevelsToLoad  additional owned-reference levels to include
	 * @param bIncludeCalc                 whether calculated references should be included
	 * @param maxRefsToLoad                maximum number of references to load before stopping
	 */
	public void loadReferences(int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad) {
		OA oa = OARuntime.oa(this);
		int x = oa.internal().objects().reflect().loadAllReferences(this, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, maxRefsToLoad);
	}

	/**
	 * Invokes the calling method on the server for all objects contained in the
	 * specified hub. This enables transparent remote execution of methods that
	 * are intended to run only on the server.
	 * <p>
	 * The method determines the originating method name via the call stack and
	 * delegates the remote invocation to the {@link RemoteServerInterface}
	 * obtained from the active {@link OASyncClient}.
	 * <p>
	 * Remote execution is not allowed when:
	 * <ul>
	 *   <li>the hub's object type is already executing on the server, or</li>
	 *   <li>the current thread is an OA remote thread.</li>
	 * </ul>
	 *
	 * @param hub  the hub whose object type determines the server-side class
	 * @param args the arguments to forward to the remotely invoked method
	 * @return the result returned by the remote server, or {@code null} if the hub is {@code null}
	 * @throws RuntimeException if remote execution is not permitted or if the remote infrastructure is unavailable
	 */
	public static Object callRemote(Hub hub, Object... args) {
		if (hub == null) {
			return null;
		}

		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		String mname = sts[2].getMethodName();

		final Class clazz = hub.getObjectClass();

		final OA oa = OARuntime.oa(hub);
		
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		if (!oa.sync().isClient() || srvcOARemoteThread.isRemoteThread()) {
			throw new RuntimeException("method " + mname + ", isRemoable=false, thread=" + Thread.currentThread());
		}

		final OASyncClient sc = oa.internal().sync().getClient();
		if (sc == null) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread());
		}

		RemoteServerInterface rs;
		try {
			rs = sc.getRemoteServer();
		} catch (Exception e) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread(), e);
		}

		if (rs == null) {
			throw new RuntimeException("method " + mname + ", RemoteServerInterface=null, thread=" + Thread.currentThread());
		}

		Object result = rs.runRemoteMethod(hub, mname, args);
		return result;
	}

	/**
	 * Invokes the calling method on the server for this object instance.
	 * <p>
	 * The method name is inferred from the call stack, and the request is
	 * forwarded to the remote server using the active {@link OASyncClient}
	 * and its {@link RemoteServerInterface}.
	 * <p>
	 * Remote execution is only permitted when {@link #isRemoteAvailable()}
	 * returns {@code true}. If remote execution is unavailable or the
	 * synchronization infrastructure is not initialized, a
	 * {@link RuntimeException} is thrown.
	 * <p>
	 * The remote invocation uses either:
	 * <ul>
	 *   <li>{@code runRemoteMethod2(this, methodName, args)} when the object
	 *       instance is not already present on the server, or</li>
	 *   <li>{@code runRemoteMethod(getClass(), OAObjectKey, methodName, args)}
	 *       when the server already has an instance for this object.</li>
	 * </ul>
	 *
	 * @param args the arguments to pass to the remotely invoked method
	 * @return the result returned by the server
	 * @throws RuntimeException if remote execution is unavailable or any
	 *         required remote component is missing
	 */
	public Object remote(Object... args) {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		String mname = sts[2].getMethodName();

		if (!isRemoteAvailable()) {
			throw new RuntimeException("method " + mname + ", isRemoable=false, thread=" + Thread.currentThread());
		}

		final OA oa = OARuntime.oa(this);
		final OASyncClient sc = oa.internal().sync().getClient();
		if (sc == null) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread());
		}

		RemoteServerInterface rs;
		try {
			rs = sc.getRemoteServer();
		} catch (Exception e) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread(), e);
		}

		if (rs == null) {
			throw new RuntimeException("method " + mname + ", RemoteServerInterface=null, thread=" + Thread.currentThread());
		}

		
		Object val;
		if (!sc.isObjectOnServer(this)) val = rs.runRemoteMethod2(this, mname, args); 		
		else {
			val = rs.runRemoteMethod(getClass(), oa.internal().objects().key().getKey(this), mname, args);
		}

		return val;
	}

	/**
	 * Determines whether a unique constraint is satisfied for the specified
	 * property and value on this object's type.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectUniqueDelegate#getUnique(Class, String, Object, boolean)}
	 * with {@code false} for auto-create. The delegate searches for an
	 * existing instance whose property matches the given value.
	 *
	 * @param property the unique-property name
	 * @param value    the value to check for uniqueness
	 * @return {@code true} if an existing object with the given value exists;
	 *         {@code false} otherwise
	 */
	public boolean isUnique(String property, Object value) {
        OA oa = OARuntime.oa(this);
		OAObject obj = oa.internal().objects().unique().getUnique(getClass(), property, value, false);
		return (obj != null);
	}

	/**
	 * Retrieves or creates (optionally) the unique instance of the specified
	 * {@link OAObject} subclass for the given unique-property value.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectUniqueDelegate#getUnique(Class, String, Object, boolean)},
	 * which searches for an existing instance whose unique-property matches the
	 * provided key. When {@code bAutoCreate} is {@code true}, a new object is
	 * created if none exists.
	 *
	 * @param clazz         the OAObject subclass to search
	 * @param propertyName  the name of the unique property
	 * @param uniqueKey     the value identifying the unique instance
	 * @param bAutoCreate   whether to create a new instance if none exists
	 * @return the existing or newly created unique object, or {@code null} if
	 *         none exists and auto-create is disabled
	 */
	public static OAObject getUniqueInstance(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey,
			final boolean bAutoCreate) {
        OA g = OARuntime.oa(clazz);
		OAObject obj = g.internal().objects().unique().getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
		return obj;
	}

	/**
	 * Determines whether remote method invocation is available for this object
	 * on the current thread.
	 * <p>
	 * Remote execution is not allowed when:
	 * <ul>
	 *   <li>the current thread is an OA remote thread, or</li>
	 *   <li>the object is executing on the server rather than a client.</li>
	 * </ul>
	 * <p>
	 * This method delegates part of its logic to OA.sync().isClient() to ensure
	 * that only client-side threads that are not remote threads may initiate
	 * remote calls.
	 *
	 * @return {@code true} if remote invocation is permitted; {@code false} otherwise
	 */
	public boolean isRemoteAvailable() {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		if (srvcOARemoteThread.isRemoteThread()) {
			return false;
		}
		final OA oa = OARuntime.oa(this);
		return oa.sync().isClient();
	}


	/**
	 * Determines whether remote method invocation is available for objects in a Hub.
	 * <p>
	 * Remote execution is available only when the current thread is not processing a
	 * remote message and the Hub's object class is running in a client OA runtime.
	 *
	 * @param hub the Hub whose object class supplies the runtime context
	 * @return {@code true} if remote invocation is available for the Hub
	 */
	public static boolean isRemoteAvailable(Hub hub) {
		if (hub == null) {
			return false;
		}
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		if (srvcOARemoteThread.isRemoteThread()) {
			return false;
		}
		final Class clazz = hub.getObjectClass();
		final OA oa = OARuntime.oa(clazz);
		if (!oa.sync().isClient()) {
			return false;
		}
		return true;
	}

	/**
	 * Determines whether the specified property has been loaded.
	 * <p>
	 * This method delegates directly to
	 * {@link OAObjectPropertyDelegate#isPropertyLoaded(OAObject, String)},
	 * which checks whether the property value has been resolved from the
	 * datasource or OA runtime, including lazy-loaded fields.
	 *
	 * @param prop the property name to evaluate
	 * @return {@code true} if the property is loaded; {@code false} otherwise
	 */
	public boolean isLoaded(String prop) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().property().isPropertyLoaded(this, prop);
	}

	/**
	 * Determines whether the specified property has been loaded.
	 * <p>
	 * This is an alias for {@link #isLoaded(String)} and delegates to
	 * {@link OAObjectPropertyDelegate#isPropertyLoaded(OAObject, String)},
	 * which checks whether the value for the given property has been resolved
	 * from the datasource or the OA runtime.
	 *
	 * @param prop the property name to check
	 * @return {@code true} if the property is loaded; {@code false} otherwise
	 */
	public boolean isPropertyLoaded(String prop) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().property().isPropertyLoaded(this, prop);
	}

	/**
	 * Determines whether the specified reference property is explicitly marked as null.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectPropertyDelegate#isReferenceNull(OAObject, String)},
	 * which checks OA’s reference-null tracking for one-to-one and other
	 * reference-type properties.
	 *
	 * @param prop the reference property name
	 * @return {@code true} if the reference is explicitly null; {@code false} otherwise
	 */
	public boolean isReferenceNull(String prop) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().property().isReferenceNull(this, prop);
	}

	/**
	 * Performs a hierarchical search starting from this object, returning the
	 * first non-empty value found for the specified property by navigating
	 * upward through the given hierarchy path.
	 * <p>
	 * This method constructs an {@link OAHierFinder} using the supplied
	 * property name and hierarchy path, then invokes
	 * {@code findFirstNotEmpty(this)} to walk the hierarchy until a
	 * non-null, non-empty value is found.
	 *
	 * @param propertyName            the property to evaluate at each hierarchy level
	 * @param heirarchyPath   the path used to navigate upward through the hierarchy
	 * @return the first non-empty value encountered, or {@code null} if none is found
	 */
	public Object hierFind(String propertyName, String heirarchyPath) {
		OAHierFinder hf = new OAHierFinder<OAObject>(propertyName, heirarchyPath);
		Object objx = hf.findFirstNotEmpty(this);
		return objx;
	}

	/**
	 * Retrieves the {@link OAObjectKey} associated with the specified
	 * reference property.
	 * <p>
	 * The method first obtains the raw property value using
	 * {@link OAObjectPropertyDelegate#getProperty(OAObject, String)}.
	 * If the value is an {@link OAObject}, its object key is returned.
	 * If the value is already an {@link OAObjectKey}, it is returned
	 * directly.
	 *
	 * @param prop the name of the reference property
	 * @return the {@code OAObjectKey} for the referenced object, or
	 *         {@code null} if the reference is not set or not an OA object
	 */
	public OAObjectKey getReferenceObjectKey(String prop) {
		OA oa = OARuntime.oa(this);
		Object obj = oa.internal().objects().property().getProperty(this, prop);
		if (obj instanceof OAObject) {
			obj = ((OAObject) obj).getObjectKey();
		}
		if (obj instanceof OAObjectKey) {
			return (OAObjectKey) obj;
		}
		return null;
	}

	/**
	 * Begins a server-only execution block for this object.
	 * <p>
	 * This method performs two checks before enabling server-only behavior:
	 * <ul>
	 *   <li>Returns {@code false} immediately if the object is currently loading.</li>
	 *   <li>Returns {@code false} if the object's class is not executing on the server,
	 *       as determined by OA.sync().isServer().</li>
	 * </ul>
	 * <p>
	 * If both checks pass, this method enables remote-message sending for the current
	 * thread by invoking {@link OARemoteThreadDelegate#sendSyncMessages(boolean)} with
	 * {@code true}. This marks the beginning of a server-only execution region.
	 *
	 * @return {@code true} if server-only execution is allowed and remote message
	 *         sending has been enabled; {@code false} otherwise
	 * @see #endServerOnly()
	 */
	public boolean startServerOnly() {
		if (isLoading()) {
			return false;
		}
		final OA oa = OARuntime.oa(this);
		if (oa.sync().isClient()) {
			return false;
		}

		OAThreadService srvcThread = OARuntime.thread();
		srvcThread.getThreadLocalService().startServerOnly();
		return true;
	}

	/**
	 * Ends a server-only execution block previously started with
	 * {@link #beginServerOnly()}.
	 * <p>
	 * This method performs two checks before disabling remote-message sending:
	 * <ul>
	 *   <li>If the object is currently loading, the method returns immediately.</li>
	 *   <li>If the object's class is not executing on the server, the method returns immediately.</li>
	 * </ul>
	 * <p>
	 * If both checks pass and the current thread is flagged as sending remote
	 * messages, this method disables message sending by invoking
	 * {@link OARemoteThreadDelegate#sendSyncMessages(boolean)} with {@code false}.
	 */
	public void endServerOnly() {
		if (isLoading()) {
			return;
		}
		final OA oa = OARuntime.oa(this);
		if (oa.sync().isClient()) {
			return;
		}

		OAThreadService srvcThread = OARuntime.thread();
		srvcThread.getThreadLocalService().endServerOnly();
	}

	/**
	 * Executes the provided {@link Runnable} within a server-only execution block.
	 * <p>
	 * The method performs an initial check to ensure that the runnable is not
	 * {@code null} and that server-only execution can begin. If either condition
	 * fails, the method returns immediately.
	 * <p>
	 * When execution is allowed, this method:
	 * <ol>
	 *   <li>Calls {@link #beginServerOnly()} to enable server-only behavior,</li>
	 *   <li>Runs the provided runnable,</li>
	 *   <li>Ensures {@link #endServerOnly()} is invoked in a {@code finally} block
	 *       to restore message-sending state.</li>
	 * </ol>
	 *
	 * @param r the runnable to execute on the server; ignored if {@code null}
	 */
	public void runOnServerOnly(Runnable r) {
		if (r == null || !startServerOnly()) {
			return;
		}
		try {
			r.run();
		} finally {
			endServerOnly();
		}
	}

	private static boolean DebugMode = false;

	/**
	 * Sets the global debug mode flag for all {@code OAObject} instances.
	 * <p>
	 * This updates the internal static {@code DebugMode} field.
	 *
	 * @param b {@code true} to enable debug mode; {@code false} to disable it
	 */
	public static void setDebugMode(boolean b) {
		LOG.config("DebugMode set to " + b);
		DebugMode = b;
	}

	/**
	 * Returns the current global debug mode flag for {@code OAObject}.
	 *
	 * @return {@code true} if debug mode is enabled; {@code false} otherwise
	 */
	public static boolean getDebugMode() {
		return DebugMode;
	}

	/**
	 * Determines whether the specified property is currently locked.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectPropertyDelegate#isPropertyLocked(OAObject, String)},
	 * which checks whether framework-level locking rules prevent the given
	 * property from being modified.
	 *
	 * @param prop the property name to check
	 * @return {@code true} if the property is locked; {@code false} otherwise
	 */
	public boolean isPropertyLocked(String prop) {
		OA oa = OARuntime.oa(this);
		boolean b = oa.internal().objects().lock().isPropertyLocked(this, prop);
		return b;
	}

	/**
	 * Returns whether this object is considered submitted according to its
	 * metadata-defined submit property or, if none exists, by recursively
	 * checking its owner objects.
	 * <p>
	 * This method simply delegates to {@link #_isSubmitted(int)} with an initial
	 * recursion depth of {@code 0}.
	 *
	 * @return {@code true} if the object or its owning hierarchy is submitted;
	 *         {@code false} otherwise
	 */
	public boolean isSubmitted() {
		return _isSubmitted(0);
	}

	/**
	 * Recursively determines whether this object is considered submitted based on
	 * its metadata-defined submit property or, if none exists, by evaluating its
	 * owned objects.
	 * <p>
	 * The method enforces a recursion limit of 10 levels to prevent infinite
	 * cycles in OA models. If this limit is exceeded, a warning is logged and
	 * the method returns {@code true}.
	 * <p>
	 * Behavior:
	 * <ul>
	 *   <li>If a submit property exists, its value is retrieved via
	 *       {@link #getProperty(String)} and converted to a boolean using
	 *       {@code OAConv.toBoolean(Object)}.</li>
	 *   <li>If no submit property exists, each owned link is retrieved from
	 *       {@link OAObjectInfoDelegate#callInfoGetObjectInfo(Class)} and examined
	 *       recursively.</li>
	 *   <li>If any owned object is not submitted, this object is considered
	 *       not submitted.</li>
	 * </ul>
	 *
	 * @param cnt the current recursion depth
	 * @return {@code true} if the submit condition is satisfied; {@code false} otherwise
	 */
	public boolean _isSubmitted(int cnt) {
		if (cnt > 10) {
			String s = "recursive > 10, will return true and continue";
			LOG.log(Level.WARNING, "recursive, obj=" + this, new Exception(s));
			return true;
		}
		OA oa = OARuntime.oa(this);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
		OAPropertyInfo pi = oi.getSubmitProperty();
		if (pi == null) {
			// check owner (recursive)
			OALinkInfo[] lis = oi.getOwnedLinkInfos();
			if (lis != null) {
				for (OALinkInfo li : lis) {
					Object objx = li.getValue(this);
					if (objx instanceof OAObject) {
						boolean b = ((OAObject) objx)._isSubmitted(cnt + 1);
						if (!b) {
							return false;
						}
					}
				}
			}
			return true;
		}
		Object objx = this.getProperty(pi.getName());
		if (objx == null) {
			return false;
		}
		boolean b = OAConv.toBoolean(objx);
		return b;
	}

	/**
	 * Performs a compare-and-swap (CAS) operation on the specified property using
	 * a distributed lock.
	 * <p>
	 * This convenience method delegates to
	 * {@link #compareAndSwap(String, Object, Object, boolean)} with the distributed
	 * lock option enabled.
	 *
	 * @param property   the name of the property to update
	 * @param oldValue   the expected current value
	 * @param newValue   the new value to assign if the comparison succeeds
	 * @return result returned from delegate
	 */
	public boolean compareAndSwap(String property, Object oldValue, Object newValue) {
		return compareAndSwap(property, oldValue, newValue, true);
	}

	/**
	 * Performs a compare-and-swap (CAS) operation on the specified property.
	 * <p>
	 * If {@code bUseDistributedLock} is {@code true}, this method acquires a
	 * distributed lock via {@link #lock()}, retrieves the current property value,
	 * compares it to {@code oldValue} using {@code OACompare.compare}, and—if the
	 * comparison succeeds—updates the property with {@code newValue}. The lock is
	 * always released in a {@code finally} block.
	 * <p>
	 * If {@code bUseDistributedLock} is {@code false}, the same CAS operation is
	 * performed using a local {@code synchronized(this)} block instead of a
	 * distributed lock.
	 * <p>
	 * If {@code property} is {@code null} or empty, the method immediately returns
	 * {@code false}.
	 *
	 * @param property the property name to update
	 * @param oldValue the expected current value
	 * @param newValue the new value to assign if the comparison succeeds
	 * @param bUseDistributedLock whether to use a distributed lock or a local lock
	 * @return {@code true} if the property was successfully updated;
	 *         {@code false} if the comparison failed or the property name is empty
	 */
	public boolean compareAndSwap(String property, Object oldValue, Object newValue, final boolean bUseDistributedLock) {
		if (OAString.isEmpty(property)) {
			return false;
		}

		if (bUseDistributedLock) {
			lock();
			try {
				Object val = getProperty(property);
				if (OACompare.compare(val, oldValue) != 0) {
					return false;
				}
				setProperty(property, newValue);
			} finally {
				unlock();
			}
		} else {
			synchronized (this) {
				Object val = getProperty(property);
				if (OACompare.compare(val, oldValue) != 0) {
					return false;
				}
				setProperty(property, newValue);
			}
		}
		return true;
	}

	/**
	 * Hook invoked during OAObject initialization.
	 * <p>
	 * The default implementation does nothing. Subclasses may override this
	 * method to initialize default property values or perform additional setup
	 * logic immediately after construction.
	 */
	public void setObjectDefaults() {
	}

	/**
	 * Attempts to set the foreign-key value for the specified property.
	 * <p>
	 * This method resolves the metadata for the given foreign-key property name,
	 * locates the corresponding {@link OALinkInfo} and {@link OAFkeyInfo} that
	 * reference it, and then delegates the update to the protected
	 * {@link #setFkeyProperty(String, OALinkInfo, OAFkeyInfo, Object)} method.
	 * <p>
	 * If no matching foreign-key definition is found, or if the property name is
	 * {@code null}, the method returns {@code false}.
	 *
	 * @param fkeyPropertyName the name of the foreign-key property to update
	 * @param newValue the new value to apply to the foreign key
	 * @return {@code true} if a matching foreign-key definition is found and the
	 *         update is delegated; {@code false} otherwise
	 */
	public boolean setFkeyProperty(final String fkeyPropertyName, final Object newValue) {
		if (fkeyPropertyName == null) {
			return false;
		}

		OA oa = OARuntime.oa(this);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
		final OAPropertyInfo pi = oi.getPropertyInfo(fkeyPropertyName);
		if (pi != null) {
			for (OALinkInfo li : oi.getLinkInfos()) {
				for (OAFkeyInfo fi : li.getFkeyInfos()) {
					if (fi.getFromPropertyInfo() != pi) {
						continue;
					}
					return setFkeyProperty(fkeyPropertyName, li, fi, newValue);
				}
			}
		}
		return false;
	}

	/**
	 * Updates the foreign-key property for the specified link based on its
	 * metadata definition.
	 * <p>
	 * This method determines whether the foreign-key value should change by
	 * comparing the current linked object (retrieved through
	 * {@link OAObjectPropertyDelegate#getProperty(OAObject, String, boolean, boolean)})
	 * with the new foreign-key value. If both values are {@code null}, or if the
	 * provided new value is {@code null} and the existing value is also
	 * {@code null}, no update is performed and the method returns {@code false}.
	 * <p>
	 * When updating, this method:
	 * <ul>
	 *   <li>Retrieves the target object's primary-key property names,</li>
	 *   <li>Maps those key values into the foreign-key fields of this object,</li>
	 *   <li>Uses normal property-setting behavior to update the foreign-key
	 *       properties.</li>
	 * </ul>
	 * <p>
	 * A return value of {@code true} indicates that at least one foreign-key
	 * property value was modified.
	 *
	 * @param fkeyPropertyName the foreign-key property being updated
	 * @param linkInfo metadata describing the link whose foreign key is affected
	 * @param fi metadata describing the specific foreign-key mapping
	 * @param newValue the new foreign-key target value
	 * @return {@code true} if the foreign-key value was changed; {@code false} otherwise
	 */
	protected boolean setFkeyProperty(final String fkeyPropertyName, final OALinkInfo linkInfo, final OAFkeyInfo fi, Object newValue) {
	    final String linkName = linkInfo.getName();
	    String linkToPropertyName = fi.getToPropertyInfo().getName();

		final OA oa = OARuntime.oa(this);
        final Object oldValue = oa.internal().objects().property().getProperty(this, linkName, false, true);
		
		if (newValue == null) {
			if (oldValue == null) return false;
		}

		OAObjectInfo oiTo = linkInfo.getToObjectInfo();

		int pos = -1;
		final String[] pkeyNames = oiTo.getIdProperties();

		if (OAString.isNotEmpty(pkeyNames) && pkeyNames.length == 1) {
			pos = 0;
			linkToPropertyName = pkeyNames[pos];
		} else {
			int x = 0;
			for (String s : pkeyNames) {
				if (linkToPropertyName.equalsIgnoreCase(s)) {
					pos = x;
					break;
				}
				x++;
			}
		}
        if (pos < 0) {
            return false;
        }

        
		Object obj = oldValue;

		if (obj instanceof OAObject) {
			obj = ((OAObject) obj).getObjectKey();
		} else if (obj != null && !(obj instanceof OAObjectKey)) {
		    return false;
		}
		OAObjectKey ok = (OAObjectKey) obj;

		Object[] objs = new Object[pkeyNames.length];
		if (ok != null) {
		    Object[] ids = ok.getObjectIds();
		    if (ids != null) {
		        int max = Math.min(ids.length, objs.length);
		        for (int i = 0; i < max; i++) {
		            objs[i] = ids[i];
		        }
		    }
		}
		
		if (newValue == null) {
			/* 20260513 this will make it look like a '0' instead of null.
			OAPropertyInfo pi = fi.getToPropertyInfo();
			if (pi.getIsPrimitive()) {
				newValue = OAReflect.getEmptyPrimitive(pi.getClassType());
			}
			*/
		}
		objs[pos] = newValue;

		final OAObjectKey okNew = new OAObjectKey(objs);

		if (ok != null && ok.compareTo(okNew) == 0) {
			return false;
		}

		if (isLoading()) {
			oa.internal().objects().property().setProperty(this, linkName, okNew);
	        return true;
		}
		
		oa.internal().objects().reflect().setProperty(this, linkName, okNew, null);
		return true;
	}

	/**
	 * Retrieves the value of a foreign-key property.
	 * <p>
	 * This method first uses metadata to determine whether the given property
	 * name corresponds to a foreign-key component of any link defined on this
	 * object. If so, it identifies the matching link and forwards the request to
	 * {@link #getFkeyProperty(String, String)} using the link name and the
	 * mapped target-property name.
	 * <p>
	 * If the property is not explicitly defined as a foreign key, the method then
	 * checks whether the property name corresponds to a link name itself. If so,
	 * it resolves the foreign-key value for that link using a {@code null}
	 * target-property name (which is allowed only when the target object has a
	 * single primary key).
	 * <p>
	 * If no metadata match is found for either case, {@code null} is returned.
	 *
	 * @param fkeyPropertyName the foreign-key property name
	 * @return the resolved foreign-key value, or {@code null} if no matching
	 *         metadata or link is found
	 */
	public Object getFkeyProperty(final String fkeyPropertyName) {
		OA oa = OARuntime.oa(this);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
		OAPropertyInfo pi = oi.getPropertyInfo(fkeyPropertyName);

		if (pi != null) {
			for (OALinkInfo li : oi.getLinkInfos()) {
				for (OAFkeyInfo fi : li.getFkeyInfos()) {
					if (fi.getFromPropertyInfo() != pi) {
						continue;
					}
					return getFkeyProperty(li.getName(), fi.getToPropertyInfo().getName());
				}
			}
			return null;
		}

		OALinkInfo li = oi.getLinkInfo(fkeyPropertyName);
		if (li == null) {
			return null;
		}
		return getFkeyProperty(li.getName(), null);
	}

	/**
	 * Returns the foreign-key component value for a TYPE_ONE link on this object.
	 * <p>
	 * The method:
	 * <ul>
	 *   <li>Validates the link name.</li>
	 *   <li>Uses metadata to locate the link definition via
	 *       {@link OAObjectInfoDelegate#callInfoGetLinkInfo(OAObjectInfo, String)}.</li>
	 *   <li>Determines which primary-key component of the target object to return,
	 *       based on {@code linkToPropertyName}. If no property name is supplied
	 *       and the target object has exactly one primary key, the single PK field
	 *       is selected automatically.</li>
	 *   <li>Retrieves the current link value using
	 *       {@link OAObjectPropertyDelegate#getProperty(OAObject, String)}.</li>
	 *   <li>If the link value is an {@link OAObject}, its {@link OAObjectKey} is
	 *       extracted before reading the PK component.</li>
	 *   <li>If the link value is an {@link OAObjectKey}, the method returns the
	 *       ID at the determined PK index.</li>
	 * </ul>
	 *
	 * @param linkName the name of the link on this object
	 * @param linkToPropertyName the target object's primary-key property name
	 *                           (or {@code null} when the target object has only
	 *                           one primary key)
	 * @return the foreign-key component value, or {@code null} if unavailable
	 */
	public Object getFkeyProperty(final String linkName, String linkToPropertyName) {
		if (OAString.isEmpty(linkName)) {
			// throw new RuntimeException("linkName cant be empty, link=" + linkName);
		    return null;
		}

		OA oa = OARuntime.oa(this);
		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
		OALinkInfo linkInfo = oa.internal().objects().info().getLinkInfo(oi, linkName);
		if (linkInfo == null) {
			// throw new RuntimeException("linkName not found, link=" + linkName);
            return null;
		}

		OAObjectInfo oiTo = linkInfo.getToObjectInfo();

		int pos = -1;
		String[] pkeyNames = oiTo.getIdProperties();

		if (OAString.isEmpty(linkToPropertyName)) {
			if (OAString.isNotEmpty(pkeyNames) && pkeyNames.length == 1) {
				pos = 0;
				linkToPropertyName = pkeyNames[pos];
			} else {
				throw new RuntimeException("linkToPropertyName can not be null, since that are " + pkeyNames.length + " pk properties");
			}
		} else {
			int x = 0;
			for (String s : pkeyNames) {
				if (linkToPropertyName.equalsIgnoreCase(s)) {
					pos = x;
					break;
				}
				x++;
			}
			if (pos < 0) {
				throw new RuntimeException("linkToPropertyName does not exist in link object=" + linkName);
			}
		}

		Object obj = oa.internal().objects().property().getProperty(this, linkName);

		if (obj instanceof OAObject) {
			obj = ((OAObject) obj).getObjectKey();
		}

		Object result = null;
		if (obj instanceof OAObjectKey) {
			OAObjectKey ok = (OAObjectKey) obj;
			result = ok.getObjectIds()[pos];
		}

		/* 20230128 changed to use wrappers and not primitives
		if (result == null) {
			OAPropertyInfo pi = oiTo.getPropertyInfo(linkToPropertyName);
			if (pi.getIsPrimitive()) {
				result = OAReflect.getEmptyPrimitive(pi.getClassType());
			}
		}
		*/
		return result;
	}

	/**
	 * Requests that this object's data be refreshed from its data source.
	 * <p>
	 * This method delegates to {@link OAObjectReflectDelegate#refresh(OAObject)},
	 * which performs the actual reload or synchronization logic. This call
	 * typically causes the object's properties and links to be re-evaluated or
	 * reloaded based on the underlying data source or cache state.
	 */
	public void refresh() {
		if (isNew()) return;

		final OA oa = OARuntime.oa(this);
		OASyncClient sc = oa.internal().sync().getClient();
		if (sc != null) {
			oa.internal().sync().callRemoteClientRefresh(getClass(), getObjectKey());
			return;
		}
		
		OADataSource ds = OARuntime.datasource().get(getClass());
		if (ds == null) {
			return;
		}

		OAObjectInfo oi = oa.internal().objects().info().getOAObjectInfo(this.getClass());
		Object objx = ds.getObject(oi, getClass(), getObjectKey(), true);

		if (objx == null) {
			this.setDeleted(true);
		} else {
			if (this.getDeleted()) {
				this.setDeleted(false);
			}
		}
	}

	/**
	 * Requests that the specified property or link be refreshed from the data source.
	 * <p>
	 * This method delegates to
	 * {@link OAObjectReflectDelegate#refresh(OAObject, String)}, which performs
	 * the actual reload of the named property or link based on the underlying
	 * data source or cache implementation.
	 *
	 * @param propName the name of the property or link to refresh
	 */
	public void refresh(String linkPropertyName) {

		final OA oa = OARuntime.oa(this);
		OASyncClient sc = oa.internal().sync().getClient();
		if (sc != null) {
			oa.internal().sync().callRemoteClientRefresh(getClass(), getObjectKey(), linkPropertyName);
			return;
		}

		OAObjectInfo oi =  oa.internal().objects().info().getOAObjectInfo(this.getClass());
		OALinkInfo li = oi.getLinkInfo(linkPropertyName);
		if (li == null) {
			return;
		}

		Object objx = oa.internal().objects().property().getProperty(this, linkPropertyName);
		if (li.getType() == li.TYPE_ONE) {
			if (objx instanceof OAObject) {
				((OAObject) objx).refresh();
			}
			return;
		}

		if (!(objx instanceof Hub)) {
			return;
		}
		Hub hub = (Hub) objx;

		OASelect sel = hub.getSelect();
		if (sel != null) {
			hub.refresh();
			return;
		}

		OADataSource ds = OARuntime.datasource().get(li.getToClass());
		if (ds == null) {
			return;
		}

		OADataSourceIterator dsi = ds.select(li.getToClass(), this, linkPropertyName, li.getSortProperty(), true);
		if (dsi == null) {
			return;
		}
		List<OAObject> alNew = new ArrayList();
		for (; dsi.hasNext();) {
			objx = dsi.next();
			alNew.add((OAObject) objx);
			if (!hub.contains(objx)) {
				hub.add((OAObject) objx);
			}
		}

		List alRemove = new ArrayList();
		for (Object obj : hub) {
			if (!alNew.contains(obj)) {
				alRemove.add(obj);
			}
		}
		for (Object obj : alRemove) {
			hub.remove(obj);
		}
		int i = 0;
		for (Object obj : alNew) {
			int pos = hub.getPos(obj);
			if (i != pos) {
				hub.move(pos, i);
			}
			i++;
		}
	}

	/**
	 * Returns the enumeration name/value pairs using VEnum, that is defined for the specified property.
	 * <p>
	 * This method delegates to {@link OAObjectEnumDelegate#getNameValues(Class, String)},
	 * which locates any {@code @OAEnum} metadata declared for the property and
	 * constructs a {@link Hub} containing the enumeration values in their defined order.
	 * <p>
	 * The returned Hub is shared across all instances of this class and should be
	 * treated as read-only.
	 *
	 * @param propertyName the name of the property whose enumeration values are requested
	 * @return a Hub of enumeration values, or {@code null} if the property has no enum metadata
	 */
	public Hub<VEnum> getNameValues(String propertyName) {
		OA oa = OARuntime.oa(this);
		return oa.internal().objects().enumx().getVEnums(this.getClass(), propertyName);
	}

	
	/**
	 * Internal friend-access bridge used by OA runtime services.
	 * <p>
	 * These methods expose selected OAObject internals to package and service code
	 * without making those fields part of the public application API.
	 */
	public static final class FriendAccess {
		private FriendAccess() {
		}
		
	    /**
	     * Returns the runtime GUID for an object.
	     *
	     * @param obj the object
	     * @return the object GUID
	     */
	    public UUID getGuid(OAObject obj) {
	        return obj.guid;
	    }

	    /**
	     * Assigns the runtime GUID for an object.
	     *
	     * @param obj the object
	     * @param guid the GUID to assign
	     */
	    public void setGuid(OAObject obj, UUID guid) {
	    	if (guid != null) {
	    		if (obj.guid != null) throw new RuntimeException("OAObject guid can not be changed once it's been assigned.");
	    	}
	    	else {
	    		if (obj.weakhubs != null && obj.weakhubs.length > 0) throw new RuntimeException("OAObject guid can't be set to null if it's in Hub.");
	    	}
	    	obj.guid = guid;
	    }

	    /**
	     * Returns the new-object flag.
	     *
	     * @param obj the object
	     * @return {@code true} if the object is new
	     */
	    public boolean isNew(OAObject obj) {
	        return obj.newFlag;
	    }
	    /**
	     * Returns the raw new-object flag.
	     *
	     * @param obj the object
	     * @return the new-object flag
	     */
	    public boolean getNewFlag(OAObject obj) {
	        return obj.newFlag;
	    }

	    /**
	     * Returns the raw deleted-object flag.
	     *
	     * @param obj the object
	     * @return the deleted-object flag
	     */
	    public boolean getDeleteFlag(OAObject obj) {
	        return obj.deletedFlag;
	    }
	    /**
	     * Sets the raw deleted-object flag.
	     *
	     * @param obj the object
	     * @param b the deleted flag value
	     */
	    public void setDeletedFlag(OAObject obj, boolean b) {
	        obj.deletedFlag = b;
	    }
	    
	    /**
	     * Sets the raw new-object flag.
	     *
	     * @param obj the object
	     * @param b the new flag value
	     */
	    public void setNew(OAObject obj, boolean b) {
	        obj.newFlag = b;
	    }

	    /**
	     * Returns the primitive-null tracking bytes.
	     *
	     * @param obj the object
	     * @return primitive-null tracking bytes
	     */
	    public byte[] getNulls(OAObject obj) {
	    	return obj.nulls;
	    }

	    /**
	     * Sets the primitive-null tracking bytes.
	     *
	     * @param obj the object
	     * @param bs primitive-null tracking bytes
	     */
	    public void setNulls(OAObject obj, byte[] bs) {
	    	obj.nulls = bs;
	    }


		/**
		 * Returns the raw changed-object flag.
		 *
		 * @param obj the object
		 * @return the changed-object flag
		 */
		public boolean getChangedFlag(OAObject obj) {
			return obj.changedFlag;
		}
		/**
		 * Sets the raw changed-object flag.
		 *
		 * @param obj the object
		 * @param b the changed flag value
		 */
		public void setChangedFlag(OAObject obj, boolean b) {
			obj.changedFlag = b;;
		}

		/**
		 * Returns the weak Hub references that currently include this object.
		 *
		 * @param obj the object
		 * @return weak Hub references
		 */
		public WeakReference<Hub<?>>[] getWeakHubs(OAObject obj) {
			return obj.weakhubs;
		}
		/**
		 * Sets the weak Hub references for this object.
		 *
		 * @param obj the object
		 * @param refs weak Hub references
		 */
		public void setWeakHubs(OAObject obj, WeakReference<Hub<?>>[] refs) {
			obj.weakhubs = refs;
		}
		
		/**
		 * Returns the internal property storage array.
		 *
		 * @param obj the object
		 * @return internal property values
		 */
		public Object[] getProperties(OAObject obj) {
			return obj.properties;
		}
		/**
		 * Sets the internal property storage array.
		 *
		 * @param obj the object
		 * @param props internal property values
		 */
		public void setProperties(OAObject obj, Object[] props) {
			obj.properties = props;
		}

		/**
		 * Fires an object property-change event.
		 *
		 * @param oaObj the object
		 * @param propertyName the property name
		 * @param oldObj the previous value
		 * @param newObj the new value
		 */
		public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj) {		
			oaObj.firePropertyChange(propertyName, oldObj, newObj);
		}
	}
	
	private final static FriendAccess friendAccess = new FriendAccess(); 
	static FriendAccess getFriendAccess() {
		return friendAccess;
	}

	/**
	 * Returns the OA runtime that owns this object.
	 *
	 * @return the owning OA runtime
	 */
	public OA getOA() {
		return OARuntime.oa(this);
	}
}

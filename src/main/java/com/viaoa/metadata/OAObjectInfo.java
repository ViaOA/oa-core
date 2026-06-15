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
package com.viaoa.metadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.compare.OACompare;
import com.viaoa.datasource.OADataSource;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.HubEvent;
import com.viaoa.lang.OAArray;
import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.performance.OAPerformance;
import com.viaoa.metadata.pojo.OAObjectPojoLoader;
import com.viaoa.metadata.pojo.Pojo;
import com.viaoa.runtime.OARemoteThreadService;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

/*qqqqqqqqqqqqqqq
CODEX

1. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1902 _runOnChange2

  concrete bug: Exceptions thrown by the trigger listener during reverse-finder dispatch are caught as if the reverse
  finder failed.

  runtime scenario: A trigger has a usable reverse path. finder.find(fromObject) reaches onFound, invokes
  ti.trigger.getTriggerListener().onTrigger(...), and the listener throws. The catch at lines 1904-1907 catches that
  listener failure, sets ti.bNoReverseFinder = true, and recursively retries through the no-reverse fallback path.

  why this violates OA/OG trigger semantics: Trigger execution failure is not the same as path traversal failure. This
  can duplicate trigger side effects, hide the original failure if the fallback path succeeds or swallows, and
  permanently changes future dispatch behavior for that trigger.

  minimal fix direction: Only fall back when the failure is known to be reverse-path/data traversal related. Listener
  invocation failures from onFound should propagate without setting bNoReverseFinder.

  suggested CODEX comment location: At the catch block around line 1904.


3. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1434 _addTrigger

  concrete bug: Trigger registration dedupes by listener identity and propPath, ignoring the trigger instance and
  execution flags.

  runtime scenario: OA code registers two triggers using the same OATriggerListener and same property path but
  different trigger options, such as server-side-only, background execution, or loaded-data behavior. _addTrigger
  returns the existing TriggerInfo at lines 1445-1449, so the second trigger is never committed.

  why this violates OA/OG trigger semantics: A committed trigger registration should become visible with its own
  execution contract. This path silently collapses distinct trigger registrations and can miss expected execution mode
  or unregister behavior.

  minimal fix direction: Either dedupe only exact same trigger instance, or include all execution-affecting trigger
  options in the dedupe key. If listener/path idempotency is intended, document it as a hard registration contract.

  suggested CODEX comment location: Above the dedupe loop at lines 1445-1449.



2. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1698 onChange / _onChange2

  concrete bug: Recursive trigger protection does not cover the actual execution of async triggers.

  runtime scenario: A trigger is configured to run in the background. The original event increments recursive trigger
  count, schedules the work, then decrements before the trigger body runs. If the background trigger changes the same
  property again, each new event schedules a fresh async task with a reset recursion count, so a self-triggering loop
  can enqueue indefinitely instead of hitting the recursion guard.

  why this violates OA/OG trigger semantics: Recursive/reentrant protection should suppress invalid trigger recursion
  without depending on whether the trigger runs inline or async. This can cause duplicate/infinite trigger execution
  and unbounded queued work.

  minimal fix direction: Carry trigger recursion/depth state into TriggerRunnable, or maintain a trigger-chain guard
  around actual async execution rather than only around scheduling.

  suggested CODEX comment location: Around src/main/java/com/viaoa/metadata/OAObjectInfo.java:1812, where async
  trigger work is scheduled.


 1. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1436 createTrigger

  concrete bug: Multi-path trigger registration is partially committed before all paths are validated.

  runtime scenario: A trigger is created with multiple property paths, for example ["valid.path", "bad.path"].
  createTrigger registers TriggerInfo entries for the valid path as it loops. When the later path fails during new
  OAPath(...) or later metadata traversal, the exception is visible to the caller, but the earlier path remains
  registered and can fire.

  why this violates OA/OG trigger semantics: Trigger registration must become visible only when fully committed. A
  failed registration should not leave a partially active trigger that the caller believes failed to add.

  minimal fix direction: Pre-validate all property paths and calculated dependent paths before mutating hmTriggerInfo,
  or collect planned registrations first and commit them only after validation succeeds. Alternatively, roll back
  already-added registrations in a catch block.

  suggested CODEX comment location: Around src/main/java/com/viaoa/metadata/OAObjectInfo.java:1436, before the
  property-path registration loop.


2. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1499 _addTrigger

  concrete bug: Trigger add/dedupe is not atomic under concurrent registration.

  runtime scenario: Two runtime threads register the same trigger/listener/path at the same time, such as dynamic
  cache filters or Hub filters being configured concurrently. Both threads can observe the same CopyOnWriteArrayList
  before either has added its TriggerInfo, both pass the dedupe loop, and both add equivalent trigger entries.

  why this violates OA/OG trigger semantics: A single committed trigger registration can become duplicated, causing
  duplicate trigger execution for the same object/property event. This is a real runtime issue because triggers are
  not only static metadata; cache/hub filter paths add and replace triggers dynamically.

  minimal fix direction: Synchronize registration per OAObjectInfo/listen-property, or use an atomic compute/update
  block that performs dedupe and add together.

  suggested CODEX comment location: Around src/main/java/com/viaoa/metadata/OAObjectInfo.java:1505, where the list is
  fetched and dedupe begins.
  

1. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1581 _addTrigger

  concrete bug: Calculated-property dependency propagation is owned by the first trigger registered for that
  calculated property.

  runtime scenario: Trigger A is registered for calculated property fullName, so _addTrigger creates a dependent
  trigger for fullName’s dependent properties. Trigger B is later registered for the same calculated property; since
  the listen-property list already exists, no dependent trigger is created for B. If Trigger A is removed, its
  dependent trigger is removed at lines 1656-1664. Trigger B remains registered for fullName, but changes to
  firstName/lastName no longer synthesize the fullName trigger event, so B silently stops firing for dependency
  changes.

  why this violates OA/OG trigger semantics: Removing one trigger must not break another committed trigger’s
  dependency chain. Calculated-property trigger expansion should remain active as long as any trigger depends on that
  calculated property.

  minimal fix direction: Manage calculated-property dependency triggers per calculated property/listen-property with
  reference counting or shared ownership, not as a child of whichever trigger registered first. Remove the dependent
  trigger only when the last trigger depending on that calculated property is removed.

  suggested CODEX comment location: Around src/main/java/com/viaoa/metadata/OAObjectInfo.java:1581, where calculated
  dependent triggers are created only when the property list is first created.
  
2. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1598 _addTrigger

  concrete bug: Shared calculated-property dependency triggers inherit execution flags from the first trigger
  registered.

  runtime scenario: Trigger A for calculated property fullName is registered with serverSideOnly=true or
  onlyUseLoadedData=true. Trigger B later registers for the same calculated property with different execution needs.
  Since no new dependent trigger is created for B, changes to the underlying dependencies are governed by Trigger A’s
  child trigger flags.

  why this violates OA/OG trigger semantics: Trigger execution flags are part of the trigger contract. A later trigger
  should not miss dependency-driven execution because the first trigger on the same calculated property had narrower
  flags.

  minimal fix direction: Either create separate dependent triggers per parent trigger, or make the shared calculated-
  property dependency trigger use the union/broadest required execution behavior and dispatch per-trigger flags at the
  final listener stage.

  suggested CODEX comment location: Around src/main/java/com/viaoa/metadata/OAObjectInfo.java:1598, where trigger2
  copies flags from the first trigger.


1. file/class/method: src/main/java/com/viaoa/metadata/OAObjectInfo.java:1953 _runOnChange2

  concrete bug: A chained trigger with fromObject == null can silently miss downstream triggers that have a reverse
  path.

  runtime scenario: A calculated property trigger is created for a dependent path that cannot reverse back to the
  calculated-property owner. That child trigger correctly calls its listener with obj == null, and the listener calls
  onChange(null, listenProperty, hubEvent) for the calculated property. If another trigger is listening through a path
  such as orders.totalCalc, its TriggerInfo has a non-empty ppToRootClass. _runOnChange2 then tries
  finder.find(fromObject) with fromObject == null; OAFinder.find(null) returns null without calling onDataNotFound, so
  the listener is never invoked through the no-root fallback.

  why this violates OA/OG trigger semantics: If OA cannot resolve the root object for a trigger event, it should use
  the no-root trigger path so listeners can scan/select affected roots. Here the no-root state is lost when dispatch
  chains through a calculated property, causing silent missed trigger execution.

  minimal fix direction: In _runOnChange2, if fromObject == null, skip reverse-finder use and call
  ti.trigger.getTriggerListener().onTrigger(null, hubEvent, ti.ppFromRootClass) directly. Also use the trigger/root
  class graph rather than OARuntime.graph(fromObject) for role checks when fromObject is null.

  suggested CODEX comment location: Around src/main/java/com/viaoa/metadata/OAObjectInfo.java:1953, before the
  reverse-finder branches.

*/


/**
 * Metadata definition for an OAObject type. OAObjectInfo describes the full
 * structural blueprint of a domain class including its persistent properties,
 * business key properties, calculated properties, link relationships, and
 * lifecycle behaviors.
 *
 * <p>This metadata is generated by OABuilder from the application model
 * and drives the entire OA runtime: UI binding, relationship navigation,
 * persistence mapping, validation, serialization, and distributed
 * synchronization.</p>
 *
 * <p>OAObjectInfo enables the OA framework to interpret an OAObject instance
 * without static code knowledge. It defines:</p>
 *
 * <ul>
 *   <li>Property and method access metadata</li>
 *   <li>Primary / unique key properties</li>
 *   <li>Link relationships and their reverse mapping</li>
 *   <li>Validation and trigger hooks</li>
 *   <li>Editable and display rules</li>
 *   <li>Datasource mapping and schema alignment</li>
 * </ul>
 *
 * <p>It provides the schema that allows OAObjects to form a
 * dynamic, model-driven Object Graph with behavior determined by metadata
 * rather than hard-coded assumptions.</p>
 *
 * @see OAObject
 * @see OAObjectInfoDelegate
 * @see OALinkInfo
 */
public class OAObjectInfo { //implements java.io.Serializable {
	private static Logger LOG = Logger.getLogger(OAObjectInfo.class.getName());
	static final long serialVersionUID = 1L;
	
	/**
	 * Shared synchronization lock used for operations involving
	 * volatile metadata updates that must be thread-safe.
	 */
	static final Object vlock = new Object();

	/**
	 * The Java class this metadata describes. Assigned during lookup
	 * through OAObjectInfoDelegate.
	 */
	protected Class thisClass; // the Class for this ObjectInfo.  Set when calling OAObjectDelegete.getOAObjectInfo

	/**
	 * Collection of link-relationship metadata entries for this type.
	 * Lazily initialized when first accessed.
	 */
	protected List<OALinkInfo> alLinkInfo;

	/**
	 * List containing calculation-property metadata. Created on demand.
	 */
	protected ArrayList<OACalcInfo> alCalcInfo;

	/**
	 * Set of names of calculated properties that operate on HUB data.
	 * Stored in uppercase for fast case-insensitive lookup.
	 */
	protected HashSet<String> hsHubCalcInfoName = new HashSet<String>();

	/**
	 * Names of identifier (business-key) properties for this type.
	 */
	protected String[] idProperties;

	/**
	 * List of primitive (non-reference) property metadata entries for
	 * this object type, built lazily.
	 */
	protected ArrayList<OAPropertyInfo> alPropertyInfo;

	/**
	 * List of method-metadata entries used for reflection-based access
	 * and behavior description.
	 */
	protected ArrayList<OAMethodInfo> alMethodInfo;

	/**
	 * Array of simple property names used for import-match resolution
	 * when matching POJO/JSON input to existing objects.
	 */
	protected String[] importMatchPropertyNames;

	/**
	 * Array of property paths (possibly multi-level) used as matching
	 * criteria during object import.
	 */
	protected String[] importMatchPropertyPaths;

	/**
	 * Flag indicating whether this type participates in datasource
	 * load/save operations.
	 */
	protected boolean bUseDataSource = true;
	
	/**
	 * When true, objects of this type are restricted to local use and
	 * are not transmitted to remote servers.
	 */
	protected boolean bLocalOnly = false; // dont send to OAServer

	/**
	 * Controls whether new or loaded objects of this type are added to
	 * the global OAObject cache.
	 */
	protected boolean bAddToCache = true; // add object to Cache

	/**
	 * Indicates whether newly created instances should have primitive
	 * properties automatically initialized by OAObject.
	 */
	protected boolean bInitializeNewObjects = true; // initialize object properties (used by OAObject)

	/**
	 * Model-defined name of this OAObject type.
	 */
	protected String name;

	/**
	 * Display-friendly name of the object type. Defaults to simple
	 * class name when unset.
	 */
	protected String displayName;

	/**
	 * Lower-cased version of the type name, typically used for
	 * generation of UI and XML identifiers.
	 */
	protected String lowerName;

	/**
	 * Plural form of the display name for use in UI and metadata-driven
	 * representations.
	 */
	protected String pluralName;

	/**
	 * Array of property paths used to determine which linked objects
	 * should be treated as “roots” when constructing a logical tree
	 * view of this OAObject type.  
	 *
	 * <p>Each entry is a full property path identifying the starting
	 * points for hierarchical traversal, enabling UI components or
	 * reporting tools to build structured, expandable trees based on
	 * the object graph.</p>
	 */
	protected String[] rootTreePropertyPaths;

	/**
	 * Array of primitive (non-reference) property names defined for this
	 * object type. These names correspond to properties tracked using
	 * OAObject’s null-bitmask mechanism and are assigned during metadata
	 * initialization by OAObjectInfoDelegate.
	 */
	protected String[] primitiveProps;

	// protected byte[] primitiveMask; // used to mask boolean to not default to null, instead false

	/**
	 * Array of hub-based property names defined for this type. These represent
	 * reference-hub properties that typically default to size zero.
	 */
	protected String[] hubProps;

	/**
	 * Cached flag indicating whether objects of this type support weak-reference
	 * behavior. Values: -1 = not yet evaluated, 0 = false, 1 = true.
	 */
	int weakReferenceable = -1; // flag set/used by OAObjectInfoDelegate.isWeakReferenceable -1=not checked, 0=false, 1=true


	/**
	 * Indicates whether recursive-link metadata has been evaluated for this type.
	 * Used to prevent redundant recursive-link resolution.
	 */
	protected volatile boolean bSetRecursive;

	/**
	 * Cached link-info references representing the ONE-side and MANY-side
	 * recursive link definitions for this type, assigned by the delegate.
	 */
	protected OALinkInfo liRecursiveOne, liRecursiveMany;

	/**
	 * Tracks whether the owning-link metadata for this type has been determined.
	 * Prevents repeated evaluations by the delegate.
	 */
	protected volatile boolean bSetLinkToOwner;

	/**
	 * Cached link-info representing the relationship in which this type is the
	 * owned side of an ownership link. Set by OAObjectInfoDelegate.
	 */
	protected OALinkInfo liLinkToOwner; // set by OAObjectInfoDelegate.getLinkToOwner

	/**
	 * Flag indicating that this metadata instance has completed initialization
	 * by OAObjectInfoDelegate. Prevents repeated initialization passes.
	 */
	protected boolean bProcessed;

	/**
	 * Flag indicating that this metadata instance has completed initialization
	 * by OAObjectInfoDelegate. Prevents repeated initialization passes.
	 */
	protected boolean bLookup;

	/**
	 * Reflected callback method assigned to this type for object-level events.
	 * May be null if no callback has been registered.
	 */
	private Method objectCallbackMethod;

	/**
	 * List of property names whose enabled/visible behavior depends on the
	 * current view context. Assigned by higher-level metadata configuration.
	 */
	private String[] viewDependentProperties;

	/**
	 * List of properties whose state or evaluation rules depend on a broader
	 * application context rather than UI view rules alone.
	 */
	private String[] contextDependentProperties;

	/**
	 * Name of the property used to determine whether this type should be
	 * considered enabled. May be null if no such rule is defined.
	 */
	private String enabledProperty;

	/**
	 * Static enabled-state value associated with this type when no enabled
	 * property is defined or when evaluated statically.
	 */
	private boolean enabledValue;

	/**
	 * Name of the property that controls the visibility of this type in UI or
	 * metadata-driven evaluations.
	 */
	private String visibleProperty;

	/**
	 * Static visibility value associated with this type when no dynamic
	 * visible-property rule is applied.
	 */
	private boolean visibleValue;

	/**
	 * Name of the property that determines whether this type is enabled within
	 * a specific context-dependent rule set.
	 */
	private String contextEnabledProperty;

	/**
	 * Static context-enabled value assigned to this type when context-dependent
	 * rules require a fixed boolean state.
	 */
	private boolean contextEnabledValue;

	/**
	 * Name of the property that controls visibility of this type under
	 * context-dependent evaluation rules.
	 */
	private String contextVisibleProperty;
	
	/**
	 * Static context-visible value associated with this type, used when
	 * visibility rules require a fixed boolean state rather than a
	 * property-driven evaluation.
	 */
	private boolean contextVisibleValue;
	
	/**
	 * Indicates whether this type defines exactly one link property, used
	 * for optimization and rule evaluation within the metadata layer.
	 */
	private boolean bHasOneAndOnlyOneLink;

	/**
	 * Name of the property that stores the soft-delete flag for this type.
	 * Used to determine logical deletion rather than physical removal.
	 */
	private String softDeleteProperty;
	
	/**
	 * Name of the property that stores the reason associated with a
	 * soft-delete operation.
	 */
	private String softDeleteReasonProperty;
	
	/**
	 * Name of the property used to store the version value for this type,
	 * typically supporting optimistic locking or version tracking.
	 */
	private String versionProperty;
	
	/**
	 * Name of the link property that associates this type with its
	 * corresponding version object.
	 */
	private String versionLinkProperty;
	
	/**
	 * Name of the property representing a time-series value for this type,
	 * supporting temporal or historical modeling.
	 */
	private String timeSeriesProperty;
    
	/**
	 * Name of the property indicating a freeze-state flag, which can be
	 * used to prevent modifications to this object instance.
	 */
	private String freezeProperty;

	/**
	 * Indicates whether this type is configured to behave as a singleton,
	 * allowing only one instance to exist within the object graph.
	 */
	private boolean singleton;
	
	/**
	 * Indicates whether this type should use a singleton Pojo instance for
	 * mapping and serialization purposes.
	 */
	private boolean pojoSingleton;
	
	/**
	 * Flag indicating whether Pojo usage is disabled for this type.
	 * When true, POJO mapping behavior is bypassed.
	 */
	private boolean noPojo;
	
	/**
	 * The Pojo instance mapped to this OAObject type. Created lazily using
	 * OAObjectPojoLoader upon first request.
	 */
	private Pojo pojo;
	
	/**
	 * Indicates whether JSON field names should begin with a capital letter
	 * when this type is serialized to or from JSON.
	 */
	private boolean bJsonUsesCapital; // JSON properties are titled (begin with capital letter)
	
	/**
	 * Indicates where the guid (UUID) from OAObject.guid is stored to datasource.
	 * Otherwise, it is generated  in the OAObject constructor.
	 */
	private boolean bGuidIsStored; 
	
	protected boolean bPreSelect;

	private volatile Map<String, OALinkInfo> hmLinkInfo;

	private volatile Map<String, OAPropertyInfo> hmPropertyInfo;
	
	private volatile Map<String, OAMethodInfo> hmMethodInfo;
	
	/**
	 * Default constructor that initializes the metadata instance with
	 * an empty identifier property list.
	 */
	public OAObjectInfo() {
		this(new String[] {});
	}

	/**
	 * Returns the Java class associated with this metadata definition.
	 *
	 * @return the OAObject class represented by this info.
	 */
	public Class getForClass() {
		return thisClass;
	}
	
	public void setForClass(Class c) {
		thisClass = c;
	}

	/**
	 * Initializes the metadata with a single identifier property.
	 *
	 * @param objectIdProperty the name of the ID property.
	 */
	public OAObjectInfo(String objectIdProperty) {
		this(new String[] { objectIdProperty });
	}

	/**
	 * Initializes the metadata with the supplied array of identifier
	 * property names.
	 *
	 * @param idProperties list of ID property names.
	 */
	public OAObjectInfo(String[] idProperties) {
		this.idProperties = idProperties;
	}

	/**
	 * Internal setter used to replace the identifier property list.
	 *
	 * @param ss the new identifier property names.
	 */
	void setPropertyIds(String[] ss) {
		this.idProperties = ss;
	}

	/**
	 * Returns the list of identifier property names. Ensures that a
	 * non-null array is always returned.
	 *
	 * @return array of ID property names.
	 */
	public String[] getIdProperties() {
		if (this.idProperties == null) {
			this.idProperties = new String[0];
		}
		return this.idProperties;
	}

	/**
	 * Returns the properties that form the object’s business key.
	 * This implementation is equivalent to {@link #getIdProperties()}.
	 *
	 * @return array of key property names.
	 */
	public String[] getKeyProperties() {
		return getIdProperties();
	}

	/**
	 * Determines whether the supplied property name is part of the
	 * business key. Delegates to {@link #isIdProperty(String)}.
	 *
	 * @param prop the property name to check.
	 * @return true if it is an ID property.
	 */
	public boolean isKeyProperty(String prop) {
		return isIdProperty(prop);
	}

	/**
	 * Checks whether the given property name matches one of the
	 * configured identifier properties, ignoring case.
	 *
	 * @param prop the property name to check.
	 * @return true if it is an identifier property.
	 */
	public boolean isIdProperty(String prop) {
		if (prop == null) {
			return false;
		}
		for (String s : getIdProperties()) {
			if (prop.equalsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this object type declares at least one
	 * import-match property used for object-matching during import.
	 *
	 * @return true if import match properties are defined.
	 */
	public boolean hasImportMatchProperties() {
		String[] ss = getImportMatchPropertyNames();
		return ss != null && getImportMatchPropertyNames().length > 0;
	}

	/**
	 * Returns the list of simple property names used for import-match
	 * processing. These identify fields used to match objects during
	 * JSON/POJO import.
	 *
	 * @return array of import-match property names, or null if none.
	 */
	public String[] getImportMatchPropertyNames() {
		return this.importMatchPropertyNames;
	}

	/**
	 * Returns the list of property paths (which may traverse links)
	 * used for import-match processing.
	 *
	 * @return array of import-match property paths, or null if none.
	 */
	public String[] getImportMatchPropertyPaths() {
		return this.importMatchPropertyPaths;
	}

	/**
	 * Returns the list of defined link relationships for this object
	 * type. The list is lazily initialized as a thread-safe
	 * CopyOnWriteArrayList that automatically resets cached lookup
	 * tables whenever modified.
	 *
	 * @return list of link metadata entries.
	 */
	public List<OALinkInfo> getLinkInfos() {
		if (alLinkInfo == null) {
			alLinkInfo = new CopyOnWriteArrayList<OALinkInfo>() {
				void reset() {
					bOwnedAndNoManyCheck = false;
					bOwnedByOneCheck = false;
					
					hmLinkInfo = null;
					ownedLinkInfos = null;
				}

				@Override
				public boolean add(OALinkInfo e) {
					reset();
					return super.add(e);
				}

				@Override
				public OALinkInfo remove(int index) {
					reset();
					return super.remove(index);
				}

				@Override
				public boolean removeAll(Collection<?> c) {
					reset();
					return super.removeAll(c);
				}

				@Override
				public boolean remove(Object o) {
					reset();
					return super.remove(o);
				}
			};
		}
		return alLinkInfo;
	}

	/**
	 * Adds a link definition to this object type. Delegates to
	 * {@link #addLinkInfo(OALinkInfo)}.
	 *
	 * @param li the link metadata to add.
	 */
	public void addLink(OALinkInfo li) {
		addLinkInfo(li);
	}

	/**
	 * Adds a link definition to this object type by inserting it into
	 * the link-info list. This also resets internal caches so that
	 * reverse-lookup tables are refreshed.
	 *
	 * @param li the link metadata to add.
	 */
	public void addLinkInfo(OALinkInfo li) {
		getLinkInfos().add(li);
	}

	/**
	 * Retrieves link metadata associated with the supplied property
	 * name. Performs a case-insensitive lookup using an internal
	 * cache, building the cache on first access.
	 *
	 * @param propertyName the link property name.
	 * @return the matching link info, or null if none exists.
	 */
	public OALinkInfo getLinkInfo(String propertyName) {
		if (propertyName == null) {
			return null;
		}
		Map<String, OALinkInfo> hm = hmLinkInfo;
		if (hm == null) {
			hm = new HashMap<String, OALinkInfo>();
			for (OALinkInfo li : getLinkInfos()) {
				String s = li.getName();
				if (s == null) {
					continue;
				}
				hm.put(s.toUpperCase(), li);
			}
			hmLinkInfo = hm;
		}
		return hm.get(propertyName.toUpperCase());
	}

	private volatile OALinkInfo[] ownedLinkInfos;

	/**
	 * Returns an array of link definitions where this object is the
	 * owner side of the relationship. The result is cached after the
	 * initial scan of link metadata.
	 *
	 * @return array of owned link metadata.
	 */
	public OALinkInfo[] getOwnedLinkInfos() {
		if (ownedLinkInfos == null) {
			int x = 0;
			for (OALinkInfo li : getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.bOwner) {
					x++;
				}
			}
			OALinkInfo[] temp = new OALinkInfo[x];
			int i = 0;
			for (OALinkInfo li : getLinkInfos()) {
				if (!li.getUsed()) {
					continue;
				}
				if (li.bOwner) {
					if (i == x) {
						return getOwnedLinkInfos();
					}
					temp[i++] = li;
				}
			}
			ownedLinkInfos = temp;
		}
		return ownedLinkInfos;
	}

	private volatile boolean bOwnedAndNoMany;
	private volatile boolean bOwnedAndNoManyCheck;

	/**
	 * Determines whether this type is owned by another object and has
	 * no reverse MANY-side link. Scans link definitions only on the
	 * first call and caches the result.
	 *
	 * @return true if owned and no reverse MANY links exist.
	 */
	public boolean isOwnedAndNoReverseMany() {
		if (bOwnedAndNoManyCheck) {
			return bOwnedAndNoMany;
		}
		for (OALinkInfo li : getLinkInfos()) {
			if (!li.getUsed()) {
				continue;
			}
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev == null) {
				continue;
			}
			if (!liRev.getUsed()) {
				continue;
			}
			if (liRev.type == OALinkInfo.MANY) {
				bOwnedAndNoMany = false;
				break;
			}
			if (li.type != OALinkInfo.ONE) {
				continue;
			}
			if (liRev.bOwner) {
				bOwnedAndNoMany = true;
			}
		}
		bOwnedAndNoManyCheck = true;
		return bOwnedAndNoMany;
	}

	private volatile boolean bOwnedByOneCheck;
	private OALinkInfo liOwnedByOne;

	/**
	 * Returns the ONE-side link info that indicates this type is
	 * owned by another object. Evaluated once and cached thereafter.
	 *
	 * @return the owning ONE link info, or null if none found.
	 */
	public OALinkInfo getOwnedByOne() {
		if (bOwnedByOneCheck) {
			return liOwnedByOne;
		}
		for (OALinkInfo li : getLinkInfos()) {
			if (!li.getUsed()) {
				continue;
			}
			if (li.type != OALinkInfo.ONE) {
				continue;
			}
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && liRev.bOwner) {
				if (!liRev.getUsed()) {
					continue;
				}
				liOwnedByOne = li;
				break;
			}
		}
		bOwnedByOneCheck = true;
		return liOwnedByOne;
	}

	/**
	 * Returns the list of calculated-property metadata entries,
	 * creating the list on first access.
	 *
	 * @return list of calculation info objects.
	 */
	public ArrayList<OACalcInfo> getCalcInfos() {
		if (alCalcInfo == null) {
			alCalcInfo = new ArrayList<OACalcInfo>(5);
		}
		return alCalcInfo;
	}

	/**
	 * Retrieves a calculated-property definition by name using
	 * case-insensitive comparison.
	 *
	 * @param s the calculated property name.
	 * @return the matching OACalcInfo, or null if not found.
	 */
	public OACalcInfo getCalcInfo(String s) {
		if (alCalcInfo == null) {
			return null;
		}
		for (OACalcInfo ci : alCalcInfo) {
			if (ci.name.equalsIgnoreCase(s)) {
				return ci;
			}
		}
		return null;
	}

	/**
	 * Adds a calculated-property metadata entry and updates the
	 * internal hub-calculation name set when the calculation is
	 * hub-based.
	 *
	 * @param ci the calculation metadata to add.
	 */
	public void addCalcInfo(OACalcInfo ci) {
		getCalcInfos().add(ci);
		if (ci.bIsForHub) {
			String s = ci.getName();
			if (OAStr.isNotEmpty(s)) {
				hsHubCalcInfoName.add(s.toUpperCase());
			}
		}
	}

	/**
	 * Determines whether the supplied name corresponds to a hub-based
	 * calculated property. Case-insensitive.
	 *
	 * @param name the property name.
	 * @return true if it represents a hub calculation.
	 */
	public boolean isHubCalcInfo(String name) {
		if (name == null) {
			return false;
		}
		return hsHubCalcInfoName.contains(name.toUpperCase());
	}

	/**
	 * Returns the list of primitive (non-reference) property metadata,
	 * initializing the list on first access.
	 *
	 * @return list of OAPropertyInfo entries.
	 */
	public ArrayList<OAPropertyInfo> getPropertyInfos() {
		if (alPropertyInfo == null) {
			alPropertyInfo = new ArrayList(5);
		}
		return alPropertyInfo;
	}

	/**
	 * Adds a primitive property metadata entry and resets cached
	 * lookup state so that dependent computations are refreshed.
	 *
	 * @param pi the property metadata to add.
	 */
	public void addPropertyInfo(OAPropertyInfo pi) {
		if (pi == null) {
			return;
		}
		getPropertyInfos().add(pi);
		resetPropertyInfo();
	}

	/**
	 * Clears cached property-lookup values so they will be recalculated
	 * when next requested. Used after property metadata is modified.
	 */
	protected void resetPropertyInfo() {
		hmPropertyInfo = null;
		bCheckTimestamp = false;
		bCheckSubmit = false;
		bCheckHasBlobProperty = false;
	}

	/**
	 * Blobs are set up as transient, OAObjectSerializer needs to know if/when to include them.
	 */
	private volatile boolean bHasBlobProperty;
	private volatile boolean bCheckHasBlobProperty;

	/**
	 * Returns true if this type defines at least one blob property.
	 * Scans once and caches the result for subsequent calls.
	 *
	 * @return true if the type has a blob property.
	 */
	public boolean getHasBlobProperty() {
		if (bCheckHasBlobProperty) {
			return bHasBlobProperty;
		}
		for (OAPropertyInfo pi : getPropertyInfos()) {
			if (pi.isBlob()) {
				bHasBlobProperty = true;
				break;
			}
		}
		bCheckHasBlobProperty = true;
		return bHasBlobProperty;
	}

	/**
	 * Deprecated spelling-compatible wrapper that returns the same
	 * value as {@link #getHasBlobProperty()}.
	 *
	 * @return true if the type has a blob property.
	 */
	public boolean getHasBlobPropery() {
		return getHasBlobProperty();
	}

	/**
	 * Retrieves primitive property metadata by name. Performs a
	 * case-insensitive lookup and builds an internal cache on demand.
	 *
	 * @param propertyName the property name.
	 * @return the matching OAPropertyInfo, or null if not found.
	 */
	public OAPropertyInfo getPropertyInfo(String propertyName) {
		if (propertyName == null) {
			return null;
		}
		Map<String, OAPropertyInfo> hm = hmPropertyInfo;
		if (hm == null) {
			hm = new HashMap<String, OAPropertyInfo>();
			for (OAPropertyInfo pi : getPropertyInfos()) {
				String s = pi.getName();
				if (s == null) {
					continue;
				}
				hm.put(s.toUpperCase(), pi);
			}
			hmPropertyInfo = hm;
		}
		return hm.get(propertyName.toUpperCase());
	}

	/**
	 * Returns the list of method metadata entries, creating the list
	 * on first access.
	 *
	 * @return list of OAMethodInfo entries.
	 */
	public ArrayList<OAMethodInfo> getMethodInfos() {
		if (alMethodInfo == null) {
			alMethodInfo = new ArrayList(5);
		}
		return alMethodInfo;
	}

	/**
	 * Adds a method metadata entry to this type and clears the cached
	 * method-lookup map so that it will be rebuilt on next access.
	 *
	 * @param mi the method metadata to add.
	 */
	public void addMethod(OAMethodInfo mi) {
		getMethodInfos().add(mi);
		hmMethodInfo = null;
	}

	/**
	 * Adds a method metadata entry. Functionally identical to
	 * {@link #addMethod(OAMethodInfo)}, and also clears the cached
	 * method-lookup map.
	 *
	 * @param mi the method metadata to add.
	 */
	public void addMethodInfo(OAMethodInfo mi) {
		getMethodInfos().add(mi);
		hmMethodInfo = null;
	}

	/**
	 * Retrieves method metadata by method name. Uses a case-insensitive
	 * lookup and lazily initializes an internal name → metadata map.
	 *
	 * @param name the method name.
	 * @return matching OAMethodInfo, or null if not found.
	 */
	public OAMethodInfo getMethodInfo(String name) {
		if (name == null) {
			return null;
		}
		Map<String, OAMethodInfo> hm = hmMethodInfo;
		if (hm == null) {
			hm = new HashMap<String, OAMethodInfo>();
			for (OAMethodInfo mi : getMethodInfos()) {
				String s = mi.getName();
				if (s == null) {
					continue;
				}
				hm.put(s.toUpperCase(), mi);
			}
			hmMethodInfo = hm;
		}
		return hm.get(name.toUpperCase());
	}

	private HashMap<String, Method> hmObjectCallbackMethod;

	/**
	 * Looks up an object-callback method by name. Returns null if no
	 * callback map exists or if the name is null.
	 *
	 * @param name the callback method name.
	 * @return the reflected Method, or null.
	 */
	public Method getObjectCallbackMethod(String name) {
		if (hmObjectCallbackMethod == null) {
			return null;
		}
		if (name == null) {
			return null;
		}
		return hmObjectCallbackMethod.get(name.toUpperCase());
	}

	/**
	 * Registers a reflected method as an object-callback method
	 * associated with the supplied name. Initializes the internal
	 * map on first use.
	 *
	 * @param name the lookup name.
	 * @param m    the callback method.
	 */
	public void addObjectCallbackMethod(String name, Method m) {
		if (name == null || m == null) {
			return;
		}
		if (hmObjectCallbackMethod == null) {
			hmObjectCallbackMethod = new HashMap<>();
		}
		hmObjectCallbackMethod.put(name.toUpperCase(), m);
	}

	/**
	 * Returns the list of primitive property names used for the
	 * OAObject null-bitmask mechanism. The list is assigned during
	 * OAObjectInfoDelegate initialization.
	 *
	 * @return array of primitive property names.
	 */
	public String[] getPrimitiveProperties() {
		return primitiveProps;
	}

	public void setPrimitiveProperties(String[] pps) {
		this.primitiveProps = pps;
	}
	
	// 20180325  20180403 removed, not used
	/**
	 * used to set which primitive properties should be set to null for new instances. boolean props will not be set to null.
	 *
	 * @return / public byte[] getPrimitiveMask() { if (primitiveMask != null) return primitiveMask; String[] ps = getPrimitiveProperties();
	 *         int x = (ps==null) ? 0 : ((int) Math.ceil(ps.length / 8.0d)); primitiveMask = new byte[x]; for (int i=0; i<x; i++) {
	 *         primitiveMask[i] = ((byte) 0xFF); } int pos = -1; // bit pos for (String prop : ps) { pos++; OAPropertyInfo pi =
	 *         getPropertyInfo(prop); if (pi == null) continue; //if (!pi.isNameValue()) { Class c = pi.getClassType(); if
	 *         (!c.equals(boolean.class)) continue; //} int posByte = (pos / 8); int posBit = 7 - (pos % 8); byte b = (byte) 0; b |= ((byte)
	 *         1) << posBit; primitiveMask[posByte] ^= b; } return primitiveMask; }
	 */

	/**
	 * Returns the list of hub-based property names defined for this
	 * type. These represent hub references with size zero defaults.
	 *
	 * @return array of hub property names.
	 */
	public String[] getHubProperties() {
		return hubProps;
	}

	/**
	 * Sets whether this type supports backing by a datasource.
	 *
	 * @param b true to enable datasource usage.
	 */
	public void setUseDataSource(boolean b) {
		bUseDataSource = b;
	}

	/**
	 * Returns whether this type supports loading from or saving to a
	 * datasource.
	 *
	 * @return true if datasource usage is enabled.
	 */
	public boolean getUseDataSource() {
		return bUseDataSource;
	}

	/**
	 * Specifies whether objects of this type should be restricted to
	 * local use, preventing transmission to remote servers.
	 *
	 * @param b true to restrict to local-only behavior.
	 */
	public void setLocalOnly(boolean b) {
		bLocalOnly = b;
	}

	/**
	 * Indicates whether this type is marked as local-only and should
	 * not be transmitted to remote servers.
	 *
	 * @return true if local-only is enabled.
	 */
	public boolean getLocalOnly() {
		return bLocalOnly;
	}

	/**
	 * Specifies whether instances of this type should be added to the
	 * global OAObjectCache during creation or loading.
	 *
	 * @param b true to enable caching behavior.
	 */
	public void setAddToCache(boolean b) {
		bAddToCache = b;
	}

	/**
	 * Returns whether instances of this type are automatically added
	 * to the object cache.
	 *
	 * @return true if caching is enabled.
	 */
	public boolean getAddToCache() {
		return bAddToCache;
	}

	/**
	 * Enables or disables automatic initialization of primitive
	 * properties for new instances of this type.
	 *
	 * @param b true to initialize new objects.
	 */
	public void setInitializeNewObjects(boolean b) {
		bInitializeNewObjects = b;
	}

	/**
	 * Returns whether new objects should have primitive property
	 * defaults initialized automatically.
	 *
	 * @return true if initialization is enabled.
	 */
	public boolean getInitializeNewObjects() {
		return bInitializeNewObjects;
	}

	/**
	 * Returns the model-defined name for this object type.
	 *
	 * @return the object name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the model-defined name for this object type.
	 *
	 * @param s the name to assign.
	 */
	public void setName(String s) {
		this.name = s;
	}

	/**
	 * Returns the display name for this object type. If not explicitly
	 * set, defaults to the simple class name.
	 *
	 * @return display-friendly name.
	 */
	public String getDisplayName() {
		if (displayName == null && thisClass != null) {
			displayName = thisClass.getSimpleName();
		}
		return displayName;
	}

	/**
	 * Sets the display name for this object type.
	 *
	 * @param s the name to display.
	 */
	public void setDisplayName(String s) {
		this.displayName = s;
	}

	/**
	 * Returns the pluralized form of the display name. If not already
	 * set, it is computed from the class name.
	 *
	 * @return plural name.
	 */
	public String getPluralName() {
		if (pluralName == null && thisClass != null) {
			pluralName = OAString.getPlural(thisClass.getSimpleName());
		}
		return pluralName;
	}

	/**
	 * Assigns the plural display name for this object type.
	 *
	 * @param s the plural name to set.
	 */
	public void setPluralName(String s) {
		this.pluralName = s;
	}

	/**
	 * Returns a lowercase-first version of the class name. Generated
	 * lazily if not explicitly set.
	 *
	 * @return lowercased name.
	 */
	public String getLowerName() {
		if (lowerName == null && thisClass != null) {
			lowerName = OAString.makeFirstCharLower(thisClass.getName());
		}
		return lowerName;
	}

	/**
	 * Sets the lowercase name for this type.
	 *
	 * @param s the name to assign.
	 */
	public void setLowerName(String s) {
		this.lowerName = s;
	}

	/**
	 * Returns the list of root-tree property paths used to construct
	 * hierarchical tree views for this type.
	 *
	 * @return array of root tree property paths.
	 */
	public String[] getRootTreePropertyPaths() {
		return rootTreePropertyPaths;
	}

	/**
	 * Assigns the list of root-tree property paths that define how
	 * instances of this type participate in hierarchical trees.
	 *
	 * @param paths property paths to assign.
	 */
	public void setRootTreePropertyPaths(String[] paths) {
		this.rootTreePropertyPaths = paths;
	}

	/**
	 * Marks the specified property as required by updating the
	 * corresponding OAPropertyInfo entry.
	 *
	 * @param prop the property name to mark required.
	 */
	public void addRequired(String prop) {
		ArrayList al = getPropertyInfos();
		for (int i = 0; i < al.size(); i++) {
			OAPropertyInfo pi = (OAPropertyInfo) al.get(i);
			if (pi.getName().equalsIgnoreCase(prop)) {
				pi.setRequired(true);
			}
		}
	}

	/**
	 * Retrieves the recursively-defined link info for the given type.
	 * Delegates to OAObjectInfoDelegate for evaluation.
	 *
	 * @param type link type constant.
	 * @return recursive link definition, or null.
	 */
	public OALinkInfo getRecursiveLinkInfo(int type) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(thisClass);
		return og.objectsInternal().callObjectInfoGetRecursiveLinkInfo(this, type);
	}


	/**
	 * Determines whether objects of this type support persistent
	 * storage. Evaluates only when needed and caches the result.
	 *
	 * @return true if storage is supported.
	 */
	public boolean getSupportsStorage() {
		OADataSource ds = OARuntime.datasource().get(thisClass);
		return ds != null && ds.supportsStorage();
	}

	/**
	 * June 2016 triggers when a property/hub is changed.
	 */
	protected static class TriggerInfo {
		OATrigger trigger;
		String ppFromRootClass;;
		String ppToRootClass;; // reverse propPath from thisClass to root Class
		String listenProperty; // property/hub to listen to.
		boolean bNoReverseFinder;
		boolean bReverseHasMany;
	}

	/**
	 * Map of trigger listeners keyed by the property name they monitor.
	 * Each key maps to a thread-safe list of TriggerInfo entries.
	 */
	protected ConcurrentHashMap<String, CopyOnWriteArrayList<TriggerInfo>> hmTriggerInfo = new ConcurrentHashMap<String, CopyOnWriteArrayList<TriggerInfo>>();

	/**
	 * Counter tracking the number of triggers created for this type.
	 */
	private final AtomicInteger aiTrigger = new AtomicInteger();
	
	/**
	 * Counter tracking the number of triggers that require execution
	 * within a background thread.
	 */
	private final AtomicInteger aiTriggerBackgroundThread = new AtomicInteger();

	/**
	 * Global counter shared across all OAObjectInfo instances that tracks
	 * the total number of triggers registered system-wide.
	 */
	private final static AtomicInteger aiAllTrigger = new AtomicInteger();

	/**
	 * Returns the total number of triggers registered across all
	 * object types. Uses a shared global counter.
	 *
	 * @return number of triggers created.
	 */
	public static int getTotalTriggers() {
		return aiAllTrigger.get();
	}

	/**
	 * Returns the list of property names for which triggers have been
	 * registered on this object type.
	 *
	 * @return list of trigger property names.
	 */
	public ArrayList<String> getTriggerPropertNames() {
		ArrayList<String> al = new ArrayList<String>();
		for (String s : hmTriggerInfo.keySet()) {
			al.add(s);
		}
		return al;
	}

	/**
	 * Creates and registers trigger metadata for each property path
	 * defined on the supplied trigger. Walks the property path,
	 * determines forward and reverse link traversal rules, and adds
	 * TriggerInfo entries accordingly.
	 *
	 * @param trigger the trigger definition to register.
	 * @param bSkipFirstNonManyProperty whether to skip listening on
	 *        the first non-many property in the path.
	 */
	public void createTrigger(final OATrigger trigger, final boolean bSkipFirstNonManyProperty) {
		if (trigger == null) {
			return;
		}

		if (trigger.getPropertyPaths() == null) {
			return;
		}

		String s = "";
		if (trigger.getPropertyPaths() != null) {
			for (String triggerPropPath : trigger.getPropertyPaths()) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += triggerPropPath;
			}
		}
		s = (thisClass.getSimpleName() + ", name=" + trigger.getName() + ", propPaths=[" + s + "], skipFirst=" + bSkipFirstNonManyProperty);
		LOG.fine(s);
		if (OAPerformance.IncludeTriggers) {
			OAPerformance.LOG.fine(s);
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(thisClass);
		for (String triggerPropPath : trigger.getPropertyPaths()) {
			if (OAString.isEmpty(triggerPropPath)) {
				continue;
			}
			OAPath pp = new OAPath(thisClass, triggerPropPath);

			// addTrigger for every prop in the propPath
			String propPath = "";
			String revPropPath = "";
			OAObjectInfo oix = this;
			boolean bNoReverseFinder = false;
			boolean bReverseHasMany = false;

			for (int i = 0; i < pp.getLinkInfos().length; i++) {
				OALinkInfo li = pp.getLinkInfos()[i];
				OALinkInfo rli = li.getReverseLinkInfo();
				if (rli == null) {
					bNoReverseFinder = true;
				} else if (rli.getType() == OALinkInfo.MANY) {
					bReverseHasMany = true;
				}

				if (bSkipFirstNonManyProperty && i == 0 && (li.getType() == OALinkInfo.ONE)) {
				} else {
					TriggerInfo ti = oix._addTrigger(trigger, propPath, revPropPath, li.getName());
					ti.bNoReverseFinder = bNoReverseFinder;
					ti.bReverseHasMany = bReverseHasMany;
				}

				if (propPath.length() > 0) {
					propPath += ".";
					revPropPath = "." + revPropPath;
				}
				propPath += li.getName();
				revPropPath = li.getReverseName() + revPropPath;

				// todo: reverse path might not work (if it has a private method)
				oix = og.objectsInternal().callObjectInfoGetOAObjectInfo(li.getToClass());
			}

			if (pp.getEndLinkInfo() == null) {
				String[] ss = pp.getProperties();
				if (!bSkipFirstNonManyProperty || ss.length > 1) {
					TriggerInfo ti = oix._addTrigger(trigger, propPath, revPropPath, ss[ss.length - 1]);
					ti.bNoReverseFinder = bNoReverseFinder;
					ti.bReverseHasMany = bReverseHasMany;
				}
			}
		}
	}

	/**
	 * Internal helper that registers a trigger listener for a specific
	 * property. Ensures duplicate trigger entries are not created,
	 * logs trigger registration, and handles creation of dependent
	 * triggers for calculated properties.
	 *
	 * @param trigger        the trigger to register.
	 * @param propPath       forward property path from root.
	 * @param revPropPath    reverse property path back to root.
	 * @param listenProperty the property to listen on.
	 * @return the TriggerInfo that was created or matched.
	 */
	private TriggerInfo _addTrigger(final OATrigger trigger, final String propPath, final String revPropPath, final String listenProperty) {
		if (trigger == null || listenProperty == null) {
			throw new IllegalArgumentException("args can not be null");
		}

		boolean bFound = true;
		CopyOnWriteArrayList<TriggerInfo> al = hmTriggerInfo.get(listenProperty.toUpperCase());
		if (al == null) {
			bFound = false;
			al = hmTriggerInfo.computeIfAbsent(listenProperty.toUpperCase(), k -> new CopyOnWriteArrayList<OAObjectInfo.TriggerInfo>()); 
		}
		for (TriggerInfo ti : al) {
			if (ti.trigger.getTriggerListener() == trigger.getTriggerListener()) {
				if (OACompare.isEqual(propPath, ti.ppFromRootClass, true)) {
					return ti;
				}
			}
		}

		int x = aiTrigger.incrementAndGet();
		if (trigger.getUseBackgroundThread()) {
			aiTriggerBackgroundThread.incrementAndGet();
		}
		int x2 = aiTriggerBackgroundThread.get();

		aiAllTrigger.incrementAndGet();

		TriggerInfo ti = new TriggerInfo();
		ti.trigger = trigger;
		ti.ppFromRootClass = propPath;
		ti.ppToRootClass = revPropPath;
		ti.listenProperty = listenProperty;

		String s = (thisClass.getSimpleName() + ", name=" + trigger.getName() + ", listenPropName=" + listenProperty + ", revPropPath="
				+ revPropPath + ", trigger.cnt=" + x + ", trigger.background=" + x2 + ", system total=" + aiAllTrigger.get());
		LOG.fine(s);
		if (false && OAPerformance.IncludeTriggers) {
			OAPerformance.LOG.fine(s);
		} else if ((x - x2) > 50) {
			LOG.warning(s);
		}

		if (!bFound) {
			String[] calcProps = null;
			for (OACalcInfo ci : getCalcInfos()) {
				if (ci.getName().equalsIgnoreCase(listenProperty)) {
					calcProps = ci.getDependentProperties();
					break;
				}
			}

			if (calcProps != null) {
				OATriggerListener tl = new OATriggerListener() {
					@Override
					public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) throws Exception {
						// notify prop
						onChange(obj, listenProperty, hubEvent);
					}
				};
				final OATrigger trigger2 = new OATrigger(listenProperty, thisClass, tl, calcProps, trigger.getOnlyUseLoadedData(),
					trigger.getServerSideOnly(), trigger.getUseBackgroundThread(), true);
				OAGraphInternal og = (OAGraphInternal) OARuntime.graph(thisClass);
		        og.triggerInternal().addTrigger(trigger2);
		        
		        
		        OATrigger[] ts = (OATrigger[]) OAArray.add(OATrigger.class, trigger.getDependentTriggers(), trigger2);
				trigger.setDependentTriggers(ts);
			}
		}
		al.add(ti);
		return ti;
	}

	/**
	 * Removes a previously registered trigger from this object type,
	 * including any recursive or dependent triggers. Also logs the
	 * removal for debugging and performance tracking.
	 *
	 * @param trigger the trigger to remove.
	 */
	public void removeTrigger(OATrigger trigger) {
		if (trigger == null) {
			return;
		}

		String s = "";
		if (trigger.getPropertyPaths() != null) {
			for (String triggerPropPath : trigger.getPropertyPaths()) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += triggerPropPath;
			}
		}
		s = (thisClass.getSimpleName() + ", name=" + trigger.getName() + ", propPaths=[" + s + "]");
		LOG.fine(s);
		if (OAPerformance.IncludeTriggers) {
			OAPerformance.LOG.fine(s);
		}

		_removeTrigger(trigger);

		if (trigger.getPropertyPaths() == null) {
			return;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(thisClass);
		for (String spp : trigger.getPropertyPaths()) {
			OAPath pp = new OAPath(thisClass, spp);

			OAObjectInfo oix = this;
			for (int i = 0; i < pp.getLinkInfos().length; i++) {
				OALinkInfo li = pp.getLinkInfos()[i];
				oix = og.objectsInternal().callObjectInfoGetOAObjectInfo(li.getToClass());
				oix._removeTrigger(trigger);
			}
		}
		if (trigger.getDependentTriggers() == null) {
			return;
		}

		// close any child/calc triggers
		for (OATrigger t : trigger.getDependentTriggers()) {
			OAObjectInfo oix = og.objectsInternal().callObjectInfoGetOAObjectInfo(t.getRootClass());
			oix.removeTrigger(t);
		}
	}

	/**
	 * Internal helper used to remove trigger listeners from this
	 * object's TriggerInfo map. Handles decrementing counters and
	 * cleaning up empty lists.
	 *
	 * @param trigger the trigger instance being removed.
	 */
	protected void _removeTrigger(final OATrigger trigger) {
		if (trigger == null) {
			return;
		}
		synchronized (hmTriggerInfo) {
			// find all that use this trigger (1+)
			for (CopyOnWriteArrayList<TriggerInfo> alTriggerInfo : hmTriggerInfo.values()) {
				for (;;) {				
					TriggerInfo tiFound = null;
					for (TriggerInfo ti : alTriggerInfo) {
						if (ti.trigger == trigger) {
							tiFound = ti;
							break;
						}
					}
					if (tiFound == null) {
						break;
					}
					alTriggerInfo.remove(tiFound);
					int x = aiTrigger.decrementAndGet();
					aiAllTrigger.decrementAndGet();
					
					if (trigger.getUseBackgroundThread()) {
						aiTriggerBackgroundThread.decrementAndGet();
					}
					
					if (alTriggerInfo.size() == 0) {
						hmTriggerInfo.remove(tiFound.listenProperty.toUpperCase());
					}
	
					String s = (thisClass.getSimpleName() + ", name=" + trigger.getName() + ", prop=" + tiFound.listenProperty + ", revPropPath="
							+ tiFound.ppToRootClass + ", trigger.cnt=" + x + ", total=" + aiAllTrigger.get());
					LOG.fine(s);
					if (false && OAPerformance.IncludeTriggers) {
						OAPerformance.LOG.fine(s);
					}
				}				
			}
		}
	}

	/**
	 * Returns true if at least one trigger has been registered for
	 * this type.
	 *
	 * @return true if triggers exist.
	 */
	public boolean getHasTriggers() {
		return hmTriggerInfo.size() > 0;
	}

	/**
	 * Returns all triggers listening to the given property name.
	 * Performs a case-insensitive lookup.
	 *
	 * @param propertyName the property to check.
	 * @return list of triggers, or null if none registered.
	 */
	public ArrayList<OATrigger> getTriggers(String propertyName) {
		if (propertyName == null) {
			return null;
		}
		CopyOnWriteArrayList<TriggerInfo> al = hmTriggerInfo.get(propertyName.toUpperCase());
		if (al == null) {
			return null;
		}
		ArrayList<OATrigger> alTrigger = new ArrayList<OATrigger>();
		for (TriggerInfo ti : al) {
			alTrigger.add(ti.trigger);
		}
		return alTrigger;
	}

	/**
	 * Dispatches a change event to all triggers registered for the
	 * specified property. Ensures recursion depth is controlled and
	 * delegates to the internal processing routine.
	 *
	 * @param fromObject the source object of the change.
	 * @param prop       the changed property name.
	 * @param hubEvent   hub event context.
	 */
	public void onChange(final OAObject fromObject, final String prop, final HubEvent hubEvent) {
		if (prop == null || hubEvent == null) {
			return;
		}

		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		final int x = srvcOAThreadLocal.getRecursiveTriggerCount();
		if (x > 25) {
			throw new RuntimeException("onChange for Triggers has caused a loop over 25");
		}

		try {
			srvcOAThreadLocal.incRecursiveTriggerCount();

			CopyOnWriteArrayList<TriggerInfo> al = hmTriggerInfo.get(prop.toUpperCase());
			if (al == null) {
				return;
			}

			for (TriggerInfo ti : al) {
				_onChange(fromObject, prop, ti, hubEvent);
			}
		} finally {
			srvcOAThreadLocal.decRecursiveTriggerCount();
		}
	}

	/**
	 * Internal trigger-processing driver that evaluates server-side
	 * conditions, logging, and response timing before delegating to
	 * deeper change-handling logic.
	 *
	 * @param fromObject source object of the change.
	 * @param prop       changed property name.
	 * @param ti         trigger info to evaluate.
	 * @param hubEvent   hub event context.
	 */
	private void _onChange(final OAObject fromObject, final String prop, final TriggerInfo ti, final HubEvent hubEvent) {

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromObject);
		if (ti.trigger.getServerSideOnly()) {
			if (og.syncInternal().isClient()) return;
		}

		String s = "";
		if (ti.trigger.getPropertyPaths() != null) {
			for (String triggerPropPath : ti.trigger.getPropertyPaths()) {
				if (s.length() > 0) {
					s += ", ";
				}
				s += triggerPropPath;
			}
		}
		s = (thisClass.getSimpleName() + ", name=" + ti.trigger.getName() + ", propPaths=[" + s + "]");
		LOG.finer(s);
		if (OAPerformance.IncludeTriggers) {
			OAPerformance.LOG.finer(s);
		}

		long ts = System.currentTimeMillis();
		_onChange2(fromObject, prop, ti, hubEvent);
		ts = System.currentTimeMillis() - ts;

		if (ts > 3) {
			s = "over 3ms, fromObject=";
			if (fromObject == null) {
				s += fromObject;
			} else {
				s += fromObject.getClass().getSimpleName();
			}
			s += ", name=" + ti.trigger.getName() + ", property=" + ti.ppFromRootClass + ", ts=" + ts;
			LOG.finer(s);
			OAPerformance.LOG.fine(s);
		}
	}

	/**
	 * Second-stage trigger processing that determines whether the
	 * trigger should run immediately or within a background thread,
	 * depending on reverse-path rules and thread constraints.
	 *
	 * @param fromObject source object.
	 * @param prop       property name.
	 * @param ti         trigger metadata entry.
	 * @param hubEvent   event context.
	 */
	private void _onChange2(final OAObject fromObject, final String prop, final TriggerInfo ti, final HubEvent hubEvent) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromObject);

		if (ti.trigger.getServerSideOnly()) {
			if (og.syncInternal().isClient()) {
				return;
			}
		}

		
		
		boolean b = false;
		if (!ti.trigger.getUseBackgroundThread() && ti.trigger.getUseBackgroundThreadIfNeeded() && OARuntime.thread().isUIThread()) {
			// if UI thread, then run in bg thread if it has to do a many reverse pp, or if no rev pp
			if (ti.bNoReverseFinder) {
				b = true;
			} else if (ti.bReverseHasMany) {
				if (og.syncInternal().isServer()) {
					OADataSource ds = OARuntime.datasource().get(thisClass);
					b = (ds != null && ds.supportsStorage()); // might have to go to ds
				} else {
					b = true; // if client
				}
			}
		}

		final OARemoteThreadService srvcOARemoteThread = ((OAThreadService) OARuntime.thread()).getRemoteThreadService();  
		if ((b || ti.trigger.getUseBackgroundThread()) && !srvcOARemoteThread.isRemoteThread()) {
			og.triggerInternal().runTrigger(new Runnable() {
				@Override
				public void run() {
					_runOnChange1(fromObject, prop, ti, hubEvent);
				}
			});
		} else {
			_runOnChange1(fromObject, prop, ti, hubEvent);
		}
	}

	/**
	 * Core trigger execution routine. Performs reverse-path lookups
	 * when applicable and invokes the trigger's listener with each
	 * resolved root object. Handles detection of missing data and
	 * fallback to non-reverse processing when necessary.
	 *
	 * @param fromObject source of the change.
	 * @param prop       changed property.
	 * @param ti         trigger metadata.
	 * @param hubEvent   event information.
	 */
	private void _runOnChange1(final OAObject fromObject, final String prop, final TriggerInfo ti, final HubEvent hubEvent) {
		final OAThreadLocalService srvcThreadLocal = OARuntime.thread().getThreadLocalService();
		boolean bWas = false;
		try {
			if (ti.trigger.getServerSideOnly()) {
				bWas = srvcThreadLocal.getSendSyncMessages();
				srvcThreadLocal.setSendSyncMessages(true);
			}
			_runOnChange2(fromObject, prop, ti, hubEvent);
		}
		finally {
			if (ti.trigger.getServerSideOnly()) {
				srvcThreadLocal.setSendSyncMessages(bWas);
			}
		}
	}
	private void _runOnChange2(final OAObject fromObject, final String prop, final TriggerInfo ti, final HubEvent hubEvent) {
		if (ti.ppToRootClass == null || ti.ppToRootClass.length() == 0) {
			try {
				ti.trigger.getTriggerListener().onTrigger(fromObject, hubEvent, ti.ppToRootClass);
			} catch (Exception e) {
				throw new RuntimeException("OAObjectInof.autoCall error, "
						+ "thisClass=" + thisClass.getSimpleName() + ", "
						+ "propertyPath=" + ti.ppToRootClass + ", rootClass=" + ti.trigger.getRootClass().getSimpleName(),
						e);
			}
			return;
		}

		if (ti.bNoReverseFinder) {
			try {
				ti.trigger.getTriggerListener().onTrigger(null, hubEvent, ti.ppFromRootClass);
			} catch (Exception e) {
				throw new RuntimeException("OAObjectInfo.trigger error, "
						+ "thisClass=" + thisClass.getSimpleName() + ", "
						+ "propertyPath=" + ti.ppToRootClass + ", rootClass=" + ti.trigger.getRootClass().getSimpleName(),
						e);
			}
			return;
		}

		final AtomicInteger aiStatus = new AtomicInteger(0);
		// 1 = check to see if data is loaded
		// 2 = data not found

		OAFinder finder = new OAFinder(ti.ppToRootClass) {
			HashSet<UUID> hs = new HashSet();

			@Override
			protected void onFound(OAObject objRoot) {
				if (aiStatus.get() == 1) {
					return;
				}

				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objRoot);
				UUID g = og.objectsInternal().callObjectKeyGetKey(objRoot).getGuid();
				if (hs.contains(g)) {
					return;
				}
				hs.add(g);
				try {
					ti.trigger.getTriggerListener().onTrigger(objRoot, hubEvent, ti.ppFromRootClass);
				} catch (Exception e) {
					throw new RuntimeException("OAObjectInfo.autoCall error, "
							+ "thisClass=" + thisClass.getSimpleName() + ", "
							+ "propertyPathToRoot=" + ti.ppToRootClass + ", rootClass=" + ti.trigger.getRootClass().getSimpleName(),
							e);
				}
			}

			@Override
			protected void onDataNotFound() {
				if (aiStatus.get() == 1) {
					aiStatus.set(2);
					stop();
				}
			}
		};
		finder.setUseOnlyLoadedData(ti.trigger.getOnlyUseLoadedData());

		if (ti.bReverseHasMany) {
			// see if all of the data is already loaded, so that a reverse pp + finder can be used.
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromObject);
			boolean b = false;
			if (og.syncInternal().isServer()) {
				OADataSource ds = OARuntime.datasource().get(thisClass);
				b = (ds == null || !ds.supportsStorage()); // server must have all data loaded
			}

			if (!b) {
				// see if finder has has all of the data loaded
				aiStatus.set(1);
				try {
					finder.find(fromObject);
				} catch (Exception e) {
					ti.bNoReverseFinder = true;
					_onChange2(fromObject, prop, ti, hubEvent);
				}
				b = (aiStatus.get() != 2); // else: data is loaded, so use reverse pp to get data
				aiStatus.set(0);
			}

			if (!b) {
				try {
					ti.trigger.getTriggerListener().onTrigger(null, hubEvent, ti.ppFromRootClass);
				} catch (Exception e) {
					throw new RuntimeException("OAObjectInfo.trigger error, "
							+ "thisClass=" + thisClass.getSimpleName() + ", "
							+ "propertyPath=" + ti.ppToRootClass + ", rootClass=" + ti.trigger.getRootClass().getSimpleName(),
							e);
				}
				return;
			}
		}

		try {
			finder.find(fromObject);
		} catch (Exception e) {
			ti.bNoReverseFinder = true;
			_onChange2(fromObject, prop, ti, hubEvent);
		}
	}

	/**
	 * Sets the lookup flag, which controls whether this type may be
	 * used as a lookup/reference type in UI components or loaders.
	 *
	 * @param b true to enable lookup behavior.
	 */
	public void setLookup(boolean b) {
		this.bLookup = b;
	}

	/**
	 * Returns whether this type is marked as a lookup type.
	 *
	 * @return true if lookup mode is enabled.
	 */
	public boolean getLookup() {
		return bLookup;
	}

	/**
	 * Indicates whether JSON field names for this type begin with a
	 * capital letter.
	 *
	 * @return true if capitalized JSON field names are used.
	 */
	public boolean getJsonUsesCapital() {
		return bJsonUsesCapital;
	}
	
	/**
	 * Specifies whether JSON field names for this type should begin
	 * with a capital letter.
	 *
	 * @param b true to use capitalized names.
	 */
	public void setJsonUsesCapital(boolean b) {
		this.bJsonUsesCapital = b;
	}

	public boolean getGuidIsStored() {
		return bGuidIsStored;
	}
	
	public void setGuidIsStored(boolean b) {
		this.bGuidIsStored = b;
	}
	

	/**
	 * Enables or disables pre-select behavior used during object
	 * loading or filtering.
	 *
	 * @param b true to enable pre-selection.
	 */
	public void setPreSelect(boolean b) {
		this.bPreSelect = b;
	}

	/**
	 * Returns whether pre-select behavior is enabled.
	 *
	 * @return true if pre-selection is enabled.
	 */
	public boolean getPreSelect() {
		return this.bPreSelect;
	}

	/**
	 * Marks this metadata instance as processed by the initialization
	 * logic.
	 *
	 * @param b true to mark processed.
	 */
	public void setProcessed(boolean b) {
		this.bProcessed = b;
	}

	/**
	 * Returns whether this metadata instance has been processed by
	 * OAObjectInfoDelegate initialization.
	 *
	 * @return true if processed.
	 */
	public boolean getProcessed() {
		return bProcessed;
	}

	/**
	 * Assigns the set of properties whose display or enabled state
	 * depends on the current view context.
	 *
	 * @param ss array of dependent property names.
	 */
	public void setViewDependentProperties(String[] ss) {
		this.viewDependentProperties = ss;
	}

	/**
	 * Returns the set of view-dependent property names assigned to
	 * this type.
	 *
	 * @return array of property names, or null if none defined.
	 */
	public String[] getViewDependentProperties() {
		return this.viewDependentProperties;
	}

	/**
	 * Assigns the set of properties whose behavior depends on the current
	 * context. Stores the supplied array as-is without modification.
	 *
	 * @param ss array of context-dependent property names.
	 */
	public void setContextDependentProperties(String[] ss) {
		this.contextDependentProperties = ss;
	}

	/**
	 * Returns the list of properties whose behavior depends on the current
	 * context. May return null if none have been assigned.
	 *
	 * @return array of context-dependent property names.
	 */
	public String[] getContextDependentProperties() {
		return this.contextDependentProperties;
	}

	/**
	 * Returns the static enabled value associated with this type.
	 */
	public String getEnabledProperty() {
		return enabledProperty;
	}

	/**
	 * Sets the enabled value associated with this type.
	 *
	 * @param s the enabled value to assign.
	 */
	public void setEnabledProperty(String s) {
		enabledProperty = s;
	}

	/**
	 * Returns the enabled value associated with this type.
	 *
	 * @return true if enabled, otherwise false.
	 */
	public boolean getEnabledValue() {
		return enabledValue;
	}

	/**
	 * Sets the enabled value associated with this type.
	 *
	 * @param b the enabled value to assign.
	 */
	public void setEnabledValue(boolean b) {
		enabledValue = b;
	}

	/**
	 * Returns the property name used to determine visibility.
	 *
	 * @return visible-property name.
	 */
	public String getVisibleProperty() {
		return visibleProperty;
	}

	/**
	 * Assigns the property name used to determine visibility.
	 *
	 * @param s name of the visible-property.
	 */
	public void setVisibleProperty(String s) {
		visibleProperty = s;
	}

	/**
	 * Returns the static visible value associated with this type.
	 *
	 * @return true if visible, otherwise false.
	 */
	public boolean getVisibleValue() {
		return visibleValue;
	}

	/**
	 * Sets the static visible value associated with this type.
	 *
	 * @param b the visible value to assign.
	 */
	public void setVisibleValue(boolean b) {
		visibleValue = b;
	}

	/**
	 * Returns the name of the property used to determine context-specific
	 * enabled state.
	 *
	 * @return context-enabled property name.
	 */
	public String getContextEnabledProperty() {
		return contextEnabledProperty;
	}

	/**
	 * Assigns the property name used to determine context-specific enabled
	 * state.
	 *
	 * @param s the context-enabled property name.
	 */
	public void setContextEnabledProperty(String s) {
		contextEnabledProperty = s;
	}

	/**
	 * Returns the static context-enabled value associated with this type.
	 *
	 * @return true if context-enabled, otherwise false.
	 */
	public boolean getContextEnabledValue() {
		return contextEnabledValue;
	}

	/**
	 * Sets the static context-enabled value for this type.
	 *
	 * @param b the context-enabled value to assign.
	 */
	public void setContextEnabledValue(boolean b) {
		contextEnabledValue = b;
	}

	/**
	 * Returns the property name used to determine context-specific
	 * visibility.
	 *
	 * @return context-visible property name.
	 */
	public String getContextVisibleProperty() {
		return contextVisibleProperty;
	}

	/**
	 * Assigns the property name used to determine context-specific
	 * visibility.
	 *
	 * @param s the context-visible property name.
	 */
	public void setContextVisibleProperty(String s) {
		contextVisibleProperty = s;
	}

	/**
	 * Returns the static context-visible value associated with this type.
	 *
	 * @return true if context-visible, otherwise false.
	 */
	public boolean getContextVisibleValue() {
		return contextVisibleValue;
	}

	/**
	 * Sets the static context-visible value for this type.
	 *
	 * @param b the context-visible value to assign.
	 */
	public void setContextVisibleValue(boolean b) {
		contextVisibleValue = b;
	}

	/**
	 * Assigns the reflected callback method associated with this type.
	 *
	 * @param m the callback method to store.
	 */
	public void setObjectCallbackMethod(Method m) {
		this.objectCallbackMethod = m;
	}

	/**
	 * Returns the reflected callback method assigned to this type.
	 *
	 * @return the callback Method, or null if none assigned.
	 */
	public Method getObjectCallbackMethod() {
		return objectCallbackMethod;
	}

	/**
	 * Cached reference to the timestamp property metadata for this type.
	 * Determined on first access and reused thereafter.
	 */
	private volatile OAPropertyInfo piTimestamp;
	
	/**
	 * Indicates whether the timestamp-property lookup has been performed,
	 * preventing repeated scans of the property list.
	 */
	private volatile boolean bCheckTimestamp;

	/**
	 * Returns the timestamp property for this type by scanning the
	 * property list on first access and caching the result.
	 *
	 * @return the timestamp property info, or null if none defined.
	 */
	public OAPropertyInfo getTimestampProperty() {
		if (bCheckTimestamp) {
			return piTimestamp;
		}
		for (OAPropertyInfo pi : getPropertyInfos()) {
			if (pi.isTimestamp()) {
				piTimestamp = pi;
				break;
			}
		}
		bCheckTimestamp = true;
		return piTimestamp;
	}

	/**
	 * Cached reference to the submit-property metadata for this type.
	 * Identified on first lookup and reused afterward.
	 */
	private volatile OAPropertyInfo piSubmit;

	/**
	 * Indicates whether the submit-property lookup has already been
	 * performed, preventing redundant scans of the property list.
	 */
	private volatile boolean bCheckSubmit;

	/**
	 * Returns the submit property for this type by scanning the property
	 * list on first access and caching the result.
	 *
	 * @return the submit property info, or null if none defined.
	 */
	public OAPropertyInfo getSubmitProperty() {
		if (bCheckSubmit) {
			return piSubmit;
		}
		for (OAPropertyInfo pi : getPropertyInfos()) {
			if (pi.isSubmit()) {
				piSubmit = pi;
				break;
			}
		}
		bCheckSubmit = true;
		return piSubmit;
	}

	/**
	 * Returns whether this type is marked as having exactly one
	 * link property.
	 *
	 * @return true if one-and-only-one link is defined.
	 */
	public boolean getHasOneAndOnlyOneLink() {
		return bHasOneAndOnlyOneLink;
	}

	/**
	 * Sets whether this type has exactly one link property.
	 *
	 * @param b true to mark as one-and-only-one.
	 */
	public void setHasOneAndOnlyOneLink(boolean b) {
		this.bHasOneAndOnlyOneLink = b;
	}

	/**
	 * Returns the property name used to indicate soft-deleted state.
	 *
	 * @return soft-delete property name.
	 */
	public String getSoftDeleteProperty() {
		return softDeleteProperty;
	}

	/**
	 * Assigns the property name used to indicate soft-deleted state.
	 *
	 * @param s the soft-delete property name.
	 */
	public void setSoftDeleteProperty(String s) {
		softDeleteProperty = s;
	}

	/**
	 * Returns the property name holding the soft-delete reason.
	 *
	 * @return soft-delete reason property name.
	 */
	public String getSoftDeleteReasonProperty() {
		return softDeleteReasonProperty;
	}

	/**
	 * Assigns the property name that stores the soft-delete reason.
	 *
	 * @param s the soft-delete reason property name.
	 */
	public void setSoftDeleteReasonProperty(String s) {
		softDeleteReasonProperty = s;
	}

	/**
	 * Assigns the property name used to store the version value.
	 *
	 * @param s the version property name.
	 */
	public String getVersionProperty() {
		return versionProperty;
	}

	/**
	 * Assigns the property name used to store the version value.
	 *
	 * @param s the version property name.
	 */
	public void setVersionProperty(String s) {
		versionProperty = s;
	}

	/**
	 * Returns the link-property name that associates this type with its
	 * version object.
	 *
	 * @return version link-property name.
	 */
	public String getVersionLinkProperty() {
		return versionLinkProperty;
	}

	/**
	 * Assigns the link-property name used to associate this type with its
	 * version object.
	 *
	 * @param s the version link-property name.
	 */
	public void setVersionLinkProperty(String s) {
		versionLinkProperty = s;
	}

	/**
	 * Returns the property name used to identify the time-series value
	 * for this type.
	 *
	 * @return time-series property name.
	 */
	public String getTimeSeriesProperty() {
		return timeSeriesProperty;
	}

	/**
	 * Assigns the property name used to identify the time-series value
	 * for this type.
	 *
	 * @param s the time-series property name.
	 */
	public void setTimeSeriesProperty(String s) {
		timeSeriesProperty = s;
	}

	/**
	 * Returns the property name used to represent a freeze-state flag.
	 *
	 * @return freeze-state property name.
	 */
    public String getFreezeProperty() {
        return freezeProperty;
    }

    /**
     * Assigns the property name used to indicate a freeze-state flag.
     *
     * @param s the freeze-state property name.
     */
    public void setFreezeProperty(String s) {
        freezeProperty = s;
    }
	
    /**
     * Returns the mapped Pojo definition for this type. Loads the Pojo
     * lazily using OAObjectPojoLoader on first access.
     *
     * @return the Pojo instance.
     */
	public Pojo getPojo() {
		if (pojo == null) {
			OAObjectPojoLoader loader = new OAObjectPojoLoader();
			pojo = loader.loadIntoPojo(this);
		}
		return pojo;
	}

	/**
	 * Returns whether this type is configured as a singleton.
	 *
	 * @return true if singleton-enabled.
	 */
	public boolean getSingleton() {
		return singleton;
	}

	/**
	 * Sets whether this type should be treated as a singleton.
	 *
	 * @param b true to enable singleton mode.
	 */
	public void setSingleton(boolean b) {
		this.singleton = b;
	}

	/**
	 * Returns whether this type is configured to use a singleton Pojo.
	 *
	 * @return true if Pojo singleton mode is enabled.
	 */
	public boolean getPojoSingleton() {
		return pojoSingleton;
	}

	/**
	 * Sets whether this type should use a singleton Pojo instance.
	 *
	 * @param b true to enable Pojo singleton mode.
	 */
	public void setPojoSingleton(boolean b) {
		this.pojoSingleton = b;
	}

	/**
	 * Returns whether this type is configured to not use a Pojo.
	 *
	 * @return true if Pojo usage is disabled.
	 */
	public boolean getNoPojo() {
		return noPojo;
	}

	/**
	 * Sets whether this type should disable Pojo usage.
	 *
	 * @param b true to disable Pojo usage.
	 */
	public void setNoPojo(boolean b) {
		this.noPojo = b;
	}

	
	/**
	 * Friend level access to package protected properties.
	 */
	public static final class FriendAccess {
		private FriendAccess() {
		}
		
		public void setName(OAObjectInfo oi, String name) {
            oi.setName(name);
        }

		public void setPropertyIds(OAObjectInfo oi, String[] ss) {
			oi.idProperties = ss;
		}
		
		public void setImportMatchPropertyNames(OAObjectInfo oi, String[] ss) {
			oi.importMatchPropertyNames = ss;
		}

		public static void setImportMatchPropertyPaths(OAObjectInfo oi, String[] ss) {
			oi.importMatchPropertyPaths = ss;
		}
		
		public void resetPropertyInfo(OAObjectInfo oi) {
			oi.resetPropertyInfo();
		}
		
		public String[] getPrimitiveProps(OAObjectInfo oi) {
			return oi.getPrimitiveProperties();
		}
		public void setPrimitiveProps(OAObjectInfo oi, String[] props) {
			oi.primitiveProps = props;
		}

		public String[] getHubProps(OAObjectInfo oi) {
			return oi.getHubProperties();
		}
		public void setHubProps(OAObjectInfo oi, String[] props) {
			oi.hubProps = props;
		}
		
		public boolean getSetRecursive(OAObjectInfo oi) {
			return oi.bSetRecursive;
		}
		public void setSetRecursive(OAObjectInfo oi, boolean b) {
			oi.bSetRecursive = b;;
		}

		public OALinkInfo getRecursiveOneLinkInfo(OAObjectInfo oi) {
			return oi.liRecursiveOne;
		}
		public void setRecursiveOneLinkInfo(OAObjectInfo oi, OALinkInfo li) {
			oi.liRecursiveOne = li;
		}


		public OALinkInfo getRecursiveManyLinkInfo(OAObjectInfo oi) {
			return oi.liRecursiveMany;
		}
		public void setRecursiveManyLinkInfo(OAObjectInfo oi, OALinkInfo li) {
			oi.liRecursiveMany = li;
		}
		
		public boolean getSetLinkToOwner(OAObjectInfo oi) {
			return oi.bSetLinkToOwner;
		}
		public void setSetLinkToOwner(OAObjectInfo oi, boolean b) {
			oi.bSetLinkToOwner = b;
		}
		
		public OALinkInfo getLinkToOwner(OAObjectInfo oi) {
			return oi.liLinkToOwner;
		}
		public void setLinkToOwner(OAObjectInfo oi, OALinkInfo li) {
			oi.liLinkToOwner = li;
		}

		public int getWeakReferenceable(OAObjectInfo oi) {
			return oi.weakReferenceable;
		}
		public void setWeakReferenceable(OAObjectInfo oi, int x) {
			oi.weakReferenceable = x;
		}
	}
	
	private final static FriendAccess friendAccess = new FriendAccess(); 
	public static FriendAccess getFriendAccess() {
		return friendAccess;
	}
}

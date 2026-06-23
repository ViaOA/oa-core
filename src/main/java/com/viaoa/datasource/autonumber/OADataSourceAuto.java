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
package com.viaoa.datasource.autonumber;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;


//qqqqqqqq what about local (negative numbers)

/*qqqqqqqqqqqqqqq
CODEX

4. src/main/java/com/viaoa/datasource/clientserver/OADataSourceClient.java, getMaxLength(...)
      - Exact execution path: multiple threads call getMaxLength concurrently; hmMax is a plain HashMap mutated
        without synchronization.
      - Why concrete bug: datasource clients are runtime shared infrastructure; concurrent metadata lookups can race
        and corrupt/cache stale values unpredictably.
      - Minimal fix: use ConcurrentHashMap<String, Integer> or synchronize the cache access.
      - Suggested test: concurrent repeated getMaxLength calls for different class/property keys using a fake remote
        client; verify stable results and no map corruption.


5. src/main/java/com/viaoa/datasource/autonumber/OADataSourceAuto.java, getNextNumber(...) / assignId(...)
      - Exact execution path: startNextNumber > 0; two threads concurrently assign IDs for the same class;
        getNextNumber adjusts nn.next outside synchronized(nn), while assignId increments inside synchronized(nn).
      - Why concrete bug: one thread can reset nn.next back to the start value after another thread has already
        incremented it, creating duplicate assigned IDs.
      - Minimal fix: perform the startNextNumber adjustment inside synchronized(nn), ideally in the same critical
        section used to read/increment nn.next.
      - Suggested test: set starting next number, concurrently create/assign IDs for many objects of the same class,
        verify no duplicate IDs and no value below the configured start.


*/

/**
 * A lightweight {@link OADataSource} implementation that does not support
 * storage or select operations. Its primary responsibility is assigning
 * autonumber-style object identifier values to newly created {@link OAObject}
 * instances.
 * <p>
 * This datasource can operate in two modes:
 * <ul>
 *   <li><b>Global mode:</b> uses a shared Hub of {@link NextNumber} objects.</li>
 *   <li><b>Local mode:</b> uses a caller-supplied Hub that defines NextNumber
 *       sequences on a per-class basis.</li>
 * </ul>
 * <p>
 * When enabled via {@link #setAssignIdOnCreate(boolean)}, object IDs are
 * automatically assigned when objects are constructed. Otherwise, IDs are
 * assigned during {@link #insert(OAObject)} or related operations.
 * <p>
 * This datasource is often used as a “dummy” fallback datasource in systems
 * that require object ID assignment but that do not need persistence or query
 * capabilities.
 */
public class OADataSourceAuto extends OADataSource {

	/**
	 * Global Hub of {@link NextNumber} objects used to store autonumber sequences
	 * shared across all OADataSourceAuto instances unless overridden.
	 */
	private static Hub<NextNumber> hubNextNumberGlobal; // new numbers for seq ids

	/**
	 * Indicates whether this datasource should respond positively to class-support
	 * checks for all classes. When true, autonumber assignment will be attempted
	 * for any class unless explicitly ignored.
	 */
	private boolean bSupportAllClasses = true;

	/**
	 * Hub containing {@link NextNumber} instances for autonumber assignment. This
	 * may point to a caller-supplied Hub or the global shared Hub.
	 */
	private Hub<NextNumber> hubNextNumber; // new numbers for seq ids

	/**
	 * Cache mapping classes to their corresponding {@link NextNumber} objects or
	 * placeholder mappings to indicate that no autonumber assignment should occur
	 * for a given class.
	 */
	private final Map<Class<?>, NextNumber> hmClassNextNumber = new ConcurrentHashMap<>();

	private int startNextNumber;
	
	
	/**
	 * Synchronization lock object used when lazily creating {@link NextNumber}
	 * instances for previously unseen classes.
	 */
	private Object LOCK = new Object();

	
	/**
	 * Creates a new OADataSourceAuto instance that configures
	 * itself as the last datasource. Uses or initializes the global Hub of
	 * {@link NextNumber} sequences.
	 */
	public OADataSourceAuto() {
		this(true);
	}

	/**
	 * Creates an OADataSourceAuto and optionally marks it as the last datasource.
	 * Registration is enabled by default.
	 *
	 * @param bMakeLastDataSource true to designate this datasource as last in chain
	 */
	public OADataSourceAuto(boolean bMakeLastDataSource) {
		setLast(bMakeLastDataSource);
	}

	/**
	 * Creates an OADataSourceAuto that uses the specified Hub to store
	 * {@link NextNumber} objects for autonumbering.
	 *
	 * @param hubNextNumber Hub containing NextNumber instances
	 */
	public OADataSourceAuto(Hub<NextNumber> hubNextNumber, boolean makeLast) {
		this(makeLast);
		this.hubNextNumber = hubNextNumber;
		setName("OADataSourceAuto DataSource");
	}

	public OADataSourceAuto(Hub hubNextNumber) {
		this(hubNextNumber, true);
	}
	

	public Hub<NextNumber> getNextNumbers() {
		if (hubNextNumber == null) {
			hubNextNumber = getGlobalNextNumbers();
			if (hubNextNumber == null) {
				hubNextNumber = new Hub<>(NextNumber.class);
			}
		}
		return hubNextNumber;
	}

	/**
	 * Returns the global Hub containing {@link NextNumber} objects.
	 *
	 * @return the shared Hub of autonumber sequences
	 */
	public static Hub<NextNumber> getGlobalNextNumbers() {
		return hubNextNumberGlobal;
	}
	
	/**
	 * Configures the global Hub of {@link NextNumber} instances used for
	 * autonumber assignment across all OADataSourceAuto instances.
	 *
	 * @param hubNextNumber the Hub to install as the global sequence source
	 */
	public static void setGlobalNextNumbers(Hub<NextNumber> hubNextNumber) {
		hubNextNumberGlobal = hubNextNumber;
	}

	public void setStartingNextNumber(int x) {
		this.startNextNumber = x;
	}

	public int getStartingNextNumber() {
		return this.startNextNumber;
	}	

	/**
	 * Always returns false. OADataSourceAuto does not support any form of storage.
	 *
	 * @return false
	 */
	public boolean supportsStorage() {
		return false;
	}

	/**
	 * Returns whether autonumber assignment should be attempted for all classes.
	 *
	 * @return true if this datasource supports all classes
	 */
	public boolean getSupportAllClasses() {
		return bSupportAllClasses;
	}

	/**
	 * Configures whether autonumber assignment should be applied to all classes.
	 *
	 * @param b true to support all classes
	 */
	public void setSupportAllClasses(boolean b) {
		bSupportAllClasses = b;
	}

	/**
	 * Determines whether autonumber assignment should be allowed for the
	 * specified class. When {@link #bSupportAllClasses} is true, all classes
	 * are treated as supported unless explicitly ignored. Otherwise, a class
	 * must already exist in the {@link #hmClassNextNumber} map or the NextNumber Hub.
	 *
	 * @param clazz  the class to test
	 * @param filter optional filter (ignored for this datasource)
	 * @return true if autonumber assignment is supported for this class
	 */
	@Override
	public boolean isClassSupported(Class clazz, OAFilter filter) {
		if (clazz == null) {
			return false;
		}
		if (clazz.equals(NextNumber.class)) {
			return true;
		}

		NextNumber nn = getNextNumber(clazz);
		return (nn != null);
	}



	
	/**
	 * Retrieves or creates the {@link NextNumber} sequence object associated with the
	 * specified class.
	 * <p>
	 * The method first checks the internal {@code hmIgnoreClass} map for an existing
	 * mapping. If a mapping is present, it is returned immediately.
	 * <p>
	 * When class support is globally enabled, the method attempts to lazily create a
	 * new {@link NextNumber} instance for classes not yet seen. Creation is performed
	 * within a synchronized block using {@link #LOCK} to ensure thread safety.
	 * <p>
	 * A new {@link NextNumber} is initialized with:
	 * <ul>
	 *   <li>its ID set to the class name</li>
	 *   <li>a property selected from the class's ID properties, if any are marked
	 *       with auto-assign</li>
	 * </ul>
	 * The created object is added to the configured {@code hubNextNumber} and cached
	 * in {@code hmIgnoreClass}.
	 *
	 * @param clazz the class whose autonumber sequence is requested
	 * @return the {@link NextNumber} for the class, or {@code null} if unsupported
	 */
	protected NextNumber getNextNumber(final Class<?> clazz) {
		NextNumber nn = _getNextNumber(clazz);
		if (nn != null && startNextNumber > 0) {
			if (nn.getNext() < startNextNumber) nn.setNext(startNextNumber);
		}
		return nn;
	}
	
	private NextNumber _getNextNumber(final Class<?> clazz) {
		NextNumber nn = hmClassNextNumber.get(clazz);
		if (nn != null) {
			return nn;
		}

		if (NextNumber.class.equals(clazz)) {
			return null; 
		}
		
		synchronized (LOCK) {
			nn = hmClassNextNumber.get(clazz);
			if (nn != null) return nn;

			nn = getNextNumbers().find(NextNumber.P_Id, clazz.getName());
			
			if (nn == null) {
				if (!bSupportAllClasses) {
					return null;
				}
				
				nn = new NextNumber();
				nn.setId(clazz.getName());
	
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
				final OAObjectInfo oi = og.internal().objects().info().getOAObjectInfo(clazz);
				final String[] props = oi.getIdProperties();
				if (props != null) {
					for (String s : props) {
						OAPropertyInfo pi = oi.getPropertyInfo(s);
						if (pi != null && pi.getAutoAssign()) {
							nn.setProperty(s);
							break;
						}
					}
				}
				getNextNumbers().add(nn);
			}
			hmClassNextNumber.put(clazz, nn);
		}
		return nn;
	}

	/**
	 * Assigns an autonumber ID to the specified object.
	 * <p>
	 * The method retrieves the {@link NextNumber} sequence for the object's class
	 * and updates the object's ID property if the property is defined and marked
	 * for auto-assignment.
	 * <p>
	 * A unique ID is generated by incrementing the sequence value and verifying
	 * that no existing cached object already uses the ID. Assignment occurs within
	 * an assigning-ID guard via {@link OAObjectDSDelegate}.
	 *
	 * @param oaObj the object to receive an assigned ID
	 */
	public void assignId(OAObject oaObj) {
		if (oaObj == null) {
			return;
		}

		NextNumber nn = getNextNumber(oaObj.getClass());
		if (nn == null) {
			return;
		}
		String prop = nn.getProperty();
		if (prop == null) {
			return;
		}

		int id;
		for (;;) {
			synchronized (nn) {
				id = nn.getNext();
				nn.setNext(id + 1);
			}
			// 20141201
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);
			Object test = og.internal().objects().cache().getObject(oaObj.getClass(), id);
			//was: Object test = OAObjectReflectDelegate.getObject(oaObj.getClass(), id);
			if (test == null) {
				break;
			}
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);
		try {
			og.internal().objects().ds().setAssigningId(oaObj, true);
			oaObj.setProperty(prop, id);
		} finally {
			og.internal().objects().ds().setAssigningId(oaObj, false);
		}
	}

	/**
	 * No-op implementation. This datasource does not manage many-to-many link updates.
	 *
	 * @param masterObject          ignored
	 * @param adds                  ignored
	 * @param removes               ignored
	 * @param propertyNameFromMaster ignored
	 */
	@Override
	public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propertyNameFromMaster) {
	}

	/**
	 * Determines whether the specified property name matches the autonumber
	 * property defined for the object's class.
	 *
	 * @param oaObj        the object being checked
	 * @param propertyName the property name to test
	 * @return true if the property corresponds to the class's autonumber field
	 */
	public boolean willCreatePropertyValue(OAObject oaObj, String propertyName) {
		if (oaObj != null && propertyName != null) {
			NextNumber nn = getNextNumber(oaObj.getClass());
			if (nn != null) {
				if (propertyName.equalsIgnoreCase(nn.getProperty())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Inserts the object by assigning an ID if automatic assignment on create
	 * is disabled. No persistence is performed.
	 *
	 * @param object the object being inserted
	 */
	public void insert(OAObject object) {
		if (!getAssignIdOnCreate()) {
			assignId(object);
		}
	}

	/**
	 * Inserts the object without references. Behaves the same as {@link #insert}
	 * by assigning an ID when automatic creation assignment is disabled.
	 *
	 * @param obj the object to insert
	 */
	public void insertWithoutReferences(OAObject obj) {
		if (!getAssignIdOnCreate()) {
			assignId(obj);
		}
	}

	/**
	 * No-op implementation. This datasource does not support updating objects.
	 *
	 * @param object            ignored
	 * @param includeProperties ignored
	 * @param excludeProperties ignored
	 */
	public void update(OAObject object, String[] includeProperties, String[] excludeProperties) {
	}

	/**
	 * No-op implementation. This datasource does not support deleting objects.
	 *
	 * @param object ignored
	 */
	public void delete(OAObject object) {
	}

	/**
	 * Always returns null. Command execution is not supported by this datasource.
	 *
	 * @param command ignored
	 * @return null
	 */
	public Object execute(String command) {
		return null;
	}

	/**
	 * Returns null. Blob property retrieval is not supported.
	 *
	 * @param obj          ignored
	 * @param propertyName ignored
	 * @return null
	 */
	@Override
	public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
		return null;
	}

	/**
	 * Always returns -1. Counting is not supported by this datasource.
	 *
	 * @param selectClass            ignored
	 * @param queryWhere             ignored
	 * @param params                 ignored
	 * @param whereObject            ignored
	 * @param propertyFromWhereObject ignored
	 * @param extraWhere             ignored
	 * @param max                    ignored
	 * @return -1
	 */
	@Override
	public int count(Class selectClass, String queryWhere, Object[] params, OAObject whereObject, String propertyFromWhereObject,
			String extraWhere, int max) {
		return -1;
	}

	/**
	 * Always returns -1. Passthru counting is not supported.
	 *
	 * @param selectClass ignored
	 * @param queryWhere  ignored
	 * @param max         ignored
	 * @return -1
	 */
	@Override
	public int countPassthru(Class selectClass, String queryWhere, int max) {
		return -1;
	}

	/**
	 * Always returns null. Selection is not supported by this datasource.
	 *
	 * @param selectClass             ignored
	 * @param queryWhere              ignored
	 * @param params                  ignored
	 * @param queryOrder              ignored
	 * @param whereObject             ignored
	 * @param propertyFromWhereObject ignored
	 * @param extraWhere              ignored
	 * @param max                     ignored
	 * @param filter                  ignored
	 * @param bDirty                  ignored
	 * @return null
	 */
	@Override
	public OADataSourceIterator select(Class selectClass, String queryWhere, Object[] params, String queryOrder, OAObject whereObject,
			String propertyFromWhereObject, String extraWhere, int max, OAFilter filter, boolean bDirty) {
		return null;
	}

	/**
	 * Always returns null. Passthru selection is not supported.
	 *
	 * @param selectClass ignored
	 * @param queryWhere  ignored
	 * @param queryOrder  ignored
	 * @param max         ignored
	 * @param filter      ignored
	 * @param bDirty      ignored
	 * @return null
	 */
	@Override
	public OADataSourceIterator selectPassthru(Class selectClass, String queryWhere, String queryOrder, int max, OAFilter filter,
			boolean bDirty) {
		return null;
	}
}

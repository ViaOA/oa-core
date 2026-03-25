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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.viaoa.comm.io.IODummy;
import com.viaoa.hub.Hub;
import com.viaoa.remote.multiplexer.io.RemoteObjectInputStream;
import com.viaoa.remote.multiplexer.io.RemoteObjectOutputStream;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.Tuple;

/**
 * Serializes and deserializes OAObject state for caching, messaging, and
 * distributed synchronization. This serializer transfers identity and data
 * in a form that preserves lazy loading and metadata-driven resolution on the
 * destination side.
 *
 * <p>Object references are represented using OAObjectKey so that related
 * objects do not need to be materialized during serialization. This enables
 * efficient graph projection for remote clients and reduces network
 * payload size.</p>
 *
 * <p>Hub properties and collections are handled through key lists so that
 * reverse-link integrity is maintained when objects are rehydrated and
 * combined with existing state in the object cache.</p>
 *
 * <p>No runtime metadata is mutated during serialization. All behavior is
 * deterministic and based on OAObjectInfo, ensuring safe transmission across
 * server/client boundaries and consistent object identity reconciliation.</p>
 *
 * <p>
 * Note: this is final so that it can not be subclassed, which would cause serialization problems when it tries to recreate with the new
 * subclass instance - this is a wrapper that is serialized and transported, then unserialized.
 * Use setCallback(..) to be able to control each object's setting as it is serialized.
 * 
 * @see OAObject
 * @see OAObjectKey
 * @see OAObjectCacheDelegate
 * @see OAObjectInfo
 */
public final class OAObjectSerializer<TYPE> implements Serializable {
	static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(OAObjectSerializer.class.getName());

	/**
	 * Identifier for the client associated with this serialization session.
	 * Used to include client context in logging and diagnostic output.
	 */
	private int clientId; // 20171216
	
	/**
	 * Unique wrapper identifier written to the stream and used for tracking
	 * serialization sessions, debugging, and correlation of transmitted data.
	 */
	private int id; // 20171216

	/**
	 * The primary root object to be serialized. This may be an OAObject,
	 * a Hub, or any serializable structure supported by the framework.
	 */
	private Object object; // object to serialize
	
	/**
	 * Optional secondary object to be serialized along with the primary
	 * object. Used when two related objects must be transported together.
	 */
	private Object extraObject; // extra object to serialize

	/**
	 * Indicates whether compression is enabled for this serializer session.
	 * When true, output is wrapped in Deflater streams for reduced size.
	 */
	private transient boolean bCompress;
	
	/**
	 * Stack used to store and restore include/exclude property lists during
	 * nested serialization callbacks. Ensures per-object property scoping.
	 */
	private transient Stack<Tuple<String[], String[]>> stack = new Stack(); // used for callback, to store properties to include and exclude
	
	/**
	 * Stack of objects currently being serialized. Tracks the active
	 * serialization path so callbacks can determine context and depth.
	 */
	private transient Stack stackObject = new Stack(); // used for callback, to know which objects are currently being serialized
	
	/**
	 * Optional list of classes whose reference properties must be excluded
	 * from serialization. Used to suppress entire reference types.
	 */
	private transient Class[] excludedReferences;

	/**
	 * Maximum allowed recursion depth for serializing linked objects. When
	 * exceeded, references are written to an overflow list rather than
	 * serialized immediately.
	 */
	private transient int overflowLimit = 100; // this might need to be adjusted for handling stackOverflow

	/**
	 * Current serialization depth counter, incremented before serializing
	 * an object and decremented after completion.
	 */
	private transient int levelsDeep;
	
	/**
	 * Count of all objects serialized in the current session, including
	 * nested and overflow objects.
	 */
	private int totalObjectsWritten;

	/**
	 * Shared empty array used to represent explicit include/exclude lists
	 * when all or no properties should be serialized.
	 */
	final static String[] EmptyProperties = new String[0];

	/**
	 * List of property names that are explicitly included during
	 * serialization. When set, all other properties are excluded.
	 */
	transient String[] includeProps;

	/**
	 * List of property names that are explicitly excluded during
	 * serialization. When set, all others are included.
	 */
	transient String[] excludeProps;

	/**
	 * Optional callback used to control serialization behavior, including
	 * selective inclusion of reference properties and custom value mapping.
	 */
	private transient OAObjectSerializerCallback callback;

	/**
	 * Global counter incremented for each write operation. Used only for
	 * diagnostic logging to trace serialization activity.
	 */
	private static volatile int wcnter;

	/**
	 * Global counter incremented for each read operation. Used to trace
	 * deserialization activity when debug mode is enabled.
	 */
	private static volatile int rcnter;

	// Solution for handling deep object graphs that can cause stack overflow exceptions:
	// This is used to handle stackTraceOverflow from happening.
	//   The graph will only allow for "overflowLimit" recursive objects, and will then
	//   add additional OAObjectSerializers that will then be included.
	private transient OAObjectSerializer parentWrapper;
	
	/**
	 * Holds the collection of overflow descriptors recorded when the serializer
	 * exceeds the configured recursion-depth limit. Each {@link Overflow} entry
	 * represents a deferred reference that could not be serialized in the
	 * primary pass.
	 *
	 * <p>The list is created lazily on the first overflow event and consumed
	 * during {@code finishWrite}, where each deferred object is serialized by its
	 * own {@link OAObjectSerializer} wrapper. During deserialization,
	 * {@code finishRead} uses this list to reconnect the reconstructed overflow
	 * objects back to their parent properties.</p>
	 */
	private transient LinkedList<Overflow> listOverflow;


	/**
	 * Lightweight descriptor used to record an overflow event during serialization
	 * when the recursion-depth limit ({@code overflowLimit}) is exceeded.
	 *
	 * <p>When a referenced object is too deep in the graph to serialize in the
	 * current pass, an Overflow instance is created holding:</p>
	 * <ul>
	 *   <li>the parent {@link OAObject} whose property will later receive the value,</li>
	 *   <li>the property name being assigned,</li>
	 *   <li>the object that triggered overflow,</li>
	 *   <li>a snapshot of the active serialization stack,</li>
	 *   <li>the depth at which overflow occurred.</li>
	 * </ul>
	 *
	 * <p>Overflow entries are written out in {@code finishWrite(ObjectOutputStream)}
	 * and later rehydrated in {@code finishRead(ObjectInputStream)}, where the
	 * deferred objects are reconstructed using new {@link OAObjectSerializer}
	 * wrappers and reassigned to their parent properties.</p>
	 */
	static class Overflow implements Serializable {

		/**
		 * The parent OAObject that owns the reference property which could not be
		 * serialized due to exceeding the recursion-depth limit. During
		 * {@code finishRead}, this parent is used to assign the resolved overflow
		 * object's value back into {@link #property}.
		 */
		OAObject parentObject;
		
		/**
		 * Name of the reference property on {@link #parentObject} that will receive
		 * the reconstructed overflow object once it is deserialized by the wrapper
		 * created for deferred processing.
		 */
		String property;
		
		/**
		 * The serialization depth at which this overflow event occurred. Used by the
		 * overflow wrapper to adjust its own {@code overflowLimit} so it can safely
		 * continue serialization without re-triggering overflow immediately.
		 */
		transient int levelsDeep;
		
		/**
		 * The actual referenced object that could not be serialized in the primary
		 * pass due to depth constraints. This object is later serialized by a new
		 * {@link OAObjectSerializer} wrapper during {@code finishWrite}.
		 */
		transient Object object;
		
		/**
		 * A cloned snapshot of the serializer's {@code stackObject} at the moment the
		 * overflow occurred. The overflow wrapper inherits this stack so its callback
		 * logic and reference-path context match the original serialization path.
		 */
		transient Stack stack;
	}

	/**
	 * Sets the serializer wrapper identifier.
	 *
	 * @param id the identifier assigned to this serializer instance
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the identifier assigned to this serializer instance.
	 *
	 * @return the current wrapper identifier
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Sets the client identifier associated with this serializer.
	 *
	 * @param id the client identifier to assign
	 */
	public void setClientId(int id) {
		this.clientId = id;
	}

	/**
	 * Returns the client identifier associated with this serializer.
	 *
	 * @return the current client identifier
	 */
	public int getClientId() {
		return this.clientId;
	}

	/**
	 * Max number of objects to serialize.
	 */
	private transient int maxObjects;
	private transient int minExpectedAmt; // minimum expected to save

	private transient int maxSize;
	private transient HashMap<OALinkInfo, Integer> hmLinkInfoCount;

	/**
	 * Creates a serializer for the specified root object. Compression can be
	 * enabled and an optional callback can be provided to control which
	 * reference properties are included during serialization.
	 *
	 * <p>If the root object is a {@link Hub}, the expected minimum number
	 * of serialized objects is set to the hub size. The callback is assigned
	 * using {@code setCallback}.</p>
	 *
	 * @param object   the root object to serialize
	 * @param bCompress whether compression should be used
	 * @param callback  the callback used to configure serialized properties
	 */
	public OAObjectSerializer(TYPE object, boolean bCompress, OAObjectSerializerCallback callback) {
		this.object = object;
		this.bCompress = bCompress;
		if (object instanceof Hub) {
			minExpectedAmt = ((Hub) object).getSize();
		}
		setCallback(callback);
	}

	/**
	 * Creates a serializer for the specified root object, with an option to
	 * enable compression. No callback is defined for property-level control.
	 *
	 * <p>If the root object is a {@link Hub}, the expected minimum number
	 * of serialized objects is set to the hub size.</p>
	 *
	 * @param object    the root object to serialize
	 * @param bCompress whether compression should be used
	 */
	public OAObjectSerializer(TYPE object, boolean bCompress) {
		this.object = object;
		this.bCompress = bCompress;
		if (object instanceof Hub) {
			minExpectedAmt = ((Hub) object).getSize();
		}
	}

	/**
	 * Creates a serializer that wraps a primary object and an additional
	 * secondary object. Compression may be enabled, and an optional
	 * callback can configure which reference properties to serialize.
	 *
	 * <p>If the primary object is a {@link Hub}, the expected minimum number
	 * of serialized objects is set to the hub size. The callback is assigned
	 * using {@code setCallback}.</p>
	 *
	 * @param object       the primary root object to serialize
	 * @param extraObject  an additional object to serialize
	 * @param bCompress    whether compression should be used
	 * @param callback     the callback used to configure serialized properties
	 */
	public OAObjectSerializer(TYPE object, Object extraObject, boolean bCompress, OAObjectSerializerCallback callback) {
		this.object = object;
		this.extraObject = extraObject;
		this.bCompress = bCompress;
		if (object instanceof Hub) {
			minExpectedAmt = ((Hub) object).getSize();
		}
		setCallback(callback);
	}

	/**
	 * Creates a serializer for the specified root object with compression
	 * support and a flag controlling serialization of reference properties.
	 *
	 * <p>If {@code bAllReferences} is true, all reference properties are
	 * included using {@code includeAllProperties}. Otherwise, all reference
	 * properties are excluded using {@code excludeAllProperties}.</p>
	 *
	 * @param object         the root object to serialize
	 * @param bCompress      whether compression should be used
	 * @param bAllReferences whether all or none of the reference properties
	 *                       should be serialized
	 */
	public OAObjectSerializer(TYPE object, boolean bCompress, boolean bAllReferences) {
		this.object = object;
		this.bCompress = bCompress;
		if (bAllReferences) {
			includeAllProperties();
		} else {
			excludeAllProperties();
		}
	}

	// 20200102 include blobs
	private boolean bIncludeBlobs;

	/**
	 * Indicates whether blob properties should be included during serialization.
	 *
	 * @return {@code true} if blob properties are included, otherwise {@code false}
	 */
	public boolean getIncludeBlobs() {
		return bIncludeBlobs;
	}

	/**
	 * Enables or disables inclusion of blob properties during serialization.
	 *
	 * @param b {@code true} to include blob properties, otherwise {@code false}
	 */
	public void setIncludeBlobs(boolean b) {
		bIncludeBlobs = b;
	}

	/**
	 * Defines classes for which reference properties should not be serialized.
	 *
	 * @param classes one or more classes to exclude from reference serialization
	 */
	public void setExcludedReferences(Class... classes) {
		//LOG.finer("excludedReferences="+classes);
		this.excludedReferences = classes;
	}

	/**
	 * Sets the list of classes for which reference properties should be excluded
	 * from serialization. This is an alias for {@link #setExcludedReferences(Class...)}.
	 *
	 * @param classes classes to exclude from reference serialization
	 */
	public void excludedClasses(Class... classes) {
		//LOG.finer("excludedReferences="+classes);
		this.excludedReferences = classes;
	}

	/**
	 * Returns the reference value to serialize for the given object. If a
	 * callback is defined, its {@code getReferenceValueToSend} method is used
	 * to determine the appropriate value.
	 *
	 * @param obj the reference object being evaluated
	 * @return the object value to send during serialization
	 */
	public Object getReferenceValueToSend(Object obj) {
		if (callback != null) {
			obj = callback.getReferenceValueToSend(obj);
		}
		return obj;
	}

	/**
	 * Sets the maximum number of objects that may be serialized by this wrapper.
	 *
	 * @param max the maximum number of objects allowed
	 */
	public void setMax(int max) {
		this.maxObjects = max;
	}

	/**
	 * Returns the maximum number of objects permitted for serialization.
	 *
	 * @return the configured maximum object count
	 */
	public int getMax() {
		return this.maxObjects;
	}

	/**
	 * Returns the total number of objects written during serialization.
	 *
	 * @return the number of objects serialized so far
	 */
	public int getTotalObjectsWritten() {
		return totalObjectsWritten;
	}

	/**
	 * Sets the maximum allowed size, in bytes, of the serialized output. When the
	 * compressed output exceeds this size, serialization of additional objects
	 * will stop.
	 *
	 * @param maxSize the maximum compressed output size
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Returns the maximum compressed output size allowed for serialization.
	 *
	 * @return the configured maximum output size
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	// private int indent;

	/**
	 * Performs setup before serializing an {@link OAObject}. Increments the total
	 * object count, delegates to any active wrapper serializer, and invokes the
	 * callback's {@code beforeSerialize} method when present.
	 *
	 * <p>The method also preserves the current include/exclude property settings
	 * on an internal stack and tracks the object in a stack used to determine
	 * reference context during serialization. The serialization depth counter
	 * is incremented.</p>
	 *
	 * @param oaObj the object about to be serialized
	 */
	void beforeSerialize(OAObject oaObj) {
		_beforeSerialize(oaObj, true, 0);
	}
	private void _beforeSerialize(OAObject oaObj, final boolean bCallOthers, final int cntx) {
		/* test        
		indent++;
		String msg = "";
		for (int i=0; i<indent; i++) msg += "  ";
		System.out.println(msg+""+oaObj.getClass()+" "+oaObj.getObjectKey().getGuid());
		 */
		if (bCallOthers) {
			totalObjectsWritten++;
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
			List<OAObjectSerializer> al = srvcOAThreadLocal.getObjectSerializers();
			if (al != null) {
				for (int i=al.size()-2; i >= 0; i--) {
					OAObjectSerializer os = al.get(i);
					if (os == this) continue;
		        	os._beforeSerialize(oaObj, false, cntx+1);
				}
			}
		}
		
		if (stackObject == null) stackObject = new Stack();
		if (stack == null) stack = new Stack();
		
		if (callback != null) {
			// save and push current settings into stack
			Tuple<String[], String[]> t = new Tuple<String[], String[]>(includeProps, excludeProps);
			stack.push(t);
			callback.beforeSerialize(oaObj);
		}

		// now save the obj in stack for further embeded objects to "see" where they are in the object tree.
		stackObject.push(oaObj);

		levelsDeep++;
	}

	/**
	 * Performs cleanup after an {@link OAObject} has been serialized. Delegates to
	 * any active wrapper serializer, invokes the callback's {@code afterSerialize}
	 * method when present, and restores include/exclude property settings from
	 * the internal stack.
	 *
	 * <p>The serialization depth counter is decremented and the object is removed
	 * from the internal stack.</p>
	 *
	 * @param obj the object that has just been serialized
	 */
	void afterSerialize(OAObject obj) {
		_afterSerialize(obj, true, 0);
	}
	private void _afterSerialize(OAObject obj, final boolean bCallOthers, final int cntx) {
		// indent--;
		if (bCallOthers) {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
			List<OAObjectSerializer> al = srvcOAThreadLocal.getObjectSerializers();
			if (al != null) {
				for (int i=al.size()-2; i >= 0; i--) {
					OAObjectSerializer os = al.get(i);
		        	os._afterSerialize(obj, false, cntx+1);
				}
			}
		}
        
		if (callback != null) {
			callback.afterSerialize(obj);
		}

		stackObject.pop();
		if (callback != null) {
			Tuple<String[], String[]> t = stack.pop();
			includeProps = t.a;
			excludeProps = t.b;
		}
		levelsDeep--;
	}

	/**
	 * Specifies the set of property names to include during serialization. When
	 * used, all other properties are excluded.
	 *
	 * @param props the property names to include
	 */
	protected void includeProperties(String[] props) {
		this.includeProps = props;
		this.excludeProps = null;
	}

	/**
	 * Specifies the set of property names to exclude during serialization. When
	 * used, all other properties are included.
	 *
	 * @param props the property names to exclude
	 */
	protected void excludeProperties(String[] props) {
		this.excludeProps = props;
		this.includeProps = null;
	}

	/**
	 * Configures serialization to include all properties. This is achieved by
	 * clearing any include list and setting the exclude list to an empty array.
	 */
	protected void includeAllProperties() {
		this.excludeProps = EmptyProperties;
		this.includeProps = null;
	}

	/**
	 * Configures serialization to exclude all properties. This is achieved by
	 * clearing any exclude list and setting the include list to an empty array.
	 */
	protected void excludeAllProperties() {
		this.includeProps = EmptyProperties;
		this.excludeProps = null;
	}

	/**
	 * Returns the number of objects currently stored in the internal serialization
	 * stack. This represents how many objects are in the active serialization path.
	 *
	 * @return the number of stacked objects
	 */
	protected int getStackSize() {
		if (stackObject == null) return 0;
		return stackObject.size();
	}

	/**
	 * Returns the previously serialized object from the internal stack. This is
	 * equivalent to requesting the stack object at position zero.
	 *
	 * @return the previous object on the serialization stack, or {@code null} if none
	 */
	protected Object getPreviousObject() {
		return getStackObject(0);
	}

	/**
	 * Returns an object from the serialization stack based on its relative position.
	 * Position {@code 0} corresponds to the most recently pushed object, with larger
	 * values referencing deeper stack entries.
	 *
	 * @param pos the relative stack index
	 * @return the object at the requested stack position, or {@code null} if out of range
	 */
	protected Object getStackObject(int pos) {
		if (stackObject == null) return null;
		int x = stackObject.size();
		x--;
		x -= pos;
		if (x < 0) {
			return null;
		}
		return stackObject.elementAt(x);
	}

	/**
	 * Returns the current serialization depth. The first serialized object is at
	 * level 0. This value is incremented in {@code beforeSerialize} and decremented
	 * in {@code afterSerialize}.
	 *
	 * @return the current serialization depth
	 */
	public int getLevelsDeep() {
		return levelsDeep;
	}

	/**
	 * Determines whether a reference property should be serialized. This overload
	 * delegates to the full version of {@code shouldSerializeReference} without
	 * providing link metadata.
	 *
	 * @param oaObj the owning object
	 * @param propertyName the name of the reference property
	 * @param obj the reference value
	 * @return {@code true} if the reference should be serialized, otherwise {@code false}
	 */
	protected boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj) {
		return shouldSerializeReference(oaObj, propertyName, obj, null);
	}

	/**
	 * Determines whether a reference should be serialized based on maximum limits,
	 * cache size rules, callback behavior, and overflow detection.
	 *
	 * <p>The method:</p>
	 * <ul>
	 *   <li>Checks cache-size constraints for MANY links.</li>
	 *   <li>Invokes callback rules when present.</li>
	 *   <li>Prevents serialization when the depth exceeds {@code overflowLimit},
	 *       instead adding the reference to an overflow list.</li>
	 * </ul>
	 *
	 * @param oaObj the object that owns the reference
	 * @param propertyName the property name being evaluated
	 * @param obj the reference value
	 * @param linkInfo optional link metadata for relationship evaluation
	 * @return {@code true} if the reference should be serialized, otherwise {@code false}
	 */
	protected boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj, OALinkInfo linkInfo) {
		boolean b = _shouldSerializeReference(oaObj, propertyName, obj);

		// 20141023 dont send more back then cache is setup for
		if (b && linkInfo != null && linkInfo.getType() == OALinkInfo.MANY) {
			int x = linkInfo.getCacheSize();
			if (x > 0) {
				if (hmLinkInfoCount == null) {
					hmLinkInfoCount = new HashMap<OALinkInfo, Integer>();
				}
				Object objx = hmLinkInfoCount.get(linkInfo);
				if (objx != null) {
					int x2 = ((Integer) objx).intValue();
					if (x2 > x) {
						return false;
					}
					hmLinkInfoCount.put(linkInfo, Integer.valueOf(x2 + 1));
				}
			}
		}
		if (callback != null) {
			b = callback.shouldSerializeReference(oaObj, propertyName, obj, b);
		}
		if (!b) {
			return false;
		}

		if (levelsDeep >= overflowLimit && obj != null) {
			Overflow overFlow = new Overflow();
			overFlow.parentObject = oaObj;
			overFlow.property = propertyName;
			overFlow.object = obj;
			if (stackObject != null) {
				overFlow.stack = (Stack) this.stackObject.clone();
			}
			overFlow.levelsDeep = this.levelsDeep;
			if (listOverflow == null) {
				listOverflow = new LinkedList<Overflow>();
			}
			listOverflow.add(overFlow);
			// LOG.finer("adding to overflow, levelsDeep="+levelsDeep+", object class="+oaObj.getClass().getName()+", property="+propertyName+", overFlowSize="+listOverflow.size());            
			return false;
		}
		return true;
	}

	private boolean bReachedMax;

	/**
	 * Indicates whether the serializer has reached its configured maximum limits.
	 * A limit is reached when either the compressed size exceeds {@code maxSize}
	 * or the expected number of serialized objects would exceed {@code maxObjects}.
	 *
	 * @return {@code true} if a maximum limit has been reached
	 */
	public boolean hasReachedMax() {
		if (bReachedMax) {
			return true;
		}
		if (maxSize > 0 && (getCompressedWritten() > maxSize)) {
			bReachedMax = true;
		} else if (maxObjects > 0 && ((totalObjectsWritten + minExpectedAmt) > maxObjects)) {
			bReachedMax = true;
		}
		return bReachedMax;
	}

	/**
	 * Core evaluator used to determine whether a reference should be serialized.
	 * This method applies size limits, object-count limits, excluded classes,
	 * and include/exclude property lists.
	 *
	 * @param oaObj the object owning the reference
	 * @param propertyName the reference property name
	 * @param reference the reference value being evaluated
	 * @return {@code true} if the reference should be serialized, otherwise {@code false}
	 */
	private boolean _shouldSerializeReference(OAObject oaObj, String propertyName, Object reference) {
		if (maxSize > 0) {
			if (getCompressedWritten() > maxSize) {
				return false;
			}
		}
		if (maxObjects > 0) {
			if ((totalObjectsWritten + minExpectedAmt) > maxObjects) {
				return false; // 20141119
			}
			if (reference instanceof Hub) {
				Hub h = (Hub) reference;
				if (totalObjectsWritten + minExpectedAmt + h.getSize() > maxObjects) {
					return false; // 20141119
				}
			}
		}
		if (parentWrapper != null) {
			return parentWrapper._shouldSerializeReference(oaObj, propertyName, reference);
		}

		if (excludedReferences != null && reference != null) {
			Class clazz;
			if (reference instanceof Hub) {
				clazz = ((Hub) reference).getObjectClass();
			} else {
				clazz = reference.getClass();
			}
			for (int i = 0; excludedReferences != null && i < excludedReferences.length; i++) {
				if (clazz.equals(excludedReferences[i])) {
					return false;
				}
			}
		}

		if (excludeProps != null) { // set when beforeSerialize() is called
			for (int i = 0; i < excludeProps.length; i++) {
				if (propertyName.equalsIgnoreCase(excludeProps[i])) {
					return false;
				}
			}
			return true;
		}
		if (includeProps != null) { // set when beforeSerialize() is called
			for (int i = 0; includeProps != null && i < includeProps.length; i++) {
				if (propertyName.equalsIgnoreCase(includeProps[i])) {
					return true;
				}
			}
			return false;
		}
		return true; // default, must be true
	}

	/**
	 * Writes this serializer wrapper to the output stream. Temporarily registers
	 * this instance as the active serializer in thread-local storage so that
	 * nested serialization uses the correct wrapper.
	 *
	 * @param stream the output stream used for serialization
	 * @throws IOException if the wrapper cannot be written
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        try {
            srvcOAThreadLocal.addObjectSerializer(this);
            _writeObject(stream);
        } 
        catch (Throwable e) {
            LOG.log(Level.WARNING, "OAObjectSerializer.writeObject exception", e);
            // note: this is ignored when invoked by RMI
            throw new IOException("OAObjectSerializer.writeObject exception", e);
        }
        finally {
            srvcOAThreadLocal.removeObjectSerializer(this);
        }
	}

	/**
	 * Returns the number of compressed bytes written by the active {@link Deflater}.
	 *
	 * @return the number of compressed bytes written, or {@code -1} if no deflater is active
	 */
	public long getCompressedWritten() {
		if (deflater == null) {
			return -1;
		}
		long x = deflater.getBytesWritten();
		return x;
	}

	private transient Deflater deflater;

	/**
	 * Writes the wrapped object and optional extra object to the output stream.
	 * Handles both compressed and uncompressed transmission modes, creating a
	 * secondary {@link RemoteObjectOutputStream} when compression is enabled.
	 *
	 * <p>The method writes wrapper metadata, serializes the main object and extra
	 * object when present, and delegates finalization to {@code finishWrite}. It
	 * records compression statistics and logs debug information when enabled.</p>
	 *
	 * @param stream the output stream receiving the serialized data
	 * @throws IOException if serialization fails
	 */
	private void _writeObject(ObjectOutputStream stream) throws IOException {
		long ts = System.currentTimeMillis();

		stream.writeInt(getId()); // 20171216
		stream.writeBoolean(bCompress);
		String msg;
		if (bCompress) {
			deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);//BEST_SPEED BEST_COMPRESSION);
			DeflaterOutputStream dos = new DeflaterOutputStream(stream, deflater, 1024 * 6);

			RemoteObjectOutputStream roos;
			boolean bStreamIsRoos = (stream instanceof RemoteObjectOutputStream);

			if (bStreamIsRoos) {
				roos = new RemoteObjectOutputStream(dos, (RemoteObjectOutputStream) stream);
			} else {
				roos = new RemoteObjectOutputStream(dos, null);
			}

			roos.writeBoolean(object != null);
			if (object != null) {
				roos.writeObject(object);
			}

			roos.writeBoolean(extraObject != null);
			if (extraObject != null) {
				roos.writeObject(extraObject);
			}

			finishWrite(roos);

			roos.flush();
			if (!bStreamIsRoos) {
				roos.close();
			}
			dos.finish();
			dos.flush();
			if (!bStreamIsRoos) {
				dos.close();
			}

			long sizeBefore = deflater.getBytesRead();
			long sizeAfter = deflater.getBytesWritten();
			deflater.end();

			long ts2 = System.currentTimeMillis();

			msg = String.format(
								"client=%d, id=%,d, class=%s, extra=%s, uncompressed=%,d, compressed=%,d, totalObjects=%,d, %,dms",
								clientId, id, 
								object == null ? "null" : object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								sizeBefore, sizeAfter, totalObjectsWritten, (ts2 - ts));
		} else {
			stream.writeBoolean(object != null);
			if (object != null) {
				stream.writeObject(object);
			}
			stream.writeBoolean(extraObject != null);
			if (extraObject != null) {
				stream.writeObject(extraObject);
			}
			finishWrite(stream);

			long ts2 = System.currentTimeMillis();
			msg = String.format(
								"client=%d, id=%,d, class=%s, extra=%s, totalObjects=%,d, %,dms",
								clientId, id, 
								object==null ? "null" : object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								totalObjectsWritten, (ts2 - ts));
		}
		stream.writeInt(totalObjectsWritten);

		/*
		if (totalObjectsWritten > 120000 || totalObjectsWritten < 0) {
		    msg = " ALERT, totalObjectsWritten is wrong";
		    LOG.warning(msg);
		}
		*/

		wcnter++;
		if (OAObject.getDebugMode()) {
	        LOG.finer(wcnter + ") " + msg);
	        OAPerformance.LOG.finer(wcnter + ") " + msg);
			// System.out.println("OAObjectSerializer.writeObject " + wcnter + ") " + msg);
		}
	}

	/**
	 * Writes any overflow entries accumulated during serialization. Overflow
	 * objects are serialized using new {@link OAObjectSerializer} wrappers that
	 * inherit stack and depth information from the recorded overflow data.
	 *
	 * <p>If no overflow objects exist, a {@code false} flag is written. Otherwise,
	 * the list of overflow descriptors is written followed by individual wrapper
	 * instances for each overflow object.</p>
	 *
	 * @param stream the current output stream
	 * @throws IOException if an overflow wrapper cannot be serialized
	 */
	private void finishWrite(ObjectOutputStream stream) throws IOException {
		if (listOverflow == null) {
			stream.writeBoolean(false);
			return;
		}
		LinkedList<Overflow> listOverflowHold = listOverflow;
		this.listOverflow = null;

		stream.writeBoolean(true);
		stream.writeObject(listOverflowHold);
		int cnt = 0;
		for (Overflow overFlow : listOverflowHold) {
			LOG.finer((++cnt) + ") writing overflow object=" + overFlow.object.getClass().getName());
			OAObjectSerializer wrapper = new OAObjectSerializer(overFlow.object, false); // compress must = false, since it is still using same stream
			wrapper.parentWrapper = this;
			wrapper.stackObject = overFlow.stack;
			wrapper.levelsDeep = overFlow.levelsDeep;
			wrapper.overflowLimit += wrapper.levelsDeep;
			stream.writeObject(wrapper);
		}
	}

	public static boolean bReadId = true; // 20171218, set to false to read older data  (ex: unit test binary file) 

	/**
	 * Reads this wrapper from the input stream, capturing the number of new and
	 * duplicate objects created during deserialization. Delegates object reading
	 * to {@code _readObject}, then updates counters based on global values in
	 * {@link OAObjectSerializeDelegate}.
	 *
	 * @param stream the input stream from which to read the wrapper
	 * @throws IOException if deserialization fails
	 * @throws ClassNotFoundException if an embedded object type is unknown
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		_readObject(stream);
	}

	// updated by OAObjectSerializeService
	public transient int newCount;
	public transient int dupCount;

	/**
	 * Reads the wrapped object and optional extra object from the stream. Handles
	 * both compressed and uncompressed modes and reconstructs objects using a
	 * corresponding {@link RemoteObjectInputStream} when necessary.
	 *
	 * <p>The method then calls {@code finishRead} to process any overflow entries
	 * and reads the final object count. Debug information is logged when enabled.</p>
	 *
	 * @param stream the stream supplying the serialized wrapper data
	 * @throws IOException if the wrapper cannot be read
	 * @throws ClassNotFoundException if a referenced class cannot be resolved
	 */
	private void _readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		long ts = System.currentTimeMillis();

		if (bReadId) {
			this.id = stream.readInt(); // 20171216
		}
		bCompress = stream.readBoolean();
		String msg;
		if (bCompress) {
			Inflater inflater = new Inflater(true);
			InflaterInputStream iis = new InflaterInputStream(stream, inflater, 1024 * 6);

			RemoteObjectInputStream rois;
			if (stream instanceof RemoteObjectInputStream) {
				rois = new RemoteObjectInputStream(iis, (RemoteObjectInputStream) stream);
			} else {
				rois = new RemoteObjectInputStream(iis, null);
			}

			boolean b = rois.readBoolean();
			if (b) {
				object = rois.readObject();
			}
			b = rois.readBoolean();
			if (b) {
				extraObject = rois.readObject();
			}
			finishRead(rois);

			//ois.close();  dont call this, it WILL affect the stream
			// iis.close();// ?? not sure
			totalObjectsWritten = stream.readInt();

			long sizeBefore = inflater.getBytesRead();
			long sizeAfter = inflater.getBytesWritten();

			long ts2 = System.currentTimeMillis();

			msg = String.format("id=%,d, class=%s, extra=%s, compressed=%,d, uncompressed=%,d, totalObjects=%,d, %,dms",
								id, 
								object == null ? "null" : object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								sizeBefore, sizeAfter, totalObjectsWritten, (ts2 - ts));
		} else {
			boolean b = stream.readBoolean();
			if (b) {
				object = stream.readObject();
			}
			b = stream.readBoolean();
			if (b) {
				extraObject = stream.readObject();
			}
			finishRead(stream);
			totalObjectsWritten = stream.readInt();

			long ts2 = System.currentTimeMillis();
			msg = String.format("id=%,d, class=%s, extra=%s, totalObjects=%,d, %,dms",
								id, 
								object == null ? "null" : object.getClass().getSimpleName(),
								extraObject == null ? "null" : extraObject.getClass().getSimpleName(),
								totalObjectsWritten, (ts2 - ts));
		}

		if (totalObjectsWritten > 100000 || totalObjectsWritten < 0) {
			msg += String.format(" totalObjectsRead=%,d",  totalObjectsWritten);
			// LOG.warning(rcnter+") "+msg);
		}

		rcnter++;
		if (OAObject.getDebugMode()) {
	        LOG.finer(rcnter + ") " + msg);
	        OAPerformance.LOG.finer(rcnter + ") " + msg);
			// System.out.println("OAObjectSerializer.readObject " + rcnter + ") " + msg);
		}
	}

	/**
	 * Processes overflow entries that were written as part of the wrapper. After
	 * reading the overflow list, each overflow wrapper is deserialized and its
	 * resolved object is assigned to the corresponding property of the parent
	 * object recorded in the overflow descriptor.
	 *
	 * @param stream the input stream providing overflow data
	 * @throws IOException if the overflow list or wrapper cannot be read
	 * @throws ClassNotFoundException if an overflow object type is unknown
	 */
	private void finishRead(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		if (!stream.readBoolean()) {
			return; //
		}

		listOverflow = (LinkedList) stream.readObject();
		int cnt = 0;
		for (Overflow overFlow : listOverflow) {
			OAObjectSerializer wrap = (OAObjectSerializer) stream.readObject();
			LOG.finer((++cnt) + ") read overflow object=" + wrap.object.getClass().getName());
			overFlow.parentObject.setProperty(overFlow.property, wrap.object);
		}
	}

	/**
	 * Returns the wrapped primary object. If this serializer is part of an overflow
	 * chain, the request is delegated to the parent wrapper to obtain the root
	 * object.
	 *
	 * <p>If the resolved object is an {@link IODummy}, a runtime exception is
	 * thrown to indicate that the underlying type could not be reconstructed.</p>
	 *
	 * @return the wrapped primary object
	 */
	public TYPE getObject() {
		Object objx;
		if (parentWrapper != null) {
			objx = parentWrapper.getObject();
		} else {
			objx = object;
		}

		if (objx instanceof IODummy) {
			throw new RuntimeException("Object was not able to be read, class not found");
		}

		return (TYPE) objx;
	}

	/**
	 * Returns the extra wrapped object, or delegates to the parent wrapper if this
	 * serializer is part of an overflow chain.
	 *
	 * @return the extra object associated with this wrapper, or the parent's value
	 */
	public Object getExtraObject() {
		if (parentWrapper != null) {
			return parentWrapper.getExtraObject();
		}
		return extraObject;
	}

	/**
	 * Sets the additional object to be serialized along with the primary object.
	 *
	 * @param extraObject the secondary object to serialize
	 */
	public void setExtraObject(Object extraObject) {
		this.extraObject = extraObject;
	}

	/**
	 * Assigns the callback used to control serialization behavior. The callback is
	 * also given a reference to this serializer instance.
	 *
	 * @param callback the callback to use for serialization decisions
	 */
	public void setCallback(OAObjectSerializerCallback callback) {
		this.callback = callback;
		callback.setOAObjectSerializer(this);
	}
	
	/**
	 * Returns the callback assigned to this serializer.
	 *
	 * @return the active serializer callback, or {@code null} if none is set
	 */
	public OAObjectSerializerCallback getCallback() {
	    return this.callback;
	}

	public static final class FriendAccess {
		private FriendAccess() {
		}
		public void beforeSerialize(OAObject obj, OAObjectSerializer os) {
			os.beforeSerialize(obj);
		}
		public void afterSerialize(OAObject obj, OAObjectSerializer os) {
			os.afterSerialize(obj);
		}
		public boolean shouldSerializeReference(OAObjectSerializer os, OAObject oaObj, String propertyName, Object obj, OALinkInfo linkInfo) {
			return os.shouldSerializeReference(oaObj, propertyName, obj, linkInfo);
		}

	}
	
	private final static FriendAccess friendAccess = new FriendAccess(); 
	static FriendAccess getFriendAccess() {
		return friendAccess;
	}
}

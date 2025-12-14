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
package com.viaoa.remote.info;

import java.lang.annotation.Annotation;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Logger;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;

/**
 * Holds runtime binding information for a remote object participating in OA’s
 * multiplexer-based remoting system. One side (client or server) contains the
 * actual implementation instance, while the other side holds a proxy. All
 * remote references are passed using an internal bind-name, which is mapped
 * back to the real or proxy object through this class.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Maintain a {@link WeakReference} to the actual object so it can be
 *       garbage-collected automatically.</li>
 *   <li>Track whether the object supports broadcast, asynchronous queues, or
 *       OA-sync behavior.</li>
 *   <li>Scan the interface class and build {@link MethodInfo} metadata for each
 *       remotely accessible method.</li>
 *   <li>Resolve remote return types and remote parameters based on
 *       {@code @OARemoteInterface}, {@code @OARemoteMethod}, and
 *       {@code @OARemoteParameter} annotations.</li>
 *   <li>Generate a stable “method signature” for overloaded methods.</li>
 *   <li>Detect invalid remote return/parameter declarations (non-interfaces)
 *       and log warnings.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * {@code BindInfo} objects are created internally by OA's multiplexer server
 * and client runtime. They are never instantiated by application code.
 * Remote calls obtain the {@link MethodInfo} for a method through this object,
 * and weak-reference behavior ensures unused remote bindings do not leak memory.
 *
 * <h2>GC Awareness</h2>
 * If the underlying object has been garbage-collected, the class logs a warning
 * and returns {@code null}. This allows the remoting layer to gracefully handle
 * stale bind references.
 *
 * @author vvia
 */
public class BindInfo {
	private static Logger LOG = Logger.getLogger(BindInfo.class.getName());

	/**
	 * Internal numeric identifier used for referencing this bound object within
	 * the remoting subsystem.
	 */
	public short id;

	/**
	 * The internal bind-name used to represent this remote object in place of the
	 * actual instance during remote communication.
	 */
	public String name;

	/**
	 * Indicates whether the bound object supports broadcast-style remote method
	 * invocation.
	 */
	public boolean isBroadcast;

	/**
	 * True if the bound object uses an asynchronous queue for remote method
	 * processing.
	 */
	public boolean usesQueue;
	
	/**
	 * Name of the asynchronous queue used for remote method dispatch, or null if
	 * queue-based dispatching is not used.
	 */
	public String asyncQueueName;
	
	/**
	 * Size of the asynchronous queue used when queue-based remote dispatch is
	 * enabled.
	 */
	public int asyncQueueSize;
	
	/**
	 * Indicates whether the interface for this bound object is annotated as
	 * supporting OA-Sync semantics according to {@code @OARemoteInterface}.
	 */
	public boolean isOASync;

	/**
	 * Weak reference to the actual implementation object. Allows the object to be
	 * garbage-collected when no longer in use.
	 */
	public WeakReference weakRef;
	
	/**
	 * Interface class used to generate the remote proxy for this binding.
	 */
	public Class interfaceClass; // used to create the proxy

	/**
	 * Maps generated method-signature strings to their associated {@link MethodInfo}
	 * metadata.
	 */
	private HashMap<String, MethodInfo> hmNameToMethod;
	
	/**
	 * Maps {@link Method} instances to their corresponding {@link MethodInfo}
	 * metadata.
	 */
	private HashMap<Method, MethodInfo> hmMethod;

	/**
	 * Constructs a new {@code BindInfo} record for a remote object. The supplied
	 * object is wrapped in a weak reference, and interface/queue metadata are
	 * initialized. The {@code @OARemoteInterface} annotation on the interface
	 * class is scanned to determine OA-Sync behavior.
	 *
	 * @param name internal bind-name for the remote object
	 * @param obj the actual implementation instance, or null
	 * @param interfaceClass interface used to create the remote proxy
	 * @param referenceQueue optional reference queue for GC notifications
	 * @param bIsBroadcast true if broadcast behavior is enabled
	 * @param queueName asynchronous queue name, or null
	 * @param queueSize maximum queue size
	 */
	public BindInfo(String name, Object obj, Class interfaceClass, ReferenceQueue referenceQueue, boolean bIsBroadcast, String queueName,
			int queueSize) {
		this.name = name;
		if (obj != null) {
			setObject(obj, referenceQueue);
		}
		this.interfaceClass = interfaceClass;
		this.isBroadcast = bIsBroadcast;
		this.asyncQueueName = queueName;
		this.asyncQueueSize = queueSize;
		this.usesQueue = (asyncQueueName != null);

		OARemoteInterface rc = (OARemoteInterface) interfaceClass.getAnnotation(OARemoteInterface.class);
		if (rc != null) {
			this.isOASync = rc.isOASync();
		}
	}

	/**
	 * Assigns the underlying implementation object for this binding, wrapping it
	 * in a {@link WeakReference}. If a reference queue is supplied, the weak
	 * reference is registered with it.
	 *
	 * @param obj the object to bind
	 * @param referenceQueue optional reference queue used to track garbage-collection
	 */
	public void setObject(Object obj, ReferenceQueue referenceQueue) {
		if (referenceQueue == null) {
			weakRef = new WeakReference<Object>(obj);
		} else {
			weakRef = new WeakReference<Object>(obj, referenceQueue);
		}
	}

	/**
	 * Tracks whether the underlying object has already been detected as
	 * garbage-collected. Used to prevent repeated warning messages.
	 */
	private boolean bObjectGCd;

	/**
	 * Returns the underlying implementation object, or null if it has been
	 * garbage-collected. Logs a warning the first time the reference is found to
	 * be cleared.
	 *
	 * @return the bound implementation object, or null if GC’d
	 */
	public Object getObject() {
		if (weakRef != null) {
			Object obj = weakRef.get();
			if (obj == null && !bObjectGCd) {
				bObjectGCd = true;
				LOG.warning("object has been GCd, name=" + name);
			}
			return obj;
		}
		return null;
	}

	/**
	 * Retrieves the {@link MethodInfo} associated with the given generated method
	 * signature. Initializes method metadata on first use.
	 *
	 * @param methodNameSig generated method signature
	 * @return the associated MethodInfo, or null if not found
	 */
	public MethodInfo getMethodInfo(String methodNameSig) {
		if (hmNameToMethod == null) {
			loadMethodInfo();
		}
		MethodInfo mi = hmNameToMethod.get(methodNameSig);
		return mi;
	}

	/**
	 * Retrieves the {@link MethodInfo} associated with the given {@link Method}
	 * instance. Initializes method metadata on first use.
	 *
	 * @param method the reflected Java method
	 * @return the corresponding MethodInfo, or null if none exists
	 */
	public MethodInfo getMethodInfo(Method method) {
		if (hmMethod == null) {
			loadMethodInfo();
		}
		MethodInfo mi = hmMethod.get(method);
		return mi;
	}

	/**
	 * Initializes metadata for all remotely accessible methods defined in the
	 * interface class. This includes generating method signatures, detecting
	 * remote return types, evaluating {@code @OARemoteMethod} and
	 * {@code @OARemoteParameter} annotations, and populating lookup maps.
	 */
	public synchronized void loadMethodInfo() {
		if (interfaceClass == null) {
			return;
		}
		hmNameToMethod = new HashMap<String, MethodInfo>(23, .75f);
		hmMethod = new HashMap<Method, MethodInfo>();

		/*
		RemoteInterface remoteInterface = (RemoteInterface) interfaceClass.getAnnotation(RemoteInterface.class);
		if (remoteInterface != null) {
		}
		*/

		Method[] methods = interfaceClass.getMethods();
		for (Method method : methods) {
			int sig = 0; // create a dummy signature, to recognize method overloading
			Class[] cs = method.getParameterTypes();
			for (int j = 0; cs != null && j < cs.length; j++) {
				sig *= 10;
				sig += (cs[j].getName().hashCode() % 500);
			}
			MethodInfo mi = new MethodInfo();
			mi.method = method;
			mi.methodNameSignature = method.getName() + sig;

			boolean bRemote = false;
			Class c = method.getReturnType();
			if (c != null && !c.isPrimitive()) {
				OARemoteInterface rc = (OARemoteInterface) c.getAnnotation(OARemoteInterface.class);
				bRemote = (rc != null);
			}
			if (bRemote) {
				mi.remoteReturn = c;
				if (!c.isInterface()) {
					Class[] csx = c.getInterfaces();
					Class cx;
					if (csx != null && csx.length > 0) {
						cx = csx[0];
					} else {
						cx = c;
					}
					String s = "bindName=" + name + ", method=" + method;
					s += ", will use interface=" + cx;
					LOG.warning("return value must be a Java Interface, since returnValueIsRemote() is true, " + s);
					mi.remoteReturn = cx;
				}
			}

			OARemoteMethod remoteMethod = method.getAnnotation(OARemoteMethod.class);
			if (remoteMethod != null) {
				if (remoteMethod.compressedReturnValue()) {
					if (mi.remoteReturn == null) {
						mi.compressedReturn = true;
					}
				}
				mi.noReturnValue = remoteMethod.noReturnValue();
				mi.timeoutSeconds = Math.max(0, remoteMethod.timeoutSeconds());
				mi.dontUseQueue = remoteMethod.dontUseQueue();
				mi.dontUseQueueForReturnValue = remoteMethod.dontUseQueueForReturnValue();
				mi.returnOnQueueSocket = remoteMethod.returnOnQueueSocket();
                mi.runInRemoteThread = remoteMethod.runInRemoteThread();
			}

			// check to see if any of the params are remote
			cs = method.getParameterTypes();
			Annotation[][] anns = method.getParameterAnnotations();

			int x = cs == null ? 0 : cs.length;

			for (int i = 0; i < x; i++) {
				boolean bCompressed = false;
				boolean bDontUseQue = false;

				OARemoteInterface rc = (OARemoteInterface) cs[i].getAnnotation(OARemoteInterface.class);
				bRemote = (rc != null) && !cs[i].isPrimitive();

				if (anns[i] != null && anns[i].length > 0) {
					OARemoteParameter rp = (OARemoteParameter) (anns[i][0]);
					if (rp != null) {
						bCompressed = rp.compressed();
						bDontUseQue = rp.dontUseQueue();
					}
				}
				if (bCompressed) {
					if (mi.compressedParams == null) {
						mi.compressedParams = new boolean[cs.length];
					}
					mi.compressedParams[i] = true;
				}
				if (bDontUseQue) {
					if (mi.dontUseQueues == null) {
						mi.dontUseQueues = new boolean[cs.length];
					}
					mi.dontUseQueues[i] = true;
				}

				if (bRemote) {
					if (mi.remoteParams == null) {
						mi.remoteParams = new Class[cs.length];
					}
					c = cs[i];
					mi.remoteParams[i] = c;
					if (!c.isInterface()) {
						Class[] csx = c.getInterfaces();
						Class cx;
						if (csx != null && csx.length > 0) {
							cx = csx[0];
						} else {
							cx = c;
						}
						String s = "bindName=" + name + ", method=" + method;
						s += ", param#" + i;
						s += ", will use interface=" + cx;
						// callback must be defined as an Interface
						LOG.warning("method " + method.getName()
								+ " has a param annotated as remote, that is invalid - the param must be an Interface, " + s);
						mi.remoteParams[i] = c;
					}
				}
			}
			hmMethod.put(method, mi);
			hmNameToMethod.put(mi.methodNameSignature, mi);
		}
	}
}

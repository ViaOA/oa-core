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
package com.viaoa.sync.remote;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.sync.model.ClientInfo;

/**
 * Base class for server-side implementations of {@link RemoteServerInterface}.
 * <p>
 * Each {@code OASyncServer} hosts one instance of a concrete subclass of
 * {@code RemoteServerImpl}. It exposes operations that clients perform on the
 * authoritative server-side model, including:
 * <ul>
 *   <li>retrieving objects from cache or datasource,</li>
 *   <li>saving objects with specific cascade rules,</li>
 *   <li>executing remote methods on OAObjects or Hubs,</li>
 *   <li>issuing GUID sequences,</li>
 *   <li>constructing {@code RemoteClientInterface} and
 *       {@code RemoteSessionInterface} implementations for each client.</li>
 * </ul>
 *
 * <h2>Remote Method Invocation</h2>
 * The {@code runRemoteMethod(...)} variants:
 * <ul>
 *   <li>locate the target object or static hub method,</li>
 *   <li>resolve matching methods using {@link OAObjectInfo},</li>
 *   <li>invoke via reflection,</li>
 *   <li>wrap and propagate exceptions back to clients.</li>
 * </ul>
 *
 * <h2>Thread-Local Behavior</h2>
 * Saves and restores the "send messages" state so that remote changes produce
 * appropriate sync events without interfering with normal server behavior.
 *
 * <h2>Diagnostics</h2>
 * {@link #performThreadDump(String)} captures and logs full JVM stack traces
 * to help diagnose hung or misbehaving client calls.
 *
 * <p>
 * This class sits at the top of the client–server RPC bridge for OA.
 */
public abstract class RemoteServerImpl implements RemoteServerInterface {
	private static Logger LOG = Logger.getLogger(RemoteServerImpl.class.getName());

	public RemoteServerImpl() {
	}
	
	
	/**
	 * Echoes a ping message.
	 *
	 * @param msg the message to echo
	 * @return the same message that was received
	 */
	@Override
	public String ping(String msg) {
		return msg;
	}

	/**
	 * Receives a ping message with no return value.
	 *
	 * @param msg the ping message
	 */
	@Override
	public void ping2(String msg) {
	}

	/**
	 * Returns a display name for this remote server.
	 *
	 * @return a display string identifying the server
	 */
	@Override
	public String getDisplayMessage() {
		return "OASyncServer";
	}

	/**
	 * Saves an object on the server using the specified cascade rule.
	 * <p>
	 * Temporarily enables message sending so that save operations generate
	 * appropriate sync events.
	 * </p>
	 *
	 * @param objectClass the class of the object to save
	 * @param objectKey the key identifying the object
	 * @param iCascadeRule the cascade rule to apply during save
	 * @return {@code true} if the object was found and saved, otherwise {@code false}
	 */
	@Override
	public boolean save(Class objectClass, OAObjectKey objectKey, int iCascadeRule) {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objectClass);
    	
		boolean bResult;
		try {
			srvcOARemoteThread.sendMessages(true);
			OAObject obj = (OAObject) og.objectsInternal().callObjectCacheGetObject(objectClass, objectKey);
			if (obj != null) {
				obj.save(iCascadeRule);
				bResult = true;
			} else {
				bResult = false;
			}
		}
		finally {
			srvcOARemoteThread.sendMessages(false);
		}
		return bResult;
	}

	/**
	 * Returns the next block of object GUIDs.
	 *
	 * @return the starting GUID for the next block of fifty GUIDs
	 */
	/*qqqqqqqqqqqq
	@Override
	public long getNextFiftyObjectGuids() {
		return OAObjectDelegate.getNextFiftyGuids(packageThis);
	}
	*/

	/**
	 * Retrieves an object by key from cache or datasource.
	 *
	 * @param objectClass the class of the object
	 * @param objectKey the key identifying the object
	 * @return the resolved object, or {@code null} if not found
	 */
	@Override
	public OAObject getObject(Class objectClass, OAObjectKey objectKey) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.objectsInternal().callObjectCacheGetObject(objectClass, objectKey);
		if (obj == null) {
			if (og.syncInternal().isServer()) {
				obj = (OAObject) OADataSource.getObject(objectClass, objectKey);
			}
		}
		return obj;
	}

	@Override
	public OAObject getObjectUsingPkey(Class objectClass, OAObjectKey objectKey) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objectClass);
		OAObject obj = (OAObject) og.objectsInternal().callObjectCacheGetObject(objectClass, objectKey.getObjectIds());
		if (obj == null) {
			if (og.syncInternal().isServer()) {
				obj = (OAObject) OADataSource.getObject(objectClass, objectKey);
			}
		}
		return obj;
	}
	
	/**
	 * Invokes an instance method on a server-side object using reflection.
	 *
	 * @param clazz the class of the target object
	 * @param objKey the key identifying the target object
	 * @param methodName the name of the method to invoke
	 * @param args arguments to pass to the method
	 * @return the result returned by the invoked method
	 * @throws RuntimeException if the object or method cannot be found, or invocation fails
	 */
	@Override
	public Object runRemoteMethod(Class clazz, OAObjectKey objKey, String methodName, Object[] args) {
		Object obj = getObject(clazz, objKey);
		if (obj == null) {
			throw new RuntimeException("Object could not be found, class=" + clazz + ", objKey=" + objKey);
		}
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(clazz);

		int x = 0;
		if (args != null && args.length > 0) {
			x += args.length;
		}
		Method method = og.objectsInternal().callObjectInfoGetMethod(oi, methodName, x);

		if (method == null) {
			throw new RuntimeException("method " + methodName + " not found in class " + clazz.getSimpleName());
		}
		Object objResult = null;
		try {
			objResult = method.invoke(obj, args);
		} catch (Exception e) {
			throw new RuntimeException("exception calling method=" + methodName + ", class=" + clazz.getSimpleName(), e);
		}
		return objResult;
	}

	

	/**
	 * Invokes an instance method on a provided server-side object using reflection.
	 *
	 * @param obj the target object
	 * @param methodName the name of the method to invoke
	 * @param args arguments to pass to the method
	 * @return the result returned by the invoked method
	 * @throws RuntimeException if the method cannot be found or invocation fails
	 */
    @Override
    public Object runRemoteMethod2(OAObject obj, String methodName, Object[] args) {
        Class clazz = obj.getClass();
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
        OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(clazz);

        int x = 0;
        if (args != null && args.length > 0) {
            x += args.length;
        }
        Method method = og.objectsInternal().callObjectInfoGetMethod(oi, methodName, x);

        if (method == null) {
            throw new RuntimeException("method " + methodName + " not found in class " + clazz.getSimpleName());
        }
        Object objResult = null;
        try {
            objResult = method.invoke(obj, args);
        } catch (Exception e) {
            throw new RuntimeException("exception calling method=" + methodName + ", class=" + clazz.getSimpleName(), e);
        }
        return objResult;
    }
	
	
    /**
     * Invokes a static hub-based method using reflection.
     *
     * @param hub the hub passed as the first argument to the static method
     * @param methodName the name of the method to invoke
     * @param args additional arguments to pass to the method
     * @return the result returned by the invoked method
     * @throws RuntimeException if the method cannot be found or invocation fails
     */
	@Override
	public Object runRemoteMethod(Hub hub, String methodName, Object[] args) {
		if (hub == null) {
			return null;
		}
		Class clazz = hub.getObjectClass();
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetObjectInfo(clazz);

		int x = 1;
		if (args != null && args.length > 0) {
			x += args.length;
		}
		Method method = og.objectsInternal().callObjectInfoGetMethod(oi, methodName, x);

		if (method == null) {
			throw new RuntimeException("method " + methodName + " not found in class " + clazz.getSimpleName());
		}
		Object objResult = null;
		try {
			Object[] objs = new Object[x];
			objs[0] = hub;
			if (x > 1) {
				System.arraycopy(args, 0, objs, 1, x - 1);
			}
			objResult = method.invoke(null, objs);
		} catch (Exception e) {
			throw new RuntimeException("exception calling method=" + methodName + ", class=" + clazz.getSimpleName() + ", hub=" + hub, e);
		}
		return objResult;
	}

	/**
	 * Creates or retrieves a {@link RemoteClientInterface} for the specified client.
	 *
	 * @param clientInfo information describing the client
	 * @return a remote client interface instance for the client
	 */
	@Override
	public abstract RemoteClientInterface getRemoteClient(ClientInfo clientInfo);

	/**
	 * Creates or retrieves a {@link RemoteSessionInterface} for the specified client.
	 *
	 * @param clientInfo information describing the client
	 * @param callback callback interface implemented by the client
	 * @return a remote session interface instance for the client
	 */
	@Override
	public abstract RemoteSessionInterface getRemoteSession(ClientInfo clientInfo, RemoteClientCallbackInterface callback);

	/**
	 * Captures and logs a full thread dump of the JVM.
	 *
	 * @param msg a message to prefix the thread dump
	 * @return the captured thread dump as a string
	 */
	@Override
	public String performThreadDump(String msg) {
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
		String s = srvcOAThreadLocal.getAllStackTraces();
		LOG.warning(msg + "\n" + s);
		return s;
	}

}

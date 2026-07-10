package com.viaoa.oa.api.internal;

import com.viaoa.cascade.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;

/**
 * Internal synchronization and remoting operation boundary for OA runtime services.
 */
public interface SyncInternalOps {

	/**
	 * Returns the current OA sync client, when running in client mode.
	 *
	 * @return the sync client, or {@code null}
	 */
	public OASyncClient getClient();
	/**
	 * Returns the current OA sync server, when running in server mode.
	 *
	 * @return the sync server, or {@code null}
	 */
	public OASyncServer getServer();
	
	/**
	 * Returns whether sync/remoting is currently connected.
	 *
	 * @return {@code true} if connected
	 */
	public boolean isConnected();
	/**
	 * Returns the current sync connection id.
	 *
	 * @return the connection id
	 */
	public int getConnectionId();
	
	/**
	 * Sends an exception through the sync/remoting channel.
	 *
	 * @param msg the exception message
	 * @param ex the exception to send
	 */
	public void sendException(String msg, Throwable ex);
	
	/**
	 * Returns information for the current sync client.
	 *
	 * @return the client information
	 */
	public ClientInfo getClientInfo();
	/**
	 * Updates sync client information.
	 *
	 * @param ci the client information to publish
	 */
	public void updateClientInfo(ClientInfo ci);

	/**
	 * Saves cached objects using the supplied cascade context and rule.
	 *
	 * @param cascade the cascade context
	 * @param iCascadeRule the cascade rule
	 */
	public void saveCache( OACascade cascade, int iCascadeRule);

	/**
	 * Performs distributed garbage-collection maintenance for sync/remoting.
	 */
	public void performDGC();
	
	/**
	 * Returns whether the runtime is operating as a sync server.
	 *
	 * @return {@code true} if server-side
	 */
	public boolean isServer();
	/**
	 * Returns whether the runtime is operating as a sync client.
	 *
	 * @return {@code true} if client-side
	 */
	public boolean isClient();
	/**
	 * Returns whether the runtime is operating without client/server sync.
	 *
	 * @return {@code true} if single-user
	 */
	public boolean isSingleUser();
	
	/**
	 * Requests a remote client refresh for an object key.
	 *
	 * @param class1 the object class
	 * @param objectKey the object key
	 */
	public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey);
	/**
	 * Requests a remote client refresh for a linked object property.
	 *
	 * @param class1 the object class
	 * @param objectKey the object key
	 * @param linkPropertyName the link property name
	 */
	public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey, String linkPropertyName);

	/**
	 * Returns the remote client interface.
	 *
	 * @return the remote client interface
	 */
	public RemoteClientInterface getRemoteClient();

	/**
	 * Returns the remote server interface.
	 *
	 * @return the remote server interface
	 */
	public RemoteServerInterface getRemoteServer();
	
	/**
	 * Returns the connection id for the current sync request.
	 *
	 * @return the request connection id
	 */
	public int getRequestConnectionId();
	
}

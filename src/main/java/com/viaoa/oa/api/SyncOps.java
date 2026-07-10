package com.viaoa.oa.api;

import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncServer;

/**
 * Real-time synchronization operations for an OA runtime.
 * <p>
 * {@code SyncOps} manages the runtime layer that allows OA model state to
 * participate in real-time distributed behavior across connected runtimes.
 * This is typically used for client-to-server synchronization.
 * <p>
 * Through synchronization, changes to objects and Hubs are propagated so that
 * connected runtimes can stay coordinated without manual refresh logic.
 * <p>
 * Synchronization is role-based. A runtime can be configured as either a server
 * or a client using {@link #createServer(int)} or
 * {@link #createClient(String, int)}, and then started using {@link #start()}.
 * For persisted master/client convergence, see {@link ReplicationOps}.
 */
public interface SyncOps {

	 /**
	  * Configures this runtime to operate as a synchronization server.
	  * <p>
	  * This defines the synchronization role as server but does not start it.
	  * Call {@link #start()} to begin accepting client connections.
	  *
	  * @param port the port used to accept client connections
	  */
	 void createServer(int port);

	 /**
	  * Configures this runtime using an existing synchronization server.
	  *
	  * @param ss the synchronization server to use
	  */
	 void createServer(OASyncServer ss);
	 
	 /**
	  * Configures this runtime to operate as a synchronization client.
	  * <p>
	  * This defines the synchronization role as client but does not start it.
	  * Call {@link #start()} to initiate the connection to the server.
	  *
	  * @param hostName the server host name or address
	  * @param serverPort the server port to connect to
	  */
	void createClient(String hostName, int serverPort);
	
	/**
	 * Configures this runtime using an existing synchronization client.
	 *
	 * @param sc the synchronization client to use
	 */
	void createClient(OASyncClient sc);
	
	/**
	 * Starts real-time synchronization for the configured role.
	 * <p>
	 * If configured as a server, this begins accepting client connections. If
	 * configured as a client, this connects to the server and begins participating
	 * in synchronized model-state coordination.
	 *
	 * @throws Exception if synchronization cannot be started
	 */
	void start() throws Exception;
	
	/**
	 * Stops real-time synchronization for this runtime.
	 * <p>
	 * If operating as a server, this stops accepting client connections. If
	 * operating as a client, this disconnects from the server.
	 *
	 * @throws Exception if synchronization cannot be stopped
	 */	
	void stop() throws Exception;

	/**
	 * Returns whether the runtime is operating without sync client/server mode.
	 *
	 * @return {@code true} if single-user mode is active
	 */
	boolean isSingleUser();
	
	/**
	 * Returns whether this runtime is configured as a synchronization server.
	 *
	 * @return {@code true} if configured as a server
	 */
	boolean isServer();
	
	/**
	 * Returns whether this runtime is configured as a synchronization client.
	 *
	 * @return {@code true} if configured as a client
	 */
	boolean isClient();
	
	/**
	 * Returns whether real-time synchronization is currently active.
	 *
	 * @return {@code true} if synchronization is running
	 */
	boolean isRunning();
	
	/**
	 * Returns the current synchronization connection id.
	 *
	 * @return the connection id
	 */
	public int getConnectionId();
}


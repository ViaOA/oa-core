package com.viaoa.graph.api;



/*qqqqqqq
CODEX



*/



/**
 * Real-time synchronization operations for an {@link OAGraph}.
 * <p>
 * {@code SyncOps} manages the runtime layer that allows an Object Graph to
 * participate in real-time distributed behavior across connected runtimes.
 * This is typically used for client-to-server synchronization.
 * <p>
 * Through synchronization, changes to objects and Hubs are propagated
 * immediately so that graph state remains consistent across all connected
 * runtimes without requiring manual coordination.
 * <p>
 * Synchronization is role-based. A graph can be configured as either a server
 * or a client using {@link #createServer(int)} or
 * {@link #createClient(String, int)}, and then started using {@link #start()}.
 * <p>
 * This represents real-time coordination of the Object Graph. For eventual
 * consistency with offline support and server-to-server convergence, see
 * {@link ReplOps}.
 */
public interface SyncOps {

	 /**
	  * Configures this graph to operate as a synchronization server.
	  * <p>
	  * {@code createServer(...)} sets up the graph to accept connections from
	  * synchronization clients and coordinate real-time graph state across them.
	  * <p>
	  * This defines the synchronization role as server but does not start it.
	  * Call {@link #start()} to begin accepting client connections.
	  * <p>
	  * This method may only be called when the graph is not already configured
	  * or running. Calling it after {@link #createClient(String, int)} or while
	  * running will result in an error.
	  *
	  * @param port the port used to accept client connections
	  */
	 void createServer(int port);

	 /**
	  * Configures this graph to operate as a synchronization client.
	  * <p>
	  * {@code createClient(...)} sets up the graph to connect to a synchronization
	  * server and participate in real-time graph state coordination.
	  * <p>
	  * This defines the synchronization role as client but does not start it.
	  * Call {@link #start()} to initiate the connection to the server.
	  * <p>
	  * This method may only be called when the graph is not already configured
	  * or running. Calling it after {@link #createServer(int)} or while running
	  * will result in an error.
	  *
	  * @param hostName the server host name or address
	  * @param serverPort the server port to connect to
	  */
	void createClient(String hostName, int serverPort);
	
	/**
	 * Starts real-time synchronization for the configured role.
	 * <p>
	 * {@code start()} activates the synchronization layer based on the configured
	 * role. If configured as a server, it begins accepting client connections.
	 * If configured as a client, it connects to the server and begins
	 * participating in real-time graph coordination.
	 * <p>
	 * Synchronization propagates changes to objects and Hubs immediately across
	 * connected runtimes so that graph state remains consistent.
	 * <p>
	 * This method may only be called after either {@link #createServer(int)} or
	 * {@link #createClient(String, int)} has been called. Calling it without a
	 * configured role or while already running will result in an error.
	 */
	void start() throws Exception;
	
	/**
	 * Stops real-time synchronization for this graph.
	 * <p>
	 * {@code stop()} deactivates the synchronization layer and terminates any
	 * active connections or coordination with other runtimes.
	 * <p>
	 * If operating as a server, it stops accepting client connections. If operating
	 * as a client, it disconnects from the server.
	 * <p>
	 * After stopping, the graph remains configured for its role and may be started
	 * again using {@link #start()}.
	 */	
	void stop() throws Exception;

	boolean isSingleUser();
	
	/**
	 * Returns whether this graph is configured as a synchronization server.
	 * <p>
	 * {@code isServer()} indicates the configured synchronization role, not
	 * whether synchronization is currently running. Use {@link #isRunning()}
	 * to determine if synchronization is active.
	 *
	 * @return {@code true} if configured as a server; otherwise {@code false}
	 */
	boolean isServer();
	
	/**
	 * Returns whether this graph is configured as a synchronization server.
	 * <p>
	 * {@code isServer()} indicates the configured synchronization role, not
	 * whether synchronization is currently running. Use {@link #isRunning()}
	 * to determine if synchronization is active.
	 *
	 * @return {@code true} if configured as a server; otherwise {@code false}
	 */
	boolean isClient();
	
	/**
	 * Returns whether real-time synchronization is currently active.
	 * <p>
	 * {@code isRunning()} indicates whether the synchronization layer has been
	 * started and is actively coordinating graph state across connected runtimes.
	 * <p>
	 * This is independent of the configured role. Use {@link #isServer()} or
	 * {@link #isClient()} to determine the synchronization role.
	 *
	 * @return {@code true} if synchronization is running; otherwise {@code false}
	 */
	boolean isRunning();
}


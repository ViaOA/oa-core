package com.viaoa.oa.api.internal;

/**
 * Top-level internal OA runtime facade exposed through {@code OA.internal()}.
 */
public interface InternalOps {

	/**
	 * Returns the internal OAObject operation families.
	 *
	 * @return the object operations facade
	 */
	public ObjectsOps objects();

	/**
	 * Returns the internal Hub operation families.
	 *
	 * @return the Hub operations facade
	 */
	public HubsOps hubs();

	/**
	 * Returns internal synchronization and remoting operations.
	 *
	 * @return the sync operations facade
	 */
	public SyncInternalOps sync();

	/**
	 * Returns internal replication operations.
	 *
	 * @return the replication operations facade
	 */
	public ReplicationInternalOps replication();
	
	/**
	 * Returns internal trigger operations.
	 *
	 * @return the trigger operations facade
	 */
	public TriggersOps triggers();
	
}

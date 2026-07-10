package com.viaoa.oa.internal.facade;

import com.viaoa.oa.api.internal.InternalOps;
import com.viaoa.oa.api.internal.HubsOps;
import com.viaoa.oa.api.internal.ObjectsOps;
import com.viaoa.oa.api.internal.ReplicationInternalOps;
import com.viaoa.oa.api.internal.SyncInternalOps;
import com.viaoa.oa.api.internal.TriggersOps;

/**
 * Top-level internal facade implementation exposed through {@code OA.internal()}.
 */
public class InternalOpsImpl implements InternalOps {
	
	private HubsOps hubs;
	private ObjectsOps objects;
	private TriggersOps triggers;
	private SyncInternalOps opsSync;
	private ReplicationInternalOps opsReplication;
	
	/**
	 * Creates the top-level internal facade from already constructed operation families.
	 *
	 * @param hubs the Hub operations facade
	 * @param objects the object operations facade
	 * @param triggers the trigger operations facade
	 * @param opsSync the sync operations facade
	 * @param opsReplication the replication operations facade
	 */
	public InternalOpsImpl(HubsOps hubs, ObjectsOps objects, TriggersOps triggers, SyncInternalOps opsSync, ReplicationInternalOps opsReplication) {
		this.hubs = hubs;
		this.objects = objects;
		this.triggers = triggers;
		this.opsSync = opsSync;
		this.opsReplication = opsReplication;
	}

	@Override
	/**
	 * Returns the internal OAObject operation families.
	 *
	 * @return the object operations facade
	 */
	public ObjectsOps objects() {
		return objects;
	}

	@Override
	/**
	 * Returns the internal Hub operation families.
	 *
	 * @return the Hub operations facade
	 */
	public HubsOps hubs() {
		return hubs;
	}

	@Override
	/**
	 * Returns the internal trigger operations facade.
	 *
	 * @return the trigger operations facade
	 */
	public TriggersOps triggers() {
		return triggers;
	}
	
	@Override
	/**
	 * Returns the internal synchronization operations facade.
	 *
	 * @return the sync operations facade
	 */
	public SyncInternalOps sync() {
		return opsSync;
	}

	@Override
	/**
	 * Returns the internal replication operations facade.
	 *
	 * @return the replication operations facade
	 */
	public ReplicationInternalOps replication() {
		return opsReplication;
	}
}

package com.viaoa.oa.service.facade;

import com.viaoa.oa.api.services.ServicesOps;
import com.viaoa.oa.api.services.HubsOps;
import com.viaoa.oa.api.services.ObjectsOps;
import com.viaoa.oa.api.services.RulesOps;
import com.viaoa.oa.api.services.TriggersOps;

/**
 * Default implementation of the top-level {@link ServicesOps} facade.
 * <p>
 * This class groups the curated OA service nouns exposed through
 * {@code oa.services()} and returns the object, Hub, trigger, and rules
 * operation facades supplied by the owning OA runtime.
 * </p>
 */
public class ServicesOpsImpl implements ServicesOps {
	
	private HubsOps hubs;
	private ObjectsOps objects;
	private TriggersOps triggers;
	private RulesOps rules;
	
	/**
	 * Creates the service facade from the supplied operation groups.
	 *
	 * @param hubs Hub service operations
	 * @param objects OAObject service operations
	 * @param triggers trigger service operations
	 * @param rules rule-evaluation service operations
	 */
	public ServicesOpsImpl(HubsOps hubs, ObjectsOps objects, TriggersOps triggers, RulesOps rules) {
		this.hubs = hubs;
		this.objects = objects;
		this.triggers = triggers;
		this.rules = rules;
	}

	/**
	 * Returns OAObject-oriented public service operations.
	 *
	 * @return object operations facade
	 */
	@Override
	public ObjectsOps objects() {
		return objects;
	}

	/**
	 * Returns Hub-oriented public service operations.
	 *
	 * @return Hub operations facade
	 */
	@Override
	public HubsOps hubs() {
		return hubs;
	}
	
	/**
	 * Returns trigger registration operations.
	 *
	 * @return trigger operations facade
	 */
	@Override
	public TriggersOps triggers() {
		return triggers;
	}

	/**
	 * Returns boolean rule-evaluation operations backed by the OA rules engine.
	 *
	 * @return rules operations facade
	 */
	@Override
	public RulesOps rules() {
		return rules;
	}
}


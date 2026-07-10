package com.viaoa.oa.api.services;

/**
 * Top-level public OA services facade exposed through {@code OA.services()}.
 * <p>
 * This interface groups curated service APIs for application and advanced OA
 * callers. Implementation-level operations remain behind {@code OA.internal()}.
 */
public interface ServicesOps {

	/**
	 * Returns public OAObject service families.
	 *
	 * @return the object services facade
	 */
	public ObjectsOps objects();

	/**
	 * Returns public Hub service families.
	 *
	 * @return the Hub services facade
	 */
	public HubsOps hubs();
	
	/**
	 * Returns public trigger services.
	 *
	 * @return the trigger services facade
	 */
	public TriggersOps triggers();
	
	
	/**
	 * Returns public OA rule services.
	 *
	 * @return the rules services facade
	 */
	public RulesOps rules();
	
}

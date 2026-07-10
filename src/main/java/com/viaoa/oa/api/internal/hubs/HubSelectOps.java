package com.viaoa.oa.api.internal.hubs;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

/**
 * Internal select/query operations for loading and refreshing Hub contents.
 */
public interface HubSelectOps {

	/**
	 * Returns the select object for a Hub, optionally creating it.
	 *
	 * @param hub the Hub to inspect
	 * @param bCreateIfNull {@code true} to create a select if missing
	 * @return the select object
	 */
	public <T extends OAObject> OASelect<T> getSelect(Hub<T> hub, boolean bCreateIfNull);
	/**
	 * Loads all data for a Hub select.
	 *
	 * @param hub the Hub to load
	 */
	public void loadAllData(Hub<?> hub);
	/**
	 * Cancels active select processing for a Hub.
	 *
	 * @param hub the Hub to update
	 * @param bRemoveSelect {@code true} to remove the select object
	 */
	public void cancelSelect(Hub<?> hub, boolean bRemoveSelect);
	/**
	 * Returns whether more selected data is available for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if more data can be loaded
	 */
	public boolean isMoreData(Hub<?> hub);
	/**
	 * Sets the select where clause for a Hub.
	 *
	 * @param hub the Hub to update
	 * @param whereClause the where clause
	 */
	public void setSelectWhere(Hub<?> hub, String whereClause);
	/**
	 * Returns the select where clause for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the where clause
	 */
	public String getSelectWhere(Hub<?> hub);
	/**
	 * Sets the select order clause for a Hub.
	 *
	 * @param hub the Hub to update
	 * @param orderClause the order clause
	 */
	public void setSelectOrder(Hub<?> hub, String orderClause);
	/**
	 * Sets the Hub used to constrain select results.
	 *
	 * @param hub the Hub to update
	 * @param hubSelect the where-Hub constraint
	 */
	public <T extends OAObject> void setSelectWhereHub(Hub<T> hub, Hub<T> hubSelect);
	/**
	 * Sets the property path from a Hub to its where-Hub constraint.
	 *
	 * @param hub the Hub to update
	 * @param ppFromHub the property path from the Hub
	 */
	public void setSelectWhereHubPath(Hub<?> hub, String ppFromHub);
	/**
	 * Returns the select order clause for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the order clause
	 */
	public String getSelectOrder(Hub<?> hub);
	/**
	 * Selects Hub data using a where object, clause, parameters, and ordering.
	 *
	 * @param hub the Hub to load
	 * @param whereObject optional where object
	 * @param whereClause optional where clause
	 * @param whereParams optional where parameters
	 * @param orderByClause optional order clause
	 * @param bAppendFlag {@code true} to append results
	 */
	public void select(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderByClause, boolean bAppendFlag);
	/**
	 * Selects Hub data using the Hub select configuration.
	 *
	 * @param hub the Hub to load
	 * @param bAppendFlag {@code true} to append results
	 */
	public void select(Hub<?> hub, boolean bAppendFlag);
	/**
	 * Selects Hub data using query options and an additional filter.
	 *
	 * @param hub the Hub to load
	 * @param whereObject optional where object
	 * @param whereClause optional where clause
	 * @param whereParams optional where parameters
	 * @param orderBy optional order clause
	 * @param bAppendFlag {@code true} to append results
	 * @param filter optional result filter
	 */
	public <T extends OAObject> void select(Hub<T> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter<T> filter);
	/**
	 * Selects Hub data using a supplied OASelect.
	 *
	 * @param hub the Hub to load
	 * @param select the select to execute
	 */
	public <T extends OAObject> void select(Hub<T> hub, OASelect<T> select);
	/**
	 * Selects Hub data using pass-through where and order clauses.
	 *
	 * @param hub the Hub to load
	 * @param whereClause the pass-through where clause
	 * @param orderClause the pass-through order clause
	 */
	public void selectPassthru(Hub<?> hub, String whereClause, String orderClause);
	/**
	 * Returns the select object for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the select object, or {@code null}
	 */
	public <T extends OAObject> OASelect<T> getSelect(Hub<T> hub);
	/**
	 * Refreshes selected Hub contents.
	 *
	 * @param hub the Hub to refresh
	 */
	public void refresh(Hub<?> hub);
	/**
	 * Returns the Hub used to constrain select results.
	 *
	 * @param hub the Hub to inspect
	 * @return the where-Hub constraint
	 */
	public <T extends OAObject> Hub<T> getSelectWhereHub(Hub<T> hub);
	/**
	 * Returns the property path to the where-Hub constraint.
	 *
	 * @param hub the Hub to inspect
	 * @return the property path
	 */
	public String getSelectWhereHubPath(Hub<?> hub);
	/**
	 * Adopts where-Hub selection configuration from another Hub.
	 *
	 * @param thisHub the Hub receiving configuration
	 * @param propName the property name used for adoption
	 * @param hubFrom the source Hub
	 * @return {@code true} if configuration was adopted
	 */
	public boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom);
}

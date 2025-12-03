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
package com.viaoa.hub;

import java.util.EventListener;

/**
 * Core listener interface for receiving {@link HubEvent} notifications from
 * {@link Hub}, {@link com.viaoa.object.OAObject}, and the OA object cache.
 * <p>
 * Covers the full event lifecycle (property changes, add/insert/remove/move,
 * new-list/after-new-list, AO changes, select/sort/load/refresh, save/delete).
 * Includes default hook methods for allow/isValid gating so listeners can
 * participate in enablement/validation without overriding every method.
 * <p>
 * Listener ordering can be controlled via {@link InsertLocation} and the
 * {@link #setLocation(InsertLocation)} / {@link #getLocation()} contract.
 */
public interface HubListener<T> extends EventListener {

	/**
	 * Called before a property is changed on a Hub or OAObject.
	 *
	 * @param e the event describing the pending property change
	 */
	public void beforePropertyChange(HubEvent<T> e);

	/**
	 * Called after a property has changed on a Hub or OAObject.
	 *
	 * @param e the event describing the completed property change
	 */
	public void afterPropertyChange(HubEvent<T> e);

	/**
	 * Called before an object is inserted into the Hub.
	 *
	 * @param e the event describing the pending insert
	 */
	public void beforeInsert(HubEvent<T> e);

	/**
	 * Called after an object has been inserted into the Hub.
	 *
	 * @param e the event describing the completed insert
	 */
	public void afterInsert(HubEvent<T> e);

	/**
	 * Called before an object is added to the Hub.
	 *
	 * @param e the event describing the pending add
	 */
	public void beforeAdd(HubEvent<T> e);

	/**
	 * Determines whether enabled state is allowed for the current event.
	 *
	 * @param e             the event being evaluated
	 * @param bCurrentValue current enabled state
	 * @return the enabled state to use
	 */
	public default boolean getAllowEnabled(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Determines whether visible state is allowed for the current event.
	 *
	 * @param e             the event being evaluated
	 * @param bCurrentValue current visibility state
	 * @return the visibility state to use
	 */
	public default boolean getAllowVisible(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Determines whether add operations are allowed for the current event.
	 *
	 * @param e             the event being evaluated
	 * @param bCurrentValue current add-allowed state
	 * @return the add-allowed state to use
	 */
	public default boolean getAllowAdd(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Determines whether remove operations are permitted.
	 *
	 * @param e             the event being evaluated
	 * @param bCurrentValue current remove-allowed state
	 * @return the remove-allowed state to use
	 */
	public default boolean getAllowRemove(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Determines whether remove-all operations are permitted.
	 *
	 * @param e             the event being evaluated
	 * @param bCurrentValue current remove-all allowed state
	 * @return the remove-all allowed state
	 */
	public default boolean getAllowRemoveAll(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Determines whether delete operations are permitted.
	 *
	 * @param e             the event being evaluated
	 * @param bCurrentValue current delete-allowed state
	 * @return the delete-allowed state to use
	 */
	public default boolean getAllowDelete(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Validates whether a pending property change is acceptable.
	 *
	 * @param e             the event describing the change
	 * @param bCurrentValue current validation state
	 * @return true if the property change is valid; otherwise false
	 */
	public default boolean isValidPropertyChange(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Validates whether an add operation is acceptable.
	 *
	 * @param e             the event describing the add
	 * @param bCurrentValue current validation state
	 * @return true if the add is valid; otherwise false
	 */
	public default boolean isValidAdd(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Validates whether a remove operation is acceptable.
	 *
	 * @param e             the event describing the removal
	 * @param bCurrentValue current validation state
	 * @return true if the removal is valid; otherwise false
	 */
	public default boolean isValidRemove(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Validates whether removing all objects is acceptable.
	 *
	 * @param e             the event describing the remove-all action
	 * @param bCurrentValue current validation state
	 * @return true if the action is valid; otherwise false
	 */
	public default boolean isValidRemoveAll(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Determines whether a delete operation is valid.
	 *
	 * @param e             the event describing the delete
	 * @param bCurrentValue current validation state
	 * @return true if deletion is valid; otherwise false
	 */
	public default boolean isValidDelete(HubEvent<T> e, boolean bCurrentValue) {
		return bCurrentValue;
	}

	/**
	 * Called after an object is added to the Hub.
	 *
	 * @param e the event describing the completed add
	 */
	public void afterAdd(HubEvent<T> e);

	/**
	 * Called before an object is removed from the Hub.
	 *
	 * @param e the event describing the pending removal
	 */
	public void beforeRemove(HubEvent<T> e);

	/**
	 * Called after an object has been removed from the Hub.
	 *
	 * @param e the event describing the completed removal
	 */
	public void afterRemove(HubEvent<T> e);

	/**
	 * Called before all objects are removed from the Hub.
	 *
	 * @param e the event describing the pending remove-all operation
	 */
	public void beforeRemoveAll(HubEvent<T> e);

	/**
	 * Called after all objects have been removed or cleared from the Hub.
	 *
	 * @param e the event describing completion of the remove-all action
	 */
	public void afterRemoveAll(HubEvent<T> e);

	/**
	 * Called before an object is moved within the Hub.
	 *
	 * @param e the event describing the pending move
	 */
	public void beforeMove(HubEvent<T> e);

	/**
	 * Called after an object has been moved within the Hub.
	 *
	 * @param e the event describing the completed move
	 */
	public void afterMove(HubEvent<T> e);

	/**
	 * Called after the active object of the Hub has been changed.
	 *
	 * @param e the event describing the active-object change
	 */
	public void afterChangeActiveObject(HubEvent<T> e);

	/**
	 * Called when the Hub receives a completely new list of objects,
	 * such as after a refresh or load operation.
	 *
	 * @param e the event describing the new list
	 */
	public void onNewList(HubEvent<T> e);

	/**
	 * Called after the onNewList event has been processed.
	 *
	 * @param e the event describing the post-new-list state
	 */
	public void afterNewList(HubEvent<T> e);

	/**
	 * Called before an OAObject save() operation occurs.
	 *
	 * @param e the event signaling a pending save
	 */
	public void beforeSave(HubEvent<T> e);

	/**
	 * Called after an OAObject save() operation has completed.
	 *
	 * @param e the event describing the completed save
	 */
	public void afterSave(HubEvent<T> e);

	/**
	 * Called before an OAObject delete() operation occurs.
	 *
	 * @param e the event describing the pending delete
	 */
	public void beforeDelete(HubEvent<T> e);

	/**
	 * Called after an OAObject delete() operation has completed.
	 *
	 * @param e the event describing the completed delete
	 */
	public void afterDelete(HubEvent<T> e);

	/**
	 * Called before a select() operation is performed on the Hub.
	 *
	 * @param e the event describing the pending select
	 */
	public void beforeSelect(HubEvent<T> e);

	/**
	 * Called after a sort() operation is completed on the Hub.
	 *
	 * @param e the event describing the completed sort
	 */
	public void afterSort(HubEvent<T> e);

	/**
	 * Defines where a listener should be inserted within the Hub's listener list.
	 *
	 * FIRST – listener is placed at the beginning  
	 * NEXT  – listener is placed after the current position  
	 * LAST  – listener is placed at the end
	 */
	public enum InsertLocation {
		FIRST, NEXT, LAST;
	}

	/**
	 * Sets the position where this listener should be inserted in the listener list.
	 *
	 * @param pos the insertion location
	 */
	public void setLocation(InsertLocation pos);

	/**
	 * Returns the configured insertion location for this listener.
	 *
	 * @return the listener's insertion position
	 */
	public InsertLocation getLocation();

	/**
	 * Called after an object or list has been loaded into the Hub.
	 *
	 * @param e the event describing the load completion
	 */
	public void afterLoad(HubEvent<T> e);

	/**
	 * Called before a refresh() operation begins on the Hub.
	 *
	 * @param e the event describing the pending refresh
	 */
	public void beforeRefresh(HubEvent<T> e);
}

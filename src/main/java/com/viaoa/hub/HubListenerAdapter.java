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

import com.viaoa.object.OAObject;

/**
 * No-op adapter for {@link HubListener}, allowing implementers to override only
 * the callbacks they need. Supports optional metadata (listener object, name,
 * description) and an insertion preference ({@link InsertLocation}).
 * <p>
 * All methods default to empty implementations; use this in UI/controllers to
 * avoid boilerplate when subscribing to a subset of events.
 */
public class HubListenerAdapter<T extends OAObject> implements HubListener<T> {

	/**
	 * Optional reference to an associated listener or owner object.
	 */
	private Object listener;
	
	/**
	 * Optional metadata fields providing a name and description for the listener.
	 */
	private String name, description;

	/**
	 * Creates a HubListenerAdapter with no associated listener, name, or description.
	 */
	public HubListenerAdapter() {

	}

	/**
	 * Creates a HubListenerAdapter with the specified listener, name, and description.
	 *
	 * @param listener    an associated owner or listener object
	 * @param name        descriptive name for this adapter
	 * @param description additional text describing this adapter
	 */
	public HubListenerAdapter(Object listener, String name, String description) {
		this.listener = listener;
		this.name = name;
		this.description = description;
	}

	/**
	 * Creates a HubListenerAdapter with the specified listener and name.
	 *
	 * @param listener an associated listener or owner object
	 * @param name     descriptive name for this adapter
	 */
	public HubListenerAdapter(Object listener, String name) {
		this.listener = listener;
		this.name = name;
	}

	/**
	 * Creates a HubListenerAdapter with the specified associated listener object.
	 *
	 * @param listener the owner or listener object
	 */
	public HubListenerAdapter(Object listener) {
		this.listener = listener;
	}

	/**
	 * No-op implementation of afterChangeActiveObject.
	 *
	 * @param e the event describing the active-object change
	 */
	@Override
	public void afterChangeActiveObject(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before a property change occurs.
	 *
	 * @param e the event describing the pending property change
	 */
	@Override
	public void beforePropertyChange(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after a property change has occurred.
	 *
	 * @param e the event describing the completed property change
	 */
	@Override
	public void afterPropertyChange(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before an insert operation.
	 *
	 * @param e the event describing the pending insert
	 */
	@Override
	public void beforeInsert(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after an insert operation completes.
	 *
	 * @param e the event describing the completed insert
	 */
	@Override
	public void afterInsert(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before an object is moved within the Hub.
	 *
	 * @param e the event describing the pending move
	 */
	@Override
	public void beforeMove(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after an object has been moved within the Hub.
	 *
	 * @param e the event describing the completed move
	 */
	@Override
	public void afterMove(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before an object is added to the Hub.
	 *
	 * @param e the event describing the pending add
	 */
	@Override
	public void beforeAdd(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after an object has been added to the Hub.
	 *
	 * @param e the event describing the completed add
	 */
	@Override
	public void afterAdd(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before an object is removed from the Hub.
	 *
	 * @param e the event describing the pending removal
	 */
	@Override
	public void beforeRemove(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after an object has been removed from the Hub.
	 *
	 * @param e the event describing the completed removal
	 */
	@Override
	public void afterRemove(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before all objects are removed from the Hub.
	 *
	 * @param e the event describing the pending remove-all action
	 */
	@Override
	public void beforeRemoveAll(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after all objects have been removed from the Hub.
	 *
	 * @param e the event describing the completed remove-all action
	 */
	@Override
	public void afterRemoveAll(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before an OAObject save() occurs.
	 *
	 * @param e the event describing the pending save
	 */
	@Override
	public void beforeSave(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after an OAObject save() has completed.
	 *
	 * @param e the event describing the completed save
	 */
	@Override
	public void afterSave(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before an OAObject delete() occurs.
	 *
	 * @param e the event describing the pending delete
	 */
	@Override
	public void beforeDelete(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after an OAObject delete() operation completes.
	 *
	 * @param e the event describing the completed delete
	 */
	@Override
	public void afterDelete(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called before a Hub select() operation is performed.
	 *
	 * @param e the event describing the pending select
	 */
	@Override
	public void beforeSelect(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after a Hub sort() operation completes.
	 *
	 * @param e the event describing the completed sort
	 */
	@Override
	public void afterSort(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called when a Hub receives a new list of objects.
	 *
	 * @param e the event describing the new list
	 */
	@Override
	public void onNewList(HubEvent<T> e) {
	}

	/**
	 * No-op implementation called after a Hub processes an onNewList event.
	 *
	 * @param e the event describing the post-new-list state
	 */
	@Override
	public void afterNewList(HubEvent<T> e) {
	}

	/**
	 * Stores the preferred listener insertion location within the Hub's listener list.
	 */
	private InsertLocation insertWhere;

	/**
	 * Sets the preferred insertion location for this listener in the listener list.
	 *
	 * @param pos the desired insertion location
	 */
	@Override
	public void setLocation(InsertLocation pos) {
		this.insertWhere = pos;
	}

	/**
	 * Returns the preferred listener insertion location for this listener.
	 *
	 * @return the configured insertion location
	 */
	@Override
	public InsertLocation getLocation() {
		return this.insertWhere;
	}

	/**
	 * No-op implementation called after an object or list has been loaded into the Hub.
	 *
	 * @param e the event describing the load completion
	 */
	@Override
	public void afterLoad(HubEvent<T> e) {
	}

	/**
	 * Returns the associated listener or owner object, if any.
	 *
	 * @return the linked listener object, or null
	 */
	public Object getListener() {
		return listener;
	}

	/**
	 * Returns the descriptive name assigned to this adapter.
	 *
	 * @return the listener's name, or null
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the descriptive text associated with this adapter.
	 *
	 * @return the listener's description, or null
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * No-op implementation called before a Hub refresh() operation is performed.
	 *
	 * @param e the event describing the pending refresh
	 */
	@Override
	public void beforeRefresh(HubEvent<T> e) {
	}
}

/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.hub;

/**
 * Adapter class that implements HubListener Interface.
 * <p>
 *
 * @see HubListener
 */
public class HubListenerAdapter<T> implements HubListener<T> {

	private Object listener;
	private String name, description;

	public HubListenerAdapter() {

	}

	public HubListenerAdapter(Object listener, String name, String description) {
		this.listener = listener;
		this.name = name;
		this.description = description;
	}

	public HubListenerAdapter(Object listener, String name) {
		this.listener = listener;
		this.name = name;
	}

	public HubListenerAdapter(Object listener) {
		this.listener = listener;
	}

	@Override
	public void afterChangeActiveObject(HubEvent<T> e) {
	}

	@Override
	public void beforePropertyChange(HubEvent<T> e) {
	}

	@Override
	public void afterPropertyChange(HubEvent<T> e) {
	}

	@Override
	public void beforeInsert(HubEvent<T> e) {
	}

	@Override
	public void afterInsert(HubEvent<T> e) {
	}

	@Override
	public void beforeMove(HubEvent<T> e) {
	}

	@Override
	public void afterMove(HubEvent<T> e) {
	}

	@Override
	public void beforeAdd(HubEvent<T> e) {
	}

	@Override
	public void afterAdd(HubEvent<T> e) {
	}

	@Override
	public void beforeRemove(HubEvent<T> e) {
	}

	@Override
	public void afterRemove(HubEvent<T> e) {
	}

	@Override
	public void beforeRemoveAll(HubEvent<T> e) {
	}

	@Override
	public void afterRemoveAll(HubEvent<T> e) {
	}

	@Override
	public void beforeSave(HubEvent<T> e) {
	}

	@Override
	public void afterSave(HubEvent<T> e) {
	}

	@Override
	public void beforeDelete(HubEvent<T> e) {
	}

	@Override
	public void afterDelete(HubEvent<T> e) {
	}

	@Override
	public void beforeSelect(HubEvent<T> e) {
	}

	@Override
	public void afterSort(HubEvent<T> e) {
	}

	@Override
	public void onNewList(HubEvent<T> e) {
	}

	@Override
	public void afterNewList(HubEvent<T> e) {
	}

	private InsertLocation insertWhere;

	@Override
	public void setLocation(InsertLocation pos) {
		this.insertWhere = pos;
	}

	@Override
	public InsertLocation getLocation() {
		return this.insertWhere;
	}

	@Override
	public void afterLoad(HubEvent<T> e) {
	}

	public Object getListener() {
		return listener;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public void beforeRefresh(HubEvent<T> e) {
	}
}

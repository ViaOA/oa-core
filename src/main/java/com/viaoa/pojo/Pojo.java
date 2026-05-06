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
package com.viaoa.pojo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Root of the POJO mapping graph for a single OAObject type.
 * <p>
 * A {@code Pojo} instance describes:
 * <ul>
 *   <li>the OAObject name, and</li>
 *   <li>all regular scalar properties ({@link PojoRegularProperty}),</li>
 *   <li>all link properties ({@link PojoLink}) which in turn reference
 *       {@link PojoLinkOne} / {@link PojoLinkMany} structures.</li>
 * </ul>
 * This metadata is generated from {@link com.viaoa.metadata.OAObjectInfo}
 * by {@link OAObjectPojoLoader} and is later used by the JSON/Jackson
 * integration to map between flat JSON POJOs and live {@code OAObject}
 * graphs during import/export.
 */
public class Pojo implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The name of the OAObject model represented by this POJO definition.
	 * <p>
	 * Typically matches {@link com.viaoa.metadata.OAObjectInfo#getName()}.
	 */
	protected volatile String name;

	// References to other objects
	// PojoLinks
	/**
	 * Thread-safe list of {@link PojoLink} entries defining link properties
	 * (one-to-one and one-to-many mappings) for the POJO model.
	 */
	protected volatile CopyOnWriteArrayList<PojoLink> alPojoLinks = new CopyOnWriteArrayList<>();

	// PojoRegularProperties
	/**
	 * Thread-safe list of scalar {@link PojoRegularProperty} definitions for the
	 * POJO model.
	 */
	protected volatile CopyOnWriteArrayList<PojoRegularProperty> alPojoRegularProperties = new CopyOnWriteArrayList<>();

	/**
	 * Creates an empty {@code Pojo} metadata instance.
	 */
	public Pojo() {
	}

	/**
	 * Returns the name of the OAObject type represented by this POJO metadata.
	 *
	 * @return the model name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the OAObject model name for this POJO definition.
	 *
	 * @param newValue the new name value
	 */
	public void setName(String newValue) {
		this.name = newValue;
	}

	/**
	 * Returns the list of link definitions ({@link PojoLink}) associated with
	 * this POJO metadata.
	 *
	 * @return list of link mappings
	 */
	public CopyOnWriteArrayList<PojoLink> getPojoLinks() {
		return alPojoLinks;
	}

	/**
	 * Replaces the current list of link mappings.
	 * <p>
	 * If {@code list} is {@code null}, the internal list is cleared; otherwise,
	 * it is replaced with a thread-safe copy.
	 *
	 * @param list the new list of {@link PojoLink} entries, or null to clear
	 */
	public void setPojoLinks(List<PojoLink> list) {
		if (list == null) {
			this.alPojoLinks.clear();
		} else {
			this.alPojoLinks = new CopyOnWriteArrayList<>(list);
		}
	}

	/**
	 * Returns the list of scalar property definitions for the POJO model.
	 *
	 * @return list of {@link PojoRegularProperty} entries
	 */
	public CopyOnWriteArrayList<PojoRegularProperty> getPojoRegularProperties() {
		return alPojoRegularProperties;
	}

	/**
	 * Replaces the current list of scalar POJO properties.
	 * <p>
	 * If {@code list} is {@code null}, the internal list is cleared. Otherwise,
	 * the list is wrapped in a {@link CopyOnWriteArrayList}.
	 *
	 * @param list the new list of regular properties, or null to clear
	 */
	public void setPojoRegularProperties(List<PojoRegularProperty> list) {
		if (list == null) {
			this.alPojoRegularProperties.clear();
		} else {
			this.alPojoRegularProperties = new CopyOnWriteArrayList<>(list);
		}
	}

	/**
	 * Returns a simple string representation including the POJO's model name.
	 *
	 * @return formatted string representation of this POJO metadata
	 */
	@Override
	public String toString() {
		return "Pojo [" +
				"name=" + name +
				"]";
	}
}

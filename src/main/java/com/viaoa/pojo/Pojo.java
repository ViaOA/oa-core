/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
 * This metadata is generated from {@link com.viaoa.object.OAObjectInfo}
 * by {@link OAObjectPojoLoader} and is later used by the JSON/Jackson
 * integration to map between flat JSON POJOs and live {@code OAObject}
 * graphs during import/export.
 */
public class Pojo implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;

	// References to other objects
	// PojoLinks
	protected volatile CopyOnWriteArrayList<PojoLink> alPojoLinks = new CopyOnWriteArrayList<>();
	// PojoRegularProperties
	protected volatile CopyOnWriteArrayList<PojoRegularProperty> alPojoRegularProperties = new CopyOnWriteArrayList<>();

	public Pojo() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	public CopyOnWriteArrayList<PojoLink> getPojoLinks() {
		return alPojoLinks;
	}

	public void setPojoLinks(List<PojoLink> list) {
		if (list == null) {
			this.alPojoLinks.clear();
		} else {
			this.alPojoLinks = new CopyOnWriteArrayList<>(list);
		}
	}

	public CopyOnWriteArrayList<PojoRegularProperty> getPojoRegularProperties() {
		return alPojoRegularProperties;
	}

	public void setPojoRegularProperties(List<PojoRegularProperty> list) {
		if (list == null) {
			this.alPojoRegularProperties.clear();
		} else {
			this.alPojoRegularProperties = new CopyOnWriteArrayList<>(list);
		}
	}

	@Override
	public String toString() {
		return "Pojo [" +
				"name=" + name +
				"]";
	}
}

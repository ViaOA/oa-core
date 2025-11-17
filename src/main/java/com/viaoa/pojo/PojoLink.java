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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Describes a single link property on a {@link Pojo}.
 * <p>
 * A {@code PojoLink} is the root for either:
 * <ul>
 *   <li>a {@link PojoLinkOne} (for {@code TYPE_ONE} associations), or</li>
 *   <li>a {@link PojoLinkMany} (for {@code TYPE_MANY} associations).</li>
 * </ul>
 * The name corresponds to the OA link name on the source {@code OAObject}
 * type. The {@link OAObjectPojoLoader} populates this structure when it
 * converts {@link com.viaoa.object.OAObjectInfo} metadata into POJO
 * descriptors.
 */
public class PojoLink implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	protected volatile String name;

	// References to other objects
	// Pojo
	protected volatile Pojo pojo;
	// PojoLinkMany
	protected volatile PojoLinkMany pojoLinkMany;
	// PojoLinkOne
	protected volatile PojoLinkOne pojoLinkOne;

	public PojoLink() {
	}

	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		this.name = newValue;
	}

	@JsonIgnore
	public Pojo getPojo() {
		return pojo;
	}

	public void setPojo(Pojo newValue) {
		this.pojo = newValue;
	}

	//@JsonIgnore
	public PojoLinkMany getPojoLinkMany() {
		return pojoLinkMany;
	}

	public void setPojoLinkMany(PojoLinkMany newValue) {
		this.pojoLinkMany = newValue;
	}

	//@JsonIgnore
	public PojoLinkOne getPojoLinkOne() {
		return pojoLinkOne;
	}

	public void setPojoLinkOne(PojoLinkOne newValue) {
		this.pojoLinkOne = newValue;
	}

	@Override
	public String toString() {
		return "PojoLink [" +
				"name=" + name +
				"]";
	}
}

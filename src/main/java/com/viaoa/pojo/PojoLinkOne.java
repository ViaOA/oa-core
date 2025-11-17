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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Describes the "one" side of a link property in the POJO metadata model.
 * <p>
 * A {@code PojoLinkOne} may carry:
 * <ul>
 *   <li>one or more {@link PojoLinkFkey} entries that represent scalar
 *       foreign-key properties on the source type,</li>
 *   <li>zero or more {@link PojoImportMatch} entries that describe
 *       alternative import-match paths, and</li>
 *   <li>an optional {@link PojoLinkUnique} definition describing a unique
 *       property (possibly reachable via {@code equalPropertyPath})
 *       that can be used for matching.</li>
 * </ul>
 * The {@link OAObjectPojoLoader} populates this structure based on
 * {@link com.viaoa.object.OALinkInfo} metadata.
 */
public class PojoLinkOne implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// References to other objects
	// PojoLink
	protected volatile PojoLink pojoLink;
	// PojoLinkUnique
	protected volatile PojoLinkUnique pojoLinkUnique;
	// PojoImportMatches
	protected volatile CopyOnWriteArrayList<PojoImportMatch> alPojoImportMatches = new CopyOnWriteArrayList<>();
	// PojoLinkFkeys
	protected volatile CopyOnWriteArrayList<PojoLinkFkey> alPojoLinkFkeys = new CopyOnWriteArrayList<>();

	public PojoLinkOne() {
	}

	@JsonIgnore
	public PojoLink getPojoLink() {
		return pojoLink;
	}

	public void setPojoLink(PojoLink newValue) {
		this.pojoLink = newValue;
	}

	// @JsonIgnore
	public PojoLinkUnique getPojoLinkUnique() {
		return pojoLinkUnique;
	}

	public void setPojoLinkUnique(PojoLinkUnique newValue) {
		this.pojoLinkUnique = newValue;
	}

	public CopyOnWriteArrayList<PojoImportMatch> getPojoImportMatches() {
		return alPojoImportMatches;
	}

	public void setPojoImportMatches(List<PojoImportMatch> list) {
		if (list == null) {
			this.alPojoImportMatches.clear();
		} else {
			this.alPojoImportMatches = new CopyOnWriteArrayList<>(list);
		}
	}

	public CopyOnWriteArrayList<PojoLinkFkey> getPojoLinkFkeys() {
		return alPojoLinkFkeys;
	}

	public void setPojoLinkFkeys(List<PojoLinkFkey> list) {
		if (list == null) {
			this.alPojoLinkFkeys.clear();
		} else {
			this.alPojoLinkFkeys = new CopyOnWriteArrayList<>(list);
		}
	}

	@Override
	public String toString() {
		return "PojoLinkOne [" +
				"]";
	}
}

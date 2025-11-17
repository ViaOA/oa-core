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
package com.viaoa.object;

import com.viaoa.annotation.OAFkey;

/**
 * Metadata that defines a foreign-key to primary/business-key mapping used by
 * {@link OALinkInfo} for ONE-side relationships. Each instance pairs a source
 * (foreign-key) property with its target (primary/unique-key) property so the
 * OA runtime can reconcile references and perform identity-safe lookups.
 *
 * <p>The mapping originates from {@link com.viaoa.annotation.OAFkey} on the
 * model and is consulted during lazy loading and reverse-link fix-up to ensure
 * that the correct target object is resolved without requiring full graph
 * materialization.</p>
 *
 * <p>This class is a simple metadata holder and has no side effects.</p>
 *
 * @see OALinkInfo
 * @see OAPropertyInfo
 * @see com.viaoa.annotation.OAFkey
 */
public class OAFkeyInfo implements java.io.Serializable {
	static final long serialVersionUID = 1L;

	private OAPropertyInfo fromPropertyInfo;
	private OAPropertyInfo toPropertyInfo;

	private OAFkey oaFkey;

	public OAPropertyInfo getFromPropertyInfo() {
		return fromPropertyInfo;
	}

	public void setFromPropertyInfo(OAPropertyInfo pi) {
		this.fromPropertyInfo = pi;
	}

	public OAPropertyInfo getToPropertyInfo() {
		return toPropertyInfo;
	}

	public void setToPropertyInfo(OAPropertyInfo pi) {
		this.toPropertyInfo = pi;
	}

	public void setOAFkey(OAFkey f) {
		oaFkey = f;
	}

	public OAFkey getOAFkey() {
		return oaFkey;
	}

}

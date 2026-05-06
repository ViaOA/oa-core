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
package com.viaoa.filter;

import com.viaoa.path.OAPath;

/**
 * Convenience filter that always evaluates the supplied property (or
 * entire object if no property path is supplied) as {@code Boolean.TRUE}.
 *
 * <p>
 * This class extends {@link OAEqualFilter} and hard-codes the comparison
 * value to {@code Boolean.TRUE}, allowing it to be used wherever a
 * property-based true/false condition needs to be enforced.
 * </p>
 *
 * <p>
 * Examples:
 * <ul>
 *   <li>Filtering objects where a boolean flag is true</li>
 *   <li>Restricting a Hub to enabled/active elements</li>
 *   <li>Selecting objects that meet a “valid” or “confirmed” rule</li>
 * </ul>
 * </p>
 */
public class OATrueFilter extends OAEqualFilter {

	/**
	 * Creates a filter that evaluates the target object itself as
	 * {@code Boolean.TRUE}. Delegates to {@code super(Boolean.TRUE)}.
	 */
	public OATrueFilter() {
		super(Boolean.TRUE);
	}

	/**
	 * Creates a filter that evaluates the value resolved from the supplied
	 * property path string as {@code Boolean.TRUE}.
	 *
	 * @param pp the property path expression used to obtain the value
	 */
	public OATrueFilter(String pp) {
		super(pp, Boolean.TRUE);
	}

	/**
	 * Creates a filter that evaluates the value resolved from the supplied
	 * {@link OAPath} as {@code Boolean.TRUE}.
	 *
	 * @param pp the property path used to obtain the value
	 */
	public OATrueFilter(OAPath pp) {
		super(pp, Boolean.TRUE);
	}

}

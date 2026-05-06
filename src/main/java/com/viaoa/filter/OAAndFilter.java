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

import com.viaoa.select.OASelect;

/**
 * Combines two {@link OAFilter} instances using logical AND.  Both filters
 * must evaluate to {@code true} for the object to be included.
 *
 * <p>
 * This filter supports datasource optimization: each contained filter is
 * given the opportunity to update the associated {@link OASelect}.  If
 * either filter contributes query-level constraints, the combined filter
 * returns {@code true} from {@link #updateSelect(OASelect)}.
 * </p>
 */
public class OAAndFilter implements OAFilter {

	/**
	 * The two delegate filters that are combined using logical AND. Both
	 * filters must allow an object for it to be accepted by this filter.
	 */
	private OAFilter filter1, filter2;

	/**
	 * Creates a new AND-composed filter using the supplied delegate filters.
	 *
	 * @param filter1 the first filter to evaluate
	 * @param filter2 the second filter to evaluate
	 */
	public OAAndFilter(OAFilter filter1, OAFilter filter2) {
		this.filter1 = filter1;
		this.filter2 = filter2;
	}

	/**
	 * Evaluates the supplied object against both delegate filters using logical
	 * AND semantics.
	 * <ul>
	 *   <li>If {@code filter1} is non-null and rejects the object, this method returns {@code false}.</li>
	 *   <li>If {@code filter1} accepts the object, {@code filter2} is evaluated (if non-null).</li>
	 * </ul>
	 *
	 * @param obj the object to evaluate
	 * @return {@code true} if both filters (when present) accept the object; otherwise {@code false}
	 */
	@Override
	public boolean isUsed(Object obj) {
		if (filter1 != null && !filter1.isUsed(obj)) {
			return false;
		}
		boolean b = (filter2 == null || filter2.isUsed(obj));
		return b;
	}

	/**
	 * Delegates select-optimization to both filters, combining their results
	 * using logical OR on the return flags.
	 * <p>
	 * Each non-null delegate filter is allowed to update the supplied
	 * {@link OASelect}. If either filter reports that it updated the select,
	 * this method returns {@code true}.
	 * </p>
	 *
	 * @param select the select instance that can be refined by the filters
	 * @return {@code true} if at least one delegate filter updated the select; otherwise {@code false}
	 */
	@Override
	public boolean updateSelect(OASelect select) {
		boolean b = false;
		if (filter1 != null) {
			b = filter1.updateSelect(select);
		}
		if (filter2 != null) {
			b |= filter2.updateSelect(select);
		}
		return b;
	}
}

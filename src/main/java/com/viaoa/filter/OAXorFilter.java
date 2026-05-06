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

/**
 * Filter that performs an exclusive-OR (XOR) between two {@link OAFilter}
 * instances.
 *
 * <p>
 * Evaluation rules:
 * <ul>
 *   <li>If exactly one filter returns {@code true}, the result is {@code true}.</li>
 *   <li>If both filters return the same result (both true or both false),
 *       the result is {@code false}.</li>
 *   <li>If both filters are {@code null}, the filter defaults to {@code true}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Useful when two conditions must not both be satisfied at once, such as
 * “object matches criterion A but not B” or “matches B but not A”.
 * </p>
 */
public class OAXorFilter implements OAFilter {

	/**
	 * The two {@link com.viaoa.filter.OAFilter} instances whose results are used in
	 * the XOR evaluation. Either filter may be {@code null}.
	 */
	private OAFilter filter1, filter2;

	/**
	 * Creates an XOR filter that evaluates exactly one of the supplied filters
	 * as {@code true}. If both filters are {@code null}, the overall filter
	 * defaults to {@code true}.
	 *
	 * @param filter1 the first filter to evaluate; may be {@code null}
	 * @param filter2 the second filter to evaluate; may be {@code null}
	 */
	public OAXorFilter(OAFilter filter1, OAFilter filter2) {
		this.filter1 = filter1;
		this.filter2 = filter2;
	}

	/**
	 * Evaluates the XOR of the two configured filters against the supplied
	 * object.
	 *
	 * <ul>
	 *   <li>If both filters are {@code null}, returns {@code true}.</li>
	 *   <li>If only one filter is non-null, its evaluation result is compared
	 *       against {@code false} to satisfy XOR semantics.</li>
	 *   <li>If both filters are non-null, returns {@code true} only when one
	 *       filter evaluates to {@code true} and the other to {@code false}.</li>
	 * </ul>
	 *
	 * @param obj the object to evaluate
	 * @return {@code true} if exactly one filter evaluates to {@code true},
	 *         {@code false} otherwise
	 */
	@Override
	public boolean isUsed(Object obj) {
		boolean b1 = false;
		if (filter1 == null) {
			if (filter2 == null) {
				return true; // no filter
			}
		} else {
			b1 = filter1.isUsed(obj);
		}

		boolean b2 = false;
		if (filter2 != null) {
			b2 = filter2.isUsed(obj);
		}
		boolean b = (b1 && !b2) || (!b1 && b2);
		return b;
	}
}

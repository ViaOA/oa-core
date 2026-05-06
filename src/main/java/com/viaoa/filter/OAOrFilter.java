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
 * Filter that performs a logical OR between two {@link OAFilter} instances.
 * <p>
 * Evaluation rules:
 * <ul>
 *   <li>If either filter returns {@code true}, the OR result is {@code true}.</li>
 *   <li>If both filters are {@code null}, the filter defaults to {@code true}.</li>
 *   <li>If only one filter is non-null, its result determines the outcome.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This filter is commonly used to combine two independent filtering criteria
 * without requiring a custom implementation.  It is also useful when building
 * dynamic filter chains where either or both filters may be optional.
 * </p>
 */
public class OAOrFilter implements OAFilter {

	/**
	 * The two filters whose results are combined logically using OR
	 * semantics. Either filter may be {@code null}.
	 */
	private OAFilter filter1, filter2;

	/**
	 * Constructs a filter that performs a logical OR between two
	 * {@link OAFilter} instances.
	 *
	 * @param filter1 the first filter; may be {@code null}
	 * @param filter2 the second filter; may be {@code null}
	 */
	public OAOrFilter(OAFilter filter1, OAFilter filter2) {
		this.filter1 = filter1;
		this.filter2 = filter2;
	}

	/**
	 * Evaluates the OR condition for the supplied object.
	 *
	 * <p>Evaluation rules:</p>
	 * <ul>
	 *   <li>If {@code filter1} is non-null and returns {@code true},
	 *       the result is {@code true}.</li>
	 *   <li>If {@code filter2} is non-null and returns {@code true},
	 *       the result is {@code true}.</li>
	 *   <li>If both filters are {@code null}, the method returns
	 *       {@code true}.</li>
	 *   <li>Otherwise, returns {@code false}.</li>
	 * </ul>
	 *
	 * @param obj the object being evaluated
	 * @return {@code true} if either filter accepts the object or both
	 *         filters are {@code null}; otherwise {@code false}
	 */
	@Override
	public boolean isUsed(Object obj) {
		if (filter1 != null) {
			if (filter1.isUsed(obj)) {
				return true;
			}
		}
		if (filter2 != null) {
			if (filter2.isUsed(obj)) {
				return true;
			}
		}
		return (filter1 == null && filter2 == null);
	}
}

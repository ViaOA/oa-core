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
package com.viaoa.util;

import java.io.Serializable;

import com.viaoa.datasource.OASelect;

/**
 * Functional filtering interface used throughout OA to evaluate whether an
 * object should be included in a collection, Hub, or query result.  An
 * {@code OAFilter} provides a lightweight, deterministic predicate that can
 * be applied locally in memory or, when possible, pushed down into a
 * datasource during query execution.
 *
 * <p>
 * Typical usage includes:
 * </p>
 *
 * <ul>
 *   <li>Filtering the contents of a {@code Hub} or derived Hub.</li>
 *   <li>Constraining results returned by an {@link com.viaoa.datasource.OASelect}.</li>
 *   <li>Filtering child collections in master/detail relationships.</li>
 *   <li>Applying UI-level filtering for tables, lists, and dropdowns.</li>
 *   <li>Providing client-side filter logic when synchronizing with remote servers.</li>
 * </ul>
 *
 * <p>
 * The filtering contract is intentionally simple: {@link #isUsed(Object)}
 * determines whether a given object is included.  Because {@code OAFilter}
 * extends {@link Serializable}, filters can be distributed across servers,
 * clients, caches, and asynchronous processing pipelines.
 * </p>
 *
 * <h3>Datasource Integration</h3>
 *
 * <p>
 * The optional {@link #updateSelect(OASelect)} callback allows filters to
 * collaborate with an {@code OASelect} before a query is executed.  If the
 * datasource can evaluate the filter natively (e.g., converting it to SQL),
 * the select may incorporate the logic directly.  Returning {@code false}
 * indicates that the filter does not need to be applied after the query is
 * executed.
 * </p>
 *
 * <p><b>Note:</b> Most filters simply return {@code true}, meaning the filter
 * is still applied to the final in-memory results.</p>
 *
 * <p>
 * Because it is a {@code @FunctionalInterface}, filters can be written
 * concisely using lambda expressions:
 * </p>
 *
 * <pre>
 * Hub&lt;Customer&gt; hubCustomers = ...;
 * hubCustomers.setFilter(cust -> cust.getBalance() &gt; 0);
 * </pre>
 *
 * <p>
 * In OA, filters play a central role in shaping how object graphs, derived
 * collections, and queries behave, while keeping the filtering logic isolated,
 * reusable, and testable.
 * </p>
 *
 * @param <T> the type of object being evaluated by this filter
 */
@FunctionalInterface
public interface OAFilter<T> extends Serializable {
	
	/**
	 * Determines whether the given object should be included.
	 *
	 * @param obj the object to evaluate
	 * @return true if the object is accepted by this filter
	 */
	boolean isUsed(T obj);

	/**
	 * Callback, that allows a Filter to be called by Select before it is performed, so that the filter can be done by the datasource that
	 * performs the select/query.
	 * <p>
	 *
	 * @param select oaselect that is using this select, before it runs the query on the datasource.
	 * @return true (default) if this filter should still be used from the select results.
	 */
	default boolean updateSelect(OASelect select) {
		return true;
	}
}

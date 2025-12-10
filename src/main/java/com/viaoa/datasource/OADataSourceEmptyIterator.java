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
package com.viaoa.datasource;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * A stateless {@link OADataSourceIterator} implementation that always
 * represents an empty result set. All query-related methods return null
 * and all iteration methods indicate the absence of elements.
 * <p>
 * This iterator is typically used when a DataSource operation produces
 * no results but a valid {@code OADataSourceIterator} instance is still
 * required by the calling code.
 * <p>
 * Characteristics:
 * <ul>
 *   <li>Always reports no available elements.</li>
 *   <li>Does not throw {@link NoSuchElementException} from {@link #next()}.</li>
 *   <li>All mutating or traversal methods are implemented as no-ops.</li>
 * </ul>
 */
public class OADataSourceEmptyIterator implements OADataSourceIterator {

	/**
	 * Always returns {@code false}, as this iterator contains no elements.
	 *
	 * @return false, indicating that no elements are available
	 */
	@Override
	public boolean hasNext() {
		return false;
	}

	/**
	 * Returns the next element in the iterator. Since this iterator is empty,
	 * the method always returns {@code null}. Although the standard iterator
	 * contract requires throwing a {@link NoSuchElementException} when no
	 * elements remain, this implementation does not throw the exception.
	 *
	 * @return {@code null}, as there are no elements to return
	 * @throws NoSuchElementException not thrown by this implementation
	 */
	@Override
	public Object next() throws NoSuchElementException {
		// ?? if no more elements, then this should throw NoSuchElementException
		return null;
	}

	/**
	 * Returns the primary query associated with this iterator. Because this
	 * iterator represents an empty result set and does not originate from a
	 * query execution, the method always returns {@code null}.
	 *
	 * @return null, since no query is associated with this iterator
	 */
	@Override
	public String getQuery() {
		return null;
	}

	/**
	 * Returns a secondary or translated query string, if applicable. Since
	 * this iterator is empty and not tied to any query, the method always
	 * returns {@code null}.
	 *
	 * @return null, because no secondary query exists
	 */
	@Override
	public String getQuery2() {
		return null;
	}

	/**
	 * No-operation implementation. Removal of elements is unsupported
	 * because this iterator contains no elements.
	 */
	@Override
	public void remove() {
		// no-op
	}

	/**
	 * Performs no action because there are no elements to supply to the
	 * given {@link Consumer}. The method simply returns.
	 *
	 * @param action the consumer that would receive remaining elements
	 */
	@Override
	public void forEachRemaining(Consumer action) {
		// no-op
	}
}

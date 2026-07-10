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
package com.viaoa.callback;

/**
 * Generic visitor callback used by OA traversal operations.
 * <p>
 * Implementations receive each object selected by the owning operation and
 * return whether traversal should continue. The owner defines the traversal
 * source, ordering, and stop boundary.
 * </p>
 *
 * @param <TYPE> object type supplied to the callback
 */
public interface OACallback<TYPE> {

	/**
	 * Invoked for the current object in an OA traversal.
	 * <p>
	 * Returning {@code false} requests that the owning traversal stop according
	 * to its documented stop rules.
	 * </p>
	 *
	 * @param obj the current object being visited
	 * @return {@code true} to continue visiting additional objects,
	 *         {@code false} to stop the visitation
	 */
	public boolean updateObject(TYPE obj);
}

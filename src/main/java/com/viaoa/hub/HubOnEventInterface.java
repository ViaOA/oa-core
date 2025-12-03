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
package com.viaoa.hub;

import com.viaoa.object.OAObject;

/**
 * Functional interface used for registering lightweight single-method
 * callbacks to handle {@link HubEvent HubEvents}.
 *
 * <p>This interface enables inline or lambda-based event handling for Hubs,
 * providing a concise alternative to the full {@link HubListener} API.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * hub.onEvent(event -> {
 *     if (event.isAdd()) {
 *         System.out.println("Added: " + event.getObject());
 *     }
 * });
 * }</pre>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Intended for simple event consumers that only need one unified
 *       callback method.</li>
 *   <li>Used internally by {@link HubDelegate} and other delegate classes to
 *       attach transient or scoped event handlers.</li>
 *   <li>Generic type parameter <code>T</code> restricts events to a specific
 *       {@link OAObject} type associated with the Hub.</li>
 * </ul>
 */
public interface HubOnEventInterface<T extends OAObject> {

	/**
	 * Handles a Hub event using a single-method functional callback.
	 *
	 * @param event the HubEvent describing the change occurring within the Hub
	 */
    void onEvent(HubEvent<T> event);
}

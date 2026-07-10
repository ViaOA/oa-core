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
package com.viaoa.trigger;

import com.viaoa.hub.HubEvent;
import com.viaoa.object.OAObject;

/**
 * Listener interface invoked by {@link OATrigger} when a dependent property path
 * changes anywhere within an {@link OAObject} graph.
 *
 * @param <T> the root object type associated with the trigger
 *
 * @see OATrigger
 * @see OATriggerDelegate
 */
public interface OATriggerListener<T extends OAObject> {
    
	/**
	 * Invoked when a change occurs along one of the trigger's dependent property
	 * paths. The method receives the root object, the associated hub event, and
	 * the property path from the root to the object where the event occurred.
	 *
	 * @param objRoot              the root object affected by the change
	 * @param hubEvent             details about the triggering event
	 * @param pathFromRoot the path from the root object to the event source
	 * @throws Exception if the listener encounters an error during processing
	 */
    public void onTrigger(T objRoot, HubEvent hubEvent, String pathFromRoot) throws Exception;
}



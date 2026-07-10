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
/**
 * Hub collection runtime and related live-view support.
 * <p>
 * A {@link com.viaoa.hub.Hub} is OA's model-aware collection type. Hubs hold
 * OA model objects, maintain active-object state, publish change events, support
 * master/detail relationships, and cooperate with OA runtime services for
 * loading, sorting, filtering, linking, merging, selection, synchronization,
 * rules, and generated UI binding.
 * <p>
 * Subpackages provide focused Hub controllers for automatic membership, copying,
 * detail views, filtering, indexing, linking, listener trees, merging, sorting,
 * triggers, utility Hubs, and derived live views. These controllers are normally
 * used through Hub APIs and OA services rather than as application-level
 * persistence containers.
 *
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.hub.HubListener
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.oa.OA
 */
package com.viaoa.hub;

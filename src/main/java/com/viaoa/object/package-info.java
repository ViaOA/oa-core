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
 * Core OA object runtime classes.
 * <p>
 * This package contains {@link com.viaoa.object.OAObject}, the base class for
 * model entities, plus object identity and internal bridge types used by the OA
 * runtime. OAObjects provide property storage, lifecycle flags, identity keys,
 * change events, rule/callback participation, persistence hooks, lazy loading,
 * synchronization support, and integration with {@link com.viaoa.hub.Hub}.
 * <p>
 * Application model classes normally extend {@code OAObject}. OA runtime and
 * service code use package-level helpers and friend-access bridges to coordinate
 * cache, metadata, serialization, Hub membership, and object state without
 * exposing those internals as public application API.
 *
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.object.OAObjectKey
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.oa.OA
 */
package com.viaoa.object;

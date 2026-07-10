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
 * Runtime object-cache support for OAObject identity, lookup, listeners, filters, and cache-driven Hub updates.
 * <p>
 * The cache package keeps weak references to OAObjects by runtime GUID and maintains secondary indexes for business-key
 * lookup. This allows the OA runtime to preserve object identity while still allowing unreferenced objects to be reclaimed
 * by the garbage collector.
 * </p>
 * <p>
 * {@link com.viaoa.cache.OAObjectCache} owns the cache storage and key indexes,
 * {@link com.viaoa.cache.OAObjectIndex} maps object keys to GUIDs, and
 * {@link com.viaoa.cache.OAObjectCacheListener} defines cache-level events. Filter and trigger helpers can maintain live
 * Hubs or run callbacks when cached objects match configured property-path and filter rules.
 * </p>
 * <p>
 * Cache utilities are runtime infrastructure. They coordinate with OA services, metadata, Hubs, sync, serialization, and
 * datasource loading, but they do not define model permissions or object-rule evaluation.
 * </p>
 */
package com.viaoa.cache;

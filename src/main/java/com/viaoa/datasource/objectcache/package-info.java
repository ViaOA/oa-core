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
 * Provides an in-memory {@link com.viaoa.datasource.OADataSource}
 * implementation and supporting iterators.
 * <p>
 * Classes in this package allow OA applications to operate without an external
 * database by storing objects directly in memory and serializing them to disk
 * when needed.
 *
 * <ul>
 *   <li>{@link com.viaoa.datasource.objectcache.OADataSourceObjectCache} —
 *       full in-memory data source with compressed save/load support.</li>
 *   <li>{@link com.viaoa.datasource.objectcache.ObjectCacheIterator} —
 *       streaming iterator for cache-based queries.</li>
 * </ul>
 */
package com.viaoa.datasource.objectcache;

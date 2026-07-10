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
 * Finder utilities for traversing OA model objects and Hubs using property paths and filters.
 * <p>
 * {@link com.viaoa.find.OAFinder} searches from an OAObject, Hub, or list root through an
 * {@link com.viaoa.path.OAPath}, applies one or more {@link com.viaoa.filter.OAFilter} instances, and returns matching
 * target OAObjects. It supports convenience filters, query filters, duplicate detection, smallest/largest matching,
 * recursive traversal protection, max-result limits, and optional lazy-load behavior.
 * </p>
 * <p>
 * {@link com.viaoa.find.OAHierFinder} searches hierarchical or recursive OAObject structures for the first matching
 * property value.
 * </p>
 *
 * @see com.viaoa.find.OAFinder
 * @see com.viaoa.find.OAHierFinder
 * @see com.viaoa.path.OAPath
 * @see com.viaoa.filter.OAFilter
 */
package com.viaoa.find;

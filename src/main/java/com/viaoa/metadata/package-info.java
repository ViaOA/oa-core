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
 * Runtime metadata for OA model classes, properties, links, calculated values, methods, object models, and POJO
 * mappings.
 * <p>
 * Metadata is the executable contract that connects generated OA model classes, annotations, Java reflection, Hubs,
 * datasource behavior, object rules, triggers, paths, serialization, synchronization, and replication. Runtime services
 * use these classes to answer model questions without rediscovering structure from raw JavaBean methods each time.
 * </p>
 * <p>
 * {@link com.viaoa.metadata.OAObjectInfo} describes an OAObject class. {@link com.viaoa.metadata.OAPropertyInfo},
 * {@link com.viaoa.metadata.OALinkInfo}, {@link com.viaoa.metadata.OACalcInfo}, and
 * {@link com.viaoa.metadata.OAMethodInfo} describe the model members that can be navigated, displayed, persisted,
 * validated, invoked, or used by OA rules.
 * </p>
 * <p>
 * The {@code pojo} subpackage contains metadata used to map external/plain data structures into OAObject models while
 * preserving identity, links, import matching, and uniqueness rules.
 * </p>
 *
 * @see com.viaoa.metadata.OAObjectInfo
 * @see com.viaoa.metadata.OAPropertyInfo
 * @see com.viaoa.metadata.OALinkInfo
 * @see com.viaoa.metadata.OACalcInfo
 * @see com.viaoa.metadata.OAMethodInfo
 */
package com.viaoa.metadata;

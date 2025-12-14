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
 * Metadata model used to describe how JSON POJOs map to live {@code OAObject}
 * instances during serialization and import.
 *
 * <h2>Overview</h2>
 * The {@code com.viaoa.pojo} package defines a lightweight metadata graph
 * used by the OA JSON/Jackson integration to translate flat JSON structures
 * into a correctly linked {@code OAObject} graph. This metadata is generated
 * once per OAObject type by {@link com.viaoa.pojo.OAObjectPojoLoader}, based
 * on {@link com.viaoa.object.OAObjectInfo}.
 *
 * <p>
 * The resulting structure mirrors an OAObject definition, but in a POJO-centric
 * form that identifies:
 * <ul>
 *   <li>regular scalar properties</li>
 *   <li>link-one and link-many associations</li>
 *   <li>foreign-key properties for link-one relationships</li>
 *   <li>import-match properties used to locate existing objects</li>
 *   <li>unique-key definitions derived through {@code equalPropertyPath}</li>
 * </ul>
 *
 * <h2>Core Concepts</h2>
 *
 * <h3>Pojo</h3>
 * A {@link com.viaoa.pojo.Pojo} object represents a single OAObject type. It
 * contains the set of:
 * <ul>
 *   <li>{@link com.viaoa.pojo.PojoRegularProperty} – regular scalar POJO fields</li>
 *   <li>{@link com.viaoa.pojo.PojoLink} – each link property (one/many)</li>
 * </ul>
 *
 * <h3>PojoProperty</h3>
 * Each scalar field participating in JSON mapping is represented as a
 * {@link com.viaoa.pojo.PojoProperty}, which includes both the POJO-side name
 * and the OA property path it corresponds to. A property may be part of:
 * <ul>
 *   <li>a foreign-key for a link-one</li>
 *   <li>an import-match definition</li>
 *   <li>a unique-key definition</li>
 * </ul>
 * If the property is part of a POJO key, its {@code keyPos} value defines its
 * position in a compound key.
 *
 * <h3>Link-One Structures</h3>
 * A {@link com.viaoa.pojo.PojoLinkOne} describes the metadata needed to resolve
 * a link-one association during JSON import. It may hold:
 * <ul>
 *   <li>{@link com.viaoa.pojo.PojoLinkFkey} – scalar foreign-key properties</li>
 *   <li>{@link com.viaoa.pojo.PojoImportMatch} – scalar or nested match rules</li>
 *   <li>{@link com.viaoa.pojo.PojoLinkUnique} – unique-key definitions</li>
 * </ul>
 *
 * <p>
 * Nested key definitions (used in {@code equalPropertyPath}-based uniqueness
 * and multi-hop import matches) are modeled using
 * {@link com.viaoa.pojo.PojoLinkOneReference}, which allows resolution to
 * follow association chains.
 *
 * <h3>Delegates</h3>
 * The delegate classes (primarily
 * {@link com.viaoa.pojo.PojoLinkOneDelegate} and
 * {@link com.viaoa.pojo.PojoDelegate}) provide utilities for:
 * <ul>
 *   <li>locating POJO properties by name</li>
 *   <li>retrieving key properties for a type or link-one definition</li>
 *   <li>flattening nested reference chains into an ordered key list</li>
 *   <li>checking whether a type uses primary-key, import-match, or unique-key resolution</li>
 * </ul>
 *
 * <h2>Role in JSON Mapping</h2>
 * During deserialization, the Jackson OA module consults the POJO metadata to:
 * <ul>
 *   <li>determine which existing {@code OAObject} should be matched or created</li>
 *   <li>apply multi-hop association lookups via import-match and unique rules</li>
 *   <li>maintain object identity and prevent duplicates</li>
 *   <li>rebuild correct link-one and link-many relationships</li>
 * </ul>
 *
 * <p>
 * This layer forms the bridge between flat JSON payloads and OA’s executable
 * object-graph model, ensuring that deserialized data merges into the live
 * graph according to OA’s deterministic identity, cascade, and relationship
 * semantics.
 *
 * <h2>Generated Metadata</h2>
 * The metadata structure defined in this package is the runtime counterpart
 * of the OABuilder {@code OABuilderPojo} model. The loader reconstructs this
 * structure at runtime from {@code OAObjectInfo}, so the JSON layer stays
 * perfectly aligned with the OA model definition and with generated code.
 */
package com.viaoa.pojo;


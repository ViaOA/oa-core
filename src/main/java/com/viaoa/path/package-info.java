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
 * Property-path parsing and evaluation for OA model objects and Hubs.
 * <p>
 * The path package resolves dotted OA property paths against model metadata, Java accessors, links, calculated
 * properties, Hub relationships, optional casts, and custom Hub filters. A path is an executable navigation contract
 * used by bindings, filters, finders, queries, serializers, controllers, generated code, and other OA runtime services.
 * </p>
 * <p>
 * {@link com.viaoa.path.OAPath} is the main parsed-path representation. It records the resolved properties, methods,
 * classes, link metadata, calculated/property metadata, filters, reverse paths, and format information needed to evaluate
 * the path repeatedly against compatible OAObject or Hub roots. {@link com.viaoa.path.OAPathDelegate} provides helper
 * methods for root-qualified path creation and metadata-driven path construction between classes.
 * </p>
 * <p>
 * Path evaluation follows OA metadata semantics. Link direction, master/detail scope, private-link policy, Hub active
 * object traversal, calculated property metadata, and terminal value type are all part of the path contract. Invalid or
 * unresolved paths either report setup errors or use the documented lenient behavior selected by the caller.
 * </p>
 *
 * @see com.viaoa.path.OAPath
 * @see com.viaoa.path.OAPathDelegate
 * @see com.viaoa.metadata.OAObjectInfo
 * @see com.viaoa.metadata.OALinkInfo
 * @see com.viaoa.hub.Hub
 */
package com.viaoa.path;

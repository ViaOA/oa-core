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
 * Provides the core template-processing engine used throughout OA for dynamic
 * string generation, HTML assembly, and metadata-driven code generation.
 *
 * <p>
 * The primary class in this package, {@link com.viaoa.template.OATemplate},
 * implements a lightweight, high-performance parser for evaluating template
 * expressions of the form <code><%= ... %></code>. Templates may include:
 * </p>
 *
 * <ul>
 *   <li>Property lookups on {@link com.viaoa.object.OAObject} instances</li>
 *   <li>Conditional blocks (<code>if</code>, <code>ifnot</code>,
 *       <code>ifequals</code>, comparisons)</li>
 *   <li>Iteration using <code>foreach</code> on {@link com.viaoa.hub.Hub}</li>
 *   <li>Formatted output using OA formatting conventions</li>
 *   <li>Template variables referenced using <code>$name</code></li>
 * </ul>
 *
 * <p>
 * Templates are parsed into a tree of lightweight nodes, allowing extremely
 * fast evaluation with minimal allocations. This architecture enables OA-Web
 * to render HTML fragments efficiently and allows OABuilder to generate large
 * volumes of code using metadata-driven templates.
 * </p>
 *
 * <p>
 * The template engine itself is non-reflective and delegates all property-path
 * resolution to callers (such as OAPropertyPath), enabling the parser to
 * remain compact while supporting complex object-graph navigation.
 * </p>
 *
 * <p>
 * This package contains only foundational template-processing classes and does
 * not impose any UI or web-layer dependencies.
 * </p>
 */
package com.viaoa.template;

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
 * Functional utilities built on top of the OA object-graph and property-path
 * model. <p>
 *
 * This package provides higher-level operations that apply to
 * {@link com.viaoa.object.OAObject} instances and {@link com.viaoa.hub.Hub}
 * collections, including aggregation, property evaluation, and text template
 * processing. These functions rely on OA's dynamic property-path navigation
 * and the {@link com.viaoa.object.OAFinder} traversal engine. <p>
 *
 * Features include:
 * <ul>
 *   <li>Object and property counting through nested relationships.</li>
 *   <li>Summation and min/max evaluation of numeric fields.</li>
 *   <li>Template processing using OA's expression and template engine.</li>
 *   <li>Helper callback interfaces for collecting generated output.</li>
 * </ul>
 *
 * These utilities are used by templating, reporting, and dynamic UI components
 * to evaluate expressions against live OAObject graphs.
 */
package com.viaoa.func;
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
 * Object-service machinery (sub-services).
 *
 * <p>This package contains internal sub-services that together implement the Object
 * functionality for a single {@code OAGraph} instance.</p>
 *
 * <p>These classes are not intended to be constructed directly. They are created,
 * wired, and managed by the owning coordinator ({@code OAObjectService}) in the
 * parent package.</p>
 *
 * <p>Sub-services may declare required outcomes via protected abstract "dependency hooks".
 * These hooks are implemented by the owner/coordinator and are intentionally used to:
 * <ul>
 *   <li>keep sub-services from depending on other services directly</li>
 *   <li>centralize coordination and lifecycle management</li>
 *   <li>prevent service-locator coupling</li>
 * </ul>
 * </p>
 */
package com.viaoa.graph.service.object;
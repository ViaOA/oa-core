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
 * OA runtime entry point and implementation package.
 * <p>
 * OA is the Object Automation runtime for an OA model. It provides the public
 * runtime interface used to create, select, find, save, delete, observe, and
 * compose live {@code OAObject} and {@code Hub} structures from model metadata.
 * <p>
 * The runtime is organized around clear boundaries:
 * <ul>
 * <li>{@code OA} is the public runtime interface.</li>
 * <li>{@code OA.services()} exposes curated public and advanced service APIs.</li>
 * <li>{@code OA.internal()} exposes OA-library/runtime-only operation APIs.</li>
 * <li>{@code modelUser()} and {@code sessionUser()} separate generated model
 * permissions from session/application identity.</li>
 * </ul>
 * <p>
 * Application and generated code should normally use {@code OA},
 * {@code OA.services()}, {@code OAObject}, and {@code Hub}. Internal OA
 * libraries may use {@code OA.internal()} when lower-level runtime behavior is
 * required.
 *
 * @see com.viaoa.oa.OA
 * @see com.viaoa.oa.api
 * @see com.viaoa.oa.api.services
 * @see com.viaoa.oa.api.internal
 */
package com.viaoa.oa;

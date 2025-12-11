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
 * Provides classes that implement lightweight autonumber assignment for
 * {@link com.viaoa.object.OAObject} instances.
 * <p>
 * This package contains a minimal {@link com.viaoa.datasource.OADataSource}
 * implementation whose sole responsibility is assigning sequential numeric
 * identifiers to objects. It does not support persistence, selection, query
 * execution, or storage operations.
 * <p>
 * Autonumber assignment can operate in two modes:
 * <ul>
 *   <li><b>Global mode:</b> a shared Hub of {@code NextNumber} sequences is used
 *       by all datasource instances.</li>
 *   <li><b>Local mode:</b> callers may supply their own Hub instance, allowing
 *       autonumber sequences to be isolated per usage context.</li>
 * </ul>
 * <p>
 * The package-level functionality supports:
 * <ul>
 *   <li>auto-assigning IDs during object construction or insertion, depending
 *       on configuration</li>
 *   <li>lazy creation of per-class {@code NextNumber} sequences</li>
 *   <li>class-level filtering that determines whether autonumber values may be
 *       assigned to a given type</li>
 * </ul>
 */
package com.viaoa.datasource.autonumber;

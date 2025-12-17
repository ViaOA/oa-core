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
 * General-purpose utility classes used throughout the OA (Object Automation) framework.
 * <p>
 * This package contains a collection of lightweight, reusable helpers that provide
 * common functionality not tied to any specific OA subsystem. The classes in this
 * package are designed to be broadly applicable, stable, and free of application-
 * specific dependencies.
 * <p>
 * Included utilities cover areas such as:
 * <ul>
 *   <li>Date, time, and timezone handling ({@link com.viaoa.util.OADate},
 *       {@link com.viaoa.util.OATime}, {@link com.viaoa.util.OATimeZone})</li>
 *   <li>String manipulation and comparison helpers</li>
 *   <li>Throttling and timing utilities ({@link com.viaoa.util.OAThrottle})</li>
 *   <li>Sentinel and marker objects used by OA comparison logic
 *       ({@link com.viaoa.util.OAUnknownObject})</li>
 *   <li>Lightweight format and configuration readers
 *       ({@link com.viaoa.util.OAYamlReader})</li>
 * </ul>
 * <p>
 * Classes in this package generally emphasize:
 * <ul>
 *   <li>Minimal dependencies</li>
 *   <li>Predictable, explicit behavior</li>
 *   <li>Thread-safety where appropriate</li>
 *   <li>Compatibility across different runtime environments</li>
 * </ul>
 * <p>
 * These utilities form part of the foundational layer of OA and are intended
 * to be used by higher-level modules such as the Object Graph, data sources,
 * synchronization, and web frameworks.
 */
package com.viaoa.util;



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
 * Concurrency utilities used throughout the OA framework. <p>
 *
 * The classes in this package provide lightweight wrappers around Java
 * concurrency primitives, integrating them with OA's execution model where
 * necessary. The package includes:
 *
 * <ul>
 *   <li><b>OAConcurrent</b>: launches a group of runnables with synchronized
 *       start timing.</li>
 *   <li><b>OAExecutorService</b>: thread-pool executor with named, daemon
 *       threads for background processing.</li>
 *   <li><b>OAScheduledExecutorService</b>: scheduler for tasks that run at
 *       specific OA temporal values or at fixed intervals.</li>
 *   <li><b>OAThread</b>: Thread subclass that propagates OA thread context into
 *       worker threads.</li>
 * </ul>
 *
 * These classes do not manage OAObject or Hub behavior directly, but they
 * provide foundational concurrency tools used by higher-level OA subsystems
 * such as remoting, caching, background updates, and object graph traversal.
 */
package com.viaoa.concurrent;

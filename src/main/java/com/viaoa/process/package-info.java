/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
 * Process-management utilities for event-driven, scheduled, and long-running
 * operations within OA applications. <p>
 *
 * The package includes:
 * <ul>
 *   <li>{@link com.viaoa.process.OAChangeProcessor} – triggers processing when
 *       Hub or property-path changes occur.</li>
 *   <li>{@link com.viaoa.process.OAChangeRefresher} – background refresher that
 *       coalesces multiple change events into serialized processing.</li>
 *   <li>{@link com.viaoa.process.OACron} – cron-style schedule definition and
 *       next-execution computation.</li>
 *   <li>{@link com.viaoa.process.OACronProcessor} – executes cron jobs using a
 *       background daemon thread.</li>
 *   <li>{@link com.viaoa.process.OAProcess} – tracks state and lifecycle of
 *       asynchronous or background processes.</li>
 *   <li>{@link com.viaoa.process.OAThreadMonitor} – diagnostic tool for thread
 *       inspection.</li>
 * </ul>
 *
 * These utilities integrate with OA's concurrency, Hub event stream, and
 * temporal classes to provide robust infrastructure for background processing.
 */
package com.viaoa.process;
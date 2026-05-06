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
package com.viaoa.performance;

import java.util.logging.Logger;


/**
 * Defines configuration flags for runtime performance diagnostics within the OA framework.
 *
 * <p>This class acts as a central toggle point for selectively including 
 * detailed timing and event logging throughout OA subsystems such as 
 * triggers, hub listeners, and circular queues.  It does not collect 
 * metrics directly; instead, dependent components check these constants 
 * to decide whether to record or skip detailed instrumentation data.</p>
 *
 * <p><b>Constants</b>:
 * <ul>
 *   <li>{@code IncludeTriggers} — when true, OA will measure and log
 *       performance data for trigger evaluations and cascade propagation.</li>
 *   <li>{@code IncludeHubListeners} — when true, include hub event dispatch timing.</li>
 *   <li>{@code IncludeCircularQueue} — when true, profile OA’s event queue performance.</li>
 * </ul>
 *
 * <p>Because this class holds only public static flags and a shared logger,
 * it can be modified dynamically at runtime to control diagnostic verbosity.</p>
 */
public class OAPerformance {
    public final static Logger LOG = Logger.getLogger(OAPerformance.class.getName());
    
    /**
     * Flag indicating whether performance diagnostics should include timing
     * and evaluation metrics for OA trigger processing. When enabled, trigger
     * cascades and related events may emit additional profiling output.
     */
    public static final boolean IncludeTriggers = true;
    
    /**
     * Flag determining whether hub listener event-dispatch timing should be
     * included in performance diagnostics. Disabled by default to reduce
     * overhead unless hub-level profiling is required.
     */
    public static final boolean IncludeHubListeners = false;
    
    /**
     * Flag controlling whether performance profiling for OA’s circular event
     * queues is active. When enabled, queue latency and throughput statistics
     * may be logged for analysis.
     */
    public static final boolean IncludeCircularQueue = true;
}

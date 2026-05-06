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
 * Scheduling utilities built on top of the OA object model. This package
 * provides components for defining date–time availability ranges, merging
 * schedules, determining availability, and assigning schedule entries to
 * OAObjects. <p>
 *
 * Key components include:
 * <ul>
 *   <li>{@link com.viaoa.schedule.OADateTimeRange} – representation of an
 *       individual date–time range.</li>
 *   <li>{@link com.viaoa.schedule.OASchedule} – interval-set structure for
 *       merging and iterating date–time ranges.</li>
 *   <li>{@link com.viaoa.schedule.OASchedulerPlan} – full plan containing
 *       open, preferred, blocked, and scheduled ranges.</li>
 *   <li>{@link com.viaoa.schedule.OAScheduler} – aggregator for multiple
 *       plans belonging to a resource or object.</li>
 *   <li>{@link com.viaoa.schedule.OASchedulerController} – controller for
 *       selecting and applying schedule date–time values to OAObjects.</li>
 * </ul>
 *
 * These classes support rich scheduling models where availability depends on
 * both object relationships and date–time logic.
 */
package com.viaoa.schedule;

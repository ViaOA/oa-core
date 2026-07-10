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
 * Core OA runtime services.
 * <p>
 * This package contains top-level services owned by an OA runtime instance,
 * including configuration, model-user state, session-user state, real-time
 * synchronization, replication, and trigger execution. These services form the
 * runtime layer behind the public {@code com.viaoa.oa.api} contracts and the
 * curated {@code OA.services()} facade.
 * <p>
 * Object and Hub implementation services live in subpackages. Public service
 * facades and internal facades delegate into these services while preserving
 * the public/internal boundary.
 *
 * @see com.viaoa.oa.OA
 * @see com.viaoa.oa.api
 * @see com.viaoa.oa.api.services
 * @see com.viaoa.oa.api.internal
 */
package com.viaoa.oa.service;

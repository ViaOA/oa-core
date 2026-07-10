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
 * Callback contracts and callback carrier objects used by OA runtime services.
 * <p>
 * This package contains small extension points for traversal, copy, serialization,
 * display-label customization, and object-rule participation. The callbacks are
 * intentionally lightweight: the owning OA service defines when a callback is
 * invoked, what state is supplied, and how returned values are interpreted.
 * </p>
 * <p>
 * {@link com.viaoa.callback.OAObjectCallback} is the request/response carrier
 * used by {@code OAObjectRulesService}. Its {@code Type} identifies the semantic
 * rule question, while its check types identify which rules-engine stages are
 * active for a particular request. Object callback methods, Hub listeners, and
 * UI/controller code can all use the same carrier.
 * </p>
 * <p>
 * {@link com.viaoa.callback.OACallback} is a generic visitor callback,
 * {@link com.viaoa.callback.OACopyCallback} customizes OA deep-copy behavior,
 * {@link com.viaoa.callback.OAObjectSerializerCallback} customizes serialization,
 * and {@link com.viaoa.callback.OACallbackLabel} carries label/display hints.
 * Mutable callback objects are intended to be scoped to a single logical
 * invocation unless a specific owner documents a broader lifecycle.
 * </p>
 */
package com.viaoa.callback;

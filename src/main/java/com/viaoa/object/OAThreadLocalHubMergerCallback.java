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
package com.viaoa.object;

/**
 * Callback interface used internally by OA’s Hub-merge process to execute
 * custom logic within the same thread-local merge context.
 *
 * <p>When OA merges one Hub’s contents into another, it temporarily installs
 * a thread-local context so that recursive updates and event propagation can
 * be suppressed or controlled.  Implementations of this interface are invoked
 * inside that protected block to perform additional merge-aware operations.</p>
 *
 * <p>Typical usage:
 * <pre>{@code
 * HubMerger.merge(hubA, hubB, () -> { /* custom post-merge logic *\/ });
 * }</pre></p>
 */
public interface OAThreadLocalHubMergerCallback {
    public void callback();
}

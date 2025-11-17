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
 * File upload and download support for OA's synchronization subsystem.
 * <p>
 * This package provides a lightweight file-transfer mechanism layered on top
 * of the multiplexer socket used by {@link com.viaoa.sync.OASyncClient} and
 * {@link com.viaoa.sync.OASyncServer}. It enables client applications to:
 * <ul>
 *   <li>download files from a server-controlled directory,</li>
 *   <li>upload files back to the server,</li>
 *   <li>use dedicated multiplexer sockets for high-throughput binary transfer,</li>
 *   <li>enforce directory safety and prevent traversal attacks.</li>
 * </ul>
 *
 * <h2>Classes</h2>
 *
 * <h3>{@link com.viaoa.sync.file.ServerFile}</h3>
 * Runs two background threads:
 * <ul>
 *   <li>one accepting upload sockets,</li>
 *   <li>one accepting download sockets.</li>
 * </ul>
 * Validates file requests, streams binary content, and ensures that clients
 * can only access files within a configured server directory.
 *
 * <h3>{@link com.viaoa.sync.file.ClientFile}</h3>
 * Client-side utility used by {@link com.viaoa.sync.OASyncClient} to
 * upload/download files. Communicates with {@code ServerFile} using a simple
 * block-based binary protocol.
 *
 * <p>
 * The file-transfer mechanism is intentionally simple, efficient, and separate
 * from the normal remote-method queue to avoid interfering with sync message
 * ordering.
 */
package com.viaoa.sync.file;

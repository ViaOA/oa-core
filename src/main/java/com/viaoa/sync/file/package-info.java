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
 * File upload and download support for the OA synchronization subsystem.
 * <p>
 * {@link com.viaoa.sync.file.ServerFile} accepts upload and download socket requests rooted at a configured server
 * directory. {@link com.viaoa.sync.file.ClientFile} provides the client-side transfer API used by sync clients. File
 * transfer uses separate socket paths so binary movement does not interfere with ordered sync remote-method traffic.
 * </p>
 */
package com.viaoa.sync.file;

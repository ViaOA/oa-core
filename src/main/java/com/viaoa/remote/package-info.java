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
 * Automates how to make method calls remote, so that they are the same as if calling a local method.
 * <p>
 * Supports the following:
 * <ul>
 * <li>Client to Server
 * <li>Server to Client
 * <li>Broadcasting from server or clients to 1+/all others.
 * </ul>
 * Independent of communication layer, transmission layer, and serialization.
 */
package com.viaoa.remote;
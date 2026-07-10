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
 * Metadata for importing, matching, and linking plain Java object data into OAObject models.
 * <p>
 * POJO metadata describes external properties, one-to-one and one-to-many links, foreign-key links, import-match
 * fields, unique-link rules, and reference relationships. It is used by the POJO loader to create or merge OAObjects
 * while preserving model identity and relationship semantics.
 * </p>
 *
 * @see com.viaoa.metadata.pojo.OAObjectPojoLoader
 * @see com.viaoa.metadata.pojo.Pojo
 */
package com.viaoa.metadata.pojo;

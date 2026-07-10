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
 * OA model metadata annotations.
 * <p>
 * These annotations describe the structure and behavior of generated and
 * hand-written {@link com.viaoa.object.OAObject} model classes. The OA runtime
 * reads them into metadata objects such as
 * {@link com.viaoa.metadata.OAObjectInfo},
 * {@link com.viaoa.metadata.OAPropertyInfo},
 * {@link com.viaoa.metadata.OALinkInfo}, and
 * {@link com.viaoa.metadata.OACalcInfo}.
 * <p>
 * Annotation metadata defines class behavior, properties, links, calculated
 * values, callbacks, trigger methods, datasource tables/columns/indexes,
 * foreign keys, link tables, and UI/code-generation hints. OABuilder-generated
 * models use this package as the declarative contract that OA services execute
 * at runtime.
 *
 * @see com.viaoa.annotation.OAClass
 * @see com.viaoa.annotation.OAProperty
 * @see com.viaoa.annotation.OAOne
 * @see com.viaoa.annotation.OAMany
 * @see com.viaoa.annotation.OAObjCallback
 * @see com.viaoa.annotation.OATriggerMethod
 */
package com.viaoa.annotation;

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
package com.viaoa.comm.io;

import com.viaoa.annotation.OAClass;
import com.viaoa.object.OAObject;

/**
 * Placeholder OAObject used during deserialization when a serialized stream
 * references a class that no longer exists or cannot be resolved.
 *
 * <p>This class is instantiated by {@link OAObjectInputStream} whenever an
 * obsolete class name is encountered. It allows the deserialization process
 * to complete without errors, while ensuring that the missing object's data
 * does not reintroduce invalid or outdated state.</p>
 *
 * <p>The {@code @OAClass} settings disable all persistence, caching, and
 * initialization behaviors so that IODummy instances exist only as inert
 * placeholders within the object graph.</p>
 *
 * <p>After creation, {@link com.viaoa.object.OAObjectPropertyDelegate#clearProperties}
 * is invoked to remove any lingering property values, ensuring that no
 * meaningful data remains from the missing class.</p>
 */
@OAClass (addToCache = false, initialize = false, localOnly = true, useDataSource = false)
public class IODummy extends OAObject {
    private static final long serialVersionUID = 1L; // internally used by Java Serialization to identify this version of OAObject.

    
}

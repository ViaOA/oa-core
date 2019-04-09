/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.comm.io;

import com.viaoa.annotation.OAClass;
import com.viaoa.object.OAObject;

/**
 * Dummy class that is used as a holder when reading an objectInputStream that has classes
 * that no longer exist.
 * @author vvia
 *
 */
@OAClass (addToCache = false, initialize = false, localOnly = true, useDataSource = false)
public class IODummy extends OAObject {
    private static final long serialVersionUID = 1L; // internally used by Java Serialization to identify this version of OAObject.

    
}

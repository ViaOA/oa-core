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
package com.viaoa.datasource.jdbc.db;

import com.viaoa.object.OAObjectKey;

/**
 * Internally used for managing M2M relationships.
 * @author vvia
 *
 */
public class ManyToMany {

    public OAObjectKey ok1;
    public OAObjectKey ok2;

    public ManyToMany(OAObjectKey ok1, OAObjectKey ok2) {
        this.ok1 = ok1;
        this.ok2 = ok2;
    }
}

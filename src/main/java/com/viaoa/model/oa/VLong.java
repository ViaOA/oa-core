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
package com.viaoa.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;

@OAClass(
    shortName = "long",
    displayName = "Long",
    displayProperty = "value",
    sortProperty = "value",
    localOnly = true
)
public class VLong extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VLong.class.getName());
    
    public static final String P_Value = "Value";
    
    private long value;
    
    @OAProperty(displayLength = 3)
    public long getValue() {
        return value;
    }
    public void setValue(long newValue) {
        fireBeforePropertyChange(P_Value, this.value, newValue);
        long old = value;
        this.value = newValue;
        firePropertyChange(P_Value, old, this.value);
    }
}

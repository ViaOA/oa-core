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
import com.viaoa.util.OADate;

@OAClass(
    shortName = "date",
    displayName = "Date",
    displayProperty = "value",
    sortProperty = "value",
    localOnly = true
)
public class VDate extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VDate.class.getName());
    
    public static final String P_Value = "Value";
    
    private OADate value;
    
    @OAProperty(displayLength = 8)
    public OADate getValue() {
        return value;
    }
    public void setValue(OADate newValue) {
        fireBeforePropertyChange(P_Value, this.value, newValue);
        OADate old = value;
        this.value = newValue;
        firePropertyChange(P_Value, old, this.value);
    }
}

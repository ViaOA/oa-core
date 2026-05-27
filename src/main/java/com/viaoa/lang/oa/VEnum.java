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
package com.viaoa.lang.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;


/*qqqqqqqqqqqqqqqqqqq
CODEX

 1. src/main/java/com/viaoa/model/oa/VEnum.java:48 and src/main/java/com/viaoa/model/oa/VNameValue.java:46 setName

  - Concrete bug: fireBeforePropertyChange(P_Name, ...) passes the current value field as the old value instead of the
    current name.
  - Runtime/tooling scenario: a Hub listener, trigger, validation callback, sync/change tracker, or UI binding listens
    to beforePropertyChange for Name. It receives oldValue as an int for VEnum, or the previous Value string for
    VNameValue, instead of the previous name.
  - Why this violates OA/OABuilder/OG model semantics: value-wrapper model objects still participate in OAObject event
    semantics. BEFORE property events must describe the property being changed, or validation/event consumers can make
    decisions using wrong metadata state.
  - Minimal fix direction: in both setters, pass this.name as the old value to fireBeforePropertyChange(P_Name,
    oldName, newValue), matching the after-event old value.
  - Suggested CODEX comment location: VEnum.setName before line 49 and VNameValue.setName before line 47.


*/

@OAClass(
    shortName = "en",
    displayName = "Enum",
    displayProperty = "name",
    sortProperty = "value",
    localOnly = true,
    useDataSource = false
)
public class VEnum extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VEnum.class.getName());
    
    public static final String P_Name = "Name";
    public static final String P_Display = "Display";
    public static final String P_Value = "Value";
    
    private String name;
    private String display;
    private int value;

    @OAProperty(displayLength = 12)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        fireBeforePropertyChange(P_Name, this.value, newValue);
        String old = name;
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(displayLength = 16)
    public String getDisplay() {
        return display;
    }
    public void setDisplay(String newValue) {
        fireBeforePropertyChange(P_Display, this.display, newValue);
        String old = display;
        this.display = newValue;
        firePropertyChange(P_Display, old, this.display);
    }
    
    @OAProperty(displayLength = 4)
    public int getValue() {
        return value;
    }
    public void setValue(int newValue) {
        fireBeforePropertyChange(P_Value, this.value, newValue);
        int old = value;
        this.value = newValue;
        firePropertyChange(P_Value, old, this.value);
    }
}

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
package com.viaoa.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;


/*qqqqqqqqqqqqqqqqqqq
CODEX

3. src/main/java/com/viaoa/model/oa/VInteger.java:38 and src/main/java/com/viaoa/model/oa/VBoolean.java:31 public
     mutable value fields

  - Concrete bug: value is public in VInteger and VBoolean, unlike the other wrapper classes. Direct assignment
    bypasses fireBeforePropertyChange, firePropertyChange, changed-state tracking, Hub listeners, triggers, sync/
    change propagation, and UI binding notifications.
  - Runtime/tooling scenario: OA tooling/test/model code can do vint.value = 5 or vbool.value = true; getValue()
    reflects the new value, but OAObject event/change semantics never ran.
  - Why this violates OA/OABuilder/OG model semantics: OA model objects must mutate through property contracts so
    metadata, Hub bindings, triggers, and object lifecycle services observe the change.
  - Minimal fix direction: make the fields private and require setValue; if direct field access is intentionally
    supported, document it as non-OA-observable and avoid using these wrappers in observable model paths.
  - Suggested CODEX comment location: the value field declarations in VInteger and VBoolean.

4. src/main/java/com/viaoa/model/oa/VInteger.java:46 inc/dec/add/sub

  - Concrete bug: arithmetic helpers use primitive int arithmetic with silent overflow.
  - Runtime/tooling scenario: a counter-style VInteger used in Hub/model tooling reaches Integer.MAX_VALUE; inc()
    wraps to Integer.MIN_VALUE while firing a normal successful value change.
  - Why this violates OA/OABuilder/OG model semantics: helper methods should not silently produce a semantically wrong
    model value while reporting a valid committed property change.
  - Minimal fix direction: either document wraparound as the explicit contract, or use checked arithmetic / visible
    failure for overflow.
  - Suggested CODEX comment location: VInteger.inc, dec, add, and sub.



*/


@OAClass(
    shortName = "int",
    displayName = "Integer",
    displayProperty = "value",
    sortProperty = "value",
    localOnly = true,
    useDataSource = false
)
public class VInteger extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VInteger.class.getName());
    
    public static final String P_Value = "Value";
    
    public int value;
    
    public VInteger() {
    }
    public VInteger(int x) {
        setValue(x);
    }
    
    public void inc() {
        setValue(value+1);
    }
    public void dec() {
        setValue(value-1);
    }
    public void add(int x) {
        setValue(value+x);
    }
    public void sub(int x) {
        setValue(value-x);
    }
    
    @OAProperty(displayLength = 3)
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

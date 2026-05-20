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

2. src/main/java/com/viaoa/model/oa/VBoolean.java:24 @OAClass

  - Concrete bug: VBoolean declares shortName = "int" and displayName = "Integer".
  - Runtime/tooling scenario: metadata, UI/model tooling, generated blueprint hints, object display, or class metadata
    inspection sees the Boolean wrapper as an Integer wrapper. VInteger already uses the same short name/display
    concept.
  - Why this violates OA/OABuilder/OG model semantics: model metadata must represent the semantic type correctly.
    Boolean values advertised as Integer can produce wrong generated metadata, display labels, route keys, or model
    analysis output.
  - Minimal fix direction: use Boolean-specific metadata, likely shortName = "boolean" or "bool" and displayName =
    "Boolean", consistent with the rest of the value wrappers.
  - Suggested CODEX comment location: VBoolean class annotation.



*/


@OAClass(shortName = "int", displayName = "Integer", displayProperty = "value", sortProperty = "value", localOnly = true, useDataSource = false)
public class VBoolean extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(VBoolean.class.getName());

	public static final String P_Value = "Value";

	public boolean value;

	public VBoolean() {
	}

	public VBoolean(boolean x) {
		setValue(x);
	}

	@OAProperty(displayLength = 3)
	public boolean getValue() {
		return value;
	}

	public void setValue(boolean newValue) {
		fireBeforePropertyChange(P_Value, this.value, newValue);
		boolean old = value;
		this.value = newValue;
		firePropertyChange(P_Value, old, this.value);
	}
}

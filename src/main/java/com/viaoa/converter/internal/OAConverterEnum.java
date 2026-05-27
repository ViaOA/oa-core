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
package com.viaoa.converter.internal;

/*qqqqqqqqqqqqqqqqqq
CODEX

#3 — OAConverterEnum.convert(...)

  Concrete bug: any Enum instance is returned without checking that it belongs to the target enum class.

  Runtime scenario: converting OtherEnum.X to TargetEnum.class returns OtherEnum.X, even though the caller requested
  TargetEnum.

  Why this violates converter semantics: converter output must match the requested target type. This can corrupt OA
  property values or fail later with a ClassCastException.

  Minimal fix direction: only return fromValue when thisClass.isInstance(fromValue) or fromValue.getClass() ==
  thisClass; otherwise convert by name/ordinal or return null.

*/


/**
 * Converter for transforming values into {@link Enum} instances and formatting
 * them into their canonical {@link Enum#name()} representation.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types when converting to an Enum:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>Enum instance of the same type → returned directly</li>
 *     <li>{@link String} → matched against {@link Enum#name()} ignoring case</li>
 *     <li>{@link Number} → treated as ordinal index</li>
 * </ul>
 *
 * <h3>String Conversion & Round-Trip Behavior</h3>
 * Formatting always uses {@link Enum#name()}:
 * <ul>
 *     <li>Case-preserved output (usually uppercase)</li>
 *     <li>Canonical and decodable format for re-conversion</li>
 *     <li>If Enum is {@code null}, returns empty string {@code ""}</li>
 * </ul>
 *
 * <p>This converter enforces type safety by only returning Enum values that are
 * members of the requested Enum class, and provides predictable fallback behavior
 * when values are out of range or invalid.</p>
 *
 * @see OAConverterInterface
 * @see Enum
 */
public class OAConverterEnum implements OAConverterInterface<Enum> {
    
    /**
     * Converts the supplied value to the specified Enum type.
     *
     * @param thisClass target Enum type
     * @param fromValue source value to convert; may be {@code null}
     * @param fmt ignored for this converter
     * @return matching Enum value, or {@code null} if not convertible
     */	
    public Enum convert(Class<Enum> thisClass, Object fromValue, String fmt) {
        if (fromValue == null || thisClass == null) return null;

        if (fromValue instanceof Enum) return (Enum) fromValue;
        if (!thisClass.isEnum()) return null;
        
        Object[] enums = thisClass.getEnumConstants();

        if (fromValue instanceof String) {
        	String s = ((String) fromValue).trim();
            for (Object obj : enums) {
                Enum e = (Enum) obj;
                String s2 = e.name();
                if (s.equalsIgnoreCase(s2)) return e;
            }
        }
        
        if (fromValue instanceof Number) {
            int ordinal = ((Number) fromValue).intValue();
            return (ordinal >= 0 && ordinal < enums.length) ? (Enum) enums[ordinal] : null;
        }
        return null;
    }

    /**
     * Converts an Enum value to its canonical {@link Enum#name()} string.
     *
     * @param fromValue Enum value; may be {@code null}
     * @param fmt ignored
     * @return {@code fromValue.name()} or {@code ""} if {@code null}
     */    
	@Override
	public String convertToString(Enum fromValue, String fmt) {
		if (fromValue == null) return "";
		return fromValue.name();
	}
}

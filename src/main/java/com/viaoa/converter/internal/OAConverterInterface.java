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

import com.viaoa.converter.OAConverter;

/**
 * Generic interface for converting values into a specific target type {@code T}
 * and optionally formatting values of that type into a {@link String}.
 * <p>
 * Implementations of this interface are registered with and invoked through
 * {@link com.viaoa.converter.OAConverter}, which selects the appropriate converter
 * based on the desired target class.
 * </p>
 *
 * <h3>Usage Notes</h3>
 * <ul>
 *   <li>{@link #convert(Class, Object, String)} is used to parse or transform an
 *       arbitrary value into a type {@code T} supported by this converter.</li>
 *   <li>{@link #convertToString(Object, String)} is used to format a value of
 *       type {@code T} as a string, using optional formatting rules.</li>
 *   <li>Converters should return {@code null} if a conversion is not supported
 *       or cannot be performed.</li>
 *   <li>Converters are expected to be pure and thread-safe.</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * public class OAConverterInteger implements OAConverterInterface<Integer> {
 *     @Override
 *     public Integer convert(Class<Integer> thisClass, Object fromValue, String fmt) {
 *         if (fromValue == null) return null;
 *         if (fromValue instanceof Number) {
 *             return ((Number) fromValue).intValue();
 *         }
 *         return Integer.valueOf(fromValue.toString());
 *     }
 *
 *     @Override
 *     public String convertToString(Integer value, String fmt) {
 *         return (value == null) ? "" : value.toString();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type that this converter converts <em>into</em>
 *
 * @see com.viaoa.converter.OAConverter
 */
public interface OAConverterInterface<T> {

    /**
     * Converts a value from any supported source type into the target type {@code T}.
     * <p>
     * The {@code thisClass} argument exists to support converters for subclasses or
     * specialized forms of {@code T} where runtime type awareness matters.
     * </p>
     *
     * @param thisClass the specific target class of the desired conversion result
     * @param fromValue the source value to be converted; may be {@code null}
     * @param fmt       optional formatting or parsing mask; may be {@code null}
     * @return the converted instance of type {@code T}, or {@code null} if conversion fails
     */
    T convert(Class<T> thisClass, Object fromValue, String fmt);

    /**
     * Converts a value of type {@code T} into a textual representation.
     *
     * @param fromValue the value to convert; may be {@code null}
     * @param fmt       optional formatting definition; may be {@code null}
     * @return a formatted string value, or an empty string if {@code fromValue} is {@code null}
     */
    String convertToString(T fromValue, String fmt);
}


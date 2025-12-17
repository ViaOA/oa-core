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
package com.viaoa.converter;

/**
 * Converter for transforming values into {@link Class} objects and formatting them
 * into fully-qualified class name {@link String} representations.
 *
 * <h3>Conversion Rules</h3>
 * Supported input types when converting to {@link Class}:
 * <ul>
 *     <li>{@code null} → {@code null}</li>
 *     <li>{@link Class} instance → returned directly</li>
 *     <li>{@link String} → resolved via {@link Class#forName(String)}</li>
 * </ul>
 *
 * <p>If direct class resolution fails due to classloader constraints, the current
 * thread's context classloader is used as a fallback. Any resolution failures
 * return {@code null} silently.</p>
 *
 * <h3>Formatting Rules</h3>
 * <ul>
 *     <li>{@code null} → empty string {@code ""}</li>
 *     <li>{@link Class#getName()} used for round-trip compatibility</li>
 *     <li>{@code fmt} parameter ignored</li>
 * </ul>
 *
 * @see OAConverterInterface
 * @see Class
 */
public class OAConverterClass implements OAConverterInterface<Class> {

    /**
     * Converts the supplied value to a {@link Class} instance.
     *
     * @param thisClass expected type ({@code Class.class})
     * @param fromValue source value; may be {@code null}
     * @param fmt ignored
     * @return resolved {@link Class} or {@code null} if unable to resolve
     */	
	@Override
	public Class convert(Class<Class> thisClass, Object fromValue, String fmt) {
		if (fromValue == null) return null;
		if (fromValue instanceof Class) return (Class) fromValue;
		if (fromValue instanceof String) {
			String s = ((String) fromValue).trim();
			try {
			    return Class.forName(s);
			} catch (ClassNotFoundException ex) {
			    try {
			        return Thread.currentThread().getContextClassLoader().loadClass(s);
			    } catch (Exception ignored) {
			    }
			}
			return null;			
		}
		return null;
	}

    /**
     * Converts a {@link Class} into a fully-qualified class name suitable for
     * storage or UI display.
     *
     * @param fromValue class to convert; may be {@code null}
     * @param fmt ignored
     * @return fully-qualified class name or {@code ""} if {@code null}
     */
	@Override
	public String convertToString(Class fromValue, String fmt) {
		return (fromValue == null) ? "" : fromValue.getName();
	}
}

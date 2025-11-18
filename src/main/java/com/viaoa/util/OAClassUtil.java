/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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
package com.viaoa.util;

/**
 * Utility methods for extracting the simple class name and package name from
 * a {@link Class} instance. These helpers provide null-safe access to commonly
 * used reflection information without requiring full class metadata. <p>
 *
 * {@link #getClassName(Class)} returns the simple (unqualified) class name,
 * while {@link #getPackageName(Class)} returns the fully qualified package
 * portion of the class name. Both methods return {@code null} when the
 * supplied class reference is {@code null}.
 */
public class OAClassUtil {

	public static String getClassName(Class c) {
		if (c == null) {
			return null;
		}
		return c.getSimpleName();
	}

	public static String getPackageName(Class c) {
		if (c == null) {
			return null;
		}
		String s = c.getName();
		int x = s.lastIndexOf('.');
		if (x > 0) {
			s = s.substring(0, x);
		}
		return s;
	}
	
}

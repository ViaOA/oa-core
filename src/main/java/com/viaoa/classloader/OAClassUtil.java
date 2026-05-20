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
package com.viaoa.classloader;

/*qqqqqqqqqqqqqqqqqqqqqqq
CODEX

4. src/main/java/com/viaoa/classloader/OAClassUtil.java / getPackageName(Class c)

  Concrete bug: classes in the default package return their class name as the package name.

  Runtime scenario: OAClassUtil.getPackageName(DefaultPackageClass.class) gets c.getName() as "DefaultPackageClass".
  Since lastIndexOf('.') is -1, the method returns the unchanged string. That is not a package name.

  Why this violates OA/OG classloader semantics: package names are used for graph/package routing, metadata grouping,
  codegen/tooling reports, and resource resolution. Returning a class name as a package name can route analysis/
  tooling to a bogus package root instead of representing “no package”.

  Minimal fix direction: if there is no dot, return "" or null according to the package contract. c.getPackage() is
  also a safer source when available.

  Suggested CODEX comment location: line 66 before the lastIndexOf('.') handling.


*/

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

	/**
	 * Returns the simple (unqualified) name of the specified class.
	 *
	 * If the supplied class reference is {@code null}, this method returns {@code null}.
	 *
	 * @param c the {@link Class} to inspect
	 * @return the simple class name, or {@code null} if the class is {@code null}
	 */
	public static String getClassName(Class c) {
		if (c == null) {
			return null;
		}
		return c.getSimpleName();
	}

	/**
	 * Returns the package name portion of the specified class.
	 *
	 * If the supplied class reference is {@code null}, this method returns {@code null}.
	 * The returned value is derived from the fully qualified class name.
	 *
	 * @param c the {@link Class} to inspect
	 * @return the package name, or {@code null} if the class is {@code null}
	 */
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

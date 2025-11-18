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
 * Convenience helper for constructing a root {@link OAPropertyPath}. This
 * delegates directly to the corresponding {@link OAPropertyPath} constructor
 * and is used when parsing a property-path string that begins with a leading
 * class qualifier (for example, {@code "[Customer].orders.item"}). <p>
 *
 * The supplied {@code packageClass} identifies the package context in which
 * the root class name will be resolved. The method performs no additional
 * parsing or validation beyond what {@link OAPropertyPath} already provides
 * and simply returns the constructed instance. The class is stateless and
 * entirely thread-safe.
 */
public class OAPropertyPathDelegate {

	/**
	 * parse a propertyPath that has a leading "[ClassName]."
	 * 
	 * @param packageClass class that the from class is in the same package as.
	 */
	public static OAPropertyPath createRootPropertyPath(String sPropPath, Class packageClass) throws Exception {
		OAPropertyPath pp = new OAPropertyPath(packageClass, sPropPath);
		return pp;
	}

}

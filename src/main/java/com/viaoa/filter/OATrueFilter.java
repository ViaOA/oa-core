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
package com.viaoa.filter;

import com.viaoa.path.OAPath;

/**
 * Filter that always returns {@code true} or uses and equal to true 
 * when using OAPath.  
 *
 */
public class OATrueFilter extends OAEqualFilter {

	private boolean bAlwaysTrue;
	
	/**
	 * Creates a filter that always returns {@code Boolean.TRUE}.
	 */
	public OATrueFilter() {
		super(Boolean.TRUE);
		bAlwaysTrue = true;
	}

	/**
	 * Creates a filter that evaluates the value resolved from the supplied
	 * property path string as {@code Boolean.TRUE}.
	 *
	 * @param pp the property path expression used to obtain the value
	 */
	public OATrueFilter(String pp) {
		super(pp, Boolean.TRUE);
	}

	/**
	 * Creates a filter that evaluates the value resolved from the supplied
	 * {@link OAPath} as {@code Boolean.TRUE}.
	 *
	 * @param pp the property path used to obtain the value
	 */
	public OATrueFilter(OAPath pp) {
		super(pp, Boolean.TRUE);
	}

	@Override
	public boolean isUsed(Object obj) {
		if (bAlwaysTrue) return true;
		return super.isUsed(obj);
	}

}

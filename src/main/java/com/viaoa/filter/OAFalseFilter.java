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
 * Filter that always returns {@code false} or uses and equal to false 
 * when using OAPath.  
 *
 */
public class OAFalseFilter extends OAEqualFilter {
	
	private boolean bAlwaysFalse;

	/**
	 * Creates a filter that always returns {@code Boolean.FALSE}.
	 */
	public OAFalseFilter() {
		super(Boolean.FALSE);
		bAlwaysFalse = true;
	}

	/**
	 * Creates a filter that always checks for path to be equal to {@code false}.
	 *
	 * @param pp the property-path expression used to retrieve the evaluated value
	 */
	public OAFalseFilter(String pp) {
		super(pp, Boolean.FALSE);
	}

	/**
	 * Creates a filter that always checks for path to be equal to {@code false}.
	 *
	 * @param pp the property path used to access the evaluated value
	 */
	public OAFalseFilter(OAPath pp) {
		super(pp, Boolean.FALSE);
	}

	@Override
	public boolean isUsed(Object obj) {
		if (bAlwaysFalse) return false;
		return super.isUsed(obj);
	}
	
}

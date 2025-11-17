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
package com.viaoa.filter;

import com.viaoa.util.OAPropertyPath;

/**
 * Filter that always returns {@code false}.  This is a utility filter used
 * when an empty result set is required, or as a placeholder to disable
 * another filter dynamically.
 *
 * <p>
 * Since this filter never selects any object, it is often used
 * programmatically to suppress results or short-circuit other filtering logic.
 * </p>
 */
public class OAFalseFilter extends OAEqualFilter {

	public OAFalseFilter() {
		super(Boolean.FALSE);
	}

	public OAFalseFilter(String pp) {
		super(pp, Boolean.FALSE);
	}

	public OAFalseFilter(OAPropertyPath pp) {
		super(pp, Boolean.FALSE);
	}

}

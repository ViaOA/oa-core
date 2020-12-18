/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.util;

import java.io.Serializable;

import com.viaoa.datasource.OASelect;

/**
 * Used to filter a collection of TYPE objects.
 *
 * @author vvia
 */
@FunctionalInterface
public interface OAFilter<T> extends Serializable {
	boolean isUsed(T obj);

	/**
	 * Callback, that allows a Filter to be called by Select before it is performed, so that the filter can be done by the datasource that
	 * performs the select/query.
	 * <p>
	 *
	 * @param select oaselect that is using this select, before it runs the query on the datasource.
	 * @return true (default) if this filter should still be used from the select results.
	 */
	default boolean updateSelect(OASelect select) {
		return true;
	}
}

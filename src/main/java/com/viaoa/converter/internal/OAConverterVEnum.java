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

import com.viaoa.lang.OAString;
import com.viaoa.lang.oa.VEnum;

public class OAConverterVEnum implements OAConverterInterface<VEnum> {
    
    public VEnum convert(Class<VEnum> thisClass, Object fromValue, String fmt) {
    	if (fromValue instanceof VEnum) return (VEnum) fromValue;
        return null;
    }

	@Override
	public String convertToString(VEnum venum, String fmt) {
		if (venum == null) return "";
		return OAString.format(venum.getName(), fmt);
	}
}

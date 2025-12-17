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
package com.viaoa.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB XML adapter used to marshal and unmarshal object identifier values
 * as simple {@link String} representations.
 */
public class OAObjectIdXmlAdapter extends XmlAdapter<String, String> {

	/**
	 * Converts an object identifier value into its XML string representation.
	 *
	 * @param id the identifier value to marshal
	 * @return the marshaled XML string value, or {@code null}
	 * @throws Exception if marshalling fails
	 */
    @Override
    public String marshal(String id) throws Exception {
        return null;
        //return id+"XXX";
    }

    /**
     * Converts an XML string value into an object identifier.
     *
     * @param s the XML string value
     * @return the unmarshaled identifier value
     * @throws Exception if unmarshalling fails
     */
    @Override
    public String unmarshal(String s) throws Exception {
        return s;
    }
    
}

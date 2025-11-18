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

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB {@link XmlAdapter} for marshalling and unmarshalling integer identifier
 * values used by OA. The adapter converts an {@link Integer} to its textual
 * representation for XML output, returning the string {@code "0"} when the
 * value is {@code null}. During unmarshalling, a {@code null} string is mapped
 * to {@code 0}, and all other non-null strings are parsed as integers. <p>
 *
 * This adapter enforces OA's convention that a missing or unassigned identifier
 * is represented as {@code 0} in XML. It does not preserve {@code null} values
 * and will throw {@link NumberFormatException} if supplied with a non-numeric
 * string. The class is stateless and thread-safe.
 */
public class OAIdXmlAdapter extends XmlAdapter<String, Integer> {

    @Override
    public String marshal(Integer id) throws Exception {
        if (id == null) return "0";
        return ""+id.intValue();
    }

    @Override
    public Integer unmarshal(String s) throws Exception {
        if (s == null) return (int) 0;
        return Integer.parseInt(s);
    }
    
}

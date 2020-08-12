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
package com.viaoa.converter;

import java.util.*;

import com.viaoa.util.OATimeZone;

/**
 * Conversions between TimeZone and String.  
 * @see OATimeZone
*/
public class OAConverterTimeZone implements OAConverterInterface {

    public Object convert(Class clazz, Object value, String fmt) {
        if (clazz == null) return null;
        if (clazz.equals(TimeZone.class)) return convertToTimeZone(value, fmt);
        if (value != null && value instanceof TimeZone) return convertFromTimeZone(clazz, (TimeZone) value, fmt);
        return null;
    }

    protected TimeZone convertToTimeZone(Object value, String fmt) {
        if (value instanceof TimeZone) return (TimeZone) value;
        if (value == null) return null;
        TimeZone tz = null;
        if (value instanceof String) {
            tz = OATimeZone.getTimeZone((String) value);
        }
        return tz;
    }

    protected Object convertFromTimeZone(Class toClass, TimeZone timeZone, String fmt) {
        if (toClass.equals(String.class)) {
            OATimeZone.TZ tz = OATimeZone.getOATimeZone(timeZone);
            return tz.getDisplay();
        }
        return null;
    }
}




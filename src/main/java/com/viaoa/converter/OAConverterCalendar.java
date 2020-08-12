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

import com.viaoa.util.*;

import java.util.*;

/**
    Convert to/from a Calendar value.
*/
public class OAConverterCalendar implements OAConverterInterface {


    /**
        Convert to/from a OADateTime value.
        @return Object of type clazz if conversion can be done, else null.
    */
    public Object convert(Class clazz, Object value, String fmt) {
        if (clazz == null) return null;
        if (clazz.isAssignableFrom(Calendar.class)) {
            OADateTime dt = convertToOADateTime(value, fmt);
            if (dt == null) return null;
            return dt.getCalendar();
        }
        if (value instanceof Calendar) {
            OADateTime dt = new OADateTime((Calendar) value);
            return convertFromOADateTime(clazz, dt, fmt);
        }
        return null;
    }
    
    protected OADateTime convertToOADateTime(Object value, String fmt) {
        if (value == null) return null;
        if (value instanceof OADateTime) return (OADateTime) value;
        if (value instanceof OADate) return new OADateTime((OADate)value);
        if (value instanceof OATime) return new OADateTime((OATime)value);
        if (value instanceof String) {
            return OADateTime.valueOf((String)value, fmt);
        }
        if (value instanceof java.sql.Time) {
            return new OADateTime((java.sql.Time) value);
        }
        if (value instanceof Date) {
            return new OADateTime((Date) value);
        }
        if (value instanceof byte[]) {
        	return new OADateTime(new java.math.BigInteger((byte[]) value).longValue());
        }
        if (value instanceof Number) {
        	return new OADateTime(((Number)value).longValue());
        }
        
        return null;
    }

    protected Object convertFromOADateTime(Class toClass, OADateTime dtValue, String fmt) {
        if (dtValue == null || toClass == null) return null;
        if (toClass.equals(String.class)) {
            return ((OADateTime) dtValue).toString(fmt);
        }
        if (toClass.equals(OADate.class)) {
            return new OADate(dtValue);
        }
        if (toClass.equals(OATime.class)) {
            return new OATime(dtValue);
        }
        if (Number.class.isAssignableFrom(toClass)) {
            return new Long(dtValue.getDate().getTime());
        }
        return null;
    }

}


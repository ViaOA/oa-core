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
import java.text.*;
import java.awt.*;
import java.math.*;

/**
    Convert to/from a Number value.

    <br>
    <b>Converting the following to a Number</b>
    <ul>
    <li>null returns a number with value = 0
    <li>Any type of Number
    <li>String, using an optional format.
    <li>Color
    <li>Character
    <li>Boolean
    <li>Rectangle by encoding x,y,w,h in 16bit positions (64 bits required).
    <li>All others value will return null.
    </ul>
    <br>
    <b>Converting a Number to any of the following</b>
    <ul>
    <li>String, using an optional format.
    </ul>

    <p>
    Format String Symbol Meaning (same as DecimalFormat)
    <pre>
    0      a digit, if no digit exists, then '0' will be used. 
                ex: '000' for 38 = '038'
    #      a digit, zero shows as absent
                ex: '#' for 8204 = '8204'
    .      placeholder for decimal separator
    ,      placeholder for grouping separator.
    ;      separates formats.
    -      default negative prefix.
    %      multiply by 100 and show as percentage
    (\u2030) � multiply by 1000 and show as per mille
    (\u00A4) � currency sign; replaced by currency symbol; if
            doubled, replaced by international currency symbol.
            If present in a pattern, the monetary decimal separator
            is used instead of the decimal separator.
    X      any other characters can be used in the prefix or suffix
    '      used to quote special characters in a prefix or suffix.
    
    Examples:
    IntegerFormat     = #,###
    DecimalFormat     = #,##0.00
    MoneyFormat       = \u00A4#,##0.00
    BooleanFormat     = true;false;null
    
    ALSO** support for OAString.format 
    
    </pre>

    <p>
    NOTE: this also does rounding when digits are truncated.  
    @see OAConverter
    @see OAString#format(double, String)
  */
public class OAConverterNumber implements OAConverterInterface {

    /** 
        Convert to/from a Number value.
        @return Object of type clazz if conversion can be done, else null.
    */
    public Object convert(Class clazz, Object value, String fmt) {
        if (clazz == null) return null;

        if (value != null && value.getClass().equals(clazz)) return value;
        if (clazz.isPrimitive()) {
            clazz = OAReflect.getPrimitiveClassWrapper(clazz);
        }
        
        if (Number.class.isAssignableFrom(clazz)) return convertToNumber(clazz, value, fmt);
        if (value != null && value instanceof Number) return convertFromNumber(clazz, (Number) value, fmt);
        return null;
    }        

    /** 
        Returns Number subclass to match clazz parameter, 0 if value is null, or null if it can not be parsed. 
    */
    protected Number convertToNumber(Class clazz, Object value, String fmt) {
        Number num = null;
        if (value == null) num = new Double(0.0D);
        else if (value instanceof Number) num = (Number) value;
        else if (value instanceof String) {
            String sValue = (String) value;
            if (sValue.length() == 0) num = new Double(0.0D);  
            else {
                if (fmt == null) fmt = "#,###";
                else fmt = fmt.replace('$', '\u00A4');

                for (int i=0; i<3; i++) {
                    FormatPool fp = getFormatter(fmt);
                    try {
                        ParsePosition pp = new ParsePosition(0);
                        num = ((DecimalFormat)fp.fmt).parse(sValue, pp);
                        if (pp.getIndex() == sValue.length()) break;
                        num = null;
                    }
                    catch (Exception e) {
                    }
                    finally {
                        fp.used = false;
                    }
                    
                    if (i == 0) sValue = cleanNumber(sValue);
                    else if (i == 1) fmt = "#,###";
                }
            }
        }
        else if (value instanceof Character) {
            num = new Integer( ((Character)value).charValue() );
        }
        else if (value instanceof Boolean) {
            num = new Integer( ((Boolean)value).booleanValue() == true ? 1 : 0 );
        }
        else if (value instanceof Rectangle) {
            Rectangle r = (Rectangle) value;
            long l = 0L;
            
            l += ((long)r.x) << 48;
            l += ((long)r.y) << 32;
            l += ((long)r.width) << 16;
            l += ((long)r.height);
            num = new Long(l);
        }
        else if (value instanceof Color) {
            Color c = (Color) value;
            num = new Long(c.getRGB());
        }
        else if (value instanceof Enum) {
            Enum e = (Enum) value;
            num = new Long(e.ordinal());
        }

		if (value instanceof byte[]) {
			num = new java.math.BigInteger((byte[]) value);
		}
        
        if (num != null && clazz != null) {
            if (num.getClass().equals(clazz));
            else if (clazz.equals(Integer.class)) num = new Integer(num.intValue());
            else if (clazz.equals(Long.class)) num = new Long(num.longValue());
            else if (clazz.equals(BigDecimal.class)) num = new BigDecimal(num.toString());
            else if (clazz.equals(Double.class)) num = new Double(num.doubleValue());
            else if (clazz.equals(Float.class)) num = new Float(num.floatValue());
            else if (clazz.equals(Short.class)) num = new Short(num.shortValue());
            else if (clazz.equals(Byte.class)) num = new Byte(num.byteValue());
        }
        return num;
    }
        
    String cleanNumber(String value) {
        char[] chars = { ',', '$', ' ' };
        for (int j=0; j<chars.length; j++) {
            for (;;) {
                int x = value.indexOf(chars[j]);
                if (x < 0) break;
                value = value.substring(0,x) + value.substring(x+1);
            }
        }
            
        int x = value.length();
        if (x > 0) {
            char c = value.charAt(x-1);
            if (c == 'k' || c == 'K') {
                value = value.substring(0,x-1)+"000";
            }
        }
        return value;
    }


    protected Vector vec = new Vector(5,5);

    protected FormatPool getFormatter(String fmt) {
        FormatPool fp = null;
        synchronized (vec) {
            int x = vec.size();
            int i = 0;
            for ( ; i<x; i++) {
                fp = (FormatPool) vec.elementAt(i);
                if (!fp.used) {
                    ((DecimalFormat)fp.fmt).applyPattern(fmt);
                    break;
                }
            }
            if (i == x) {
                fp = new FormatPool(new DecimalFormat(fmt));
                vec.addElement(fp);
            }
            fp.used = true;
        }
        return fp;
    }

    class FormatPool {
        boolean used;
        Format fmt;
        public FormatPool(Format fmt) {
            this.fmt = fmt;
        }
    }



    protected Object convertFromNumber(Class toClass, Number numValue, String fmt) {
        if (toClass.equals(String.class)) {
            if (fmt == null) return numValue.toString();
            
            if (fmt.length() > 1 && fmt.indexOf('R') >= 0 || fmt.indexOf('L') >= 0 || fmt.indexOf('C') >= 0) {
                return OAString.format(numValue.toString(), fmt);
            }
            
            String s = null;
            FormatPool fp = getFormatter(fmt);
            try {
                s = ((DecimalFormat)fp.fmt).format(numValue);
            }
            finally {
                fp.used = false;
            }
            return s;
        }
        return null;
    }


    public static void main(String[] args) {
        BigDecimal bd = BigDecimal.valueOf(1.23456);
        BigDecimal bd2 = bd.setScale(3, BigDecimal.ROUND_HALF_UP);
        // System.out.println(bd + " -> "+bd2);
        
        DecimalFormat fmt = new DecimalFormat("#.000000000000000");
        // System.out.println(fmt.format(bd));
        
        
        MathContext mc = new MathContext(2); // this will only have 4 digits 
                
        bd = new BigDecimal("125.00");
        System.out.println("==>"+bd.toPlainString());
        System.out.println("==>"+bd.scale());
        
        
        bd = bd.setScale(0);  // 4 decimals
        System.out.println("==>"+bd);
        bd = bd.round(mc);
        System.out.println("==>"+bd);
        

        
        
        bd = bd.multiply(BigDecimal.valueOf(2.1));
        
        bd = bd.setScale(8, BigDecimal.ROUND_HALF_UP);
        System.out.println("==>"+bd);        
        //System.out.println("==>"+bd+", "+bd.setScale(2));        
        
        
        // bd = bd.round(mc);
        
        //bd = bd.setScale(8, BigDecimal.ROUND_HALF_UP);
        bd2 = new BigDecimal(125);
        bd2 = bd2.setScale(4);
        //bd2 = bd2.round(mc);
        
        
        System.out.println("==>"+(bd.equals(bd2))+", bd="+bd+", bd2="+bd2);        
        
    }
    
}












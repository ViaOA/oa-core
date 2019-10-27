package com.viaoa.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;


//qqqqqq does not work with JAXB when using "int" instead of Integer 
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

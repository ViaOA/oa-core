package com.viaoa.jaxb.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class OASecureXmlAdapter extends XmlAdapter<String, String> {
    @Override
    public String marshal(String val) throws Exception {
        if (val == null) return null;
        String s = "********";        
        return s;
    }

    @Override
    public String unmarshal(String val) throws Exception {
        return val;
    }
    
}

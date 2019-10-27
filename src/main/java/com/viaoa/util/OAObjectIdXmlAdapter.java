package com.viaoa.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class OAObjectIdXmlAdapter extends XmlAdapter<String, String> {

    @Override
    public String marshal(String id) throws Exception {
        return null;
        //return id+"XXX";
    }

    @Override
    public String unmarshal(String s) throws Exception {
        return s;
    }
    
}

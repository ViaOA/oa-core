package com.viaoa.jaxb.adapter;

import java.time.Clock;
import java.time.ZoneId;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OAConv;

public class OADoubleXmlAdapter extends XmlAdapter<String, Double> {

    @Override
    public String marshal(Double d) throws Exception {
        if (d == null) return null;

        String s = (String) OAConv.convert(String.class, d);        
        return s;
    }

    @Override
    public Double unmarshal(String val) throws Exception {
        if (val == null) return null;
        
        Double d = (Double) OAConv.convert(Double.class, val);
        
        return d;
    }
    
}

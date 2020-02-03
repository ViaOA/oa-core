package com.viaoa.util.xmladapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OAConv;

public class OADouble2XmlAdapter extends XmlAdapter<String, Double> {

    @Override
    public String marshal(Double d) throws Exception {
        if (d == null) return null;

        String s = (String) OAConv.convert(String.class, d, "#0.00");        
        return s;
    }

    @Override
    public Double unmarshal(String val) throws Exception {
        if (val == null) return null;
        
        Double d = (Double) OAConv.convert(Double.class, val);
        
        return d;
    }
    
    public static void main(String[] args) throws Exception {
        OADouble2XmlAdapter da = new OADouble2XmlAdapter();
        String s = da.marshal(4.2349d);
        int xx = 4;
        xx++;
    }
    
}

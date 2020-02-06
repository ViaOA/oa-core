package com.viaoa.util.xmladapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Used to convert a decimal (double) to a percentage
 * @author vvia
 *
 */
public class OAPercent2XmlAdapter extends XmlAdapter<String, Double> {

    @Override
    public String marshal(Double d) throws Exception {
        if (d == null) return null;

        double dx = d.doubleValue() * 100.0;
        String s = (String) OAConv.convert(String.class, dx, "#0.00");        
        return s;
    }

    @Override
    public Double unmarshal(String val) throws Exception {
        if (val == null) return null;
        
        val = OAString.convert(val, "%", "");
        Double d = (Double) OAConv.convert(Double.class, val);
        d /= 100;
        
        return d;
    }
    
    public static void main(String[] args) throws Exception {
        
        OAPercent2XmlAdapter da = new OAPercent2XmlAdapter();
        String s = da.marshal(4.2349d);
        int xx = 4;
        xx++;
    }
    
}

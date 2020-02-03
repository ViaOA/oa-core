package com.viaoa.util.xmladapter;

import java.time.Clock;
import java.time.ZoneId;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OATime;

public class OATimeXmlAdapter extends XmlAdapter<String, OATime> {

    
    @Override
    public String marshal(OATime dt) throws Exception {
        if (dt == null) return null;

        String s = dt.toString("HH:mm:ss");        
        return s;
    }

    @Override
    public OATime unmarshal(String cal) throws Exception {
        if (cal == null) return null;
        
        OATime dt = new OATime(cal, "HH:mm:ssZ");
        
        return dt;
    }
    
}

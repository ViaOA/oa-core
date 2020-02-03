package com.viaoa.util.xmladapter;

import java.time.Clock;
import java.time.ZoneId;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OADateTime;

public class OADateTimeXmlAdapter extends XmlAdapter<String, OADateTime> {

//qqqqqqqqqqqq need to create for OADate and OATime, OANumber (add to doubles annotation deci digits) qqqqqqqqqqqqqqqqq
   //qqqqqqq need to XML annotate props that use OANumberXmlAdapter
    
    
    @Override
    public String marshal(OADateTime dt) throws Exception {
        if (dt == null) return null;

/*        
        Clock clock = Clock.system(ZoneId.of("Asia/Calcutta"));
        System.out.println(clock.instant());        
        
        
String s2 = dt.toString("yyyy-MM-dd'T'HH:mm:ssZ");        
String s3 = dt.toString("yyyy-MM-dd'T'HH:mm:sszzz");        
String s = s2;
*/        
        String s = dt.toString("yyyy-MM-dd'T'HH:mm:ss");        
        return s;
    }

    @Override
    public OADateTime unmarshal(String cal) throws Exception {
        if (cal == null) return null;
        
        OADateTime dt = new OADateTime(cal, "yyyy-MM-dd'T'HH:mm:ssZ");
        
        return dt;
    }
    
}

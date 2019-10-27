package com.viaoa.hub;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.hub.Hub;


// not needed or used
public class HubXmlAdapter extends XmlAdapter<ArrayList, Hub> {

    @Override
    public ArrayList marshal(Hub v) throws Exception {
        if (v != null) {
            ArrayList lst = new ArrayList();
            for (Object objx : v) {
                lst.add(objx);
            }
            return lst;
        }
        return null;
    }

    @Override
    public Hub unmarshal(ArrayList v) throws Exception {
        Hub hub = new Hub<>();
        if (v != null) {
            for (Object obj : v) {
                hub.add(obj);
            }
        }
        return null;
    }
    
}

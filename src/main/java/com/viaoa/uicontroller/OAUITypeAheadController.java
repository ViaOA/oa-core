package com.viaoa.uicontroller;

import java.util.*;

import com.viaoa.hub.*;
import com.viaoa.hub.HubChangeListener.Type;
import com.viaoa.object.*;
import com.viaoa.util.*;

/**
 * 
 * Used to manage TypeAhead inputs.
 * 
 * @author vince
 */
public abstract class OAUITypeAheadController extends OAUIController {

    private final OATypeAhead typeAhead;

    private OAUIController controlLinkHub;

    public static class TypeAheadValue {
        public String id, display, dropDownDisplay;
        
        public TypeAheadValue(String id, String display, String dropDownDisplay) {
            this.id = id;
            this.display = display;
            if (OAStr.isNotEqual(display, dropDownDisplay)) this.dropDownDisplay = dropDownDisplay;
        }
    }
    
    
    public OAUITypeAheadController(OATypeAhead typeAhead) {
        super(typeAhead.getHub(), null, null, false, Type.HubValid);
        this.typeAhead = typeAhead;
        getLinkUIController();
    }
    
    public OATypeAhead getTypeAhead() {
        return typeAhead;
    }
    
    
    public void reset() {
        OAUIController c = controlLinkHub;
        if (c != null) c.reset();
    }
    
    public void close() {
        OAUIController c = controlLinkHub;
        if (c != null) c.close();
    }
    
    
    protected OAUIController getLinkUIController() {
        if (controlLinkHub != null) return controlLinkHub;
    
        Hub hub = getTypeAhead().getHub();
        Hub hubLink = hub.getLinkHub(true);
        String linkPropertyName = null;
        
        if (hubLink != null) {
            linkPropertyName = hub.getLinkPath(true);
        }
        else {
            Hub hubx = HubDetailDelegate.getMasterHub(hub);
            if (hubx != null) {
                OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterToDetail(hub);
                if (li != null && li.getType() == li.TYPE_ONE) {
                    hubLink = hubx;
                    linkPropertyName = li.getName();
                }
            }
        }

        if (hubLink == null) return null;
        
        controlLinkHub = new OAUIController(hubLink, null, linkPropertyName, true, HubChangeListener.Type.AoNotNull) {
            @Override
            public void updateComponent(Object object) {
                OAUITypeAheadController.this.updateComponent(object);
            }
            @Override
            public void updateLabel(Object object) {
                OAUITypeAheadController.this.updateLabel(object);
            }
        };
        
        return controlLinkHub;
    }
    
    
    
    public List<TypeAheadValue> getTypeAheadValues(final String search) {
        List<TypeAheadValue> al =  new ArrayList<>(); 
        
        OATypeAhead ta = getTypeAhead();
        if (ta == null) return al;
        
        List<OAObject> alObj = ta.search(search);
        if (alObj != null) {
            for (OAObject obj : alObj) {
                TypeAheadValue tav = new TypeAheadValue(obj.getObjectKey().toString(), ta.getDisplayValue(obj), ta.getDropDownDisplayValue(obj));
                al.add(tav);
            }
        }
        return al;
    }

    public Object findObjectUsingId(String id) {
        Object obj = getTypeAhead().findObjectUsingId(id);
        return obj;
    }
 
    
    public String getJson(String search) {
        List<TypeAheadValue> al = getTypeAheadValues(search);
        
        String json = "";
        for (TypeAheadValue tav : al) {
            if (json.length() > 0) json += ", ";
  
            json += "{\"id\":\"" + OAString.escapeJSON(tav.id) + "\"" + 
                    ",\"display\":\"" + OAString.escapeJSON(tav.display) + "\"";
            
            if (OAStr.isNotEmpty(tav.dropDownDisplay)) {
                json += ",\"dropDownDisplay\":\"" + OAString.escapeJSON(tav.dropDownDisplay) + "\""; 
            }
            json += "}";
        }
        return "[" + json + "]";
    }    
    
    
    /** 
     * Called when a change is necessary for UI component. 
     * */
    public abstract void updateComponent(Object object);
    

    public abstract void updateLabel(Object object);
}

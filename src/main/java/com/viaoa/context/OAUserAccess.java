package com.viaoa.context;

import java.util.ArrayList;

import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAPropertyPath;


/**
 * Used to determine if an object is included in a propertyPath from an OAObject/Hub.AO 
 *
 * Has methods to add multiple obj/hub and propertyPaths, so that all are searched to
 * see if an Object is included any any of the root + paths.
 *   
 * @author vvia
 *
 */
public class OAUserAccess {

    private ArrayList<UserAccess> alUserAccess;
    protected static class UserAccess {
        Hub hub;
        OAObject obj;
        
        
        OAPropertyPath pp, ppReverse;
        boolean bOnlyEndProperty;
        
        
        public UserAccess(OAObject obj, String pp) {
            this.obj = obj;
            this.pp = new OAPropertyPath(obj.getClass(), pp);
            this.ppReverse = this.pp.getReversePropertyPath();
        }
        public UserAccess(Hub hub, String pp) {
            this.obj = obj;
            this.pp = new OAPropertyPath(hub.getObjectClass(), pp);
            this.ppReverse = this.pp.getReversePropertyPath();
        }
    }

/*

 buyer
     location.company.clients.products.campaigns 
     
need to know if buyer has access to all objs in pp or just campaigns

 */
    
    public void add(OAObject obj, String pp) {
        if (obj == null) return;
        if (alUserAccess == null) alUserAccess = new ArrayList<>();
        alUserAccess.add(new UserAccess(obj, pp));
    }
    public void add(Hub hub, String pp) {
        if (hub == null) return;
        if (hub.getObjectClass() == null) throw new RuntimeException("hub getObjectClass can not be null");
        if (alUserAccess == null) alUserAccess = new ArrayList<>();
        alUserAccess.add(new UserAccess(hub, pp));
    }

    
    /**
     * See if an Object is included in any of the Root obj/hub + propertyPaths.
     * 
     * This is done by using the property path to search from the root obj/hub.at 
     * and then reversing the pp from the search object to find a common root.    
     * 
     * @param objSearch
     * @return
     */
    public boolean getIsInSamePropertyPath(final OAObject objSearch) {
        if (objSearch == null) return false;
        final Class cz = objSearch.getClass();

        for (UserAccess ua : alUserAccess) {
            if (ua.obj == objSearch) return true;
            if (ua.hub != null && ua.hub.getAO() == objSearch) return true;
            
            // see if obj type is in ua propertyPath type of objects
            OALinkInfo[] lis = ua.pp.getLinkInfos();
            
            boolean b = ua.pp.getFromClass().equals(cz) && (lis.length == 0 || !ua.bOnlyEndProperty);
            
            for (int i=0; i<lis.length; i++) {
                if (b) break;
                OALinkInfo li = lis[i];

                if (!li.getToClass().equals(cz)) continue;

                Object objx = ua.obj;
                if (objx == null) {
                    if (ua.hub == null) break;
                    objx = ua.hub.getAO();
                    if (objx == null) break;
                }
                if (objx == objSearch) return true;
                
                int j = 0;
                for ( ; j<i; j++) {
                    if (lis[j].getType() != OALinkInfo.TYPE_ONE) break;
                    objx = lis[j].getValue(objx);
                    if (objx == null) break;
                    if (objx == objSearch) return true;
                }
                if (objx == null) continue;

                OALinkInfo[] liz = ua.ppReverse.getLinkInfos();
                int k = (liz.length - i) - 1;
                Object objz = objSearch;
                for ( ; k < lis.length; k++) {
                    if (liz[k].getType() != OALinkInfo.TYPE_ONE) break;
                    objz = liz[k].getValue(objz);
                    if (objz == null) break;
                    if (objz == objx) return true; // common master
                }
            }
        }
        return false;
    }
    
    
    
    public boolean getHasAccess(Hub hub) {
        //qqqqqqqqq
        return true;
    }

    public boolean getHasVisibleAccess(OAObject obj) {
        return true;
    }
    public boolean getHasVisibleAccess(Hub hub) {
        //qqqqqqqqq
        return true;
    }
    public boolean getHasEnableAccess(OAObject obj) {
        //qqqqqq
        return true;
    }
    public boolean getHasEnableAccess(Hub hub) {
        //qqqqqq
        return true;
    }


}




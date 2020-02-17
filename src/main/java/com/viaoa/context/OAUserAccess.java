package com.viaoa.context;

import java.util.ArrayList;
import java.util.HashSet;

import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAPropertyPath;

/**
 * Used to determine if an object is included in a propertyPath from an OAObject/Hub.AO
 * 
 * Separate methods for Visible and Enabled property paths, both On and Off (Not).
 *
 * Has methods to add multiple obj/hub and propertyPaths, so that all are searched to
 * see if an Object is included in any of the root + paths.
 *   
 * @author vvia
 *
 */
public class OAUserAccess {
    
    private final ArrayList<UserAccess> alEnabledUserAccess = new ArrayList<>();
    private final ArrayList<UserAccess> alVisibleUserAccess = new ArrayList<>();

    private final ArrayList<UserAccess> alNotEnabledUserAccess = new ArrayList<>();
    private final ArrayList<UserAccess> alNotVisibleUserAccess = new ArrayList<>();

    private final HashSet<Class<? extends OAObject>> hsEnabledClass = new HashSet<>();
    private final HashSet<Class<? extends OAObject>> hsNotEnabledClass = new HashSet<>();

    private final HashSet<Class<? extends OAObject>> hsVisibleClass = new HashSet<>();
    private final HashSet<Class<? extends OAObject>> hsNotVisibleClass = new HashSet<>();

    /**
     * Keeps track of all defined propertyPaths, with root obj/hub.ao
     */
    protected static class UserAccess {
        Hub hub;
        OAObject obj;
        OAPropertyPath pp, ppReverse;
        boolean bOnlyEndProperty;
        
        public UserAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
            this.obj = obj;
            this.pp = new OAPropertyPath(obj.getClass(), pp);
            this.ppReverse = this.pp.getReversePropertyPath();
            this.bOnlyEndProperty = bOnlyEndProperty;
        }
        public UserAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
            this.obj = obj;
            this.pp = new OAPropertyPath(hub.getObjectClass(), pp);
            this.ppReverse = this.pp.getReversePropertyPath();
            this.bOnlyEndProperty = bOnlyEndProperty;
        }
    }

    
    
    public void addDefaultEnabled(Class<? extends OAObject> c) {
        hsEnabledClass.add(c);
    }
    public void addDefaultNotEnabled(Class<? extends OAObject> c) {
        hsNotEnabledClass.add(c);
    }
    public void addDefaultVisible(Class<? extends OAObject> c) {
        hsVisibleClass.add(c);
    }
    public void addDefaultNotVisible(Class<? extends OAObject> c) {
        hsNotVisibleClass.add(c);
    }

    
    public void addEnabledAccess(OAObject obj, String pp) {
        addEnabledAccess(obj, pp, false);
    }
    public void addEnabledAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
        if (obj == null) return;
        UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
        alEnabledUserAccess.add(ua);
    }
    public void addEnabledAccess(Hub hub, String pp) {
        addEnabledAccess(hub, pp, false);
    }
    public void addEnabledAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
        if (hub == null) return;
        if (hub.getObjectClass() == null) throw new RuntimeException("hub getObjectClass can not be null");
        alEnabledUserAccess.add(new UserAccess(hub, pp, bOnlyEndProperty));
    }
    public void addNotEnabledAccess(OAObject obj, String pp) {
        addNotEnabledAccess(obj, pp, false);
    }
    public void addNotEnabledAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
        if (obj == null) return;
        UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
        alNotEnabledUserAccess.add(ua);
    }
    public void addNotEnabledAccess(Hub hub, String pp) {
        addNotEnabledAccess(hub, pp, false);
    }
    public void addNotEnabledAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
        if (hub == null) return;
        if (hub.getObjectClass() == null) throw new RuntimeException("hub getObjectClass can not be null");
        alNotEnabledUserAccess.add(new UserAccess(hub, pp, bOnlyEndProperty));
    }

    
    public void addVisibleAccess(OAObject obj, String pp) {
        addVisibleAccess(obj, pp, false);
    }
    public void addVisibleAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
        if (obj == null) return;
        UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
        alVisibleUserAccess.add(ua);
    }
    public void addVisibleAccess(Hub hub, String pp) {
        addVisibleAccess(hub, pp, false);
    }
    public void addVisibleAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
        if (hub == null) return;
        if (hub.getObjectClass() == null) throw new RuntimeException("hub getObjectClass can not be null");
        alVisibleUserAccess.add(new UserAccess(hub, pp, bOnlyEndProperty));
    }
    public void addNotVisibleAccess(OAObject obj, String pp) {
        addNotVisibleAccess(obj, pp, false);
    }
    public void addNotVisibleAccess(OAObject obj, String pp, boolean bOnlyEndProperty) {
        if (obj == null) return;
        UserAccess ua = new UserAccess(obj, pp, bOnlyEndProperty);
        alNotVisibleUserAccess.add(ua);
    }
    public void addNotVisibleAccess(Hub hub, String pp) {
        addNotVisibleAccess(hub, pp, false);
    }
    public void addNotVisibleAccess(Hub hub, String pp, boolean bOnlyEndProperty) {
        if (hub == null) return;
        if (hub.getObjectClass() == null) throw new RuntimeException("hub getObjectClass can not be null");
        alNotVisibleUserAccess.add(new UserAccess(hub, pp, bOnlyEndProperty));
    }

    

    
    /**
     * Checks all of the enabled propertyPaths to find a match.
     * If true, will then check Not enabled property paths.
     */
    public boolean getHasEnabledAccess(OAObject obj) {
        if (obj == null) return false;
        boolean b = getIsInSamePropertyPath(obj, alEnabledUserAccess);
        
        if (!b) {
            b = getIsInSamePropertyPath(obj, alNotEnabledUserAccess);
            if (b) b = false;
            else {
                Class cz = obj.getClass();
                if (hsEnabledClass.contains(cz)) b = true;
                else if (hsNotEnabledClass.contains(cz)) b = false;
                else {
                    b = alEnabledUserAccess.size() == 0 && hsEnabledClass.size() == 0;
                }
            }
        }
        return b;
    }
    
    
    /**
     * Checks all of the visible propertyPaths to find a match.
     * If true, will then check Not visible property paths.
     */
    public boolean getHasVisibleAccess(OAObject obj) {
        if (obj == null) return false;
        boolean b = getIsInSamePropertyPath(obj, alVisibleUserAccess);
        
        if (!b) {
            b = getIsInSamePropertyPath(obj, alNotVisibleUserAccess);
            if (b) b = false;
            else {
                Class cz = obj.getClass();
                if (hsVisibleClass.contains(cz)) b = true;
                else if (hsNotVisibleClass.contains(cz)) b = false;
                else {
                    b = alVisibleUserAccess.size() == 0 && hsVisibleClass.size() == 0;
                }
            }
        }
        return b;
    }


    /**
     * See if an Object is included in any of the Root obj/hub + propertyPaths.
     * 
     * This is done by using the property path to search from the root obj/hub.at 
     * and then reversing the pp from the search object to find a common root.    
     */
    protected boolean getIsInSamePropertyPath(final OAObject objSearch, final ArrayList<UserAccess> alUserAccess) {
        if (objSearch == null || alUserAccess == null) return false;
        final Class cz = objSearch.getClass();

        for (final UserAccess ua : alUserAccess) {
            if (ua.obj == objSearch) return true;
            if (ua.hub != null && ua.hub.getAO() == objSearch) return true;
            
            // see if obj type is in ua propertyPath type of objects
            OALinkInfo[] lis = ua.pp.getLinkInfos();
            
            int i = 0;
            if (ua.bOnlyEndProperty) i = Math.max(0, lis.length-1);
            for ( ; i<lis.length; i++) {
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
                for ( ; j<=i; j++) {
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
}


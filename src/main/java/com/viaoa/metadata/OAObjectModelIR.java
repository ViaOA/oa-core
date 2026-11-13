package com.viaoa.metadata;

import com.viaoa.annotation.OAClass;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubCombined;
import com.viaoa.hub.view.HubGroupBy;
import com.viaoa.lang.OAStr;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class OAObjectModelIR<T extends OAObject> {

    protected Class<T> ctype;
    protected Hub<T> hubContext;
    protected Hub<T> hubChoices;  

    protected Hub<?> hubFrom;
    protected String fromLinkProperty;
    
    protected OA oaFrom;
    protected OAObjectInfo oiFrom;
    protected OALinkInfo liFrom;
//qqqq    protected Hub<T> hubFromDetail;

    protected OA oa;
    protected OAObjectInfo oi;
    
    
    
    protected Hub<T> hubCombinedChoices;    
    
    protected Hub<T> hubSelectFrom;
    
    
    protected boolean bPrivateMany;

    protected boolean bAutoCreated;
    private String extraWhere;
    private OAFilter<T> extraFilter;  //qqq was extraWhereFilter
    private Hub<?> hubSelectWhere;  // for type=ManyPrivate, used by OASelect
    private String selectWhereHubPath;  // for type=ManyPrivate, used by OASelect
    
    
    protected boolean bFiltered;
    protected boolean bGroupBy;
    protected boolean bRecursive;

    protected Hub<T> hubGroupByShare;
    protected HubGroupBy<T, ?>[] hubGroupBys;
    protected Hub<T> hubRecursiveShare;
    protected Hub<T> hubRecursiveRoot;
    protected Hub<T> hubRecursiveFlat;
    protected Hub<T> hubRecursive;

    protected Hub<T> hubFilterShare;
    protected Hub<T>[] hubFilters;
    
    
    
    
    protected static enum Type {
        /*
        no hubContext, or hubChoices - creates a new hubContext, free to add/remove/search
        only hubContext - free to add/remove/search
        only hubChoices -  create a new hubContext, hubContext picks (multiselect) from hubChoices
        both - hubContext picks (multiselect) from hubChoices
        */
        None,
        
        /*
        hubContext is detail hub, hubChoices is for selecting and linked to hubFrom.linkProp
         */
        One,    
        
        /*
        based on hubFrom.hubMaster and hubFrom isLinkObjectFor
        HubContext will be many Hub, hubFrom will automatch (OMO linkProperty)
        HubChoices 
         */
        OMO, 
        
        /*
        HubContext is detail hub (multiselect), HubChoices is to select from.
         */
        Many,
        
        /*
        no detail hub, uses hubFrom.masterHub+path to contain search (ex: Customer.Orders search will only select Order with customer=customer)
         */
        ManyPrivate  
    }
    protected Type type;
    
    
    public OAObjectModelIR(Class<T> ctype, Hub<T> hubContext, Hub<T> hubChoices, final Hub<?> hubFrom, final String linkPropertyName) {
        this.ctype = ctype;
        this.hubContext = hubContext;
        this.hubChoices = hubChoices;
        this.hubFrom = hubFrom;
        this.fromLinkProperty = linkPropertyName;
        config();
    }


    protected void config() {
        configType();
        
        switch (type) {
        case None:
            configTypeNone(); 
            break;
        case One:
            configTypeOne();
            break;
        case OMO:
            configTypeOMO();
            break;
        case Many:
            configTypeMany();
            break;
        case ManyPrivate:
            configTypeManyPrivate();
            break;
        }
        
        configRecursiveAndFilters();
    }


    protected void configType() {
        if (ctype == null) {
            if (hubContext != null) ctype = hubContext.getObjectClass();
            if (ctype == null) {
                if (hubChoices != null) ctype = hubChoices.getObjectClass();
                if (ctype == null && hubFrom != null && OAStr.isNotEmpty(fromLinkProperty)) {
                    Class<? extends OAObject> ct = hubFrom.getObjectClass();
                    if (ct != null) {
                        OA oa = OARuntime.oa(ct);
                        OALinkInfo li = oa.info(ct).getLinkInfo(fromLinkProperty);
                        if (li != null) ctype = li.getToClass();
                    }
                }
            }
        }
        if (ctype == null) {
            throw new IllegalArgumentException("ctype cant be determined");
        }
        
        if (hubFrom != null && OAStr.isNotEmpty(fromLinkProperty)) {
            oaFrom = OARuntime.oa(hubFrom);
            oiFrom = oaFrom.info(hubFrom);
            liFrom = oiFrom.getLinkInfo(fromLinkProperty);
            
            if (liFrom == null) {
                throw new IllegalArgumentException("Hub fromLinkProperty is invalid, hubFrom="+hubFrom+", fromLinkProperty="+fromLinkProperty);
            }
            if (!liFrom.getToClass().equals(ctype)) {
                throw new IllegalArgumentException("Hub fromLinkProperty.class does not match ctype");
            }
            
            Hub<T> hubx = hubFrom.getDetailHub(fromLinkProperty);
            if (hubContext != null) {
                if (!oaFrom.internal().hubs().share().isUsingSameSharedHub(hubx, hubContext)) {
                    throw new IllegalArgumentException("hubFrom.detail and hubContext are not the same");
                }
            }
            else {
                hubContext = hubx;
            }
        }
        else if (hubContext != null) {
            Class<?> cx = hubContext.getObjectClass();
            if (cx != null && !ctype.equals(cx)) {
                throw new IllegalArgumentException("HubContext.objectClass does not match ctype");
            }
            Hub<?> hubMaster = hubContext.getMasterHub();
            if (hubMaster != null) {
                hubFrom = hubMaster;
                oaFrom = OARuntime.oa(hubMaster);
                oiFrom = oaFrom.info(hubMaster);
                liFrom = oaFrom.internal().hubs().detail().getLinkInfoFromMasterToDetail(hubContext);
                fromLinkProperty = liFrom.getName();
                if (!liFrom.getToClass().equals(ctype)) {
                    throw new IllegalArgumentException("HubContext.objectClass does not match ctype");
                }
            }
        }

        this.oa = OARuntime.oa(ctype);
        this.oi = oa.info(ctype);
        
        if (hubContext != null && hubContext.getObjectClass() == null) {
            oa.internal().hubs().data().setObjectClass(hubContext, ctype);
        }
        
//qqqqqqqqqqqqqqqqqqqq COntext ONE or MANY decides Select,Add qqqqqqqqqqq
        if (liFrom == null) {
            type = Type.None;
        }
        else if (liFrom.getType() == OALinkInfo.TYPE_ONE) {
            // check OMO                
            Hub<?> hubMaster = hubFrom.getMasterHub();
            if (hubMaster != null && hubFrom.isLinkObjectFor(liFrom.getName())) {
                type = Type.OMO;
            }
            else {
                type = Type.One;
            }
        }
        else { // Many
            if (liFrom.getPrivateMethod()) {
                type = Type.ManyPrivate;
            }
            else {
                type = Type.Many;
            }
        }
    }


    
    protected void configTypeNone() {
        if (hubContext == null) hubContext = new Hub<>(ctype);
    }
    
    protected void configTypeOne() {
        bAutoCreated = liFrom.getAutoCreateNew();
        configSelectFromHub();
        if (hubSelectFrom != null) {
            hubChoices = hubSelectFrom;
        }
        if (hubChoices != null) {
            Hub<T> hubCombined = new Hub<>(ctype);
            new HubCombined(hubCombined, hubChoices, hubContext);
            hubChoices = hubCombined;
            hubChoices.setLinkHub(hubFrom, fromLinkProperty);
        }
        configExtraFilterAndWhereQuery();
    }

    protected void configTypeOMO() {
        OALinkInfo lix = oaFrom.internal().hubs().detail().getLinkInfoFromMasterObjectToDetail(hubFrom);
        String pp = lix.getName() + "." + liFrom.getName();
        hubContext = new Hub<T>(ctype);
        new HubMerger(hubFrom.getMasterHub(), hubContext, pp);
        
        configSelectFromHub();
        if (hubSelectFrom != null) {
            hubChoices = hubSelectFrom;
        }
     
        if (hubChoices != null) {
            Hub<T> hubCombined = new Hub<>(ctype);
            new HubCombined(hubCombined, hubChoices, hubContext);
            hubChoices = hubCombined;
        }
        configExtraFilterAndWhereQuery();
    }
    

    protected void configTypeMany() {
        this.bAutoCreated = liFrom.getMatchHub() != null;
        this.hubContext = hubFrom.getDetailHub(fromLinkProperty);

        configSelectFromHub();
        if (hubSelectFrom != null) {
            hubChoices = hubSelectFrom;
        }

        if (hubChoices != null) {
            Hub<T> hubCombined = new Hub<>(ctype);
            new HubCombined(hubCombined, hubChoices, hubContext);
            hubChoices = hubCombined;
        }
        configExtraFilterAndWhereQuery();
    }

    protected void configTypeManyPrivate() {
        if (hubContext == null) hubContext = new Hub<T>(ctype);
        this.hubSelectWhere = hubFrom;
        this.selectWhereHubPath = fromLinkProperty;
    }
    
    
    
    
    protected void configSelectFromHub() {
        if (liFrom != null) {
            String s = liFrom.getSelectFromPath();
            if (OAStr.isEmpty(s)) s = liFrom.getMergerPath();
            if (OAStr.isNotEmpty(s)) {
                hubSelectFrom = new Hub<>(ctype);
                new HubMerger(hubFrom, hubSelectFrom, s, false);
            }
        }        
    }
    
    protected void configExtraFilterAndWhereQuery() {
        if (liFrom == null) return;
        OAFilter<T> filter = null;
        
        String path = liFrom.getEqualPath();
        OALinkInfo liRev = liFrom.getReverseLinkInfo();
        if (liRev != null) {
            String path2 = liRev.getEqualPath();
            if (OAStr.isNotEmpty(path) || OAStr.isNotEmpty(path2)) {
                OAEqualPathFilter filterEqualPath = new OAEqualPathFilter(hubFrom, path, path2);
                if (filter == null) filter = filterEqualPath;
                else filter = new OAAndFilter(filter, filterEqualPath);
                extraFilter = filter;
            }
        }
        String filterName = liFrom.getFilterName();

        String s = liFrom.getSelectFromPath();
        if (OAStr.isNotEmpty(s)) {
            if (OAStr.isNotEmpty(filterName)) {
                if (OAStr.indexOf(s.toUpperCase(), ":" + filterName.toUpperCase(), true) >= 0) {
                    filterName = null;
                }
            }
        }

        if (OAStr.isNotEmpty(filterName)) {
            extraWhere = OAStr.append(extraWhere, ":" + filterName, " AND ");
        }
    }


    protected void configRecursiveAndFilters() {
        OALinkInfo liRecursive = oi.getRecursiveLinkInfo(OALinkInfo.MANY);
        bRecursive = (liRecursive != null);
        
        String[] ss = oi.getGroupByPaths();
        bGroupBy = ss != null && ss.length > 0;
        if (bGroupBy) {
            hubGroupBys = new HubGroupBy[ss.length];
        }

        Class[] cs = null;
        OAClass oac = (OAClass) ctype.getAnnotation(OAClass.class);
        if (oac != null) {
            cs = oac.filterClasses();
        }
        bFiltered = cs != null && cs.length > 0;
        if (bFiltered) {
            hubFilters = new Hub[cs.length];
            for (int i = 0; i < cs.length; i++) {
                hubFilters[i] = new Hub<T>(ctype);
            }
        }
        
    }
   
}

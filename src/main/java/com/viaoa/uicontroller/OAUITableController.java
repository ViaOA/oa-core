package com.viaoa.uicontroller;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.*;

/*qqqqqqqqqqq

todo: if table is not enabled, then dont allow changing active row

*/

/**
 * Used by UI Components that use a Hub to present a listing to populate an HTML Table.
 * 
 * For multi-select, it uses a hubSelect.
 * 
 * @author vince
 */
public abstract class OAUITableController  {

    private OAUIController controlHub;
    private OAUIController controlLinkHub;
    
    private Hub hub;
    private Hub hubLink;
    private String linkPropertyName; 
    private Hub hubSelect;

    private HubListenerAdapter hlSelect;

    private HubListenerAdapter hlTableColumns;
    private String gridListenerName;
    private static final AtomicInteger aiNameCounter = new AtomicInteger();
    
    private final boolean bInitialized;

    
    public OAUITableController(Hub hub, Hub hubSelect, String[] columnPropertyPaths) {
        this.hub = hub;
        this.hubSelect = hubSelect;
        
        getUIController();
        getLinkUIController();
        
        if (hubSelect != null) {
            hlSelect = new HubListenerAdapter() {
                @Override
                public void afterAdd(HubEvent e) {
                    updateSelected();
                }
                @Override
                public void afterInsert(HubEvent e) {
                    updateSelected();
                }
                @Override
                public void afterNewList(HubEvent e) {
                    updateSelected();
                }
                @Override
                public void afterRemove(HubEvent e) {
                    updateSelected();
                }
                @Override
                public void afterRemoveAll(HubEvent e) {
                    updateSelected();
                }
            };
            hubSelect.addHubListener(hlSelect);
        }
        
        if (columnPropertyPaths != null && columnPropertyPaths.length > 0) {
            this.gridListenerName = "table_"+aiNameCounter.incrementAndGet();
            
            hlTableColumns = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(final HubEvent e) {
                    if (!gridListenerName.equalsIgnoreCase(e.getPropertyName())) return;
                    int row = getHub().getPos(e.getObject());
                    changed(row);
                }
            };
        
            hub.addHubListener(hlTableColumns, gridListenerName, columnPropertyPaths);
        }
        
        bInitialized = true;
        if (hubSelect != null) updateSelected();
    }
    

    public Hub getHub() {
        return getUIController().getHub();
    }
    
    public Hub getSelectHub() {
        return hubSelect;
    }
    
    
    public Hub getLinkHub() {
        if (getLinkUIController() == null) return null;
        return getLinkUIController().getHub();
    }
    public String getLinkPropertyName() {
        if (getLinkUIController() == null) return null;
        return getLinkUIController().getPropertyPath();
    }
    
    public void reset() {
        getUIController().reset();
        OAUIController c = getLinkUIController();
        if (c != null) c.reset();
    }

    
    public void close() {
        getUIController().close();
        if (controlLinkHub != null) controlLinkHub.close();
        if (hlSelect != null) hubSelect.removeHubListener(hlSelect);
        if (hlTableColumns != null) hub.removeListener(hlTableColumns);
    }
    
    protected OAUIController getUIController() {
        if (controlHub != null) return controlHub ;
        
        controlHub = new OAUIController(hub, null, null, false, HubChangeListener.Type.HubValid) {
            @Override
            protected void reset() {
                if (bInitialized) {
                    super.reset();
                }
            }
            @Override
            public void updateComponent(Object object) {
                // OAUITableController.this.updateComponent(object);
            }
            @Override
            public void updateLabel(Object object) {
                // OAUITableController.this.updateLabel(object);
            }

            @Override
            public void afterAdd(HubEvent e) {
                OAUITableController.this.add(e.getObject());
            }
            @Override
            public void afterChangeActiveObject(HubEvent e) {
                OAUITableController.this.setChangeAO(hub.getPos(e.getObject()));
            }
            @Override
            public void afterInsert(HubEvent e) {
                OAUITableController.this.insert(e.getObject(), e.getPos());
            }
            @Override
            public void afterMove(HubEvent e) {
                OAUITableController.this.remove(e.getFromPos());
                OAUITableController.this.insert(e.getObject(), e.getToPos());
            }
            @Override
            public void afterNewList(HubEvent e) {
                OAUITableController.this.newList();
            }
            @Override
            public void afterRemove(HubEvent e) {
                OAUITableController.this.remove(e.getPos());
            }
            @Override
            public void afterRemoveAll(HubEvent e) {
                OAUITableController.this.clear();
            }
            @Override
            public void afterSort(HubEvent e) {
                OAUITableController.this.newList();
            }
        };
        return controlHub;
    }
        
    protected OAUIController getLinkUIController() {
        if (controlLinkHub != null || hubSelect != null) return controlLinkHub;
    
        hubLink = hub.getLinkHub(true);
        
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
            protected void reset() {
                if (bInitialized) {
                    super.reset();
                }
            }
            @Override
            public void updateComponent(Object object) {
                // OAUITableController.this.updateComponent(object);
            }
            @Override
            public void updateLabel(Object object) {
                // OAUITableController.this.updateLabel(object);
            }
        };
        
        return controlLinkHub;
    }

    
    public boolean isEnabled() {
        boolean b = getUIController().isEnabled();
        if (b) {
            b = getLinkUIController() == null || getLinkUIController().isEnabled();
        }
        return b;
    }

    public boolean isVisible() {
        boolean b = getUIController().isVisible();
        if (b) {
            b = (getLinkUIController() == null || getLinkUIController().isVisible());
        }
        return b;
    }
    
    protected void updateSelected() {
        int[] poss = new int[0];
        int pos = 0;
        for (Object obj : OAUITableController.this.getHub()) {
            boolean b  = OAUITableController.this.getSelectHub().contains(obj);
            if (b) poss = OAArray.add(poss,  pos);
            pos++;
        }
        setMultiSelect(poss);
    }
    
    
    public void setChangeAO(int pos) {
        setMultiSelect(new int[] {pos});
    }
    public void setMultiSelect(int[] poss) {
    }
    public void add(Object obj) {
    }
    public void insert(Object obj, int pos) {
    }
    public void remove(int pos) {
    }
    public void clear() {
    }
    public void newList() {
    }
    public void changed(int row) {
    }
    
}

/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * Abstract controller for grid or table UI components that display the
 * contents of a {@link Hub}. OAUITableController keeps the table rows,
 * active row selection, and optional link/select Hubs synchronized with
 * the underlying object graph.
 *
 * <p>
 * Responsibilities include:
 * </p>
 *
 * <ul>
 *   <li>Mapping table rows to objects in the main Hub.</li>
 *   <li>Updating the Hub's active object when the user changes the
 *       selected row.</li>
 *   <li>Coordinating with an optional link Hub and select Hub.</li>
 *   <li>Listening for changes to table column definitions through a
 *       dedicated Hub.</li>
 * </ul>
 *
 * <p>
 * Concrete subclasses integrate the controller logic with a specific table
 * widget (Swing JTable, web grid, etc.). Some behaviors, such as disallowing
 * row selection when the table is disabled, are noted as TODOs in the source
 * and can be refined in later phases.
 * </p>
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

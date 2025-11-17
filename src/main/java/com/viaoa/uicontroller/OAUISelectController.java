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

import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.*;

/**
 * Abstract controller used by UI components that present a selectable list
 * of OAObjects from a {@link Hub}. Once a value is chosen, the controller
 * updates either the active object in the main Hub or a linked reference,
 * depending on configuration.
 *
 * <p>
 * OAUISelectController coordinates several Hubs:
 * </p>
 *
 * <ul>
 *   <li>The main Hub whose active object will be affected by the selection.</li>
 *   <li>An optional link Hub representing a reference property.</li>
 *   <li>An optional select Hub that provides the list of choices.</li>
 * </ul>
 *
 * <p>
 * It supports both single-select and multi-select scenarios and uses a
 * Hub listener to keep the UI and the underlying Hubs synchronized. Concrete
 * subclasses integrate with specific UI toolkits (Swing, web controls, etc.).
 * </p>
 */
public abstract class OAUISelectController  {

    private OAUIController controlHub;
    private OAUIController controlLinkHub;
    private Hub hub;
    private String propertyPath;
    private Hub hubLink;
    private boolean linkOnPos;
    private String linkPropertyName; 
    private final boolean bInitialized;
    private Hub hubSelect;
    private HubListenerAdapter hlSelect;
    
    
    public OAUISelectController(Hub hub, String propertyPath, Hub hubSelect, boolean bCallReset) {
        this.hub = hub;
        this.propertyPath = propertyPath;
        this.hubSelect = hubSelect;
        
        getUIController();
        getLinkUIController();
        
        if (hubSelect != null) {
            hubSelect.addHubListener(new HubListenerAdapter() {
                void updateSelected() {
                    int[] poss = new int[0];
                    int pos = 0;
                    for (Object obj : OAUISelectController.this.getHub()) {
                        boolean b  = OAUISelectController.this.getSelectHub().contains(obj);
                        if (b) poss = OAArray.add(poss,  pos);
                        pos++;
                    }
                    OAUISelectController.this.setSelected(poss);
                }
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
            });
        }
        
        bInitialized = true;
        if (bCallReset) reset();
    }
    
    public Hub getHub() {
        return getUIController().getHub();
    }
    public String getPropertyName() {
        return getUIController().getPropertyPath();
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
        OAUIController c = getLinkUIController();
        if (c != null) c.close();
        if (hubSelect != null) {
            hubSelect.removeHubListener(hlSelect);
        }
    }
    
    protected OAUIController getUIController() {
        if (controlHub != null) return controlHub ;
        
        controlHub = new OAUIController(hub, null, propertyPath, false, HubChangeListener.Type.HubValid) {
            @Override
            protected void reset() {
                if (bInitialized) {
                    super.reset();
                }
            }
            @Override
            public void updateComponent(Object object) {
                OAUISelectController.this.updateComponent(object);
            }
            @Override
            public void updateLabel(Object object) {
                OAUISelectController.this.updateLabel(object);
            }

            @Override
            public void afterAdd(HubEvent e) {
                OAUISelectController.this.add(e.getObject());
            }
            @Override
            public void afterChangeActiveObject(HubEvent e) {
                if (OAUISelectController.this.getSelectHub() == null) {
                    OAUISelectController.this.setSelected(hub.getPos(e.getObject()));
                }
            }
            @Override
            public void afterInsert(HubEvent e) {
                OAUISelectController.this.insert(e.getObject(), e.getPos());
            }
            @Override
            public void afterMove(HubEvent e) {
                OAUISelectController.this.remove(e.getFromPos());
                OAUISelectController.this.insert(e.getObject(), e.getToPos());
            }
            @Override
            public void afterNewList(HubEvent e) {
                OAUISelectController.this.newList();
            }
            @Override
            public void afterPropertyChange(HubEvent e) {
                if (OAStr.isEqualIgnoreCase(e.getPropertyName(), OAUISelectController.this.propertyPath)) {
                    OAUISelectController.this.changed(e.getObject());
                }
            }
            @Override
            public void afterRemove(HubEvent e) {
                OAUISelectController.this.remove(e.getPos());
            }
            @Override
            public void afterRemoveAll(HubEvent e) {
                OAUISelectController.this.clear();
            }
            @Override
            public void afterSort(HubEvent e) {
                OAUISelectController.this.newList();
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
        linkOnPos = HubLinkDelegate.getLinkedOnPos(hub);
        
        controlLinkHub = new OAUIController(hubLink, null, linkPropertyName, true, HubChangeListener.Type.AoNotNull) {
            @Override
            protected void reset() {
                if (bInitialized) {
                    super.reset();
                }
            }
            @Override
            public void updateComponent(Object object) {
                OAUISelectController.this.updateComponent(object);
            }
            @Override
            public void updateLabel(Object object) {
                OAUISelectController.this.updateLabel(object);
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
    

    public String getValueAsString(Hub hubFrom, Object obj) {
        Object objx = getLinkUIController().getValue(obj);
        if (linkOnPos && objx instanceof Number) objx = getHub().getAt(OAConv.toInt(objx));  
        String s = getUIController().getValueAsString(objx);
        return s;
    }
    
    public String getValueAsString(Object obj) {
        String s = getUIController().getValueAsString(obj);
        return s;
    }
    
    public void setSelected(int pos) {
        setSelected(new int[] {pos});
    }
    public void setSelected(int[] poss) {
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
    public void changed(Object object) {
    }

    
    
    /** 
     * Called when a change is necessary for UI component. 
     * */
    public abstract void updateComponent(Object object);
    

    public abstract void updateLabel(Object object);
    
    
    
    //qqqqqqqq events from client JS to change hub.AO ... need to validate ...
    /*
    call this to check hubLink.AO change is valid ...
    
    1: isEnabled
    2: public String isValid(final Object obj, Object newValue)
    3: [confirm]
    4: hubLink.setAO(newValue) 
    */
    
}

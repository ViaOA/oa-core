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
package com.viaoa.hub;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.object.*;

/**
 * Encapsulates state and metadata that are unique to a specific Hub instance,
 * even when its data list is shared with others.
 *
 * <p>Manages:</p>
 * <ul>
 *   <li>Active-object update flags</li>
 *   <li>Listener trees and detail-hub collections</li>
 *   <li>Link-to and link-from Hub relationships</li>
 *   <li>Shared-hub registry and weak references</li>
 *   <li>Auto-create behavior for link targets</li>
 * </ul>
 *
 * <p>Each Hub has one {@link HubDataUnique}; shared Hubs maintain independent
 * instances for AO, listener, and linkage tracking.</p>
 */
class HubDataUnique implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization
	private static Logger LOG = Logger.getLogger(HubDataUnique.class.getName());
	private transient volatile HubDataUniquex hubDataUniquex;  // extended settings

    private HubDataUniquex getHubDataUniquex() {
        if (hubDataUniquex == null) {
            synchronized (this) {
                if (hubDataUniquex == null) {
                    this.hubDataUniquex = new HubDataUniquex();
                }
            }
        }
        return hubDataUniquex;
    }
    

    public int getDefaultPos() {
        if (hubDataUniquex == null) return -1;
        return hubDataUniquex.defaultPos;
    }
    public void setDefaultPos(int defaultPos) {
        if (hubDataUniquex != null || defaultPos != -1) {
            getHubDataUniquex().defaultPos = defaultPos;
        }
    }

    public boolean isNullOnRemove() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.bNullOnRemove;
    }
    public void setNullOnRemove(boolean bNullOnRemove) {
        if (hubDataUniquex != null || bNullOnRemove) {
            getHubDataUniquex().bNullOnRemove = bNullOnRemove;
        }
    }

    public HubListenerTree getListenerTree() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.listenerTree;
    }
    public void setListenerTree(HubListenerTree listenerTree) {
        if (hubDataUniquex != null || listenerTree != null) {
            getHubDataUniquex().listenerTree = listenerTree;
        }
    }

    public Vector<HubDetail> getVecHubDetail() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.vecHubDetail;
    }
    public void setVecHubDetail(Vector<HubDetail> vecHubDetail) {
        if (hubDataUniquex != null || vecHubDetail != null) {
            getHubDataUniquex().vecHubDetail = vecHubDetail;
        }
    }

    private static ConcurrentHashMap<HubDataUnique, HubDataUnique> hmUpdatingActiveObject = new ConcurrentHashMap<HubDataUnique, HubDataUnique>(11, .85f);
    public boolean isUpdatingActiveObject() {
        return hmUpdatingActiveObject.containsKey(this);
    }
    public void setUpdatingActiveObject(boolean bUpdatingActiveObject) {
        if (bUpdatingActiveObject) {
            Object objx = hmUpdatingActiveObject.put(this, this);
        }
        else {
            Object objx = hmUpdatingActiveObject.remove(this);
        }
    }

    public Hub getLinkToHub() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToHub;
    }
    public void setLinkToHub(Hub linkToHub) {
        if (hubDataUniquex != null || linkToHub != null) {
            getHubDataUniquex().linkToHub = linkToHub;
        }
    }

    public boolean isLinkPos() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.linkPos;
    }
    public void setLinkPos(boolean linkPos) {
        if (hubDataUniquex != null || linkPos) {
            getHubDataUniquex().linkPos = linkPos;
        }
    }
    
    public String getLinkToPropertyName() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToPropertyName;
    }
    public void setLinkToPropertyName(String linkToPropertyName) {
        if (hubDataUniquex != null || linkToPropertyName != null) {
            getHubDataUniquex().linkToPropertyName = linkToPropertyName;
        }
    }

    public Method getLinkToGetMethod() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToGetMethod;
    }
    public void setLinkToGetMethod(Method linkToGetMethod) {
        if (hubDataUniquex != null || linkToGetMethod != null) {
            getHubDataUniquex().linkToGetMethod = linkToGetMethod;
        }
    }

    public Method getLinkToSetMethod() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToSetMethod;
    }
    public void setLinkToSetMethod(Method linkToSetMethod) {
        if (hubDataUniquex != null || linkToSetMethod != null) {
            getHubDataUniquex().linkToSetMethod = linkToSetMethod;
        }
    }

    public String getLinkFromPropertyName() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkFromPropertyName;
    }
    public void setLinkFromPropertyName(String linkFromPropertyName) {
        if (hubDataUniquex != null || linkFromPropertyName != null) {
            getHubDataUniquex().linkFromPropertyName = linkFromPropertyName;
        }
    }

    public Method getLinkFromGetMethod() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkFromGetMethod;
    }
    public void setLinkFromGetMethod(Method linkFromGetMethod) {
        if (hubDataUniquex != null || linkFromGetMethod != null) {
            getHubDataUniquex().linkFromGetMethod = linkFromGetMethod;
        }
    }


    public HubLinkEventListener getHubLinkEventListener() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.hubLinkEventListener;
    }
    public void setHubLinkEventListener(HubLinkEventListener hubLinkEventListener) {
        if (hubDataUniquex != null || hubLinkEventListener != null) {
            getHubDataUniquex().hubLinkEventListener = hubLinkEventListener;
        }
    }

    public Hub getSharedHub() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.sharedHub;
    }
    public void setSharedHub(Hub sharedHub) {
        if (hubDataUniquex != null || sharedHub != null) {
            getHubDataUniquex().sharedHub = sharedHub;
        }
    }
    public WeakReference<Hub>[] getWeakSharedHubs() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.weakSharedHubs;
    }
    public void setWeakSharedHubs(WeakReference<Hub>[] weakSharedHubs) {
        if (hubDataUniquex != null || (weakSharedHubs != null && weakSharedHubs.length > 0)) {
            getHubDataUniquex().weakSharedHubs = weakSharedHubs;
        }
    }

    public Hub getAddHub() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.addHub;
    }
    public void setAddHub(Hub addHub) {
        if (hubDataUniquex != null || addHub != null) {
            getHubDataUniquex().addHub = addHub;
        }
    }

    public boolean isAutoCreate() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.bAutoCreate;
    }
    public void setAutoCreate(boolean bAutoCreate) {
        if (hubDataUniquex != null || bAutoCreate) {
            getHubDataUniquex().bAutoCreate = bAutoCreate;
        }
    }

    public boolean isAutoCreateAllowDups() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.bAutoCreateAllowDups;
    }
    public void setAutoCreateAllowDups(boolean bAutoCreateAllowDups) {
        if (hubDataUniquex != null || bAutoCreateAllowDups) {
            getHubDataUniquex().bAutoCreateAllowDups = bAutoCreateAllowDups;
        }
    }

}

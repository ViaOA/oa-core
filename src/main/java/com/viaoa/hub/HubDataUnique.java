/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
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
public class HubDataUnique implements java.io.Serializable {
    static final long serialVersionUID = 1L;  // used for object serialization
	private static Logger LOG = Logger.getLogger(HubDataUnique.class.getName());

	/**
	 * Lazily created extended unique-data container holding optional
	 * settings and metadata for this Hub.
	 */
	private transient volatile HubDataUniquex hubDataUniquex;  // extended settings

	/**
	 * Lazily creates and returns the extended unique-data container for this Hub.
	 *
	 * @return the {@link HubDataUniquex} instance associated with this Hub
	 */
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
    
    /**
     * Returns the default position for this Hub.
     *
     * @return the default position, or {@code -1} if not set
     */
    public int getDefaultPos() {
        if (hubDataUniquex == null) return -1;
        return hubDataUniquex.defaultPos;
    }

    /**
     * Sets the default position for this Hub.
     *
     * @param defaultPos the position to use as default
     */
    public void setDefaultPos(int defaultPos) {
        if (hubDataUniquex != null || defaultPos != -1) {
            getHubDataUniquex().defaultPos = defaultPos;
        }
    }

    /**
     * Indicates whether removing the active object should set the reference to null.
     *
     * @return {@code true} if the active-object reference is nulled on removal, otherwise {@code false}
     */
    public boolean isNullOnRemove() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.bNullOnRemove;
    }
    
    /**
     * Sets whether removing the active object should null its reference.
     *
     * @param bNullOnRemove {@code true} to null the reference on removal
     */
    public void setNullOnRemove(boolean bNullOnRemove) {
        if (hubDataUniquex != null || bNullOnRemove) {
            getHubDataUniquex().bNullOnRemove = bNullOnRemove;
        }
    }

    /**
     * Returns the listener tree for this Hub.
     *
     * @return the {@link HubListenerTree}, or {@code null} if none exists
     */
    public HubListenerTree getListenerTree() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.listenerTree;
    }
    
    /**
     * Assigns the listener tree for this Hub.
     *
     * @param listenerTree the listener tree to associate with this Hub
     */
    public void setListenerTree(HubListenerTree listenerTree) {
        if (hubDataUniquex != null || listenerTree != null) {
            getHubDataUniquex().listenerTree = listenerTree;
        }
    }

    /**
     * Returns the collection of HubDetail instances associated with this Hub.
     *
     * @return a vector of {@link HubDetail} objects, or {@code null} if none exist
     */
    public Vector<HubDetail> getVecHubDetail() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.vecHubDetail;
    }
    
    /**
     * Sets the collection of HubDetail instances for this Hub.
     *
     * @param vecHubDetail the vector of HubDetail instances
     */
    public void setVecHubDetail(Vector<HubDetail> vecHubDetail) {
        if (hubDataUniquex != null || vecHubDetail != null) {
            getHubDataUniquex().vecHubDetail = vecHubDetail;
        }
    }

    private static ConcurrentHashMap<HubDataUnique, HubDataUnique> hmUpdatingActiveObject = new ConcurrentHashMap<HubDataUnique, HubDataUnique>(11, .85f);

    /**
     * Indicates whether this Hub is currently updating its active object.
     *
     * @return {@code true} if an active-object update is in progress, otherwise {@code false}
     */
    public boolean isUpdatingActiveObject() {
        return hmUpdatingActiveObject.containsKey(this);
    }

    /**
     * Sets whether this Hub is currently updating its active object.
     *
     * @param bUpdatingActiveObject {@code true} to mark as updating, {@code false} to clear
     */
    public void setUpdatingActiveObject(boolean bUpdatingActiveObject) {
        if (bUpdatingActiveObject) {
            Object objx = hmUpdatingActiveObject.put(this, this);
        }
        else {
            Object objx = hmUpdatingActiveObject.remove(this);
        }
    }

    /**
     * Returns the Hub linked to by this Hub.
     *
     * @return the linked Hub, or {@code null} if not set
     */
    public Hub getLinkToHub() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToHub;
    }
    
    /**
     * Sets the Hub linked to by this Hub.
     *
     * @param linkToHub the Hub to link to
     */
    public void setLinkToHub(Hub linkToHub) {
        if (hubDataUniquex != null || linkToHub != null) {
            getHubDataUniquex().linkToHub = linkToHub;
        }
    }

    /**
     * Indicates whether this Hub uses positional linking.
     *
     * @return {@code true} if positional linking is enabled, otherwise {@code false}
     */
    public boolean isLinkPos() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.linkPos;
    }
    
    /**
     * Sets whether this Hub uses positional linking.
     *
     * @param linkPos {@code true} to enable positional linking
     */
    public void setLinkPos(boolean linkPos) {
        if (hubDataUniquex != null || linkPos) {
            getHubDataUniquex().linkPos = linkPos;
        }
    }
    
    /**
     * Returns the property name used for the link-to relationship.
     *
     * @return the property name, or {@code null} if not set
     */
    public String getLinkToPropertyName() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToPropertyName;
    }
    
    /**
     * Sets the property name used for the link-to relationship.
     *
     * @param linkToPropertyName the name of the link-to property
     */
    public void setLinkToPropertyName(String linkToPropertyName) {
        if (hubDataUniquex != null || linkToPropertyName != null) {
            getHubDataUniquex().linkToPropertyName = linkToPropertyName;
        }
    }

    /**
     * Returns the getter method for the link-to property.
     *
     * @return the getter {@link Method}, or {@code null} if not set
     */
    public Method getLinkToGetMethod() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToGetMethod;
    }
    
    /**
     * Sets the getter method for the link-to property.
     *
     * @param linkToGetMethod the method used to retrieve the link-to value
     */
    public void setLinkToGetMethod(Method linkToGetMethod) {
        if (hubDataUniquex != null || linkToGetMethod != null) {
            getHubDataUniquex().linkToGetMethod = linkToGetMethod;
        }
    }

    /**
     * Sets the setter method for the link-to property.
     *
     * @param linkToSetMethod the method used to assign the link-to value
     */
    public Method getLinkToSetMethod() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkToSetMethod;
    }
    
    /**
     * Sets the setter method for the link-to property.
     *
     * @param linkToSetMethod the method used to assign the link-to value
     */
    public void setLinkToSetMethod(Method linkToSetMethod) {
        if (hubDataUniquex != null || linkToSetMethod != null) {
            getHubDataUniquex().linkToSetMethod = linkToSetMethod;
        }
    }

    /**
     * Returns the property name used for the link-from relationship.
     *
     * @return the link-from property name, or {@code null} if not set
     */
    public String getLinkFromPropertyName() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkFromPropertyName;
    }
    
    /**
     * Sets the property name used for the link-from relationship.
     *
     * @param linkFromPropertyName the name of the link-from property
     */
    public void setLinkFromPropertyName(String linkFromPropertyName) {
        if (hubDataUniquex != null || linkFromPropertyName != null) {
            getHubDataUniquex().linkFromPropertyName = linkFromPropertyName;
        }
    }

    /**
     * Returns the getter method used for the link-from relationship.
     *
     * @return the getter {@link Method}, or {@code null} if not set
     */
    public Method getLinkFromGetMethod() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.linkFromGetMethod;
    }

    /**
     * Sets the getter method used for the link-from relationship.
     *
     * @param linkFromGetMethod the method used to retrieve the link-from value
     */
    public void setLinkFromGetMethod(Method linkFromGetMethod) {
        if (hubDataUniquex != null || linkFromGetMethod != null) {
            getHubDataUniquex().linkFromGetMethod = linkFromGetMethod;
        }
    }

    /**
     * Returns the HubLinkEventListener assigned to this Hub.
     *
     * @return the listener, or {@code null} if not set
     */
    public HubLinkEventListener getHubLinkEventListener() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.hubLinkEventListener;
    }
    
    /**
     * Sets the HubLinkEventListener for this Hub.
     *
     * @param hubLinkEventListener the listener to assign
     */
    public void setHubLinkEventListener(HubLinkEventListener hubLinkEventListener) {
        if (hubDataUniquex != null || hubLinkEventListener != null) {
            getHubDataUniquex().hubLinkEventListener = hubLinkEventListener;
        }
    }

    /**
     * Returns the shared Hub associated with this Hub.
     *
     * @return the shared Hub, or {@code null} if none
     */
    public Hub getSharedHub() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.sharedHub;
    }

    /**
     * Sets the shared Hub associated with this Hub.
     *
     * @param sharedHub the Hub to share data with
     */
    public void setSharedHub(Hub sharedHub) {
        if (hubDataUniquex != null || sharedHub != null) {
            getHubDataUniquex().sharedHub = sharedHub;
        }
    }
    
    /**
     * Returns the array of weak references to shared Hubs.
     *
     * @return an array of {@link WeakReference} objects, or {@code null} if not set
     */
    public WeakReference<Hub>[] getWeakSharedHubs() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.weakSharedHubs;
    }
    
    /**
     * Sets the array of weak references to shared Hubs.
     *
     * @param weakSharedHubs the array of weak Hub references
     */
    public void setWeakSharedHubs(WeakReference<Hub>[] weakSharedHubs) {
        if (hubDataUniquex != null || (weakSharedHubs != null && weakSharedHubs.length > 0)) {
            getHubDataUniquex().weakSharedHubs = weakSharedHubs;
        }
    }

    /**
     * Returns the Hub used for add operations.
     *
     * @return the add Hub, or {@code null} if not defined
     */
    public Hub getAddHub() {
        if (hubDataUniquex == null) return null;
        return hubDataUniquex.addHub;
    }
    
    /**
     * Sets the Hub used for add operations.
     *
     * @param addHub the Hub to assign for add operations
     */
    public void setAddHub(Hub addHub) {
        if (hubDataUniquex != null || addHub != null) {
            getHubDataUniquex().addHub = addHub;
        }
    }

    /**
     * Indicates whether auto-create behavior is enabled for this Hub.
     *
     * @return {@code true} if auto-create is enabled, otherwise {@code false}
     */
    public boolean isAutoCreate() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.bAutoCreate;
    }
    
    /**
     * Sets whether auto-create behavior is enabled for this Hub.
     *
     * @param bAutoCreate {@code true} to enable auto-creation of link targets
     */
    public void setAutoCreate(boolean bAutoCreate) {
        if (hubDataUniquex != null || bAutoCreate) {
            getHubDataUniquex().bAutoCreate = bAutoCreate;
        }
    }

    /**
     * Indicates whether duplicate objects are allowed during auto-create.
     *
     * @return {@code true} if duplicates are allowed, otherwise {@code false}
     */
    public boolean isAutoCreateAllowDups() {
        if (hubDataUniquex == null) return false;
        return hubDataUniquex.bAutoCreateAllowDups;
    }
    
    /**
     * Sets whether duplicate objects are allowed during auto-create operations.
     *
     * @param bAutoCreateAllowDups {@code true} to allow duplicates
     */
    public void setAutoCreateAllowDups(boolean bAutoCreateAllowDups) {
        if (hubDataUniquex != null || bAutoCreateAllowDups) {
            getHubDataUniquex().bAutoCreateAllowDups = bAutoCreateAllowDups;
        }
    }

}

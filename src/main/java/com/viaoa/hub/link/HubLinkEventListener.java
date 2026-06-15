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
package com.viaoa.hub.link;

import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectHubService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubInternalBridge;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;

/**
 * Internal listener created by {@link HubLinkDelegate} to keep two linked {@link Hub}s
 * synchronized when their active objects (AOs) or link properties change.
 *
 * <p>This class listens to events on the "linked-to" Hub (the Hub that a "from"
 * Hub is linked to via {@link Hub#setLink(Hub, String)}). Whenever the active
 * object or the linked property changes on the "to" Hub, this listener updates
 * the "from" Hub’s active object to stay consistent.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Listen for {@code afterChangeActiveObject} events on the linked-to Hub
 *       and call {@link HubLinkDelegate#updateLinkedToHub} to realign the
 *       linking Hub’s AO.</li>
 *   <li>Listen for {@code afterPropertyChange} on the target object’s link
 *       property and update the "from" Hub if the relationship property was
 *       changed directly.</li>
 *   <li>Handle many-to-many/private link cases where weak Hub references are not
 *       automatically tracked, adding missing Hub references as needed.</li>
 *   <li>On {@code onNewList}, re-establish link relationships after the
 *       "to" Hub’s list is replaced or refreshed.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Used only by {@link HubLinkDelegate}; never instantiated directly by user code.</li>
 *   <li>Ensures that cross-Hub AO synchronization and link-to-property
 *       propagation remain consistent even in many-to-many link configurations.</li>
 *   <li>Implements {@link java.io.Serializable} so that Hub link topology can be
 *       serialized with its parent Hub graph.</li>
 * </ul>
 */
public class HubLinkEventListener extends HubListenerAdapter implements java.io.Serializable {
	
	/**
	 * The Hub that this listener is monitoring for active-object and property changes.
	 */
	Hub linkToHub;
	
	/**
	 * The Hub whose active object must stay synchronized with the linked-to Hub.
	 */
	Hub fromHub;
	
	/**
	 * Flag indicating whether weak Hub references must be updated for many-to-many
	 * private link configurations.
	 */
	boolean bUpdateWeakHub;

	private HubInternalBridge faBridge = new HubInternalBridge();
	private Hub.FriendAccess faHub;
	
	
	/**
	 * Constructs a new listener that synchronizes the from-Hub with changes coming
	 * from the link-to Hub. Determines whether weak Hub references must be updated
	 * based on the link metadata.
	 *
	 * @param fromHub   the Hub that must follow updates from the linked-to Hub
	 * @param linkToHub the Hub this listener monitors for change events
	 */
	public HubLinkEventListener(Hub fromHub, Hub linkToHub) {
	    this.fromHub = fromHub;
	    this.linkToHub = linkToHub;  // hub that is linked to, that this HubListener is listening to.
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromHub);
	    
	    // 20130708
        OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromDetailToMaster(linkToHub);
        if (li != null && li.getPrivateMethod()) {
            if (og.objectsInternal().callObjectInfoIsMany2Many(li)) {
                bUpdateWeakHub = true;
            }
        }
		faHub = faBridge.getHubFriendAccess();
	}
	
	/**
	 * Called after the linked-to Hub changes its active object. Updates the
	 * from-Hub’s active object through {@link HubLinkDelegate#updateLinkedToHub}.
	 *
	 * @param hubEvent the event containing the new active object
	 */
	public @Override void afterChangeActiveObject(HubEvent hubEvent) {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromHub);
		og.hubsInternal().callHubLinkUpdateLinkedToHub(fromHub, linkToHub, hubEvent.getObject(), null);
	}
	
	/**
	 * Called after a property on the linked-to Hub’s active object changes.
	 * If the changed property matches the from-Hub’s link-to property, a link
	 * update is triggered.
	 *
	 * @param hubEvent the event describing the property change
	 */
	public @Override void afterPropertyChange(HubEvent hubEvent) {
	    if (hubEvent.getObject() == linkToHub.getActiveObject()) {
	    	String prop = hubEvent.getPropertyName(); 
            if (prop != null && prop.equalsIgnoreCase(faHub.getHubDataUnique(fromHub).getLinkToPropertyName())) {
        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromHub);
            	og.hubsInternal().callHubLinkUpdateLinkedToHub(fromHub, linkToHub, hubEvent.getObject(), prop);
            }
	    }
	}
	
	// 20130708 check if linkToHub is based on a M2M&private, where the oaObj.weakRefs[] do not have the hub
	//     if so, then need to add it
	/**
	 * Called when the linked-to Hub’s list is refreshed or replaced. Updates weak
	 * Hub references for many-to-many private links if required, then realigns
	 * the from-Hub by updating its active object.
	 *
	 * @param e the event signaling a new list
	 */
	@Override
	public void onNewList(HubEvent e) {
	    if (bUpdateWeakHub) {
    	    for (Object objx : linkToHub) {
    	        OAObject oaObj = (OAObject) objx;
        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);
    	        if (!og.objectsInternal().callObjectHubAddHub(oaObj, linkToHub, true)) {
    	            break;
    	        }
            }
	    }
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(fromHub);
        og.hubsInternal().callHubLinkUpdateLinkedToHub(fromHub, linkToHub, linkToHub.getAO(), null);
	}
}


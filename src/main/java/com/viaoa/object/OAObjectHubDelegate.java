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
package com.viaoa.object;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAArray;

/**
 * Maintains the set of Hubs that an {@link OAObject} is a member of, using
 * weak references so Hub membership does not prevent garbage collection.
 * Provides add/remove/query operations and integrates with client/server
 * cache behavior when objects enter or leave mastered Hubs.
 *
 * <p>Design highlights:</p>
 * <ul>
 *   <li>Membership stored as {@code WeakReference<Hub<?>>[]} with
 *       compaction and occasional resizing.</li>
 *   <li>Duplicate membership is prevented; GC’d hubs are pruned lazily.</li>
 *   <li>Small-array reuse and shared-weakref reuse minimize memory footprint
 *       for large lists.</li>
 *   <li>Many-to-many links with private reverse methods can opt out of
 *       tracking to avoid excessive references.</li>
 *   <li>In client mode, removal from the last mastered Hub can notify the
 *       server so the object is tracked in the server cache again.</li>
 * </ul>
 *
 * <p>All mutations are synchronized on the OAObject instance. Methods never
 * force lazy loading and never mutate relationship integrity; they only
 * maintain membership bookkeeping used by Hub/Event delegates.</p>
 *
 * @see Hub
 * @see OAObject
 * @see HubDetailDelegate
 * @see OAObjectEventDelegate
 */
public class OAObjectHubDelegate {

    private static Logger LOG = Logger.getLogger(OAObjectHubDelegate.class.getName());

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectHubService().??(oaObj);
    */
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}


    // 20120827 might be used later
    // send event to master object when a change is made to one of its reference hubs
    // called by HubEventDelegate when a change happens to a hub
    public static void fireMasterObjectHubChangeEvent(Hub thisHub, boolean bRefreshFlag) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return;
    	g.objects().getOAObjectHubService().fireMasterObjectHubChangeEvent(thisHub, bRefreshFlag);
    }

    public static boolean isInHub(OAObject oaObj) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().isInHub(oaObj);
    }
    
    public static boolean isInHubWithMaster(OAObject oaObj) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().isInHubWithMaster(oaObj);
    }
    
    public static boolean isInHubWithMaster(OAObject oaObj, Hub hubToIgnore) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().isInHubWithMaster(oaObj, hubToIgnore);
    }
    
    /**
     * Called by Hub when an OAObject is removed from a Hub.
     */
    public static void removeHub(final OAObject oaObj, Hub hub, boolean bIsOnHubFinalize) {
    	OAGraph g = getGraph(hub, oaObj);
    	if (g == null) return;
    	g.objects().getOAObjectHubService().removeHub(oaObj, hub, bIsOnHubFinalize);
    }

    /**
     * Return all Hubs that this object is a member of. Note: could have null values
     */
    public static Hub[] getHubReferences(OAObject oaObj) { // Note: this needs to be public
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return null;
    	return g.objects().getOAObjectHubService().getHubReferences(oaObj);
    }

    public static WeakReference<Hub<?>>[] getHubReferencesNoCopy(OAObject oaObj) { // Note: this needs to be public
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return null;
    	return g.objects().getOAObjectHubService().getHubReferencesNoCopy(oaObj);
    }
    
    /** removed 20180613
    // note:  need to use HubDataDelegate.contains(..) instead, since a certain type of hub wont be stored in obj.weakrefs
    public static boolean isInHub(OAObject oaObj, Hub hub) {
        if (oaObj == null || hub == null) return false;
        WeakReference<Hub<?>>[] refs = oaObj.weakhubs;
        int cnt = 0;
        for (int i = 0; refs != null && i < refs.length; i++) {
            WeakReference wr = refs[i];
            if (wr != null && wr.get() == hub) return true;
        }
        return false;
    }
    **/
    
    public static int getHubReferenceCount(OAObject oaObj) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return 0;
    	return g.objects().getOAObjectHubService().getHubReferenceCount(oaObj);
    }

    public static boolean addHub(OAObject oaObj, Hub hub) {
    	OAGraph g = getGraph(hub, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().addHub(oaObj, hub);
    }

    /**
     * Called by Hub when an OAObject is added to a Hub.
     */
    public static boolean addHub(final OAObject oaObj, final Hub hubOrig, final boolean bAlwaysAddIfM2M) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().addHub(oaObj, hubOrig, bAlwaysAddIfM2M);
    }

    
    /**
     * Used by Hub to read serialized objects. Check to see if this object is already loaded in a hub
     * with same LinkInfo.
     */
    public static boolean isAlreadyInHub(OAObject oaObj, OALinkInfo li) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().isAlreadyInHub(oaObj, li);
    }

    public static Hub getHub(OAObject oaObj, OALinkInfo li) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return null;
    	return g.objects().getOAObjectHubService().getHub(oaObj, li);
    }

    /**
     * Used by Hub.add() before adding, quicker then checking array
     */
    public static boolean isAlreadyInHub(OAObject oaObj, Hub hubFind) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().isAlreadyInHub(oaObj, hubFind);
    }


    protected static boolean getChanged(Hub thisHub, int changedRule, OACascade cascade) {
    	OAGraph g = getGraph(thisHub, null);
    	if (g == null) return false;
    	return g.objects().getOAObjectHubService().getChanged(thisHub, changedRule, cascade);
    }

    public static void saveAll(Hub hub, int iCascadeRule, OACascade cascade) {
    	//qqqqqqqqq method was protected
    	OAGraph g = getGraph(hub, null);
    	if (g == null) return;
    	g.objects().getOAObjectHubService().saveAll(hub, iCascadeRule, cascade);
    }

    public static void deleteAll(Hub hub, OACascade cascade) {
    	//qqqqqqqqq method was protected
    	OAGraph g = getGraph(hub, null);
    	if (g == null) return;
    	g.objects().getOAObjectHubService().deleteAll(hub, cascade);
    }

    public static void setMasterObject(Hub hub, OAObject oaObj, OALinkInfo liDetailToMaster) {
    	OAGraph g = getGraph(null, oaObj);
    	if (g == null) return;
    	g.objects().getOAObjectHubService().setMasterObject(hub, oaObj, liDetailToMaster);
    }

    public static void setMasterObject(Hub hub, OAObject oaObj, String nameFromMasterToDetail) {
    	OAGraph g = getGraph(hub, oaObj);
    	if (g == null) return;
    	g.objects().getOAObjectHubService().setMasterObject(hub, oaObj, nameFromMasterToDetail);
    }
}

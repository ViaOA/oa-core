package com.viaoa.graph.service.object;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.graph.service.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAArray;

public abstract class OAObjectHubService {
	private static final Logger LOG = Logger.getLogger(OAObjectHubService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final HubService srvcHub;
	private final OASyncService srvcSync;
	
    public OAObjectHubService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, HubService srvcHub, OASyncService srvcSync) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
    	if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
    	this.srvcSync = srvcSync;
    }
	
    public OAObjectService getObjectService() {
    	return srvcObject;
    }

    /**
     * Enables periodic diagnostic warnings when weak-hub arrays grow to large sizes.
     * Intended to help identify unusual Hub-membership patterns.
     */
    public static boolean ShowWarnings=true; // if weakHubs.len % 50

    // 20120827 might be used later
    // send event to master object when a change is made to one of its reference hubs
    // called by HubEventDelegate when a change happens to a hub
    public void fireMasterObjectHubChangeEvent(Hub thisHub, boolean bRefreshFlag) {
        if (thisHub == null) return;

        OAObject objMaster = srvcHub.getMasterObject(thisHub);
        if (objMaster == null) return;

        String prop = srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
        if (prop == null) return;
        /*
         * if (bRefreshFlag || thisHub.getSize() < 2) { updateMasterObjectEmptyHubFlag(thisHub, prop,
         * (OAObject)objMaster, true); }
         */
        srvcObject.getOAObjectEventService().sendHubPropertyChange((OAObject) objMaster, prop, thisHub, thisHub, null);
        srvcObject.getOAObjectCacheService().fireAfterPropertyChange((OAObject) objMaster, 
        		srvcObject.getOAObjectKeyService().getKey((OAObject) objMaster), 
        		prop, thisHub, thisHub, true, true);
    }

    public boolean isInHub(OAObject oaObj) {
        if (oaObj == null) return false;
        WeakReference<Hub<?>>[] weakhubs = faObject.getWeakHubs(oaObj);
        if (weakhubs == null) return false;
        for (WeakReference<Hub<?>> ref : weakhubs) {
            if (ref != null) {
                if (ref.get() != null) return true;
            }
        }
        return false;
    }

    public boolean isInHubWithMaster(OAObject oaObj) {
        return isInHubWithMaster(oaObj, null);
    }
    
    public boolean isInHubWithMaster(OAObject oaObj, Hub hubToIgnore) {
        if (oaObj == null) return false;
        
        synchronized (oaObj) {
	        WeakReference<Hub<?>>[] refs = faObject.getWeakHubs(oaObj);
	        if (refs == null) return false;
	        for (WeakReference<Hub<?>> ref : refs) {
	            if (ref != null) {
	                Hub h = ref.get();
	                if (h == hubToIgnore) continue;
	                if (h != null && h.getMasterObject() != null) return true;
	            }
	        }
        }
        return false;
    }

    /**
     * Called by Hub when an OAObject is removed from a Hub.
     */
    public void removeHub(final OAObject oaObj, Hub hub, boolean bIsOnHubFinalize) {
        if (oaObj == null) return;
        WeakReference<Hub<?>>[] weakhubs = faObject.getWeakHubs(oaObj);
        if (weakhubs == null) return;
        
        Hub hubx = hub.getRealHub();
        if (hubx != hub) {
            if (bIsOnHubFinalize) return; // the sharedHub is being finalized
            hub = hubx;
        }

        boolean bFound = false;
        synchronized (oaObj) {
        	weakhubs = faObject.getWeakHubs(oaObj);
            if (weakhubs == null) return;
            int currentSize = weakhubs.length;
            int lastEndPos = currentSize - 1;

            for (int pos = 0; !bFound && pos < currentSize; pos++) {
                if (weakhubs[pos] == null) break; // the rest will be nulls

                Hub hx = weakhubs[pos].get();

                bFound = (hx == hub);
                if (hx != null && !bFound) continue;
                
                if (currentSize < 4) {
                    // 20160105 weakhubs[] size <4 can be shared by other objs - need to create a new weakref[]
                    if (currentSize == 1) {
                    	weakhubs = null;
                    	faObject.setWeakHubs(oaObj, weakhubs);
                    }
                    else {
                    	weakhubs = (WeakReference<Hub<?>>[]) OAArray.removeAt(WeakReference.class, weakhubs, pos);
                    	faObject.setWeakHubs(oaObj, weakhubs);
                    }
                    if (!bFound && weakhubs != null) {
                        pos--; // need to revisit
                        currentSize--;  // array was resized
                        continue;
                    }
                    break;
                }
                else {
                	weakhubs[pos] = null;
    
                    // compress: get last one, move it back to this slot
                    for (; lastEndPos > pos; lastEndPos--) {
                        if (weakhubs[lastEndPos] == null) continue;
                        if (weakhubs[lastEndPos].get() == null) {
                        	weakhubs[lastEndPos] = null;
                            continue;
                        }
                        weakhubs[pos] = weakhubs[lastEndPos];
                        weakhubs[lastEndPos] = null;
                        if (!bFound) {
                            pos--; // need to revisit this slot (currentSize is still the same)
                        }
                        break;
                    }
                    if (!bFound) continue;
                    
                    if (currentSize > 10 && ((currentSize - lastEndPos) < (currentSize * .75))) {
                        // resize array
                        int newSize = lastEndPos + (lastEndPos / 10) + 1;
                        newSize = Math.min(lastEndPos + 20, newSize);
                        WeakReference<Hub<?>>[] newRefs = new WeakReference[newSize];
                        System.arraycopy(weakhubs, 0, newRefs, 0, lastEndPos);
                        weakhubs = newRefs;
                        faObject.setWeakHubs(oaObj, weakhubs);
                        currentSize = newSize;
                    }
                    if (weakhubs[0] == null) {
                    	weakhubs = null;
                    	faObject.setWeakHubs(oaObj, weakhubs);
                        break;
                    }
                    if (bFound) {
                        break;
                    }
                }
            }

            if (!srvcSync.isClient()) return;
            
            // could be a hub from hubMerger, that populates with One references
            // which means that the one reference keeps it from gc
            if (!bIsOnHubFinalize && hub.getMasterObject() != null) {
                // 20141201 add !bIsOnHubFinalize so that if it is from a Hub finalize, then dont 
                //    use the finalizer thread to send msg to server.
                if (!isInHubWithMaster(oaObj)) {
					final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
                    if (srvcOARemoteThread.shouldSendMessages() && !oaObj.isDeleted()) {
                        // CACHE_NOTE: if it was on the Server.cache, it was removed when it was added
                        // to a hub. Need to add to cache now that it is no longer in a hub.
                        
                        // 20150827 dont cache if one2one and owned, and it is assigned to owner
                        // which means that the owner will "hold on to it"
                        boolean b = true;
                        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj.getClass());
                        if (oi.isOwnedAndNoReverseMany()) {
                            OALinkInfo li = oi.getOwnedByOne();
                            if (li != null && srvcObject.getOAObjectPropertyService().getProperty(oaObj, li.getName()) != null) {
                                b = false;
                            }
                        }
                        
                        if (b) srvcObject.getOAObjectCSService().updateObjectsWithoutHubs(oaObj);
                    }
                }
            }
        }
    }

    /**
     * Return all Hubs that this object is a member of. Note: could have null values
     */
    public Hub[] getHubReferences(OAObject oaObj) { // Note: this needs to be public
        if (oaObj == null) return null;

        WeakReference<Hub<?>>[] refs = faObject.getWeakHubs(oaObj);
        if (refs == null) return null;

        Hub[] hubs = new Hub[refs.length];

        for (int i = 0; i < refs.length; i++) {
            WeakReference<Hub<?>> ref = refs[i];
            if (ref == null) continue;
            hubs[i] = ref.get();
        }
        return hubs;
    }

    public WeakReference<Hub<?>>[] getHubReferencesNoCopy(OAObject oaObj) {
        if (oaObj == null) return null;
        return faObject.getWeakHubs(oaObj);
    }

    public int getHubReferenceCount(OAObject oaObj) {
        if (oaObj == null) return 0;
        WeakReference<Hub<?>>[] refs = faObject.getWeakHubs(oaObj);
        int cnt = 0;
        for (int i = 0; refs != null && i < refs.length; i++) {
            if (refs[i] != null && refs[i].get() != null) cnt++;
        }
        return cnt;
    }

    public boolean addHub(OAObject oaObj, Hub hub) {
        // 20140313 was: addHub(oaObj, hub, true, false);
        return addHub(oaObj, hub, false);
    }
    
    /**
     * Called by Hub when an OAObject is added to a Hub.
     */
    public boolean addHub(final OAObject oaObj, final Hub hubOrig, final boolean bAlwaysAddIfM2M) {
        if (oaObj == null || hubOrig == null) return false;
        final Hub hub = hubOrig.getRealHub();

        // 20120702 dont store hub if M2M&Private: reverse linkInfo does not have a method.
        // since this could have a lot of references (ex: VetJobs JobCategory has m2m Jobs)
        if (!bAlwaysAddIfM2M) {
            OALinkInfo li = srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(hub);
            if (li != null && li.getPrivateMethod()) {
                if (srvcObject.getOAObjectInfoService().isMany2Many(li)) {
                    return false;
                }
            }
        }
        boolean bRemoveFromServerCache = false;
        boolean bReused = false;
        WeakReference<Hub<?>>[] weakhubs = faObject.getWeakHubs(oaObj);
        		
        synchronized (oaObj) {
            int pos;
            
            if (weakhubs == null) {
                // check to use same as another object in hub
                int x = Math.min(4, hub.getCurrentSize());
                for (int i=0; i<x; i++) {
                    OAObject objx = (OAObject) hub.getAt(i);
                    if (objx == null) break;
                    if (objx == oaObj) continue;
                    WeakReference<Hub<?>>[] wrs = faObject.getWeakHubs(objx);
                    if (wrs != null && wrs.length == 1 && wrs[0] != null && wrs[0].get() == hub) {
                        weakhubs = wrs;
                        faObject.setWeakHubs(oaObj, weakhubs);
                        bReused = true;
                        break;
                    }
                }
                if (!bReused) {
                	weakhubs = new WeakReference[1];
                    faObject.setWeakHubs(oaObj, weakhubs); 
                }
                pos = 0;
                // CACHE_NOTE: if it was on the Server.cache, it can be removed when it is added to a
                // hub. Need to add to cache if/when it is no longer in a hub.
                if (hub.getMasterObject() != null) {
                    bRemoveFromServerCache = true;
                }
            }
            else {
                // check for an empty slot at the end
                for (pos = weakhubs.length - 1; pos >= 0; pos--) {
                    if (weakhubs[pos] == null) {
                        if (pos != 0) continue;
                    }
                    else {
                        Hub h = weakhubs[pos].get(); 
                        if (h == hub) {
                            return false;
                        }
                        if (h == null) {
                            weakhubs[pos] = null;
                            if (pos != 0) continue;
                        }
                        else {
                            // make sure that hub is not already in the list
                            for (int i=0; i<pos; i++) {
                                if (weakhubs[i] == null) continue;
                                h = weakhubs[i].get();
                                if (h == hub) {
                                    return false;
                                }
                            }
                            pos++; 
                        }
                    }

                    if (pos < 3) {  //  check to see if it can use the same as another obj in hub
                        int x = Math.min(4, hub.getCurrentSize());
                        for (int i=0; i<x; i++) {
                            OAObject objx = (OAObject) hub.getAt(i);
                            if (objx == null) break;
                            if (objx == oaObj) continue;
                            WeakReference<Hub<?>>[] wrs = faObject.getWeakHubs(objx);
                            if (wrs == null) continue;
                            if (wrs.length != pos+1) continue;
                            
                            bReused = true;
                            for (int j=0; j<pos; j++) {
                                if (wrs[j] != weakhubs[j]) {
                                    bReused = false;
                                    break;
                                }
                            }
                            if (bReused) {
                                if (wrs[pos] == null || wrs[pos].get() != hub) bReused = false;
                                else {
                                    weakhubs = wrs;
                                    faObject.setWeakHubs(oaObj, weakhubs);
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (!bReused && (pos < 3 || pos >= weakhubs.length)) {  // else use open [pos]
                        // need to expand
                        int newSize = pos + 1;
                        if (pos > 3) {
                            newSize += (newSize / 10); 
                            newSize = Math.min(newSize, pos + 20);
                        }
                        WeakReference<Hub<?>>[] refs = new WeakReference[newSize];
    
                        int x = Math.min(weakhubs.length, refs.length);
                        System.arraycopy(weakhubs, 0, refs, 0, x);
                        weakhubs = refs;
                        faObject.setWeakHubs(oaObj, weakhubs);
                    }                    
                    break;
                }

                if (hub.getMasterObject() != null) {
                    bRemoveFromServerCache = true;
                    for (int i = 0; i < pos; i++) {
                        WeakReference<Hub<?>> ref = weakhubs[i];
                        if (ref == null) continue;
                        Hub h = ref.get();
                        if (h != null && h.getMasterObject() != null) {
                            bRemoveFromServerCache = false; // already done
                            break;
                        }
                    }
                }
            }
            if (!bReused) {
                // see if a weakRef=hub used by another object can be reused
                boolean b = false;
                int x = Math.min(4, hub.getCurrentSize());
                for (int i=0; !b && i<x; i++) {
                    OAObject objx = (OAObject) hub.getAt(i);
                    if (objx == null) break;
                    if (objx == oaObj) continue;
                    WeakReference<Hub<?>>[] wrs = faObject.getWeakHubs(objx);
                    if (wrs == null) continue;
                    for (WeakReference wr : wrs) {
                        if (wr == null) break;
                        if (wr.get() == hub) {
                            weakhubs[pos] = wr;
                            b = true;
                            break;
                        }
                    }
                }
                if (!b) {
                    weakhubs[pos] = new WeakReference(hub);
                }
                else aiReuseWeakRef.incrementAndGet();
                if (pos>0 && pos%50==0 && ShowWarnings) {
                    LOG.fine("object="+oaObj+", weakhubs="+pos);
                }
            }
        }
        if (bReused) aiReuseWeakRefArray.incrementAndGet();

        final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  

        if (bRemoveFromServerCache && srvcSync.isClient() && srvcOARemoteThread.shouldSendMessages()) {
        	srvcObject.getOAObjectCSService().updateObjectsWithoutHubs(oaObj);
        }
        return true;
    }

    
    /**
     * Counter tracking reuse of individual WeakReference instances to avoid
     * unnecessary object creation when adding an OAObject to a Hub.
     */
    public final AtomicInteger aiReuseWeakRefArray = new AtomicInteger();

    /**
     * Counter tracking reuse of individual WeakReference instances to avoid
     * unnecessary object creation when adding an OAObject to a Hub.
     */
    public static final AtomicInteger aiReuseWeakRef = new AtomicInteger();
    
    /**
     * Used by Hub to read serialized objects. Check to see if this object is already loaded in a hub
     * with same LinkInfo.
     */
    public boolean isAlreadyInHub(OAObject oaObj, OALinkInfo li) {
        if (oaObj == null || li == null) return false;

        WeakReference<Hub<?>>[] refs = faObject.getWeakHubs(oaObj);
        for (int i = 0; refs != null && i < refs.length; i++) {
            WeakReference<Hub<?>> ref = refs[i];
            if (ref == null) continue;
            Hub h = ref.get();
            if (h != null && srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(h) == li) return true;
        }
        return false;
    }

    public Hub getHub(OAObject oaObj, OALinkInfo li) {
        if (oaObj == null || li == null) return null;

        WeakReference<Hub<?>>[] refs = faObject.getWeakHubs(oaObj);
        for (int i = 0; refs != null && i < refs.length; i++) {
            WeakReference<Hub<?>> ref = refs[i];
            if (ref == null) continue;
            Hub h = ref.get();
            if (h != null && srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(h) == li) return h;
        }
        return null;
    }
    
    /**
     * Used by Hub.add() before adding, quicker then checking array
     */
    public boolean isAlreadyInHub(OAObject oaObj, Hub hubFind) {
        if (oaObj == null || hubFind == null) return false;
        hubFind = hubFind.getRealHub();
        boolean b = _isAlreadyInHub(oaObj, hubFind);
        if (b) return true;

        OALinkInfo li = null;
        Object master = hubFind.getMasterObject();
        if (master != null) li = srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(hubFind);        
        if (li == null) {
            if (hubFind.isOAObject()) return false;
            return srvcHub.getHubDataService().containsDirect(hubFind, oaObj);
        }

        // could be in the hub, but not in weakHubs, if M2M and private
        // ex: VJ jobCategories M2M Jobs, where jobCategory objects dont have weakhub for
        // all of the Job.jobCategories Hubs that exist
        if (li.getPrivateMethod()) { // if hub method is off
            if (srvcObject.getOAObjectInfoService().isMany2Many(li)) { // m2m objects do not have Hub in weakRef[]
                return srvcHub.getHubDataService().containsDirect(hubFind, oaObj);
            }
        }
        return false;
    }

    private boolean _isAlreadyInHub(OAObject oaObj, Hub hubFind) {
        if (oaObj == null) return false;

        WeakReference<Hub<?>>[] refs = faObject.getWeakHubs(oaObj);
        for (int i = 0; refs != null && i < refs.length; i++) {
            WeakReference<Hub<?>> ref = refs[i];
            if (ref == null) continue;
            Hub h = ref.get();
            if (h == hubFind) return true;
        }
        return false;
    }

    
    public boolean getChanged(Hub thisHub, int changedRule, OACascade cascade) {
        return srvcHub.getChanged(thisHub, changedRule, cascade);
    }

    public void saveAll(Hub hub, int iCascadeRule, OACascade cascade) {
    	//qqqqqqqqqq method was protected
        if (hub == null) return; 
        srvcHub.getHubSaveService().saveAll(hub, iCascadeRule, cascade); // cascade save and update M2M links
    }

    public void deleteAll(Hub hub, OACascade cascade) {
    	//qqqqqqqqqq method was protected
    	srvcHub.getHubDeleteService().deleteAll(hub, cascade); // cascade delete and update M2M links
    }

    public void setMasterObject(Hub hub, OAObject oaObj, OALinkInfo liDetailToMaster) {
        if (srvcHub.getHubDetailService().getMasterObject(hub) == null) {
        	srvcHub.getHubDetailService().setMasterObject(hub, oaObj, liDetailToMaster);
        }
    }

    public void setMasterObject(Hub hub, OAObject oaObj, String nameFromMasterToDetail) {
        if (hub == null || oaObj == null || nameFromMasterToDetail == null) return;
        Object objx = srvcHub.getHubDetailService().getMasterObject(hub);
        if (objx != null && objx == oaObj) {
            return;  // already set
        }

        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(oaObj);
        
        OALinkInfo li = oi.getLinkInfo(nameFromMasterToDetail);
        if (li == null) return;
        li = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
        srvcHub.getHubDetailService().setMasterObject(hub, oaObj, li);
    }

    
    
    
    

//qqqqqqqqqqqqqqq next    
    /*	
	@OAParentProvided (example = "")
	public abstract 

	@OAParentProvided (example = "")
	public abstract

	@OAParentProvided (example = "")
	public abstract 

	@OAParentProvided (example = "")
	public abstract
*/	 


}


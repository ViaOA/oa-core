package com.viaoa.graph.service.hub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

public abstract class HubStatusService {
	private final Logger LOG = Logger.getLogger(HubStatusService.class.getName());

	private final Hub.FriendAccess faHub;

	
	/**
	 * Returns whether the Hub is marked as changed.
	 *
	 * @param thisHub the Hub to check
	 * @return {@code true} if the Hub is marked as changed, otherwise {@code false}
	 */
	public boolean getChanged(Hub<?> thisHub) {
		return faHub.getHubData(thisHub).getChanged();
	}

	/**
	 * Determines whether this hub or any of its contained OAObjects are marked as
	 * changed according to the supplied cascade rules.
	 *
	 * <p>
	 * The method first checks whether this hub has already been processed in the
	 * current cascade; if so, it returns {@code false}. It then evaluates the hub’s
	 * own changed state. If cascade rules allow, it iterates through each object in
	 * the hub and checks whether any OAObject reports a changed state.
	 *
	 * @param thisHub      the hub being evaluated
	 * @param iCascadeRule the cascade rule used to determine how far change
	 *                     detection should propagate
	 * @param cascade      the cascade tracker used to prevent reprocessing
	 * @return {@code true} if the hub or any contained OAObject is changed;
	 *         otherwise {@code false}
	 */
	public <T extends OAObject> boolean getChanged(Hub<T> thisHub, int iCascadeRule, OACascade cascade) {
		if (cascade.wasCascaded(thisHub, true)) {
			return false;
		}

		if (getChanged(thisHub)) {
			return true;
		}
		if (iCascadeRule == OAObject.CASCADE_NONE) {
			return false;
		}

		for (T obj : thisHub) {
			if (callObjectChangeGetChanged(obj, iCascadeRule, cascade)) {
				return true;
			}
		}
		return false;
	}
	
	
	
	/**
	 * Updates the Hub’s changed flag and increments its change counter
	 * when the value transitions. Clearing the changed flag also clears
	 * tracked add/remove lists. When marking as changed, the master
	 * object may also be marked changed based on link metadata.
	 *
	 * @param thisHub   the hub whose changed state is being updated
	 * @param bChanged  the new changed value
	 */
	public <T extends OAObject> void setChanged(Hub<T> thisHub, boolean bChanged) {
	    if (thisHub == null) return;
	    
	    final HubData<T> hd = faHub.getHubData(thisHub);
        boolean old = hd.getChanged();
        if (bChanged == old) return;
        hd.setChanged(bChanged);
        if (bChanged != old) hd.incrementChangeCount();
        if (!bChanged) {
        	callHubDataClearHubChanges(thisHub);
        }
        else {  // 20180529 if changed, then masterObject needs to be flagged as changed
            OAObject obj = thisHub.getMasterObject();
            if (obj != null) {
                OALinkInfo li = callHubDetailGetLinkInfoFromMasterHubToDetail(thisHub);
                if (li != null && (li.getType() == li.MANY)) {
                    boolean bx = (li.getOwner() || li.getCascadeSave());
                    if (!bx) { 
                        OALinkInfo rli = li.getReverseLinkInfo();
                        bx = (rli != null && rli.getType() == li.MANY);
                    }
                    if (bx) obj.setChanged(true);
                }
            }
        }
    }
	

	
	
	
	/**
	 * Enumeration describing the synchronization state of a hub during updates.
	 *
	 * <ul>
	 *   <li>{@code InSync} – the hub is correctly aligned with its master or linked
	 *       state.</li>
	 *   <li>{@code DetailDisconnectedFromMaster} – the detail hub does not match its
	 *       expected master state.</li>
	 *   <li>{@code DetailHubNotSameAsMasterObject} – the detail hub contains a
	 *       different object than the master hub’s active object.</li>
	 *   <li>{@code HubMergerNotUpdated} – a hub merger is not in sync with its
	 *       source hubs.</li>
	 * </ul>
	 */
	public static enum HubCurrentStateEnum {
		InSync,
		DetailDisconnectedFromMaster,
		DetailHubNotSameAsMasterObject, // caused when object/hubs are in flux (hub event that is calling listeners and changing linkages)
		HubMergerNotUpdated
	}
	
	
	public HubStatusService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	
	/**
	 * Evaluates the current synchronization state of the hub, optionally populating
	 * a replacement hub or list when a mismatch is detected. This is a wrapper that
	 * delegates to the internal recursive implementation.
	 *
	 * @param thisHub the hub being examined
	 * @param hubNew  optional hub to receive corrected state contents
	 * @param alNew   optional list to receive corrected state contents
	 * @return the hub’s synchronization status
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
    public <T extends OAObject> HubCurrentStateEnum getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew) {
        return _getCurrentState(thisHub, hubNew, alNew, new HashSet<Hub>());
    }

    /**
     * Internal recursive implementation for evaluating hub synchronization state.
     * Prevents cyclic traversal using the provided hub set. Traverses master hubs,
     * shared hubs, mergers, combined hubs, and filters to determine whether the hub
     * is aligned with its correct source.
     *
     * @param thisHub the hub being evaluated
     * @param hubNew  optional hub for corrected content
     * @param alNew   optional list for corrected content
     * @param hmHub   set of hubs visited to prevent cycles
     * @return the computed synchronization status, or {@code null} when a cycle is
     *         detected
     */
	@SuppressWarnings({"unchecked","rawtypes"})
    public <T extends OAObject> HubCurrentStateEnum _getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew, final Set<Hub> hmHub) {
		if (thisHub == null) {
			return HubCurrentStateEnum.InSync;
		}
		if (hmHub.contains(thisHub)) {
            return null;
		}
		hmHub.add(thisHub);

		Hub<?> hub = thisHub;
		Hub<?> hubMaster;
		boolean bHasMaster = false;
		for (int i = 0;; i++, hub = hubMaster) {
			HubDataMaster dm = callHubDetailGetDataMaster(hub, true);

			hubMaster = dm.getMasterHub();
			if (hubMaster == null) {
				break; // check for hubMerger
			}
			bHasMaster = true;

			final Object objMaster = hubMaster.getAO();
			if (objMaster == dm.getMasterObject()) {
				if (objMaster == null && thisHub.getSize() > 0) {
					return HubCurrentStateEnum.DetailDisconnectedFromMaster;
				}
				continue;
			}

			if (i > 0) {
				return HubCurrentStateEnum.DetailDisconnectedFromMaster;
			}

			if (objMaster != null && (hubNew != null || alNew != null)) {
				// find correct hub
				OALinkInfo li = dm.getDetailToMasterLinkInfo();
				if (li != null) {
					Object value = li.getReverseLinkInfo().getValue(objMaster);
					if (value != null) {
						if (value instanceof Hub) {
							if (hubNew != null) {
								hubNew.setSharedHub((Hub<T>) value);
							}
							if (alNew != null) {
								for (T objNext : ((Hub<T>) value)) {
									alNew.add(objNext);
								}
							}
						} else {
							if (hubNew != null) {
								hubNew.add((T) value);
							}
							if (alNew != null) {
								alNew.add((T) value);
							}
						}
					}
				}
			}
			return HubCurrentStateEnum.DetailHubNotSameAsMasterObject;
		}

		// check to see if hub is derived from another Hub, then check it

		hub = callHubShareGetMainSharedHub(hub);

		HubMerger<?, ?> hubMerger = null;
		HubCombined<T> hubCombined = null;
		HubFilter<T> hubFilter = null;

		HubListener<?>[] hls = callHubEventGetAllListeners(hub);
		
		if (hls != null) {
			for (HubListener<?> hl : hls) {
				if (!(hl instanceof HubListenerAdapter)) {
					continue;
				}
				HubListenerAdapter<?> hla = (HubListenerAdapter<?>) hl;
				Object listener = hla.getListener();
				if (listener instanceof HubMerger) {
					hubMerger = (HubMerger) hla.getListener();
					Hub hubx = hubMerger.getCombinedHub();
					if (hubx == hub) {
						break;
					}
					hubMerger = null;
				} else if (listener instanceof HubCombined) {
					hubCombined = (HubCombined) hla.getListener();
					Hub hubx = hubCombined.getMasterHub();
					if (hubx == hub) {
						break;
					}
					hubCombined = null;
				} else if (listener instanceof HubFilter) {
					hubFilter = (HubFilter) hla.getListener();
					Hub hubx = hubFilter.getHub();
					if (hubx == hub) {
						break;
					}
					hubFilter = null;
				}

			}
		}

		if (hubFilter != null) {
			Hub<?> hubx = hubFilter.getMasterHub();

			HubCurrentStateEnum hcs = _getCurrentState(hubx, null, null, hmHub);
			if (hcs == HubCurrentStateEnum.InSync) {
				return hcs;
			}
			if (hubNew == null && alNew == null) {
				return hcs;
			}

			Hub<T> hubTemp = new Hub<T>();
			_getCurrentState( (Hub<OAObject>) hubx, (Hub<OAObject>) hubTemp, null, hmHub);

			for (T objx : hubTemp) {
				if (!hubFilter.isUsed(objx)) {
					continue;
				}
				if (hubNew != null) {
					hubNew.add(objx);
				}
				if (alNew != null) {
					alNew.add(objx);
				}
			}

		} else if (hubCombined != null) {
			ArrayList<Hub<T>> al = hubCombined.getHubs();
			if (al != null) {
				HubCurrentStateEnum hcs = null;
				for (Hub<T> hubx : al) {
					hcs = _getCurrentState(hubx, null, null, hmHub);
					if (hcs != HubCurrentStateEnum.InSync) {
						break;
					}
				}
				if (hcs == null) {
					return HubCurrentStateEnum.InSync;
				}
				if (hubNew == null && alNew == null) {
					return hcs;
				}

				for (Hub hubx : al) {
					hcs = _getCurrentState(hubx, hubNew, alNew, hmHub);
				}
				return hcs;
			}

		} else if (hubMerger != null) {
			Hub hubx = hubMerger.getRootHub();

			HubCurrentStateEnum hcs = _getCurrentState(hubx, null, null, hmHub);

			if (hcs == HubCurrentStateEnum.InSync) {
				if (!callThreadLocalIsHubMergerChanging() && !hubMerger.isLoadingCombinedHub()) {
					return hcs;
				}
			}

			if (hubNew == null && alNew == null) {
				return HubCurrentStateEnum.HubMergerNotUpdated;
			}

			Hub<T> hubTemp = new Hub<T>();

			_getCurrentState(hubx, hubTemp, null, hmHub);

			OAFinder finder = new OAFinder(hubMerger.getPath());

			List al;
			if (hubMerger.getUseAll()) {
				al = finder.find(hubTemp);
			} else {
				// ?? not sure that AO will be set
				al = finder.find((OAObject) hubTemp.getAO());
			}

			if (hubNew != null) {
				hubNew.add((List<T>) al);
			}
			if (alNew != null) {
				alNew.addAll((List<T>) al);
			}

			return HubCurrentStateEnum.HubMergerNotUpdated;
		}
		return HubCurrentStateEnum.InSync;
	}

    
	/**
	 * Determines whether the hub is in a valid state. A hub is invalid if its
	 * master hub exists but has no active master object, or if any linked hub is
	 * invalid and cannot auto-create missing objects. If an addHub exists, its
	 * validity is also checked recursively.
	 *
	 * @param thisHub the hub being evaluated
	 * @return {@code true} if the hub is valid; otherwise {@code false}
	 */
	public boolean isValid(final Hub<?> thisHub) {
		HubDataMaster dm = callHubDetailGetDataMaster(thisHub, true);
		if (dm.getMasterHub() != null && dm.getMasterObject() == null) {
			return false;
		}

		// 20181119 reworked to check other hubs for hubWithLink
		Hub<?> h = callHubLinkGetHubWithLink(thisHub, true);
		if (h != null) {
			Hub<?> hx = faHub.getHubDataUnique(h).getLinkToHub();
			if (hx != null) {
				if (!isValid(hx)) {
					return false;
				}
				
				if (faHub.getHubDataActive(hx).getActiveObject() == null) {
					if (!faHub.getHubDataUnique(h).isAutoCreate()) {
						return false;
					}
				}
			}
		}

		HubDataUnique<?> hdu = faHub.getHubDataUnique(thisHub);
		if (hdu.getAddHub() != null) {
			return isValid(hdu.getAddHub());
		}
		return true;
	}
    

	
	
	/**
	 * Updates referenceability settings for this hub and its parent objects. If the
	 * hub is server-side and the object class supports weak referencing, this method
	 * adjusts weak-reference behavior based on whether references should be
	 * maintained. When enabling referenceability, parent objects are also updated.
	 *
	 * @param hub            the hub whose referenceability is being updated
	 * @param bReferenceable whether objects referenced by this hub should remain
	 *                       strongly referenceable
	 */
	public void setReferenceable(Hub<?> hub, boolean bReferenceable) {
		if (hub == null) {
			return;
		}
		if (!callSyncIsServer()) {
			return;
		}

		OAObjectInfo oi = callObjectInfoGetOAObjectInfo(hub.getObjectClass());
		if (!callObjectInfoIsWeakReferenceable(oi)) {
			return;
		}
		boolean bSupportStorage = oi.getSupportsStorage();

		OAObject master = callHubMasterGetMasterObject(hub);
		if (master == null) return;

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		if (li == null) {
			return;
		}
		OALinkInfo liRev = li.getReverseLinkInfo();
		if (liRev == null) {
			return;
		}

		if (liRev.getCacheSize() > 0) {
			if (bReferenceable || bSupportStorage) {
				boolean b = callObjectPropertySetPropertyWeakRef(master, liRev.getName(), !bReferenceable, hub);
				if (!b) {
					return; // already done, dont need to check/change parents
				}
			}
		}

		if (bReferenceable) {
			// make parents referenceable
			callObjectPropertySetReferenceable(master, bReferenceable);
		}
	}
	
	
	public abstract HubDataMaster callHubDetailGetDataMaster(final Hub<?> thisHub, boolean bIncludedFilteredHub);
	public abstract <T extends OAObject> Hub<T> callHubShareGetMainSharedHub(Hub<T> hub);
	public abstract <T extends OAObject> HubListener<T>[] callHubEventGetAllListeners(Hub<T> thisHub);	
	public abstract boolean callThreadLocalIsHubMergerChanging();

	public abstract <T extends OAObject> Hub<T> callHubLinkGetHubWithLink(final Hub<T> thisHub, boolean bIncludeCopiedHubs);
	public abstract boolean callObjectChangeGetChanged(final OAObject oaObj, int iCascadeRule, OACascade cascade);
    
	public abstract boolean callSyncIsServer();
	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(Class<?> clazz);
	public abstract boolean callObjectInfoIsWeakReferenceable(OAObjectInfo oi);
	public abstract OAObject callHubMasterGetMasterObject(Hub<?> hub);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);	
	public abstract boolean callObjectPropertySetPropertyWeakRef(OAObject oaObj, String name, boolean bToWeakRef, Object value);				
	public abstract void callObjectPropertySetReferenceable(OAObject obj, boolean bReferenceable);

	public abstract void callHubDataClearHubChanges(Hub<?> thisHub);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<?> thisDetailHub);
}




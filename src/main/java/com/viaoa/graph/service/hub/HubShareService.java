package com.viaoa.graph.service.hub;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.*;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.OAFilter;

public class HubShareService {
	private final Logger LOG = Logger.getLogger(HubShareService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;

	public HubShareService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
		if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
		this.srvcHub = srvcHub;
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	/**
	 * Returns all Hubs that share the same underlying HubData as {@code thisHub}.
	 *
	 * <p>Equivalent to calling {@link #getAllSharedHubs(Hub, boolean, OAFilter)}
	 * with {@code bChildrenOnly = false} and no filter.</p>
	 *
	 * @param thisHub the Hub whose shared group is requested
	 * @return array of shared Hubs (possibly size 0), including {@code thisHub}
	 */
	public Hub[] getAllSharedHubs(Hub thisHub) {
		return getAllSharedHubs(thisHub, false, null);
	}

	/**
	 * Returns all Hubs that share the same data as {@code thisHub}, optionally
	 * restricting results to children in the shared chain.
	 *
	 * @param thisHub        the Hub to evaluate
	 * @param bChildrenOnly  true to return only Hubs directly shared from thisHub
	 * @return array of shared Hubs
	 */
	public Hub[] getAllSharedHubs(Hub thisHub, boolean bChildrenOnly) {
		return getAllSharedHubs(thisHub, bChildrenOnly, null);
	}

	/**
	 * Returns all shared Hubs that satisfy the given filter.
	 *
	 * @param thisHub the Hub whose shared group is being enumerated
	 * @param filter  an OAFilter determining which Hubs to include
	 * @return array of filtered shared Hubs
	 */
	public Hub[] getAllSharedHubs(Hub thisHub, OAFilter<Hub> filter) {
		return getAllSharedHubs(thisHub, false, filter);
	}

	/**
	 * Returns all Hubs sharing the same HubData as {@code thisHub}, with
	 * optional child-only restriction and filtering.
	 *
	 * @param thisHub       the Hub whose relationships are examined
	 * @param bChildrenOnly true to restrict results to downstream shared Hubs
	 * @param filter        optional filter to select which Hubs to return
	 * @return array of shared Hubs
	 */
	public Hub[] getAllSharedHubs(Hub thisHub, boolean bChildrenOnly, OAFilter<Hub> filter) {
		return getAllSharedHubs(thisHub, bChildrenOnly, filter, false, false);
	}

	/**
	 * Core implementation for discovering all shared Hubs, including those
	 * linked through HubFilters or AO-sharing relationships.
	 *
	 * @param thisHub              base Hub
	 * @param bChildrenOnly        restrict to descendants only
	 * @param filter               filter applied to discovered Hubs
	 * @param bIncludeFilteredHubs whether HubFilter-based shared Hubs are included
	 * @param bOnlyIfSharedAO      true to include only Hubs sharing the AO source
	 * @return array of discovered shared Hubs
	 */
	public Hub[] getAllSharedHubs(Hub thisHub, boolean bChildrenOnly, OAFilter<Hub> filter, boolean bIncludeFilteredHubs,
			boolean bOnlyIfSharedAO) {

		if (thisHub == null) {
			return null;
		}

		Hub h = thisHub;
		if (!bChildrenOnly) {
			h = getMainSharedHub(h);
		}
		ArrayList<Hub> alHub = new ArrayList<Hub>(10);
		_getAllSharedHubs(h, thisHub, alHub, filter, 0, bIncludeFilteredHubs, bOnlyIfSharedAO, bIncludeFilteredHubs);
		Hub[] hubs = new Hub[alHub.size()];
		alHub.toArray(hubs);
		return hubs;
	}

	/**
	 * Recursive worker used by getAllSharedHubs() to traverse shared Hub
	 * relationships via weak references, HubFilters, and HubShareAO links.
	 *
	 * @param hub          current Hub in traversal
	 * @param findHub      originating Hub used for AO-sharing checks
	 * @param alHub        accumulator list
	 * @param filter       filter deciding whether to include the current Hub
	 * @param cnter        recursion depth counter
	 * @param bIncludeFilteredHubs include HubFilter-based paths
	 * @param bOnlyIfSharedAO      restrict traversal to AO-sharing Hubs
	 * @param bIncludeHubShareAO   include HubShareAO-linked Hubs
	 */
	private void _getAllSharedHubs(final Hub hub, final Hub findHub, final ArrayList<Hub> alHub, final OAFilter<Hub> filter,
			final int cnter, final boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO, boolean bIncludeHubShareAO) {

		if (filter == null || filter.isUsed(hub)) {
			alHub.add(hub);
		}

		WeakReference<Hub>[] refs = srvcHub.getHubShareService().getSharedWeakHubs(hub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub h2 = ref.get();
			if (h2 == null) {
				continue;
			}
			if (bOnlyIfSharedAO && !srvcHub.getHubShareService().isUsingSameSharedAO(findHub, h2)) {
				continue;
			}
			_getAllSharedHubs(h2, findHub, alHub, filter, cnter + 1, bIncludeFilteredHubs, bOnlyIfSharedAO, bIncludeHubShareAO);
		}

		if (!bIncludeFilteredHubs || cnter > 0) {
			return;
		}

		HubFilter hf = getHubFilter(hub);
		if (hf != null) {
			if (!bOnlyIfSharedAO || hf.isSharingAO()) {
				Hub mh = hf.getMasterHub();
				Hub h = getMainSharedHub(mh);
				// note: use "mh" instead of findHub, since it is going thru a hubFiler
				_getAllSharedHubs(h, mh, alHub, filter, 0, bIncludeFilteredHubs, bOnlyIfSharedAO, bIncludeHubShareAO);
			}
		}

		if (!bIncludeHubShareAO) {
			return;
		}
		HubShareAO hs = getHubShareAO(hub);
		if (hs != null) {
			Hub mh = hs.getHub2();
			if (mh == hub) {
				mh = hs.getHub1();
			}
			Hub h = getMainSharedHub(mh);
			// note: use "mh" instead of findHub, since it is going thru a hubFilter
			_getAllSharedHubs(h, mh, alHub, filter, 0, bIncludeFilteredHubs, bOnlyIfSharedAO, (h != mh));
		}
	}

	/**
	 * Returns the HubCopy associated with the shared Hub group, if present.
	 * Searches the main shared Hub’s listeners for a HubCopy instance.
	 *
	 * @param thisHub the Hub to examine
	 * @return the HubCopy instance, or null if none exists
	 */
	public HubCopy getHubCopy(Hub thisHub) {
		Hub h = srvcHub.getHubShareService().getMainSharedHub(thisHub);
		if (faHub.getHubDataMaster(h).getMasterObject() != null || faHub.getHubDataMaster(h).getMasterHub() != null) {
			// filtered hubs will not have a master
			return null;
		}

		// find a HubFilter in the listener list
		HubListener[] hls = srvcHub.getHubEventService().getHubListeners(h);
		if (hls != null) {
			for (HubListener hl : hls) {
				if (hl instanceof HubCopy) {
					return (HubCopy) hl;
				}
			}
		}
		return null;
	}

	/**
	 * Locates a HubFilter attached to the main shared Hub.
	 *
	 * @param thisHub the Hub to examine
	 * @return a HubFilter instance, or null if none is found
	 */
	public HubFilter getHubFilter(Hub thisHub) {
		Hub h = srvcHub.getHubShareService().getMainSharedHub(thisHub);
		if (faHub.getHubDataMaster(h).getMasterObject() != null || faHub.getHubDataMaster(h).getMasterHub() != null) {
			// filtered hubs will not have a master
			return null;
		}

		// find a HubFilter in the listener list
		HubListener[] hls = srvcHub.getHubEventService().getHubListeners(h);
		if (hls != null) {
			for (HubListener hl : hls) {
				if (hl instanceof HubFilter) {
					return (HubFilter) hl;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the HubShareAO listener (if any) that synchronizes Active Objects
	 * for the given Hub’s shared group.
	 *
	 * @param thisHub the Hub to inspect
	 * @return HubShareAO listener or null
	 */
	public HubShareAO getHubShareAO(Hub thisHub) {
		Hub h = srvcHub.getHubShareService().getMainSharedHub(thisHub);

		// find a HubShareAO in the listener list
		HubListener[] hls = srvcHub.getHubEventService().getHubListeners(h);
		if (hls == null || hls.length == 0) {
			return null;
		}
		for (HubListener hl : hls) {
			if (hl instanceof HubShareAO) {
				return (HubShareAO) hl;
			}
		}
		return null;
	}

	/**
	 * Returns the Hub that {@code thisHub} is shared with, optionally
	 * including filtered or AO-sharing relationships.
	 *
	 * @param thisHub             the Hub whose shared parent is requested
	 * @param bIncludeFilteredHubs include HubFilters when resolving shared hubs
	 * @param bOnlyIfSharedAO     restrict results to AO-sharing cases
	 * @return the shared Hub or null
	 */
	public Hub getSharedHub(final Hub thisHub, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
		if (thisHub == null) {
			return null;
		}

		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			if (bOnlyIfSharedAO && !srvcHub.getHubShareService().isUsingSameSharedAO(thisHub, faHub.getHubDataUnique(thisHub).getSharedHub())) {
				return null;
			}
			return faHub.getHubDataUnique(thisHub).getSharedHub();
		}
		if (!bIncludeFilteredHubs) {
			return null;
		}

		// a HubCopy could also be sharing the AO
		HubCopy hc = getHubCopy(thisHub);
		if (hc != null) {
			if (!bOnlyIfSharedAO || hc.isSharingAO()) {
				return hc.getMasterHub();
			}
		}
		HubShareAO hs = getHubShareAO(thisHub);
		if (hs != null) {
			Hub mh = hs.getHub2();
			if (mh == thisHub) {
				mh = hs.getHub1();
			}
			return mh;
		}
		return null;
	}

	/**
	 * Traverses the shared-Hub graph to find the first Hub satisfying the
	 * given filter.
	 *
	 * @param thisHub              starting Hub
	 * @param filter               filter to test against
	 * @param bIncludeFilteredHubs include HubFilters in traversal
	 * @param bOnlyIfSharedAO      restrict traversal to AO-sharing Hubs
	 * @return the first matching Hub, or null if none found
	 */
	public Hub getFirstSharedHub(Hub thisHub, OAFilter<Hub> filter, boolean bIncludeFilteredHubs, boolean bOnlyIfSharedAO) {
		Hub h = getMainSharedHub(thisHub);
		return _getFirstSharedHub(h, thisHub, filter, bIncludeFilteredHubs, 0, bOnlyIfSharedAO, bIncludeFilteredHubs);
	}

	/**
	 * Recursive worker that navigates shared-Hub relationships to find the
	 * first Hub matching the provided filter.
	 *
	 * @param thisHub current Hub being tested
	 * @param findHub originating Hub used for AO-sharing rules
	 * @param filter  evaluation filter
	 * @param bIncludeFilteredHubs include HubFilter paths
	 * @param cnter   recursion depth
	 * @param bOnlyIfSharedAO restrict traversal to AO-sharing nodes
	 * @param bIncludeHubShareAO include HubShareAO-linked Hubs
	 * @return a matching Hub or null
	 */
	private Hub _getFirstSharedHub(
			final Hub thisHub, final Hub findHub,
			final OAFilter<Hub> filter, final boolean bIncludeFilteredHubs,
			final int cnter, boolean bOnlyIfSharedAO, boolean bIncludeHubShareAO) {

		if (filter == null) {
			return thisHub;
		}

		// first try a quickcheck on the main shared hub
		if (!bOnlyIfSharedAO || srvcHub.getHubShareService().isUsingSameSharedAO(findHub, thisHub)) {
			if (filter.isUsed(thisHub)) {
				return thisHub;
			}
		}

		WeakReference<Hub>[] refs = srvcHub.getHubShareService().getSharedWeakHubs(thisHub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub h2 = ref.get();
			if (h2 == null) {
				continue;
			}

			Hub hx = _getFirstSharedHub(h2, findHub, filter, bIncludeFilteredHubs, cnter + 1, bOnlyIfSharedAO, bIncludeHubShareAO);
			if (hx != null) {
				return hx;
			}
		}
		if (!bIncludeFilteredHubs || cnter > 0) {
			return null;
		}

		// not found, check to see if there is a HubCopy that is shared
		HubFilter hf = getHubFilter(thisHub);
		if (hf != null) {
			if (!bOnlyIfSharedAO || hf.isSharingAO()) {
				Hub mh = hf.getMasterHub();
				Hub h = getMainSharedHub(mh);
				// note: use "mh" instead of findHub, since this is going thru a hubFilter
				Hub hx = _getFirstSharedHub(h, mh, filter, bIncludeFilteredHubs, 0, bOnlyIfSharedAO, bIncludeHubShareAO);
				if (hx != null) {
					return hx;
				}
			}
		}

		if (!bIncludeHubShareAO) {
			return null;
		}
		HubShareAO hs = getHubShareAO(thisHub);
		if (hs != null) {
			Hub mh = hs.getHub2();
			if (mh == thisHub) {
				mh = hs.getHub1();
			}
			Hub h = getMainSharedHub(mh);
			// note: use "mh" instead of findHub, since this is going thru a hubFilter
			boolean b = ((mh != h) && (faHub.getHubDataActive(mh) != faHub.getHubDataActive(h)));
			Hub hx = _getFirstSharedHub(h, mh, filter, bIncludeFilteredHubs, 0, bOnlyIfSharedAO, b);
			if (hx != null) {
				return hx;
			}
		}
		return null;
	}

	/**
	 * Returns the root of a shared-Hub chain by following the sharedHub links
	 * upward until no further parent exists.
	 *
	 * @param hub starting Hub
	 * @return the root shared Hub
	 */
	public Hub getMainSharedHub(Hub hub) {
		Hub h = hub;
		for (;;) {
			Hub hx = h.getSharedHub();
			if (hx == null) {
				break;
			}
			h = hx;
		}
		return h;
	}

	/**
	 * Determines whether two Hubs share the same HubData instance.
	 *
	 * @param hub1 first Hub
	 * @param hub2 second Hub
	 * @return true if both use identical HubData
	 */
	public boolean isUsingSameSharedHub(Hub hub1, Hub hub2) {
		if (hub1 == null || hub2 == null) {
			return false;
		}
		return faHub.getHubData(hub1) == faHub.getHubData(hub2);
	}

	/**
	 * Determines whether two Hubs share the same Active Object source.
	 *
	 * @param hub1 first Hub
	 * @param hub2 second Hub
	 * @return true if they use the same HubDataActive instance
	 */
	public boolean isUsingSameSharedAO(Hub hub1, Hub hub2) {
		return isUsingSameSharedAO(hub1, hub2, false);
	}

	/**
	 * Determines AO-sharing equivalence, optionally including filtered Hub
	 * relationships.
	 *
	 * @param hub1 first Hub
	 * @param hub2 second Hub
	 * @param bIncludeFilteredHubs include HubFilter-based AO sharing
	 * @return true if the Hubs share an AO source
	 */
	public boolean isUsingSameSharedAO(Hub hub1, Hub hub2, boolean bIncludeFilteredHubs) {
		if (hub1 == null || hub2 == null) {
			return false;
		}
		if (faHub.getHubDataActive(hub1) == faHub.getHubDataActive(hub2)) {
			return true;
		}
		if (!bIncludeFilteredHubs) {
			return false;
		}

		Hub[] hs1 = getAllSharedHubs(hub1, false, null, bIncludeFilteredHubs, true);
		Hub[] hs2 = getAllSharedHubs(hub2, false, null, bIncludeFilteredHubs, true);

		for (Hub h1 : hs1) {
			for (Hub h2 : hs2) {
				if (h1 == h2) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Synchronizes HubData and HubDataActive instances across all Hubs in the
	 * shared group. Updates AO state when required and ensures detail-Hub link
	 * consistency.
	 *
	 * @param thisHub            the Hub undergoing changes
	 * @param bShareActiveObject whether AO state should be shared
	 * @param daOld              prior HubDataActive instance
	 * @param daNew              new HubDataActive instance
	 * @param bUpdateLink        whether link-based AO adjustments should occur
	 */
	public void syncSharedHubs(Hub thisHub, boolean bShareActiveObject, HubDataActive daOld, HubDataActive daNew,
			boolean bUpdateLink) {
		//qqqqqqqqqq method was protected
		// all shared hubs need to use same data
		Hub[] hubs = getAllSharedHubs(thisHub, true); // 201809123 added "true" so that other details using core hub would not be changed
		for (int i = 0; i < hubs.length; i++) {
			if (hubs[i] == thisHub) {
				continue;
			}
			faHub.setHubData(hubs[i], faHub.getHubData(thisHub)); // use same data 
			faHub.setHubDataMaster(hubs[i], faHub.getHubDataMaster(thisHub)); // use same data 
			if (bShareActiveObject) {
				// all hubs that are shared with the "dHub" need to have dataa shared
				if (faHub.getHubDataActive(hubs[i]) == daOld) {
					faHub.setHubDataActive(hubs[i], daNew);
				}
			} else {
				if (hubs[i] != thisHub && faHub.getHubDataActive(hubs[i]) != faHub.getHubDataActive(thisHub)) {
					if (faHub.getHubDataActive(hubs[i]).getActiveObject() != null && !hubs[i].contains(faHub.getHubDataActive(hubs[i]).getActiveObject())) {
						// make sure that it is not linked
						//   20120505 note: it could have a detail that is linked, so bUpdateLink was added so that the linked to prop wont be changed
						if (faHub.getHubDataUnique(hubs[i]).getLinkToHub() == null) {
							// 20120505 added new arg for bUpdateDetail
							srvcHub.getHubAOService().setActiveObject(hubs[i], null, false, bUpdateLink, false); // adjustMaster, bUpdateLink, bForce
							// was: hubs[i].setAO(null);
						}
					}
				}
			}
		}
	}

	/**
	 * Resets AO state in all shared Hubs after a remove-all operation and
	 * recursively propagates the change through the shared-Hub graph.
	 *
	 * @param thisHub the Hub where the remove-all occurred
	 */
	public void setSharedHubsAfterRemoveAll(Hub thisHub) {
		//qqqqqqqq method was protected
		faHub.getHubDataActive(thisHub).setActiveObject(null);
		srvcHub.getHubAOService().setActiveObject(thisHub, -1, false, false, false); // bUpdateLink, bForce, bCalledByShareHub

		WeakReference<Hub>[] refs = srvcHub.getHubShareService().getSharedWeakHubs(thisHub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub h2 = ref.get();
			if (h2 == null) {
				continue;
			}
			setSharedHubsAfterRemoveAll(h2);
		}
	}

	/**
	 * Updates Active Object values across all shared Hubs after a single
	 * object removal. Ensures AO validity based on size, linkHub status,
	 * null-on-remove rules, and AO-sharing behavior.
	 *
	 * @param thisHub     the Hub where removal occurred
	 * @param objRemoved  the object removed
	 * @param posRemoved  its position within the Hub
	 */
	public void setSharedHubsAfterRemove(Hub thisHub, Object objRemoved, int posRemoved) {
    	//qqqqqqqqqq method was protected
		if (faHub.getHubDataActive(thisHub).getActiveObject() == objRemoved) {
			/* this must be set to null. Otherwise, setActiveObject
			   could fail when it sends out event.
			*/
			faHub.getHubDataActive(thisHub).setActiveObject(null);

			final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
			if (thisHub.getSize() == 0 || thisHub.getLinkHub(true) != null || faHub.getHubDataUnique(thisHub).isNullOnRemove() || og.getSyncService().isRemoteThread()) {
				// 20120505 dont update a linked value that has already been set
				srvcHub.getHubAOService().setActiveObject(thisHub, -1, false, true, false); // bUpdateLink, bForce, bCalledByShareHub
				// was: srvcHub.getHubAOService().setActiveObject(thisHub, -1, true, true,false); // bUpdateLink,bForce,bCalledByShareHub
			} else {
				// 20101228
				if (thisHub.getSize() > posRemoved) {
					srvcHub.getHubAOService().setActiveObject(thisHub, posRemoved, false, false, false);
				} else {
					//was: if (faHub.getHubData(thisHub)a.activeObject == null && posRemoved > 0) {
					srvcHub.getHubAOService().setActiveObject(thisHub, posRemoved - 1, false, false, false);
				}
			}
		}

		// 20120715
		WeakReference<Hub>[] refs = srvcHub.getHubShareService().getSharedWeakHubs(thisHub);
		for (int i = 0; refs != null && i < refs.length; i++) {
			WeakReference<Hub> ref = refs[i];
			if (ref == null) {
				continue;
			}
			Hub h2 = ref.get();
			if (h2 == null) {
				continue;
			}
			setSharedHubsAfterRemove(h2, objRemoved, posRemoved);
		}
		/* was
		Hub[] hubs = getSharedHubs(thisHub);
		for (int i=0; i<hubs.length; i++) {
			setSharedHubsAfterRemove(hubs[i], objRemoved, posRemoved);
		}
		*/
	}

	/**
	 * Creates a new Hub instance that shares its underlying data with the
	 * specified {@code thisHub}. The new Hub is initialized using the same
	 * object type as {@code thisHub} and is then configured to participate in
	 * the shared-Hub relationship.
	 *
	 * @param thisHub      the Hub whose data and shared group the new Hub will join
	 * @param bShareActive true to share the active object state with {@code thisHub};
	 *                     false to maintain a separate active object
	 * @return the newly created shared Hub
	 */
	public Hub createSharedHub(Hub thisHub, boolean bShareActive) {
		Hub sharedHub = new Hub(thisHub.getObjectClass());
		srvcHub.getHubShareService().setSharedHub(sharedHub, thisHub, bShareActive);
		return sharedHub;
	}

	/**
	 * Assigns {@code thisHub} to share data with the specified
	 * {@code sharedMasterHub}. Sharing may optionally include the active
	 * object state. This method performs the basic shared-Hub setup and
	 * delegates extended behavior to the 4-argument overload.
	 *
	 * @param thisHub           the Hub to configure for sharing
	 * @param sharedMasterHub   the Hub whose data will be shared
	 * @param shareActiveObject true to share active-object state, false for independent AO
	 */
	public void setSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject) {
		setSharedHub(thisHub, sharedMasterHub, shareActiveObject, null);
	}

	/**
	 * Internal implementation for establishing a shared-Hub relationship.
	 * Performs full validation, detaches any previous shared configuration,
	 * aligns HubData/HubDataActive as required, updates shared children,
	 * recalculates active-object values, and fires list-reset events.
	 *
	 * @param thisHub           the Hub being configured
	 * @param sharedMasterHub   the Hub whose data is shared
	 * @param shareActiveObject true to share active-object state
	 * @param newLinkValue      optional pending link-value used during AO updates
	 */
	public void setSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject, Object newLinkValue) {
		//qqqqqqq method was protected
		_setSharedHub(thisHub, sharedMasterHub, shareActiveObject, newLinkValue);
		// 20181030 update temp listener cache
		srvcHub.getHubEventService().clearGetAllListenerCache(thisHub);

		// 20211125 if thisHub is linked & AO != null, and sharedHub is recursive, might need to adjust thisHub
		if (sharedMasterHub != null && thisHub.getAO() == null) {
			final Hub hx = thisHub.getLinkHub(true);
			if (hx != null) {
				if (sharedMasterHub.getOAObjectInfo().getRecursiveLinkInfo(OALinkInfo.ONE) != null) {
					// fire a fake changeActiveObject
					srvcHub.getHubEventService().fireAfterChangeActiveObjectEvent(hx, hx.getAO(), hx.getPos(), true);
				}
			}
		}
	}

	/**
	 * Core worker that performs the detailed process of attaching
	 * {@code thisHub} to the shared-Hub graph. Handles recursive-Hub protection,
	 * compatibility checks, data replacement, AO alignment, listener updates,
	 * and propagation of active-object recalculation.
	 *
	 * @param thisHub           Hub being attached
	 * @param sharedMasterHub   Hub that supplies shared data
	 * @param shareActiveObject whether to share AO state
	 * @param newLinkValue      temporary link value used during AO recalculation
	 */
	public void _setSharedHub(Hub thisHub, Hub sharedMasterHub, boolean shareActiveObject, Object newLinkValue) {
		if (thisHub == null) {
			return;
		}
		if (thisHub == sharedMasterHub) {
			sharedMasterHub = null;
			// added: 2004/05/13, removed 2004/05/14
			// if (getMasterHub() != null) throw new OAHubException(this,61);
		}

		// 20180328 check to see if thisHub has masterObject and no masterHub
		if (OAObject.getDebugMode() && faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
			if (faHub.getHubDataMaster(thisHub).getMasterHub() == null) {
				OALinkInfo li = srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster(thisHub);
				if (li != null && !li.getCalculated()) {
					li = srvcHub.getHubDetailService().getLinkInfoFromMasterHubToDetail(thisHub);
					if (li != null && li.getType() == OALinkInfo.ONE) {
						LOG.log(Level.WARNING,
								"thisHub should not be used for sharing, thisHub=" + thisHub + ", sharedMasterHub=" + sharedMasterHub,
								new Exception("illegal hub share"));
						return;
					}
				}
			}
		}

		srvcHub.getHubDataService().incChangeCount(thisHub);
		final Hub hubOrigSharedHub = faHub.getHubDataUnique(thisHub).getSharedHub();
		if (hubOrigSharedHub == sharedMasterHub) {
			if (sharedMasterHub == null) {
				return;
			}
			if (shareActiveObject == (faHub.getHubDataActive(thisHub) == faHub.getHubDataActive(sharedMasterHub))) {

				// 20110809 this was removed, since there could be a linkToHub, which
				//     would mean that the setting thisHub.setPos(-1) should instead
				//     set AO to the linkToHub.ao.propertyValue
				/*was
				if (!shareActiveObject) thisHub.setPos(-1);  // in case masterHub was re-shared after a new select
				return; // same as previous call
				*/

				// 20130331 since the SharedHub is the same, do more checking to see if thisHub has changed or not
				if (!shareActiveObject || (faHub.getHubDataActive(thisHub).getActiveObject() == faHub.getHubDataActive(sharedMasterHub).getActiveObject())) {
					if (faHub.getHubDataUnique(thisHub).getLinkToHub() == null) {
						if (!shareActiveObject) {
							// 20180305
							Object objx = thisHub.getAO();
							if (objx != null && !thisHub.contains(objx)) {
								thisHub.setPos(-1); // in case masterHub was re-shared after a new select
							}
							// was: thisHub.setPos(-1);  // in case masterHub was re-shared after a new select
						}
						return;
					}

					// see if this AO is already set correctly with the linkHub
					try {
						Object obj = faHub.getHubDataUnique(thisHub).getLinkToHub().getActiveObject();
						if (obj != null) {
							obj = faHub.getHubDataUnique(thisHub).getLinkToGetMethod().invoke(obj, (Object[]) null);
						}

						// 20110110 the link value is in the process of being changed - see srvcHub.getHubDataService().getPos(...)
						if (newLinkValue != null && newLinkValue != obj) {
							return;
						}

						if (faHub.getHubDataUnique(thisHub).isLinkPos()) {
							int x = -1;
							if (obj != null && obj instanceof Number) {
								x = ((Number) obj).intValue();
							}
							if (thisHub.getPos() == x) {
								return;
							}
						} else {
							if (faHub.getHubDataActive(thisHub).getActiveObject() == obj) {
								return;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}
			}
		}

		if (sharedMasterHub != null && faHub.getHubDataUnique(sharedMasterHub).getSharedHub() == thisHub) {
			throw new RuntimeException("the masterHub is already shared with thisHub - cant set thisHub.sharedHub with masterHub");
		}

		// 20110120
		if (sharedMasterHub == thisHub) {
			return;
			//was: if (sharedMasterHub == thisHub) sharedMasterHub = null;
		}

		// make sure both hubs are compatible
		if (sharedMasterHub != null) {
			if (thisHub.getObjectClass() == null) {
				srvcHub.setObjectClass(thisHub, sharedMasterHub.getObjectClass());
			} else if (sharedMasterHub.getObjectClass() == null) {
				srvcHub.setObjectClass(sharedMasterHub, thisHub.getObjectClass());
			}
			Class c = thisHub.getObjectClass();
			if (c != null && !c.equals(sharedMasterHub.getObjectClass())) {
				if (!c.isAssignableFrom(sharedMasterHub.getObjectClass())) {
					throw new RuntimeException("objectClasses do not match");
				}
			}
		}

		// save orig dataa so that hubs that are shared with this hub can be updated
		HubDataActive originalDataa = faHub.getHubDataActive(thisHub);

		// first unset any prev set sharedHub
		Hub h = faHub.getHubDataUnique(thisHub).getSharedHub();
		if (h != null) {
			removeSharedHub(h, thisHub);
			if (faHub.getHubDataActive(h) == faHub.getHubDataActive(thisHub)) {
				faHub.setHubDataActive(thisHub, new HubDataActive());
			}
		} else {
			// 20171015 need to remove objects from it
			for (Object obj : thisHub) {
				srvcObject.getOAObjectHubService().removeHub((OAObject) obj, thisHub, false);
			}
		}

		Object activeObject = null;
		boolean shareActiveObject2 = true;

		if (sharedMasterHub == null) {
			faHub.setHubData(thisHub, new HubData(faHub.getHubData(thisHub).getObjClass()));
			faHub.setHubDataMaster(thisHub, new HubDataMaster());
		} else {
			activeObject = sharedMasterHub.getAO();

			// recursive hubs
			// if this hub is a masterHub of the sharedMasterHub
			// then use the "original" shared hub of the sharedMasterHub and dont share AO
			h = sharedMasterHub.getMasterHub();

			ArrayList<Hub> al = null;
			for (int i = 0; h != null; i++) {
				if (h == thisHub) {
					h = sharedMasterHub;
					for (;;) {
						if (faHub.getHubDataUnique(h).getSharedHub() == null) {
							break;
						}
						h = faHub.getHubDataUnique(h).getSharedHub();
					}
					sharedMasterHub = h;
					shareActiveObject2 = false;
					break;
				}
				// 20120717 added extra check against endless loop, since a recursive hub being shared by multiple parents can casue a loop
				if (i > 5) {
					if (al == null) {
						al = new ArrayList<Hub>();
					} else if (al.contains(h)) {
						break;
					}
					al.add(h);
				}
				h = h.getMasterHub();
			}

			// 2006/05/31 moved from below
			addSharedHub(sharedMasterHub, thisHub); // adds to datau.vecSharedHub
			faHub.setHubData(thisHub, faHub.getHubData(sharedMasterHub));
			faHub.setHubDataMaster(thisHub, faHub.getHubDataMaster(sharedMasterHub)); // 20171218
			// dont share "datau"
			// dont share "dataa" unless shareActiveObject is true

			if (shareActiveObject && shareActiveObject2) {
				/**
				 * 2004/03/18 HubDataActive hold = this.dataa; this.dataa = sharedMasterHub.dataa; for (int i=0; i<hubShared.length; i++) {
				 * if (hubShared[i].dataa == hold) hubShared[i].dataa = this.dataa; }
				 */
			} else {
				if (thisHub.getLinkHub(true) != null) { // 2003/04/25
					shareActiveObject = false; // cant share since this hub is linked to a master hub
				}
			}
			// 2006/05/31 moved to above
			// sharedMasterHub.datau.addSharedHub(this); // adds to datau.vecSharedHub
		}

		faHub.getHubDataUnique(thisHub).setSharedHub(sharedMasterHub); // the master Hub that this hub is shared with

		Hub[] hubShared = getAllSharedHubs(thisHub, true, null); // get shared hubs under this Hub
		if (sharedMasterHub != null && shareActiveObject && shareActiveObject2) {
			faHub.setHubDataActive(thisHub, faHub.getHubDataActive(sharedMasterHub));
		}
		for (int i = 0; i < hubShared.length; i++) {
			faHub.setHubData(hubShared[i], faHub.getHubData(thisHub)); // share same data
			faHub.setHubDataMaster(hubShared[i],  faHub.getHubDataMaster(thisHub)); // 20171218
			if (faHub.getHubDataActive(hubShared[i]) == originalDataa) {
				faHub.setHubDataActive(hubShared[i], faHub.getHubDataActive(thisHub));
			}
		}

		// set active object in each shared hub, which will update detail hubs
		for (int i = 0; i < hubShared.length; i++) {
			h = hubShared[i];
			if (faHub.getHubDataUnique(h).getLinkToHub() == null) {
				// if there is not a linkHub, then go to first object
				int pos;
				if (faHub.getHubDataUnique(h).getSharedHub() != null && faHub.getHubDataActive(h) == faHub.getHubDataActive(faHub.getHubDataUnique(h).getSharedHub())) {
					// shared hubs
					pos = faHub.getHubDataUnique(h).getSharedHub().getPos();
				} else {
					// 08/18/2001 - always set to null
					// pos = size() > 0 ? 0 :-1;
					pos = faHub.getHubDataUnique(h).getDefaultPos(); // default is -1
				}
				srvcHub.getHubAOService().setActiveObject(h, pos, false, true, true); // updateLink, bForce, bCalledByShareHub
			} else {
				// if linkHub & !bUpdateLink, then retrieve value from linked property
				// and make that the activeObject in this Hub
				try {
					Object obj = faHub.getHubDataUnique(h).getLinkToHub().getActiveObject();
					if (obj != null) {
						obj = faHub.getHubDataUnique(h).getLinkToGetMethod().invoke(obj, (Object[]) null);
					}

					// 20110110 the link value is in the process of being changed - see srvcHub.getHubDataService().getPos(...)
					if (newLinkValue != null && newLinkValue != obj) {
						continue;
					}

					if (faHub.getHubDataUnique(h).isLinkPos()) {
						int x = -1;
						if (obj != null && obj instanceof Number) {
							x = ((Number) obj).intValue();
						}
						if (h.getPos() != x) {
							srvcHub.getHubAOService().setActiveObject(h, h.elementAt(x), x, false, false, true);//bUpdateLink,bForce,bCalledByShareHub
						}
					} else {
						int pos = h.getPos(obj);
						if (obj != null && pos < 0) {
							obj = null;
						}
						srvcHub.getHubAOService().setActiveObject(h, obj, pos, false, false, true);//bUpdateLink,bForce,bCalledByShareHub
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}

		// 20120229 might need to temp set AO=newLinkValue
		boolean b = (newLinkValue != null && newLinkValue != faHub.getHubDataActive(thisHub).getActiveObject());
		Object hold = null;
		if (b) {
			hold = faHub.getHubDataActive(thisHub).getActiveObject();
			faHub.getHubDataActive(thisHub).setActiveObject(newLinkValue);
		}

		// 20130317 added this to stop an infinite loop
		if (faHub.getHubDataUnique(thisHub).getSharedHub() != hubOrigSharedHub) {
			srvcHub.getHubEventService().fireOnNewListEvent(thisHub, false); // only for this shared hub
		}
		// was: srvcHub.getHubEventService().fireOnNewListEvent(thisHub, false); // only for this shared hub

		// 20101113 not sure why this is here, since it would resort the sharedMasterHub
		// srvcHub.getHubSortService().sort(thisHub);

		// 20120614 the change from 0229 looks wrong
		if (b) {
			faHub.getHubDataActive(thisHub).setActiveObject(hold);
			/*was:
			// 20120229
			if (b && hold == faHub.getHubData(thisHub)a.activeObject) {
			    faHub.getHubData(thisHub)a.activeObject = hold;
			}
			*/
		}
	}

	/**
	 * Returns an array of all of the Hubs that are shared with this Hub.
	 */
	/*
	public Hub[] getSharedHubs_OLD(Hub thisHub) {
	    if (faHub.getHubDataUnique(thisHub).vecSharedHub == null) return new Hub[0];
	    synchronized (faHub.getHubDataUnique(thisHub).vecSharedHub) {
		    int x = faHub.getHubDataUnique(thisHub).vecSharedHub.size();
		    Hub[] hubs = new Hub[x];
		    faHub.getHubDataUnique(thisHub).vecSharedHub.copyInto(hubs);
		    return hubs;
	    }
	}
	*/
	/**
	 * Add Hub that is being shared with this Hub. This will use a WeakReference, so that the shared Hub will be removed when it is garbage
	 * collected.
	 */

	/*
	public void addSharedHub_OLD(Hub thisHub, Hub hub) {
	    if (faHub.getHubDataUnique(thisHub).vecSharedHub == null) {
		    synchronized (faHub.getHubData(thisHub)u) {
		    	if (faHub.getHubDataUnique(thisHub).vecSharedHub == null) faHub.getHubDataUnique(thisHub).vecSharedHub = new Vector(3,5);
		    }
	    }
	    faHub.getHubDataUnique(thisHub).vecSharedHub.addElement(hub);
	}
	*/
	/**
	 * Remove shared Hub from list of shared Hubs.
	 */
	/*
	public void removeSharedHub_OLD(Hub thisHub, Hub hub) {
	    if (faHub.getHubDataUnique(thisHub).vecSharedHub == null) return;
	    synchronized (faHub.getHubDataUnique(thisHub)uvecSharedHub) {
	    	faHub.getHubDataUnique(thisHub).vecSharedHub.removeElement(hub);
	    }
	}
	*/

	/**
	 * Adds {@code hub} to the list of Hubs that are shared with {@code thisHub}.
	 * This operation records the shared relationship using a weak reference and
	 * then clears the cached listener map so that subsequent listener lookup
	 * reflects the updated shared-Hub configuration.
	 *
	 * @param thisHub the Hub whose shared-Hub list is being updated
	 * @param hub     the Hub to add as a shared participant
	 */
	public void addSharedHub(Hub thisHub, Hub hub) {
		_addSharedHub(thisHub, hub);
		// 20181030 update temp listener cache
		srvcHub.getHubEventService().clearGetAllListenerCache(thisHub);
	}

	/**
	 * Internal worker that inserts {@code hub} into {@code thisHub}'s
	 * weak-shared-Hubs array. Expands the underlying array when full and
	 * reuses empty or garbage-collected slots when available.
	 *
	 * @param thisHub the Hub whose weak-shared-Hub list is being modified
	 * @param hub     the Hub to add as a shared reference
	 */
	public void _addSharedHub(Hub thisHub, Hub hub) {
		if (thisHub == null || hub == null) {
			return;
		}

		int pos;
		synchronized (faHub.getHubDataUnique(thisHub)) {
			if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs() == null) {
				faHub.getHubDataUnique(thisHub).setWeakSharedHubs(new WeakReference[1]);
				pos = 0;
			} else {
				// check for empty slot at the end
				int currentSize = faHub.getHubDataUnique(thisHub).getWeakSharedHubs().length;
				for (pos = currentSize - 1; pos >= 0; pos--) {
					if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[pos] == null) {
						continue;
					}
					if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[pos].get() == null) {
						faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[pos] = null;
						continue;
					}
					// found last used slot
					if (pos < currentSize - 1) {
						pos++; // first empty slot
						break;
					}

					// need to expand
					int newSize = currentSize + 1 + (currentSize / 3);
					newSize = Math.min(newSize, currentSize + 50);
					WeakReference<Hub>[] refs = new WeakReference[newSize];

					System.arraycopy(faHub.getHubDataUnique(thisHub).getWeakSharedHubs(), 0, refs, 0, currentSize);
					faHub.getHubDataUnique(thisHub).setWeakSharedHubs(refs);
					pos = currentSize;
					break;
				}
				if (pos < 0) {
					pos = 0;
				}
			}
			faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[pos] = new WeakReference(hub);
		}
		if (pos > 99) {
			if (pos + 1 % 25 == 0) {
				LOG.warning("Hub has " + (pos + 1) + " sharedHubs, Hub=" + thisHub);
			}
		}
	}

	/**
	 * Removes {@code hub} from {@code sharedHub}'s weak-shared-Hub list.
	 * Delegates structural cleanup to the internal worker and clears both
	 * Hubs’ cached listener data so the system will recalculate listeners
	 * the next time they are requested.
	 *
	 * @param sharedHub the Hub whose shared list is being updated
	 * @param hub       the Hub to remove
	 */
	public void removeSharedHub(Hub sharedHub, Hub hub) {
		_removeSharedHub(sharedHub, hub);
		//qqqqqqqqq method was protected
		// 20181030 update temp listener cache
		srvcHub.getHubEventService().clearGetAllListenerCache(hub); // will clear both hubs
	}

	/**
	 * Internal worker that removes {@code hub} from {@code sharedHub}'s
	 * weak-shared-Hub array. Handles compaction of the array, removal of
	 * null or garbage-collected references, and resizing when appropriate
	 * to reduce unused capacity.
	 *
	 * @param sharedHub the Hub whose stored references are modified
	 * @param hub       the Hub being removed
	 */
	public void _removeSharedHub(Hub sharedHub, Hub hub) {
		if (faHub.getHubDataUnique(sharedHub).getWeakSharedHubs() == null) {
			return;
		}
		boolean bFound = false;
		synchronized (faHub.getHubDataUnique(sharedHub)) {
			if (faHub.getHubDataUnique(sharedHub).getWeakSharedHubs() == null) {
				return;
			}
			int currentSize = faHub.getHubDataUnique(sharedHub).getWeakSharedHubs().length;
			int lastEndPos = currentSize - 1;
			for (int pos = 0; !bFound && pos < currentSize; pos++) {
				if (faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[pos] == null) {
					break; // the rest will be nulls
				}

				Hub hx = faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[pos].get();
				if (hx != null && hx != hub) {
					continue;
				}
				bFound = (hx == hub);
				faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[pos] = null;

				// compress:  get last one, move it back to this slot
				for (; lastEndPos > pos; lastEndPos--) {
					if (faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[lastEndPos] == null) {
						continue;
					}
					if (faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[lastEndPos].get() == null) {
						faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[lastEndPos] = null;
						continue;
					}
					faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[pos] = faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[lastEndPos];
					faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[lastEndPos] = null;
					break;
				}
				if (currentSize > 20 && ((currentSize - lastEndPos) > currentSize / 3)) {
					// resize array
					int newSize = lastEndPos + (lastEndPos / 10) + 1;
					newSize = Math.min(lastEndPos + 20, newSize);
					WeakReference<Hub>[] refs = new WeakReference[newSize];

					System.arraycopy(faHub.getHubDataUnique(sharedHub).getWeakSharedHubs(), 0, refs, 0, lastEndPos);
					faHub.getHubDataUnique(sharedHub).setWeakSharedHubs(refs);
					currentSize = newSize;
				}
			}
			if (faHub.getHubDataUnique(sharedHub).getWeakSharedHubs()[0] == null) {
				faHub.getHubDataUnique(sharedHub).setWeakSharedHubs(null);
			}
		}
	}

	private final static Hub[] EmptyHubs = new Hub[0];

	/**
	 * Returns an array of Hubs directly recorded as shared with {@code thisHub}.
	 * Uses the weak-shared-Hub array and trims trailing null or empty entries.
	 * This method is deprecated in favor of {@link #getAllSharedHubs}.
	 *
	 * @param thisHub the Hub to inspect
	 * @return array of directly shared Hubs (may contain null entries)
	 * @deprecated use {@link #getAllSharedHubs} instead
	 */
	public Hub[] getSharedHubs(Hub thisHub) {
		if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs() == null) {
			return EmptyHubs;
		}
		synchronized (faHub.getHubDataUnique(thisHub)) {
			if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs() == null) {
				return EmptyHubs;
			}

			int x = faHub.getHubDataUnique(thisHub).getWeakSharedHubs().length;
			for (int j = x - 1; j >= 0; j--) {
				if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[j] == null) {
					continue;
				}
				if (faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[j].get() == null) {
					faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[j] = null;
					continue;
				}
				Hub[] hubs = new Hub[j + 1];
				for (int i = 0; i < hubs.length; i++) {
					hubs[i] = faHub.getHubDataUnique(thisHub).getWeakSharedHubs()[i].get();
				}
				// note: could be nulls in array
				return hubs;
			}
		}
		return EmptyHubs;
	}

	/**
	 * Returns the internal weak-reference array that stores Hubs sharing
	 * data with {@code thisHub}. May be null if no shared-Hub relationships
	 * have been established.
	 *
	 * @param thisHub the Hub whose shared structure is requested
	 * @return weak-reference array, or null if none exist
	 */
	public WeakReference<Hub>[] getSharedWeakHubs(Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		return faHub.getHubDataUnique(thisHub).getWeakSharedHubs();
	}

	/**
	 * Counts valid Hub references in {@code thisHub}'s weak-shared-Hub array.
	 * Entries that are null or whose referent has been garbage-collected
	 * are not included.
	 *
	 * @param thisHub the Hub to inspect
	 * @return number of active shared-Hub references
	 */
	public int getSharedWeakHubSize(Hub thisHub) {
		if (thisHub == null) {
			return 0;
		}
		WeakReference<Hub>[] refs = faHub.getHubDataUnique(thisHub).getWeakSharedHubs();
		if (refs == null) {
			return 0;
		}
		int cnt = 0;
		for (WeakReference<Hub> ref : refs) {
			if (ref != null && ref.get() != null) {
				cnt++;
			}
		}
		return cnt;
	}


}

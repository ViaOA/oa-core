/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.hub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Delegate used for Hub selecting and lazy loading (fetching) from datasource.
 *
 * @author vvia
 */
public class HubSelectDelegate {
	private static Logger LOG = Logger.getLogger(HubSelectDelegate.class.getName());

	/** Internal method to retrieve objects from last select() */
	protected static int fetchMore(Hub thisHub) {
		int x = fetchMore(thisHub, HubSelectDelegate.getSelect(thisHub));
		return x;
	}

	protected static int fetchMore(Hub thisHub, OASelect sel) {
		if (sel == null) {
			return 0;
		}
		int x = sel.getFetchAmount();
		x = fetchMore(thisHub, sel, x);
		return x;
	}

	/** Internal method to retrieve objects from last select() */
	protected static int fetchMore(Hub thisHub, int famt) {
		int x = fetchMore(thisHub, HubSelectDelegate.getSelect(thisHub), famt);
		return x;
	}

	private static int cntWarning;

	private static ConcurrentHashMap<Hub, Integer> hmHubFetch = new ConcurrentHashMap<Hub, Integer>(11, .85f);

	protected static int fetchMore(Hub thisHub, OASelect sel, int famt) {
        if (sel == null) {
            return 0;
        }
        if (sel.hasNextCompleted()) return 0;
		try {
			// get fetch lock
			for (;;) {
				synchronized (hmHubFetch) {
					if (hmHubFetch.get(thisHub) == null) {
						hmHubFetch.put(thisHub, 0);
						break;
					}
					try {
						hmHubFetch.put(thisHub, 1);
						Thread.yield();
						//was: hmHubFetch.wait(1);
					} catch (Exception e) {
					}
				}
			}
			return _fetchMore(thisHub, sel, famt);
		} finally {
			synchronized (hmHubFetch) {
				int x = hmHubFetch.remove(thisHub);
				if (x > 0) {
					hmHubFetch.notifyAll();
				}
			}
		}
	}

	protected static int _fetchMore(Hub thisHub, OASelect sel, int famt) {
		if (sel == null) {
			return 0;
		}
        if (sel.hasNextCompleted()) return 0;
		int fa = sel.getFetchAmount(); // default amount to load

		HubData hubData = thisHub.data;

		boolean holdDataChanged = hubData.changed;

		if (famt > 0) {
			fa = famt;
		}
		int cnt = 0;

		try {
			int capacity = hubData.vector.capacity(); // number of available 'slots'
			int size = hubData.vector.size(); // number of elements

			for (; cnt < fa || fa == 0;) {
				Object obj;
				if (!HubSelectDelegate.isMoreData(sel)) {
					boolean bRemoveSelectFromHub;
					if (thisHub.getMasterObject() != null) {
						OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
						if (li.getType() == OALinkInfo.ONE && li.getPrivateMethod()) {
							bRemoveSelectFromHub = false;
						} else {
							bRemoveSelectFromHub = true;
						}
					} else {
						bRemoveSelectFromHub = false; // dont remove, so that it can be refreshed
					}

					cancelSelect(thisHub, bRemoveSelectFromHub);
					// was: thisHub.cancelSelect();
					sel.cancel();
					break;
				}

				obj = sel.next();
				if (obj != null) {
					if (size == (capacity - 1)) { // resize Vector according to select
						/*
						if (thisHub.data.loadingAllData) {
							capacity = HubSelectDelegate.getCount(thisHub);
							if (capacity <= 0) capacity = size+1;
						}
						*/
						capacity += (capacity > 250) ? 75 : capacity; // this will override the default behavior of how the Vector grows itself (which is to double in size)
						//LOG.config("resizing, from:"+size+", to:"+capacity+", hub:"+thisHub);
						HubDataDelegate.ensureCapacity(thisHub, capacity);
					}
					try {
						OAThreadLocalDelegate.setLoading(true);
						HubAddRemoveDelegate.add(thisHub, obj);
					} finally {
						OAThreadLocalDelegate.setLoading(false);
					}
					size++;
					cnt++;
				}
			}
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "Hub=" + thisHub + ", will cancel select", ex);
			cancelSelect(thisHub, false);
			sel.cancel();
			throw new RuntimeException(ex);
		} finally {
			hubData.changed = holdDataChanged;
		}
		return cnt;
	}

	/**
	 * Find out if more objects are available from last select from OADataSource.
	 */
	public static boolean isMoreData(Hub thisHub) {
		OASelect sel = getSelect(thisHub);
		if (sel == null) {
			return false;
		}
        if (sel.hasNextCompleted()) return false;
		if (!sel.hasBeenStarted()) {
			sel.select();
		}
		return sel.hasMore();
	}

	public static boolean isMoreData(OASelect sel) {
		if (sel == null) {
			return false;
		}
        if (sel.hasNextCompleted()) return false;
		if (!sel.hasBeenStarted()) {
			sel.select();
		}
		return sel.hasMore();
	}

	/**
	 * This will automatically read all records from current select(). By default, only 45 objects are read at a time from datasource.
	 */
	public static void loadAllData(Hub thisHub) {
		loadAllData(thisHub, thisHub.getSelect());
	}

	public static void loadAllData(Hub thisHub, OASelect select) {
		if (thisHub == null) {
			return;
		}

		if (select == null || select.hasNextCompleted()) {
		    return;
		}
		
        long ms = 0;
		int i = 0;
		for (;; i++) {
			boolean bCanRun = false;
			synchronized (thisHub.data) {
				if (!thisHub.data.isLoadingAllData()) {
					if (select == null) {
						break;
					}
					thisHub.data.setLoadingAllData(true);
					bCanRun = true;
				}
			}

			if (bCanRun) {
				try {
					while (isMoreData(select)) {
						fetchMore(thisHub, select);
					}
				} finally {
					synchronized (thisHub.data) {
						thisHub.data.setLoadingAllData(false);
					}
				}
				break;
			}

			// else wait and try again
			if (select == null) {
				if (i >= 500) {
				    if (System.currentTimeMillis() - ms > 500) {
				        break;
				    }
				}
				else if (i == 25) {
				    if (ms == 0) ms = System.currentTimeMillis();
				}
			}
			try {
			    Thread.yield();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Returns OASelect used for querying datasource.
	 */
	protected static OASelect getSelect(Hub thisHub) {
		return getSelect(thisHub, false);
	}

	protected static OASelect getSelect(Hub thisHub, boolean bCreateIfNull) {
		if (thisHub == null) {
			return null;
		}
		OASelect sel = thisHub.data.getSelect();
		if (sel != null || !bCreateIfNull) {
			return sel;
		}

		sel = new OASelect(thisHub.getObjectClass());
		thisHub.data.setSelect(sel);
		return sel;
	}

	/**
	 * Used to populate Hub with objects returned from a OADataSource select. By default, all objects will first be removed from the Hub,
	 * OASelect.select() will be called, and the first 45 objects will be added to Hub and active object will be set to null. As the Hub is
	 * accessed for more objects, more will be returned until the query is exhausted of objects.
	 */
	public static void select(final Hub thisHub, OASelect select) { // This is the main select method for Hub that all of the other select methods call.
		cancelSelect(thisHub, true);
		if (select == null) {
			return;
		}

		if (thisHub.datau.getSharedHub() != null) {
			select(thisHub.datau.getSharedHub(), select);
			return;
		}
		if (thisHub.data.objClass == null) {
			thisHub.data.objClass = select.getSelectClass();
			if (thisHub.data.objClass == null) {
				return;
			}
		}

		if (thisHub.datam.getMasterObject() != null && thisHub.datam.liDetailToMaster != null) {
			if (select != thisHub.data.getSelect() && thisHub.data.getSelect() != null) {
				throw new RuntimeException("select cant be changed for detail hub");
			}

		}
		if (select.getWhereObject() != null) {
			if (thisHub.datam.liDetailToMaster != null && select.getWhereObject() == thisHub.datam.getMasterObject()) {
				select.setPropertyFromWhereObject(thisHub.datam.liDetailToMaster.getReverseName());
			}
		}

		select.setSelectClass(thisHub.getObjectClass());
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());

		// 20200302
		Hub hx = thisHub.data.getSelectWhereHub();
		if (hx != null) {
			String s = thisHub.data.getSelectWhereHubPropertyPath();
			if (OAString.isNotEmpty(s)) {
				select.setWhereHub(hx, s);
			}
		}

		HubEventDelegate.fireBeforeSelectEvent(thisHub);

		boolean bRunSelect;
		bRunSelect = oi.getUseDataSource() || select.getDataSource() != null;

		// 20160110 selects now have hubFinders, etc to do selects
		bRunSelect = (bRunSelect && (select.getDataSource() != null || select.getFinder() != null));
		//was: bRunSelect = (bRunSelect && select.getDataSource() != null);

		HubDataDelegate.incChangeCount(thisHub);

		if (select.getAppend()) {
			thisHub.data.setSelect(select);
		} else {
			thisHub.setAO(null); // 20100507
			if (thisHub.isOAObject()) {
				int z = HubDataDelegate.getCurrentSize(thisHub);
				for (int i = 0; i < z; i++) {
					OAObject oa = (OAObject) HubDataDelegate.getObjectAt(thisHub, i);
					OAObjectHubDelegate.removeHub(oa, thisHub, false);
				}
			}
			HubDataDelegate.clearAllAndReset(thisHub);
			thisHub.data.setSelect(select);

			if (select.getRewind()) {

				// 20120716
				OAFilter<Hub> filter = new OAFilter<Hub>() {
					@Override
					public boolean isUsed(Hub h) {
						if (h != thisHub && h.dataa != thisHub.dataa) {
							if (h.datau.getLinkToHub() == null) {
								return true;
							}
						}
						return false;
					}
				};
				Hub[] hubs = HubShareDelegate.getAllSharedHubs(thisHub, filter);

				for (int i = 0; i < hubs.length; i++) {
					if (hubs[i] != thisHub && hubs[i].dataa != thisHub.dataa) {
						if (hubs[i].datau.getLinkToHub() == null) {
							hubs[i].setAO(null);
						}
					}
				}
			}
		}

		if (bRunSelect) {
			select.select(); // run query
			fetchMore(thisHub); // load up fetch amount objects into hub
		}

		if (select.isSelectAll()) {
			thisHub.data.setSelectAllHub(true);
			OAObjectCacheDelegate.setSelectAllHub(thisHub);
		} else {
			thisHub.data.setSelectAllHub(false);
			OAObjectCacheDelegate.removeSelectAllHub(thisHub);
		}

		if (!select.getAppend()) {
			HubEventDelegate.fireOnNewListEvent(thisHub, true);
		}
	}

	/**
	 * Cancels current OASelect, calling select.cancel() This will also set SelectLater to false and RequiredWhere to null.
	 */
	protected static void cancelSelect(Hub thisHub, boolean bRemoveSelect) {
		OASelect sel = thisHub.data.getSelect();
		boolean bHasMoreData;
		if (sel != null) {
			boolean b = sel.hasBeenStarted();
			bHasMoreData = (b && (sel.isSelectingNow() || sel.hasMore()));
			if (b) {
				sel.cancel();
			}
			if (bRemoveSelect) {
				thisHub.data.setSelect(null);
			}
			HubDataDelegate.resizeToFit(thisHub);
		} else {
			bHasMoreData = false;
		}

		if (thisHub.data.isSelectAllHub() && bHasMoreData) {
			thisHub.data.setSelectAllHub(false);
			OAObjectCacheDelegate.removeSelectAllHub(thisHub);
		}
	}

	public static int getCount(Hub thisHub) {
		if (thisHub == null) {
			return -1;
		}
		OASelect sel = getSelect(thisHub);
		if (sel == null) {
			return -1;
		}
		return sel.getCount();
	}

	public static boolean isCounted(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}
		OASelect sel = getSelect(thisHub);
		if (sel == null) {
			return true;
		}
		return sel.isCounted();
	}

	/**
	 * WHERE clause to use for select.
	 *
	 * @see #setSelectOrder
	 * @see OASelect
	 */
	public static void setSelectWhere(Hub thisHub, String s) {
		OASelect sel = getSelect(thisHub);
		if (sel == null) {
			sel = new OASelect(thisHub.getObjectClass());
			thisHub.data.setSelect(sel);
		}
		sel.setWhere(s);
	}

	public static String getSelectWhere(Hub thisHub) {
		OASelect sel = getSelect(thisHub);
		if (sel == null) {
			return null;
		}
		return sel.getWhere();
	}

	/**
	 * Sort Order clause to use for select.
	 *
	 * @see #getSelectOrder
	 * @see OASelect
	 */
	public static void setSelectOrder(Hub thisHub, String s) {
		thisHub.data.setSortProperty(s);

		OASelect sel = getSelect(thisHub);
		if (!OAString.isEmpty(s) && sel == null) {
			sel = new OASelect(thisHub.getObjectClass());
			thisHub.data.setSelect(sel);
		}
		sel.setOrder(s);
	}

	/**
	 * Sort Order clause to use for select.
	 *
	 * @see #setSelectOrder
	 * @see OASelect
	 */
	public static String getSelectOrder(Hub thisHub) {
		OASelect sel = getSelect(thisHub);
		if (sel == null) {
			return null;
		}
		return sel.getOrder();
	}

	public static void select(Hub thisHub, boolean bAppendFlag) {
		if (thisHub == null) {
			return;
		}
		OASelect sel = new OASelect(thisHub.getObjectClass());
		sel.setAppend(bAppendFlag);
		select(thisHub, sel);
	}

	// Main Select here:
	protected static void select(Hub thisHub, OAObject whereObject, String whereClause,
			Object[] whereParams, String orderByClause, boolean bAppendFlag) {
		OASelect sel = new OASelect(thisHub.getObjectClass());
		sel.setWhereObject(whereObject);
		sel.setParams(whereParams);
		sel.setWhere(whereClause);
		sel.setAppend(bAppendFlag);
		sel.setOrder(orderByClause);
		select(thisHub, sel);
	}

	protected static void select(Hub thisHub, OAObject whereObject, String whereClause,
			Object[] whereParams, String orderByClause, boolean bAppendFlag, OAFilter filter) {
		OASelect sel = new OASelect(thisHub.getObjectClass());
		sel.setWhereObject(whereObject);
		sel.setParams(whereParams);
		sel.setWhere(whereClause);
		sel.setAppend(bAppendFlag);
		sel.setOrder(orderByClause);
		sel.setFilter(filter);
		select(thisHub, sel);
	}

	public static void selectPassthru(Hub thisHub, String whereClause, String orderClause) {
		OASelect sel = new OASelect(thisHub.getObjectClass());
		sel.setPassthru(true);
		sel.setWhere(whereClause);
		sel.setOrder(orderClause);
		select(thisHub, sel);
	}

	public static void selectPassthru(Hub thisHub, String whereClause, String orderClause, boolean bAppend) {
		OASelect sel = new OASelect(thisHub.getObjectClass());
		sel.setPassthru(true);
		sel.setAppend(bAppend);
		sel.setWhere(whereClause);
		sel.setOrder(orderClause);
		select(thisHub, sel);
	}

	public static Hub getSelectWhereHub(Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		return thisHub.data.getSelectWhereHub();
	}

	public static void setSelectWhereHub(Hub thisHub, Hub hub) {
		if (thisHub == null) {
			return;
		}
		thisHub.data.setSelectWhereHub(hub);
	}

	public static String getSelectWhereHubPropertyPath(Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		return thisHub.data.getSelectWhereHubPropertyPath();
	}

	public static void setSelectWhereHubPropertyPath(Hub thisHub, String pp) {
		if (thisHub == null) {
			return;
		}
		thisHub.data.setSelectWhereHubPropertyPath(pp);
	}

	/**
	 * Check to see if thisHub can use the whereHub and "converted" PP as another Hub.
	 * <p>
	 * Example setup:<br>
	 * hubWhere&lt;Company&gt;<br>
	 * PP: [Company.]customers.orders.orderItems<br>
	 * reversePP: orderItem.order.customer.company<br>
	 * <p>
	 * hubFrom = Hub&lt;OrderItem&gt;, hubWhere+PP=hub&lt;Company&gt;+"customers.orders.orderItems"<br>
	 * a select for hubFrom&lt;OrderItem&gt; would add a whereClause "AND order.customer.company=?", hubWhere&lt;Company&gt;.AO<br>
	 * Which would limit it to only selecting orderitems that are for a company.
	 * <p>
	 * Now ... if we have a Hub&lt;Order&gt; that is used to select an Order for an OrderItem, then it would need to only select under the
	 * same Company.<br>
	 * thisHub&lt;Order&gt;<br>
	 * calling adoptWhereHub(thisHub, "order", hubFrom&lt;OrderItem&gt;, where "order" is the property from hubFrom&lt;OrderItem&gt; to
	 * thisHub&lt;Order&gt;<br>
	 * would get hubWhere+PP from hubFrom&lt;Orderitem&gt;, which would be hub&lt;Company&gt;+"customers.orders.orderItems", and would
	 * reverse it to "order.customer.company", where the first link is "order", which matches propName "order", and thisHub&lt;Order&gt;
	 * would end up using hub&lt;Company&gt;+"customers.orders"
	 * <p>
	 *
	 * @param thisHub  Hub that could be in the same propertyPath of the hubFromHub.whereHubPropertyPath
	 * @param propName the link name of thisHub from hubFrom.
	 * @param hubFrom  hub that might have a selectWhereHub & PP that can be used by thisHub.
	 */
	public static boolean adoptWhereHub(final Hub thisHub, final String propName, final Hub hubFrom) {
		if (hubFrom == null) {
			return false;
		}
		if (thisHub == null) {
			return false;
		}
		if (OAString.isEmpty(propName)) {
			return false;
		}
		final Hub hubSelectWhere = HubSelectDelegate.getSelectWhereHub(hubFrom);
		if (hubSelectWhere == null) {
			return false;
		}
		final String pp = HubSelectDelegate.getSelectWhereHubPropertyPath(hubFrom);
		if (OAString.isEmpty(pp)) {
			return false;
		}
		OAPropertyPath propPath = new OAPropertyPath(hubSelectWhere.getObjectClass(), pp, true);
		OAPropertyPath ppRev = propPath.getReversePropertyPath();

		String s = ppRev.getFirstPropertyName();
		if (!propName.equalsIgnoreCase(s)) {
			return false;
		}

		int x = OAString.dcount(pp, '.');
		s = OAString.field(pp, '.', 1, x - 1);

		thisHub.setSelectWhereHub(hubSelectWhere, s);
		return true;
	}

	// 20221209
	public static boolean refresh(final Hub thisHub) {

		boolean b = false;

		OAThreadLocalDelegate.setRefreshing(true);
		try {
			HubEventDelegate.fireBeforeRefreshEvent(thisHub);
			b = _refresh(thisHub);
		} finally {
			OAThreadLocalDelegate.setRefreshing(false);
		}

		return b;
	}

	// 20220311
	protected static boolean _refresh(final Hub thisHub) {
		if (thisHub == null) {
			return false;
		}

		OASelect sel = thisHub.getSelect();
		if (sel == null) {
			OAObject obj = HubDetailDelegate.getMasterObject(thisHub);
			if (obj != null) {
				String s = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
				obj.refresh(s);
				return true;
			}
			return false;
		}

		cancelSelect(thisHub, false);
		sel.reset(false);

		boolean bWasDirty = sel.getDirty();
		sel.setDirty(true);

		sel.select();

		List alNew = new ArrayList();
		for (Object objx : sel) {
			alNew.add(objx);
			if (!thisHub.contains(objx)) {
				thisHub.add(objx);
			}
		}

		List alRemove = new ArrayList();
		for (Object objx : thisHub) {
			if (!alNew.contains(objx)) {
				alRemove.add(objx);
			}
		}
		for (Object objx : alRemove) {
			thisHub.remove(objx);
		}
		int i = 0;
		for (Object objx : alNew) {
			int pos = thisHub.getPos(objx);
			if (i != pos) {
				thisHub.move(pos, i);
			}
			i++;
		}

		if (!bWasDirty) {
			sel.setDirty(false);
		}
		return true;
	}

	/**
	 * This will re-run the last select.
	 *
	 * @see OASelect
	 */
	public static boolean refreshSelect(Hub thisHub) {
		if (thisHub == null) {
			return false;
		}
		Object objAO = thisHub.getAO();
		OASelect sel = getSelect(thisHub);

		if (sel == null) {
			Object obj = thisHub.getMasterObject();
			if (!(obj instanceof OAObject)) {
				return false;
			}

			OALinkInfo linkInfo = HubDetailDelegate.getLinkInfoFromDetailToMaster(thisHub);
			if (linkInfo == null) {
				return false;
			}

			sel = new OASelect(thisHub.getObjectClass());
			sel.setWhereObject((OAObject) obj);
			sel.setPropertyFromWhereObject(linkInfo.getReverseName());
		} else {
			cancelSelect(thisHub, false);
			sel.reset(false);
		}

		boolean bWasDirty = sel.getDirty();
		if (!bWasDirty) {
			sel.setDirty(true);
		}
		sel.select();
		HashSet<Object> hs = new HashSet<Object>();
		for (; sel.hasMore();) {
			Object objx = sel.next();
			hs.add(objx);
			thisHub.add(objx);
		}
		if (!bWasDirty) {
			sel.setDirty(false);
		}

		// check to see if any objects need to be removed from the original list
		for (Object obj : thisHub) {
			if (!hs.contains(obj)) {
				thisHub.remove(obj);
			}
		}

		thisHub.setAO(objAO);
		return true;
	}

}

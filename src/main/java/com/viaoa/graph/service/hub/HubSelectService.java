package com.viaoa.graph.service.hub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.*;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

public abstract class HubSelectService {
	private final Logger LOG = Logger.getLogger(HubSelectService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubSelectService(Hub.FriendAccess faHub ) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Retrieves additional objects for the Hub from its most recent select()
	 * operation. Uses the select tied to the Hub.
	 *
	 * @param thisHub the Hub whose select results are being extended
	 * @return number of objects loaded during this fetch
	 */
	public int fetchMore(Hub<?> thisHub) {
		int x = fetchMore(thisHub, getSelect(thisHub));
		return x;
	}

	
	
	/**
	 * Retrieves more objects from the given {@link OASelect}, using its
	 * configured fetch amount to determine how many items to load.
	 *
	 * @param thisHub the Hub to populate
	 * @param sel     the OASelect instance providing objects
	 * @return number of objects fetched
	 */
	public int fetchMore(Hub<?> thisHub, OASelect<?> sel) {
		if (sel == null) {
			return 0;
		}
		int x = sel.getFetchAmount();
		x = fetchMore(thisHub, sel, x);
		return x;
	}

	/**
	 * Retrieves more objects using an explicit fetch amount instead of the
	 * OASelect’s configured value.
	 *
	 * @param thisHub the Hub to populate
	 * @param famt    number of objects to attempt retrieval
	 * @return number of objects fetched
	 */
	public int fetchMore(Hub<?> thisHub, int famt) {
		int x = fetchMore(thisHub, getSelect(thisHub), famt);
		return x;
	}

	/**
	 * Counter used internally to track the number of warnings issued for
	 * select/fetch operations.
	 */
	private int cntWarning;

	/**
	 * Tracks fetch locks per Hub to serialize concurrent fetchMore operations.
	 * Ensures only one thread fetches data for a Hub at a time.
	 */
	private  ConcurrentHashMap<Hub<?>, Integer> hmHubFetch = new ConcurrentHashMap<>(11, .85f);

	/**
	 * Core thread-safe implementation of fetchMore. Ensures only one thread
	 * loads data at a time for the given Hub by managing fetch locks in
	 * {@code hmHubFetch}.
	 *
	 * @param thisHub the Hub being populated
	 * @param sel     the OASelect providing data
	 * @param famt    fetch amount to use
	 * @return number of objects fetched
	 */
	public int fetchMore(Hub<?> thisHub, OASelect<?> sel, int famt) {
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
						hmHubFetch.put(thisHub, 1); // 1 == waiters present (debug/diagnostic); cleared when fetch lock released elsewhere
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

	/**
	 * Internal worker that pulls objects from the OASelect and inserts them into
	 * the Hub’s backing vector.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Stops when no more data is available.</li>
	 *   <li>Dynamically grows Hub capacity when needed.</li>
	 *   <li>Adds objects using {@link HubAddRemoveDelegate} with loading guarded
	 *       by {@link OAThreadLocalDelegate}.</li>
	 *   <li>Restores Hub “changed” state after loading completes.</li>
	 * </ul>
	 *
	 * @return number of objects successfully added
	 */
	public int _fetchMore(Hub<?> thisHub, OASelect<?> sel, int famt) {
		if (sel == null) {
			return 0;
		}
        if (sel.hasNextCompleted()) return 0;
		int fa = sel.getFetchAmount(); // default amount to load

		HubData<?> hubData = faHub.getHubData(thisHub);

		boolean holdDataChanged = hubData.getChanged();

		if (famt > 0) {
			fa = famt;
		}
		int cnt = 0;

		try {
			int capacity = hubData.getVector().capacity(); // number of available 'slots'
			int size = hubData.getVector().size(); // number of elements

			for (; cnt < fa || fa == 0;) {
				Object obj;
				if (!isMoreData(sel)) {
					boolean bRemoveSelectFromHub;
					if (thisHub.getMasterObject() != null) {
						OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(thisHub);
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
						if (faHub.getHubData(thisHub).loadingAllData) {
							capacity = getCount(thisHub);
							if (capacity <= 0) capacity = size+1;
						}
						*/
						capacity += (capacity > 250) ? 75 : capacity; // this will override the default behavior of how the Vector grows itself (which is to double in size)
						//LOG.config("resizing, from:"+size+", to:"+capacity+", hub:"+thisHub);
						callHubDataEnsureCapacity(thisHub, capacity);
					}
					
					try {
						callThreadLocalSetLoading(true);
						callHubAddRemoveAdd(thisHub, obj);
					} finally {
						callThreadLocalSetLoading(false);
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
			hubData.setChanged(holdDataChanged);
		}
		return cnt;
	}

	/**
	 * Determines whether additional data is available for the Hub’s current
	 * select() operation. Starts the select if needed.
	 *
	 * @param thisHub the Hub being queried
	 * @return true if more data is available; false otherwise
	 */
	public boolean isMoreData(Hub<?> thisHub) {
		OASelect<?> sel = getSelect(thisHub);
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
	 * Determines whether the given OASelect has more data to fetch.
	 *
	 * @param sel the OASelect instance
	 * @return true if more data is available; false otherwise
	 */
	public boolean isMoreData(OASelect<?> sel) {
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
	 * Loads all remaining data for the Hub’s current select(), using the Hub’s
	 * default select instance.
	 *
	 * @param thisHub the Hub whose select results should be fully loaded
	 */
	public void loadAllData(Hub thisHub) {
		loadAllData(thisHub, thisHub.getSelect());
	}

	/**
	 * Fully loads all remaining objects from the given OASelect into the Hub.
	 *
	 * <p>Ensures only one thread performs the full-load operation at a time.
	 * Handles cancellation, error recovery, and select state transitions.</p>
	 *
	 * @param thisHub the Hub being populated
	 * @param select  the OASelect instance to load from
	 */
	public void loadAllData(Hub<?> thisHub, OASelect<?> select) {
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
			synchronized (faHub.getHubData(thisHub)) {
				if (!faHub.getHubData(thisHub).isLoadingAllData()) {
					if (select == null) {
						break;
					}
					faHub.getHubData(thisHub).setLoadingAllData(true);
					bCanRun = true;
				}
			}

			if (bCanRun) {
				try {
					while (isMoreData(select)) {
						fetchMore(thisHub, select);
					}
				} finally {
					synchronized (faHub.getHubData(thisHub)) {
						faHub.getHubData(thisHub).setLoadingAllData(false);
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
	 * Returns the OASelect associated with the Hub, or null if none exists.
	 *
	 * @param thisHub the Hub being queried
	 * @return the Hub’s current OASelect, or null
	 */
	public <T extends OAObject> OASelect<T> getSelect(Hub<T> thisHub) {
		return getSelect(thisHub, false);
	}

	/**
	 * Retrieves the Hub’s OASelect instance, optionally creating a new one if none
	 * exists.
	 *
	 * @param thisHub       the Hub being queried
	 * @param bCreateIfNull true to create a new OASelect when missing
	 * @return the existing or newly created OASelect
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> OASelect<T> getSelect(Hub<T> thisHub, boolean bCreateIfNull) {
		if (thisHub == null) {
			return null;
		}
		OASelect<T> sel = faHub.getHubData(thisHub).getSelect();
		if (sel != null || !bCreateIfNull) {
			return sel;
		}

		Class<OAObject> classX = (Class) thisHub.getObjectClass();
		sel = new OASelect(classX);
		faHub.getHubData(thisHub).setSelect(sel);
		return sel;
	}

	/**
	 * Main select() method for Hubs. Prepares and executes the OASelect query,
	 * initializes Hub metadata, handles append/overwrite modes, and loads the
	 * first batch of data.
	 *
	 * @param thisHub the Hub to populate
	 * @param select  the select definition to run
	 */
	public <T extends OAObject> void select(final Hub<T> thisHub, OASelect<T> select) { // This is the main select method for Hub that all of the other select methods call.
		cancelSelect(thisHub, true);
		if (select == null) {
			return;
		}

		if (faHub.getHubDataUnique(thisHub).getSharedHub() != null) {
			select(faHub.getHubDataUnique(thisHub).getSharedHub(), select);
			return;
		}
		if (faHub.getHubData(thisHub).getObjClass() == null) {
			faHub.getHubData(thisHub).setObjClass(select.getSelectClass());
			if (faHub.getHubData(thisHub).getObjClass() == null) {
				return;
			}
		}

		if (faHub.getHubDataMaster(thisHub).getMasterObject() != null && faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null) {
			if (select != faHub.getHubData(thisHub).getSelect() && faHub.getHubData(thisHub).getSelect() != null) {
				throw new RuntimeException("select cant be changed for detail hub");
			}

		}
		if (select.getWhereObject() != null) {
			if (faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null && select.getWhereObject() == faHub.getHubDataMaster(thisHub).getMasterObject()) {
				select.setPropertyFromWhereObject(faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getReverseName());
			}
		}

		select.setSelectClass(thisHub.getObjectClass());
		OAObjectInfo oi = callObjectInfoGetObjectInfo(thisHub.getObjectClass());

		// 20200302
		Hub hx = faHub.getHubData(thisHub).getSelectWhereHub();
		if (hx != null) {
			String s = faHub.getHubData(thisHub).getSelectWhereHubPropertyPath();
			if (OAString.isNotEmpty(s)) {
				select.setWhereHub(hx, s);
			}
		}

		callHubEventFireBeforeSelectEvent(thisHub);

		boolean bRunSelect;
		bRunSelect = oi.getUseDataSource() || select.getDataSource() != null;

		// 20160110 selects now have hubFinders, etc to do selects
		bRunSelect = (bRunSelect && (select.getDataSource() != null || select.getFinder() != null));
		//was: bRunSelect = (bRunSelect && select.getDataSource() != null);

		callHubDataIncChangeCount(thisHub);

		if (select.getAppend()) {
			faHub.getHubData(thisHub).setSelect(select);
		} else {
			thisHub.setAO(null); // 20100507
			int z = callHubDataGetCurrentSize(thisHub);
			for (int i = 0; i < z; i++) {
				OAObject oa = (OAObject) callHubDataGetObjectAt(thisHub, i);
				callObjectHubRemoveHub(oa, thisHub, false);
			}
			callHubDataClearAllAndReset(thisHub);
			faHub.getHubData(thisHub).setSelect(select);

			if (select.getRewind()) {

				// 20120716
				OAFilter<Hub<?>> filter = new OAFilter<Hub<?>>() {
					@Override
					public boolean isUsed(Hub h) {
						if (h != thisHub && faHub.getHubDataActive(h) != faHub.getHubDataActive(thisHub)) {
							if (faHub.getHubDataUnique(h).getLinkToHub() == null) {
								return true;
							}
						}
						return false;
					}
				};
				Hub<?>[] hubs = callHubShareGetAllSharedHubs(thisHub, filter);

				for (int i = 0; i < hubs.length; i++) {
					if (hubs[i] != thisHub && faHub.getHubDataActive(hubs[i]) != faHub.getHubDataActive(thisHub)) {
						if (faHub.getHubDataUnique(hubs[i]).getLinkToHub() == null) {
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
			faHub.getHubData(thisHub).setSelectAllHub(true);
			callObjectCacheSetSelectAllHub(thisHub);
		} else {
			faHub.getHubData(thisHub).setSelectAllHub(false);
			callObjectCacheRemoveSelectAllHub(thisHub);
		}

		if (!select.getAppend()) {
			callHubEventFireOnNewListEvent(thisHub, true);
		}
	}

	/**
	 * Cancels the Hub’s current OASelect and optionally removes it from Hub data.
	 * Also resets selectAllHub flags and resizes the Hub to fit its contents.
	 *
	 * @param thisHub       the Hub whose select is being canceled
	 * @param bRemoveSelect true to clear the Hub’s select reference
	 */
	public void cancelSelect(Hub thisHub, boolean bRemoveSelect) {
		OASelect sel = faHub.getHubData(thisHub).getSelect();
		boolean bHasMoreData;
		if (sel != null) {
			boolean b = sel.hasBeenStarted();
			bHasMoreData = (b && (sel.isSelectingNow() || sel.hasMore()));
			if (b) {
				sel.cancel();
			}
			if (bRemoveSelect) {
				faHub.getHubData(thisHub).setSelect(null);
			}
			callHubDataResizeToFit(thisHub);
		} else {
			bHasMoreData = false;
		}

		if (faHub.getHubData(thisHub).isSelectAllHub() && bHasMoreData) {
			faHub.getHubData(thisHub).setSelectAllHub(false);
			callObjectCacheRemoveSelectAllHub(thisHub);
		}
	}

	/**
	 * Returns the total number of matching records for the Hub’s current select().
	 *
	 * @param thisHub the Hub being queried
	 * @return the count value, or -1 if no select exists
	 */
	public int getCount(Hub<?> thisHub) {
		if (thisHub == null) {
			return -1;
		}
		OASelect<?> sel = getSelect(thisHub);
		if (sel == null) {
			return -1;
		}
		return sel.getCount();
	}

	/**
	 * Indicates whether the current select() has been counted.
	 *
	 * @param thisHub the Hub being checked
	 * @return true if counted; false otherwise
	 */
	public boolean isCounted(Hub<?> thisHub) {
		if (thisHub == null) {
			return false;
		}
		OASelect<?> sel = getSelect(thisHub);
		if (sel == null) {
			return true;
		}
		return sel.isCounted();
	}

	/**
	 * Updates the Hub’s select() WHERE clause, creating a new OASelect if needed.
	 *
	 * @param thisHub the Hub whose select WHERE clause is modified
	 * @param s       the WHERE clause string
	 */
	public <T extends OAObject> void setSelectWhere(Hub<T> thisHub, String s) {
		OASelect<T> sel = getSelect(thisHub);
		if (sel == null) {
			sel = new OASelect(thisHub.getObjectClass());
			faHub.getHubData(thisHub).setSelect(sel);
		}
		sel.setWhere(s);
	}

	/**
	 * Returns the WHERE clause associated with the Hub’s current select().
	 *
	 * @param thisHub the Hub being queried
	 * @return the WHERE clause, or null if none exists
	 */
	public String getSelectWhere(Hub<?> thisHub) {
		OASelect<?> sel = getSelect(thisHub);
		if (sel == null) {
			return null;
		}
		return sel.getWhere();
	}

	/**
	 * Sets the ORDER BY clause for the Hub’s select operation. Creates a new
	 * OASelect instance if none exists and the sort property is non-empty.
	 *
	 * @param thisHub the Hub whose sort order is being modified
	 * @param s       the ORDER BY clause string
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> void setSelectOrder(Hub<T> thisHub, String s) {
		if (thisHub == null) return;
		faHub.getHubData(thisHub).setSortProperty(s);

		OASelect<T> sel = getSelect(thisHub);
		if (!OAString.isEmpty(s) && sel == null) {
			Class<OAObject> classX = (Class) thisHub.getObjectClass();
			sel = new OASelect(classX);
			faHub.getHubData(thisHub).setSelect(sel);
		}
		sel.setOrder(s);
	}

	/**
	 * Returns the ORDER BY clause associated with the Hub’s select().
	 *
	 * @param thisHub the Hub being queried
	 * @return the ORDER BY clause or null if none exists
	 */
	public String getSelectOrder(Hub<?> thisHub) {
		OASelect<?> sel = getSelect(thisHub);
		if (sel == null) {
			return null;
		}
		return sel.getOrder();
	}

	/**
	 * Executes a select() operation on the Hub with an optionally append-mode
	 * OASelect created automatically.
	 *
	 * @param thisHub     the Hub to populate
	 * @param bAppendFlag true to append results; false to overwrite
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> void select(Hub<T> thisHub, boolean bAppendFlag) {
		if (thisHub == null) {
			return;
		}
		Class<T> classX = thisHub.getObjectClass();
		OASelect<T> sel = new OASelect(classX);
		sel.setAppend(bAppendFlag);
		select(thisHub, sel);
	}

	/**
	 * Runs a select() using a WHERE object, WHERE clause, parameter list,
	 * ORDER BY clause, and append mode. Creates a new OASelect accordingly.
	 *
	 * @param thisHub      the Hub to populate
	 * @param whereObject  the object used for property-based filtering
	 * @param whereClause  the textual WHERE clause
	 * @param whereParams  parameter values for the WHERE clause
	 * @param orderByClause the sort expression
	 * @param bAppendFlag  true to append results; false to overwrite
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> void select(Hub<T> thisHub, OAObject whereObject, String whereClause,
			Object[] whereParams, String orderByClause, boolean bAppendFlag) {
		Class<T> classX = thisHub.getObjectClass();
		OASelect<T> sel = new OASelect(classX);
		sel.setWhereObject(whereObject);
		sel.setParams(whereParams);
		sel.setWhere(whereClause);
		sel.setAppend(bAppendFlag);
		sel.setOrder(orderByClause);
		select(thisHub, sel);
	}

	/**
	 * Same as the other select() overload, but applies an {@link OAFilter}
	 * to further restrict which objects qualify after retrieval.
	 *
	 * @param thisHub     the Hub to populate
	 * @param whereObject the WHERE-object used for reverse property resolution
	 * @param whereClause the WHERE clause
	 * @param whereParams list of parameters for the WHERE clause
	 * @param orderByClause ORDER BY clause
	 * @param bAppendFlag true to append results
	 * @param filter      filter applied to objects after select()
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public void select(Hub<?> thisHub, OAObject whereObject, String whereClause,
			Object[] whereParams, String orderByClause, boolean bAppendFlag, OAFilter filter) {
		Class<OAObject> classX = (Class) thisHub.getObjectClass();
		OASelect sel = new OASelect(classX);
		sel.setWhereObject(whereObject);
		sel.setParams(whereParams);
		sel.setWhere(whereClause);
		sel.setAppend(bAppendFlag);
		sel.setOrder(orderByClause);
		sel.setFilter(filter);
		select(thisHub, sel);
	}

	/**
	 * Performs a passthru select(), sending raw WHERE and ORDER clauses directly
	 * to the underlying data source without additional Hub-based constraints.
	 *
	 * @param thisHub     the Hub to populate
	 * @param whereClause raw WHERE clause
	 * @param orderClause raw ORDER BY clause
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> void selectPassthru(Hub<T> thisHub, String whereClause, String orderClause) {
		Class<T> classX = (Class) thisHub.getObjectClass();
		OASelect<T> sel = new OASelect(classX);
		sel.setPassthru(true);
		sel.setWhere(whereClause);
		sel.setOrder(orderClause);
		select(thisHub, sel);
	}

	/**
	 * Passthru select() variant that also supports append mode.
	 *
	 * @param thisHub     the Hub to populate
	 * @param whereClause raw WHERE clause
	 * @param orderClause raw ORDER BY clause
	 * @param bAppend     whether to append instead of clearing the Hub first
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> void selectPassthru(Hub<T> thisHub, String whereClause, String orderClause, boolean bAppend) {
		Class<T> classX = (Class) thisHub.getObjectClass();
		OASelect<T> sel = new OASelect(classX);
		sel.setPassthru(true);
		sel.setAppend(bAppend);
		sel.setWhere(whereClause);
		sel.setOrder(orderClause);
		select(thisHub, sel);
	}

	/**
	 * Returns the Hub currently used as the "whereHub" for select(), or null if none.
	 *
	 * @param thisHub the Hub being queried
	 * @return the whereHub controlling select filtering, or null
	 */
	public <T extends OAObject> Hub<T> getSelectWhereHub(Hub<T> thisHub) {
		if (thisHub == null) {
			return null;
		}
		return faHub.getHubData(thisHub).getSelectWhereHub();
	}

	/**
	 * Sets the Hub to be used as the "whereHub" for select(), which constrains
	 * queries based on a linked property path.
	 *
	 * @param thisHub the Hub whose whereHub is being set
	 * @param hub     the Hub to use for filtering
	 */
	public void setSelectWhereHub(Hub<?> thisHub, Hub hub) {
		if (thisHub == null) {
			return;
		}
		faHub.getHubData(thisHub).setSelectWhereHub(hub);
	}

	/**
	 * Returns the property path associated with the Hub’s whereHub, or null.
	 *
	 * @param thisHub the Hub being queried
	 * @return the whereHub property path
	 */
	public String getSelectWhereHubPropertyPath(Hub<?> thisHub) {
		if (thisHub == null) {
			return null;
		}
		return faHub.getHubData(thisHub).getSelectWhereHubPropertyPath();
	}

	/**
	 * Sets the property path used for converting a whereHub into an equivalent
	 * WHERE clause during select().
	 *
	 * @param thisHub the Hub being configured
	 * @param pp      the property path to use for filtering
	 */
	public void setSelectWhereHubPropertyPath(Hub<?> thisHub, String pp) {
		if (thisHub == null) {
			return;
		}
		faHub.getHubData(thisHub).setSelectWhereHubPropertyPath(pp);
	}

	/*
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
	
	/**
	 * Attempts to adopt the whereHub + propertyPath from another Hub if thisHub
	 * participates in the same property path chain.
	 *
	 * <p>Used to propagate filtering constraints across related Hubs.</p>
	 *
	 * @param thisHub  the Hub attempting to adopt whereHub filtering
	 * @param propName the property linking hubFrom → thisHub
	 * @param hubFrom  the Hub that may supply whereHub filtering rules
	 * @return true if the whereHub was successfully adopted
	 */
	public boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom) {
		if (hubFrom == null) {
			return false;
		}
		if (thisHub == null) {
			return false;
		}
		if (OAString.isEmpty(propName)) {
			return false;
		}
		final Hub hubSelectWhere = getSelectWhereHub(hubFrom);
		if (hubSelectWhere == null) {
			return false;
		}
		final String pp = getSelectWhereHubPropertyPath(hubFrom);
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

	/**
	 * Refreshes the Hub’s contents. Fires pre-refresh events, then delegates
	 * to {@link #_refresh(Hub)} while honoring thread-local refresh flags.
	 *
	 * @param thisHub the Hub to refresh
	 * @return true if refresh occurred; false otherwise
	 */
	public boolean refresh(final Hub<?> thisHub) {
		boolean b = false;
		callThreadLocalSetRefreshing(true);
		try {
			callHubEventFireBeforeRefreshEvent(thisHub);
			b = _refresh(thisHub);
		} finally {
			callThreadLocalSetRefreshing(false);
		}

		return b;
	}

	/**
	 * Re-runs the select() operation or reloads the master-object detail list
	 * to bring the Hub’s contents into consistency with the data source.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>If no select exists: refreshes based on the master object link.</li>
	 *   <li>If select exists: resets, re-selects, and reconciles adds/removes.</li>
	 *   <li>Maintains element ordering to match the freshly retrieved dataset.</li>
	 * </ul>
	 *
	 * @param thisHub the Hub being refreshed
	 * @return true if refresh succeeded, false otherwise
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> boolean _refresh(final Hub<T> thisHub) {
		if (thisHub == null) {
			return false;
		}

		OASelect<?> sel = thisHub.getSelect();
		if (sel == null) {
			OAObject obj = callHubDetailGetMasterObject(thisHub);
			if (obj != null) {
				String s = callHubDetailGetPropertyFromMasterToDetail(thisHub);
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

		List<T> alNew = new ArrayList<>();
		for (Object objx : sel) {
			alNew.add((T) objx);
			if (!thisHub.contains(objx)) {
				thisHub.add((T) objx);
			}
		}

		List<T> alRemove = new ArrayList();
		for (T objx : thisHub) {
			if (!alNew.contains(objx)) {
				alRemove.add(objx);
			}
		}
		for (Object objx : alRemove) {
			thisHub.remove(objx);
		}
		int i = 0;
		for (T objx : alNew) {
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
	 * Re-executes the last select() operation associated with the Hub.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>Rebuilds the OASelect if needed (e.g., detail Hub with no select).</li>
	 *   <li>Runs select(), then merges results into the Hub.</li>
	 *   <li>Restores Active Object after reload.</li>
	 * </ul>
	 *
	 * @param thisHub the Hub to refresh
	 * @return true if successful
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <T extends OAObject> boolean refreshSelect(Hub<T> thisHub) {
		if (thisHub == null) {
			return false;
		}
		T objAO = thisHub.getAO();
		OASelect<T> sel = getSelect(thisHub);

		if (sel == null) {
			OAObject obj = thisHub.getMasterObject();
			if (obj == null) return false;

			OALinkInfo linkInfo = callHubDetailGetLinkInfoFromDetailToMaster(thisHub);
			if (linkInfo == null) {
				return false;
			}
			Class<T> classX = (Class) thisHub.getObjectClass();
			sel = new OASelect(classX);
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
		HashSet<T> hs = new HashSet();
		for (; sel.hasMore();) {
			T objx = sel.next();
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

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(Class<?> clazz);
	
	@OAParentProvided (example = "srvcObject.getOAObjectHubService().removeHub")
	public abstract void callObjectHubRemoveHub(final OAObject oaObj, Hub<?> hub, boolean bIsOnHubFinalize);
	
	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().setSelectAllHub")
	public abstract void callObjectCacheSetSelectAllHub(Hub hub);

	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().removeSelectAllHub")
	public abstract void callObjectCacheRemoveSelectAllHub(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getLinkInfoFromDetailToMaster")
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDataService().ensureCapacity")
	public abstract void callHubDataEnsureCapacity(Hub<?> hub, int size);

	@OAParentProvided (example = "srvcHub.getHubAddRemoveService().add")
	public abstract boolean callHubAddRemoveAdd(final Hub<?> hub, final Object obj);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireBeforeSelectEvent")
	public abstract void callHubEventFireBeforeSelectEvent(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDataService().incChangeCount")
	public abstract void callHubDataIncChangeCount(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDataService().getCurrentSize")
	public abstract int callHubDataGetCurrentSize(Hub<?> hub);
	
	@OAParentProvided (example = "srvcHub.getHubDataService().getObjectAt")
	public abstract Object callHubDataGetObjectAt(Hub<?> hub, int pos);

	@OAParentProvided (example = "srvcHub.getHubDataService().clearAllAndReset")
	public abstract void callHubDataClearAllAndReset(Hub<?> hub);
	
	@OAParentProvided (example = "srvcHub.getHubShareService().getAllSharedHubs")
	public abstract Hub<?>[] callHubShareGetAllSharedHubs(Hub<?> hub, OAFilter<Hub<?>> filter);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireOnNewListEvent")
	public abstract void callHubEventFireOnNewListEvent(Hub<?> hub, boolean bAll);

	@OAParentProvided (example = "srvcHub.getHubDataService().resizeToFit")
	public abstract void callHubDataResizeToFit(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubEventService().fireBeforeRefreshEvent")
	public abstract void callHubEventFireBeforeRefreshEvent(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getMasterObject")
	public abstract OAObject callHubDetailGetMasterObject(Hub<?> hub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getPropertyFromMasterToDetail")
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<?> hub);
	
	@OAParentProvided (example = "srvcThreadLocal.setLoading")
	public abstract boolean callThreadLocalSetLoading(boolean b);

	@OAParentProvided (example = "srvcThreadLocal.setRefreshing")
	public abstract void callThreadLocalSetRefreshing(boolean b);
}


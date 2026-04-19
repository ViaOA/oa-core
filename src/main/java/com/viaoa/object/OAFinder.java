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

import java.lang.reflect.*;
import java.util.*;

import com.viaoa.filter.*;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.graph.service.object.OAObjectPropertyService;
import com.viaoa.graph.service.object.OAObjectSiblingService;
import com.viaoa.hub.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.util.*;

/**
 * Utility for searching the OA Object Graph along a declarative
 * {@link com.viaoa.util.OAPropertyPath}. OAFinder can operate purely
 * in-memory or dynamically lazy-load graph segments with sibling
 * optimization to minimize round-trips to a datasource.
 *
 * <h3>How It Works</h3>
 * <ul>
 *   <li>Start from a root {@link com.viaoa.object.OAObject} or {@link com.viaoa.hub.Hub}</li>
 *   <li>Walk a {@code OAPropertyPath} (links + hubs) using metadata {@code OALinkInfo[]}</li>
 *   <li>Apply programmatic filters or parsed SQL-like Object Queries</li>
 *   <li>Return matching target objects with optional max-results cutoff</li>
 * </ul>
 *
 * <h3>Integration with OAPropertyPath</h3>
 * {@link com.viaoa.util.OAPropertyPath} parses dotted navigation such as:
 * <pre>
 *   "orders.items.product.vendor"
 * </pre>
 * including:
 * <ul>
 *   <li>link traversal</li>
 *   <li>hub collection steps</li>
 *   <li>casts (e.g. "(Manager)")</li>
 *   <li>embedded filter directives</li>
 * </ul>
 * OAFinder uses this metadata to safely traverse the Graph without writing
 * domain-specific traversal code.
 *
 * <h3>Filters</h3>
 * Filters may be added via:
 * <ul>
 *   <li>Programmatic criteria: {@code addEqualFilter()}, {@code addBetweenFilter()}, ...</li>
 *   <li>PropertyPath-embedded filters</li>
 *   <li><b>Query Filters</b> parsed from SQL-style Object Query strings</li>
 * </ul>
 *
 * <h4>SQL-style Object Queries</h4>
 * Ad-hoc queries over the Object Graph using business-friendly syntax:
 * <pre>
 *   "user.lastName = 'Smith' AND loginCount >= 3"
 * </pre>
 * The expression is parsed and translated into a composition of
 * {@link com.viaoa.filter.OAFilter} instances. Nested properties can
 * reference full PropertyPaths. Queries are evaluated against in-memory
 * values and may trigger lazy loads when enabled.
 *
 * <h3>Lazy Loading + Sibling Optimization</h3>
 * When scanning from a {@link com.viaoa.hub.Hub} and in lazy-mode, OAFinder
 * registers a sibling fetch helper to proactively prefetch adjacent rows
 * along the current PropertyPath context. This:
 * <ul>
 *   <li>dramatically reduces round-trips for scrolling lists</li>
 *   <li>optimizes parent/child expansion</li>
 *   <li>preserves single-instance identity using OAObjectCache</li>
 * </ul>
 * Set {@code useOnlyLoadedData = true} to enforce strictly in-memory traversal.
 *
 * <h3>Execution Control</h3>
 * <ul>
 *   <li>{@code findFirst()}, {@code findNext()}, {@code findLast()}</li>
 *   <li>{@code setMaxFound(n)} to stop once enough matches are found</li>
 *   <li>{@code stop()} inside {@code onFound()} to early-abort</li>
 *   <li>Optional traversal {@code stack} maintained for diagnostics</li>
 * </ul>
 *
 * <h3>Cycle Prevention</h3>
 * Object cycles are automatically prevented using {@link com.viaoa.object.OACascade}.
 *
 * <h3>Thread Safety</h3>
 * Not thread-safe. Each {@code OAFinder} instance should be used for a single
 * search operation on a single thread.
 *
 * <h3>Examples</h3>
 *
 * Find by PropertyPath and exact match:
 * <pre>{@code
 * OAFinder<Store, Customer> f =
 *     new OAFinder<>(storeHub, StorePP.customers().pp);
 * f.addEqualFilter(CustomerPP.status().pp, "Active");
 * Customer c = f.findFirst();
 * }</pre>
 *
 * With SQL-style Object Query:
 * <pre>{@code
 * OAFinder<Order, Order> f =
 *     new OAFinder<>(ordersHub, OrderPP.self().pp);
 * f.setQueryFilter("status = 'Open' AND totalAmount > 100");
 * List<Order> result = f.find(ordersHub);
 * }</pre>
 *
 * Find largest by numeric PropertyPath:
 * <pre>{@code
 * Order biggest =
 *     OAFinder.findLargest(ordersHub, OrderPP.totalAmount().pp);
 * }</pre>
 *
 * Detect duplicates:
 * <pre>{@code
 * List<Customer> dups =
 *     OAFinder.findDuplicates(customerHub, CustomerPP.email().pp);
 * }</pre>
 *
 * @param <F> From-type (root)
 * @param <T> Target type returned by the search
 *
 * @see com.viaoa.util.OAPropertyPath
 * @see com.viaoa.filter.*
 * @see com.viaoa.object.OACascade
 * @see com.viaoa.hub.Hub
 */
public class OAFinder<F extends OAObject, T extends OAObject> {
	
	/**
	 * The raw property-path expression (string form) defining the navigation
	 * route for this finder before it is parsed into an OAPropertyPath.
	 */
	private String strPropertyPath;
	
	/**
	 * Parsed representation of the property path used to navigate through
	 * the Object Graph during a search.
	 */
	private OAPropertyPath<T> propertyPath;

	/**
	 * LinkInfo representing the recursive parent link for the root object,
	 * enabling upward traversal for recursive models.
	 */
	private OALinkInfo liRecursiveRoot;

	/**
	 * LinkInfo array describing the navigational steps for each segment
	 * of the property path.
	 */
	private OALinkInfo[] linkInfos;
	
	/**
	 * Optional LinkInfo array for recursive link evaluation when a path
	 * includes recursive relationships.
	 */
	private OALinkInfo[] recursiveLinkInfos;
	
	/**
	 * Java reflection methods used to access intermediate property values
	 * along the property path.
	 */
	private Method[] methods;

	/**
	 * Flag indicating that the next added filter should be composed using OR
	 * with the existing filter.
	 */
	private boolean bAddOrFilter;
	
	/**
	 * Flag indicating that the next added filter should be composed using AND
	 * with the existing filter.
	 */
	private boolean bAddAndFilter;
	
	/**
	 * The root filter applied to restrict which objects are considered matches
	 * during search traversal.
	 */
	private OAFilter filter;
	
	/**
	 * Cascade trackers used for each level of the property-path traversal to
	 * prevent infinite loops and redundant recursion.
	 */
	private OACascade[] cascades;

	/**
	 * Flag indicating that the active search should be stopped as soon as
	 * possible.
	 */
	private volatile boolean bStop;
	
	/**
	 * The list of objects that have been found and accepted during the search.
	 */
	private List<T> alFound;

	/**
	 * Flag indicating whether the diagnostic traversal stack should be
	 * maintained during find operations.
	 */
	private boolean bEnableStack;

	/**
	 * Current index within the traversal stack indicating how deep the
	 * property-path recursion is.
	 */
	private int stackPos;

	/**
	 * Stack tracking the objects and positions visited during traversal for
	 * diagnostic and debugging purposes.
	 */
	private StackValue[] stack;
	
	/**
	 * Maximum number of objects to find before the search should stop.
	 * A value of zero means no limit.
	 */
	private int maxFound;
	
	/**
	 * The root OAObject from which the search begins when not using a Hub.
	 */
	private F fromObject;
	
	/**
	 * The root Hub supplying starting objects for the search.
	 */
	private Hub<F> fromHub;
	
	/**
	 * Indicates whether all Hub elements should be used as search roots
	 * instead of only the active object.
	 */
	private boolean bUseAll;
	
	/**
	 * Flag indicating whether recursive traversal from the root object or
	 * Hub should be allowed.
	 */
	private boolean bEnableRecursiveRoot;
	
	/**
	 * Internal flag indicating whether recursive-root settings were explicitly
	 * modified by client code.
	 */
	private boolean bEnableRecursiveRootWasCalled;

	/**
	 * Optional externally supplied cascade tracker applied before internal
	 * cascades to prevent repeated visits.
	 */
	private OACascade cascade; // cascade that can be set by calling code

	/**
	 * Flag indicating whether traversal must operate strictly on already-loaded
	 * in-memory data, without triggering lazy loads.
	 */
	private boolean bUseOnlyLoadedData;

	/**
	 * Current positional index within the root Hub during a Hub-based search.
	 */
	private int rootHubPos;
	
	/**
	 * Indicates whether the finder has already been initialized based on
	 * the root object’s class and property path.
	 */
	private boolean bSetup;
	
	/**
	 * Creates an empty finder with no root and no property path defined.
	 * <p>
	 * A root object or Hub and a property path must be supplied before
	 * performing any search operations.
	 * </p>
	 */
	public OAFinder() {
	}

	/**
	 * Creates a finder configured with the specified property path.
	 * <p>
	 * A root object or Hub must still be provided before performing a search.
	 * </p>
	 *
	 * @param propPath the property path to navigate during the search
	 */
	public OAFinder(String propPath) {
		this.strPropertyPath = propPath;
	}

	/**
	 * Creates a finder configured with a root object and a property path.
	 *
	 * @param fromObject the starting root object for the search
	 * @param propPath the property path to navigate during the search
	 */
	public OAFinder(F fromObject, String propPath) {
		this.fromObject = fromObject;
		this.strPropertyPath = propPath;
	}

	/**
	 * Creates a finder using the specified Hub as the root and the given
	 * property path. All objects in the Hub may be used as search roots.
	 *
	 * @param fromHub the Hub supplying root objects
	 * @param propPath the property path to navigate during the search
	 */
	public OAFinder(Hub<F> fromHub, String propPath) {
		this(fromHub, propPath, true);
	}

	/**
	 * Creates a finder using the specified Hub as the root and the given
	 * property path, with control over whether all Hub objects are used as
	 * search roots.
	 *
	 * @param fromHub the Hub supplying root objects
	 * @param propPath the property path to navigate during the search
	 * @param bUseAll {@code true} to evaluate all Hub elements as roots;
	 *                 {@code false} to evaluate only the active object
	 */
	public OAFinder(Hub<F> fromHub, String propPath, boolean bUseAll) {
		this.fromHub = fromHub;
		this.strPropertyPath = propPath;
		this.bUseAll = bUseAll;
	}

	/**
	 * Enables or disables recursive traversal starting from the root object
	 * or root Hub during a search.
	 *
	 * @param b {@code true} to allow recursive navigation from the root,
	 *          {@code false} to disable it
	 */
	public void setAllowRecursiveRoot(boolean b) {
		this.bEnableRecursiveRoot = b;
		this.bEnableRecursiveRootWasCalled = true;
	}

	/**
	 * Returns whether recursive traversal from the root object or root Hub
	 * is enabled.
	 *
	 * @return {@code true} if recursive root traversal is allowed,
	 *         otherwise {@code false}
	 */
	public boolean getAllowRecursiveRoot() {
		return this.bEnableRecursiveRoot;
	}

	/**
	 * Called when a matching object is found during a search.
	 * <p>
	 * The default implementation adds the object to the result list. If a
	 * maximum result count has been set, this method will stop the search
	 * once the limit is reached.
	 * </p>
	 *
	 * @param obj the object that was found
	 */
	protected void onFound(T obj) {
		alFound.add(obj);
		if (maxFound > 0 && alFound.size() >= maxFound) {
			stop();
		}
	}

	/**
	 * Called when a required object or link is not found during traversal.
	 * <p>
	 * The default implementation performs no action. Subclasses may override
	 * to handle missing data or abort the search using {@link #stop()}.
	 * </p>
	 */
	protected void onDataNotFound() {
	}

	/**
	 * Requests that the active search operation be stopped.
	 * <p>
	 * This flag is checked during traversal and allows subclasses or filters
	 * to abort a search early, typically from within {@link #onFound(Object)}
	 * or {@link #onDataNotFound()}.
	 * </p>
	 */
	public void stop() {
		bStop = true;
	}

	/**
	 * Returns whether the current search has been flagged to stop.
	 *
	 * @return {@code true} if a stop has been requested, otherwise {@code false}
	 */
	public boolean getStop() {
		return bStop;
	}

	/**
	 * Specifies whether the search should operate only on data that is
	 * already loaded in memory.
	 * <p>
	 * When enabled, traversal will not trigger any lazy loading and will
	 * abort paths where required data is not yet loaded.
	 * </p>
	 *
	 * @param b {@code true} to restrict traversal to loaded data only,
	 *          {@code false} to allow lazy loading
	 */
	public void setUseOnlyLoadedData(boolean b) {
		this.bUseOnlyLoadedData = b;
	}

	/**
	 * Returns whether the search is restricted to data already loaded
	 * in memory.
	 *
	 * @return {@code true} if traversal avoids lazy loading,
	 *         otherwise {@code false}
	 */
	public boolean getUseOnlyLoadedData() {
		return bUseOnlyLoadedData;
	}

	/**
	 * Sets the maximum number of objects to return during a search.
	 * <p>
	 * A value of {@code 0} indicates no limit. Once the limit is reached,
	 * the search will stop automatically.
	 * </p>
	 *
	 * @param x the maximum number of results to return
	 */
	public void setMaxFound(int x) {
		this.maxFound = x;
	}

	/**
	 * Returns the maximum number of objects the search is allowed to return.
	 *
	 * @return the maximum result count, or {@code 0} if unlimited
	 */
	public int getMaxFound() {
		return this.maxFound;
	}

	/**
	 * Initiates a search using the currently configured root object or Hub.
	 * <p>
	 * If a root object is defined, the search begins there. If a root Hub is
	 * defined, the search uses either all Hub elements or only the active
	 * object depending on configuration. If no root is defined, {@code null}
	 * is returned.
	 * </p>
	 *
	 * @return the list of matching objects, or {@code null} if no root is available
	 */
	public List<T> find() {
		if (fromObject != null) {
			return find(fromObject);
		}
		if (fromHub != null) {
			if (bUseAll) {
				return find(fromHub);
			}
			F obj = fromHub.getAO();
			if (obj != null) {
				return find(obj);
			}
		}
		return null;
	}

	/**
	 * Sets the root object from which searches will begin.
	 *
	 * @param obj the new root object
	 */
	public void setRoot(F obj) {
		this.fromObject = obj;
	}

	/**
	 * Sets the root Hub from which searches will begin.
	 *
	 * @param hub the Hub to use as the search root
	 */
	public void setRoot(Hub<F> hub) {
		this.fromHub = hub;
	}

	/**
	 * Performs a search beginning at the specified Hub using the configured
	 * property path.
	 *
	 * @param hubRoot the Hub supplying root objects for the search
	 * @return the list of matching objects
	 */
	public List<T> find(Hub<F> hubRoot) {
		List<T> al = find(hubRoot, null);
		return al;
	}

	/**
	 * Performs a search beginning from each object in the supplied list
	 * using the configured property path.
	 *
	 * @param alRoot the list of root objects to evaluate
	 * @return the list of matching objects
	 */
	public List<T> find(List<F> alRoot) {
		return find(alRoot, null);
	}

	/**
	 * Performs a search beginning from the objects in the supplied list,
	 * starting immediately after the specified last-used root object.
	 *
	 * @param alRoot the list of potential root objects
	 * @param objectLastUsed the root object after which searching should begin;
	 *                       may be {@code null} to start at the beginning
	 * @return the list of matching objects
	 */
	public List<T> find(List<F> alRoot, F objectLastUsed) {
		if (!bEnableRecursiveRootWasCalled) {
			bEnableRecursiveRoot = false;
		}

		alFound = new ArrayList<T>();
		if (bEnableStack) {
			stack = new StackValue[5];
		}

		if (alRoot == null) {
			return alFound;
		}
		int x = alRoot.size();
		if (x == 0) {
			return alFound;
		}

		F sample = alRoot.get(0);

		bStop = false;
		setup(sample.getClass());

		int pos;
		if (objectLastUsed == null) {
			pos = 0;
		} else {
			pos = alRoot.indexOf(objectLastUsed) + 1;
		}

		for (; pos < x; pos++) {
			F objectRoot = alRoot.get(pos);
			if (objectRoot == null) {
				continue;
			}
			stackPos = 0;
			performFind(objectRoot);
			if (bStop) {
				break;
			}
		}
		List<T> al = alFound;
		this.alFound = null;
		this.stack = null;
		this.stackPos = 0;
		this.cascades = null;
		return al;
	}

	/**
	 * Performs a search beginning from a Hub, starting immediately after
	 * the specified last-used root object.
	 * <p>
	 * When lazy loading is enabled, a sibling helper is temporarily
	 * installed to optimize adjacent data loading during traversal.
	 * </p>
	 *
	 * @param hubRoot the Hub supplying root objects for the search
	 * @param objectLastUsed the object after which searching should begin;
	 *                       may be {@code null} to start at the beginning
	 * @return the list of matching objects
	 */
	public List<T> find(Hub<F> hubRoot, F objectLastUsed) {
		if (!bEnableRecursiveRootWasCalled) {
			if (hubRoot != null) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubRoot);
				OALinkInfo li = og.hubsInternal().callHubDetailGetLinkInfoFromMasterObjectToDetail(hubRoot);
				if (li != null && li.getRecursive()) {
					bEnableRecursiveRoot = true;
				}
			} else {
				bEnableRecursiveRoot = true;
			}
		}
		List<T> al = null;

		OASiblingHelper<F> siblingHelper = null;
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		if (!bUseOnlyLoadedData) {
			siblingHelper = new OASiblingHelper<F>(hubRoot);
			siblingHelper.add(strPropertyPath);
			srvcOAThreadLocal.addSiblingHelper(siblingHelper);
		}
		try {
			al = _find(hubRoot, objectLastUsed);
		} finally {
			if (siblingHelper != null) {
				srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
			}
		}
		return al;
	}

	public int getRootHubPos() {
		return this.rootHubPos;
	}

	/**
	 * Given the propertyPath, find all of the objects from a Hub, starting after objectLastFound
	 */
	protected List<T> _find(Hub<F> hubRoot, F objectLastUsed) {
		alFound = new ArrayList<T>();
		if (bEnableStack) {
			stack = new StackValue[5];
		}

		if (hubRoot == null) {
			return alFound;
		}

		rootHubPos = -1;
		bStop = false;
		setup(hubRoot.getObjectClass());

		if (objectLastUsed == null) {
			rootHubPos = 0;
		} else {
			rootHubPos = hubRoot.getPos(objectLastUsed) + 1;
		}

		for (;; rootHubPos++) {
			F objectRoot = hubRoot.getAt(rootHubPos);
			if (objectRoot == null) {
				break;
			}
			stackPos = 0;
			performFind(objectRoot);
			if (bStop) {
				break;
			}
		}
		List<T> al = alFound;
		this.alFound = null;
		this.stack = null;
		this.stackPos = 0;
		cascades = null;
		return al;
	}

	public void clearFilters() {
		filter = null;
	}

	public void addFilter(OAFilter filter) {
		if (this.filter == null) {
			this.filter = filter;
		} else {
			if (bAddOrFilter) {
				this.filter = new OAOrFilter(this.filter, filter);
			} else {
				this.filter = new OAAndFilter(this.filter, filter);
			}
		}
		bAddAndFilter = bAddOrFilter = false;
	}

	public OAFilter getFilter() {
		return this.filter;
	}

	public void setFilter(OAFilter f) {
		this.filter = f;
	}

	/**
	 * Returns true if a matching value is found.
	 */
	public boolean canFindFirst(F objectRoot) {
		int holdMax = getMaxFound();
		setMaxFound(1);
		List<T> al = find(objectRoot);
		if (getMaxFound() == 1) {
			setMaxFound(holdMax);
		}
		return (al != null && al.size() > 0);
	}

	public T findFirst() {
		if (fromObject != null) {
			return findFirst(fromObject);
		}
		if (fromHub != null) {
			if (bUseAll) {
				return findFirst(fromHub);
			}
			F obj = fromHub.getAO();
			if (obj != null) {
				return findFirst(obj);
			}
		}
		return null;
	}

	
	/**
	 * Finds the first matching value. If searching for a null, then this would return a null, so use the canFindFirst method instead.
	 */
	public T findFirst(F objectRoot) {
		if (objectRoot == null) {
			return null;
		}
		int holdMax = getMaxFound();
		setMaxFound(1);
		List<T> al = find(objectRoot);
		T obj;
		if (al != null && al.size() > 0) {
			obj = al.get(0);
		} else {
			obj = null;
		}
		if (getMaxFound() == 1) {
			setMaxFound(holdMax);
		}
		return obj;
	}

	public T findFirst(Hub<F> hub) {
		int holdMax = getMaxFound();
		setMaxFound(1);
		List<T> al = find(hub);
		T obj;
		if (al.size() > 0) {
			obj = al.get(0);
		} else {
			obj = null;
		}
		if (getMaxFound() == 1) {
			setMaxFound(holdMax);
		}
		return obj;
	}

	public T findNext(Hub<F> hub, F objectLastUsed) {
		int holdMax = getMaxFound();
		setMaxFound(1);
		List<T> al = find(hub, objectLastUsed);
		T obj;
		if (al.size() > 0) {
			obj = al.get(0);
		} else {
			obj = null;
		}
		if (getMaxFound() == 1) {
			setMaxFound(holdMax);
		}
		return obj;
	}

	
    public T findLast() {
        List<T> al = find();
        if (al == null) return null;
        int x = al.size();
        if (x == 0) return null;
        return al.get(x-1);
    }
    public T findLast(F objectRoot) {
        List<T> al = find(objectRoot);
        if (al == null) return null;
        int x = al.size();
        if (x == 0) return null;
        return al.get(x-1);
    }
    public T findLast(Hub<F> hub) {
        List<T> al = find(hub);
        if (al == null) return null;
        int x = al.size();
        if (x == 0) return null;
        return al.get(x-1);
    }


    static protected abstract class CompareFilter implements OAFilter {
        final String pp;
        Object objFound;
        Object valueFound;
        
        boolean bCancel;
        
        public CompareFilter(final String pp) {
            this.pp = pp;
        }
        @Override
        public boolean isUsed(final Object obj) {
            if (bCancel) return true;
            if (!(obj instanceof OAObject)) return false;
            Object objValue = ((OAObject)obj).getProperty(pp);
            if (objFound == null) {
                objFound = obj;
                valueFound = objValue;
                return true;
            }
           
            int x = OACompare.compare(objValue,  valueFound);
            if (isCompareUsed(x)) {
                objFound = obj;
                valueFound = objValue;
                return true;
            }
            return false;
        }
        protected void cancel() {
            this.bCancel = true;
        }
        abstract boolean isCompareUsed(int compareValue);
    }
    
    
    public T findLargest(final String pp) {
        CompareFilter cf = new CompareFilter(pp) {
            @Override
            boolean isCompareUsed(int compareValue) {
                return compareValue >= 0;
            }
        };
        addFilter(cf);
        T t = findLast();
        cf.cancel();
        return t;
    }
    public T findLargest(F objectRoot, String pp) {
        CompareFilter cf = new CompareFilter(pp) {
            @Override
            boolean isCompareUsed(int compareValue) {
                return compareValue >= 0;
            }
        };
        addFilter(cf);
        T t = findLast(objectRoot);
        cf.cancel();
        return t;
    }
    public T findLargest(Hub<F> hub, String pp) {
        CompareFilter cf = new CompareFilter(pp) {
            @Override
            boolean isCompareUsed(int compareValue) {
                return compareValue >= 0;
            }
        };
        addFilter(cf);
        T t = findLast(hub);
        cf.cancel();
        return t;
    }
    
    public T findSmallest(final String pp) {
        CompareFilter cf = new CompareFilter(pp) {
            @Override
            boolean isCompareUsed(int compareValue) {
                return compareValue <= 0;
            }
        };
        addFilter(cf);
        T t = findLast();
        cf.cancel();
        return t;
    }
    public T findSmallest(F objectRoot, String pp) {
        CompareFilter cf = new CompareFilter(pp) {
            @Override
            boolean isCompareUsed(int compareValue) {
                return compareValue <= 0;
            }
        };
        addFilter(cf);
        T t = findLast(objectRoot);
        cf.cancel();
        return t;
    }
    public T findSmallest(Hub<F> hub, String pp) {
        CompareFilter cf = new CompareFilter(pp) {
            @Override
            boolean isCompareUsed(int compareValue) {
                return compareValue <= 0;
            }
        };
        addFilter(cf);
        T t = findLast(hub);
        cf.cancel();
        return t;
    }

    static protected class DuplicateFilter implements OAFilter {
        final String pp;
        boolean bCancel;
        final Map<Object, OAObject> hm = new HashMap<>();
        final Map<Object, OAObject> hm2 = new HashMap<>();
        
        public DuplicateFilter(final String pp) {
            this.pp = pp;
        }
        @Override
        public boolean isUsed(final Object obj) {
            if (bCancel) return true;
            if (!(obj instanceof OAObject)) return false;
            
            Object objValue = ((OAObject)obj).getProperty(pp);
            if (objValue == null) return false;
           
            OAObject objx = hm.get(objValue);
            if (objx != null) {
                hm2.put(objValue, objx);
                return true;
            }
            else {
                hm.put(objValue, (OAObject) obj);
            }
            return false;
        }
        protected void cancel() {
            this.bCancel = true;
        }
    }
    
    /**
     * Returns all objects that have another object with a duplicate value for a property (path).<br>
     * Note: null values are not included.
     */
    public List<T> findDuplicates(F objectRoot, String pp) {
        DuplicateFilter f = new DuplicateFilter(pp);
        addFilter(f);
        List<T> al = find(objectRoot);
        f.cancel();
        f.hm.clear();
        
        for (Map.Entry<Object, OAObject> entry : f.hm2.entrySet()) {
            al.add((T) entry.getValue());
        }
        f.hm2.clear();
        return al;
    }
    
	/**
	 * Given the propertyPath, find all of the objects from a root object.
	 *
	 * @param objectRoot starting object to begin navigating through the propertyPath.
	 */
	public List<T> find(F objectRoot) {
		if (objectRoot == null) {
			return null;
		}
		alFound = new ArrayList<T>();
		if (bEnableStack) {
			stack = new StackValue[5];
		}
		stackPos = 0;

		if (objectRoot == null) {
			return alFound;
		}

		bStop = false;
		setup(objectRoot.getClass());
		performFind(objectRoot);
		List<T> al = alFound;
		this.alFound = null;
		this.stack = null;
		this.stackPos = 0;
		this.cascades = null;
		return al;
	}

	public OAPropertyPath getPropertyPath() {
		return this.propertyPath;
	}

	protected void setup(Class c) {
		if (bSetup) {
			return;
		}
		bSetup = true;
		if (propertyPath != null || c == null) {
			return;
		}

		propertyPath = new OAPropertyPath(c, strPropertyPath, false);

		linkInfos = propertyPath.getLinkInfos();
		recursiveLinkInfos = propertyPath.getRecursiveLinkInfos();
		methods = propertyPath.getMethods();

		int x = linkInfos == null ? 0 : linkInfos.length;
		if (x < methods.length) {
			// oafinder is to get from one OAObj/Hub to another, not a property/etc
			throw new RuntimeException("propertyPath " + strPropertyPath + " must end in an OAObject/Hub");
		}

		cascades = new OACascade[linkInfos.length];
		for (int i = 0; i < linkInfos.length; i++) {
			cascades[i] = new OACascade();
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(c);
		liRecursiveRoot = oi.getRecursiveLinkInfo(OALinkInfo.MANY);

		if (liRecursiveRoot != null && linkInfos != null && linkInfos.length > 0) {
			if (linkInfos[0].getType() == OALinkInfo.ONE && linkInfos[0].getReverseLinkInfo().getType() == OALinkInfo.MANY
					&& !linkInfos[0].getReverseLinkInfo().getRecursive()) {
				liRecursiveRoot = null;
			}
		}

		// match filters
		String[] names = propertyPath.getFilterNames();
		Object[][] values = propertyPath.getFilterParamValues();
		Constructor[] constructors = propertyPath.getFilterConstructors(); // (hub, hub, [params ...])

		x = names.length;
		for (int i = 0; i < x; i++) {
			if (names[i] == null) {
				continue;
			}
			if (constructors[i] == null) {
				continue;
			}
			try {
				HubFilter hubFilter = createHubFilter(names[i]);
				if (hubFilter == null) {

					// hubFilter constructor that PP finds needs to have 2 hub params as first 2 params
					int xx = values[i] == null ? 0 : values[i].length;
					Object[] objs = new Object[2 + xx];
					objs[0] = null;
					objs[1] = null;
					if (xx > 0) {
						System.arraycopy(values[i], 0, objs, 2, xx);
					}

					hubFilter = ((CustomHubFilter) constructors[i].newInstance(objs)).getHubFilter();
				}
				if (filter == null) {
					filter = hubFilter;
				} else {
					filter = new OAAndFilter(filter, hubFilter);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Filter " + names[i] + " can not be created", e);
			}
		}
	}

	private void performFind(F obj) {
		if (obj == null) {
			return;
		}
		find(obj, 0);
	}

	protected void find(Object obj, int pos) {
		if (obj == null || bStop) {
			return;
		}
		if (pos > 20) {
			return;
		}
		try {
			if (bEnableStack) {
				push(obj, pos);
			}
			_find(obj, pos);
		} finally {
			if (bEnableStack) {
				pop();
			}
		}
	}

	private void _find(Object obj, int pos) {
		if (obj == null) {
			return;
		}
		if (obj instanceof Hub) {
			for (Object objx : (Hub) obj) {
				find(objx, pos);
				if (bStop) {
					break;
				}
			}
			return;
		}

		if (!(obj instanceof OAObject)) {
			return;
		}

		if (cascade != null) {
			boolean b = cascade.wasCascaded((OAObject) obj, true);
			if (b) {
				return;
			}
		}

		if (pos > 0 && cascades != null) {
			boolean b = cascades[pos - 1].wasCascaded((OAObject) obj, true);
			if (b) {
				return;
			}
		}

		if (linkInfos == null || pos >= linkInfos.length) {
			boolean bIsUsed;
			if (filter != null && !bStop) {
				bIsUsed = filter.isUsed(obj);
			} else {
				bIsUsed = true;
			}
			bIsUsed = !bStop && bIsUsed && isUsed((T) obj);

			if (bIsUsed) {
				onFound((T) obj);
			}
			if (bStop) {
				return;
			}
		}

		// check if recursive
		if (pos == 0) {
			// see if root object is recursive
			if (bEnableRecursiveRoot && liRecursiveRoot != null) {
				if (getUseOnlyLoadedData()) {
					if (!liRecursiveRoot.isLoaded(obj)) {
						onDataNotFound();
						return;
					}
					/* 20180606 all will be sorted, since they are same li
					if ((obj instanceof OAObject) && getNeedsToBeSorted((OAObject) obj, liRecursiveRoot)) {
					    onDataNotFound();
					    return;
					}
					*/
				}
				Object objx = liRecursiveRoot.getValue(obj);
				find(objx, pos); // go up a level to then go through hub
				if (bStop) {
					return;
				}
			}
		} else if (recursiveLinkInfos != null && pos <= recursiveLinkInfos.length) {
			if (recursiveLinkInfos[pos - 1] != null) {
				if (getUseOnlyLoadedData()) {
					if (!recursiveLinkInfos[pos - 1].isLoaded(obj)) {
						onDataNotFound();
						return;
					}
					/* 20180606 all will be sorted, since they are same li
					if ((obj instanceof OAObject) && getNeedsToBeSorted((OAObject) obj, recursiveLinkInfos[pos - 1])) {
					    onDataNotFound();
					    return;
					}
					*/
				}
				Object objx = recursiveLinkInfos[pos - 1].getValue(obj);
				find(objx, pos);
				if (bStop) {
					return;
				}
			}
		}

		if (linkInfos != null && pos < linkInfos.length) {
			if (getUseOnlyLoadedData()) {
				// 20180713 check if it needs to be sorted, and if a sortListener already created
				boolean b = linkInfos[pos].isLoaded(obj);
				if (b && linkInfos[pos].getType() == OALinkInfo.TYPE_MANY) {
					if (OAString.isNotEmpty(linkInfos[pos].getSortProperty())) {
						final OAGraphInternal og = (OAGraphInternal) OARuntime.graph((OAObject) obj);
						Object objx;
						if (linkInfos[pos].getCalculated()) {
							objx = linkInfos[pos].getValue((OAObject) obj);
						} else {
							objx = og.objectsInternal().callObjectPropertyGetProperty((OAObject) obj, linkInfos[pos].getName());
						}
						if (objx instanceof Hub) {
							Hub h = (Hub) objx;
							if (og.hubsInternal().callHubSortGetSortListener(h) == null && og.hubsInternal().callHubSequenceGetAutoSequence(h) == null) {
								final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
								OAThreadLocal tl = srvcOAThreadLocal.getThreadLocal(true);
								if (tl.cntGetSiblingCalled > 1) {
									b = false;
								}
							}
						}
					}
				}

				if (!b) {
					onDataNotFound();
					return;
				}
				/* 20180606 might need to put this back in
				if ((obj instanceof OAObject) && getNeedsToBeSorted((OAObject) obj, linkInfos[pos])) {
				    onDataNotFound();
				    return;
				}
				*/
			}
			Object objx = linkInfos[pos].getValue(obj);
			find(objx, pos + 1);
			if (bStop) {
				return;
			}
		}
	}

	private boolean getNeedsToBeSorted(OAObject obj, OALinkInfo li) {
		if (obj == null || li == null) {
			return false;
		}
		if (li.type != OALinkInfo.MANY) {
			return false;
		}
		if (OAString.isEmpty(li.getSortProperty())) {
			return false;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph((OAObject) obj);
		Hub hx;
		if (li.getCalculated()) {
			hx = (Hub) li.getValue((OAObject) obj);
		} else {
			hx = (Hub) og.objectsInternal(). callObjectPropertyGetProperty((OAObject) obj, li.name, false, true);
		}
		if (hx == null || (og.hubsInternal().callHubSortGetSortListener(hx) == null && og.hubsInternal().callHubSequenceGetAutoSequence(hx) == null)) {
			return true;
		}
		return false;
	}

	/**
	 * This will be called to create a filter that is in the propertyPaths.
	 *
	 * @param name name of the filter in the propertyPath
	 */
	protected HubFilter createHubFilter(String name) {
		return null;
	}

	/**
	 * Called for all objects that are found, and pass the filter.
	 *
	 * @return true (default) to include in arrayList results, false to skip.
	 */
	protected boolean isUsed(T obj) {
		return true;
	}

	/**
	 * This will have the internal stack updated when a find is being performed.
	 *
	 * @param b, default is false
	 */
	public void setEnabledStack(boolean b) {
		bEnableStack = b;
	}

	// used to keep track of the objects in the stack
	static class StackValue {
		Object obj;
		int pos;

		StackValue(Object obj, int pos) {
			this.obj = obj;
			this.pos = pos;
		}
	}

	private void push(Object obj, int pos) {
		StackValue sv = new StackValue(obj, pos);
		push(sv);
	}

	private void push(StackValue sv) {
		if (sv == null) {
			return;
		}
		int x = stack.length;
		if (stackPos == x) {
			StackValue[] temp = new StackValue[x + 10];
			System.arraycopy(stack, 0, temp, 0, x);
			stack = temp;
		}
		stack[stackPos++] = sv;
	}

	private StackValue pop() {
		if (stackPos == 0) {
			return null;
		}
		StackValue sv = stack[--stackPos];
		stack[stackPos] = null;
		return sv;
	}

	/**
	 * The objects that are in the current stack. This can be used when overwriting the onFound(..) to know the object path.
	 *
	 * @see #setEnabledStack(boolean) to enable this information.
	 */
	public Object[] getStackObjects() {
		Object[] objs = new Object[stackPos];
		for (int i = 0; i < stackPos; i++) {
			objs[i] = stack[i].obj;
		}
		return objs;
	}

	/**
	 * The property name of the objects that are in the current stack.
	 *
	 * @see #setEnabledStack(boolean) to enable this information.
	 */
	public String[] getStackPropertyNames() {
		String[] ss = new String[stackPos];
		for (int i = 0; i < stackPos; i++) {
			String methodName;
			if (stack[i].pos == 0) {
				methodName = "[root]";
			} else if (stack[i].pos <= linkInfos.length) {
				methodName = linkInfos[stack[i].pos - 1].getName();
			} else {
				methodName = methods[stack[i - 1].pos].getName();
			}
			ss[i] = methodName;
		}
		return ss;
	}

	public void addBetweenFilter(String pp, Object val1, Object val2) {
		addFilter(new OABetweenFilter(pp, val1, val2));
	}

	public void addBetweenOrEqualFilter(String pp, Object val1, Object val2) {
		addFilter(new OABetweenOrEqualFilter(pp, val1, val2));
	}

	public void addEmptyFilter(String pp) {
		addFilter(new OAEmptyFilter(pp));
	}

	public void addNotEmptyFilter(String pp) {
		addFilter(new OANotEmptyFilter(pp));
	}

	public void addQueryFilter(Class<F> c, String pp) {
		addFilter(new OAQueryFilter(c, pp));
	}

	public void addQueryFilter(Class<F> c, String query, Object[] args) {
		addFilter(new OAQueryFilter(c, query, args));
	}

	public void addEqualFilter(String pp, Object val) {
		addFilter(new OAEqualFilter(pp, val));
	}

	public void addEqualFilter(String pp, Object matchValue, boolean bIgnoreCase) {
		OAEqualFilter f = new OAEqualFilter(pp, matchValue);
		f.setIgnoreCase(bIgnoreCase);
		addFilter(f);
	}

    public void addEqualFilter(String pp, Object matchValue, int decimalPlaces) {
        OAEqualFilter f = new OAEqualFilter(pp, matchValue, decimalPlaces);
        addFilter(f);
    }
	
	public void addTrueFilter(String pp) {
		addFilter(new OAEqualFilter(pp, Boolean.TRUE));
	}

	public void addFalseFilter(String pp) {
		addFilter(new OAEqualFilter(pp, Boolean.FALSE));
	}

	public void addNullFilter(String pp) {
		addFilter(new OANullFilter(pp));
	}

	public void addNotNullFilter(String pp) {
		addFilter(new OANotNullFilter(pp));
	}

	public void addGreaterFilter(String pp, Object val) {
		addFilter(new OAGreaterFilter(pp, val));
	}

	public void addGreaterOrEqualFilter(String pp, Object val) {
		addFilter(new OAGreaterOrEqualFilter(pp, val));
	}

	public void addLessFilter(String pp, Object val) {
		addFilter(new OALessFilter(pp, val));
	}

	public void addLessOrEqualFilter(String pp, Object val) {
		addFilter(new OALessOrEqualFilter(pp, val));
	}

	public void addLikeFilter(String pp, Object val) {
		addFilter(new OALikeFilter(pp, val));
	}

	public void addNotLikeFilter(String pp, Object val) {
		addFilter(new OANotLikeFilter(pp, val));
	}

	public void addOrFilter(OAFilter f1, OAFilter f2) {
		OAOrFilter f = new OAOrFilter(f1, f2);
		addFilter(f);
	}

	/**
	 * This will create an Or with the existing filter and the next filter that is added.
	 */
	public void addOrFilter() {
		bAddOrFilter = true;
		bAddAndFilter = false;
	}

	/**
	 * This will create an And with the existing filter and the next filter that is added.
	 */
	public void addAndFilter() {
		bAddAndFilter = true;
		bAddOrFilter = false;
	}

	public void setCascade(OACascade cascade) {
		this.cascade = cascade;
	}
}

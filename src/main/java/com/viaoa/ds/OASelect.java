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
package com.viaoa.ds;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import com.viaoa.object.*;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAComparator;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;
import com.viaoa.hub.*;

/**
   Helper Class used for submitting and managing queries for any OADataSource. 
   This is used by Hub.select() methods. 
   All queries are based on object names, property names, and property paths.
   <p>
   A <b>property path</b> is a dot (".") separated list of property names that are used to navigate 
   from a root Class to a property value.  To go from object to object, reference property names are used.
  
   <p>
   An OAFinder can be used to act as the datasource.
   <p>
   An OAFilter can be used to further filter the results.
  
   <p>
   Queries
   <ul>
   <li>All property names and connectors names are case insensitive.
   <li>Can use the following connectors "AND", "&amp;&amp;", "||", "OR", "(", ")"
   <li>Can use "=", "==", "!=", "&lt;", "&lt;=", "&gt;", "&gt;=", "LIKE", "%" (wildcard), "null" (any case)
   <li>use "PASS[" to begin a passthru part of the query, and "]THRU" to end it.
   <li>"ASC" ascending, "DESC" descending can be used with Order By properties.
   </ul>

   <pre>
        OASelect select = new OASelect();
        String query = OAConverter.toDataSourceString("dept", dept); // converts to dept.Id = 'MIS'
        String fname = "John";
        query += " &amp;&amp; (dept.manager.lastName like 'Jones%'";
        query += " || (dept.manager.firstName == " + OAConvert.toDataSourceString(fname) + ")";
        select.setWhere(query);
        select.setOrder("dept.name, Emp.LastName DESC, emp.firstName");
        select.setPassthru(false);    // needs to be converted to native query language
        select.setCountFirst(false);  // dont need count
        select.setMax(250);           // only select first 250 objects.  (default=0 ALL)
        select.setFetchAmount(40);    // amount of objects to read at a time (default=45)

        // or use params for where query
		query = "dept = ? &amp;&amp; dept.manager.lastName like ? || dept.manager.firstname = ?";
		Object[] params = new Object[] {dept, "Jones%", fname};
        select.setWhere(query);
		select.setParams(params);
   </pre>
    <p>
    For more information about this package, see <a href="package-summary.html#package_description">documentation</a>.
*/
public class OASelect<TYPE extends OAObject> implements Iterable<TYPE> {
    static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(OASelect.class.getName());
    
    private static final AtomicInteger aiId = new AtomicInteger(); 
    private final int id;
    
    protected Class clazz;
    protected String where;
    protected String order;
    protected boolean bPassthru;
    protected boolean bAppend;
    protected boolean bRewind = true; // set back to first object
    

    /**
     * Select based on a "root" object/hub.ao
     *  
     *  examples: 
     *  if whereObject is Dept and this.class is Emp, then "emps"
     *  if whereObject is Dept and this.class is Order, then "emps.orders"
     */
    protected OAObject whereObject;
    protected Hub whereHub;
    protected String whereObjectPropertyPath; 
    
    protected int max;  // max amount of objects to load
    protected boolean bCountFirst; // count before selecting
    protected int amountRead=-1;
    protected int amountCount=-1;
    protected Object[] params;
    public volatile transient OADataSourceIterator query;

    public static final int defalutFetchAmount = 45;
    protected int fetchAmount=defalutFetchAmount;  // used by Hub to know how many to read at a time
    protected boolean bCancelled;
    protected boolean bHasBeenStarted;
    protected boolean bUseFinder;
    protected volatile long lastReadTime; // used with for determining timeout
    protected OAFilter<TYPE> oaFilter;  // this will be used by OASelect to filter iterator returned values
    protected OAFilter<TYPE> dsFilter;  // this will be sent to DataSource, which will use it if it does not support queries
    protected OAFinder<?, TYPE> finder; // will be used instead of calling datasource
    protected Hub<TYPE> hubSearch;      // hub used to search from, instead of using DataSource
    private boolean bDirty;  // data should always be loaded from datasource  
    private volatile boolean bIsSelectingNow;

    
    
    /** Create a new OASelect that is not initialzed. */
    public OASelect() {
        this.id = aiId.incrementAndGet();
    }

    /** Create a new OASelect that is initialzed to query Objects for a Class. */
    public OASelect(Class<TYPE> c) {
        this();
        setSelectClass(c);
    }

    /** 
        Create a new OASelect that is initialzed to query Objects for a Class for a passthru query. 
    */
    public OASelect(Class<TYPE> c, boolean passthru, String where, String order) {
        this();
        setSelectClass(c);
        setPassthru(passthru);
        setWhere(where);
        setOrder(order);
    }

    /** 
        Create a new OASelect that is initialized to query Objects for a Class. 
    */
    public OASelect(Class<TYPE> c, String where, String order) {
        this();
        setSelectClass(c);
        setWhere(where);
        setOrder(order);
    }

    /** 
    Create a new OASelect that is initialized to query Objects for a Class. 
	*/
	public OASelect(Class<TYPE> c, String where, Object[] params, String order) {
        this();
	    setSelectClass(c);
	    setWhere(where);
	    setParams(params);
	    setOrder(order);
	}

    
    /** 
        Create a new OASelect that is initialzed to query Objects for a Class. 
    */
    public OASelect(Class<TYPE> c, OAObject whereObject, String order) {
        this();
        setWhereObject(whereObject);
        setOrder(order);
    }

    public int getId() {
        return id;
    }
    
    
    /**
     * Values used to replace '?' in where clause.
     * @param params list of values to replace '?' in where clause.
     */
    public void setParams(Object[] params) {
    	this.params = params;
    }
    public Object[] getParams() {
    	return this.params;
    }
    
    // 20180726
    public void add(String whereClause, Object[] params) {
        this.where = OAString.concat(this.where, whereClause, " AND "); 
        this.params = OAArray.add(Object.class, this.params, params);
    }
    

    public void setSearchHub(Hub<TYPE> hub) {
        this.hubSearch = hub;
    }
    public Hub<TYPE> getSearchHub() {
        return this.hubSearch;
    }
    
    /** 
        Calls reset(false)
    */
    public void reset() {
        reset(false);
    }
    /** 
        Reset so that select can be used again
        @param bClearOutValues if true then clears where, order, whereObject 
    */
    public void reset(boolean bClearOutValues) {
        closeQuery();
        if (bClearOutValues) {
            where = null;
            order = null;
            whereObject = null;
        }
        amountCount = -1;
        amountRead = -1;
        bCancelled = false;
        bHasBeenStarted = false;
        lastReadTime = 0;
    }

    
    public void setWhereObject(OAObject whereObject, String pp) {
        this.whereObject = whereObject;
        this.whereObjectPropertyPath = pp;
    }
    
    /**
        WhereObject is used to build a where statement that will select all objects that have
        have a reference to whereObject.
        @see #setWhereObjectPropertyPath
    */
    public void setWhereObject(OAObject whereObject) {
        this.whereObject = whereObject;
    }
    /**
        WhereObject is used to build a where statement that will select all objects that have
        have a reference to whereObject.
        @see #setWhereObjectPropertyPath
    */
    public Object getWhereObject() {
        return whereObject;
    }

    /** 
        Property name in whereObject that is used to select objects for this class.
        This is not required, but should be supplied if the whereObject has more 
        then one path to the select class.
        <p>
        example: if whereObject is Dept and class is Emp, then "emps"
    */
    public void setPropertyFromWhereObject(String propName) {
        whereObjectPropertyPath = propName;
    }
    public void setWhereObjectPropertyPath(String propName) {
        whereObjectPropertyPath = propName;
    }

    /**
        Returns property from Where Object
        @see #setWhereObjectPropertyPath
    */
    public String getPropertyFromWhereObject() {
        return whereObjectPropertyPath;
    }
    public String getWhereObjectPropertyPath() {
        return whereObjectPropertyPath;
    }

    /**
        DataSource that will be used for query/select.
    */
    public OADataSource getDataSource() {
        if (clazz == null) return null;
        OADataSource ds = OADataSource.getDataSource(clazz, getDataSourceFilter());
        return ds;
    }

    /**
        Class of objects that are being selected.
    */
    public void setSelectClass(Class c) {
        this.clazz = c;
    }
    /**
        Class of objects that are being selected.
    */
    public Class getSelectClass() {
        return clazz;
    }

    public void setWhere(String s) {
        where = s;
    }
    public void setWhere(String s, Object[] params) {
        where = s;
        setParams(params);
    }
    public void setWhere(String s, Object param) {
        where = s;
        setParams(new Object[] {params});
    }
    /**
        Where clause to use for query.  See notes at beginning of class.
    */
    public String getWhere() {
        return where;
    }

    
    public void setHasBeenSelected(boolean b) {
        this.bHasBeenStarted = b;
    }
    public boolean getHasBeenSelected() {
        return this.bHasBeenStarted;
    }


    // 20120617
    public void setHubFilter(OAFilter<TYPE> hfi) {
        this.oaFilter = hfi;
    }
    public OAFilter<TYPE> getHubFilter() {
        return this.oaFilter ;
    }
    public void setFilter(OAFilter<TYPE> hfi) {
        this.oaFilter = hfi;
    }
    public OAFilter<TYPE> getFilter() {
        return this.oaFilter ;
    }

    public void setDataSourceFilter(OAFilter<TYPE> hfi) {
        this.dsFilter = hfi;
    }
    public OAFilter<TYPE> getDataSourceFilter() {
        return this.dsFilter;
    }

    /**
     * 
     * @param finder
     */
    public void setFinder(OAFinder<?, TYPE> finder) {
        this.finder = finder;
    }
    public OAFinder<?, TYPE> getFinder() {
        return this.finder;
    }
    
    /**
        Sort order clause to use for query.  See notes at beginning of class.
    */
    public void setOrder(String s) {
        order = s;
    }
    public String getOrder() {
        return order;
    }

    public void setSortBy(String s) {
        setOrder(s);
    }
    public String getSortBy() {
        return getOrder();
    }
    
    /** 
        Flag to show if query should use OADataSource.selectPassthru() instead of OADataSource.select() 
    */
    public void setPassThru(boolean b) {
        setPassthru(b);
    }
    /** 
        Flag to show if query should use OADataSource.selectPassthru() instead of OADataSource.select() 
    */
    public void setPassthru(boolean b) {
        bPassthru = b;
    }
    /** 
        Flag to show if query should use OADataSource.selectPassthru() instead of OADataSource.select() 
    */
    public boolean getPassthru() {
        return bPassthru;
    }
    /** 
        Flag to show if query should use OADataSource.selectPassthru() instead of OADataSource.select() 
    */
    public boolean getPassThru() {
        return bPassthru;
    }

    /** 
        Flag to show if data should be append to existing collection (used by Hub).
    */
    public void setAppend(boolean b) {
        bAppend = b;
    }
    /** 
        Flag to show if data should be append to existing collection (used by Hub).
    */
    public boolean getAppend() {
        return bAppend;
    }

    /** 
        Flag to show if data should be rewound to beginning object (used by Hub). 
    */
    public void setRewind(boolean b) {
        bRewind = b;
    }
    /** 
        Flag to show if data should be rewound to beginning object (used by Hub). 
    */
    public boolean getRewind() {
        return bRewind;
    }


    /** 
        Flag have a count performed before query is executed.
    */
    public void setCountFirst(boolean b) {
        this.bCountFirst = b;
    }
    /** 
        Flag have a count performed before query is executed.
    */
    public boolean getCountFirst() {
        return bCountFirst;
    }

    /** 
        Maximum number of objects to load.  Default=0 (read all).
    */
    public void setMax(int x) {
        max = x;
    }
    /** 
        Maximum number of objects to load.  Default=0 (read all).
    */
    public int getMax() {
        return max;
    }

    /** 
        Set the amount of records to read at a time (Default = 45).
    */
    public int getFetchAmount() {
        return fetchAmount;
    }
    /** 
        Set the amount of records to read at a time (Default = 45).
    */
    public void setFetchAmount(int fa) {
        fetchAmount = Math.max(0,fa);
    }


    /** 
        Returns the amount of records that will be returned from select.  
        Calls the OADataSource.count() method.
        @see OASelect#isCounted to see if count was already preformed.
        @see OASelect#setCountFirst to have count ran before select is performed
    */
    public synchronized int getCount() {
        if (amountCount < 0) {
            OADataSource ds = getDataSource();

            if (!hasMore() && amountRead >= 0) {
                return amountRead;
            }
            else {
                if (alFinderResults != null) {
                    return alFinderResults.size();
                }
                
                if (ds == null || !ds.getSupportsPreCount()) {
                    // load all
                    return amountRead+1; /// only know that there is at least one more
                }
                else if (bPassthru) {
                    amountCount = ds.countPassthru(where, max);
                }
                else {
                    if (whereObject != null) {
                        amountCount = ds.count(clazz, whereObject, whereObjectPropertyPath, max);
                    }
                    else amountCount = ds.count(clazz, where, params, max);
                }
            }
        }
        return amountCount;
    }

    /** 
        Flag to see if a count has been executed for this query/select. 
    */
    public synchronized boolean isCounted() {
        if (amountCount != -1) return true;
        if (!bHasBeenStarted) return false;
        return (!hasMore());  // if hasMore is false, then all are loaded
    }
    /** 
        Number of objects loaded so far. 
        @see #next
    */
    public int getAmountRead() {
        return (Math.max(0,amountRead));
    }

    /** 
        Used to send a "passThru" command to OADataSource. 
    */
    public void execute(String command) {
        if (clazz == null) throw new RuntimeException("OASelect.execute() needs selectClass set");
        OADataSource ds = getDataSource();
        if (ds == null) throw new RuntimeException("OASelect.execute() cant find datasource for class "+clazz);
        ds.execute(command);
    }
    
    /**
        Used to set where and order by clauses, and then perform select.
    */
    public void select(String where, String order) {
        setWhere(where);
        setOrder(order);
        select();
    }

    /**
    Used to set where and order by clauses, and then perform select.
	*/
	public void select(String where, Object[] params, String order) {
	    setWhere(where);
	    setOrder(order);
	    setParams(params);
	    select();
	}
	    
    /**
        Used to set where clause, and then perform select.
    */
    public void select(String where) {
        setWhere(where);
        select();
    }
    /**
    Used to set where clause, and then perform select.
	*/
	public void select(String where, Object[] params) {
	    setWhere(where);
	    setParams(params);
	    select();
	}
    
    
	private ArrayList<TYPE> alFinderResults;
	private int posFinderResults;
	
    /**
        Used to perform select.
    */
    public synchronized void select() {
        lastReadTime = System.currentTimeMillis();
        _select();
        long x = System.currentTimeMillis() - lastReadTime;
        if (x > 2500) {
            OAPerformance.LOG.fine("query took "+x+"ms, class="+getSelectClass()+", where="+getWhere()+", whereObj="+getWhereObject());
        }
    }
    protected void _select() {
        if (bHasBeenStarted && !bCancelled) {
            closeQuery();  // cancel previous select
        }
        bHasBeenStarted = true;
        bCancelled = false;
        alFinderResults = null;
        posFinderResults = 0;
        amountRead = 0;
        amountCount = -1;
        bUseFinder = false;
        
        if (hubSearch != null && finder == null) {
            finder = new OAFinder(hubSearch, null);
            OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterObjectToDetail(hubSearch);
            if (li != null && !li.getRecursive()) {
                finder.setAllowRecursiveRoot(false);
            }
            bUseFinder = true;
        }
        
        if (!bUseFinder && finder != null) {
            if ((whereHub != null || whereObject != null) && OAString.isNotEmpty(whereObjectPropertyPath)) {
                OADataSource ds = getDataSource();
                bUseFinder = ds == null || !ds.supportsStorage();
            }
            else bUseFinder = true;
        }
        
        // 20140129
        if (bUseFinder) {
            OAFilter filter = new OAFilter<TYPE>() {
                @Override
                public boolean isUsed(TYPE obj) {
                    if (dsFilter != null && !dsFilter.isUsed(obj)) return false;
                    if (oaFilter != null && !oaFilter.isUsed(obj)) return false;
                    if (hubSearch != null) {
                        if (!hubSearch.contains(obj)) return false;
                    }
                    return true;
                }
            };
            OAFilter filterx = finder.getFilter();
            try {
                finder.addFilter(filter);
                alFinderResults = finder.find();
                
                // sort the array
                if (alFinderResults.size() > 0) {
                    String ord = getSortBy();
                    if (OAString.isNotEmpty(ord)) {
                        OAComparator comparator = new OAComparator(getSelectClass(),  ord, true);
                        Collections.sort(alFinderResults, comparator);
                    }
                }
            }
            finally {
                finder.setFilter(filterx);
            }
            return;
        }

    	if (clazz == null) throw new RuntimeException("OASelect.select() needs selectClass set");
        OADataSource ds = getDataSource();
        if (ds == null) {
            //throw new RuntimeException("OASelect.select() cant find datasource for class "+clazz);
            cancel();
            return;
        }

        try {
            bIsSelectingNow = true;
            OAObject whereObjx = whereObject;
            if (whereObjx == null && whereHub != null) whereObjx = (OAObject) whereHub.getAO();
            if (whereObjx != null) {
                if (bCountFirst && amountCount < 0) {
                	amountCount = ds.count(clazz, whereObjx, where, params, whereObjectPropertyPath, max);
                }
                query = ds.select(clazz, whereObjx, where, params, whereObjectPropertyPath, order, max, getDataSourceFilter(), getDirty());
            }
            else {
                if (bPassthru) {
                    if (bCountFirst && amountCount < 0) amountCount = ds.countPassthru(where, max);
                    query = ds.selectPassthru(clazz, where, order, max, getDataSourceFilter(), getDirty());
                }
                else {
                    if (bCountFirst && amountCount < 0) amountCount = ds.count(clazz, where, params, max);
                    query = ds.select(clazz, where, params, order, max, getDataSourceFilter(), getDirty());
                }
            }
            OADataSourceIterator q = query;
            if (q != null) q.hasNext();
        }
        finally {
            bIsSelectingNow = false;
        }
        // 20110407
        OASelectManager.add(this);
    }
    
    public boolean isSelectingNow() {
        return bIsSelectingNow;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        closeQuery();
    }
   
    public String getDataSourceQuery() {
        if (query == null) return null;
        return query.getQuery();
    }
    public String getDataSourceQuery2() {
        if (query == null) return null;
        return query.getQuery2();
    }
    
    /**
        Returns next object loaded from select, else null if no other objects are available.
    */
    public TYPE next()  {
        // 20120617 added hubFilter
        TYPE obj;
        for (;;) {
            obj = _next();
            if (obj == null) break;
            if (oaFilter == null || finder != null) break;
            
            // 20190130 
            OASiblingHelper siblingHelper = query == null ? null : query.getSiblingHelper(); 
            boolean bx = ((siblingHelper != null) && OAThreadLocalDelegate.addSiblingHelper(siblingHelper)); 
            try {
                if (oaFilter.isUsed(obj)) break;
            }
            finally {
                if (bx) OAThreadLocalDelegate.removeSiblingHelper(siblingHelper);
            }
        }
        return obj;
    }
    public synchronized TYPE _next()  {
        if (!bHasBeenStarted) {
            select();
        }
        
        TYPE obj = null;
        if (bUseFinder && finder != null) {
            if (alFinderResults == null) return null;
            int x = alFinderResults.size();
            if (posFinderResults >= x) {
                alFinderResults = null;
                return null;
            }
            obj = alFinderResults.get(posFinderResults++);
        }
        else {
            if (query == null) return null;
            obj = (TYPE) query.next();
            /*was
            try {
                obj = (TYPE) query.next();
            }
            catch (Exception e) {
                obj = null;
                if (query != null) {
                    LOG.log(Level.WARNING, "", e);
                }
            }
            */
        }
        if (obj == null) closeQuery();
        else {
            amountRead++;
            if (max > 0 && amountRead >= max) closeQuery();
            lastReadTime = System.currentTimeMillis();
        }

        return obj;
    }

    public synchronized boolean isCancelled() {
    	return bCancelled;
    }
    
    /**
        Cancels and releases query Iterator from OADataSource.
    */
    public synchronized void cancel() {
    	if (!bHasBeenStarted) bCancelled = true;
    	else bCancelled = (bIsSelectingNow || hasMore());
        alFinderResults = null;
    	closeQuery();
    }
    public synchronized void close() {
        cancel();
    }

    
    private void closeQuery() {
        if (query != null) {
            query.remove();
            query = null;
        }
        OASelectManager.remove(this);
        alFinderResults = null;     
    }
    
    
    /** 
        Returns true if more objects are available to be loaded. 
    */
    public synchronized boolean hasMore() {
        if (!bHasBeenStarted) {
            select();
        }
        
        if (bUseFinder && finder != null) {
            if (alFinderResults == null) return false;
            int x = alFinderResults.size();
            return (posFinderResults < x);
        }
        
        boolean b = query != null && query.hasNext();
        if (!b) {
            closeQuery();
        }
        return b;
    }
    
    public boolean isSelectAll() {
    	boolean result = false;
    	if (!bCancelled && OAString.isEmpty(getWhere()) && getFilter() == null && getFinder() == null && getMax() == 0 && getWhereObject() == null && getWhereHub() == null && getSearchHub() == null) {
			result = true;
    	}
        return result;
    }
    public synchronized boolean hasBeenStarted() {
    	return bHasBeenStarted;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public Iterator<TYPE> iterator() {
        Iterator<TYPE> iter = new Iterator<TYPE>() {
            int pos;
            Object objNext;

            @Override
            public boolean hasNext() {
                boolean b = OASelect.this.hasMore();
                return b;
            }

            @Override
            public void remove() {
            }

            @Override
            public TYPE next() {
                return OASelect.this.next();
            }
        };
        return iter;
    }

    public void setDirty(boolean b) {
        this.bDirty = b;
    }
    public boolean getDirty() {
        return this.bDirty;
    }

    
    // similiar to whereObject, uses hub.AO as the whereObject
    public void setWhereHub(Hub hubWhere, String ppFromWhereHub) {
        setWhereHub(hubWhere);
        setWhereHubPropertyPath(ppFromWhereHub);
    }

    public void setWhereHub(Hub hub) {
        this.whereHub = hub;
    }
    public Hub getWhereHub() {
        return whereHub;
    }
    public String getWhereHubPropertyPath() {
        return this.whereObjectPropertyPath;
    }
    public void setWhereHubPropertyPath(String pp) {
        this.whereObjectPropertyPath = pp;
    }
    
    
}


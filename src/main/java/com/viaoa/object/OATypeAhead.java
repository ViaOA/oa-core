/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.model.oa.VString;
import com.viaoa.template.OATemplate;
import com.viaoa.util.*;

/**
 * Provides dynamic type-ahead (auto-complete) search and display support for
 * {@link OAObject} collections and Hubs.
 * <p>
 * {@code OATypeAhead} performs real-time filtering and sorting of objects
 * based on text input, typically for use in interactive UI fields. It can
 * search in-memory lists or full Hub graphs, using property paths, templates,
 * or custom filters to determine matches and display values.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Supports both direct list and {@link com.viaoa.hub.Hub}-based searches.</li>
 *   <li>Uses {@link OAPropertyPath} to traverse object graphs for flexible matching.</li>
 *   <li>Integrates with {@link OATemplate} for formatted display and dropdown rendering.</li>
 *   <li>Thread-safe: concurrent searches are automatically canceled when superseded.</li>
 *   <li>Supports {@link OAFilter} for custom inclusion/exclusion logic.</li>
 *   <li>Prevents duplicate results using GUID tracking.</li>
 *   <li>Configurable match, display, sort, and dropdown formats.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * OATypeAhead<Address, Country> ta =
 *     new OATypeAhead<>(hubAddress, new OATypeAheadParams<>() {{
 *         finderPropertyPath = AddressPP.country();
 *         matchPropertyPath = CountryPP.name();
 *         sortValuePropertyPath = CountryPP.name();
 *         maxResults = 10;
 *     }});
 * List<Country> results = ta.search("uni");
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Thread-safe implementation using {@link ReentrantReadWriteLock}.</li>
 *   <li>Concurrent search cancellation via {@link AtomicInteger} version tracking.</li>
 *   <li>Templates ({@link OATemplate}) enable advanced display formatting.</li>
 *   <li>Does not require a DataSource; operates entirely in memory.</li>
 * </ul>
 *
 * @param <F> the root {@link OAObject} type providing search context
 * @param <T> the {@link OAObject} type being searched
 *
 * @see OAFilter
 * @see OAPropertyPath
 * @see OATemplate
 * @see com.viaoa.hub.Hub
 */
public class OATypeAhead<F extends OAObject,T extends OAObject> {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(OATypeAhead.class.getName());

    // base hub
    protected Hub<F> hub;
    protected List<T> alTo;    

    /* flag to know if searches that use a finder only use the root Hub's AO */
    private boolean bUseAOOnly;
    
    /**
     *  pp from objects F to to objects T
     *  this is not used/needed if finder is not null.
     */
    protected String finderPropertyPath;
    protected OAPropertyPath ppFinder;
    
    /** 
     * property in T to match with search text.
     */
    protected String matchPropertyPath;
    protected OAPropertyPath ppMatch;

    /**
     * pp from T used for display value
     */
    protected String displayPropertyPath;
    protected OAPropertyPath ppDisplay;
    protected String displayFormat;    
    
    /**
     * pp from T used for sorting the matched objects.
     */
    protected String sortValuePropertyPath; 
    protected OAPropertyPath ppSortValue;
    protected String sortValueFormat;    

    /**
     * pp from T used for dropdown display
     */
    protected String dropDownDisplayPropertyPath;
    protected OAPropertyPath ppDropDownDisplay;
    protected String dropDownDisplayFormat;

    
    /**
     * additional custom finder for filtering T objects
     */
    protected OAFilter<T> filter;
    
    /**
     * To class T using finderPropertyPath
     */
    private Class<T> classTo;
    
    /*
     * used to get from hub<F>.activeObj to hub<T>
     */
    protected OAFinder<F,T> finder;

    protected String searchText;
    protected String[] searchTextSplit;
    
    protected int minInputLength = -1;
    protected int maxResults;
    protected boolean showHint=false;
    
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final AtomicInteger aiSearch = new AtomicInteger(); 
    private final HashSet<Long> hsGuid = new HashSet<>();
    
    protected String matchTemplate;
    protected OATemplate templateMatch;
    protected String displayTemplate;
    protected OATemplate templateDisplay;
    protected String dropDownDisplayTemplate;
    protected OATemplate templateDropDownDisplay;

    
    /**
     * Creates a type-ahead instance using the supplied list as the source for
     * matching operations.
     *
     * @param arrayToUse the list of objects used for type-ahead lookup
     */
    public OATypeAhead(List<T> arrayToUse) {
        alTo = arrayToUse;
    }

    /**
     * Creates a type-ahead instance using the specified root hub and
     * initialization parameters. The hub provides context for finding related
     * objects, and the parameters control matching, display, and filtering
     * behavior.
     *
     * @param hub    the root hub used for searches
     * @param params the configuration parameters
     */
    public OATypeAhead(Hub<F> hub, OATypeAheadParams params) {
        if (hub == null) throw new IllegalArgumentException("hub can not be null");
        this.hub = hub;
        if (params == null) throw new IllegalArgumentException("params can not be null");
        setup(params);
    }
    
    
    /**
     * Creates a type-ahead instance backed by a hub of {@link VString}
     * objects, using each string in the supplied array as an available
     * value. This supports freeform input where entries may not be
     * restricted to the predefined list.
     *
     * @param values the array of string values
     * @return the constructed type-ahead instance
     */
    public static OATypeAhead createTypeAhead(String[] values) {
        if (values == null) values = new String[0];
        Hub<VString> hub = new Hub<>(VString.class);
        for (String s : values) {
            hub.add(new VString(s));
        }
        //hub.sort(VString.P_Value);
        
        OATypeAheadParams tap = new OATypeAheadParams();
        tap.matchPropertyPath = VString.P_Value;
        OATypeAhead<VString, VString> ta = new OATypeAhead<>(hub, tap);
        return ta;
    }
    

    
    /**
     * Helper class to enter all of the params.
     * @author vvia
     */
    public static class OATypeAheadParams<F extends OAObject,T extends OAObject> {
        public String finderPropertyPath;
        
        public String matchPropertyPath;
        public String matchTemplate;
        protected OATemplate templateMatch;

        public String displayPropertyPath; 
        public String displayFormat;    
        public String displayTemplate;
        protected OATemplate templateDisplay;
        
        public String sortValuePropertyPath; 
        public String sortValueFormat;    
        
        public String dropDownDisplayPropertyPath;
        public String dropDownDisplayFormat;
        public String dropDownDisplayTemplate;
        protected OATemplate templateDropDownDisplay;
        

        public OAFilter<T> filter;
        
        public int minInputLength = -1;
        public int maxResults;
        
        /** flag to have TA show the full value on the textfield */
        public boolean showHint=false;
        
        public boolean useAOOnly=false;
        
        /**
         * Initializes display-related parameters, selecting a default display
         * property when none is provided. This method is called during setup of
         * an {@link OATypeAheadParams} instance.
         */
        void setup() {
            if (OAString.isEmpty(displayPropertyPath)) {
                displayPropertyPath = dropDownDisplayPropertyPath;
                displayFormat = dropDownDisplayFormat;
                if (OAString.isEmpty(displayPropertyPath)) {
                    displayPropertyPath = matchPropertyPath;
                    displayFormat = null;
                }                
            }
        }
    }
    

    
    /**
     * Initializes the type-ahead instance using the supplied parameters.
     * Sets up property paths, templates, filters, and finder definitions
     * used for searching, matching, sorting, and formatting.
     *
     * @param params the configuration parameters to apply
     */
    protected void setup(OATypeAheadParams params) {
        if (params == null) return;
        params.setup();

        this.bUseAOOnly = params.useAOOnly;
        
        this.finderPropertyPath = params.finderPropertyPath;
        classTo = (Class<T>) hub.getObjectClass();
        if (OAString.isNotEmpty(finderPropertyPath)) {
            ppFinder = new OAPropertyPath<F>(hub.getObjectClass(), finderPropertyPath);
            OALinkInfo[] lis = ppFinder.getLinkInfos();
            if (lis != null && lis.length > 0) {
                classTo = lis[lis.length-1].getToClass();
            }
        }

        this.minInputLength = params.minInputLength;
        this.maxResults = params.maxResults;
        this.showHint = params.showHint;
        
        if (ppFinder != null) {
            finder = new OAFinder<F,T>(this.finderPropertyPath) {
                @Override
                protected boolean isUsed(T obj) {
                    if (filter != null) {
                        if (!filter.isUsed(obj)) return false;
                    }
                    return OATypeAhead.this.isUsed(obj);
                }
            };
            finder.setMaxFound(params.maxResults);
        }
        
        
        this.matchPropertyPath = params.matchPropertyPath;
        if (OAString.isNotEmpty(matchPropertyPath)) {
            ppMatch = new OAPropertyPath<T>(classTo, matchPropertyPath);
        }

        this.displayPropertyPath = params.displayPropertyPath;
        this.displayFormat = params.displayFormat;
        if (OAString.isNotEmpty(displayPropertyPath)) {
            ppDisplay = new OAPropertyPath<T>(classTo, displayPropertyPath);
        }
        
        this.sortValuePropertyPath = params.sortValuePropertyPath;
        this.sortValueFormat = params.sortValueFormat;
        if (OAString.isNotEmpty(sortValuePropertyPath)) {
            ppSortValue = new OAPropertyPath<T>(classTo, sortValuePropertyPath);
        }

        this.dropDownDisplayPropertyPath = params.dropDownDisplayPropertyPath;
        this.dropDownDisplayFormat = params.dropDownDisplayFormat;
        if (OAString.isNotEmpty(dropDownDisplayPropertyPath)) {
            ppDropDownDisplay = new OAPropertyPath<T>(classTo, dropDownDisplayPropertyPath);
        }

        this.filter = params.filter;
        
        if (OAString.isEmpty(dropDownDisplayPropertyPath)) {
            dropDownDisplayPropertyPath = displayPropertyPath;
            dropDownDisplayFormat = displayFormat;
        }
        
        this.matchTemplate = params.matchTemplate;
		if (OAString.isNotEmpty(matchTemplate)) {
			templateMatch = new OATemplate<>(matchTemplate);
		}
		this.displayTemplate = params.displayTemplate;
		if (OAString.isNotEmpty(displayTemplate)) {
			templateDisplay = new OATemplate<>(displayTemplate);
		}
		this.dropDownDisplayTemplate = params.dropDownDisplayTemplate;
		if (OAString.isNotEmpty(dropDownDisplayTemplate)) {
			templateDropDownDisplay = new OATemplate<>(dropDownDisplayTemplate);
		}
    }

    /**
     * Returns the current search text used for the most recent lookup.
     *
     * @return the current search text, or null if none
     */
    public String getSearchText() {
        return this.searchText;
    }

    
    /**
     * Executes a search using the supplied text and returns all matching
     * objects. Previous search state is cleared, and concurrent searches are
     * canceled using version tracking.
     *
     * @param searchText the text to match against
     * @return the list of matching objects, or null if superseded
     */
    public List<T> search(String searchText) {
        this.searchText = searchText;
        try {
            final int cntSearch = aiSearch.incrementAndGet();
            if (finder != null) finder.stop();
            rwLock.writeLock().lock();
            hsGuid.clear();
            return _search(searchText, cntSearch);
        }
        finally {
            hsGuid.clear();
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Attempts to locate an object of type T using its identifier. Searches
     * the hub, list, or a finder depending on configuration.
     *
     * @param id the string identifier
     * @return the matching object, or null if not found
     */
    public T findObjectUsingId(String id) {
        final OAObjectKey ok = OAObjectKeyDelegate.createObjectKey(classTo, id);
        
        if (finder == null) {
            if (hub != null) {
                for (T obj : ((Hub<T>)hub)) {
                    if (OAObjectKeyDelegate.isForSameOAObject(null, obj.getObjectKey(), ok)) return obj;
                }
            }
            else if (alTo != null) {
                for (T obj : alTo) {
                    if (OAObjectKeyDelegate.isForSameOAObject(null, obj.getObjectKey(), ok)) return obj;
                }
            }
        }
        else {
            OAFinder<F, T> finder2 = new OAFinder<F,T>(this.finderPropertyPath) {
                @Override
                protected boolean isUsed(T obj) {
                    return OAObjectKeyDelegate.isForSameOAObject(null, obj.getObjectKey(), ok);
                }
            };
                
            if (bUseAOOnly) {
                return finder2.findFirst();
            }
            else {
                return finder.findFirst(hub);
            }
        }
        return null;
    }
    
    /**
     * Internal search implementation that performs the actual filtering and
     * optional sorting of results. Abort occurs when superseded by a newer
     * search.
     *
     * @param searchText the text to match against
     * @param cntSearch  the version stamp of this search
     * @return the matching objects, or null if canceled
     */
    protected List<T> _search(String searchText, final int cntSearch) {
        if (cntSearch != aiSearch.get()) return null;
        if (searchText == null) {
            searchTextSplit = null;            
        }
        else {
            String s = searchText.trim().toUpperCase();
            searchTextSplit = s.split(" ");
        }
        
        List<T> alToFound;
        
        if (finder == null) {
            alToFound = new ArrayList<T>();
            if (hub != null) {
                for (T obj : ((Hub<T>)hub)) {
                    if (cntSearch != aiSearch.get()) return null;
                    if (isUsed(obj)) {
                        alToFound.add(obj);
                        if (maxResults > 0 && alToFound.size() >= maxResults) break;
                    }
                }
            }
            else if (alTo != null) {
                for (T obj : alTo) {
                    if (cntSearch != aiSearch.get()) return null;
                    if (isUsed(obj)) {
                        alToFound.add(obj);
                        if (maxResults > 0 && alToFound.size() >= maxResults) break;
                    }
                }
            }
        }
        else {
            if (bUseAOOnly) {
                OAObject objFrom = hub.getAO();
                if (objFrom == null) return null;
                alToFound = finder.find(((F)objFrom));
            }
            else {
                alToFound = finder.find(hub);
            }
        }
        
        if (cntSearch != aiSearch.get()) return null;
        // sort     
        if (ppSortValue != null) {
            Collections.sort(alToFound, new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    String s1 = OATypeAhead.this.getSortValue(o1);
                    String s2 = OATypeAhead.this.getSortValue(o2);
                    
                    int x = OAString.compare(s1, s2);
                    return x;
                }
            });
        }
        return alToFound;
    }

    /**
     * Returns the minimum number of characters required before a search is
     * performed.
     *
     * @return the minimum input length
     */
    public int getMinimumInputLength() {
        return minInputLength;
    }

    /**
     * Sets the minimum number of characters required before performing a
     * search.
     *
     * @param x the minimum length
     */
    public void setMinimumInputLength(int x) {
        this.minInputLength = x;
    }

    /**
     * Returns the maximum number of results returned by a search.
     *
     * @return the result limit
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Sets the maximum number of results that will be returned by a search.
     *
     * @param x the maximum number of results
     */
    public void setMaxResults(int x) {
        this.maxResults = x;
    }
    
    /**
     * Enables display of the full value as a hint in the associated
     * text field.
     *
     * @param b ignored; enabling is unconditional
     */
    public void setShowHint(boolean b) {
        this.showHint = true;
    }

    /**
     * Returns whether the hint display is enabled.
     *
     * @return true if the hint should be shown
     */
    public boolean getShowHint() {
        return this.showHint;
    }

    /**
     * Determines whether the supplied object should be included in search
     * results, applying duplicate suppression based on GUID.
     *
     * @param obj the object to evaluate
     * @return true if the object is accepted
     */
    protected boolean isUsed(T obj) {
        boolean b = _isUsed(obj);
        if (b) {
            if (hsGuid.contains(obj.getGuid())) b = false;
            else hsGuid.add(obj.getGuid());
        }
        return b;
    }

    /**
     * Retrieves the value used for matching the search text against the
     * supplied object. Uses templates, property paths, or conversion to
     * construct the comparison value.
     *
     * @param obj the object to evaluate
     * @return the value used for matching
     */
    protected String getMatchValue(T obj) {
        Object objCompare;
        
        if (templateMatch != null) {
        	String s = templateMatch.process(obj);
        	return s;
        }
        
        if (ppMatch != null) {
            objCompare = ppMatch.getValue(obj);
        }
        else objCompare = obj;
        
        String str = OAConv.toString(objCompare);
        return str;
    }
        
    /**
     * Evaluates whether the object is a match based on its derived match
     * value and the current search text.
     *
     * @param obj the object to test
     * @return true if the object matches
     */
    protected boolean _isUsed(T obj) {
        String str = getMatchValue(obj); 
        boolean b = isUsed(obj, str, getSearchText(), searchTextSplit);
        return b;
    }
    
    /**
     * Determines whether the supplied object matches all components of the
     * search text. Comparison is case-insensitive.
     *
     * @param obj              the object being tested
     * @param objSearchValue   the comparison value for the object
     * @param searchText       the raw search text
     * @param searchTextSplit  the tokenized search text
     * @return true if the object matches
     */
    protected boolean isUsed(T obj, String objSearchValue, String searchText, String[] searchTextSplit) {
        // searchText is included in case this method is overwritten
        if (objSearchValue != null) objSearchValue = objSearchValue.toUpperCase();
        
        if (searchTextSplit == null || searchTextSplit.length == 0) {
            return true; // OAString.isEmpty(objSearchValue);
        }
        if (OAString.isEmpty(objSearchValue)) return false;
        
        for (String s : searchTextSplit) {
            if (objSearchValue.indexOf(s.toUpperCase()) < 0) return false;
        }
        return true;
    }
    
    
    /**
     * Returns the formatted display value for the supplied object using
     * templates, property paths, or converters.
     *
     * @param obj the object to format
     * @return the display value
     */
    public String getDisplayValue(T obj) {
        String s;

        if (templateDisplay != null) {
        	s = templateDisplay.process(obj);
        	return s;
        }
        
        if (ppDisplay != null) {
            s = ppDisplay.getValueAsString(null, obj, displayFormat);
        }
        else {
            s = OAConverter.toString(obj, displayFormat);
        }
        return s;
    }
    
    
    /**
     * Returns the dropdown display value for the supplied object using
     * templates, property paths, or converters.
     *
     * @param obj the object to format
     * @return the dropdown display value
     */
    public String getDropDownDisplayValue(T obj) {
        String s;
    
        if (templateDropDownDisplay != null) {
        	s = templateDropDownDisplay.process(obj);
        	return s;
        }
        
        
        if (ppDropDownDisplay != null) {
            s = ppDropDownDisplay.getValueAsString(null, obj, dropDownDisplayFormat);
        }
        else {
            s = OAConverter.toString(obj, dropDownDisplayFormat);
        }
        return s;
    }
    
    /**
     * Returns the value used for sorting the supplied object in search
     * results. Uses templates, property paths, or converters, and
     * returns the value in uppercase when available.
     *
     * @param obj the object to evaluate
     * @return the sort value for the object
     */
    public String getSortValue(T obj) {
        String s;
        if (ppSortValue != null) {
            s = ppSortValue.getValueAsString(null, obj, sortValueFormat);
        }
        else {
            s = OAConverter.toString(obj, sortValueFormat);
        }
        if (s != null) s = s.toUpperCase();
        return s;
    }

    /**
     * Returns the target class associated with the type-ahead lookup.
     *
     * @return the target class for objects being searched
     */
    public Class getToClass() {
        return classTo;
    }

    /**
     * Returns the hub used as the root search context, or null if the
     * type-ahead instance is list-based.
     *
     * @return the associated hub, or null
     */
    public Hub getHub() {
        return hub;
    }
}


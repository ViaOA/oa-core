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
package com.viaoa.datasource.objectcache;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.viaoa.comm.io.OAObjectInputStream;
import com.viaoa.datasource.*;
import com.viaoa.datasource.autonumber.OADataSourceAuto;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.graph.object.OAObjectKeyService;
import com.viaoa.graph.object.OAObjectPropertyService;
import com.viaoa.hub.Hub;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.util.*;

/**
 * In-memory implementation of {@link com.viaoa.datasource.OADataSource}
 * backed by the OA object cache.
 * <p>
 * {@code OADataSourceObjectCache} allows all OAObjects of each class to be
 * stored, queried, and serialized directly in memory without a database.
 * It is primarily used for testing, client-side caching, or fully in-memory
 * applications.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Thread-safe storage using {@link java.util.concurrent.ConcurrentHashMap}
 *       and {@link java.util.concurrent.locks.ReentrantReadWriteLock}.</li>
 *   <li>Supports {@link com.viaoa.filter.OAQueryFilter}, {@link com.viaoa.filter.OAAndFilter},
 *       and hub/property-path based selection.</li>
 *   <li>Automatic ID assignment via {@link com.viaoa.datasource.autonumber.OADataSourceAuto}.</li>
 *   <li>Persistent save/load using compressed serialization streams.</li>
 *   <li>Full CRUD operations with integration to {@link com.viaoa.object.OAObjectCacheDelegate}.</li>
 * </ul>
 *
 * Typical usage:
 * <pre>{@code
 * OADataSourceObjectCache ds = new OADataSourceObjectCache();
 * ds.insert(myObject);
 * ds.saveToStorageFile(new File("backup.oacache"), null);
 * ds.loadFromStorageFile(new File("backup.oacache"));
 * }</pre>
 *
 */
public class OADataSourceObjectCache extends OADataSourceAuto {
    private static final Logger LOG = OALogger.getLogger(OADataSourceObjectCache.class);

    /**
     * Thread-safe map storing the in-memory object sets for each OAObject class.
     * Each key is a class type, and each value is the set of its instantiated
     * objects currently held in the cache.
     */
    private final ConcurrentHashMap<Class, Set> hmClass = new ConcurrentHashMap<>();
    
    /**
     * Read/write lock protecting modifications to the in-memory object sets
     * and ensuring thread-safe access during persistence operations.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new object-cache data source and registers it as the active
     * data source. Delegates to {@link #OADataSourceObjectCache(boolean)} with
     * {@code true}.
     */
    public OADataSourceObjectCache() {
        this(true);
    }

    /**
     * Creates a new object-cache data source with optional registration
     * behavior. Delegates to
     * {@link #OADataSourceObjectCache(Hub, boolean, boolean)} with a null hub
     * and {@code bMakeLastDataSource = true}.
     *
     * @param bRegister whether this data source should be registered
     */
    public OADataSourceObjectCache(boolean bRegister) {
        this(null, bRegister, true);
    }

    /**
     * Creates a new object-cache data source with explicit registration and
     * ordering flags. Delegates to the full constructor with a null hub.
     *
     * @param bRegister whether this data source should be registered
     * @param bMakeLastDataSource whether this instance should become the final
     *                            data source in the chain
     */
    public OADataSourceObjectCache(boolean bRegister, boolean bMakeLastDataSource) {
        this(null, bRegister, bMakeLastDataSource);
    }

    /**
     * Full constructor allowing specification of the hub used for autonumber
     * operations as well as registration and ordering flags.
     *
     * @param hubNextNumber hub used for autonumbering operations
     * @param bRegister whether to register this data source
     * @param bMakeLastDataSource whether this instance should be last in the
     *                            data-source chain
     */
    public OADataSourceObjectCache(Hub hubNextNumber, boolean bRegister, boolean bMakeLastDataSource) {
        super(hubNextNumber, bRegister, bMakeLastDataSource);
    }

    /**
     * Selects objects from the in-memory cache matching the supplied filters,
     * query expressions, and optional ordering. Query text is converted to
     * {@link OAQueryFilter} instances, merged via {@link OAAndFilter}, and
     * applied to the object sets. If a {@code whereObject} and property path
     * are supplied, the method resolves the referenced objects or collections
     * and returns a list-based iterator. Otherwise, an {@link ObjectCacheIterator}
     * is used.
     *
     * @param selectClass the class of objects to search
     * @param queryWhere where-clause expression used to create a filter
     * @param params parameters for the where-clause
     * @param queryOrder property-path ordering expression
     * @param whereObject reference object used for property-path based selection
     * @param propertyFromWhereObject property or property path from the reference object
     * @param extraWhere additional query filter expression
     * @param max maximum number of results, or zero for unlimited
     * @param filterx optional filter applied before query evaluation
     * @param bDirty whether to include dirty or uncommitted objects
     * @return iterator over selected objects
     */
    @Override
    public OADataSourceIterator select(final Class selectClass, String queryWhere, Object[] params, String queryOrder, OAObject whereObject, String propertyFromWhereObject,
        String extraWhere, int max, OAFilter filterx, boolean bDirty) {

        if (extraWhere != null && OAString.isNotEmpty(extraWhere.trim())) {
            try {
                OAFilter filter2 = new OAQueryFilter(selectClass, extraWhere, null);
                if (filterx == null) {
                    filterx = filter2;
                }
                else {
                    filterx = new OAAndFilter(filterx, filter2);
                }
            }
            catch (Exception e) {
                throw new RuntimeException("query parsing failed", e);
            }
        }

        if (!OAString.isEmpty(queryWhere)) {
            try {
                OAFilter filter2 = new OAQueryFilter(selectClass, queryWhere, params);
                if (filterx == null) {
                    filterx = filter2;
                }
                else {
                    filterx = new OAAndFilter(filterx, filter2);
                }
            }
            catch (Exception e) {
                throw new RuntimeException("query parsing failed", e);
            }
        }

        if (whereObject != null && OAStr.isNotEmpty(propertyFromWhereObject)) {
            // 20240123
        	final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(whereObject).objects().getOAObjectInfoService();
            OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(whereObject.getClass());
            OALinkInfo li = oi.getLinkInfo(propertyFromWhereObject);

            if (li == null) {
                // check to see if propertyFromWhereObject is a propertyPath. 
                //   If so, then add to the query and re-select
                OAPropertyPath pp = new OAPropertyPath(whereObject.getClass(), propertyFromWhereObject);

                OALinkInfo[] lis = pp.getLinkInfos();
                if (lis != null && lis.length > 0) {
                    for (int i = 0; i < lis.length; i++) {
                        if (lis[i].getType() != OALinkInfo.ONE) {
                            break;
                        }
                        Object objx = lis[i].getValue(whereObject);
                        whereObject = (OAObject) objx;

                        if (whereObject == null) {
                            return new OADataSourceEmptyIterator();
                        }
                        // shorten pp
                        int pos = propertyFromWhereObject.indexOf('.');
                        int pos2 = propertyFromWhereObject.indexOf(')');
                        if (pos < pos2) {
                            pos = propertyFromWhereObject.indexOf('.', pos2);
                        }
                        propertyFromWhereObject = propertyFromWhereObject.substring(pos + 1);
                        pp = new OAPropertyPath(whereObject.getClass(), propertyFromWhereObject);
                    }
                }
                else {
                    throw new RuntimeException("whereObject's propertyFromWhereObject is not a valid link, whereObject=" + whereObject
                            + ", propertyFromWhereObject=" + propertyFromWhereObject);
                }
                
                pp = pp.getReversePropertyPath();
                if (pp == null) {
                    return new OADataSourceEmptyIterator();
                }

                if (OAString.isNotEmpty(queryWhere)) {
                    queryWhere += " AND ";
                }
                else if (queryWhere == null) {
                    queryWhere = "";
                }
                queryWhere += pp.getPropertyPath() + " == ?";
                params = OAArray.add(Object.class, params, whereObject);
                return select(selectClass, queryWhere, params, queryOrder, null, null, extraWhere, max, filterx, bDirty);
            }
            
            
            // 20250407 use reference object from oaobj.properties[]
			OAObjectPropertyService srvcOAObjectProperty = OARuntime.get().graph(whereObject).objects().getOAObjectPropertyService();
            Object objx = srvcOAObjectProperty.getProperty(whereObject, propertyFromWhereObject);
            final List al = new ArrayList();
            if (!(objx instanceof Hub)) {
                if (objx instanceof OAObject && (filterx == null || filterx.isUsed(objx))) al.add(objx);
            }
            else {
                for (Object obj2 : ((Hub)objx)) {
                    if (filterx == null || filterx.isUsed(obj2)) al.add(obj2);
                }
            }
            if (OAString.isNotEmpty(queryOrder)) {
                OAComparator comparator = new OAComparator(selectClass, queryOrder, true);
                Collections.sort(al, comparator);
            }
            OADataSourceIterator dsi = new OADataSourceListIterator(al);
            return dsi;
            
            /* was: 
            // find using selectFromPropertyPath, or equalPropertyPath
            final OALinkInfo liRev = li.getReverseLinkInfo();
            String spp = liRev.getSelectFromPropertyPath();
            if (OAStr.isNotEmpty(spp)) {
                OAPropertyPath pp = new OAPropertyPath(li.getToClass(), spp);
                pp = pp.getReversePropertyPath();
                if (pp == null) spp = null;
                else spp = pp.getPropertyPath();
            }
            else {
                spp = li.getEqualPropertyPath();
                if (OAStr.isNotEmpty(spp)) {
                    String s = liRev.getEqualPropertyPath();
                    if (OAStr.isNotEmpty(s)) {
                        OAPropertyPath pp = new OAPropertyPath(li.getToClass(), s);
                        pp = pp.getReversePropertyPath();
                        if (pp == null) spp = null;
                        else {
                            s = pp.getPropertyPath();
                            spp += "." + s;
                        }
                    }
                    else spp = null;
                }
            }

            if (OAStr.isNotEmpty(spp)) {
                final OAObject whereObjectx = whereObject;
                final OAFilter filterz = filterx;
                OAFinder f = new OAFinder(spp) {
                    protected boolean isUsed(OAObject obj) {
                        Object objx = OAObjectPropertyDelegate.getProperty(obj, liRev.getName(), false, true);
                        if (objx instanceof OAObjectKey) {
                            return objx.equals(whereObjectx.getObjectKey());
                        }
                        if (objx != whereObjectx) return false;
                        if (filterz == null) return true;
                        return filterz.isUsed(obj);
                    }
                };

                final List al = f.find(whereObject);
                if (OAString.isNotEmpty(queryOrder)) {
                    OAComparator comparator = new OAComparator(selectClass, queryOrder, true);
                    Collections.sort(al, comparator);
                }

                OADataSourceIterator dsi = new OADataSourceListIterator(al);
                return dsi;
            }
            
            // else ... need to add filter to objectCache iterator
            final OAObject whereObjectx = whereObject;
            OAFilter filter2 = new OAEqualFilter(li.getName(), whereObject) {
                public boolean isUsed(Object obj) {
                    boolean b;
                    if (obj instanceof OAObject) {
                        Object objx = OAObjectPropertyDelegate.getProperty((OAObject) obj, liRev.getName());
                        b = (whereObjectx == objx);
                        //was:  b = b || OACompare.isEqual(objx, whereObjectx);
                    }
                    else {
                        b = super.isUsed(obj);
                    }
                    return b;
                }
            };
            if (filterx == null) filterx = filter2;
            else filterx = new OAAndFilter(filterx, filter2);
            */
        }

        ObjectCacheIterator itx = new ObjectCacheIterator(selectClass, filterx);
        itx.setMax(max);

        if (OAString.isNotEmpty(queryOrder)) {
            OAComparator comparator = new OAComparator(selectClass, queryOrder, true);
            ArrayList al = new ArrayList();
            for (; itx.hasNext();) {
                al.add(itx.next());
            }
            Collections.sort(al, comparator);

            OADataSourceIterator dsi = new OADataSourceListIterator(al);
            return dsi;
        }

        return itx;
    }

    /**
     * Performs a select-operation identical to {@link #select(Class, String,
     * Object[], String, OAObject, String, String, int, OAFilter, boolean)}
     * but without parameters or reference-object processing. Equivalent to a
     * passthrough query for this in-memory data source.
     *
     * @param selectClass the class of objects to retrieve
     * @param queryWhere query expression to apply
     * @param queryOrder ordering expression
     * @param max maximum number of results
     * @param filter additional filter to apply
     * @param bDirty whether to include dirty objects
     * @return iterator over selected objects
     */
    @Override
    public OADataSourceIterator selectPassthru(Class selectClass, String queryWhere, String queryOrder, int max, OAFilter filter, boolean bDirty) {

        // 20211012 same as select for this datasource
        return select(selectClass, queryWhere, null, queryOrder, null, null, null, max, filter, bDirty);
        /*
         * was: if (!OAString.isEmpty(queryWhere)) { filter = new OAFilter() {
         * @Override public boolean isUsed(Object obj) { return false; } }; } return new ObjectCacheIterator(selectClass, filter);
         */
    }

    /**
     * Assigns an ID to the given object using the autonumber mechanism
     * provided by the superclass.
     *
     * @param obj the object requiring an ID assignment
     */
    public @Override void assignId(OAObject obj) {
        super.assignId(obj); // have autonumber handle this
    }

    /**
     * Indicates whether this data source can return row counts without
     * performing a full select. The object-cache implementation does not
     * support pre-count behavior.
     *
     * @return always {@code false}
     */
    public boolean getSupportsPreCount() {
        return false;
    }

    /**
     * Determines whether multiple data sources are currently registered.
     *
     * @return {@code true} if more than one data source exists; otherwise false
     */
    protected boolean isOtherDataSource() {
        OADataSource[] dss = OADataSource.getDataSources();
        return dss != null && dss.length > 1;
    }

    /**
     * Determines whether the specified class is supported by this in-memory
     * data source. Support depends on the presence of other data sources and
     * whether all objects for the class have been loaded.
     *
     * @param clazz the class to check
     * @param filter optional filter for conditional support
     * @return true if the class is fully supported for selection
     */
    @Override
    public boolean isClassSupported(Class clazz, OAFilter filter) {
        if (filter == null) {
            if (isOtherDataSource()) {
                return false;
            }
            return super.isClassSupported(clazz, null);
        }
        // only if all objects are loaded, or no other DS
        if (!isOtherDataSource()) {
            return true;
        }

		final OAGraph og = OARuntime.get().graph(clazz);
    	final OAObjectCacheService srvcObjectCache = og.objects().getOAObjectCacheService();
        if (srvcObjectCache.getSelectAllHub(clazz) != null) {
            return true;
        }
        return false;
    }

    /**
     * Inserts the given object into the in-memory cache after delegating to
     * the superclass for autonumber and reference handling. The object's class
     * set is updated under write-lock protection.
     *
     * @param object the object to insert
     */
    @Override
    public void insert(OAObject object) {
        super.insert(object);
        if (object == null) {
            return;
        }
        Set hs = getSet(object.getClass());
        
        try {
            lock.writeLock().lock();
            hs.add(object);
        }
        finally {
            lock.writeLock().unlock();
        }        
    }

    /**
     * Saves all cached objects—and an optional extra object—to a compressed
     * storage file. The method writes using a {@link DeflaterOutputStream}
     * and protects the write sequence with the class write-lock.
     *
     * @param file target file for serialized output
     * @param extraObject optional additional object to serialize first
     * @throws Exception if file I/O or serialization fails
     */
    public void saveToStorageFile(File file, Object extraObject) throws Exception {
        LOG.fine("saving to storage file=" + file);
        if (file == null) {
            return;
        }
        
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 64 * 1024); 

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(bos, deflater, 32 * 1024);

        ObjectOutputStream oos = new ObjectOutputStream(deflaterOutputStream);

        try {
            lock.writeLock().lock();
            _saveToStorageFile(file, oos, extraObject);
        }
        finally {
            deflaterOutputStream.finish();
            deflaterOutputStream.close();
            bos.close();
            fos.close();

            lock.writeLock().unlock();
        }
        LOG.fine("saved to storage file=" + file);
    }        
        

    /**
     * Internal implementation that writes class-grouped cached objects to the
     * provided output stream. Only supported classes are serialized. An optional
     * extra object is serialized first.
     *
     * @param file the original target file
     * @param oos output stream used for serialization
     * @param extraObject optional extra object to serialize
     * @throws Exception if serialization fails
     */
    protected void _saveToStorageFile(File file, ObjectOutputStream oos, Object extraObject) throws Exception {
        oos.writeBoolean(extraObject != null);
        if (extraObject != null) {
            OAObjectSerializer wrap = new OAObjectSerializer(extraObject, false, true);
            wrap.setIncludeBlobs(true);
            oos.writeObject(wrap);
        }
        
        for (Entry<Class, Set> entry : hmClass.entrySet()) {
            Class c = entry.getKey();
            if (!isClassSupported(c)) {
                continue;
            }

            oos.writeBoolean(true);
            oos.writeObject(c);

            Set hs = entry.getValue();
            OAObjectSerializer wrap = new OAObjectSerializer(hs, false, true);
            wrap.setIncludeBlobs(true);
            oos.writeObject(wrap);
        }
        oos.writeBoolean(false);
        oos.close();
    }

    /**
     * Loads cached objects from a compressed storage file, replacing in-memory
     * data. The method uses an {@link InflaterInputStream} and protects all
     * modifications with the write-lock.
     *
     * @param file the file to read from
     * @return true if any objects were loaded
     * @throws Exception if deserialization fails
     */
    public boolean loadFromStorageFile(final File file) throws Exception {
        LOG.fine("loading from storage file=" + file);
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            LOG.fine("storage file=" + file + " does not exist");
            return false;
        }
        
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);

        Inflater inflater = new Inflater();
        InflaterInputStream inflaterInputStream = new InflaterInputStream(fis, inflater, 32 * 1024);


        final Set<Class<?>> hsClasses = new HashSet<>();
        OAObjectInputStream ois = new OAObjectInputStream(inflaterInputStream) {
        	@Override
        	protected Object resolveObject(Object obj) throws IOException {
        		if (obj instanceof OAObject) {
        			hsClasses.add(obj.getClass());
        		}
        		return super.resolveObject(obj);
        	}
        };
        
        boolean bResult = false;
        try {
            lock.writeLock().lock();
            bResult = _loadFromStorageFile(file, ois, hsClasses);
        }
        finally {
            ois.close();
            fis.close();

            lock.writeLock().unlock();
        }
        LOG.fine("loaded storage file=" + file);
        return bResult;
    }
    
    /**
     * Internal method for deserializing cached objects and optional extra data
     * from the provided input stream. Results are merged into the existing
     * class-based sets.
     *
     * @param file the source file
     * @param ois the object-input stream used for reading
     * @return true if any objects or extra data were loaded
     * @throws Exception if deserialization fails
     */
    protected boolean _loadFromStorageFile(final File file, OAObjectInputStream ois, final Set<Class<?>> hsClasses) throws Exception {
        int cnt = 0;
        
        boolean b = ois.readBoolean();
        if (b) {
            OAObjectSerializer wrap = (OAObjectSerializer) ois.readObject();
            Object extra = wrap.getObject();
            cnt++;
        }

        for (final Class c : hsClasses) {
            final Set hs = getSet(c);
    		final OAGraph og = OARuntime.get().graph(c);
        	final OAObjectCacheService srvcObjectCache = og.objects().getOAObjectCacheService();
        	srvcObjectCache.callback(c, new OACallback() {
                @Override
                public boolean updateObject(Object obj) {
                    hs.add(obj);
                    return true;
                }
            });
        }
        
        for (;;) {
            b = ois.readBoolean();
            if (!b) {
                break;
            }
            cnt++;
            Class c = (Class) ois.readObject();
            OAObjectSerializer wrap = (OAObjectSerializer) ois.readObject();
            Set hs = (Set) wrap.getObject();
            
            Set hs2 = getSet(c);
            if (hs2 != null) hs.addAll(hs2);
            
            hmClass.put(c, hs);
        }
        return (cnt > 0);
    }

    /**
     * Retrieves the object set for the specified class, creating it if necessary.
     *
     * @param c the class whose object set is requested
     * @return the set associated with the class, or null if class is null
     */
    private Set getSet(Class c) {
        if (c == null) {
            return null;
        }
        Set hs = hmClass.computeIfAbsent(c, k -> new HashSet());
        return hs;
    }

    /**
     * Inserts the given object into the cache without processing references.
     * If the object is not already present in its class set, it is added under
     * write-lock protection.
     *
     * @param obj the object to insert without reference handling
     */
    @Override
    public void insertWithoutReferences(OAObject obj) {
        super.insertWithoutReferences(obj);
        if (obj == null) {
            return;
        }
        Set hs = getSet(obj.getClass());
        
        try {
            lock.writeLock().lock();
            if (!hs.contains(obj)) {
                hs.add(obj);
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes the specified object from the in-memory cache after delegating to
     * the superclass for reference cleanup. If the object's class set exists, the
     * object is removed under write-lock protection.
     *
     * @param obj the object to delete
     */
    @Override
    public void delete(OAObject obj) {
        super.delete(obj);
        if (obj == null) {
            return;
        }
        final Class c = obj.getClass();
        Set hs = (Set) hmClass.get(c);
        if (hs != null) {
            try {
                lock.writeLock().lock();
                hs.remove(obj);
            }
            finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Removes all objects of the specified class from the in-memory cache and
     * delegates to {@link OAObjectCacheDelegate#removeAllObjects(Class)} to clear
     * the global cache. The class set is cleared under write-lock protection.
     *
     * @param c the class whose objects should be removed
     */
    @Override
    public void deleteAll(Class c) {
        super.deleteAll(c);
        if (c == null) {
            return;
        }
        Set hs = (Set) hmClass.get(c);
        if (hs != null) {
            try {
                lock.writeLock().lock();
                hs.clear();
            }
            finally {
                lock.writeLock().unlock();
            }
        }
		final OAGraph og = OARuntime.get().graph(c);
    	final OAObjectCacheService srvcObjectCache = og.objects().getOAObjectCacheService();
    	srvcObjectCache.removeAllObjects(c);
    }

}

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
import com.viaoa.hub.Hub;
import com.viaoa.object.*;
import com.viaoa.util.*;

// 20140124
/**
 * OADataSource for storing objects in serialized file.
 * <p>
 *
 */
public class OADataSourceObjectCache extends OADataSourceAuto {
    private static final Logger LOG = OALogger.getLogger(OADataSourceObjectCache.class);

    private final ConcurrentHashMap<Class, Set> hmClass = new ConcurrentHashMap();
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public OADataSourceObjectCache() {
        this(true);
    }

    public OADataSourceObjectCache(boolean bRegister) {
        this(null, bRegister, true);
    }

    public OADataSourceObjectCache(boolean bRegister, boolean bMakeLastDataSource) {
        this(null, bRegister, bMakeLastDataSource);
    }

    public OADataSourceObjectCache(Hub hubNextNumber, boolean bRegister, boolean bMakeLastDataSource) {
        super(hubNextNumber, bRegister, bMakeLastDataSource);
    }

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
            OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(whereObject.getClass());
            OALinkInfo li = oi.getLinkInfo(propertyFromWhereObject);

            if (li == null) {
                // 20200219 check to see if propertyFromWhereObject is a propertyPath. 
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
            
            
            // find using equalPropertyPath, or equalPropertyPath
            final OALinkInfo liRev = li.getReverseLinkInfo();
            String spp = liRev.getSelectFromPropertyPath();
            if (OAStr.isNotEmpty(spp)) {
                OAPropertyPath pp = new OAPropertyPath(li.getToClass(), spp);
                pp = pp.getReversePropertyPath();
                spp = pp.getPropertyPath();
            }
            else {
                spp = li.getEqualPropertyPath();
                if (OAStr.isNotEmpty(spp)) {
                    String s = liRev.getEqualPropertyPath();
                    if (OAStr.isNotEmpty(s)) {
                        OAPropertyPath pp = new OAPropertyPath(li.getToClass(), s);
                        pp = pp.getReversePropertyPath();
                        s = pp.getPropertyPath();
                        spp += "." + s;
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

    @Override
    public OADataSourceIterator selectPassthru(Class selectClass, String queryWhere, String queryOrder, int max, OAFilter filter, boolean bDirty) {

        // 20211012 same as select for this datasource
        return select(selectClass, queryWhere, null, queryOrder, null, null, null, max, filter, bDirty);
        /*
         * was: if (!OAString.isEmpty(queryWhere)) { filter = new OAFilter() {
         * @Override public boolean isUsed(Object obj) { return false; } }; } return new ObjectCacheIterator(selectClass, filter);
         */
    }

    public @Override void assignId(OAObject obj) {
        super.assignId(obj); // have autonumber handle this
    }

    public boolean getSupportsPreCount() {
        return false;
    }

    protected boolean isOtherDataSource() {
        OADataSource[] dss = OADataSource.getDataSources();
        return dss != null && dss.length > 1;
    }

    @Override
    public boolean isClassSupported(Class clazz, OAFilter filter) {
        if (filter == null) {
            if (isOtherDataSource()) {
                return false;
            }
            return super.isClassSupported(clazz, filter);
        }
        // only if all objects are loaded, or no other DS
        if (!isOtherDataSource()) {
            return true;
        }

        if (OAObjectCacheDelegate.getSelectAllHub(clazz) != null) {
            return true;
        }
        return false;
    }

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
            
            OAObjectSerializer wrap = new OAObjectSerializer(hs, false, false);
            wrap.setIncludeBlobs(true);
            oos.writeObject(wrap);
        }
        oos.writeBoolean(false);
        oos.close();
    }

    public boolean loadFromStorageFile(final File file) throws Exception {
        LOG.fine("loading to storage file=" + file);
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

        OAObjectInputStream ois = new OAObjectInputStream(inflaterInputStream);
        
        boolean bResult = false;
        try {
            lock.writeLock().lock();
            bResult = _loadFromStorageFile(file, ois);
        }
        finally {
            ois.close();
            fis.close();

            lock.writeLock().unlock();
        }
        LOG.fine("loaded storage file=" + file);
        return bResult;
    }
    
    protected boolean _loadFromStorageFile(final File file, OAObjectInputStream ois) throws Exception {
        int cnt = 0;
        
        boolean b = ois.readBoolean();
        if (b) {
            OAObjectSerializer wrap = (OAObjectSerializer) ois.readObject();
            Object extra = wrap.getObject();
            cnt++;
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
            hmClass.put(c, hs);
        }

        return (cnt > 0);
    }

    private Set getSet(Class c) {
        if (c == null) {
            return null;
        }
        Set hs = hmClass.get(c);
        if (hs == null) {
            synchronized (hmClass) {
                hs = hmClass.get(c);
                if (hs == null) {
                    hs = new HashSet();
                    hmClass.put(c, hs);
                }
            }
        }
        return hs;
    }

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
        OAObjectCacheDelegate.removeAllObjects(c);
    }

}

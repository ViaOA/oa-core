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
package com.viaoa.sync.remote;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import com.viaoa.ds.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.sync.OASyncDelegate;

public abstract class RemoteClientImpl implements RemoteClientInterface {
    private static Logger LOG = Logger.getLogger(RemoteClientImpl.class.getName());
    // protected ConcurrentHashMap<Object, Object> hashCache = new ConcurrentHashMap<Object, Object>();
    // protected ConcurrentHashMap<Object, Object> hashLock = new ConcurrentHashMap<Object, Object>();
    private ClientGetDetail clientGetDetail; 
    private volatile RemoteDataSource remoteDataSource;
    private int sessionId;
    
    public RemoteClientImpl(int sessionId) {
        this.sessionId = sessionId;
        clientGetDetail = new ClientGetDetail(sessionId) {
            @Override
            protected void loadDataInBackground(OAObject obj, String property) {
                RemoteClientImpl.this.loadDataInBackground(obj, property);
            }
        };
    }
    
    /**
     * called when a other props or sibling data cant be loaded for current request, because of timeout.
     * This can be overwritten to have it done in a background thread.
     */
    protected void loadDataInBackground(OAObject obj, String property) {
    }
    
    
    // 20160101
    public void close() {
        clientGetDetail.close();
        clientGetDetail = null;
        remoteDataSource = null;
    }
    
    /**
     * this is called when objects are removed on the client,
     * so that the guid can be removed from the clientGetDetail cache of object.guids that have been sent to client. 
     */
    public void removeGuids(int[] guids) {
        if (guids == null) return;
        if (clientGetDetail == null) return;
        int x = guids.length;
        for (int i=0; i<x; i++) {
            clientGetDetail.removeGuid(guids[i]);
            //LOG.fine("remove guid="+guids[i]+" for "+sessionId);
        }
    }


    @Override
    public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger) {
        LOG.fine(id+") masterClass="+masterClass+", prop="+property);
        Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, masterProps, siblingKeys, bForHubMerger);
        return obj;
    }

    // 20151129 does not put in the msg queue, but will write the return value using the same vsocket that the msg queue thread uses.
    @Override
    public Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger) {
        LOG.fine(id+") masterClass="+masterClass+", prop="+property);
        Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, masterProps, siblingKeys, bForHubMerger);
        return obj;
    }
    
    @Override
    public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger) {
        Object obj = clientGetDetail.getDetail(id, masterClass, masterObjectKey, property, null, null, bForHubMerger);
        return obj;
    }

    
    public RemoteDataSource getRemoteDataSource() {
        if (remoteDataSource == null) {
            synchronized (this) {
                if (remoteDataSource == null) {
                    remoteDataSource = new RemoteDataSource() {
                        // used when an object from ds is not already in a hub with master.
                        @Override
                        public void setCached(OAObject obj) {
                            RemoteClientImpl.this.setCached(obj);
                        }
                    };            
                }
            }
        }
        return remoteDataSource;
    }
    @Override
    public Object datasource(int command, Object[] objects) {
        Object result = getRemoteDataSource().datasource(command, objects);
        return result;
    }
    @Override
    public void datasourceNoReturn(int command, Object[] objects) {
        getRemoteDataSource().datasource(command, objects);
    }

    protected OADataSource getDataSource(Class c) {
        if (c != null) {
            OADataSource ds = OADataSource.getDataSource(c);
            if (ds != null) return ds;
        }
        if (defaultDataSource == null) {
            OADataSource[] dss = OADataSource.getDataSources();
            if (dss != null && dss.length > 0) return dss[0];
        }
        return defaultDataSource;
    }
    protected OADataSource defaultDataSource;

    protected OADataSource getDataSource() {
        return getDataSource(null);
    }

    @Override
    public OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties) {
        OAObject obj = OAObjectCacheDelegate.getObject(objectClass, objectKey);
        if (obj == null) return null;
        OAObject objx = OAObjectReflectDelegate.createCopy(obj, excludeProperties);
        setCached(objx);
        return objx;
    }
    
    
    /**
     * Called to add objects to a client's server side cache, so that server with not GC the object.
     */
    public abstract void setCached(OAObject obj);
    

    @Override
    public boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
        OAObject obj = getObject(objectClass, objectKey);
        if (obj == null) return false;
        
        Hub h = getHub(obj, hubPropertyName);
        if (h == null) {
            // store null so that it can be an empty hub if needed (and wont have to get from server)
            if (!OASyncDelegate.isServer(objectClass)) {
                OAObjectPropertyDelegate.setPropertyCAS(obj, hubPropertyName, null, null, true, false);                
            }
            return false;
        }
        h.deleteAll();
        return true;
    }

    // on the server, if the object is not found in the cache, then it will be loaded by the datasource 
    private OAObject getObject(Class objectClass, OAObjectKey origKey) {
        OAObject obj = OAObjectCacheDelegate.get(objectClass, origKey);
        if (obj == null && OASyncDelegate.isServer(objectClass)) {
            obj = (OAObject) OADataSource.getObject(objectClass, origKey);
            if (obj != null) {
                // object must have been GCd, use the original guid
                OAObjectDelegate.reassignGuid(obj, origKey);
            }
        }
        return obj;
    }
    
    // on the server, if the Hub is not found in the cache, then it will be loaded by the datasource
    private Hub getHub(OAObject obj, String hubPropertyName) {
        if (obj == null) return null;
        boolean bWasLoaded = OAObjectReflectDelegate.isReferenceHubLoaded(obj, hubPropertyName);
        if (!bWasLoaded && !OASyncDelegate.isServer(obj.getClass())) {
            return null;
        }
        Object objx =  OAObjectReflectDelegate.getProperty(obj, hubPropertyName);
        if (!(objx instanceof Hub)) return null;

        // loadCachedOwners will have been done by the call to getObject(masterObj)
        return (Hub) objx;
    }

    @Override
    public boolean delete(Class objectClass, OAObjectKey objectKey) {
        OAObject obj = getObject(objectClass, objectKey);
        if (obj == null) return false;
        obj.delete();
        return true;
    }
}


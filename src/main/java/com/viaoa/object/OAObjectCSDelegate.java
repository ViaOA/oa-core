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
package com.viaoa.object;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

import com.viaoa.sync.*;
import com.viaoa.sync.remote.RemoteSessionInterface;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.*;
import com.viaoa.remote.*;

public class OAObjectCSDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectCSDelegate.class.getName());

    
    /**
     * @return true if the current thread is from the OAClient.getMessage().
     */
    public static boolean isRemoteThread() {
       return OARemoteThreadDelegate.isRemoteThread(); 
    }

    /**
    * Used to determine if this JDK is running as an OAServer or OAClient.
    * @return true if this is not a Client, either the Server or Stand alone
    */
    public static boolean isServer(OAObject obj) {
        Class c;
        if (obj == null) c = Object.class;
        else c = obj.getClass();
		return OASyncDelegate.isServer(c);
    }

    /**
    * Used to determine if this JDK is running as an OAServer or OAClient.
    * @return true if this is not a Client, either the Server or Stand alone
    */
    public static boolean isWorkstation(OAObject obj) {
        Class c;
        if (obj == null) c = Object.class;
        else c = obj.getClass();
        return !OASyncDelegate.isServer(c);
    }

    
    
    
    
    /**
    * Called by OAObjectDelegate.initialize(). 
    * If Object is being created on workstation, then it needs to be flagged that it is only on the client.
    */
    public static void objectCreated(OAObject obj) {
	    if (obj == null) return;
	    
        OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
        if (sc != null) sc.objectCreated(obj);
    }

    
    /**
     * called when an object has been removed from a client.
     * called by OAObject.finalize
     */
    protected static void objectFinalized(OAObject obj) {
        if (obj == null) return;
        OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
        if (sc != null) sc.objectFinalized(obj.getGuid());
    }

    public static void updateObjectsWithoutHubs(OAObject obj) {
        if (obj == null) return;
        int guid = obj.getGuid();
        if (guid < 0) return;

        OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
        if (sc != null) sc.updateObjectsWithoutHubs(obj);
    }

    
    /** Create a new copy of an object.
        If OAClient.client exists, this will create the object on the server.
     */
     protected static OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
         if (oaObj == null) return null;
         RemoteClientInterface ri = OASyncDelegate.getRemoteClient(oaObj.getClass());
         if (ri != null) {
             OAObject obj = ri.createCopy(oaObj.getClass(), oaObj.getObjectKey(), excludeProperties);;
             return obj; 
         }
         return null;
     }
	
     protected static int getGuidFromServer(OAObject obj) {
         Class c;
         if (obj == null) c = Object.class;
         else c = obj.getClass();
         return getGuidFromServer(c);
     }
     protected static int getGuidFromServer(Class clazz) {
         if (clazz == null) clazz = Object.class;
         int guid = OASyncDelegate.getGuidFromServer(clazz);
         return guid;
    }

    // returns true if this was saved on server
    protected static boolean save(OAObject oaObj, int iCascadeRule) {
        if (oaObj == null) return false;
        RemoteServerInterface rs = OASyncDelegate.getRemoteServer(oaObj.getClass());
        if (rs != null) {
            return rs.save(oaObj.getClass(), oaObj.getObjectKey(), iCascadeRule);
        }
        return false;
    }

    /**
     * 20150815 returns true if this should be deleted on this computer, false if it is done on the server. 
    */
    protected static boolean delete(final OAObject obj) {
        if (obj == null) return false;
        LOG.finer("obj="+obj);

        if (OASyncDelegate.isSingleUser()) {
            return true; // run delete
        }

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return true;
        
        if (OASyncDelegate.isServer(obj.getClass())) { 
            // this will invoke on the server using OARemoteThread
            rs.serverDelete(obj.getClass(), obj.getObjectKey());
            return false;  
        }

        // OAClient
        if (!OARemoteThreadDelegate.shouldSendMessages()) return true;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return true;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return true; 
        
        rs.serverDelete(obj.getClass(), obj.getObjectKey());  // will call OAObjectDeleteDelegate
        
        return false;
    }

    
    protected static void sendDeleteToClients(OAObject obj) {
        if (obj == null) return;

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return;
        
        if (!OASyncDelegate.isServer(obj.getClass())) return;
//qqqqqqqqqqqqqqqqqqqqqq needs to send these to client if on RemoteThread        
        
        /*qqqqqq
        Thread t = Thread.currentThread();
        if (t instanceof OARemoteThread) {
            if (!((OARemoteThread) t).getSendMessages()) return;
            // dont use this, which is the default:  
            //   if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        }
        */
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return; 
        
        rs.clientDelete(obj.getClass(), obj.getObjectKey());
    }
    
    

	protected static OAObject getServerObject(Class clazz, OAObjectKey key) {
	    if (clazz == null || key == null) return null;
        RemoteServerInterface rs = OASyncDelegate.getRemoteServer(clazz);
        OAObject result;
        if (rs != null) {
            result = rs.getObject(clazz, key);
        }       
        else result = null;
        return result;
	}    
	
    protected static byte[] getServerReferenceBlob(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
        Object obj = null;
        
        OASyncClient sc = OASyncDelegate.getSyncClient(oaObj.getClass());
        if (sc != null) {
            obj = sc.getDetail(oaObj, linkPropertyName);
        }
        else {
            LOG.warning("This should only be called from workstations, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
        }
        if (obj instanceof byte[]) return (byte[]) obj;
        return null;
    }    
	
    // used by OAObjectReflectDelegate.getReferenceHub()
    protected static Object getServerReference(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
        Object value = null;
        OASyncClient sc = OASyncDelegate.getSyncClient(oaObj.getClass());
        if (sc != null) {
            value = sc.getDetail(oaObj, linkPropertyName);
        }
        else {
            LOG.warning("This should only be called from workstations, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
        }
        return value;
    }

    
	// used by OAObjectReflectDelegate.getReferenceHub()
	public static Hub getServerReferenceHub(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
    	Hub hub = null;
        OASyncClient sc = OASyncDelegate.getSyncClient(oaObj.getClass());
        if (sc != null) {
            Object obj = sc.getDetail(oaObj, linkPropertyName);
            if (obj instanceof Hub) hub = (Hub) obj;
            if (hub == null) {
                LOG.warning("OAObject.getDetail(\""+linkPropertyName+"\") not found on server for "+oaObj.getClass().getName());
            }
        }
        else {
            LOG.warning("This should only be called from clients, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
        }
		return hub;
	}
	
	// used by OAObjectReflectDelegate.getReferenceHub() to have all data loaded on server.
	protected static boolean loadReferenceHubDataOnServer(Hub thisHub, OASelect select) {
        if (thisHub == null) return false;
        boolean bResult;
        if (OASyncDelegate.isServer(thisHub)) {
            //LOG.finest("hub="+hub);

            // 20140328 performance improvement 
            if (thisHub.getSelect() == null && select == null) return true;
            
            
            bResult = true;
            // load all data without sending messages
            // even though Hub.writeObject does this, this data could be used on server application
        	try {
        		OAThreadLocalDelegate.setSuppressCSMessages(true);
        		HubSelectDelegate.loadAllData(thisHub, select);
        	}
        	finally {
        		OAThreadLocalDelegate.setSuppressCSMessages(false);        	
        	}
        }
        else bResult = false;
        return bResult;
	}
	
	
    protected static void fireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
        if (obj == null) return;
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return;
        
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        
        if (OAThreadLocalDelegate.isLoading()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return;

        // LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        
        // 20130319 dont send out calc prop changes
        OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
        if (li != null && li.bCalculated) return;
        // LOG.finer("object="+obj+", key="+origKey+", prop="+propertyName+", newValue="+newValue+", oldValue="+oldValue);

        
        // 20130318 if blob, then set a flag so that the server does not broadcast to all clients
        //     the clients (OAClient.procesPropChange) will recv the msg and know how to handle it.
        //       so that the next time the prop getXxx is called, it will then get it from the server
        boolean bIsBlob = false;
        if (newValue != null && newValue instanceof byte[]) {
            byte[] bs = (byte[]) newValue;
            if (bs.length > 400) {
                OAPropertyInfo pi = OAObjectInfoDelegate.getPropertyInfo(oi, propertyName);
                if (pi.isBlob()) {
                    bIsBlob = true;
                }
            }
        }
        OAObjectKey key = obj.getObjectKey();
        rs.propertyChange(obj.getClass(), key, propertyName, newValue, bIsBlob);
	}
	
    protected static void fireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue) {
        // qqqqqqqqqqqqqqqqqqqqqqq  Important NOTE: dont send, it is now using beforePropertyChange
        if (true || false) return; //qqqqqqqqqqqqqq

        //LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        
        if (OAThreadLocalDelegate.isLoading()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return;

        // 20130319 dont send out calc prop changes
        OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
        if (li != null && li.bCalculated) return;
        // LOG.finer("object="+obj+", key="+origKey+", prop="+propertyName+", newValue="+newValue+", oldValue="+oldValue);

        
        // 20130318 if blob, then set a flag so that the server does not broadcast to all clients
        //     the clients (OAClient.procesPropChange) will recv the msg and know how to handle it.
        //       so that the next time the prop getXxx is called, it will then get it from the server
        boolean bIsBlob = false;
        if (newValue != null && newValue instanceof byte[]) {
            byte[] bs = (byte[]) newValue;
            if (bs.length > 400) {
                OAPropertyInfo pi = OAObjectInfoDelegate.getPropertyInfo(oi, propertyName);
                if (pi.isBlob()) {
                    bIsBlob = true;
                }
            }
        }
        
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs != null) {
            rs.propertyChange(obj.getClass(), origKey, propertyName, newValue, bIsBlob);
        }
    }
}


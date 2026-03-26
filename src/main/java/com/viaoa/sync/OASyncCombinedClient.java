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
package com.viaoa.sync;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.graph.service.object.OAObjectPropertyService;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.remote.multiplexer.*;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.remote.RemoteSyncInterface;

/**
 * Experimental client capable of combining multiple independent
 * {@link OASyncServer} instances into a single logical synchronization space.
 * <p>
 * Each source server maintains its own OA model instance and object key space.
 * {@code OASyncCombinedClient} creates:
 * <ul>
 *   <li>one {@link OASyncClient} connected to a designated combined server, and</li>
 *   <li>one {@link OASyncClient} for each source server being merged.</li>
 * </ul>
 *
 * <h2>Key Mapping</h2>
 * Because each server uses its own {@link com.viaoa.object.OAObjectKey}
 * sequence and namespace, this class maintains per-class bidirectional mapping
 * tables so that sync messages can be forwarded:
 * <ul>
 *   <li>from a source server to the combined server, and</li>
 *   <li>from the combined server back to the appropriate source server.</li>
 * </ul>
 *
 * <h2>Routing Sync Events</h2>
 * Custom {@code RemoteSyncInterface} implementations translate object keys
 * using the maintained mappings and then forward the change events to the
 * appropriate target server.
 *
 * <h3>Status</h3>
 * This class is not fully implemented, and several operations (such as delete
 * propagation and hub-move semantics) remain incomplete. It should be treated
 * as experimental and not used in production.
 */
public class OASyncCombinedClient {
    private static Logger LOG = Logger.getLogger(OASyncCombinedClient.class.getName());

    /**
     * The {@link OASyncClient} connected to the combined (master) sync server.
     * All forwarded sync events from source servers ultimately route through
     * this client.
     */
    private OASyncClient syncClient;

    /**
     * Mapping of each participating source server's remote multiplexer client
     * to its associated {@link ClientSession}, which tracks per-class mappers
     * and GUID allocation for that source.
     */
    private ConcurrentHashMap<OARemoteMultiplexerClient, ClientSession> hmClientSession = new ConcurrentHashMap<OARemoteMultiplexerClient, ClientSession>();
    
    /**
     * Holds state for a single source sync server participating in the combined
     * sync space. Each session maintains:
     * <ul>
     *   <li>a dedicated {@link OASyncClient} connected to that server,</li>
     *   <li>per-class key mappers,</li>
     *   <li>state required to allocate GUIDs for remapped objects.</li>
     * </ul>
     */
    private class ClientSession {
    	/**
    	 * The sync client connected directly to a specific source server. All
    	 * forwarded messages destined for that source will use this client.
    	 */
        OASyncClient syncClient;

        /**
         * Per-class mapping table storing bidirectional {@link Mapper} instances
         * that translate object keys between the source server and the combined
         * server.
         */
        ConcurrentHashMap<Class, Mapper> hmClassToMapper = new ConcurrentHashMap<Class, OASyncCombinedClient.Mapper>();
        

        /**
         * Lock object used to synchronize allocation of server-side GUIDs when
         * forwarding newly created objects to the combined server.
         */
        private final Object NextGuidLock = new Object();

        /**
         * The next GUID value to assign when mapping new objects from the source
         * server to the combined server.
         */
        private long nextGuid;
        
        /**
         * The upper bound (exclusive) for the current block of GUIDs allocated from
         * the combined server. When {@link #nextGuid} reaches this value, a new block
         * is requested.
         */
        private long maxNextGuid;
        
        /**
         * Allocates and returns the next GUID for a source-server object being
         * mapped into the combined server namespace. When the local block is
         * exhausted, requests a new block of 50 GUIDs from the combined server.
         *
         * @return the next GUID assigned for combined-server mapping
         */
        long getNextGuid() {
            long x = 0;
            synchronized (NextGuidLock) {
                if (nextGuid == maxNextGuid) {
                    try {
                        nextGuid = syncClient.getRemoteServer().getNextFiftyObjectGuids();
                        maxNextGuid = nextGuid + 50; 
                    }
                    catch (Exception ex) {
                        LOG.log(Level.WARNING, "", ex);
                    }
                }
                x = nextGuid++;
            }
            return x;
        }
    }
    
    /**
     * Holds two bidirectional key-mapping tables for a specific class:
     * <ul>
     *   <li>{@code hmClientToServer}: maps source-server keys → combined-server keys,</li>
     *   <li>{@code hmServerToClient}: maps combined-server keys → source-server keys.</li>
     * </ul>
     */
    private class Mapper {
    	/**
    	 * Mapping from source-server {@link OAObjectKey} values to their
    	 * combined-server key counterparts.
    	 */
        ConcurrentHashMap<OAObjectKey, OAObjectKey> hmClientToServer = new ConcurrentHashMap<OAObjectKey, OAObjectKey>();

        /**
         * Mapping from combined-server {@link OAObjectKey} values back to their
         * source-server key counterparts.
         */
        ConcurrentHashMap<OAObjectKey, OAObjectKey> hmServerToClient = new ConcurrentHashMap<OAObjectKey, OAObjectKey>();
    }
    
    /**
     * Default constructor for creating an {@code OASyncCombinedClient}.
     * Combined-client registration is currently disabled (commented out).
     */
    public OASyncCombinedClient() {
//        OASyncDelegate.setSyncCombinedClient(this);
    }

    /**
     * Finds the {@link ClientSession} whose mapping for the given class contains
     * the specified combined-server key. Used to determine which source server an
     * incoming combined-server sync event should be forwarded to.
     *
     * @param c the object's class
     * @param okServer the combined-server object key
     * @return the matching client session, or {@code null} if not found
     */
    private ClientSession getClientSession(Class c, OAObjectKey okServer) {
        for (Map.Entry<OARemoteMultiplexerClient, ClientSession> me : hmClientSession.entrySet()) { 
            Mapper m = me.getValue().hmClassToMapper.get(c);
            if (m == null) continue;
            if (m.hmServerToClient.get(okServer) != null) return me.getValue(); 
        }
        return null;
    }
    
    
    /**
     * Lazily creates and returns the {@link OASyncClient} connected to the
     * combined sync server. Overrides its {@code getRemoteSyncImpl} method so
     * that all sync events received from the combined server are remapped
     * (via mappers) and forwarded to their appropriate source servers.
     *
     * @param packagex the model package used by the combined server
     * @param hostName the host name of the combined server
     * @param port the port of the combined server
     * @return the initialized combined sync client
     */
    public OASyncClient getCombinedSyncClient(Package packagex, String hostName, int port) {
        if (syncClient != null) return syncClient;
        syncClient = new OASyncClient(packagex, hostName, port, true) {

        	/**
        	 * Cached remote sync callback implementation responsible for translating
        	 * combined-server sync events into source-server sync operations.
        	 */
        	RemoteSyncInterface remoteSync;

            // redirect changes from combined server to the correct server
            @Override
            public RemoteSyncInterface getRemoteSyncImpl() throws Exception {
                if (remoteSync != null) return remoteSync;
                
                remoteSync = new RemoteSyncInterface() {
                    
                	/**
                	 * Helper method that retrieves the remote sync interface for a given
                	 * source server’s {@link ClientSession}.
                	 *
                	 * @param cs the client session associated with a source server
                	 * @return the remote sync interface, or {@code null} if unavailable
                	 */
                    RemoteSyncInterface getRemoteSyncInterface(ClientSession cs) {
                        try {
                            return cs.syncClient.getRemoteSync();
                        }
                        catch (Exception e) {
                        }
                        return null;
                    }
                    
                    /**
                     * Combined-server implementation of {@code sort}. Currently a stub that
                     * performs no sorting and always returns {@code true}.
                     */
                    @Override
                    public boolean sort(Class objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending, Comparator comp) {
                        return true;
                    }

                    /**
                     * Forwards a hub-removal event from the combined server to the correct
                     * source server by:
                     * <ul>
                     *   <li>resolving the appropriate {@link ClientSession},</li>
                     *   <li>translating both object keys from combined-server → source-server
                     *       keys using per-class mappers,</li>
                     *   <li>invoking {@code removeFromHub} on the target server’s
                     *       {@link RemoteSyncInterface}.</li>
                     * </ul>
                     *
                     * @return {@code true} if forwarded successfully, otherwise {@code false}
                     */
                    @Override
                    public boolean removeFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, Class objectClassX, OAObjectKey objectKeyX) {
                        ClientSession cs = getClientSession(objectClass, objectKey);
                        if (cs == null) return false;
                        
                        Mapper m = cs.hmClassToMapper.get(objectClass);
                        if (m == null) return false;
                        
                        OAObjectKey k1 = m.hmServerToClient.get(objectKey);
                        if (k1 == null) return false;
                        
                        m = cs.hmClassToMapper.get(objectClassX);
                        if (m == null) return false;

                        OAObjectKey k2 = m.hmServerToClient.get(objectKeyX);
                        if (k2 == null) return false;

                        
                        RemoteSyncInterface rs = getRemoteSyncInterface(cs);
                        if (rs == null) return false;
                        
                        rs.removeFromHub(objectClass, k1, hubPropertyName, objectClassX, k2);
                        
                        return true;
                    }
                    
                    /**
                     * Forwards a "remove all from hub" event from the combined server to the
                     * appropriate source server. Resolves the target {@link ClientSession},
                     * translates the combined-server key to the source-server key, and invokes
                     * {@code removeAllFromHub} on the source server’s
                     * {@link RemoteSyncInterface}.
                     *
                     * @return {@code true} if the event is forwarded successfully; otherwise {@code false}
                     */
                    @Override
                    public boolean removeAllFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
                        ClientSession cs = getClientSession(objectClass, objectKey);
                        if (cs == null) return false;
                        
                        Mapper m = cs.hmClassToMapper.get(objectClass);
                        if (m == null) return false;
                        
                        OAObjectKey k1 = m.hmServerToClient.get(objectKey);
                        if (k1 == null) return false;

                        RemoteSyncInterface rs = getRemoteSyncInterface(cs);
                        if (rs == null) return false;
                        
                        rs.removeAllFromHub(objectClass, k1, hubPropertyName);
                        return true;
                    }
                    
                    /**
                     * Forwards a property-change event from the combined server to the correct
                     * source server. Handles:
                     * <ul>
                     *   <li>mapping combined-server → source keys,</li>
                     *   <li>special handling for ID-property changes,</li>
                     *   <li>recognition of "new" and "changed" markers,</li>
                     *   <li>translating {@link OAObject} or {@link OAObjectKey} values as needed,</li>
                     *   <li>creating mapped keys for newly encountered objects.</li>
                     * </ul>
                     *
                     * @param objectClass the class of the affected object
                     * @param origServerKey the combined-server key for the object
                     * @param propertyName the property being updated
                     * @param newValue the new property value (may be an object or key)
                     * @param bIsBlob whether the property represents BLOB data
                     * @return {@code true} if successfully forwarded; otherwise {@code false}
                     */
                    @Override
                    public boolean propertyChange(final Class objectClass, final OAObjectKey origServerKey, final String propertyName, Object newValue, final boolean bIsBlob) {
//qqqqqqq check to see if objectId is changed, if so then use the old value to find match
                        // and update the hm with new valeu
//qqqqqq dont send pkey changes                        
                        if (propertyName == null) return false;
                        
                        ClientSession clientSession = getClientSession(objectClass, origServerKey);
                        if (clientSession == null) return false;
                        
                        final Mapper mapper = clientSession.hmClassToMapper.get(objectClass);
                        if (mapper == null) return false;
                        
                        OAObjectKey clientKey = mapper.hmServerToClient.get(origServerKey);
                        if (clientKey == null) return false;

        				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objectClass);
                        final OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(objectClass);
                        if (oi == null) return false;
                        
                        if (oi.isIdProperty(propertyName)) {
                            // dont send pkey changes                        
                            // update key with new value, replace old with new in mapper
                            OAObjectKey newServerKey = new OAObjectKey(new Object[] {newValue}, origServerKey.getGuid());
                            mapper.hmServerToClient.remove(origServerKey);
                            mapper.hmServerToClient.put(newServerKey, clientKey);
                        }
                        else if (propertyName.equalsIgnoreCase("new")) {
                            if (!(newValue instanceof Boolean)) return false;
                            mapper.hmServerToClient.remove(origServerKey);
                            OAObjectKey newServerKey = new OAObjectKey(origServerKey.getObjectIds(), origServerKey.getGuid());
                            mapper.hmServerToClient.put(newServerKey, clientKey);
                        }
                        else if (propertyName.equalsIgnoreCase("changed")) {
                            return false;
                        }
                        else {
                            // new value might be another oaObject
                            // change it's key/etc and set to oaObjKey instead 
                            if (newValue instanceof OAObject) {
                                OAObject objValue = (OAObject) newValue;
                                // see if it already exists, then change the value to match orig
                                Mapper m = getMapper(clientSession, objValue.getClass());
                                OAObjectKey clientNewValueKey = m.hmServerToClient.get(objValue.getObjectKey());
                                
                                if (clientNewValueKey == null) {
                                    // new object, does not exist on the source server
                                
                                    OAObjectKey k1 = objValue.getObjectKey();
                                    
                                    // need to change key 
                                    og.objectsInternal().callObjectCacheRemoveObject(objValue);
                                    og.objectsInternal().callObjectInitializeSetAsNewObject(objValue, UUID.randomUUID());
                                    
                                    // need to add it to mapper
                                    OAObjectKey k2 = objValue.getObjectKey();
                                    m.hmServerToClient.put(k1, k2);
                                    m.hmClientToServer.put(k2, k1);
                                }
                                else {
                                    newValue = clientNewValueKey; // send key only
                                }
                            }
                            else if (newValue instanceof OAObjectKey) {
                                OAObjectKey ok = (OAObjectKey) newValue;
                                OALinkInfo li = oi.getLinkInfo(propertyName);
                                if (li == null) return false;
                                
                                Mapper m3 = clientSession.hmClassToMapper.get(li.getToClass());
                                if (m3 == null) return false;
                                
                                OAObjectKey ok2 = m3.hmServerToClient.get(ok);
                                if (ok2 == null) {
                                    // create new to send
                                }
                                else {
                                    newValue = ok2;
                                }
                            }
                            
                            final RemoteSyncInterface remoteSyncInterface = getRemoteSyncInterface(clientSession);
                            if (remoteSyncInterface == null) return false;
                            
                            remoteSyncInterface.propertyChange(objectClass, clientKey, propertyName, newValue, bIsBlob);
                        }
                        
                        return true;
                    }
                    
                    /**
                     * Stub implementation for forwarding reordering events in hubs.
                     * Currently unsupported and always returns {@code false}.
                     */
                    @Override
                    public boolean moveObjectInHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, int posFrom, int posTo) {
                        return false;
                    }
                    
                    /**
                     * Forwards an insert-into-hub event from the combined server to the
                     * appropriate source server. Translates the combined-server key to
                     * the source-server key and invokes {@code insertInHub} on the target's
                     * remote sync interface.
                     *
                     * @return {@code true} if forwarded successfully; otherwise {@code false}
                     */
                    @Override
                    public boolean insertInHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos) {
                        OAObjectKey k1 = getClientToServerKey(masterObjectClass, masterObjectKey);
                        if (k1 == null) return false;

                        try {
                            syncClient.getRemoteSync().insertInHub(masterObjectClass, k1, hubPropertyName, obj, pos);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Stub implementation for clearing hub-change logs.
                     * No action is taken.
                     */
                    @Override
                    public void clearHubChanges(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
                    }
                    
                    /**
                     * Forwards an "add to hub" event from the combined server to the correct
                     * source server by translating the combined-server key to the source-server
                     * key and invoking the corresponding remote sync method.
                     *
                     * @return {@code true} if forwarded successfully; otherwise {@code false}
                     */
                    @Override
                    public boolean addToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj) {
                        OAObjectKey k1 = getClientToServerKey(masterObjectClass, masterObjectKey);
                        if (k1 == null) return false;

                        try {
                            syncClient.getRemoteSync().addToHub(masterObjectClass, k1, hubPropertyName, obj);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Convenience wrapper that extracts the object from the serializer and
                     * forwards the operation via {@link #addToHub(Class, OAObjectKey, String, Object)}.
                     *
                     * @return {@code true} if forwarded successfully
                     */
                    @Override
                    public void addNewToCache(OAObjectSerializer obj) {
                        obj.getObject();
                    }

                    /**
                     * Forwards a refresh request from the combined server to the appropriate
                     * source server. Resolves the source {@link ClientSession}, translates
                     * the combined-server key to the source-server key, and calls
                     * {@code refresh} on the remote sync interface.
                     */
                    @Override
                    public void refresh(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
                        ClientSession cs = getClientSession(masterObjectClass, masterObjectKey);
                        if (cs == null) return;
                        
                        Mapper m = cs.hmClassToMapper.get(masterObjectClass);
                        if (m == null) return;
                        
                        OAObjectKey k1 = m.hmServerToClient.get(masterObjectKey);
                        if (k1 == null) return;

                        RemoteSyncInterface rs = getRemoteSyncInterface(cs);
                        if (rs == null) return;
                        
                        rs.refresh(masterObjectClass, masterObjectKey, hubPropertyName);
                    }

                    /**
                     * Stub method for forwarding server-side delete events.
                     * Not yet implemented.
                     */
                    @Override
                    public void serverDelete(Class objectClass, OAObjectKey objectKey) {
                        // TODO Auto-generated method stub
//qqqqqqqqqqqqqqqqqqqqqqqq                        
                    }

                    /**
                     * Stub method for forwarding client-side delete events.
                     * Not yet implemented.
                     */
                    @Override
                    public void clientDelete(Class objectClass, OAObjectKey objectKey) {
                        // TODO Auto-generated method stub
//qqqqqqqqqqqqqqqqqqqqqqq                        
                    }
                };
                return remoteSync;
            }
        };
        return syncClient;
    }
    
//qqqqqq create a dsCombinedClient that will send requests to this
    //  save, getRef obj/hub ...
    
    
    
    
    /**
     * Creates a new {@link OASyncClient} for a source server that participates
     * in the combined sync space. The created sync client overrides
     * {@code getRemoteSyncImpl()} so that all sync events originating from the
     * source server are forwarded to the combined server after appropriate
     * key translation.
     *
     * Registers a new {@link ClientSession} for the source server.
     *
     * @param packagex the model package for the source server
     * @param hostName the source-server host name
     * @param port the source-server port
     * @return the created sync client associated with the source server
     */
    public OASyncClient createSyncClient(Package packagex, String hostName, int port) {
        OASyncClient sc = new OASyncClient(packagex, hostName, port, false) {
        	/**
        	 * Cached remote sync callback implementation used to forward source-server
        	 * sync events to the combined server.
        	 */
            RemoteSyncInterface remoteSync;
            
            // redirect changes from one server to the combined server
            @Override
            public RemoteSyncInterface getRemoteSyncImpl() throws Exception {
                if (remoteSync != null) return remoteSync;
                
                remoteSync = new RemoteSyncInterface() {
                	/**
                	 * Stub implementation for sorting operations on a source server.
                	 * Currently performs no operation and always returns {@code true}.
                	 */
                    @Override
                    public boolean sort(Class objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending, Comparator comp) {
                        return true;
                    }
                    
                    /**
                     * Forwards a hub-removal event from a source server to the combined server.
                     * The source keys are translated to combined-server keys before invoking
                     * {@code removeFromHub} on the combined server’s remote sync interface.
                     *
                     * @return {@code true} if forwarded successfully; otherwise {@code false}
                     */
                    @Override
                    public boolean removeFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, Class objectClassX, OAObjectKey objectKeyX) {
                        OAObjectKey k1 = getClientToServerKey(objectClass, objectKey);
                        if (k1 == null) return false;

                        OAObjectKey k2 = getClientToServerKey(objectClassX, objectKeyX);
                        if (k2 == null) return false;
                            
                        try {
                            syncClient.getRemoteSync().removeFromHub(objectClass, k1, hubPropertyName, objectClassX, k2);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Forwards a remove-all-from-hub event from a source server to the combined
                     * server via key translation and a remote sync invocation.
                     *
                     * @return {@code true} if forwarded successfully; otherwise {@code false}
                     */
                    @Override
                    public boolean removeAllFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
                        OAObjectKey k1 = getClientToServerKey(objectClass, objectKey);
                        if (k1 == null) return false;

                        try {
                            syncClient.getRemoteSync().removeAllFromHub(objectClass, k1, hubPropertyName);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Forwards a property-change event from a source server to the combined
                     * server after resolving and translating the source key to its corresponding
                     * combined-server key.
                     *
                     * @return {@code true} if successfully forwarded; otherwise {@code false}
                     */
                    @Override
                    public boolean propertyChange(Class objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob) {
                        OAObjectKey k1 = getClientToServerKey(objectClass, origKey);
                        if (k1 == null) return false;
//qqqqqq could be pkey change                        
                        try {
                            syncClient.getRemoteSync().propertyChange(objectClass, k1, propertyName, newValue, bIsBlob);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Stub for source-server hub reordering events.
                     * Currently unsupported and always returns {@code false}.
                     */
                    @Override
                    public boolean moveObjectInHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, int posFrom, int posTo) {
                        return false;
                    }
                    
                    /**
                     * Forwards an insertion event from a source server to the combined server
                     * after translating the source keys to combined-server keys.
                     *
                     * @return {@code true} if forwarded; otherwise {@code false}
                     */
                    @Override
                    public boolean insertInHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos) {
                        OAObjectKey k1 = getClientToServerKey(masterObjectClass, masterObjectKey);
                        if (k1 == null) return false;

                        try {
                            syncClient.getRemoteSync().insertInHub(masterObjectClass, k1, hubPropertyName, obj, pos);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Stub implementation for clearing hub change history.
                     * No operations are performed.
                     */
                    @Override
                    public void clearHubChanges(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
                    }
                    
                    /**
                     * Forwards an add-to-hub event from a source server to the combined server
                     * by translating object keys and invoking the combined server’s
                     * {@code addToHub}.
                     *
                     * @return {@code true} if forwarded; otherwise {@code false}
                     */
                    @Override
                    public boolean addToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj) {
                        OAObjectKey k1 = getClientToServerKey(masterObjectClass, masterObjectKey);
                        if (k1 == null) return false;

                        try {
                            syncClient.getRemoteSync().addToHub(masterObjectClass, k1, hubPropertyName, obj);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        return true;
                    }
                    
                    /**
                     * Convenience wrapper used when the source server sends a serialized object.
                     * Extracts the object from the {@link OAObjectSerializer} and forwards the
                     * add-to-hub event to the combined server after key translation.
                     *
                     * @return {@code true} if forwarded successfully; otherwise {@code false}
                     */
                    @Override
                    public void addNewToCache(OAObjectSerializer obj) {
                        obj.getObject();
                    }
                    
                    /**
                     * Forwards a refresh request from a source server to the combined server.
                     * Translates the source-server key to the corresponding combined-server key,
                     * then invokes the combined server's {@code refresh}.
                     */
                    @Override
                    public void refresh(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName) {
                        OAObjectKey k1 = getClientToServerKey(masterObjectClass, masterObjectKey);
                        if (k1 == null) return;

                        try {
                            syncClient.getRemoteSync().refresh(masterObjectClass, k1, hubPropertyName);
                        }
                        catch (Exception e) {
                            LOG.log(Level.WARNING, "", e);
                        }
                    }
                    
                    /**
                     * Stub for forwarding server-side delete events from a source server to the
                     * combined server. Not implemented.
                     */
                    @Override
                    public void serverDelete(Class objectClass, OAObjectKey objectKey) {
                        // TODO Auto-generated method stub
//qqqqqqqqqqqqqqqqqqqqqqqq                        
                    }
                    
                    /**
                     * Stub for forwarding client-side delete events from a source server to the
                     * combined server. Not implemented.
                     */
                    @Override
                    public void clientDelete(Class objectClass, OAObjectKey objectKey) {
                        // TODO Auto-generated method stub
//qqqqqqqqqqqqqqqqqqqqqqqq                        
                    }
                };
                return remoteSync;
            }
        };

        OARemoteMultiplexerClient rmc = sc.getRemoteMultiplexerClient();
        ClientSession session = new ClientSession();
        session.syncClient = sc;
        hmClientSession.put(rmc, session);
        
        return sc;
    }
    
    
    /**
     * Returns the {@link OASyncClient} associated with the current thread's
     * remote multiplexer client. If no mapping exists, {@code null} is returned.
     *
     * @return the thread-associated sync client, or {@code null} if unavailable
     */
    public OASyncClient getCurrentThreadSyncClient() {
        OARemoteMultiplexerClient rmc = null;//OAThreadLocalDelegate.getRemoteMultiplexerClient();
        if (rmc == null) {
            return null;
        }
        ClientSession session = hmClientSession.get(rmc);
        if (session == null) {
            return null;
        }
        return session.syncClient;
    }

    
    /**
     * Returns the {@link Mapper} for the specified class within a given
     * {@link ClientSession}. If the mapper does not exist, it is created and
     * added to the session.
     *
     * @param cs the client session owning the mapping
     * @param c the class whose mapper is requested
     * @return the existing or newly created mapper
     */
    private Mapper getMapper(ClientSession cs, Class c) {
        Mapper mapper = cs.hmClassToMapper.computeIfAbsent(c, k -> new Mapper());
        return mapper;
    }

    
    /*
     * Called from OAObjectSerialization.resolveObject, to get the correct object that is used on the
     * combined server.
     * @param objClient
     * @return null if this is not used, otherwise it will change the object with new Id for combined server.
     */
    /**
     * Resolves an object originating from a source server into its corresponding
     * combined-server object. Handles:
     * <ul>
     *   <li>detecting whether resolution applies to the current call context,</li>
     *   <li>translating client → server keys,</li>
     *   <li>creating new server-side objects and assigning new GUIDs,</li>
     *   <li>updating mappers for newly created mappings,</li>
     *   <li>rewriting object-key-valued properties to use mapped server keys.</li>
     * </ul>
     *
     * @param objClient the client-side object to resolve
     * @return the combined-server object, or {@code null} if resolution does not apply
     */
    public OAObject resolveObject(final OAObject objClient) {

        OARemoteMultiplexerClient rmc = null;//OAThreadLocalDelegate.getRemoteMultiplexerClient();
        
        if (rmc == null || rmc == syncClient.getRemoteMultiplexerClient()) {
            //qqqqqqqqqqq from Server to Client(s)  - there might not be any clients
            return null;
        }

        ClientSession session = hmClientSession.get(rmc);
        if (session == null) {
            return null;
        }

        // from Client to Server
        Mapper mapper = getMapper(session, objClient.getClass());
        
        OAObject objServer;
        OAObjectKey keyClient = objClient.getObjectKey();
        OAObjectKey keyServer = mapper.hmClientToServer.get(keyClient);
        
        // if null create new obj for server
        if (keyServer == null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objClient);
        	og.objectsInternal().callObjectInitializeSetAsNewObject(objClient);
            objServer = objClient;
//qqqqq need to know when key is changed on server and then update the map            
            keyServer = objClient.getObjectKey();
            mapper.hmClientToServer.put(keyClient, keyServer);
            mapper.hmServerToClient.put(keyServer, keyClient);
            
            
            // qqqqqq remap all of the props that are objkeys
            OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(objServer.getClass());
            for (String prop : og.objectsInternal().callObjectPropertyGetPropertyNames(objServer)) {
                Object objx = og.objectsInternal().callObjectPropertyGetProperty(objServer, prop);
                if (!(objx instanceof OAObjectKey)) continue;
                OAObjectKey k = (OAObjectKey) objx;
                OALinkInfo li = oi.getLinkInfo(prop);
                if (li == null) continue;
                Class c = li.getToClass();
                Mapper m = getMapper(session, c);
                OAObjectKey k2 = mapper.hmClientToServer.get(k);
                if (keyServer == null) {
/*qqqqqqqqqqqq 20251229 todo:  getNextGuid needs packagex                  	
                    k2 = new OAObjectKey(null, OAObjectDelegate.getNextGuid());
                    mapper.hmClientToServer.put(k, k2);
                    mapper.hmServerToClient.put(k2, k);
qqqqqqqqqqqqq */                    
                }
                og.objectsInternal().callObjectPropertySetProperty(objServer, prop, k2);
            }
            
        }
        else {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(objClient);
            objServer = og.objectsInternal().callObjectCacheGet(objClient.getClass(), keyServer);
            if (objServer == null) {
                // get from original server
//qqqqqqqqq                
                
            }
        }
        return objServer;
    }

        
//qqqqqqqqq also        
//        oi.getImportMatchProperties();
    
    
    // get the key that was created for the combined server
    /**
     * Translates a source-server object key into its corresponding combined-server
     * object key using the per-class mapping tables in the relevant
     * {@link ClientSession}.
     *
     * @param c the class of the object whose key is being translated
     * @param keyClient the object key from the source server
     * @return the mapped combined-server key, or {@code null} if none exists
     */
    public OAObjectKey getClientToServerKey(final Class c, final OAObjectKey keyClient) {
        
        OARemoteMultiplexerClient rmc = null;//OAThreadLocalDelegate.getRemoteMultiplexerClient();
        
        if (rmc == null || rmc == syncClient.getRemoteMultiplexerClient()) {
            //qqqqqqqqqqq from Server to Client(s)  - there might not be any clients
            return null;
        }
        
        ClientSession session = hmClientSession.get(rmc);
        if (session == null) {
            return null;
        }
        
        Mapper mapper = session.hmClassToMapper.get(c);
        if (mapper == null) {
            synchronized (session.hmClassToMapper) {
                mapper = session.hmClassToMapper.get(c);
                if (mapper == null) {
                    mapper = new Mapper();
                    session.hmClassToMapper.put(c, mapper);
                }
            }
        }
        
        OAObjectKey keyServer = mapper.hmClientToServer.get(keyClient);
        return keyServer;
    }
}

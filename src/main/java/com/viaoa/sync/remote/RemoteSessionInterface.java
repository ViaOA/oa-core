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

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.sync.model.ClientInfo;


/**
 * Used to manage a Client session on the Server. 
 */
@OARemoteInterface()
public interface RemoteSessionInterface {
    
    /**
     * This is called when a new OAObject is created on the Client, 
     * so that the Server side Session can use it when filtering broadcast msgs.
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void objectCreated(long guid);

    /**
     * Called by client OAObject finalization, to remove guid from server side client session.
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void objectsFinalized(long[] guids);

    /**
     * Used to make sure that objects are stored in the server side and wont be GCd.
     * This is used when a client removes an OAObject from hubs, which means it might not be referenceable on the server (and get GC'd)
     * This will keep it referenceable on the server  
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void updateObjectsWithoutHubs(Class c, OAObjectKey ok, boolean bIsInHub);
    
    
    boolean setLock(Class objectClass, OAObjectKey objectKey, boolean bLock);
    boolean isLocked(Class objectClass, OAObjectKey objectKey);
    boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey);
    boolean isLockedByThisClient(Class objectClass, OAObjectKey objectKey);

    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void update(ClientInfo ci); 
    
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void sendException(String msg, Throwable ex);

    @OARemoteMethod(dontUseQueue=true)
    String ping(String msg);
    
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void ping2(String msg);
}

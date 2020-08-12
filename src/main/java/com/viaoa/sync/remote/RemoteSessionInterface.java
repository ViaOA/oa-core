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
import com.viaoa.remote.annotation.OARemoteInterface;
import com.viaoa.remote.annotation.OARemoteMethod;
import com.viaoa.sync.model.ClientInfo;


/**
 * session for client. 
 */
@OARemoteInterface()
public interface RemoteSessionInterface {
    
    /**
     * create a new object on the server,
     * and call addToCache
     */
    OAObject createNewObject(Class clazz);

    /**
     * Used to make sure that object is stored in the server side 
     * @param obj

     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void addToServerCache(OAObject obj);

    /**
     * Objects on server side cache, that are on client that might get gc'd on server.
     */
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void removeFromServerCache(int[] guids);

    
    boolean setLock(Class objectClass, OAObjectKey objectKey, boolean bLock);
    boolean isLocked(Class objectClass, OAObjectKey objectKey);
    boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey);
    boolean isLockedByThisClient(Class objectClass, OAObjectKey objectKey);

    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void update(ClientInfo ci); 
    
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void sendException(String msg, Throwable ex);

    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    /**
     * Objects that have been GDd on the client, so that the server can be remove them from
     * the session cache and guid tree of objects that have been sent to client - so they can be resent if needed again.
     */
    void removeGuids(int[] guids);
    
    @OARemoteMethod(dontUseQueue=true)
    String ping(String msg);
    
    @OARemoteMethod(noReturnValue=true, dontUseQueue=true)
    void ping2(String msg);
}

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
package com.viaoa.hub;

import com.viaoa.object.OAObject;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.util.OAFilter;

/**
 * This is used to listen to Hub for objects that match filter criteria and then call the onTrigger method.
 * @author vvia
 */
public abstract class HubTrigger<T extends OAObject> extends HubFilter<T> {
    private static final long serialVersionUID = 1L;
    
    public HubTrigger(Hub<T> hubMaster) {
        super(hubMaster, null);
    }
    public HubTrigger(Hub<T> hubMaster, OAFilter filter, String ... dependentPropertyPaths) {
        super(hubMaster, null, filter, dependentPropertyPaths);
    }

    @Override
    protected void addObject(T obj, boolean bIsInitialzing) {
        super.addObject(obj, bIsInitialzing);
        if (bIsInitialzing) return;
        onTrigger(obj);
    }
    @Override
    protected void removeObject(T obj) {
        super.removeObject(obj);
    }
    
    public abstract void onTrigger(T obj);
}

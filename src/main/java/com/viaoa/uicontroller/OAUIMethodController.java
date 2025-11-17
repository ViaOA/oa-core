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
package com.viaoa.uicontroller;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAStr;

/**
 * Controller that binds a UI action to a named method on the current
 * active {@link OAObject} in a {@link Hub}. The controller uses
 * {@link OAObjectCallback} rules to determine whether the method is
 * currently enabled or visible, and then invokes the method when the
 * UI component is activated.
 *
 * <p>
 * Responsibilities include:
 * </p>
 *
 * <ul>
 *   <li>Tracking the method name to be called on the active OAObject.</li>
 *   <li>Using {@link OAObjectCallbackDelegate} to evaluate AllowEnabled
 *       and AllowVisible callbacks.</li>
 *   <li>Invoking the method via reflection (using OAReflect) when allowed.</li>
 *   <li>Handling completion messages and failure responses for the UI.</li>
 * </ul>
 *
 * <p>
 * This controller is typically used for domain actions such as
 * {@code approve()}, {@code close()}, {@code submit()}, etc., where
 * business rules determine when the operation is allowed for the
 * active object.
 * </p>
 */
public class OAUIMethodController extends OAUIBaseController {
    private final String methodName;
    
    public OAUIMethodController(Hub hub, String methodName) {
        super(hub);
        this.methodName = methodName;
    }
    
    public String getMethodName() {
        return this.methodName;
    }
    
    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) return false;

        OAObject obj = (OAObject) hub.getAO();
        if (obj == null) return false;
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, getHub(), obj, getMethodName());
        return eq.getAllowed();
    }
    
        
    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(getHub(), (OAObject) hub.getAO(), getMethodName());
        return eq.getAllowed();
    }

    
    public boolean onCallMethod() {
        return onCallMethod(hub, (OAObject) hub.getAO());
    }

    public boolean onCallMethod(final Hub hub, final OAObject obj) {
        Response resp = new Response();
        _onCallMethod(hub, obj, resp);
        if (resp.bCompleted) {
            String msg = getCompletedMessage();
            if (OAStr.isNotEmpty(msg)) {
                onCompleted(msg, getTitle()); 
            }
        }
        return true;
    }
    
    private static class Response {
        boolean bCompleted;
        Object result;
    }
    
    private void _onCallMethod(final Hub hub, final OAObject obj, final Response resp) {
        OAObjectCallback cb; 
        String s;

        // 1: confirm
        cb = OAObjectCallbackDelegate.getConfirmCommandObjectCallback(obj, getMethodName(), getConfirmMessage(), getTitle());
        s = cb.getConfirmMessage();
        if (OAStr.isNotEmpty(s)) {
            if (!onConfirm(s, OAStr.notEmpty(cb.getConfirmTitle(), getTitle()) )) {
                resp.bCompleted = false;
            }
        }
        
        // 2: verify
        cb = OAObjectCallbackDelegate.getVerifyCommandObjectCallback(obj, getMethodName(), OAObjectCallback.CHECK_ALL);
        if (!cb.getAllowed()) {
            onError(cb.getResponse(), cb.getDisplayResponse());
            resp.bCompleted = false;
            return;
        }
            
        // 3: call method
        resp.result = OAReflect.executeMethod(obj, getMethodName());
        resp.bCompleted = true;
    }


}

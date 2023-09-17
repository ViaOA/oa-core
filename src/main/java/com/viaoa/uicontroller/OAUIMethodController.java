package com.viaoa.uicontroller;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAStr;

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

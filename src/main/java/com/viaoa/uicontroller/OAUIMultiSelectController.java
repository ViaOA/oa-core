package com.viaoa.uicontroller;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;

/**
 * HtmlSelect that allows picking multivalues/objects from a Hub. 
 */
public class OAUIMultiSelectController extends OAUIBaseController {

    public OAUIMultiSelectController(Hub hubSelect) {
        super(hubSelect);
    }

    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) return false;
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(getHub());
        return eq.getAllowed();
    }
    
    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(getHub());
        return eq.getAllowed();
    }

    
    /**
     * This can be used to get the confirm message before the actual new value is known.<br>
     * This is used to send a confirm message (javascript) to browser.
     */
    public OAObjectCallback getPreConfirmMessage() {
        OAObjectCallback cb = null; 
        //qqqqqq todo: need to confirm any add/remove
        return cb;
    }

    
}

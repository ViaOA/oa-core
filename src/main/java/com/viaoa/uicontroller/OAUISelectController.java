package com.viaoa.uicontroller;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.util.OAStr;

/**
 * Used by UI Components that use a Hub to present a list to choose from.
 * Once a value it selected then it will set the AO in the Hub.
 * If there is a Link Hub, then the new select Value will be verified before setting.
 * 
 * @see #onChangeAO(Object) to verify and set AO.
 * @author vince
 */
public class OAUISelectController extends OAUIBaseController {

    private final String linkPropertyName;
    private final Hub linkToHub;
    
    public OAUISelectController(Hub hub) {
        super(hub);
        this.linkToHub = hub.getLinkHub(true);
        this.linkPropertyName = hub.getLinkPath(true);
    }

    public Hub getLinkToHub() {
        return linkToHub;
    }
    public String getLinkPropertyName() {
        return this.linkPropertyName;
    }
    
    
    @Override
    public boolean isEnabled() {
        if (!super.isEnabled()) return false;

        if (linkToHub == null) {
            return getHub().isValid();
        }
        
        OAObject obj = (OAObject) linkToHub.getAO();
        if (obj == null) return false;
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, linkToHub, obj, linkPropertyName);
        return eq.getAllowed();
    }
    
    public boolean isEnabled(OAObject obj) {
        if (!super.isEnabled()) return false;

        if (linkToHub == null) {
            return getHub().isValid();
        }
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, linkToHub, obj, linkPropertyName);
        return eq.getAllowed();
    }
    
        
    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        if (linkToHub == null) return true;
        return isVisible((OAObject) linkToHub.getAO());
    }
    
    public boolean isVisible(OAObject linkToLinkObject) {
        if (linkToHub == null) return true;
        if (linkToLinkObject == null) return true;
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(linkToHub, linkToLinkObject, linkPropertyName);
        return eq.getAllowed();
    }

/*qqqqq    
    public boolean onChangeAO(final Object newValue) {
        if (_onChangeAO(newValue)) {
            String msg = getCompletedMessage();
            if (OAStr.isNotEmpty(msg)) {
                onCompleted(msg, getTitle()); 
            }
        }
        return true;
    }
*/
    
    public boolean onAOChange(final OAObject origLinkHubAO, final OAObject origValue, final Object newValue) {
        if (_onAOChange(origLinkHubAO, origValue, newValue)) {
            String msg = getCompletedMessage();
            if (OAStr.isNotEmpty(msg)) {
                onCompleted(msg, getTitle()); 
            }
        }
        return true;
    }
    
    
    private boolean _onAOChange(final OAObject origLinkHubAO, final OAObject origValue, final Object newValue) {
        boolean bChangeAO = true;

        if (linkToHub != null) {
            if (origLinkHubAO == null) return true;

            if (newValue == origValue) {
                return true;
            }
            
            if (origLinkHubAO.getProperty(linkPropertyName) != origValue) {
                // something else changed it
                return true; 
            }

            OAObjectCallback cb; 
            String s;
            
            // 1: confirm
            cb = OAObjectCallbackDelegate.getConfirmPropertyChangeObjectCallback(origLinkHubAO, linkPropertyName, newValue, getConfirmMessage(), getTitle());
            s = cb.getConfirmMessage();
            if (OAStr.isNotEmpty(s)) {
                if (!onConfirm(s, OAStr.notEmpty(cb.getConfirmTitle(), getTitle()) )) {
                    return false;
                }
            }
            
            // 2: verify
            cb = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, origLinkHubAO, linkPropertyName, origValue, newValue); 
            if (!cb.getAllowed()) {
                onError(cb.getResponse(), cb.getDisplayResponse());
                return false;
            }
        
            if (linkToHub.getAO() != origLinkHubAO) {
                // changing the AO would update the wrong object in the link hub.    
                bChangeAO = false;
            }
        }
        
        // 3: call method
        if (bChangeAO) {
            getHub().setAO(newValue);
            return true;
        }
        return false;
    }
    
    /**
     * This can be used to get the confirm message before the actual new value is known.<br>
     * This is used to send a confirm message (javascript) to browser.
     */
    public OAObjectCallback getPreConfirmMessage() {
        OAObjectCallback cb = null; 

        if (linkToHub != null) {
            final OAObject obj = (OAObject) linkToHub.getAO();
            cb = OAObjectCallbackDelegate.getPreConfirmPropertyChangeObjectCallback(obj, linkPropertyName, getConfirmMessage(), getTitle());
        }
        return cb;
    }

    
}

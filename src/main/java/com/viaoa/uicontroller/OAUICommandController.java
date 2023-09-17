package com.viaoa.uicontroller;

import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.OALogger;
import com.viaoa.util.OAStr;

/**
 * Controller used to have UI components commands interact with Hub and OAObjects.
 * <p>
 * 
 * @author vince
 */
public class OAUICommandController extends OAUIBaseController {
    private static final Logger LOG = OALogger.getLogger(OAUICommandController.class);

    private Command command;

    public static enum Command {
        /**
         * Misc command that uses a Hub or AO.
         * These should overwrite performCommand.
         */
        OtherUsesHub, OtherUsesAO,
        /**
         * Save the current object.
         */
        Save, // might want to use submit command instead of Save 
        /**
         * Nav commands for changing active object.
         */
        First(true), Last(true),
        Next(true), Previous(true), 
        
        /**
         * Delete the Hub.AO
         */
        Delete(true), 
        /**
         * Remove the Hub.AO
         */
        Remove(true),
        /**
         * Remove all objects in Hub.
         */
        RemoveAll(true),
        /**
         * Submit (save) the current object.
         * Uses the OAObject.isSubmitted to check.
         */
        Submit,

        /**
         * Create new object and add or insert.
         */
        InsertNew(true), AddNew(true),
        /**
         * Manually add or insert.  This will call getManualObject to supply the object to use.
         */
        NewManual(true), AddManual(true),
        /**
         * Manually change the Hub AO, by calling getManualObject to get the object to use.
         */
        ManualChangeAO(true),
        /**
         * Set Hub AO to null.
         */
        ClearAO(true), 
        /**
         * Used to go to the Hub AO.
         */
        GoTo, 
        HubSearch(true), 
        Search,
        /**
         * Creates a copy of the current AO and adds to Hub.
         */
        Copy,
        Select, 
        /**
         * Calls OAObject.refresh on the current AO.
         */
        Refresh;
        
        
        private boolean bChangesAO;
        
        private Command() {
        }
        
        private Command(boolean changesAO) {
            this.bChangesAO = changesAO;
        }
        
        public boolean getChangesAO() {
            return bChangesAO;
        }
    }

    public OAUICommandController(Hub hub, Command command) {
        super(hub);
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public boolean isEnabled() {
        Hub h = getHub();
        return isEnabled(h, (OAObject) h.getAO());
    }    
    
    public boolean isEnabled(final Hub hub, final OAObject obj) {
        if (!hub.isValid()) return false;
        
        if (command.getChangesAO()) {
            Hub hubLink = hub.getLinkHub(true);
            if (hubLink != null) {
                if (obj == null) return false;
                else {
                    if (!obj.isEnabled(hub.getLinkPath(true))) return false;                    
                }
            }
        }
        
        final int hubSize = hub.getSize();
        final int pos = hub.getPos();
        OAObjectCallback cb = null; 
        
        switch (command) {
        case OtherUsesHub:
            return hub.isValid(); 
        case OtherUsesAO:
            return hub.getAO() != null; 
        case Save:
            cb = OAObjectCallbackDelegate.getAllowSaveObjectCallback(obj, OAObjectCallback.CHECK_ALL);
            break;
        case First:
            if (hubSize == 0) return false;
            break;
        case Last:
            if (hubSize == 0) return false;
            break;
        case Next:
            if (pos+1 >= hubSize) return false;
            break;
        case Previous:
            if (pos <= 0) return false;
            break;
        case Delete:
            if (pos < 0) return false;
            cb = OAObjectCallbackDelegate.getAllowDeleteObjectCallback((OAObject) hub.getAO());
            break;
        case Remove:
            if (pos < 0) return false;
            cb = OAObjectCallbackDelegate.getAllowRemoveObjectCallback(hub, (OAObject) hub.getAO(), OAObjectCallback.CHECK_ALL);
            break;
        case RemoveAll:
            cb = OAObjectCallbackDelegate.getAllowRemoveAllObjectCallback(hub, OAObjectCallback.CHECK_ALL);
            break;
        case Submit:
            if (obj == null) return false;
            if (!obj.isSubmitted()) return false;
            cb = obj.getAllowSubmit();
            break;
        case InsertNew:
            cb = OAObjectCallbackDelegate.getAllowNewObjectCallback(hub);
            break;
        case AddNew:
            cb = OAObjectCallbackDelegate.getAllowNewObjectCallback(hub);
            break;
        case NewManual:
        case AddManual:
            cb = OAObjectCallbackDelegate.getAllowNewObjectCallback(hub);
            break;
        case ManualChangeAO:
            break;
        case ClearAO:
            return pos >= 0;
        case GoTo:
            return hub.isValid();
        case HubSearch:
            return hubSize > 0;
        case Search:
            break;
        case Copy:
            cb = OAObjectCallbackDelegate.getAllowAddObjectCallback(hub, obj, OAObjectCallback.CHECK_ALL);
            if (cb.getAllowed()) {
                cb = OAObjectCallbackDelegate.getAllowCopyObjectCallback(obj);
            }
            break;
            
        case Select:
            break;
        case Refresh:
            return pos >= 0;
        default:
            LOG.warning("Unhandled command "+command+" for OAUICommandController");
        }
        return cb == null || cb.getAllowed();
    }
    
    public boolean onCommand() {
        final OAObject obj = (OAObject) hub.getAO();
        if (_onCommand(hub, obj)) {
            String msg = getCompletedMessage();
            if (OAStr.isNotEmpty(msg)) {
                onCompleted(msg, getTitle()); 
            }
        }
        return true;
    }
    
    private boolean _onCommand(final Hub hub, final OAObject obj) {
        OAObjectCallback cb; 
        OAObject newObject = null;
        String s;
        boolean bUseNewObject = false;
        
        // Step 1: get or create newObject
        cb = null;
        switch (command) {
        case OtherUsesHub:
        case OtherUsesAO:
            break;
        case First:
            newObject = (OAObject) hub.getAt(0);
            bUseNewObject = true;
            break;
        case Last:
            newObject = (OAObject) hub.getAt(hub.getSize()-1);
            bUseNewObject = true;
            break;
        case Next:
            newObject = (OAObject) hub.getAt(hub.getPos()+1);
            bUseNewObject = true;
            break;
        case Previous:
            newObject = (OAObject) hub.getAt(hub.getPos()-1);
            bUseNewObject = true;
            break;
        case Delete:
            break;
        case Remove:
            break;
        case RemoveAll:
            newObject = null;
            bUseNewObject = true;
            break;
        case Submit:
            break;
        case InsertNew:
        case AddNew:
            newObject = (OAObject) OAObjectReflectDelegate.createNewObject(hub.getObjectClass());
            bUseNewObject = true;
            break;
        case NewManual:
            newObject = (OAObject) getManualObject();
            bUseNewObject = true;
            break;
        case AddManual:
            newObject = (OAObject) getManualObject();
            bUseNewObject = true;
            break;
        case ManualChangeAO:
            newObject = (OAObject) getManualObject();
            bUseNewObject = true;
            break;
        case ClearAO:
            newObject = null;
            bUseNewObject = true;
            break;
        case GoTo:
            break;
        case HubSearch:
            break;
        case Search:
            break;
        case Copy:
            newObject = OAObjectCallbackDelegate.getCopy(obj);
            bUseNewObject = true;
            break;
        case Select:
            break;
        case Refresh:
            break;
        }
        
        // Step 2: check to see if there is a link hub change
        cb = null;
        if (command.getChangesAO()) {
            Hub hubLink = hub.getLinkHub(true);
            if (hubLink != null) {
                final OAObject objx = (OAObject) hubLink.getAO();
                if (objx == null) {
                    onError("Link to hub AO is null", "");
                    return false;
                }
                
                final String propx = hub.getLinkPath(true);
                
                cb = objx.getIsValidPropertyChangeObjectCallback(propx, newObject);
                if (!cb.getAllowed()) {
                    onError(cb.getResponse(), cb.getDisplayResponse());
                    return false;
                }
                
                cb = OAObjectCallbackDelegate.getConfirmPropertyChangeObjectCallback(objx, propx, newObject, getConfirmMessage(), getTitle());
                s = cb.getConfirmMessage();
                if (OAStr.isNotEmpty(s)) {
                    if (!onConfirm(s, OAStr.notEmpty(cb.getConfirmTitle(), getTitle()) )) return false;
                }
            }
        }
        
        // Step 3: confirm
        cb = null;

        switch (command) {
        case OtherUsesHub:
        case OtherUsesAO:
            break;
        case Save:
            cb = OAObjectCallbackDelegate.getConfirmSaveObjectCallback(obj, getConfirmMessage(), getTitle());
            break;
        case First:
            break;
        case Last:
            break;
        case Next:
            break;
        case Previous:
            break;
        case Delete:
            cb = OAObjectCallbackDelegate.getConfirmDeleteObjectCallback(obj, getConfirmMessage(), getTitle());
            break;
        case Remove:
            cb = OAObjectCallbackDelegate.getConfirmRemoveObjectCallback(hub, obj, getConfirmMessage(), getTitle());
            break;
        case RemoveAll:
            cb = OAObjectCallbackDelegate.getConfirmRemoveAllObjectCallback(hub, getConfirmMessage(), getTitle());
            break;
        case InsertNew:
            cb = OAObjectCallbackDelegate.getConfirmAddObjectCallback(hub, newObject, getConfirmMessage(), getTitle());
            break;
        case AddNew:
            cb = OAObjectCallbackDelegate.getConfirmAddObjectCallback(hub, newObject, getConfirmMessage(), getTitle());
            break;
        case NewManual:
            cb = OAObjectCallbackDelegate.getConfirmAddObjectCallback(hub, newObject, getConfirmMessage(), getTitle());
            break;
        case AddManual:
            cb = OAObjectCallbackDelegate.getConfirmAddObjectCallback(hub, newObject, getConfirmMessage(), getTitle());
            break;
        case ManualChangeAO:
            break;
        case ClearAO:
            break;
        case GoTo:
            break;
        case HubSearch:
            break;
        case Search:
            break;
        case Copy:
            cb = OAObjectCallbackDelegate.getConfirmAddObjectCallback(hub, newObject, getConfirmMessage(), getTitle());
            break;
        case Select:
            break;
        case Refresh:
            break;
        default:
            LOG.warning("Unhandled command "+command+" for OAUICommandController");
        }

        if (cb != null) {
            s = cb.getConfirmMessage();
            if (OAStr.isNotEmpty(s)) {
                if (!onConfirm(s, OAStr.notEmpty(cb.getConfirmTitle(), getTitle()) )) return false;
            }
        }
        else {
            s = this.getConfirmMessage();
            if (OAStr.isNotEmpty(s)) {
                if (!onConfirm(s, getTitle())) return false;
            }
        }
        
        // Step 4: actual command
        return performCommand(hub, bUseNewObject ? newObject : obj);
    }


    
    /**
     * This is called to perform the actual command,
     * after calling onConfirm, and before calling onCompleted.
     * <p>
     * This should be overwritten for Commands:
     * OtherUsesHub, OtherUsesAO
     * <p>
     * Call onError if needed.
     * @return true if command was performed, false otherwise.
     */
    protected boolean performCommand(final Hub hub, final OAObject obj) {
        switch (command) {
        case OtherUsesHub:
        case OtherUsesAO:
            break;
        case Save:
            if (obj == null) return false;
            obj.save();
            break;
        case First:
            hub.setPos(0);
            break;
        case Last:
            hub.setPos(hub.getSize()-1);
            break;
        case Next:
            hub.setPos(hub.getPos()+1);
            break;
        case Previous:
            hub.setPos(hub.getPos()-1);
            break;
        case Delete:
            if (obj == null) return false;
            obj.delete();
            break;
        case Remove:
            hub.remove(obj);
            break;
        case RemoveAll:
            hub.removeAll();
            break;
        case Submit:
            if (obj == null) return false;
            obj.save();
            break;
        case InsertNew:
            hub.insert(obj, hub.getPos());
            hub.setAO(obj);
            break;
        case AddNew:
            hub.add(obj);
            hub.setAO(obj);
            break;
        case NewManual:
            hub.add(obj);
            hub.setAO(obj);
            break;
        case AddManual:
            hub.add(obj);
            hub.setAO(obj);
            break;
        case ManualChangeAO:
            hub.setAO(obj);
            break;
        case ClearAO:
            hub.setPos(-1);
        case GoTo:
            break;
        case HubSearch:
            break;
        case Search:
            break;
        case Copy:
            hub.add(obj);
            hub.setAO(obj);
            break;
        case Select:
            break;
        case Refresh:
            if (obj != null) obj.refresh();
            break;
        default:
            LOG.warning("Unhandled command "+command+" for OAUICommandController, title="+getTitle());
        }
        
        return true;
    }
    
    /**
     * These allow for overwriting to handle user interactions.
     */
    protected boolean onConfirm(String confirmMessage, String title) {
        return true;
    }

    protected void onError(String errorMessage, String detailMessage) {
    }
    
    protected void onCompleted(String completedMessage, String title) {
    }

    /**
     * For commands that will be manually assigning an object.
     * <br>
     * For Commands NewManual, AddManual, and ManualChangeAO
     * <p>
     * This should be overwritten to supply the object to use. 
     */
    protected Object getManualObject() {
        return null;
    }
}

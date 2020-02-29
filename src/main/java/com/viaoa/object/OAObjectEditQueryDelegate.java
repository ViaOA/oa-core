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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.swing.JLabel;

import com.viaoa.context.OAContext;
import com.viaoa.context.OAUserAccess;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubChangeListener;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OAObjectEditQuery.Type;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Allows OA to be able to control permission to object/hub, 
 * and allow other code/compenents to interact with objects.  
 * 
 * Works with OAObject and Hub to determine what is allowed/permitted.
 * Uses OAObject annoations, specific methods (onEditQuery*, *Callback), and HubListeners.
 * 
 * Used to query objects, and find out if certain functions are enabled/visible/allowed,
 * along with other interactive settings/data.
 *
 * Used by OAObject (beforePropChange), Hub (add/remove/removeAll) to check if method is permitted/enabled.
 * Used by OAJfcController and Jfc to set UI components (enabled, visible, tooltip, rendering, etc)
 * 
 * @see OAObjectEditQuery for list of types that can be used.
 * @see OAEditQuery annotation that lists proppaths and values used for enabled/visible.
 * @see OAAnnotationDelegate to see how class and annotation information is stored in Info objects (class/prop/calc/link/method)
 * @author vvia
 */
public class OAObjectEditQueryDelegate {
    private static Logger LOG = Logger.getLogger(OAObjectEditQueryDelegate.class.getName());
    
    public static boolean getAllowVisible(Hub hub, OAObject obj, String name) {
        return getAllowVisibleEditQuery(hub, obj, name).getAllowed();
    }
    
    public static boolean getAllowEnabled(int checkType, Hub hub, OAObject obj, String name) {
        return getAllowEnabledEditQuery(checkType, hub, obj, name).getAllowed();
    }

    public static boolean getAllowCopy(OAObject oaObj) {
        if (oaObj == null) return false;
        return getAllowCopyEditQuery(oaObj).getAllowed();
    }
    public static OAObject getCopy(OAObject oaObj) {
        if (oaObj == null) return null;
        OAObjectEditQuery eq = getCopyEditQuery(oaObj);

        Object objx = eq.getValue();
        if (!(objx instanceof OAObject)) {
            if (!eq.getAllowed()) return null;
            objx = oaObj.createCopy();
        }
        
        getAfterCopyEditQuery(oaObj, (OAObject) objx);
        return (OAObject) objx;
    }
    /*
    public static void afterCopy(OAObject oaObj, OAObject oaObjCopy) {
        if (oaObj == null || oaObjCopy == null) return;
        getAfterCopyEditQuery(oaObj, oaObjCopy);
    }
    */
    
    // OAObjectEditQuery.CHECK_*
    public static boolean getVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue) {
        return getVerifyPropertyChangeEditQuery(checkType, obj, propertyName, oldValue, newValue).getAllowed();
    }
    
    public static boolean getAllowAdd(Hub hub, OAObject obj, int checkType) {
        return getAllowAddEditQuery(hub, obj, checkType).getAllowed();
    }
    
    public static boolean getVerifyAdd(Hub hub, OAObject obj, int checkType) {
        return getVerifyAddEditQuery(hub, obj, checkType).getAllowed();
    }
    
    public static boolean getAllowRemove(Hub hub, OAObject obj, int checkType) {
        return getAllowRemoveEditQuery(hub, obj, checkType).getAllowed();
    }
    public static boolean getVerifyRemove(Hub hub, OAObject obj, int checkType) {
        return getVerifyRemoveEditQuery(hub, obj, checkType).getAllowed();
    }
    
    public static boolean getAllowRemoveAll(Hub hub, int checkType) {
        return getAllowRemoveAllEditQuery(hub, checkType).getAllowed();
    }
    public static boolean getVerifyRemoveAll(Hub hub, int checkType) {
        return getVerifyRemoveAllEditQuery(hub, checkType).getAllowed();
    }
    
    public static boolean getAllowDelete(Hub hub, OAObject obj, int checkType) {
        return getAllowDeleteEditQuery(hub, obj, checkType).getAllowed();
    }
    public static boolean getVerifyDelete(Hub hub, OAObject obj, int checkType) {
        return getVerifyDeleteEditQuery(hub, obj, checkType).getAllowed();
    }
    
    public static boolean getAllowSave(OAObject obj, int checkType) {
        return getAllowSaveEditQuery(obj, checkType).getAllowed();
    }
    public static boolean getVerifySave(OAObject obj, int checkType) {
        return getVerifySaveEditQuery(obj, checkType).getAllowed();
    }

    
    public static String getFormat(OAObject obj, String propertyName, String defaultFormat) {
        OAObjectEditQuery em = new OAObjectEditQuery(Type.GetFormat);
        em.setObject(obj);
        em.setFormat(defaultFormat);
        callEditQueryMethod(em);
        em.setPropertyName(propertyName);
        callEditQueryMethod(em);
        return em.getFormat();
    }
    
    
    public static String getToolTip(OAObject obj, String propertyName, String defaultToolTip) {
        OAObjectEditQuery em = new OAObjectEditQuery(Type.GetToolTip);
        em.setObject(obj);
        em.setToolTip(defaultToolTip);
        callEditQueryMethod(em);
        em.setPropertyName(propertyName);
        callEditQueryMethod(em);
        return em.getToolTip();
    }
    public static void renderLabel(OAObject obj, String propertyName, JLabel label) {
        OAObjectEditQuery em = new OAObjectEditQuery(Type.RenderLabel);
        em.setObject(obj);
        em.setLabel(label);
        callEditQueryMethod(em);
        em.setPropertyName(propertyName);
        callEditQueryMethod(em);
    }
    public static void updateLabel(OAObject obj, String propertyName, JLabel label) {
        OAObjectEditQuery em = new OAObjectEditQuery(Type.UpdateLabel);
        em.setObject(obj);
        em.setPropertyName(propertyName);
        em.setLabel(label);
        callEditQueryMethod(em);
    }


    
    public static OAObjectEditQuery getAllowVisibleEditQuery(Hub hub, OAObject oaObj, String name) {
        if (hub == null && oaObj == null) return null;
        if (oaObj == null) {
            if (name == null) {
                name = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
                oaObj = hub.getMasterObject();
            }
            else {
                oaObj = (OAObject) hub.getAO();
            }
        }
        OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AllowVisible, OAObjectEditQuery.CHECK_ALL, hub, null, oaObj, name, null);
        processEditQuery(editQuery);
        return editQuery;
    }
    public static OAObjectEditQuery getAllowEnabledEditQuery(final int checkType, final Hub hub, OAObject oaObj, String name) {
        if (hub == null && oaObj == null) return null;
        if (oaObj == null) {
            if (name == null) {
                name = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
                oaObj = hub.getMasterObject();
            }
            else {
                oaObj = (OAObject) hub.getAO();
            }
        }
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AllowEnabled, checkType, hub, null, oaObj, name, null);
        processEditQuery(editQuery);
        return editQuery;
    }

    
    
    
    public static OAObjectEditQuery getAllowEnabledEditQuery(Hub hub) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AllowEnabled);

        OAObject objMaster = hub.getMasterObject();
        if (objMaster == null) {
            processEditQueryForHubListeners(editQuery, hub, null, null, null, null);
        }
        else {
            String propertyName = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
            editQuery.setPropertyName(propertyName);
            editQuery.setObject(objMaster);
            processEditQuery(editQuery);
        }
        return editQuery;
    }
    public static OAObjectEditQuery getAllowCopyEditQuery(final OAObject oaObj) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AllowCopy, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, null, null);
        processEditQuery(editQuery);
        return editQuery;
    }
    public static OAObjectEditQuery getCopyEditQuery(final OAObject oaObj) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.GetCopy, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, null, null);
        processEditQuery(editQuery);
        return editQuery;
    }
    public static OAObjectEditQuery getAfterCopyEditQuery(final OAObject oaObj, final OAObject oaObjCopy) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AfterCopy, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, null, oaObjCopy);
        processEditQuery(editQuery);
        return editQuery;
    }

    public static OAObjectEditQuery getVerifyPropertyChangeEditQuery(final int checkType, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.VerifyPropertyChange, checkType, null, null, oaObj, propertyName, newValue);
        editQuery.setOldValue(oldValue);
        processEditQuery(editQuery);
        return editQuery;
    }
    public static OAObjectEditQuery getVerifyCommandEditQuery(final OAObject oaObj, final String methodName, int checkType) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.VerifyCommand, checkType, null, null, oaObj, methodName, null);
        processEditQuery(editQuery);
        return editQuery;
    }
    
    public static void updateEditProcessed(OAObjectEditQuery editQuery) {
        if (editQuery == null) return;
        if (!OAContext.getAllowEditProcessed()) {
            String sx = OAContext.getAllowEditProcessedPropertyPath();
            editQuery.setResponse("User."+sx+"=false");
            editQuery.setAllowed(false);
        }
    }
    
    
    
    
    public static OAObjectEditQuery getAllowAddEditQuery(final Hub hub, OAObject objAdd, final int checkType) {
        if (hub == null) return null;
        
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.AllowAdd, checkType, hub, null, null, null, objAdd);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, objAdd);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo();
            if (liRev != null && !liRev.getCalculated()) { 
                editQuery = new OAObjectEditQuery(Type.AllowAdd, checkType, hub, null, objMaster, liRev.getName(), objAdd);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) editQuery = new OAObjectEditQuery(Type.AllowAdd, checkType, hub, null, null, null, objAdd);
        return editQuery;
    }

    public static OAObjectEditQuery getVerifyAddEditQuery(final Hub hub, final OAObject oaObj, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();
        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.VerifyAdd, checkType, hub, null, oaObj, null, null);
            processEditQueryForHubListeners(editQuery, hub, oaObj, null, null, null);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo();
            if (liRev != null && !liRev.getCalculated()) { 
                editQuery = new OAObjectEditQuery(Type.VerifyAdd, checkType, hub, null, objMaster, liRev.getName(), oaObj);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) {
            editQuery = new OAObjectEditQuery(Type.VerifyAdd, checkType, hub, null, null, null, oaObj);
            processEditQuery(editQuery);
        }
        return editQuery;
    }

    public static OAObjectEditQuery getAllowNewEditQuery(final Class clazz) {
        if (clazz == null) return null;
        
        OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AllowVisible, OAObjectEditQuery.CHECK_Processed, null, clazz, null, null, null);
        processEditQuery(editQuery);
        if (editQuery.getAllowed()) {
            editQuery = new OAObjectEditQuery(Type.AllowNew, OAObjectEditQuery.CHECK_UserEnabledProperty, null, clazz, null, null, null);
            processEditQuery(editQuery);
        }
        return editQuery;
    }
    
    
    public static OAObjectEditQuery getAllowNewEditQuery(final Hub hub, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.AllowNew, checkType, hub, null, null, null, null);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, null);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo();
            if (liRev != null && !liRev.getCalculated()) { 
                editQuery = new OAObjectEditQuery(Type.AllowNew, checkType, hub, null, objMaster, liRev.getName(), null);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) {
            editQuery = new OAObjectEditQuery(Type.AllowNew, checkType, hub, null, null, null, null);
            processEditQuery(editQuery);
        }
        return editQuery;
    }
    
    
    public static OAObjectEditQuery getAllowRemoveEditQuery(final Hub hub, final OAObject objRemove, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, objRemove);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo(); 
            if (liRev != null && !li.getCalculated()) {
                editQuery = new OAObjectEditQuery(Type.AllowRemove, checkType, hub, null, objMaster, liRev.getName(), objRemove);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) editQuery = new OAObjectEditQuery(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
        return editQuery;
    }

    public static OAObjectEditQuery getVerifyRemoveEditQuery(final Hub hub, final OAObject objRemove, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, objRemove);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo(); 
            if (liRev != null && !li.getCalculated()) {
                editQuery = new OAObjectEditQuery(Type.AllowRemove, checkType, hub, null, objMaster, liRev.getName(), objRemove);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) editQuery = new OAObjectEditQuery(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
        return editQuery;
    }

    
    public static OAObjectEditQuery getAllowRemoveAllEditQuery(final Hub hub, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.AllowRemoveAll, checkType, hub, null, null, null, null);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, null);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo(); 
            if (liRev != null && !li.getCalculated()) {
                editQuery = new OAObjectEditQuery(Type.AllowRemoveAll, checkType, hub, null, objMaster, liRev.getName(), null);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) editQuery = new OAObjectEditQuery(Type.AllowRemoveAll, checkType, hub, null, null, null, null);
        return editQuery;
    }

    public static OAObjectEditQuery getVerifyRemoveAllEditQuery(final Hub hub, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.VerifyRemoveAll, checkType, hub, null, null, null, null);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, null);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo(); 
            if (liRev != null && !li.getCalculated()) {
                editQuery = new OAObjectEditQuery(Type.VerifyRemoveAll, checkType, hub, null, objMaster, liRev.getName(), null);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) editQuery = new OAObjectEditQuery(Type.VerifyRemoveAll, checkType, hub, null, null, null, null);
        return editQuery;
    }
    

    public static OAObjectEditQuery getAllowSaveEditQuery(final OAObject oaObj, final int checkType) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.AllowSave, checkType, null, null, oaObj, null, null);
        processEditQuery(editQuery);
        return editQuery;
    }
    public static OAObjectEditQuery getVerifySaveEditQuery(final OAObject oaObj, final int checkType) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.VerifySave, checkType, null, null, oaObj, null, null);
        processEditQuery(editQuery);
        return editQuery;
    }

    
    
    
    
    public static OAObjectEditQuery getAllowDeleteEditQuery(final Hub hub, final OAObject objDelete, final int checkType) {
        if (hub == null) return null;

        OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
        OAObject objMaster = hub.getMasterObject();

        OAObjectEditQuery editQuery = null;
        
        if (li == null || (li.getPrivateMethod() && objMaster == null)) {
            editQuery = new OAObjectEditQuery(Type.AllowDelete, checkType, hub, null, null, null, objDelete);
            if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                if (hub.getOAObjectInfo().getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            processEditQueryForHubListeners(editQuery, hub, null, null, null, null);
        }
        else {
            OALinkInfo liRev = li.getReverseLinkInfo(); 
            if (liRev != null && !li.getCalculated()) {
                editQuery = new OAObjectEditQuery(Type.AllowDelete, checkType, hub, null, objMaster, liRev.getName(), objDelete);
                processEditQuery(editQuery);
            }
        }
        if (editQuery == null) {
            editQuery = new OAObjectEditQuery(Type.AllowDelete, checkType, hub, null, null, null, objDelete);
            processEditQuery(editQuery);
        }
        return editQuery;
    }
    
    public static OAObjectEditQuery getVerifyDeleteEditQuery(final Hub hub, final OAObject objDelete, final int checkType) {

        OAObjectEditQuery editQuery = null;
        if (hub != null) {
            OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
            OAObject objMaster = hub.getMasterObject();
    
            if (li == null || (li.getPrivateMethod() && objMaster == null)) {
                editQuery = new OAObjectEditQuery(Type.VerifyDelete, checkType, hub, null, null, null, objDelete);
                if ( (checkType & OAObjectEditQuery.CHECK_Processed) > 0) {
                    if (hub.getOAObjectInfo().getProcessed()) {
                        updateEditProcessed(editQuery);
                    }
                }
                processEditQueryForHubListeners(editQuery, hub, objDelete, null, null, null);
            }
            else {
                OALinkInfo liRev = li.getReverseLinkInfo(); 
                if (liRev != null && !li.getCalculated()) {
                    editQuery = new OAObjectEditQuery(Type.VerifyDelete, checkType, hub, null, objMaster, liRev.getName(), objDelete);
                    processEditQuery(editQuery);
                }
            }
        }
        if (editQuery == null) {
            editQuery = new OAObjectEditQuery(Type.VerifyDelete, checkType, hub, null, objDelete, null, null);
            processEditQuery(editQuery);
        }
        return editQuery;
    }

    
//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq

    public static OAObjectEditQuery getConfirmPropertyChangeEditQuery(final OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.SetConfirmForPropertyChange, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, property, newValue);
//qqqqqqqqqqqqqqqqqqq needs to include OldValue qqqqqqqqqqqqq        
        editQuery.setValue(newValue);
        editQuery.setPropertyName(property);
        editQuery.setConfirmMessage(confirmMessage);
        editQuery.setConfirmTitle(confirmTitle);
        
        processEditQuery(editQuery);
        return editQuery;
    }
    public static OAObjectEditQuery getConfirmCommandEditQuery(final OAObject oaObj, String methodName, String confirmMessage, String confirmTitle) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.SetConfirmForCommand, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, methodName, null);
        editQuery.setConfirmMessage(confirmMessage);
        editQuery.setConfirmTitle(confirmTitle);
        
        processEditQuery(editQuery);
        return editQuery;
    }

    public static OAObjectEditQuery getConfirmSaveEditQuery(final OAObject oaObj, String confirmMessage, String confirmTitle) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.SetConfirmForSave, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, null, null);
        editQuery.setConfirmMessage(confirmMessage);
        editQuery.setConfirmTitle(confirmTitle);
        
        processEditQuery(editQuery);
        return editQuery;
    }

    public static OAObjectEditQuery getConfirmDeleteEditQuery(final OAObject oaObj, String confirmMessage, String confirmTitle) {
        final OAObjectEditQuery editQuery = new OAObjectEditQuery(Type.SetConfirmForDelete, OAObjectEditQuery.CHECK_ALL, null, null, oaObj, null, null);
        editQuery.setConfirmMessage(confirmMessage);
        editQuery.setConfirmTitle(confirmTitle);
        processEditQuery(editQuery);
        return editQuery;
    }
    
    public static OAObjectEditQuery getConfirmRemoveEditQuery(final Hub hub, final OAObject oaObj, String confirmMessage, String confirmTitle) {
        OAObjectEditQuery editQuery;
        OAObject objMaster = hub.getMasterObject();
        if (objMaster != null) {
            String propertyName = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
            editQuery = new OAObjectEditQuery(Type.SetConfirmForRemove, OAObjectEditQuery.CHECK_ALL, hub, null, oaObj, propertyName, null);
            editQuery.setConfirmMessage(confirmMessage);
            editQuery.setConfirmTitle(confirmTitle);
            processEditQuery(editQuery);
        }
        else {
            editQuery = new OAObjectEditQuery(Type.SetConfirmForRemove, OAObjectEditQuery.CHECK_ALL, hub, null, oaObj, null, null);
            editQuery.setConfirmMessage(confirmMessage);
            editQuery.setConfirmTitle(confirmTitle);
        }
        return editQuery;
    }

    public static OAObjectEditQuery getConfirmAddEditQuery(final Hub hub, final OAObject oaObj, String confirmMessage, String confirmTitle) {
        OAObjectEditQuery editQuery;
        OAObject objMaster = hub.getMasterObject();
        if (objMaster != null) {
            String propertyName = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
            editQuery = new OAObjectEditQuery(Type.SetConfirmForAdd, OAObjectEditQuery.CHECK_ALL, hub, null, oaObj, propertyName, null);
            editQuery.setConfirmMessage(confirmMessage);
            editQuery.setConfirmTitle(confirmTitle);
            processEditQuery(editQuery);
        }
        else {
            editQuery = new OAObjectEditQuery(Type.SetConfirmForAdd, OAObjectEditQuery.CHECK_ALL, hub, null, oaObj, null, null);
            editQuery.setConfirmMessage(confirmMessage);
            editQuery.setConfirmTitle(confirmTitle);
        }
        return editQuery;
    }

/*qqqqqqq    
    protected static void processEditQuery(OAObjectEditQuery editQuery, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
        processEditQuery(editQuery, null, null, oaObj, propertyName, oldValue, newValue, false);
    }
    protected static void processEditQuery(OAObjectEditQuery editQuery, final Class<? extends OAObject> clazz, final String propertyName, final Object oldValue, final Object newValue) {
        processEditQuery(editQuery, null, clazz, null, propertyName, oldValue, newValue, false);
    }
*/    
    protected static void processEditQuery(OAObjectEditQuery editQuery) {
int xx = 4;
xx++;
        _processEditQuery(editQuery);
        if (DEMO_AllowAllToPass) {
            editQuery.setThrowable(null);
            editQuery.setAllowed(true);
        }
        else if ((!editQuery.getAllowed() || editQuery.getThrowable() != null)) {
            // allow AppUser.admin=true to always be valid
            if (OAContext.isSuperAdmin()) {  // allow all if super admin
                editQuery.setThrowable(null);
                editQuery.setAllowed(true);
            }
        }
    }
    private static boolean DEMO_AllowAllToPass;
    public static void demoAllowAllToPass(boolean b) {
        String msg = "WARNING: OAObjectEditQueryDelegate.demoAllowAllToPass="+b;
        if (b) msg += " - all OAObjectEditQuery will be allowed";
        LOG.warning(msg);
        for (int i=0; i<20; i++) {
            System.out.println(msg);
            if (!b) break;
        }
        OAObjectEditQueryDelegate.DEMO_AllowAllToPass = b;
    }
    
    
    /** 
     * This will process an Edit Query, calling editQuery methods on OAObject, properties, links, methods (depending on type of edit query)
     * 
     *  used by:
     *      OAJfcController to see if an UI component should be enabled
     *      OAObjetEventDelegate.fireBeforePropertyChange
     *      Hub add/remove/removeAll 
     *      OAJaxb
     */
    protected static void _processEditQuery(final OAObjectEditQuery editQuery) {
        final Hub hubThis = editQuery.getHub();
        final Class clazz = editQuery.getCalcClass();
        final OAObject oaObj = editQuery.getObject();
        final String propertyName = editQuery.getPropertyName();
        final Object oldValue = editQuery.getOldValue();
        final Object value = editQuery.getValue();
        final int checkType = editQuery.getCheckType();
        
        final boolean bCheckProcessedCheck = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_Processed) != 0;
        final boolean bCheckEnabledProperty = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_EnabledProperty) != 0;
        final boolean bCheckUserEnabledProperty = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_UserEnabledProperty) != 0;
        final boolean bCheckCallbackMethod = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_CallbackMethod) != 0;
        final boolean bCheckIncludeMaster = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_IncludeMaster) != 0;

        final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
        
        if (bCheckProcessedCheck) {
            if (editQuery.getType() == Type.AllowNew) {
                if (oi.getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
            else if (editQuery.getType() == Type.AllowDelete && value != null) {
                OAObjectInfo oix = OAObjectInfoDelegate.getOAObjectInfo(value.getClass());
                if (oi.getProcessed()) {
                    updateEditProcessed(editQuery);
                }
            }
        }
  
        // 20200217 add OAUserAccess
        if (editQuery.getType() == Type.AllowEnabled || editQuery.getType() == Type.AllowVisible) {
            final OAUserAccess userAccess = OAContext.getContextUserAccess();
            if (userAccess != null) {
                boolean bx = true;
                if (editQuery.getType() == Type.AllowEnabled) {
                    if (oaObj != null) bx = userAccess.getEnabled(oaObj);
                    else bx = userAccess.getEnabled(clazz);
                }
                else {
                    if (oaObj != null) bx = userAccess.getVisible(oaObj);
                    else bx = userAccess.getVisible(clazz); 
                }
                if (!bx) {
                    editQuery.setAllowed(false);
                    editQuery.setResponse("UserAccess returned false");
                    return; 
                }
            }
        }
        
        
        // follow the first link (if any), if it is not owner
        if (bCheckIncludeMaster && hubThis != null && (editQuery.getType() == Type.AllowEnabled || editQuery.getType().checkEnabledFirst || editQuery.getType() == Type.AllowVisible)) {
            OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterHubToDetail(hubThis);
            if (li != null && !li.getOwner()) {
                OAObject objx = hubThis.getMasterObject();
                if (objx != null) {
                    if (editQuery.getType() == Type.AllowEnabled || editQuery.getType().checkEnabledFirst) {
                        OAObjectEditQuery editQueryX = new OAObjectEditQuery(Type.AllowEnabled, (editQuery.getCheckType() ^ editQuery.CHECK_IncludeMaster), hubThis.getMasterHub(), null, objx, li.getName(), null);
                        editQueryX.setAllowed(editQuery.getAllowed());
                        _processEditQuery(editQueryX);
                        editQuery.setAllowed(editQueryX.getAllowed());
                        if (OAString.isEmpty(editQuery.getResponse())) editQuery.setResponse(editQueryX.getResponse());
                    }
                    else if (editQuery.getType() == Type.AllowVisible) {
                        OAObjectEditQuery editQueryX = new OAObjectEditQuery(Type.AllowVisible, (editQuery.getCheckType() ^ editQuery.CHECK_IncludeMaster), hubThis.getMasterHub(), null, objx, li.getName(), null);
                        editQueryX.setAllowed(editQuery.getAllowed());
                        _processEditQuery(editQueryX);
                        editQuery.setAllowed(editQueryX.getAllowed());
                        if (OAString.isEmpty(editQuery.getResponse())) editQuery.setResponse(editQueryX.getResponse());
                    }
                }
            }
        }
        
        if (oaObj != null && editQuery.getType().checkOwner) {
            ownerHierProcess(editQuery, oaObj, propertyName);
        }
        if (bCheckProcessedCheck && oi.getProcessed() && OAString.isEmpty(propertyName) && editQuery.getAllowed() && ((editQuery.getType() == Type.AllowEnabled) || editQuery.getType().checkEnabledFirst)) {
            updateEditProcessed(editQuery);
        }

        // "allow" can be overwritten, if there is a lower level annotation/editQuery defined
        if (editQuery.getType() == Type.AllowVisible && OAString.isNotEmpty(propertyName)) {
            String sx = null;
            boolean bx = true;
            OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
            if (pi != null) {
                sx = pi.getVisibleProperty();
                bx = pi.getVisibleValue();
            }
            else {
                OALinkInfo li = oi.getLinkInfo(propertyName);
                if (li != null) {
                    sx = li.getVisibleProperty();
                    bx = li.getVisibleValue();
                }
                else {
                    OACalcInfo ci = oi.getCalcInfo(propertyName);
                    if (ci != null) {
                        sx = ci.getVisibleProperty();
                        bx = ci.getVisibleValue();
                    }
                    else {
                        OAMethodInfo mi = oi.getMethodInfo(propertyName);
                        if (mi != null) {
                            sx = mi.getVisibleProperty();
                            bx = mi.getVisibleValue();
                        }
                    }
                }
            }
            final boolean bHadVisibleProperty = (oaObj != null && OAString.isNotEmpty(sx)); 
            if  (bHadVisibleProperty) {
                Object valx = OAObjectReflectDelegate.getProperty(oaObj, sx);
                editQuery.setAllowed(bx == OAConv.toBoolean(valx));
                if (!editQuery.getAllowed() && OAString.isEmpty(editQuery.getResponse())) {
                    editQuery.setAllowed(false);
                    String s = "Not visible, "+oaObj.getClass().getSimpleName()+"."+sx+" is not "+bx;
                    editQuery.setResponse(s);
                }
            }

            sx = null;
            bx = true;
            pi = oi.getPropertyInfo(propertyName);
            if (pi != null) {
                sx = pi.getContextVisibleProperty();
                bx = pi.getContextVisibleValue();
            }
            else {
                OALinkInfo li = oi.getLinkInfo(propertyName);
                if (li != null) {
                    sx = li.getContextVisibleProperty();
                    bx = li.getContextVisibleValue();
                }
                else {
                    OACalcInfo ci = oi.getCalcInfo(propertyName);
                    if (ci != null) {
                        sx = ci.getContextVisibleProperty();
                        bx = ci.getContextVisibleValue();
                    }
                    else {
                        OAMethodInfo mi = oi.getMethodInfo(propertyName);
                        if (mi != null) {
                            sx = mi.getContextVisibleProperty();
                            bx = mi.getContextVisibleValue();
                        }
                    }
                }
            }
            if ((!bHadVisibleProperty || editQuery.getAllowed()) && OAString.isNotEmpty(sx)) {
                OAObject user = OAContext.getContextObject();
                if (user == null) {
                    if (!OASync.isServer()) {
                        editQuery.setAllowed(false);
                    }
                }
                else {
                    Object valx = OAObjectReflectDelegate.getProperty(user, sx);
                    editQuery.setAllowed(bx == OAConv.toBoolean(valx));
                }
                if (!editQuery.getAllowed() && OAString.isEmpty(editQuery.getResponse())) {
                    editQuery.setAllowed(false);
                    String s = user == null ? "User" : user.getClass().getSimpleName();
                    s = "Not visible, "+s+"."+sx+" is not "+bx;
                    editQuery.setResponse(s);
                }
            }
        }
        else if ((editQuery.getType() == Type.AllowEnabled || editQuery.getType().checkEnabledFirst) && OAString.isNotEmpty(propertyName)) {
        // was: else if (editQuery.getAllowed() && (editQuery.getType() == Type.AllowEnabled || editQuery.getType().checkEnabledFirst) && OAString.isNotEmpty(propertyName)) {
            if (oaObj == null) return;
            String enabledName = null;
            boolean enabledValue = true;
            OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
            boolean bIsProcessed = false;
            if (pi != null) {
                enabledName = pi.getEnabledProperty();
                enabledValue = pi.getEnabledValue();
                bIsProcessed = pi.getProcessed();
            }
            else {
                OALinkInfo li = oi.getLinkInfo(propertyName);
                if (li != null) {
                    enabledName = li.getEnabledProperty();
                    enabledValue = li.getEnabledValue();
                    bIsProcessed = li.getProcessed();
                }
                else {
                    OACalcInfo ci = oi.getCalcInfo(propertyName);
                    if (ci != null) {
                        enabledName = ci.getEnabledProperty();
                        enabledValue = ci.getEnabledValue();
                    }
                    else {
                        OAMethodInfo mi = oi.getMethodInfo(propertyName);
                        if (mi != null) {
                            enabledName = mi.getEnabledProperty();
                            enabledValue = mi.getEnabledValue();
                        }
                    }
                }
            }

            if (bCheckProcessedCheck && bIsProcessed) {
                updateEditProcessed(editQuery);
            }

            final boolean bHadEnabledProperty = (editQuery.getAllowed() && OAString.isNotEmpty(enabledName)); 
            if (bHadEnabledProperty && bCheckEnabledProperty) {
                Object valx = OAObjectReflectDelegate.getProperty(oaObj, enabledName);
                editQuery.setAllowed(enabledValue == OAConv.toBoolean(valx));
                if (!editQuery.getAllowed() && OAString.isEmpty(editQuery.getResponse())) {
                    editQuery.setAllowed(false);
                    String s = "Not enabled, "+oaObj.getClass().getSimpleName()+"."+enabledName+" is not "+enabledValue;
                    editQuery.setResponse(s);
                }
            }
            
            enabledName = null;
            enabledValue = true;
            pi = oi.getPropertyInfo(propertyName);
            if (pi != null) {
                enabledName = pi.getContextEnabledProperty();
                enabledValue = pi.getContextEnabledValue();
            }
            else {
                OALinkInfo li = oi.getLinkInfo(propertyName);
                if (li != null) {
                    enabledName = li.getContextEnabledProperty();
                    enabledValue = li.getContextEnabledValue();
                }
                else {
                    OACalcInfo ci = oi.getCalcInfo(propertyName);
                    if (ci != null) {
                        enabledName = ci.getContextEnabledProperty();
                        enabledValue = ci.getContextEnabledValue();
                    }
                    else {
                        OAMethodInfo mi = oi.getMethodInfo(propertyName);
                        if (mi != null) {
                            enabledName = mi.getContextEnabledProperty();
                            enabledValue = mi.getContextEnabledValue();
                        }
                    }
                }
            }
            if ((!bHadEnabledProperty || editQuery.getAllowed()) && OAString.isNotEmpty(enabledName) && bCheckUserEnabledProperty) {
                boolean b = OAContext.isEnabled(enabledName, enabledValue);
                editQuery.setAllowed(b);
                if (!b) {
                    editQuery.setAllowed(false);
                    if (OAString.isEmpty(editQuery.getResponse())) {
                        editQuery.setAllowed(false);
                        OAObject user = OAContext.getContextObject();
                        String s = user == null ? "User" : user.getClass().getSimpleName();
                        s = "Not enabled, "+s+"."+enabledName+" is not "+enabledValue;
                        editQuery.setResponse(s);
                    }
                }
            }
        }
        
        if (oaObj == null) return;
        Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);
        
        // call the callback method, this can override eq.allowed
        if (OAString.isNotEmpty(propertyName) && editQuery.getType().checkEnabledFirst) {
            OAObjectEditQuery editQueryX = new OAObjectEditQuery(Type.AllowEnabled, OAObjectEditQuery.CHECK_CallbackMethod, null, null, oaObj, propertyName, null);
            editQueryX.setAllowed(editQuery.getAllowed());
            callEditQueryMethod(editQueryX);

            // call hub listeners
            if (hubs != null) {
                for (Hub h : hubs) {
                    if (h == null) continue;
                    processEditQueryForHubListeners(editQueryX, h, oaObj, propertyName, oldValue, value);
                }
            }
            editQuery.setAllowed(editQueryX.getAllowed());
            if (OAString.isEmpty(editQuery.getResponse())) editQuery.setResponse(editQueryX.getResponse());
        }

        if (bCheckCallbackMethod) {
            callEditQueryMethod(editQuery);
        }
        
        // call hub listeners
        if (hubs != null) {
            for (Hub h : hubs) {
                if (h == null) continue;
                processEditQueryForHubListeners(editQuery, h, oaObj, propertyName, oldValue, value);
            }
        }
    }
    
    /**
     * Calls visible|enabled for this object and all of it's owner/parents
     */
    protected static void ownerHierProcess(OAObjectEditQuery editQuery, final OAObject oaObj, final String propertyName) {
        _ownerHierProcess(editQuery, oaObj, propertyName, null);
    }
    protected static void _ownerHierProcess(OAObjectEditQuery editQuery, final OAObject oaObj, final String propertyName, final OALinkInfo li) {
        if (oaObj == null) return;
        // recursive, goto top owner first
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);
        
        OALinkInfo lix = oi.getOwnedByOne();
        if (lix != null) {
            OAObject objOwner = (OAObject) lix.getValue(oaObj);
            if (objOwner != null) {
                lix = lix.getReverseLinkInfo();
                _ownerHierProcess(editQuery, objOwner, lix.getName(), lix);
            }
        }
        
        String pp;
        boolean b;
        Object valx;
        boolean bPassed = editQuery.getAllowed();
        
        // check class level @OAEditQuery annotation
        if (editQuery.getType() == Type.AllowVisible) {
            pp = oi.getVisibleProperty();
            if (bPassed && OAString.isNotEmpty(pp)) {
                b = oi.getVisibleValue();
                valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
                bPassed = (b == OAConv.toBoolean(valx));
                if (!bPassed) {
                    editQuery.setAllowed(false);
                    String s = "Not visible, rule for "+oaObj.getClass().getSimpleName()+", "+pp+" != "+b;
                    editQuery.setResponse(s);
                }
            }
            pp = oi.getContextVisibleProperty();
            if (bPassed && OAString.isNotEmpty(pp)) {
                b = oi.getContextVisibleValue();
                OAObject user = OAContext.getContextObject();
                if (user == null) {
                    if (!OASync.isServer()) {
                        bPassed = false;
                    }
                }
                else {
                    valx = OAObjectReflectDelegate.getProperty(user, pp);
                    bPassed = (b == OAConv.toBoolean(valx));
                }
                if (!bPassed) {
                    editQuery.setAllowed(false);
                    String s = "Not visible, user rule for "+oaObj.getClass().getSimpleName()+", ";
                    if (user == null) s = "OAAuthDelegate.getUser returned null";
                    else s = "User."+pp+" != "+b;
                    editQuery.setResponse(s);
                }
            }
            
            // this can overwrite editQuery.allowed        
            callEditQueryMethod(oaObj, null, editQuery);
            bPassed = editQuery.getAllowed();
            if (!bPassed && OAString.isEmpty(editQuery.getResponse())) {
                String s = "Not visible, edit query for "+oaObj.getClass().getSimpleName()+" allowVisible returned false";
                editQuery.setResponse(s);
            }
            
            if (bPassed && li != null) {
                pp = li.getVisibleProperty();
                if (OAString.isNotEmpty(pp)) {
                    b = li.getVisibleValue();
                    valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
                    bPassed = (b == OAConv.toBoolean(valx));
                    if (!bPassed) {
                        editQuery.setAllowed(false);
                        String s = "Not visible, rule for "+oaObj.getClass().getSimpleName()+"."+propertyName+", "+pp+" != "+b;
                        editQuery.setResponse(s);
                    }
                }
            }
            if (bPassed && li != null) {
                pp = li.getContextVisibleProperty();
                if (OAString.isNotEmpty(pp)) {
                    b = li.getContextVisibleValue();
                    OAObject user = OAContext.getContextObject();
                    if (user == null) {
                        if (!OASync.isServer()) {
                            bPassed = false;
                        }
                    }
                    else {
                        valx = OAObjectReflectDelegate.getProperty(user, pp);
                        bPassed = (b == OAConv.toBoolean(valx));
                    }
                    if (!bPassed) {
                        editQuery.setAllowed(false);
                        String s = "Not visible, user rule for "+oaObj.getClass().getSimpleName()+"."+propertyName+", ";
                        if (user == null) s = "OAAuthDelegate.getUser returned null";
                        else s = "User."+pp+" must be "+b;
                        editQuery.setResponse(s);
                    }
                }
            }
            
            // this can overwrite editQuery.allowed        
            if (li != null && OAString.isNotEmpty(propertyName)) {
                callEditQueryMethod(oaObj, propertyName, editQuery);
                bPassed = editQuery.getAllowed();
                if (!bPassed && OAString.isEmpty(editQuery.getResponse())) {
                    String s = "Not visible, edit query for "+oaObj.getClass().getSimpleName()+"." + propertyName + " allowVisible returned false";
                    editQuery.setResponse(s);
                }
            }
        }
        else if (editQuery.getType() == Type.AllowEnabled || editQuery.getType().checkEnabledFirst) {
        //was:  else if ( (editQuery.getType() == Type.AllowEnabled || editQuery.getType().checkEnabledFirst) && !(OASync.isServer() && OAThreadLocalDelegate.getContext() == null)) {
            
            // final boolean bCheckProcessedCheck = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_Processed) != 0;
            final boolean bCheckEnabledProperty = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_EnabledProperty) != 0;
            final boolean bCheckUserEnabledProperty = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_UserEnabledProperty) != 0;
            final boolean bCheckCallbackMethod = (editQuery.getCheckType() & OAObjectEditQuery.CHECK_CallbackMethod) != 0;
            
            if (bPassed) {
                pp = oi.getEnabledProperty();
                if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
                    b = oi.getEnabledValue();
                    valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
                    bPassed = (b == OAConv.toBoolean(valx));
                    if (!bPassed) {
                        editQuery.setAllowed(false);
                        String s = "Not enabled, rule for "+oaObj.getClass().getSimpleName()+", "+pp+" != "+b;
                        editQuery.setResponse(s);
                    }
                }
            }
            pp = oi.getContextEnabledProperty();
            if (bPassed && OAString.isNotEmpty(pp) && bCheckUserEnabledProperty) {
                b = oi.getContextEnabledValue();
                if (!OAContext.isEnabled(pp, b)) {
                    bPassed = false;
                    editQuery.setAllowed(false);
                    String s = "Not enabled, user rule for "+oaObj.getClass().getSimpleName()+", ";
                    OAObject user = OAContext.getContextObject();
                    if (user == null) s = "OAAuthDelegate.getUser returned null";
                    else s = "User."+pp+" must be "+b;
                    editQuery.setResponse(s);
                }
            }
            
            // this can overwrite editQuery.allowed
            if (bCheckCallbackMethod) {
                OAObjectEditQuery editQueryX = new OAObjectEditQuery(Type.AllowEnabled, editQuery.getCheckType(), editQuery);
                
                callEditQueryMethod(oaObj, null, editQueryX);
                bPassed = editQueryX.getAllowed();
                editQuery.setAllowed(bPassed);
                if (!bPassed && OAString.isEmpty(editQuery.getResponse())) {
                    String s = "Not enabled, edit query for "+oaObj.getClass().getSimpleName()+" allowEnabled returned false";
                    editQuery.setResponse(s);
                }
            }

            
            if (li != null && bPassed) {
                pp = li.getEnabledProperty();
                if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
                    b = li.getEnabledValue();
                    valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
                    bPassed = (b == OAConv.toBoolean(valx));
                    if (!bPassed) {
                        editQuery.setAllowed(false);
                        String s = "Not enabled, rule for "+oaObj.getClass().getSimpleName()+"."+propertyName+", "+pp+" != "+b;
                        editQuery.setResponse(s);
                    }
                }
            }
            
            if (li != null && bPassed) {
                pp = li.getContextEnabledProperty();
                if (OAString.isNotEmpty(pp) && bCheckUserEnabledProperty) {
                    b = li.getContextEnabledValue();
                    if (!OAContext.isEnabled(pp, b)) {
                        OAObject user = OAContext.getContextObject();
                        editQuery.setAllowed(false);
                        String s = "Not enabled, user rule for "+oaObj.getClass().getSimpleName()+"."+propertyName+", ";
                        if (user == null) s = "OAAuthDelegate.getUser returned null";
                        else s = "User."+pp+" must be "+b;
                        editQuery.setResponse(s);
                    }
                }
            }
            
            // this can overwrite editQuery.allowed        
            if (bCheckCallbackMethod && li != null && OAString.isNotEmpty(propertyName)) {
                OAObjectEditQuery editQueryX = new OAObjectEditQuery(Type.AllowEnabled, editQuery.getCheckType(), editQuery);
                callEditQueryMethod(oaObj, propertyName, editQueryX);
                bPassed = editQueryX.getAllowed();
                editQuery.setAllowed(bPassed);
                if (!bPassed && OAString.isEmpty(editQuery.getResponse())) {
                    String s = "Not enabled, edit query for "+oaObj.getClass().getSimpleName()+"." + propertyName + " allowEnabled returned false";
                    editQuery.setResponse(s);
                }
            }
        }
    }
    
    // called directly if hub.masterObject=null
    protected static void processEditQueryForHubListeners(OAObjectEditQuery editQuery, final Hub hub, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
        if (editQuery.getType().checkEnabledFirst) {
            OAObjectEditQuery editQueryX = new OAObjectEditQuery(Type.AllowEnabled);
            editQueryX.setAllowed(editQuery.getAllowed());
            editQueryX.setPropertyName(editQuery.getPropertyName());
            _processEditQueryForHubListeners(editQueryX, hub, oaObj, propertyName, oldValue, newValue);
            editQuery.setAllowed(editQueryX.getAllowed());
        }    
        _processEditQueryForHubListeners(editQuery, hub, oaObj, propertyName, oldValue, newValue);        
    }
    protected static void _processEditQueryForHubListeners(OAObjectEditQuery editQuery, final Hub hub, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
        HubListener[] hl = HubEventDelegate.getAllListeners(hub);
        if (hl == null) return;
        int x = hl.length;
        if (x == 0) return;
        final boolean bBefore = editQuery.getAllowed();
        
        HubEvent hubEvent = null;
        try {
            for (int i=0; i<x; i++) {
                boolean b = editQuery.getAllowed();

                switch (editQuery.getType()) {
                case AllowEnabled:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, oaObj, propertyName);
                    b = hl[i].getAllowEnabled(hubEvent, b);
                    break;
                case AllowVisible:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, oaObj, propertyName);
                    b = hl[i].getAllowVisible(hubEvent, b);
                    break;

                case VerifyPropertyChange:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, oaObj, propertyName, oldValue, newValue);
                    b = hl[i].isValidPropertyChange(hubEvent, b);
                    break;

                case AllowNew:
                    if (hubEvent == null) hubEvent = new HubEvent(hub);
                    b = hl[i].getAllowAdd(hubEvent, b);
                    break;
                case AllowAdd:
                    if (hubEvent == null) hubEvent = new HubEvent(hub);
                    b = hl[i].getAllowAdd(hubEvent, b);
                    break;
                case VerifyAdd:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, newValue);
                    b = hl[i].isValidAdd(hubEvent, b);
                    break;
                case AllowRemove:
                    if (hubEvent == null) hubEvent = new HubEvent(hub);
                    b = hl[i].getAllowRemove(hubEvent, b);
                    break;
                case VerifyRemove:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, newValue);
                    b = hl[i].isValidRemove(hubEvent, b);
                    break;
                case AllowRemoveAll:
                    if (hubEvent == null) hubEvent = new HubEvent(hub);
                    b = hl[i].getAllowRemoveAll(hubEvent, b);
                    break;
                case VerifyRemoveAll:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, newValue);
                    b = hl[i].isValidRemoveAll(hubEvent, b);
                    break;
                case AllowDelete:
                    if (hubEvent == null) hubEvent = new HubEvent(hub);
                    b = hl[i].getAllowDelete(hubEvent, b);
                    break;
                case VerifyDelete:
                    if (hubEvent == null) hubEvent = new HubEvent(hub, newValue);
                    b = hl[i].isValidDelete(hubEvent, b);
                    break;
                }
                
                if (hubEvent == null) break;
                editQuery.setAllowed(b);
                String s = hubEvent.getResponse();
                if (OAString.isNotEmpty(s)) editQuery.setResponse(s);
            }
        }
        catch (Exception e) {
            editQuery.setThrowable(e);
            editQuery.setAllowed(false);
        }

        if (bBefore != editQuery.getAllowed()) {
            String s = editQuery.getResponse();
            if (OAString.isEmpty(s)) s = editQuery.getType() + " failed for " + oaObj.getClass().getSimpleName() + "." + propertyName;
            editQuery.setResponse(s);
        }
    }
    
    
    protected static void callEditQueryMethod(final OAObjectEditQuery em) {
        callEditQueryMethod(em.getObject(), em.getPropertyName(), em);
    }    
    protected static void callEditQueryMethod(final Object object, String propertyName, final OAObjectEditQuery em) {
        if (object == null) return;
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(object.getClass());
        
        if (propertyName == null) propertyName = "";  // blank will be method for class level:   onEditQuery(..)  or callback(OAObjectEditQuery)
        
        Method method = oi.getEditQueryMethod(propertyName);
        if (method == null) return;
            //Class[] cs = method.getParameterTypes();
            //if (cs[0].equals(OAObjectEditQuery.class)) {
                try {
                    method.invoke(object, new Object[] {em});
                }
                catch (Exception e) {
                    em.setThrowable(e);
                    em.setAllowed(false);
                }
            //}
    }    
   
    
    /**
     * Used by OAObjectModel objects to allow model object to be updated after it is created by calling EditQuery method.
     * @param clazz, ex: from SalesOrderModel, SalesOrder.class
     * @param property  ex:  "SalesOrderItems"
     * @param model ex: SalesOrderItemModel
     */
    public static void onEditQueryModel(Class clazz, String property, OAObjectModel model) {
        if (clazz == null || OAString.isEmpty(property) || model == null) return;
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
        Method m = OAObjectInfoDelegate.getMethod(oi, "onEditQuery" + property + "Model", 1);
        if (m == null) {
            m = OAObjectInfoDelegate.getMethod(oi, property + "ModelCallback", 1);
        }
        if (m != null) {
            Class[] cs = m.getParameterTypes();
            if (cs[0].equals(OAObjectModel.class)) {
                try {
                    m.invoke(null, new Object[] {model});
                }
                catch (Exception e) {
                    throw new RuntimeException("Exception calling static method "+m, e);
                }
            }
        }
    }
    

    /**
     * Used by HubChangedListener.addXxx to listen to dependencies found for an EditQuery.
     */
    public static void addEditQueryChangeListeners(final Hub hub, final Class cz, final String prop, String ppPrefix, final HubChangeListener changeListener, final boolean bEnabled) {
        if (ppPrefix == null) ppPrefix = "";
        OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(cz);
        String s;
        
        if (bEnabled) s = oi.getEnabledProperty();
        else s = oi.getVisibleProperty(); 
        if (OAString.isNotEmpty(s)) changeListener.add(hub, ppPrefix+s);

        // dependent properties
        addDependentProps(hub, ppPrefix, 
            bEnabled ? null : oi.getViewDependentProperties(), 
            bEnabled ? oi.getContextDependentProperties() : null, 
            (OAString.isEmpty(prop) && oi.getProcessed()),
            changeListener
        );
        
        final Hub hubUser = OAContext.getContextHub();
        if (bEnabled) s = oi.getContextEnabledProperty();
        else s = oi.getContextVisibleProperty();
        if (OAString.isNotEmpty(s)) changeListener.add(hubUser, s);
        
        if (OAString.isEmpty(prop)) return;
        
        OAPropertyInfo pi = oi.getPropertyInfo(prop);
        if (pi != null) {
            if (bEnabled) s = pi.getEnabledProperty();
            else s = pi.getVisibleProperty();
            if (OAString.isNotEmpty(s)) changeListener.add(hub, ppPrefix+s);
            addDependentProps(hub, ppPrefix, pi.getViewDependentProperties(), pi.getContextDependentProperties(), (bEnabled && pi.getProcessed()), changeListener);
            
            if (bEnabled) s = pi.getContextEnabledProperty();
            else s = pi.getContextVisibleProperty();
            if (OAString.isNotEmpty(s)) changeListener.add(hubUser, s);
        }
        else {
            OALinkInfo li = oi.getLinkInfo(prop);
            if (li != null) {
                if (bEnabled) s = li.getEnabledProperty();
                else s = li.getVisibleProperty();
                if (OAString.isNotEmpty(s)) changeListener.add(hub, ppPrefix+s);
                addDependentProps(hub, ppPrefix, li.getViewDependentProperties(), li.getContextDependentProperties(), (bEnabled && li.getProcessed()), changeListener);

                if (bEnabled) s = li.getContextEnabledProperty();
                else s = li.getContextVisibleProperty();
                if (OAString.isNotEmpty(s)) changeListener.add(hubUser, s);
            }
            else {
                OACalcInfo ci = oi.getCalcInfo(prop);
                if (ci != null) {
                    if (bEnabled) s = ci.getEnabledProperty();
                    else s = ci.getVisibleProperty();
                    if (OAString.isNotEmpty(s)) changeListener.add(hub, ppPrefix+s);
                    addDependentProps(hub, ppPrefix, ci.getViewDependentProperties(), ci.getContextDependentProperties(), false, changeListener);
                    
                    if (bEnabled) s = ci.getContextEnabledProperty();
                    else s = ci.getContextVisibleProperty();
                    if (OAString.isNotEmpty(s)) changeListener.add(hubUser, s);
                }
                else {
                    OAMethodInfo mi = oi.getMethodInfo(prop);
                    if (mi != null) {
                        if (bEnabled) s = mi.getEnabledProperty();
                        else s = mi.getVisibleProperty(); 
                        if (OAString.isNotEmpty(s)) changeListener.add(hub, ppPrefix+s);
                        addDependentProps(hub, ppPrefix, mi.getViewDependentProperties(), mi.getContextDependentProperties(), false, changeListener);
                        
                        if (bEnabled) s = mi.getContextEnabledProperty();
                        else s = mi.getContextVisibleProperty();
                        if (OAString.isNotEmpty(s)) changeListener.add(hubUser, s);
                    }
                }
            }
        }
    }
    
    protected static void addDependentProps(Hub hub, String prefix,  String[] viewDependentProperties, String[] contextDependentProperties, boolean bProcessed, HubChangeListener changeListener) {
        if (viewDependentProperties != null) {
            for (String s : viewDependentProperties) {
                changeListener.add(hub, prefix+s);
            }
        }
        if (contextDependentProperties != null) {
            Hub hubUser = OAContext.getContextHub();
            if (contextDependentProperties.length > 0 && hubUser == null) {
                changeListener.addAlwaysFalse(hub);
            }
            for (String s : contextDependentProperties) {
                changeListener.add(hubUser, s);
            }
        }
        if (bProcessed) {
            Hub hubUser = OAContext.getContextHub();
            if (hubUser == null) {
                changeListener.addAlwaysFalse(hub);
            }
            changeListener.add(hubUser, OAContext.getAllowEditProcessedPropertyPath());
        }
    }
}

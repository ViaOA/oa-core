package com.viaoa.uicontroller;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAEncryption;
import com.viaoa.util.OAStr;
import com.viaoa.util.OAString;

public class OAUIPropertyController extends OAUIBaseController {

    private final String propertyName;
    private String format;
    private char conversion;
    
    // This is used to handle password/encrypted data
    private final static String ignorePasswordValue = "*****";

    
    public OAUIPropertyController(Hub hub, String propertyName) {
        super(hub);
        this.propertyName = propertyName;
    }
    
    public String getPropertyName() {
        return this.propertyName;
    }
    
    /**
     * 'U'ppercase, 'L'owercase, 'T'itle, 'J'ava identifier 'E'ncrpted password/encrypt 'S'HA password (one way hash)
     */
    public void setConversion(char conv) {
        conversion = conv;
    }

    public char getConversion() {
        return conversion;
    }
    
    
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    
    public boolean isRequired() {
        if (hub == null) return false;
        OAPropertyInfo pi = hub.getOAObjectInfo().getPropertyInfo(getPropertyName()); 
        return (pi != null && pi.getRequired());
    }
    
    @Override
    public boolean isEnabled() {
        return isEnabled((OAObject) hub.getAO());
    }
    public boolean isEnabled(OAObject obj) {
        if (!super.isEnabled()) return false;
        if (obj == null) return false;
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowEnabledObjectCallback(OAObjectCallback.CHECK_ALL, getHub(), obj, getPropertyName());
        return eq.getAllowed();
    }
        
    @Override
    public boolean isVisible() {
        return isVisible((OAObject) hub.getAO());
    }
    public boolean isVisible(OAObject obj) {
        if (!super.isVisible()) return false;
        
        OAObjectCallback eq = OAObjectCallbackDelegate.getAllowVisibleObjectCallback(getHub(), obj, getPropertyName());
        return eq.getAllowed();
    }
    
    public boolean onSetProperty(Object value) {
        final OAObject obj = (OAObject) hub.getAO();
        return onSetProperty(obj, value);
    }

    public boolean onSetProperty(OAObject obj, Object value) {
        if (_onSetProperty(obj, value)) {
            String msg = getCompletedMessage();
            if (OAStr.isNotEmpty(msg)) {
                onCompleted(msg, getTitle()); 
            }
        }
        return true;
    }
    
    /**
     * This can be used to get the confirm message before the actual new value is known.<br>
     * This is used to send a confirm message to browser.
    public OAObjectCallback getPreConfirmMessage() {
        final OAObject obj = (OAObject) hub.getAO();

        OAObjectCallback cb = OAObjectCallbackDelegate.getPreConfirmPropertyChangeObjectCallback(obj, getPropertyName(), getConfirmMessage(), getTitle());
        return cb;
    }
    */

    public static String getIgnorePasswordValue() {
        return ignorePasswordValue;
    }
    
    private boolean _onSetProperty(final OAObject obj, Object newValue) {
        OAObjectCallback cb; 
        String s;

        // 0: conversion
        if (newValue instanceof String && (getConversion() != 0) && ((String) newValue).length() > 0) {
            String text = (String) newValue;
            if (conversion == 'U' || conversion == 'u') {
                text = text.toUpperCase();
            } else if (conversion == 'L' || conversion == 'l') {
                text = text.toLowerCase();
            } else if (conversion == 'T' || conversion == 't') {
                if (text.toLowerCase().equals(text) || text.toUpperCase().equals(text)) {
                    text = OAString.toTitleCase(text);
                }
            } else if (conversion == 'J' || conversion == 'j') {
                text = OAString.makeJavaIndentifier(text);
            } else if (conversion == 'S' || conversion == 's') {
                if (ignorePasswordValue.equals(text)) return true;
                text = OAString.getSHAHash(text);
            } else if (conversion == 'P' || conversion == 'p') {
                if (ignorePasswordValue.equals(text)) return true;
                text = OAString.getSHAHash(text);
            } else if (conversion == 'E' || conversion == 'e') {
                try {
                    if (ignorePasswordValue.equals(text)) return true;
                    text = OAEncryption.encrypt(text);
                } catch (Exception e) {
                    throw new RuntimeException("encryption failed", e);
                }
            }
            newValue = text;
        }        
        
        // 1: confirm
        cb = OAObjectCallbackDelegate.getConfirmPropertyChangeObjectCallback(obj, getPropertyName(), newValue, getConfirmMessage(), getTitle());
        s = cb.getConfirmMessage();
        if (OAStr.isNotEmpty(s)) {
            if (!onConfirm(s, OAStr.notEmpty(cb.getConfirmTitle(), getTitle()) )) {
                return false;
            }
        }
        
        // 2: verify
        cb = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, obj, getPropertyName(), null, newValue); 
        if (!cb.getAllowed()) {
            onError(cb.getResponse(), cb.getDisplayResponse());
            return false;
        }
            
        // 3: call method
        obj.setProperty(getPropertyName(), newValue, getFormat());

        return true;
    }
    
    public String getValueAsString() {
        if (hub == null) return null;
        final Object obj = hub.getAO();
        return getValueAsString(obj);
    }

    public String getValueAsString(Object obj) {
        if (obj == null) return null;
        
        if (!(obj instanceof OAObject)) return OAConv.toString(obj, getFormat());
        String s = ((OAObject) obj).getPropertyAsString(getPropertyName(), getFormat());
        return s;
    }
    
    public Object getValue() {
        if (hub == null) return null;
        final Object obj = hub.getAO();
        if (obj == null) return null;
        return getValue(obj);
    }

    public Object getValue(Object obj) {
        if (obj == null) return null;
        
        if (!(obj instanceof OAObject)) return obj;
        
        Object objx = ((OAObject) obj).getProperty(getPropertyName());
        return objx;
    }
}

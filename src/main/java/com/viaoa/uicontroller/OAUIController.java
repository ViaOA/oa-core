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
package com.viaoa.uicontroller;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.viaoa.annotation.OAOne;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubChangeListener;
import com.viaoa.hub.HubChangeListener.HubProp;
import com.viaoa.model.oa.VString;
import com.viaoa.object.*;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.HubTemp;
import com.viaoa.template.OATemplate;
import com.viaoa.undo.OAUndoableEdit;
import com.viaoa.util.*;

/**
 * Base controller class for OA UI components. Implements the HubListeners and provides most 
 * of the methods required for creating controller Classes (Model/View/Controller) for UI components.
 * 
 * @see #updateComponent() abstract method of update UI component.
 * @see #updateLabel() abstract method of update label.
 */
public abstract class OAUIController extends HubListenerAdapter {
    private static Logger LOG = Logger.getLogger(OAUIController.class.getName());

    public boolean DEBUG; // used for debugging a single component. ex: ((OALabel)lbl).setDebug(true)
    public static boolean DEBUGUI = false; // used by debug() to show info

    protected Hub hub;
    protected final boolean bAoOnly;
    protected String propertyPath;
    protected OAPropertyPath oaPropertyPath;

    private volatile HubEvent heLastUpdate;

    protected Class endPropertyFromClass; // oaObj class (same as hub, or class for pp end)
    protected String endPropertyName;
    protected Class endPropertyClass;

    protected String hubListenerPropertyName;

    protected Object hubObject; // single object, that will be put in temp hub
    protected Hub hubTemp;

    protected final boolean bUseObjectCallback = true;


    protected HubChangeListener.Type hubChangeListenerType;

    protected boolean bIsHubCalc;
    protected boolean bListenToHubSize;
    protected boolean bEnableUndo = true;
    protected String undoDescription;
    
    protected Hub hubSelect;

    protected String format;
    protected String fontPropertyPath;
    protected String backgroundColorPropertyPath;
    protected String foregroundColorPropertyPath;
    protected String iconColorPropertyPath;

    private String confirmMessage;

    protected int maxImageHeight, maxImageWidth;

    protected String imageDirectory;
    protected String imageClassPath;
    protected Class rootImageClassPath;;
    protected String imagePropertyPath;

    protected String toolTipTextPropertyPath;
    protected String nullDescription = "";
    protected boolean bHtml;

    private int minDisplay;
    private int maxDisplay;
    private int maxLength;

    protected MyHubChangeListener changeListener; // listens for any/all hub+propPaths needed for component
    protected MyHubChangeListener changeListenerEnabled;
    protected MyHubChangeListener changeListenerVisible;

    /** HTML used for displaying in some components (label, combo, list, autocomplete), and used for table cell rendering */
    protected String displayTemplate;
    protected OATemplate templateDisplay;

    protected String toolTipTextTemplate;
    protected OATemplate templateToolTipText;
    
    protected boolean bRequired;

    private String enabledMessage;
    private String visibleMessage;
    
    /**
     * 'U'ppercase, 'L'owercase, 'T'itle, 'J'ava identifier 'E'ncrpted password/encrypt 'S'HA password
     */
    protected char conversion;
    
    // This is used to handle password/encrypted data
    private final static String maskPasswordValue = "******";

    private String title;
    private String description;
    private String completedMessage;
    
    
    
    public OAUIController(Hub hub, String propertyPath) {
        this(hub, null, propertyPath, true, HubChangeListener.Type.AoNotNull);
    }    
    
    /**
     * Create new controller for Hub and Jfc component
     *
     * @param hub
     * @param object             if hub is null, then this object will be put in temp hub and made the AO
     * @param propertyPath       property used by component
     * @param bAoOnly            should controller listen to propChange for all objects in hub, or just AO.
     * @param type               default type of change listener
     */
    public OAUIController(Hub hub, Object object, 
        String propertyPath,
        boolean bAoOnly, 
        HubChangeListener.Type type) 
    {
        this.hub = hub;
        this.hubObject = object;
        this.propertyPath = propertyPath;
        this.bAoOnly = bAoOnly;
        this.hubChangeListenerType = type;

        reset();
    }

    // used to track the last values used by reset
    private Hub hubLast;
    private Object hubObjectLast;
    private HubChangeListener.HubProp hubChangeListenerTypeLast;
    protected volatile boolean bIgnoreUpdate;

    /**
     * Called when changes are made that affects the setup for component.
     */
    protected void reset() {
        try {
            bIgnoreUpdate = true;
            _reset();
        }
        finally {
            bIgnoreUpdate = false;
        }
        callUpdate();
    }

    // called when hub, property, etc is changed.
    // does not include resetting HubChangeListeners (changeListener, visibleChangeListener, enabledChangeListener)
    protected void _reset() {
        // note: dont call close, want to keep visibleChangeListener, enabledChangeListener
        if (hubLast != null) {
            hubLast.removeHubListener(this);
        }
        if (hubObjectLast != null) {
            HubTemp.deleteHub(hubObjectLast);
        }
        if (changeListenerEnabled != null && hubChangeListenerTypeLast != null) {
            changeListenerEnabled.remove(hubChangeListenerTypeLast);
            hubChangeListenerTypeLast = null;
        }
        if (changeListener != null) {
            changeListener.close();
            changeListener = null;
        }

        if (hub != null) {
            this.hubTemp = null;
            this.hubObject = null;
        }
        else {
            if (hubObject == null) {
                this.hub = null;
                this.hubTemp = null;
            }
            else {
                this.hub = this.hubTemp = HubTemp.createHub(hubObject);
            }
        }

        hubObjectLast = hubObject;
        hubLast = this.hub;

        if (this.hub == null) {
            return;
        }

        if (propertyPath != null && propertyPath.indexOf('.') >= 0) {
            hubListenerPropertyName = propertyPath.replace('.', '_'); // (com.cdi.model.oa.WebItem)B_WebPart_Title
            hub.addHubListener(this, hubListenerPropertyName, new String[] { propertyPath }, bAoOnly);
        }
        else {
            hubListenerPropertyName = propertyPath;
            if (OAString.isNotEmpty(hubListenerPropertyName)) {
                hub.addHubListener(this, hubListenerPropertyName, bAoOnly);
            }
            else {
                hub.addHubListener(this);
            }
        }

        oaPropertyPath = new OAPropertyPath(hub.getObjectClass(), propertyPath);
        final String[] properties = oaPropertyPath.getProperties();
        endPropertyName = (properties == null || properties.length == 0) ? null : properties[properties.length - 1];

        if (hubChangeListenerType != null) { // else: this class already is listening to hub
            if (hubChangeListenerType == HubChangeListener.Type.HubNotEmpty || hubChangeListenerType == HubChangeListener.Type.HubEmpty) {
                bListenToHubSize = true;
            }
            hubChangeListenerTypeLast = getEnabledChangeListener().add(hub, hubChangeListenerType);
        }

        if (oaPropertyPath.getEndLinkInfo() != null && properties != null && properties.length == 1) {
            OAOne oaOne = oaPropertyPath.getOAOneAnnotation();
            if (oaOne != null) {
                if (OAString.isNotEmpty(oaOne.defaultPropertyPath())) {
                    if (!oaOne.defaultPropertyPathCanBeChanged()) {
                        getEnabledChangeListener().addPropertyNull(hub, properties[0]);
                    }
                }
            }
        }

        Method[] ms = oaPropertyPath.getMethods();
        endPropertyFromClass = hub.getObjectClass();
        if (ms != null && ms.length > 0) {
            Class[] cs = ms[ms.length - 1].getParameterTypes();
            bIsHubCalc = cs.length == 1 && cs[0].equals(Hub.class);
            endPropertyClass = ms[ms.length - 1].getReturnType();

            if (ms.length > 1) {
                endPropertyFromClass = ms[ms.length - 2].getReturnType();
            }
        }
        else {
            bIsHubCalc = false;
            endPropertyClass = String.class;
        }
        bDefaultFormat = false;

        if (bUseObjectCallback) {
            Class cz = hub.getObjectClass();
            String ppPrefix = "";
            int cnt = 0;
            for (String prop : properties) {
                if (cnt == 0) {
                    addEnabledObjectCallbackCheck(hub, prop);
                    addVisibleObjectCallbackCheck(hub, prop);
                }
                else {
                    OAObjectCallbackDelegate.addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, getEnabledChangeListener(), true);
                    OAObjectCallbackDelegate.addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, getVisibleChangeListener(), false);
                }
                ppPrefix += prop + ".";
                cz = oaPropertyPath.getClasses()[cnt++];
            }

            if (cnt == 0) {
                addEnabledObjectCallbackCheck(hub, null);
                addVisibleObjectCallbackCheck(hub, null);
            }
        }

        
        if (hub != null) {
            OAPropertyInfo pi = hub.getOAObjectInfo().getPropertyInfo(endPropertyName);
            if (pi != null) {
                bRequired = pi.getRequired();
                minDisplay = pi.getDisplayLength();
                maxLength = pi.getMaxLength();

                OADataSource ds = OADataSource.getDataSource(hub.getObjectClass());
                if (ds != null) {
                    maxLength = ds.getMaxLength(hub.getObjectClass(), endPropertyName);
                    if (endPropertyClass != null) {
                        if (endPropertyClass.equals(String.class)) {
                            if (maxLength > 254) {
                                maxLength = -1;
                            }
                        }
                    }
                }
            }
            else {
                OALinkInfo li = hub.getOAObjectInfo().getLinkInfo(endPropertyName);
                bRequired = (li != null && li.getRequired());
            }
        }
    }

    public void bind(Hub hub, String propertyPath) {
        this.hub = hub;
        this.propertyPath = propertyPath;
        reset();
    }

    public Hub getHub() {
        return hub;
    }

    public void setHub(Hub hub) {
        this.hub = hub;
        reset();
    }

    public Object getObject() {
        return hubObject;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(String propPath) {
        propertyPath = propPath;
        reset();
    }

    public String getEndPropertyName() {
        return endPropertyName;
    }

    public Class getEndPropertyClass() {
        return endPropertyClass;
    }

    public Class getEndPropertyFromClass() {
        return endPropertyFromClass;
    }

    public String getHubListenerPropertyName() {
        return hubListenerPropertyName;
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        if (hubObject != null) {
            HubTemp.deleteHub(hubObject);
        }
        if (changeListener != null) {
            changeListener.close();
            changeListener = null;
        }
        if (changeListenerEnabled != null) {
            changeListenerEnabled.close();
            changeListenerEnabled = null;
        }
        if (changeListenerVisible != null) {
            changeListenerVisible.close();
            changeListenerVisible = null;
        }

        enableVisibleListener(false);
        if (hub != null) {
            hub.removeHubListener(this);
        }
    }

    /**
     * Called to add or remove a UI specific listener (ex: inactive/hidden tab)  that is used to know if the component is 
     * visible to the user.
     */
    public void enableVisibleListener(boolean b) {
    }

    /**
     * Returns the Hub that this component will work with.
     */
    public Hub getSelectHub() {
        return hubSelect;
    }

    public Hub getMultiSelectHub() {
        return hubSelect;
    }

    public boolean getAllowRemovingFromSelectHub() {
        return bAllowRemovingFromSelectHub;
    }

    /*
     * flag to know if components can remove objects from selectHub, default: true
     *
     *
     */
    private boolean bAllowRemovingFromSelectHub;

    /**
     * Sets the MultiSelect that this component will work with.
     */
    public void setSelectHub(Hub newHub, boolean bAllowRemovingFromSelectHub) {
        this.bAllowRemovingFromSelectHub = bAllowRemovingFromSelectHub;
        if (hubSelect != null) {
            getChangeListener().remove(hubSelect);
        }
        this.hubSelect = newHub;
        if (hubSelect != null) {
            getChangeListener().add(hubSelect);
        }
    }

    public void setSelectHub(Hub newHub) {
        setSelectHub(newHub, true);
    }

    public void setMultiSelectHub(Hub newHub) {
        setSelectHub(newHub, true);
    }

    /**
     * This will find the real object in this hub to use, in cases where a comp is added to a table, and the table.hub is different then the
     * comp.hub, which could be a detail or link type relationship to the table.hub
     */
    private Class fromParentClass;
    private String fromParentPropertyPath;

    protected Object getRealObject(Object fromObject) {
        if (fromObject == null || hub == null) {
            return fromObject;
        }
        Class c = hub.getObjectClass();
        if (c == null || c.isAssignableFrom(fromObject.getClass())) {
            return fromObject;
        }
        if (!(fromObject instanceof OAObject)) {
            return fromObject;
        }
        if (!OAObject.class.isAssignableFrom(getHub().getObjectClass())) {
            return fromObject;
        }

        if (fromParentClass == null || !fromParentClass.equals(fromObject.getClass())) {
            fromParentClass = fromObject.getClass();
            fromParentPropertyPath = OAObjectReflectDelegate.getPropertyPathFromMaster((OAObject) fromObject, getHub());
        }
        return OAObjectReflectDelegate.getProperty((OAObject) fromObject, fromParentPropertyPath);
    }

    public Object getValue(Object obj) {
        obj = getRealObject(obj);
        if (obj == null) {
            return null;
        }

        if (OAString.isEmpty(propertyPath) && hubSelect != null) {
            return hubSelect.contains(obj);
        }

        if (bIsHubCalc) {
            obj = OAObjectReflectDelegate.getProperty(getHub(), propertyPath);
        }
        else {
            if (OAString.isEmpty(propertyPath)) {
                return obj;
            }
            if (!(obj instanceof OAObject)) {
                return obj;
            }
            obj = ((OAObject) obj).getProperty(propertyPath);
        }
        return obj;
    }

    public String getValueAsString(Object obj) {
        return getValueAsString(obj, getFormat());
    }

    public String getValueAsString(Object obj, String fmt) {
        return getValueAsString(obj, fmt, -1);
    }
    
    public String getValueAsString(Object obj, final String fmt, final int maxLength) {
        String s;
        if (obj == null) s = "";
        else if (obj instanceof OAObject) {
            if (oaPropertyPath != null && oaPropertyPath.getHasHubProperty()) {
                final VString vs = new VString("");
                OAFinder finder = new OAFinder(oaPropertyPath.getPropertyPathLinksOnly()) {
                    @Override
                    protected void onFound(OAObject obj) {
                        Object objx = obj.getProperty(oaPropertyPath.getLastPropertyName());
                        if (maxLength < 0 || vs.getValue().length() < maxLength) {
                            String s = OAConv.toString(objx, getFormat());
                            vs.setValue(OAString.concat(vs.getValue(), s, ", "));
                            if (maxLength > 0 && vs.getValue().length() >= maxLength) {
                                vs.setValue(vs.getValue().substring(0, maxLength-3) + "...");
                            }
                        }
                    }
                };
                finder.find((OAObject) obj);
                s = vs.getValue();
            }        
            else {
                s = ((OAObject) obj).getPropertyAsString(propertyPath, fmt);
            }
        }
        else {
            obj = getValue(obj);
            s = OAConv.toString(obj, fmt);
        }
        return s;
    }

    // calls the set method on the actualHub.ao
    public void setValue(Object value) {
        String fmt = getFormat();
        Object obj = getHub().getAO();
        setValue(obj, value, fmt);
    }

    public void setValue(Object obj, Object value) {
        String fmt = getFormat();
        setValue(obj, value, fmt);
    }

    public void setValue(Object obj, Object newValue, String fmt) {
        if (obj == null) {
            return;
        }
        
        // conversion
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
                if (maskPasswordValue.equals(text)) return; // no change
                text = OAString.getSHAHash(text);
            } else if (conversion == 'P' || conversion == 'p') {
                if (maskPasswordValue.equals(text)) return; // no change
                text = OAString.getSHAHash(text);
            } else if (conversion == 'E' || conversion == 'e') {
                try {
                    if (maskPasswordValue.equals(text)) return; // no change
                    text = OAEncryption.encrypt(text);
                } catch (Exception e) {
                    throw new RuntimeException("encryption failed", e);
                }
            }
            newValue = text;
        }        
        
        if (obj instanceof OAObject) {
            ((OAObject) obj).setProperty(propertyPath, newValue, fmt);
        }
    }

    public void setValueDirectly(Object obj, Object newValue) {
        if (obj instanceof OAObject) {
            ((OAObject) obj).setProperty(propertyPath, newValue, null);
        }
    }    
    
    /**
     * Flag to enable undo, default is true.
     */
    public void setEnableUndo(boolean b) {
        bEnableUndo = b;
    }

    public boolean getEnableUndo() {
        return bEnableUndo;
    }

    /**
     * Popup message used to confirm button click before running code.
     */
    public void setConfirmMessage(String msg) {
        confirmMessage = msg;
    }

    /**
     * Popup message used to confirm button click before running code.
     */
    public String getConfirmMessage() {
        return confirmMessage;
    }

    @Override
    public void beforePropertyChange(HubEvent e) {
        // TODO Auto-generated method stub
        super.beforePropertyChange(e);
    }

    /*
     * public void setNameValueHub(Hub<String> hub) { this.hubNameValue = hub; } public Hub<String> getNameValueHub() { return hubNameValue; }
     */

    /**
     * confirm a new change.
     */
    protected boolean confirmPropertyChange(final Object obj, Object newValue) {
        String confirmMessage = getConfirmMessage();
        String confirmTitle = "Confirm";

        if (obj instanceof OAObject) {
            Object objx = obj;
            String prop;
            if (oaPropertyPath != null && oaPropertyPath.hasLinks()) {
                prop = endPropertyName;
                objx = oaPropertyPath.getLastLinkValue(obj);
            }
            else {
                prop = propertyPath;
            }
            if (objx instanceof OAObject) {
                OAObjectCallback em = OAObjectCallbackDelegate.getConfirmPropertyChangeObjectCallback((OAObject) objx, prop, newValue, confirmMessage, confirmTitle);
                confirmMessage = em.getConfirmMessage();
                confirmTitle = em.getConfirmTitle();
            }
        }

        boolean result = true;
        if (OAString.isNotEmpty(confirmMessage)) {
            if (OAString.isEmpty(confirmTitle)) {
                confirmTitle = "Confirmation";
            }

            if (confirmMessage != null && confirmMessage.indexOf("<%=") >= 0 && obj instanceof OAObject) {
                OATemplate temp = new OATemplate(confirmMessage);
                temp.setProperty("newValue", newValue); // used by <%=$newValue%>
                confirmMessage = temp.process((OAObject) obj);
                if (confirmMessage != null && confirmMessage.indexOf('<') >= 0 && confirmMessage.toLowerCase().indexOf("<html>") < 0) {
                    confirmMessage = "<html>" + confirmMessage;
                }
            }

            result = onConfirmPropertyChangeShowOptionDialog(confirmMessage, confirmTitle);
        }
        return result;
    }

    protected boolean onConfirmPropertyChangeShowOptionDialog(String confirmMessage, String confirmTitle) {
        return true;
    }


    /**
     * Converts a value to correct type needed for setMethod
     */
    public Object getConvertedValue(Object value, String fmt) {
        value = OAConv.convert(endPropertyClass, value, fmt);
        return value;
    }

    private HubProp hpViewOnly;

    public void setViewOnly(boolean b) {
        if (b) {
            if (hpViewOnly == null) {
                hpViewOnly = getEnabledChangeListener().addOnlySuperAdmin(); // viewOnly, unless OAContext.SuperAdmin=true
            }
        }
        else {
            if (hpViewOnly != null) {
                getEnabledChangeListener().remove(hpViewOnly);
                hpViewOnly = null;
            }
        }
    }

    public void setReadOnly(boolean b) {
        setViewOnly(b);
    }

    /**
     * Used to verify a property change.
     *
     * @return null if no errors, else error message
     */
    public String isValid(final Object obj, Object newValue) {
        if (!bUseObjectCallback) {
            return null;
        }
        if (!(obj instanceof OAObject)) {
            return null;
        }
        OAObject oaObj = (OAObject) obj;

        String fmt = getFormat();
        newValue = getConvertedValue(newValue, fmt);

        Object objx = obj;
        String prop;
        if (oaPropertyPath != null && oaPropertyPath.hasLinks()) {
            prop = endPropertyName;
            objx = oaPropertyPath.getLastLinkValue(obj);
        }
        else {
            prop = propertyPath;
        }

        String result = null;
        if (objx instanceof OAObject) {
            OAObjectCallback em = OAObjectCallbackDelegate.getVerifyPropertyChangeObjectCallback(OAObjectCallback.CHECK_ALL, (OAObject) objx, prop, null, newValue);
            if (!em.getAllowed()) {
                result = em.getResponse();
                Throwable t = em.getThrowable();
                if (OAString.isEmpty(result)) {
                    if (t != null) {
                        for (; t != null; t = t.getCause()) {
                            result = t.getMessage();
                            if (OAString.isNotEmpty(result)) {
                                break;
                            }
                        }
                        if (OAString.isEmpty(result)) {
                            result = em.getThrowable().toString();
                        }
                    }
                    else {
                        result = "invalid value";
                    }
                }
            }
        }
        return result;
    }

    private boolean bDefaultFormat;
    private String defaultFormat;

    /**
     * Returns format to use for displaying value as a String.
     *
     * @see OADate#OADate see OAConverterNumber#OAConverterNumber
     */
    public String getFormat() {
        if (format != null) {
            return format;
        }
        if (!bDefaultFormat) {
            bDefaultFormat = true;
            if (oaPropertyPath != null) {
                defaultFormat = oaPropertyPath.getFormat();
            }
            if (defaultFormat == null) {
                defaultFormat = OAConverter.getFormat(endPropertyClass);
            }
        }

        if (bUseObjectCallback) {
            Object objx = hub.getAO();
            if (objx instanceof OAObject) {
                String prop;
                if (oaPropertyPath != null && oaPropertyPath.hasLinks()) {
                    objx = oaPropertyPath.getLastLinkValue(objx);
                }
                if (objx instanceof OAObject) {
                    return OAObjectCallbackDelegate.getFormat((OAObject) objx, endPropertyName, defaultFormat);
                }
            }
        }
        return defaultFormat;
    }

    /**
     * Format used to display this property. Used to format Date, Times and Numbers. set to "" (blank) for no formatting. If null, then the
     * default format will be used.
     *
     * @see OADate#OADate see OAConverterNumber#OAConverterNumber
     */
    public void setFormat(String fmt) {
        String old = this.format;
        this.format = fmt;
        bDefaultFormat = true;
        defaultFormat = null;
        if (OACompare.isNotEqual(this.format, old)) {
            callUpdate();
        }
    }

    /**
     * Utility used to "see" if this component or any of its parent containers are disabled.
     */
    public boolean isParentEnabled() {
        return true;
    }

    public void setFontPropertyPath(String pp) {
        String old = this.fontPropertyPath;
        fontPropertyPath = pp;
        if (OAString.isNotEmpty(pp)) {
            getChangeListener().add(hub, pp);
        }
        if (OACompare.isNotEqual(this.fontPropertyPath, old)) {
            callUpdate();
        }
    }

    public String getFontProperty() {
        return fontPropertyPath;
    }

    public void setForegroundColorPropertyPath(String pp) {
        String old = this.foregroundColorPropertyPath;
        this.foregroundColorPropertyPath = pp;
        if (OAString.isNotEmpty(pp)) {
            getChangeListener().add(hub, pp);
        }
        if (OACompare.isNotEqual(this.foregroundColorPropertyPath, old)) {
            callUpdate();
        }
    }

    public String getForegroundColorPropertyPath() {
        return foregroundColorPropertyPath;
    }

    public void setBackgroundColorPropertyPath(String pp) {
        String old = this.backgroundColorPropertyPath;
        this.backgroundColorPropertyPath = pp;
        if (OAString.isNotEmpty(pp)) {
            getChangeListener().add(hub, pp);
        }
        if (OACompare.isNotEqual(this.backgroundColorPropertyPath, old)) {
            callUpdate();
        }
    }

    public String getBackgroundColorPropertyPath() {
        return backgroundColorPropertyPath;
    }

    public void setIconColorPropertyPath(String pp) {
        String old = iconColorPropertyPath;
        iconColorPropertyPath = pp;
        if (OAString.isNotEmpty(pp)) {
            getChangeListener().add(hub, pp);
        }
        if (OACompare.isNotEqual(this.iconColorPropertyPath, old)) {
            callUpdate();
        }
    }

    public String getIconColorPropertyPath() {
        return iconColorPropertyPath;
    }

    public void setToolTipTextPropertyPath(String pp) {
        String old = this.toolTipTextPropertyPath;
        this.toolTipTextPropertyPath = pp;
        if (OAString.isNotEmpty(pp)) {
            getChangeListener().add(hub, pp);
        }
        if (OACompare.isNotEqual(this.toolTipTextPropertyPath, old)) {
            callUpdate();
        }
    }

    public String getToolTipTextPropertyPath() {
        return toolTipTextPropertyPath;
    }

    /**
     * Root directory path where images are stored.
     */
    public void setImageDirectory(String s) {
        String old = this.imageDirectory;
        if (s != null) {
            s += "/";
            s = OAString.convert(s, "\\", "/");
            s = OAString.convert(s, "//", "/");
        }
        this.imageDirectory = s;
        if (OACompare.isNotEqual(this.imageDirectory, old)) {
            callUpdate();
        }
    }

    /**
     * Root directory path where images are stored.
     */
    public String getImageDirectory() {
        return imageDirectory;
    }

    /**
     * Class path where images are stored.
     */
    public void setImageClassPath(Class root, String path) {
        String old = this.imageClassPath;
        this.rootImageClassPath = root;
        this.imageClassPath = path;
        if (OACompare.isNotEqual(this.imageClassPath, old)) {
            callUpdate();
        }
    }

    public void setImagePropertyPath(String pp) {
        String old = this.imagePropertyPath;
        this.imagePropertyPath = pp;
        if (OAString.isNotEmpty(pp)) {
            getChangeListener().add(hub, pp);
        }
        if (OACompare.isNotEqual(this.imagePropertyPath, old)) {
            callUpdate();
        }
    }

    public String getImagePropertyPath() {
        return imagePropertyPath;
    }

    /**
     * The "word(s)" to use for the empty slot (null value). Example: "none of the above". Default: "" (blank). Set to null if none should be
     * used
     */
    public String getNullDescription() {
        return nullDescription;
    }

    public void setNullDescription(String s) {
        String old = this.nullDescription;
        this.nullDescription = s;
        if (OACompare.isNotEqual(this.nullDescription, old)) {
            callUpdate();
        }
    }

    /**
     * Used to listen to additional changes that will then call this.update()
     */
    public HubChangeListener getChangeListener() {
        if (changeListener != null) {
            return changeListener;
        }
        changeListener = new MyHubChangeListener() {
            @Override
            protected void onChange() {
                OAUIController.this.callUpdate();
            }
        };
        return changeListener;
    }

    public HubChangeListener getEnabledChangeListener() {
        if (changeListenerEnabled != null) {
            return changeListenerEnabled;
        }
        changeListenerEnabled = new MyHubChangeListener() {
            @Override
            protected void onChange() {
                OAUIController.this.callUpdate();
            }
        };
        return changeListenerEnabled;
    }

    public HubChangeListener getVisibleChangeListener() {
        if (changeListenerVisible != null) {
            return changeListenerVisible;
        }
        changeListenerVisible = new MyHubChangeListener() {
            @Override
            protected void onChange() {
                OAUIController.this.callUpdate();
            }
        };
        return changeListenerVisible;
    }

    public HubProp addEnabledCheck(Hub hub, String pp) {
        return getEnabledChangeListener().add(hub, pp);
    }

    public HubProp addEnabledCheck(Hub hub, String pp, Object value) {
        return getEnabledChangeListener().add(hub, pp, value);
    }

    public HubProp addEnabledCheck(Hub hub, String property, HubChangeListener.Type type) {
        return getEnabledChangeListener().add(hub, property, type);
    }

    public HubProp addEnabledCheck(Hub hub, HubChangeListener.Type type) {
        return getEnabledChangeListener().add(hub, type);
    }

    public HubProp addEnabledEditQueryCheck(Hub hub, String propertyName) {
        return getEnabledChangeListener().addObjectCallbackEnabled(hub, propertyName);
    }

    public HubProp addEnabledObjectCallbackCheck(Hub hub, String propertyName) {
        return getEnabledChangeListener().addObjectCallbackEnabled(hub, propertyName);
    }

    public HubProp addVisibleCheck(Hub hub, String pp) {
        return getVisibleChangeListener().add(hub, pp);
    }

    public HubProp addVisibleCheck(Hub hub, String pp, Object value) {
        return getVisibleChangeListener().add(hub, pp, value);
    }

    public HubProp addVisibleCheck(Hub hub, String property, HubChangeListener.Type type) {
        return getVisibleChangeListener().add(hub, property, type);
    }

    public HubProp addVisibleEditQueryCheck(Hub hub, String propertyName) {
        return getVisibleChangeListener().addObjectCallbackVisible(hub, propertyName);
    }

    public HubProp addVisibleObjectCallbackCheck(Hub hub, String propertyName) {
        return getVisibleChangeListener().addObjectCallbackVisible(hub, propertyName);
    }


    /**
     * Called to have component update itself.
     * This will call updateComponent and updateLabel
     */
    protected void callUpdate() {
        if (bIgnoreUpdate) return;
        _update();
    }

    protected void _update() {
        if (bIgnoreUpdate) return;

        final HubEvent he = OAThreadLocalDelegate.getCurrentHubEvent();
        if (heLastUpdate != null && (he == heLastUpdate)) {
            return;
        }
        heLastUpdate = he;

        if (isVisibleListenerEnabled()) {
            // check to see if component is visible
            if (!isVisibleOnScreen()) {
                return;
            }
        }
        
        Object obj;
        if (hub != null) {
            obj = hub.getAO();
        }
        else {
            obj = null;
        }
        
        updateComponent(obj);
        updateLabel(obj);
    }



    
    public boolean isVisible() {
        return getVisibleChangeListener().getValue();
    }
    public boolean isEnabled() {
        return getEnabledChangeListener().getValue();
    }
    
    public String getEnabledMessage() {
        return enabledMessage;
    }

    public String getVisibleMessage() {
        return visibleMessage;
    }

    /** minimum number of characters to display (number of characters). */
    public void setMinDisplay(int x) {
        this.minDisplay = x;
    }
    public int getMinDisplay() {
        return this.minDisplay;
    }

    /** max number of characters to display (number of characters). */
    public void setMaxDisplay(int x) {
        this.maxDisplay = x;
    }
    public int getMaxDisplay() {
        return this.maxDisplay;
    }
    
    /** max input length for input (number of characters). */
    public void setMaxLength(int x) {
        maxLength = x;
    }


    public int getMaxImageHeight() {
        return maxImageHeight;
    }

    public void setMaxImageHeight(int maxImageHeight) {
        int old = this.maxImageHeight;
        this.maxImageHeight = maxImageHeight;
        if (OACompare.isNotEqual(this.maxImageHeight, old)) {
            callUpdate();
        }
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    public void setMaxImageWidth(int maxImageWidth) {
        int old = this.maxImageWidth;
        this.maxImageWidth = maxImageWidth;
        if (OACompare.isNotEqual(this.maxImageWidth, old)) {
            callUpdate();
        }
    }

    public void setHtml(boolean b) {
        this.bHtml = b;
    }

    public boolean getHtml() {
        return this.bHtml;
    }

    /**
     * Description to use for Undo and Redo presentation names.
     *
     * @see OAUndoableEdit#setPresentationName
     */
    public void setUndoDescription(String s) {
        undoDescription = s;
    }

    /**
     * Description to use for Undo and Redo presentation names.
     * @see OAUndoableEdit#setPresentationName
     */
    public String getUndoDescription() {
        return undoDescription;
    }

    @Override
    public void afterAdd(HubEvent e) {
        if (bIsHubCalc) {
            callUpdate();
        }
        else if (bListenToHubSize) {
            if (getHub().size() == 1) {
                callUpdate();
            }
        }
    }

    @Override
    public void afterRemove(HubEvent e) {
        if (bIsHubCalc) {
            callUpdate();
        }
        else if (bListenToHubSize) {
            if (getHub().size() == 0) {
                callUpdate();
            }
        }
    }

    @Override
    public void afterRemoveAll(HubEvent e) {
        if (bIsHubCalc) {
            callUpdate();
        }
        else if (bListenToHubSize) {
            callUpdate();
        }
    }

    @Override
    public void onNewList(HubEvent e) {
        callUpdate();
    }

    @Override
    public void afterInsert(HubEvent e) {
        if (bIsHubCalc) {
            callUpdate();
        }
        else if (bListenToHubSize) {
            if (getHub().size() == 1) {
                callUpdate();
            }
        }
    }

    @Override
    public void afterChangeActiveObject(HubEvent e) {
        afterChangeActiveObject();
    }

    @Override
    public void afterNewList(HubEvent e) {
        callUpdate();
    }

    @Override
    public void afterPropertyChange(final HubEvent e) {
        Object ao = getHub().getAO();
        if (bAoOnly) {
            if (ao == null || e.getObject() != ao) {
                return;
            }
        }
        if (!isListeningTo(hub, e.getObject(), e.getPropertyName())) {
            return;
        }
        _afterPropertyChange(e);
    }

    protected void _afterPropertyChange(HubEvent e) {
        afterPropertyChange();
        callUpdate();
    }

    // called if the actual property is changed in the actualHub.activeObject
    protected void afterPropertyChange() {
    }

    protected void afterChangeActiveObject() {
        callUpdate();
    }

    public void setDisplayTemplate(String s) {
        this.displayTemplate = s;
        templateDisplay = null;
    }

    public String getDisplayTemplate() {
        return displayTemplate;
    }

    public OATemplate getTemplateForDisplay() {
        if (OAString.isNotEmpty(getDisplayTemplate())) {
            if (templateDisplay == null) {
                templateDisplay = new OATemplate<>(getDisplayTemplate());
            }
        }
        return templateDisplay;
    }

    /**
     * Used to display values, uses display template if defined.
     */
    public String getDisplayText(Object obj, String defaultText) {
        obj = getRealObject(obj);
        if (!(obj instanceof OAObject)) {
            return defaultText;
        }

        String s = getDisplayTemplate();
        if (OAString.isEmpty(s)) {
            return defaultText;
        }

        defaultText = getTemplateForDisplay().process((OAObject) obj);
        if (defaultText != null && defaultText.indexOf('<') >= 0 && defaultText.toLowerCase().indexOf("<html>") < 0) {
            defaultText = "<html>" + defaultText;
        }

        return defaultText;
    }

    public void setToolTipTextTemplate(String s) {
        this.toolTipTextTemplate = s;
        templateToolTipText = null;
    }

    public String getToolTipTextTemplate() {
        return toolTipTextTemplate;
    }

    public OATemplate getTemplateForToolTipText() {
        if (OAString.isNotEmpty(getToolTipTextTemplate())) {
            if (templateToolTipText == null) {
                templateToolTipText = new OATemplate<>(getToolTipTextTemplate());
            }
        }
        return templateToolTipText;
    }

    public String getToolTipText(Object obj, String ttDefault) {
        obj = getRealObject(obj);
        Object objx = obj;

        if (obj instanceof OAObject) {
            if (OAString.isNotEmpty(toolTipTextPropertyPath)) {
                ttDefault = ((OAObject) obj).getPropertyAsString(toolTipTextPropertyPath);
            }

            String s = getToolTipTextTemplate();
            if (OAString.isNotEmpty(s)) {
                ttDefault = s;
            }

            String prop;
            if (oaPropertyPath != null && oaPropertyPath.hasLinks()) {
                objx = oaPropertyPath.getLastLinkValue(objx);
            }
            if (objx instanceof OAObject) {
                ttDefault = OAObjectCallbackDelegate.getToolTip((OAObject) objx, endPropertyName, ttDefault);
            }
        }
        else {
            if (OAString.isNotEmpty(toolTipTextPropertyPath) || OAString.isNotEmpty(getToolTipTextTemplate())) {
                ttDefault = null;
            }
        }

        if (ttDefault != null && ttDefault.indexOf("<%=") >= 0 && objx instanceof OAObject) {
            if (templateToolTipText == null || !ttDefault.equals(templateToolTipText.getTemplate())) {
                templateToolTipText = new OATemplate(ttDefault);
            }
            ttDefault = templateToolTipText.process((OAObject) obj, (OAObject) objx);
        }

        if (ttDefault != null && ttDefault.indexOf('<') >= 0 && ttDefault.toLowerCase().indexOf("<html>") < 0) {
            ttDefault = "<html>" + ttDefault;
        }

        return ttDefault;
    }

    /**
     * Used when enabledVisibleListener(true) is set up. Will be called when component is in a visible window/tab. default is to call update().
     */
    protected void onVisibleListenerChange() {
        callUpdate();
    }


    protected boolean isListeningTo(Hub hub, Object object, String prop) {
        if (hub == null || object == null || prop == null) {
            return false;
        }

        if (getHub() == hub && prop.equalsIgnoreCase(getHubListenerPropertyName())) {
            if (!bAoOnly || hub.getAO() == object) {
                return true;
            }
        }

        final MyHubChangeListener[] mcls = new MyHubChangeListener[] { changeListener, changeListenerEnabled, changeListenerVisible };
        for (MyHubChangeListener mcl : mcls) {
            if (mcl == null) {
                continue;
            }
            if (mcl.originalIsListeningTo(hub, object, prop)) {
                return true;
            }
        }
        return false;
    }

    // Shares hubListeners between hub and all 3 hubChangeListeners
    protected abstract class MyHubChangeListener extends HubChangeListener {
        @Override
        public boolean isListeningTo(Hub hub, Object object, String property) {
            // need to check others, since the hubListener is shared across the changeListeners
            return OAUIController.this.isListeningTo(hub, object, property);
        }

        public boolean originalIsListeningTo(Hub hub, Object object, String property) {
            // just check this one by calling the super
            return super.isListeningTo(hub, object, property);
        }

        @Override
        public void remove(Hub hub, String prop) {
            final MyHubChangeListener[] mcls = new MyHubChangeListener[] { changeListener, changeListenerEnabled, changeListenerVisible };

            HubProp hp = null;
            for (MyHubChangeListener mcl : mcls) {
                if (mcl == null) {
                    continue;
                }

                for (HubProp hpx : mcl.hubProps) {
                    if (hpx.hub != hub) {
                        continue;
                    }
                    if (hpx.hubListener == null) {
                        continue;
                    }
                    if (!OAString.equals(prop, hpx.propertyPath)) {
                        continue;
                    }
                    hp = hpx;
                    break;
                }
            }
            if (hp == null) {
                return;
            }

            int cnt = 0;
            for (MyHubChangeListener mcl : mcls) {
                if (mcl == null) {
                    continue;
                }
                for (HubProp hpx : mcl.hubProps) {
                    if (hpx.hubListener == hp.hubListener) {
                        cnt++;
                    }
                }
            }

            if (cnt == 1) {
                hp.hub.removeHubListener(hp.hubListener);
            }
            hp.hubListener = null;
        }

        @Override
        public void close() {
            final MyHubChangeListener[] mcls = new MyHubChangeListener[] { changeListener, changeListenerEnabled, changeListenerVisible };

            for (final HubProp hp : hubProps) {
                if (hp.bIgnore) {
                    continue;
                }
                if (hp.hubListener == null) {
                    continue;
                }
                if (hp.hub == OAUIController.this.hub) {
                    hp.hubListener = null;
                    continue;
                }

                boolean b = false;
                for (MyHubChangeListener mcl : mcls) {
                    if (mcl == null) {
                        continue;
                    }
                    if (mcl == this) {
                        continue;
                    }
                    for (HubProp hpx : mcl.hubProps) {
                        if (hpx.hubListener == hp.hubListener) {
                            b = true;
                            break;
                        }
                    }
                }
                if (!b && hp.hub != null) {
                    hp.hub.removeHubListener(hp.hubListener);
                }
                for (HubProp hpx : hubProps) {
                    if (hpx == hp) {
                        continue;
                    }
                    if (hpx.hubListener == hp.hubListener) {
                        hpx.hubListener = null;
                    }
                }
                hp.hubListener = null;
            }
        }

        @Override
        protected void assignHubListener(HubProp newHubProp) {
            if (!_assignHubListener(newHubProp)) {
                super.assignHubListener(newHubProp);
            }
        }

        protected boolean _assignHubListener(HubProp newHubProp) {
            if (OAUIController.this.hub == newHubProp.hub) {
                if (newHubProp.propertyPath == null) {
                    return true;
                }
                if (newHubProp.propertyPath.indexOf('.') < 0) {
                    if (newHubProp.hub.getOAObjectInfo().getCalcInfo(newHubProp.propertyPath) == null) {
                        // 20221011
                        OALinkInfo lix = newHubProp.hub.getOAObjectInfo().getLinkInfo(newHubProp.propertyPath);
                        if (lix == null || lix.getType() == OALinkInfo.ONE) {
                            return true;
                        }
                    }
                }
                if (newHubProp.propertyPath.equalsIgnoreCase(OAUIController.this.propertyPath)) {
                    return true;
                }
            }

            Hub h = OAUIController.this.hub;
            if (h != null) {
                h = h.getLinkHub(true);
                if (h != null && h == newHubProp.hub) {
                    if (newHubProp.propertyPath == null) {
                        return true;
                    }
                }
            }

            final MyHubChangeListener[] mcls = new MyHubChangeListener[] { changeListener, changeListenerEnabled, changeListenerVisible };
            for (MyHubChangeListener mcl : mcls) {
                if (mcl == null) {
                    continue;
                }
                for (HubProp hp : mcl.hubProps) {
                    if (hp.bIgnore) {
                        continue;
                    }
                    if (hp.hub != newHubProp.hub) {
                        continue;
                    }
                    if (hp.hubListener == null) {
                        continue;
                    }
                    if (newHubProp.propertyPath == null) {
                        newHubProp.hubListener = hp.hubListener;
                        return true;
                    }
                    if (newHubProp.propertyPath.indexOf('.') < 0) {
                        if (newHubProp.hub != null && newHubProp.hub.getOAObjectInfo().getCalcInfo(newHubProp.propertyPath) == null) {
                            newHubProp.hubListener = hp.hubListener;
                            return true;
                        }
                    }
                    if (!newHubProp.propertyPath.equalsIgnoreCase(hp.propertyPath)) {
                        continue;
                    }
                    newHubProp.hubListener = hp.hubListener;
                    return true;
                }
            }
            return false;
        }
    }
    
    public boolean isRequired() {
        return bRequired;
    }

    public static String getMaskPasswordValue() {
        return maskPasswordValue;
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
    
    
    public boolean isVisibleListenerEnabled() {
        return false;
    }

    public boolean isVisibleOnScreen() {
        return true;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return this.title;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return this.description;
    }
    
    public void setCompletedMessage(String msg) {
        this.completedMessage = msg;
    }
    public String getCompletedMessage() {
        return this.completedMessage;
    }

    
    /** Called when a change is necessary for UI component. */
    public abstract void updateComponent(Object object);

    public abstract void updateLabel(Object object);

}

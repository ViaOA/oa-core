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
package com.viaoa.object;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.util.OAString;

/**
 * Metadata describing a calculated property of an OAObject. A calculated
 * property does not store a value directly; instead it is derived on demand
 * using a getter method. Calculated properties are fully integrated into the
 * OA Object Graph and participate in UI binding and change notification.
 *
 * <p>Each calculated property may declare a list of dependent properties.
 * When any dependency changes, the calculated property is automatically
 * invalidated and refreshed, ensuring that the UI and distributed clients
 * always see the correct value without requiring explicit update logic.</p>
 *
 * <p>Calculated properties are not persisted and do not affect dirty tracking,
 * but they behave like normal properties for display and navigation purposes.
 * This supports dynamic domain behavior without introducing additional storage
 * fields or duplication of database state.</p>
 *
 * @see OAPropertyInfo
 * @see OAObjectInfo
 * @see OAObject
 */
public class OACalcInfo implements java.io.Serializable {
    static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(OACalcInfo.class.getName());
    
    String name;
    String lowerName;
    String[] dependentProperties;  // dependent properties
    private OACalculatedProperty oaCalculatedProperty;
    private Class classType;

    /** 20131027
     *  true if this calcProp is for the whole Hub, and the method has a static method with a Hub param
     */
    boolean bIsForHub;  
    private transient Method objectCallbackMethod;
    private boolean isHtml;
    private boolean isObjectStatus;
    
    private String[] viewDependentProperties;
    private String[] contextDependentProperties;

    private String enabledProperty;
    private boolean enabledValue;
    private String visibleProperty;
    private boolean visibleValue;

    private String contextEnabledProperty;
    private boolean contextEnabledValue;
    private String contextVisibleProperty;
    private boolean contextVisibleValue;
    
    /** 
     Create new Calculated Property.  
     * <pre>
     * Example:  
     *   new CalcInfo("totalCostOfOrder",String { "orderItem.qty", "orderItem.product.cost", "customer.freight", "customer.state.taxRate" } );            
     * </pre>
     * @param name name of calculated property
     * @param props array of depend property paths
     */
    public OACalcInfo(String name, String[] props) {
        this.name = name;
        dependentProperties = props;
    }
    public OACalcInfo(String name, String[] props, boolean bIsForHub) {
        this.name = name;
        dependentProperties = props;
        this.bIsForHub = bIsForHub;
    }

    public Class getClassType() {
        return classType;
    }
    public void setClassType(Class classType) {
        this.classType = classType;
    }
    
    /** get Calculated Property name */
    public String getName() {
        return name;
    }
    
    public String getLowerName() {
        if (OAString.isNotEmpty(lowerName)) {
            return lowerName;
        }
        return OAString.mfcl(name);
    }

    public void setLowerName(String name) {
        this.lowerName = name;
    }
    
    
    public boolean isHtml() {
        return isHtml;
    }
    public void setHtml(boolean b) {
        this.isHtml = b;
    }
    
	public void setObjectStatus(boolean b) {
		this.isObjectStatus = b;
	}
	public boolean getObjectStatus() {
		return this.isObjectStatus;
	}

	public boolean isObjectStatus() {
		return this.isObjectStatus;
	}
    
    /** get property paths of all dependent properties */
    public String[] getDependentProperties() {
        return dependentProperties;
    }
    public void setDependentProperties(String[] props) {
        dependentProperties = props;
    }

    public boolean getIsForHub() {
        return bIsForHub;
    }

    public OACalculatedProperty getOACalculatedProperty() {
        return oaCalculatedProperty;
    }
    public void setOACalculatedProperty(OACalculatedProperty c) {
        oaCalculatedProperty = c;
    }

    public void setViewDependentProperties(String[] ss) {
        this.viewDependentProperties = ss;
    }
    public String[] getViewDependentProperties() {
        return this.viewDependentProperties;
    }

    public void setContextDependentProperties(String[] ss) {
        this.contextDependentProperties = ss;
    }
    public String[] getContextDependentProperties() {
        return this.contextDependentProperties;
    }
    
    public String getEnabledProperty() {
        return enabledProperty;
    }
    public void setEnabledProperty(String s) {
        enabledProperty = s;
    }
    public boolean getEnabledValue() {
        return enabledValue;
    }
    public void setEnabledValue(boolean b) {
        enabledValue = b;
    }
    public String getVisibleProperty() {
        return visibleProperty;
    }
    public void setVisibleProperty(String s) {
        visibleProperty = s;
    }
    public boolean getVisibleValue() {
        return visibleValue;
    }
    public void setVisibleValue(boolean b) {
        visibleValue = b;
    }

    public String getContextEnabledProperty() {
        return contextEnabledProperty;
    }
    public void setContextEnabledProperty(String s) {
        contextEnabledProperty = s;
    }
    public boolean getContextEnabledValue() {
        return contextEnabledValue;
    }
    public void setContextEnabledValue(boolean b) {
        contextEnabledValue = b;
    }
    public String getContextVisibleProperty() {
        return contextVisibleProperty;
    }
    public void setContextVisibleProperty(String s) {
        contextVisibleProperty = s;
    }
    public boolean getContextVisibleValue() {
        return contextVisibleValue;
    }
    public void setContextVisibleValue(boolean b) {
        contextVisibleValue = b;
    }

    public void setObjectCallbackMethod(Method m) {
        this.objectCallbackMethod = m;
    }
    public Method getObjectCallbackMethod() {
        return objectCallbackMethod;
    }

}


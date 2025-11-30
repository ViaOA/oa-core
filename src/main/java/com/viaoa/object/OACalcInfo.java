/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
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
     * Creates a new calculated property definition using the supplied name and
     * dependent property paths. The calculated property does not store its own
     * value; instead, its getter method derives the value dynamically whenever
     * requested.  
     *
     * <p>The list of dependent property paths is used to determine when this
     * calculated property should be invalidated and refreshed. Any change to a
     * dependent property triggers recalculation, ensuring consistent and current
     * values throughout the OA Object Graph.</p>
     *
     * @param name  the name of the calculated property
     * @param props the array of property paths that this calculated property
     *              depends on
     */
    public OACalcInfo(String name, String[] props) {
        this.name = name;
        dependentProperties = props;
    }
    
    /**
     * Creates a new calculated property definition with support for Hub-level
     * calculations. In addition to the calculated property name and dependent
     * property paths, this constructor allows specifying whether the calculation
     * applies to an entire Hub rather than an individual object.
     *
     * <p>When {@code bIsForHub} is {@code true}, the corresponding calculated
     * property is expected to be implemented as a static method that receives a
     * Hub parameter, enabling aggregate computations across all objects in the
     * Hub.</p>
     *
     * @param name       the name of the calculated property
     * @param props      the array of dependent property paths
     * @param bIsForHub  {@code true} if this calculation is defined for the Hub
     *                    rather than individual objects
     */
    public OACalcInfo(String name, String[] props, boolean bIsForHub) {
        this.name = name;
        dependentProperties = props;
        this.bIsForHub = bIsForHub;
    }

    /**
     * Returns the class type associated with this calculated property. This
     * typically represents the return type of the calculation method.
     *
     * @return the class type for the calculated property, or {@code null} if not set
     */
    public Class getClassType() {
        return classType;
    }
    
    /**
     * Sets the class type associated with this calculated property. This value
     * is typically used to represent the return type of the calculation method.
     *
     * @param classType the class type to associate with the calculated property
     */
    public void setClassType(Class classType) {
        this.classType = classType;
    }
    
    /**
     * Returns the name of the calculated property.
     *
     * @return the calculated property name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the lowercase version of the calculated property name. If a
     * lowercase value has been explicitly set, it is returned; otherwise the
     * lowercase value is generated from the property name.
     *
     * @return the lowercase version of the calculated property name
     */
    public String getLowerName() {
        if (OAString.isNotEmpty(lowerName)) {
            return lowerName;
        }
        return OAString.mfcl(name);
    }

    /**
     * Explicitly sets the lowercase form of the calculated property name.
     *
     * @param name the lowercase name to assign
     */
    public void setLowerName(String name) {
        this.lowerName = name;
    }
    
    
    /**
     * Explicitly sets the lowercase form of the calculated property name.
     *
     * @param name the lowercase name to assign
     */
    public boolean isHtml() {
        return isHtml;
    }

    /**
     * Sets whether the calculated property's value should be treated as
     * HTML content.
     *
     * @param b {@code true} to mark the value as HTML, {@code false} otherwise
     */
    public void setHtml(boolean b) {
        this.isHtml = b;
    }
    
    /**
     * Sets whether this calculated property represents an object status value.
     *
     * @param b {@code true} to indicate the property is an object status value, otherwise {@code false}
     */
	public void setObjectStatus(boolean b) {
		this.isObjectStatus = b;
	}

	/**
	 * Returns whether this calculated property is designated as an
	 * object status value.
	 *
	 * @return {@code true} if this property represents an object status value, otherwise {@code false}
	 */
	public boolean getObjectStatus() {
		return this.isObjectStatus;
	}

	/**
	 * Indicates whether this calculated property is marked as an object
	 * status value. This method is equivalent to {@link #getObjectStatus()}.
	 *
	 * @return {@code true} if the property is an object status value, otherwise {@code false}
	 */
	public boolean isObjectStatus() {
		return this.isObjectStatus;
	}
    
	/**
	 * Returns the list of dependent property paths for this calculated
	 * property. Any change to one of these properties triggers the
	 * recalculation of this property's value.
	 *
	 * @return an array of dependent property paths, or {@code null} if none are defined
	 */
    public String[] getDependentProperties() {
        return dependentProperties;
    }
    
    /**
     * Sets the list of dependent property paths for this calculated
     * property. Changes to any of the specified properties will cause
     * this calculated property to be refreshed.
     *
     * @param props the array of dependent property paths
     */
    public void setDependentProperties(String[] props) {
        dependentProperties = props;
    }

    /**
     * Indicates whether this calculated property is defined for an entire Hub
     * rather than for individual objects. Hub-level calculated properties are
     * expected to be implemented using a static method that receives a Hub
     * parameter.
     *
     * @return {@code true} if the calculation applies to a Hub, otherwise {@code false}
     */
    public boolean getIsForHub() {
        return bIsForHub;
    }

    /**
     * Returns the {@link OACalculatedProperty} annotation associated with
     * this calculated property, if one is defined.
     *
     * @return the annotation for this calculated property, or {@code null} if not set
     */
    public OACalculatedProperty getOACalculatedProperty() {
        return oaCalculatedProperty;
    }
    
    /**
     * Assigns the {@link OACalculatedProperty} annotation metadata for this
     * calculated property. This annotation may contain configuration details
     * used during calculation or evaluation.
     *
     * @param c the annotation instance to associate with this calculated property
     */
    public void setOACalculatedProperty(OACalculatedProperty c) {
        oaCalculatedProperty = c;
    }

    /**
     * Sets the list of view-dependent property paths. These properties
     * influence when the calculated property should be refreshed in a
     * view/UI context.
     *
     * @param ss the array of view-dependent property paths
     */
    public void setViewDependentProperties(String[] ss) {
        this.viewDependentProperties = ss;
    }

    /**
     * Returns the list of view-dependent property paths. These properties
     * are used to determine when the calculated property should be refreshed
     * in a view or UI context.
     *
     * @return an array of view-dependent property paths, or {@code null} if none are defined
     */
    public String[] getViewDependentProperties() {
        return this.viewDependentProperties;
    }

    /**
     * Sets the list of context-dependent property paths. These properties
     * determine when the calculated property should be refreshed based on
     * changes in contextual state.
     *
     * @param ss the array of context-dependent property paths
     */
    public void setContextDependentProperties(String[] ss) {
        this.contextDependentProperties = ss;
    }
    
    /**
     * Returns the list of context-dependent property paths. These properties
     * help determine when the calculated property should be refreshed based on
     * changes in contextual state.
     *
     * @return an array of context-dependent property paths, or {@code null} if none are defined
     */
    public String[] getContextDependentProperties() {
        return this.contextDependentProperties;
    }
    
    /**
     * Returns the name of the property that determines whether this
     * calculated property is enabled.
     *
     * @return the enabling property name, or {@code null} if none is defined
     */
    public String getEnabledProperty() {
        return enabledProperty;
    }
    
    /**
     * Sets the name of the property used to determine whether this
     * calculated property is enabled.
     *
     * @param s the enabling property name
     */
    public void setEnabledProperty(String s) {
        enabledProperty = s;
    }

    /**
     * Returns the explicit enabled value associated with this calculated
     * property. This value is used when the enabling logic is based on a
     * boolean condition.
     *
     * @return {@code true} if the property should be enabled, otherwise {@code false}
     */
    public boolean getEnabledValue() {
        return enabledValue;
    }

    /**
     * Sets the boolean value used to determine whether this calculated
     * property is enabled.
     *
     * @param b {@code true} to mark the property as enabled, otherwise {@code false}
     */
    public void setEnabledValue(boolean b) {
        enabledValue = b;
    }
    
    /**
     * Returns the name of the property used to determine whether this
     * calculated property is visible.
     *
     * @return the visibility property name, or {@code null} if none is defined
     */
    public String getVisibleProperty() {
        return visibleProperty;
    }

    /**
     * Sets the name of the property that controls whether this calculated
     * property is visible.
     *
     * @param s the visibility property name
     */
    public void setVisibleProperty(String s) {
        visibleProperty = s;
    }
    
    /**
     * Returns the explicit visibility value associated with this calculated
     * property. This boolean determines whether the property should be shown.
     *
     * @return {@code true} if the property should be visible, otherwise {@code false}
     */
    public boolean getVisibleValue() {
        return visibleValue;
    }

    /**
     * Sets the explicit visibility value for this calculated property.
     *
     * @param b {@code true} to mark the property as visible, otherwise {@code false}
     */
    public void setVisibleValue(boolean b) {
        visibleValue = b;
    }

    /**
     * Returns the name of the property that determines whether this
     * calculated property is enabled within a contextual scope.
     *
     * @return the context-enabled property name, or {@code null} if none is defined
     */
    public String getContextEnabledProperty() {
        return contextEnabledProperty;
    }

    /**
     * Sets the name of the property used to determine whether this calculated
     * property is enabled within a contextual scope.
     *
     * @param s the context-enabled property name
     */
    public void setContextEnabledProperty(String s) {
        contextEnabledProperty = s;
    }
    
    /**
     * Returns the explicit enabled value used when evaluating whether
     * this calculated property is enabled within a contextual scope.
     *
     * @return {@code true} if the property is contextually enabled, otherwise {@code false}
     */
    public boolean getContextEnabledValue() {
        return contextEnabledValue;
    }
    
    /**
     * Sets the explicit boolean value used to determine whether this
     * calculated property is enabled within a contextual scope.
     *
     * @param b {@code true} to mark the property as contextually enabled, otherwise {@code false}
     */
    public void setContextEnabledValue(boolean b) {
        contextEnabledValue = b;
    }
    
    /**
     * Returns the name of the property that determines whether this
     * calculated property is visible within a contextual scope.
     *
     * @return the context-visible property name, or {@code null} if none is defined
     */
    public String getContextVisibleProperty() {
        return contextVisibleProperty;
    }
    
    /**
     * Sets the explicit boolean value used to determine whether this calculated
     * property is visible within a contextual scope.
     *
     * @param b {@code true} to mark the property as contextually visible,
     *          otherwise {@code false}
     */
    public void setContextVisibleProperty(String s) {
        contextVisibleProperty = s;
    }
    
    /**
     * Returns the explicit visibility value used when determining whether
     * this calculated property is visible within a contextual scope.
     *
     * @return {@code true} if the property should be contextually visible,
     *         otherwise {@code false}
     */
    public boolean getContextVisibleValue() {
        return contextVisibleValue;
    }

    /**
     * Sets the explicit boolean value used to determine whether this calculated
     * property is visible within a contextual scope.
     *
     * @param b {@code true} to mark the property as contextually visible,
     *          otherwise {@code false}
     */
    public void setContextVisibleValue(boolean b) {
        contextVisibleValue = b;
    }

    /**
     * Sets the callback method associated with this calculated property.
     * The callback method is invoked on the object when evaluating the
     * calculated property's value.
     *
     * @param m the method to be used as the callback
     */
    public void setObjectCallbackMethod(Method m) {
        this.objectCallbackMethod = m;
    }

    /**
     * Returns the callback method associated with this calculated property.
     * This method is invoked when evaluating the calculated property's value.
     *
     * @return the callback method, or {@code null} if none is assigned
     */
    public Method getObjectCallbackMethod() {
        return objectCallbackMethod;
    }

}


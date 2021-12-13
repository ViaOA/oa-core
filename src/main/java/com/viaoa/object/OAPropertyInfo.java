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

import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.util.OAString;

public class OAPropertyInfo implements java.io.Serializable {
	static final long serialVersionUID = 1L;

	private String name;

	private int maxLength;
	private int displayLength;

	// UI grid/table column header name
	private String columnName;
	private int columnLength;

	private boolean required;
	private boolean id;
	private boolean unique;
	private Class classType;
	private int decimalPlaces = -1;
	private boolean isBlob;
	private boolean isNameValue;
	private String displayName;

	private boolean isUnicode;
	private boolean isImportMatch;
	private boolean isSHAHash;
	private boolean isEncrypted;
	private Hub<String> hubNameValue;
	private boolean isCurrency;
	private transient Method objectCallbackMethod;
	private boolean isProcessed;
	private boolean isHtml;
	private boolean isTimestamp;
	private boolean bIsPrimitive;

	private String enabledProperty;
	private boolean enabledValue;
	private String visibleProperty;
	private boolean visibleValue;
	private boolean isSubmit;

	private String contextEnabledProperty;
	private boolean contextEnabledValue;
	private String contextVisibleProperty;
	private boolean contextVisibleValue;

	private String[] contextDependentProperties;
	private String[] viewDependentProperties;
	private boolean trackPrimitiveNull = true;

	private OAProperty oaProperty;

	private boolean ignoreTimeZone;
	private String timeZonePropertyPath;
	private boolean isUpper;
	private boolean isLower;
	private boolean sensitiveData;

	public OAPropertyInfo() {
	}

	public Class getClassType() {
		return classType;
	}

	public void setClassType(Class classType) {
		this.classType = classType;
		bIsPrimitive = classType == null ? false : classType.isPrimitive();
	}

	public boolean getIsPrimitive() {
		return bIsPrimitive;
	}

	public boolean getPrimitive() {
		return bIsPrimitive;
	}

	public boolean getId() {
		return id;
	}

	public void setId(boolean id) {
		this.id = id;
	}

	public boolean getUnique() {
		return unique;
	}

	public void setUnique(boolean bUnique) {
		this.unique = bUnique;
	}

	public boolean getProcessed() {
		return isProcessed;
	}

	public void setProcessed(boolean b) {
		this.isProcessed = b;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public int getDisplayLength() {
		return displayLength;
	}

	public void setDisplayLength(int length) {
		this.displayLength = length;
	}

	public int getColumnLength() {
		return columnLength;
	}

	public void setColumnLength(int length) {
		this.columnLength = length;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String colName) {
		this.columnName = colName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLowerName() {
		return OAString.mfcl(name);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String name) {
		this.displayName = name;
	}

	public boolean getRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public void setDecimalPlaces(int x) {
		this.decimalPlaces = x;
	}

	public int getDecimalPlaces() {
		return this.decimalPlaces;
	}

	public boolean isBlob() {
		return isBlob;
	}

	public void setBlob(boolean b) {
		this.isBlob = b;
	}

	public boolean isNameValue() {
		return isNameValue;
	}

	public void setNameValue(boolean b) {
		this.isNameValue = b;
	}

	public boolean isUnicode() {
		return isUnicode;
	}

	public void setUnicode(boolean b) {
		this.isUnicode = b;
	}

	public boolean isImportMatch() {
		return isImportMatch;
	}

	public void setImportMatch(boolean b) {
		this.isImportMatch = b;
	}

	public boolean isEncrypted() {
		return isEncrypted;
	}

	public void setEncrypted(boolean b) {
		this.isEncrypted = b;
	}

	public boolean isSHAHash() {
		return isSHAHash;
	}

	public void setSHAHash(boolean b) {
		this.isSHAHash = b;
	}

	public void setOAProperty(OAProperty p) {
		oaProperty = p;
	}

	public OAProperty getOAProperty() {
		return oaProperty;
	}

	public Hub<String> getNameValues() {
		if (hubNameValue == null) {
			hubNameValue = new Hub<String>(String.class);
		}
		return hubNameValue;
	}

	public boolean isCurrency() {
		return isCurrency;
	}

	public void setCurrency(boolean b) {
		this.isCurrency = b;
	}

	public boolean isHtml() {
		return isHtml;
	}

	public void setHtml(boolean b) {
		this.isHtml = b;
	}

	public boolean isTimestamp() {
		return isTimestamp;
	}

	public void setTimestamp(boolean b) {
		this.isTimestamp = b;
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

	public boolean getTrackPrimitiveNull() {
		return trackPrimitiveNull;
	}

	public void setTrackPrimitiveNull(boolean b) {
		trackPrimitiveNull = b;
	}

	public void setIsSubmit(boolean b) {
		this.isSubmit = b;
	}

	public void setSubmit(boolean b) {
		this.isSubmit = b;
	}

	public boolean getIsSubmit() {
		return this.isSubmit;
	}

	public boolean getSubmit() {
		return this.isSubmit;
	}

	public boolean isSubmit() {
		return this.isSubmit;
	}

	public boolean getIgnoreTimeZone() {
		return ignoreTimeZone;
	}

	public void setIgnoreTimeZone(boolean b) {
		this.ignoreTimeZone = b;
	}

	public String getTimeZonePropertyPath() {
		return timeZonePropertyPath;
	}

	public void setTimeZonePropertyPath(String s) {
		this.timeZonePropertyPath = s;
	}

	public void setUpper(boolean b) {
		this.isUpper = b;
	}

	public boolean getIsUpper() {
		return this.isUpper;
	}

	public boolean getUpper() {
		return this.isUpper;
	}

	public boolean isUpper() {
		return this.isUpper;
	}

	public void setLower(boolean b) {
		this.isLower = b;
	}

	public boolean getIsLower() {
		return this.isLower;
	}

	public boolean getLower() {
		return this.isLower;
	}

	public boolean isLower() {
		return this.isLower;
	}

	public Object getValue(Object obj) {
		return OAObjectReflectDelegate.getProperty((OAObject) obj, name);
	}

	public void setSensitiveData(boolean b) {
		this.sensitiveData = b;
	}

	public boolean getSensitiveData() {
		return sensitiveData;
	}
}

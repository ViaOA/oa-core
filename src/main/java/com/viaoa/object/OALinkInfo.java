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

import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAOne;
import com.viaoa.util.OAString;

/**
 * Defines reference properties between OAObjects.
 * <p>
 * <b>Note:</b> this will be replaced by com.viaoa.model.OALinkPropertyDef <b>WARNING:</b> this object is past with Hub (using RMI), so make
 * sure of transient properties.
 */
// 20141115 removed serializable, so that it is always handled in other object's read/writeObject
public class OALinkInfo { //implements java.io.Serializable {
	static final long serialVersionUID = 1L;
	public static final int ONE = 0;
	public static final int MANY = 1;

	public static final int TYPE_ONE = 0;
	public static final int TYPE_MANY = 1;

	String name;
	String displayName;
	Class toClass;
	int type;
	boolean required;
	boolean cascadeSave; // save, delete of this object will do same with link hub
	boolean cascadeDelete; // save, delete of this object will do same with link hub
	// property that needs to be updated in an inserted object.  same as Hub.propertyToMaster
	protected String reverseName; // reverse property name
	boolean bOwner; // this object is the owner of the linked to object/hub
	boolean bRecursive;
	private boolean bTransient;
	private boolean bAutoCreateNew;
	private String matchHub; // propertyPath to find matching hub
	private String matchProperty; // propertyPath to match, using HubAutoMatch
	boolean mustBeEmptyForDelete; // this link must be empty before other side can be deleted
	private String uniqueProperty; // unique propertyPath
	private String sortProperty; // sort propetyPath
	private boolean sortAsc = true; // sort ascending
	private String seqProperty; // sequence propetyPath
	private boolean couldBeLarge;
	private boolean oneAndOnlyOne;

	private String equalPropertyPath;

	// runtime
	protected transient int cacheSize;
	private OALinkInfo revLinkInfo;
	protected boolean bCalculated;
	protected boolean bProcessed;
	protected boolean bServerSideCalc;
	protected boolean bPrivateMethod; // true if the method is not created, or is private
	protected boolean bNotUsed; // 20180615 flag to know that link is only used one way
	private transient Method uniquePropertyGetMethod;
	private String[] calcDependentProperties;
	private String mergerPropertyPath;
	private OAOne oaOne;
	private OAMany oaMany;
	private Method objectCallbackMethod;
	private String[] viewDependentProperties;
	private String[] contextDependentProperties;
	private String[] usesProperties;
	private String[] pojoNames;

	private String defaultPropertyPath;
	private boolean defaultPropertyPathIsHierarchy;
	private boolean defaultPropertyPathCanBeChanged;

	// default value comes from Context object.  "." is to use this object.
	private String defaultContextPropertyPath;

	private Method schedulerMethod;

	private boolean importMatch;

	public OALinkInfo(String name, Class toClass, int type) {
		this(name, toClass, type, false, false, null, false);
	}

	public OALinkInfo(String name, Class toClass, int type, boolean cascade, String reverseName) {
		this(name, toClass, type, cascade, cascade, reverseName, false);
	}

	public OALinkInfo(String name, Class toClass, int type, boolean cascade, String reverseName, boolean bOwner) {
		this(name, toClass, type, cascade, cascade, reverseName, bOwner);
	}

	public OALinkInfo(String name, Class toClass, int type, boolean cascadeSave, boolean cascadeDelete, String reverseName) {
		this(name, toClass, type, cascadeSave, cascadeDelete, reverseName, false);
	}

	public OALinkInfo(String name, Class toClass, int type, boolean cascadeSave, boolean cascadeDelete, String reverseName,
			boolean bOwner) {
		this.name = name;
		this.toClass = toClass;
		this.type = type;
		this.cascadeSave = cascadeSave;
		this.cascadeDelete = cascadeDelete;
		this.reverseName = reverseName;
		this.bOwner = bOwner;
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof OALinkInfo)) {
			return false;
		}

		OALinkInfo li = (OALinkInfo) obj;

		if (li.toClass != this.toClass) {
			if (li.toClass == null || this.toClass == null) {
				return false;
			}
			if (!li.toClass.equals(this.toClass)) {
				return false;
			}
		}

		if (li.name == null) {
			if (this.name != null) {
				return false;
			}
		} else {
			if (!li.name.equalsIgnoreCase(this.name)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return 1;
	}

	public boolean isOwner() {
		return bOwner;
	}

	/* note: a recursive link cant be owned by itself */
	public boolean getOwner() {
		return bOwner;
	}

	public void setOwner(boolean b) {
		bOwner = b;
	}

	public boolean getRecursive() {
		return bRecursive;
	}

	public void setRecursive(boolean b) {
		bRecursive = b;
	}

	public Class getToClass() {
		return toClass;
	}

	public void setToClass(Class c) {
		this.toClass = c;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getLowerName() {
		return OAString.mfcl(name);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String dn) {
		this.displayName = dn;
	}

	public String getReverseName() {
		return reverseName;
	}

	public void setReverseName(String name) {
		this.reverseName = name;
	}

	public void setTransient(boolean b) {
		this.bTransient = b;
	}

	public boolean getTransient() {
		return bTransient;
	}

	public void setCalculated(boolean b) {
		this.bCalculated = b;
	}

	public boolean getCalculated() {
		return bCalculated;
	}

	public void setProcessed(boolean b) {
		this.bProcessed = b;
	}

	public boolean getProcessed() {
		return bProcessed;
	}

	public boolean getUsed() {
		return !bNotUsed;
	}

	public void setServerSideCalc(boolean b) {
		this.bServerSideCalc = b;
	}

	public boolean getServerSideCalc() {
		return bServerSideCalc;
	}

	public void setPrivateMethod(boolean b) {
		this.bPrivateMethod = b;
	}

	public boolean getPrivateMethod() {
		return this.bPrivateMethod;
	}

	public boolean getCascadeSave() {
		return cascadeSave;
	}

	public void setCascadeSave(boolean b) {
		this.cascadeSave = b;
	}

	public boolean getCascadeDelete() {
		return cascadeDelete;
	}

	public void setCascadeDelete(boolean b) {
		this.cascadeDelete = b;
	}

	public boolean getAutoCreateNew() {
		return bAutoCreateNew;
	}

	public void setAutoCreateNew(boolean bAutoCreateNew) {
		this.bAutoCreateNew = bAutoCreateNew;
	}

	public boolean getMustBeEmptyForDelete() {
		return mustBeEmptyForDelete;
	}

	public void setMustBeEmptyForDelete(boolean b) {
		this.mustBeEmptyForDelete = b;
	}

	/**
	 * Set the number of hubs that will be cached.
	 */
	public void setCacheSize(int x) {
		this.cacheSize = Math.max(0, x);
	}

	public int getCacheSize() {
		return this.cacheSize;
	}

	// 2008/01/02 all of these were created to support the old oa.html package
	public Object getValue(Object obj) {
		return OAObjectReflectDelegate.getProperty((OAObject) obj, name);
	}

	public boolean isLoaded(Object obj) {
		if (!(obj instanceof OAObject)) {
			return true;
		}
		OAObject oaObj = (OAObject) obj;
		return OAObjectPropertyDelegate.isPropertyLoaded(oaObj, name);
	}

	public boolean isLocked(Object obj) {
		if (!(obj instanceof OAObject)) {
			return true;
		}
		OAObject oaObj = (OAObject) obj;
		return OAObjectPropertyDelegate.isPropertyLocked(oaObj, name);
	}

	public void setMatchProperty(String prop) {
		this.matchProperty = prop;
	}

	public String getMatchProperty() {
		return this.matchProperty;
	}

	public void setUniqueProperty(String prop) {
		this.uniqueProperty = prop;
	}

	public String getUniqueProperty() {
		return this.uniqueProperty;
	}

	public void setSortProperty(String prop) {
		this.sortProperty = prop;
	}

	public String getSortProperty() {
		return this.sortProperty;
	}

	public void setSortAsc(boolean b) {
		this.sortAsc = b;
	}

	public boolean isSortAsc() {
		return this.sortAsc;
	}

	public void setSeqProperty(String prop) {
		this.seqProperty = prop;
	}

	public String getSeqProperty() {
		return this.seqProperty;
	}

	public Method getUniquePropertyGetMethod() {
		if (uniquePropertyGetMethod != null) {
			return uniquePropertyGetMethod;
		}
		if (uniqueProperty == null) {
			return null;
		}
		uniquePropertyGetMethod = OAObjectInfoDelegate.getMethod(getToObjectInfo(), "get" + uniqueProperty);
		return uniquePropertyGetMethod;
	}

	// pp = propertyPath to matchingHub
	public void setMatchHub(String pp) {
		this.matchHub = pp;
	}

	public String getMatchHub() {
		return this.matchHub;
	}

	public OALinkInfo getReverseLinkInfo() {
		if (revLinkInfo != null) {
			return revLinkInfo;
		}
		String findName = reverseName;
		if (findName == null) {
			return null;
		}
		for (OALinkInfo lix : getToObjectInfo().getLinkInfos()) {
			if (lix.name != null && findName.equalsIgnoreCase(lix.name)) {
				revLinkInfo = lix;
				break;
			}
		}
		return revLinkInfo;
	}

	private transient OAObjectInfo oi;

	public OAObjectInfo getToObjectInfo() {
		if (oi == null) {
			oi = OAObjectInfoDelegate.getOAObjectInfo(toClass);
		}
		return oi;
	}

	public boolean getCouldBeLarge() {
		return couldBeLarge;
	}

	public void setCouldBeLarge(boolean b) {
		this.couldBeLarge = b;
	}

	public void setOAOne(OAOne o) {
		this.oaOne = o;
	}

	public OAOne getOAOne() {
		return this.oaOne;
	}

	public void setOAMany(OAMany m) {
		this.oaMany = m;
	}

	public OAMany getOAMany() {
		return this.oaMany;
	}

	/** for calc links */
	public String[] getCalcDependentProperties() {
		return calcDependentProperties;
	}

	public void setCalcDependentProperties(String[] props) {
		calcDependentProperties = props;
	}

	public String[] getViewDependentProperties() {
		return viewDependentProperties;
	}

	public void setViewDependentProperties(String[] props) {
		viewDependentProperties = props;
	}

	public String[] getContextDependentProperties() {
		return contextDependentProperties;
	}

	public void setContextDependentProperties(String[] props) {
		contextDependentProperties = props;
	}

	public String[] getUsesProperties() {
		return usesProperties;
	}

	public void setUsesProperties(String[] props) {
		usesProperties = props;
	}

	public String[] getPojoNames() {
		return pojoNames;
	}

	public void setPojoNames(String[] names) {
		pojoNames = names;
	}

	public String getMergerPropertyPath() {
		return mergerPropertyPath;
	}

	public void setMergerPropertyPath(String pp) {
		this.mergerPropertyPath = pp;
	}

	public boolean isOne2One() {
		if (getType() != TYPE_ONE) {
			return false;
		}
		OALinkInfo rli = getReverseLinkInfo();
		if (rli == null) {
			return false;
		}
		if (rli.getType() != TYPE_ONE) {
			return false;
		}
		return true;
	}

	public boolean isOne() {
		return getType() == TYPE_ONE;
	}

	public boolean isMany() {
		return getType() == TYPE_MANY;
	}

	public boolean isMany2Many() {
		if (getType() != TYPE_MANY) {
			return false;
		}
		OALinkInfo rli = getReverseLinkInfo();
		if (rli == null) {
			return false;
		}
		if (rli.getType() != TYPE_MANY) {
			return false;
		}
		return true;
	}

	public boolean isOne2Many() {
		if (getType() != TYPE_ONE) {
			return false;
		}
		OALinkInfo rli = getReverseLinkInfo();
		if (rli == null) {
			return false;
		}
		if (rli.getType() != TYPE_MANY) {
			return false;
		}
		return true;
	}

	public boolean isMany2One() {
		if (getType() != TYPE_MANY) {
			return false;
		}
		OALinkInfo rli = getReverseLinkInfo();
		if (rli == null) {
			return false;
		}
		if (rli.getType() != TYPE_ONE) {
			return false;
		}
		return true;
	}

	private String enabledProperty;
	private boolean enabledValue;
	private String visibleProperty;
	private boolean visibleValue;

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

	private String contextEnabledProperty;
	private boolean contextEnabledValue;
	private String contextVisibleProperty;
	private boolean contextVisibleValue;

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

	public void setSchedulerMethod(Method m) {
		this.schedulerMethod = m;
	}

	public Method getSchedulerMethod() {
		return schedulerMethod;
	}

	public String getDefaultPropertyPath() {
		return defaultPropertyPath;
	}

	public void setDefaultPropertyPath(String pp) {
		this.defaultPropertyPath = pp;
	}

	public boolean getDefaultPropertyPathIsHierarchy() {
		return defaultPropertyPathIsHierarchy;
	}

	public void setDefaultPropertyPathIsHierarchy(boolean b) {
		defaultPropertyPathIsHierarchy = b;
		;
	}

	public boolean getDefaultPropertyPathCanBeChanged() {
		return defaultPropertyPathCanBeChanged;
	}

	public void setDefaultPropertyPathCanBeChanged(boolean b) {
		defaultPropertyPathCanBeChanged = b;
		;
	}

	public String getDefaultContextPropertyPath() {
		return defaultContextPropertyPath;
	}

	public void setDefaultContextPropertyPath(String pp) {
		this.defaultContextPropertyPath = pp;
	}

	public boolean getOneAndOnlyOne() {
		return oneAndOnlyOne;
	}

	public void setOneAndOnlyOne(boolean b) {
		oneAndOnlyOne = b;
	}

	public boolean getRequired() {
		return this.required;
	}

	public void setRequired(boolean b) {
		this.required = b;
	}

	public boolean isImportMatch() {
		return importMatch;
	}

	public boolean getImportMatch() {
		return importMatch;
	}

	public void setImportMatch(boolean b) {
		this.importMatch = b;
	}

	public String getEqualPropertyPath() {
		return equalPropertyPath;
	}

	public void setEqualPropertyPath(String s) {
		equalPropertyPath = s;
	}
}

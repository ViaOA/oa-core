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
package com.viaoa.hub;

import java.lang.reflect.Method;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.runtime.OARuntime;

/**
 * Internally used by Hub that is used to know the owner object of this Hub. The owner is the object that was used to get this Hub. If this
 * Hub was created by using getDetail(), then the MasterHub is set. When creating a shared Hub, this object will also be used for shared
 * Hub.
 * <p>
 * Example: a Hub of Employee Objects can "come" from a Department Object by calling department.getEmployees() method. For this, the
 * masterObject for the employee Hub will be set to the Department Object.
 */
public class HubDataMaster implements java.io.Serializable {
	//qqqqqqqqq calss was package protected
	static final long serialVersionUID = 2L; // used for object serialization

	/**
	 * The master Hub associated with this detail Hub; used only when
	 * the Hub originates from a detail relationship.
	 */
	private transient volatile Hub masterHub;

	/**
	 * The object that owns this Hub, representing the source object
	 * from which the Hub was obtained.
	 */
	private transient volatile OAObject masterObject;

	/**
	 * Returns the master Hub for this detail Hub.
	 *
	 * @return the master Hub, or {@code null} if none is assigned
	 */
	public Hub getMasterHub() {
		return this.masterHub;
	}

	/**
	 * Sets the master Hub for this detail Hub.
	 *
	 * @param h the Hub to assign as master
	 */
	public void setMasterHub(Hub h) {
		this.masterHub = h;
	}

	/**
	 * Sets the master object associated with this Hub.
	 *
	 * @param obj the object that owns this Hub
	 */
	public void setMasterObject(OAObject obj) {
		this.masterObject = obj;
	}

	/**
	 * Returns the master object associated with this Hub.
	 *
	 * @return the master object, or {@code null} if none is assigned
	 */
	public OAObject getMasterObject() {
		return this.masterObject;
	}

	/**
	 * Link information describing the detail-to-master relationship for
	 * this Hub, used to access reverse-link metadata.
	 */
	protected transient volatile OALinkInfo liDetailToMaster;

	/**
	 * Returns the link information describing the detail-to-master relationship.
	 *
	 * @return the {@link OALinkInfo} for the detail-to-master link, or {@code null} if not set
	 */
	public OALinkInfo getDetailToMasterLinkInfo() {
		return liDetailToMaster;
	}

	public void setDetailToMasterLinkInfo(OALinkInfo li) {
		liDetailToMaster = li;;
	}
	
	
	/**
	 * Returns the unique property name associated with the reverse link of the
	 * detail-to-master relationship.
	 *
	 * @return the unique property name, or {@code null} if unavailable
	 */
	public String getUniqueProperty() {
		if (liDetailToMaster == null) {
			return null;
		}
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(liDetailToMaster.getToClass()).objects().getOAObjectInfoService();
		OALinkInfo rli = srvcObjectInfo.getReverseLinkInfo(liDetailToMaster);
		if (rli == null) {
			return null;
		}
		return rli.getUniqueProperty();
	}

	/**
	 * Returns the getter method for the unique property associated with the
	 * reverse link of the detail-to-master relationship.
	 *
	 * @return the getter {@link Method}, or {@code null} if unavailable
	 */
	public Method getUniquePropertyGetMethod() {
		if (liDetailToMaster == null) {
			return null;
		}
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(liDetailToMaster.getToClass()).objects().getOAObjectInfoService();
		OALinkInfo rli = srvcObjectInfo.getReverseLinkInfo(liDetailToMaster);
		if (rli == null) {
			return null;
		}
		return rli.getUniquePropertyGetMethod();
	}

	/**
	 * Determines whether change tracking is enabled for this Hub.
	 *
	 * <p>Tracking is enabled only when a master object exists and the
	 * detail-to-master link is non-calculated, and when the target object
	 * is associated with a data source.</p>
	 *
	 * @return {@code true} if this Hub should track changes, otherwise {@code false}
	 */
	public boolean getTrackChanges() {
		if (masterObject == null) {
			return false;
		}

		// 20160505 change to false.  ex: ServerRoot.hubUsers (calc/merged)
		if (liDetailToMaster == null) {
			return false;
		}
		//was:  if (liDetailToMaster == null) return true;

		if (liDetailToMaster.getCalculated()) {
			return false;
		}

		// 20160623 so that serverRoot wont store changes to objects
		if (!liDetailToMaster.getToObjectInfo().getUseDataSource() && OADataSource.getDataSource(liDetailToMaster.getToClass()) == null) {
			return false;
		}

		// 20160505 check to see if rev li is calc.
		OALinkInfo liRev = liDetailToMaster.getReverseLinkInfo();
		if (liRev != null && liRev.getCalculated()) {
			return false;
		}

		return true;
	}

	/**
	 * Returns the sort property associated with the reverse link of the
	 * detail-to-master relationship.
	 *
	 * @return the sort property name, or {@code null} if unavailable
	 */
	public String getSortProperty() {
		if (liDetailToMaster == null) {
			return null;
		}
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(liDetailToMaster.getToClass()).objects().getOAObjectInfoService();
		OALinkInfo rli = srvcObjectInfo.getReverseLinkInfo(liDetailToMaster);
		if (rli == null) {
			return null;
		}
		return rli.getSortProperty();
	}

	/**
	 * Indicates whether the sort direction for this Hub is ascending.
	 *
	 * @return {@code true} if ascending, otherwise {@code false}
	 */
	public boolean isSortAsc() {
		if (liDetailToMaster == null) {
			return false;
		}
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(liDetailToMaster.getToClass()).objects().getOAObjectInfoService();
		OALinkInfo rli = srvcObjectInfo.getReverseLinkInfo(liDetailToMaster);
		if (rli == null) {
			return false;
		}
		return rli.isSortAsc();
	}

	/**
	 * Returns the sequential property name associated with the reverse link of
	 * the detail-to-master relationship.
	 *
	 * @return the sequential property name, or {@code null} if unavailable
	 */
	public String getSeqProperty() {
		if (liDetailToMaster == null) {
			return null;
		}
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(liDetailToMaster.getToClass()).objects().getOAObjectInfoService();
		OALinkInfo rli = srvcObjectInfo.getReverseLinkInfo(liDetailToMaster);
		if (rli == null) {
			return null;
		}
		return rli.getSeqProperty();
	}

	/**
	 * Custom serialization method that writes default fields and suppresses
	 * serialization of link information and master object references.
	 *
	 * @param s the output stream used for serialization
	 * @throws java.io.IOException if an I/O error occurs
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
		s.defaultWriteObject();
		s.writeByte(0);
	}

	
	/**
	 * Custom deserialization method that restores default fields and reads
	 * a placeholder byte written during serialization.
	 *
	 * @param s the input stream used for deserialization
	 * @throws java.io.IOException if an I/O error occurs
	 * @throws ClassNotFoundException if a referenced class cannot be found
	 */
	private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		byte bx = s.readByte();
	}

}

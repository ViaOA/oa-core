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
package com.viaoa.datasource.autonumber;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;

/**
 * Stores and manages sequential counters used for assigning autonumber-style
 * property values to {@link OAObject} instances. Each {@code NextNumber}
 * record corresponds to a specific class/property pair and maintains the
 * next integer to be assigned.
 * <p>
 * This class is annotated as {@code localOnly=true} and {@code useDataSource=false},
 * meaning it is never persisted through an {@link com.viaoa.datasource.OADataSource}
 * and exists only within the local runtime for autonumber management.
 */
@OAClass(localOnly = true, useDataSource = false, initialize = false)
public class NextNumber extends OAObject {
	static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(NextNumber.class.getName());

	public static final String P_Id = "Id";
	
	
	protected String id; // class name
	protected int nextNum = 1;
	protected String propertyName;

	private static int cnter;

	/**
	 * Creates a local sequence holder and increments the diagnostic instance
	 * counter used by logging.
	 */
	public NextNumber() {
		cnter++;
	}

	/**
	 * Returns the identifier for this sequence, normally the fully qualified class
	 * name that owns the autonumber property.
	 *
	 * @return sequence identifier
	 */
	@OAProperty(isUnique = true)
	@OAId()
	public String getId() {
		return id;
	}

	/**
	 * Sets the identifier for this sequence and fires the OA property-change event.
	 *
	 * @param id sequence identifier, normally a fully qualified class name
	 */
	public void setId(String id) {
		String old = this.id;
		this.id = id;
		firePropertyChange("Id", old, this.id);
		LOG.finer("NextNumber, id=" + id);
		if (cnter >= 200 && cnter % 100 == 0) {
			LOG.warning("NOTE: NextNumber over cnter=" + cnter + ", class/id=" + id);
		}
	}

	/**
	 * Returns the next number that should be assigned by this sequence.
	 *
	 * @return next sequence value
	 */
	public int getNext() {
		return nextNum;
	}

	/**
	 * Sets the next number that should be assigned by this sequence.
	 *
	 * @param nextNum next sequence value
	 */
	public void setNext(int nextNum) {
		int old = this.nextNum;
		this.nextNum = nextNum;
		firePropertyChange("next", old, this.nextNum);
	}

	/**
	 * Sets the model property name that receives values from this sequence.
	 *
	 * @param prop property name
	 */
	public void setProperty(String prop) {
		String old = this.propertyName;
		this.propertyName = prop;
		firePropertyChange("property", old, this.propertyName);
	}

	/**
	 * Returns the model property name that receives values from this sequence.
	 *
	 * @return property name
	 */
	public String getProperty() {
		return propertyName;
	}

	/*========================= Object Info ============================
	public static OAObjectInfo getOAObjectInfo() {
	    return oaObjectInfo;
	}
	protected static OAObjectInfo oaObjectInfo;
	static {
	    oaObjectInfo = new OAObjectInfo(new String[] {"Id"});
	    oaObjectInfo.setLocalOnly(true);
	    oaObjectInfo.setUseDataSource(false);
	    oaObjectInfo.setInitializeNewObjects(false);
	}
	*/
}

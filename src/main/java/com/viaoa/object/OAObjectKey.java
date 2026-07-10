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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight identity holder for {@link OAObject} instances.
 * <p>
 * An {@code OAObjectKey} combines an optional runtime GUID with optional
 * persistent or business key values. The GUID represents runtime identity when
 * available. Object ID values represent datasource identity and can be used for
 * unloaded references, foreign-key references, cache lookup, and distributed
 * messages.
 * <p>
 * When both keys are present, GUID equality takes precedence. Otherwise, the
 * object ID values are compared in order.
 *
 * @see OAObject
 */
public class OAObjectKey implements Serializable, Comparable<Object> {
	static final long serialVersionUID = 1L;

	/**
	 * Array of ID components that represent the business or persistent
	 * identity values for the object. Elements may include primitive
	 * types, wrapper types, or nested OAObjectKey instances.
	 * Assigned at construction time and never modified.
	 */
	private final Object[] objectIds; 
	
	/**
	 * Globally unique identifier assigned to this key. Represents the
	 * runtime identity of the underlying OAObject and takes precedence
	 * over business ID values for equality and ordering.
	 */
	private final UUID guid; 

	/**
	 * Creates a new {@link OAObjectKey} using the provided ID values and GUID.
	 * <p>
	 * The ID values are normalized so that any {@link OAObject} instances are
	 * converted to their corresponding {@link OAObjectKey} values.
	 *
	 * @param ids  the ID values to include in the key (may contain {@link OAObject})
	 * @param guid the GUID assigned to this key
	 */
    public OAObjectKey(Object[] ids, UUID guid) {
        this.guid = guid;
        this.objectIds = normalize(ids);
    }

    /**
     * Creates a new {@link OAObjectKey} using the given ID values and a GUID of {@code 0}.
     *
     * @param ids the ID values to include in the key
     */
    public OAObjectKey(Object[] ids) {
        this(ids, null);
    }

    /**
     * Creates a new {@link OAObjectKey} containing a single integer ID value.
     * The GUID is initialized to {@code 0}.
     *
     * @param id the integer ID value
     */
    public OAObjectKey(int id) {
    	this( new Object[] {id}, null);
    }

    /**
     * Creates a new {@link OAObjectKey} containing a single long ID value.
     * The GUID is initialized to {@code 0}.
     *
     * @param id the long ID value
     */
    public OAObjectKey(long id) {
    	this( new Object[] {id}, null);
    }
    
    /**
     * Creates a new {@link OAObjectKey} containing a single ID value.
     * The GUID is initialized to {@code 0}.
     *
     * @param id the ID value to include in the key
     */
    public OAObjectKey(Object id) {
    	this( new Object[] {id}, null);
    }
    
    
    /**
     * Normalizes the provided ID values by converting any {@link OAObject}
     * instances into their corresponding {@link OAObjectKey}. All other
     * values are copied as-is.
     *
     * @param ids the raw ID values
     * @return a normalized array of ID components, or {@code null} if the input is {@code null}
     */
    private Object[] normalize(Object[] ids) {
        if (ids == null) return null;
        Object[] result = new Object[ids.length];
        for (int i = 0; i < ids.length; i++) {
            Object val = ids[i];
            if (val instanceof OAObject) {
                result[i] = ((OAObject) val).getObjectKey();
            } else {
                result[i] = val;
            }
        }
        return result;
    }

    /**
     * Returns the array of ID values associated with this key.
     *
     * @return the ID value array, or {@code null} if none were provided
     */
	public Object[] getObjectIds() {
	    return objectIds == null ? null : objectIds.clone();
	}
	
	
	/**
	 * Returns the GUID assigned to this key.
	 *
	 * @return the GUID value
	 */
	public UUID getGuid() {
		return guid;
	}
	
	/**
	 * Determines whether this key contains a non-empty set of ID values,
	 * and that none of the values are {@code null}.
	 *
	 * @return {@code true} if all ID components are present and non-null;
	 *         otherwise {@code false}
	 */
	public boolean hasValidObjectIds() {
		if (this.objectIds == null || this.objectIds.length == 0) return false;
		for (int i=0; i<this.objectIds.length; i++) {
			if (this.objectIds[i] == null) return false;
		}
		return true;
	}
	

	/**
	 * Compares this key with another key using GUID identity when available,
	 * otherwise using the normalized object ID values.
	 *
	 * @param obj the object to compare
	 * @return {@code true} if both keys represent the same identity
	 */
	@Override
	public boolean equals(final Object obj) {
	    if (obj == this) return true;
	    if (!(obj instanceof OAObjectKey)) return false;
	    OAObjectKey other = (OAObjectKey) obj;
	    
	    if (this.guid != null || other.guid != null) {
	    	return Objects.equals(this.guid, other.guid);
	    }

	    if (this.objectIds == other.objectIds) return true;
	    if (this.objectIds == null || other.objectIds == null) return false;
	    
        return Arrays.equals(this.objectIds, other.objectIds);	    
	}	
	
	/**
	 * Computes a hash code for this key using the GUID when available, otherwise using object ID values.
	 *
	 * @return the hash code for this key
	 */
	@Override
	public int hashCode() {
		if (guid != null) return guid.hashCode();
		return Arrays.hashCode(objectIds);
	}
	
	/**
	 * Compares this key to another for ordering.
	 * <p>
	 * Comparison rules:
	 * <ul>
	 *   <li>If both keys have non-zero GUIDs, they are compared by GUID.</li>
	 *   <li>If ID arrays exist, each ID element is compared in sequence.</li>
	 *   <li>If ID elements implement {@link Comparable}, they are compared directly;
	 *       otherwise comparison falls back to class name or string value.</li>
	 *   <li>If all comparable elements match, the GUID values are compared last.</li>
	 * </ul>
	 *
	 * @param obj the object to compare with this key
	 * @return a negative, zero, or positive value based on ordering
	 */
	@Override
	public int compareTo(Object obj) {
	    if (obj == this) return 0;
	    if (obj == null) return 1;
	    
	    if (!(obj instanceof OAObjectKey)) return 1;

	    OAObjectKey other = (OAObjectKey) obj;
	    if (this.guid != null || other.guid != null) {
	    	if (this.guid == null) return -1;
	    	if (other.guid == null) return 1;
	    	return this.guid.compareTo(other.guid);
	    }
	    
        if (this.objectIds == null && other.objectIds == null) {
	    	return 0;
        }
        if (this.objectIds == null) {
        	return -1;
        }
        if (other.objectIds == null) {
        	return 1;
        }

    	final int x = Math.min(this.objectIds.length, other.objectIds.length);
    	for (int i=0; i<x; i++) {
    		if (this.objectIds[i] == other.objectIds[i]) continue;
    		if (this.objectIds[i] == null) return -1;
    		if (other.objectIds[i] == null) return 1;
    		
    		if (this.objectIds[i] instanceof Comparable) {
    			if (this.objectIds[i].getClass() == other.objectIds[i].getClass()) {
    			    int z = ((Comparable)this.objectIds[i]).compareTo(other.objectIds[i]);
    			    if (z != 0) return z;
    			} else {
    			    int z = this.objectIds[i].getClass().getName().compareTo(other.objectIds[i].getClass().getName());
    			    if (z != 0) return z;
    			}
    		}
    		else {
    			int z = this.objectIds[i].toString().compareTo(other.objectIds[i].toString());
    			if (z != 0) return z;
    		}
    	}
    	if (this.objectIds.length > x) return 1;
    	if (other.objectIds.length > x) return -1;
    	return 0;
	}	
	
	/**
	 * Returns a string representation of this key, including the GUID and ID values.
	 *
	 * @return a string describing the key contents
	 */
	@Override
	public String toString() {
		return "guid=" + guid + ", ids=" + Arrays.toString(objectIds);		
	}

}

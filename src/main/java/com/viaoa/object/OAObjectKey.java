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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Used to represent the unique guid and ID property value(s) of an OAObject. <br>
 * <p>
 * Note: to be "lighter" the guid can exist (!= 0) and the objectIds can be null, or viceversa.  
 * 
 *<p>   
 * Use {@link OAObjectKeyDelegate#isForSameOAObject(Class, OAObjectKey, OAObjectKey)} to determine if the OAObjectKey represents (and is equal) to another value.
 * example: an Employee with ID=8998, guid=123 ...  ok1 guid = 123,  ok2 objectIds[] = {8998} ... ok1 and ok2 will be equals if using Delegate.
 * <p>  
 * 
 * For more information about this package, see <a href="package-summary.html#package_description">documentation</a>.
 */
public class OAObjectKey implements Serializable, Comparable<Object> {
	static final long serialVersionUID = 1L;

	private final Object[] objectIds; 
	private final long guid; 


    public OAObjectKey(Object[] ids, long guid) {
        this.guid = guid;
        this.objectIds = normalize(ids);
    }

    public OAObjectKey(Object[] ids) {
        this(ids, 0);
    }

    public OAObjectKey(int id) {
    	this( new Object[] {id}, 0L);
    }
    public OAObjectKey(long id) {
    	this( new Object[] {id}, 0L);
    }
    public OAObjectKey(Object id) {
    	this( new Object[] {id}, 0L);
    }
    
    
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


	public Object[] getObjectIds() {
		return this.objectIds;
	}
	
	public long getGuid() {
		return guid;
	}
	
	public boolean hasValidObjectIds() {
		if (this.objectIds == null || this.objectIds.length == 0) return false;
		for (int i=0; i<this.objectIds.length; i++) {
			if (this.objectIds[i] == null) return false;
		}
		return true;
	}
	

	/**
	 * Checks to see if obj is an OAObjectKey, has matching guid and objectIds.
	 * @see OAObjectDelegate.isForSameOAObject(..) Note: call this to check if keys represent the same OAObject.
	 */
	@Override
	public boolean equals(final Object obj) {
	    if (obj == this) return true;
	    if (!(obj instanceof OAObjectKey)) return false;
	    OAObjectKey other = (OAObjectKey) obj;
	    
	    if (this.guid != other.guid) return false; 
	    
	    if (this.objectIds == other.objectIds) return true;
	    if (this.objectIds == null || other.objectIds == null) return false;
	    
        return Arrays.equals(this.objectIds, other.objectIds);	    
	}	
	
	@Override
	public int hashCode() {
	    int hash = Long.hashCode(guid);
	    hash = 31 * hash + Arrays.hashCode(objectIds);
	    return hash;
	}
	
	@Override
	public int compareTo(Object obj) {
	    if (obj == this) return 0;
	    if (obj == null) return 1;
	    
	    if (!(obj instanceof OAObjectKey)) return 1;

	    OAObjectKey other = (OAObjectKey) obj;
	    
	    if (this.guid != 0 && other.guid != 0) {
	    	return Long.compare(this.guid, other.guid);
	    }
	    
        if (this.objectIds == null && other.objectIds == null) {
	    	return Long.compare(this.guid, other.guid);
        }
        if (this.objectIds == null || other.objectIds == null) {
	    	return Long.compare(this.guid, other.guid);
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
    	return Long.compare(this.guid, other.guid);
	}	
	
	@Override
	public String toString() {
		return "guid=" + guid + ", ids=" + Arrays.toString(objectIds);		
	}
}

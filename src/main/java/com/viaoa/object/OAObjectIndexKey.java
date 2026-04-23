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

import java.util.Arrays;

/**
 * Immutable representation of a primary/business key used for indexing in
 * {@link OAObjectIndex}. Wraps an array of identifier values and supplies
 * stable equals and hashCode semantics based on the identifier content.
 *
 * <p>All identifier values must be non-null to be considered valid for
 * indexing. Instances are safe for use in concurrent maps.</p>
 *
 * @see OAObjectIndex
 */
public final class OAObjectIndexKey {
	/**
	 * Immutable array of identifier values that define this index key.
	 * Created as a defensive clone during construction to ensure that
	 * external modifications cannot affect equality or hash semantics.
	 */
    private final Object[] ids;

    /**
     * Precomputed hash code derived from the identifier array.
     * Enables efficient, stable use of this key in concurrent maps
     * without recalculating the hash on each lookup.
     */
    private final int hash;

    /**
     * Constructs a new immutable index key by cloning the supplied
     * identifier array. Precomputes the hash code for consistent and
     * efficient use as a map key.
     *
     * @param ids the identifier values to wrap.
     */
    public OAObjectIndexKey(Object[] ids) {
        this.ids = (ids == null) ? new Object[0] : ids.clone();
        this.hash = ids == null ? 0 : Arrays.hashCode(this.ids);
    }

    /**
     * Determines whether this key contains at least one identifier
     * and that all identifiers are non-null. Only keys meeting these
     * requirements are eligible for indexing.
     *
     * @return true if all identifier values are non-null.
     */
	public boolean hasValidIds() {
		if (this.ids.length == 0) return false;
		for (int i=0; i<this.ids.length; i++) {
			if (this.ids[i] == null) return false;
		}
		return true;
	}
    
	/**
	 * Returns the underlying array of identifier values. The array
	 * is the internal clone created at construction time.
	 *
	 * @return the identifier array.
	 */
	public Object[] getIds() {
		return this.ids;
	}
	
	/**
	 * Compares this key to another object for equality. Returns
	 * true only when the other object is an OAObjectIndexKey
	 * with an identifier array equal to this key's array.
	 *
	 * @param obj the object to compare.
	 * @return true if the keys match.
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OAObjectIndexKey)) return false;
        return Arrays.equals(ids, ((OAObjectIndexKey) obj).ids);
    }

    /**
     * Returns the precomputed hash value for this key, based solely
     * on the identifier array content.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Produces a string representation of the identifier array in
     * standard {@code Arrays.toString(...)} format.
     *
     * @return the string form of this key.
     */
    @Override
    public String toString() {
        return Arrays.toString(ids);
    }
}
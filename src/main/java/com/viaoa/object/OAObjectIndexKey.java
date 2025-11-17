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
    private final Object[] ids;
    private final int hash;

    public OAObjectIndexKey(Object[] ids) {
        this.ids = (ids == null) ? new Object[0] : ids.clone();
        this.hash = ids == null ? 0 : Arrays.hashCode(this.ids);
    }

	public boolean hasValidIds() {
		if (this.ids == null || this.ids.length == 0) return false;
		for (int i=0; i<this.ids.length; i++) {
			if (this.ids[i] == null) return false;
		}
		return true;
	}
    
	public Object[] getIds() {
		return this.ids;
	}
	
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OAObjectIndexKey)) return false;
        return Arrays.equals(ids, ((OAObjectIndexKey) obj).ids);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return Arrays.toString(ids);
    }
}
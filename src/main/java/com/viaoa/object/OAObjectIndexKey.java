package com.viaoa.object;

import java.util.Arrays;

/**
 * Used by OAObjectIndex to store OAObject ID property values.
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
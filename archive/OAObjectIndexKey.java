package com.viaoa.object;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

public class OAObjectIndexKey {
	static final long serialVersionUID = 1L;

/*qqqqqq New, wont be used
 *  	
	private final Object[] ids;

	public OAObjectIndexKey(Object... ids) {
		this.ids = ids;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(ids);
	}

	@Override
	public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
		
        // Case 1: same type
		if (obj instanceof OAObjectIndexKey) {
			OAObjectIndexKey ok = (OAObjectIndexKey) obj;
			return Arrays.equals(this.ids, ok.ids);
		}
		
		// Case 2: object array
        if (obj instanceof Object[]) {
            return Arrays.equals(this.ids, (Object[]) obj);
        }
		
        // Case 3: primitive array
        if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);
            if (len != ids.length) return false;
            for (int i = 0; i < len; i++) {
                Object val = Array.get(obj, i);
                if (!Objects.equals(ids[i], val)) return false;
            }
            return true;
        }		
        
		return false;
	}
	
	public Object[] getIds() {
		return ids;
	}
	
	public boolean isValid() {
		if (ids == null || ids.length == 0) return false;
		for (int i=0; i<ids.length; i++) {
			if (ids[i] == null) return false;
		}
		return true;
		// or: return ids != null && ids.length > 0 && Arrays.stream(ids).allMatch(Objects::nonNull);
	}
*/
}


package com.viaoa.object;

import com.viaoa.util.OAConv;

public class OAObjectIndexKeyDelegate {

	/**
	 * Form an index key to be used for OAObjectIndexDelegate.
	 * <br>
	 * Note: if any of the values are null, then OAObjectIndexKey.isValid() will be false,
	 * and it will not be added to the index (until all pkey props are non-null).
	 * <br>
	 * @return value(s) for an OAObject's index property value(s).
	 */
	
	
/* qqqqqqqqqqqqq new, wont be used qqqqqq	
	public static OAObjectIndexKey createObjectIndexKey(OAObject obj) {
		if (obj == null) return null;
		
		Object[] ids = OAObjectInfoDelegate.getPropertyIdValues(obj);
		if (ids == null || ids.length == 0) return null;
		
		OAObjectIndexKey ok = new OAObjectIndexKey(ids);
		
		return ok;
	}

	public static OAObjectIndexKey createObjectIndexKey(Class<? extends OAObject> clazz, Object ... values) {
		if (clazz == null) return null;

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		OAObjectIndexKey ok = createObjectIndexKey(oi, values );
		return ok;
	}

	
	public static OAObjectIndexKey createObjectIndexKey(OAObjectInfo oi, Object ... values) {
		if (oi == null || values == null || values.length == 0) {
			return null;
		}
		
		String[] ids = oi.idProperties;
		if (ids == null || ids.length != values.length) return null;
		Object[] objsNew = new Object[ids.length];
		
		for (int i=0; i<ids.length; i++) {
			Class c = OAObjectInfoDelegate.getPropertyClass(oi, ids[i]);
			objsNew[i] = OAConv.convert(c, values[i], null);
		}
		
		OAObjectIndexKey ok = new OAObjectIndexKey(objsNew);
		return ok;
	}
*/
}

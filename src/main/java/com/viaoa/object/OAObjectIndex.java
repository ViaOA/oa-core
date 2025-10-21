package com.viaoa.object;

import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.util.OAArray;

/**
 * Indexes OAObjects by pkey Property values to it's OAObject.guid value.
 * <br>
 * Return value is (long) guid, which is used to lookup OAObject using OAObjectCache.
 * <br>
 * This is managed by OAObjectCache, including updates when an indexed property is changed.
 * <br>
 * Note: only OAObjectKey with non-null objectIds[] is added.
 *  
 * Also called by OAObjectCache weakref queue when an OAObject weakref is GC'd 
 * 
 * @see OAObjectInfoDelegate.getPropertyIdValues(..) and OAObjectKey.
 * 
 */
public class OAObjectIndex {

	private final ConcurrentHashMap<Class<? extends OAObject>, ConcurrentHashMap<OAObjectKey, Long>> hmOAObjectById = new ConcurrentHashMap<>(151, 0.75F);

	public long lookupGuid(Class<? extends OAObject> c, Object... ids) {
		if (c == null) return 0L;
		if (ids == null || ids.length == 0) return 0L;
		if (OAArray.hasNull(ids)) return 0L;
		OAObjectKey ok = new OAObjectKey(ids);
		return lookupGuid(c, ok);
	}

	/**
	 * @return 0 if not found.  
	 */
	public long lookupGuid(final Class<? extends OAObject> c, final OAObjectKey ok) {
		if (c == null) return 0L;
		if (ok == null) return 0L;
		if (!ok.hasValidObjectIds()) return 0L;
		ConcurrentHashMap<OAObjectKey, Long> hm = hmOAObjectById.get(c);
		if (hm == null) {
			return 0L;
		}
		
		Long guid = hm.get(ok);
		if (guid == null) return 0L;
		return guid;
	}
	

	public boolean addToIndex(final OAObject obj) {
		if (obj == null) return false;
		
		OAObjectKey ok = OAObjectKeyDelegate.getKey(obj);
		return addToIndex(obj, ok);
	}
	
	public boolean addToIndex(final OAObject obj, OAObjectKey ok) {
		if (obj == null) return false;
		if (ok == null) return false;
		if (!ok.hasValidObjectIds()) return false;

		final Class<? extends OAObject> clazz = obj.getClass();
		ConcurrentHashMap<OAObjectKey, Long> hm = hmOAObjectById.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());

		long guid = obj.getGuid();
		hm.put(ok, guid);
		return true;
	}
	
	public boolean removeFromIndex(OAObject obj) {
		if (obj == null) return false;
		Class<? extends OAObject> c = obj.getClass();
		ConcurrentHashMap<OAObjectKey, Long> hm = hmOAObjectById.get(c);
		if (hm == null) return false;
		
		OAObjectKey ok = new OAObjectKey(obj);
		return (ok.hasValidObjectIds() && hm.remove(ok) != null);
	}

	public boolean removeFromIndex(Class<? extends OAObject> clazz, OAObjectKey ok) {
		if (clazz == null || ok == null || !ok.hasValidObjectIds()) return false;
		ConcurrentHashMap<OAObjectKey, Long> hm = hmOAObjectById.get(clazz);
		if (hm == null) return false;
		return (hm.remove(ok) != null);
	}
	
	public void updateIndex(final OAObject obj, OAObjectKey okNew, OAObjectKey okOld) {
		if (obj == null) return;

		if (okNew != null && okNew.equals(okOld)) return;
		
		if (okNew != null && okNew.hasValidObjectIds()) {
			addToIndex(obj, okNew);
		}
		if (okOld != null && okOld.hasValidObjectIds()) {
			removeFromIndex(obj.getClass(), okOld);
		}
	}
	
}

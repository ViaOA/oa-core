package com.viaoa.object;

import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.util.OAArray;

/**
 * Used by OAObjectCache, that Indexes OAObjects by pkey Property values (using OAObjectIndexKey) to it's OAObject.guid value.
 * <br>
 * This is managed by OAObjectCache, including updates when an indexed property is changed.
 * <br>
 * Note: only OAObjectKey that have all non-null objectIds[] are added.
 *  
 * Also called by OAObjectCache weakref queue when an OAObject weakref is GC'd, removing from this index. 
 * 
 * @see OAObjectInfoDelegate.getPropertyIdValues(..) and OAObjectKey.
 */
public class OAObjectIndex {
	private final ConcurrentHashMap<Class<? extends OAObject>, ConcurrentHashMap<OAObjectIndexKey, Long>> hmGuidByIndexKey = new ConcurrentHashMap<>(151, 0.75F);

//qqqqq change to add(..)	
	public boolean addToIndex(final OAObject obj) {
		if (obj == null) return false;
		OAObjectKey ok = OAObjectKeyDelegate.getKey(obj);
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		Class<? extends OAObject> c = obj.getClass();
		return addToIndex(c, ik, obj.getGuid());
	}

	protected boolean addToIndex(final OAObject obj, OAObjectKey ok) {
		if (obj == null) return false;
		if (ok == null) return false;
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return addToIndex(obj.getClass(), ik, obj.getGuid());
	}
	
	protected boolean addToIndex(final Class<? extends OAObject> c, OAObjectIndexKey ik, long guid) {
		if (c == null || ik == null || guid == 0 || !ik.hasValidIds()) return false;
		ConcurrentHashMap<OAObjectIndexKey, Long> hm = hmGuidByIndexKey.computeIfAbsent(c, k -> new ConcurrentHashMap<>());
		hm.put(ik, guid);
		return true;
	}
	
	
//qqqqqqq change to getGuid(..)	
	public long lookupGuid(Class<? extends OAObject> c, Object[] ids) {
		if (c == null) return 0L;
		OAObjectIndexKey ik = new OAObjectIndexKey(ids);
		return lookupGuid(c, ik);
	}

	public long lookupGuid(final Class<? extends OAObject> c, final OAObjectKey ok) {
		if (c == null) return 0L;
		if (ok == null) return 0L;
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return lookupGuid(c, ik);
	}

	protected long lookupGuid(final Class<? extends OAObject> c, final OAObjectIndexKey ik) {
		if (c == null || ik == null || !ik.hasValidIds()) return 0L;
		ConcurrentHashMap<OAObjectIndexKey, Long> hm = hmGuidByIndexKey.get(c);
		if (hm == null) return 0;
		return hm.get(ik);
	}
	
	
	
	public boolean removeFromIndex(OAObject obj) {
		if (obj == null) return false;
		Class<? extends OAObject> c = obj.getClass();
		OAObjectKey ok = OAObjectKeyDelegate.createObjectKey(obj);
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return removeFromIndex(c, ik);
	}
	
	public boolean removeFromIndex(Class<? extends OAObject> c, OAObjectKey ok) {
		if (c == null || ok == null) return false;
		OAObjectIndexKey ik = new OAObjectIndexKey(ok.getObjectIds());
		return removeFromIndex(c, ik);
	}

	protected boolean removeFromIndex(final Class<? extends OAObject> c, OAObjectIndexKey ik) {
		if (c == null || ik == null || !ik.hasValidIds()) return false;
		ConcurrentHashMap<OAObjectIndexKey, Long> hm = hmGuidByIndexKey.computeIfAbsent(c, k -> new ConcurrentHashMap<>());
		if (hm == null) return false;
		return (hm.remove(ik) != null);
	}
	
	
	
	public void updateIndex(final OAObject obj, OAObjectKey okNew, OAObjectKey okOld) {
		if (obj == null) return;

		Class c = obj.getClass();
		if (okNew != null && okNew.equals(okOld)) return;
		
		if (okNew != null) {
			addToIndex(obj, okNew);
		}
		if (okOld != null) {
			removeFromIndex(obj.getClass(), okOld);
		}
	}
	
}

package com.viaoa.object;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.model.oa.VBoolean;

/**
 * Caches OAObjects by guid, and uses the OAObjectIndex to index using OAObject pkey/ID propery values.
 */
public class OAObjectCache {
	private static Logger LOG = Logger.getLogger(OAObjectCache.class.getName());

	private final ConcurrentHashMap<
	    Class<? extends OAObject>,
	    ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>>> 
	    hmOAObjectByGuid = new ConcurrentHashMap<>(151, 0.75F);	
	
	private final ReferenceQueue<OAObject> refQueue = new ReferenceQueue<>();
	private final OAObjectIndex objectIndex = new OAObjectIndex();

	private int cntGetObject;
	private int cntGCd;
	
	public Class<?>[] getClasses() {
		return hmOAObjectByGuid.keySet().toArray(new Class[0]);
	}
	public int getTotal(Class<? extends OAObject> clazz) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return 0;
		return hm.size();
	}
	public void clearCache(Class<? extends OAObject> clazz) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return;
		hm.clear();
	}
	

	@SuppressWarnings("unchecked")
	public <T extends OAObject> T getObject(Class<T> c, long guid) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(c);
		if (hm == null) return null;
		OAWeakRef<? extends OAObject> wr = hm.get(guid);
		if ((++cntGetObject % 100) == 0) checkReferenceQueue();
		if (wr == null) return null;
		return (T) wr.get();
	}
	
	
	public <T extends OAObject> T getObject(Class<T> clazz, Object[] ids) {
		if (clazz == null || ids == null) return null;
		OAObjectKey ok = new OAObjectKey(ids);
		return getObject(clazz, ok);
	}

	public <T extends OAObject> T getObject(Class<T> clazz, OAObjectKey ok) {
		if (clazz == null || ok == null) return null;
		long guid = ok.getGuid();
		if (guid == 0) {
			guid = objectIndex.lookupGuid(clazz, ok);
		}
		if (guid == 0) return null;
		return getObject(clazz, guid);
	}
	
	
	
	/**
	 * Called when loading an Object, or when a OAObject pkey Property is changed.
	 * 
	 * @return true if object already existed in cache.
	 */
	public boolean updateObject(final OAObject obj) {
		if (obj == null) return false;
		final Class<? extends OAObject> clazz = obj.getClass();
		final ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
		
		final OAObjectKey ok = OAObjectKeyDelegate.createObjectKey((OAObject) obj);
		
		boolean[] bsWasFound = new boolean[] {true};

		OAWeakRef<? extends OAObject> wrOld = hm.computeIfAbsent(ok.getGuid(), k -> {
			bsWasFound[0] = false;
			return new OAWeakRef(obj, ok, refQueue);
		});
		
		if (bsWasFound[0]) {
			OAObjectKey okOld = wrOld.key;
			wrOld.key = ok;
			objectIndex.updateIndex(obj, ok, okOld);
		}
		else {
			objectIndex.addToIndex(obj, ok);
		}
		checkReferenceQueue();
		return bsWasFound[0];
	}



	protected void checkReferenceQueue() {
		for (int i=0; i<5000; i++) {
			@SuppressWarnings("unchecked")
			OAWeakRef<? extends OAObject> wr = (OAWeakRef<? extends OAObject>) refQueue.poll();
			if (wr == null) break;
			++cntGCd;
			ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(wr.clazz);
			if (hm != null) hm.remove(wr.key.getGuid());
			objectIndex.removeFromIndex(wr.clazz, wr.key);
		}
	}
	
	
	public void visit(OACallback callback) {
		for (Class<? extends OAObject> c : hmOAObjectByGuid.keySet()) {
			visit(c, callback);
		}
	}

	public void visit(Class<? extends OAObject> clazz, OACallback callback) {
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) return;
		for (OAWeakRef<? extends OAObject> wr : hm.values()) {
			OAObject obj = wr.get();
			if (obj != null) callback.updateObject(obj);
		}
	}
	
	public Object find(final Object fromObject, final Class<? extends OAObject> clazz, final OAFinder finder,
		boolean bSkipNew, int fetchAmount, final List<OAObject> alResults) 
	{
		ConcurrentHashMap<Long, OAWeakRef<? extends OAObject>> hm = hmOAObjectByGuid.get(clazz);
		if (hm == null) {
			return null;
		}
		boolean bFoundFirst = fromObject == null;
		for (OAWeakRef<? extends OAObject> wr : hm.values()) {
			OAObject obj = wr.get();
			if (obj == null) continue;
			if (!bFoundFirst) {
				if (obj != fromObject) continue;
				bFoundFirst = true;
				continue;
			}
			if (bSkipNew && obj.isNew()) continue;
			
			if (finder == null || finder.findFirst(obj) != null) {
				if (alResults == null) {
					return obj;
				}
				alResults.add(obj);
				if (alResults.size() >= fetchAmount) {
					return obj;
				}
			}
		}
		return null;
	}
}

final class OAWeakRef<T extends OAObject> extends WeakReference<T> {
    final Class<? extends OAObject> clazz;
    OAObjectKey key; 

    OAWeakRef(T obj, OAObjectKey key, ReferenceQueue<? super T> queue) {
        super(obj, queue);
        this.clazz = obj.getClass();
        this.key = key;
    }
}



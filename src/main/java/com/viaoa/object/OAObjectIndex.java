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

import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.util.OAArray;

/**
 * Maintains a runtime index of OAObject instances by their primary/business key
 * property values, enabling fast lookup of an object GUID when only its
 * identifier fields are known. This supports identity reconciliation during
 * lazy loading and distributed synchronization.
 *
 * <p>The index is maintained by {@code OAObjectCache}, including updates when
 * primary key values change. Only keys with all non-null identifier values are
 * indexed to ensure correct and deterministic lookup behavior.</p>
 *
 * <p>Map entries are removed automatically when their referenced OAObject is
 * garbage collected, preserving memory and avoiding stale identity entries.</p>
 *
 * @see OAObjectKey
 * @see OAObjectKeyDelegate#getKey
 * @see OAObjectCache
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
		Long lx = hm.get(ik);
		if (lx == null) return 0L;
		return lx.longValue();
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
	
	public void clear() {
		hmGuidByIndexKey.clear();
	}
	
}

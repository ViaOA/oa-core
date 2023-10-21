package com.viaoa.json.jackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.OASelect;
import com.viaoa.datasource.objectcache.OADataSourceObjectCache;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.json.OAJson.StackItem;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.pojo.PojoDelegate;
import com.viaoa.pojo.PojoLink;
import com.viaoa.pojo.PojoLinkOne;
import com.viaoa.pojo.PojoLinkOneDelegate;
import com.viaoa.pojo.PojoLinkUnique;
import com.viaoa.pojo.PojoProperty;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

// todo: unit test with CorpToStore model ... to check multipart keys

/**
 * Used by OAJson to convert JSON to OAObject(s). <br>
 * This will find the existing OAObjects and Hubs and add/update/delete.
 * <p>
 * This supports object graphs, json from POJOs that might not have matching pkey properties. <br>
 * For POJOs without direct pkey & fkey properties, it uses the following to find and update matching OAObjects
 * <ul>
 * <li>Match using unique pkey property(s)
 * <li>Guid match
 * <li>ImportMatch properties
 * <li>Links that have a unique property match.
 * <li>Links that have an equals property path with a unique property match.
 * </ul>
 */
public class OAJacksonDeserializerLoader {

	private final OAJson oajson;
	private final boolean bUsesPojo;

	private boolean debug = false;
	private int cntLoadCalled;

	// list of references that were not found, that will be retried once at the end of load.
	private final List<RetryPojoReference> alRetryPojoReference = new ArrayList();

	public OAJacksonDeserializerLoader(OAJson oaj) {
		this.oajson = oaj;
		this.bUsesPojo = oaj.getReadingPojo();
	}

	public <T extends OAObject> T load(final JsonNode node, final T root) {
		T t = load(node, root, null);
		return t;
	}

	/**
	 * Main method called by OAJacksonModule to load an OAObject and any references from json tree.
	 */
	public <T extends OAObject> T load(final JsonNode node, final T root, Class<T> clazz) {
		if (node == null) {
			return root;
		}
		if (clazz == null) {
			if (root == null) {
				return null;
			}
			clazz = (Class<T>) root.getClass();
		}
		OAJson.StackItem stackItem = new OAJson.StackItem();
		stackItem.oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		stackItem.obj = root;
		stackItem.node = node;

		final StackItem siHold = oajson.getStackItem();
		oajson.setStackItem(stackItem);

		try {
			load(stackItem);
			retryPojoReferences();
		} finally {
			oajson.setStackItem(siHold);
		}

		return (T) stackItem.obj;
	}

	/**
	 * Main method for loading JsonObject into a (new or existing) OAObject.<br>
	 * Recursively will load all link objects.
	 * <p>
	 * find or create a matching OAObject from an JsonObject.<br>
	 * This will first use the stack, then guid, pkeys, importMatches, linkWithUnique, linkWithEqualAndUnique.
	 */
	protected boolean load(final StackItem stackItem) {
		if (stackItem == null) {
			return false;
		}

		String debug = ++cntLoadCalled + ")";
		debug(stackItem, "BEG " + debug);

		if (stackItem.node == null) {
			return false;
		}

		// 1:
		findExistingObject(stackItem);

		if (stackItem.node.isObject()) {
			if (stackItem.obj == null) {
				// 2a:
				createObject(stackItem);
			} else {
				if (stackItem.li != null && stackItem.li.getAutoCreateNew()) {
					OAObjectKey ok = stackItem.obj.getObjectKey();
					if (ok.isNew()) {
						// 2b:
						loadObjectIdProperties(stackItem);
					}
				}
			}

			// 3:
			loadObject(stackItem);
		}
		debug(stackItem, "END " + debug);
		return true;
	}

	protected void createObject(final StackItem stackItem) {
		final Class clazz = stackItem.oi.getForClass();
		debug2(stackItem, "createObject");

		oajson.beforeReadCallback(stackItem.node);
		stackItem.obj = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);

		boolean bNeedsAssignedId = loadObjectIdProperties(stackItem);

		if (OAThreadLocalDelegate.isLoading()) {
			OAThreadLocalDelegate.setLoading(false);
			try {
				OAObjectDelegate.initializeAfterLoading((OAObject) stackItem.obj, bNeedsAssignedId, false, false);
			} finally {
				OAThreadLocalDelegate.setLoading(true);
			}
		}
	}

	protected boolean loadObjectIdProperties(final StackItem stackItem) {
		boolean bNeedsAssignedId = false;
		if (bUsesPojo) {
			for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
				if (pi.getId() && pi.getAutoAssign()) {
					bNeedsAssignedId = true;
					break;
				}
			}

			for (PojoProperty pp : PojoDelegate.getPojoPropertyKeys(stackItem.oi.getPojo())) {
				String propertyName = pp.getName();
				if (!oajson.getUsePropertyCallback(null, propertyName)) {
					continue;
				}
				propertyName = oajson.getPropertyNameCallback(null, propertyName);

				JsonNode jn = stackItem.node.get(propertyName);

				OAPropertyInfo pi = OAObjectInfoDelegate.getPropertyInfo(stackItem.oi, propertyName);
				if (pi == null) {
					continue;
				}
				if (pi.getAutoAssign()) {
					bNeedsAssignedId = false;
				}

				Object objx = convert(jn, pi);

				objx = oajson.getPropertyValueCallback(null, pi.getLowerName(), objx);

				stackItem.obj.setProperty(pi.getLowerName(), objx);
			}
		} else {
			for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
				if (!pi.getId()) {
					continue;
				}

				String propertyName = pi.getLowerName();
				if (!oajson.getUsePropertyCallback(null, propertyName)) {
					continue;
				}
				
				propertyName = oajson.getPropertyNameCallback(null, stackItem.oi.getJsonUsesCapital() ? pi.getName() : propertyName);

				JsonNode jn = stackItem.node.get(propertyName);

				if (jn == null) {
					bNeedsAssignedId |= pi.getAutoAssign();
					continue;
				}

				Object objx = convert(jn, pi);

				objx = oajson.getPropertyValueCallback(null, pi.getLowerName(), objx);

				stackItem.obj.setProperty(pi.getLowerName(), objx);
			}
		}
		return bNeedsAssignedId;
	}

	protected void loadObject(final StackItem stackItem) {
		if (stackItem.node == null || !stackItem.node.isObject()) {
			throw new RuntimeException("loadObject does not have a node.isObject=true");
		}

		debug2(stackItem, "loadObject " + stackItem.oi.getName());

		loadObjectProperties(stackItem);

		final Set<OALinkInfo> hsLinkInfoOneLoaded = new HashSet();

		if (stackItem.li != null && stackItem.li.isMany2One()) {
			hsLinkInfoOneLoaded.add(stackItem.li.getReverseLinkInfo());
		}

		Iterator<String> itx = stackItem.node.fieldNames();
		for (; itx.hasNext();) {
			String name = itx.next();

			OALinkInfo lix = stackItem.oi.getLinkInfo(name);
			if (lix == null) {
				continue;
			}
			if (lix.isOne()) {
				if (!hsLinkInfoOneLoaded.contains(lix)) {
					if (loadObjectOneLink(stackItem, lix)) {
						hsLinkInfoOneLoaded.add(lix);
					}
				}
			} else {
				loadObjectManyLink(stackItem, lix);
			}
		}

		loadObjectReferences(stackItem, hsLinkInfoOneLoaded);
	}

	protected void loadObjectProperties(final StackItem stackItem) {
		OAObjectKey objKey = stackItem.obj.getObjectKey();

		// debug2(stackItem, "loadObjectProperties");

		if (!bUsesPojo) {
			JsonNode jn = stackItem.node.get("guid");
			if (jn != null) {
				int guid = jn.asInt();
				if (oajson != null) {
					oajson.getGuidMap().put(guid, stackItem.obj);
				}
			}
		}

		// load properties
		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}

			if (!oajson.getUsePropertyCallback(stackItem.obj, pi.getLowerName())) {
				continue;
			}

			String propertyName = stackItem.oi.getJsonUsesCapital() ? pi.getName() : pi.getLowerName();
			
			propertyName = oajson.getPropertyNameCallback(stackItem.obj, propertyName);

			JsonNode jn = stackItem.node.get(propertyName);
			if (jn == null) {
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oajson.getPropertyValueCallback(stackItem.obj, pi.getLowerName(), objx);

			stackItem.obj.setProperty(pi.getLowerName(), objx);
		}
	}

	protected boolean loadObjectOneLink(final StackItem stackItem, final OALinkInfo li) {
		if (li.getType() != li.TYPE_ONE) {
			return false;
		}
		if (li.getPrivateMethod()) {
			return false;
		}

		if (!oajson.getUsePropertyCallback(stackItem.obj, li.getLowerName())) {
			return false;
		}

		// debug2(stackItem, "loadObjectOneLink " + li.getName());

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.oi = li.getToObjectInfo();
		stackItemChild.li = li;
		stackItemChild.node = stackItem.node.get(stackItem.oi.getJsonUsesCapital() ? li.getName() : li.getLowerName());

		try {
			oajson.setStackItem(stackItemChild);
			boolean b = loadObjectOneLink(stackItem, stackItemChild);
			return b;
		} finally {
			oajson.setStackItem(stackItem);
		}
	}

	protected boolean loadObjectOneLink(final StackItem stackItem, final StackItem stackItemChild) {
		debug2(stackItem, "loadObjectOneLink " + stackItemChild.li.getName());

		if (stackItem.node == null) {
			return false;
		}

		boolean b = load(stackItemChild);
		if (b) {
			stackItem.obj.setProperty(stackItemChild.li.getLowerName(), stackItemChild.obj);
		}
		return b;
	}

	protected void loadObjectManyLink(final StackItem stackItem, final OALinkInfo li) {
		// load links of type=many
		debug2(stackItem, "loadObjectManyLink " + li.getName());
		if (li.getType() != li.TYPE_MANY) {
			return;
		}
		if (li.getPrivateMethod()) {
			return;
		}

		if (!oajson.getUsePropertyCallback(stackItem.obj, li.getLowerName())) {
			return;
		}

		String propertyName = oajson.getPropertyNameCallback(stackItem.obj, stackItem.oi.getJsonUsesCapital() ? li.getName() : li.getLowerName());
		JsonNode nodex = stackItem.node.get(propertyName);

		if (!(nodex instanceof ArrayNode)) {
			return;
		}
		Hub<OAObject> hub = (Hub<OAObject>) li.getValue(stackItem.obj);
		ArrayNode nodeArray = (ArrayNode) nodex;

		List<OAObject> alAdded = new ArrayList();
		int x = nodeArray.size();
		for (int i = 0; i < x; i++) {
			nodex = nodeArray.get(i);

			StackItem stackItemChild = new StackItem();
			stackItemChild.parent = stackItem;
			stackItemChild.oi = li.getToObjectInfo();
			stackItemChild.li = li;
			stackItemChild.node = nodex;

			try {
				oajson.setStackItem(stackItemChild);
				load(stackItemChild);
				alAdded.add(stackItemChild.obj);
				if (!hub.contains(stackItemChild.obj)) {
					hub.add(stackItemChild.obj);
				}

			} finally {
				oajson.setStackItem(stackItem);
			}
		}

		List<OAObject> alRemove = new ArrayList();
		for (OAObject objx : hub) {
			if (!alAdded.contains(objx)) {
				alRemove.add(objx);
			}
		}
		for (OAObject objx : alRemove) {
			hub.remove(objx);
		}

		// same order
		int i = 0;
		for (OAObject objx : alAdded) {
			int pos = hub.getPos(objx);
			if (pos != i) {
				hub.move(pos, i);
			}
			i++;
		}

		oajson.afterReadCallback(stackItem.node, stackItem.obj);
	}

	protected void loadObjectReferences(final StackItem stackItem, final Set<OALinkInfo> hsLinkInfoOneLoaded) {
		if (!bUsesPojo) {
			loadObjectNonPojoReferences(stackItem, hsLinkInfoOneLoaded);
		} else {
			loadObjectPojoReferences(stackItem, hsLinkInfoOneLoaded);
		}
	}

	protected void loadObjectNonPojoReferences(final StackItem stackItem, final Set<OALinkInfo> hsLinkInfoOneLoaded) {
		// load linkOne using fkeys, if the linkOne node did not exist
		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (!li.isOne()) {
				continue;
			}
			if (hsLinkInfoOneLoaded.contains(li)) {
				continue;
			}

			boolean bHasNull = false;
			ArrayList<Object> alKey = new ArrayList();
			for (OAFkeyInfo fi : li.getFkeyInfos()) {
				if (fi.getFromPropertyInfo() == null) {
					continue;
				}

				JsonNode jn = null;
				if (stackItem.parent != null) {
					if (stackItem.parent.li.getReverseLinkInfo() == li) {
						String s = stackItem.oi.getJsonUsesCapital() ? fi.getFromPropertyInfo().getName() : fi.getFromPropertyInfo().getLowerName();
						jn = stackItem.parent.node.get(s);
					}
				}
				if (jn == null) {
					String s = li.getToObjectInfo().getJsonUsesCapital() ? fi.getToPropertyInfo().getName() : fi.getToPropertyInfo().getLowerName();
					jn = stackItem.node.get(s);
				}

				if (jn == null) {
					bHasNull = true;
				}
				else {
					Object objx = convert(jn, fi.getToPropertyInfo());
					alKey.add(objx);
					bHasNull |= (objx == null);
				}
			}

			Object[] objs = alKey.toArray(new Object[alKey.size()]);
			OAObjectKey ok = (bHasNull || objs == null || objs.length == 0) ? null : new OAObjectKey(objs);

			OAObject obj;
			if (bHasNull) {
				obj = null;
			} else {
				obj = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
				if (obj == null) {
					obj = (OAObject) OADataSource.getObject(li.getToClass(), ok);
				}
			}
			
			if (obj == null && ok != null) {
				OAObjectPropertyDelegate.setProperty(stackItem.obj, li.getName(), ok);
			} else {
				stackItem.obj.setProperty(li.getName(), obj);
			}
		}
	}

	protected void loadObjectPojoReferences(final StackItem stackItem, final Set<OALinkInfo> hsLinkInfoOneLoaded) {
		// load Pojo key properties (fkey, importMatch, linkUnique+equalsPp)

		for (PojoLink pl : stackItem.oi.getPojo().getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo == null) {
				continue;
			}

			final OALinkInfo li = stackItem.oi.getLinkInfo(pl.getName());
			if (hsLinkInfoOneLoaded.contains(li)) {
				continue;
			}

			if (!loadObjectPojoFkeyReferences(stackItem, plo, li)) {
				if (!loadObjectPojoImportMatchReferences(stackItem, plo, li)) {
					loadObjectPojoUniqueReferences(stackItem, plo, li);
				}
			}
		}
	}

	protected boolean loadObjectPojoFkeyReferences(final StackItem stackItem, final PojoLinkOne plo, final OALinkInfo li) {
		List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkFkeyPojoProperties(plo);
		if (alPojoProperty == null || alPojoProperty.size() == 0) {
			return false;
		}

		final Map<String, Object> hm = new HashMap<>();
		for (final PojoProperty pjp : alPojoProperty) {
			final String fkeyName = pjp.getName();
			JsonNode jn = stackItem.node.get(fkeyName);
			if (jn == null) {
				hm.clear();
				break;
			}

			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), pjp.getPropertyPath());
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			Object objx = convert(jn, pi);
			if (objx == null) {
				hm.clear();
				break;
			}
			hm.put(fkeyName.toLowerCase(), objx);
		}

		OAObjectKey ok = null;
		if (hm.size() > 0) {
			ArrayList<Object> alKey = new ArrayList();
			for (OAFkeyInfo fi : li.getFkeyInfos()) {
				if (fi.getFromPropertyInfo() == null) {
					continue;
				}
				String s = fi.getFromPropertyInfo().getLowerName();
				Object objx = hm.get(s.toLowerCase());
				if (objx == null) {
					alPojoProperty.clear();
					break;
				}
				alKey.add(objx);
			}
			if (alKey.size() != 0) {
				Object[] objs = alKey.toArray(new Object[alKey.size()]);
				ok = new OAObjectKey(objs);
			}
		}

		OAObject obj = null;
		if (ok != null) {
			obj = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
			if (obj == null) {
				obj = (OAObject) OADataSource.getObject(li.getToClass(), ok);
			}
		}
		if (obj == null && ok != null) {
			OAObjectPropertyDelegate.setProperty(stackItem.obj, li.getName(), ok);
		} else {
			stackItem.obj.setProperty(li.getName(), obj);
		}

		return true;
	}

	protected boolean loadObjectPojoImportMatchReferences(final StackItem stackItem, final PojoLinkOne plo, final OALinkInfo li) {
		List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getImportMatchPojoProperties(plo);
		if (alPojoProperty == null || alPojoProperty.size() == 0) {
			return false;
		}

		String sql = null;
		Object[] values = new Object[] {};

		for (final PojoProperty pjp : alPojoProperty) {
			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), pjp.getPropertyPath());
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			JsonNode jn = stackItem.node.get(pjp.getName());
			Object val = convert(jn, pi);

			if (val == null) {
				sql = null;
				break;
			}

			if (sql == null) {
				sql = "";
			} else {
				sql += " AND ";
			}

			sql += OAString.field(pjp.getPropertyPath(), ".", 2, 99) + " = ?";
			values = OAArray.add(Object.class, values, val);
		}

		if (sql == null) {
			stackItem.obj.setProperty(li.getName(), null);
			return true;
		}

		OAFinder finder = new OAFinder();
		OAQueryFilter filter = new OAQueryFilter(li.getToClass(), sql, values);
		finder.addFilter(filter);
		OAObject objNew = (OAObject) OAObjectCacheDelegate.find(li.getToClass(), finder);

		if (objNew == null) {
			OASelect sel = new OASelect(li.getToClass(), sql, values, "");
			objNew = sel.next();
			sel.close();
		}

		if (objNew == null) {
			RetryPojoReference rpr = new RetryPojoReference();
			rpr.stackItem = stackItem;
			rpr.plo = plo;
			rpr.li = li;
			getRetryPojoReferences().add(rpr);
		}

		stackItem.obj.setProperty(li.getName(), objNew);
		return true;
	}

	protected boolean loadObjectPojoUniqueReferences(final StackItem stackItem, final PojoLinkOne plo, final OALinkInfo li) {
		// link unique with equalPp
		List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(plo);
		if (alPojoProperty == null || alPojoProperty.size() == 0) {
			return false;
		}

		String sql = null;
		Object[] values = new Object[] {};

		for (final PojoProperty pjp : alPojoProperty) {
			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), pjp.getPropertyPath());
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			JsonNode jn = stackItem.node.get(pjp.getName());
			Object val = convert(jn, pi);

			if (val == null) {
				sql = null;
				break;
			}

			if (sql == null) {
				sql = "";
			} else {
				sql += " AND ";
			}

			sql += OAString.field(pjp.getPropertyPath(), ".", 2, 99) + " = ?";
			values = OAArray.add(Object.class, values, val);
		}

		if (sql == null) {
			stackItem.obj.setProperty(li.getName(), null);
			return true;
		}

		EqualQueryForReference equalQuery = getEqualQueryForReference(stackItem, plo.getPojoLinkUnique());
		boolean bFound = false;
		if (equalQuery.value != null) {
			sql += " AND " + equalQuery.propPath + " = ?";
			values = OAArray.add(Object.class, values, equalQuery.value);

			OAFinder finder = new OAFinder();
			OAQueryFilter filter = new OAQueryFilter(li.getToClass(), sql, values);
			finder.addFilter(filter);
			OAObject objNew = (OAObject) OAObjectCacheDelegate.find(li.getToClass(), finder);

			if (objNew == null) {
				OASelect sel = new OASelect(li.getToClass(), sql, values, "");
				objNew = sel.next();
				sel.close();
			}
			stackItem.obj.setProperty(li.getName(), objNew);
			bFound = (objNew != null);
		}

		if (!bFound) {
			RetryPojoReference rpr = new RetryPojoReference();
			rpr.stackItem = stackItem;
			rpr.plo = plo;
			rpr.li = li;
			getRetryPojoReferences().add(rpr);
		}

		return true;
	}

	public void findExistingObject(final StackItem stackItem) {
		if (stackItem.li != null && stackItem.li.getAutoCreateNew() && stackItem.parent != null) {
			stackItem.obj = (OAObject) stackItem.li.getValue(stackItem.parent.obj);
			if (stackItem.obj != null) {
				return;
			}
		}
		if (!bUsesPojo) {
			_findExistingObject(stackItem);
		} else {
			_findExistingObjectFromPojo(stackItem);
		}
	}

	// NOTE: the same logic is also in _findExistingObjectPojo
	protected void _findExistingObject(final StackItem stackItem) {
		final String[] keys = stackItem.oi.getIdProperties();
		final boolean bHasKey = keys != null && keys.length > 0;
		if (!bHasKey) {
			return;
		}

		final boolean bCompoundKey = keys.length > 1;

		String sql = null;
		Object[] args = new Object[0];

		if (stackItem.node.isObject()) {
			for (String key : keys) {
				OAPropertyInfo pi = stackItem.oi.getPropertyInfo(key);

				JsonNode jn = stackItem.node.get(stackItem.oi.getJsonUsesCapital() ? pi.getName() : pi.getLowerName());
				Object val = convert(jn, pi);

				if (val == null) {
					sql = null;
					break;
				}

				if (sql == null) {
					sql = "";
				} else {
					sql += " AND ";
				}

				sql += pi.getLowerName() + " = ?";
				args = OAArray.add(Object.class, args, val);
			}
		} else {
			if (stackItem.node.isTextual()) {
				String s = stackItem.node.asText();
				if (s != null && s.startsWith("guid.")) {
					OAObject objx = oajson.getGuidMap().get(s.substring(5));
					stackItem.obj = objx;
					return;
				}
			}

			int pos = -1;
			for (String key : keys) {
				pos++;
				OAPropertyInfo pi = stackItem.oi.getPropertyInfo(key);

				Object val = stackItem.node.asText();
				if (bCompoundKey) {
					val = OAString.field((String) val, '-', pos + 1);
				}
				val = OAConv.convert(pi.getClassType(), val, null);

				if (val == null) {
					sql = null;
					break;
				}

				if (sql == null) {
					sql = "";
				} else {
					sql += " AND ";
				}

				sql += pi.getLowerName() + " = ?";
				args = OAArray.add(Object.class, args, val);
			}
		}

		// first, see if there is Hub to look in
		Hub hub = null;
		if (stackItem.li != null) {
			if (stackItem.li.isMany()) {
				hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);
			} else {
				String pp = stackItem.li.getSelectFromPropertyPath();
				if (OAString.isNotEmpty(pp)) {
					OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), pp);
					hub = (Hub) ppx.getValue(stackItem.parent.obj);
				}
			}
		}

		if (sql == null) {
			if (hub == null) {
				return;
			}
			if (!stackItem.node.isObject()) {
				return;
			}
			if (OAString.isEmpty(stackItem.li.getUniqueProperty())) {
				return;
			}
		}

		if (hub != null) {
			OAFilter filter;
			if (sql != null) {
				filter = new OAQueryFilter(stackItem.li.getToClass(), sql, args);
			} else {
				String s = stackItem.li.getUniqueProperty();
				OAPropertyInfo pi = stackItem.oi.getPropertyInfo(s);
				if (pi == null) {
					return;
				}

				JsonNode jn = stackItem.node.get(s);
				Object val = convert(jn, pi);
				if (val == null) {
					return;
				}
				filter = new OAQueryFilter(stackItem.li.getToClass(), s + " = ?", new Object[] { val });
			}

			for (Object objx : hub) {
				if (filter.isUsed(objx)) {
					stackItem.obj = (OAObject) objx;
					break;
				}
			}
			if (stackItem.obj != null) {
				return;
			}
		}

		if (sql == null) {
			return;
		}

		// see if it's in objCache (since it might not be in DS)
		OADataSource ds = null;
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss != null) {
			for (OADataSource dsx : dss) {
				if (dsx instanceof OADataSourceObjectCache) {
					ds = dsx;
					break;
				}
			}
		}
		if (ds == null) {
			ds = new OADataSourceObjectCache(false);
		}

		OADataSourceIterator dsi = ds.select(stackItem.oi.getForClass(), sql, args, null, false);
		Object objx = dsi.next();
		if (objx == null && OADataSource.getDataSource(stackItem.oi.getForClass()) != ds) {
			OASelect sel = new OASelect(stackItem.oi.getForClass(), sql, args, null);
			objx = sel.next();
		}
		stackItem.obj = (OAObject) objx;
	}

	// NOTE: the same logic is also in _findExistingObject
	protected void _findExistingObjectFromPojo(final StackItem stackItem) {
		final List<PojoProperty> alPojoProperyKeys = PojoDelegate.getPojoPropertyKeys(stackItem.oi.getPojo());
		final boolean bHasKey = alPojoProperyKeys != null && alPojoProperyKeys.size() > 0;
		final boolean bCompoundKey = bHasKey && alPojoProperyKeys.size() > 1;

		final boolean bUsesPKey = bHasKey && PojoDelegate.hasPkey(stackItem.oi);
		final boolean bUsesImportMatch = bHasKey && !bUsesPKey && PojoDelegate.hasImportMatchKey(stackItem.oi);
		final boolean bUseLinkUnique = bHasKey && !bUsesPKey && !bUsesImportMatch && PojoDelegate.hasLinkUniqueKey(stackItem.oi);

		String sql = null;
		Object[] args = new Object[0];

		if (stackItem.node.isObject()) {
			for (PojoProperty pojoProp : alPojoProperyKeys) {
				OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), pojoProp.getPropertyPath());
				OAPropertyInfo pi = pp.getEndPropertyInfo();

				JsonNode jn = stackItem.node.get(pojoProp.getName());
				Object val = convert(jn, pi);

				if (val == null) {
					sql = null;
					break;
				}

				if (sql == null) {
					sql = "";
				} else {
					sql += " AND ";
				}

				sql += pojoProp.getPropertyPath() + " = ?";
				args = OAArray.add(Object.class, args, val);
			}

		} else {
			int pos = -1;
			for (PojoProperty pojoProp : alPojoProperyKeys) {
				pos++;
				OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), pojoProp.getPropertyPath());
				OAPropertyInfo pi = pp.getEndPropertyInfo();

				Object val;

				val = stackItem.node.asText();
				if (bCompoundKey) {
					val = OAString.field((String) val, '-', pos + 1);
				}
				val = OAConv.convert(pi.getClassType(), val, null);

				if (val == null) {
					sql = null;
					break;
				}

				if (sql == null) {
					sql = "";
				} else {
					sql += " AND ";
				}

				sql += pojoProp.getPropertyPath() + " = ?";
				args = OAArray.add(Object.class, args, val);
			}
		}

		String sqlUnique = null;
		Object argUnique = null;

		EqualQueryForObject equalQuery = null;
		if (sql != null && bUseLinkUnique) {
			equalQuery = getEqualQueryForObject(stackItem);
			if (equalQuery.value != null) {
				sqlUnique = equalQuery.propPath + " = ?";
				argUnique = equalQuery.value;
			}
		}

		// first, see if there is Hub to look in
		Hub hub = null;
		if (stackItem.li != null) {
			if (stackItem.li.isMany()) {
				if (stackItem.parent != null) {
					hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);
				}
			} else {
				String pp = stackItem.li.getSelectFromPropertyPath();
				if (OAString.isNotEmpty(pp)) {
					OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), pp);
					hub = (Hub) ppx.getValue(stackItem.parent.obj);
				}
			}
		}

		if (sql == null) {
			if (hub == null) {
				return;
			}
			if (!stackItem.node.isObject()) {
				return;
			}
			if (OAString.isEmpty(stackItem.li.getUniqueProperty())) {
				return;
			}
		}

		if (hub != null) {
			OAFilter filter;
			if (sql != null) {
				filter = new OAQueryFilter(stackItem.li.getToClass(), sql, args);
			} else {
				String s = stackItem.li.getUniqueProperty();
				OAPropertyInfo pi = stackItem.oi.getPropertyInfo(s);
				if (pi == null) {
					return;
				}

				JsonNode jn = stackItem.node.get(s);
				Object val = convert(jn, pi);
				if (val == null) {
					return;
				}
				filter = new OAQueryFilter(stackItem.li.getToClass(), s + " = ?", new Object[] { val });
			}

			for (Object objx : hub) {
				if (filter.isUsed(objx)) {
					stackItem.obj = (OAObject) objx;
					return;
				}
			}
		}

		if (sql == null) {
			return;
		}

		if (sqlUnique != null) {
			args = OAArray.add(Object.class, args, argUnique);

			if (equalQuery != null && equalQuery.cntOrs > 0) {
				sqlUnique = "(" + sqlUnique + " OR " + equalQuery.sqlOrs + ")";
				for (int i = 0; i < equalQuery.cntOrs; i++) {
					args = OAArray.add(Object.class, args, equalQuery.value);
				}
			}
			sql += " AND " + sqlUnique;
		}

		// look in objectCache first
		OADataSource ds = null;
		OADataSource[] dss = OADataSource.getDataSources();
		if (dss != null) {
			for (OADataSource dsx : dss) {
				if (dsx instanceof OADataSourceObjectCache) {
					ds = dsx;
					break;
				}
			}
		}
		if (ds == null) {
			ds = new OADataSourceObjectCache(false);
		}

		if (debug) {
			System.out.println("SQL>>>> " + sql);
		}

		OADataSourceIterator dsi = ds.select(stackItem.oi.getForClass(), sql, args, null, false);
		Object objx = dsi.next();
		if (objx == null && OADataSource.getDataSource(stackItem.oi.getForClass()) != ds) {
			OASelect sel = new OASelect(stackItem.oi.getForClass(), sql, args, null);
			objx = sel.next();
		}
		stackItem.obj = (OAObject) objx;
	}

	protected Object convert(final JsonNode jn, final OAPropertyInfo pi) {
		if (jn == null) {
			return null;
		}
		Object objx;
		if (jn.isNull()) {
			objx = null;
		} else {
			if (pi.isNameValue()) {
				objx = jn.asText();
				if (objx != null) {
					for (int i = 0; i < pi.getNameValues().size(); i++) {
						if (((String) objx).equalsIgnoreCase(pi.getNameValues().get(i))) {
							objx = i;
							break;
						}
					}
				}
			} else if (jn.isNumber()) {
				objx = OAConv.convert(pi.getClassType(), jn.asText(), null);
			} else if (jn.isTextual()) {
				Class paramClass = pi.getClassType();
				String fmt = null;
				if (paramClass.equals(OADate.class)) {
					fmt = pi.getFormat();
					if (OAString.isEmpty(fmt)) {
						fmt = "yyyy-MM-dd";
					}
				} else if (paramClass.equals(OADateTime.class)) {
					fmt = pi.getFormat();
					if (OAString.isEmpty(fmt)) {
						fmt = "yyyy-MM-dd'T'HH:mm:ss";
					}
				} else if (paramClass.equals(OATime.class)) {
					fmt = pi.getFormat();
					if (OAString.isEmpty(fmt)) {
						fmt = "HH:mm:ss";
					}
				} else {
					fmt = pi.getFormat();
				}
				objx = OAConv.convert(pi.getClassType(), jn.asText(), fmt);
			} else {
				objx = OAConv.convert(pi.getClassType(), jn.toString()); //was: asText()
			}
		}
		return objx;
	}

	protected static class EqualQueryForObject {
		String propPath;
		OAObject value;

		String sqlOrs;
		int cntOrs;
	}

	// build query based on Link that has unique and equalPp
	protected EqualQueryForObject getEqualQueryForObject(StackItem stackItem) {
		if (stackItem == null) {
			return null;
		}

		EqualQueryForObject eq = new EqualQueryForObject();

		eq.propPath = stackItem.li.getReverseLinkInfo().getEqualPropertyPath();
		if (OAString.isEmpty(eq.propPath)) {
			eq.propPath = stackItem.li.getReverseLinkInfo().getName();
		}

		final String ppEqualOrig = eq.propPath;

		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (!li.isOne2Many()) {
				continue;
			}
			if (li == stackItem.li.getReverseLinkInfo()) {
				continue;
			}

			String s = li.getEqualPropertyPath();
			if (OAString.isEmpty(s)) {
				continue;
			}

			if (OAString.dcount(eq.propPath, '.') < OAString.dcount(s, '.')) {
				if (s.toLowerCase().startsWith(eq.propPath.toLowerCase())) {
					eq.propPath = s;
				}
			}
		}

		String ppFrom = stackItem.li.getEqualPropertyPath();
		if (OAString.isEmpty(ppFrom)) {
			ppFrom = "";
		}

		if (eq.propPath.length() > ppEqualOrig.length()) {
			String extra = eq.propPath.substring(ppEqualOrig.length() + 1);
			if (!OAString.isEmpty(ppFrom)) {
				ppFrom += ".";
			}
			ppFrom += extra;
		}

		OAPropertyPath pp = new OAPropertyPath(stackItem.li.getReverseLinkInfo().getToClass(), ppFrom);

		// see if any of the props in Pp can be skipped - if they are in stack
		int pos = 0;
		OALinkInfo[] lis = pp.getLinkInfos();
		StackItem si = stackItem.parent;
		for (; lis != null && pos < lis.length;) {
			if (lis[pos] != si.li.getReverseLinkInfo()) {
				break;
			}
			pos++;
			if (si.parent == null) {
				break;
			}
			si = si.parent;
		}

		if (lis != null && pos < lis.length) {
			eq.value = (OAObject) pp.getValue(si.obj, pos);
		} else if (si != null) {
			eq.value = si.obj;
		}

		// include other linkMany that it could be in
		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (!li.isOne2Many()) {
				continue;
			}
			if (li == stackItem.li.getReverseLinkInfo()) {
				continue;
			}

			String s = li.getEqualPropertyPath();
			if (OAString.isEmpty(s)) {
				continue;
			}

			String sx = li.getReverseLinkInfo().getEqualPropertyPath();
			if (OAString.isEmpty(sx)) {
				sx = li.getName();
			} else {
				sx = li.getLowerName() + "." + sx;
			}

			if (eq.propPath.length() > li.getEqualPropertyPath().length()) {
				String extra = eq.propPath.substring(li.getEqualPropertyPath().length() + 1);
				if (!OAString.isEmpty(sx)) {
					sx += ".";
				}
				sx += extra;
			}

			if (eq.sqlOrs == null) {
				eq.sqlOrs = "";
			} else {
				eq.sqlOrs += " OR ";
			}
			eq.sqlOrs += sx + " = ?";

			eq.cntOrs++;
		}

		return eq;
	}

	protected static class EqualQueryForReference {
		StackItem stackItem;
		PojoLinkUnique plu;
		String propPath;
		OAObject value;
	}

	// build query based on Link that has unique and equalPp
	protected EqualQueryForReference getEqualQueryForReference(final StackItem stackItem, final PojoLinkUnique plu) {
		if (stackItem == null || plu == null) {
			return null;
		}

		// String sx = stackItem.toString();
		//	sx += "=>" + plu.getPojoLinkOne().getPojoLink().getName();

		final EqualQueryForReference eq = new EqualQueryForReference();
		eq.stackItem = stackItem;
		eq.plu = plu;

		final OALinkInfo liToRef = stackItem.oi.getLinkInfo(plu.getPojoLinkOne().getPojoLink().getName());
		final OALinkInfo liFromRef = liToRef.getReverseLinkInfo();

		eq.propPath = liFromRef.getEqualPropertyPath();

		// get the root object used in equalPp
		String sppToMatch = liToRef.getEqualPropertyPath();
		OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), sppToMatch);

		// see if any of the props in ppx can be skipped - if they are in stack
		int pos = 0;
		OALinkInfo[] lis = pp.getLinkInfos();
		StackItem si = stackItem;
		for (; lis != null && pos < lis.length;) {
			if (si.li == null || lis[pos] != si.li.getReverseLinkInfo()) {
				break;
			}
			pos++;
			if (si.parent == null) {
				break;
			}
			si = si.parent;
		}

		if (lis != null && pos < lis.length) {
			eq.value = (OAObject) pp.getValue(si.obj, pos);
		} else if (si != null) {
			eq.value = si.obj;
		}
		if (eq.value != null) {
			return eq;
		}

		// find the root object using other links + equalPp
		for (OALinkInfo lix : stackItem.oi.getLinkInfos()) {
			if (!lix.isOne2Many()) {
				continue;
			}
			if (lix == liToRef) {
				continue;
			}

			String s = lix.getEqualPropertyPath();
			if (OAString.isEmpty(s)) {
				continue;
			}

			String extraPp = null;
			if (sppToMatch.toLowerCase().startsWith(s.toLowerCase())) {
				if (sppToMatch.length() > s.length()) {
					extraPp = sppToMatch.substring(s.length());
				} else {
					extraPp = "";
				}
			} else if (s.toLowerCase().startsWith(sppToMatch.toLowerCase())) {
				int x = OAString.dcount(s, ".") - OAString.dcount(sppToMatch, ".");
				OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), s);
				ppx = ppx.getReversePropertyPath();
				OALinkInfo[] lisx = ppx.getLinkInfos();

				for (int i = 0; i < x; i++) {
					if (extraPp == null) {
						extraPp = "";
					}
					extraPp += ".";
					extraPp += lisx[i].getName();
				}
			} else {
				continue;
			}

			if (lix == stackItem.li.getReverseLinkInfo()) {
				String pps = lix.getReverseLinkInfo().getEqualPropertyPath() + extraPp;
				OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pps);

				// see if any of the props in ppx can be skipped - if they are in stack
				pos = 0;
				lis = ppx.getLinkInfos();
				si = stackItem.parent;
				for (; lis != null && pos < lis.length;) {
					if (si.li == null || lis[pos] != si.li.getReverseLinkInfo()) {
						break;
					}
					pos++;
					if (si.parent == null) {
						break;
					}
					si = si.parent;
				}

				eq.value = (OAObject) ppx.getValue(si.obj, pos);
			} else {
				String pps = lix.getEqualPropertyPath() + extraPp;
				OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), pps);
				eq.value = (OAObject) ppx.getValue(stackItem.obj);
			}
			if (eq.value != null) {
				break;
			}
		}
		return eq;
	}

	protected static class RetryPojoReference {
		StackItem stackItem;
		PojoLinkOne plo;
		OALinkInfo li;
	}

	protected void retryPojoReferences() {
		List<RetryPojoReference> al = new ArrayList();
		al.addAll(getRetryPojoReferences());
		getRetryPojoReferences().clear();
		for (RetryPojoReference rpr : al) {
			if (!loadObjectPojoImportMatchReferences(rpr.stackItem, rpr.plo, rpr.li)) {
				loadObjectPojoUniqueReferences(rpr.stackItem, rpr.plo, rpr.li);
			}
		}
	}

	/**
	 * List of references that were not found.
	 */
	public List<RetryPojoReference> getRetryPojoReferences() {
		return alRetryPojoReference;
	}

	public void setDebug(boolean b) {
		this.debug = b;
	}

	public boolean getDebug() {
		return this.debug;
	}

	public void debug(StackItem si, String msg) {
		debug(si, true, msg);
	}

	protected void debug2(StackItem si, String msg) {
		debug(si, false, msg);
	}

	protected void debug(StackItem si, boolean bShowObj, String msg) {
		if (!debug) {
			return;
		}

		String objpath = "";
		int indent = -1;
		for (; si != null; si = si.parent) {
			String s2 = si.oi.getName();
			if (si.li != null) {
				s2 = si.li.getLowerName();
				// s2 = si.li.getLowerName() + " (" + s2 + ")";
			}

			if (objpath.length() > 0) {
				objpath = " => " + objpath;
			}
			objpath = s2 + objpath;

			/*
			if (objpath.length() == 0) {
				objpath = s2;
			}
			*/
			++indent;
		}

		String prefix = "";
		for (int i = 0; i < indent; i++) {
			prefix += "| ";
		}
		if (!bShowObj) {
			objpath = "";
			prefix += "|     ";
		}
		if (OAString.isEmpty(msg)) {
			msg = "";
		} else {
			msg += " ";
		}
		System.out.println(prefix + msg + objpath);
	}
}

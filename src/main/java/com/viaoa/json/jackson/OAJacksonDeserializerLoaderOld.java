package com.viaoa.json.jackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.OASelect;
import com.viaoa.datasource.objectcache.OADataSourceObjectCache;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataDelegate;
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
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.pojo.PojoDelegate;
import com.viaoa.pojo.PojoLink;
import com.viaoa.pojo.PojoLinkOne;
import com.viaoa.pojo.PojoLinkOneDelegate;
import com.viaoa.pojo.PojoProperty;
import com.viaoa.util.OAArray;
import com.viaoa.util.OACompare;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

//qqqqqqqqqqqqqqqqqqqqq
//qqqqqqqqqqqqqqqqqqqqq

// OLD ... see OAJacksonDeserializer

//qqqqqqqqqqqqqqqqqqqqq
//qqqqqqqqqqqqqqqqqqqqq

public class OAJacksonDeserializerLoaderOld {

	private final OAJson oajson;
	private final boolean bUsesPojo;

	private boolean debug = true;

	// need to try again
	protected List<StackItem> alGetObjectUsingLinkEqualWithUnique = new ArrayList<>();
	protected List<StackItem> alGetReferenceUsingLinkEqualWithUnique = new ArrayList<>();

	public OAJacksonDeserializerLoaderOld(OAJson oaj) {
		this.oajson = oaj;
		this.bUsesPojo = oaj.getReadingPojo();
	}

	public <T extends OAObject> T load(final JsonNode node, final T root) {
		return load(node, root, null);
	}

	/**
	 * Main method called by OAJacksonModlue to load an OAObject and any references from json tree.
	 */
	public <T extends OAObject> T load(final JsonNode node, final T root, Class<T> clazz) {
		if (node == null) {
			return null;
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

		StackItem siHold = oajson.getStackItem();
		oajson.setStackItem(stackItem);

		try {
			load(stackItem);
		} finally {
			oajson.setStackItem(siHold);
		}

		for (int i = 0;; i++) {
			int x = alGetObjectUsingLinkEqualWithUnique.size();
			if (x == 0) {
				break;
			}
			List<StackItem> alGetObject = new ArrayList<>(alGetObjectUsingLinkEqualWithUnique);
			alGetObjectUsingLinkEqualWithUnique.clear();
			for (StackItem si : alGetObject) {
				getObjectUsingLinkEqualWithUnique(si);
			}
			if (x == alGetObjectUsingLinkEqualWithUnique.size()) {
				break;
			}
		}

		for (int i = 0;; i++) {
			int x = alGetReferenceUsingLinkEqualWithUnique.size();
			if (x == 0) {
				break;
			}
			List<StackItem> alGetReference = new ArrayList<>(alGetReferenceUsingLinkEqualWithUnique);
			alGetReferenceUsingLinkEqualWithUnique.clear();
			for (StackItem si : alGetReference) {
				getReferenceUsingLinkEqualWithUnique(si);
			}
			if (x == alGetReferenceUsingLinkEqualWithUnique.size()) {
				break;
			}
		}

		return (T) stackItem.obj;
	}

	public void debug(StackItem si, String msg) {
		debug(si, true, msg);
	}

	public void debug2(StackItem si, String msg) {
		debug(si, false, msg);
	}

	public void debug(StackItem si, boolean bShowObj, String msg) {
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

	private int cntLoad;

	/**
	 * Main method for loading JsonObject into a (new or existing) OAObject.<br>
	 * Recursively will load all link objects.
	 * <p>
	 * find or create a matching OAObject from an JsonObject.<br>
	 * This will first use the stack, then guid, pkeys, importMatches, linkWithUnique, linkWithEqualAndUnique.
	 */
	protected void load(final StackItem stackItem) {
		if (stackItem == null) {
			return;
		}

		String debug = ++cntLoad + ")";
		debug(stackItem, "BEG " + debug);

		boolean bNew = false;
		if (stackItem.obj == null) {
			getObject(stackItem);
			if (stackItem.obj == null) {
				createObject(stackItem);
				bNew = true;
			} else {
				if (stackItem.li.getAutoCreateNew()) {
					OAObjectKey ok = stackItem.obj.getObjectKey();
					if (ok.isNew()) {
						loadObjectIdProperties(stackItem);
					}
				}
			}
		}
		loadObject(stackItem);
		if (bNew) {
			addNewObjectToHubInEqualPropertyPath(stackItem);
		}
		debug(stackItem, "END " + debug);
	}

	/**
	 * main method for finding an existing OAObject to match the stackItem.ObjectNode
	 */
	protected boolean getObject(final StackItem stackItem) {
		boolean b = (stackItem.obj != null);
		if (!b) {
			b = getObjectUsingStack(stackItem);
			if (!b) {
				b = getObjectUsingGuid(stackItem);
				if (!b) {
					b = getObjectUsingPKeys(stackItem);
					if (b) {
						debug2(stackItem, "getObjectUsingPKeys *Found");
					}
					if (!b) {
						b = getObjectUsingImportMatch(stackItem);
						if (b) {
							debug2(stackItem, "getObjectUsingImportMatch *Found");
						}
						if (!b) {
							b = getObjectUsingLinkUnique(stackItem);
							if (b) {
								debug2(stackItem, "getObjectUsingLinkUnique *Found");
							}
							if (!b) {
								b = getObjectUsingLinkEqualWithUnique(stackItem);
								if (b) {
									debug2(stackItem, "getObjectUsingLinkEqualWithUnique *Found");
								}
								if (!b) {
									b = getObjectUsingNewObjectGuid(stackItem);
									if (b) {
										debug2(stackItem, "getObjectUsingNewObjectGuid *Found");
									}
									if (!b) {
										// load(..) will call createObject
									}
								}
							}
						}
					}
				}
			}
		}
		return b;

	}

	protected boolean getObjectUsingStack(final StackItem stackItem) {
		if (stackItem.parent == null) {
			return false;
		}
		boolean b;
		b = getObjectUsingStackAutoCreate(stackItem);
		if (b) {
			debug2(stackItem, "getObjectUsingStackAutoCreate *Found");
		}
		if (!b) {
			b = getObjectUsingStackPKeys(stackItem);
			if (b) {
				debug2(stackItem, "getObjectUsingStackPKeys *Found");
			}
			if (!b) {
				b = getObjectUsingStackImportMatch(stackItem);
				if (b) {
					debug2(stackItem, "getObjectUsingStackImportMatch *Found");
				}
				if (!b) {
					b = getObjectUsingStackLinkUnique(stackItem);
					if (b) {
						debug2(stackItem, "getObjectUsingStackLinkUnique *Found");
					}
					if (!b) {
						b = getObjectUsingStackLinkEqualWithUnique(stackItem);
						if (b) {
							debug2(stackItem, "getObjectUsingStackLinkEqualWithUnique *Found");
						}
					}
				}
			}
		}
		return b;
	}

	protected boolean getObjectUsingStackAutoCreate(final StackItem stackItem) {
		if (stackItem.parent == null) {
			return false;
		}
		if (!stackItem.li.getAutoCreateNew() || !stackItem.li.isOne()) {
			return false;
		}
		stackItem.obj = (OAObject) stackItem.li.getValue(stackItem.parent.obj);
		return stackItem.obj != null;
	}

	protected boolean getObjectUsingStackPKeys(final StackItem stackItem) {
		if (stackItem.parent == null) {
			return false;
		}
		if (stackItem.parent.obj == null) {
			return false;
		}

		final OAObjectKey ok = getObjectKeyForJsonObject(stackItem);
		if (ok == null) {
			return false;
		}

		if (stackItem.li.isOne()) {
			OAObject objx = (OAObject) stackItem.li.getValue(stackItem.parent.obj);
			boolean b = (ok.equals(objx));
			if (b) {
				stackItem.obj = objx;
			}
			return b;
		}

		final Hub hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);
		stackItem.obj = (OAObject) HubDataDelegate.getObject(hub, ok);
		return (stackItem.obj != null);
	}

	protected boolean getObjectUsingStackImportMatch(final StackItem stackItem) {
		if (stackItem.parent == null) {
			return false;
		}
		if (stackItem.parent.obj == null) {
			return false;
		}

		if (stackItem.li.isOne()) {
			for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
				if (li.isOne() && li.getImportMatch()) {
					return false; // not supporting import Matches in this method that checks stack.
				}
			}

			Object objx = stackItem.li.getValue(stackItem.parent.obj);
			if (!(objx instanceof OAObject)) {
				return false;
			}

			final OAObject obj = (OAObject) objx;

			boolean bFoundOne = false;
			for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
				if (pi.getId()) {
					continue;
				}
				if (!pi.getImportMatch()) {
					continue;

				}
				bFoundOne = true;
				JsonNode jn = stackItem.node.get(pi.getLowerName());
				if (jn == null) {
					return false;
				}
				Object val = convert(jn, pi);
				if (val == null) {
					return false;
				}

				if (!OACompare.isEqual(val, obj.getProperty(pi.getName()))) {
					return false;
				}
			}
			if (!bFoundOne) {
				return false;
			}
			stackItem.obj = obj;
			return true;
		}

		// Many - find in hub
		if (!stackItem.li.isMany()) {
			return false;
		}

		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (!li.isOne()) {
				continue;
			}

			if (li.getImportMatch()) {
				return false;
			}
		}

		final Hub<OAObject> hub = (Hub<OAObject>) stackItem.li.getValue(stackItem.parent.obj);
		if (hub.isEmpty()) {
			return false;
		}

		List<OAObject> alFound = new ArrayList();
		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}
			if (!pi.getImportMatch()) {
				continue;

			}
			JsonNode jn = stackItem.node.get(pi.getLowerName());
			if (jn == null) {
				return false;
			}
			Object val = convert(jn, pi);
			if (val == null) {
				return false;
			}

			if (alFound.size() > 0) {
				List<OAObject> alNewFound = new ArrayList();
				for (OAObject objx : alFound) {
					if (OACompare.isEqual(val, objx.getProperty(pi.getName()))) {
						alNewFound.add(objx);
					}
				}
				alFound = alNewFound;
			} else {
				for (OAObject objx : hub) {
					if (OACompare.isEqual(val, objx.getProperty(pi.getName()))) {
						alFound.add(objx);
					}
				}
			}
			if (alFound.size() == 0) {
				return false;
			}
		}
		if (alFound.size() != 1) {
			return false;
		}
		stackItem.obj = alFound.get(0);
		return true;
	}

	protected OAObjectKey getObjectKeyForJsonObject(final StackItem stackItem) {
		boolean bIdMissing = false;
		ArrayList<Object> alKeys = new ArrayList();
		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			String propertyName = pi.getLowerName();
			if (!oajson.getUsePropertyCallback(null, propertyName)) {
				continue;
			}
			propertyName = oajson.getPropertyNameCallback(null, propertyName);

			JsonNode jn = stackItem.node.get(propertyName);

			if (jn == null) {
				bIdMissing = true;
				break;
			}

			Object objx = convert(jn, pi);

			if (objx == null) {
				bIdMissing = true;
				break;
			}

			//qq might need to be called in other methods
			objx = oajson.getPropertyValueCallback(null, pi.getLowerName(), objx);

			alKeys.add(objx);
		}

		if (bIdMissing) {
			return null;
		}

		OAObjectKey objKey = new OAObjectKey(alKeys.toArray());
		return objKey;
	}

	protected boolean getObjectUsingStackLinkUnique(final StackItem stackItem) {
		if (stackItem.parent == null) {
			return false;
		}

		if (!stackItem.li.isMany()) {
			return false;
		}

		if (OAString.isEmpty(stackItem.li.getUniqueProperty())) {
			return false;
		}

		if (!stackItem.li.getReverseLinkInfo().isOne()) {
			return false;
		}

		OAPropertyPath pp = new OAPropertyPath(stackItem.li.getToClass(), stackItem.li.getUniqueProperty());
		OAPropertyInfo pi = pp.getEndPropertyInfo();

		final Hub<OAObject> hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);

		if (pi == null) {
			// see if you can get unique object or objKey
			OALinkInfo[] lis = pp.getLinkInfos();
			if (lis == null) {
				return false;
			}
			if (lis.length != 1) {
				return false;
			}

			StackItem stackItemUnique = new StackItem();
			stackItemUnique.parent = stackItem;
			stackItemUnique.oi = lis[0].getToObjectInfo();
			stackItemUnique.li = lis[0];

			try {
				oajson.setStackItem(stackItemUnique);
				getReference(stackItemUnique, false);
			} finally {
				oajson.setStackItem(stackItem);
			}

			if (stackItemUnique.obj != null) {
				OAObject objx = (OAObject) hub.find(lis[0].getLowerName(), stackItemUnique.obj);
				if (objx != null) {
					stackItem.obj = objx;
					return true;
				}
			} else if (stackItemUnique.key != null) {
				for (OAObject objx : hub) {
					if (objx.equals(stackItemUnique.key)) {
						stackItem.obj = objx;
						return true;
					}
				}
			}
			return false;
		}

		JsonNode jn = stackItem.node.get(pi.getLowerName());
		Object val = convert(jn, pi);

		if (val == null) {
			return false;
		}

		OAObject objx = (OAObject) hub.find(pi.getLowerName(), val);
		if (objx == null) {
			return false;
		}

		stackItem.obj = objx;
		return true;
	}

	protected boolean getObjectUsingStackLinkEqualWithUnique(final StackItem stackItem) {
		// see if link equal pp is in the stack
		if (stackItem.parent == null) {
			return false;
		}

		if (!stackItem.li.isOne()) { //qqqqqqqqq needs to also use many
			return false;
		}

		String s = stackItem.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		final OAPropertyPath pp = new OAPropertyPath(stackItem.parent.oi.getForClass(), s);
		if (pp.getEndLinkInfo() == null) {
			return false;
		}
		if (pp.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return false;
		}

		// see if it matches StackItems
		StackItem siEquals = stackItem.parent;
		OALinkInfo[] lis = pp.getLinkInfos();
		if (lis == null) {
			return false;
		}

		for (int i = 0;; i++) {
			if (i == lis.length) {
				break;
			}
			if (siEquals == null) {
				return false; // only using Stack in this method
			}
			if (siEquals.li == null) {
				return false;
			}
			if (!lis[i].equals(siEquals.li.getReverseLinkInfo())) {
				return false;
			}
			siEquals = siEquals.parent;
		}

		if (siEquals.obj == null) {
			return false;
		}

		final List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItem.parent.oi.getPojo(),
																									stackItem.li.getLowerName());
		if (alPojoProperty == null || alPojoProperty.size() != 1) {
			return false;
		}
		final PojoProperty pojoProperty = alPojoProperty.get(0);

		OALinkInfo liRev = stackItem.li.getReverseLinkInfo();
		s = liRev.getEqualPropertyPath();
		if (s == null) {
			return false;
		}

		OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), s);
		if (ppx.getEndLinkInfo() == null) {
			return false;
		}
		if (ppx.getEndLinkInfo().getToObjectInfo() != pp.getEndLinkInfo().getToObjectInfo()) {
			return false;
		}

		OAPropertyPath ppRev = ppx.getReversePropertyPath();
		if (ppRev.getEndLinkInfo() == null) {
			return false;
		}
		if (ppRev.getEndLinkInfo().getType() != OALinkInfo.TYPE_MANY) {
			return false;
		}
		if (OAString.isEmpty(ppRev.getEndLinkInfo().getUniqueProperty())) {
			return false;
		}

		Hub hub = (Hub) ppRev.getEndLinkInfo().getValue(siEquals.obj);

		String uniqueFkeyName = pojoProperty.getName();

		ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pojoProperty.getPropertyPath());
		String uniquePropName = ppx.getEndPropertyInfo().getLowerName();

		JsonNode jn = stackItem.node.get(uniquePropName);
		Object uniqueValueToFind = convert(jn, ppx.getEndPropertyInfo());

		if (uniqueValueToFind == null) {
			return false;
		}

		OAObject objx = (OAObject) hub.find(uniquePropName, uniqueValueToFind);
		if (objx == null) {
			return false;
		}
		stackItem.obj = objx;
		return true;
	}

	protected boolean getObjectUsingGuid(final StackItem stackItem) {
		// will see if object is already loaded from this json loader
		boolean bResult = false;
		JsonNode jn = stackItem.node.get("guid");
		if (jn != null) {
			int guid = jn.asInt();

			OAObject objNew = oajson.getGuidMap().get(guid);
			stackItem.obj = objNew;
			bResult = (objNew != null);
		}
		return bResult;
	}

	protected boolean getObjectUsingPKeys(final StackItem stackItem) {
		final OAObjectKey objKey = getObjectKeyForJsonObject(stackItem);
		if (objKey == null) {
			return false;
		}
		stackItem.key = objKey;

		OAObject objNew = (OAObject) OAObjectCacheDelegate.get(stackItem.oi.getForClass(), objKey);

		if (objNew == null) {
			objNew = (OAObject) OADataSource.getObject(stackItem.oi.getForClass(), objKey);
		}
		stackItem.obj = objNew;
		return (objNew != null);
	}

	protected boolean getObjectUsingNewObjectGuid(final StackItem stackItem) {
		JsonNode jn = stackItem.node.get("guid");
		if (jn == null) {
			return false;
		}

		int guid = jn.asInt();
		OAObjectKey objKey = new OAObjectKey(new Object[0], guid, true);
		stackItem.obj = (OAObject) OAObjectCacheDelegate.get(stackItem.oi.getForClass(), objKey);
		return (stackItem.obj != null);
	}

	protected boolean getObjectUsingImportMatch(final StackItem stackItem) {
		//qqqqq todo:
		// make sure that   oi.getImportMatchPropertyNames())  ==> pojoImportMatch.pojoProperty.propertyPath
		boolean bResult = false;

		String sql = null;
		Object[] values = new Object[] {};

		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}
			if (!pi.getImportMatch()) {
				continue;

			}
			JsonNode jn = stackItem.node.get(pi.getLowerName());
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

			values = OAArray.add(Object.class, values, val);
		}

		for (PojoLink pl : stackItem.oi.getPojo().getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo == null) {
				continue;
			}
			if (plo.getPojoImportMatches().size() == 0) {
				continue;
			}

			for (PojoProperty pjp : PojoLinkOneDelegate.getImportMatchPojoProperties(plo)) {

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

				sql += pjp.getPropertyPath() + " = ?";
				values = OAArray.add(Object.class, values, val);
			}
		}

		if (sql == null) {
			return false;
		}

		OASelect sel = new OASelect(stackItem.oi.getForClass(), sql, values, "");
		OAObject objNew = sel.next();
		sel.close();

		if (objNew == null) {
			OAFinder finder = new OAFinder();
			OAQueryFilter filter = new OAQueryFilter(stackItem.oi.getForClass(), sql, values);
			finder.addFilter(filter);
			objNew = (OAObject) OAObjectCacheDelegate.find(stackItem.oi.getForClass(), finder);
		}

		stackItem.obj = objNew;
		return (objNew != null);
	}

	protected boolean getObjectUsingLinkUnique(final StackItem stackItem) {
		// see if object can be found in an existing O2M hub that has a link.uniqueProp
		boolean bResult = false;

		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (li.getType() != OALinkInfo.TYPE_ONE) {
				continue;
			}
			OALinkInfo rli = li.getReverseLinkInfo();

			if (rli.getType() != OALinkInfo.TYPE_MANY) {
				continue;
			}
			if (OAString.isEmpty(rli.getUniqueProperty())) {
				continue;
			}

			// see if you can get linkTo Object
			StackItem stackItemChild = new StackItem();
			stackItemChild.parent = stackItem;
			stackItemChild.oi = li.getToObjectInfo();
			stackItemChild.li = li;

			if (stackItem.parent != null && stackItem.li == rli) {
				stackItemChild.obj = stackItem.parent.obj;
			} else {
				try {
					oajson.setStackItem(stackItemChild);
					boolean b = getReference(stackItemChild, false);
					if (stackItemChild.obj == null && stackItemChild.key != null) {
						stackItemChild.obj = (OAObject) OAObjectCacheDelegate.get(stackItemChild.li.getToClass(), stackItemChild.key);
						if (stackItemChild.obj == null) {
							// get from DS
							stackItemChild.obj = (OAObject) OADataSource.getObject(stackItemChild.li.getToClass(), stackItemChild.key);
						}
					}
				} finally {
					oajson.setStackItem(stackItem);
				}
			}

			if (stackItemChild.obj == null) {
				continue;
			}

			Hub<OAObject> hub = (Hub) rli.getValue(stackItemChild.obj);
			if (hub.isEmpty()) {
				continue;
			}
			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), rli.getUniqueProperty());
			OAPropertyInfo pi = pp.getEndPropertyInfo();
			if (pi == null) {
				// see if you can get unique object or objKey
				OALinkInfo[] lis = pp.getLinkInfos();
				if (lis == null) {
					continue;
				}
				if (lis.length != 1) {
					continue;
				}

				StackItem stackItemUnique = new StackItem();
				stackItemUnique.parent = stackItem;
				stackItemUnique.oi = lis[0].getToObjectInfo();
				stackItemUnique.li = lis[0];

				try {
					oajson.setStackItem(stackItemUnique);
					getReference(stackItemUnique, false);
				} finally {
					oajson.setStackItem(stackItem);
				}

				if (stackItemUnique.obj != null) {
					OAObject objx = (OAObject) hub.find(lis[0].getLowerName(), stackItemUnique.obj);
					if (objx != null) {
						stackItem.obj = objx;
						return true;
					}
				} else if (stackItemUnique.key != null) {
					for (OAObject objx : hub) {
						if (objx.equals(stackItemUnique.key)) {
							stackItem.obj = objx;
							return true;
						}
					}
				}
				continue;
			}

			JsonNode jn = stackItem.node.get(pi.getLowerName());
			Object val = convert(jn, pi);

			if (val == null) {
				continue;
			}
			OAObject objx = (OAObject) hub.find(pi.getLowerName(), val);
			if (objx != null) {
				stackItem.obj = objx;
				return true;
			}
		}
		return bResult;
	}

	protected boolean getObjectUsingLinkEqualWithUnique(final StackItem stackItem) {
		if (stackItem.parent == null || stackItem.parent.obj == null) {
			return false;
		}

		if (!stackItem.li.isOne()) {
			return false;
		}

		String s = stackItem.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		final OAPropertyPath ppEquals = new OAPropertyPath(stackItem.parent.oi.getForClass(), s);
		if (ppEquals.getEndLinkInfo() == null) {
			return false;
		}
		if (ppEquals.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return false;
		}

		OALinkInfo liRev = stackItem.li.getReverseLinkInfo();
		s = liRev.getEqualPropertyPath();
		if (s == null) {
			return false;
		}

		OAPropertyPath ppEquals2 = new OAPropertyPath(stackItem.oi.getForClass(), s);
		if (ppEquals2.getEndLinkInfo() == null) {
			return false;
		}
		if (ppEquals.getEndLinkInfo().getToObjectInfo() != ppEquals2.getEndLinkInfo().getToObjectInfo()) {
			return false;
		}

		OAPropertyPath ppEquals2Rev = ppEquals2.getReversePropertyPath();
		if (ppEquals2Rev.getEndLinkInfo() == null) {
			return false;
		}
		if (ppEquals2Rev.getEndLinkInfo().getType() != OALinkInfo.TYPE_MANY) {
			return false;
		}
		if (OAString.isEmpty(ppEquals2Rev.getEndLinkInfo().getUniqueProperty())) {
			return false;
		}

		final List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItem.parent.oi.getPojo(),
																									stackItem.li.getLowerName());
		if (alPojoProperty == null || alPojoProperty.size() != 1) {
			return false;
		}
		final PojoProperty pojoProperty = alPojoProperty.get(0);

		stackItem.obj = null;

		String uniqueFkeyName = pojoProperty.getName();

		OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pojoProperty.getPropertyPath());
		String uniquePropName = ppx.getEndPropertyInfo().getLowerName();

		JsonNode jn = stackItem.node.get(uniquePropName);
		Object uniqueValueToFind = convert(jn, ppx.getEndPropertyInfo());

		if (uniqueValueToFind == null) {
			return true;
		}

		//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq

		OAObject objEquals = (OAObject) ppEquals.getValue(stackItem.parent.obj);

		if (objEquals == null) {
			alGetObjectUsingLinkEqualWithUnique.add(stackItem);
			return true;
		}

		final Hub hub = (Hub) ppEquals2Rev.getValue(objEquals);
		OAObject objx = (OAObject) hub.find(uniquePropName, uniqueValueToFind);
		stackItem.obj = objx;

		return stackItem.obj != null;
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
				OAObjectDelegate.initializeAfterLoading((OAObject) stackItem.obj, bNeedsAssignedId, false);
			} finally {
				OAThreadLocalDelegate.setLoading(true);
			}
		}
	}

	protected boolean loadObjectIdProperties(final StackItem stackItem) {
		boolean bNeedsAssignedId = false;
		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			String propertyName = pi.getLowerName();
			if (!oajson.getUsePropertyCallback(null, propertyName)) {
				continue;
			}
			propertyName = oajson.getPropertyNameCallback(null, propertyName);

			JsonNode jn = stackItem.node.get(propertyName);

			if (jn == null) {
				bNeedsAssignedId |= pi.getAutoAssign();
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oajson.getPropertyValueCallback(null, pi.getLowerName(), objx);

			stackItem.obj.setProperty(pi.getLowerName(), objx);
		}
		return bNeedsAssignedId;
	}

	protected void addNewObjectToHubInEqualPropertyPath(StackItem stackItem) {
		if (stackItem == null || stackItem.li == null) {
			return;
		}
		String s = stackItem.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return;
		}

		final OAPropertyPath ppEquals = new OAPropertyPath(stackItem.parent.oi.getForClass(), s);
		if (ppEquals.getEndLinkInfo() == null) {
			return;
		}
		if (ppEquals.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return;
		}

		OALinkInfo liRev = stackItem.li.getReverseLinkInfo();
		s = liRev.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return;
		}

		OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), s);
		if (ppx.getEndLinkInfo() == null) {
			return;
		}
		if (ppx.getEndLinkInfo().getToObjectInfo() != ppEquals.getEndLinkInfo().getToObjectInfo()) {
			return;
		}

		OAPropertyPath ppRev = ppx.getReversePropertyPath();
		if (ppRev.getEndLinkInfo() == null) {
			return;
		}
		if (ppRev.getEndLinkInfo().getType() != OALinkInfo.TYPE_MANY) {
			return;
		}
		if (OAString.isEmpty(ppRev.getEndLinkInfo().getUniqueProperty())) {
			return;
		}

		OAObject objEqual = (OAObject) ppEquals.getValue(stackItem.parent.obj);

		Hub hub = (Hub) ppRev.getValue(objEqual);

		if (hub != null && !hub.contains(stackItem.obj)) {
			hub.add(stackItem.obj);
		}
	}

	protected void loadObject(final StackItem stackItem) {
		if (stackItem.node == null || !stackItem.node.isObject()) {
			throw new RuntimeException("loadObject does not have a node.isObject=true");
		}

		debug2(stackItem, "loadObject " + stackItem.oi.getName());
		loadObjectProperties(stackItem);

		Iterator<String> itx = stackItem.node.fieldNames();
		for (; itx.hasNext();) {
			String name = itx.next();

			OALinkInfo lix = stackItem.oi.getLinkInfo(name);
			if (lix == null) {
				continue;
			}
			if (lix.isOne()) {
				loadObjectOneLink(stackItem, lix);
			} else {
				loadObjectManyLink(stackItem, lix);
			}
		}
	}

	protected void loadObjectProperties(final StackItem stackItem) {
		OAObjectKey objKey = stackItem.obj.getObjectKey();

		// debug2(stackItem, "loadObjectProperties");

		JsonNode jn = stackItem.node.get("guid");
		if (jn != null) {
			int guid = jn.asInt();
			if (oajson != null) {
				oajson.getGuidMap().put(guid, stackItem.obj);
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

			String propertyName = oajson.getPropertyNameCallback(stackItem.obj, pi.getLowerName());

			jn = stackItem.node.get(propertyName);
			if (jn == null) {
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oajson.getPropertyValueCallback(stackItem.obj, pi.getLowerName(), objx);

			stackItem.obj.setProperty(pi.getLowerName(), objx);
		}
	}

	protected void loadObjectOneLink(final StackItem stackItem, final OALinkInfo li) {
		if (li.getType() != li.TYPE_ONE) {
			return;
		}
		if (li.getPrivateMethod()) {
			return;
		}

		if (!oajson.getUsePropertyCallback(stackItem.obj, li.getLowerName())) {
			return;
		}

		// debug2(stackItem, "loadObjectOneLink " + li.getName());

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItem;
		stackItemChild.oi = li.getToObjectInfo();
		stackItemChild.li = li;
		stackItemChild.node = stackItem.node.get(li.getLowerName());

		try {
			oajson.setStackItem(stackItemChild);
			loadObjectOneLink(stackItem, stackItemChild);
		} finally {
			oajson.setStackItem(stackItem);
		}
	}

	protected void loadObjectOneLink(final StackItem stackItem, final StackItem stackItemChild) {
		/* choices:
		 	1: node with link name exists
		 	1a: null
			1b: object node (use load object)
			1c: json number node, prop Id value (75)
			1d: text node ("guid.999", or "obj-key")

			2: use reference properties and pojo*
			2a: fkey props
			2b: link match props
			2c: linkUnique props
		*/

		debug2(stackItem, "loadObjectOneLink " + stackItemChild.li.getName());

		boolean b = (stackItemChild.node != null) && loadObjectOneLink1(stackItemChild);
		if (!b) {
			getReference(stackItemChild);
		}

		if (stackItemChild.obj == null && stackItemChild.key != null) {
			stackItemChild.obj = (OAObject) OAObjectCacheDelegate.get(stackItemChild.li.getToClass(), stackItemChild.key);
			if (stackItemChild.obj == null) {
				// get from DS
				stackItemChild.obj = (OAObject) OADataSource.getObject(stackItemChild.li.getToClass(), stackItemChild.key);
			}
		}

		if (stackItemChild.obj != null || (b && stackItemChild.key == null)) {
			stackItem.obj.setProperty(stackItemChild.li.getLowerName(), stackItemChild.obj);
		} else if ((b && stackItemChild.key == null)) {
			stackItem.obj.setProperty(stackItemChild.li.getLowerName(), null);
		} else if (stackItemChild.key != null) {
			//qqqqqqqqq test this, might need to use propetyDelegate to set the objKey and also send change event??
			stackItem.obj.setProperty(stackItemChild.li.getLowerName(), stackItemChild.key);
		} else {
			//qqqqqqqq exception?? if it's not included (.. do nothing)
			debug2(stackItemChild, "loadObjectOneLink *Not Found");
		}
	}

	protected boolean loadObjectOneLink1(final StackItem stackItemChild) {
		if (stackItemChild.node == null) {
			return false;
		}
		if (loadObjectOneLink1a(stackItemChild)) { // null
			debug2(stackItemChild, "loadObjectOneLink1a *Found");
			return true;
		}
		if (loadObjectOneLink1b(stackItemChild)) { // json object node (use load object)
			debug2(stackItemChild, "loadObjectOneLink1b *Found");
			return true;
		}
		if (loadObjectOneLink1c(stackItemChild)) { // json number node, prop Id value (75)
			debug2(stackItemChild, "loadObjectOneLink1c *Found");
			return true;
		}
		if (loadObjectOneLink1d(stackItemChild)) { // text node ("guid.999", or "obj-key")
			debug2(stackItemChild, "loadObjectOneLink1d *Found");
			return true;
		}
		return false;
	}

	protected boolean loadObjectOneLink1a(final StackItem stackItemChild) {
		return stackItemChild.node.isNull();
	}

	protected boolean loadObjectOneLink1b(final StackItem stackItemChild) {
		if (!stackItemChild.node.isObject()) {
			return false;
		}
		load(stackItemChild);
		return true;
	}

	protected boolean loadObjectOneLink1c(final StackItem stackItemChild) {
		if (!stackItemChild.node.isNumber()) {
			return false;
		}
		String id = stackItemChild.node.asText();
		stackItemChild.key = OAJson.convertJsonSinglePartIdToObjectKey(stackItemChild.li.getToClass(), id);
		return true;
	}

	protected boolean loadObjectOneLink1d(final StackItem stackItemChild) {
		if (!stackItemChild.node.isTextual()) {
			return false;
		}
		String s = stackItemChild.node.asText();
		if (s.indexOf("guid.") == 0) {
			s = s.substring(5);
			int guid = Integer.parseInt(s);
			if (oajson != null) {
				stackItemChild.obj = oajson.getGuidMap().get(guid);
			}
		} else {
			stackItemChild.key = OAJson.convertJsonSinglePartIdToObjectKey(stackItemChild.li.getToClass(), s);
		}
		return true;
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

		String propertyName = oajson.getPropertyNameCallback(stackItem.obj, li.getLowerName());
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
				loadObjectManyLinkPos(stackItemChild, i);
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

	protected void loadObjectManyLinkPos(final StackItem stackItem, final int pos) {

		debug2(stackItem, "loadObjectManyLinkPos pos=" + pos);

		/*
		1b: object node (use load object)
		1c: json number node, prop Id value (75)
		1d: text node ("guid.999", or "obj-key")
		*/

		boolean b = loadObjectManyLinkPos1b(stackItem, pos); // json object node
		if (b) {
			debug2(stackItem, "loadObjectManyLinkPos1b (objectNode) *Found");
			return;
		}

		if (PojoDelegate.hasPkey(stackItem.oi)) {
			b = loadObjectManyLinkPos1c(stackItem, pos); // json numeric node
			if (b) {
				debug2(stackItem, "loadObjectManyLinkPos1c (json number) *Found");
			}
			if (!b) {
				b = loadObjectManyLinkPos1d(stackItem, pos); // json text node for guid or multipart-key
				if (b) {
					debug2(stackItem, "loadObjectManyLinkPos1d (guid or objKey) *Found");
				}
			}
			/*qqqqqqqqqq			
					} else if (bUsesPojo && PojoDelegate.hasImportMatch(stackItem.oi)) {
						//qqqqqqqqqq
						getReference(stackItem, true);
					} else if (bUsesPojo) {
						// qqqqq equal and unique
						getReference(stackItem, true);
			*/
		}

		if (b) {
			if (stackItem.obj == null && stackItem.key != null) {
				stackItem.obj = (OAObject) OAObjectCacheDelegate.get(stackItem.li.getToClass(), stackItem.key);
				if (stackItem.obj == null) {
					// get from DS
					stackItem.obj = (OAObject) OADataSource.getObject(stackItem.li.getToClass(), stackItem.key);
				}
			}
		} else {
			//qqqqqqq error
			if (b) {
				debug2(stackItem, "loadObjectManyLinkPos *NotFound");
			}
		}
	}

	protected boolean loadObjectManyLinkPos1b(final StackItem stackItem, final int pos) {
		if (!stackItem.node.isObject()) {
			return false;
		}
		load(stackItem);
		return true;
	}

	protected boolean loadObjectManyLinkPos1c(final StackItem stackItem, final int pos) {
		if (!stackItem.node.isNumber()) {
			return false;
		}
		String id = stackItem.node.asText();
		stackItem.key = OAJson.convertJsonSinglePartIdToObjectKey(stackItem.li.getToClass(), id);
		return true;
	}

	protected boolean loadObjectManyLinkPos1d(final StackItem stackItem, final int pos) {
		if (!stackItem.node.isTextual()) {
			return false;
		}
		String s = stackItem.node.asText();
		if (s.indexOf("guid.") == 0) {
			s = s.substring(5);
			int guid = Integer.parseInt(s);
			if (oajson != null) {
				stackItem.obj = oajson.getGuidMap().get(guid);
			}
		} else {
			stackItem.key = OAJson.convertJsonSinglePartIdToObjectKey(stackItem.li.getToClass(), s);
		}
		return true;

	}

	// REFERENCE OBJECTS

	/**
	 * main method for finding an existing OAObject to match a reference from the stackItem.parent.objectNode <br>
	 * This will first look in the stack, and then find using fkey property(s), importMatch(s), linkUnique, linkEqualsWithUnique.
	 */
	protected void getReference(final StackItem stackItem) {
		if (stackItem.parent != null && stackItem.obj != null) {
			debug2(stackItem, "getReference");
		}

		boolean bCreateIfNotFound = true;
		if (stackItem.li != null) {
			if (!stackItem.li.isOwner()) {
				if (stackItem.li.getReverseLinkInfo().isOwner()) {
					bCreateIfNotFound = false;
				} else {
					if (stackItem.li.getToObjectInfo().getOwnedByOne() != null) {
						bCreateIfNotFound = false;
					}
				}
			}
		}
		getReference(stackItem, bCreateIfNotFound);
	}

	protected boolean getReference(final StackItem stackItem, final boolean bCreateIfNotFound) {
		boolean bDebug = stackItem.parent != null && stackItem.parent.obj != null;
		if (bDebug) {
			debug2(stackItem, "getReference " + stackItem.li.getName() + ", createIfNotFound=" + bCreateIfNotFound);
		}
		// this does not load the object or set the parent's link property, only finds the matching one and updates stackItemChild.key and/or obj
		boolean b = false;
		if (stackItem.parent != null) {
			b = getReferenceUsingStack(stackItem);
		}
		if (!b) {
			b = getReferenceUsingFKeys(stackItem);
			if (b && bDebug) {
				debug2(stackItem, "getReferenceUsingFKeys *Found");
			}
			if (!b && bUsesPojo) {
				b = getReferenceUsingImportMatch(stackItem);
				if (b && bDebug) {
					debug2(stackItem, "getReferenceUsingImportMatch *Found");
				}
				if (!b) {
					b = getReferenceUsingLinkEqualWithUnique(stackItem);
					if (b && bDebug) {
						debug2(stackItem, "getReferenceUsingLinkEqualWithUnique *Found");
					}

					/*
					//qqqqqqqqqqqqqqqqqq
					if (!b && PojoDelegate.hasLinkUniqueKey(stackItem.oi)) {
						b = getReferenceUsingOtherLinkEqualWithUnique(stackItem);
						if (b && bDebug) {
							debug2(stackItem, "getReferenceUsingLinkEqualWithUnique *Found");
						}
					}
					*/

				}
			}
			if (bCreateIfNotFound) {
				b = true;
				if (bDebug) {
					debug2(stackItem, "createReference *CreatedNew");
				}
				createReference(stackItem);
				addNewObjectToHubInEqualPropertyPath(stackItem);
			} else {
				if (bDebug) {
					debug2(stackItem, "getReference *Not Found/Not Created");
				}
			}
		}
		return b;
	}

	protected boolean getReferenceUsingStack(final StackItem stackItem) {
		// debug2(stackItem, "getReferenceUsingStack");
		if (stackItem.parent == null) {
			return false;
		}

		int xx = 4;
		xx++;//qqqqq

		boolean b = getReferenceUsingStackAutoCreate(stackItem);
		if (b) {
			debug2(stackItem, "getReferenceUsingStackAutoCreate *Found");
		}

		if (!b) {
			b = getReferenceUsingStackParentOwner(stackItem);
			if (b) {
				debug2(stackItem, "getReferenceUsingStackParentOwner *Found");
			}
			if (!b) {
				b = getReferenceUsingStackFKeys(stackItem);
				if (b) {
					debug2(stackItem, "getReferenceUsingStackFKeys *Found");
				}
				if (!b && bUsesPojo) {
					b = getReferenceUsingStackLinkUnique(stackItem);
					if (b) {
						debug2(stackItem, "getReferenceUsingStackLinkUnique *Found");
					}
					if (!b) {
						b = getReferenceUsingStackLinkEqualWithUnique(stackItem);
						if (b) {
							debug2(stackItem, "getReferenceUsingStackLinkEqualWithUnique *Found");
						}
					}
				}
			}
			if (!b) {
				b = getReferenceUsingStackWithOwnerAutoCreate(stackItem);
				if (b) {
					debug2(stackItem, "getReferenceUsingStackWithOwnerAutoCreate *Found");
				}
			}
		}
		return b;
	}

	/**
	 * If stackItem has an 1to1 that has an owner, then find owner object in stack.
	 */
	protected boolean getReferenceUsingStackWithOwnerAutoCreate(final StackItem stackItem) {
		OALinkInfo liOwner = stackItem.oi.getOwnedByOne();
		if (liOwner == null) {
			return false;
		}
		OALinkInfo liRev = liOwner.getReverseLinkInfo();
		if (!liRev.isOne()) {
			return false;
		}
		if (!liRev.getAutoCreateNew()) {
			return false;
		}

		StackItem si = stackItem;
		for (; si != null; si = si.parent) {
			if (si.oi.getForClass().equals(liOwner.getToClass())) {
				if (si.obj instanceof OAObject) {
					stackItem.obj = (OAObject) liRev.getValue(si.obj);
					break;
				}
			}
		}
		return stackItem.obj != null;
	}

	protected boolean getReferenceUsingStackAutoCreate(final StackItem stackItem) {
		// debug2(stackItem, "getReferenceUsingStackAutoCreate");
		if (stackItem.parent == null) {
			return false;
		}
		if (!stackItem.li.getAutoCreateNew()) {
			return false;
		}
		stackItem.obj = (OAObject) stackItem.li.getValue(stackItem.parent.obj);
		return stackItem.obj != null;
	}

	//qqqqqq put in unittests
	protected boolean getReferenceUsingStackParentOwner(final StackItem stackItem) {
		if (stackItem.li == null || stackItem.parent == null || stackItem.parent.li == null) {
			return false;
		}
		if (stackItem.parent.parent == null) {
			return false;
		}

		if (stackItem.li != stackItem.parent.li.getReverseLinkInfo()) {
			return false;
		}
		if (!stackItem.parent.li.getOwner()) {
			return false;
		}

		stackItem.obj = (OAObject) stackItem.li.getValue(stackItem.parent.parent.obj);
		return stackItem.obj != null;
	}

	protected boolean getReferenceUsingStackFKeys(final StackItem stackItem) {
		// debug2(stackItem, "getReferenceUsingStackFKeys");
		if (stackItem.parent.obj == null) {
			return false;
		}

		OAObjectKey ok = getObjectKeyFromFkeys(stackItem);
		if (ok == null) {
			return false;
		}

		if (stackItem.li.getType() != OALinkInfo.TYPE_MANY) {
			OAObject objx = (OAObject) stackItem.li.getValue(stackItem.parent.obj);
			boolean b = (ok.equals(objx));
			if (b) {
				stackItem.obj = objx;
			}
			return b;
		}

		Hub hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);

		OAObject objx = (OAObject) HubDataDelegate.getObject(hub, ok);
		if (objx == null) {
			return false;
		}

		stackItem.obj = objx;
		return true;
	}

	// see if stackItem.parent.obj.hub has this stackItem.node with matching uniqueProp
	protected boolean getReferenceUsingStackLinkUnique(final StackItem stackItem) {
		// debug2(stackItem, "getReferenceUsingStackLinkUnique");
		if (stackItem.parent.obj == null) {
			return false;
		}
		OALinkInfo liMany = stackItem.li;
		if (liMany.getType() != OALinkInfo.TYPE_MANY) {
			return false;
		}

		if (OAString.isEmpty(liMany.getUniqueProperty())) {
			return false;
		}

		OALinkInfo liOne = liMany.getReverseLinkInfo();
		if (liOne.getType() != OALinkInfo.TYPE_ONE) {
			return false;
		}

		OAPropertyPath pp = new OAPropertyPath(liMany.getToClass(), liMany.getUniqueProperty());
		OAPropertyInfo pi = pp.getEndPropertyInfo();
		if (pi == null) {
			return false; // needs to support link value also ??
		}

		JsonNode jn = stackItem.node.get(pi.getLowerName());
		Object val = convert(jn, pi);

		if (val == null) {
			return false;
		}

		Hub hub = (Hub) liMany.getValue(stackItem.parent.obj);

		OAObject objx = (OAObject) hub.find(pi.getLowerName(), val);
		if (objx != null) {
			stackItem.obj = objx;
			return true;
		}
		return false;
	}

	protected boolean getReferenceUsingStackLinkEqualWithUnique(final StackItem stackItem) {

		// see if link equal pp is in the stack
		String s = stackItem.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		final OAPropertyPath pp = new OAPropertyPath(stackItem.parent.oi.getForClass(), s);
		if (pp.getEndLinkInfo() == null) {
			return false;
		}
		if (pp.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return false;
		}

		// see if it matches StackItems
		StackItem siEquals = stackItem.parent;
		OALinkInfo[] lis = pp.getLinkInfos();
		if (lis == null) {
			return false;
		}

		for (int i = 0;; i++) {
			if (i == lis.length) {
				break;
			}
			if (siEquals == null) {
				return false; // only using Stack in this method
			}
			if (siEquals.li == null) {
				return false;
			}
			if (!lis[i].equals(siEquals.li.getReverseLinkInfo())) {
				return false;
			}
			siEquals = siEquals.parent;
		}

		if (siEquals.obj == null) {
			return false;
		}

		final List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItem.parent.oi.getPojo(),
																									stackItem.li.getLowerName());
		if (alPojoProperty == null || alPojoProperty.size() != 1) {
			return false;
		}
		final PojoProperty pojoProperty = alPojoProperty.get(0);

		OALinkInfo liRev = stackItem.li.getReverseLinkInfo();
		s = liRev.getEqualPropertyPath();
		if (s == null) {
			return false;
		}

		OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), s);
		if (ppx.getEndLinkInfo() == null) {
			return false;
		}
		if (ppx.getEndLinkInfo().getToObjectInfo() != pp.getEndLinkInfo().getToObjectInfo()) {
			return false;
		}

		OAPropertyPath ppRev = ppx.getReversePropertyPath();
		if (ppRev.getEndLinkInfo() == null) {
			return false;
		}
		if (ppRev.getEndLinkInfo().getType() != OALinkInfo.TYPE_MANY) {
			return false;
		}
		if (OAString.isEmpty(ppRev.getEndLinkInfo().getUniqueProperty())) {
			return false;
		}

		Hub hub = (Hub) ppRev.getEndLinkInfo().getValue(siEquals.obj);

		String uniqueFkeyName = pojoProperty.getName();

		ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pojoProperty.getPropertyPath());
		String uniquePropName = ppx.getEndPropertyInfo().getLowerName();

		JsonNode jn = stackItem.parent.node.get(uniqueFkeyName);
		if (jn == null) {
			return false;
		}
		Object uniqueValueToFind = convert(jn, ppx.getEndPropertyInfo());

		if (uniqueValueToFind == null) {
			return false;
		}

		OAObject objx = (OAObject) hub.find(uniquePropName, uniqueValueToFind);
		if (objx == null) {
			return false;
		}
		stackItem.obj = objx;
		return true;
	}

	protected boolean getReferenceUsingFKeys(final StackItem stackItem) {
		if (bUsesPojo && !PojoDelegate.hasPkey(stackItem.oi)) {
			return false;
		}
		OAObjectKey ok = getObjectKeyFromFkeys(stackItem);
		if (ok == null) {
			return false;
		}
		stackItem.key = ok;
		return true;
	}

	protected OAObjectKey getObjectKeyFromFkeys(final StackItem stackItem) {
		if (stackItem.parent == null) {
			return null;
		}
		Map<String, Object> hm = new HashMap<>();
		for (PojoProperty pjp : PojoLinkOneDelegate.getLinkFkeyPojoProperties(	stackItem.parent.oi.getPojo(),
																				stackItem.li.getLowerName())) {
			String fkeyName = pjp.getName();
			OAPropertyPath pp = new OAPropertyPath(stackItem.parent.oi.getForClass(), pjp.getPropertyPath());
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			JsonNode jn = stackItem.parent.node.get(fkeyName);
			if (jn == null) {
				hm.clear();
				break;
			}
			Object objx = convert(jn, pi);
			hm.put(fkeyName, objx);
		}
		if (hm.size() == 0) {
			return null;
		}

		ArrayList<Object> al = new ArrayList();
		for (OAFkeyInfo fi : stackItem.li.getFkeyInfos()) {
			if (fi.getFromPropertyInfo() == null) {
				continue;
			}
			String s = fi.getFromPropertyInfo().getLowerName();
			Object objx = hm.get(s);
			if (objx == null) {
				al.clear();
				break;
			}
			al.add(objx);
		}
		if (al.size() == 0) {
			return null;
		}
		Object[] objs = al.toArray(new Object[al.size()]);
		OAObjectKey ok = new OAObjectKey(objs);
		return ok;
	}

	protected boolean getReferenceUsingImportMatch(final StackItem stackItem) {
		// debug2(stackItem, "getReferenceUsingImportMatch");
		if (stackItem.parent == null) {
			return false;
		}

		int pos = 0;
		String sql = null;
		Object[] values = new Object[] {};
		for (PojoProperty pjp : PojoLinkOneDelegate.getImportMatchPojoProperties(	stackItem.parent.oi.getPojo(),
																					stackItem.li.getLowerName())) {
			String propertyName = pjp.getName();
			String propertyPath = pjp.getPropertyPath();

			OAPropertyPath pp = new OAPropertyPath(stackItem.parent.oi.getForClass(), propertyPath);

			JsonNode jn = stackItem.parent.node.get(propertyName);
			if (jn == null) {
				if (pp.getEndPropertyInfo() != null && pp.getEndPropertyInfo().isKey()) {//qqqqq not sure this is needed
					continue;
				}
				sql = null;
				break;
			}

			if (sql == null) {
				sql = "";
			} else {
				sql += " AND ";
			}

			OAPropertyInfo pi;
			if (OAString.isEmpty(propertyPath)) {
				sql += propertyName + " = ?";
				pi = stackItem.parent.oi.getPropertyInfo(propertyName);
			} else {
				sql += propertyPath + " = ?";
				pi = pp.getEndPropertyInfo();
			}

			Object val = convert(jn, pi);

			if (val == null) {
				sql = null;
				break;
			}

			values = OAArray.add(Object.class, values, val);
		}
		if (OAString.isEmpty(sql)) {
			return false;
		}

		OAObject oaObj = null;
		OASelect sel = new OASelect(stackItem.parent.oi.getForClass(), sql, values, "");
		oaObj = sel.next();
		sel.close();

		if (oaObj == null) {
			OAFinder finder = new OAFinder();
			OAQueryFilter filter = new OAQueryFilter(stackItem.parent.oi.getForClass(), sql, values);
			finder.addFilter(filter);
			oaObj = (OAObject) OAObjectCacheDelegate.find(stackItem.parent.oi.getForClass(), finder);
			if (oaObj == null) {
				return false;
			}
		}
		stackItem.obj = oaObj;
		return true;
	}

	//qqqqqqqqqqqqqqqqqqqqqq
	/* this cant work, not needed
	protected boolean getReferenceUsingLinkUnique(final StackItem stackItem) {
		// see if object can be found in an existing O2M hub that has a link.uniqueProp
		boolean bResult = false;

		if (stackItem.node == null) {
			return false;
		}

		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (li.getType() != OALinkInfo.TYPE_ONE) {
				continue;
			}
			OALinkInfo rli = li.getReverseLinkInfo();

			if (rli.getType() != OALinkInfo.TYPE_MANY) {
				continue;
			}
			if (OAString.isEmpty(rli.getUniqueProperty())) {
				continue;
			}

			// see if you can get linkTo Object
			StackItem stackItemChild = new StackItem();
			stackItemChild.parent = stackItem;
			stackItemChild.oi = li.getToObjectInfo();
			stackItemChild.li = li;

			if (stackItem.parent != null && stackItem.li == rli) {
				stackItemChild.obj = stackItem.parent.obj;
			} else {
				try {
					oajson.setStackItem(stackItemChild);
					getReference(stackItemChild);
				} finally {
					oajson.setStackItem(stackItem);
				}
			}

			if (stackItemChild.obj == null) {
				continue;
			}

			Hub hub = (Hub) rli.getValue(stackItemChild.parent.obj);

			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), rli.getUniqueProperty());
			OAPropertyInfo pi = pp.getEndPropertyInfo();
			if (pi == null) {
				continue; // needs to support link value also ??
			}

			JsonNode jn = stackItem.parent.node.get(pi.getLowerName());
			Object val = convert(jn, pi);

			if (val == null) {
				continue;
			}
			OAObject objx = (OAObject) hub.find(pi.getLowerName(), val);
			if (objx != null) {
				stackItem.obj = objx;
				return true;
			}
		}
		return bResult;
	}
	*/

	/*
	// not used, this is a version that builds a query
	protected boolean getReferenceUsingLinkUnique2(final StackItem stackItemChild) {
		if (stackItemChild.parent == null) {
			return false;
		}

		final List<PojoProperty> al = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItemChild.parent.oi.getPojo(),
																						stackItemChild.li.getLowerName());
		if (al == null || al.size() == 0) {
			return false;
		}

		//qqqqqqqqq only if you can get to the value for the equal property path

		String s = stackItemChild.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		OAPropertyPath pp = new OAPropertyPath(stackItemChild.parent.oi.getForClass(), s);

		OAObject equalObject = null;

		//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq

		getObjectUsingStackLinkEqualWithUnique(stackItemChild);

		// equalObject = getStackExistingValueFromStack(stackItemChild, pp);

		if (equalObject == null) {
			return false;
		}

		int pos = 0;
		String sql = null;
		Object[] values = new Object[] {};

		for (PojoProperty pjp : al) {
			String propertyName = pjp.getName();
			String propertyPath = pjp.getPropertyPath();

			pp = new OAPropertyPath(stackItemChild.parent.oi.getForClass(), propertyPath);

			JsonNode jn = stackItemChild.parent.node.get(propertyName);
			if (jn == null) {
				if (pp.getEndPropertyInfo() != null && pp.getEndPropertyInfo().isKey()) {
					continue;
				}
				sql = null;
				break;
			}

			if (sql == null) {
				sql = "";
			} else {
				sql += " AND ";
			}

			sql += "(";

			OAPropertyInfo pi;
			if (OAString.isEmpty(propertyPath)) {
				sql += propertyName + " = ?";
				pi = stackItemChild.parent.oi.getPropertyInfo(propertyName);
			} else {
				sql += propertyPath + " = ?";
				pi = pp.getEndPropertyInfo();
			}

			Object val = convert(jn, pi);

			if (val == null) {
				sql = null;
				break;
			}

			values = OAArray.add(Object.class, values, val);

			sql += " AND ";

			sql += stackItemChild.li.getReverseLinkInfo().getEqualPropertyPath() + " = ?";
			values = OAArray.add(Object.class, values, equalObject);

			sql += ")";
		}

		// qqqqqqqqqq might want to use reverse pp of toObj equal pp and do a find on that Hub

		OAObject oaObj = null;
		if (sql != null) {
			OASelect sel = new OASelect(stackItemChild.oi.getForClass(), sql, values, "");
			oaObj = sel.next();
			sel.close();

			if (oaObj == null) {
				OAFinder finder = new OAFinder();
				OAQueryFilter filter = new OAQueryFilter(stackItemChild.oi.getForClass(), sql, values);
				finder.addFilter(filter);
				oaObj = (OAObject) OAObjectCacheDelegate.find(stackItemChild.oi.getForClass(), finder);
			}
		}

		return true;
	}
	*/

	protected boolean getReferenceUsingLinkEqualWithUnique(final StackItem stackItem) {
		// debug2(stackItem, "getReferenceUsingLinkEqualWithUnique");
		if (stackItem.parent == null) {
			return false;
		}

		if (!stackItem.li.isOne()) {
			return false;
		}

		String s = stackItem.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		final OAPropertyPath ppEquals = new OAPropertyPath(stackItem.parent.oi.getForClass(), s);
		if (ppEquals.getEndLinkInfo() == null) {
			return false;
		}
		if (ppEquals.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return false;
		}

		OALinkInfo liRev = stackItem.li.getReverseLinkInfo();
		s = liRev.getEqualPropertyPath();
		if (s == null) {
			return false;
		}

		OAPropertyPath ppEquals2 = new OAPropertyPath(stackItem.oi.getForClass(), s);
		if (ppEquals2.getEndLinkInfo() == null) {
			return false;
		}
		if (ppEquals.getEndLinkInfo().getToObjectInfo() != ppEquals2.getEndLinkInfo().getToObjectInfo()) {
			return false;
		}

		OAPropertyPath ppEquals2Rev = ppEquals2.getReversePropertyPath();
		if (ppEquals2Rev.getEndLinkInfo() == null) {
			return false;
		}
		if (ppEquals2Rev.getEndLinkInfo().getType() != OALinkInfo.TYPE_MANY) {
			return false;
		}
		if (OAString.isEmpty(ppEquals2Rev.getEndLinkInfo().getUniqueProperty())) {
			return false;
		}

		final List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItem.parent.oi.getPojo(),
																									stackItem.li.getLowerName());
		if (alPojoProperty == null || alPojoProperty.size() != 1) {
			return false;
		}
		PojoProperty pojoProperty = alPojoProperty.get(0);

		String uniqueFkeyName = pojoProperty.getName();
		JsonNode jn = stackItem.parent.node.get(uniqueFkeyName);

		final OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pojoProperty.getPropertyPath());
		Object uniqueValueToFind = convert(jn, ppx.getEndPropertyInfo());

		if (uniqueValueToFind == null) {
			return true; // null
		}

		OAObject objEquals = null;

		if (stackItem.parent.obj != null) {
			objEquals = (OAObject) ppEquals.getValue(stackItem.parent.obj);
		} else if (stackItem.parent.parent != null) {
			//qqqqqqqqqqqqqqqqqq find equals object qqqqqqqqqqqqq
			objEquals = getEqualsObject(stackItem);
		}

		if (objEquals == null) {
			alGetReferenceUsingLinkEqualWithUnique.add(stackItem);
			return true;
		}

		String uniquePropName = ppx.getEndPropertyInfo().getLowerName();

		final Hub hub = (Hub) ppEquals2Rev.getValue(objEquals);
		OAObject objx = (OAObject) hub.find(uniquePropName, uniqueValueToFind);
		stackItem.obj = objx;

		return stackItem.obj != null;
	}

	public void findExistingObject(final StackItem stackItem) {
		if (!bUsesPojo) {
			_findExistingObject(stackItem);
		} else {
			_findExistingObjectFromPojo(stackItem);
		}
	}

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

				JsonNode jn = stackItem.node.get(pi.getLowerName());
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
		if (sql == null) {
			return;
		}

		// first, see if there is Hub to look in
		Hub hub = null;
		if (stackItem.li.isMany()) {
			hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);
		} else {
			String pp = stackItem.li.getSelectFromPropertyPath();
			if (OAString.isNotEmpty(pp)) {
				OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), pp);
				hub = (Hub) ppx.getValue(stackItem.parent.obj);
			}
		}

		if (hub != null) {
			OAFilter filter = new OAQueryFilter(stackItem.li.getToClass(), sql, args);
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
			OASelect sel = new OASelect(stackItem.li.getToClass(), sql, args, null);
			objx = sel.next();
		}
		stackItem.obj = (OAObject) objx;

	}

	protected void _findExistingObjectFromPojo(final StackItem stackItem) {
		List<PojoProperty> alProjProperty = PojoDelegate.getPojoPropertyKeys(stackItem.oi.getPojo());

		final List<PojoProperty> alPojoProperyKeys = PojoDelegate.getPojoPropertyKeys(stackItem.oi.getPojo());
		final boolean bHasKey = alPojoProperyKeys != null && alPojoProperyKeys.size() > 0;
		if (!bHasKey) {
			return;
		}

		final boolean bCompoundKey = bHasKey && alProjProperty.size() > 1;

		final boolean bUsesPKey = bHasKey && PojoDelegate.hasPkey(stackItem.oi);
		final boolean bUsesImportMatch = bUsesPojo && bHasKey && !bUsesPKey && PojoDelegate.hasImportMatchKey(stackItem.oi);
		final boolean bUseLinkUnique = bUsesPojo && bHasKey && !bUsesPKey && !bUsesImportMatch
				&& PojoDelegate.hasLinkUniqueKey(stackItem.oi);

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
		if (sql == null) {
			return;
		}

		// first, see if there is Hub to look in
		Hub hub = null;
		if (stackItem.li.isMany()) {
			hub = (Hub) stackItem.li.getValue(stackItem.parent.obj);
		} else {
			String pp = stackItem.li.getSelectFromPropertyPath();
			if (OAString.isNotEmpty(pp)) {
				OAPropertyPath ppx = new OAPropertyPath(stackItem.oi.getForClass(), pp);
				hub = (Hub) ppx.getValue(stackItem.parent.obj);
			}
		}

		if (hub != null) {
			OAFilter filter = new OAQueryFilter(stackItem.li.getToClass(), sql, args);
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

		// a linkUnique with equalPp will need to include the equal object in the query
		if (sql != null && bUseLinkUnique) {
			if (stackItem.parent == null) {
				return;
			}

			sql += " AND ";
			sql += stackItem.li.getReverseLinkInfo().getEqualPropertyPath() + " = ?";

			OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), stackItem.li.getEqualPropertyPath());
			Object valx = ppx.getValue(stackItem.parent.obj);
			if (valx == null) {
				return;
			}
			args = OAArray.add(Object.class, args, valx);
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

		OADataSourceIterator dsi = ds.select(stackItem.oi.getForClass(), sql, args, null, false);
		Object objx = dsi.next();
		if (objx == null && OADataSource.getDataSource(stackItem.oi.getForClass()) != ds) {
			OASelect sel = new OASelect(stackItem.oi.getForClass(), sql, args, null);
			objx = sel.next();
		}
		stackItem.obj = (OAObject) objx;
	}

	//qqqqqq if needing to create new, then need to populate the key value (ex: if key is the counter property)

	//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq

	protected boolean getReferenceUsingOtherLinkEqualWithSameUnique(final StackItem stackItem) {
		// see if object can be found in an existing O2M hub that has a link.uniqueProp
		//     that matches value for reference in stackItem.node

		boolean bResult = false;

		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {

			if (li.getType() != OALinkInfo.TYPE_ONE) {
				continue;
			}
			OALinkInfo rli = li.getReverseLinkInfo();

			if (rli.getType() != OALinkInfo.TYPE_MANY) {
				continue;
			}
			if (OAString.isEmpty(rli.getUniqueProperty())) {
				continue;
			}

			if (OAString.isEmpty(rli.getEqualPropertyPath())) {
				continue;
			}

			String sql = null;
			Object[] values = new Object[] {};

			for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
				if (pi.getId()) {
					continue;
				}
				if (!pi.getImportMatch()) {
					continue;

				}
				JsonNode jn = stackItem.node.get(pi.getLowerName());
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

				values = OAArray.add(Object.class, values, val);
			}

			// see if you can get linkTo Object
			StackItem stackItemChild = new StackItem();
			stackItemChild.parent = stackItem;
			stackItemChild.oi = li.getToObjectInfo();
			stackItemChild.li = li;

			if (stackItem.parent != null && stackItem.li == rli) {
				stackItemChild.obj = stackItem.parent.obj;
			} else {
				try {

					//qqqqqqqqqqqqqqqqqqqqq

					oajson.setStackItem(stackItemChild);
					boolean b = getReference(stackItemChild, false);
					if (stackItemChild.obj == null && stackItemChild.key != null) {
						stackItemChild.obj = (OAObject) OAObjectCacheDelegate.get(stackItemChild.li.getToClass(), stackItemChild.key);
						if (stackItemChild.obj == null) {
							// get from DS
							stackItemChild.obj = (OAObject) OADataSource.getObject(stackItemChild.li.getToClass(), stackItemChild.key);
						}
					}
				} finally {
					oajson.setStackItem(stackItem);
				}
			}

			if (stackItemChild.obj == null) {
				continue;
			}

			Hub<OAObject> hub = (Hub) rli.getValue(stackItemChild.obj);
			if (hub.isEmpty()) {
				continue;
			}
			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), rli.getUniqueProperty());
			OAPropertyInfo pi = pp.getEndPropertyInfo();
			if (pi == null) {
				// see if you can get unique object or objKey
				OALinkInfo[] lis = pp.getLinkInfos();
				if (lis == null) {
					continue;
				}
				if (lis.length != 1) {
					continue;
				}

				StackItem stackItemUnique = new StackItem();
				stackItemUnique.parent = stackItem;
				stackItemUnique.oi = lis[0].getToObjectInfo();
				stackItemUnique.li = lis[0];

				try {
					oajson.setStackItem(stackItemUnique);
					getReference(stackItemUnique, false);
				} finally {
					oajson.setStackItem(stackItem);
				}

				if (stackItemUnique.obj != null) {
					OAObject objx = (OAObject) hub.find(lis[0].getLowerName(), stackItemUnique.obj);
					if (objx != null) {
						stackItem.obj = objx;
						return true;
					}
				} else if (stackItemUnique.key != null) {
					for (OAObject objx : hub) {
						if (objx.equals(stackItemUnique.key)) {
							stackItem.obj = objx;
							return true;
						}
					}
				}
				continue;
			}

			JsonNode jn = stackItem.node;
			Object val = convert(jn, pi);

			if (val == null) {
				continue;
			}
			OAObject objx = (OAObject) hub.find(pi.getLowerName(), val);
			if (objx != null) {
				stackItem.obj = objx;
				return true;
			}
		}
		return bResult;
	}

	protected void createReference(final StackItem stackItem) {
		debug2(stackItem, "createReference");
		final Class clazz = stackItem.oi.getForClass();

		stackItem.obj = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);

		List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkFkeyPojoProperties(	stackItem.parent.oi.getPojo(),
																							stackItem.li.getLowerName());

		boolean bNeedsAssignedId = true;
		if (alPojoProperty != null) {
			for (PojoProperty pjp : alPojoProperty) {
				OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pjp.getPropertyPath());
				JsonNode jn = stackItem.parent.node.get(pjp.getName());
				Object objx = convert(jn, ppx.getEndPropertyInfo());
				stackItem.obj.setProperty(ppx.getLastPropertyName(), objx);
				bNeedsAssignedId = false;
			}
		}

		alPojoProperty = PojoLinkOneDelegate.getImportMatchPojoProperties(	stackItem.parent.oi.getPojo(),
																			stackItem.li.getLowerName());
		if (alPojoProperty != null) {
			for (PojoProperty pjp : alPojoProperty) {
				OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pjp.getPropertyPath());
				JsonNode jn = stackItem.parent.node.get(pjp.getName());
				Object objx = convert(jn, ppx.getEndPropertyInfo());
				stackItem.obj.setProperty(ppx.getLastPropertyName(), objx);
			}
		}

		alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItem.parent.oi.getPojo(),
																			stackItem.li.getLowerName());
		if (alPojoProperty != null) {
			for (PojoProperty pjp : alPojoProperty) {
				OAPropertyPath ppx = new OAPropertyPath(stackItem.parent.oi.getForClass(), pjp.getPropertyPath());
				JsonNode jn = stackItem.parent.node.get(pjp.getName());
				Object objx = convert(jn, ppx.getEndPropertyInfo());
				stackItem.obj.setProperty(ppx.getLastPropertyName(), objx);
			}
		}

		if (OAThreadLocalDelegate.isLoading()) {
			OAThreadLocalDelegate.setLoading(false);
			try {
				OAObjectDelegate.initializeAfterLoading((OAObject) stackItem.obj, bNeedsAssignedId, false);
			} finally {
				OAThreadLocalDelegate.setLoading(true);
			}
		}
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

	protected OAObject getEqualsObject(StackItem stackItem) {
		// stackItem has a linkInfo One

		if (stackItem.parent == null) {
			return null;
		}

		if (!stackItem.li.isOne()) {
			return null;
		}

		OALinkInfo liOne = stackItem.li;
		String ppLinkOneEquals = liOne.getEqualPropertyPath();
		if (OAString.isEmpty(ppLinkOneEquals)) {
			return null;
		}

		final OAPropertyPath ppLinkOneEqualsPP = new OAPropertyPath(stackItem.parent.oi.getForClass(), ppLinkOneEquals);
		if (ppLinkOneEqualsPP.getEndLinkInfo() == null) {
			return null;
		}
		if (ppLinkOneEqualsPP.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return null;
		}

		OALinkInfo liMany = stackItem.li.getReverseLinkInfo();
		String ppLinkManyEquals = liMany.getEqualPropertyPath();
		if (OAString.isEmpty(ppLinkManyEquals)) {
			return null;
		}

		OAPropertyPath ppLinkManyEqualsPP = new OAPropertyPath(stackItem.oi.getForClass(), ppLinkManyEquals);
		if (ppLinkManyEqualsPP.getEndLinkInfo() == null) {
			return null;
		}
		if (ppLinkOneEqualsPP.getEndLinkInfo().getToObjectInfo() != ppLinkManyEqualsPP.getEndLinkInfo().getToObjectInfo()) {
			return null;
		}

		if (stackItem.obj != null) {
			OAObject obj = (OAObject) ppLinkOneEqualsPP.getValue(stackItem.obj);
			return obj;
		}

		if (stackItem.parent.obj != null) {
			OAObject obj = (OAObject) ppLinkManyEqualsPP.getValue(stackItem.obj);
			return obj;
		}

		Class classEquals = ppLinkOneEqualsPP.getEndLinkInfo().getToObjectInfo().getForClass();

		OAObject obj = null;
		StackItem si = stackItem;
		for (; si != null; si = si.parent) {
			if (si.oi.getForClass().equals(classEquals)) {
				if (si.obj instanceof OAObject) {
					obj = (OAObject) si.obj;
					break;
				}
			}
		}
		return obj;
	}

}

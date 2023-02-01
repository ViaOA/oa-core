package com.viaoa.json.jackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
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
import com.viaoa.pojo.PojoLink;
import com.viaoa.pojo.PojoLinkOne;
import com.viaoa.pojo.PojoLinkOneDelegate;
import com.viaoa.pojo.PojoProperty;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

/**
 * Used by OAJson to convert JSON to OAObject(s).
 * <p>
 * This supports object graphs, json from POJOs without directly matching pkeys properties.
 * <p>
 * For POJOs without direct pkey & fkey properties, it uses the following to find and update matching OAObjects
 * <ul>
 * <li>Matching unique pkey properties
 * <li>Guid match
 * <li>ImportMatch properties
 * <li>Links that have a unique property match.
 * <li>Seed objects, that are set before deserialization.
 * </ul>
 */
public class OAJacksonDeserializerLoader {

	private final OAJson oajson;

	public OAJacksonDeserializerLoader(OAJson oaj) {
		this.oajson = oaj;
	}

	public <T extends OAObject> T load(final JsonNode node, final T root) {
		return load(node, root, null);
	}

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
		return (T) stackItem.obj;
	}

	/**
	 * Main method for loading JsonObject into a (new or existing) OAObject.<br>
	 * Recursively will load all link objects.
	 * <p>
	 * * find or create a matching OAObject from an JsonObject.<br>
	 * This will first use the stack, then guid, pkeys, importMatches, linkWithUnique.
	 */
	protected void load(final StackItem stackItem) {
		if (stackItem == null) {
			return;
		}

		if (stackItem.obj == null) {
			getObject(stackItem);
			if (stackItem.obj == null) {
				createObject(stackItem);
			}
		}
		loadObject(stackItem);
	}

	protected boolean getObject(final StackItem stackItem) {
		boolean b = (stackItem.obj != null);
		if (!b) {
			b = getObjectUsingStack(stackItem);
			if (!b) {
				b = getObjectUsingGuid(stackItem);
				if (!b) {
					b = getObjectUsingPKeys(stackItem);
					if (!b) {
						b = getObjectUsingNewObjectGuid(stackItem);
						if (!b) {
							b = getObjectUsingImportMatches(stackItem);
							if (!b) {
								b = getObjectUsingLinkUnique(stackItem);
								if (!b) {
									// load(..) will call createObject
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
		final StackItem stackItemParent = stackItem.parent;
		if (stackItemParent == null) {
			return false;
		}
		boolean b = getObjectUsingStackAutoCreate(stackItemParent, stackItem);
		if (!b) {
			b = getObjectUsingStackPKeys(stackItemParent, stackItem);
			if (!b) {
				b = getObjectUsingStackLinkUnique(stackItemParent, stackItem);
				if (!b) {
					b = getObjectUsingStackLinkEqualWithUnique(stackItemParent, stackItem);
				}
			}
		}
		return b;
	}

	protected boolean getObjectUsingStackAutoCreate(final StackItem stackItemParent, final StackItem stackItemChild) {
		if (!stackItemChild.li.getAutoCreateNew()) {
			return false;
		}
		stackItemChild.obj = (OAObject) stackItemChild.li.getValue(stackItemParent.obj);
		return stackItemChild.obj != null;
	}

	protected boolean getObjectUsingStackPKeys(final StackItem stackItemParent, final StackItem stackItemChild) {

		if (stackItemParent.obj == null) {
			return false;
		}

		OAObjectKey ok = getObjectKeyForJsonObject(stackItemChild);
		if (ok == null) {
			return false;
		}

		if (stackItemChild.li.getType() != OALinkInfo.TYPE_MANY) {
			OAObject objx = (OAObject) stackItemChild.li.getValue(stackItemParent.obj);
			boolean b = (ok.equals(objx));
			if (b) {
				stackItemChild.obj = objx;
			}
			return b;
		}

		Hub hub = (Hub) stackItemChild.li.getValue(stackItemParent.obj);

		OAObject objx = (OAObject) HubDataDelegate.getObject(hub, ok);
		if (objx == null) {
			return false;
		}

		stackItemChild.obj = objx;
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

			objx = oajson.getPropertyValueCallback(null, pi.getLowerName(), objx);

			alKeys.add(objx);
		}

		if (bIdMissing) {
			return null;
		}

		OAObjectKey objKey = new OAObjectKey(alKeys.toArray());
		return objKey;
	}

	protected boolean getObjectUsingStackLinkUnique(final StackItem stackItemParent, final StackItem stackItemChild) {
		OALinkInfo liMany = stackItemChild.li;
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
			return false; //qqqqqqqq needs to support link value also ??
		}

		JsonNode jn = stackItemChild.node.get(pi.getLowerName());
		Object val = convert(jn, pi);

		if (val == null) {
			return false;
		}

		Hub hub = (Hub) liMany.getValue(stackItemParent.obj);

		OAObject objx = (OAObject) hub.find(pi.getLowerName(), val);
		if (objx != null) {
			stackItemChild.obj = objx;
			return true;
		}
		return false;
	}

	protected boolean getObjectUsingStackLinkEqualWithUnique(final StackItem stackItemParent,
			final StackItem stackItemChild) {
		// see if link equal pp is in the stack
		String s = stackItemChild.li.getEqualPropertyPath();
		if (s == null) {
			return false;
		}

		final OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), s);
		if (pp.getEndLinkInfo() == null) {
			return false;
		}
		if (pp.getEndLinkInfo().getType() != OALinkInfo.TYPE_ONE) {
			return false;
		}

		// see if it matches StackItems
		StackItem siEquals = stackItemParent;
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

		final List<PojoProperty> alPojoProperty = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItemParent.oi.getPojo(),
																									stackItemChild.li.getLowerName());
		if (alPojoProperty == null || alPojoProperty.size() != 1) {
			return false;
		}
		final PojoProperty pojoProperty = alPojoProperty.get(0);

		OALinkInfo liRev = stackItemChild.li.getReverseLinkInfo();
		s = liRev.getEqualPropertyPath();
		if (s == null) {
			return false;
		}

		OAPropertyPath ppx = new OAPropertyPath(stackItemChild.oi.getForClass(), s);
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

		ppx = new OAPropertyPath(stackItemParent.oi.getForClass(), pojoProperty.getPropertyPath());
		String uniquePropName = ppx.getEndPropertyInfo().getLowerName();

		JsonNode jn = stackItemChild.node.get(uniquePropName);
		Object uniqueValueToFind = convert(jn, ppx.getEndPropertyInfo());

		if (uniqueValueToFind == null) {
			return false;
		}

		OAObject objx = (OAObject) hub.find(uniquePropName, uniqueValueToFind);
		if (objx == null) {
			return false;
		}
		stackItemChild.obj = objx;
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
		OAObjectKey objKey = getObjectKeyForJsonObject(stackItem);
		if (objKey == null) {
			return false;
		}

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

	protected boolean getObjectUsingImportMatches(final StackItem stackItem) {
		//qqqqqqqqqqqqqq todo:
		//make sure that   oi.getImportMatchPropertyNames())  ==> pojoImportMatch.pojoProperty.propertyPath
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
					getReferenceObject(stackItemChild);
				} finally {
					oajson.setStackItem(stackItem);
				}
			}

			if (stackItemChild.obj == null) {
				continue;
			}

			Hub hub = (Hub) rli.getValue(stackItemChild.obj);

			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), rli.getUniqueProperty());
			OAPropertyInfo pi = pp.getEndPropertyInfo();
			if (pi == null) {
				continue; //qqqqqqqq needs to support link value also ??
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

	protected void createObject(final StackItem stackItem) {
		final Class clazz = stackItem.oi.getForClass();

		oajson.beforeReadCallback(stackItem.node);
		stackItem.obj = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);

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
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oajson.getPropertyValueCallback(null, pi.getLowerName(), objx);

			stackItem.obj.setProperty(pi.getLowerName(), objx);
		}

		if (OAThreadLocalDelegate.isLoading()) {
			OAObjectDelegate.initializeAfterLoading((OAObject) stackItem.obj, false, false);
		}
	}

	protected void loadObject(final StackItem stackItem) {

		if (stackItem.node == null || !stackItem.node.isObject()) {
			throw new RuntimeException("loadObject does not have a node.isObject=true");
		}

		loadObjectProperties(stackItem);
		loadObjectOneLinks(stackItem);
		loadObjectManyLinks(stackItem);
	}

	protected void loadObjectProperties(final StackItem stackItem) {
		OAObjectKey objKey = stackItem.obj.getObjectKey();

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

	protected void loadObjectOneLinks(final StackItem stackItem) {
		// load links of type=one
		final Set<OALinkInfo> setLinkFound = new HashSet<>();

		for (final OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (!oajson.getUsePropertyCallback(stackItem.obj, li.getLowerName())) {
				continue;
			}

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

		boolean b = loadObjectOneLink1(stackItemChild);
		if (!b) {
			getReferenceObject(stackItemChild);
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
		}
	}

	//qqqqqq unit test with CorpToStore model ... to check multipart keys

	protected boolean loadObjectOneLink1(final StackItem stackItemChild) {
		if (stackItemChild.node == null) {
			return false;
		}
		if (loadObjectOneLink1a(stackItemChild)) { // null
			return true;
		}
		if (loadObjectOneLink1b(stackItemChild)) { // json object node (use load object)
			load(stackItemChild);
			return true;
		}
		if (loadObjectOneLink1c(stackItemChild)) { // json number node, prop Id value (75)
			return true;
		}
		if (loadObjectOneLink1d(stackItemChild)) { // text node ("guid.999", or "obj-key")
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

	protected void loadObjectManyLinks(final StackItem stackItem) {

		// load links of type=many
		for (OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_MANY) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (!oajson.getUsePropertyCallback(stackItem.obj, li.getLowerName())) {
				continue;
			}

			String propertyName = oajson.getPropertyNameCallback(stackItem.obj, li.getLowerName());
			JsonNode nodex = stackItem.node.get(propertyName);

			if (!(nodex instanceof ArrayNode)) {
				continue;
			}
			Hub<OAObject> hub = (Hub<OAObject>) li.getValue(stackItem.obj);
			ArrayNode nodeArray = (ArrayNode) nodex;

			List<OAObject> alNew = new ArrayList();
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
					alNew.add(stackItemChild.obj);
				} finally {
					oajson.setStackItem(stackItem);
				}
			}

			//qqqqqqqq test this to make sure that hub adds/removes/moves all work

			List<OAObject> alRemove = new ArrayList();
			for (OAObject objx : hub) {
				if (!alNew.contains(objx)) {
					alRemove.add(objx);
				}
			}
			for (OAObject objx : alNew) {
				if (!hub.contains(objx)) {
					hub.add(objx);
				}
			}
			for (OAObject objx : alRemove) {
				hub.remove(objx);
			}

			// same order
			int i = 0;
			for (OAObject objx : alNew) {
				int pos = hub.getPos(objx);
				hub.move(pos, i);
				i++;
			}
		}
		oajson.afterReadCallback(stackItem.node, stackItem.obj);
	}

	protected void loadObjectManyLinkPos(final StackItem stackItemChild, final int pos) {
		/*
		1b: object node (use load object)
		1c: json number node, prop Id value (75)
		1d: text node ("guid.999", or "obj-key")
		*/

		boolean b = loadObjectManyLinkPos1b(stackItemChild, pos);
		if (!b) {
			b = loadObjectManyLinkPos1c(stackItemChild, pos);
			if (!b) {
				b = loadObjectManyLinkPos1d(stackItemChild, pos);
			}
		}
		if (b) {
			if (stackItemChild.obj == null && stackItemChild.key != null) {
				stackItemChild.obj = (OAObject) OAObjectCacheDelegate.get(stackItemChild.li.getToClass(), stackItemChild.key);
				if (stackItemChild.obj == null) {
					// get from DS
					stackItemChild.obj = (OAObject) OADataSource.getObject(stackItemChild.li.getToClass(), stackItemChild.key);
				}
			}
		} else {
			//qqqqqqq error
		}
	}

	protected boolean loadObjectManyLinkPos1b(final StackItem stackItemChild, final int pos) {
		if (!stackItemChild.node.isObject()) {
			return false;
		}
		load(stackItemChild);
		return true;
	}

	protected boolean loadObjectManyLinkPos1c(final StackItem stackItemChild, final int pos) {
		if (!stackItemChild.node.isNumber()) {
			return false;
		}
		String id = stackItemChild.node.asText();
		stackItemChild.key = OAJson.convertJsonSinglePartIdToObjectKey(stackItemChild.li.getToClass(), id);
		return true;
	}

	protected boolean loadObjectManyLinkPos1d(final StackItem stackItemChild, final int pos) {
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

	//qqqqqqqqqqqqqqqq REFERENCE OBJECTS

	// this does not load the object or set the parent's link property, only finds the matching one
	protected void getReferenceObject(final StackItem stackItemChild) {
		final StackItem stackItemParent = stackItemChild.parent;

		boolean b = getReferenceUsingStackFKeys(stackItemChild);
		if (b) {
			return;
		}

		boolean bUsesFkey = getReferenceUsingFKeys(stackItemParent, stackItemChild);
		if (!bUsesFkey) {
			boolean bImportMatch = getReferenceUsingImportMatch(stackItemParent, stackItemChild);
			if (!bImportMatch) {
				boolean bLinkUnique = getReferenceUsingPojoLinkUnique(stackItemParent, stackItemChild);
				if (!bLinkUnique) {
					//qqqq done trying to find ??
				}
			}
		}
	}

	protected boolean getReferenceUsingStackFKeys(final StackItem stackItemChild) {
		return false; //qqqqqqqqqqqqqqqqqqqqqqqqqqqq
	}

	//qqqqqqqqqqq PojoLinkOneDelegate.getLinkFkeyPojoProperties(    ... could be null
	protected boolean getReferenceUsingFKeys(final StackItem stackItemParent, final StackItem stackItemChild) {
		Map<String, Object> hm = new HashMap<>();
		for (PojoProperty pjp : PojoLinkOneDelegate.getLinkFkeyPojoProperties(	stackItemParent.oi.getPojo(),
																				stackItemChild.li.getLowerName())) {
			String fkeyName = pjp.getName();
			OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), pjp.getPropertyPath());
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			JsonNode jn = stackItemParent.node.get(fkeyName);
			if (jn == null) {
				hm.clear();
				break;
			}
			Object objx = convert(jn, pi);
			hm.put(fkeyName, objx);
		}
		if (hm.size() == 0) {
			return false;
		}

		ArrayList<Object> al = new ArrayList();
		for (OAFkeyInfo fi : stackItemChild.li.getFkeyInfos()) {
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
		if (al.size() > 0) {
			Object[] objs = al.toArray(new Object[al.size()]);
			stackItemChild.key = new OAObjectKey(objs);
		}
		return true;
	}

	protected boolean getReferenceUsingImportMatch(final StackItem stackItemParent, final StackItem stackItemChild) {
		int pos = 0;
		String sql = null;
		Object[] values = new Object[] {};
		//qqqqqqqqq PojoLinkOneDelegate.getImportMatchPojoProperties ... could be null
		for (PojoProperty pjp : PojoLinkOneDelegate.getImportMatchPojoProperties(	stackItemParent.oi.getPojo(),
																					stackItemChild.li.getLowerName())) {
			String propertyName = pjp.getName();
			String propertyPath = pjp.getPropertyPath();

			OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), propertyPath);

			JsonNode jn = stackItemParent.node.get(propertyName);
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
				pi = stackItemParent.oi.getPropertyInfo(propertyName);
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
		if (sql == null) {
			return false;
		}

		OAObject oaObj = null;
		if (sql != null) {
			OASelect sel = new OASelect(stackItemParent.oi.getForClass(), sql, values, "");
			oaObj = sel.next();
			sel.close();

			if (oaObj == null) {
				OAFinder finder = new OAFinder();
				OAQueryFilter filter = new OAQueryFilter(stackItemParent.oi.getForClass(), sql, values);
				finder.addFilter(filter);
				oaObj = (OAObject) OAObjectCacheDelegate.find(stackItemParent.oi.getForClass(), finder);
			}
		}
		return true;
	}

	protected boolean getReferenceUsingPojoLinkUnique(final StackItem stackItemParent, final StackItem stackItemChild) {
		//qqqqqqqqqqqqqqqqqq iterator could be null
		final List<PojoProperty> al = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItemParent.oi.getPojo(),
																						stackItemChild.li.getLowerName());
		if (al == null || al.size() == 0) {
			return false;
		}

		//qqqqqqqqq only if you can get to the value for the equal property path

		String s = stackItemChild.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), s);

		OAObject equalObject = null;

		//qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq

		getObjectUsingStackLinkEqualWithUnique(stackItemParent, stackItemChild);

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

			pp = new OAPropertyPath(stackItemParent.oi.getForClass(), propertyPath);

			JsonNode jn = stackItemParent.node.get(propertyName);
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
				pi = stackItemParent.oi.getPropertyInfo(propertyName);
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

	protected StackItem getObjectFromStack(StackItem stackItem, OAPropertyPath pp) {
		if (stackItem == null) {
			return null;
		}
		if (pp == null) {
			return null;
		}
		StackItem si = stackItem;
		for (OALinkInfo li : pp.getLinkInfos()) {
			if (si.li != li) {
				return null;
			}
			si = si.parent;
			if (si == null) {
				return null;
			}
		}
		return si;
	}
}

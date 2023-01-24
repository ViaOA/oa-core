package com.viaoa.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
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
import com.viaoa.object.OAObjectImportMatchDelegate;
import com.viaoa.object.OAObjectImportMatchDelegate.ImportMatch;
import com.viaoa.object.OAObjectImportMatchDelegate.ImportMatchDetail;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.pojo.Pojo;
import com.viaoa.pojo.PojoLink;
import com.viaoa.pojo.PojoLinkFkey;
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
import com.viaoa.util.Tuple;

/*
qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq In the works
*/

/**
 * Used by OAJson to convert JSON to OAObject(s).
 * <p>
 * This supports object graphs, json from POJOs without directly matching pkeys properties.
 * <p>
 * For POJOs without direct pkey properties, it uses the following to find and update matching OAObjects
 * <ul>
 * <li>Matching unique pkey properties
 * <li>Guid match
 * <li>ImportMatch properties
 * <li>Links that have a unique property match.
 * <li>Seed objects, that are set before deserialization.
 * </ul>
 */
public class OAJacksonDeserializerNew extends JsonDeserializer<OAObject> {

	// https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/JsonDeserializer.html

	@Override
	public OAObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oaj.getReadObjectClass();

		JsonNode node = jp.getCodec().readTree(jp);

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		oaj.getStack().clear();

		OAObject root = oaj.getRoot(); // qqq need to clear qqqqqqqqqqqqqqq

		loadObject(oaj, oi, root, node);

		//qqqqqqqqq clean up

		// use the import match property values to finish populating object graph
		List<ImportMatch> alImportMatch = oaj.getImportMatchList();
		for (ImportMatch im : alImportMatch) {
			OAObjectImportMatchDelegate.process(im);
		}

		return obj;
	}

	protected OAObject getObject(final OAObjectInfo oi, final JsonNode node) {
		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oi.getForClass();

		oaj.beforeReadCallback(node);
		OAObject root = oaj.getRoot(); // qqq need to clear qqqqqqq

		OAObject obj = _getObject1(oaj, oi, node, root, extraWhereClause, extraWhereParam);
		return obj;
	}

	private void loadObject(final OAJson oaj, final OAObjectInfo oi, final OAObject oaObj, final JsonNode node) {
		OAJson.StackItem stackItem = new OAJson.StackItem();
		stackItem.oi = oi;
		stackItem.obj = oaObj;
		stackItem.node = node;
		oaj.getStack().push(stackItem);

		try {
			load(oaj, stackItem);

		} finally {
			oaj.getStack().pop();
		}
	}

	protected void load(final OAJson oaj, final StackItem stackItem) {
		if (stackItem == null) {
			return;
		}

		if (stackItem.obj == null) {
			OAObject obj = getExistingObject(stackItem.oi.getForClass(), stackItem.node);
			if (obj == null) {

			}
			stackItem.obj = obj;
		}

		//qqqqqqqqqqq code to load object

		OAObject oaObj = _getObject3(oaj, stackItem);
	}

	//qqqqqqqqqqqqq dont create new one
	//GOOD
	private boolean getExistingObject(final OAJson oaj, final StackItem stackItem) {

		boolean b = getExistingObjectUsingPKeys(oaj, stackItem);
		if (!b) {
			b = getExistingObjectUsingGuid(oaj, stackItem);
			if (!b) {
				b = getExistingObjectUsingImportMatches(oaj, stackItem);
				if (!b) {
					b = getExistingObjectUsingLinkUnique(oaj, stackItem);
					if (!b) {
						b = getExistingObjectUsingStack(oaj, stackItem);
					}
				}
				//qqqqqqqq also, might want to optimise to find exising hubs that it can search in
			}
		}
		return b;
	}

	//GOOD
	private void getReferenceObject(final OAJson oaj, final StackItem stackItemParent, final OALinkInfo liOne) {

		// STEP 1: see if the json has a node
		JsonNode node = stackItemParent.node.get(liOne.getName());

		if (node == null) {

		} else if (node.isObject()) {
			StackItem stackItem = new StackItem();
			stackItem.parent = stackItemParent;
			stackItem.oi = liOne.getToObjectInfo();
			stackItem.li = liOne;
			stackItem.node = node;

			getObject(oaj, stackItem);

			return;
		} else if (node.isTextual()) {
			String s = node.asText();
			if (s.indexOf("guid.") == 0) {
				s = s.substring(5);
				int guid = Integer.parseInt(s);
				if (oaj != null) {
					objNew = oaj.getGuidMap().get(guid);
				}
			} else {
				OAObjectKey ok = OAJson.convertJsonSinglePartIdToObjectKey(li.getToClass(), s);

				objNew = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
				if (objNew != null) {
					fromObject.setProperty(li.getName(), objNew);
				} else {
					// just save the objectKey in the object's property ??
					Object objx = OAObjectPropertyDelegate.getProperty(fromObject, li.getName(), false, true);
					if (objx instanceof OAObjectKey) {
						OAObjectPropertyDelegate.setPropertyCAS(fromObject, li.getName(), ok, objx);
					} else if (objx instanceof OAObject) {
						// need to get and replace, since it's loaded
					} else if (objx == null) {
						//  need to get and replace, since it could be loaded
					}

					// get from DS
					objNew = (OAObject) OADataSource.getObject(li.getToClass(), ok);
				}
			}
			return;
		}

		// Step 1B: use POJO to find using fkey/importMatch/linkUnique

		PojoLinkOne plo = PojoLinkOneDelegate.getPojoLinkOne(stackItemParent.oi.getPojo(), liOne.getName());
		if (plo == null) {
			return;
		}

		//qqqqqq the linkOne from nodeFrom might be an jsonObject{}, and not keys/importMatches/etc

		//qqqqq will either be json with value to object or fkey/importMatch/linkUnique qqqqqqqq

		StackItem stackItemChild = new StackItem();
		stackItemChild.parent = stackItemParent;
		stackItemChild.oi = liOne.getToObjectInfo();
		stackItemChild.li = liOne;
		stackItemChild.node = null; // uses parent.node

		boolean bUsesFkey = getReferenceKey(oaj, stackItemParent, stackItemChild);
		if (bUsesFkey) {
			if (stackItemChild.key == null) {
				//qqqqqq set to null
			} else {
				//qqqqq try to find it, or just set the parent.ref.property = ok
				//qqqqq set property
			}
		} else {
			boolean bImportMatch = getReferenceUsingImportMatch(oaj, stackItemParent, stackItemChild);

			if (!bImportMatch) {
				boolean bLinkUnique = getReferenceUsingLinkUnique(oaj, stackItemParent, stackItemChild);

				if (!bLinkUnique) {

				}
			}

			//qqqqqqqqqqqqqqqqqqq also try to use stack ??

			//qqqqqqqqqqq
			// if not found, then put in a cache to find and update later

		}
		//qqqqqqqqq also include 1to1 link that has autocreate qqqqqqqqqqqqq

	}

	//GOOD
	private boolean getReferenceUsingLinkUnique(final OAJson oaj, final StackItem stackItemParent, final StackItem stackItemChild) {

		final List<PojoProperty> al = PojoLinkOneDelegate.getLinkUniquePojoProperties(	stackItemParent.oi.getPojo(),
																						stackItemChild.li.getName());
		if (al == null || al.size() == 0) {
			return false;
		}

		//qqqqqqqqq only if you can get to the value for the equal property path

		String s = stackItemChild.li.getEqualPropertyPath();
		if (OAString.isEmpty(s)) {
			return false;
		}

		OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), s);
		OAObject equalObject = getExistingValueFromStack(oaj, stackItemChild, pp);

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

	//GOOD
	private boolean getReferenceUsingImportMatch(final OAJson oaj, final StackItem stackItemParent, final StackItem stackItemChild) {

		int pos = 0;
		String sql = null;
		Object[] values = new Object[] {};

		for (PojoProperty pjp : PojoLinkOneDelegate.getImportMatchPojoProperties(	stackItemParent.oi.getPojo(),
																					stackItemChild.li.getName())) {
			String propertyName = pjp.getName();
			String propertyPath = pjp.getPropertyPath();

			OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), propertyPath);

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
		return oaObj;
	}

	//GOOD
	private boolean getReferenceKey(final OAJson oaj, final StackItem stackItemParent, final StackItem stackItemChild) {
		Map<String, Object> hm = new HashMap<>();
		for (PojoProperty pjp : PojoLinkOneDelegate.getLinkFkeyPojoProperties(stackItemParent.oi.getPojo(), stackItemChild.li.getName())) {
			String fkeyName = pjp.getName();
			OAPropertyPath pp = new OAPropertyPath(stackItemParent.oi.getForClass(), pjp.getPropertyPath());
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			JsonNode jn = stackItemParent.node.get(pi.getLowerName());
			if (jn == null) {
				hm.clear();
				break;
			}
			Object objx = convert(jn, pi);
			hm.put(pi.getName(), objx);
		}
		if (hm.size() == 0) {
			return false;
		}

		ArrayList<Object> al = new ArrayList();
		for (OAFkeyInfo fi : stackItemChild.li.getFkeyInfos()) {
			if (fi.getFromPropertyInfo() == null) {
				continue;
			}
			String s = fi.getFromPropertyInfo().getName();
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

	private boolean getExistingObjectUsingPKeys(final OAJson oaj, final StackItem stackItem) {
		boolean bResult = false;

		boolean bIdMissing = false;
		ArrayList<Object> alKeys = new ArrayList();
		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			String propertyName = pi.getLowerName();
			if (!oaj.getUsePropertyCallback(null, propertyName)) {
				continue;
			}
			propertyName = oaj.getPropertyNameCallback(null, propertyName);

			JsonNode jn = stackItem.node.get(propertyName);

			if (jn == null) {
				bIdMissing = true;
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oaj.getPropertyValueCallback(null, pi.getLowerName(), objx);

			alKeys.add(objx);
		}

		if (!bIdMissing) {
			final Class clazz = stackItem.oi.getForClass();
			OAObjectKey objKey = new OAObjectKey(alKeys.toArray());
			OAObject objNew = (OAObject) OAObjectCacheDelegate.get(clazz, objKey);

			if (objNew == null) {
				objNew = (OAObject) OADataSource.getObject(clazz, objKey);
			}
			stackItem.obj = objNew;
			bResult = (objNew != null);
		}
		return bResult;
	}

	private boolean getExistingObjectUsingGuid(final OAJson oaj, final StackItem stackItem) {
		//qqqqqqq todo:  quid could also be placeholder for object ref, ex: "customer": "guid.1234"
		boolean bResult = false;
		JsonNode jn = stackItem.node.get("guid");
		if (jn != null) {
			int guid = jn.asInt();

			OAObject objNew = oaj.getGuidMap().get(guid);
			stackItem.obj = objNew;
			bResult = (objNew != null);
		}
		return bResult;
	}

	private boolean getExistingObjectUsingImportMatches(final OAJson oaj, final StackItem stackItem) {
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
			JsonNode jn = stackItem.node.get(pi.getName());
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

			sql += pi.getName() + " = ?";

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

		if (sql != null) {
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
			bResult = (objNew != null);
		}
		return bResult;
	}

	private boolean getExistingObjectUsingLinkUnique(final OAJson oaj, final StackItem stackItem) {
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

			OAObject obj = getExistingLinkOneObject(oaj, stackItem.oi, stackItem.node, li);
			if (obj == null) {
				continue;
			}

			Hub hub = (Hub) rli.getValue(obj);

			OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), rli.getUniqueProperty());
			OAPropertyInfo pi = pp.getEndPropertyInfo();
			if (pi == null) {
				continue; //qqqqqqqq needs to support link value also ??
			}

			JsonNode jn = stackItem.node.get(pi.getName());
			Object val = convert(jn, pi);

			if (val == null) {
				continue;
			}
			OAObject objx = (OAObject) hub.find(pi.getName(), val);
			if (objx != null) {
				stackItem.obj = objx;
				return true;
			}
		}

		//qqqqqqqqq also include 1to1 link that has autocreate qqqqqqqqqqqqq

		return bResult;
	}

	//qqqqqq what about jsonnode that is array => Hub

	private boolean getExistingObjectUsingStack(final OAJson oaj, final StackItem stackItem) {
		// qqqqqqqq combine this with previous method qqqqqqqq
		boolean bResult = false;

		//qqqqqqqqqqqqqqqqqqq
		for (PojoLink pl : stackItem.oi.getPojo().getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo == null) {
				continue;
			}
			if (plo.getPojoImportMatches().size() == 0) {
				continue;
			}

			for (PojoProperty pjp : PojoLinkOneDelegate.getImportMatchPojoProperties(plo)) {
			}
		}

		return bResult;
	}

	private void getFirstObject(final OAJson oaj, final JsonNode node, final Class clazz, final OAObject object) {
		if (node.isArray()) {
			//qqqqqqqqq
		} else if (node.isObject()) {
			StackItem stackItem = new StackItem();
			stackItem.obj = object;
			stackItem.oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
			stackItem.node = node;

			getExistingObject(oaj, stackItem);
		} else {

		}
	}

	//qqqqqqqqqqqqqqqqq

	private OAObject _getObject3(final OAJson oaj, final StackItem stackItem) {

		final Class clazz = stackItem.oi.getForClass();

		oaj.beforeReadCallback(stackItem.node);

		OAObject objNew = stackItem.obj;

		if (objNew == null) {
			objNew = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);

			for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
				if (!pi.getId()) {
					continue;
				}

				String propertyName = pi.getLowerName();
				if (!oaj.getUsePropertyCallback(null, propertyName)) {
					continue;
				}
				propertyName = oaj.getPropertyNameCallback(null, propertyName);

				JsonNode jn = stackItem.node.get(propertyName);

				if (jn == null) {
					continue;
				}

				Object objx = convert(jn, pi);

				objx = oaj.getPropertyValueCallback(null, pi.getLowerName(), objx);

				objNew.setProperty(pi.getLowerName(), objx);
			}


			if (OAThreadLocalDelegate.isLoading()) {
				OAObjectDelegate.initializeAfterLoading((OAObject) objNew, false, false);
			}
		}

		OAObjectKey objKey = objNew.getObjectKey();

		JsonNode jn = stackItem.node.get("guid");
		if (jn != null) {
			int guid = jn.asInt();
			if (oaj != null) {
				oaj.getGuidMap().put(guid, objNew);
			}
		}

		// load properties
		for (OAPropertyInfo pi : stackItem.oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}

			if (!oaj.getUsePropertyCallback(objNew, pi.getLowerName())) {
				continue;
			}

			String propertyName = oaj.getPropertyNameCallback(objNew, pi.getLowerName());

			jn = stackItem.node.get(propertyName);
			if (jn == null) {
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oaj.getPropertyValueCallback(objNew, pi.getLowerName(), objx);

			objNew.setProperty(pi.getLowerName(), objx);
		}

		// load links of type=one
		final Set<OALinkInfo> setLinkFound = new HashSet<>();

		// 1: linkOne with whole object
		//qqqqqqqq use getExisting Object from it qqqqqqqqqqqqqq
		for (final OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (!oaj.getUsePropertyCallback(objNew, li.getLowerName())) {
				continue;
			}


			String propertyName = oaj.getPropertyNameCallback(objNew, li.getLowerName());
			JsonNode nodeChild = stackItem.node.get(propertyName);

			if (nodeChild == null) {
				continue;
			}

			if (nodeChild.isNull()) {
				setLinkFound.add(li);
				objNew.setProperty(li.getLowerName(), null);
				continue;
			}

			StackItem stackItemChild = new StackItem();
			stackItemChild.parent = stackItem;
			stackItemChild.oi = li.getToObjectInfo();
			stackItemChild.li = li;
			stackItemChild.node = nodeChild;


//qqqqqqqqqqqqqqqqqqq only need objKey at this time qqqqqqqqq??
			getReferenceObject(oaj, stackItemChild, li);

			if (stack)



		}



			if (nodex != null) {
			setLinkFound.add(li);

			// get pkey props from nodex, create objkey and store it as ref in objectNew
			Map<String, Object> hm = new HashMap<>();
			for (PojoProperty pjp : PojoLinkOneDelegate.getLinkFkeyPojoProperties(stackItem.oi.getPojo(), li.getName())) {
				String fkeyName = pjp.getName();
				OAPropertyPath pp = new OAPropertyPath(stackItem.oi.getForClass(), pjp.getPropertyPath());
				OAPropertyInfo pi = pp.getEndPropertyInfo();

				jn = nodex.get(pi.getLowerName());
				if (jn == null) {
					hm.clear();
					break;
				}
				Object objx = convert(jn, pi);
				hm.put(pi.getName(), objx);
			}
			if (hm.size() > 0) {
				ArrayList<Object> al = new ArrayList();
				for (OAFkeyInfo fi : li.getFkeyInfos()) {
					if (fi.getFromPropertyInfo() == null) {
						continue;
					}
					String s = fi.getFromPropertyInfo().getName();
					Object objx = hm.get(s);
					if (objx == null) {
						al.clear();
						break;
					}
					al.add(objx);
				}
				if (al.size() > 0) {
					setLinkFound.add(li);
					Object[] objs = al.toArray(new Object[al.size()]);
					OAObjectKey okx = new OAObjectKey(objs);

					OAObject objx = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), okx);
					if (objx == null) {
						objx = (OAObject) OADataSource.getObject(li.getToClass(), okx);
						if (objx == null) {
							objx = (OAObject) OAObjectReflectDelegate.createNewObject(li.getToClass());
							// need to populate
							int i = 0;
							for (OAFkeyInfo fi : li.getFkeyInfos()) {
								String s = fi.getFromPropertyInfo().getName();
								Object val = al.get(i++);
								objx.setProperty(s, val);
							}
						}
					}
					objNew.setProperty(li.getLowerName(), objx);
				}
			}

		}


		// now, load the whole object
		for (final OALinkInfo li : stackItem.oi.getLinkInfos()) {
			if (!setLinkFound.contains(li)) {
				continue;
			}

			String propertyName = oaj.getPropertyNameCallback(objNew, li.getLowerName());
			JsonNode nodex = stackItem.node.get(propertyName);

			if (nodex == null || nodex.isNull()) {
				continue;
			}


			//qqqqqq
			// new stackItem, etc, recursively load it
		}



qqqqqqqqq
				}else

	{
		ArrayList<Object> al = new ArrayList();
		OAObjectInfo oix = li.getToObjectInfo();
		int pos = 0;
		for (OAPropertyInfo pi : oix.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			String name;
			if (li.getFkeyInfos() != null && pos < li.getFkeyInfos().size()) {
				name = li.getFkeyInfos().get(pos++).getFromPropertyInfo().getName();
			} else {
				name = li.getLowerName() + pi.getName();
			}

			jn = node.get(name);
			if (jn == null) {
				al.clear();
				break;
			}
			Object objx = convert(jn, pi);
			al.add(objx);
		}
		if (al.size() > 0) {
			setLinkFound.add(li);
			Object[] objs = al.toArray(new Object[al.size()]);
			OAObjectKey okx = new OAObjectKey(objs);

			OAObject objx = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), okx);
			if (objx == null) {
				objx = (OAObject) OADataSource.getObject(li.getToClass(), okx);
				if (objx == null) {
					objx = (OAObject) OAObjectReflectDelegate.createNewObject(li.getToClass());
					// need to populate
					int i = 0;
					for (OAPropertyInfo pi : oix.getPropertyInfos()) {
						if (!pi.getId()) {
							continue;
						}
						Object val = objs[i++];
						objx.setProperty(pi.getName(), val);
					}
				}
			}
			objNew.setProperty(li.getLowerName(), objx);
		}
	}}}

	// load any (optional) ImportMatch properties for finding OneLinks.
	//   These are "extra" properties added to Pojos that can use importMatch value(s) instead of p/fkey(s)
	for(
	final OALinkInfo li:oi.getLinkInfos())
	{
		if (li.getType() != li.TYPE_ONE) {
			continue;
		}
		if (li.getPrivateMethod()) {
			continue;
		}
		if (hsLinkFound.contains(li)) {
			continue;
		}

		if (!oaj.getUsePropertyCallback(objNew, li.getLowerName())) {
			continue;
		}

		final OAObjectInfo oix = li.getToObjectInfo();
		final String[] importMatchPropertyNames = oix.getImportMatchPropertyNames();

		if (importMatchPropertyNames == null || importMatchPropertyNames.length == 0) {
			continue;
		}

		if (li.getToClass().equals(oi.getForClass())) {
			continue;
		}

		hsLinkFound.add(li);
		final Map<String, Tuple<OAPropertyPath, Object>> hmNameValue = new HashMap<>();

		boolean bIsNull = false;
		int pos = 0;
		for (final String propertyName : importMatchPropertyNames) {
			String propertyPath = oix.getImportMatchPropertyPaths()[pos];

			OAPropertyPath pp;
			if (OAString.isEmpty(propertyPath)) {
				pp = new OAPropertyPath(li.getToClass(), propertyName);
			} else {
				pp = new OAPropertyPath(li.getToClass(), propertyPath);
			}
			OAPropertyInfo pi = pp.getEndPropertyInfo();

			jn = node.get(propertyName);
			if (jn != null) {
				if (jn.isNull()) {
					bIsNull = true;
				} else {
					Object val = convert(jn, pi);
					hmNameValue.put(propertyName, new Tuple(pp, val));
				}
			}
			pos++;
		}

		if (bIsNull) {
			objNew.setProperty(li.getLowerName(), null);
			continue;
		}

		if (hmNameValue.size() == 0) {
			continue;
		}

		// see if oaObjKey can be created.  This is only when the linkTo's pkey prop values are included.
		//   otherwise, the other values could be importMatch values that can be used to find the link object (at later time)

		final String[] keyProps = li.getToObjectInfo().getKeyProperties();
		final List<Object> alKey = new ArrayList<>();
		for (String keyProp : keyProps) {
			OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(keyProp);
			for (String propertyName : importMatchPropertyNames) {
				Tuple<OAPropertyPath, Object> t = hmNameValue.get(propertyName);
				if (t == null || t.b == null) {
					continue;
				}

				OAPropertyPath pp = t.a;
				Object val = t.b;

				OALinkInfo[] lis = pp.getLinkInfos();
				if (lis != null && lis.length == 1 && pp.getEndPropertyInfo() == pi) {
					alKey.add(val);
				}
			}
		}
		if (alKey.size() == keyProps.length) {
			Object[] objs = alKey.toArray(new Object[alKey.size()]);
			OAObjectKey okx = new OAObjectKey(objs);

			OAObject objx = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), okx);
			if (objx == null) {
				objx = (OAObject) OADataSource.getObject(li.getToClass(), okx);
				if (objx == null) {
					objx = (OAObject) OAObjectReflectDelegate.createNewObject(li.getToClass());
					// need to populate
					int i = 0;
					for (String keyProp : keyProps) {
						Object val = alKey.get(i++);
						objx.setProperty(keyProp, val);
					}

				}
			}
			objNew.setProperty(li.getLowerName(), objx);

		} else {
			ImportMatch imi = new ImportMatch();
			imi.fromObject = objNew;
			imi.liTo = li;

			boolean bValid = false;
			pos = 0;

			for (String propertyName : importMatchPropertyNames) {
				String propertyPath = oix.getImportMatchPropertyPaths()[pos++];

				final OAPropertyPath pp = new OAPropertyPath(oix.getForClass(),
						OAString.isNotEmpty(propertyPath) ? propertyPath : propertyName);

				Tuple<OAPropertyPath, Object> t = hmNameValue.get(propertyName);
				if (t == null) {
					bValid = false;
					break;
				}
				bValid = true;

				ImportMatchDetail imd = new ImportMatchDetail();
				imi.importMatchDetails.add(imd);
				imd.propertyName = propertyName;
				imd.value = t.b;
				imd.propertyPath = t.a.getPropertyPath();
			}

			if (bValid) {
				alImportMatch.add(imi);
			}
		}
	}

	// load links of type=many
	for(
	OALinkInfo li:oi.getLinkInfos())
	{
		if (li.getType() != li.TYPE_MANY) {
			continue;
		}
		if (li.getPrivateMethod()) {
			continue;
		}

		if (!oaj.getUsePropertyCallback(objNew, li.getLowerName())) {
			continue;
		}

		String propertyName = oaj.getPropertyNameCallback(objNew, li.getLowerName());
		JsonNode nodex = node.get(propertyName);
		if (nodex instanceof ArrayNode) {
			Hub<OAObject> hub = (Hub<OAObject>) li.getValue(objNew);
			ArrayNode nodeArray = (ArrayNode) nodex;

			List<OAObject> alNew = new ArrayList();
			int x = nodeArray.size();
			for (int i = 0; i < x; i++) {
				nodex = nodeArray.get(i);

				OAObject objx = getLinkObject(objNew, li, nodex);
				alNew.add(objx);
			}

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
	}oaj.afterReadCallback(node,objNew);

	return objNew;
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

	protected OAObject getLinkObject(OAObject fromObject, OALinkInfo li, JsonNode nodeForLinkProperty) {
		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();

		OAObject objNew = null;
		if (nodeForLinkProperty.isObject()) {
			if (li.getAutoCreateNew() && li.getType() == OALinkInfo.ONE) {
				// was: if (li.getOwner() && li.getAutoCreateNew() && li.getType() == OALinkInfo.ONE) {
				objNew = (OAObject) li.getValue(fromObject);
				loadObject(objNew, nodeForLinkProperty);
			} else {
				String extraWhereClause = null;
				Object extraWhereParam = null;

				boolean b = li.getOwner();
				if (!b) {
					final String uniqueName = li.getUniqueProperty();
					if (OAString.isNotEmpty(uniqueName)) {
						OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(li.getToClass());
						OAPropertyInfo pi = oi.getPropertyInfo(uniqueName);
						if (pi != null) {
							b = pi.getImportMatch();
						} else {
							OALinkInfo lix = oi.getLinkInfo(uniqueName);
							if (lix != null) {
								b = lix.getImportMatch();
							}
						}
					}
				}

				if (b) {
					OALinkInfo rli = li.getReverseLinkInfo();
					extraWhereClause = rli.getName() + " = ?";
					extraWhereParam = fromObject;

				}
				objNew = getObject(li.getToObjectInfo(), nodeForLinkProperty, extraWhereClause, extraWhereParam);
			}
		} else if (nodeForLinkProperty.isNull()) {
			// no-op
		} else if (nodeForLinkProperty.isNumber()) {
			// single part id
			OAObjectKey ok = new OAObjectKey(nodeForLinkProperty.asLong());
			objNew = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
			if (objNew == null) {
				objNew = (OAObject) OADataSource.getObject(li.getToClass(), ok);
			}
		} else if (nodeForLinkProperty.isTextual()) {
			String s = nodeForLinkProperty.asText();
			if (s.indexOf("guid.") == 0) {
				s = s.substring(5);
				int guid = Integer.parseInt(s);
				if (oaj != null) {
					objNew = oaj.getGuidMap().get(guid);
				}
			} else {
				OAObjectKey ok = OAJson.convertJsonSinglePartIdToObjectKey(li.getToClass(), s);

				objNew = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
				if (objNew != null) {
					fromObject.setProperty(li.getName(), objNew);
				} else {
					// just save the objectKey in the object's property ??
					Object objx = OAObjectPropertyDelegate.getProperty(fromObject, li.getName(), false, true);
					if (objx instanceof OAObjectKey) {
						OAObjectPropertyDelegate.setPropertyCAS(fromObject, li.getName(), ok, objx);
					} else if (objx instanceof OAObject) {
						// need to get and replace, since it's loaded
					} else if (objx == null) {
						//  need to get and replace, since it could be loaded
					}

					// get from DS
					objNew = (OAObject) OADataSource.getObject(li.getToClass(), ok);
				}
			}
		}
		return objNew;
	}

}

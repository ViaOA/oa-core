package com.viaoa.json.jackson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.json.OAJson.StackItem;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.pojo.Pojo;
import com.viaoa.pojo.PojoImportMatch;
import com.viaoa.pojo.PojoLink;
import com.viaoa.pojo.PojoLinkFkey;
import com.viaoa.pojo.PojoLinkOne;
import com.viaoa.pojo.PojoLinkOneReference;
import com.viaoa.pojo.PojoLinkUnique;
import com.viaoa.pojo.PojoProperty;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

//qqqqqqqqqqqqqqqqqqqqqqqqqqqqq RENAME this qqqqqqqqqqqqqqqqqqqqqqqq

//qqqqq needs a way to know that it's going to be read from a Pojo
//     qqqqqqq and not send references instead of objects
//              >> need to have pojo use the jsonObjId annoations for importMatch props ??

/**
 * Used by OAJson to convert OAObject(s) & Hub to JSON. Includes mapping to work with POJO classes.
 * <p>
 */
public class OAJacksonSerializerPojo extends JsonSerializer<OAObject> {

	@Override
	public void serialize(final OAObject value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();

		final OAObject oaObj = (OAObject) value;

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());

		gen.writeStartObject();

		boolean b = (oaj.getStackItem() == null);
		if (b) {
			StackItem stackItem = new StackItem();
			stackItem.parent = null;
			stackItem.oi = oi;
			stackItem.li = null;
			stackItem.obj = value;
			oaj.setStackItem(stackItem);

		}
		try {
			_serialize(oaj, oaObj, oi, value, gen, serializers);
		} finally {
			if (b) {
				oaj.setStackItem(null);
			}
		}
	}

	protected void _serialize(final OAJson oaj, final OAObject oaObj, final OAObjectInfo oi, final OAObject value, final JsonGenerator gen,
			final SerializerProvider serializers) throws IOException {

		// write id props
		boolean bNullId = true;
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			if (oaj.getWriteAsPojo() && pi.getAutoAssign()) {
				if (OAString.isNotEmpty(oi.getImportMatchPropertyNames())) {
					boolean b = false;
					for (String pp : oi.getImportMatchPropertyPaths()) {
						OAPropertyPath ppx = new OAPropertyPath(oi.getForClass(), pp);
						if (pp.indexOf('.') < 0 && pp.equalsIgnoreCase(pi.getName())) {
							b = true;
							break;
						}
					}
					if (!b) {
						continue;
					}
				}
			}

			String propertyName = pi.getLowerName();
			Object objx = pi.getValue(oaObj);

			if (!oaj.getUsePropertyCallback(oaObj, propertyName)) {
				continue;
			}
			propertyName = oaj.getPropertyNameCallback(oaObj, propertyName);
			objx = oaj.getPropertyValueCallback(oaObj, propertyName, objx);

			if (objx == null) {
				gen.writeNullField(pi.getLowerName());
			} else {
				writeProperty(pi, gen, oaObj);
				bNullId = false;
			}
		}

		if (bNullId && !oaj.getWriteAsPojo()) {
			gen.writeNumberField("guid", oaObj.getGuid());
		}

		// write (non-id) props
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}
			if (pi.getIsFkeyOnly() && oaj.getWriteAsPojo()) {
				continue;
			}
			writeProperty(pi, gen, oaObj);
		}

		if (oaj.getWriteAsPojo()) {
			Pojo pojo = oi.getPojo();
			writeExtraPojoProperties(oaj, oi, oaObj, gen);
		}

		final ArrayList<String> alPropertyPaths = oaj == null ? null : oaj.getPropertyPaths();
		final boolean bIncludeOwned = oaj == null ? true : oaj.getIncludeOwned();

		// write one references
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (li.getCalculated()) {
				continue;
			}

			/*
			if (oaj.getWriteAsPojo()) {
				//qqqq make this dynamic and use propertyPaths to include
				if (!li.getOwner()) {
					continue;
				}
			}
			*/

			String propertyName = li.getLowerName();
			if (!oaj.getUsePropertyCallback(oaObj, propertyName)) {
				continue;
			}

			boolean bSerialized = false;

			if ((oaj != null && oaj.getIncludeAll()) || shouldInclude(oaj, li, bIncludeOwned, alPropertyPaths)) {
				propertyName = oaj.getPropertyNameCallback(oaObj, propertyName);
				StackItem si = new StackItem();
				si.parent = oaj.getStackItem();
				si.li = li;
				si.obj = oaObj;
				oaj.setStackItem(si);

				try {
					OAObject objx = (OAObject) li.getValue(oaObj);
					objx = (OAObject) oaj.getPropertyValueCallback(oaObj, propertyName, objx);

					if (objx == null) {
						gen.writeNullField(propertyName);
						bSerialized = true;
					} else {
						if (oaj != null && !oaj.getCascade().wasCascaded(objx, true)) {
							bSerialized = true;
							gen.writeObjectField(li.getLowerName(), objx);
						} else {
							bSerialized = false;
						}
					}

				} finally {
					oaj.setStackItem(si.parent);
				}
			}

			if (!bSerialized) {
				OAObjectKey key = null;
				Object obj = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);

				obj = oaj.getPropertyValueCallback(oaObj, li.getLowerName(), obj);

				if (obj instanceof OAObject) {
					key = ((OAObject) obj).getObjectKey();
				} else if (obj instanceof OAObjectKey) {
					key = (OAObjectKey) obj;
				}

				if (key == null) {
					gen.writeNullField(propertyName);
				} else {
					String id = OAJson.convertObjectKeyToJsonSinglePartId(key);

					if (id.indexOf('-') >= 0 || id.indexOf("guid.") == 0) {
						gen.writeStringField(li.getLowerName(), id);
					} else {
						if (OAString.isNumber(id)) {
							gen.writeNumberField(li.getLowerName(), OAConv.toLong(id));
						} else {
							gen.writeStringField(li.getLowerName(), id);
						}
					}
				}
			}
		}

		// write many references
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_MANY) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (li.getCalculated()) {
				continue;
			}

			String propertyName = li.getLowerName();
			if (!oaj.getUsePropertyCallback(oaObj, propertyName)) {
				continue;
			}

			if (oaj.getWriteAsPojo()) {
				if (!li.getOwner()) {
					continue;
				}
			}

			// only send owned objects for the root object(s)
			//    also include any owned auto-created linkOne owned links
			boolean bx = bIncludeOwned;
			if (bx) {
				OAJson.StackItem si = oaj.getStackItem();
				bx = si == null || si.parent == null;

				//qqqqqqq create a rule & unit test for this
				if (!bx && si.li.isOne2One() && si.li.getOwner() && si.li.getAutoCreateNew()) {
					bx = true;
				}
			}

			if ((oaj != null && oaj.getIncludeAll()) || shouldInclude(oaj, li, bx, alPropertyPaths)) {
				StackItem si = new StackItem();
				si.parent = oaj.getStackItem();
				si.li = li;
				oaj.setStackItem(si);
				try {
					Hub hub = (Hub) li.getValue(oaObj);

					gen.writeArrayFieldStart(propertyName);

					for (OAObject objx : (Hub<OAObject>) li.getValue(oaObj)) {
						si.obj = objx;

						// check cascade to see if its been sent ...if so, then only output key (/guid)
						// note:  deserializer needs to check array values for object, string, number to "know" how to get it

						if (oaj != null && !oaj.getCascade().wasCascaded(objx, true)) {
							gen.writeObject(objx);
						} else {

							OAObjectKey key = objx.getObjectKey();
							String id = OAJson.convertObjectKeyToJsonSinglePartId(key);

							if (id.indexOf('-') >= 0 || id.indexOf("guid.") == 0) {
								gen.writeString(id);
							} else {
								gen.writeNumber(OAConv.toLong(id));
							}
						}
					}

				} finally {
					oaj.setStackItem(si.parent);
					gen.writeEndArray();
				}
			} else {
				// if hub is loaded and it is empty, then send empty array (for convenience only)
				Object obj = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
				if (obj instanceof Hub) {
					if (((Hub) obj).isEmpty()) {
						gen.writeArrayFieldStart(li.getLowerName());
						gen.writeEndArray();
					}
				}

			}
		}

		// todo: want to add any calcs ??

		gen.writeEndObject();
	}

	/**
	 * Include other properties that could be needed for Pojo. Pojo classes dont always have a pkey property, but uniqueness can be found
	 * using other data. Uses pojo information to determine other fkey'like values to include. This includes importMatch, links with unique
	 * property, and fkeys from another link.
	 * <p>
	 * see OABuilder model OABuilderPojo
	 */
	protected void writeExtraPojoProperties(final OAJson oaj, final OAObjectInfo oi, final OAObject oaObj, final JsonGenerator gen)
			throws IOException {
		Pojo pojo = oi.getPojo();
		for (PojoLink pl : pojo.getPojoLinks()) {
			PojoLinkOne plo = pl.getPojoLinkOne();
			if (plo != null) {
				writePojoLinkOne(oaj, oi, oaObj, gen, plo);
			}
		}
	}

	protected void writePojoLinkOne(final OAJson oaj, final OAObjectInfo oi, final OAObject oaObj, final JsonGenerator gen,
			final PojoLinkOne plo) throws IOException {

		// fkeys
		for (PojoLinkFkey plf : plo.getPojoLinkFkeys()) {
			PojoProperty pjp = plf.getPojoProperty();
			writePojoProperty(oaj, oi, oaObj, gen, pjp);
		}

		// importMatches
		for (PojoImportMatch pim : plo.getPojoImportMatches()) {
			PojoProperty pjp = pim.getPojoProperty();
			if (pim != null) {
				writePojoProperty(oaj, oi, oaObj, gen, pjp);
			}

			PojoLinkOneReference plor = pim.getPojoLinkOneReference();
			if (plor == null) {
				continue;
			}
			PojoLinkOne plox = plor.getPojoLinkOne();
			writePojoLinkOne(oaj, oi, oaObj, gen, plo);
		}

		// link with unique property
		PojoLinkUnique plu = plo.getPojoLinkUnique();
		if (plu != null) {
			PojoProperty pjp = plu.getPojoProperty();
			if (pjp != null) {
				writePojoProperty(oaj, oi, oaObj, gen, pjp);
			}

			PojoLinkOneReference plor = plu.getPojoLinkOneReference();
			if (plor != null) {
				PojoLinkOne plox = plor.getPojoLinkOne();
				writePojoLinkOne(oaj, oi, oaObj, gen, plox);
			}
		}
	}

	protected void writePojoProperty(final OAJson oaj, final OAObjectInfo oi, final OAObject oaObj, final JsonGenerator gen,
			final PojoProperty pjp) throws IOException {
		String propertyName = pjp.getName();
		String pp = pjp.getPropertyPath();
		OAPropertyPath ppx = new OAPropertyPath(oi.getForClass(), pp);
		OAPropertyInfo pi = ppx.getEndPropertyInfo();

		Object objx = oaObj.getProperty(pp);

		propertyName = oaj.getPropertyNameCallback(oaObj, propertyName);
		objx = oaj.getPropertyValueCallback(oaObj, propertyName, objx);

		if (objx == null) {
			gen.writeNullField(propertyName);
		} else {
			writeProperty(pi, propertyName, objx, gen, oaObj);
		}
	}

	protected boolean shouldInclude(OAJson oaj, OALinkInfo li, boolean bIncludeOwned, ArrayList<String> alPropertyPaths) {
		if (li == null) {
			return false;
		}
		if (bIncludeOwned && (li.getOwner() || li.getAutoCreateNew())) {
			return true;
		}
		if (alPropertyPaths == null) {
			return false;
		}

		String cpp = oaj.getCurrentPropertyPath();

		cpp = OAString.append(cpp, li.getName(), ".");
		cpp = cpp.toLowerCase();

		for (String pp : alPropertyPaths) {
			if (pp.toLowerCase().indexOf(cpp) == 0) {
				return true;
			}
		}

		return false;
	}

	protected void writeProperty(OAPropertyInfo pi, JsonGenerator gen, OAObject oaObj) throws IOException {
		writeProperty(pi, pi.getLowerName(), null, gen, oaObj);
	}

	protected void writeProperty(OAPropertyInfo pi, final String lowerName, Object value, JsonGenerator gen, OAObject oaObj)
			throws IOException {
		boolean bCheckValue = (value == null);

		if (bCheckValue) {
			value = pi.getValue(oaObj);
			if (pi.getIsPrimitive() && pi.getTrackPrimitiveNull() && OAObjectReflectDelegate.getPrimitiveNull(oaObj, lowerName)) {
				value = null;
			}
		}
		if (value == null) {
			gen.writeNullField(lowerName);
			return;
		}

		if (value != null && !(value instanceof String) && OAConverter.getConverter(value.getClass()) == null) {
			gen.writeNullField(lowerName);
			return;
		}

		if (pi.isNameValue() && (value instanceof Integer)) {
			value = (String) pi.getNameValues().get((Integer) value);
		}

		if (value instanceof String) {
			if (pi.isJson()) {
				gen.writeFieldName(lowerName);
				gen.writeRawValue((String) value);
			} else {
				gen.writeStringField(lowerName, (String) value);
			}
		} else if (value instanceof Boolean) {
			gen.writeBooleanField(lowerName, (boolean) value);
		} else if (value instanceof BigDecimal) {
			gen.writeNumberField(lowerName, (BigDecimal) value);
		} else if (value instanceof Double) {
			BigDecimal bd = OAConv.toBigDecimal((Double) value, pi.getDecimalPlaces());
			gen.writeNumberField(lowerName, bd);
		} else if (value instanceof Float) {
			BigDecimal bd = OAConv.toBigDecimal((Float) value, pi.getDecimalPlaces());
			gen.writeNumberField(lowerName, bd);
		} else if (value instanceof Long) {
			gen.writeNumberField(lowerName, (Long) value);
		} else if (value instanceof Integer) {
			gen.writeNumberField(lowerName, (Integer) value);
		} else if (value instanceof Short) {
			gen.writeNumberField(lowerName, (Short) value);
		} else if (value instanceof OADate) {
			String fmt = pi.getFormat();
			if (OAString.isEmpty(fmt)) {
				fmt = "yyyy-MM-dd";
			}
			String result = ((OADate) value).toString(fmt);
			gen.writeStringField(lowerName, result);
		} else if (value instanceof OATime) {
			String fmt = pi.getFormat();
			if (OAString.isEmpty(fmt)) {
				fmt = "HH:mm:ss";
			}
			String result = ((OATime) value).toString(fmt);
			gen.writeStringField(lowerName, result);
		} else if (value instanceof OADateTime) {
			String fmt = pi.getFormat();
			if (OAString.isEmpty(fmt)) {
				fmt = "yyyy-MM-dd'T'HH:mm:ss";
			}
			String result = ((OADateTime) value).toString(fmt); // "2020-12-26T19:21:09"
			gen.writeStringField(lowerName, result);
		} else if (value instanceof byte[]) {
			gen.writeBinaryField(lowerName, (byte[]) value);
		} else {
			String result = OAConv.toString(value);
			gen.writeStringField(lowerName, result);
		}
	}

}

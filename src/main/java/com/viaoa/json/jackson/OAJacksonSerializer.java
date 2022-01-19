package com.viaoa.json.jackson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Stack;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

/**
 * Used by OAJackson to convert OAObject(s) & Hub to JSON.
 */
public class OAJacksonSerializer extends JsonSerializer<OAObject> {

	/*
		see: https://spin.atomicobject.com/2016/07/01/custom-serializer-jackson/
	*/

	// stack of link objects from marshalling, that can be used to know the propertyPath
	private final Stack<OALinkInfo> stackLinkInfo = new Stack<>();

	public String getCurrentPropertyPath() {
		String pp = "";
		if (stackLinkInfo != null) {
			OALinkInfo liPrev = null;
			for (OALinkInfo li : stackLinkInfo) {
				if (li == liPrev) {
					continue; // recursive
				}
				if (pp.length() > 0) {
					pp += ".";
				}
				pp += li.getLowerName();
				liPrev = li;
			}
		}
		return pp;
	}

	@Override
	public void serialize(OAObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();

		final OAObject oaObj = (OAObject) value;

		if (oaj != null) {
			oaj.getCascade().wasCascaded(oaObj, true);
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj.getClass());

		gen.writeStartObject();

		// write id props
		boolean bNullId = true;
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}
			Object objx = pi.getValue(oaObj);

			if (objx == null) {
				gen.writeNullField(pi.getLowerName());
			} else {
				writeProperty(pi, gen, oaObj);
				bNullId = false;
			}
		}

		if (bNullId) {
			gen.writeNumberField("guid", oaObj.getGuid());
		}

		// write (non-id) props
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}
			writeProperty(pi, gen, oaObj);
		}

		final ArrayList<String> alPropertyPaths = oaj == null ? null : oaj.getPropertyPaths();
		final boolean bIncludeOwned = oaj == null ? true : oaj.getIncludeOwned();

		getCurrentPropertyPath();

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

			boolean bSerialized = false;

			if ((oaj != null && oaj.getIncludeAll()) || shouldInclude(li, bIncludeOwned, alPropertyPaths)) {
				try {
					stackLinkInfo.push(li);

					OAObject objx = (OAObject) li.getValue(oaObj);
					if (objx == null) {
						gen.writeNullField(li.getLowerName());
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
					stackLinkInfo.pop();
				}
			}

			if (!bSerialized) {
				OAObjectKey key = null;
				Object obj = OAObjectPropertyDelegate.getProperty(oaObj, li.getName(), false, true);
				if (obj instanceof OAObject) {
					key = ((OAObject) obj).getObjectKey();
				} else if (obj instanceof OAObjectKey) {
					key = (OAObjectKey) obj;
				}

				if (key == null) {
					gen.writeNullField(li.getLowerName());
				} else {
					String id = OAJson.convertObjectKeyToJsonSinglePartId(key);

					if (id.indexOf('-') >= 0 || id.indexOf("guid.") == 0) {
						gen.writeStringField(li.getLowerName(), id);
					} else {
						gen.writeNumberField(li.getLowerName(), OAConv.toLong(id));
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

			if ((oaj != null && oaj.getIncludeAll()) || shouldInclude(li, bIncludeOwned, alPropertyPaths)) {
				try {
					stackLinkInfo.push(li);

					Hub hub = (Hub) li.getValue(oaObj);

					gen.writeArrayFieldStart(li.getLowerName());

					for (OAObject objx : (Hub<OAObject>) li.getValue(oaObj)) {

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
					stackLinkInfo.pop();
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

	protected boolean shouldInclude(OALinkInfo li, boolean bIncludeOwned, ArrayList<String> alPropertyPaths) {
		if (li == null) {
			return false;
		}
		if (bIncludeOwned && li.getOwner()) {
			return true;
		}
		if (alPropertyPaths == null) {
			return false;
		}

		String cpp = getCurrentPropertyPath();
		if (cpp == null) {
			return false;
		}

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
		Object value = pi.getValue(oaObj);

		final String lowerName = pi.getLowerName();

		if (pi.getIsPrimitive() && pi.getTrackPrimitiveNull() && OAObjectReflectDelegate.getPrimitiveNull(oaObj, lowerName)) {
			value = null;
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
			gen.writeStringField(lowerName, (String) value);
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
			String result = ((OADate) value).toString("yyyy-MM-dd");
			gen.writeStringField(lowerName, result);
		} else if (value instanceof OATime) {
			String result = ((OATime) value).toString("HH:mm:ss");
			gen.writeStringField(lowerName, result);
		} else if (value instanceof OADateTime) {
			String result = ((OADateTime) value).toString("yyyy-MM-dd'T'HH:mm:ss"); // "2020-12-26T19:21:09"
			gen.writeStringField(lowerName, result);
		} else if (value instanceof byte[]) {
			gen.writeBinaryField(lowerName, (byte[]) value);
		} else {
			String result = OAConv.toString(value);
			gen.writeStringField(lowerName, result);
		}
	}
}

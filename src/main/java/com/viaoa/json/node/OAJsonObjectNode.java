package com.viaoa.json.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.viaoa.json.OAJson;
import com.viaoa.json.io.JsonOutputStream;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

/**
 * Represents a json object.
 *
 * @author vvia
 */
public class OAJsonObjectNode extends OAJsonRootNode {
	private Map<String, OAJsonNode> map = new HashMap<>();
	private ArrayList<String> al = new ArrayList<>();

	@Override
	public ArrayList<String> getChildrenPropertyNames() {
		return al;
	}

	protected void toJson(final JsonOutputStream jos) {
		jos.append("{");
		boolean bFirst = true;
		for (String key : al) {
			OAJsonNode nodex = map.get(key);
			if (bFirst) {
				bFirst = false;
				jos.endLine();
				jos.addIndent();
			} else {
				jos.append(",");
				if (!jos.endLine()) {
					jos.append(" ");
				}
			}

			jos.startLine();
			jos.append("\"" + key + "\":");
			jos.addSpace();

			nodex.toJson(jos);
		}
		if (bFirst) {
		} else {
			jos.endLine();
			jos.subtractIndent();
			jos.startLine();
		}
		jos.append("}");
	}

	@Override
	public OAJsonNode get(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return null;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			return super.get(propertyName);
		}
		return map.get(propertyName);
	}

	public OADate getDate(String propertyName) {
		String s = getString(propertyName);
		if (OAString.isNotEmpty(s)) {
			OADate date = new OADate(s, OADate.JsonFormat);
			return date;
		}
		return null;
	}

	public OADateTime getDateTime(String propertyName) {
		String s = getString(propertyName);
		if (OAString.isNotEmpty(s)) {
			OADateTime dateTime = new OADateTime(s, OADateTime.JsonFormat);
			return dateTime;
		}
		return null;
	}

	public OADateTime getDateTimeTZ(String propertyName) {
		String s = getString(propertyName);
		if (OAString.isNotEmpty(s)) {
			OADateTime dateTime = new OADateTime(s, OADateTime.JsonFormatTZ);
			return dateTime;
		}
		return null;
	}

	@Override
	public String getString(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return null;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			return super.getString(propertyName);
		}
		OAJsonNode node = map.get(propertyName);
		if (node instanceof OAJsonStringNode) {
			return ((OAJsonStringNode) node).value;
		}
		return null;
	}

	@Override
	public Number getNumber(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return null;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			return super.getNumber(propertyName);
		}
		OAJsonNode node = map.get(propertyName);
		if (node instanceof OAJsonNumberNode) {
			return ((OAJsonNumberNode) node).value;
		}
		return null;
	}

	public int getInt(String propertyName) {
		Number num = getNumber(propertyName);
		if (num == null) {
			return -1;
		}
		return num.intValue();
	}

	public long getLong(String propertyName) {
		Number num = getNumber(propertyName);
		if (num == null) {
			return -1;
		}
		return num.longValue();
	}

	public double getDouble(String propertyName) {
		Number num = getNumber(propertyName);
		if (num == null) {
			return -1;
		}
		return num.doubleValue();
	}

	@Override
	public Boolean getBoolean(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return null;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			return super.getBoolean(propertyName);
		}
		OAJsonNode node = map.get(propertyName);
		if (node instanceof OAJsonBooleanNode) {
			return ((OAJsonBooleanNode) node).value;
		}
		return null;
	}

	@Override
	public OAJsonObjectNode getObject(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return null;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			return super.getObject(propertyName);
		}
		OAJsonNode node = map.get(propertyName);
		if (node instanceof OAJsonObjectNode) {
			return (OAJsonObjectNode) node;
		}
		return null;
	}

	@Override
	public OAJsonArrayNode getArray(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return null;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			return super.getArray(propertyName);
		}
		OAJsonNode node = map.get(propertyName);
		if (node instanceof OAJsonArrayNode) {
			return (OAJsonArrayNode) node;
		}
		return null;
	}

	@Override
	public void set(final String propertyName, String value) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.set(propertyName, value);
			return;
		}
		if (value != null) {
			value = OAString.escape(value);
		}
		set(propertyName, new OAJsonStringNode(value));
	}

	@Override
	public void set(final String propertyName, Number value) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.set(propertyName, value);
			return;
		}
		set(propertyName, new OAJsonNumberNode(value));
	}

	@Override
	public void set(final String propertyName, Boolean value) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.set(propertyName, value);
			return;
		}
		set(propertyName, new OAJsonBooleanNode(value));
	}

	@Override
	public void set(final String propertyName, OADate date) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.set(propertyName, date);
			return;
		}
		if (date == null) {
			setNull(propertyName);
		} else {
			String value = date.toString(date.JsonFormat);
			set(propertyName, new OAJsonStringNode(value));
		}
	}

	@Override
	public void set(final String propertyName, OADateTime dateTime) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.set(propertyName, dateTime);
			return;
		}
		if (dateTime == null) {
			setNull(propertyName);
		} else {
			String value = dateTime.toString(dateTime.JsonFormat);
			set(propertyName, new OAJsonStringNode(value));
		}
	}

	@Override
	public void setNull(final String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.setNull(propertyName);
			return;
		}
		set(propertyName, new OAJsonNullNode());
	}

	@Override
	public void setJson(final String propertyName, String json) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.setJson(propertyName, json);
			return;
		}
		OAJson oaJson = new OAJson();
		OAJsonNode node = oaJson.load(json);
		set(propertyName, node);
	}

	@Override
	public void set(String propertyName, OAJsonNode node) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.set(propertyName, node);
			return;
		}
		Object objx = map.put(propertyName, node);
		if (objx != null) {
			al.remove(propertyName);
		}
		al.add(propertyName);
		// node.name = propertyName;
	}

	@Override
	public void remove(String propertyName) {
		if (OAString.isEmpty(propertyName)) {
			return;
		}
		if (propertyName.indexOf('.') >= 0 || propertyName.indexOf('[') >= 0) {
			super.remove(propertyName);
			return;
		}
		if (propertyName == null) {
			return;
		}
		al.remove(propertyName);
		map.remove(propertyName);
	}
}

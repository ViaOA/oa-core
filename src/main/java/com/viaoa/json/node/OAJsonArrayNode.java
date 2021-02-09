package com.viaoa.json.node;

import java.util.ArrayList;

import com.viaoa.json.io.JsonOutputStream;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

/**
 * Represents a json array, which can be any json data types.
 *
 * @author vvia
 */
public class OAJsonArrayNode extends OAJsonRootNode {
	private ArrayList<OAJsonNode> al = new ArrayList<>();

	public ArrayList<OAJsonNode> getArray() {
		return al;
	}

	@Override
	public ArrayList<String> getChildrenPropertyNames() {
		return null;
	}

	public OAJsonNode get(int pos) {
		if (pos >= al.size()) {
			return null;
		}
		return al.get(pos);
	}

	public OAJsonObjectNode getObject(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonObjectNode) {
			return (OAJsonObjectNode) node;
		}
		return null;
	}

	public String getString(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonStringNode) {
			return ((OAJsonStringNode) node).value;
		}
		return null;
	}

	public boolean getBoolean(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonBooleanNode) {
			return ((OAJsonBooleanNode) node).value;
		}
		return false;
	}

	public Number getNumber(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonNumberNode) {
			return ((OAJsonNumberNode) node).value;
		}
		return null;
	}

	public boolean isNull(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonNullNode) {
			return true;
		}
		return false;
	}

	public OADate getDate(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonStringNode) {
			OADate date = new OADate(((OAJsonStringNode) node).value, OADate.JsonFormat);
			return date;
		}
		return null;
	}

	public OADateTime getDateTime(int pos) {
		OAJsonNode node = get(pos);
		if (node instanceof OAJsonStringNode) {
			OADateTime dateTime = new OADate(((OAJsonStringNode) node).value, OADateTime.JsonFormat);
			return dateTime;
		}
		return null;
	}

	public void remove(OAJsonNode node) {
		al.remove(node);
	}

	public void remove(int pos) {
		if (pos < al.size()) {
			al.remove(pos);
		}
	}

	public int size() {
		return al.size();
	}

	public int getSize() {
		return al.size();
	}

	public void insert(int pos, OAJsonNode node) {
		int x = al.size();
		if (pos > x) {
			// pad array
			for (int j = x; j < pos; j++) {
				al.add(new OAJsonNullNode());
			}
			al.add(node);
		} else {
			al.add(pos, node);
		}
	}

	protected void toJson(JsonOutputStream jos) {
		jos.append("[");
		boolean bFirst = true;

		for (OAJsonNode nodex : al) {
			if (bFirst) {
				jos.endLine();
				jos.addIndent();
				jos.startLine();
				bFirst = false;
			} else {
				jos.append(",");
				if (jos.endLine()) {
					jos.startLine();
				} else {
					jos.append(" ");
				}
			}
			nodex.toJson(jos);
		}
		if (!bFirst) {
			jos.endLine();
			jos.subtractIndent();
			jos.startLine();
		}
		jos.append("]");
	}

	public void add(OAJsonNode node) {
		al.add(node);
	}

	public void add(OAJsonRootNode node) {
		al.add(node);
	}
}

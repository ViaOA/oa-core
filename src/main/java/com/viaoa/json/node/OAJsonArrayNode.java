package com.viaoa.json.node;

import java.util.ArrayList;

import com.viaoa.json.io.JsonOutputStream;

public class OAJsonArrayNode extends OAJsonRootNode {
	private ArrayList<OAJsonObjectNode> al = new ArrayList<>();

	public ArrayList<OAJsonObjectNode> getArray() {
		return al;
	}

	public OAJsonObjectNode get(int pos) {
		if (pos >= al.size()) {
			return null;
		}
		return al.get(pos);
	}

	public void add(OAJsonObjectNode node) {
		al.add(node);
	}

	public void remove(OAJsonObjectNode node) {
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

	public void insert(int pos, OAJsonObjectNode node) {
		int x = al.size();
		if (pos > x) {
			// pad array
			for (int j = x; j < pos; j++) {
				al.add(new OAJsonObjectNode());
			}
			al.add(node);
		} else {
			al.add(pos, node);
		}
	}

	protected void toJson(JsonOutputStream jos) {
		jos.append("[");
		boolean bFirst = true;

		for (OAJsonObjectNode nodex : al) {
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
}

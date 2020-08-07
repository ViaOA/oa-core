package com.viaoa.json.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.viaoa.json.io.JsonOutputStream;

public class OAJsonPropertyNode extends OAJsonNode {
	Map<String, OAJsonNode> map = new HashMap<>();

	protected void toJson(final JsonOutputStream jos) {
		jos.startLine();
		jos.append("{");
		jos.endLine();
		jos.addIndent();
		boolean bFirst = true;
		for (Entry<String, OAJsonNode> entry : map.entrySet()) {
			if (!bFirst) {
				jos.append(",");
				if (!jos.endLine()) {
					jos.append(" ");
				}
			}
			bFirst = false;

			jos.startLine();
			jos.append(String.format("\"%s\":", entry.getKey()));
			jos.addSpace();

			OAJsonNode nodex = entry.getValue();
			nodex.toJson(jos);
		}
		jos.endLine();
		jos.subtractIndent();
		jos.startLine();
		jos.append("}");
	}

}

package com.viaoa.json;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.viaoa.util.OAFile;

/*
	see: https://jaunt-api.com
		https://jaunt-api.com/javadocs/index.html

*/

//qqqqqqqqqqqqqqqqqqqqqq NOT USED qqqqqqqqqqqqqqqq

/**
 * JSON Util for parsing, queries.
 *
 * @author vvia
 */
public class OAJson1 {

	public static class JNode {
		/**
		 * Property name and values for all non object and non array properties.
		 */
		Map<String, String> mapKeyValue;

		/**
		 * Property name and object for reference properties.
		 */
		Map<String, JNode> mapNode;

		/**
		 * Property with array values.
		 */
		ArrayList<JNode> alNode;
	}

	/**
	 * Simply load flat json into a map.
	 */
	public Map<String, String> loadIntoFlatMap(final String json) {
		final Map<String, String> map = new HashMap();

		final JsonParser parser = Json.createParser(new StringReader(json));
		String key = null;
		while (parser.hasNext()) {
			final Event event = parser.next();
			if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				if (key != null) {
					String value = parser.getString();
					map.put(key, value);
				}
			}
		}
		parser.close();
		return map;
	}

	protected static class FoundNode {
		JNode node;
		String value;
	}

	/**
	 * Query for value, which can be any property (value, object, array)
	 *
	 * @param root
	 * @param propertyPath can also include array position (zero based).<br>
	 *                     Examples:<br>
	 *                     customer.orders[0].lineItem[0].quantity<br>
	 *                     customer.orders (return array)<br>
	 *                     order.customer.contact (return object)<br>
	 *                     order.customer.contact.phone.number (return value)<br>
	 *                     order.customer.active (return value)
	 */
	protected FoundNode findNode(JNode root, String propertyPath) {
		if (root == null) {
			return null;
		}
		if (propertyPath == null) {
			return null;
		}

		final FoundNode foundNode = new FoundNode();
		JNode node = root;

		for (String tok : propertyPath.split("\\.")) {
			if (node == null) {
				foundNode.value = null;
				break;
			}
			int pos = tok.indexOf('[');
			if (pos >= 0) {
				int pos2 = tok.indexOf(']');
				if (pos2 > pos) {
					String s = tok.substring(pos + 1, pos2);
					tok = tok.substring(0, pos);
					pos = Integer.valueOf(s);

				} else {
					pos = -1;
				}
			}

			JNode nodex = null;
			if (pos >= 0) {
				if (tok.length() == 0) {
					if (node.alNode != null) {
						if (pos < node.alNode.size()) {
							nodex = node.alNode.get(pos);
						}
					}
				} else if (node != null && node.mapNode != null) {
					nodex = node.mapNode.get(tok);
					if (nodex != null && nodex.alNode != null && pos < nodex.alNode.size()) {
						nodex = nodex.alNode.get(pos);
					} else {
						nodex = null;
					}
				}
			} else {
				if (node.mapKeyValue != null && node.mapKeyValue.containsKey(tok)) {
					foundNode.value = node.mapKeyValue.get(tok);
					nodex = node; // end leaf
				}
				if (node != null && node.mapNode != null) {
					nodex = node.mapNode.get(tok);
				}
			}
			node = nodex;
		}
		foundNode.node = node;
		return foundNode;
	}

	public String getValue(JNode root, String propertyPath) {
		if (root == null) {
			return null;
		}
		if (propertyPath == null) {
			return null;
		}

		FoundNode foundNode = findNode(root, propertyPath);
		String val = null;
		if (foundNode != null) {
			val = foundNode.value;
			if (val == null) {
				val = toJson(foundNode.node);
			}
		}
		return val;
	}

	public int getArraySize(JNode root, String propertyPath) {
		int result = -1;
		if (root == null) {
			return result;
		}
		if (propertyPath == null) {
			return result;
		}

		FoundNode foundNode = findNode(root, propertyPath);
		if (foundNode != null) {
			if (foundNode.node.alNode != null) {
				result = foundNode.node.alNode.size();
			}
		}
		return result;
	}

	public String toJson(JNode node) {
		if (node == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		_toJson(node, sb);

		return sb.toString();
	}

	protected void _toJson(JNode node, final StringBuilder sb) {
		if (node == null) {
			return;
		}

		boolean bHasObjectBrackets = false;

		boolean bFirst = true;
		if (node.mapKeyValue != null) {
			if (!bHasObjectBrackets) {
				bHasObjectBrackets = true;
				sb.append("{");
			}
			for (Entry<String, String> entry : node.mapKeyValue.entrySet()) {
				if (!bFirst) {
					sb.append(", ");
				}
				sb.append(String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()));
				bFirst = false;
			}
		}

		if (node.mapNode != null) {
			if (!bHasObjectBrackets) {
				bHasObjectBrackets = true;
				sb.append("{");
			}
			for (Entry<String, JNode> entry : node.mapNode.entrySet()) {
				JNode nodex = entry.getValue();
				if (!bFirst) {
					sb.append(", ");
				}
				StringBuilder sbx = new StringBuilder();
				_toJson(nodex, sbx);
				sb.append(String.format("\"%s\": %s", entry.getKey(), sbx.toString()));
				bFirst = false;
			}
		}
		if (node.alNode != null) {
			sb.append("[");
			for (JNode nodex : node.alNode) {
				if (!bFirst) {
					sb.append(", ");
				}
				StringBuilder sbx = new StringBuilder();
				_toJson(nodex, sbx);
				sb.append(sbx.toString());
				bFirst = false;
			}
			sb.append("]");
		}
		if (bHasObjectBrackets) {
			sb.append("}");
		}
	}

	public JNode load(final String json) {
		final JsonParser parser = Json.createParser(new StringReader(json));

		JNode root = load(parser, null, false);

		parser.close();
		return root;
	}

	protected JNode load(final JsonParser parser, JNode node, boolean bUsingArray) {
		if (node == null) {
			node = new JNode();
		}

		String key = null;
		while (parser.hasNext()) {
			final Event event = parser.next();

			if (event == Event.START_OBJECT) {
				if (bUsingArray) {
					JNode newNode = load(parser, null, bUsingArray);
					node.alNode.add(newNode);
				} else if (key != null) {
					if (node.mapNode == null) {
						node.mapNode = new HashMap<String, OAJson1.JNode>();
					}
					JNode newNode = load(parser, null, bUsingArray);
					node.mapNode.put(key, newNode);
					key = null;
				}
			} else if (event == Event.END_OBJECT) {
				break;
			} else if (event == Event.START_ARRAY) {
				if (key != null) {
					JNode newNode = new JNode();
					newNode.alNode = new ArrayList<>();
					load(parser, newNode, true);
					if (node.mapNode == null) {
						node.mapNode = new HashMap<String, OAJson1.JNode>();
					}
					node.mapNode.put(key, newNode);
					key = null;
				} else {
					node.alNode = new ArrayList<>();
					load(parser, node, true);
				}
			} else if (event == Event.END_ARRAY) {
				break;
			} else if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				if (key != null) {
					String value = parser.getString();
					if (node.mapKeyValue == null) {
						node.mapKeyValue = new HashMap<>();
					}
					node.mapKeyValue.put(key, value);
					key = null;
				}
			} else if (event == Event.VALUE_NUMBER) {
				if (key != null) {
					String value = parser.getString();
					if (node.mapKeyValue == null) {
						node.mapKeyValue = new HashMap<>();
					}
					node.mapKeyValue.put(key, value);
					key = null;
				}
			} else if (event == Event.VALUE_FALSE) {
				if (key != null) {
					if (node.mapKeyValue == null) {
						node.mapKeyValue = new HashMap<>();
					}
					node.mapKeyValue.put(key, "false");
					key = null;
				}
			} else if (event == Event.VALUE_TRUE) {
				if (key != null) {
					if (node.mapKeyValue == null) {
						node.mapKeyValue = new HashMap<>();
					}
					node.mapKeyValue.put(key, "true");
					key = null;
				}
			}
		}
		return node;
	}

	public static void main(String[] args) throws Exception {
		OAJson1 util = new OAJson1();
		String txt = OAFile.readTextFile(new File("/home/vvia/git/oa-core/src/main/java/com/viaoa/json/json.txt"), 0);
		JNode root = util.load(txt);

		String json = util.toJson(root);

		String s = util.getValue(root, "employees[1].weight");

		s = util.getValue(root, "employees[1]");

		s = util.getValue(root, "employees");

		int x = util.getArraySize(root, "employees");

		assert x == 4;

		int xx = 4;
		xx++;
	}
	//qqqqqqqqqqqqqq set property, array: insert/remove/add
}

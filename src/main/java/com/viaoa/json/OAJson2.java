package com.viaoa.json;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.viaoa.util.OAString;

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
public class OAJson2 {

	protected static class JsonOutputStream {
		int indent;
		String indentSpaces;
		OutputStream os;

		public JsonOutputStream(OutputStream os, String indentSpaces) {
			this.os = os;
			this.indentSpaces = indentSpaces;
		}

		public void append(String txt) {
			try {
				os.write(txt.getBytes());
			} catch (Exception e) {
				throw new RuntimeException("", e);
			}
		}

		public boolean endLine() {
			if (OAString.isEmpty(indentSpaces)) {
				return false;
			}
			try {
				os.write("\n".getBytes());
			} catch (Exception e) {
				throw new RuntimeException("", e);
			}
			return true;
		}

		public boolean addSpace() {
			if (OAString.isEmpty(indentSpaces)) {
				return false;
			}
			try {
				os.write(" ".getBytes());
			} catch (Exception e) {
				throw new RuntimeException("", e);
			}
			return true;
		}

		public boolean startLine() {
			if (OAString.isEmpty(indentSpaces)) {
				return false;
			}
			try {
				for (int i = 0; i < indent; i++) {
					os.write(indentSpaces.getBytes());
				}
			} catch (Exception e) {
				throw new RuntimeException("", e);
			}
			return true;
		}

		public void addIndent() {
			indent++;
		}

		public void subtractIndent() {
			indent--;
		}
	};

	public abstract static class OAJsonNode {
		String name;

		public String toJson() {
			return toJson(2);
		}

		public String toJson(int indentAmount) {
			String indent;
			if (indentAmount > 0) {
				indent = "";
				for (int i = 0; i < indentAmount; i++) {
					indent += " ";
				}
			} else {
				indent = null;
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			JsonOutputStream jos = new JsonOutputStream(bos, indent);

			toJson(jos);
			String result = bos.toString();
			return result;
		}

		protected abstract void toJson(final JsonOutputStream jos);
	}

	public static class OAJsonArrayNode extends OAJsonNode {
		ArrayList<OAJsonObjectNode> al = new ArrayList<>();

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
			al.remove(pos);
		}

		public int size() {
			return al.size();
		}

		public void insert(int pos, OAJsonObjectNode node) {
			al.add(pos, node);
		}

		protected void toJson(JsonOutputStream jos) {
			jos.append("[");
			jos.endLine();
			jos.addIndent();
			boolean bFirst = true;

			for (OAJsonObjectNode nodex : al) {
				if (!bFirst) {
					jos.append(",");
					if (!jos.endLine()) {
						jos.append(" ");
					}
				}
				nodex.toJson(jos);
				bFirst = false;
			}
			jos.endLine();
			jos.subtractIndent();
			jos.startLine();
			jos.append("]");
		}

		public OAJsonNode getNode(int pos, String propertyPath) {
			if (propertyPath == null) {
				return null;
			}

			OAJsonObjectNode node = this.get(pos);
			OAJsonNode nodex = node.getNode(propertyPath);
			return nodex;
		}
	}

	public static class OAJsonObjectNode extends OAJsonNode {
		Map<String, OAJsonNode> map = new HashMap<>();

		public void put(OAJsonNode node) {
			map.put(node.name, node);
		}

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

		//qqqq add property path support
		public void removeProperty(String propertyName) {
			if (propertyName == null) {
				return;
			}
			map.remove(propertyName);
		}

		public OAJsonObjectNode getObjectNode(String propertyPath) {
			OAJsonNode node = getNode(propertyPath);
			if (node instanceof OAJsonObjectNode) {
				return (OAJsonObjectNode) node;
			}
			return null;
		}

		public OAJsonArrayNode getArrayNode(String propertyPath) {
			OAJsonNode node = getNode(propertyPath);
			if (node instanceof OAJsonArrayNode) {
				return (OAJsonArrayNode) node;
			}
			return null;
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
		public OAJsonNode getNode(String propertyPath) {
			if (propertyPath == null) {
				return null;
			}

			OAJsonNode node = this;

			for (String tok : propertyPath.split("\\.")) {
				if (node == null) {
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

				OAJsonNode nodex = null;
				if (pos >= 0) {
					if (tok.length() == 0) {
						if (pos < ((OAJsonArrayNode) node).al.size()) {
							nodex = ((OAJsonArrayNode) node).al.get(pos);
						}
					} else if (node != null) {
						nodex = ((OAJsonObjectNode) node).map.get(tok);
						if (nodex != null && pos < ((OAJsonArrayNode) nodex).al.size()) {
							nodex = ((OAJsonArrayNode) nodex).al.get(pos);
						} else {
							nodex = null;
						}
					}
				} else {
					if (node != null) {
						nodex = ((OAJsonObjectNode) node).map.get(tok);
					}
				}
				node = nodex;
			}
			return node;
		}

		public int getArraySize(String propertyPath) {
			int result = -1;
			if (propertyPath == null) {
				return result;
			}

			OAJsonNode node = getNode(propertyPath);
			if (node instanceof OAJsonArrayNode) {
				result = ((OAJsonArrayNode) node).al.size();
			}
			return result;
		}

		public void setProperty(final String propertyPath, String value) {
			if (value != null) {
				value = OAString.escape(value);
			}
			setNode(propertyPath, new OAJsonStringNode(value));
		}

		public void setProperty(final String propertyPath, Number value) {
			setNode(propertyPath, new OAJsonNumberNode(value));
		}

		public void setProperty(final String propertyPath, Boolean value) {
			setNode(propertyPath, new OAJsonBooleanNode(value));
		}

		public void setNullProperty(final String propertyPath) {
			setNode(propertyPath, new OAJsonNullNode());
		}

		public void setProperty(final String propertyPath, OAJsonObjectNode node) {
			setNode(propertyPath, node);
		}

		public void setProperty(final String propertyPath, OAJsonArrayNode node) {
			setNode(propertyPath, node);
		}

		//QQQQQQQQQQQQQQQQQ create this for removeProperty qqqqqqqqqqqqqqq
		//qqqqqqq remove array element/pos

		public void setNode(final String propertyPath, final OAJsonNode newJsonNode) {
			if (propertyPath == null || newJsonNode == null) {
				return;
			}

			OAJsonNode node = this;

			final String[] tokens = propertyPath.split("\\.");

			for (int i = 0; i < tokens.length; i++) {
				String tok = tokens[i];
				final boolean bIsLastToken = (i + 1 == tokens.length);

				if (node == null) {
					break;
				}
				// if pos >= array.size, then just add it
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

				OAJsonNode nodex = null;
				if (pos >= 0) {
					OAJsonArrayNode arrayNode;

					if (tok.length() == 0) { // property path begins with array location ... ex: "[3].name"
						arrayNode = (OAJsonArrayNode) node;
						if (!(node instanceof OAJsonArrayNode)) {
							throw new RuntimeException("node must be an Array node, propertyPath=" + propertyPath);
						}
					} else {
						nodex = ((OAJsonObjectNode) node).map.get(tok);
						if (node instanceof OAJsonArrayNode) {
							arrayNode = (OAJsonArrayNode) node;
						} else {
							arrayNode = new OAJsonArrayNode();
							((OAJsonObjectNode) node).map.put(tok, arrayNode);
						}
					}
					// pad array
					for (int j = arrayNode.al.size(); j <= pos; j++) {
						arrayNode.al.add(new OAJsonObjectNode());
					}

					if (bIsLastToken) {
						if (!(newJsonNode instanceof OAJsonObjectNode)) {
							throw new RuntimeException(
									"cant setNode in array, newNode must be an objectNode, propertyPath=" + propertyPath);
						}
						arrayNode.al.set(pos, (OAJsonObjectNode) newJsonNode);
					} else {
						nodex = arrayNode.al.get(pos);
					}
				} else {
					if (!(node instanceof OAJsonObjectNode)) {
						throw new RuntimeException(
								"cant setNode, node must be an objectNode, propertyPath=" + propertyPath);
					}
					nodex = ((OAJsonObjectNode) node).map.get(tok);
					if (nodex == null) {
						if (bIsLastToken) {
							((OAJsonObjectNode) node).map.put(tok, newJsonNode);
						} else {
							nodex = new OAJsonObjectNode();
							((OAJsonObjectNode) node).map.put(tok, nodex);
						}
					} else {
						if (bIsLastToken) {
							((OAJsonObjectNode) node).map.put(tok, newJsonNode);
						} else {
							if (!(node instanceof OAJsonObjectNode)) {
								throw new RuntimeException("cant setNode, node in path is not an objectNode, propertyPath=" + propertyPath);
							}
						}
					}
				}
				node = nodex;
			}
		}
	}

	public static abstract class OAJsonPropertyNode extends OAJsonNode {
	}

	public static class OAJsonNullNode extends OAJsonPropertyNode {
		protected void toJson(final JsonOutputStream jos) {
			jos.append("null");
		}
	}

	public static class OAJsonStringNode extends OAJsonPropertyNode {
		String value;

		public OAJsonStringNode(String val) {
			this.value = val;
		}

		protected void toJson(final JsonOutputStream jos) {
			jos.append(String.format("\"%s\"", value));
		}
	}

	public static class OAJsonBooleanNode extends OAJsonPropertyNode {
		Boolean value;

		public OAJsonBooleanNode(Boolean val) {
			this.value = val;
		}

		protected void toJson(final JsonOutputStream jos) {
			jos.append(value.toString());
		}
	}

	public static class OAJsonNumberNode extends OAJsonPropertyNode {
		Number value;

		public OAJsonNumberNode(Number val) {
			this.value = val;
		}

		protected void toJson(final JsonOutputStream jos) {
			jos.append(value.toString());
		}

	}

	/*qqqqqqqqqqqqqqqqqqqqqqqqqqqqqq
		public static class OAJsonRootNode extends OAJsonObjectNode {
		}
	*/

	public OAJsonNode load(final String json) {
		final JsonParser parser = Json.createParser(new StringReader(json));

		//		OAJsonNode root = new OAJsonNode();
		OAJsonNode root = _load(parser, null, false);

		parser.close();
		return root;
	}

	//qqqqqqqqqqq test with a leading array ... ex:  "[ {}, {} }"

	protected OAJsonNode _load(final JsonParser parser, OAJsonNode node, boolean bUsingArray) {

		String key = null;
		while (parser.hasNext()) {
			final Event event = parser.next();

			if (event == Event.START_OBJECT) {
				if (bUsingArray) {
					OAJsonObjectNode newNode = (OAJsonObjectNode) _load(parser, null, bUsingArray);
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (key != null) {
						OAJsonObjectNode newNode = new OAJsonObjectNode();
						_load(parser, newNode, bUsingArray);
						((OAJsonObjectNode) node).map.put(key, newNode);
						key = null;
					} else if (node == null) {
						node = new OAJsonObjectNode();
					}
				}
			} else if (event == Event.END_OBJECT) {
				break;
			} else if (event == Event.START_ARRAY) {
				if (key != null) {
					OAJsonArrayNode newNode = new OAJsonArrayNode();
					_load(parser, newNode, true);
					((OAJsonObjectNode) node).map.put(key, newNode);
					key = null;
				} else {
					if (node == null) {
						node = new OAJsonArrayNode();
					}
					_load(parser, node, true);
				}
			} else if (event == Event.END_ARRAY) {
				break;
			} else if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				if (key != null) {
					String value = parser.getString();
					OAJsonStringNode newNode = new OAJsonStringNode(value);
					((OAJsonObjectNode) node).map.put(key, newNode);
					key = null;
				}
			} else if (event == Event.VALUE_NUMBER) {
				if (key != null) {
					Number value = parser.getBigDecimal();
					OAJsonNumberNode newNode = new OAJsonNumberNode(value);
					((OAJsonObjectNode) node).map.put(key, newNode);
					key = null;
				}
			} else if (event == Event.VALUE_FALSE) {
				if (key != null) {
					((OAJsonObjectNode) node).map.put(key, new OAJsonBooleanNode(Boolean.FALSE));
					key = null;
				}
			} else if (event == Event.VALUE_TRUE) {
				if (key != null) {
					((OAJsonObjectNode) node).map.put(key, new OAJsonBooleanNode(Boolean.TRUE));
					key = null;
				}
			}
		}
		return node;
	}

	/*
		public void test2X() throws Exception {
			String txt = OAFile.readTextFile(new File("/home/vvia/git/oa-core/src/main/java/com/viaoa/json/json.txt"), 0);
			OAJsonObjectNode root = load(txt);

			String json = root.toJson();

			OAJsonNode node = root.getNode("employees[1].weight");

			node = root.getNode("employees[1]");

			node = root.getNode("employees");
			int x = root.getArraySize("employees");

			OAJsonArrayNode na = (OAJsonArrayNode) node;
			OAJsonObjectNode objNode = new OAJsonObjectNode();
			objNode.setProperty("Name", new OAJsonStringNode("Value"));
			na.insert(2, objNode);

			String s = root.toJson();
			// assert x == 4;

		}

		public OAJsonRootNode test1() throws Exception {
			OAJsonRootNode node = new OAJsonRootNode();

			node.setProperty("firstName", new OAJsonStringNode("Vincent"));
			node.setProperty("nickName", new OAJsonStringNode("Vince"));
			node.setProperty("middleInitial", "A");
			node.setProperty("lastName", "Via");

			// OAJsonArrayNode arrayNode = new OAJsonArrayNode()
			String json = node.toJson();
			return node;
		}

		public OAJsonObjectNode test2() throws Exception {
			OAJsonRootNode root = new OAJsonRootNode();

			OAJsonRootNode node = test1();
			root.setProperty("customer", node);

			root.setNullProperty("discount");

			root.setProperty("cost", 123.45);

			root.setProperty("discount", new BigDecimal(10.00D, MathContext.DECIMAL64));

			String json = root.toJson();

			OAJsonNode nodex = root.getNode("customer");

			root.removeProperty("customer");

			json = root.toJson();

			OAJsonArrayNode arrayNode = new OAJsonArrayNode();

			root.setProperty("customers", arrayNode);
			arrayNode.add((OAJsonObjectNode) nodex);

			arrayNode.add((OAJsonObjectNode) nodex);
			json = root.toJson();

			nodex = root.getNode("customers");
			json = nodex.toJson(2);

			json = root.toJson(2);
			System.out.println(json);

			OAJsonRootNode rootx = load(json);
			OAJsonObjectNode customerNode = rootx.getObjectNode("customers[1]");
			customerNode.setNullProperty("NullProp");
			customerNode.setProperty("Alive", true);
			customerNode.setProperty("StringProp", "StringPropVAlue");
			json = rootx.toJson();
			System.out.println(json);

			OAJsonArrayNode customers = rootx.getArrayNode("customers");
			customerNode = customers.get(0);
			customerNode.setNullProperty("NullProp2");
			customerNode.setProperty("Alive2", true);
			customerNode.setProperty("StringProp2", "StringPropVAlue2");
			json = rootx.toJson();
			System.out.println(json);

			return node;
		}
	*/
	public void test3() throws Exception {
		OAJsonObjectNode root = new OAJsonObjectNode();

		root.setProperty("orderItem.order.customer.company.name", "ABC Company");

		String json = root.toJson();
		System.out.println(json);
	}

	public void test4() throws Exception {
		OAJsonObjectNode root = new OAJsonObjectNode();

		//qqqqqqqq instead of doing an add ... might want to have it create new ones to fill the array to pos  qqqq
		root.setProperty("company.customers[1].name", "ABC Company");

		String json = root.toJson();
		System.out.println(json);
	}

	public static void main(String[] args) throws Exception {
		OAJson2 util = new OAJson2();

		util.test4();

		int xx = 4;
		xx++;
	}

	//qqqqqqqqqqqqqq set property, array: insert/remove/add
}

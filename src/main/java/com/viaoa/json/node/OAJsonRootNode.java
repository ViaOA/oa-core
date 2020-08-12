package com.viaoa.json.node;

import com.viaoa.json.OAJson;
import com.viaoa.json.io.JsonOutputStream;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

/**
 * "Top" node when loading JSON. Will be either an Array or Object node.<br>
 * Has methods to get and set using property and property paths.
 * <p>
 * Property paths are dot separated, and allow for array indexing (ex: "customer.orders[1].date")
 *
 * @author vvia
 */
public abstract class OAJsonRootNode extends OAJsonNode {

	public void set(final String propertyPath, String value) {
		if (value != null) {
			value = OAString.escape(value);
		}
		set(propertyPath, new OAJsonStringNode(value));
	}

	public void set(final String propertyPath, Number value) {
		set(propertyPath, new OAJsonNumberNode(value));
	}

	public void set(final String propertyPath, Boolean value) {
		set(propertyPath, new OAJsonBooleanNode(value));
	}

	public void set(final String propertyPath, OADate date) {
		if (date == null) {
			setNull(propertyPath);
		} else {
			String value = date.toString(date.JsonFormat);
			set(propertyPath, new OAJsonStringNode(value));
		}
	}

	public void set(final String propertyPath, OADateTime dateTime) {
		if (dateTime == null) {
			setNull(propertyPath);
		} else {
			String value = dateTime.toString(dateTime.JsonFormat);
			set(propertyPath, new OAJsonStringNode(value));
		}
	}

	public void setNull(final String propertyPath) {
		set(propertyPath, new OAJsonNullNode());
	}

	public void setJson(final String propertyPath, String json) {
		OAJson oaJson = new OAJson();
		OAJsonRootNode node = oaJson.load(json);
		set(propertyPath, node);
	}

	//	.. also setJson(pp, json)
	public OAJsonObjectNode getObject(String propertyPath) {
		return getObjectNode(propertyPath);
	}

	public OAJsonObjectNode getObjectNode(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonObjectNode) {
			return (OAJsonObjectNode) node;
		}
		return null;
	}

	public OADate getDate(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonStringNode) {
			String s = ((OAJsonStringNode) node).value;
			OADate date = new OADate(s, OADate.JsonFormat);
			return date;
		}
		return null;
	}

	public OADateTime getDateTime(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonStringNode) {
			String s = ((OAJsonStringNode) node).value;
			OADateTime dateTime = new OADateTime(s, OADate.JsonFormat);
			return dateTime;
		}
		return null;
	}

	public OADateTime getDateTimeTZ(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonStringNode) {
			String s = ((OAJsonStringNode) node).value;
			OADateTime dateTime = new OADateTime(s, OADate.JsonFormatTZ);
			return dateTime;
		}
		return null;
	}

	public String getString(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonStringNode) {
			return ((OAJsonStringNode) node).value;
		}
		return null;
	}

	public Number getNumber(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonStringNode) {
			return ((OAJsonNumberNode) node).value;
		}
		return null;
	}

	public Boolean getBoolean(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonBooleanNode) {
			return ((OAJsonBooleanNode) node).value;
		}
		return null;
	}

	public OAJsonArrayNode getArray(String propertyPath) {
		return getArrayNode(propertyPath);
	}

	public OAJsonArrayNode getArrayNode(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonArrayNode) {
			return (OAJsonArrayNode) node;
		}
		return null;
	}

	// get array size
	public int getSize(String propertyPath) {
		OAJsonNode node = get(propertyPath);
		int x = -1;
		if (node instanceof OAJsonArrayNode) {
			x = ((OAJsonArrayNode) node).getArray().size();
		}
		return x;
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
	public OAJsonNode get(String propertyPath) {
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
					if (!(node instanceof OAJsonArrayNode)) {
						node = null;
						break;
					}
					if (pos < ((OAJsonArrayNode) node).getArray().size()) {
						nodex = ((OAJsonArrayNode) node).getArray().get(pos);
					}
				} else if (node != null) {
					if (!(node instanceof OAJsonObjectNode)) {
						node = null;
						break;
					}
					nodex = ((OAJsonObjectNode) node).get(tok);
					if (!(nodex instanceof OAJsonArrayNode)) {
						node = null;
						break;
					}
					if (nodex != null && pos < ((OAJsonArrayNode) nodex).getArray().size()) {
						nodex = ((OAJsonArrayNode) nodex).getArray().get(pos);
					} else {
						nodex = null;
					}
				}
			} else {
				if (!(node instanceof OAJsonObjectNode)) {
					node = null;
					break;
				}
				nodex = ((OAJsonObjectNode) node).get(tok);
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

		OAJsonNode node = get(propertyPath);
		if (node instanceof OAJsonArrayNode) {
			result = ((OAJsonArrayNode) node).getArray().size();
		}
		return result;
	}

	public void set(final String propertyPath, final OAJsonNode newJsonNode) {
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
					if (!(node instanceof OAJsonArrayNode)) {
						throw new RuntimeException("node must be an Array node, propertyPath=" + propertyPath);
					}
					arrayNode = (OAJsonArrayNode) node;
				} else {
					if (!(node instanceof OAJsonObjectNode)) {
						throw new RuntimeException("node must be an Object node, propertyPath=" + propertyPath);
					}
					nodex = ((OAJsonObjectNode) node).get(tok);
					if (nodex instanceof OAJsonArrayNode) {
						arrayNode = (OAJsonArrayNode) nodex;
					} else {
						arrayNode = new OAJsonArrayNode();
						((OAJsonObjectNode) node).set(tok, arrayNode);
					}
				}
				// pad array
				for (int j = arrayNode.getArray().size(); j <= pos; j++) {
					arrayNode.getArray().add(new OAJsonNullNode());
				}

				if (bIsLastToken) {
					arrayNode.getArray().set(pos, newJsonNode);
				} else {
					nodex = arrayNode.getArray().get(pos);
					if (nodex instanceof OAJsonNullNode) {
						// change out the null to allow for newValue
						nodex = new OAJsonObjectNode();
						arrayNode.getArray().set(pos, nodex);
					}
				}
			} else {
				if (!(node instanceof OAJsonObjectNode)) {
					throw new RuntimeException(
							"cant setNode, node must be objectNode, propertyPath=" + propertyPath);
				}
				nodex = ((OAJsonObjectNode) node).get(tok);
				if (nodex == null) {
					if (bIsLastToken) {
						((OAJsonObjectNode) node).set(tok, newJsonNode);
					} else {
						nodex = new OAJsonObjectNode();
						((OAJsonObjectNode) node).set(tok, nodex);
					}
				} else {
					if (bIsLastToken) {
						((OAJsonObjectNode) node).set(tok, newJsonNode);
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

	// remove properties or array positions
	public void remove(String propertyPath) {
		if (propertyPath == null) {
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
				OAJsonArrayNode arrayNode = null;

				if (tok.length() == 0) { // property path begins with array location ... ex: "[3].name"
					if (!(node instanceof OAJsonArrayNode)) {
						throw new RuntimeException("node must be an Array node, propertyPath=" + propertyPath);
					}
					arrayNode = (OAJsonArrayNode) node;
				} else {
					if (!(node instanceof OAJsonObjectNode)) {
						throw new RuntimeException("node must be an Object node, propertyPath=" + propertyPath);
					}
					nodex = ((OAJsonObjectNode) node).get(tok);
					if (nodex instanceof OAJsonArrayNode) {
						arrayNode = (OAJsonArrayNode) nodex;
					}
				}

				if (arrayNode == null) {
					break;
				}
				if (pos >= arrayNode.getArray().size()) {
					break;
				}

				if (bIsLastToken) {
					arrayNode.getArray().remove(pos);
				} else {
					nodex = arrayNode.getArray().get(pos);
				}
			} else {
				if (!(node instanceof OAJsonObjectNode)) {
					throw new RuntimeException(
							"cant setNode, node must be an objectNode, propertyPath=" + propertyPath);
				}
				if (bIsLastToken) {
					((OAJsonObjectNode) node).remove(tok);
				} else {
					nodex = ((OAJsonObjectNode) node).get(tok);
				}
			}
			node = nodex;
		}
	}

	//  insert into array (w/ padding) at a index/position
	public void insert(final String propertyPath, final int index, final OAJsonObjectNode newJsonNode) {
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
				if (bIsLastToken) {
					throw new RuntimeException(
							"cant insert into array, property path must end in array property, propertyPath=" + propertyPath);
				}

				OAJsonArrayNode arrayNode;

				if (tok.length() == 0) { // property path begins with array location ... ex: "[3].name"
					if (!(node instanceof OAJsonArrayNode)) {
						throw new RuntimeException("node must be an Array node, propertyPath=" + propertyPath);
					}
					arrayNode = (OAJsonArrayNode) node;
				} else {
					if (!(node instanceof OAJsonObjectNode)) {
						throw new RuntimeException("node must be an Object node, propertyPath=" + propertyPath);
					}
					nodex = ((OAJsonObjectNode) node).get(tok);
					if (node instanceof OAJsonArrayNode) {
						arrayNode = (OAJsonArrayNode) node;
					} else {
						arrayNode = new OAJsonArrayNode();
						((OAJsonObjectNode) node).set(tok, arrayNode);
					}
				}
				// pad array
				for (int j = arrayNode.getArray().size(); j <= pos; j++) {
					arrayNode.getArray().add(new OAJsonNullNode());
				}

				nodex = arrayNode.getArray().get(pos);
			} else {
				if (!(node instanceof OAJsonObjectNode)) {
					if (OAString.isEmpty(tok) && bIsLastToken && node instanceof OAJsonArrayNode) {
						break;
					}
					throw new RuntimeException(
							"cant setNode, node must be an objectNode, propertyPath=" + propertyPath);
				}
				nodex = ((OAJsonObjectNode) node).get(tok);
				if (nodex == null) {
					OAJsonArrayNode arrayNode = new OAJsonArrayNode();
					((OAJsonObjectNode) node).set(tok, arrayNode);
					nodex = arrayNode;
				}
			}
			node = nodex;
		}

		if (!(node instanceof OAJsonArrayNode)) {
			throw new RuntimeException(
					"cant insert, property must be an JsonArrayNode, propertyPath=" + propertyPath);

		}
		OAJsonArrayNode arrayNode = (OAJsonArrayNode) node;
		arrayNode.insert(index, newJsonNode);
	}

	/**
	 * Add an object to an array.
	 */
	public void add(String propertyPath, OAJsonObjectNode newJsonNode) {
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
				if (bIsLastToken) {
					throw new RuntimeException(
							"cant insert into array, property path must end in array property, propertyPath=" + propertyPath);
				}

				OAJsonArrayNode arrayNode;

				if (tok.length() == 0) { // property path begins with array location ... ex: "[3].name"
					if (!(node instanceof OAJsonArrayNode)) {
						throw new RuntimeException("node must be an Array node, propertyPath=" + propertyPath);
					}
					arrayNode = (OAJsonArrayNode) node;
				} else {
					if (!(node instanceof OAJsonObjectNode)) {
						throw new RuntimeException("node must be an Object node, propertyPath=" + propertyPath);
					}
					nodex = ((OAJsonObjectNode) node).get(tok);
					if (node instanceof OAJsonArrayNode) {
						arrayNode = (OAJsonArrayNode) node;
					} else {
						arrayNode = new OAJsonArrayNode();
						((OAJsonObjectNode) node).set(tok, arrayNode);
					}
				}
				// pad array
				for (int j = arrayNode.getArray().size(); j <= pos; j++) {
					arrayNode.getArray().add(new OAJsonNullNode());
				}

				nodex = arrayNode.getArray().get(pos);
			} else {
				if (!(node instanceof OAJsonObjectNode)) {
					if (OAString.isEmpty(tok) && bIsLastToken && node instanceof OAJsonArrayNode) {
						break;
					}

					throw new RuntimeException(
							"cant setNode, node must be an objectNode, propertyPath=" + propertyPath);
				}
				nodex = ((OAJsonObjectNode) node).get(tok);
				if (nodex == null) {
					OAJsonArrayNode arrayNode = new OAJsonArrayNode();
					((OAJsonObjectNode) node).set(tok, arrayNode);
					nodex = arrayNode;
				}
			}
			node = nodex;
		}

		if (!(node instanceof OAJsonArrayNode)) {
			throw new RuntimeException(
					"cant insert, property must be an JsonArrayNode, propertyPath=" + propertyPath);

		}
		OAJsonArrayNode arrayNode = (OAJsonArrayNode) node;

		arrayNode.add(newJsonNode);
	}

	//  remove from array at an index/position
	public void remove(final String propertyPath, final int index) {
		if (propertyPath == null) {
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
				if (bIsLastToken) {
					throw new RuntimeException(
							"cant insert into array, property path must end in array property, propertyPath=" + propertyPath);
				}

				OAJsonArrayNode arrayNode;

				if (tok.length() == 0) { // property path begins with array location ... ex: "[3].name"
					if (!(node instanceof OAJsonArrayNode)) {
						throw new RuntimeException("node must be an Array node, propertyPath=" + propertyPath);
					}
					arrayNode = (OAJsonArrayNode) node;
				} else {
					if (!(node instanceof OAJsonObjectNode)) {
						throw new RuntimeException("node must be an Object node, propertyPath=" + propertyPath);
					}
					nodex = ((OAJsonObjectNode) node).get(tok);
					if (node instanceof OAJsonArrayNode) {
						arrayNode = (OAJsonArrayNode) node;
					} else {
						arrayNode = new OAJsonArrayNode();
						((OAJsonObjectNode) node).set(tok, arrayNode);
					}
				}
				if (pos >= arrayNode.getArray().size()) {
					node = null;
					break;
				}

				nodex = arrayNode.getArray().get(pos);
			} else {
				if (!(node instanceof OAJsonObjectNode)) {
					if (OAString.isEmpty(tok) && bIsLastToken && node instanceof OAJsonArrayNode) {
						break;
					}
					throw new RuntimeException(
							"cant remove array from Node, node must be an objectNode, propertyPath=" + propertyPath);
				}
				nodex = ((OAJsonObjectNode) node).get(tok);
				if (nodex == null) {
					node = null;
					break;
				}
			}
			node = nodex;
		}

		if (!(node instanceof OAJsonArrayNode)) {
			throw new RuntimeException(
					"cant insert, property must be an JsonArrayNode, propertyPath=" + propertyPath);

		}
		OAJsonArrayNode arrayNode = (OAJsonArrayNode) node;
		arrayNode.remove(index);
	}

	@Override
	protected abstract void toJson(JsonOutputStream jos);

}

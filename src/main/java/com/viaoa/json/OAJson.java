package com.viaoa.json;

import java.io.StringReader;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.viaoa.jaxb.OAJaxb;
import com.viaoa.json.node.OAJsonArrayNode;
import com.viaoa.json.node.OAJsonBooleanNode;
import com.viaoa.json.node.OAJsonNode;
import com.viaoa.json.node.OAJsonNullNode;
import com.viaoa.json.node.OAJsonNumberNode;
import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.json.node.OAJsonStringNode;

/**
 * Create and load JSON data into a tree node object graph . Supports property paths for getting and setting data.
 * <p>
 * Loads json from from string value into tree nodes.
 * <p>
 * The tree nodes can then be used to change the json object graph and to convert back to json text (String), with or without formatting
 * (indenting).
 *
 * @see OAJsonObjectNode for creating with an single object as the root.
 * @see OAJsonArrayNode for creating with an array as the root.
 * @see OAJaxb for marshalling/unmarshalling java to/from json
 * @author vvia
 */
public class OAJson {

	public OAJsonNode load(final String json) {
		final JsonParser parser = Json.createParser(new StringReader(json));

		//		OAJsonNode root = new OAJsonNode();
		OAJsonNode root = (OAJsonNode) _load(parser, null, false);

		parser.close();
		return root;
	}

	public OAJsonObjectNode loadObject(final String json) {
		final JsonParser parser = Json.createParser(new StringReader(json));

		//		OAJsonNode root = new OAJsonNode();
		OAJsonNode root = (OAJsonNode) _load(parser, null, false);

		parser.close();
		return (OAJsonObjectNode) root;
	}

	public OAJsonArrayNode loadArray(final String json) {
		final JsonParser parser = Json.createParser(new StringReader(json));

		//		OAJsonNode root = new OAJsonNode();
		OAJsonNode root = (OAJsonNode) _load(parser, null, false);

		parser.close();
		return (OAJsonArrayNode) root;
	}

	protected OAJsonNode _load(final JsonParser parser, OAJsonNode node, boolean bUsingArray) {

		String key = null;
		int cnt = 0;
		while (parser.hasNext()) {
			final Event event = parser.next();
			cnt++;

			if (event == Event.START_OBJECT) {
				if (bUsingArray) {
					OAJsonObjectNode newNode = new OAJsonObjectNode();
					_load(parser, newNode, false);
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (key != null) {
						OAJsonObjectNode newNode = new OAJsonObjectNode();
						_load(parser, newNode, false);
						((OAJsonObjectNode) node).set(key, newNode);
						key = null;
					} else if (node == null) {
						node = new OAJsonObjectNode();
						_load(parser, node, false);
					}
				}
			} else if (event == Event.END_OBJECT) {
				break;
			} else if (event == Event.START_ARRAY) {
				if (bUsingArray) {
					// 20210217 an array of arrays
					OAJsonArrayNode newNode = new OAJsonArrayNode();
					_load(parser, newNode, true);
					((OAJsonArrayNode) node).add(newNode);
				} else if (key != null) {
					OAJsonArrayNode newNode = new OAJsonArrayNode();
					_load(parser, newNode, true);
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				} else {
					node = new OAJsonArrayNode();
					_load(parser, node, true);
				}
			} else if (event == Event.END_ARRAY) {
				break;
			} else if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				String value = parser.getString();
				OAJsonStringNode newNode = new OAJsonStringNode(value);
				if (key != null) {
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				} else if (bUsingArray) {
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (cnt == 1) {
						node = newNode;
					}
				}
			} else if (event == Event.VALUE_NUMBER) {
				Number value = parser.getBigDecimal();
				OAJsonNumberNode newNode = new OAJsonNumberNode(value);
				if (key != null) {
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				} else if (bUsingArray) {
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (cnt == 1) {
						node = newNode;
					}
				}
			} else if (event == Event.VALUE_FALSE) {
				OAJsonBooleanNode newNode = new OAJsonBooleanNode(Boolean.FALSE);
				if (key != null) {
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				} else if (bUsingArray) {
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (cnt == 1) {
						node = newNode;
					}
				}
			} else if (event == Event.VALUE_TRUE) {
				OAJsonBooleanNode newNode = new OAJsonBooleanNode(Boolean.TRUE);
				if (key != null) {
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				} else if (bUsingArray) {
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (cnt == 1) {
						node = newNode;
					}
				}
			} else if (event == Event.VALUE_NULL) {
				OAJsonNullNode newNode = new OAJsonNullNode();
				if (key != null) {
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				} else if (bUsingArray) {
					((OAJsonArrayNode) node).add(newNode);
				} else {
					if (cnt == 1) {
						node = newNode;
					}
				}
			}
		}
		return node;
	}
}

package com.viaoa.json;

import java.io.StringReader;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.viaoa.json.node.OAJsonArrayNode;
import com.viaoa.json.node.OAJsonBooleanNode;
import com.viaoa.json.node.OAJsonNode;
import com.viaoa.json.node.OAJsonNullNode;
import com.viaoa.json.node.OAJsonNumberNode;
import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.json.node.OAJsonRootNode;
import com.viaoa.json.node.OAJsonStringNode;

/*
	see: https://jaunt-api.com
		https://jaunt-api.com/javadocs/index.html
*/

/**
 * JSON Util for parsing, queries.
 *
 * @author vvia
 */
public class OAJson {

	public OAJsonRootNode load(final String json) {
		final JsonParser parser = Json.createParser(new StringReader(json));

		//		OAJsonNode root = new OAJsonNode();
		OAJsonRootNode root = (OAJsonRootNode) _load(parser, null, false);

		parser.close();
		return root;
	}

	protected OAJsonNode _load(final JsonParser parser, OAJsonNode node, boolean bUsingArray) {

		String key = null;
		while (parser.hasNext()) {
			final Event event = parser.next();

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
					}
				}
			} else if (event == Event.END_OBJECT) {
				break;
			} else if (event == Event.START_ARRAY) {
				if (key != null) {
					OAJsonArrayNode newNode = new OAJsonArrayNode();
					_load(parser, newNode, true);
					((OAJsonObjectNode) node).set(key, newNode);
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
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				}
			} else if (event == Event.VALUE_NUMBER) {
				if (key != null) {
					Number value = parser.getBigDecimal();
					OAJsonNumberNode newNode = new OAJsonNumberNode(value);
					((OAJsonObjectNode) node).set(key, newNode);
					key = null;
				}
			} else if (event == Event.VALUE_FALSE) {
				if (key != null) {
					((OAJsonObjectNode) node).set(key, new OAJsonBooleanNode(Boolean.FALSE));
					key = null;
				}
			} else if (event == Event.VALUE_TRUE) {
				if (key != null) {
					((OAJsonObjectNode) node).set(key, new OAJsonBooleanNode(Boolean.TRUE));
					key = null;
				}
			} else if (event == Event.VALUE_NULL) {
				if (key != null) {
					((OAJsonObjectNode) node).set(key, new OAJsonNullNode());
					key = null;
				}
			}
		}
		return node;
	}

}

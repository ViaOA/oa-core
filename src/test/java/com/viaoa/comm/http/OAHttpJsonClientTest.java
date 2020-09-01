package com.viaoa.comm.http;

import java.util.HashMap;
import java.util.Map;

import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.json.node.OAJsonRootNode;
import com.viaoa.util.OADate;

/**

 */
public class OAHttpJsonClientTest {

	public void testItemRestrictionPut() throws Exception {
		// create JSON object for IR put request

		OAHttpJsonClient client = new OAHttpJsonClient();

		OAJsonRootNode node = new OAJsonObjectNode();
		node.set("itemRuleType", "LINE_ITEM"); // LINE, PRODUCT_LINE_CODE, PRODUCT_LINE_SUBCODE, LINE_ITEM;
		node.set("changeType", "SALES_RESTRICTED"); // SALES_RESTRICTED, FLIGHT_RESTRICTED, CAUSTIC, HYBRID_ELECTRIC, FREON_RESTRICTED
		node.set("updateType", "CHANGE"); // CHANGE, ADD, DELETE
		node.set("newValue", "true"); // boolean
		node.set("line", "WIX");
		node.set("productLineCode", "");
		node.set("productLineSubcode", "");
		node.set("item", "51515");
		node.set("storeId", "01234");
		node.set("zipcode", "44260");
		node.set("state", "VA");
		node.set("county", "FAIRFAX");
		node.set("salesRestrictedEffectiveDate", new OADate());
		// node.set("", "");

		String json = node.toJson();

		String s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/put", json);

		int xx = 4;
		xx++;
	}

	// @Test
	public void test() throws Exception {

		String s;
		// s = httpGet("http://localhost:8082/servlet/oarest/salesorder/23548?pp=salesorderitems.item.mold");

		// s = httpPost("http://localhost:8082/servlet/oarest/salesorder/23548", null, null);

		// s = httpPost("http://localhost:8082/servlet/oarest/salesorder/23548", null, null);

		OAHttpJsonClient client = new OAHttpJsonClient();

		// ?line=fRE&productLineCode=0&productLineSubcode=123&item=R134A-30&storeId=12345&zipcode=44260&state=wI&county=Oranga");
		Map<String, String> map = new HashMap<String, String>();
		map.put("line", "fRE");
		map.put("productLineCode", "0");
		map.put("productLineSubcode", "123");
		map.put("item", "R134A-30");
		map.put("storeId", "12345");
		map.put("zipcode", "20108");
		map.put("state", "VA");
		map.put("county", "FAIRFAX");
		/*
				s = client.get("http://localhost:18080/retail-products/itemRestriction", map);

				s = client.post("http://localhost:18080/retail-products/iseries/itemRestriction/get", map);
		*/

		s = client.post("http://localhost:8081/retail-products/iseries/items/getSalesRestrictedItemsByLocation", map);

		//qqqqqqqqq put json into a Map qqqqqqqqqqq

		// localhost:18080/retail-products/itemRestriction?line=14&productLineCode=0&productLineSubcode=0&item=2343&storeId=4&zipcode=12345&state=GA&county=Cobb

		// s = client.get("http://localhost:8081/retail-products/iseries/itemRestriction/get?line=fRE&productLineCode=0&productLineSubcode=123&item=R134A-30&storeId=12345&zipcode=44260&state=wI&county=Oranga");

		/*
		String json = "{'line'='fRE'&'productLineCode'=0&'productLineSubcode'=123&'item'='R134A-30'&'storeId'=12345&'zipcode'='44260'&'state'='wI'&'county'='Oranga'&'restrictedEffectiveDate'='2020-01-15'}";
		json = json.replace("&", ",\n");
		json = json.replace('=', ':');
		json = json.replace('\'', '\"');
		
		s = OAHttpClient
				.httpPost("http://localhost:8081/retail-products/iseries/itemRestriction/getRestriction", json);
		*/

		int xx = 4;
		xx++;
	}

	public static void main(String[] args) throws Exception {
		OAHttpJsonClientTest test = new OAHttpJsonClientTest();
		test.testItemRestrictionPut();

		System.out.println("DONE");
	}
}

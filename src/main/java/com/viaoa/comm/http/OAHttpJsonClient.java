package com.viaoa.comm.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.viaoa.jaxb.OAJaxb;
import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.json.node.OAJsonRootNode;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.util.Base64;
import com.viaoa.util.OADate;
import com.viaoa.util.OAString;

/*
http://rest.elkstein.org/2008/02/using-rest-in-java.html

https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types
https://developer.mozilla.org/en-US/docs/Web/HTTP

https://www.codota.com/code/java/methods/java.net.URLConnection/setRequestProperty

https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication

HttpStatus.java
  response 401 (Unauthorized)
 WWW-Authenticate ...
 response 403 Forbidden

	Transfer-Encoding: chunked
	Server: Jetty(9.4.19.v20190610)
	Set-Cookie: JSESSIONID=node010qaw7pt1lgctisiwjo2vipcd3.node0; Path=/
	Expires: Thu, 01 Jan 1970 00:00:00 GMT
	Content-Type: application/json;charset=utf-8


see OAHttpsUtil.java for using https/SSL/TLS



*/

/**
 * @author vvia
 */
public class OAHttpJsonClient {

	private String userId;
	private transient String password;
	private String cookie;

	public void setUserAccess(String userId, String password) {
		this.userId = userId;
		this.password = password;
	}

	public void setCookie(String val) {
		this.cookie = val;
	}

	public String get(String urlStr) throws IOException {
		String json = perform(urlStr, "GET", null);
		return json;
	}

	public <T extends OAObject> T get(String urlStr, Class<T> responseClass) throws Exception {
		String json = perform(urlStr, "GET", null);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String get(String urlStr, Map<String, String> mapRequest) throws Exception {
		String s = urlEncode(mapRequest);
		String json = perform(urlStr + "?" + s, "GET", null);
		return json;
	}

	public <T extends OAObject> T get(String urlStr, Class<T> responseClass, Map<String, String> mapRequest) throws Exception {
		String json = get(urlStr, mapRequest);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String get(String urlStr, OAObject objRequest) throws Exception {
		String s = urlEncode(objRequest);
		String json = perform(urlStr + "?" + s, "GET", null);
		return json;
	}

	public <T extends OAObject> T get(String urlStr, Class<T> responseClass, OAObject objRequest) throws Exception {
		String json = get(urlStr, objRequest);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String post(String urlStr) throws IOException {
		String json = perform(urlStr, "POST", null);
		return json;
	}

	public String post(String urlStr, String jsonRequest) throws IOException {
		String json = perform(urlStr, "POST", jsonRequest);
		return json;
	}

	public <T extends OAObject> T post(String urlStr, Class<T> responseClass) throws Exception {
		String json = perform(urlStr, "POST", null);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String post(String urlStr, Map<String, String> mapRequest) throws Exception {
		String jsonRequest = "";
		if (mapRequest != null) {
			boolean bFirst = true;
			for (Entry<String, String> entry : mapRequest.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (jsonRequest.length() != 0) {
					jsonRequest += ", ";
				}
				jsonRequest += "\"" + key + "\": \"" + val + "\"";
			}
		}
		String json = perform(urlStr, "POST", "{" + jsonRequest + "}");
		return json;
	}

	public <T extends OAObject> T post(String urlStr, Class<T> responseClass, Map<String, String> mapRequest) throws Exception {
		String jsonRequest = "";

		if (mapRequest != null) {
			boolean bFirst = true;
			for (Entry<String, String> entry : mapRequest.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (jsonRequest.length() == 0) {
					jsonRequest += ", ";
				}
				jsonRequest += "\"" + key + "\": \"" + val + "\"";
			}
		}

		String json = perform(urlStr, "POST", "{" + jsonRequest + "}");

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String post(String urlStr, OAObject reqObject) throws Exception {
		String jsonRequest;
		if (reqObject == null) {
			jsonRequest = null;
		} else {
			OAJaxb jaxb = new OAJaxb<>(reqObject.getClass());
			jsonRequest = jaxb.convertToJSON(reqObject);
		}

		String json = perform(urlStr, "POST", jsonRequest);

		return json;
	}

	public <T extends OAObject> T post(String urlStr, Class<T> responseClass, String jsonRequest) throws Exception {
		String json = perform(urlStr, "POST", jsonRequest);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public <T extends OAObject> T post(String urlStr, Class<T> responseClass, OAObject reqObject) throws Exception {
		String jsonRequest;
		if (reqObject == null) {
			jsonRequest = null;
		} else {
			OAJaxb jaxb = new OAJaxb<>(reqObject.getClass());
			jsonRequest = jaxb.convertToJSON(reqObject);
		}

		String json = perform(urlStr, "POST", jsonRequest);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String perform(String urlStr, String methodName, String jsonRequest) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestProperty("User-Agent", "OAHttpJsonClient");
		conn.setRequestMethod(methodName);
		conn.setDoOutput(true);

		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		if (OAString.isNotEmpty(jsonRequest)) {
			conn.setRequestProperty("Content-Type", "application/json");
		}
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Accept", "application/json"); // "application/json, text/*;q=0.7");

		if (OAString.isNotEmpty(cookie)) {
			conn.addRequestProperty("cookie", cookie);
		}

		if (OAString.isNotEmpty(userId)) {
			String s = userId + ":" + password;
			conn.setRequestProperty("Authorization", "Basic " + Base64.encode(s));
		}

		if (OAString.isNotEmpty(jsonRequest)) {
			OutputStream out = conn.getOutputStream();
			Writer writer = new OutputStreamWriter(out, "UTF-8");

			writer.write(jsonRequest);
			writer.close();
			out.close();
		}

		String setcookie = conn.getHeaderField("Set-Cookie");
		if (OAString.isNotEmpty(setcookie)) {
			this.cookie = OAString.field(setcookie, ";", 1);
		}

		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
		int responseCode = conn.getResponseCode();

		if (responseCode != 200) {
			throw new IOException("Error non 200 Response code:" + responseCode + ", msg=" + conn.getResponseMessage());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		for (;;) {
			int ch = br.read();
			if (ch < 0) {
				break;
			}
			sb.append((char) ch);
		}

		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();

		conn.disconnect();
		return sb.toString();
	}

	protected String urlEncode(Map<String, String> map) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (map != null) {
			boolean bFirst = true;
			for (Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (!bFirst) {
					sb.append("&");
				}
				bFirst = false;
				sb.append(key);
				sb.append("=");
				sb.append(URLEncoder.encode(val, "UTF-8"));
				// https://www.jmarshall.com/easy/http/http_footnotes.html#urlencoding
			}
		}

		return sb.toString();
	}

	public String urlEncode(OAObject obj) throws Exception {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
		Map<String, String> map = new HashMap<>();

		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			String val = pi.getValue(obj) + "";
			map.put(pi.getName(), val);
		}

		String result = urlEncode(map);

		return result;
	}

	public static void displayHeaderFields(final HttpURLConnection httpURLConnection) throws IOException {
		StringBuilder builder = new StringBuilder();
		Map<String, List<String>> map = httpURLConnection.getHeaderFields();
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}
			builder.append(entry.getKey()).append(": ");

			List<String> headerValues = entry.getValue();
			Iterator<String> it = headerValues.iterator();
			if (it.hasNext()) {
				builder.append(it.next());
				while (it.hasNext()) {
					builder.append(", ").append(it.next());
				}
			}
			builder.append("\n");
		}
		System.out.println(builder);
	}

	public static void main(String[] args) throws Exception {
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
		map.put("stocking", "true");
		/*
				s = client.get("http://localhost:18080/retail-products/itemRestriction", map);
		
				s = client.post("http://localhost:18080/retail-products/iseries/itemRestriction/get", map);
		*/

		// s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/get", map);

		map = new HashMap<String, String>();
		map.put("itemRuleType", "LINE_ITEM"); // LINE, PRODUCT_LINE_CODE, PRODUCT_LINE_SUBCODE, LINE_ITEM;
		map.put("changeType", "SALES_RESTRICTED"); // SALES_RESTRICTED, FLIGHT_RESTRICTED, CAUSTIC, HYBRID_ELECTRIC, FREON_RESTRICTED;
		map.put("updateType", "ADD"); // CHANGE, ADD, DELETE, CLEAR
		// node.set("locationRuleType", ""); // NOT_USED, STORE_ID, ZIPCODE, STATE, COUNTY
		map.put("newValue", "");
		map.put("line", "WIX");
		map.put("productLineCode", "-1");
		map.put("productLineSubcode", "-1");
		map.put("item", "");
		map.put("storeId", "1234");
		map.put("zipcode", "54321");
		map.put("state", "MO");
		map.put("county", "GREENE");
		map.put("salesRestrictedEffectiveDate", new OADate().toString(OADate.JsonFormat));
		s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/put", map);

		OAJsonRootNode node = new OAJsonObjectNode();
		node.set("itemRuleType", "LINE_ITEM"); // LINE, PRODUCT_LINE_CODE, PRODUCT_LINE_SUBCODE, LINE_ITEM;
		node.set("changeType", "SALES_RESTRICTED"); // SALES_RESTRICTED, FLIGHT_RESTRICTED, CAUSTIC, HYBRID_ELECTRIC, FREON_RESTRICTED;
		node.set("updateType", "ADD"); // CHANGE, ADD, DELETE, CLEAR
		// node.set("locationRuleType", ""); // NOT_USED, STORE_ID, ZIPCODE, STATE, COUNTY
		node.set("newValue", "");
		node.set("line", "WIX");
		node.set("productLineCode", -1);
		node.set("productLineSubcode", -1);
		node.set("item", "");
		node.set("storeId", 1234);
		node.set("zipcode", "54321");
		node.set("state", "MO");
		node.set("county", "GREENE");
		node.set("salesRestrictedEffectiveDate", new OADate());

		String json = node.toJson();
		s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/put", json);

		//		s = client.post("http://localhost:8081/retail-products/iseries/items/getSalesRestrictedItemsByLocation", map);

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

}

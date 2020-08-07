package com.viaoa.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.viaoa.object.OAObject;

/*

 http://rest.elkstein.org/2008/02/using-rest-in-java.html

https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types

https://www.codota.com/code/java/methods/java.net.URLConnection/setRequestProperty

https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication

  response 401 (Unauthorized)
 WWW-Authenticate ...
 response 403 Forbidden


	Transfer-Encoding: chunked
	Server: Jetty(9.4.19.v20190610)
	Set-Cookie: JSESSIONID=node010qaw7pt1lgctisiwjo2vipcd3.node0; Path=/
	Expires: Thu, 01 Jan 1970 00:00:00 GMT
	Content-Type: application/json;charset=utf-8
*/

// qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq  NOT DONE .... original plan is to be a client for  OARestServlet

/**
 * @author vvia
 */
public class OARestClient {

	private String hostUrl;
	private String userId;
	private transient String password;
	private String cookie;

	public OARestClient(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	public void setUserAccess(String userId, String password) {
		this.userId = userId;
		this.password = password;
	}

	public void setCookie(String val) {
		this.cookie = val;
	}

	public <T extends OAObject> T get(Class<T> responseClass, String id) throws Exception {
		String json = get(hostUrl + "/" + id);

		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);

		return obj;
	}

	public String get(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestProperty("User-Agent", "OAHttpJsonClient");
		conn.setRequestMethod("GET");
		conn.setDoOutput(true);

		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		conn.setRequestProperty("Content-Type", "text/plain");
		conn.setRequestProperty("charset", "utf-8");

		conn.setRequestProperty("Accept", "application/json"); // "application/json, text/*;q=0.7");

		if (OAString.isNotEmpty(cookie)) {
			conn.addRequestProperty("cookie", cookie);
		}

		if (OAString.isNotEmpty(userId)) {
			String s = userId + ":" + password;
			conn.setRequestProperty("Authorization", "Basic " + Base64.encode(s));
		}

		// perform http

		String setcookie = conn.getHeaderField("Set-Cookie");
		if (OAString.isNotEmpty(setcookie)) {
			this.cookie = OAString.field(setcookie, ";", 1);
		}

		if (conn.getResponseCode() != 200) {
			throw new IOException("Response code not 200, returned: " + conn.getResponseCode() + ", msg=" + conn.getResponseMessage());
		}

		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		for (;;) {
			int ch = rd.read();
			if (ch < 0) {
				break;
			}
			sb.append((char) ch);
		}

		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();
	}

	public <T extends OAObject> String post(String strUrlBase, OAObject obj, Class<T> responseClass) throws Exception {
		if (obj == null) {
			return null;
		}
		if (strUrlBase != null && !strUrlBase.endsWith("/")) {
			strUrlBase += "/";
		}
		OAJaxb jaxb = new OAJaxb<>(obj.getClass());
		jaxb.setIncludeOwned(true);

		String json = jaxb.convertToJSON(obj);

		String surl = strUrlBase + obj.getClass().getSimpleName() + "/" + obj.getJaxbSinglePartId();

		String s = post(surl, json);

		return s;
	}

	public static String post(String urlStr, String json) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		conn.setRequestProperty("Content-Type", "application/json");

		// Create the form content
		OutputStream out = conn.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");

		writer.write(json);
		writer.close();
		out.close();

		int respCode = conn.getResponseCode();

		if (respCode != 200) {
			//      throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();
	}

	public static String httpPost(String urlStr, String[] paramName, String[] paramVal) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		// Create the form content
		OutputStream out = conn.getOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		if (paramName != null) {
			for (int i = 0; i < paramName.length; i++) {
				writer.write(paramName[i]);
				writer.write("=");
				writer.write(URLEncoder.encode(paramVal[i], "UTF-8"));
				writer.write("&");
			}
		}
		writer.close();
		out.close();

		if (conn.getResponseCode() != 200) {
			//      throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();
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

		s = httpGetAsJson("http://localhost:8081/retail-products/iseries/itemRestriction/getRestriction?line=fRE&productLineCode=0&productLineSubcode=123&item=R134A-30&storeId=12345&zipcode=44260&state=wI&county=Oranga");

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

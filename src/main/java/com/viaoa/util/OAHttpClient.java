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

import com.viaoa.object.OAObject;

// http://rest.elkstein.org/2008/02/using-rest-in-java.html

public class OAHttpClient {

	public static String httpGet(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
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

	//qqqqqq todo:  need to be able to set body as JSON
	public static String httpPost(String urlStr, OAObject obj) throws Exception {
		if (obj == null) {
			return null;
		}
		OAJaxb jaxb = new OAJaxb<>(obj.getClass());
		jaxb.setIncludeOwned(true);

		String json = jaxb.convertToJSON(obj);
		String s = httpPost(urlStr, json);
		return s;
	}

	public static String httpPost(String urlStr, String json) throws Exception {
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

	public static void main(String[] args) throws Exception {
		String s;
		// s = httpGet("http://localhost:8082/servlet/oarest/salesorder/23548?pp=salesorderitems.item.mold");

		// s = httpPost("http://localhost:8082/servlet/oarest/salesorder/23548", null, null);

		// s = httpPost("http://localhost:8082/servlet/oarest/salesorder/23548", null, null);

		s = httpGet("http://localhost:8081/retail-products/iseries/itemRestriction/getRestriction?line=fRE&productLineCode=0&productLineSubcode=123&item=R134A-30&storeId=12345&zipcode=44260&state=wI&county=Oranga");

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

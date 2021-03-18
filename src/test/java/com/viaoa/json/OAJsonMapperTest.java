package com.viaoa.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Test;

import com.oreillyauto.remodel.model.oa.Column;
import com.oreillyauto.remodel.model.oa.Database;
import com.oreillyauto.remodel.model.oa.Table;
import com.viaoa.OAUnitTest;
import com.viaoa.json.node.OAJsonArrayNode;
import com.viaoa.json.node.OAJsonNode;
import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.util.OADate;
import com.viaoa.util.OAReflect;

/**
 * OAJson unit tests.<br>
 * Uses sample files in resource for comparing and validating.
 *
 * @author vvia
 */
public class OAJsonMapperTest extends OAUnitTest {

	// formatting was validated using:  https://www.freeformatter.com/json-formatter.html

	@Test
	public void convertObjectToJsonTest() throws Exception {
		Database database = new Database();
		database.setId(7);
		database.setName("dbName");

		String json = OAJsonMapper.convertObjectToJson(database);

		OAJson oaj = new OAJson();
		OAJsonObjectNode node = (OAJsonObjectNode) oaj.load(json);
		node.set("name", "newDBName");
		json = node.toJson();

		Database database2 = (Database) OAJsonMapper.convertJsonToObject(json, Database.class);

		assertEquals(database2.getName(), "newDBName");
	}

	@Test
	public void test() throws Exception {
		Database database = new Database();
		database.setId(8);
		database.setName("dbName");
		for (int i = 0; i < 5; i++) {
			Table t = new Table();
			t.setName("table" + i);
			database.getTables().add(t);
			for (int j = 0; j < 5; j++) {
				Column c = new Column();
				c.setName("col" + j);
				t.getColumns().add(c);
			}
		}

		ArrayList<String> al = new ArrayList();
		String json = OAJsonMapper.convertObjectToJson(database, al);
		assertTrue(json.indexOf("tables") < 0);
		assertTrue(json.indexOf("columns") < 0);

		al.add("tables");
		json = OAJsonMapper.convertObjectToJson(database, al);
		assertTrue(json.indexOf("tables") >= 0);
		assertTrue(json.indexOf("columns") < 0);

		al.add("tables.columns");
		json = OAJsonMapper.convertObjectToJson(database, al);
		assertTrue(json.indexOf("tables") >= 0);
		assertTrue(json.indexOf("columns") >= 0);

		OAJson oaj = new OAJson();
		OAJsonObjectNode node = (OAJsonObjectNode) oaj.load(json);
		node.set("name", "newDBName");
		json = node.toJson();

		Database database2 = (Database) OAJsonMapper.convertJsonToObject(json, Database.class);

		assertEquals(database2.getName(), "newDBName");
	}

	@Test
	public void convertToJsonValueTest() throws Exception {
		String s = OAJsonMapper.convertToJsonValue(12);
		assertEquals("12", s);

		s = OAJsonMapper.convertToJsonValue(new OADate());
		assertEquals("\"" + (new OADate()).toString(OADate.JsonFormat) + "\"", s);

		s = OAJsonMapper.convertToJsonValue(null);
		assertEquals("null", s);

		s = OAJsonMapper.convertToJsonValue(123.456);
		assertEquals("123.456", s);
	}

	@Test
	public void convertMethodArgumentsToJsonTest() throws Exception {
		String s = OAJsonMapper.convertToJsonValue(12);
		assertEquals("12", s);

		// int testMethod(Table table, int i, OADate d, String s)
		Method method = OAReflect.getMethod(Database.class, "testMethod");
		assertNotNull(method);

		Database database = new Database();
		database.setId(9);
		/*
		convertMethodArgumentsToJson(final Method method, final Object[] argValues,
						final List<String>[] lstIncludePropertyPathss, boolean bSkipFirst) throws Exception {
				*/

		Object[] objs = new Object[] { new Table(), 7, new OADate(), "abc" };
		OAJsonArrayNode arrayNode = OAJsonMapper.convertMethodArgumentsToJson(method, objs, null, null);

		objs = new Object[] { database, new Table(), 7, new OADate(), "abc" };
		OAJsonArrayNode arrayNode2 = OAJsonMapper.convertMethodArgumentsToJson(method, objs, null, new int[] { 0 });

		ArrayList<String> al = arrayNode.getChildrenPropertyNames();
		assertEquals(5, al == null ? 0 : al.size());
	}

	@Test
	public void convertObjectToJsonNodeTest() throws Exception {
		Database[] dbs = new Database[3];
		for (int r = 0; r < 3; r++) {
			Database database = new Database();
			dbs[r] = database;
			database.setId(10 + r);
			database.setName("dbName" + r);
			for (int i = 0; i < 5; i++) {
				Table t = new Table();
				t.setName("table" + i);
				database.getTables().add(t);
				for (int j = 0; j < 5; j++) {
					Column c = new Column();
					c.setName("col" + j);
					t.getColumns().add(c);
				}
			}
		}

		ArrayList<String> al = new ArrayList();
		OAJsonNode node = OAJsonMapper.convertObjectToJsonNode(dbs, al);
		assertTrue(node instanceof OAJsonArrayNode);

		al.add("tables.columns");
		node = OAJsonMapper.convertObjectToJsonNode(dbs, al);
		assertTrue(node instanceof OAJsonArrayNode);

		int xx = 4;
		xx++;
	}

	public static void main(String[] args) throws Exception {
		OAJsonMapperTest util = new OAJsonMapperTest();

		util.convertObjectToJsonTest();

		int xx = 4;
		xx++;
	}

}
